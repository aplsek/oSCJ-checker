//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope_A.getCurrent;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionManager;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@Scope("crossScope_A.getCurrent.TestInference") 
public class TestGetCurrent  extends Mission  {

    protected
    void initialize() { 
        new Handler(null, null, null, 0, this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }



    @Scope("crossScope_A.getCurrent.MyMission")  
    @RunsIn("crossScope_A.getCurrent.Handler") 
    class Handler extends PeriodicEventHandler {

    	private TestGetCurrent mission;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestGetCurrent mission) {
            super(priority, parameters, scp, memSize);
            
            this.mission = mission;
        }

        public
        void handleEvent() {
        	Mission mission = Mission.getCurrentMission();
        	
        	MissionManager mm = MissionManager.getCurrentMissionManager();
        	
        	RealtimeThread rt = RealtimeThread.currentRealtimeThread();
        	
        	
        	MemoryArea memory = RealtimeThread.getCurrentMemoryArea();
        	MemoryArea outerMemory = RealtimeThread.getOuterMemoryArea(1);
        	MemoryArea errMemory = RealtimeThread.getOuterMemoryArea(150);    // ERROR when inferring the scope of returned object
        	
        	
        	MyMemoryArea mem = (MyMemoryArea) RealtimeThread.getCurrentMemoryArea();  // ERROR - the cast?
        	
        	
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @Scope("crossScope_A.getCurrent.Handler")
    class MyMemoryArea extends MemoryArea {
    	
    }


    
}

