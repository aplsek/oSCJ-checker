package s3.core.domain;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantPool;
import ovm.core.domain.ConstantResolvedInstanceFieldref;
import ovm.core.domain.ConstantResolvedInstanceMethodref;
import ovm.core.domain.ConstantResolvedInterfaceMethodref;
import ovm.core.domain.ConstantResolvedStaticFieldref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.ResolvedConstant;
import ovm.core.domain.Type;
import ovm.core.repository.Binder;
import ovm.core.repository.ConstantPool.UTF8Index;
import ovm.core.repository.ConstantClass;
import ovm.core.repository.ConstantFieldref;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.Constants;
import ovm.core.repository.ConstantsEditor;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.reader.ByteCodeConstants;
import ovm.util.ByteBuffer;
import ovm.util.OVMError;
import ovm.util.PragmaLiveClass;
import s3.core.S3Base;
import ovm.util.HTObject2int;
import ovm.core.Executive;
import s3.util.PragmaMayNotLink;

/**
 * @author Christian Grothoff, jv
 **/
public class S3Constants extends S3Base implements ConstantPool, JVMConstants {

    /**For every entry in the constant pool, we keep its tag.  Tags are
     * constants which indicate the type of constant pool entry that
     * follows. See JVM Spec 4.4.
     **/
    protected byte[] tags_;
    protected Object[] constants; /* prims are boxed */
    
    public Object[] constants() { return constants; }
    /** The owning class. */
    protected final Type.Compound type_;

    private int nextFree = 1;

    S3Constants(Type.Compound type, byte[] tags, Object[] values) {
	this.type_ = type;
	this.tags_ = tags;
	this.constants = values;
    }

    /**
     * Create a linkable constant pool from a repository constant
     * pool.  The underlying arrays of base are reused.
     **/
    public S3Constants(Type.Compound type,
		       ovm.core.repository.ConstantPool base) {
	this(type, base.getTags(), base.getValues());
    }

    /**
     * Copy a constant pool.
     * @param base the basic constant pool that should be extended with
     *        more constants
     **/
    public S3Constants(Type.Compound type, S3Constants base) {
	this(type,
	     (byte[]) base.tags_.clone(),
	     (Object[]) base.constants.clone());
    }

    protected int set(int index, int tag, Object val) {
	tags_[index] = (byte) tag;
	VM_Address.uncheckedAASTORE(constants, index, val);
	return index;
    }

    protected final int set(int index, ResolvedConstant val) {
	return set(index, val.getTag(), val);
    }

    public boolean isStaticMethodResolved(int index) {
	return tags_[index] == JVMConstants.CONSTANT_ResolvedStaticMethod;
    }

    public boolean isInstanceMethodResolved(int index) {
	return tags_[index] == JVMConstants.CONSTANT_ResolvedInstanceField;
    }

    public boolean isInterfaceMethodResolved(int index) {
	return tags_[index] == JVMConstants.CONSTANT_ResolvedInterfaceMethod;
    }

    public boolean isStaticFieldResolved(int index) {
	return tags_[index] == JVMConstants.CONSTANT_ResolvedStaticField;
    }

    public boolean isInstanceFieldResolved(int index) {
	return tags_[index] == JVMConstants.CONSTANT_ResolvedInstanceField;
    }

    public boolean isConstantResolved(int index) {
	return tags_[index] == JVMConstants.CONSTANT_Reference;
    }

    public boolean isClassResolved(int index) {
	return tags_[index] == JVMConstants.CONSTANT_ResolvedClass;
    }

    private Object enterMetaDataArea() {
	return MemoryPolicy.the().enterMetaDataArea(type_.getContext());
    }

    public ConstantResolvedStaticMethodref resolveStaticMethod(int index) throws LinkageException {
	S3Domain dom = (S3Domain) type_.getDomain();
	sanity(index, dom, JVMConstants.CONSTANT_ResolvedStaticMethod, JVMConstants.CONSTANT_Methodref);

	byte tag = tags_[index];
	// Already resolved ? //
	if (tag == JVMConstants.CONSTANT_ResolvedStaticMethod) return (ConstantResolvedStaticMethodref) constants[index];

	// Resolution //
	Object value = constants[index];
	if (! (value instanceof Selector.Method)) {
	    d("FATAL: S3Constants has a constant of wrong type"
	      + "in the slog tagged as CONSTANT_Methodref");
	    d(value.toString());
	    throw new OVMError("aborting");
	}
	Selector.Method sel = (Selector.Method)value;
	Type.Context ctx = type_.getContext();
	Blueprint bp = dom.blueprintFor(ctx.typeFor(sel.getDefiningClass()));
	Method method = S3MemberResolver.resolveStaticMethod(bp.getType().asCompound(), sel.getUnboundSelector(), null);
	if (method == null) 
	    throw new LinkageException("method " + sel.getUnboundSelector() + " not found in " + bp.getType()); 
	// bp may not be the declaringBP 
	bp = dom.blueprintFor(method.getDeclaringType());
	Object r = enterMetaDataArea();
	try {
	   int offset = S3MemberResolver.resolveNonVTableOffset(bp, sel.getUnboundSelector(), type_);
	    if (offset==-1)  throw new LinkageException(sel + " not found 2"); 
	    Oop shst = bp.getInstanceBlueprint().getSharedState();
	    if (shst == null) throw new Error("shst null");
	    ConstantResolvedStaticMethodref smi = ConstantResolvedStaticMethodref.make(method, offset, shst);
	    set(index, smi);
	    return smi;
	} finally { MemoryPolicy.the().leave(r); }
    }

    public ConstantResolvedInstanceMethodref resolveInstanceMethod(int index) throws LinkageException {
	S3Domain dom = (S3Domain) type_.getDomain();
	sanity(index, dom, JVMConstants.CONSTANT_ResolvedInstanceMethod, JVMConstants.CONSTANT_Methodref);
	byte tag = tags_[index];

	// Already resolved ? //
	if (tag == JVMConstants.CONSTANT_ResolvedInstanceMethod) {
	    return (ConstantResolvedInstanceMethodref)constants[index];
	}

	Selector.Method sel = (Selector.Method) constants[index];
	Blueprint bp;
	try {
	    Type.Context ctx = type_.getContext();
	    bp = dom.blueprintFor(ctx.typeFor(sel.getDefiningClass()));
	} catch (LinkageException le) { throw le;}

	ConstantResolvedInstanceMethodref smi = null;
	int offset =  ((S3Blueprint) bp).getNonVTableOffsets().get( sel);
	if (offset ==-1) {
	    offset = S3MemberResolver.resolveVTableOffset((S3Blueprint) bp, sel.getUnboundSelector(), type_);
	    if (offset == -1) throw new LinkageException(sel + " not found");
	    else { // resolved as a virtual call
		Method method = S3MemberResolver.resolveInstanceMethod(bp.getType().asCompound(), sel.getUnboundSelector(), type_);
		Object r = enterMetaDataArea();
		try {
		    smi = ConstantResolvedInstanceMethodref.make(method, offset, bp);
		} finally { MemoryPolicy.the().leave(r); }
	    }
	} else { // resolved as a special call
	    Method method = S3MemberResolver.resolveInstanceMethod(bp.getType().asCompound(), sel.getUnboundSelector(), type_);
	    Object r = enterMetaDataArea();
	    try {
		smi = ConstantResolvedInstanceMethodref.make(method, offset, bp);
	    } finally { MemoryPolicy.the().leave(r); }
	}
	set(index, smi);
	return smi;
    }

    private void sanity(int index, S3Domain dom, byte RESOLVED, byte TAG) {
	byte tag = tags_[index];
	if (tag == RESOLVED) 
	    if (constants[index] == null) throw new NullPointerException();
	    else return;
	//checkTagAt(TAG, index);
	Object value = constants[index];
	if (!(value instanceof Selector.Method)) throw new OVMError(
		"S3Constants has a constant of wrong type in the slot " + value.toString());
    }

    private ConstantResolvedInterfaceMethodref getResolvedInterfaceMethod(int index) throws LinkageException {
	S3Domain dom = (S3Domain) type_.getDomain();
	sanity(index, dom, JVMConstants.CONSTANT_ResolvedInterfaceMethod, JVMConstants.CONSTANT_InterfaceMethodref);
	byte tag = tags_[index];
	// Already resolved ? 
	if (tag == JVMConstants.CONSTANT_ResolvedInterfaceMethod) return (ConstantResolvedInterfaceMethodref) constants[index];
	else return null;
    }
    private ConstantResolvedInterfaceMethodref doResolveInterfaceMethod(int index) throws LinkageException, PragmaMayNotLink  {
	S3Domain dom = (S3Domain) type_.getDomain();
	Object value = constants[index];
	Selector.Method sel = (Selector.Method) value;
	Type.Context ctx = type_.getContext();
	Blueprint bp;
	try {
	    bp = dom.blueprintFor(ctx.typeFor(sel.getDefiningClass()));
	} catch (LinkageException le) { throw le; }

	Object r = enterMetaDataArea();
	try {
	    int offset = dom.getDispatchBuilder().getIFTableOffset(sel.getUnboundSelector());
	    if (offset == -1) { throw new LinkageException(); }
	    Method m = S3MemberResolver.resolveInterfaceMethod(bp.getType().asScalar(), sel.getUnboundSelector(), null);	  
	    if (m == null) { throw new LinkageException(); } // FIXME: the exception object leaks into the MetaDataArea --jv
	    ConstantResolvedInterfaceMethodref imi = ConstantResolvedInterfaceMethodref.make(m, offset, bp);		
	   
	    return imi;
	} finally { MemoryPolicy.the().leave(r); }
    }
    public ConstantResolvedInterfaceMethodref resolveInterfaceMethod(int index) throws LinkageException {
	ConstantResolvedInterfaceMethodref m = getResolvedInterfaceMethod(index);
	if (m!= null) return m;
	m = doResolveInterfaceMethod(index);
	set(index, m);
	return m;
    }
    public ConstantResolvedInterfaceMethodref resolveAndAddInterfaceMethod(int index) throws LinkageException {
	ConstantResolvedInterfaceMethodref m = getResolvedInterfaceMethod(index);
	if (m!= null) return m;
	m = doResolveInterfaceMethod(index);
	addInterfaceMethodref(m);
	//set(index, m);
	return m;
    }
    public ConstantResolvedStaticFieldref resolveStaticField(int index) throws LinkageException {

	S3Domain dom = (S3Domain) type_.getDomain();
	fieldsanity(index, dom, JVMConstants.CONSTANT_ResolvedStaticField, JVMConstants.CONSTANT_Fieldref);

	byte tag = tags_[index];
	// Already resolved ? //
	if (tag == JVMConstants.CONSTANT_ResolvedStaticField) return (ConstantResolvedStaticFieldref) constants[index];
	// Resolution //
	Object value = constants[index];
	Selector.Field sel = (Selector.Field) value;
	Type.Context ctx = type_.getContext();
	Blueprint bp = dom.blueprintFor(ctx.typeFor(sel.getDefiningClass()));
	S3Field field = (S3Field) S3MemberResolver
		.resolveStaticField((Type.Class) bp.getType(), sel.getUnboundSelector(), null);
	if (field == null) throw new LinkageException(sel + " not found"); 
	// bp may not be the declaringBP 
	bp = dom.blueprintFor(field.getDeclaringType());
	Oop shst = bp.getInstanceBlueprint().getSharedState();
	if (shst == null) throw new Error("shst null");
	Object r = enterMetaDataArea();
	try {
	    ConstantResolvedStaticFieldref sfi = ConstantResolvedStaticFieldref.make(field, field.getOffset(), shst);
	    set(index, sfi);
	    return sfi;
	} finally {
	    MemoryPolicy.the().leave(r);
	}
    }

    private void fieldsanity(int index, S3Domain dom, byte RESOLVED, byte TAG) {
	byte tag = tags_[index];
	// Already resolved ?
	if (tag == RESOLVED) {
	    if (constants[index] == null) throw new NullPointerException();
	    return;
	}
	checkTagAt(TAG, index);
	// Resolution 
	Object value = constants[index];
	if (!(value instanceof Selector.Field)) throw new OVMError(
		"S3Constants has a constant of wrong type in the slot " + value.toString());
    }

    public ConstantResolvedInstanceFieldref resolveInstanceField(int index) throws LinkageException {

	S3Domain dom = (S3Domain) type_.getDomain();
	fieldsanity(index, dom, JVMConstants.CONSTANT_ResolvedInstanceField, JVMConstants.CONSTANT_Fieldref);

	byte tag = tags_[index];
	// Already resolved ? //
	if (tag == JVMConstants.CONSTANT_ResolvedInstanceField) return (ConstantResolvedInstanceFieldref) constants[index];
	Object value = constants[index];
	Selector.Field sel = (Selector.Field) value;
	Type.Context ctx = type_.getContext();
	Blueprint bp; // shstBP
	try {
	    bp = dom.blueprintFor(ctx.typeFor(sel.getDefiningClass()));
	} catch (LinkageException le) {
	    throw le;
	}
	S3Field field = (S3Field) S3MemberResolver.resolveField(((Type.Class) bp.getType()), sel.getUnboundSelector(), null);
	if (field == null) { throw new LinkageException(); }
	Object r = enterMetaDataArea();
	try {
	    ConstantResolvedInstanceFieldref ifi = ConstantResolvedInstanceFieldref.make(field, field.getOffset(), bp);
	    set(index, ifi);
	    return ifi;
	} finally {
	    MemoryPolicy.the().leave(r);
	}
    }

    public Blueprint resolveClassAt(int index) throws LinkageException {
	byte tag = tags_[index];
	// Already resolved ? //
	if (tag == JVMConstants.CONSTANT_ResolvedClass) {
	    if (constants[index] == null) throw new NullPointerException();
	    return (S3Blueprint) constants[index];
	}
	// Tag check //
	checkTagAt(JVMConstants.CONSTANT_Class, index);
	// Resolution //
	S3Domain dom = (S3Domain) type_.getDomain();
	Object value = constants[index];
	if (!(value instanceof TypeName.Compound)) throw new OVMError("constant of wrong type, expected Class got "
		+ value.toString());

	TypeName.Compound tn = (TypeName.Compound) value;
	Type.Context ctx = type_.getContext();
	Blueprint bp;
	try {
	    bp = dom.blueprintFor(ctx.typeFor(tn));
	} catch (LinkageException le) {
	    throw le;
	}
	set(index, JVMConstants.CONSTANT_ResolvedClass, bp);
	return (S3Blueprint) bp;
    }

    /**
     * Resolve the object at the given index and return the corresponding
     * revolved instance.
     */
    public Oop resolveConstantAt(int index) {
	if (tags_[index] != JVMConstants.CONSTANT_Reference) {
	    S3Domain dom = (S3Domain) type_.getDomain();
	    switch (tags_[index]) {
	    case JVMConstants.CONSTANT_String:
		if (!(constants[index] instanceof RepositoryString)) {
		    d("FATAL: S3ConstantPool has entry of wrong type tagged as CONSTANT_String!");
		    d(constants[index].toString());
		    throw new OVMError("aborting");
		}
		RepositoryString s = (RepositoryString) constants[index];
		set(index, JVMConstants.CONSTANT_Reference, dom.internString(s.getUtf8Index()));
		if (constants[index] == null) throw new NullPointerException("Resolving RepositoryString " + s
			+ " resulted in null!");
		tags_[index] = JVMConstants.CONSTANT_Reference;
		break;
	    case JVMConstants.CONSTANT_SharedState:
		if (!(constants[index] instanceof TypeName.Gemeinsam)) {
		    d("FATAL: S3ConstantPool has entry of wrong type tagged as CONSTANT_SharedState!");
		    d(constants[index].toString());
		    throw new OVMError("aborting");
		}
		TypeName.Gemeinsam tn = (TypeName.Gemeinsam) constants[index];
		Type.Context tc = type_.getContext();
		Blueprint.Factory bf = tc.getDomain();
		TypeName.Compound bpn = tn.getInstanceTypeName().asCompound();
		try {
		    Blueprint ibp = bf.blueprintFor(tc.typeFor(bpn));
		    Object o = ibp.getSharedState();
		    if (o == null) throw new NullPointerException("Resolving shared state of " + constants[index]
			    + " resulted in null");
		    set(index, JVMConstants.CONSTANT_Reference, o);
		    // also: trigger static initialization (if needed)
		    if (!dom.isExecutive()) // don't do this at bootimage-build time; -- and at RT, we never hit this case for ED resolution :-)
		    dom.getCoreServicesAccess().initializeBlueprint(VM_Address.fromObject(o).asOop());
		} catch (LinkageException le) {
		    pln("CP PUNT SharedState " + tn);
		    throw new OVMError(le);
		}
		break;
	    case JVMConstants.CONSTANT_Binder:
	    default:
		fail("constant type " + getTagAt(index) + " not supported");
	    }
	}
	if (constants[index] == null) throw new NullPointerException();
	return VM_Address.fromObject(constants[index]).asOop();
    }

    public byte getTagAt(int offset) {
	return tags_[offset];
    }

    public int getConstantCount() {
	return constants.length;
    }

    public Object getConstantAt(int offset) {
	if ((offset >= 0) && (offset < constants.length) && ((offset > 0) || (tags_[offset] != 0))) return constants[offset];
	// offset 0 is ok if tags_[0] != 0 because then we
	// are the upper part of a diff-constant pool!
	throw failure("Illegal tag for access to getConstantAt: " + offset + " valid range is [1," + constants.length
		+ ")");
    }

    public void checkTagAt(byte expectedTag, int offset) throws Constants.AccessException {
	if (tags_[offset] != expectedTag) {
	    String str = "??";  
	    int o = tags_[offset]; 
	    if (o<ByteCodeConstants.CONSTANT_NAMES.length)
		str = ByteCodeConstants.CONSTANT_NAMES[o];	    
	    String message = "Invalid Constant Pool Entry " + str + " instead of " + ByteCodeConstants.CONSTANT_NAMES[expectedTag] + 
	    	", CP index " + offset + ", was " + getContents(offset);
	    throw new Constants.AccessException(message);
	}
    }

    /**
     * Return the typename at the given index.
     * See JVM Spec 4.4.1.
     * @param cpIndex the index of the TypeName to be retrieved
     * @return the TypeName at the given index
     * @throws Constants.AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public ConstantClass getClassAt(int cpIndex) throws Constants.AccessException {
	try {
	    return (ConstantClass) constants[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException("Invalid CP index " + cpIndex + " >= " + constants.length);
	}
    }

    public Type getType() {
	return type_;
    }

    public ConstantFieldref getFieldrefAt(int cpIndex) throws Constants.AccessException {
	try {
	    return (ConstantFieldref) constants[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException("Invalid CP index " + cpIndex + " >= " + constants.length);
	}
    }

    public ConstantMethodref getMethodrefAt(int cpIndex) throws Constants.AccessException {
	try {
	    return (ConstantMethodref) constants[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e + " have: "
		    + constants[cpIndex].getClass());
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException("Invalid CP index " + cpIndex + " >= " + constants.length);
	}
    }

    public int getValueAt(int cpIndex) throws Constants.AccessException {
	if ((cpIndex < 0) || (cpIndex >= tags_.length)) throw new Constants.AccessException("CP index " + cpIndex
		+ " out of bounds");
	byte t = tags_[cpIndex];
	switch (t) {
	case JVMConstants.CONSTANT_Long:
	case JVMConstants.CONSTANT_Double:
	    throw new Constants.AccessException("use getWideValueAt for that");
	case JVMConstants.CONSTANT_Integer:
	    return ((Integer) constants[cpIndex]).intValue();
	case JVMConstants.CONSTANT_Float:
	    ByteBuffer conv_ = ByteBuffer.allocate(4);
	    conv_.putFloat(0, ((Float) constants[cpIndex]).floatValue());
	    return conv_.getInt(0);
	default:
	    throw new Constants.AccessException("getValueAt only works for int or float!");
	}
    }

    public long getWideValueAt(int cpIndex) throws Constants.AccessException {
	if ((cpIndex < 0) || (cpIndex >= tags_.length)) throw new Constants.AccessException("CP index " + cpIndex
		+ " out of bounds");
	byte t = tags_[cpIndex];
	switch (t) {
	case JVMConstants.CONSTANT_Integer:
	case JVMConstants.CONSTANT_Float:
	    throw new Constants.AccessException("use getValueAt for that");
	case JVMConstants.CONSTANT_Long:
	    return ((Long) constants[cpIndex]).longValue();
	case JVMConstants.CONSTANT_Double:
	    ByteBuffer conv_ = ByteBuffer.allocate(8);
	    conv_.putDouble(0, ((Double) constants[cpIndex]).doubleValue());
	    return conv_.getLong(0);
	default:
	    throw new Constants.AccessException("getWideValueAt only works for long or double!");
	}
    }

    /**
     * Debug method. Get the contents of the constant pool at index
     * <code>cpIndex</code> as a <code>String</code>.
     */
    String getContents(int cpIndex) {
	String contents = "\"(" + tags_[cpIndex] + ")";
	switch (tags_[cpIndex]) {
	case JVMConstants.CONSTANT_Utf8:
	    contents += "Utf8: " + constants[cpIndex];
	    break;
	case JVMConstants.CONSTANT_NameAndType:
	    contents += "NameAndType";
	    break;
	case JVMConstants.CONSTANT_Integer:
	    contents += "INTEGER: " + constants[cpIndex];
	    break;
	case JVMConstants.CONSTANT_Float:
	    contents += "FLOAT:" + constants[cpIndex];
	    break;
	case JVMConstants.CONSTANT_Double:
	    contents += "DOUBLE" + constants[cpIndex];
	    cpIndex++;
	    break;
	case JVMConstants.CONSTANT_Long:
	    contents += "LONG" + constants[cpIndex];
	    cpIndex++;
	    break;
	case 0:
	    contents += "<EMPTY>";
	    break;
	default:
	    contents += "OTHER" + constants[cpIndex] + ", " + constants[cpIndex].getClass();
	}
	return contents + "\"";
    }

    /**
     * This constant pool is already mutable.
     **/
    public ConstantsEditor getBuilder() {
	return this;
    }
    
    /**
     * Throw away existing contents and realloc for size <code>n</code>.
     * @param n the new size of the constant pool in number of
     * constants.
     *
     * FIXME: Why is this public?  Hell, why is it even here?
     **/
    public void realloc(int n) {
	tags_ = new byte[n];
	constants = new Object[n];
    }
    
    /**
     * Clears a builder.
     **/
    public void reset() {
	realloc(1); // constant 0 is reserved!
    }

    /**
     * Build the corresponding constants object.
     * FIXME: Why is this here?
     */
    public Constants unrefinedBuild() {
	return this;
    }
    
    /* **************** build methods with auto-index allocation ********* */

    public int addResolvedConstant(Object o) {
	int n = testPresent(o, CONSTANT_Reference);
	if (-1 != n)
	    return n;
	return set(allocateIndex(), CONSTANT_Reference, o);
    }

    /**
     * Add a Binder constant.
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @param rb the bunder to be added
     **/
    public int addUnresolvedBinder(Binder rb) {
	int n = testPresent(rb, CONSTANT_Binder);
	if (-1 != n)
	    return n;
	return set(allocateIndex(), CONSTANT_Binder, rb);
    }

    /**
     * Add a double to the constant pool. (JVM Spec 4.4.5)
     * @param value the value to be added to the constant pool
     * @return the offset of the new entry	 
     **/
    public int addConstantDouble(double value) {
	Double d = new Double(value);
	int n = testPresent(d, CONSTANT_Double);
	if (-1 != n)
	    return n;
	n = set(allocateIndex(), CONSTANT_Double, d);
	allocateIndex();
	return n;
    }

    /**
     * Add a float to the constant pool. (JVM Spec 4.4.4)
     * @param value the value to be added to the constant pool
     * @return the offset of the new entry	 
     **/
    public int addConstantFloat(float value) {
	Float f = new Float(value);
	int n = testPresent(f, CONSTANT_Float);
	if (-1 != n)
	    return n;
	return set(allocateIndex(), CONSTANT_Float, f);
    }
    
    /**
     * Add an int to the constant pool. (JVM Spec 4.4.4)
     * @param value the value to be added to the constant pool     
     * @return the offset of the new entry	 
     **/
    public int addConstantInt(int value) {
	Integer val = new Integer(value);
	int n = testPresent(val, CONSTANT_Integer);
	if (-1 != n)
	    return n;
	return set(allocateIndex(), CONSTANT_Integer, val);
    }
    
    /**
     * Add a long to the constant pool. (JVM Spec 4.4.5)
     * @param value the value to be added to the constant pool
     * @return the offset of the new entry	 
     **/
    public int addConstantLong(long value) {
	Long l = new Long(value);
	int n = testPresent(l, CONSTANT_Long);
	if (-1 != n)
	    return n;
	n = set(allocateIndex(), CONSTANT_Long, l);
	allocateIndex(); // allocate 2nd index (wide!)
	return n;
    }

    /**
     * Add a bound field selector to the constant pool.
     * See JVM Spec 4.4.2
     * @param fieldsel the selector to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addFieldref(ConstantFieldref fieldsel) {
	int n = testPresent(fieldsel, fieldsel.getTag());
	if (-1 != n)
	    return n;
	return set(allocateIndex(), fieldsel.getTag(), fieldsel);
    }
    
    /**
     * Add an interface method selector to the constant pool.
     * See JVM Spec 4.4.2
     * @param methodsel the selector to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addInterfaceMethodref(ConstantMethodref methodsel) {
	// This wierd code is due to the fact that a Selector.Method
	// can't tell whether it is an interface method or not and
	// hence it always gives CONSTANT_Methodref. -HY
	byte tag;
	if (methodsel instanceof Selector.Method)
	    tag = CONSTANT_InterfaceMethodref;
	else if (methodsel 
		 instanceof ConstantResolvedInterfaceMethodref)
	    tag = CONSTANT_ResolvedInterfaceMethod;
	else
	    throw new Error("unexpected type : " + methodsel);
	int n = testPresent(methodsel, tag);
	if (-1 != n)
	    return n;
	return set(allocateIndex(), tag, methodsel);
    }

    /**
     * Add a method selector to the constant pool.	 
     * See JVM Spec 4.4.2
     * @param methodsel the selector to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addMethodref(ConstantMethodref methodsel) {
	int n = testPresent(methodsel, methodsel.getTag());
	if (-1 != n)
	    return n;
	return set(allocateIndex(), methodsel.getTag(), methodsel);
    }
    
    /**
     * Add a symbolic shared state constant.
     * This began an OVM speciality and does not correspond to
     * anything in the JVM Spec, but has since grown in interact with
     * LDCs on CONSTANT_Class.
     * @param tn The shared state type name
     * @return the offset of the new entry
     **/
    public int addUnresolvedSharedState(TypeName.Gemeinsam tn) {
        int n = testPresent(tn, CONSTANT_SharedState);
        if (-1 != n)
            return n;
	return set(allocateIndex(), CONSTANT_SharedState, tn);
    }

    /**
     * Add a string to the constant pool. (JVM Spec 4.4.3)
     * @param str the string to be added
     * @return the offset of the new entry	 
     **/
    public int addUnresolvedString(RepositoryString str) {
        int n = testPresent(str, CONSTANT_String);
        if (-1 != n)
            return n;
	return set(allocateIndex(), CONSTANT_String, str);
    }

    /**
     * Add a <code>TypeName</code> to the constant pool.
     * See JVM Spec 4.4.1.
     * @param name the TypeName object to be added
     * @return the offset of the new entry
     **/
    public int addClass(ConstantClass name) {
        if (name == null)
            throw new NullPointerException();
	// The same TypeName.Gemeinsam object can be stored in the
	// constant pool with different tags CONSTANT_Class or
	// CONSTANT_SharedState. -HY
        int n = testPresent(name, name.getTag());
        if (-1 != n)
            return n;
	return set(allocateIndex(), name.getTag(), name);
    }

    /**
     * Add an <code>UnboundSelector</code> to the constant pool.
     * See JVM Spec 4.4.6
     * @param usel the unbound selector to be added
     * @return the offset of the new entry
     *
     * FIXME: needed?
     **/
    public int addUnboundSelector(UnboundSelector usel) {
        int n = testPresent(usel, CONSTANT_NameAndType);
        if (-1 != n)
            return n;
	return set(allocateIndex(), CONSTANT_NameAndType, usel);
    }

    /**
     * Add a UTF8 string to the constant pool via its UTF8 index
     * See JVM Spec 4.4.7.
     * @param utf_index the index into the UTF8 table of the repository
     * @return the offset of the new entry
     *
     * FIXME: needed?
     **/
    public int addUtf8(int utf_index) {
        UTF8Index utf = new UTF8Index(utf_index);
        int n = testPresent(utf, CONSTANT_Utf8);
        if (-1 != n)
            return n;
	return set(allocateIndex(), CONSTANT_Utf8, utf);
    }

 
    /* ************** little convenience method ***************** */
    
    /**
     * Allocate a new index into the constant pool.  The pool may be
     * extremely sparse due to UTF8 elimination.
     *
     * @return the new index.
     **/
    private int allocateIndex() {
	int n = nextFree;
	while (n < tags_.length && tags_[n] != 0)
	    n++;
	nextFree = n + 1;
	reserveTagNumbers(n + 1);
	return n;
    }
   
    /**
     * Make sure there is room for n constants.
     * @param n the new number of constants supported (0...n-1)
     **/
    public void reserveTagNumbers(int n) {
	if (n <= tags_.length)
	    return; // got enough space
	byte[] oldTags = tags_;
	Object[] oldValues = constants;
	realloc(n);
	System.arraycopy(oldTags, 0, tags_, 0, oldTags.length);
	System.arraycopy(oldValues, 0, constants, 0, oldValues.length);
    }
     
    /**
     * Test if a constant equal to "o" with the expected tag is
     * already in the constant pool.  If yes, the respective index is
     * returned.
     * @param o the object whose presence is being tested 
     * @return -1 if o was not found
     */
    protected int testPresent(Object o, byte expectedTag) {
	switch (expectedTag) {
	case CONSTANT_Methodref:
	    for (int i = 1; i < constants.length; i++)
		if (o == constants[i] ||
		    ((tags_[i] == CONSTANT_ResolvedStaticMethod ||
		      tags_[i] == CONSTANT_ResolvedInstanceMethod) &&
		     ((ConstantMethodref) constants[i]).asSelector() == o))
		    return i;
	    return -1;
	case CONSTANT_Fieldref:
	    for (int i = 1; i < constants.length; i++)
		if (o == constants[i] ||
		    ((tags_[i] == CONSTANT_ResolvedStaticField ||
		      tags_[i] == CONSTANT_ResolvedInstanceField) &&
		     ((ConstantFieldref) constants[i]).asSelector() == o))
		    return i;
	    return -1;
	case CONSTANT_Class:
	    for (int i = 1; i < constants.length; i++)
		if (o == constants[i] ||
		    (tags_[i] == CONSTANT_ResolvedClass &&
		     ((S3Blueprint) constants[i]).getName() == o))
		    
		    return i;
	    return -1;
	default:
	    for (int i = 1; i < constants.length; i++)
		if (expectedTag == tags_[i]
		    && equal(asOop(o), asOop(constants[i]), expectedTag))
		    return i;
	    return -1;
	}
    }

    /**
     * Return true if two constant pool entries with the same tag are
     * equal.  Only call equals() when such a call is both safe and
     * necessary.
     **/
    private boolean equal(Object o1, Object o2, int tag) {
	switch (tag) {
	 // The following constants may be user-domain objects, and
	 // cannot be compared with the executive domain equals method
	case CONSTANT_Reference:
	case CONSTANT_SharedState:
	 // The following constants are interned symbols, and
	 // comparisons using == are equalivalent to equals.
	case CONSTANT_Class:
	case CONSTANT_Fieldref:
	case CONSTANT_Methodref:
	case CONSTANT_InterfaceMethodref:
	case CONSTANT_NameAndType:
	case CONSTANT_ResolvedClass:
	case CONSTANT_ResolvedStaticMethod:
	case CONSTANT_ResolvedInstanceMethod:
	case CONSTANT_ResolvedInterfaceMethod:
	case CONSTANT_ResolvedStaticField:
	case CONSTANT_ResolvedInstanceField:
	    return o1 == o2;

	// For the rest, equals() may be needed
	case CONSTANT_Utf8:
	case CONSTANT_Integer:
	case CONSTANT_Float:
	case CONSTANT_Long:
	case CONSTANT_Double:
	case CONSTANT_String:
	// Not sure what Binder is used for...
	case CONSTANT_Binder:
	    return o1.equals(o2);
	default:
	    throw Executive.panic("unknown tag " + tag);
	}
    }

    /**
     * An implementation that maintains hashtables of existing
     * constant pool entries for speedy add operations.  Currently
     * unused, but might come in handy if we wish to share constant
     * pools between classes (maybe at a package or Context
     * granularity).
     **/
    static class Caching extends S3Constants {
	HTObject2int entries = new HTObject2int();
	HTObject2int shStEntries = new HTObject2int();

	public Caching(Type.Compound t) {
	    super(t, (S3Constants) ((Type.Scalar) t).getConstantPool());
	}

	protected int set(int index, int tag, Object value) {
	    if (constants[index] != null) {
		HTObject2int cache = (tags_[index] == CONSTANT_SharedState
				      ? shStEntries
				      : entries);
		cache.remove(constants[index]);
	    }
	    super.set(index, tag, value);
	    HTObject2int cache = (tags_[index] == CONSTANT_SharedState
				  ? shStEntries
				  : entries);
	    cache.put(value, tag);
            return index;
	}

	protected int testPresent(Object o, byte expectedTag) {
	    HTObject2int cache = (expectedTag == CONSTANT_SharedState
				  ? shStEntries
				  : entries);
	    int val = cache.get(o);
	    assert(val == -1
			      || (tags_[val] == expectedTag
				  && o.equals(constants[val])));
	    return val;
	}
    }
} // end of S3Constants
