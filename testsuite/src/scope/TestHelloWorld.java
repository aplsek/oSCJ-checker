// no error here

package scope;

import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.DefineScope;

@SCJAllowed(members=true)
@Scope("scope.TestHelloWorld")
@DefineScope(name="scope.TestHelloWorld",parent=IMMORTAL)
public class TestHelloWorld extends CyclicExecutive {
  
    public TestHelloWorld() {
        super(null);
    }
    
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    public void initialize() {
        new WordHandler(20000);
    }

    // @Override
    public long missionMemorySize() {
        return 5000000;
    }


    public static void main(final String[] args) {
    }
    
    @SCJAllowed()
    @Scope("scope.TestHelloWorld")
    @DefineScope(name="scope.TestHelloWorld.WordHandler",parent="scope.TestHelloWorld")
    public class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        private WordHandler(long psize) {
            super(null, null, null);
        }

        @SCJAllowed()
        @RunsIn("scope.TestHelloWorld.WordHandler")
        public void handleAsyncEvent() {
            // printing HelloWorld!!!!
        }

        @SCJAllowed()
        public void cleanUp() {}
        
        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @Override
    public void setUp() {}
    @Override
    public void tearDown() {}

}
