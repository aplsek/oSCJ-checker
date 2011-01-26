package copyInOut;

import static javax.safetycritical.annotate.Allocate.Area.THIS;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.realtime.MemoryArea;
import javax.realtime.ScopedMemory;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

public class LL {
	int id;
	LL next;

	// returns a deep copy of a list in current scope
	@RunsIn(UNKNOWN)
	public LL copyDown() {
		final LL c = new LL();
		c.id = this.id;												// DEEP-COPY all the fields
		final LL ct = this.next.copyDown();
		c.next = ct;
		return c;
	}

	@RunsIn(UNKNOWN)
	public void copyUp(final LL h) {
		if (h == null)
			return;												// TODO: copy-up an empty list?
		this.id = h.id;											// DEEP-COPY all the data in the node
		if (h.next == null)
			this.next = null;
		else {
			try {
				if (this.next == null) {
					final MemoryArea memT = ScopedMemory.getMemoryArea(this);
					final LL c = (LL) memT.newInstance(LL.class);				// inference 
					this.next = c;
				}
				this.next.copyUp(h.next);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	@Allocate({THIS})				                // OK: Returns value because it uses @Allocate
	@RunsIn(UNKNOWN)
	public LL copyDown2Up(LL h) { 					// XXX: This is valid only in our new annotation system!
		if (h == null)
			return null;
		LL c = null;
		try {
			final MemoryArea mem = ScopedMemory.getMemoryArea(this);
			c = (LL) mem.newInstance(LL.class);

			c.id = h.id; 										// DEEP-COPY all the fields
			LL ct = c.copyDown2Up(h.next);

			final MemoryArea memC = ScopedMemory.getMemoryArea(c);
			final MemoryArea memCT = ScopedMemory.getMemoryArea(ct);
			if (memC == memCT) {								 // ---> GUARD
				c.next = ct;
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return c; 							// OK if used with @Allocate
		                                    // ERROR otherwise: returns an object that is not allocated in the
											//    current scope
	}

	@RunsIn(UNKNOWN)
	public LL testBadReturn() {
		try {
			final MemoryArea mem = ScopedMemory.getMemoryArea(this);
			LL c = (LL) mem.newInstance(LL.class);
			return c;														// ERROR
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

}
