
package s3.services.java.realtime;

import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.execution.InvocationMessage;
import ovm.core.execution.ReturnMessage;
import ovm.core.repository.TypeCodes;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.threads.OVMThreadContext;
import ovm.services.java.JavaDispatcher;
import ovm.services.java.JavaOVMThread;
import ovm.services.realtime.RealtimeOVMThread;
import ovm.util.OVMError;
import s3.core.domain.S3RealtimeJavaUserDomain;
import s3.services.java.ulv1.JavaOVMThreadImpl;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import s3.util.queues.SingleLinkElement;
import s3.util.queues.SingleLinkPriorityQueue;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.domain.ReflectiveMethod;

/**
 * An extension of the {@link JavaOVMThreadImpl Java OVM thread} representation
 * for use in a real-time Java configuration. Within such a configuration all
 * Java threads must be bound to an OVM thread of this type. This class
 * provides support for the priority-inheritance-protocol as implemented by
 * the {@link RealtimeJavaDispatcherImpl} and 
 * {@link PriorityInheritanceJavaMonitorImpl} classes; and supports control
 * over the initial allocation context for a thread.
 *
 * <p>The notion of priority that we inherit is our <em>active</em> priority.
 * This is the priority set by the dispatcher and which is used to maintain
 * our position in any priority queue. To this we add a notion of the 
 * <em>base priority</em> which is the priority (still set by the dispatcher)
 * which reflects the priority that we have been assigned programmatically.
 * In the context of the RTSJ it is the priority set by binding a
 * {@code javax.realtime.PriorityParameters PriorityParameters} object to a
 * real-time thread, or subsequently changing the priority value of that
 * object.
 *
 * @author David Holmes
 *
 * @see <a href="{@docRoot}/s3/services/realtime/doc-files/PriorityInheritance.html">
 * Priority Inheritance Protocol Implementation</a>
 *
 */
public class RealtimeJavaThreadImpl 
    extends JavaOVMThreadImpl
    implements RealtimeOVMThread {

    /**
     * Pre-allocated invocation message for the finalizeThread upcall.
     * This has to be pre-allocated because the current thread could be 
     * allocated in scope while its initial (and hence final) allocation 
     * context is heap or immortal
     */
    final InvocationMessage finalizeThread_im;


    /**
     * Pre-allocated ReturnMessage for use with the invocation message.
     * This is pre-allocated to avoid OutOfMemoryError in the ED trying to
     * do the reflective up call
     */
    final ReturnMessage finalizeThread_rmsg;

    /**
     * Our factory class
     */
    public static class Factory implements JavaOVMThread.Factory {

        public JavaOVMThread newInstance(Oop javaThread) {
            return new RealtimeJavaThreadImpl(javaThread);
        }

        // this should really propagate to a new interface but we're already
        // tightly coupled in our realtime implementations
        public JavaOVMThread newInstance(Oop javaThread, boolean noHeap) {
            if (noHeap)
                return new NoHeapRealtimeJavaThreadImpl(javaThread);
            else
                return new RealtimeJavaThreadImpl(javaThread);
        }

    }

    /**
     * Our factory instance. This is a per-class value and so must always
     * be accessed using the specific class name.
     */
    public static final JavaOVMThread.Factory factory = new Factory();

    /**
     * This inner class provides a "node" wrapper for each thread so that
     * it can be used in a {@link SingleLinkPriorityQueue}. While each thread
     * is itself a possible element of such a queue it can only be used in
     * one queue at a time, and for the inheritance queue all threads are
     * already linked into a monitor entry queue.
     */
    class Node implements SingleLinkElement {
        SingleLinkElement next;
        public void setNext(SingleLinkElement next)
            throws ovm.core.services.memory.PragmaNoBarriers,
                   PragmaInline {
            this.next = next;
        }
        public SingleLinkElement getNext() throws PragmaInline {
            return next;
        }

        /** Return a reference to the enclosing thread instance */
        public RealtimeJavaThreadImpl getThread() throws PragmaInline {
            return RealtimeJavaThreadImpl.this;
        }
    }

    /** 
     * The node for this thread that can be used to link it into
     * an inheritance queue.
     */
    final Node node = new Node();

    /** 
     * The inheritance queue used by this thread. This contains the set of
     * all other threads that could bequest their priority to this thread, due
     * to this thread owning a monitor upon which the other thread is
     * blocked. The active priority of this thread is the maximum of its
     * base priority and the active priority of the thread at the head of
     * this queue.
     */
    /* package*/ SingleLinkPriorityQueue inheritanceQueue = null;

    /** 
     * The real comparator that our node comparator forwards to.
     * This is set by the dispatcher as its only available after the
     * dispatcher has been initialized, not at build/load time
     */
    protected static java.util.Comparator realComp;

    /** Special comparator to unwrap threads from Nodes */
    protected static final java.util.Comparator comp = 
        new java.util.Comparator() {
            public int compare(Object o1, Object o2) throws PragmaNoPollcheck {
                Node n1 = (Node) o1;
                Node n2 = (Node) o2;
                return realComp.compare(n1.getThread(), n2.getThread());
            }
        };

    /** The base priority of this thread */
    /*package-access*/ volatile int basePriority;


    /** Reference to the Realtime user-domain to which our Java thread belongs
     */
    protected S3RealtimeJavaUserDomain rtUserDom;


    /** Is this thread bound to a realtime or non-realtime Java thread?
     */
    protected final boolean isRTT;

    // NOTE: this is called by our superclass constructor
    public void bindJavaThread(Oop jThread) {
        super.bindJavaThread(jThread);
        rtUserDom = (S3RealtimeJavaUserDomain) userDom;
    }


    VM_Area getInitialArea() {
	Object r = MemoryPolicy.the().enterScratchPadArea();
	try {
	    ReflectiveMethod getInitArea = 
		rtUserDom.VMThread_getInitialArea.dispatch(javaThread);
	    Oop ma = getInitArea.call(javaThread).getOop();
	    return (VM_Area) rtUserDom.memoryArea_area.get(ma);
	} finally {
	    MemoryPolicy.the().leave(r);
	}
    }

    /**
     * Construct a new real-time OVM thread bound to the given Java thread.
     * When this thread starts executing it will execute the startup
     * method of the Java thread.
     *
     * @param javaThread the Java thread to which this thread should be bound
     * @throws OVMError.IllegalArgument if <code>javaThread</code> is 
     * <code>null</code>
     *
     */
    protected RealtimeJavaThreadImpl(Oop javaThread) {
         super(javaThread);

         Type rtt = 
             rtUserDom.commonRealtimeTypes().java_lang_VMRealtimeThread;
         isRTT = javaThread.getBlueprint().getType().isSubtypeOf(rtt);

         inheritanceQueue = new SingleLinkPriorityQueue(comp);

	 ReflectiveMethod fMethod =
	    rtUserDom.realtimeThread_finalizeThread.dispatch(javaThread);
	 finalizeThread_im = fMethod.makeMessage();
	 finalizeThread_rmsg = new ReturnMessage(TypeCodes.VOID);
    }

    /**
     * Constructs an unbound thread. This thread can act as the
     * primordial thread. Its initial allocation context is the heap.
     */
    protected RealtimeJavaThreadImpl(OVMThreadContext ctx) {
        super(ctx);
        inheritanceQueue = new SingleLinkPriorityQueue(comp);
        isRTT = false;
	finalizeThread_im = null;
	finalizeThread_rmsg = null;
    }

    /**
     * {@inheritDoc}
     * <p>In addition, we establish the equivalence of the initial active
     * and base priorities.
     */
    public void prepareForStart(JavaDispatcher dsp) {
        super.prepareForStart(dsp);
        setBasePriority(getPriority());
    }



    /**
     * {@inheritDoc}
     * <p>Overridden to sanity check active and base priorities
     */
    protected void doRun() {
	assert getPriority() == getBasePriority():
	    "base priority " + getBasePriority() + 
	    " != active priority" + getPriority();
        super.doRun();
    }


    /**
     * Set the current allocation context to be our initial allocation 
     * context
     */
    protected void prepareForRun() {
        MemoryManager.the().setCurrentArea(getInitialArea());
    }

    /**
     * Sets the base priority of this thread. This method should only be
     * called by the priority inheritance dispatcher, or by the thread
     * itself (but only when not alive).
     */
    protected void setBasePriority(int newPriority) throws PragmaNoPollcheck {
        basePriority = newPriority;
    }

    /**
     * Returns the base priority of this thread
     * @return the base priority of this thread
     */
    public int getBasePriority() throws PragmaNoPollcheck {
        return basePriority;
    }

    /**
     * Calculates what the current active priority of this thread should be.
     * This is used to maintain the active priority invariant.
     * @return the maximum of the base priority and the active priority of
     * the head of the inheritance-queue.
     */
    /*package*/int getCurrentActivePriority() throws PragmaNoPollcheck {
        if (!inheritanceQueue.isEmpty()) {
            int topPriority = 
                ((Node)inheritanceQueue.head()).getThread().getPriority();
            if (topPriority > basePriority) {
                return topPriority;
            }
        }
        return basePriority;
    }


    /**
     * Finalize this thread. A thread is finalized just prior to termination
     * while it is still the current thread and rescheduling is disabled.
     * We hook this up to reflectively invoke our user-domain Java real-time 
     * thread's <tt>finalizeThread</tt> method.
     */
    final void finalizeThread() throws PragmaNoPollcheck {
        if (isRTT) {
            //ovm.core.execution.Native.print_string("Finalizing a thread\n");
            finalizeThread_im.invoke(javaThread, 
                                     finalizeThread_rmsg).rethrowWildcard();
        }
        else {
            //ovm.core.execution.Native.print_string("No finalization method found\n");
        }
        javaThread = null; // assist GC?
    }

    // utility methods for testing checking the priority-inheritance mechanism

    public int getInheritanceQueueSize() {
        return inheritanceQueue.size();
    }

    public boolean checkInheritanceQueueHead(RealtimeJavaThreadImpl t) {
        return ((Node)inheritanceQueue.head()).getThread() == t;
    }

    public boolean checkInheritanceQueueTail(RealtimeJavaThreadImpl t) {
        return ((Node)inheritanceQueue.tail()).getThread() == t;
    }

}
