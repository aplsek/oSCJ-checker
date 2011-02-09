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
import javax.safetycritical.MissionManager;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="crossScope.getCurrent.TestGetCurrent2", parent=IMMORTAL)
@Scope("crossScope.getCurrent.TestGetCurrent2") 
public class TestGetCurrent2  extends Mission  {

    protected
    void initialize() { 
        new Handler(null, null, null, 0, this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }



    @Scope("crossScope.getCurrent.TestGetCurrent2")  
    @DefineScope(name="crossScope.getCurrent.Handler", parent="crossScope.getCurrent.TestGetCurrent2")
    class Handler extends PeriodicEventHandler {

    	private TestGetCurrent2 mission;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestGetCurrent2 mission) {
            super(priority, parameters, scp);
            
            this.mission = mission;
        }

        @RunsIn("crossScope.getCurrent.Handler") 
        public
        void handleAsyncEvent() {
        
        	
        	// TODO get current
        	
        	
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @Scope("crossScope.getCurrent.Handler")
    class MyMemoryArea extends MemoryArea {
    	
    }


    
}

