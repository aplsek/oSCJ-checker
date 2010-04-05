package ovm.core.services.events;

/**
 *
 * @author Filip Pizlo, David Holmes
 */
public interface EventManager extends ovm.services.ServiceInstance {
    
    public interface EventProcessor {
        
        public void eventFired();

	public String eventProcessorShortName();

    }
    
    public interface WaitingAuditor {
	/** override the wait time.  -1 means wait forever, 0 means
	    don't wait at all.  the units are nanoseconds. */
	long overrideWaitTime(long waitTime);
    }
    
    public void processEvents();
    
    /** returns the resolution of delayed waits, or -1 if it is impossible to
	wait for a specific amount of time.  note that all event managers
	must be able to wait for no time (not wait at all).  the units are
	nanoseconds. */
    public long delayedWaitResolution();

    public void addWaitingAuditor(WaitingAuditor wa);
    public void removeWaitingAuditor(WaitingAuditor wa);

    /**
     * Thread manager will call this when no threads are ready to run.
     */
    public void waitForEvents();
    
    public void addEventProcessor(EventProcessor handler);
    
    public void removeEventProcessor(EventProcessor handler);
    
    /**
     * Enables or disables the processing of events. When disabled the
     * polling functions are not invoked. Event processors need their
     * own means to detect missed events if that is important to them.
     * 
     * @param enabled if <tt>true</tt> then processing of events is enabled,
     * otherwise they are disabled.
     * @return the previous status
     * @see #isEnabled
     *
     */
    public boolean setEnabled(boolean enabled);

    /**
     * Queries whether the processing of events is currently enabled.
     * @return <code>true</code> if the processing of events is currently
     * enabled, and <code>false</code> otherwise.
     *
     * @see #setEnabled
     */
    public boolean isEnabled();

    /** returns false if the dumpStateHook() has nothing interesting
     * to say for this implementation of EventManager */
    public boolean hasNonTrivialDumpStateHook();

    /** hook for dumping the state; used by Dispatcher */
    public void dumpStateHook();

    /** reset the histograms (ignored if not applicable) */
    public void resetProfileHistograms();
    
    /** disable the histograms (ignored if not applicable) */
    public void disableProfileHistograms();

}

