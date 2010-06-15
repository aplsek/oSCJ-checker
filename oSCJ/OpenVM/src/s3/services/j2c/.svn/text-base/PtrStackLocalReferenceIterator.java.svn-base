package s3.services.j2c;

import ovm.core.Executive;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.NativeInterface;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import s3.core.domain.S3Blueprint;
import ovm.core.services.memory.LocalReferenceIterator;
import ovm.core.execution.Native;
import ovm.core.services.memory.MemoryManager;

public class PtrStackLocalReferenceIterator
    extends LocalReferenceIterator {

    static final class Nat implements NativeInterface {
	static native VM_Address getOVMContextLoc(int nativeContext);
	static native VM_Address getEngineSpecificPtrLoc(int nativeContext);
	static native VM_Address getPtrStackBase(int nativeContext);
	static native VM_Address getPtrStackTop(int nativeContext);
	static native VM_Address getPtrStackLimit(int nativeContext);
	
	// we use this directly to zero stacks
	static native void bzero(VM_Address addr, int nb);
    }
    
    Context walkedThread;
    Context curThread;
    VM_Address curThreadTop;

    // are we looking at the ovm context pointer?
    boolean atCtxPtr;

    // is the thread running? (cached so we don't have to make too many native calls)
    boolean isRunning;

    // for threads that are running
    VM_Address cur,end;

    // for threads that are not yet running
    boolean atEnginePtr;

    int frameHeaderSize;
    
    boolean zero;
    
    public PtrStackLocalReferenceIterator(boolean zero) {
	this.zero=zero;
    }
    
    public PtrStackLocalReferenceIterator(String zero) {
	this(zero!=null && zero.equalsIgnoreCase("true"));
    }
    
    private static final boolean DEBUG=false;
    
    protected void aboutToWalkHook() {
	walkedThread=null;
	atCtxPtr=false;
	isRunning=false;
	cur=null;
	end=null;
	atEnginePtr=false;
	frameHeaderSize=0;
    }
    
    protected boolean moreReferencesInThread() {
	if (atCtxPtr) {
	    return true;
	} else if (isRunning) {
	    return cur!=end;
	} else {
	    return atEnginePtr
		&& (Nat.getEngineSpecificPtrLoc(walkedThread.nativeContextHandle_)
		    != VM_Address.fromObject(null));
	}
    }
    
    protected VM_Address nextReferenceInThread() {
	if (atCtxPtr) {
	    atCtxPtr=false;
	    if (DEBUG) Native.print_string("Returning context pointer.\n");
	    return Nat.getOVMContextLoc(walkedThread.nativeContextHandle_);
	} else if (isRunning) {
	    VM_Address ret=cur;
	    if (DEBUG) {
		Native.print_string("Returning ");
		Native.print_ptr(ret);
		Native.print_string(" with end=");
		Native.print_ptr(end);
		Native.print_string("\n");
	    }
	    cur=cur.add(4);
	    return ret;
	} else {
	    atEnginePtr=false;
	    if (DEBUG) Native.print_string("Returning engine-specific pointer.\n");
	    return Nat.getEngineSpecificPtrLoc(walkedThread.nativeContextHandle_);
	}
    }
    
    void zeroThread() {
    }
    
    protected void setThread(Context ctx) {
	walkedThread=ctx;
	atCtxPtr=true;
	atEnginePtr=true;
	cur=Nat.getPtrStackBase(ctx.nativeContextHandle_);
	if (ctx==curThread) {
	    end=curThreadTop;
	} else {
	    end=Nat.getPtrStackTop(ctx.nativeContextHandle_);
	}
	isRunning=end.isNonNull();
	if (zero && walkedThread!=curThread && isRunning) {
	    if (DEBUG) {
		Native.print_string("Zeroing unused stack for ");
		Native.print_int(walkedThread.nativeContextHandle_);
		Native.print_string("\n");
	    }
	    int sz=Nat.getPtrStackLimit(walkedThread.nativeContextHandle_)
		.diff(end).asInt()+0;
	    Nat.bzero(end,sz);
	    if (DEBUG) Native.print_string("DONE!\n");
	}
	if (DEBUG) {
	    Native.print_string("Setting thread to ");
	    Native.print_int(walkedThread.nativeContextHandle_);
	    Native.print_string(", with cur=");
	    Native.print_ptr(cur);
	    Native.print_string(", end=");
	    Native.print_ptr(end);
	    Native.print_string(", isRunning=");
	    Native.print_string(isRunning?"true":"false");
	    Native.print_string("\n");
	}
    }
    
    public void prepareForGC() {
	prepareForGCHook();
	curThread=Context.getCurrentContext();
	curThreadTop=Nat.getPtrStackTop(curThread.nativeContextHandle_);
	if (DEBUG) {
	    Native.print_string("in prepareForGC, curThread is ");
	    Native.print_int(curThread.nativeContextHandle_);
	    Native.print_string(", top=");
	    Native.print_ptr(curThreadTop);
	    Native.print_string("\n");
	}
	MemoryManager.the().doGC();
	if (zero) {
	    if (DEBUG) Native.print_string("Zeroing current thread's unused stack.\n");
	    int sz=Nat.getPtrStackLimit(curThread.nativeContextHandle_)
		.diff(curThreadTop).asInt()+0;
	    Nat.bzero(curThreadTop,sz);
	    if (DEBUG) Native.print_string("DONE!!\n");
	}
    }
}



