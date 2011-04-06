package java.util;

import java.util.Iterator;
import java.util.List;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class LinkedList {
    
    @SCJAllowed
    public Iterator iterator() {
        return null;
    }
    
    @SCJAllowed
    public int size() {
        return 0;
    }
}