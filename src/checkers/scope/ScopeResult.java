package checkers.scope;

class ScopeResult {
    public final String name;
    public final boolean isError;

    ScopeResult(String name, boolean isError) {
        this.name = name;
        this.isError = isError;
    }
}
