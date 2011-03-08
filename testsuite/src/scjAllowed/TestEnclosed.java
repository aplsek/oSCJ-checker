package scjAllowed;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.SCJAllowed;


@SCJAllowed(members = true)
public class TestEnclosed {

    static abstract class A extends Mission {
        A a;

        public void methodY() {
        }
    }
}