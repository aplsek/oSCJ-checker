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

    ScopeChecker scopeChecker;
    SchedulableChecker schedulableChecker;
    ScopeRunsInChecker scopeRunsInChecker;
    ScopeTreeChecker scopeTreeChecker;
    DefineScopeChecker defineScopeChecker;

    public SCJChecker() {

        addPass(new SCJRestrictedChecker());
        addPass(new SCJAllowedChecker());

        // Scope checking
        ScopeCheckerContext ctx = new ScopeCheckerContext();
        defineScopeChecker = new DefineScopeChecker(ctx);
        scopeTreeChecker = new ScopeTreeChecker(ctx);
        scopeRunsInChecker = new ScopeRunsInChecker(ctx);
        schedulableChecker = new SchedulableChecker(ctx);
        scopeChecker = new ScopeChecker(ctx);

        addPass(defineScopeChecker);
        addPass(scopeTreeChecker);
        addPass(scopeRunsInChecker);
        addPass(schedulableChecker);
        addPass(scopeChecker);

    }

    @Override
    public final void init(ProcessingEnvironment env) {
        super.init(env);

        Utils.DEBUG = processingEnv.getOptions().containsKey("debug");
        Utils.NO_SCOPE_CHECKS = processingEnv.getOptions().containsKey(
                "noScopeChecks");
        String level = processingEnv.getOptions().get("level");

        if (level != null) {
            if (level.equals("0") || level.equals("1") || level.equals("2")  )
                Utils.setDefaultLevel(Level.getLevel(level));
        }

        if (Utils.NO_SCOPE_CHECKS) {
            removePass(defineScopeChecker);
            removePass(scopeTreeChecker);
            removePass(scopeRunsInChecker);
            removePass(schedulableChecker);
            removePass(scopeChecker);
            System.out.println("WARNING: Scope-Checks DISABLED.");
        }

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
