package checkers;

import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.safetycritical.annotate.Level;

import checkers.scjAllowed.SCJAllowedChecker;
import checkers.scjRestricted.SCJRestrictedChecker;
import checkers.scope.DefineScopeChecker;
import checkers.scope.SchedulableChecker;
import checkers.scope.ScopeChecker;
import checkers.scope.ScopeCheckerContext;
import checkers.scope.ScopeRunsInChecker;
import checkers.scope.ScopeTreeChecker;

public class SCJChecker extends MultiPassChecker {
    public SCJChecker() {

        addPass(new SCJRestrictedChecker());
        addPass(new SCJAllowedChecker());


        // TODO: this does not work as it is supposed to work.
        if (Utils.SCOPE_CHECKS) {
            // Scope checking
            ScopeCheckerContext ctx = new ScopeCheckerContext();
            addPass(new DefineScopeChecker(ctx));
            addPass(new ScopeTreeChecker(ctx));
            addPass(new ScopeRunsInChecker(ctx));
            addPass(new SchedulableChecker(ctx));
            addPass(new ScopeChecker(ctx));
        } else
            System.out.println("WARNING: Scope-Checks DISABLED.");

    }

    @Override
    public final void init(ProcessingEnvironment env) {
        super.init(env);

        Utils.DEBUG = processingEnv.getOptions().containsKey("debug");
        Utils.SCOPE_CHECKS = processingEnv.getOptions().containsKey("noScopeChecks");
        String level = processingEnv.getOptions().get("level");

        if (level != null)
            Utils.defaultLevel = Level.getLevel(level);

    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> opts = super.getSupportedOptions();
        opts.add("debug");
        opts.add("noScopeChecks");
        opts.add("level");
        return opts;
    }

}
