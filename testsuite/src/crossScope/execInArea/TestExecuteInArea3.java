//        mem.enterPrivateMemory(1000, new 
//crossScope_A/execInArea/TestExecuteInArea2.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
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
import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

/**
 * @CrossScope + "executeInArea"
    --> each runnable used for "executeInArea" must be annotated with @CrossScope
 * 
 * 
 */
@Scope("crossScope_A.execInArea.TestExecuteInArea3") 
public class TestExecuteInArea3 extends Mission  {

    protected
    void initialize() { 
        new Handler(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Scope("crossScope_A.execInArea.TestExecuteInArea3")  
    @RunsIn("crossScope_A.execInArea.Handler") 
    class Handler extends PeriodicEventHandler {

        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize) {
            super(priority, parameters, scp, memSize);
        }

        public
        void handleEvent() {
        	ImmortalMemory mem = ImmortalMemory.instance();
        	MyRunnable runner = new MyRunnable();
        	mem.executeInArea(runner);						// OK
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @Scope("crossScope_A.execInArea.Handler")
    @RunsIn("Immortal")
    class MyRunnable implements Runnable {
    	
    	@Override
    	@CrossScope
    	public void run() {
    		Mission mission = Mission.getCurrentMission();      /// ERROR
    		Foo f = new Foo();									// 
    		f.method();											// OK 
    		
    		
    		Bar b = new Bar();
    		b.method();											// ERROR
    		b.methodErr();										// OK
    	
    	
    		f.method(b);										// ERROR : method is not @crossScope
    	}
    }
    
    class Foo {
    	Bar b ;
    	
    	public void method() {									// OK, does not have to be @CrossScope
    		Mission mission = Mission.getCurrentMission();			// ERROR since Foo is not annotated with @Scope
    	}
    	
    	public void method(Bar b) {									// ERROR must be cross-scope
    		//..
    	}
    	
    	@CrossScope
    	public void method2(Bar b) {									// OK
    		myMethod();
    	}
    	
    	private void myMethod() {
    		this.b = new Bar();
    	}
    }
    
    @Scope("Immortal")
    class Bar {
    	public void method() {
    		Mission mission = Mission.getCurrentMission();			// OK
    	}
    	
    	@CrossScope
    	public void methodErr() {
    		Mission mission = Mission.getCurrentMission();			// ERROR
    	}
    }
    
}

