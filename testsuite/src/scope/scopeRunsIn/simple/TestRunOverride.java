package scope.scopeRunsIn.simple;

import javax.realtime.PriorityParameters;

import javax.safetycritical.ManagedThread;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Level.SUPPORT;



@SCJAllowed(value=LEVEL_2, members=true)
@Scope(IMMORTAL)
@DefineScope(name="D", parent=IMMORTAL)
public abstract class TestRunOverride extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestRunOverride(int priority) {
        super(new PriorityParameters(priority), new StorageParameters(0, null, 0, 0));
    }

    @Scope("D")
    @DefineScope(name="C", parent="D")
    @SCJAllowed(value=LEVEL_2, members=true)
    static class MyThread extends ManagedThread {

        @SCJRestricted(INITIALIZATION)
        public MyThread(int priority) {
            super(new PriorityParameters(priority), new StorageParameters(0, null, 0, 0));
        }

        @Override
        @RunsIn("C")
        public void run() { }
    }

    static class MyRun implements Runnable {
        @RunsIn("C")
        public void run() {}
    }

    static class MyRun2 extends MyRun {
        @Override
        @RunsIn("D")
        public void run() {}
    }

    static class MyRun3 implements Runnable {
        @Override
        @RunsIn("CALLER")
        public void run() {}
    }
}