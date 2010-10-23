//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope_A.inference;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Allocate.Area.*;

@Scope("immortal")
public class TestScopeClash {

	static final BigFoo bfoo = new BigFoo();
	
	public void main() {
		BigFoo bf= TestScopeClash.getBigFoo();
		Foo foo = new Foo();
		
		foo = bf;							// ERROR
	}
	
	@Allocate(THIS)
	public static BigFoo getBigFoo() {
		return bfoo;
	}
	
}


class Foo {
}


class BigFoo extends Foo {
}	