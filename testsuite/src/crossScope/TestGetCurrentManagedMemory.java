package crossScope;


import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

import crossScope.TestErrorCrossScope.Foo;
import crossScope.TestErrorCrossScope.Handler;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope("crossScope.TestGetCurrentManagedMemory") 
public class TestGetCurrentManagedMemory {


    protected
    void initialize() { 
        new Handler(null, null, null, 0);
    }

    public long missionMemorySize() {
        return 0;
    }
    
    @Scope("crossScope.TestGetCurrentManagedMemory")  
    @RunsIn("crossScope.Handler") 
    class Handler extends PeriodicEventHandler {

    	Foo foo ;
    	
        public Handler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize) {
            super(priority, parameters, scp);
        
            this.foo = new Foo();			// OK
        }

        public void handleAsyncEvent() {

        	this.foo = new Foo();			// ERROR
        	
        	MyRunnable aRunnable = new MyRunnable(this);
        	@DefineScope(name="crossScope.Handler.Child", parent="crossScope.Handler") 
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
            mem.enterPrivateMemory(1000, aRunnable);  
        	
        }        

        public Foo getCurrentFoo() {
        	return this.foo;
        }
        
        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }
    
    
    @Scope("crossScope.Handler")  
    @RunsIn("crossScope.Handler.Child") 
    class MyRunnable implements Runnable {
		
    	private Handler handler;
    	
    	public MyRunnable(Handler handler) {
    		this.handler = handler;
    	}
    	
    	public void run() {
			BigBar bb = new BigBar();
			bb.method();
			
			Foo foo = handler.getCurrentFoo();				// OK
			
			foo.methodAlloc();								// ERROR< should be @RunsIn(UNKNOWN)
		}
    }
    
    
    class Bar {
    	public void method() {
    		ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();  // ERROR
    	}
    }

    @RunsIn("crossScope.Handler.Child") 
    class BigBar {
    	public void method() {
    		ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();  // OK
    	}
    }
    
    
    class Foo {
    	
    	Bar bar;
    	
    	public void method() {
    		
    	}
    	
    	public void methodAlloc() {
    		this.bar = new Bar();			// OK
    	}
    }

	
}
