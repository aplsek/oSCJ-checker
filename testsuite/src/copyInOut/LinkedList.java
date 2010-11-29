package copyInOut;

import java.util.NoSuchElementException;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.Scope;

public class LinkedList<E> {
	private transient Entry<E> header = new Entry<E>();
	private transient int size = 0;
	protected transient int modCount = 0;
	
	@CrossScope
	public MyInternalIterator getCrossScopeIterator() {
		return new MyInternalIterator(0);
	}
	
	static class MyInternalIterator {
		
		@Scope(UNKNOWN) private Entry<E> lastReturned;
		@Scope(UNKNOWN) private Entry<E> next;
		private int nextIndex;

		@CrossScope
		public MyInternalIterator(int index) {
			// TODO : DYNAMIC CHECK next.isAbove(header);
			next = header;
		}

		@CrossScope
		public MyInternalIterator(LinkedList list,int index) {
			// TODO : DYNAMIC CHECK next.isAbove(header);
			next = header;
		}
		
		public boolean hasNext() {
			return nextIndex != size;
		}

		public E next() {
			if (nextIndex == size)
				throw new NoSuchElementException();

			lastReturned = next;     // NOTE: no dynamic check needed, both are the fields of "this"
			next = next.next; 		 // NOTE: no dynamic check needed, "next.next" is an unannotated field of "next" and therefore they are in the same scope 
			
			nextIndex++;
			return lastReturned.element;
		}

		public void add(@Scope(UNKNOWN) E e) {
			// TODO:
			@Scope(UNKNOWN) final Entry<E> temp_header = header;
			
			final MemoryArea mem1 = MemoryArea.getMemoryArea(this);
			final MemoryArea mem2 = MemoryArea.getMemoryArea(temp_header);
			if (mem1 == mem2) {
				// ...
				lastReturned = header;		// NOTE: assigning to a field of this, dynamic check is for "this".
				addBefore(e, next);
				nextIndex++;
			}
		}
		
		public void remove() {
			// TODO:
		}

		private  Entry<E> addBefore(final @Scope(UNKNOWN) E e,final @Scope(UNKNOWN) Entry<E> entry) {
			final MemoryArea mem1 = MemoryArea.getMemoryArea(e);
			final MemoryArea mem2 = MemoryArea.getMemoryArea(entry);
			Entry newItem = (Entry) mem2.newInstance(Entry.class);    // TODO: inferred to be mem2 ?? 			

			if ( (mem1 == mem2) ) {
				newItem.element = e;
				newItem.previous = entry.previous;
				newItem.next = entry.next;

				newItem.previous.next = newItem;
				newItem.next.previous = newItem;

				size++;
				modCount++;
				return newItem;
			}
			else {
				throw new Error();
				return null;
			}
		}
	}


	class Entry<E> {
		E element;
		Entry<E> next;
		Entry<E> previous;
	}
}




class MyOtherMission extends Mission {	
	LinkedList list;
	
	public void initialize() {
		MyHandler handler = new MyHandler();
		handler.list = 	list;	
	}

	@Override
	public long missionMemorySize() {
		return 0;
	}	
}

class MyHandler extends PeriodicEventHandler {
    public MyHandler() {
    	super(null,null,null,0);
    }
	
	public MyHandler(PriorityParameters priority, PeriodicParameters period,
			StorageParameters storage, long size) {
		super(priority, period, storage, size);
	}

	@Scope(UNKNOWN) public LinkedList list;
    
    public void handleEvent() {
        LinkedList.MyInternalIterator myIterator = list.getCrossScopeIterator();
        for ( ; myIterator.hasNext() ; ) {
            Foo f = (Foo) myIterator.next();   
            //..
        }
    }

	@Override
	public StorageParameters getThreadConfigurationParameters() {
		return null;
	}
}
