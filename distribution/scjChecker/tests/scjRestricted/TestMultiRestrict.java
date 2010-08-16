//scjRestricted/TestMultiRestrict.java:17: @SCJRestricted annotation may only contain one of: ANY_TIME, CLEANUP, INITIALIZATION
//    public void foo2() {
//                ^
//1 error

package scjRestricted;


import static javax.safetycritical.annotate.Restrict.INITIALIZATION;
import static javax.safetycritical.annotate.Restrict.ALLOCATE_FREE;
import static javax.safetycritical.annotate.Restrict.CLEANUP;
import javax.safetycritical.annotate.SCJRestricted;

public class TestMultiRestrict {

    @SCJRestricted({INITIALIZATION,ALLOCATE_FREE})
    public void foo() {
        
    }
    
    @SCJRestricted({INITIALIZATION,CLEANUP})
    public void foo2() {
        
    }
}
