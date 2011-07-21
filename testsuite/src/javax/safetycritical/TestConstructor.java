package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_0;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class TestConstructor {

    @SCJAllowed
    public static final A a = new A(){};

    @SCJAllowed
    public static final B b = new B();

    @SCJAllowed(LEVEL_0)
    public static interface A  {

    }

    @SCJAllowed(LEVEL_0)
    public static class B  {

    }

}
