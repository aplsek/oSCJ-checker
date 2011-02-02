//scope/TestScopeTree.java:25: Scope Definitions are not consistent: 
//public class TestScopeTree extends PeriodicEventHandler {
//       ^
//        Scope *TestVariable* is not defined but is parent to other scope.
//1 error

package scope;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

//@DefineScope(name = "TestVariable", parent = IMMORTAL)
class TestVariableClass {}

@Scope("MyHandler")
@DefineScope(name = "MyHandler", parent = "TestVariable")
public class TestScopeTree extends PeriodicEventHandler {

    public TestScopeTree(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp);
    }

    @SCJRestricted(maySelfSuspend=true)
    @RunsIn("MyHandler")
    public void handleAsyncEvent() {

    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }

}
