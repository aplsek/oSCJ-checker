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

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed
public abstract class Number implements Serializable {

  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Number() {
  }

  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public byte byteValue() {
    return (byte) 0; // skeleton
  }
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public abstract double doubleValue();
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public abstract float floatValue();
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public abstract int intValue();
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public abstract long longValue();
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public abstract short shortValue();
}
