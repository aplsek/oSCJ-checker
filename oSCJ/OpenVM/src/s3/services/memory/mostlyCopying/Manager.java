
// FIXME: possible speedups: use PragmaAssertNoExceptions and PragmaAssertNoSafePoints (precise gc) for 
// performance critical code
// FIXME: rewrite image store barrier so that it does not use array access with a bounds check

package s3.services.memory.mostlyCopying;

import ovm.core.domain.Oop;
import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;
import ovm.core.execution.NativeInterface;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.ExtentWalker;
import ovm.core.services.memory.MovingGC;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.memory.VM_Word;
import ovm.services.bytecode.JVMConstants.Throwables;
import ovm.core.Executive;
import ovm.core.services.memory.VM_Area.Destructor;
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

import ovm.core.domain.Type;
import ovm.core.services.memory.ImageAllocator;
import ovm.core.repository.JavaNames;

import java.io.FileOutputStream;
import ovm.core.services.memory.LocalReferenceIterator;
import ovm.core.OVMBase;

/**
 * The basic Mostly Copying implementation.  Provides pinning and the
 * bootimage write barrier, but does not support realtime features.
 *
 * @author <a href="mailto://baker29@cs.purdue.edu"> Jason Baker </a>
 */
public class Manager extends MemoryManager
    implements NativeConstants, ImageAllocator.Implementation
{

    static final boolean DEBUG = true;
    
    static final boolean FAST_IMAGE_SCAN = false;
    static final boolean NEEDS_IMAGE_BARRIER = FAST_IMAGE_SCAN;
    static final boolean FORCE_IMAGE_BARRIER = false;

    /* Number of extra blocks to hold in reserve for GC.  We need a
     * little extra space to do formatted IO.
     */
    static final int GC_WIGGLE_ROOM = 10;
    /*
     * Conservative collector parameters.  It really doesn't matter
     * whether gcc generates displaced pointers, as long as there is a
     * pointer to the object base.
     */
    static final boolean SKIP_UNALIGNED = true;
    static final boolean SKIP_OFF_BLOCK = true;

    /**
     * If true, zero out memory whether it is allocated for a new
     * object or a copy.  This results in a leaner path through
     * getHeapMem, but it also results in the to-space being
     * needlessly zeroed out.
     **/
    static final boolean ZERO_TO_SPACE = true;
    
    static final boolean DEBUG_PROFILE = false;
    static final boolean DEBUG_PROFILE_WALK = false;
    static final boolean BASIC_STATS = false;

    /**
     * Disabled by S3Executive when -verbose-gc is not passed; this
     * means that we are verbose up until command line parsing
     * happens.  in practice this is OK, since not much interesting
     * stuff happens prior to command line parsing.
     **/
    boolean verbose = true; 
    
    /**
     * Because MemoryManager is a boot-time-allocated singleton, J2c
     * be able to substitute these final fields.
     **/
    final int heapSize;

    final int blockSize;
    /** 1 &lt;&lt; blockShift == blockSize **/
    final int blockShift;
    /** blockMask == blockSize - 1 **/
    final int blockMask;

    boolean dumpOOM;
    static final boolean DUMP_ON_GC = false;
    
    int gcThreshold;		// # blocks allocated before GC
    int allocated = 0;		// # of allocated blocks;
    /**
     * The number of blocks that could not be moved because either an
     * object on the block was explicitly {@link #pin}ed, or because a
     * conservative pointer to the block was encountered during stack
     * walking.  The pinCount is recomputed on each GC.
     **/
    int pinCount;
    /**
     * Total size of blocks pinned either explicitly or through stack
     * references. @see {@link #pinCount}.
     **/
    int pinSize;
    int movedBytes = 0;
    boolean inGC = false;	// true during collection
    
    // I'm not sure how the compiler can take advantadge of this final
    // modifier, but it is final, and in fact must be build-time
    // allocated.
    final VecList block;
    VM_Address heapBase;

    /**
     * The number of explicit pinned objects in each block.  If
     * pinnedBlocks[i] is 0, we are free to move block[i].  This
     * reference count is not to be confused with {@link #pinCount},
     * which describes explicit pinning and pinning of conservative
     * roots. 
     **/
    short[] pinnedBlocks;

    // perforance counters
    long timeSpentInGC;
    long timeSpentMarkingConsrvtvRoots;
    long timeSpentMarkingPreciseRoots;
    long timeSpentWalkingGrey;

    boolean useImageBarrier = true;
    int rootPages = 0;

    public static final class Nat implements NativeInterface {
	static native void mc_barrier_init(int imageEnd,
					   int[] continued,
					   int[] dirty);
    }

    /**
     * Adapt The ExtentWalker interface to iterate over blocks of the
     * heap rather than address ranges.
     */
    public class BlockWalker {
	ExtentWalker w;

	public BlockWalker(ExtentWalker w) { this. w = w; }

	public void walk(int idx, int sz) {
	    VM_Address base = heapBase.add(idx << blockShift);
	    VM_Address end = base.add(sz << blockShift);
	    w.walk(base, end);
	}

    }

    public Manager(String heapSize) {
	// Basic GC functionality
	this.heapSize = CommandLine.parseSize(heapSize);
	this.blockSize = PAGE_SIZE;
	this.blockMask = this.blockSize - 1;
        
	blockOffset = this.blockSize; // first allocation grabs a block

	int _blockShift = -1;
	for (int i = 7; i < 20; i++)
	    if (this.blockSize == 1 << i) {
		_blockShift = i;
		break;
	    }
	if (_blockShift == -1)
	    throw new OVMError("bad block size " + blockSize);
	this.blockShift = _blockShift;
	gcThreshold = (this.heapSize >> (_blockShift + 1)) - 10;
	block = new VecList(this.heapSize >> _blockShift);

	// pin()/unpin() support
	pinnedBlocks = new short[this.heapSize >> blockShift];

	// Image write barrier support
	ImageAllocator.override(this);
	int _heapBase = super.getImageBaseAddress();
	int _imageBase = _heapBase + this.heapSize;
	// round up to a 32-block boundary
	_imageBase = _imageBase + (31<<blockShift) & (-1 << 5+blockShift);

	heapBase = VM_Address.fromInt(_heapBase);
	imageBase = VM_Address.fromInt(_imageBase);
	
	imageBaseIdx =  (_imageBase - _heapBase) >> blockShift;
/*
	try {
	    params = new PrintWriter(new FileOutputStream
				     ("mc_barrier_params.h"),
				     true);
	    params.println("#define OVM_PAGE_SIZE " + this.blockSize);
	    params.println("#define HEAP_START 0x" +
			   Integer.toHexString(_heapBase));
	    params.println("#define IMAGE_CONTINUED_RANGES \\");

	    Driver.gen_ovm_c.println("#include <mc_barrier_params.h>");
	    Driver.gen_ovm_c.println("#include <mc_barrier.h>");
	    Driver.gen_ovm_c.flush();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
*/	
    }

    // FIXME: refactor
    // Several tricks are used to lay out the bootimage in a manner
    // compatible with card-marking, the tricks started in the
    // constructor above, and persist through the boot() method that
    // is called at runtime.  This code is duplicated in
    // triPizlo.theMan.  It should really be moved to a common
    // location since it is shared between both garbage collectors

    // This little hook sets a variable, intArray that is used at
    // runtime by boot().
    private void initLate() throws BCdead {
	try {
	    Domain ed = DomainDirectory.getExecutiveDomain();
	    Type.Context stc = ed.getSystemTypeContext();
	    Type intArrayT = stc.typeFor(JavaNames.arr_int);
	    intArray = (Blueprint.Array) ed.blueprintFor(intArrayT);
	} catch (LinkageException e) {
	    throw e.unchecked();
	}
    }

    transient boolean lastBig;
    transient PrintWriter params;

    public int getFixedHeapAddress() {
	return heapBase.asInt();
    }

    public int getImageBaseAddress() {
	return imageBase.asInt();
    }
    
    public int allocateInImage(int firstFree, int size, Blueprint bp, int _)
	throws BCdead
    {
	if (intArray == null)
	    initLate();
	if (!lastBig
	    && ((firstFree + size) >> blockShift) == (firstFree >> blockShift))
	    return firstFree;
	else if (size <= blockSize) {
	    lastBig = false;
	    return ((firstFree + blockSize - 1) & ~(blockSize - 1));
	} else {
	    lastBig = true;
	    int ret = (firstFree + blockSize - 1) & ~(blockSize - 1);
/*	    params.println("\t{ 0x" + Integer.toHexString(ret) +
			     ", 0x" + Integer.toHexString(ret+size) + "}, \\");
*/			     
	    return ret;
	}
    }

    public void boot(boolean useImageBarrier) {
	this.useImageBarrier = useImageBarrier;

	if (!useImageBarrier) {
	  Native.print_string("WARNING: Image barrier can no longer be turned off.");
	}

	VM_Address ret = Native.mmap(heapBase, imageBase.diff(heapBase).asInt(),
				     PROT_READ|PROT_WRITE,
				     MAP_FIXED|MAP_ANON|MAP_PRIVATE,
				     -1, 0);
	if (ret != heapBase) {
	  throw new OVMError("Cannot map heap to a specified address, aborting...\n");
	}

	int heapLen = (heapSize + (31<<blockShift)) >> (blockShift + 5);
	int heapSize = (int) intArray.computeSizeFor(heapLen);
	int fullByteSize = Native.getImageEndAddress().diff(heapBase).asInt();
	int fullLen = ((fullByteSize + (31<<blockShift) + blockSize - 1)
		       >> (blockShift + 5));
	int fullSize = (int) intArray.computeSizeFor(fullLen);

	VM_Address p = Native.mmap(null,
				   4*heapSize + 1*fullSize,
				   PROT_READ|PROT_WRITE,
				   MAP_ANON|MAP_PRIVATE,
				   -1, 0);

	/* if these assertions break, image scanning breaks too */
	assert(imageBase.asInt() == getImageBaseAddress());

//	assert( Native.getImageBaseAddress().asInt() == getImageBaseAddress()); // this DOES NOT HOLD
// Native.getImageBaseAddress().asInt() == 0x45a00034 
// getImageBaseAddress() == 0x45a00000 == imageBase.asInt()
	
	assert( heapBase.add(imageBaseIdx << blockShift).asInt() == imageBase.asInt() );

        // must be first, because of image barrier
        
	intArray.stamp(p, fullLen);
	block.dirty = (int[]) p.asAnyObject();
	p = p.add(fullSize);

	intArray.stamp(p, heapLen);
	block.freeBlocks = (int[]) p.asAnyObject();
	p = p.add(heapSize);

	intArray.stamp(p, heapLen);
	block.whiteBlocks = (int[]) p.asAnyObject();
	block.freshBlocks = block.whiteBlocks;
	p = p.add(heapSize);

	intArray.stamp(p, heapLen);
	block.greyBlocks = (int[]) p.asAnyObject();
	p = p.add(heapSize);


	intArray.stamp(p, heapLen);
	block.cont = (int[]) p.asAnyObject();
	p = p.add(fullSize);

	block.initFreelist();

	lastImageBlockIdx = Native.getImageEndAddress().diff(heapBase).asInt();
	lastImageBlockIdx >>= blockShift;
	lastImageBlock = VM_Word.fromInt(lastImageBlockIdx);
	

	heapArea.setupDestructors(this.heapSize);
	
        if (NEEDS_IMAGE_BARRIER || FORCE_IMAGE_BARRIER) {
  	  // now mark the roots arrays in the image as dirty
  	  // (strings are being written there without the image barrier)
  	  
  	  for (Iterator it = DomainDirectory.domains(); it.hasNext(); ) {
            S3Domain d = (S3Domain) it.next();
            imageStoreBarrier(VM_Address.fromObjectNB(d.j2cRoots).asOop(),Assert.IMAGEONLY|Assert.NONNULL);
          }
        }
    }

    // Runtime support for the image barrier.
    final VM_Address imageBase;
    final int imageBaseIdx;
    VM_Word  lastImageBlock;
    int lastImageBlockIdx;
    
    // call DomainDirectory.getExecutiveDomain late to avoid an
    // initialization cycle
    /*final*/ Blueprint.Array intArray;
    
    /**
     * Return a {@link BlockWalker} that applies mprotect to
     * the address ranges given by a set of blocks.
     * @param prot the protection flags to apply
     **/
    protected BlockWalker makeProtector(final int prot) {
	ExtentWalker doIt = new ExtentWalker() {
		public void walk(VM_Address start, VM_Address end) {
		    Native.mprotect(start, end.asInt() - start.asInt(), prot);
		}
		public VM_Address walkRet(VM_Address start, VM_Address end) {
		  walk(start,end);
		  return end;
		}
	    };
	return new BlockWalker(doIt);
    }

    boolean protectFreePages = false;
    BlockWalker prot_none_protector = makeProtector(PROT_NONE);
    
    VM_Address getBlock(int size) {
	if (!inGC && allocated + size > gcThreshold) {
            // we'd prefer to call doGC but the gc sync policy is tied up
            // in garbageCollect. So we have some redundant synchronization
            // here
	    garbageCollect();
	    if (allocated + size > gcThreshold)
		return null;
	}
	int b = block.alloc(size);
	if (b == -1)
	    // FIXME: block.alloc() may fail due to fragmentation.  If
	    // we did not GC above, maybe it would be a good idea to
	    // GC here.
	    return null;
	allocated += size;
	VM_Address ret = heapBase.add(b << blockShift);
	if (protectFreePages)
	    Native.mprotect(ret, size*PAGE_SIZE, PROT_READ|PROT_WRITE);
	return ret;
    }

    boolean inFromSpace(VM_Address addr) {
	VM_Word off = addr.diff(heapBase);
	if (off.uLT(VM_Word.fromInt(heapSize))) {
	    // in the heap...
	    int bidx = off.asInt() >>> blockShift;
	    return block.inFromSpace(bidx);
	}
	return false;
    }
    
    static void addressDiagnostics(String name,
				   int bidx,
				   VecList vlb,
				   int[] blocks) {
	Native.print("   Is ");
	Native.print(name);
	Native.print(": ");
	if (vlb.getBit(blocks,bidx)) {
	    Native.print("yes\n");
	} else {
	    Native.print("no\n");
	}
    }

    public void assertAddressValid(VM_Address ptr) {
	Native.print("Asserting address: ");
	Native.print_ptr(ptr);
	Native.print("\n");
	Native.print("   In heap: ");
	VM_Word off = ptr.diff(heapBase);
	if (off.uLT(VM_Word.fromInt(heapSize))) {
	    Native.print("yes\n");
	    Native.print("   In from space: ");
	    Native.print(inFromSpace(ptr)?"yes\n":"no\n");
	    int bidx = off.asInt() >>> blockShift;
	    addressDiagnostics("free",bidx,block,block.freeBlocks);
	    addressDiagnostics("white",bidx,block,block.whiteBlocks);
	    addressDiagnostics("grey",bidx,block,block.greyBlocks);
	    addressDiagnostics("cont",bidx,block,block.cont);
	    addressDiagnostics("fresh",bidx,block,block.freshBlocks);
	} else {
	    Native.print("no\n");
	}
    }
    
    /**
     * This class is responsible for copying objects reachable from
     * other objects and pinning objects that reachable from the
     * stack.
     *
     * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
     */
    protected class Updater extends ConservativeUpdater {
	    public boolean needsUpdate(MovingGC oop) {
		return inFromSpace(VM_Address.fromObject(oop));
	    }

	    public void checkConservativeRoot(VM_Word w) {
		if (SKIP_UNALIGNED) {
		    int low = w.asInt() & 0x3;
		    if (low != 0)
			return;
		}
		VM_Word offset = w.sub(heapBase.asInt());
		if (offset.uGE(VM_Word.fromInt(heapSize)))
		    return;
		int idx = offset.asInt() >>> blockShift;
		if (block.isContinued(idx)) { /* this is in heap, not image */
		    if (SKIP_OFF_BLOCK)
			return;
		    else
			idx = block.getBase(idx);
		}
		if (block.inFromSpace(idx)) {
		    //BasicIO.err.print("found conservative pointer: 0x");
		    //BasicIO.err.println(Integer.toHexString(w.asInt()));
		    pinCount++;
		    pinSize += block.getSize(idx);
		    block.pin(idx);
		}
	    }

	    public Oop updateReference(MovingGC oop) {
		//Blueprint _bp = oop.getBlueprint();
		int sz = oop.getBlueprint().getVariableSize(oop);
		if (sz >= blockSize) {
		    movedBytes += sz;
		    allocated += (sz + blockMask) >> blockShift;
		    VM_Word off = VM_Address.fromObject(oop).diff(heapBase);
		    int idx = off.asInt() >>> blockShift;
		    block.pin(idx);
		    return oop;
		} else {
		    VM_Address newLoc = getHeapMem(sz, false);
		    Mem.the().cpy(newLoc, VM_Address.fromObject(oop), sz);
		    oop.markAsForwarded(newLoc);
		    return newLoc.asOop();
		}
	    }

    }

    final ConservativeUpdater copier = makeCopier();

    /**
     * A wrapper for {@link #copier} that deals with heap blocks rather
     * than address ranges.
     **/
    final BlockWalker bcopier = new BlockWalker(copier);

    /**
     * Allocate the {@link ConservativeUpdater} used to scan thread
     * stacks and update grey objects in the heap.
     **/
    protected ConservativeUpdater makeCopier() { return new Updater(); }

    /**
     * Return the BlockWalker used for updating regions of root
     * objects (such as the bootimage and immortal space).  By
     * default, the root walker is a wrapper around the object created
     * by {@link #makeCopier}.
     **/
    protected BlockWalker getRootWalker() { return bcopier; }

    /**
     * The first GC step: pin objects that may be referenced from the
     * stack.  These objects are marked as grey, and will be traversed
     * at a later stack in the GC.
     **/
    void markConservativeRoots() throws PragmaNoInline {
        // subtle point: block.pin will complain if we try to pin the
        // same block twice.  Be sure to do explicit pinning before
        // the conservative stack walk.
        for (int i = 0; i < pinnedBlocks.length; i++)
            if (pinnedBlocks[i] > 0) {
		pinCount++;
		pinSize += block.getSize(i);
                block.pin(i);
	    }

	Activation act = Context.getCurrentContext().getCurrentActivation();
	act.caller(act);
	copier.walkThreadStacks(act);
	allocated += pinSize;
    }

    /**
     * The second GC step:  copy heap objects reachable from the
     * bootimage.
     **/
    void markPreciseRoots() {
    
      /* 
        assumptions of this code: "the image is not too small":
          - the image size is at least one block
               
        observations needed to make this work:
            imageBaseIdx - the first block of the image, which starts with a header that cannot be correctly scanned,
              because when cast to Oop, Oop.getBlueprint() returns invalid non-NULL pointer ; blocks are indexed from
              the heap base

            getImageBaseAddress() - the first byte of the header of the image (first byte of the block imageBaseIdx)
               imageBase - same as getImageBaseAddress()                    
                   
               Native.getImageBaseAddress() - the first byte of the image after the header (scanning starts here)
                
               super.getImageBaseAddress() - the HEAP (!!!) address
      */
           
      BlockWalker bcopier = getRootWalker();
      
      if (FAST_IMAGE_SCAN) {
        VM_Address addr = Native.getImageBaseAddress();
	    
        /* walk from the first byte after image header up to a block aligned address */
	    
        int idx = imageBaseIdx;
	    
        if (block.getBit(block.dirty, imageBaseIdx)) {
	    
          addr = bcopier.w.walkRet(addr, addr.add(1 << blockShift) ); /* assumes that image has at least one block */
          idx = addr.diff(heapBase).asInt() >> blockShift;
        }
	    
        /* now, after the first block.ffs, we will be at block aligned address of a dirty block */
        /* then keep scanning dirty blocks, stop when we get to last image block beginning or beyond */
	    
        for (idx = block.ffs(block.dirty, idx); (idx > 0) && (idx < lastImageBlockIdx); 
          idx = block.ffs(block.dirty, idx )) {

          int lastIdx = idx;
          
          addr = heapBase.add(idx << blockShift);
          addr = bcopier.w.walkRet( addr, addr.add(blockSize) );
    	  idx = addr.diff(heapBase).asInt() >> blockShift;
    	  
    	  rootPages += idx-lastIdx;
        }
	  
        /* we may have to scan the last image block, possibly not whole, as image may end before
           this block */
	      
        if ( (idx>0) && (idx==lastImageBlockIdx) && block.getBit(block.dirty, lastImageBlockIdx) ) {    
          bcopier.w.walk(heapBase.add(lastImageBlockIdx << blockShift),
            Native.getImageEndAddress());
        }
      } else {
          bcopier.w.walk(Native.getImageBaseAddress(), Native.getImageEndAddress());
      }
    }

    /** Base address of the block we are currently allocating from **/
    protected VM_Address blockPointer = null;
    /** offset within current block, or blockSize if blockPointer is null **/
    protected int blockOffset;

    int b2k(int b) { return (b << blockShift) >> 10; }

    protected void printStats() {
	if (verbose) {
	    if (pinCount > 0) 
		BasicIO.out.print(" pinned " + b2k(pinSize) + "k in " +
				  pinCount + " blocks");
	    if (movedBytes > 0)
		BasicIO.out.print(" logically copied " + (movedBytes>>10) + "k");
	    if (rootPages != 0)
		BasicIO.out.print(" " + b2k(rootPages) + "k of bootimage traversed");
	}
	pinCount = pinSize = 0;
	movedBytes = 0;
	rootPages = 0;
    }
    

    /**
     * Performs garbage collection.
     * This method provides atomicity through use of {@link PragmaAtomic}
     */
    public void garbageCollect() throws PragmaNoInline, PragmaAtomic {
        LocalReferenceIterator.the().prepareForGC();
    }

    /**
     * GC consists of three major steps.
     * <ol>
     * <li> {@link #markConservativeRoots},
     * <li> {@link #markPreciseRoots}, and
     * <li> {@link VecList#walkGrey}
     * </ol>
     * Once all blocks of to-space have been traversed, we can reuse
     * from-space and continue allocating.
     **/
    public void doGC() throws PragmaNoInline {
	inGC = true;
	if (DUMP_ON_GC) {
	    final ObjectCounter oc = new ObjectCounter();
	    block.walkWhite(new BlockWalker(oc));
	    if (verbose) {
		BasicIO.out.println("GC triggered");
	    }
	    oc.dumpStats();
	}
	blockPointer = null;
	blockOffset = blockSize;
	int liveBefore = allocated;
	allocated = 0;
	block.startGC(true);
	VM_Area.DestructorWalker updater =
	    heapArea.new DestructorUpdater();
	if (verbose) {
	    BasicIO.out.print("[Garbage collecting...");
	}
	long tmBegin = Native.getCurrentTime();
	
	markConservativeRoots();

	long tmPrePrecise = Native.getCurrentTime();
	markPreciseRoots();
	
	long tmPreWalkGrey = Native.getCurrentTime();
	block.walkGrey(bcopier);
	for (int kind = 0; kind < Destructor.N_KINDS; kind++){
	    VM_Address lastBlock = blockPointer;
	    int lastOffset = blockOffset;
	    int lastAlloc = allocated;
	    heapArea.walkDestructors(kind, updater);
	    if (allocated != lastAlloc || lastOffset != blockOffset) {
		// scan newly grey objects
		if (lastBlock != null)
		    // On the previously scanned block
		    copier.walk(lastBlock.add(lastOffset),
				lastBlock.add(blockSize));
		// and freshy copied ones
		block.walkGrey(bcopier);
	    }
	}

	block.endGC();
	inGC = false;

	if (!ZERO_TO_SPACE && blockPointer != null)
	    Mem.the().zero(blockPointer.add(blockOffset),
			   blockSize - blockOffset);
	if (protectFreePages)
	    block.walkFree(prot_none_protector);
	long tmEnd = Native.getCurrentTime();
	
	long thisTimeSpentInGC = tmEnd - tmBegin;
	long thisTimeSpentMarkingConsrvtvRoots = tmPrePrecise - tmBegin;
	long thisTimeSpentMarkingPreciseRoots = tmPreWalkGrey - tmPrePrecise;
	long thisTimeSpentWalkingGrey = tmEnd - tmPreWalkGrey;
	
	timeSpentInGC += thisTimeSpentInGC;
	timeSpentMarkingConsrvtvRoots += thisTimeSpentMarkingConsrvtvRoots;
	timeSpentMarkingPreciseRoots += thisTimeSpentMarkingPreciseRoots;
	timeSpentWalkingGrey += thisTimeSpentWalkingGrey;

	if (allocated < gcThreshold - 16) {
	    if (verbose) {
		BasicIO.out.print("reclaimed " + b2k(liveBefore - allocated) +
				  "k in " + thisTimeSpentInGC/1000000 + "ms ("
				  + b2k(allocated) + "k live)");
	    }
	    printStats();
	}
	if (verbose) {
	    BasicIO.out.println("]");
	}
    }
    
    public void vmShuttingDown() {
	if (verbose) {
	    BasicIO.out.println("Time spent in GC: Consrvtv: "+
				timeSpentMarkingConsrvtvRoots+
				"  Precise: "+timeSpentMarkingPreciseRoots+
				"  WalkGrey: "+timeSpentWalkingGrey+
				"  TOTAL: "+timeSpentInGC);
	}
    }
    
    private void exhausted() {
	    if (inGC)
		Native.abort();
	    if (dumpOOM) {
		inGC = true;
		final ObjectCounter oc = new ObjectCounter();
		block.walkWhite(new BlockWalker(oc));
		if (verbose) {
		    BasicIO.out.println("heap exhausted");
		}
		oc.dumpStats();
	    }
	    throw outOfMemory();
    }

    private VM_Address getHeapMemSlow(int size, boolean shouldClear) {
	VM_Address mem;

	if (size > blockSize) {
	    int nBlocks = (size + blockSize - 1) >> blockShift;
	    mem = getBlock(nBlocks);
	    if (mem == null)
		exhausted();
	    else if (ZERO_TO_SPACE || !inGC) {
		int alloced = nBlocks << blockShift;
		int toClear = size + 4 < alloced ? size + 4 : alloced;
		Mem.the().zero(mem, toClear);
	    }
	    // We can't continue allocating out of this block and hope
	    // to use card marking.  We also can't keep allocating
	    // from the old block, since it is no longer last in the
	    // Cheney scan.  So, we discard the remainder of both
	    // blocks.
	    blockPointer = null;
	    blockOffset = blockSize;
	}
	else {
	    mem = getBlock(1);
	    if (mem == null)
		exhausted();
	    else if (ZERO_TO_SPACE || !inGC)
		Mem.the().zero(mem, blockSize);
	    blockPointer = mem;
	    blockOffset = size;
	}
	return mem;
    }

    /** Return a pointer to an uninitialized chunk of garbage collected
        memory of size sz.  This memory will be zero filled unless all
        / of ZERO_TO_SPACE, inGC, and shouldClear are false.  Setting
        ZERO_TO_SPACE to false slows down allocation significantly.
        
        As long as ZERO_TO_SPACE is true, gcc should inline this method
        everywhere.  Given that it includes two getfields, a setfield,
        a call, and a branch, I'm not sure that j2c will ever think
        about inlining on its own.
    */
    VM_Address getHeapMem(int size, boolean shouldClear) {
	VM_Address mem;
	
	if (blockOffset + size <= blockSize) {
	    mem = blockPointer.add(blockOffset);
	    blockOffset += size;
	}
	else
	    mem = getHeapMemSlow(size, shouldClear);

	if (!ZERO_TO_SPACE && inGC) {
	    // Inside GC, make sure that the word following the last
	    // object copied is null (this terminates scanning of the
	    // block), and explicitly zero out memory allocated by new
	    if (shouldClear)
		Mem.the().zero(mem, size);

	    if (blockOffset != blockSize)
		// small object, not at end
		blockPointer.add(blockOffset).setAddress(null);
	    else if (blockPointer == null && (size & blockMask) != 0) {
		// large object not filling space
		VM_Address justPast = mem.add(size);
		justPast.setAddress(null);
	    }
	}
	return mem;
    }

    /** {@inheritDoc}
     * <p>Provides atomicity by using {@link PragmaAtomic}.
     */
    protected VM_Address getMem(int size) throws PragmaAtomic {
	return getHeapMem(size, true);
    }

    /**
     * Prevents the garbage collector from moving the given object by pinning
     * the page upon which it resides. This method executes atomically,
     * that is without any interruption, but is not safe to call from a thread
     * that has preempted the GC as the pinned block won't be seen by an in
     * progress GC pass.
     */
    public void pin(Oop oop) throws PragmaNoPollcheck {
	if (OVMBase.isBuildTime())
	    // Force pin in image
	    VM_Address.fromObject(oop).asInt();
	VM_Word offset = VM_Address.fromObject(oop).diff(heapBase);
	if (offset.uLT(VM_Word.fromInt(heapSize))) {
	    int idx = offset.asInt() >>> blockShift;
	    pinnedBlocks[idx]++;
	    // check for overflow
	    assert(pinnedBlocks[idx] > 0);
	}
    }

    /**
     * Allows the garbage collector to move the given object (by unpinning
     * its page. It is an error to call this if the object has not previously
     * been pinned. This method executes atomically, that is without any 
     * interruption, but if called from a thread that has preempted the GC, 
     * the unpinned block won't be seen as such by an in progress GC pass -
     * which is not in itself a harmful occurrence.
     */
    public void unpin(Oop oop) throws PragmaNoPollcheck {
	if (OVMBase.isBuildTime())
	    return;
	VM_Word offset = VM_Address.fromObject(oop).diff(heapBase);
	if (offset.uLT(VM_Word.fromInt(heapSize))) {
	    int idx = offset.asInt() >>> blockShift;
	    pinnedBlocks[idx]--;
	    assert(pinnedBlocks[idx] >= 0);
	}
    }

    /** Factory method to allow specialization of the heap area 
        by subclasses.
        NOTE: this is called prior to initialization of the subclass
        instance.
     */
    protected VM_Area makeHeapArea() {
        return new HeapArea();
    }

    protected class HeapArea extends VM_Area {
        public int size() { return gcThreshold << blockShift; }
        public int memoryConsumed() { return allocated << blockShift; }
        public String toString()throws PragmaNoPollcheck { return "Heap Area"; }
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
    }

    final protected VM_Area heapArea = makeHeapArea();

    public VM_Area getHeapArea() { return heapArea; }
    public boolean supportsDestructors() { return true; }
    
    public void enableSilentMode() {
	verbose=false;
    }

    public void enableAllDebug() {
	dumpOOM = true;
	protectFreePages = true;
	block.walkFree(prot_none_protector);
    }
    
    // this method must not modify references on the heap (or in the image)
    // (see updateReference)
    
    public void putFieldBarrier(CoreServicesAccess csa,
				Oop src, int offset, Oop tgt, int aSrc, int aTgt) 
        throws PragmaNoPollcheck, PragmaNoBarriers, PragmaInline, PragmaAssertNoExceptions {
        
	updateReference(csa, src, offset, tgt, aSrc, aTgt);
    }

    // this method must not modify references on the heap (or in the image)
    // (see updateReference)
    
    public void aastoreBarrier(CoreServicesAccess csa,
                               Oop src, int offset, Oop tgt, int aSrc, int aTgt) 
        throws PragmaNoPollcheck, PragmaNoBarriers, PragmaInline, PragmaAssertNoExceptions {
        
	updateReference(csa, src, offset, tgt, aSrc, aTgt);
    }

    public boolean needsWriteBarrier() {
      /* since we have a software image barrier, we need this */
      return NEEDS_IMAGE_BARRIER || FORCE_IMAGE_BARRIER;
    }


    void imageStoreBarrier( Oop src, int aSrc ) throws PragmaNoPollcheck, PragmaNoBarriers, PragmaInline,
      PragmaAssertNoExceptions, PragmaCAlwaysInline {

        if ( !((aSrc&Assert.HEAPONLY)!=0) && (NEEDS_IMAGE_BARRIER || FORCE_IMAGE_BARRIER) ) {

          int idx=0;
          idx = VM_Address.fromObject(src).diff(heapBase).asInt() >> blockShift;
          
          if ( ((aSrc&Assert.IMAGEONLY)!=0) || ((idx>=imageBaseIdx) && (idx<=lastImageBlockIdx)) ) {
              block.setBit(block.dirty,idx);
          }
        }      
    }

    // this method must not modify references on the heap (or in the image),
    // although it uses "PragmaNoBarriers", the barriers would still be inserted,
    // because we use forceBarriers() == true
    
    void updateReference(CoreServicesAccess csa, Oop src, int offset, Oop tgt, int aSrc, int aTgt) 
        throws PragmaNoPollcheck, PragmaNoBarriers, PragmaInline, PragmaAssertNoExceptions {
        
        VM_Address addr = VM_Address.fromObject(src).add(offset);
	addr.setAddress(VM_Address.fromObject(tgt));

	if ((aTgt&Assert.IMAGEONLY)==0) {
          // must be after the store because of initialization code	
          imageStoreBarrier(src,aSrc);
        }
    }
    
    public boolean forceBarriers() {
      return true;
    }

    public void setReferenceField( Oop object, int offset, Oop src ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      putFieldBarrier( null, object, offset, src, 0, 0 );
    }

    // FIXME: can we rewrite these methods not to make assertions about how individual primitive types are represented ?
    public void setPrimitiveField( Oop object, int offset, boolean value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
    
      VM_Address addr = VM_Address.fromObject(object);
      addr.add(offset).setInt( value ? 1 : 0 );
    }
    
    public void setPrimitiveField( Oop object, int offset, byte value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObject(object);
      addr.add(offset).setInt( value );
    }    

    public void setPrimitiveField( Oop object, int offset, short value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObject(object);
      addr.add(offset).setInt( value );
    }

    public void setPrimitiveField( Oop object, int offset, char value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObject(object);
      addr.add(offset).setInt( value );
    }

    public void setPrimitiveField( Oop object, int offset, int value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObject(object);
      addr.add(offset).setInt( value );
    }

    public void setPrimitiveField( Oop object, int offset, long value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObject(object);
      addr.add(offset).setLong( value );
    }
    
    public void setPrimitiveField( Oop object, int offset, float value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObject(object);
      addr.add(offset).setFloat( value );
    }
    
    public void setPrimitiveField( Oop object, int offset, double value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObject(object);
      addr.add(offset).setDouble( value );
    }

    public void setPrimitiveArrayElementAtByteOffset( Oop object, int byteOffset , int value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      setPrimitiveField( object, byteOffset, value );      
    }        

    public void setPrimitiveArrayElementAtByteOffset( Oop object, int byteOffset , char value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      VM_Address addr = VM_Address.fromObject(object);
      addr.add(byteOffset).setChar( value );
    }        

    public void setReferenceArrayElementAtByteOffset( Oop object, int byteOffset , Oop value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      setReferenceField( object, byteOffset, value );      
    }        

}

    
