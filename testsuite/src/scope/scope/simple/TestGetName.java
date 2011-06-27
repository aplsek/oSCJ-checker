package scope.scope.simple;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;




public class TestGetName {


    @Scope(IMMORTAL)
    @DefineScope(name = "D", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class TestRunnable extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public TestRunnable(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @Scope("D")
    @DefineScope(name = "E", parent = "D")
    @SCJAllowed(value = LEVEL_2, members = true)
    static class PEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public PEH(PriorityParameters priority, PeriodicParameters period,
                StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        @RunsIn( "E")
        public void handleAsyncEvent() {
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            String str = getName();

            new String(getName());
        }

    }

}
