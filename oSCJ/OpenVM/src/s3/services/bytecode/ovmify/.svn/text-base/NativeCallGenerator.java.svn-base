package s3.services.bytecode.ovmify;

import java.io.PrintWriter;

import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Type;
import ovm.core.execution.Context;
import ovm.core.execution.Interpreter;
import ovm.core.execution.Native;
import ovm.core.execution.NativeInterface;
import ovm.core.execution.RuntimeExports;
import ovm.core.repository.Attribute;
import ovm.core.repository.Descriptor;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.format.CFormat;
import ovm.core.services.memory.MemoryManager;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import ovm.services.bytecode.editor.Cursor;
import ovm.util.ArrayList;
import ovm.util.BitSet;
import ovm.util.HashMap;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.Map;
import ovm.util.OVMError;
import s3.core.domain.S3ByteCode;
import ovm.core.domain.ConstantPool;
import s3.core.domain.S3Method;
import s3.services.bootimage.BootBase;
import s3.services.bootimage.ReflectionSupport;
import s3.services.transactions.Transaction;
import s3.util.PragmaTransformCallsiteIR;
import ovm.core.stitcher.InvisibleStitcher;
import java.io.IOException;

/**
 * Responsible for dealing with most types of native method
 * definitions and calls.  The details of native method handling
 * differ greatly between user domains and the executive domain, but
 * in both cases, calls into C code are done using an
 * <code>INVOKE_NATIVE</code> instruction.  Static members of
 * <code>NativeCallGenerator</code> maintain the mapping between C
 * function names and <code>INVOKE_NATIVE</code> operands, and
 * generate C code that is used to implement
 * <code>INVOKE_NATIVE</code>.
 *
 * Within the user domain, C functions should only be called with
 * primitive and array-of-primitive arguments.  Executive domain code
 * is a bit more flexible, due to the implicit conversions we perform:
 * <ul>
 * <li> Primitive arrays are tranlated to pointers to the first array
 *      element.
 * <li> <code>String</code> values are passed as two arguments, the
 *      first argument is a pointer to the (executive domain)
 *      string's first byte, and the second is the string's length.
 * <li> {@link ovm.core.services.memory.VM_Address} values are passed
 *      as <code>void *</code>s 
 * <li> {@link ovm.core.domain.Oop} values are passed as 
 *      <code>struct HEADER *</code>s 
 *      (the C definition of an object header).
 * <li> {@link ovm.core.execution.Native.Ptr} values are unboxed to
 *      reveal the underlying C pointer.
 * <li> {@link ovm.core.execution.Context} objects are passed as the
 *      structure type  declared in <code>structs.h</code>
 * <li> Other reference-typed arguments are not allowed.
 * </ul>
 **/
public abstract class NativeCallGenerator 
    implements JVMConstants, JVMConstants.Opcodes
{
    public static void d(int kind, Object o1, Object o2, Object o3)
	throws PragmaTransformCallsiteIR.Ignore
    {
	BootBase.d(kind, o1, o2, o3);
    }

    public static void d(int kind, Object o1, Object o2)
	throws PragmaTransformCallsiteIR.Ignore
    {
	BootBase.d(kind, o1, o2);
    }

    // UnboundSelector.Method x byte[2] { INVOKE_NATIVE, # }
    private static final HashMap nativeCalls = new HashMap();

    // UnboundSelector.Method x byte[3] { INVOKE_SYSTEM, #, # }
    private static final HashMap systemCalls = new HashMap();

    private static final int INIT = RepositoryUtils.asUTF("<init>");
    private static final int CLINIT = RepositoryUtils.asUTF("<clinit>");

    static final TypeName.Scalar OPAQUE_TYPE_NAME =
        RepositoryUtils.makeTypeName("org/ovmj/java", "Opaque");
    static final int LibraryGlue_INDEX = RepositoryUtils.asUTF("LibraryGlue");
    static final int LibraryImports_INDEX =
        RepositoryUtils.asUTF("LibraryImports");
    static final Selector.Method ULE_CTOR =
        Selector.Method.make(JavaNames.INIT_WITH_STRING,
			     JavaNames.java_lang_UnsatisfiedLinkError);

    protected abstract boolean allowNonFinal();
    protected abstract boolean declaredNativeOnly();

    static int nextCallNum = 0;

    // FIXME: why is the first call assigned 1 instead of 0?
    protected void defineNativeInterface(Type.Compound t) {
	t = t.getInstanceType().asCompound();

	// Don't require LibraryGlue to be final
	if (!(allowNonFinal() || t.getMode().isFinal())) {
	    throw new OVMError(
			       "Native interface class "
			       + t.getName()
			       + " must be declared final");
	}
	for (Method.Iterator it = t.localMethodIterator();
	     it.hasNext();
	     ) {
	    Method m = it.next();
	    Selector.Method sel = m.getSelector();	    
	    if (!sel.isConstructor())
		throw new OVMError(
				   "Native interface class "
				   + t.getName()
				   + " has non-static methods");
	}
	Method.Iterator it = t.getSharedStateType().localMethodIterator();
	while (it.hasNext()) {
	    Method m = it.next();
	    if (m.getMode().isSynthetic()
		// Why isn't <clinit> synthetic?  Does it correspond
		// to a method declaration in source code?  I don't
		// think so.
		|| m.getSelector().getNameIndex() == CLINIT
		|| Transaction.the().isTransMethod(m)) {
		continue;
	    }
	    // NativeInterface methods can contain hosted
	    // implementations of natives, but LibraryGlue
	    // native methods are always declared as such.
	    if (!declaredNativeOnly() || m.getMode().isNative()) {
		Selector.Method sel = m.getSelector();
		UnboundSelector.Method ubs = sel.getUnboundSelector();
		if (nativeCalls.get(ubs) == null) {
		    if (!OVMBase.isBuildTime()) {
			LinkageException ch =
			    new LinkageException.UnsatisfiedLink("can't link native method " + ubs);
			throw ch.unchecked();
		    } else {
			nativeCalls.put(ubs,
					new byte[] { (byte) INVOKE_NATIVE,
						     (byte) ++nextCallNum });
		    }
		}
	    }
	}
    }

    static {
        //FIXME for 64 bit addresses!
        sys(
            "setPollingEventCount:(J)J",
            (byte) InvokeSystemArguments.SETPOLLCOUNT);
        sys(
            "destroyNativeContext:(I)V",
            (byte) InvokeSystemArguments.DESTROY_NATIVE_CONTEXT);
/*        sys(
            "incrementCounter:(I)V",
            (byte) InvokeSystemArguments.INCREMENT_COUNTER);            */
        sys(
            "makeActivation:"
                + "(I"
                + "Lovm/core/domain/Code;"
                + "Lovm/core/execution/InvocationMessage;"
                + ")I",
            (byte) InvokeSystemArguments.MAKE_ACTIVATION);
        sys(
            "invokeVoid:("
                + "Lovm/core/domain/Code;"
                + "Lovm/core/execution/InvocationMessage;"
                + ")V",
            (byte) InvokeSystemArguments.INVOKE,
            (byte) TypeCodes.VOID);
        sys(
            "invokeInteger:("
                + "Lovm/core/domain/Code;"
                + "Lovm/core/execution/InvocationMessage;"
                + ")I",
            (byte) InvokeSystemArguments.INVOKE,
            (byte) TypeCodes.INT);
        sys(
            "invokeFloat:("
                + "Lovm/core/domain/Code;"
                + "Lovm/core/execution/InvocationMessage;"
                + ")F",
            (byte) InvokeSystemArguments.INVOKE,
            (byte) TypeCodes.FLOAT);

        sys(
            "invokeLong:("
                + "Lovm/core/domain/Code;"
                + "Lovm/core/execution/InvocationMessage;"
                + ")J",
            (byte) InvokeSystemArguments.INVOKE,
            (byte) TypeCodes.LONG);
        sys(
            "invokeDouble:("
                + "Lovm/core/domain/Code;"
                + "Lovm/core/execution/InvocationMessage;"
                + ")D",
            (byte) InvokeSystemArguments.INVOKE,
            (byte) TypeCodes.DOUBLE);
        sys(
            "invokeOop:("
                + "Lovm/core/domain/Code;"
                + "Lovm/core/execution/InvocationMessage;"
                + ")Lovm/core/domain/Oop;",
            (byte) InvokeSystemArguments.INVOKE,
            (byte) TypeCodes.OBJECT);

        sys("getActivation:(I)I", (byte) InvokeSystemArguments.GET_ACTIVATION);
        sys(
            "getContext:(I)Lovm/core/execution/Context;",
            (byte) InvokeSystemArguments.GET_CONTEXT);
        sys(
            "cutToActivation:(II)V",
            (byte) InvokeSystemArguments.CUT_TO_ACTIVATION);
        sys(
            "newNativeContext:(Lovm/core/execution/Context;)I",
            (byte) InvokeSystemArguments.NEW_CONTEXT);
        sys("run:(II)V", (byte) InvokeSystemArguments.RUN);
        sys("emptyCSACall:()V", InvokeSystemArguments.EMPTY_CSA_CALL);
        sys("startTracing:(Z)V", (byte) InvokeSystemArguments.START_TRACING);
        sys("stopTracing:()V", (byte) InvokeSystemArguments.STOP_TRACING);
        sys(
            "beginOverrideTracing:()V",
            (byte) InvokeSystemArguments.BEGIN_OVERRIDE_TRACING);
        sys(
            "endOverrideTracing:()V",
            (byte) InvokeSystemArguments.END_OVERRIDE_TRACING);
        sys(
            "breakpoint:(Ljava/lang/Object;)V",
            (byte) InvokeSystemArguments.BREAKPOINT);
    }

    /**
     * True if <code>t</code> contains methods whose call sites should
     * be translated into <code>INVOKE_NATIVE</code>.
     **/
    protected abstract boolean isNativeClass(Type t);
    /**
     * True if <code>t</code> contains methods whose call sites should
     * be translated into <code>INVOKE_SYSTEM</code>.<p>
     * FIXME: down to {@link NativeCallGenerator.Executive}?
     * @see Interpreter
     **/
    protected abstract boolean isSystemClass(Type t);

    /**
     * Return the OvmIR representation of a native or system call, or
     * return null if <code>m</code> has no such translation.
     **/
    public byte[] getSpecialSequence(Method m) {
        if (!m.isNonVirtual() || !m.getDeclaringType().isSharedState())
            return null;
	Type t = m.getDeclaringType().getInstanceType();
	if (isNativeClass(t))
	    return (byte[]) nativeCalls.get(m.getSelector().getUnboundSelector());
	else if (isSystemClass(t))
	    return (byte[]) systemCalls.get(m.getSelector().getUnboundSelector());
	else
	    return null;
    }

    // called from w/in this package
    /**
     * Provide a bytecode implemenation of a method that appears
     * native in the source, or null.
     **/
    protected abstract S3ByteCode.Builder
	fillInNativeMethod(S3Method m, ConstantPool b);

    /**
     * If calls to m are forwarded to some java method m', return m'.
     * Otherwise, return null.
     **/
    public Method getNativeMethodImplementation(Method m) {
	return null;
    }
    /**
     * Return true if a native method has no java-visible effect other
     * than possibly throwing UnsatisfiedLinkError.
     **/
    public boolean isNativeMethodHarmless(Method nat) {
	return !isNativeMethodVMCall(nat);
    }
    /**
     * Return true if a native method declaration actually declares a
     * VM entry point.
     * FIXME: Why isn't all RuntimeExports support here?
     * @see RuntimeExports
     **/
    public boolean isNativeMethodVMCall(Method nat) {
	int idx = nat.getDeclaringType().getName().getShortNameIndex();
	return idx == LibraryImports_INDEX;
    }
    
    /** Assumes the method m is static. * */
    private static void sys(String m, byte maj, byte min) {
        Selector.Method sel =
            ReflectionSupport.methodSelectorFor(Interpreter.class, m, true);
        //Descriptor.Method rdm = sel.getDescriptor();
      //  int argwords = rdm.getArgumentCount() + rdm.getWideArgumentCount();
        systemCalls.put(
            sel.getUnboundSelector(),
            new byte[] {(byte) Opcodes.INVOKE_SYSTEM, maj, min });
    }

    private static void sys(String m, byte b) {
        sys(m, b, (byte) 0);
    }

    private interface S extends InvokeSystemArguments {}
    private interface D extends DereferenceOps {}
    private interface W extends WordOps {}
    
    private static final UnboundSelector.Method[][] invokeSystemMap;
    static {
	int maxMajor = S.CAS64;
	int maxMinor = W.uI2L;
	for (Iterator it = systemCalls.values().iterator();
	     it.hasNext(); ) {
	    byte[] b = (byte[]) it.next();
	    maxMajor = Math.max(maxMajor, 0xff & b[1]);
	    maxMinor = Math.max(maxMinor, 0xff & b[2]);
	}
	invokeSystemMap = new UnboundSelector.Method[maxMajor+1][maxMinor+1];
	for (Iterator it = systemCalls.entrySet().iterator();
	     it.hasNext(); ) {
	    Map.Entry ent = (Map.Entry) it.next();
	    byte[] b = (byte[]) ent.getValue();
	    invokeSystemMap[0xff & b[1]][0xff & b[2]] =
		(UnboundSelector.Method) ent.getKey();
	}
	deref("getByte:(Lovm/core/services/memory/VM_Address;)B", D.getByte);
	deref("getShort:(Lovm/core/services/memory/VM_Address;)S", D.getShort);
	deref("getChar:(LLovm/core/services/memory/VM_Address;)C", D.getChar);
	deref("setByte:(Lovm/core/services/memory/VM_Address;B)V", D.setByte);
	deref("setShort:(Lovm/core/services/memory/VM_Address;S)V", D.setShort);
	deref("setBlock:(Lovm/core/services/memory/VM_Address;" +
	                "Lovm/core/services/memory/VM_Address;" +
	                "Lovm/core/services/memory/VM_Word;)V", D.getByte);
	word("sCMP:(Lovm/core/services/memory/VM_Word;" +
	           "Lovm/core/services/memory/VM_Word;)I", W.sCMP);
	word("uCMP:(Lovm/core/services/memory/VM_Word;" +
	           "Lovm/core/services/memory/VM_Word;)I", W.uCMP);
	word("uLT:(Lovm/core/services/memory/VM_Word;" +
	          "Lovm/core/services/memory/VM_Word;)B", W.uLT);
	word("uLE:(Lovm/core/services/memory/VM_Word;" +
	          "Lovm/core/services/memory/VM_Word;)B", W.uLE);
	word("uGE:(Lovm/core/services/memory/VM_Word;" +
	          "Lovm/core/services/memory/VM_Word;)B", W.uGE);
	word("uGT:(Lovm/core/services/memory/VM_Word;" +
	          "Lovm/core/services/memory/VM_Word;)B", W.uGT);
	word("uI2L:(Lovm/core/services/memory/VM_Word;)J", W.uI2L);
	// Why no attemptUpdate(Adress, long, long)?
	sysP("attemptUpdate:(Lovm/core/services/memory/VM_Address;" +
	                    "Lovm/core/services/memory/VM_Word;" +
	                    "Lovm/core/services/memory/VM_Word;)Z",
	     S.CAS32);
	sysP("attemptUpdate:(Lovm/core/services/memory/VM_Address;II)Z",
	     S.CAS32);
	sysP("makeNativeContext:(Lovm/core/execuction/Context;)I",
	     S.NEW_CONTEXT);
    }
    // FIXME: Eliminate pragmas on methods registered above.
    // Most INVOKE_SYSTEM instructions are generated here, but
    // a few in VM_Address, VM_Word, AtomicOps, and Context are
    // generated by pragmas.  We want to be able to give the caller a
    // signature for each INVOKE_SYSTEM encountered, so we hard-code
    // the definitions here, as well as in the pragmas
    private static void deref(String m, int minor) {
	invokeSystemMap[S.DEREFERENCE][minor] = 
	    RepositoryUtils.makeUnboundSelector(m).asMethod();
    }
    private static void word(String m, int minor) {
	invokeSystemMap[S.WORD_OP][minor] = 
	    RepositoryUtils.makeUnboundSelector(m).asMethod();
    }
    private static void sysP(String m, int op) {
	invokeSystemMap[op][0] =
	    RepositoryUtils.makeUnboundSelector(m).asMethod();
    }

    // Memoize return value between native call additions
    private static UnboundSelector.Method[] invokeNativeArgumentMap =
	new UnboundSelector.Method[1];

    /**
     * Return an array of C function signatures index by the immediate
     * operand to <code>INVOKE_NATIVE</code> instructions.  In other
     * words, an <code>INVOKE_NATIVE <em>n</em></code> instruction
     * calls the C function
     * <code>getNativeArgumentMap()[<em>n</em>]</code>. This method
     * may be called before all native methods have been defined, and
     * is careful to return the most up-to-date list of functoin
     * signatures.
     **/
    public static UnboundSelector.Method[] getNativeArgumentMap() {
	if (invokeNativeArgumentMap.length == nativeCalls.size() + 1)
	    return invokeNativeArgumentMap;
	invokeNativeArgumentMap =
	    new UnboundSelector.Method[nativeCalls.size() + 1];
	for (Iterator it = nativeCalls.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry ent = (Map.Entry) it.next();
	    UnboundSelector.Method sel = (UnboundSelector.Method) ent.getKey();
	    byte[] insn = (byte[]) ent.getValue();
	    invokeNativeArgumentMap[0xff & insn[1]] = sel;
	}
        return invokeNativeArgumentMap;
    }

    /**
     * Return the name and type of a C function called through
     * {@link ovm.services.bytecode.Instruction.INVOKE_NATIVE}
     **/
    public static UnboundSelector.Method invokeNativeType(int code) {
	if (code >= invokeNativeArgumentMap.length)
	    // for effect, method should be removed
	    getNativeArgumentMap();
	return invokeNativeArgumentMap[code];
    }

    /**
     * Return the name and type of a builtin operation called through
     * {@link ovm.services.bytecode.Instruction.INVOKE_SYSTEM}.
     **/
    public static UnboundSelector.Method invokeSystemType(int code, int sub) {
	return invokeSystemMap[code][sub];
    }

    /**
     * Generate the body of a C switch statement that dispatches
     * <code>INVOKE_NATIVE</code> calls and applies the type
     * conversions described above.  The generated C code is defined
     * in terms of four macros
     * <ol>
     * <li> <code>POP()</code> retrieves the next argument (from right
     *      to left) as a <code>struct jvalue</code>.
     * <li> <code>PUSH_P()</code> returns a 32-bit primitive value
     * <li> <code>PUSH_R()</code> returns a pointer value
     * <li> <code>PUSH_W()</code> returns a 64-bit primitive value
     * </ol>
     * Note that <code>POP()</code> processes arguments in the opposite
     * order from <code>va_arg</code>.  Note also that the interpreter
     * defines these same four macros for its own reasons.
     **/
    public static void output(String fname) throws IOException {
	PrintWriter out = new PrintWriter(fname);
	boolean profile=InvisibleStitcher.getBoolean("profile-natives");
	UnboundSelector.Method[] nat = getNativeArgumentMap();
	out.println("#define NNATIVES "+nat.length);
	if (profile) {
	    out.println("#define PROFILING_NATIVES");
	    out.println("profileNativeBegin(code);");
	    out.println("#undef DONE_HOOK");
	    out.println("#define DONE_HOOK profileNativeEnd(code);");
	}
	out.println("switch (code) {");
	for (int i = 1; i < nat.length; i++)
	    outputForSelector(nat[i], i, out);
	out.println("default: fprintf(stderr,\"unknown native call %d\\n\",code); abort(); }");
	out.close();
    }

    private static void outputForSelector(UnboundSelector.Method sel,
					  int callNum,
					  PrintWriter out) {
        Descriptor.Method desc = sel.getDescriptor();
        out.println("case " + callNum+ ":{");

        declareArgs(sel, out);

        // we avoid explicit casts where possible to ensure type matches at the
        // C level. The one exception is for int because we use it for all
        // native pointer types.
        // Boolean also has to be handled specially - not sure why - DH
        TypeName rettype = desc.getType();
	String pushOp = null;
        switch (rettype.getTypeTag()) {
            case TypeCodes.VOID :
                out.print("\t");
                break;
            case TypeCodes.LONG :
                // need cast for unsigned (off_t)
                out.print("\tjlong res = (jlong) ");
		pushOp = "PUSH_W";
                break;
            case TypeCodes.BYTE :
            case TypeCodes.CHAR :
            case TypeCodes.SHORT :
                out.print("\tjint res = ");
		pushOp = "PUSH_P";
                break;
            case TypeCodes.INT :
                out.print("\tjint res = "); // need cast for pointers
		pushOp = "PUSH_P";
                break;
            case TypeCodes.FLOAT :
                out.print("\tjfloat res = ");
		pushOp = "PUSH_P";
                break;
            case TypeCodes.DOUBLE :
                out.print("\tjdouble res = ");
		pushOp = "PUSH_W";
                break;
            case TypeCodes.BOOLEAN :
                out.print("\tjint res = "); // boolean = int on opstack
		pushOp =  "PUSH_P";
                break; // OK
            case TypeCodes.ARRAY :
                out.print("\tjref res = ");
		pushOp = "PUSH_R";
                break;
            case TypeCodes.OBJECT :
                {
                    if (rettype == JavaNames.ovm_core_services_memory_VM_Address) {
                        out.print("\tjint res = (jint)");
			pushOp = "PUSH_P";
                    } else {
                        out.print("\tjref res = ");
			pushOp = "PUSH_R";
                    }
                    break;
                }
                // no break here!
            default :
                throw new OVMError(
                    "Unsupported return type "
                        + rettype
                        + " for native method in "
                        + sel);
        }
        out.print(sel.getName());
        String prefix = "(";
        for (int j = 0; j < desc.getArgumentCount(); j++) {
            out.print(prefix);
            // byte[] gets cast to char*
            if (desc.getArgumentType(j) == JavaNames.arr_byte)
                out.print("(char*)");
            out.print("arg" + j);
            prefix = ", ";

            // Strings have an extra length argument
            if (desc.getArgumentType(j) == JavaNames.java_lang_String) {
                out.print(", arg" + j + "_len");
            }
        }

        if (prefix == "(")
            out.print(prefix);
        out.println(");");
	out.println("DONE_HOOK;");
	if (pushOp != null)
	    out.println("\t" + pushOp + "(res);");
        out.println("\tbreak;");
        out.println("}");
    }

    private static String forwardPointer(String pointer) {
    
    	int forwardOffset = ObjectModel.getObjectModel().getForwardOffset();
	return "(* (typeof("+pointer+")*) ( (char *) "+pointer+" + "+forwardOffset+" ) )";
    }

    private static void declareArgs(UnboundSelector.Method sel,
				    PrintWriter out) {
        Descriptor.Method desc = sel.getDescriptor();
        for (int i = desc.getArgumentCount() - 1; i >= 0; i--) {
            TypeName arg = desc.getArgumentType(i);

	    switch (arg.getTypeTag()) {
	    // all integer types are accessed through jint and then
	    // cast as needed
	    case TypeCodes.SHORT : 
	    case TypeCodes.CHAR :
	    case TypeCodes.BYTE :
	    case TypeCodes.INT :
		{
		    String jname = CFormat._.format(arg);
		    out.println(
                                "\t"
				+ jname
				+ " arg"
				+ i
				+ " = ("
				+ jname
				+ ")POP().jint"
				+ ";");
		    break;
		}
	    case TypeCodes.FLOAT :
		{
		    String jname = CFormat._.format(arg);
		    out.println(
				"\t"
				+ jname
				+ " arg"
				+ i
				+ " = "
				+ "POP()."
				+ jname
				+ ";");
		    break;
		}
	    case TypeCodes.LONG :
	    case TypeCodes.DOUBLE :
		{
		    String jname = CFormat._.format(arg);
		    out.println(
                                "\t"
				+ jname
				+ " arg"
				+ i
				+ " = "
				+ "POP_W()."
				+ jname
				+ ";");
		    break;
		}

	    case TypeCodes.BOOLEAN :
		out.println("\tjboolean arg" + i + " = (POP().jint != 0);");
		break;
	    case TypeCodes.ARRAY :
		{
		    TypeName.Array t = arg.asArray();
		    TypeName ct = t.getInnermostComponentTypeName();
		    if (ct.isPrimitive() && t.getDepth() == 1) {
			out.print('\t');
			out.println(
				    CFormat._.format(arg)
				    + "* arr_"
				    + i
				    + " = "
				    + "("
				    + CFormat._.format(arg)
				    + "*)POP().jref;");

			out.print('\t');
			
			// this is a hack!! we assume the array is contiguous
			// but it is accompanied by hacks elsewhere that try to make sure that it is the case
			if (MemoryManager.the().usesArraylets()) {
				out.println(
				    CFormat._.format(ct)
				    + "* arg"
				    + i
				    + " = 0;");
				out.println(
				    "\tif ( arr_"+i+"->length > 0 ) {");
				out.println("\t\targ"+i+" = "
				    + "* ( " + CFormat._.format(ct) +" **)("
				    + "arr_"
				    + i
				    + "->values) ;");			
				out.println("#ifndef NDEBUG");
				//FIXME: this is not exact!!! make it exact to make sure it does not allow non-contiguous arrays
				out.println("\t\tassert( (( ((int)arg"+i+") > ((int)arr_"+i+"->values) ) && "+
					"( ((int)arg"+i+") <  (((int)arr_"+i+"->values) + sizeof( "+CFormat._.format(ct)+") * sizeof(int) * arr_"+i+"->length / "+
					MemoryManager.the().arrayletSize()+" + 8 ) )) || (arr_"+i+"->length < "+MemoryManager.the().arrayletSize()+") );");
				out.println("#endif");
				out.println("\t}");
				
			} else if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
			
				/*
				int forwardOffset = ObjectModel.getObjectModel().getForwardOffset();
				
				out.println(
				    CFormat._.format(ct)
				    + "* arg"
				    + i
				    + " = " + "(*( "+CFormat._.format(arg)+ "**) ( (char *) arr_"+i+" + "+forwardOffset+" ))"
				    + "->values;");				
				*/
				out.println(
				    CFormat._.format(ct)
				    + "* arg"
				    + i
				    + " = "
				    + forwardPointer( "arr_" + i ) +
				    "->values;");				
			} else {
				out.println(
				    CFormat._.format(ct)
				    + "* arg"
				    + i
				    + " = "
				    + "arr_"
				    + i
				    + "->values;");
			}
		    } else
			OVMBase.fail("bad array type " + ct);

		    break;
		}
	    case TypeCodes.GEMEINSAM :
		OVMBase.fail("Can this happen?");
		break;
	    case TypeCodes.OBJECT :
		{
		    // Yuck, but oh well, Domain code needed here ...
		    Class argclass = null;
		    argclass = ReflectionSupport.classForTypeName(arg);
		    if (argclass == null) { // can't instantiate
			OVMBase.fail("unknown object class");
			// FIXME: what the fuck?
			String jname = CFormat._.format(arg);
			out.print("\t");
			out.print(jname);
			out.print("* arg" + i + " = ");
			out.print("(");
			out.print(jname);
			out.println("*) POP().jref;");
			break;
		    }


		    // NOTE: this is an executive domain String
		    if (arg == JavaNames.java_lang_String) {
			out.print('\t');
			out.println(
				    CFormat._.format(arg)
				    + "* stringObj_"
				    + i
				    + " = "
				    + "("
				    + CFormat._.format(arg)
				    + "*)POP().jref;");

			out.println(
				    "\tstruct arr_jbyte* arr_"
				    + i
				    + " = "
				    + "stringObj_"
				    + i
				    + "->data;");
			if (MemoryManager.the().usesArraylets()) {
				out.println(
				    "\tjbyte* arg" + i + " = 0;");
				out.println(
				    "\tif ( arr_"+i+"->length > 0 ) {");			
				out.println("\t\targ"+i+" = "
				    	+ "* ( jbyte** )("
				    	+ "arr_"
				    	+ i
				    	+ "->values) ;");			
				
				out.println("#ifndef NDEBUG");
				//FIXME: this is not exact!!! make it exact to make sure it does not allow non-contiguous arrays				
				out.println("\t\tassert( (( ((int)arg"+i+") > ((int)arr_"+i+"->values) ) && "+
					"( ((int)arg"+i+") <  (((int)arr_"+i+"->values) + sizeof(jbyte) * sizeof(int) * arr_"+i+"->length / "+
						MemoryManager.the().arrayletSize()+"+8 ) )) || (arr_"+i+"->length < "+MemoryManager.the().arrayletSize()+") || "+
						"( stringObj_"+i+"->count + stringObj_"+i+"->offset < "+MemoryManager.the().arrayletSize()+")"+" );");
				out.println("#endif");
				
				if (MemoryManager.the().needsBrooksTranslatingBarrier()) { // FIXME: is translation needed ?
					out.println("\t\targ"+i+" += " + forwardPointer( "stringObj_" + i) + "->offset;");
				} else {
					out.println("\t\targ"+i+" += stringObj_" + i + "->offset;");
				}
				out.println("\t}");

				out.println(
				    "\tjint arg" + i + "_len"
				    + " = stringObj_" + i + "->count;");				
			} else if (MemoryManager.the().needsBrooksTranslatingBarrier()) { 
				out.println(
				    "\tjbyte* arg" + i + " = " + forwardPointer("arr_" + i) + "->values + "
				    + forwardPointer("stringObj_" + i) + "->offset;");			
				out.println( //FIXME: is translation needed ?
				    "\tjint arg" + i + "_len"
				    + " = " + forwardPointer("stringObj_" + i) + "->count;");				    
			} else {
				out.println(
				    "\tjbyte* arg" + i + " = " + "arr_" + i + "->values + "
				    + "stringObj_" + i + "->offset;");

				out.println(
				    "\tjint arg" + i + "_len"
				    + " = stringObj_" + i + "->count;");
			}

		    } else  if (Native.Ptr.class.isAssignableFrom(argclass)) {
			out.println(
                                    "\tstruct ovm_core_execution_Native_Ptr* ptrObj_"
				    + i
				    + " = "
				    + "((struct ovm_core_execution_Native_Ptr*)POP().jref);");
			String typeName = null;
			try {
			    java.lang.reflect.Method m =
				argclass.getMethod("getTypeName", null);
			    typeName = (String) m.invoke(null, null);
			} catch (Exception e) {
			    throw new OVMError(e);
			}
			out.println(
                                    "\t"
				    + typeName
				    + " arg"
				    + i
				    + " = "
				    + "("
				    + typeName
				    + ") ptrObj_"
				    + i
				    + "->value;");
		    } else if (Context.class.isAssignableFrom(argclass)) {
			out.println(
                                    "\tstruct ovm_core_execution_Context* arg"
				    + i
				    + " = "
				    + "((struct ovm_core_execution_Context*)POP().jref);");
		    } else if (arg == JavaNames.ovm_core_services_memory_VM_Address) {
			out.println(
                                    "\tvoid *arg"
				    + i
				    + " = "
				    + "(void*) POP().jref;");
		    } else if (arg == JavaNames.ovm_core_domain_Oop) {
			out.println(
                                    "\tstruct HEADER *arg"
				    + i
				    + " = "
				    + "(struct HEADER *) POP().jref;");
		    } else {
			// FIXME: why is this allowed
			OVMBase.fail("bad INVOKE_NATIVE object class");
			String jname = CFormat._.format(arg);
			out.print("\t");
			out.print(jname);
			out.print("* arg" + i + " = ");
			out.print("(");
			out.print(jname);
			out.println("*) POP().jref;");
		    }
		    break;
		}
	    default :
		throw new OVMError("can't deal with args of type " + arg);
	    }
        }
    }
    /**
     * Within the executive domain, calls to any method in a subtype
     * of {@link NativeInterface} are translated into
     * <code>INVOKE_NATIVE</code> instructions.  These methods must be
     * static and unsynchronized, but need not be declared
     * <code>native</code>.  If such a "native" method has a java
     * implementation, that implemenation will be available at
     * image-build time (when <code>INVOKE_NATIVE</code> instructions
     * cannot be executed).
     **/
    public static class Executive extends NativeCallGenerator {
	protected boolean allowNonFinal()      { return false; }
	protected boolean declaredNativeOnly() { return false; }

	private Type nativeInterfaceType;
	private final BitSet nativeInterfaces = new BitSet();
	private final BitSet nonNativeInterfaces = new BitSet();

	private final Domain domain;

	public Executive(Domain d) {
	    domain = d;
	}

	private final TypeName nativeInterface
	    = RepositoryUtils.makeTypeName("ovm/core/execution",
					   "NativeInterface");
	private final TypeName interpreter
	    = RepositoryUtils.makeTypeName("ovm/core/execution",
					   "Interpreter");
	
	protected boolean isNativeClass(Type t) {
	    try {
                Blueprint bp = domain.blueprintFor(t);
		if (nonNativeInterfaces.get(bp.getUID()))
		    return false;
		else if (nativeInterfaces.get(bp.getUID()))
		    return true;
		else {
		    if (nativeInterfaceType == null) {
			Type.Context ctx = domain.getSystemTypeContext();
			nativeInterfaceType = ctx.typeFor(nativeInterface);
		    }
                    // XXX: subtype tests on blueprints are faster, but 
                    // there are currently phase issues
		    if (t.isSubtypeOf(nativeInterfaceType)) {
			defineNativeInterface(t.asCompound());
			nativeInterfaces.set(bp.getUID());
			return true;
		    } else {
			nonNativeInterfaces.set(bp.getUID());
			return false;
		    }
		}
	    } catch (LinkageException e) { throw e.unchecked(); }
	}

	protected boolean isSystemClass(Type t) {
	    return t.getUnrefinedName() == interpreter;
	}
					   
	protected S3ByteCode.Builder fillInNativeMethod(S3Method method,
							ConstantPool cpBuilder) {
	    return null;
	}
    }
    /**
     * Link native method declarations in the standard library with
     * underlying Ovm implemenations.  GNU Classpath makes heavy use
     * of <code>native</code> methods, and provides implemenations of
     * these methods in JNI. These implementations do not fit well in
     * Ovm for several reasons:
     * <ul>
     * <li> Ovm implements core functionality such as user-level
     *      threading in Java, rather than C.
     * <li> Ovm does not, as yet, support JNI.
     * <li> Our existing foreign function interface,
     *      <code>INVOKE_NATIVE</code> is limited in that native
     *      methods cannot be called virtually, cannot be synchronized,
     *      and cannot interact with Java.
     * </ul>
     *
     * We could address these issues by making large-scale changes to
     * the library code, but such an approach would tie us to a
     * particular release of Classpath.  Instead, we link our
     * implementations of Classpath native methods through bytecode
     * rewriting.  {@link #fillInNativeMethod(S3Method,
     * ConstantPool)} constructs a bytecode body for every
     * native mehtod in the library.  This approach allows classpath
     * native methods to be virtual and synchronized.  It also allows
     * classpath native methods to be implemented in Java code.
     * <code>fillInNativeMethod</code> generates a body that forwards
     * all calls to the method chosen by {@link
     * #getNativeMethodImplementation(Method)}.  Further rewriting may
     * expand this call to <code>INVOKE_NATIVE</code>, a VM call, or
     * an ordinary static method call.<p>
     *
     * The <code>INVOKE_NATIVE</code> FFI is available only within classes
     * named <code>LibraryGlue</code>.
     **/
    public static class User extends NativeCallGenerator {
	private final boolean rewriteAppCode;
	private final HashSet seen = new HashSet();

	/**
	 * By default, <code>LibraryGlue</code> and
	 * <code>LibraryImports</code> classes only have special
	 * meaning within the system libraries.  Application code may
	 * not call C functions or the virtual machine methods
	 * directly.  We relax this rule when
	 * <code>rewriteAppCode</code> is true.
	 **/
	public User() {
	    rewriteAppCode =
		InvisibleStitcher.getBoolean("system-rewrite-application-code");
	}

	protected boolean allowNonFinal()      { return true; }
	protected boolean declaredNativeOnly() { return true; }
	protected boolean isSystemClass(Type _){ return false; }

	protected boolean isNativeClass(Type t) {
	    boolean ret =
		(t.asScalar().getName().getShortNameIndex() == LibraryGlue_INDEX
		 && (rewriteAppCode
		     || t.getContext() == t.getDomain().getSystemTypeContext()));
	    if (ret && !seen.contains(t)) {
		seen.add(t);
		defineNativeInterface(t.asCompound());
	    }
	    return ret;
	}

	private Descriptor.Method pushArg(Descriptor.Method orig, TypeName arg)
	{
	    ArrayList args = new ArrayList(orig.getArgumentCount() + 1);
	    args.add(arg);
	    for (int i = 0; i < orig.getArgumentCount(); i++)
		args.add(orig.getArgumentType(i));
	    return Descriptor.Method.make(args, orig.getType());
	}

	private Method findSelector(Type.Context ctx,
				    int pkgIndex,
				    int nameIndex,
				    UnboundSelector.Method sel) {
	    try {
		TypeName.Scalar tn = TypeName.Scalar.make(pkgIndex, nameIndex);
		Type.Class shSt = ctx.typeFor(tn).getSharedStateType();
		return shSt.getMethod(sel);
	    } catch (LinkageException _) {
		return null;
	    }
	}

	public boolean isNativeMethodHarmless(Method nat) {
	    return (super.isNativeMethodHarmless(nat)
		    && getNativeMethodImplementation(nat) == null);
	}

	public boolean isNativeMethodVMCall(Method nat) {
	    if (!rewriteAppCode) {
		Type.Context ctx = nat.getDeclaringType().getContext();
		if (ctx != ctx.getDomain().getSystemTypeContext())
		    return false;
	    }
	    return super.isNativeMethodVMCall(nat);
	}

	/**
	 * In the user-domain, we wish to forward calls to native
	 * methods within gnu-classpath to either LibraryGlue or
	 * LibraryImports.  Methods within LibraryImports are always
	 * declared native and represent calls into the VM, while
	 * methods declared native within LibaryGlue represent calls
	 * to ordinary C code, that does not interact with java
	 * objects.
	 * <p>
	 * Mapping a native instance method
	 * <i>p</i>.<i>C</i>.<i>m</i>(<i>arg, ...</i>), in classpath
	 * to a static method in one of our magic classes is fairly
	 * straightfoward.  We search for first:
	 * <ul>
	 * <li> <i>p</i>.LibraryGlue.<i>m</i>(<i>C</i>, <i>arg, ...</i>), then
	 * <li> <i>p</i>.LibraryImports.<i>m</i>(<i>C</i>, <i>arg, ...</i>)
	 * </ul>
	 * <p>
	 * The fact that the native method's receiver appears
	 * explicitly in the implemenation method's argument list
	 * makes the choice unambiguous.  In that case of static
	 * methods, things are a bit more involved, and we search for
	 * 5 possible equivalents in turn.
	 * <ol>
	 * <li> <i>p</i>.LibraryGlue.<i>C</i>_<i>m</i>(org.ovmj.java.Opaque, <i>arg, ...</i>)
	 * <li> <i>p</i>.LibraryGlue.<i>C</i>_<i>m</i>(<i>arg, ...</i>)
	 * <li> <i>p</i>.LibraryImports.<i>C</i>_<i>m</i>(<i>arg, ...</i>)
	 * <li> <i>p</i>.LibraryGlue.<i>m</i>(<i>arg, ...</i>)
	 * <li> <i>p</i>.LibraryImports.<i>m</i>(<i>arg, ...</i>)
	 * </ol>
	 * <b>NOTE</b>: The LibraryGlue equivalent of a static native
	 * method may take an extra dummy argument.
	 * <p>
	 * This all may be better described on
	 * <a href="http://www.ovmj.org/pipermail/ovm/2003-July/003625.html">
	 * the mailing list</a>.
	 *
	 **/
	public Method getNativeMethodImplementation(Method method) {
	    boolean isStatic = method.getMode().isStatic();
	    Selector.Method sel = method.getSelector();
	    UnboundSelector.Method usel = sel.getUnboundSelector();
	    Descriptor.Method origDesc = usel.getDescriptor();
	    TypeName.Compound defClass = method.getDeclaringType().getName();
	    int pkgIndex = defClass.getPackageNameIndex();
	    int clsIndex = defClass.getShortNameIndex();
	    Method newMethod = null;
	    Descriptor.Method desc = origDesc;

	    if ((clsIndex == LibraryImports_INDEX) || (clsIndex == LibraryGlue_INDEX)) return null;

	    Type.Context ctx = method.getDeclaringType().getContext();
	    Domain d = ctx.getDomain();
	    if (!rewriteAppCode && ctx != d.getSystemTypeContext()) return null;
	    usel = Transaction.the().translateTransactionalSelector(usel);
	    // We are given a method <pkg>.<class> <method>(...), and must find a
	    // replacement in <pkg>.LibraryGlue or <pkg>.LibraryImports
	    if (isStatic) {
		// We try several varients here
		Descriptor.Method newDesc = pushArg(desc, OPAQUE_TYPE_NAME);
		String base = UTF8Store._.getUtf8(usel.getNameIndex()).toString();
		String prefix = UTF8Store._.getUtf8(defClass.getShortNameIndex()).toString();
		int name = RepositoryUtils.asUTF(prefix + "_" + base);
		UnboundSelector.Method renamed = UnboundSelector.Method.make(name, newDesc);

		// LibraryGlue.<class>_<method>(Opaque, ...)
		newMethod = findSelector(ctx, pkgIndex, LibraryGlue_INDEX, renamed);

		renamed = UnboundSelector.Method.make(name, desc);
		if (newMethod == null) 	// LibraryGlue.<class>_<method>(...)
		    newMethod = findSelector(ctx, pkgIndex, LibraryGlue_INDEX, renamed);
		if (newMethod == null)	// LibraryImports.<class>_<method>(...)
		    newMethod = findSelector(ctx, pkgIndex, LibraryImports_INDEX, renamed);
	    } else {
		// The receiver is always passed explicity, so prepend
		// <class> to the argument list ((...))
		desc = pushArg(desc, sel.getDefiningClass());
		usel = UnboundSelector.Method.make(usel.getNameIndex(), desc);
	    }
	    if (newMethod == null)    // LibraryGlue.<method>(...)
		newMethod = findSelector(ctx, pkgIndex, LibraryGlue_INDEX, usel);
	    if (newMethod == null)    // LibraryImports.<method>(...)
		newMethod = findSelector(ctx, pkgIndex, LibraryImports_INDEX, usel);
	    return newMethod;
	}

	protected S3ByteCode.Builder fillInNativeMethod(S3Method method, ConstantPool cpBuilder) {
	    Descriptor.Method origDesc = method.getSelector().getDescriptor();
	    int maxStack = origDesc.getArgumentLength()/4 + 1;
	    Method newMethod = getNativeMethodImplementation(method);
	    S3ByteCode emptyByteCodeFragment =
		new S3ByteCode.Builder(method,
				       new byte[1], //code
				       (char) maxStack,
				       (char) 0, // maxLocals
				       new ExceptionHandler[0],
				       new Attribute[0],
				       method.getMode().isSynchronized())
		    .build();
	    CodeFragmentEditor editor = new CodeFragmentEditor(emptyByteCodeFragment, cpBuilder);
	    editor.removeInstruction(0);
	    Cursor c = editor.getCursorBeforeMarker(0);
	    boolean isStatic = method.getMode().isStatic();
	    if (newMethod != null) {
		Descriptor.Method newDesc = newMethod.getSelector().getDescriptor();
		int firstPush;
		int argOff;
		if (isStatic) {
		    if (origDesc.getArgumentCount() < newDesc.getArgumentCount())
		    // push dummy first argument
		    firstPush = ACONST_NULL;
		    else firstPush = NOP;
		    // locals have not been shifted
		    argOff = 0;
		} else {
		    // push implicit this
		    firstPush = ALOAD_0;
		    argOff = 1;
		}
		d(BootBase.LOG_NATIVE, method.getSelector(), " => ", newMethod.getSelector());
		if (firstPush != NOP) c.addSimpleInstruction(firstPush);
		for (int i = 0; i < origDesc.getArgumentCount(); i++) {
		    char tag = origDesc.getArgumentType(i).getTypeTag();
		    switch (tag) {
		    case TypeCodes.OBJECT:
		    case TypeCodes.GEMEINSAM:
		    case TypeCodes.ARRAY:
		    case TypeCodes.THENULL:
			c.addALoad((char) argOff++);
			break;
		    case TypeCodes.INT:
		    case TypeCodes.SHORT:
		    case TypeCodes.CHAR:
		    case TypeCodes.BYTE:
		    case TypeCodes.BOOLEAN:
			c.addILoad((char) argOff++);
			break;
		    case TypeCodes.FLOAT:
			c.addFLoad((char) argOff++);
			break;
		    case TypeCodes.LONG:
			c.addLLoad((char) argOff);
			argOff += 2;
			break;
		    case TypeCodes.DOUBLE:
			c.addDLoad((char) argOff);
			argOff += 2;
			break;
		    default:
			throw new RuntimeException("weird type tag " + tag);
		    }
		}
		c.addINVOKESTATIC(newMethod.getSelector());
		char tag = newDesc.getType().getTypeTag();
		switch (tag) {
		case TypeCodes.OBJECT:
		case TypeCodes.GEMEINSAM:
		case TypeCodes.ARRAY:
		case TypeCodes.THENULL:
		    c.addSimpleInstruction(ARETURN);
		    break;
		case TypeCodes.INT:
		case TypeCodes.SHORT:
		case TypeCodes.CHAR:
		case TypeCodes.BYTE:
		case TypeCodes.BOOLEAN:
		    c.addSimpleInstruction(IRETURN);
		    break;
		case TypeCodes.FLOAT:
		    c.addSimpleInstruction(FRETURN);
		    break;
		case TypeCodes.LONG:
		    c.addSimpleInstruction(LRETURN);
		    argOff += 2;
		    break;
		case TypeCodes.DOUBLE:
		    c.addSimpleInstruction(DRETURN);
		    argOff += 2;
		    break;
		case TypeCodes.VOID:
		    c.addSimpleInstruction(RETURN);
		    break;
		default:
		    throw new RuntimeException("weird type tag " + tag);
		}
	    } else {
		d(BootBase.LOG_MISSING_NATIVE, method.getSelector(), " => LinkageError");
		c.addNEW(JavaNames.java_lang_UnsatisfiedLinkError);
		c.addSimpleInstruction(DUP);
		String msg = "unimplemented native method ("+ method.getSelector()+")";
		c.addLoadConstant(new RepositoryString(msg));
		c.addINVOKESPECIAL(ULE_CTOR);
		c.addSimpleInstruction(ATHROW);
	    }
	    char nlocals = (char) ((origDesc.getArgumentLength() + (isStatic ? 0 : 4)) / 4);
	    S3ByteCode.Builder ret = (S3ByteCode.Builder)
	        editor.commit(new S3ByteCode.Builder(method), nlocals);
	    ret.setMaxStack((char) maxStack);
	    return ret;
	}
    }
	
} // end of NativeCallGenerator
