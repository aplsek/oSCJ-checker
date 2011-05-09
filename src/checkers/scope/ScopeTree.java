package checkers.scope;

import static checkers.scope.DefineScopeChecker.ERR_SCOPE_HAS_NO_PARENT;
import java.util.HashMap;
import java.util.Map;
import checkers.source.Result;
import com.sun.source.tree.Tree;

public class ScopeTree {
    private Map<ScopeInfo, ScopeInfo> scopeTree = null;    // maps child <-> parent
    private Map<ScopeInfo, Tree> scopeMap = null;     // points to a place where each scope is defined

    HashMap<String, Tree> errorScopes;

    public ScopeTree() {
        scopeTree = new HashMap<ScopeInfo, ScopeInfo>();
        scopeMap = new HashMap<ScopeInfo, Tree>();
        put(ScopeInfo.IMMORTAL, new ScopeInfo(""), null);

        errorScopes = new HashMap<String, Tree>();
    }

    public ScopeInfo getParent(ScopeInfo child) {
        if (child.isReservedScope())
            return null;

        return scopeTree.get(child);
    }

    public boolean hasScope(ScopeInfo name) {
        return scopeTree.containsKey(name);
    }

    /**
     * See if one scope is the direct parent of another.
     */
    public boolean isParentOf(ScopeInfo child, ScopeInfo parent) {
        if (child.isImmortal() && parent.isImmortal()) return true;
        return parent.equals(getParent(child));
    }

    /**
     * See if one scope is equal to or an ancestor of another.
     */
    public boolean isAncestorOf(ScopeInfo child, ScopeInfo parent) {
        // IMMORTAL is always an ancestor of CALLER or THIS.
        if (child.isCaller() || child.isThis())
            return parent.isImmortal();

        while (child != null) {
            if (child.equals(parent))
                return true;
            child = scopeTree.get(child);
        }
        return false;
    }

    public void put(ScopeInfo name, ScopeInfo parent, Tree node) {
        scopeTree.put(name, parent);
        scopeMap.put(name, node);
    }

    public void dumpTree() {
        for (Map.Entry<ScopeInfo, ScopeInfo> entry : scopeTree.entrySet()) {
            ScopeInfo scope = entry.getKey();
            ScopeInfo parent = entry.getValue();
            System.out.println("\t scope=" + scope + ", parent=" + parent);
        }
    }

    /**
     * Check that the scope tree is indeed a tree.
     */
    public void checkScopeTree(DefineScopeChecker checker) {
        for (Map.Entry<ScopeInfo, ScopeInfo> entry : scopeTree.entrySet()) {
            ScopeInfo scope = entry.getKey();
            ScopeInfo parent = entry.getValue();
            if (!hasScope(parent) && !scope.isImmortal()) {
               // checker.report(Result.failure(ERR_SCOPE_HAS_NO_PARENT,
               //         scope, parent), scopeMap.get(scope));

               errorScopes.put(parent.getScope(), scopeMap.get(scope));
            }
        }
    }

}
