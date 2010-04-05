/**
 * QueryableMonitor.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/QueryableMonitor.java,v 1.5 2007/06/03 01:25:47 baker29 Exp $
 *
 */
package ovm.services.monitors;

import ovm.core.services.threads.OVMThread;

/**
 * Defines a {@link Monitor} that supports querying of 
 * the number of threads waiting to enter the monitor and whether a
 * particular thread is waiting.
 * <p>This is a mix-in interface that should be implemented along with a
 * particular monitor type.
 *
 * @author David Holmes
 *
 */
public interface QueryableMonitor {

    /**
     * Returns the number of threads currently waiting to enter this monitor.
     *
     * @return the number of threads waiting to enter this monitor.
     *
     */
    int getEntryQueueSize();

    
    /**
     * Queries if the specified thread is attempting entry
     * of this monitor.
     *
     * @param t the thread to look for
     *
     * @return <code>true</code> if the specified thread is attempting entry
     * of this monitor, and <code>false</code> otherwise.
     *
     */
     boolean isEntering(OVMThread t);
}












