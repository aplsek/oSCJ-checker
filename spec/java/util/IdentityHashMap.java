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
class IdentityHashMap  {
   
    @SCJAllowed
    public IdentityHashMap () {
    }
    
    @SCJAllowed
    public Object put(Object key, Object value) {
        return null;
    }
   
    /////@RunsIn(CALLER) //////@Scope(THIS)
    @SCJAllowed
    //@RunsIn(CALLER)
    public Object get(Object key) {
        return null;
    }
    
    @SCJAllowed
    public Collection values() {
        return null;
    }
    
}
