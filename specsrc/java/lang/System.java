/*---------------------------------------------------------------------*\
 *
 * Copyright (c) 2007, 2008 Aonix, Paris, France
 *
 * This code is provided for educational purposes under the LGPL 2
 * license from GNU.  This notice must appear in all derived versions
 * of the code and the source must be made available with any binary
 * version.
 *
\*---------------------------------------------------------------------*/
package java.lang;

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public final class System
{
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  protected System() {}

  /**
   * Allocates no memory.
   * Does not allow "src" or "dest" arguments to escape local variables.
   * Allocates no memory.
   * <b>
   * Requires that the contents of array src enclose array dest.  TBD:
   * our annotation system doesn't have a way to describe this scope
   * constraint.
   */
  @BlockFree
  @SCJAllowed
  public static void arraycopy(Object src, int srcPos, Object dest,
                               int destPos, int length)
  {
  }

  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static long currentTimeMillis() {
    return (long) 0; // skeleton
  }

  /**
   * Allocates no memory.
   * <p>
   * Unlike traditional J2SE, this method shall not cause a set of
   * system properties to be created and initialized if not already
   * existing.  Any necessary initialization shall occur during system
   * startup.
   *
   * @return the value returned is either null or it resides in
   * immortal memory.
   */
  @BlockFree
  @SCJAllowed
  public static String getProperty(String key)
  {
    return (null); // skeleton
  }

  /**
   * Allocates no memory.
   * <p>
   * Unlike traditional J2SE, this method shall not cause a set of
   * system properties to be created and initialized if not already
   * existing.  Any necessary initialization shall occur during system
   * startup.
   *
   * @return The value of the property associated with key, or the
   * value of default_value if no property is associated with key.  The
   * value returned resides in immortal memory, or it is the value of
   * default.
   */
  @BlockFree
  @SCJAllowed
  public static String getProperty(String key, String default_value)
  {
    return (null); // skeleton
  }

  /**
   * Does not allow argument "x" to escape local variables. Allocates no
   * memory.
   */
  @BlockFree
  @SCJAllowed
  public static int identityHashCode(Object x)
  {
    return 0;
  }

  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static void exit(int code)
  {
  }


  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static long nanoTime() {
    return (long) 0; // skeleton
  }
}
