package ovm.core.execution;
import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.JavaUserDomain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.domain.WildcardException;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.util.OVMError;

/**
 * An <tt>InstantiationMessage</tt> is used to reflectively construct a
 * new instance of a type.
 * <p>Note this class is not thread-safe, but can be used multiple times
 * returning a fresh return value object each time.
 *
 * <h3>Allocation Notes</h3>
 * <p>This class performs allocation during construction.
 * <p>This class performs allocation during {@link #instantiate}.
 * <p>The current allocation context is always used.
 *
 * @author Krzysztof Palacz, updated: David Holmes
 */
public class InstantiationMessage extends OVMBase {

    public static final boolean REFLECTION_DEBUGGING = false;
    private final ValueUnion rbox = new ValueUnion(TypeCodes.OBJECT);
    private final InvocationMessage constructor;

    /**
     * Create an instantiation message for the given constructor.
     * @param method the Method object of the constructor
     */
    public InstantiationMessage(Method method) {
	assert method.isConstructor(): "not a constructor";
	constructor = new InvocationMessage(method);
    }

    /**
     * Create an instantiation message for the no-arg constructor
     * of the given type
     * @param type the type to construct
     */
    public InstantiationMessage(Type.Scalar type) throws LinkageException {
	this(type, new TypeName[0]);
    }

    /**
     * Create an instantiation message for a 1-arg constructor
     * of the given type
     * @param type the type to construct
     * @param arg the typename of the argument
     */
    public InstantiationMessage(Type.Scalar type, TypeName arg) 
	throws LinkageException {
	this(type, new TypeName[] { arg });
    }

    /**
     * Create an instantiation message for a 2-arg constructor
     * of the given type.
     * @param type the type to construct
     * @param arg1 the typename of the first argument
     * @param arg2 the typename of the second argument
     */
    public InstantiationMessage(Type.Scalar type, TypeName arg1, TypeName arg2)
	throws LinkageException {
	this(type, new TypeName[] { arg1, arg2});
    }
    
    public InstantiationMessage(Type.Scalar type, TypeName arg1, TypeName arg2,
				TypeName arg3)
	throws LinkageException {
	this(type, new TypeName[] { arg1, arg2, arg3});
    }

    public InstantiationMessage(Type.Scalar type, TypeName arg1, TypeName arg2,
				TypeName arg3, TypeName arg4)
	throws LinkageException {
	this(type, new TypeName[] { arg1, arg2, arg3, arg4});
    }
    

    /**
     * Create an instantiation message for an arbitrary constructor
     * of the given type.
     * @param type the type to construct
     * @param arguments array of typename for the arguments
     */
    public InstantiationMessage(Type.Scalar type, TypeName[] arguments) 
	throws LinkageException {
	Selector.Method sel = RepositoryUtils.selectorFor(type.getName(),					
		TypeName.VOID, "<init>", arguments);
	Method m = type.getMethod(sel.getUnboundSelector());
	assert  m != null: "Could not resolve method " + sel.toString();
	constructor = new InvocationMessage(m);
	
        if (REFLECTION_DEBUGGING) {
  	  Native.print_string("Instantiation message created for constructor, if system crashes, try_to_add_method: ");
  	  Native.print_string(sel.getDefiningClass().toString()+" "+sel.getUnboundSelector().toString()+"\n");
        }
    }

    /**
     * Return the argument holder, in the given position, that will be, or was,
     * used for this invocation. Typically the caller retrieves this and then
     * sets the actual parameter value to be used.
     * @param index the index of the argument. Arguments are indexed from zero.
     */
    public ValueUnion getInArgAt(int index) {
	return constructor.getInArgAt(index);
    }
    
    /**
     * Instantiate an instance of the type this instantiation message
     * represents. If the type has not been initialized (eg. the user-domain
     * class has not been initialized) then it is initialized.
     * Executive domain types are by definition already initialized.
     * @return the result of the instantiation as a ReturnMessage. If
     * initialization fails, or allocation fails, then we return a
     * return message holding the exception. Otherwise we return the result
     * of the constructor invocation.
     */
    public ReturnMessage instantiate() throws LinkageException {
	Type type = constructor.getMethod().getDeclaringType();
	Domain dom = type.getDomain();
	CoreServicesAccess csa = dom.getCoreServicesAccess();
	Blueprint.Scalar bpt = dom.blueprintFor(type).asScalar();
        ReturnMessage msg;
        if (type.getLifecycleState() != Type.State.INITIALIZED) {
            try {
                csa.initializeBlueprint(bpt.getSharedState());
            }
            catch(WildcardException ex) {
		msg = new ReturnMessage(TypeCodes.VOID);
                msg.setException(ex);
                return msg;
            }
        }
	Oop obj = null;
        try {
            obj = csa.allocateObject(bpt);
        }
        catch (WildcardException ex) {
            msg = new ReturnMessage(TypeCodes.VOID);
            msg.setException(ex);
            return msg;
        }
	if (dom instanceof JavaUserDomain)
	    ((JavaUserDomain) dom).registerFinalizer(obj);
	try {
	    msg = constructor.invoke(obj);
	} catch (WildcardException e) {	    // WCEs should go through
	    throw e;
	} catch (Exception e) {	// slightly better diagnostics
	    throw new OVMError("Failure for " + constructor.getMethod() + " with: " + e);
	}
	if (msg.getException() == null) 
	    msg.setReturnValue(new ValueUnion(TypeCodes.OBJECT, obj));
	return msg;
    }
}


