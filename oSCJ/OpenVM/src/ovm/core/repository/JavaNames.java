package ovm.core.repository;
import java.lang.reflect.Field;

import ovm.core.OVMBase;
import ovm.services.bytecode.JVMConstants;
import s3.services.transactions.Transaction;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;

/**
 * Directory of useful repository constants, Java specific.
 * FIXME Better name and location (language specific ...)
 **/
public final class JavaNames extends OVMBase {

    private static int asUTF(String name) {
	return RepositoryUtils.asUTF(name);
    }
    private static TypeName.Scalar makeName(int pkg, String cls) {
	return TypeName.Scalar.make(pkg, asUTF(cls));
    }
    static final int JAVA_LANG = asUTF("java/lang");
    static final int JAVA_IO = asUTF("java/io");
    static final int GNU_CLASSPATH = asUTF("gnu/classpath");
    static final int JAVAX_REALTIME = asUTF("javax/realtime");
    static final int JAVA_LANG_REFLECT = asUTF("java/lang/reflect");
    static final int OVM_CORE_SERVICES_MEMORY = asUTF("ovm/core/services/memory");
    static final int OVM_CORE_EXECUTION = asUTF("ovm/core/execution");
    static final int OVM_CORE_DOMAIN = asUTF("ovm/core/domain");
    static final int OVM_UTIL = asUTF("ovm/util");
    static final int OVM_HW = asUTF("org/ovmj/hw");
    static final int S3_CORE_DOMAIN = asUTF("s3/core/domain");

    private static final int INIT = asUTF("<init>");
//PARBEGIN
    private static final int INIT_T = asUTF("<init>" +   Transaction.TRANS_SUFFIX);
    public static final int[] INIT_NAMES = {INIT, INIT_T};
    public static final UnboundSelector.Method PAR_READ = (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector("par_read:()V");
    public static final UnboundSelector.Method PAR_LOG = (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector("par_log:(Lovm/core/services/memory/VM_Address;I)V");
    public static final UnboundSelector.Method PAR_LOGW = (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector("par_logw:(Lovm/core/services/memory/VM_Address;I)V");
    public static final UnboundSelector.Method PAR_LOG_ARR = (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector("par_log_arr:(Lovm/core/domain/Oop;I)V");
    public static final UnboundSelector.Method PAR_LOG_ARRW = (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector("par_log_arrw:(Lovm/core/domain/Oop;I)V");
//PAREND
    public static final TypeName.Scalar java_lang_Object = makeName(JAVA_LANG, "Object");
    public static final TypeName.Scalar java_lang_String = makeName(JAVA_LANG, "String");  
    public static final TypeName.Scalar java_lang_StringBuffer = makeName(JAVA_LANG, "StringBuffer");
    public static final TypeName.Scalar java_lang_Class = makeName(JAVA_LANG, "Class");
    public static  final TypeName.Array arr_java_lang_Class = TypeName.Array.make(java_lang_Class, 1);
    public static final TypeName.Scalar java_lang_VMClass = makeName(JAVA_LANG, "VMClass");
    public static final TypeName.Scalar java_lang_System =  makeName(JAVA_LANG, "System");
    public static final TypeName.Scalar java_lang_ClassLoader = makeName(JAVA_LANG, "ClassLoader");
    public static final TypeName.Scalar java_lang_Thread = makeName(JAVA_LANG, "Thread");
    public static final TypeName.Scalar java_lang_VMThreadBase =
	makeName(JAVA_LANG, "VMThreadBase");
    public static final TypeName.Scalar java_lang_VMThread =
	makeName(JAVA_LANG, "VMThread");
    public static final TypeName.Scalar java_lang_VMRealtimeThread =
	makeName(JAVA_LANG, "VMRealtimeThread");
    public static final TypeName.Scalar java_lang_Cloneable = makeName(JAVA_LANG, "Cloneable");
    // The LinkageError hierarchy is defined here, in its entirety
    public static final TypeName.Scalar java_lang_LinkageError = makeName(JAVA_LANG, "LinkageError");
    public static final TypeName.Scalar java_lang_NoClassDefFoundError = makeName(JAVA_LANG, "NoClassDefFoundError");
    public static final TypeName.Scalar java_lang_ClassFormatError = makeName(JAVA_LANG, "ClassFormatError");
    public static final TypeName.Scalar java_lang_UnsupportedClassVersionError = makeName(JAVA_LANG, "UnsupportedClassVersionError");
    public static final TypeName.Scalar java_lang_ClassCircularityError = makeName(JAVA_LANG, "ClassCircularityError");
    public static final TypeName.Scalar java_lang_VerifyError = makeName(JAVA_LANG, "VerifyError");
    public static final TypeName.Scalar java_lang_ExceptionInInitializerError = makeName(JAVA_LANG, "ExceptionInInitializerError");
    public static final TypeName.Scalar java_lang_IncompatibleClassChangeError = makeName(JAVA_LANG, "IncompatibleClassChangeError");
    public static final TypeName.Scalar java_lang_AbstractMethodError = makeName(JAVA_LANG, "AbstractMethodError");
    public static final TypeName.Scalar java_lang_InstantiationError = makeName(JAVA_LANG, "InstantiationError");
    public static final TypeName.Scalar java_lang_NoSuchMethodError = makeName(JAVA_LANG, "NoSuchMethodError");
    public static final TypeName.Scalar java_lang_NoSuchFieldError = makeName(JAVA_LANG, "NoSuchFieldError");
    public static final TypeName.Scalar java_lang_IllegalAccessError = makeName(JAVA_LANG, "IllegalAccessError");
    public static final TypeName.Scalar java_lang_UnsatisfiedLinkError = makeName(JAVA_LANG, "UnsatisfiedLinkError");
    public static final TypeName.Scalar java_lang_CloneNotSupportedException = makeName(JAVA_LANG, "CloneNotSupportedException");
    public static final TypeName.Scalar java_io_InputStream = makeName(JAVA_IO, "InputStream");
    public static final TypeName.Scalar java_io_PrintStream = makeName(JAVA_IO, "PrintStream");
    public static final TypeName.Scalar java_io_Serializable = makeName(JAVA_IO, "Serializable");
    public static final TypeName.Scalar java_lang_Integer = makeName(JAVA_LANG, "Integer");
    public static final TypeName.Scalar java_lang_Character = makeName(JAVA_LANG, "Character");
    public static final TypeName.Scalar java_lang_Short = makeName(JAVA_LANG, "Short");
    public static final TypeName.Scalar java_lang_Boolean = makeName(JAVA_LANG, "Boolean");
    public static final TypeName.Scalar java_lang_Byte = makeName(JAVA_LANG, "Byte");
    public static final TypeName.Scalar java_lang_Float = makeName(JAVA_LANG, "Float");
    public static final TypeName.Scalar java_lang_Double = makeName(JAVA_LANG, "Double");
    public static final TypeName.Scalar java_lang_Long = makeName(JAVA_LANG, "Long");

    public static final TypeName.Scalar gnu_classpath_VMStackWalker = makeName(GNU_CLASSPATH, "VMStackWalker");

    // Exceptions thrown directly from native code  
    // - see JVMConstants.Throwables

    public static final TypeName.Scalar java_lang_NullPointerException = makeName(JAVA_LANG, "NullPointerException");
    public static final TypeName.Scalar java_lang_ArrayIndexOutOfBoundsException = makeName(JAVA_LANG, "ArrayIndexOutOfBoundsException");
    public static final TypeName.Scalar java_lang_NegativeArraySizeException = makeName(JAVA_LANG, "NegativeArraySizeException");
    public static final TypeName.Scalar java_lang_StackOverflowError = makeName(JAVA_LANG, "StackOverflowError");
    public static final TypeName.Scalar java_lang_ArithmeticException = makeName(JAVA_LANG, "ArithmeticException");
    public static final TypeName.Scalar java_lang_ClassCastException = makeName(JAVA_LANG, "ClassCastException");
    public static final TypeName.Scalar java_lang_OutOfMemoryError = makeName(JAVA_LANG, "OutOfMemoryError");
    public static final TypeName.Scalar java_lang_InternalError = makeName(JAVA_LANG, "InternalError");
    public static final TypeName.Scalar java_lang_IllegalArgumentException = makeName(JAVA_LANG, "IllegalArgumentException");
    public static final TypeName.Scalar java_lang_Throwable = makeName(JAVA_LANG, "Throwable");
    public static final TypeName.Scalar java_lang_Error = makeName(JAVA_LANG, "Error");
    public static final TypeName.Scalar java_lang_RuntimeException = makeName(JAVA_LANG, "RuntimeException");
    public static final TypeName.Scalar javax_realtime_IllegalAssignmentError = makeName(JAVAX_REALTIME, "IllegalAssignmentError");
    public static final TypeName.Scalar javax_realtime_MemoryAccessError = makeName(JAVAX_REALTIME, "MemoryAccessError");
    public static final TypeName.Scalar javax_realtime_MemoryArea = makeName(JAVAX_REALTIME, "MemoryArea");
    public static final TypeName.Scalar javax_realtime_RealtimeThread = makeName(JAVAX_REALTIME, "RealtimeThread");

    
    // exceptions thrown from kernel services

    public static final TypeName.Scalar java_lang_ArrayStoreException = makeName(JAVA_LANG, "ArrayStoreException");
    public static final TypeName.Scalar java_lang_VirtualMachineError = makeName(JAVA_LANG, "VirtualMachineError");
    public static final TypeName.Scalar java_lang_IllegalMonitorStateException = makeName(JAVA_LANG, "IllegalMonitorStateException");
    public static final TypeName.Scalar ovm_core_domain_Type = makeName(OVM_CORE_DOMAIN, "Type");
    public static final TypeName.Scalar ovm_core_domain_Code = makeName(OVM_CORE_DOMAIN, "Code");
    public static final TypeName.Array arr_ovm_core_domain_Code = TypeName.Array.make(ovm_core_domain_Code, 1);
    public static final TypeName.Scalar ovm_core_domain_Oop = makeName(OVM_CORE_DOMAIN, "Oop");
    public static final TypeName.Array arr_Oop = TypeName.Array.make(ovm_core_domain_Oop, 1);
    public static final TypeName.Scalar ovm_core_domain_WildcardException = 	makeName(OVM_CORE_DOMAIN, "WildcardException");
    public static final TypeName.Scalar ovm_core_services_memory_VM_Address = makeName(OVM_CORE_SERVICES_MEMORY, "VM_Address");
    public static final TypeName.Scalar ovm_core_services_memory_VM_Area_Destructor = makeName(OVM_CORE_SERVICES_MEMORY, "VM_Area$Destructor");
    public static final TypeName.Scalar ovm_core_services_memory_VM_Word = makeName(OVM_CORE_SERVICES_MEMORY, "VM_Word");
    public static final TypeName.Scalar ovm_core_execution_CoreServicesAccess = makeName(OVM_CORE_EXECUTION, "CoreServicesAccess");
    public static final TypeName.Scalar ovm_core_execution_Context = makeName(OVM_CORE_EXECUTION, "Context");
    public static final TypeName.Scalar ovm_core_execution_Engine = makeName(OVM_CORE_EXECUTION, "Engine");

    public static final TypeName.Scalar ovm_util_UnsafeAccess = makeName(OVM_UTIL, "UnsafeAccess");
    
    public static final TypeName.Scalar ovm_hw_PragmaNoHWIORegistersAccess = makeName(OVM_HW, "PragmaNoHWIORegistersAccess");
    public static final TypeName.Scalar ovm_hw_HardwareObject = makeName(OVM_HW, "HardwareObject");    
    public static final TypeName.Scalar ovm_hw_InterruptHandler = makeName(OVM_HW, "InterruptHandler");    
    
    public static final TypeName.Scalar s3_core_domain_S3ByteCode = makeName(S3_CORE_DOMAIN, "S3ByteCode");
    /**
     * Names of throwables indexed by JVMConstants.Throwables.  This
     * array lists all exception types that can implicitly thrown by
     * bytecode via the generateThrowable CSA call
     **/
    public static final TypeName.Scalar[] throwables = new TypeName.Scalar[JVMConstants.Throwables.N_THROWABLES];

    // map constants defined in JVMConstants.Throwables, such as
    // NULL_POINTER_EXCEPTION and IO_EXCEPTION to constants defined in
    // JavaNames, such as java_lang_NullPointerException and
    // java_io_IOException.  We can't simply replace underscores with
    // StudlyCaps, so we look for exactly one field in JavaNames where
    // everything after the last underscore in its name
    // case-insensitively matches the Throwables field without
    // underscores.
    //
    // the THROWABLE_NAMES array is used in CoreServicesAccess and 
    // J2cImageCompiler.  By filling it in reflectively, we force
    // JavaNames and JVMConstants.Throwables to remain in sync.
    static { init(); }
    static private void init() throws BCbootTime {
	try {
	    Field[] thrField = JVMConstants.Throwables.class.getFields();
	    Field[] jnField = JavaNames.class.getFields();
	    StringBuffer b = new StringBuffer();

	    for (int i = 0; i < thrField.length; i++) {
		String thrName = thrField[i].getName();
		b.setLength(0);
		if (thrName.equals("NO_THROWABLE")
		    || thrName.equals("N_THROWABLES"))
		    continue;
		for (int j = 0; j < thrName.length(); j++) {
		    char c = thrName.charAt(j);
		    if (c != '_')
			b.append(c);
		}
		String expected = b.toString();
		Field result = null;
		for (int j = 0; j < jnField.length; j++) {
		    String found = jnField[j].getName();
		    found = found.substring(found.lastIndexOf('_') + 1,
					    found.length());
		    if (expected.equalsIgnoreCase(found)) {
			if (result != null)
			    throw new RuntimeException
				("throwable name " + thrField[i] + 
				 " ambiguous: matches " + result +
				 " and " + jnField[j]);
			result = jnField[j];
		    }
		}
		if (result == null)
		    throw new RuntimeException("throwable name "
					       + thrField[i]
					       + " not found");
		throwables[thrField[i].getInt(null)]
		    = (TypeName.Scalar) result.get(null);
	    }
	} catch (IllegalAccessException e) {
	    throw new RuntimeException("impossible");
	}
    }

    // primitive array types
    public static final TypeName.Array arr_boolean =
        TypeName.Array.make(TypeName.BOOLEAN, 1);

    public static final TypeName.Array arr_byte =
        TypeName.Array.make(TypeName.BYTE, 1);

    public static final TypeName.Array arr_short =
        TypeName.Array.make(TypeName.SHORT, 1);

    public static final TypeName.Array arr_char =
        TypeName.Array.make(TypeName.CHAR, 1);

    public static final TypeName.Array arr_int =
        TypeName.Array.make(TypeName.INT, 1);

    public static final TypeName.Array arr_long =
        TypeName.Array.make(TypeName.LONG, 1);

    public static final TypeName.Array arr_float =
        TypeName.Array.make(TypeName.FLOAT, 1);

    public static final TypeName.Array arr_double =
        TypeName.Array.make(TypeName.DOUBLE, 1);



    // misc types
    public static final TypeName.Scalar java_lang_StackTraceElement =
        makeName(JAVA_LANG, "StackTraceElement");

    public static final TypeName.Scalar java_lang_VMThrowable =
        makeName(JAVA_LANG, "VMThrowable");

    public static final TypeName.Array arr_String =
        TypeName.Array.make(JavaNames.java_lang_String, 1);

    public static final TypeName.Scalar java_lang_reflect_Constructor =
        makeName(JAVA_LANG_REFLECT, "Constructor");

    public static final TypeName.Scalar java_lang_reflect_Field =
        makeName(JAVA_LANG_REFLECT, "Field");

    public static final TypeName.Scalar java_lang_reflect_Method =
        makeName(JAVA_LANG_REFLECT, "Method");

    public static final TypeName.Scalar java_lang_reflect_InvocationTargetException =
        makeName(JAVA_LANG_REFLECT, "InvocationTargetException");

    public static final TypeName.Scalar org_ovmj_java_Opaque =
        makeName(asUTF("org/ovmj/java"), "Opaque");

    // misc selectors

    public static final UnboundSelector.Method MAIN =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "main:([Ljava/lang/String;)V");

    public static final UnboundSelector.Method CLINIT =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "<clinit>:()V");
    public static final UnboundSelector.Method BOOT_ =
	(UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
	    "boot_:()V");
    public static final UnboundSelector.Method INIT_V =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "<init>:()V");
    public static final UnboundSelector.Method INIT_WITH_STRING =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "<init>:(Ljava/lang/String;)V");
    public static final UnboundSelector.Method FINALIZE =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "finalize:()V");
    public static final Selector.Method java_lang_Object_finalize =
	Selector.Method.make(FINALIZE, java_lang_Object);

    public static final UnboundSelector.Method LOAD_CLASS =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "loadClass:(Ljava/lang/String;)Ljava/lang/Class;");
    public static final Selector.Method java_lang_ClassLoader_loadClass =
	Selector.Method.make(LOAD_CLASS, java_lang_ClassLoader);

    public static final UnboundSelector.Method GET_CALLING_CLASS =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "getCallingClass:()Ljava/lang/Class;");

    public static final UnboundSelector.Method GET_CALLING_CLASS_LOADER =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "getCallingClassLoader:()Ljava/lang/ClassLoader;");

    public static final UnboundSelector.Method VMClass_GET_CLASS_LOADER =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "getClassLoader:(Ljava/lang/Class;)Ljava/lang/ClassLoader;");

    // FIXME: Find out how many of these selectors are being used, and
    // whether they should be used.
    public static final Selector.Method java_lang_Object_toString =
        RepositoryUtils.selectorFor( java_lang_Object,
                                     java_lang_String,
                                     "toString",
                                     TypeName.EMPTY_ARRAY);

    public static final UnboundSelector.Method REGISTER_FINALIZER =
        (UnboundSelector.Method) RepositoryUtils.makeUnboundSelector(
            "registerFinalizer:(Lovm/core/domain/Oop;)V");

    public static final Selector.Method
        java_lang_StringBuffer_STRING_Constructor =
        RepositoryUtils.selectorFor(java_lang_StringBuffer,
                                    TypeName.VOID,
                                    "<init>",
                                    new TypeName[] { java_lang_String });
    
    public static final Selector.Method
        java_lang_StringBuffer_STRING_append =
        RepositoryUtils.selectorFor(java_lang_StringBuffer,
                                    java_lang_StringBuffer,
                                    "append",
                                    new TypeName[] { java_lang_String });
    
    public static final Selector.Method java_lang_String_PLAIN_Constructor =
	RepositoryUtils.selectorFor(java_lang_String,
				    TypeName.VOID,
				    "<init>",
				    new TypeName[] { arr_char });

    public static final Selector.Method
	java_lang_String_NOCOPY_byte__Constructor =
	RepositoryUtils.selectorFor(java_lang_String,
				    TypeName.VOID,
				    "<init>",
				    new TypeName[] {
					arr_char,
					TypeName.INT,
					TypeName.INT,
					TypeName.BOOLEAN
				    });

    public static final Selector.Method java_lang_String_BYTES_Constructor =
	RepositoryUtils.selectorFor(java_lang_String,
				    TypeName.VOID,
				    "<init>",
				    new TypeName[] { arr_byte });


    public static final Selector.Method java_lang_String_intern =
	RepositoryUtils.selectorFor(java_lang_String,
				    java_lang_String,
				    "intern",
				    TypeName.EMPTY_ARRAY);	


    public static final Selector.Method java_lang_String_getBytes =
        RepositoryUtils.selectorFor(java_lang_String,
                                    arr_byte,
                                    "getBytes",
                                    TypeName.EMPTY_ARRAY);

    public static final Selector.Method java_lang_String_toCharArray =
        RepositoryUtils.selectorFor(java_lang_String,
                                    arr_char,
                                    "toCharArray",
                                    TypeName.EMPTY_ARRAY);
    /**
     * gnu-classpath specific: the underlying char[] for this string.
     **/
    public static final Selector.Field java_lang_String_value =
	RepositoryUtils.selectorFor(java_lang_String, "value", arr_char);

    /**
     * gnu-classpath specific: the starting offset of this string's contents
     * in the char array
     **/
    public static final Selector.Field java_lang_String_offset =
	RepositoryUtils.selectorFor(java_lang_String, "offset", TypeName.INT);

    /**
     * gnu-classpath/ovm specific: the length of this string in chars/bytes.
     **/
    public static final Selector.Field java_lang_String_count =
	RepositoryUtils.selectorFor(java_lang_String, "count", TypeName.INT);

    
    public static final Selector.Method java_lang_String_INIT =
        RepositoryUtils.selectorFor(
            java_lang_String,
            TypeName.VOID,
            "<init>",
            new TypeName[] { arr_byte });

    /**
     * ovm-specific: the underlying byte[] for this string's value.
     **/
    public static final Selector.Field java_lang_String_data =
        RepositoryUtils.selectorFor(java_lang_String, "data", arr_byte);

    public static final Selector.Method VMThread_getInitialArea =
        RepositoryUtils.selectorFor(java_lang_VMThread,
				    javax_realtime_MemoryArea,
				    "getInitialArea",
				    new TypeName[0]);
    public static final Selector.Method VMThread_finalizeThread =
        RepositoryUtils.selectorFor(java_lang_VMThread,
				    TypeName.VOID,
				    "finalizeThread",
				    new TypeName[0]);
}