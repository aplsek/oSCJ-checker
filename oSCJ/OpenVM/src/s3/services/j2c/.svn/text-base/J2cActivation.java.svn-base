package s3.services.j2c;

import ovm.core.domain.Code;
import ovm.core.domain.Oop;
import ovm.core.execution.Context;
import ovm.core.execution.Activation;
import ovm.core.execution.NativeInterface;
import ovm.core.services.memory.VM_Address;
import s3.services.transactions.PragmaPARSafe;
import s3.util.PragmaNoInline;

public class J2cActivation extends Activation {
    static final class Native implements NativeInterface {
	/* architecture-specific library routines */
	native static VM_Address getCallerPC(VM_Address a) throws PragmaPARSafe;

	native static VM_Address getCurrentActivation(int skipCount) throws PragmaPARSafe;
	
	native static VM_Address getCaller(VM_Address a) throws PragmaPARSafe;

	/* j2c-specific support routines */
	native static VM_Address j2c_getCode(VM_Address absPC) throws PragmaPARSafe;

	static native VM_Address j2c_topFrame(int nc);

	static native VM_Address j2c_bottomFrame(int nc) throws PragmaPARSafe;

	static native void j2c_throw(Oop throwable);
    }
    int nc;
    VM_Address a;
    VM_Address callee;
    VM_Address absPC;
    J2cCodeFragment code;

    public int getNativeHandle() { return a.asInt(); }

    public Code getCode() {
	// FIXME: I should probably use J2cCodeFragment.fromPC() instead
	// of Native.j2c_getCode(), because an inlined binary search
	// has to be faster than calling bsearch().  However, the
	// latter is tested and the former isn't.
	return code;
    }

    public int getPC() {
	return absPC.asInt();
    }

    protected void setToCurrent(int nc) throws PragmaNoInline {
	this.nc = nc;
	if (Context.getCurrentContext().nativeContextHandle_ == nc) {
	    // skip invoke_native()
	    this.a = Native.getCurrentActivation(1);
	    // find caller's PC
	    caller(this);
	} else {
	    callee = null;
	    code = null;
	    absPC = VM_Address.fromObject(null);
	    a = Native.j2c_topFrame(nc);
	    if (a != null)
		// we currently point to run(), find the top java
		// activation
		caller(this);
	}
    }
    
    public Activation caller(Activation _act) {
	J2cActivation act = (J2cActivation) _act;
	VM_Address bottom = Native.j2c_bottomFrame(nc);
	act.nc = this.nc;
	act.a = this.a;
	VM_Address _code = VM_Address.fromObject(null);
	while (_code == null && act.a != bottom) {
	    act.callee = act.a;
	    act.absPC = Native.getCallerPC(act.a);
	    act.a = Native.getCaller(act.a);
	    _code = Native.j2c_getCode(act.absPC);
	}

	if (act.a == bottom)
	    return null;
	act.code = (J2cCodeFragment) _code.asAnyObject();
	return act;
    }

    public void processThrowable(Oop throwable) {
    
        // the following comment also applies to CEXCEPTIONS
        
	// this almost doesn't work for the no-cpp-exceptions case.  what
	// happens is that the j2c_throw() function returns normally but
	// with an exception set, and it is only when this method
	// (processThrowable()) returns that the exception handling
	// machinery kicks in.  but right now that is perfectly fine, since
	// this method returns after Native.j2c_throw() - so it is irrelevant
	// whether the exception handling machinery is active here or not.
	
	// so watch out: if you put code after the following method call,
	// the effect will be that this code will execute until the first
	// method return, at which point it will start to behave like the
	// exception was thrown.  of course, you'd have to be crazy to put
	// code after this method call, since if that code did execute
	// normally, then it would imply that something was terribly broken.

	Native.j2c_throw(throwable);
    }

    public static class Factory extends Activation.Factory {
	public Activation make() { return new J2cActivation(); }
    }
}
