package checkers.scope;

import static checkers.Utils.isFinal;
import static checkers.Utils.isStatic;
import static checkers.scjAllowed.EscapeMap.escapeAnnotation;
import static checkers.scjAllowed.EscapeMap.escapeEnum;
import static javax.safetycritical.annotate.Scope.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import checkers.Utils;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypes;
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
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

import static checkers.scope.ScopeChecker.*;


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
@SuppressWarnings("restriction")
public class ScopeVisitor<P> extends SourceVisitor<ScopeInfo, P> {

    static private void pln(String s) {
        System.out.println(s);
    }

    private AnnotatedTypeFactory atf;
    private AnnotatedTypes ats;
    private String currentScope = null;
    private String currentRunsIn = null;
    private ScopeCheckerContext ctx;
    private ScopeTree scopeTree;
    private static EnumSet<ElementKind> classOrInterface = EnumSet.of(ElementKind.CLASS, ElementKind.INTERFACE);

    public ScopeVisitor(SourceChecker checker, CompilationUnitTree root, ScopeCheckerContext ctx) {
        super(checker, root);

        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
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
            if (lhs.equals(rhs)) {
                return lhs;
            }
            checkAssignment(node.getVariable(), node.getExpression(), node);
            return lhs;
        } finally {
            debugIndentDecrement();
        }
    }

    @Override
    public ScopeInfo visitBinary(BinaryTree node, P p) {
        super.visitBinary(node, p);
        if (TreeUtils.isCompileTimeString(node)) {
            return new ScopeInfo(IMMORTAL);
        } else if (TreeUtils.isStringConcatenation(node)) {
            return new ScopeInfo(CURRENT);
        }
        return null; // Primitive expressions have no scope
    }

    @Override
    public ScopeInfo visitBlock(BlockTree node, P p) {
        debugIndentIncrement("visitBlock");
        String oldRunsIn = currentRunsIn;
        if (node.isStatic()) {
            currentRunsIn = IMMORTAL;
        }
        super.visitBlock(node, p);
        currentRunsIn = oldRunsIn;
        debugIndentDecrement();
        return null;
    }

    @Override
    public ScopeInfo visitClass(ClassTree node, P p) {
        debugIndentIncrement("\nvisitClass " + node.getSimpleName());
        debugIndent("\nvisitClass :" + TreeUtils.elementFromDeclaration(node).getQualifiedName());

        if (escapeEnum(node) || escapeAnnotation(node)) {
            debugIndent("\nvisitClass : escaping the Class. ");
            debugIndentDecrement();
            return null;
        }

        /*
         * Scope tree verification
         */
        if (!scopeTree.verifyScopeTree()) {
            scopeTree.reportErrors(this);
            debugIndentDecrement();
            return null;
        }

        TypeElement t = TreeUtils.elementFromDeclaration(node);
        String oldScope = currentScope;
        String oldRunsIn = currentRunsIn;

        try {
            String scope = ctx.getClassScope(t);

            // TODO: assume defaults for inner classes?
            Utils.debugPrintln("Seen class " + t.getQualifiedName() + ": @Scope(" + scope + ")");

            currentScope = scope;
            currentRunsIn = scope;

            return super.visitClass(node, p);
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
            if (!lhs.getScope().equals(CURRENT)) {
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
        return super.visitEnhancedForLoop(node, p);
    }

    @Override
    public ScopeInfo visitIdentifier(IdentifierTree node, P p) {
        // TODO
        return super.visitIdentifier(node, p);
    }

    @Override
    public ScopeInfo visitIf(IfTree node, P p) {
        // TODO: Guards
        return super.visitIf(node, p);
    }

    @Override
    public ScopeInfo visitLiteral(LiteralTree node, P p) {
        if (node.getValue() instanceof String) {
            // TODO I foresee much sadness in this later on. Strings can't
            // really interact between IMMORTAL and other scopes if it's not
            // RunsIn(UNKNOWN).
            return new ScopeInfo(IMMORTAL);
        }
        return null;
    }

    @Override
    public ScopeInfo visitMemberSelect(MemberSelectTree node, P p) {
        Element elem = TreeUtils.elementFromUse(node);
        ScopeInfo receiver = node.getExpression().accept(this, p);

        try {
            debugIndentIncrement("visitMemberSelect: " + node.toString());
            if (elem.getKind() != ElementKind.FIELD) {
                return null; // Part of a method invocation
            }
            VariableElement f = (VariableElement) elem;
            String fScope = ctx.getFieldScope(f);
            TypeElement fType = Utils.getTypeElement(f.asType());
            String typeScope = ctx.getClassScope(fType);
            String receiverScope = receiver.getScope();
            if (!CURRENT.equals(typeScope)) {
                return new FieldScopeInfo(typeScope, receiverScope, fScope);
            } else if (CURRENT.equals(fScope)) {
                return new FieldScopeInfo(receiverScope, receiverScope,
                        fScope);
            } else {
                // UNKNOWN
                return new FieldScopeInfo(fScope, receiverScope, fScope);
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
        debugIndent("RUNS IN: " + currentRunsIn);
        debugIndent("scope: " + currentScope);

        String oldRunsIn = currentRunsIn;
        try {
            String runsIn = ctx.getMethodRunsIn(method);
            debugIndent("@RunsIn(" + runsIn + ") " + method.getSimpleName());
            if (runsIn != null) {
                currentRunsIn = runsIn;
            }
            super.visitMethod(node, p);
        } finally {
            currentRunsIn = oldRunsIn;
        }

        debugIndentDecrement();
        return null;
    }

    @Override
    public ScopeInfo visitMethodInvocation(MethodInvocationTree node, P p) {
        try {
            debugIndentIncrement("visitMethodInvocation");
            // pln("visit method invocation: " + node);
            checkMethodInvocation(node, p);
            return super.visitMethodInvocation(node, p);
        } finally {
            debugIndentDecrement();
        }
    }

    @Override
    public ScopeInfo visitNewArray(NewArrayTree node, P p) {
        try {
            debugIndentIncrement("visitNewArray");
            AnnotatedArrayType arrayType = atf.getAnnotatedType(node);
            while (arrayType.getComponentType().getKind() == TypeKind.ARRAY) {
                arrayType = (AnnotatedArrayType) arrayType.getComponentType();
            }
            AnnotatedTypeMirror componentType = arrayType.getComponentType();
            if (!componentType.getKind().isPrimitive()) {
                TypeElement t = Utils.getTypeElement(componentType.getUnderlyingType());
                String scope = ctx.getClassScope(t);
                if (!(scope.equals(CURRENT) || scope.equals(currentAllocScope()))) {
                    report(Result.failure(ERR_BAD_ALLOCATION, currentAllocScope(), scope), node);
                }
            }
            super.visitNewArray(node, p);
            return new ScopeInfo(currentAllocScope());
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
            String nodeClassScope = ctx.getClassScope(Utils.getMethodClass(ctorElement));
            if (nodeClassScope != null && !currentAllocScope().equals(nodeClassScope) && !nodeClassScope.equals(CURRENT)) {
                // Can't call new unless the type has the same scope as the
                // current context
                report(Result.failure(ERR_BAD_ALLOCATION, currentAllocScope(), nodeClassScope), node);
            }
            super.visitNewClass(node, p);
            return new ScopeInfo(currentAllocScope());
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
        // enableDebug();
        debugIndentIncrement(indent + "visit Return:" + node.toString());

        // Don't try to check return expressions for void methods.
        if (node.getExpression() == null)
            return super.visitReturn(node, p);

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(getCurrentPath());
        String returnScope = ctx.getMethodScope(TreeUtils.elementFromDeclaration(enclosingMethod));

        ScopeInfo ret = node.getExpression().accept(this, p);
        debugIndent("return scope is :" + returnScope);

        if (returnScope == null || !returnScope.equals(UNKNOWN))
            checkReturnScope(node.getExpression(), node, returnScope);

        debugIndent("\n\n");
        // disableDebug();
        debugIndentDecrement();
        return super.visitReturn(node, p);
    }

    @Override
    public ScopeInfo visitTypeCast(TypeCastTree node, P p) {
        ScopeInfo scope = node.getExpression().accept(this, p);

        if (isPrimitiveExpression(node)) {
            return null;
        }

        debugIndentIncrement("visitTypeCast " + node);
        try {
            AnnotatedTypeMirror am = atf.fromTypeTree(node.getType());
            while (am.getKind() == TypeKind.ARRAY) {
                am = ((AnnotatedArrayType) am).getComponentType();
            }
            TypeElement t = Utils.getTypeElement(am.getUnderlyingType());
            String tScope = ctx.getClassScope(t);
            if (CURRENT.equals(tScope)) {
                return scope;
            } else {
                return new ScopeInfo(tScope);
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
        String oldScope = currentScope;
        String oldRunsIn = currentRunsIn;
        try {
            debugIndentIncrement("visitVariable : " + node.toString());
            checkVariable(node, p);
            return super.visitVariable(node, p);
        } finally {
            currentScope = oldScope;
            currentRunsIn = oldRunsIn;
            debugIndentDecrement();
        }
    }

    private boolean checkAssignment(Tree varTree, ExpressionTree exprTree, Tree errorNode) {
        debugIndent("check Assignment : ");

        if (isPrimitiveExpression(exprTree)) {
            return true; // primitive assignments are always allowed
        }

        VariableElement varElem = lhsToVariable(varTree);
        if (varElem == null) {
            return true; // annotation assignments are irrelevant
        }

        String varName = varElem.getSimpleName().toString();
        String varScope = scope(varElem), exprScope = null;
        exprTree = simplify(exprTree);
        Kind exprKind = exprTree.getKind();

        if (exprKind == Kind.NULL_LITERAL || exprKind == Kind.STRING_LITERAL) {
            return true;
        } else if (exprKind == Kind.NEW_ARRAY) {
            // Handled by visitNewArray
            return true;
        } else if (exprKind == Kind.METHOD_INVOCATION) {
            MethodInvocationTree methodExpr = (MethodInvocationTree) exprTree;
            ExecutableElement methodElem = TreeUtils.elementFromUse(methodExpr);
            AnnotatedExecutableType methodType = atf.getAnnotatedType(methodElem);
            TypeMirror retMirror = methodType.getReturnType().getUnderlyingType();
            while (retMirror.getKind() == TypeKind.ARRAY) {
                retMirror = ((ArrayType) retMirror).getComponentType();
            }
            if (retMirror.getKind().isPrimitive()) {
                exprScope = varScope; // primitives are always assignable
            } else {
                exprScope = ctx.getClassScope(Utils.getTypeElement(retMirror));
                if (exprScope == null) {
                    // first, look if the method has @Scope annotation
                    exprScope = getScope(methodElem);

                    // second, if its null, then look at its @RunsIn
                    if (exprScope == null)
                        exprScope = ctx.getMethodRunsIn(methodElem); // TODO:
                    // revisit
                }
                if (exprScope == null) {
                    debugIndent("Expression Scope is NULL, ERR?? :" + methodExpr.getMethodSelect().getKind());
                }

                debugIndent("Assignment, method invoke:" + errorNode);
                debugIndent("varScope:" + varScope);
                debugIndent("exprScope:" + exprScope);
                debugIndent("expr Tree:" + exprTree);
            }
        } else if (exprKind == Kind.NEW_CLASS) {
            /*
             * checks that @Scope(parent) data = @Scope(child) new Object() <--
             * this is assignment error while we are in the child
             */
            if (currentAllocScope() != null && !currentAllocScope().equals(varScope)) {
                /* checked by TestScopeCheck2.java */
                report(Result.failure(ERR_BAD_ASSIGNMENT_SCOPE, currentAllocScope(), varScope), errorNode);
            }

            // Handled by visitNewClass
            return true;
        } else if (exprKind == Kind.PLUS) {

        } else if (exprKind == Kind.MEMBER_SELECT) {
            /* CHECKING the FIELD */

            // pln("Assignmentm: MEMBER_SELECT ::: " +
            // errorNode);

            VariableElement var = (VariableElement) TreeUtils.elementFromUse(exprTree);
            // pln("variable element ::: " + var);
            // pln("variable element ::: " +
            // var.getAnnotationMirrors());

            exprScope = scope(var);

            // pln("\t expr element ::: " + var);
            // pln("\t expr element ::: " +
            // var.getAnnotationMirrors());
            // pln("\t expre scope ::: " + exprScope);
            //
            // pln("\t var element ::: " + varElem);
            // pln("\t varScope ::: " + varScope);

            if (var.getSimpleName().toString().equals("cs")) {
                System.err.println("Assignment: @Scope(" + varScope + ") " + varName + " = @Scope(" + exprScope + ")");
            }

            VariableElement var1 = (VariableElement) TreeUtils.elementFromUse(exprTree);
            VariableElement var2 = lhsToVariable(varTree);
            if (checkPrivateMemAssignmentError(var1, var2)) {
                /* checked by TestPrivateMemoryAssignment */
                report(Result.failure(ERR_BAD_ASSIGNMENT_PRIVATE_MEM, exprScope, varScope), errorNode);
            }

        } else if (exprKind == Kind.IDENTIFIER) {
            /* CHECKING the LOCAL VARIABLE */

            // pln("Assignmentm: IDENTIFIER :::" + errorNode);

            VariableElement var = (VariableElement) TreeUtils.elementFromUse(exprTree);
            exprScope = scope(var);

            // pln("\t variable element ::: " + var);
            // pln("\t variable element ::: " +
            // var.getAnnotationMirrors());
            // pln("\t expre scope ::: " + exprScope);

            if (var.getSimpleName().toString().equals("cs")) {
                System.err.println("Assignment: @Scope(" + varScope + ") " + varName + " = @Scope(" + exprScope + ")");
            }

            VariableElement var1 = (VariableElement) TreeUtils.elementFromUse(exprTree);
            VariableElement var2 = lhsToVariable(varTree);
            if (checkPrivateMemAssignmentError(var1, var2)) {
                /* checked by TestPrivateMemoryAssignment */
                report(Result.failure(ERR_BAD_ASSIGNMENT_PRIVATE_MEM, exprScope, varScope), errorNode);
            }
        }

        else if (exprKind == Kind.TYPE_CAST) {
            // VariableElement var = (VariableElement)
            // TreeUtils.elementFromUse(exprTree);

            Element el = TreeInfo.symbol((JCTree) exprTree);
            debugIndent("element " + el);
            debugIndent("expr " + exprTree);
            // exprScope = scope(var);

            TypeMirror castType = atf.fromTypeTree(((TypeCastTree) exprTree).getType()).getUnderlyingType();
            while (castType.getKind() == TypeKind.ARRAY) {
                castType = ((ArrayType) castType).getComponentType();
            }
            if (castType.getKind().isPrimitive()) {
                exprScope = varScope;
            } else {
                exprScope = ctx.getClassScope(Utils.getTypeElement(castType));
            }

        } else {
            throw new RuntimeException("Need a new case for " + exprKind);
        }

        Utils.debugPrintln("Assignment: @Scope(" + varScope + ") " + varName + " = @Scope(" + exprScope + ")");
        boolean isLegal;

        isLegal = varScope == null || varScope.equals(exprScope);
        if (exprScope != null) {
            isLegal = varScope == null || varScope.equals(exprScope);
        } else {
            // for the variable that has no annotation
            isLegal = varScope == null || varScope.equals(currentAllocScope());
        }

        if (!isLegal) {
            report(Result.failure(ERR_BAD_ASSIGNMENT_SCOPE, exprScope, varScope), errorNode);
        }

        return isLegal;
    }

    private void checkDynamicGuard(MethodInvocationTree node) {
        ExpressionTree arg1 = node.getArguments().get(0);
        ExpressionTree arg2 = node.getArguments().get(1);

        checkFinal(arg1, node);
        checkFinal(arg2, node);

        // TODO : finish the checking of the guard
    }

    private void checkEnterPrivateMemory(MethodInvocationTree node) {
        debugIndent("enterPrivateMemory Invocation");

        ExpressionTree e = node.getMethodSelect();
        ExpressionTree arg = node.getArguments().get(1);

        ExecutableElement method = TreeUtils.elementFromUse(node);
        String runsIn = ctx.getMethodRunsIn(method);

        String argRunsIn = null;
        String argScope = null;

        // pln("arg kind:" + arg.getKind());

        // TODO: Single exit point sure would be nicer
        switch (arg.getKind()) {
            case IDENTIFIER :

                VariableElement var = (VariableElement) TreeUtils.elementFromUse((IdentifierTree) arg);

                argRunsIn = getRunsInFromRunnable(var.asType());
                argScope = scope(var);

                break;
            case MEMBER_SELECT :
                Element tmp = TreeUtils.elementFromUse((MemberSelectTree) arg);
                if (tmp.getKind() != ElementKind.FIELD) {
                    report(Result.failure(ERR_BAD_ENTER_PARAM), arg);
                    return;
                } else {
                    argRunsIn = getRunsInFromRunnable(tmp.asType());
                    argScope = ctx.getClassScope((TypeElement) tmp.asType());
                }
                break;
            case NEW_CLASS :
                ExecutableElement ctor = TreeUtils.elementFromUse((NewClassTree) arg);

                argRunsIn = getRunsInFromRunnable(ctor.getEnclosingElement().asType());
                argScope = ctx.getClassScope(Utils.getMethodClass(ctor));

                break;
            case TYPE_CAST :
                // e.g. enterPrivateMemory(...,(Runnable) myRun)

                Element el = TreeInfo.symbol((JCTree) arg);
                debugIndent("element " + el);
                debugIndent("expr " + arg);
                // exprScope = scope(var);

                TypeMirror castType = atf.fromTypeTree(((TypeCastTree) arg).getType()).getUnderlyingType();
                argScope = ctx.getClassScope(Utils.getTypeElement(castType));
                argRunsIn = getRunsInFromRunnable(castType);
                // pln("argScope:" + argScope);
                // pln("argScope:" + argRunsIn);

                break;
            default :
                report(Result.failure(ERR_BAD_ENTER_PARAM), arg);
                return;
        }

        if (argRunsIn == null) {
            /* checked by scope.MyMission2.java */
            report(Result.failure(ERR_BAD_ENTER_PRIVATE_MEM_NO_RUNS_IN), node);
        } else if (argScope == null || !argScope.equals(currentAllocScope())) {
            /* checked by scope.MyMission2.java */
            report(Result.failure(ERR_BAD_ENTER_PRIVATE_MEM_NO_SCOPE_ON_RUNNABLE), node);
        } else if (!scopeTree.isParentOf(argRunsIn, currentAllocScope())) {
            /* checked by scope.MyMission2.java */
            report(Result.failure(ERR_BAD_ENTER_PRIVATE_MEM_RUNS_IN_NO_MATCH, argRunsIn, currentAllocScope()), node);
        }
        debugIndent("enterPrivateMemory Invocation: DONE.");
    }

    private void checkExecuteInArea(MethodInvocationTree node) {
        ExecutableElement method = TreeUtils.elementFromUse(node);
        String methodName = method.getSimpleName().toString();
        ExpressionTree e = node.getMethodSelect();
        ExpressionTree arg = node.getArguments().get(0);
        String argRunsIn = null; // runsIn of the Runnable
        String varScope = null; // @Scope of the target

        debugIndent("executeInArea Invocation: ");
        // TODO: Single exit point sure would be nicer
        switch (arg.getKind()) {
            case IDENTIFIER :
                VariableElement var = (VariableElement) TreeUtils.elementFromUse((IdentifierTree) arg);
                argRunsIn = getRunsInFromRunnable(var.asType());
                break;
            case MEMBER_SELECT :
                pln("\tEXEC: Type cast   ::: bad.enter.parameter");

                // TODO:
                Element tmp = TreeUtils.elementFromUse((MemberSelectTree) arg);
                if (tmp.getKind() != ElementKind.FIELD) {
                    report(Result.failure(ERR_BAD_ENTER_PARAM), arg);
                    return;
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
                // pln("\tEXEC: Type cast");
                report(Result.failure(ERR_TYPE_CAST_BAD_ENTER_PARAMETER), arg);
                break;
            default :
                // pln("\tEXEC: Type cast + " + arg.getKind());
                report(Result.failure(ERR_DEFAULT_BAD_ENTER_PARAMETER), arg);
                return;
        }

        if (argRunsIn == null) {
            // All Runnables used with executeInArea/enter should have
            // @RunsIn on "run()" method
            report(Result.failure(ERR_RUNNABLE_WITHOUT_RUNSIN), node);
        } else {
            switch (e.getKind()) {
                case IDENTIFIER :
                    // TODO: This only happens for this/super constructor calls
                    // or implicit this.method calls. How do we
                    // handle this.method? Do we need to?
                    varScope = scopeDef((VariableElement) TreeUtils.elementFromUse((IdentifierTree) e));
                    break;
                case MEMBER_SELECT :
                    // varScope = getScopeDef(((MemberSelectTree) e)
                    // .getExpression());
                    ExpressionTree ee = ((MemberSelectTree) e).getExpression();
                    Element el = TreeUtils.elementFromUse(ee);
                    varScope = scope(el);
                    break;
            }

            //pln("\n\n-------------------- :");
            //pln("argRunsIn :" + argRunsIn);
            // pln("argScope :" + argScope);
            //pln("varScope :" + varScope);
            // pln("current :" + currentAllocScope());
            //pln("ancestor :" + scopeTree.isAncestorOf(currentAllocScope(), varScope));

            if (varScope == null || !varScope.equals(argRunsIn)) {
                // The Runnable and the PrivateMemory must have agreeing
                // scopes
                report(Result.failure(ERR_BAD_EXECUTE_IN_AREA_OR_ENTER), node);
            }
            if ("executeInArea".equals(methodName) && !scopeTree.isAncestorOf(currentAllocScope(), varScope)) {
                report(Result.failure(ERR_BAD_EXECUTE_IN_AREA_TARGET), node);
            } else if ("enter".equals(methodName) && !scopeTree.isParentOf(varScope, currentAllocScope())) {
                report(Result.failure(ERR_BAD_ENTER_TARGET), node);
            }
        }
    }

    private void checkFinal(ExpressionTree arg, MethodInvocationTree node) {
        switch (arg.getKind()) {
            case IDENTIFIER :
                VariableElement var = (VariableElement) TreeUtils.elementFromUse((IdentifierTree) arg);
                var.getModifiers();

                if (!isFinal(var.getModifiers())) {
                    report(Result.failure(ERR_BAD_GUARD_NO_FINAL, arg), node);
                }
                break;
            default :
                report(Result.failure(ERR_BAD_GUARD_ARGUMENT, arg), node);
                return;
        }

    }

    private void checkGetMemoryArea(MethodInvocationTree node) {
        // TODO: revisit checking of the "getMemoryArea"!!!!
    }

    private void checkMethodInvocation(MethodInvocationTree node, P p) {
        ExecutableElement method = TreeUtils.elementFromUse(node);
        TypeElement type = Utils.getMethodClass(method);
        String runsIn = ctx.getMethodRunsIn(method);
        // Ignore constructors, since they should be type checked by the class
        // visitation anyway.

        String methodName = method.getSimpleName().toString();

        if (isMemoryAreaType(type) && ("executeInArea".equals(methodName) || "enter".equals(methodName))) {
            checkExecuteInArea(node);
        } else if (isMemoryAreaType(type) && "newInstance".equals(methodName)) {
            checkNewInstance(node);
        } else if (isMemoryAreaType(type) && "getMemoryArea".equals(methodName)) {
            checkGetMemoryArea(node);
        } else if (isMemoryAreaType(type) && "enterPrivateMemory".equals(methodName)) {
            checkEnterPrivateMemory(node);
        } else if (isManagedMemoryType(type)
                && ("allocInSame".equals(methodName) || "allocInParent".equals(methodName))) {
            checkDynamicGuard(node);
        } else {
            if (!method.getSimpleName().toString().startsWith("<init>")) {

                /*
                 * Method Invocation:
                 *   @Scope(Mission )Foo foo = mission.getFoo();      // OK
                 *           foo.method(bar);                //  ---> OK
                 *           foo.methodErr(bar);             // ERROR: is not @RunsIn(UNKNOWN)
                 *      --> if "foo" is not current, then the method invocation may be only to @RunsIn(UNKNOWN) method!!
                 */

                // TODO: static methods will probably fail here!!

                // TODO: we check only scope on declaration, not the scope of the type!!
                String varScope = getVarScope(node);
                String current = currentAllocScope();

                //pln("\n========: ");
                //pln("\t method : " + method);
                //pln("\t varScope : " + varScope);
                //pln("\t current: " + currentAllocScope());
                //pln("\t runsIn: " + runsIn);

                if (current.equals(UNKNOWN)) {
                    if (runsIn == null || !runsIn.equals(UNKNOWN)) {
                        /* TEST WITH: scope/unknown/UnknownMethod */
                        report(Result.failure(ERR_BAD_METHOD_INVOKE, CURRENT, UNKNOWN), node);
                    }
                }
                else if (current.equals(varScope)) {
                    if (runsIn != null) {
                        if (!(runsIn.equals(current) || (Utils.isAllocFree(method, ats)
                                && scopeTree.isParentOf(current, runsIn)))) {
                            /* TEST WITH: scope/unknown/TestCrossScope */
                            // Can only call methods that run in the same scope.
                            // Allows parent scopes as well, if they are marked @AllocFree.
                            report(Result.failure(ERR_BAD_METHOD_INVOKE, runsIn, current), node);
                        }
                    }
                } else if (runsIn == null) {
                    /* TEST WITH: scope/unknown/TestCrossScope **/
                    report(Result.failure(ERR_BAD_METHOD_INVOKE, varScope, current), node);
                } else if (!runsIn.equals(UNKNOWN)) {
                    /* TEST WITH: scope/unknown/TestCrossScope **/
                    report(Result.failure(ERR_BAD_METHOD_INVOKE, runsIn, current), node);
                }
            }
        }
    }

    private void checkNewInstance(MethodInvocationTree node) {
        // TODO: revisit checking of the "newInstance"!!!!
        ExpressionTree e = node.getMethodSelect();
        ExpressionTree arg = node.getArguments().get(0);
        String varScope = getVarScope(node);
        Element newInstanceType;

        if (arg.getKind() == Kind.MEMBER_SELECT
                && ((MemberSelectTree) arg).getExpression().getKind() == Kind.IDENTIFIER
                && (newInstanceType = TreeUtils.elementFromUse((IdentifierTree) ((MemberSelectTree) arg)
                        .getExpression())).getKind() == ElementKind.CLASS) {
            String instanceScope = ctx.getClassScope((TypeElement) newInstanceType);
            if (instanceScope != null && !varScope.equals(instanceScope)) {
                report(Result.failure(ERR_BAD_NEW_INSTANCE, ((TypeElement) newInstanceType).getQualifiedName(),
                        varScope), node);
            }
        } else {
            // TODO: We only accept newInstance(X.class) right now
        }
    }

    private boolean checkReturnScope(ExpressionTree exprTree, Tree errorNode, String returnScope) {

        debugIndent("check Assignment : ");

        if (isPrimitiveExpression(exprTree)) {
            return true; // primitive assignments are always allowed
        }

        String exprScope = null;
        exprTree = simplify(exprTree);
        Kind exprKind = exprTree.getKind();

        if (exprKind == Kind.NULL_LITERAL || exprKind == Kind.STRING_LITERAL) {
            return true;
        } else if (exprKind == Kind.NEW_ARRAY) {
            // TODO:
            // Handled by visitNewArray
            return true;
        } else if (exprKind == Kind.METHOD_INVOCATION) {
            MethodInvocationTree methodExpr = (MethodInvocationTree) exprTree;
            ExecutableElement methodElem = TreeUtils.elementFromUse(methodExpr);
            AnnotatedExecutableType methodType = atf.getAnnotatedType(methodElem);
            TypeMirror retMirror = methodType.getReturnType().getUnderlyingType();
            while (retMirror.getKind() == TypeKind.ARRAY) {
                retMirror = ((ArrayType) retMirror).getComponentType();
            }
            if (retMirror.getKind().isPrimitive()) {
                exprScope = null;
            } else {
                exprScope = ctx.getClassScope(Utils.getTypeElement(retMirror));
                if (exprScope == null) {
                    exprScope = ctx.getMethodRunsIn(methodElem); // TODO: revisit
                }
                if (exprScope == null) {
                    debugIndent("Expression Scope is NULL, ERR?? :" + methodExpr.getMethodSelect().getKind());
                }
            }
        } else if (exprKind == Kind.NEW_CLASS) {
            /*
             * checks that @Scope(parent) data = @Scope(child) new Object() <--
             * this is assignment error while we are in the child
             */
            if (currentAllocScope() != null && !currentAllocScope().equals(returnScope)) {
                /* checked by scope/scopeReturn/ScopeReturn.java */
                report(Result.failure(ERR_BAD_RETURN_SCOPE, currentAllocScope(), returnScope), errorNode);
            }

            // Handled by visitNewClass
            return true;
        } else if (exprKind == Kind.PLUS) {

        } else if (exprKind == Kind.MEMBER_SELECT || exprKind == Kind.IDENTIFIER) {
            VariableElement var = (VariableElement) TreeUtils.elementFromUse(exprTree);
            exprScope = scope(var);

            if (var.getSimpleName().toString().equals("cs")) {
                System.err.println("Assignment: @Scope(" + returnScope + ") " + returnScope + " = @Scope(" + exprScope
                        + ")");
            }
        } else if (exprKind == Kind.TYPE_CAST) {
            // VariableElement var = (VariableElement)
            // TreeUtils.elementFromUse(exprTree);

            Element el = TreeInfo.symbol((JCTree) exprTree);
            debugIndent("element " + el);
            debugIndent("expr " + exprTree);
            // exprScope = scope(var);

            TypeMirror castType = atf.fromTypeTree(((TypeCastTree) exprTree).getType()).getUnderlyingType();
            while (castType.getKind() == TypeKind.ARRAY) {
                castType = ((ArrayType) castType).getComponentType();
            }
            if (castType.getKind().isPrimitive()) {
                exprScope = returnScope;
            } else {
                exprScope = ctx.getClassScope(Utils.getTypeElement(castType));
            }
        } else {
            throw new RuntimeException("Need a new case for " + exprKind);
        }

        Utils.debugPrintln("Return SCOPES: @Scope(" + returnScope + ") " + " = @Scope(" + exprScope + ")");
        boolean isLegal;

        isLegal = returnScope == null || returnScope.equals(exprScope);
        if (exprScope != null) {
            isLegal = returnScope == null || returnScope.equals(exprScope);
        } else {
            // for the variable that has no annotation
            isLegal = returnScope == null || returnScope.equals(currentAllocScope());
        }

        if (!isLegal) {
            report(Result.failure(ERR_BAD_RETURN_SCOPE, exprScope, returnScope), errorNode);
        }
        return isLegal;
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
            // pln("check :" + var2);
            if (!var1.getAnnotationMirrors().toString().equals(var2.getAnnotationMirrors().toString()))
                return true;
        }
        return false;
    }

    private void checkVariable(VariableTree node, P p) {
        // pln("\n\\n \t >>>Visit Variable :" + node);

        // This assumes that the TypeElement for primitives is null, because
        // primitives have no class bodies.
        Tree nodeTypeTree = node.getType();
        while (nodeTypeTree.getKind() == Kind.ARRAY_TYPE) {
            nodeTypeTree = ((ArrayTypeTree) nodeTypeTree).getType();
        }
        if (nodeTypeTree.getKind() == Kind.PRIMITIVE_TYPE) {
            return;
        }
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        String varScope = scope(TreeUtils.elementFromDeclaration(node));
        Element varEnv = var.getEnclosingElement();

        debugIndent("Variable " + var.getSimpleName() + " is ");

        if (isStatic(var.getModifiers())) {
            Utils.debugPrintln("static (IMMORTAL)");
            // Class variable
            if (varScope != null && !IMMORTAL.equals(varScope)) {
                // If a variable is static, its type should be
                // @Scope(IMMorTAL) or nothing at all
                // pln("static var: " + varScope);

                report(Result.failure(ERR_STATIC_NOT_IMMORTAL), node);
            }
            // We need to set this for visiting the variable initializer.
            // Apparently sometimes the static initializer is optimized into
            // individual initializers?
            currentRunsIn = IMMORTAL;
        } else if (classOrInterface.contains(varEnv.getKind())) {
            // Instance variable
            /* FIELD OF CLASS checking... */
            // pln("\n INSTANCE :" + node);
            // pln("\t  @Scope(" + varScope + ")");

            Utils.debugPrintln("instance @Scope(" + varScope + ")");
            String varEnvScope = Utils.scope(varEnv.getAnnotationMirrors());
            String explicitScope = getExplicitScopeOnField(node);

            // pln("\t \t explicit scope:" + explicitScope);

            /*
             * pln("\n-----\n\t FIELD SCOPE CHECK:" + varScope);
             * pln("\t \t varEnvScope:" + varEnvScope); pln("\t \t node:" +
             * node); pln("\t \t is parent:" + scopeTree.isParentOf(varEnvScope,
             * varScope)); pln("\t \t varScope:" + varScope);
             * pln("\t \t type Scope:" + varTypeScope);
             * pln("\t \t explicit scope:" + explicitScope);
             */

            if (!(var.asType().getKind().isPrimitive() || varEnvScope == null || varScope == null
                    || scopeTree.isParentOf(varEnvScope, varScope) || explicitScope != null)) {
                // Instance fields must be in the same or parent scope as the
                // instance
                report(Result.failure(ERR_BAD_FIELD_SCOPE), node);
            }

            currentRunsIn = varScope;
        } else if (isPrivateMemory(node, p)) {
            // checkDefineScope(node, p);
            // TODO: are we sure that we dont need to check this assignment?
            // TODO: this should implement the getCurrent rules
            return; // we do not need to check assignement

        } else {
            // pln("\n\n-----\n VARIABLE CHECK:" + node);
            // pln("\t varScope ::" + varScope );
            // pln("\t currentAllocScope :: " +
            // currentAllocScope() );

            Utils.debugPrintln("local @Scope(" + varScope + ")");
            /* LOCAL VARIABLE checking... */
            if (varScope != null && !varScope.equals(UNKNOWN) && !scopeTree.isParentOf(currentAllocScope(), varScope)) {
                // @Scopes of local variable types should agree with the current
                // allocation context
                report(Result.failure(ERR_BAD_VARIABLE_SCOPE, atf.fromTypeTree(node.getType()), currentAllocScope()),
                        node);
                // but if it doesn't, assume the variable scope is correct.
                // TODO:
                currentRunsIn = varScope;
            }
        }

        if (node.getInitializer() != null) {
            checkAssignment(node, node.getInitializer(), node);
        }
    }

    private String getExplicitScopeOnField(VariableTree node) {
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        return getScope(var);
    }

    private String getScope(Element element) {
        Scope scope = element.getAnnotation(Scope.class);
        if (scope == null)
            return null;

        return scope.value();
    }

    private String getVarScope(MethodInvocationTree node) {
        ExpressionTree e = node.getMethodSelect();
        String varScope = null;
        switch (e.getKind()) {
            case IDENTIFIER :
                pln("\t\t IDENTIFIER : " + e);
                //ExpressionTree expTree = ((MethodInvocationTree) e).getExpression();
                Element elem = TreeUtils.elementFromUse((IdentifierTree) e);
                //Element element = TreeUtils.elementFromUse(expTree);
                varScope = getScope(elem);

                if (varScope == null) {

                }
                break;
            case MEMBER_SELECT :
                ExpressionTree ee = ((MemberSelectTree) e).getExpression();
                Element el = TreeUtils.elementFromUse(ee);
                varScope = getScope(el);

                //pln("\t\t element : " + el);
                //pln("\t\t\t varScope : " + varScope);
                break;
        }

        if (varScope == null) {
            // TODO: !!!!!!
            // 1. look at the type to see @Scope
            // 2. if no @Scope,
              // 2.1 if this is field, look at enclosing classes(element)
              // 2.2 if this is local var, look at enclosing method

        }

        return varScope;
    }

    private boolean hasRunsIn(Element element) {
        return element.getAnnotation(RunsIn.class) != null;
    }

    // Doesn't work for interfaces (nor should it)
    private boolean isMemoryAreaType(TypeElement t) {
        if (t.getKind() == ElementKind.INTERFACE)
            return false;
        while (!TypesUtils.isDeclaredOfName(t.asType(), "javax.realtime.MemoryArea")
                && !TypesUtils.isObject(t.asType())) {
            t = Utils.getTypeElement(t.getSuperclass());
        }
        return TypesUtils.isDeclaredOfName(t.asType(), "javax.realtime.MemoryArea");
    }

    // Doesn't work for interfaces (nor should it)
    private boolean isManagedMemoryType(TypeElement t) {
        if (t.getKind() == ElementKind.INTERFACE) {
            return false;
        }
        while (!TypesUtils.isDeclaredOfName(t.asType(), "javax.safetycritical.ManagedMemory")
                && !TypesUtils.isObject(t.asType())) {
            t = Utils.getTypeElement(t.getSuperclass());
        }
        return TypesUtils.isDeclaredOfName(t.asType(), "javax.safetycritical.ManagedMemory");
    }

    private boolean isPrimitiveExpression(ExpressionTree expr) {
        if (expr.getKind() == Kind.NEW_ARRAY)
            return false;
        return atf.fromExpression(expr).getKind().isPrimitive();
    }

    private boolean isPrivateMemory(VariableTree node, P p) {
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        return isPrivateMemory(var.asType());
    }

    private boolean isPrivateMemory(TypeMirror asType) {
        return asType.toString().equals("javax.safetycritical.PrivateMemory")
                || asType.toString().equals("javax.safetycritical.ManagedMemory");
    }

    private String currentAllocScope() {
        return currentRunsIn != null ? currentRunsIn : currentScope;
    }

    private String scope(Element var) {
        String varScope = varScope(var);

        if (isStatic(var.getModifiers())) // static
            if (varScope == null || varScope.equals(IMMORTAL)) {
                return IMMORTAL;
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
            return currentAllocScope();
        }
    }

    private String varScope(Element var) {
        /*
         * 1. Look for the @Scope annotation on the variable. For example
         *
         * @Scope(UNKNOWN) Foo foo;
         */
        String varScope = getScope(var);
        // if (varScope != null) {
        // //pln("variable element scope is ::: " + varScope);
        // return varScope;
        // }

        String typeScope = null;
        /*
         * 2. Look for the @Scope annotation on the variable's type. For example
         *
         * @Scope("Foo") class Foo {....}
         */
        AnnotatedTypeMirror exprType = atf.getAnnotatedType(var);
        while (exprType.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType arrayType = (AnnotatedArrayType) exprType;
            exprType = arrayType.getComponentType();
        }
        if (exprType.getKind() == TypeKind.DECLARED) {
            // return scope(elements.getTypeElement(exprType.toString()), null);
            typeScope = ctx.getClassScope((TypeElement) ((AnnotatedDeclaredType) exprType).getUnderlyingType()
                    .asElement());
        }

        if (typeScope != null && varScope != null) {
            if (!typeScope.equals(varScope)) {
                throw new RuntimeException("error.var.and.type.scope.annotation.mismatch");

                // throw new
                // ScopeException("ERROR: Variable is annotated with @Scope(" +
                // varScope +") but" +
                // "its type is anontated with @Scope("+ typeScope
                // +"). The scopes must correspond!");
            }
        }
        if (varScope != null)
            return varScope;

        return typeScope;
    }

    private String directRunsIn(VariableElement var) {
        AnnotatedTypeMirror exprType = atf.getAnnotatedType(var);
        // TODO: since the exprType sometimes does not contains the annotations
        // if this does not work, we use the loop below...
        if (exprType.getKind() == TypeKind.DECLARED) {
            Element type = ((AnnotatedDeclaredType) exprType).getUnderlyingType().asElement();
            if (hasRunsIn(type))
                return runsIn(type);
        }

        while (!TypesUtils.isObject(exprType.getUnderlyingType())) {
            String exprScope = Utils.runsIn(exprType.getAnnotations());
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
    private String getRunsInFromRunnable(TypeMirror var) {
        String result = null;
        // TypeElement myType = context.getTypeElement(var.asType().toString());
        TypeElement myType = Utils.getTypeElement(elements, var.toString());
        List<? extends Element> elements = myType.getEnclosedElements();
        // search for run() method:
        for (Element el : elements)
            if (el.getSimpleName().toString().equals("run"))
                result = runsIn(el);
        return result;
    }

    private String runsIn(Element type) {
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            if (TypesUtils.isDeclaredOfName(mirror.getAnnotationType(), "javax.safetycritical.annotate.RunsIn")) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> vals = mirror.getElementValues();
                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : vals.entrySet()) {
                    if ("value".equals(e.getKey().getSimpleName().toString())) {
                        return e.getValue().getValue().toString();
                    }
                }
            }
        }
        return null;
    }

    private String scopeDef(VariableElement var) {
        for (AnnotationMirror mirror : var.getAnnotationMirrors()) {
            if (TypesUtils.isDeclaredOfName(mirror.getAnnotationType(), "javax.safetycritical.annotate.DefineScope")) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> vals = mirror.getElementValues();
                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : vals.entrySet()) {
                    if ("name".equals(e.getKey().getSimpleName().toString())) {
                        return e.getValue().getValue().toString();
                    }
                }
            }
        }
        return null;
    }

    // Returns the variable corresponding to the left-hand side of an
    // assignment. If the LHS is an array access, it
    // returns the array variable.
    private VariableElement lhsToVariable(Tree lhs) {
        while (lhs.getKind() == Kind.ARRAY_ACCESS) {
            lhs = ((ArrayAccessTree) lhs).getExpression();
        }
        if (lhs.getKind() == Kind.VARIABLE) {
            return TreeUtils.elementFromDeclaration((VariableTree) lhs);
        } else {
            // pln("left-hand side: " + lhs);

            Element elem = TreeUtils.elementFromUse((ExpressionTree) lhs);
            if (elem.getKind() == ElementKind.FIELD || elem.getKind() == ElementKind.LOCAL_VARIABLE
                    || elem.getKind() == ElementKind.PARAMETER) {
                return (VariableElement) elem;
            } else {
                return null;
            }
        }
    }

    // Strips out unnecessary parts of the AST in the RHS of an assignment
    private ExpressionTree simplify(ExpressionTree exprTree) {
        while (true) {
            exprTree = TreeUtils.skipParens(exprTree);
            switch (exprTree.getKind()) {
                case ARRAY_ACCESS :
                    exprTree = ((ArrayAccessTree) exprTree).getExpression();
                    break;
                case ASSIGNMENT :
                    exprTree = ((AssignmentTree) exprTree).getExpression();
                    break;
                    /*
                     * case TYPE_CAST: exprTree = ((TypeCastTree)
                     * exprTree).getExpression(); break;
                     */
                default :
                    return exprTree;
            }
        }
    }

    public void report(Result r, Object src) {
        if (src != null) {
            checker.report(r, src);
        }
    }

    /*
     * Debug/helper methods
     */

    private String indent = "";

    private void debugIndentDecrement() {
        indent = indent.substring(1);
    }

    private void debugIndentIncrement(String method) {
        Utils.debugPrintln(indent + method);
        indent += " ";
    }

    private void debugIndent(String method) {
        Utils.debugPrintln(indent + method);
    }
}
