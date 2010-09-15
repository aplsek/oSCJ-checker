
package javax.safetycritical;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;


@SCJAllowed(members=true,value=Level.LEVEL_1)
public class FakeSCJMembers {
    
    public static int member;

    public static void level1Call() {
        scjProtected();
    }

    @SCJAllowed(INFRASTRUCTURE)
    public static void scjProtected() {
    }
}
