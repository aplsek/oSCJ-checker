 
package s3.services.threads;


import java.util.Comparator;

import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.services.monitors.Monitor;
import ovm.services.threads.MonitorTrackingThread;
import ovm.services.threads.PriorityOVMDispatcher;
import ovm.services.threads.PriorityOVMThread;
import ovm.util.OVMError;
import ovm.util.Ordered;
import s3.util.PragmaNoPollcheck;
/**
 * A dispatcher implementation for the basic user-level thread manager
 * of the OVM, that supports priority based dispatching.
 * <p>All methods that disable rescheduling or which are only called with
 * rescheduling disabled, declare PragmaNoPollcheck.
 */
public class PriorityDispatcherImpl 
    extends DispatcherImpl implements PriorityOVMDispatcher {

    /** The singleton instance of this class */
    final static PriorityOVMDispatcher instance = new PriorityDispatcherImpl();

    /**
     * Return the singleton instance of this class 
     * @return the singleton instance of this class 
     */
    public static OVMDispatcher getInstance() {
        return instance;
    }

    /**
     * Trivial no-arg constructor
     * @see #init
     */
    protected PriorityDispatcherImpl() {}

    /**
     * The comparator we use to set up the thread manager.
     */
    protected Comparator comp = null;

    /**
     * Initialisation of the priority dispatcher. Extracts the required
     * comparator and sets it in the thread manager.
     *
     * @throws OVMError.Configuration if the thread manager does not support the
     * {@link ovm.util.Ordered Ordered} interface.
     */
    public void init() {
        super.init();
        if (!(tm instanceof Ordered)) {
            throw new OVMError.Configuration(
                "Priority dispatcher requires Ordered thread manager");
        }
        comp = PriorityOVMThread.Comparator.instance();
        if (!tm.isInited()) {
            tm.init();
        }
        ((Ordered)tm).setComparator(comp);
        isInited = true;
    }

    /**
     * Sets the priority of the primordial thread to a mid-range value
     * between the minimum and maximum supported priorities.
     */
    protected void initPrimordialThread(OVMThread primordialThread) {
        if (primordialThread instanceof PriorityOVMThread) {
            ((PriorityOVMThread)primordialThread).setPriority(
                getMinPriority()/2 + getMaxPriority()/2 );
          d("Primordial thread priority set to " + 
            ((PriorityOVMThread)primordialThread).getPriority());
        }
        else {
            throw new OVMError.Configuration(
                "Priority dispatcher can only work with priority threads");
        }
    }


    public int getMinPriority() throws PragmaNoPollcheck{
        return 0x80000000; // Integer.MIN_VALUE
    }

    public int getMaxPriority() throws PragmaNoPollcheck{
        return 0x7FFFFFFF; // Integer.MAX_VALUE
    }

    public void setPriority(PriorityOVMThread thread, int prio) 
     throws PragmaNoPollcheck {
	assert thread != null : "null thread passed to setPriority";
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            // this check is not just an optimisation to save time. When
            // priority changes we have to reorder the queue the thread is in.
            // The only way to do this is to remove the thread and then add 
            // it back.
            // If the priority didn't really change this could change the 
            // threads position in the queue - and that would be wrong.
            if (prio != thread.getPriority()) {
                thread.setPriority(prio);
                // we aren't tracking the state of the thread so we have to 
                // "guess" what to do - safely of course
                if (thread instanceof MonitorTrackingThread) {
                    Monitor mon = ((MonitorTrackingThread)thread).getWaitingMonitor();
                    if (mon != null) {
                        if (mon instanceof ovm.util.Ordered) {
                            ((ovm.util.Ordered)mon).changeNotification(thread);
                            return; // nothing more to do
                        }
                    }
                }
                // if we aren't waiting on a monitor, or can't tell that we're 
                // waiting then we must update the ready queue in case
                if (tm instanceof ovm.util.Ordered) {
                    ((ovm.util.Ordered)tm).changeNotification(thread);
                }
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }

    
    public int getPriority(PriorityOVMThread thread) throws PragmaNoPollcheck{
        return thread.getPriority();
    }
}











