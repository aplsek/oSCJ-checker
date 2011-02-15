package checkers.scope;

import static javax.lang.model.util.ElementFilter.methodsIn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import checkers.Utils;
import checkers.scope.ScopeTree;
import checkers.util.TypesUtils;


import static checkers.scope.ScopeChecker.*;


public class ScopeCheckerContext {
    private final Elements elements;

    public ScopeCheckerContext(ScopeChecker checker) {
        this.elements = checker.getProcessingEnvironment().getElementUtils();
    }

    //
    // Class @Scope and @RunsIn annotations
    //
    private final Map<String, ScopeResult> classScopes = new HashMap<String, ScopeResult>();
    private final Map<String, ScopeResult> classRunsIns = new HashMap<String, ScopeResult>();

    public String getScope(String clazz) throws ScopeException {
        TypeElement t = elements.getTypeElement(clazz);
        if (t == null) {

            Utils.debugPrintln("class is :" + clazz);
            Utils.debugPrintln("t is null :" + t);

            if (clazz.equals("")) {
                Utils.debugPrintln("class is EMPTY!! :" + clazz);
                return null;
            }

            dummyException();
            return null;
        } else {
            return getScope(t);
        }
    }

    public String getScope(TypeElement t) throws ScopeException {
        return getScopeInternal(MapType.SCOPE, t);
    }

    public String getRunsIn(String clazz) throws ScopeException {
        TypeElement t = elements.getTypeElement(clazz);
        if (t == null) {
            if (clazz.equals("")) {
                Utils.debugPrintln("class is EMPTY!! :" + clazz);
                return null;
            }

            dummyException();
            return null;
        } else {
            return getRunsIn(t);
        }
    }

    public TypeElement getTypeElement(String clazz) {
        return elements.getTypeElement(clazz);
    }

    public String getRunsIn(TypeElement t) throws ScopeException {
        return getScopeInternal(MapType.RUNS_IN, t);
    }

    private String getScopeInternal(MapType mapType, TypeElement t) throws ScopeException {
        // System.out.println("  getting scope");
        // printContext();

        Map<String, ScopeResult> map = mapType == MapType.SCOPE ? classScopes : classRunsIns;
        String name = t.getQualifiedName().toString();

        // System.out.println("  name :" + name);
        if (name.equals("byte"))
            return IMMORTAL;

        ScopeResult res = map.get(name);

        if (res == null) {
            res = getScopeInternalSlow(mapType, t);
            map.put(name, res);
        }

        if (res.isError) {
            throw new ScopeException(res.name);
        } else {
            return res.name;
        }
    }

    private ScopeResult getScopeInternalSlow(MapType mapType, TypeElement t) {
        if (t == null) {
            dummyException();
        }
        String nodeAnno = getScopeValue(t, mapType);

        if (t.getKind() == ElementKind.CLASS) {
            TypeElement s = Utils.superType(t);
            while (s != null && !TypesUtils.isObject(s.asType())) {
                String superAnno = getScopeValue(s, mapType);
                if (superAnno != null) {
                    if (nodeAnno != null && !nodeAnno.equals(superAnno)) {
                        // Child classes must have the same annotation as their
                        // parent, or none at all
                        return new ScopeResult(String.format(
                                "Class %s has a disagreeing @%s annotation from parent class %s", s, mapType, t), true);
                    }
                    nodeAnno = superAnno;
                    break;
                }
                s = Utils.superType(s);
            }
        }
        // All interfaces should have the same annotation, if any
        // TODO: Maybe unnecessary to get parent interfaces, since they'll be
        // visited individually later
        HashSet<TypeElement> ifaces = Utils.getAllInterfaces(t);
        HashMap<String, ArrayList<TypeElement>> annos = new HashMap<String, ArrayList<TypeElement>>(ifaces.size());
        for (TypeElement iface : ifaces) {
            String scope = getScopeValue(iface, mapType);
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
            return new ScopeResult(String.format(
                    "One or more interfaces of class %s has a mismatching @%s annotation.", t, mapType), true);
        }

        if (!scopeExists(nodeAnno)) {
            return new ScopeResult(String.format("Class %s has a scope annotation with no matching @DefineScope", t),
                    true);
        }

        return new ScopeResult(nodeAnno, false);
    }

    //
    // Method @RunsIn annotations
    //
    private final Map<String, ScopeResult> methodRunsIns = new HashMap<String, ScopeResult>();

    public String getRunsIn(ExecutableElement m) throws ScopeException {
        String methodName = m.getSimpleName().toString();
        TypeElement methodEnv = (TypeElement) m.getEnclosingElement();
        String methodKey = methodEnv.getQualifiedName() + "#" + methodName;
        // Method key needs to include parameter types to handle overloads
        for (VariableElement var : m.getParameters()) {
            methodKey += var.asType().toString();
        }
        ScopeResult res = methodRunsIns.get(methodKey);
        // TODO: methodRunsIns.size() is always 0!!!

        // System.out.println("methodRusnIns size:" + methodRunsIns.size());

        if (res == null) {
            res = getRunsInSlow(m);
        }

        // System.out.println("ScopeCheckerContext: getRunsIn: " +
        // res.toString());
        // System.out.println("ScopeCheckerContext: getRunsIn name: " +
        // res.name);

        if (res.isError) {
            throw new ScopeException(res.name);
        } else {
            return res.name;
        }
    }

    private ScopeResult getRunsInSlow(ExecutableElement m) throws ScopeException {
        String methodName = m.getSimpleName().toString();
        TypeElement methodEnv = (TypeElement) m.getEnclosingElement();
        String methodEnvScope = getScope(methodEnv);
        String runsIn = getScopeValue(m, MapType.RUNS_IN);

        // System.out.println("\t method:" + methodName);
        // System.out.println("\t method:" + methodEnv);
        // System.out.println("\t method:" + methodEnvScope);
        // System.out.println("\t runsIn:" + runsIn);
        //
        // System.out.println("\t method element:" + m);
        // System.out.println("\t method element ann:" +
        // m.getAnnotationMirrors());

        // debugIndent("\t method:" + methodName);
        // debugIndent("\t method:" + methodEnv);
        // debugIndent("\t method:" + methodEnvScope);
        // debugIndent("\t runsIn:" + runsIn);

        if (methodName.startsWith("<init>")) {
            return new ScopeResult(methodEnvScope, false);
        } else if (methodName.startsWith("<clinit>")) {
            return new ScopeResult(Scope.IMMORTAL, false);
        } else {
            if (!scopeExists(runsIn)) {
                return new ScopeResult(String.format("Scope %s does not exist.", runsIn), true);
            }
            if (runsIn != null) {
                Collection<ExecutableElement> overrides = orderedOverriddenMethods(m);
                for (ExecutableElement override : overrides) {
                    String overriddenRunsIn = Utils.runsIn(override.getAnnotationMirrors());
                    if (overriddenRunsIn != null && !runsIn.equals(overriddenRunsIn)) {
                        // A method must have the same @RunsIn as its overrides,
                        // or none at all
                        return new ScopeResult(ERR_BAD_RUNS_IN_OVERRIDE, true);
                    }
                }
                return new ScopeResult(runsIn, false);
            } else {
                for (ExecutableElement override : orderedOverriddenMethods(m)) {
                    String overriddenRunsIn = Utils.runsIn(override.getAnnotationMirrors());
                    if (overriddenRunsIn != null) {
                        return new ScopeResult(ERR_BAD_RUNS_IN_OVERRIDE, true);
                    }
                }
                String methodScope = getRunsIn(methodEnv.getQualifiedName().toString());
                if (methodScope == null) {
                    methodScope = getScope(methodEnv.getQualifiedName().toString());
                }
                return new ScopeResult(methodScope, false);
            }
        }
    }

    //
    // Helpers
    //

    private boolean scopeExists(String scope) {
        return scope == null || scope.equals(UNKNOWN) || ScopeTree.get(scope) != null;
    }

    private String getScopeValue(TypeElement t, MapType mapType) {
        String ret;
        if (mapType == MapType.SCOPE) {
            Scope s = t.getAnnotation(Scope.class);
            ret = s == null ? null : s.value();
        } else {
            RunsIn r = t.getAnnotation(RunsIn.class);
            ret = r == null ? null : r.value();
        }
        return ret;
    }

    private String getScopeValue(ExecutableElement m, MapType mapType) {
        String ret = null;
        if (mapType == MapType.RUNS_IN) {
            RunsIn r = m.getAnnotation(RunsIn.class);
            ret = r == null ? null : r.value();
        }
        return ret;
    }

    private void dummyException() {
        throw new RuntimeException("add a custom exception and message here");
    }

    // Like ats.overriddenMethods(), except not a map and is guaranteed to
    // iterate in hierarchical order.
    private Collection<ExecutableElement> orderedOverriddenMethods(ExecutableElement method) {
        TypeElement enclosing = (TypeElement) method.getEnclosingElement();

        if (enclosing.getKind() == ElementKind.INTERFACE) {
            return orderedOverriddenMethodsInterface(method, enclosing);
        }

        LinkedList<ExecutableElement> overrides = new LinkedList<ExecutableElement>();
        if (!TypesUtils.isObject(enclosing.asType())) {
            TypeElement superType = Utils.superType(enclosing);
            if (superType == null) {
                Utils.debugPrintln("super type null!!!");
                return overrides;
            }

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
                superType = Utils.superType(superType);
            }
        }
        return overrides;
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

    enum MapType {
        SCOPE {
            public String toString() {
                return "Scope";
            }
        },
        RUNS_IN {
            public String toString() {
                return "RunsIn";
            }
        }
    }

    public void printContext() {
        System.out.println("SCope context is:" + classScopes.size());
        Iterator iterator = classScopes.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            ScopeResult value = classScopes.get(key);

            System.out.println("\t" + key + "\t: " + value.name);
        }

        System.out.println("RUNsIn context is:" + classRunsIns.size());
        Iterator iterator2 = classRunsIns.keySet().iterator();
        while (iterator2.hasNext()) {
            String key = iterator2.next().toString();
            ScopeResult value = classRunsIns.get(key);

            // System.out.println("\t" +key + "\t: " + value.name);
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
}
