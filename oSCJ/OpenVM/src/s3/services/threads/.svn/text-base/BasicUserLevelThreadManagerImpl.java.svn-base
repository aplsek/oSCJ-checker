
package s3.services.threads;

import ovm.core.services.events.EventManager;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.threads.OVMThread;
import ovm.core.stitcher.EventServicesFactory;
import ovm.core.stitcher.IOServiceConfigurator;
import ovm.services.threads.OrderedUserLevelThreadManager;
import ovm.services.threads.UserLevelThreadManagerCoreImpl;
import ovm.util.OVMError;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaAssertNoExceptions;
import s3.util.PragmaIgnoreExceptions;
import ovm.core.services.memory.PragmaNoBarriers;
import s3.util.queues.SingleLinkElement;
import s3.util.queues.SingleLinkPriorityQueue;
import ovm.core.execution.Native;
/**
 * A basic thread manager implementation for priority-preemptive, but not
 * time-preemptive OVM threads. There is no support for timed-suspension,
 * just the minimal ability to cooperatively context switch and create
 * threads.
 * <p>The basic operation of this thread manager is as follows:
 * <ul>
 * <li>A thread may only appear in the ready queue once</li>
 * <li>The current thread is always the thread at the head of the ready
 * queue (except when a context switch is pending of course)</li>
 * <li>The ready queue is maintained in an order determined by the comparator
 * that is configured. Setting of the comparator would typically be
 * done by the dispatcher. The comparator must be set before {@link #init} is
 * invoked.
 * <p>If no comparator is configured then a default
 * comparator makes all threads equal and will schedule FIFO - when a thread
 * is added to the ready queue it will go to the end of the set of threads at
 * the same priority. Under this comparator the following properties hold:
 * <ul>
 * <li>As the current thread remains in the ready queue, preemption by a higher
 * priority thread leaves a thread at the head of its priority level.
 * <li>Changes to the priority of a thread, as indicated by the 
 * {@link #changeNotification} method cause a thread to move to the tail for
 * it's priority level.
 * </ul>
 * </li>
 * <li>There is no time-based preemption</li>
 * </ul>
 * <p>The choice of leaving the current thread in the ready queue is not an
 * arbitrary one. By leaving the current thread in the ready queue it retains
 * it's position when preempted by a higher priority thread. So when a thread
 * is preempted, it will be the next to execute, once all the higher priority
 * threads complete.
 * <h3>Rescheduling, Context Switching and Atomicity</h3>
 * <p>Rescheduling, that is actual context switching, can occur manually or
 * automatically depending on the situation. The ready queue is marked as dirty
 * whenever a thread is added, removed or the queue is reordered. 
 * Access to the ready queue must be protected by calls to
 * {@link #setReschedulingEnabled} with an argument of <tt>false</tt>. 
 * When {@link #setReschedulingEnabled} is called with an argument of 
 * <tt>true</tt> then {@link #runNextThread} is called.
 * {@link #runNextThread} returns immediately if the ready queue is not dirty, 
 * otherwise it clears the dirty flag on the queue and causes a context switch.
 * <p>If a method removes the current thread from the ready queue it may need 
 * to call {@link #runNextThread} directly - <em>this is the <b>only</b>
 * time that <tt>runNextThread</tt> should be called directly as atomicity
 * depends on it.</em>.
 * <p>By seperating the queue modifications
 * from the actual context switch we allow for multiple queue changes to take
 * place atomically. By allowing the context switch to be deferred until
 * rescheduling is enabled we allow for calling contexts that can't invoke
 * <tt>runNextThread</tt> directly.
 * <p>As a thread might be preempted at any time due to the release of a higher
 * priority thread (waiting on I/O for example) then we need a means to
 * prevent preemption. Interference is avoided by invoking
 * <code>reschedulingEnabled(false)</code> before entering critical sections
 * and then restoring the rescheduling state after the critical section.
 * <b>It is the responsibility of the calling code to protect access to the
 * thread manager.</b>
 * <p>All methods that disable rescheduling or which are only called with
 * rescheduling disabled, declare PragmaNoPollcheck.
 * <p>If no thread is ready when a context switch must occur we invoke a 
 * special method of the event manager that causes the &quot;current&quot; 
 * thread to wait for an event that makes another thread ready to run (or the
 * same thread if, for example, it does a sleep). 
 *
 * @author David Holmes
 *
 */
public class BasicUserLevelThreadManagerImpl extends UserLevelThreadManagerCoreImpl
    implements OrderedUserLevelThreadManager {


    private static final boolean MEMORY_MANAGER_CALLBACK = true;
    private static final boolean DEBUG = false;

    /** The event manager if any */
    protected EventManager eventManager;

    /** The queue of threads ready to execute */
    private SingleLinkPriorityQueue readyQ;

    /** Flag to indicate the readyQ has been modified and so runNextThread
        should context switch
    */
    private volatile boolean readyQDirty = false;

    /** The current size of the ready queue */
    private volatile int readySize = 0;
    
    /**
     * The Comparator used to maintain the order of the ready queue.
     * By default threads go to the end of the queue.
     */
    java.util.Comparator comp = new java.util.Comparator() {
            public int compare(Object o1, Object o2) throws PragmaNoPollcheck {
                Native.print_string("Comparator in BasicUserLevel... used\n");
                return 0; // appear equal, so FIFO within a priority level
            }
        };

    /** Flag to signify whether rescheduling is currently enabled */
    protected volatile boolean schedulingEnabled = false; 

    /* Implementation of ThreadManager methods */


    // note that construction of service instances occurs at build time.
    public BasicUserLevelThreadManagerImpl() {
        // we set the EventManager at construction rather than in init()
        // because the steReschedulingEnabled method will be called during
        // the earlies phases of bootstrapping before we would want it to
        // be called. At that time init() has not run so we must have a
        // null check on eventManager. By initializing here we avoid the
        // null check. Note that we don't want to eventmanager to try and do
        // the same thing with us!
        eventManager = ((EventServicesFactory) IOServiceConfigurator.config
		.getServiceFactory(EventServicesFactory.name)).getEventManager();
        if (eventManager==null) {
            throw new ovm.util.OVMError.Configuration("no event manager!");
        }
    }


    /**
     * Completes the configuration of the thread manager by constructing
     * the internal queues now that the comparator to be used must have
     * been established.
     */
    public void start() {
        if (isStarted()) {
            throw new OVMError.IllegalState("Thread manager already started");
        }
        readyQ = new SingleLinkPriorityQueue(comp);
        assert readyQ != null : "readyQ null";
        super.start();
    }

    /* implementation of UserLevelThreadManager methods */

    /**
     * {@inheritDoc}
     * <p>This implementation returns immediately if the ready queue is not
     * dirty. If there is no ready thread then the current thread, while not
     * actually in the ready queue, continues executing to process any events
     * that may occur that may cause another thread to become ready.
     * <p><b>This method should only be called directly when the current
     * thread has been removed from the ready queue.</b>
     */
    public final void runNextThread() throws PragmaNoPollcheck, 
                                             PragmaNoBarriers {
                                             
        assert(!isReschedulingEnabled());

        if (!readyQDirty)
            return;

        readyQDirty = false;

        if (DEBUG) Native.print_string("runNextThread getting head...\n");
        OVMThread next = (OVMThread)readyQ.head();
        // wait while no thread to switch to
        if (DEBUG) Native.print_string("runNextThread waiting to thread to switch to...\n");
        
        if (next == null) {
        
          if (MEMORY_MANAGER_CALLBACK) {
            MemoryManager.the().runNextThreadHook( null, null );
          }
                  
          do {
            if (DEBUG) Native.print_string("runNextThread to call waitForEvents\n");
            eventManager.waitForEvents();
            next = (OVMThread)readyQ.head();
          } while (next == null);
          
        }
        if (DEBUG) Native.print_string("runNextThread got a thread to switch to\n");
        /* we always switch to the thread at the head of the ready queue,
         * but if that is the current thread then there's no need to do
         * anything.
         * Q: what costs more - doing a context switch to the current thread
         * or querying what is the current thread? -- DH
         */
        OVMThread current = getCurrentThread();
        if (DEBUG && false) {
          Native.print_string("runNextThread - current thread is ");
          Native.print_string(current.toString());
          Native.print_string(", next thread is ");
          Native.print_string(next.toString());
          Native.print_string(", ready queue size is ");
          Native.print_int(readySize);
          Native.print_string("\n");
        }
        
        if (MEMORY_MANAGER_CALLBACK) {
              MemoryManager.the().runNextThreadHook( current, next );
        }
        
        if (current == next) {
            if (DEBUG) Native.print_string("runNextThread switching to the current thread\n");
            return; // do nothing
        }
        else {
            if (DEBUG) Native.print_string("runNextThread switching to another thread\n");        
            runThread(next);
        }

        // note for the future:
        // to implement async interrupted exceptions, all we need to do is 
        // associate an exception object with the thread object and then check
        // if such an association exists while we are here.  if the association
        // exists, we simply throw that exception! (Filip)

        // It is not quite that simple as we must establish if the current 
        // thread is in an asynchronously interruptible region. Only then 
        // would the AIE be thrown when this thread becomes the current thread.
        // Otherwise we have to check for the AIE on method returns and on 
        // exit from synchronized blocks within AI methods.
        // And if the the current thread interrupts itself then we need an 
        // additional mechanism. 
        // Also note that "throw AIE" does not cause an asynchronous exception
        // which means we'd have to use processThrowable directly - DH
    }

    /**
     * Inserts the given thread into the ready queue, marking the queue dirty
     * in the process.
     * <p>This method in itself does not cause a context switch to occur. The
     * calling code may invoke {@link #runNextThread} to perform a context
     * switch, if suitable, otherwise the need for a context switch will be 
     * checked the next time rescheduling is enabled.
     *
     * @throws OVMError.IllegalState if the specified thread is already in the
     * ready queue.
     */
    public final void makeReady(OVMThread thread) throws PragmaNoPollcheck,
                                                         PragmaNoBarriers {
                                                         
//            BasicIO.err.print("Thread " + thread + "entered makeReady\n");
	assert schedulingEnabled == false: "scheduling enabled";
	assert readyQ != null: "readyQ null in makeReady";
	assert thread != null: "thread null";
	// only check this when assertions enabled, and don't throw
	// IllegalState
	assert !readyQ.contains((SingleLinkElement)thread);
//          BasicIO.err.print("Thread " + thread + " being added to ready queue\n");
        readyQ.add((SingleLinkElement)thread);
        readySize++;
        readyQDirty = true;
//          BasicIO.err.println("Added - Ready queue length = " + readySize);
    }


    /**
     * Removes the given thread from the ready queue, if it was present.
     * If the thread was removed then the ready queue is marked dirty.
     *
     * <p>This method in itself does not cause a context switch to occur. The
     * calling code may invoke {@link #runNextThread} to perform a context
     * switch, if suitable, otherwise the need for a context switch will be 
     * checked the next time rescheduling is enabled. If the thread being
     * removed is the current thread then it is recommended that, for clarity,
     * the calling code invoke {@link #runNextThread} explicitly - and in some
     * cases this may be essential for correctness.
     */
    public final boolean removeReady(OVMThread thread) 
        throws PragmaNoPollcheck, PragmaNoBarriers {
//          BasicIO.err.println("removeReady on thread " + thread);
        assert schedulingEnabled == false:  "scheduling still enabled";
        boolean result = readyQ.remove((SingleLinkElement)thread);
        if (result) {
//              BasicIO.err.print("Thread " + thread + " found and removed\n");
            readySize--;
            readyQDirty = true;
//              BasicIO.err.println("Ready queue length = " + readySize);
        }
        else {
//            BasicIO.err.print("Thread " + thread + " not found\n");
//            BasicIO.err.println("Ready queue length = " + readySize);
        }
        return result;
    }

    public final boolean isReady(OVMThread thread) throws PragmaNoPollcheck,
                                                          PragmaNoBarriers{
        assert schedulingEnabled == false:  "scheduling still enabled";
        return readyQ.contains((SingleLinkElement)thread);
    }

    public final int getReadyLength() {
        return readySize;
    }


    /**
     * {@inheritDoc}
     * <p>This implementation sets the enabled state of the event manager,
     * invokes {@link #runNextThread} if we are transitioning to
     * a state where rescheduling is enabled, and sets the current rescheduling
     * state.
     * <p><b>NOTE:</b> if this method is overridden by a subclass, the subclass
     * implementation should either invoke 
     * <tt>super.setReschedulingEnabled</tt> or else duplicate the 
     * functionality that is here.
     */
    // warning: don't do any fancy debugging here - low-level raw I/O
    // only, no allocation - otherwise infinite recursion will occur
    // And no exceptions due to sync in that code path
    public boolean setReschedulingEnabled(boolean enabled) 
        throws PragmaNoPollcheck, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaIgnoreExceptions {
        boolean temp = schedulingEnabled;

        // watch for deferred context switches - but don't restore enabled 
        // state until after the context switch, otherwise we can be left with
        // rescheduling/event-processing disabled while we run
        if (enabled && readyQDirty) // checking readyQDirty is an optimisation
            runNextThread();

        eventManager.setEnabled(enabled);
        schedulingEnabled = enabled;
        return temp;
    }

    public boolean setReschedulingEnabledNoYield(boolean enabled) 
        throws PragmaNoPollcheck, PragmaNoBarriers, PragmaAssertNoExceptions, PragmaIgnoreExceptions {

        boolean temp = schedulingEnabled;

        eventManager.setEnabled(enabled);
        schedulingEnabled = enabled;
        return temp;        
    }

    public boolean isReschedulingEnabled() throws PragmaNoPollcheck, PragmaAssertNoExceptions {
        return schedulingEnabled;
    }

    /* implementation of Ordered methods */

    /**
     * Sets the <code>Comparator</code> used to maintain the order of the
     * ready list.
     * <p>The comparator can only be set prior to starting the
     * thread manager.
     *
     * @param comp the <code>Comparator</code> to be used
     *
     * @throws OVMError.IllegalArgument if <code>comp</code> is <code>null</code>
     * @throws OVMError.IllegalState if attempts are made to set the comparator
     * after the thread manager has been started
     *
     * @see #init
     *
     */
    public void setComparator(java.util.Comparator comp) {
        if (comp == null) {
            throw new OVMError.IllegalArgument("comparator may not be null");
        }
        if (isStarted()) {
            throw new OVMError.IllegalState("thread manager already started");
        }
        this.comp = comp;
    }

    public java.util.Comparator getComparator() {
        return comp;
    }

    /**
     * {@inheritDoc}
     * <p>Any changes to the order being used by the comparator of this
     * thread manager, must be communicated to the thread manager, using
     * this method. This must be done atomically by disabling rescheduling
     * before hand.
     * <p>When this method is called the ready queue is marked as dirty, even
     * though there may not have been an actual change to the order.
     */
    public final void changeNotification(Object o) throws PragmaNoPollcheck,
                                                          PragmaNoBarriers {
        readyQ.changeNotification(o);
        readyQDirty = true; // may not be true but we can't tell
    }

}









