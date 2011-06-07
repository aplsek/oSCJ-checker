package scope.schedulable.simple;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name = "A", parent = IMMORTAL)
public class TestMissionSequencerRunsIn extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestMissionSequencerRunsIn(PriorityParameters priority,
            StorageParameters storage) {
        super(priority, storage);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("B")
    //## checkers.scope.SchedulableChecker.ERR_MISSION_SEQUENCER_RUNS_IN
    protected Mission getNextMission() {
        return null;
    }

    @Scope("A")
    @DefineScope(name = "B", parent = "A")
    @SCJAllowed(members=true)
    static abstract class PEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public PEH(PriorityParameters priority,
                PeriodicParameters period, StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("B")
        public void handleAsyncEvent() {
        }
    }
}