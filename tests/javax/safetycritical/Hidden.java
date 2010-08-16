package javax.safetycritical;

import static javax.safetycritical.annotate.Level.HIDDEN;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class Hidden {
    
    @SCJAllowed(HIDDEN)
    public static void hiddenFoo() {
    }
}
