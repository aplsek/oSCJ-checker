//crossScope_A/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope_A;

import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;
import javax.safetycritical.annotate.CrossScope;



public class TestFieldStore2 {
	
	class Foo {
		@Allocate({CURRENT})
		@CrossScope
		public Bar myMethod(Bar in) {
		    in.result = in.x + in.y;        // TODO: is it OK or ERROR????? it should be OK, no?    	
			return in;                      //  -----> OK
		}
	}

	class Bar{
		public long x,y;
		public long result;
	}
}
