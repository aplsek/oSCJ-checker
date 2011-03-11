package checkers.scope;

import static checkers.scope.ScopeRunsInChecker.ERR_BAD_LIBRARY_ANNOTATION;
import static checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_FIELD_SCOPE;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_STATIC_FIELD_SCOPE;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE;
import static checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT;
import static checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE;
import static checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_NO_DEFINE_SCOPE;
import static checkers.scope.ScopeRunsInChecker.ERR_RUNS_IN_ON_CLASS;
import static checkers.scope.ScopeRunsInChecker.ERR_RUNS_IN_ON_CONSTRUCTOR;
import static checkers.scope.ScopeRunsInChecker.ERR_SCOPE_ON_VOID_OR_PRIMITIVE_RETURN;
import static javax.safetycritical.annotate.Level.SUPPORT;

import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
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
import com.sun.source.tree.PrimitiveTypeTree;
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

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree node, Void p) {
        TypeMirror m = InternalUtils.typeOf(node);
        TypeElement boxed = types.boxedClass((PrimitiveType) m);
        checkClassScope(boxed, null, node);
        return super.visitPrimitiveType(node, p);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        VariableElement v = TreeUtils.elementFromDeclaration(node);
        if (v.getKind() == ElementKind.LOCAL_VARIABLE) {
            Scope s = v.getAnnotation(Scope.class);
            if (s != null) {
                ScopeInfo si = new ScopeInfo(s.value());
                if (!si.isValidVariableScope(v, scopeTree))
                    fail(ERR_BAD_SCOPE_NAME, node, si);
            }
        }
        return super.visitVariable(node, p);
    }

    void pln(String str) {System.out.println("\t" + str);}

    /**
     * Check that a class has a valid Scope annotation.
     * <ul>
     * <li>The scope name must exist in the scope tree or be CALLER
     * <li>The scope name must match the effective scope name of its parent
     * class, unless the effective scope name is CALLER
     * </ul>
     * <p>
     * The effective scope name of a class C which is annotated Scope(S) is:
     * <ul>
     * <li>S, if S is not CALLER
     * <li>The effective scope name of D, if C extends D and S is CALLER
     * </ul>
     * <p>
     * If C has no explicit Scope annotation, it is assumed to be annotated as
     * Scope(CALLER).
     */
    void checkClassScope(TypeElement t, ClassTree node, Tree errNode) {
        debugIndentIncrement("checkClassScope: " + t);
        if (ctx.getClassScope(t) != null) {
            // Already visited or is in the process of being visited
            debugIndentDecrement();
            return;
        }
        ScopeInfo scope = scopeOfClassDefinition(t);
        if (!scopeTree.hasScope(scope) && !scope.isCaller())
            fail(ERR_BAD_SCOPE_NAME, node, errNode, scope);

        TypeElement p = Utils.superType(t);
        if (p == null)
            ctx.setClassScope(scope, t); // t == java.lang.Object
        else {
            ScopeInfo parent = getParentScopeAndVisit(p, errNode);
            if (parent.isCaller())
                ctx.setClassScope(scope, t);
            else if (scope.equals(parent))
                ctx.setClassScope(parent, t);
            else {
                // Set the scope to something, in case processing continues past
                // this class
                ctx.setClassScope(scope, t);
                fail(ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE, node, errNode, t, p);
            }
        }
        // Ensure that the class doesn't change any Scope annotations on its
        // implemented interfaces. This shouldn't require the retrieval of
        // all interfaces implemented by superclasses and interfaces, since
        // they should be visited as well prior to this point.
        for (TypeMirror i : t.getInterfaces()) {
            TypeElement ie = Utils.getTypeElement(i);
            ScopeInfo is = getParentScopeAndVisit(ie, errNode);
            if (!is.isCaller() && !is.equals(scope))
                fail(ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE, node, errNode, t, ie);
        }

        for (ExecutableElement c : Utils.constructorsIn(t)) {
            MethodTree mTree = trees.getTree(c);
            Tree mErr = mTree != null ? mTree : errNode;
            checkConstructor(c, mTree, mErr);
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

    private void checkConstructor(ExecutableElement m, MethodTree mTree,
            Tree mErr) {
        RunsIn runsIn = m.getAnnotation(RunsIn.class);
        if (runsIn != null) {
            String msg = "\n\t ERROR class is :" + m.getEnclosingElement() + "." + m;
            fail(ERR_RUNS_IN_ON_CONSTRUCTOR, mTree, mErr, msg);
        }

        checkMethod(m, mTree, mErr);
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
        checkMethodParameters(m, mTree, errNode);
        debugIndentDecrement();
    }

    void checkMethodParameters(ExecutableElement m, MethodTree mTree,
            Tree errNode) {
        List<? extends VariableElement> params = m.getParameters();
        List<? extends VariableTree> paramTrees = mTree != null ? mTree
                .getParameters() : null;
        for (int i = 0; i < params.size(); i++) {
            // TODO: Check overridden annotations
            VariableElement p = params.get(i);
            VariableTree pTree = paramTrees != null ? paramTrees.get(i) : null;
            ScopeInfo scope = checkMethodParameter(p, pTree, i, m, errNode);
            ctx.setParameterScope(scope, i, m);
        }
    }

    ScopeInfo checkMethodParameter(VariableElement p, VariableTree tree, int i,
            ExecutableElement m, Tree errNode) {
        ScopeInfo scope = checkVariableScopeOverride(p, tree, errNode);
        ScopeInfo effectiveScope = scope;
        if (scope.isCaller())
            effectiveScope = ctx.getEffectiveMethodRunsIn(m,
                    getEnclosingClassScope(m), ScopeInfo.CALLER);
        scope = checkMemoryAreaVariable(p, effectiveScope, tree, errNode);
        return scope;
    }

    ScopeInfo checkField(VariableElement f, Tree node, Tree errNode) {
        ScopeInfo scope = checkVariableScopeOverride(f, node, errNode);
        ScopeInfo classScope = getEnclosingClassScope(f);

        if (Utils.isStatic(f)) {
            if (!scope.isValidStaticScope())
                fail(ERR_ILLEGAL_STATIC_FIELD_SCOPE, node, errNode, scope);
            scope = ScopeInfo.IMMORTAL;
        } else if (!scope.isValidInstanceFieldScope(classScope, scopeTree))
            fail(ERR_ILLEGAL_FIELD_SCOPE, node, errNode, scope, classScope);

        scope = checkMemoryAreaVariable(f, scope, node, errNode);
        return scope;
    }

    private ScopeInfo checkMemoryAreaVariable(VariableElement v,
            ScopeInfo effectiveVarScope, Tree node, Tree errNode) {
        if (!Utils.isUserLevel(v))
            return effectiveVarScope;
        if (v.asType().getKind() != TypeKind.DECLARED)
            return effectiveVarScope;
        if (!needsDefineScope(Utils.getTypeElement(v.asType())))
            return effectiveVarScope;

        DefineScope ds = v.getAnnotation(DefineScope.class);
        if (ds == null) {
            fail(ERR_MEMORY_AREA_NO_DEFINE_SCOPE, node, errNode);
            return effectiveVarScope.representing(ScopeInfo.INVALID);
        }

        ScopeInfo scope = new ScopeInfo(ds.name());
        ScopeInfo parent = new ScopeInfo(ds.parent());

        if (!scopeTree.hasScope(scope) || !scopeTree.isParentOf(scope, parent)) {
            fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT, node, errNode);
        }

        if (!effectiveVarScope.equals(parent))
            fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE, node,
                    errNode, effectiveVarScope, parent);

        return effectiveVarScope.representing(scope);
    }

    /**
     * Check that a variable has a valid Scope annotation, if any. Four kinds of
     * variables are considered:
     * <ol>
     * <li>Primitive variables have no scope
     * <li>Primitive array variables are CALLER if not annotated, and S if
     * annotated Scope(S)
     * <li>Object variables are CALLER if not annotated, Scope(S) if the type of
     * the variable is annotated Scope(S), or S if the type of the variable is
     * not annotated and the field is annotated Scope(S).
     * <li>Object arrays follow the same rules as object variables based on the
     * type of their basic element type.
     */
    ScopeInfo checkVariableScopeOverride(VariableElement v, Tree node,
            Tree errNode) {
        debugIndent("checkVariableScopeOverride: " + v);
        TypeMirror mv = v.asType();
        TypeMirror bmv = Utils.getBaseType(mv);
        ScopeInfo ret;

        if (bmv.getKind() == TypeKind.DECLARED)
            getParentScopeAndVisit(Utils.getTypeElement(bmv), errNode);

        ScopeInfo defaultScope = Utils.getDefaultVariableScope(v, ctx);

        if (!defaultScope.isValidVariableScope(v, scopeTree))
            fail(ERR_BAD_SCOPE_NAME, node, errNode, defaultScope);

        if (Utils.isPrimitive(mv))
            ret = ScopeInfo.PRIMITIVE;
        else if (Utils.isPrimitiveArray(mv))
            ret = defaultScope;
        else if (bmv.getKind() == TypeKind.TYPEVAR)
            ret = defaultScope;
        else {
            ScopeInfo stv = ctx.getClassScope(Utils.getTypeElement(bmv));
            if (stv.isCaller())
                ret = defaultScope;
            else {
                if (defaultScope.isUnknown() || !defaultScope.equals(stv))
                    fail(ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE, node, errNode,
                            defaultScope, stv);
                ret = stv;
            }
        }
        return ret;
    }

    /**
     * Check that a method has a valid RunsIn annotation. A method's RunsIn
     * annotation is valid as long as it exists in the scope tree, or is CALLER
     * or THIS. It is also illegal to change the RunsIn of an overridden method,
     * unless it is annotated SUPPORT.
     */
    void checkMethodRunsIn(ExecutableElement m, MethodTree node, Tree errNode) {
        RunsIn ann = m.getAnnotation(RunsIn.class);
        ScopeInfo runsIn = ann != null ? new ScopeInfo(ann.value())
                : Utils.getDefaultMethodRunsIn(m);

        // UNKNOWN is no longer a valid RunsIn annotation.
        if (!runsIn.isValidRunsIn(scopeTree))
            fail(ERR_BAD_SCOPE_NAME, node, errNode, runsIn);
        if (Utils.isStatic(m) && runsIn.isThis())
            fail(ERR_BAD_SCOPE_NAME, node, errNode, runsIn);

        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
                .overriddenMethods(m);
        for (ExecutableElement e : overrides.values()) {
            ScopeInfo eRunsIn = getOverrideRunsInAndVisit(e, errNode);
            SCJAllowed eLevelAnn = e.getAnnotation(SCJAllowed.class);
            Level eLevel = eLevelAnn != null ? eLevelAnn.value() : null;
            // eLevel can be SUPPORT or INFRASTRUCTURE, though overriding an
            // INFRASTRUCTURE method in user code is illegal. This part is
            // checked in SCJAllowedVisitor; we include INFRASTRUCTURE here
            // because we're pulling in SCJ types.
            if (!eRunsIn.equals(runsIn) && Utils.isUserLevel(eLevel))
                fail(ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE, node, errNode);
        }
        ctx.setMethodRunsIn(runsIn, m);
    }

    /**
     * Check that a method has a valid Scope annotation. A method's Scope
     * annotation is valid as long as it exists in the scope tree, or is
     * UNKNOWN, CALLER, or THIS. It is also illegal to change the RunsIn of an
     * overridden method, unless it is annotated SUPPORT.
     */
    void checkMethodScope(ExecutableElement m, MethodTree node, Tree errNode) {
        Scope ann = m.getAnnotation(Scope.class);
        ScopeInfo scope = ann != null ? new ScopeInfo(ann.value())
                : ScopeInfo.CALLER;
        // TODO: Need to take class annotations into consideration

        if (!scopeTree.hasScope(scope) && !scope.isCaller() && !scope.isThis()
                && !scope.isUnknown())
            fail(ERR_BAD_SCOPE_NAME, node, errNode, scope);

        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
                .overriddenMethods(m);
        for (ExecutableElement e : overrides.values()) {
            ScopeInfo eScope = getOverrideScopeAndVisit(e, errNode);
            SCJAllowed eLevelAnn = e.getAnnotation(SCJAllowed.class);
            Level eLevel = eLevelAnn != null ? eLevelAnn.value() : null;
            if (!eScope.equals(scope) && eLevel != SUPPORT)
                fail(ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE, node, errNode);
        }
        TypeKind r = m.getReturnType().getKind();
        if ((r.isPrimitive() || r == TypeKind.VOID) && ann != null)
            warn(ERR_SCOPE_ON_VOID_OR_PRIMITIVE_RETURN, node, errNode);

        DefineScope ds = m.getAnnotation(DefineScope.class);
        if (ds != null) {
            ScopeInfo name = new ScopeInfo(ds.name());
            ScopeInfo parent = new ScopeInfo(ds.parent());

            if (!scopeTree.hasScope(name)
                    || !scopeTree.isParentOf(name, parent))
                fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT, node, errNode);

            if (!scope.equals(parent))
                fail(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE, node,
                        errNode, scope, parent);

            scope = scope.representing(name);
        }
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
        else if (r.isFailure())
            // Current item is something from a library. Can't put an error on
            // it, so put an error on the node being visited stating that
            // something from the parent class or interface is broken. If the
            // result is a warning, we ignore it, since they are purely
            // informational.
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
                : ScopeInfo.CALLER;
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
        // Don't need the same check that getOverrideScopeAndVisit has, because
        // this method is called after it, ensuring that the method RunsIn is
        // already in the cache.
        return runsIn;
    }

    /**
     * Get a method or field's owning class.
     */
    private ScopeInfo getEnclosingClassScope(Element e) {
        return ctx.getClassScope((TypeElement) e.getEnclosingElement());
    }
}
