package checkers;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import checkers.scope.ScopeCheckerContext;
import checkers.scope.ScopeInfo;
import checkers.util.TypesUtils;

public final class Utils {
    /**
     * debugging flag
     */
    public static boolean DEBUG = false;
    private static String indent = "";

    public static void increaseIndent() {
        indent += " ";
    }

    public static void decreaseIndent() {
        indent = indent.substring(1);
    }

    public static void resetDebugIndent() {
        indent = "";
    }

    public static void debugPrint(String msg) {
        if (DEBUG) System.err.print(indent + msg);
    }

    public static void debugPrintln(String msg) {
        if (DEBUG) System.err.println(indent + msg);
    }

    public static boolean isAbstract(Element e) {
        return e.getModifiers().contains(Modifier.ABSTRACT);
    }

    public static boolean isFinal(Element e) {
        return e.getModifiers().contains(Modifier.FINAL);
    }

    public static boolean isPublic(Element e) {
        return e.getModifiers().contains(Modifier.PUBLIC);
    }

    public static boolean isStatic(Element e) {
        return e.getModifiers().contains(Modifier.STATIC);
    }

    public static boolean isAllocFree(ExecutableElement methodElement) {
        SCJRestricted r = methodElement.getAnnotation(SCJRestricted.class);
        if (r != null && r.value() != null) {
            return !r.mayAllocate();
        }
        return false;
    }

    public static TypeElement superType(TypeElement type) {
        if (TypesUtils.isObject(type.asType())) { return null; }
        if (type.getSuperclass().toString().equals("<none>")) return null;

        return getTypeElement(type.getSuperclass());
    }

    public static TypeElement getTypeElement(Elements elements, String clazz) {
        return elements.getTypeElement(clazz);
    }

    public static TypeMirror getTypeMirror(Elements elements, String clazz) {
        return getTypeElement(elements, clazz).asType();
    }

    /**
     * Get an object representing the declaration of the class that contains
     * a method.
     *
     * @param m the method whose class declaration is needed
     * @return an object representing the class declaration
     */
    public static TypeElement getMethodClass(ExecutableElement m) {
        return (TypeElement) m.getEnclosingElement();
    }

    /**
     * Get an object representing the declaration of the class that contains
     * a field.
     *
     * @param f the field whose class declaration is needed
     * @return an object representing the class declaration
     */
    public static TypeElement getFieldClass(VariableElement f) {
        return (TypeElement) f.getEnclosingElement();
    }

    /**
     * Convert a TypeMirror object to a TypeElement object. Assumes that the
     * TypeMirror is actually convertible to a TypeElement (aka is not a
     * primitive type, etc.)
     *
     * @param mirror the TypeMirror to convert
     * @return the TypeElement representing the TypeMirror's class
     */
    public static TypeElement getTypeElement(TypeMirror mirror) {
        return (TypeElement) ((DeclaredType) mirror).asElement();
    }

    /**
     * Given a method declaration, construct a String that uniquely identifies
     * a method.
     */
    public static String buildSignatureString(ExecutableElement m) {
        return buildSignatureString(m.getSimpleName().toString(),
                getParameterTypeNames(m));
    }

    /**
     * Given a method name and a list of parameter names, construct a String
     * that uniquely identifies a method.
     *
     * For example, if a method has a Java type signature:
     *
     *     String foo(Bar1 a1, Bar2 a2)
     *
     * this method yields the string foo(Bar1,Bar2)
     */
    public static String buildSignatureString(String method, String... params) {
        int size = method.length() + params.length + 2;
        for (String param : params) {
            size += param.length();
        }

        StringBuilder sb = new StringBuilder(size);
        sb.append(method);
        sb.append('(');
        int len = params.length;
        int i = 0;
        for (String param : params) {
            sb.append(param);
            if (++i < len)
                sb.append(',');

        }
        sb.append(')');
        return sb.toString();
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

    /**
     * Get the RunsIn annotation of a method given its declaration.
     */
    public static String getMethodSignature(ExecutableElement m) {
        String sig = buildSignatureString(m.getSimpleName().toString(),
                getParameterTypeNames(m));
        return sig;
    }

    public enum SCJMethod {
        DEFAULT {
            @Override public String toString() { return null; }
        },

        /* MemoryArea */

        NEW_INSTANCE {
            @Override public String toString() { return "newInstance(java.lang.Class)"; }
        },

        NEW_INSTANCE_IN_AREA {
            @Override public String toString() { return "newInstanceInArea(java.lang.Object,java.lang.Class)"; }
        },

        NEW_ARRAY {
            @Override public String toString() { return "newArray(java.lang.Class,int)"; }
        },

        NEW_ARRAY_IN_AREA {
            @Override public String toString() { return "newArrayInArea(java.lang.Object,java.lang.Class,int)"; }
        },

        ENTER {
            @Override public String toString() { return "enter(int,java.lang.Runnable)"; }
        },

        GET_MEMORY_AREA {
            @Override public String toString() { return "getMemoryArea(java.lang.Object)"; }
        },

        /* AllocationContext */

        EXECUTE_IN_AREA {
            @Override public String toString() { return "executeInArea(javax.safetycritical.SCJRunnable)"; }
        },

        /* ManagedMemory */

        ENTER_PRIVATE_MEMORY {
            @Override public String toString() { return "enterPrivateMemory(long,javax.safetycritical.SCJRunnable)"; }
        },

        GET_CURRENT_MANAGED_MEMORY {
            @Override public String toString() { return "getCurrentManagedMemory()"; }
        },

        ALLOC_IN_SAME {
            @Override public String toString() { return "allocInSame(java.lang.Object,java.lang.Object)"; }
        },

        ALLOC_IN_PARENT {
            @Override public String toString() { return "allocInParent(java.lang.Object,java.lang.Object)"; }
        };
    }

    public static List<ExecutableElement> constructorsIn(TypeElement t) {
        return ElementFilter.constructorsIn(t.getEnclosedElements());
    }

    public static List<ExecutableElement> methodsIn(TypeElement t) {
        return ElementFilter.methodsIn(t.getEnclosedElements());
    }

    public static List<VariableElement> fieldsIn(TypeElement t) {
        return ElementFilter.fieldsIn(t.getEnclosedElements());
    }

    /**
     * Get the base component type of an array.
     * <p>
     * The base component type of int[][][] is int.
     */
    public static TypeMirror getBaseType(TypeMirror t) {
        while (t.getKind() == TypeKind.ARRAY) {
            t = ((ArrayType) t).getComponentType();
        }
        return t;
    }

    public static final String JAVAX_REALTIME = "javax.realtime";
    public static final String JAVAX_SAFETYCRITICAL = "javax.safetycritical";

    /**
     * Given a declaration, see if it's user level code.
     *
     * User level code is defined as anything outside of the javax.realtime and
     * javax.safetycritical packages.
     */
    public static boolean isUserLevel(Element e) {
        ElementKind k = e.getKind();
        while (!(k.isClass() || k.isInterface())) {
            e = e.getEnclosingElement();
            k = e.getKind();
        }
        TypeElement t = (TypeElement) e;
        String name = t.getQualifiedName().toString();
        return !(name.startsWith(JAVAX_REALTIME) || name
                .startsWith(JAVAX_SAFETYCRITICAL));
    }

    public static boolean isUserLevel(Level l) {
        return !(l == Level.INFRASTRUCTURE || l == Level.SUPPORT);
    }

    public static ScopeInfo getDefaultMethodRunsIn(ExecutableElement m) {
        if (Utils.isStatic(m))
            return ScopeInfo.CALLER;
        return ScopeInfo.THIS;
    }

    public static ScopeInfo getDefaultVariableScope(VariableElement v,
            ScopeCheckerContext ctx) {
        TypeKind k = v.asType().getKind();
        if (k.isPrimitive())
            return ScopeInfo.PRIMITIVE;
        Scope s = v.getAnnotation(Scope.class);
        if (s != null)
            return new ScopeInfo(s.value());
        if (k == TypeKind.DECLARED) {
            TypeElement t = Utils.getTypeElement(v.asType());
            ScopeInfo si = ctx.getClassScope(t);
            if (!si.isCaller())
                return si;
        }
        if (Utils.isStatic(v))
            return ScopeInfo.IMMORTAL;
        else if (v.getKind() == ElementKind.FIELD)
            return ScopeInfo.THIS;
        else
            return ScopeInfo.CALLER;
    }

    public static boolean isPrimitive(TypeMirror m) {
        return m.getKind().isPrimitive();
    }

    public static boolean isPrimitiveArray(TypeMirror m) {
        return Utils.getBaseType(m).getKind().isPrimitive();
    }
}
