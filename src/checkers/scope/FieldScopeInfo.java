package checkers.scope;

public class FieldScopeInfo extends ScopeInfo {
    private final ScopeInfo receiverScope;
    private final ScopeInfo fieldScope;

    public FieldScopeInfo(ScopeInfo receiverScope, ScopeInfo fieldScope) {
        this(calculateScope(receiverScope, fieldScope), receiverScope,
                fieldScope, null);
    }

    public FieldScopeInfo(ScopeInfo receiverScope, ScopeInfo fieldScope,
            DefineScopeInfo defineScopeInfo) {
        this(calculateScope(receiverScope, fieldScope), receiverScope,
                fieldScope, defineScopeInfo);
    }

    public FieldScopeInfo(ScopeInfo scope, ScopeInfo receiverScope,
            ScopeInfo fieldScope, DefineScopeInfo defineScopeInfo) {
        super(scope.getScope(), defineScopeInfo);
        this.receiverScope = receiverScope;
        this.fieldScope = fieldScope;
    }

    public ScopeInfo getReceiverScope() {
        return receiverScope;
    }

    public ScopeInfo getFieldScope() {
        return fieldScope;
    }

    @Override
    public boolean isFieldScope() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        else if (obj instanceof FieldScopeInfo) {
            FieldScopeInfo o = (FieldScopeInfo) obj;
            return super.equals(obj) && receiverScope.equals(o.receiverScope)
                    && fieldScope.equals(o.receiverScope);
        } else if (obj instanceof ScopeInfo)
            return super.equals(obj);
        return false;
    }

    static ScopeInfo calculateScope(ScopeInfo receiverScope,
            ScopeInfo fieldScope) {
        if (fieldScope.isCurrent())
            return receiverScope;
        else
            return fieldScope;
    }
}
