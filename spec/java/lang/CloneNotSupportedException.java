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

import java.io.Serializable;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;

@SCJAllowed
public class CloneNotSupportedException
   extends Exception
   implements Serializable
{

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public CloneNotSupportedException() {
  }
  
  /**
   * Shall not copy "this" to any instance or
   * static field. The scope containing the msg argument must enclose the
   * scope containing "this". Otherwise, an IllegalAssignmentError will be
   * thrown.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @MemoryAreaEncloses(inner = {"this"}, outer = {"msg"})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public CloneNotSupportedException(String msg) {
  }
}
