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

@SCJAllowed
public interface CharSequence {

  /**
   * Implementations of this method must not allocate memory and must not
   * allow "this" to escape the local variables.
   */
  @BlockFree
  @SCJAllowed
  public int length();
  
  /**
   * Implementations of this method must not allocate memory and must not
   * allow "this" to escape the local variables.
   */
  @BlockFree
  @SCJAllowed
  public char charAt(int index);
  
  /**
   * Implementations of this method may allocate a CharSequence object in the
   * scope of the caller to hold the result of this method. 
   * <p>
   * This method shall not allow "this" to escape the
   * local variables.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public CharSequence subSequence(int start, int end);
  
  /**
   * Implementations of this method may allocate a String object in the scope
   * of the caller to hold the result of this method. 
   * <p>
   * This method shall not allow "this" to escape the
   * local variables.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public String toString();
}


