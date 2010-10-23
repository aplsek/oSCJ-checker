package crossScope;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.CrossScope;
import static javax.safetycritical.annotate.Allocate.Area.*;

public class TestInference {

	class Foo {

		Bar x;

		@Allocate(parameter="bar")
	    @CrossScope
		public Bar method(Bar bar) {
			
			return bar;
		}
		
		
		
		@Allocate({THIS})
	    public Bar method2() {
			return this.x;
		}	 
	}


	class Bar {
		
	}
	
}
