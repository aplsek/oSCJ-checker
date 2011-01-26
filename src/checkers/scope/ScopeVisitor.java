package checkers.scope;

import static checkers.Utils.isStatic;

import static checkers.scjAllowed.EscapeMap.escapeAnnotation;
import static checkers.scjAllowed.EscapeMap.escapeEnum;

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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import checkers.Utils;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypes;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.TreeUtils;
import checkers.util.TypesUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

//TODO: Defaults for @RunsIn/@Scope on specific SCJ classes
//TODO: Unscoped method parameters?
//TODO: Anonymous runnables
//TODO: Errors for using annotated classes in unannotated classes
//TODO: Add illegal scope location errors back in

/**
 * 
 * @Scope("FooMission") RunsIn is implied to be FooMission class FooMission
 * 
 *                      We can no longer infer the scope we need to add based on
 *                      the annotations. Thus, each mission must have a @DefineScope
 *                      on it now, and if it doesn't it's assumed to add a scope
 *                      name with the name of the mission and the parent as
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
public class ScopeVisitor<R, P> extends SourceVisitor<R, P> {

    private AnnotatedTypeFactory atf;
    private AnnotatedTypes ats;
    private String currentScope = null;
    private String currentRunsIn = null;
    private ScopeCheckerContext context;
    private static EnumSet<ElementKind> classOrInterface = EnumSet.of(
            ElementKind.CLASS, ElementKind.INTERFACE);

    public ScopeVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext context) {
        super(checker, root);

        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
        this.context = context;

    }

    @Override
    public R visitAssignment(AssignmentTree node, P p) {
        try {
            debugIndentIncrement("visitAssignment : " + node);

            checkAssignment(node.getVariable(), node.getExpression(), node);
            return super.visitAssignment(node, p);
        } catch (ScopeException e) {
            Utils.debugPrintException(e);
            return null;
        } finally {
            debugIndentDecrement();
        }
    }

    @Override
    public R visitBlock(BlockTree node, P p) {
        debugIndentIncrement("visitBlock");
        String oldRunsIn = currentRunsIn;
        if (node.isStatic()) {
            currentRunsIn = "immortal";
        }
        R r = super.visitBlock(node, p);
        currentRunsIn = oldRunsIn;
        debugIndentDecrement();
        return r;
    }

    @Override
    public R visitClass(ClassTree node, P p) {
        debugIndentIncrement("\nvisitClass " + node.getSimpleName());
        debugIndent("\nvisitClass " + node.toString());
        debugIndent("\nvisitClass :"
                + TreeUtils.elementFromDeclaration(node).getQualifiedName());

        if (escapeEnum(node) || escapeAnnotation(node)) {
            debugIndentDecrement();
            return null;
        }

        // DEBUG::
        /*
         * if (node.getSimpleName().toString().equals("Reducer")) {
         * System.out.println("my class"); Utils.DEBUG = true; } else
         * Utils.DEBUG = false;
         */

        /**
         * SCOPE TREE Verification
         */
        if (!ScopeTree.verifyScopeTree()) {
            ScopeTree.reportErrors(this);
            debugIndentDecrement();
            return null;
        }

        // verifying ScopeTree:
        // System.out.println("\n\n\n\nVerify SCOPE:");
        // ScopeTree.printTree();

        TypeElement t = TreeUtils.elementFromDeclaration(node);
        String oldScope = currentScope;
        String oldRunsIn = currentRunsIn;

        try {

            // context.printContext();

            String scope = context.getScope(t.getQualifiedName().toString());
            String runsIn = context.getRunsIn(t.getQualifiedName().toString());

            if (runsIn != null && scope == null) {
                throw new ScopeException(
                "Class may not have @RunsIn annotation with no @Scope annotation.");
            }

            // check parent-child relationship between scope/runsIn
            if (runsIn != null && scope != null)
                if (!ScopeTree.isParentOf(runsIn, scope))
                    /** TESTED BY: scope/TestRunsIn2.java **/
                    report(Result.failure("scope.runs.in.disagreement"), node);

            if (runsIn == null && scope == null) {
                /**
                 * TODO: The correct behavior for visiting unannotated classes
                 * is to make sure the class doesn't mention any annotated
                 * classes to ensure that there is no leakage.
                 **/
                // / System.out.println("NO ANNOTATIONS FOUND!!!");
            }

            // TODO: assume defaults for inner classes?
            Utils.debugPrintln("Seen class " + t.getQualifiedName()
                    + ": @Scope(" + scope + ") @RunsIn(" + runsIn + ")");

            currentScope = scope;
            currentRunsIn = runsIn;

            return super.visitClass(node, p);
        } catch (ScopeException e) {

            Utils.debugPrintException(e);
            checker.report(Result.failure(e.getMessage()), node);
            return null;
        } finally {
            currentScope = oldScope;
            currentRunsIn = oldRunsIn;
            debugIndentDecrement();
        }
    }

    @Override
    public R visitMemberSelect(MemberSelectTree node, P p) {
        Element elem = TreeUtils.elementFromUse(node);

        // System.out.println("\n member select:" + node.toString());
        // System.out.println("element:" + elem);
        // System.out.println("element:" + elem.getKind());

        try {
            debugIndentIncrement("visitMemberSelect : " + node.toString());
            if (elem.getKind() == ElementKind.FIELD) {
                // System.out.println("   is field!!:" + elem);
                TypeElement type = (TypeElement) elem.getEnclosingElement();

                // System.out.println("   type - kind:" +
                // elem.asType().getKind());
                String typeScope = context.getScope(type);
                // System.out.println("   typ scope :" + typeScope);
                // System.out.println("   scope elem :" +
                // scope((VariableElement) elem));

                // The field is a reference type with no @Scope and its owner is
                // not allocated in the same scope
                // as the current allocation context
                if (elem.asType().getKind() == TypeKind.DECLARED
                        && scope((VariableElement) elem) == null
                        && typeScope != null
                        && typeScope.equals(currentAllocScope())) {
                    // Can't reference a field that has an unannotated type
                    // unless it's in the same scope
                    report(Result.failure("escaping.nonannotated.field"), node);
                }
            }
            return super.visitMemberSelect(node, p);
        } catch (ScopeException e) {
            Utils.debugPrintException(e);
            return null;
        } finally {
            debugIndentDecrement();
        }
    }

    @Override
    public R visitMethod(MethodTree node, P p) {

        debugIndentIncrement("visitMethod " + node.getName());
        ExecutableElement method = TreeUtils.elementFromDeclaration(node);

        debugIndent("Seen method " + method.getSimpleName());
        debugIndent("RUNS IN: " + currentRunsIn);
        debugIndent("scope: " + currentScope);

        TypeElement type = (TypeElement) method.getEnclosingElement();

        checkWellFormed(node);

        // run() requires a @RunsIn annotation on concrete classes that
        // implement Runnable
        // TODO: check super class interfaces?
        if (type.getKind() == ElementKind.CLASS) {
            boolean isRunnable = false;
            while (type != null) {
                for (TypeMirror iface : type.getInterfaces()) {
                    TypeElement ifaceType = (TypeElement) ((DeclaredType) iface)
                    .asElement();
                    if (TypesUtils.isDeclaredOfName(ifaceType.asType(),
                    "java.lang.Runnable")) {
                        isRunnable = true;
                        break;
                    }
                }
                type = Utils.superType(type);
            }
            if (isRunnable && "run".equals(method.getSimpleName().toString())
                    && method.getParameters().size() == 0
                    && Utils.runsIn(method.getAnnotationMirrors()) != null) {
                report(Result.failure("bad.runs.in.override"), node);
            }
        }
        String oldRunsIn = currentRunsIn;
        R r = null;
        try {
            String runsIn = context.getRunsIn(method);
            debugIndent("@RunsIn(" + runsIn + ") " + method.getSimpleName());
            // TODO: If we don't have an allocation context, we need to skip the
            // method?
            if (runsIn != null)
                currentRunsIn = runsIn;
            if (currentAllocScope() != null)
                r = super.visitMethod(node, p);
        } catch (ScopeException e) {
            Utils.debugPrintException(e);
            report(Result.failure(e.getMessage()), node);
        } finally {
            currentRunsIn = oldRunsIn;
        }
        debugIndentDecrement();

        if (r == null)
            debugIndent("Skipping the method: " + method.getSimpleName());

        return r;
        // TODO: Do methods have any restrictions on return types?
    }

    /**
     * Checks that visited method has a well-formed annotations
     * 
     * RunsIn is not legal on constructors.
     * 
     * a method cannot have a @Scope annotation
     * 
     * 
     * @param node
     */
    private void checkWellFormed(MethodTree node) {
        ExecutableElement method = TreeUtils.elementFromDeclaration(node);
        if (TreeUtils.isConstructor(node) && hasRunsIn(method))
            report(Result.failure("runs.in.on.ctor"), node);
        if (hasScope(method))
            report(Result.failure("scope.on.method"), node);

    }

    private boolean hasScope(Element element) {
        return element.getAnnotation(Scope.class) != null;
    }

    private boolean hasRunsIn(Element element) {
        return element.getAnnotation(RunsIn.class) != null;
    }

    private boolean hasRunsIn(MethodTree node) {
        ExecutableElement var = TreeUtils.elementFromDeclaration(node);
        for (AnnotationMirror ann : var.getAnnotationMirrors())
            if (ann.getAnnotationType().toString()
                    .equals("javax.safetycritical.annotate.RunsIn"))
                return true;
        return false;
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {

        try {
            debugIndentIncrement("visitMethodInvocation");

            checkMethodInvocation(node, p);
            return super.visitMethodInvocation(node, p);
        } catch (ScopeException e) {
            Utils.debugPrintException(e);
            return null;
        } finally {
            debugIndentDecrement();
        }
    }

    private void checkMethodInvocation(MethodInvocationTree node, P p)
    throws ScopeException {
        ExecutableElement method = TreeUtils.elementFromUse(node);
        TypeElement type = (TypeElement) method.getEnclosingElement();
        String runsIn = context.getRunsIn(method);
        // Ignore constructors, since they should be type checked by the class
        // visitation anyway.
        String methodName = method.getSimpleName().toString();
        if (isMemoryAreaType(type)
                && ("executeInArea".equals(methodName) || "enter"
                        .equals(methodName))) {
            ExpressionTree e = node.getMethodSelect();
            ExpressionTree arg = node.getArguments().get(0);
            String argRunsIn = null;

            // TODO: Single exit point sure would be nicer
            switch (arg.getKind()) {
            case IDENTIFIER:
                argRunsIn = directRunsIn((VariableElement) TreeUtils
                        .elementFromUse((IdentifierTree) arg));
                // System.out.println("argument var " + (VariableElement)
                // TreeUtils.elementFromUse((IdentifierTree) arg));

                break;
            case MEMBER_SELECT:
                Element tmp = TreeUtils.elementFromUse((MemberSelectTree) arg);
                if (tmp.getKind() != ElementKind.FIELD) {
                    report(Result.failure("bad.enter.parameter"), arg);
                    return;
                } else {
                    argRunsIn = directRunsIn((VariableElement) tmp);
                }
                break;
            case NEW_CLASS:
                ExecutableElement ctor = (ExecutableElement) TreeUtils
                .elementFromUse((NewClassTree) arg);
                argRunsIn = context.getRunsIn((TypeElement) ctor
                        .getEnclosingElement());
                break;
            default:
                report(Result.failure("bad.enter.parameter"), arg);
                return;
            }

            if (argRunsIn == null) {
                // All Runnables used with executeInArea/enter should have
                // @RunsIn
                // TODO: Prevent Runnables from having their run override
                // annotations?
                report(Result.failure("runnable.without.runsin"), node);
            } else {
                String varScope = null;
                switch (e.getKind()) {
                case IDENTIFIER:
                    // TODO: This only happens for this/super constructor calls
                    // or implicit this.method calls. How do we
                    // handle this.method? Do we need to?
                    varScope = scopeDef((VariableElement) TreeUtils
                            .elementFromUse((IdentifierTree) e));
                    break;
                case MEMBER_SELECT:
                    varScope = getScopeDef(((MemberSelectTree) e)
                            .getExpression());
                    break;
                }
                if (varScope == null || !varScope.equals(argRunsIn)) {
                    // The Runnable and the PrivateMemory must have agreeing
                    // scopes
                    report(Result.failure("bad.executeInArea.or.enter"), node);
                }
                if ("executeInArea".equals(methodName)
                        && !ScopeTree.isParentOf(currentAllocScope(), varScope)) {
                    report(Result.failure("bad.executeInArea.target"), node);
                } else if ("enter".equals(methodName)
                        && !ScopeTree.isParentOf(varScope, currentAllocScope())) {
                    report(Result.failure("bad.enter.target"), node);
                }
            }
        } else if (isMemoryAreaType(type) && "newInstance".equals(methodName)) {

            ExpressionTree e = node.getMethodSelect();
            ExpressionTree arg = node.getArguments().get(0);
            String varScope = null;
            switch (e.getKind()) {
            case IDENTIFIER:
                varScope = scopeDef((VariableElement) TreeUtils
                        .elementFromUse((IdentifierTree) e));
                break;
            case MEMBER_SELECT:
                varScope = getScopeDef(((MemberSelectTree) e).getExpression());
                break;
            }

            Element newInstanceType;

            if (arg.getKind() == Kind.MEMBER_SELECT
                    && ((MemberSelectTree) arg).getExpression().getKind() == Kind.IDENTIFIER
                    && (newInstanceType = TreeUtils
                            .elementFromUse((IdentifierTree) ((MemberSelectTree) arg)
                                    .getExpression())).getKind() == ElementKind.CLASS) {
                String instanceScope = context
                .getScope((TypeElement) newInstanceType);
                if (instanceScope != null && !varScope.equals(instanceScope)) {
                    report(Result.failure("bad.newInstance",
                            ((TypeElement) newInstanceType).getQualifiedName(),
                            varScope), node);
                }
            } else {
                // TODO: We only accept newInstance(X.class) right now
            }
        } else if (isMemoryAreaType(type)
                && "enterPrivateMemory".equals(methodName)) {

            // TODO:
            method.getParameters();
            System.out.println("Parameters: " + method.getParameters());
            List<? extends VariableElement> params = method.getParameters();

            VariableElement runnable = params.get(1);

            runnable.getEnclosingElement();
            runnable.getEnclosedElements();
            System.out.println("Runnable:" + runnable.getKind().getClass());
            System.out.println("Runnable:" + runnable.getKind());
            System.out.println("Runnable:" + runnable.asType());
            List<? extends ExpressionTree> arg = node.getArguments();

            ExpressionTree ex = arg.get(1);
            System.out.println("arg:" + ex);
            Element xxx = TreeUtils.elementFromUse(ex);
            System.out.println("element:" + xxx.asType());
            // TODO: get runnable class

            // TODO: get runnable instantiation variable and its DefineScope

        } else if (!method.getSimpleName().toString().startsWith("<init>")
                && runsIn != null) {
            if (!(runsIn.equals(currentAllocScope()) || (Utils.isAllocFree(
                    method, ats) && ScopeTree.isParentOf(currentAllocScope(),
                            runsIn)))) {
                // Can only call methods that run in the same scope. Allows
                // parent scopes as well, if
                // they are marked @AllocFree.
                report(Result.failure("bad.method.invoke", runsIn,
                        currentAllocScope()), node);
            }
            // for (int i = 0, args = node.getArguments().size(); i < args; i++)
            // {
            // ExpressionTree arg = node.geta
            // }
        }
    }

    @Override
    public R visitNewArray(NewArrayTree node, P p) {
        try {
            debugIndentIncrement("visitNewArray");
            AnnotatedArrayType arrayType = atf
            .getAnnotatedType((NewArrayTree) node);
            while (arrayType.getComponentType().getKind() == TypeKind.ARRAY) {
                arrayType = (AnnotatedArrayType) arrayType.getComponentType();
            }
            AnnotatedTypeMirror componentType = arrayType.getComponentType();
            if (!componentType.getKind().isPrimitive()) {
                TypeElement t = elements.getTypeElement(componentType
                        .getUnderlyingType().toString());
                String scope = context.getScope(t);
                if (!(scope == null || scope.equals(currentAllocScope()))) {
                    report(Result.failure("bad.allocation",
                            currentAllocScope(), scope), node);
                }
            }
            return super.visitNewArray(node, p);
        } catch (ScopeException e) {
            Utils.debugPrintException(e);
            return null;
        } finally {
            debugIndentDecrement();
        }
    }

    /**
     * Object allocation is only allowed if the current allocation context is
     * the same scope as what's defined by the class.
     */
    @Override
    public R visitNewClass(NewClassTree node, P p) {
        try {
            debugIndentIncrement("visitNewClass");
            ExecutableElement ctorElement = TreeUtils.elementFromUse(node);
            String nodeClassScope = context.getScope((TypeElement) ctorElement
                    .getEnclosingElement());
            if (nodeClassScope != null
                    && !currentAllocScope().equals(nodeClassScope)) {
                // Can't call new unless the type has the same scope as the
                // current context
                report(Result.failure("bad.allocation", currentAllocScope(),
                        nodeClassScope), node);
            }
            // TODO: what is this? (Ales)
            // System.err.println(nodeClassScope + " =? " +
            // currentAllocScope());
            return super.visitNewClass(node, p);
        } catch (ScopeException e) {
            Utils.debugPrintException(e);
            return null;
        } finally {
            debugIndentDecrement();
        }
    }

    /**
     * - Static variables must always be in the immortal scope. - Instance
     * variables must make sure that the scope of the enclosing class is a child
     * scope of the variable type's scope. - Local variables are similar to
     * instance variables, only it must first use the @RunsIn annotation, if any
     * exists, before using the @Scope annotation on the class that it belongs
     * to.
     */
    @Override
    public R visitVariable(VariableTree node, P p) {
        String oldScope = currentScope;
        String oldRunsIn = currentRunsIn;
        try {
            debugIndentIncrement("visitVariable : " + node.toString());

            checkVariable(node, p);
            return super.visitVariable(node, p);
        } catch (ScopeException e) {
            Utils.debugPrintException(e);
            return null;
        } finally {
            currentScope = oldScope;
            currentRunsIn = oldRunsIn;
            debugIndentDecrement();
        }
    }

    /**
     * When casting, we must consider also @Scope allocation of each class.
     * 
     */
    public R visitTypeCast(TypeCastTree node, P p) {

        if (escapeTypes(node.getType())) {
            return null;
        }

        String exprScope = null;
        String typeScope = null;

        debugIndentIncrement("visitTypeCast " + node);
        // debugIndent("type " + node.getType());
        // debugIndent("kind : " + node.getExpression().getKind());
        // debugIndent("type2 : " + node.getType());

        // 1. get type and its scope
        Kind typeKind = node.getType().getKind();
        if (typeKind == Kind.IDENTIFIER) {
            Element var = TreeInfo.symbol((JCTree) node.getType());
            typeScope = Utils.scope(var.getAnnotationMirrors());
        } else if (typeKind == Kind.ARRAY_TYPE) {
            Tree nodeTypeTree = ((ArrayTypeTree) node.getType()).getType();
            Element var = TreeInfo.symbol((JCTree) nodeTypeTree);
            typeScope = Utils.scope(var.getAnnotationMirrors());
        } else {
            debugIndent("ERROR : kind :" + node.getType().getKind());
            // typeScope = Utils.scope(type.getAnnotationMirrors());

            // ERROR : no other kind of expression is possible here
        }

        // 2. here is the expression:
        Kind exprKind = node.getExpression().getKind();
        if (exprKind == Kind.NEW_CLASS) {
            ExpressionTree expr = node.getExpression();
            ExecutableElement ctor = (ExecutableElement) TreeUtils
            .elementFromUse((NewClassTree) expr);
            TypeElement expressionType = (TypeElement) ctor
            .getEnclosingElement();
            exprScope = Utils.annotationValue(
                    expressionType.getAnnotationMirrors(),
            "javax.safetycritical.annotate.Scope");

        } else if (exprKind == Kind.METHOD_INVOCATION) {
            // debugIndent("kind : " + node.getExpression().getKind());
            // debugIndent("type2 : " + node.getType());

            MethodInvocationTree methodExpr = (MethodInvocationTree) node
            .getExpression();
            ExecutableElement methodElem = TreeUtils.elementFromUse(methodExpr);
            AnnotatedExecutableType methodType = atf
            .getAnnotatedType(methodElem);
            TypeMirror retMirror = methodType.getReturnType()
            .getUnderlyingType();
            while (retMirror.getKind() == TypeKind.ARRAY) {
                retMirror = ((ArrayType) retMirror).getComponentType();
            }

            // debugIndent("exec : " + methodElem);
            // debugIndent("met type : " + methodType);
            // debugIndent("met mirror : " + retMirror);

            try {
                exprScope = context
                .getScope((TypeElement) ((DeclaredType) retMirror)
                        .asElement());
            } catch (ScopeException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if (exprScope == null) {
                try {
                    exprScope = context.getRunsIn(methodElem);
                } catch (ScopeException e) {
                    e.printStackTrace();
                } // TODO: revisit
            }
            if (exprScope == null) {
                debugIndent("Expression Scope ERROR? : "
                        + methodExpr.getMethodSelect().getKind());
            }
        } else if (exprKind == Kind.MEMBER_SELECT
                || exprKind == Kind.IDENTIFIER) {
            VariableElement var = (VariableElement) TreeUtils
            .elementFromUse(node.getExpression());
            try {
                exprScope = scope(var);
            } catch (ScopeException e) {
                System.err
                .println("ERROR: type of class cast can not be resolved!!!!");
                e.printStackTrace();
            }
        } else {
            exprScope = null;
            debugIndent("ERROR  : kind is : " + exprKind);
        }

        debugIndent("typescope  " + typeScope);
        debugIndent("exprScope  " + exprScope);

        if (exprScope != null)
            if (typeScope == null) {
                debugIndent("curr : " + currentAllocScope());
                if (!exprScope.equals(currentAllocScope()))
                    report(Result.failure("scope.cast", exprScope,
                            currentAllocScope()), node);
            } else if (!exprScope.equals(typeScope))
                report(Result.failure("scope.cast", exprScope, typeScope), node);

        debugIndentDecrement();
        return super.visitTypeCast(node, p);
    }

    private boolean escapeTypes(Tree type) {
        if (type.toString().startsWith("float"))
            return true;
        else if (type.toString().startsWith("byte"))
            return true;
        else if (type.toString().startsWith("int"))
            return true;
        else if (type.toString().startsWith("double"))
            return true;
        else if (type.toString().startsWith("short"))
            return true;
        else if (type.toString().startsWith("char"))
            return true;
        else if (type.toString().startsWith("boolean"))
            return true;
        else if (type.toString().startsWith("long"))
            return true;
        return false;
    }

    private void checkVariable(VariableTree node, P p) throws ScopeException {

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
            Utils.debugPrintln("static (immortal)");
            // Class variable
            if (varScope != null && !"immortal".equals(varScope)) {
                // If a variable is static, its type should be
                // @Scope("immortal") or nothing at all
                report(Result.failure("static.not.immortal"), node);
            }
            // We need to set this for visiting the variable initializer.
            // Apparently sometimes the static initializer is optimized into
            // individual initializers?
            currentRunsIn = "immortal";
        } else if (classOrInterface.contains(varEnv.getKind())) {
            // Instance variable
            Utils.debugPrintln("instance @Scope(" + varScope + ")");
            String varEnvScope = Utils.scope(varEnv.getAnnotationMirrors());
            if (!(var.asType().getKind().isPrimitive() || varEnvScope == null
                    || varScope == null || ScopeTree.isParentOf(varEnvScope,
                            varScope))) {
                // Instance fields must be in the same or parent scope as the
                // instance
                report(Result.failure("bad.field.scope"), node);
            }
            currentRunsIn = varScope;
        } else if (isPrivateMemory(node, p)) {
            checkDefineScope(node, p);

            return; // we do not need to check assignement

        } else {
            Utils.debugPrintln("local @Scope(" + varScope + ")");
            // Local variable
            if (varScope != null
                    && !ScopeTree.isParentOf(currentAllocScope(), varScope)) {
                // @Scopes of local variable types should agree with the current
                // allocation context
                report(Result.failure("bad.variable.scope",
                        atf.fromTypeTree(node.getType()), currentAllocScope()),
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

    private boolean isPrivateMemory(VariableTree node, P p) {
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        if (isPrivateMemory(var.asType()))
            return true;
        return false;
    }

    /**
     * 1. For each @DefineScope on a PrivateMemory variable, first verify that
     * the scope/parent pair is in the scope tree (we don't do this in the
     * DefineScopeVisitor because the tree is incomplete until the end),
     * 
     * 2. and then that the @DefineScope is valid. think the syntax will usually
     * be: something like
     * 
     * @DefineScope(name=x, parent=y) PrivateMemory mem =
     *                      ManagedMemory.getCurrentManagedMemory() (actually
     *                      this method returns ManagedMemory so this requires a
     *                      little more thinking). We have to check that x is
     *                      the current allocation context where it is called
     *                      (determined by currentAllocContext()).
     * 
     * @param node
     * @param p
     */
    private void checkDefineScope(VariableTree node, P p) {
        DefineScope scopeDef = getDefineScope(node);
        if (scopeDef != null) {
            if (!ScopeTree.hasScope(scopeDef.name)) {
                // its not in ScopeTree!! ERROR
                // should never be here!!
                report(Result.failure("checker.bug",
                "ERROR: ScopeVisitor.checkDefineScope error behavior"),
                node);
            }
            if (!currentAllocScope().toString().equals(scopeDef.name))
                report(Result.failure("bad.enterPrivateMem.defineScope",
                        currentScope.toString(), scopeDef.name), node);

            // we verify that this @DefineScope is well formed??
            if (!ScopeTree.get(scopeDef.name).equals(scopeDef.parent))
                /** tested by TestGetCurrentManMem3.java */
                report(Result.failure("bad.enterPrivateMem.defineScope.parent",
                        currentScope.toString(), scopeDef.name), node);

        } else
            report(Result.failure("bad.enterPrivateMem.no.defineScope",
                    currentScope.toString()), node);
        // this should be detected by the DefineScopeChecker anyway !!
    }

    class DefineScope {
        String name;
        String parent;
    }

    private DefineScope getDefineScope(VariableTree node) {
        DefineScope result = null;

        VariableElement var = TreeUtils.elementFromDeclaration(node);

        for (AnnotationMirror ann : var.getAnnotationMirrors()) {
            if (ann.getAnnotationType().toString()
                    .equals("javax.safetycritical.annotate.DefineScope")) {

                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : ann
                        .getElementValues().entrySet()) {
                    if ("name()".equals(entry.getKey().toString())) {
                        result = new DefineScope();
                        result.name = (String) entry.getValue().getValue();
                    } else if ("parent()".equals(entry.getKey().toString())) {
                        result.parent = (String) entry.getValue().getValue();
                    }
                }
            }
        }
        return result;
    }

    private boolean isPrivateMemory(TypeMirror asType) {
        if (asType.toString().equals("javax.safetycritical.PrivateMemory"))
            return true;
        if (asType.toString().equals("javax.safetycritical.ManagedMemory"))
            return true;
        return false;
    }

    private String currentAllocScope() {
        return currentRunsIn != null ? currentRunsIn : currentScope;
    }

    private String scope(VariableElement var) throws ScopeException {
        if (isStatic(var.getModifiers())) { // static
            return "immortal";
        }

        String varScope = varScope(var);
        if (varScope != null) {
            return varScope;
        }

        // Variable's type is not annotated, so go by the enclosing environment.
        if (classOrInterface.contains(var.getEnclosingElement().getKind())) { // instance
            return context.getScope((TypeElement) var.getEnclosingElement());
        } else { // local
            return currentAllocScope();
        }
    }

    private String varScope(VariableElement var) throws ScopeException {
        AnnotatedTypeMirror exprType = atf.getAnnotatedType(var);
        while (exprType.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType arrayType = (AnnotatedArrayType) exprType;
            exprType = arrayType.getComponentType();
        }
        if (exprType.getKind() == TypeKind.DECLARED) {
            // return scope(elements.getTypeElement(exprType.toString()), null);
            return context
            .getScope((TypeElement) ((AnnotatedDeclaredType) exprType)
                    .getUnderlyingType().asElement());
        }
        return null;
    }

    private String directRunsIn(VariableElement var) {
        AnnotatedTypeMirror exprType = atf.getAnnotatedType(var);
        // TODO: since the exprType sometimes does not contains the annotations
        // if this does not work, we use the loop below...
        if (exprType.getKind() == TypeKind.DECLARED) {
            Element type = ((AnnotatedDeclaredType) exprType)
            .getUnderlyingType().asElement();
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

    private String runsIn(Element type) {
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            if (TypesUtils.isDeclaredOfName(mirror.getAnnotationType(),
            "javax.safetycritical.annotate.RunsIn")) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> vals = mirror
                .getElementValues();
                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : vals
                        .entrySet()) {
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
            if (TypesUtils.isDeclaredOfName(mirror.getAnnotationType(),
            "javax.safetycritical.annotate.DefineScope")) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> vals = mirror
                .getElementValues();
                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : vals
                        .entrySet()) {
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
            Element elem = TreeUtils.elementFromUse((ExpressionTree) lhs);
            if (elem.getKind() == ElementKind.FIELD
                    || elem.getKind() == ElementKind.LOCAL_VARIABLE
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
            case ARRAY_ACCESS:
                exprTree = ((ArrayAccessTree) exprTree).getExpression();
                break;
            case ASSIGNMENT:
                exprTree = ((AssignmentTree) exprTree).getExpression();
                break;
                /*
                 * case TYPE_CAST: exprTree = ((TypeCastTree)
                 * exprTree).getExpression(); break;
                 */
            default:
                return exprTree;
            }
        }
    }

    private boolean checkAssignment(Tree varTree, ExpressionTree exprTree,
            Tree errorNode) throws ScopeException {

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
            AnnotatedExecutableType methodType = atf
            .getAnnotatedType(methodElem);
            TypeMirror retMirror = methodType.getReturnType()
            .getUnderlyingType();
            while (retMirror.getKind() == TypeKind.ARRAY) {
                retMirror = ((ArrayType) retMirror).getComponentType();
            }
            if (retMirror.getKind().isPrimitive()) {
                exprScope = varScope; // primitives are always assignable
            } else {
                exprScope = context
                .getScope((TypeElement) ((DeclaredType) retMirror)
                        .asElement());
                if (exprScope == null) {
                    exprScope = context.getRunsIn(methodElem); // TODO: revisit
                }
                if (exprScope == null) {
                    debugIndent("Expression Scope is NULL, ERR?? :"
                            + methodExpr.getMethodSelect().getKind());
                }
            }
        } else if (exprKind == Kind.NEW_CLASS) {

            /**
             * checks that @Scope(parent) data = @Scope(child) new Object() <--
             * this is assignment error while we are in the child
             */
            if (currentAllocScope() != null
                    && !currentAllocScope().equals(varScope)) {
                /** checked by TestScopeCheck2.java */
                report(Result.failure("bad.assignment.scope",
                        currentAllocScope(), varScope), errorNode);
            }

            // Handled by visitNewClass
            return true;
        } else if (exprKind == Kind.PLUS) {

        } else if (exprKind == Kind.MEMBER_SELECT
                || exprKind == Kind.IDENTIFIER) {

            VariableElement var = (VariableElement) TreeUtils
            .elementFromUse(exprTree);
            exprScope = scope(var);

            if (var.getSimpleName().toString().equals("cs")) {
                System.err.println("Assignment: @Scope(" + varScope + ") "
                        + varName + " = @Scope(" + exprScope + ")");
            }

            VariableElement var1 = (VariableElement) TreeUtils
            .elementFromUse(exprTree);
            VariableElement var2 = lhsToVariable(varTree);
            if (checkForPrivateMemAssignementError(var1, var2)) {
                /** checked by TestPrivateMemoryAssignment */
                report(Result.failure("bad.assignment.private.mem", exprScope,
                        varScope), errorNode);
            }

        } else if (exprKind == Kind.TYPE_CAST) {

            // VariableElement var = (VariableElement)
            // TreeUtils.elementFromUse(exprTree);

            Element el = TreeInfo.symbol((JCTree) exprTree);
            debugIndent("element " + el);
            debugIndent("expr " + exprTree);
            // exprScope = scope(var);

            TypeMirror castType = atf.fromTypeTree(
                    ((TypeCastTree) exprTree).getType()).getUnderlyingType();
            while (castType.getKind() == TypeKind.ARRAY) {
                castType = ((ArrayType) castType).getComponentType();
            }
            if (castType.getKind().isPrimitive()) {
                exprScope = varScope;
            } else {
                exprScope = context
                .getScope((TypeElement) ((DeclaredType) castType)
                        .asElement());
            }

        } else {
            throw new RuntimeException("Need a new case for " + exprKind);
        }

        Utils.debugPrintln("Assignment: @Scope(" + varScope + ") " + varName
                + " = @Scope(" + exprScope + ")");
        boolean isLegal;

        isLegal = varScope == null || varScope.equals(exprScope);
        if (exprScope != null)
            isLegal = varScope == null || varScope.equals(exprScope);
        else
            // for the variable that has no annotation
            isLegal = varScope == null || varScope.equals(currentAllocScope());

        if (!isLegal) {
            report(Result.failure("bad.assignment.scope", exprScope, varScope),
                    errorNode);
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
    private boolean checkForPrivateMemAssignementError(VariableElement var1,
            VariableElement var2) {
        if (var1.asType().toString()
                .equals("javax.safetycritical.PrivateMemory")
                && var2.asType().toString()
                .equals("javax.safetycritical.PrivateMemory")) {
            // System.out.println("check :" + var2);
            if (!var1.getAnnotationMirrors().toString()
                    .equals(var2.getAnnotationMirrors().toString()))
                return true;
        }
        return false;
    }

    // Doesn't work for interfaces (nor should it)
    private boolean isMemoryAreaType(TypeElement t) {
        if (t.getKind() == ElementKind.INTERFACE) {
            return false;
        }
        while (!TypesUtils.isDeclaredOfName(t.asType(),
        "javax.realtime.MemoryArea")
        && !TypesUtils.isObject(t.asType())) {
            t = (TypeElement) ((DeclaredType) t.getSuperclass()).asElement();
        }
        return TypesUtils.isDeclaredOfName(t.asType(),
        "javax.realtime.MemoryArea");
    }

    private String getScopeDef(ExpressionTree e) throws ScopeException {
        String scope = null;
        switch (e.getKind()) {
        case METHOD_INVOCATION:
            ExecutableElement method = TreeUtils
            .elementFromUse((MethodInvocationTree) e);
            String methodName = method.getSimpleName().toString();
            TypeElement type = (TypeElement) method.getEnclosingElement();
            if (TypesUtils.isDeclaredOfName(type.asType(),
            "javax.realtime.ImmortalMemory")
            && "instance".equals(methodName)) {
                return "immortal";
            } else if (TypesUtils.isDeclaredOfName(type.asType(),
            "javax.realtime.MemoryArea")
            && "getMemoryArea".equals(methodName)) {
                MethodInvocationTree methodTree = (MethodInvocationTree) e;
                ExpressionTree arg = methodTree.getArguments().get(0);
                switch (arg.getKind()) {
                case IDENTIFIER:
                case MEMBER_SELECT:
                    VariableElement var = (VariableElement) TreeUtils
                    .elementFromUse(arg);
                    scope = scope(var);
                }
            }
            // TODO: warning? the only method invocation that has a valid
            // scopedef right now would be
            // ImmortalMemory.instance(). We should support
            // MemoryArea.getMemoryArea() to the degree that it is
            // possible.
            break;
        case IDENTIFIER:
        case MEMBER_SELECT:
            VariableElement var = (VariableElement) TreeUtils.elementFromUse(e);
            scope = scopeDef(var);
        }
        if (scope != null) {
            return scope;
        }

        if (e.toString().trim()
                .startsWith("ManagedMemory.getCurrentManagedMemory()"))
            return currentAllocScope();

        report(Result.warning("unresolvable.scopedef"), e);
        return null;
    }

    private boolean isPrimitiveExpression(ExpressionTree expr) {
        // System.out.println("atf:" + atf);

        // System.out.println("\n\n expr:" + expr.toString());
        // System.out.println("expr kind:" +expr.getKind());

        if (expr.getKind() == Kind.NEW_ARRAY)
            return false;

        // System.out.println("atf:" + atf.fromExpression(expr));
        // System.out.println("kind:" + atf.fromExpression(expr).getKind());
        // System.out.println("kind:" +
        // atf.fromExpression(expr).getKind().isPrimitive());

        return atf.fromExpression(expr).getKind().isPrimitive();
    }

    public void report(Result r, Object src) {
        if (src != null) {
            checker.report(r, src);
        }
    }

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

    /**
     * 
     */
    public R visitAnnotation(AnnotationTree node, P p) {
        // we are escaping @SCJRestricted annotation
        // it its not the concern of this checker and since it
        // can have assignements, it brings errors to the regular code
        // assignenments taht we need to check
        if (node.getAnnotationType().toString().equals("SCJRestricted"))
            return null;
        if (node.getAnnotationType().toString().equals("Target"))
            return null;
        if (node.getAnnotationType().toString().equals("Retention"))
            return null;

        return super.visitAnnotation(node, p);
    }
}
