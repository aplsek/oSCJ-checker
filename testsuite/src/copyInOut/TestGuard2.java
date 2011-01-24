package copyInOut;

import java.util.Iterator;
import java.util.LinkedList;

import javax.realtime.MemoryArea;
import javax.realtime.ScopedMemory;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


public class TestGuard2 {

	Foo foo;
	
	@RunsIn(UNKNOWN)
	public Foo method(final Foo h) {
		
		final MemoryArea ma = ScopedMemory.getMemoryArea(h);
		
		
		LinkedList list = null;
		Iterator iter = (Iterator) list.iterator();
		
		try {
			final Foo foo = (Foo) ma.newInstance(Foo.class);			// OK
		
			boolean bool = true;
			final MemoryArea mm;
			if (bool) {
				mm = ScopedMemory.getMemoryArea(h);
			} else {
				mm = ScopedMemory.getMemoryArea(this.foo);
			}
			final Foo foo2 = (Foo) mm.newInstance(Foo.class);	
		
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return h;
	}
	
	
	
	class Foo {}
	class Bar {}
}
