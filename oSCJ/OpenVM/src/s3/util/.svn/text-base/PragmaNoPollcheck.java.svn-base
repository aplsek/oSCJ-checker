// $Header: /p/sss/cvs/OpenVM/src/s3/util/PragmaNoPollcheck.java,v 1.2 2004/03/03 21:36:31 baker29 Exp $

package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * No pollchecks will be squirted inside methods that are declared to throw
 * this pragma.  Such methods will be atomic provided that they do not make
 * calls to methods that are themselves not atomic.
 * @author Filip Pizlo
 */
public class PragmaNoPollcheck extends PragmaException {
    
    private static final TypeName.Scalar me=
        register( "s3.util.PragmaNoPollcheck",
                  "cookie");
    
    public static Object descendantDeclaredBy( Selector.Method ms,
					       Blueprint bp) {
	return descendantDeclaredBy( me, ms, bp);
    }

    public static boolean declaredBy(Selector.Method ms, Blueprint bp) {
      return declaredBy(me, ms, bp);
    }
}

