package javax.realtime;

import javax.safetycritical.annotate.SCJAllowed;

  /**
   * Note:
   */
@SCJAllowed
public class ControlledHappening extends EventHappening {



 /**
   *
   */
  @SCJAllowed
  public ControlledHappening() {};

  /**
   *
   */
  @SCJAllowed
  public ControlledHappening(int id) {};

  /**
   *
   */
  @SCJAllowed
  public ControlledHappening(int id, String name) {};

  /**
   *
   */
  @SCJAllowed
  public ControlledHappening(String name) {};


  /**
   *
   */
  @SCJAllowed
  public final void attach(AsyncEvent ae) {} ;

  /**
   *
   */
  @SCJAllowed
  protected void process() {};

  /**
   *
   */
  @SCJAllowed
  public final void takeControl()   {};

  /**
   *
   */
  @SCJAllowed
  public final void takeControlInterruptible() {};

  /**
   *
   */
  @SCJAllowed
  protected final Object visit(EventExaminer logic)  { return null; };

}
