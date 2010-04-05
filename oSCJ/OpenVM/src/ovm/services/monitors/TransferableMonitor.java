package ovm.services.monitors;
import ovm.core.services.threads.OVMThread;
import ovm.util.OVMError;

/**
 * A monitor that supports transfer of its ownership from the current
 * thread to another thread.
 *
 * @author David Holmes
 */
public interface TransferableMonitor {

    /**
     * Transfer ownership of this monitor from the current thread to the
     * given thread. The current thread must not hold this monitor
     * recursively, and the new owner must not already be in the entry queue
     * of, or waiting on, this monitor.
     *
     * @param to the thread to transfer ownship to.
     *
     * @throws OVMError.IllegalState if new owner is waiting on or entering 
     * this monitor; or the current thread holds this monitor recursively
     * @throws IllegalMonitorStateException if the current thread doesn't
     * own this monitor
     */
    void transfer(OVMThread to) throws IllegalMonitorStateException,
                                       OVMError.IllegalState;

}
