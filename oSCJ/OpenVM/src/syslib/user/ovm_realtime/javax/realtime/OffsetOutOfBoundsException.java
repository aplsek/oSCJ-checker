package javax.realtime;

/**
 * Thrown if the constructor of an {@link ImmortalPhysicalMemory},
 * {@link LTPhysicalMemory}, {@link VTPhysicalMemory}, {@link RawMemoryAccess},
 * or {@link RawMemoryFloatAccess} is given an invalid address.
 *
 * @since 1.0.1 Becomes unchecked
 *
 * @spec RTSJ 1.0.1
 */
public class OffsetOutOfBoundsException extends RuntimeException {

    /**
     * A constructor for <tt>OffsetOutOfBoundsException</tt>
     */
    public OffsetOutOfBoundsException() { }

    /**
     * A descriptive constructor for <tt>OffsetOutOfBoundsException</tt>
     *
     * @param description A description of the exception.
     */
    public OffsetOutOfBoundsException(String description) {
	super(description);
    }
}
