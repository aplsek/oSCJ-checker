package ovm.core.execution;
import ovm.core.OVMBase;
import ovm.core.domain.Oop;
import ovm.core.domain.WildcardException;
import ovm.core.repository.TypeCodes;
/**
 * A holder for the results of an reflective invocation via an
 * {@link InvocationMessage}, or an {@link InstantiationMessage}.
 *
 * <h3>Allocation Notes</h3>
 * <p>This class performs allocation only during construction, for non-void
 * return types.
 * <p>The current allocation context is always used.
 *
 * @author Krzysztof Palacz, updated: David Holmes
 */
public class ReturnMessage extends OVMBase {
    ValueUnion returnValue;
    WildcardException wildcard;

    /**
     * Create a <tt>ReturnMessage<tt> for the given <tt>InvocationMessage</tt>.
     * @param tag the type tag for the return type represented by this. If
     * <tt>tag</tt> equals {@link TypeCodes#VOID} then this return message
     * can be used to obtain exception information only.
     */
    public ReturnMessage(char tag) {
        if (tag != TypeCodes.VOID) {
            returnValue = new ValueUnion(tag);
        }
        
    }

    /** Return the type tag for this return message
     */
    public char getTypeTag() {
        return returnValue == null ? TypeCodes.VOID : returnValue.getTypeTag();
    }

    /**
     * Return the result of the invocation. If the invocation results in
     * an exception then the contents of the return value is undefined and
     * may be null, so always check for an exception before extracting.
     * @return the result of the invocation, or null if the associated 
     * invocation is for a void method.
     */
    public ValueUnion getReturnValue() {
	return returnValue;
    }

    /**
     * Set the type of return value object to return.
     * Possibly changing the type mid-execution. This 
     * is used so that instantiation messages return
     * the instantiated object, not void.
     * Not necessarily a great idea.
     */
    void setReturnValue(ValueUnion value) {
	returnValue = value;
    }


    /**
     * Return the exception, if any, as an oop
     */
    public Oop getException() {
        return wildcard == null ? null : wildcard.getOriginalThrowable();
    }

    /**
     * Return the wrapped executive domain throwable, if it is one, else
     * return null.
     */
    public Throwable getExecutiveThrowable() {
        return wildcard == null ? null : wildcard.getExecutiveThrowable();
    }

    /**
     * Return the exception, if any, if it is a user domain throwable,
     * else return null.
     */
    public Oop getUserThrowable() {
        return wildcard == null ? null : wildcard.getUserThrowable();
    }

    /**
     * Set the exception as a wildcard so it can be rethrown if necessary.
     */
    void setException(WildcardException wildcard_) {
        assert wildcard_ != null:  "null wildcard set in ReturnMessage";
        this.wildcard = wildcard_;
    }

    /**
     * Rethrow the exception, if any, wrapped as a WildcardException;
     * otherwise do nothing.
     */
    public void rethrowWildcard() {
        if (wildcard != null) throw wildcard;
    }                
}



