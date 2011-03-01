package checkers.scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, ClassScopeInfo> classScopes;

    /**
     * A map of fully qualified class names to relevant DefineScope information - only for fields that must
     * have a @DefineScope annotation.
     */
    private Map<String, DefineScopeInfo> memDefineScope;

    public ScopeCheckerContext() {
        scopeTree = new ScopeTree();
        classScopes = new HashMap<String, ClassScopeInfo>();
        memDefineScope = new HashMap<String, DefineScopeInfo>();
    }

    public ScopeTree getScopeTree() {
        return scopeTree;
    }

    /**
     * Get the Scope annotation of a class by its fully qualified name.
     */
    public ScopeInfo getClassScope(String clazz) {
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null) {
            return csi.scope;
        }
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
        ClassScopeInfo csi = classScopes.get(clazz);
        return csi.fieldScopes.get(field);
    }

    /**
     * Get the Scope annotation of a field by its declaration.
     */
    public ScopeInfo getFieldScope(VariableElement f) {
        TypeElement t = Utils.getFieldClass(f);
        return getFieldScope(t.getQualifiedName().toString(), f.getSimpleName()
                .toString());
    }

    public List<ScopeInfo> getParameterScopes(String clazz, String method,
            String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null) {
            String sig = Utils.buildSignatureString(method, params);
            MethodScopeInfo msi = csi.methodScopes.get(sig);
            if (msi != null) {
                return Collections.unmodifiableList(msi.parameters);
            }
        }
        return null;
    }

    /**
     * Get the RunsIn annotation of a method given its fully qualified class
     * name, its own name, and the fully qualified names of the types of its
     * parameters.
     */
    public ScopeInfo getMethodRunsIn(String clazz, String method,
            String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null) {
            String sig = Utils.buildSignatureString(method, params);
            MethodScopeInfo msi = csi.methodScopes.get(sig);
            if (msi != null) {
                return msi.runsIn;
            }
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
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null) {
            String sig = Utils.buildSignatureString(method, params);
            MethodScopeInfo msi = csi.methodScopes.get(sig);
            if (msi != null) {
                return msi.scope;
            }
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

    public List<ScopeInfo> getParameterScopes(ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        return getParameterScopes(t.getQualifiedName().toString(), m
                .getSimpleName().toString(), getParameterTypeNames(m));
    }

    /**
     * Get the effective RunsIn of a method. This translates a CURRENT
     * annotation to something more concrete, if available.
     *
     * The "effective" methods get a scope annotation relative to the receiver
     * object. you have to see that the scope of the receiver is "b" and say
     * that the effective method runs-in is "b" because the annotation is
     * CURRENT.
     */
    public ScopeInfo getEffectiveMethodRunsIn(ExecutableElement m,
            ScopeInfo recvScope) {
        ScopeInfo methodRunsIn = getMethodRunsIn(m);
        if (!methodRunsIn.isCurrent() || Utils.isStatic(m.getModifiers()))
            return methodRunsIn;

        TypeElement clazz = (TypeElement) m.getEnclosingElement();
        ScopeInfo scope = getClassScope(clazz);

        // if the scope is CURRENT, we need to consider the Scope of the
        // receiver object.
        return scope.isCurrent() ? recvScope : scope;
    }

    /**
     * Get the effective Scope of a method. This translates a CURRENT annotation
     * to something more concrete, if available, based on the scope of the
     * receiver.
     */
    public ScopeInfo getEffectiveMethodScope(ExecutableElement m,
            ScopeInfo recvScope) {
        if (!getMethodScope(m).isCurrent() || Utils.isStatic(m.getModifiers()))
            return getMethodScope(m);

        TypeElement clazz = (TypeElement) m.getEnclosingElement();
        ScopeInfo scope = getClassScope(clazz);

        // if the scope is CURRENT, we need to consider the Scope of the
        // receiver object.
        return scope.isCurrent() ? recvScope : scope;
    }

    /**
     * Store the Scope annotation of a class, given its fully qualified class
     * name. Does not work if a different Scope annotation is already stored.
     */
    public void setClassScope(ScopeInfo scope, String clazz) {
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null && !csi.scope.equals(scope)) {
            throw new RuntimeException("Class scope already set");
        }
        csi = new ClassScopeInfo(scope);
        classScopes.put(clazz, csi);
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
        ClassScopeInfo csi = classScopes.get(clazz);
        ScopeInfo f = csi.fieldScopes.get(field);
        if (f != null && !f.equals(scope)) {
            throw new RuntimeException("Field scope already set");
        }
        csi.fieldScopes.put(field, scope);
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
        ClassScopeInfo csi = classScopes.get(clazz);
        String sig = Utils.buildSignatureString(method, params);
        MethodScopeInfo msi = csi.methodScopes.get(sig);
        if (msi != null) {
            if (msi.runsIn != null && !msi.runsIn.equals(scope)) {
                throw new RuntimeException("Method runsin already set");
            }
        } else {
            msi = new MethodScopeInfo(params.length);
            csi.methodScopes.put(sig, msi);
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
        ClassScopeInfo csi = classScopes.get(clazz);
        String sig = Utils.buildSignatureString(method, params);
        MethodScopeInfo msi = csi.methodScopes.get(sig);
        if (msi != null) {
            if (msi.scope != null && !msi.scope.equals(scope)) {
                throw new RuntimeException("Method scope already set");
            }
        } else {
            msi = new MethodScopeInfo(params.length);
            csi.methodScopes.put(sig, msi);
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

    public void setParameterScope(ScopeInfo scope, int i, String clazz,
            String method, String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        String sig = Utils.buildSignatureString(method, params);
        MethodScopeInfo msi = csi.methodScopes.get(sig);
        ScopeInfo psi = msi.parameters.get(i);
        if (psi != null && !psi.equals(scope)) {
            throw new RuntimeException("Parameter scope already set");
        }
        msi.parameters.set(i, scope);
    }

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
        for (int i = 0; i < paramsSize; i++) {
            paramsArray[i] = params.get(i).asType().toString();
        }
        return paramsArray;
    }

    static class ClassScopeInfo {
        ScopeInfo scope;
        /**
         * A map of method signatures to method scope information.
         */
        Map<String, MethodScopeInfo> methodScopes;
        Map<String, ScopeInfo> fieldScopes;

        ClassScopeInfo(ScopeInfo scope) {
            this.scope = scope;
            methodScopes = new HashMap<String, MethodScopeInfo>();
            fieldScopes = new HashMap<String, ScopeInfo>();
        }

        public void dumpCSI() {
            System.out.println("\n\n\t size : " + methodScopes.size());
            for (String key : methodScopes.keySet()) {
                System.out.println("\t key:" + key + "  - "
                        + methodScopes.get(key));
            }
        }
    }

    static class MethodScopeInfo {
        ScopeInfo scope;
        ScopeInfo runsIn;
        List<ScopeInfo> parameters;

        MethodScopeInfo(int params) {
            parameters = new ArrayList<ScopeInfo>(params);
            for (int i = 0; i < params; i++) {
                parameters.add(null);
            }
        }
    }

    /**
     * Store the Scope annotation of a field, given its declaration.
     */
    public void setFieldDefineScope(DefineScopeInfo scope, VariableElement f) {
        TypeElement t = Utils.getFieldClass(f);
        setFieldDefineScope(scope, t.getQualifiedName().toString(), f.getSimpleName()
                .toString());
    }

    /**
     * Store the Scope annotation of a field given its fully qualified class
     * name and its own name.
     */
    public void setFieldDefineScope(DefineScopeInfo scope, String clazz, String field) {
        DefineScopeInfo dsi = memDefineScope.get(clazz + "." + field);
        if (dsi != null && !dsi.equals(scope)) {
            throw new RuntimeException("Field's @DefineScope already set!");
        }
        memDefineScope.put(clazz+field,scope);
    }

    /**
     * Get the @DefineScope annotation of a field by its fully qualified class name and
     * its own name.
     */
    public DefineScopeInfo getFieldDefineScope(String clazz, String field) {
        DefineScopeInfo csi = memDefineScope.get(clazz + "." + field);
        return csi;
    }

    /**
     * Get the @DefineScope annotation of a field by its declaration.
     */
    public DefineScopeInfo getFieldDefineScope(VariableElement f) {
        TypeElement t = Utils.getFieldClass(f);
        return getFieldDefineScope(t.getQualifiedName().toString(), f.getSimpleName()
                .toString());
    }

    public void dumpDefineScopes () {
        for (String field : memDefineScope.keySet()) {
            System.err.println("field: " + field + ", @DefineScope(" + memDefineScope.get(field).toString() + ")");
        }
    }
}
