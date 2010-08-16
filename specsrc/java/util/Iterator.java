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
package java.util;

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * TBD: James wants to think about whether this is really SCJAllowed.
 */
@SCJAllowed
public interface Iterator<E> {

  @BlockFree
  @SCJAllowed
  public boolean hasNext();

  @BlockFree
  @SCJAllowed
  public E next();

  @BlockFree
  @SCJAllowed
  void remove();
}
