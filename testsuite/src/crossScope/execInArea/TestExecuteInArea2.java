//crossScope/execInArea/TestExecuteInArea2.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope.execInArea;

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionManager;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import crossScope.getCurrent.TestGetCurrent;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


/**
 * @RunsIn(UNKNOWN) + "executeInArea"
    --> each runnable used for "executeInArea" must be annotated with @RunsIn(UNKNOWN)
 * 
 * 
 */
@Scope("crossScope.execInArea.TestExecuteInArea2") 
public class TestExecuteInArea2 extends Mission  {

    protected
    void initialize() { 
        new Handler(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
    


    @Scope("crossScope.execInArea.TestExecuteInArea2")  
    @RunsIn("crossScope.execInArea.Handler") 
    class Handler extends PeriodicEventHandler {

        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize) {
            super(priority, parameters, scp);
        }

        public
        void handleAsyncEvent() {
        	ImmortalMemory mem = ImmortalMemory.instance();
        	MyRunnable runner = new MyRunnable();
        	mem.executeInArea(runner);						// OK
        	
        	
        	MyRunnableErr runErr = new MyRunnableErr();
        	mem.executeInArea(runErr);						// ERROR - the runnable is not annotated @RunsIn(UNKNOWN)
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }


    @Scope("crossScope.execInArea.Handler")
    @RunsIn("ImmortalMemory")
    class MyRunnable implements Runnable {
    	
    	@Override
    	@RunsIn(UNKNOWN)
    	public void run() {
    		//..
    	}
    
    }

    @Scope("crossScope.execInArea.Handler")
    @RunsIn("ImmortalMemory")
    class MyRunnableErr implements Runnable {
    	
    	@Override
    	public void run() {
    		BigDecimal one = BigDecimal.ONE;
    		one.add(null);							// this is cross-scope but BigDecimal is reference-immutable
    	}
    }
}

