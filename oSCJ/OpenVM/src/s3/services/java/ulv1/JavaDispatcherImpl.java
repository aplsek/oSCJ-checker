
package s3.services.java.ulv1;

import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Field;
import ovm.core.domain.JavaUserDomain;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.timer.TimeConversion;
import ovm.core.stitcher.JavaServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.services.java.JavaDispatcher;
import ovm.services.java.JavaMonitor;
import ovm.services.java.JavaOVMThread;
import ovm.services.java.JavaUserLevelThreadManager;
import ovm.services.threads.PriorityOVMThread;
import ovm.util.OVMError;
import s3.services.threads.PriorityDispatcherImpl;
import s3.util.PragmaNoPollcheck;
import ovm.core.domain.ReflectiveField;
import ovm.core.repository.JavaNames;

/** 
 * An implementation of the {@link JavaDispatcher} utilising a user-level 
 * thread manager. Although fairly general, assumptions needs to made about
 * how the specific thread manager behaves, so this is implementation 
 * dependent (unless we change the service specification to match those of 
 * the implementation). We extend the {@link PriorityDispatcherImpl} to
 * pick up the basic OVM threading capabilities, overriding where necessary
 * to support the Java threading semantics, or to take advantage of the use
 * of {@link JavaOVMThread}.
 * <p>This dispatcher is tightly coupled with the {@link java.lang.Thread} 
 * implementation in that we rely on knowledge of when the thread object is 
 * locked at the Java level. By locking the Thread object we ensure the 
 * thread can't terminate while we are processing it. However, there
 * is a problem with setting the priority as we must not hold the lock at 
 * that time, to prevent a priority inversion from occurring. In that case we 
 * detect a terminated thread and ignore the request.
 * <p>General thread-safety is achieved by disabling rescheduling during 
 * critical sections, using the facilities of the thread manager. We don't 
 * know whether or not our thread manager supports time-based preemption but 
 * the atomicity requirements are the same regardless.
 * <p>All methods that disable rescheduling or which are only called with
 * rescheduling disabled, declare PragmaNoPollcheck.
 * <p>We expect to deal with stateful threads and so set the state accordingly.
 * <p>This is a singleton class that conforms to the 
 * {@link ovm.services.ServiceInstance ServiceInstance} interface.
 *
 * <h3>Thread Suspension</h3>
 * <p>Is not supported. An earlier scheme that utilised an ultra-low priority
 * for the suspended thread has been abandoned as it is not compatible with
 * priority inheritance.
 *
 *
 * @see ovm.services.java.JavaUserLevelThreadManager
 */
public class JavaDispatcherImpl 
    extends PriorityDispatcherImpl
    implements JavaDispatcher{

    /** The singleton instance of this class */
    final static JavaDispatcher instance = new JavaDispatcherImpl();

    /**
     * Return the singleton instance of this class 
     * @return the singleton instance of this class 
     */
    public static OVMDispatcher getInstance() {
        return instance;
    }


    /** no construction allowed */
    protected JavaDispatcherImpl() {}

    /** Our thread manager implementation */
    protected JavaUserLevelThreadManager jtm;

    /** The Java OVM thread factory for the current configuration. */
    protected JavaOVMThread.Factory factory;

    /**
     * Initialize the Java Dispatcher, binding it to the currently configured 
     * thread manager.  This has an ordering
     * dependency with the thread manager as the thread manager's
     * comparator must be set before the manager is initialised.
     *
     * @throws OVMError.IllegalState if the thread manager has already been
     * initialised.
     * @throws ClassCastException if the configured thread manager is not a 
     * {@link JavaUserLevelThreadManager}.
     * @throws OVMError.Configuration if any required services are missing 
     * from the configuration
     */
    public void init() {
        super.init();
        jtm = (JavaUserLevelThreadManager)tm;
        JavaServicesFactory jsf = (JavaServicesFactory)
            ThreadServiceConfigurator.config.getServiceFactory(
                JavaServicesFactory.name);
        if (jsf == null) {
            throw new OVMError.Configuration("javaservicesfactory null");
        }
        else {
//            ovm.core.services.io.BasicIO.out.println("Using Java Services factory: " + jsf);
        }

        factory = jsf.getJavaOVMThreadFactory();

        if (factory == null) {
            throw new OVMError.Configuration("JavaOVMThreadFactory null");
        }
        else {
//            ovm.core.services.io.BasicIO.out.println("Using Java OVM Thread factory: " + factory);
        }
        assert jtm.canWakeUp() : "Java thread manager should support wakeup";
        // sanity check
        if (this != jsf.getJavaDispatcher()) {
            throw new OVMError.Configuration("this != configured dispatcher");
        }

        isInited = true;
    }

    /**
     * Does nothing. The Java Dispatcher does not require explicit starting
     */
    public void start() {}

    /**
     * Does nothing. The Java Dispatcher does not require explicit stopping
     */
    public void stop() {}

    /**
     * Resets the Java dispatcher, revoking all associations in the current 
     * configuration. After invoking this method any use of the dispatcher is 
     * likely to result in a <code>NullPointerException</code>
     */
    public void destroy() {
        jtm = null;
        factory = null;
    }

    /* ---- Actual service methods --- */

    /* We inherit all of the interface defined doc comments */

    public Oop getCurrentJavaThread() {
        JavaOVMThread current = (JavaOVMThread) jtm.getCurrentThread();
        assert current.getJavaThread() != null: "current thread not bound";
        return current.getJavaThread();
    }

    public JavaOVMThread createVMThread(Oop javaThread) {
        return factory.newInstance(javaThread);
    }

    public JavaOVMThread getCurrentVMThread() {
        return (JavaOVMThread)jtm.getCurrentThread();
    }
    
    private final long MAX_SAFE_SLEEP_NANOS = Long.MAX_VALUE;
    
    public boolean delayCurrentThread(long millis, int nanos) 
        throws PragmaNoPollcheck {
        long sleepTime = nanos + millis*TimeConversion.NANOS_PER_MILLI;
        
        while (sleepTime < 0) { // handle the unlikely case that someone wants to sleep for very long
                                // this is actually a bugfix - long sleep was used for infinite blocking
                                // wait in an SCJ implementation...
            boolean notInterrupted = delayCurrentThread( MAX_SAFE_SLEEP_NANOS );
            if (!notInterrupted) {
                return false;
            }
            
            long sleptMillis = MAX_SAFE_SLEEP_NANOS / TimeConversion.NANOS_PER_MILLI;
            long sleptNanos = MAX_SAFE_SLEEP_NANOS - ( sleptMillis * TimeConversion.NANOS_PER_MILLI );
            
            sleepTime = (nanos - sleptNanos) + (millis - sleptMillis)*TimeConversion.NANOS_PER_MILLI;
        }
             
        return delayCurrentThread(sleepTime);
    }
        
    protected boolean delayCurrentThread(long sleepTime) {  /* in nanoseconds */
    
        JavaOVMThread current = (JavaOVMThread) jtm.getCurrentThread();
        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            // Need to check for interruption
            if (current.isInterrupted()) {
                return false;
            }
            else {
                // this is how we handle sleep(0) - as a no-op
                // note: we do this after the interrupt check
                if (sleepTime == 0) {
                    return true;
                }
                current.setState(JavaOVMThread.BLOCKED_SLEEP);
                jtm.sleep(current, sleepTime);
                assert current.getState() == JavaOVMThread.READY:
		    "thread not ready after relative sleep";
                if (current.getUnblockState() == 
                          JavaOVMThread.UNBLOCKED_INTERRUPTED) {
                    current.setUnblockState(JavaOVMThread.UNBLOCKED_NORMAL);
                    return false;
                }
                else {
                    assert (current.getUnblockState() == 
			    JavaOVMThread.UNBLOCKED_NORMAL):
                        "wrong unblock state";
                    return true;
                }
            }
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }
    }


    // override to invoke prepareForStart on the JavaOVMThread
    public void startThread(OVMThread thread)  throws PragmaNoPollcheck {
        JavaOVMThread vmThread = (JavaOVMThread) thread;
        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            vmThread.prepareForStart(this);
            super.startThread(vmThread);
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }
    }


    /** 
     * Terminates the current thread. This method never returns and must never
     * throw an exception.
     * <p>This method should be called as part of thread termination due to the
     * completion (normally or abnormally) of the {@link java.lang.Thread#run}
     * method.
    */
    public void terminateCurrentThread()  throws PragmaNoPollcheck {
        JavaOVMThread current = (JavaOVMThread)jtm.getCurrentThread();
        assert current.getState() == JavaOVMThread.READY:
	    "terminating thread not ready: " + current.getState();
        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            current.setState(JavaOVMThread.TERMINATED);
            super.terminateCurrentThread(current);
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }
    }

    public boolean isAlive(JavaOVMThread vmThread) {
        // this isn't atomic but it doesn't need to be. The state
        // could change as soon as we have queried it.
        int state =  vmThread.getState();
        return state != JavaOVMThread.NOT_STARTED && 
            state != JavaOVMThread.TERMINATED;
    }

    public void bindPrimordialJavaThread(Oop javaThread) {
        JavaOVMThreadImpl currentThread = (JavaOVMThreadImpl)jtm.getCurrentThread();
        currentThread.bindJavaThread(javaThread);
        // set the priority of the VM thread based on the bound Java thread
        setPriority(currentThread, currentThread.getStartupPriority());
    }


    /**
     * {@inheritDoc}
     * <p><b>NOTE:</b>: interruption of blocked I/O is not supported.
     */
    public void interruptThread(JavaOVMThread vmThread) 
        throws PragmaNoPollcheck {
        // NOTE: no other thread can change the targets state as we hold
        // the lock. But the passage of time could change it's state, either
        // due to the expiry of a timed-wait or sleep, or the completion of I/O
        //
        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            int state = vmThread.getState();
            switch(state) {
              case JavaOVMThread.NOT_STARTED:
                    throw new OVMError.Internal("interrupt non-started thread");
                case JavaOVMThread.READY:
                case JavaOVMThread.BLOCKED_MONITOR:
                case JavaOVMThread.BLOCKED_SLEEP_NOINTERRUPT:
                case JavaOVMThread.BLOCKED_IO:
                    break; // nothing to do
                case JavaOVMThread.BLOCKED_WAIT:
                case JavaOVMThread.BLOCKED_TIMEDWAIT:
                  vmThread.setUnblockState(JavaOVMThread.UNBLOCKED_INTERRUPTED);
                    JavaMonitor mon = 
                        (JavaMonitor)vmThread.getWaitingConditionQueue();
                   assert mon != null : "null monitor for waiting thread";
                   mon.abortWait(vmThread); // will context switch if needed
                    break;
                case JavaOVMThread.BLOCKED_SLEEP:
                    if (jtm.wakeUp(vmThread)) {
                        vmThread.setUnblockState(JavaOVMThread.UNBLOCKED_INTERRUPTED);
                        vmThread.setState(JavaOVMThread.READY);
                    }
                    else {
                        throw new OVMError.Internal("Thread not found");
                    }
                    break;

                default: throw new OVMError.Internal("Unknown thread state: " 
                                                     + state);
            }
            
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }

    }

    // override to set priority range

    /* we support the 1 to 10 normal Java priorities plus system priority
       levels of 11, 12, 13 for the highest priority JVM threads - eg
       event handlers.
    */
    public int getMinPriority() {
        return 1;
    }

    public int getMaxPriority() {
        return 13;
    }


    // override to deal with state

    /**
     * @throws ClassCastException if <tt>thread</tt> is not a
     * {@link JavaOVMThread}.
     */
    public void setPriority(PriorityOVMThread thread, int prio) 
        throws PragmaNoPollcheck {
        JavaOVMThread vmThread = (JavaOVMThread) thread;
        // We do not hold the lock on the thread so it could have terminated
        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            if (!isAlive(vmThread)) {
                // the Java level code needs to know this
                throw new OVMError.IllegalState("terminated"); 
            }

            int oldPrio = vmThread.getPriority();

            // this check is not just an optimisation to save time. When
            // priority changes we have to reorder the queue the thread is in.
            // The only way to do this is to remove the thread and then add 
            // it back.
            // If the priority didn't really change this could change the 
            // threads position in the queue - and that would be wrong.
            if (prio != oldPrio) {
                vmThread.setPriority(prio); // sets priority field
                // what we need to do depends on what state the thread is in
                int state = vmThread.getState();
                switch(state) {
                    case JavaOVMThread.READY:
                        jtm.changeNotification(vmThread);
                       break;
                    case JavaOVMThread.NOT_STARTED:
                        break; // shouldn't get here
                    case JavaOVMThread.BLOCKED_SLEEP:
                    case JavaOVMThread.BLOCKED_SLEEP_NOINTERRUPT:
                    case JavaOVMThread.BLOCKED_IO:
                        break; // nothing more to do - not priority queue
                    case JavaOVMThread.BLOCKED_MONITOR:
                    {
                        JavaMonitor mon = 
                            (JavaMonitor) vmThread.getWaitingMonitor();
                        assert mon != null : "null monitor when waiting";
                        mon.changeNotification(vmThread);
                        break;
                    }
                    case JavaOVMThread.BLOCKED_WAIT:
                    case JavaOVMThread.BLOCKED_TIMEDWAIT:
                    {
                        JavaMonitor mon = 
                            (JavaMonitor) vmThread.getWaitingConditionQueue();
                        assert mon != null : "null monitor when waiting";
                        mon.changeNotification(vmThread);
                        break;
                    }
                    default: 
                        throw new OVMError.Internal("Unknown thread state: " 
                                                    + state);
                }
            }
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }
    }

    public int getPriority(PriorityOVMThread thread) 
        throws PragmaNoPollcheck {
        // this has to be atomic with respect to setThreadPriority
        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            return thread.getPriority();
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }
    }



}











