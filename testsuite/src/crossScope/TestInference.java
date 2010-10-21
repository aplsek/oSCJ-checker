package crossScope;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.CrossScope;

public class TestInference {

}



class Foo {

	Bar x;

	@Allocate("bar")
    @CrossScope
	public Bar method(Bar bar) {
		
		return bar;
	}
	
	
	
	@Allocate({THIS})
    public Bar method2() {
		return this.x;
	}	 
}


class Bar {
	
}