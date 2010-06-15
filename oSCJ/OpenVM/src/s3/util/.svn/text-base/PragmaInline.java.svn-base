/* MUST MATCH STRING BELOW! */ package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * This pragma indicates a method that should always be inlined.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 **/
public class PragmaInline extends PragmaException {

    private static final TypeName.Scalar me = // UPDATE IF CLASS/PACKAGE CHANGED!
	register( "s3.util.PragmaInline",
		  null); // force complaint if a throws declares THIS class directly

    public static boolean declaredBy(Selector.Method ms, Blueprint bp) {
	return declaredBy(me, ms, bp);
    }
}
