package javax.safetycritical;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value = LEVEL_1, members = true)
public class FakeSupport2 {
   
    @SCJAllowed(INFRASTRUCTURE)
    public int foo2() {
        return 0;
    }
}
