//scjAllowed/TestAllowedProtectedClash.java:18: @SCJAllowed(INFRASTRUCTURE) methods may not be called outside of javax.realtime or javax.safetycritical packages.
//        FakeSCJ.scjProtected();
//                            ^
//scjAllowed/TestAllowedProtectedClash.java:22: Methods outside of javax.realtime or javax.safetycritical packages cannot be annotated with @SCJAllowed(INFRASTRUCTURE).
//    public void bar() {
//                ^
//2 errors

package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;

@SCJAllowed
public class TestAllowedProtectedClash {
    
    @SCJAllowed
    public void foo() {
        FakeSCJ.scjProtected();
    }
    
    @SCJAllowed(INFRASTRUCTURE)
    public void bar() {
        
    }
}
