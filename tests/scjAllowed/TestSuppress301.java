//scjAllowed/TestSuppress301.java:31: Constructor call is not allowed at level 0.
//        connection_types_ = new ArrayList<String>();
//                            ^
//1 error

package scjAllowed;

import java.util.ArrayList;
import java.util.List;
import javax.safetycritical.SCJFakeNestedClasses;
import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SuppressSCJ;

@SCJAllowed(members = true, value = Level.LEVEL_0)
public class TestSuppress301 {

    /**
     * This is the ERROR:
     */
    @SuppressSCJ
    private static final List<String> connection_types_;

    static {
        connection_types_ = new ArrayList<String>();

    }

    /**
     * Correct examples:
     */
    @SuppressSCJ
    private static final List<String> list = new ArrayList<String>();

    private static final List<String> connection_types_2;
    static {
        connection_types_2 = new /*@SuppressSCJ*/ArrayList<String>();

    }
}
