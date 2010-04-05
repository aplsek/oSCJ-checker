/*
 * PragmaUnsafe.java
 * Ovm project, (c) Purdue University May 4, 2003.
 */
package ovm.util; /* MUST MATCH STRING BELOW! */

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;
import s3.util.PragmaException;

/**
 *  
 * Methods that are inhrently unsafe such as asInt() in VM_Address
 * are declared with PragmaUnsafe. The rewriter checks that only
 * classes that implement UnsafeAccess are allowed to invoke these
 * methods.
 * @author jv
 */
public class PragmaUnsafe
/* MUST MATCH STRING! */
extends PragmaException {

    private static final TypeName.Scalar me =
        getTypeName("ovm.util.PragmaUnsafe");
    // UPDATE IF CLASS/PACKAGE CHANGED!

    public static boolean declaredBy(Selector.Method ms, Blueprint bp) {
        return declaredBy(me, ms, bp);
    }
}
