//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope.allocate;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

import static javax.safetycritical.annotate.Allocate.Area.*;
import javax.safetycritical.annotate.CrossScope;

@Scope("Immortal")
public class TestParameter {

	@DefineScope(name = "Mission", parent = "Immortal")
    PrivateMemory mission = new PrivateMemory(0);
	
	Bar b;
	
	@Allocate(parameter="b")
	@CrossScope
	public Bar method(Bar b) {				// OK
		return b;
	} 

	@Allocate(scope="Immortal")				// OK
	@CrossScope
	public Bar method2(Bar b, Foo f) {	    // OK 
		return this.b;						// OK - returned object lives in immortal
	} 
	
	@Allocate(scope="Mission")				// ERROR
	@CrossScope
	public Bar method3(Bar b, Foo f) {	    // OK 
		return this.b;						// OK - returned object lives in immortal
	} 
	
	@Allocate(scope="Mission")
	@CrossScope
	public Bar method4(Bar b, Foo f) {		//OK 
		return null;						// OK : null must work for any scope!
	} 
	
	@Allocate(scope="Immortal")
	@CrossScope
	public Bar methodNull(Bar b, Foo f) {   //OK 
		return null;						// OK : null must work for any scope!
	} 
	
	@Allocate(scope="Immortal")
	@CrossScope
	public void method5(Bar b, Foo f) {	   // ERROR: is void and had @Allocate
	} 
	
	@Allocate(scope="private")				// ERROR - the scope does not exist
	@CrossScope
	public Bar method6(Bar b) {		
		return null;						
	} 
	
	class Bar {}
	class Foo{}
}
