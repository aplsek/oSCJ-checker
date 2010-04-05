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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.safetycritical.annotate.SCJAllowed;

@Retention(value = RetentionPolicy.SOURCE)
@SCJAllowed
@Target(value = { ElementType.TYPE, ElementType.FIELD, ElementType.METHOD,
		ElementType.PARAMETER, ElementType.CONSTRUCTOR,
		ElementType.LOCAL_VARIABLE })
public @interface SuppressWarnings {
	public String[] value();
}
