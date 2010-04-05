package javax.realtime;

/**
 * This class maintains an estimate of the amount of memory required to
 * store a set of objects.
 * <p><code>SizeEstimator</code> is a floor on the amount of memory that should
 * be allocated.  Many objects allocate other objects when they are 
 * constructed. <code>SizeEstimator</code> only estimates the memory 
 * requirement of the object itself, it does not include memory required for 
 * any objects allocated at construction time.
 * If the instance itself is allocated in several parts, the size estimate
 * shall include the sum of the sizes of all the parts that are allocated from
 * the same memory area as the instance.
 * <p>Alignment considerations, and possibly other order-dependent issues may 
 * cause the allocator to leave a small amount of unusable space, consequently 
 * the size estimate cannot be seen as more than a close estimate.
 *
 * @see MemoryArea#MemoryArea(SizeEstimator)
 * @see LTMemory#LTMemory(SizeEstimator, SizeEstimator)
 * @see VTMemory#VTMemory(SizeEstimator, SizeEstimator)
 *
 * @spec RTSJ 1.0.1(b)
 */
public final class SizeEstimator {

    /** the number of bytes we've reserved so far */
    private long size = 0; 


    /** the size of a monitor - as would be allocated in the same MA as its
        object. This has to be included in the size estimate.
    */
    static long sizeOfMonitor = LibraryImports.sizeOfMonitor();

    /**
     * Construct a <tt>SizeEstimator</tt>
     */
    public SizeEstimator() {}

    /**
     * Get an estimate of the number of bytes needed to store all of 
     * the objects reserved.
     *
     * @return The estimated size in bytes.
     */
    public long getEstimate() {
        return size;
    }


    /**
     * Take into account additional <tt>number</tt> instances of Class
     * <tt>c</tt> when estimating the size of the 
     * {@link javax.realtime.MemoryArea}.
     *
     * @param c The class to take into account.
     * @param number The number of instances of <tt>c</tt> to estimate.
     */
    public void reserve(Class c, int number) {
        // behaviour on null not specified
        if (c != null) {
            size +=  (number * (LibraryImports.sizeOf(c) + 
                                (c.isPrimitive() ? 0 : sizeOfMonitor)));
        }
    }


    /**
     * Take into account an additional instance of SizeEstimator 
     * <tt>estimator</tt> 
     * when estimating the size of the {@link MemoryArea}.
     *
     * @param estimator The given instance of <tt>SizeEstimator</tt>
     */
    public void reserve(SizeEstimator estimator)  {
        // behaviour on null not specified
        reserve(estimator, 1);
    }

    /**
     * Take into account an additional <tt>number</tt> instances of 
     * SizeEstimator <tt>estimator</tt> 
     * when estimating the size of the {@link MemoryArea}.
     *
     * @param estimator The given instance of SizeEstimator
     * @param number The number of times to reserve the size denoted by
     * <tt>estimator</tt>
     */
    public void reserve(SizeEstimator estimator, int number) {
        // behaviour on null not specified
        if (estimator != null) {
            size += (number * estimator.size);
        }
    }

    /**
     * Take into account an additional instance of an array of 
     * <code>length</code> reference values when estimating the size of 
     * the {@link MemoryArea}.
     *
     *   @param length The number of entries in the array.
     * 
     * @throws IllegalArgumentException Thrown if <code>length</code> is 
     * negative.
     * 
     *  @since 1.0.1
     */
    public void reserveArray(int length){
        if (length < 0)
            throw new IllegalArgumentException("negative length");
        else 
            size += (LibraryImports.sizeOfReferenceArray(length) + sizeOfMonitor);
    }

    /**
     * Take into account an additional instance of an array of 
     * <code>length</code> primitive values when estimating the size of the 
     * {@link MemoryArea}.
     * <p>
     * Class values for the primitive types are available from the 
     * corresponding class types; e.g. Byte.TYPE, Integer.TYPE, and Short.TYPE.
     * 
     *
     * @param length The number of entries in the array.
     * 
     * @param type The class representing a primitive type. The reservation 
     * will leave room for an array of <code>length</code> of the primitive 
     * type corresponding to <code>type</code>.
     * 
     * @throws IllegalArgumentException Thrown if <code>length</code> is 
     * negative or <code>type</code> does not represent a primitive type.
     * 
     * @since 1.0.1
     */
    public void reserveArray(int length, Class type){
        if (length < 0)
            throw new IllegalArgumentException("negative length");
        else if (type == null || !type.isPrimitive())
            throw new IllegalArgumentException("type is not a primitive type");
        else 
            size += (LibraryImports.sizeOfPrimitiveArray(length, type) +
                     sizeOfMonitor);
    }

}

