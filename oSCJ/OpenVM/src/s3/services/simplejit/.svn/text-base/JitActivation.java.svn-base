package s3.services.simplejit;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.Oop;
import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.ExceptionTableActivation;
import ovm.core.execution.NativeInterface;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.VM_Address;
import s3.util.PragmaNoInline;

public class JitActivation extends ExceptionTableActivation {
    static final class Native implements NativeInterface {
	/* architecture-specific library routines */
	native static void setCallerPC(VM_Address a, VM_Address pc);

	/** Gets the ABSOLUTE pc (not relative to the code fragment).  **/
	// Warning: this PC actually points to the next instruction to
	// execute in SimpleJIT
	native static VM_Address getCallerPC(VM_Address a);

	native static VM_Address getCaller(VM_Address a);

	native static VM_Address getCurrentActivation(int skipCount);

	/* simplejit-specific support routines */
	static native VM_Address jit_getLocalAddress(int ctx,
						     VM_Address act,
						     char index);
	static native VM_Address jit_getOperandAddress(int ctx,
						       VM_Address act,
						       char index);
    
	native static void jit_setOpstackDepth(int ctx,
					       VM_Address act,
					       int newDepth);

	native static VM_Address jit_getCode(VM_Address a);

	static native VM_Address jit_topFrame(int nc);

	static native VM_Address jit_bottomFrame(int nc);

	static native void jit_cutToActivation(int nc, VM_Address callee);
    }
    int nc;
    VM_Address f;
    VM_Address callee;
    VM_Address absPC;

    public int getNativeHandle() { return f.asInt(); }

    public Code getCode() {
	return (Code) Native.jit_getCode(f).asAnyObject();
    }

    public int getPC() {
	Code _code = getCode();
	if (_code instanceof SimpleJITCode) {
	    SimpleJITCode code = (SimpleJITCode) _code;
	    return absPC.diff(code.getCodeEntry()).asInt();
	} else {
	    // The compiler trampoline frame will be for S3ByteCode.
	    // In the future, this will not show up as an Activation
	    // at all, but for now, we just try to ignore it.
	    return -1;
	}
    }

    protected void setToCurrent(int nc) throws PragmaNoInline {
	this.nc = nc;
	if (Context.getCurrentContext().nativeContextHandle_ == nc) {
	    // skip invoke_native()
	    this.f = Native.getCurrentActivation(1);
	    caller(this);
	} else {
	    // jit_topActivation returns ovmrt_run()
	    this.f = Native.jit_topFrame(nc);
	    caller(this);
	}
    }
    
    public Activation caller(Activation _act) {
	JitActivation act = (JitActivation) _act;
	act.callee = this.f;
	act.absPC = Native.getCallerPC(f);
	act.f = Native.getCaller(f);
	if (act.f == Native.jit_bottomFrame(nc))
	    return null;
	return act;
    }

    public Oop getLocal(int idx) {
	VM_Address addr = Native.jit_getLocalAddress(nc, f, (char) idx);
	return addr.getAddress().asOop();
    }

    public void setOperand(int idx, Oop value) {
	VM_Address addr = Native.jit_getOperandAddress(nc, f, (char) idx);
	addr.setAddress(VM_Address.fromObject(value));
    }

    public void setOperandStackDepth(int depth) {
        Native.jit_setOpstackDepth(nc, f, depth);
    }

    public void setPC(int pc) {
	SimpleJITCode code = (SimpleJITCode) getCode();
	absPC = code.getCodeEntry().add(pc);
	Native.setCallerPC(callee, absPC);
    }

    public void cutTo() {
	Native.jit_cutToActivation(nc, callee);
    }

    public static class Factory extends Activation.Factory {
	public Activation make() { return new JitActivation(); }
    }
}
