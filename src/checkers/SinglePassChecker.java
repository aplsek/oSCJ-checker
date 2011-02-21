package checkers;

import checkers.source.Result;
import checkers.source.SourceChecker;

public abstract class SinglePassChecker extends SourceChecker {
    private boolean hasErrors = false;
    public boolean hasErrors() {
        return hasErrors;
    }
    @Override
    public void report(Result r, Object node) {
        hasErrors = hasErrors || r.isFailure();
        super.report(r, node);
    }
}
