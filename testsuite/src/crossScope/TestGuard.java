//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope;

import javax.realtime.MemoryArea;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

public class TestGuard {

	Bar b;

	@Allocate(parameter="b")
	@RunsIn(UNKNOWN)
	public Bar method(Bar b) {
		if (MemoryArea.getMemoryArea(this) == MemoryArea.getMemoryArea(b)) { //  ---> GUARD
			this.b = b;   // OK ---> field store in this is allowed because the runtime check guarantees that we are in the same scopes!
		}
		this.b = b;    // ERROR
		return b;
	} 

	class Bar {}
	
}
