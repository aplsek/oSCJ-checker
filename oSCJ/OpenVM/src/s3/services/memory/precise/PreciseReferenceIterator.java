package s3.services.memory.precise;

import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.Interpreter;
import ovm.core.execution.NativeInterface;
import ovm.core.execution.Native;
import ovm.core.execution.Processor;
import ovm.core.services.memory.VM_Address;
import ovm.util.Iterator;
import ovm.core.services.memory.LocalReferenceIterator;

public class PreciseReferenceIterator extends LocalReferenceIterator {
    private static final class NativeHelper implements NativeInterface {
	static native VM_Address getAuxStackTop(int nc);
	static native VM_Address getAuxStackBottom(int nc);
	static native boolean    localStateIsRunning(int nc);
	static native void       walkStack(int nc);
	static native int	 getAuxHeaderSize();
	static native VM_Address getOVMContextLoc(int nc);
	static native VM_Address getEngineSpecificPtrLoc(int nc);
    }

    static String flavor;
    
    static public String getFlavor() { return flavor; }

    static public boolean canHandleExceptions() {
	return flavor == "COUNTER";
    }
    /**
     * If true, a method containing non-empty safepoints cannot be inlined.
     **/
    static public boolean noInlineWithSafePoints() {
	return flavor != "COUNTER";
    }
    /**
     * If true, CATCHRESTORE is used, and the list of variables saved
     * at every safepoint must extend the list restored at every
     * reachable catchpoint.
     **/
    static public boolean catchPointsUsed() {
	return flavor != "THUNK";
    }
    
    Context walkedThread;

    // are we looking at the ovm context pointer?
    boolean atCtxPtr;

    // is the thread running? (cached so we don't have to make too
    // many native calls)
    boolean isRunning;

    // for threads that are running
    VM_Address end;
    VM_Address referencePtr;
    int referencesInFrame;

    // for threads that are not yet running
    boolean atEnginePtr;

    int frameHeaderSize;
    
    private static final boolean DEBUG=false;
    
    protected void aboutToWalkHook() {
	walkedThread=null;
	atCtxPtr=false;
	isRunning=false;
	end=null;
	referencePtr=null;
	referencesInFrame=0;
	atEnginePtr=false;
	frameHeaderSize=0;
    }
    
    protected boolean moreReferencesInThread() {
	if (atCtxPtr) {
	    return true;
	} else if (isRunning) {
	    return referencePtr != end;
	} else {
	    return atEnginePtr
		&& (NativeHelper.getEngineSpecificPtrLoc(walkedThread.nativeContextHandle_)
		    != VM_Address.fromObject(null));
	}
    }

    protected VM_Address nextReferenceInThread() {
	if (atCtxPtr) {
	    atCtxPtr=false;
	    if (DEBUG) Native.print("Returning contextPtrPtr\n");
	    return NativeHelper.getOVMContextLoc(walkedThread.nativeContextHandle_);
	} else if (isRunning) {
	    if (DEBUG) {
		Native.print_string("Returning another pointer with nptrs left = ");
		Native.print_int(referencesInFrame);
		Native.print_string("\n");
	    }
	    VM_Address ret = referencePtr;
	    referencePtr = referencePtr.add(4);
	    referencesInFrame--;
	    advance();
	    return ret;
	} else {
	    atEnginePtr=false;
	    if (DEBUG) Native.print("Returning argsPtrPtr\n");
	    return NativeHelper.getEngineSpecificPtrLoc(walkedThread.nativeContextHandle_);
	}
    }

    protected void setThread(Context ctx) {
	if (DEBUG) {
	    Native.print_string("considering context: ");
	    Native.print_int(ctx.nativeContextHandle_);
	    Native.print_string("\n");
	}
	walkedThread = ctx;
	atCtxPtr=true;
	if (isRunning=NativeHelper.localStateIsRunning(ctx.nativeContextHandle_)) {
	    if (DEBUG) Native.print_string("yar, it be running!\n");
	    end = NativeHelper.getAuxStackBottom(ctx.nativeContextHandle_);
	    referencePtr = NativeHelper.getAuxStackTop(ctx.nativeContextHandle_);
	    referencesInFrame = 0;
	    advance();
	} else {
	    if (DEBUG) Native.print_string("not running.\n");
	    atEnginePtr=true;
	}
    }

    // only for the case where the thread is running
    private void advance() {
	while (referencesInFrame == 0 && moreReferencesInThread()) {
	    referencesInFrame = referencePtr.getInt();
	    if (DEBUG) {
		Native.print("Considering frame with nptrs = ");
		Native.print_int(referencesInFrame);
		Native.print("\n");
	    }
	    referencePtr = referencePtr.add(frameHeaderSize);
	}
    }
    
    protected void callNextThreadForFirstThread() {
	frameHeaderSize = NativeHelper.getAuxHeaderSize();
	GCContext = Interpreter.getContext(0);
	if (DEBUG) {
	    Native.print("GCContext.nch = ");
	    Native.print_int(GCContext.nativeContextHandle_);
	    Native.print("\n");
	}
	contexts = Context.iterator();
	Interpreter.run(0, nextThreadToWalk());
	nextThread();
    }

    static private Context GCContext;
    static private Iterator contexts;
    static int nextThreadToWalk() {
	while (true) {
	    Context ctx;
	    if (DEBUG) Native.print("contexts.hasNext() = ");
	    if (contexts.hasNext()) {
		if (DEBUG) Native.print("true\n");
		ctx = (Context) contexts.next();
		if (DEBUG) {
		    Native.print("native context handle = ");
		    Native.print_int(ctx.nativeContextHandle_);
		    Native.print("\n");
		}
		if (!NativeHelper.localStateIsRunning(ctx.nativeContextHandle_)) {
		    // get another context, since this one is a new
		    // thread that hasn't started yet.
		    if (DEBUG) Native.print("this context is not yet running.");
		    continue;
		}
		NativeHelper.walkStack(ctx.nativeContextHandle_);
	    } else {
		if (DEBUG) Native.print("false\n");
		ctx = GCContext;
	    }
	    if (DEBUG) {
		Native.print("returning native context handle = ");
		Native.print_int(ctx.nativeContextHandle_);
		Native.print("\n");
	    }
	    return ctx.nativeContextHandle_;
	}
    }
    
    public PreciseReferenceIterator(String flvr) {
	flavor = flvr.toUpperCase().replace('-', '_').intern();
    }
}
