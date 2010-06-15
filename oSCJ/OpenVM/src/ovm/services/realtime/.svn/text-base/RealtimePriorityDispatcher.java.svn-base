
package ovm.services.realtime;

import ovm.core.services.threads.OVMThread;
import ovm.services.threads.PriorityOVMDispatcher;
import ovm.services.threads.PriorityOVMThread;
/**
 * A priority dispatcher that recognises the existence of realtime threads
 * (those for which realtime scheduling guarantees are made). A realtime
 * priority dispatcher maintains the priority ranges that can be assigned to
 * normal and realtime threads. For example, a RTSJ compliant dispatcher might
 * provide ten priorities for non-realtime threads and 
 * twenty-eight realtime priorities. 
 * A realtime dispatcher also supports the delayed start of a thread - used,
 * for example when periodic threads have a deferred initial release time.
 *
 * <p>The priorities supported by a given dispatcher should form a contiguous
 * range. The sub-ranges for normal and realtime priorities should not 
 * overlap. A higher priority value represents a higher execution eligibility.
 * The maximum non-realtime priority must be less than the minimum realtime
 * priority.
 *
 * <p>Supporting different priority ranges is problematic because non-RT 
 * threads may not even know that RT threads exist and so they don't know to
 * ask for a non-RT priority. For this reason a 
 * <code>RealtimePriorityDispatcher</code> looks like a non-realtime dispatcher
 * to non-realtime threads.
 *
 * @see "src/syslib/user/ovm_realtime"
 *
 * @author David Holmes
 */
public interface RealtimePriorityDispatcher extends PriorityOVMDispatcher {

    // override inherited methods to add throws clause

    /**
     * @throws OVMError.IllegalArgument if the priority value is out of
     * range for the give type of thread
     */
    void setPriority(PriorityOVMThread thread, int priority);


    // overload to support realtime threads directly

    /**
     * Sets the priority of the given realtime thread and ensures that the 
     * priority change is reflected in the runtime behaviour of the system. 
     * The actual meaning of a priority value is implementation specific. 
     *
     * @param thread the realtime thread whose priority is to be changed
     * @param priority the new priority value
     *
     * @throws OVMError.IllegalArgument if the priority value is out of
     * range for the realtime thread
     */
    void setPriority(RealtimeOVMThread thread, int priority);

    /**
     * Returns the minimum non-realtime priority supported by this dispatcher.
     * @return the minimum non-realtime priority supported by this dispatcher.
     */
    int getMinNonRTPriority();

    /**
     * Returns the maximum non-realtime priority supported by this dispatcher.
     * @return the maximum non-realtime priority supported by this dispatcher.
     */
    int getMaxNonRTPriority();

    /**
     * Returns the minimum realtime priority supported by this dispatcher.
     * @return the minimum realtime priority supported by this dispatcher.
     */
    int getMinRTPriority();

    /**
     * Returns the maximum realtime priority supported by this dispatcher.
     * @return the maximum realtime priority supported by this dispatcher.
     */
    int getMaxRTPriority();

    /**
     * Returns the minimum priority supported by this dispatcher.
     * This is the same as {@link #getMinNonRTPriority}.
     * @return the minimum overall priority supported by this dispatcher.
     */
    int getMinPriority();

    /**
     * Returns the maximum priority supported by this dispatcher.
     * This is the same as {@link #getMaxNonRTPriority}.
     * <p>These methods must be usable by non-realtime threads that know
     * nothing about realtime and non-realtime priorities.
     *
     * @return the maximum overall priority supported by this dispatcher.
     */
    int getMaxPriority();

    /**
     * Queries if the given value is a valid non-realtime priority.
     * This is a convenience function to avoid multiple calls to perform
     * a range-check.
     *
     * @param priority the value to be checked
     * @return the value of <code>priority >= getMinNonRTPriority() &&
     * priority <= getMaxNonRTPriority()</code>
     */
    boolean isValidNonRTPriority(int priority);

    /**
     * Queries if the given value is a valid realtime priority.
     * This is a convenience function to avoid multiple calls to perform
     * a range-check.
     *
     * @param priority the value to be checked
     * @return the value of <code>priority >= getMinRTPriority() &&
     * priority <= getMaxRTPriority()</code>
     */
    boolean isValidRTPriority(int priority);

    /**
     * Causes the specified thread to become eligible for execution at
     * the specified time.
     * When the current scheduling policy dictates, the context of the
     * specified thread can be made the current context and the thread
     * will commence execution in the method defined by that context.
     * <p>If the release time has already passed then the thread is
     * immediately eligible for execution.
     * @param thread the thread to make eligible for execution
     * @param releaseTime the absolute time in nanoseconds at which this
     * thread should first become eligible for execution
     * @return <code>true</code> if the thread was started delayed, and
     * <code>false</code> if the thread started immediately due to its
     * release time having passed.
     */
    public boolean startThreadDelayed(OVMThread thread, long releaseTime);

}








