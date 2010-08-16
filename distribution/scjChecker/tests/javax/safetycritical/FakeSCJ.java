package javax.safetycritical;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;

import javax.safetycritical.annotate.*;

/**
 * ERRORS: 
 * 
 * tests/javax/safetycritical/FakeSCJ.java:28: warning: Illegal visibility increase of an enclosing element.
    public static int variable;
                      ^
tests/javax/safetycritical/FakeSCJ.java:31: warning: Illegal visibility increase of an enclosing element.
        public static void level1Call() {
                           ^
tests/javax/safetycritical/FakeSCJ.java:37: warning: Illegal visibility increase of an enclosing element.
    public static void scjProtected() {
                       ^
tests/javax/safetycritical/FakeSCJ.java:45: warning: Illegal visibility increase of an enclosing element.
        class NestedClass {
        ^
 * 
 * 
 * @author plsek
 *
 */
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