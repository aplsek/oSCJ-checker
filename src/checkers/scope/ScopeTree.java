package checkers.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static javax.safetycritical.annotate.Scope.*;
import checkers.source.Result;

import com.sun.source.tree.Tree;

public abstract class ScopeTree {
    private static Map<String, String> scopeTree = null;    // maps child <-> parent
    private static Map<String, Tree> scopeMap = null;     // points to a place where each scope is defined
    
    /**
     * we store here the errors in case the ScopeTree is not consistent
     */
    private static List<Error> errors = new ArrayList<Error>();
    private static boolean errorsReported = false;
    
    public static String get(String name) {
        return scopeTree.get(name);
    }
    
    public static void initialize() {
        scopeTree = new HashMap<String, String>();
        scopeMap = new HashMap<String, Tree>();
        put(IMMORTAL, "", null);
    }
    
    public static boolean isInitialized() {
        return scopeTree != null;
    }
    
    public static boolean hasScope(String name) {
        return scopeTree.containsKey(name);
    }
    
    public static boolean isParentOf(String name, String expectedParent) {
        //printTree();
        if (expectedParent == null) 
            return false;
            
        while (name != null) {
            if (name.equals(expectedParent)) {
                return true;
            }
            name = get(name);
        }
        return false;
    }
    
    public static void put(String name, String parent, Tree node) {
        scopeTree.put(name, parent);
        scopeMap.put(name, node);
    }
    
    public static void printTree() {
        System.out.println("SCOPE TREE : \n" + scopeTree.toString());
    }
    
    /**
     * Checks that it forms tree
     * 
     * @return null if all is ok.
     */
    public static boolean verifyScopeTree() {
        if (errorsReported) 
            return true;
        
        //printTree();
        
        // all parents are DefScope
        for(Map.Entry<String,String> entry : scopeTree.entrySet()) 
            if(!isDefined(entry.getValue())) {
                if (entry.getValue().equals(""))   // for the case "immortal="
                    continue;
                errors.add(new Error("Scope *" + entry.getValue() + "* is not defined but is parent to other scope.",
                        scopeMap.get(entry.getKey())));
                
            }
        /*
        // all scopes are connected with root
        for(Map.Entry<String,String> entry : scopeTree.entrySet()) 
            if(!connectsWithRoot(entry.getKey())) {
                if (entry.getValue().equals(""))   // for the case "immortal="
                    continue;
                //errors.put("Scopes do not form a tree! Scope " + entry.getValue() + " is not reachable from the Immortal memory!",
                //        scopeMap.get(entry.getKey()));
                errors.add(new Error("Scopes do not form a tree! Scope " + entry.getValue() + " is not reachable from the Immortal memory!",
                               scopeMap.get(entry.getKey())));
                
                System.out.println("error:" + entry.getKey() + " val:" + entry.getValue());
            }
        */

        //also, there should not be any cycles, but this is already checked...
        
        if (errors.isEmpty()) 
            return true;
        else
            return false;
    }
    
    private static boolean connectsWithRoot(String key) {
        if (isParentOf(key,IMMORTAL))
            return true;
        else
            return false;
    }

    private static boolean isDefined(String entry) {
        if (scopeTree.containsKey(entry))
            return true;
        else
            return false;
    }
    
    
    public static void  reportErrors(ScopeVisitor visitor) {
        if (errorsReported)
            return;
        errorsReported = true;
       
        for (Error err: errors) 
            visitor.report(Result.failure("scope.Tree.not.consistent",err.scope), err.node);
        return;
    }
}



class Error {
    public String scope;
    public Tree node;
    
    public Error(String sc, Tree n) {
        scope = sc;
        node = n;
    }
}
