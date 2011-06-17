package checkers.scope;

import static checkers.SCJMethod.ALLOC_IN_PARENT;
import static checkers.Utils.isFinal;
import static checkers.scjAllowed.EscapeMap.escapeAnnotation;
import static checkers.scjAllowed.EscapeMap.escapeEnum;
import static checkers.scope.ScopeChecker.*;
import static checkers.scope.ScopeInfo.CALLER;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.DefineScope;

import checkers.SCJSafeUpcastDefinition;
import checkers.SCJMethod;
import checkers.SCJVisitor;
import checkers.Utils;
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
        TypeMirror m = InternalUtils.typeOf(node);
        TypeKind k = m.getKind();
        if (k.isPrimitive())
            s = ScopeInfo.PRIMITIVE;
        else if (k == TypeKind.DECLARED) {
            TypeElement t = Utils.getTypeElement(InternalUtils.typeOf(node));
            ScopeInfo ts = ctx.getClassScope(t);
            if (!ts.isCaller())
                s = ts;
        }
        return s;
    }

    @Override
    public ScopeInfo visitAssignment(AssignmentTree node, P p) {
        debugIndentIncrement("visitAssignment : " + node);

        ScopeInfo lhs = node.getVariable().accept(this, p);
        ScopeInfo rhs = node.getExpression().accept(this, p);

        if (!lhs.equals(rhs) || lhs.isUnknown()
                || rhs.getRepresentedScope() != null)
            checkAssignment(lhs, rhs, node);
        checkUpcast(node);

        debugIndentDecrement();
        return lhs;
    }

    private void checkUpcast(Tree node) {

        if (node.getKind() == Kind.PRIMITIVE_TYPE) {
            return;
        } else if (node.getKind() == Kind.ASSIGNMENT) {
            AssignmentTree tree = (AssignmentTree) node;
            TypeMirror cT = InternalUtils.typeOf(tree);
            TypeMirror eT = InternalUtils.typeOf(tree.getExpression());
            checkUpcastTypes(cT, eT, node);

        } else if (node.getKind() == Kind.VARIABLE) {
            VariableTree tree = (VariableTree) node;
            TypeMirror cT = InternalUtils.typeOf(tree);
            TypeMirror eT = InternalUtils.typeOf(tree.getInitializer());
            checkUpcastTypes(cT, eT, node);

        } else if (node.getKind() == Kind.TYPE_CAST) {
            TypeCastTree tree = (TypeCastTree) node;
            TypeMirror cT = InternalUtils.typeOf(tree);
            TypeMirror eT = InternalUtils.typeOf(tree.getExpression());
            checkUpcastTypes(cT, eT, node);

        } else if (node.getKind() == Kind.METHOD_INVOCATION) {
            MethodInvocationTree tree = (MethodInvocationTree) node;
            if (escapeUpcastOnInvoke(tree))
                return;
            List<? extends VariableElement> params = TreeUtils.elementFromUse(
                    tree).getParameters();
            List<? extends ExpressionTree> args = tree.getArguments();
            checkMethodArgsForUpcast(params, args, node);

        } else if (node.getKind() == Kind.NEW_CLASS) {
            List<? extends VariableElement> params = TreeUtils.elementFromUse(
                    (NewClassTree) node).getParameters();
            List<? extends ExpressionTree> args = ((NewClassTree) node)
                    .getArguments();
            checkMethodArgsForUpcast(params, args, node);

        } else if (node.getKind() == Kind.NEW_ARRAY) {
            // There is nothing to check for NEW_ARRAY. -
            // the result will be assigned - which will be checked later.
            return;
        } else if (node.getKind() == Kind.RETURN) {
            ReturnTree tree = (ReturnTree) node;
            MethodTree enclosingMethod = TreeUtils
                    .enclosingMethod(getCurrentPath());
            ExecutableElement m = TreeUtils
                    .elementFromDeclaration(enclosingMethod);
            if (m.getReturnType().getKind().isPrimitive())
                return;
            if (escapeUpcastOnReturn(node))
                return;
            TypeMirror cT = m.getReturnType();
            TypeMirror eT = InternalUtils.typeOf(tree.getExpression());
            checkUpcastTypes(cT, eT, node);

        } else
            throw new RuntimeException("Unexpected assignment AST node: "
                    + node.getKind());
    }

    private void checkMethodArgsForUpcast(
            List<? extends VariableElement> params,
            List<? extends ExpressionTree> args, Tree node) {
        for (int i = 0; i < args.size(); i++) {
            TypeMirror cT = params.get(i).asType();
            TypeMirror eT = InternalUtils.typeOf(args.get(i));

            checkUpcastTypes(cT, eT, node);
        }
    }

    private void checkUpcastTypes(TypeMirror castType, TypeMirror exprType,
            Tree node) {

        castType = Utils.getBaseType(castType);
        exprType = Utils.getBaseType(exprType);

        if (castType.getKind().isPrimitive() || exprType.getKind().isPrimitive())
            return;

        if (exprType.toString().equals("<nulltype>"))
            return;

        if (castType.toString().equals(exprType.toString()))
            return;

        if (!ctx.isSafeUpcast(exprType, castType))
            fail(ERR_BAD_UPCAST, node, exprType.toString(), castType.toString());
    }

    /**
     * Determine the corner cases that are safe to UPCAST.
     *
     * @return
     */
    private boolean escapeUpcastOnInvoke(MethodInvocationTree tree) {
        // executeInArea and enterPrivateMemory can upcast as they want:
        switch (SCJMethod.fromMethod(TreeUtils.elementFromUse(tree), elements,
                types)) {
        case ENTER_PRIVATE_MEMORY:
        case EXECUTE_IN_AREA:
            return true;
        default:
            return false;
        }
    }

    /**
     * This method deals with the special cases where UPCAST is allowed no
     * matter what. This is defined by the SCJ SPEC.
     *
     * Example:
     *
     * (i) The value returned from MissionSequencer.getNextMission() is upcast
     * to Mission.
     *
     */
    private boolean escapeUpcastOnReturn(Tree node) {
        // the specific upcasts appear only in return statements.
        if (node.getKind() != Kind.RETURN)
            return false;

        switch (SCJSafeUpcastDefinition.fromMethod(getEnclosingMethod(),
                elements, types)) {
        case GET_NEXT_MISSION:
        case SAFELET_GET_SEQUENCER:
        case MSEQ_GET_INSTANCE:
            return true;
        default:
            return false;
        }
    }

    private ExecutableElement getEnclosingMethod() {
        MethodTree enclosingMethod = TreeUtils
                .enclosingMethod(getCurrentPath());
        ExecutableElement m = TreeUtils.elementFromDeclaration(enclosingMethod);
        return m;
    }

    @Override
    public ScopeInfo visitBinary(BinaryTree node, P p) {
        super.visitBinary(node, p);
        if (TreeUtils.isCompileTimeString(node))
            return ScopeInfo.IMMORTAL;
        else if (TreeUtils.isStringConcatenation(node))
            return ScopeInfo.CALLER;
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
        debugIndent("Seen class " + t.getQualifiedName() + ": @Scope(" + scope
                + ")");

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
            ScopeInfo current = currentScope();
            if (!lhs.equals(current)) {
                fail(ERR_BAD_ASSIGNMENT_SCOPE, node, current, lhs);
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
        if (t != null) {
            if (!t.equals(f)) {
                fail(ERR_BAD_ASSIGNMENT_SCOPE, node, t, f);
            }
            ret = t;
        }
        debugIndentDecrement();
        return ret;
    }

    @Override
    public ScopeInfo visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        // Not sure if this needs to be checked. This implicitly does
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

        TypeMirror type = Utils.getBaseType(elem.asType());

        if (elem.getKind() == ElementKind.FIELD && !isThis(node)) {
            // when accessing this.method(), then this is type of FIELD, but
            // we need to handle the this case specially.

            VariableElement v = (VariableElement) elem;
            ScopeInfo scope = ctx.getFieldScope(v);
            ScopeInfo defineScope = null;
            ScopeInfo receiver;

            if (!type.getKind().isPrimitive()
                    && needsDefineScope(Utils.getTypeElement(type)))
                defineScope = ctx.getFieldScope(v).getRepresentedScope();

            // Since the receiver is implicit, we need to figure out whether
            // or not the field is static or not and set the scope accordingly.
            if (Utils.isStatic(v))
                receiver = ScopeInfo.IMMORTAL;
            else
                receiver = varScopes.getVariableScope("this");

            ret = new FieldScopeInfo(receiver, scope, defineScope);
        } else if (elem.getKind() == ElementKind.LOCAL_VARIABLE
                || elem.getKind() == ElementKind.PARAMETER) {
            ret = varScopes.getVariableScope(node.getName().toString());
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
        } else if (elem.getKind() == ElementKind.ENUM
                || elem.getKind() == ElementKind.ENUM_CONSTANT) {
            // NOTE: java.lang.Enum has @Scope(IMMORTAL)
            ret = ScopeInfo.IMMORTAL;
        } else {
            throw new RuntimeException("Unexpected assignment AST node: "
                    + elem.getKind());
        }

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

        ScopeInfo ret = ScopeInfo.PRIMITIVE;
        if (node.getValue() == null) {
            ret = ScopeInfo.NULL;
        } else if (node.getValue() instanceof String) {
            // STRING LITERALS - instead of making them IMMORTAL, we infer
            // their scope to be the "current scope".
            debugIndent("  string literal, currentScope: " + currentScope());
            ret = currentScope();
        }
        debugIndentDecrement();
        return ret;
    }

    @Override
    public ScopeInfo visitMemberSelect(MemberSelectTree node, P p) {
        debugIndentIncrement("visitMemberSelect: " + node.toString());

        Element elem = TreeUtils.elementFromUse(node);
        ScopeInfo receiver = node.getExpression().accept(this, p);
        ScopeInfo ret;

        if (elem.getKind() == ElementKind.METHOD) {
            // If a MemberSelectTree is not a field, then it is a method
            // that is part of a MethodInvocationTree. In this case, we
            // want to return the scope of the receiver object so that
            // visitMethodInvocation has its scope.
            ret = node.getExpression().accept(this, p);
        } else if (elem.getKind() == ElementKind.CLASS) {
            ScopeInfo scope = ctx.getClassScope((TypeElement) elem);
            ret = scope;
        } else if (elem.getKind() == ElementKind.ENUM) {
            // NOTE: java.lang.Enum is IMMORTAL
            ret = ScopeInfo.IMMORTAL;
        } else {
            VariableElement f = (VariableElement) elem;
            if (f.toString().equals("this")) {
                // handling the case when the field is "Class.this"
                ret = varScopes.getVariableScope("this");
            } else {
                ScopeInfo fScope = ctx.getFieldScope(f);
                ret = new FieldScopeInfo(receiver, fScope);
            }
        }
        debugIndentDecrement();
        return ret;
    }

    public ScopeInfo getEnclosingClassScope(Element elem) {
        ScopeInfo scope = ScopeInfo.CALLER;
        while (elem != null) {
            scope = ctx.getClassScope(elem.toString());
            if (!scope.isCaller())
                break;
            elem = elem.getEnclosingElement();
        }
        return scope;
    }

    @Override
    public ScopeInfo visitMethod(MethodTree node, P p) {
        debugIndentIncrement("visitMethod " + node.getName());
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);

        ScopeInfo oldRunsIn = currentRunsIn;
        ScopeInfo runsIn = ctx.getEffectiveMethodRunsIn(m, currentScope(),
                ScopeInfo.CALLER);

        currentRunsIn = runsIn;
        varScopes.pushBlock();
        List<? extends VariableTree> params = node.getParameters();
        List<ScopeInfo> paramScopes = ctx.getParameterScopes(m);
        for (int i = 0; i < paramScopes.size(); i++) {
            VariableTree param = params.get(i);
            String paramName = param.getName().toString();
            debugIndent(" add VarScope: " + paramName + ", scope:"
                    + paramScopes.get(i));
            varScopes.addVariableScope(paramName, paramScopes.get(i));
        }

        // if this is not an abstract method, visit it:
        if (node.getBody() != null)
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
            if (!(scope.isCaller() || scopeTree.isAncestorOf(currentScope(),
                    scope)))
                fail(ERR_BAD_ALLOCATION_ARRAY, node, scope);
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
                && !nodeClassScope.isCaller()) {
            // Can't call new unless the type has the same scope as the
            // current context
            fail(ERR_BAD_ALLOCATION, node, currentScope(), nodeClassScope);
        }

        checkUpcast(node);

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

        ExecutableElement m = TreeUtils.elementFromDeclaration(enclosingMethod);
        ScopeInfo returnScope = ctx.getMethodScope(m);
        ScopeInfo exprScope = node.getExpression().accept(this, p);

        checkReturnScope(exprScope, returnScope, node);
        checkUpcast(node);

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
            return ScopeInfo.PRIMITIVE;
        }

        ScopeInfo scope = node.getExpression().accept(this, p);
        TypeMirror m = Utils.getBaseType(InternalUtils.typeOf(node));
        ScopeInfo cast = null;
        if (m.getKind().isPrimitive()) {
            // if we are casting to a primitive type, we take on the ScopeInfo
            // of the rhs expresion
            // e.g. for: (byte []) MemoryArea.newArayInArea(...)
            cast = scope;
        } else {
            cast = ctx.getClassScope(Utils.getTypeElement(m));
            checkUpcast(node);
        }

        debugIndentDecrement();
        return cast.isCaller() ? scope : cast;
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
            varScopes.addVariableScope(node.getName().toString(),
                    ScopeInfo.PRIMITIVE);
            return null;
        }

        ScopeInfo lhs = checkVariableScope(node);
        debugIndent("variable's scope: " + lhs);
        varScopes.addVariableScope(node.getName().toString(), lhs);
        VariableElement var = TreeUtils.elementFromDeclaration(node);

        // Static variable, change the context to IMMORTAL
        if (Utils.isStatic(var))
            currentRunsIn = ScopeInfo.IMMORTAL;

        if (node.getInitializer() != null) {
            ScopeInfo rhs = node.getInitializer().accept(this, p);
            checkAssignment(lhs, rhs, node);
            checkUpcast(node);
        }

        currentRunsIn = oldRunsIn;
        debugIndentDecrement();
        return null;
    }

    private boolean isArrayAssignment(Tree node) {
        if (node.getKind() != Kind.ASSIGNMENT)
            return false;
        AssignmentTree at = (AssignmentTree) node;
        return at.getVariable().getKind() == Kind.ARRAY_ACCESS;
    }

    private void checkArrayAssignment(ScopeInfo lhs, ScopeInfo rhs, Tree node) {
        ExpressionTree lhsTree = ((AssignmentTree) node).getVariable();
        TypeMirror m = InternalUtils.typeOf(lhsTree);
        TypeKind k = m.getKind();

        lhs = concretize(lhs);
        rhs = concretize(rhs);

        if (k.isPrimitive() || rhs.isNull())
            return;
        else if (k == TypeKind.ARRAY) {
            // Nested arrays in a "multi-dimensional" array must be in the same
            // scope.
            if (!lhs.isUnknown()) {
                if (!lhs.equals(rhs))
                    fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
            } else {
                // This dynamic guard requires that the lhs array be stored in a
                // final local.
                String lhsVar = getLhsVariableNameFromArrayAssignment(node);
                String rhsVar = getRhsVariableNameFromAssignment(node);
                if (!varScopes.hasSameRelation(lhsVar, rhsVar))
                    fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
            }
        } else if (k == TypeKind.DECLARED) {
            ScopeInfo classScope = ctx.getClassScope(Utils.getTypeElement(m));
            if (classScope.isCaller()) {
                if (!lhs.isUnknown()) {
                    if (!lhs.equals(rhs))
                        fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
                } else {
                    // This dynamic guard requires that the lhs array be stored
                    // in a final local.
                    String lhsVar = getLhsVariableNameFromArrayAssignment(node);
                    String rhsVar = getRhsVariableNameFromAssignment(node);
                    if (!varScopes.hasSameRelation(lhsVar, rhsVar))
                        fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
                }
            }
        } else
            throw new RuntimeException("missing case");
    }

    private void checkAssignment(ScopeInfo lhs, ScopeInfo rhs, Tree node) {
        debugIndentIncrement("checkAssignment: " + node.toString());

        if (lhs.isFieldScope())
            checkFieldAssignment((FieldScopeInfo) lhs, rhs, node);
        else if (isArrayAssignment(node))
            checkArrayAssignment(lhs, rhs, node);
        else
            checkLocalAssignment(lhs, rhs, node);
        debugIndentDecrement();
    }

    private ScopeInfo checkDefineScopeOnVariable(VariableElement var,
            ScopeInfo varScope, VariableTree node) {
        debugIndent("checkDefineScopeOnVariable.");

        if (!Utils.isUserLevel(var))
            return varScope;
        if (var.asType().getKind() != TypeKind.DECLARED)
            return varScope;

        DefineScope ds = var.getAnnotation(DefineScope.class);
        if (ds == null) {
            fail(ERR_MEMORY_AREA_NO_DEFINE_SCOPE_ON_VAR, node);
            return varScope.representing(ScopeInfo.INVALID);
        }

        ScopeInfo scope = new ScopeInfo(ds.name());
        ScopeInfo parent = new ScopeInfo(ds.parent());

        if (!scopeTree.hasScope(scope) || !scopeTree.isParentOf(scope, parent))
            fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT, node);

        varScope = concretize(varScope);
        if (!varScope.equals(parent))
            fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE, node,
                    varScope, parent);

        return varScope.representing(scope);
    }

    private void checkFieldAssignment(FieldScopeInfo lhs, ScopeInfo rhs,
            Tree node) {
        debugIndentIncrement("checkFieldAssignment");

        rhs = concretize(rhs);

        String rhsVar = getRhsVariableNameFromAssignment(node);
        String lhsVar = getLhsVariableNameFromAssignment(node);

        if (!rhs.isNull()) {
            if (!lhs.isUnknown()) {
                if (lhs.getFieldScope().isThis()) {    // no check for THIS?
                    if (!lhs.getReceiverScope().equals(rhs)
                            && !varScopes.hasSameRelation(lhsVar, rhsVar))
                        fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
                } else if (!lhs.getFieldScope().equals(rhs))
                    fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
            } else {
                if (lhs.getFieldScope().isThis()) {
                    if (!varScopes.hasSameRelation(lhsVar, rhsVar))
                        fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
                } else if (!varScopes.hasParentRelation(lhsVar, rhsVar))
                    fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
            }
        }

        ScopeInfo rhsDsi = rhs.getRepresentedScope();
        if (rhsDsi != null && !rhsDsi.equals(lhs.getRepresentedScope())) {
            fail(ERR_BAD_ALLOCATION_CONTEXT_ASSIGNMENT, node);
        }
        debugIndentDecrement();
    }

    private String getLhsVariableNameFromArrayAssignment(Tree node) {
        if (node.getKind() == Kind.ASSIGNMENT) {
            AssignmentTree tree = (AssignmentTree) node;
            ExpressionTree lhs = Utils.getBaseTree(tree.getVariable());

            if (lhs.getKind() == Kind.IDENTIFIER)
                return lhs.toString();
            return null;
        } else
            throw new RuntimeException("Unexpected assignment AST node: "
                    + node.getKind());
    }

    private String getLhsVariableNameFromAssignment(Tree node) {
        if (node.getKind() == Kind.ASSIGNMENT) {
            AssignmentTree tree = (AssignmentTree) node;
            ExpressionTree lhs = tree.getVariable();

            if (lhs.getKind() == Kind.MEMBER_SELECT) {
                MemberSelectTree mst = (MemberSelectTree) lhs;
                if (mst.getExpression().getKind() == Kind.IDENTIFIER)
                    return mst.getExpression().toString();
            } else {
                // If we don't see a member select, we have an implicit this
                return "this";
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
        ScopeInfo rhsDsi = rhs.getRepresentedScope();
        if (rhsDsi != null && !rhsDsi.equals(lhs.getRepresentedScope()))
            fail(ERR_BAD_ALLOCATION_CONTEXT_ASSIGNMENT, node);
    }

    private ScopeInfo concretize(ScopeInfo scope) {
        if (scope.isCaller())
            return currentScope();
        if (scope.isThis())
            return varScopes.getVariableScope("this");
        return scope;
    }

    private void checkEnterPrivateMemory(ScopeInfo recvScope,
            MethodInvocationTree node) {
        ScopeInfo target = recvScope.getRepresentedScope();
        ExpressionTree arg = node.getArguments().get(1);
        ScopeInfo argRunsIn = getRunsInFromRunnable(arg);

        if (!scopeTree.isParentOf(argRunsIn, currentScope())) {
            fail(ERR_BAD_ENTER_PRIVATE_MEMORY_RUNS_IN_NO_MATCH, node,
                    argRunsIn, currentScope());
            return;
        }

        if (!scopeTree.isParentOf(argRunsIn, target)) {
            // TODO: target may be null??
            fail(ERR_BAD_ENTER_PRIVATE_MEMORY_TARGET, node, argRunsIn, target);
        }
    }

    private void checkExecuteInArea(ScopeInfo recvScope,
            MethodInvocationTree node) {
        ScopeInfo target = recvScope.getRepresentedScope();
        ExpressionTree arg = node.getArguments().get(0);
        ScopeInfo argRunsIn = getRunsInFromRunnable(arg);

        if (!scopeTree.isAncestorOf(currentScope(), target))
            // the executeInArea must target an Ancestor scope
            fail(ERR_BAD_EXECUTE_IN_AREA_TARGET, node, currentScope(), target);

        if (!target.equals(argRunsIn))
            // target and @RunsIn on runnable must be the same
            fail(ERR_BAD_EXECUTE_IN_AREA_RUNS_IN, node, target, argRunsIn);
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

        SCJMethod sig = SCJMethod.fromMethod(m, elements, types);
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
        debugIndent("\n\t checkMethodInvocation : " + node);

        ScopeInfo runsIn = ctx.getEffectiveMethodRunsIn(m, recvScope,
                currentScope());
        checkMethodRunsIn(m, runsIn, node);
        checkMethodParameters(m, argScopes, node);

        switch (SCJMethod.fromMethod(m, elements, types)) {
        case ENTER_PRIVATE_MEMORY:
            if (isCallerContext(currentScope(), node))
                checkEnterPrivateMemory(recvScope, node);
            return null; // void methods don't return a scope
        case EXECUTE_IN_AREA:
            if (isCallerContext(currentScope(), node))
                checkExecuteInArea(recvScope, node);
            return null;
        case NEW_INSTANCE:
            return checkNewInstance(recvScope, node.getArguments().get(0), node);
        case NEW_INSTANCE_IN_AREA:
            return checkNewInstanceInArea(argScopes.get(0), node);
        case NEW_ARRAY:
            return checkNewArray(recvScope, node.getArguments().get(0), node);
        case NEW_ARRAY_IN_AREA:
            return checkNewArrayInArea(argScopes.get(0), node);
        case GET_MEMORY_AREA:
            return checkGetMemoryArea(argScopes.get(0), node);
        case GET_CURRENT_MEMORY_AREA:
        case GET_CURRENT_MANAGED_MEMORY:
            return checkGetCurrentManagedMemory(node);
        case IMMORTAL_MEMORY_INSTANCE:
            return ScopeInfo.IMMORTAL.representing(ScopeInfo.IMMORTAL);
        default:
            return concretize(ctx.getEffectiveMethodScope(m, recvScope,
                    currentScope()));
        }
    }

    private boolean isCallerContext(ScopeInfo currentScope,
            MethodInvocationTree node) {
        if (currentScope.isCaller()) {
            fail(ERR_BAD_CONTEXT_CHANGE_CALLER, node);
            return false;
        }
        return true;
    }

    private void checkMethodParameters(ExecutableElement m,
            List<ScopeInfo> argScopes, MethodInvocationTree node) {

        List<ScopeInfo> paramScopes = ctx.getParameterScopes(m);
        for (int i = 0; i < paramScopes.size(); i++)
            checkLocalAssignment(paramScopes.get(i), argScopes.get(i), node);

        checkUpcast(node);
    }

    /**
     * Check to see if a method is invokable in the current allocation context.
     *
     * Since this method is passed the effective RunsIn of the method being
     * tested, there is no need to look at the scope of the receiver object.
     *
     * @see ScopeCheckerContext#getEffectiveMethodRunsIn(ExecutableElement,
     *      ScopeInfo)
     *
     * @param m
     *            the element representing the method invocation
     * @param effectiveRunsIn
     *            the effective scope in which the method runs
     * @param node
     *            method invocation tree
     */
    private void checkMethodRunsIn(ExecutableElement m,
            ScopeInfo effectiveRunsIn, MethodInvocationTree node) {
        if (currentScope().isCaller() && !effectiveRunsIn.isCaller())
            fail(ERR_BAD_METHOD_INVOKE, node, effectiveRunsIn, CALLER);
        else if (!effectiveRunsIn.isCaller()
                && !effectiveRunsIn.equals(currentScope()))
            fail(ERR_BAD_METHOD_INVOKE, node, effectiveRunsIn, currentScope());
    }

    private ScopeInfo checkNewInstance(ScopeInfo recvScope, ExpressionTree arg,
            MethodInvocationTree node) {
        TypeMirror instType = getNewInstanceType(arg);
        ScopeInfo target = recvScope.getRepresentedScope();

        if (isValidNewInstanceType(instType)) {
            ScopeInfo argScope = ctx.getClassScope(instType.toString());
            if (!(argScope.isCaller() || argScope.equals(target)))
                fail(ERR_BAD_NEW_INSTANCE, node, argScope, target);
            if (!scopeTree.isAncestorOf(currentScope(), target)) {
                fail(ERR_BAD_NEW_INSTANCE_REPRESENTED_SCOPE, node, currentScope(), target);
            }
        } else
            fail(ERR_BAD_NEW_INSTANCE_TYPE, node, instType);
        return target;
    }

    private boolean isValidNewInstanceType(TypeMirror m) {
        TypeKind k = m.getKind();

        if (k == TypeKind.ARRAY || k == TypeKind.WILDCARD
                || k == TypeKind.TYPEVAR || k.isPrimitive())
            return false;
        TypeElement t = Utils.getTypeElement(m);
        return !(t.getKind().isInterface() || Utils.isAbstract(t));
    }

    /**
     * Convert a newInstance object to its type.
     */
    private TypeMirror getNewInstanceType(ExpressionTree arg) {
        TypeMirror type = InternalUtils.typeOf(arg);
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType decl = (DeclaredType) type;
            return decl.getTypeArguments().get(0);
        }
        return null;
    }

    /**
     * @return parent scope of the current scope
     */
    private ScopeInfo checkGetCurrentManagedMemory(MethodInvocationTree node) {
        ScopeInfo current = currentScope();
        ScopeInfo parent = scopeTree.getParent(current);
        if (parent == null) {
            fail(ERR_BAD_GET_CURRENT_MANAGED_MEMORY, node);
            return ScopeInfo.INVALID.representing(current);
        }

        return parent.representing(current);
    }

    private ScopeInfo checkNewArray(ScopeInfo recvScope, ExpressionTree arg,
            MethodInvocationTree node) {
        TypeMirror instType = getNewInstanceType(arg);
        ScopeInfo target = recvScope.getRepresentedScope();

        if (isValidNewArrayType(instType)) {
            instType = Utils.getBaseType(instType);
            if (!instType.getKind().isPrimitive()) {
                ScopeInfo argScope = ctx.getClassScope(instType.toString());

                if (!(argScope.isCaller() || argScope.equals(target)))
                    fail(ERR_BAD_NEW_ARRAY, node, argScope, target);
            }
        } else
            fail(ERR_BAD_NEW_ARRAY_TYPE, node, instType);
        return target;
    }

    private boolean isValidNewArrayType(TypeMirror m) {
        TypeKind k = m.getKind();
        pln("\n type: " + k);
        return !(k == TypeKind.VOID || k == TypeKind.WILDCARD || k == TypeKind.TYPEVAR);
    }

    private ScopeInfo checkNewInstanceInArea(ScopeInfo scope,
            MethodInvocationTree node) {
        ScopeInfo target = checkGetMemoryArea(scope, node);
        return checkNewInstance(target, node.getArguments().get(1), node);
    }

    private ScopeInfo checkNewArrayInArea(ScopeInfo scope,
            MethodInvocationTree node) {
        ScopeInfo target = checkGetMemoryArea(scope, node);
        return checkNewArray(target, node.getArguments().get(1), node);
    }

    private ScopeInfo checkGetMemoryArea(ScopeInfo scope,
            MethodInvocationTree node) {

        scope = concretize(scope);
        if (scope.isUnknown() || scope.isCaller()) {
            // CALLER is also illegal if it can't be made into a concrete
            // scope name.
            fail(ERR_BAD_GET_MEMORY_AREA, node);
            return ScopeInfo.UNKNOWN;
        }
        if (scope.isImmortal())
            return new ScopeInfo(ScopeInfo.IMMORTAL.toString(),
                    ScopeInfo.IMMORTAL);

        ScopeInfo parent = scopeTree.getParent(scope);

        return new ScopeInfo(parent.getScope(), scope);
    }

    private void checkReturnScope(ScopeInfo exprScope, ScopeInfo expectedScope,
            ReturnTree node) {
        debugIndent("checkReturnScope");
        expectedScope = concretize(expectedScope);
        if (expectedScope.isUnknown() || expectedScope.equals(exprScope)
                || exprScope == null || exprScope.isNull())
            return;
        fail(ERR_BAD_RETURN_SCOPE, node, exprScope, expectedScope);
    }

    private ScopeInfo checkVariableScope(VariableTree node) {
        debugIndent("checkVariableScope");
        VariableElement v = TreeUtils.elementFromDeclaration(node);
        TypeMirror mv = v.asType();
        TypeMirror bmv = Utils.getBaseType(mv);
        ScopeInfo ret;

        ScopeInfo defaultScope = concretize(Utils.getDefaultVariableScope(v,
                ctx));
        if (!defaultScope.isReservedScope() && !scopeTree.isAncestorOf(currentScope(), defaultScope)) {
            // local variables can reference only the currentScope() or an ancestor scope:
            fail(ERR_BAD_VARIABLE_SCOPE, node, defaultScope, currentScope());
        }

        if (v.getKind() == ElementKind.FIELD) {
            ScopeInfo f = ctx.getFieldScope(v);
            return new FieldScopeInfo(currentScope(), f).representing(f
                    .getRepresentedScope());
        }

        if (Utils.isPrimitive(mv))
            ret = ScopeInfo.PRIMITIVE;
        else if (Utils.isPrimitiveArray(mv))
            ret = defaultScope;
        else if (bmv.getKind() == TypeKind.TYPEVAR)
            ret = defaultScope;
        else if (mv.getKind() == TypeKind.DECLARED) {
            ScopeInfo stv = ctx.getClassScope(Utils.getTypeElement(bmv));
            if (stv.isCaller())
                ret = defaultScope;
            else {
                if (!defaultScope.equals(stv))
                    fail(ERR_BAD_VARIABLE_SCOPE, node, mv, currentScope());
                ret = stv;
            }
            if (needsDefineScope(mv))
                ret = checkDefineScopeOnVariable(v, ret, node);
        } else if (mv.getKind() == TypeKind.ARRAY)
            ret = defaultScope;
        else
            throw new RuntimeException("missing case");

        return ret;
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
    private ScopeInfo getRunsInFromRunnable(ExpressionTree expressionTree) {
        TypeMirror runnableType = InternalUtils.typeOf(expressionTree);
        TypeElement t = Utils.getTypeElement(runnableType);
        return ctx.getMethodRunsIn(t.getQualifiedName().toString(), "run");
    }

    private boolean isThis(IdentifierTree node) {
        String s = node.getName().toString();
        return s.equals("this") || s.equals("super");
    }
}
