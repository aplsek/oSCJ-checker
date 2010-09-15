//blockfree/BlockFreeTest.java:17: Illegal invocation of a method annotated MAY_BLOCK from within a method annotated BLOCK_FREE
//        foo3();
//            ^
//1 error

package blockfree;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class BlockFreeTest2 {
        public void foo() {
            foo2();
        }
        
        public void foo2() {
            foo3();
        }
        
        @SCJRestricted(maySelfSuspend=true)
        public void foo3() {                            // ERROR
            
        }
}
