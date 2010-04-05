package ovm.core.services.memory; // SYNC PACKAGE NAME TO STRNG BELOW

import java.lang.reflect.Array;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Word.BCeq;
import ovm.core.services.memory.VM_Word.BCeq0;
import ovm.core.services.memory.VM_Word.BCiadd;
import ovm.core.services.memory.VM_Word.BCiconst_4;
import ovm.core.services.memory.VM_Word.BCisub;
import ovm.core.services.memory.VM_Word.BCne;
import ovm.core.services.memory.VM_Word.BCne0;
import ovm.core.services.memory.VM_Word.BCscmp;
import ovm.core.services.memory.VM_Word.BCsge;
import ovm.core.services.memory.VM_Word.BCsgt;
import ovm.core.services.memory.VM_Word.BCsle;
import ovm.core.services.memory.VM_Word.BCslt;
import ovm.core.services.memory.VM_Word.BCucmp;
import ovm.core.services.memory.VM_Word.BCuge;
import ovm.core.services.memory.VM_Word.BCugt;
import ovm.core.services.memory.VM_Word.BCule;
import ovm.core.services.memory.VM_Word.BCult;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.JVMConstants.DereferenceOps;
import ovm.services.bytecode.JVMConstants.InvokeSystemArguments;
import ovm.util.ByteBuffer;
import ovm.util.HashSet;
import ovm.util.OVMError;
import ovm.util.OVMRuntimeException;
import ovm.util.PragmaUnsafe;
import ovm.util.Set;
import ovm.util.UnsafeAccess;
import s3.core.domain.S3Blueprint;
import s3.services.bootimage.Ephemeral;
import s3.services.bootimage.ReflectionSupport;
import s3.util.PragmaTransformCallsiteIR;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.util.PragmaTransformCallsiteIR.BCnothing;
import s3.core.domain.MachineSizes;
import ovm.core.stitcher.InvisibleStitcher;
import s3.services.bootimage.GC;
import s3.services.bootimage.BootImage;

/**
 * Representation of an address in the VM.  The width in bits of this type
 * is not specified. No reference to this class is to exist in bytecode;
 * rewriting replaces all operations on VM_Address with primitive
 * operations on the appropriate primitive type. The behavior of this
 * class's methods when hosted is determined by the method bodies; the
 * native behavior is determined by substitute bytecode inlined at the call
 * site of any method in this class.
 *
 * <p /> <strong>Current design decision (revisitable):</strong> VM_Addresses
 * are not mutated when running hosted; arithmetic ops return new
 * ones. They could even be interned (org.gjt.cuspy.Interned?) but for now
 * are not. The alternative, mutating arithmetic, would probably be quite
 * ugly with code like <code>a.add(12).getWord()</code> flying around at
 * image build time.  {@link VM_Word}, however, takes the mutating
 * arithmetic approach. Should both classes act the same?
 * 
 * <p />Looking for atomic operations?  We couldn't reach agreement on making them
 * methods of VM_Address, so look in {@link AtomicOps} instead.
 *
 * @author Chapman Flack
 **/
public abstract class VM_Address implements Ephemeral, UnsafeAccess
{ // SYNC CLASS NAME TO STRING BELOW

    /** Controls the generation of logDefer messages, which Jan doesn't like.
     *  Other types of log messages are still governed by OVMBase.dbg_mode_on
     *  rather than this one.
     **/
    public static final boolean dbg_mode_on = /* OVMBase.dbg_mode_on */ false;

    public static final boolean FIAT_GET = true;

    public static TypeName.Scalar typeNameVMA =
	JavaNames.ovm_core_services_memory_VM_Address;

    /**
     * Expose the blueprint to ObjectModel.Proxy implementations,
     * which extend this class.
     **/
    protected Blueprint getBlueprint()
    	throws BCdead // should never have to rewrite this; useful only hosted
	{
    	    if ( bp != null ) {
    		return bp;
	    }
    	    throw new ClassCastException( toString());
    	}

    protected void setBlueprint(Blueprint bp) {
	assert (this.bp == bp);
    }

    private static VM_Address NULL = VM_Address.fromInt(0);
    
    /**
     * Value of the address, <em>only when hosted</em>.  During image
     * writing, references to VM_Address are <em>replaced</em> with the
     * contents of this field from the referenced VM_Address; operations on
     * them are primitive bytecode inlined at call sites.
     **/
    private int value;

    /** Only when hosted, the host vm object (if any) for which this Address
     * was created.
     **/
    Object source;

    /** OVM-specific: Only when hosted, the Blueprint associated with the
     * object (cached by fromObject to later support the OVM-specific and
     * still somewhat out-of-place getBlueprint method).
     **/
    Blueprint bp;

    /**
     * True if this address object should be treated as an unbound
     * symbol.
     * @see #makeUnbound
     * @see #bind
     **/
    private boolean isUnbound;

    /**
     * True if the object at this address is pinned.  Pinned objects
     * should never be removed by the {@link GC}.
     **/
    private boolean isPinned;

    /**
     * Mark bit.
     * @see GC
     **/
    private boolean isMarked;

    /**
     * Set this flag to trace stores.  Currently unused.
     **/
    private boolean interesting_ = false;

    /** Only hosted: the memory image to which all of this class's
     * allocation and dereference methods apply.
     **/
    private static ByteBuffer memory_;

    /** Only hosted: map from host objects to VM_Addresses that have been
     * assigned them.
     **/
    private static Map map_;

    /**
     * We compute the image base address on demand to keep the
     * MemoryManager out of the s3.services.bootimage bootstrapping
     * sequence.
     * @see s3.services.bootimage
     **/
    private static int base = -1;
    private static final int headerSkip = BootImage.HEADER_SIZE;

    /**
     * Return the address of the first object within the bootimage.
     * Only hosted and ovm-specific.
     */
    private static int baseAddress() throws BCdead {
	if (base == -1)
	    base = MemoryManager.the().getImageBaseAddress();
	return base + headerSkip;
    }

    /**
     * Hosted operation: define the bootimage in terms of a bytebuffer
     * (used to hold the image contents), and an IdentityHashMap used
     * to map hosted objects to their addresses.
     */
    public static void initialize(ByteBuffer mem, IdentityHashMap map) {
	memory_ = mem;
	map_ = map;
    }

    /** Return the width of an Address, in bytes. **/
    public static final int widthInBytes()
        throws BCiconst_4
        { return 4; }

    //        //
    // Casts: //
    //        //

    /** VM_Address from Object.
     * Hosted: returns a VM_Address object representing allocated space in
     * the image and associated with the object (allocated when an object
     * is seen the first time).
     * <p>To grok the recursion-busting stuff added to this method and
     * {@link #setAddress(VM_Address) setAddress}, see also the comments
     * in ovm.core.domain.Blueprint.Scalar#allocate(Allocator).
     * KLB: not found: Blueprint.Scalar.allocate() doesn't exist...
     * @param o an Object. <code>null</code> is allowed, and gets you the
     * singleton VM_Address representing null.
     * @return native: <em>o</em>'s VM_Address; hosted: VM_Address representing
     * space allocated for <em>o</em> in image.
     * @throws BCnothing <em>this is a pragma</em>
     **/


    public static final VM_Address fromObject(Object o)
        throws BCfiatretwbar,          // native action
        ovm.core.services.memory.PragmaNoBarriers
    {
                                    // hosted action:
        return fromObjectHosted(o);
    }

    public static final VM_Address fromObjectNB(Object o)
        throws BCfiatret,          // native action
        ovm.core.services.memory.PragmaNoBarriers
    {                              // hosted action:
        return fromObjectHosted(o);
    }
    
    public static final VM_Address fromObjectHosted(Object o)
    {                              // hosted action:
	if (o == null)
	    return NULL;
	else if (o instanceof VM_Address)
            return (VM_Address) o;

 	VM_Address ret = (VM_Address) map_.get(o);
 	if (ret != null) {
	    assert ret.source == o;
 	    return ret;
	} else {
	    Blueprint bp;
	    try {
		bp = blueprintFor(o.getClass());
	    } catch (LinkageException e) {
		throw e.unchecked();
	    }
	    return fromObjectBlueprint(o, bp);
	}
    }

    /**
     * OVM-specific: <em>unsafe</em> cast of VM_Address to Oop (the OVM type
     * for any object reference when we are agnostic what sort of object it
     * may be, but want only methods to obtain and manipulate its header
     * (e.g. type) information.
     * @see #asAnyOop()
     **/
    public final Oop asOop()
//        throws BCfiatret
        throws BCfiatretwrbar
    {	// tag with arbitrary ref type (Oop)
	return (isNull() ? null : (Oop) this);
    }

    public final Oop asOopUnchecked()
        throws BCfiatret
    {	// tag with arbitrary ref type (Oop)
	return (isNull() ? null : (Oop) this);
    }

    /**
     * OVM-specific: VM_Address for Object with a specified Blueprint.
     * Hosted: returns a VM_Address object representing allocated space in
     * the image and associated with the object (allocated when an object
     * is seen the first time).
     * @param o an Object
     * @param bp the Blueprint to use for allocating the object, in preference
     * to what would be returned by <code>blueprintFor(o)</code>.
     * @return native: <em>o</em>'s VM_Address; hosted: VM_Address representing
     * space allocated for <em>o</em> in image.
     * @throws BCdead <em>this is a pragma</em>
     **/
    public static final VM_Address fromObject(Object o, Blueprint bp)
        throws BCdead           // disallowed to native bytecode
    {                           // hosted action:
	if ( o == null)
	    return NULL;
        else if ( o instanceof VM_Address )
            return (VM_Address) o;

	VM_Address ret = fromObjectBlueprint( o, bp);
	assert (ret.bp == bp);
	return ret;
    }

    /**
     * Treat references to the object o as references to an unbound
     * label somewhere in the program's address space.  Before the
     * image build completes, all unbound addresses created by this
     * method should be resolved by calling {@link #bind}. 
     **/
    public static final VM_Address makeUnbound(Object o) throws BCdead {
	VM_Address a = fromObject(o);
	assert (a.value == 0);
	a.isUnbound = true;
	return a;
    }

    /**
     * Bind this address label to an absolute address in the VM under
     * construction.
     **/
    public void bind(int value) throws BCdead {
	assert (isUnbound);
	this.value = value;
	if (source != null) {
	    isPinned = true;
	    GC.the().addRoot(source);
	}
	isUnbound = false;
    }

    /**
     * Return true if this is the address of a "real" object that will
     * become part of a compiled virtual machine's bootimage.
     **/
    public boolean inBootImage() throws BCdead {
	return source != null && !isUnbound;
    }

    /**
     * Toggle this object's mark bit.
     **/
    public boolean setMarkBit(boolean newValue) throws BCdead {
	boolean ret = isMarked;
	isMarked = newValue;
	return ret;
    }

    public boolean isMarked() throws BCdead {
	return isMarked;
    }

    public boolean isPinned() throws BCdead {
	return isPinned;
    }

    /**
     * <em>Unsafe</em> cast of VM_Address to Object.  Hosted: return the
     * Object from which this VM_Address was created, if there is one.
     * <em>It is the caller's responsibility to use this method only on a
     * VM_Address known to point to an Object (i.e. with a filled in
     * header).</em>
     * @return native: this address unsafely cast to Object; hosted: the Object
     * (if any) mapped to this VM_Address (this method is safe when hosted,
     * and only then).
     * @throws ClassCastException only when hosted, if this VM_Address was not
     * obtained by mapping an Object into the image space
     * @throws BCnothing <em>this is a pragma</em>
     * @see #asAnyObject()
     **/
    public final Object asObject()
//        throws BCfiatret
        throws BCfiatretwrbar
    {
        if ( source == null  &&  isNonNull() )
           throw new ClassCastException();
        return source;
    }
    
    /**
     * Unsafe cast building block: you immediately cast the return
     * value of this method to any object type, and the
     * <code>checkcast</code> is replaced by a <code>fiat</code> in
     * rewriting, resulting in an <em>unchecked, unsafe</em> cast to
     * the desired type. If you simply want this address as an Object,
     * and not to cast it unchecked to some more specific type, just
     * use {@link #asObject()}.
     *<p>
     * It is possible, if not likely, that the source-to-bytecode
     * compiler might not put your checkcast immediately after this
     * method invocation even if you did in the source. At present,
     * rewriting will simply log a message (and not eat anything) if
     * the immediately following instruction is not a checkcast. Worst
     * case, that will leave you with a checked cast where you
     * expected unchecked, and if that becomes a problem (i.e. if any
     * source-to-bytecode compiler actually does this) the rewriting
     * should be updated to follow flow until it finds the
     * checkcast. I doubt the added complexity will ever be necessary
     * in practice.
     *<p>
     * <em>What the pragma really does is, if the next bytecode is a
     * checkcast, insert a goto around it; this idea (thanks to CG)
     * preserves correctness of any other flow that might happen to
     * converge at the checkcast, without requiring any analysis. The
     * cost of the goto is not quite zero when interpreted, but
     * certainly less than the checkcast, and when compiled the
     * optimizer should easily finish it off.</em>
     * @throws BCfiatcast <em>this is a pragma</em>
     **/
    public final Object asAnyObject()
//      	throws BCfiatcast
        	throws BCfiatcastwrbar
	{ return asObject(); }
    
    /** Unsafe cast building block to anything that is or extends Oop; complete the
     *  idiom (as for {@link #asAnyObject()}) by following this method with a cast to
     *  the desired type, which will disappear.  If you simply want to have this
     *  address as an Oop, without magically casting it to a more specific type,
     *  just use {@link #asOop()}.
     *  <p />Hosted, this method is subtly
     *  different from {@link #asAnyObject()} cast to an Oop type: asAnyObject
     *  assumes you want to manipulate the associated build-time object,
     *  asAnyOop will give you the VM_Address itself (as an Oop). At run time
     *  there is no difference.
     *  @throws BCfiatcast <em>this is a pragma</em>
     **/
    public final Oop asAnyOop()
//      	throws BCfiatcast
      	throws BCfiatcastwrbar

    { return asOop(); }

    /**
     * VM_Address from integer. This address is always considered absolute;
     * the parameter is not relative to the bootimage base or anything else.
     *   If you genuinely need this, explain why.
     * @return a new VM_Address; never the null address, even if the parameter
     * is zero.
     * @throws BCnothing <em>this is a pragma</em>
     **/
    public static final VM_Address fromInt( int i)
        throws BCfiatret, PragmaUnsafe     // native action
        {                                  // hosted action
            VM_Address result = ObjectModel.getObjectModel().newInstance();
            result.value = i;
            return result;
        }

    /**
     * integer from VM_Address.
     *  If you genuinely need this, explain why.
     * @throws BCnothing <em>this is a pragma</em>
     **/

    public final int asInt()
    throws BCfiatret, PragmaUnsafe, ovm.core.services.memory.PragmaNoBarriers {

	assert(!isUnbound);
	if (value == 0 && source != null) {
	    GC.the().addRoot(source);
	    isPinned = true;
	    
            // attempt the allocation
	    VM_Address adr;
	    int size;
            if ( bp instanceof Blueprint.Array ) {
            
        //  a = (VM_Address)((Blueprint.Array) bp).allocate( Array.getLength( o),
        //                                                   allocator_);
                int length = Array.getLength(source);
		// Hopefully, we will not have boot images larger than 2G!
		
		if (MemoryManager.the().usesArraylets()) {
		  size = MemoryManager.the().sizeOfContinuousArray( (Blueprint.Array)bp, length );
		  if (false) {
  		    System.err.println("size with arraylets = "+size+" without = "+(int) ((Blueprint.Array)bp).computeSizeFor(length) +
		    " this = "+this+" array blueprint = "+bp+" nArraylets = "+MemoryManager.the().arrayletPointersInArray( (Blueprint.Array)bp, length)+
		    " length = "+length);
                  }
		} else {
  		  size = (int) ((Blueprint.Array)bp).computeSizeFor(length);
                }
                
		int absPos = baseAddress() + memory_.position();
		value = ImageAllocator.the().allocateInImage(absPos, size, bp,
							     length);
		assert(value >= absPos);
		if (asRelativeInt() >= memory_.position())
		    memory_.position(asRelativeInt() + size);
		else if (asRelativeInt() + size >= memory_.position())
		    throw new Error("object " + source + "@" +
				    Integer.toHexString(asInt()) +
				    " overlaps with " + lastObject +
				    " end = " +
				    Integer.toHexString(baseAddress() + memory_.position()));
		lastObject = source;
		Oop obj = ((Blueprint.Array)bp).stamp(this, length);
		assert (obj == this);
		
		if (MemoryManager.the().usesArraylets()) { // initialize arraylets
		
		  Blueprint.Array abp = (Blueprint.Array)bp;
		  
                  int skip = ObjectModel.getObjectModel().headerSkipBytes()+MachineSizes.BYTES_IN_WORD;
                  int arrayletSize = MemoryManager.the().arrayletSize();
                  int nArraylets = MemoryManager.the().arrayletPointersInArray(abp, length);
                  int bytesToData = MemoryManager.the().continuousArrayBytesToData(abp, length);
                  
                  for(int i=0; i< nArraylets; i++) {
                    memory_.putInt( asRelativeInt() + skip + i*MachineSizes.BYTES_IN_WORD, 
                        asInt() + bytesToData + i*arrayletSize );
                  }
		}
		
            }  else{ // I'll bet you a ClassCastException it's gotta be a Scalar
            
                //a = (VM_Address)((Blueprint.Scalar) bp).allocate( allocator_);
		size = bp.getFixedSize();
		int absPos = baseAddress() + memory_.position();
		value = ImageAllocator.the().allocateInImage(absPos, size, bp, 0);
		if (asRelativeInt() >= memory_.position())
		    memory_.position(asRelativeInt() + size);
		else if (asRelativeInt() + size >= memory_.position())
		    throw new Error("object " + source + "@" +
				    Integer.toHexString(asInt()) +
				    " overlaps with " + lastObject +
				    " end = " +
				    Integer.toHexString(baseAddress() + memory_.position()));
		lastObject = source;
		Oop obj = ((Blueprint.Scalar)bp).stamp(this);
		assert (obj == this);
	    }

          if (false) {
            System.err.println("\nI at "+ Integer.toHexString(value) + " bp "+ bp );
          }
	}
	
	return value;
    }

    private int asRelativeInt() {
	return asInt() - baseAddress();
    }

    /**
     * VM_Word from VM_Address.
     * @throws BCnothing <em>this is a pragma</em>
     **/

    public final VM_Word asWord()
        throws BCfiatret
        { return VM_Word.fromInt( asInt()); }

    //                    //
    // Dereference: Fetch //
    //                    //

    /**
     * Dereference this VM_Address and fetch a byte.
     * @return the byte fetched.
     * @throws BCgetbyte <em>this is a pragma</em>
     **/
    public final byte getByte()
        throws BCgetbyte
        { nullCk(); return memory_.get( asRelativeInt()); }

    /**
     * Dereference this VM_Address and fetch a short.
     * @return the short fetched.
     * @throws BCgetshort <em>this is a pragma</em>
     **/
    public final short getShort()
        throws BCgetshort
        { nullCk(); return memory_.getShort( asRelativeInt()); }

    /**
     * Implementation detail: somewhat unconvincing: store into a reference array
     * without checking (when hosted, the store is checked), using the
     * uncheckedAASTORE instruction in OvmIR; Christian put this method here;
     * I'm not sure VM_Address is where it belongs.
     * @throws BCuncheckedAASTORE
     */
    public static final void uncheckedAASTORE(Object[] arr, int index, Object value)
	throws BCuncheckedAASTORE {
	arr[index] = value; // hosted implementation -- checked, but it can't be helped. Pray.
    }

    /**
     * Dereference this VM_Address and fetch a char.
     * @return the char fetched.
     * @throws BCgetshort <em>this is a pragma</em>
     **/
    public final char getChar()
        throws BCgetchar
        { nullCk(); return memory_.getChar( asRelativeInt()); }

    /**
     * Dereference this VM_Address and fetch an int.
     * @return the int fetched.
     * @throws BCgetword <em>this is a pragma</em>
     **/
    public final int getInt()
        throws BCgetword
        { nullCk(); return memory_.getInt( asRelativeInt()); }

    /**
     * Dereference this VM_Address and fetch a long.
     * @return the long fetched.
     * @throws BCgetwide <em>this is a pragma</em>
     **/
    public final long getLong()
        throws BCgetwide
        { nullCk(); return memory_.getLong( asRelativeInt()); }

    /**
     * Dereference this VM_Address and fetch a float.
     * @return the float fetched.
     * @throws BCgetword <em>this is a pragma</em>
     **/
    public final float getFloat()
        throws BCgetfloat
        { nullCk(); return memory_.getFloat( asRelativeInt()); }

    /**
     * Dereference this VM_Address and fetch a double.
     * @return the double fetched.
     * @throws BCgetwide <em>this is a pragma</em>
     **/
    public final double getDouble()
        throws BCgetdouble
        { nullCk(); return memory_.getDouble( asRelativeInt()); }

    /**
     * Dereference this VM_Address and fetch a VM_Word.
     * @return the VM_Word fetched.
     * @throws BCgetword <em>this is a pragma</em>
     **/
    public final VM_Word getWord()
        throws BCgetword
        { nullCk(); return VM_Word.fromInt( memory_.getInt( asRelativeInt())); }

    /**
     * Dereference this VM_Address and fetch a VM_Address.
     * Hosted behavior not yet implemented (needs integration with image
     * memory).
     * @return the VM_Address fetched.
     * @throws BCgetword <em>this is a pragma</em>
     **/
    public final VM_Address getAddress()
        throws BCgetword
        { nullCk(); throw new UnsupportedOperationException(); }
    
    //                    //
    // Dereference: Store //
    //                    //

    /**
     * Dereference this VM_Address and store a byte.
     * Hosted, the argument value is written to the image memory, but <em>only
     * if it is non-zero</em> (assumes image memory is initially zeroed, and
     * the writing of constant pools depends on this convention).
     * @param content the byte to store.
     * @throws BCsetbyte <em>this is a pragma</em>
     **/
    public final void setByte( byte content)
        throws BCsetbyte
    {
        logStore( "Byte", content); // see OVMBase.dbg_mode_on
        nullCk();
        if (content != 0) memory_.put( asRelativeInt() , content);
    }

    /**
     * Dereference this VM_Address and store a short.
     * Hosted, the argument value is written to the image memory, but <em>only
     * if it is non-zero</em> (assumes image memory is initially zeroed, and
     * the writing of constant pools depends on this convention).
     * @param content the short to store.
     * @throws BCsetshort <em>this is a pragma</em>
     **/
    public final void setShort( short content)
        throws BCsetshort
    {
        logStore( "Short", content); // see OVMBase.dbg_mode_on
        nullCk();
        if (content != 0) memory_.putShort( asRelativeInt() , content);
    }

    /**
     * Dereference this VM_Address and store a char.
     * Hosted, the argument value is written to the image memory, but <em>only
     * if it is non-zero</em> (assumes image memory is initially zeroed, and
     * the writing of constant pools depends on this convention).
     * @param content the char to store.
     * @throws BCsetshort <em>this is a pragma</em>
     **/
    public final void setChar( char content)
        throws BCsetshort
    {
        logStore( "Char", content); // see OVMBase.dbg_mode_on
        nullCk();
        if (content != 0) memory_.putChar( asRelativeInt() , content);
    }

    /**
     * Dereference this VM_Address and store an int.
     * Hosted, the argument value is written to the image memory, but <em>only
     * if it is non-zero</em> (assumes image memory is initially zeroed, and
     * the writing of constant pools depends on this convention).
     * @param content the int to store.
     * @throws BCsetword <em>this is a pragma</em>
     **/
    public final void setInt( int content)
        throws BCsetword
    {
        logStore( "Int", content); // see OVMBase.dbg_mode_on
        nullCk();
        if (content != 0) memory_.putInt( asRelativeInt() , content);
    }

    /**
     * Dereference this VM_Address and store a long.
     * Hosted, the argument value is written to the image memory, but <em>only
     * if it is non-zero</em> (assumes image memory is initially zeroed, and
     * the writing of constant pools depends on this convention).
     * @param content the long to store.
     * @throws BCsetwide <em>this is a pragma</em>
     **/
    public final void setLong( long content)
        throws BCsetwide
    {
        logStore( "Long", content); // see OVMBase.dbg_mode_on
        nullCk();
        if (content != 0) memory_.putLong( asRelativeInt() , content);
    }

    /**
     * Dereference this VM_Address and store a float.
     * Hosted, the argument value is written to the image memory, but <em>only
     * if it is non-zero</em> (assumes image memory is initially zeroed, and
     * the writing of constant pools depends on this convention).
     * @param content the float to store.
     * @throws BCsetword <em>this is a pragma</em>
     **/
    public final void setFloat( float content)
        throws BCsetword
    {
        logStore( "Float", content); // see OVMBase.dbg_mode_on
        nullCk();
        if (content != 0) memory_.putFloat( asRelativeInt() , content);
    }

    /**
     * Dereference this VM_Address and store a double.
     * Hosted, the argument value is written to the image memory, but <em>only
     * if it is non-zero</em> (assumes image memory is initially zeroed, and
     * the writing of constant pools depends on this convention).
     * @param content the double to store.
     * @throws BCsetwide <em>this is a pragma</em>
     **/
    public final void setDouble( double content)
        throws BCsetwide
    {
        logStore( "Double", content); // see OVMBase.dbg_mode_on
        nullCk();
        if (content != 0) memory_.putDouble( asRelativeInt() , content);
    }

    /**
     * Dereference this VM_Address and store a VM_Word.
     * <strong>Hosted</strong>, the VM_Word argument's <em>value</em> is
     * written to the image memory, but <em>only if it is non-zero</em>
     * (assumes image memory is initially zeroed, and the writing of constant
     * pools depends on this convention).
     * @param content the VM_Word to store.
     * @throws BCsetword <em>this is a pragma</em>
     **/
    public final void setWord( VM_Word content)
        throws BCsetword
    {
        logStore( "Word", content); // see OVMBase.dbg_mode_on
        nullCk();
        if (content.asInt() != 0)
            memory_.putInt( asRelativeInt(), content.asInt());
    }

    /**
     * Dereference this VM_Address and store a VM_Address.
     * <strong>Hosted</strong>, this method:
     * <ul><li>
     *       Will not store a zero/null value (assumes image memory is
     *       initially zeroed, and the writing of constant pools depends on this
     *       convention)</li>
     * <li>
     *       Will transparently arrange, if either this (destination) address,
     *       or the address (value) to be stored, can not yet be resolved to
     *       an absolute int value, to complete the operation when the values
     *       are available.</li>
     * <li>
     *       Otherwise, writes the <em>value</em> of the VM_Address
     *       argument.</li>
     * </ul>
     * @param content the VM_Address to store.
     * @throws BCsetword <em>this is a pragma</em>
     **/
    public final void setAddress( VM_Address content)
        throws BCsetword
    {
        logStore( "Address", content); // see OVMBase.dbg_mode_on
        nullCk();
        if ( content.isNull() ) // this is null, no need to write.
            return;

	// this is an offset in the bootimage (zero based)
	int bootImageOffset = asRelativeInt();

	// this is an offset in memory (baseAddress based)
	int valueAsInt = content.asInt();
	memory_.putInt(bootImageOffset,  valueAsInt);
    }
    
    /**
     * Copy contiguous bytes from a given source address to this address.
     * The sense of semantic equivalence between the native and hosted case is that
     * only primitive arrays are supported while hosted, but as long as a primitive
     * array is to be copied, the same call with the same arguments would be used
     * to copy it in either the hosted or native case.
     * <p>
     * <strong>Native:</strong> The source and destination blocks are allowed
     * to overlap (semantics of C <code>memmove</code>).
     * <p>
     * <strong>Hosted:</strong> There are severe restrictions on use of this
     * method under the host VM. Both this (destination) address and the source
     * address must represent an array (i.e. the <em>base</em> of the array object,
     * not the interior address of a component),
     * the component sizes must match, the source array component type must be
     * primitive, <em>bytes</em> may exceed the fixed (header) size obtained from
     * the destination array's Blueprint only by a multiple of the component size,
     * and the multiple may not be greater than the length of either
     * array. If all those conditions are met, the components only (not the header)
     * from the host VM object corresponding to the source are
     * copied into the image memory corresponding to the destination. Components
     * with value zero are not copied (image memory is assumed initially zero,
     * and copying of constant pools depends on this behavior).
     * Though the destination address is the base of the object, the header portion
     * is not written; it does not, of course, correspond to the hosted source, and
     * should have been stamped correctly by the image allocator.
     * @param source Address of start of source block
     * @param bytes Number of bytes to copy
     * @throws BCsetblock <em>this is a pragma</em>
     **/
     
    // FIXME: with arraylets, this method is assumed to be only called to write
    //	data of whole arrays when hosted ; this is because we need to know the 
    //	length of the array to write it ; this assumption seems to hold, so there
    //	should be no problem
     
    public final void setBlock( VM_Address source, VM_Word bytes)
      	throws BCsetblock
    {
      	int size = bytes.asInt();
	if ( VM_Word.fromInt( size).NE( bytes) )
	    OVMBase.failUnimplemented( "hosted setBlock size > signed int");
	
	Object srcObject = source.asObject();
	Class  srcClass  = srcObject.getClass();
	Blueprint dstBp  = asOop().getBlueprint();
	
	if ( ! ( srcClass.isArray()  &&  dstBp instanceof Blueprint.Array ) )
	    OVMBase.failUnimplemented( "hosted setBlock on non-array");
	
	Class srcComponentClass = srcClass.getComponentType();
	
	if ( ! srcComponentClass.isPrimitive() )
	    OVMBase.failUnimplemented( "hosted setBlock from reference array");
	    // why? handling references requires remapping, which is the
	    // job of Walkabout/ISerializer and should not gunk up VM_Address

	int componentSize; // from src, but assert equal to dst size below, too.
	try { 
	    componentSize = ((Blueprint.Array)blueprintFor(srcClass)).getComponentSize();
	} catch (LinkageException e) {
	    throw e.unchecked();
	}
	
	S3Blueprint.Array dstArrayBp = (S3Blueprint.Array)dstBp; // want comp sz
	if ( componentSize != dstArrayBp.getComponentSize() )
	    OVMBase.failUnimplemented(
	      	"hosted setBlock mismatched src/dst component sizes");

        // this is just to calculate the data size of the array and the component
        // size ; it is recalculated for arraylets later
        
	int skip = dstArrayBp.getUnpaddedFixedSize();
	
	/* this is now already part of getUnpaddedFixedSize - yes, it makes no sense
        if (componentSize > MachineSizes.BYTES_IN_WORD) {
          skip = (skip + MachineSizes.BYTES_IN_WORD*2 -1) & ~ (MachineSizes.BYTES_IN_WORD*2 -1);
        }
        */
	size -= skip;
	
	int components = size / componentSize;
	
	if ( size != components * componentSize )
	    OVMBase.failUnimplemented(
	      	"hosted setBlock size not multiple of component size");
	
	if (false) {
  	  System.err.println("setBlock: src_length="+Array.getLength( srcObject)+" dst_length="+ dstArrayBp.getLength( asOop()) +
	  " components="+ components+" this="+this+ "size="+size+" dst_address="+asInt()+" componentSize "+componentSize);
        }
	  
	if ( components > Array.getLength( srcObject) )
	    throw new ArrayIndexOutOfBoundsException(
	      	"hosted setBlock overruns source array");
	
	if ( components > dstArrayBp.getLength( asOop()) )
	    throw new ArrayIndexOutOfBoundsException(
	      	"hosted setBlock overruns destination array");

        if (MemoryManager.the().usesArraylets()) {
        
          // note that the arraylet pointers are initialized in asInt()
          skip = MemoryManager.the().continuousArrayBytesToData( dstArrayBp, components );
        }

        int offset = asRelativeInt() + skip;
	
	if ( srcClass == byte[].class ) {
            byte[] array = (byte[]) srcObject;
            for ( int i = 0; i < components; ++ i ) {
                byte v = array[i];
		if ( v != 0 ) memory_.put( offset, v);
		offset += componentSize;
            }
        } else if ( srcClass == int[].class ) {
            int[] array = (int[]) srcObject;
            for ( int i = 0; i < components; ++ i ) {
                int v = array[i];
		if ( v != 0 ) memory_.putInt( offset, v);
		offset += componentSize;
            }
        } else if ( srcClass == boolean[].class ) {
            boolean[] array = (boolean[]) srcObject;
            for ( int i = 0; i < components; ++ i ) {
                boolean v = array[i];
		if ( v ) memory_.put( offset, (byte)1);
		offset += componentSize;
            }
        } else if ( srcClass == short[].class ) {
            short[] array = (short[]) srcObject;
            for ( int i = 0; i < components; ++ i ) {
                short v = array[i];
		if ( v != 0 ) memory_.putShort( offset, v);
		offset += componentSize;
            }
        } else if ( srcClass == char[].class ) {
            char[] array = (char[]) srcObject;
            for ( int i = 0; i < components; ++ i ) {
                char v = array[i];
		if ( v != 0 ) memory_.putChar( offset, v);
		offset += componentSize;
            }
        } else if ( srcClass == float[].class ) {
            float[] array = (float[]) srcObject;
            for ( int i = 0; i < components; ++ i ) {
                float v = array[i];
		if ( v != 0 ) memory_.putFloat( offset, v);
		offset += componentSize;
            }
        } else if ( srcClass == long[].class ) {
            long[] array = (long[]) srcObject;
            for ( int i = 0; i < components; ++ i ) {
                long v = array[i];
		if ( v != 0 ) memory_.putLong( offset, v);
		offset += componentSize;
            }
        } else if ( srcClass == double[].class ) {
            double[] array = (double[]) srcObject;
            for ( int i = 0; i < components; ++ i ) {
                double v = array[i];
		if ( v != 0 ) memory_.putDouble( offset, v);
		offset += componentSize;
            }
        } else OVMBase.fail( "not reached");
    }

    //              //
    // Arithmetic:  //
    //              //

    /**
     * Twos-complement add with wraparound, no overflow detect.
     * @return Address+increment (hosted: another Address, unless the
     * increment is zero; this is not mutated)
     * @throws BCiadd <em>this is a pragma</em>
     **/
    public final VM_Address add(VM_Word increment)
        throws BCiadd
        { return weakFromInt( asInt() + increment.asInt()); }

    /**
     * Twos-complement add with wraparound, no overflow detect.
     * @return Address+increment (hosted: another Address, unless the
     * increment is zero; this is not mutated)
     * @throws BCiadd <em>this is a pragma</em>
     **/
    public final VM_Address add( int increment)
        throws BCiadd
        { return weakFromInt( asInt() + increment); }

    /**
     * Twos-complement subtract with wraparound, no overflow detect.
     * @return Address-subtrahend (hosted: another Address, unless the
     * subtrahend is zero; this is not mutated)
     * @throws BCisub <em>this is a pragma</em>
     **/
    public final VM_Address sub( VM_Word subtrahend)
        throws BCisub
        { return weakFromInt( asInt() - subtrahend.value); }

    /**
     * Twos-complement subtract with wraparound, no overflow detect.
     * @return Address-subtrahend (hosted: another Address, unless the
     * subtrahend is zero; this is not mutated)
     * @throws BCisub <em>this is a pragma</em>
     **/
    public final VM_Address sub( int subtrahend)
        throws BCisub
        { return weakFromInt( asInt() - subtrahend); }

    /**
     * Twos-complement subtract with wraparound, no overflow detect.
     * @return VM_Word representing the difference <code>this - other</code>.
     * @throws BCisub <em>this is a pragma</em>
     **/
    public final VM_Word diff( VM_Address other)
        throws BCisub
        {
            return VM_Word.fromInt( asInt()
                                  - other.asInt());
        }
    // Word.fromInt method is deprecated for ordinary purposes, but this is
    // a legitimate purpose supporting the happy interface that makes it
    // unnecessary for ordinary ones.

    //              //
    // Comparisons: //
    //              //

    /**
     * Null predicate.
     * @return true iff this address is (hosted: represents) the null reference
     * @throws BCeq0 <em>this is a pragma</em>
     **/
    public final boolean isNull()
        throws BCeq0
    { return source == null && value == 0; }

    /**
     * Nonnull predicate.
     * @return false iff this address is (hosted: represents) the null reference
     * @throws BCne0 <em>this is a pragma</em>
     **/
    public final boolean isNonNull()
        throws BCne0
    { return ! isNull(); }

    /**
     * Pointer equality.
     * @return true iff both addresses (hosted: their values) are equal
     * @throws BCeq <em>this is a pragma</em>
     **/
    public final boolean EQ( VM_Address other)
        throws BCeq
        {
            return this == other || asInt() == other.asInt();
        }

    /**
     * Value inequality.
     * @return true iff the addresses (hosted: their values) are not equal
     * @throws BCne <em>this is a pragma</em>
     **/
    public final boolean NE( VM_Address other)
        throws BCne
        {
            return this == other || asInt() != other.asInt();
        }

    /**
     * Value strictly less (signed).
     * @return true iff this address (hosted: value) strictly less than other
     * in signed comparison.
     * @throws BCslt <em>this is a pragma</em>
     **/
    public final boolean sLT( VM_Address other)
        throws BCslt
        {
            return asInt() < other.asInt();
        }

    /**
     * Value weakly less (signed).
     * @return true iff this address (hosted: value) weakly less than other
     * in signed comparison.
     * @throws BCsle <em>this is a pragma</em>
     **/
    public final boolean sLE( VM_Address other)
        throws BCsle
        {
            return asInt() <= other.asInt();
        }

    /**
     * Value weakly greater (signed).
     * @return true iff this address (hosted: value) weakly greater than other
     * in signed comparison.
     * @throws BCsge <em>this is a pragma</em>
     **/
    public final boolean sGE( VM_Address other)
        throws BCsge
        {
            return asInt() >= other.asInt();
        }

    /**
     * Value strictly greater (signed).
     * @return true iff this address (hosted: value) strictly greater than other
     * in signed comparison.
     * @throws BCsgt <em>this is a pragma</em>
     **/
    public final boolean sGT( VM_Address other)
        throws BCsgt
        {
            return asInt() > other.asInt();
        }

    /**
     * Signed compare ordering.
     * @return -1, 0, or 1 as this Address is less than, equal to, or greater
     * than other.
     * @throws BCscmp <em>this is a pragma</em>
     **/
    public final int sCMP( VM_Address other)
      	throws BCscmp
	{
            int lhs = asInt();
	    int rhs = other.asInt();
	    return lhs == rhs ? 0 : lhs < rhs ? -1 : 1;
	}
    
    /**
     * Unsigned compare ordering.
     * @return -1, 0, or 1 as this Address is below, equal to, or above other.
     * @throws BCucmp <em>this is a pragma</em>
     **/
    public final int uCMP( VM_Address other)
      	throws BCucmp
	{ return EQ( other) ? 0 : uLT( other) ? -1 : 1; }
    
    /**
     * Value strictly less (unsigned).
     * @return true iff this address (hosted: value) strictly less than other
     * in unsigned comparison.
     * @throws BCult <em>this is a pragma</em>
     **/
    public final boolean uLT( VM_Address other)
        throws BCult
        {
            int lhs = asInt();
            int rhs = other.asInt();
            return (lhs ^ rhs) >= 0 ? lhs < rhs
                                    : lhs > rhs;
        }

    /**
     * Value weakly less (unsigned).
     * @return true iff this address (hosted: value) weakly less than other
     * in unsigned comparison.
     * @throws BCule <em>this is a pragma</em>
     **/
    public final boolean uLE( VM_Address other)
        throws BCule
        {
            int lhs = asInt();
            int rhs = other.asInt();
            return (lhs ^ rhs) >= 0 ? lhs <= rhs
                                    : lhs >  rhs;
        }

    /**
     * Value weakly greater (unsigned).
     * @return true iff this address (hosted: value) weakly greater than other
     * in unsigned comparison.
     * @throws BCuge <em>this is a pragma</em>
     **/
    public final boolean uGE( VM_Address other)
        throws BCuge
        {
            int lhs = asInt();
            int rhs = other.asInt();
            return (lhs ^ rhs) >= 0 ? lhs >= rhs
                                    : lhs <  rhs;
        }

    /**
     * Value strictly greater (unsigned).
     * @return true iff this address (hosted: value) strictly greater than other
     * in unsigned comparison.
     * @throws BCugt <em>this is a pragma</em>
     **/
    public final boolean uGT( VM_Address other)
        throws BCugt
        {
            int lhs = asInt();
            int rhs = other.asInt();
            return (lhs ^ rhs) >= 0 ? lhs > rhs
                                    : lhs < rhs;
        }

    //                 //
    // Helper methods: //
    //                 //

    /** Used by the arithmetic methods: return <code>this</code> if it has
     *  the desired value, otherwise create and return a new VM_Address with
     *  that value (instead of mutating <code>this</code>).
     * <p />This method propagates the &lquo;Unresolved Relative&rquo; property of
     * the receiver, if it creates a new object. This assumes that the value
     * passed as the argument comes from a computation involving exactly one
     * VM_Address, and that address is <code>this</code>.
     * Interestingness is also propagated.
     */
    private VM_Address weakFromInt( int i) {
      	if ( i == value )
            return this;
        VM_Address a = fromInt( i);
        return a;
    }
    
    private static Object lastObject;
    
    /** Implementation detail: implementation factor of the various public
     *  fromObject flavors.
     **/
    private static VM_Address fromObjectBlueprint( Object o, Blueprint bp)
	throws BCdead {
        VM_Address a = (VM_Address) map_.get(o);
	if (a != null) {
	    assert (a.source == o);
	    return a;
	} else {
	    a = ObjectModel.getObjectModel().newInstance();
	    a.source = o;
	    a.bp = bp;
	    if (GC.the().rootsFrozen)
		throw new IllegalStateException("New address taken");
	    Object existing = map_.put(o, a);
	    assert (existing == null);
	    return a;
	}
    }

    /** implementation detail: only hosted: prevent dereferencing the
     *  distinguished Null address.
     **/
    private void nullCk() throws BCdead {
        if ( isNull() ) throw new NullPointerException();
    }

    /** Debugging: hosted only: log store to an interesting Address. **/
    private void logStore( String suffix, double content) throws BCdead {
        if ( OVMBase.dbg_mode_on  &&  interesting_ )
            d( "set"+suffix+": *"+this+" = "+content);
    }

    /** Debugging: hosted only: log store to or of an interesting Address. **/
    private void logStore( String suffix, VM_Address content) throws BCdead {
        if ( OVMBase.dbg_mode_on  &&
           ( interesting_  ||  content.interesting_ ) )
            d( "set"+suffix+": *"+this+" = "+content);
    }

    /** Debugging: hosted only: log store to an interesting Address. **/
    private void logStore( String suffix, Object content) throws BCdead {
        if ( OVMBase.dbg_mode_on  &&  interesting_ )
            d( "set"+suffix+": *"+this+" = "+content);
    }

    
    /** Debugging: hosted only: called by log methods to print the messages;
     *  easy common place to hang a jdb breakpoint.
     **/
    private static void d( String s) throws BCdead { OVMBase.d( s); }

    /** Hosted: return a String representation of this Address, indicating
     *  both the image allocation and the corresponding host object instance
     *  and class. Native: return this Address as an unsigned hex string
     *  preceded by an @ sign.
     **/
    public String toString() throws BCtostring {
        String tgt = "?", hst = "", txt = "?";

	if (value != 0)
	    tgt = Integer.toHexString( value);
        if ( source != null ) {
            hst = Integer.toHexString( System.identityHashCode( source)) + "/";
            txt = source.getClass().getName();
            Blueprint _bp = asOop().getBlueprint();
            if (_bp != null) {
                txt += "[" + _bp.getType().getDomain()  + "]";
            }
        }
        else if ( bp != null )
            txt = bp.toString();
        return "((tgt,hst)=(" + tgt + "," + hst + txt + "))";
    }

    //                                  //
    // Substitute bytecode definitions: //
    //                                  //

    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BC extends PragmaTransformCallsiteIR {
        static {
            r( "", null);
	    r( "uncheckedAASTORE", new byte[] { (byte) UNCHECKED_AASTORE });
            r( "getbyte" , new byte[] { (byte)INVOKE_SYSTEM,
	      	      	      	      	InvokeSystemArguments.DEREFERENCE,
					DereferenceOps.getByte });
            r( "getshort", new byte[] { (byte)INVOKE_SYSTEM,
	      	      	      	      	InvokeSystemArguments.DEREFERENCE,
					DereferenceOps.getShort });
            r( "getchar", new byte[] { (byte)INVOKE_SYSTEM,
	      	      	      	      	InvokeSystemArguments.DEREFERENCE,
					DereferenceOps.getChar });
            r( "getword" , new byte[] { (byte)GETFIELD_QUICK, 0, 0 });
	    r( "getfloat",
	       (FIAT_GET
		? new byte[] { (byte) GETFIELD_QUICK, 0, 0,
			       (byte) FFIAT }
		: new byte[] { (byte) GETFIELD_QUICK, 0, 0 }));
	       
            r( "getwide" , new byte[] { (byte)GETFIELD2_QUICK, 0, 0 });
	    r( "getdouble",
	       (FIAT_GET
		? new byte[] { (byte) GETFIELD2_QUICK, 0, 0,
			       (byte) DFIAT }
		: new byte[] { (byte) GETFIELD2_QUICK, 0, 0 }));
	       
            r( "setbyte" , new byte[] { (byte)INVOKE_SYSTEM,
	      	      	      	      	InvokeSystemArguments.DEREFERENCE,
					DereferenceOps.setByte });
            r( "setshort", new byte[] { (byte)INVOKE_SYSTEM,
	      	      	      	      	InvokeSystemArguments.DEREFERENCE,
					DereferenceOps.setShort });
            r( "setword", new byte[] { (byte)PUTFIELD_QUICK, 0, 0 });
            r( "setwide", new byte[] { (byte)PUTFIELD2_QUICK, 0, 0 });
            r( "setblock", new byte[] { (byte)INVOKE_SYSTEM,
	      	      	      	      	InvokeSystemArguments.DEREFERENCE,
					DereferenceOps.setBlock });
	    r( "fiatcast", new PragmaTransformCallsiteIR.Rewriter() {
                    protected boolean rewrite() {
                        Instruction.CHECKCAST inst = followingCheckCast();
			if ( null != inst ) {
                            TypeName castTypeName;
                            // no manipulation of code.position() is needed here;
                            // access to the istream_ins relies on code.getPC(),
                            // distinct from position() and already reflecting
                            // the last fetched instruction.
                            castTypeName = inst.getResultTypeName( code, cp);
			    int around = code.getPC() + inst.size( code);
			    cursor.addFiat( castTypeName);
			    cursor.addGoto( cfe.getMarkerAtPC( around));
                            return true;
			}
                        String offender =
                            code.get( code.getPC()).toString( code);
			throw new PuntException("expected cast, got: "+offender);
                    }
		});
	    r( "fiatcastwrbar", new PragmaTransformCallsiteIR.Rewriter() {
                    protected boolean rewrite() {
                        Instruction.CHECKCAST inst = followingCheckCast();
			if ( null != inst ) {
                            TypeName castTypeName;
                            // no manipulation of code.position() is needed here;
                            // access to the istream_ins relies on code.getPC(),
                            // distinct from position() and already reflecting
                            // the last fetched instruction.
                            castTypeName = inst.getResultTypeName( code, cp);
			    int around = code.getPC() + inst.size( code);
			    cursor.addFiat( castTypeName);
                            if (MemoryManager.the().needsReadBarrier()) { // this is for debugging
                              cursor.addSimpleInstruction( DUP ); 
                              cursor.addSimpleInstruction( READ_BARRIER ); 
                            }
			    cursor.addGoto( cfe.getMarkerAtPC( around));
                            return true;
			}
                        String offender =
                            code.get( code.getPC()).toString( code);
			throw new PuntException("expected cast, got: "+offender);
                    }
		});		
	    r( "fiatret", new PragmaTransformCallsiteIR.Rewriter() {
		    protected boolean rewrite() {
			addFiat();
			return true;
		    }
		});
	    r( "fiatretwrbar", new PragmaTransformCallsiteIR.Rewriter() {
		    protected boolean rewrite() {
			addFiat();
                        if (MemoryManager.the().needsReadBarrier()) { // this is for debugging
                          cursor.addSimpleInstruction( DUP );                         
                          cursor.addSimpleInstruction( READ_BARRIER ); 
                        }
			return true;
		    }
		});		
	    r( "fiatretwbar", new PragmaTransformCallsiteIR.Rewriter() {
		    protected boolean rewrite() {
		        if (MemoryManager.the().needsBrooksTranslatingBarrier()) { 
		          // check this condition on every change of the memory manager or
		          // the way VM_Address.fromObject is used ...
		          // FIXME: add run-time debugging check here to warn if there are two copies
		          //   of the object when this executes
  		          cursor.addSimpleInstruction( CHECKING_TRANSLATING_READ_BARRIER ); 
                        }
			addFiat();
			return true;
		    }
		});		
            r( "tostring", new PragmaTransformCallsiteIR.Rewriter() {
                    final ConstantMethodref iths =
                        RepositoryUtils.selectorFor(
                            JavaNames.java_lang_Integer.getGemeinsamTypeName(),
                            JavaNames.java_lang_String,
                            "toHexString",
                            new TypeName[] { TypeName.INT });
                    final RepositoryString at =
                        new RepositoryString( "@");
                    protected boolean rewrite() {
                        cursor.addFiat( TypeName.INT);  // i
                        cursor.addINVOKESTATIC( iths);  // hex
                        cursor.addNEW( JavaNames.       // hex [sb]
                            java_lang_StringBuffer);
                        cursor.addSimpleInstruction( DUP); // hex [sb] [sb]
                        cursor.addLoadConstant( at);    // hex [sb] [sb] @
                        cursor.addINVOKESPECIAL( JavaNames.    // hex sb
                            java_lang_StringBuffer_STRING_Constructor);
                        cursor.addSimpleInstruction( SWAP); // sb hex
                        cursor.addINVOKEVIRTUAL( JavaNames.
                            java_lang_StringBuffer_STRING_append); // sb
                        cursor.addINVOKEVIRTUAL( JavaNames.
                            java_lang_Object_toString);
                        return true;
                    }
                });
        }
        /** OVM-specific: implementation detail: register substitute bytecode
         *  for a subpragma of this pragma.
         **/
        protected static void r( String name, Object bc) {
            // SYNCHRONIZE THIS STRING WITH CLASS/PACKAGE NAME CHANGES
            register( "ovm.core.services.memory.VM_Address$BC" + name, bc);
        }
        /** OVM-specific: implementation detail: register substitute bytecode
         *  (single byte) for a subpragma of this pragma.
         **/
        protected static void r( String name, int bc) {
            r( name, new byte[] { (byte)bc });
        }
    }

    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCgetbyte  extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCgetshort extends BC { }
    public static class BCgetchar  extends BC { }
    public static class BCuncheckedAASTORE extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCgetword  extends BC { }
    public static class BCgetfloat extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCgetwide  extends BC { }
    public static class BCgetdouble extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCsetbyte  extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCsetshort extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCsetword  extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCsetwide  extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCsetblock extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCfiatcast extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCfiatcastwrbar extends BC { }    
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCfiatret extends BC { }
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCfiatretwrbar extends BC { }    
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCfiatretwbar extends BC { }    
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCtostring extends BC { }

    private static Blueprint blueprintFor(Class cls) 
	throws LinkageException, BCbootTime {
	TypeName tn = ReflectionSupport.typeNameForClass(cls);
	Domain curDom = DomainDirectory.getExecutiveDomain();
	Type t = curDom.getSystemTypeContext().typeFor(tn);
	return curDom.blueprintFor(t);
    }
} // end of VM_Address
