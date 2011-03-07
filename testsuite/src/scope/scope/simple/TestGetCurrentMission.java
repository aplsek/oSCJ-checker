package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name = "MyMission", parent = IMMORTAL)
@Scope("MyMission")
public abstract class TestGetCurrentMission extends Mission {

    @Override
    protected void initialize() {
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Scope("MyMission")
    @DefineScope(name = "MyHandler", parent = "MyMission")
    static abstract class PEH extends Mission {
        @RunsIn("MyHandler")
        public void handleAsyncEvent() {
            @Scope(UNKNOWN)
            Mission mission = Mission.getCurrentMission();

            @Scope("MyMission")
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            Mission mission2 = Mission.getCurrentMission();
        }
    }

    static class MyFoo {
    }

    @Scope("MyHandler")
    static class Bar extends MyFoo {
    }
}
