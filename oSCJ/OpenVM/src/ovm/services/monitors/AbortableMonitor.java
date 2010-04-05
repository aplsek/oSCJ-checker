/**
 * AbortableMonitor.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/AbortableMonitor.java,v 1.3 2004/02/20 08:48:46 jthomas Exp $
 *
 */
package ovm.services.monitors;

import ovm.core.services.threads.OVMThread;

/**
 * Defines a {@link Monitor} which supports aborting an attempt to
 * enter the monitor. A thread trying to enter the monitor can be
 * specified as the target of an {@link #abortEntry} call. If that thread
 * was using an abortable entry point then it will return from that
 * entry point.
 * <p>This interface defines a new abortable entry point rather than
 * changing the semantics of the base {@link Monitor#enter} method. This
 * is partly pragmatic because the base method does not have a return value
 * that can be used to signify whether or not entry was successful, but it
 * also allows for an implementation to have both abortable and
 * non-abortable entry points.
 * @author David Holmes
 *
 */
public interface AbortableMonitor {

    /**
     * Causes the current thread to attempt entry of the monitor. If the
     * monitor is unavailable and the thread is the target of the
     * {@link #abortEntry} call, this method will return with a 
     * <code>false</code> value.
     *
     * @return <code>true</code> if the monitor was entered, and 
     * <code>false</code> if the thread was the target of an
     * {@link #abortEntry} call.
     *
     * @see #abortEntry
     */
    boolean enterAbortable();

    
    /**
     * Attempts to abort the entry of the specified thread into the monitor.
     * If the thread is not attempting to enter the monitor through an
     * {@link #enterAbortable abortable entry point} then nothing
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



