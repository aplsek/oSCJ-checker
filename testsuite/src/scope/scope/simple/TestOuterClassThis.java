package scope.scope.simple;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

/**
 * This class is the Safelet for unit test. This unit test program is also an example level one
 * SCJ application.
 */
@SCJAllowed(members = true)
@Scope("A")
public class TestOuterClassThis  {

    Y y;

    @SCJAllowed(members = true)
    @Scope("A")
    public class X {

        @RunsIn("A")
        void method () {
            Y y = TestOuterClassThis.this.y;
        }

    }

    @SCJAllowed(members = true)
    public static class Y {
    }


    @Scope(IMMORTAL)
    @DefineScope(name = "A", parent = IMMORTAL)
    @SCJAllowed(members = true)
    static class MS extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public MS(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("A")
        protected Mission getNextMission() {
            return null;
        }
    }
}