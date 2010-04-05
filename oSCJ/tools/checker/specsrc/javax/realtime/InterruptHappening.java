package javax.realtime;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;

  /**
   * Note: IT IS NOT CLEAR WHICH PACKAGE THIS LIVES IN
   * IF THIS DOES NOT APPEAR I AN RTSJ EXTENSION PACKAGE
   * THEN THIS AND ManagedInterruptHappenings SHOULD BE MERGED.
   */
@SCJAllowed(LEVEL_1)
public class InterruptHappening extends Happening {

  /**
   *
   */
  @SCJAllowed(LEVEL_1)
  public InterruptHappening() {};

  /**
   *
   */
  @SCJAllowed(LEVEL_1)
  public InterruptHappening(int id) {};

  /**
   *
   */
  @SCJAllowed(LEVEL_1)
  public InterruptHappening(int id, String name) {};

  /**
   *
   */
  @SCJAllowed(LEVEL_1)
  public InterruptHappening(String name) {};
  
  
  /**
   *
   */
  @SCJAllowed(LEVEL_1)
  protected void process()  {}; 
  
  /**
   *
   */
  @SCJAllowed(LEVEL_1)
  public final int getPriority(int id)   { return 1;};
  
}
