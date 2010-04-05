/*
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/threads/MonitorTrackingThread.java,v 1.4 2004/02/20 08:48:51 jthomas Exp $
 *
 */
package ovm.services.threads;

import ovm.services.monitors.Monitor;

/**
 * This is a mix-in interface for threads that keep track of whether or not
 * they are waiting on a monitor. This is useful information if, for example,
 * you need to update queues when a threads priority changes.
 *
 * @author David Holmes
 *
 */
public interface MonitorTrackingThread {

    /**
     * Sets the {@link ovm.services.monitors.Monitor} upon which this thread 
     * is awaiting entry.
     *
     * @param mon the monitor upon which this thread is waiting, or 
     * <code>null</code> if this thread is no longer waiting upon any monitor.
     * @throws IllegalStateException if <code>mon</code> is not 
     * <code>null</code> and this thread is already waiting on a monitor
     */
    void setWaitingMonitor(Monitor mon);

    /**
     * Returns the {@link ovm.services.monitors.Monitor} upon which this 
     * thread is waiting.
     * @return the monitor upon which this thread is waiting, or 
     * <code>null</code> if this thread is not waiting on a monitor
     *
     */
    Monitor getWaitingMonitor();

}
