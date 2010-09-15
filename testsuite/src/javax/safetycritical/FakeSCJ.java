package javax.safetycritical;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;

import javax.safetycritical.annotate.*;


@SCJAllowed(Level.LEVEL_2)
public class FakeSCJ {
	
    @SCJAllowed(Level.LEVEL_1)
    public static int variable;
    
    @SCJAllowed(Level.LEVEL_1)
	public static void level1Call() {
		scjProtected();
	}

    @SCJAllowed(INFRASTRUCTURE)
    public static void scjProtected() {
    }
    
	
	public static void fooHidden() {
	}
	
	@SCJAllowed(Level.LEVEL_1)
	class NestedClass {
	    public void foo() {
	    }
	}
	
}