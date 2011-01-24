package crossScope;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope("crossScope.TestErrorCrossScope") 
public class TestErrorCrossScope extends Mission  {

	Foo foo = new Foo();
	
    protected
    void initialize() { 
        new Handler(null, null, null, 0, this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
    
    @Allocate({THIS})
    public Foo getCurrentFoo() {
    	return this.foo;
    }
    

    @Scope("crossScope.TestErrorCrossScope")  
    @RunsIn("crossScope.Handler") 
    class Handler extends PeriodicEventHandler {

    	TestErrorCrossScope mission;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestErrorCrossScope mission) {
            super(priority, parameters, scp);
            
            this.mission = mission;
        }

        public
        void handleAsyncEvent() {

        	Foo foo = mission.getCurrentFoo();			// OK, will be inferred to be in Mission
        	Bar b = foo.method();						// ERROR? should this method be @RunsIn(UNKNOWN)??
        }												//      YES - "method" should be crosScope to amke this correct
        												//      default @RunsIn of this method is the same as @scope 
        												//      of "foo", which is "mission" in this case


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }
    
    class Foo {
    	private Bar field;
    	
    	@Allocate({THIS})
    	public Bar method() {
    		this.field = new Bar();						// ERROR
    		
    		return this.field;
    	}
    	
    }
    
    class Bar {
    }


}

