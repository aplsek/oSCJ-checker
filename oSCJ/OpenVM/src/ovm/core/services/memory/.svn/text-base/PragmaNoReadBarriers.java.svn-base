package ovm.core.services.memory;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;
import s3.util.PragmaException;

public class PragmaNoReadBarriers extends PragmaException {

    private static final TypeName.Scalar me =
	register( "ovm.core.services.memory.PragmaNoReadBarriers",
		  "cookie");
	
    public static Object descendantDeclaredBy( Selector.Method ms,
					       Blueprint bp) {
	return descendantDeclaredBy( me, ms, bp);
    }
  
}
