//tests/scope/TestEnterPrivateMemory.java:93: (Class scope.MyTestRunnable has a scope annotation with no matching @DefineScope)
//class MyTestRunnable implements Runnable {
//^
//1 error

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
@Scope("scope.TestEnterPrivateMemory")
public class TestEnterPrivateMemory extends CyclicExecutive {
  
    public TestEnterPrivateMemory() {
        super(null);
    }
    
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    @SCJAllowed()
    public void initialize() {
        new WordHandler(20000);
    }

    public long missionMemorySize() {
        return 5000000;
    }

    public static void main(final String[] args) {
    }
    
    @SCJAllowed()
    @Scope("scope.TestEnterPrivateMemory")
    @RunsIn("scope.TestEnterPrivateMemory.WordHandler")
    public class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        private WordHandler(long psize) {
            super(null, null, null, psize);
        }

        @SCJAllowed()
        @RunsIn("scope.TestEnterPrivateMemory.WordHandler")
        public void handleEvent() {
            ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(300, 
                            new /*@DefineScope(name="handler22",   // FAIL!!
                             parent="scope.TestEnterPrivateMemory.WordHandler")*/ 
                                MyTestRunnable());
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

    public void setUp() {}
    public void tearDown() {}
}

@SCJAllowed(members=true)
@Scope("scope.TestEnterPrivateMemory.WordHandler")
@RunsIn("handler_child")
class MyTestRunnable implements Runnable {
    public void run() {
    }
}