/*
 * $Header: /p/sss/cvs/OpenVM/src/s3/services/realtime/PriorityInheritanceOVMThreadImpl.java,v 1.15 2007/06/03 01:25:47 baker29 Exp $
 *
 */
package s3.services.realtime;
import ovm.core.OVMBase;
import ovm.core.services.threads.OVMThreadContext;
import s3.util.queues.SingleLinkElement;
import s3.util.queues.SingleLinkPriorityQueue;
/**
 * A priority real-time thread implementation that supports the 
 * priority inheritance protocol. 
 * The support for the priority inheritance protocol is not
 * general purpose (hence no interface types capture the semantics) but a
 * specific implementation that works in conjunction with the
 * {@link PriorityInheritanceDispatcherImpl} and the 
 * {@link PriorityInheritanceMonitorImpl} in this package. 
 * This implementation accounts for
 * the arbitrary change of a thread's priority programmatically at runtime.
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
 * <p>This class be used in a configuration that does not require the
 * priority inheritance protocol. The methods defined in this class are only
 * used by the {@link PriorityInheritanceDispatcherImpl priority inheritance 
 * dispatcher} and the 
 * {@link PriorityInheritanceMonitorImpl priority
 * inheritance monitor}.
 *
 * <p><b>NOTE:</b> When using priority-inheritance all threads must be
 * priority-inheritance threads, hence this class appears above JLThread
 * in the class hierarchy.
 *
 * <p>This class is not generally thread-safe. It is expected that the caller
 * ensures exclusive access to this thread - typically we are used by the
 * thread manager, indirectly via the dispatcher, and the dispatcher ensures
 * thread safety.
 *
 * @see <a href="doc-files/PriorityInheritance.html">Priority Inheritance Protocol 
 * Implementation</a>
 * @see PriorityInheritanceDispatcherImpl
 * @see PriorityInheritanceMonitorImpl
 *
 * @author David Holmes
 *
 */
public abstract class PriorityInheritanceOVMThreadImpl 
    extends RealtimeOVMThreadImpl {

    /**
     * This inner class provides a "node" wrapper for each thread so that
     * it can be used in a {@link SingleLinkPriorityQueue}. While each thread
     * is itself a possible element of such a queue it can only be used in
     * one queue at a time, and for the inheritance queue all threads are
     * already linked into a monitor entry queue.
     */
    class Node extends OVMBase implements SingleLinkElement {
        SingleLinkElement next;
        public void setNext(SingleLinkElement next) {
            this.next = next;
        }
        public SingleLinkElement getNext() {
            return next;
        }

        /** Return a reference to the enclosing thread instance */
        public PriorityInheritanceOVMThreadImpl getThread() {
            return PriorityInheritanceOVMThreadImpl.this;
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
    SingleLinkPriorityQueue inheritanceQueue = null;

    /** 
     * The real comparator that our node comparator forwards to.
     * This is set by the PI dispatcher as its only available after the
     * dispatcher has been initialized, not at build/load time.
     */
    protected static java.util.Comparator realComp;

    /** Special comparator to unwrap threads from Nodes */
    protected static final java.util.Comparator comp = 
    new java.util.Comparator() {
            public int compare(Object o1, Object o2) {
                Node n1 = (Node) o1;
                Node n2 = (Node) o2;
                return realComp.compare(n1.getThread(), n2.getThread());
            }
        };

    /** The base priority of this thread */
    /*package-access*/ volatile int basePriority;

    public PriorityInheritanceOVMThreadImpl(OVMThreadContext ctx) {
        super(ctx);
        // if we don't have a priority inheritance dispatcher then we
        // don't initialize the inheritance queue as we don't need it 
        if ((dispatcher instanceof PriorityInheritanceDispatcherImpl)) {
            inheritanceQueue = new SingleLinkPriorityQueue(comp);
        }
        else {
            // nothing - inheritanceQueue stays null and should never be used
        }
    }
    

    public PriorityInheritanceOVMThreadImpl() {
        // if we don't have a priority inheritance dispatcher then we
        // don't initialize the inheritance queue as we don't need it 
        if ((dispatcher instanceof PriorityInheritanceDispatcherImpl)) {
            inheritanceQueue = new SingleLinkPriorityQueue(comp);
        }
        else {
            // nothing - inheritanceQueue stays null and should never be used
        }
    }

    /**
     * Sets the base priority of this thread. This method should only be
     * called by the priority inheritance dispatcher, or by the thread
     * itself (but only when not alive).
     */
    protected void setBasePriority(int newPriority) {
        basePriority = newPriority;
    }

    /**
     * Returns the base priority of this thread
     * @return the base priority of this thread
     */
    protected int getBasePriority() {
        return basePriority;
    }

    /**
     * Calculates what the current active priority of this thread should be.
     * This is used to maintain the active priority invariant.
     * @return the maximum of the base priority and the active priority of
     * the head of the inheritance-queue.
     */
    /*package-access*/ int getCurrentActivePriority() {
        if (!inheritanceQueue.isEmpty()) {
            int topPriority = ((Node)inheritanceQueue.head()).getThread().getPriority();
            if (topPriority > basePriority) {
                return topPriority;
            }
        }
        return basePriority;
    }
}

    










