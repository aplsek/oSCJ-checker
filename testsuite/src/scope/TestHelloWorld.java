// no error here

package scope;

import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

import javax.safetycritical.annotate.DefineScope;

@SCJAllowed(members=true)
@Scope("scope.TestHelloWorld")
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
    @RunsIn("scope.TestHelloWorld.WordHandler")
    public class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        private WordHandler(long psize) {
            super(null, null, null, psize);
        }

        @SCJAllowed()
        @RunsIn("scope.TestHelloWorld.WordHandler")
        public void handleEvent() {
            // printing HelloWorld!!!!
        }

        @SCJAllowed()
        public void cleanUp() {}
        
        @SCJAllowed()
        @Override
        public void handleAsyncEvent() {}

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