package checkers.scope;

public class FieldScopeInfo extends ScopeInfo {
    private final ScopeInfo receiverScope;
    private final ScopeInfo fieldScope;

    public FieldScopeInfo(ScopeInfo receiverScope, ScopeInfo fieldScope) {
        this(calculateScope(receiverScope, fieldScope), receiverScope,
                fieldScope, fieldScope.getRepresentedScope());
    }

    public FieldScopeInfo(ScopeInfo receiverScope, ScopeInfo fieldScope,
            ScopeInfo represented) {
        this(calculateScope(receiverScope, fieldScope), receiverScope,
                fieldScope, represented);
    }

    private FieldScopeInfo(ScopeInfo scope, ScopeInfo receiverScope,
            ScopeInfo fieldScope, ScopeInfo represented) {
        super(scope.getScope(), represented);
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
            if (receiverScope == null) {        // this branch is added to avoid failing on ScopeInfo.IMMORTAL
                if (o.receiverScope == null)
                    return super.equals(obj) && fieldScope.equals(o.receiverScope);
            } else {
                return super.equals(obj) && receiverScope.equals(o.receiverScope)
                        && fieldScope.equals(o.fieldScope);
            }
        } else if (obj instanceof ScopeInfo)
            return super.equals(obj);
        return false;
    }

    static ScopeInfo calculateScope(ScopeInfo receiverScope,
            ScopeInfo fieldScope) {
        if (fieldScope.isThis())
            return receiverScope;
        else
            return fieldScope;
    }

    @Override
    public FieldScopeInfo representing(ScopeInfo represented) {
        return new FieldScopeInfo(receiverScope, fieldScope, represented);
    }

    @Override
    public void dump() {
        super.dump();
        System.out.println("\t\t receiverScope:" + receiverScope);
        System.out.println("\t\t fieldScope:" + fieldScope);
    }
}
