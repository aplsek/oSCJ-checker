package ovm.core.domain;
import ovm.core.repository.Selector;
import ovm.core.OVMBase;

/**
 * Wrap a reflective virtual or interface method invocation.  This
 * class is a factory for {@link ReflectiveMethod} objects.  Given a
 * receiver, it will return the <code>ReflectiveMethod</code> for
 * calls on that receiver.<p>
 *
 * The consistent use of reflective wrapper objects permits
 * whole-program analysis.  Reflective wrappers must be created at
 * image build time (before our static analysis completes).  When a
 * <code>ReflectiveVirtualFunction</code> is created, a static
 * analysis can account for calls in VM-internal code.
 * 
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 **/
public class ReflectiveVirtualFunction {
    private Selector.Method name;

    /**
     * Create an object capable of dispatching calls to name within dom
     * @param dom  the domain being called into
     * @param name the name of the virtual function's base method
     **/
    public ReflectiveVirtualFunction(Domain dom, Selector.Method name) {
	dom.registerVirtualCall(name);
	this.name = name;
    }

    /**
     * Return the implemenation of this virtual function for the
     * given receiver object.
     **/
    public Method findMethod(Oop receiver) {
	/* FIXME: package-private base methods, and the whole lookup
	 * thing.  We should resolve the base method, ask it for
	 * it's vtable offset, and look that up in the receiver type's
	 * vtable.
	 */
	Type t = receiver.getBlueprint().getType();
	Method ret = null;
	while ((ret = t.getMethod(name.getUnboundSelector())) == null)
	    t = t.getSuperclass();
	return ret;
    }

    /**
     * Return a wrapper that can be used to invoke this virtual
     * function for a particular receiver
     **/
    public ReflectiveMethod dispatch(Oop receiver) {
	return dispatch(findMethod(receiver));
    }

    /**
     * Return a wrapper that can be used to invoke this function given
     * the result of a call to <code>findMethod</code>.
     **/
    public ReflectiveMethod dispatch(Method m) {
	// If the asserted condition is false, ovm was built using
	// unsound static analysis, and will behave ins strange ways.
	// This method MUST be called with a method returned by
	// findMethod or a similar lookup for this VF.
	assert (m.getSelector().getUnboundSelector()
		== name.getUnboundSelector()
		/* && base class is supertype */);
	return new ReflectiveMethod(m);
    }
}
