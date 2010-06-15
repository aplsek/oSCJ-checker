package s3.services.simplejit;

import ovm.core.execution.Trampoline;
import ovm.core.services.memory.VM_Address;
import ovm.core.execution.Activation;
import s3.core.domain.S3ByteCode;
import ovm.core.Executive;
import ovm.core.domain.Code;
import ovm.core.execution.NativeConstants;
import ovm.core.repository.TypeCodes;
import ovm.util.ByteBuffer;
import ovm.util.FixedByteBuffer;
import s3.services.simplejit.CodeGenerator.Precomputed;
import s3.services.simplejit.x86.X86Trampoline;
import s3.services.simplejit.powerpc.PPCTrampoline;

public class SimpleJITTrampoline extends Trampoline {
    // 123 words should be enough for the compileAndRestart
    // trampoline.  A larger buffer, and multiple buffers,  will be
    // needed for mixed-mode execution
    private static final int TRAMPOLINE_BUF_SIZE = 492;

    /**
     * A properly typed reference to the simplejit tramponline
     * implementation.
     **/
    static SimpleJITTrampoline _;

    public SimpleJITTrampoline() {
	assert(_ == null);
	_ = this;
    }

    private Impl impl;

    public static void init(SimpleJITCompilerVMInterface vmInterface) {
	if (the() != _) {
	    System.out.println("warning: simplejit trampolines not configured");
	    return;
	}
	if (NativeConstants.OVM_X86)
	    _.impl = new X86Trampoline();
	else if (NativeConstants.OVM_PPC)
	    _.impl = new PPCTrampoline();
	else
	    throw new Error("Unsupported architecture");

	byte[] bytes = new byte[TRAMPOLINE_BUF_SIZE];
	_.impl.codeBuf = (FixedByteBuffer) ByteBuffer.wrap(bytes);
    	_.impl.codeBuf.order(NativeConstants.BYTE_ORDER);    	
	_.impl.precomputed = new Precomputed(vmInterface);
    }
	
    public static abstract class Impl implements TypeCodes {
	protected FixedByteBuffer codeBuf;
	protected Precomputed precomputed;

	public abstract VM_Address compileAndRestart(Code compileMethod,
						     Object compileRecv);
    }

    /**
     * A thunk that unwinds the stack to a given frame pointer,
     * restores registers, and makes a tail call to
     * Activation.rethrow.  This thunk is installed on the call stack
     * when an exception propagates out of a simplejit activation
     * record.
     **/
    public interface RethrowThunk {
	VM_Address getAddress();
	void setOperands(VM_Address rethrowFramePointer,
			 Activation rethrowActivation);
    }

    /**
     * A thunk that unwinds the stack to a given frame pointer, and
     * jumps to a given PC.  This thunk is used to unwind to a
     * simplejit activation that handles an exception.
     *
     * The stackPointer and programCounter arguments are the
     **/
    public interface UnwindThunk {
	VM_Address getAddress();
	void setOperands(VM_Address framePointer,
			 VM_Address stackPointer,
			 VM_Address programCounter);
    }

    private Code compileCode;
    private Object compileRecv;
    private VM_Address compileAndRestart;
    
    public VM_Address compileAndRestart(S3ByteCode _,
					Code compileCode,
					Object compileRecv) {
	if (this.compileCode != compileCode
	    || this.compileRecv != compileRecv)	{
	    // The trampoline we generate does not depend on the
	    // method to be compiled, so we cache the code for a given
	    // method and receiver object.  (Currently, these are
	    // shared by all compileAndRestart trampolines.)
	    compileAndRestart = impl.compileAndRestart(compileCode,
						       compileRecv);
	    this.compileCode = compileCode;
	    this.compileRecv = compileRecv;
	}
	return compileAndRestart;
    }
}
