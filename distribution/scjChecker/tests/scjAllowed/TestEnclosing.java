//scjAllowed/TestEnclosing.java:8: Nested elements may not increase visibility of their outer elements.
//    public static int variable;
//                      ^
//scjAllowed/TestEnclosing.java:11: Nested elements may not increase visibility of their outer elements.
//    public static void level1Call() {
//                       ^
//scjAllowed/TestEnclosing.java:15: Nested elements may not increase visibility of their outer elements.
//    class NestedClass {
//    ^
//3 errors

package scjAllowed;

import javax.safetycritical.annotate.*;

@SCJAllowed(Level.LEVEL_2)
public class TestEnclosing {
    @SCJAllowed(Level.LEVEL_1)
    public static int variable;
    
    @SCJAllowed(Level.LEVEL_1)
    public static void level1Call() {
    }
    
    @SCJAllowed(Level.LEVEL_1)
    class NestedClass {
        public void foo() {
        }
    }
}