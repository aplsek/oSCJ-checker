package crossScope_A.motivation;


import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;

@Scope("crossScope_A.motivation.TestErrorCrossScope") 
public class TestNoParametersMethod extends Mission  {

	Foo foo;
	
    protected
    void initialize() { 
        new Handler(null, null, null, 0, this);
        
        foo = new Foo();
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
    
    @Allocate({THIS})
    public Foo getCurrentFoo() {
    	return this.foo;
    }
    

    @Scope("crossScope_A.motivation.TestErrorCrossScope")  
    @RunsIn("crossScope_A.motivation.Handler") 
    class Handler extends PeriodicEventHandler {

    	TestNoParametersMethod mission;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestNoParametersMethod mission) {
            super(priority, parameters, scp, memSize);
            
            this.mission = mission;
        }

        public void handleEvent() {
        	Foo foo = mission.getCurrentFoo();			// OK, foo will be inferred to be in Mission
        	Bar b = foo.method();						// ERROR? should this method be @CrossScope??
        												//      YES - "method" should be crosSCope to make this correct
        	Bar b2 = foo.method2();
        
        }

        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }
    
   


}

class Foo {
	private Bar field;
	
	@Allocate({THIS})
	@CrossScope
	public Bar method() {
		this.field = new Bar();						// ERROR and will be detected
		return this.field;
	}
	
	@Allocate({THIS})
	public Bar method2() {
		try {
			MemoryArea mem = MemoryArea.getMemoryArea(this);
			this.field = (Bar) mem.newInstance(Bar.class);	    // OK
		} catch (Exception e) {
			e.printStackTrace();
		}					
		return this.field;
	}
}

class Bar {
}
