package checkers.scope;

import static checkers.Utils.isFinal;
import static checkers.Utils.SCJMethod.ALLOC_IN_PARENT;
import static checkers.scjAllowed.EscapeMap.escapeAnnotation;
import static checkers.scjAllowed.EscapeMap.escapeEnum;
import static checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION;
import static checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE;
import static checkers.scope.ScopeChecker.ERR_BAD_ENTER_PARAM;
import static checkers.scope.ScopeChecker.ERR_BAD_ENTER_PRIVATE_MEMORY_RUNS_IN_NO_MATCH;
import static checkers.scope.ScopeChecker.ERR_BAD_ENTER_TARGET;
import static checkers.scope.ScopeChecker.ERR_BAD_EXECUTE_IN_AREA_OR_ENTER;
import static checkers.scope.ScopeChecker.ERR_BAD_EXECUTE_IN_AREA_TARGET;
import static checkers.scope.ScopeChecker.ERR_BAD_GUARD_ARGUMENT;
import static checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE;
import static checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE;
import static checkers.scope.ScopeChecker.ERR_BAD_RETURN_SCOPE;
import static checkers.scope.ScopeChecker.ERR_BAD_VARIABLE_SCOPE;
import static checkers.scope.ScopeChecker.ERR_DEFAULT_BAD_ENTER_PARAMETER;
import static checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT;
import static checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE;
import static checkers.scope.ScopeChecker.ERR_MEMORY_AREA_NO_DEFINE_SCOPE_ON_VAR;
import static checkers.scope.ScopeChecker.ERR_RUNNABLE_WITHOUT_RUNS_IN;
import static checkers.scope.ScopeChecker.ERR_TYPE_CAST_BAD_ENTER_PARAMETER;
import static checkers.scope.ScopeInfo.CURRENT;
import static checkers.scope.ScopeInfo.UNKNOWN;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

import checkers.SCJVisitor;
import checkers.Utils;
import checkers.Utils.SCJMethod;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;

//TODO: Defaults for @RunsIn/@Scope on specific SCJ classes
//TODO: Unscoped method parameters?
//TODO: Anonymous runnables
//TODO: Errors for using annotated classes in unannotated classes
//TODO: Add illegal scope location errors back in

/**
 * @Scope("FooMission") RunsIn is implied to be FooMission class FooMission
 *
 *                      We can no longer infer the scope we need to add based on
 *                      the annotations. Thus, each mission must have a @DefineScope
 *                      on it now, and if it doesn't it's assumed to add a scope
 *                      namve with the name of the mission and the parent as
 *                      immortal.
 *
 *                      For any EH, you should have something like
 *
 * @Scope("AMissionMemory") @RunsIn("MyPEH") class MyPEH extends PEH ...
 *
 *                          And the checker should automatically detect that
 *                          allocations of MyPEH objects are in AMissionMemory.
 *                          It might be worth it to restrict it to initialize(),
 *                          but that's just extra work. I wouldn't bother doing
 *                          it unless not restricting it breaks in some case
 *                          (which it may, I have not thought about it much).
 *
 */
public class ScopeVisitor<P> extends SCJVisitor<ScopeInfo, P> {

    private AnnotatedTypeFactory atf;
    private ScopeInfo currentRunsIn = null;
    private ScopeCheckerContext ctx;
    private ScopeTree scopeTree;
    private VariableScopeTable varScopes = new VariableScopeTable();

    public ScopeVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext ctx) {
        super(checker, root);
        atf = checker.createFactory(root);
        this.ctx = ctx;
        scopeTree = ctx.getScopeTree();
    }

    @Override
    public ScopeInfo visitAnnotation(AnnotationTree node, P p) {
        // Don't check annotations, since they result in assignment ASTs that
        // shouldn't be checked as normal assignments.
        return null;
    }

    @Override
    public ScopeInfo visitArrayAccess(ArrayAccessTree node, P p) {
        // Since arrays live in the same scope as their component type, the
        // scope of the array is the same as the scope of any access to its
        // elements.
        ScopeInfo s = node.getExpression().accept(this, p);
        node.getIndex().accept(this, p);
        return s;
    }

    @Override
    public ScopeInfo visitAssignment(AssignmentTree node, P p) {
        debugIndentIncrement("visitAssignment : " + node);

        ScopeInfo lhs = node.getVariable().accept(this, p);
        ScopeInfo rhs = node.getExpression().accept(this, p);

        debugIndent("> lhs : " + lhs.getScope());
        debugIndent("> rhs : " + rhs.getScope());

        if (!lhs.equals(rhs) || lhs.isUnknown())
            checkAssignment(lhs, rhs, node);
        debugIndentDecrement();
        return lhs;
    }

    @Override
    public ScopeInfo visitBinary(BinaryTree node, P p) {
        super.visitBinary(node, p);
        if (TreeUtils.isCompileTimeString(node))
            return ScopeInfo.CURRENT;
        else if (TreeUtils.isStringConcatenation(node))
            return ScopeInfo.CURRENT;
        return ScopeInfo.PRIMITIVE; // Primitive expressions have no scope
    }

    @Override
    public ScopeInfo visitBlock(BlockTree node, P p) {
        debugIndentIncrement("visitBlock");
        ScopeInfo oldRunsIn = currentRunsIn;
        if (node.isStatic())
            currentRunsIn = ScopeInfo.IMMORTAL;
        varScopes.pushBlock();
        super.visitBlock(node, p);
        varScopes.popBlock();
        currentRunsIn = oldRunsIn;
        debugIndentDecrement();
        return null;
    }

    @Override
    public ScopeInfo visitClass(ClassTree node, P p) {
        debugIndentIncrement("visitClass " + node.getSimpleName());
        debugIndent("visitClass :"
                + TreeUtils.elementFromDeclaration(node).getQualifiedName());

        if (escapeEnum(node) || escapeAnnotation(node)) {
            debugIndent("visitClass : escaping the Class. ");
            debugIndentDecrement();
            return null;
        }

        TypeElement t = TreeUtils.elementFromDeclaration(node);
        ScopeInfo oldRunsIn = currentRunsIn;

        ScopeInfo scope = ctx.getClassScope(t);
        varScopes.pushBlock();
        varScopes.addVariableScope("this", scope);

        // TODO: assume defaults for inner classes?
        Utils.debugPrintln("Seen class " + t.getQualifiedName() + ": @Scope("
                + scope + ")");

        currentRunsIn = scope;
        super.visitClass(node, p);
        varScopes.popBlock();
        currentRunsIn = oldRunsIn;
        debugIndentDecrement();
        return null;
    }

    @Override
    public ScopeInfo visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        debugIndentIncrement("visitCompoundAssignment : " + node);
        ScopeInfo ret = null;
        ScopeInfo lhs = node.getVariable().accept(this, p);
        if (TreeUtils.isStringCompoundConcatenation(node)) {
            if (!lhs.isCurrent()) {
                // TODO: report error
            }
            ret = lhs;
        }
        debugIndentDecrement();
        return ret;
    }

    @Override
    public ScopeInfo visitConditionalExpression(ConditionalExpressionTree node,
            P p) {
        debugIndentIncrement("visitConditionalExpression : " + node);

        node.getCondition().accept(this, p);
        ScopeInfo t = node.getTrueExpression().accept(this, p);
        ScopeInfo f = node.getFalseExpression().accept(this, p);
        ScopeInfo ret = null;
        // TODO: Not sure if this is right
        if (t != null) {
            if (!t.equals(f)) {
                // TODO: report error
            }
            ret = t;
        }
        debugIndentDecrement();
        return ret;
    }

    @Override
    public ScopeInfo visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        // TODO: Not sure if this needs to be checked. This implicitly does
        // .iterator() and .next() calls.
        varScopes.pushBlock();
        super.visitEnhancedForLoop(node, p);
        varScopes.popBlock();
        return null;
    }

    @Override
    public ScopeInfo visitForLoop(ForLoopTree node, P p) {
        varScopes.pushBlock();
        super.visitForLoop(node, p);
        varScopes.popBlock();
        return null;
    }

    @Override
    public ScopeInfo visitIdentifier(IdentifierTree node, P p) {
        debugIndentIncrement("visitIdentifier : " + node);
        Element elem = TreeUtils.elementFromUse(node);
        ScopeInfo ret = null;

        // when accessing this.method(), then this is type of FIELD, but
        // we need to handle the this case specially.
        if (elem.getKind() == ElementKind.FIELD && !isThis(node)) {
            ScopeInfo scope = ctx.getFieldScope((VariableElement) elem);
            DefineScopeInfo defineScope = null;

            //add DefineScopeInfo where needed
            if (needsDefineScope(Utils.getTypeElement(Utils.getBaseType(elem.asType()))))
                defineScope = ctx.getFieldDefineScope((VariableElement) elem);

            ret = new FieldScopeInfo(varScopes.getVariableScope("this"), scope, defineScope);
        } else if (elem.getKind() == ElementKind.LOCAL_VARIABLE
                || elem.getKind() == ElementKind.PARAMETER) {
            String var = node.getName().toString();
            ScopeInfo scope = varScopes.getVariableScope(var);

            //add DefineScopeInfo where needed
            if (needsDefineScope(Utils.getTypeElement(Utils.getBaseType(elem.asType()))))
                scope.defineScope = varScopes.getVariableDefineScope(var);

            ret = scope;
        } else if (elem.getKind() == ElementKind.METHOD
                || elem.getKind() == ElementKind.CONSTRUCTOR
                || elem.getKind() == ElementKind.FIELD && isThis(node)) {
            // If an identifier gets visited and its element is a method, then
            // it is part of a MethodInvocationTree as the method select.
            // It's either a static method, in which case there is no receiver
            // object, or it's an instance method invoked on the current
            // object, in which case it is implicitly invoked on "this". We
            // return the scope of "this", which will be discarded if the
            // method being invoked is static.
            ScopeInfo scope = varScopes.getVariableScope("this");
            debugIndent("\t method/constructor scope:" + scope);
            ret = scope;
        } else if (elem.getKind() == ElementKind.CLASS
                || elem.getKind() == ElementKind.INTERFACE) {
            // If we're visiting an identifer that's a class, then it's
            // probably being used in a static field member select.
            ret = ScopeInfo.IMMORTAL;
        } else
            debugIndent("\t identifier scope [NO CASE]:" + null);
        debugIndentDecrement();
        return ret;
    }

    @Override
    public ScopeInfo visitIf(IfTree node, P p) {
        node.getCondition().accept(this, p);
        varScopes.pushBlock();
        checkForDynamicGuard(node.getCondition());
        node.getThenStatement().accept(this, p);
        varScopes.popBlock();
        // We don't need a new block for the else block. The block for the
        // if statement is just to cover the relation when the if statement
        // is a guard.
        StatementTree elseBlock = node.getElseStatement();
        if (elseBlock != null)
            elseBlock.accept(this, p);
        return null;
    }

    @Override
    public ScopeInfo visitLiteral(LiteralTree node, P p) {
        debugIndentIncrement("visitLiteral : " + node);
        debugIndent(" node's value : " + node.getValue());
        // TODO: Are array literals handled in this?
        ScopeInfo ret = ScopeInfo.PRIMITIVE;
        if (node.getValue() == null)
            ret = ScopeInfo.NULL;
        else if (node.getValue() instanceof String)
            // TODO I foresee much sadness in this later on. Strings can't
            // really interact between IMMORTAL and other scopes if it's not
            // RunsIn(UNKNOWN).
            ret = ScopeInfo.IMMORTAL;
        debugIndentDecrement();
        return ret;
    }

    @Override
    public ScopeInfo visitMemberSelect(MemberSelectTree node, P p) {
        Element elem = TreeUtils.elementFromUse(node);
        ScopeInfo receiver = node.getExpression().accept(this, p);
        ScopeInfo ret;

        debugIndentIncrement("visitMemberSelect: " + node.toString());
        if (elem.getKind() == ElementKind.METHOD)
            // If a MemberSelectTree is not a field, then it is a method
            // that is part of a MethodInvocationTree. In this case, we
            // want to return the scope of the receiver object so that
            // visitMethodInvocation has its scope.
            ret = node.getExpression().accept(this, p);
        else {
            VariableElement f = (VariableElement) elem;
            // TODO: this is ugly
            if (f.toString().equals("class")) {
                // TODO: is this correct?
                ret = new FieldScopeInfo(ScopeInfo.CURRENT, ScopeInfo.CURRENT);
            }
            else {
                ScopeInfo fScope = ctx.getFieldScope(f);
                ret = new FieldScopeInfo(receiver, fScope);
            }
        }
        debugIndentDecrement();
        return ret;
    }

    @Override
    public ScopeInfo visitMethod(MethodTree node, P p) {
        debugIndentIncrement("visitMethod " + node.getName());
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);

        debugIndent("Seen method " + m.getSimpleName());
        debugIndent("RunsIn: " + currentRunsIn);

        ScopeInfo oldRunsIn = currentRunsIn;
        ScopeInfo runsIn = ctx.getEffectiveMethodRunsIn(m, currentScope());
        debugIndent("@RunsIn(" + runsIn + ") " + m.getSimpleName());

        currentRunsIn = runsIn;
        varScopes.pushBlock();
        List<? extends VariableTree> params = node.getParameters();
        List<ScopeInfo> paramScopes = ctx.getParameterScopes(m);
        List<DefineScopeInfo> defineScopes = ctx.getParameterDefineScopes(m);
        for (int i = 0; i < paramScopes.size(); i++) {
            VariableTree param = params.get(i);
            String paramName = param.getName().toString();
            debugIndent(" add VarScope: " + paramName + ", scope:"
                    + paramScopes.get(i));
            varScopes.addVariableScope(paramName, paramScopes.get(i));
            DefineScopeInfo dsi = defineScopes.get(i);
            if (dsi != null) {
                varScopes.addVariableDefineScope(paramName, dsi);
            }
        }
        node.getBody().accept(this, p);
        // TODO: make sure we don't need to visit more
        varScopes.popBlock();
        currentRunsIn = oldRunsIn;
        debugIndentDecrement();
        return null;
    }

    @Override
    public ScopeInfo visitMethodInvocation(MethodInvocationTree node, P p) {
        debugIndentIncrement("visitMethodInvocation : " + node);
        List<ScopeInfo> argScopes = new ArrayList<ScopeInfo>(node
                .getArguments().size());
        for (ExpressionTree arg : node.getArguments())
            argScopes.add(arg.accept(this, p));

        ScopeInfo recvScope = node.getMethodSelect().accept(this, p);
        debugIndentDecrement();
        return checkMethodInvocation(TreeUtils.elementFromUse(node), recvScope,
                argScopes, node);
    }

    @Override
    public ScopeInfo visitNewArray(NewArrayTree node, P p) {
        debugIndentIncrement("visitNewArray");
        TypeMirror arrayType = InternalUtils.typeOf(node);
        TypeMirror componentType = Utils.getBaseType(arrayType);
        if (!componentType.getKind().isPrimitive()) {
            TypeElement t = Utils.getTypeElement(componentType);
            ScopeInfo scope = ctx.getClassScope(t);
            if (!(scope.isCurrent() || scope.equals(currentScope())))
                fail(ERR_BAD_ALLOCATION, node, currentScope(), scope);
        }
        super.visitNewArray(node, p);
        debugIndentDecrement();
        return currentScope();
    }

    /**
     * Object allocation is only allowed if the current allocation context is
     * the same scope as what's defined by the class.
     */
    @Override
    public ScopeInfo visitNewClass(NewClassTree node, P p) {
        debugIndentIncrement("visitNewClass");
        ExecutableElement ctorElement = TreeUtils.elementFromUse(node);
        ScopeInfo nodeClassScope = ctx.getClassScope(Utils
                .getMethodClass(ctorElement));
        if (nodeClassScope != null && !currentScope().equals(nodeClassScope)
                && !nodeClassScope.isCurrent())
            // Can't call new unless the type has the same scope as the
            // current context
            fail(ERR_BAD_ALLOCATION, node, currentScope(), nodeClassScope);
        super.visitNewClass(node, p);
        debugIndentDecrement();
        return currentScope();
    }

    @Override
    public ScopeInfo visitParenthesized(ParenthesizedTree node, P p) {
        return node.getExpression().accept(this, p);
    }

    @Override
    public ScopeInfo visitReturn(ReturnTree node, P p) {
        debugIndentIncrement("visitReturn:" + node.toString());

        // Don't try to check return expressions for void methods.
        if (node.getExpression() == null)
            return super.visitReturn(node, p);

        MethodTree enclosingMethod = TreeUtils
                .enclosingMethod(getCurrentPath());

        // skip checking when return is primitive
        Tree nodeTypeTree = enclosingMethod.getReturnType();
        if (nodeTypeTree.getKind() == Kind.PRIMITIVE_TYPE)
            return ScopeInfo.PRIMITIVE;

        ExecutableElement m = TreeUtils.elementFromDeclaration(enclosingMethod);
        ScopeInfo returnScope = ctx.getMethodScope(m);
        ScopeInfo exprScope = node.getExpression().accept(this, p);

        debugIndent("expected return scope is: " + returnScope);
        debugIndent("actual return scope is:" + exprScope);
        checkReturnScope(exprScope, returnScope, node);
        debugIndentDecrement();
        return returnScope;
    }

    @Override
    public ScopeInfo visitTry(TryTree node, P p) {
        varScopes.pushBlock();
        node.getBlock().accept(this, p);
        varScopes.popBlock();
        for (CatchTree c : node.getCatches()) {
            varScopes.pushBlock();
            c.accept(this, p);
            varScopes.popBlock();
        }
        varScopes.pushBlock();
        node.getFinallyBlock();
        varScopes.popBlock();
        // There is another accessor called getResources. No idea what it does.
        return null;
    }

    @Override
    public ScopeInfo visitTypeCast(TypeCastTree node, P p) {
        debugIndentIncrement("visitTypeCast " + node);
        if (isPrimitiveExpression(node)) {
            debugIndentDecrement();
            return null;
        }

        ScopeInfo scope = node.getExpression().accept(this, p);
        TypeMirror m = Utils.getBaseType(InternalUtils.typeOf(node));
        ScopeInfo cast = ctx.getClassScope(Utils.getTypeElement(m));

        debugIndentDecrement();
        return cast.isCurrent() ? scope : cast;
    }

    /**
     * <ul>
     * <li>Static variables must always be in the immortal scope.
     * <li>Instance variables must make sure that the scope of the enclosing
     * class is a child scope of the variable type's scope.
     * <li>Local variables are similar to instance variables, only it must first
     * use the RunsIn annotation, if any exists, before using the Scope
     * annotation on the class that it belongs to.
     * </ul>
     */
    @Override
    public ScopeInfo visitVariable(VariableTree node, P p) {
        debugIndentIncrement("visitVariable : " + node.toString());
        ScopeInfo oldRunsIn = currentRunsIn;

        if (node.getType().getKind() == Kind.PRIMITIVE_TYPE) {
            debugIndentDecrement();
            return null;
        }

        ScopeInfo lhs = checkVariableScope(node);
        debugIndent(" scope's variable: " + lhs);
        varScopes.addVariableScope(node.getName().toString(), lhs);
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        if (needsDefineScope(Utils.getTypeElement(Utils.getBaseType(var.asType())))) {
            debugIndent(" needs @DefineScope.");
            // if this is IDENTIFIER(a field), then this was already processed in ScopeRunsInVisitor
            if (TreeUtils.elementFromDeclaration(node).getKind()  != ElementKind.FIELD)
                checkDefineScopeOnVariable(var,lhs,node);
        }

        // Static variable, change the context to IMMORTAL
        if (Utils.isStatic(var))
            currentRunsIn = ScopeInfo.IMMORTAL;

        if (node.getInitializer() != null) {
            ScopeInfo rhs = node.getInitializer().accept(this, p);
            checkAssignment(lhs, rhs, node);
        }

        currentRunsIn = oldRunsIn;
        debugIndentDecrement();
        return null;
    }

    private void checkAssignment(ScopeInfo lhs, ScopeInfo rhs, Tree node) {
        debugIndentIncrement("checkAssignment: " + node.toString());
        if (lhs.isFieldScope())
            checkFieldAssignment((FieldScopeInfo) lhs, rhs, node);
        else
            // TODO: Do we need an extra case for arrays?
            checkLocalAssignment(lhs, rhs, node);
        debugIndentDecrement();
    }

    private void checkDefineScopeOnVariable(VariableElement var, ScopeInfo varScope,
            VariableTree node) {
        debugIndent("checkDefineScopeOnVariable.");

        // TODO: Is this replaceable with isUserElement(Element)?
        if (!Utils.isUserLevel(var.getEnclosingElement().toString()))
            return;
        if (var.asType().getKind() != TypeKind.DECLARED)
            return;

        DefineScope ds = var.getAnnotation(DefineScope.class);
        if (ds == null) {
            fail(ERR_MEMORY_AREA_NO_DEFINE_SCOPE_ON_VAR, node);
            return;
        }

        ScopeInfo scope = new ScopeInfo(ds.name());
        ScopeInfo parent = new ScopeInfo(ds.parent());
        DefineScopeInfo dsi = new DefineScopeInfo(scope, parent);

        if (!scopeTree.hasScope(scope) || !scopeTree.isParentOf(scope, parent))
            fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT, node);

        ScopeInfo runsInScope = getEnclosingMethodRunsIn();
        if (!varScope.isCurrent()) {
            if (!varScope.equals(parent))
                fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE,
                        node, varScope, parent);
        } else if (runsInScope.isUnknown()) {
            fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE, node,
                    UNKNOWN, parent);
        } else if (!runsInScope.equals(parent))
            fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE, node,
                    runsInScope, parent);

        varScopes.addVariableDefineScope(var.toString(), dsi);
    }

    private void checkFieldAssignment(FieldScopeInfo lhs, ScopeInfo rhs,
            Tree node) {
        debugIndentIncrement("checkFieldAssignment");
        debugIndent("lhs receiver scope = " + lhs.getReceiverScope());
        debugIndent("lhs field scope = " + lhs.getFieldScope());
        debugIndent("lhs scope = " + lhs);
        debugIndent("rhs scope = " + rhs);
        if (!lhs.isUnknown()) {
            if (lhs.getFieldScope().isCurrent()) {
                if (!lhs.getReceiverScope().equals(rhs))
                    fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
            } else if (!lhs.getFieldScope().equals(rhs))
                fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
        } else {
            ScopeInfo fScope = lhs.getFieldScope();
            String rhsVar = getRhsVariableNameFromAssignment(node);
            String lhsVar = getLhsVariableNameFromAssignment(node);
            if (fScope.isCurrent()) {
                if (!varScopes.hasSameRelation(lhsVar, rhsVar))
                    fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
            } else if (!varScopes.hasParentRelation(lhsVar, rhsVar))
                fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
        }
        debugIndentDecrement();
    }

    private String getLhsVariableNameFromAssignment(Tree node) {
        if (node.getKind() == Kind.ASSIGNMENT) {
            AssignmentTree tree = (AssignmentTree) node;
            ExpressionTree lhs = tree.getVariable();

            if (lhs.getKind() == Kind.MEMBER_SELECT) {
                MemberSelectTree mst = (MemberSelectTree) lhs;
                if (mst.getExpression().getKind() == Kind.IDENTIFIER)
                    return mst.getExpression().toString();
            }
            return null;
        } else if (node.getKind() == Kind.VARIABLE)
            return "this";
        else
            throw new RuntimeException("Unexpected assignment AST node: "
                    + node.getKind());
    }

    private String getRhsVariableNameFromAssignment(Tree node) {
        ExpressionTree rhs;
        if (node.getKind() == Kind.ASSIGNMENT) {
            AssignmentTree tree = (AssignmentTree) node;
            rhs = tree.getExpression();
        } else if (node.getKind() == Kind.VARIABLE) {
            VariableTree tree = (VariableTree) node;
            rhs = tree.getInitializer();
        } else
            throw new RuntimeException("Unexpected assignment AST node: "
                    + node.getKind());
        if (rhs.getKind() == Kind.IDENTIFIER)
            return ((IdentifierTree) rhs).toString();
        return null;
    }

    private void checkLocalAssignment(ScopeInfo lhs, ScopeInfo rhs, Tree node) {
        if (lhs.isUnknown() || rhs.isNull())
            return;
        if (!concretize(lhs).equals(concretize(rhs)))
            fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
    }

    private ScopeInfo concretize(ScopeInfo scope) {
        return scope.isCurrent() ? currentScope() : scope;
    }

    /**
     *
     * @param recvScope
     *            - managedMemory instance, the target of the invocation
     * @param node
     * @return
     */
    private void checkEnterPrivateMemory(MethodInvocationTree node) {
        TypeMirror runnableType = InternalUtils.typeOf(node.getArguments().get(
                1));
        ScopeInfo argRunsIn = getRunsInFromRunnable(runnableType);

        if (!scopeTree.isParentOf(argRunsIn, currentScope()))
            fail(ERR_BAD_ENTER_PRIVATE_MEMORY_RUNS_IN_NO_MATCH, node,
                    argRunsIn, currentScope());
    }

    private ScopeInfo checkExecuteInArea(MethodInvocationTree node) {
        ScopeInfo scope = null;

        debugIndent("executeInArea Invocation: ");
        // Leaving the failures in so the static imports don't get warnings
        fail(ERR_BAD_ENTER_PARAM, node);
        fail(ERR_TYPE_CAST_BAD_ENTER_PARAMETER, node);
        fail(ERR_DEFAULT_BAD_ENTER_PARAMETER, node);
        fail(ERR_RUNNABLE_WITHOUT_RUNS_IN, node);
        fail(ERR_BAD_EXECUTE_IN_AREA_OR_ENTER, node);
        fail(ERR_BAD_EXECUTE_IN_AREA_TARGET, node);
        fail(ERR_BAD_ENTER_TARGET, node);
        return scope;
    }

    private boolean checkForValidGuardArgument(ExpressionTree arg) {
        switch (arg.getKind()) {
        case IDENTIFIER:
            VariableElement var = (VariableElement) TreeUtils
                    .elementFromUse((IdentifierTree) arg);
            var.getModifiers();

            if (!isFinal(var)) {
                fail(ERR_BAD_GUARD_ARGUMENT, arg, arg);
                return false;
            }
            break;
        default:
            fail(ERR_BAD_GUARD_ARGUMENT, arg, arg);
            return false;
        }
        return true;
    }

    private void checkForDynamicGuard(ExpressionTree condition) {
        // This should be necessary, but for some reason it is. JUnit test
        // run seems to consistently give a parenthesized AST for the
        // condition, despite the fact that it's not parenthesized.
        condition = TreeUtils.skipParens(condition);
        if (condition.getKind() != Kind.METHOD_INVOCATION)
            return;
        MethodInvocationTree method = (MethodInvocationTree) condition;
        ExecutableElement m = TreeUtils.elementFromUse(method);
        SCJMethod sig = compareName(m);
        switch (sig) {
        case ALLOC_IN_PARENT:
        case ALLOC_IN_SAME:
            ExpressionTree var1 = method.getArguments().get(0);
            ExpressionTree var2 = method.getArguments().get(1);
            boolean hasValidArgs = checkForValidGuardArgument(var1);
            hasValidArgs = hasValidArgs && checkForValidGuardArgument(var2);
            if (hasValidArgs) {
                String var1Name = var1.toString();
                String var2Name = var2.toString();
                if (sig == ALLOC_IN_PARENT)
                    varScopes.addParentRelation(var1Name, var2Name);
                else
                    varScopes.addSameRelation(var1Name, var2Name);
            }
            break;
        }
    }

    private ScopeInfo checkMethodInvocation(ExecutableElement m,
            ScopeInfo recvScope, List<ScopeInfo> argScopes,
            MethodInvocationTree node) {
        // TODO: static methods ? :
        debugIndent("\n\t checkMethodInvocation : " + node);

        ScopeInfo runsIn = ctx.getEffectiveMethodRunsIn(m, recvScope);
        checkMethodRunsIn(m, runsIn, node);
        checkMethodParameters(m, argScopes, node);

        switch (compareName(m)) {
        case ENTER_PRIVATE_MEMORY:
            checkEnterPrivateMemory(node);
            return null; // void methods don't return a scope
        case EXECUTE_IN_AREA:
            checkExecuteInArea(node);
            return null;
        case ENTER:
            // checkExecuteInArea(node); // TODO: how to check the enter()?
            // this cannot by invoked by user!!
            return null;
        case NEW_INSTANCE:
            return checkNewInstance(recvScope,node);
        case NEW_INSTANCE_IN_AREA:
            return checkNewInstanceInArea(node);
        case NEW_ARRAY:
            return checkNewArray(node);
        case NEW_ARRAY_IN_AREA:
            return checkNewInstanceInArea(node);
        case GET_MEMORY_AREA:
            return checkGetMemoryArea(node);
        case GET_CURRENT_MANAGED_MEMORY:
            return checkGetCurrentManagedMemory(node);
        default:
            return ctx.getEffectiveMethodScope(m, recvScope);
        }
    }

    private void checkMethodParameters(ExecutableElement m,
            List<ScopeInfo> argScopes, MethodInvocationTree node) {
        List<ScopeInfo> paramScopes = ctx.getParameterScopes(m);
        for (int i = 0; i < paramScopes.size(); i++)
            checkLocalAssignment(paramScopes.get(i), argScopes.get(i), node);
    }

    /**
     * Check to see if a method is invokable in the current allocation context.
     *
     * Since this method is passed the effective RunsIn of the method being
     * tested, there is no need to look at the scope of the receiver object.
     *
     * @see ScopeCheckerContext#getEffectiveMethodRunsIn(ExecutableElement, ScopeInfo)
     *
     * @param m  the element representing the method invocation
     * @param effectiveRunsIn  the effective scope in which the method runs
     * @param node  method invocation tree
     */
    private void checkMethodRunsIn(ExecutableElement m,
            ScopeInfo effectiveRunsIn, MethodInvocationTree node) {
        if (currentScope().isUnknown() && !effectiveRunsIn.isUnknown())
            fail(ERR_BAD_METHOD_INVOKE, node, CURRENT, UNKNOWN);
        else if (!effectiveRunsIn.isUnknown()
                && !effectiveRunsIn.equals(currentScope()))
            fail(ERR_BAD_METHOD_INVOKE, node, effectiveRunsIn, currentScope());
    }

    //void pln(String str ) {System.err.println(str);}

    private ScopeInfo checkNewInstance(ScopeInfo recvScope, MethodInvocationTree node) {
        ExpressionTree arg = node.getArguments().get(0);
        ScopeInfo argScope = ctx.getClassScope(getType(arg));

        if (recvScope.defineScope == null)
            throw new RuntimeException("ERROR : Could not retrieve DefineScopeInfo. " +
            		"A variable/field whose type implements Allocation Context must have a @DefineScope annotation."
                    );

        if (!argScope.equals(recvScope.defineScope.getScope()))
            fail(ERR_BAD_NEW_INSTANCE,node,argScope,recvScope.defineScope.getScope());

        return recvScope.defineScope.getScope();
    }

    /**
     * this is ugly
     * TODO: how to go from ExpressionTree to TypeMirror?
     *
     * from "Foo.class" --> "scope.memory.Foo"
     */
    private String getType(ExpressionTree arg) {
        TypeMirror type = TreeUtils.elementFromUse(arg).asType();
        String res = type.toString();
        return res.substring(res.indexOf('<')+1, res.indexOf('>'));
    }

    /**
     * @return parent scope of the current scope
     */
    private ScopeInfo checkGetCurrentManagedMemory(MethodInvocationTree node) {
        return scopeTree.getParent(currentScope());
    }

    private ScopeInfo checkNewArray(MethodInvocationTree node) {
        // TODO:
        ScopeInfo scope = null;
        return scope;
    }

    private ScopeInfo checkNewInstanceInArea(MethodInvocationTree node) {
        // TODO:
        ScopeInfo scope = null;
        return scope;
    }

    private ScopeInfo checkGetMemoryArea(MethodInvocationTree node) {
        debugIndent("checkGetMemoryArea");
        //node.getMethodSelect().accept(this, p);


        ScopeInfo scope = null;
        return scope;
    }

    private void checkReturnScope(ScopeInfo exprScope, ScopeInfo expectedScope,
            ReturnTree node) {
        debugIndent("checkReturnScope");
        if (expectedScope.isUnknown() || expectedScope.equals(exprScope)
                || exprScope == null || exprScope.isNull())
            return;
        fail(ERR_BAD_RETURN_SCOPE, node, exprScope, expectedScope);
    }

    /**
     * TODO: replace those string comparisons
     *
     * TODO: extend it to support also ManagedMemory!!!!
     *
     * @param var1
     * @param var2
     * @return
     */
    private boolean checkPrivateMemAssignmentError(VariableElement var1,
            VariableElement var2) {
        if (var1.asType().toString()
                .equals("javax.safetycritical.PrivateMemory")
                && var2.asType().toString()
                        .equals("javax.safetycritical.PrivateMemory"))
            if (!var1.getAnnotationMirrors().toString()
                    .equals(var2.getAnnotationMirrors().toString()))
                return true;
        return false;
    }

    private ScopeInfo checkVariableScope(VariableTree node) {
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        if (Utils.isStatic(var))
            return ScopeInfo.IMMORTAL;
        // TODO: UNKNOWN parameters
        TypeMirror varMirror = var.asType();
        TypeMirror varBaseMirror = Utils.getBaseType(varMirror);
        if (varBaseMirror.getKind().isPrimitive())
            if (varMirror == varBaseMirror)
                return ScopeInfo.PRIMITIVE;
            else
                // Primitive array
                // TODO: Don't feel like thinking about this now, but
                // I think we should be using the @Scope annotation
                return currentScope();
        TypeElement t = Utils.getTypeElement(varBaseMirror);
        ScopeInfo tScope = ctx.getClassScope(t);

        Scope varScope = var.getAnnotation(Scope.class);
        debugIndent("varScope : " + varScope);
        debugIndent("tScope : " + tScope);

        if (varScope == null) {
            if (tScope.isCurrent())
                return currentScope();
            else
                return tScope;
        } else if (tScope.isCurrent())
            return new ScopeInfo(varScope.value());
        else {
            if (!tScope.equals(new ScopeInfo(varScope.value())))
                fail(ERR_BAD_VARIABLE_SCOPE, node, t.getSimpleName(),
                        currentScope());
            return tScope;
        }
    }

    private boolean isPrimitiveExpression(ExpressionTree expr) {
        if (expr.getKind() == Kind.NEW_ARRAY)
            return false;
        return atf.fromExpression(expr).getKind().isPrimitive();
    }

    private ScopeInfo currentScope() {
        return currentRunsIn;
    }

    /**
     * @return the @RunsIn annotation of the "run()" method - we know that the
     *         "var" is a type of "java.lang.Runnable", so we are safe to look
     *         for the "run()" method
     * @param var
     *            - the variable passed into the
     *            enterPrivateMemory/executeInArea call as parameter
     */
    private ScopeInfo getRunsInFromRunnable(TypeMirror var) {
        TypeElement t = Utils.getTypeElement(var);
        return ctx.getMethodRunsIn(t.getQualifiedName().toString(), "run");
    }

    private static final String THIS = "this";

    private boolean isThis(IdentifierTree node) {
        return node.getName().toString().equals(THIS);
    }

    /**
     * @return - for a given position in the Tree, return the scope of the enclosing method.
     *           if the method is @RunsIn(CURRENT), we look at the scope of the enclosing class.
     */
    private ScopeInfo getEnclosingMethodRunsIn() {
        MethodTree enclosingMethod = TreeUtils.enclosingMethod(getCurrentPath());
        ExecutableElement m = TreeUtils.elementFromDeclaration(enclosingMethod);
        ScopeInfo scope = ctx.getMethodRunsIn(m);
        if (scope.isCurrent())
            scope = ctx.getClassScope(m.getEnclosingElement().toString());
        return scope;
    }
}
