//crossScope/TestTransitiveCall.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope;

import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

public class TestTransitiveCall {

		@RunsIn(UNKNOWN)
		public void method(Bar b) {
			foo(b);							// OK since that method is @RunsIn(UNKNOWN)
			
			foo2(b);					 	// ERROR, foo2 is not @RunsIn(UNKNOWN)
			
			Type t = new Type();
			bar(t);							//OK since the type t is local and the same scope as this.
		}
		
		@RunsIn(UNKNOWN)
		public void foo(Bar b) {
		}
		
		public void foo2(Bar b) {
		}
		
		public void bar(Type t) {
		}
		
		class Bar {
		}
		
		class Type{
		}
}
