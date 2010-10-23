package crossScope_A;

import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;
import javax.safetycritical.annotate.CrossScope;

public class TestFieldStore4 {

	class PEH {
		  void handleEvent() {
		       Foo foo = getCurrentFoo();     // OK --> foo will be inferred to be "Immortal"    
		       Bar myBar = new Bar();       
		   
		       myBar = foo.mySimpleMethod(myBar);    // OK
		       
		       Bar myBar2 = new Bar();  
		       myBar = foo.myMethodLocal(myBar2);    // OK  ---> the assignement is ok since myMethod is @Allocate("current")
		        						    //  ---> parameter myBar is "borrowed" and therefore its ok, otherwise it would be an error.
		  }	
		  
		  @Allocate(scope="immortal")
		  public Foo getCurrentFoo() {
			  return null;
		  }
		}
	
	class Foo {
		
		private Bar field;
		
		@CrossScope
		public Bar mySimpleMethod(Bar b) {
			return this.field;						// ERROR: has not @Allocate, we loose scope info!!
		}
		
		@Allocate({THIS})
		@CrossScope
		public Bar myMethod(Bar b) {
			return this.field;
		}
		
		@Allocate({CURRENT})
		@CrossScope
		public Bar myMethodLocal(Bar b) {
			return b;
		}
	}
	
	class Bar {
	}
}
