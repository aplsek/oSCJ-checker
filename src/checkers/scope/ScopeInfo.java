package checkers.scope;

import javax.safetycritical.annotate.Scope;

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

    public boolean isCurrent() {
        return Scope.CURRENT.equals(scope);
    }

    public boolean isUnknown() {
        return Scope.UNKNOWN.equals(scope);
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && scope.equals(((ScopeInfo) obj).scope);
    }
}
