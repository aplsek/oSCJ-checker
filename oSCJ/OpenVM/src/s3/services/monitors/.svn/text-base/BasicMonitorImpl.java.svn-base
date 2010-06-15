 
package s3.services.monitors;

import java.util.Comparator;

import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.QueryableMonitor;
import ovm.services.monitors.RecursiveMonitor;
import ovm.services.threads.MonitorTrackingThread;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.Ordered;
import s3.util.queues.SingleLinkElement;
import s3.util.queues.SingleLinkPriorityQueue;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaInline;
import s3.core.domain.S3Domain;
/**
 * A basic implementation of a {@link Monitor} to provide basic support
 * for synchronized methods and blocks within OVM. As it is an OVM-level
 * monitor there are no condition queues as used with <code>Object.wait</code>
 * etc. It is based on user-level thread management with priority ordering
 * and assumes that there is no thread suspension.
 * We also provide additional
 * query interfaces to assist in debugging and testing.
 * <p>As with Java language monitors this monitor allows recursive entry by
 * the owning thread.
 * <p>We use a hand-off protocol such that releasing the monitor gives
 * ownership to an entering thread (if any) and makes that thread ready to
 * run.
 * <p>Customization hooks allow a subclass to perform special action on 
 * blocking and on monitor release exit. These could allow for extended
 * functionality like enforcing the priority inheritance protocol, or priority
 * ceiling protocol, for RTSJ; or for providing try-lock semantics for JSR-166.
 * <em>Monitor acquisition might also provide a customisation hook, but as we
 * don't have an example for this we're not sure what the semantics should be,
 * so we have omitted it until it is needed.</em>
 *
 *
 * @author David Holmes
 *
 */
public class BasicMonitorImpl extends ovm.core.OVMBase
    implements Monitor, Ordered, QueryableMonitor, RecursiveMonitor {

    /** Reference to current thread manager */
    protected static final UserLevelThreadManager tm;

    // This static initializer runs at image build time and expects to set
    // all the above static references from the current configuration.
    // Ideally this class would never get loaded except in a configuration
    // in which all the initialization is guaranteed to succeed, but that
    // is not yet the case. So this is written such that finding null values
    // or the wrong types of service instances, is not considered an error.
    // - DH 1 March 2005
    static {
        ThreadManager t = 
            ((ThreadServicesFactory)ThreadServiceConfigurator.config.
             getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
        if (t instanceof UserLevelThreadManager )
            tm = (UserLevelThreadManager) t;
        else
            tm = null;
    }


    /** The queue of threads trying to enter this monitor */
    protected SingleLinkPriorityQueue entryQ;

    /** The comparator used with our queues - obtained from the thread
     *  manager if not set explicitly
     */
    protected final Comparator comp;

    /** The current owner of this monitor */
    protected volatile OVMThread owner = null;

    /** The number of times the owner has locked this monitor */
    protected volatile int entryCount = 0;

    /** The size of the entry queue */
    protected volatile int entrySize = 0;

    /* constructors are not public because construction should occur via
     * a factory.
     */

    /**
     * Construct a monitor using the default comparator as configured in
     * the current thread manager. If the current thread manager is not
     * an {@link Ordered} thread manager then an exception is thrown.
     * @throws ClassCastException if the thread manager is not 
     * {@link Ordered}
     */
    protected BasicMonitorImpl() {
        this(((Ordered)tm).getComparator());
    }

    /**
     * Construct a monitor using the supplied comparator for maintaining
     * the entry and wait queues.
     *
     */
    protected BasicMonitorImpl(Comparator comp) {
	assert  comp != null: "null comparator";
        this.comp = comp;
        entryQ = new SingleLinkPriorityQueue(comp);
    }


    /** The factory object */
    public static final Monitor.Factory factory = new Factory();

    /**
     * The factory class for creating monitors 
     *
     */
    public static class Factory extends ovm.core.OVMBase 
        implements Monitor.Factory
    {
        public void initialize() {}

        public Monitor newInstance() {
            return new BasicMonitorImpl();
        }
        public int monitorSize() {
            return BasicMonitorImpl.sizeOf();
        }
    }


    /**
     *  Returns the actual size of an instance of this class, including the
     *  space needed for the object header and all fields, plus the space
     *  needed for creating referenced objects (and transitively the space
     *  they need to create referenced objects) during construction.
     */
    static int sizeOf() {
        return 
            S3Domain.sizeOfInstance("s3/services/monitors/BasicMonitorImpl")
            + constructionSizeOf();
    }


    /**
     * Returns the maximum space allocated during the execution of the
     * constructor of an instance of this class, and transitively the space
     * needed by any object allocation performed in this constructor.
     * Note this doesn't include "temporary" allocations like debug strings
     * etc, but it does include super constructors. Hence for any class the
     * total space needed to do "new" is the base size plus the construction
     * size.
     */
    protected static int constructionSizeOf() {
        // the only object constructed is SingleLinkPriorityQueues.
        // That class performs no allocation during construction so we just
        // need its base size.
        return S3Domain.sizeOfInstance("s3/util/queues/SingleLinkPriorityQueue");
    }


    /** Flag for controlling debug output */

    public static final boolean DEBUG = false;

    /* customization hooks */

    /**
     * Executed when a thread trying to acquire the monitor finds that the
     * monitor is already owned. This method is called before the monitor
     * state or the thread state are altered. If the method returns 
     * <tt>true</tt> then we continue with the blocking, otherwise we
     * abort the monitor acquisition.
     * <p><b>FIXME</b>: the pieces aren't quite in place to do tryLock yet.
     *
     * @param current the thread that is trying to acquire the monitor
     * @return <tt>true if the monitor acquisition should continue to block
     * the current thread, and <tt>false</tt> if the monitor acquisition should
     * be aborted.
     */
    protected boolean onBlocking(OVMThread current) throws PragmaNoPollcheck { 
        return true;
    }


    /**
     * Executed when the monitor is about to be released and there are waiting
     * threads. This method is called
     * before the monitor, or any waiting thread's state, is modified.
     * The current thread is the monitor's owner.
     *
     */
    protected void onExit() throws PragmaNoPollcheck {
    }
    
    /**
     * Executed when the monitor has been handed off to a waiting thread, but
     * before the new owner is made ready
     */
    protected void onAcquire() throws PragmaNoPollcheck {
    }


    /* Implementation of Monitor methods */

    public void enter(OVMThread thr) {
	if (owner == null) {
	    owner = thr;
	    entryCount = 1;
	} else {
	    assert owner == thr: "enter(thread) invoked on locked monitor";
	    entryCount++;
	}
    }


    public void enter(){
        OVMThread current = tm.getCurrentThread();
        if (DEBUG) 
            BasicIO.out.println("Monitor entry by thread " + current);
        if (enterQuick(current)) {
            return;
        }
        else if (owner == current) {
            // recursive entry doesn't require atomicity
            if (DEBUG) BasicIO.out.println("\tdoing recursive entry: count = " +entryCount);
            enterRecursive();
            if (DEBUG) BasicIO.out.println("\tdid recursive entry: count = " +entryCount);
        }
        else { // need atomicity
            boolean enabled = tm.setReschedulingEnabled(false);
            try {
                if (DEBUG) BasicIO.out.println("\tdoing initial entry");
                doEnter(current);
                if (DEBUG) BasicIO.out.println("Monitor acquired by thread " + current);
            }
            finally {
                tm.setReschedulingEnabled(enabled);
            }
        }
    }


    /**
     * Atomically check-for and acquire an unowned monitor. This method uses
     * the fastest atomicity mechanism available - which currently means no
     * poll checks.
     */
    private final boolean enterQuick(OVMThread current) 
        throws PragmaNoPollcheck, PragmaInline,
               ovm.core.services.memory.PragmaNoBarriers {
        if (owner == null) {
            owner = current;
            entryCount = 1;
            if (DEBUG) BasicIO.out.println("\tacquiring unowned monitor-quick");
            return true;
        }
        return false;
    }



    /**
     * Provides the common mechanics used for monitor entry. This should only
     * be called from within an atomic region.
     */
    protected void doEnter(OVMThread current) {
        if (owner == null) {
            if (DEBUG) BasicIO.out.println("\tacquiring unowned monitor");
            owner = current;
            entryCount = 1;
        }
        else {
            if (DEBUG) BasicIO.out.println("\tmonitor is owned - calling on-blocking");
            if (!onBlocking(current)) {
                if (DEBUG) BasicIO.out.println("\ton-blocking returned false - returning");

                return;
            }
            if (DEBUG) BasicIO.out.println("\ton-blocking returned true - continuing");
            tm.removeReady(current); // must remove first
            entryQ.add((SingleLinkElement)current);
            entrySize++;
            if (current instanceof MonitorTrackingThread) {
                ((MonitorTrackingThread)current).setWaitingMonitor(this);
            }
            if (DEBUG) BasicIO.out.println("\tthread queued: size = " +entrySize);
            tm.runNextThread(); // switch to runnable thread
            // when we return we are the owner
	    assert owner == current && entryCount == 1: "wrong state on return";
        }
    }


    public void enterRecursive() throws PragmaNoPollcheck, PragmaInline {
        // assert: owner = currentThread 
        entryCount++;
    }

    public void exit() {
        OVMThread current = tm.getCurrentThread();
        if (DEBUG) BasicIO.out.println("Monitor exit by thread " + current);
        checkOwner(current);
        if (DEBUG) BasicIO.out.println("\tcurrent thread is owner");

        if (entryCount == 1) {
            if (exitQuick()) {
                return;
            }
            else {
                // need atomicity
                boolean enabled = tm.setReschedulingEnabled(false);
                try {
                    if (DEBUG) BasicIO.out.println("\tdoing final exit");
                    doExit();
                    if (DEBUG) BasicIO.out.println("Monitor released by thread " + current);
                }
                finally {
                    tm.setReschedulingEnabled(enabled);
                }
            }
        }
        else {
            if (DEBUG) 
                BasicIO.out.println("\t> recursive exit: entryCount " + 
                                    entryCount);
            exitRecursive();
            if (DEBUG) 
                BasicIO.out.println("\t< recursive exit: entryCount " + 
                                    entryCount);
        }
    }


    /**
     * Atomically release an uncontended monitor. This method uses
     * the fastest atomicity mechanism available - which currently means no
     * poll checks.
     */
    private final boolean exitQuick() 
        throws PragmaNoPollcheck, PragmaInline,
               ovm.core.services.memory.PragmaNoBarriers {
        if (entrySize == 0) {
            owner = null;
            entryCount = 0;
            if (DEBUG) 
                BasicIO.out.println("\tNo waiters - releasing monitor quick");
            return true;
        }
        return false;
    }


    public void exitRecursive() {
        // assert: entryCount > 1
        entryCount--;
    }

    /* this doesn't need sync as owner cannot change to be the current
       thread, nor can it change from being the current thread
    */
    protected void checkOwner(OVMThread current) 
        throws IllegalMonitorStateException {
        if (owner != current) {
            throw new IllegalMonitorStateException();
        }
    }

    /** perform the common mechanics of releasing the monitor. This should
     * only be called from an atomic region by the owning thread.
     */
    protected void doExit() 
        throws ovm.core.services.memory.PragmaNoBarriers,
               PragmaNoPollcheck {
	assert entryCount == 1 : "releasing too early";
        
        if (entrySize > 0) {
            if (DEBUG) BasicIO.out.println("\tcalling onExit as entrySize>0");
            onExit();
            if (DEBUG) {
                BasicIO.out.println("\tonExit completed");
                BasicIO.out.println("\twaking up queued thread: entrySize = " +
                                    entrySize);
            }
            owner = (OVMThread) entryQ.take();
            if (DEBUG) BasicIO.out.println("\tnew owner is " + owner);
            onAcquire();
            entrySize--;
            if (owner instanceof MonitorTrackingThread) {
                ((MonitorTrackingThread)owner).setWaitingMonitor(null);
            }
            tm.makeReady(owner);
        }
        else {
            if (DEBUG) BasicIO.out.println("\tNo waiters - releasing monitor");
            owner = null;
            entryCount = 0;
        }
    }


    public int getEntryQueueSize() {
        return entrySize;
    }

    public OVMThread getOwner() {
        return owner;
    }

    public boolean isEntering(OVMThread t) {
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            return entryQ.contains((SingleLinkElement)t);
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }

    public int entryCount() {
        return entryCount;
    }

    /* Implementation of Ordered methods */

    /**
     * The comparator must be set via a constructor argument or else the
     * default thread manager comparator is used.
     * @throws IllegalStateException always
     *
     */
    public void setComparator(Comparator comp) {
        throw new IllegalStateException("comparator can not be set");
    }


    public Comparator getComparator() {
        return comp;
    }


    public void changeNotification(Object o) {
         entryQ.changeNotification(o);
    }

    // additional utility methods

    public SingleLinkPriorityQueue getEntryQueue() { return entryQ; }

}    

