//crossScope_A/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope_A;

import javax.realtime.MemoryArea;
import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.Allocate;


public class TestGuard {

	Bar b;

	@Allocate(parameter="b")
	@CrossScope
	public Bar method(Bar b) {
		if (MemoryArea.getMemoryArea(this) == MemoryArea.getMemoryArea(b)) { //  ---> GUARD
			this.b = b;   // OK ---> field store in this is allowed because the runtime check guarantees that we are in the same scopes!
		}
		this.b = b;    // ERROR
		return b;
	} 

	class Bar {}
	
}
