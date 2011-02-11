//testsuite/src/scope/unknown/TestCrossScope.java:76: Illegal invocation of method of object in scope crossScope.TestCrossScope while in scope crossScope.MyHandler.
//            foo.methodErr(list);                                // ERROR: is not @RunsIn(UNKNOWN)
//                         ^
//testsuite/src/scope/unknown/TestCrossScope.java:78: Illegal invocation of method of object in scope crossScope.TestCrossScope while in scope crossScope.MyHandler.
//            foo.methodCross();                  //  ERROR: foo's methodCross runs in "Mission" so it
//                           ^
//testsuite/src/scope/unknown/TestCrossScope.java:81: Cannot assign expression in scope crossScope.TestCrossScope to variable in scope crossScope.MyHandler.
//            Foo foo2 = foo;
//                ^
//testsuite/src/scope/unknown/TestCrossScope.java:85: Illegal invocation of method of object in scope crossScope.TestCrossScope while in scope crossScope.MyHandler.
//            bar.methodCross();
//                           ^
//4 errors

package scope.unknown;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import static javax.safetycritical.annotate.Scope.IMMORTAL;




@DefineScope(name="crossScope.TestCrossScope", parent=IMMORTAL)
@Scope("crossScope.TestCrossScope") 
public class TestCrossScope extends Mission {

	public Foo foo;
	public Bar bar;
	
    protected
    void initialize() { 
        new MyHandler(null, null, null, 0, this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Scope("crossScope.TestCrossScope")
    @RunsIn(UNKNOWN)
    public Foo getFoo() {
    	return this.foo;
    }
    
    @Scope("crossScope.TestCrossScope")
    @RunsIn(UNKNOWN)
    public Bar getBar() {
        return this.bar;
    }



    @DefineScope(name="crossScope.MyHandler", parent="crossScope.TestCrossScope")
    @Scope("crossScope.TestCrossScope")  
    class MyHandler extends PeriodicEventHandler {

    	private TestCrossScope mission;
    	
        public MyHandler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize, TestCrossScope mission) {
            super(priority, parameters, scp);
            
            this.mission = mission;
        }

        @RunsIn("crossScope.MyHandler") 
        public void handleAsyncEvent() {
        	
            @Scope("crossScope.TestCrossScope") Foo foo = mission.getFoo();		// OK
            List list = new List();
            
            foo.method(list);                //  ---> OK
            foo.methodErr(list);				// ERROR: is not @RunsIn(UNKNOWN)
            
            foo.methodCross();			//  ERROR: foo's methodCross runs in "Mission" so it
             							//   should be annocated with "@RunsIn(UNKNOWN)"
       
            Foo foo2 = foo;             // ERROR
            
            
            Bar bar = mission.getBar();
            bar.methodCross();              // ERROR
            
        }


        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }



    class Foo {

    	List x;

    	public List methodCross() {				// this should be annotated with @RunsIn(UNKNOWN) to prevent the error abour
    		this.x = new List();
    		return x;
    	}
    	
    	
        @RunsIn(UNKNOWN)
    	public List method(List bar) {
    		return bar;
    	}
    	
    	public List methodErr(List bar) {
    		return null;
    	}
    	
    	
        public List method2() {
    		return this.x;
    	}	 
    }
    
    @Scope("crossScope.TestCrossScope") 
    class Bar {

        List x;

        @RunsIn("crossScope.TestCrossScope")
        public List methodCross() {             // this should be annotated with @RunsIn(UNKNOWN) to prevent the error abour
            this.x = new List();
            return x;
        }
       
    }



    class List {
    	String field;
    }
    
}
