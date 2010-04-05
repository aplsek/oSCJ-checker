package ovm.core.domain;

import ovm.core.Executive;
import ovm.core.execution.InstantiationMessage;
import ovm.core.execution.ReturnMessage;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Area;


/**
 * Wrap a constructor that is invoked reflectively.  This class caches 
 * reflection information, and provides convienence methods to
 * reflectively invoke methods of up to two referencee arguments.<p>
 *
 * The consistent use of reflective wrapper objects also permits
 * whole-program analysis.  Reflective wrappers must be created at
 * image build time (before our static analysis completes).  When a
 * <code>ReflectiveConstructor</code> is created, a static analysis can
 * account for the calls to this method in VM-internal code.
 * 
 * @author <a href="mailto://baker29@cs.purdue.edu"> Jason Baker </a>
 **/
public class ReflectiveConstructor extends ReflectiveCall {
    /**
     * Define a constructor in a type context.
     * @param ctx  the type's context
     * @param name the constructor's name, type, and defining class
     **/
    public ReflectiveConstructor(Type.Context ctx, Selector.Method name) {
	super(ctx, name);
    }

    /**
     * Define a constructor in the system type context of a domain.
     * @param dom  the type's domain
     * @param name the constructors name, type, and defining class
     * This constructor is equivalent to:
     * <pre>ReflectiveConstructor(dom.getSystemTypeContext(), name)</pre>
     **/
    public ReflectiveConstructor(Domain dom, Selector.Method name) {
	super(dom, name);
    }
    /**
     * Define a constructor in the system type context of a domain.
     * @param dom  the type's domain
     * @param t    the type's name
     * @param args the constructors argument types.
     * This constructor is equivalent to:
     * <pre>
       ReflectiveConstructor(dom.getSystemTypeContext(),
       			     RepositoryUtils.selectorFor(t, JavaNames.VOID,
			     				 "<init>", args))
       </pre>
     */
    public ReflectiveConstructor(Domain dom, TypeName.Scalar t,
				 TypeName[] args) {
	super(dom,
	      RepositoryUtils.selectorFor(t, TypeName.VOID, "<init>", args));
    }
    /**
     * Return a fresh InstantiationMessage for this constructor.
     **/
    public InstantiationMessage makeMessage() {
	return new InstantiationMessage(getMethod());
    }

    /**
     * Call a constructor with no arguments.  The InstantiationMessage
     * is allocated in the scratchpad, and the object itself is
     * allocated in the current memory area.  Any exception the
     * constructor generates will be rethrown as a WildcardException,
     * and any LinkageExceptions resolving the constructor will result
     * in a panic.
     * @throws WildcardException
     **/
    public Oop make() {
	VM_Area outer = MemoryManager.the().getCurrentArea();
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
	try {
	    InstantiationMessage imsg = makeMessage();
	    VM_Area r2 = MemoryManager.the().setCurrentArea(outer);
	    try {
		ReturnMessage rmsg = imsg.instantiate();
		rmsg.rethrowWildcard();
		return rmsg.getReturnValue().getOop();
	    } finally {
		MemoryManager.the().setCurrentArea(r2);
	    }
	} catch (LinkageException e) {
	    throw Executive.panicOnException(e);
	} finally {
	    MemoryPolicy.the().leave(r1);
	}
    }

    /**
     * Call a constructor with one reference parameter.  The
     * InstantiationMessage is allocated in the scratchpad, and the
     * object itself is allocated in the current memory area.
     * Any exception the constructor generates will be rethrown as a
     * WildcardException, and any LinkageExceptions resolving the
     * constructor will result in a panic.
     * @throws WildcardException
     **/
    public Oop make(Oop arg1) {
	VM_Area outer = MemoryManager.the().getCurrentArea();
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
	try {
	    InstantiationMessage imsg = makeMessage();
	    imsg.getInArgAt(0).setOop(arg1);
	    VM_Area r2 = MemoryManager.the().setCurrentArea(outer);
	    try {
		ReturnMessage rmsg = imsg.instantiate();
		rmsg.rethrowWildcard();
		return rmsg.getReturnValue().getOop();
	    } finally {
		MemoryManager.the().setCurrentArea(r2);
	    }
	} catch (LinkageException e) {
	    throw Executive.panicOnException(e);
	} finally {
	    MemoryPolicy.the().leave(r1);
	}
    }

    /**
     * Call a constructor with two reference parameters.  The
     * InstantiationMessage is allocated in the scratchpad, and the
     * object itself is allocated in the current memory area.
     * Any exception the constructor generates will be rethrown as a
     * WildcardException, and any LinkageExceptions resolving the
     * constructor will result in a panic.
     * @throws WildcardException
     **/
    public Oop make(Oop arg1, Oop arg2) {
	VM_Area outer = MemoryManager.the().getCurrentArea();
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
	try {
	    InstantiationMessage imsg = makeMessage();
	    imsg.getInArgAt(0).setOop(arg1);
	    imsg.getInArgAt(1).setOop(arg2);
	    VM_Area r2 = MemoryManager.the().setCurrentArea(outer);
	    try {
		ReturnMessage rmsg = imsg.instantiate();
		rmsg.rethrowWildcard();
		return rmsg.getReturnValue().getOop();
	    } finally {
		MemoryManager.the().setCurrentArea(r2);
	    }
	} catch (LinkageException e) {
	    throw Executive.panicOnException(e);
	} finally {
	    MemoryPolicy.the().leave(r1);
	}
    }
}
