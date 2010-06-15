package s3.services.memory.conservative;

import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.NativeInterface;
import ovm.core.execution.Native;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import ovm.util.Iterator;
import ovm.core.services.memory.LocalReferenceIterator;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.core.execution.Context;
import ovm.core.Executive;
import s3.util.PragmaNoInline;

public class ConservativeLocalReferenceIterator extends LocalReferenceIterator {
    private static final boolean DEBUG=false;

    public static final class NativeHelpers implements NativeInterface {
	static native VM_Address getOVMContextLoc(int nativeContext);
	static native VM_Address getEngineSpecificPtrLoc(int nativeContext);
	// bottom frame, highest address
	static native VM_Address getStackBase(int nativeContext);
	// top frame, lowest address
	static native VM_Address getStackTop(int nativeContext);

	// These methods are used in combination with getStackTop to
	// walk non-current threads.
	static native VM_Address getSavedRegisters(int nativeContext);
	static native int getSavedRegisterSize();

	// This method is called on the garbageCollect() frame to walk
	// the current thread.  It relies on garbageCollect() using
	// all call-preserved registers.
	static native VM_Address callerLocalsEnd(VM_Address frameHandle);
	
	static native boolean stackGrowsUp();
    }

    public int supportedModes() { return PRECISE | CONSERVATIVE; }

    public boolean expectsEngineSupport() {
	return false;
    }

    Context ctx;
    Context curCtx;
    
    // for precise walking
    int idx; // 0 = context, 1 = engine-specific, 2 = done

    // for conservative walking
    int nctx;
    boolean swap;
    VM_Address nact;
    boolean onRegs;
    VM_Address start, end;
    VM_Address stackStart, stackEnd;

    protected boolean moreReferencesInThread() {
	if (conservative()) {
	    if (DEBUG) {
		Native.print_string("moreReferencesInThread(), conservative: onRegs = ");
		Native.print_string(onRegs?"yes":"no");
		Native.print_string(", stackStart = ");
		Native.print_ptr(stackStart);
		Native.print_string(", stackEnd = ");
		Native.print_ptr(stackEnd);
		Native.print_string(", start = ");
		Native.print_ptr(start);
		Native.print_string(", end = ");
		Native.print_ptr(end);
		Native.print_string("\n");
	    }
	    return (onRegs && stackStart.uLT(stackEnd)) || start.uLT(end);
	} else {
	    if (DEBUG) {
		Native.print_string("moreReferencesInThread(), precise: idx = ");
		Native.print_int(idx);
		Native.print_string(", engine specific ptr loc = ");
		Native.print_ptr(NativeHelpers.getEngineSpecificPtrLoc(ctx.nativeContextHandle_));
		Native.print_string("\n");
	    }
	    return idx<1 || (idx<2 &&
			     NativeHelpers.getEngineSpecificPtrLoc(ctx.nativeContextHandle_)
			     !=VM_Address.fromObject(null));
	}
    }
    protected VM_Address nextReferenceInThread() {
	VM_Address result;
	if (precise()) {
	    if (idx==0) {
		result=NativeHelpers.getOVMContextLoc(ctx.nativeContextHandle_);
	    } else {
		result=NativeHelpers.getEngineSpecificPtrLoc(ctx.nativeContextHandle_);
	    }
	    idx++;
	} else {
	    if (start.uGE(end)) {
		// onRegs must be true if we get here
		onRegs=false;
		start=stackStart;
		end=stackEnd;
	    }
	    result=start;
	    start=start.add(VM_Word.widthInBytes());
	}
	return result;
    }
    protected void setThread(Context ctx) {
	this.ctx=ctx;
	if (conservative()) {
	    nctx=ctx.nativeContextHandle_;
	    if (ctx!=curCtx) {
		if (DEBUG) Native.print_string("not our context case.\n");
		start=NativeHelpers.getSavedRegisters(nctx);
		end=start.add(NativeHelpers.getSavedRegisterSize());
		stackStart=NativeHelpers.getStackTop(nctx);
		stackEnd=NativeHelpers.getStackBase(nctx);
		if (swap) {
		    VM_Address tmp=stackEnd;
		    stackEnd=stackStart;
		    stackStart=tmp;
		}
		onRegs=true;
	    } else {
		start=NativeHelpers.callerLocalsEnd(nact);
		end=NativeHelpers.getStackBase(nctx);
		if (DEBUG) {
		    Native.print_string("start = ");
		    Native.print_ptr(start);
		    Native.print_string(", end = ");
		    Native.print_ptr(end);
		    Native.print_string("\n");
		}
		if (swap) {
		    if (DEBUG) Native.print_string("swapping!\n");
		    VM_Address tmp=end;
		    end=start;
		    start=tmp;
		}
	    }
	} else {
	    idx=0;
	}
    }
    
    private static void printActivation(Activation act) {
	Native.print_string("activation: ");
	Native.print_ptr(VM_Address.fromInt(act.getNativeHandle()));
	Native.print_string("\n");
    }
    
    protected void prepareForGCHook() throws PragmaNoInline {
	super.prepareForGCHook();
	curCtx=Context.getCurrentContext();
	Activation act=curCtx.getCurrentActivation();
	if (DEBUG) printActivation(act);
	act.caller(act); // get to prepareForGC
	if (DEBUG) printActivation(act);
	nact=VM_Address.fromInt(act.getNativeHandle());
	if (DEBUG) {
	    Native.print_string("caller locals end = ");
	    Native.print_ptr(NativeHelpers.callerLocalsEnd(nact));
	    Native.print_string("\n");
	}
	swap=NativeHelpers.stackGrowsUp();
    }

    /**
     * NOTE: SAVE_REGISTERS can do strange things with gcc on PowerPC
     * macs.  In particular, it can make the use of local variables
     * impossible when optimization is turned on.  The use of ovm_this
     * below is OK, because ovm_this will come from r3, but if the
     * MemoryManager object where passed as a parameter, it would be
     * read from an uninitalized call-preserving register (r30)
     * following the call to prepareForGCHook.
     **/
    public void prepareForGC() throws PragmaNoInline {
	MemoryManager.GCNative.SAVE_REGISTERS();
	prepareForGCHook();
	MemoryManager.the().doGC();
    }
}


