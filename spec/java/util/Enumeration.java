
package java.util;

import java.util.NoSuchElementException;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public interface Enumeration {
    
    @SCJAllowed
    boolean hasMoreElements();

    @SCJAllowed
    Object nextElement();

}