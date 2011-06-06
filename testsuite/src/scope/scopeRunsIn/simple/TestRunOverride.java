package scope.scopeRunsIn.simple;

import javax.realtime.PriorityParameters;

import javax.safetycritical.ManagedThread;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJAllowed;

import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_NAMED_RUNS_IN_OVERRIDE;
import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RESERVED_RUNS_IN_OVERRIDE;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Level.SUPPORT;



@SCJAllowed(value=LEVEL_2, members=true)
@Scope("D")
@DefineScope(name="D", parent="IMMORTAL")
public abstract class TestRunOverride extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestRunOverride(int priority) {
        super(new PriorityParameters(priority), new StorageParameters(0, 0, 0));
    }

    @Scope("D")
    @DefineScope(name="C", parent="D")
    @SCJAllowed(value=LEVEL_2, members=true)
    class MyThread extends ManagedThread {

        @SCJRestricted(INITIALIZATION)
        public MyThread(int priority) {
            super(new PriorityParameters(priority), new StorageParameters(0, 0, 0));
        }

        @Override
        @RunsIn("C")
        public void run() { }
    }

    class MyRun implements Runnable {
        @RunsIn("C")
        public void run() {}
    }

    class MyRun2 extends MyRun {
        @Override
        @RunsIn("D")
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE
        public void run() {}
    }

    class MyRun3 implements Runnable {
        @Override
        @RunsIn("CALLER")
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RESERVED_RUNS_IN_OVERRIDE
        public void run() {}
    }
}