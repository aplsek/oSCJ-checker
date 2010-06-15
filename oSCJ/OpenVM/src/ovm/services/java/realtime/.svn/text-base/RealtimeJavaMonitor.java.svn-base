/**
 * RealtimeJavaMonitor.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/java/realtime/RealtimeJavaMonitor.java,v 1.1 2004/10/20 05:39:03 dholmes Exp $
 *
 */
package ovm.services.java.realtime;

import ovm.services.monitors.AbsoluteTimedAbortableConditionQueue;
import ovm.services.monitors.Monitor;
import ovm.services.java.JavaMonitor;
/**
 * 
 * Extends {@link JavaMonitor} with absolute timed wait capabilities as
 * required by the Realtime Specification for Java.
 *
 *
 * @author David Holmes
 *
 */
public interface RealtimeJavaMonitor 
    extends JavaMonitor, 
            AbsoluteTimedAbortableConditionQueue {

    /**
     * Defines a factory method for creating RealtimeJavaMonitors
     *
     */
    public interface Factory extends Monitor.Factory {
        /**
         * Return a new Realtime Java monitor instance.
         * @return a new Realtime Java monitor instance.
         */
        Monitor newInstance();
    }
}






