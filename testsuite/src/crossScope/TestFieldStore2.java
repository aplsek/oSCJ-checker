//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope;

import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


public class TestFieldStore2 {
	
	class Foo {
		
		@Scope("in")
		@RunsIn(UNKNOWN)
		public Bar myMethod(Bar in) {
		    in.result = in.x + in.y;        // OK, primitive fields modified	
			return in;                      //  -----> OK
		}
		
		
		@RunsIn(UNKNOWN)
		public Bar myMethod2(Bar in) {
		    in.result = in.x + in.y;        // OK, primitive fields modified	
			return in;                      //  -----> ERROR, we do not know that "in" is "current"
		}
		
		@RunsIn(UNKNOWN)
		public Bar myMethod3(Bar in) {
			return new Bar();                  // OK
		}
		
		
	}

	class Bar{
		public long x,y;
		public long result;
	}
}
