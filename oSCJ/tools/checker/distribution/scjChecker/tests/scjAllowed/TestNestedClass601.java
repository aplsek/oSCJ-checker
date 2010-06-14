///Users/plsek/_work/workspace_RT/scj-annotations/tests/scjAllowed/TestNestedClass601.java:12: Hidden code can not invoke an SCJAllowed code.
//        SCJFakeNestedClasses.foo();
//                                ^
///Users/plsek/_work/workspace_RT/scj-annotations/tests/scjAllowed/TestNestedClass601.java:14: Hidden code can not invoke an SCJAllowed code.
//        SCJFakeNestedClasses.Nested.nested1(); 
//                                           ^
//2 errors

package scjAllowed;

import javax.safetycritical.SCJFakeNestedClasses;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value = Level.LEVEL_1)
public class TestNestedClass601 {

    public void nestedFoo() {

        SCJFakeNestedClasses.foo();

        SCJFakeNestedClasses.Nested.nested1(); 
    }

}
