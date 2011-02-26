package checkers.scope;

import static checkers.scope.DefineScopeChecker.ERR_SCOPE_HAS_NO_PARENT;

import java.util.HashMap;
import java.util.Map;

import checkers.source.Result;

import com.sun.source.tree.Tree;

public class ScopeTree {
    private Map<ScopeInfo, ScopeInfo> scopeTree = null;    // maps child <-> parent
    private Map<ScopeInfo, Tree> scopeMap = null;     // points to a place where each scope is defined

    public ScopeTree() {
        scopeTree = new HashMap<ScopeInfo, ScopeInfo>();
        scopeMap = new HashMap<ScopeInfo, Tree>();
        put(ScopeInfo.IMMORTAL, new ScopeInfo(""), null);
    }

    public ScopeInfo get(ScopeInfo name) {
        return scopeTree.get(name);
    }

    public boolean hasScope(ScopeInfo name) {
        return scopeTree.containsKey(name);
    }

    public boolean isParentOf(ScopeInfo child, ScopeInfo parent) {
        if (child.isCurrent())
            return false;

        while (child != null) {
            if (child.equals(parent))
                return true;
            child = get(child);
        }
        return false;
    }

    public boolean isAncestorOf(ScopeInfo name, ScopeInfo expectedParent) {
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

    public void put(ScopeInfo name, ScopeInfo parent, Tree node) {
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
        for (Map.Entry<ScopeInfo, ScopeInfo> entry : scopeTree.entrySet()) {
            ScopeInfo scope = entry.getKey();
            ScopeInfo parent = entry.getValue();
            if (!hasScope(parent) && !scope.isImmortal()) {
                checker.report(Result.failure(ERR_SCOPE_HAS_NO_PARENT,
                        scope, parent), scopeMap.get(scope));
            }
        }
    }
}
