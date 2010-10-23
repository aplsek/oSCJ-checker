//crossScope_A/TestTransitiveCall.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope;

import javax.realtime.ImmortalMemory;
import javax.realtime.RealtimeThread;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Allocate.Area.*;

@SCJAllowed(members=true)
public class TestUnannotatedClass {

	private Foo myFoo;
	
	public void defineScopes() {
		    @DefineScope(name = "foo", parent = "immortal")
		    PrivateMemory a = new PrivateMemory(0);
	}
	
	public void test1() {
		ImmortalMemory im = getMyImmortal();
		
		Foo foo = new Foo();
		
		myFoo = new Foo();
		
	}
	
	
	public void test2() {
		ImmortalMemory im = getMyImmortal();
		
		Foo foo = new Foo();
		
		myFoo = new Foo();
		
	}
	
	public void test3() {
		RealtimeThread.getCurrentMemoryArea();
		
	}
	
	public ImmortalMemory getMyImmortal() {
		return ImmortalMemory.instance();
	}

	

	@Scope("foo")
	class Foo {
		
	}
	
}

