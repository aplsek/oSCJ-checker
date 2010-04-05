package s3.core.services.interpreter;

import ovm.core.Executive;
import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.NativeInterface;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import s3.core.services.memory.ActivationAwareLocalReferenceIterator;
import ovm.core.services.memory.LocalReferenceIterator;

/**
 * @author <a href=mailto=baker29@cs.purdue.edu>Jason Baker</a>
 **/
public class InterpreterLocalReferenceIterator
    extends ActivationAwareLocalReferenceIterator {

    static final class Native implements NativeInterface {
	static native VM_Address getOVMContextLoc(int nativeContext);
	static native VM_Address getValueStack(int nativeContext);
	static native VM_Address getTagStack(int nativeContext);
	static native int        getStackDepth(int nativeContext,
					       int frameHandle);
    }

    static final int GLOBALS = 0;
    static final int STACK = 1;

    // FIXME: should this be in NativeConstants?  Always, or just for
    // the interpreter?
    static final int TAG_REF = 1;
    
    Context ctx;
    int state;
    VM_Address tags;
    VM_Address values;
    int end;
    int index = 0;
    
    protected void aboutToWalkHook() {
	ctx=null;
	state=0;
	tags=null;
	values=null;
	end=0;
	index=0;
    }
    
    /* (non-Javadoc)
     * @see s3.core.services.memory.LocalReferenceIterator#moreReferencesInThread()
     */
    protected boolean moreReferencesInThread() {
        return index < end;
    }

    /* (non-Javadoc)
     * @see s3.core.services.memory.LocalReferenceIterator#nextReferenceInThread()
     */
    protected VM_Address nextReferenceInThread() {
	VM_Address ret;
	switch (state) {
	case GLOBALS:
	    ret = Native.getOVMContextLoc(ctx.nativeContextHandle_);
	    state = STACK;
	    break;
	case STACK:
	    ret = values.add(VM_Word.widthInBytes() * index++);
	    break;
	default:
	    throw Executive.panic("bad thread walking state");
	}
	// find the next in-use reference location, stop when we hit
	// stack top
        while (index < end && tags.add(index).getByte() != TAG_REF)
	    index++;
        return ret;
    }

    /* (non-Javadoc)
     * @see s3.core.services.memory.LocalReferenceIterator#setThread(ovm.core.services.threads.OVMThread, ovm.core.execution.Activation)
     */
    protected void setThreadAndTopFrame(Context thread, Activation topFrame) {
	ctx = thread;
	state = GLOBALS;
	values = Native.getValueStack(thread.nativeContextHandle_);
	tags = Native.getTagStack(thread.nativeContextHandle_);
	index = 0;
	int f = ((InterpreterActivation) topFrame).f;
	end = Native.getStackDepth(thread.nativeContextHandle_, f);
    }
}
