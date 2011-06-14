package all.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

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
public class Level0Hello extends CyclicExecutive {

    @SCJRestricted(INITIALIZATION)
    public Level0Hello() {
        super(null);
    }

    @Override
    @SCJAllowed(SUPPORT)
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("Level0App")
    public void initialize() {
        new WordHandler(20000);
    }

    @Override
    public long missionMemorySize() {
        return 5000000;
    }

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public void setUp() {
    }

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void tearDown() {
    }

    @SCJAllowed(members=true)
    @Scope("Level0App")
    @DefineScope(name="WordHandler", parent="Level0App")
    class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        @SCJRestricted(INITIALIZATION)
        public WordHandler(long psize) {
            super(null, null, null);
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("WordHandler")
        public void handleAsyncEvent() {
            // printing HelloWorld!!!!
        }

        @Override
        @SCJAllowed(SUPPORT)
        @SCJRestricted(CLEANUP)
        public void cleanUp() {
        }
    }
}
