//crossScope_A/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope;

import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;
import javax.safetycritical.annotate.CrossScope;



public class TestFieldStore2 {
	
	class Foo {
		
		@Allocate(parameter="in")
		@CrossScope
		public Bar myMethod(Bar in) {
		    in.result = in.x + in.y;        // OK, primitive fields modified	
			return in;                      //  -----> OK
		}
		
		
		@Allocate({CURRENT})
		@CrossScope
		public Bar myMethod2(Bar in) {
		    in.result = in.x + in.y;        // OK, primitive fields modified	
			return in;                      //  -----> ERROR, we do not know that "in" is "current"
		}
		
		@Allocate({CURRENT})
		@CrossScope
		public Bar myMethod3(Bar in) {
			return new Bar();                  // OK
		}
		
		
	}

	class Bar{
		public long x,y;
		public long result;
	}
}
