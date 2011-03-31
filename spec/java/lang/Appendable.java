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
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Allocate.Area.THIS;

@SCJAllowed
public interface Appendable {

  @Allocate({THIS})
  @SCJAllowed
  public Appendable append(char c);

  @Allocate({THIS})
  @SCJAllowed
  public Appendable append(CharSequence csq);

  @Allocate({THIS})
  @SCJAllowed
  public Appendable append(CharSequence csq, int start, int end);
}
