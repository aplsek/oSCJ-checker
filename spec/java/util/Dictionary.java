package java.util;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.THIS;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed
public abstract class Dictionary {
    
    @SCJAllowed
    public Object get(Object key) {
        return null;
    }
}