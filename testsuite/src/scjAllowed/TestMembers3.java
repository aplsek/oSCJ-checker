
package scjAllowed;

import javax.safetycritical.FakeSCJMembers;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;

@SCJAllowed(LEVEL_1)
public class TestMembers3 {

   
    @SCJAllowed(members=true,value=LEVEL_1)      // TODO: this should be an ERROR 
    public void foo() {
    }
    
    
}
