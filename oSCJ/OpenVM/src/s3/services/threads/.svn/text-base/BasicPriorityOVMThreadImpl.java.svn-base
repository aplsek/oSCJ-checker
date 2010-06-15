
package s3.services.threads;


import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.services.threads.OVMThreadCoreImpl;
import ovm.core.stitcher.ThreadDispatchServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.services.monitors.Monitor;
import ovm.services.threads.MonitorTrackingThread;
import ovm.services.threads.PriorityOVMThread;
import s3.util.PragmaNoPollcheck;
import s3.util.queues.SingleLinkElement;
/**
 * An implementation of {@link ovm.core.services.threads.OVMThread} supporting
 * the {@link s3.util.queues.SingleLinkElement SingleLinkElement} interface
 * for use with a {@link BasicUserLevelThreadManagerImpl basic user-level 
 * thread manager}.
 * <p>Subclass this class and override {@link #doRun} to provide the behaviour
 * desired.
 * <p>This class is not generally thread-safe. It is expected that the caller
 * ensures exclusive access to this thread - typically we are used by the
 * thread manager, indirectly via the dispatcher, and the dispatcher ensures
 * thread safety.
 *
 * @author David Holmes
 *
 */
public abstract class BasicPriorityOVMThreadImpl extends OVMThreadCoreImpl
    implements PriorityOVMThread, MonitorTrackingThread, SingleLinkElement {

    /** Next element in the queue */
    SingleLinkElement next = null;

    /** The execution priority of this thread */
    protected volatile int priority;

    /** Current dispatcher we should use */
    protected static final OVMDispatcher dispatcher;

    // This static initializer runs at image build time and expects to set
    // all the above static references from the current configuration.
    // Ideally this class would never get loaded except in a configuration
    // in which all the initialization is guaranteed to succeed, but that
    // is not yet the case. So this is written such that finding null values
    // or the wrong types of service instances, is not considered an error.
    // - DH 1 March 2005
    static {
        dispatcher = 
            ((ThreadDispatchServicesFactory)ThreadServiceConfigurator.config.
             getServiceFactory(ThreadDispatchServicesFactory.name)).
            getThreadDispatcher();
    }

    public BasicPriorityOVMThreadImpl(OVMThreadContext ctx) {
        super(ctx);
    }

    public BasicPriorityOVMThreadImpl() {}

    /** 
      * The monitor upon which we are current waiting.
      * This is needed to ensure that we get reordered in the queue
     * if our priority gets changed
    */
    volatile Monitor waitingMonitor = null;

    /* implementation methods for MonitorTrackingThread */

    public void setWaitingMonitor(Monitor mon) 
        throws ovm.core.services.memory.PragmaNoBarriers {
        if (waitingMonitor != null && mon != null) {
            throw new IllegalStateException("already waiting on a monitor");
        }
        waitingMonitor = mon;
    }

    public Monitor getWaitingMonitor() {
        return waitingMonitor;
    }

    /* Implementation methods for OVMPriorityThread */

    /** 
     * Sets the priority of this thread. This method is only intended for use
     * by the priority dispatcher as it only sets the internal value of
     * priority. To change the runtime priority of a thread the program must
     * use the dispatcher.
     *
     */
    public void setPriority(int newPriority) throws PragmaNoPollcheck {
        priority = newPriority;
    }

    public int getPriority() throws PragmaNoPollcheck {
        return priority;
    }

    /* Implementation methods for SingleLinkElement */

    public void setNext(SingleLinkElement next) 
        throws ovm.core.services.memory.PragmaNoBarriers,
               PragmaNoPollcheck {
        assert next != this : "next == this";
        this.next = next;
    }

    public SingleLinkElement getNext() throws PragmaNoPollcheck {
        return this.next;
    }
}
