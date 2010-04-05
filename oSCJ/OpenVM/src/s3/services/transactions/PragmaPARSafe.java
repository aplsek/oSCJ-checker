package s3.services.transactions;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;
import s3.util.PragmaException;

/**
 * Methods that are declared to throw this Pragma are of two types:
 *
 * 1) Native methods that have no heap writes and are therefore 
 * safe to call from within PARs.
 *
 * 2) RuntimeExports methods that are safe to call from within PARs, 
 * because they have been analyzed thoroughly.
 */
public class PragmaPARSafe extends PragmaException {
    
    private static final TypeName.Scalar me=
        register( "s3.services.transactions.PragmaPARSafe",
                  "cookie");
    
    public static Object descendantDeclaredBy( Selector.Method ms,
					       Blueprint bp) {
	return descendantDeclaredBy( me, ms, bp);
    }
  
}

