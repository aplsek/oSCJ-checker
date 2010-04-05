package ovm.core.services.memory;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;
import s3.util.PragmaException;

/**
 * Read and Write barrier calls will not be emited inside  methods
 * that are declared to throw this pragma.
 * @author Christian Grothoff
 **/
public class PragmaNoBarriers extends PragmaException {

    private static final TypeName.Scalar me =
	register( "ovm.core.services.memory.PragmaNoBarriers",
		  "cookie");
	
    public static Object descendantDeclaredBy( Selector.Method ms,
					       Blueprint bp) {
	return descendantDeclaredBy( me, ms, bp);
    }
  
}
