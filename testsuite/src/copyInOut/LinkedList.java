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
	
	class MyInternalIterator {
		@Scope(UNKNOWN) private Entry<E> lastReturned;
		@Scope(UNKNOWN) private Entry<E> next;
		private int nextIndex;

		@CrossScope
		public MyInternalIterator(int index) {
			// TODO: can we do this?:
			next = header;
		}

		@CrossScope
		public boolean hasNext() {
			return nextIndex != size;
		}

		@CrossScope
		public E next() {
			if (nextIndex == size)
				throw new NoSuchElementException();

			// TODO: DYNAMIC SCOPE CHECK FOR THE:
			lastReturned = next;  
			next = next.next;
			// --> end of the scope check
			
			nextIndex++;
			return lastReturned.element;
		}


		@CrossScope
		public void add(E e) {
			// TODO:
			final MemoryArea mem1 = MemoryArea.getMemoryArea(e);
			final MemoryArea mem2 = MemoryArea.getMemoryArea(header);
			if (mem1 == mem2) {
				// ...
				lastReturned = header;
				addBefore(e, next);
				nextIndex++;
			}
		}

		@CrossScope
		private  Entry<E> addBefore(E e, Entry<E> entry) {
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
