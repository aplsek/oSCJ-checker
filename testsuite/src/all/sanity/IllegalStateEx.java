package all.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import java.util.Arrays;
import java.util.List;

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
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;


@SCJAllowed(members=true)
@Scope("MyApp")
@DefineScope(name="MyApp", parent=IMMORTAL)
public class IllegalStateEx extends CyclicExecutive {

    @Scope("MyApp")
    @DefineScope(name="MyPEH1", parent="MyApp")
    public ManagedMemory pri;

    @Override
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return new CyclicSchedule(
                new CyclicSchedule.Frame[] { new CyclicSchedule.Frame(new RelativeTime(5, 0),
                        handlers) });
    }

    @SCJRestricted(INITIALIZATION)
    public IllegalStateEx() {
        super(new PriorityParameters(18), new StorageParameters(1000L, 1000L, 1000L));
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    public void initialize() {
        new MyPEH1();
        new MyPEH2();
    }

    /**
     * A method to query the maximum amount of memory needed by this mission.
     *
     * @return the amount of memory needed
     */
    @Override
    public long missionMemorySize() {
        return 1420; // MIN without printing is 430 bytes.
    }

    @SCJRestricted(INITIALIZATION)
    public void setUp() {
    }

    @SCJRestricted(CLEANUP)
    public void tearDown() {
    }

    @Override
    @SCJRestricted(CLEANUP)
    public void cleanUp() {
    }


    @SCJAllowed(members=true)
    @Scope("MyApp")
    @DefineScope(name="MyPEH1", parent="MyApp")
    public static class MyPEH1 extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MyPEH1() {
            super(new PriorityParameters(13),
                    new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                            500, 0)),
                            new StorageParameters(1000L, 1000L, 1000L));
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("MyPEH1")
        public void handleAsyncEvent() {
            IllegalStateEx m1 = (IllegalStateEx) Mission.getCurrentMission();
            m1.pri = (ManagedMemory) MemoryArea.getMemoryArea(new int[0]);
        }

        @Override
        @SCJRestricted(CLEANUP)
        public void cleanUp() {
        }

        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }


    @SCJAllowed(members = true)
    @Scope("MyApp")
    @DefineScope(name="MyPEH2", parent="MyApp")
    public static class MyPEH2 extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MyPEH2() {
            super(new PriorityParameters(13),
                  new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                            500, 0)),
                  new StorageParameters(1000L, 1000L, 1000L));
        }

        @Override
        @RunsIn("MyPEH2")
        public void handleAsyncEvent() {
            try {
                MyRunnable run = new MyRunnable();

                IllegalStateEx m1 = (IllegalStateEx) Mission.getCurrentMission();
                m1.pri.newInstance(List.class);
                m1.pri.enterPrivateMemory(500, run);

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cleanUp() {
        }

        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @SCJAllowed(members = true)
    @DefineScope(name = "new-scope", parent = "MyPEH1")
    public static class MyRunnable implements Runnable {

        @SCJAllowed(SUPPORT)
        @RunsIn("new-scope")
        public void run() {
        }
    }
}
