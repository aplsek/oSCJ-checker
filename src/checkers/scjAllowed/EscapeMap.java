package checkers.scjAllowed;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;

import checkers.Utils;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;

public class EscapeMap {
    public static final Map<String, Integer> escape    = new HashMap<String, Integer>();

    static {
        escape.put("java.lang.Object", 0);
        escape.put("java.lang.String", 0);
        escape.put("java.lang.StringBuffer", 0);
        escape.put("java.lang.Float", 0);
        escape.put("java.lang.Math", 0);
        escape.put("java.lang.Thread", 0);
        escape.put("java.lang.Runtime", 0);
        escape.put("java.lang.Runnable", 0);
        
        escape.put("java.lang.Eception", 0);
        escape.put("System", 0);
        escape.put("System.out.println", 0);
        escape.put("java.lang.System", 0);
        escape.put("java.lang.ArithmeticException", 0);
        escape.put("java.lang.RuntimeException", 0);
        
        escape.put("java.lang.Exception", 0);
        escape.put("java.lang.Throwable", 0);
        escape.put("java.util.LinkedList", 0);
        escape.put("java.util.HashMap", 0);
        escape.put("java.lang.InterruptedException", 0);
        escape.put("java.lang.IllegalMonitorStateException", 0);
        escape.put("java.util.BitSet", 0);
        
        escape.put("javax.realtime.ThrowBoundaryError", 0);
        escape.put("java.lang.annotation.RetentionPolicy", 0);
        escape.put("java.lang.annotation.ElementType", 0);
        escape.put("javax.safetycritical.annotate.Level", 0);
        
        escape.put("java.io.PrintStream", 0);
        
        
        
        escape.put("byte", 0);
        escape.put("Array", 0);
        escape.put("java.lang.Integer", 0);
        escape.put("java.lang.Long", 0);
    }
    
    public static boolean isEscaped(String str) {
        //System.out.println("escape:" + str);
        if (escape.containsKey(str))
            return true;
        else
            return false;
    }
    
    
    public static boolean escapeEnum(ClassTree node) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);
        TypeElement superType = Utils.superType(t);
        
        if (superType != null &&
                superType.toString().equals("java.lang.Enum")) {
            Utils.debugPrintln("escaping enum!! " + superType);
            
            return true;
        }
        return false;
    }
    
    /**
     * @param node
     * @return
     */
    public static boolean escapeAnnotation(ClassTree node) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);
        
        if (t != null &&
                (t.toString().contains(".annotate.")) || t.toString().contains("annotation") ) {
            Utils.debugPrintln("escaping Annotation!! " + t);
            
            return true;
        }
        
        return false;
    }
    
    
}
