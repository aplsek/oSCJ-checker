
// FIXME: we should be using atomic arrays.. 

// FIXME: we are not using prev pointers in Freelists - so we don't have to set them! (freeList.*)
//	we only use it in code that is checking if these pointers are set correctly
//	however, we may need these if we optimize some sweep code

// FIXME: the core is becoming reliant on a particular object model
//	and bypassing the object model functions to access the object headers

// FIXME: the verification code (compaction) is very model dependent
// 	if verification fails, check the verification code, first
//	most of bugs I found was in the verification code as opposed to
//	the real code

// FIXME: PragmaNoReadBarriers is used kind of randomly

package s3.services.memory.triPizlo;

import ovm.core.execution.*;
import ovm.core.services.memory.*;
import ovm.core.services.threads.OVMThread;
import ovm.services.threads.PriorityOVMThread;
import ovm.services.java.JavaDispatcher;
import ovm.services.threads.UserLevelThreadManager;
import ovm.core.services.timer.TimerInterruptAction;
import ovm.core.services.timer.TimerManager;
import ovm.core.stitcher.JavaServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.core.stitcher.TimerServicesFactory;
import ovm.core.domain.*;
import ovm.core.*;
import ovm.util.*;
import s3.util.*;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.services.bootimage.*;
import ovm.services.monitors.MonitorMapperNB;
import ovm.services.monitors.MonitorMapper;
import s3.core.services.memory.*;
import ovm.core.repository.JavaNames;
import java.io.*;
import s3.services.memory.conservative.ConservativeLocalReferenceIterator;
import s3.core.domain.MachineSizes;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3JavaUserDomain;
import ovm.services.monitors.Monitor;



/** implementation of a tricolor collector.  eventually this'll be
    a production-worthy real-time GC.  for now it's just a pathetic
    hack. */
public class TheMan
    extends MemoryManager
    implements ImageAllocator.Implementation,
	       NativeConstants, TimerInterruptAction {
    
    static final boolean DEBUG_BUILD=true;
    static final boolean DEBUG_INIT=true;
    static final boolean DEBUG_ALLOC_SLOW=false;
    static final boolean DEBUG_ALLOC_SLOW_VERBOSE=false;
    static final boolean DEBUG_ALLOC_FAST=false;
    static final boolean DEBUG_ALLOC_FAST_VERBOSE=false;
    static final boolean DEBUG_GC=false;
    static final boolean DEBUG_WALK_IMAGE=false;
    static final boolean DEBUG_WALK=false;
    static final boolean DEBUG_WALK_VERBOSE=false;
    static final boolean DEBUG_MARK=false;
    static final boolean DEBUG_MARK_VERBOSE=false;
    static final boolean DEBUG_SWEEP=false;
    static final boolean DEBUG_EXHAUSTED=false;
    static final boolean DEBUG_DUMP_HEAP=false;
    static final boolean DEBUG_PAUSE=false;
    static final boolean DEBUG_TIMER=false;
    static final boolean DEBUG_TIMER_VERBOSE=false;
    static final boolean DEBUG_PIN=false;
    static final boolean DEBUG_FREE=false;
    static final boolean DEBUG_WEIRD=false;
    static final boolean DEBUG_BARRIER=false;
    static final boolean DEBUG_INTERRUPTIBLE_STACK_SCANNING=false;
    static final boolean DEBUG_COMPACTION=false;
    static final boolean DEBUG_MONITORS=false;
    static final boolean DEBUG_ARRAYLETS=false;
    static final boolean DEBUG_POLLING=false;
    static final boolean DEBUG_BUG=false;

    static boolean debugBuild=true;
    static boolean debugInit=true;
    static boolean debugAllocSlow=true;
    static boolean debugAllocSlowVerbose=true;
    static boolean debugAllocFast=true;
    static boolean debugAllocFastVerbose=true;
    static boolean debugGC=true;
    static boolean debugWalkImage=true;
    static boolean debugWalk=true;
    static boolean debugWalkVerbose=true;
    static boolean debugMark=true;
    static boolean debugMarkVerbose=true;
    static boolean debugSweep=true;
    static boolean debugExhausted=true;
    static boolean debugDumpHeap=true;
    static boolean debugPause=true;
    static boolean debugTimer=true;
    static boolean debugTimerVerbose=true;
    static boolean debugPin=true;
    static boolean debugFree=true;
    static boolean debugWeird=true;
    static boolean debugBarrier=true;
    static boolean debugCompaction=true;
    static boolean debugMonitors=true;
    static boolean debugArraylets=true;

    static final boolean PROFILE_ALLOC=false;
    static boolean profileAlloc=false;
    
    static final boolean PROFILE_BLOCK_USAGE = false;
    static final boolean PROFILE_MEM_USAGE = false;  // has runtime overhead !!!
    static final boolean FULL_MEM_PROFILE = false;
    static final boolean PERIODIC_MEM_PROFILE = false;  // report every memTracePeriod -th sample

    static final boolean MAX_LIVE_MEM_PROFILE = false; // this is inaccurate - reports memory usage after sweep
    static final boolean MAX_USED_MEM_PROFILE = false; // this is accurate
    
    static final boolean PROFILE_MEM_FRAGMENTATION = false;
    
    static final int memTracePeriod = 500; // collect every memTracePeriod's sample
    static final boolean USE_REFERENCE_MEMORY_SIZE = true; // uses memory size of objects as in base OVM (no arraylets, ...)
                                                         // for memory tracing
    static boolean profileMemUsage = false; // keep false, enable by -profile-mem-usage

    static final boolean REPORT_FRAGMENTATION=false; // outdated... needs rewriting/checking
    static final boolean reportFragmentation=false;  // outdated, don't use
    
    static final boolean ALLOC_STATS=false;
    static boolean allocStats=false;

    static final boolean PROFILE_GC_PAUSE = false;
    static final boolean PAUSE_ONLY = true;
    static boolean timeProfile = false;
    static boolean vmIsShuttingDown = false;

    static final boolean PROFILE_TIMER_INTERRUPTS = false; // for periodic scheduling
    static final int maxArrivalTimes = 200;
    
    static final boolean EXCLUDED_BLOCKS = false; // for debugging with periodic (hybrid) scheduling
                                                 // the runtime GC verification (atomic) is not included into the schedule

    static final boolean VERIFY_BUG=false;
    static final boolean VERIFY_ALLOCATION=false;
    static final boolean VERIFY_ARRAYLETS=false;
    static final boolean VERIFY_ARRAYCOPY=false;
    static final boolean VERIFY_ASSERTIONS=false;
    static final boolean VERIFY_PINNING=false;    
    static final boolean VERIFY_COMPACTION=false;  // some of the checks (like the checkReferees in sweep) may stop progress
    static final boolean VERIFY_HEAP_INTEGRITY=false;
    static final boolean VERIFY_IMAGE_INTEGRITY=false;
    static final boolean VERIFY_REPLICAS_SYNC=false;
    static final boolean VERIFY_SWEEP=false;
    static final boolean VERIFY_MARK=false;
    static final boolean VERIFY_HI_PRI_ALLOC=false; // probably broken, because gcStopped is not honored by pollLat.t?c FIXME
    static final boolean VERIFY_FAST_IMAGE_SCAN=false;  // I think this requires disabling concurrency, but, then there seems to be no progress
    static final boolean VERIFY_MEMORY_ACCESSES=false;  // this is really slow
    static final boolean VERIFY_MEMORY_PROFILE=false;
    static final boolean VERIFY_POLLING=false;

    static final boolean REPORT_LONG_LATENCY=false; // for debugging
    static final boolean REPORT_LONG_PAUSE=false; // for debugging !!! NEEDED PROFILE_GC_PAUSE and -gc-enable-time-trace
    
    static final boolean DONT_MOVE_BYTE_ARRAYS = false; // for debugging - it makes compaction ineffective
    static final boolean SLOW_PINNING = false; // slower (but safer) implementation of pinning - use when bug hunting
    static final boolean KEEP_INFLATED_MONITORS = false; // just for debugging - inflated monitors are kept reachable forever
    static final boolean IGNORE_ASSERTIONS = false; // just for debugging - assertions are ignored (assumed we know nothing)
    static final boolean PARANOID_ARRAYCOPY_MARKING = false; // just for debugging - mark all (yuasa) pointers in target space at start of arraycopy
    static final boolean KEEP_OLD_BLUEPRINT = false; // just for debugging - keeps blueprint of the original object when freeing, so that when the
                                                    // object is incorrectly re-used, we know which type has not been marked    
    static final boolean COMPACTION = true;
      // activates the actual compaction - with moving objects
      // activates the updating of heap, stack, image that is needed when objects have multiple locations
      // requires exactly one of BROOKS, REPLICATING to be set

    static final boolean BROOKS = false;
    static final boolean REPLICATING = true; // BROOKS and REPLICATING are mutually exclusive
                // when no compaction or debugging needed, better keep off
                // it should not matter, but there is some measurable perfomance difference... why?!
                
      // selects the way the forwarding pointer is interpreted and used
      // needs either COMPACTION or FORCE_XXX_BARRIER to be set in order to have any efect
      // (does not activate the actual compaction)
      
    static final boolean PREDICTABLE_BARRIER = true; // if both Dijkstra and Yuasa barriers are off, there should be no difference between predicatble and
                                                     // non-predictable
    static final boolean INCREMENTAL_STACK_SCAN = true; //should be turned off when stop-the-world
    static final boolean FAST_IMAGE_SCAN = true;

    static final boolean NEEDS_DIJKSTRA_BARRIER = INCREMENTAL_STACK_SCAN;
    static final boolean NEEDS_YUASA_BARRIER = true; // must be stop the world if to disable
    static final boolean NEEDS_IMAGE_BARRIER = FAST_IMAGE_SCAN;
      /* needs translating barrier = COMPACTION */
    static final boolean FORCE_DIJKSTRA_BARRIER=false;
    static final boolean FORCE_TRANSLATING_BARRIER=false; 
      // needs exactly one of BROOKS, REPLICATING to be set
      
    static final boolean FORCE_YUASA_BARRIER=false;
    static final boolean FORCE_IMAGE_BARRIER=false;

    static final boolean INCREMENTAL_OBJECT_COPY=false;
      // only has effect with REPLICATING BARRIER and COMPACTION
      // makes copying of an object in replication interruptible
    
    static final boolean ARRAYLETS = true;
    
    static final boolean EFFECTIVE_MEMORY_SIZE = true; // allow to lower the memory size for a particular run
                                                    // debugging (what's the smallest amount of memory the application could run with ?)
                                                    // it's about the same as detecting maximum block usage, except for that
                                                    //  - it influences GC triggers
                                                    //  - you can find out there is not enough memory faster (panic)


    static final boolean PERIODIC_TRIGGER = false; // determines how often it is checked that the amount of free memory dropped enough to run gc
                                                  // true ... checking periodically 
                                                  // false .. checking in allocation and waking up gc immediatelly if it is allowed to run
                                                  //	      (should be used for stop-the-world, Hendrickson mode, and possibly Metronome as well)
                                                  //
                                                  // can be used even with Hendrickson, if desired
                                                  
    static final boolean SYNCHRONOUS_TRIGGER = true; // checks amount of free memory after each allocation
                                                     // at least one trigger has to be activated for the GC to work
                                                                                            
                                   
    static final boolean SUPPORT_MUTATOR_DISABLE = false;
                                                            
    static final boolean SUPPORT_PERIODIC_SCHEDULER = true;
                                                  // metronome style scheduling
                                                  //	false ... Hendrickson style
    static final boolean SUPPORT_APERIODIC_SCHEDULER = true;
                                                  // Hendrickson style 
    static final boolean SUPPORT_HYBRID_SCHEDULER = true;  
                                                  // periodic changes of 2 priorities
                                                  //	if metronome blocks the gc thread, hybrid only sets its priority
                                                  //	if metronome wakes up the thread, hybrid only boosts its priority
                                                      
    static boolean periodicScheduler = true;
    static boolean hybridScheduler = false;
    static boolean aperiodicScheduler = false;
    
    static boolean disableConcurrency = false; // FIXME: doesn't have good semantics now, false is the normal setting
    static boolean abortOnGcReentry = false;
    static boolean stackUninterruptible = false;
    static boolean markUninterruptible = false;
    static boolean sweepUninterruptible = false;
    static boolean compactUninterruptible = false;
    static boolean logUninterruptible = false;
    
    static long longLatency = 0;

    static long maxObservedLatency = 0;
    static int maxObservedLatencyLineFrom = 0;
    static int maxObservedLatencyLineTo = 0;
    
    static long longPause = 0;
    static long maxObservedPause = 0;
    static int maxObservedPauseLine = 0;
    
    static boolean[] interruptibilityMask = new boolean[50];
    
    static boolean useImageBarrier;
    
    static VM_Address walkObjectPointerSource;
    static boolean fastImageScanVerificationActive=false;
    static boolean fastImageScanFailed=false;

    static volatile int finishedSweeps = 0; // just debugging
    static volatile int finishedCompactions = 0; // just debugging

    
    static void setAllDebug(boolean value) {
	debugBuild=value;
	debugInit=value;
	debugAllocSlow=value;
	debugAllocSlowVerbose=value;
	debugAllocFast=value;
	debugAllocFastVerbose=value;
	debugGC=value;
	debugWalkImage=value;
	debugWalk=value;
	debugWalkVerbose=value;
	debugMark=value;
	debugMarkVerbose=value;
	debugSweep=value;
	debugExhausted=value;
	debugDumpHeap=value;
	debugPause=value;
	debugTimer=value;
	debugTimerVerbose=value;
	debugPin=value;
	debugFree=value;
	debugWeird=value;
	debugBarrier=value;
    }
    
    static void enableSilentModeImpl() {
	setAllDebug(false);
    }
    
    public void enableSilentMode() {
	enableSilentModeImpl();
    }

    static void enableAllDebugImpl() {
	setAllDebug(true);
    }

    public void enableAllDebug() {
	enableAllDebugImpl();
    }

    static final class Nat implements NativeInterface {
/*	static native void mc_barrier_init(int imageEnd,
					   int[] continued,
					   int[] dirty); */
	
	// we use this directly when we are zeroing less than a block
	// at a time
	static native void bzero(VM_Address addr, int nb);
	
        static native void memcpy(VM_Address to, VM_Address from, int nb);
        static native void memmove(VM_Address to, VM_Address from, int nb);        
    }

    static void arrayCopyBoundsCheck( Oop array, int fromElement, int nElements ) {
      if (nElements==0) {
        return ;
      }
      
      int length = array.getBlueprint().asArray().getLength(array);
      int toElement = fromElement + nElements - 1;
      
      if ( (fromElement < 0) || (fromElement>=length) || (toElement < 0) || (toElement>=length) ) {
        Native.print_string("ERROR: array index out of bounds during rray copy\n");
        Native.print_string("ERROR: this should not happen ! it should have been checked at higher levels\n");
        Native.print_string("ERROR: array is ");
        printAddr(VM_Address.fromObject(array));
        Native.print_string(" fromElement=");
        Native.print_int(fromElement);
        Native.print_string(" toElement=");
        Native.print_int(toElement);        
        Native.print_string(" nElements=");
        Native.print_int(nElements);
        Native.print_string(" array length=");
        Native.print_int(length);
        Native.print_string("\n");
        throw Executive.panic("fix");
      }
    }

//    static int checkcount = 0;
    static void arrayBoundsCheck( Oop array, VM_Address addr, int nb ) {
    
  //    checkcount++; //1000
  //    if (checkcount < 100000) return ;
  //    Native.print_string("B");
      
      if (nb==0) return;
      
      enterExcludedBlock();
      
      Blueprint.Array bp = (Blueprint.Array) array.getBlueprint();
      int boffset = bp.byteOffset(0);
      int csize = bp.getComponentSize();
      int len = bp.getLength(array);
      int eoffset = boffset + len*csize - 1 ;

      int arrayStart = VM_Address.fromObjectNB(array).asInt();
      int arrayEnd = arrayStart + eoffset;

      int forwardedArrayStart = translateNonNullPointer(VM_Address.fromObjectNB(array)).asInt();
      int forwardedArrayEnd = forwardedArrayStart + eoffset;
      
      int ab = addr.asInt();
      int ae = ab+ nb-1;
      
      if ( (ab < arrayStart || ab > arrayEnd || ae < arrayStart || ae > arrayEnd) &&
           (ab < forwardedArrayStart || ab > forwardedArrayEnd || ae < forwardedArrayStart || ae > forwardedArrayEnd)) {

        Native.print_string("Array copy - bounds error\n");
        Native.print_string("array content first byte: ");
        Native.print_hex_int(arrayStart);
        Native.print_string(" other replica: ");
        Native.print_hex_int(forwardedArrayStart);
        Native.print_string("\n");
        Native.print_string("array content last byte: ");
        Native.print_hex_int(arrayEnd);
        Native.print_string(" other replica: ");
        Native.print_hex_int(forwardedArrayEnd);
        Native.print_string("\n");
        Native.print_string("array access address (first byte): ");
        Native.print_ptr(addr);
        Native.print_string("array access address (last byte): ");
        Native.print_ptr(addr.add(nb-1));        
        Native.print_string("\n");
        
        throw Executive.panic("Error");
      }
      
      leaveExcludedBlock();
    }

    static void checkedMemcpy(Oop toArray, Oop fromArray, VM_Address to, VM_Address from, int nb) throws PragmaInline {
    
      if (VERIFY_ARRAYCOPY && !ARRAYLETS) {
        arrayBoundsCheck(toArray, to, nb);
        arrayBoundsCheck(fromArray, from, nb);
      }
      
      if ( (VERIFY_MEMORY_ACCESSES) && (nb>0)) {
        verifyMemoryAccess( to );
        verifyMemoryAccess( from );
        verifyMemoryAccess( to.add(nb-1));
        verifyMemoryAccess( from.add(nb-1));
      }
    
      Nat.memcpy(to, from, nb);
    }

    static void checkedMemmove(Oop array, VM_Address to, VM_Address from, int nb) throws PragmaInline {
    
      if (VERIFY_ARRAYCOPY && !ARRAYLETS) {
        arrayBoundsCheck(array, to, nb);
        arrayBoundsCheck(array, from, nb);
      }

      if ( (VERIFY_MEMORY_ACCESSES) && (nb>0)) {
        verifyMemoryAccess( to );
        verifyMemoryAccess( from );
        verifyMemoryAccess( to.add(nb-1));
        verifyMemoryAccess( from.add(nb-1));
      }

      Nat.memmove(to, from, nb);
    }

    private static boolean inited=false;

    // design notes:
    // this is a snapshot-at-the-beginning collector that marks new
    // objects black.  I use the Technique #6 barrier that shades the
    // old referrent on a write.

    // implementation notes.  what needs to be done:
    // 1) free lists.  manage memory using that style where there are
    //    blocks and each block is dedicated to a particular size.
    // 2) I suppose I need to manage these blocks.
    // 3) write barriers.  I need a write barrier somehow.  not sure
    //    how.
    // 4) some manner of a worklist for when we're shading stuff

    // how we're going to do it:
    // 1) well, I suppose the object header is big enough that a free
    //    object has plenty of room for a 'next' pointer
    // 2) blocks can be managed with a huge bitvector
    // 3) this needs to return 'true' in needsWriteBarrier() and then
    //    it needs to implement putFieldBarrier() and aastoreBarrier().
    // 4) do it stupid way for now
    
    static final int imgPageSize = PAGE_SIZE;
    static int lastImagePageIdx = -1;
    
    // this is just what getImageBaseAddress() returns - but we want it in a static context
    static int imageBaseAddress;
    
    static VM_Address nativeImageBaseAddress;
    static VM_Word imageSize;

    static final int blockSize = 2048;
    static final int arrayletSize = blockSize; // the code depends on this 

    
    // fraction multNum/multDen that determines the geometric sequence
    // used for allocation sizes
    static final int multNum = 9; 
//    static final int multNum = 2; 
    static final int multDen = 8;
//    static final int multDen = 1;

    static final int forwardOffset = ObjectModel.getObjectModel().getForwardOffset();
    static final int monitorOffset = ObjectModel.getObjectModel().getMonitorOffset();
    
    static final int wordSize = VM_Word.widthInBytes();
    static final int alignment = 16; // the size and address of all objects must be a multiple of this number
    static final int minSize = 16;
    
    static final int maxBlockAlloc = computeMaxBlockAlloc();
    static final int sizeClassIndices = sizeClassIdx(maxBlockAlloc)+1;

    /** means that the object is untouched by GC, so cannot be assumed to
	be either available for freelisting or reachable. */
    static int WHITE = 3;
    
    /** means that the object is available for putting onto a free list */
    static final int FREE = 1;
    
    /** means that the object is marked and so is definitely reachable and
	may be on the worklist. */
    static int GREYBLACK = 2;
    
    /** means that the object is pinned.  we rely on this to be 0, so that
	an object stamped with a blueprint but untouched by GC will look to
	the GC as if it were pinned. */
    static final int FRESH = 0;
    
    static int memSize; // in bytes, set at VM build time (image creation, ...)
    static int effectiveMemSize; // for debugging, set at VM start time 
                                 // must not be larger than memSize
                                 // see EFFECTIVE_MEMORY_SIZE
    
    static int gcThreshold; // in blocks

        // do compaction if the number of free blocks(pages) is <= compactionThreshold
    static int compactionThreshold = -1; // set in code below
    static int mutatorDisableThreshold = -1; // set in code below

    static int fixedHeap; // -1 means heap is not fixed
    static int maxAtomicRefCopy;
    static int maxAtomicRefCopyHalf;
    static int maxAtomicBytesCopy;
    static int maxAtomicBytesCopyHalf;    
    static VM_Address memory;

    static boolean lastCycleEvacuatedObjects = false;
    static boolean replicasShouldBeInSync = true;
    
    // double indirection because multiple indices may give the same
    // size class
    static class SizeClass {
	int size;
	
	  // fragmentation info
	int nObjects; // number of allocated objects
	int nBlocks; // number of blocks that this class spans
	
	int nonfullHead; // index of the first nonfull block used by this size class
	int nFreeObjectsInEvacuatedBlocks; // used only during compaction
	boolean compacted; // used only during compaction
	
	int nBlocksPinned;
	  // number of blocks with at least one pinned object
	
	int padding;
	
	VM_Address block;
	VM_Address blockCur;
	
	SizeClass(int size) {
	    this.size=size;
	    this.nonfullHead=-1;
	    this.block=null;
	    this.blockCur=null;
	    this.nObjects=0;
	    this.nBlocks=0;
	    this.nFreeObjectsInEvacuatedBlocks=0;
	    this.padding = blockSize % size;
	}
    }
    
    static int[] largeBits;
    static int[] arrayletBits;
    static int[] usedBits;
    static int[] sizes;
    static int[] uses; // only valid for blocks used for small objects
    
    static int[] checkedBits;

      // free pages
    static int[] blockPrev;
    static int[] blockNext;
    static volatile int blockHead;
    
     // non-free pages with some free space
     // heads are in individual size classes
    static int[] nonfullPrev;
    static int[] nonfullNext;

     // heads of lists of nonfull blocks (pages)
     // a special list for each size - for bucket sorting in compaction
    static int[] nonfullHeads;
    static int[] nonfullTails;
    static final int maxOccupancy = blockSize/minSize ;
    
     // heads of list of free blocks within a page
     // null is indicator of nonnull block
    static int[] freeHeadsPtrs;
    
     // number of pinned objects in each block
    static int[] nPinnedObjects;

    static int nImgBits;
    static int nImgBitWords;
    static int[] continued;
    static int[] dirty;
    static volatile VM_Address dirtyValuesStart = VM_Address.fromObjectNB(null);
    
    transient boolean lastBig;
//    transient PrintWriter params;

    static volatile int nUsedBlocks; // this is essential; it is used by the GC logic.  this is also always what is reported when the memoryConsumed() method is called.
    
    static volatile int memUsage; // only maintained if PROFILE_MEM_USAGE is set, and then it is only used for the mem usage trace.

    static int maxMemUsage = 0; // only used if MAX_USED_MEM_PROFILE
    static int maxUsedBlocks = 0;
    
    static int liveMemUsage = 0; // only used if MAX_LIVE_MEM_PROFILE
    static int liveBlockUsage = 0; 
    
    static int memSmallObjectFragmentation = 0;
    static int memSizeClassFragmentation = 0;
    static int memLargeObjectFragmentation = 0;
    static int memRecentlyDeadFragmentation = 0; 
    static int memLongDeadFragmentation = 0;
    static int memInReplicas = 0;

    static int nBlocks;
    static int effectiveNBlocks;
    static volatile int highestUsedBlock = 0;
    
    static int nBitWords;
    static int nObjBitWords;
    static volatile int currentNBitWords = 0;
    
    static SizeClass[] sizeClassBySize;
    static SizeClass[] sizeClasses;
    
    static int[] sizeHisto;
    static int sizeOverflow;

    static volatile boolean collecting;
    static volatile boolean marking;
    static volatile boolean dijkstraBarrierOn = false;
    static volatile boolean updatingPointersWrittenToHeapNeeded = false;
    static volatile boolean shouldPause = false;
    static volatile boolean shouldFreeSomeMemory = false;
    static volatile boolean shouldDisableMutator = false;
    static volatile boolean threadRunning;
    static volatile boolean gcStopped=true;

    static volatile int allocColor;
    
    static VM_Address worklist;
    static int worklistSize;
    static int worklistBits;
    static int worklistPuti;
    static int worklistGeti;
    static boolean worklistEmpty=true;

    static int marked;
    static int waited;
    

    static /*final*/ Blueprint.Array intArray;
    static Blueprint.Array byteArray;
    static Blueprint.Array longArray;
    
    private static void updateHighestUsedBlock(int bIdx) throws PragmaAssertNoExceptions {
    
      if (bIdx>highestUsedBlock) {
        highestUsedBlock = bIdx;
        currentNBitWords = ((highestUsedBlock+31)/32); 
      }
    }
    
    private static void initLate() throws BCdead {
	try {
	    Domain ed = DomainDirectory.getExecutiveDomain();
	    Type.Context stc = ed.getSystemTypeContext();
	    Type intArrayT = stc.typeFor(JavaNames.arr_int);
	    intArray = (Blueprint.Array) ed.blueprintFor(intArrayT);
	    Type byteArrayT = stc.typeFor(JavaNames.arr_byte);
	    byteArray = (Blueprint.Array) ed.blueprintFor(byteArrayT);	    
	    Type longArrayT = stc.typeFor(JavaNames.arr_long);
	    longArray = (Blueprint.Array) ed.blueprintFor(longArrayT);	    	    
	} catch (LinkageException e) {
	    throw e.unchecked();
	}
    }

    static class Area extends VM_Area {
	public int size() { 
          if (EFFECTIVE_MEMORY_SIZE) {
  	    return effectiveMemSize;
  	  } else {
  	    return memSize;
          }
        }
        
	public int memoryConsumed() { return nUsedBlocks*blockSize; }
	public void addDestructor(Destructor d) {}
	public void removeDestructor(Destructor d) {}
	public Oop revive(Destructor d) { throw Executive.panic("revive() not implemented"); }
	public int destructorCount(int kind) { return 0; }
	public void walkDestructors(int kind, DestructorWalker w) {}
	public void walkDestructors(int kind, DestructorWalker w, int max) {}
    }

    static Area heap=new Area();
    
    static int inBlocks(int n) {
	return (n+blockSize-1)/blockSize;
    }
    
    static int blockRoundUp(int n) {
	return inBlocks(n)*blockSize;
    }

    static int inAlignments(int n) {
	return (n+alignment-1)/alignment;
    }
    
    static int alignmentRoundUp(int n) {
	return inAlignments(n)*alignment;
    }
    
    static int sizeClassIdx(int n) {
	return n/alignment;
    }
    
    static int iterateSize(int size) {
	size*=multNum;
	size/=multDen;
	size=(size+alignment-1)&~(alignment-1);
	return size;
    }
    
    // slow, don't use too much
    static int allocSize(int n) {
	int size=minSize;
	while (size<n) {
	    size=iterateSize(size);
	}
	return size;
    }
    
    static int computeMaxBlockAlloc() {
	int size=minSize;
	while (blockSize/iterateSize(size) >= 1) {
	    size=iterateSize(size);
	}
	return size;
    }
    
    static int offset(VM_Address ptr)
	throws PragmaNoPollcheck {
	return ptr.diff(memory).asInt();
    }
    
    static int blockIdxOff(int offset)
	throws PragmaNoPollcheck {
	return offset/blockSize;
    }
    
    static int blockIdx(VM_Address ptr)
	throws PragmaNoPollcheck {
	return blockIdxOff(offset(ptr));
    }
    
    static class Timer {
	String name;
	
	long begin;
	
	long lost;
	long lat;
	
	Timer(String name) {
	    this.name=name;
	}
	
	void ticHook(long curTime) {}
	
	final void tic() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
	    tic(getCurrentTime());
	}

	final void tic(long curTime) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {

	    if (DEBUG_TIMER_VERBOSE && debugTimerVerbose) {
		Native.print_string("triPizlo: TIMER_VERBOSE: in ");
		Native.print_string(name);
		Native.print_string(": recorded tic = ");
		Native.print_long(curTime);
		Native.print_string("\n");
	    }
	    ticHook(curTime);
	    begin=curTime;
	    lost=0;
	}	
	
	void tocHook(long curTime) {}
	
	final void toc() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
	  toc(getCurrentTime());
	}

	final void toc(long curTime) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {

	    if (DEBUG_TIMER_VERBOSE && debugTimerVerbose) {
		Native.print_string("triPizlo: TIMER_VERBOSE: in ");
		Native.print_string(name);
		Native.print_string(": recorded toc = ");
		Native.print_long(curTime);
		Native.print_string("\n");
	    }
	    tocHook(curTime);
	    lat=curTime-begin;
	}
	
	void lose(long lat) throws PragmaAssertNoExceptions {
	    lost+=lat;
	}
	
	void toc(String text) throws PragmaAssertNoExceptions {
	    toc();
	    if (DEBUG_TIMER && debugTimer) {
		Native.print_string("triPizlo: TIMER: ");
		Native.print_string(text);
		Native.print_string(" in ");
		Native.print_long(lat);
		Native.print_string(" ns (");
		Native.print_long(lat-lost);
		Native.print_string(" ns not sleeping).\n");
	    }
	}
	
	void dump() {}
    }
    
    static class NumberLog {
        long[] buffer;
        
	volatile int n;
	int dumpAt;
	byte[] filename;
	
	static byte[] appendMode=new byte[]{'a',0};
	
	NumberLog(String filename, int size) {
	    //this.buffer = new long[size];
	    this.buffer = allocLongArrayRaw(size); 
	    dumpAt = (2*size)/3;
	    this.filename=Native.Utils.string2c_string(filename);
	}
	
	NumberLog(String filename) {
	  this(filename, 100000);
	}
	
	void dump() throws PragmaNoPollcheck {
	    int floutInt=Native.fopen(filename,appendMode);
	    if (floutInt!=0) {
		Native.FilePtr flout=new Native.FilePtr(floutInt);
		for (int i=0;i<n;++i) {
		    pollLogging();
		    Native.print_long_on(flout,buffer[i]);
		    pollLogging();
		    Native.print_string_on(flout,"\n");
		}
		Native.fclose(flout);
	    }
	    n=0;
	}

	void log(long value) {
	    buffer[n++]=value;
	    if (n>=dumpAt) {
		dump();
	    }
	}
    }
    
    static class IntNumberLog {
	int[] buffer;
        
	volatile int n;
	int dumpAt;
	
	byte[] filename;
	
	static byte[] appendMode=new byte[]{'a',0};
	
	IntNumberLog(String filename, int size) {
//	    this.buffer = new int[size];
            this.buffer = allocIntArrayRaw(size);
	    dumpAt = (2*size)/3;	    
	    this.filename=Native.Utils.string2c_string(filename);
	}
	
	IntNumberLog(String filename) {
	    this(filename, 100000);
	}
	
	void dump() throws PragmaNoPollcheck {
	    int floutInt=Native.fopen(filename,appendMode);
	    if (floutInt!=0) {
		Native.FilePtr flout=new Native.FilePtr(floutInt);
		for (int i=0;i<n;++i) {
		    pollLogging();
		    Native.print_int_on(flout,buffer[i]);
		    pollLogging();
		    Native.print_string_on(flout,"\n");
		}
		Native.fclose(flout);
	    }
	    n=0;
	}

	void log(int value) {
	    buffer[n++]=value;
	    if (n>=dumpAt) {
		dump();
	    }
	}
    }
    
    static class LoggingTimer extends Timer {
	NumberLog ticLog;
	NumberLog tocLog;
	
	LoggingTimer(String name,String filename, int size) {
	    super(name);
	    ticLog=new NumberLog("tic_"+filename,size);
	    tocLog=new NumberLog("toc_"+filename,size);
	}
	
	LoggingTimer(String name, String filename) {
	  this(name, filename, 100000);
	}
	
	void ticHook(long curTime) {
	    ticLog.log(curTime);
	}
	
	void tocHook(long curTime) {
	    tocLog.log(curTime);
	}
	
	void dump() {
	    ticLog.dump();
	    tocLog.dump();
	}
    }
    
    static long totalGCTime=0;
    static int totalMovedObjects=0;
    static int totalMovedBytes=0;
    static Timer outer=new Timer("outer");
    static Timer inner=new Timer("inner");
    static Timer pollLat=new Timer("pollLat");
    static Timer stackScan=new Timer("stackScan");
//    static Timer threadScan=new Timer("threadScan");
    static Timer imageScan=new Timer("imageScan");    
    static Timer heapScan=new Timer("heapScan");        
    static Timer monitorScan=new Timer("monitorScan");            
    static Timer conservativeScan=new Timer("conservativeScan");    
    static Timer preciseScan=new Timer("preciseScan");        
//    static Timer prepareScanA=new Timer("prepareScanA");            
//    static Timer prepareScanB=new Timer("prepareScanB");            
    static Timer markTimer=new Timer("mark");
    static Timer stackUpdateTimer=new Timer("stackUpdate");
    static Timer sweepTimer=new Timer("sweep");
    static Timer compactionTimer=new Timer("compaction");
    static Timer sortTimer=new Timer("sort");
//    static Timer copyTimer=new Timer("copy");
    static Timer debug=new Timer("debug");
    
    static NumberLog allocTimestamp;
    static NumberLog allocSize;
    
    static NumberLog memUsageTimestamp; // anything below changed
    static IntNumberLog blockUsageValue; // updated during allocation, sweep
    static IntNumberLog memUsageValue; // updated during allocation, sweep
    static IntNumberLog memSmallObjectFragmentationValue; // padding of small objects
    static IntNumberLog memSizeClassFragmentationValue; // padding in blocks of a specific size class when the size is not divisible
                                                        // by allocation size
    static IntNumberLog memLargeObjectFragmentationValue; // padding of large objects
    static IntNumberLog memRecentlyDeadFragmentationValue; // FREE small objects up to the next sweep -- not really fragmentation
    static IntNumberLog memLongDeadFragmentationValue; // FREE small objects after next sweep -- this is already fragmentation
    static IntNumberLog memInReplicasValue; // memory in replicas of small objects (in padded size)
    
    static GCData.Model gcData;
    static FreeList freeList;
    
    static boolean noBarrier;
    
    public TheMan(String memSize,
		  String gcThreshold,
		  String fixedHeap,
		  String noBarrier) {
	this(CommandLine.parseSize(memSize),
	     CommandLine.parseSize(gcThreshold),
	     CommandLine.parseAddress(fixedHeap),
	     noBarrier!=null);
    }
    
    public TheMan(int memSize,
		  int gcThreshold,
		  int fixedHeap,
		  boolean noBarrier) {
	if (inited) {
	    throw new Error("Already have a triPizlo.TheMan");
	}
	inited=true;
	
	ImageAllocator.override(this);
	
	// stupid
	nativeImageBaseAddress=VM_Address.fromInt(0);
	imageSize=VM_Word.fromInt(0);
	
	gcData=(GCData.Model)ObjectModel.getObjectModel();
	freeList=(FreeList)ObjectModel.getObjectModel();

	TheMan.noBarrier=noBarrier;
	TheMan.memSize=blockRoundUp(memSize);
	if (EFFECTIVE_MEMORY_SIZE) {
  	  TheMan.effectiveMemSize = TheMan.memSize;
        }
	TheMan.gcThreshold=gcThreshold/blockSize;
	TheMan.fixedHeap=fixedHeap;
	nBlocks=memSize/blockSize;
	if (EFFECTIVE_MEMORY_SIZE) {
	  TheMan.effectiveNBlocks = TheMan.nBlocks;
	}
	compactionThreshold = nBlocks; // debugging only, it means always compact everything
	mutatorDisableThreshold = 10;
//	compactionThreshold = TheMan.gcThreshold/2; // there is no thinking behind this... 
//	compactionThreshold = TheMan.gcThreshold + (nBlocks - TheMan.gcThreshold) / 3 ; // there is no thinking behind this... 
	nBitWords=((nBlocks+31)/32);
	
	
	maxAtomicRefCopy = (((Mem.PollingAware)Mem.the()).MAX_ATOMIC_COPY+MachineSizes.BYTES_IN_ADDRESS-1)/MachineSizes.BYTES_IN_ADDRESS;
        maxAtomicRefCopyHalf = maxAtomicRefCopy/2;
        maxAtomicBytesCopy = ((Mem.PollingAware)Mem.the()).MAX_ATOMIC_COPY;
        maxAtomicBytesCopyHalf = maxAtomicBytesCopy/2;
	
	imageBaseAddress = getImageBaseAddress();
	
        if (BROOKS && REPLICATING) {
          throw Executive.panic("Cannot use both BROOKS and REPLICATING at the same time.\n");
        }
        
        if (COMPACTION && !BROOKS && !REPLICATING) {
          throw Executive.panic("Neither Brooks nor replicating barrier was selected.\n");
        }
        
        if (PERIODIC_TRIGGER && disableConcurrency) {
          System.out.println("WARNING !!!! - periodic trigger is enabled in stop-the-world collector");
        }
        
	if (DEBUG_BUILD && debugBuild) {
	    System.out.println("triPizlo: BUILD: creating TheMan.");
	    System.out.println("triPizlo: BUILD: noBarrier = "+noBarrier);
	    if (noBarrier) {
		System.out.println("triPizlo: BUILD: !!! WARNING !!! BARRIER TURNED OFF !!! WARNING !!!");
		System.out.println("triPizlo: WARNING !!! REQUEST TO TURN OFF THE BARRIER IGNORED !!!");
	    }
	    System.out.println("triPizlo: BUILD: imgPageSize = "+imgPageSize);
	    System.out.println("triPizlo: BUILD: blockSize = "+blockSize);
	    System.out.println("triPizlo: BUILD: multNum = "+multNum);
	    System.out.println("triPizlo: BUILD: multDen = "+multDen);
	    System.out.println("triPizlo: BUILD: wordSize = "+wordSize);
	    System.out.println("triPizlo: BUILD: alignment = "+alignment);
	    System.out.println("triPizlo: BUILD: minSize = "+minSize);
	    System.out.println("triPizlo: BUILD: maxBlockAlloc = "+maxBlockAlloc);
	    System.out.println("triPizlo: BUILD: sizeClassIndices = "+sizeClassIndices);
	    System.out.println("triPizlo: BUILD: memSize = "+memSize);
	    System.out.println("triPizlo: BUILD: gcThreshold = "+this.gcThreshold);
	    if (COMPACTION) {
              System.out.println("triPizlo: BUILD: compactionThreshold = "+this.compactionThreshold);
            }
	    System.out.println("triPizlo: BUILD: fixedHeap = "+fixedHeap);
	    System.out.println("triPizlo: BUILD: nBlocks = "+nBlocks);
	    System.out.println("triPizlo: BUILD: nBitWords = "+nBitWords);
	    System.out.println("triPizlo: BUILD: imageBaseAddress = "+getImageBaseAddress());
	}
	
	sizeClassBySize=new SizeClass[sizeClassIndices];
	sizeClasses=new SizeClass[0];
	for (int i=0;i<sizeClassIndices;++i) {
	    int n=i*alignment;
	    int size=allocSize(n);
	    if (size>blockSize) {
		throw new Error();
	    }
	    if (i>0 && sizeClassBySize[i-1].size==size) {
		sizeClassBySize[i]=sizeClassBySize[i-1];
	    } else {
		if (DEBUG_BUILD && debugBuild) {
		    System.out.println("triPizlo: BUILD: size class for size range "+n+".."+size);
		}
		
		SizeClass fl=new SizeClass(size);
		
		sizeClassBySize[i]=fl;

		SizeClass[] newSizeClasses=new SizeClass[sizeClasses.length+1];
		System.arraycopy(sizeClasses,0,
				 newSizeClasses,0,
				 sizeClasses.length);
		newSizeClasses[sizeClasses.length]=fl;
	 	sizeClasses=newSizeClasses;
	    }
	}
	
	largeBits=new int[nBitWords+1];
	arrayletBits=new int[nBitWords+1];
	usedBits=new int[nBitWords+1];
	sizes=new int[nBlocks];
	uses=new int[nBlocks];
	blockNext=new int[nBlocks];
	blockPrev=new int[nBlocks];
	nonfullNext=new int[nBlocks];
	nonfullPrev=new int[nBlocks];	
	nonfullHeads = new int[maxOccupancy + 1];
	nonfullTails = new int[maxOccupancy + 1];
	freeHeadsPtrs=new int[nBlocks];
	nPinnedObjects=new int[nBlocks];
	blockHead=0;

	if (VERIFY_MARK || VERIFY_HEAP_INTEGRITY || VERIFY_COMPACTION || VERIFY_SWEEP || VERIFY_BUG) {
	  nObjBitWords = ((nBlocks*maxOccupancy+31)/32);
	  checkedBits = new int[nObjBitWords + 1];
	}

	for (int i=0;i<nBlocks;++i) {
	    blockPrev[i]=i-1;
	    if (i==nBlocks-1) {
		blockNext[i]=-1;
	    } else {
		blockNext[i]=i+1;
	    }
	    
	    freeHeadsPtrs[i] = 0;
	    nonfullNext[i] = -1;
	    nonfullPrev[i] = -1;
	    nPinnedObjects[i] = 0;
	}

	for (worklistSize=1;worklistSize<(memSize/minSize);worklistSize*=2) ;
	//worklistSize*=4;
	worklistBits=worklistSize-1;
	if (DEBUG_BUILD && debugBuild) {
	    Native.print_string("triPizlo: BUILD: worklistSize = ");
	    Native.print_int(worklistSize);
	    Native.print_string("\n");
	    
	    Native.print_string("triPizlo: BUILD: worklistBits = ");
	    Native.print_int(worklistBits);
	    Native.print_string("\n");
	}
	
	allocColor=WHITE;

/*	try {
	    params = new PrintWriter(new FileOutputStream
				     ("mc_barrier_params.h"),
				     true);
	    params.println("#define OVM_PAGE_SIZE " + imgPageSize);
	    params.println("#define HEAP_START 0x" +
			   Integer.toHexString(getImageBaseAddress()));
	    params.println("#define IMAGE_CONTINUED_RANGES \\");

	    Driver.gen_ovm_c.println("#include <mc_barrier_params.h>");
	    Driver.gen_ovm_c.println("#include <mc_barrier.h>");
	    Driver.gen_ovm_c.flush();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	} */
	// FIXME: WHY does timedWait run fast?
	// Is there something wrong with our regression tests, or
	// something wrong with the RTGC?
	RuntimeExports.defineVMProperty("org.ovmj.timedWaitRunsFast", true);
    }

    static int[] allocIntArrayRaw(int size) {
      return (int[])allocArrayRaw(size, intArray).asAnyObject();
    }
    
    static long[] allocLongArrayRaw(int size) {
      return (long[])allocArrayRaw(size, longArray).asAnyObject();
    }
    
    static VM_Address allocArrayRaw(int size, Blueprint.Array arrayBlueprint) {
      int totalSize=-1;
	
      if (ARRAYLETS) {
        totalSize =  sizeOfContinuousArraySpine( arrayBlueprint, size );
      } else {
        totalSize = (int)arrayBlueprint.computeSizeFor(size);
      }
	
      VM_Address result=Native.getmem(totalSize);
      Nat.bzero(result,totalSize);
      arrayBlueprint.stamp(result,size);
	
      if (ARRAYLETS) {
        VM_Address ptr = result.add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD );

        int nptrs = allArrayletsInArray(arrayBlueprint, size);
        VM_Address data = result.add( bytesToDataInSpine(arrayBlueprint, size, true) );
      
        // initialize arraylets in the spine
        for(int i=0;i < nptrs; i++) {
          ptr.setAddress( data.add(i*arrayletSize) );
          ptr = ptr.add( MachineSizes.BYTES_IN_ADDRESS );
        }
      }
      
      return result;
    }
    
    static boolean inHeap(VM_Address addr) {
	return addr.diff(memory).uLT(VM_Word.fromInt(memSize));
    }
    
    static boolean inHeap(Oop oop) {
	return inHeap(VM_Address.fromObjectNB(oop));
    }

    // does not work before init finishes, because 
    // nativeImageBaseAddress and imageSize get initialized 
    // there [ nothing is said to be in image at that time ]
    
    static boolean inImage(VM_Address addr) throws PragmaInline, PragmaAssertNoExceptions, PragmaAssertNoSafePoints  {
	return addr.diff(nativeImageBaseAddress).uLT(imageSize);
    }
    
    static boolean inImage(Oop oop) {
	return inImage(VM_Address.fromObjectNB(oop));
    }
    
    static void verifyPointer(VM_Address addr) throws PragmaNoReadBarriers {
      verifyPointer("(no verify pointer-with-implied-inheap context)",addr, true);
    }

    static void verifyPointer(String contextMsg, VM_Address addr) throws PragmaNoReadBarriers {
      verifyPointer(contextMsg,addr, true);
    }
    
    static int vpincall = 1; // enabled at boot
    static void verifyPointer(VM_Address addr, boolean inheap) throws PragmaNoReadBarriers, PragmaAtomicNoYield {    
      verifyPointer("(no verifyPointer-with-inheap context)", addr, inheap);
    }

    static void verifyPointer(String contextMsg, VM_Address addr, boolean inheap) throws PragmaNoReadBarriers, PragmaAtomicNoYield {

        if (!inHeapWorksNow) {
          return ;
        }
        
        if (vpincall != 0 ) {
          return ;
        }
        vpincall ++;
        
	if (inHeap(addr)) {
	    int blockIdx=blockIdx(addr);
	    VM_Address blockBase=memory.add(blockIdx*blockSize);
	    int size=sizes[blockIdx];
	    int offset=addr.diff(blockBase).asInt();
	    
	    if (ARRAYLETS && Bits.getBit(arrayletBits, blockIdx)) {
              Native.print_string("VERIFY_HEAP_INTEGRITY: verifyPointer: ");
              Native.print_string(contextMsg);
              Native.print_string("\n");	    
	      Native.print_string("VERIFY_HEAP_INTEGRITY: ERROR: saw pointer to an arraylet");
	      throw Executive.panic("fix");
	    } else if (Bits.getBit(largeBits,blockIdx)) {
		if (size<=0) {
                    Native.print_string("VERIFY_HEAP_INTEGRITY: verifyPointer: ");
                    Native.print_string(contextMsg);
                    Native.print_string("\n");		
		    Native.print_string("VERIFY_HEAP_INTEGRITY: ERROR: saw offset pointer ");
		    Native.print_ptr(addr);
		    Native.print_string("!  this is probably a large object pointer, but it points at a continued block.\n");
		}
		if (offset!=0) {
                    Native.print_string("VERIFY_HEAP_INTEGRITY: verifyPointer: ");
                    Native.print_string(contextMsg);
                    Native.print_string("\n");		
		    Native.print_string("VERIFY_HEAP_INTEGRITY: ERROR: saw offset pointer ");
		    Native.print_ptr(addr);
		    Native.print_string("!  this is probably a large object pointer, but it does not point at the beginning of a block.\n");
		}
	    } else if ((offset%size)!=0) {
                Native.print_string("VERIFY_HEAP_INTEGRITY: verifyPointer: ");
                Native.print_string(contextMsg);
                Native.print_string("\n");
		Native.print_string("VERIFY_HEAP_INTEGRITY: ERROR: saw offset pointer ");
		Native.print_ptr(addr);
		Native.print_string("!  should be at ");
		Native.print_ptr(blockBase.add((offset/size)*size));
		Native.print_string(", with the block base at ");
		Native.print_ptr(blockBase);
		Native.print_string(" and an allocation size of ");
		Native.print_int(size);
		Native.print_string("\n");
		
		Native.print_string("Attempting to find from which objects was this referenced:\n");
		findReferees(addr);
                Native.print_string("\n");
		
		throw Executive.panic("Fix...");
	    }
	    if (gcData.getColor(addr)==FREE) {
                Native.print_string("VERIFY_HEAP_INTEGRITY: verifyPointer: ");
                Native.print_string(contextMsg);
                Native.print_string("\n");
		Native.print_string("VERIFY_HEAP_INTEGRITY: ERROR: saw pointer to a free object at ");
		Native.print_ptr(addr);
		Native.print_string(" the pointer is referenced from ");
		findReferees(addr);
		Native.print_string("\n");
	    }
	    if (!Bits.getBit(usedBits,blockIdx)) {
                Native.print_string("VERIFY_HEAP_INTEGRITY: verifyPointer: ");
                Native.print_string(contextMsg);
                Native.print_string("\n");
		Native.print_string("VERIFY_HEAP_INTEGRITY: ERROR: saw pointer into unused block at ");
		Native.print_ptr(addr);
		Native.print_string("\n");
	    }
	    
//	    if (COMPACTION) {
//	      verifyForwardingPointer(addr, FWD_ALWAYS);
//           }
	    
	} else {
	    if (inheap) {
              Native.print_string("VERIFY_HEAP_INTEGRITY: verifyPointer: ");
              Native.print_string(contextMsg);
              Native.print_string("\n");
  	      Native.print_string("VERIFY_HEAP_INTEGRITY: ERROR: pointer ");
  	      Native.print_ptr(addr);
  	      Native.print_string(" is not in the heap!\n");
            }
	}
	vpincall --;
    }
    
    static void verifyPointer(Oop oop) {
	verifyPointer("(verifyPointer-oop)", oop);
    }

    static void verifyPointer(String contextMsg, Oop oop) {
	verifyPointer(contextMsg, VM_Address.fromObjectNB(oop));
    }
    
    public final void boot(boolean useImageBarrier) throws PragmaNoPollcheck {
	this.useImageBarrier = useImageBarrier;
	
	if (DEBUG_INIT && debugInit) {
	    Native.print_string("triPizlo: INIT: booting.\n");
	    if (noBarrier) {
		Native.print_string("triPizlo: INIT: !!! WARNING !!! BARRIER IS ON !!! WARNING !!!\n");
		Native.print_string("triPizlo: WARNING !!! REQUEST TO TURN OFF THE BARRIER IGNORED !!!\n");		
	    }
	}
	if (fixedHeap!=-1) {
	    memory=VM_Address.fromInt(fixedHeap);
	} else {
	    memory=Native.getheap(memSize);
	    if (DEBUG_INIT && debugInit) {
		Native.print_string("triPizlo: INIT: heap allocated at ");
		Native.print_ptr(memory);
		Native.print_string(".\n");
	    }
	    memory=memory.asWord().add(blockSize-1).div(blockSize).mul(blockSize).asAddress();
	}
	if (DEBUG_INIT && debugInit) {
	    Native.print_string("triPizlo: INIT: usable heap base at ");
	    Native.print_ptr(memory);
	    Native.print_string(".\n");
	}
	Nat.bzero(memory,memSize);
	    
	worklist=Native.getmem(worklistSize*wordSize);
	
	nImgBits=
	    (Native.getImageEndAddress().asInt()-getImageBaseAddress())
	    /imgPageSize;
	nImgBitWords=(nImgBits+31)/32;
	
	if (DEBUG_INIT && debugInit) {
	    Native.print_string("triPizlo: INIT: imageBaseAddress = ");
	    Native.print_ptr(Native.getImageBaseAddress());
	    Native.print_string("\n");
	    
	    Native.print_string("triPizlo: INIT: imageEndAddress = ");
	    Native.print_ptr(Native.getImageEndAddress());
	    Native.print_string("\n");
	    
	    Native.print_string("triPizlo: INIT: nImgBits = ");
	    Native.print_int(nImgBits);
	    Native.print_string("\n");
	    
	    Native.print_string("triPizlo: INIT: nImgBitWords = ");
	    Native.print_int(nImgBitWords);
	    Native.print_string("\n");
	}
	
	// note the strange order of commands
	// this is because the write to "dirty" triggers the image barrier
	//dirty=allocIntArrayRaw(nImgBitWords+1);
	
	int[] dummy = allocIntArrayRaw(nImgBitWords+1);

	if (ARRAYLETS) {
	  dirtyValuesStart = VM_Address.fromObjectNB(dummy).add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD ).getAddress();
        } else {
          int fsize = VM_Address.fromObjectNB(dummy).asOopUnchecked().getBlueprint().getFixedSize();
  	  dirtyValuesStart = VM_Address.fromObjectNB(dummy).add(fsize); 
        }
	dirty = dummy;
	
	nativeImageBaseAddress = Native.getImageBaseAddress();
	imageSize = Native.getImageEndAddress().diff(nativeImageBaseAddress);
	lastImagePageIdx = imageAddressToPageIndex(Native.getImageEndAddress());
	
	// repeated for the side effect of the image barrier, so that it marks
	// the image page with "this" as dirty 
	// (maybe not necessary here, but it's good to stress that this has to 
	// be marked)
        lastImagePageIdx = imageAddressToPageIndex(Native.getImageEndAddress());
	

        if (NEEDS_IMAGE_BARRIER || FORCE_IMAGE_BARRIER) {
  	  // now mark the roots arrays in the image as dirty
  	  // (strings are being written there without the image barrier)
  	  
  	  for (Iterator it = DomainDirectory.domains(); it.hasNext(); ) {
            S3Domain d = (S3Domain) it.next();
            imageStoreBarrier(VM_Address.fromObjectNB(d.j2cRoots),Assert.IMAGEONLY|Assert.NONNULL);
          }
        }
	
	if (DEBUG_INIT && debugInit) {
	    Native.print_string("triPizlo: INIT: lastImagePageIdx = ");
	    Native.print_int(lastImagePageIdx);
	    Native.print_string("\n");	    
	    
	    Native.print_string("triPizlo: INIT: image meta-data initialized.\n");
	}

        if (DEBUG_INTERRUPTIBLE_STACK_SCANNING) {
	    Native.print_string("triPizlo: INIT: INTERRUPTIBLE_STACK_SCANNING: stackUninterruptible =  ");
	    Native.print_string( stackUninterruptible ? "true" : "false");
	    Native.print_string("\n");        
	    
	    Native.print_string("triPizlo: INIT: INTERRUPTIBLE_STACK_SCANNING: INCREMENTAL_STACK_SCAN =  ");
	    Native.print_string( INCREMENTAL_STACK_SCAN ? "true" : "false");
	    Native.print_string("\n");        
        }

	if (DEBUG_INIT && debugInit) {
	    Native.print_string("triPizlo: INIT: disableConcurency = ");
	    Native.print_boolean(disableConcurrency);
	    Native.print_string("\n");
	    Native.print_string("triPizlo: INIT: compaction = ");
	    Native.print_boolean(COMPACTION);
	    Native.print_string("\n");
	    Native.print_string("triPizlo: INIT: forwarding style = ");
	    if (BROOKS) {
	      Native.print_string("Brooks\n");
	    }
	    if (REPLICATING) {
	      Native.print_string("replication\n");
	    }
	    Native.print_string("\n");	    
            Native.print_string("triPizlo: INIT: Yuasa barrier on = ");	 
            Native.print_boolean( NEEDS_YUASA_BARRIER || FORCE_YUASA_BARRIER );
            Native.print_string("\n");
            Native.print_string("triPizlo: INIT: Dijkstra barrier on = ");
            Native.print_boolean( NEEDS_DIJKSTRA_BARRIER | FORCE_DIJKSTRA_BARRIER );
            Native.print_string("\n");            
            Native.print_string("triPizlo: INIT: Image barrier on = ");
            Native.print_boolean( NEEDS_IMAGE_BARRIER | FORCE_IMAGE_BARRIER );
            Native.print_string("\n");
            Native.print_string("triPizlo: INIT: Translating barrier on = ");
            Native.print_boolean( COMPACTION || FORCE_TRANSLATING_BARRIER );
            Native.print_string("\n");
            Native.print_string("triPizlo: INIT: Periodic trigger = ");
            Native.print_boolean( PERIODIC_TRIGGER );
            Native.print_string("\n");
            Native.print_string("triPizlo: INIT: Synchronous trigger = ");
            Native.print_boolean( SYNCHRONOUS_TRIGGER );
            Native.print_string("\n");         

            
            if (PERIODIC_TRIGGER && SYNCHRONOUS_TRIGGER) {
              Native.print_string("WARNING !!! Having both the periodic and synchronous trigger active doesn't make much sense. Please check that it's what was intended.\n");
            }
               
            Native.print_string("triPizlo: INIT: Periodic scheduler support = ");
            Native.print_boolean( SUPPORT_PERIODIC_SCHEDULER );
            Native.print_string("\n");
            Native.print_string("triPizlo: INIT: Aperiodic scheduler support = ");
            Native.print_boolean( SUPPORT_APERIODIC_SCHEDULER );
            Native.print_string("\n");            
            Native.print_string("triPizlo: INIT: Hybrid scheduler support = ");
            Native.print_boolean( SUPPORT_HYBRID_SCHEDULER );
            Native.print_string("\n");                        
            
            if (REPLICATING && COMPACTION && INCREMENTAL_OBJECT_COPY) {
              Native.print_string("triPizlo: INIT: Incremental object copy active.\n");
            } else {
              Native.print_string("triPizlo: INIT: Incremental object copy NOT active.\n");
            }
            
            Native.print_string("triPizlo: INIT: Arraylets on = ");
            Native.print_boolean( ARRAYLETS );
            Native.print_string("\n");
        }
        
        if (VERIFY_ARRAYLETS) {
  	  for (int i=0;i<nBlocks;++i) {
	    if (blockPrev[i]!=i-1) {
	      Native.print_string("triPizlo: ERROR: blockPrev (maybe) corrupted, at index ");
	      Native.print_int(i);
	      Native.print_string(" should be ");
	      Native.print_int(i-1);
	      Native.print_string(" is ");
	      Native.print_int(blockPrev[i]);
	      Native.print_string("\n");
	    }
	    if (i==nBlocks-1) {
		if (blockNext[i] != -1) {
		  Native.print_string("triPizlo: ERROR: blockNext (most likely) corrupted, at index ");
                  Native.print_int(i);
                  Native.print_string(" should be -1 is ");
                  Native.print_int(blockNext[i]);
                  Native.print_string("\n");		
		}
	    } else {
		if (blockNext[i]!=i+1) {
		  Native.print_string("triPizlo: ERROR: blockNext (maybe) corrupted, at index ");
                  Native.print_int(i);
                  Native.print_string(" should be ");
                  Native.print_int(i+1);
                  Native.print_string(" is ");
                  Native.print_int(blockNext[i]);
                  Native.print_string("\n");		
		}
	    }
	  }  
        
        }
        
        if (VERIFY_IMAGE_INTEGRITY) {
          verifyImageIntegrity();
        }
        
        if (DEBUG_GC && debugGC || VERIFY_COMPACTION) {
          // force compilation of the debug routines that are very useful in GDB sessions
          
              Native.print_string("triPizlo: referees of the manager itself are:\n");
              findReferees(VM_Address.fromObjectNB(this));
              
              if (ARRAYLETS) {
                Native.print_string("triPizlo: array of the first arraylet of the dirty array is:\n");
                findArrayOfArraylet(dirtyValuesStart);
                Native.print_string("(the list above should be empty since the array is not in heap)\n");
              }
        }
     
       if (VERIFY_HEAP_INTEGRITY || VERIFY_COMPACTION) {
         vpincall = 0;
       }   

       inHeapWorksNow = true ;
    }

    static boolean inHeapWorksNow = false;
    
    public final void initWithCommandLineArguments() throws PragmaNoPollcheck {

        Native.print_string("triPizlo: initializing with access to VM command line arguments.\n");
        // init periodic scheduling table
        
        threadManager = (UserLevelThreadManager) ((ThreadServicesFactory)ThreadServiceConfigurator.config.
          getServiceFactory(ThreadServicesFactory.name)).getThreadManager();

        timerManager = ((TimerServicesFactory)ThreadServiceConfigurator.config.
          getServiceFactory(TimerServicesFactory.name)).getTimerManager();

        if ( (SUPPORT_PERIODIC_SCHEDULER && periodicScheduler) || (SUPPORT_HYBRID_SCHEDULER && hybridScheduler) ) {
        
          S3JavaUserDomain userDomain = (S3JavaUserDomain) DomainDirectory.getUserDomain(1);
          RuntimeExports re = userDomain.getRuntimeExports();
          
          int mutatorPeriods = re.getGCTimerMutatorCounts();
          int collectorPeriods = re.getGCTimerCollectorCounts();
          int windowPeriods = mutatorPeriods+collectorPeriods;
          
          if (collectorPeriods > mutatorPeriods) {
            throw Executive.panic("collectorPeriods > mutatorPeriods is presently not supported. It is however trivial to add, feel free...");
          }

          long timerPeriodNanos = timerManager.getTimerInterruptPeriod();
          periodNanos = re.getGCTimerPeriod();
          
          if (periodNanos == -1) { 
            periodNanos = timerPeriodNanos;
          }

          Native.print_string("triPizlo: hybrid/periodic scheduling, VM timer period is ");
          Native.print_long(timerPeriodNanos);
          Native.print_string("ns , gc timer period is ");
          Native.print_long(periodNanos);
          Native.print_string("ns\n");
          
          if (timerPeriodNanos > periodNanos) {
            throw Executive.panic("VM timer period cannot be longer than gc timer period");
          }
          windowNanos = windowPeriods * periodNanos;
				    
          enableCollector = new boolean[windowPeriods];
                                
          // M C M C M C ... M C M M M M M M  | again
          // if mutator_counts==14, collector_counts==6
          //  M C M C M C M C M C M C M M M M M M M M

          // the initial M C ... M C block
          for(int c=0; c<collectorPeriods; c++) {
            enableCollector[2*c] = false;
            enableCollector[2*c+1] = true;
          }
                                
          // the final M .... M block (sleep set to bypass the first M of the next window)
          for(int m=0; m<(mutatorPeriods-collectorPeriods); m++) {
            enableCollector[ collectorPeriods * 2 + m ] = false;
          }

          Native.print_string("triPizlo: hybrid/periodic scheduling, each window has ");
          Native.print_int(windowPeriods);
          Native.print_string(" periods, ");
          Native.print_int(mutatorPeriods);
          Native.print_string(" are reserved for mutator\n");

          Native.print_string("triPizlo: hybrid/periodic scheduling, mask is ");
          for(int i=0;i<windowPeriods;i++) {
            if (enableCollector[i]) {
              Native.print_string("C");
            } else {
              Native.print_string("M");
            }
          }
          Native.print_string("\n");
        }
        
        if (SUPPORT_HYBRID_SCHEDULER && hybridScheduler) {        
          
          S3JavaUserDomain userDomain = (S3JavaUserDomain) DomainDirectory.getUserDomain(1);
          RuntimeExports re = userDomain.getRuntimeExports();
          
          dispatcher = ((JavaServicesFactory) ThreadServiceConfigurator.config.
            getServiceFactory(JavaServicesFactory.name)).getJavaDispatcher();

          gcThreadPriority = re.getGCThreadPriority();
          gcThreadLowPriority = re.getGCThreadLowPriority();
          
          Native.print_string("triPizlo: hybrid scheduling, high priority is ");
          Native.print_int(gcThreadPriority);
          Native.print_string(", low priority is");
          Native.print_int(gcThreadLowPriority);
          Native.print_string("\n");
        }
        
        if (SUPPORT_APERIODIC_SCHEDULER && aperiodicScheduler) {
        
          S3JavaUserDomain userDomain = (S3JavaUserDomain) DomainDirectory.getUserDomain(1);
          RuntimeExports re = userDomain.getRuntimeExports();
          
          Native.print_string("triPizlo: aperiodic scheduling, thread priority is ");
          Native.print_int(re.getGCThreadPriority());
          Native.print_string("\n");
        }
        
        Native.print_string("triPizlo: Abort on gc reentry (infinite gc loop detection&abort) = ");
        Native.print_boolean( abortOnGcReentry );
        Native.print_string("\n");                 
        
        if (SUPPORT_MUTATOR_DISABLE) {
          Native.print_string("triPizlo: mutator disable threshold = ");
          Native.print_int( mutatorDisableThreshold );
          Native.print_string("\n");                 
        }
        
        if (REPORT_LONG_LATENCY) {
          Native.print_string("triPizlo: reporting pollcheck latencies over ");
          Native.print_long(longLatency);
          Native.print_string("ns\n");
        }

        if (REPORT_LONG_PAUSE) {
          Native.print_string("triPizlo: reporting pauses over ");
          Native.print_long(longPause);
          Native.print_string("ns\n");
        }        
    }


    static boolean[] enableCollector = null;
    static long windowNanos;
    static long periodNanos;
    static int gcThreadPriority;
    static int gcThreadLowPriority;

    static boolean inCollector = true;
    
    // note that current thread can be == next
    // also, next can be null 
    
    static boolean timeProfileHook = false;
    
    static long gcPauseStartTime = 0;
    
    public void runNextThreadHook( OVMThread current, OVMThread next ) {
    
      if (VERIFY_POLLING && !gcReschedulingExpected) {
        
        if (current == gcThread) {
          Native.print_string("triPizlo: ERROR: GC thread was suspended when this was assumed impossible.\n");
          Native.print_string("pollcheck at source line ");
          int line = Native.getStoredLineNumber();
          Native.print_int(line);
          Native.print_string("\n");
          
          throw Executive.panic("fix");
        }
        
        if (next == gcThread) {
          Native.print_string("triPizlo: ERROR: GC thread was woken up when this was assumed impossible.\n");
          Native.print_string("pollcheck at source line ");
          int line = Native.getStoredLineNumber();
          Native.print_int(line);
          Native.print_string("\n");          
          throw Executive.panic("fix");          
        }

        // we will not catch gc thread going to idle processing this way
      }
    
      if ( (PROFILE_GC_PAUSE || REPORT_LONG_PAUSE) && timeProfileHook ) {
      
        if (inCollector) {
        
          if (next != gcThread) {
            inCollector = false;
            long now = getCurrentTime();
            
            pollLat.tic(now);
            
            if (REPORT_LONG_PAUSE && gcPauseStartTime>0) {
              long pause = now-gcPauseStartTime;
              
              if (pause > longPause) {
                Native.print_string("Long gc pause of ");
                Native.print_long(pause);
                Native.print_string(" ns detected, starting roughly at pollcheck at line ");
                Native.print_int(afterMutatorLine);
                Native.print_string("\n");
              }
              
              if (pause > maxObservedPause) {
                maxObservedPause = pause;
                maxObservedPauseLine = afterMutatorLine;
              }
            }
          }
        
        } else { // !inCollector
        
          if (next == gcThread) {
            inCollector = true;
            long now = getCurrentTime();
            
            pollLat.toc(now);
            outer.lose(pollLat.lat);
            inner.lose(pollLat.lat);            
            
            if (REPORT_LONG_PAUSE && longPause>0) {
              gcPauseStartTime = now;
            }
          }
        }
      }
    }
    
    static long arrivalTimes[] = new long[maxArrivalTimes];
    static int recordedArrivalTimes = 0;
    
    static int enteredExcludedBlocks = 0;
    static long excludingFrom = -1;
    static long excludedFromFinishedBlocks = 0;
    
    private static long getExcludedTime() throws PragmaAssertNoExceptions, PragmaInline, 
      PragmaAssertNoSafePoints, PragmaIgnoreSafePoints, PragmaIgnoreExceptions {
    
      long res;
      
      if (enteredExcludedBlocks > 0) {
        res = excludingFrom - excludedFromFinishedBlocks;
      } else {
        res = Native.getCurrentTime() - excludedFromFinishedBlocks;
      }

      if (false) {
        Native.print_string("EX getExcludedTime: ");
        Native.print_long(res);
        Native.print_string("\n");
      }      
      return res;
    }
    
    private static void enterExcludedBlock() throws PragmaAssertNoExceptions {
    
      if (enteredExcludedBlocks == 0) {
        excludingFrom = Native.getCurrentTime();
        
        if (false) {
          Native.print_string("current time is: ");
          Native.print_long(getCurrentTime());
          Native.print_string("\n");        
        }
      }
      
      enteredExcludedBlocks++;
      
      if (false) {
        Native.print_string("EX enterExcludedBlock\n");
        Native.print_string("current time is: ");
        Native.print_long(getCurrentTime());
        Native.print_string("\n");
      }      
    }
    
    private static void leaveExcludedBlock() throws PragmaAssertNoExceptions {
    
      if (enteredExcludedBlocks == 1) {
        long now = Native.getCurrentTime();
        excludedFromFinishedBlocks += now-excludingFrom;
        excludingFrom = -1;
      }
      
      enteredExcludedBlocks --;
      if (false) {
        Native.print_string("EX leaveExcludedBlock\n");
        Native.print_string("current time is: ");
        Native.print_long(getCurrentTime());
        Native.print_string("\n");        
      }            
    }
    
    private static long getCurrentTime() throws PragmaAssertNoExceptions, PragmaInline, 
      PragmaAssertNoSafePoints, PragmaIgnoreSafePoints, PragmaIgnoreExceptions  {
    
      long now;
      
      if (EXCLUDED_BLOCKS) {
        now = getExcludedTime();
      } else {
        now = Native.getCurrentTime();
      }
      
      return now;
    }
    
    public void fire(int ticks) throws PragmaIgnoreExceptions, PragmaAssertNoExceptions, PragmaNoPollcheck {
      
      if (VERIFY_POLLING) {
        if (threadManager.isReschedulingEnabled()) {
          throw Executive.panic("Rescheduling is enabled in RTGC's fire() !");
        }
      }
      
      // can we trust the "ticks" value ?
      // if we could, then we could use just the number of ticks for scheduling, instead of relatively
      // expensive time ?
      
      //long now = 0; // read carefully where this is initialized
                    // intended to run fast, not to be readable
      
      /*
      if (PROFILE_GC_PAUSE) {
        this does not work...it reports much longer pauses than the GC really causes
        
        now = getCurrentTime(); // nanos
        
        if (false && timeProfile) {
          if ( (threadManager.getCurrentThread() == gcThread) && threadManager.isReady(gcThread) ) {
            // now in collector
          
            if (!inCollector) {
              pollLat.toc(now);
              outer.lose(pollLat.lat);
	      inner.lose(pollLat.lat);
	    
	      inCollector = true;
            }
          } else {
        
            if (inCollector) {
	      pollLat.tic(now);
	      inCollector = false;
            }
          }
        }
      }
      
      */
      // this may release the GC thread, even if it should not run now (periodic scheduling)
      // this is why the trigger is called _before_ the logic that would block the thread again
      
      if (PERIODIC_TRIGGER) {
        triggerGCIfNeeded(); // periodic trigger for aperiodic collector
      }
      
      if (SUPPORT_PERIODIC_SCHEDULER && periodicScheduler) {
      
        long now = getCurrentTime();

        if (PROFILE_TIMER_INTERRUPTS) {
        
          if (recordedArrivalTimes < maxArrivalTimes) {
            arrivalTimes[ recordedArrivalTimes ] = now;
            recordedArrivalTimes++;
          }
        }
      
        long floor=(now/windowNanos)*windowNanos;
        int periodIndex = (int) ((now - floor)/periodNanos);
        
        if (SUPPORT_MUTATOR_DISABLE && shouldDisableMutator) {
          shouldPause = false;
        } else {
          shouldPause = !enableCollector[periodIndex];
        }
        
        if (!shouldPause) {
          if (gcThreadBlockedByScheduler) {
            gcThreadBlockedByScheduler = false;
            threadManager.makeReady(gcThread);        
          } 
        } else {
          if (!gcThreadBlockedByScheduler && !gcThreadBlockedByTrigger) {
            gcThreadBlockedByScheduler = true;
            threadManager.removeReady(gcThread);
          }
        }
      }
      
      if (SUPPORT_HYBRID_SCHEDULER && hybridScheduler) {
      
        long now = getCurrentTime();
      
        if (PROFILE_TIMER_INTERRUPTS) {
        
          if (recordedArrivalTimes < maxArrivalTimes) {
            arrivalTimes[ recordedArrivalTimes ] = now;
            recordedArrivalTimes++;
          }
        }

        long floor=(now/windowNanos)*windowNanos;
        int periodIndex = (int) ((now - floor)/periodNanos);

        if (SUPPORT_MUTATOR_DISABLE && shouldDisableMutator) {
          shouldPause = false;
        } else {
          shouldPause = !enableCollector[periodIndex];
        }
        
        if (!shouldPause) {
          if (gcThreadBlockedByScheduler) {
            gcThreadBlockedByScheduler = false;
            dispatcher.setPriority( gcThread, gcThreadPriority );
          } 
        } else {
          if (!gcThreadBlockedByScheduler) {
            gcThreadBlockedByScheduler = true;
            dispatcher.setPriority( gcThread, gcThreadLowPriority );
          }
        }
      }
    }
    
    volatile static JavaDispatcher dispatcher = null;
    volatile static UserLevelThreadManager threadManager = null;
    volatile static TimerManager timerManager = null;
    volatile static PriorityOVMThread gcThread = null;
    volatile static boolean gcThreadBlockedByScheduler = false;
    volatile static boolean gcThreadBlockedByTrigger = false;

    public String timerInterruptActionShortName() {
      return "RTGC timer";
    }
    
    static String colorName(int color) {
	if (color==WHITE) {
	    return "W";
	} else if (color==GREYBLACK) {
	    return "G";
	} else if (color==FREE) {
	    return "F";
	} else if (color==FRESH) {
	    return "P";
	} else {
	    return "?";
	}
    }
    
    static void dumpHeap() throws PragmaNoReadBarriers  {
	if (DEBUG_DUMP_HEAP && debugDumpHeap) {
	    for (int bi=0;bi<nBlocks;) {
		int increment=1;
		if (Bits.getBit(usedBits,bi)) {
		    Native.print_string("triPizlo: DUMP_HEAP: #");
		    Native.print_int(bi);
		    Native.print_string(" base=");
		    Native.print_ptr(memory.add(bi*blockSize));
		    Native.print_string(" size=");
		    Native.print_int(sizes[bi]);
		    Native.print_string(" next=");
		    Native.print_int(blockNext[bi]);
		    Native.print_string(" ");
		    if (ARRAYLETS && Bits.getBit(arrayletBits, bi)) {
		        Native.print_string("A");
		    } else {
		      if (Bits.getBit(largeBits,bi)) {
			Native.print_string("L");
                      } else {
			Native.print_string("S");
                      }
		    
                      if (Bits.getBit(largeBits,bi)) {
			Native.print_string(" color=");
			Native.print_string(colorName(gcData.getColor(memory.add(bi*blockSize))));
			increment=sizes[bi];
                      } else {
			Native.print_string(" colors=");
			for (int oi=0;oi<blockSize/sizes[bi];++oi) {
			    Native.print_string(colorName(gcData.getColor(memory.add(bi*blockSize).add(oi*sizes[bi]))));
                        }
                      }
                    }
		    Native.print_string("\n");
		}
		bi+=increment;
	    }
	    for (int sci=0;sci<sizeClasses.length;++sci) {
		// FIXME: this is an incomplete dump
		SizeClass sc=sizeClasses[sci];
		Native.print_string("triPizlo: DUMP_HEAP: size class for ");
		Native.print_int(sc.size);
		Native.print_string(": objects:");

                int bIndex = sc.nonfullHead;

                for( int b = 0; bIndex != -1 ; b++ ) {

                   for (VM_Address cur = VM_Address.fromInt(freeHeadsPtrs[bIndex]); 
                      cur != null;
                      cur = freeList.getNext(cur)) {
                      
                      Native.print_string(" ");
                      Native.print_ptr(cur);
                   }
                  
                   bIndex = nonfullNext[bIndex];
                }
                  
		Native.print_string("\n");
	    }
	}
    }
    

    static int countBlockFreeObjects(int bIdx, boolean scan) throws PragmaNoReadBarriers, PragmaNoPollcheck  {

      int nfree = 0;
      
      if (scan) {
        VM_Address addr = memory.add(bIdx*blockSize);
        
        int size = sizes[bIdx];
        int nObjs = blockSize/size;
      
        for(int i=0;i<nObjs; i++) {
          int color = gcData.getColor(addr);
          if (color == FREE) {
            nfree++;
          }
          addr = addr.add(size);
        }
      } else {
        VM_Address addr = VM_Address.fromInt(freeHeadsPtrs[bIdx]);
        
        while(addr != null) {
          nfree ++;
          addr = freeList.getNext(addr);
        }
      }
    
      return nfree;
    }
    
    static int countSCFreeObjects(SizeClass sc, boolean scan) throws PragmaNoPollcheck {
      
      int head = sc.nonfullHead;
      int nfree = 0;
      
      while (head!=-1) {
        nfree += countBlockFreeObjects(head, scan);
        head = nonfullNext[head];
      }
      
      return nfree;
    }
    
    static void memoryInfo() throws PragmaNoPollcheck {
    
          Native.print_string("Memory info:--------------------\n");

    	  Native.print_string("free blocks: ");
	  Native.print_int(nBlocks-nUsedBlocks);
	  Native.print_string("\n");
	  
	  if (EFFECTIVE_MEMORY_SIZE) {
      	    Native.print_string("effective free blocks: ");
      	    Native.print_int(effectiveNBlocks-nUsedBlocks);	  
 	    Native.print_string("\n");      	    
	  }
	  
	  Native.print_string("\nsize classes:");
	  for(int i=0; i<sizeClasses.length ; i++) {
	    SizeClass sc = sizeClasses[i];
	    Native.print_string("\n\tsize=");
	    Native.print_int(sc.size);
	    Native.print_string(" free_objects=");
	    Native.print_int( (sc.nBlocks*blockSize - sc.size*sc.nObjects)/sc.size );
	    Native.print_string(" counted_free_objects=");
	    Native.print_int(countSCFreeObjects(sc, false));
	    Native.print_string(" scanned_free_objects=");
	    Native.print_int(countSCFreeObjects(sc, true));
	    Native.print_string(" objects=");
	    Native.print_int(sc.nObjects);
	    Native.print_string(" blocks=");
	    Native.print_int(sc.nBlocks);
	    Native.print_string(" freeable_blocks=");
	    Native.print_int(freeableBlocks(sc));
            Native.print_string(" blocks_with_pinned_objects=");
            Native.print_int(sc.nBlocksPinned);
	  }
          Native.print_string("\n");	  
    }
    
    static long memoryExhaustedTime = -1;
    
    static boolean alreadyExhausted=false;
    static Error memoryExhausted(int size) {
	// the logic for alreadyExhausted is funky.  the key to understanding why
	// the code works the way it does is to notice the call to getMemImpl,
	// which does not return, but instead reenters memoryExhausted().  this
	// is what the alreadyExhausted flag is for.
	if (!alreadyExhausted) {
	    alreadyExhausted=true;
	    if (DEBUG_DUMP_HEAP && debugDumpHeap) {
		dumpHeap();
	    }
	    if (DEBUG_EXHAUSTED && debugExhausted) {
		Native.print_string("triPizlo: EXHAUSTED: exhausted memory with request for ");
		Native.print_int(size);
		Native.print_string(" bytes.  Re-executing memory request with all debug flags enabled.\n");
		enableAllDebugImpl();
		// this works because if memoryExhausted() is called, the memory allocation
		// request did not lead to anything being changed, other than GC being notified.
		// but since we are in atomic code, that makes no difference.  so, re-executing
		// the allocation request will lead to the same results, modulo the fact that
		// alreadyExhausted will be set to true, so this won't execute twice.
		getMemImpl(size);
		
		// should never get here, because getMemImpl() should call
		// memoryExhausted() again, which should either panic or throw.
		throw Executive.panic("in memoryExhausted: "+
				      "getMemImpl returned successfully on second "+
				      "try, which shouldn't happen.");
	    }
	}
	if (alreadyExhausted) {
	    alreadyExhausted=false;
	}
	
	if (DEBUG_COMPACTION && debugCompaction) {
	  Native.print_string("Out of memory info:\n");
	  Native.print_string("\nrequested size:");	  
	  Native.print_int(size);
	  Native.print_string("\n");
          memoryInfo();	  
	}

	// FIXME: I may want to change this to use outOfMemory()
	Native.print_string("Out of memory in memoryExhausted.\n");
	memoryInfo();

      Native.print_string("Out of memory in memoryExhausted, the earliest OOM was detected at ");
      Native.print_long(memoryExhaustedTime);
      Native.print_string(" (ns)\n");


      Native.print_string("Out of memory in memoryExhausted, attempting to dump VM logs...\n");
      dumpLogs();

      Native.print_string("Out of memory in memoryExhausted, crashing the VM...\n");
	throw Executive.panic("Out Of Memory");
    }

    public VM_Address getMem(int size) {
	throw Executive.panic("triPizlo does not implement getMem directly.");
    }

    static volatile int callsInAllocation = 0;
    
    static VM_Address getMemImpl(int size) {
    
        if (VERIFY_BUG) {
          callsInAllocation ++;
          if (callsInAllocation!=1) {
            throw Executive.panic("getMemImpl executing in multiple mutator threads");
          }
        }
        
	if (DEBUG_ALLOC_FAST && debugAllocFast) {
	    Native.print_string("triPizlo: ALLOC_FAST: allocating object of size ");
	    Native.print_int(size);
	    Native.print_string("\n");
	}
	
	if (PROFILE_ALLOC && profileAlloc) {
	    profileAlloc=false;
//	    allocTimestamp.log(Native.getTimeStamp());
	    allocTimestamp.log(getCurrentTime());
	    allocSize.log(size);
	    profileAlloc=true;
	}
	
	if (VERIFY_HI_PRI_ALLOC && !gcStopped) {
	    gcStopped=true;
	    new Error().printStackTrace();
	    throw Executive.panic("attempt to allocate memory when the GC is not stopped");
	}
	
	if (ALLOC_STATS && allocStats) {
	    if (size<sizeHisto.length) {
		sizeHisto[size]++;
	    } else {
		sizeOverflow++;
	    }
	}

	VM_Address res = null;
	
	if (size<=maxBlockAlloc) {
	    res = getMemSmall(size);
	} else {
	    res = getMemLarge(size);
	}
	
	/*
	if (PROFILE_MEM_USAGE && !USE_REFERENCE_MEMORY_SIZE) {
	  memUsage += size;
	}
	
	if ((PROFILE_MEM_USAGE && !USE_REFERENCE_MEMORY_SIZE) || PROFILE_BLOCK_USAGE) {
	  observeMemUsageChanged();
	}
	*/
	
        if (VERIFY_BUG) {
          callsInAllocation --;
        }	
        
	return res;
    }
    
    
    static boolean triggerGCIfNeeded() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaIgnoreSafePoints, PragmaIgnoreExceptions, PragmaNoPollcheck  {

	 if ( ( (!EFFECTIVE_MEMORY_SIZE && (nUsedBlocks+gcThreshold >= nBlocks)) ||
            (EFFECTIVE_MEMORY_SIZE && (nUsedBlocks+gcThreshold >= effectiveNBlocks))) && !shouldFreeSomeMemory ) {
            
              if (DEBUG_POLLING) {
                Native.print_string("triPizlo: DEBUG_POLLING: triggering GC");
              }
              
              shouldFreeSomeMemory = true;              
              
              if (gcThreadBlockedByTrigger) {
                gcThreadBlockedByTrigger = false;
                
                if (SUPPORT_PERIODIC_SCHEDULER && periodicScheduler && shouldPause) {
                  gcThreadBlockedByScheduler = true ;
                } else {
                  threadManager.makeReady(gcThread);              
                }
              }
              
              if (DEBUG_POLLING) {
                Native.print_string("triPizlo: DEBUG_POLLING: triggered GC");
              }              
              
              if (SUPPORT_MUTATOR_DISABLE) {
                shouldDisableMutator = (!EFFECTIVE_MEMORY_SIZE && (nUsedBlocks+mutatorDisableThreshold >= nBlocks)) ||
                          (EFFECTIVE_MEMORY_SIZE && (nUsedBlocks+mutatorDisableThreshold >= effectiveNBlocks));
              }
                          
              return true;
         }
         return false;    
    }
    
      // size is the value passed to getMemImpl
    public void memoryProfileAfterStamp(int size, VM_Address obj) {
    
      if (PROFILE_MEM_USAGE) {
      
        int osize = objectSize(obj);
        memUsage += osize;
        
        if (PROFILE_MEM_FRAGMENTATION) {
        
          int psize = contiguousPartObjectSize(obj);
          if (size<=maxBlockAlloc) { // note - it was increased by real allocation unit size in the allocation call
	    memSmallObjectFragmentation -= psize;
	    if ( memSmallObjectFragmentation < 0 ) {
	      throw Executive.panic("memSmallObjectFragmentation went negative");
	    }	    
          } else {
	    memLargeObjectFragmentation -= psize;
	    if ( memLargeObjectFragmentation < 0 ) {
	      throw Executive.panic("memLargeObjectFragmentation went negative");
	    }
	    
          }
        }

        observeMemUsageChanged();      
      }
      
    }
    
    public Oop allocate(Blueprint.Scalar bp) throws PragmaAtomicNoYield {
    
        int size = bp.getFixedSize();
	Oop res = stampGCBits(bp.stamp(getMemImpl(size)));
	
	if (PROFILE_MEM_USAGE) {
	  memoryProfileAfterStamp(size, VM_Address.fromObjectNB(res) );
	}
	
	if (VERIFY_BUG && res == null) {
	  Native.print_string("ERROR: allocate returning null\n");
	  throw Executive.panic("fix...");
	}
	return res;
    }
    
    public static VM_Address addressOfArrayElement(VM_Address array, int index) throws PragmaNoReadBarriers,
      PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      
      return addressOfArrayElement(array.asOopUnchecked(), index);
    }
    
    // WARNING: this function is not optimized, intended only for debugging
    public static VM_Address addressOfArrayElement(Oop array, int index) throws PragmaNoReadBarriers,
      PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
    
      Blueprint.Array bp = (Blueprint.Array)array.getBlueprint();

      if (ARRAYLETS) {
        VM_Address addr = VM_Address.fromObject(array);
        return addressOfArrayElement(addr, index, bp.getComponentSize(),0);
      } else {
        return bp.addressOfElement( array, index );
      }
    }

    public static VM_Address addressOfArrayElement(Oop array, int index, int componentSize) throws PragmaNoReadBarriers, 
      PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      
        return addressOfArrayElement(array, index, componentSize, 0);
    }

    
    public static VM_Address addressOfArrayElement(Oop array, int index, int componentSize, int aArray) throws PragmaNoReadBarriers, 
      PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      
      if (ARRAYLETS) {
        return addressOfArrayElement(VM_Address.fromObjectNB(array), index, componentSize, aArray );
      } else {
        Blueprint.Array bp = (Blueprint.Array)array.getBlueprint();
        return bp.addressOfElement( array, index );
      }
    }

    public VM_Address addressOfElement(VM_Address array, int index, int componentSize) throws PragmaNoReadBarriers,
      PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      
      return addressOfArrayElement( array, index, componentSize, 0 );
    }

    public static VM_Address addressOfArrayElement(VM_Address array, int index, int componentSize, int aArray) throws PragmaNoReadBarriers, 
      PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaCAlwaysInline {

      if (ARRAYLETS) {
        if ( (aArray&Assert.ARRAY_UP_TO_SINGLE_ARRAYLET) != 0 ) {
        
          if ( (!COMPACTION && !FORCE_TRANSLATING_BARRIER) || (aArray&Assert.IMAGEONLY)!=0 ) {
          
            // here we know the array has only one copy, so we don't have to dereference at all
          
            int bytesToData =  ObjectModel.getObjectModel().headerSkipBytes() + 
              MachineSizes.BYTES_IN_WORD /* array length field */ +
              MachineSizes.BYTES_IN_ADDRESS; /* first and only arraylet pointer field */
                          
            if (componentSize > MachineSizes.BYTES_IN_WORD) {
              bytesToData = (bytesToData + MachineSizes.BYTES_IN_WORD*2 -1) & ~ (MachineSizes.BYTES_IN_WORD*2 -1);
            }           
          
            return array.add( bytesToData + index*componentSize );
              
          } else {
          
            // we have to follow the first arraylet
            
            return array.add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD /* array length field */)
              .getAddress().add( index*componentSize );
          }
            
        } else {
          int dataByteOffset = index*componentSize;
          int arrayletIndex = dataByteOffset / arrayletSize;
          int offset = dataByteOffset % arrayletSize;

          return array.add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD + 
            arrayletIndex * MachineSizes.BYTES_IN_ADDRESS ).getAddress().add(offset);
        }
        
      } else {
        Blueprint.Array bp = (Blueprint.Array)array.asOopUnchecked().getBlueprint();
        return bp.addressOfElement( array.asOopUnchecked(), index );
      }
    }      
    
    
    private void freeAndZeroArrayletsOf( VM_Address array, int size ) throws PragmaNoPollcheck {
    
      // WARNING: this method may be called with forwarding pointer of array pointing
      //  to a no longer valid location
      //	(can happen in replication, if the other location has been swept before)
          
      if (VERIFY_ARRAYLETS) {
        if ( updatePointer(array) != array ) {
          throw Executive.panic("freeAndZeroArrayletsOf called on old location of an array.");
        }
      }
      
      Blueprint.Array abp = array.asOopUnchecked().getBlueprint().asArray();
      int arraylets = fullArrayletsInArray( abp, abp.getLength(array.asOopUnchecked()) );    
      
      // first arraylet
      VM_Address ptrs = array.add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD);
      
      while(arraylets-- > 0) {
        
        pollSweeping(1);
      
        VM_Address arraylet = ptrs.getAddress();
        if (arraylet.diff(array).uLT(VM_Word.fromInt(size))) {
          // arraylet points to array, which means all following
          // arraylets will as well
          break; 
        }
        if (DEBUG_SWEEP && debugSweep) {
          Native.print_string("triPizlo: DEBUG_SWEEP: freeing arraylet at address ");
          Native.print_ptr(arraylet);
          Native.print_string("\n");
        }

	int blockIdx=blockIdx(arraylet);
        zeroBlock(blockIdx);
        Bits.clrBit(usedBits,blockIdx);
        Bits.clrBit(arrayletBits,blockIdx);
        blocksFreed++; 
        freeBlock(blockIdx);
        
        if (PROFILE_MEM_USAGE && !USE_REFERENCE_MEMORY_SIZE) {
          memUsage -= blockSize;
        }
        
        if (PROFILE_BLOCK_USAGE || ( PROFILE_MEM_USAGE && !USE_REFERENCE_MEMORY_SIZE )) {
          observeMemUsageChanged();	
        }
        
	ptrs = ptrs.add(MachineSizes.BYTES_IN_ADDRESS);
      }
    }
    
    public int sizeOfContinuousArray(Blueprint.Array bp, int length) throws PragmaInline, PragmaAssertNoExceptions {
      return sizeOfArraySpine( bp, length, true );
    }
    
    public static int sizeOfContinuousArraySpine(Blueprint.Array bp, int length) throws PragmaInline, PragmaAssertNoExceptions {
      return sizeOfArraySpine( bp, length, true );
    }
    
    public static int sizeOfRegularArraySpine(Blueprint.Array bp, int length) throws PragmaInline, PragmaAssertNoExceptions {
      return sizeOfArraySpine( bp, length, false );
    }
    
    public static int sizeOfArraySpine(Blueprint.Array bp, int length, boolean contiguous) throws PragmaInline, PragmaAssertNoExceptions {

      if (ARRAYLETS) {
        int headerSize = ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD; // object header + array length
        int componentSize = bp.getComponentSize();
        int dataSize = length*componentSize;
        int spineDataSize = 0;
        
        if (contiguous) {
          spineDataSize = dataSize;
        } else {
          spineDataSize = dataSize % arrayletSize;
        }
         
        int nArrayletPtrs = dataSize/arrayletSize;
        if ( (nArrayletPtrs*arrayletSize != dataSize) && (dataSize>0)) {
          nArrayletPtrs ++;
        }

        int bytesToData = headerSize + nArrayletPtrs*MachineSizes.BYTES_IN_ADDRESS;
        if (componentSize > MachineSizes.BYTES_IN_WORD) {
          bytesToData = (bytesToData + MachineSizes.BYTES_IN_WORD*2 -1) & ~ (MachineSizes.BYTES_IN_WORD*2 -1);
        }

        return (bytesToData + spineDataSize + MachineSizes.BYTES_IN_WORD*2 - 1) & ~( MachineSizes.BYTES_IN_WORD*2 -1 );
        
      } else {
        return (int)bp.computeSizeFor(length);
      }
    }
    
    
    // FIXME: ever used ?
/*    
    public static boolean isContiguousArray( Oop array, Blueprint.Array bp ) {
    
      int length =  bp.getLength(array);
      if (length == 0) {
        return true;
      }
      
      int bytesToData = bytesToDataInSpine( bp, length, true );
      VM_Address arr = VM_Address.fromObjectNB(array); 

      if (COMPACTION) {
        arr = updatePointer(arr); 
          // old location of the array has arraylet pointers to the
          // new array's spine  - so we have to make sure we work
          // with the new one

          // FIXME: this does not currently work with incremental object copy

      }
      VM_Address alet = getArraylet( arr, bp, 0 );
      
      return alet.diff(arr).asInt() == bytesToData;
    }
 */   
    public int continuousArrayBytesToData(Blueprint.Array bp, int length) {
      return bytesToDataInSpine(bp, length, true);
    }
    
    public static int bytesToDataInSpine(Blueprint.Array bp, int length, boolean contiguous) throws PragmaInline, PragmaAssertNoExceptions {
      if (ARRAYLETS) {
        int headerSize = ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD; // object header + array length
        int componentSize = bp.getComponentSize();
        int dataSize = length*componentSize;
        int nArraylets = dataSize/arrayletSize;
        
        if ( (!contiguous) && (nArraylets * arrayletSize == dataSize) ) {
          return -1; // no data in spine
        }
        
        if ( (nArraylets * arrayletSize != dataSize) && (dataSize>0) ) {
          nArraylets ++;
        }
        
        int arrayletPtrsSize = nArraylets * MachineSizes.BYTES_IN_ADDRESS;

        int bytesToData = headerSize + arrayletPtrsSize;
        if (componentSize > MachineSizes.BYTES_IN_WORD) {
          bytesToData = (bytesToData + MachineSizes.BYTES_IN_WORD*2 -1) & ~ (MachineSizes.BYTES_IN_WORD*2 -1);
        } 
        return bytesToData;
      } else {
        return bp.byteOffset(0);
      }
    }
    
    // this is including all arraylets possibly in the spine, including
    // the last arraylet which may not be full
    
    public static int allArrayletsInArray(Blueprint.Array bp, int length) throws PragmaInline, PragmaAssertNoExceptions {
      if (ARRAYLETS) {
        int dataSize = length*bp.getComponentSize();
        int res = dataSize/arrayletSize;
        if ( (res * arrayletSize != dataSize) && (dataSize > 0) ) {
          res ++;
        }
        return res;
      } else {
        return 0;
      }
    }

    public int arrayletPointersInArray(Blueprint.Array bp, int len) {
      return allArrayletsInArray(bp, len);
    }

    // note that for non-contiguous arrays, 
    // full arraylets is the same as external arraylets, due to blockSize == arrayletSize
    public static int fullArrayletsInArray(Blueprint.Array bp, int len) throws PragmaInline, PragmaAssertNoExceptions {
      if (ARRAYLETS) {
        int dataSize = len*bp.getComponentSize();
        return dataSize/arrayletSize ;        
      } else {
        return 0;
      }
    }
    
    public static VM_Address getArraylet(VM_Address array, Blueprint.Array bp, int arrayletIndex) throws PragmaInline {
      return array.add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD + 
        arrayletIndex * MachineSizes.BYTES_IN_ADDRESS ).getAddress();
    }
    
    
    public byte[] allocateContinuousByteArray(int length) {
    
      Oop array = allocateContinuousArray(byteArray, length);
      return (byte[]) VM_Address.fromObjectNB(array).asAnyObject();
    }
    
    
    // FIXME: do we need to be that atomic in array allocation ?
    
    public Oop allocateContinuousArray(Blueprint.Array bp, int len) throws PragmaAtomicNoYield {

      // allocate an array that always has data as a continuous sequence in virtual memory,
      // but can still be used through the abstraction of arraylets
      // (place all arraylets in the spine and place pointers to them to the spine as well)

      if (!ARRAYLETS) {
        return allocateArray(bp, len);
      }

      // FIXME: Could be made more efficient if integrated with functions that calculate the sizes
      
      long size = sizeOfContinuousArraySpine(bp, len);
     
      if (size>memSize) {
        Native.print_string("Out of memory in allocate continuous array.\n");
        throw outOfMemory();
      }
      
      Oop array = stampGCBits(bp.stamp(getMemImpl((int) size), len));
      
      if (PROFILE_MEM_USAGE) {
        memoryProfileAfterStamp((int)size, VM_Address.fromObjectNB(array) );
      }
      
      VM_Address ptr = VM_Address.fromObjectNB(array).add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD );

      int nptrs = allArrayletsInArray(bp, len);

      VM_Address data = VM_Address.fromObjectNB(array).add(bytesToDataInSpine(bp, len, true));
      
      // initialize arraylets in the spine
      for(int i=0;i < nptrs; i++) {
        ptr.setAddress( data.add(i*arrayletSize) );
        ptr = ptr.add( MachineSizes.BYTES_IN_ADDRESS );
      }
                  
      return array;
    }
    
    public Oop allocateArray(Blueprint.Array bp, int len) throws PragmaAtomicNoYield {

      if (!ARRAYLETS) {
        long size = bp.computeSizeFor(len);
        if (size <= memSize) {
          Oop res = stampGCBits(bp.stamp(getMemImpl((int) size), len));
          
          if (PROFILE_MEM_USAGE) {
            memoryProfileAfterStamp((int)size, VM_Address.fromObjectNB(res) );
          }
          
          if (DEBUG_BUG && false) {
            if (bp.getComponentSize()==1) {
              Native.print_string("triPizlo: ALLOC: allocated byte array ");
              printAddr(VM_Address.fromObjectNB(res));
              Native.print_string(" of length ");
              Native.print_int(len);
              Native.print_string(" at source line ");
              int line = Native.getStoredLineNumber();
              Native.print_int(line);
              Native.print_string("\n");
            }
          }
          
          return res;
        } else {
          Native.print_string("Out of memory in allocateArray.\n");
          throw outOfMemory();
        }
      }

      int spineAllocSize = sizeOfRegularArraySpine(bp, len);
      if (spineAllocSize > memSize) {
        Native.print_string("Out of memory in allocateArray - spine.\n");
        throw outOfMemory();
      }
      
      Oop array = stampGCBits(bp.stamp(getMemImpl(spineAllocSize), len));
      
      if (PROFILE_MEM_USAGE) {
        memoryProfileAfterStamp(spineAllocSize, VM_Address.fromObjectNB(array) );
      }
      
      VM_Address ptr = VM_Address.fromObjectNB(array).add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD);
      
      int nptrs = fullArrayletsInArray(bp, len);

      // allocate arraylets and initialize pointers in the spine
      for(int i=0;i < nptrs; i++) {
      
        int block = blockHead;
        
        if (SYNCHRONOUS_TRIGGER) {
          triggerGCIfNeeded();
        }
        updateHighestUsedBlock(block);

        if ((block == -1) || ( EFFECTIVE_MEMORY_SIZE && (nUsedBlocks >= effectiveNBlocks)) ) {
          Native.print_string("Out of memory in allocateArray when allocating external arraylets.\n");
          throw outOfMemory();
        }
        
        blockHead = blockNext[block];
        if (blockHead != -1) {
          blockPrev[blockHead] = -1;
        }
        
        blockNext[block] = -1;
        blockPrev[block] = -1;
        
        sizes[block] = 1;
        
        Bits.setBit(arrayletBits,block);
        Bits.setBit(usedBits,block);
        nUsedBlocks++;
        
        if (PROFILE_MEM_USAGE && !USE_REFERENCE_MEMORY_SIZE) {
          memUsage += blockSize;
        }
        
        if (PROFILE_BLOCK_USAGE || (PROFILE_MEM_USAGE && !USE_REFERENCE_MEMORY_SIZE)) {
          observeMemUsageChanged();
        }
        
        ptr.setAddress( memory.add(block*blockSize) );
        ptr = ptr.add( MachineSizes.BYTES_IN_ADDRESS );
        
        if (DEBUG_ALLOC_SLOW) {
          Native.print_string("triPizlo: DEBUG_ALLOC: allocated arraylet in allocateArray at ");
          printAddr( memory.add(block*blockSize)  );
          Native.print_string("\n");
        }
        
        if (VERIFY_ARRAYLETS) {
          if (inImage(memory.add(block*blockSize))) {
            Native.print_string("triPizlo: ERROR: block on free list is in image\n");
            throw Executive.panic("fix");
          }
        }
        
        if (VERIFY_ALLOCATION) {
          verifyZeroed(memory.add(block*blockSize), blockSize);
        }
      }
      
      // set the last arraylet, if any
      int bytesToData = bytesToDataInSpine(bp, len, false);
      if (bytesToData != -1) {
        ptr.setAddress( VM_Address.fromObjectNB(array).add(bytesToData) );
      }
      
      if (false) {
        Native.print_string("allocateArray returning array ");
        printAddr(VM_Address.fromObjectNB(array));
        Native.print_string("first arraylet pointer of this array is ");
        printAddr(VM_Address.fromObjectNB(array).add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD).getAddress() );
        Native.print_string(" length of the array is ");
        Native.print_int(len);
        Native.print_string(" blueprint is ");
        byte[] str=bp.get_dbg_string();
        Native.print_bytearr_len(str,str.length);
        Native.print_string("\n");
      }


      return array;
    }        
    
    public int getHeapObjectStorageSize(VM_Address addr) {
    
      int blockIdx = blockIdx(addr);
      VM_Address blockBase = memory.add(blockIdx*blockSize);
      int size = sizes[blockIdx];
      if (Bits.getBit(arrayletBits, blockIdx)) {
        return arrayletSize;
      } else if (Bits.getBit(largeBits,blockIdx)) {
        return size * blockSize;
      } else {
        return size;
      }
    }
    
    public int getArraySpineStorageSize(VM_Address array) {
    
      if (inHeap(array)) {
        return getHeapObjectStorageSize(array);
      } else {
        // contiguous array (image, raw memory(
        Blueprint.Array abp = array.asOop().getBlueprint().asArray();
        return sizeOfContinuousArraySpine( abp, abp.getLength( array.asOop() ) );
      }
    }
    
    public Oop cloneNonArrayletArrayAtomic (Oop oop, Blueprint bp) throws PragmaAtomicNoYield, PragmaNoReadBarriers {
    
      int size = (int) bp.getVariableSize(oop);
      Oop res =  stampGCBits(bp.clone(oop, getMemImpl(size)));
      if (PROFILE_MEM_USAGE) {
        memoryProfileAfterStamp( size, VM_Address.fromObjectNB(res) );
      }

      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && updatingPointersWrittenToHeapNeeded ) {
        updateObjectsPayloadReferences(res,bp);
      }
      return res;
      
    }

    // we could probably save some cycles by having a specialized arraycopy function
    // that would take advantage of the knowledge that arrays have the same size,
    // same offsets, and don't need marking... 
        
    public Oop clone(Oop oop) throws PragmaNoReadBarriers {
      
      Blueprint bp = oop.getBlueprint();
        
      if (!ARRAYLETS || !bp.isArray()) {
        return cloneNonArrayletArrayAtomic(oop,bp);
      }
      
      // we are NOT atomic now

      if (DEBUG_ARRAYLETS && debugArraylets) {
        Native.print_string("In clone, arraylet arrays.\n");
      }
        
      S3Blueprint.Array abp = (S3Blueprint.Array)bp;
      int length = abp.getLength(oop);

      // allocate & stamp new array
      Oop res = allocateArray(abp, length);
      copyArrayElementsWithArraylets( oop, 0, abp, res, 0, length, abp.getComponentBlueprint().isReference() );
      
      if (VERIFY_ARRAYCOPY) {
        verifyArraySubsets(oop, res, 0, 0, length, "after array clone");
      }

      if (DEBUG_ARRAYLETS && debugArraylets) {
        Native.print_string("In clone, done.\n");
      }      
      
      return res;
    }

    // doesn't matter if it has read barriers or not
    Oop stampGCBits(Oop oop) {
	if (VERIFY_HEAP_INTEGRITY) {
	    verifyPointer("stampGCBits",oop);
	}
	GCData goop=(GCData)oop.asAnyOop();
	goop.setColor(allocColor);  //barriers don't matter
	  // we don't have to stamp the oldness, because the default is zero
	return goop;
    }
    
    private static void verifyZeroed(VM_Address addr, int size) {
      for(int i=0;i<size;i++) {
        if (addr.getByte()!=0) {
          Native.print_string("triPizlo: VERIFY_ALLOCATION: memory being returned is not zeroed: ");
          printAddr(addr);
          Native.print_string("\n");
          Executive.panic("allocated memory is not zeroed");
        }
        addr = addr.add(1);
      }
    }
    
    // we rely on that this function is atomic
    // it does not have loops, and we rely on that it means there are no back-branches in bytecode
    static VM_Address getMemSmall(int requestSize) throws PragmaNoReadBarriers, PragmaNoPollcheck {
      int alignments=inAlignments(requestSize);
      SizeClass sc=sizeClassBySize[alignments];
      return getMemSmall(sc, requestSize);
    }

    // request size is before rounding up to alignments and then looking up the size class    
    // the request size is however not critical to be correct, it is only used when OOM and for memory usage profiling
    //	(it is not correct when the allocation is called from compaction - moving objects)
    static VM_Address getMemSmall(SizeClass sc, int requestSize) throws PragmaNoReadBarriers, PragmaNoPollcheck, PragmaInline {
    
	int realSize=sc.size;
        VM_Address result = null;

        // (this is an old comment that may not fully apply to current code)
        //
	// once I make it so that instead of having one linked list for all
	// objects that are free of a particular size, to having a linked
	// list of blocks that have free lists, then we could rewrite this
	// to unify the handling of pointer-bump blocks and freelist blocks.
	// basically, if 'null' next pointer could mean bump the pointer.
	// this means that you have to have a check when you read off the
	// next pointer, but it also means that you DON'T have to have any
	// checks to see if the block is suitable for freelist allocation
	// or not.  such a fast path may or may not look faster, but the
	// total code will be less, leading to more inlining opportunities.

	
	if (sc.nonfullHead != -1) {
	  
	    if (DEBUG_ALLOC_FAST_VERBOSE && debugAllocFastVerbose) {
		Native.print_string("triPizlo: ALLOC_FAST_VERBOSE: found freelist, proceeding.\n");
	    }
	    
	    if (DEBUG_ALLOC_FAST_VERBOSE && debugAllocFastVerbose) {
		Native.print_string("triPizlo: ALLOC_FAST_VERBOSE: updating freelist.\n");
	    }

	    result = VM_Address.fromInt(freeHeadsPtrs[ sc.nonfullHead ]);
	    
	    VM_Address next = freeList.getNext(result);
	    freeHeadsPtrs[ sc.nonfullHead ] = next.asInt();
	    
	    if (next != null) {
              if (VERIFY_COMPACTION) {
	        if (gcData.getColor(next) != FREE) {
	          throw Executive.panic("Object on freelist is not FREE.");
                }
              }
              
              freeList.setPrev(next, null);
              
	    } else {

  	      if (DEBUG_ALLOC_FAST_VERBOSE && debugAllocFastVerbose) {
		Native.print_string("triPizlo: ALLOC_FAST_VERBOSE: filled-up nonfull block.\n");
              }

	      /* the block is now full */
	      
	      int newhead = nonfullNext[sc.nonfullHead];
	      
	      /* maybe not necessary */
	      nonfullPrev[sc.nonfullHead] = -1;
	      nonfullNext[sc.nonfullHead] = -1;
	      
	      sc.nonfullHead = newhead;
	      
	      if (sc.nonfullHead != -1) {
	        nonfullPrev[sc.nonfullHead] = -1;
	      }
	    }

            if (VERIFY_COMPACTION) {
              if (gcData.getColor(result) != FREE) {
                throw Executive.panic("Object on freelist is not FREE.");
              }
            }
            
            if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) {
              if (gcData.getOld(result)!=0) {
                memLongDeadFragmentation -= realSize;
                result.setInt(0);
              } else {
                memRecentlyDeadFragmentation -= realSize;
              }
            }

	    // zero out freelist link pointers (this may not be necessary)
	    // it probably is not, as it will be overwritten by stamp functions
	    // - well, it's needed for runtime checks - there is a check if the allocated memory is all zeros
	    // - it'd be needed even for correctness, if we had Yuasa barrier on the respective header fields
	    //	(which we currently do not have)
	    
	    freeList.setPrev(result,null);
	    freeList.setNext(result,null);
	    
	    if (KEEP_OLD_BLUEPRINT) {
	      freeList.setCustom(result, null);
	    }
	    
	} else {
	    if (sc.block==null) {
		if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
		    Native.print_string("triPizlo: ALLOC_SLOW: need to find new block.\n");
		}

		if ((blockHead==-1) || (EFFECTIVE_MEMORY_SIZE && nUsedBlocks>=effectiveNBlocks)) {
                    Native.print_string("Out of memory in getMemSmall().\n");
                    if (memoryExhaustedTime==-1) {
                      memoryExhaustedTime = getCurrentTime();
                    }
		    throw memoryExhausted(requestSize);
		}
		int curBlock=blockHead;

                updateHighestUsedBlock(curBlock);
                if (SYNCHRONOUS_TRIGGER) {
                  triggerGCIfNeeded();
                }
                
		sc.block=memory.add(curBlock*blockSize);
		sc.blockCur=sc.block;
		
		if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
		    Native.print_string("triPizlo: ALLOC_SLOW: got a block: ");
		    Native.print_int(curBlock);
		    Native.print_string(", with address: ");
		    Native.print_ptr(sc.block);
		    Native.print_string(", sizes: ");
		    Native.print_int(sizes[curBlock]);
		    Native.print_string(", uses: ");
		    Native.print_int(uses[curBlock]);
		    Native.print_string(", usedBit: ");
		    Native.print_string(Bits.getBit(usedBits,curBlock) ? "SET" : "NOT-SET");
		    Native.print_string("\n");
		}

		blockHead=blockNext[curBlock];

		// this may not be necessary
		blockNext[curBlock]=-1;
		blockPrev[curBlock]=-1;
		
		if (blockHead>=0) {
		    blockPrev[blockHead]=-1;
		}
		
		nUsedBlocks++;
		sc.nBlocks++;

		if (VERIFY_ALLOCATION) {
                  if (Bits.getBit(usedBits, curBlock)) {
                    throw Executive.panic("allocated small object within a block from free list with used bit set");
                  }	  		
                }
		Bits.setBit(usedBits,curBlock);
		sizes[curBlock]=realSize;
		
		// block memory usage profiling in call sites
		
		if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) {
                  memSizeClassFragmentation += sc.padding;
		}
	    }
	    
	    if (DEBUG_ALLOC_FAST_VERBOSE && debugAllocFastVerbose) {
		Native.print_string("triPizlo: ALLOC_FAST_VERBOSE: allocating by bumping pointer from block: ");
		Native.print_ptr(sc.block);
		Native.print_string("\n");
	    }

	    result=sc.blockCur;
	    sc.blockCur=result.add(realSize);
	    
	    if ((blockSize-sc.blockCur.diff(sc.block).asInt())
		< realSize) {
		if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
		    Native.print_string("triPizlo: ALLOC_SLOW: block is exhausted.\n");
		}
		sc.blockCur=null;
		sc.block=null;
	    }
	}

	if (DEBUG_ALLOC_FAST && debugAllocFast) {
	    Native.print_string("triPizlo: ALLOC_FAST: object allocated: ");
	    Native.print_ptr(result);
	    Native.print_string("\n");
	}	
        
	uses[blockIdx(result)]++;
	sc.nObjects++;

	if (VERIFY_ALLOCATION) {
	  if (VERIFY_ARRAYLETS) {
	    if (Bits.getBit(arrayletBits, blockIdx(result))) {
	      throw Executive.panic("allocated small object within an arraylet");
	    }
	  }

	  if (result.getInt() == FREE) { // the free color.. 
  	    result.setInt(0);
          }

  	  verifyZeroed(result, requestSize);
        }
	
	if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) { 
	  memSmallObjectFragmentation += realSize; // will be made consistent by caller
        }
        
	return result;
    }
    
    // note that this is always called with rescheduling disabled, anyway (PragmaAtomicNoYield)
    // this said, I think it doesn't have to be completely atomic
    
    static VM_Address getMemLarge(int size) throws PragmaNoReadBarriers, PragmaNoPollcheck {
	if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
	    Native.print_string("triPizlo: ALLOC_SLOW: in large object slow path for size ");
	    Native.print_int(size);
	    Native.print_string("\n");
	}
	
	int sizeInBlocks=inBlocks(size);
	
	if (EFFECTIVE_MEMORY_SIZE && ((sizeInBlocks + nUsedBlocks) > effectiveNBlocks)) {
          if (memoryExhaustedTime==-1) {
            memoryExhaustedTime = getCurrentTime();
           }
	  throw memoryExhausted(size);
	}
	
	if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
	    Native.print_string("triPizlo: ALLOC_SLOW: need ");
	    Native.print_int(sizeInBlocks);
	    Native.print_string(" consecutive blocks.\n");
	}

	int cur=0;
	for (;;) {
	    if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
		Native.print_string("triPizlo: ALLOC_SLOW: looking for free block.\n");
	    }
	    
	    cur=Bits.findClr(usedBits,cur);
	    if (cur<0 || cur>=nBlocks) {
		break;
	    }
	    
	    if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
		Native.print_string("triPizlo: ALLOC_SLOW: found potential start: ");
		Native.print_int(cur);
		Native.print_string("\n");
	    }
	    
	    // found a span of free blocks.  are there enough free blocks
	    // to satisfy the request?
	    
	    int end=Bits.findSet(usedBits,nBitWords,cur+1);
	    if (DEBUG_ALLOC_SLOW_VERBOSE && debugAllocSlowVerbose) {
		Native.print_string("triPizlo: ALLOC_SLOW_VERBOSE: findSet returned ");
		Native.print_int(end);
		Native.print_string(", while nBlocks = ");
		Native.print_int(nBlocks);
		Native.print_string("\n");
	    }
	    if (end<0 || end>nBlocks) {
		end=nBlocks;
	    }
	    
	    if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
		Native.print_string("triPizlo: ALLOC_SLOW: end of span: ");
		Native.print_int(end);
		Native.print_string("\n");
	    }
	    
	    if (end-cur >= sizeInBlocks) {
		// return this
		VM_Address result=memory.add(cur*blockSize);
		
		if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
		    Native.print_string("triPizlo: ALLOC_SLOW: all good, returning: ");
		    Native.print_ptr(result);
		    Native.print_string("\n");
		}
		
		// first, mark it as used.
		// WARNING: this is atomic and slow!  yikes!
		for (int i=cur;i<cur+sizeInBlocks;++i) {
		
		    updateHighestUsedBlock(i);
		    
		    Bits.setBit(usedBits,i);
		    Bits.setBit(largeBits,i);
		    if (blockPrev[i]<0) {
			blockHead=blockNext[i];
		    } else {
			blockNext[blockPrev[i]]=blockNext[i];
		    }
		    if (blockNext[i]>=0) {
			blockPrev[blockNext[i]]=blockPrev[i];
		    }
		    blockNext[i]=-1;
		    blockPrev[i]=-1;
		    sizes[i]=-1; // this marks the block as a continuation
		}
		
		if (SYNCHRONOUS_TRIGGER) {
  		  triggerGCIfNeeded();
                }
		
		// next, set the size
		sizes[cur]=sizeInBlocks;
		
		// do some book keeping
		nUsedBlocks+=sizeInBlocks;
		
         
         	if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) { 
         	  memLargeObjectFragmentation += sizeInBlocks * blockSize; // will be made consistent by caller
                }
         
                // memory usage profiling in call sites
		
                if (VERIFY_ALLOCATION) {
                  verifyZeroed(result, size);
                }
                
		return result;
	    }
	    
	    // failed, keep looking.
	    cur=end;
	    if (cur>=nBlocks) {
		break;
	    }
//	    pollcheckInstr();
//	has no effect since we are PragmaAtomicNoYield anyway
	}
	
	if (DEBUG_ALLOC_SLOW && debugAllocSlow) {
	    Native.print_string("triPizlo: ALLOC_SLOW: could not find sufficiently large span.\n");
	}
	
	if (memoryExhaustedTime==-1) {
          memoryExhaustedTime = getCurrentTime();
        }
	throw memoryExhausted(size);
    }
    
    private static volatile boolean gcReschedulingExpected = true;
    
    private static long afterMutatorTime = 0;
    private static int afterMutatorLine = 0;
    
    public static void pollcheck() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
    
      if (VERIFY_POLLING) {
        if (!threadManager.isReschedulingEnabled()) {
          throw Executive.panic("Rescheduling is NOT enabled in RTGC's pollcheck() before call to pollcheckInstr() !");
        }
      }
      
      if (VERIFY_POLLING && EXCLUDED_BLOCKS) {
        if (enteredExcludedBlocks > 0) {
          throw Executive.panic("pollcheck called when in an excluded block.");
        }
      }
      
      if (VERIFY_POLLING) {
        gcReschedulingExpected = true;
      }
      
      if (REPORT_LONG_LATENCY && longLatency>0 && !vmIsShuttingDown) {

        long beforeMutatorTime = getCurrentTime();
        long latency = beforeMutatorTime - afterMutatorTime;
        boolean tooLong = latency>longLatency && afterMutatorTime>0 ;
        boolean newRecord = latency>maxObservedLatency && afterMutatorTime>0;
        int beforeMutatorLine = Native.getStoredLineNumber();


        if (beforeMutatorLine!=afterMutatorLine && afterMutatorLine!=0) {
          Native.print_string("pollcheck(): ERROR-maybe: bypassed pollcheck at line ");
          Native.print_int(beforeMutatorLine);
          Native.print_string("\n");
        }
        
        pollcheckInstr(); // line number is stored _after_ a pollcheck

        afterMutatorTime = getCurrentTime();
        afterMutatorLine = Native.getStoredLineNumber();        

        if (tooLong) {
          Native.print_string("pollcheck(): long latency detected between pollchecks at lines ");
          Native.print_int(beforeMutatorLine);
          Native.print_string(" and ");
          Native.print_int(afterMutatorLine);
          Native.print_string(", latency was ");
          Native.print_long(latency);
          Native.print_string(" ns \n");        
        }
        
        if (newRecord) {
          maxObservedLatency = latency;
          maxObservedLatencyLineFrom = beforeMutatorLine;
          maxObservedLatencyLineTo = afterMutatorLine;
        }


      } else {
        pollcheckInstr();
      }
      
      if (VERIFY_POLLING) {
        gcReschedulingExpected = false;
      }      
      
      if (VERIFY_POLLING) {
        if (!threadManager.isReschedulingEnabled()) {
          throw Executive.panic("Rescheduling is NOT enabled in RTGC's pollcheck() after call to pollcheckInstr() !");
        }
        if (gcThreadBlockedByScheduler) {
          throw Executive.panic("gcThreadBlockedByScheduler true after pollcheckInstr");
        }
      }
      
    }
    
    public static final void pollcheckInstr() throws BCpollcheck {};
    
    public static class BCpollcheck extends PragmaTransformCallsiteIR implements s3.services.bootimage.Ephemeral.Void {
      static {
        register("s3.services.memory.triPizlo.TheMan$BCpollcheck", new byte[] { (byte) POLLCHECK } );
      }
    }
  

/*
    static void pollSlow() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaIgnoreSafePoints, PragmaIgnoreExceptions {

	    boolean oldGCStopped=gcStopped;
	    gcStopped=true;  // needs to be before tic(), see below 
	    pollLat.tic();
	    
	    doPoll();

	    pollLat.toc();
	    gcStopped=oldGCStopped;  // this needs to come after the toc(), because
				     //  pollLat may be a LoggingTimer, and a LoggingTimer
				     //  may want to allocate memory. 
	    outer.lose(pollLat.lat);
	    inner.lose(pollLat.lat);
	    stackScan.lose(pollLat.lat);
    }
*/

    static void pollStackScanning() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaIgnoreExceptions, PragmaNoPollcheck, PragmaInline {
    
        if (disableConcurrency || stackUninterruptible) {
          return;
        }
        
        pollcheck();
    }
    
    static void pollMarking() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaIgnoreExceptions, PragmaNoPollcheck, PragmaInline {

        if (disableConcurrency || markUninterruptible) {
          return;
        }
        
        pollcheck();
    }
        
    
    static void pollSweeping(int index) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaIgnoreExceptions, PragmaNoPollcheck, PragmaInline {
    
      if (false && (index <= interruptibilityMask.length && !interruptibilityMask[index])) {
        return ;
      }
    
      pollSweeping();
      
      if (VERIFY_BUG) {
        if (callsInAllocation!=0) {
          throw Executive.panic("gcThread interrupted getMemImpl in sweep");
        }
      }
    }
    
    
    static void pollSweeping() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaIgnoreExceptions, PragmaNoPollcheck, PragmaInline {
    
        if (disableConcurrency || sweepUninterruptible) {
          return;
        }
        
        pollcheck();
        
    }

    static void pollLogging() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaIgnoreExceptions, PragmaNoPollcheck, PragmaInline {
    
        if (disableConcurrency || logUninterruptible) {
          return;
        }
        
        pollcheck();
        
    }
    
    static void pollCompacting() throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaIgnoreExceptions, PragmaNoPollcheck, PragmaInline {
    
        if (disableConcurrency || compactUninterruptible) {
          return;
        }
        
        pollcheck();
        
    }
    
    /*
    static Object getPinnedObjects() {
      return ((TheMan)(MemoryManager.the())).pinnedObjects;
    }
    */
    
    // this must not be called when arraylets are used
    static void updateObjectsPayloadReferences(Oop oop, Blueprint bp) throws PragmaNoReadBarriers, PragmaNoPollcheck { // we are atomic here and we have only one copy of oop
    
        if (VERIFY_COMPACTION) {
          VM_Address addr = VM_Address.fromObjectNB(oop);
          if (addr != translateNonNullPointer(addr)) {
            Native.print_string("triPizlo: ERROR: moved object in clone.\n");
            throw Executive.panic("error");
          }
        }
        
    	if (bp.isArray()) {
    	    if (VERIFY_ARRAYLETS && ARRAYLETS) {
    	      Native.print_string("triPizlo: ERROR: updateObjectsPayloadReferences called while arraylets are on.");
    	      throw Executive.panic("error");
    	    }
    	    
	    Blueprint.Array abp = bp.asArray();
	    if (abp.getComponentBlueprint().isReference()) {
		VM_Address p = abp.addressOfElement(oop, 0);
		VM_Address pend = p.add(VM_Word.widthInBytes()
					* abp.getLength(oop));

		while (p.uLT(pend)) {
                  
                  updatePointerAtAddress(p);
                  p=p.add(VM_Word.widthInBytes());
		}
	    }
	}
	else {
	    VM_Address base = VM_Address.fromObjectNB(oop);
	    int[] offset = bp.getRefMap();

	    for (int i = 0; i < offset.length; i++) {
	        updatePointerAtAddress( base.add(offset[i]) );
	    }
	}
    }
    
    // must be called on the new location of the object (oop)
    static void walkObject(Oop oop,
			   Blueprint bp) throws PragmaNoReadBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaIgnoreExceptions,
			                       PragmaNoPollcheck  {
			  
	if (VERIFY_COMPACTION || VERIFY_BUG) {
	  VM_Address ptr = VM_Address.fromObjectNB(oop);
	  VM_Address ptrF = null;
	  if (REPLICATING) {
	    ptrF = updatePointer(ptr);
	  }
	  if (BROOKS) {
	    ptrF = translatePointer(ptr);
	  }
	  
	  if (ptr != ptrF) {
	    Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: walkObject called on old location of a moved object:");
	    printAddr(ptr);
	    Native.print_string("=>");
	    printAddr(ptrF);
	    Native.print_string("\n");
	  }
	}		   
		
	if (VERIFY_COMPACTION) {
	  VM_Address ptr = VM_Address.fromObjectNB(oop);
	  int color = gcData.getColor(ptr);
	  if (color == FREE) {
	    Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: walkObject walking free object..\n");
	    throw Executive.panic("this cannot be right");
	  }
	}		   

	if (VERIFY_FAST_IMAGE_SCAN) {
	    walkObjectPointerSource=VM_Address.fromObject(oop);
	}
	if (DEBUG_WALK && debugWalk) {
	    Native.print_string("triPizlo: WALK: Walking object ");
	    Native.print_ptr(VM_Address.fromObject(oop));
	    Native.print_string(", which has blueprint at ");
	    Native.print_ptr(VM_Address.fromObject(bp));
	    Native.print_string(", and the debug string is");
	    //Native.print_string(", and the debug string is DISABLED");
	    byte[] str=bp.get_dbg_string();
	    Native.print_bytearr_len(str,str.length);
	    Native.print_string("\n");
        }
	for (int i = 0;
	     i < ObjectModel.getObjectModel().maxReferences();
	     i++) {
	    if (DEBUG_WALK_VERBOSE && debugWalkVerbose) {
		Native.print_string("triPizlo: WALK_VERBOSE: walking object model reference #");
		Native.print_int(i);
		Native.print_string(" of value ");
		printAddr(VM_Address.fromObjectNB(oop.getReference(i)));
		Native.print_string("\n");
	    }
	    
	    if (COMPACTION) {
	      VM_Address oldPtr = VM_Address.fromObjectNB(oop.getReference(i));
	      if (oldPtr.isNonNull()) {
                if ( BROOKS || ( REPLICATING && gcData.getOld(oldPtr)!=0 )) {
                  VM_Address newPtr = translateNonNullPointer(oldPtr);
                  Oop.WithUpdate wu = (Oop.WithUpdate)oop.asAnyOop();
                  wu.updateReference(i, newPtr.asOop()); // FIXME: updateReference has the same branch (even slower)	    	        
                  markNonNull(newPtr);
                } else {
                  markNonNull(oldPtr);
                }
              }
            } else {
              mark( VM_Address.fromObject(oop.getReference(i)) );
            }
	}
	if (bp.isArray()) {
	    if (DEBUG_WALK_VERBOSE && debugWalkVerbose) {
		Native.print_string("triPizlo: WALK_VERBOSE: object is an array.\n");
	    }
	    Blueprint.Array abp = bp.asArray();
	    if (abp.getComponentBlueprint().isReference()) {
	    
              if (!ARRAYLETS) {
		VM_Address p = abp.addressOfElement(oop, 0);
		VM_Address pend = p.add(VM_Word.widthInBytes()
					* abp.getLength(oop));
		if (DEBUG_WALK_VERBOSE && debugWalkVerbose) {
		    Native.print_string("triPizlo: WALK_VERBOSE: object is an array of pointers, with begin = ");
		    Native.print_ptr(p);
		    Native.print_string(" and end = ");
		    Native.print_ptr(pend);
		    Native.print_string("\n");
		}
		while (p.uLT(pend)) {
		    pollMarking();
		    if (DEBUG_WALK_VERBOSE && debugWalkVerbose) {
			Native.print_string("triPizlo: WALK_VERBOSE: walking array, at ");
			Native.print_ptr(p);
			Native.print_string(", with end = ");
			Native.print_ptr(pend);
			Native.print_string("\n");
		    }
		    
                    updateAndMarkAt(p);
		    p=p.add(VM_Word.widthInBytes());
		}
              } else {

                int elems = abp.getLength(oop);
                if (elems > 0) {
                
                  VM_Address array = VM_Address.fromObjectNB(oop);
                  
                  int arrayletsOffset = ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD;
                  int innerElems = arrayletSize / MachineSizes.BYTES_IN_ADDRESS;
                  if (innerElems > elems) {
                    innerElems = elems;
                  }
                  int maxAtomicMark = (maxAtomicBytesCopy * 2)/MachineSizes.BYTES_IN_ADDRESS ;
                  
                  while(elems > 0) {
                  
                    VM_Address addr = array.add(arrayletsOffset).getAddress();

                    while( innerElems >= maxAtomicMark ) {
                      // pollMarking(); FIXME: this is just a test to make the marking more interruptible
                      
                      for(int o=0; o<maxAtomicMark; o++) {
                        pollMarking(); // ***
                        updateAndMarkAt(addr);
                        addr = addr.add(MachineSizes.BYTES_IN_ADDRESS);
                      }
                      
                      innerElems -= maxAtomicMark;
                      elems -= maxAtomicMark;
                    }
                    
                    pollMarking();
                    
                    for(int o=0; o<innerElems; o++) {
                      updateAndMarkAt(addr);
                      addr = addr.add(MachineSizes.BYTES_IN_ADDRESS);
                    }
                  
                    elems -= innerElems;
                    if (elems > 0) {
                      arrayletsOffset += MachineSizes.BYTES_IN_ADDRESS;
                      innerElems = arrayletSize / MachineSizes.BYTES_IN_ADDRESS;
                      if (innerElems > elems) {
                        innerElems = elems;
                      }
                    }
                  }
                }        
              }
            }
	}
	else {
	    VM_Address base = VM_Address.fromObjectNB(oop); // the function already requires the oop to be forwarded
	    int[] offset = bp.getRefMap();
	    if (DEBUG_WALK_VERBOSE && debugWalkVerbose) {
		Native.print_string("triPizlo: WALK_VERBOSE: walking object with bp = ");
		Native.print_ptr(VM_Address.fromObject(bp));
		Native.print_string("(");
                byte[] str=bp.get_dbg_string();
                Native.print_bytearr_len(str,str.length);
                Native.print_string(") ");
		Native.print_string(", ref map at ");
		Native.print_ptr(VM_Address.fromObject(offset));
		Native.print_string(", and ");
		Native.print_int(offset.length);
		Native.print_string(" pointers.\n");
	    }
	    for (int i = 0; i < offset.length; i++) {

// now this is already merged into offset at build time
//	        if (!bp.refMaskGet(offset[i])) {
//	          continue;
//	        }

		if (DEBUG_WALK_VERBOSE && debugWalkVerbose) {
		    Native.print_string("triPizlo: WALK_VERBOSE: walking pointer #");
		    Native.print_int(i);
		    Native.print_string(", which is at offset ");
		    Native.print_int(offset[i]);
		    Native.print_string("\n");
		}
		VM_Address ptr=base.add(offset[i]);
		if (DEBUG_WALK_VERBOSE && debugWalkVerbose) {
		    Native.print_string("triPizlo: WALK_VERBOSE: the address of the pointer is ");
		    Native.print_ptr(ptr);
		    Native.print_string("\n");
		}
		
		pollMarking(); // overkill ?
                updateAndMarkAt(ptr);
	    }
	}
	if (DEBUG_WALK_VERBOSE && debugWalkVerbose) {
	    Native.print_string("triPizlo: WALK_VERBOSE: walked object.\n");
	}
    }
    
    static void walkObject(VM_Address addr) throws PragmaAssertNoSafePoints, PragmaNoPollcheck, PragmaAssertNoExceptions {
	Oop oop = addr.asOop();
	Blueprint bp = oop.getBlueprint();
	walkObject(oop,bp);
    }

    static void walkObject(VM_Address addr, Blueprint bp) throws PragmaAssertNoSafePoints, PragmaNoPollcheck, PragmaAssertNoExceptions {
	Oop oop = addr.asOop();
	walkObject(oop,bp);
    }
    
    // for memory usage profiling only
    // should return the size of the object+header at the address
    //	(excluding external arraylets, if any)
    
    static int objectSize(VM_Address addr) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      return objectSize(addr, USE_REFERENCE_MEMORY_SIZE );
    }

    static int contiguousPartObjectSize(VM_Address addr) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      return objectSize(addr, false );
    }


    static int objectSize(VM_Address addr, boolean referenceSize) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
	Oop oop=addr.asOopUnchecked();
	Blueprint bp=oop.getBlueprint();
	if (bp==null) {
	    return 0;
	} else {
            if (bp.isArray()) {
            
              Blueprint.Array abp = bp.asArray();
              // we have to read the array length from the original location of an object when using Brooks,
              // because the new location might already be reused by other object / zeroed / ...
              int length = addr.add( ObjectModel.getObjectModel().headerSkipBytes() ).getInt();
              
              if (!ARRAYLETS || referenceSize) {
                return (int)abp.computeSizeFor( length );
              }
              
              // array with arraylets
              int bytesToData = bytesToDataInSpine( abp, length, true );
              
              VM_Address alet = getArraylet( addr, abp, 0 );
              
              if (alet.diff(addr).asInt() == bytesToData) {
                return sizeOfContinuousArraySpine( abp, length );
              }
              
              if (COMPACTION) {
                VM_Address newAddr = updatePointer(addr);
                
                if (alet.diff(newAddr).asInt() == bytesToData) {
                  return sizeOfContinuousArraySpine( abp, length );
                } 
              }
              return sizeOfRegularArraySpine( abp, length );

            } else {
              return bp.getFixedSize();
            }
    	}
    }

    static int imageAddressToPageIndex( VM_Address addr ) throws PragmaInline, PragmaAssertNoExceptions, PragmaAssertNoSafePoints  {
      return (addr.asInt() - imageBaseAddress/imgPageSize*imgPageSize ) / imgPageSize;
    }
    
    static VM_Address pageIndexToImageAddress( int pageIdx ) {
      return VM_Address.fromInt(imageBaseAddress/imgPageSize*imgPageSize + pageIdx*imgPageSize);
    }
    
    private static abstract class ImageScanner extends ExtentWalker.Scanner {
      public int getVariableSize(Blueprint bp, Oop o) { // this only works for image, where all arrays are contiguous
      
        if (ARRAYLETS && bp.isArray()) {
            Blueprint.Array abp = bp.asArray();
            return sizeOfContinuousArraySpine( abp, abp.getLength(o) );
        } else {
          return super.getVariableSize(bp, o);
        }
      }  
    }
    
    static ImageScanner mc = new ImageScanner() {
    
            protected void pollWalking() {
              pollMarking();
            }
            
	    protected void walk (Oop oop, Blueprint bp) {
	        //pollMarking();
	        // must forward oop
	        VM_Address addr = VM_Address.fromObjectNB(oop);
	        // ??? why forwarding a pointer to the image ?
	        //walkObject( updateNonNullPointer( addr ), bp );
	        
	        gcData.setColor( addr, allocColor ); // this will be GREYBLACK
	                  // this will prevent subsequent marking and duplicit walking
                          // of image objects
	                  
	        walkObject( addr, bp );
	    }
	    
	    /*
	      start walking image from the beginning of the page of index
	      pageIdx, walk all dirty objects starting at this page ; return
	      the index of the page where next scanning should be done
	      (it is the first following non-continued page)
            */
    };

    protected int walkImageFromPage(int pageIdx) {
	VM_Address addr=pageIndexToImageAddress(pageIdx);
	if (DEBUG_WALK_IMAGE && debugWalkImage) {
	    Native.print_string("triPizlo: WALK_IMAGE: walking image from page ");
	    Native.print_int(pageIdx);
	    Native.print_string(" which is at ");
	    Native.print_ptr(addr);
	    Native.print_string("\n");
	}
	
	return walkImageFromAddress( pageIndexToImageAddress(pageIdx) );
    }
    
    protected int walkImageFromAddress(VM_Address addr) {
	if (DEBUG_WALK_IMAGE && debugWalkImage) {
	    Native.print_string("triPizlo: WALK_IMAGE: walking image from address ");
	    Native.print_ptr(addr);
	    Native.print_string("\n");
	}
	
	VM_Address resultAddr = mc.walkRet( addr, addr.add(imgPageSize) );
	int result = imageAddressToPageIndex( resultAddr );
	
	if (DEBUG_WALK_IMAGE && debugWalkImage) {
	    Native.print_string("triPizlo: WALK_IMAGE: got back ");
	    Native.print_ptr(resultAddr);
	    Native.print_string(", which translates to ");
	    Native.print_int(result);
	    Native.print_string("\n");
	}
	
	return result;
    }



    public boolean caresAboutDisableConcurrency() { return true; }
    public void setDisableConcurrency(boolean disableConcurrency) {
	TheMan.disableConcurrency=disableConcurrency;
    }
    
    public boolean caresAboutStackUninterruptible() { return true; }
    public void setStackUninterruptible(boolean stackUninterruptible) {
    
        if (DEBUG_INTERRUPTIBLE_STACK_SCANNING) {
	    Native.print_string("triPizlo: INTERRUPTIBLE_STACK_SCANNING: setStackUninterruptible( ");
	    Native.print_string( stackUninterruptible ? "true" : "false");
	    Native.print_string(")\n");        
        }    
	TheMan.stackUninterruptible=stackUninterruptible;
    }
    
    public boolean caresAboutMarkUninterruptible() { return true; }
    public void setMarkUninterruptible(boolean markUninterruptible) {
    
	TheMan.markUninterruptible=markUninterruptible;
    }
    
    public boolean caresAboutSweepUninterruptible() { return true; }
    public void setSweepUninterruptible(boolean sweepUninterruptible) {
	TheMan.sweepUninterruptible=sweepUninterruptible;
    }

    public boolean caresAboutLogUninterruptible() { return true; }
    public void setLogUninterruptible(boolean logUninterruptible) {
	TheMan.logUninterruptible=logUninterruptible;
    }

    public boolean caresAboutCompactUninterruptible() { return true; }
    public void setCompactUninterruptible(boolean compactUninterruptible) {
	TheMan.compactUninterruptible=compactUninterruptible;
    }
    
    public boolean caresAboutLongLatency() { return REPORT_LONG_LATENCY; }
    public void setLongLatency(long latency) {
      longLatency = latency;
    }

    public boolean caresAboutLongPause() { return REPORT_LONG_PAUSE; }
    public void setLongPause(long pause) {
      longPause = pause;
    }
    
    public void setInterruptibilityMask(String setMask) {
      int setLength = setMask.length();
      int max = setLength;
      
      if (max>interruptibilityMask.length) {
        max = interruptibilityMask.length;
      }
      
      Native.print_string("triPizlo: setting interruptibility mask...\n");
      for(int i=0; i<max ; i++) {
        Native.print_int(i);
        Native.print_string(" ... ");
        
        boolean interruptible = setMask.charAt(i)=='1';
        Native.print_boolean(interruptible);
        Native.print_string("\n");
        
        interruptibilityMask[i] = interruptible;
      }
      Native.print_string("\n");
    }
    
    public boolean caresAboutEnableTimeTrace() { return true; }
    public void setEnableTimeTrace(boolean enableTimeTrace) throws PragmaAtomicNoYield {
	if (enableTimeTrace) {
          pollLat=new LoggingTimer("pollLat","gc_pause.txt",150000);
        } else {
          pollLat=new Timer("pollLat");
        }
        
        if (enableTimeTrace && !PAUSE_ONLY) {
    //	    threadScan=new LoggingTimer("threadScan","thread_scan.txt",2000);	    
	    imageScan=new LoggingTimer("imageScan","image_scan.txt",2000);	    	    
	    heapScan=new LoggingTimer("heapScan","heap_scan.txt",2000);	    	    	    
	    monitorScan=new LoggingTimer("monitorScan","monitor_scan.txt",2000);	    	    	    	    
	    conservativeScan=new LoggingTimer("conservativeScan","conservative_scan.txt",2000);	    	    
	    preciseScan=new LoggingTimer("preciseScan","precise_scan.txt",2000);	    	    
//	    prepareScanA=new LoggingTimer("prepareScanA","prepare_scan_a.txt",2000);	    	    	    
//	    prepareScanB=new LoggingTimer("prepareScanB","prepare_scan_b.txt",2000);	    	    	    
	    stackScan=new LoggingTimer("stackScan","stack_scan.txt",200);
	    markTimer=new LoggingTimer("markTimer","mark.txt",200);
	    stackUpdateTimer=new LoggingTimer("stackUpdate", "stack_update.txt",200);
	    sweepTimer=new LoggingTimer("sweep", "sweep.txt",200);
	    compactionTimer=new LoggingTimer("compaction", "compaction.txt",200);
	    sortTimer=new LoggingTimer("sort", "sort.txt",200);
//	    copyTimer=new LoggingTimer("copy", "copy.txt",150000);	    
	} else {
//	    threadScan=new Timer("threadScan");	    
	    heapScan=new Timer("heapScan");	    	    
	    monitorScan=new Timer("monitorScan");	    	    	    
	    imageScan=new Timer("imageScan");	    	    
	    conservativeScan=new Timer("conservativeScan");	    	    
	    preciseScan=new Timer("preciseScan");	    	    
//	    prepareScanA=new Timer("prepareScanA");	    	    	    
//	    prepareScanB=new Timer("prepareScanB");	    	    	    
	    stackScan=new Timer("stackScan");
	    markTimer=new Timer("mark");
	    stackUpdateTimer=new Timer("stackUpdate");
	    sweepTimer=new Timer("sweep");
	    compactionTimer=new Timer("compaction");	    
	    sortTimer=new Timer("sort");
//	    copyTimer=new Timer("copy");	    
	}
        timeProfile = enableTimeTrace;	
    }
    
    public void setGCThreshold(long gcThreshold) throws PragmaAtomicNoYield {
	this.gcThreshold=(int)gcThreshold/blockSize;
    }
    
    public void setCompactionThreshold(long compactionThreshold) throws PragmaNoPollcheck {
      this.compactionThreshold = (int)compactionThreshold/blockSize;
    }


    public void setMutatorDisableThreshold(long mutatorDisableThreshold) throws PragmaNoPollcheck {
    
      if (!SUPPORT_MUTATOR_DISABLE) {
        Native.print_string("ERROR: Mutator disable threshold not supported, but attempted to be set !\n");
      }
      this.mutatorDisableThreshold = (int)mutatorDisableThreshold/blockSize;
    }
    
    public void setEffectiveMemorySize(long effectiveMemSize) throws PragmaNoPollcheck {
      this.effectiveMemSize = blockRoundUp((int)effectiveMemSize);
      this.effectiveNBlocks = this.effectiveMemSize / blockSize;
      
      if (this.effectiveNBlocks > nBlocks) {
        throw Executive.panic("Cannot set effective memory size to be larger than the compiled-in memory size.\n");
      }
    }
    
    public void enableAllocProfiling() throws PragmaAtomicNoYield {
	if (PROFILE_ALLOC) {
	    if (profileAlloc) {
		return;
	    }
	    allocTimestamp=new NumberLog("alloc_timestamp.txt");
	    allocSize=new NumberLog("alloc_size.txt");
	    profileAlloc=true;
	}
    }
    
    public void enableSizeHisto(int sizeHistoSize) throws PragmaAtomicNoYield {
	sizeHisto=allocIntArrayRaw(sizeHistoSize);
	sizeOverflow=0;
	allocStats=true;
    }
    
    public void enableProfileMemUsage() throws PragmaAtomicNoYield {
	if (PROFILE_BLOCK_USAGE || PROFILE_MEM_USAGE) {
	    if (profileMemUsage) {
		return;
	    }
	    memUsageTimestamp=new NumberLog("mem_usage_timestamp.txt");
	    blockUsageValue=new IntNumberLog("block_usage_value.txt");
	    memUsageValue=new IntNumberLog("mem_usage_value.txt");
	    memSmallObjectFragmentationValue = new IntNumberLog("mem_small_object_fragmentation_value.txt");
	    memSizeClassFragmentationValue = new IntNumberLog("mem_size_class_fragmentation_value.txt");
	    memLargeObjectFragmentationValue = new IntNumberLog("mem_large_object_fragmentation_value.txt");
	    memRecentlyDeadFragmentationValue = new IntNumberLog("mem_recently_dead_fragmentation_value.txt");
	    memLongDeadFragmentationValue = new IntNumberLog("mem_long_dead_fragmentation_value.txt");
	    memInReplicasValue = new IntNumberLog("mem_in_replicas.txt");
	    
	    profileMemUsage=true;
	}
    }
    
    static int memTraceCounter = 0;
    static void observeMemUsageChanged() {

        if ( PROFILE_MEM_USAGE && MAX_USED_MEM_PROFILE ) {
          if (memUsage > maxMemUsage) {
            maxMemUsage = memUsage;
          }
        }
        
        if ( PROFILE_BLOCK_USAGE && MAX_USED_MEM_PROFILE ) {
          if (nUsedBlocks > maxUsedBlocks) {
            maxUsedBlocks = nUsedBlocks;
          }
        }

	if ((PROFILE_BLOCK_USAGE || PROFILE_MEM_USAGE) 
	    && profileMemUsage
	    && ( FULL_MEM_PROFILE  || PERIODIC_MEM_PROFILE )
	    ) {
	    
	    if ( PERIODIC_MEM_PROFILE && ((memTraceCounter++) % memTracePeriod != 0) ) {
	      return ;
	    }
	    
	    boolean lastGCStopped=gcStopped;
	    gcStopped=true;
	    profileMemUsage=false;
//	    memUsageTimestamp.log(Native.getTimeStamp());
	    memUsageTimestamp.log(getCurrentTime());
	    if (PROFILE_BLOCK_USAGE) {
		blockUsageValue.log(nUsedBlocks);
	    }
	    if (PROFILE_MEM_USAGE) {
		memUsageValue.log(memUsage);
	    }
	    if (PROFILE_MEM_FRAGMENTATION) {
	      memSmallObjectFragmentationValue.log(memSmallObjectFragmentation);
	      memSizeClassFragmentationValue.log(memSizeClassFragmentation);
	      memLargeObjectFragmentationValue.log(memLargeObjectFragmentation);
	      memRecentlyDeadFragmentationValue.log(memRecentlyDeadFragmentation);
	      memLongDeadFragmentationValue.log(memLongDeadFragmentation);
	      memInReplicasValue.log(memInReplicas);
	    }
	    if (true) {
	      if (memUsage<0) {
	        throw Executive.panic("memUsage went negative");
	      }
	    }
	    gcStopped=lastGCStopped;
	    profileMemUsage=true;
	}
    }

    private static void dumpLogs() throws PragmaNoPollcheck {

	pollLat.dump();
//	threadScan.dump();
	imageScan.dump();
	heapScan.dump();	
	monitorScan.dump();
	conservativeScan.dump();	
	preciseScan.dump();	
//	prepareScanA.dump();		
//	prepareScanB.dump();		
	stackScan.dump();
	markTimer.dump();
	stackUpdateTimer.dump();
	sweepTimer.dump();
	compactionTimer.dump();
	sortTimer.dump();
//	copyTimer.dump();	
	
	if (PROFILE_MEM_USAGE && MAX_USED_MEM_PROFILE) {
	  Native.print_string("Maximum used memory: ");
	  Native.print_int(maxMemUsage);
	  Native.print_string("\n");
	}

	if (PROFILE_BLOCK_USAGE && MAX_USED_MEM_PROFILE) {
	  Native.print_string("Maximum used blocks: ");
	  Native.print_int(maxUsedBlocks);
	  Native.print_string("\n");
	}

	if (PROFILE_MEM_USAGE && MAX_LIVE_MEM_PROFILE) {
	  Native.print_string("Maximum observed live memory: ");
	  Native.print_int(liveMemUsage);
	  Native.print_string("\n");
	}

	if (PROFILE_BLOCK_USAGE && MAX_LIVE_MEM_PROFILE) {
	  Native.print_string("Maximum observed blocks after sweep: ");
	  Native.print_int(liveBlockUsage);
	  Native.print_string("\n");
	}

	if (REPORT_LONG_LATENCY && maxObservedLatency>0) {
	  Native.print_string("Maximum observed atomic block in the GC (latency): ");
	  Native.print_long(maxObservedLatency);
	  Native.print_string(" ns, which took place between lines ");
	  Native.print_int(maxObservedLatencyLineFrom);
	  Native.print_string(" and ");
	  Native.print_int(maxObservedLatencyLineTo);
	  Native.print_string("\n");
	}

	if (REPORT_LONG_PAUSE && maxObservedPause>0) {
	  Native.print_string("Maximum observed GC pause: ");
	  Native.print_long(maxObservedPause);
	  Native.print_string(" ns, which took place when GC started around pollcheck at line ");
	  Native.print_int(maxObservedPauseLine);
	  Native.print_string("\n");
	}

	if (PROFILE_ALLOC && profileAlloc) {
	    profileAlloc=false;
	    allocTimestamp.dump();
	    allocSize.dump();
	}
	if ((PROFILE_MEM_USAGE || PROFILE_BLOCK_USAGE)
	    && profileMemUsage) {
	    profileMemUsage=false;
	    memUsageTimestamp.dump();
	    blockUsageValue.dump();
	    memUsageValue.dump();
	    if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) {
	      memSmallObjectFragmentationValue.dump();
	      memSizeClassFragmentationValue.dump();	      
	      memLargeObjectFragmentationValue.dump();
	      memRecentlyDeadFragmentationValue.dump();
	      memLongDeadFragmentationValue.dump();
	      memInReplicasValue.dump();
            }
	}
	if (ALLOC_STATS && allocStats) {
	    Native.print_string("triPizlo: size overflow: ");
	    Native.print_int(sizeOverflow);
	    Native.print_string("\n");
	    Native.print_string("triPizlo: size histogram:\n");
	    for (int i=0;i<sizeHisto.length;++i) {
		Native.print_int(i);
		Native.print_string(" ");
		Native.print_int(sizeHisto[i]);
		Native.print_string("\n");
	    }
	}
	if ( ((SUPPORT_PERIODIC_SCHEDULER && periodicScheduler) || (SUPPORT_HYBRID_SCHEDULER && hybridScheduler)) && 
	    PROFILE_TIMER_INTERRUPTS) {
	    
	  Native.print_string("Recorded gc timer interrupt arrival times: \n");
	  
	  if (recordedArrivalTimes>0) {
  	    Native.print_string("Absolute time of first arrival: ");
  	    Native.print_long(arrivalTimes[0]);
  	    Native.print_string("\n");
  	    Native.print_string("Relative times follow: \n" );
          }
	  
	  for(int i=1;i<recordedArrivalTimes;i++) {
  	    Native.print_string("GC Timer: ");
  	    Native.print_int(i);
            Native.print_string(" ");
            Native.print_long(arrivalTimes[i]-arrivalTimes[i-1]);
            Native.print_string("\n");
          }
          
          Native.print_string("\n");
	}
    }

    public void vmShuttingDown() {
    
        vmIsShuttingDown = true;
        timeProfileHook = false;
        dumpLogs();

	if (VERIFY_HEAP_INTEGRITY) {
	    if (!collecting) {
		verifyHeapIntegrity();
		Native.print_string("triPizlo: VERIFY_HEAP_INTEGRITY: doing a collection just to veriy heap integrity...\n");
		disableConcurrency=true;
		reallyCollect();
		Native.print_string("triPizlo: VERIFY_HEAP_INTEGRITY: collected.\n");
		verifyHeapIntegrity();
	    } else {
		Native.print_string("triPizlo: VERIFY_HEAP_INTEGRITY: not verifying heap integrity because we are in the middle of a collection.\n");
	    }
	}
	if (true) {
	
	    if (PROFILE_GC_PAUSE) {
  	      Native.print_string("triPizlo: total time spent in GC is (very approximate !!)");
	      Native.print_long(totalGCTime);
	      Native.print_string(" ns\n");
            }
	    
	    if (COMPACTION) {
	      Native.print_string("triPizlo: total number of moved objects during compaction: ");
	      Native.print_int(totalMovedObjects);
	      Native.print_string("\n");
	      Native.print_string("triPizlo: total bytes of moved objects during compaction: ");
	      Native.print_int(totalMovedBytes);
	      Native.print_string("\n");	      
            }
	}
    }
    
    public boolean caresAboutAbortOnGcReentry() { return true; }
    public boolean usesPeriodicScheduler() { return SUPPORT_PERIODIC_SCHEDULER && periodicScheduler; }
    public boolean usesAperiodicScheduler() { return SUPPORT_APERIODIC_SCHEDULER && aperiodicScheduler; }    
    public boolean usesHybridScheduler() { return SUPPORT_HYBRID_SCHEDULER && hybridScheduler; }        
    public boolean supportsPeriodicScheduler() { return SUPPORT_PERIODIC_SCHEDULER; }
    public boolean supportsAperiodicScheduler() { return SUPPORT_APERIODIC_SCHEDULER; }
    public boolean supportsHybridScheduler() { return SUPPORT_HYBRID_SCHEDULER; }    
    
    public void setAbortOnGcReentry( boolean abortOnGcReentry ) {
      this.abortOnGcReentry = true;
    }

    public void setPeriodicScheduler( boolean periodicScheduler ) {
    
      if (periodicScheduler) {
        if (!SUPPORT_PERIODIC_SCHEDULER) {
          throw Executive.panic("Periodic scheduler not supported.");
        }
        this.periodicScheduler = true;
        aperiodicScheduler = false;
        hybridScheduler = false;
      } else {
        this.periodicScheduler = false;
      }
    }

    public void setAperiodicScheduler( boolean aperiodicScheduler ) {
    
      if (aperiodicScheduler) {
        if (!SUPPORT_APERIODIC_SCHEDULER) {
          throw Executive.panic("Aperiodic scheduler not supported.");
        }
        this.aperiodicScheduler = true;
        periodicScheduler = false;
        hybridScheduler = false;
      } else {
        this.aperiodicScheduler = false;
      }
    }    

    public void setHybridScheduler( boolean hybridScheduler ) {
    
      if (hybridScheduler) {
        if (!SUPPORT_HYBRID_SCHEDULER) {
          throw Executive.panic("Hybrid scheduler not supported.");
        }
        this.hybridScheduler = true;
        periodicScheduler = false;
        aperiodicScheduler = false;
      } else {
        this.hybridScheduler = false;
      }
    }    
    
    public boolean needsGCThread() { return true; }
    
    static void printStats() {
	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: marked ");
	    Native.print_int(marked);
	    Native.print_string(" objects and waited ");
	    Native.print_int(waited);
	    Native.print_string(" times.\n");
	}
    }
    
    public boolean canCollect() { return threadRunning; }
    
    static Runnable sweepingPoller=new Runnable(){
	    public void run() {
		pollSweeping();
	    }
    };

    static Runnable markingPoller=new Runnable(){
	    public void run() {
		pollMarking();
	    }
    };

    static void zero(VM_Address addr,int nb) {
        if (VERIFY_SWEEP) {
          if (!inHeap(addr) || !inHeap(addr.add(nb))) {
            Native.print_string("triPizlo: ERROR: zeroing memory which is not in heap (using heap zero() call).");
          }
        }
        if (false) { // this is really not needed - zero does not forward as it should not
          addr.add(forwardOffset).setAddress(addr);
        }
	Mem.the().zeroAtomic(addr,nb,sweepingPoller);
    }

    static void zeroBlock(int i) {
	zero(memory.add(i*blockSize),blockSize);
    }

    static void freeBlock(int i) {
    	nUsedBlocks--;
	blockPrev[i]=-1;
	blockNext[i]=blockHead;
	blockPrev[blockHead]=i;
	blockHead=i;
	
	if (SUPPORT_MUTATOR_DISABLE) {
          shouldDisableMutator = (!EFFECTIVE_MEMORY_SIZE && (nUsedBlocks+mutatorDisableThreshold >= nBlocks)) ||
              (EFFECTIVE_MEMORY_SIZE && (nUsedBlocks+mutatorDisableThreshold >= effectiveNBlocks));    
        }
    }
    
    // when updating, no marking is needed
    
    static void walkStacks(int mode, boolean updateSeenPointers, LocalReferenceIterator lri) throws PragmaInline, 
      PragmaNoPollcheck, PragmaNoReadBarriers, PragmaAssertNoSafePoints, PragmaIgnoreSafePoints {
    
//      threadScan.tic();
      
      while (lri.hasNext()) {
        VM_Address ptrPtr = lri.next();

        // FIXME: this needs clean-up - see mark, markAmbiguous, ...
        if (COMPACTION) {
          if (VERIFY_COMPACTION && updateSeenPointers) {
            if (mode != LocalReferenceIterator.PRECISE) {
              throw Executive.panic("walkStacks: pointer update requested for non-precise scan.");  
            }
          }
        }
		       
        if (COMPACTION && updateSeenPointers) {
          updatePointerAtAddress(ptrPtr);
        } else {
        
            // no COMPACTION, precise or conservative mode, no update of seen pointers
            // COMPACTION, precise or conservative mode, no update of seen pointers
            if (mode==LocalReferenceIterator.PRECISE) {
            
              VM_Address ptr = ptrPtr.getAddress();
              updateAndMark(ptr); 
              if (VERIFY_BUG) {
                if (markingBugDetected) {
                  markingBugDetected = false;
                  ptrPtr.setAddress(null);
                  Native.print_string("ERROR: BUG: updating pointer on stack to NULL during stack scanning (the pointer that is forwarded to null)\n");
                  Native.print_string("ERROR: The pointer is stored at address: ");
                  Native.print_ptr(ptrPtr);
                  Native.print_string("ERROR: And the pointer is ");
                  printAddr(ptr);
                  Native.print_string("\n");
                }
              
              }
            } else {
              markAmbiguous(ptrPtr.getAddress());
            }
	}
      }
      
//      threadScan.toc();
    }


    static boolean uniIteratorHas = true;
    static Object uniIteratorObject = null;
    
    static Iterator uniIterator = new Iterator() {

      public boolean hasNext() {
        return uniIteratorHas;
      }
      
      public Object next() {
        uniIteratorHas=false;
        return uniIteratorObject;
      }
      
      public void remove() {
        throw new OVMError.Unimplemented("you've got to be kidding me");
      }
    };

    static void walkStacks(int mode, boolean update) throws PragmaInline, PragmaNoReadBarriers,
      PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaIgnoreSafePoints  {
    
        if (VERIFY_POLLING && !threadManager.isReschedulingEnabled()) {
          Native.print_string("In walkStacks - non-atomic, rescheduling enabled is ");
          Native.print_boolean(threadManager.isReschedulingEnabled());
          Native.print_string("\n");
          throw Executive.panic("fix");          
        }

	LocalReferenceIterator lri=LocalReferenceIterator.the();
	if (INCREMENTAL_STACK_SCAN) {
	    // QUESTION: what if a new thread is created during the
	    // stack scan?
	    // ANSWER: this is no different than a stack growing after
	    // it is scanned.

            if (DEBUG_INTERRUPTIBLE_STACK_SCANNING) {
    	      Native.print_string("triPizlo: walkStacks: INCREMENTAL_STACK_SCAN\n");
            }
	    
	    for (Iterator i=Context.iterator();
		 i.hasNext();) {
		 
		pollStackScanning();

//		prepareScanA.tic();
		uniIteratorObject = i.next();
		uniIteratorHas = true;
//		prepareScanA.toc();
//		prepareScanB.tic();
		lri.walkTheseContexts(uniIterator,mode);
//		prepareScanB.toc();
		walkStacks(mode, update, lri);
	    }
	} else {
	    lri.walkCurrentContexts(mode);
	    walkStacks(mode, update, lri);
	}
    }
        
    void reportFragmentation() {
    
       if (REPORT_FRAGMENTATION && reportFragmentation) {
        
          Native.print_string("Preparing fragmentation report\n");
          
          // internal fragmentation
          
          // (small objects)
          // sizes [ blockIdx ] ... allocation size for this block/page
          // uses [ blockIdx ] ... number of allocation blocks used in 
          //				this block/page
        
          // large blocks (internal) fragmentation
          // 	lbReserved - lbUsed
          int lbReserved = 0;
          int lbUsed = 0;
          
          // small blocks internal object fragmentation
          // 	(space between the object and the unit of allocation size
          //     of the specific allocation size class)
          
          int sbWastedInObjects = 0;
          
          // small blocks internal block fragmentation
          //	(unused allocateable space within the blocks reserved
          //	for a class)
            
          int sbReserved = 0;  // taken for small objects
          int sbAvailable = 0; // allocateable taken space 
          int sbFreeable = 0;  // space in blocks that could be freed by 
                               // defragmentation
          
          for(int blockIdx = Bits.findSet(usedBits, currentNBitWords, 0); 
            blockIdx >= 0; 
            blockIdx = Bits.findSet(usedBits, currentNBitWords, blockIdx)) {
          
            if (ARRAYLETS && Bits.getBit(arrayletBits, blockIdx)) {
              blockIdx++;
              continue;
            }
            
            if (Bits.getBit(largeBits, blockIdx)) {
              // large block
              
              if (sizes[ blockIdx ] == -1) {
                // continuation of a large block
                // should not happen
                Native.print_string("REPORT_FRAGMENTATION: In continuation block\n");
                blockIdx++;
                continue;
              }
              
              lbReserved += sizes [ blockIdx ] * blockSize;
              VM_Address addr = memory.add( blockIdx * blockSize );
              lbUsed += objectSize(addr);
              blockIdx += sizes [ blockIdx ];
            
            } else {
              // small block
              
              int allocSize = sizes [ blockIdx ];
              int nObjs = blockSize/allocSize;

              VM_Address addr = memory.add( blockIdx * blockSize );
              
              for(int j=nObjs; j-- > 0; addr=addr.add(allocSize) ) {
              
                  // the problem with FRESH is that objects with uninitialized blueprints
                  // (bump-pointer allocation space) are also PINNED, thus objectSize
                  // fails on them
                  
//                if (gcData.getColor(addr) != FREE) {
                if (gcData.getColor(addr) == WHITE) {
                  sbWastedInObjects += allocSize - objectSize(addr);
                }
              }
              blockIdx ++;
            }
          }
        
          for(int scIdx = 0; scIdx < sizeClasses.length ; scIdx ++ ) {
          
            SizeClass sc = sizeClasses[scIdx];
            //sc.nObjects, sc.nBlocks
            int scReserved = sc.nBlocks * blockSize;
            int scAvailable = scReserved - (sc.size*sc.nObjects);
            int scFreeable = ( scAvailable / blockSize ) * blockSize ;
            
            sbReserved += scReserved;
            sbAvailable += scAvailable;
            sbFreeable += scFreeable;
          }
        
        // report results
        Native.print_string("Fragmentation summary:\n");
        Native.print_string("\t total reserved: ");
        Native.print_int(lbReserved + sbReserved);
        Native.print_string("\n");
        
        Native.print_string("\t total used: ");
        Native.print_int(lbUsed + sbReserved - sbWastedInObjects - sbAvailable);
        Native.print_string("\n");
        
        Native.print_string("\t wasted in large objects: ");
        Native.print_int(lbReserved - lbUsed);
        Native.print_string("\n");
        
        Native.print_string("\t wasted in small objects - allocation units: ");
        Native.print_int(sbWastedInObjects);
        Native.print_string("\n");
        Native.print_string("\t available in for small objects: ");
        Native.print_int(sbAvailable);
        Native.print_string("\n");        
        Native.print_string("\t freeable by defragmentation: ");
        Native.print_int(sbFreeable);
        Native.print_string("\n");        
      }
    }
    
    static int freeableBlocks( SizeClass sc ) {
        int scAvailable = (sc.nBlocks * (blockSize-sc.padding)) - (sc.size * (sc.nObjects + sc.nFreeObjectsInEvacuatedBlocks) ); 


//        int scFreeable = scAvailable / blockSize  ; this worked when there used to be no padding in block

        int scFreeable = scAvailable / (blockSize-sc.padding);
        
        // scAvailable, and thus scFreeable, could be negative if some of the free objects in evacuated blocks get allocated
        
        if (scFreeable < 0) {
          if (DEBUG_COMPACTION) {
            Native.print_string("\ntriPizlo: COMPACTION: mutator started allocating objects in evacuated blocks.\n\t\t");
          }
          return 0;
        }
        
        int scNonPinned = sc.nBlocks - sc.nBlocksPinned;
        
        if ( scNonPinned < scFreeable ) {
          return scNonPinned;
        } else {
          return scFreeable;
        }
    }
    
    static int freeableBlocks( int scIndex ) {
      return freeableBlocks( sizeClasses[scIndex] );
    }
    
    // only called with COMPACTION true
    
    static VM_Address objectBeingMoved = null;
    
    void moveObject( VM_Address dst, VM_Address src, int size ) throws PragmaNoReadBarriers, PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaIgnoreSafePoints  {

      if (DEBUG_BUG) {

        Blueprint bp = VM_Address.fromObject(src).asOop().getBlueprint();
        
        if (bp.isArray()) {
        
          Blueprint.Array abp = bp.asArray();
          if (abp.getComponentSize()==1) {
          
            int length = abp.getLength( src.asOop() );
            
            Native.print_string("Moving byte array ");
            printAddr(src);
            Native.print_string(" of length ");
            Native.print_int(length);
            Native.print_string(" to ");
            printAddr(dst);
            Native.print_string("\n");            
          }
        }
      }

      boolean arrayWithInlineArraylet = false;
    
      if (!COMPACTION) {
        throw Executive.panic("moveObject called when COMPACTION is false!");
      }
    
      if (VERIFY_COMPACTION) {
        if (!inHeap(dst) || !inHeap(dst.add(size))) {
          Native.print_string("triPizlo: ERROR: moveObject called on destination not in heap.\n");
        }
        if (!inHeap(src) || !inHeap(src.add(size))) {
          Native.print_string("triPizlo: ERROR: moveObject called on source not in heap.\n");
        }

        int sbIdx = blockIdx(src);
                
        if ( !Bits.getBit(usedBits, sbIdx) ) {
          throw Executive.panic("moveObject called on source in unused block.");
        }

        if ( Bits.getBit(largeBits, sbIdx) ) {
          throw Executive.panic("moveObject called on source in large object.");
        }

        int dbIdx = blockIdx(dst);
        
        if ( !Bits.getBit(usedBits, dbIdx) ) {
          throw Executive.panic("moveObject called on source in unused block.");
        }

        if ( Bits.getBit(largeBits, dbIdx) ) {
          throw Executive.panic("moveObject called on destination in large object.");
        }        

        if (sizes[sbIdx] <= 0) {
          throw Executive.panic("moveObject called on source in a block for non-positive size");
        }

        if (sizes[dbIdx] <= 0) {
          throw Executive.panic("moveObject called on source in a block for non-positive size");
        }        
        SizeClass ssc = sizeClassBySize[sizes[sbIdx]/alignment];
        SizeClass dsc = sizeClassBySize[sizes[dbIdx]/alignment];
        
        if (ssc!=dsc) {
          throw Executive.panic("moveObject called on locations not in the same size class.\n");
        }
      }
    
      if (ARRAYLETS) {
        // some arraylets may be within the array
        // as maximum small object size == arraylet size,
        // it can only be one arraylet in the array, in particular the last one
      
        Blueprint bp = src.asOop().getBlueprint();
        if (bp.isArray()) {
        
          if (VERIFY_ARRAYLETS) {
            assert(size <= arrayletSize);
          }
          
          Blueprint.Array abp = bp.asArray();
          int length = abp.getLength(src.asOop());
          int nArraylets = allArrayletsInArray(abp, length);
          
          
          if (nArraylets>0)  { // zero-length arrays have no arraylets

            int last_arraylet = nArraylets - 1;
            
            VM_Address ptr = src.add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD + last_arraylet*MachineSizes.BYTES_IN_ADDRESS);
            VM_Word offset = ptr.getAddress().diff(src);

            if ( offset.uLT(VM_Word.fromInt(size)) ) {
            
              if (REPLICATING && INCREMENTAL_OBJECT_COPY) {
                arrayWithInlineArraylet = true;
              }
              ptr.setAddress( dst.add(offset) );  
            }
          }
        }
      }

      if (REPLICATING && INCREMENTAL_OBJECT_COPY && !arrayWithInlineArraylet) {

        if (VERIFY_COMPACTION) {
          objectBeingMoved = dst;
        }

        int toCopy = size;
        int offset = 0;
        
        Nat.memcpy( dst, src, ObjectModel.getObjectModel().headerSkipBytes() );
        offset += ObjectModel.getObjectModel().headerSkipBytes() ;
        toCopy -= ObjectModel.getObjectModel().headerSkipBytes() ;
        
        src.add(forwardOffset).setAddress( dst );
        
        // now 
        //    src.forward -> dst
        //    dst.forward -> src
        
        gcData.markOld(src);
        
        while (toCopy > maxAtomicBytesCopy) {
        
          pollCompacting();
          
          Nat.memcpy( dst.add(offset), src.add(offset), maxAtomicBytesCopy );
          offset += maxAtomicBytesCopy;
          toCopy -= maxAtomicBytesCopy;
        }
        
        if (toCopy > 0) {
          
          pollCompacting();

          Nat.memcpy( dst.add(offset), src.add(offset), toCopy );
        }
              
      
      } else { // atomic object copy
      
        src.add(forwardOffset).setAddress( dst );
      
        // this has read barriers inside (on the the(), Mem.PollingAware)
        // it also has pollchecks, but it doesn't matter if we are atomic
        // Mem.the().cpy(dst, src, size);

        Nat.memcpy(dst,src,size);
        
        if (REPLICATING) {
          dst.add(forwardOffset).setAddress(src);
          gcData.markOld(src);
        }
      }
      
      if (REPLICATING && INCREMENTAL_OBJECT_COPY) {
        if (VERIFY_COMPACTION) {
          objectBeingMoved = null;
        }
      }
      
      if (false) {
        Native.print_string("Moved ");
        printAddr(src);
        Native.print_string(" to ");
        printAddr(dst);
        Native.print_string("\n");      
      }
      
      totalMovedObjects++;
      totalMovedBytes+=size;
    }
    
    int blocksFreed = 0;
    
    void reallyCollect() throws PragmaNoReadBarriers, PragmaIgnoreExceptions, 
      PragmaAssertNoExceptions, PragmaIgnoreSafePoints, PragmaNoPollcheck {

        if (VERIFY_POLLING && !threadManager.isReschedulingEnabled()) {
          Native.print_string("In reallyCollect, rescheduling enabled is ");
          Native.print_boolean(threadManager.isReschedulingEnabled());
          Native.print_string("\n");
          throw Executive.panic("fix");          
        }

        if (REPORT_FRAGMENTATION && reportFragmentation) {
          Native.print_string("Fragmentation before collection...\n");
          reportFragmentation();
        }
             
        pollStackScanning();
	outer.tic();
	
	if (VERIFY_HEAP_INTEGRITY || VERIFY_BUG) {
	  verifyNoPointerHasColor("at start of a cycle, no objects should be GREYBLACK", GREYBLACK);
	}	
		
	collecting=true;
	allocColor=GREYBLACK;

	marked=0;
	waited=0;
	
	// in the predictable write barrier mode, it is necessary to reset the worklist before commencing GC
	worklistEmpty=true;
	worklistPuti=0;
	worklistGeti=0;

	marking=true; // if we are using the "unpredictable" write barrier, this enables it.

	updatingPointersWrittenToHeapNeeded = true; 
	  // now we need to make sure that dirty pointers from stack don't get into the heap, because we are 
	  // fixing the pointers now when walking objects in the image and on the heap
	  
	  // this flag is only used with incremental object copy & replication
	  // for other modes, the pointers are always being fixed when written to the heap, although it is
	  // not necessary
	  
	  // NOTE:!! this has to be enabled no later than new black objects can be allocated
	  //  otherwise, dirty pointers can be written to these objects and they will never get fixed


	if (VERIFY_COMPACTION) {
  	  verifyForwardingPointers( lastCycleEvacuatedObjects ? FWD_ALWAYS : FWD_NEVER);
        }

	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: collector has awakened.  starting with ");
	    Native.print_int(nUsedBlocks);
	    Native.print_string(" used blocks.\n");
	    
	    Native.print_string("triPizlo: GC: allocColor is now ");
	    Native.print_int(allocColor);
	    Native.print_string(" (");
	    Native.print_string(colorName(allocColor));
	    Native.print_string(")");
	    Native.print_string("\n");
	    
	    if (!PREDICTABLE_BARRIER) {
		Native.print_string("triPizlo: GC: activating write barrier.\n");
	    }
	    
	    Native.print_string("triPizlo: GC: scanning stacks...\n");
	}
	
	
	if (VERIFY_COMPACTION) {
  	  verifyForwardingPointers( lastCycleEvacuatedObjects ? FWD_ALWAYS : FWD_NEVER );
        }
		
	// 1) scan stacks
	// warning: this code allocates memory.
	
	dijkstraBarrierOn = true; // if we are using the incremental stack scanning, and thus Dijsktra's barrier to
	                      // protect values from escaping from stacks before being scanned, this enables 
	                      // the barrier (in unpredictable mode)

	pollStackScanning();
	stackScan.tic();
	
	if (VERIFY_COMPACTION) {
  	  verifyForwardingPointers( lastCycleEvacuatedObjects ? FWD_ALWAYS : FWD_NEVER );
          Native.print_string("scanning conservative roots:\n");
        }

        conservativeScan.tic();
	walkStacks(LocalReferenceIterator.CONSERVATIVE, false); // with pointer stacks, this does nothing
	conservativeScan.toc();
	
	if (VERIFY_COMPACTION) {
	  Native.print_string("conservative roots scanned.\n");	
  	  verifyForwardingPointers( lastCycleEvacuatedObjects ? FWD_ALWAYS : FWD_NEVER );
  	  Native.print_string("scanning precise roots:\n");	
        }
	
	preciseScan.tic();
	walkStacks(LocalReferenceIterator.PRECISE, false );
	preciseScan.toc();
	
	if (VERIFY_COMPACTION) {
	  Native.print_string("precise roots scanned.\n");	
  	  verifyForwardingPointers( lastCycleEvacuatedObjects ? FWD_ALWAYS : FWD_NEVER );
        }
	
        pollStackScanning();
	stackScan.toc("scanned stacks");

	dijkstraBarrierOn = false;
		
	gcStopped=false; // I don't think whis is used anymore

	printStats();

	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: stacks scanned.\n");
	}
		
	if (VERIFY_COMPACTION) {
  	  verifyForwardingPointers( lastCycleEvacuatedObjects ? FWD_ALWAYS : FWD_NEVER );
        }

	// 2) scan image
	// avoid walking the image header


	inner.tic();
	markTimer.tic();

	pollMarking();
		
	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: walking image...\n");
	}
		
	imageScan.tic();
	
	if (FAST_IMAGE_SCAN) {
	    
	    // first non-image-header byte of image 
	    // (first byte of first object header in the image
        
	    /* if scanning the first page, avoid walking image header */
        
	    int idx;
        
	    if (Bits.getBit(dirty, 0)) {
		if (DEBUG_WALK_IMAGE && debugWalkImage) {
		    Native.print_string("triPizlo: WALK_IMAGE: walking from first image page\n");
		}
		idx = walkImageFromAddress( Native.getImageBaseAddress() );
	    } else {
		idx = 1;
	    }
	    pollMarking();
        
	    /* avoid scanning the last page, as image may end within it */ 
        
	    // FIXME: the handling of image ending on page unaligned addres was
	    // not here before the SW barrier ; is it really needed ?
        
/*
  FIXME: this is very slow
	    for(idx = Bits.findSetIncremental(dirty, nImgBitWords, idx, markingPoller); 
		(idx >= 0) && (idx < lastImagePageIdx);
		idx = Bits.findSetIncremental(dirty, nImgBitWords, idx, markingPoller)) {
*/
	    for(idx = Bits.findSet(dirty, nImgBitWords, idx); 
		(idx >= 0) && (idx < lastImagePageIdx);
		idx = Bits.findSet(dirty, nImgBitWords, idx)) {

		pollMarking();

		if (DEBUG_WALK_IMAGE && debugWalkImage) {
		    Native.print_string("triPizlo: WALK_IMAGE: walking from page ");
		    Native.print_int(idx);
		    Native.print_string("\n");
		}
            
		idx = walkImageFromPage( idx );
	    }

	    pollMarking();
	    /* scan the last page, potentially a partial one */         
        
	    if ( idx == lastImagePageIdx ) {
		if (DEBUG_WALK_IMAGE && debugWalkImage) {
		    Native.print_string("triPizlo: WALK_IMAGE: walking last page (");
		    Native.print_int(idx);
		    Native.print_string(")\n");
		}
            
		mc.walk( pageIndexToImageAddress( lastImagePageIdx ), 
			 Native.getImageEndAddress() );
	    }
	    
	    if (VERIFY_FAST_IMAGE_SCAN) {
		fastImageScanVerificationActive=true;
		mc.walk(Native.getImageBaseAddress(),
			Native.getImageEndAddress());
		if (fastImageScanFailed) {
		    throw Executive.panic("image scan verification failed");
		}
		fastImageScanVerificationActive=false;
	    }
	    
	} else {
	    
	    mc.walk(Native.getImageBaseAddress(),
		    Native.getImageEndAddress());
	}

	inner.toc("scanned image");
	imageScan.toc();
	
	heapScan.tic();
	if (VERIFY_COMPACTION) {
  	  verifyForwardingPointers( lastCycleEvacuatedObjects ? FWD_ALWAYS : FWD_NEVER );
        }
	
	printStats();
	
	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: walking heap...\n");
	}

	// 3) walk the heap
	inner.tic();
	int walked=0;
	while (!worklistEmpty) {
	    pollMarking();
	    walkObject(worklistPop()); // forwarding ok, the worklist has new locations of objects
	    walked++;
	}
	inner.toc("walked heap");


	if (VERIFY_MARK && walked!=marked) {
	    throw Executive.panic("Did not walk all of the objects I marked.");
	}

	    
	printStats();

        if (DEBUG_GC && debugGC) {
          Native.print_string("triPizlo: GC: updating monitors.\n");
        } 
        
	if (VERIFY_COMPACTION) {
  	  verifyForwardingPointers(lastCycleEvacuatedObjects ? FWD_ALWAYS : FWD_NEVER);
        }

        heapScan.toc();
        
        monitorScan.tic();
        monitorRegistryAfterWalk(); // after this, we can have black objects unreachable from the roots
        monitorScan.toc();

        pollMarking();
	markTimer.toc("marking done");
	
	marking=false;
		
	if (DEBUG_GC && debugGC) {
	    if (PREDICTABLE_BARRIER) {
		Native.print_string("triPizlo: GC: marking phase complete.\n");
	    } else {
		Native.print_string("triPizlo: GC: marking phase complete.  write barrier (the marking part of it) deactivated.\n");
	    }
	}
	
	if (VERIFY_COMPACTION) {
  	  verifyForwardingPointers(lastCycleEvacuatedObjects ? FWD_ALWAYS : FWD_NEVER);
        }
        
        if (VERIFY_MARK || VERIFY_HEAP_INTEGRITY || VERIFY_BUG) {
	  verifyHeapAfterMarking();
	}

	if (COMPACTION && lastCycleEvacuatedObjects) {
	
	  // note: we don't need Dijkstra barrier now
	  stackUpdateTimer.tic();
	  
	  if (DEBUG_COMPACTION && debugCompaction || DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: COMPACTION: updating pointers on the stack.\n");
	  }
    	  walkStacks(LocalReferenceIterator.PRECISE, true );

    	  pollStackScanning();
    	  stackUpdateTimer.toc();
        }
    	
    	if (VERIFY_BUG || VERIFY_COMPACTION || VERIFY_HEAP_INTEGRITY) {
    	  verifyHeapAfterStackUpdating();
    	}
    	 
    	if (VERIFY_COMPACTION) {
    	  Native.print_string("triPizlo: COMPACTION: checking pointers on stack after stack update - precise\n");
          verifyForwardingPointersInStack(FWD_NEVER, LocalReferenceIterator.PRECISE);
    	  Native.print_string("triPizlo: COMPACTION: checking pointers on stack after stack update - conservative\n");          
          verifyForwardingPointersInStack(FWD_NEVER, LocalReferenceIterator.CONSERVATIVE);
    	  Native.print_string("triPizlo: COMPACTION: done checking pointers on stack after stack update - conservative\n");                    
  	  verifyForwardingPointers(FWD_WHITE_SOURCE);
        }

        updatingPointersWrittenToHeapNeeded = false; // there are no reachable old pointers now

        if (DEBUG_GC && debugGC) {
          Native.print_string("triPizlo: GC: sweeping.\n");
        } 
        
        // FIXME: sweeping itself could be without barriers... 
        // would it really help ?

                
	// 4) do sweeping

	sweepTimer.tic();
	inner.tic();
	blocksFreed=0;
	int objsFreed=0;
/*	
	for (int i=Bits.findSetIncremental(usedBits,currentNBitWords,0,sweepingPoller);
	     i>=0;
	     i=Bits.findSetIncremental(usedBits,currentNBitWords,i,sweepingPoller)) {
*/	     
        for (int i=Bits.findSet(usedBits,currentNBitWords,0);
	     i>=0;
	     i=Bits.findSet(usedBits,currentNBitWords,i)) {
	    if (DEBUG_SWEEP && debugSweep) {
		Native.print_string("triPizlo: SWEEP: sweeping block ");
		Native.print_int(i);
		Native.print_string("\n");
	    }
	    pollSweeping(3);
	    
            if (ARRAYLETS && Bits.getBit(arrayletBits,i)) {
              i++;
            } else if (Bits.getBit(largeBits,i)) {
		if (sizes[i]<0) {
		    if (DEBUG_SWEEP && debugSweep) {
			Native.print_string("triPizlo: SWEEP: stumbled upon the continuation of a large object, stepping forward.\n");
		    }
		    i++;
		} else {
		    if (DEBUG_SWEEP && debugSweep) {
			Native.print_string("triPizlo: SWEEP: got ourselves a large object.\n");
		    }
		    VM_Address addr=memory.add(i*blockSize);
		    int size=sizes[i];
		    int bits=gcData.getColor(addr);
		    if (bits==WHITE) {
			// free it
			if (DEBUG_FREE && debugFree) {
			    Native.print_string("triPizlo: FREE: freeing large object at ");
			    Native.print_ptr(addr);
			    Native.print_string("\n");
			}
			if (ARRAYLETS) { // note - large objects don't move, thus are not forwarded
                          Blueprint bp = addr.asOop().getBlueprint();
                          if (bp.isArray()) {
                            if (DEBUG_SWEEP && debugSweep) {
                              Native.print_string("triPizlo: FREE: removing arraylets of array with large object spine at ");
                              Native.print_ptr(addr);
                              Native.print_string("\n");
                            }
                            freeAndZeroArrayletsOf( addr, size * blockSize );
                          }
                        }
                          
                        if (PROFILE_MEM_USAGE) {
                            int osize = objectSize(addr);
			    memUsage -= osize;
			    if (PROFILE_MEM_FRAGMENTATION) {
			      int psize = contiguousPartObjectSize(addr);
			      memLargeObjectFragmentation -= ( size * blockSize ) - psize;
                            }
			    observeMemUsageChanged();
			}  

			for (int j=size+i;j-->i;) {
			    pollSweeping(4);
			    zeroBlock(j);
			    Bits.clrBit(usedBits,j);
			    Bits.clrBit(largeBits,j);
			    blocksFreed++;
			    freeBlock(j);
			    if (PROFILE_BLOCK_USAGE) {
  			      observeMemUsageChanged();
                            }
                            sizes[j]=0; // not necessary, but maybe good for runtime tests
			}
		    }
		    i+=size;
		}
	    } else {

	        if (VERIFY_SWEEP) {
	          if (uses[i]==0) {
	            throw Executive.panic("Small used block in sweep has 0 uses count.");
	          }
                }
                	        
		if (DEBUG_SWEEP && debugSweep) {
		    Native.print_string("triPizlo: SWEEP: got ourselves a small object block: ");
		    Native.print_ptr(memory.add(i*blockSize));
		    Native.print_string("\n");
		}
		// small object sweep
		int allocSize=sizes[i];
		int nObjs=blockSize/allocSize;
		VM_Address block=memory.add(i*blockSize);
		    
		SizeClass sc=sizeClassBySize[allocSize/alignment];
		
		if (VERIFY_COMPACTION) {
		  verifyFreelistIntegrity(sc);
		}
		
		VM_Address cur=block;
		boolean needsWork = false;
		
		for (int j=nObjs;j-->0;cur=cur.add(allocSize)) {
		    pollSweeping(5);
		    
		    int color = gcData.getColor(cur);
		    
		    if (color==WHITE) {
			uses[i]--;
			sc.nObjects--;
			needsWork = true;
			
//			if ((DEBUG_SWEEP && debugSweep) || DEBUG_BUG) {
			if (DEBUG_SWEEP && debugSweep) {
			  Native.print_string("triPizlo: SWEEP: object ");
			  printAddr(cur);
			  Native.print_string(" with blueprint ");
			  printBlueprint(cur);
			  Native.print_string(" is unreachable and will be swept.\n");
			}
			
			if (DEBUG_BUG && (BROOKS && cur.add(forwardOffset).getAddress() != cur)) {
			  Blueprint bp = VM_Address.fromObjectNB(cur).asOop().getBlueprint();
			  
			  if (bp.isArray()) {
			    Blueprint.Array abp = bp.asArray();
			    
			    if (abp.getComponentSize() == 1) {
  			      int length = abp.getLength( VM_Address.fromObjectNB(cur).asOop() );
			    
  			      Native.print_string("triPizlo: SWEEP: byte array ");
  			      printAddr(cur);
  			      Native.print_string(" of length ");
  			      Native.print_int(length);
  			      Native.print_string(" is unreachable and will be swept.\n");
                            }
                          }
			}			
			
			if (COMPACTION && PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) {
			  if ((REPLICATING && gcData.getOld(cur)!=0) ||
			      (BROOKS && cur.add(forwardOffset).getAddress() != cur )) {

                            memInReplicas -= objectSize(cur);
                          }
			}
			
			if (COMPACTION && REPLICATING && gcData.getOld(cur)!=0) { // could be conditional also on lastCycleEvacuatedObjects
			  // old location of an object ; have to fix forwarding pointer in the new location, so 
			  // that mutator does not double-write to the old location anymore
			  // if the object is dead, mutator won't write to it ; if the object is live, mutator has only pointer to the new location
			  
			  
			  if (VERIFY_COMPACTION) {
			    replicasShouldBeInSync = false;
			    // below we update forwarding pointers so that we loose information about old locations of object
			    // then, it's impossible to check if old copies are outdated only modulo forwarding (which is ok), or
			    // modulo a bug
			  }
			  
			  VM_Address newCopy = cur.add(forwardOffset).getAddress();
                          VM_Address newCopyFwdPtr = newCopy.add(forwardOffset);
                          
                          if (newCopyFwdPtr.getAddress() == cur) {  // I think we could also check that the object is GREYBLACK
                                                                    // and once/if we zero on allocation, we don't need to care anymore
                            newCopyFwdPtr.setAddress(newCopy);
                          }
                          
                          if (VERIFY_COMPACTION && false) { // this is very very very slow
                            checkReferees( cur, REF_WHITESOURCE );
                          }
			} else {
			  if ( ARRAYLETS && ( 
  			      ((COMPACTION && REPLICATING) || (COMPACTION && BROOKS && cur.add(forwardOffset).getAddress() == cur)) || 
  			      !COMPACTION) ) {  // note we cannot do translateNonNullPointer, because its internal checks
  			                        // (non-null pointer forwarded to null error)
                                                // note we know that we have the new location of an object even when REPLICATING
			       
			    // cur is the new copy of the object
			    // we need this to make sure we only remove the external arraylets once and not prematurely
			    // (we remove them with the last replica of the array)
			    Blueprint bp = cur.asOopUnchecked().getBlueprint();
			    if (bp.isArray()) {
                              if (DEBUG_SWEEP && debugSweep) {
                                Native.print_string("triPizlo: FREE: removing arraylets of array with small object spine at ");
                                Native.print_ptr(cur);
                                Native.print_string("\n");
                              }			    
			      freeAndZeroArrayletsOf( cur, allocSize );
                            }
                          } 
			}
		    } else if ( (VERIFY_COMPACTION && ( (REPLICATING && gcData.getOld(cur)!=0) ||
		      (BROOKS && cur.add(forwardOffset).getAddress() != cur && cur.add(forwardOffset).getAddress() != null ))) 
                      && !( PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION && color==FREE) ) {
                        Native.print_string("triPizlo: ERROR: Old object is not white during sweeping.\n");
                        printAddr(cur);
                      throw Executive.panic("Fix.");
                      
                    } else if (color==FREE) {
                      if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION && gcData.getOld(cur)==0 ) {
                        gcData.markOld(cur);
                        memRecentlyDeadFragmentation -= allocSize;
                        memLongDeadFragmentation += allocSize;
                      }
                    }
		}
		
		if (!needsWork) {
		  i++;
		  continue;
		}
		
		
		if (VERIFY_SWEEP && uses[i]<0) {
		    throw Executive.panic("uses went negative");
		}

		boolean wasBumpPointerBlock = false;

		// remove block from nonfull list, if it is there
		// (and kill the free list of the block)
		// this is done also to prevent mutator from using this
		// block

                if (freeHeadsPtrs[i]!= 0) {     // i was a nonfull block

                    if (DEBUG_SWEEP && debugSweep) {
                        Native.print_string("triPizlo: SWEEP: removing freelist of block: ");
                        Native.print_ptr(memory.add(i*blockSize));
                        Native.print_string("\n");
                    }

                    // remove from nonfull blocks, as it is now empty
                    if (nonfullPrev[i] != -1) {
                        nonfullNext[ nonfullPrev[i] ] = nonfullNext[i];
                    }
                        
                    if (nonfullNext[i] != -1) {
                        nonfullPrev[ nonfullNext[i] ] = nonfullPrev[i];
                    }
		        		        
                    // if it is head of a class's nonfull list, remove it
                    if (sc.nonfullHead == i) {
                        sc.nonfullHead = nonfullNext[i];
                    }

                    freeHeadsPtrs[i] = 0;
                    nonfullNext[i] = -1;
                    nonfullPrev[i] = -1;
  
                } else if (sc.block==block) { // i was a bump pointer allocation block
                
		    if (DEBUG_SWEEP && debugSweep) {
			Native.print_string("triPizlo: SWEEP: block is in use for pointer-bump allocation, turning it into a normal block: ");
			Native.print_ptr(memory.add(i*blockSize));
			Native.print_string("\n");
		    }
		    
                    if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) {
                      wasBumpPointerBlock = true;
                    }
                    
		    if (VERIFY_SWEEP && sc.blockCur==null) {
			throw Executive.panic("blockCur is null when block is non-null.");
		    }
		    // turn off pointer-bump for this block
		    VM_Address blockCur=sc.blockCur;
		    sc.block=null;
		    sc.blockCur=null;
		    // and now flip the bits, in an interruptible way
		    
		    if (uses[i]>0) { 
  		      for (cur=blockCur;
			 block.add(blockSize).diff(cur).asInt()
			     >= allocSize;
			 cur=cur.add(allocSize)) {
			pollSweeping(6);  // overkill ?
			
			if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) {
			  memRecentlyDeadFragmentation += allocSize;
			  observeMemUsageChanged();
			}

			if (VERIFY_SWEEP) {
			  verifyZeroed(cur, allocSize);
			}
			cur.setInt(FREE); // we don't need the blueprints +
			                   // they shouldn't be there anyway...
			                   // marking it free has the side effect of
			                   // it not being zeroed again
			
			//gcData.setColor(cur,WHITE); // must not have read barrier 
			                            // hmm, this is unnecessarily slow
                      }
                    }
		}

		int olduses = 0;
		
		if (VERIFY_SWEEP) {
		  olduses = uses[i];
                }
		

                // mutator now cannot use block i

		if (uses[i] == 0) { 
		
		    if (DEBUG_SWEEP && debugSweep) {
		      Native.print_string("triPizlo: SWEEP: zeroing and freeing an unused block: ");
		      Native.print_ptr(memory.add(i*blockSize));
		      Native.print_string("\n");
		    }
		    pollSweeping(7);
                    if (PROFILE_MEM_USAGE) {
                      cur=block;
                      for (int j=nObjs;j-->0;cur=cur.add(allocSize)) { // yes, this sucks
                        int bits=gcData.getColor(cur); 
                        
                        if (bits==WHITE) {
                          int osize = objectSize(cur);
                          memUsage -= osize;
                          if (PROFILE_MEM_FRAGMENTATION) {
                            int psize = contiguousPartObjectSize(cur);
                            memSmallObjectFragmentation -= allocSize - psize;
                          }
                        } else if (PROFILE_MEM_FRAGMENTATION && bits==FREE) {
                          if (gcData.getOld(cur)!=0) {
                            memLongDeadFragmentation -= allocSize;
                          } else {
                            memRecentlyDeadFragmentation -= allocSize;
                          }
                        }
                      }
                    }

                    sc.nBlocks--;
                    sizes[i] = 0; // not necessary, but reveals a lot of forgotton things...
                    
                    zeroBlock(i);
                    Bits.clrBit(usedBits,i);
                    freeBlock(i);
                    blocksFreed++;
                    
                    if (PROFILE_MEM_USAGE || PROFILE_BLOCK_USAGE) {
                      
                      if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) {
                        memSizeClassFragmentation -= sc.padding;
                      }
                      observeMemUsageChanged(); 
                    }
			
                    i++;
                    
                    if (VERIFY_COMPACTION) {
                      verifyFreelistIntegrity(sc); //!!! not i-1, because i-1 is not on the size class anymore
                    }

                    if (VERIFY_SWEEP) {
                      if (uses[i-1]!=olduses) {
                        Native.print_string("ERROR: Mutator allocated from a block when we assumed it was impossible (1).\n");
                      }
                    }
                    continue;
		}
		
                
		// ((uses[i]>0) && (uses[i]<nObjs))
		
		if (VERIFY_SWEEP && uses[i] == nObjs) { 
		  throw Executive.panic("Uses count error detected during sweep.");
		}
		
                if (DEBUG_SWEEP && debugSweep) {
                    Native.print_string("triPizlo: SWEEP: adding free objects in block to the block's freelist: ");
                    Native.print_ptr(memory.add(i*blockSize));
                    Native.print_string("\n");
                    
                    Native.print_string("triPizlo: SWEEP: free list head is currently at ");
                    Native.print_int(freeHeadsPtrs[i]);
                    Native.print_string("\n");
                }
		    
                // we have to rebuild the free-list of this block and
                // we want to put block to nonfull list if it is not there		    
                // but only when we have at least one free object on it
		
		if (VERIFY_SWEEP) {
		  if (freeHeadsPtrs[i]!=0) {
		    Native.print_string("triPizlo: ERROR: block has nonempty freelist when it should have an empty one (none)\n");
		  }
		}
		    
                cur=block;
                for (int j=nObjs;j-->0;cur=cur.add(allocSize)) {
                    pollSweeping(8);
                    int bits=gcData.getColor(cur); // must not have read barrier
                    if (DEBUG_SWEEP && debugSweep) {
		        Native.print_string("triPizlo: SWEEP: object at ");
                        Native.print_ptr(cur);
                        Native.print_string(" has bits ");
                        Native.print_int(bits);
                        Native.print_string("=");
                        Native.print_string(colorName(bits));
                        Native.print_string("\n");
                    }
                    
                    if (bits==WHITE) {
                        if ((DEBUG_FREE && debugFree) || (DEBUG_SWEEP && debugSweep)) {
                            Native.print_string("triPizlo: FREE/SWEEP: freeing object ");
                            Native.print_ptr(cur);
                            Native.print_string("\n");
                        }

                        if (PROFILE_MEM_USAGE) {
                          int osize = objectSize(cur);
                          
                          memUsage -= osize;
                          if (PROFILE_MEM_USAGE && PROFILE_MEM_FRAGMENTATION) {
                            memRecentlyDeadFragmentation += allocSize;
                            int psize = contiguousPartObjectSize(cur);
                            memSmallObjectFragmentation -= allocSize - psize;
                          }
                          observeMemUsageChanged();
                        }
                        
                        VM_Address custom = null;
                        
                        if (KEEP_OLD_BLUEPRINT) {
                          custom = cur.getAddress();
                          freeList.setCustom( cur, cur.getAddress() );
                        }
                        
                        zero(cur,allocSize);
                        // gcData.setColor(cur,FREE); // must not have read barrier
                        cur.setInt(FREE); // FIXME: make a destructive setColor call ?
		    
                        freeList.setPrev(cur,null);
                        freeList.setNext(cur,VM_Address.fromInt(freeHeadsPtrs[i]));
                        
                        if (KEEP_OLD_BLUEPRINT) {
                          freeList.setCustom( cur, custom );                        
                        }
                        
                        if (freeHeadsPtrs[i]!=0) {
                            freeList.setPrev(VM_Address.fromInt(freeHeadsPtrs[i]),cur);
                        } 
                        freeHeadsPtrs[i]=cur.asInt();
                        objsFreed++;
                        
                    } else if (bits==FREE) {
                        if (DEBUG_SWEEP && debugSweep) {
                            Native.print_string("triPizlo: SWEEP: adding already free object to new freelist ");
                            Native.print_ptr(cur);
                            Native.print_string("\n");
                        }


                        freeList.setPrev(cur,null);
                        freeList.setNext(cur,VM_Address.fromInt(freeHeadsPtrs[i]));
                        if (freeHeadsPtrs[i]!=0) {
                            freeList.setPrev(VM_Address.fromInt(freeHeadsPtrs[i]),cur);
                        } 

                        freeHeadsPtrs[i]=cur.asInt();                    
                        
                    }
                }
		
                // putting it back to non-full list

                if (VERIFY_SWEEP) {
                  if (uses[i]!=olduses) {
                    Native.print_string("ERROR: Mutator allocated from a block when we assumed it was impossible (2).\n");
                  }
                }
                if (DEBUG_SWEEP && debugSweep) {
                    Native.print_string("triPizlo: SWEEP: a block became nonfull, adding to nonfull list: ");
                    Native.print_ptr(memory.add(i*blockSize));
                    Native.print_string("\n");
                }

                nonfullNext[i] = sc.nonfullHead;
                nonfullPrev[i] = -1;
                                
                if (sc.nonfullHead!=-1) {
                    nonfullPrev[ sc.nonfullHead ] = i;
                }
                sc.nonfullHead = i;	
		i++;
                if (VERIFY_COMPACTION) {
                  verifyFreelistIntegrity(sc); 
                }
                if (VERIFY_SWEEP) {
                  if (uses[i-1]!=olduses) {
                    Native.print_string("ERROR: Mutator allocated from a block when we assumed it was impossible (3).\n");
                  }
                }                
	    }
	}
	inner.toc("swept");
	pollSweeping(9);
	
	if (VERIFY_BUG) {
  	  finishedSweeps++;
        }
	sweepTimer.toc();
	
        if (MAX_LIVE_MEM_PROFILE && PROFILE_MEM_USAGE) {
          if (memUsage > liveMemUsage) {
            liveMemUsage = memUsage;
          }
        }
        
        if (MAX_LIVE_MEM_PROFILE && PROFILE_BLOCK_USAGE) {
          if (nUsedBlocks > liveBlockUsage) {
            liveBlockUsage = nUsedBlocks;
          }
          
          if ( (disableConcurrency || abortOnGcReentry ) && 
            ( (!EFFECTIVE_MEMORY_SIZE && (nUsedBlocks >= nBlocks - gcThreshold)) ||
              (EFFECTIVE_MEMORY_SIZE && (nUsedBlocks >= effectiveNBlocks - gcThreshold)) ) ) {
            
              Native.print_string("triPizlo: ERROR gc threshold is too high (GC would start immediatelly after sweep and abortOnGcReentry is true)\n"); 
              throw outOfMemory();
          }
          
	}

	collecting=false;
	lastCycleEvacuatedObjects= false;
	
	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: waited ");
	    Native.print_int(waited);
	    Native.print_string(" times.\n");
		
	    Native.print_string("triPizlo: GC: freed ");
	    Native.print_int(blocksFreed);
	    Native.print_string(" blocks and ");
	    Native.print_int(objsFreed);
	    Native.print_string(" objects.  there are now ");
	    Native.print_int(nUsedBlocks);
	    Native.print_string(" blocks used.");
	    if (COMPACTION) {
	      Native.print_string(" Until now, ");
	      Native.print_int(totalMovedObjects);
	      Native.print_string(" objects were moved (");
	      Native.print_int(totalMovedBytes);
	      Native.print_string(" bytes)");
	    }
	    Native.print_string("\n");
	}


	if (VERIFY_SWEEP || VERIFY_COMPACTION || VERIFY_BUG) {
	  Native.print_string("Verifying heap after sweeping...\n");
//	  verifyHeapAfterStackUpdating(); // this only checks the reachable pointers
	  verifyHeapAfterSweeping();
	}  
	
	
	if (VERIFY_HEAP_INTEGRITY || VERIFY_BUG) {
	  Native.print_string("Verifying that heap has no white objects after sweeping...\n");
	  verifyNoPointerHasColor("after sweep, no objects should be WHITE", WHITE);
	}
	
	// there are no objects in the heap that are white at this moment.
	// we swap the meaning of GREYBLACK and WHITE so that all GREYBLACK
	// objects become WHITE.
	swapColors();
	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: GREYBLACK is now ");
	    Native.print_int(GREYBLACK);
	    Native.print_string(" and WHITE is now ");
	    Native.print_int(WHITE);
	    Native.print_string("\n");
	}
	
	// this does nothing, but I leave it in to illustrate that the
	// allocColor is now WHITE.
	allocColor=WHITE;
	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: allocColor is now ");
	    Native.print_int(allocColor);
            Native.print_string(" (");
	    Native.print_string(colorName(allocColor));
	    Native.print_string(")");
	    Native.print_string("\n");
	}
	
	if (VERIFY_COMPACTION) {
          replicasShouldBeInSync = true;	
	}
	
	
	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: sweeping done.  collector going to compact.\n");
	}

	outer.toc("collected");
	outer.tic();



        if (VERIFY_COMPACTION || VERIFY_BUG) {
          Native.print_string("triPizlo: COMPACTION: verifying pointers after sweep.\n");
          verifyForwardingPointers(FWD_NO_OLD_COPIES);
          Native.print_string("\n");
        }

    	if (VERIFY_COMPACTION || VERIFY_BUG) { // this can detect that stacks were not scanned properly
    	                         // swept reachable object => forwarding to null
    	  Native.print_string("triPizlo: COMPACTION: checking pointers on stack after sweep - precise\n");
          verifyForwardingPointersInStack(FWD_NEVER, LocalReferenceIterator.PRECISE);
    	  Native.print_string("triPizlo: COMPACTION: checking pointers on stack after sweep - conservative\n");          
          verifyForwardingPointersInStack(FWD_NEVER, LocalReferenceIterator.CONSERVATIVE);
    	  Native.print_string("triPizlo: COMPACTION: done checking pointers on stack after sweep - conservative\n");                    
  	  verifyForwardingPointers(FWD_NO_OLD_COPIES);
        }

	if (COMPACTION) {
	
	  int blocksToEvacuate = 0;
	  
	  if (EFFECTIVE_MEMORY_SIZE) {
	    blocksToEvacuate = nUsedBlocks+compactionThreshold - effectiveNBlocks;
	  } else {
	    blocksToEvacuate = nUsedBlocks+compactionThreshold - nBlocks;
	  } 
	  
	  if (blocksToEvacuate > 0) { // some compaction needed 
	  
          pollCompacting();
          compactionTimer.tic();
          
	  if (DEBUG_COMPACTION && debugCompaction) {
	    Native.print_string("triPizlo: GC: not enough free pages, need to do some compaction.\n");
	  }
	  
	  int evacuatedBlocks = 0; 
	      // number of blocks evacuated so far
	      // will become free after next sweep

          for( int scIdx = 0; scIdx < sizeClasses.length ; scIdx ++ ) {
            sizeClasses[scIdx].nFreeObjectsInEvacuatedBlocks = 0;
            sizeClasses[scIdx].compacted = false;
//            pollCompacting(); // overkill ?
          }

//          while (evacuatedBlocks < compactionThreshold) {
//            while (nUsedBlocks+compactionThreshold >= nBlocks) {

          pollCompacting();
          
          
//          while (evacuatedBlocks < (compactionThreshold - gcThreshold)) { 
            while (blocksToEvacuate > evacuatedBlocks) {
            // stops automatically when no more opportunities for compaction exist

            if (DEBUG_COMPACTION && debugCompaction) {
              Native.print_string("triPizlo: COMPACTION: so far evacuated ");
              Native.print_int(evacuatedBlocks);
              Native.print_string(" pages, looking for a class to compact.\n");
             }

            // find a size class with most freeable blocks
              
            int bestIdx = 0;
            int bestFreeable = 0;
              
            for(int scIdx = 0; scIdx<sizeClasses.length ; scIdx++ ) {
   
              pollCompacting(); // overkill ?
              
              int scFreeable =  freeableBlocks(scIdx);
                
              if (false && DEBUG_COMPACTION && debugCompaction) {
                Native.print_string("triPizlo: COMPACTION: class size for size ");
                Native.print_int(sizeClasses[scIdx].size);
                Native.print_string(" has ");
                Native.print_int(scFreeable);
                Native.print_string(" freeable blocks.\n");
              }

              if ((scFreeable > bestFreeable) && !sizeClasses[scIdx].compacted) {
                bestFreeable = scFreeable;
                bestIdx = scIdx;
              }
            } 
	  
            SizeClass bestSC = sizeClasses[bestIdx];
            bestSC.compacted = true;                    
                      
            if (bestFreeable==0) {
              if (DEBUG_COMPACTION && debugCompaction) {
                Native.print_string("triPizlo: COMPACTION: no more pages can be evacuated.\n");
              }
              break;	  
            }

            if (DEBUG_COMPACTION && debugCompaction) {
              Native.print_string("triPizlo: COMPACTION: will compact class ");
              Native.print_int(bestIdx);
              Native.print_string(" for size ");
              Native.print_int(bestSC.size);
              Native.print_string(" with ");
              Native.print_int(bestFreeable);
              Native.print_string(" freeable blocks.\n");
            }
                  
            if (DEBUG_COMPACTION && debugCompaction) {
              memoryInfo();
            }

            // this reduces pause times, but complicates worst case analysis
            // the mutator can now fill up the size class
          
            pollCompacting();
          
            int scFreeable =  freeableBlocks(bestSC); // note this is inaccurate due to pinning
          
            if (scFreeable < 1) {
              if (DEBUG_COMPACTION && debugCompaction) {
                Native.print_string("triPizlo: COMPACTION: mutator took all space intended for compaction, bailing out.\n");
              }
              continue;
            }
            
            // sort the nonfull list of the class by decreasing occupancy, pinned blocks first
            // and occupancy does not matter for pinned
          
            // FIXME: can be done more efficiently (or probably more efficiently)
            // the prev pointers can be set already for the buckets, and then the whole
            // lists for each bucket concatenated
            // (this however preserves ordering and is probably more predictable)
          
            sortTimer.tic();
          
            for(int o = 0; o<=maxOccupancy; o++) {
              nonfullHeads[o] = -1;
              nonfullTails[o] = -1;
//              pollCompacting(); // overkill ?
            }
          
            pollCompacting();
            
            int bIdx = bestSC.nonfullHead;
            
            //if (VERIFY_COMPACTION) {
            //  bestSC.nonfullHead = -1; // just for sanity checks
            //}
            // WARNING: Initially, sort was uninterruptible by mutator
            //  but it turned out to take too long (max around 3ms)
            //  so it is now interruptible - expect bugs
            
            if (VERIFY_COMPACTION && bIdx==-1) {
              throw Executive.panic("Nonfull list empty before sort.\n");
            }
            
            if (DEBUG_COMPACTION && debugCompaction) {
              Native.print_string("Sorting non-full list, phase 1...\n");
            }
            
            while (bIdx!=-1) {
              int o;
              
              if (nPinnedObjects[bIdx]>0) {
                o = maxOccupancy;
              } else {
                o = uses[bIdx];
              }
              
              if (nonfullHeads[o] == -1) {
                nonfullHeads[o] = bIdx; 
                nonfullTails[o] = bIdx; 
                nonfullPrev[bIdx] = -1; 
              } else {
                int tail = nonfullTails[ o ];
                if (VERIFY_COMPACTION && tail==-1) {
                  throw Executive.panic("Tail undefined in nonfull list sort.\n");
                }
                nonfullNext[ tail ] = bIdx;
                nonfullTails[ o ] = bIdx;
                nonfullPrev[ bIdx ] = tail;
              }
              
              int next = nonfullNext[bIdx];              
              nonfullNext[bIdx] = -1;

              
              bIdx = next;
              bestSC.nonfullHead = next;

              if (next == -1) {
                break;
              } else {
                nonfullPrev[next] = -1;
                pollCompacting();
                bIdx = bestSC.nonfullHead; // this is needed, because the block may well be full by now
              }
            }
            
            int head = -1; // this will become new bestSC.nonfullHead
            int tail = -1;

            // now bestSC.nonfullHead == -1
            
            if (DEBUG_COMPACTION && debugCompaction) {
              Native.print_string("Sorting non-full list, phase 2...\n");
            }

            for(int o = maxOccupancy; o>0; o--) {
              pollCompacting();
              bIdx = nonfullHeads[o];  

              if (bIdx==-1) {
                continue;
              }
              
              if (head == -1) {
                head = bIdx;
              } else {
                nonfullNext[ tail ] = bIdx;
                nonfullPrev[ bIdx ] = tail;
              }

              tail = nonfullTails[o];
            }
    
            // put the currently allocated space at the end - many
            // new objects die, and we have to put it somewhere, anyway
            
            if (bestSC.nonfullHead != -1) {
              nonfullNext [ tail ] = bestSC.nonfullHead;
              nonfullPrev [ bestSC.nonfullHead ] = tail;
            }
              
            bestSC.nonfullHead = head;

            if (DEBUG_COMPACTION && debugCompaction) {
              Native.print_string("Done sorting non-full list.\n");
            }

            if (VERIFY_COMPACTION) {
              verifyFreelistIntegrity(bestSC);
            }
            
            if (DEBUG_COMPACTION && debugCompaction && false) {
              Native.print_string("Dumping sorted freelist (uses): \n");
              int h = bestSC.nonfullHead;
              
              while(h != -1) {
                Native.print_int(uses[h]);
                Native.print_string(", ");
                h = nonfullNext[h];
              }
                          
              Native.print_string("Verifying sorted freelist integrity...\n");
              verifyFreelistIntegrity(bestSC);
            }
          
 
            if (VERIFY_COMPACTION) {
               assert(tail!=-1);
            }
          
            sortTimer.toc();
            pollCompacting();

            int lastFreeable = scFreeable;
            
            scFreeable =  freeableBlocks(bestSC);
          
            if (scFreeable < 1) {
              if (DEBUG_COMPACTION && debugCompaction) {
                Native.print_string("triPizlo: COMPACTION: mutator took all space intended for compaction after sorting, bailing out.\n");
              }
              continue;
            }

            int objectsInBlock = blockSize/bestSC.size;
	    VM_Address addr = memory.add( tail * blockSize );
	    int tailPrev = nonfullPrev[tail];
	    int objIndex = 0;
	    
            // nonfull list is now sorted, start compacting
            
            while( (tailPrev == nonfullPrev[tail]) && (tailPrev != -1) && (evacuatedBlocks<blocksToEvacuate) ) {
            
              // move next object
    
              // FIXME: we need to rewrite this, so that getMemSmall keeps the freelist sorted
              // this way, we might bail out prematurely          
              // FIXME: this breaks the "nFreeObjectsInEvacuatedBlocks"
             
              if (nPinnedObjects[tail]>0) {
                if (DEBUG_COMPACTION && debugCompaction) {
                  Native.print_string("triPizlo: COMPACTION: cannot attempt to evacuate next block, because it has pinned objects, bailing out from class.\n");
                }
                break;
              }
              
              int color = gcData.getColor(addr);
              if (color==WHITE) {
              
                if (VERIFY_COMPACTION) {
                  verifyForwardingPointer("compaction",addr, FWD_NO_OLD_COPIES);
                }
                
                // FIXME: can this ever happen ?
                
                if ( (REPLICATING && gcData.getOld(addr)!=0 ) ||
                     (BROOKS && translateNonNullPointer(addr) != addr) ) {
                     
                  if (DEBUG_COMPACTION && debugCompaction) {
                    Native.print_string("triPizlo: COMPACTION: ERROR: got to an object that is already moved. Bailing out from class.\n");
                  }
                  break;
                }

                if (VERIFY_ARRAYLETS) {
                  if (Bits.getBit(arrayletBits, tail)) {
                    Native.print_string("triPizlo: ERROR: Compaction code got to arraylet block.\n");
                    throw Executive.panic("error");
                  }
                }

                
                VM_Address newAddr = getMemSmall(bestSC, bestSC.size); // - for performance, we allocate more than the old copy has
                                                               // (it's padding anyway, but note that it must be handled in memory usage profiling)
                          
                gcData.setColor(newAddr ,allocColor);
                  // this is probably not necessary, but keeps the colors more consistent
                  
                
                boolean preventObjectMove = false;
                  
                if (DONT_MOVE_BYTE_ARRAYS) {
                  Blueprint bp = VM_Address.fromObject(addr).asOop().getBlueprint();
        
                  if (bp.isArray()) {
                    if (bp.asArray().getComponentSize()==1) {
                      preventObjectMove = true; 
                    }
                  }
                }
                
                if (!DONT_MOVE_BYTE_ARRAYS || !preventObjectMove) {
//                  copyTimer.tic();
                  moveObject(newAddr, addr, bestSC.size); // moving the padding doesn't matter... note that objectSize is not always the right amount for here,
                                                        // as it can be the reference size

//                  copyTimer.toc();
                  lastCycleEvacuatedObjects = true;
                
                  if (PROFILE_MEM_USAGE) {
                    int osize = objectSize(addr);
                    memUsage += osize;
                  
                    if (PROFILE_MEM_FRAGMENTATION) {
                      memInReplicas += osize;
                      // we know here that we have a small object
                      int psize = contiguousPartObjectSize(addr);
                      memSmallObjectFragmentation -= psize;
                    }
                    observeMemUsageChanged();
                  }
                
                  if (VERIFY_COMPACTION) {
                    verifyForwardingPointer("compaction2",addr, FWD_ALWAYS);
                  }
                }

                //pollCompacting();
              } else if (color==FREE) {
                bestSC.nFreeObjectsInEvacuatedBlocks++;
              }
              
              pollCompacting();
              objIndex++;
              
              if ((objIndex==objectsInBlock) || (color==FRESH) || (nPinnedObjects[objIndex]>0)) {
              
                if (objIndex==objectsInBlock) {
                  evacuatedBlocks ++;
                  
                 // NO!!!  nonfullNext[tail] = -1;
                 // although removing these from freelist has some justification, doing it this way breaks
                 // many invariants about free objects
                 
                 } else {
                  if (DEBUG_COMPACTION && debugCompaction) {
                  
                    // FIXME: this breaks the "nFreeObjectsInEvacuatedBlocks"
                    if (color==FRESH) {
                      Native.print_string("triPizlo: COMPACTION: bailing out from a block with FRESH object, cannot evacuate.\n");
                    } else {
                      Native.print_string("triPizlo: COMPACTION: bailing out from a block with pinned object, cannot evacuate.\n");
                    }
                  }
                }
                
                objIndex = 0;
                if ((nonfullPrev[tail] == tailPrev) && (freeableBlocks(bestSC) > 0)) {
                  tail = tailPrev;
                  tailPrev = nonfullPrev[tail];
                  addr = memory.add( tail * blockSize );
                } else {
                  if (DEBUG_COMPACTION && debugCompaction) {
                    Native.print_string("triPizlo: COMPACTION: mutator got to compaction, cannot compact more in this class.\n");
                  }
                  break ;
                }
                // evacuate next block
                continue;
              }
              
              int capacity = (bestSC.nBlocks -1) * objectsInBlock - (bestSC.nObjects - uses[tail] + bestSC.nFreeObjectsInEvacuatedBlocks);
              
              if (capacity < uses[tail]) {
                if (DEBUG_COMPACTION && debugCompaction) {
                  Native.print_string("triPizlo: COMPACTION: no more compaction opportunities in class: not space for objects in the page considered for evacuation.\n");
                }
                break;
              }
              
              addr = addr.add( bestSC.size );        
            }
          }
        
          pollCompacting();
          compactionTimer.toc();
          
          if (VERIFY_BUG) {
            finishedCompactions++;
          }
          
          if (DEBUG_COMPACTION && debugCompaction) {
            Native.print_string("triPizlo: GC: evacuated ");
            Native.print_int(evacuatedBlocks);
            Native.print_string(" pages.\n");
          }
        
          //lastCycleEvacuatedObjects= evacuatedBlocks > 0;
          
          if (VERIFY_COMPACTION) {
	    verifyForwardingPointers(FWD_ALWAYS);
	  }

	}} // some compaction needed

	if (DEBUG_GC && debugGC) {
	    Native.print_string("triPizlo: GC: compaction done.  collector going to sleep.\n");
	}

	outer.toc("compacted");
	totalGCTime+=outer.lat-outer.lost;
	
	gcStopped=true;
	
        if (REPORT_FRAGMENTATION && reportFragmentation) {
          Native.print_string("Fragmentation after collection...\n");
          reportFragmentation();
        }
    }
    
    static void swapColors() {
	int tmp=GREYBLACK;
	GREYBLACK=WHITE;
	WHITE=tmp;
    }
    
    private static void verifyFreeObject(VM_Address cur) {
	if (gcData.getColor(cur)!=FREE) {
	    Native.print_string("ERROR: object in freelist is not free.\n");
	}
	if (!Bits.getBit(usedBits,blockIdx(cur))) {
	    Native.print_string("ERROR: block holding object in freelist is not in use.\n");
	}
	if (Bits.getBit(largeBits,blockIdx(cur))) {
	    Native.print_string("ERROR: block holding object in freelist is a large object block.\n");
	}
    }
    
    private static void verifyFreelistIntegrity(int bIdx) throws PragmaAtomicNoYield {
    
      enterExcludedBlock();
      
      try {
      
      int nfree = 0;
      
      VM_Address addr = memory.add(bIdx*blockSize);
        
      int size = sizes[bIdx];
      
      if (size==0) {
        Native.print_string("ERROR: verifyFreelistIntegrity called on a block belonging to no size class.\n");
        return;
      }
      int nObjs = blockSize/size;
      
      for(int i=0;i<nObjs; i++) {
        int color = gcData.getColor(addr);
        if (color == FREE) {
          nfree++;
        }
        addr = addr.add(size);
      }

      int ncfree = 0;
        
      addr = VM_Address.fromInt(freeHeadsPtrs[bIdx]);
        
      while(addr != null) {
        ncfree ++;
        int color = gcData.getColor(addr);
        if (color != FREE) {
          Native.print_string("ERROR: non-FREE object on freelist: ");
          printAddr(addr);
          Native.print_string("\n");
        }
        addr = freeList.getNext(addr);
      }
        
      if (ncfree != nfree) {
        Native.print_string("ERROR: numbers of free objects in block and its freelist do not match: ");
        Native.print_int(bIdx);
        Native.print_string("( in block:");
        Native.print_int(nfree);
        Native.print_string(", in freelist:");
        Native.print_int(ncfree);
        Native.print_string(" )\n");
      }
      
      if (DEBUG_SWEEP && debugSweep) {
        Native.print_string("triPizlo: SWEEP: block has ");
        Native.print_int(nfree);
        Native.print_string(" free objects, its freelist has ");
        Native.print_int(ncfree);
        Native.print_string(" objects\n");
      }
      } finally {
        leaveExcludedBlock();
      }
    }
    
    private static void verifyFreelistIntegrity(SizeClass sc) throws PragmaAtomicNoYield {

        if (false) {    
          int fscanned = countSCFreeObjects(sc,true);
          int fcounted = countSCFreeObjects(sc,false);
        
          if (fscanned != fcounted) {
            Native.print_string("ERROR: counting and scanning free objects in a size class give different results: scanning=");
            Native.print_int(fscanned);
            Native.print_string(", counting=");
            Native.print_int(fcounted);
            Native.print_string("\n");
          }
        }
   
        if (sc.nonfullHead != -1) {
          if (nonfullPrev[sc.nonfullHead]!=-1) {
              Native.print_string("ERROR: first block in a nonfull list has non -1 prev pointer.\n");
          }
        }

        int lastbIndex = -1;
        for( int bIndex = sc.nonfullHead;
          bIndex != -1;
          bIndex = nonfullNext[ bIndex ]
        ) {

            if (lastbIndex != -1) {
              if ( nonfullPrev[bIndex]!=lastbIndex ) {
                Native.print_string("ERROR: a (non-head) object in nonfull list has incorrect prev pointer.\n");
              }
            }

            if (sizes[bIndex]!=sc.size) {
              Native.print_string("ERROR: a nonfull block's size does not match it's size classes size. Either should not be on the list, or it's size is wrong: ");
              Native.print_int(bIndex);
              Native.print_string("\n");
            }

	    VM_Address cur = VM_Address.fromInt(freeHeadsPtrs[bIndex]);
	    
	    if (cur==null) {
	      Native.print_string("ERROR: a block in nonfull list does not have a free list: ");
	      Native.print_int(bIndex);
	      Native.print_string("\n");
	    }
	    
	    if (freeList.getPrev(cur)!=null) {
		Native.print_string("ERROR: first object in freelist has non-null prev pointer.\n");
	    }
	    
	    for (;;) {
		verifyFreeObject(cur);
		VM_Address next=freeList.getNext(cur);
		if (next==null) {
		    break;
		}
		if (cur!=freeList.getPrev(next)) {
		    Native.print_string("ERROR: object in freelist has incorrect prev pointer.\n");
		}
		cur=next;
	    }
	    
	    lastbIndex = bIndex;
	}
    }
    
    private static void verifyPointerBumpIntegrity(SizeClass sc) throws PragmaAtomicNoYield {
	if (sc.block!=null) {
	    Native.print_string("Doing pointer bump allocation in block ");
	    Native.print_ptr(sc.block);
	    Native.print_string(" (");
	    Native.print_int(blockIdx(sc.block));
	    Native.print_string(") for size class ");
	    Native.print_int(sc.size);
	    Native.print_string("\n");
	    for (VM_Address cur=sc.block;
		 cur.uLT(sc.blockCur);
		 cur=cur.add(sc.size)) {
		if (gcData.getColor(cur)==FREE) {
		    Native.print_string("ERROR: object allocated using pointer-bump GC is free.\n");
		}
	    }
	    for (VM_Address cur=sc.blockCur;
		 cur.uLE(sc.block.add(blockSize).sub(sc.size));
		 cur=cur.add(sc.size)) {
		if (gcData.getColor(cur)!=0) {
		    Native.print_string("ERROR: free object in pointer-bump allocated block has non-zero GC bits.\n");
		}
	    }
	}
    }
    
    private static void verifyFreelistIntegrity() throws PragmaAtomicNoYield {
	for (int sci=0;sci<sizeClasses.length;++sci) {
	    verifyPointerBumpIntegrity(sizeClasses[sci]);
	    verifyFreelistIntegrity(sizeClasses[sci]);
	}
    }
    
    
    public void checkAccess(VM_Address ptr) {
      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(ptr);
      }
    }
    
    private static void verifyMemoryAccess(VM_Address ptr) throws PragmaAtomicNoYield {
      
      if (inHeap(ptr)) {
        int blockIdx = blockIdx(ptr);

        if (!Bits.getBit(usedBits, blockIdx)) {
          throw Executive.panic("memory access to unused block (page)");
        }
        
        if (Bits.getBit(arrayletBits, blockIdx) || Bits.getBit(largeBits, blockIdx)) {
          return ;
        }
        
        // small block

        VM_Address blockBase=memory.add(blockIdx*blockSize);
        int size=sizes[blockIdx];
        int offset=ptr.diff(blockBase).asInt();
        VM_Address obj = blockBase.add((offset/size)*size);
        
        int color = gcData.getColor(obj);
        
        if (color == FREE) {
          throw Executive.panic("memory access to a free small object");
        }
      }
    }
    
    static private final int FWD_WHITE_SOURCE = 1;  // Brooks: only white source can be forwarded
                                                    // Replicating: only white object can be marked old 
    static private final int FWD_NEVER = 2;  // Brooks: pointer must be unforwarded (forwarded to itself)
                                             // Replicating: pointer must not be old, but can point to it's old location
    static private final int FWD_ALWAYS = 0; // Brooks: pointers can be forwarded
                                             // Replicating: Any object can be marked old
    static private final int FWD_NO_OLD_COPIES = 3; // there can be no old copy of an object in the heap
                                                    // Brooks: checking as FWD_NEVER
                                                    // Replicating: all objects have to point to themselves

//    private static void verifyForwardingPointer(VM_Address src, int forwardingOK) throws PragmaNoReadBarriers, PragmaAtomicNoYield {

    private static void verifyForwardingPointer(VM_Address src, int forwardingOK) throws PragmaNoReadBarriers, PragmaAtomicNoYield {
      verifyForwardingPointer("(no context)", src, forwardingOK);
    }

    private static void verifyForwardingPointer(String contextMsg, VM_Address src, int forwardingOK) throws PragmaNoReadBarriers, PragmaAtomicNoYield {    
    
      verifyPointer(contextMsg, src, false);

      if (nativeImageBaseAddress == null) {
        // the boot is not yet finished, cannot verify yet
        return;
      }
      
      if (src == null) {
        return ;
      }
      
      if ( false && (!inHeap(src)) && (!inImage(src)) ) {
        // this does not necessarily mean an error, some objects are allocated outside of heap and image
        // for instance objects allocated using Native.getmem (malloc or mmap), such as dirty or worklist,
        // we can verify their forwarding pointers, yes, but if it is really a garbled pointer, we will crash, and
        // if we are optimized, the debug info gets so garbled that it's not possible to find out what went wrong..
        
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");
        Native.print_string("triPizlo: verifyForwardingPointer: WARNING: pointer is neither in image, nor in heap:");
        Native.print_ptr(src);
        Native.print_string(" : ");
        printAddr(src);
        Native.print_string("\n"); 
      }
      
      int scolor =  gcData.getColor(src); 

      if (scolor==FREE) {
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");      
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: mutator has pointer to a free object (found in verifyForwardingPointer):");
        printAddr(src);
        Native.print_string("\n");
        throw Executive.panic("this should not happen.\n");
      }
      
      Blueprint sbp = src.asOopUnchecked().getBlueprint();
      if ((sbp!=null) && (!inImage(VM_Address.fromObjectNB(sbp)))) {
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");      
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: pointer source blueprint is not in image: ");
        printAddr(VM_Address.fromObjectNB(sbp));
        Native.print_string("(");
        printAddr(src);
        Native.print_string(")\n");
         throw Executive.panic("this should not happen.\n");
      }
       
      VM_Address dst = src.add(forwardOffset).getAddress();  // translatePointer call would cause infinite recursion    
      if (dst==null) {
      
        if (COMPACTION && REPLICATING && INCREMENTAL_OBJECT_COPY && objectBeingMoved==src) {
          return ; 
        }
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");      
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: non-null pointer forwarded to null: ");
        printAddr(src);
        Native.print_string(" (");

        Native.print_string(colorName(scolor));
        Native.print_string(")");
        Native.print_string("\n");
        throw Executive.panic("this should not happen.\n");
      }
      
      if (COMPACTION && REPLICATING && INCREMENTAL_OBJECT_COPY && objectBeingMoved==dst) {
        return ; 
      }
      
      int dcolor = gcData.getColor(dst);
      
      if ( (src!=dst) && (dcolor==FREE) ) {
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: pointer forwarded to a FREE object" );
        printAddr(src);
        Native.print_string("trying to find referees:" );
        findReferees(src);
        Native.print_string("\n");
        throw Executive.panic("fix..");
      }
      
      Blueprint dbp = dst.asOopUnchecked().getBlueprint();
      if ( (src!=dst) && (!inImage(VM_Address.fromObjectNB(dbp)))) {
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: pointer destination blueprint is not in image: ");
        printAddr(VM_Address.fromObjectNB(dbp));
        Native.print_string("( the destination with this blueprint is ");
        printAddr(dst);
        Native.print_string("and the source is ");
        printAddr(src);
        Native.print_string(")\n");
      }

      if (sbp!=dbp) {
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: blueprints of old and new objects do not match.\n");
      }
      
      if (src!=dst) {
        verifyPointer(contextMsg, dst, true);      
      }
      
      if (src!=null && src != dst) {
        if (sbp.isArray()) {
            int l1 = src.add( ObjectModel.getObjectModel().headerSkipBytes() ).getInt();
            int l2 = dst.add( ObjectModel.getObjectModel().headerSkipBytes() ).getInt();
            
            if (l1!=l2) {
              Native.print_string("ERROR: old and new copy of an array do not have the same array length, one is ");
              Native.print_int(l1);
              Native.print_string(", the other is ");
              Native.print_int(l2);
              Native.print_string("the array is ");
              printAddr(src);
              Native.print_string("\n");
              throw Executive.panic("fix");
            }
        }
      }
      
      if (REPLICATING) {
        if ( gcData.getOld(src)!=0 && forwardingOK==FWD_NEVER ) {
          Native.print_string("triPizlo: verifyForwardingPointer: ");
          Native.print_string(contextMsg);
          Native.print_string("\n");
          Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: mutator has a pointer to old object when it should not have.\n ");  
          throw Executive.panic("this should not happen.\n");
        }
        
        if (gcData.getOld(dst)!=0) {
        
          if (dst==src) {
            Native.print_string("triPizlo: verifyForwardingPointer: ");
            Native.print_string(contextMsg);
            Native.print_string("\n");
            Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: the only copy of an object is marked old.");
            throw Executive.panic("this should not happen.\n");
          }

          VM_Address tmp = src;
          int tmpc = scolor;
          src = dst;
          scolor = dcolor;
          
          dst=tmp;
          dcolor=tmpc;
        }
        
        if ( gcData.getOld(dst)!=0 ) {
          Native.print_string("triPizlo: verifyForwardingPointer: ");
          Native.print_string(contextMsg);
          Native.print_string("\n");        
          Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: both copies of an object are marked old.\n");
          throw Executive.panic("this should not happen.\n");
        }
        
        if ( src!=dst && gcData.getOld(src)==0 ) {
          Native.print_string("triPizlo: verifyForwardingPointer: ");
          Native.print_string(contextMsg);
          Native.print_string("\n");        
          Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: neither of two copies of an object is marked old.\n");
          throw Executive.panic("this should not happen.\n");
        }
        
        if ( src!=dst && forwardingOK==FWD_NO_OLD_COPIES ) {
          Native.print_string("triPizlo: verifyForwardingPointer: ");
          Native.print_string(contextMsg);
          Native.print_string("\n");        
          Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: there is an old copy of an object in the system when it should not be.\n");
          throw Executive.panic("this should not happen.\n");          
        }
      }
      
      // now if replicating
      //	either the object has only one copy
      //	or src is it's old location and dst it's new location
      
      if ( (src!=dst) && (
          ( forwardingOK==FWD_NEVER && BROOKS )  || 
          ( forwardingOK==FWD_NO_OLD_COPIES ) ||  
          ( forwardingOK==FWD_WHITE_SOURCE  && scolor!=WHITE ))  ) {
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: there is a forwarded object when it should not be: ");
        printAddr(src);
        Native.print_string(" => ");
        printAddr(dst);
        Native.print_string("  ");
        Native.print_string(colorName(scolor));
        Native.print_string(" => ");
        Native.print_string(colorName(dcolor));
        Native.print_string(" object blueprint is ");
        printBlueprint(src);
        Native.print_string("\n");
        throw Executive.panic("This should not happen.\n");
      }

      if ((scolor==FRESH) && (src!=dst)) {         
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");      
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: found a forwarded FRESH object at ");
        printAddr(src);
        Native.print_string("\n");
      }
        
      if ( (src!=dst) && (scolor!=WHITE) ) {
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: original location of a moved object is not WHITE: ");
        printAddr(src);
        Native.print_string(" (");
        Native.print_string(colorName(scolor));
        Native.print_string(")\n");
      } 
        
      if ( (src!=dst) && (dcolor!=WHITE) && (dcolor!=GREYBLACK)) {
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: new location of a moved object is neither WHITE, nor GREYBLACK: ");
        printAddr(dst);
        Native.print_string(" (");
        Native.print_string(colorName(dcolor));
        Native.print_string(")\n");
      }

      boolean inHeapSrc = inHeap(src);
        
      if ((src!=dst) && (!inHeapSrc)) {
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: forwarded object is not in heap: ");
        printAddr(src);
        Native.print_string("\n");
      }
        
      boolean inHeapDst = inHeap(dst);
        
      if ((src!=dst) && (!inHeapDst)) {
        Native.print_string("triPizlo: verifyForwardingPointer: ");
        Native.print_string(contextMsg);
        Native.print_string("\n");
        Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: target of forwarded object is not in heap: ");
        printAddr(dst);
        Native.print_string("\n");
      }
        
      VM_Address dst2 = dst.add(forwardOffset).getAddress(); // translatePointer call would cause infinite recursion
       
      if (REPLICATING) {
        if (dst2!=src) {
          Native.print_string("triPizlo: verifyForwardingPointer: ");
          Native.print_string(contextMsg);
          Native.print_string("\n");
          Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: the new copy of an object does not point to the old one (or the object does not point to itself).\n");
          throw Executive.panic("This should not happen.\n");
        }
      }
      
      if (BROOKS) {
        if ( (dst2!=dst) && (src!=dst) ) {
          Native.print_string("triPizlo: verifyForwardingPointer: ");
          Native.print_string(contextMsg);
          Native.print_string("\n");
          Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: target of forwarded object is also forwarded: ");
          printAddr(src);
          Native.print_string(" => ");
          printAddr(dst);
          Native.print_string(" => ");
          printAddr(dst2);
          Native.print_string("\n");
        }
      }
      
      // check that the primary copy and the replica agree
      // !!!
      if (VERIFY_REPLICAS_SYNC && REPLICATING && (src!=dst) && replicasShouldBeInSync ) {
      
        int sbIdx=blockIdx(src);
        int dbIdx=blockIdx(dst);
        int ssize=sizes[sbIdx];
        int dsize=sizes[dbIdx];
        
        if (Bits.getBit(largeBits,sbIdx) || Bits.getBit(largeBits,dbIdx)) {
          Native.print_string("triPizlo: verifyForwardingPointer: ");
          Native.print_string(contextMsg);
          Native.print_string("\n");
          Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: moved object is a large object.\n");
          throw Executive.panic("Fix..");
        }
        
        if (ssize!=dsize) {
          Native.print_string("triPizlo: verifyForwardingPointer: ");
          Native.print_string(contextMsg);
          Native.print_string("\n");
          Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: sizes of original and new object do not match.\n");
          throw Executive.panic("Fix..");          
        }

        int[] offset = sbp.getRefMap();
        boolean isReferenceArray = false;
        
        
        if (sbp.isArray()) {
          Blueprint.Array abp = sbp.asArray();
          if (abp.getComponentBlueprint().isReference()) {
            isReferenceArray = true;
          }
        }

        for(int i=1;i<ssize/4.;i++) {
          int s = src.add(i*4).getInt();
          int d = dst.add(i*4).getInt();
          
          if (i==2) continue; // forwarding pointer
          if (s!=d) {
            // note that during scanning, we are only updating references in non-old copies of an object
            // this means that when doing this comparison, we may have to do forwarding
            
            boolean isReference = false;
            
            if (offset != null ) {
              for (int j=0;j<offset.length;j++) {
                if (offset[j]==i*4) {
                  isReference = true;
                  break ;
                }
              }
            }    
            
            if (isReference || i==3 || isReferenceArray) { // 3 is scope pointer 
              if (comparePointers(VM_Address.fromInt(s).asOopUnchecked(), VM_Address.fromInt(d).asOopUnchecked(),0,0)) {
                continue;
              }
            }
            Native.print_string("triPizlo: verifyForwardingPointer: ");
            Native.print_string(contextMsg);
            Native.print_string("\n");          
            Native.print_string("triPizlo: VERIFY_COMPACTION: ERROR: old and new copy of an object are not in sync.\n");
            Native.print_string("at offset ");
            Native.print_int(i);
            Native.print_string(" words \n");
            throw Executive.panic("Fix..");            
          }
        }
      }
    }
    
    private static void verifyForwardingPointersInStack(int forwardingOK, int mode) throws PragmaNoReadBarriers, PragmaAtomicNoYield {

        enterExcludedBlock();
        
        try {

        // either there is too many errors, or this just produces too many false
        // alarms to be really usable
        
        // FIXME: this is probably wrong, the iterator is not initialized, because
        // prepareForGCHook wasn't called on it
        // the fastest solution might be to create a subclass of the conservative iterator,
        // which would not run the real GC, but would initialize correctly and would
        // also provide the activations

        LocalReferenceIterator lri=LocalReferenceIterator.the();
	
        lri.walkCurrentContexts(mode);
        while (lri.hasNext()) {
            VM_Address ptrPtr = lri.next();
            VM_Address oldPtr = VM_Address.fromObjectNB(ptrPtr.getAddress());
            
            if (!inHeap(oldPtr)) {
              continue;
            }
            
            // FIXME: why the aligning is not handled by the iterator ?
            int ia = oldPtr.asInt();
            if ( (ia/alignment)*alignment != ia ) {
              continue;
            }
            
            verifyForwardingPointer(oldPtr, forwardingOK);

            /*            
            if ( oldPtr.getInt() == 0 ) {
              continue;
            }
            
            if (oldPtr.add(forwardOffset).getAddress()==null) {
              continue;
            }
            */
	}
	
        } finally {
          leaveExcludedBlock();
        }	
      
    }
    
    
    private static int addressToObjectIndex( VM_Address addr ) throws PragmaInline {
      
      int byteOffset = offset(addr);
      return byteOffset / minSize ;
    }
    
    private static void clearObjectBits( int[] bits ) {
      for(int i=0; i<bits.length; i++) {
        bits[i]=0;
      }
    }
    
    private static boolean getObjectBit( int[] bits, VM_Address addr ) {
      return Bits.getBit( bits, addressToObjectIndex(addr) );
    }
    
    private static void setObjectBit( int[] bits, VM_Address addr ) {
      Bits.setBit( bits, addressToObjectIndex(addr) );
    }


    private static AfterSweepingPointerWalker afterSweepingPointerWalker = new AfterSweepingPointerWalker();

    private static class AfterSweepingPointerWalker extends PointerWalker {

      public boolean checkPointer(VM_Address addr) {
      
        int color = gcData.getColor(addr);
        if (color==WHITE) {
          Native.print_string("ERROR: Found a WHITE object after sweep: ");
          printAddr(addr);
          Native.print_string("\n");
          
          return true;
        }
        
        if (color == FREE) {
          Native.print_string("ERROR: Found a FREE object after sweep where it should not be: ");
          printAddr(addr);
          Native.print_string("\n");
          
          return true;
        }
        
        VM_Address target = updateNonNullPointer(addr);
        if (addr!=target) {
          Native.print_string("ERROR: Found a forwarded object after sweep: ");
          printAddr(addr);
          Native.print_string("\n");
          
          return true;
        }
        
        return false;
      }

      public boolean walkObjectPointer(VM_Address parent, VM_Address addr) {
       
        return checkPointer(parent) || checkPointer(addr);
      }
      
      public boolean walkStackPointer(VM_Address addr) {
        return checkPointer(addr);
      }
      
      public boolean walkImagePointer(VM_Address addr) {
        if (checkPointer(addr)) {
          return true;
        }
        
        walkObject(addr);
        return false;
      }
      
      public boolean walkHeapPointer(VM_Address addr) {
      
        int color = gcData.getColor(addr);
        if (color==WHITE) {
          Native.print_string("ERROR: Found a WHITE object in heap after sweep: ");
          printAddr(addr);
          Native.print_string("\n");
          
          return true;
        }
        
        walkObject(addr);
        return false;
      }
    
    }

    // this is still called with dirty stack
    
    private static AfterStackUpdatingPointerWalker afterStackUpdatingPointerWalker = new AfterStackUpdatingPointerWalker();
    
    private static class AfterStackUpdatingPointerWalker extends AfterMarkingPointerWalker {
    
          public boolean walkStackPointer(VM_Address addr) {
            int color = gcData.getColor(addr);
            
            if (color == WHITE || color == FREE) {
              if (color == WHITE) {
                Native.print_string("ERROR: there is a WHITE pointer on the stack  ");
              } else {
                Native.print_string("ERROR: there is a FREE pointer on the stack  ");
              }
              
              printAddr(addr);
              Native.print_string(" of type ");
              printBlueprint(addr);
              Native.print_string("\n");
              return true;
            }
              
            walkObjectIfNew( addr ); 
            return false;
          }
    };
    
    
    private static AfterMarkingPointerWalker afterMarkingPointerWalker = new AfterMarkingPointerWalker();

    private static class AfterMarkingPointerWalker extends PointerWalker {

          protected void walkObjectIfNew( VM_Address addr ) {
          
            if (!inHeap(addr)) {
              return ;
            }
            
            if (getObjectBit( checkedBits, addr )) {
              return;
            }
            
            setObjectBit( checkedBits, addr );
            walkObject(addr);
          }

          public boolean walkImagePointer(VM_Address addr) {
          
            VM_Address target = updateNonNullPointer(addr);
            if (target!=addr) {
              Native.print_string("ERROR: there is a forwarded object in the image: ");
              printAddr(addr);
              Native.print_string(" of type ");
              printBlueprint(addr);
              Native.print_string("\n");
              return true;
            }
            
            int color = gcData.getColor(addr);
            if (color == WHITE) {
              Native.print_string("ERROR: there is a WHITE object in the image: ");
              printAddr(addr);
              Native.print_string(" of type ");
              printBlueprint(addr);
              Native.print_string("\n");
              return true;
            }
            
            walkObject(addr); 
            return false;
          }
          
          public boolean walkStackPointer(VM_Address addr) {
            int color = gcData.getColor(addr);
            VM_Address target = updateNonNullPointer(addr);
            
            if (color == WHITE) {
              int tcolor = gcData.getColor(target);
              if (tcolor != GREYBLACK) {
                Native.print_string("ERROR: there is a WHITE pointer on the stack that is forwarded to non-GREYBLACK object: ");
                printAddr(addr);
                Native.print_string(" of type ");
                printBlueprint(addr);
                Native.print_string("\n");
                return true;
              }
              
              if (!lastCycleEvacuatedObjects) {
                Native.print_string("ERROR: there is a forwarded object on the stack, although last cycle did not evacuate any objects ");
                printAddr(addr);
                Native.print_string(" of type ");
                printBlueprint(addr);
                Native.print_string("\n");
                return true;
              }
              
              walkObjectIfNew( target ); 
            } else {
            
              if (target != addr) {
                Native.print_string("ERROR: found non-WHITE forwarded pointer on the stack: ");
                printAddr(addr);
                Native.print_string(" of type ");
                printBlueprint(addr);
                Native.print_string("\n");
                return true;
              }
              walkObjectIfNew( addr );              
            }
            
            return false;
          }
          
          public boolean walkObjectPointer(VM_Address parent, VM_Address addr) {
            int pcolor = gcData.getColor(parent);
            int ocolor = gcData.getColor(addr);
            
            if (ocolor == WHITE || ocolor == FREE) {
              if (ocolor == WHITE) {
                Native.print_string("ERROR: found a reachable WHITE object ");
              } else {
                Native.print_string("ERROR: found a reachable FREE object ");
              }
              printAddr(addr);
              Native.print_string(" of type ");
              printBlueprint(addr);
              Native.print_string("\n");
              return true;
            } 
            
            VM_Address target = updateNonNullPointer(addr);
              
            if (target != addr) {
              Native.print_string("ERROR: found a reachable forwarded pointer in the heap ");
              printAddr(addr);
              Native.print_string(" to object of type ");
              printBlueprint(addr);
              Native.print_string("\n");
              return true;
            }
            
            walkObjectIfNew(addr);
            return false;
          }
          
          public void walkRoots() throws PragmaAtomicNoYield {
            enterExcludedBlock();
            
            try {
              clearObjectBits( checkedBits );
              super.walkRoots();
            } finally {
              leaveExcludedBlock();
            }
          }
    }

    private static void verifyHeapAfterSweeping() throws PragmaAtomicNoYield {
    
      enterExcludedBlock();
      
      try {
        afterSweepingPointerWalker.walk();
        
      } finally {
        leaveExcludedBlock();
      }
    }

    private static void verifyHeapAfterMarking() throws PragmaAtomicNoYield {
    
      enterExcludedBlock();
      
      try {
        afterMarkingPointerWalker.walkRoots();
        
      } finally {
        leaveExcludedBlock();
      }
    }

    // this is already called with (supposedly) clean stack
    
    private static void verifyHeapAfterStackUpdating() throws PragmaAtomicNoYield {
    
      enterExcludedBlock();
      
      try {

        afterStackUpdatingPointerWalker.walkRoots();
        
      } finally {
        leaveExcludedBlock();
      }
    }
    
    private static class PointerWalker {
      
      public boolean walkObjectPointer(VM_Address parent, VM_Address addr) {
        return false;
      }
      
      public boolean walkStackPointer(VM_Address addr) {
        return false;
      }
      
      public boolean walkImagePointer(VM_Address addr) {
        return false;
      }
      
      public boolean walkHeapPointer(VM_Address addr) {
        return false;
      }
      
      public void walkStack(int mode) throws PragmaAtomicNoYield, PragmaNoReadBarriers {
        
        enterExcludedBlock();
        
        try {

          LocalReferenceIterator lri=LocalReferenceIterator.the();
          lri.walkCurrentContexts(mode);
        
          while (lri.hasNext()) {
            VM_Address ptrPtr = lri.next();
            VM_Address oldPtr = VM_Address.fromObjectNB(ptrPtr.getAddress());
            
            if (!inHeap(oldPtr)) {
              continue;
            }
            
            // FIXME: why the aligning is not handled by the iterator ?
            int ia = oldPtr.asInt();
            if ( (ia/alignment)*alignment != ia ) {
              continue;
            }
            
            if (oldPtr==null) {
              continue;
            }
            
            if (walkStackPointer(oldPtr)) {
              Native.print_string("WALK: pointer was found on the stack at address ");
              Native.print_ptr(ptrPtr);
              Native.print_string("\n");
            }

            //walkObject(oldPtr);
            
            /*            
            if ( oldPtr.getInt() == 0 ) {
              continue;
            }
            
            if (oldPtr.add(forwardOffset).getAddress()==null) {
              continue;
            }
            */
          }
        } finally {
          leaveExcludedBlock();
        }
      }
	
	
      public void walkImage() throws PragmaNoReadBarriers, PragmaAtomicNoYield {

        enterExcludedBlock();
        
        try {
          ImageScanner imageWalker = new ImageScanner() {
            protected void walk (Oop oop, Blueprint bp) {
            
              VM_Address addr = VM_Address.fromObjectNB(oop);
              if (walkImagePointer(addr)) {
                Native.print_string("WALK: pointer (object) found in image\n");
              }
              //walkObject(addr);
            }
          };
      
          imageWalker.walk(Native.getImageBaseAddress(), Native.getImageEndAddress());
        } finally {
          leaveExcludedBlock();
        }
      }
      
      public void walkHeap() throws PragmaNoReadBarriers, PragmaAtomicNoYield {
      
        enterExcludedBlock();
        
        try {

          for (int cur=0;cur<=highestUsedBlock;) {
            if (Bits.getBit(usedBits,cur)) {
              if (ARRAYLETS && Bits.getBit(arrayletBits, cur)) {
                cur++;
                continue;
              } else if (Bits.getBit(largeBits,cur)) {
                int size=sizes[cur];
                
                if (size==-1) {
                  throw Executive.panic("Error in walking large objects.");
                }
                
                VM_Address ptr=memory.add(blockSize*cur);
                if (walkHeapPointer(ptr)) {
                  Native.print_string("WALK: pointer (object) found is a large object spanning ");
                  Native.print_int(size);
                  Native.print_string(" blocks.\n");
                }
                //walkObject( ptr );
                
                cur += size;
                continue;
              } else {
              
                int allocSize=sizes[cur];
                int nObjs=blockSize/allocSize;
                
                VM_Address ptr=memory.add(blockSize*cur);
                SizeClass sc=sizeClassBySize[allocSize/alignment];

                for (int i=nObjs;i-->0;ptr=ptr.add(allocSize)) {
                  if (walkHeapPointer( ptr )) {
                    Native.print_string("WALK: pointer (object) found is a small object of allocation size ");
                    Native.print_int(allocSize);
                    Native.print_string(" in block of index ");
                    Native.print_int(cur);
                    Native.print_string("\n");
                  }
                  //walkObject( ptr );
                }
              }
            }
            cur++;
          }
        
        } finally {
          leaveExcludedBlock();
        }
      
      }

      public void walkObject(VM_Address addr) throws PragmaNoReadBarriers, PragmaAtomicNoYield {

          int color = gcData.getColor(addr);
          if (color!=WHITE && color!=GREYBLACK) {
            return;
          }

          Oop oop = addr.asOopUnchecked();
          Blueprint bp = oop.getBlueprint();
        
          for (int i = 0;
	     i < ObjectModel.getObjectModel().maxReferences();
	     i++) {
	    
	    VM_Address a = VM_Address.fromObjectNB(oop.getReference(i));
	    if (a!=null && walkObjectPointer( addr, a)) {
	      Native.print_string("WALK: pointer was found in header of object ");
	      printAddr(addr);
	      Native.print_string(" as a header reference of index ");
	      Native.print_int(i);
	      Native.print_string("\n");
	      throw Executive.panic("fix");
	    }
          }

          if (bp.isArray()) {

	    Blueprint.Array abp = bp.asArray();
	    if (abp.getComponentBlueprint().isReference()) {
	        int length = abp.getLength(oop);
	        for(int i=0; i<length; i++) {
  		  VM_Address p = addressOfArrayElement(oop, i);
  		  VM_Address a = p.getAddress();
                  if (a!=null && walkObjectPointer(addr, a)) {
                    Native.print_string("WALK: pointer was found in array ");
                    printAddr(addr);
                    Native.print_string(" of blueprint ");
                    printBlueprint(abp);
                    Native.print_string(" at index ");
                    Native.print_int(i);
                    Native.print_string("\n");
                  }
		}
	    }
          } else {
	    VM_Address base = addr;
	    int[] offset = bp.getRefMap();
	    for (int i = 0; i < offset.length; i++) {
		VM_Address ptr=base.add(offset[i]);
		VM_Address a = ptr.getAddress();
		
                if (a!=null && walkObjectPointer(addr, a)) {
                  Native.print_string("WALK: pointer was found in object ");
                  printAddr(addr);
                  Native.print_string(" of blueprint ");
                  printBlueprint(bp);
                  Native.print_string(" as reference field of index ");
                  Native.print_int(i);
                  Native.print_string(" at byte offset ");
                  Native.print_int( offset[i] );
                  Native.print_string("\n");
                }
	    }
          }
      }
      
      public void walk() throws PragmaNoReadBarriers, PragmaAtomicNoYield {
        enterExcludedBlock();
        
        try {
        
          walkHeap();
          walkImage();
          walkStack(LocalReferenceIterator.PRECISE);

        } finally {
          leaveExcludedBlock();
        }
      }     

      public void walkRoots() throws PragmaNoReadBarriers, PragmaAtomicNoYield {
        enterExcludedBlock();
        
        try {
        
          walkImage();
          walkStack(LocalReferenceIterator.PRECISE);

        } finally {
          leaveExcludedBlock();
        }
      }           
    };


    /* verify that forwarding pointers are not null &
       that there is at most one forwarding for an object &
       that there is no forwarding after sweep (forwardingOK false)
    */
    private static void verifyForwardingPointers(int forwardingOK) throws PragmaNoReadBarriers, PragmaAtomicNoYield {
    
      enterExcludedBlock();
      
      try {
    
      Native.print_string("Verifying heap...\n");
      for (int cur=0;cur<nBlocks;) {
        if (Bits.getBit(usedBits,cur)) {
          if (ARRAYLETS && Bits.getBit(arrayletBits, cur)) {
            cur++;
          } else if (Bits.getBit(largeBits,cur)) {
            cur+=sizes[cur];
          } else {
            int allocSize=sizes[cur];
            int nObjs=blockSize/allocSize;
            VM_Address ptr=memory.add(blockSize*cur);
            SizeClass sc=sizeClassBySize[allocSize/alignment];

            for (int i=nObjs;i-->0;ptr=ptr.add(allocSize)) {
              int color = gcData.getColor(ptr);
              
              if ( (color==WHITE) || (color==GREYBLACK)) {
                verifyForwardingPointer(ptr, forwardingOK);
                verifyForwardingPointersInObject( ptr.asOopUnchecked(), ptr.asOopUnchecked().getBlueprint(), forwardingOK);
              }
            }
            cur++;
          }
        } else {
          cur++;
        }
      }    
      
      final int fOK = forwardingOK;
      
      ImageScanner forwarder = new ImageScanner() {
        protected void walk (Oop oop, Blueprint bp) {
          verifyForwardingPointersInObject(oop, bp, fOK);
        }
      };
      
      Native.print_string("Verifying image...\n");
      forwarder.walk(Native.getImageBaseAddress(), Native.getImageEndAddress());

      if (false) { // this is wrong also because the rules for clean stack are different from
                   // those of the heap (stack can be dirty more often)
        Native.print_string("-------------BEGIN-CONSERVATIVE-WALK--------------\n");      
        verifyForwardingPointersInStack(forwardingOK, LocalReferenceIterator.PRECISE);
        verifyForwardingPointersInStack(forwardingOK, LocalReferenceIterator.CONSERVATIVE);
        Native.print_string("-------------END-CONSERVATIVE-WALK--------------\n");      
      }
      
      Native.print_string("Verifying stack...\n");      
      if (true) {
        verifyForwardingPointersInStack(FWD_ALWAYS, LocalReferenceIterator.PRECISE);
      }
     
      } finally {
        leaveExcludedBlock();
      }
    }
    
    // FIXME: refactor these checkk, find, ... functions using a visitor pattern

    private static final int REF_NONE = 0;
    private static final int REF_WHITESOURCE = 1;
    
    private static void checkReferees(VM_Address target, int refereesOK)  throws PragmaAtomicNoYield {

      for (int cur=0;cur<nBlocks;) {
        if (Bits.getBit(usedBits,cur)) {
          if (ARRAYLETS && Bits.getBit(arrayletBits, cur)) {
            cur ++;
          } else if (Bits.getBit(largeBits,cur)) {
            cur+=sizes[cur];
          } else {
            int allocSize=sizes[cur];
            int nObjs=blockSize/allocSize;
            VM_Address ptr=memory.add(blockSize*cur);
            SizeClass sc=sizeClassBySize[allocSize/alignment];

            for (int i=nObjs;i-->0;ptr=ptr.add(allocSize)) {
              int color = gcData.getColor(ptr);
              
              
              if ( (color==GREYBLACK && refereesOK==REF_WHITESOURCE) || ((color==GREYBLACK || color==WHITE) && refereesOK==REF_NONE) ) {
                if (findRefereesInObject( ptr.asOopUnchecked(), ptr.asOopUnchecked().getBlueprint(), target)) {
                  Native.print_string("triPizlo: ERROR: In heap, forgotten referee to object ");
                  printAddr(target);
                  Native.print_string("\n");
                }
              }
            }
            cur++;
          }
        } else {
          cur++;
        }
      }
      
      final VM_Address tgt = target;
      final int refOK = refereesOK;
      
      ImageScanner finder = new ImageScanner() {
        protected void walk (Oop oop, Blueprint bp) {
          if (refOK==REF_WHITESOURCE || refOK==REF_NONE) {
            if (findRefereesInObject(oop, bp, tgt)) {
              Native.print_string("triPizlo: ERROR: In image, forgotten referee to object ");
              printAddr(tgt);
              Native.print_string("\n");              
            }
          }
        }
      };
      
      finder.walk(Native.getImageBaseAddress(), Native.getImageEndAddress());
    }
    

    private static void verifyImageIntegrity() {
    
      ImageScanner verifier = new ImageScanner() {
        protected void walk (Oop oop, Blueprint bp) {
          Native.print_string("\nI at ");
          Native.print_hex_int(VM_Address.fromObjectNB(oop).asInt());
          Native.print_string(" bp ");
          byte[] str=bp.get_dbg_string();
          Native.print_bytearr_len(str,str.length);
          Native.print_string("\n");
        }
      };

      verifier.walk(Native.getImageBaseAddress(), Native.getImageEndAddress());
    }

    private static void	findArrayletPointerInObject( VM_Address ptr, VM_Address target ) throws PragmaNoPollcheck {
  
      Oop obj = ptr.asOopUnchecked();
      Blueprint bp = obj.getBlueprint();
      
      if (!inImage(VM_Address.fromObjectNB(bp))) {
        Native.print_string("Skipping object with a blueprint not in image: ");
        Native.print_ptr(ptr);
        Native.print_string("\n");
        return ;
      }
      
      if (!bp.isArray()) {
        return ;
      }
      
      Blueprint.Array abp = bp.asArray();
      int length = abp.getLength(obj);
      int nptrs = allArrayletsInArray(abp, length);
      
      for(int i=0;i<nptrs;i++) {
        VM_Address aptr = getArraylet( ptr, abp, i );
        if (aptr == ptr) {
          Native.print_string("\tFound as arraylet pointer of index ");
          Native.print_int(i);
          Native.print_string(" in array at ");
          Native.print_ptr(ptr);
          Native.print_string("\n");
          
        }
      }
    }

    // for debugging only, this is indeed slow as hell
    private static void findArrayOfArraylet(VM_Address target) throws PragmaNoPollcheck {

      Native.print_string("Looking for arrays of arraylet ");
      printAddr(target);
      Native.print_string("\n");
      
      Native.print_string("-------------------------------\n");
      Native.print_string("In heap:\n\n");    
      for (int cur=0;cur<nBlocks;) {
        if (Bits.getBit(usedBits,cur)) {
          if (ARRAYLETS && Bits.getBit(arrayletBits, cur)) {
            cur++;
          } else if (Bits.getBit(largeBits,cur)) {
          
            if (sizes[cur]==-1) { // continuation
              cur++;
              continue;
            }
            VM_Address ptr = memory.add(blockSize*cur);
            
            int color = gcData.getColor(ptr);
            if (color==WHITE && color==GREYBLACK) {
              findArrayletPointerInObject(ptr, target);
            }

            cur+=sizes[cur];
          } else {
            int allocSize=sizes[cur];
            int nObjs=blockSize/allocSize;
            VM_Address ptr=memory.add(blockSize*cur);
            SizeClass sc=sizeClassBySize[allocSize/alignment];

            for (int i=nObjs;i-->0;ptr=ptr.add(allocSize)) {
              int color = gcData.getColor(ptr);
              
              if ( (color==WHITE) || (color==GREYBLACK)) {
                findArrayletPointerInObject(ptr, target);
              }
            }
            cur++;
          }
        } else {
          cur++;
        }
      }
      
      Native.print_string("-------------------------------\n");
      Native.print_string("In image:\n\n");              
      
      final VM_Address tgt = target;
      
      ImageScanner finder = new ImageScanner() {
        protected void walk (Oop oop, Blueprint bp) {
          findArrayletPointerInObject(VM_Address.fromObject(oop), tgt);
        }
      };
      
      finder.walk(Native.getImageBaseAddress(), Native.getImageEndAddress());
      
      Native.print_string("\n-------------------------------\n");
      Native.print_string("-------------------------------\n");
    }


    // for debugging only, this is indeed slow as hell
    private static void findReferees(VM_Address target) throws PragmaAtomicNoYield {

      Native.print_string("Looking for referees of object ");
      printAddr(target);
      Native.print_string("\n");
      
      Native.print_string("-------------------------------\n");
      Native.print_string("In heap:\n\n");    
      for (int cur=0;cur<nBlocks;) {
        if (Bits.getBit(usedBits,cur)) {
          if (ARRAYLETS && Bits.getBit(arrayletBits, cur)) {
            cur++;
          } else if (Bits.getBit(largeBits,cur)) {
          
            if (sizes[cur]==-1) { // continuation
              cur++;
              continue;
            }
            VM_Address ptr = memory.add(blockSize*cur);
            int color = gcData.getColor(ptr);
              
            if ( (color==WHITE) || (color==GREYBLACK)) {
              findRefereesInObject( ptr.asOopUnchecked(), ptr.asOopUnchecked().getBlueprint(), target);
            }
            cur+=sizes[cur];
            
          } else {
            int allocSize=sizes[cur];
            int nObjs=blockSize/allocSize;
            VM_Address ptr=memory.add(blockSize*cur);
            SizeClass sc=sizeClassBySize[allocSize/alignment];

            for (int i=nObjs;i-->0;ptr=ptr.add(allocSize)) {
              int color = gcData.getColor(ptr);
              
              if ( (color==WHITE) || (color==GREYBLACK)) {
                findRefereesInObject( ptr.asOopUnchecked(), ptr.asOopUnchecked().getBlueprint(), target);
              }
            }
            cur++;
          }
        } else {
          cur++;
        }
      }
      
      Native.print_string("-------------------------------\n");
      Native.print_string("In image:\n\n");              
      
      final VM_Address tgt = target;
      
      ImageScanner finder = new ImageScanner() {
        protected void walk (Oop oop, Blueprint bp) {
          findRefereesInObject(oop, bp, tgt);
        }
      };
      
      finder.walk(Native.getImageBaseAddress(), Native.getImageEndAddress());
      
      Native.print_string("\n-------------------------------\n");
      Native.print_string("-------------------------------\n");
    }

    static boolean findRefereesInObject(Oop oop,
			   Blueprint bp, VM_Address target ) throws PragmaNoReadBarriers, PragmaAtomicNoYield {

        if (!inImage(VM_Address.fromObjectNB(bp))) {
          Native.print_string("Skipping object with a blueprint not in image: ");
          Native.print_ptr(VM_Address.fromObjectNB(oop));
          Native.print_string("\n");
          return false;
        }

        boolean found = false;
        
	for (int i = 0;
	     i < ObjectModel.getObjectModel().maxReferences();
	     i++) {
	    
	    VM_Address ptr = VM_Address.fromObjectNB(oop.getReference(i));
	    if (ptr == target) {
	      Native.print_string("in object ");
	      printAddr(VM_Address.fromObjectNB(oop));
	      Native.print_string(" as object model reference no. ");
	      Native.print_int(i);
	      Native.print_string("\n");
	      found = true;
	    }
	}

	if (bp.isArray()) {
          Blueprint.Array abp = bp.asArray();
          
          if (abp.getComponentBlueprint().isReference()) { 
            int length = abp.getLength(oop);
            for(int i=0; i<length; i++) {
              VM_Address ptr = addressOfArrayElement(oop, i).getAddress();
              if (ptr == target) {
                Native.print_string("in array ");
                printAddr(VM_Address.fromObjectNB(oop));
                Native.print_string(" with blueprint ");
                printBlueprint(abp);
                Native.print_string(" as element at index ");
                Native.print_int(i);
                Native.print_string("\n");
                found = true;
              }              
            }
          }
          
	/*
	    Blueprint.Array abp = bp.asArray();
	    int elindex = 0;
	    if (abp.getComponentBlueprint().isReference()) {
		VM_Address p = abp.addressOfElement(oop, 0);
		VM_Address pend = p.add(VM_Word.widthInBytes()
					* abp.getLength(oop));
		while (p.uLT(pend)) {
                    VM_Address ptr = p.getAddress();
                    if (ptr == target) {
                      Native.print_string("in array ");
                      printAddr(VM_Address.fromObjectNB(oop));
                      Native.print_string(" as element at index ");
                      Native.print_int(elindex);
                      Native.print_string("\n");
                      found = true;
                    }
                    elindex++;            
		    p=p.add(VM_Word.widthInBytes());
		}
	    }
        */
	}
	else {
	    VM_Address base = VM_Address.fromObjectNB(oop);
	    int[] offset = bp.getRefMap();
	    for (int i = 0; i < offset.length; i++) {
		VM_Address ptr=base.add(offset[i]).getAddress();
		if (ptr == target) {
                      Native.print_string("in object ");
                      printAddr(VM_Address.fromObjectNB(oop));
                      Native.print_string(" with blueprint ");
                      printBlueprint(bp);
                      Native.print_string(" as reference field of index ");
                      Native.print_int(i);
                      Native.print_string(" at offset ");
                      Native.print_int(offset[i]);
                      Native.print_string("\n");		
                      found = true;
		}
	    }
	}
	
      return found;
    }
    
    
    static void verifyForwardingPointersInObject(Oop oop,
			   Blueprint bp, int forwardingOK ) throws PragmaNoReadBarriers, PragmaAtomicNoYield {

	for (int i = 0;
	     i < ObjectModel.getObjectModel().maxReferences();
	     i++) {
	    
	    verifyForwardingPointer( VM_Address.fromObjectNB(oop.getReference(i)), forwardingOK );
	}

	if (bp.isArray()) {

	    Blueprint.Array abp = bp.asArray();
	    if (abp.getComponentBlueprint().isReference()) {
	        int length = abp.getLength(oop);
	        for(int i=0; i<length; i++) {
  		  VM_Address p = addressOfArrayElement(oop, i);
                  verifyForwardingPointer("verifyForwardingPointerInObject",p.getAddress(),forwardingOK);
		}
	    }
	}
	else {
	    VM_Address base = VM_Address.fromObject(oop);
	    int[] offset = bp.getRefMap();
	    for (int i = 0; i < offset.length; i++) {
		VM_Address ptr=base.add(offset[i]);
                verifyForwardingPointer(ptr.getAddress(), forwardingOK);
	    }
	}
    }
    
    private static void verifyBlockIntegrity() throws PragmaNoPollcheck {
	for (int cur=0;cur<nBlocks;) {
	    if (Bits.getBit(usedBits,cur)) {
	        if (ARRAYLETS && Bits.getBit(arrayletBits, cur)) {
		    cur++;
		} else if (Bits.getBit(largeBits,cur)) {
		    if (gcData.getColor(memory.add(cur*blockSize))==FREE) {
			Native.print_string("ERROR: large object is free.\n");
		    }
		    int nBlocks=sizes[cur];
		    for (int i=cur+1;i<cur+nBlocks;++i) {
			if (!Bits.getBit(usedBits,i)) {
			    Native.print_string("ERROR: large object continuation is not marked used.\n");
			}
			if (!Bits.getBit(largeBits,i)) {
			    Native.print_string("ERROR: large object continuation is not marked large.\n");
			}
			if (sizes[i]!=-1) {
			    Native.print_string("ERROR: large object continuation is not marked as a continuation.\n");
			}
		    }
		    cur+=nBlocks;
		} else {
		    int allocSize=sizes[cur];
		    int nObjs=blockSize/allocSize;
		    VM_Address ptr=memory.add(blockSize*cur);
		    SizeClass sc=sizeClassBySize[allocSize/alignment];
		    int nPinned = 0;
		    int nPinnedScannedKeepAlive = 0;
		    for (int i=nObjs;i-->0;ptr=ptr.add(allocSize)) {
			if (gcData.getColor(ptr)==FREE) {
			    if (freeList.getPrev(ptr)==null) {
				if (VM_Address.fromInt(freeHeadsPtrs[cur])!=ptr) {
				    Native.print_string("ERROR: prev==null but we are not the head.\n");
				}
			    } else if (freeList.getNext(freeList.getPrev(ptr))!=ptr) {
				Native.print_string("ERROR: prev->next != this\n");
			    }
			    if (freeList.getNext(ptr)!=null &&
				freeList.getPrev(freeList.getNext(ptr))!=ptr) {
				Native.print_string("ERROR: next->prev != this\n");
			    }
			} else if (gcData.getColor(ptr)==FRESH) {
			  nPinned ++;
			} else if (isPinnedScannedKeepAlive(ptr)) {
			  nPinnedScannedKeepAlive ++;
			}
		    }
		    
		    if (nPinned == 0) {
		      if (nPinnedScannedKeepAlive != nPinnedObjects[cur]) { 
		        Native.print_string("ERROR: number of pinned objects (pinned-scanned-kept-alive) in the block is not in sync with nPinnedObjects, block index is:");
		        Native.print_int(cur);
		        Native.print_string("\n");
                      }
		    } 
		    else 
                    if (nPinnedScannedKeepAlive == 0) {
                      VM_Address blockBase = memory.add(blockSize*cur);
		      if ( (blockBase != sc.block) && (nPinned != nPinnedObjects[cur])) { 
		        Native.print_string("ERROR: number of pinned objects (pinned-nonscanned-not-kept-alive) in the block is not in sync with nPinnedObjects, block index is:");
		        Native.print_int(cur);
		        Native.print_string("\n");
                      }                    
                    }
		    else {
		      VM_Address blockBase = memory.add(blockSize*cur);
		      if (blockBase != sc.block) {
  		        if ((nPinned+nPinnedScannedKeepAlive) != nPinnedObjects[cur]) { 
		          Native.print_string("ERROR: number of pinned objects (both types) in the block is not in sync with nPinnedObjects, block index is:");
		          Native.print_int(cur);
		          Native.print_string("\n");
                        }
                      }
                    }
                    
		    cur++;
		}
	    } else {
		if (ARRAYLETS && Bits.getBit(arrayletBits,cur)) {
		    Native.print_string("ERROR: arraylet block is not marked used.\n");
		}	    
		if (Bits.getBit(largeBits,cur)) {
		    Native.print_string("ERROR: large object block is not marked used.\n");
		}
		cur++;
	    }
	}
    }
    
    private static void verifyBlockFreelistIntegrity() throws PragmaNoPollcheck {
	int prev=-1;
	for (int cur=blockHead;cur>=0;cur=blockNext[cur]) {
	    if (prev!=blockPrev[cur]) {
		Native.print_string("ERROR: invalid prev pointer in block freelist.\n");
	    }
	    if (Bits.getBit(usedBits,cur)) {
		Native.print_string("ERROR: block in block freelist is in use.\n");
	    }
	    if (Bits.getBit(largeBits,cur)) {
		Native.print_string("ERROR: block in block freelist is a large object block.\n");
	    }
	    if (ARRAYLETS && Bits.getBit(arrayletBits,cur)) {
		Native.print_string("ERROR: block in block freelist is an arraylet.\n");
	    }
	    verifyZeroed(memory.add(blockSize*cur),blockSize);	    
	    prev=cur;
	}
    }
    
    static void verifyHeapIntegrity() throws PragmaAtomicNoYield {
	// things this doesn't check for:
	// - blockCur (pointer bump allocation)
	// - errors that happen just before the crash

	Native.print_string("triPizlo: VERIFY_HEAP_INTEGRITY: running...\n");
	verifyFreelistIntegrity();
	verifyBlockIntegrity();
	verifyBlockFreelistIntegrity();
	Native.print_string("triPizlo: VERIFY_HEAP_INTEGRITY: done running.\n");
    }
    
    static void verifyNoPointerHasColor(String contextMsg, int color) throws PragmaAtomicNoYield, PragmaNoPollcheck {

        enterExcludedBlock();
	try {
	    for (int i=Bits.findSet(usedBits,currentNBitWords,0);
		 i>=0;
		 i=Bits.findSet(usedBits,currentNBitWords,i)) {
		if (false) {
  		  Native.print_string("VERIFY_HEAP_INTEGRITY: examining block ");
  		  Native.print_int(i);
  		  Native.print_string("\n");
                }
		VM_Address block=memory.add(i*blockSize);
		int size=sizes[i];
		
		if (ARRAYLETS && Bits.getBit(arrayletBits, i)) {
		  i++;
		  continue;
		}
		if (Bits.getBit(largeBits,i)) {
		    if (gcData.getColor(block)==color) {
		        Native.print_string("ERROR: ");
		        Native.print_string(contextMsg);
		        Native.print_string(": ");
		        printAddr(memory.add(i*blockSize));
		        Native.print_string("\n");
		        
			throw Executive.panic("VERIFY_HEAP_INTEGRITY: ERROR: there is a large object with not allowed color.\n");
		    }
		    i+=size;
		} else {
		    int nObjs=blockSize/size;
		    VM_Address cur=block;
		    for (int j=nObjs;j-->0;cur=cur.add(size)) {
                        if (false) {
    			  Native.print_string("VERIFY_HEAP_INTEGRITY: looking at object ");
    			  Native.print_ptr(cur);
    			  Native.print_string(", which has bits ");
    			  Native.print_int(gcData.getColor(cur));
    			  Native.print_string("\n");
                        }
			if (gcData.getColor(cur)==color) {
  		        Native.print_string("ERROR: ");
		        Native.print_string(contextMsg);
		        Native.print_string(": ");
		        printAddr(cur);
		        Native.print_string("\n");			
			    throw Executive.panic("VERIFY_HEAP_INTEGRITY: ERROR: there is a small object with not allowed color.\n");
			}
		    }
		    i++;
		}
	    }
	} finally {
	  leaveExcludedBlock();
	}
    
    }
    
    public void doGC() throws PragmaAssertNoExceptions, PragmaNoPollcheck {
    
        if (VERIFY_POLLING && !threadManager.isReschedulingEnabled()) {
          Native.print_string("In doGC, rescheduling enabled is ");
          Native.print_boolean(threadManager.isReschedulingEnabled());
          Native.print_string("\n");
          throw Executive.panic("fix");          
        }
    
        if (DEBUG_POLLING) {
          Native.print_string("triPizlo: DEBUG_POLLING: In doGC(), GC thread started.\n");
        }
        
        if (threadManager != null) {
          gcThread = (PriorityOVMThread)threadManager.getCurrentThread();
        }
        
        if (PERIODIC_TRIGGER || (SUPPORT_PERIODIC_SCHEDULER && periodicScheduler) || (SUPPORT_HYBRID_SCHEDULER && hybridScheduler)) {
        
          timerManager.addTimerInterruptAction(this);
        }                
        
        if (PROFILE_GC_PAUSE && timeProfile || REPORT_LONG_PAUSE) {
          timeProfileHook = true;
        }
        
        if (VERIFY_POLLING) {
          gcReschedulingExpected = false;
        }
        
	for (;;) {
	    
	  pollcheck(); // note we are PragmaNoPollcheck, so this is why...
	    
          if (VERIFY_POLLING && !threadManager.isReschedulingEnabled()) {
            Native.print_string("In doGC loop, rescheduling enabled is ");
            Native.print_boolean(threadManager.isReschedulingEnabled());
            Native.print_string("\n");
            throw Executive.panic("fix");            
          }
	    
          if (!shouldFreeSomeMemory) {
              
            if (DEBUG_POLLING) {
              Native.print_string("triPizlo: DEBUG_POLLING: In doGC(), waiting on aperiodic trigger for more memory to be filled up.\n");              
            }
                
            boolean enabled = threadManager.setReschedulingEnabled(false);
      
            if (VERIFY_POLLING && gcThreadBlockedByTrigger) {
              throw Executive.panic("gcThreadBlockedByTrigger is true when the thread is running - in doGC().");
            }

            if (VERIFY_POLLING && gcThreadBlockedByScheduler) {
              throw Executive.panic("gcThreadBlockedByScheduler is true when the thread is running - in doGC().");
            }
                
            try {

              if (VERIFY_POLLING) {
                gcReschedulingExpected = true;
              }
              
              threadManager.removeReady(gcThread);
              gcThreadBlockedByTrigger = true;
              
              if (REPORT_LONG_LATENCY && longLatency>0 && !vmIsShuttingDown) {

                long beforeMutatorTime = getCurrentTime();
                long latency = beforeMutatorTime - afterMutatorTime;
                
                boolean tooLong = latency>longLatency && afterMutatorTime>0 ;
                boolean newRecord = latency>maxObservedLatency && afterMutatorTime>0 ;
                int beforeMutatorLine = Native.getStoredLineNumber();

                if (beforeMutatorLine!=afterMutatorLine && afterMutatorLine!=0) {
                  Native.print_string("Trigger: ERROR-maybe: bypassed pollcheck at line ");
                  Native.print_int(beforeMutatorLine);
                  Native.print_string("\n");
                }
        
                threadManager.runNextThread();
                /* woken up */                
                
                pollcheckInstr(); // forces storing of the line number
                afterMutatorTime = getCurrentTime();
                afterMutatorLine = Native.getStoredLineNumber();        

                if (tooLong) {
                  Native.print_string("Trigger: long latency detected between pollchecks at lines ");
                  Native.print_int(beforeMutatorLine);
                  Native.print_string(" and ");
                  Native.print_int(afterMutatorLine);
                  Native.print_string(", latency was ");
                  Native.print_long(latency);
                  Native.print_string(" ns \n");        
                }

                if (newRecord) {
                  maxObservedLatency = latency;
                  maxObservedLatencyLineFrom = beforeMutatorLine;
                  maxObservedLatencyLineTo = afterMutatorLine;
                }
                
              } else {
                threadManager.runNextThread();
                /* woken up */                
              }
              
              if (VERIFY_POLLING) {
                gcReschedulingExpected = false;
              }
                            
            } finally {
              threadManager.setReschedulingEnabled(enabled);  
            }                

            if (DEBUG_POLLING) {
              Native.print_string("triPizlo: DEBUG_POLLING: In doGC(), woken up - there is a lot of used memory, time to do gc.\n");
            }
            
            pollcheck();  // make sure the GC is stopped if it should not run now (periodic scheduling, for instance)
              // FIXME: couldn't the GC steal a quantum this way ? let's fix it
          }
            
          reallyCollect();
	    
          if ( ( (!EFFECTIVE_MEMORY_SIZE && (nUsedBlocks+gcThreshold < nBlocks)) ||
            (EFFECTIVE_MEMORY_SIZE && (nUsedBlocks+gcThreshold < effectiveNBlocks))) ) {
            
            shouldFreeSomeMemory = false;
          }	    
            
          if (VERIFY_HEAP_INTEGRITY) {
              verifyHeapIntegrity();
          }
	}
    }

    public void runGCThread() {
	try {
	    threadRunning=true;
	    if (DEBUG_GC && debugGC) {
		Native.print_string("triPizlo: GC: GC thread running\n");
	    }
	    LocalReferenceIterator.the().prepareForGC();
	} catch (Throwable e) {
	    Executive.panicOnException(e,"In gcThreadMain");
	}
    }

    static void updateAndMarkAt(VM_Address ptr) throws PragmaInline, PragmaAssertNoExceptions {
    
      VM_Address addr = ptr.getAddress();
     
      if (addr.isNonNull()) {
      
        if ( (COMPACTION && REPLICATING && gcData.getOld(addr)!=0) ||
             (COMPACTION && BROOKS ) ) {
             
            VM_Address newAddr = translateNonNullPointer(addr);
            ptr.setAddress(newAddr);
            markNonNull(newAddr);
        } else {
          markNonNull(addr);
        }
      }
    }

    static void updateAndMark(VM_Address addr) throws PragmaInline, PragmaAssertNoExceptions {
      if (addr.isNonNull()) {
        updateAndMarkNonNull(addr);
      }
    }
    
    static void updateAndMarkNonNull(VM_Address addr) throws PragmaInline, PragmaAssertNoExceptions {
    
      if ( (COMPACTION && REPLICATING && gcData.getOld(addr)!=0) ||
           (COMPACTION && BROOKS) ) {

        VM_Address newAddr = translateNonNullPointer(addr);
        markNonNull(newAddr);
      } else {
        markNonNull(addr);
      }
    }

    static void markUnchecked(VM_Address addr)
	throws PragmaNoPollcheck, PragmaAssertNoExceptions, PragmaAssertNoSafePoints {

        if (VERIFY_COMPACTION || VERIFY_BUG) {
          verifyForwardingPointer("markUnchecked",addr, FWD_NEVER);
          
          int color = gcData.getColor(addr);
          if (color!=WHITE) {
            Native.print_string("ERROR: marking object which is not white\n");
            printAddr(addr);
            Native.print_string("\n");
          }
        }	
	if (DEBUG_MARK && debugMark) {
	    Native.print_string("triPizlo: MARK: marking ");
	    Native.print_ptr(addr);
	    Native.print_string("\n");
	}
	if (VERIFY_FAST_IMAGE_SCAN && fastImageScanVerificationActive) {
	    Native.print_string("found new root at ");
	    Native.print_ptr(walkObjectPointerSource);
	    Native.print_string(" which has  color ");
	    Native.print_string(colorName(gcData.getColor(walkObjectPointerSource)));
	    Native.print_string(" which has a bp at ");
	    Native.print_ptr(VM_Address.fromObject(walkObjectPointerSource.asOopUnchecked().getBlueprint()));
	    Native.print_string(" and points to ");
	    Native.print_ptr(addr);
	    Native.print_string(" which has color ");
	    Native.print_string(colorName(gcData.getColor(addr)));
	    Native.print_string(" which has a bp at ");
	    Native.print_ptr(VM_Address.fromObject(addr.asOopUnchecked().getBlueprint()));
	    Native.print_string(" and is at image page ");
	    Native.print_int(imageAddressToPageIndex(walkObjectPointerSource));
	    Native.print_string(" which is ");
	    Native.print_string( Bits.getBit(dirty, imageAddressToPageIndex(walkObjectPointerSource)) ? "DIRTY" : "NOT-DIRTY!" );
	    Native.print_string("\n");
	    fastImageScanFailed=true;
	}
	
	if (VERIFY_BUG) {
	  if (allocColor != GREYBLACK) {
	    throw Executive.panic("Would mark with a color which is not allocation color. Forgotten call to mark ?");
	  }
	}
	//gcData.setColor(addr,GREYBLACK);
	gcData.setColor(addr, allocColor); // -- this is not any slower, and it is more robust against incorrect calls of this function
	
	if (!worklistEmpty && worklistPuti==worklistGeti) {
	  Native.print_string("triPizlo: ERROR: Ran out of worklist space in the GC (markUnchecked)\n");
	  throw Executive.panic("Too bad...");
	}
	
	worklist.add(worklistPuti*wordSize).setAddress(addr); //!! note no forwarding here - not neeed, mark is called on the new location anyway
	worklistEmpty=false;
	worklistPuti++;
	worklistPuti&=worklistBits;
	marked++;
    }
    
    static boolean markingBugDetected = false;
    
    static void markNonNull(VM_Address addr) throws PragmaNoPollcheck, PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
	if (DEBUG_MARK_VERBOSE && debugMarkVerbose) {
	    Native.print_string("triPizlo: MARK_VERBOSE: attempting to mark ");
	    Native.print_ptr(addr);
	    Native.print_string("\n");
	}
	if (VERIFY_HEAP_INTEGRITY && inHeap(addr)) {
	    verifyPointer("markNonNull",addr);
	}
	
	if (VERIFY_BUG) {
	  if (addr.isNull()) {
//	    throw Executive.panic("ERROR: BUG: null pointer in markNonNull");
	    Native.print_string("ERROR: BUG: null pointer in markNonNull\n");
	    Native.print_string("ERROR: NOT MARKING NULL POINTER\n");
	    markingBugDetected = true;
	    return;
	  }
	  
/*	  if (!inHeap(addr) && !inImage(addr)) {
	    Native.print_string("ERROR: BUG: pointer is neither in heap, nor in image (markNonNull): ");
	    printAddr(addr);
	    Native.print_string(" with blueprint ");
	    printBlueprint(addr);
	    Native.print_string("\n");
	    throw Executive.panic("Fix..");
	  }*/
	  verifyForwardingPointer("markNonNull", addr, FWD_NEVER);
	}
	
	if (gcData.getColor(addr)==WHITE) {
	    markUnchecked(addr);
	}
    }
    
    static void mark(VM_Address addr) throws PragmaNoPollcheck {
	if (addr.isNonNull()) {
	    markNonNull(addr);
	}
    }
    
    static void markAmbiguous(VM_Address addr) throws PragmaNoPollcheck {
	int offset=offset(addr);
	int block=blockIdxOff(offset);
	if (block>=0 && block<nBlocks
	    && Bits.getBit(usedBits,block)) {
	    int size=sizes[block];
	    if (ARRAYLETS && Bits.getBit(arrayletBits, block)) {
	      // nobody should have a pointer to an arraylet
	      return;
	    }
	    if (Bits.getBit(largeBits,block)) {
		while (size<0) {
		    block--;
		    size=sizes[block];
		}
		markNonNull(memory.add(block*blockSize));
	    } else {
		// this works for bump-pointer blocks because all objects
		// above the pointer are FRESH (because FRESH is 0), thus they are not marked
		
		VM_Address objAddr = memory.add(((offset-block*blockSize)/size)*size+block*blockSize); 
		// so we can have pointers pointing inside objects ?
		
		
                // calling markNonNull on a FREE object is not in general a problem, because a FREE object is not WHITE,
                // and thus will not be really marked,
                // but, with VERIFY_HEAP_INTEGRITY, an error would be reported anyway, which is confusing
                
// this worked without compaction
//   but with compaction, updateAndMarkNonNull will try to forward the object
//   but if the object is free, then, the forwarding will fail
//                 
//		if ( (!VERIFY_HEAP_INTEGRITY) || (gcData.getColor(objAddr)!=FREE) ) {

                if (gcData.getColor(objAddr)==WHITE) {  
                  updateAndMarkNonNull(objAddr);
                }
	    }
	}
    }
    
    
    static VM_Address worklistPop()
	throws PragmaNoPollcheck {
	VM_Address result=worklist.add(worklistGeti*wordSize).getAddress();
	worklistGeti++;
	worklistGeti&=worklistBits;
	worklistEmpty=(worklistGeti==worklistPuti);
	return result;
    }
    
    static void storeWithoutBarrier( Oop referee, int refOffset, Oop newReferent ) {
    
      VM_Address ptrPtr=VM_Address.fromObjectNB(referee).add(refOffset);
      ptrPtr.setAddress(VM_Address.fromObjectNB(newReferent));
    }
    
    // WARNING
    // WARNING
    //   does not check where the target address is... (image, nowhere, heap, ...)
    //   does not forward / nullcheck    
    static void markFromBarrier( VM_Address addr ) throws PragmaNoPollcheck, PragmaInline {

	if (gcData.getColor(addr)==WHITE) {

          if (VERIFY_COMPACTION || VERIFY_BUG) {
            verifyForwardingPointer("markUnchecked",addr, FWD_NEVER);
          }	

	  // when marking, set to black; when not marking, set to
	  // white (so no change)
	  gcData.setColor(addr ,allocColor);
	  
          // when not marking, this happily corrupts the worklist,
          // but we don't care - it is simply reset at the beginning
          // of the next GC.
		
          if (!worklistEmpty && worklistPuti==worklistGeti) {
          
            // if allocation color is WHITE, the marking is only to give predictable overhead ; running out of 
            //  worklist / corrupting it does not matter
            if (allocColor == GREYBLACK) {
  	      Native.print_string("triPizlo: ERROR: Ran out of worklist space in the GC (markFromBarrier)\n");
  	      throw Executive.panic("Too bad...");
            }
          }
		
          worklist.add(worklistPuti*wordSize).setAddress(addr);
          worklistEmpty=false;
          worklistPuti++;
          worklistPuti&=worklistBits;
          marked++;
        }
    }
    
    // works with unforwarded refereePtr
    static void imageStoreBarrier(VM_Address refereePtr, int aReferee) 
      throws PragmaNoPollcheck, PragmaInline, PragmaAssertNoExceptions, PragmaAssertNoSafePoints  {

        if (VERIFY_ASSERTIONS) {
          verifyAssertions(refereePtr, aReferee);
        }
        
        if (IGNORE_ASSERTIONS) {
          aReferee = 0;
        }

        if (! (NEEDS_IMAGE_BARRIER || FORCE_IMAGE_BARRIER) ) {
          return ;
        }
        
        if ((aReferee&Assert.HEAPONLY)!=0) {
          return ;
        }
        
	if ( (aReferee&Assert.IMAGEONLY)!=0 || inImage(refereePtr)) {
	    int ipIdx = imageAddressToPageIndex(refereePtr);
	    
	    // assert((ipIdx >=0) && (ipIdx <= imageAddressToPageIndex(Native.getImageEndAddress())));
	    //     Bits.setBit(dirty,ipIdx); (too slow)
            VM_Address slotAddr = dirtyValuesStart.add( (ipIdx/32)*4 );
            slotAddr.setInt( slotAddr.getInt() | (1<<(ipIdx&31)) );

	} else {
	  if ((DEBUG_BARRIER && debugBarrier)) {
	    if (imageSize.EQ(VM_Word.fromInt(0))) { // this happens for the TheMan instance itself, where it does not matter
	      Native.print_string("ERROR: storeBarrier called when imageSize is still zero. Don't know if pointer is in image: ");
	      Native.print_ptr(refereePtr);
	      Native.print_string(" with blueprint ");
	      printBlueprint(refereePtr);
	      Native.print_string("\n");
            }
          }
	}
    }
    
    // note: forwards both oldPtr and newPtr before marking if needed
    // note: storeTo must be null, or forwarded location
    static void markingStoreBarrier(VM_Address oldPtr, VM_Address newPtr, int aHostObj, int aNewObj, VM_Address storeTo, VM_Address storeToUnfwd, boolean doStore, boolean doUnfwdStore)
      throws PragmaNoPollcheck, PragmaInline, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {


        if (IGNORE_ASSERTIONS) {
          aHostObj = 0;
          aNewObj = 0;
        }
        
        if (PREDICTABLE_BARRIER) {
	    // this barrier is designed to have the same performance
	    // whether it is active or not.
	    // !! currently, this does not completely hold for incremental object copy & replication
	    
//	    if ( (NEEDS_YUASA_BARRIER || FORCE_YUASA_BARRIER) && !((aHostObj&Assert.IMAGEONLY)!=0) && !((aHostObj&Assert.IN_IMAGEONLY_SLOT)!=0)) {
	    if ( (NEEDS_YUASA_BARRIER || FORCE_YUASA_BARRIER) && !((aHostObj&Assert.IN_IMAGEONLY_SLOT)!=0)) {

  	      /* Yuasa */
  	      
  	      if (DEBUG_BARRIER && debugBarrier) {
		Native.print_string("triPizlo: BARRIER: dealing with old referrent (predictable barrier)");
		Native.print_ptr(oldPtr);
		Native.print_string("\n");
              }

              if (oldPtr != null ) {
                VM_Address oldPtrFwd = updateNonNullPointer(oldPtr);
                markFromBarrier(oldPtrFwd);
              }
            }

	    
            /* Dijkstra */
            if ( (NEEDS_DIJKSTRA_BARRIER || FORCE_DIJKSTRA_BARRIER) &&  !((aNewObj&Assert.NULL)!=0) && !((aNewObj&Assert.IMAGEONLY)!=0) ) {                    

    	        if (DEBUG_BARRIER && debugBarrier) {
		  Native.print_string("triPizlo: BARRIER: dealing with new referrent (predictable barrier)");
		  Native.print_ptr(newPtr);
		  Native.print_string("\n");
                }              

                if ( (aNewObj&Assert.NONNULL)!=0 || ( (aNewObj&Assert.NULL)==0 && newPtr!=null ) ) {
                  VM_Address newPtrFwd = updateNonNullPointer(newPtr, aNewObj);
                  markFromBarrier( newPtrFwd );
                  if (doStore) {
                  
                    if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && INCREMENTAL_OBJECT_COPY && !updatingPointersWrittenToHeapNeeded) {
                      // need to write the old pointer
                      storeTo.setAddress( newPtr );
                      if (doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
                        storeToUnfwd.setAddress( newPtr );                    
                      }                      
                    } else {
                      storeTo.setAddress( newPtrFwd );
                      if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
                        storeToUnfwd.setAddress( newPtrFwd );                    
                      }
                    }
                  }
                } else {
                  /* newPtr is null */
                  if (doStore) {
                    storeTo.setAddress( null );
                    if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
                      storeToUnfwd.setAddress( null );
                    }
                  }
                }
	    } else {
	      if (doStore) { 
	        if ( (aNewObj&Assert.NONNULL)!=0 || ( (aNewObj&Assert.NULL)==0 && newPtr!=null ) ) {
  	          
  	          if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && INCREMENTAL_OBJECT_COPY && !updatingPointersWrittenToHeapNeeded) {
  	            // need to write the old pointer
  	            storeTo.setAddress( newPtr );
  	            if (doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
                      storeToUnfwd.setAddress( newPtr );
                    }
                  } else {
                    VM_Address newPtrFwd = updateNonNullPointer(newPtr, aNewObj); 
                    storeTo.setAddress( newPtrFwd );
                    if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
                      storeToUnfwd.setAddress( newPtrFwd );                    
                    }
                  } 	           
                } else {
                  /* newPtr is null */
                  storeTo.setAddress( null );
                  if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
                    storeToUnfwd.setAddress( null );                  
                  }
                }
	      }
	    }            
	} else { 
//	    if ( (NEEDS_YUASA_BARRIER || FORCE_YUASA_BARRIER) &&  !((aHostObj&Assert.IMAGEONLY)!=0) && !((aHostObj&Assert.IN_IMAGEONLY_SLOT)!=0) && marking) {
	    if ( (NEEDS_YUASA_BARRIER || FORCE_YUASA_BARRIER) && !((aHostObj&Assert.IN_IMAGEONLY_SLOT)!=0) && marking) {

  	      /* Yuasa */
  	      
  	      if (DEBUG_BARRIER && debugBarrier) {
		Native.print_string("triPizlo: BARRIER: dealing with old referrent (non-predictable barrier)");
		Native.print_ptr(oldPtr);
		Native.print_string("\n");
              }

              if (oldPtr.isNonNull() ) {
                VM_Address oldPtrFwd = updateNonNullPointer(oldPtr);
                markNonNull(oldPtrFwd);
              }
            }

            /* Dijkstra */
            if ( (NEEDS_DIJKSTRA_BARRIER || FORCE_DIJKSTRA_BARRIER) && !((aNewObj&Assert.NULL)!=0) && !((aNewObj&Assert.IMAGEONLY)!=0) && dijkstraBarrierOn ) {
              // note dijkstraBarrierOn => we are now marking and not moving objects, so we can safely write updated
              // pointers unconditionally even for replication and incremental object copy

    	        if (DEBUG_BARRIER && debugBarrier) {
		  Native.print_string("triPizlo: BARRIER: dealing with new referrent (non-predictable barrier)");
		  Native.print_ptr(newPtr);
		  Native.print_string("\n");
                }              

                if ( (aNewObj&Assert.NONNULL)!=0 || ((aNewObj&Assert.NULL)==0 && newPtr.isNonNull()) ) {
                  VM_Address newPtrFwd = updateNonNullPointer(newPtr, aNewObj);
                  markNonNull( newPtrFwd );
                  if (doStore) {
                    storeTo.setAddress( newPtrFwd );
                    if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
                      storeToUnfwd.setAddress( newPtrFwd );
                    }
                  }
                } else {
                  /* newPtr is null */
                  if (doStore) {
                    storeTo.setAddress( null );
                    if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
                      storeToUnfwd.setAddress( null );
                    }
                  }
                }
	    } else { // here, we may or may not be marking (we have to check before writing updated pointers to the heap)
	      if (doStore) { 
	        if ( (aNewObj&Assert.NONNULL)!=0 || ((aNewObj&Assert.NULL)==0 && newPtr!=null) ) {
	        
                  if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && INCREMENTAL_OBJECT_COPY && !updatingPointersWrittenToHeapNeeded) {
                    storeTo.setAddress( newPtr );
                    if (doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
                      storeToUnfwd.setAddress( newPtr );
                    }
                  } else {
                    VM_Address newPtrFwd = updateNonNullPointer(newPtr, aNewObj); // this translation could be again conditional on GC state
                    storeTo.setAddress( newPtrFwd ); 
                    if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
  	              storeToUnfwd.setAddress( newPtrFwd );
                    }  
                  }
                } else {
                  /* newPtr is null */
                  storeTo.setAddress( null );
  	          if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && doUnfwdStore && !((aHostObj&Assert.IMAGEONLY)!= 0)) {
  	            storeToUnfwd.setAddress( null );
                  }                    
                }
	      }
	    }
	} /* non-predictable barrier */      
    }
    

    
    // when compacting, the barrier forwards both the referee (host) and the new referent
    //  - if needed
    static void storeBarrier(Oop referee,int refOffset,Oop newReferent, int aReferee, int aReferent)
	throws PragmaNoPollcheck, PragmaInline, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers {
	// what happens when we are scanning an array?
	// answer: it just works.  the thing is, we don't ever have to
	// know the difference between grey and black.

	if (IGNORE_ASSERTIONS) {
	  aReferent = 0;
	  aReferee = 0;
	}

	VM_Address ptrPtrOld = VM_Address.fromObjectNB(referee).add(refOffset);

        VM_Address refereePtr = translatePointer( VM_Address.fromObjectNB(referee), aReferee );
	VM_Address ptrPtr = refereePtr.add(refOffset); // points to forwarded location in case of Brooks barrier

        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(ptrPtr);
          verifyMemoryAccess(ptrPtrOld);          
        }

        // note - the barrier does the store
        // it's faster to do it there than here, we save one branch	
        
        if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
          // note the "ptrPtrOld" is important here ; after sweep, the mutator has only pointers to new locations of objects and
          // is only allowed to read from the new locations
          // the old locations however still exist - if referenced from object header - and contain unfixed pointers -if these pointers
          // got back to stack, and in between the old location was swept, .. 
          //
          // these new locations still point to the old locations, however, so ptrPtr.getAddress() would not work
          markingStoreBarrier( ptrPtrOld.getAddress(), VM_Address.fromObjectNB(newReferent), aReferee, aReferent, ptrPtr, ptrPtrOld, true, true);
        } else {
          // Brooks or nothing
          markingStoreBarrier( ptrPtr.getAddress(), VM_Address.fromObjectNB(newReferent), aReferee, aReferent, ptrPtr, null, true, false);
        }
	
	// must be after the write (due to boot-time initialization)
	if ( (aReferent&Assert.IMAGEONLY) == 0 ) {
  	  imageStoreBarrier(refereePtr, aReferee); 
        }
	
	if (REPLICATING && VERIFY_COMPACTION) {
	  if ( ptrPtr.getAddress() != ptrPtrOld.getAddress() ) {
	    Native.print_string("triPizlo: ERROR: Error in storeBarrier - replicas don't have the same content after write\n");
	  }
	}
	
	if (VERIFY_COMPACTION && (!REPLICATING || !VERIFY_REPLICAS_SYNC)) {   
	  verifyForwardingPointer( refereePtr, FWD_ALWAYS );
	  verifyForwardingPointer( ptrPtr.getAddress(), FWD_ALWAYS );
	}
    }
    

    // FIXME: we can also inline these ; we get by 8% larger binary, but it may sometimes be what we want
    // make this configurable one (now it's about removing PragmaNoInline from these)
        
    static void storeBarrier_2_1(CoreServicesAccess csa,
				Oop src,int offset, Oop tgt)
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	storeBarrier(src,offset,tgt,2,1);
    }

    static void storeBarrier_10_8(CoreServicesAccess csa,
				Oop src,int offset, Oop tgt)
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	storeBarrier(src,offset,tgt,10,8);
    }
    
    static void storeBarrier_2_2(CoreServicesAccess csa,
				Oop src,int offset, Oop tgt)
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	storeBarrier(src,offset,tgt,2,2);
    }    

    static void storeBarrier_2_0(CoreServicesAccess csa,
				Oop src,int offset, Oop tgt)
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	storeBarrier(src,offset,tgt,2,0);
    }

    static void storeBarrier_18_4(CoreServicesAccess csa,
				Oop src,int offset, Oop tgt)
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	storeBarrier(src,offset,tgt,18,4);
    }    

    static void storeBarrier_10_1(CoreServicesAccess csa,
				Oop src,int offset, Oop tgt)
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	storeBarrier(src,offset,tgt,10,1);
    }


    public void storeBarrierDispatch(CoreServicesAccess csa,
				Oop src,int offset, Oop tgt, int aSrc, int aTgt)
	throws PragmaNoPollcheck, PragmaInline, PragmaAssertNoSafePoints, PragmaAssertNoExceptions   {
	

	  // top putfield combinations
	  // NONNULL <- NULL
	  // NONNULL HEAPONLY <- HEAPONLY
	  // NONNULL <- NONNULL
	  
	  // top aastore combinations
	  // NONNULL HEAPONLY <- HEAPONLY
	  // NONNULL IMAGEONLY_SLOT <- IMAGEONLY
	  // NONNULL HEAPONLY <- NULL
	  
	if ((aSrc&31)==2 && (aTgt&31)==1) {
	    // NONNULL <- NULL
	  storeBarrier_2_1(csa, src,offset,tgt);
	} 
	else       
	if ((aSrc&31)==10 && (aTgt&31)==8) {
	    // NONNULL HEAPONLY <- HEAPONLY
	  storeBarrier_10_8(csa, src,offset,tgt);
	} 	
	else       	
	if ((aSrc&31)==2 && (aTgt&31)==2) {
	    // NONNULL <- NONNULL
	  storeBarrier_2_2(csa, src,offset,tgt);
	} 		
	else 
	if ((aSrc&31)==18 && (aTgt&31)==4) {
	    // NONNULL IMAGEONLY_SLOT <- IMAGEONLY
	  storeBarrier_18_4(csa, src,offset,tgt);
	} 	
	else       	
	if ((aSrc&31)==10 && (aTgt&31)==1) {
	    // NONNULL HEAPONLY <- NULL
	  storeBarrier_10_1(csa, src,offset,tgt);
	} 		
	else {
	    // NONNULL <- UNKNOWN
	  storeBarrier_2_0(csa, src,offset,tgt);
	}
    }

        
    // note: forwards both the src and tgt before use if needed
    public void putFieldBarrier(CoreServicesAccess csa,
				Oop src,int offset, Oop tgt, int aSrc, int aTgt)
	throws PragmaNoPollcheck, PragmaInline, PragmaAssertNoSafePoints, PragmaAssertNoExceptions   {

        if (false) { // profiling assertions
          Native.print_string("\n");
          Native.print_int(aSrc);
          Native.print_string("=");
          Native.print_int(aTgt);
          Native.print_string("\n");
          storeBarrier(src,offset,tgt,aSrc,aTgt);  
          return ;
        }

        if (VERIFY_ASSERTIONS) {
          verifyAssertions(src, aSrc);
          verifyAssertions(tgt, aTgt);
        }

        storeBarrierDispatch(csa, src, offset, tgt, aSrc, aTgt);
    }    

    // note: forwards both the src and tgt before use if needed
    public void aastoreBarrier(CoreServicesAccess csa,
			       Oop src,int offset, Oop tgt, int aSrc, int aTgt)
        throws PragmaNoPollcheck, PragmaInline, PragmaAssertNoSafePoints, PragmaAssertNoExceptions   {

        if (false) { // profiling assertions
          Native.print_string("\n");
          Native.print_int(aSrc);
          Native.print_string("=");
          Native.print_int(aTgt);
          Native.print_string("\n");
          storeBarrier(src,offset,tgt,aSrc,aTgt);  
          return ;
        }

        if (VERIFY_ASSERTIONS) {
          verifyAssertions(src, aSrc);
          verifyAssertions(tgt, aTgt);
        }

        storeBarrierDispatch(csa, src, offset, tgt, aSrc, aTgt);
    }
    
    // array store barriers
    //   no compaction - easy, should be optimized by gcc to a simple array access
    //		when we start doing bounds checks here, they should often be elided based on statically
    //		known info
    //		       - single pointer to write to is ok
    //   Brooks - single pointer to write to is ok
    //		we can know statically that the array is contiguous (when small), then we can elide the 
    //			dereference of arraylet pointer
    //		we can know statically that the array in unmovable (when large), the we can completely elide
    //			translating barrier -- BTW, this works even without arraylets
    //		we can know statically that the storage reference is in an arraylet, and then we can elide 
    //			translating barrier
    //		we can know statically in which arraylet it is
    //
    //	FIXME:  we need to know statically also the header size ; we must not read it from even final
    //		java fields, because that cannot be optimized by the compiler
    //
    //		well, headerSkipBytes() of ObjectModel seems safe -- it's bytecode-rewritten to return a constant
    //		after header there is the length field (word size)
    //
    //		then, for primitive arrays that store wides (doubles, longs), the beginning of the data is aligned
    //		- which means ... that we should align such data as well, but the padding should be after the 
    //		arraylet pointers... not before
    //		- this is bad, because now the presence of padding would not only depend on array type, but also
    //		on array size, which might prevent some optimizations - oh, maybe not, because we need to know the length
    //		anyway to do those optimizations
    
    // lets' start with an implementation that will intentionally only support brooks barrier and do it the stupid way

    public static void arrayletAastoreBarrier(Oop array, int index, Oop newReferent, int aReferee, int aReferent) 
      throws PragmaNoPollcheck, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
    
      if (VERIFY_ASSERTIONS) {
        verifyAssertions( VM_Address.fromObjectNB(array), aReferee );
        verifyAssertions( VM_Address.fromObjectNB(newReferent), aReferent );
      }
    
      if (IGNORE_ASSERTIONS) {
        aReferent = 0;
        aReferee = 0;
      }
    
      if (!ARRAYLETS) {
        throw Executive.panic("aastoreBarrier - arraylet version - called when it should not");
      }
      
      VM_Address addr = addressOfArrayElement(array, index, MachineSizes.BYTES_IN_ADDRESS, aReferee);
      
      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr);
      }
      
      // note - the barrier does the store
      // it's faster to do it there than here, we save one branch	
      markingStoreBarrier( addr.getAddress(), VM_Address.fromObjectNB(newReferent), aReferee, aReferent, addr, null, true, false);
	
      // must be after the write (due to boot-time initialization)
      if ( (aReferent&Assert.IMAGEONLY) == 0 ) {
        imageStoreBarrier(VM_Address.fromObjectNB(array), aReferee); 
      }
	
    }
    
    // the arraylet version
    // FIXME: array size assertions are lost here
    public void aastoreBarrier(CoreServicesAccess csa,
				Oop array,int index, Oop tgt, int componentSize, int aArray, int aTgt)
	throws PragmaNoPollcheck, PragmaInline, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaCAlwaysInline  {
	
        if (!ARRAYLETS) {
          throw Executive.panic("arraylet version of aastoreBarrier when arraylets are off");
        }
        
        // this should probably be implicit
        if (VERIFY_ARRAYLETS) {
          assert(componentSize == MachineSizes.BYTES_IN_WORD);
        }

	  // I'm using also the putfield combinations, because it shouldn't cost much and because
	  // of the turned off heaponly types detection
	  
	  // top putfield combinations
	  // NONNULL <- NULL
	  // NONNULL HEAPONLY <- HEAPONLY
	  // NONNULL <- NONNULL
	  
	  // top aastore combinations
	  // NONNULL HEAPONLY <- HEAPONLY
	  // NONNULL IMAGEONLY_SLOT <- IMAGEONLY
	  // NONNULL HEAPONLY <- NULL
	  
	if ((aArray&31)==2 && (aTgt&31)==1) {
	    // NONNULL <- NULL
	  arrayletAastoreBarrier_2_1(csa, array, index, tgt);
	} 
	else       
	if ((aArray&31)==10 && (aTgt&31)==8) {
	    // NONNULL HEAPONLY <- HEAPONLY
	  arrayletAastoreBarrier_10_8(csa, array, index, tgt);
	} 	
	else       	
	if ((aArray&31)==2 && (aTgt&31)==2) {
	    // NONNULL <- NONNULL
	  arrayletAastoreBarrier_2_2(csa, array, index, tgt);
	} 		
	else 
	if ((aArray&31)==18 && (aTgt&31)==4) {
	    // NONNULL IMAGEONLY_SLOT <- IMAGEONLY
	  arrayletAastoreBarrier_18_4(csa, array, index, tgt);
	} 	
	else       	
	if ((aArray&31)==10 && (aTgt&31)==1) {
	    // NONNULL HEAPONLY <- NULL
	  arrayletAastoreBarrier_10_1(csa, array, index, tgt);
	} 		
	else {
	    // NONNULL <- UNKNOWN
	  arrayletAastoreBarrier_2_0(csa, array, index, tgt);
	}
    }
    
    static void arrayletAastoreBarrier_2_1(CoreServicesAccess csa,
				Oop array,int index, Oop tgt) 
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	arrayletAastoreBarrier(array,index,tgt,2,1);
    }
    
    static void arrayletAastoreBarrier_10_8(CoreServicesAccess csa,
				Oop array,int index, Oop tgt) 
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	arrayletAastoreBarrier(array,index,tgt,10,8);
    }

    static void arrayletAastoreBarrier_2_2(CoreServicesAccess csa,
				Oop array,int index, Oop tgt) 
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	arrayletAastoreBarrier(array,index,tgt,2,2);
    }

    static void arrayletAastoreBarrier_18_4(CoreServicesAccess csa,
				Oop array,int index, Oop tgt) 
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	arrayletAastoreBarrier(array,index,tgt,18,4);
    }

    static void arrayletAastoreBarrier_10_1(CoreServicesAccess csa,
				Oop array,int index, Oop tgt) 
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	arrayletAastoreBarrier(array,index,tgt,10,1);
    }

    static void arrayletAastoreBarrier_2_0(CoreServicesAccess csa,
				Oop array,int index, Oop tgt) 
	throws PragmaNoPollcheck, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaNoInline  {
	arrayletAastoreBarrier(array,index,tgt,2,0);
    }
        
    public void bastoreBarrier(CoreServicesAccess csa, Oop array, int index, byte newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        addr.setByte(newValue);
    }

    public void castoreBarrier(CoreServicesAccess csa, Oop array, int index, char newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }        
        addr.setChar(newValue);
    }

    public void dastoreBarrier(CoreServicesAccess csa, Oop array, int index, double newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        addr.setDouble(newValue);
    }

    public void fastoreBarrier(CoreServicesAccess csa, Oop array, int index, float newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        addr.setFloat(newValue);
    }

    public void iastoreBarrier(CoreServicesAccess csa, Oop array, int index, int newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        addr.setInt(newValue);
    }

    public void lastoreBarrier(CoreServicesAccess csa, Oop array, int index, long newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        addr.setLong(newValue);
    }

    public void sastoreBarrier(CoreServicesAccess csa, Oop array, int index, short newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        addr.setShort(newValue);
    }

    public Oop aaloadBarrier(CoreServicesAccess csa, Oop array, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
        
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        return addr.getAddress().asOopUnchecked();
    }

    public byte baloadBarrier(CoreServicesAccess csa, Oop array, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
        
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        return addr.getByte();
    }

    public char caloadBarrier(CoreServicesAccess csa, Oop array, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
        
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        return addr.getChar();
    }

    public double daloadBarrier(CoreServicesAccess csa, Oop array, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
        
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        return addr.getDouble();
    }

    public float faloadBarrier(CoreServicesAccess csa, Oop array, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
        
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        return addr.getFloat();
    }

    public int ialoadBarrier(CoreServicesAccess csa, Oop array, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
        
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        return addr.getInt();
    }

    public long laloadBarrier(CoreServicesAccess csa, Oop array, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
        
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        return addr.getLong();
    }

    public short saloadBarrier(CoreServicesAccess csa, Oop array, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
        
        VM_Address addr = addressOfArrayElement(array, index, componentSize, aSrc);
        if (VERIFY_MEMORY_ACCESSES) {
          verifyMemoryAccess(addr);
        }
        return addr.getShort();
    }

    public int acmpneBarrier(CoreServicesAccess csa, Oop v1, Oop v2, int aV1, int aV2)
        throws PragmaNoPollcheck, PragmaInline, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaCAlwaysInline   {

        if (false) { // profiling assertions
          Native.print_string("\n");
          Native.print_int(aV1);
          Native.print_string("!");
          Native.print_int(aV2);
          Native.print_string("\n");
        }

//       return (comparePointers(v1,v2, aV1&(Assert.NULL|Assert.NONNULL), aV2&(Assert.NULL|Assert.NONNULL)) == 0) ? 1 : 0;
        return comparePointers(v1, v2, aV1, aV2) ? 0 : 1;
    }

    public int acmpeqBarrier(CoreServicesAccess csa, Oop v1, Oop v2, int aV1, int aV2)
        throws PragmaNoPollcheck, PragmaInline, PragmaAssertNoSafePoints, PragmaAssertNoExceptions, PragmaCAlwaysInline   {

        if (false) { // profiling assertions
          Native.print_string("\n");
          Native.print_int(aV1);
          Native.print_string("=");
          Native.print_int(aV2);
          Native.print_string("\n");
        }


//       return comparePointers(v1, v2, aV1&(Assert.NULL|Assert.NONNULL|Assert.ADDRESS), aV2&(Assert.NULL|Assert.NONNULL|Assert.ADDRESS));
        return comparePointers(v1, v2, aV1, aV2) ? 1 : 0;
    }
    
    // FIXME: it's enough to return "C int" instead of boolean (nonzero - true, zero - false)
    // but, can't the compiler optimize this anyway ?
    // FIXME: handling of addresses is currently not needed, as it is done in CodeGen (barriers not inserted)
    // so we can assume to have two real object references, potentially null
//    static boolean comparePointers(Oop v1, Oop v2, int aV1, int aV2) throws PragmaAssertNoExceptions, PragmaCAlwaysInline {
    static boolean comparePointers(Oop v1, Oop v2, int aV1, int aV2) throws PragmaAssertNoExceptions {
    
      if (VERIFY_ASSERTIONS) {
        verifyAssertions(v1, aV1);
        verifyAssertions(v2, aV2);
      }
    
      if (IGNORE_ASSERTIONS) {
        aV1 = 0;
        aV2 = 0;
      }

      if ( (aV1&Assert.NULL)!=0 && (aV2&Assert.NULL)!=0 ) {
        return true;  // null == null
      }

      if ( ((aV1&Assert.NONNULL)!=0 && (aV2&Assert.NULL)!=0) ||
         ((aV1&Assert.NULL)!=0 && (aV2&Assert.NONNULL)!=0) ) {
          
        return false; // null != !null
      }
      
      VM_Address a1 = VM_Address.fromObjectNB(v1);
      VM_Address a2 = VM_Address.fromObjectNB(v2);
      
      /*
      if ( (aV1&Assert.ADDRESS)!=0 || (aV2&Assert.ADDRESS)!=0 ) {
        // we actually know here that && would hold as well
        if (VERIFY_ASSERTIONS) {
          assert ( (aV1&Assert.ADDRESS)!=0 && (aV2&Assert.ADDRESS)!=0 );
        }
        return a1.asInt() == a2.asInt();
      }
      */
      
      if ( ((aV1&Assert.NULL)!=0) || ((aV1&Assert.NONNULL)==0 && a1.isNull()) ) { // hopefully this will not turn into the barrier again...
        return a2.isNull();
      }

      if ( ((aV2&Assert.NULL)!=0) || ((aV2&Assert.NONNULL)==0 && a2.isNull()) ) { // hopefully this will not turn into the barrier again...
        return a1.isNull();
      }
    
      // both a1 and a2 are non-null
      
      if ( (aV1&Assert.IMAGEONLY)!=0  || (aV2&Assert.IMAGEONLY)!=0 ) {
        return a1.asInt() == a2.asInt();
      }
      
      // both a1 and an2 are non-null and on the heap (potentially with two copies)
      if (a1.asInt()==a2.asInt()) {
        return true;
      }
      
      a1 = translateNonNullPointer(a1);
      if (a1.asInt()==a2.asInt()) {
        return true;
      }
      
      if (REPLICATING) {
        return false;
      } else {
        a2 = translateNonNullPointer(a2);
        return a1.asInt()==a2.asInt();
      }
      
//      return v1.getHash() - v2.getHash();
// this has drawbacks:
//	hashes can overflow
//	hashes could be (although are not) generated on demand
  
      
    }
    
    public boolean forceBarriers() {
        // this means - ignore PragmaNoBarriers, which should instead be named
        //	PragmaNoScopeChecks and which is still used in OVM code as such
	return true;
    }

    public boolean needsWriteBarrier() {
//	return !noBarrier;
        return FORCE_YUASA_BARRIER || FORCE_DIJKSTRA_BARRIER || FORCE_IMAGE_BARRIER ||
          NEEDS_YUASA_BARRIER || NEEDS_DIJKSTRA_BARRIER || FAST_IMAGE_SCAN || COMPACTION;
          
          // note that even with concurrency disabled and Yuasa, Dijkstra, and Image B. disabled,
          // we still need to make sure that we write to the new location / both locations of an object
          // and that we write there the new value
          
    }
    
    public boolean needsReadBarrier() {
        return VERIFY_COMPACTION || VERIFY_HEAP_INTEGRITY || VERIFY_BUG;
    }

    public boolean needsAcmpBarrier() {
        return COMPACTION || FORCE_TRANSLATING_BARRIER;
    }
    
    
    static int rbincall = 0;
    public void readBarrier(CoreServicesAccess csa,
			    Oop src) throws PragmaNoPollcheck, PragmaNoReadBarriers {
      rbincall++;			    
      
      if (VERIFY_HEAP_INTEGRITY) {
        if (rbincall == 1) {
  	  if (inHeap(src)) {
	    verifyForwardingPointer("readBarrier1",VM_Address.fromObjectNB(src),FWD_ALWAYS);
          }
	}
      }

      if (VERIFY_COMPACTION) {
        if (rbincall==1) {
          verifyForwardingPointer("readBarrier2", VM_Address.fromObjectNB(src),FWD_ALWAYS);	
        }
      }
      
      if (false) {  // this is slow... but it came very handy once
        if (src!=null && src.getBlueprint().isArray()) {
          verifyForwardingPointersInObject(src, src.getBlueprint(), FWD_ALWAYS);
        }
      }
      
      rbincall --;
    }
    
    public static void verifyAssertions(Oop addr, int a) {
      verifyAssertions(VM_Address.fromObjectNB(addr), a);
    }
    
    public static void verifyAssertions(VM_Address addr, int a) {
    
      if (!inHeapWorksNow) {
        return ;
      }
    
      if ( (a&Assert.NULL)!=0 && !addr.isNull() ) {
        Native.print_string("triPizlo: VERIFY_ASSERTIONS: Pointer asserted null is nonnull\n");
        throw Executive.panic("fix");
      }

      if ( (a&Assert.NONNULL)!=0 && addr.isNull() ) {
        Native.print_string("triPizlo: VERIFY_ASSERTIONS: Pointer asserted nonnull is null\n");
        throw Executive.panic("fix");        
      }      
      
      if ( (a&Assert.IMAGEONLY)!=0 && addr.isNonNull() && inHeap(addr) ) {
        Native.print_string("triPizlo: VERIFY_ASSERTIONS: Pointer asserted image only is in heap\n");
        throw Executive.panic("fix");        
      }            

//      if ( (a&Assert.HEAPONLY)!=0 && addr.isNonNull() && !inHeap(addr) ) {
      if ( (a&Assert.HEAPONLY)!=0 && addr.isNonNull() && inImage(addr) ) {
        Native.print_string("triPizlo: VERIFY_ASSERTIONS: Pointer asserted heap only is in image\n");
        throw Executive.panic("Fix..");
      }          
    }

/*
    public boolean needsEagerTranslatingReadBarrier() {
        return false;
    }

    public boolean needsLazyTranslatingReadBarrier() {
      return COMPACTION || FORCE_TRANSLATING_BARRIER;
    }
*/
    public boolean needsBrooksTranslatingBarrier() {
      return ( COMPACTION || FORCE_TRANSLATING_BARRIER ) && BROOKS;
    }

    public boolean needsReplicatingTranslatingBarrier() {
      return ( COMPACTION || FORCE_TRANSLATING_BARRIER ) && REPLICATING;
    }

    public Oop checkingTranslatingReadBarrier(CoreServicesAccess csa,
			    Oop src, int aSrc) throws PragmaNoPollcheck, PragmaInline, 
			      PragmaNoBarriers {

        return translatePointer( src, aSrc );
    }

    public Oop nonCheckingTranslatingReadBarrier(CoreServicesAccess csa,
			    Oop src, int aSrc) throws PragmaNoPollcheck, PragmaInline, 
			      PragmaNoBarriers {
	return translatePointer( src, aSrc|Assert.NONNULL );		      
    }


    public void setReferenceField( Oop object, int offset, Oop src ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      putFieldBarrier( null, object, offset, src, 0, 0 );
    }

    // FIXME: can we rewrite these methods not to make assertions about how individual primitive types are represented ?
    public void setPrimitiveField( Oop object, int offset, boolean value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
    
      VM_Address addr = VM_Address.fromObjectNB(object);
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && BROOKS ) {
        addr = translateNonNullPointer( addr );
      }
      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr.add(offset));
      }      
      addr.add(offset).setInt( value ? 1 : 0 );
      
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
        translateNonNullPointer( addr ).add(offset).setInt( value ? 1 : 0 );
      }
    }
    
    public void setPrimitiveField( Oop object, int offset, byte value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObjectNB(object);
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && BROOKS ) {
        addr = translateNonNullPointer( addr );
      }

      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr.add(offset));
      }

      addr.add(offset).setInt( value );
      
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
        translateNonNullPointer( addr ).add(offset).setInt( value );
      }
    }    

    public void setPrimitiveField( Oop object, int offset, short value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObjectNB(object);
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && BROOKS ) {
        addr = translateNonNullPointer( addr );
      }

      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr.add(offset));
      }

      addr.add(offset).setInt( value );
      
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
        translateNonNullPointer( addr ).add(offset).setInt( value );
      }    
    }

    public void setPrimitiveField( Oop object, int offset, char value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObjectNB(object);
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && BROOKS ) {
        addr = translateNonNullPointer( addr );
      }

      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr.add(offset));
      }

      addr.add(offset).setInt( value );
      
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
        translateNonNullPointer( addr ).add(offset).setInt( value );
      }    
    }

    public void setPrimitiveField( Oop object, int offset, int value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObjectNB(object);
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && BROOKS ) {
        addr = translateNonNullPointer( addr );
      }

      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr.add(offset));
      }

      addr.add(offset).setInt( value );
      
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
        translateNonNullPointer( addr ).add(offset).setInt( value );
      }    
    }

    public void setPrimitiveField( Oop object, int offset, long value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObjectNB(object);
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && BROOKS ) {
        addr = translateNonNullPointer( addr );
      }

      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr.add(offset));
      }

      addr.add(offset).setLong( value );
      
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
        translateNonNullPointer( addr ).add(offset).setLong( value );
      }        
    }
    
    public void setPrimitiveField( Oop object, int offset, float value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObjectNB(object);
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && BROOKS ) {
        addr = translateNonNullPointer( addr );
      }

      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr.add(offset));
      }

      addr.add(offset).setFloat( value );
      
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
        translateNonNullPointer( addr ).add(offset).setFloat( value );
      }            
    }
    
    public void setPrimitiveField( Oop object, int offset, double value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      VM_Address addr = VM_Address.fromObjectNB(object);
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && BROOKS ) {
        addr = translateNonNullPointer( addr );
      }

      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr.add(offset));
      }

      addr.add(offset).setDouble( value );
      
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
        translateNonNullPointer( addr ).add(offset).setDouble( value );
      }                
    }

    public void setPrimitiveArrayElement( Oop object, int index, int value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
    
      if (ARRAYLETS) {
        iastoreBarrier( null, object, index, value, MachineSizes.BYTES_IN_WORD , 0 ); // sync with S3Blueprint.java, Primitive constructor
      } else {
        setPrimitiveArrayElementAtByteOffset( object, ((Blueprint.Array)object.getBlueprint()).byteOffset(index), value );
      }
    }

    public void setPrimitiveArrayElementAtByteOffset( Oop object, int byteOffset , int value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
    
      if (ARRAYLETS) {
        throw Executive.panic("setPrimitiveArrayElementAtByteOffset called with arraylets");
      } else {
        setPrimitiveField( object, byteOffset, value );      
      }
    }        

    public void setPrimitiveArrayElement( Oop object, int index, char value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
    
      if (ARRAYLETS) {
        castoreBarrier( null, object, index, value, 2 , 0 ); // sync with S3Blueprint.java, Primitive constructor
      } else {
        setPrimitiveArrayElementAtByteOffset( object, ((Blueprint.Array)object.getBlueprint()).byteOffset(index), value );
      }
    }

    public void setPrimitiveArrayElementAtByteOffset( Oop object, int byteOffset , char value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      if (ARRAYLETS)   throw Executive.panic("setPrimitiveArrayElementAtByteOffset called with arraylets");

      VM_Address addr = VM_Address.fromObjectNB(object);
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && BROOKS ) {
        addr = translateNonNullPointer( addr );
      }

      if (VERIFY_MEMORY_ACCESSES) {
        verifyMemoryAccess(addr.add(byteOffset));
      }

      addr.add(byteOffset).setChar( value );
      
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING) {
        translateNonNullPointer( addr ).add(byteOffset).setChar( value );
      }    
    }        

    public void setReferenceArrayElement( Oop object, int index, Oop value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      if (ARRAYLETS) {
        // FIXME: could add constant for component size
        aastoreBarrier( null,  object, index, value, MachineSizes.BYTES_IN_ADDRESS , 0, 0 );
      } else {
        super.setReferenceArrayElement( object, index, value );
      }
    }

    public void setReferenceArrayElementAtByteOffset( Oop object, int byteOffset , Oop value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {

      if (ARRAYLETS) {
        throw Executive.panic("setReferenceArrayElementAtByteOffset called with arraylets");
      }
      setReferenceField( object, byteOffset, value );      
    }        

    public char getCharArrayElement( Oop array, int index )  throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      if (ARRAYLETS) {
        return caloadBarrier(null, array, index, 2, 0);
      } else {
        return super.getCharArrayElement( array, index );
      }
    }

    public Oop getReferenceArrayElement( Oop array, int index )  throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      if (ARRAYLETS) {
        return aaloadBarrier(null, array, index, MachineSizes.BYTES_IN_ADDRESS, 0);
      } else {
        return super.getReferenceArrayElement( array, index );
      }
    }

    static void printBlueprint(VM_Address addr) {

      if (addr==null) {
        Native.print_string("null pointer");
        return;
      }    
      Blueprint bp = addr.asOopUnchecked().getBlueprint();
      printBlueprint(bp);
    }
    
    static int inPrintBlueprint = 0;
    static void printBlueprint(Blueprint bp) {
    
      inPrintBlueprint++; // recursion caused by the array access below - this should all be native code
      
      if (inPrintBlueprint!=1) {
        return ;
      }
      if (bp!=null) {
        byte[] str=bp.get_dbg_string();
        Native.print_bytearr_len(str,str.length);
      } else {
        Native.print_string("null blueprint");
      }
      
      inPrintBlueprint--;
    }
    
    public void printAddress(VM_Address addr) {
      printAddr(addr);
    }

    static int recCount = 0;
    
    static void printAddr(VM_Address addr) {
      if (COMPACTION && addr!=null) {
        if (addr.add(forwardOffset).getAddress() != addr) {
          Native.print_string(" forwarded pointer [");
          printAddrReal(addr);
          Native.print_string("] ==>> [");
          printAddrReal(addr.add(forwardOffset).getAddress());
          Native.print_string("] ");
          return;
        }
      } 
      printAddrReal(addr);
    }

    static void printAddrReal(VM_Address addr) {
    
      recCount ++;
      
      Native.print_ptr(addr);
      
      if (inHeap(addr)) {
        Native.print_string(" (heap) ");
      } else if (inImage(addr)) {
        Native.print_string(" (image) ");
      } else if (addr.isNonNull()) {
        Native.print_string(" (?point-away?) ");
      }
      
      if (addr.isNonNull()) {
        int color = gcData.getColor(addr);
        Native.print_string(colorName(color));
        if (REPLICATING) {
          Native.print_string( gcData.getOld(addr) == 0 ? " not-old " : " old " );
        }
        
        if (KEEP_OLD_BLUEPRINT && color==FREE) {
          VM_Address old = freeList.getCustom(addr);
          
          if (old!=null) {
            Blueprint oldBP = old.asOopUnchecked().getBlueprint();
            int oldBits = old.asInt() & 7;
          
            Native.print_string(" (FREE object was before of type ");
            printBlueprint(oldBP);
            Native.print_string(" with bits ");
            Native.print_int(oldBits);
            Native.print_string(") ");
          }
        }
      }
      
      if (inHeap(addr)) {
        int bIdx = blockIdx(addr);

        Native.print_string(" blockIdx=");
        Native.print_int(bIdx);
        Native.print_string(" ");
        
        if (!Bits.getBit(usedBits, bIdx)) {
          Native.print_string("(UNUSED)");
        } else {
          Native.print_string("(USED)");
                
          if (Bits.getBit(largeBits,bIdx)) {
            Native.print_string("(LARGE)");
          } else {
            Native.print_string("(SMALL)");
            
            int size=sizes[bIdx];
            Native.print_string(" sizeClassSize=");
            Native.print_int(size);
            
            Native.print_string(" inBlockOffset=");
            VM_Address blockBase=memory.add(bIdx*blockSize);
            int offset=addr.diff(blockBase).asInt();
            Native.print_int(offset);
          }
        }
        
    
        if (VERIFY_BUG) {
          Native.print_string(" finishedSweeps= ");
          Native.print_int(finishedSweeps);
          Native.print_string(" finishedCompactions= ");
          Native.print_int(finishedCompactions);
        }        

        if (ARRAYLETS && Bits.getBit(arrayletBits,bIdx)) {
          Native.print_string("(ARRAYLET)");
        }
        
        /*
        this can cause problems - probably if the bad object is pinned
        */
        
        if (recCount==1) {
          if (isPinnedScannedKeepAlive(addr)) {
            Native.print_string("(PINNED)");
          } else {
            Native.print_string("(NOT-PINNED)");
          }
        }
      }
      
      if (addr!=null) {
        int hash = addr.asOopUnchecked().getHash();
        Native.print_string(" hash=");
        Native.print_int(hash);
        Native.print_string("\n");
      }
      
      if (false) {
        if (recCount!=1) {
          Native.print_string("[RECURSIVE]");
        } else {
          Blueprint bp = addr.asOopUnchecked().getBlueprint();
          if (bp!=null) {
	    byte[] str=bp.get_dbg_string();
	    Native.print_string("[");
	    Native.print_bytearr_len(str,str.length);
	    Native.print_string("]");
          } else {
            Native.print_string("[NULL-BP]");
          }
        }
      }
      
      recCount--;        
    }

    // follow the forwarding pointer (both Brooks and replicating, though in each
    // it has different meaning) ; to get ultimately a new copy, use updatePointer instead
    
    static Oop translatePointer( Oop src, int aSrc ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {
      
        if (VERIFY_ASSERTIONS) {
          verifyAssertions(VM_Address.fromObjectNB(src), aSrc);
        }

        if (IGNORE_ASSERTIONS) {
          aSrc = 0;
        }
        
        if (COMPACTION || FORCE_TRANSLATING_BARRIER) {
          if ( (aSrc&Assert.NULL)!=0 || (aSrc&Assert.IMAGEONLY)!=0 ) {
            return src;
          }

          if ( !((aSrc&Assert.NONNULL)!=0) && src==null ) {
            return src;
          }
        
          MovingGC oop = (MovingGC)src.asAnyOop();

          // there will be a few false alarms before the manager inits
          // (the native image base address, ...)
          if (VERIFY_COMPACTION && (!REPLICATING || !VERIFY_REPLICAS_SYNC) ) { // in replicating, this would also be called in the middle of store barrier,
                                                   // in replicating, this would also be called in the middle of store barrier,
                                                   // when old and new copy are not in sync, but it is ok in such case
            verifyForwardingPointer(VM_Address.fromObjectNB(src), FWD_ALWAYS);
//                if (VM_Address.fromObjectNB(src).add(forwardOffset).getAddress().isNull()) {
//                  throw Executive.panic("translate pointer - forwarded to null");
//                }
          }
          
          Oop result = oop.getForwardAddress().asOopUnchecked();          
          if (VERIFY_BUG) {
            readForwardingPointer(VM_Address.fromObjectNB(oop));
          }
          return result;
        } else {
          return src;
        }
        
        
    }
    
    static VM_Address readForwardingPointer( VM_Address addr ) throws  PragmaNoPollcheck, 
      PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {
      
        if (VERIFY_BUG) {
          if (addr.isNull()) {
            throw Executive.panic("reading forwarding pointer of a null object");
          }
        }      
      
        VM_Address fwdAddr = addr.add(forwardOffset).getAddress();
        
        if (VERIFY_BUG) {
          if (fwdAddr.isNull()) {
            
            Native.print_string("ERROR: forwarded to null, pointer of type ");
            printBlueprint(addr);
            Native.print_string(" address ");
            printAddr(addr);
            Native.print_string(" referenced from ");
            findReferees(addr);
            Native.print_string("\n");
            Native.print_string("ERROR: !!! RETURNING NULL POINTER");
            //throw Executive.panic("read forwarding pointer which is null");
            return fwdAddr;
          }
          
          if (fwdAddr != addr) {
            if (!inHeap(fwdAddr)) {
              printAddr(addr);
              Native.print_string("->");
              printAddr(fwdAddr);
              Native.print_string("\n");
              throw Executive.panic("old location of forwarded object is not in heap");
            }
            
            if (!inHeap(addr)) {
              printAddr(addr);
              Native.print_string("->");
              printAddr(fwdAddr);
              Native.print_string("\n");            
              throw Executive.panic("new location of forwarded object is not in heap");
            }
          }
        }

        if (VERIFY_BUG&&false) {
          verifyForwardingPointer( "readForwardingPointer", addr, FWD_ALWAYS );         
        }
        
        return fwdAddr;
    }

    static VM_Address translatePointer( VM_Address src, int aSrc ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {

        return VM_Address.fromObjectNB( translatePointer( src.asOopUnchecked(), aSrc ) );
    }
    
    static VM_Address translateNonNullPointer( VM_Address src, int aSrc ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {

          return translatePointer( src, aSrc|Assert.NONNULL);
    }

    static VM_Address translateNonNullPointer( VM_Address src ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {

          return translatePointer( src, Assert.NONNULL);
    }

    static VM_Address translatePointer( VM_Address src )
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {

          return translatePointer( src, 0 );
    }


    // get always the new copy of an object (no matter if replicating or Brooks)
    static VM_Address updatePointer( VM_Address src, int aSrc ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {
      
        if (VERIFY_ASSERTIONS) {
          verifyAssertions(src, aSrc);
        }

        if (IGNORE_ASSERTIONS) {
          aSrc = 0;
        }
        
        if (BROOKS) {
          return translatePointer( src, aSrc );
        }
      
        if (
          (!COMPACTION && !FORCE_TRANSLATING_BARRIER)  ||  /* we don't want the barrier ever */
          (aSrc&Assert.NULL)!=0 ||   /* the address is known to be null */
          (aSrc&Assert.IMAGEONLY)!=0  || /* the object is known not to move */
          ( !((aSrc&Assert.NONNULL)!=0) && src==null )  || /* the object is null */
          (gcData.getOld(src)==0)  /* we have already the new location of the object */
        ) {
            return src;
        } else {

            if (VERIFY_COMPACTION && ( !REPLICATING || !VERIFY_REPLICAS_SYNC) ) { // in replicating, this would also be called in the middle of store barrier,
                                                   // when old and new copy are not in sync, but it is ok in such case
              verifyForwardingPointer(src, FWD_ALWAYS);
//                if (src.add(forwardOffset).getAddress().isNull()) {
//                  throw Executive.panic("update pointer - forwarded to null");
//                }
            }        
            return readForwardingPointer(src);
        }
    }

    static VM_Address updatePointer( VM_Address src ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {

      return updatePointer( src, 0 );      
    }


    static void updatePointerAtAddress( VM_Address srcPtr, int aSrc ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {
      
        if (VERIFY_ASSERTIONS) {
          verifyAssertions(srcPtr.getAddress(), aSrc);
        }

        if (IGNORE_ASSERTIONS) {
          aSrc = 0;
        }

        if (
          (!COMPACTION && !FORCE_TRANSLATING_BARRIER)  ||  /* we don't want the barrier ever */
          (aSrc&Assert.NULL)!=0 ||   /* the address is known to be null */
          (aSrc&Assert.IMAGEONLY)!=0   /* the object is known not to move */
        ) {
          return ;
        }
        
        VM_Address src = srcPtr.getAddress();
        
        if ( !((aSrc&Assert.NONNULL)!=0) && src==null ) { /* the object in null */
          return ;
        }
        
        if (REPLICATING && gcData.getOld(src)==0)  { /* we have already the new location of the object */
          return ;
        } else {
            srcPtr.setAddress( translateNonNullPointer(src, aSrc) );
        }
    }

    static void updatePointerAtAddress( VM_Address srcPtr ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {
      
        updatePointerAtAddress( srcPtr, 0 );
    }


    static VM_Address updateNonNullPointer( VM_Address src, int aSrc ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {

          return updatePointer( src, aSrc|Assert.NONNULL);
    }

    static VM_Address updateNonNullPointer( VM_Address src ) 
      throws PragmaNoPollcheck, PragmaInline, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaNoReadBarriers  {

          return updatePointer( src, Assert.NONNULL);
    }

        
    public VM_Area getHeapArea() {
	return heap;
    }
    
    public int allocateInImage(int firstFree,
			       int size,
			       Blueprint bp,
			       int arrayLength) throws BCdead {
	if (intArray==null) {
	    initLate();
	}
	if (!lastBig
	    && ((firstFree + size)/imgPageSize) == (firstFree/imgPageSize))
	    return firstFree;
	else if (size <= imgPageSize) {
	    lastBig = false;
	    return ((firstFree + imgPageSize - 1) & ~(imgPageSize - 1));
	} else {
	    lastBig = true;
	    int ret = (firstFree + imgPageSize - 1) & ~(imgPageSize - 1);
/*	    params.println("\t{ 0x" + Integer.toHexString(ret) +
			     ", 0x" + Integer.toHexString(ret+size) + "}, \\");
*/			     
	    return ret;
	}
    }
    
    public void garbageCollect() {}

    public boolean shouldPinCrazily() { return false; }

    // this set is maintained solely to make sure that pinned objects are reachable
    // I am not sure if this hashset implementation is safe
    // (i.e. would not call things that could call pin, would not pollcheck, would not
    //  end up in a recursive call when used for monitors, ...)
    // however, we want a mean of knowing if an object is pinned or not
    // with the hashset, tests seem to work, but it crashes when
    // printAddr called from debugger tries to check if and object is pinned (why ?)
    // interestingly, the list implementation does not crash
//    private HashSet pinnedObjects = new HashSet(10);

    private class OopSet {
    
      private class Entry {
        Oop obj;
        Entry next;
        
        Entry(Oop obj, Entry next) {
          this.obj = obj;
          this.next = next;
        }
      }
      
      Entry head = null;
      
      boolean add(Oop o) {
        if (!contains(o)) {
          head = new Entry(o, head);
          return true;
        } else {
          return false;
        }
      }

      boolean contains(Oop o) {
        
        for(Entry e = head; e!=null; e=e.next) {
          if (e.obj == o) {
            return true;
          }
        }      
        return false;
      }
      
      boolean remove(Oop o) {
        if (head==null) {
          return false;
        }
        
        Entry e = head;
        Entry prev = null;
        do {
         if (e.obj == o) {
           if (prev != null) {
             prev.next = e.next;
           } else {
             head = e.next;
           }
           // we can remove this to make it more "worst-case"ish
           return true;
         }
         prev = e;
         e = e.next;
       } while ( e!=null );       

       return false;      
      } 
    }

    public OopSet pinnedObjects = new OopSet();
    
    static boolean isPinnedScannedKeepAlive(VM_Address ptr) {
        Oop obj = ptr.asOop();
        return ((TheMan)MemoryManager.the()).pinnedObjects.contains(obj);
    }
 
    private void incPinnedCountFor(Oop oop) throws PragmaInline {
    
      int bIdx = blockIdx(VM_Address.fromObjectNB(oop));

      if (Bits.getBit(largeBits,bIdx)) {
        return ;
      }
          
      if (nPinnedObjects[bIdx]==0) {
        int size = sizes[bIdx];
        SizeClass sc = sizeClassBySize[size/alignment];
        sc.nBlocksPinned++;
      }
      
      nPinnedObjects[bIdx]++;
    }
    
    private void decPinnedCountFor(Oop oop) throws PragmaInline {
    
      int bIdx = blockIdx(VM_Address.fromObject(oop));
          
      if (Bits.getBit(largeBits,bIdx)) {
        return ;
      }
      
      if (nPinnedObjects[bIdx]==1) {
        int size = sizes[bIdx];
        SizeClass sc = sizeClassBySize[size/alignment];
        sc.nBlocksPinned--;
      }
      nPinnedObjects[bIdx]--;
    }

    // this prevents an object from being moved
    // the object is still scanned, kept alive, and pointers in it updated
    
    public void pinScannedKeepLive(Oop oop) {
      pinScannedKeepLive(oop, false);
    }

    public void pinScannedKeepLiveNewLocation(Oop oop) {
      pinScannedKeepLive(oop, true);
    }
    
    public void pinScannedKeepLive(Oop oop, boolean newLocation) throws PragmaAtomicNoYield {
	if (DEBUG_PIN && debugPin) {
	    Native.print_string("triPizlo: PIN: pinning ");
	    Native.print_ptr(VM_Address.fromObject(oop));
	    Native.print_string("\n");
	}

//	if (OVMBase.isBuildTime() || !inHeap(oop)) {
        if (OVMBase.isBuildTime()) { // FIXME: does this get optimized out during build ?
	    // For side effect at build time
	    VM_Address.fromObject(oop).asInt();
	    if (DEBUG_PIN && debugPin) {
		Native.print_string("triPizlo: PIN: skipping, not in heap.\n");
	    }
	    return;
	}
	
	if (VERIFY_PINNING) {
  	  GCData goop=(GCData)oop.asAnyOop();
	
  	  if (goop.getColor()!=WHITE &&
	    goop.getColor()!=GREYBLACK) {
	    Native.print_string("triPizlo: PIN: object has GC bits ");
	    Native.print_int(goop.getColor());
	    Native.print_string("\n");
	    throw Executive.panic("Attempt to pin an object that is neither "+
				  "white, grey, or black.");
          }
        }
	
	if ( (VERIFY_COMPACTION || true) && newLocation) {
	  VM_Address addr = VM_Address.fromObjectNB(oop);
	  VM_Address addrFwd = readForwardingPointer(addr);
	  if (addr!=addrFwd) {
	    Native.print_string("triPizlo: PIN: (potential) ERROR: pinning - pinScannedKeepAlive an object that has been moved.\n");
	  }
	}
	
	// FIXME: add another bit to object header instead ? this is slow...
	if (inHeap(oop)) {
  	  boolean added = pinnedObjects.add(oop);
	
  	  if (DEBUG_PIN && debugPin && !added) {
	    Native.print_string("triPizlo: PIN: failed to add object to pinnedObjects HashSet. Already pinned ?\n");
          }
	
          if (added) {
            incPinnedCountFor( oop );
          }
        }

    }
    
    public void unpinScannedKeepLive(Oop oop) throws PragmaAtomicNoYield {
	if (DEBUG_PIN && debugPin) {
	    Native.print_string("triPizlo: PIN: unpinning ");
	    Native.print_ptr(VM_Address.fromObject(oop));
	    Native.print_string("\n");
	}
	if (OVMBase.isBuildTime() || !inHeap(oop)) {
	    if (DEBUG_PIN && debugPin) {
		Native.print_string("triPizlo: PIN: skipping, not in heap.\n");
	    }
	    return;
	}
	
	// any side effects here ?
	//GCBits goop=(GCBits)oop.asAnyOop(); // read barriers are ok
	
	if (inHeap(oop)) {
  	  boolean removed = pinnedObjects.remove(oop);

  	  if (DEBUG_PIN && debugPin && !removed) {
	    Native.print_string("triPIzlo: PIN: failed to remove object from pinnedObjects HashSet. Already unpinned ?\n");  
          }
          
          if (removed) {
            // FIXME:
            // the object could have moved before being pinned
            // however, if that happened, we're screwed anyway, 
            // so change this to non-translating conversion...
            
            decPinnedCountFor(oop);
          }
        }
    }
    
    
    // !!! the object will not be scanned
    // !!! the object will not be made reachable
    // !!! beware that in general the object can move before this call, but if it does, we are screwed
    // (well, or the caller must know exactly what is he doing - like call on new location of the object
    // and be aware that mutator can in general have pointers to the old copy as well)
    
    // !! well, it probably can't be used correctly if the object can move before being pinned ; we should have a special
    // call "allocatePinned" instead

    // note that since the object is not scanned, pointers it may be holding are not updated ; if those objects move, after next sweep,
    // we are screwed
    // note that this applies also to pointers in object header ; monitors should still be updated, because they are treated specially, but
    // memory areas won't be fixed.. well, as currently we don't really use scopes, the only scope is allocated in image, so it should be ok
    
    // maybe we shouldn't use this at all
    
    // newLocation == true - the caller knows that the object can have two replicas and
    //   only needs to pin the newer location of the object ; it must call this on the new location
    
    public void pin(Oop oop) {
      pin(oop, false);
    }
        
    public void pinNewLocation(Oop oop) {
      pin(oop, true);
    }
    
    public void pin(Oop oop, boolean newLocation) throws PragmaAtomicNoYield {
    
        if (SLOW_PINNING) {
          pinScannedKeepLive(oop);
          return ;
        }
        
	if (DEBUG_PIN && debugPin) {
	    Native.print_string("triPizlo: PIN: pinning ");
	    Native.print_ptr(VM_Address.fromObject(oop));
	    Native.print_string("\n");
	}
	
	if (DEBUG_PIN && debugPin && !OVMBase.isBuildTime() && !inHeap(oop)) {
		Native.print_string("triPizlo: PIN: skipping, not in heap.\n");
        }
	
	if (OVMBase.isBuildTime()) {
	    // For side effect
	    VM_Address.fromObject(oop).asInt();
	    return;
	}

        GCData goop=(GCData)oop.asAnyOop();	

	if (VERIFY_PINNING) {
  	  if (goop.getColor()!=WHITE &&
	    goop.getColor()!=GREYBLACK) {
	    Native.print_string("triPizlo: PIN: object has GC bits ");
	    Native.print_int(goop.getColor());
	    Native.print_string("\n");
	    throw Executive.panic("Attempt to pin an object that is neither "+
				  "white, grey, or black.");
          }
        }

	if ( !newLocation && COMPACTION && (VERIFY_PINNING || VERIFY_COMPACTION || true)) {
	  VM_Address addr = VM_Address.fromObjectNB(oop);
	  VM_Address addrFwd = readForwardingPointer(addr);
	  if (addr!=addrFwd) {
	    throw Executive.panic("triPizlo: PIN: ERROR: pinning an object that has been moved.\n");
	  }
	  
	  if (COMPACTION && (VERIFY_PINNING || VERIFY_COMPACTION)) {
  	    Blueprint bp = oop.getBlueprint();
            if (bp.isArray()) {
              if (bp.asArray().getComponentBlueprint().isReference()) {
                throw Executive.panic("triPizlo: PIN: ERROR: pinning a reference array.\n");
              }
            } else {
              if (bp.getRefMap().length > 0) {
                throw Executive.panic("triPizlo: PIN: ERROR: pinning an object that contains references.\n");
              }
            }
          }
	}
        
	goop.setColor(FRESH);
	
	incPinnedCountFor(oop);
    }

    public void unpin(Oop oop) throws PragmaAtomicNoYield {

        if (SLOW_PINNING) {
          unpinScannedKeepLive(oop);
          return ;
        }

	if (DEBUG_PIN && debugPin) {
	    Native.print_string("triPizlo: PIN: unpinning ");
	    Native.print_ptr(VM_Address.fromObject(oop));
	    Native.print_string("\n");
	}

	if (DEBUG_PIN && debugPin && !OVMBase.isBuildTime() && !inHeap(oop)) {
		Native.print_string("triPizlo: PIN: skipping, not in heap.\n");
        }
	
	if (OVMBase.isBuildTime()) {
	    // For side effect
	    VM_Address.fromObject(oop).asInt();
	    return;
	}

	GCData goop=(GCData)oop.asAnyOop();
	
	if (VERIFY_PINNING) {
  	  if (goop.getColor()!=FRESH) {
	    throw Executive.panic("Attempt to unpin an object that is not "+
				  "pinned.");
          }
        }
	stampGCBits(goop);
	
	decPinnedCountFor(oop);
    }

    
    // FIXME!!! ?
    public boolean isPrecise() { return true; }
    
    
    static void verifyArraySubsets(Oop a, Oop b, int aOffset, int bOffset, int nElems, String msg) {
      verifyArraySubsets(a, b, aOffset, bOffset, nElems, msg, false);
    }
    static void verifyArraySubsets(Oop a, Oop b, int aOffset, int bOffset, int nElems, String msg, boolean dump) {
    
        enterExcludedBlock();
      
        Blueprint.Array abp = (Blueprint.Array)a.getBlueprint();
        Blueprint.Array bbp = (Blueprint.Array)b.getBlueprint();
        int csize = abp.getComponentSize();
        
        if (csize != bbp.getComponentSize()) {
          Native.print_string("triPizlo: ERROR: verifyArraySubsets: component sizes differ\n");
          throw Executive.panic("fix");
        }
        
        boolean isRef = abp.getComponentBlueprint().isReference();
      
        if (dump && ARRAYLETS) {
          Native.print_string("\nFirst arraylets: ");
          Native.print_ptr(getArraylet(VM_Address.fromObject(a), abp, 0));
          Native.print_string(" ");
          Native.print_ptr(getArraylet(VM_Address.fromObject(b), abp, 0));
          Native.print_string("\n");
        }
        for(int e=0; e<nElems; e++) {
          VM_Address aAddress = addressOfArrayElement(VM_Address.fromObject(a), aOffset, csize, 0);
          VM_Address bAddress = addressOfArrayElement(VM_Address.fromObject(b), bOffset, csize, 0);
          
          if (isRef) {
            if (dump) {
              Native.print_string("\nElement ");
              Native.print_int(e);
              Native.print_string(": ");
              Native.print_ptr(aAddress.getAddress());
              Native.print_string("(");
              Native.print_int(aOffset);
              Native.print_string(")");
              Native.print_string(" ");
              Native.print_ptr(bAddress.getAddress());
              Native.print_string("(");
              Native.print_int(bOffset);
              Native.print_string(")");              
            }
            
            if (!comparePointers(aAddress.getAddress().asOop(), bAddress.getAddress().asOop(),0,0)) {
              if (!dump) {
                verifyArraySubsets(a, b, aOffset-e, bOffset-e, nElems, msg, true);
                Native.print_string("verifyArraySubsets: reference arrays differ: ");
                Native.print_string(msg);
                Native.print_string("\n");
                throw Executive.panic("verifyArraySubsets: reference arrays differ");                

              } else {
                Native.print_string(" (ERROR)\n");
              }
            } else {
              if (dump) {
                Native.print_string(" (ok)\n");
              }
            }
            verifyForwardingPointer(aAddress.getAddress(),FWD_ALWAYS);
            verifyForwardingPointer(bAddress.getAddress(),FWD_ALWAYS);

          } else {
            for(int bo=0; bo<csize; bo++) {
            
              if (dump) {
                Native.print_string("\nElement ");
                Native.print_int(e);
                Native.print_string(" byte ");
                Native.print_int(bo);
                Native.print_string(": ");
                Native.print_ptr(aAddress.getAddress());
                Native.print_string(" ");
                Native.print_ptr(bAddress.getAddress());
              }
              
              if (aAddress.add(bo).getByte() != bAddress.add(bo).getByte()) {

                if (!dump) {
                  verifyArraySubsets(a, b, aOffset-e, bOffset-e, nElems, msg, true);
                  Native.print_string("verifyArraySubsets: primitive arrays differ: ");
                  Native.print_string(msg);
                  Native.print_string("\n");
                  throw Executive.panic("verifyArraySubsets: primitive arrays differ");
                } else {
                  Native.print_string(" (ERROR)\n");
                }               
              } else {
                if (dump) {
                  Native.print_string(" (ok)\n");
                }
              }
            }
          }
          
          aOffset ++;
          bOffset ++;
        }
        
        leaveExcludedBlock();
    }
    
    static void updateAndMarkPointers(VM_Address objPtr, int offset, int count) 
      throws PragmaNoPollcheck, PragmaInline {
    
      if (!NEEDS_YUASA_BARRIER && !FORCE_YUASA_BARRIER)  {
        return ;
      }
      
      if (!marking) {
        return;
      }
      
      VM_Address ptr = objPtr.add(offset);
      for(int o=0; o<count ; o++) {
        updateAndMark(ptr.getAddress());
        ptr = ptr.add(MachineSizes.BYTES_IN_ADDRESS);
      }
    }

    static void updatePointers(VM_Address objPtr, int offset, int count) 
      throws PragmaNoPollcheck, PragmaInline {

      // assumes to be called from array copy functions
      // it is also caled from clone
      if (  !(COMPACTION || FORCE_TRANSLATING_BARRIER)  || !updatingPointersWrittenToHeapNeeded ) {
        return;
      }
      
      VM_Address ptr = objPtr.add(offset);
      for(int o=0;o<count;o++) { // note that we know here the is no ongoing incremental object copy
        
        // ptr.setAddress( ptr.getAddress().add(forwardOffset).getAddress() ); -- ?!?! no nullcheck
        updatePointerAtAddress(ptr);
        ptr = ptr.add(MachineSizes.BYTES_IN_ADDRESS);
      }
    }
    
    // ?needed?
/*    
    static void updateOrMarkPointersWithArraylets(VM_Address arrayPtr, int offset, int count, boolean update) 
      throws PragmaNoPollcheck, PragmaInline {

      // assumes to be called from array copy functions
      if (  (!COMPACTION && !FORCE_TRANSLATING_BARRIER && update)  ||
        (!NEEDS_DIJKSTRA_BARRIER && !FORCE_DIJKSTRA_BARRIER && !update) || !marking || count==0 ) {
        return;
      }
      
      int firstArraylet = offset/arrayletSize;
      
      VM_Address aptr = arrayPtr.add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD +
        firstArraylet * MachineSizes.BYTES_IN_ADDRESS);
        
      int innerOffset =  offset % arrayletSize;
      VM_Address ptr = aptr.getAddress().add( innerOffset );
        
      for(;;) {
      
        while ( innerOffset < arrayletSize ) {
          if (update) {
            updatePointerAtAddress(ptr);
          } else {
            mark(ptr.getAddress());
          }
          count --;
          if (count==0) {
            return;
          }
          innerOffset += MachineSizes.BYTES_IN_ADDRESS;
          ptr = ptr.add(MachineSizes.BYTES_IN_ADDRESS);
        }  
        
        aptr = aptr.add(MachineSizes.BYTES_IN_ADDRESS);
        ptr = aptr.getAddress();
      }
    }
    
    //?needed
    static void markPointersWithArraylets(VM_Address arrayPtr, int offset, int count) 
      throws PragmaNoPollcheck, PragmaInline {

      updateOrMarkPointersWithArraylets( arrayPtr, offset, count, false);
    }

    //?needed
    static void updatePointersWithArraylets(VM_Address arrayPtr, int offset, int count) 
      throws PragmaNoPollcheck, PragmaInline {

      updateOrMarkPointersWithArraylets( arrayPtr, offset, count, true);
    }
    
*/
    public final void copyOverlapping(Oop array, int fromOffset, int toOffset, int nElems) {

      if (VERIFY_ARRAYCOPY) {
        arrayCopyBoundsCheck( array, fromOffset, nElems );
        arrayCopyBoundsCheck( array, toOffset, nElems );
      }

      if (!COMPACTION && !FORCE_TRANSLATING_BARRIER && !FORCE_YUASA_BARRIER && !NEEDS_YUASA_BARRIER &&
        !FORCE_IMAGE_BARRIER && !NEEDS_IMAGE_BARRIER && !ARRAYLETS) {
      
	Mem.the().copyOverlapping(array, fromOffset, toOffset, nElems);
        return;
      }

      if (VERIFY_COMPACTION) {
        verifyForwardingPointer(VM_Address.fromObjectNB(array),FWD_ALWAYS);
      }
	
      // more checks are expected to be done by the caller			  
      Blueprint.Array bp = (Blueprint.Array)array.getBlueprint();
      
      if (PARANOID_ARRAYCOPY_MARKING && !ARRAYLETS && marking) {
        if (bp.getComponentBlueprint().isReference() ) {
          enterExcludedBlock();
          
          updateAndMarkPointers(VM_Address.fromObject(array), bp.byteOffset(toOffset), nElems);

          leaveExcludedBlock();
        }
      }

      if (!ARRAYLETS && !( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING ) ) {
        if (!bp.getComponentBlueprint().isReference() ) {
          Mem.the().copyOverlapping(array, fromOffset, toOffset, nElems);          
          return;
        }
      }

      VM_Address addr = VM_Address.fromObjectNB(array);  
      
      Oop origArray = null;
      if (VERIFY_ARRAYCOPY) {
        enterExcludedBlock();
        origArray = clone(array);
        leaveExcludedBlock();
      }

      if (ARRAYLETS) {
        boolean fromStart = toOffset <= fromOffset;
      
        if (bp.getComponentBlueprint().isReference()) {
          imageStoreBarrier( addr, 0 );

          if (false) {
            Native.print_string("Dumping array and its clone before copy\n");
            verifyArraySubsets(array, origArray, 0, 0, bp.getLength(array), "dump with clone check only",true);
            Native.print_string("Doing the copy\n");
          }
          
          copyOverlappingWithArraylets(array, fromOffset, toOffset, bp, nElems, true, fromStart);
          
          if (false) {
            Native.print_string("Dumping array after copy\n");
            verifyArraySubsets(array, array, 0, 0, bp.getLength(array), "dump only",true);
            Native.print_string("Dumping array clone after copy\n");
            verifyArraySubsets(origArray, origArray, 0, 0, bp.getLength(origArray), "dump only",true);          
            Native.print_string("Copy done.\n");
          }
        } else {
          copyOverlappingWithArraylets(array, fromOffset, toOffset, bp, nElems, false, fromStart);        
        }
      }
      
      if (!ARRAYLETS) {
      
//      if ( (COMPACTION || FORCE_DIJKSTRA_BARRIER || FORCE_YUASA_BARRIER) && REPLICATING && inHeap(addr)) { // FIXME: assertions would help here
        if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING && inHeap(addr)) { // FIXME: assertions would help here

          int blockIdx=blockIdx(addr);
          if (Bits.getBit(largeBits,blockIdx)) {
            if (bp.getComponentBlueprint().isReference()) {        
              copyOverlappingSingleCopy( array, fromOffset, bp, toOffset, nElems );
            } else {
              Mem.the().copyOverlapping(array, fromOffset, toOffset, nElems);          
              return ;
            }
          } else {
            if (bp.getComponentBlueprint().isReference()) {
              copyOverlappingTwoCopies( array, fromOffset, bp, toOffset, nElems );            
            } else {
              copyOverlappingBytesTwoCopies( array, fromOffset, bp, toOffset, nElems );
            }
          }
        } else { // in image or 
               // brooks compaction/forced barriers
          if ( ( COMPACTION || FORCE_TRANSLATING_BARRIER ) && REPLICATING ) {
            // now we know we are in the image, no arraylets
        
            if (bp.getComponentBlueprint().isReference()) {
              imageStoreBarrier(addr, Assert.IMAGEONLY); // we now know it's in the image        
              copyOverlappingSingleCopy( array, fromOffset, bp, toOffset, nElems );            
            } else {
              // no image barrier for primitives
              Mem.the().copyOverlapping(array, fromOffset, toOffset, nElems);                      
              return;              
            }
          } else {
            // BROOKS style barrier, or FORCE_YUASA_BARRIER
            // we know that we have a reference array

            imageStoreBarrier(addr, 0);
            copyOverlappingSingleCopy( array, fromOffset, bp, toOffset, nElems );
          }
        }
      } // !arraylets
      
      if (VERIFY_ARRAYCOPY) {
        enterExcludedBlock();
        
        verifyArraySubsets(array, origArray, toOffset, fromOffset, nElems, "after copyOverlapping");
        int low = min(toOffset,fromOffset)-1;  // last element that should be identical
        int high = max(toOffset+nElems-1, fromOffset+nElems-1)+1; // first element that should be identical
        
        if (low>=0) {
          verifyArraySubsets(array, origArray, 0, 0, low+1, "after copyOverlapping, stable prefix");
        }
        
        int len = bp.getLength(array);
        if (high<len) {
          verifyArraySubsets(array, origArray, high, high, len-high, "after copyOverlapping, stable suffix");
        }
        
        leaveExcludedBlock();
      }   
    }
    

    // FIXME: merge this code with the code handling reference handling
    // abuse inlining to make sure it's not slower
    public void copyOverlappingBytesTwoCopies(Oop array, int fromOffset, Blueprint.Array bp, int toOffset, int nElems) 
      throws PragmaAssertNoExceptions {
    
      Blueprint.Array arrayBP = (Blueprint.Array)array.getBlueprint();
      fromOffset = arrayBP.byteOffset(fromOffset);
      toOffset = arrayBP.byteOffset(toOffset);
      
      nElems = nElems * arrayBP.getComponentSize();
      
      VM_Address addrOther = null;
      VM_Address addr = null;

      // FIXME: maybe remove this... 
      // it saves some branch sometimes, but adds it other times
      /*
      if (nElems < maxAtomicBytesCopyHalf) { 
        addr = VM_Address.fromObjectNB(array);
        Nat.memmove(addr.add(toOffset), addr.add(fromOffset), nElems);
        addrOther = translateNonNullPointer(addr);
        if (addr != addrOther) {
          Nat.memcpy(addrOther.add(toOffset), addr.add(toOffset), nElems);
        }
        return ;
      }
      */
      if (toOffset <= fromOffset) {
      
        while(nElems > maxAtomicBytesCopyHalf) {
        
          addr = VM_Address.fromObjectNB(array);
          
          checkedMemmove(array, addr.add(toOffset), addr.add(fromOffset), 
            maxAtomicBytesCopyHalf);
          
          addrOther = translateNonNullPointer( addr );
          if (addrOther != addr) {
            checkedMemcpy( array, array, addrOther.add(toOffset), addr.add(toOffset),
                maxAtomicBytesCopyHalf);
          }

          toOffset += maxAtomicBytesCopyHalf;
          fromOffset += maxAtomicBytesCopyHalf;
          nElems -= maxAtomicBytesCopyHalf;
        }

      } else {

        int toOff = toOffset + (nElems-1);
        int fromOff = fromOffset + (nElems-1);
        
        while(nElems > maxAtomicBytesCopyHalf) {
        
          addr = VM_Address.fromObjectNB(array);

          checkedMemmove(array, addr.add(toOff), addr.add(fromOff),
            maxAtomicBytesCopyHalf);
          
          addrOther = translateNonNullPointer(addr);
          if (addrOther != addr) {
            checkedMemcpy(array, array, addrOther.add(toOff), addr.add(toOff),
              maxAtomicBytesCopyHalf);
          }
          
          toOff -= maxAtomicBytesCopyHalf;
          fromOff -= maxAtomicBytesCopyHalf;
          nElems -= maxAtomicBytesCopyHalf;
        }            
      }
      
      if (nElems > 0) {
        addr = VM_Address.fromObjectNB(array);      

        checkedMemmove(array, addr.add(toOffset), addr.add(fromOffset),
          nElems);
          
        addrOther = translateNonNullPointer(addr);
        if (addrOther != addr) {
          checkedMemcpy(array, array, addrOther.add(toOffset), addr.add(toOffset),
            nElems);
        }
      }
    }
    

    public void copyOverlappingSingleCopy(Oop array, int fromOffset, Blueprint.Array bp, int toOffset, int nElems) 
      throws PragmaAssertNoExceptions {
      
        copyOverlapping( array, fromOffset, toOffset, bp, nElems, false, maxAtomicRefCopy );
    }
    
    public void copyOverlappingTwoCopies(Oop array, int fromOffset, Blueprint.Array bp, int toOffset, int nElems) 
      throws PragmaAssertNoExceptions {
      
        copyOverlapping( array, fromOffset, toOffset, bp, nElems, true, maxAtomicRefCopyHalf );
    }
    
    public void copyOverlappingWithArraylets(Oop array, int fromOffset, int toOffset, Blueprint.Array bp, int nElems,
      boolean references, boolean fromStart)  throws PragmaAssertNoExceptions {        
    
      if (nElems == 0) {
        return ;
      }  
      
      int componentSize = bp.getComponentSize();
      int maxAtomicCopy = uncheckedDiv(maxAtomicBytesCopy,componentSize);
      
      int fromFirstArraylet; // arraylet of the byte to be first copied 
      int fromArrayletsOffset; // offset to the list of arraylets, positioned to the arraylet to be copied now
      int fromInnerOffset; // in-arraylet offset of the byte to be copied now
      int fromInnerElems; // in-arraylet number of elements that remain to be copied 
      
      int toFirstArraylet; // arraylet of the first byte destination
      int toArrayletsOffset;
      int toInnerOffset;
      int toInnerElems;

      int toMark = 0; // number of elements left to mark
                  // (we mark first toMark destinations)
            
      // FIXME: locally refactor this code, hopefully GCC can do this too..
      
      if (fromStart) { // to <= from
        fromFirstArraylet = (fromOffset*componentSize)/arrayletSize;
        fromInnerOffset = (fromOffset*componentSize) % arrayletSize;
        toFirstArraylet = (toOffset*componentSize)/arrayletSize;
        toInnerOffset = (toOffset*componentSize) % arrayletSize;
        if (references) {
          //toMark = (fromOffset - toOffset + MachineSizes.BYTES_IN_ADDRESS) / MachineSizes.BYTES_IN_ADDRESS;        
          // number of ovewritten (lost) pointers
          
          //toMark = min(nElems, fromOffset-toOffset);
          toMark = nElems; // because of incremental scanning
        }
      } else {
        fromFirstArraylet = ((fromOffset + nElems)*componentSize - 1) / arrayletSize; 
        fromInnerOffset = ((fromOffset + nElems)*componentSize - 1) % arrayletSize;
        toFirstArraylet = ((toOffset + nElems)*componentSize - 1) / arrayletSize;
        toInnerOffset = ((toOffset + nElems)*componentSize - 1) % arrayletSize;
        if (references) {
          //toMark = (toOffset - fromOffset + MachineSizes.BYTES_IN_ADDRESS) / MachineSizes.BYTES_IN_ADDRESS;
          toMark = min(nElems, toOffset-fromOffset);
        }
        
      }

      fromArrayletsOffset = ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD + fromFirstArraylet*MachineSizes.BYTES_IN_ADDRESS;
      toArrayletsOffset = ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD + toFirstArraylet*MachineSizes.BYTES_IN_ADDRESS;

      if (fromStart) {
        fromInnerElems = uncheckedDiv(arrayletSize-fromInnerOffset,componentSize);
        toInnerElems = uncheckedDiv(arrayletSize-toInnerOffset,componentSize);
      } else {
        fromInnerElems = uncheckedDiv(fromInnerOffset+1,componentSize);
        toInnerElems = uncheckedDiv(toInnerOffset+1,componentSize);        
      }

      if (fromInnerElems > nElems) {
        fromInnerElems = nElems;
      }

      if (toInnerElems > nElems) {
        toInnerElems = nElems;
      }
      
      int markNow;
      VM_Address toAddress;
      VM_Address fromAddress;
      
      while(nElems > 0) {
        
        while( toInnerElems >= maxAtomicCopy && fromInnerElems >= maxAtomicCopy ) {

          if (fromStart) {
            toAddress = VM_Address.fromObjectNB(array).add(toArrayletsOffset).getAddress().add(toInnerOffset);
            fromAddress = VM_Address.fromObjectNB(array).add(fromArrayletsOffset).getAddress().add(fromInnerOffset);
            
            if (references) {
              if (toMark > maxAtomicCopy) { // does this check pay off ?
                markNow = maxAtomicCopy;
              } else {
                markNow = toMark;
              }
              updateAndMarkPointers( toAddress, 0, markNow );            
              toMark -= markNow;
            }
            
          } else {
            toAddress = VM_Address.fromObjectNB(array).add(toArrayletsOffset).getAddress().add(toInnerOffset - maxAtomicBytesCopy + 1);
            fromAddress = VM_Address.fromObjectNB(array).add(fromArrayletsOffset).getAddress().add(fromInnerOffset - maxAtomicBytesCopy + 1);
            
            if (references) {
              if (toMark > maxAtomicCopy) { // does this check pay off ?
                markNow = maxAtomicCopy;
                updateAndMarkPointers( toAddress, 0, markNow );
              } else {
                markNow = toMark;            
                updateAndMarkPointers( toAddress, (maxAtomicCopy-markNow)*MachineSizes.BYTES_IN_ADDRESS, markNow );
              }
              toMark -= markNow;
            }
          }
          
          checkedMemmove( array,  toAddress, fromAddress, maxAtomicBytesCopy );
          
          if (references) {
            updatePointers( toAddress, 0, maxAtomicCopy );
          }
        
          fromInnerElems -= maxAtomicCopy;
          toInnerElems -= maxAtomicCopy;
          nElems -= maxAtomicCopy;
          
          if (fromStart) {
            fromInnerOffset += maxAtomicBytesCopy;
            toInnerOffset += maxAtomicBytesCopy;
          } else {
            fromInnerOffset -= maxAtomicBytesCopy;
            toInnerOffset -= maxAtomicBytesCopy;
          }
        }
      
        if (VERIFY_ARRAYCOPY) {
          assert (toInnerElems < maxAtomicCopy || fromInnerElems < maxAtomicCopy );
        }
        
        if (nElems==0) {            
          return ;
        }

        if (fromInnerElems == 0) {
          if (fromStart) {
            fromArrayletsOffset += MachineSizes.BYTES_IN_ADDRESS;
            fromInnerOffset = 0;
          } else {
            fromArrayletsOffset -= MachineSizes.BYTES_IN_ADDRESS;
            fromInnerOffset = arrayletSize - 1;
          }
            
          fromInnerElems = uncheckedDiv(arrayletSize,componentSize);
          if (fromInnerElems > nElems) {
            fromInnerElems = nElems;
          }
          continue;
        }
        
        if (toInnerElems == 0) {
          if (fromStart) {
            toArrayletsOffset += MachineSizes.BYTES_IN_ADDRESS;
            toInnerOffset = 0;
          } else {
            toArrayletsOffset -= MachineSizes.BYTES_IN_ADDRESS;
            toInnerOffset = arrayletSize - 1;
          }
            
          toInnerElems = uncheckedDiv(arrayletSize,componentSize);
          if (toInnerElems > nElems) {
            toInnerElems = nElems;
          }
          continue;
        }
        
        if (toInnerElems <= fromInnerElems) { // toInnerElems < maxAtomicCopy

          if (fromStart) {
            toAddress = VM_Address.fromObjectNB(array).add(toArrayletsOffset).getAddress().add(toInnerOffset);
            fromAddress = VM_Address.fromObjectNB(array).add(fromArrayletsOffset).getAddress().add(fromInnerOffset);
            
            if (references) {
              if (toMark > toInnerElems) { // does this check pay off ?
                markNow = toInnerElems;
              } else {
                markNow = toMark;
              }
              updateAndMarkPointers( toAddress, 0, markNow );            
              toMark -= markNow;
            }
            
          } else {
            toAddress = VM_Address.fromObjectNB(array).add(toArrayletsOffset).getAddress().add(toInnerOffset - toInnerElems * componentSize + 1 );
            fromAddress = VM_Address.fromObjectNB(array).add(fromArrayletsOffset).getAddress().add(fromInnerOffset - toInnerElems * componentSize + 1);
            
            if (references) {
              if (toMark > toInnerElems) { // does this check pay off ?
                markNow = toInnerElems;
                updateAndMarkPointers( toAddress, 0, markNow );
              } else {
                markNow = toMark;            
                updateAndMarkPointers( toAddress, (toInnerElems-markNow)*MachineSizes.BYTES_IN_ADDRESS, markNow );
              }
              toMark -= markNow;
            }
          }
          
          checkedMemmove( array, toAddress, fromAddress, toInnerElems * componentSize );
          
          if (references) {
            updatePointers( toAddress, 0, toInnerElems );
          }
          
          nElems -= toInnerElems;
          fromInnerElems -= toInnerElems;
          
          if (fromStart) {
            fromInnerOffset += toInnerElems * componentSize;
          } else {
            fromInnerOffset -= toInnerElems * componentSize;
          }

          toInnerElems = 0; // toInnerOffset and toArrayletsOffset will be updated in the main cycle
          
        } else {
          // toInnerElems > fromInnerElems

          if (fromStart) {
            toAddress = VM_Address.fromObjectNB(array).add(toArrayletsOffset).getAddress().add(toInnerOffset);
            fromAddress = VM_Address.fromObjectNB(array).add(fromArrayletsOffset).getAddress().add(fromInnerOffset);
            
            if (references) {
              if (toMark > fromInnerElems) { // does this check pay off ?
                markNow = fromInnerElems;
              } else {
                markNow = toMark;
              }
              updateAndMarkPointers( toAddress, 0, markNow );            
              toMark -= markNow;
            }            
            
          } else {
            toAddress = VM_Address.fromObjectNB(array).add(toArrayletsOffset).getAddress().add(toInnerOffset - fromInnerElems * componentSize + 1 );
            fromAddress = VM_Address.fromObjectNB(array).add(fromArrayletsOffset).getAddress().add(fromInnerOffset - fromInnerElems * componentSize + 1);
            
            if (references) {
              if (toMark > fromInnerElems) { // does this check pay off ?
                markNow = fromInnerElems;
                updateAndMarkPointers( toAddress, 0, markNow );
              } else {
                markNow = toMark;            
                updateAndMarkPointers( toAddress, (fromInnerElems-markNow)*MachineSizes.BYTES_IN_ADDRESS, markNow );
              }
              toMark -= markNow;              
            }            
          }
          
          checkedMemmove( array, toAddress, fromAddress, fromInnerElems * componentSize );
          
          if (references) {
            updatePointers( toAddress, 0, fromInnerElems );
          }
          
          nElems -= fromInnerElems;
          toInnerElems -= fromInnerElems;
          
          if (fromStart) {
            toInnerOffset += fromInnerElems * componentSize;
          } else {
            toInnerOffset -= fromInnerElems * componentSize;
          }
          
          fromInnerElems = 0; // fromInnerOffset and fromArrayletsOffset will be updated in the main cycle
        }
      }
    }

    // FIXME: check if this is the fast way to do it
    // it should be possible to write a function in assembly for very fast copy to two destinations
    // however, then we would have to handle pointer updating differently, so it is not obvious if it 
    // would really help ; the same applies to copyArrayElements    
    public void copyOverlapping(Oop array, int fromOffset, int toOffset, Blueprint.Array bp, int nElems, 
      boolean twoCopies, int maxAtomicCopy) throws PragmaInline, PragmaAssertNoExceptions {        

      int toMark; 
      
      int fromByteOffset = bp.byteOffset(fromOffset);
      int toByteOffset = bp.byteOffset(toOffset);
      
      if (toByteOffset <= fromByteOffset) {
      
        //toMark = min( nElems, fromOffset-toOffset );  !!! this is wrong becuase of incremental scanning

        while(nElems > maxAtomicCopy) {
        
          VM_Address addr = VM_Address.fromObjectNB(array);
          
          if (BROOKS) {
            addr = translateNonNullPointer(addr);
          }
          
          updateAndMarkPointers( addr, toByteOffset, maxAtomicCopy );
          
          checkedMemmove(array, addr.add(toByteOffset), addr.add(fromByteOffset), 
            maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS);
          
          updatePointers( addr, toByteOffset, maxAtomicCopy);

          if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && twoCopies) {
            VM_Address addrOther = translateNonNullPointer( addr );
            if (addrOther != addr) {
              checkedMemcpy( array, array, addrOther.add(toByteOffset), addr.add(toByteOffset),
                maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS);
            }
          }

          toByteOffset += maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS;
          fromByteOffset += maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS;
          nElems -= maxAtomicCopy;
        }

        if (nElems > 0) {
          VM_Address addr = VM_Address.fromObjectNB(array);      
          if (BROOKS) {
            addr = translateNonNullPointer(addr);
          }

          updateAndMarkPointers( addr, toByteOffset, nElems );

          checkedMemmove(array, addr.add(toByteOffset), addr.add(fromByteOffset),
            nElems * MachineSizes.BYTES_IN_ADDRESS);
          
          updatePointers( addr, toByteOffset, nElems);
      
          if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && twoCopies) {
            VM_Address addrOther = translateNonNullPointer(addr);
            if (addrOther != addr) {
              checkedMemcpy(array, array, addrOther.add(toByteOffset), addr.add(toByteOffset),
                nElems * MachineSizes.BYTES_IN_ADDRESS);
            }
          }
        }
      } else {
        
        toMark = min(nElems, toOffset-fromOffset); // incremental scanning does not matter, because it progresses forward
        
        while(nElems > maxAtomicCopy) {
        
          int toOff = toByteOffset + (nElems-maxAtomicCopy) * MachineSizes.BYTES_IN_ADDRESS;
          int fromOff = fromByteOffset + (nElems-maxAtomicCopy) * MachineSizes.BYTES_IN_ADDRESS;
  
          VM_Address addr = VM_Address.fromObjectNB(array);
          if (BROOKS) {
            addr = translateNonNullPointer(addr);
          }

          int markNow = toMark;
          if (markNow > maxAtomicCopy) {
            markNow = maxAtomicCopy;
            updateAndMarkPointers( addr, toOff, markNow );
          } else {
            //!!! updateAndMarkPointers( addr, toOff + (markNow-maxAtomicCopy)*MachineSizes.BYTES_IN_ADDRESS, markNow) ;
            updateAndMarkPointers( addr, toOff + (maxAtomicCopy-markNow)*MachineSizes.BYTES_IN_ADDRESS, markNow) ;
          }
          toMark -= markNow;
          
          checkedMemmove(array, addr.add(toOff), addr.add(fromOff),
            maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS);
          
          // hmm, is it necessary when copying in this direction ?
          updatePointers( addr, toOff, maxAtomicCopy);

          if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && twoCopies) {
            VM_Address addrOther = translateNonNullPointer(addr);
            if (addrOther != addr) {
              checkedMemcpy(array, array, addrOther.add(toOff), addr.add(toOff),
                maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS);
            }
          }
          
          toOff -= maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS;
          fromOff -= maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS;
          nElems -= maxAtomicCopy;
          
        }
        
        if (nElems > 0) {
          VM_Address addr = VM_Address.fromObjectNB(array);      
        
          if (BROOKS) {
            addr = translateNonNullPointer(addr);
          }

          updateAndMarkPointers( addr, toByteOffset + (nElems-toMark)*MachineSizes.BYTES_IN_ADDRESS, toMark );

          checkedMemmove(array, addr.add(toByteOffset), addr.add(fromByteOffset),
            nElems * MachineSizes.BYTES_IN_ADDRESS);
          
          updatePointers( addr, toByteOffset, nElems);
      
          if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && twoCopies) {
            VM_Address addrOther = translateNonNullPointer(addr);
            if (addrOther != addr) {
              checkedMemcpy(array, array, addrOther.add(toByteOffset), addr.add(toByteOffset),
                nElems * MachineSizes.BYTES_IN_ADDRESS);
            }
          }
        }
      }
    }
    
    /**
     * Unsafe, no checks, just copy. Use to implement
     * System.arraycopy.  
     **/
     
     // FIXME:
     // can we refactor these methods to have less copy-paste code ?
     
    public void copyArrayElements(Oop fromArray, int fromOffset,
				  Oop toArray, int toOffset, 
				  int nElems) { 
			
      if (VERIFY_ARRAYCOPY) {
        arrayCopyBoundsCheck( fromArray, fromOffset, nElems );
        arrayCopyBoundsCheck( toArray, toOffset, nElems );
      }			
				  
      if (!COMPACTION && !FORCE_TRANSLATING_BARRIER && !FORCE_YUASA_BARRIER && !NEEDS_YUASA_BARRIER &&
        !FORCE_IMAGE_BARRIER && !NEEDS_IMAGE_BARRIER && !ARRAYLETS) {
      	  Mem.the().copyArrayElements(fromArray, fromOffset,
				    toArray, toOffset,
				    nElems);      
          return;
      }

      if (VERIFY_COMPACTION) {
        verifyForwardingPointer(VM_Address.fromObjectNB(fromArray),FWD_ALWAYS);
        verifyForwardingPointer(VM_Address.fromObjectNB(toArray),FWD_ALWAYS);
      }
	
      // more checks are expected to be done by the caller			  
      Blueprint.Array fromBP = (Blueprint.Array)fromArray.getBlueprint();
      
      if (PARANOID_ARRAYCOPY_MARKING && !ARRAYLETS && marking) {
        if (fromBP.getComponentBlueprint().isReference() ) {
          enterExcludedBlock();
          
          Blueprint.Array toBP = (Blueprint.Array)toArray.getBlueprint();
          updateAndMarkPointers(VM_Address.fromObject(toArray), toBP.byteOffset(toOffset), nElems);
          
          leaveExcludedBlock();
        
        }
      }
      
      if (!ARRAYLETS && !( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING ) ) {
        if (!fromBP.getComponentBlueprint().isReference() ) {
          Mem.the().copyArrayElements(fromArray, fromOffset,
              toArray, toOffset,
              nElems);      
          return;
        }
      }
      
      VM_Address toAddress = VM_Address.fromObjectNB(toArray);        

      if (ARRAYLETS) {
        if (fromBP.getComponentBlueprint().isReference()) {
          imageStoreBarrier( toAddress, 0 );
          copyArrayElementsWithArraylets( fromArray, fromOffset, fromBP, 
              toArray, toOffset, nElems, true );
        } else {
          copyArrayElementsWithArraylets( fromArray, fromOffset, fromBP, 
              toArray, toOffset, nElems, false );
        }
      }
      
      if (!ARRAYLETS) {
      
//      if ( (COMPACTION || FORCE_DIJKSTRA_BARRIER || FORCE_YUASA_BARRIER) && REPLICATING && inHeap(toAddress)) { // FIXME: assertions would help here
        if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING && inHeap(toAddress)) { // FIXME: assertions would help here

          int blockIdx=blockIdx(toAddress);
          if (Bits.getBit(largeBits,blockIdx)) {
            if (fromBP.getComponentBlueprint().isReference()) {        
              copyArrayElementsSingleCopy( fromArray, fromOffset, fromBP, toArray, toOffset, nElems);
            } else {
              Mem.the().copyArrayElements(fromArray, fromOffset,
                toArray, toOffset,
                nElems);      
              return ;
            }
          } else {
            if (fromBP.getComponentBlueprint().isReference()) {
              copyArrayElementsTwoCopies( fromArray, fromOffset, fromBP, toArray, toOffset, nElems);
            } else {
              copyArrayBytesTwoCopies( fromArray, fromOffset, fromBP, toArray, toOffset, nElems );
            }
          }
        } else { // in image or 
               // brooks compaction/forced barriers
          if ( ( COMPACTION || FORCE_TRANSLATING_BARRIER ) && REPLICATING ) {
            // now we know we are in the image
        
            if (fromBP.getComponentBlueprint().isReference()) {
              imageStoreBarrier(toAddress, Assert.IMAGEONLY); // we now know it's in the image        
              copyArrayElementsSingleCopy( fromArray, fromOffset, fromBP, toArray, toOffset, nElems);
            } else {
              // no image barrier for primitives
              Mem.the().copyArrayElements(fromArray, fromOffset,
                toArray, toOffset,
                nElems);      
              return;              
            }
          } else {
            // BROOKS style barrier or some other barrier needed of forced (Yuasa, Image)
            // we know that we have an array of references

            imageStoreBarrier(toAddress, 0);
            copyArrayElementsSingleCopy( fromArray, fromOffset, fromBP, toArray, toOffset, nElems);
          }
        }
      }
      
      if (VERIFY_COMPACTION) {
        verifyForwardingPointer(VM_Address.fromObjectNB(fromArray), FWD_ALWAYS);
        verifyForwardingPointer(VM_Address.fromObjectNB(toArray), FWD_ALWAYS);
      }

      if (VERIFY_ARRAYCOPY) {
        verifyArraySubsets(toArray, fromArray, toOffset, fromOffset, nElems, "after copyArrayElements");
      }
    }
    
    // assumes replicating compaction
    public void  copyArrayBytesTwoCopies( Oop fromArray, int fromOffset, Blueprint.Array fromBP, 
      Oop toArray, int toOffset, int nElems ) throws PragmaAssertNoExceptions {
    
      Blueprint.Array toBP = (Blueprint.Array)toArray.getBlueprint();
      fromOffset = fromBP.byteOffset(fromOffset);
      toOffset = toBP.byteOffset(toOffset);
      
      nElems = nElems * fromBP.getComponentSize();
      
      VM_Address toAddressOther = null;
      VM_Address toAddress = null;
      VM_Address fromAddress = null;
      
      while(nElems > maxAtomicBytesCopyHalf) {

        toAddress = VM_Address.fromObjectNB(toArray);
        fromAddress = VM_Address.fromObjectNB(fromArray);

        checkedMemcpy(toArray, fromArray, toAddress.add(toOffset),
          fromAddress.add(fromOffset),
          maxAtomicBytesCopyHalf);
        
        toAddressOther = translateNonNullPointer(toAddress);
        if (toAddressOther != toAddress ) {
          checkedMemcpy(toArray, toArray, toAddressOther.add(toOffset),
            toAddress.add(toOffset),
            maxAtomicBytesCopyHalf);  
        }
          
        toOffset += maxAtomicBytesCopyHalf;
        fromOffset += maxAtomicBytesCopyHalf;
        nElems -= maxAtomicBytesCopyHalf;
      }
      
      if (nElems > 0) {
        toAddress = VM_Address.fromObjectNB(toArray);
        fromAddress = VM_Address.fromObjectNB(fromArray);

        checkedMemcpy(toArray, fromArray, toAddress.add(toOffset),
          fromAddress.add(fromOffset),
          nElems);

        toAddressOther = translateNonNullPointer(toAddress);
        if (toAddressOther != toAddress ) {
          checkedMemcpy(toArray, toArray, toAddressOther.add(toOffset),
            toAddress.add(toOffset),
            nElems);  
        }                  
      }
    }
    
    // there is always a single copy of the array
    // (large objects, objects in image )
    public void copyArrayElementsSingleCopy(Oop fromArray, int fromOffset, Blueprint.Array fromBP, 
				  Oop toArray, int toOffset, int nElems)  
          throws PragmaAssertNoExceptions {


      copyArrayElements( fromArray, fromOffset, fromBP, toArray, toOffset, nElems,
        false, maxAtomicRefCopy );      
        
    }    

    // at some point during the (interruptible) copy, there can be multiple array copies
    // can it really happen for small objects, if maxAtomicRefCopy is large ?
    // but we should have this if we wanted maxAtomicRefCopy small ... 
    public void copyArrayElementsTwoCopies(Oop fromArray, int fromOffset, Blueprint.Array fromBP, 
				  Oop toArray, int toOffset, int nElems)  
          throws PragmaAssertNoExceptions {

      copyArrayElements( fromArray, fromOffset, fromBP, toArray, toOffset, nElems,
        true, maxAtomicRefCopyHalf );      
        
    }    

    public void copyArrayElements(Oop fromArray, int fromOffset, Blueprint.Array fromBP, 
				  Oop toArray, int toOffset, 
				  int nElems, boolean twoCopies, int maxAtomicCopy)  
				  
          throws PragmaInline, PragmaAssertNoExceptions {

      Blueprint.Array toBP = (Blueprint.Array)toArray.getBlueprint();
      
      fromOffset = fromBP.byteOffset(fromOffset);
      toOffset = toBP.byteOffset(toOffset);
        
      VM_Address toAddress = null;
      VM_Address fromAddress = null;
      VM_Address toAddressOther = null;
      
      while (nElems > maxAtomicCopy) {
      
        toAddress = VM_Address.fromObjectNB(toArray);
        fromAddress = VM_Address.fromObjectNB(fromArray);

        if (BROOKS) {
          toAddress = translateNonNullPointer(toAddress);
          fromAddress = translateNonNullPointer(fromAddress);
        }
        
        updateAndMarkPointers( toAddress, toOffset, maxAtomicCopy);  
        
        checkedMemcpy(toArray, fromArray, toAddress.add(toOffset),
          fromAddress.add(fromOffset),
          maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS);
        
        updatePointers( toAddress, toOffset, maxAtomicCopy);
          
        if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && twoCopies) {
          toAddressOther = translateNonNullPointer(toAddress);
          if (toAddressOther != toAddress ) {
            checkedMemcpy(toArray, toArray, toAddressOther.add(toOffset),
              toAddress.add(toOffset),
              maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS);  
          }
        }
          
        toOffset += maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS;
        fromOffset += maxAtomicCopy * MachineSizes.BYTES_IN_ADDRESS;
        nElems -= maxAtomicCopy;
        
      }

      if (nElems > 0) {
        toAddress = VM_Address.fromObjectNB(toArray);
        fromAddress = VM_Address.fromObjectNB(fromArray);

        if (BROOKS) {
          toAddress = translateNonNullPointer(toAddress);
          fromAddress = translateNonNullPointer(fromAddress);
        }
            
        updateAndMarkPointers( toAddress, toOffset, nElems );
        
        checkedMemcpy(toArray, fromArray, toAddress.add(toOffset),
          fromAddress.add(fromOffset),
          nElems * MachineSizes.BYTES_IN_ADDRESS);

        updatePointers( toAddress, toOffset, nElems);       

        if (REPLICATING && (COMPACTION || FORCE_TRANSLATING_BARRIER) && twoCopies) {
          toAddressOther = translateNonNullPointer(toAddress);
          if (toAddressOther != toAddress) {
            checkedMemcpy(toArray, toArray, toAddressOther.add(toOffset),
              toAddress.add(toOffset),
              nElems * MachineSizes.BYTES_IN_ADDRESS);
          }
        }
      }
    }    

    public void copyArrayElementsWithArraylets(Oop fromArray, int fromOffset, Blueprint.Array fromBP, 
				  Oop toArray, int toOffset, int nElems, boolean references)  
				  
          throws PragmaAssertNoExceptions {

      // observations:
      //  arraylets don't move, except for the last arraylet in an array
      //	because the last one can be in a small object spine, which can move
      //  as most arrays are however small, it probably is not worth checking for

      if (false) {
        Native.print_string("copyArrayElementsWithArraylets called for destination array ");
        printAddr(VM_Address.fromObjectNB(toArray));
        Native.print_string(" first arraylet pointer of this array is ");
        printAddr(VM_Address.fromObjectNB(toArray).add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD).getAddress());
        Native.print_string("\n");
        
        Native.print_string("   source array is ");
        printAddr(VM_Address.fromObjectNB(fromArray));
        Native.print_string(" first arraylet pointer of this array is ");
        printAddr(VM_Address.fromObjectNB(fromArray).add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD).getAddress());
        Native.print_string(" elements to copy: ");
        Native.print_int(nElems);
        Native.print_string("\n"); 
      }

      if (nElems==0) {
        return;
      }

      int componentSize = fromBP.getComponentSize();
      int maxAtomicCopy = uncheckedDiv(maxAtomicBytesCopy,componentSize);
      
      int fromFirstArraylet = (fromOffset*componentSize)/arrayletSize;
      int fromArrayletsOffset = ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD + fromFirstArraylet*MachineSizes.BYTES_IN_ADDRESS;
      int fromInnerOffset = (fromOffset*componentSize) % arrayletSize;
      int fromInnerElems = uncheckedDiv(arrayletSize-fromInnerOffset,componentSize);
      if (fromInnerElems > nElems) {
        fromInnerElems = nElems;
      }
      
      int toFirstArraylet = (toOffset*componentSize)/arrayletSize;
      int toArrayletsOffset = ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD + toFirstArraylet*MachineSizes.BYTES_IN_ADDRESS;
      int toInnerOffset = (toOffset*componentSize) % arrayletSize;
      int toInnerElems = uncheckedDiv(arrayletSize-toInnerOffset, componentSize);
      if (toInnerElems >  nElems) {
        toInnerElems = nElems;
      }
      
      VM_Address toAddress = null;
      VM_Address fromAddress = null;
      
      while (nElems > 0) {
     
        if (VERIFY_ARRAYCOPY) {
          assert(toInnerElems <= nElems);
          assert(fromInnerElems <= nElems);
        } 
        
        while( toInnerElems >= maxAtomicCopy && fromInnerElems >= maxAtomicCopy ) {
        
          // FIXME: this can maybe be optimized, as only the last arraylet can move
          
          toAddress = VM_Address.fromObjectNB(toArray).add(toArrayletsOffset).getAddress().add(toInnerOffset);
          fromAddress = VM_Address.fromObjectNB(fromArray).add(fromArrayletsOffset).getAddress().add(fromInnerOffset);
          
          if (references) {
            updateAndMarkPointers( toAddress, 0, maxAtomicCopy);  
          }

          checkedMemcpy(toArray, fromArray, toAddress,
            fromAddress, maxAtomicCopy * componentSize);
        
          if (references) {
            updatePointers( toAddress, 0, maxAtomicCopy);
          }
          
          toInnerElems -= maxAtomicCopy;
          fromInnerElems -= maxAtomicCopy;
          toInnerOffset += maxAtomicCopy * componentSize;
          fromInnerOffset += maxAtomicCopy * componentSize;
          nElems -= maxAtomicCopy;
        }
        
        if (VERIFY_ARRAYCOPY) {
          assert (toInnerElems < maxAtomicCopy || fromInnerElems < maxAtomicCopy );
        }

        if (nElems==0) {            
          return ;
        }        

        if (fromInnerElems == 0) {

          fromArrayletsOffset += MachineSizes.BYTES_IN_ADDRESS;
          fromInnerOffset = 0;
          fromInnerElems = uncheckedDiv( arrayletSize, componentSize );
          if (fromInnerElems > nElems) {
            fromInnerElems = nElems;
          }        
          continue;
        }
        
        if (toInnerElems == 0) {

          toArrayletsOffset += MachineSizes.BYTES_IN_ADDRESS;
          toInnerOffset = 0;
          toInnerElems = uncheckedDiv( arrayletSize, componentSize );
          if (toInnerElems > nElems) {
            toInnerElems = nElems;
          }        
          continue;
        }
        
        toAddress = VM_Address.fromObjectNB(toArray).add(toArrayletsOffset).getAddress().add(toInnerOffset);
        fromAddress = VM_Address.fromObjectNB(fromArray).add(fromArrayletsOffset).getAddress().add(fromInnerOffset);

        if (toInnerElems <= fromInnerElems) {
          
          if (references) {
            updateAndMarkPointers( toAddress, 0, toInnerElems);
          }
          
          checkedMemcpy(toArray, fromArray, toAddress, fromAddress, toInnerElems * componentSize);
          
          if (references) {
            updatePointers( toAddress, 0, toInnerElems);
          }
          
          nElems -= toInnerElems;
          fromInnerElems -= toInnerElems;
          fromInnerOffset += toInnerElems * componentSize;

          toInnerElems = 0; // toInnerOffset and toArrayletsOffset will be updated in the main cycle

        } else {
          // toInnerElems > fromInnerElems
        
          if (references) {
            updateAndMarkPointers( toAddress, 0, fromInnerElems);
          }
          
          checkedMemcpy(toArray, fromArray, toAddress, fromAddress, fromInnerElems * componentSize);
 
          if (references) {
            updatePointers( toAddress, 0, fromInnerElems);
          }
          
          nElems -= fromInnerElems;
          toInnerElems -= fromInnerElems;
          toInnerOffset += fromInnerElems * componentSize;
          
          fromInnerElems = 0; // fromInnerOffset and fromArrayletsOffset will be updated in the main cycle          
        }
      }
    }    
        
    private class MonitorRegistry {
      VM_Address owner;
      MonitorRegistry next;
      Monitor monitor;
      
      MonitorRegistry(Monitor monitor, VM_Address owner, MonitorRegistry next) {
        this.next = next;
        this.owner = owner;
        this.monitor = monitor;
      }
    }

    private static MonitorRegistry monitorRegistryHead = null;
    
    public void registerMonitor( Monitor monitor, VM_Address owner ) throws PragmaNoPollcheck {
    
      if (DEBUG_MONITORS && debugMonitors) {
        Native.print_string("Registering monitor ");
        printAddr(VM_Address.fromObjectNB(monitor));
        Native.print_string(" of object ");
        printAddr(owner);
        Native.print_string(" with fastlock field ");
        Native.print_hex_int(owner.add(0x4).getInt());
        Native.print_string("\n");
      }
      monitorRegistryHead = new MonitorRegistry( monitor, owner, monitorRegistryHead );
      
      if (DEBUG_MONITORS && debugMonitors) {
        Native.print_string("Dumping monitor registry after registration:\n");
        dumpMonitorRegistry();
      }
    }
    

    public void dumpMonitorRegistry() {
    
      MonitorRegistry r = monitorRegistryHead;
      
      while(r!=null) {
        Native.print_string("[O:");
        printAddr(r.owner);
        Native.print_string(" M: ");
        printAddr(VM_Address.fromObjectNB(r.monitor));
        Native.print_string("]");
        r = r.next;
      }
      Native.print_string("\n");
    }

    // mark monitors of white owners white and remove their entries
    // update references to owners
  
    public void monitorRegistryAfterWalk() throws PragmaNoPollcheck, PragmaInline {
    
      MonitorRegistry r = monitorRegistryHead;
      MonitorRegistry prev = null;


      if (DEBUG_MONITORS && debugMonitors) {
        Native.print_string("Dumping monitor registry before walking it.\n");
        dumpMonitorRegistry();
      }
      // FIXME: we don't need the automatic translating barriers here - such as r.monitor
      // because we are just after marking and before compaction, so we know nothing has moved
      while(r!=null) {
        pollMarking();
        
        if (KEEP_INFLATED_MONITORS && r.owner==null) {
          continue;  
        }
        
        VM_Address ownerUpd = updateNonNullPointer(r.owner);
        int color = gcData.getColor(ownerUpd);

        if (color == WHITE) {
        // this was an attempt to make the clean-up faster, which did not work, at least with the replicating barrier
        // not doing that is probably also more robust - if OVM is changed later to keep the monitor alive after object's death
        // for any reason, good or bad
        // gcData.setColor(VM_Address.fromObjectNB(r.monitor), WHITE); // barrier not needed because this was already walked and updated
          
          if (DEBUG_MONITORS && debugMonitors) {        
            Native.print_string("Removing monitor ");
            printAddr(VM_Address.fromObjectNB(r.monitor));
            Native.print_string(" of dead object ");
            printAddr(ownerUpd);
            Native.print_string(" with fastlock field ");
            Native.print_hex_int(ownerUpd.add(0x4).getInt());
            Native.print_string(" from registry\n");
          }
          
          if (KEEP_INFLATED_MONITORS) {
            r.owner = null;  
          } else {
            if (prev==null) {
              monitorRegistryHead = r.next;
            } else {
              prev.next = r.next;
            }
          }
            
        } else {

          if (COMPACTION) {
            if (DEBUG_MONITORS && debugMonitors) {
              Native.print_string("Updating reference to monitor ");
              printAddr(VM_Address.fromObjectNB(r.monitor));
              Native.print_string(" in header of object ");
              printAddr(ownerUpd);
              Native.print_string(" with fastlock field ");
              Native.print_hex_int(ownerUpd.add(monitorOffset).getInt());
              Native.print_string("\n");
            }
            r.owner = ownerUpd;
          
            // this works - but it's kind of a hackery
            
            VM_Address monitorAddr = VM_Address.fromObjectNB(r.monitor); // this is the new location, because we are after marking
            
            // FIXME: maybe it would be faster to do it unconditionally...
            if ( BROOKS || monitorAddr != translateNonNullPointer(monitorAddr) ) {
              // the monitor was moved in previous compaction, so update it
              
              // this works - but it's kind of a hackery
              // FIXME: make it cleaner using model operations            
              int fastlock = monitorAddr.asInt() | 0x3; // inflated bits
              ownerUpd.add(monitorOffset).setInt(fastlock);
             
              // FIXME: this should not be necessary - the old copy can have old pointers
              /*
              VM_Address ownerOther = translateNonNullPointer(ownerUpd);
              ownerOther.add(monitorOffset).setInt(fastlock); 
              */
            }
          }
          
// FIXME: how to do this ? I'm still getting class cast exceptions and I have no clue why...
//          MonitorMapperNB mm = (MonitorMapperNB) (ownerUpd.asAnyOop()); 
//          mm.setMonitorNB(r.monitor);
// the generated code for asAnyOop includes a check that throws a class cast exception whenever
// the pointer is not null ; however, the same code fragment works in S3CoreServicesAccess.ensureMonitor
// why ??
          prev=r;
        }
        r=r.next;
      }
    
      if (DEBUG_MONITORS && debugMonitors) {
        Native.print_string("Dumping monitor registry after walking it.\n");
        dumpMonitorRegistry();
      }

    }
    
    public boolean usesArraylets() {
      return ARRAYLETS;
    }
    
    public int arrayletSize() {
      return this.arrayletSize;
    }
    
    public boolean needsArrayAccessBarriers() {
      return ARRAYLETS;
    }
    
    private static final int uncheckedDiv( int a, int b ) throws PragmaInline, PragmaAssertNoExceptions {
      // FIXME: how to do unchecked div ???  do I have to create a pragma or write a 
      // native C function ?
      return a/b;
    }
    
    private static final int min( int a, int b) throws PragmaInline, PragmaAssertNoExceptions {
      return (a<b) ? a : b;
    }

    private static final int max( int a, int b) throws PragmaInline, PragmaAssertNoExceptions {
      return (a>b) ? a : b;
    }
    
    // this is a hack needed for I/O code
    public Oop getOtherReplica( Oop ptr ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints, PragmaInline {
      if ( (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING ) {
        return translatePointer( ptr, 0 );
      } else {
        return ptr;
      }
    }
    
    public boolean hasArrayReplicas() {
      return (COMPACTION || FORCE_TRANSLATING_BARRIER) && REPLICATING && !ARRAYLETS;
    }
    
    public void assertSingleReplica(VM_Address ptr){
      if (COMPACTION && REPLICATING && VERIFY_COMPACTION) {
        VM_Address tptr = translatePointer(ptr);
        if (tptr != ptr) {
          Native.print_string("ERROR: Object has two replicas when not handled:");
          printAddr(ptr);
          Native.print_string("\n");
          
          throw Executive.panic("ERROR: Object has two replicas when it is a problem.");
        }
      }
    }
}


