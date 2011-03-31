/*---------------------------------------------------------------------*\
 *
 * Copyright (c) 2007-2009 Aonix, Paris, France
 *
 * This code is provided for educational purposes under the LGPL 2
 * license from GNU.  This notice must appear in all derived versions
 * of the code and the source must be made available with any binary
 * version.  
 *
\*---------------------------------------------------------------------*/
package java.io;

import javax.safetycritical.annotate.SCJAllowed;

/**
 * This interface is provided for compatibility with standard edition
 * Java.  However, JSR302 does not support serialization, so the
 * presence or absence of this interface has no visible effect within a
 * JSR302 application.
 */
@SCJAllowed
public interface Serializable{

}

