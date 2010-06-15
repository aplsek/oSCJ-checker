// $Header: /p/sss/cvs/OpenVM/src/s3/util/PragmaAtomic.java,v 1.2 2004/05/24 06:02:46 dholmes Exp $

package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * A method that throws PragmaAtomic is wrapped in suitable code to guarantee
 * that the method executes atomically ie that no external event source will cause
 * this thread to be preempted. For example, with user-level thread management this
 * pragma causes the thread manager's setReschedulingEnabled method to be called to
 * disable rescheduling. This pragma extends PragmaNoPollCheck as a convenience -
 * an atomic method disables the test that the poll check would look at.
 * <p>Note that an atomic method can, and may, cause a new thread to run due to the
 * synchronous action of the code executed in that method (such as locking a locked monitor
 * or doing a sleep etc).
 *
 */
public class PragmaAtomic extends PragmaNoPollcheck {
    
    private static final TypeName.Scalar me=
        register( "s3.util.PragmaAtomic",
                  "cookie");
    
    public static Object descendantDeclaredBy( Selector.Method ms,
					       Blueprint bp) {
	return descendantDeclaredBy( me, ms, bp);
    }
  
}

