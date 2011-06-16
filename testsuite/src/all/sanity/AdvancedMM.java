package all.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.Clock;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name="APP", parent=IMMORTAL)
public class AdvancedMM extends CyclicExecutive {

    @Override
    @SCJAllowed(SUPPORT)
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return new CyclicSchedule(new CyclicSchedule.Frame[]{new CyclicSchedule.Frame(
                new RelativeTime(5,0),handlers)});
    }

    @SCJRestricted(INITIALIZATION)
    public AdvancedMM() {
        super(new PriorityParameters(18),
                new StorageParameters(1000L, 1000L, 1000L));
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("APP")
    public void initialize() {
        new MyPEH4();
    }

    /**
     * A method to query the maximum amount of memory needed by this
     * mission.
     *
     * @return the amount of memory needed
     */
    @Override
    public long missionMemorySize() {
        return 1420;   // MIN without printing is 430  bytes.
    }

    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public void setUp() {
    }

    @SCJRestricted(CLEANUP)
    @SCJAllowed(SUPPORT)
    public void tearDown() {
    }

    @Override
    @SCJRestricted(CLEANUP)
    @SCJAllowed(SUPPORT)
    public void cleanUp() {
    }


    @SCJAllowed(members=true)
    @Scope("APP")
    @DefineScope(name="PEH", parent="APP")
    public static class MyPEH4 extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MyPEH4() {
            super(new PriorityParameters(13),
                    new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                    500, 0)),
                    new StorageParameters(1000L, 1000L, 1000L));
        }

        Tick tock;

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("PEH")
        public void handleAsyncEvent() {
            try {
                @Scope(IMMORTAL)
                @DefineScope(name="APP", parent=IMMORTAL)
                ManagedMemory m = (ManagedMemory) MemoryArea.getMemoryArea(this);

                @Scope("APP") Tick time = (Tick) m.newInstance(Tick.class);
                MySCJRunnable r = new MySCJRunnable();
                m.executeInArea(r);

            } catch (InstantiationException e) {
                //e.printStackTrace();
            } catch (IllegalAccessException e) {
                //e.printStackTrace();
            }
        }

        @Override
        @SCJRestricted(CLEANUP)
        @SCJAllowed(SUPPORT)
        public void cleanUp() {
        }

        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }

        @SCJAllowed(members=true)
        @DefineScope(name="APP",parent=IMMORTAL)
        class MySCJRunnable implements Runnable {
            @RunsIn("APP")
            public void run() {
                MyPEH4.this.tock = new Tick();
            }
        }
    }

    @SCJAllowed(members=true)
    public static class Tick {
    }
}
