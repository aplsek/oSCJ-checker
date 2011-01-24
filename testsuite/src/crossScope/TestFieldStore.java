//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope;

import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

public class TestFieldStore {

	@RunsIn(UNKNOWN)
    public void method(Bar b) {
		 Bar myBar =new Bar(b); 				 //---> OK if the constructor is @RunsIn(UNKNOWN)
  
		 Bar bb=new Bar();				        // OK
		 
		 bb.updateField(myBar);					// OK
    }

	 class Bar{
		 int i;
		 Bar next;
		 
		 public Bar() {
			 
		 }
		 
		 @RunsIn(UNKNOWN)
		 public Bar(Bar b) {
			 this.i = b.i;
		 }
		 
		 @RunsIn(UNKNOWN)
		 public Bar(Bar b, Bar b2) {
		 }
		 
		 @RunsIn(UNKNOWN)
		 public void updateField(Bar b) {
			 this.next = b;								// ERROR cause we are cross scope
		 }
		 
	 }
}
