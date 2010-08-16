package javax.safetycritical;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;

  /**
   * 
   */
@SCJAllowed
public class ManagedInterruptHappening extends InterruptHappening {

 /**
   * Creates a Happening in the current memory area with a system assigned name and id.
   */
  @SCJAllowed(LEVEL_1)
  public ManagedInterruptHappening() {};

  /**
   * Creates a Happening in the current memory area with the specified id and a system-assigned name.
   */
  @SCJAllowed(LEVEL_1)
  public ManagedInterruptHappening(int id) {};

  /**
   * Creates a Happening in the current memory area with the name and id given.
   */
  @SCJAllowed(LEVEL_1)
  public ManagedInterruptHappening(int id, String name) {};

  /**
   * Creates a Happening in the current memory area with the name name and a system-assigned id.
   */
  @SCJAllowed(LEVEL_1)
  public ManagedInterruptHappening(String name) {};
  
  /**
   * Called by the Infrastructure if an interrupt handler throws
   * an uncaught exception
   */
  @SCJAllowed(LEVEL_1)
  public void uncaughtException(Exception E) {};
}
