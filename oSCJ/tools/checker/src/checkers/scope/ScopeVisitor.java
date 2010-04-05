package checkers.scope;

import static checkers.Utils.isStatic;
import static javax.lang.model.util.ElementFilter.methodsIn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
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
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.Tree.Kind;

// TODO: Defaults for @RunsIn/@Scope on specific SCJ classes
// TODO: Unscoped method parameters?

/*
 * Rules for classes with no @Scope:
 * 
 * 1. Object creation is allowed anywhere.
 * 2. Methods may not have a return type of a non-annotated class.
 * 3. Fields may not escape the class that owns them. 
 * 4. Methods of non-annotated classes can be called if the current context is the same as the allocation context,
 *    since the instance it is called on cannot escape. This means the method should always be called in the
 *    correct context.
 */

@SuppressWarnings("restriction")
public class ScopeVisitor<R, P> extends SourceVisitor<R, P> {
    private static Map<String, String>  classScopes      = new HashMap<String, String>();
    private static Map<String, String>  runsIns          = new HashMap<String, String>();
    private AnnotatedTypeFactory        atf;
    private AnnotatedTypes              ats;
    private String                      currentScope     = null;
    private String                      currentRunsIn    = null;
    private static EnumSet<ElementKind> classOrInterface = EnumSet.of(ElementKind.CLASS, ElementKind.INTERFACE);

    public ScopeVisitor(SourceChecker checker, CompilationUnitTree root) {
        super(checker, root);

        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
    }

    @Override
    public R visitAssignment(AssignmentTree node, P p) {
        if (!isAssignable(node.getVariable(), node.getExpression())) {
            checker.report(Result.failure("bad.assignment.scope"), node);
        }
        return super.visitAssignment(node, p);
    }

    @Override
    public R visitBlock(BlockTree node, P p) {
        String oldRunsIn = currentRunsIn;
        if (node.isStatic()) {
            currentRunsIn = "immortal";
        }
        R r = super.visitBlock(node, p);
        currentRunsIn = oldRunsIn;
        return r;
    }

    @Override
    public R visitClass(ClassTree node, P p) {
        TypeElement nodeType = TreeUtils.elementFromDeclaration(node);
        String scope = scope(nodeType, node);
        String runsIn = runsIn(nodeType, node);

        if (scope != null && runsIn != null && !ScopeTree.isParentOf(runsIn, scope)) {
            // If a class has a @Scope and a @RunsIn, the @RunsIn scope must be a child of the @Scope scope.
            checker.report(Result.failure("scope.runs.in.disagreement"), node);
        }

        Utils.debugPrintln("Seen class " + nodeType.getQualifiedName() + ": @Scope(" + scope + ") @RunsIn(" + runsIn
                + ")");

        String oldScope = currentScope;
        String oldRunsIn = currentRunsIn;
        currentScope = scope;
        currentRunsIn = runsIn;
        R r = super.visitClass(node, p);
        currentScope = oldScope;
        currentRunsIn = oldRunsIn;
        return r;
    }

    @Override
    public R visitMemberSelect(MemberSelectTree node, P p) {
        Element e = TreeUtils.elementFromUse(node);
        switch (e.getKind()) {
        case FIELD:
            TypeElement type = (TypeElement) e.getEnclosingElement();
            String typeScope = scope(type, node);

            // The field is a reference type with no @Scope and its owner is not allocated in the same scope
            // as the current allocation context
            if (e.asType().getKind() == TypeKind.DECLARED && scope((VariableElement) e, node) == null
                    && typeScope.equals(currentAllocScope())) {
                // Can't reference a field that has an unannotated type unless it's in the same scope
                checker.report(Result.failure("escaping.nonannotated.field"), node);
            }
        }
        return super.visitMemberSelect(node, p);
    }

    @Override
    public R visitMethod(MethodTree node, P p) {
        ExecutableElement method = TreeUtils.elementFromDeclaration(node);
        Utils.debugPrintln("Seen method " + method.getSimpleName());
        if (Utils.scope(method.getAnnotationMirrors()) != null) {
            // Methods only have @RunsIn annotations, not @Scope (because @Scope indicates allocation location of a
            // type)
            checker.report(Result.failure("scope.on.method"), node);
        }
        TypeElement type = (TypeElement) method.getEnclosingElement();
        // run() requires a @RunsIn annotation on concrete classes that implement Runnable
        // TODO: check super class interfaces?
        if (type.getKind() == ElementKind.CLASS) {
            boolean isRunnable = false;
            while (type != null) {
                for (TypeMirror iface : type.getInterfaces()) {
                    TypeElement ifaceType = (TypeElement) ((DeclaredType) iface).asElement();
                    if (TypesUtils.isDeclaredOfName(ifaceType.asType(), "java.lang.Runnable")) {
                        isRunnable = true;
                        break;
                    }
                }
                type = superType(type);
            }
            if (isRunnable && "run".equals(method.getSimpleName().toString()) && method.getParameters().size() == 0
                    && Utils.runsIn(method.getAnnotationMirrors()) != null) {
                checker.report(Result.failure("bad.runs.in.override"), node);
            }
        }
        String runsIn = runsIn(method, node);
        Utils.debugPrintln("@RunsIn(" + runsIn + ") " + method.getSimpleName());
        R r = null;
        // TODO: If we don't have an allocation context, we need to skip the method?
        String oldRunsIn = currentRunsIn;
        if (runsIn != null) {
            currentRunsIn = runsIn;
        }
        if (currentAllocScope() != null) {
            r = super.visitMethod(node, p);
        }
        currentRunsIn = oldRunsIn;

        // TODO: Don't know if this is necessary
        // Tree retTree = node.getReturnType();
        // if (retTree != null) { // not a constructor
        // AnnotatedTypeMirror mirror = atf.getAnnotatedType(retTree);
        // TypeElement retType = (TypeElement) mirror.getElement(); // TODO: make this better
        // if (retType != null) { // not a primitive
        // String returnScope = scope(retType, node);
        // if (!ScopeTree.isParentOf(returnScope, methodRunsIn)) {
        // // Methods can't return types that aren't unannotated (should maybe allow this for private?)
        // checker.report(Result.failure("bad.return.type"), retTree);
        // }
        // }
        // }
        return r;
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        ExecutableElement method = TreeUtils.elementFromUse(node);
        TypeElement type = (TypeElement) method.getEnclosingElement();
        String runsIn = runsIn(method, node);
        // Ignore constructors, since they should be type checked by the class
        // visitation anyway.
        String methodName = method.getSimpleName().toString();
        if (isMemoryAreaType(type) && ("executeInArea".equals(methodName) || "enter".equals(methodName))) {
            ExpressionTree e = node.getMethodSelect();
            ExpressionTree arg = node.getArguments().get(0);
            String argRunsIn = null;
            // TODO: Single exit point sure would be nicer
            switch (arg.getKind()) {
            case IDENTIFIER:
                argRunsIn = directRunsIn((VariableElement) TreeUtils.elementFromUse((IdentifierTree) arg));
                break;
            case MEMBER_SELECT:
                Element tmp = TreeUtils.elementFromUse((MemberSelectTree) arg);
                if (tmp.getKind() != ElementKind.FIELD) {
                    checker.report(Result.failure("bad.enter.parameter"), arg);
                    return super.visitMethodInvocation(node, p);
                } else {
                    argRunsIn = directRunsIn((VariableElement) tmp);
                }
                break;
            case NEW_CLASS:
                ExecutableElement ctor = (ExecutableElement) TreeUtils.elementFromUse((NewClassTree) arg);
                argRunsIn = runsIn((TypeElement) ctor.getEnclosingElement(), null);
                break;
            default:
                checker.report(Result.failure("bad.enter.parameter"), arg);
                return super.visitMethodInvocation(node, p);
            }
            if (argRunsIn == null) {
                // All Runnables used with executeInArea/enter should have @RunsIn
                // TODO: Prevent Runnables from having their run override annotations?
                checker.report(Result.failure("runnable.without.runsin"), node);
            } else {
                String varScope = null;
                switch (e.getKind()) {
                case IDENTIFIER:
                    // TODO: This only happens for this/super constructor calls or implicit this.method calls. How do we
                    // handle this.method? Do we need to?
                    varScope = scopeDef((VariableElement) TreeUtils.elementFromUse((IdentifierTree) e));
                    break;
                case MEMBER_SELECT:
                    varScope = getScopeDef(((MemberSelectTree) e).getExpression());
                    break;
                }
                if (varScope == null || !varScope.equals(argRunsIn)) {
                    // The Runnable and the PrivateMemory must have agreeing scopes
                    checker.report(Result.failure("bad.executeInArea.or.enter"), node);
                }
                if ("executeInArea".equals(methodName) && !ScopeTree.isParentOf(currentAllocScope(), varScope)) {
                    checker.report(Result.failure("bad.executeInArea.target"), node);
                } else if ("enter".equals(methodName) && !ScopeTree.isParentOf(varScope, currentAllocScope())) {
                    checker.report(Result.failure("bad.enter.target"), node);
                }
            }
        } else if (isMemoryAreaType(type) && "newInstance".equals(methodName)) {
            ExpressionTree e = node.getMethodSelect();
            ExpressionTree arg = node.getArguments().get(0);
            String varScope = null;
            switch (e.getKind()) {
            case IDENTIFIER:
                varScope = scopeDef((VariableElement) TreeUtils.elementFromUse((IdentifierTree) e));
                break;
            case MEMBER_SELECT:
                varScope = getScopeDef(((MemberSelectTree) e).getExpression());
                break;
            }
            Element newInstanceType;
            if (arg.getKind() == Kind.MEMBER_SELECT
                    && ((MemberSelectTree) arg).getExpression().getKind() == Kind.IDENTIFIER
                    && (newInstanceType = TreeUtils.elementFromUse((IdentifierTree) ((MemberSelectTree) arg)
                        .getExpression())).getKind() == ElementKind.CLASS) {
                String instanceScope = scope((TypeElement) newInstanceType, ((MemberSelectTree) arg).getExpression());
                if (instanceScope != null && !varScope.equals(instanceScope)) {
                    checker.report(Result.failure("bad.newInstance",
                        ((TypeElement) newInstanceType).getQualifiedName(), varScope), node);
                }
            } else {
                // TODO: We only accept newInstance(X.class) right now
            }
        } else if (!method.getSimpleName().toString().startsWith("<init>") && runsIn != null
                && !runsIn.equals(currentAllocScope())) {
            if (!(Utils.isAllocFree(method, ats) && ScopeTree.isParentOf(currentAllocScope(), runsIn))) {
                // Can only call methods that run in the same scope. Allows parent scopes as well, if
                // they are marked @AllocFree.
                checker.report(Result.failure("bad.method.invoke", runsIn, currentAllocScope()), node);
            }
        }
        return super.visitMethodInvocation(node, p);
    }

    // Object allocation is only allowed if the current allocation context is
    // the same scope as what's defined by the class.
    @Override
    public R visitNewClass(NewClassTree node, P p) {
        ExecutableElement ctorElement = TreeUtils.elementFromUse(node);
        String nodeClassScope = scope((TypeElement) ctorElement.getEnclosingElement(), node);
        if (nodeClassScope != null && !currentAllocScope().equals(nodeClassScope)) {
            // Can't call new unless the type has the same scope as the current context
            checker.report(Result.failure("bad.allocation", currentAllocScope(), nodeClassScope), node);
        }
        return super.visitNewClass(node, p);
    }

    // - Static variables must always be in the immortal scope.
    // - Instance variables must make sure that the scope of the enclosing class
    // is a child scope of the variable type's scope.
    // - Local variables are similar to instance variables, only it must first
    // use the @RunsIn annotation, if any exists, before using the @Scope
    // annotation on the class that it belongs to.
    @Override
    public R visitVariable(VariableTree node, P p) {
        AnnotatedTypeMirror mirror = atf.getAnnotatedType(node.getType());
        // This assumes that the TypeElement for primitives is null, because primitives have no class bodies.
        Tree nodeTypeTree = node.getType();
        while (nodeTypeTree.getKind() == Kind.ARRAY_TYPE) {
            nodeTypeTree = ((ArrayTypeTree) nodeTypeTree).getType();
        }
        if (nodeTypeTree.getKind() == Kind.PRIMITIVE_TYPE) { return super.visitVariable(node, p); }
        TypeElement nodeType = (TypeElement) TreeUtils.elementFromUse((ExpressionTree) nodeTypeTree);
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        String varScope = scope(TreeUtils.elementFromDeclaration(node), node);
        Element varEnv = var.getEnclosingElement();
        String oldRunsIn = currentRunsIn;

        Utils.debugPrint("Variable " + var.getSimpleName() + " is ");

        if (isStatic(var.getModifiers())) {
            Utils.debugPrintln("static (immortal)");
            // Class variable
            if (varScope != null && !"immortal".equals(varScope)) {
                // If a variable is static, its type should be @Scope("immortal") or nothing at all
                checker.report(Result.failure("static.not.immortal"), node);
            }
            // We need to set this for visiting the variable initializer.
            // Apparently sometimes the static initializer is optimized into
            // individual initializers?
            currentRunsIn = "immortal";
        } else if (classOrInterface.contains(varEnv.getKind())) {
            // Instance variable
            Utils.debugPrintln("instance @Scope(" + varScope + ")");
            String varEnvScope = Utils.scope(varEnv.getAnnotationMirrors());
            if (!(var.asType().getKind().isPrimitive() || varEnvScope == null || varScope == null || ScopeTree
                .isParentOf(varEnvScope, varScope))) {
                // Instance fields must be in the same or parent scope as the instance
                checker.report(Result.failure("bad.field.scope"), node);
            }
            currentRunsIn = varScope;
        } else {
            Utils.debugPrintln("local @Scope(" + varScope + ")");
            // Local variable
            if (varScope != null && !ScopeTree.isParentOf(currentAllocScope(), varScope)) {
                // @Scopes of local variable types should agree with the current allocation context
                checker.report(Result.failure("bad.variable.scope", atf.fromTypeTree(node.getType()),
                    currentAllocScope()), node);
                // but if it doesn't, assume the variable scope is correct. TODO
                currentRunsIn = varScope;
            }
        }
        if (node.getInitializer() != null && !isAssignable(node, node.getInitializer())) {
            checker.report(Result.failure("bad.assignment.scope"), node);
        }
        R r = super.visitVariable(node, p);
        currentRunsIn = oldRunsIn;
        return r;
    }

    private interface AnnotationRetriever {
        String getAnnotationValue(Collection<? extends AnnotationMirror> mirrors);

        String getName();
    }

    private static AnnotationRetriever scopeRetriever  = new AnnotationRetriever() {
                                                           public String getAnnotationValue(
                                                                   Collection<? extends AnnotationMirror> mirrors) {
                                                               return Utils.scope(mirrors);
                                                           }

                                                           public String getName() {
                                                               return "Scope";
                                                           }
                                                       };

    private static AnnotationRetriever runsInRetriever = new AnnotationRetriever() {
                                                           public String getAnnotationValue(
                                                                   Collection<? extends AnnotationMirror> mirrors) {
                                                               return Utils.runsIn(mirrors);
                                                           }

                                                           public String getName() {
                                                               return "RunsIn";
                                                           }
                                                       };

    // Retrieves a type annotation on a class that adheres to the following rules:
    // - A class cannot modify the annotation value of any super classes
    // - A class or interface may not implement a set of interfaces that have differing annotation values
    private String typeAnnotation(TypeElement nodeType, Tree errorNode, Map<String, String> cache,
            AnnotationRetriever retriever) {
        String nodeName = nodeType.getQualifiedName().toString();
        if (cache.containsKey(nodeName)) { return cache.get(nodeName); }
        String nodeAnno = retriever.getAnnotationValue(nodeType.getAnnotationMirrors());
        if (nodeType.getKind() == ElementKind.CLASS) {
            TypeElement superType = superType(nodeType);
            while (superType != null && !TypesUtils.isObject(superType.asType())) {
                String superAnno = retriever.getAnnotationValue(superType.getAnnotationMirrors());
                if (superAnno != null) {
                    if (nodeAnno != null && !nodeAnno.equals(superAnno)) {
                        // Child classes must have the same annotation as their parent, or none at all
                        checker.report(Result.failure("bad.annotation", retriever.getName()), errorNode);
                    }
                    nodeAnno = superAnno;
                    break;
                }
                superType = superType(superType);
            }
        }
        // All interfaces should have the same annotation, if any
        // TODO: Maybe unnecessary to get parent interfaces, since they'll be visited individually later
        HashSet<TypeElement> ifaces = Utils.getAllInterfaces(nodeType);
        HashMap<String, ArrayList<TypeElement>> annos = new HashMap<String, ArrayList<TypeElement>>(ifaces.size());
        for (TypeElement iface : ifaces) {
            String scope = retriever.getAnnotationValue(iface.getAnnotationMirrors());
            if (scope != null) {
                ArrayList<TypeElement> ifaceList = annos.get(scope);
                if (ifaceList == null) {
                    annos.put(scope, ifaceList = new ArrayList<TypeElement>());
                }
                ifaceList.add(iface);
            }
        }
        if (annos.size() > 1
                || (annos.size() == 1 && nodeAnno != null && !annos.keySet().iterator().next().equals(nodeAnno))) {
            checker.report(Result.failure("interface.annotation.mismatch", retriever.getName()), errorNode);
        }
        checkScopeExistence(nodeAnno, errorNode);
        cache.put(nodeName, nodeAnno);
        return nodeAnno;
    }

    private String scope(TypeElement nodeType, Tree errorNode) {
        return typeAnnotation(nodeType, errorNode, classScopes, scopeRetriever);
    }

    private String runsIn(TypeElement nodeType, Tree errorNode) {
        return typeAnnotation(nodeType, errorNode, runsIns, runsInRetriever);
    }

    private String runsIn(ExecutableElement method, Tree errorNode) {
        // TODO: key doesn't handle overloads, I think
        String methodName = method.getSimpleName().toString();
        TypeElement methodEnv = (TypeElement) method.getEnclosingElement();
        String methodEnvScope = scope(methodEnv, errorNode);
        String methodKey = methodEnv.getQualifiedName() + "#" + methodName;
        for (VariableElement var : method.getParameters()) {
            methodKey += var.asType().toString();
        }
        if (runsIns.containsKey(methodKey)) { return runsIns.get(methodKey); }
        String runsIn = Utils.runsIn(method.getAnnotationMirrors());
        if (methodName.startsWith("<init>")) {
            if (runsIn != null) {
                // @RunsIn should not be on constructors
                checker.report(Result.failure("runs.in.on.ctor"), errorNode);
            }
            return methodEnvScope;
        } else if (methodName.startsWith("<clinit>")) {
            if (runsIn != null) {
                // @RunsIn should not be on static initializers (if it's even possible)
                checker.report(Result.failure("runs.in.on.ctor"), errorNode);
            }
            return "immortal";
        } else {
            checkScopeExistence(runsIn, errorNode);
            if (runsIn != null) {
                if (!ScopeTree.isParentOf(runsIn, methodEnvScope)) {
                    // A method must run in a child scope (or same scope) as the allocation context of its type
                    checker.report(Result.failure("bad.runs.in.method"), errorNode);
                }
                runsIns.put(methodKey, runsIn);

                Collection<ExecutableElement> overrides = orderedOverriddenMethods(method);
                for (ExecutableElement override : overrides) {
                    String overriddenRunsIn = Utils.runsIn(override.getAnnotationMirrors());
                    if (overriddenRunsIn != null && !runsIn.equals(overriddenRunsIn)) {
                        // A method must have the same @RunsIn as its overrides, or none at all
                        checker.report(Result.failure("bad.runs.in.override"), errorNode);
                    }
                }
                return runsIn;
            } else {
                for (ExecutableElement override : orderedOverriddenMethods(method)) {
                    String overriddenRunsIn = Utils.runsIn(override.getAnnotationMirrors());
                    if (overriddenRunsIn != null) {
                        runsIns.put(methodKey, overriddenRunsIn);
                        return overriddenRunsIn;
                    }
                }
                String methodScope = runsIn(methodEnv, errorNode);
                if (methodScope == null) {
                    methodScope = scope(methodEnv, errorNode);
                }
                runsIns.put(methodKey, methodScope);
                return methodScope;
            }
        }
    }

    private String currentAllocScope() {
        return currentRunsIn != null ? currentRunsIn : currentScope;
    }

    private TypeElement superType(TypeElement type) {
        if (TypesUtils.isObject(type.asType())) { return null; }
        return (TypeElement) ((DeclaredType) type.getSuperclass()).asElement();
    }

    private void checkScopeExistence(String scope, Tree node) {
        if (scope != null && ScopeTree.get(scope) == null) {
            checker.report(Result.failure("nonexistent.scope", scope), node);
        }
    }

    private void addInterfaceOverrides(TypeElement t, ExecutableElement m, Collection<ExecutableElement> overrides,
            HashSet<TypeElement> seenIfaces) {
        for (TypeMirror iface : t.getInterfaces()) {
            TypeElement ifaceElem = (TypeElement) ((DeclaredType) iface).asElement();
            if (seenIfaces.add(ifaceElem)) {
                for (ExecutableElement ifaceMethod : methodsIn(ifaceElem.getEnclosedElements())) {
                    if (elements.overrides(m, ifaceMethod, ifaceElem)) {
                        overrides.add(ifaceMethod);
                        break;
                    }
                }
                overrides.addAll(orderedOverriddenMethodsInterface(m, ifaceElem));
            }
        }
    }

    // Like ats.overriddenMethods(), except not a map and is guaranteed to iterate in hierarchical order.
    private Collection<ExecutableElement> orderedOverriddenMethods(ExecutableElement method) {
        TypeElement enclosing = (TypeElement) method.getEnclosingElement();

        if (enclosing.getKind() == ElementKind.INTERFACE) { return orderedOverriddenMethodsInterface(method, enclosing); }

        LinkedList<ExecutableElement> overrides = new LinkedList<ExecutableElement>();
        if (!TypesUtils.isObject(enclosing.asType())) {
            TypeElement superType = superType(enclosing);
            HashSet<TypeElement> seenIfaces = new HashSet<TypeElement>();
            addInterfaceOverrides(enclosing, method, overrides, seenIfaces);
            while (!TypesUtils.isObject(superType.asType())) {
                for (ExecutableElement superMethod : methodsIn(superType.getEnclosedElements())) {
                    if (elements.overrides(method, superMethod, superType)) {
                        overrides.add(superMethod);
                        break;
                    }
                }
                addInterfaceOverrides(superType, method, overrides, seenIfaces);
                superType = superType(superType);
            }
        }
        return overrides;
    }

    // Breadth first search of interfaces to find overridden methods.
    private Collection<ExecutableElement> orderedOverriddenMethodsInterface(ExecutableElement method, TypeElement iface) {
        LinkedList<ExecutableElement> overrides = new LinkedList<ExecutableElement>();
        LinkedList<TypeElement> work = new LinkedList<TypeElement>();
        work.add(iface);
        while (!work.isEmpty()) {
            TypeElement workIface = work.removeFirst();
            List<? extends TypeMirror> workSuperIfaces = workIface.getInterfaces();
            for (TypeMirror superIface : workSuperIfaces) {
                if (superIface.getKind() == TypeKind.DECLARED) {
                    work.addLast((TypeElement) ((DeclaredType) superIface).asElement());
                }
            }
            // Assuming a method overrides itself, we want to skip iface.
            if (workIface != iface) {
                for (ExecutableElement workIfaceMethod : methodsIn(workIface.getEnclosedElements())) {
                    if (elements.overrides(method, workIfaceMethod, workIface)) {
                        overrides.addLast(workIfaceMethod);
                    }
                }
            }
        }
        return overrides;
    }

    private String scope(VariableElement var, Tree errorNode) {
        if (isStatic(var.getModifiers())) { // static
            return "immortal";
        }

        String directScope = directScope(var, errorNode);
        if (directScope != null) { return directScope; }

        // Variable's type is not annotated, so go by the enclosing environment.
        // TODO: use currentAllocScope()?
        if (classOrInterface.contains(var.getEnclosingElement().getKind())) { // instance
            return scope((TypeElement) var.getEnclosingElement(), errorNode);
        } else { // local
            return runsIn((ExecutableElement) var.getEnclosingElement(), errorNode);
        }
    }

    private String directScope(VariableElement var, Tree errorNode) {
        AnnotatedTypeMirror exprType = atf.getAnnotatedType(var);
        while (exprType.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType arrayType = (AnnotatedArrayType) exprType;
            exprType = arrayType.getComponentType();
        }
        if (exprType.getKind() == TypeKind.DECLARED) {
            // return scope(elements.getTypeElement(exprType.toString()), null);
            return scope((TypeElement) ((AnnotatedDeclaredType) exprType).getUnderlyingType().asElement(), errorNode);
        }
        return null;
    }

    private String directRunsIn(VariableElement var) {
        AnnotatedTypeMirror exprType = atf.getAnnotatedType(var);
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

    private String scopeDef(VariableElement var) {
        for (AnnotationMirror mirror : var.getAnnotationMirrors()) {
            if (TypesUtils.isDeclaredOfName(mirror.getAnnotationType(), "javax.safetycritical.annotate.ScopeDef")) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> vals = mirror.getElementValues();
                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : vals.entrySet()) {
                    if ("name".equals(e.getKey().getSimpleName().toString())) { return e.getValue().getValue()
                        .toString(); }
                }
            }
        }
        return null;
    }

    private boolean isAssignable(ExpressionTree varTree, ExpressionTree exprTree) {
        Element rawVarElem;
        // TODO: This assumption means that arrays are allocated in the same scope as the objects
        // that they contain
        while (varTree.getKind() == Kind.ARRAY_ACCESS) {
            varTree = ((ArrayAccessTree) varTree).getExpression();
        }
        rawVarElem = TreeUtils.elementFromUse(varTree);
        switch (rawVarElem.getKind()) {
        case METHOD:
            return true; // Annotation assignments that we don't care about
        case FIELD:
        case LOCAL_VARIABLE:
        case PARAMETER:
            return isAssignable((VariableElement) rawVarElem, exprTree, varTree);
        }
        throw new RuntimeException("Missing a case statement for " + rawVarElem.getKind());
    }

    private boolean isAssignable(VariableTree varTree, ExpressionTree exprTree) {
        return isAssignable(TreeUtils.elementFromDeclaration(varTree), exprTree, varTree);
    }

    private boolean isAssignable(VariableElement varElem, ExpressionTree exprTree, Tree errorNode) {
        // Local variables should be handled by visitVariable
        if (classOrInterface.contains(varElem.getKind())) {
            String varName = varElem.getSimpleName().toString();
            String varScope = scope(varElem, errorNode);
            Element expr;
            switch (exprTree.getKind()) {
            case ARRAY_ACCESS:
                while (exprTree.getKind() == Kind.ARRAY_ACCESS) {
                    exprTree = ((ArrayAccessTree) exprTree).getExpression();
                }
                return isAssignable(varElem, exprTree, errorNode);
            case METHOD_INVOCATION:
                expr = TreeUtils.elementFromUse((MethodInvocationTree) exprTree);
                break;
            case NEW_CLASS:
                expr = TreeUtils.elementFromUse((NewClassTree) exprTree);
                break;
            case NULL_LITERAL:
                return true; // No fancy checking needed for null literals
            case STRING_LITERAL:
                return true;
            case TYPE_CAST:
                // TODO: revisit this, because it looks pretty wrong
                while (exprTree.getKind() == Kind.TYPE_CAST) {
                    exprTree = ((TypeCastTree) exprTree).getExpression();
                }
                return isAssignable(varElem, exprTree, errorNode);
            default:
                expr = TreeUtils.elementFromUse(exprTree);
            }
            String exprScope = null;
            switch (expr.getKind()) {
            case CONSTRUCTOR: {
                ExecutableElement methodElem = (ExecutableElement) expr;
                exprScope = scope((TypeElement) methodElem.getEnclosingElement(), null);
                // If the allocated type isn't annotated, then it's always allowed. The scope is bound by
                // the variable that it's assigned to.
                if (exprScope == null) {
                    exprScope = varScope;
                }
                break;
            }
            case METHOD: {
                ExecutableElement methodElem = (ExecutableElement) expr;
                AnnotatedExecutableType methodType = atf.getAnnotatedType(methodElem);
                TypeMirror retMirror = methodType.getReturnType().getUnderlyingType();
                if (retMirror.getKind() == TypeKind.DECLARED) {
                    TypeElement retType = (TypeElement) ((DeclaredType) retMirror).asElement();
                    exprScope = scope(retType, null);
                } else {
                    exprScope = varScope; // primitives are always assignable
                }
                break;
            }
            case FIELD:
            case LOCAL_VARIABLE:
            case PARAMETER:
                exprScope = scope((VariableElement) expr, null);
                break;
            default:
                throw new RuntimeException("Need a new case statement for " + expr.getKind());
            }
            Utils.debugPrintln("Assignment: @Scope(" + varScope + ") " + varName + " = @Scope(" + exprScope + ")");
            return varScope == null || varScope.equals(exprScope);
        } else {
            return true; // primitives are always assignable
        }
    }

    // Doesn't work for interfaces (nor should it)
    private boolean isMemoryAreaType(TypeElement t) {
        if (t.getKind() == ElementKind.INTERFACE) { return false; }
        while (!TypesUtils.isDeclaredOfName(t.asType(), "javax.realtime.MemoryArea")
                && !TypesUtils.isObject(t.asType())) {
            t = (TypeElement) ((DeclaredType) t.getSuperclass()).asElement();
        }
        return TypesUtils.isDeclaredOfName(t.asType(), "javax.realtime.MemoryArea");
    }

    private String getScopeDef(ExpressionTree e) {
        String scope = null;
        switch (e.getKind()) {
        case METHOD_INVOCATION:
            ExecutableElement method = TreeUtils.elementFromUse((MethodInvocationTree) e);
            String methodName = method.getSimpleName().toString();
            TypeElement type = (TypeElement) method.getEnclosingElement();
            if (TypesUtils.isDeclaredOfName(type.asType(), "javax.realtime.ImmortalMemory")
                    && "instance".equals(methodName)) {
                return "immortal";
            } else if (TypesUtils.isDeclaredOfName(type.asType(), "javax.realtime.MemoryArea")
                    && "getMemoryArea".equals(methodName)) {
                MethodInvocationTree methodTree = (MethodInvocationTree) e;
                ExpressionTree arg = methodTree.getArguments().get(0);
                switch (arg.getKind()) {
                case IDENTIFIER:
                case MEMBER_SELECT:
                    VariableElement var = (VariableElement) TreeUtils.elementFromUse(arg);
                    scope = scope(var, e);
                }
            }
            // TODO: warning? the only method invocation that has a valid scopedef right now would be
            // ImmortalMemory.instance(). We should support MemoryArea.getMemoryArea() to the degee that it is possible.
            break;
        case IDENTIFIER:
        case MEMBER_SELECT:
            VariableElement var = (VariableElement) TreeUtils.elementFromUse(e);
            scope = scopeDef(var);
        }
        if (scope != null) { return scope; }
        checker.report(Result.warning("unresolvable.scopedef"), e);
        return null;
    }
}
