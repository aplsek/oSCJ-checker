/* MUST MATCH STRING BELOW! */ package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * Example of a pragma hierarchy.  Instead of simply initializing with
 * {@link PragmaException#getTypeName(String) getTypeName} and saving its
 * name, each member of a pragma hierarchy calls
 * {@link PragmaException#register(String,Object) register} to both save
 * its name and provide a cookie to represent it. With one call to
 * {@link #descendantDeclaredBy(Selector.Method,Blueprint) descendantDeclaredBy}
 * the rewriter can determine if a method declares <em>any</em> of a class of
 * pragmas, and the returned cookie indicates which one.
 * <p>
 * Only the root of a hierarchy (the one whose
 * {@link #descendantDeclaredBy(Selector.Method,Blueprint) descendantDeclaredBy}
 * method will be called) needs to provide the method and save its typename in
 * a variable; its children must only be registered, which they can do
 * themselves in static initializers, or the root can put many
 * {@link PragmaException#register(String,Object) register} calls together in
 * its static initializer, or the styles may be combined.
 * @author Chapman Flack
 **/
public class PragmaHierarchy /* MUST MATCH STRING! */ extends PragmaException {

  private static final TypeName.Scalar me =
    register( "s3.util.PragmaHierarchy", // UPDATE IF CLASS/PACKAGE CHANGED!
              "the root");

  public static Object descendantDeclaredBy( Selector.Method ms,
					     Blueprint bp) {
    return descendantDeclaredBy( me, ms, bp);
  }
  
  public static class Foo extends PragmaHierarchy
  { static { register( "s3.util.PragmaHierarchy$Foo", "Foo"); } }
  
  public static class Bar extends PragmaHierarchy
  { static { register( "s3.util.PragmaHierarchy$Bar", "Bar"); } }
}
