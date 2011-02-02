//scope/TestEnterPrivateMemory3.java:61: Cannot assign expression in scope ... to variable in scope .... 
//                  Runnable run = (Runnable) myRunnable;
//                               ^
//scope/TestEnterPrivateMemory3.java:58: The Runnable passed into the enterPrivateMemory() call must have a run() method with a @RunsIn annotation.
//                getCurrentManagedMemory().enterPrivateMemory(300, 
//                                                            ^
//2 errors

package scope;

import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name="scope.TestEnterPrivateMemory2", parent=IMMORTAL)
public class TestEnterPrivateMemory3 extends CyclicExecutive {
  
    public TestEnterPrivateMemory3() {
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
            
            MyTestRunnable3 myRunnable = new MyTestRunnable3();
            Runnable run = (Runnable) myRunnable; 
            
            ManagedMemory.
                getCurrentManagedMemory().enterPrivateMemory(300, 
                        run);
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
@DefineScope(name="runnable", parent="scope.TestEnterPrivateMemory2.WordHandler")
class MyTestRunnable3 implements Runnable {
    @RunsIn("runnable")
    public void run() {
    }
}