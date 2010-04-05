// $Header: /p/sss/cvs/OpenVM/src/ovm/services/java/UnsafeJavaMonitor.java,v 1.1 2004/04/17 19:31:39 pizlofj Exp $

package ovm.services.java;

import ovm.services.monitors.*;

/**
 *
 * @author Filip Pizlo
 */
public interface UnsafeJavaMonitor
    extends JavaMonitor, UnsafeConditionQueueSignaller {

    /**
     * Defines a factory method for creating UnsafeJavaMonitors
     *
     */
    public interface Factory extends JavaMonitor.Factory {
        /**
         * Return a new unsafe Java monitor instance.
         * @return a new Java monitor instance.
         */
        UnsafeJavaMonitor newUnsafeJavaMonitorInstance();
    }
}

