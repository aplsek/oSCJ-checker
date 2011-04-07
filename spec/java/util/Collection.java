package java.util;

import java.util.Iterator;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public interface Collection{
    
    @SCJAllowed
    Iterator iterator();
}