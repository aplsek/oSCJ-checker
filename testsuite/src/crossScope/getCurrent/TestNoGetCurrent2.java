package crossScope.getCurrent;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.CrossScope;

public class TestNoGetCurrent2 {

	@CrossScope
	public void method(Foo f) {
		Bar b = new Bar();
		b.method();   							// OK, we call a non-@CrossScope method 
		
		Bar field = new Bar();
		b.setField(field);						// OK, we call a non-@CrossScope method
	
		b.testFoo(f);							// ERROR: can not be called since we do not know where "f" is.
	}
	
	class Bar {
		Bar b;
		Foo f; 
		
		public Bar() {
			this.b = null;
		}
		public void method() {
			Mission m = Mission.getCurrentMission(); 	// ERROR - Bar has no @scope annotation, can not call getCurrent
		}
		
		public void setField(Bar field) {
			this.b = field;						// OK
		}
		
		public void testFoo(Foo f) {			// OK
			this.f = f;							// OK
		}
	}
	
	
	class Foo {
	}
}