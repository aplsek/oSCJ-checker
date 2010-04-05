
package s3.services.realtime;

import ovm.services.realtime.RealtimePriorityDispatcher;
import ovm.services.realtime.RealtimeOVMThread;
import ovm.services.threads.PriorityOVMThread;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.services.threads.TimedSuspensionThreadManager;
import s3.services.threads.PriorityDispatcherImpl;
import ovm.util.OVMError;

/**
 * A realtime priority dispatcher that defines priority ranges suitable for use
 * in the RTSJ. This class also adds support for the delayed start-up of
 * real-time threads.
 *
 * @author David Holmes
 */
public class RealtimeDispatcherImpl extends PriorityDispatcherImpl 
    implements RealtimePriorityDispatcher {


    /** Our time-suspension thread manager */
    protected TimedSuspensionThreadManager sleepMan;

    /** The singleton instance of this class */
    static final RealtimePriorityDispatcher instance = new RealtimeDispatcherImpl();


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
    protected RealtimeDispatcherImpl() {}

    /**
     * Init checks to see that we are in a consistent configuration and that
     * the thread manager supports the functionality we need.
     * @throws OVMError.Configuration is the thread manager does not support
     * timed-suspension
     */
    public void init() {
        super.init();
        if (tm instanceof TimedSuspensionThreadManager) {
            sleepMan = (TimedSuspensionThreadManager) tm;
        }
        else {
            throw new OVMError.Configuration("realtime dispatcher needs a timed-suspension thread manager");
        }
    }


    protected static final int MIN_NON_RT_PRIORITY = 1;
    protected static final int MAX_NON_RT_PRIORITY = 10;
    protected static final int MIN_RT_PRIORITY = 11;
    protected static final int MAX_RT_PRIORITY = 38;

    public int getMinPriority() {
        return MIN_NON_RT_PRIORITY;
    }

    public int getMaxPriority() {
        return MAX_NON_RT_PRIORITY;
    }

    public int getMinNonRTPriority() {
        return MIN_NON_RT_PRIORITY;
    }

    public int getMaxNonRTPriority() {
        return MAX_NON_RT_PRIORITY;
    }

    public int getMinRTPriority() {
        return MIN_RT_PRIORITY;
    }

    public int getMaxRTPriority() {
        return MAX_RT_PRIORITY;
    }


    public boolean isValidNonRTPriority(int priority) {
        return priority >= MIN_NON_RT_PRIORITY && 
               priority <= MAX_NON_RT_PRIORITY;
    }

    public boolean isValidRTPriority(int priority) {
        return priority >= MIN_RT_PRIORITY && 
               priority <= MAX_RT_PRIORITY;
    }


    public void setPriority(PriorityOVMThread thread, int prio) {
        if (thread instanceof RealtimeOVMThread) {
            setPriority((RealtimeOVMThread)thread, prio);
        }
        else {
            if (isValidNonRTPriority(prio)) {
                super.setPriority(thread, prio);
            }
            else {
            throw new OVMError.IllegalArgument(
                "priority out of range: min(" + 
                MIN_NON_RT_PRIORITY + ") -> " + prio + 
                " -> max(" + MAX_NON_RT_PRIORITY + ")" );

            }
        }
    }


    public void setPriority(RealtimeOVMThread thread, int prio) {
        if (isValidRTPriority(prio)) {
            super.setPriority(thread, prio);
        }
        else {
            throw new OVMError.IllegalArgument(
                "realtime priority out of range: min(" + 
                MIN_RT_PRIORITY + ") -> " + prio + 
                " -> max(" + MAX_RT_PRIORITY + ")" );
        }
    }

    public boolean startThreadDelayed(OVMThread thread, long releaseTime) {
        // note: sleepMan == tm, but you can only get the sleep functions
        //        through sleepMan (without a cast)
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            nThreads++;
            if (!sleepMan.sleepAbsolute(thread, releaseTime) ){
                tm.makeReady(thread);
                return false;
            }
            else {
                return true;
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }

    /**
     * Sets the priority of the primordial thread to a mid-range value
     * between the minimum and maximum supported priorities, taking into
     * account whether it is a real-time thread or not.
     */
    protected void initPrimordialThread(OVMThread primordialThread) {
        if (primordialThread instanceof RealtimeOVMThread) {
            ((PriorityOVMThread)primordialThread).setPriority(
                getMinRTPriority()/2 + getMaxRTPriority()/2 );
          d("Realtime Primordial thread priority set to " + 
            ((PriorityOVMThread)primordialThread).getPriority());
        }
        else {
            super.initPrimordialThread(primordialThread);
        }
    }

}











