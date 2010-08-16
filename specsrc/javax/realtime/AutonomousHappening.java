package javax.realtime;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;

  /**
   *
   */
@SCJAllowed(LEVEL_1)
public class AutonomousHappening extends EventHappening {

  /**
   * Creates a Happening in the current memory area with a system assigned name and id.
   */
  @SCJAllowed(LEVEL_1)
  public AutonomousHappening() {};

  /**
   * Creates a Happening in the current memory area with the specified id and a system-assigned name.
   */
  @SCJAllowed(LEVEL_1)
  public AutonomousHappening(int id) {};

  /**
   * Creates a Happening in the current memory area with the name and id given.
   */
  @SCJAllowed(LEVEL_1)
  public AutonomousHappening(int id, String name) {};

  /**
   * Creates a Happening in the current memory area with the name name and a system-assigned id.
   */
  @SCJAllowed(LEVEL_1)
  public AutonomousHappening(String name) {};

}
