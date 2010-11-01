package crossScope;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.Allocate;
import static javax.safetycritical.annotate.Allocate.Area.*;
import javax.safetycritical.annotate.CrossScope;

public class TestFieldStore4 {

	@Scope("")
	@RunsIn("")
	class PEH {
		
		  public void handleEvent() {
		       Foo foo = getCurrentFoo();     // OK --> foo will be inferred to be "Immortal"    
		       Bar myBar = new Bar();       
		   
		       myBar = foo.mySimpleMethod(myBar);    // OK
		       
		       Bar myBar2 = new Bar();  
		       myBar = foo.myMethodLocal(myBar2);    // OK  ---> the assignement is ok since myMethod is @Allocate("current")
		        
		       foo = foo.getMyFoo(myBar2);			// ERROR
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
		}											// - TODO: if the default for @allocate is not "this"
		
		@Allocate({THIS})
		@CrossScope
		public Bar myMethod(Bar b) {
			return this.field;
		}
		
		@Allocate({CURRENT})						// ERRO, should be ...
		@CrossScope
		public Bar myMethodLocal(Bar b) {
			return b;
		}
		
		@Allocate({CURRENT})						// ERRO, should be ...
		@CrossScope
		public Foo getMyFoo(Bar b) {
			return null;
		}
	}
	
	class Bar {
	}
}
