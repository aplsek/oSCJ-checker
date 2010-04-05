package checkers.scope;

import java.util.HashMap;
import java.util.Map;

public abstract class ScopeTree {
    private static Map<String, String> scopeTree = null;
    
    public static String get(String name) {
        return scopeTree.get(name);
    }
    
    public static void initialize() {
        scopeTree = new HashMap<String, String>();
        put("immortal", "");
    }
    
    public static boolean isInitialized() {
        return scopeTree != null;
    }
    
    public static boolean isParentOf(String name, String expectedParent) {
        while (name != null) {
            if (name.equals(expectedParent)) {
                return true;
            }
            name = get(name);
        }
        return false;
    }
    
    public static void put(String name, String parent) {
        scopeTree.put(name, parent);
    }
}
