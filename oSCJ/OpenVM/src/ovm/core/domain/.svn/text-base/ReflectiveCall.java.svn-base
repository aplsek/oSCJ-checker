package ovm.core.domain;
import ovm.core.Executive;

import ovm.core.repository.Selector;

abstract class ReflectiveCall {
    private Type.Context ctx;
    private Selector.Method name;
    protected Method method;

    ReflectiveCall(Method method) { this.method = method; }

    protected ReflectiveCall(Type.Context ctx, Selector.Method name) {
	this.ctx = ctx;
	this.name = name;
	ctx.getDomain().registerCall(name);
    }
    protected ReflectiveCall(Domain dom, Selector.Method name) {
	this(dom.getSystemTypeContext(), name);
    }

    /**
     * Return the Method that this wrapper calls.
     **/
    public Method getMethod()  {
	if (method == null) {
	    try {
		Type.Scalar t = ctx.typeFor(name.getDefiningClass()).asScalar();
		method = t.getMethod(name.getUnboundSelector());
		if (method == null)
		    throw new LinkageException("method " + name +
					       " not found");
	    } catch (LinkageException e) {
		Executive.panicOnException(e);
	    }
	}
	return method;
    }
}
	
