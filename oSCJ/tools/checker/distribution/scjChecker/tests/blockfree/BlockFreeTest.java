package blockfree;

import javax.safetycritical.annotate.*;

public class BlockFreeTest {
	public void foo() {
		foo2();
	}
	@BlockFree
	public void foo2() {
		foo3();
	}
	public void foo3() {
		
	}
}
