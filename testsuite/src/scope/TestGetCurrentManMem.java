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
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name = "scope.TestGetCurrentManMem", parent = IMMORTAL)
public class TestGetCurrentManMem extends CyclicExecutive {

    public TestGetCurrentManMem() {
        super(null);
    }
    
    @SCJAllowed()
    @RunsIn("scope.TestGetCurrentManMem")
    public void initialize() {
        new WordHandler(20000);
    }

    public long missionMemorySize() {
        return 5000000;
    }


    public static void main(final String[] args) {
    }
    
    @SCJAllowed()
    @Scope("scope.TestGetCurrentManMem")
    @DefineScope(name = "scope.TestGetCurrentManMem.WordHandler", parent = "scope.TestGetCurrentManMem")
    public class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        private WordHandler(long psize) {
            super(null, null, null);
        }

        @SCJAllowed()
        @RunsIn("scope.TestGetCurrentManMem.WordHandler")
        public void handleAsyncEvent() {
           
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
            mem.enterPrivateMemory(300, 
                            new MyTestRunnable333());
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

    public CyclicSchedule getSchedule(PeriodicEventHandler[] peh) {
        return null;
    }

}

@SCJAllowed(members=true)
@Scope("scope.TestGetCurrentManMem.WordHandler")
@DefineScope(name="MyTestRunnable_area",
                             parent="scope.TestGetCurrentManMem.WordHandler")
class MyTestRunnable333 implements Runnable {
    @RunsIn("MyTestRunnable_area")
    public void run() {
    }
}