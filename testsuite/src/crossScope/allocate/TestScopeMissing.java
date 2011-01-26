//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope.allocate;

import javax.safetycritical.annotate.Scope;


public class TestScopeMissing {

	Bar b;
	
	@Scope("b")
	public Bar method(Bar b) {
		return b;
	} 
	
	@Scope("bbb")
	public Bar method(Bar b, Foo f) {		/// ERROR : no such parameter exists
		return b;
	} 
	
	public Bar method() {
		return this.b;
	}
	
	public Bar methodErr(Bar b) {
		return b;							// must return this.b!! in case Bar is not the same scope1!
	} 

	class Bar {}
	
	class Foo{}
	
}
