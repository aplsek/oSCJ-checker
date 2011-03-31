package scope.scope.simple;

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
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestBadOverride {

    @Scope("a")
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends Mission { }

    @Scope("a")
    @DefineScope(name="PEH", parent="a")
    static abstract class PEH extends PeriodicEventHandler {

        public PEH(PriorityParameters priority, PeriodicParameters period,
                StorageParameters storage) {
            super(priority, period, storage);
        }

        PEHImplementation peh;

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("PEH")
        public void handleAsyncEvent () {
            peh.run();
        }

    }

    public class PEHImplementation implements MySCJRunnable {
        //## ERROR: the checker should require restating the @RunsIn
        public void run() {
        }
    }
}


interface MySCJRunnable extends SCJRunnable {

    @SCJAllowed(SUPPORT)
    @RunsIn("PEH")
    public void run();
}
