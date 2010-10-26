//crossScope_A/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope_A;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;


import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


@Scope("crossScope_A.CrossScope") 
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

    @Allocate({THIS})
    public Foo getFoo() {
    	return this.foo;
    }


    @Scope("crossScope_A.MyMission")  
    @RunsIn("crossScope_A.MyHandler") 
    class MyHandler extends PeriodicEventHandler {

    	private TestCrossScope mission;
    	
        public MyHandler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestCrossScope mission) {
            super(priority, parameters, scp, memSize);
            
            this.mission = mission;
        }

        public void handleEvent() {
            Foo foo = mission.getFoo();
            List bar = new List();
            
            foo.method(bar);                //  ---> OK
            foo.methodErr(bar);				// ERROR: is not @crossScope
           
            
            foo.methodCross();			//  ERROR: foo's methodCross runs in "Mission" so it
             							//   should be annocated with "@crossScope"
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }



    class Foo {

    	List x;

    	public List methodCross() {				// this should be annotated with @crossScope to prevent the error abour
    		this.x = new List();
    		return x;
    	}
    	
    	
    	@Allocate({CURRENT})
        @CrossScope
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
