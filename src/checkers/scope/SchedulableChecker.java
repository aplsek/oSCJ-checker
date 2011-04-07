package checkers.scope;


import java.util.Properties;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;

public class SchedulableChecker extends SinglePassChecker {
    public static final String ERR_SCHEDULABLE_NO_SCOPE = "schedulable.no.scope";
    public static final String ERR_SCHEDULABLE_SCOPE_DEFINESCOPE_MISMATCH = "schedulable.scope.mismatch";
    public static final String ERR_SCHEDULABLE_NO_RUNS_IN = "schedulable.no.runsIn";
    public static final String ERR_SCHEDULABLE_RUNS_IN_MISMATCH = "schedulable.runsIn.mismatch";

    private ScopeCheckerContext ctx;

    public SchedulableChecker(ScopeCheckerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new SchedulableVisitor(this, root, ctx);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        p.put(ERR_SCHEDULABLE_NO_SCOPE, "Class that implements Schedulable must have a @Scope annotation.");
        p.put(ERR_SCHEDULABLE_SCOPE_DEFINESCOPE_MISMATCH, "The @Scope of a Schedulable must match with the parent value of Schedulable's @DefineScope.");
        p.put(ERR_SCHEDULABLE_NO_RUNS_IN, "Class that implements Schedulable must have a @RunsIn annotation on the according method.");
        p.put(ERR_SCHEDULABLE_RUNS_IN_MISMATCH, "The @RunsIn annotation of the schedulable's method (@RusnIn(%s)) is not in accordance with the @DefineScope annotation (@DefineScope(name=%s,...)).");


        return p;
    }
}
