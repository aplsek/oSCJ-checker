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
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;

@SCJAllowed
public class Exception extends Throwable implements Serializable {

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Invokes System.captureStackBacktrace(this) to save the back trace
   * associated with the current thread.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public Exception() {
  }
  
  /**
   * Shall not copy "this" to any instance or
   * static field. The scope containing the msg argument must enclose the
   * scope containing "this". Otherwise, an IllegalAssignmentError will be
   * thrown.
   * <p>
   * Invokes System.captureStackBacktrace(this) to save the back trace
   * associated with the current thread.
   */
  @Allocate({CURRENT})
  @BlockFree
  @MemoryAreaEncloses(inner = {"this"}, outer = {"msg"})
  @SCJAllowed
  public Exception(String msg) {
  }

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Does not invoke System.captureStackBacktrace(this) so as to not
   * overwrite the backtrace associated with cause.
   */
  @BlockFree
  @MemoryAreaEncloses(inner = {"this"}, outer = {"cause"})
  @SCJAllowed
  public Exception(Throwable cause) {
  }

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Does not invoke System.captureStackBacktrace(this) so as to not
   * overwrite the backtrace associated with cause.
   */
  @BlockFree
  @MemoryAreaEncloses(inner = {"this", "this"}, outer = {"cause", "msg"})
  @SCJAllowed
  public Exception(String msg, Throwable cause) {
  }
}