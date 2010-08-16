//scjAllowed/TestNoSCJAllowed.java:10: Method call is not allowed at level 0.
//        NoAllowed.foo();
//                     ^
//1 error

package scjAllowed;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class TestNoSCJAllowed {
   
    @SCJAllowed
    public void foo () {
        NoAllowed.foo();
    }
}
