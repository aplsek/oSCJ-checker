/*---------------------------------------------------------------------*\
 *
 * Copyright (c) 2007-2009 Aonix, Paris, France
 *
 * This code is provided for instructional purposes under the LGPL 2
 * license from GNU.  This notice must appear in all derived versions
 * of the code and the source must be made available with any binary
 * version.  
 *
\*---------------------------------------------------------------------*/

package java.lang.annotation;

import javax.safetycritical.annotate.SCJAllowed;

@Documented 
@Retention(RetentionPolicy.RUNTIME) 
@SCJAllowed
@Target(ElementType.ANNOTATION_TYPE)
public @interface Target {
  @SCJAllowed
  public ElementType[] value();
}
