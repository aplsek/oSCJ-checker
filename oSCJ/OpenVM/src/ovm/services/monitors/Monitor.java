/**
 * Monitor.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/Monitor.java,v 1.14 2005/08/30 00:19:01 dholmes Exp $
 **/
package ovm.services.monitors;

import ovm.core.services.threads.OVMThread;
/**
 * Defines the abstract notion of a &quot;monitor&quot; in line with the
 * monitors of Hoare & Brinch-Hansen and those used in languages like
 * Modula-3 and Java. This interface provides very few semantic details as
 * they vary between monitor types. More specific monitor types will
 * provide specific semantics; for example {@link
 * ovm.services.java.JavaMonitor}. The general notion of a monitor only
 * allows a single thread to be active within the monitor at a time.  
 * 
 * <p>This interface defines only those methods related to the notion of a
 * monitor as a protected object, methods for working with
 * &quot;conditions&quot; are defined in specialisations of {@link
 * ConditionQueue} and {@link ConditionQueueSignaller}.
 *
 * @see ConditionQueue
 * 
 * @author David Holmes
 **/
public interface Monitor {

    /**
     * Causes the current thread to attempt entry of the monitor.
     */
    public void enter();

    /**
     * When called on an unlocked monitor, force the monitor to be
     * held by owner.
     **/
    public void enter(OVMThread owner);
    
    /**
     * Causes the current thread to leave the monitor.
     */
    public void exit();

    /**
     * Returns the thread that currently owns (that is, has entered), 
     * this monitor.
     *
     * @return the thread that currently owns this monitor, or 
     * <code>null</code> if the monitor is un-owned.
     *
     */
    OVMThread getOwner();

    /**
     * Defines a factory method for creating Monitors
     */
    public interface Factory {

	/**
         * An initialization hook for use after the system services have been
         * initialized and started. This is typically invoked by the dispatcher
         * as part of initialization of the threading system. Typically a
         * factory will forward the initialization request to the actual
         * monitor implementation class.
         */
        void initialize();

        /**
         * Return a new monitor instance.
         * @return a new monitor instance.
         */
        Monitor newInstance();



        /** 
         * Return an estimate of the number of bytes needed to allocate
         * a monitor using this factory.
         */
        int monitorSize();

    }

} // End of Monitor

