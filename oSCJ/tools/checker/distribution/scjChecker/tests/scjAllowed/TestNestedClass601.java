package scjAllowed;

import javax.safetycritical.SCJFakeNestedClasses;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * @author plsek
 */
@SCJAllowed(value = Level.LEVEL_1)
public class TestNestedClass601 {

    public void nestedFoo() {

        SCJFakeNestedClasses.foo();

        SCJFakeNestedClasses.Nested.nested1();
    }

}
