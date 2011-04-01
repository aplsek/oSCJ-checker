package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestBadOverride {

    @Scope("a")
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends Mission { }

    @Scope("a")
    @DefineScope(name="PEH", parent="a")
    static abstract class PEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public PEH(PriorityParameters priority, PeriodicParameters period,
                StorageParameters storage) {
            super(priority, period, storage);
        }

        PEHImplementation peh;

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("PEH")
        public void handleAsyncEvent () {
            //peh.run();
        }

    }

    public class PEHImplementation implements MySCJRunnable {
        //ERROR: the checker should require restating the @RunsIn
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE_RESTATE
        public void run() {
        }
    }
}


interface MySCJRunnable extends SCJRunnable {

    @SCJAllowed(SUPPORT)
    @RunsIn("PEH")
    public void run();
}
