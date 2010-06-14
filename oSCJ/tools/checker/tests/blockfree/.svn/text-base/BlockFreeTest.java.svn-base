//blockfree/BlockFreeTest.java:17: Illegal invocation of a method annotated MAY_BLOCK from within a method annotated BLOCK_FREE
//        foo3();
//            ^
//1 error

package blockfree;

import javax.safetycritical.annotate.*;

public class BlockFreeTest {
	public void foo() {
		foo2();
	}
	
	@SCJRestricted({Restrict.BLOCK_FREE})
	public void foo2() {
		foo3();
	}
	public void foo3() {
		
	}
}
