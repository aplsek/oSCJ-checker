package checkers;

import static checkers.Utils.SCJ_METHODS.ALLOC_IN_PARENT;
import static checkers.Utils.SCJ_METHODS.ALLOC_IN_SAME;
import static checkers.Utils.SCJ_METHODS.DEFAULT;
import static checkers.Utils.SCJ_METHODS.ENTER_PRIVATE_MEMORY;
import static checkers.Utils.SCJ_METHODS.EXECUTE_IN_AREA;
import static checkers.Utils.SCJ_METHODS.GET_MEMORY_AREA;
import static checkers.Utils.SCJ_METHODS.NEW_ARRAY;
import static checkers.Utils.SCJ_METHODS.NEW_INSTANCE;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.safetycritical.annotate.SCJRestricted;

import checkers.types.AnnotatedTypes;
import checkers.util.TypesUtils;

public final class Utils {
    /**
     * debugging flag
     */
    public static boolean DEBUG = true;

    public static void debugPrint(String msg) {
        if (DEBUG) System.err.print(indent + msg);
    }

    public static void debugPrintln(String msg) {
        if (DEBUG) System.err.println(indent + msg);
    }

    public static void debugPrintException(Exception e) {
        System.err.println("\n\n Exception: " + e.getMessage());
    }

    public static String annotationValue(Collection<? extends AnnotationMirror> annotations, String annotation) {
        AnnotationMirror a = getAnnotation(annotations, annotation);
        if (a != null) { return (String) a.getElementValues().values().iterator().next().getValue(); }
        return null;
    }

    public static AnnotationMirror getAnnotation(Collection<? extends AnnotationMirror> annotations, String annotation) {
        for (AnnotationMirror anno : annotations) {
            if (anno.getAnnotationType().toString().equals(annotation)) { return anno; }
        }
        return null;
    }

    public static boolean isStatic(Collection<Modifier> modifiers) {
        return modifiers.contains(Modifier.STATIC);
    }

    public static boolean isFinal(Collection<Modifier> modifiers) {
        return modifiers.contains(Modifier.FINAL);
    }

    public static boolean isAllocFree(ExecutableElement methodElement, AnnotatedTypes ats) {
        SCJRestricted r;
        if ((r = methodElement.getAnnotation(SCJRestricted.class)) != null && r.value() != null) {
            return !r.mayAllocate();
        }
        return false;
    }

    public static String scope(Collection<? extends AnnotationMirror> annotations) {
        return annotationValue(annotations, "javax.safetycritical.annotate.Scope");
    }

    public static String runsIn(Collection<? extends AnnotationMirror> annotations) {
        return annotationValue(annotations, "javax.safetycritical.annotate.RunsIn");
    }

    public static HashSet<TypeElement> getAllInterfaces(TypeElement type) {
        HashSet<TypeElement> ret = new LinkedHashSet<TypeElement>();
        for (TypeMirror iface : type.getInterfaces()) {
            TypeElement ifaceElement = getTypeElement(iface);
            ret.add(ifaceElement);
            ret.addAll(getAllInterfaces(ifaceElement));
        }
        if (type.getKind() == ElementKind.CLASS && !TypesUtils.isObject(type.asType())) {
            ret.addAll(getAllInterfaces(getTypeElement(type.getSuperclass())));
        }
        return ret;
    }

    public static TypeElement superType(TypeElement type) {
        //Utils.debugPrintln("superType: " + TypesUtils.isObject(type.asType()) );
        //Utils.debugPrintln("as type: " + type.asType() );
        //Utils.debugPrintln("super : " + type.getSuperclass());

        if (TypesUtils.isObject(type.asType())) { return null; }
        if (type.getSuperclass().toString().equals("<none>")) return null;

        return (TypeElement) ((DeclaredType) type.getSuperclass()).asElement();
    }

    public static TypeElement getTypeElement(Elements elements, String clazz) {
        return elements.getTypeElement(clazz);
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

    private static String indent = "";

    public static void increaseIndent() {
        indent += " ";
    }

    public static void decreaseIndent() {
        indent = indent.substring(1);
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
        int len = params.length;
        int i = 0;
        for (String param : params) {
            sb.append(param);
            if (++i < len)
                sb.append(',');         // TODO: is this necessary? is this valid in the general case?

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
        TypeElement t = Utils.getMethodClass(m);
        String sig = buildSignatureString( m.getSimpleName().toString(), getParameterTypeNames(m));

        return sig;
    }

    public enum SCJ_METHODS {
        DEFAULT {
            @Override public String toString() { return null; } 
        },
        
        /* MemoryArea */
        
        NEW_INSTANCE { 
            @Override public String toString() { return "newInstance(java.lang.Class)"; } 
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
            @Override public String toString() { return "executeInArea(java.lang.Runnable)"; } 
        },
        
        
        /* ManagedMemory */
        
        ENTER_PRIVATE_MEMORY { 
            @Override public String toString() { return "enterPrivateMemory(long,java.lang.Runnable)"; } 
        },
        
        GET_CURRENT_MANAGED_AREA { 
            @Override public String toString() { return "getCurrentManagedMemory()"; } 
        },

        ALLOC_IN_SAME { 
            @Override public String toString() { return "allocatedInSame(java.lang.Object,java.lang.Object)"; } 
        },
       
        ALLOC_IN_PARENT { 
            @Override public String toString() { return "allocatedInParent(java.lang.Object,java.lang.Object)"; } 
        };
        
        
    }
    
    static private void pln(String s) {
        System.out.println(s);
    }

}
