
package ovm.core.services.memory;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.execution.Native;
import ovm.core.execution.NativeInterface;
import ovm.core.execution.NativeConstants;
import ovm.core.execution.RuntimeExports;
import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMThread;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.services.bytecode.JVMConstants.Throwables;
import ovm.services.memory.fifo.VM_FIFOArea;
import ovm.util.Iterator;
import ovm.util.OVMError;
import s3.util.PragmaNoInline;
import s3.util.PragmaAssertNoSafePoints;
import s3.util.PragmaAssertNoExceptions;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import ovm.core.execution.RuntimeExports;
import ovm.services.memory.scopes.VM_ScopedArea;
import ovm.core.OVMBase;
import ovm.util.Mem;
import ovm.services.monitors.Monitor;

import ovm.core.Executive;

/**
 * The abstract memory management interface. 
 * This class provides a broad API that 
 * supports all the different kinds of memory regions we may want: heap,
 * immortal, explicit, scope, and all features like destructors and GC. It is
 * up to each specific memory manager implementation to either support or
 * not support a given feature.
 * <p>All allocation via <tt>new</tt> is forwarded to the memory manager's
 * allocation methods ({@link #allocate allocate} and 
 * {@link #allocateArray allocateArray}) by the
 * CSA.
 * The default implementation for the allocation routines (and clone) are based
 * on the abstract {@link #getMem} method. If these methods are not
 * overridden then it is presumed that <tt>getMem</tt> takes responsibility for
 * protecting the memory manager against unsafe concurrent access. Otherwise
 * the overridden allocation methods take on this responsibility themselves.
 * The CSA does not provide any exclusion/atomicity guarantees.
 *
 * <p><b>Note:</b> Much of the documentation of this class is lacking.
 *
 * @author jv, cf, jb
 *
 */
public abstract class MemoryManager {
    /**
     * If true, pin any object if references to it might be embedded
     * in JIT-compiled code.  This may be a bad idea, because it can
     * lead to lots of garbage retention in the mostly-copying
     * collector, and leads to premature free in the RT collector.
     **/
    public boolean shouldPinCrazily() { return true; }
    
    public static final class GCNative implements NativeInterface {
	/**
	 * An engine-specific primitive operation.  Force the calling
	 * method to save all call-preserved registers.  This method
	 * may have an empty implementation in engines that do not
	 * perform register allocation.
	 */
	public static native void SAVE_REGISTERS();
    }

    public MemoryManager() {
	RuntimeExports.defineVMProperty("org.ovmj.supportsGC",
					supportsGC());
	RuntimeExports.defineVMProperty("org.ovmj.supportsFinalizers",
					supportsDestructors());
	RuntimeExports.defineVMProperty("org.ovmj.supportsAreaOf",
					supportScopeAreaOf());
	RuntimeExports.defineVMProperty("org.ovmj.supportsScopeChecks",
					reallySupportNHRTT());
    }

    // FIXME: next two methods should really be put into ImageAllocator
    /**
     * Return the base address for the Ovm bootimage
     * (<code>img</code>).  MemoryManager implementations may override
     * this method to get finer control over global memory layout and
     * the layout of the boot image.
     * <p>
     * FIXME: This should return VM_Address rather than int.
     */
    public int getImageBaseAddress() {
	// historically, this value has been 0x60000000, but on ppc
	// linux, that value is a bit large for MemoryManagers that
	// wish to map the heap immediately after the bootimage.
	if (NativeConstants.FORCE_IMAGE_LOCATION!=0) {
	    return NativeConstants.FORCE_IMAGE_LOCATION;
	} else {
	    return 0x40000000;
	}
    }

    /**
     * Return the virtual address at which the heap MUST be mapped, or
     * 0 if no particular address is required.  This value is passed
     * to the makefiles, which attempt to reserve the -heap-size chunk
     * of space starting at this address.  Reserving this space is
     * non-portable, and is currently done only under linux/elf
     * systems.  The memory manager should manually allocate space at
     * this address with mmap(MAP_FIXED).
     */
    public int getFixedHeapAddress() {
	return 0;
    }
    // public static final Object gcCriticalRegionLock = new Object();

    /**
     * Prevent the garbage collector from moving this object, because
     * non-moveable references exist (for example in variables of
     * type VM_Address or in in-progress system calls such as
     * aio_read).
     **/
    public abstract void pin(Oop oop);
    
    public void pinNewLocation(Oop oop) {
      pin(oop);
    }
    
    /**
     * Prevent the garbage collector from moving this object, because
     * non-moveable references exist (for example in variables of
     * type VM_Address or in in-progress system calls such as
     * aio_read).
     **/
    public final void pin(Object o) {
	// Do not permit calls to pin() before VM_Address.fromObject
	// is safe.  The only way to pin an object at image build time
	// is to call VM_Address.asInt().
	pin(VM_Address.fromObject(o).asOop());
    }
    
    public final void pinNewLocation(Object o) {
	// Do not permit calls to pin() before VM_Address.fromObject
	// is safe.  The only way to pin an object at image build time
	// is to call VM_Address.asInt().
	pinNewLocation(VM_Address.fromObject(o).asOop());
    }
    
  

    /**
     * Allow the garbage collector to move an object that has been
     * previously pinned.
     **/
    public abstract void unpin(Oop oop);
    /**
     * Allow the garbage collector to move an object that has been
     * previously pinned.
     **/
    public final void unpin(Object o) {
	if (!OVMBase.isBuildTime())
	    unpin(VM_Address.fromObject(o).asOop());
    }

    /**
     * Return or throw an OutOfMemoryError without performing any
     * allocation.
     **/
    protected Error outOfMemory() {
	CoreServicesAccess csa 
	    = DomainDirectory.getExecutiveDomain().getCoreServicesAccess();
	csa.generateThrowable(Throwables.OUT_OF_MEMORY_ERROR, 0);
	return null;
    }

    /**
     * Free block if supported by the configuartion.
     * FIXME perhaps we should have a size parameter, unless we want to force
     * any MM that supports freeing to do its own block-size bookkeeping, which
     * seems like it would duplicate information in the {@link ovm.core.domain.ObjectModel}
     * (which we are striving to keep the allocator a layer below).
     **/
    public void free(VM_Address adr) {
        throw new OVMError.Unimplemented();
    }

    /**
     * Runtime initialization of the memory manager. Must be called (once)
     * in advance of any code that might do allocation.
     * <p>This is essentially the first thing executed at OVM startup and so
     * it can't rely on any other executive domain services.
     */
    public abstract void boot(boolean useImageBarrier);
    
    public void initWithCommandLineArguments() {};

    /**
     * Informs the memory manager that the OVM has completed booting. 
     **/
    public void fullyBootedVM() {
    }
    
    /**
     * Informs the memory manager that the OVM is shutting down.  Note,
     * you are not guaranteed that this call will happen, esp. if the VM
     * is shutting down due to an error.  But generally it would be unwise
     * to rely on this or put important things here; for now it is only
     * used as a convenient place from which to print post-mortem
     * diagnostics.
     */
    public void vmShuttingDown() {
    }

    /**
     * Return the configured MemoryManager. Callsites of this method are
     * rewritten to load-constant of the (singleton) memory manager, courtesy
     * of the {@link ovm.core.stitcher.InvisibleStitcher}.
     * @return Singleton instance of the configured MemoryManager implementation
     * @throws PragmaStitchSingleton <em>this is a pragma</em>
     **/
    public static MemoryManager the()
        throws InvisibleStitcher.PragmaStitchSingleton {

        return (MemoryManager) InvisibleStitcher.singletonFor(
            MemoryManager.class.getName());
    }

    /**
     * Allocate a scalar.
     * Convenience version implemented in terms of
     * {@link #getMem getMem}, for memory managers that provide such
     * a raw interface and do not distinguish between allocating scalars and
     * arrays. 
     * If that is not possible, the memory manager should override this
     * method and supply a throws-unsupported version of getMem.
     * @param bp To stamp the allocation; override to do type-specific 
     * allocation
     * @return stamped, zeroed, allocated memory
     */
    public Oop allocate( Blueprint.Scalar bp) {
        return bp.stamp(getMem(bp.getFixedSize()));
    }

    /**
     * Allocate an array.
     * Convenience version implemented in terms of
     * {@link #getMem getMem}, for memory managers that provide such
     * a raw interface and do not distinguish between allocating scalars and
     * arrays.
     * If that is not possible, the memory manager should override this
     * method and supply a throws-unsupported version of getMem.
     * @param bp To stamp the allocation; override to do type-specific 
     * allocation
     * @param len number of elements 
     * @return stamped, zeroed, allocated memory
     */
    public Oop allocateArray(Blueprint.Array bp, int len) {
	long size = bp.computeSizeFor(len);
	if (size < Integer.MAX_VALUE)
	    return bp.stamp(getMem((int) size), len);
	else
	    throw outOfMemory();
    }

    public Oop clone(Oop oop) {
        Blueprint bp = oop.getBlueprint();
        return bp.clone(oop, getMem(bp.getVariableSize(oop)));
    }
    
    /** Ignore PragmaNoBarriers and force barriers everywhere?  (Default is no)  */
    public boolean forceBarriers() {
	return false;
    }

    public boolean needsWriteBarrier() {
 	return false;
    }

    public boolean needsReadBarrier() {
 	return false;
    }

    public boolean needsAcmpBarrier() {
 	return false;
    }

    // if false, the manager can still need aastore barrier (needsWriteBarrier)
    public boolean needsArrayAccessBarriers() {
        return false;
    }

    public boolean needsBrooksTranslatingBarrier() {
 	return false;
    }

    public boolean needsReplicatingTranslatingBarrier() {
 	return false;
    }

    public void registerMonitor( Monitor monitor, VM_Address owner ) {};

    /* assertions about pointers */
    public class Assert {
      public static final int NULL = 1;
      public static final int NONNULL = 2;
      public static final int IMAGEONLY = 4;
      public static final int HEAPONLY = 8;
      public static final int IN_IMAGEONLY_SLOT = 16;
      
      public static final int SCALAR = 64;
      public static final int ARRAY = 128;
      public static final int ADDRESS = 256; /* VM_Address ... */

      public static final int ARRAY_LENGTH_KNOWN = 512;
      public static final int ARRAY_INDEX_KNOWN = 1024;
      public static final int ARRAY_UP_TO_SINGLE_ARRAYLET = 2048;
    }


    public void putFieldBarrier(CoreServicesAccess csa,
				Oop src, int offset, Oop tgt) {
	throw new OVMError.Unimplemented("write barrier enabled but not defined!");
    }

    public void putFieldBarrier(CoreServicesAccess csa,
				Oop src, int offset, Oop tgt, int aSrc, int aTgt) {
        putFieldBarrier(csa,src,offset, tgt);  /* fallback to barrier that does not support assertions */	
    }
    
    public void aastoreBarrier(CoreServicesAccess csa,
			       Oop src, int offset, Oop tgt) {
	throw new OVMError.Unimplemented("write barrier enabled but not defined!");
    }


    public void aastoreBarrier(CoreServicesAccess csa,
			       Oop src, int offset, Oop tgt, int aSrc, int aTgt) {
	aastoreBarrier(csa,src,offset,tgt); /* fallback to barrier that does not support assertions */	
    }

    // !!! must handle addresses correctly, assertions are needed for correctness here
    // returns nonzero if not equal, otherwise returns zero
    public int acmpneBarrier(CoreServicesAccess csa,
			       Oop v1, Oop v2, int aV1, int aV2) {
        return  (acmpeqBarrier( csa, v1, v2, aV1, aV2 ) != 0) ? 0 : 1; // FIXME: can we do faster ?
    }

    // !!! must handle addresses correctly, assertions are needed for correctness here
    // returns nonzero if equal, otherwise returns zero
    public int acmpeqBarrier(CoreServicesAccess csa,
			       Oop v1, Oop v2, int aV1, int aV2) {

        if ( (aV1&Assert.NULL)!=0 && (aV2&Assert.NULL)!=0 ) {
          return 1;  // null == null
        }

        if ( ((aV1&Assert.NONNULL)!=0 && (aV2&Assert.NULL)!=0) ||
            ((aV1&Assert.NULL)!=0 && (aV2&Assert.NONNULL)!=0) ) {
          
          return 0; // null != !null
        }
                
        // return (VM_Address.fromObject(v1).asInt() == VM_Address.fromObject(v2).asInt()) ? 1 : 0;
        return VM_Address.fromObject(v1).asInt() - VM_Address.fromObject(v2).asInt();
    }

    
    public void readBarrier(CoreServicesAccess csa,
			    Oop src) {
	throw new OVMError.Unimplemented("read barrier enabled but not defined!");
    }

    public Oop checkingTranslatingReadBarrier(CoreServicesAccess csa,
			    Oop src, int aSrc) {
	throw new OVMError.Unimplemented("eager translating read barrier enabled but not defined!");
    }

    public Oop nonCheckingTranslatingReadBarrier(CoreServicesAccess csa,
			    Oop src, int aSrc) {
	throw new OVMError.Unimplemented("lazy translating read barrier enabled but not defined!");
    }

    public void copyOverlapping(Oop array, int soff, int doff, int nelt) {
	Mem.the().copyOverlapping(array, soff, doff, nelt);
    }
    
    public void copyArrayElements(Oop fromArray, int fromOffset,
				  Oop toArray, int toOffset, 
				  int nElems) {
	Mem.the().copyArrayElements(fromArray, fromOffset,
				    toArray, toOffset,
				    nElems);
    }

    public void setReferenceField( Oop object, int offset, Oop src ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setReferenceField not implemented");
    };
    public void setPrimitiveField( Oop object, int offset, boolean value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveField for boolean not implemented");
    };
    public void setPrimitiveField( Oop object, int offset, byte value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveField for byte not implemented");
    };    
    public void setPrimitiveField( Oop object, int offset, short value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveField for short not implemented");
    };    
    public void setPrimitiveField( Oop object, int offset, char value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveField for char not implemented");
    };    
    public void setPrimitiveField( Oop object, int offset, int value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveField for int not implemented");
    };    
    public void setPrimitiveField( Oop object, int offset, long value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveField for long not implemented");
    };            
    public void setPrimitiveField( Oop object, int offset, float value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveField for float not implemented");
    };        
    public void setPrimitiveField( Oop object, int offset, double value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveField for double not implemented");
    };        

    public void setPrimitiveArrayElement( Oop object, int index, int value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      setPrimitiveArrayElementAtByteOffset( object, ((Blueprint.Array)object.getBlueprint()).byteOffset(index), value );
    }
    
    public void setPrimitiveArrayElementAtByteOffset( Oop object, int byteOffset , int value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveArrayElement for int not implemented"); //? default to setPrimitiveField ?
    }

    public void setPrimitiveArrayElement( Oop object, int index, char value) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      setPrimitiveArrayElementAtByteOffset( object, ((Blueprint.Array)object.getBlueprint()).byteOffset(index), value );
    }
    
    public void setPrimitiveArrayElementAtByteOffset( Oop object, int byteOffset , char value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setPrimitiveArrayElement for char not implemented"); //? default to setPrimitiveField ?
    }

    public void setReferenceArrayElement( Oop object, int index, Oop value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      setReferenceArrayElementAtByteOffset( object, ((Blueprint.Array)object.getBlueprint()).byteOffset(index), value );
    }
    
    public void setReferenceArrayElementAtByteOffset( Oop object, int byteOffset , Oop value ) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("setReferenceArrayElement not implemented"); //? default to setPrimitiveField ?
    }
    
    public char getCharArrayElement( Oop array, int index )  throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      return VM_Address.fromObject( array ).add( array.getBlueprint().asArray().byteOffset(index) ).getChar();
    }

    public Oop getReferenceArrayElement( Oop array, int index) throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      return VM_Address.fromObject( array ).add( array.getBlueprint().asArray().byteOffset(index) ).getAddress().asOop();
    }
    
    public void aastoreBarrier(CoreServicesAccess csa, Oop src, int index, Oop tgt, int componentSize, int aSrc, int aTgt)  throws PragmaAssertNoExceptions, PragmaAssertNoSafePoints {
      throw new OVMError.Unimplemented("aastoreBarrier: not implemented");
    }
    
    public void bastoreBarrier(CoreServicesAccess csa, Oop src, int index, byte newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("bastoreBarrier: not implemented");
    }

    public void castoreBarrier(CoreServicesAccess csa, Oop src, int index, char newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("castoreBarrier: not implemented");
    }

    public void dastoreBarrier(CoreServicesAccess csa, Oop src, int index, double newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("dastoreBarrier: not implemented");
    }

    public void fastoreBarrier(CoreServicesAccess csa, Oop src, int index, float newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("dastoreBarrier: not implemented");
    }

    public void iastoreBarrier(CoreServicesAccess csa, Oop src, int index, int newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("iastoreBarrier: not implemented");
    }

    public void lastoreBarrier(CoreServicesAccess csa, Oop src, int index, long newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("lastoreBarrier: not implemented");
    }

    public void sastoreBarrier(CoreServicesAccess csa, Oop src, int index, short newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("sastoreBarrier: not implemented");
    }

    public Oop aaloadBarrier(CoreServicesAccess csa, Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("aaloadBarrier: not implemented");
    }

    public byte baloadBarrier(CoreServicesAccess csa, Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("baloadBarrier: not implemented");
    }

    public char caloadBarrier(CoreServicesAccess csa, Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("caloadBarrier: not implemented");
    }

    public double daloadBarrier(CoreServicesAccess csa, Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("daloadBarrier: not implemented");
    }

    public float faloadBarrier(CoreServicesAccess csa, Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("faloadBarrier: not implemented");
    }

    public int ialoadBarrier(CoreServicesAccess csa, Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("ialoadBarrier: not implemented");
    }

    public long laloadBarrier(CoreServicesAccess csa, Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("laloadBarrier: not implemented");
    }
    
    public short saloadBarrier(CoreServicesAccess csa, Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
      throw new OVMError.Unimplemented("saloadBarrier: not implemented");
    }
    

    /*
     * Some region support.  The default implemenation of these hooks
     * ignores regions.
     */
    
    /**
     * This hook is called immediately after a context switch occurs.
     * When switching to a new-born thread, it is called before any
     * memory is allocated in that thread
     *
     * @param context an object representing the new execution context.
     * This will either be the actual 
     * {@link ovm.core.execution.Context Context} object, or the
     * {@link ovm.core.services.threads.OVMThread} object, depending on the caller.
     */
    public void observeContextSwitch(Object context) {
    }
    
    public void assertAddressValid(VM_Address ptr) {
	// do nothing by default.
    }
    
    /**
     * Create the memory context assoicated with a newly created
     * ovm.core.execution.Context object, it can be retrieved with
     * ScopedMemoryContext.getMemoryContext(), and may be updated by
     * Context.setMemoryContext()
     */
    public Object makeMemoryContext() {
	return null;
    }

    /**
     * Make the given area current for this thread, and return
     * the area that once was current.  This function does not
     * enforce the RTSJ single parent rule.
     */
    public VM_Area setCurrentArea(VM_Area a) {
	return null;
    }
    
    public VM_Area setCurrentArea(int idx,VM_Area area) {
	if (idx!=0) {
	    throw new OVMError.Unimplemented("idx!=0 in setCurrentArea()");
	}
	return setCurrentArea(area);
    }

    public abstract VM_Area getHeapArea();

    public VM_Area getImmortalArea() {
	return null;
    }

    public VM_Area getCurrentArea() {
	return getHeapArea();
    }
    
    public VM_Area getCurrentArea(int idx) {
	if (idx!=0) {
	    throw Executive.panic("idx!=0 in getCurrentArea()");
	}
	return getCurrentArea();
    }

    public VM_Area areaOf(Oop oop) {
	return getHeapArea();
    }

    public boolean readBarriersEnabled() {
	return false;
    }

    public void enableReadBarriers(boolean val) {
	if (val)
	    throw Executive.panic("no heap checks in memory manager");
    }

    public boolean reallySupportNHRTT() {
	return false;
    }
    
    public boolean supportScopeAreaOf() {
	return false;
    }

    /**
     * Create a memory area that is not subject to scope checks.
     *
     * <p>If the memory area can't be allocated this implementation returns
     * null, but throwing <tt>OutOfMemoryError</tt> is also permitted.
     * 
     * @param size the size of the area. The actual size of the area may be
     * larger due to rounding/alignment or other imposed allocator constraints
     *
     * @return a new memory area
     *
     **/
    public VM_Area makeExplicitArea(int size) {
	return null;
    }
    
    /**
     * Create an area.  The type of this object is completely
     * opaque so that both JMTk and native region implementations can
     * use it.
     *
     * <p>If the memory area can't be allocated an implementation may return
     * null, or throw <tt>OutOfMemoryError</tt>.
     *
     * <p>This implementation always throws <tt>OVMError.Unimplemented</tt>
     * as scoped memory is not supported by default.
     *
     * @param mirror an arbitrary reference associated with this area 
     * @param size the size of the area. The actual size of the area may be
     * larger due to rounding/alignment or other imposed allocator constraints
     *
     */
    public VM_ScopedArea makeScopedArea(Oop mirror, int size) {
	throw new OVMError.Unimplemented
	    ("this allocator does not support scoped memory");
    }

    /**
     * Create a FIFO area.
     */
    // this is Filip's experimental stuff
    public VM_FIFOArea makeFIFOArea(Oop mirror,
                                    Oop pastMirror,
                                    Oop futureMirror,
                                    int size) {
	throw new OVMError.Unimplemented
	    ("this allocator does not support FIFO memory");
    }

    public void freeArea(VM_Area area) {
	throw new OVMError.Unimplemented("How was an area allocated here?");
    }

    /**
     * Allocate exactly size-many bytes of memory from the current
     * allocation context.  This is primary interface between Ovm and
     * a MemoryManager implementation, and not quite perfect.  size
     * should really be a VM_Word rather than an int, and we should
     * pass in the Blueprint being allocated, so that the memory
     * manager can respond more cleverly (for instance by giving
     * arrays or arrays of primitives special handling).
     **/
    protected abstract VM_Address getMem(int size);

    // A reasonable default, since we have 3 Managers that support GC,
    // but only one that doesn't
    public boolean supportsGC() { return true; }

    // FIXME: more managers need to support destructors, and the ones
    // that don't need to be rewritten to ignore addDestructor calls.
    public boolean supportsDestructors() { return false; }

    /**
     * The actual garbage collection implementation.
     * This method is called from the {@link LocalReferenceIterator}
     * when a gc is required.
     **/
    public abstract void doGC() throws PragmaNoInline;
    
    // If you override garbageCollect with a no-op, be sure to
    // override supportsGC as well.
    public abstract void garbageCollect() throws PragmaNoInline;

    public class ObjectCounter extends ExtentWalker.Scanner {
	int ntc = DomainDirectory.maxContextID() + 1;
	int[][] counts = new int[ntc][];
	int[][] bytes =  new int[ntc][];
	Blueprint[][] rev = new Blueprint[ntc][];

	public ObjectCounter() {
	    for (int i = 0; i < ntc; i++) {
		Type.Context ctx = DomainDirectory.getContext(i);
		if (ctx == null)
		    continue;
		int nbp = ctx.getBlueprintCount();
		counts[i] = new int[nbp + 1];
		bytes[i]  = new int[nbp + 1];
		rev[i] = new Blueprint[nbp];
	    }
	

	    for (Iterator dit = DomainDirectory.domains(); dit.hasNext(); ) {
		for (Iterator bit = ((Domain) dit.next()).getBlueprintIterator();
		     bit.hasNext();
		     ) {
		    Blueprint bp = (Blueprint) bit.next();
		    rev[bp.getCID()][bp.getUID()] = bp;
		}
	    }
	}

	public void walk(Oop oop, Blueprint bp) {
	    int i = bp.getCID();
	    int j = bp.isSharedState() ? counts[i].length - 1 : bp.getUID();
	    counts[i][j]++;
	    bytes[i][j] += bp.getVariableSize(oop);
	}

	public void dumpStats() {
	    BasicIO.out.println("OBJS\tBYTES\tBLUEPRINT");
	    for (int i = 0; i < ntc; i++) {
		if (counts[i] == null)
		    continue;
		for (int j = 0; j < counts[i].length - 1; j++)
		    if (counts[i][j] != 0)
			BasicIO.out.println(counts[i][j] + "\t" +
					    bytes[i][j] + "\t" +
					    rev[i][j]);
		int nbp = counts[i].length - 1;
		if (counts[i][nbp] != 0)
		    BasicIO.out.println(counts[i][nbp] + "\t" +
					bytes[i][nbp] + "\t" +
					DomainDirectory.getContext(i) + 
					" shared state");
	    }
	}
    }
    
    public void dumpExtent(VM_Address start, VM_Address end) {
	ObjectCounter oc = new ObjectCounter();
	oc.walk(start, end);
	oc.dumpStats();
    }

    public boolean needsGCThread() { return false; }
    public boolean usesPeriodicScheduler() { return false; }
    public boolean usesAperiodicScheduler() { return false; }    
    public boolean usesHybridScheduler() { return false; }    
    public boolean supportsPeriodicScheduler() { return false; }
    public boolean supportsAperiodicScheduler() { return false; }    
    public boolean supportsHybridScheduler() { return false; }        
    public void setPeriodicScheduler( boolean enabled ) {};
    public void setAperiodicScheduler( boolean enabled ) {};    
    public void setHybridScheduler( boolean enabled ) {};    

    public void setShouldPause(boolean shouldPause) {}
    public void runGCThread() {}
    
    /** can a garbage collection be triggered <i>right now<i>?  some
	collectors (like triPizlo) cannot start to collect after UD
	initialization.  the ED, or other domains, may not know when
	the UD has had the chance to initialize the collector.  this
	method will tell you.  in particular, if this method returns
	false, you should assume that unbounded allocation will lead
	to OOME and a VM crash. */
    public boolean canCollect() { return supportsGC(); }
    
    /** hook used by VM to enable full tracing in the collector, if available */
    public void enableAllDebug() {}
    
    public boolean caresAboutAbortOnGcReentry() { return false; } 
    public void setAbortOnGcReentry(boolean abortOnGcReentry) {}

    public boolean caresAboutLongLatency() { return false; } 
    public void setLongLatency(long longLatency) {}
    
    public boolean caresAboutLongPause() { return false; } 
    public void setLongPause(long longPause) {}    
    
    public boolean caresAboutDisableConcurrency() { return false; }
    public void setDisableConcurrency(boolean disableConcurrency) {}
    
    public boolean caresAboutMarkUninterruptible() { return false; }
    public void setMarkUninterruptible(boolean markUninterruptible) {}
    
    public boolean caresAboutSweepUninterruptible() { return false; }
    public void setSweepUninterruptible(boolean sweepUninterruptible) {}

    public boolean caresAboutLogUninterruptible() { return false; }
    public void setLogUninterruptible(boolean logUninterruptible) {}

    public boolean caresAboutCompactUninterruptible() { return false; }
    public void setCompactUninterruptible(boolean compactUninterruptible) {}
    
    public boolean caresAboutEnableTimeTrace() { return false; }
    public void setEnableTimeTrace(boolean enableTimeTrace) {}

    public void enableSilentMode() {}
    
    public void enableAllocProfiling() {}
    public void enableProfileMemUsage() {}
    
    public boolean caresAboutStackUninterruptible() { return false; }
    public void setStackUninterruptible(boolean stackUninterruptible) {}
    
    public void setGCThreshold(long gcThreshold) {}
    
    public void setEffectiveMemorySize(long effectiveMemorySize) {}
    
    public void setCompactionThreshold(long compactionThreshold) {}
    
    public void setMutatorDisableThreshold(long mutatorDisableThreshold) {}
    
    public void enableSizeHisto(int sizeHistoSize) {}
    
    public boolean usesArraylets() {
      return false;
    }
    
    public int arrayletSize() {
      return -1;
    }
    
    public int sizeOfContinuousArray(Blueprint.Array bp, int length) {
      return -1;
    }
    
    public int continuousArrayBytesToData(Blueprint.Array bp, int length) {
      return -1;
    }
    
    public int arrayletPointersInArray(Blueprint.Array bp, int length) { 
      return -1;
    }
    
    public byte[] allocateContinuousByteArray(int length) {
      return new byte[length];
    }

    public VM_Address addressOfElement(VM_Address array, int index, int componentSize) {
      return array.asOop().getBlueprint().asArray().addressOfElement( array.asOopUnchecked(), index );
    }
    
    // debugging
    public void checkAccess(VM_Address ptr) {
    }
    public void assertSingleReplica(VM_Address ptr){
    }
    
    //I/O hack
    public Oop getOtherReplica( Oop obj ) {
      return obj;
    }
    
    
    public boolean hasArrayReplicas() {
      return false;
    }
    
    public void runNextThreadHook( OVMThread current, OVMThread next ) {};
    
    public void setInterruptibilityMask( String interruptibilityMask ) {};
    
    public void printAddress( VM_Address addr ) {
      Native.print_ptr(addr);
    }
}
