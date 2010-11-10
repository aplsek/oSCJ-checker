package copyInOut;

import static javax.safetycritical.annotate.Allocate.Area.THIS;

import javax.realtime.MemoryArea;
import javax.realtime.ScopedMemory;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.CrossScope;


public class LL {
	int id;
	LL next;

	// returns a deep copy of a list in current scope
	@CrossScope
	public LL copyDown() {
		LL c = new LL();
		c.id = this.id;												// DEEP-COPY all the fields

		LL ct = this.next.copyDown();
		final MemoryArea memC = ScopedMemory.getMemoryArea(c);
		final MemoryArea memCT = ScopedMemory.getMemoryArea(ct);
		if (memC == memCT) { 										// ---> GUARD
			c.next = ct;
		}
		return c;
	}

	@CrossScope
	public void copyUp(LL h) {
		if (h == null)
			return;												// TODO: copy-up an empty list?

		this.id = h.id;											// DEEP-COPY all the data in the node

		if (h.next == null)
			this.next = null;
		else {
			try {
				if (this.next == null) {
					final MemoryArea memT = ScopedMemory
					.getMemoryArea(this);
					final MemoryArea memC = ScopedMemory
					.getMemoryArea(this);
					LL c = (LL) memT.newInstance(LL.class);
					if (memT == memC)
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

	@Allocate({THIS})				                // Returns value because it uses @Allocate
	@CrossScope
	public LL copyDown2Up(LL h) { 					// XXX: This is valid only in our new annotation system!
		if (h == null)
			return null;

		LL c = null;
		try {
			final MemoryArea mem = ScopedMemory.getMemoryArea(this);
			c = (LL) mem.newInstance(LL.class);

			c.id = h.id; // DEEP-COPY all the fields
			LL ct = c.copyDown2Up(h.next);

			final MemoryArea memC = ScopedMemory.getMemoryArea(c);
			final MemoryArea memCT = ScopedMemory.getMemoryArea(ct);
			if (memC == memCT) { // ---> GUARD
				c.next = ct;
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return c; // ERROR: returns an object that is not allocated in the
		// current scope
	}

	@CrossScope
	public LL testBadReturn() {
		try {
			final MemoryArea mem = ScopedMemory.getMemoryArea(this);
			LL c = (LL) mem.newInstance(LL.class);
			return c;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
