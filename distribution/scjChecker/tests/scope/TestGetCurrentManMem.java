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
@Scope("immortal")
public class TestGetCurrentManMem extends CyclicExecutive {

    public TestGetCurrentManMem() {
        super(null);
    }
    
    @SCJAllowed()
    @RunsIn("scope.TestGetCurrentManMem")
    public void initialize() {
        new WordHandler(20000);
    }

    // @Override
    public long missionMemorySize() {
        return 5000000;
    }


    public static void main(final String[] args) {
        Safelet safelet = new TestGetCurrentManMem();
        safelet.setUp();
        //safelet.getSequencer().start();   // TODO: why this is not allowed at level 0????
        safelet.tearDown();
        
    }
    
    @SCJAllowed()
    @Scope("scope.TestGetCurrentManMem")
    @RunsIn("scope.TestGetCurrentManMem.WordHandler")
    public class WordHandler extends PeriodicEventHandler {

        @SCJAllowed()
        private WordHandler(long psize) {
            super(null, null, null, psize);
        }

        @SCJAllowed()
        @RunsIn("scope.TestGetCurrentManMem.WordHandler")
        public void handleEvent() {
           
            @DefineScope(name="scope.TestGetCurrentManMem.WordHandler",  
                       parent="scope.TestGetCurrentManMem")
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
            
            mem.enterPrivateMemory(300, 
                            new /*@DefineScope(name="MyTestRunnable_area",
                             parent="scope.TestGetCurrentManMem.WordHandler")*/ 
                                MyTestRunnable333());
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
@Scope("scope.TestGetCurrentManMem.WordHandler")
@RunsIn("MyTestRunnable_area")
class MyTestRunnable333 implements Runnable {
    
    public void run() {
    }
    
}