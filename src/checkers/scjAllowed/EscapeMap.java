package checkers.scjAllowed;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.safetycritical.annotate.Level;

import checkers.Utils;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;

public class EscapeMap {
    private static final Map<String, Level> escape = new HashMap<String, Level>();

    static {
        escape.put("Array", Level.LEVEL_0);
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

    public static Level get(String str) {
        if (escape.containsKey(str))
            return escape.get(str);

        // this allows also package names
        for (Map.Entry<String, Level> entry : escape.entrySet())
            if (str.startsWith(entry.getKey()))
                return escape.get(entry.getKey());

        return null;
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
