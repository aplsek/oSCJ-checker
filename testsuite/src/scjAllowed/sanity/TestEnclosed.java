package scjAllowed.sanity;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class TestEnclosed {
    static abstract class X {
        X a;
        public void methodY() { }
    }
}
