package s3.services.memory.mostlyCopying;

import ovm.core.Executive;
import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Oop;
import ovm.core.domain.RealtimeJavaDomain;
import ovm.core.execution.Context;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.execution.Native;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.AtomicOps;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.MovingGC;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.ScopedMemoryContext;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.memory.VM_Word;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.OVMThreadContext;
import ovm.services.bytecode.JVMConstants.Throwables;
import ovm.services.memory.FinalizableArea;
import ovm.services.realtime.NoHeapRealtimeOVMThread;
import ovm.services.memory.scopes.VM_ScopedArea;
import ovm.services.memory.scopes.PrimordialScope;
import ovm.util.CommandLine;
import ovm.util.Iterator;
import ovm.util.Mem;
import ovm.util.OVMError;
import s3.core.domain.S3Domain;
import s3.util.PragmaAtomic;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import ovm.core.stitcher.InvisibleStitcher.Component;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;

/**
 * This region manager divides memory into scoped, heap and immortal, so
 * that allocation of scopes is independent of actions on the heap. This
 * allows no-heap threads to create/destroy scopes regardless of what may
 * be happening in the heap - ie when GC is in progress.
 * <p>
 * <b>Note:</b> enabling concurrent execution of no-heaps thread with the
 * GC is still a work-in-progress. There is a tight coupling between this
 * memory manager and the threading subsystem to allow this to occur.
 *
 * <h3>Exclusion Control and Atomicity</h3>
 * <p>The key requirement is that no-heap thread should not be delayed by GC
 * when not directly synchronizing with heap using entities. This means that
 * GC can not be globally atomic (or put another way: stop-the-world-GC has to
 * become stop-the-heap-using-world-GC). The current policy for exclusion 
 * control is as follows:
 * <ul>
 * <li>As immortal memory is never GC'ed and allocation is constant time,
 * we simply make <tt>immortal.getMem</tt> PragmaAtomic</li>
 * <li>Similarly, creation of scoped areas and transient areas, through
 * {@link #makeScopedArea} and {@link #makeExplicitArea}, are also PragmaAtomic
 * </li>
 * <li>Each scoped area has its <tt>getMem</tt> method marked PragmaAtomic</li>
 * <li>A TransientArea is expected to be primarily thread-local and so it does
 * not enforce any exclusion
 * <li>We specialise the heap-area class so that its <tt>getMem</tt> method
 * is PragmaAtomic.</li>
 * </ul>
 * This policy will be updated as we allow GC preemption to occur
 * @author Jason Baker
 * @author David Holmes (scope tree and related scope support)
 */
public class SplitRegionManager extends Manager implements Component {

    /** A quick &amp; easy way to access the alignment. */
    static final int ALIGN = VM_Address.widthInBytes();
    
    /**
     * If false, allocate memory the first time a region is entered
     * and free it when the region is reset.  Otherwise, allocate
     * memory when a region is allocated and free it when it becomes
     * unreachable.
     * <p>
     * NOTE: since we don't detect when a region become
     * unreachable, regions are never implicitly destroyed.  When this
     * variable is set, regions aren't explicitly destroyed in reset()
     * either.  This represents a memory leak.
     **/
    static final boolean RETAIN_REGION_MEMORY = true;

    /**
     * If true, emit checks in read/write barriers.
     **/
    final boolean DO_CHECKS;


    /** Indicates whether a load check must occur. This is set whenever we
        context switch to a no-heap thread and cleared otherwise
    */
    boolean doLoadCheck = false;

    /*
     * Immortal and heap memory are distinct.  We have to statically
     * partition memory into heap and immortal regions to avoid GC for
     * immortal allocation.  We might as well partition the address
     * space too, and treat immortal just like the bootimage.
     */
    VM_Address immortalBase;
    int immortalSize;
    int immortalOffset = 0;

    /*
     * Scoped memory and heap are also distinct. You must be able to create,
     * use and destroy scopes while GC is in progress in the heap.
     */
    VM_Address scopeBase;
    VM_Word scopeBlocks;
    int scopeBaseIndex;
    int scopeSize;

    /**
     * Indexed by block numbers.  For each block in an allocated area,
     * this array contains a pointer to the area object.  The area
     * object is at offset zero of the area's first block.<p>
     *
     * Ranges of free blocks are represented by a doubly-linked
     * freelist of {@link FreeArea} objects.  As with allocated areas,
     * these objects are placed at the start of the corresponding
     * memory range.  However, entries in the scopeOwner array are
     * only maintained for the first and last block in a free range.<p>
     *
     * This array is mostly used for write-barrier checks.  It could
     * also be used to speed up scope free operations:  Rather than
     * searching forward from firstFree in the linked list to find a
     * FreeArea's prev pointer, we can search backward from it's index
     * in scopeOwner.  However, this is not implemented.
     */
    TransientArea[] scopeOwner;

    /**
     * The first free memory area in address order.
     **/
    FreeArea firstFree;

    public SplitRegionManager(String heapSize,
			      String immortalSize,
			      String scopeSize,
			      String disableChecks)
    {
	super(heapSize);
	this.immortalSize = CommandLine.parseSize(immortalSize);
	this.scopeSize = CommandLine.parseSize(scopeSize);
	scopeOwner = new TransientArea[this.scopeSize >> blockShift];
	
	DO_CHECKS = (disableChecks == null || disableChecks.equals("false"));
        // ovm.core.services.io.BasicIO.out.println("$$$$ Region checks enabled: " + DO_CHECKS);
    }

    public void initialize() {
	// This check is currently not needed since the manager and
	// context are specified in the same stitcher file.
	if (!(Context.factory() instanceof ScopedMemoryContext.Factory))
	    throw new MisconfiguredException("realtime java memory manager "+
					     "configured without threading "+
					     "support");
    }
	
    public void boot(boolean useImageBarrier) throws PragmaNoBarriers {
	super.boot(useImageBarrier);
	immortalSize = (immortalSize + PAGE_SIZE - 1) & ~(PAGE_SIZE - 1);
	immortalBase = Native.mmap(null, immortalSize,
				   PROT_READ|PROT_WRITE,
				   MAP_ANON|MAP_PRIVATE, -1, 0);
	if (immortalBase.asInt() == -1)
	    Native.abort();
	scopeBase = Native.mmap(null, scopeSize,
				PROT_READ|PROT_WRITE,
				MAP_ANON|MAP_PRIVATE, -1, 0);
	if (scopeBase.asInt() == -1)
	    Native.abort();
	scopeBaseIndex = scopeBase.asInt() >>> blockShift;
	scopeBlocks = VM_Word.fromInt(scopeSize >>> blockShift);

	VM_Area r = setCurrentArea(placeNew.at(scopeBase));
	try {
	    new FreeArea(scopeSize);
	} finally {
	    setCurrentArea(r);
	}
        initPrimordial();
    }

    public Error throwOOM() {
        CoreServicesAccess csa = DomainDirectory.getExecutiveDomain()
                                                .getCoreServicesAccess();
        csa.generateThrowable(Throwables.OUT_OF_MEMORY_ERROR, 0);
        // Return statement unreachable
        return null;
    }


    // can't statically initialize these values as the blueprints needed don't
    // exist when this class is initialized, nor at boot() time. So we defer 
    // to after fully booted. However that means these values are uninitialized
    // the first time they are used - for the primordial scope and initial
    // scratchpad.
    static int transientSize;
    static int scopedSize;

    static void initSizes() {
        transientSize = 
            S3Domain.sizeOfInstance("s3/services/memory/mostlyCopying/SplitRegionManager$TransientArea");

        int areaSize =  S3Domain.sizeOfInstance("s3/services/memory/mostlyCopying/SplitRegionManager$ScopedArea");

        // for scopes: determine what memory is used by setupDestructors
        int destructorSize = VM_Area.getSetupDestructorsMemUsage();

        scopedSize = areaSize + destructorSize;

        scopedSize += 12;  // somehow the calculations are missing something
                           // so we pad based on empirical observation. Of
                           // course with a different object model the pad may
                           // need to change

        if (false) {
            Native.print_string("Calculated size of TransientArea = ");
            Native.print_int(transientSize);
            Native.print_string("\nRaw size of ScopedArea = ");
            Native.print_int(areaSize);
            Native.print_string("\nDestructor setup used ");
            Native.print_int(destructorSize);
            Native.print_string("\nCalculated size of ScopedArea = ");
            Native.print_int(scopedSize);
            Native.print_string("\n");
        }
    }

    public void fullyBootedVM() {
        super.fullyBootedVM();
        initSizes();
    }


    /**
     * Base class for our regions. This adds the range values that are to
     * perform the store barrier checks.
     */
    static abstract class Area extends VM_ScopedArea {
	public abstract VM_Address getMem(int size);
	protected abstract VM_Address getBaseAddress();
	
	Area(PrimordialScope primordial) {
	    super(primordial);
	}
	
	Area() {}
	
	public boolean isScope() { return false; }
    }


    /**
     * A memory area that implements placement new.
     * @see SplitRegionManager#placeNew
     **/
    static class PlacementArea extends Area {
	public int size() { return Integer.MAX_VALUE; }
	public int memoryConsumed() { return 0; }
	
	VM_Address place;
	/**
	 * Return the address into which this area allocates, and
	 * reset that address to null
	 **/
	public VM_Address getMem(int size) {
	    VM_Address ret = place;
	    place = ret.add(size);
	    Mem.the().zero(ret, size);
	    return ret;
	}

	public VM_Address getBaseAddress() {
	    throw Executive.panic("unsupported");
	}

	/**
	 * Set the address into which this area allocates and return
	 * <code>this</code>.
	 **/
	public VM_Area at(VM_Address addr) {
	    place = addr;
	    return this;
	}

	/**
	 * Return the next location in memory that this object will
	 * allocate from
	 **/
	public VM_Address next() { return place; }
    }

    /**
     * This field is used to implement placement new.  Typical useage
     * is as follows:
     * <code><pre>
     * VM_Area r = setCurrentArea(placeNew.at(<i>address</i>));
     * try {
     *    new <i>C</i>(); // allocate a C instance at address
     * } finally {
     *    setCurrentArea(r);
     * }
     * </pre></code>
     *
     * Note: the C constructor must not perform allocation in the current
     * memory area.<p>
     * 
     * Note further: {@link SplitRegionManager.PlacementArea} is not
     * reentrant.
     **/
    final PlacementArea placeNew = new PlacementArea();
	
    /*
     * Allocate immortal memory.  We never have to zero it, since it
     * is never reused.
     */
    final VM_Area immortalArea = new Area() {
	    boolean dumpedOnce = false;

	    // Ignore destructors in the immortal area
	    public void addDestructor(Destructor _) { }
	    public void removeDestructor(Destructor _) { }
	    public int destructorCount(int _) { return 0; }

	    public VM_Address getMem(int size) throws PragmaAtomic {
		if (size + immortalOffset < immortalSize) {
		    VM_Address ret = immortalBase.add(immortalOffset);
		    immortalOffset += size;
		    return ret;
		} else {
		    if (dumpOOM && !dumpedOnce) {
			VM_Area r = setCurrentArea(heapArea);
                        boolean lc = doLoadCheck;
                        doLoadCheck = false;
			try {
			    BasicIO.out.println("immortal space exhausted");
                            //new Error().printStackTrace(); // show where we are
			    dumpExtent(immortalBase,
				       immortalBase.add(immortalOffset));
			    dumpedOnce = true;
			} finally { 
                            setCurrentArea(r); 
                            doLoadCheck = lc;
                        }
		    }
		    throw throwOOM();
		}
	    }

	    public int size() {	return immortalSize; }
	    public int memoryConsumed() { return immortalOffset; }
            public String toString() throws PragmaNoPollcheck { 
                return "Immortal Area"; 
            }
	    public VM_Address getBaseAddress() {
		return immortalBase;
	    }
	};


    /** Factory method to allow specialization of the heap area 
        NOTE: this is called prior to initialization of this subclass
        instance.
     */
    protected VM_Area makeHeapArea() {
        return new HeapArea();
    }

    /**
     * Duplicate Manager.HeapArea functionality, but in a subclass of
     * VM_ScopedArea.  Also, define getBaseAddress() and getMem(),
     * methods that are used in this implementation of scopes.
     **/
    protected class HeapArea extends Area {
        public int size() { return gcThreshold << blockShift; }
        public int memoryConsumed() { return allocated << blockShift; }
        public boolean isLive(Oop oop) {
            return !inFromSpace(VM_Address.fromObject(oop));
        }
        
        public Oop revive(Destructor d) {
            if (inGC) {
                Oop doop = VM_Address.fromObject(d).asOop();
                copier.updateLoc(destructorRef.addressWithin(doop));
            }
            return super.revive(d);
        }
        public String toString()throws PragmaNoPollcheck { 
            return "Split-Region-Heap Area";
	}

	public VM_Address getBaseAddress() {
	    return heapBase;
	}
        public VM_Address getMem(int size) throws PragmaAtomic {
            return getHeapMem(size, true);
        }
    }

    /**
     * Transient areas are special purpose temporary allocation regions
     * used by the OVM internally - both in the ED and the UD. Stores into
     * a plain transient area are not checked (there is no way to determine the
     * validity of such a store as the transient region doesn't form part of
     * the region hierarchy. Subclasses override this.
     */
    class TransientArea extends Area {
        int rsize;
	int offset;
	final int baseOffset;

	/**
	 * This method is called before this memory area's base offset
	 * is set.  Any memory allocated in init() will survive calls
	 * to reset.
         * <p><b>WARNING:</b> if you override this, remember it is called
         * during the execution of the TransientArea constructor, prior to
         * the actual subclass constructor executing.
	 */
	protected void init() { }

	TransientArea(int size) throws PragmaNoBarriers {
	    super(primordial);
	    unchecked=true;
	    setRange((short) 0, Short.MAX_VALUE);
	    mirror = null;
	    rsize = size;
	    int idx = idx();
	    int i = idx + (rsize >> blockShift);
	    for (;i-- > idx;)
		scopeOwner[i] = this;
	    
	    init();
	    // Allocation is finished, mark unused space with null.
	    offset = baseOffset = placeNew.next().diff(base()).asInt();
//             Native.print_string("Transient area constructed: offset = ");
//             Native.print_int(offset);
//             Native.print_string("\n");
	    reset();
	}

	TransientArea() throws PragmaNoBarriers {
	    super(primordial);
	    baseOffset = 0;
	}

	VM_Address base() throws PragmaInline {
	    return VM_Address.fromObject(this);
	}
	int idx() throws PragmaInline {
	    return base().diff(scopeBase).asInt() >>> blockShift;
	}

	public VM_Address getBaseAddress() {
	    return base();
	}

        public String toString() { return "area@" + hashCode(); }

        // can we assume that transient areas are always used by only a
        // single thread? That is true so far as I know - DH
	public VM_Address getMem(int size) {
	    if (offset + size > rsize) {
                try {
                    if (dumpOOM) {
                        boolean lc = doLoadCheck;
                        doLoadCheck = false;
                        VM_Area r = setCurrentArea(heapArea);
                        try {
                            BasicIO.out.print(this + ": transient area exhausted "
                                              +"("+offset+" of "+rsize+" used) ");
                            Oop mirror = getMirror();
                            int mnum = (mirror == null ? 0 : mirror.getHash());
                            BasicIO.out.println("mirror @" +
                                                Integer.toHexString(mnum));
                            new Error().printStackTrace(); // show where we are
                            dumpExtent(base(), base().add(offset));
                        }
                        finally {
                            setCurrentArea(r);
                            doLoadCheck = lc;
                        }
                    }
                }
                finally {
                    throwOOM();
                }
	    }

	    VM_Address ret = base().add(offset);
	    offset += size;

            // we zero here to get more predictable behavior.
            // note: must zero out the word following this object.  but
            // the first word of this object is already zeroed.  hence
            // this weird expression.
	    //
	    // Note: it may appear tempting to zero out a scope on
	    // per-block basis (as we do with the heap).  However,
	    // this will not work with the scratchpad, because we
	    // constantly discard the top few scratchpad objects, and
	    // allocate over them.
            Mem.the().zero(ret.add(ALIGN),offset==rsize?size-ALIGN:size);
	    
            return ret;
	}

	public int size() { return rsize; }

	public int memoryConsumed() { return offset; }

	public void reset() {
	    offset = baseOffset;
	    base().add(offset).setAddress(null);
	}

	public void reset(int newOffset) {
	    assert(0 <= newOffset && newOffset <= offset &&
		   newOffset >= base().asOop().getBlueprint().getFixedSize());
	    offset = newOffset;
	    base().add(offset).setAddress(null);
	}

	public void destroy() throws PragmaAtomic {
	    int size = rsize;
	    // tricky: be sure do load this$0 before zeroing out the
	    // fields of the FreeArea object we are allocating.
	    SplitRegionManager copyThis = SplitRegionManager.this;
	    VM_Area r = setCurrentArea(placeNew.at(base()));
	    try { copyThis.new FreeArea(size); }
	    finally { setCurrentArea(r); }
	}
    }

    static final boolean DEBUG_FREELIST = false;

    /**
     * Provides our free-list management of the scope area.
     */
    class FreeArea extends TransientArea {
	FreeArea prev;
	FreeArea next;

	void setFirst() throws PragmaNoBarriers {
	    scopeOwner[idx()] = this;
	}
	void setLast() throws PragmaNoBarriers {
	    scopeOwner[idx() + (rsize >> blockShift) - 1] = this;
	}

	/**
	 * Attach this FreeArea to the list.  Coalesce with next and
	 * previous if possible.
	 **/
	FreeArea(int size) throws PragmaNoBarriers {
            if (DEBUG_FREELIST) {
                Native.print_string("\nFreeing area - base: ");
                Native.print_hex_int(base().asInt());
                Native.print_string(" to: ");
                Native.print_hex_int(base().add(size).asInt());
                Native.print_string(" (");
                Native.print_hex_int(size);
                Native.print_string(")\n");
                Native.print_string("FreeArea (size) >>>\n");
                dump();
            }

	    FreeArea prev = null;
	    FreeArea next = firstFree;
	    while (next != null && next.base().uLT(base())) {
		prev = next;
		next = next.next;
	    }
            if (DEBUG_FREELIST) {
                Native.print_string("Slotting in before: ");
                if (next == null)
                    Native.print_string("\b\b\b\b\b\b\b\b new freehead");
                else
                    Native.print_hex_int(next.base().asInt());
                Native.print_string("\n");
            }

	    if (prev != null && prev.base().add(prev.rsize) == base()) {
		// Coalesce this with prev, then try to coalesce prev
		// with next;
		prev.rsize += size;
                if (DEBUG_FREELIST)
                    Native.print_string("coalesced with prev\n");
	    } else {
		// set prev pointer of this, and try to coalesce this
		// with next
		setRange(Short.MAX_VALUE, (short) 0);
		rsize = size;
		setFirst();
		if (prev == null) {
		    firstFree = this;
		    this.prev = null;
		} else {
			prev.next = this;
			this.prev = prev;
		}
		prev = this;
	    }

	    if (next != null && prev.base().add(prev.rsize) == next.base()) {
                if (DEBUG_FREELIST)
                    Native.print_string("coalescing with next\n");
		prev.rsize += next.rsize;
		prev.next = next.next;
		if (prev.next != null)
		    prev.next.prev = prev;
	    } else {
		prev.next = next;
		if (next != null)
		    next.prev = prev;
	    }
	    prev.setLast();

            if (DEBUG_FREELIST) {
                Native.print_string("FreeArea (size) <<<\n");
                dump();
            }

	}

	/**
	 * Create a smaller FreeArea from the second portion of a
	 * larger FreeArea (oldStart).
	 **/
	FreeArea(FreeArea oldStart, int sizeDelta) throws PragmaNoBarriers {
            if (DEBUG_FREELIST) {
                Native.print_string("FreeArea (old, deta) >>>\n");
                dump();
            }

	    unchecked = false;
	    setRange(Short.MAX_VALUE, (short) 0);
	    prev = oldStart.prev;
            if (prev != null)
                prev.next = this;
	    next = oldStart.next;
            if (next != null)
                next.prev = this;
	    rsize = oldStart.rsize - sizeDelta;
	    setFirst();
	    setLast();
	    if (prev == null)
		firstFree = this;

            if (DEBUG_FREELIST) {
                Native.print_string("FreeArea (old, deta) <<<\n");
                dump();
            }
	}


        // debugging use only
        void dump() {
            FreeArea head = firstFree;
            if (head == null) {
                Native.print_string("firstFree == null\n");
            }
            else {
                Native.print_string("firstFree = ");
                Native.print_hex_int(firstFree.base().asInt());
                Native.print_string("\n");

                int i = 0;
                for (FreeArea fa = firstFree; fa != null; fa = fa.next, i++) {
                    Native.print_string(spaces[i]);
                    Native.print_string("Current = ");
                    Native.print_hex_int(fa.base().asInt());
                    Native.print_string("\n");
                    Native.print_string(spaces[i]);
                    Native.print_string("next = ");
                    Native.print_hex_int(fa.next != null ? fa.next.base().asInt() : 0);
                    Native.print_string("\n");
                    Native.print_string(spaces[i]);
                    Native.print_string("prev = ");
                    Native.print_hex_int(fa.prev != null ? fa.prev.base().asInt() : 0);
                    Native.print_string("\n");
                }
            }
        }
    }

    // debugging use only
    static String[] spaces = new String[] {
            "",
            " ",
            "  ",
            "   ",
            "    ",
            "     ",
        };


    /** The primordial scope */
    PrimordialScope primordial;


    // invoked by boot()
    void initPrimordial() throws PragmaNoBarriers {
	primordial = new PrimordialScope(true);
    }

    /** Default name given to a scope if it can't allocate its own name.
        Only used for debugging
    */
    static final String DEFAULT_SCOPE_NAME = "scope";

    /**
     * FIXME: The scope checks belong on the ovm side!
     * 
     * ScopedArea represents a RTSJ ScopedMemory object. 
     * @author Filip Pizlo (original UD version of scopetree management)
     * @author David Holmes (finalization and ED version)
     */
    public class ScopedArea extends TransientArea implements FinalizableArea {

        // NOTE: we're not allowed to do allocation in the constructor
        //       It is done in init() instead
	ScopedArea(int size, Oop mirror) {
	    super(size);
	    this.mirror = mirror;
            this.unchecked = false; 
	}
	
	public boolean isScope() { return true; }


        /** 
         * Initialization method called during construction when this is the
         * current allocation context
         * <p><b>WARNING:</b> this method is called by the super constructor
         * before our own constructor is executed. Consequently  the reference
         * to our enclosing SplitRegionManager instance has not been set, and
         * so this method can not directly, or indirectly, use that reference
         * or any other local fields.
         */
	protected void init() {
	    setupDestructors();
	}

        // note: we don't pre-allocate this as it takes too much memory.
        // We also don't allocate it in this area because the most likely
        // reason this is called is during dump-on-out-of-memory processing.
        public String toString() { 
            return "scope@" + hashCode();
        }

        /**
         * Invokes the finalization methods for all user-domain objects 
         * currently allocated in this memory area.
         * @return true if there are more finalizable objects to be
         * finalized (ie a finalizer has created a new finalizable object);
         * and false otherwise.
         */
        public boolean runFinalizers() {
            final int initialCount = destructorCount(Destructor.ALL);
//             ovm.core.execution.Native.print_string("initial finalizer count ");
//             ovm.core.execution.Native.print_int(initialCount);
//             ovm.core.execution.Native.print_string("\n");
            if (initialCount == 0)
                return false;
	    final VM_Address end = base().add(offset);
	    Object r = MemoryPolicy.the().enterScratchPadArea();
	    try {
		walkDestructors(Destructor.ALL, new DestructorWalker() {
		    public void walk(DestructorTable dt, Destructor d) {
			VM_Address addr = VM_Address.fromObject(revive(d));
			removeDestructor(d);
			d.destroy(ScopedArea.this);
		    }
		    }, initialCount);
	    }
	    finally {
		MemoryPolicy.the().leave(r);
	    }
//             ovm.core.execution.Native.print_string("final finalizer count ");
//             ovm.core.execution.Native.print_int(destructorCount(Destructor.ALL));
//             ovm.core.execution.Native.print_string("\n");

	    return destructorCount(Destructor.ALL) != 0;
        }


	public VM_Address getMem(int size) throws PragmaAtomic {
            return super.getMem(size);
        }

	public void reset() {
	    assert(destructorCount(Destructor.ALL) == 0);
	    super.reset();
	}
    }  // end ScopedArea
    
    // back to the SplitRegionManager methods

    /** Allocates a block of memory of the requested size from the
        current allocation context
    */
    public VM_Address getMem(int size) {
	Area a = (Area) ScopedMemoryContext.getMemoryContext();
        VM_Address mem = a.getMem(size);
        return mem;
    }

    public Oop clone(Oop oop) {
	Oop ret = super.clone(oop);
	if (DO_CHECKS) {
	    Blueprint bp = ret.getBlueprint();
	    CoreServicesAccess csa =
		bp.getDomain().getCoreServicesAccess();
	    if (bp.isArray()) {
		Blueprint.Array abp = bp.asArray();
		if (abp.getComponentBlueprint().isReference()) {
		    int off = abp.byteOffset(0);
		    for (int len = abp.getLength(oop); len --> 0; ) {
			VM_Address addr = VM_Address.fromObject(oop);
			Oop tgt = addr.add(off).getAddress().asOop();
			storeCheck(csa, ret, off, tgt);
			off += VM_Address.widthInBytes();
		    }
		}
	    } else {
		int[] refMap = bp.getRefMap();
		for (int i = 0; i < refMap.length; i++) {
		    VM_Address addr = VM_Address.fromObject(oop);
		    Oop tgt = addr.add(refMap[i]).getAddress().asOop();
		    storeCheck(csa, ret, refMap[i], tgt);
		}
	    }
	}
	return ret;
    }
	    
	
    /**
     * Account for NHRT weirdness when scanning thread stacks and heap
     * objects.  Allow NHRTs to be added and removed from the system
     * while heap-using thread stacks are walked.
     **/
    protected ConservativeUpdater makeCopier() {
	return new Updater() {
		protected Context getNext(Iterator it) throws PragmaAtomic {
		    while (it.hasNext()) {
			Context ret = (Context) it.next();
			// FIXME: When is a Context NOT an OVMThreadContext?
			if (!(ret instanceof OVMThreadContext)
			    || !(((OVMThreadContext) ret).getThread()
				 instanceof NoHeapRealtimeOVMThread))
			    return ret;
		    }
		    return null;
		}
	    };
    }

    /**
     * This object is used to walk the fields of non-moving root
     * objects in the image, immortal area and scopes.  It uses a
     * compare-and-swap to allow an NHRT to overwrite heap references
     * with non-heap references.
     **/
    private final ExtentUpdater rootScanner = new Updater() {
	    public void updateLoc(VM_Address loc) {
		MovingGC oop = (MovingGC) loc.getAddress().asAnyOop();
		if (needsUpdate(oop)) {
		    Oop newVal = (oop.isForwarded()
				  ? oop.getForwardAddress().asOop()
				  : updateReference(oop));
		    AtomicOps.attemptUpdate
			(loc,
			 VM_Address.fromObject(oop).asWord(),
			 VM_Address.fromObject(newVal).asWord());
		}
	    }
	};

    private final BlockWalker rootBlockScanner = new BlockWalker(rootScanner);

    /**
     * Account for NHRT weirdness walking root objects in the
     * bootimage, immortal and scopes.  See {@link #rootScanner}.
     **/
    protected BlockWalker getRootWalker() { return rootBlockScanner; }

    void markPreciseRoots() {
	super.markPreciseRoots();
	rootScanner.walk(immortalBase, immortalBase.add(immortalOffset));
	for (int i = 0; i < scopeOwner.length; ) {
	    TransientArea a = scopeOwner[i];
	    if (!(a instanceof FreeArea))
		rootScanner.walk(a.base(), a.base().add(a.offset));
	    i += (a.rsize >> blockShift);
	}
    }

    public Object makeMemoryContext() {
	return heapArea;
    }
    public VM_Area getHeapArea() {
	return heapArea;
    }
    public VM_Area getImmortalArea() {
	return immortalArea;
    }
    public VM_Area getCurrentArea() {
	return (VM_Area) ScopedMemoryContext.getMemoryContext();
    }
    public VM_Area setCurrentArea(VM_Area area) {
	VM_Area ret = (VM_Area) ScopedMemoryContext.getMemoryContext();
	ScopedMemoryContext.setMemoryContext(area);
	return ret;
    }

    /**
     * Remove a chunk of memory from the freelist, and return it's
     * base pointer.
     **/
    private VM_Address firstFit(int size) throws PragmaNoBarriers {
        if (DEBUG_FREELIST) {
            Native.print_string("firstfit(");
            Native.print_hex_int(size);
            Native.print_string(") start\n");
            Native.print_string("firstfree = ");
            Native.print_hex_int(firstFree != null ? firstFree.base().asInt() : 0);
            Native.print_string("\n");
        }
	FreeArea fr = firstFree;
	while (fr != null && fr.rsize < size)
	    fr = fr.next;
	if (fr == null)
	    throwOOM();
	VM_Address ret = fr.base();
	if (fr.rsize > size) {
            if (DEBUG_FREELIST)
                Native.print_string("splitting free area\n");
	    VM_Address newStart = ret.add(size);
	    VM_Area r = setCurrentArea(placeNew.at(newStart));
	    try {
		new FreeArea(fr, size);
	    } finally {
		setCurrentArea(r);
	    }
	} else {
            if (DEBUG_FREELIST)
                Native.print_string("Not splitting free area\n");
	    if (fr.prev == null) {
                if (DEBUG_FREELIST)
                    Native.print_string("prev was null - setting firstFree\n");
		firstFree = fr.next;
            }
	    else {
		fr.prev.next = fr.next;
            }
	    if (fr.next != null) {
		fr.next.prev = fr.prev;
            }

            if (DEBUG_FREELIST)
                fr.dump();
	}

        if (DEBUG_FREELIST) {
            Native.print_string("firstfree = ");
            Native.print_hex_int(firstFree != null ? firstFree.base().asInt() : 0);
            Native.print_string("\n");
            Native.print_string("firstfit(");
            Native.print_hex_int(size);
            Native.print_string(") returning ");
            Native.print_hex_int(ret.asInt());
            Native.print_string(" to ");
            Native.print_hex_int(ret.add(size).asInt());
            Native.print_string("\n");
        }
	return ret;
    }

    public void freeArea(VM_Area _area) {
	TransientArea area = (TransientArea) _area;
	int rsize = area.rsize;
	VM_Address raw = VM_Address.fromObject(area);
	VM_Area r = setCurrentArea(placeNew.at(raw));
	try {
	    new FreeArea(rsize);
	} finally {
	    setCurrentArea(r);
	}
    }

    /**
     * Create a memory area that is not subject to scope checks.<p>
     *
     * If this method is called at image build time, it will return
     * null.  Areas created through this method at image build time
     * are handled just like areas returned from
     * {@link ovm.core.services.memory.StandaloneMemoryPolicy} at build
     * time.
     *
     * @param size the size of the area. The actual size of the area is
     * always a multiple of the block size, and is expanded to include the
     * memory consumed by the area object itself.
     *
     * @return a new memory area
     *
     * @throws OutOfMemoryError if the area can't be allocated.
     *
     */
    public VM_Area makeExplicitArea(int size) throws PragmaAtomic {
	if (scopeBase == null)
	    return null;
	
        //Native.print_string("\nAllocating transientArea\n");
	int rsize = (size + transientSize + blockMask) & ~blockMask;
	VM_Address raw = firstFit(rsize);
	VM_Area r = setCurrentArea(placeNew.at(raw));
	try {
            VM_Area ret = new TransientArea(rsize);
            // First time called is prior to fullyBooted so transientSize==0
	    assert transientSize == 0 || ret.memoryRemaining() >= size:
		"available space too small: " +
		ret.memoryRemaining() + " vs. " + size;
            return ret;
	} finally {
	    setCurrentArea(r);
	}
    }

    /**
     * Create a scoped memory area.
     *
     * @param mirror the user-domain <tt>ScopedMemory</tt> object associated
     * with this scoped area.
     *
     * @param size the size of the area. The actual size of the area is
     * always a multiple of the block size, and is expanded to include the
     * memory consumed by the area object itself.
     *
     * @return a new memory area
     *
     * @throws OutOfMemoryError if the area can't be allocated.
     *
     */
    public VM_ScopedArea makeScopedArea(Oop mirror, int size)
	throws PragmaAtomic {
        //Native.print_string("\nAllocating scopeArea\n");
	int rsize = (size + scopedSize + blockMask) & ~blockMask;
	VM_Address raw = firstFit(rsize);
	VM_Area r = setCurrentArea(placeNew.at(raw));
	try {
            VM_ScopedArea ret = new ScopedArea(rsize, mirror);
            // First time called is prior to fullyBooted so scopedSize==0
	    assert scopedSize == 0 || ret.memoryRemaining() >= size:
		"available space too small: " +
		ret.memoryRemaining() + " vs. " + size;
            return ret;
	} finally {
	    setCurrentArea(r);
	}
    }

    VM_Area areaOf(Object obj) {
        return areaOf(VM_Address.fromObject(obj).asOop());
    }
    
    public VM_Area areaOf(Oop mem) {
	VM_Word off = VM_Address.fromObject(mem).diff(heapBase);
	if (off.uLT(VM_Word.fromInt(heapSize)))
	    return heapArea;
	off = VM_Address.fromObject(mem).diff(scopeBase);
	if (!off.uLT(VM_Word.fromInt(scopeSize)))
	    return immortalArea;
	int idx = off.asInt() >>> blockShift;
	return scopeOwner[idx];
    }

    public void doGC() {
	// GC actually allocates some strings for its messages, be
	// sure not to allocate them in a scoped area
	VM_Area r = setCurrentArea(heapArea);
	try { super.doGC(); }
	finally { setCurrentArea(r); }
    }
	    
    public boolean needsWriteBarrier() {
        return DO_CHECKS;
    }

    public boolean needsReadBarrier() {
        return DO_CHECKS;
    }

    public boolean supportsDestructors() {
	return false;
    }

    /**
     * Slow path for heap store checks.  Takes addresses that have
     * already been shifted by blockShift and do not fall on the same
     * block.
     * @param csa used to single error
     * @param sb  block pointed from (source)
     * @param tb  block pointed to (target)
     */
    void storeCheckSlow(CoreServicesAccess csa, int sb, int tb) 
        throws PragmaNoPollcheck, PragmaNoBarriers
    {
	VM_Word tidx = VM_Word.fromInt(tb - scopeBaseIndex);
	if (tidx.uLT(scopeBlocks)) {
	    // target is in scope, it must outlive source
            Area ta = scopeOwner[tidx.asInt()];
	    VM_Word sidx = VM_Word.fromInt(sb - scopeBaseIndex);
	    if (sidx.uLT(scopeBlocks)) {
		// both in a scope
		Area sa = scopeOwner[sidx.asInt()];
		// sa must be the same as, or a strict descendent of, ta
		if (sa != ta && !sa.canPointTo(ta)) {
                    RealtimeJavaDomain dom =
			(RealtimeJavaDomain) csa.getDomain();
                    dom.storeBarrierFailed();
                }
	    } else {
		// It is OK to store scratchpad pointers in heap/immortal,
		// but not ScopedMemory pointers
                if (ta.isScope()) {
                    RealtimeJavaDomain dom =
                        (RealtimeJavaDomain) csa.getDomain();
                    dom.storeBarrierFailed();
                }
	    }
	}
    }

    /**
     * Perform region store check, and raise the appropriate exception
     * when a store is bogus.
     *
     * @param csa used to signal error
     * @param src object being updated
     * @param tgt new field/element value
     **/
    void storeCheck(CoreServicesAccess csa, Oop src, int offset, Oop tgt) 
        throws PragmaNoPollcheck, PragmaNoBarriers
    {
	int sb = VM_Address.fromObject(src).asInt() >>> blockShift;
	int tb = VM_Address.fromObject(tgt).asInt() >>> blockShift;
	if (sb != tb)
	    storeCheckSlow(csa, sb, tb);
	VM_Address addr = VM_Address.fromObject(src).add(offset);
	addr.setAddress(VM_Address.fromObject(tgt));
    }

    
    /**
     * Called immediately after a context switch to a new thread. We use this
     * hook to determine if the new thread requires load checks to be performed
     * and update the flag accordingly
     */
    public void observeContextSwitch(Object context) throws PragmaNoPollcheck,
                                                            PragmaNoBarriers{
        OVMThread thread = null;
        if (context instanceof OVMThreadContext) {
            thread = ((OVMThreadContext) context).getThread();
        }
        else if (context instanceof OVMThread) {
            thread = (OVMThread) context;
        }
        doLoadCheck = (thread instanceof NoHeapRealtimeOVMThread &&
                       ((NoHeapRealtimeOVMThread)thread).heapChecksEnabled());

        // debug tracing
        if (false) {
            if (doLoadCheck) {
                Native.print_string("\nTurning on heap checks\n");
            }
            else {
                Native.print_string("\nTurning off heap checks\n");
            }
        }
    }

    
    static final boolean DEBUG_HEAP_CHECKS = false;

    public boolean readBarriersEnabled() {
	return doLoadCheck;
    }

    public void enableReadBarriers(boolean enabled) {
        doLoadCheck = enabled;
    }

    public void readBarrier(CoreServicesAccess csa, Oop src) 
        throws PragmaInline, PragmaNoBarriers
    {
	if (doLoadCheck &&
	    VM_Address.fromObject(src).diff(heapBase).
               uLT(VM_Word.fromInt(heapSize)))
	{
	    if (DEBUG_HEAP_CHECKS) 
                debugHeapCheck(src);

	    RealtimeJavaDomain dom = (RealtimeJavaDomain) csa.getDomain();
	    dom.readBarrierFailed();
	}
    }

    
    public void putFieldBarrier(CoreServicesAccess csa,
				Oop src, int offset, Oop tgt) 
        throws PragmaNoPollcheck, PragmaNoBarriers, PragmaInline {

        super.putFieldBarrier(csa, src, offset, tgt);
//!!! DISABLED 	storeCheck(csa, src, offset, tgt);
    }

    public void aastoreBarrier(CoreServicesAccess csa,
                               Oop src, int offset, Oop tgt) 
        throws PragmaNoPollcheck, PragmaNoBarriers, PragmaInline {
        
        super.aastoreBarrier(csa, src, offset, tgt);        
//!!! DISABLED	storeCheck(csa, src, offset, tgt);
    }

    // debugging support

    // causes a stack trace to be printed when dumpOn reaches zero. This allows
    // you to see where a specific heap access is occurring
    int dumpOn = 2;

    void debugHeapCheck(Oop src) {
        VM_Area current = getCurrentArea();
        setCurrentArea(getImmortalArea());
        doLoadCheck = false;
        try {
            Native.print_string(" - Heap:");
            Native.print_string(src.getBlueprint().getType().
                                getUnrefinedName().toString());
	    Native.print_string(" at ");
	    Native.print_ptr(VM_Address.fromObject(src));
	    Native.print_string(" with heap at ");
	    Native.print_ptr(heapBase);
	    Native.print_string("...");
	    Native.print_ptr(heapBase.add(heapSize));
	    Native.print_string(" and image at ");
	    Native.print_ptr(Native.getImageBaseAddress());
	    Native.print_string("...");
	    Native.print_ptr(Native.getImageEndAddress());
            Native.print_string("\n");
            if (--dumpOn == 0) {
                StackTraceElement[] stack = new Throwable().getStackTrace();
                for (int i = 0; i < stack.length; i++) {
                    Native.print_string(stack[i].toString());
                    Native.print_string("\n");
                }
            }
        }
        finally {
            setCurrentArea(current);
            doLoadCheck = true;
        }
    }

    public boolean reallySupportNHRTT() {
	return true;
    }

    public boolean supportScopeAreaOf() {
	return true;
    }

}
