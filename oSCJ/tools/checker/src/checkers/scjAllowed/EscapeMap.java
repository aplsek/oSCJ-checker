package checkers.scjAllowed;

import java.util.HashMap;
import java.util.Map;

public class EscapeMap {
    public static final Map<String, Integer> escape    = new HashMap<String, Integer>();

    static {
        escape.put("java.lang.Object", 0);
        escape.put("java.lang.InterruptedException", 0);
        escape.put("java.lang.IllegalMonitorStateException", 0);
        escape.put("java.util.BitSet", 0);
        
        escape.put("javax.realtime.ThrowBoundaryError", 0);
        escape.put("java.lang.annotation.RetentionPolicy", 0);
        escape.put("java.lang.annotation.ElementType", 0);
        escape.put("javax.safetycritical.annotate.Level", 0);
    }
    
    public static boolean isEscaped(String str) {
        if (escape.containsKey(str))
            return true;
        else
            return false;
    }
}
