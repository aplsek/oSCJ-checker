/*---------------------------------------------------------------------*\
 *
 * Copyright (c) 2007, 2008, 2009 Aonix, Paris, France
 *
 * This code is provided for educational purposes under the LGPL 2
 * license from GNU.  This notice must appear in all derived versions
 * of the code and the source must be made available with any binary
 * version.  
 *
\*---------------------------------------------------------------------*/
package java.lang;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;

@SCJAllowed
public class StackTraceElement {

  /**
   * Shall not copy "this" to any instance or
   * static field.
   */
  @MemoryAreaEncloses(inner = {"this", "this", "this"},
                      outer = {"declaringClass", "methodName", "fileName"})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public StackTraceElement(String declaringClass,
                           String methodName,
                           String fileName,
                           int lineNumber)
  {
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "obj" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean equals(Object obj) {
    return false; // skeleton
  }
  
  /**
   * Performs no memory allocation. Returns a reference to the same String
   * message that was supplied as an argument to the constructor, or null if
   * the class name was not specified at construction time.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String getClassName() {
    return null;
  }
  
  /**
   * Performs no memory allocation. Returns a reference to the same String
   * message that was supplied as an argument to the constructor, or null if
   * the file name was not specified at construction time.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String getFileName() {
    return null;
  }
  
  /**
   * Performs no memory allocation. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int getLineNumber() {
    return 0;
  }
  
  /**
   * Performs no memory allocation. Returns a reference to the same String
   * message that was supplied as an argument to the constructor, or null if
   * the method name was not specified at construction time.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String getMethodName() {
    return null;
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int hashCode() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isNativeMethod() {
    return false; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's
   * scope. (Note: this 
   * semantics is desired for consistency with overridden implementation of
   * Object.toString()).
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String toString() {
    return null; // skeleton
  }
  
}



