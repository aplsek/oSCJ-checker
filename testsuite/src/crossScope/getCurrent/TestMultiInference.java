//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope.getCurrent;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import crossScope.TestInference;




@Scope("crossScope.TestNullInference")  
@RunsIn("crossScope.TestInferenceClash") 
class TestMultiInference extends PeriodicEventHandler {

    public TestMultiInference(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp, memSize);
    }

    public
    void handleEvent() {
    	MemoryArea memCurrent = null;
    	
    	memCurrent = RealtimeThread.getCurrentMemoryArea();
    	MemoryArea memUp =  RealtimeThread.getOuterMemoryArea(1);   
    	
    	memCurrent = memUp;				// ERROR
    }


    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}