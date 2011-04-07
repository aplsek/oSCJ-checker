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
public class HashMap  {
   
    @SCJAllowed
    public HashMap () {
        
    }
    
    @SCJAllowed
    public Object put(Object key, Object value) {
        return null;
    }
   
   
    @SCJAllowed
    @RunsIn(CALLER) @Scope(THIS)
    public Object get(Object key) {
        return null;
    }
    
    @SCJAllowed
    public Collection values() {
        return null;
    }
    
    @SCJAllowed
    public boolean containsKey(Object key) {
        return true;
    }
    
    @SCJAllowed
    public void clear() {}
}
