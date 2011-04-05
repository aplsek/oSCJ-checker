package java.util;

import java.util.AbstractSet;
import java.util.Set;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.THIS;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed
public class HashSet {
    
    @SCJAllowed
    public HashSet() {
        
    }
    
    /////@RunsIn(CALLER) //////@Scope(THIS)
    @SCJAllowed
    //@RunsIn(CALLER)
    public Object get(Object key) {
        return null;
    }
    
    @SCJAllowed
    public boolean add(Object e) {
        return true;
    }
}