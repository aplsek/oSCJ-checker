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


import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


@Scope("crossScope.TestCrossScope2") 
public class TestCrossScope2 extends Mission {

	public Foo foo;
	
    protected
    void initialize() { 
        new MyHandler(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    public void method() {
    	System.out.println("This is NOT cross-scope");
    }
    
    @CrossScope
    public void methodCS() {
    	System.out.println("This is cross-scope");
    }
    
    @CrossScope
    public Foo getFoo() {
    	return this.foo;							// ERROR!!! this must be checked!
    }
    
    @CrossScope
    public Foo getFooCopy() {
    	
    	Foo foo = new Foo();
    	foo.id = this.foo.id;										// DEEP-COPY of all the arguments
    	
    	return foo;						// the return object must be a copy of Foo;
    }
    
    public Bar getBar() {
    	return new Bar();
    }

    @Scope("crossScope.TestCrossScope2")  
    @RunsIn("crossScope.MyHandler") 
    class MyHandler extends PeriodicEventHandler {

        public MyHandler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize) {
            super(priority, parameters, scp, memSize);
        }

        public void handleEvent() {
        	
        	Mission mission = Mission.getCurrentMission();				// OK, scope type is inferred
        	TestCrossScope2 myMission = (TestCrossScope2) mission;		// OK, both are of the same type
        	
        	myMission.method();											// ERROR
        	myMission.methodCS();										// OK
        	
        	
        	Foo foo = myMission.getFoo();								// ERROR ???
        	Foo foo2 = myMission.getFooCopy();						 	// OK, its @CrossScope
        	
        	
        	Bar bar = myMission.getBar();								// OK
        	bar.method();												// OK - because bar.method() is @CrossScope
        	
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    class Foo {
    	int id;
    }
    
    @Scope("crossScope.TestCrossScope2")
    class Bar {
    	int id;
    	
    	@CrossScope
    	public void method() {
    		return;
    	}
    }
    
}
