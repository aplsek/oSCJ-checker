package s3.services.io.async;

import ovm.services.io.async.*;
import ovm.services.io.signals.*;
import ovm.services.threads.*;
import ovm.core.services.memory.*;
import ovm.core.execution.*;
import ovm.core.*;
import s3.util.*;

/**
 *
 * @author Filip Pizlo
 */
abstract class AsyncOpQueue {
    public static final boolean DEBUG=false;
    
    protected Object lock;
    protected int fd;
    protected UserLevelThreadManager tm;
    
    // never access these directly.  always use methods below.
    private volatile OpNode head_=null;
    private volatile OpNode tail_=null;
    
    private void setHead(OpNode n) throws PragmaNoBarriers {
        head_=n;
    }
    private OpNode getHead() throws PragmaNoBarriers {
        return head_;
    }
    private void setTail(OpNode n) throws PragmaNoBarriers {
        tail_=n;
    }
    private OpNode getTail() throws PragmaNoBarriers {
        return tail_;
    }
    
    private String name_;
    private int cnt_=0;
    
    public AsyncOpQueue(Object lock,
                        int fd,
                        UserLevelThreadManager tm,
			String name) {
        this.lock=lock;
        this.fd=fd;
        this.tm=tm;
	this.name_=""+fd+":"+name;
    }
    
    public String toString() {
	return name_;
    }
    
    protected abstract void addToSignalManager(OpNode cback);

    protected abstract void removeFromSignalManager(OpNode cback,
						    Object byWhat);
    
    protected void enqueue(OpNode n) {
	if (DEBUG) {
	    Native.print_string("enqueueing: ");
	    Native.print_string(n.toString());
	    Native.print_string("\n");
	}
	
        n.setNext(null);
        if (getTail()==null) {
            setTail(n);
            setHead(n);
        } else {
            getTail().setNext(n);
            setTail(n);
        }
    }
    
    protected OpNode peek() {
	if (DEBUG) {
	    Native.print_string("peeking: ");
	    Native.print_string(head_.toString());
	    Native.print_string("\n");
	}
	
        return getHead();
    }
    
    protected OpNode dequeue() {
	if (DEBUG) {
	    Native.print_string("dequeueing: ");
	    Native.print_string(head_.toString());
	    Native.print_string("\n");
	}
	
        if (getHead()==null) {
            throw Executive.panic(
                "AsyncOpQueue.dequeue() called on an empty queue.");
        }
        
        OpNode result=getHead();
        setHead(getHead().getNext());
        if (getHead()==null) {
            setTail(null);
        }
        result.setNext(null);
        return result;
    }

    protected boolean removeNotFirst(OpNode n) {
	if (DEBUG) {
	    Native.print_string("removeNotFirst: ");
	    Native.print_string(n.toString());
	    Native.print_string("\n");
	}

	for (OpNode cur=getHead();
	     cur.getNext()!=null;
	     cur=cur.getNext()) {
	    if (cur.getNext()==n) {
		cur.setNext(cur.getNext().getNext());
		if (n==getTail()) {
		    setTail(cur);
		}
		return true;
	    }
	}

	return false;
    }
    
    /**
     * Tells you if the queue is empty.
     * @return <code>true</code> iff the size is 0
     */
    public boolean empty() {
        return getHead()==null;
    }
    
    /**
     * Tells you if the queue has one element in it.
     * @return <code>true</code> iff the size is 1
     */
    public boolean hasOne() {
        return !empty() && getHead()==getTail();
    }
    
    // MUST BE CALLED FROM SAME VM_AREA AS PARAMETER OPNODE
    // must synchronize against the lock before calling
    public OpNode performOp(OpNode n) {
        n.init(this,cnt_++);
        
        // attempt fast-path if no-one else is waiting.
        if (empty() && n.doOp()) {
            n.notifyOpDone();
            //ovm.core.OVMBase.d("calling "+n+".ready() from "+this+" after fast path");
            n.ready();
            return n;
        }
        
        // slow path.
        enqueue(n);
        if (hasOne()) {
	    // FIXME: currently, when using the SIGIO configuration,
	    // addToSignalManager() has to call select().  this could be
	    // quite slow if we have a lot of file descriptors.  instead,
	    // addToSignalManager() should return false if there is a
	    // chance that a pre-existing readiness condition would have
	    // been missed; in that case the code here will simply
	    // reattempt the doOp().  this will also allow
	    // IOSignalManagerViaSIGIO to go back to being Agnostic.
            addToSignalManager(n);
        }

	return n;
    }
    
    // it is your responsibility to call the appropriate remove method
    // on the signal manager.  you should this action atomic with this
    // one by synchronizing on the lock that this object was instantiated
    // with
    public int cancelAll(IOException error) {
	int result=0;
	if (DEBUG) {
	    Native.print_string(toString());
	    Native.print_string(" in cancelAll(");
	    Native.print_string(error.toString());
	    Native.print_string(")\n");
	}
        while (!empty()) {
	    if (DEBUG) {
		Native.print_string("Cancelling ");
		Native.print_string(peek().toString());
		Native.print_string("\n");
	    }
            dequeue().cancelImpl(error);
	    ++result;
        }
	return result;
    }

    /**
     * Represents an operation that is waiting to be completed.  An OpNode serves
     * three purposes:
     * <ul>
     * <li>It defines how an operation is to be performed,
     * <li>It implements the IOSignalManager.Callback and sits on the IOSignalManager's
     *     lists, and
     * <li>It implements the AsyncFinalizer and is handed to AsyncCallback(s) whenever
     *     the IOSignalManager signals that it is ready.
     * </ul>
     * Typically, an IODescriptor implementation that is based on an IOSignalManager
     * and does queueing with an AsyncOpQueue will implement all of its operations as
     * OpNodes.  Note that these OpNode are usually static inner classes.  They cannot
     * be non-static inner classes because of the RTSJ reference problems that emerge
     * with outer-this pointers.
     */
    public static abstract class OpNode
        extends ErrnoFinalizerBase
	implements IOSignalManager.Callback,
		   AsyncHandle {
        
        // never access directly
        OpNode next=null;
        
        void setNext(OpNode n) throws PragmaNoBarriers {
            next=n;
        }
        OpNode getNext() throws PragmaNoBarriers {
            return next;
        }
        
        // invariant: areaOf(this) == areaOf(cback_)
        private AsyncCallback cback_;
        
        // never access directly
        // Q: is this really necessary?
        // A: yes!  you could have an AsyncOpQueue allocated in a scope but
        //    used from immortal, or from an adjacent scope.  this may be
        //    necessary if we ever mix selectors with scopes.
        private AsyncOpQueue queue_;
        
        private void setQueue(AsyncOpQueue q) throws PragmaNoBarriers {
            queue_=q;
        }
        private AsyncOpQueue getQueue() throws PragmaNoBarriers {
            return queue_;
        }
        
        private volatile boolean opAlreadyDone_=false;
        private volatile boolean opCanceled_=false;
        private volatile boolean signalShouldNotBeCalledAgain_=false;
        private volatile boolean readyCalled_=false;
	
	private String name_;
        
        private boolean setReadyOnce() throws PragmaNoPollcheck {
            if (readyCalled_) {
                return false;
            }
            return readyCalled_=true;
        }
        
        public OpNode(AsyncCallback cback) {
            super(null);
            this.cback_=cback;
        }
        
        // MUST BE CALLED FROM SAME VM_AREA AS RECEIVER
        void init(AsyncOpQueue queue,
		  int cnt) {
            this.setQueue(queue);
            this.tm=queue_.tm;
            
            // use StringBuffer directly to avoid calls to
            // StringBuffer(String).
            StringBuffer buf=new StringBuffer();
            buf.append(queue.toString());
            buf.append('/');
            buf.append(cnt);
            this.name_=buf.toString();
        }
	
	public String toString() {
	    return name_;
	}
        
        void notifyOpDone() {
            opAlreadyDone_=true;
        }
        
        void ready() {
            ready(this);
        }
        
        void ready(AsyncFinalizer otherFinalizer) {
	    // why have this guard?

	    // ready() can be called for the following reasons:
	    // 1) immediate (fast path) completion in performOp()
	    // 2) signal() calls this.ready()
	    // 3) cancel() calls this.ready()
	    // 4) finish() calls ready() on the next node in the queue
	    // 5) cancel() calls ready() on the next node in the queue

	    // note: only 2 is done without the queue lock held

	    // we can have ready() called multiple times before we enter
	    // finish().  the possible combinations are:
	    // - 2 followed by 3
	    // - 4 followed by 3

	    // hence we need this guard.

            if (!setReadyOnce()) {
                return;
            }
	    cback_.ready(otherFinalizer);
        }

	public boolean canCancelQuickly() {
	    return true;
	}

	public void cancel(IOException error) {
	    synchronized (getQueue()) {
		if (opAlreadyDone_ || opCanceled_) {
		    return;
		}
		getQueue().removeFromSignalManager(this,null);
		if (getQueue().peek()==this) {
		    getQueue().dequeue();
		    if (!getQueue().empty()) {
			getQueue().peek().ready();
		    }
		} else {
		    getQueue().removeNotFirst(this);
		}
		cancelImpl(error);
	    }
	}

	// called from cancel() above and also cancelAll().  never call
	// this directly.
	void cancelImpl(IOException error) {
            if (opAlreadyDone_) {
		if (DEBUG) {
		    Native.print_string("op already done!\n");
		}
                return;
            }
            opCanceled_=true;
            setError(error);
	    if (DEBUG) {
		Native.print_string("calling ready!\n");
	    }
            ready();
        }
        
        /** attempt to do the operation but only if it completes immediately. */
        public abstract boolean doOp();
        
        public final boolean finish() {
	    if (DEBUG) {
		Native.print_string("in finish: ");
		Native.print_string(toString());
		Native.print_string("\n");
	    }
	    
            if (opAlreadyDone_ || opCanceled_) {
                return true;
            }
            
            synchronized (getQueue().lock) {
		// in here, we know that ready() cannot be called, because:
		// - cancel() cannot call ready(), since cancel() is waiting on
		//   us to relinquish the lock.
		// - another finish() call cannot call ready(), since finish()
		//   would be waiting on this same lock.
		// - signal() cannot call ready(), because:
		//   - if we're in here because of signal(), then signal() would
		//     have returned false, causing itself to be unregistered
		//   - if we're in here because of cancel(), then cancel() would
		//     have unregistered signal().

                if (opCanceled_) {
                    return true;
                }
                
                if (doOp()) {
                    opAlreadyDone_=true;
                    getQueue().dequeue();
                    if (!getQueue().empty()) {
                        getQueue().peek().ready();
                    }
                    return true;
                }
                
		readyCalled_=false; /* reset to allow ready() to be called
				       again when we return. */
		signalShouldNotBeCalledAgain_=false;
                getQueue().addToSignalManager(this);
                return false;
            }
        }
        
        public final boolean signal(boolean certain) {
            if (signalShouldNotBeCalledAgain_) {
		ovm.core.Executive.panic(
                    "signal() called again after it returned false but before "+
		    "being reset");
            }
	    
	    if (DEBUG) {
		Native.print_string("got signaled: ");
		Native.print_string(toString());
		Native.print_string("\n");
	    }
            
            if (getQueue().peek()!=this) {
		ovm.core.Executive.panic(
		    "queue_.peek()!=this in signal()");
	    }
            
            signalShouldNotBeCalledAgain_=true;

            ready();
            
            return false;
        }

        public final void removed(Object byWhat) {
	    if (DEBUG) {
		Native.print_string("got removed: ");
		Native.print_string(toString());
		Native.print_string("\n");
	    }
            
	    signalShouldNotBeCalledAgain_=true;
        }
    }
}

