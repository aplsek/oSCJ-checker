package checkers;

import java.util.Set;
import static javax.safetycritical.annotate.Level.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.safetycritical.annotate.Level;

import checkers.scjAllowed.SCJAllowedChecker;
import checkers.scjRestricted.SCJRestrictedChecker;
import checkers.scope.DefineScopeChecker;
import checkers.scope.SchedulableChecker;
import checkers.scope.ScopeChecker;
import checkers.scope.ScopeCheckerContext;
import checkers.scope.ScopeRunsInChecker;

public class SCJChecker extends MultiPassChecker {
    public SCJChecker() {
        addPass(new SCJRestrictedChecker());
        addPass(new SCJAllowedChecker());
        // Scope checking
        ScopeCheckerContext ctx = new ScopeCheckerContext();
        addPass(new DefineScopeChecker(ctx));
        addPass(new ScopeRunsInChecker(ctx));
        addPass(new SchedulableChecker(ctx));
        addPass(new ScopeChecker(ctx));
    }

    @Override
    public final void init(ProcessingEnvironment env) {
        super.init(env);
        Utils.DEBUG = processingEnv.getOptions().containsKey("debug");
        String level = processingEnv.getOptions().get("level");
        Utils.level = Level.getLevel(level);
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> opts = super.getSupportedOptions();
        opts.add("debug");
        opts.add("level");
        return opts;
    }

}
