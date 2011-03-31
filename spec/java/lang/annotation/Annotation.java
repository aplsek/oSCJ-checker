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
package java.lang.annotation;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public interface Annotation
{
  @SCJAllowed
  public abstract boolean equals(Object o);

  @SCJAllowed
  public abstract int hashCode();

  @SCJAllowed
  public abstract String toString();

  @SCJAllowed
  public abstract Class<? extends Annotation> annotationType();

}
