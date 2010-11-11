package copyInOut;

import javax.realtime.MemoryArea;
import javax.realtime.ScopedMemory;
import javax.safetycritical.annotate.CrossScope;

public class TestGuard2 {

	Foo foo;
	
	@CrossScope
	public Foo method(final Foo h) {
		
		final MemoryArea ma = ScopedMemory.getMemoryArea(h);
		
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
