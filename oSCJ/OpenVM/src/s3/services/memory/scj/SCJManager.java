package s3.services.memory.scj;

// YES version, with zeroing, no debug

//FIXME: possible speedups: use PragmaAssertNoExceptions and PragmaAssertNoSafePoints (precise gc) for 
//performance critical code
//FIXME: rewrite image store barrier so that it does not use array access with a bounds check

import ovm.core.domain.Oop;
import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;
import ovm.core.execution.NativeInterface;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.AtomicOps;
import ovm.core.services.memory.ExtentWalker;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.MovingGC;
import ovm.core.services.memory.ScopedMemoryContext;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.memory.VM_Word;
import ovm.services.bytecode.JVMConstants.Throwables;
import ovm.services.memory.FinalizableArea;
import ovm.services.memory.scopes.PrimordialScope;
import ovm.services.memory.scopes.VM_ScopedArea;
import ovm.services.realtime.NoHeapRealtimeOVMThread;
import ovm.core.Executive;
import ovm.util.CommandLine;
import ovm.util.Mem;
import ovm.util.OVMError;
import ovm.util.Iterator;
import s3.util.PragmaAtomic;
import s3.util.PragmaNoInline;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaCAlwaysInline;
import s3.util.PragmaAssertNoExceptions;
import s3.util.PragmaAssertNoSafePoints;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.MemoryManager;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import ovm.core.domain.Blueprint;
import ovm.core.execution.CoreServicesAccess;
import s3.core.domain.S3Domain;

import java.io.PrintWriter;
import ovm.core.domain.Domain;

import java.io.IOException;

import s3.services.bootimage.Driver;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.RealtimeJavaDomain;

import ovm.core.domain.Type;
import ovm.core.services.memory.ImageAllocator;
import ovm.core.repository.JavaNames;

import java.io.FileOutputStream;
import ovm.core.services.memory.LocalReferenceIterator;
import ovm.core.services.memory.VM_Area.Destructor;
import ovm.core.services.memory.VM_Area.DestructorWalker;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.stitcher.InvisibleStitcher.Component;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;
import ovm.core.OVMBase;

public class SCJManager extends MemoryManager implements NativeConstants,
		ImageAllocator.Implementation, Component {

	final static boolean DEBUG_INIT = false;
	final static boolean DEBUG_BOOT = false;
	final static boolean DEBUG_SCJ = false;

	/**
	 * Disabled by S3Executive when -verbose-gc is not passed; this means that
	 * we are verbose up until command line parsing happens. In practice this is
	 * OK, since not much interesting stuff happens prior to command line
	 * parsing.
	 */
	boolean verbose = true;

	/**
	 * Because MemoryManager is a boot-time-allocated singleton, J2c be able to
	 * substitute these final fields.
	 */
	final int heapSize;

	static final int pageSize = PAGE_SIZE;
	boolean dumpOOM = true;

	static VM_Address heapBase;
	VM_Address bumpPointer;

	/** A quick and easy way to access the alignment. */
	static final int ALIGN = VM_Address.widthInBytes();

	/**
	 * Scoped memory and heap are also distinct. You must be able to create, use
	 * and destroy scopes while GC is in progress in the heap.
	 */
	VM_Address scopeBase;
	int scopeBaseIndex;
	int scopeSize;
	int bsSize;
	int bsCeilingBlkIdx; // not included
	int bsNextAvailBlkIdx;

	/**
	 * Indexed by block numbers. For each block in an allocated area, this array
	 * contains a pointer to the area object. The area object is at offset zero
	 * of the area's first block.
	 * <p>
	 * 
	 * Ranges of free blocks are represented by a doubly-linked freelist of
	 * {@link FreeArea} objects. As with allocated areas, these objects are
	 * placed at the start of the corresponding memory range. However, entries
	 * in the scopeOwner array are only maintained for the first and last block
	 * in a free range.
	 * <p>
	 * 
	 * This array is mostly used for write-barrier checks. It could also be used
	 * to speed up scope free operations: Rather than searching forward from
	 * firstFree in the linked list to find a FreeArea's prev pointer, we can
	 * search backward from it's index in scopeOwner. However, this is not
	 * implemented.
	 */
	TransientArea[] scopeOwner;

	/**
	 * The first free memory area in address order.
	 */
	FreeArea firstFree;

	final int blockShift;

	final static int alignment = 16;
	int availableHeap;

	final int scratchPadSize;

	public SCJManager(String heapSize_, String scopeSize_,
			String disableChecks_) {

		heapSize = CommandLine.parseSize(heapSize_);
		scopeSize = CommandLine.parseSize(scopeSize_);
		scratchPadSize = CommandLine.parseSize("4m");

		int _blockShift = -1;
		for (int i = 7; i < 20; i++)
			if (pageSize == 1 << i) {
				_blockShift = i;
				break;
			}
		if (_blockShift == -1)
			throw new OVMError("bad block size " + pageSize);
		blockShift = _blockShift;
		bsSize = scopeSize - scratchPadSize;
		int rbsSize = roundUp(bsSize, pageSize);

		bsCeilingBlkIdx = rbsSize >>> blockShift;
		bsNextAvailBlkIdx = 0;

		ImageAllocator.override(this);

		int rscopeSize = roundUp(scopeSize, pageSize);
		scopeOwner = new TransientArea[rscopeSize >>> blockShift];

		if (DEBUG_INIT) {
			Native.print_string("\n");
			Native.print_string("# SCJManager #####################\n");
			Native.print_string("# SCJManager : Init Started\n");

			Native.print_string("# SCJManager :     blockShift is ");
			Native.print_int(blockShift);
			Native.print_string("\n");

			Native.print_string("# SCJManager :     heapSize is ");
			Native.print_int(heapSize);
			Native.print_string("\n");

			Native.print_string("# SCJManager :     scopeSize is ");
			Native.print_int(scopeSize);
			Native.print_string("\n");

			Native.print_string("# SCJManager : Init Finished\n");
			Native.print_string("# SCJManager : ####################\\n");
		}

	}

	public int allocateInImage(int firstFree, int size, Blueprint bp, int _)
			throws BCdead {
		return firstFree;
	}

	public void boot(boolean ignore_useImageBarrier) {

		int rheapSize = roundUp(heapSize, pageSize);
		heapBase = Native.getheap(rheapSize);
		Mem.the().zero(heapBase, rheapSize);
		availableHeap = rheapSize;
		bumpPointer = heapBase;
		if (heapBase.asInt() == -1)
			Native.abort();

		int rscopeSize = roundUp(scopeSize, pageSize);
		scopeBase = Native.getheap(rscopeSize);
		scopeBaseIndex = scopeBase.asInt();
		if (scopeBase.asInt() == -1)
			Native.abort();

		VM_Area r = setCurrentArea(placeNew.at(scopeBase.add(bsCeilingBlkIdx
				* pageSize)));
		try {
			new FreeArea(rscopeSize);
		} finally {
			setCurrentArea(r);
		}
		initPrimordial();

		if (DEBUG_BOOT) {
			Native.print_string("\n");
			Native
					.print_string("# SCJManager : #############################################\n");
			Native.print_string("# SCJManager : Boot Started\n");

			Native.print_string("# SCJManager :     Allocated heapSize is ");
			Native.print_int(rheapSize);
			Native.print_string("\n");

			Native.print_string("# SCJManager :     heapBase is at ");
			Native.print_ptr(heapBase);
			Native.print_string("\n");

			Native.print_string("# SCJManager :     Allocated scopeSize is ");
			Native.print_int(rscopeSize);
			Native.print_string("\n");

			Native.print_string("# SCJManager :     scopeBase is at ");
			Native.print_ptr(scopeBase);
			Native.print_string("\n");

			Native.print_string("# SCJManager :     bsSize is ");
			Native.print_int(bsSize);
			Native.print_string("\n");

			Native.print_string("# SCJManager :     bsUpperIndex is ");
			Native.print_int(bsCeilingBlkIdx);
			Native.print_string("\n");

			Native.print_string("# SCJManager :     rscopeSize is at ");
			Native.print_int(rscopeSize);
			Native.print_string("\n");

			Native.print_string("# SCJManager :     bsUpperIndex is at ");
			Native.print_int(rscopeSize);
			Native.print_string("\n");

			Native.print_string("# SCJManager : Boot Finished\n");
			Native
					.print_string("# SCJManager : #############################################\n\n");
		}
	}

	public void initialize() {
	}

	public Error throwOOM() {
		CoreServicesAccess csa = DomainDirectory.getExecutiveDomain()
				.getCoreServicesAccess();
		csa.generateThrowable(Throwables.OUT_OF_MEMORY_ERROR, 0);
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
		transientSize = S3Domain
				.sizeOfInstance("s3/services/memory/scj/SCJManager$TransientArea");
		int areaSize = S3Domain
				.sizeOfInstance("s3/services/memory/scj/SCJManager$ScopedArea");

		scopedSize = areaSize;

		/**
		 * Somehow the calculations are missing something so we pad based on
		 * empirical observation. Of course with a different object model the
		 * pad may need to change
		 */
		scopedSize += 12;
	}

	public void fullyBootedVM() {
		super.fullyBootedVM();
		initSizes();
	}

	VM_Address getHeapMem(int size, boolean shouldClear) {
		VM_Address mem = null;

		if (availableHeap > size) {
			mem = bumpPointer;
			bumpPointer = bumpPointer.add(size);
			availableHeap -= size;

			return mem;
		} else
			exhausted();

		return mem;
	}

	/**
	 * Allocates a block of memory of the requested size from the current
	 * allocation context.
	 */
	public VM_Address getMem(int size) {
		Area a = (Area) ScopedMemoryContext.getMemoryContext();

		VM_Address mem = a.getMem(size);
		return mem;
	}

	protected class HeapArea extends Area {
		public int size() {
			return heapSize;
		}

		public int memoryConsumed() {
			return bumpPointer.diff(heapBase).asInt();
		}

		public String toString() throws PragmaNoPollcheck {
			return "ImmortalMemory";
		}

		public boolean isLive(Oop oop) {
			return true;
		}

		public VM_Address getBaseAddress() {
			return heapBase;
		}

		public VM_Address getMem(int size) throws PragmaAtomic {
			return getHeapMem(size, true);
		}
	}

	final protected VM_Area heapArea = new HeapArea();

	public VM_Area getHeapArea() {
		return heapArea;
	}

	public boolean supportsDestructors() {
		return false;
	}

	public Object makeMemoryContext() {
		return heapArea;
	}

	public VM_Area getImmortalArea() {
		return heapArea;
	}

	public VM_Area getCurrentArea() {
		return (VM_Area) ScopedMemoryContext.getMemoryContext();
	}

	public VM_Area setCurrentArea(VM_Area area) {
		VM_Area ret = (VM_Area) ScopedMemoryContext.getMemoryContext();
		ScopedMemoryContext.setMemoryContext(area);
		return ret;
	}

	/************************* MEMORY AREAS *************************/

	/**
	 * Base class for our regions. This adds the range values that are to
	 * perform the store barrier checks.
	 */
	public static abstract class Area extends VM_ScopedArea {

		public abstract VM_Address getMem(int size);

		protected abstract VM_Address getBaseAddress();

		Area(PrimordialScope primordial) {
			super(primordial);
		}

		Area() {
		}

		public boolean isScope() {
			return false;
		}
	}

	/**
	 * A memory area that implements placement new.
	 * 
	 * @see SplitRegionManager#placeNew
	 */
	static class PlacementArea extends Area {

		public int size() {
			return Integer.MAX_VALUE;
		}

		public int memoryConsumed() {
			return 0;
		}

		VM_Address place;

		/**
		 * Return the address into which this area allocates, and reset that
		 * address to null
		 */
		public VM_Address getMem(int size) {

			int rawsize = roundUp(size, alignment);
			VM_Address ret = place;
			place = ret.add(rawsize);
			Mem.the().zero(ret, rawsize);
			return ret;
		}

		public VM_Address getBaseAddress() {
			throw Executive.panic("unsupported");
		}

		/**
		 * Set the address into which this area allocates and return
		 * <code>this</code>.
		 */
		public VM_Area at(VM_Address addr) {
			place = addr;
			return this;
		}

		/**
		 * Return the next location in memory that this object will allocate
		 * from
		 */
		public VM_Address next() {
			return place;
		}
	}

	/**
	 * This field is used to implement placement new. Typical usage is as
	 * follows: <code><pre>
	 * VM_Area r = setCurrentArea(placeNew.at(<i>address</i>));
	 * try {
	 *    new <i>C</i>(); // allocate a C instance at address
	 * } finally {
	 *    setCurrentArea(r);
	 * }
	 * </pre></code>
	 * 
	 * Note: the C constructor must not perform allocation in the current memory
	 * area.
	 * <p>
	 * 
	 * Note further: {@link SplitRegionManager.PlacementArea} is not reentrant.
	 */
	final PlacementArea placeNew = new PlacementArea();

	/**
	 * Transient areas are special purpose temporary allocation regions used by
	 * the OVM internally - both in the ED and the UD. Stores into a plain
	 * transient area are not checked (there is no way to determine the validity
	 * of such a store as the transient region doesn't form part of the region
	 * hierarchy. Subclasses override this.
	 */
	class TransientArea extends Area {
		int rsize;
		int offset;
		final int baseOffset;

		TransientArea(int size) throws PragmaNoBarriers {
			super(primordial);
			unchecked = true;
			setRange((short) 0, Short.MAX_VALUE);
			mirror = null;
			rsize = size;
			int idx = idx();
			int i = idx + (rsize >>> blockShift);
			for (; i-- > idx;)
				scopeOwner[i] = this;

			offset = baseOffset = placeNew.next().diff(base()).asInt();
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

		public String toString() {
			return "area@" + hashCode();
		}

		// can we assume that transient areas are always used by only a
		// single thread? That is true so far as I know - DH
		public VM_Address getMem(int size) {
			if (offset + size > rsize) {
				try {
					if (dumpOOM) {
						VM_Area r = setCurrentArea(heapArea);
						try {
							BasicIO.out.print(this
									+ ": transient area exhausted " + "("
									+ offset + " of " + rsize + " used) ");
							BasicIO.out.print(" size is: " + size);
							Oop mirror = getMirror();
							int mnum = (mirror == null ? 0 : mirror.getHash());
							BasicIO.out.println("mirror @"
									+ Integer.toHexString(mnum));
							new Error().printStackTrace(); // show where we are
							dumpExtent(base(), base().add(offset));
						} finally {
							setCurrentArea(r);
						}
					}
				} finally {
					throwOOM();
				}
			}

			VM_Address ret = base().add(offset);
			offset += size;

			// we zero here to get more predictable behavior.
			// note: must zero out the word following this object. but
			// the first word of this object is already zeroed. hence
			// this weird expression.
			//
			// Note: it may appear tempting to zero out a scope on
			// per-block basis (as we do with the heap). However,
			// this will not work with the scratchpad, because we
			// constantly discard the top few scratchpad objects, and
			// allocate over them.
			Mem.the().zero(
					ret.add(ALIGN),
					offset + ALIGN <= rsize ? size : size - ALIGN + rsize
							- offset);

			return ret;
		}

		public int size() {
			return rsize;
		}

		public int memoryConsumed() {
			return offset;
		}

		public void reset() {
			offset = baseOffset;
			base().add(offset).setAddress(null);
		}

		public void reset(int newOffset) {
			assert (0 <= newOffset && newOffset <= offset && newOffset >= base()
					.asOop().getBlueprint().getFixedSize());
			offset = newOffset;
			base().add(offset).setAddress(null);
		}

		public void destroy() throws PragmaAtomic {
			int size = rsize;
			// tricky: be sure do load this$0 before zeroing out the
			// fields of the FreeArea object we are allocating.
			SCJManager copyThis = SCJManager.this;
			VM_Area r = setCurrentArea(placeNew.at(base()));
			try {
				copyThis.new FreeArea(size);
			} finally {
				setCurrentArea(r);
			}
		}
	}

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
			scopeOwner[idx() + (rsize >>> blockShift) - 1] = this;
		}

		/**
		 * Attach this FreeArea to the list. Coalesce with next and previous if
		 * possible.
		 */
		FreeArea(int size) throws PragmaNoBarriers {

			FreeArea prev = null;
			FreeArea next = firstFree;
			while (next != null && next.base().uLT(base())) {
				prev = next;
				next = next.next;
			}

			if (prev != null && prev.base().add(prev.rsize) == base()) {
				// Coalesce this with prev, then try to coalesce prev
				// with next;
				prev.rsize += size;
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
		}

		/**
		 * Create a smaller FreeArea from the second portion of a larger
		 * FreeArea (oldStart).
		 */
		FreeArea(FreeArea oldStart, int sizeDelta) throws PragmaNoBarriers {

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

		}

	}

	/** The primordial scope */
	PrimordialScope primordial;

	// invoked by boot()
	void initPrimordial() throws PragmaNoBarriers {
		primordial = new PrimordialScope(true);
	}

	/**
	 * FIXME: The scope checks belong on the ovm side!
	 * 
	 * ScopedArea represents a RTSJ ScopedMemory object.
	 * 
	 * @author Filip Pizlo (original UD version of scopetree management)
	 * @author David Holmes (finalization and ED version)
	 */
	public class ScopedArea extends TransientArea {

		public void destroy() throws PragmaAtomic {
			bsNextAvailBlkIdx = idx();
		}

		// NOTE: we're not allowed to do allocation in the constructor
		// It is done in init() instead
		ScopedArea(int size, Oop mirror) {
			super(size);
			this.mirror = mirror;
			this.unchecked = false;
			reset();
		}

		public boolean isScope() {
			return true;
		}

		// note: we don't pre-allocate this as it takes too much memory.
		// We also don't allocate it in this area because the most likely
		// reason this is called is during dump-on-out-of-memory processing.
		public String toString() {
			return "scope@" + hashCode();
		}

		public VM_Address getMem(int size) throws PragmaAtomic {
			if (offset + size > rsize) {
				try {
					if (dumpOOM) {
						VM_Area r = setCurrentArea(heapArea);
						try {
							BasicIO.out
									.print(this + ": scoped area exhausted "
											+ "(" + offset + " of " + rsize
											+ " used) ");
							Oop mirror = getMirror();
							int mnum = (mirror == null ? 0 : mirror.getHash());
							BasicIO.out.println("mirror @"
									+ Integer.toHexString(mnum));
							new Error().printStackTrace(); // show where we are
							dumpExtent(base(), base().add(offset));
						} finally {
							setCurrentArea(r);
						}
					}
				} finally {
					throwOOM();
				}
			}

			VM_Address ret = base().add(offset);
			offset += size;

			// we zero here to get more predictable behavior.
			// note: must zero out the word following this object. but
			// the first word of this object is already zeroed. hence
			// this weird expression.
			//
			// Note: it may appear tempting to zero out a scope on
			// per-block basis (as we do with the heap). However,
			// this will not work with the scratchpad, because we
			// constantly discard the top few scratchpad objects, and
			// allocate over them.

			// Mem.the().zero(ret.add(ALIGN), offset == rsize ? size-ALIGN :
			// size);

			/*
			 * if ( offset == rsize) { //Mem.the().zero(ret.add(ALIGN),
			 * size-ALIGN ); zero += size-ALIGN; } else {
			 * //Mem.the().zero(ret.add(ALIGN), size); zero += size; }
			 */
			return ret;

		}

		public void reset() {
			Mem.the().zero(base().add(baseOffset), offset - baseOffset);
			offset = baseOffset;
			base().add(offset).setAddress(null);
		}
	}

	/**
	 * Remove a chunk of memory from the freelist, and return it's base pointer.
	 */
	private VM_Address firstFit(int size) throws PragmaNoBarriers {
		FreeArea fr = firstFree;
		while (fr != null && fr.rsize < size)
			fr = fr.next;
		if (fr == null)
			throwOOM();
		VM_Address ret = fr.base();
		if (fr.rsize > size) {
			VM_Address newStart = ret.add(size);
			VM_Area r = setCurrentArea(placeNew.at(newStart));
			try {
				new FreeArea(fr, size);
			} finally {
				setCurrentArea(r);
			}
		} else {

			if (fr.prev == null) {
				firstFree = fr.next;
			} else {
				fr.prev.next = fr.next;
			}
			if (fr.next != null) {
				fr.next.prev = fr.prev;
			}
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
	 * Create a memory area that is not subject to scope checks.
	 * <p>
	 * 
	 * If this method is called at image build time, it will return null. Areas
	 * created through this method at image build time are handled just like
	 * areas returned from
	 * {@link ovm.core.services.memory.StandaloneMemoryPolicy} at build time.
	 * 
	 * @param size
	 *            the size of the area. The actual size of the area is always a
	 *            multiple of the block size, and is expanded to include the
	 *            memory consumed by the area object itself.
	 * 
	 * @return a new memory area
	 * 
	 * @throws OutOfMemoryError
	 *             if the area can't be allocated.
	 * 
	 */
	public VM_Area makeExplicitArea(int size) throws PragmaAtomic {

		if (scopeBase == null)
			return null;

		int rsize = roundUp(size + transientSize, pageSize);
		VM_Address raw = firstFit(rsize);
		VM_Area r = setCurrentArea(placeNew.at(raw));
		VM_Area ret = null;
		try {
			ret = new TransientArea(rsize);
			// First time called is prior to fullyBooted so transientSize==0
			assert transientSize == 0 || ret.memoryRemaining() >= size : "available space too small: "
					+ ret.memoryRemaining() + " vs. " + size;
			return ret;
		} finally {
			setCurrentArea(r);
			if (DEBUG_SCJ) {
				Native
						.print_string("[SCJ DB] SCJManager.makeExplicitArea() - ");
				Native.print_string(ret == null ? "" : ret.toString());
				Native.print_string("\n size: ");
				Native.print_int(size);
				Native.print_string("\n raw size: ");
				Native.print_int(rsize);
				Native.print_string("\n");
			}
		}
	}

	/**
	 * Create a scoped memory area.
	 * 
	 * @param mirror
	 *            the user-domain <tt>ScopedMemory</tt> object associated with
	 *            this scoped area.
	 * 
	 * @param size
	 *            the size of the area. The actual size of the area is always a
	 *            multiple of the block size, and is expanded to include the
	 *            memory consumed by the area object itself.
	 * 
	 * @return a new memory area
	 * 
	 * @throws OutOfMemoryError
	 *             if the area can't be allocated.
	 * 
	 */
	public VM_ScopedArea makeScopedArea(Oop mirror, int size)
			throws PragmaAtomic {

		int rsize = roundUp(size + scopedSize, pageSize);
		int blocks = rsize >>> blockShift;
		VM_Address raw = scopeBase.add(bsNextAvailBlkIdx << blockShift);

		if (bsNextAvailBlkIdx + blocks > bsCeilingBlkIdx) {
			Native.print_string("Run out of backingStore\n");
			throwOOM();
		}
		bsNextAvailBlkIdx += blocks;
		VM_Area r = setCurrentArea(placeNew.at(raw));
		VM_ScopedArea ret = null;
		try {
			return ret = new ScopedArea(rsize, mirror);
		} finally {
			setCurrentArea(r);
			if (DEBUG_SCJ) {
				Native.print_string("[SCJ DB] SCJManager.makeScopedArea() - ");
				Native.print_string(ret == null ? "" : ret.toString());
				Native.print_string("\n size: ");
				Native.print_int(size);
				Native.print_string("\n raw size: ");
				Native.print_int(rsize);
				Native.print_string("\n");
			}
		}
	}

	VM_Area areaOf(Object obj) {
		return areaOf(VM_Address.fromObject(obj).asOop());
	}

	public VM_Area areaOf(Oop mem) {
		VM_Word off = VM_Address.fromObject(mem).diff(heapBase);
		if (off.uLT(VM_Word.fromInt(heapSize)))
			return heapArea;

		// SCOPE:
		off = VM_Address.fromObject(mem).diff(scopeBase);
		if (!off.uLT(VM_Word.fromInt(scopeSize)))
			return heapArea;

		int idx = off.asInt() >>> blockShift;
		return scopeOwner[idx];
	}

	/**
	 * Slow path for heap store checks. Takes addresses that have already been
	 * shifted by blockShift and do not fall on the same block.
	 * 
	 * @param csa
	 *            used to single error
	 * @param sb
	 *            block pointed from (source)
	 * @param tb
	 *            block pointed to (target)
	 */
	void storeCheckSlow(CoreServicesAccess csa, int sb, int tb)
			throws PragmaNoPollcheck, PragmaNoBarriers {
		Native.print_string("[SCJ Manager] who called storeCheckSlow? \n");
	}

	/**
	 * Perform region store check, and raise the appropriate exception when a
	 * store is bogus.
	 * 
	 * @param csa
	 *            used to signal error
	 * @param src
	 *            object being updated
	 * @param tgt
	 *            new field/element value
	 */
	void storeCheck(CoreServicesAccess csa, Oop src, int offset, Oop tgt)
			throws PragmaNoPollcheck, PragmaNoBarriers {
		Native.print_string("[SCJ Manager] who called storeCheck? \n");
	}

	/**************************** GC ****************************/

	public boolean supportsGC() {
		return false;
	}

	public void garbageCollect() {
	}

	public void doGC() {
	}

	private void exhausted() {
		if (dumpOOM && verbose)
			BasicIO.out.println("heap exhausted");
		throw outOfMemory();
	}

	public void pin(Oop o) {
	}

	public void unpin(Oop _) {
	}

	/*
	 * Should we remove this method? - vhs
	 */
	public void enableSilentMode() {
		verbose = false;
	}

	/*
	 * Should we remove this method? - vhs
	 */
	public void enableAllDebug() {
		dumpOOM = true;
	}

	/************************* BARRIER *************************/

	/*
	 * Should we remove this method? - vhs
	 */
	public boolean needsWriteBarrier() {
		return false;
	}

	/*
	 * Should we remove this method? - vhs
	 */
	public boolean needsReadBarrier() {
		return false;
	}

	/*
	 * Should we remove this method? - vhs
	 */
	public boolean readBarriersEnabled() {
		return false;
	}

	/*
	 * Should we remove this method? - vhs
	 */
	public boolean forceBarriers() {
		return false;
	}

	/*
	 * Should we remove this method? - vhs
	 */
	public void enableReadBarriers(boolean enabled) {
	}

	/*
	 * Should we remove this method? - vhs
	 */
	public void readBarrier(CoreServicesAccess csa, Oop src)
			throws PragmaInline, PragmaNoBarriers {
	}

	public boolean reallySupportNHRTT() {
		return true;
	}

	public boolean supportScopeAreaOf() {
		return true;
	}

	public void putFieldBarrier(CoreServicesAccess csa, Oop src, int offset,
			Oop tgt, int aSrc, int aTgt) throws PragmaNoPollcheck,
			PragmaNoBarriers, PragmaInline, PragmaAssertNoExceptions {
		updateReference(csa, src, offset, tgt, aSrc, aTgt);
	}

	public void aastoreBarrier(CoreServicesAccess csa, Oop src, int offset,
			Oop tgt, int aSrc, int aTgt) throws PragmaNoPollcheck,
			PragmaNoBarriers, PragmaInline, PragmaAssertNoExceptions {
		updateReference(csa, src, offset, tgt, aSrc, aTgt);
	}

	void imageStoreBarrier(Oop src, int aSrc) throws PragmaNoPollcheck,
			PragmaNoBarriers, PragmaInline, PragmaAssertNoExceptions,
			PragmaCAlwaysInline {
	}

	// this method must not modify references on the heap (or in the image),
	// although it uses "PragmaNoBarriers", the barriers would still be
	// inserted,
	// because we use forceBarriers() == true

	void updateReference(CoreServicesAccess csa, Oop src, int offset, Oop tgt,
			int aSrc, int aTgt) throws PragmaNoPollcheck, PragmaNoBarriers,
			PragmaInline, PragmaAssertNoExceptions {

		VM_Address addr = VM_Address.fromObject(src).add(offset);
		addr.setAddress(VM_Address.fromObject(tgt));

		if ((aTgt & Assert.IMAGEONLY) == 0) {
			// must be after the store because of initialization code
			imageStoreBarrier(src, aSrc);
		}
	}

	public void setReferenceField(Oop object, int offset, Oop src)
			throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints,
			PragmaInline {
		putFieldBarrier(null, object, offset, src, 0, 0);
	}

	// FIXME: can we rewrite these methods not to make assertions about how
	// individual primitive types are represented ?
	public void setPrimitiveField(Oop object, int offset, boolean value)
			throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints,
			PragmaInline {
		VM_Address addr = VM_Address.fromObject(object);
		addr.add(offset).setInt(value ? 1 : 0);
	}

	public void setPrimitiveField(Oop object, int offset, byte value)
			throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints,
			PragmaInline {
		VM_Address addr = VM_Address.fromObject(object);
		addr.add(offset).setInt(value);
	}

	public void setPrimitiveField(Oop object, int offset, short value)
			throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints,
			PragmaInline {
		VM_Address addr = VM_Address.fromObject(object);
		addr.add(offset).setInt(value);
	}

	public void setPrimitiveField(Oop object, int offset, char value)
			throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints,
			PragmaInline {
		VM_Address addr = VM_Address.fromObject(object);
		addr.add(offset).setInt(value);
	}

	public void setPrimitiveField(Oop object, int offset, int value)
			throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints,
			PragmaInline {
		VM_Address addr = VM_Address.fromObject(object);
		addr.add(offset).setInt(value);
	}

	public void setPrimitiveField(Oop object, int offset, long value)
			throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints,
			PragmaInline {
		VM_Address addr = VM_Address.fromObject(object);
		addr.add(offset).setLong(value);
	}

	public void setPrimitiveField(Oop object, int offset, float value)
			throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints,
			PragmaInline {
		VM_Address addr = VM_Address.fromObject(object);
		addr.add(offset).setFloat(value);
	}

	public void setPrimitiveField(Oop object, int offset, double value)
			throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints,
			PragmaInline {
		VM_Address addr = VM_Address.fromObject(object);
		addr.add(offset).setDouble(value);
	}

	public void setPrimitiveArrayElementAtByteOffset(Oop object,
			int byteOffset, int value) throws PragmaAssertNoExceptions,
			PragmaAssertNoSafePoints, PragmaInline {
		setPrimitiveField(object, byteOffset, value);
	}

	public void setPrimitiveArrayElementAtByteOffset(Oop object,
			int byteOffset, char value) throws PragmaAssertNoExceptions,
			PragmaAssertNoSafePoints, PragmaInline {
		VM_Address addr = VM_Address.fromObject(object);
		addr.add(byteOffset).setChar(value);
	}

	public void setReferenceArrayElementAtByteOffset(Oop object,
			int byteOffset, Oop value) throws PragmaAssertNoExceptions,
			PragmaAssertNoSafePoints, PragmaInline {
		setReferenceField(object, byteOffset, value);
	}

	private static int roundUp(int base, int block) {
		return (base + block - 1) & ~(block - 1);
	}
}
