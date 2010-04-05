package s3.services.simplejit;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.ExecutiveDomain;
import ovm.core.domain.Field;
import ovm.core.domain.JavaDomain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Type;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.JVMConstants.Throwables;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import s3.core.domain.S3ExecutiveDomain;
import s3.core.domain.S3Field;
import s3.core.domain.S3MemberResolver;
import s3.services.bytecode.ovmify.NativeCallGenerator;
import s3.services.simplejit.CompilerVMInterface.ObjectLayout;

public class SimpleJITCompilerVMInterface 
	implements CompilerVMInterface, TypeCodes, Throwables {
    /**
     * The list of runtime C functions which are to be called by
     * compiled native code
     **/
    public static final String[] runtimeFunctionSymbols =
	new String[] {
	    "printf",
	    "fflush_out",
	    "is_subtype_of",
	    "ldiv",
	    "lrem",
	    "fmod",
	    "l2f",
	    "l2d",
	    "f2l",
	    "d2l",
	    "f2i",
	    "d2i",
	    "f2d",
	    "fcmpg",
	    "SYSTEM_GET_CONTEXT",
	    "SYSTEM_NEW_CONTEXT",
	    "SYSTEM_GET_ACTIVATION",
	    "memmove",
	    "SYSTEM_MAKE_ACTIVATION",
	    "SYSTEM_INVOKE",
	    "SYSTEM_START_TRACING",
	    "SYSTEM_STOP_TRACING",
	    "SYSTEM_DESTROY_NATIVE_CONTEXT",
	    "SYSTEM_RUN",		
	    "eventPollcheck",
	    "check_stack_overflow",
	    "invoke_native",
	    "hit_unrewritten_code",
	    "eventUnion",
	    "memDebugHook",
	    "currentContext",
	    "CAS32",
	};

    private S3ExecutiveDomain executiveDomain;
    private S3Domain targetDomain;
    private boolean positionIndependentCode;
    private boolean movingGC;
    private boolean dynamicClassLoading;
    private VM_Address jitHeader;
    private UnboundSelector.Method[] csaMethods;
    private VM_Address runtimeFunctionTableHandle;

    /**
     * @param executiveDomain
     * @param targetDomain the domain to which the method to be
     * compiled belong
     * @param jitHeader the address of the JIT header object
     **/
    public SimpleJITCompilerVMInterface
	(S3Domain executiveDomain,
	 S3Domain targetDomain,
	 UnboundSelector.Method[] csaMethods,
	 VM_Address jitHeader) {
	this.executiveDomain = (S3ExecutiveDomain)executiveDomain;
	this.targetDomain = targetDomain;
	this.csaMethods = csaMethods;
	this.positionIndependentCode = true;
	this.movingGC = true;
	this.dynamicClassLoading = (executiveDomain != targetDomain);
	this.jitHeader = jitHeader;
    }

    public JavaDomain getTargetDomain() {
	return targetDomain;
    }

    public ExecutiveDomain getExecutiveDomain() {
	return executiveDomain;
    }

    public boolean positionIndependentCode(Domain domain) { return true; }
    public boolean movingGC(Domain domain) { return true; }
    public boolean dynamicClassLoading(Domain domain) { return false; }

    public UnboundSelector.Method[] getNativeMethodList() {
	return NativeCallGenerator.getNativeArgumentMap();
    }

    public int getCSAMethodIndex(UnboundSelector.Method method) {
	// FIXME: use hashtable
	for (int i = 0; i < csaMethods.length; i++) {
	    if (method.equals(csaMethods[i]))
		return i;
	}
	return -1;
    }

    // FIXME maybe some of these VM_Addresses should be Oops. Remember
    // VM_Addresses are invisible to the garbage collector!
    public VM_Address getRuntimeFunctionTableHandle() {
	if (runtimeFunctionTableHandle == null) {
	    Type.Scalar ts =
		jitHeader.asOop().getBlueprint().getType().asScalar();
	    Selector.Field bs =
		RepositoryUtils.selectorFor(ts.getName(),
					    "runtimeFunctionTableHandle",
					    JavaNames.ovm_core_services_memory_VM_Address);
	     S3Field f = (S3Field)ts.getField( bs);// FIXME: replace with ?? S3Field f = (S3Field)ts.getField( bs.getUnboundSelector());
	    runtimeFunctionTableHandle = f.bug370addressWithin(jitHeader.asOop());
	}
	return runtimeFunctionTableHandle;
    }

    public int getRuntimeFunctionIndex(String symbol) {
	// FIXME use hashtable
	for (int i = 0; i < runtimeFunctionSymbols.length; i++) {
	    if (symbol.equals(runtimeFunctionSymbols[i]))
		return i;
	}
	return -1;
    }

    public ObjectLayout getObjectLayout(Domain domain) {
	return new SimpleJITObjectLayout(domain);
    }

    private class SimpleJITObjectLayout implements ObjectLayout {
	private S3Domain domain;
	private SimpleJITObjectLayout(Domain domain) {
	    this.domain = (S3Domain)domain;
	}
	public int getFieldOffset(TypeName.Scalar classname,
				  UnboundSelector.Field field) {
	    try {
		Type.Class tc =
		    (Type.Class)domain
		    .getApplicationTypeContext().typeFor(classname);
		Field fld = S3MemberResolver.resolveField(tc, field, ((Type.Compound) null));
		return ((S3Field)fld).getOffset();
	    } catch (LinkageException le) {
		throw le.fatal();
	    }
	}

	public int getFieldSize(char typeTag) {
	    if (typeTag == OBJECT) {
		return s3.core.domain.MachineSizes.BYTES_IN_ADDRESS;
	    } else {
		try {
		    TypeName.Primitive ptn = TypeName.Primitive.make(typeTag);
		    Type pt = domain.getApplicationTypeContext().typeFor(ptn);
		    Blueprint.Primitive pbp =
			(Blueprint.Primitive)domain.blueprintFor(pt);
		    return pbp.getUnpaddedFixedSize();
		} catch (LinkageException le) {
		    throw le.fatal();
		}
	    }
	}

	public int getArrayHeaderSize(char typeTag) {
	    return getArrayBpt(typeTag).getUnpaddedFixedSize();
	}

	public int getArrayElementSize(char typeTag) {
	    return getArrayBpt(typeTag).getComponentSize();
	}

	public int getArrayLengthFieldOffset() {
	    S3Blueprint.Array bp = getArrayBpt(OBJECT);
	    return bp.lengthFieldOffset();
	}

	private S3Blueprint.Array getArrayBpt(char typeTag) {
	    try {
		if (typeTag == OBJECT) {
		    TypeName.Compound atn =
			TypeName.Array.make(JavaNames.java_lang_Object, 1);
		    return (S3Blueprint.Array) 
			domain.blueprintFor
			(atn, domain.getApplicationTypeContext());
		} else {
		    TypeName.Primitive tn = TypeName.Primitive.make(typeTag);
		    TypeName.Compound atn = TypeName.Array.make(tn, 1);
		    return (S3Blueprint.Array) 
			domain.blueprintFor
			(atn, domain.getApplicationTypeContext());
		}
	    } catch (LinkageException le) {
		throw le.fatal();
	    }
	}

	public int getArrayLengthFieldSize() {
	    S3Blueprint.Array bp = getArrayBpt(OBJECT);
	    return bp.lengthFieldSize();
	}
    }

    // This has to be synchronized with JVMConstants.Throwables
    public int getExceptionID(Domain domain,
			      Class c) {
	if (c == NullPointerException.class)
	    return NULL_POINTER_EXCEPTION;
	if (c == ArrayIndexOutOfBoundsException.class)
	    return ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION;
	if (c == ClassCastException.class)
	    return CLASS_CAST_EXCEPTION;
	if (c == StackOverflowError.class)
	    return STACK_OVERFLOW_ERROR;
	if (c == ArrayStoreException.class)
	    return ARRAY_STORE_EXCEPTION;
	if (c == ArithmeticException.class)
	    return ARITHMETIC_EXCEPTION;
	throw new Error("RuntimeException " + c + " not supported");
    }

    public byte[] getGetBlueprintCode_X86(byte objReg,
				      byte bpReg) {
	return ObjectModel.getObjectModel().x86_getBlueprint(objReg, bpReg);
    }
    public int[] getGetBlueprintCode_PPC(int bpReg, int objReg) {
        return ObjectModel.getObjectModel().ppc_getBlueprint(bpReg, objReg);
    }

}
