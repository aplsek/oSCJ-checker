// no error here



import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope("Level0App")
@DefineScope(name="Level0App", parent=IMMORTAL)
public class Level0App extends CyclicExecutive {

    @SCJRestricted(INITIALIZATION)
    public Level0App() {
        super(null);
    }

    @Override
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    public void initialize() {
        new WordHandler(20000);
    }

    @Override
    public long missionMemorySize() {
        return 5000000;
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
    }

}
