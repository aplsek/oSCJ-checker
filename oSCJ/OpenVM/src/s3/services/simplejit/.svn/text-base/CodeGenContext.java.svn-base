package s3.services.simplejit;


import ovm.core.repository.ExceptionHandler;
import ovm.core.services.memory.MemoryPolicy;
import ovm.util.ByteBuffer;
import ovm.util.FixedByteBuffer;
import ovm.core.domain.Type;
import ovm.core.execution.NativeConstants;
import ovm.util.HTString2int;
import s3.core.domain.S3ByteCode;
import s3.services.simplejit.Assembler.Branch;
import ovm.core.services.memory.MemoryManager;
import ovm.core.domain.ObjectModel;
import ovm.core.repository.Selector;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.VM_Address;
import ovm.core.domain.Oop;
import ovm.core.domain.Blueprint;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.services.bootimage.GC;
import ovm.core.OVMBase;

/**
 * @author Hiroshi Yamauchi
 **/
public abstract class CodeGenContext  {
    /**
     * A big byte buffer that contains compiled code for many methods.
     * Machine code is allocated in large increments, and code is
     * added to the global buffer until it fills up.  When this
     * happens, {@link ovm.util.ByteBuffer.BufferOverflowException} is
     * thrown, and the method must be recompiled after first calling
     * {@link #resetGlobalCodeArray}.
     * <p>
     * Because this never moves and never grows, we know the absolute
     * address of code as it is being generated.
     **/
    public static FixedByteBuffer globalCodeArray;
    public static byte[] globalCodeBytes;

    /**
     * The size of each globalCodeArray buffer.  If a single method
     * exceeds this size, we should double it.  But, if a single
     * method exceeds 4M in size, something is terribly wrong.
     **/
    private static final int CODE_ARRAY_SIZE = 4 * 1024 * 1024;

    public void expandGlobalCodeArray(Selector sel) {
	if (globalCodeArrayOffset < globalCodeBytes.length/2) {
	    int newSz = globalCodeBytes.length;
	    newSz += ObjectModel.getObjectModel().headerSkipBytes() + 4;
	    newSz = 2*newSz - (ObjectModel.getObjectModel().headerSkipBytes() + 4);
	    BasicIO.out.println("Growing code array for " + sel + ": "
				+ (globalCodeBytes.length
				   - globalCodeArrayOffset)
				+ " not enough, growing to " + newSz);
	    resetGlobalCodeArray(newSz);
	} else {
	    resetGlobalCodeArray(globalCodeBytes.length);
	}
    }

    // Is it safe to put this dead code into resetGlobalCodeArray?
    // Probably best to keep it here, where it will be ignored
    // outright.
    static void buildTimePin(Object o) throws BCdead {
	GC.the().addRoot(o);
    }

    public static void resetGlobalCodeArray(int sz) {
	if (sz == 0)
	    sz = (CODE_ARRAY_SIZE
		  - (ObjectModel.getObjectModel().headerSkipBytes() + 4));

	globalCodeBytes = new byte[sz];
	// It always makes sense to pin these arrays, we are only
	// going to hold references to array elements, not the array
	// itself.
	if (OVMBase.isBuildTime())
	    buildTimePin(globalCodeBytes);
	else
	    MemoryManager.the().pin(globalCodeBytes);
	globalCodeArray = (FixedByteBuffer) ByteBuffer.wrap(globalCodeBytes);
    	globalCodeArray.order(NativeConstants.BYTE_ORDER);    	
    }

    // Entry point relative to &globalCodeArray[0]
    int globalCodeArrayOffset;

    /**
     * A mapping from a bytecode pc to its corresponding native pc
     **/
    private int[] bytecodePC2NativePC;

    /**
     * An array of native exception handlers
     **/
    private ExceptionHandler[] nativeExceptionHandlers;

    /**
     * Relative/absolute/call code linker
     **/
    protected CodeLinker codeLinker;

    private int bytecode_len;
    
    private Type.Context tcontext;

    public CodeGenContext(int bytecode_len, Type.Context tcontext) {
	if (globalCodeArray == null) {
	    resetGlobalCodeArray(0);
	}
	this.bytecode_len = bytecode_len;
	globalCodeArrayOffset = globalCodeArray.position();
	bytecodePC2NativePC = new int[bytecode_len];
	codeLinker = makeCodeLinker(bytecodePC2NativePC);
	this.tcontext = tcontext;
    }

    public int[] getBytecodePC2NativePC() {
	return bytecodePC2NativePC;
    }

    protected abstract CodeLinker makeCodeLinker(int[] _bytecodePC2NativePC);

    public ExceptionHandler[] 
	getNativeExceptionHandlers(S3ByteCode bytecode) {
	if (nativeExceptionHandlers != null)
	    return nativeExceptionHandlers;

	//Object r = MemoryPolicy.the().enterMetaDataArea(tcontext);
	//try {
	ExceptionHandler[] bceh = bytecode.getExceptionHandlers();
	nativeExceptionHandlers = new ExceptionHandler[bceh.length];
	for(int i = 0; i < bceh.length; i++) {
	    nativeExceptionHandlers[i] =
		new ExceptionHandler(bytecodePC2NativePC[bceh[i].getStartPC()],
				     bytecodePC2NativePC[bceh[i].getEndPC()],
				     bytecodePC2NativePC[bceh[i].getHandlerPC()],
				     bceh[i].getCatchTypeName());
	}
	//} finally { MemoryPolicy.the().leave(r); }
	return nativeExceptionHandlers;
    }

    public Assembler getAssembler() {
	throw new Error();
    }

    public FixedByteBuffer getCodeBuffer() {
	return globalCodeArray;
    }

    public CodeLinker getCodeLinker() {
	return codeLinker;
    }

    /**
     * Return the native PC of the next instruction we emit.  The
     * native PC is defined as a signed (but always positive) distance
     * in bytes from the method's entry point.  These PC
     * representations can be converted to actual pointers to code
     * using {@link #absoluteNativePC}.
     **/
    public int getNativePC() {
	return globalCodeArray.position() - globalCodeArrayOffset;
    }

    /**
     * Translate a native PC, which is relative to the beginning of
     * the method, into an absolute virtual address.
     **/
    public VM_Address absoluteNativePC(int relPC) {
	Oop arrOop = VM_Address.fromObject(globalCodeBytes).asOop();
	Blueprint.Array bp = (Blueprint.Array) arrOop.getBlueprint();
	return bp.addressOfElement(arrOop, globalCodeArrayOffset + relPC);
    }

    /**
     * Return the virtual address of this method's entry point.
     **/
    public VM_Address getCodeEntry() {
	return absoluteNativePC(0);
    }

    /**
     * @return native pc if the native pc is set yet,  otherwise -1.
     **/
    public int getBytecodePC2NativePC(int bpc) {
	assert 0 <= bpc && bpc < bytecodePC2NativePC.length:
	       "Context.getBytecodePC2NativePC";
	       
	return bytecodePC2NativePC[bpc];
    }
    public void setBytecodePC2NativePC(int bpc, int npc) {
	assert 0 <= bpc && bpc < bytecode_len:
	       "Context.getBytecodePC2NativePC, bytecode pc";
       assert (0 <= npc  &&
	       // <= because we may be forced to reallocate on the
	       // next hardware instruction emitted
	       (npc + globalCodeArrayOffset) <= globalCodeArray.capacity()):
	      "Context.getBytecodePC2NativePC, native pc";
	bytecodePC2NativePC[bpc] = npc;
    }

    public void addRelativeJumpPatch(Branch branch, int targetBytecodePC) {
        codeLinker.registerRelativeJump(branch.sourcePC, branch.shift, branch.bits, targetBytecodePC);
    }

    public void addRelativeJumpPatch(int branchPC,
				     int targetBytecodePC) {
	codeLinker.registerRelativeJump(branchPC,
					targetBytecodePC);
    }

    public void linkRelativeJumps() {
	codeLinker.linkRelativeJumps(globalCodeArray, globalCodeArrayOffset);
    }
}
