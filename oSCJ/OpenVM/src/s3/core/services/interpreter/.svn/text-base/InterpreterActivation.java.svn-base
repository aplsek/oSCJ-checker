package s3.core.services.interpreter;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.Oop;
import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.ExceptionTableActivation;
import ovm.core.execution.NativeInterface;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.JVMConstants.InvokeSystemArguments;
import s3.util.PragmaNoInline;
import s3.util.PragmaTransformCallsiteIR;
import s3.core.domain.S3ByteCode;

public class InterpreterActivation extends ExceptionTableActivation {
    static final class Native implements NativeInterface {
	static native VM_Address interp_getLocalAddress(int ctx, int f,
							char index);
	static native VM_Address interp_getOperandAddress(int ctx, int f,
							  char index);
    
	static native void interp_setOperandReference(int ctx, int f,
							    char index,
							    boolean isRef);
    
	native static void interp_setOpstackDepth(int ctx,
						  int f,
						  int newDepth);

	/** Sets the ABSOLUTE pc (not relative to the code fragment).  **/
	native static void interp_setPC(int f, VM_Address pc);    

	/** Gets the ABSOLUTE pc (not relative to the code fragment).  **/
	native static VM_Address interp_getPC(int f);

	native static VM_Address interp_getCode(int f);

	native static int interp_topActivation(int nc);

	native static int interp_getCaller(int nc, int f);

    }
    // FIXME: this should be a standard native call
    static native void interp_cutTo(int nc, int f) throws BCSys;
    
    static class BCSys extends PragmaTransformCallsiteIR
	implements InvokeSystemArguments
    {
	static {
	    register(BCSys.class.getName(),
		     new byte[] { (byte) INVOKE_SYSTEM,
				  (byte) CUT_TO_ACTIVATION,
				  0 });
	}
    }
				  
    int nc;
    int f;

    public int getNativeHandle() { return f; }

    public Code getCode() {
	return (Code) Native.interp_getCode(f).asOop();
    }
    public int getPC() {
	byte[] codeb = ((S3ByteCode) getCode()).getBytes();
	VM_Address code = VM_Address.fromObject(codeb);
	Blueprint.Array bp = (Blueprint.Array)code.asOop().getBlueprint();
	    
	return Native.interp_getPC( f)
	    .diff( code).sub( bp.getUnpaddedFixedSize()).asInt();
    }
    protected void setToCurrent(int nc) throws PragmaNoInline {
	this.nc = nc;
	f = Native.interp_topActivation(nc);
	if (Context.getCurrentContext().nativeContextHandle_ == nc)
	    caller(this);
    }
    public Activation caller(Activation _act) {
	InterpreterActivation act = (InterpreterActivation) _act;
	act.nc = nc;
	act.f = Native.interp_getCaller(nc, f);
	return act.f == 0 ? null : act;
    }
    public Oop getLocal(int idx) {
	VM_Address addr = Native.interp_getLocalAddress(nc, f, (char) idx);
	return addr.getAddress().asOop();
    }
    public void setOperand(int idx, Oop value) {
	VM_Address addr = Native.interp_getOperandAddress(nc, f, (char) idx);
	addr.setAddress(VM_Address.fromObject(value));
	Native.interp_setOperandReference(nc, f, (char) idx, true);
    }
    public void setOperandStackDepth(int depth) {
        Native.interp_setOpstackDepth(nc, f, depth);
    }
    public void setPC(int pc) {
	byte[] codeb = ((S3ByteCode) getCode()).getBytes();
        if (codeb == null)
            BasicIO.err.println("NULL code!");

        VM_Address addr = VM_Address.fromObject(codeb);
        Blueprint.Array bp = (Blueprint.Array)addr.asOop().getBlueprint();
        addr = addr.add( pc + bp.getUnpaddedFixedSize());
        Native.interp_setPC(f, addr);
    }
    public void cutTo() {
	/*Native.*/interp_cutTo(nc, f);
    }

    public static class Factory extends Activation.Factory {
	public Activation make() { return new InterpreterActivation(); }
    }
}
