package crossScope.allocate;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@DefineScope(name="crossScope.TestNullInference", parent=IMMORTAL)
@Scope("crossScope.allocate.TestErrorAlloc") 
public class TestErrorAlloc extends Mission  {

	Foo foo = new Foo();
	
    protected
    void initialize() { 
        new Handler(null, null, null, 0, this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
    
    @Scope("crossScope.allocate.TestErrorAlloc")
    public Foo getCurrentFoo() {					
    	return this.foo;
    }
    
    @DefineScope(name="crossScope.allocate.Handler", parent="crossScope.allocate.TestErrorAlloc")
    @Scope("crossScope.allocate.TestErrorAlloc")  
    class Handler extends PeriodicEventHandler {

    	TestErrorAlloc mission;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestErrorAlloc mission) {
            super(priority, parameters, scp);
            
            this.mission = mission;
        }

        @RunsIn("crossScope.allocate.Handler") 
        public void handleAsyncEvent() {
        	Foo foo = mission.getCurrentFoo();			//OK, but this should be inferred to be in Mission
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }


    class Foo {
    }
    
}

