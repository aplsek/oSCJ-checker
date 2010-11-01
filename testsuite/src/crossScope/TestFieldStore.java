//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope;

import javax.safetycritical.annotate.CrossScope;


public class TestFieldStore {

	@CrossScope
    public void method(Bar b) {
		 Bar myBar =new Bar(b); 				 //---> OK if the constructor is @CrossScope
  
		 Bar bb=new Bar();				        // OK
		 
		 bb.updateField(myBar);					// OK
    }

	 class Bar{
		 int i;
		 Bar next;
		 
		 public Bar() {
			 
		 }
		 
		 @CrossScope
		 public Bar(Bar b) {
			 this.i = b.i;
		 }
		 
		 @CrossScope
		 public Bar(Bar b, Bar b2) {
		 }
		 
		 @CrossScope
		 public void updateField(Bar b) {
			 this.next = b;								// ERROR cause we are cross scope
		 }
		 
	 }
}
