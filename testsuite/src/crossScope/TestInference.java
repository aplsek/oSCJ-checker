//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@DefineScope(name="crossScope.TestInference", parent=IMMORTAL)
@Scope("crossScope.TestInference") 
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

    public Foo getFoo() {
    	return this.foo;
    }

    @Scope("crossScope.TestInference")  
    @DefineScope(name="crossScope.MyHandler2", parent="crossScope.TestInference")
    class MyHandler2 extends PeriodicEventHandler {

    	private TestInference mission;
    	
        public MyHandler2(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestInference mission) {
            super(priority, parameters, scp);
            
            this.mission = mission;
        }

        @RunsIn("crossScope.MyHandler2") 
        public void handleAsyncEvent() {
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

        @RunsIn(UNKNOWN)
    	public Bar method(Bar bar) {
    		return bar;
    	}
    	
    	public Bar methodErr(Bar bar) {
    		return null;
    	}
    	
        public Bar method2() {
    		return this.field;
    	}	 
    }
    class Bar {
    }
}