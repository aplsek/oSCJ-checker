//        mem.enterPrivateMemory(1000, new 
//crossScope/execInArea/TestExecuteInArea2.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//                                     ^
//1 error

package crossScope.execInArea;



import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.realtime.ImmortalMemory;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

/**
 * 
 * 
 */
@Scope("crossScope.execInArea.TestExecuteInArea3") 
@DefineScope(name="crossScope.execInArea.TestExecuteInArea3",parent="IMMORTAL")
public class TestExecuteInArea3 extends Mission  {

    protected
    void initialize() { 
        new Handler(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Scope("crossScope.execInArea.TestExecuteInArea3")  
    @DefineScope(name="crossScope.execInArea.Handler",parent="crossScope.execInArea.TestExecuteInArea3")  
    class Handler extends PeriodicEventHandler {

        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize) {
            super(priority, parameters, scp);
        }

        @RunsIn("crossScope.execInArea.Handler") 
        public void handleAsyncEvent() {
        	ImmortalMemory mem = ImmortalMemory.instance();
        	MyRunnable runner = new MyRunnable();
        	mem.executeInArea(runner);						// OK
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @Scope("crossScope.execInArea.Handler")
    class MyRunnable implements Runnable {
    	
    	@Override
    	@RunsIn(IMMORTAL)
    	public void run() {
    		Mission mission = Mission.getCurrentMission();      /// ERROR
    		Foo f = new Foo();									// 
    		f.method();											// OK 
    		
    		
    		Bar b = new Bar();
    		b.method();											// ERROR
    		b.methodErr();										// OK
    	
    	
    		f.method(b);										// ERROR : method is not @RunsIn(UNKNOWN)
    	}
    }
    
    class Foo {
    	Bar b ;
    	
    	public void method() {									// OK, does not have to be @RunsIn(UNKNOWN)
    		Mission mission = Mission.getCurrentMission();			// ERROR since Foo is not annotated with @Scope
    	}
    	
    	public void method(Bar b) {									// ERROR must be cross-scope
    		//..
    	}
    	
    	@RunsIn(UNKNOWN)
    	public void method2(Bar b) {									// OK
    		myMethod();
    	}
    	
    	private void myMethod() {
    		this.b = new Bar();
    	}
    }
    
    @Scope(IMMORTAL)
    class Bar {
    	public void method() {
    		Mission mission = Mission.getCurrentMission();			// OK
    	}
    	
    	@RunsIn(UNKNOWN)
    	public void methodErr() {
    		Mission mission = Mission.getCurrentMission();			// ERROR
    	}
    }
    
}

