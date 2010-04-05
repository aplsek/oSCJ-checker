/*
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/threads/PriorityOVMDispatcher.java,v 1.7 2004/02/20 08:48:51 jthomas Exp $
 */
package ovm.services.threads;
import ovm.core.services.threads.OVMDispatcher;

/**
 * The &quot;priority&quot; dispatcher extends the basic dispatcher by adding
 * methods that allow the priorities of threads to be changed. The methods of
 * {@link PriorityOVMThread} only change a priority field within the thread,
 * and it is up to the dispatcher to make sure that such changes are reflected
 * in the actual runtime behaviour of the systems. For example, by updating the
 * ready queue of a user-level thread manager, or the queue of a monitor.
 *
 *
 * @author David Holmes
 *
 */
public interface PriorityOVMDispatcher extends OVMDispatcher {

    /**
     * Sets the priority of the given OVM thread and ensures that the
     * priority change is reflected in the runtime behaviour of the system.
     * The actual meaning of a priority value is implementation specific.
     *
     * @param thread the thread whose priority is to be changed
     * @param prio the new priority value
     */
    void setPriority(PriorityOVMThread thread, int prio);


    /**
     * Returns the priority of the given OVM thread. This method exists mainly
     * for symmetry in the API.
     * @return the priority of the given OVM thread
     *
     */
    int getPriority(PriorityOVMThread thread);


    /**
     * Returns the minimum priority value suported by this dispatcher.
     * @return  the minimum priority value suported by this dispatcher.
     *
     */
    int getMinPriority();

    /**
     * Returns the maximum priority value suported by this dispatcher.
     * @return  the maximum priority value suported by this dispatcher.
     *
     */
    int getMaxPriority();

}






