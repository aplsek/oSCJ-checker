package javax.realtime;

/**
 * Thrown by the fire method of an instance of {@link AsyncEvent}
 * when on a minimum interarrival time violation.  
 * More specifically, it is thrown when:
 * <ul>
 * <li>Any instance of {@link AsyncEventHandler} associated with the
 * <tt>AsyncEvent</tt> has {@link ReleaseParameters} from the class
 * {@link SporadicParameters};
 * <li>The MIT violation behavior for the async event handler is
 * {@link SporadicParameters#mitViolationExcept mitViolationExcept}; and
 * <li>The invocation of <tt>fire</tt> violates the minimum interarrival time
 * constraint
 * </ul>
 *
 * @since 1.0.1 Becomes unchecked
 *
 * @spec RTSJ 1.0.1
 */
public class MITViolationException extends RuntimeException {

    /**
     * A constructor for <tt>MITViolationException</tt>.
     */
    public MITViolationException() {}

    /**
     * A descriptive constructor for <tt>MITViolationException</tt>.
     *
     * @param description Description of the exception.
     */
    public MITViolationException(String description)  {
        super(description);
    }
}


