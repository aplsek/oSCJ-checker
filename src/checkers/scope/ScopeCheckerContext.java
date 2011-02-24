package checkers.scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

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
        classScopes = new HashMap<String, ClassScopeInfo>();
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
     * Get the Scope annotation of a field by its fully qualified class name
     * and its own name.
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
        return getFieldScope(t.getQualifiedName().toString(),
                f.getSimpleName().toString());
    }

    public List<ScopeInfo> getParameterScopes(String clazz, String method,
            String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        if (csi != null) {
            String sig = buildSignatureString(method, params);
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
    public ScopeInfo getMethodRunsIn(ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        return getMethodRunsIn(t.getQualifiedName().toString(),
                m.getSimpleName().toString(), getParameterTypeNames(m));
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
    public ScopeInfo getMethodScope(ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        return getMethodScope(t.getQualifiedName().toString(),
                m.getSimpleName().toString(), getParameterTypeNames(m));
    }

    public List<ScopeInfo> getParameterScopes(ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        return getParameterScopes(t.getQualifiedName().toString(),
                m.getSimpleName().toString(), getParameterTypeNames(m));
    }

    /**
     * Get the effective RunsIn of a method. This translates a CURRENT
     * annotation to something more concrete, if available.
     */
    public ScopeInfo getEffectiveMethodRunsIn(ExecutableElement m) {
        ScopeInfo methodRunsIn = getMethodRunsIn(m);
        if (!methodRunsIn.isCurrent())
            return methodRunsIn;

        TypeElement clazz = (TypeElement) m.getEnclosingElement();
        ScopeInfo scope = getClassScope(clazz);
        
        //TODO: see the getEffectiveMethodScope() for the issue of "enclosing classes"
        return scope;
    }

    /**
     * Get the effective Scope of a method. This translates a CURRENT
     * annotation to something more concrete, if available.
     *
     * TODO: enclosing classes
     *
     * @return - CURRENT if the enclosing classes are not annotated.
     */
    public ScopeInfo getEffectiveMethodScope(ExecutableElement m) {
        if (!getMethodScope(m).isCurrent())
            return getMethodScope(m);

        TypeElement clazz = (TypeElement) m.getEnclosingElement();
        ScopeInfo scope = getClassScope(clazz);

        //Utils.debugPrintln("clazz :" + clazz);
        /*
         * TODO: enclosing class may change this, see the issue on "enclosing classes"
        while (clazz != null || scope.equals(CURRENT)) {
            scope = getClassScope(clazz);
            Element cl = clazz.getEnclosingElement();
            if (cl instanceof TypeElement)
                clazz = (TypeElement) clazz.getEnclosingElement();
            else
                break;
        }*/

        return scope;
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
        setFieldScope(scope, t.getQualifiedName().toString(),
                f.getSimpleName().toString());
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
        String sig = buildSignatureString(method, params);
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
     * Store the RunsIn annotation of a class, given its class declaration.
     * Does not work if a different RunsIn annotation is already stored.
     */
    public void setMethodRunsIn(ScopeInfo scope, ExecutableElement m) {
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
    public void setMethodScope(ScopeInfo scope, String clazz, String method,
            String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        String sig = buildSignatureString(method, params);
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
     * Store the Scope annotation of a class, given its class declaration.
     * Does not work if a different Scope annotation is already stored.
     */
    public void setMethodScope(ScopeInfo scope, ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        String[] params = getParameterTypeNames(m);
        setMethodScope(scope, t.getQualifiedName().toString(),
                m.getSimpleName().toString(), params);
    }

    public void setParameterScope(ScopeInfo scope, int i, String clazz,
            String method, String... params) {
        ClassScopeInfo csi = classScopes.get(clazz);
        String sig = buildSignatureString(method, params);
        MethodScopeInfo msi = csi.methodScopes.get(sig);
        ScopeInfo psi = msi.parameters.get(i);
        if (psi != null && !psi.equals(scope)) {
            throw new RuntimeException("Parameter scope already set");
        }
        msi.parameters.set(i, scope);
    }

    public void setParameterScope(ScopeInfo scope, int i, ExecutableElement m) {
        TypeElement t = Utils.getMethodClass(m);
        setParameterScope(scope, i, t.getQualifiedName().toString(),
                m.getSimpleName().toString(), getParameterTypeNames(m));
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
            for( String key: methodScopes.keySet() ){
                System.out.println("\t key:" + key + "  - " + methodScopes.get(key));
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
}
