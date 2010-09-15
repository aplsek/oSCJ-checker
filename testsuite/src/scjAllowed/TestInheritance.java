//scjAllowed/TestInheritance.java:16: Subclasses may not decrease visibility of their superclasses.
//class TestInheritance2 extends TestInheritance {
//^
//1 error

package scjAllowed;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(Level.LEVEL_1)
public class TestInheritance {
}

@SCJAllowed
class TestInheritance2 extends TestInheritance {
    
}
