package s3.services.j2c;
import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.SpecificationIR;
import ovm.services.bytecode.SpecificationIR.*;
import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.AbstractValueError;
import ovm.util.HashMap;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Method;
import s3.services.bootimage.Ephemeral;
import s3.util.PragmaAssertNoSafePoints;
import ovm.core.domain.Method;

public abstract class J2cValue extends SpecificationIR.Value
    implements Ephemeral.Void, Cloneable
{
    public static final J2cValue[] EMPTY_ARRAY = new J2cValue[0];
    public static final String[] typeCodeToCtype = new String[128];
    static {
	typeCodeToCtype[TypeCodes.BOOLEAN] = "jboolean";
	typeCodeToCtype[TypeCodes.BYTE] = "jbyte";
	typeCodeToCtype[TypeCodes.UBYTE] = "unsigned char";
	typeCodeToCtype[TypeCodes.SHORT] = "jshort";
	typeCodeToCtype[TypeCodes.USHORT] = "jchar";
	typeCodeToCtype[TypeCodes.CHAR] = "jchar";
	typeCodeToCtype[TypeCodes.INT] = "jint";
	typeCodeToCtype[TypeCodes.UINT] = "unsignedjint";
	typeCodeToCtype[TypeCodes.ARRAY] = "jref";
	typeCodeToCtype[TypeCodes.OBJECT] = "jref";
	typeCodeToCtype[TypeCodes.FLOAT] = "jfloat";
	typeCodeToCtype[TypeCodes.LONG] = "jlong";
	typeCodeToCtype[TypeCodes.ULONG] = "unsignedjlong";
	typeCodeToCtype[TypeCodes.DOUBLE] = "jdouble";
    }

    /*
     * Extend the Specification IR slightly:
     */
    public static class PhiSource implements ValueSource {
	J2cValue[] val;		// record all defs that flow into this
				// value.  Too much?

	boolean contains(J2cValue v) {
	    for (int i = 0; i < val.length; i++)
		if (val[i] == v) return true;
	    return false;
	}

	PhiSource(J2cValue[] v) { val = v; }
    }

    ValueSource phi(AbstractValue _other) {
	// At one point, there was a large peice of code intended to
	// track merged ValueSources.  This code was never used, and
	// actually caused problems if the resulting PhiSource
	// objects where ever seen.  Hence, as of r1.34, it is gone.
	return null;
    }

    public static class PCRefExp implements ValueSource {
	public int target;

	public PCRefExp(int target) {
	    this.target = target;
	}
    }

    public static class JValueExp implements ValueSource {
	public Value source;
	public JValueExp(Value source) { this.source = source; }
    }

    public static class SeqExp implements ValueSource {
	public Value[] v;
	public SeqExp(Value[] v) { this.v = v; }
	public SeqExp(Value v1, Value v2) {
	    this.v = new Value[] { v1, v2 };
	}
    }
	
    public static class J2cLocalExp extends JValueExp {
	public int num;
	public J2cLocalExp(int num) {
	    super(new InternalReference("_local" + num));
	    this.num = num;
	}
    }

    /**
     * An intermediate expression for calls to initializeBlueprint.
     * SpecInstantiation is going to spit out a lot of these, and they
     * will be expanded to:
     * <pre><code>
     * if (needsInit(<i>bp.getCID()</i>,  <i>bp.getUID()</i>) {
     *   <i>csaCall</i>;
     *   didInit(<i>bp.getCID()</i>, <i>bp.getUID()</i>);
     * }
     * </code></pre>
     * inside BBspec.  We want to avoid exanding this if too early, so
     * that we can easily optmize away duplicate checks.
     * <p>
     * We also want to expand this in BBSpec translation so that the
     * cross-domain call becomes explict.
     **/
    public static class ClinitExp implements ValueSource {
	public Blueprint bp;
	public ValueSource csaCall;

	public ClinitExp(Blueprint bp, ValueSource csaCall) {
	    this.bp = bp;
	    this.csaCall = csaCall;
	}
    }

    public static class InvocationExp implements ValueSource {
	public Value target;
	public Value[] args;
	public S3Blueprint rt;
	public S3Blueprint[] at;

	// Not final by any means.  If an invocation need not be
	// treated as a safe point, the easiest thing is to clear this
	// flag.
	public boolean isSafePoint;

	// Is it possible for the garbage collector to run while this
	// call is in progress?
	public final boolean gcInScope;
	
	// FIXME: m hasn't been set in ages.  At this point it
	// should be easy to define constructors that take
	// MethodReference target, and let overloading on subtypes
	// take care of the rest.  How useful can `m' be if I haven't
	// needed it in ages?
	// public S3Method m;

	private InvocationExp(Value target) {
	    this.target = target;
	    
	    if (target instanceof MethodReference) {
	    	S3Method m = ((MethodReference)target).method;
	    	S3Blueprint targetBP = (S3Blueprint) m.getDeclaringType().
	    		getDomain().blueprintFor(m.getDeclaringType());
	    	
	    	gcInScope = ( PragmaAssertNoSafePoints.descendantDeclaredBy(
			m.getSelector(), targetBP ) == null );
	    } else {
	    	gcInScope = ( target == j2cInvokeTarget || target == runTarget );
	    }
	    
	    isSafePoint = gcInScope;
	}
	
	public InvocationExp(Value target, Value[] args, S3Blueprint rt) {
	    this(target);
	    this.args = args;
	    this.rt = rt;
	}
	public InvocationExp(Value target, Value[] args, S3Blueprint rt,
			     S3Blueprint[] at) {
	    this(target);
	    this.args = args;
	    this.rt = rt;
	    this.at = at;
	}
	public InvocationExp(Value target, Value arg1, S3Blueprint rt) {
	    this(target, new Value[] { arg1 }, rt);
	}
	public InvocationExp(String target, Value arg1, S3Blueprint rt) {
	    this(makeSymbolicReference(target),
		 new Value[] { arg1 }, rt);
	}
	public InvocationExp(String target, Value arg1, Value arg2, S3Blueprint rt) {
	    this(makeSymbolicReference(target),
		 new Value[] { arg1, arg2 }, rt);
	}
	public InvocationExp(String target, AbstractValue[] args, int length,
			     boolean isReversed, S3Blueprint rt) {
	    this(makeSymbolicReference(target));
	    this.args = new Value[length];
	    if (isReversed)
		for (int i = 0; i < length; i++)
		    this.args[i] = (Value) args[length - (i + 1)];
	    else
		for (int i = 0; i < length; i++)
		    this.args[i] = (Value) args[i];
	    this.rt = rt;
	}
    }

    public static class J2cFieldAccessExp extends FieldAccessExp {
	public S3Blueprint declaringBP;
	public J2cFieldAccessExp(Value object,
				 S3Blueprint declaringBP,
				 Selector.Field fsel)
	{
	    super(fsel, object);
	    this.declaringBP = declaringBP;
	}
    }

    public static class ValueAccessExp implements ValueSource {
        public J2cValue v;
        public ValueAccessExp(J2cValue v) { this.v = v; }
    }
    
    public static class J2cLookupExp extends LookupExp {
	public J2cLookupExp(Value bp, String tableName, Value index) {
	    super(bp, tableName, index);
	}
    }

    /**
     * The interpreter doesn't generate any IR for
     * MULTINEWARRAY_QUICK.  The compiler is much happier treating all
     * CSA calls uniformly
     * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
     */
    public static class DimensionArrayExp implements ValueSource {
	public J2cValue[] dims;
	public DimensionArrayExp(AbstractValue[] dims, int len) {
	    this.dims = new J2cValue[len];
	    System.arraycopy(dims, 0, this.dims, 0, len);
	}
    }

    public static class CCastExp implements ValueSource {
	public String type;
	public Value exp;

	public CCastExp(String type, Value exp) {
	    this.type = type;
	    this.exp = exp;
	}
    }

    // TypeCode for addresses converted to ints
    static final char SIZE_T_CODE = TypeCodes.UINT;

    // Allocation info. 
    int kind = NOVAR;

    // A unique number for all values generated on the stack, 
    // "phi" variables, JVM-level local variables, and "spill"
    // variables.  Essentially, this is a number for every C++-level
    // local variable.  
    int number = -1;

    int pc = -1;
    
    int flavor = -1;
    int index = -1;
    String name;

    /*
     * A single-linked list of all J2cValues that refer to the same
     * variable.  This is a complete list if the head is the canonical
     * value of that name.
     */
    J2cValue renamed;

    // Variable kinds
    static final int LOCAL = 0;
    static final int STACK_SLOT = 1;
    static final int MERGED_STACK_SLOT = 2;
    static final int LOCAL_VAR = 3; // not local JValue, but typed.
    static final int NOVAR = -1;

    static final String[] names
	= new String[] { "_local", "stack_", "phi_" };
    
    // return a copy of this value with the appropriate source
    public abstract J2cValue copy(ValueSource src);

    // This method should almost certainly be removed  It is called
    // when the abstract interpreter sets a local variable, and the
    // third argument is always LOCAL rather than LOCAL_VAR.
    //
    // I don't think that the naming scheme this method implements was
    // ever actually used to generate C++ code.
    public J2cValue allocate(int pcval, int idx, int knd) {
	if (false)
	    throw new Error("allocate(III) NOT DEAD");
//	try {
	    J2cValue ret = this; // (J2cValue) clone();
// 	    if (ret.kind != NOVAR)
// 		throw new Error("reallocating "
// 				+ ret.getType() + " " + ret.getName()
// 				+ " as " + kind + ":" + pc + ":" + index);
	    //ret.UID = UID;
	    ret.pc = pcval;
	    ret.index = idx;
	    ret.kind = knd;
	    if (knd == LOCAL)
		name = "_local" + idx;
	    else
		name = names[knd] + pcval + "_" + idx;
	    return ret;
//	} catch (CloneNotSupportedException e) {
// 	    throw new Error("impossible");
// 	}
    }

    public J2cValue allocate(String n, int k, int f, int i) {
	this.name = n;
	this.kind = k;
        this.flavor = f;
        this.index = i;
	return this;
    }

    public String getName() {
	return name;
    }

    public String getType() {
	if (kind == LOCAL)
	    return "jvalue";
	else
	    return getTypeInternal();
    }

    public abstract Type getJavaType(S3Domain d);
    public S3Blueprint getBlueprint(S3Domain d) {
	Type t = getJavaType(d);
	if (t != null) {
	    S3Domain realDom = (S3Domain) t.getContext().getDomain();
	    return (S3Blueprint) realDom.blueprintFor(t);
	} else
	    return null;
    }

    protected String getTypeInternal() { return null; }

    public static final Factory bogusFactory = new Factory() {
	    public Int    makePrimitiveBool()
	    { return null; }
	    public Int    makePrimitiveByte()
	    { return null; }
	    public Int    makePrimitiveShort()
	    { return null; }
	    public Int    makePrimitiveChar()
	    { return null; }
	    public Int    makePrimitiveInt()
	    { return null; }
	    public Float  makePrimitiveFloat()
	    { return null; }
	    public Long   makePrimitiveLong()
	    { return null; }
	    public Double makePrimitiveDouble()
	    { return null; }
	    public Reference makeReference(TypeName.Compound _)
	    { return null; }
	    public Array  makeArray(AbstractValue _)
	    { return null; }
	    public Reference makeUninitialized(TypeName.Compound _)
	    { return null; }
	    public AbstractValue makeNull()
	    { return null; }
	    public Invalid makeInvalid()
	    { return null; }
	    public JumpTarget makeJumpTarget(int pc)
	    { return new J2cJumpTarget(pc); }
	    public AbstractValue typeName2AbstractValue(TypeName _)
	    { return null; }
	};

    J2cValue(ValueSource vs) {
	super(vs);
    }
    /**
     * the IR stuffs expressions executed for effect in plain-old
     * Value objects.  Since we are assigning types to values, we
     * explicitly wrap side-effect expressions in void values.
     *
     * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
     */
    public static class J2cVoid extends J2cValue {
	J2cVoid(ValueSource vs) { super(vs); }

	public J2cValue copy(ValueSource v) { return new J2cVoid(v); }

	protected String getTypeInternal() { return "void"; }
	public Type getJavaType(S3Domain d) { return d.VOID; }
    }

    static private int jumpTargetCnt;
    
    public static class J2cJumpTarget extends J2cValue implements JumpTarget {
	int pc = -1;
	
	J2cJumpTarget(int pc) {
	    this(new PCRefExp(pc), pc);
	    if (KEEP_CACHE_STATS) jumpTargetCnt++;
	}

	J2cJumpTarget(ValueSource vs, int pc) {
	    super(vs);
	    this.pc = pc;
	}

	public boolean isJumpTarget() { return true; }
	public JumpTarget getJumpTarget() { return this; }
	public int getTarget() { return pc; }

	public boolean includes(AbstractValue other) {
	    return (other == this
		    || (other instanceof J2cJumpTarget
			&& ((J2cJumpTarget) other).pc == pc));
	}
	public AbstractValue merge(AbstractValue other) {
	    return includes(other) ? this : null;
	}
	protected String getTypeInternal() { return "int"; }

	public J2cValue copy(ValueSource v)
	{ return new J2cJumpTarget(v, pc); }

	public Type getJavaType(S3Domain d)
	{ return null; }
    }
   
    // Invalid really isn't a value at all
    public static class J2cInvalid extends J2cValue
	implements Invalid, Ephemeral.Void
    {
	public boolean isInvalid() { return true; }
	public Invalid getInvaid() { return this; }
	public AbstractValue merge(AbstractValue _) { return this; }
	J2cInvalid() { super(null) ; }
	public String toString() { return "<invalid>"; }
	public boolean includes(AbstractValue other) {
	    return this == other;
	}
	public J2cValue copy(ValueSource vs) { return this; }
	public J2cValue allocate(int p, int i, int k) {
	    return this;
	}

	public Type getJavaType(S3Domain _) { throw new Error("undefined"); }
    }

    // Null is a value, but unlike reference values, but it has an
    // oddball type
    public static class J2cNull extends J2cValue implements Null, Ephemeral.Void
    {
	public boolean isReference() { return true; }
	public Reference getReference() { return this; }
	public boolean isArray() { return false; }
	public Array getArray() { throw new AbstractValueError(); }
	public boolean isInitialized() { return true; }
	public void initialize() { }
	public boolean isNull() { return true; }
	public Null getNull() { return this; }
	public TypeName.Compound getCompoundTypeName() { return null; }
	public J2cNull() { super(null); }

	// needed for REF_GETFIELD_QUICK(null)
	public J2cNull(ValueSource vs) { super(vs); }

	public boolean isConcrete() { return true; }
	public Object concreteValue() { return null; }

	public J2cValue copy(ValueSource vs) { return this; }
	public J2cValue allocate(int p, int i, int k) {
	    return this;
	}
	public J2cValue allocate(String n, int k, int f, int i) {
	    assert(this != NULL);
	    return super.allocate(n, k, f, i);
	}

	public Type getJavaType(S3Domain d) { return d.ROOT_TYPE; }
    }

    // A reference to an implementation object, such as a c++ non-member
    // function
    public static class InternalReference extends J2cValue {
	InternalReference(ValueSource vs) { super(vs); }
	InternalReference(String name) {
	    super(new SymbolicConstant(name));
	}
	public String toString() {
	    if (source instanceof SymbolicConstant)
		return ("<InternalReference: " + getName() + ">");
	    else
		return super.toString();
	}
	
	public J2cValue copy(ValueSource vs)
	{ return new InternalReference(vs); }

	public Type getJavaType(S3Domain _) { throw new Error("undefined"); }

	public String getName() { return ((SymbolicConstant) source).name; }
    }

    public static class MethodReference extends InternalReference {
	public S3Method method;
	MethodReference(ValueSource vs, S3Method method) {
	    super(vs);
	    this.method = method;
	}
	MethodReference(String name, S3Method method) {
	    super(name);
	    this.method = method;
	}
	public J2cValue copy(ValueSource vs) {
	    return new MethodReference(vs, method);
	}
    }

    static final J2cInvalid INVALID = new J2cInvalid();
    static final J2cInvalid TERMINATOR = new J2cInvalid();
    static final J2cNull NULL = new J2cNull();

    public boolean includes(AbstractValue other) { return this == other; }
    public AbstractValue merge(AbstractValue other) {
	if (this == other)
	    return this;
	else if (other instanceof J2cReference)
	    return other.merge(this);
	else
	    return INVALID;
    }
    boolean isConcrete() { return false; }

    public String toString() {
	String qname = getClass().getName();
	String nm = qname.substring(qname.lastIndexOf('$') + 1,
				      qname.length());
	if (name != null)
	    return "<" + nm + " " + name + ">";
	else
	    return "<" + nm + ": " + source + ">";
    }

    public static class J2cReference extends J2cValue implements Reference
    {
	// TypeInfo carries a variety of information, such as whether
	// a value can be null, and which concrete types it may take
	// on.  Exactly what is contained in a TypeInfo node, and what
	// we can know about fields and method return values depends
	// on the nature of any interprocedural analysis we carry out.
	TypeInfo t;

	public boolean isReference() { return true; }

	public final TypeInfo getTypeInfo() {
	    return t;
	}
	
	final S3Blueprint getBlueprint() {
	    return t.getBlueprint();
	}

	public final S3Blueprint getBlueprint(S3Domain _) {
	    return t.getBlueprint();
	}
	public final boolean exactTypeKnown() {
	    return t.exactTypeKnown();
	}

	public Type getJavaType(S3Domain _)
	{ return getBlueprint().getType(); }

	J2cReference(ValueSource vs, TypeInfo t) {
	    super(vs);
	    this.t = t;

	    if (isArray())
		assert(getJavaType(null).isArray());
	    // can't really do this for locals!
// 	    else
// 		assert(!getJavaType(null).isArray());
	}

	J2cReference(ValueSource vs, S3Blueprint staticType,
		     boolean isExact, boolean canBeNull) {
	   this(vs, TypeInfo.make(staticType, isExact, canBeNull));
	}

	J2cReference(ValueSource vs, S3Blueprint bp) {
	    this(vs, bp, false, true);
	}

	public TypeName.Compound getCompoundTypeName() {
	    return getBlueprint().getType().asCompound().getName();
	}

	public AbstractValue merge(AbstractValue _other)
	{
	    if (_other == this)	// needed?
		return this;
	    else if (_other instanceof J2cReference) {
		J2cReference other = (J2cReference) _other;
		return new J2cReference(phi(other), t.merge(other.t));
	    } else if (_other == NULL)
		return new J2cReference(phi(_other),
					t.mergeWithNull());
	    else if (_other instanceof J2cInt) {
		return _other.merge(new J2cInt(new ConversionExp(this),
					       SIZE_T_CODE));
	    }
	    else
		return super.merge(_other);
	}

	public void destructiveMerge(J2cValue other) {
            if (other instanceof J2cNull)
                /* nothing to do here */;
            else
	       t = t.merge(((J2cReference) other).t);
	}

	public boolean includes(AbstractValue v) {
	    if (v == NULL)
		return t.includesNull();
	    else if (v instanceof J2cReference)
		return t.includes(((J2cReference) v).t);
	    else
		return false;
	}

	public boolean isArray() { return this instanceof Array; }
	public Array getArray() { return (Array) this; }
	public boolean isInitialized() { return true; } // who cares?
	public void initialize() { } // who cares?
	public boolean isNull() { return false; }
	public Null getNull() { throw new Error("not a null"); }
	public String toString() {
	    return ("<ref " + getBlueprint()
		    + (name == null
		       ? ">"
		       : (" " + name + ">")));
	}

	protected String getTypeInternal() {
	    return J2cFormat.format(getBlueprint()) + " *";
	}

	public J2cValue copy(ValueSource vs) {
	    return new J2cReference(vs, t);
	}
    }

    static final J2cInt UNKNOWN_DIM = new J2cInt(null, TypeCodes.INT,
						 0, Integer.MAX_VALUE);
    
    public static class J2cArray extends J2cReference
	implements Array
    {
	J2cInt[] dims;
	public J2cInt[] getDimmensions() { return dims; }

	public boolean isArray() { return true; }

	public AbstractValue getComponentType() {
	    return null;
	}

	public String toString() {
	
	    StringBuffer dimsStr = new StringBuffer(10);
	    for (int i=0; i<dims.length; i++) {
	    	dimsStr.append("[");
	    	dimsStr.append(dims[i]);
	    	dimsStr.append("]");
	    }
	    return ("<" + J2cArray.this.getBlueprint() + dimsStr.toString() + ">");
//		    "[" + dims[0] + "]>");
	}

	// method return constructor
	J2cArray(ValueSource vs, S3Blueprint staticType) {
	    this(vs, TypeInfo.make(staticType, false, true), UNKNOWN_DIM);
	}

	// translating (nullchecking) read barrier
	// needs isExact==false, canBeNull==false
	J2cArray(ValueSource vs, S3Blueprint staticType, boolean isExact, boolean canBeNull) {
	    this(vs, TypeInfo.make(staticType, isExact, canBeNull), UNKNOWN_DIM);
	}

	// newarray constructor
	J2cArray(ValueSource vs, S3Blueprint staticType, J2cInt outer) {
	    this(vs, TypeInfo.make(staticType, true, false), outer);
	}

	// merge constructor
	J2cArray(ValueSource vs, TypeInfo t, J2cInt outerDim) {
	    super(vs, t);
	    Blueprint.Array bp = (Blueprint.Array) J2cArray.this.getBlueprint();
	    int ndims = 1;
	    for (Blueprint elm = bp.getComponentBlueprint();
		 elm instanceof Blueprint.Array;
		 elm = ((Blueprint.Array) elm).getComponentBlueprint())
		ndims++;
	    dims = new J2cInt[ndims];
	    dims[0] = outerDim;
	    for (int i = 1; i < ndims; i++)
		dims[i] = UNKNOWN_DIM;
	}
	
	public AbstractValue merge(AbstractValue _other) {
	    if (this == _other)
		return this;
	    else if (_other instanceof J2cArray) {
		J2cArray other = (J2cArray) _other;
		TypeInfo nt = t.merge(other.t);
		if (nt.getBlueprint().isArray())
		    // This is true unless we are merging a prim array
		    // with an incompatible array
		    return new J2cArray(phi(other), nt,
					(J2cInt) dims[0].merge(other.dims[0]));
		else
		    return new J2cReference(phi(other), nt);
	    } else if (_other == NULL)
		return new J2cArray(phi(_other),
				    t.mergeWithNull(), dims[0]);
	    else
		return super.merge(_other);
	}

	public boolean includes(AbstractValue v) {
	    return (super.includes(v)
		    && (v instanceof J2cArray
			? dims[0].includes(((J2cArray) v).dims[0])
			: true /* is true really what I want?  Is the */));
                               /* test needed? */
	}

	public J2cValue copy(ValueSource vs) {
	    return new J2cArray(vs, t, dims[0]);
	}
    }
    
    public static class ConcreteScalar extends J2cReference {
	Oop value;

	ConcreteScalar(ValueSource vs, Oop value) {
	    this(vs, value, (S3Blueprint) value.getBlueprint());
	}
	ConcreteScalar(ValueSource vs, Oop value, S3Blueprint realType) {
	    super(vs, realType, true, false);
	    this.value = value;
	}

	public boolean isConcrete() { return true; }
	// XXX: may not be an Object
	public Object concreteValue() {
	    return VM_Address.fromObject(value).asObject();
	}
	
	public AbstractValue merge(AbstractValue other) {
	    if (this == other)
		return this;
	    if (other instanceof ConcreteScalar
		// XXX: Is this right.  value is more likely a
		// VM_Address that must be compared with equals
		&& ((ConcreteScalar) other).value == value)
		return new ConcreteScalar(phi(other), value,
					  ConcreteScalar.this.getBlueprint());
	    else
		return super.merge(other);
	}

	public boolean includes(AbstractValue other) {
	    return (other instanceof ConcreteScalar
		    && value == ((ConcreteScalar) other).value);
	}

	public String toString() {
	    return "<" + ConcreteScalar.this.getBlueprint() + " reference = " + value + ">";
	}

	public J2cValue copy(ValueSource vs) {
	    return new ConcreteScalar(vs, value,
				      ConcreteScalar.this.getBlueprint());
	}
    }

    // efficacy of caching shared state and linkset entries
    private static int shStCnt;
    private static int bpLitCnt;
    
    /**
     * We need to track shared-state objects on the stack in order to
     * recreate getstatic/putstatic/invokestatic and figure out which
     * field is referenced.  ldc of a shared state will be converted
     * to a nop during code gen.
     *
     * It might be interesting to see whether it is faster to pass
     * NULL in to static methods or this-shared-state.
     */
    public static class SharedStateReference extends ConcreteScalar {
	S3Blueprint.Scalar shStBp;
	SharedStateReference(ValueSource vs, Oop value) {
	    super(vs, value);
	    if (KEEP_CACHE_STATS) shStCnt++;
	    shStBp = (S3Blueprint.Scalar) value.getBlueprint();
	    assert(shStBp.isSharedState());
	}
	public J2cValue copy(ValueSource vs) {
	    return new SharedStateReference(vs, value);
	}
    }

    public static class BlueprintReference extends ConcreteScalar {
	S3Blueprint blueprintValue() {
	    return (S3Blueprint) VM_Address.fromObject(value).asAnyObject();
	}
	BlueprintReference(S3Blueprint value) {
	    this(null, value);
	    if (KEEP_CACHE_STATS) bpLitCnt++;
	}
	BlueprintReference(ValueSource source, S3Blueprint value) {
	    super(source, VM_Address.fromObject(value).asOop());
	}
	public boolean isConcrete() { return source == null; }

	public J2cValue copy(ValueSource vs) {
	    return new BlueprintReference(vs, blueprintValue());
	}
    }

    static abstract class ArraySizer {
	/*
	 * Return an array with the same structure as value, except
	 * that each leaf [] is replaced by an int[1] containing its
	 * length.
	 *
	 * Unless we know that outer arrays are immutable, there is no
	 * sense in computing this.
	 */
	//abstract Object getLengths(Oop value);
	abstract int getOuterLength(Oop value);
    }

    static class ReflectiveSizer extends ArraySizer implements Ephemeral.Void {
	int getOuterLength(Oop value) {
	    return java.lang.reflect.Array.getLength(((VM_Address) value).asAnyObject());
	}
    }

    static class OvmSizer extends ArraySizer {
	int getOuterLength(Oop value) {
	    Blueprint.Array bp = (Blueprint.Array) value.getBlueprint();
	    return bp.getLength(value);
	}
    }

    static private ArraySizer sizer = new ReflectiveSizer();
    
    public static class ConcreteArray extends J2cArray {
	J2cInt[] dimensions;	// Since arrays are mutable, we only
				// know the outermost dimension
	Oop value;

	ConcreteArray(ValueSource vs, S3Blueprint.Array bp,
		      Oop value) {
	    super(vs, bp, new J2cInt(null, TypeCodes.INT,
				     sizer.getOuterLength(value)));
	    this.value = value;
	}

	public AbstractValue merge(AbstractValue other) {
	    if (other == this)
		return this;
	    else if (other instanceof ConcreteArray
		     && value == ((ConcreteArray) other).value)
		return new ConcreteArray(phi(other),
					 (S3Blueprint.Array) ConcreteArray.this.getBlueprint(),
					 value);
	    else
		return super.merge(other);
	}

	public boolean includes(AbstractValue other) {
	    return (other instanceof ConcreteArray
		    && value == ((ConcreteArray) other).value);
	}

	public boolean isConcrete() { return true; }
	public Object concreteValue() {
	    return VM_Address.fromObject(value).asObject();
	}
	public String toString() {
	    return "<" + ConcreteArray.this.getBlueprint() + " reference = " + value + ">";
	}
	public J2cValue copy(ValueSource vs) {
	    return new ConcreteArray(vs,
				     (S3Blueprint.Array) ConcreteArray.this.getBlueprint(),
				     value);
	}
    }

    public static class J2cInt extends J2cNumeric implements Int {
	int min = Integer.MIN_VALUE;
	int max = Integer.MAX_VALUE;

	public String toString() {
	    return ("<int:" + typeCode + "[" + min + "," + max +"]"
		    + (name == null
		       ? ">"
		       : (" " + name + ">")));
	}

	J2cInt(ValueSource vs, char code, int value) {
	    super(vs, new java.lang.Integer(value), code);
	    min = max = value;
	}

	J2cInt(ValueSource vs, char code, int min, int max) {
	    super(vs, null, code);
	    this.min = min;
	    this.max = max;
	}
	J2cInt(ValueSource vs, Number value) {
	    super(vs, value, TypeCodes.INT);
	    this.max = this.min = value.intValue();
	}

	static final int[] minValue = new int[128];
	static final int[] maxValue = new int[128];
	static {
	    minValue[TypeCodes.BOOLEAN] = 0;
	    maxValue[TypeCodes.BOOLEAN] = 1;
	    minValue[TypeCodes.BYTE]  = -128;
	    maxValue[TypeCodes.BYTE]  = 127;
	    minValue[TypeCodes.UBYTE] = 0;
	    maxValue[TypeCodes.UBYTE] = 255;
	    minValue[TypeCodes.SHORT] = -32768;
	    maxValue[TypeCodes.SHORT] = 32767;
	    minValue[TypeCodes.CHAR]  = 0;
	    maxValue[TypeCodes.CHAR]  = 65536;
	    minValue[TypeCodes.INT]   = Integer.MIN_VALUE;
	    maxValue[TypeCodes.INT]   = Integer.MAX_VALUE;
	    // FIXME: can't represent UINT as int.
	    minValue[TypeCodes.UINT]  = Integer.MIN_VALUE;
	    maxValue[TypeCodes.UINT]  = Integer.MAX_VALUE;
	}
	    
	J2cInt(ValueSource vs, char code) {
	    super(vs, null, code);
	    this.typeCode = code;
	    min = minValue[code];
	    max = maxValue[code];
	}
	/**
	 * Return true if this value is in the range of the given
	 * integer type.
	 **/
	public boolean inRange(char code) {
	    return min >= minValue[code] && max <= maxValue[code];
	}
	J2cNumeric make(ValueSource vs, Number val) {
	    return new J2cInt(vs, val);
	}
	
	protected String getTypeInternal() {
	    return typeCodeToCtype[typeCode];
	}

	public Type getJavaType(S3Domain d) {
	    switch (typeCode) {
	    case TypeCodes.BOOLEAN:return d.BOOLEAN;
	    case TypeCodes.BYTE:   return d.BYTE;
	    case TypeCodes.SHORT:  return d.SHORT;
	    case TypeCodes.CHAR:   return d.CHAR;
	    case TypeCodes.INT:    return d.INT;
	    default:
		throw new Error("no java type for " + typeCode);
	    }
	}

	public J2cValue copy(ValueSource vs) {
	    return new J2cInt(vs, typeCode, min, max);
	}

	public boolean isConcrete() { return min == max; }
	public Object concreteValue() {
	    return isConcrete() ? new Integer(min) : null;
	}
	public int intValue() {
	    assert(min == max);
	    return min;
	}
	public boolean isPrimitive() { return true; }
	public boolean isInt()       { return true; }
	public Int getInt()          { return this; }
	public boolean isFloat()     { return false; }
	public Float getFloat()      { throw new AbstractValueError(); }

	public AbstractValue merge(AbstractValue _other) {
	    if (this == _other)
		return this;
	    else if (_other instanceof J2cInt) {
		J2cInt other = (J2cInt) _other;

		char mergedType;
		if (typeCode == other.typeCode)
		    mergedType = typeCode;
		else
		    mergedType = TypeCodes.INT;
	    
		return new J2cInt(phi(other), mergedType,
				  min < other.min ? min : other.min,
				  max > other.max ? max : other.max);
	    } else if (_other instanceof J2cReference)
		return merge(new J2cInt(new ConversionExp((J2cValue) _other),
					SIZE_T_CODE));
	    else
		return super.merge(_other);
	}

	public boolean includes(AbstractValue other) {
	    if (other instanceof J2cInt) {
		J2cInt i = (J2cInt) other;
		// what about type codes?
		return (min <= i.min && max >= i.max);
	    } else return false;
	}
    }

    static char promoteInt(char t1, char t2) {
	return t1 == t2 ? t1 : TypeCodes.INT;
    }

    public static abstract class J2cNumeric extends J2cValue {
	java.lang.Number value;
	char typeCode;

	public Object concreteValue() { return value; }

	public boolean isConcrete() { return value != null; }

	public boolean isLong()   { return (typeCode == TypeCodes.LONG
					    || typeCode == TypeCodes.ULONG); }
	public Long getLong()     { return (Long) this; }
	public boolean isDouble() { return typeCode == TypeCodes.DOUBLE; }
	public Double getDouble() { return (Double) this; }
	public boolean isInt()    { return false; }
	public Int getInt()       { throw new AbstractValueError(); }
	public boolean isFloat()  { return typeCode == TypeCodes.FLOAT; }
	public Float getFloat()   { return (Float) this; }

	public boolean isPrimitive() { return isFloat() || isInt(); }
	public Primitive getPrimitive() { return (Primitive) this; }
	public boolean isWidePrimitive() { return isLong() || isDouble(); }
	public WidePrimitive getWidePrimitive() {
	    return (WidePrimitive) this;
	}

	public int intValue() { return value.intValue(); }

	J2cNumeric(ValueSource vs, Number value, char typeCode) {
	    super(vs);
	    this.value = value;
	    this.typeCode = typeCode;
	}

	protected String getTypeInternal() {
	    return typeCodeToCtype[typeCode];
	}

	public Type getJavaType(S3Domain d) {
	    switch (typeCode) {
	    case TypeCodes.FLOAT:  return d.FLOAT;
	    case TypeCodes.DOUBLE: return d.DOUBLE;
	    case TypeCodes.LONG:   return d.LONG;
	    default:
		throw new Error("no java type for " + typeCode);
	    }
	}

	abstract J2cNumeric make(ValueSource vs, Number val);
	
	public AbstractValue merge(AbstractValue other) {
	    if (this == other)
		return this;
	    if (getClass() == other.getClass())
		return make(phi(other), (value == ((J2cNumeric) other).value
					 ? value
					 : null));
	    else
		return super.merge(other);
	}

	public boolean includes(AbstractValue other) {
	    return (getClass() == other.getClass()
		    && (concreteValue() == null
			? true
			: concreteValue().equals
			    (((J2cNumeric) other).concreteValue())));
	}

	public J2cValue copy(ValueSource vs) {
	    return make(vs, value);
	}
    }

    public static class J2cLong extends J2cNumeric implements Long {
	J2cLong(ValueSource vs, Number value, char typeCode) {
	    super(vs, value, typeCode);
	}
	J2cNumeric make(ValueSource vs, Number val) {
	    return new J2cLong(vs, val, TypeCodes.LONG);
	}
	
	public AbstractValue merge(AbstractValue other) {
	    if (this == other)
		return this;
	    if (typeCode == TypeCodes.ULONG
		&& other instanceof J2cLong
		&& ((J2cLong) other).typeCode == TypeCodes.ULONG)
	    {
		J2cLong ret = (J2cLong) super.merge(other);
		ret.typeCode = TypeCodes.ULONG;
		return ret;
	    }
	    else return super.merge(other);
	}

	public J2cValue copy(ValueSource vs) {
	    return new J2cLong(vs, value, typeCode);
	}
    }

    public static class J2cFloat extends J2cNumeric implements Float {
	J2cFloat(ValueSource vs, Number value) {
	    super(vs, value, TypeCodes.FLOAT);
	}
	J2cNumeric make(ValueSource vs, Number val) {
	    return new J2cFloat(vs, value);
	}
	
    }
    
    public static class J2cDouble extends J2cNumeric implements Double {
	J2cDouble(ValueSource vs, Number val) {
	    super(vs, val, TypeCodes.DOUBLE);
	}
	J2cNumeric make(ValueSource vs, Number val) {
	    return new J2cDouble(vs, val);
	}
    }

    static final boolean KEEP_CACHE_STATS = J2cImageCompiler.KEEP_STATS;

    /*
     * Memoize common leaf expressions and values
     */
    private static ValueSource[] localExp = new ValueSource[0];
    private static int localExpHits;
    public static ValueSource makeLocalExp(int idx) {
	if (idx >= localExp.length) {
	    ValueSource[] nlocals = new ValueSource[(idx + 16) & ~0xf];
	    System.arraycopy(localExp, 0, nlocals, 0, localExp.length);
	    for (int i = localExp.length; i < nlocals.length; i++)
		nlocals[i] = new J2cLocalExp(i);
	    localExp = nlocals;
	    
	}
	if (KEEP_CACHE_STATS)
	    localExpHits++;
	return localExp[idx];
    }

    private static MethodReference[][] nvTarget;
    private static int nvTargetHits;
    public static MethodReference makeNonvirtualReference(Method method) {
	// It is PROBABLY safe to call S3Method.getCount() from a
	// static initializer, but it seems fragile
	if (nvTarget == null) {
	    nvTarget = new MethodReference[DomainDirectory.maxContextID()+1][];
	    for (int i = 0; i < nvTarget.length; i++) {
		Type.Context tc = DomainDirectory.getContext(i);
		if (tc != null)
		    nvTarget[i] = new MethodReference[tc.getMethodCount()];
	    }
	}

	int cid = method.getCID();
	int uid = method.getUID();
	// We now compile the user domain before the executive domain
	// has been analyzed.  This means that the number of methods
	// in the executive domain will suddenly increase once we move
	// on to it.
	if (uid > nvTarget[cid].length) {
	    Type.Context tc = DomainDirectory.getContext(cid);
	    MethodReference[] newNvt = new MethodReference[tc.getMethodCount()];
	    System.arraycopy(nvTarget[cid], 0, newNvt, 0, nvTarget[cid].length);
	    nvTarget[cid] = newNvt;
	}
	MethodReference ret = nvTarget[cid][uid];
	if (ret == null) {
	    nvTarget[cid][uid] = ret
		= new MethodReference(J2cFormat.format(method),
				      (S3Method) method);
	}
	if (KEEP_CACHE_STATS)
	    nvTargetHits++;
	return ret;
    }

    public static boolean isMethodNamed(Method m) {
	return nvTarget[m.getCID()][m.getUID()] != null;
    }

    private static HashMap symbolicRefs = new HashMap();
    private static int symbolicRefHits = 0;
    public static InternalReference makeSymbolicReference(String name) {
	InternalReference ret = (InternalReference) symbolicRefs.get(name);
	if (ret == null) {
	    ret = new InternalReference(name);
	    symbolicRefs.put(name, ret);
	}
	if (KEEP_CACHE_STATS)
	    symbolicRefHits++;
	return ret;
    }

    // big enough for INVOKE_NATIVE numbers, probably most interface
    // method numbers
    private static final int SMALL_CONSTANT_SIZE = 256;
    private static final char[] intCode = new char[] {
	TypeCodes.BOOLEAN,	// unused in IR
	TypeCodes.BYTE,
	TypeCodes.UBYTE,
	TypeCodes.SHORT,
	TypeCodes.USHORT,
	TypeCodes.CHAR,		// unused in IR
	TypeCodes.INT,
	TypeCodes.UINT		// unused in IR
    };
    private static J2cInt[][] smallConstant = new J2cInt[128][];
    private static int[] smallConstantHits = new int[128];
    private static int smallConstantMisses = 0;
    static {
	for (int i = 0; i < intCode.length; i++) {
	    char code = intCode[i];
	   /* int sz = (code == TypeCodes.BOOLEAN ? 2
			: code == TypeCodes.BYTE ? 128
			: SMALL_CONSTANT_SIZE);
	  */
            smallConstant[code] = new J2cInt[SMALL_CONSTANT_SIZE];
	    for (int j = 0; j < smallConstant[code].length; j++)
		smallConstant[code][j] = new J2cInt(null, code, j);
	}
    }

    public static J2cNumeric makeAnyConstant(Number value) {
	if (value instanceof java.lang.Integer)
	    return makeIntConstant(TypeCodes.INT,
				   ((Integer)value).intValue());
	else if (value instanceof java.lang.Float)
	    return new J2cFloat(null, value);
	else if (value instanceof java.lang.Double)
	    return new J2cDouble(null, value);
	else if (value instanceof java.lang.Long)
	    return new J2cLong(null, value, TypeCodes.LONG);
	else
	    throw new Error("What is this: " + value.getClass());
    }
    public static J2cInt makeIntConstant(char tag, int value) {
	if (value < 0 || value >= SMALL_CONSTANT_SIZE) {
	    if (KEEP_CACHE_STATS)
		smallConstantMisses++;
	    return new J2cInt(null, tag, value);
	}
	if (KEEP_CACHE_STATS)
	    smallConstantHits[tag]++;
	return smallConstant[tag][value];
    }

    static {
	if (KEEP_CACHE_STATS)
	    new J2cImageCompiler.StatPrinter() {
		public void printStats() {
		    System.err.println("\nJ2cValue:\n");
		    System.err.println("Local References:");
		    System.err.println("\tSlots Allocated:  "
				       + localExp.length);
		    System.err.println("\tValues Allocated: "
				       + countNonNull(localExp));
		    System.err.println("\tHits:             "
				       + localExpHits);
		    System.err.println("Nonvirtual Methods:");
		    System.err.println("\tSlots Allocated:  "
				       + nvTarget.length);
		    System.err.println("\tValues Allocated: "
				       + countNonNull(nvTarget));
		    System.err.println("\tHits:             "
				       + nvTargetHits);
		    System.err.println("Symbolic Constants:");
		    System.err.println("\tValues Allocated: "
				       + symbolicRefs.size());
		    System.err.println("\tHits:             "
				       + symbolicRefHits);
		    for (int i = 0; i < intCode.length; i++) {
			System.err.println("Integer Constants ("
					   + intCode[i]
					   + ")");
			System.err.println("\tValues Allocated:  "
					   + smallConstant[intCode[i]].length);
			System.err.println("\tHits:             "
					   + smallConstantHits[intCode[i]]);
		    }
		    System.err.println("Integer Constant Misses: "
				       + smallConstantMisses);

		    // Candidates for caching:
		    System.err.println("SharedStateReferences alloced:      "
				       + shStCnt);
		    System.err.println("direct BlueprintReferences alloced: "
				       + bpLitCnt);
		    System.err.println("J2cJumpTargets alloced:             "
				       + jumpTargetCnt);
		}
	    };
    }

    static final J2cValue j2cInvokeTarget =
	makeSymbolicReference("j2cInvoke");
    static final J2cValue runTarget =
	makeSymbolicReference("run");
    static final J2cValue to_jvalueTarget =
	makeSymbolicReference("to_jvalue");
}

