package scope.scopeRunsIn.sanity;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.SCJAllowed;


@SCJAllowed(members=true)
@Scope("D")
@DefineScope(name="D", parent=IMMORTAL)
public abstract class TestRunnable extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestRunnable(PriorityParameters priority, StorageParameters storage) {
        super(priority, storage);
    }

    @SCJAllowed(members=true)
    class MyRunnable implements Runnable {

        @RunsIn("D")
        public void run() {}
    }


    @SCJAllowed(members=true)
    @DefineScope(name="E", parent="D")
    class MyRunnable2 implements Runnable {

        @RunsIn("E")
        public void run() {}
    }

}