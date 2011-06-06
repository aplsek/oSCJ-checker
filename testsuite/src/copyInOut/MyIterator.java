package copyInOut;

import java.util.LinkedList;
import java.util.Iterator;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


/**
*    MyIterator is the same as Dan's LinkedList iterator from the email below.
**/
class MyIterator {
    @Scope(UNKNOWN) private LinkedList list;

    @Scope(UNKNOWN) private Iterator iter;

    int nextIndex ;
    int size;

    @RunsIn(UNKNOWN)
    public MyIterator (final LinkedList list) {
            final MemoryArea mem1 = MemoryArea.getMemoryArea(list);
            final MemoryArea mem2 = MemoryArea.getMemoryArea(this);
            if (ManagedMemory.allocatedInSame(list, this)) {
                this.list = list;
                iter = list.iterator();
            } else {
                throw new Error();
            }

            //TODO: assign index and size

        }
        @RunsIn(UNKNOWN) boolean hasNext() {
        	  return nextIndex != size;
        }

        @RunsIn(UNKNOWN) Object next() {
            if (hasNext()) {

            	Rget r = new Rget(iter);


            	// Even though list is in some above scope, tail is not
                // UNKNOWN so I believe we can assume it's the same scope
                //list = list.tail;
                //return list.item;
            	nextIndex++;

            } else {
                throw new Error();
            }
			return iter;   // FIXME: ????
        }

        @RunsIn(UNKNOWN) void remove() {
            throw new Error();
        }


        @RunsIn(UNKNOWN) void add(Foo item) {
        	  final MemoryArea mem1 = MemoryArea.getMemoryArea(item);
              final MemoryArea mem2 = MemoryArea.getMemoryArea(list);
              if (mem1 == mem2) {
            	  list.add(item);  // can we do this???
            	  Radd r = new Radd(item,list);
            	  mem1.executeInArea(r);
              }

              size++;
        }

        class Rget implements Runnable {
        	@Scope(UNKNOWN)  Foo item;
        	@Scope(UNKNOWN)  Iterator iter;

        	public Rget(Iterator iter) {
        		this.iter = iter;
        	}

        	public void run() {
        		this.item = (Foo) iter.next();
        	}
        }

        class Radd implements Runnable {
        	@Scope(UNKNOWN)  Foo item;
        	@Scope(UNKNOWN)  LinkedList list;

        	public Radd(Foo item, LinkedList list) {
        		this.item = item;
        		this.list = list;
        	}

        	public void run() {
        		list.add(item);
        	}
        }

}

class MyMission extends Mission {
    LinkedList list;

	@Override
    public void initialize() {
        Handler handler = new Handler();
        handler.myIterator = new MyIterator(list);
    }

	@Override
    public long missionMemorySize() {
		return 0;
	}
}

class Handler extends PeriodicEventHandler {
    public Handler() {
    	super(null,null,null);
    }

	public Handler(PriorityParameters priority, PeriodicParameters period,
			StorageParameters storage, long size) {
		super(priority, period, storage);
	}


	MyIterator myIterator;

    @Override
    public void handleAsyncEvent() {
        // f is @Scope(UNKNOWN) if we now assume @RunsIn(UNKNOWN)s return UNKNOWN
        for ( ; myIterator.hasNext() ; ) { // does f need to be final? is it already?
            Foo f = (Foo) myIterator.next();    // @Scope(UNKNOWN) object casted to @Scope(Mission) Foo

            f.crossScope(); // legal, we don't know where f lives
            f.notCrossScope(); // not legal, must be same scope
            final MemoryArea mem1 = MemoryArea.getMemoryArea(f);
            final MemoryArea mem2 = ManagedMemory.getCurrentManagedMemory();
            if (mem1 == mem2) {
                f.crossScope(); // still legal
                f.notCrossScope(); // now legal because we have same-scope
            }
        }
    }
	public StorageParameters getThreadConfigurationParameters() {
		return null;
	}
}

@Scope("Mission")                                // <<-- Foo annotated with @Scope("Mission")
class Foo {
    void notCrossScope() { }
    @RunsIn(UNKNOWN) void crossScope() { }
}
