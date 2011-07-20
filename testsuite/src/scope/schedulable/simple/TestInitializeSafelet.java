package scope.schedulable.simple;


import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@SCJAllowed(members=true)
@DefineScope(name="A", parent=IMMORTAL)
public class TestInitializeSafelet implements Safelet {

    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("A")
    //## checkers.scope.SchedulableChecker.ERR_SAFELET_RUNSIN
    public void setUp() {
    }

    @SCJRestricted(CLEANUP)
    @SCJAllowed(SUPPORT)
    @RunsIn("A")
    //## checkers.scope.SchedulableChecker.ERR_SAFELET_RUNSIN
    public void tearDown() {
    }

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    @RunsIn("A")
    //## checkers.scope.SchedulableChecker.ERR_SAFELET_RUNSIN
    public MissionSequencer getSequencer() {
        return null;
    }

    @Override
    @SCJAllowed(SUPPORT)
    public long immortalMemorySize() {
        // TODO Auto-generated method stub
        return 0;
    }

}


