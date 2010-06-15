
package s3.core.services.timer;

import ovm.core.execution.Native;
import ovm.core.execution.NativeInterface;
import ovm.core.services.events.EventManager;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.process.ForkManager;
import ovm.core.services.timer.TimeConversion;
import ovm.core.services.timer.TimerInterruptAction;
import ovm.core.services.timer.TimerManager;
import ovm.core.stitcher.*;
import ovm.core.services.events.EventManager;
import ovm.core.services.events.PollcheckManager;
import ovm.util.OVMError;
import ovm.core.services.process.ForkManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.execution.Native;
import ovm.core.services.memory.*;
import ovm.core.services.io.BasicIO;

import s3.util.queues.*;
import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;
import s3.util.queues.DelayableSingleLinkDeltaElement;
import s3.util.queues.SingleLinkDeltaQueue;
import s3.util.queues.SingleLinkTimerQueue;

/**
 * A straight-foward and generic implementation of the {@link TimerManager} 
 * interface.
 * <p>This implementation works in conjunction with the {@link EventManager}.
 *
 * <p>The functionality of this class is divided into four groups:
 * <ul>
 * <li>The control functions: {@link #start}, {@link #stop}, 
 * {@link #setTimerInterruptPeriod} etc, which deal with
 * the configuration and general operation of the timer
 * <li>The {@link #eventFired} method 
 * which defines the mechanics of
 * processing the registered actions on each interrupt;
 * <li>The methods for maintaining the list of registered actions 
 * ({@link #addTimerInterruptAction}, {@link #removeTimerInterruptAction},
 * and {@link #getRegisteredActions}; and
 * <li>The methods for inserting, updating and querying the delay queues
 * </ul>
 * <h3>Concurrency Control and Exclusion Requirements</h3>
 * <p>This class is not thread safe in a general sense. Concurrency control
 * is defined in terms of the four groups defined above.
 * <p>The control functions are not generally expected to be thread-safe, as
 * it makes little sense to have threads racing to try and configure and 
 * control the timer.
 * <p>The {@link #eventFired} method can 
 * logically occur in between the execution of any two bytecodes. Consequently,
 * there will be interference between this method and any of the methods that
 * manipulate or use the list of registered actions. The converse is not true,
 * as further interrupts are disabled when 
 * {@link #eventFired} executes and nothing can &quot;interrupt&quot; it.
 * <p>The methods for maintaining the list of actions may be invoked 
 * concurrently and so exclusion should be enforced across their
 * execution. 
 * <p>The delay queue operations are not thread-safe and rely upon external
 * synchronization.
 * <p>When the event manager is in charge, atomicity relies on
 * external controls. To deal with this we simply declare that the action
 * list manipulation methods must be atomic (using <tt>PragmaAtomic</tt>).
 * We could use a concurrent data structure for the action lists to avoid 
 * the need for atomicity (or we could preclude dynamic updates).
 * The <eventFired</tt> method is always
 * executed atomically.
 *
 * @author David Holmes
 *
 */
public class TimerManagerImpl extends OSTimerManagerImplBase {

    /** Internal helper class defining the native call interface
     */
    private static final class Helper implements NativeInterface {
        /**
         * Get the address of the interruptCount variable so we can access 
         * it directly
         */
        static native VM_Address getInterruptCountAddress();
    }

    /**
     * Proxy reference to a native code value
     */
    private VM_Address interruptCount;

    /**
     * This is a singleton class. The instance can be accessed from here
     * or more usually via the service configurator.
     */
    protected static TimerManager instance = new TimerManagerImpl();

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
    public TimerManagerImpl() {}

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
    public TimerManagerImpl(int size) {
	super(size);
    }

    protected void initHook() {
	interruptCount=Helper.getInterruptCountAddress();
    }
    
    protected int getCurrentCount() {
	return interruptCount.getInt();
    }


}









