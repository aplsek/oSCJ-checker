//scope/TestScopeCheck.java:106: Cannot assign expression in scope MyTestRunnable to variable in scope scope.TestScopeCheck.MyWordHandler.
//            mydata = new Object();
//                   ^
//                     visitNewClass
//                    visitAssignment : data = mydata
//scope/TestScopeCheck.java:107: Cannot assign expression in scope scope.TestScopeCheck.MyWordHandler to variable in scope scope.TestScopeCheck.
//            data = mydata;   //// ERROR
//                 ^
//2 errors

package scope;

import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("scope.TestScopeCheck")
@DefineScope(name="scope.TestScopeCheck",parent=IMMORTAL)
public class TestScopeCheck  extends CyclicExecutive  {

    public TestScopeCheck() {
        super(null);
    }
    
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    public void initialize() {
        new MyWordHandler(20000);
    }

    // @Override
    public long missionMemorySize() {
        return 5000000;
    }

    @Override
    public void setUp() {}
    @Override
    public void tearDown() {}

}


@Scope("scope.TestScopeCheck")
@DefineScope(name="scope.TestScopeCheck.MyWordHandler", parent="scope.TestScopeCheck")
class MyWordHandler extends PeriodicEventHandler {

    public MyWordHandler(long psize) {
        super(null, null, null);
    }

    public Object data;
    
    @RunsIn("scope.TestScopeCheck.MyWordHandler")
    public void handleAsyncEvent() {
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
        mem.enterPrivateMemory(300, 
                     new MyErrorRunnable());
    }

    @SCJAllowed()
    public void cleanUp() {}
    
    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }

    @Scope("scope.TestScopeCheck.MyWordHandler")
    @DefineScope(name="MyTestRunnable",parent="scope.TestScopeCheck.MyWordHandler")
    class MyErrorRunnable implements Runnable {

        Object mydata;
        
        public MyErrorRunnable() {
        }
        
        @Override
        @RunsIn("MyTestRunnable")
        public void run() {
            mydata = new Object();     
            data = mydata;   //// ERROR
        }
    }
}


