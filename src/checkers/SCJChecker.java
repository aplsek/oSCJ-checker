package checkers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

import checkers.scjAllowed.SCJAllowedChecker;
import checkers.scjRestricted.SCJRestrictedChecker;
import checkers.scope.AggregateScopeChecker;
import checkers.scope.DefineScopeChecker;
import checkers.scope.ScopeChecker;
import checkers.scope.ScopeCheckerContext;
import checkers.scope.ScopeRunsInChecker;
import checkers.source.SourceChecker;
import checkers.util.AggregateChecker;

public class SCJChecker extends MultiPassChecker {
    public SCJChecker() {
        addPass(new SCJRestrictedChecker());
        addPass(new SCJAllowedChecker());
        // Scope checking
        ScopeCheckerContext ctx = new ScopeCheckerContext();
        addPass(new DefineScopeChecker(ctx));
        addPass(new ScopeRunsInChecker(ctx));
        addPass(new ScopeChecker(ctx));
    }

    @Override
    public final void init(ProcessingEnvironment env) {
        super.init(env);
        Utils.DEBUG = processingEnv.getOptions().containsKey("debug");
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> opts = super.getSupportedOptions();
        opts.add("debug");
        return opts;
    }
}