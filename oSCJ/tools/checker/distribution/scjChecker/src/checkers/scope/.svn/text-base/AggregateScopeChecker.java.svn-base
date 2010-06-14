package checkers.scope;

import checkers.MultiPassChecker;

// Runs both passes for scope checking.
public class AggregateScopeChecker extends MultiPassChecker {
    public AggregateScopeChecker() {
        addPass(new DefineScopeChecker());
        addPass(new ScopeChecker());
    }
}
