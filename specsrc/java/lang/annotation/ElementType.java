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

/**
 * Unless specified to the contrary, see JSE 5.0 documentation.
 */
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public enum ElementType
{
  @SCJAllowed TYPE,
  @SCJAllowed FIELD,
  @SCJAllowed METHOD,
  @SCJAllowed PARAMETER,
  @SCJAllowed CONSTRUCTOR,
  @SCJAllowed LOCAL_VARIABLE,
  @SCJAllowed ANNOTATION_TYPE,
  @SCJAllowed PACKAGE
}
