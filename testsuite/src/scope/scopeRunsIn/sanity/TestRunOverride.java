package scope.scopeRunsIn.sanity;

import javax.realtime.PriorityParameters;

import javax.safetycritical.ManagedThread;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


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
        @SCJAllowed(SUPPORT)
        public void run() { }
    }

    static class MyRun implements Runnable {
        @RunsIn("C")
        public void run() {}
    }
}