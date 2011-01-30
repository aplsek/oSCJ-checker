//scope/TestScopeCheck3.java:112: Cannot assign expression in scope MyTestRunnable to variable in scope scope.TestScopeCheck3.
//        peh.data = mydata;   //// ERROR
//                 ^
//        visitMemberSelect : peh.data
//1 error

package scope;

import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("scope.TestScopeCheck3")
@DefineScope(name="scope.TestScopeCheck3",parent=IMMORTAL)
public class TestScopeCheck3  extends CyclicExecutive  {

    public TestScopeCheck3() {
        super(null);
    }
    
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    public void initialize() {
        new MyWordHandler3(30000);
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



@Scope("scope.TestScopeCheck3")
@DefineScope(name="scope.TestScopeCheck3.MyWordHandler3",parent="scope.TestScopeCheck3")
class MyWordHandler3 extends PeriodicEventHandler {

    public MyWordHandler3(long psize) {
        super(null, null, null);
    }

    public Object data;
    
    @RunsIn("scope.TestScopeCheck3.MyWordHandler3")
    public void handleAsyncEvent() {
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
        mem.enterPrivateMemory(300, 
                     new MyErrorRunnable(this));
    }

    @SCJAllowed()
    public void cleanUp() {}
    
    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}


@Scope("scope.TestScopeCheck3.MyWordHandler3")
@DefineScope(name="MyTestRunnable",parent="scope.TestScopeCheck3.MyWordHandler3")
class MyErrorRunnable implements Runnable {

    MyWordHandler3 peh;
    
    public MyErrorRunnable(MyWordHandler3 p) {
        peh = p;
    }
    
    @Override
    @RunsIn("MyTestRunnable")
    public void run() {
        Object mydata = new Object();
        peh.data = mydata;   //// ERROR
    }
}


