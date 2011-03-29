package scjAllowed.simple;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class TestCloneMethod {

    static byte padding[] = {
        (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    public void run() {

        method(padding);                // ERROR: padding is IMMORTAL!!

        method(padding.clone());            // ERROR: the clone method is not @SCJAllowed!!
    }

    private void method (byte [] arg) {
    }
}
