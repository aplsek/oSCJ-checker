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
    public static final String ERR_SCHED_INIT_OUT_OF_INIT_METH = "schedulable.ctor.out.of.init.method" ;

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
        p.put(ERR_SCHEDULABLE_NO_SCOPE, "Missing @Scope: Class that implements Schedulable must have a @Scope annotation.");
        p.put(ERR_SCHEDULABLE_SCOPE_DEFINESCOPE_MISMATCH, "Illegal @Scope annotation: The @Scope annotation must refer to the parent-scope in the @DefineScope annotation of this Schedulable.");
        p.put(ERR_SCHEDULABLE_RUNS_IN_MISMATCH, "Bad @RunsIn: The @RunsIn annotation of the schedulable's method (@RusnIn(%s)) does not correspond to the @DefineScope annotation (@DefineScope(name=%s,...)).");
        p.put(ERR_SCHED_INIT_OUT_OF_INIT_METH, "A Schedulable object may be instantiated only in an initialization method of a Mission object.");
        return p;
    }
}
