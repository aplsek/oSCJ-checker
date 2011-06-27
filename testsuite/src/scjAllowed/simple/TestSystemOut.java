package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_0;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true, value=LEVEL_0)
public class TestSystemOut {

    public void run() {
        //System.out.println(new String(""));
    }
}
