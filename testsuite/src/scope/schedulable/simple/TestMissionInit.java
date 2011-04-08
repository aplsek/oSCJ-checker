package scope.schedulable.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


@SCJAllowed(members=true)
@Scope("Level0App")
@DefineScope(name="Level0App", parent=IMMORTAL)
public class TestMissionInit extends CyclicExecutive {

    @SCJRestricted(INITIALIZATION)
    public TestMissionInit() {
        super(null);
    }

    @Override
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    public void initialize() {
        new PEH(null,null,null);
        new PEH(null,null,null);
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
    }


    @Scope("a")
    @DefineScope(name = "b", parent = "a")
    public class PEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public PEH(PriorityParameters priority,
                PeriodicParameters period, StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        @RunsIn("b")
        public void handleAsyncEvent() {
        }
    }


    @Override
    public long missionMemorySize() {
        return 0;
    }

}