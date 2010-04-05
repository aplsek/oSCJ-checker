package ovm.core.domain;

import ovm.core.Executive;
import ovm.core.execution.InvocationMessage;
import ovm.core.execution.ReturnMessage;
import ovm.core.execution.ValueUnion;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.RepositoryUtils;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Area;

/**
 * Wrap a method that is inovked reflectively.  This class caches 
 * reflection information, and provides convienence methods to
 * reflectively invoke methods of up to two referencee arguments.<p>
 *
 * The consistent use of reflective wrapper objects also permits
 * whole-program analysis.  Reflective wrappers must be created at
 * image build time (before our static analysis completes).  When a
 * <code>ReflectiveMethod</code> is created, a static analysis can
 * account for the calls this method in VM-internal code.
 *
 * @author <a href="mailto://baker29@cs.purdue.edu"> Jason Baker </a>
 **/
public class ReflectiveMethod extends ReflectiveCall {
    private Oop sharedStateOop;

    ReflectiveMethod(Method m) { super(m); }

    /**
     * Wrap a method in a particular type context.
     * @param ctx the context in which the declaring class is defined
     * @param sel the name, type, and declaring class of the method.
     **/
    public ReflectiveMethod(Type.Context ctx, Selector.Method sel) {
	super(ctx, sel);
    }
    /**
     * Wrap a method in the system Type.Context for a domain.
     * @param dom the method's domain
     * @param sel the method's name,type, and declaring class
     * This constructor is equivalent to:
     * <pre>ReflectiveMethod(dom.getSystemTypeContext(), sel)</pre>
     **/
    public ReflectiveMethod(Domain dom, Selector.Method sel) {
	super(dom, sel);
    }
    /**
     * Wrap a method in the system Type.Context for a domain.
     * @param dom  the domain in which the method lives
     * @param ret  the method's return type
     * @param recv the method's declaring class
     * @param name the method's name
     * @param args the method's argument types
     * This constructor is equivalent to:
     * <pre>
       ReflectiveMethod(dom.getSystemTypeContext(),
       			RepositoryUtils.selectorFor(recv, ret, name, args))
       </pre>
     *
     **/
    public ReflectiveMethod(Domain dom, TypeName ret,
			    TypeName.Scalar recv, String name,
			    TypeName[] args) {
	super(dom, RepositoryUtils.selectorFor(recv, ret, name, args));
    }

    public ReflectiveMethod(Domain dom, String tn, String ubs) {
	super(dom, RepositoryUtils.selectorFor(tn, ubs).asMethod());
    }

    /**
     * Resolve our method signature to a linked Method object.
     **/
    public Method getMethod() {
	if (method == null) {
	    Type t = super.getMethod().getDeclaringType();
	    if (t.isSharedState()) {
		Domain d = t.getDomain();
		sharedStateOop = d.blueprintFor(t)
			.getInstanceBlueprint()
			.getSharedState();
	    }
	}
	return method;
    }
    /**
     * This method is only valid for the reflective wrapper around a
     * static method.  It returns the SharedState object for the
     * method's declaring type, which can be passed to
     * InvocationMessage.invoke().
     **/
    public Oop getStaticReceiver() {
	if (sharedStateOop == null)
	    getMethod();
	return sharedStateOop;
    }
    /**
     * Create an InvocationMessage for this method.  A method's
     * signature is not compatible with any of the predefined call
     * methods in ReflectiveMethod, you must explicitly create an
     * InvocationMessage object, populate it's argument array, and
     * invoke it.
     **/
    public InvocationMessage makeMessage() {
	return new InvocationMessage(getMethod());
    }

    /**
     * Call this method with no arguments.  If the receiver object is
     * null, a default receiver will be computed with
     * getStaticReceiver.  If the call results in an exception, the
     * exception will be rethrown as a WildcardException.  If a
     * linkage error occurs resolving the method, we panic.
     *
     * @param recv The receiver object or null
     * @return the method's return value, boxed
     **/
    public ValueUnion call(Oop recv) {
	VM_Area area = MemoryManager.the().getCurrentArea();
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
	try {
	    InvocationMessage msg = makeMessage();
	    if (recv == null)
		recv = getStaticReceiver();
	    VM_Area r2 = MemoryManager.the().setCurrentArea(area);
	    try {
		ReturnMessage ret = msg.invoke(recv);
		ret.rethrowWildcard();
		return ret.getReturnValue();
	    } finally {
		MemoryManager.the().setCurrentArea(r2);
	    }
	}
	//catch (LinkageException e) { throw Executive.panicOnException(e); }
	finally { MemoryPolicy.the().leave(r1); }
    }

    /**
     * Call this method with one reference argument.  If the receiver
     * object is null, a default receiver will be computed with
     * getStaticReceiver.  If the call results in an exception, the
     * exception will be rethrown as a WildcardException.  If a
     * linkage error occurs resolving the method, we panic.
     *
     * @param recv The receiver object or null
     * @param arg1 The first argument
     * @return the method's return value, boxed
     **/
    public ValueUnion call(Oop recv, Oop arg1) {
	VM_Area area = MemoryManager.the().getCurrentArea();
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
	try {
	    InvocationMessage msg = makeMessage();
	    if (recv == null)
		recv = getStaticReceiver();
	    msg.getInArgAt(0).setOop(arg1);
	    VM_Area r2 = MemoryManager.the().setCurrentArea(area);
	    try {
		ReturnMessage ret = msg.invoke(recv);
		ret.rethrowWildcard();
		return ret.getReturnValue();
	    } finally { MemoryManager.the().setCurrentArea(r2); }
	}
	//catch (LinkageException e) { throw Executive.panicOnException(e); }
	finally { MemoryPolicy.the().leave(r1); }
    }

    /**
     * Call this method with two reference arguments.  If the receiver
     * object is null, a default receiver will be computed with
     * getStaticReceiver.  If the call results in an exception, the
     * exception will be rethrown as a WildcardException.  If a
     * linkage error occurs resolving the method, we panic.
     *
     * @param recv The receiver object or null
     * @param arg1 The first argument
     * @param arg2 The second argument
     * @return the method's return value, boxed
     **/
    public ValueUnion call(Oop recv, Oop arg1, Oop arg2) {
	VM_Area area = MemoryManager.the().getCurrentArea();
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
	try {
	    InvocationMessage msg = makeMessage();
	    if (recv == null)
		recv = getStaticReceiver();
	    msg.getInArgAt(0).setOop(arg1);
	    msg.getInArgAt(1).setOop(arg2);
	    VM_Area r2 = MemoryManager.the().setCurrentArea(area);
	    try {
		ReturnMessage ret = msg.invoke(recv);
		ret.rethrowWildcard();
		return ret.getReturnValue();
	    } finally { MemoryManager.the().setCurrentArea(r2); }
	}
	//catch (LinkageException e) { throw Executive.panicOnException(e); }
	finally { MemoryPolicy.the().leave(r1); }
    }
}
