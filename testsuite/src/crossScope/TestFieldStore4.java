package crossScope;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

public class TestFieldStore4 {

	@Scope("")
	@RunsIn("")
	class PEH {
		
		  public void handleAsyncEvent() {
		       Foo foo = getCurrentFoo();     // OK --> foo will be inferred to be "Immortal"    
		       Bar myBar = new Bar();       
		   
		       myBar = foo.mySimpleMethod(myBar);    // OK
		       
		       Bar myBar2 = new Bar();  
		       myBar = foo.myMethodLocal(myBar2);    // OK  ---> the assignement is ok since myMethod is @Allocate("current")
		        
		       foo = foo.getMyFoo(myBar2);			// ERROR
		  }	
		  
		  @Scope("immortal")
		  public Foo getCurrentFoo() {
			  return null;
		  }
		}
	
	class Foo {
		
		private Bar field;
		
		@RunsIn(UNKNOWN)
		public Bar mySimpleMethod(Bar b) {
			return this.field;						// ERROR: has not @Allocate, we loose scope info!!
		}											// - TODO: if the default for @allocate is not "this"
		
		@RunsIn(UNKNOWN)
		public Bar myMethod(Bar b) {
			return this.field;
		}
		
		@RunsIn(UNKNOWN)
		public Bar myMethodLocal(Bar b) {
			return b;
		}
		
		@RunsIn(UNKNOWN)
		public Foo getMyFoo(Bar b) {
			return null;
		}
	}
	
	class Bar {
	}
}
