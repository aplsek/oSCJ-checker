//scope/TestEnterPrivateMemory2.java:95: (Class scope.MyTestRunnable2 has a scope annotation with no matching @DefineScope)
//class MyTestRunnable2 implements Runnable {
//^
//1 error

package scope;

import static javax.safetycritical.annotate.Scope.UNKNOWN;
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
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name="scope.TestEnterPrivateMemory2", parent=IMMORTAL)
public class TestEnterPrivateMemory2 extends CyclicExecutive {
  
    public TestEnterPrivateMemory2() {
        super(null);
    }
    
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    @SCJAllowed()
    @RunsIn("scope.TestEnterPrivateMemory2")
    public void initialize() {
        new WordHandler(20000);
    }

    public long missionMemorySize() {
        return 5000000;
    }
    
    @SCJAllowed()
    @Scope("scope.TestEnterPrivateMemory2")
    @DefineScope(name="scope.TestEnterPrivateMemory2.WordHandler", parent="scope.TestEnterPrivateMemory2")
    public class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        private WordHandler(long psize) {
            super(null, null, null);
        }

        @SCJAllowed()
        @RunsIn("scope.TestEnterPrivateMemory2.WordHandler")
        public void handleAsyncEvent() {
            
            ManagedMemory.
                getCurrentManagedMemory().enterPrivateMemory(300, 
                            new 
                            MyTestRunnable2());
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

@SCJAllowed(members=true)
@Scope("scope.TestEnterPrivateMemory2.WordHandler")
@DefineScope(name="error_name", parent="scope.TestEnterPrivateMemory2.WordHandler")
class MyTestRunnable2 implements Runnable {
    @RunsIn("handler")
    public void run() {
    }
}