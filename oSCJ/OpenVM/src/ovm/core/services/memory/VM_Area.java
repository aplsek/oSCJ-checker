package ovm.core.services.memory;

import ovm.core.domain.Oop;
import s3.util.PragmaNoPollcheck;
import ovm.util.OVMError;
import ovm.core.domain.ReflectiveField;
import ovm.core.domain.DomainDirectory;
import ovm.core.repository.JavaNames;
import ovm.core.execution.Interpreter;

/**
 * An area of memory in which objects live.  Objects may be assiciated
 * with {@link VM_Area.Destructor}s that implement weak references or
 * java finalization semantics, and these destructors are stored in
 * per-VM_Area tables (see {@link VM_Area.DestructorTable}).  This
 * abstraction exists to support Realtime Java scoped memory, but the
 * base VM_Area class is better suited to memory management
 * configurations in which there is a single heap.
 **/
public abstract class VM_Area {
    public VM_Area() { }

    /**
     * Free all object managed by this area.  Subsequent
     * allocations from this area will reuse their storage.<p>
     * This is an optional operations
     * @throws OVMError.Unimplemented
     **/
    public void reset() {
	throw new OVMError.Unimplemented();
    }

    /**
     * Travel back to the time when memoryConsumed() == newOffset,
     * freeing all objects that where subsequently allocated in this
     * area.
     * <p>
     * The argument should be &lt;= the current value of
     * memoryConsumed().
     *
     * @param newOffset the new allocation offset
     * This is an optional operation
     * @throws OVMError.Unimplemented
     **/
    public void reset(int newOffset) {
	throw new OVMError.Unimplemented();
    }
    
    /**
     * Destroy this allocator and return its memory to the system.<p>
     * This is an optional operation.
     * @throws OVMError.Unimplemented
     **/
    public void destroy() {
	throw new OVMError.Unimplemented();
    }

    public abstract int size();
    public abstract int memoryConsumed();

    public int memoryRemaining() {
	return size() - memoryConsumed();
    }

    // Destructor support

    /**
     * Each destructor object stores the VM_Address of the reference
     * the destructor is defined on.  An implementation of
     * VM_Address.revive() may need to take the address of t
     **/
    static protected ReflectiveField.Address destructorRef = 
	new ReflectiveField.Address 
	(DomainDirectory.getExecutiveDomain(), 
	 JavaNames.ovm_core_services_memory_VM_Address, 
	 JavaNames.ovm_core_services_memory_VM_Area_Destructor,
	 "ref");

    /**
     * Destructor objects contain code that is executed after some
     * object becomes otherwise unreachable.  They can be used to
     * implement java finalizers, the java.lang.ref API, and other
     * cleanup actions.
     **/
    static public abstract class Destructor {
	/**
	 * @see VM_Area#destructorCount
	 **/
	static public final int ALL = -1;
	
	/**
	 * These destructors are processed before normal destructors.
	 * <code>BEFORE</code> destructors are guaranteed to run
	 * whether or not {@link #NORMAL} or {@link #AFTER}
	 * destructors revive their referent, and are suitable for
	 * implementing java weak references.
	 **/
	static public final int BEFORE = 0;
	/**
	 * These destructors are processed after all {@link #BEFORE}
	 * and before all {@link #AFTER} destructors.  They are
	 * perfectly normal, and will not be run if an object becomes
	 * reachable due to the actions of a <code>BEFORE</code>
	 * destructor.
	 **/
	static public final int NORMAL = 1;
	/**
	 * These destructors are processed after all {@link #BEFORE}
	 * and {@link #NORMAL} destructors.  If, by calling {@link
	 * VM_Area#revive}, a <code>BEFORE</code> or
	 * <code>NORMAL</code> destructor makes an object directly or
	 * indirectly reachable, its after destructor will not be run.
	 * <code>AFTER</code> destructors are suitable for
	 * implementing java phantom references.
	 **/
	static public final int AFTER = 2;

	static public final int N_KINDS = 3;

	VM_Address ref;
	public Destructor(Oop ref) { this.ref = VM_Address.fromObject(ref); }

	/**
	 * This method is called inside the garbage collector when
	 * the referenced object is found to be unreachable.  
	 **/
	public abstract void destroy(VM_Area home);

	/**
	 * Override this method if a destructor should be run {@link #BEFORE}
	 * or {@link #AFTER} all {@link #NORMAL} destructors
	 **/
	public abstract int getKind();

	Destructor next, prev;
    }

    /**
     * If destructors where sorted by something else, (perhaps
     * address), then we might need to reinsert a destructor every
     * time an object was moved.  As it stands, however, this variable
     * is {@value}.
     **/
    static private final boolean DESTRUCTORS_SORTED_BY_AGE = false;
    
    /**
     * Maintain a table of all destructors of a certain kind on
     * objects within an area.  This table supports 3 operations: add,
     * remove and walk.  Within a walk of the table, it must be
     * possible to remove any destructor within the table, and re-add
     * the current destructor at a different address.  This table may
     * contain multiple destructors defined on a single object.<p>
     * 
     * It seems preferable to maintain destructors in some kind of
     * sorted structure such as a binary tree or address-based
     * hashtable.  When traversing a sorted structure, we will run all
     * destructors defined on a particular object (and objects on a
     * particular cache line) together.  A sorted structure can also
     * be used to traverse destructors in the younger generation only.<p>
     *
     * While a sorted structure may be desireable, it is by no means
     * required.  Because the remove method takes a destructor rather
     * than an address, we do not need to search inside remove.  Also,
     * it is interesting to note that a simple doubly-linked list of
     * finalizers will remain in addresss order in a sinmple copying
     * collector.<p>
     *
     * Because address-based hashtables are slow and use a great deal
     * of memory, while balanced binary trees seem like overkill, a
     * doubly-linked list is used.
     *
     * <p><b>NOTE:</b> The finalization of scoped memory relies on a FIFO
     * ordering of destructors, so that only the destructors that exist
     * when finalization commences, can be processed simply by specifying the
     * number of destructors. Any finalizable objects created by those
     * destructors would appear at the end of the list, and would not be
     * processed within that count. - David Holmes Sep. 2005
     **/
    protected static class DestructorTable {
	private final Destructor clasp;

        private static class Clasp extends Destructor {
            Clasp() {
                super(VM_Address.fromInt(0).asOop());
            }
            public void destroy(VM_Area _) { }
            public int getKind() { return NORMAL; }
        }

	public DestructorTable(int addressRange) {
	    clasp = new Clasp();
	    clasp.next = clasp.prev = clasp;
	}
	
	public void add(Destructor d) {
	    assert (d != clasp && d.next == null && d.prev == null);
	    clasp.prev.next = d;
	    d.next = clasp;
	    d.prev = clasp.prev;
	    clasp.prev = d;
	}

	public void remove(Destructor d) {
	    assert (d != clasp);
	    d.prev.next = d.next;
	    d.next.prev = d.prev;
	    // We shouldn't need to null out these fields, so only do
	    // so when asserts are enabled.
	    assert ((d.next = d.prev = null) == null);
	}


        /** Walk all the destructors in this table */
	public void walk(DestructorWalker w) {
	    Destructor cur = clasp.next;
	    while (cur != clasp) {
		Destructor next = cur.next;
		w.walk(this, cur);
		cur = next;
	    }
	}

        /** Walk up to the maximum specified number of destructors in this
            table
            @param max the maximum number of destructors to walk, which
            should be less than or equal to the number of destructors 
            registered when this method is called.
        */
	public void walk(DestructorWalker w, int max) {
	    Destructor cur = clasp.next;
	    while (cur != clasp && max-- > 0) {
		Destructor next = cur.next;
		w.walk(this, cur);
		cur = next;
	    }
	}

    }

    public static abstract class DestructorWalker {
	public abstract void walk(DestructorTable t, Destructor d);
    }

    public class DestructorUpdater extends DestructorWalker {
	public void walk(DestructorTable t, Destructor d) {
	    MovingGC oop = (MovingGC) d.ref.asAnyOop();
	    if (oop.isForwarded()) {
		if (!DESTRUCTORS_SORTED_BY_AGE)
		    t.remove(d);
		d.ref = oop.getForwardAddress(); // FIXME: is this used by TheMan ?
		if (!DESTRUCTORS_SORTED_BY_AGE)
		    t.add(d);
	    } else if (!isLive(oop)) {
		t.remove(d);
		d.destroy(VM_Area.this);
	    }
	}
    }

    /**
     * Called by DestructorUpdater.walk to determine whether an object
     * that has not been forwarded is live.
     **/
    public boolean isLive(Oop oop) {
	return false;
    }

    private DestructorTable[] destructorTable;
    private int destructorCount[];

    public void setupDestructors() {
	setupDestructors(size());
    }

    public void setupDestructors(int addressRange) {
	destructorTable = new DestructorTable[Destructor.N_KINDS];
	// The first object allocated in the heap
	// Interpreter.breakpoint(destructorTable);
	for (int i = 0; i < destructorTable.length; i++)
	    destructorTable[i] = new DestructorTable(addressRange);
	destructorCount = new int[Destructor.N_KINDS];
    }

    /** Report how much memory is consumed when {@link #setupDestructors()} 
     *  is invoked.
     */
    public static int getSetupDestructorsMemUsage() {
        s3.core.domain.S3Domain dom = 
            (s3.core.domain.S3Domain)DomainDirectory.getExecutiveDomain();
        int size = (int)dom.sizeOfReferenceArray(Destructor.N_KINDS);
        size += (Destructor.N_KINDS * dom.sizeOfClassInstance("ovm/core/services/memory/VM_Area$DestructorTable"));
        size += (Destructor.N_KINDS * dom.sizeOfClassInstance("ovm/core/services/memory/VM_Area$DestructorTable$Clasp"));
        size += (int)dom.sizeOfPrimitiveArray(Destructor.N_KINDS, 'I');
        return size;
    }

   /**
    * Mark an unreachable object as live.  This method should only be
    * called from {@link Destructor#destroy}, the virtual function on
    * which all GC callbacks are defined.
    **/
    public Oop revive(Destructor d) {
	return d.ref.asOop();
    }
    
    /**
     * Register a destructor for an object in this area.<p>
     * Precondition:<code>areaOf(d.getReference()) == this</code><p>
     * Postcondition:<code>d.destroy()</code> will be called when
     * <code>d.getReference()</code> becomes otherwise unreachable.
     **/
    public void addDestructor(Destructor d) {
	destructorTable[d.getKind()].add(d);
	destructorCount[d.getKind()]++;
    }

    /**
     * Remove a destructor for an object in this area.<p>
     * Precondition:<code>areaOf(d.getReference()) == this</code><p>
     * Postcondition:<code>d.destroy()</code> will never be called.
     **/
    public void removeDestructor(Destructor d) {
	destructorTable[d.getKind()].remove(d);
	destructorCount[d.getKind()]--;
    }

    /**
     * Return the number of destructors of a particular kind
     * registered for objects in this area.  With the argument
     * {@link Destructor#ALL}, return the total number of destructors
     * registered in this area.
     **/
    public int destructorCount(int kind) {
	if (kind == Destructor.ALL) {
	    int ret = 0;
	    for (int i = 0; i < destructorCount.length; i++)
		ret += destructorCount[i];
	    return ret;
	} else
	    return destructorCount[kind];
    }

    /**
     * Iterate over destructors of a particular kind registered for
     * objects in this area.
     **/
    public void walkDestructors(int kind, DestructorWalker walker) {
	if (kind == Destructor.ALL)
	    for (int i = 0; i < destructorTable.length; i++)
		destructorTable[i].walk(walker);
	else
	    destructorTable[kind].walk(walker);
    }

    /**
     * Iterate over a specified number of destructors of a particular kind 
     * registered for objects in this area.
     * @param max the maximum number of destructors to walk, which
     * should be less than or equal to the number of destructors 
     * registered when this method is called.
     **/
    public void walkDestructors(int kind, DestructorWalker walker, int max) {
	if (kind == Destructor.ALL)
	    for (int i = 0; i < destructorTable.length; i++)
		destructorTable[i].walk(walker, max);
	else
	    destructorTable[kind].walk(walker, max);
    }
}

