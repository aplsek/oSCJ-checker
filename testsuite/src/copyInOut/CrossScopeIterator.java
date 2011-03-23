package copyInOut;

import java.util.NoSuchElementException;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.safetycritical.annotate.RunsIn;

class LinkedList<E> {
    public transient Entry<E> header = new Entry<E>();
    public transient int size = 0;
    public transient int modCount = 0;

    @RunsIn(UNKNOWN)
    public CrossScopeIterator<E> getCrossScopeIterator() {
        return new CrossScopeIterator<E>(this, 0);
    }
}

class CrossScopeIterator<E> {

    @Scope(UNKNOWN)
    private Entry<E> lastReturned;
    @Scope(UNKNOWN)
    private Entry<E> next;
    private int nextIndex;

    @Scope(UNKNOWN)
    LinkedList<E> list;

    @RunsIn(UNKNOWN)
    public CrossScopeIterator(LinkedList<E> list, int index) {
        // TODO : DYNAMIC CHECK next.isAbove(header);
        next = list.header;
        this.list = list;
        nextIndex = 0;
    }

    public boolean hasNext() {
        return nextIndex != list.size;
    }

    public @Scope(UNKNOWN)
    E next() {
        if (nextIndex == list.size)
            throw new NoSuchElementException();

        lastReturned = next; // NOTE: no dynamic check needed, both are the
                             // fields of "this"
        next = next.next; // NOTE: no dynamic check needed, "next.next" is an
                          // unannotated field of "next" and therefore they are
                          // in the same scope

        nextIndex++;
        return lastReturned.element;
    }

    public void add(@Scope(UNKNOWN) E e) {
        // TODO:
        @Scope(UNKNOWN)
        final Entry<E> temp_header = list.header;

        // asdfasd

        if (ManagedMemory.allocInSame(temp_header, this)) {
            // ...
            this.lastReturned = temp_header; // NOTE: assigning to a field of
                                             // this, dynamic check is for
                                             // "this".
            addBefore(e, this.next);
            this.nextIndex++;
        }
    }

    public void remove() {
        // TODO:
        // ..
    }

    private @Scope(UNKNOWN)
    Entry<E> addBefore(final @Scope(UNKNOWN) E e,
            final @Scope(UNKNOWN) Entry<E> entry) {
        Entry newItem = null;
        try {
            final MemoryArea mem1 = MemoryArea.getMemoryArea(e);
            final MemoryArea mem2 = MemoryArea.getMemoryArea(entry);
            newItem = (Entry) mem2.newInstance(Entry.class); // TODO: inferred
                                                             // to be mem2 ??

            if ((mem1 == mem2)) {
                newItem.element = e;
                newItem.previous = entry.previous;
                newItem.next = entry.next;

                newItem.previous.next = newItem;
                newItem.next.previous = newItem;

                list.size++;
                list.modCount++;
            } else {
                throw new Error();
            }
        } catch (InstantiationException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        }

        return newItem;
    }
}

class Entry<E> {
    E element;
    Entry<E> next;
    Entry<E> previous;
}

class MyOtherMission extends Mission {
    LinkedList list;

    @Override
    public void initialize() {
        MyHandler handler = new MyHandler();
        handler.list = list;
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
}

class MyHandler extends PeriodicEventHandler {
    public MyHandler() {
        super(null, null, null);
    }

    public MyHandler(PriorityParameters priority, PeriodicParameters period,
            StorageParameters storage, long size) {
        super(priority, period, storage);
    }

    @Scope(UNKNOWN)
    public LinkedList list;

    @Override
    public void handleAsyncEvent() {
        CrossScopeIterator myIterator = list.getCrossScopeIterator();
        while (myIterator.hasNext()) {
            Foo f = (Foo) myIterator.next();
            // ..
        }
    }
}
