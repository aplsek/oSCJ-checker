package checkers.scope;

public class ScopeInfo {
    private String scope;

    public ScopeInfo(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public boolean equals(Object obj) {
        return scope.equals(((ScopeInfo) obj).scope);
    }
}
