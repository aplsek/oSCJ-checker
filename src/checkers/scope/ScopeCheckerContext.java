package checkers.scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import checkers.Utils;

/**
 * Context object that stores information about scopes in a program.
 *
 * This class is meant to be simply a container with no validation. It is
 * assumed that the checkers that add data to the context (i.e., DefineScope and
 * ScopeRunsIn checkers) perform all validation.
 */
public class ScopeCheckerContext {
    /**
     * A scope tree constructed by processing DefineScope annotations.
     */
    private ScopeTree scopeTree;
    /**
     * A map of fully qualified class names to relevant scope information.
     */
    private Map<String, ClassInfo> classScopes;

    public ScopeCheckerContext() {
        scopeTree = new ScopeTree();
        classScopes = new HashMap<String, ClassInfo>();
    }

    public ScopeTree getScopeTree() {
        return scopeTree;
    }

    /**
     * Get the Scope annotation of a class by its fully qualified name.
     */
    public ScopeInfo getClassScope(String clazz) {
        ClassInfo ci = classScopes.get(clazz);
        if (ci != null)
            return ci.scope;
        return null;
    }

    /**
     * Get the Scope annotation of a class by its declaration.
     */
    public ScopeInfo getClassScope(TypeElement t) {
        return getClassScope(t.getQualifiedName().toString());
    }

    /**
     * Get the Scope annotation of a field by its fully qualified class name and
     * its own name.
     */
    public ScopeInfo getFieldScope(String clazz, String field) {
        // XXX Hacky way to support X.class field accesses
        if ("class".equals(field)) {
            return ScopeInfo.IMMORTAL;
        }
        return classScopes.get(clazz).fieldScopes.get(field);
    }

    /**
     * Get the Scope annotation of a field by its declaration.
     */
    public ScopeInfo getFieldScope(VariableElement f) {
        TypeElement t = Utils.getFieldClass(f);

        return getFieldScope(t.getQualifiedName().toString(), f.getSimpleName()
                .toString());
    }

    /**
     * Get the RunsIn annotation of a method given its fully qualified class
     * name, its own name, and the fully qualified names of the types of its
     * parameters.
     */
    public ScopeInfo getMethodRunsIn(String clazz, String method,
            String... params) {
        ClassInfo ci = classScopes.get(clazz);
        if (ci != null) {
            String sig = Utils.buildSignatureString(method, params);
            MethodScopeInfo msi = ci.methodScopes.get(sig);
            if (msi != null)
                return msi.runsIn;
        }
        return null;
    }

    /**
     * Get the RunsIn annotation of a method given its declaration.
     */
    public ScopeInfo getMethodRunsIn(ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        return getMethodRunsIn(t.getQualifiedName().toString(), m
                .getSimpleName().toString(), getParameterTypeNames(m));
    }

    /**
     * Get the Scope annotation of a method given its fully qualified class
     * name, its own name, and the fully qualified names of the types of its
     * parameters.
     */
    public ScopeInfo getMethodScope(String clazz, String method,
            String... params) {
        ClassInfo ci = classScopes.get(clazz);
        if (ci != null) {
            String sig = Utils.buildSignatureString(method, params);
            MethodScopeInfo msi = ci.methodScopes.get(sig);
            if (msi != null)
                return msi.scope;
        }
        return null;
    }

    /**
     * Get the Scope annotation of a method given its declaration.
     */
    public ScopeInfo getMethodScope(ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        return getMethodScope(t.getQualifiedName().toString(), m
                .getSimpleName().toString(), getParameterTypeNames(m));
    }

    /**
     * Get the Scope annotations on a method's parameters by its fully qualified
     * class name and its own name.
     */
    public List<ScopeInfo> getParameterScopes(String clazz, String method,
            String... params) {
        ClassInfo ci = classScopes.get(clazz);
        if (ci != null) {
            String sig = Utils.buildSignatureString(method, params);
            MethodScopeInfo msi = ci.methodScopes.get(sig);
            if (msi != null)
                return Collections.unmodifiableList(msi.parameters);
        }
        return null;
    }

    /**
     * Get the Scope annotations on a method's parameters by its declaration.
     */
    public List<ScopeInfo> getParameterScopes(ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        return getParameterScopes(t.getQualifiedName().toString(), m
                .getSimpleName().toString(), getParameterTypeNames(m));
    }

    /**
     * Get the effective RunsIn of a method at one of its call sites. This
     * translates a CALLER or THIS annotation to something more concrete, if
     * available.
     *
     * <ul>
     * <li>If the method annotation is RunsIn(CALLER), the effective RunsIn is
     * the current scope.
     * <li>If the method annotation is RunsIn(THIS), the effective RunsIn is the
     * receiver object.
     * <li>Otherwise, the effective RunsIn is the annotation that was calculated
     * from the ScopeRunsInVisitor.
     */
    public ScopeInfo getEffectiveMethodRunsIn(ExecutableElement m,
            ScopeInfo recvScope, ScopeInfo currentScope) {
        ScopeInfo methodRunsIn = getMethodRunsIn(m);
        if (methodRunsIn.isCaller())
            return currentScope;
        if (methodRunsIn.isThis())
            return recvScope;
        return methodRunsIn;
    }

    /**
     * Get the effective return Scope of a method at one of its call sites. This
     * translates a CALLER or THIS annotation to something more concrete, if
     * available.
     *
     * <ul>
     * <li>If the method annotation is Scope(CALLER), the effective Scope is the
     * current scope.
     * <li>If the method annotation is Scope(THIS), the effective Scope is the
     * receiver object.
     * <li>Otherwise, the effective Scope is the annotation that was calculated
     * from the ScopeRunsInVisitor.
     */
    public ScopeInfo getEffectiveMethodScope(ExecutableElement m,
            ScopeInfo recvScope, ScopeInfo currentScope) {
        ScopeInfo methodScope = getMethodScope(m);
        if (methodScope.isCaller())
            return currentScope;
        if (methodScope.isThis())
            return recvScope;
        return methodScope;
    }

    /**
     * Store the Scope annotation of a class, given its fully qualified class
     * name. Does not work if a different Scope annotation is already stored.
     */
    public void setClassScope(ScopeInfo scope, String clazz) {
        ClassInfo ci = classScopes.get(clazz);
        if (ci != null && ci.scope != null && !ci.scope.equals(scope))
            throw new RuntimeException("Class scope already set");
        if (ci == null)
            ci = new ClassInfo();
        ci.scope = scope;
        classScopes.put(clazz, ci);
    }

    /**
     * Store the Scope annotation of a class, given its class declaration. Does
     * not work if a different Scope annotation is already stored.
     */
    public void setClassScope(ScopeInfo scope, TypeElement t) {
        setClassScope(scope, t.getQualifiedName().toString());
    }

    /**
     * Store the Scope annotation of a field given its fully qualified class
     * name and its own name.
     */
    public void setFieldScope(ScopeInfo scope, String clazz, String field) {
        ClassInfo ci = classScopes.get(clazz);
        ScopeInfo f = ci.fieldScopes.get(field);
        if (f != null && !f.equals(scope))
            throw new RuntimeException("Field scope already set");
        if (scope.isCaller())
            throw new RuntimeException("Bad scope");
        ci.fieldScopes.put(field, scope);
    }

    /**
     * Store the Scope annotation of a field, given its declaration.
     */
    public void setFieldScope(ScopeInfo scope, VariableElement f) {
        TypeElement t = Utils.getFieldClass(f);
        setFieldScope(scope, t.getQualifiedName().toString(), f.getSimpleName()
                .toString());
    }

    /**
     * Store the RunsIn annotation of a method, given its fully qualified class
     * name, its own name, and the fully qualified names of the types of its
     * parameters. Does not work if a different RunsIn annotation is already
     * stored.
     */
    public void setMethodRunsIn(ScopeInfo scope, String clazz, String method,
            String... params) {
        ClassInfo ci = classScopes.get(clazz);
        String sig = Utils.buildSignatureString(method, params);
        MethodScopeInfo msi = ci.methodScopes.get(sig);
        if (msi != null) {
            if (msi.runsIn != null && !msi.runsIn.equals(scope))
                throw new RuntimeException("Method runsin already set");
        } else {
            msi = new MethodScopeInfo(params.length);
            ci.methodScopes.put(sig, msi);
        }
        msi.runsIn = scope;
    }

    /**
     * Store the RunsIn annotation of a class, given its class declaration. Does
     * not work if a different RunsIn annotation is already stored.
     */
    public void setMethodRunsIn(ScopeInfo scope, ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        String[] params = getParameterTypeNames(m);
        setMethodRunsIn(scope, t.getQualifiedName().toString(), m
                .getSimpleName().toString(), params);
    }

    /**
     * Store the Scope annotation of a method, given its fully qualified class
     * name, its own name, and the fully qualified names of the types of its
     * parameters. Does not work if a different Scope annotation is already
     * stored.
     */
    public void setMethodScope(ScopeInfo scope, String clazz, String method,
            String... params) {
        ClassInfo ci = classScopes.get(clazz);
        String sig = Utils.buildSignatureString(method, params);
        MethodScopeInfo msi = ci.methodScopes.get(sig);
        if (msi != null) {
            if (msi.scope != null && !msi.scope.equals(scope))
                throw new RuntimeException("Method scope already set");
        } else {
            msi = new MethodScopeInfo(params.length);
            ci.methodScopes.put(sig, msi);
        }
        msi.scope = scope;
    }

    /**
     * Store the Scope annotation of a class, given its class declaration. Does
     * not work if a different Scope annotation is already stored.
     */
    public void setMethodScope(ScopeInfo scope, ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        String[] params = getParameterTypeNames(m);
        setMethodScope(scope, t.getQualifiedName().toString(), m
                .getSimpleName().toString(), params);
    }

    /**
     * Set the Scope annotation of a method parameter, given the method's name
     * and its fully qualified class name.
     */
    public void setParameterScope(ScopeInfo scope, int i, String clazz,
            String method, String... params) {
        ClassInfo ci = classScopes.get(clazz);
        String sig = Utils.buildSignatureString(method, params);
        MethodScopeInfo msi = ci.methodScopes.get(sig);
        ScopeInfo psi = msi.parameters.get(i);
        if (psi != null && !psi.equals(scope))
            throw new RuntimeException("Parameter scope already set");
        msi.parameters.set(i, scope);
    }

    /**
     * Set the Scope annotation of a method parameter, given the method's
     * declaration and the parameter index.
     */
    public void setParameterScope(ScopeInfo scope, int i, ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        setParameterScope(scope, i, t.getQualifiedName().toString(), m
                .getSimpleName().toString(), getParameterTypeNames(m));
    }

    /**
     * Get an array of String objects, where the ith String represents the name
     * of the ith parameter type of a given method.
     */
    static String[] getParameterTypeNames(ExecutableElement m) {
        List<? extends VariableElement> params = m.getParameters();
        int paramsSize = params.size();
        String[] paramsArray = new String[paramsSize];
        for (int i = 0; i < paramsSize; i++)
            paramsArray[i] = params.get(i).asType().toString();
        return paramsArray;
    }

    public ClassInfo testCI(TypeMirror exprType) {
        ClassInfo expr = classScopes.get(exprType.toString());
        return expr;
    }

    public ClassInfo getClassInfo(String clazz) {
        ClassInfo ci = classScopes.get(clazz);
        if (ci == null) {
            // try to deal with generics
            clazz = clazz.substring(0, clazz.indexOf("<"));
            ci = classScopes.get(clazz);
        }
        return ci;
    }

    /**
     * Determines if a given subtype can be upcasted to a given supertype.
     * @return true if upcast is safe.
     */
    public boolean isSafeUpcast(TypeMirror exprType, TypeMirror castType) {

        ClassInfo expr = getClassInfo(exprType.toString());
        ClassInfo cast = getClassInfo(castType.toString());

        if (expr == null) {
            // TODO: this e.g. happens for generics!!!
            throw new RuntimeException("ClassScopes: given class is not in the map of the classscopes: " + exprType.toString());
        }


        if (!Utils.isUserLevel(exprType.toString()) && !Utils.isUserLevel(castType.toString())) {
            // ignore upcasting between SCJ classes (classes from javax.safetycritical and javax.realtime)
            // Note: This is for example for the executeInArea() method that has different @RunsIn but we need to upcast here.
            return true;
        }

        for (Entry<String, MethodScopeInfo> e : cast.methodScopes.entrySet()) {
            if (expr.methodScopes.containsKey(e.getKey())) {            // TODO: check that its a signature
                MethodScopeInfo eM = expr.methodScopes.get(e.getKey());
                if (!e.getValue().runsIn.equals(eM.runsIn) ) {
                    if (!isSupport(castType,e.getKey()))
                        return false;
                }
            }
        }
        return true;
    }

    /**
     * return true if a give method is declared as SUPPORT
     */
    private boolean isSupport(TypeMirror castType, String method) {
        TypeElement elem = Utils.getTypeElement(castType);
        List<? extends Element> elements = elem.getEnclosedElements();
        for (Element meth : elements ) {
            if (meth instanceof ExecutableElement)
                if (Utils.buildSignatureString((ExecutableElement)meth).equals(method))
                    if (Utils.hasSUPPORT(meth))
                        return true;
        }
        return false;
    }

    static class ClassInfo {
        ScopeInfo scope;
        /**
         * A map of method signatures to method scope information.
         */
        Map<String, MethodScopeInfo> methodScopes;
        Map<String, ScopeInfo> fieldScopes;

        ClassInfo() {
            methodScopes = new HashMap<String, MethodScopeInfo>();
            fieldScopes = new HashMap<String, ScopeInfo>();
        }

        public void dumpCSI() {
            System.out.println("Method Scopes : " + methodScopes.size());
            for (String key : methodScopes.keySet()) {
                System.out.print("\t key:" + key + "  - ");
                methodScopes.get(key).dump();
            }

            System.out.println(" fieldScopes : " + fieldScopes.size());
            for (String key : fieldScopes.keySet())
                System.out.println("\t key:" + key + "  - "
                        + fieldScopes.get(key));
        }
    }

    static class MethodScopeInfo {
        ScopeInfo scope;
        ScopeInfo runsIn;
        List<ScopeInfo> parameters;

        MethodScopeInfo(int params) {
            parameters = new ArrayList<ScopeInfo>(params);
            for (int i = 0; i < params; i++)
                parameters.add(null);
        }

        public void dump() {
            System.out.print(" @RunsIn(" + runsIn + ")" + ", @Scope(" + scope
                    + "), args:");
            for (ScopeInfo sc : parameters) {
                System.out.print(sc.getScope() + ", ");
            }
            System.out.println();
        }
    }

    public void dumpClassInfo(String str) {
        System.out.println("\n\n============ CLASS INFO-=========");
        System.out.println("class:" + str);
        for (Entry<String, ClassInfo> e : classScopes.entrySet()) {
            if (e.getKey().contains(str)) {
                e.getValue().dumpCSI();
            }

        }
        System.err.println("============ CLASS INFO-=========\n\n");
    }

    public void dumpClassScopes() {
        System.out.println("\n\n============ CLASS SCOPES-=========");
        for (Entry<String, ClassInfo> e : classScopes.entrySet()) {
            System.out.println("class: " + e.getKey() + ",@Scope("
                    + e.getValue().scope + ")");
        }
        System.out.println("============ CLASS SCOPES-=========\n\n");
    }
}
