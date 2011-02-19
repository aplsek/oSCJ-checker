package checkers.scope;

import checkers.MultiPassChecker;

// Runs both passes for scope checking.
public class AggregateScopeChecker extends MultiPassChecker {
    public AggregateScopeChecker() {
        ScopeCheckerContext ctx = new ScopeCheckerContext();
        addPass(new DefineScopeChecker(ctx));
        addPass(new ScopeRunsInChecker(ctx));
        addPass(new ScopeChecker(ctx));
    }
}
