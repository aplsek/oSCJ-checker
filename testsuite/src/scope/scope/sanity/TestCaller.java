package scope.scope.sanity;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.realtime.AbsoluteTime;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@Scope("D")
@DefineScope(name = "D", parent = IMMORTAL)
@SCJAllowed(members = true)
public class TestCaller extends Mission {

    AbsoluteTime update_time;
    AbsoluteTime prior_update_time;

    // called periodically by the GPS Driver
    @RunsIn(CALLER)
    synchronized void updatePosition(@Scope(UNKNOWN) AbsoluteTime time_stamp) {
        //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
        update_time.set(time_stamp);

        //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
        update_time.set(0, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Override
    protected void initialize() {
    }
}
