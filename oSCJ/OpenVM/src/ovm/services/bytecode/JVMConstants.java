/**
 * Constants for the JAVA VM.
 **/
package ovm.services.bytecode; 


/**
 * Constant occuring in Java class files.
 *
 * @author Christian Grothoff
 **/
public interface JVMConstants {

    int CLASSFILE_MAGIC = 0xCAFEBABE;

    /** All method tags must be negative **/

    int IS_FRAME_GETCURRENT       = 11;
    int IS_FRAME_GETPREVIOUS      = 12;
    int IS_FRAME_GET_PC_OFFSET    = 13;
    int IS_FRAME_RUNCODE          = 14;
    int IS_FRAME_POPALL           = 15;
    int IS_FRAME_PUSHOBJECT       = 16;
    int IS_FRAME_POPOBJECT        = 17;
    int IS_FRAME_GET_CF           = 18;



    // Access flags for classes, fields and methods.\

    int ACC_DEFAULT        = 0x0000;
    short ACC_PUBLIC       = 0x0001;
    short ACC_PRIVATE      = 0x0002;
    short ACC_PROTECTED    = 0x0004;
    short ACC_STATIC       = 0x0008;
    short ACC_FINAL        = 0x0010;
    short ACC_SYNCHRONIZED = 0x0020;
    short ACC_VOLATILE     = 0x0040;
    short ACC_TRANSIENT    = 0x0080;    
    short ACC_NATIVE       = 0x0100;
    short ACC_INTERFACE    = 0x0200;
    short ACC_ABSTRACT     = 0x0400;

    // Applies to classes compiled by new compilers only

    short ACC_SUPER        = 0x0020;
    short MAX_ACC_FLAG     = ACC_ABSTRACT;


    // A Utf8 constant pool enty.
    byte CONSTANT_Utf8               = 1;
    // An integer constant pool enty.
    byte CONSTANT_Integer            = 3;
    // A float constant pool enty.
    byte CONSTANT_Float              = 4;
    // A long constant pool enty.
    byte CONSTANT_Long               = 5;
    // A byte constant pool enty.
    byte CONSTANT_Double             = 6;
    // A class constant pool enty.
    byte CONSTANT_Class              = 7;
    // A fieldref constant pool enty.
    byte CONSTANT_Fieldref           = 9;
    // A string constant pool enty.
    byte CONSTANT_String             = 8;
    // A methodref constant pool enty.
    byte CONSTANT_Methodref          = 10;
    // An interface methodref constant pool entry.
    byte CONSTANT_InterfaceMethodref = 11;
    // A name and type constant pool enty.
    byte CONSTANT_NameAndType        = 12;
    // Utf8s that have not been installed in the repository.

    // RESOLVED  objects in the Cpool - another OVM invention!
    byte CONSTANT_Reference          = 13;

    byte CONSTANT_SharedState        = 14;
    // shared state in the Cpool - another OVM invention!

    byte CONSTANT_Binder             = 15;
    // invisible stitcher specified singletons - another OVM invention!

    // The resolved class. Holds a Blueprint.
    byte CONSTANT_ResolvedClass      = 16;

    // For the resolved version of INVOKESTATIC
    byte CONSTANT_ResolvedStaticMethod     = 17;
    // For the resolved version of INVOKESPECIAL and INVOKEVIRTUAL
    byte CONSTANT_ResolvedInstanceMethod    = 18;
    // For the resolved version of INVOKEINTERFACE
    byte CONSTANT_ResolvedInterfaceMethod  = 19;
    // For resolved versions of PUTSTATIC, GETSTATIC
    byte CONSTANT_ResolvedStaticField      = 20;
    // For resolved versions of GETFIELD, PUTFIELD
    byte CONSTANT_ResolvedInstanceField    = 21;
    
    public interface PrimitiveArrayTypes {
        byte T_BOOLEAN =  4;
        byte T_CHAR    =  5;
        byte T_FLOAT   =  6;
        byte T_DOUBLE  =  7;
        byte T_BYTE    =  8;
        byte T_SHORT   =  9;
        byte T_INT     = 10;
        byte T_LONG    = 11;
    }

    /**
     * Opcodes of the Instructions
     * @author Christian Grothoff
     **/
  public interface Opcodes {
	int NOP         =  0x00;
	int ACONST_NULL =  0x01;
	int ICONST_M1   =  0x02;
	int ICONST_0    =  0x03;
	int ICONST_1    =  0x04;
	int ICONST_2    =  0x05;
	int ICONST_3    =  0x06;
	int ICONST_4    =  0x07;
	int ICONST_5    =  0x08;
	int LCONST_0    =  0x09;
	int LCONST_1    =  0x0a;
	int FCONST_0    =  0x0b;
	int FCONST_1    =  0x0c;
	int FCONST_2    =  0x0d;
	int DCONST_0    =  0x0e;
	int DCONST_1    =  0x0f;
	int BIPUSH      =  0x10;
	int SIPUSH      =  0x11;
	int LDC         =  0x12;
	int LDC_W       =  0x13;
	int LDC2_W      =  0x14;
	int ILOAD       =  0x15;
	int LLOAD       =  0x16;
	int FLOAD       =  0x17;
	int DLOAD       =  0x18;
	int ALOAD       =  0x19;
	int ILOAD_0     =  0x1a;
	int ILOAD_1     =  0x1b;
	int ILOAD_2     =  0x1c;
	int ILOAD_3     =  0x1d;
	int LLOAD_0     =  0x1e;
	int LLOAD_1     =  0x1f;
	int LLOAD_2     =  0x20;
	int LLOAD_3     =  0x21;
	int FLOAD_0     =  0x22;
	int FLOAD_1     =  0x23;
	int FLOAD_2     =  0x24;
	int FLOAD_3     =  0x25;
	int DLOAD_0     =  0x26;
	int DLOAD_1     =  0x27;
	int DLOAD_2     =  0x28;
	int DLOAD_3     =  0x29;
	int ALOAD_0     =  0x2a;
	int ALOAD_1     =  0x2b;
	int ALOAD_2     =  0x2c;
	int ALOAD_3     =  0x2d;
	int IALOAD      =  0x2e;
	int LALOAD      =  0x2f;
	int FALOAD      =  0x30;
	int DALOAD      =  0x31;
	int AALOAD      =  0x32;
	int BALOAD      =  0x33;
	int CALOAD      =  0x34;
	int SALOAD      =  0x35;
	int ISTORE      =  0x36;
	int LSTORE      =  0x37;
	int FSTORE      =  0x38;
	int DSTORE      =  0x39;
	int ASTORE      =  0x3a;
	int ISTORE_0    =  0x3b;
	int ISTORE_1    =  0x3c;
	int ISTORE_2    =  0x3d;
	int ISTORE_3    =  0x3e;
	int LSTORE_0    =  0x3f;
	int LSTORE_1    =  0x40;
	int LSTORE_2    =  0x41;
	int LSTORE_3    =  0x42;
	int FSTORE_0    =  0x43;
	int FSTORE_1    =  0x44;
	int FSTORE_2    =  0x45;
	int FSTORE_3    =  0x46;
	int DSTORE_0    =  0x47;
	int DSTORE_1    =  0x48;
	int DSTORE_2    =  0x49;
	int DSTORE_3    =  0x4a;
	int ASTORE_0    =  0x4b;
	int ASTORE_1    =  0x4c;
	int ASTORE_2    =  0x4d;
	int ASTORE_3    =  0x4e;
	int IASTORE     =  0x4f;
	int LASTORE     =  0x50;
	int FASTORE     =  0x51;
	int DASTORE     =  0x52;
	int AASTORE     =  0x53;
	int BASTORE     =  0x54;
	int CASTORE     =  0x55;
	int SASTORE     =  0x56;
	int POP         =  0x57;
	int POP2        =  0x58;
	int DUP         =  0x59;
	int DUP_X1      =  0x5a;
	int DUP_X2      =  0x5b;
	int DUP2        =  0x5c;
	int DUP2_X1     =  0x5d;
	int DUP2_X2     =  0x5e;
	int SWAP        =  0x5f;
	int IADD        =  0x60;
	int LADD        =  0x61;
	int FADD        =  0x62;
	int DADD        =  0x63;
	int ISUB        =  0x64;
	int LSUB        =  0x65;
	int FSUB        =  0x66;
	int DSUB        =  0x67;
	int IMUL        =  0x68;
	int LMUL        =  0x69;
	int FMUL        =  0x6a;
	int DMUL        =  0x6b;
	int IDIV        =  0x6c;
	int LDIV        =  0x6d;
	int FDIV        =  0x6e;
	int DDIV        =  0x6f;
	int IREM        =  0x70;
	int LREM        =  0x71;
	int FREM        =  0x72;
	int DREM        =  0x73;
	int INEG        =  0x74;
	int LNEG        =  0x75;
	int FNEG        =  0x76;
	int DNEG        =  0x77;
	int ISHL        =  0x78;
	int LSHL        =  0x79;
	int ISHR        =  0x7a;
	int LSHR        =  0x7b;
	int IUSHR       =  0x7c;
	int LUSHR       =  0x7d;
	int IAND        =  0x7e;
	int LAND        =  0x7f;
	int IOR         =  0x80;
	int LOR         =  0x81;
	int IXOR        =  0x82;
	int LXOR        =  0x83;
	int IINC        =  0x84;
	int I2L         =  0x85;
	int I2F         =  0x86;
	int I2D         =  0x87;
	int L2I         =  0x88;
	int L2F         =  0x89;
	int L2D         =  0x8a;
	int F2I         =  0x8b;
	int F2L         =  0x8c;
	int F2D         =  0x8d;
	int D2I         =  0x8e;
	int D2L         =  0x8f;
	int D2F         =  0x90;
	int I2B         =  0x91;
	int I2C         =  0x92;
	int I2S         =  0x93;
	int LCMP        =  0x94;
	int FCMPL       =  0x95;
	int FCMPG       =  0x96;
	int DCMPL       =  0x97;
	int DCMPG       =  0x98;
	int IFEQ        =  0x99;
	int IFNE        =  0x9a;
	int IFLT        =  0x9b;
	int IFGE        =  0x9c;
	int IFGT        =  0x9d;
	int IFLE        =  0x9e;
	int IF_ICMPEQ   =  0x9f;
	int IF_ICMPNE   =  0xa0;
	int IF_ICMPLT   =  0xa1;
	int IF_ICMPGE   =  0xa2;
	int IF_ICMPGT   =  0xa3;
	int IF_ICMPLE   =  0xa4;
	int IF_ACMPEQ   =  0xa5;
	int IF_ACMPNE   =  0xa6;
	int GOTO        =  0xa7;
	int JSR         =  0xa8;
	int RET         =  0xa9;
	int TABLESWITCH =  0xaa;
	int LOOKUPSWITCH=  0xab;
	int IRETURN     =  0xac;
	int LRETURN     =  0xad;
	int FRETURN     =  0xae;
	int DRETURN     =  0xaf;
	int ARETURN     =  0xb0;
	int RETURN      =  0xb1;
	int GETSTATIC   =  0xb2; // FIXME works around Eclipse bug 35438
	int PUTSTATIC   =  0xb3;
	int GETFIELD    =  0xb4;
	int PUTFIELD    =  0xb5;
	int INVOKEVIRTUAL   =  0xb6; // FIXME works around Eclipse bug 35438
	int INVOKESPECIAL   =  0xb7;
	int INVOKESTATIC    =  0xb8;
	int INVOKEINTERFACE =  0xb9;
	int XXXUNUSEDXXX=  0xba;
	int NEW         =  0xbb;
	int NEWARRAY    =  0xbc;
	int ANEWARRAY   =  0xbd;
	int ARRAYLENGTH =  0xbe;
	int ATHROW      =  0xbf;
	int CHECKCAST   =  0xc0;
	int INSTANCEOF  =  0xc1;
	int MONITORENTER=  0xc2;
	int MONITOREXIT =  0xc3;
	int WIDE        =  0xc4;
	int MULTIANEWARRAY = 0xc5;
	int IFNULL      =  0xc6;
	int IFNONNULL   =  0xc7;
	int GOTO_W      =  0xc8;
	int JSR_W       =  0xc9;
	int BREAKPOINT  =  0xca;// not used in OVM (yet?)

	/* the following opcodes are defined in the vm spec but do not
	 * need to be adhered to. */
	int LDC_INT_QUICK          = 0xcb; 
	int LDC_REF_QUICK          = 0xcc;
	int LDC_W_REF_QUICK        = 0xcd;

	int GETFIELD_QUICK         = 0xce;
	int PUTFIELD_QUICK         = 0xcf;
	int GETFIELD2_QUICK        = 0xd0;
	int PUTFIELD2_QUICK        = 0xd1;

	int PUTFIELD_WITH_BARRIER_REF  = 0xd2;
	//int GETSTATIC_QUICK        = 0xd2;// redefined!
	int AASTORE_WITH_BARRIER   = 0xd3;
	//int PUTSTATIC_QUICK        = 0xd3;// redefined!
	int PUTFIELD_QUICK_WITH_BARRIER_REF = 0xd4;
	//int GETSTATIC2_QUICK       = 0xd4;// redefined!
	//int PUTSTATIC2_QUICK       = 0xd5;// redefined!
	int INVOKEVIRTUAL_QUICK    = 0xd6;
	int INVOKENONVIRTUAL_QUICK = 0xd7;
	int INVOKESUPER_QUICK      = 0xd8;
	int INVOKESTATIC_QUICK     = 0xd9;// not used in OVM
	int INVOKEINTERFACE_QUICK  = 0xda;  // not used in OVM
	int INVOKEVIRTUALOBJECT_QUICK = 0xdb;// not used in OVM
	int UNCHECKED_AASTORE      = 0xdc;// not defined in JVM spec
	int NEW_QUICK              = 0xdd;
	int ANEWARRAY_QUICK        = 0xde;
	int MULTIANEWARRAY_QUICK   = 0xdf;
	int CHECKCAST_QUICK        = 0xe0;
	int INSTANCEOF_QUICK       = 0xe1;

        ////int INITIALIZE           = 0xe2;  // OBSOLETE
        int NONCHECKING_TRANSLATING_READ_BARRIER	= 0xe2;
        int CHECKING_TRANSLATING_READ_BARRIER	= 0xf8;

        int POLLCHECK              = 0xe3;

	/* ovm refactoring artefacts */
	////int INVOKEVIRTUAL_QUICK_W = 0xe2;// not used in OVM
	////int GETFIELD_QUICK_W      = 0xe3;// not used in OVM
	////int PUTFIELD_QUICK_W      = 0xe4;// not used in OVM
	
	int NULLCHECK              = 0xe4;

	/**
	 * Invoke OS method (i.e. exit, getmem, freemem, printstring).
	 * See InvokeSystemArguments!
	 **/
	int INVOKE_SYSTEM          = 0xe5;
	/**
	 * A new quick opcode to be used when the field is known to be of
	 * reference type. We only rewrite to GETFIELD_QUICK for fields known
	 * to be of primitive type. With this we are able to do dynamic
	 * tagging of stack slots.
	 **/
	int REF_GETFIELD_QUICK     = 0xe6;
	int INVOKE_NATIVE          = 0xe7;

	int INB			   = 0xe8;
	int OUTB		   = 0xe9;

	int ROLL                   = INVOKEVIRTUALOBJECT_QUICK; // FIXME collision with the unused INVOKEVIRTUALOBJECT_QUICK!
	int COPY		   = 0xf9;
	int INVOKENONVIRTUAL2_QUICK = INVOKESTATIC_QUICK;

	int LDC_DOUBLE_QUICK       = 0xea; 
	int LDC_FLOAT_QUICK        = 0xeb; 
	int LDC_LONG_QUICK         = 0xec;

        int AFIAT                  = 0xed;
        int IFIAT                  = 0xee;
        int LFIAT                  = 0xef;
        int FFIAT                  = 0xf0;
        int DFIAT                  = 0xf1; 

        int SINGLEANEWARRAY        = 0xf2;
        int LOAD_SHST_FIELD        = 0xf3;
        int LOAD_SHST_METHOD       = 0xf4;
        int LOAD_SHST_FIELD_QUICK  = 0xf5;
        int LOAD_SHST_METHOD_QUICK = 0xf6;

        

        // used by the read barrier code
        int READ_BARRIER  = 0xf7;
        
        int PUTSTATIC_WITH_BARRIER_REF = 0xfa;

        int LABEL = 0xfb;

	/* also defined in the spec, but not used in ovm are: */
	// int IMPDEP1     =  0xfe;
	// int IMPDEP2     =  0xff;

	/*what to add to an opcode to get the wide-opcode number */
	
	int INCREMENT_COUNTER = 0xfe;
	int WIDE_OFFSET = 0x100;

    }
    
    public interface InvokeSystemArguments {
	byte ACTIVE_CONTEXT = 0x03;
	byte SWITCH_CONTEXT = 0x04;
	int DESTROY_NATIVE_CONTEXT = 0x05;

        // poll count for interpeter hook execution
	int SETPOLLCOUNT    = 0x09;

	byte EMPTY_CSA_CALL  = 0x19;
	
	byte WORD_OP         = 0x20;
	byte DEREFERENCE     = 0x21;

	// interpreter debugging 
	int START_TRACING     = 0x30;
	int STOP_TRACING      = 0x31;
	int BREAKPOINT        = 0x32;
        int BEGIN_OVERRIDE_TRACING = 0x33;
        int END_OVERRIDE_TRACING   = 0x34;

	// stack manipulation
	int NEW_CONTEXT       = 0x89;
	int MAKE_ACTIVATION   = 0x8C;
	int CUT_TO_ACTIVATION = 0x8D;
	int RUN       	      = 0x8E;
	int GET_ACTIVATION    = 0x8F;
	int GET_CONTEXT       = 0x90;
	int INVOKE            = 0x91;
	int CAS32             = 0x92;
	int CAS64             = 0x93;
	int GET_NATIVE_CONTEXT	= 0x94;
	int NATIVE_CONTEXT_TO_CONTEXT = 0x95;
	
//	int INCREMENT_COUNTER = 0xa0;
    }

    /**
     * Exception types, from which we derive throwables.h
     **/
    public interface Throwables {
	/*
	  Hit ^x^e on the last line of this program to generate new
	  consecutive throwable numbers.  When adding an error code, 
	  be sure to include an equal-sign and semicolon.
	  
	  (The format 123 thing is to avoid using an open-brace
	  character in this comment.)

	  (save-excursion
	    (require 'cl)
	    (search-backward (format "%c" 123))
            (mark-sexp)
	    (narrow-to-region (point) (mark))
	    (loop for i from 0
	          while (re-search-forward "= *[0-9]*;" nil t)
		  do (replace-match (format "= %d;" i)))
            (widen))
	*/
	int NO_THROWABLE = -1;
	int NULL_POINTER_EXCEPTION = 0;
	int ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = 1;
	int CLASS_CAST_EXCEPTION = 2;
        int OUT_OF_MEMORY_ERROR = 3;
        int INTERNAL_ERROR = 4;
        int ILLEGAL_ARGUMENT_EXCEPTION = 5;
	int STACK_OVERFLOW_ERROR = 6;
	int ARRAY_STORE_EXCEPTION = 7;
	// this isn't actually passed to generateThrowable, but giving
	// it a number implicitly adds it to list of exceptions thrown
	// by CSA calls in JavaNames.
        int ILLEGAL_MONITOR_STATE_EXCEPTION = 8;
	int ARITHMETIC_EXCEPTION = 9;

	int VIRTUAL_MACHINE_ERROR = 10;

        int NEGATIVE_ARRAY_SIZE_EXCEPTION = 11;	

	// LinkageError and a few of its many wonderful subtypes...
	int LINKAGE_ERROR = 12;

	// errors during loading
	int NO_CLASS_DEF_FOUND_ERROR = 13;
	int CLASS_FORMAT_ERROR = 14;
	int CLASS_CIRCULARITY_ERROR = 15;
	int UNSATISFIED_LINK_ERROR = 16;

	// errors during verification
	int VERIFY_ERROR = 17;

	// errors during initialization
	int EXCEPTION_IN_INITIALIZER_ERROR = 18;

	// errors during execution
	int INCOMPATIBLE_CLASS_CHANGE_ERROR = 19;
	int INSTANTIATION_ERROR = 20;
	int NO_SUCH_METHOD_ERROR = 21;

	// This type is not thrown directly, but it is used in
	// Instruction.java to capture unpredictable throwing behavior.
	int ERROR = 22;

	int THROWABLE = 23;	// must be last
	int N_THROWABLES = THROWABLE + 1;
    }
    
    /**
     * Types of word comparison (matching defs in interpreter_defs.h for now)
     **/
    public interface WordOps {
      	byte sCMP = 0;
	byte uCMP = 1;
	byte uLT  = 2;
	byte uLE  = 3;
	byte uGE  = 4;
	byte uGT  = 5;
	byte uI2L = 6;
    }

    /**
     * Dereference operations (matching defs in interpreter_defs.h for now).
     * These are only the operations that aren't easily mapped to quick
     * get/putfields.
     **/
    public interface DereferenceOps {
      	byte getByte  = 0;
	byte getShort = 1;
	byte getChar  = 2;
	byte setByte  = 3;
	byte setShort = 4;
	byte setBlock = 5;
    }

    /**
     * Byte-length type tag.
     **/
    public interface ByteLengthTypeTag {
	byte TYPE_TAG_VOID      = 0;
	byte TYPE_TAG_INT       = 1;
	byte TYPE_TAG_LONG      = 2;
	byte TYPE_TAG_FLOAT     = 3;
	byte TYPE_TAG_DOUBLE    = 4;
	byte TYPE_TAG_REFERENCE = 5;
	byte TYPE_TAG_SHORT     = 6;
	byte TYPE_TAG_CHAR      = 7;
	byte TYPE_TAG_BYTE      = 8;
	byte TYPE_TAG_BOOLEAN   = 9;
    }

} // End of JVMConstants
