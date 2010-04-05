package javax.realtime;

/**
 * Thrown when the underlying hardware does not support the type of 
 * physical memory requested from an instance of
 * one of the physical memory or raw memory access classes.
 *
 * @see RawMemoryAccess
 * @see RawMemoryFloatAccess
 * @see ImmortalPhysicalMemory
 * @see LTPhysicalMemory
 * @see VTPhysicalMemory
 * 
 * 
 * @since 1.0.1 Becomes unchecked
 *
 * @spec RTSJ 1.0.1
 */
public class UnsupportedPhysicalMemoryException extends RuntimeException {

    /**
     * A constructor for <tt>UnsupportedPhysicalMemoryException</tt>.
     */
    public UnsupportedPhysicalMemoryException() { }

    /**
     * A descriptive constructor for 
     * <tt>UnsupportedPhysicalMemoryException</tt>.
     *
     * @param description The description of the exception.
     */
    public UnsupportedPhysicalMemoryException(String description) {
    	super(description);
    }
}
