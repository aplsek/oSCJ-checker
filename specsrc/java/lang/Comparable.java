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
public interface Comparable<T> {

  /**
   * The implementation of this method shall not allocate memory and shall not
   * allow "this" or "o" argument to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int compareTo(T o) throws ClassCastException;
}
