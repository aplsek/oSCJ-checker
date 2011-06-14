package checkers;

import static javax.safetycritical.annotate.Level.HIDDEN;
import static javax.safetycritical.annotate.Level.SUPPORT;

import java.util.List;
import java.util.Map;

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
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

import checkers.scope.ScopeCheckerContext;
import checkers.scope.ScopeInfo;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypes;
import checkers.util.TypesUtils;

public final class Utils {
    private static Level defaultLevel = HIDDEN;
    public static boolean DEBUG = false;
    public static boolean SCOPE_CHECKS = true;
    private static String indent = "";

    public static void setDefaultLevel(Level l) {
        defaultLevel = l;
    }

    public static Level getDefaultLevel(Element e) {
        if (isUserLevel(e))
            return defaultLevel;
        else
            return HIDDEN;
    }

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
     * Get the expression at the base of an array access or a chain of array
     * accesses.
     */
    public static ExpressionTree getBaseTree(ExpressionTree tree) {
        while (tree.getKind() == Kind.ARRAY_ACCESS)
            tree = ((ArrayAccessTree) tree).getExpression();
        return tree;
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
        if (e == null)
            return true;

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

    public static boolean isUserLevel(String name) {
        if (name == null)
            return true;
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

    public static Level getSCJAllowedLevel(Element e) {
        SCJAllowed a = e.getAnnotation(SCJAllowed.class);
        if (a == null) {
            if (Utils.defaultLevel.isHIDDEN())
                return HIDDEN;
            else
                return Utils.defaultLevel;
        } else
            return a.value();
    }

    public static boolean isSCJSupport(ExecutableElement m, AnnotatedTypes ats) {
       // If we're in the user level with an SUPPORT annotation, we have
       // to see if the method overrides a @SCJAllowed(SUPPORT) method
        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
                .overriddenMethods(m);
       for (ExecutableElement override : overrides.values()) {
           if (getSCJAllowedLevel(override) == SUPPORT)
               return true;
       }
       return false;
    }

    public static boolean areSameType(AnnotatedTypeMirror t1, AnnotatedTypeMirror t2) {
        if (t1.equals(t2))
            return true;
        return false;
    }

    /**
     * Returns true if element is annotated by SCJAllowed
     */
    public static DefineScope getDefineScope(Element e) {
        DefineScope ds = e.getAnnotation(DefineScope.class);
        if (ds!= null)
            return ds;
        return null;
    }
}
