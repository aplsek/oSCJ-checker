//crossScope_A/TestInference.java:64: error message.
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
import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Allocate.Area.*;


@Scope("crossScope_A.TestInference") 
public class TestInference extends Mission {

	public Foo foo;
	
    protected
    void initialize() { 
        new MyHandler2(null, null, null, 0, this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Allocate({THIS})
    public Foo getFoo() {
    	return this.foo;
    }

    @Scope("crossScope_A.TestInference")  
    @RunsIn("crossScope_A.MyHandler2") 
    class MyHandler2 extends PeriodicEventHandler {

    	private TestInference mission;
    	
        public MyHandler2(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestInference mission) {
            super(priority, parameters, scp, memSize);
            
            this.mission = mission;
        }

        public
        void handleEvent() {
            Foo foo = mission.getFoo();
            Bar bar = new Bar();
            
            foo.field = bar;                   // ERROR
        }

        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    class Foo {

    	public Bar field;

    	@Allocate({CURRENT})
        @CrossScope
    	public Bar method(Bar bar) {
    		return bar;
    	}
    	
    	public Bar methodErr(Bar bar) {
    		return null;
    	}
    	
    	
    	@Allocate({THIS})
        public Bar method2() {
    		return this.field;
    	}	 
    }
    class Bar {
    }
}