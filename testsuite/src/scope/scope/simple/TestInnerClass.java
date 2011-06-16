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
public class TestInnerClass  {

    @RunsIn(IMMORTAL)
    void method () {

      //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION
        new TestInnerClass.X();  // OK

        new TestInnerClass.Y();  // OK

        //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION
        new TestInnerClass.Z();  // ERROR
    }

    @SCJAllowed(members = true)
    public class X {}

    @SCJAllowed(members = true)
    static class Y {}

    @SCJAllowed(members = true)
    @Scope("A") class Z {}

    @RunsIn("A")
    void foo() {
        new TestInnerClass.X();  // OK

        new TestInnerClass.Y();  // OK

        new TestInnerClass.Z();  // OK
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