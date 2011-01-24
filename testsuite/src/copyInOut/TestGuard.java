package copyInOut;

import javax.realtime.MemoryArea;
import javax.realtime.ScopedMemory;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;



public class TestGuard {
	
	Foo foo;
	
	@RunsIn(UNKNOWN)
	public Foo method(Foo h) {
		Foo c = new Foo();
		Foo c2 = new Foo();
		
		final MemoryArea memC = ScopedMemory.getMemoryArea(c);
		final MemoryArea memC2 = ScopedMemory.getMemoryArea(c2);
		final MemoryArea memFoo = ScopedMemory.getMemoryArea(foo);
		if (memC == memFoo) { 										// ---> GUARD
			c = foo;												// OK
		}
		if (memC == memFoo) { 										// ---> GUARD
			c2 = foo;												// ERROR
		}
		if (memC2 == memFoo) { 										// ---> GUARD
			c = foo;												// OK
		}
		return c;
	}
	
	class Foo {}
}
