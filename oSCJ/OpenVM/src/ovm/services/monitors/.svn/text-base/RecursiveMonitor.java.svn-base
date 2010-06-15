/**
 * RecursiveMonitor.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/RecursiveMonitor.java,v 1.6 2004/09/29 17:45:46 cunei Exp $
 *
 */
package ovm.services.monitors;

import ovm.core.services.threads.OVMThread;

/**
 * Defines a {@link Monitor} which is recursive, that is, the thread owning, 
 * or currently in, the monitor can successfully attempt re-entry of the 
 * monitor. The monitor is only released when the same number of exits as
 * entries have occurred.
 * <p>The interaction of recursive monitors and condition queues is not
 * specified but depends on the concrete types being implemented.
 * <p>The recursive nature of other monitor entry points is determined by the
 * implementation. For example, a {@link ovm.services.java.JavaMonitor} makes
 * all of its entry points recursive. The additional entry points defined here
 * allow for a more optimal implementation of recursive monitors.
 * 
 * @author David Holmes
 *
 */
public interface RecursiveMonitor {

    /**
     * When called on an unlocked monitor, force the monitor to be
     * held by owner.  When called on a monitor already held by owner,
     * bump the recursion count.
     **/
    public void enter(OVMThread owner);

    /**
     * Causes the current thread to attempt entry of the monitor when it
     * already owns the monitor. As long as the current thread is the owner
     * this call must succeed without blocking. The entry count is increased by
     * one.
     *
     * @throws IllegalMonitorStateException if the current thread is not the
     * owner (Optional)
     *
     */
    void enterRecursive();


    /**
     * Causes the current thread to attempt to leave the monitor. 
     * The entry count is decreased by one and if it is zero then the
     * monitor is released.
     *
     * @throws IllegalMonitorStateException if the current thread is not the
     * owner (Optional)
     */
    void exitRecursive();

    /**
     * Queries how many times this monitor has been recursively entered.
     *
     * @return the number of times the monitor has been recursively entered
     * by the current owning thread, but not exited.
     *
     */
    int entryCount();
}

