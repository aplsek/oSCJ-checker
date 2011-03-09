package scjAllowed.sanity;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members = true)
public class TestEnclosed {
    static abstract class A {
        A a;
        public void methodY() {
        }
    }
}