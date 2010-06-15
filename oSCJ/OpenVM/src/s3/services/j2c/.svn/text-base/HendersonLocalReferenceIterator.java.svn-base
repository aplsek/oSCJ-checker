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
import s3.core.services.memory.ActivationAwareLocalReferenceIterator;

/**
 * @author <a href="mailto://baker29@cs.purdue.edu">Jason Baker</a>
 **/
public class HendersonLocalReferenceIterator
    extends ActivationAwareLocalReferenceIterator {

    static final class Native implements NativeInterface {
	static native VM_Address getOVMContextLoc(int nativeContext);
	static native VM_Address getInvocationArgsLoc(int nativeContext);
	static native int        getGcRecord(int nativeContext,
					     VM_Address callee);
	static native Oop[]      getLocalReferences(int gcFrame);
	static native int        getNextGcRecord(int gcFrame);
    }

    static final int GLOBALS = 0;
    static final int STARTUP = 1;
    static final int STACK = 2;
    
    Context ctx;
    int gcFrame;
    Oop[] refs;
    static S3Blueprint.Array refsBP;
    int state = GLOBALS;
    int index = -1;
    
    protected void aboutToWalkHook() {
	ctx=null;
	gcFrame=0;
	refs=null;
	state=GLOBALS;
	index=-1;
    }
    
    /* (non-Javadoc)
     * @see s3.core.services.memory.LocalReferenceIterator#moreReferencesInThread()
     */
    protected boolean moreReferencesInThread() {
        return !(state == STACK && gcFrame == 0);
    }

    /* (non-Javadoc)
     * @see s3.core.services.memory.LocalReferenceIterator#nextReferenceInThread()
     */
    protected VM_Address nextReferenceInThread() {
	VM_Address ret;
	switch (state) {
	case GLOBALS:
	    state = gcFrame == 0 ? STARTUP : STACK;
	    ret = Native.getOVMContextLoc(ctx.nativeContextHandle_);
	    // If gcFrame is non-null, we advance to the first reference
	    // here.  Otherwise, this thread has yet to start, and we
	    // must be sure to copy its arguments.
	    break;
	case STARTUP:
	    ret = Native.getInvocationArgsLoc(ctx.nativeContextHandle_);
	    state = STACK;	// gcFrame is null, and we are at the end
	    break;
	case STACK:
	    ret = refsBP.addressOfElement(VM_Address.fromObject(refs).asOop(),
					  index);
	    break;
	default:
	    throw Executive.panic("bad thread walking state");
	}
	while (gcFrame != 0) {
	    index++;
	    if (index == refs.length) {
		index = -1;
		gcFrame = Native.getNextGcRecord(gcFrame);
		if (gcFrame != 0)
		    refs = Native.getLocalReferences(gcFrame);
	    } else
		break;
	}
	return ret;
    }

    /* (non-Javadoc)
     * @see s3.core.services.memory.LocalReferenceIterator#setThread(ovm.core.services.threads.OVMThread, ovm.core.execution.Activation)
     */
    protected void setThreadAndTopFrame(Context thread, Activation _topFrame) {
	ctx = thread;
	J2cActivation topFrame = (J2cActivation) _topFrame;
	gcFrame = Native.getGcRecord(thread.nativeContextHandle_,
				     topFrame.callee);
	state = GLOBALS;
	index = -1;
	if (gcFrame != 0)
	    refs = Native.getLocalReferences(gcFrame);
    }
    
    static void boot_() {
	try {
	    // Must this be done at runtime?
	    Domain d = DomainDirectory.getExecutiveDomain();
	    Type.Context ctx = d.getSystemTypeContext();
	    TypeName n = JavaNames.arr_Oop;
	    refsBP = (S3Blueprint.Array) d.blueprintFor(n.asCompound(), ctx);
	} catch (LinkageException e) {
	    throw Executive.panic("no blueprint for Oop[]: "
				  + e.getMessage());
	}
    }
}
