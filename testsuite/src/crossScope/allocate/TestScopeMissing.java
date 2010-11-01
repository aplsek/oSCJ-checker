//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope.allocate;

import static javax.safetycritical.annotate.Allocate.Area.THIS;

import javax.safetycritical.annotate.Allocate;


public class TestScopeMissing {

	Bar b;
	
	@Allocate(parameter="b")
	public Bar method(Bar b) {
		return b;
	} 
	
	@Allocate(parameter="bbb")
	public Bar method(Bar b, Foo f) {		/// ERROR : no such parameter exists
		return b;
	} 
	
	@Allocate({THIS})
	public Bar method() {
		return this.b;
	}
	
	@Allocate({THIS})
	public Bar methodErr(Bar b) {
		return b;							// must return this.b!! in case Bar is not the same scope1!
	} 

	class Bar {}
	
	class Foo{}
	
}
