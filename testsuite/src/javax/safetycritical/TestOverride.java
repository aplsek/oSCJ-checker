package javax.safetycritical;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import static javax.safetycritical.annotate.Level.SUPPORT;

@SCJAllowed
public class TestOverride {


    @SCJAllowed
    public static class A extends B {

        @SCJAllowed(INFRASTRUCTURE)
        private void method() {

        }
    }

    @SCJAllowed
    public static abstract class B {

        @SCJAllowed(SUPPORT)
        private void method() {

        }
    }

    class Foo {}
}
