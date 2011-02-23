package checkers.scope;

import static checkers.scope.DefineScopeChecker.ERR_SCOPE_TREE_NOT_CONSISTENT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import java.util.HashMap;
import java.util.Map;

import checkers.source.Result;

import com.sun.source.tree.Tree;

public class ScopeTree {
    private Map<String, String> scopeTree = null;    // maps child <-> parent
    private Map<String, Tree> scopeMap = null;     // points to a place where each scope is defined

    public ScopeTree() {
        scopeTree = new HashMap<String, String>();
        scopeMap = new HashMap<String, Tree>();
        put(IMMORTAL, "", null);
    }

    public String get(String name) {
        return scopeTree.get(name);
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
     * Check that the scope tree is indeed a tree.
     */
    public void checkScopeTree(DefineScopeChecker checker) {
        // all parents are DefScope
        for (Map.Entry<String, String> entry : scopeTree.entrySet()) {
            String scope = entry.getKey();
            String parent = entry.getValue();
            if (!hasScope(parent) && !scope.equals(IMMORTAL)) {
                checker.report(Result.failure(ERR_SCOPE_TREE_NOT_CONSISTENT,
                        scope, parent), scopeMap.get(scope));
            }
        }
    }
}
