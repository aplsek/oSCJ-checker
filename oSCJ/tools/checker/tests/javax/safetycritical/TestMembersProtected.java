package javax.safetycritical;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;

@SCJAllowed(members=true,value=Level.LEVEL_1)
public class TestMembersProtected {
    public static int member;

    public static void level1Call() {
        scjProtected();
    }

    @SCJProtected
    public static void scjProtected() {
    }
}


