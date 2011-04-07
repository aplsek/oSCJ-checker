// no error here

package scope.schedulable.simple;

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
public class TestHandleAsyncEventRunsIn extends CyclicExecutive {

    @DefineScope(name="Level007", parent=IMMORTAL)
    abstract class Level007 extends CyclicExecutive {

        public Level007(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }}

    @SCJRestricted(INITIALIZATION)
    public TestHandleAsyncEventRunsIn() {
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

    @SCJAllowed(members=true)
    @Scope("Level0App")
    @DefineScope(name="WordHandler", parent="Level0App")
    static class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        @SCJRestricted(INITIALIZATION)
        public WordHandler(long psize) {
            super(null, null, null);
        }

        //## ERROR
        @Override
        @SCJAllowed(SUPPORT)
        //@RunsIn("WordHandler")
        //@RunsIn("Level0App")                // ERROR!!!!!    or   @RunsIn("Level007") is also ERROR!!!
        public void handleAsyncEvent() {
            // printing HelloWorld!!!!
        }

        @Override
        @SCJAllowed()
        public void cleanUp() {
        }
    }
}
