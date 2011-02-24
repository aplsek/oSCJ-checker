package checkers.scope;

import javax.safetycritical.annotate.Scope;

public class ScopeInfo {
    private final String scope;

    public ScopeInfo(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public boolean isCurrent() {
        return Scope.CURRENT.equals(scope);
    }

    public boolean isUnknown() {
        return Scope.UNKNOWN.equals(scope);
    }

    public boolean isFieldScope() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && scope.equals(((ScopeInfo) obj).scope);
    }
}
