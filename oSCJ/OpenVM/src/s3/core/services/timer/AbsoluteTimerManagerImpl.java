package s3.core.services.timer;

import ovm.core.execution.Native;
import ovm.core.services.timer.TimerManager;


public class AbsoluteTimerManagerImpl extends OSTimerManagerImplBase {
    
    
    /**
     * This is a singleton class. The instance can be accessed from here
     * or more usually via the service configurator.
     */
    protected static TimerManager instance = new AbsoluteTimerManagerImpl();

    /**
     * Returns the singleton instance of this class
     * @return the singleton instance of this class
     */
    public static TimerManager getInstance() {
        return instance;
    }

    /**
     * Create a timer manager
     * using the default initial action list size.
     *
     *
     */
    public AbsoluteTimerManagerImpl() {}

    /**
     * Create a timer manager 
     * using the given initial action list size.
     * <p>For a given configuration the number of registered actions may be
     * fixed - in fact in this implementation we expect that - so we don't
     * attempt to do anything fancy in terms of maintaining the action
     * list.
     *
     * @param size The initial size of the action list.
     *
     */
    public AbsoluteTimerManagerImpl(int size) {
	super(size);
    }

    protected int getCurrentCount() {
	return (int)(Native.getCurrentTime()/period);
    }

    
}

