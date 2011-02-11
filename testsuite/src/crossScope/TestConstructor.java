//testsuite/src/crossScope/TestConstructor.java:30: @RunsIn annotations not allowed on constructors.
//                 public Bar(Bar b) {
//                        ^
//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//3 errors

package crossScope;


import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

public class TestConstructor {

	 @RunsIn(UNKNOWN)
     public void method(Bar b) {
		 Bar myBar =new Bar(b); 				 //---> OK if the constructor is @RunsIn(UNKNOWN)
   
		 Bar bb=new Bar(b,b);				    // ERROR
     }

	 class Bar{
		 int i;
		 Bar b;
		 
		 public Bar() {
			 //..
		 }
		 
		 @RunsIn(UNKNOWN)         // ERROR
		 public Bar(Bar b) {
			 this.i = b.i;
			 this.b = b;    			// ERROR ???
		 }
		
		 public Bar(Bar b, Bar b2) {
			 this.i = b.i;
			 this.b = b;   				 // OK but should not be called from "@RunsIn(UNKNOWN)"
		 }
		 
	 }
}
