package all.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import java.util.Arrays;

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
@DefineScope(name="APP", parent=IMMORTAL)
@Scope("APP")
public class ErrorApp extends CyclicExecutive {
    static PriorityParameters p = new PriorityParameters(18);
    static StorageParameters s = new StorageParameters(1000L, 1000L, 1000L);
    static RelativeTime t = new RelativeTime(5,0);

    @Override
    @SCJAllowed(SUPPORT)
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return new CyclicSchedule(new CyclicSchedule.Frame[]{new CyclicSchedule.Frame(t,handlers)});
    }

    @SCJRestricted(INITIALIZATION)
    public ErrorApp() {
        super(p, s);
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public void initialize() {
        new PEH();
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
    public static class PEH extends PeriodicEventHandler {

        static PriorityParameters pri;
        static PeriodicParameters per;
        static StorageParameters stor;

        static {
            pri = new PriorityParameters(13);
            per = new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                    500, 0));
            stor = new StorageParameters(1000L, 1000L, 1000L);
        }

        @SCJRestricted(INITIALIZATION)
        public PEH() {
            super(pri, per, stor);
        }

        List a = new List();

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("PEH")
        public void handleAsyncEvent() {
            List b = new List();
            setTail(b, a);
            setTail(a, b);
            this.a = b;

            Mission.getCurrentMission().requestSequenceTermination();
        }

        @RunsIn("PEH")
        public void setTail(List x, List y) {
            x.tail = y;
        }

        @Override
        @SCJRestricted(CLEANUP)
        @SCJAllowed(SUPPORT)
        public void cleanUp() {
        }

        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }


    @SCJAllowed(members=true)
    public static class List {
        List tail;
    }


}
