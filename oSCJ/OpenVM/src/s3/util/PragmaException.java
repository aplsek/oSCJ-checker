package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.repository.Bytecode;
import ovm.core.repository.RepositoryUtils;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import ovm.util.HashMap;
import ovm.util.Map;
import ovm.util.OVMRuntimeException;
import s3.core.domain.S3JavaUserDomain;

/**
 * Base class for dummy unchecked exceptions that will be used in method
 * throws clauses to associate method-granular properties/pragmas with methods.
 *
 * @author Chapman Flack
 **/
public abstract class PragmaException extends java.lang.RuntimeException {
  /**
   * Is the class represented by <code>eclass</code> in the throws clause of
   * <code>method</code>?
   * <p>
   * This method factors out the work common to
   * {@link #declaredBy(Selector.Method,Blueprint)}, which must be implemented
   * in each
   * extending class. Each class <code>foo</code> extending this class must
   * provide:
   * <pre>
   * private static final
   *   TypeName.Scalar me = getTypeName( "fully.qualified.foo");
   * public static boolean declaredBy( Selector.Method ms, Blueprint bp) {
   *   return declaredBy( me, ms, bp);
   * }
   * </pre>
   * @param _exc A TypeName.Scalar representing any subclass of this class.
   * (TypeName rather than Type is compared because for a subclass of
   * PragmaException to get its own Type in a static initializer at build time
   * is a problem; the image-build driver's Type.Context is needed. Discussed
   * adding a magic to get the Type of the defining class for the executing
   * bytecode, but that wouldn't work at build time, anyway.)
   * In the current implementation nothing bad should happen if <code>exc</code>
   * represents some other {@link java.lang.Throwable Throwable} (you'll find
   * out if the method can throw it), or even a non-Throwable object (you'll
   * find out that the method can't throw it) but this method isn't intended
   * for those purposes so that behavior is not defined.
   * @param sel Selector representing any method.
   * @param bp  Blueprint of the class defining method <code>sel</code>.
   * Ensuring that it really is the defining class's blueprint is the caller's
   * responsibility.
   * @return true iff <code>eclass</code> represents a Throwable declared in the
   * throws clause of the method represented by <code>sel</code>.
   **/
  protected final static boolean declaredBy(TypeName.Scalar _exc,
                                            Selector.Method sel,
                                            Blueprint bp) {
    TypeName.Scalar[] thrownTypeNames = null;
    Type unrefined = getType( sel, bp);
    Type exc = unrefined.getDomain().getPragma(_exc);

    if (exc == null)
	return false;
    Type.Reference[] thrownTypes = getThrownTypes( sel, unrefined);
    if ( thrownTypes != null ) {
        for ( int i = 0 ; i < thrownTypes.length ; ++ i ) {
	    if (thrownTypes[i] == exc)
		return true;
        }
    }
    return false;
  }
  /**
   * Get the {@link ovm.core.repository.TypeName.Scalar} object corresponding to a class
   * name. Every extending class should have
   * a <CODE>private static final TypeName.Scalar</CODE> field that it
   * initializes by passing its class name to this method. The contents of
   * that field can then be passed by that class's
   * {@link #declaredBy(Selector.Method,Blueprint) declaredBy} method to the
   * general
   * {@link #declaredBy(TypeName.Scalar,Selector.Method,Blueprint) declaredBy}
   * method above.
   * <P>
   * In using a <CODE>static final</CODE> field, there is
   * an <STRONG>assumption</CODE> that a {@link ovm.core.repository.TypeName.Scalar}
   * object found at image-writing time will be at run time a valid
   * {@link ovm.core.repository.TypeName.Scalar} object representing the same name.
   *
   * @param className String naming a class, in exactly the form that would be
   * returned by {@link java.lang.Class#getName() Class.getName}. This requires
   * you to duplicate your class name in a string literal and invites typos, so
   * be careful. It would be better to accept a {@link java.lang.Class Class}
   * as a parameter, which is how this method works in JikesRVM, but in OVM
   * it can't be done. <strong>So, beware changes to class or package names and
   * be sure to update the string literals.</strong>
   * @return The corresponding {@link ovm.core.repository.TypeName.Scalar} object
   **/
  protected final static TypeName.Scalar getTypeName( String className) {
   
 // similar logic in Driver.makeTypeNameFromClass depends on JDK2OVM, which
    // contains a dependency on java.lang.reflect.Field that makes phase 1 blow
    // up. Doing the hard way:

    String packageName;
    int split = className.lastIndexOf( '.');
    if ( split == -1 )
      packageName = "";
    else {
      packageName =
        className.substring( 0, split).replace( '.',
                                                '/');
      //FIXME should be:      S3TypeName.PACKAGE_SEPARATOR);
      // removed while trying to extricate ovm.* from s3.*
      className = className.substring( split + 1);
    }
    return RepositoryUtils.makeTypeName( packageName, className);
  }

  /**
   * Is this class declared in the throws clause of <code>method</code>?
   * <p>
   * Every extending class must provide this, simply as a stub that calls
   * {@link #declaredBy(TypeName.Scalar,Selector.Method,Blueprint)} passing its
   * own TypeName (which it can get with {@link #getTypeName(String)}.
   * If this method
   * is called directly, or on a subclass that has neglected to provide it,
   * {@link java.lang.UnsupportedOperationException} is thrown (all because
   * the JLS prohibited abstract static methods).
   * @param method Selector.Method object representing any method.
   * @param bp Blueprint of the method's declaring class.
   * @return true iff this class is declared in the
   * throws clause of the method represented by <code>method</code>.
   **/
  public static boolean declaredBy( Selector.Method method, Blueprint bp) {
    throw new java.lang.UnsupportedOperationException(
      "Subclass of PragmaException must implement declaredBy()");
  }

  private static Map map = new HashMap();

  /**
   * @throws UnregisteredPragmaException if the method does declare some
   * descendant of the target pragma, but that descendant has not registered
   * its behavior, or has explicitly registered <code>null</code>
   **/
  protected final static Object descendantDeclaredBy( TypeName.Scalar exc,
                                                      Selector.Method sel,
                                                      Blueprint       bp) {
      Type unrefined = getType( sel, bp);
      Type root = unrefined.getDomain().getPragma(exc);
      if (root == null)
	  return null;
      
      Type.Reference[] thrownTypes = getThrownTypes( sel, unrefined);
      if ( thrownTypes == null )
	  return null;

      for ( int i = 0; i < thrownTypes.length; ++i ) {
	  if ( thrownTypes[i] != null  &&  thrownTypes[i].isSubtypeOf( root) ) {
	      TypeName tn = thrownTypes[i].getName();
	      Object cookie = map.get( tn);
	      if ( cookie == null )
		  throw new UnregisteredPragmaException( tn);
	      return cookie;
	  }
      }
      return null;
  }

  protected final static TypeName.Scalar register( String className,
                                                   Object cookie) {
    TypeName.Scalar name = getTypeName( className);
    map.put( name, cookie);
    
    map.put( TypeName.Scalar.make(S3JavaUserDomain.PRAGMA_MAGIC_PKG,
				  name.getShortNameIndex()), cookie);
    
    return name;
  }

  public static Object descendantDeclaredBy( Selector.Method method,
                                             Blueprint bp) {
    throw new java.lang.UnsupportedOperationException(
      "Hierarchical PragmaException must implement descendantDeclaredBy()");
  }

  private static Type getType( Selector.Method sel, Blueprint bp) {
    Type t = bp.getType();
//     assert( t.getUnrefinedName() == sel.getDefiningClass(),
// 		       "selector/blueprint mismatch: " + t + " vs " + sel);
    return t;
  }

   private static Type.Reference[] getThrownTypes(Selector.Method sel, Type t) {
    Method method;
    if ( t instanceof Type.Reference ) 
      method = ((Type.Reference)t).getMethod( sel.getUnboundSelector());
    else 
	throw new ClassCastException();  
    
//  if (method == null) {                           No longer a special case: for
//	method = t.getSharedStateType().getMethod(sel); statics we'll be passed
//  }                                                      the shared state type
    if (method == null) 
	return null;
 
    
    Type.Class[] thrownTypes = new Type.Class[method.getThrownTypeCount()];
    for (int i = 0; i < thrownTypes.length; i++) {
	try {
	    thrownTypes[i] = method.getThrownType(i);
	} catch (LinkageException e) {
	    continue; // not in repo->not of interest cf TypeClosure.coddle
	}
    }  
    return thrownTypes;
  }

  /**
   * Exception thrown if, in testing whether a method declares some descendant
   * of a target pragma, it turns out the method does, but that descendant has
   * not registered any behavior.
   **/
  public static class UnregisteredPragmaException extends OVMRuntimeException {
    public UnregisteredPragmaException( TypeName tn) { super( tn.toString()); }
  }













    /*********************** NEW instance-based Pragma API **  
     *********** for Phase2 rewriting ********************* */
    /****** bugs by grothoff@cs.purdue.edu **************** */
    

    public PragmaException() {
    }

    /**
     * Should a method with this type of pragma be eliminated during
     * rewriting? If this returns true, the method must also specify a
     * rewriting rule that at all call sites eliminates the call!
     **/
    public boolean removeMethod() {
	return false;
    }

    /**
     * This method is called to give the Pragma the chance to substitute
     * a given code fragment (to which the pragma was attached) with
     * a different fragment.  The returned fragment is final, that is,
     * it is not subjected to other pragma rewriting rules.
     * If the method returns null, substituteCodeIterative will be
     * invoked next.
     **/
    public Bytecode substituteCodeFinal(Bytecode x) {
	return null; 
    }

    /**
     * This method is called to give the Pragma the chance to
     * substitute a given code fragment (to which the pragma was
     * attached) with a different fragment.  The returned fragment is
     * iterative, that is, it will still be subjected to other pragma
     * rewriting rules. If the method returns null, the original
     * fragment (x) will be subjected as usual to the pragma rewriting
     * rules.
     **/
    public Bytecode substituteCodeIterative(Bytecode x) {
	return null; 
    }

    /**
     * A call to a method flagged with this pragma was detected.
     * Rewrite the code if needed.
     *
     * @param i the call instruction
     * @param codeBuf the instruction buffer
     * @param cfe the editor to edit
     **/ 
    public void substituteCallSite(Instruction.Invocation i,
				   InstructionBuffer codeBuf,
				   CodeFragmentEditor cfe) {
    }

} // end of PragmaException
