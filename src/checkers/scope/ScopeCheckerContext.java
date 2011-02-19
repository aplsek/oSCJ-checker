package checkers.scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.safetycritical.annotate.Scope;

import checkers.Utils;

/**
 * Context object that stores information about scopes in a program.
 *
 * This class is meant to be simply a container with no validation. It is
 * assumed that the checkers that add data to the context (i.e., DefineScope
 * and ScopeRunsIn checkers) perform all validation.
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

    public ScopeCheckerContext() {
        scopeTree = new ScopeTree();
        scopeTree.initialize();
        classScopes = new HashMap<String, ClassScopeInfo>();
    }

    public ScopeTree getScopeTree() {
        return scopeTree;
    }

    /**
     * Get the Scope annotation of a class by its fully qualified name.
     */
    public String getClassScope(String clazz) {
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null) {
            return csi.scope;
        }
        return null;
    }

    /**
     * Get the Scope annotation of a class by its declaration.
     */
    public String getClassScope(TypeElement t) {
        return getClassScope(t.getQualifiedName().toString());
    }

    /**
     * Get the Scope annotation of a field by its fully qualified class name
     * and its own name.
     */
    public String getFieldScope(String clazz, String field) {
        ClassScopeInfo csi = classScopes.get(clazz);
        return csi.fieldScopes.get(field);
    }

    /**
     * Get the Scope annotation of a field by its declaration.
     */
    public String getFieldScope(VariableElement f) {
        TypeElement t = Utils.getFieldClass(f);
        return getFieldScope(t.getQualifiedName().toString(),
                f.getSimpleName().toString());
    }

    /**
     * Get the RunsIn annotation of a method given its fully qualified class
     * name, its own name, and the fully qualified names of the types of its
     * parameters.
     */
    public String getMethodRunsIn(String clazz, String method,
            String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null) {
            String sig = buildSignatureString(method, params);
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
    public String getMethodRunsIn(ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        return getMethodRunsIn(t.getQualifiedName().toString(),
                m.getSimpleName().toString(), getParameterTypeNames(m));
    }

    /**
     * Get the Scope annotation of a method given its fully qualified class
     * name, its own name, and the fully qualified names of the types of its
     * parameters.
     */
    public String getMethodScope(String clazz, String method,
            String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null) {
            String sig = buildSignatureString(method, params);
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
    public String getMethodScope(ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        return getMethodScope(t.getQualifiedName().toString(),
                m.getSimpleName().toString(), getParameterTypeNames(m));
    }

    /**
     * Get the effective RunsIn of a method. This translates a CURRENT
     * annotation to something more concrete, if available.
     */
    public String getEffectiveMethodRunsIn(ExecutableElement m) {
        // TODO
        return null;
    }

    /**
     * Get the effective Scope of a method. This translates a CURRENT
     * annotation to something more concrete, if available.
     */
    public String getEffectiveMethodScope(ExecutableElement m) {
        // TODO
        return null;
    }

    /**
     * Store the Scope annotation of a class, given its fully qualified class
     * name. Does not work if a different Scope annotation is already stored.
     */
    public void setClassScope(String scope, String clazz) {
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null && !scope.equals(csi.scope)) {
            throw new RuntimeException("Class scope already set");
        }
        csi = new ClassScopeInfo(scope);
        classScopes.put(clazz, csi);
    }

    /**
     * Store the Scope annotation of a class, given its class declaration. Does
     * not work if a different Scope annotation is already stored.
     */
    public void setClassScope(String scope, TypeElement t) {
        setClassScope(scope, t.getQualifiedName().toString());
    }

    /**
     * Store the Scope annotation of a field given its fully qualified class
     * name and its own name.
     */
    public void setFieldScope(String scope, String clazz, String field) {
        ClassScopeInfo csi = classScopes.get(clazz);
        String f = csi.fieldScopes.get(field);
        if (f != null && !f.equals(scope)) {
            throw new RuntimeException("Field scope already set");
        }
        csi.fieldScopes.put(field, scope);
    }

    /**
     * Store the Scope annotation of a field, given its declaration.
     */
    public void setFieldScope(String scope, VariableElement f) {
        TypeElement t = Utils.getFieldClass(f);
        setFieldScope(scope, t.getQualifiedName().toString(),
                f.getSimpleName().toString());
    }

    /**
     * Store the RunsIn annotation of a method, given its fully qualified class
     * name, its own name, and the fully qualified names of the types of its
     * parameters. Does not work if a different RunsIn annotation is already
     * stored.
     */
    public void setMethodRunsIn(String scope, String clazz, String method,
            String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        String sig = buildSignatureString(method, params);
        MethodScopeInfo msi = csi.methodScopes.get(sig);
        if (msi != null) {
            if (!scope.equals(msi.runsIn) && msi.runsIn != null) {
                throw new RuntimeException("Method runsin already set");
            }
        } else {
            msi = new MethodScopeInfo();
            csi.methodScopes.put(sig, msi);
        }
        msi.runsIn = scope;
    }

    /**
     * Store the RunsIn annotation of a class, given its class declaration.
     * Does not work if a different RunsIn annotation is already stored.
     */
    public void setMethodRunsIn(String scope, ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        String[] params = getParameterTypeNames(m);
        setMethodRunsIn(scope, t.getQualifiedName().toString(),
                m.getSimpleName().toString(), params);
    }

    /**
     * Store the Scope annotation of a method, given its fully qualified class
     * name, its own name, and the fully qualified names of the types of its
     * parameters. Does not work if a different Scope annotation is already
     * stored.
     */
    public void setMethodScope(String scope, String clazz, String method,
            String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        String sig = buildSignatureString(method, params);
        MethodScopeInfo msi = csi.methodScopes.get(sig);
        if (msi != null) {
            if (!scope.equals(msi.scope) && msi.scope != null) {
                throw new RuntimeException("Method scope already set");
            }
        } else {
            msi = new MethodScopeInfo();
            csi.methodScopes.put(sig, msi);
        }
        msi.scope = scope;
    }

    /**
     * Store the Scope annotation of a class, given its class declaration.
     * Does not work if a different Scope annotation is already stored.
     */
    public void setMethodScope(String scope, ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        String[] params = getParameterTypeNames(m);
        setMethodScope(scope, t.getQualifiedName().toString(),
                m.getSimpleName().toString(), params);
    }

    /**
     * Given a method name and a list of parameter names, construct a String
     * that uniquely identifies a method.
     *
     * For example, if a method has a Java type signature:
     *
     *     String foo(Bar1 a1, Bar2 a2)
     *
     * this method yields the string foo(Bar1,Bar2,)
     */
    static String buildSignatureString(String method, String... params) {
        int size = method.length() + params.length + 2;
        for (String param : params) {
            size += param.length();
        }

        StringBuilder sb = new StringBuilder(size);
        sb.append(method);
        sb.append('(');
        for (String param : params) {
            sb.append(param);
            sb.append(',');
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Given a method declaration, construct a String that uniquely identifies
     * a method.
     */
    static String buildSignatureString(ExecutableElement m) {
        return buildSignatureString(m.getSimpleName().toString(),
                getParameterTypeNames(m));
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
        String scope;
        /**
         * A map of method signatures to method scope information.
         */
        Map<String, MethodScopeInfo> methodScopes;
        Map<String, String> fieldScopes;

        public ClassScopeInfo(String scope) {
            this.scope = scope;
            methodScopes = new HashMap<String, MethodScopeInfo>();
            fieldScopes = new HashMap<String, String>();
        }
    }

    static class MethodScopeInfo {
        String scope;
        String runsIn;
    }
}
