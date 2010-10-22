//scope/TestGetCurrentManMem2.java:57: The variable referencing getCurrentManagedMemory must have a @DefineScope with name that equals to the current allocation context.
//            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
//                          ^
//         Allocation-Context: scope.TestGetCurrentManMem2 
//         DefineScope name: wronge_scope_name
//1 error

package scope;

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
@Scope("scope.TestGetCurrentManMem2")
@RunsIn("scope.TestGetCurrentManMem2")
public class TestGetCurrentManMem2 extends CyclicExecutive {

    public TestGetCurrentManMem2() {
        super(null);
    }
    
    @SCJAllowed()
    public void initialize() {
        new WordHandler(20000);
    }

    // @Override
    public long missionMemorySize() {
        return 5000000;
    }

    @SCJAllowed()
    @Scope("scope.TestGetCurrentManMem2")
    @RunsIn("scope.TestGetCurrentManMem2.WordHandler")
    public class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        private WordHandler(long psize) {
            super(null, null, null, psize);
        }

        @SCJAllowed()
        @RunsIn("scope.TestGetCurrentManMem2.WordHandler")
        public void handleEvent() {
           
            /*@DefineScope(name="wronge_scope_name",    // ERROR: must be same as current alloc. context
                       parent="scope.TestGetCurrentManMem2")*/
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
            
            mem.enterPrivateMemory(300, 
                            new /*@DefineScope(name="MyTestRunnable_area",
                             parent="scope.TestGetCurrentManMem2.WordHandler")*/ 
                                MyTestRunnable33());
        }

        @SCJAllowed()
        public void cleanUp() {}
        
        @SCJAllowed()
        @Override
        public void handleAsyncEvent() {}

        @Override
        public StorageParameters getThreadConfigurationParameters() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    @Override
    public void setUp() {}
    @Override
    public void tearDown() {}

    public CyclicSchedule getSchedule(PeriodicEventHandler[] peh) {
        return null;
    }

}

@SCJAllowed(members=true)
@Scope("scope.TestGetCurrentManMem2.WordHandler")
@RunsIn("MyTestRunnable_area")
class MyTestRunnable33 implements Runnable {
    public void run() {
    }
}