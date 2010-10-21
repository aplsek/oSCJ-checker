//scope/TestEnterPrivateMemory2.java:95: (Class scope.MyTestRunnable2 has a scope annotation with no matching @DefineScope)
//class MyTestRunnable2 implements Runnable {
//^
//1 error

package crossScope;

import javax.realtime.ImmortalMemory;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedEventHandler;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionManager;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.SingleMissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

import javax.safetycritical.annotate.DefineScope;

import scope.TestEnterPrivateMemory2;

@SCJAllowed(members=true)
@Scope("immortal")
public class TestUnannotatedClass2 extends CyclicExecutive {
  
    public TestUnannotatedClass2() {
        super(null);
    }
    
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
    	 CyclicSchedule.Frame[] frames = new CyclicSchedule.Frame[1];
         CyclicSchedule schedule = new CyclicSchedule(frames);
         frames[0] = new CyclicSchedule.Frame(new RelativeTime(200, 0), handlers);
         return schedule;
    }

    @SCJAllowed()
    @RunsIn("scope.TestEnterPrivateMemory2")
    public void initialize() {
        new WordHandler2(20000);
    }

    public long missionMemorySize() {
        return 5000000;
    }
    
    @SCJAllowed()
    @Scope("scope.TestEnterPrivateMemory2")
    @RunsIn("scope.TestEnterPrivateMemory2.WordHandler")
    public class WordHandler2 extends PeriodicEventHandler {

        @SCJAllowed()
        private WordHandler2(long psize) {
            super(null, null, null, psize);
            
        }

        @SCJAllowed()
        @RunsIn("scope.TestEnterPrivateMemory2.WordHandler")
        public void handleEvent() {
            
        	Foo foo = new Foo();
        	
        	@DefineScope(name="error_name", parent="scope.TestEnterPrivateMemory2.WordHandler")
        	MyTestRunnable2 runnable = new MyTestRunnable2(foo);
            ManagedMemory.
                getCurrentManagedMemory().enterPrivateMemory(300, 
                            runnable);
            
       
            
        }

        @SCJAllowed()
        public void cleanUp() {}
        
        @SCJAllowed()
        @Override
        public void handleAsyncEvent() {}

        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @Override
    public void setUp() {}
    @Override
    public void tearDown() {}

}

@SCJAllowed(members=true)
@Scope("scope.TestEnterPrivateMemory2.WordHandler")
@RunsIn("handler")
class MyTestRunnable2 implements Runnable {
   
	Foo myFoo;
	
	public MyTestRunnable2(Foo foo) {
		myFoo = foo;
	}
	
	public void run() {
		int val = myFoo.foo;
		myFoo.next = new Foo();
	}


}



class Foo {
	int foo;
	Foo next;
}