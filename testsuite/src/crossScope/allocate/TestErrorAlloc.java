package crossScope.allocate;

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
    
    //@Allocate({THIS})
    public Foo getCurrentFoo() {					// ERROR, should be @Allocate({THIS})
    	return this.foo;
    }
    

    @Scope("crossScope.allocate.TestErrorAlloc")  
    @RunsIn("crossScope.allocate.Handler") 
    class Handler extends PeriodicEventHandler {

    	TestErrorAlloc mission;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestErrorAlloc mission) {
            super(priority, parameters, scp, memSize);
            
            this.mission = mission;
        }

        public
        void handleEvent() {
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

