package javax.safetycritical;

import static javax.safetycritical.annotate.Level.HIDDEN;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class TestHidden {

    @SCJAllowed(HIDDEN)
    public static void foo() {
        Hidden.hiddenFoo();
    }
}
