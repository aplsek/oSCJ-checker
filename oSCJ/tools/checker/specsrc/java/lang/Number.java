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

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public abstract class Number implements Serializable {

  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @BlockFree
  @SCJAllowed
  public Number() {
  }

  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @BlockFree
  @SCJAllowed
  public byte byteValue() {
    return (byte) 0; // skeleton
  }
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @BlockFree
  @SCJAllowed
  public abstract double doubleValue();
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @BlockFree
  @SCJAllowed
  public abstract float floatValue();
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @BlockFree
  @SCJAllowed
  public abstract int intValue();
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @BlockFree
  @SCJAllowed
  public abstract long longValue();
  
  /**
   * The implementation of this method shall not allow "this" to escape the
   * method's local variables.
   */
  @BlockFree
  @SCJAllowed
  public abstract short shortValue();
}
