//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error
package crossScope_A;

import javax.safetycritical.annotate.CrossScope;

public class TestMethod {


	class Foo {
		@CrossScope
		public void method1(Bar b) {			// OK
		}
		
		@CrossScope
		public void method2() {					// ERROR : does not have any input parameter 
		}
	}
	
	class Bar {}
}
