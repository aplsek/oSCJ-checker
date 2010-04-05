package javax.safetycritical;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;

/**
 * 
 * tests/javax/safetycritical/FakeSCJMembers.java:28: warning: Illegal visibility increase of an enclosing element.
    public static void scjProtected() {
                       ^
 * 
 * 
 * @author plsek
 *
 */
@SCJAllowed(members=true,value=Level.LEVEL_1)
public class FakeSCJMembers {
    
    public static int member;

    public static void level1Call() {
        scjProtected();
    }

    @SCJAllowed
    @SCJProtected
    public static void scjProtected() {
    }
}
