/* MUST MATCH STRING BELOW! */ package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * This pragma indicates a method that should not ever, under any
 * circumstances, be inlined.  Typically, this is used on methods that
 * obtain a reference to their own activation record, and expect
 * Activation.caller() to really return the caller.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 **/
public class PragmaNoInline extends PragmaException {

    private static final TypeName.Scalar me = // UPDATE IF CLASS/PACKAGE CHANGED!
	register( "s3.util.PragmaNoInline",
		  null); // force complaint if a throws declares THIS class directly

    public static boolean declaredBy(Selector.Method ms, Blueprint bp) {
	return declaredBy(me, ms, bp);
    }
}
