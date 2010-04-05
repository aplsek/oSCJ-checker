//scjAllowed/TestAllowedProtectedClash.java:13: @SCJProtected methods may not be called outside of javax.realtime or javax.safetycritical packages.
//        FakeSCJ.scjProtected();
//                            ^
//1 error

package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;

@SCJAllowed
public class TestAllowedProtectedClash {
    
    @SCJAllowed
    public void foo() {
        FakeSCJ.scjProtected();
    }
    
    @SCJProtected
    public void bar() {
        
    }
}
