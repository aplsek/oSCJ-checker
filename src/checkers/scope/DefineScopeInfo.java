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

    public ScopeInfo getParent() {
        return parent;
    }
}
