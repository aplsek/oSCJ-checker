package javax.realtime;

/**
 * Thrown if an attempt is made to exceed a system resource limit,
 * such as the maximum number of locks.
 *
 * @spec RTSJ 1.0.1
 */
public class ResourceLimitError extends Error {

    /**
     * A  constructor for <tt>ResourceLimitError</tt>.
     */
    public ResourceLimitError() { }
    
    /**
     * A descriptive constructor for <tt>ResourceLimitError</tt>.
     * @param description The description of the error.
     */
    public ResourceLimitError(String description) {
	super(description);
    }
    
}
