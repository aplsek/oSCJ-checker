package java.util;

import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.THIS;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class Vector {

    @SCJAllowed
    public Vector() {

    }
    
    @SCJAllowed
    public int size() {
        return 0;
    }

    @SCJAllowed
    public Object elementAt(int index) {
        return null;
    }
    
    @SCJAllowed
    public void addElement(Object obj) {
    }
    
}