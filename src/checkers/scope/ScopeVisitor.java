package checkers.scope;

import static checkers.Utils.isFinal;
import static checkers.Utils.isStatic;
import static checkers.Utils.SCJ_METHODS.ALLOC_IN_PARENT;
import static checkers.Utils.SCJ_METHODS.ALLOC_IN_SAME;
import static checkers.Utils.SCJ_METHODS.DEFAULT;
import static checkers.Utils.SCJ_METHODS.ENTER_PRIVATE_MEMORY;
import static checkers.Utils.SCJ_METHODS.EXECUTE_IN_AREA;
import static checkers.Utils.SCJ_METHODS.GET_CURRENT_MANAGED_AREA;
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
import java.util.EnumSet;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import checkers.SCJVisitor;
import checkers.Utils;
import checkers.Utils.SCJ_METHODS;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;
import checkers.util.TypesUtils;

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
    private static EnumSet<ElementKind> classOrInterface = EnumSet.of(ElementKind.CLASS, ElementKind.INTERFACE);

    public ScopeVisitor(SourceChecker checker, CompilationUnitTree root, ScopeCheckerContext ctx) {
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

    @Override
    public ScopeInfo visitClass(ClassTree node, P p) {
        debugIndentIncrement("visitClass " + node.getSimpleName());
        debugIndent("visitClass :" + TreeUtils.elementFromDeclaration(node).getQualifiedName());

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
            Utils.debugPrintln("Seen class " + t.getQualifiedName() + ": @Scope(" + scope + ")");

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
        ScopeInfo lhs = node.getVariable().accept(this, p);
        if (TreeUtils.isStringCompoundConcatenation(node)) {
            if (!lhs.isCurrent()) {
                // TODO: report error
            }
            return lhs;
        }
        return null; // Primitives have no scope
    }

    @Override
    public ScopeInfo visitConditionalExpression(ConditionalExpressionTree node, P p) {
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
        Element elem = TreeUtils.elementFromUse(node);
        if (elem.getKind() == ElementKind.FIELD ||
                elem.getKind() == ElementKind.LOCAL_VARIABLE ||
                elem.getKind() == ElementKind.PARAMETER) {
            String var = node.getName().toString();
            ScopeInfo scope = varScopes.getVariableScope(var);
            return scope;
        } else if (elem.getKind() == ElementKind.METHOD
                || elem.getKind() == ElementKind.CONSTRUCTOR) {
            // If an identifier gets visited and its element is a method, then
            // it is part of a MethodInvocationTree as the method select. It's
            // either a static method, in which case there is no receiver
            // object, or it's an instance method invoked on the current
            // object, in which case it is implicitly invoked on "this". We
            // return the scope of "this", which will be discarded if the
            // method being invoked is static.
            return varScopes.getVariableScope("this");
        }
        return super.visitIdentifier(node, p);
    }

    @Override
    public ScopeInfo visitIf(IfTree node, P p) {
        // TODO: Guards
        return super.visitIf(node, p);
    }

    @Override
    public ScopeInfo visitLiteral(LiteralTree node, P p) {
        if (node.getValue() == null) {
            return ScopeInfo.NULL;
        } else if (node.getValue() instanceof String) {
            // TODO I foresee much sadness in this later on. Strings can't
            // really interact between IMMORTAL and other scopes if it's not
            // RunsIn(UNKNOWN).
            return ScopeInfo.IMMORTAL;
        }
        return null;
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
            TypeElement fType = Utils.getTypeElement(f.asType());
            ScopeInfo typeScope = ctx.getClassScope(fType);
            if (!typeScope.isCurrent()) {
                return new FieldScopeInfo(typeScope, receiver, fScope);
            } else if (fScope.isCurrent()) {
                return new FieldScopeInfo(receiver, receiver,
                        fScope);
            } else {
                // UNKNOWN
                return new FieldScopeInfo(fScope, receiver, fScope);
            }
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
            ScopeInfo runsIn = ctx.getEffectiveMethodRunsIn(method, currentScope());
            debugIndent("@RunsIn(" + runsIn + ") " + method.getSimpleName());

            if (runsIn != null) {
                currentRunsIn = runsIn;
            }
            varScopes.pushBlock();
            List<? extends VariableTree> params = node.getParameters();
            List<ScopeInfo> paramScopes = ctx.getParameterScopes(method);
            for (int i = 0; i < paramScopes.size(); i++) {
                varScopes.addVariableScope(params.get(i).getName().toString(),
                        paramScopes.get(i));
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
        try {
            debugIndentIncrement("visitMethodInvocation : " + node);
            ExecutableElement m = TreeUtils.elementFromUse(node);
            List<? extends ExpressionTree> args = node.getArguments();
            List<ScopeInfo> argScopes = new ArrayList<ScopeInfo>(args.size());
            for (ExpressionTree arg : args) {
                argScopes.add(arg.accept(this, p));
            }
            ScopeInfo recvScope = node.getMethodSelect().accept(this, p);
            debugIndent("recvScope : " + recvScope);

            ScopeInfo scope = checkMethodInvocation(m, recvScope, argScopes,
                    node);
            return scope;
        } finally {
            debugIndentDecrement();
        }
    }

    @Override
    public ScopeInfo visitNewArray(NewArrayTree node, P p) {
        try {
            debugIndentIncrement("visitNewArray");
            TypeMirror arrayType = InternalUtils.typeOf(node);
            TypeMirror componentType = Utils.getArrayBaseType(arrayType);
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
            ScopeInfo nodeClassScope = ctx.getClassScope(Utils.getMethodClass(ctorElement));
            if (nodeClassScope != null && !currentScope().equals(nodeClassScope) && !nodeClassScope.isCurrent()) {
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

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(getCurrentPath());
        ExecutableElement m = TreeUtils.elementFromDeclaration(enclosingMethod);
        ScopeInfo returnScope = ctx.getMethodScope(m);
        ScopeInfo exprScope = node.getExpression().accept(this, p);
        debugIndent("expected return scope is: " + returnScope);
        debugIndent("actual return scope is:" + exprScope);
        checkReturnScope(exprScope, returnScope, node);
        debugIndentDecrement();
        return null;
    }

    @Override
    public ScopeInfo visitTypeCast(TypeCastTree node, P p) {
        ScopeInfo scope = node.getExpression().accept(this, p);

        if (isPrimitiveExpression(node)) {
            return null;
        }

        debugIndentIncrement("visitTypeCast " + node);
        try {
            TypeMirror m = InternalUtils.typeOf(node);
            m = Utils.getArrayBaseType(m);

            TypeElement t = Utils.getTypeElement(m);
            ScopeInfo tScope = ctx.getClassScope(t);
            if (tScope.isCurrent()) {
                return scope;
            } else {
                return tScope;
            }
        } finally {
            debugIndentDecrement();
        }
    }

    /**
     * <ul>
     * <li>Static variables must always be in the immortal scope.
     * <li>Instance variables must make sure that the scope of the enclosing
     *     class is a child scope of the variable type's scope.
     * <li>Local variables are similar to instance variables, only it must
     *     first use the @RunsIn annotation, if any exists, before using the
     *     @Scope annotation on the class that it belongs to.
     * </ul>
     */
    @Override
    public ScopeInfo visitVariable(VariableTree node, P p) {
        ScopeInfo oldScope = currentScope;
        ScopeInfo oldRunsIn = currentRunsIn;
        try {
            debugIndentIncrement("visitVariable : " + node.toString());
            ScopeInfo lhs = checkVariableScope(node);
            varScopes.addVariableScope(node.getName().toString(), lhs);
            if (Utils.isStatic(node.getModifiers().getFlags())) {
                // Static variable, change the context to IMMORTAL while
                // evaluating the initializer
                currentRunsIn = ScopeInfo.IMMORTAL;
            }
            ExpressionTree init = node.getInitializer();
            if (init != null) {
                ScopeInfo rhs = init.accept(this, p);
                checkAssignment(lhs, rhs, node);
            }
            return null;
        } finally {
            currentScope = oldScope;
            currentRunsIn = oldRunsIn;
            debugIndentDecrement();
        }
    }

    private void checkAssignment(ScopeInfo lhs, ScopeInfo rhs, Tree errorNode) {
        if (lhs.isFieldScope()) {
            checkFieldAssignment((FieldScopeInfo) lhs, rhs, errorNode);
        } else {
            // TODO: Do we need an extra case for arrays?
            checkLocalAssignment(lhs, rhs, errorNode);
        }
    }

    private void checkFieldAssignment(FieldScopeInfo lhs, ScopeInfo rhs, Tree errorNode) {
        ScopeInfo fScope = lhs.getFieldScope();
        if (fScope.equals(rhs) && !fScope.isUnknown()) {

        }
    }

    private void checkLocalAssignment(ScopeInfo lhs, ScopeInfo rhs, Tree errorNode) {
        if (!concretize(lhs).equals(concretize(rhs))) {
            fail(ERR_BAD_ASSIGNMENT_SCOPE, errorNode, rhs, lhs);
        }
    }

    private ScopeInfo concretize(ScopeInfo scope) {
        // TODO
        return scope;
    }

    private ScopeInfo checkDynamicGuard(MethodInvocationTree node) {
        ExpressionTree arg1 = node.getArguments().get(0);
        ExpressionTree arg2 = node.getArguments().get(1);

        checkFinal(arg1, node);
        checkFinal(arg2, node);

        // TODO : finish the checking of the guard

        return null;
    }

    /**
     *
     * @param m - unused TODO
     * @param recvScope - managedMemory instance, the target of the invocation
     * @param node
     * @return
     */
    private void checkEnterPrivateMemory(MethodInvocationTree node) {
        TypeMirror runnableType = InternalUtils.typeOf(node.getArguments().get(1));
        ScopeInfo argRunsIn = getRunsInFromRunnable(runnableType);

        if (!scopeTree.isParentOf(argRunsIn, currentScope()))
            fail(ERR_BAD_ENTER_PRIVATE_MEMORY_RUNS_IN_NO_MATCH, node, argRunsIn, currentScope());
    }

    private ScopeInfo checkExecuteInArea(MethodInvocationTree node) {
        ScopeInfo scope = null;
        ExecutableElement method = TreeUtils.elementFromUse(node);
        String methodName = method.getSimpleName().toString();
        ExpressionTree e = node.getMethodSelect();
        ExpressionTree arg = node.getArguments().get(0);
        ScopeInfo argRunsIn = null; // runsIn of the Runnable
        ScopeInfo varScope = null; // @Scope of the target

        debugIndent("executeInArea Invocation: ");
        // TODO: Single exit point sure would be nicer
        switch (arg.getKind()) {
        case IDENTIFIER :
            VariableElement var = (VariableElement) TreeUtils.elementFromUse((IdentifierTree) arg);
            argRunsIn = getRunsInFromRunnable(var.asType());
            break;
        case MEMBER_SELECT :
            // TODO:
            Element tmp = TreeUtils.elementFromUse((MemberSelectTree) arg);
            if (tmp.getKind() != ElementKind.FIELD) {
                fail(ERR_BAD_ENTER_PARAM, arg);
                return null;
            } else {
                argRunsIn = directRunsIn((VariableElement) tmp);
            }
            break;
        case NEW_CLASS :

            ExecutableElement ctor = TreeUtils.elementFromUse((NewClassTree) arg);
            TypeElement el = Utils.getMethodClass(ctor);
            argRunsIn = getRunsInFromRunnable(el.asType());

            break;
        case TYPE_CAST :
            fail(ERR_TYPE_CAST_BAD_ENTER_PARAMETER, arg);
            break;
        default :
            fail(ERR_DEFAULT_BAD_ENTER_PARAMETER, arg);
            return null;
        }

        if (argRunsIn == null) {
            // All Runnables used with executeInArea/enter should have
            // @RunsIn on "run()" method
            fail(ERR_RUNNABLE_WITHOUT_RUNS_IN, node);
        } else {
            switch (e.getKind()) {
            case IDENTIFIER :
                // TODO: This only happens for this/super constructor calls
                // or implicit this.method calls. How do we
                // handle this.method? Do we need to?
                varScope = defineScope((VariableElement) TreeUtils.elementFromUse((IdentifierTree) e));
                break;
            case MEMBER_SELECT :
                // varScope = getScopeDef(((MemberSelectTree) e)
                // .getExpression());
                ExpressionTree ee = ((MemberSelectTree) e).getExpression();
                Element el = TreeUtils.elementFromUse(ee);
                varScope = scope(el);
                break;
            }

            if (varScope == null || !varScope.equals(argRunsIn)) {
                // The Runnable and the PrivateMemory must have agreeing
                // scopes
                fail(ERR_BAD_EXECUTE_IN_AREA_OR_ENTER, node);
            }
            if ("executeInArea".equals(methodName) && !scopeTree.isAncestorOf(currentScope(), varScope)) {
                fail(ERR_BAD_EXECUTE_IN_AREA_TARGET, node);
            } else if ("enter".equals(methodName) && !scopeTree.isParentOf(varScope, currentScope())) {
                fail(ERR_BAD_ENTER_TARGET, node);
            }
        }
        return scope;
    }

    private void checkFinal(ExpressionTree arg, MethodInvocationTree node) {
        switch (arg.getKind()) {
        case IDENTIFIER :
            VariableElement var = (VariableElement) TreeUtils.elementFromUse((IdentifierTree) arg);
            var.getModifiers();

            if (!isFinal(var.getModifiers())) {
                fail(ERR_BAD_GUARD_NO_FINAL, node, arg);
            }
            break;
        default :
            fail(ERR_BAD_GUARD_ARGUMENT, node, arg);
            return;
        }
    }

    private ScopeInfo checkGetMemoryArea(MethodInvocationTree node) {
        // TODO: revisit checking of the "getMemoryArea"!!!!
        ScopeInfo scope = null;
        return scope;
    }

    private ScopeInfo checkMethodInvocation(ExecutableElement m,
            ScopeInfo recvScope, List<ScopeInfo> argScopes,
            MethodInvocationTree node) {
        // TODO: static methods ?
        debugIndent("\n\t checkMethodInvocation : " + node);

        ExecutableElement method = TreeUtils.elementFromUse(node);

        switch(compareName(m)) {
        case ENTER_PRIVATE_MEMORY:
            checkEnterPrivateMemory(node);
            return null;  // void methods don't return a scope
        case EXECUTE_IN_AREA:
            checkExecuteInArea(node);
            return null;
        case ENTER:
            checkExecuteInArea(node);       // TODO: how to check the enter()?
            return null;
        case NEW_INSTANCE:
            return checkNewInstance(node);
        case GET_MEMORY_AREA:
            return checkGetMemoryArea(node);
        case ALLOC_IN_SAME:
            checkDynamicGuard(node);
            return null;
        case ALLOC_IN_PARENT:
            checkDynamicGuard(node);
            return null;
        default:
            return checkRegularMethodInvocation(m, recvScope, argScopes, node);
        }
    }



    private ScopeInfo checkRegularMethodInvocation(ExecutableElement m,
            ScopeInfo recvScope, List<ScopeInfo> argScopes,
            MethodInvocationTree node) {

        ScopeInfo runsIn = ctx.getEffectiveMethodRunsIn(m,currentScope());

        checkMethodRunsIn(m, recvScope, runsIn, node);
        checkMethodParameters(m, argScopes, node);
        return ctx.getEffectiveMethodScope(m,currentScope());
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
        } else if (currentScope().equals(recvScope)) {
            if (!(runsInScope.equals(currentScope()))) {
                // Can only call methods that run in the same scope.
                // Allows parent scopes as well, if they are marked @AllocFree.
                fail(ERR_BAD_METHOD_INVOKE, n, runsInScope, currentScope());
            }
        } else if (!runsInScope.isUnknown()) {
            fail(ERR_BAD_METHOD_INVOKE, n, runsInScope, currentScope());
        }
    }

    /**
     * TODO: perhaps move this method to UTILs?
     *  - the same for isManagedMemoryType?
     *
     *
     * @param method
     * @return
     */
    private static SCJ_METHODS compareName(ExecutableElement method) {
        TypeElement type = Utils.getMethodClass(method);

        if (isManagedMemoryType(type)) {
            if (Utils.getMethodSignature(method).equals(ENTER_PRIVATE_MEMORY.toString()))
                return ENTER_PRIVATE_MEMORY;
            if (Utils.getMethodSignature(method).equals(ALLOC_IN_SAME.toString()))
                return ALLOC_IN_SAME;
            if (Utils.getMethodSignature(method).equals(ALLOC_IN_PARENT.toString()))
                return ALLOC_IN_PARENT;
            if (Utils.getMethodSignature(method).equals(GET_CURRENT_MANAGED_AREA.toString()))
                return GET_CURRENT_MANAGED_AREA;
        }
        if (implementsAllocationContext(type)) {
            if (Utils.getMethodSignature(method).equals(NEW_INSTANCE.toString()))
                return NEW_INSTANCE;
            if (Utils.getMethodSignature(method).equals(NEW_ARRAY.toString()))
                return NEW_ARRAY;
            if (Utils.getMethodSignature(method).equals(NEW_ARRAY_IN_AREA.toString()))
                return NEW_ARRAY_IN_AREA;
            if (Utils.getMethodSignature(method).equals(EXECUTE_IN_AREA.toString()))   // TODO : why it needs to call toSTring???, should be implicit
                return EXECUTE_IN_AREA;
        }

        if (isMemoryAreaType(type) && Utils.getMethodSignature(method).equals(GET_MEMORY_AREA) )
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
                && (newInstanceType = TreeUtils.elementFromUse((IdentifierTree) ((MemberSelectTree) arg)
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

    private void checkReturnScope(ScopeInfo exprScope, ScopeInfo expectedScope,
            ReturnTree node) {
        debugIndent("checkReturnScope");
        if (expectedScope.isUnknown() || expectedScope.equals(exprScope)
                || exprScope.isNull()) {
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
    private boolean checkPrivateMemAssignmentError(VariableElement var1, VariableElement var2) {
        if (var1.asType().toString().equals("javax.safetycritical.PrivateMemory")
                && var2.asType().toString().equals("javax.safetycritical.PrivateMemory")) {
            if (!var1.getAnnotationMirrors().toString().equals(var2.getAnnotationMirrors().toString()))
                return true;
        }
        return false;
    }

    private ScopeInfo checkVariableScope(VariableTree node) {
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        if (Utils.isStatic(node.getModifiers().getFlags())) {
            // TODO:
            return ScopeInfo.IMMORTAL;
        }
        // TODO: UNKNOWN parameters
        Tree nodeTypeTree = getArrayTypeTree(node.getType());
        if (nodeTypeTree.getKind() == Kind.PRIMITIVE_TYPE) {
            if (nodeTypeTree == node.getType()) {
                return null;
            } else {
                // Primitive array
                return currentScope();
            }
        }
        TypeElement t = Utils.getTypeElement(var.asType());
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

    private ScopeInfo getScope(Element element) {
        Scope scope = element.getAnnotation(Scope.class);
        if (scope == null)
            return null;

        return new ScopeInfo(scope.value());
    }

    private boolean hasRunsIn(Element element) {
        return element.getAnnotation(RunsIn.class) != null;
    }

    // Doesn't work for interfaces (nor should it)
    private static boolean isMemoryAreaType(TypeElement t) {
        if (t.getKind() == ElementKind.INTERFACE)
            return false;
        while (!TypesUtils.isDeclaredOfName(t.asType(), MEMORY_AREA)
                && !TypesUtils.isObject(t.asType())) {
            t = Utils.getTypeElement(t.getSuperclass());
        }
        return TypesUtils.isDeclaredOfName(t.asType(), MEMORY_AREA);
    }

    private final static String ALLOCATION_CONTEXT = "javax.realtime.AllocationContext";
    private final static String MEMORY_AREA = "javax.realtime.MemoryArea";
    private final static String MANAGED_MEMORY = "javax.safetycritical.ManagedMemory";

    /**
     *
     * @param t
     * @return true if the type of the element t impelements interface AllocationContext
     */
    private static boolean implementsAllocationContext(TypeElement t) {
        if (t.getKind() == ElementKind.INTERFACE) {
            if (t.toString().equals(ALLOCATION_CONTEXT))
                return true;
        }

        while (!TypesUtils.isDeclaredOfName(t.asType(), MANAGED_MEMORY)
                && !TypesUtils.isObject(t.asType())) {
            for (TypeMirror inter : t.getInterfaces()) {
                if (inter.toString().equals(ALLOCATION_CONTEXT))
                    return true;
            }
            t = Utils.getTypeElement(t.getSuperclass());
        }

        return false;
    }

    // Doesn't work for interfaces (nor should it)
    private static boolean isManagedMemoryType(TypeElement t) {
        if (t.getKind() == ElementKind.INTERFACE) {
            return false;
        }
        while (!TypesUtils.isDeclaredOfName(t.asType(), MANAGED_MEMORY)
                && !TypesUtils.isObject(t.asType())) {
            t = Utils.getTypeElement(t.getSuperclass());
        }
        return TypesUtils.isDeclaredOfName(t.asType(), MANAGED_MEMORY);
    }

    private boolean isPrimitiveExpression(ExpressionTree expr) {
        if (expr.getKind() == Kind.NEW_ARRAY)
            return false;
        return atf.fromExpression(expr).getKind().isPrimitive();
    }

    private ScopeInfo currentScope() {
        return currentRunsIn != null ? currentRunsIn : currentScope;
    }

    private ScopeInfo scope(Element var) {
        ScopeInfo varScope = varScope(var);

        if (isStatic(var.getModifiers())) // static
            if (varScope == null || varScope.isImmortal()) {
                return ScopeInfo.IMMORTAL;
            } else {
                return varScope; // this is ERROR and should fail
            }

        if (varScope != null) {
            return varScope;
        }

        // Variable's type is not annotated, so go by the enclosing environment.
        if (classOrInterface.contains(var.getEnclosingElement().getKind())) { // instance
            return ctx.getClassScope((TypeElement) var.getEnclosingElement());
        } else { // local
            return currentScope();
        }
    }

    private ScopeInfo varScope(Element var) {
        /*
         * 1. Look for the @Scope annotation on the variable. For example
         *
         * @Scope(UNKNOWN) Foo foo;
         */
        ScopeInfo varScope = getScope(var);
        // if (varScope != null) {
        // return varScope;
        // }

        ScopeInfo typeScope = null;
        /*
         * 2. Look for the @Scope annotation on the variable's type. For example
         *
         * @Scope("Foo") class Foo {....}
         */
        TypeMirror exprType = atf.getAnnotatedType(var).getUnderlyingType();
        exprType = Utils.getArrayBaseType(exprType);

        if (exprType.getKind() == TypeKind.DECLARED) {
            // return scope(elements.getTypeElement(exprType.toString()), null);
            typeScope = ctx.getClassScope(Utils.getTypeElement(exprType));
        }

        if (typeScope != null && varScope != null) {
            if (!typeScope.equals(varScope)) {
                throw new RuntimeException("error.var.and.type.scope.annotation.mismatch");
            }
        }
        if (varScope != null)
            return varScope;

        return typeScope;
    }

    private ScopeInfo directRunsIn(VariableElement var) {
        AnnotatedTypeMirror exprType = atf.getAnnotatedType(var);
        // TODO: since the exprType sometimes does not contains the annotations
        // if this does not work, we use the loop below...
        if (exprType.getKind() == TypeKind.DECLARED) {
            Element type = ((AnnotatedDeclaredType) exprType).getUnderlyingType().asElement();
            if (hasRunsIn(type))
                return runsIn(type);
        }

        while (!TypesUtils.isObject(exprType.getUnderlyingType())) {
            ScopeInfo exprScope = Utils.runsIn(exprType.getAnnotations());
            if (exprScope != null) {
                return exprScope;
            } else {
                exprType = exprType.directSuperTypes().get(0);
            }
        }
        return null;
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

    private ScopeInfo runsIn(Element type) {
        RunsIn ri = type.getAnnotation(RunsIn.class);
        if (ri != null) {
            return new ScopeInfo(ri.value());
        }
        return null;
    }

    private ScopeInfo defineScope(VariableElement var) {
        DefineScope ds = var.getAnnotation(DefineScope.class);
        if (ds != null) {
            return new ScopeInfo(ds.name());
        }
        return null;
    }

    private static Tree getArrayTypeTree(Tree nodeTypeTree) {
        while (nodeTypeTree.getKind() == Kind.ARRAY_TYPE) {
            nodeTypeTree = ((ArrayTypeTree) nodeTypeTree).getType();
        }
        return nodeTypeTree;
    }
}

