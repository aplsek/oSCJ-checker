/**
 * TimedAbortableMonitor.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/TimedAbortableMonitor.java,v 1.4 2004/02/20 08:48:46 jthomas Exp $
 *
 */
package ovm.services.monitors;

import ovm.core.services.threads.OVMThread;

/**
 * Defines a {@link Monitor} which supports both aborting of and timing-out
 * from an attempted entry into the monitor. 
 * 
 * <p>This interface defines a new entry point to allow different forms of
 * entry to be supported by one implementation and because we need a ternary
 * return value.
 * @author David Holmes
 *
 */
public interface TimedAbortableMonitor {

    /**
     * Constant to reflect a succesful entry
     */
    static final int ENTERED = 0;

    /**
     * Constant to reflect a timeout on entry
     */
    static final int TIMED_OUT = 1;

    /**
     * Constant to reflect an aborted entry
     */
    static final int ABORTED = 2;

    /**
     * Causes the current thread to attempt entry of the monitor. 
     * The thread can return from entry under three conditions:
     * <ol>
     * <li>the monitor became available and was entered
     * <li>the specified timeout elapsed
     * <li>it was the target of a call to {@link #abortEntry}
     * </ol>
     *
     * <p>The timeout value must be greater than, or equal to zero. A zero
     * timeout has implementation specific semantics - it could mean a balking
     * response, or it could mean an infinite timeout.
     *
     * @param timeout the timeout in nanoseconds.
     *
     * @return {@link #ENTERED} if the monitor was successfuly entered.
     *         {@link #TIMED_OUT} if the entry attempt timed out.
     *         {@link #ABORTED} if the entry attempt was aborted by a call to
     * {@link #abortEntry}.
     *
     */
    int enterTimedAbortable(long timeout);

    /**
     * Attempts to abort the entry of the specified thread into the monitor.
     * If the thread is not attempting to enter the monitor through an
     * {@link #enterTimedAbortable abortable entry point} then nothing
     * happens and <code>false</code> is returned.
     *
     * @param thread the thread whose entry should be aborted
     *
     * @return <code>true</code> if <code>thread</code> was attempting entry
     * through an abortable entry point and has now had that entry
     * aborted; and <code>false</code> otherwise.
     *
     */
    boolean abortEntry(OVMThread thread);

}
