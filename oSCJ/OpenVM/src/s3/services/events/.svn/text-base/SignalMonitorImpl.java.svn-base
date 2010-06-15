
package s3.services.events;

import ovm.core.execution.NativeInterface;
import ovm.core.execution.OVMSignals;
import ovm.core.services.events.EventManager;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.threads.OVMThread;
import ovm.core.stitcher.EventServicesFactory;
import ovm.core.stitcher.IOServiceConfigurator;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.ServiceInstanceImpl;
import ovm.services.events.SignalMonitor;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.ArrayList;
import ovm.util.OVMError;
import s3.util.PragmaNoPollcheck;

/**
 * The signal monitor implementation allows a thread to block until a
 * specified signal occurs, or to install a signal handler.
 *
 * <h3>Restrictions on Signal Handlers</h3>
 * <p>Signal handlers are executed atomically during event processing. This
 * means that 
 * <ul>
 * <li>They should be very short in length (or else rarely used, or
 * for debugging - such as a thread dump); 
 * <li>They <b>must not block</b>
 * <li>By virtue of the previous statement they must not perform
 * synchronization unless it is guaranteed that the monitor lock is free.
 * </ul>
 * <p><b>Warning:</b> Signal handlers execute in the context of an arbitrary
 * thread, so no &quot;thread local&quot; data of any kind should be used.
 *
 * <h3>Memory Management</h3>
 * <p>If memory management were not an issue we'd use data structures that
 * grew on demand. However, growing key data structures in the face of
 * scoped memory and no-heap threads implies the only place to grow safely
 * is into Immortal memory.
 * <p>While this service isn't generally aware of how many clients may use it,
 * a particular Vm configuration is (or should be). Hence we can pre-allocate
 * data structures that are large enough for the current configuration and
 * simply throw an error if we exceed the limits. In the current RTSJ JVM
 * config we have two user-domain system threads that wait for signals, and
 * a single potential ED signal-handler (for thread dumps). The default size of
 * 8 more than accommodates this. (March 2005)
 *
 * @author David Holmes
 */
public class SignalMonitorImpl extends ServiceInstanceImpl
    implements SignalMonitor, EventManager.EventProcessor {

    /** Native interface helper class */
    private static final class SignalHelper implements NativeInterface {

        static native int initSignalMonitor();
        static native void releaseSignalMonitor();

        /** Register interest in the given signal */
        static native boolean registerSignal(int sig);

        /** Register interest in all specified signals */
        static native boolean registerSignalVector(int[] sigs, int siglen);

        /** Unregister interest in the given signal */
        static native void unregisterSignal(int sig);

        /** Unregister interest in all specified signals */
        static native void unregisterSignalVector(int[] sigs, int siglen);

        /** Retrieve the fire counts for each signal */
        static native boolean getFiredSignals(int[] sigs, int siglen);

        /** query if the given signal can be waited upon */
        static native boolean canMonitorSignal(int sig);
    }
    
    private final static SignalMonitor instance = new SignalMonitorImpl();
    
    public static SignalMonitor getInstance() {
        return instance;
    }
    
    /**
     * Are we started?
     */
    protected volatile boolean started = false;

    /**
     * Are we stopped?
     */
    protected volatile boolean stopped = false;

    /** 
     * Reference to the thread manager being used 
     */
    protected UserLevelThreadManager tm;

    /** 
     * Reference to the event manager being used 
     */
    protected EventManager em;


    /**
     * Helper class for queuing threads and handlers
     */
    static class Node {
        Object target; // the thread or the signal handler - could be scoped!
        int[] sigVector;  // signal vector for thread; or
        int sig = -1;     // single signal - firecount on return for thread
    }

    /** Cache of nodes to avoid allocation
    */
    private Node[] nodes;

    /** Maximum number of users of this service we allow for */
    private static final int MAX_USERS = 8;

    /** The number of free nodes: the next free node is always
        length-freeCount as we keep free nodes together at the tail.
     */
    private int freeCount;

    /**
     * Get a node from the pool, growing the pool if needed
     */
    private Node getNode() {
        if (freeCount == 0) {
            throw new OVMError.IllegalState("SignalMonitorImpl ran out of internal space");
        }
        else {
            return nodes[nodes.length - freeCount--];
        }
    }

    /**
     * Return the given node to the pool
     */
    void freeNode(Node n) {
        for(int i = 0; i < nodes.length; i++) {
            if (nodes[i] == n) {
                nodes[i] = nodes[nodes.length - ++freeCount];
                nodes[nodes.length - freeCount] = n;
                n.target = null;
                n.sigVector = null;
                n.sig = -1;
                return;
            }
        }
        throw new OVMError("Node " + n + " not found in node list");
    }


    /** The fired signals - filled in by native code in OVM signal order */
    int[] firedSignals;

    /** The monitored signals: set of threads/handlers waiting on each signal.
     */
    ArrayList[] monitoredSignals;

    public String eventProcessorShortName() {
	return "sigmon";
    }


    public SignalMonitorImpl() {
        firedSignals = new int[OVMSignals.NSIGNALS];
        monitoredSignals = new ArrayList[OVMSignals.NSIGNALS];
        for(int i = 0; i < monitoredSignals.length; i++) {

            // NOTE: these ArrayLists will never expand as we have to run
            // out of nodes first
            monitoredSignals[i] = new ArrayList(MAX_USERS);
        }

        nodes = new Node[MAX_USERS];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new Node();
            //            BasicIO.out.println("Constructed node " + nodes[i]);
        }

        freeCount = nodes.length;
    }

    /*
      Here's the basic approach:
      
      When a thread waits for a set of signals, or a handler is added,
      it is allocated a node from the freepool and that node is added 
      to the arraylist for each signal being monitored.

      When a signal fires we remove a node from the arraylist for that signal
      and update the signal vector with the fire count for that
      signal. If the node is for a thread we then have to do two things:

         1. We have to remove that thread's node from all arraylists for
            every other signal that it was waiting upon - whether they
            fired or not.

         2. We have to capture the fire counts for all the other signals
            it was waiting upon.

      If the node is for a handler then we execute it's fire() method.
    */

    // debugging flag
    int firing = 0;

    // this is executed atomically by the event manager with further event
    // processing disabled
    public void eventFired() throws PragmaNoPollcheck {

        if (!SignalHelper.getFiredSignals(firedSignals, firedSignals.length)) {
            // nothing fired
            return;
        }
        firing++;
        for (int i = 0; i < firedSignals.length; i++) {
            int count = firedSignals[i];
            if (count > 0) {
//                 BasicIO.out.println(firing + " Processing fired signal " + i+ " with count " + count);
                int size = monitoredSignals[i].size();
//                 BasicIO.out.println("Number of targets registered: " + size);

                for (int j = 0, index = 0; j < size; j++, index++) {
//                     BasicIO.out.println(j + ": index " + index);
                    Node n = (Node)monitoredSignals[i].get(index);
                    if (n.target instanceof OVMThread) {
//                         BasicIO.out.println("Processing thread " + n.target);
                        // remove the node for threads and adjust the index
                        monitoredSignals[i].remove(index--);
                        // unregister the signals this thread was waiting for
                        if (n.sigVector != null) {
                            SignalHelper.unregisterSignalVector(n.sigVector, 
                                                                n.sigVector.length);
                            
                            // update the fire count for this sig in this node
                            n.sigVector[i] = count;
                            
                            // process all the other signals, if any, 
                            // this node is queued for
                            processThreadNode(n, firedSignals, i);
                        }
                        else {
			    assert n.sig != -1: "null vector and -1 sig value";
                            SignalHelper.unregisterSignal(n.sig);
                            n.sig = count;
                        }

                        // FIXME: Thread state
                        // now unblock the thread
                        tm.makeReady((OVMThread)n.target);
                    }
                    else { // it's a handler
//                         BasicIO.out.println("Processing handler " + n.target);
                        SignalHandler h = (SignalHandler) n.target;

                        try {
                            h.fire(i, count);
                        }
                        catch(Throwable t) {
                            BasicIO.err.println("Unhandled exception in signal handler");
                            t.printStackTrace();
                        }
                        // the handler may have removed itself so we have to
                        // adjust the index if necessary
                        if (index == monitoredSignals[i].size() ||
                            monitoredSignals[i].get(index) != n) {
                            index--;
                        }
                    }
                }
            }
        }
        // reset the array for next time.
        for (int i = 0; i < firedSignals.length; i++)
            firedSignals[i] = 0;
    }


    // only called on thread nodes using a sigVector
    void processThreadNode(Node n, int[] firedSignals_ , int fireIndex) 
        throws PragmaNoPollcheck {
        // the fireIndex tells us which entry in firedSignals we were
        // processing. So when we run through the thread's signal vector
        // looking for 1's, we skip the given index as it already holds
        // the updated fire count
        // Remember the signal vector can be smaller than the total number 
        // of signals
        for (int i = 0; i < n.sigVector.length; i++) {
            if (i == fireIndex) continue;
            if (n.sigVector[i] == 1) {
                // remove from the other lists
                monitoredSignals[i].remove(n);
                // update the fire count - which might be zero
                n.sigVector[i] = firedSignals_[i];
            }
        }
    }



    public void addSignalHandler(SignalHandler handler, int sig) {
        if (!canMonitor(sig)) {
            throw new OVMError.IllegalArgument("Can't monitor signal " + 
                                               OVMSignals.sigNames[sig]);
        }

        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            if (SignalHelper.registerSignal(sig)) {
                Node n = getNode();
                n.target = handler;
                n.sigVector = null;
                n.sig = sig;
                monitoredSignals[sig].add(n);
            }
            else {
                throw new Error("Unable to add signal handler");
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }

    // the handler must be able to remove itself when its running

    public void removeSignalHandler(SignalHandler handler, int sig) {
        if (sig < 0 || sig >= OVMSignals.NSIGNALS) {
            throw new OVMError.IllegalArgument("Invalid signal value");
        }
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            ArrayList l = monitoredSignals[sig];
            for (int i = l.size()-1; i >= 0; i--) {
                Node n = (Node)l.get(i);
                if (n.target == handler) {
                    l.remove(i);
                    freeNode(n);
                    SignalHelper.unregisterSignal(sig);                    
                    break;
                }
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }

    // note: a thread calling this might be scope allocated due to the
    // policy of not allocating ED threads in the heap.
    public int waitForSignal(int sig) throws PragmaNoBarriers {
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            // with only one sig we can register first
            if (SignalHelper.registerSignal(sig)) {
                Node n = getNode();
                try {
                    OVMThread current = tm.getCurrentThread();
                    n.target = current;
                    n.sigVector = null;
                    n.sig = sig;
                    monitoredSignals[sig].add(n);
                    
                    // FIXME: thread state
                    tm.removeReady(current);
                    tm.runNextThread(); // switch to runnable thread
                    return n.sig;
                }
                finally {
                    freeNode(n);
                }
            }
            else {
                return 0;
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }


    public boolean waitForSignal(int[] sigVector) throws PragmaNoBarriers {
        if (sigVector.length > OVMSignals.NSIGNALS) {
            throw new ArrayIndexOutOfBoundsException();
        }
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            Node n = getNode();
            try {
                OVMThread current = tm.getCurrentThread();
                n.target = current;
                n.sigVector = sigVector;
                boolean mustWait = false;
                for (int i = 0; i < sigVector.length; i++) {
                    if (sigVector[i] != 0) {
                        monitoredSignals[i].add(n);
                        mustWait = true;
                    }
                }
                if (mustWait) {
                    if (SignalHelper.registerSignalVector(sigVector, 
                                                           sigVector.length)) {
                        
                        // FIXME: thread state
                        tm.removeReady(current);
                        tm.runNextThread(); // switch to runnable thread
                        return true;
                    }
                    else {
                        // remove us from the monitored signals
                        for (int i = 0; i < sigVector.length; i++) {
                            monitoredSignals[i].remove(n);
                        }
                        return false;
                    }
                }
                else { // empty mask
                    return true;
                }
            }
            finally {
                freeNode(n);
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }



    public boolean canMonitor(int sig) {
        if (sig < 0 || sig >= OVMSignals.NSIGNALS) {
            return false;
        }
        else {
            return SignalHelper.canMonitorSignal(sig);
        }
    }


    /** 
     * Initialisation of the signal manager simply involves grabbing
     * a thread manager and event manager.
     */
    public void init() {
        tm = (UserLevelThreadManager)
            ((ThreadServicesFactory)ThreadServiceConfigurator.config.
             getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
        if (tm == null) {
            throw new OVMError.Configuration("need a configured thread manager");
        }

        em = ((EventServicesFactory)IOServiceConfigurator.config.
         getServiceFactory(EventServicesFactory.name)).getEventManager();
        if (em == null) {
            throw new OVMError.Configuration("need a configured event manager");
        }

        isInited = true;
    }


    /**
     * Starts the signal manager by initialising the native resources,
     * and registering with the event manager.
     * @throws OVMError.IllegalState {@inheritDoc}
     */
    public void start() {
        if (started) {
            throw new OVMError.IllegalState("signal manager already started");
        }

        if (SignalHelper.initSignalMonitor() != 0) {
            throw new OVMError("error initializing the native signal monitor");
        }
        em.addEventProcessor(this);

        started = true;
        d("SignalMonitor has been started");
    }

    public void stop() {
        if (!started) {
            return;
        }
        em.removeEventProcessor(this);
        SignalHelper.releaseSignalMonitor();
        stopped = true;
    }

    public boolean isRunning() {
        return started & !stopped;
    }

    /**
     * @throws OVMError.IllegalState if the signal manager has not been stopped
     */
    public void destroy() {
        if (isRunning()) {
            throw new OVMError.IllegalState("must stop signal manager first");
        }
    }


    static final int NOT_WATCHED = -1;

    /** Inner class to implement SignalWatcher */
    class SignalWatcherImpl implements SignalWatcher {

        // true means we watch the signal
        boolean[] watched = new boolean[OVMSignals.NSIGNALS];

        // number of signals watched
        int watchCount = 0;

        // keep track of signal occurrences
        int[] counts = new int[OVMSignals.NSIGNALS];

        // mark all signals as not watched
        {
            for (int i = 0; i < counts.length; i++) {
                counts[i] = NOT_WATCHED;
            }
        }

        boolean fired = false;

        OVMThread waiter = null;  // could be scoped!

        // the signal handler is executed atomically within event processing
        SignalHandler handler = new SignalHandler() {
                public void fire(int sig, int count) {
                    // update count
                    counts[sig] += count;
                    fired = true;
                    // wakeup any waiting thread
                    if (waiter != null) {
                        tm.makeReady(waiter);
                        waiter = null;
                    }
                }
            };

        // Because all methods interact with the counts array that is updated
        // by the signal handler, we have to disable rescheduling to get
        // atomicity across everything. There are more sophisticated ways to
        // deal with this but this will do for now. Note that the handler
        // can not use synchronization as it must never block.

        public void addWatch(int sig) {
            if (!SignalMonitorImpl.this.canMonitor(sig)) {
                throw new OVMError.IllegalArgument("invalid signal");
            }
            boolean enabled = tm.setReschedulingEnabled(false);
            try {
                if (!watched[sig]) {
                    SignalMonitorImpl.this.addSignalHandler(handler, sig);
                    watched[sig] = true;
                    watchCount++;
                    assert(counts[sig] == NOT_WATCHED);
                    counts[sig] = 0;
                }
            }
            finally {
                tm.setReschedulingEnabled(enabled);
            }
        }

        public void removeWatch(int sig) {
            if (sig < 0 || sig >= OVMSignals.NSIGNALS) {
                throw new OVMError.IllegalArgument("invalid signal");
            }
            boolean enabled = tm.setReschedulingEnabled(false);
            try {
                if (watched[sig]) {
                    SignalMonitorImpl.this.removeSignalHandler(handler, sig);
                    watched[sig] = false;
                    watchCount--;
                    if (watchCount == 0) {
                        // wakeup any waiting thread
                        if (waiter != null) {
                            tm.makeReady(waiter);
                            waiter = null;
                        }
                    }
                    assert(counts[sig] != NOT_WATCHED);
                    counts[sig] = NOT_WATCHED;
                }
            }
            finally {
                tm.setReschedulingEnabled(enabled);
            }
        }


        public void clearAllWatches() {
            boolean enabled = tm.setReschedulingEnabled(false);
            try {
                for (int i = 0; i < watched.length; i++) {
                    watched[i] = false;
                    counts[i] = NOT_WATCHED;
                }
                watchCount = 0;
            }
            finally {
                tm.setReschedulingEnabled(enabled);
            }
        }


        // array used by getWatchCounts
        int[] out = new int[OVMSignals.NSIGNALS];

        /**
         * {@inheritDoc}.
         * <p>This method is not thread-safe. It returns an internal array
         * that will be reused on subsequent calls to this, or
         * {@link #waitForSignal}.
         */
        public int[] getWatchCounts() {
            boolean enabled = tm.setReschedulingEnabled(false);
            try {
                for (int i = 0; i < counts.length; i++) {
                    // copy all values over including NOT_WATCHED as we may
                    // have removed a watch since the last call
                    out[i] = counts[i];
                    // zero watched counts
                    if (counts[i] != NOT_WATCHED) {
                        counts[i] = 0;
                    }
                }
                fired = false;
                return out;
            }
            finally {
                tm.setReschedulingEnabled(enabled);
            }
        }

        /**
         * {@inheritDoc}.
         * <p>This method is not thread-safe. It returns an internal array
         * that will be reused on subsequent calls to this, or
         * {@link #getWatchCounts}.
         */
        public int[] waitForSignal() throws PragmaNoBarriers {
            boolean enabled = tm.setReschedulingEnabled(false);
            try {
                if (!fired && watchCount > 0) {
                    OVMThread current = tm.getCurrentThread();
                    waiter = current;
                    tm.removeReady(current);
                    tm.runNextThread(); // switch to runnable thread
                }
                // The thread returns when a watched signal fires, or when all
                // watches have been removed
                return getWatchCounts();
            }
            finally {
                tm.setReschedulingEnabled(enabled);
            }
        }

    }


    /**
     * Returns a signal watcher for this signal monitor. The signal watcher
     * is not thread-safe and should only be used by a single thread.
     */
    public SignalWatcher newSignalWatcher() {
        return new SignalWatcherImpl();
    }
    
}

