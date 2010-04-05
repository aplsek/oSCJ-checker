package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.PriorityParameters;
import javax.realtime.AperiodicParameters;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.MemoryAreaEncloses;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed(LEVEL_1)
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
     * @param mem_info
     *            The mem_info parameter describes the organization of memory
     *            dedicated to execution of the underlying thread.
     * 
     * @throws IllegalArgumentException
     *             if priority, parameters or if memSize is negative.
     */
// (annotations turned off to work with Java 1.4)     @MemoryAreaEncloses(inner = { "this", "this", "this" }, outer = {
//            "priority", "release_info", "mem_info" })
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public AperiodicEventHandler(PriorityParameters priority,
            AperiodicParameters release, StorageConfigurationParameters scp,
            long memSize) {
        this(priority, release, scp, memSize, null);
    }

    /**
     * Constructor to create an aperiodic event handler.
     * <p>
     * Does not perform memory allocation. Does not allow this to escape local
     * scope. Builds links from this to priority, parameters, and name so those
     * three arguments must reside in scopes that enclose this.
     * <p>
     * 
     * @param priority
     *            specifies the priority parameters for this periodic event
     *            handler. Must not be null.
     *            <p>
     * @param release_info
     *            specifies the periodic release parameters, in particular the
     *            deadline and deadline miss handlers.
     *            <p>
     * @param mem_info
     *            The mem_info parameter describes the organization of memory
     *            dedicated to execution of the underlying thread.
     *            <p>
     * 
     * 
     * @throws IllegalArgumentException
     *             if priority, parameters or if memSize is negative.
     */
// (annotations turned off to work with Java 1.4)     @MemoryAreaEncloses(inner = { "this", "this", "this", "this" }, outer = {
//            "priority", "release_info", "mem_info", "name" })
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public AperiodicEventHandler(PriorityParameters priority,
            AperiodicParameters release, StorageConfigurationParameters scp,
            long memSize, String name) {
        super(priority, release, scp, memSize, name);
    }
}
