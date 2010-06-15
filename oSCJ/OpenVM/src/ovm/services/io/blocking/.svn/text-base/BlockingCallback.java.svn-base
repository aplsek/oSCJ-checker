// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/blocking/BlockingCallback.java,v 1.11 2004/10/10 03:01:17 pizlofj Exp $

package ovm.services.io.blocking;

import ovm.core.services.threads.*;
import ovm.services.io.async.*;
import ovm.services.threads.*;
import ovm.core.services.memory.*;
import s3.util.*;
import ovm.core.execution.*;

/**
 * An adaptor of sorts that gives proper (non-stalling) blocking semantics
 * to any Async I/O call.
 * <p>
 * An example use would be:
 * <pre>
 * BlockingCallback bc=new BlockingCallback(bm,tm);
 * someObject.someAsyncCall(someParameter,bc);
 * bc.waitOnDone(); // blocking happens here
 * IOException error=bc.getError();
 * if (error!=null) {
 *     // handle error here
 * } else {
 *     // operation succeeded; get the finalizer
 *     AsyncFinalizer f=bc.getFinalizer();
 *     // extract return value from finalizer...
 * }
 * </pre>
 * @author Filip Pizlo
 */
public class BlockingCallback implements AsyncCallback {

    public static final boolean DEBUG=false;
    
    private final BlockingManager bm_;
    private final UserLevelThreadManager tm_;

    // this could be a scoped object
    // explanation: this reference could be illegal according
    // to the RTSJ
    private OVMThread thread_;
    private void setThread(OVMThread t) throws PragmaNoBarriers {
        thread_=t;
    }
    private OVMThread getThread() throws PragmaNoBarriers {
        return thread_;
    }
    
    private volatile boolean done_;
    private volatile boolean blocked_;

    // Could this be scoped too?
    // this object could most definitely be scoped, but the following
    // property is guaranteed to be satisfied:
    // areaOf(this) == areaOf(this.finalizer_)
    private volatile AsyncFinalizer finalizer_;
    
    public BlockingCallback(BlockingManager bm,
                            UserLevelThreadManager tm) {
        this.bm_=bm;
        this.tm_=tm;
        this.setThread(tm.getCurrentThread());
        
        this.done_=false;
        this.blocked_=false;
        this.finalizer_=null;
    }
    
    public void ready(AsyncFinalizer finalizer) throws PragmaAtomic {
	if (done_) {
	    ovm.core.Executive.panic("unblock() called when already done.");
	}

	if (DEBUG) {
	    Native.print_string("in BlockingCallback.ready()\n");
	}
	
	this.finalizer_=finalizer;
	done_=true;
	
	if (!blocked_) {
	    if (DEBUG) {
		Native.print_string("wasn't blocked!\n");
	    }
	    return;
	}
	blocked_ = false;
	
	// this makeReady() call dirties the ready queue so that the next
	// time that setReschedulingEnabled() transitions us from disabled to
	// enabled, runNextThread() will be called.
	if (DEBUG) {
	    Native.print_string("marking thread as ready!\n");
	}
	tm_.makeReady(getThread());
	bm_.notifyUnblock(getThread());
    }
    
    public int getPriority() {
        return bm_.getPriority(getThread());
    }
    
    /**
     * Wait until <code>ready()</code> gets called.  <i>Must be called
     * from within the same thread that this object was constructed
     * in.</i>
     */
    public void waitOnDone() {
        for (;;) {
            if (done_) {
		if (DEBUG) {
		    Native.print_string("looks like I might be done\n");
		}
		// reset done_ to allow for finalizer chaining.  the finish() method
		// may return false but call ready() first, indicating that we should
		// call the finish() method of a new finalizer.
		done_=false;
		
                if (finalizer_.finish()) {
		    if (DEBUG) {
			Native.print_string("yes, I am\n");
		    }
                    return;
                } else if (done_) {
		    if (DEBUG) {
			Native.print_string("finalizer chain fast-path!\n");
		    }
		    // finalizer chaining will happen here.  we get here iff finish()
		    // returned false but ready() was called with a new finalizer.
		    continue;
		}
            }
            
	    // we get to here /only/ if done_ is false.  but, done_ may get set to
	    // true by a call to ready() from a separate thread or from an interrupt
	    // right at the beginning of the call to setReschedulingEnabled().
	    
            boolean enabled=tm_.setReschedulingEnabled(false);
            try {
		// because ready() could have been called between the last check and
		// here, we check done_ again.
                if (done_) {
		    if (DEBUG) {
			Native.print_string("ready called already\n");
		    }
                    continue;
                }
                
		if (DEBUG) {
		    Native.print_string("blocking...\n");
		}

                done_=false;
                blocked_=true;
                tm_.removeReady(getThread());
                bm_.notifyBlock(getThread());
                tm_.runNextThread();

		if (DEBUG) {
		    Native.print_string("awake now!\n");
		}
		
		// this thread should only get woken up if ready() gets called.  hence,
		// done_ must be true.
		if (!done_) {
		    ovm.core.Executive.panic(
			"!done_ coming out of runNextThread() in BlockingCallback");
		}
                
                // now we unblocked.  must set this
                // in case an event happens at the back-branch of this
                // loop.  (actually, if an event happens on the back-branch,
		// ready() will panic because done_ is true.  however, having
		// this set to false is necessary for finalizer chaining
		// above, so we might as well do it.)
                blocked_=false;
            } finally {
                tm_.setReschedulingEnabled(enabled);
            }
        }
    }
    
    public IOException getError() {
        return finalizer_.getError();
    }
    
    public AsyncFinalizer getFinalizer() {
        return finalizer_;
    }
    
}

