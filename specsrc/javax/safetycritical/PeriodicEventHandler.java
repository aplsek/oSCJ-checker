package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.LEVEL_0;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;


@SCJAllowed
public abstract class PeriodicEventHandler extends ManagedEventHandler {

    /**
     * Constructor to create a periodic event handler.
     * <p>
     * Does not perform memory allocation. Does not allow this to escape local
     * scope. Builds links from this to priority and parameters, so those two
     * arguments must reside in scopes that enclose this.
     * <p>
     * 
     * @param priority
     *            specifies the priority parameters for this periodic event
     *            handler. Must not be null.
     * 
     * @param parameters
     *            specifies the periodic release parameters, in particular the
     *            start time, period and deadline miss and cost overrun
     *            handlers. Note that a relative start time is not relative to
     *            NOW but relative to the point in time when initialization is
     *            finished and the timers are started. This argument must not be
     *            null.
     *    
     * @param scp
     *            The scp parameter describes the organization of memory
     *            dedicated to execution of the underlying thread. (added by MS)
     * @param memSize
     *            the size in bytes of the memory area to be used for the
     *            execution of this event handler. 0 for an empty memory area.
     *            Must not be negative.
     * 
     * @throws IllegalArgumentException
     *             if priority, parameters or if memSize is negative.
     */
    @MemoryAreaEncloses(inner = { "this", "this", "this" }, outer = {
            "priority", "parameters", "memSize" })
    @SCJAllowed
    public PeriodicEventHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageConfigurationParameters scp, long memSize) {
        super(null, null, null, 0, null);
    }

    /**
     * Constructor to create a periodic event handler.
     * <p>
     * Does not perform memory allocation. Does not allow this to escape local
     * scope. Builds links from this to priority, parameters, and name so those
     * three arguments must reside in scopes that enclose this.
     * <p>
     * @param priority
     *            specifies the priority parameters for this periodic event
     *            handler. Must not be null.
     * <p>
     * @param release
     *            specifies the periodic release parameters, in particular the
     *            start time and period. Note that a relative start time is not relative to
     *            NOW but relative to the point in time when initialization is
     *            finished and the timers are started. This argument must not be
     *            null.
     * <p>          
     * @param scp
     *            The scp parameter describes the organization of memory
     *            dedicated to execution of the underlying thread. (added by MS)
     * <p>
     * @param memSize
     *            the size in bytes of the private scoped memory area to be used for the
     *            execution of this event handler. 0 for an empty memory area.
     *            Must not be negative.
     * <p>
     * @throws IllegalArgumentException
     *             if priority parameters are null or if memSize is negative.
     */
    @MemoryAreaEncloses(inner = { "this", "this", "this", "this" }, outer = {
            "priority", "parameters", "memSize", "name" })
    @SCJAllowed(LEVEL_1)
    public PeriodicEventHandler(PriorityParameters priority,
            PeriodicParameters release, StorageConfigurationParameters scp, long memSize, String name) {
        super(null, null, null, 0, null);
    }
}
