package copyInOut;

import java.util.NoSuchElementException;

import javax.realtime.MemoryArea;
import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.Scope;

import copyInOut.MyIterator.Radd;

public class LinkedList<E> {
	private transient Entry<E> header = new Entry<E>();
	private transient int size = 0;
	protected transient int modCount = 0;

	
	
	@CrossScope
	public MyInternalIterator getCrossScopeIterator() {
		return new MyInternalIterator(0);
	}
	
	
	class MyInternalIterator {
		@Scope(UNKNOWN) private Entry<E> lastReturned = header;
		@Scope(UNKNOWN) private Entry<E> next;
		private int nextIndex;
		private int expectedModCount = modCount;

		@CrossScope
		public MyInternalIterator(int index) {
			if (index < 0 || index > size)
				throw new IndexOutOfBoundsException("Index: "+index+
						", Size: "+size);
			if (index < (size >> 1)) {
				next = header.next;
				for (nextIndex=0; nextIndex<index; nextIndex++)
					next = next.next;
			} else {
				next = header;
				for (nextIndex=size; nextIndex>index; nextIndex--)
					next = next.previous;
			}
		}

		@CrossScope
		public boolean hasNext() {
			return nextIndex != size;
		}

		@CrossScope
		public E next() {
			//checkForComodification();  // TODO: needs to be rewritten to 
			if (nextIndex == size)
				throw new NoSuchElementException();

			// QUESTION: do we need the dynamic scope checks for the following?:
			lastReturned = next;
			next = next.next;
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
				expectedModCount++;
			}
		}

		@CrossScope
		private  Entry<E> addBefore(E e, Entry<E> entry) {
			final MemoryArea mem1 = MemoryArea.getMemoryArea(e);
			final MemoryArea mem2 = MemoryArea.getMemoryArea(entry);
			final MemoryArea mem3 = MemoryArea.getMemoryArea(e);
			Entry newItem = (Entry) mem2.newInstance(Entry.class);    // inferred to be mem2 ?? 			

			if ( (mem1 == mem2) && ( mem2 == mem3) ) {
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