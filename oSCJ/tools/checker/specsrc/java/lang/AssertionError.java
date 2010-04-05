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
public class AssertionError extends Error implements Serializable {

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public AssertionError() {
  }
  
  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public AssertionError(boolean b) {
  }

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public AssertionError(char c) {
  }
  
  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public AssertionError(double d) {
  }

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public AssertionError(float f) {
  }

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public AssertionError(int i) {
  }

  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public AssertionError(long l) {
  }
  
  /**
   * Shall not copy "this" to any instance or
   * static field.
   * <p>
   * Allocates an application- and implementation-dependent amount of
   * memory in the current scope (to represent stack backtrace).
   */
  @Allocate({CURRENT})
  @BlockFree
  @MemoryAreaEncloses(inner = {"this"}, outer = {"o"})
  @SCJAllowed
  public AssertionError(Object o) {
  }
}
