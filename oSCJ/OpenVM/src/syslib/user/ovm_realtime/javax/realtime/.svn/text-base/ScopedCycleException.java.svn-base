package javax.realtime;

/**
 * Thrown when a scheudlable object attempts to enter an instance of
 * {@link ScopedMemory} where that operation would cause a violation of the
 * single parent rule.
 *
 * @spec RTSJ 1.0.1
 */
public class ScopedCycleException extends RuntimeException {

    /**
     * A constructor for <tt>ScopedCycleException</tt>
     */
    public ScopedCycleException() {}

    /**
     * A descriptive constructor for <tt>ScopedCycleException</tt>
     * @param description Description of the error
     */
    public ScopedCycleException(String description) {
        super(description);
    }

}
