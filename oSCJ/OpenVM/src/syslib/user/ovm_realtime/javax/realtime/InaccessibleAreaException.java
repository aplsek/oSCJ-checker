package javax.realtime;

/**
 * The specified memory area is not above the current allocation context
 * on the current thread scope stack.
 *
 * @since 1.0.1 Becomes unchecked
 *
 * @spec RTSJ 1.0.1
 */
public class InaccessibleAreaException extends RuntimeException {

    /**
     * A constructor for <tt>InaccessibleAreaException</tt>
     */
    public InaccessibleAreaException() {}

    /**
     * A descriptive constructor for <tt>InaccessibleAreaException</tt>
     * @param description Description of the exception
     */
    public InaccessibleAreaException(String description) {
        super(description);
    }

}
