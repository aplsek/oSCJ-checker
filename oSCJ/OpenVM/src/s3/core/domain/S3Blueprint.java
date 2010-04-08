package s3.core.domain;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.Domain;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.domain.ReferenceVisitor;
import ovm.core.domain.ResolvedConstant;
import ovm.core.domain.Type;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.repository.ConstantClass;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.JVMConstants;
import ovm.util.HTObject;
import ovm.util.Mem;
import ovm.util.OVMError;
import ovm.util.OVMRuntimeException;
import ovm.util.UnsafeAccess;
import s3.core.S3Base;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaTransformCallsiteIR.BCbadalloc;
import s3.util.PragmaNoInline;
import s3.util.PragmaMayNotLink;

/**
 * @author Krzysztof Palacz
 * @author Christian Grothoff
 * @author Jan Vitek
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 */
public abstract class S3Blueprint extends S3Base
    implements Blueprint, TypeCodes, ConstantClass, ResolvedConstant,
	       UnsafeAccess
{
    protected byte[] dbg_string;

    protected void set_dbg_string() {
          dbg_string = toString().getBytes(); 
//          dbg_string = null; 
    }
    
    public byte[] get_dbg_string() { return dbg_string; }
    
    protected HTUnboundSel2int vtableOffsets_ =  HTUnboundSel2int.EMPTY;
    protected HTSelector_Method2int nvtableOffsets_ = HTSelector_Method2int.EMPTY;
    /** The blueprint of the parent class. **/
    protected final S3Blueprint.Scalar parent_;
    // Fun once we support class unloading!
    protected S3Blueprint firstChild;
    protected S3Blueprint nextSibling;
    char ctxNumber;
    char number;

    /** Size of fixed-size instances (i.e. not arrays). */
    protected int size_;
    /**  The method table for objects made with this <code>Blueprint</code>. */
    protected Code[] vTable = Code.EMPTY_ARRAY;
    /**  The interface methods for objects made with this <code>Blueprint</code>. */
    protected Code[] ifTable;
    /**  For private methods and constructors or static methods. */
    protected Code[] nvTable;
    /** Used by the interpreter to dispatch CSA calls; */
    protected CoreServicesAccess myCSA;

    /* ************ JIT SUPPORT CODE STARTS HERE ************** */

    // simplejit -simplejitopt caches is_subtype_of in the following
    // two feields.
    // Last blueprint to pass subtype test for this type?
    private VM_Address lastsucc;
    // Last blueprint to fail subtype test for this type?
    private VM_Address lastfail;

    /* These methods are not polymorphic, which should result in
     * better code.
     */
    public boolean isArray()    throws PragmaNoPollcheck { return this instanceof Array;  }
    public boolean isScalar()   throws PragmaNoPollcheck { return this instanceof Scalar;  }
    public boolean isPrimitive() throws PragmaNoPollcheck{ return this instanceof Primitive; }

    public Blueprint.Array asArray()  throws PragmaNoPollcheck {  return (Blueprint.Array) this; }
    public Blueprint.Scalar asScalar() {  return (Blueprint.Scalar) this; }
    public Blueprint.Primitive asPrimitive() {
	return (Blueprint.Primitive) this;
    }

    // The native dispatch tables are allocated in the readonly data
    // segment.  During image-build we create a distinct objects for
    // each address we substitute.
    public VM_Address[] j2cVTable = new VM_Address[0];
    public VM_Address[] j2cIfTable = new VM_Address[0];

    // Subtype checks
    // bp1.typeDisplay_[bp2.typeBucket_] == bp2.typeBucketID_ <=> bp1 <: bp2
    protected byte[] typeDisplay_;      // The array where typeBucketID_ of supertypes live
    protected int typeBucket_;          // The index of typeDisplay_
    protected int typeBucketID_;        // values in typeDisplay_ at typeBucket_ in all subtypes
    protected int arrayDepth_;          // Dimension

    /**
     * For use with instance blueprints (shared state --static info--
     * already exists).
     * @param parent parent class  blueprint
     **/
    S3Blueprint(S3Blueprint.Scalar parent, Type t) {
        this.parent_ = parent;
	if (parent_ != null) {
	    nextSibling = parent_.firstChild;
	    parent_.firstChild = this;
	}
	myCSA = t.getDomain().getCoreServicesAccess();
	((S3TypeContext) t.getContext()).addBlueprint(this);
    }
  
    public boolean isResolved()  throws PragmaNoPollcheck {
	return true;
    }
    public byte getTag() {
	return JVMConstants.CONSTANT_ResolvedClass;
    }
    public TypeName asTypeName() {
	return getType().getUnrefinedName();
    }
    public String asTypeNameString() {
        return asTypeName().asTypeNameString();
    }
    public Blueprint asBlueprint() {
	return this;
    }

    public TypeName getName() {
        return getType().getUnrefinedName();
    }

    /**
     * @return a unique identifier within our domain
     */
    public final int getUID() {
	return number;
    }

    public final int getCID() {
	return ctxNumber;
    }

    /**
     * The basic requirement here is to return a number unique within
     * a domain.  Right now, we can go a step further by returning a
     * globablly unique number, but the 16 bit getUID() probably won't
     * be enough when classes can be unloaded.
     */
    public final int hashCode()  throws PragmaNoPollcheck {
	return (ctxNumber << 16) + number;
    }

    public Domain getDomain()  throws PragmaNoPollcheck {
        return getType().getDomain();
    }
    public S3Domain getS3Domain()  throws PragmaNoPollcheck {
	return (S3Domain) getDomain();
    }
    public int getVariableSize(Oop oop)  throws PragmaNoPollcheck {
        return getFixedSize();
    }
    // when asked, pretend the size is a multiple of 2 words
    public int getFixedSize()  throws PragmaNoPollcheck {
        return size_;
    }

    // The scanning logic that is now in CxxStructGenerator.dump() should be moved in this
    // class. In the meantime, let's leak there the unpadded size
    public int getUnpaddedFixedSize()  throws PragmaNoPollcheck {
        return size_;		// overridden in Scalar
    }

    /** Returns true for shared state object blueprints.  **/
    public final boolean isSharedState() {
        return getType().isSharedState();
    }
    /** Returns the shared state object for all instances of this blueprint.
     *  See OVMSharedStates on the wiki for suggestions to reduce the
     *  overhead of this naive first implementation.
     * <p />
     *  The semantics changed subtly in rev. 1.307.  See OVMSharedStates for
     *  the details.
     * <p />
     *  This method is now historical sugar; it asks the shared state type for
     *  its singleton, which is what the caller should be doing.
     **/
    public Oop getSharedState() {
        Type t = getType();
        if (t.isSharedState())
    	   return ((Type.Class)t).getSingleton();
        else
    	   return t.getSharedStateType().getSingleton();
    }

    // FIXME GC: disabled here?
    public Oop clone(Oop original, VM_Address dstAddr) {
        assert(original.getBlueprint() == this);
        // so, we won't write to the array length field twice 
        Oop clone = ObjectModel.getObjectModel().stamp(dstAddr, this);
	int skip = ObjectModel.getObjectModel().headerSkipBytes();
	int byteCnt = getVariableSize(original) - skip;
	Mem.the().cpy(clone, skip, original, skip, byteCnt);
        return clone;
    }

    /**How to follow the instance->shared-state blueprint pointer "backwards".
     * This is possible by existing means without needing an actual back
     * pointer or another map; but there are several steps, so it is now a
     * method.  This will allow ISerializer to handle statics without yet
     * another hash map, for example.
     **/
    public Blueprint getInstanceBlueprint() {
	assert isSharedState() : "Already instance: " + this ;
	Type type = getType();
	return type.getDomain().blueprintFor(type.getInstanceType());
    }

    // Promote primitive blueprints for use in fields/local variables
    public S3Blueprint promote() { return this; }

    public boolean isReference() { return false; }

    /**
     * Returns a blueprint for the direct supertype of this blueprint's
     * type.  If the current blueprint is the root of the hierarchy
     * (e.g. <code>java.lang.Object</code> in the JDK) null will be
     * returned.
     * @return blueprint for parent type or null if root
     **/
    public S3Blueprint.Scalar getParentBlueprint() {  return parent_;  }
    public Blueprint firstChildBlueprint() { return firstChild; }
    public Blueprint nextSiblingBlueprint() { return nextSibling; }

    public byte[] getTypeDisplay() { return typeDisplay_; }
    public int getTypeBucket()     { return typeBucket_; }
    public int getTypeBucketID()   { return typeBucketID_; }
    
    public void setSubtypeInfo(int typeBucket, int typeBucketID, byte[] display) {
        this.typeBucket_ = typeBucket;
        this.typeBucketID_ = typeBucketID;
        this.typeDisplay_ = display;
    }

    public boolean hasSubtypeInfo() {
	return typeBucketID_ != Subtyping.UNCOMPUTED;
    }

    public void ensureSubtypeInfo() {
	if (!hasSubtypeInfo())
	    recomputeSubtypeInfo();
    }

    private void recomputeSubtypeInfo()	throws PragmaMayNotLink {
	getS3Domain().subtyping.recomputeSubtypeInfo();
    }
    /**
     * If subtyping information has been computed, this method is much more 
     * efficient than Type.isSubTypeOf(). But, if subtyping information has
     * not been computed, this method is infinitely less efficient (it will 
     * often throw NullPointerExceptions).<p>
     *
     * Question: should this method automatically recompute subtype
     * info as needed?
     */
    public boolean isSubtypeOf(Blueprint _other) {
	S3Blueprint other = (S3Blueprint) _other;  // is typeDisplay initially null, or do we need an assert?
	ensureSubtypeInfo();
        other.ensureSubtypeInfo();
        return this == other
            || (other.typeBucket_ < typeDisplay_.length
                && typeDisplay_[other.typeBucket_] == other.typeBucketID_);
    }

     

    /**
     * Find the most-specific common supertypes of two blueprints.  We
     * rely on type displays and the reverse mapping in
     * S3Domain.findBlueprint(int, int) to come up with an answer more
     * quickly than you could using S3Type.  But, we still have
     * quadratic complexity on total number of common supertypes.
     *
     * @param other type to unify
     *
     * @return an array of all supertypes of this and other s.t. no
     * array element is a subtype of any other
     */
    public S3Blueprint[] leastCommonSupertypes(S3Blueprint other) {

        if (other.isSubtypeOf(this))  return new S3Blueprint[] { this };
        else if (isSubtypeOf(other))  return new S3Blueprint[] { other };
        else if (other instanceof Primitive) return new S3Blueprint[0];        
        
        Type tt = this.getType();
        Type to = other.getType();
        Type.Reference[] common = ((S3Type)tt).getLeastCommonSupertypes(to);
        S3Blueprint[] res = new S3Blueprint[common.length];
        for (int i = 0; i < res.length; i++)
	    res[i]= (S3Blueprint) getDomain().blueprintFor(common[i]);
        return res;
    }

    /**
     * Perform a reverse search on a dispatch table, more or less.
     * This method can also find the S3Method corresponding to an
     * interface invocation, even though interface blueprints don't
     * have ifTables.
     *
     * @param index     a dispatch table offset
     * @param tableName an interned string: "vTable", "ifTable", or "nvTable"
     *
     * @return the s3method defined at this location or null
     *
     */
    public S3Method lookupMethod(int index, String tableName) {
	Code[] table = (tableName == "vTable" ? 
		getVTable() : tableName == "ifTable" ? getIFTable() : getNVTable());
	if (tableName == "ifTable" && getType().isInterface())
	/* Interfaces don't have an ifTable.  Why should they? */
	{
	    Type.Scalar t = (Type.Scalar) getType();
	    DispatchBuilder db = getS3Domain().getDispatchBuilder();
	    UnboundSelector.Method usel = db.getInterfaceMethod(t, index); // FIXME See bug #513.
	    Method ret = t.getMethod(usel, true);
	    if (ret != null) return (S3Method) ret;
	    // Hmm.  We may have inherited this interface method
	    // multiple times.  Do we care about returning some "best" version?
	    Type.Interface[] allIfs = t.getAllInterfaces();
	    for (int i = 0; i < allIfs.length; i++) {
		ret = allIfs[i].getMethod(usel, true);
		if (ret != null) return (S3Method) ret;
	    }
	    return null;
	}

	if (index < 0 || index >= table.length) {
	    pln(this + ": " + tableName + "[" + index + "] not found");
	    return null;
	}

	Code cf = table[index];
	if (cf.getMethod()==null)
	    throw new OVMError.Internal(this + ": " + tableName + "[" + index + "] not found");
	return (S3Method) cf.getMethod();
    }

    
    /** This may exist, if not in exactly this form, at the conclusion of work on
     *  bug 417; the fieldID is an opaque integer unique to a field within a Type,
     *  and this method returns the address of that field within an Oop o
     *  <strong>assuming (without checking)</strong> that this blueprint is o's
     *  blueprint and corresponds to that Type.
     * <p />
     *  In the present implementation, offsets cannot vary between blueprints
     *  representing one Type, the fieldID returned by S3Type <em>is</em> the offset
     *  (shhh...), and this method is simply an add. FIXME 547
     * @param o An Oop assumed to be described by this blueprint
     * @param fieldID A field ID returned by
     *        {@link S3Type.Scalar#bug417fieldID(Field)}
     * @return address of the wanted field.
     **/
    // inlining this would be an obvious optimization. can use PragmaISB in the absence
    // of anything more automatic.
    VM_Address bug417addressWithin( Oop o, int fieldID) {
        return VM_Address.fromObject( o).add( fieldID);
    }

    //FIXME: remove
    /*
    VM_Address bug417addressWithinForwarded( Oop o, int fieldID) {
        MovingGC oop = (MovingGC)o.asAnyOop();
        o = oop.getForwardAddress().asOop()
        return VM_Address.fromObject( o).add( fieldID);
    }
    */
    HTUnboundSel2int getVTableOffsets() {
        return vtableOffsets_;
    }
    public int getVirtualMethodOffset(Method m) {
	// FIXME: package-private
	UnboundSelector.Method usel = m.getExternalSelector().getUnboundSelector();
	int ret = -1;

	for (S3Blueprint definer = this;
	     definer != null && ret == -1;
	     definer = definer.getParentBlueprint())
	    ret = definer.vtableOffsets_.get(usel);
	return ret;
    }
    public int getInterfaceMethodOffset(Method m) {
	UnboundSelector.Method usel = m.getExternalSelector().getUnboundSelector();
	return getS3Domain().getDispatchBuilder().getIFTableOffset(usel);
    }

    HTSelector_Method2int getNonVTableOffsets() {
        return nvtableOffsets_;
    }

    /** Get the VTBL for this Blueprint
     * @return the array of code fragments representing the VTBL
     **/
    public Code[] getVTable() {
        return vTable;
    }
    public Code[] getNVTable() {
        return nvTable;
    }

    /**
     * Return an array of method selectors corresponding in order to the
     * bytecode fragments in the Blueprint's vtable array. The selectors
     * will be bound, and wherever possible bound to the class
     * corresponding to this dispatcher. The exception is where a vtable
     * slot corresponds to a <em>hidden</em> method in a superclass--one
     * that is inaccessible and has been hidden by an accessible method of
     * the same name and signature.  Such hidden methods will get selectors
     * bound to the classes in which they were declared.
     * @return the array of method selectors in the order they appear in
     *         the Blueprint's vtable
     **/
    public Selector.Method[] getSelectors() throws BCbadalloc {
	Selector.Method[] selectors = new Selector.Method[vTable.length];
	HTObject collisions = new HTObject();
	for (S3Blueprint cur = this; cur != null; cur = cur.getParentBlueprint()) {

	    HTUnboundSel2int map = cur.vtableOffsets_;

	    HTUnboundSel2int.Iterator it = map.getIterator();

	    while (it.hasNext()) {
		UnboundSelector.Method me = it.next();
		Selector.Method bound = Selector.Method.make(me, this.getName().asCompound());

		int off = S3MemberResolver.resolveVTableOffsetNoAccessibility(cur, me);
		if (off == -1) { throw new OVMError(cur + " does not have " + me); }

		if (selectors[off] != null) continue;
		if (collisions.get(bound) != null) 
		    bound = Selector.Method.make(me, cur.getName().asCompound());
		
		collisions.put(bound);
		selectors[off] = bound;
	    }
	}
	return selectors;
    }

    private Field[] layout_ = null;

    /**
     * Return an array mapping each word in our layout to zero or one
     * fields.  A word may be null if it is occupied by the second
     * half of a wide value or because it is part of the object
     * header.  
     *<p />
     * This method is defined because you often want to know the
     * layout of an S3Blueprint.  This method exposes the fact that
     * everything is padded out to 4 byte boundaries, but code that
     * does anything useful with an S3Blueprint's layout should
     * already be aware of that fact.
     *<p />
     * This method does not belong in Blueprint because it is very
     * specific to the S3Blueprint implementation.
     *<p />
     * So far nobody uses this method except fieldAt below, and CxxStructGen.
     * This API encodes too many assumptions.
     */
    public Field[] getLayout() throws BCbadalloc {
        if (layout_ == null) {
            if ( parent_ != null 
		 &&  (getUnpaddedFixedSize() == parent_.getUnpaddedFixedSize()))
                layout_ = parent_.getLayout();
            else {
                // iterator includes inherited fields, separate step not needed
                Field.Iterator it = getType().asCompound().fieldIterator();
		// layout_ needs to have as many words as those returned
		// by getFixedSize(), CxxStructGen depends on it.
                layout_ = new Field [ getFixedSize() / 4 ];
                while (it.hasNext()) {
                    Field f = it.next();
                    // FIXME 547
                    int off = ((S3Type.Scalar)getType()).bug417fieldID( f);
                    layout_[off / 4] = f;
                }
            }
        }
        return layout_;
    }

    /**
     * This is a variant of the above layout_/getLayout() for local
     * fields. This is used by Class.getDeclaredFields() -HY, JT
     *
     * UNUSED -- jv
    private Field[] localLayout_ = null;
    public Field[] getLocalLayout() throws BCbadalloc {
        if (localLayout_ == null) {
	    // iterator DOES NOT include inherited fields, separate step not needed
	    Field.Iterator it = getType().asCompound().localFieldIterator();
	    localLayout_ = new Field [ getFixedSize() / 4 ];
	    while (it.hasNext()) {
		Field f = it.next();
		// FIXME 547
		int off = ((S3Type.Scalar)getType()).bug417fieldID( f);
		localLayout_[off / 4] = f;
	    }
        }
        return localLayout_;
    }
    */
    
    /**
     * Return the field stored at a particular offset in this
     * blueprint.  (Does this find inherited fields?)  Return the
     * field's Selector, or null if no field was found.
     * @param offset offset of field from base of object
     * @return the Selector.Field naming this field
     */
    public Field fieldAt(int offset) {
        if (layout_ == null)
            getLayout();
        if (offset % 4 != 0)
            return null;
        offset /= 4;
        if (offset >= layout_.length)
            return null;
        return layout_[offset];
    }

    /**
     * Get the interface method table for this Blueprint
     * @return the array of code fragments representing the interface
     *         method table*/
    public Code[] getIFTable() { return ifTable;  }

    private int[] refmap;
    public int[] getRefMap()   { return refmap;   }
    public void setRefMap(int[] map) { refmap = map; }

//    private int[] refmask;
//    public int[] getRefMask()  { return refmask; }

/*
    public void refMaskSet(int offset) {
      int woffset = offset / MachineSizes.BYTES_IN_WORD;
      refmask[woffset >> 5] |= 1 << woffset;
    }
    
    public void refMaskClear(int offset) {
      int woffset = offset / MachineSizes.BYTES_IN_WORD;
      refmask[woffset >> 5] &= ~(1 << woffset);
    }
    
    public boolean refMaskGet(int offset) {
      int woffset = offset / MachineSizes.BYTES_IN_WORD;
      return (refmask[woffset >> 5] & (1 << woffset)) != 0;
    }
*/
    void generateRefMap(S3Type.Scalar type) {
	int[] parentRefs = (parent_ != null ? parent_.getRefMap() : new int[0]);
	int refs = 0;
	int nFields = type.localFieldCount();

        for (int i = 0; i < nFields; i++) {
	    if (type.getLocalField(i) instanceof Field.Reference) 
		refs++;
        }

	if (refs == 0) {
	    refmap = parentRefs;
// 	    refmask = (parent_ != null ? parent_.getRefMask() : new int[0]);
	} else {
	    this.refmap = new int[parentRefs.length + refs];
	
//	    int maxoffset = 0;
	    
	    for (int i = 0; i < parentRefs.length; i++) {
		refmap[i] = parentRefs[i];
//                if (refmap[i]>maxoffset) {
//                  maxoffset = refmap[i];
//                }
            }

	    for (int i = 0, j = parentRefs.length; i < nFields; i++) {
		S3Field f = type.getLocalField(i);
		if (f instanceof Field.Reference) {
		    int o = f.getOffset();
		    refmap[j++] = o;
//		    if (o>maxoffset) {
//		      maxoffset = o;
//		    }
                }
	    }
// 	    int maskWordBytes = 32 * MachineSizes.BYTES_IN_WORD;
//	    this.refmask = new int[(maxoffset + 1+ maskWordBytes - 1)
// 				   / maskWordBytes];
// 	    for (int i = 0; i < refmap.length; i++) {
//              refMaskSet(refmap[i]);
// 	    }
	}
    }

    int allocKind=0;
    public int getAllocKind() { return allocKind; }
    public void setAllocKind(int allocKind) { this.allocKind=allocKind; }

    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    //-------------------------Primitive-------------------------------
    //-----------------------------------------------------------------
    //-----------------------------------------------------------------

    /**
     * Blueprint for the primitive types.
     **/
    public static class Primitive
        extends S3Blueprint
        implements Blueprint.Primitive, TypeCodes {

        /** The type. **/
        private final Type.Primitive type_;

	public S3Blueprint promote()
	{
	    if (size_ < 4)
		return (S3Blueprint)
			getS3Domain().blueprintFor(getS3Domain().INT);
	    else
		return this;
	}
    
        /**
         * Create a new primitive blueprint.
         * @param tn the typename object for this primitive type
         * @param type the type object for this primitive tyoe
         **/
        public Primitive(TypeName.Primitive tn, Type.Primitive type) {
            super(null, type);
            type_ = type;
            switch (tn.getTypeTag()) {
                case VOID :
                    size_ = 0;
                    break;
                case BOOLEAN :
                case BYTE :
                case UBYTE :
                    size_ = 1;
                    break;
                case CHAR :
                case SHORT :
                case USHORT :
                    size_ = 2;
                    break;
                case FLOAT :
                case INT :
                case UINT :
                    size_ = MachineSizes.BYTES_IN_WORD;
                    break;
                case LONG :
                case ULONG :
                case DOUBLE :
                    size_ = MachineSizes.BYTES_IN_DOUBLE_WORD;
                    break;
                default :
                    throw failure("Unsupported type code: " + tn.getTypeTag());
            }
	    set_dbg_string();
        }

        public Type getType() {
            return type_;
        }
        public Code[] getVTable() {
            throw new NoMethodsError();
        }
        public Code[] getIFTable() {
            throw new NoMethodsError();
        }
      
        /** Get a <code>String</code> representation of the typename
         * associated with this blueprint.
         **/
        public String toString() {
            return "Bpt.Primitive{" + type_ + "}";
        }
        // FIXME? There are subtype relationships between integral types and between
        // floating-point types, but these
        // relationships don't apply to array element types and aren't
        // encoded in the typeDisplay anyway.
        public boolean isSubtypeOf(Blueprint other) {
            return this == other;
        }
        /** Primitives have no references. **/
        public void visitReferencesOf(Oop record, ReferenceVisitor visitor) {
        }

        /** Error to be thrown when actions are taken on primitive
         * blueprints which expect the blueprint to contain methods or data
         * structures that support methods. **/
        public static class NoMethodsError extends OVMError {
            public NoMethodsError() {
                super("primitives have no methods");
            }
        }
    } // end of S3Blueprint.Primitive


    //-----------------------------------------------------------------
    //---------------------------Scalar--------------------------------
    //-----------------------------------------------------------------

    /**
     * Blueprints for objects which are not arrays.
     * @author K. Palacz, Christian Grothoff
     **/
    public static class Scalar
        extends S3Blueprint
        implements Blueprint.Scalar {

        /**The <code>Type</code> object for the type this blueprint represents.**/
        private final Type.Scalar type_;

        /**
         * Create a new blueprint for a non-array reference type. Without a parent.
         * @param type  the represented type
            **/
        Scalar(Type.Scalar type) {
	    super(null, type);
            type_ = type;
            init(type);
        }
        /**
           * Create a new blueprint for a non-array reference type.
           * @param type  the represented type
           * @param parent blueprint of the parent class
           **/
        public Scalar(Type.Scalar type, S3Blueprint.Scalar parent) {
            super(parent, type);
            type_ = type;
            init(type);
	    if (type.isSharedState()) {
		vTable = parent.vTable;
		ifTable = parent.ifTable;
		j2cVTable = parent.j2cVTable;
		j2cIfTable = parent.j2cIfTable;
	    }
        }

	public int getUnpaddedFixedSize() {
	    return ((S3Type.Scalar) type_).getLogicalEnd();
	}

        private void init(Type.Scalar type) throws PragmaMayNotLink {
            assert(type == type_);
            // try to check that we are only called once on the cheap.
            S3Domain dom = (S3Domain) type.getDomain();
            DispatchBuilder dbuilder = dom.getDispatchBuilder();
            dbuilder.setBlueprint(this);
            // create sharedState
            nvTable = dbuilder.createNVTable();
	    generateRefMap((S3Type.Scalar) type);
	    int logSize = getUnpaddedFixedSize();
	    size_ = ((logSize + MachineSizes.BYTES_IN_WORD*2 -1) 
		     & ~ (MachineSizes.BYTES_IN_WORD*2 -1));
	    if (!isSharedState()) {
                vTable = dbuilder.createVTable();
                assert(vTable != null);
                try {
                    ifTable = dbuilder.createIFTable();
                } catch (LinkageException e) {
                    throw e.fatal("While linking interfaces for " + this);
                }
            }
            vtableOffsets_ = dbuilder.getVTableOffsets();
            nvtableOffsets_ = dbuilder.getNonVTableOffsets();
            dbuilder.reset();
	    set_dbg_string();
        }

        public Type getType() {
            return type_;
        }
	public boolean isReference()
	{ return true; }

        public String toString() {
            return isSharedState()
                ? "ShStBpt{" + getType() + "}"
                : "Bpt{" + getType() + "}";
        }

     

        public Oop stamp(VM_Address obj)  throws PragmaNoPollcheck {
            return ObjectModel.getObjectModel().stamp(obj, this);
        }
    } // end of S3Blueprint.Scalar

    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    //-------------------------Array-----------------------------------
    //-----------------------------------------------------------------
    //-----------------------------------------------------------------

    /**
     * The class for actual array object blueprints.
     **/
    static public class Array extends S3Blueprint implements Blueprint.Array {
	static private final int LENGTH_FIELD_IN_BYTES = 
	    MachineSizes.BYTES_IN_WORD;

        private final int lengthFieldOffset_;
        /** The <code>Type</code> for this array **/
        private final Type.Array type_;
        /**The size of a component in this array **/
        private final int componentSize_;
        /** The <code>Blueprint</code> for component objects **/
        private final S3Blueprint componentBlueprint_;

	public boolean isReference() { return true; }

        public boolean isSubtypeOf(Blueprint other)  throws PragmaNoPollcheck {
            return (this == other ? true
		    : other instanceof S3Blueprint.Array
                    ? componentBlueprint_.isSubtypeOf(
                        ((S3Blueprint.Array) other).componentBlueprint_)
                    : super.isSubtypeOf(other));
            // In Java, match Object, Serializable, and Cloneable
        } 
        
        /**
         * Create an array blueprint.
         * @param type  the represented type
         * @param parent array's parent class blueprint
         * @param componentBlueprint the <code>Blueprint</code> for
         *        the component objects of this array blueprint
         **/
        public Array(
            Type.Array type,
            S3Blueprint.Scalar parent,
            S3Blueprint componentBlueprint) {

            super(parent, type);

            assert type != null : "Null Type.";
            this.type_ = type;
	    assert(componentBlueprint != null);
            this.componentBlueprint_ = componentBlueprint;
            Type comp = type.getComponentType(); // NOT innermost!
            assert(
                (type.getDepth() > 1)
                    ^ (comp.getUnrefinedName().getTypeTag() != ARRAY));

            if (componentBlueprint instanceof Blueprint.Primitive
                || componentBlueprint instanceof Blueprint.Record)
                componentSize_ = componentBlueprint.size_;
            else
                componentSize_ = MachineSizes.BYTES_IN_ADDRESS;

            vTable =
                ((S3Domain) type.getContext().getDomain()).getArrayVTable();
            assert vTable != null : "vTable is null.";

            lengthFieldOffset_ = ObjectModel.getObjectModel().headerSkipBytes(); 
            size_ = lengthFieldOffset_ + LENGTH_FIELD_IN_BYTES;
	    // preserve 2-word alignment if component is long/ulong/double
	    if (componentSize_ > MachineSizes.BYTES_IN_WORD)
	        size_ = (size_ + MachineSizes.BYTES_IN_WORD*2 -1) & ~ (MachineSizes.BYTES_IN_WORD*2 -1);
	    // size_ is moved -> UnpaddedFixedSize(), which is also used to skip the initial
	    // part of an array, *will* include the extra word
            arrayDepth_ = (short) type.getDepth();
	    S3Domain dom = getS3Domain();
	    j2cVTable = dom.getArrayJ2cVTable();
	    j2cIfTable = null;
	    set_dbg_string();
        }

	public final int lengthFieldOffset()  throws PragmaNoPollcheck {
	    return lengthFieldOffset_;
	}
	public final int lengthFieldSize() {
	    return LENGTH_FIELD_IN_BYTES;
	}

        public final int byteOffset(int index)  throws PragmaNoPollcheck {
            return size_ + (index * componentSize_);
        }

        public int getLength(Oop addr)  throws PragmaNoPollcheck {
            return VM_Address.fromObject(addr).add(lengthFieldOffset_).getInt();
        }

        public int getVariableSize(Oop oop)  throws PragmaNoPollcheck {
            return (int) computeSizeFor(getLength(oop));
        }

        public long computeSizeFor(int length) throws PragmaNoPollcheck {
	    long ret = size_ + ((long) length)*componentSize_;
	    // align to a 2 word value
	    return (ret + MachineSizes.BYTES_IN_WORD*2 -1) & ~ (MachineSizes.BYTES_IN_WORD*2 -1);
        }
        /** Retrieve the blueprint of the component object of this array.
         * @return the component blueprint
         **/
        public Blueprint getComponentBlueprint() throws PragmaNoPollcheck {
            return componentBlueprint_;
        }
        final public int getComponentSize() throws PragmaNoPollcheck {
            return componentSize_;
        }
        public VM_Address addressOfElement(Oop array, int index) throws PragmaNoPollcheck {
            return (VM_Address.fromObject(array)).add(byteOffset(index));
        }
        public Type getType() {
            return type_;
        }
        public String toString() {
            return "ArrBpt{" + getType() + "}";
        }
        public Oop stamp(VM_Address adr, int length) throws PragmaNoPollcheck {
            adr.add(lengthFieldOffset_).setInt(length); // bp & length loc8ns.
            return ObjectModel.getObjectModel().stamp(adr, this);
        }

        /** Visit the references of the given object.
         * @param object the object must have this as its blueprint.
         **/
        public void visitReferencesOf(Oop object, ReferenceVisitor visitor) {
            assert(object.getBlueprint() == this);
            visitor.process(object, 0); /* type ptr */
            Type elementType = type_.getInnermostComponentType();
            if ((elementType instanceof Type.Primitive)
                && (type_.getDepth() == 1))
                return;

            int length = getLength(object);
            assert length >= 0 : "Array length is negative!";

            for (int i = 0; i < length; i++)
                visitor.process(object, byteOffset(i));
        }

    } 
}
