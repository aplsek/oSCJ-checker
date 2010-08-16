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

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;
import static javax.safetycritical.annotate.Level.LEVEL_2;

@SCJAllowed
public class Object {

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public Object() { // skeleton
  }
  
  /**
   *
   */
  public void finalize() {
  }

  /**
   * On our April 3, 2009 teleconference, we resolved that the clone()
   * method and the Cloneable interface would not be @SCJAllowed.
   * We all agreed that it is desirable for the standard libraries to
   * provide a mechanism for deeply copying an arbitrary object.
   * However, the Cloneable interface and clone() method, as defined in
   * standard edition Java, do not represent a reliable service.  No
   * replacement has been defined (yet).
   */
  protected Object clone() throws CloneNotSupportedException { return null; }

  /**
   * Allocates no memory. Does not allow "this" or "obj" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean equals(Object obj) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "obj" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public final Class<? extends Object> getClass() {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int hashCode() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public final void notify() { // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public final void notifyAll() { // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public String toString() {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed(LEVEL_2)
  public final void wait() throws InterruptedException { // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed(LEVEL_2)
  public final void wait(long timeout)
    throws InterruptedException
  { // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed(LEVEL_2)
  public final void wait(long timeout, int nanos)
    throws InterruptedException
  { // skeleton
  }
}
