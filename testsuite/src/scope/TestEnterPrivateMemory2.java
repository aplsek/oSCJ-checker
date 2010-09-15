//scope/TestEnterPrivateMemory2.java:95: (Class scope.MyTestRunnable2 has a scope annotation with no matching @DefineScope)
//class MyTestRunnable2 implements Runnable {
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
@Scope("immortal")
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
    @RunsIn("scope.TestEnterPrivateMemory2.WordHandler")
    public class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        private WordHandler(long psize) {
            super(null, null, null, psize);
        }

        @SCJAllowed()
        @RunsIn("scope.TestEnterPrivateMemory2.WordHandler")
        public void handleEvent() {
            
            ManagedMemory.
                getCurrentManagedMemory().enterPrivateMemory(300, 
                            new /*@DefineScope(name="error_name", parent="scope.TestEnterPrivateMemory2.WordHandler")*/ 
                            MyTestRunnable2());
        }

        @SCJAllowed()
        public void cleanUp() {}
        
        @SCJAllowed()
        public void register() {}
        
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

@SCJAllowed(members=true)
@Scope("scope.TestEnterPrivateMemory2.WordHandler")
@RunsIn("handler")
class MyTestRunnable2 implements Runnable {
    public void run() {
    }
}