package s3.services.simplejit;

import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.repository.Attribute;
import ovm.core.repository.Descriptor;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.repository.Attribute.LineNumberTable;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.InstructionSet;
import ovm.services.bytecode.JVMConstants;
import ovm.util.ByteBuffer;
import ovm.util.OVMError;
import ovm.util.logging.Logger;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Constants;
import s3.core.domain.S3Method;
import ovm.util.ByteBuffer.BufferOverflowException;
import ovm.core.services.io.BasicIO;
import ovm.core.domain.Code;
import ovm.core.execution.NativeConstants;

/**
 * A compilation task on a per-method basis
 *
 * @author Hiroshi Yamauchi
 * @author Christian Grothoff
 **/
public abstract class CodeGenerator 
    extends Instruction.IVisitor 
    implements JVMConstants, TypeCodes {

    // Precompute the domain-specific values which need be computed
    // only once for a domain
    public static class Precomputed {
	CompilerVMInterface compilerVMInterface;

	/*
	 * Precomputed field offsets
	 */
	final public int offset_vtbl_in_bp;
	final public int offset_nvtbl_in_bp;
	final public int offset_code_in_cf;
	final public int offset_iftbl_in_bp;
	final public int offset_cp_in_cf;
	final public int offset_csa_in_cf;
	final public int offset_name_in_cf;
	final public int offset_values_in_cp;
	final public int offset_componentbp_in_arraybp;
	final public int offset_argumentLength_in_SimpleJITCode;
	final public int offset_bp_in_instancemref;
	final public int offset_nonvirtual_in_instancemref;
	final public int offset_shst_in_staticmref;
	final public int offset_shst_in_staticfref;
	final public int offset_mtb_in_nc;
	final public int offset_pso_in_nc;
	final public int offset_redzone_in_mtb;
	final public int offset_lastsucc_in_bp;
	final public int offset_lastfail_in_bp;
	
	final public VM_Address runtimeFunctionTableHandle;
	
	/**
	 * Precomputed CSA method indexes
	 **/
	final public int csa_allocateObject_index;
	final public Descriptor.Method csa_allocateObject_desc;
	final public int csa_allocateArray_index;
	final public Descriptor.Method csa_allocateArray_desc;
	final public int csa_allocateMultiArray_index;
	final public Descriptor.Method csa_allocateMultiArray_desc;
	final public int csa_generateThrowable_index;
	final public Descriptor.Method csa_generateThrowable_desc;
	final public int csa_processThrowable_index;
	final public Descriptor.Method csa_processThrowable_desc;
	final public int csa_monitorEnter_index;
	final public Descriptor.Method csa_monitorEnter_desc;
	final public int csa_monitorExit_index;
	final public Descriptor.Method csa_monitorExit_desc;
	final public int csa_emptyCall_index;
	final public Descriptor.Method csa_emptyCall_desc;
	final public int csa_initializeBlueprint_index;
	final public Descriptor.Method csa_initializeBlueprint_desc;
	final public int csa_pollingEventHook_index;
	final public Descriptor.Method csa_pollingEventHook_desc;
	final public int csa_resolveLdc_index;
	final public Descriptor.Method csa_resolveLdc_desc;
	final public int csa_resolveNew_index;
	final public Descriptor.Method csa_resolveNew_desc;
	final public int csa_aastoreBarrier_index;
	final public Descriptor.Method csa_aastoreBarrier_desc;
	final public int csa_putFieldBarrier_index;
	final public Descriptor.Method csa_putFieldBarrier_desc;
	final public int csa_readBarrier_index;
	final public Descriptor.Method csa_readBarrier_desc;
	final public int csa_resolveInstanceField_index;
	final public Descriptor.Method csa_resolveInstanceField_desc;
	final public int csa_resolveInstanceMethod_index;
	final public Descriptor.Method csa_resolveInstanceMethod_desc;
	final public int csa_resolveStaticField_index;
	final public Descriptor.Method csa_resolveStaticField_desc;
	final public int csa_resolveStaticMethod_index;
	final public Descriptor.Method csa_resolveStaticMethod_desc;
	final public int csa_resolveInterfaceMethod_index;
	final public Descriptor.Method csa_resolveInterfaceMethod_desc;
	final public int csa_resolveClass_index;
	final public Descriptor.Method csa_resolveClass_desc;

	
	final public 
	    CompilerVMInterface.ObjectLayout eObjectLayout;
	final public
	    CompilerVMInterface.ObjectLayout tObjectLayout;
	// executive domain object layout
	final public int eByteArrayHeaderSize;
    
	final public int eByteArrayElementSize;
	final public int eIntArrayHeaderSize;
	final public int eIntArrayElementSize;
	final public int eShortArrayHeaderSize;
	final public int eShortArrayElementSize;
	final public int eCharArrayHeaderSize;
	final public int eCharArrayElementSize;
	final public int eLongArrayHeaderSize;
	final public int eLongArrayElementSize;
	final public int eFloatArrayHeaderSize;
	final public int eFloatArrayElementSize;
	final public int eDoubleArrayHeaderSize;
	final public int eDoubleArrayElementSize;
    
	final public int eObjectArrayHeaderSize;
	final public int eObjectArrayElementSize;
	final public int eArrayLengthFieldSize;
	final public int eArrayLengthFieldOffset;
	// target domain object layout
	final public int tByteArrayHeaderSize;
	final public int tByteArrayElementSize;
	final public int tIntArrayHeaderSize;
	final public int tIntArrayElementSize;
	final public int tShortArrayHeaderSize;
	final public int tShortArrayElementSize;
	final public int tCharArrayHeaderSize;
	final public int tCharArrayElementSize;
	final public int tLongArrayHeaderSize;
	final public int tLongArrayElementSize;
	final public int tFloatArrayHeaderSize;
	final public int tFloatArrayElementSize;
	final public int tDoubleArrayHeaderSize;
	final public int tDoubleArrayElementSize;
	final public int tObjectArrayHeaderSize;
	final public int tObjectArrayElementSize;
	final public int tArrayLengthFieldSize;
	final public int tArrayLengthFieldOffset;

	final public int nullPointerExceptionID;
	final public int classCastExceptionID;
	final public int arrayIndexOutOfBoundsExceptionID;
	final public int stackOverflowErrorID;
	final public int arrayStoreExceptionID;
	final public int arithmeticExceptionID;

	final public boolean isExecutive;

	public Precomputed(CompilerVMInterface compilerVMInterface) {
	    this.compilerVMInterface = compilerVMInterface;
	    Domain executiveDomain = compilerVMInterface.getExecutiveDomain();
	    Domain targetDomain = compilerVMInterface.getTargetDomain(); 
	    isExecutive = (executiveDomain == targetDomain);
	    eObjectLayout = compilerVMInterface.getObjectLayout(executiveDomain);
	    tObjectLayout = compilerVMInterface.getObjectLayout(targetDomain);

	    offset_cp_in_cf = 
		getExeFieldOffset("s3/services/simplejit",
				  "SimpleJITCode",
				  "constants_",
				  "Lovm/core/repository/Constants;");
	    offset_name_in_cf = 
		getExeFieldOffset("s3/services/simplejit",
				  "SimpleJITCode",
				  "methodname_",
				  "[B");	
	    offset_csa_in_cf = 
		getExeFieldOffset("s3/services/simplejit",
				  "SimpleJITCode",
				  "myCSA_",
				  "Lovm/core/execution/CoreServicesAccess;");
	    offset_values_in_cp = 
		getExeFieldOffset("s3/core/domain",
				  "S3Constants",
				  "constants",
				  "[Ljava/lang/Object;");
	    offset_vtbl_in_bp = 
		getExeFieldOffset("s3/core/domain",
				  "S3Blueprint",
				  "vTable",
				  "[Lovm/core/domain/Code;");
	    offset_nvtbl_in_bp = 
		getExeFieldOffset("s3/core/domain",
				  "S3Blueprint",
				  "nvTable",
				  "[Lovm/core/domain/Code;");
	    offset_iftbl_in_bp = 
		getExeFieldOffset("s3/core/domain",
				  "S3Blueprint",
				  "ifTable",
				  "[Lovm/core/domain/Code;");
	    offset_code_in_cf = 
		getExeFieldOffset("ovm/core/domain",
				  "Code",
				  "foreignEntry",
				  "Lovm/core/services/memory/VM_Address;");
	    offset_componentbp_in_arraybp = 
		getExeFieldOffset("s3/core/domain",
				  "S3Blueprint$Array",
				  "componentBlueprint_",
				  "Ls3/core/domain/S3Blueprint;");
	    
	    offset_argumentLength_in_SimpleJITCode = 
		getExeFieldOffset("s3/services/simplejit",
				  "SimpleJITCode",
				  "argumentLength_",
				  "I");

	    offset_bp_in_instancemref =
		getExeFieldOffset("ovm/core/domain",
				  "ConstantResolvedInstanceMethodref",
				  "staticDefinerBlueprint",
				  "Lovm/core/domain/Blueprint;");

	    offset_nonvirtual_in_instancemref =
		getExeFieldOffset("ovm/core/domain",
				  "ConstantResolvedInstanceMethodref",
				  "isNonVirtual",
				  "Z");

	    offset_shst_in_staticmref =
		getExeFieldOffset("ovm/core/domain",
				  "ConstantResolvedStaticMethodref",
				  "sharedState",
				  "Lovm/core/domain/Oop;");

	    offset_shst_in_staticfref =
		getExeFieldOffset("ovm/core/domain",
				  "ConstantResolvedStaticFieldref",
				  "sharedState",
				  "Lovm/core/domain/Oop;");

	    offset_mtb_in_nc =
		getExeFieldOffset("s3/services/simplejit",
				  "NativeContext",
				  "mtb",
				  "Ls3/services/simplejit/MThreadBlock;");

	    offset_pso_in_nc =
		getExeFieldOffset("s3/services/simplejit",
				  "NativeContext",
				  "processing_stack_overflow",
				  "I");

	    offset_redzone_in_mtb =
		getExeFieldOffset("s3/services/simplejit",
				  "MThreadBlock",
				  "redzone",
				  "I");

	    offset_lastsucc_in_bp =
		getExeFieldOffset("s3/core/domain",
				  "S3Blueprint",
				  "lastsucc",
				  "Lovm/core/services/memory/VM_Address;");

	    offset_lastfail_in_bp =
		getExeFieldOffset("s3/core/domain",
				  "S3Blueprint",
				  "lastfail",
				  "Lovm/core/services/memory/VM_Address;");

	    final String str_csa_allocateObject_name = "allocateObject";
	    final String str_csa_allocateObject_desc = 
		"(Lovm/core/domain/Blueprint$Scalar;)Lovm/core/domain/Oop;";
	    final String str_csa_allocateArray_name = "allocateArray";
	    final String str_csa_allocateArray_desc = 
		"(Lovm/core/domain/Blueprint$Array;I)Lovm/core/domain/Oop;";
	    final String str_csa_allocateMultiArray_name = "_allocateMultiArray";
	    final String str_csa_allocateMultiArray_desc = 
		"(Lovm/core/domain/Blueprint$Array;Lovm/core/services/memory/VM_Address;I)Lovm/core/domain/Oop;";
	    final String str_csa_generateThrowable_name = "generateThrowable";
	    final String str_csa_generateThrowable_desc = 
		"(II)V";
	    final String str_processThrowable_name = "processThrowable";
	    final String str_processThrowable_desc = 
		"(Lovm/core/domain/Oop;)Ljava/lang/Error;";
	    final String str_emptyCall_name = "emptyCall";
	    final String str_emptyCall_desc = 
		"()V";
	    final String str_monitorEnter_name = "monitorEnter";
	    final String str_monitorEnter_desc = 
		"(Lovm/core/domain/Oop;)V";
	    final String str_monitorExit_name = "monitorExit";
	    final String str_monitorExit_desc = 
		"(Lovm/core/domain/Oop;)V";
	    final String str_initializeBlueprint_name = "initializeBlueprint";
	    final String str_initializeBlueprint_desc = 
		"(Lovm/core/domain/Oop;)Lovm/core/domain/Oop;";
	    final String str_pollingEventHook_name = "pollingEventHook";
	    final String str_pollingEventHook_desc = 
		"()V";
	    final String str_resolveLdc_name = "resolveLdc";
	    final String str_resolveLdc_desc = 
		"(ILovm/core/repository/Constants;)V";
	    final String str_resolveNew_name = "resolveNew";
	    final String str_resolveNew_desc = 
		"(ILovm/core/repository/Constants;)V";
	    final String str_aastoreBarrier_name = "aastoreBarrier";
	    final String str_aastoreBarrier_desc = 
		"(Lovm/core/domain/Oop;ILovm/core/domain/Oop;)V";
	    final String str_readBarrier_name = "readBarrier";
	    final String str_readBarrier_desc = 
		"(Lovm/core/domain/Oop;)V";
	    final String str_putFieldBarrier_name = "putFieldBarrier";
	    final String str_putFieldBarrier_desc = 
		"(Lovm/core/domain/Oop;ILovm/core/domain/Oop;)V";
	    final String str_resolveInstanceField_name = "resolveInstanceField";
	    final String str_resolveInstanceField_desc = 
		"(ILovm/core/repository/Constants;)I";
	    final String str_resolveInstanceMethod_name = "resolveInstanceMethod";
	    final String str_resolveInstanceMethod_desc = 
		"(ILovm/core/repository/Constants;)I";
	    final String str_resolveStaticField_name = "resolveStaticField";
	    final String str_resolveStaticField_desc = 
		"(ILovm/core/repository/Constants;)I";
	    final String str_resolveStaticMethod_name = "resolveStaticMethod";
	    final String str_resolveStaticMethod_desc = 
		"(ILovm/core/repository/Constants;)I";
	    final String str_resolveInterfaceMethod_name = "resolveInterfaceMethod";
	    final String str_resolveInterfaceMethod_desc = 
		"(ILovm/core/repository/Constants;)I";
	    final String str_resolveClass_name = "resolveClass";
	    final String str_resolveClass_desc = 
		"(ILovm/core/repository/Constants;)V";
	    
	    csa_allocateObject_index = 
		getCSAMethodIndex(str_csa_allocateObject_name,
				  str_csa_allocateObject_desc);
	    csa_allocateObject_desc = 
		getCSAMethodDescriptor(str_csa_allocateObject_name,
				       str_csa_allocateObject_desc);
	    csa_allocateArray_index = 
		getCSAMethodIndex(str_csa_allocateArray_name,
				  str_csa_allocateArray_desc);
	    csa_allocateArray_desc =
		getCSAMethodDescriptor(str_csa_allocateArray_name,
				       str_csa_allocateArray_desc);
	    csa_allocateMultiArray_index =
		getCSAMethodIndex(str_csa_allocateMultiArray_name,
				  str_csa_allocateMultiArray_desc);
	    csa_allocateMultiArray_desc = 
		getCSAMethodDescriptor(str_csa_allocateMultiArray_name,
				       str_csa_allocateMultiArray_desc);
	    csa_generateThrowable_index =
		getCSAMethodIndex(str_csa_generateThrowable_name,
				  str_csa_generateThrowable_desc);
	    csa_generateThrowable_desc = 
		getCSAMethodDescriptor(str_csa_generateThrowable_name,
				       str_csa_generateThrowable_desc);
	    csa_processThrowable_index =
		getCSAMethodIndex(str_processThrowable_name,
				  str_processThrowable_desc);
	    csa_processThrowable_desc =
		getCSAMethodDescriptor(str_processThrowable_name,
				       str_processThrowable_desc);
	    csa_emptyCall_index = getCSAMethodIndex(str_emptyCall_name, 
						    str_emptyCall_desc);
	    csa_emptyCall_desc = getCSAMethodDescriptor(str_emptyCall_name, 
							str_emptyCall_desc);
	    csa_monitorEnter_index = 
		getCSAMethodIndex(str_monitorEnter_name,
				  str_monitorEnter_desc);
	    csa_monitorEnter_desc =
		getCSAMethodDescriptor(str_monitorEnter_name,
				       str_monitorEnter_desc);
	    csa_monitorExit_index = 
		getCSAMethodIndex(str_monitorExit_name,
				  str_monitorExit_desc);
	    csa_monitorExit_desc =
		getCSAMethodDescriptor(str_monitorExit_name,
				       str_monitorExit_desc);
	    csa_initializeBlueprint_index = 
		getCSAMethodIndex(str_initializeBlueprint_name,
				  str_initializeBlueprint_desc);
	    csa_initializeBlueprint_desc =
		getCSAMethodDescriptor(str_initializeBlueprint_name,
				       str_initializeBlueprint_desc);

	    csa_pollingEventHook_index = 
		getCSAMethodIndex(str_pollingEventHook_name,
				  str_pollingEventHook_desc);
	    csa_pollingEventHook_desc =
		getCSAMethodDescriptor(str_pollingEventHook_name,
				       str_pollingEventHook_desc);
	    csa_resolveLdc_index =
		getCSAMethodIndex(str_resolveLdc_name,
				  str_resolveLdc_desc);
	    csa_resolveLdc_desc = 
		getCSAMethodDescriptor(str_resolveLdc_name,
				       str_resolveLdc_desc);
	    csa_resolveNew_index =
		getCSAMethodIndex(str_resolveNew_name,
				  str_resolveNew_desc);
	    csa_resolveNew_desc =
		getCSAMethodDescriptor(str_resolveNew_name,
				       str_resolveNew_desc);

	    csa_aastoreBarrier_index =
		getCSAMethodIndex(str_aastoreBarrier_name,
				  str_aastoreBarrier_desc);
	    csa_aastoreBarrier_desc =
		getCSAMethodDescriptor(str_aastoreBarrier_name,
				       str_aastoreBarrier_desc);

	    csa_readBarrier_index =
			getCSAMethodIndex(str_readBarrier_name,
					  str_readBarrier_desc);
		    csa_readBarrier_desc =
			getCSAMethodDescriptor(str_readBarrier_name,
					       str_readBarrier_desc);

	    csa_putFieldBarrier_index =
		getCSAMethodIndex(str_putFieldBarrier_name,
				  str_putFieldBarrier_desc);
	    csa_putFieldBarrier_desc =
		getCSAMethodDescriptor(str_putFieldBarrier_name,
				       str_putFieldBarrier_desc);

	    csa_resolveInstanceField_index =
		getCSAMethodIndex(str_resolveInstanceField_name,
				  str_resolveInstanceField_desc);
	    csa_resolveInstanceField_desc =
		getCSAMethodDescriptor(str_resolveInstanceField_name,
				       str_resolveInstanceField_desc);

	    csa_resolveInstanceMethod_index =
		getCSAMethodIndex(str_resolveInstanceMethod_name,
				  str_resolveInstanceMethod_desc);
	    csa_resolveInstanceMethod_desc =
		getCSAMethodDescriptor(str_resolveInstanceMethod_name,
				       str_resolveInstanceMethod_desc);

	    csa_resolveStaticField_index =
		getCSAMethodIndex(str_resolveStaticField_name,
				  str_resolveStaticField_desc);
	    csa_resolveStaticField_desc =
		getCSAMethodDescriptor(str_resolveStaticField_name,
				       str_resolveStaticField_desc);

	    csa_resolveStaticMethod_index =
		getCSAMethodIndex(str_resolveStaticMethod_name,
				  str_resolveStaticMethod_desc);
	    csa_resolveStaticMethod_desc =
		getCSAMethodDescriptor(str_resolveStaticMethod_name,
				       str_resolveStaticMethod_desc);
	    csa_resolveInterfaceMethod_index =
		getCSAMethodIndex(str_resolveInterfaceMethod_name,
				  str_resolveInterfaceMethod_desc);
	    csa_resolveInterfaceMethod_desc =
		getCSAMethodDescriptor(str_resolveInterfaceMethod_name,
				       str_resolveInterfaceMethod_desc);

	    csa_resolveClass_index =
		getCSAMethodIndex(str_resolveClass_name,
				  str_resolveClass_desc);
	    csa_resolveClass_desc =
		getCSAMethodDescriptor(str_resolveClass_name,
				       str_resolveClass_desc);
		    
	    runtimeFunctionTableHandle = 
		compilerVMInterface.getRuntimeFunctionTableHandle();

	    eByteArrayHeaderSize = eObjectLayout.getArrayHeaderSize(BYTE);
	    eByteArrayElementSize = eObjectLayout.getArrayElementSize(BYTE);
	    eShortArrayHeaderSize = eObjectLayout.getArrayHeaderSize(SHORT);
	    eShortArrayElementSize = eObjectLayout.getArrayElementSize(SHORT);
	    eCharArrayHeaderSize = eObjectLayout.getArrayHeaderSize(CHAR);
	    eCharArrayElementSize = eObjectLayout.getArrayElementSize(CHAR);
	    eIntArrayHeaderSize = eObjectLayout.getArrayHeaderSize(INT);
	    eIntArrayElementSize = eObjectLayout.getArrayElementSize(INT);
	    eLongArrayHeaderSize = eObjectLayout.getArrayHeaderSize(LONG);
	    eLongArrayElementSize = eObjectLayout.getArrayElementSize(LONG);
	    eFloatArrayHeaderSize = eObjectLayout.getArrayHeaderSize(FLOAT);
	    eFloatArrayElementSize = eObjectLayout.getArrayElementSize(FLOAT);
	    eDoubleArrayHeaderSize = eObjectLayout.getArrayHeaderSize(DOUBLE);
	    eDoubleArrayElementSize = eObjectLayout.getArrayElementSize(DOUBLE);
	    eObjectArrayHeaderSize = eObjectLayout.getArrayHeaderSize(OBJECT);
	    eObjectArrayElementSize = eObjectLayout.getArrayElementSize(OBJECT);
	    eArrayLengthFieldOffset = eObjectLayout.getArrayLengthFieldOffset();
	    eArrayLengthFieldSize = eObjectLayout.getArrayLengthFieldSize();

	    tByteArrayHeaderSize = tObjectLayout.getArrayHeaderSize(BYTE);
	    tByteArrayElementSize = tObjectLayout.getArrayElementSize(BYTE);
	    tShortArrayHeaderSize = tObjectLayout.getArrayHeaderSize(SHORT);
	    tShortArrayElementSize = tObjectLayout.getArrayElementSize(SHORT);
	    tCharArrayHeaderSize = tObjectLayout.getArrayHeaderSize(CHAR);
	    tCharArrayElementSize = tObjectLayout.getArrayElementSize(CHAR);
	    tIntArrayHeaderSize = tObjectLayout.getArrayHeaderSize(INT);
	    tIntArrayElementSize = tObjectLayout.getArrayElementSize(INT);
	    tLongArrayHeaderSize = tObjectLayout.getArrayHeaderSize(LONG);
	    tLongArrayElementSize = tObjectLayout.getArrayElementSize(LONG);
	    tFloatArrayHeaderSize = tObjectLayout.getArrayHeaderSize(FLOAT);
	    tFloatArrayElementSize = tObjectLayout.getArrayElementSize(FLOAT);
	    tDoubleArrayHeaderSize = tObjectLayout.getArrayHeaderSize(DOUBLE);
	    tDoubleArrayElementSize = tObjectLayout.getArrayElementSize(DOUBLE);
	    tObjectArrayHeaderSize = tObjectLayout.getArrayHeaderSize(OBJECT);
	    tObjectArrayElementSize = tObjectLayout.getArrayElementSize(OBJECT);
	    tArrayLengthFieldOffset = tObjectLayout.getArrayLengthFieldOffset();
	    tArrayLengthFieldSize = tObjectLayout.getArrayLengthFieldSize();

	    nullPointerExceptionID = 
		compilerVMInterface
		.getExceptionID(targetDomain, NullPointerException.class);
	    classCastExceptionID =
		compilerVMInterface
		.getExceptionID(targetDomain, ClassCastException.class);
	    arrayIndexOutOfBoundsExceptionID =
		compilerVMInterface
		.getExceptionID(targetDomain, 
				ArrayIndexOutOfBoundsException.class);
	    stackOverflowErrorID =
		compilerVMInterface
		.getExceptionID(targetDomain, StackOverflowError.class);
	    arrayStoreExceptionID =
		compilerVMInterface
		.getExceptionID(targetDomain, ArrayStoreException.class);
	    arithmeticExceptionID =
		compilerVMInterface
		.getExceptionID(targetDomain, ArithmeticException.class);
	}

	public int getExeFieldOffset(String packagename, 
				      String classname,
				      String fieldname,
				      String fieldsig) {
	    TypeName.Scalar tn 
		= RepositoryUtils.makeTypeName(packagename, 
					       classname);
	    UnboundSelector.Field usel = 
		(UnboundSelector.Field)RepositoryUtils.makeUnboundSelector
		(fieldname + ':' + fieldsig);
	    return eObjectLayout.getFieldOffset(tn, usel);
	}

	public int getCSAMethodIndex(String methodname,
				      String methodsig) {
	    UnboundSelector.Method usel = 
		(UnboundSelector.Method)RepositoryUtils.makeUnboundSelector
		(methodname + ":" + methodsig);
	    int mindex = compilerVMInterface.getCSAMethodIndex(usel);
	    if (mindex == -1)
		throw new OVMError.Internal("CSA method " + usel + " not found");
	    return mindex;
	}
	public Descriptor.Method
	    getCSAMethodDescriptor(String methodname,
				   String methodsig) {
	    UnboundSelector.Method usel = 
		(UnboundSelector.Method)RepositoryUtils.makeUnboundSelector
		(methodname + ":" + methodsig);
	    return usel.getDescriptor();
	}

    }

    // Flags

    /**
     * If true, a breakpoint is inserted in the code positions
     * corresponding to the start of each bytecode instruction.
     */
    protected final static boolean ENABLE_BYTECODE_MARKING = false;
    /*
     * The flags for runtime checks
     */
    protected final static boolean OMIT_NULLPOINTER_CHECKS =
	NativeConstants.OVM_X86 && NativeConstants.LINUX_BUILD;
    protected final static boolean OMIT_NULLPOINTER_CHECKS_MONITOR = false;
    protected final static boolean OMIT_ARRAYBOUND_CHECKS = false;
    protected final static boolean OMIT_ARRAYSTORE_CHECKS = false;
    protected final static boolean OMIT_STACKOVERFLOW_CHECKS = false;
    protected final static boolean OMIT_DIVISION_BY_ZERO_CHECKS = false;
    protected final static boolean OMIT_POLLCHECKS = false;
    
    /**
     * Whether or not to inline "eventPollcheck" (hard-coded)
     */
    protected final static boolean OPTIMIZE_POLLCHECK = true;

    /**
     * Whether or not to inline "stack_overflow_check" (hard-coded)
     */
    protected final static boolean OPTIMIZE_STACK_OVERFLOW_CHECK = true;

    /**
     * Whether or not to inline "setEnabled" and "eventsSetEnabled" (hard-coded)
     */
    protected final static boolean INLINE_SOME_IN_INVOKENATIVE = true;

    /**
     * Whether or not to assume ED objects such native code, shared
     * states, etc don't move. In other words, whether the addresses
     * of these ED objects can be cached in the machine code stream
     * as an optimization.
     */
    protected final static boolean ED_OBJECT_DONT_MOVE = true;

    protected final static boolean AOT_RESOLUTION_UD = false;

    /**
     * Whether or not to devirtualize CSA calls. This is valid only
     * when ED_OBJECT_DONT_MOVE == true. If this option is true, the
     * native code object of CSA methods are cached in the machine
     * code stream.
     */
    protected final static boolean DEVIRTUALIZE_CSA = true;

    protected static final boolean DO_MEM_DEBUG = false;

    /**
     * If true, SimpleJIT prints warnings about unresolvable
     * references in ED .
     */
    protected static final boolean WARN = false;

    /**
     * A flag used to print the message that the warnings are turned
     * off in SimpleJIT when WARN = false.
     */
    protected static boolean NO_WARNING_WARNING_PRINTED = false;


    protected final S3Constants cp;
    final protected StackLayout stackLayout;
    final protected int offset_code_in_stack;
    final protected Precomputed precomputed;
    protected CodeGenContext codeGenContext;
    protected final InstructionSet is_;
    protected final S3ByteCode bytecode;
    protected final ExceptionHandler[] exceptionHandlers;
    protected final CompilerVMInterface compilerVMInterface;
    protected boolean debugPrintOn;
    protected final boolean isSynchronized;
    protected static int counter = 0;
    protected S3Method method;
    protected S3Blueprint blueprint;
    protected Domain targetDomain;
    protected Domain executiveDomain;

    /**
     * Create a compilation task for a method.
     **/
    public CodeGenerator(S3Method method,
			 InstructionSet is,
			 CompilerVMInterface compilerVMInterface,
			 boolean debugPrintOn,
			 Precomputed precomputed) {
        super(InstructionBuffer.wrap(method.getByteCode()));
        this.method = method;
        this.compilerVMInterface = compilerVMInterface;
        this.is_ = is;
        this.bytecode = method.getByteCode();
        this.exceptionHandlers = bytecode.getExceptionHandlers();
        this.isSynchronized = method.getMode().isSynchronized();
        this.debugPrintOn = debugPrintOn;
	this.targetDomain = compilerVMInterface.getTargetDomain();
	this.executiveDomain = compilerVMInterface.getExecutiveDomain();
	this.cp = (S3Constants)bytecode.getConstantPool();
	this.precomputed = precomputed;
	this.stackLayout = makeStackLayout(bytecode.getMaxLocals(),
					   bytecode.getMaxStack(),
					   method.getArgumentLength() / 4 + 1);
	this.offset_code_in_stack = stackLayout.getCodeFragmentOffset();
	this.blueprint = 
		(S3Blueprint)targetDomain
		.blueprintFor(method.getDeclaringType());
	if (! WARN && ! NO_WARNING_WARNING_PRINTED) {
	    NO_WARNING_WARNING_PRINTED = true;
	    Logger.global.warning("Warnings turned off in SimpleJIT");
	}
    }

    protected void warn(Instruction i, String s) {
	if (! WARN)
	    return;
        String message =
            "[SimpleJIT] Compiling " + i.getName() + ": " + s + " in " + method.getSelector();
        Logger.global.warning(message);
    }

    protected abstract StackLayout makeStackLayout(int maxLocals,
						   int maxStack,
						   int argLength);

    protected abstract CodeGenContext makeCodeGenContext();

    protected abstract void prepareAssembler(CodeGenContext context);

    public void compile(VM_Area compileArea) {
	while (true) {
	    if (compileArea != null) {
    		VM_Area prev = MemoryManager.the().setCurrentArea(compileArea);
    		try {
		    this.codeGenContext = makeCodeGenContext();
    		} finally {
		    MemoryManager.the().setCurrentArea(prev);
    		}
	    } else {
    		this.codeGenContext = makeCodeGenContext();
	    }
	    try {
		prepareAssembler(this.codeGenContext);
		generatePrologue();
		generateBody();
		generateEpilogue();
		break;
	    } catch (BufferOverflowException e) {
		codeGenContext.expandGlobalCodeArray(method.getSelector());
	    }
	}
	if (compileArea != null) {
	    VM_Area prev1 = MemoryManager.the().setCurrentArea(compileArea);
	    try {
		makeNativeCode();
	    } finally {
		MemoryManager.the().setCurrentArea(prev1);
	    }
	} else {
	    makeNativeCode();
	}
    }

    protected int getArrayElementOffset(Domain domain, char typeTag, int index) {
	int headerSize = compilerVMInterface
	    .getObjectLayout(domain).getArrayHeaderSize(typeTag);
	int elementSize = compilerVMInterface
	    .getObjectLayout(domain).getArrayElementSize(typeTag);
	return headerSize + index * elementSize;
    }

    private Attribute.LineNumberTable makeLineNumberTable() {
	Attribute[] att = bytecode.getAttributes();
	LineNumberTable bct = null;
	for (int i = 0; i < att.length; i++)
	    if (att[i] instanceof LineNumberTable) {
		bct = (LineNumberTable) att[i];
		break;
	    }
	if (bct == null)
	    return null;
	int[] bc_startPCTable = bct.getStartPCTable();
	int[] native_startPCTable = new int[bc_startPCTable.length];

	for(int i = 0; i < bc_startPCTable.length; i++) {
	    native_startPCTable[i] = 
		codeGenContext.getBytecodePC2NativePC(bc_startPCTable[i]);
	}
	int[] lineNumberTable = bct.getLineNumberTable();
	int[] copy_lineNumberTable = (int[])lineNumberTable.clone();
	return Attribute.LineNumberTable.make(native_startPCTable,
					      copy_lineNumberTable);
    }

    protected void makeNativeCode() {
	Code nc = new SimpleJITCode(method,
				    codeGenContext.getCodeEntry(),
				    codeGenContext.getNativeExceptionHandlers(bytecode),
				    getConstantPool(),
				    stackLayout.getMaxLocals(),
				    stackLayout.getMaxStack(),
				    stackLayout.getArgLength(),
				    codeGenContext.getBytecodePC2NativePC(),
				    makeLineNumberTable());
	method.addCode(nc);
    }

    protected abstract void generatePrologue();

    private String getShortInstructionName(Instruction i) {
        String org = i.toString();
        int dollarI = org.lastIndexOf('$');
        int atI = org.lastIndexOf('@');
	if (dollarI == -1 || atI == -1)
	    return "[" + org + "]";
	else
	    return "[" + org.substring(dollarI + 1, atI) + "]";
    }

    protected void generateBody() {
        Instruction[] set = is_.set;
        ByteBuffer _bytecode = this.getCode();
        _bytecode.rewind(); // set PC to 0
        if (debugPrintOn)
            debugPrint(
                "[Calling "
                    + getSelector().toString()
                    + " ##"
                    + counter
                    + "##]\n");
        while (_bytecode.remaining() > 0) {
            Instruction i = set[(_bytecode.get(getPC()) + 0x100) & 0xFF];
            codeGenContext.setBytecodePC2NativePC(
                getPC(),
                codeGenContext.getNativePC());
            if (debugPrintOn) {
		String name = getShortInstructionName(i);
		if (name.equals("[INVOKE_SYSTEM]") ||
		    name.equals("[INVOKE_NATIVE]")) {
		    name += " " + ((_bytecode.get(getPC() + 1) + 0x100) & 0xFF);
		}
                debugPrint("\t" + name + "\n");
	    }
	    beforeBytecode();
            this.visitAppropriate(i);
	    afterBytecode();
            _bytecode.advance(i.size(buf));
        }
        codeGenContext.linkRelativeJumps();
        counter++;
    }

    protected abstract void beforeBytecode();
    protected abstract void afterBytecode();

    protected abstract void generateEpilogue();

    protected abstract void debugPrint(String message);
} // end of CodeGenerator
