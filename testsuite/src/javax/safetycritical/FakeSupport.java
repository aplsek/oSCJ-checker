package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(LEVEL_1)
public class FakeSupport extends FakeSupport2 {

    @SCJAllowed(SUPPORT)
    public int foo() {
        return 0;
    }
    
    @SCJAllowed(SUPPORT)
    public int foo2() {
        return 0;
    }
    
    @SCJAllowed(INFRASTRUCTURE)
    public int bar() {       
        return 1;
    }
    
    @SCJAllowed(INFRASTRUCTURE)
    public static int infrastructureCall() {       
        return 1;
    }
}
