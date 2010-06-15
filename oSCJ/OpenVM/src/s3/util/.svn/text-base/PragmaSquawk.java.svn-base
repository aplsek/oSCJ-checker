/* MUST MATCH STRING BELOW! */ package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * Example of a simple pragma.  IRewriter will squawk when rewriting any
 * method that throws this.  If an instance method, the rewriter will also
 * squawk when rewriting <code>INVOKEVIRTUAL</code> sites that call it.
 * @author Chapman Flack
 **/
public class PragmaSquawk /* MUST MATCH STRING! */ extends PragmaException {

  private static final TypeName.Scalar me =
    getTypeName( "s3.util.PragmaSquawk"); // UPDATE IF CLASS/PACKAGE CHANGED!

  public static boolean declaredBy( Selector.Method ms, Blueprint bp) {
    return declaredBy( me, ms, bp);
  }
}
