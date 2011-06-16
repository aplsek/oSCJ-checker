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

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@SCJAllowed(value = LEVEL_2, members = true)
@Scope(IMMORTAL)
@DefineScope(name = "D", parent = IMMORTAL)
public abstract class TestRunOverride2 extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestRunOverride2(int priority) {
        super(new PriorityParameters(priority), new StorageParameters(0, 0, 0));
    }

    @Scope("D")
    @DefineScope(name = "C", parent = "D")
    @SCJAllowed(value = LEVEL_2, members = true)
    static class MyThread extends ManagedThread {

        @SCJRestricted(INITIALIZATION)
        public MyThread(int priority) {
            super(new PriorityParameters(priority), new StorageParameters(0, 0,
                    0));
        }

        @Override
        @RunsIn("C")
        public void run() {
        }
    }

    @Scope("D")
    @DefineScope(name = "E", parent = "D")
    @SCJAllowed(value = LEVEL_2, members = true)
    static class MyThread2 extends MyThread {

        @SCJRestricted(INITIALIZATION)
        public MyThread2(int priority) {
            super(priority);
        }

        @Override
        @RunsIn("E")
        public void run() {
        }
    }

    @Scope("D")
    @DefineScope(name = "F", parent = "D")
    @SCJAllowed(value = LEVEL_2, members = true)
    static class MyThread3 extends ManagedThread {

        @SCJRestricted(INITIALIZATION)
        public MyThread3(int priority) {
            super(new PriorityParameters(priority), new StorageParameters(0, 0,
                    0));
        }

        @Override
        @RunsIn("F")
        public void run() {
        }
    }

}