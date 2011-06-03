package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;


import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.ManagedThread;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;


@SCJAllowed(members=true)
@Scope("D")
@DefineScope(name="D", parent=IMMORTAL)
public abstract class TestRunnable2 extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestRunnable2(PriorityParameters priority, StorageParameters storage) {
        super(priority, storage);
    }

    @SCJAllowed(members=true)
    class MyRun implements Runnable {
        @RunsIn("D")
        public void run() {}
    }

    @SCJAllowed(members=true)
    class MyRunnable implements Runnable {
        public void run() {}
    }

    @SCJAllowed(members=true)
    class MyExtendedRunnable extends MyRunnable {
        @Override
        @RunsIn("D")
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE
        public void run() {}
    }

/*
    @SCJAllowed(members=true)
    class MyRunnable2 implements Runnable {
        @RunsIn(CALLER)
        public void run() {}
    }
    /*/
}
