package checkers.scope;

import static checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_TYPE_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE;
import static checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_TYPE_DEFINE_SCOPE_NOT_CONSISTENT;
import static checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_TYPE_NO_DEFINE_SCOPE;
import static checkers.scope.ScopeRunsInChecker.ERR_BAD_LIBRARY_ANNOTATION;
import static checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_FIELD_SCOPE;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE;
import static checkers.scope.ScopeRunsInChecker.ERR_RUNS_IN_ON_CLASS;
import static javax.safetycritical.annotate.Level.SUPPORT;

import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

import checkers.SCJVisitor;
import checkers.Utils;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypes;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * This visitor is responsible for retrieving Scope and RunsIn annotations from
 * classes and methods and making sure they are valid. This information is
 * stored into a context object so that the ScopeVisitor doesn't have to deal
 * with retrieving this information.
 */
public class ScopeRunsInVisitor extends SCJVisitor<Void, Void> {
    private ScopeCheckerContext ctx;
    private ScopeTree scopeTree;
    private AnnotatedTypeFactory atf;
    private AnnotatedTypes ats;

    public ScopeRunsInVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext ctx) {
        super(checker, root);
        this.ctx = ctx;
        scopeTree = ctx.getScopeTree();
        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
    }

    @Override
    public Void visitClass(ClassTree node, Void p) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);
        checkClassScope(t, node, node);
        return super.visitClass(node, p);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        // Visiting the types and super types of the provided sources is not
        // enough. Therefore, if we see any type names in the source, we
        // should check them out.
        Element elem = TreeUtils.elementFromUse(node);
        if (elem.getKind() == ElementKind.CLASS
                || elem.getKind() == ElementKind.INTERFACE) {
            TypeElement t = (TypeElement) elem;
            checkClassScope(t, trees.getTree(t), node);
        }
        return super.visitIdentifier(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        // Visiting the types and super types of the provided sources is not
        // enough. In addition to visitIdentifier, we need to check the
        // return types of method invocations, because it's possible to use
        // a type without ever having its type name mentioned through method
        // chaining.
        TypeMirror mirror = InternalUtils.typeOf(node);
        if (mirror.getKind() == TypeKind.DECLARED) {
            TypeElement t = Utils.getTypeElement(mirror);
            checkClassScope(t, trees.getTree(t), node);
        }
        return super.visitMethodInvocation(node, p);
    }

    /**
     * Check that a class has a valid Scope annotation.
     * <ul>
     * <li>The scope name must exist in the scope tree or be CURRENT
     * <li>The scope name must match the effective scope name of its parent
     * class, unless the effective scope name is CURRENT
     * </ul>
     * <p>
     * The effective scope name of a class C which is annotated Scope(S) is:
     * <ul>
     * <li>S, if S is not CURRENT
     * <li>The effective scope name of D, if C extends D and S is CURRENT
     * </ul>
     * <p>
     * If C has no explicit Scope annotation, it is assumed to be annotated as
     * Scope(CURRENT).
     */
    void checkClassScope(TypeElement t, ClassTree node, Tree errNode) {
        debugIndentIncrement("checkClassScope: " + t);
        if (ctx.getClassScope(t) != null) {
            // Already visited or is in the process of being visited
            debugIndentDecrement();
            return;
        }
        ScopeInfo scope = scopeOfClassDefinition(t);
        if (!scopeTree.hasScope(scope) && !scope.isCurrent())
            fail(ERR_BAD_SCOPE_NAME, node, errNode);

        TypeElement p = Utils.superType(t);
        if (p == null)
            ctx.setClassScope(scope, t); // t == java.lang.Object
        else {
            ScopeInfo parent = getParentScopeAndVisit(p, errNode);
            if (parent.isCurrent())
                ctx.setClassScope(scope, t);
            else if (scope.equals(parent))
                ctx.setClassScope(parent, t);
            else {
                // Set the scope to something, in case processing continues past
                // this class
                ctx.setClassScope(scope, t);
                fail(ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE, node, errNode);
            }

        }
        // Ensure that the class doesn't change any Scope annotations on its
        // implemented interfaces. This shouldn't require the retrieval of
        // all interfaces implemented by superclasses and interfaces, since
        // they should be visited as well prior to this point.
        for (TypeMirror i : t.getInterfaces()) {
            TypeElement ie = Utils.getTypeElement(i);
            ScopeInfo is = getParentScopeAndVisit(ie, errNode);
            if (!is.isCurrent() && !is.equals(scope))
                fail(ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE, node, errNode);
        }

        for (ExecutableElement c : Utils.constructorsIn(t)) {
            MethodTree mTree = trees.getTree(c);
            Tree mErr = mTree != null ? mTree : errNode;
            checkMethod(c, mTree, mErr);
        }

        for (ExecutableElement m : Utils.methodsIn(t)) {
            MethodTree mTree = trees.getTree(m);
            Tree mErr = mTree != null ? mTree : errNode;
            checkMethod(m, mTree, mErr);
        }

        for (VariableElement f : Utils.fieldsIn(t)) {
            Tree fTree = trees.getTree(f);
            Tree fErr = fTree != null ? fTree : errNode;
            ScopeInfo fScope = checkField(f, fTree, fErr);
            if (fScope != null)
                ctx.setFieldScope(fScope, f);
        }
        // We don't allow RunsIn annotations on classes anymore.
        if (t.getAnnotation(RunsIn.class) != null)
            fail(ERR_RUNS_IN_ON_CLASS, node, errNode);

        debugIndentDecrement();
    }

    void checkMethod(ExecutableElement m, MethodTree mTree, Tree errNode) {
        debugIndentIncrement("checkMethod: " + m);

        if (ctx.getMethodRunsIn(m) != null) {
            // Already visited or in the process of being visited
            debugIndentDecrement();
            return;
        }
        checkMethodScope(m, mTree, errNode);
        checkMethodRunsIn(m, mTree, errNode);
        List<? extends VariableElement> params = m.getParameters();
        List<? extends VariableTree> paramTrees = mTree != null ? mTree
                .getParameters() : null;
        for (int i = 0; i < params.size(); i++) {
            // TODO: Check overridden annotations
            VariableElement param = params.get(i);
            VariableTree paramTree = paramTrees != null ? paramTrees.get(i)
                    : null;
            ScopeInfo scope = checkVariableScopeOverride(param, paramTree,
                    errNode);
            if (scope != null)
                ctx.setParameterScope(scope, i, m);
        }
        debugIndentDecrement();
    }

    ScopeInfo checkField(VariableElement f, Tree node, Tree errNode) {
        ScopeInfo scope = checkVariableScopeOverride(f, node, errNode);
        ScopeInfo clazzScope = getEnclosingClassScope(f);

        if (!isValidFieldScope(scope, clazzScope))
            fail(ERR_ILLEGAL_FIELD_SCOPE, node, errNode, scope, clazzScope);

        checkMemoryAreaField(f, clazzScope, node, errNode);

        return scope;
    }

    private void checkMemoryAreaField(VariableElement f, ScopeInfo clazzScope,
            Tree node, Tree errNode) {
        if (!Utils.isUserLevel(f.getEnclosingElement().toString()))
            return;
        if (f.asType().getKind() != TypeKind.DECLARED)
            return;
        if (!needsDefineScope(Utils.getTypeElement(f.asType())))
            return;

        DefineScope ds = f.getAnnotation(DefineScope.class);
        if (ds == null) {
            fail(ERR_MEMORY_AREA_TYPE_NO_DEFINE_SCOPE, node);
            return;
        }

        ScopeInfo scope = new ScopeInfo(ds.name());
        ScopeInfo parent = new ScopeInfo(ds.parent());
        DefineScopeInfo dsi = new DefineScopeInfo(scope, parent);
        if (!scopeTree.hasScope(scope) || !scopeTree.hasScope(parent)
                || !scopeTree.isParentOf(scope, parent))
            fail(ERR_MEMORY_AREA_TYPE_DEFINE_SCOPE_NOT_CONSISTENT, node);

        // we need to check that the @Scope and @DefineScope are consistent.
        Scope s = f.getAnnotation(Scope.class);
        if (s != null && !s.value().equals(parent.getScope()))
            fail(ERR_MEMORY_AREA_TYPE_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE,
                    node, s.value(), parent);
        else if (s == null
                && (clazzScope.isCurrent() || !clazzScope.equals(parent)))
            fail(ERR_MEMORY_AREA_TYPE_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE,
                    node, clazzScope, parent);

        ctx.setFieldDefineScope(dsi, f.getEnclosingElement().toString(),
                f.toString());
    }

    /**
     * Check that a variable has a valid Scope annotation, if any. Four kinds of
     * variables are considered:
     * <ol>
     * <li>Primitive variables have no scope
     * <li>Primitive array variables are CURRENT if not annotated, and S if
     * annotated Scope(S)
     * <li>Object variables are CURRENT if not annotated, Scope(S) if the type
     * of the variable is annotated Scope(S), or S if the type of the variable
     * is not annotated and the field is annotated Scope(S).
     * <li>Object arrays follow the same rules as object variables based on the
     * type of their basic element type.
     */
    ScopeInfo checkVariableScopeOverride(VariableElement v, Tree node,
            Tree errNode) {
        debugIndentIncrement("checkVariableScopeOverride: " + v);

        TypeMirror vMirror = v.asType();
        Scope s = v.getAnnotation(Scope.class);
        ScopeInfo scope = ScopeInfo.CURRENT;
        ScopeInfo ret;
        if (s != null)
            scope = new ScopeInfo(s.value());

        if (!scopeTree.hasScope(scope) && !scope.isCurrent()
                && !scope.isUnknown())
            report(Result.failure(ERR_BAD_SCOPE_NAME, scope), node, errNode);

        // Arrays reside in the same scope as their element types, so if this
        // field is an array, reduce it to its base component type.
        vMirror = Utils.getBaseType(vMirror);
        if (vMirror.getKind() != TypeKind.DECLARED) {
            // The field type in here is either a primitive or a primitive
            // array. Only store a field scope if the field was an array.
            if (vMirror != v.asType())
                ret = scope;
            else
                ret = ScopeInfo.PRIMITIVE; // Primitives have no scope
        } else {
            TypeElement t = Utils.getTypeElement(vMirror);
            ScopeInfo tScope = ctx.getClassScope(t);
            if (tScope == null)
                checkClassScope(t, trees.getTree(t), errNode);

            tScope = ctx.getClassScope(t);
            if (s == null)
                if (tScope.isCurrent() && isUnknownMethodParameter(v))
                    scope = ScopeInfo.UNKNOWN;
                else
                    scope = tScope;
            if (tScope.isCurrent())
                ret = scope;
            else {
                ret = tScope;
                if (scope.isUnknown() || !scope.equals(tScope))
                    fail(ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE, node, errNode,
                            scope, tScope);
            }
        }
        debugIndentDecrement();
        return ret;
    }

    private boolean isUnknownMethodParameter(VariableElement v) {
        if (v.getKind() != ElementKind.PARAMETER)
            return false;

        ExecutableElement m = (ExecutableElement) v.getEnclosingElement();
        return ctx.getMethodRunsIn(m).isUnknown();
    }

    /**
     * Check that a method has a valid RunsIn annotation. A method's RunsIn
     * annotation is valid as long as it exists in the scope tree, or is UNKNOWN
     * or CURRENT. It is also illegal to change the RunsIn of an overridden
     * method, unless it is annotated SUPPORT.
     */
    void checkMethodRunsIn(ExecutableElement m, MethodTree node, Tree errNode) {
        RunsIn ann = m.getAnnotation(RunsIn.class);
        ScopeInfo runsIn = ann != null ? new ScopeInfo(ann.value())
                : ScopeInfo.CURRENT;

        if (!scopeTree.hasScope(runsIn) && !runsIn.isCurrent()
                && !runsIn.isUnknown())
            report(Result.failure(ERR_BAD_SCOPE_NAME, runsIn), node, errNode);

        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
                .overriddenMethods(m);
        for (ExecutableElement e : overrides.values()) {
            ScopeInfo eRunsIn = getOverrideRunsInAndVisit(e, errNode);
            SCJAllowed eLevelAnn = e.getAnnotation(SCJAllowed.class);
            Level eLevel = eLevelAnn != null ? eLevelAnn.value() : null;
            if (!eRunsIn.equals(runsIn) && eLevel != SUPPORT)
                report(Result.failure(ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE),
                        node, errNode);
        }
        ctx.setMethodRunsIn(runsIn, m);
    }

    /**
     * Check that a method has a valid Scope annotation. A method's Scope
     * annotation is valid as long as it exists in the scope tree, or is UNKNOWN
     * or CURRENT. It is also illegal to change the RunsIn of an overridden
     * method, unless it is annotated SUPPORT.
     */
    void checkMethodScope(ExecutableElement m, MethodTree node, Tree errNode) {
        Scope ann = m.getAnnotation(Scope.class);
        ScopeInfo scope = ann != null ? new ScopeInfo(ann.value())
                : ScopeInfo.CURRENT;

        if (!scopeTree.hasScope(scope) && !scope.isCurrent()
                && !scope.isUnknown())
            report(Result.failure(ERR_BAD_SCOPE_NAME, scope), node, errNode);

        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
                .overriddenMethods(m);
        for (ExecutableElement e : overrides.values()) {
            ScopeInfo eScope = getOverrideScopeAndVisit(e, errNode);
            SCJAllowed eLevelAnn = e.getAnnotation(SCJAllowed.class);
            Level eLevel = eLevelAnn != null ? eLevelAnn.value() : null;
            if (!eScope.equals(scope) && eLevel != SUPPORT)
                fail(ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE, node, errNode);
        }
        // TODO: Need to check that scopes agree on the method and the return
        // type
        ctx.setMethodScope(scope, m);
    }

    /**
     * Report the proper error from the context of the current check method.
     * Each check method may be invoked on two categories of items:
     * <ol>
     * <li>A code element that is in the compiled set of files (aka has source).
     * We analyze this in full and report any errors we find.
     * <li>A code element that is in a library. Since there is no source for
     * these, it is impossible to report errors on the AST of these classes.
     * Therefore, we report that a library is badly annotated, rather than the
     * actual error.
     * </ol>
     */
    void report(Result r, Tree node, Tree errNode) {
        if (node != null)
            // Current item being visited. Report the error as usual.
            checker.report(r, errNode);
        else
            // Current item is something from a library. Can't put an error on
            // it, so put an error on the node being visited stating that
            // something from the parent class or interface is broken.
            fail(ERR_BAD_LIBRARY_ANNOTATION, errNode);
    }

    void fail(String msg, Tree src, Tree err, Object... msgParams) {
        report(Result.failure(msg, msgParams), src, err);
    }

    void warn(String msg, Tree src, Tree err, Object... msgParams) {
        report(Result.warning(msg, msgParams), src, err);
    }

    private static ScopeInfo scopeOfClassDefinition(TypeElement t) {
        Scope scopeAnn = t.getAnnotation(Scope.class);
        return scopeAnn != null ? new ScopeInfo(scopeAnn.value())
                : ScopeInfo.CURRENT;
    }

    private ScopeInfo getParentScopeAndVisit(TypeElement p, Tree errNode) {
        ScopeInfo parent = ctx.getClassScope(p);
        if (parent == null) {
            checkClassScope(p, trees.getTree(p), errNode);
            parent = ctx.getClassScope(p);
        }
        return parent;
    }

    private ScopeInfo getOverrideScopeAndVisit(ExecutableElement m, Tree errNode) {
        ScopeInfo scope = ctx.getMethodScope(m);
        if (scope != null)
            return scope;
        checkMethod(m, trees.getTree(m), errNode);
        return ctx.getMethodScope(m);
    }

    private ScopeInfo getOverrideRunsInAndVisit(ExecutableElement m,
            Tree errNode) {
        ScopeInfo runsIn = ctx.getMethodRunsIn(m);
        if (runsIn != null)
            return runsIn;
        checkMethod(m, trees.getTree(m), errNode);
        return ctx.getMethodRunsIn(m);
    }

    /**
     *
     * @param f
     *            - field under consideration
     * @return - returns the Scope of the class enclosing the field
     */
    private ScopeInfo getEnclosingClassScope(VariableElement f) {
        TypeElement t = Utils.getTypeElement(f.getEnclosingElement().asType());
        return ctx.getClassScope(t);
    }

    /**
     * Check that a field has a valid Scope annotation.
     * <p>
     * Fields must live in the same scope or parent scope of the objects which
     * refer to them. UNKNOWN annotations are accepted, since assignments to
     * UNKNOWN fields are checked by a dynamic guard.
     */
    boolean isValidFieldScope(ScopeInfo fieldScope, ScopeInfo clazzScope) {
        return fieldScope == null || fieldScope.isCurrent()
                || fieldScope.isUnknown() || fieldScope.isPrimitive()
                || scopeTree.isParentOf(clazzScope, fieldScope);
    }
}
