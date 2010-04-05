package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.AperiodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;


@SCJAllowed(LEVEL_1)
public abstract class AperiodicEventHandler extends ManagedEventHandler {

    /**
     * Constructor to create an aperiodic event handler.
     * <p>
     * Does not perform memory allocation. Does not allow this to escape local
     * scope. Builds links from this to priority and parameters, so those two
     * arguments must reside in scopes that enclose this.
     * 
     * @param priority
     *            specifies the priority parameters for this periodic event
     *            handler. Must not be null.
     * 
     * @param release_info
     *            specifies the periodic release parameters, in particular the
     *            start time, period and deadline miss and cost overrun
     *            handlers. Note that a relative start time is not relative to
     *            NOW but relative to the point in time when initialization is
     *            finished and the timers are started. This argument must not be
     *            null. TBD whether we support deadline misses and cost overrun
     *            detection.
     * 
     * @param scp
     *            The mem_info parameter describes the organization of memory
     *            dedicated to execution of the underlying thread.
     * 
     * @param memSize
     *            the size in bytes of the memory area to be used for the
     *            execution of this event handler. 0 for an empty memory area.
     *            Must not be negative. (added by MS)
     *            
     * @throws IllegalArgumentException
     *             if priority, parameters or if memSize is negative.
     */
    @MemoryAreaEncloses(inner = { "this", "this", "this" }, outer = {
            "priority", "release_info", "mem_info" })
    @SCJAllowed(LEVEL_1)
    public AperiodicEventHandler(PriorityParameters priority,
            AperiodicParameters release_info,
            StorageConfigurationParameters scp, long memSize) {
        super(null, null, null, 0, null);
    }

    /**
     * Constructor to create an aperiodic event handler.
     * <p>
     * Does not perform memory allocation. Does not allow this to escape local
     * scope. Builds links from this to priority, parameters, and name so those
     * three arguments must reside in scopes that enclose this.
     * <p>
     * @param priority
     *            specifies the priority parameters for this periodic event
     *            handler. Must not be null.
     * <p>
     * @param release_info
     *            specifies the periodic release parameters, in particular the
     *            deadline and deadline miss handlers.
     * <p>
     * @param scp
     *            The mem_info parameter describes the organization of memory
     *            dedicated to execution of the underlying thread.
     * <p>
     * @param memSize
     *            the size in bytes of the memory area to be used for the
     *            execution of this event handler. 0 for an empty memory area.
     *            Must not be negative. (added by MS)
     * 
     * 
     * @throws IllegalArgumentException
     *             if priority, parameters or if memSize is negative.
     */
    @MemoryAreaEncloses(inner = { "this", "this", "this", "this" }, outer = {
            "priority", "release_info", "mem_info", "name" })
    @SCJAllowed(LEVEL_1)
    public AperiodicEventHandler(PriorityParameters priority,
            AperiodicParameters release_info,
            StorageConfigurationParameters scp, long memSize, String name) {
        super(null, null, null, 0, null);
    }
}
