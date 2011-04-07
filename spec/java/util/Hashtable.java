package java.util;

import java.io.*;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.THIS;

@SCJAllowed
public
class Hashtable  {
   
    @SCJAllowed
    public Hashtable () {
        
    }
    
    /////@RunsIn(CALLER) //////@Scope(THIS)
    //@RunsIn(CALLER)
    @SCJAllowed
    public Object get(Object key) {
        return null;
    }
    
}
