package ovm.core.domain;
import ovm.util.OVMRuntimeException;

/**
 * A <tt>WildcardException</tt> is used to handle arbitrary exceptions, both 
 * user-domain exceptions and executive domain exceptions, in the executive 
 * domain. Due to the separate type-systems in domains
 * a user-domain exception object is not an executive domain <tt>Throwable</tt>
 * and so will never be caught by explicit handlers, or the implicit handlers
 * associated with synchronized blocks and finally clauses. The wildcard
 * exception allows any exception to be caught by code that is unaware of
 * of the domain context in which it is executing - typically reflection
 * code within the executive domain.
 * <p>The way the wildcard works is quite simple: a catch clause that lists
 * its catch type as <tt>WildcardException</tt> will catch any exception
 * that is thrown by the try block. The OVM treats this type of handler as a
 * special case and wraps the exception object in a <tt>WildcardException</tt>
 * object that is then caught by the handler. The original exception is stored
 * within the wildcard as an oop. Because all exceptions will be caught by
 * such a catch clause you must be careful to list other catch clauses ahead
 * of a wildcard catch. Also note that the caught exception could be a checked
 * exception.
 * <p>The wildcard exception can also be used to throw a user-domain
 * exception in the executive domain, simply by wrapping it. This allows
 * a user-domain exception to be thrown but still have all executive domain
 * finally clauses and &quot;catch-all&quot; handlers processed. It is up
 * to the code that catches the wildcard to determine how it should process
 * the wrapped exception. Note that the catch clause cannot assume that the
 * wrapped exception is a user-domain exception. Also note that an explicitly
 * thrown wildcard exception will not itself be wrapped in another
 * wildcard exception.
 *
 * @see ovm.core.execution.ReturnMessage
 */
public class WildcardException extends OVMRuntimeException {

    Oop origThrowable; // could be ED or UD and could be scoped

    /**
     * Create a <tt>WildcardException</tt> that wraps the given
     * executive-domain or user-domain throwable object.
     * @param origThrowable the throwable object to wrap
     */
    public WildcardException(Oop origThrowable) 
        throws ovm.core.services.memory.PragmaNoBarriers {
	this.origThrowable = origThrowable;
    }

    /**
     * Create a <tt>WildcardException</tt> that is not bound to any
     * throwable.
     * @see #setOriginalThrowable
     */
    public WildcardException() {
    }

    /**
     * Set this <tt>WildcardException</tt> to wrap the given
     * executive-domain or user-domain throwable object.
     *
     * @param origThrowable the throwable object to wrap
     */
    public void setOriginalThrowable(Oop origThrowable) 
        throws ovm.core.services.memory.PragmaNoBarriers {
	this.origThrowable = origThrowable;
    }

    /**
     * Return the original executive-domain or user-domain throwable object 
     * that this <tt>WildcardException</tt> wraps.
     */
    public Oop getOriginalThrowable() {
	return origThrowable;
    }
    
    /**
     * Return <tt>true</tt> if the wrapped object is an executive domain
     * <tt>Throwable</tt> object, and <tt>false</tt> otherwise.
     */
    public boolean isExecutiveThrowable() {
  	return origThrowable.getBlueprint().getDomain() == DomainDirectory.getExecutiveDomain();
    }

    /**
     * Return the wrapped executive domain throwable, if it is one, else
     * return null.
     */
    public Throwable getExecutiveThrowable() {
	if (isExecutiveThrowable()) {
	    return (Throwable)origThrowable;
	} else {
	    return null;
	}
    }

    /**
     * Return the wrapped user domain throwable, if it is one, else
     * return null.
     */
    public Oop getUserThrowable() {
	if (isExecutiveThrowable()) {
	    return null;
	} else {
	    return origThrowable;
	}
    }

    /**
     * Returns <tt>false</tt> to  disable stack trace creation at construction 
     * time as it is never needed.
     */
    protected boolean shouldFillInStackTrace() { return false; }
}

