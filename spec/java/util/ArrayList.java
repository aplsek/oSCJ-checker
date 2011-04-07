package java.util;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class ArrayList {

    @SCJAllowed
    public ArrayList() {
    }
    
    @SCJAllowed
    public boolean add(Object e) {
        return true;
    }
}