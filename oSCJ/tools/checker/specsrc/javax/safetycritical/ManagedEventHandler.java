package javax.safetycritical;

import javax.realtime.BoundAsyncEventHandler;
import javax.realtime.PriorityParameters;
import javax.realtime.ReleaseParameters;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_0;

@SCJAllowed(LEVEL_0)
public abstract class ManagedEventHandler extends BoundAsyncEventHandler
{
    /**
     * Constructor to create an event handler.
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

  @SCJAllowed(LEVEL_0)
  public ManagedEventHandler(PriorityParameters priority,
                             ReleaseParameters release,
                             StorageConfigurationParameters scp,
                             long memSize, String name) {}

  /**
   * Application developers override this method with code to be executed when
   * this event handler's execution is disabled (upon termination of the
   * enclosing mission).
   *
   */
  @SCJAllowed(LEVEL_0)
  protected void cleanup() {}

  /**
   * This is overridden to ensure entry into the local scope for each release.
   * This may change for RTSJ 1.1, where a provided scope is automatically
   * entered at each release.
   */
  @Override
  @SCJAllowed(LEVEL_0)
  public final void handleAsyncEvent(){}

  /**
   * Application developers override this method with code to be executed
   * whenever the event(s) to which this event handler is bound is fired.
   */
  @SCJAllowed(LEVEL_0)
  public abstract void handleEvent();

  public String getName() { return null; }
}
