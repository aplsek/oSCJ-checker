package crossScope.inference;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionManager;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@Scope("crossScope.inference.TestRunsIn") 
public class TestRunsIn   extends Mission {

	Foo f;
	
    protected
    void initialize() { 
        new Handler(null, null, null, 0, this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    public Foo getFoo() {
    	return this.f;
    }


    @Scope("crossScope.inference.TestRunsIn")  
    @RunsIn("crossScope.inference.Handler") 
    class Handler extends PeriodicEventHandler {

    	private TestRunsIn mission;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestRunsIn mission) {
            super(priority, parameters, scp, memSize);
            
            this.mission = mission;
        }

        public
        void handleEvent() {
        	Foo foo = mission.getFoo();		// OK, inferred
        	
        	foo.method();					// ERROR, the method must be cross-scope, @RunsIn inferred!!
        	
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    class Foo {
    	Foo f;
    	
    	public void method() {
    		this.f = new Foo();				// OK
    	}
    }

	
}
