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
public class Throwable implements Serializable {

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Invokes System.captureStackBacktrace(this) to save the back trace
   * associated with the current thread.
   */
  @SCJAllowed
  public Throwable() {
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
  public Throwable(Throwable cause) {
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
  public Throwable(String msg, Throwable cause) {
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
  @BlockFree
  @MemoryAreaEncloses(inner = {"this"}, outer = {"msg"})
  @SCJAllowed
  public Throwable(String msg) {
  }
  
  /**
   * Performs no memory allocation. Returns a reference to the same String
   * message that was supplied as an argument to the constructor, or null if
   * no message was specified at construction time.
   */
  @BlockFree
  @SCJAllowed
  public String getMessage() {
    return null;
  }

  /**
   * Performs no memory allocation. Returns a reference to the same Throwable
   * that was supplied as an argument to the constructor, or null if
   * no cause was specified at construction time.
   */
  @BlockFree
  @SCJAllowed
  public Throwable getCause() {
    return null;
  }
  
  /**
   * Shall not copy "this" to any instance or static field.
   * <p>
   * Allocates a StackTraceElement array, StackTraceElement objects,
   * and all internal structure, including String objects referenced
   * from each StackTraceElement to represent the 
   * stack backtrace information available for the exception that was
   * most recently associated with this Throwable object.
   * <p>
   * Each Schedulable maintains a single thread-local buffer to
   * represent the stack back trace information associated with the
   * most recent invocation of System.captureStackBacktrace().  The
   * size of this buffer is specified by providing a
   * StorageConfigurationParameters object as an argument to
   * construction of the Schedulable.
   * Most commonly, System.captureStackBacktrace() is invoked from
   * within the constructor of java.lang.Throwable.
   * getStackTrace() returns a representation of this
   * thread-local back trace information.
   * <p>
   * If System.captureStackBacktrace() has been invoked within this
   * thread more recently than the construction of this Throwable,
   * then the stack trace information returned from this method may
   * not represent the stack back trace for this particular
   * Throwable. 
  */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public StackTraceElement[] getStackTrace() throws IllegalStateException {
    return null;
  }

  // not scj allowed
  public void printStackTrace() 
  {
  }
}

