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

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

/**
 * TBD: James wants to think about whether this is really SCJAllowed.
 */
@SCJAllowed
public interface Iterator<E> {

  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean hasNext();

  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public E next();

  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  void remove();
}
