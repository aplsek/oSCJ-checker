package crossScope.motivation;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;

@DefineScope(name="crossScope.motivation.TestErrorCrossScope", parent=IMMORTAL)
@Scope("crossScope.motivation.TestErrorCrossScope") 
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
    
    @DefineScope(name="crossScope.motivation.Handler", parent="crossScope.motivation.TestErrorCrossScope")
    @Scope("crossScope.motivation.TestErrorCrossScope")  
    class Handler extends PeriodicEventHandler {

    	TestNoParametersMethod mission;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestNoParametersMethod mission) {
            super(priority, parameters, scp);
            
            this.mission = mission;
        }

        @RunsIn("crossScope.motivation.Handler") 
        public void handleAsyncEvent() {
        	Foo foo = mission.getCurrentFoo();			// OK, foo will be inferred to be in Mission
        	Bar b = foo.method();						// ERROR? should this method be @RunsIn(UNKNOWN)??
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
	
	@RunsIn(UNKNOWN)
	public Bar method() {
		this.field = new Bar();						// ERROR and will be detected
		return this.field;
	}
	
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
