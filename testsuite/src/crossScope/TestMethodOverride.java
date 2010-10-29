//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope;

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

		Bar bar;
		
		@CrossScope
		public void method(Bar b) {					// OK
		}

		public void method2(Bar b) {				// OK, overriding method is inheriting @CrossScope from above 
													// we automatically allow this
			
			this.bar = new Bar();					// ERROR 
		}
	
	}
	
	class Bar {}
}
