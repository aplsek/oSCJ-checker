package copyInOut;

import static javax.safetycritical.annotate.Allocate.Area.THIS;

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.ScopedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("copyInOut.TestLL")
public class TestLL extends Mission {

	LL ll;

	protected void initialize() {
		new MyHandler(null, null, null, 0, this);
	}

	@Override
	public long missionMemorySize() {
		return 0;
	}

	@CrossScope
	public LL getLL() {
		return ll.copyDown();
	}

	public LL getRealLL() {
		return this.ll;
	}

	@CrossScope
	public void putLL(LL h) { // ---> does not return a reference!!
		this.ll.copyUp(h);
	}

	@Scope("copyInOut.TestLL")
	@RunsIn("copyInOut.TestLL")
	class MyHandler extends PeriodicEventHandler {

		TestLL myMission;

		public MyHandler(PriorityParameters priority,
				PeriodicParameters parameters, StorageParameters scp,
				long memSize, TestLL mission) {
			super(priority, parameters, scp, memSize);
			this.myMission = mission;
		}

		public void handleEvent() {

			LL myList = new LL();

			LL list = myMission.getLL();

			@Scope("MyMission") 
			LL realLL = myMission.getRealLL();

			// to copy from down to up???
			myMission.putLL(myList);

			@Scope("MyMission")
			Mission mission = Mission.getCurrentMission(); // where this lives?
															// mission can't be passed into any method, only into @CS method
															// all mission's method visible from here must be @CS
															// --> implicit or explicit inference, these limitations holds

			@Scope("Immortal")
			MemoryArea immMemory = ImmortalMemory.instance();
			@Scope("MyMissison")
			MemoryArea mem = RealtimeThread.getCurrentMemoryArea();

			// MyRun run = new MyRun();
			// run.ll = myList;
			// run.myMission = myMission;
			// MemoryArea mem = MemoryArea.areaOf(myMission);
			// mem.executeInArea(run);

		}

		@RunsIn("MyMission")
		@Scope("Handler")
		class MyRun implements Runnable {

			LL ll;
			TestLL myMission;

			public void run() {
				myMission.putLL(ll.copyDown());
			}
		}

		@Override
		public StorageParameters getThreadConfigurationParameters() {
			return null;
		}
	}

	class LL {
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
		
		@Allocate({THIS})				// Returns value because it uses @Allocate
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
			return c; // ERROR: returns an object that is not alloc in the
						// current scope
		}
	}
}
