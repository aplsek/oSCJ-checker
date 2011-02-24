package checkers.scope;

public class FieldScopeInfo extends ScopeInfo {
    private final String receiverScope;
    private final String fieldScope;

    public FieldScopeInfo(String scope, String receiverScope,
            String fieldScope) {
        super(scope);
        this.receiverScope = receiverScope;
        this.fieldScope = fieldScope;
    }

    public String getReceiverScope() {
        return receiverScope;
    }

    public String getFieldScope() {
        return fieldScope;
    }

    @Override
    public boolean isFieldScope() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof FieldScopeInfo) {
            FieldScopeInfo o = (FieldScopeInfo) obj;
            return super.equals(obj) && receiverScope == o.receiverScope &&
                    fieldScope == o.receiverScope;
        } else if (obj instanceof ScopeInfo) {
            return super.equals(obj);
        }
        return false;
    }
}
