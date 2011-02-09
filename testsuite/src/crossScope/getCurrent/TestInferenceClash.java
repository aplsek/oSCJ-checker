//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope.getCurrent;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import crossScope.TestInference;



@DefineScope(name="crossScope.TestNullInference", parent=IMMORTAL)
@Scope("crossScope.TestNullInference")  
class TestInferenceClash extends PeriodicEventHandler {

    public TestInferenceClash(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp);
    }

    @RunsIn("crossScope.TestInferenceClash") 
    public void handleAsyncEvent() {
    	MemoryArea mem = null;
    	
    	mem = RealtimeThread.getCurrentMemoryArea();
    	mem =  RealtimeThread.getOuterMemoryArea(1);   // ERROR
    }


    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}