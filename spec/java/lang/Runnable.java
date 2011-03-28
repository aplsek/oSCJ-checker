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
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Allocate.Area.IMMORTAL;
import static javax.safetycritical.annotate.Allocate.Area.MISSION;
import static javax.safetycritical.annotate.Allocate.Area.CURRENT;
import static javax.safetycritical.annotate.Allocate.Area.THIS;
import static javax.safetycritical.annotate.Allocate.Area.SCOPED;

@SCJAllowed
public interface Runnable {

  /**
   * The implementation of this method may, in general, perform allocations in
   * immortal memory.
   */
  @Allocate({CURRENT, IMMORTAL, MISSION, THIS, SCOPED})
  @SCJAllowed
  public void run();
}
