//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import static javax.safetycritical.annotate.Scope.IMMORTAL;




@DefineScope(name="crossScope.TestCrossScope", parent=IMMORTAL)
@Scope("crossScope.TestCrossScope") 
public class TestCrossScope extends Mission {

	public Foo foo;
	
    protected
    void initialize() { 
        new MyHandler(null, null, null, 0, this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Scope("crossScope.TestCrossScope")
    @RunsIn(UNKNOWN)
    public Foo getFoo() {
    	return this.foo;
    }


    @DefineScope(name="crossScope.MyHandler", parent="crossScope.TestCrossScope")
    @Scope("crossScope.TestCrossScope")  
    class MyHandler extends PeriodicEventHandler {

    	private TestCrossScope mission;
    	
        public MyHandler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestCrossScope mission) {
            super(priority, parameters, scp);
            
            this.mission = mission;
        }

        @RunsIn("crossScope.MyHandler") 
        public void handleAsyncEvent() {
        	
        	Foo foo = mission.getFoo();		// OK
            List bar = new List();
            
            foo.method(bar);                //  ---> OK
            foo.methodErr(bar);				// ERROR: is not @RunsIn(UNKNOWN)
            
            foo.methodCross();			//  ERROR: foo's methodCross runs in "Mission" so it
             							//   should be annocated with "@RunsIn(UNKNOWN)"
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }



    class Foo {

    	List x;

    	public List methodCross() {				// this should be annotated with @RunsIn(UNKNOWN) to prevent the error abour
    		this.x = new List();
    		return x;
    	}
    	
    	
        @RunsIn(UNKNOWN)
    	public List method(List bar) {
    		return bar;
    	}
    	
    	public List methodErr(List bar) {
    		return null;
    	}
    	
    	
    	@Allocate({THIS})
        public List method2() {
    		return this.x;
    	}	 
    }


    class List {
    	String field;
    }
    
}
