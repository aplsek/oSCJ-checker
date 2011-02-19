package checkers.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static javax.safetycritical.annotate.Scope.*;
import checkers.source.Result;

import com.sun.source.tree.Tree;

import static checkers.scope.ScopeChecker.*;

public class ScopeTree {
    private Map<String, String> scopeTree = null;    // maps child <-> parent
    private Map<String, Tree> scopeMap = null;     // points to a place where each scope is defined

    /**
     * We store here the errors in case the ScopeTree is not consistent
     */
    private List<Error> errors = new ArrayList<Error>();
    private boolean errorsReported = false;

    public String get(String name) {
        return scopeTree.get(name);
    }

    public void initialize() {
        scopeTree = new HashMap<String, String>();
        scopeMap = new HashMap<String, Tree>();
        put(IMMORTAL, "", null);
    }

    public boolean isInitialized() {
        return scopeTree != null;
    }

    public boolean hasScope(String name) {
        return scopeTree.containsKey(name);
    }

    public boolean isParentOf(String name, String expectedParent) {
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

    public boolean isAncestorOf(String name, String expectedParent) {
        if (expectedParent == null)
            return false;

        //printTree();
        if (expectedParent.equals(name)) // if they are the same, its not ancestor
            return false;

        while (name != null) {
            if (name.equals(expectedParent)) {
                return true;
            }
            name = get(name);
        }
        return false;
    }

    public void put(String name, String parent, Tree node) {
        scopeTree.put(name, parent);
        scopeMap.put(name, node);
    }

    public void printTree() {
        System.out.println("SCOPE TREE : \n" + scopeTree.toString());
    }

    /**
     * Checks that it forms tree
     *
     * @return null if all is ok.
     */
    public boolean verifyScopeTree() {
        if (errorsReported)
            return true;

        //printTree();

        // all parents are DefScope
        for (Map.Entry<String, String> entry : scopeTree.entrySet()) {
            if (!isDefined(entry.getValue())) {
                if (entry.getValue().equals("")) // for the case "immortal="
                    continue;
                errors.add(new Error("Scope *" + entry.getValue()
                        + "* is not defined but is parent to other scope.",
                        scopeMap.get(entry.getKey())));
            }
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

        return errors.isEmpty();
    }

    private boolean isDefined(String entry) {
        return scopeTree.containsKey(entry);
    }

    public void reportErrors(ScopeVisitor<?> visitor) {
        if (errorsReported)
            return;
        errorsReported = true;

        for (Error err: errors)
            visitor.report(Result.failure(ERR_SCOPE_TREE_NOT_CONSISTENT, err.scope), err.node);
    }

    static class Error {
        public String scope;
        public Tree node;

        public Error(String sc, Tree n) {
            scope = sc;
            node = n;
        }
    }
}
