package javax.safetycritical;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class TestInfrastructure {


    @SCJAllowed
    public static class A extends B {

        public A() {
        }

        public A(Foo f) {
            super(f);
        }
    }

    @SCJAllowed
    public static abstract class B {

        @SCJAllowed(INFRASTRUCTURE)
        protected B() {}

        @SCJAllowed(INFRASTRUCTURE)
        protected B(Foo f) {}
    }

    class Foo {}
}
