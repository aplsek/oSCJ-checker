package javax.safetycritical;

import javax.safetycritical.annotate.*;

/**
 * ERRORS: - the class is not annotated
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
public class FakeSCJ {
	
    @SCJAllowed(Level.LEVEL_1)
    public static int variable;
    
    @SCJAllowed(Level.LEVEL_1)
	public static void level1Call() {
		scjProtected();
	}

    @SCJAllowed
    @SCJProtected
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