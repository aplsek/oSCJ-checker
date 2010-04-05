
package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * Methods declared to throw this exceptions will not 
 * encapsulate recursive calls by exception checks
 */
public class PragmaIgnoreSafePoints extends PragmaException {
    
    private static final TypeName.Scalar me=
        register( "s3.util.PragmaIgnoreSafePoints",
                  "cookie");
    
    public static Object descendantDeclaredBy( Selector.Method ms,
					       Blueprint bp) {
	return descendantDeclaredBy( me, ms, bp);
    }
  
}

