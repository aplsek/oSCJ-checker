package checkers.scope;

import static checkers.Utils.isFinal;
import static checkers.Utils.SCJ_METHODS.ALLOC_IN_PARENT;
import static checkers.Utils.SCJ_METHODS.ALLOC_IN_SAME;
import static checkers.Utils.SCJ_METHODS.DEFAULT;
import static checkers.Utils.SCJ_METHODS.ENTER_PRIVATE_MEMORY;
import static checkers.Utils.SCJ_METHODS.EXECUTE_IN_AREA;
import static checkers.Utils.SCJ_METHODS.GET_CURRENT_MANAGED_MEMORY;
import static checkers.Utils.SCJ_METHODS.GET_MEMORY_AREA;
import static checkers.Utils.SCJ_METHODS.NEW_ARRAY;
import static checkers.Utils.SCJ_METHODS.NEW_ARRAY_IN_AREA;
import static checkers.Utils.SCJ_METHODS.NEW_INSTANCE;
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
import static checkers.scope.ScopeChecker.ERR_BAD_GUARD_NO_FINAL;
import static checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE;
import static checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE;
import static checkers.scope.ScopeChecker.ERR_BAD_RETURN_SCOPE;
import static checkers.scope.ScopeChecker.ERR_BAD_VARIABLE_SCOPE;
import static checkers.scope.ScopeChecker.ERR_DEFAULT_BAD_ENTER_PARAMETER;
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
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.Scope;

import checkers.SCJVisitor;
import checkers.Utils;
import checkers.Utils.SCJ_METHODS;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
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
    private ScopeInfo currentScope = null;
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
        try {
            debugIndentIncrement("visitAssignment : " + node);

            ScopeInfo lhs = node.getVariable().accept(this, p);
            ScopeInfo rhs = node.getExpression().accept(this, p);

            debugIndent("> lhs : " + lhs.getScope());
            debugIndent("> rhs : " + rhs.getScope());

            if (lhs.equals(rhs) && !lhs.isUnknown()) {
                return lhs;
            }
            checkAssignment(lhs, rhs, node);
            return lhs;
        } finally {
            debugIndentDecrement();
        }
    }

    @Override
    public ScopeInfo visitBinary(BinaryTree node, P p) {
        super.visitBinary(node, p);
        if (TreeUtils.isCompileTimeString(node)) {
            return ScopeInfo.CURRENT;
        } else if (TreeUtils.isStringConcatenation(node)) {
            return ScopeInfo.CURRENT;
        }
        return null; // Primitive expressions have no scope
    }

    @Override
    public ScopeInfo visitBlock(BlockTree node, P p) {
        debugIndentIncrement("visitBlock");
        ScopeInfo oldRunsIn = currentRunsIn;
        if (node.isStatic()) {
            currentRunsIn = ScopeInfo.IMMORTAL;
        }
        varScopes.pushBlock();
        super.visitBlock(node, p);
        varScopes.popBlock();
        currentRunsIn = oldRunsIn;
        debugIndentDecrement();
        return null;
    }


    void pln(String str) {System.err.println(str);}

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
        ScopeInfo oldScope = currentScope;
        ScopeInfo oldRunsIn = currentRunsIn;

        try {
            ScopeInfo scope = ctx.getClassScope(t);
            varScopes.pushBlock();
            varScopes.addVariableScope("this", scope);

            // TODO: assume defaults for inner classes?
            Utils.debugPrintln("Seen class " + t.getQualifiedName()
                    + ": @Scope(" + scope + ")");

            currentScope = scope;
            currentRunsIn = scope;

            super.visitClass(node, p);
            varScopes.popBlock();
            return null;
        } finally {
            currentScope = oldScope;
            currentRunsIn = oldRunsIn;
            debugIndentDecrement();
        }
    }

    @Override
    public ScopeInfo visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        debugIndentIncrement("visitCompoundAssignment : " + node);
        try {
            ScopeInfo lhs = node.getVariable().accept(this, p);
            if (TreeUtils.isStringCompoundConcatenation(node)) {
                if (!lhs.isCurrent()) {
                    // TODO: report error
                }
                return lhs;
            }
            return null; // Primitives have no scope
        } finally {
            debugIndentDecrement();
        }
    }

    @Override
    public ScopeInfo visitConditionalExpression(ConditionalExpressionTree node,
            P p) {
        debugIndentIncrement("visitConditionalExpression : " + node);

        try {
            node.getCondition().accept(this, p);
            ScopeInfo t = node.getTrueExpression().accept(this, p);
            ScopeInfo f = node.getFalseExpression().accept(this, p);
            // TODO: Not sure if this is right
            if (t != null) {
                if (!t.equals(f)) {
                    // TODO: report error
                }
                return t;
            }
            return null; // Primitives have no scope
        } finally {
            debugIndentDecrement();
        }
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
        try {
            Element elem = TreeUtils.elementFromUse(node);

            // when accessing this.method(), then this is type of FIELD, but
            // we need to handle the this case specially.
            if (elem.getKind() == ElementKind.FIELD && !isThis(node)) {
                ScopeInfo scope = ctx.getFieldScope((VariableElement) elem);
                debugIndent("\t elem:" + elem.getEnclosingElement());
                debugIndent("\t node:" + node.getName().toString());
                debugIndent("\t FIELD scope :" + scope.getScope());
                return new FieldScopeInfo(varScopes.getVariableScope("this"),
                        scope);
            } else if (elem.getKind() == ElementKind.LOCAL_VARIABLE
                    || elem.getKind() == ElementKind.PARAMETER) {
                String var = node.getName().toString();
                ScopeInfo scope = varScopes.getVariableScope(var);
                debugIndent("\t varScope:" + scope.getScope());
                return scope;
            } else if (elem.getKind() == ElementKind.METHOD
                    || elem.getKind() == ElementKind.CONSTRUCTOR
                    || (elem.getKind() == ElementKind.FIELD && isThis(node))) {
                // If an identifier gets visited and its element is a method,
                // then
                // it is part of a MethodInvocationTree as the method select.
                // It's
                // either a static method, in which case there is no receiver
                // object, or it's an instance method invoked on the current
                // object, in which case it is implicitly invoked on "this". We
                // return the scope of "this", which will be discarded if the
                // method being invoked is static.
                ScopeInfo scope = varScopes.getVariableScope("this");
                debugIndent("\t method/constructor scope:" + scope);
                return scope;
            }

            debugIndent("\t identifier scope [NO CASE]:" + null);
            return super.visitIdentifier(node, p);
        } finally {
            debugIndentDecrement();
        }
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
        if (elseBlock != null) {
            elseBlock.accept(this, p);
        }
        return null;
    }

    @Override
    public ScopeInfo visitLiteral(LiteralTree node, P p) {
        debugIndentIncrement("visitLiteral : " + node);
        debugIndent(" node's value : " + node.getValue());
        try {
            if (node.getValue() == null) {
                return ScopeInfo.NULL;
            } else if (node.getValue() instanceof String) {
                // TODO I foresee much sadness in this later on. Strings can't
                // really interact between IMMORTAL and other scopes if it's not
                // RunsIn(UNKNOWN).
                return ScopeInfo.IMMORTAL;
            }
            return null;
        } finally {
            debugIndentDecrement();
        }
    }

    @Override
    public ScopeInfo visitMemberSelect(MemberSelectTree node, P p) {
        Element elem = TreeUtils.elementFromUse(node);
        ScopeInfo receiver = node.getExpression().accept(this, p);

        try {
            debugIndentIncrement("visitMemberSelect: " + node.toString());
            // TODO: Does Class.this work?
            if (elem.getKind() == ElementKind.METHOD) {
                // If a MemberSelectTree is not a field, then it is a method
                // that is part of a MethodInvocationTree. In this case, we
                // want to return the scope of the receiver object so that
                // visitMethodInvocation has its scope.
                return node.getExpression().accept(this, p);
            }
            VariableElement f = (VariableElement) elem;
            ScopeInfo fScope = ctx.getFieldScope(f);
            return new FieldScopeInfo(receiver, fScope);
        } finally {
            debugIndentDecrement();
        }
    }

    @Override
    public ScopeInfo visitMethod(MethodTree node, P p) {
        debugIndentIncrement("visitMethod " + node.getName());
        ExecutableElement method = TreeUtils.elementFromDeclaration(node);

        debugIndent("Seen method " + method.getSimpleName());
        debugIndent("RunsIn: " + currentRunsIn);
        debugIndent("Scope: " + currentScope);

        ScopeInfo oldRunsIn = currentRunsIn;
        try {
            ScopeInfo runsIn = ctx.getEffectiveMethodRunsIn(method,
                    currentScope());
            debugIndent("@RunsIn(" + runsIn + ") " + method.getSimpleName());

            if (runsIn != null) {
                currentRunsIn = runsIn;
            }
            varScopes.pushBlock();
            List<? extends VariableTree> params = node.getParameters();
            List<ScopeInfo> paramScopes = ctx.getParameterScopes(method);
            for (int i = 0; i < paramScopes.size(); i++) {
                debugIndent(" add VarScope: "
                        + params.get(i).getName().toString() + ", scope:"
                        + paramScopes.get(i));
                Tree nodeTypeTree = getArrayTypeTree(params.get(i).getType());
                // skipping the primitive variables.
                if (nodeTypeTree.getKind() != Kind.PRIMITIVE_TYPE)
                    varScopes.addVariableScope(params.get(i).getName()
                            .toString(), paramScopes.get(i));
            }
            node.getBody().accept(this, p);
            // TODO: make sure we don't need to visit more
            varScopes.popBlock();
            return null;
        } finally {
            currentRunsIn = oldRunsIn;
            debugIndentDecrement();
        }
    }

    @Override
    public ScopeInfo visitMethodInvocation(MethodInvocationTree node, P p) {
        debugIndentIncrement("visitMethodInvocation : " + node);
        List<ScopeInfo> argScopes = new ArrayList<ScopeInfo>(node.getArguments().size());
        for (ExpressionTree arg : node.getArguments())
            argScopes.add(arg.accept(this, p));

        ScopeInfo recvScope = node.getMethodSelect().accept(this, p);
        debugIndent("recvScope : " + recvScope);

        debugIndentDecrement();
        return checkMethodInvocation(TreeUtils.elementFromUse(node), recvScope, argScopes,
                node);
    }

    @Override
    public ScopeInfo visitNewArray(NewArrayTree node, P p) {
        try {
            debugIndentIncrement("visitNewArray");
            TypeMirror arrayType = InternalUtils.typeOf(node);
            TypeMirror componentType = Utils.getBaseType(arrayType);
            if (!componentType.getKind().isPrimitive()) {
                TypeElement t = Utils.getTypeElement(componentType);
                ScopeInfo scope = ctx.getClassScope(t);
                if (!(scope.isCurrent() || scope.equals(currentScope()))) {
                    fail(ERR_BAD_ALLOCATION, node, currentScope(), scope);
                }
            }
            super.visitNewArray(node, p);
            return currentScope();
        } finally {
            debugIndentDecrement();
        }
    }

    /**
     * Object allocation is only allowed if the current allocation context is
     * the same scope as what's defined by the class.
     */
    @Override
    public ScopeInfo visitNewClass(NewClassTree node, P p) {
        try {
            debugIndentIncrement("visitNewClass");
            ExecutableElement ctorElement = TreeUtils.elementFromUse(node);
            ScopeInfo nodeClassScope = ctx.getClassScope(Utils
                    .getMethodClass(ctorElement));
            if (nodeClassScope != null
                    && !currentScope().equals(nodeClassScope)
                    && !nodeClassScope.isCurrent()) {
                // Can't call new unless the type has the same scope as the
                // current context
                fail(ERR_BAD_ALLOCATION, node, currentScope(), nodeClassScope);
            }
            super.visitNewClass(node, p);
            return currentScope();
        } finally {
            debugIndentDecrement();
        }
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
        Tree nodeTypeTree = getArrayTypeTree(enclosingMethod.getReturnType());
        if (nodeTypeTree.getKind() == Kind.PRIMITIVE_TYPE) {
            debugIndent(" Returns primitive value. Stop visiting. Return null as ScopeInfo.");
            return null;
        }

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
    public ScopeInfo visitTypeCast(TypeCastTree node, P p) {
        debugIndentIncrement("visitTypeCast " + node);
        if (isPrimitiveExpression(node))
            return null;

        ScopeInfo scope = node.getExpression().accept(this, p);
        TypeMirror m =  Utils.getBaseType(InternalUtils.typeOf(node));
        ScopeInfo cast = ctx.getClassScope(Utils.getTypeElement(m));

        //TODO: compare cast and scope
        // TODO: call super typecast!
        // see the type system
        // unannotated ->

        debugIndentDecrement();
        return cast.isCurrent() ? scope : cast;
    }

    /**
     * <ul>
     * <li>Static variables must always be in the immortal scope.
     * <li>Instance variables must make sure that the scope of the enclosing
     * class is a child scope of the variable type's scope.
     * <li>Local variables are similar to instance variables, only it must first
     * use the @RunsIn annotation, if any exists, before using the
     *
     * @Scope annotation on the class that it belongs to.
     *        </ul>
     */
    @Override
    public ScopeInfo visitVariable(VariableTree node, P p) {
        debugIndentIncrement("visitVariable : " + node.toString());
        ScopeInfo oldRunsIn = currentRunsIn;

        if (getArrayTypeTree(node.getType()).getKind() == Kind.PRIMITIVE_TYPE)
            return null;

        ScopeInfo lhs = checkVariableScope(node);
        varScopes.addVariableScope(node.getName().toString(), lhs);

        // Static variable, change the context to IMMORTAL
        if (Utils.isStatic(node.getModifiers().getFlags()))
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
        if (lhs.isFieldScope()) {
            checkFieldAssignment((FieldScopeInfo) lhs, rhs, node);
        } else {
            // TODO: Do we need an extra case for arrays?
            checkLocalAssignment(lhs, rhs, node);
        }
        debugIndentDecrement();
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
                if (!lhs.getReceiverScope().equals(rhs)) {
                    fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
                }
            } else if (!lhs.getFieldScope().equals(rhs)) {
                fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
            }
        } else {
            ScopeInfo fScope = lhs.getFieldScope();
            String rhsVar = getRhsVariableNameFromAssignment(node);
            String lhsVar = getLhsVariableNameFromAssignment(node);
            if (fScope.isCurrent()) {
                if (!varScopes.hasSameRelation(lhsVar, rhsVar)) {
                    fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
                }
            } else {
                if (!varScopes.hasParentRelation(lhsVar, rhsVar)) {
                    fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
                }
            }
        }
        debugIndentDecrement();
    }

    private String getLhsVariableNameFromAssignment(Tree node) {
        if (node.getKind() == Kind.ASSIGNMENT) {
            AssignmentTree tree = (AssignmentTree) node;
            ExpressionTree lhs = tree.getVariable();

            if (lhs.getKind() == Kind.MEMBER_SELECT) {
                MemberSelectTree mst = (MemberSelectTree) lhs;
                if (mst.getExpression().getKind() == Kind.IDENTIFIER) {
                    return mst.getExpression().toString();
                }
            }
            return null;
        } else if (node.getKind() == Kind.VARIABLE) {
            return "this";
        } else {
            throw new RuntimeException("Unexpected assignment AST node: "
                    + node.getKind());
        }
    }

    private String getRhsVariableNameFromAssignment(Tree node) {
        ExpressionTree rhs;
        if (node.getKind() == Kind.ASSIGNMENT) {
            AssignmentTree tree = (AssignmentTree) node;
            rhs = tree.getExpression();
        } else if (node.getKind() == Kind.VARIABLE) {
            VariableTree tree = (VariableTree) node;
            rhs = tree.getInitializer();
        } else {
            throw new RuntimeException("Unexpected assignment AST node: "
                    + node.getKind());
        }
        if (rhs.getKind() == Kind.IDENTIFIER) {
            return ((IdentifierTree) rhs).toString();
        }
        return null;
    }

    private void checkLocalAssignment(ScopeInfo lhs, ScopeInfo rhs, Tree node) {
        if (lhs.isUnknown() || rhs.isNull()) {
            return;
        }
        if (!concretize(lhs).equals(concretize(rhs))) {
            fail(ERR_BAD_ASSIGNMENT_SCOPE, node, rhs, lhs);
        }
    }

    private ScopeInfo concretize(ScopeInfo scope) {
        // TODO
        return scope;
    }

    /**
     *
     * @param m
     *            - unused TODO
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

            if (!isFinal(var.getModifiers())) {
                fail(ERR_BAD_GUARD_NO_FINAL, arg, arg);
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
        if (condition.getKind() != Kind.METHOD_INVOCATION) {
            return;
        }
        MethodInvocationTree method = (MethodInvocationTree) condition;
        ExecutableElement m = TreeUtils.elementFromUse(method);
        SCJ_METHODS sig = compareName(m);
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
                if (sig == ALLOC_IN_PARENT) {
                    varScopes.addParentRelation(var1Name, var2Name);
                } else {
                    varScopes.addSameRelation(var1Name, var2Name);
                }
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
        checkMethodRunsIn(m, recvScope, runsIn, node);
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
            return checkNewInstance(node);
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
        for (int i = 0; i < paramScopes.size(); i++) {
            checkLocalAssignment(paramScopes.get(i), argScopes.get(i), node);
        }
    }

    private void checkMethodRunsIn(ExecutableElement m, ScopeInfo recvScope,
            ScopeInfo runsInScope, MethodInvocationTree n) {
        if (currentScope().isUnknown() && !runsInScope.isUnknown()) {
            fail(ERR_BAD_METHOD_INVOKE, n, CURRENT, UNKNOWN);
        } else if (!runsInScope.isUnknown()
                && !runsInScope.equals(currentScope())) {
            fail(ERR_BAD_METHOD_INVOKE, n, runsInScope, currentScope());
        }
    }

    /**
     * TODO: perhaps move this method to UTILs? - the same for
     * isManagedMemoryType?
     *
     *
     * @param method
     * @return
     */
    private SCJ_METHODS compareName(ExecutableElement method) {
        TypeElement type = Utils.getMethodClass(method);

        if (isManagedMemoryType(type)) {
            if (Utils.getMethodSignature(method).equals(
                    ENTER_PRIVATE_MEMORY.toString()))
                return ENTER_PRIVATE_MEMORY;
            if (Utils.getMethodSignature(method).equals(
                    ALLOC_IN_SAME.toString()))
                return ALLOC_IN_SAME;
            if (Utils.getMethodSignature(method).equals(
                    ALLOC_IN_PARENT.toString()))
                return ALLOC_IN_PARENT;
            if (Utils.getMethodSignature(method).equals(
                    GET_CURRENT_MANAGED_MEMORY.toString()))
                return GET_CURRENT_MANAGED_MEMORY;
        }
        if (implementsAllocationContext(type)) {
            if (Utils.getMethodSignature(method)
                    .equals(NEW_INSTANCE.toString()))
                return NEW_INSTANCE;
            if (Utils.getMethodSignature(method).equals(NEW_ARRAY.toString()))
                return NEW_ARRAY;
            if (Utils.getMethodSignature(method).equals(
                    NEW_ARRAY_IN_AREA.toString()))
                return NEW_ARRAY_IN_AREA;
            if (Utils.getMethodSignature(method).equals(
                    EXECUTE_IN_AREA.toString()))
                return EXECUTE_IN_AREA;
        }

        if (isMemoryAreaType(type)
                && Utils.getMethodSignature(method).equals(GET_MEMORY_AREA))
            return GET_MEMORY_AREA;

        return DEFAULT;
    }

    private ScopeInfo checkNewInstance(MethodInvocationTree node) {
        ScopeInfo scope = null;
        // TODO: revisit checking of the "newInstance"!!!!
        ExpressionTree e = node.getMethodSelect();
        ExpressionTree arg = node.getArguments().get(0);
        ScopeInfo varScope = e.accept(this, null);
        Element newInstanceType;

        if (arg.getKind() == Kind.MEMBER_SELECT
                && ((MemberSelectTree) arg).getExpression().getKind() == Kind.IDENTIFIER
                && (newInstanceType = TreeUtils
                        .elementFromUse((IdentifierTree) ((MemberSelectTree) arg)
                                .getExpression())).getKind() == ElementKind.CLASS) {
            TypeElement newInstanceTypeElement = (TypeElement) newInstanceType;
            ScopeInfo instanceScope = ctx.getClassScope(newInstanceTypeElement);
            if (instanceScope != null && !varScope.equals(instanceScope)) {
                fail(ERR_BAD_NEW_INSTANCE, node, newInstanceTypeElement,
                        varScope);
            }
        } else {
            // TODO: We only accept newInstance(X.class) right now
        }
        return scope;
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
        // TODO: revisit checking of the "getMemoryArea"!!!!
        ScopeInfo scope = null;
        return scope;
    }

    private void checkReturnScope(ScopeInfo exprScope, ScopeInfo expectedScope,
            ReturnTree node) {
        debugIndent("checkReturnScope");
        if (expectedScope.isUnknown() || expectedScope.equals(exprScope)
                || (exprScope == null || exprScope.isNull())) {
            return;
        }
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
                        .equals("javax.safetycritical.PrivateMemory")) {
            if (!var1.getAnnotationMirrors().toString()
                    .equals(var2.getAnnotationMirrors().toString()))
                return true;
        }
        return false;
    }

    private ScopeInfo checkVariableScope(VariableTree node) {
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        if (Utils.isStatic(node.getModifiers().getFlags())) {
            return ScopeInfo.IMMORTAL;
        }
        // TODO: UNKNOWN parameters
        TypeMirror varMirror = var.asType();
        TypeMirror varBaseMirror = Utils.getBaseType(varMirror);
        if (varBaseMirror.getKind().isPrimitive()) {
            if (varMirror == varBaseMirror) {
                // Primitives have no scope
                return null;
            } else {
                // Primitive array
                // TODO: Don't feel like thinking about this now, but
                // I think we should be using the @Scope annotation
                return currentScope();
            }
        }
        TypeElement t = Utils.getTypeElement(varBaseMirror);
        ScopeInfo tScope = ctx.getClassScope(t);

        Scope varScope = var.getAnnotation(Scope.class);
        if (varScope == null) {
            if (tScope.isCurrent()) {
                return currentScope();
            } else {
                return tScope;
            }
        } else if (tScope.isCurrent()) {
            return tScope;
        } else {
            if (!tScope.equals(new ScopeInfo(varScope.value()))) {
                fail(ERR_BAD_VARIABLE_SCOPE, node, t.getSimpleName(),
                        currentScope());
            }
            return tScope;
        }
    }

    private boolean isPrimitiveExpression(ExpressionTree expr) {
        if (expr.getKind() == Kind.NEW_ARRAY)
            return false;
        return atf.fromExpression(expr).getKind().isPrimitive();
    }

    private ScopeInfo currentScope() {
        return currentRunsIn != null ? currentRunsIn : currentScope;
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

    private static Tree getArrayTypeTree(Tree nodeTypeTree) {
        while (nodeTypeTree.getKind() == Kind.ARRAY_TYPE) {
            nodeTypeTree = ((ArrayTypeTree) nodeTypeTree).getType();
        }
        return nodeTypeTree;
    }

    private static final String THIS = "this";

    private boolean isThis(IdentifierTree node) {
        return node.getName().toString().equals(THIS);
    }
}