// no error here

package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope("Level0App")
@DefineScope(name="Level0App", parent=IMMORTAL)
public class Level0App extends CyclicExecutive {

    @DefineScope(name="Level007", parent=IMMORTAL)
    abstract class Level007 extends CyclicExecutive {

        public Level007(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }}

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

    public static void main(final String[] args) {
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
    }

    @SCJAllowed(members=true)
    @Scope("Level0App")
    @DefineScope(name="WordHandler", parent="Level0App")
    static class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        @SCJRestricted(INITIALIZATION)
        public WordHandler(long psize) {
            super(null, null, null);
        }

        @Override
        @SCJAllowed(SUPPORT)
        //@RunsIn("WordHandler")
        @RunsIn("Level0App")                // ERROR!!!!!    or   @RunsIn("Level007")
        public void handleAsyncEvent() {
            // printing HelloWorld!!!!
        }

        @Override
        @SCJAllowed()
        public void cleanUp() {
        }
    }


}
