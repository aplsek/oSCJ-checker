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
@Scope(IMMORTAL)
public class ErrorApp extends CyclicExecutive {

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("APP")
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return new CyclicSchedule(new CyclicSchedule.Frame[]{new CyclicSchedule.Frame(null,handlers)});
    }

    @SCJRestricted(INITIALIZATION)
    public ErrorApp() {
        super(null, null);
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("APP")
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
    @SCJAllowed(SUPPORT)
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

        @SCJRestricted(INITIALIZATION)
        public PEH() {
            super(null, null, null);
        }

        List a = new List();

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("PEH")
        public void handleAsyncEvent() {
            List b = new List();

            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            setTail(b, a);

            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            setTail(a, b);

            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
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
