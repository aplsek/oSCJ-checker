package s3.core.domain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ovm.core.Executive;
import ovm.core.domain.Blueprint;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Field;
import ovm.core.domain.JavaUserDomain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.ReflectiveArray;
import ovm.core.domain.ReflectiveConstructor;
import ovm.core.domain.ReflectiveField;
import ovm.core.domain.ReflectiveMethod;
import ovm.core.domain.ReflectiveVirtualFunction;
import ovm.core.domain.Type;
import ovm.core.domain.WildcardException;
import ovm.core.execution.InstantiationMessage;
import ovm.core.execution.InvocationMessage;
import ovm.core.execution.NativeConstants;
import ovm.core.execution.ReturnMessage;
import ovm.core.execution.RuntimeExports;
import ovm.core.execution.ValueUnion;
import ovm.core.execution.Native;
import ovm.core.repository.Descriptor;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.memory.VM_Area.Destructor;
import ovm.util.HashSet;
import ovm.util.Mem;
import ovm.util.OVMError;
import ovm.util.UnicodeBuffer;
import s3.services.transactions.Transaction;
import s3.util.PragmaAtomic;
import ovm.core.services.memory.PragmaNoReadBarriers;
import s3.util.queues.SingleLinkElement;
import s3.util.queues.SingleLinkQueue;
import ovm.util.Map;
import ovm.util.HashMap;
import ovm.core.stitcher.InvisibleStitcher;
import s3.util.PragmaMayNotLink;

public class S3JavaUserDomain extends S3Domain implements JavaUserDomain {


    protected final S3TypeContext appContext_;
    protected final String classPath;

    private final ReflectiveMethod main;

    public String toString() {
	return "UserDomain_" + ((int)instanceCounter_);
    }
    
    
    private boolean isFrozen = false;
    
    static private class Dumper extends ByteArrayOutputStream {
	int fd;
	HashSet seenMethods = new HashSet();
	HashSet seenClasses = new HashSet();
	RuntimeExports myRTE =
	    DomainDirectory.getExecutiveDomain().getRuntimeExports();
	StringBuffer forNameBuf = new StringBuffer(65535);
	
	Dumper(String _name) {
	    super(2*(1<<16) + 2);
	    Oop name = VM_Address.fromObject(_name).asOop();
	    int flags = (NativeConstants.O_WRONLY|
			 NativeConstants.O_APPEND|
			 NativeConstants.O_CREAT);
	    fd = myRTE.open(name, flags, 0644);
	}

	synchronized void dump(Selector.Method sel) {
	    if (!seenMethods.contains(sel)) {
		VM_Area imm = MemoryManager.the().getImmortalArea();
		VM_Area r = MemoryManager.the().setCurrentArea(imm);
		try {
		    sel.getDefiningClass().write(this);
		    write(' ');
		    sel.getUnboundSelector().write(this);
		    write('\n');
		    myRTE.write(fd, VM_Address.fromObject(buf).asOop(),
				0, count, true);
		    reset();
		    // this should be the only allocation point
		    seenMethods.add(sel);
		}
		catch (IOException e) { Executive.panicOnException(e); }
		finally { MemoryManager.the().setCurrentArea(r); }
	    }
	}

	synchronized void dump(TypeName.Compound tn) {
	    if (!seenClasses.contains(tn)) {
		VM_Area imm = MemoryManager.the().getImmortalArea();
		VM_Area r = MemoryManager.the().setCurrentArea(imm);
		try {
		    // JavaFormat leaks like a seive
		    JavaFormat._.format(tn, forNameBuf);
		    for (int i = 0; i < forNameBuf.length(); i++)
			write(forNameBuf.charAt(i));
		    forNameBuf.setLength(0);
		    write('\n');
		    myRTE.write(fd, VM_Address.fromObject(buf).asOop(),
				0, count, true);
		    reset();
		    // it would be nice if this where the only
		    // allocation point.
		    seenClasses.add(tn);
		}
		finally { MemoryManager.the().setCurrentArea(r); }
	    }
	}
    }

    ReflectiveConstructor[] wrappers = new ReflectiveConstructor[128];
    {
	wrappers['B'] =
	    new ReflectiveConstructor(this, JavaNames.java_lang_Byte,
				      new TypeName[] { TypeName.BYTE });
	wrappers['C'] =
	    new ReflectiveConstructor(this, JavaNames.java_lang_Character,
				      new TypeName[] { TypeName.CHAR });
	wrappers['D'] =
	    new ReflectiveConstructor(this, JavaNames.java_lang_Double,
				      new TypeName[] { TypeName.DOUBLE });
	wrappers['F'] =
	    new ReflectiveConstructor(this, JavaNames.java_lang_Float,
				      new TypeName[] { TypeName.FLOAT });
	wrappers['I'] =
	    new ReflectiveConstructor(this, JavaNames.java_lang_Integer,
				      new TypeName[] { TypeName.INT });
	wrappers['J'] =
	    new ReflectiveConstructor(this, JavaNames.java_lang_Long,
				      new TypeName[] { TypeName.LONG });
	wrappers['S'] =
	    new ReflectiveConstructor(this, JavaNames.java_lang_Short,
				      new TypeName[] { TypeName.SHORT });
	wrappers['Z'] =
	    new ReflectiveConstructor(this, JavaNames.java_lang_Boolean,
				      new TypeName[] { TypeName.BOOLEAN });
    }

    Dumper invocationDumper;
    Dumper forNameDumper;


    public Oop invokeMethod(Oop receiver, Method theMethod, Oop argArray,
			    Oop callerClass) throws PragmaNoReadBarriers {
	try {
	    S3Method method = (S3Method)theMethod;

	    if (invocationDumper != null)
		invocationDumper.dump(method.getSelector());

	    Type.Scalar objectType = commonTypes().java_lang_Object;
	   // S3Blueprint.Scalar objectBP =
	//	(S3Blueprint.Scalar)blueprintFor(objectType).asScalar();
	    Type.Array objectArrayType = makeType(objectType, 1);
	    S3Blueprint.Array objectArrayBP =
		(S3Blueprint.Array)blueprintFor(objectArrayType).asArray();
	    int argOopArrayLength = argArray == null ? 0 : 
                objectArrayBP.getLength(argArray);
	    Descriptor.Method desc = method.getExternalSelector().getDescriptor();
	    assert(argOopArrayLength == desc.getArgumentCount());
	    desc = method.getSelector().getDescriptor();
	    int[] synthetic = method.getSyntheticParameterOffsets();
	    assert synthetic.length < 2: "unknown synthetic parameter";
	    TypeName[] argTypeNames = new TypeName[desc.getArgumentCount()];
	    for(int i = 0; i < argTypeNames.length; i++)
		argTypeNames[i] = desc.getArgumentType(i);
	    InvocationMessage msg = 
		new InvocationMessage(method);
	    for(int i = 0; i < argOopArrayLength; i++) {
		Oop e = null;
		
		if (MemoryManager.the().usesArraylets()) {
		  e = MemoryManager.the().addressOfElement( VM_Address.fromObject( argArray ), i , objectArrayBP.getComponentSize()).getAddress().asOopUnchecked();
		} else {
		  e = objectArrayBP
		    .addressOfElement(argArray, i).getAddress().asOopUnchecked();
		}

		setValueUnionFromWrappedObject(msg.getInArgAt(i),
					       argTypeNames[i],
					       e);
	    }

	    if (synthetic.length > 0)
		msg.getInArgAt(synthetic[0]).setOop(callerClass);
            // static methods have to be invoked on the shared-state instance
            // and we ignore any receiver the user passed us (which is 
            // typically null for a static method)
            Type.Compound declType = method.getDeclaringType();
	    if (declType.isSharedState()) {
		receiver = 
                    blueprintFor(declType).getSharedState();
	    }
            ReturnMessage ret = msg.invoke(receiver);
            ret.rethrowWildcard(); // may not return
	    if( !method.getReturnType().isPrimitive())
		return ret.getReturnValue().getOop();
	    //have to wrap the return value if the return type is primitive

	    char typeTag = 
		((S3Type.Primitive) method.getReturnType()).getName().getTypeTag();
	    if (typeTag == 'V')
		return null;
	    InstantiationMessage im = wrappers[typeTag].makeMessage();
	    switch(typeTag){
	    case 'B': //wrap in Byte
		im.getInArgAt(0).setByte(ret.getReturnValue().getByte());
		break;
	    case 'C': //wrap in Character
		im.getInArgAt(0).setChar(ret.getReturnValue().getChar());
		break;
	    case 'D': //wrap in Double
		im.getInArgAt(0).setDouble(ret.getReturnValue().getDouble());
		break;
	    case 'F': //wrap in Float
		im.getInArgAt(0).setFloat(ret.getReturnValue().getFloat());
		break;
	    case 'I': //wrap in Integer
		im.getInArgAt(0).setInt(ret.getReturnValue().getInt());
		break;
	    case 'J': //wrap in Long
		im.getInArgAt(0).setLong(ret.getReturnValue().getLong());
		break;
	    case 'S': //wrap in Short
		im.getInArgAt(0).setShort(ret.getReturnValue().getShort());
		break;
	    case 'Z': //wrap in Boolean
		im.getInArgAt(0).setBoolean(ret.getReturnValue().getBoolean());
		break;
	    default:
		throw new OVMError.IllegalArgument("Unexpected type tag " + typeTag);
	    }
	    ReturnMessage retmsg = im.instantiate();
	    retmsg.rethrowWildcard(); // may not return
	    Oop res = retmsg.getReturnValue().getOop();
	    return res;
	} catch (LinkageException e) {
	    d("Linkage exception in S3JavaUserDomain.invokeMethod");
	    e.printStackTrace();
	    throw Executive.panicOnException(e);
	}
    }

    /**
     * Reflective invocation of a constructor in this domain.
     * @param constructorMethod the method object representing the constructor
     * @param argArray the arguments to pass to the constructor. This
     * array should match the number and types of the parameters declared by 
     * the constructor, except that for primitive parameters, passed via
     * wrappers, widening conversions are applied (eg. a Byte can be used to
     * set a byte, short, int, long, float or double).
     * @return the constructed instance
     */
    public Oop newInstance(Method constructorMethod, Oop argArray,
			   Oop callerClass) {
	try {
	    S3Method method = (S3Method)constructorMethod;

	    if (invocationDumper != null)
		invocationDumper.dump(method.getSelector());

	    Type.Scalar objectType = commonTypes().java_lang_Object;
	    Type.Array objectArrayType = makeType(objectType, 1);
	    S3Blueprint.Array objectArrayBP =
		(S3Blueprint.Array)blueprintFor(objectArrayType).asArray();
	    int argOopArrayLength = argArray == null ? 0 : 
                objectArrayBP.getLength(argArray);
	    Descriptor.Method desc = method.getExternalSelector().getDescriptor();
	    assert argOopArrayLength == desc.getArgumentCount():
		"arg mismatch:  arg array length = " + argOopArrayLength
		+ " expected arg count = " + desc.getArgumentCount();
	    int[] synthetic = method.getSyntheticParameterOffsets();
	    if (synthetic.length > 1)
		throw Executive.panic("unknown synthetic parameter");
	    desc = method.getSelector().getDescriptor();
	    TypeName[] argTypeNames = new TypeName[desc.getArgumentCount()];
	    for(int i = 0; i < argTypeNames.length; i++)
		argTypeNames[i] = desc.getArgumentType(i);
	    InstantiationMessage msg = 
		new InstantiationMessage(method.getDeclaringType().asScalar(), 
					 argTypeNames);
	    for(int i = 0; i < argOopArrayLength; i++) {
		Oop e = null;
		    
		if (MemoryManager.the().usesArraylets()) {
		  e = MemoryManager.the().addressOfElement( VM_Address.fromObject( argArray ), i , objectArrayBP.getComponentSize()).getAddress().asOopUnchecked();
		} else {
		  e = objectArrayBP
		    .addressOfElement(argArray, i).getAddress().asOopUnchecked();
		}
    
		setValueUnionFromWrappedObject(msg.getInArgAt(i),
					       argTypeNames[i],
					       e);
	    }
	    if (synthetic.length > 0)
		msg.getInArgAt(synthetic[0]).setOop(callerClass);
	    ReturnMessage ret = msg.instantiate();
            ret.rethrowWildcard(); // may not return
            return ret.getReturnValue().getOop();
	} catch (LinkageException e) {
	    throw Executive.panicOnException(e);
	}
    }

    public void observeForName(TypeName tn) {
	if (forNameDumper != null && tn.isCompound())
	    forNameDumper.dump(tn.asCompound());
    }
    
    // classpath-specific: we peek inside java-level boxes both
    // because it is faster, and because attempts to call methods on
    // them trip over RTSJ scope checks
    private ReflectiveField.Integer Integer_value =
	new ReflectiveField.Integer(this,
				    JavaNames.java_lang_Integer, "value");
    private ReflectiveField.Short Short_value =
	new ReflectiveField.Short(this,
				  JavaNames.java_lang_Short, "value");
    private ReflectiveField.Character Character_value =
	new ReflectiveField.Character(this,
				      JavaNames.java_lang_Character, "value");
    private ReflectiveField.Boolean Boolean_value =
	new ReflectiveField.Boolean(this,
				    JavaNames.java_lang_Boolean, "value");
    private ReflectiveField.Byte Byte_value =
	new ReflectiveField.Byte(this,
				 JavaNames.java_lang_Byte, "value");
    private ReflectiveField.Long Long_value =
	new ReflectiveField.Long(this,
				 JavaNames.java_lang_Long, "value");
    private ReflectiveField.Float Float_value =
	new ReflectiveField.Float(this,
				  JavaNames.java_lang_Float, "value");
    private ReflectiveField.Double Double_value =
	new ReflectiveField.Double(this,
				   JavaNames.java_lang_Double, "value");

    /**
     * Set a ValueUnion appropriately based on the expected type, and
     * accounting for primitive values that are wrapped, and for which
     * widening conversions may be applied.
     */
    private void setValueUnionFromWrappedObject(ValueUnion vu, TypeName tn, 
                                                Oop value) {

        int tag = tn.getTypeTag();
	if (tag == TypeCodes.OBJECT || tag == TypeCodes.ARRAY ||
            tag == TypeCodes.GEMEINSAM) {
	    vu.setOop(value);
            return;
        }

        // we're dealing with a primitive

        // if value is a wrapper of a primitive we need to peek inside to see
        // what primitive it wraps. value itself is just an oop, so we have to
        // check the actual typename
        int valTag = TypeCodes.OBJECT;
        TypeName valTn = value.getBlueprint().getType().getUnrefinedName();
        if (valTn == JavaNames.java_lang_Boolean)
            valTag = TypeCodes.BOOLEAN;
        else if (valTn == JavaNames.java_lang_Character)
            valTag = TypeCodes.CHAR;
        else if (valTn == JavaNames.java_lang_Byte)
            valTag = TypeCodes.BYTE;
        else if (valTn == JavaNames.java_lang_Short)
            valTag = TypeCodes.SHORT;
        else if (valTn == JavaNames.java_lang_Integer)
            valTag = TypeCodes.INT;
        else if (valTn == JavaNames.java_lang_Long)
            valTag = TypeCodes.LONG;
        else if (valTn == JavaNames.java_lang_Float)
            valTag = TypeCodes.FLOAT;
        else if (valTn == JavaNames.java_lang_Double)
            valTag = TypeCodes.DOUBLE;


	switch(tag) {
	case TypeCodes.BOOLEAN:
	    vu.setBoolean(Boolean_value.get(value));
	    break;
	case TypeCodes.CHAR:
	    vu.setChar(Character_value.get(value));
	    break;
	case TypeCodes.BYTE:
	    vu.setByte(Byte_value.get(value));
	    break;
	case TypeCodes.SHORT: {
            short newVal = 0;
            switch(valTag) {
            case TypeCodes.BYTE:
                newVal = Byte_value.get(value); break;
            case TypeCodes.SHORT:
                newVal = Short_value.get(value); break;
            }
	    vu.setShort(newVal);
	    break;
        }
	case TypeCodes.INT: {
            int newVal = 0;
            switch(valTag) {
            case TypeCodes.BYTE:
                newVal = Byte_value.get(value); break;
            case TypeCodes.SHORT:
                newVal = Short_value.get(value); break;
            case TypeCodes.CHAR:
                newVal = Character_value.get(value); break;
            case TypeCodes.INT:
                newVal = Integer_value.get(value); break;
            }
	    vu.setInt(newVal);
	    break;
        }
	case TypeCodes.LONG: {
            long newVal = 0;
            switch(valTag) {
            case TypeCodes.BYTE:
                newVal = Byte_value.get(value); break;
            case TypeCodes.SHORT:
                newVal = Short_value.get(value); break;
            case TypeCodes.CHAR:
                newVal = Character_value.get(value); break;
            case TypeCodes.INT:
                newVal = Integer_value.get(value); break;
            case TypeCodes.LONG:
                newVal = Long_value.get(value); break;
            }
	    vu.setLong(newVal);
	    break;
        }
	case TypeCodes.FLOAT: {
            float newVal = 0;
            switch(valTag) {
            case TypeCodes.BYTE:
                newVal = Byte_value.get(value); break;
            case TypeCodes.SHORT:
                newVal = Short_value.get(value); break;
            case TypeCodes.CHAR:
                newVal = Character_value.get(value); break;
            case TypeCodes.INT:
                newVal = Integer_value.get(value); break;
            case TypeCodes.LONG:
                newVal = Long_value.get(value); break;
            case TypeCodes.FLOAT:
                newVal = Float_value.get(value); break;
            }
	    vu.setFloat(newVal);
	    break;
        }
	case TypeCodes.DOUBLE: {
            double newVal = 0;
            switch(valTag) {
            case TypeCodes.BYTE:
                newVal = Byte_value.get(value); break;
            case TypeCodes.SHORT:
                newVal = Short_value.get(value); break;
            case TypeCodes.CHAR:
                newVal = Character_value.get(value); break;
            case TypeCodes.INT:
                newVal = Integer_value.get(value); break;
            case TypeCodes.LONG:
                newVal = Long_value.get(value); break;
            case TypeCodes.FLOAT:
                newVal = Float_value.get(value); break;
            case TypeCodes.DOUBLE:
                newVal = Double_value.get(value); break;
            }
	    vu.setDouble(newVal);
	    break;
        }
	default:
	    throw new OVMError.IllegalArgument("Unexpected type tag " + tn.getTypeTag());
	}
    }


    protected final ReflectiveVirtualFunction finalizeVF =
	new ReflectiveVirtualFunction(this,
				      JavaNames.java_lang_Object_finalize);
    protected final ReflectiveMethod finalizeBase =
	new ReflectiveMethod(this, JavaNames.java_lang_Object_finalize);

    public Method findFinalizer(Oop oop) {
	Method ret = finalizeVF.findMethod(oop);
	if (ret == finalizeBase.getMethod())
	    return null;
	return ret;
    }

    private SingleLinkQueue finalizeQ = new SingleLinkQueue();

    private class FinalizeNode extends Destructor
	implements SingleLinkElement
    {
	SingleLinkElement next;
	Oop unboxed;

	public void setNext(SingleLinkElement n) { next = n; }
	public SingleLinkElement getNext() { return next; }

	public int getKind() { return NORMAL; }

	public void destroy(VM_Area heap) throws PragmaAtomic {
	    unboxed = heap.revive(this);
	    if (finalizeQ.isEmpty())
		synchronized (finalizeQ) {
		    finalizeQ.notify();
		}
	    finalizeQ.add(this);
	}

	public FinalizeNode(Oop fin) {
	    super(fin);
	}
    }
    
    public void registerFinalizer(Oop oop) {
	ReflectiveMethod rm = finalizeVF.dispatch(oop);
	if (rm.getMethod().getDeclaringType() != ROOT_TYPE) {
	    VM_Area a = MemoryManager.the().getHeapArea();
	    a.addDestructor(new FinalizeNode(oop));
	}
    }

    private int finCount = 0;
    
    private Oop nextFinalizer() throws PragmaAtomic {
	synchronized (finalizeQ) {
	    while (finalizeQ.isEmpty())
		try {
		    BasicIO.out.println("[finalizer thread sleeping after "
					+ finCount + " objects finalized]");
		    finalizeQ.wait();
		    finCount = 0;
		}
		catch (InterruptedException _) { }
	}
	FinalizeNode n = (FinalizeNode) finalizeQ.take();
	return n.unboxed;
    }

    public void runFinalizers() {
	while (true) {
	    Oop oop = nextFinalizer();
	    ReflectiveMethod rm = finalizeVF.dispatch(oop);
	    try {
		rm.call(oop);
	    } catch (WildcardException _) {
		BasicIO.out.println("exception in heap finalizer");
	    }
	    finCount++;
	}
    }

    public boolean isExecutive() {
	return false;
    }
    
    protected void newTypeHook(Type.Compound type)
	throws LinkageException, PragmaMayNotLink
    {
        super.newTypeHook(type);
	if (isFrozen) {
            throw 
		new LinkageException.DomainFrozen(type.getName()).unchecked();
	}
        if (subtyping == null) {
            pln("will die: trying to load  " + type);
        }
    }

    /**
     * In the user domain, pragma exceptions live in a VM-specific
     * package (org.ovmj.util).  We translate a pragma typename into a
     * user-domain type by looking for a type with the same simple
     * name in our magic package.
     **/
    public static final int PRAGMA_MAGIC_PKG =
	RepositoryUtils.asUTF("org/ovmj/util");

    /**
     * Map an ED TypeName to either a UD type (for pragmas with UD
     * peers), or Boolean.FALSE (for ED-only pragmas).  This cache
     * avoids both creating new TypeName.Scalar objects and repeatedly
     * attempting to load noexistent caches.
     **/
    private final Map pragmaCache = new HashMap();
    
    // I was using Boolean.FALSE to mark ED-only pragmas, however, the
    // value of this constant changes between build time and run time.
    private static final Object NOT_FOUND = new Object();
    
    /**
     * Map an ED TypeName to either a UD type (for pragmas with UD
     * peers), or null (for ED-only pragmas).  We cache results to
     * avoid repeatedly attempting to load nonexistent classes.
     **/
    public Type getPragma(TypeName.Scalar exc) {
	Object ret = pragmaCache.get(exc);
	if (ret == NOT_FOUND)
	    return null;
	else if (ret == null) {
	    try {
		TypeName.Scalar renamed = 
		    TypeName.Scalar.make(PRAGMA_MAGIC_PKG,
					 exc.getShortNameIndex());
		ret = getSystemTypeContext().typeFor(renamed);
		pragmaCache.put(exc, ret);
	    } catch (LinkageException _) {
		ret = null;
		pragmaCache.put(exc, NOT_FOUND);
	    }
	}
	return (Type) ret;
    }

    // invoke the main method of our main class reflectively so that it
    // executes in this domain. (The code here actually executes in the
    // executive domain.)
    private void runReflectively(String[] arguments) throws LinkageException {
	myRTE.setCommandlineArgumentStringArray(arguments);
        try {
            main.call(null);
        }
        catch(WildcardException wc) {
            Oop exc = wc.getUserThrowable();
            if (exc != null) {
                // can't throw a user-domain exception so replace with this
                throw new OVMError("Uncaught user-domain exception: " + 
                                   getMessageFromThrowable(exc));
            }
            Throwable t = wc.getExecutiveThrowable();
            // No ED exceptions should escape the UD, but this accounts for
            // bugs, omissions and oversights. We don't want to silently
            // swallow these if they happen to occur.
            if (t != null) {
                if (t instanceof Error) throw (Error) t;
                if (t instanceof RuntimeException) throw (RuntimeException) t;
                throw (Error) new Error("uncaught checked ED exception").initCause(t);
            }
        }
    }


    final static Selector.Field detailMessageSel = 
	RepositoryUtils.fieldSelectorFor("Ljava/lang/Throwable;",
					 "detailMessage:Ljava/lang/String;");

    private String getMessageFromThrowable(Oop oopThrowable) {
	Blueprint bp = oopThrowable.getBlueprint();
	Type.Scalar type = bp.getType().asScalar();
        assert type.getDomain() == this : "Domain mismatch";
        Field.Reference detailMessageFld = (Field.Reference) type.getField(detailMessageSel.getUnboundSelector());
	if (detailMessageFld == null)
	    return type.getUnrefinedName().toString();
	return type.getUnrefinedName() + ": " + 
            getString(detailMessageFld.get(oopThrowable));
	
    }


    public void startup() {
        super.startup();
        pln("Initializing the user domain");
        myCSA.boot();
	pln("csa booted");
        Transaction.the().boot();
	pln("transactions booted");
        // sync is off by default when constructed, but for the UD we can
        // enable it straight away
        myCSA.enableSynchronization();
	pln("sync enabled");
    }

    public void run(final String[] arguments) {
        try {
	    pln("running reflectively");
	    runReflectively(arguments);
	} catch (LinkageException e) {
	    throw e.unchecked();
	}
    }


    private ReflectiveArray arr_char
	= new ReflectiveArray(this, TypeName.CHAR);
    
    static private class UDCharArrayBuffer extends UnicodeBuffer.UTF16Buffer {
	Oop carr;

	// the carr is a char array, which is later passed to
	
	//   String(char[] data, int offset, int count, boolean dont_copy)
	
	// as the "data" argument 
	//
	// "offset" is set to 0
	// "count" is set to charCount, which is "end - start"
	// "don_copy" is set to true
	//
	// !!! it doesn't make any sense to me, but it seems that the array
	// is passed to the String constructor without the 

	protected char getAbsoluteChar(int index) {
//	    return VM_Address.fromObject(carr).add(2 * index).getChar();
	    return MemoryManager.the().getCharArrayElement( carr, index );
	}
	protected void setAbsoluteChar(int index, char value) {
//	    VM_Address.fromObject(carr).add(2 * index).setChar(value);
//	    MemoryManager.the().setPrimitiveArrayElementAtByteOffset(carr, 2*index, value);
	    MemoryManager.the().setPrimitiveArrayElement(carr, index, value);
	}

	public UnicodeBuffer slice(int start, int end) {
	    return new UDCharArrayBuffer(carr, start, end);
	}
	
	private UDCharArrayBuffer(Oop carr, int start, int end) {
	    super(start, end);
	    this.carr = carr;
	}
	
	UDCharArrayBuffer(Oop carr, Blueprint.Array bp,
			  int offset, int count) {
/*
    	      this(carr,
		 bp.byteOffset(offset)/2,
		 bp.byteOffset(offset+count)/2);
*/		 
    	      this(carr, offset, offset+count);
//    	      printIt();
	}

	UDCharArrayBuffer(ReflectiveArray arr_char, UnicodeBuffer b) {
	
	    super(0, b.charCount());	
/*	
	    super(arr_char.bp().byteOffset(0)/2,
		  arr_char.bp().byteOffset(b.charCount())/2);
*/		  
	    carr = arr_char.make(end);
	    copy(b);
//	    printIt();
	    
	}
	
	private void printIt() {
		Native.print_string("\nthe string is: \n");
		for(int i=start ; i<end ; i++) {
			Native.print_char( getAbsoluteChar(i) );
		}
		Native.print_string("\nthe string ends here \n");		
	}
    }

    private ReflectiveField.Integer string_offset =
	new ReflectiveField.Integer(this,
				    JavaNames.java_lang_String, "offset");
				    
    private ReflectiveField.Integer string_count =
	new ReflectiveField.Integer(this,
				    JavaNames.java_lang_String, "count");
				    
    private ReflectiveField.Reference string_value =
	new ReflectiveField.Reference(this, JavaNames.arr_char,
				      JavaNames.java_lang_String, "value");
    
    public UnicodeBuffer getString(Oop dString) {
	return new UDCharArrayBuffer(string_value.get(dString),
				     arr_char.bp(),
				     string_offset.get(dString),
				     string_count.get(dString));
    }

    private ReflectiveConstructor makeString = 
	new ReflectiveConstructor(this, JavaNames.java_lang_String,
				  new TypeName[] {
				      JavaNames.arr_char,
				      TypeName.INT,
				      TypeName.INT,
				      TypeName.BOOLEAN
				  });

    /**
     * FIXME: This method leaks a UDCharArrayBuffer object into the
     * current memory area.  The code could easily be restructured to
     * prevent this leak, but we are allocating quite a bit anyway.
     *
     * This method allocates an InstantiationMessage in the scratchpad
     **/
    public Oop makeString(UnicodeBuffer contents) {
	UDCharArrayBuffer copy = new UDCharArrayBuffer(arr_char, contents);
	VM_Area outer = MemoryManager.the().getCurrentArea();
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
	try {
	    InstantiationMessage msg = makeString.makeMessage();
	    msg.getInArgAt(0).setOop(copy.carr);
	    msg.getInArgAt(1).setInt(0);
	    msg.getInArgAt(2).setInt(copy.charCount());
	    msg.getInArgAt(3).setBoolean(true);
	    VM_Area r2 = MemoryManager.the().setCurrentArea(outer);
	    try {
		ReturnMessage ret = msg.instantiate();
		ret.rethrowWildcard();
		Oop toReturn = ret.getReturnValue().getOop();
		
		if (false) {
			Native.print_string("in makeString, created user domain string: \"");
			Native.print_ustring_at_address( VM_Address.fromObject(toReturn) );
			Native.print_string("\"\n");
		}
		return toReturn;
		
	    } finally {
		MemoryManager.the().setCurrentArea(r2);
	    }
	} catch (LinkageException e) {
	    throw Executive.panicOnException(e);
	} finally {
	    MemoryPolicy.the().leave(r1);
	}
    }

    private ReflectiveMethod string_getBytes
	= new ReflectiveMethod(this, JavaNames.arr_byte,
			       JavaNames.java_lang_String, "getBytes",
			       new TypeName[] { });
    /**
     * This method potentially peforms a whole lot of allocation.
     * Calling String.getBytes() will certainly allocate a
     * user-domain array, but who knows what, exactly, it will do.
     *
     * Unfortunately, the user domain byte[] is one byte too short,
     * so we must copy it to the executive domain, rather than
     * restamping it.
     *
     * This method allocates an InvocationMessage in the scratchpad 
     **/
     
     // with arraylets, returns a contiguous array (because the c strings
     // are then used for native calls)
    public byte[] getLocalizedCString(Oop dString) {
	Oop uArray = string_getBytes.call(dString).getOop();
	Blueprint.Array ubp = uArray.getBlueprint().asArray();
	int len = ubp.getLength(uArray);
	byte[] ret = null;
	if (MemoryManager.the().usesArraylets()) {
		ret = MemoryManager.the().allocateContinuousByteArray(len + 1);
	} else {
		ret = new byte[len + 1];
        }
	Oop retOop = VM_Address.fromObject(ret).asOop();
	Blueprint.Array ebp = retOop.getBlueprint().asArray();
	MemoryManager.the().copyArrayElements(uArray, 0, retOop, 0, len);
	
	if (false) {
		Native.print_string("In getLocalizedCString, created string: ");
		Native.print_bytearr_len( ret, len );
		Native.print_string("\n from user domain string ");
		Native.print_ustring_at_address( VM_Address.fromObject(dString) );
		Native.print_string("\n");
	}
	return ret;
    }

    public Type.Context makeTypeContext() {
	return new S3TypeContext(this);
    }

    private final ReflectiveVirtualFunction loadClassVF =
	new ReflectiveVirtualFunction(this,
				      JavaNames.java_lang_ClassLoader_loadClass);

    public Type.Loader makeTypeLoader(final Oop classLoader) {
	Type.Scalar t = classLoader.getBlueprint().getType().asScalar();
	Method m;
	final ReflectiveMethod loadClass = loadClassVF.dispatch(classLoader);
	return new Type.Loader() {
		public Oop getMirror() { return classLoader; }
		public Type loadType(TypeName.Scalar name)
		    throws LinkageException
		{
		    UnicodeBuffer dotted = JavaFormat._.formatUnicode(name);
		    try {
			ValueUnion rv = loadClass.call(classLoader,
						       internString(dotted));
			if (rv.getOop() == null)
			    throw new LinkageException.NoClassDef(name);
			return myRTE.class2type(rv.getOop());
		    } catch (WildcardException w) {
			Oop ex = w.getUserThrowable();
			Type ext = ex.getBlueprint().getType();
			JavaTypes jt = commonTypes();
			if (ext.isSubtypeOf(jt.java_lang_Error)
			    || ext.isSubtypeOf(jt.java_lang_RuntimeException))
			    throw new LinkageException.User(ex);
			else
			    throw new LinkageException.NoClassDef(name);
		    }

		}
	    };
    }

    S3JavaUserDomain(TypeName.Scalar mainClassName, 
		     String systemResourcePath,
		     String userResourcePath) {
	super(systemResourcePath);
	appContext_ = new S3TypeContext(this);
	classPath = userResourcePath;
	mainClassName = mainClassName.getGemeinsamTypeName();
	main = new ReflectiveMethod(appContext_,
				    Selector.Method.make(JavaNames.MAIN,
							 mainClassName));
	throwableCode =
	    new ReflectiveField.Reference(this,
					  JavaNames.org_ovmj_java_Opaque,
					  JavaNames.java_lang_VMThrowable,
					  "code");
	throwablePC =
	    new ReflectiveField.Reference(this,
					  JavaNames.org_ovmj_java_Opaque,
					  JavaNames.java_lang_VMThrowable,
					  "pc");

	// If -classpath is not given a build time, we should not load
	// anything from the app context, and we should allow
	// -classpath or -Djava.class.path to be given at runtime.
	if (classPath != null)
	    RuntimeExports.defineVMProperty("java.class.path", classPath);

	RuntimeExports.defineVMProperty("org.ovmj.boot.class.path",
					bootClassPath);
	// for compatibility with sun
	RuntimeExports.defineVMProperty("sun.boot.class.path",
					bootClassPath);
	// We dump all classpath resource files into ovm_rt_user.jar.
	// Why not?  The bootstrap classloader implements getResource
	// (and everyting else) in terms of URLClassLoader, so we
	// might as well store ResourceBundles and whatnot in a .jar.
	// (The whatnot is stored in /lib.)
	//
	// See syslib/user/ovm_classpath/Makefile.mk for the horror.
	String rt_user_jar = InvisibleStitcher.getString("ovm_rt_user.jar");
	RuntimeExports.defineVMProperty("gnu.classpath.home.url",
					"jar:file:" + rt_user_jar + "!/lib");

	RuntimeExports.defineVMProperty("java.home",
					InvisibleStitcher.getString("ovm-home"));
	/* The constructor is used at runtime but allocated at
	 *  boottime. A better place for this code would be nice. The
	 *  conditional avoids allocating the constructor.
	 */
	if (Transaction.the().transactionalMode())
	    Transaction.the().setExceptionConstructors(this);
     }

    public void freezeClassLoading(boolean permanently) {
	super.freezeClassLoading(permanently);
	appContext_.freeze(permanently);
    }

    public Type.Context getApplicationTypeContext() {
	return appContext_;
    }

    public String getClassPath() {
	return classPath;
    }

    public Type.Context getExtensionsTypeContext() {
        return null;
    }

    // XXX: getSystemTypeContext() returns the boot Type.Context.  In
    // the ED, the two are identical, but in the UD, we really want
    // appContext_ (which maps to the default system Type.Context).
    public Type.Context getBootTypeContext() {
	return context_;
    }
    public void thaw() {
        super.thaw();
        appContext_.thaw();
	S3DomainDirectory dir =
	    (S3DomainDirectory) DomainDirectory.the();
	Object r = MemoryPolicy.the().enterMetaDataArea(this);
	try {
	    if (dir.reflectiveMethodTrace != null)
		invocationDumper = new Dumper(dir.reflectiveMethodTrace);
	    if (dir.reflectiveClassTrace != null)
		forNameDumper = new Dumper(dir.reflectiveClassTrace);
	} finally {
	    MemoryPolicy.the().leave(r);
	}
    }


    // Reflection Helper for the threading system

    /** convient empty array for no-arg methods */
    protected static final TypeName[] NO_ARGS = new TypeName[0];

    /**
     * ReflectiveMethod object for the java.lang.Thread runThread method
     */
    public final ReflectiveMethod thread_runThread
	= new ReflectiveMethod(this, 
                               TypeName.VOID,
			       JavaNames.java_lang_VMThreadBase, 
                               "run",
			       NO_ARGS);

    public final ReflectiveMethod thread_startupPriority
	= new ReflectiveMethod(this,
			       TypeName.Primitive.INT,
			       JavaNames.java_lang_VMThreadBase,
			       "getStartupPriority",
			       NO_ARGS);

    public final  ReflectiveField.Boolean interrupted =
	new ReflectiveField.Boolean(this,
				    JavaNames.java_lang_VMThreadBase,
				    "interrupted");

}
