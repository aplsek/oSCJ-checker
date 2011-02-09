//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error


package crossScope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="crossScope.TestErrorCrossScope", parent=IMMORTAL)
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
    
    public Foo getCurrentFoo() {
    	return this.foo;
    }
    

    @Scope("crossScope.TestErrorCrossScope")  
    @DefineScope(name="crossScope.Handler", parent="crossScope.TestErrorCrossScope")
    class Handler extends PeriodicEventHandler {

    	TestErrorCrossScope mission;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestErrorCrossScope mission) {
            super(priority, parameters, scp);
            
            this.mission = mission;
        }

        @RunsIn("crossScope.Handler") 
        public void handleAsyncEvent() {

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
    	
    	public Bar method() {
    		this.field = new Bar();						// ERROR
    		
    		return this.field;
    	}
    	
    }
    
    class Bar {
    }


}

