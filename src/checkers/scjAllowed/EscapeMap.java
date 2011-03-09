package checkers.scjAllowed;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.safetycritical.annotate.Level;

import checkers.Utils;
import checkers.scope.ScopeInfo;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;

public class EscapeMap {
    public static final Map<String, Level> escape = new HashMap<String, Level>();

    static {
        escape.put("java.lang.Object", Level.LEVEL_0);
        escape.put("java.lang.String", Level.LEVEL_0);
        escape.put("java.lang.StringBuffer", Level.LEVEL_0);
        escape.put("java.lang.Float", Level.LEVEL_0);
        escape.put("java.lang.Math", Level.LEVEL_0);
        escape.put("java.lang.Thread", Level.LEVEL_0);
        escape.put("java.lang.Runtime", Level.LEVEL_0);
        escape.put("java.lang.Runnable", Level.LEVEL_0);

        escape.put("java.lang.Eception", Level.LEVEL_0);
        escape.put("System", Level.LEVEL_0);
        escape.put("System.out.println", Level.LEVEL_0);
        escape.put("java.lang.System", Level.LEVEL_0);
        escape.put("java.lang.ArithmeticException", Level.LEVEL_0);
        escape.put("java.lang.RuntimeException", Level.LEVEL_0);

        escape.put("java.lang.Exception", Level.LEVEL_0);
        escape.put("java.lang.Throwable", Level.LEVEL_0);
        escape.put("java.util.LinkedList", Level.LEVEL_0);
        escape.put("java.util.HashMap", Level.LEVEL_0);
        escape.put("java.lang.InterruptedException", Level.LEVEL_0);
        escape.put("java.lang.IllegalMonitorStateException", Level.LEVEL_0);
        escape.put("java.util.BitSet", Level.LEVEL_0);

        escape.put("javax.realtime.ThrowBoundaryError", Level.LEVEL_0);
        escape.put("java.lang.annotation.RetentionPolicy", Level.LEVEL_0);
        escape.put("java.lang.annotation.ElementType", Level.LEVEL_0);
        escape.put("javax.safetycritical.annotate.Level", Level.LEVEL_0);

        escape.put("java.io.PrintStream", Level.LEVEL_0);

        escape.put("byte", Level.LEVEL_0);
        escape.put("Array", Level.LEVEL_0);
        escape.put("java.util", Level.LEVEL_0);
        escape.put("java.lang.Integer", Level.LEVEL_0);
        escape.put("java.lang.Long", Level.LEVEL_0);
    }

    public static boolean isEscaped(String str) {
        if (escape.containsKey(str))
            return true;

        // this allows also package names
        for (Map.Entry<String, Level> entry : escape.entrySet())
            if (str.startsWith(entry.getKey()))
                return true;

        return false;
    }

    public static boolean escapeEnum(ClassTree node) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);
        TypeElement superType = Utils.superType(t);

        if (superType != null && superType.toString().equals("java.lang.Enum")) {
            Utils.debugPrintln("escaping enum!! " + superType);

            return true;
        }
        return false;
    }

    public static boolean escapeAnnotation(ClassTree node) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);

        if (t != null && t.toString().contains(".annotate.")
                || t.toString().contains("annotation")) {
            Utils.debugPrintln("escaping Annotation!! " + t);
            return true;
        }
        return false;
    }
}
