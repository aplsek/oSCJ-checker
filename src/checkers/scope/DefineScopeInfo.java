package checkers.scope;

public class DefineScopeInfo {
    private ScopeInfo scope;
    private ScopeInfo parent;

    public DefineScopeInfo(ScopeInfo scope, ScopeInfo parent) {
        this.scope = scope;
        this.parent = parent;
    }

    public ScopeInfo getScope() {
        return scope;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null &&
            scope.equals(((DefineScopeInfo)obj).scope) && parent.equals(((DefineScopeInfo)obj).parent);
    }

    @Override
    public int hashCode() {
        return 31 * scope.hashCode() + parent.scope.hashCode();
    }

    @Override
    public String toString() {
        return "DefineScope(name=" + scope + ", parent=" + parent + ")";
    }

    public ScopeInfo getParent() {
        return parent;
    }
}
