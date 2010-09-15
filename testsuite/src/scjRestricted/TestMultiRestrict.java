package scjRestricted;


import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import javax.safetycritical.annotate.SCJRestricted;

public class TestMultiRestrict {

    @SCJRestricted(value=INITIALIZATION,mayAllocate=false)
    public void foo() {
        
    }
    
    @SCJRestricted(value=INITIALIZATION)
    public void foo2() {
        
    }
}
