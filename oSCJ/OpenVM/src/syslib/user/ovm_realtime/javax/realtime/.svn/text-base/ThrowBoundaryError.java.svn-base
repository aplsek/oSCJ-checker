package javax.realtime;


/**
 * The error thrown by {@link MemoryArea#enter(Runnable)}
 * when a <tt>java.lang.Throwable</tt> allocated from memory that 
 * is not usable in the surrounding scope tries to propagate out of the
 * scope of the <tt>enter</tt>.
 *
 *
 * @spec RTSJ 1.0.1
 */
public class ThrowBoundaryError extends Error {

    /**
     * A constructor for <tt>ThrowBoundaryError</tt>. 
     */
    public ThrowBoundaryError() {}

    /**
     * A descriptive constructor for <tt>ThrowBoundaryError</tt>. 
     *
     * @param description Description of the error.
     */
    public ThrowBoundaryError(String description) {
        super(description);
    }
}
