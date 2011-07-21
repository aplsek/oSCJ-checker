package javax.safetycritical;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;


@SCJAllowed
public class TestOverride {


    @SCJAllowed
    public static class A extends B {
        @SCJAllowed
        public A(Foo f) {
            //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_METHOD_CALL
            super(f);
        }

        @SCJAllowed
        public A(Foo f, Foo ff) {
            super(f,ff);
        }
    }

    @SCJAllowed
    public static abstract class B {

        protected B(Foo f) {}

        @SCJAllowed(INFRASTRUCTURE)
        protected B(Foo f, Foo ff) {}
    }

    class Foo {}
}
