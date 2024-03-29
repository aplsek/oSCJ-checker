package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name="Level0App", parent=IMMORTAL)
public class TestMemberSelectClass extends CyclicExecutive {

    @SCJRestricted(INITIALIZATION)
    public TestMemberSelectClass() {
        super(null);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("Level0App")
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return new CyclicSchedule(
                          new CyclicSchedule.Frame[] { new CyclicSchedule.Frame(new RelativeTime(200, 0),
                                  handlers) });
    }

    void method() {

    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("Level0App")
    public void initialize() {
    }

    @Override
    @SCJAllowed(SUPPORT)
    public long missionMemorySize() {
        return 5000000;
    }

}
