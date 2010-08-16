package javax.realtime;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;

  /**
   *
   */
@SCJAllowed(LEVEL_1)
public abstract class Happening {

  /**
   * Find a happening by its name. 
   */
  @SCJAllowed(LEVEL_1)
  public static Happening getHappening(String name) { return null; }

 /**
   * Return the id of this happening. 
   */
  @SCJAllowed(LEVEL_1)
  public final int getId() {return 1; }

  /**
   * Return the ID of the happening with the name name. If there is not happening with that name return 0.
   */
  @SCJAllowed(LEVEL_1)
  public static int getId(java.lang.String name) {return 1; }

  /**
   * Returns the string name of this happening
   */
  @SCJAllowed(LEVEL_1)
  public final String getName() {return "Happ"; }

  /**
   * Is there a Happening with name name?
   * @return True if there is.
   */
  @SCJAllowed(LEVEL_1)
  public static boolean isHappening(java.lang.String name) {return false; }

  /**
   * @return Return true if this happening is presently registered. 
   */
  @SCJAllowed(LEVEL_1)
  public boolean isRegistered() {return false; }

  /**
   * Register this Happening. 
   * @mem 
   * @throws ???? if called from outside the mission initialization phase.
   */
  @SCJAllowed(LEVEL_1)
  public final void register() {}

  /**
   * Causes the happening corresponding to happeningId to occur.
   * @return  true if a happening with id happeningId was found, false otherwise.
   */
  @SCJAllowed(LEVEL_1)
  public static final boolean trigger(int happeningId) {return true; }

  /**
   * Unregister this Happening. 
   * @throws ???? if called from outside the mission initialization phase.
   */
  @SCJAllowed(LEVEL_1)
  public final void unRegister() {}
}
