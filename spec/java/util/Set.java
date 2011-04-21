package java.util;

import java.util.Iterator;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public interface Set {
    
    @SCJAllowed
    public Iterator iterator();
    
    @SCJAllowed
    boolean hasNext();
    
    
    @SCJAllowed
    public Object next(int i);

}
