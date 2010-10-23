//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope_A;

import javax.safetycritical.annotate.CrossScope;

public class TestMethodOverride {
	
	class Foo {
		@CrossScope
		public void method1(Bar b) {
		}
		@CrossScope
		public void method2(Bar b) {
		}
	}
	
	class BigFoo extends Foo {
		@CrossScope
		public void method(Bar b) {					// OK
		}

		public void method2(Bar b) {				// ERROR, overriding method is not @CrossScope
		}
	
	}
	
	class Bar {}
}
