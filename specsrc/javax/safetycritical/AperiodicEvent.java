package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.AsyncEvent;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Allocate.Area;

@SCJAllowed(LEVEL_1)
public class AperiodicEvent extends AsyncEvent
{

  /**
   * Constructor for an aperiodic event that is linked to a given handler.
   * <p>
   * Does not allocate memory. Does not allow this to escape the local
   * variables. Builds a link from ``this" to handler, so handler must reside
   * in memory that encloses ``this".
   * <p>
   * @param handler
   *            -- the handler that is to be added to this event.
   */
  @Allocate( { Area.THIS })
  @MemoryAreaEncloses(inner = { "this" }, outer = { "handler" })
  @SCJAllowed(LEVEL_1)
  public AperiodicEvent(AperiodicEventHandler handler) {}

  /**
   * Constructor for an aperiodic event that is linked to multiple handlers.
   * <p>
   * Does not allow this or handlers to escape the local variables. Allocates
   * and initializes an array of AperiodicEventHandler within the same scope
   * as this in order to copy the handlers array. The elements of
   * the handlers array must reside in memory areas that enclose this.
   * <p>
   * Aside: we do not need to require that ``handlers" encloses
   * ``this", because 
   * we need to make a copy of handlers in order to be robust. However, we do
   * need to required that handlers[i] encloses this for every value
   * of i. Our 
   * existing notation does not allow us to say what we might want to say.
   * What I have said is sufficient, but not necessary.
   * 
   * @param handlers
   *            the handlers that are to be added to this event.
   */
  @Allocate({ Area.THIS })
  @MemoryAreaEncloses(inner = { "this" }, outer = { "handlers" })
  @SCJAllowed(LEVEL_1)
  public AperiodicEvent(AperiodicEventHandler[] handlers) {}
}
