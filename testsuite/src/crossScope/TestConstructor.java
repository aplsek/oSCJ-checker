//crossScope_A/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope;

import javax.safetycritical.annotate.CrossScope;

public class TestConstructor {

	 @CrossScope
     public void method(Bar b) {
		 Bar myBar =new Bar(b); 				 //---> OK if the constructor is @CrossScope
   
		 Bar bb=new Bar(b,b);				    // ERR
     }

	 class Bar{
		 int i;
		 Bar b;
		 
		 public Bar() {
			 //..
		 }
		 
		 @CrossScope
		 public Bar(Bar b) {
			 this.i = b.i;
			 this.b = b;    			// ERROR
		 }
		 
		 public Bar(Bar b, Bar b2) {
			 this.i = b.i;
			 this.b = b;   				 // OK but should not be called from "@CrossScope"
		 }
		 
	 }
}
