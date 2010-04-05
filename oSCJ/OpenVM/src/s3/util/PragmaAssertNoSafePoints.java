
package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * Methods declared to throw this exceptions will be assumed not to throw
 * any exception (including unchecked exceptions).
 */
public class PragmaAssertNoSafePoints extends PragmaException {
    
    private static final TypeName.Scalar me=
        register( "s3.util.PragmaAssertNoSafePoints",
                  "cookie");
    
    public static Object descendantDeclaredBy( Selector.Method ms,
					       Blueprint bp) {
	return descendantDeclaredBy( me, ms, bp);
    }
  
}

