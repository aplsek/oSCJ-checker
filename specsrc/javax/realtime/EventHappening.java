package javax.realtime;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;

  /**
   *
   */
@SCJAllowed(LEVEL_1)
public abstract class EventHappening extends Happening {

  /**
   * Attach the AsyncEvent ae to this Happening.
   * ADD LEVEL CONSTRAINTS????
   *  @throws ???? if called from outside the mission initialization phase. 
   */
  @SCJAllowed(LEVEL_1)
  public void attach(AsyncEvent ae) {}

 /**
   * Detach the AsyncEvent ae from this Happening.
   *  @throws ???? if called from outside the mission initialization phase.
   */
  @SCJAllowed(LEVEL_1)
  public void detach(AsyncEvent ae) {}
}
