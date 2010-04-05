package checkers;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.AllocFree;
import checkers.types.AnnotatedTypes;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.TypesUtils;

public final class Utils {

    /**
     * debugging flag
     */
    public static boolean DEBUG = false;

    public static void debugPrint(String msg) {
        if (DEBUG) System.err.print(msg);
    }
    
    public static void debugPrintln(String msg) {
        if (DEBUG) System.err.println(msg);
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

    public static boolean isAllocFree(ExecutableElement methodElement, AnnotatedTypes ats) {
        if (methodElement.getAnnotation(AllocFree.class) != null) { return true; }
        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats.overriddenMethods(methodElement);
        for (ExecutableElement override : overrides.values()) {
            if (override.getAnnotation(AllocFree.class) != null) { return true; }
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
            TypeElement ifaceElement = (TypeElement) ((DeclaredType) iface).asElement();
            ret.add(ifaceElement);
            ret.addAll(getAllInterfaces(ifaceElement));
        }
        if (type.getKind() == ElementKind.CLASS && !TypesUtils.isObject(type.asType())) {
            ret.addAll(getAllInterfaces((TypeElement) ((DeclaredType) type.getSuperclass()).asElement()));
        }
        return ret;
    }
}
