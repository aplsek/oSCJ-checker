package javax.realtime;

/*
 * Class <code>HighResolutionTime</code> is the base class for
 * <code>AbsoluteTime</code>, <code>RelativeTime</code>,
 * <code>RationalTime</code>.
 * Used to express time with nanosecond accuracy. This class is never used
 * directly: it is abstract and has no public constructor. Instead, use one
 * of its subclasses {@link AbsoluteTime}, {@link RelativeTime}, or
 * {@link RationalTime}. When an API is defined that has an
 * <code>HighResolutionTime</code> as a parameter, it can take either an
 * absolute, relative, or rational time and will do something appropriate.
 * <P>
 * <B>Caution:</B> This class is explicitly unsafe in multithreaded
 * situations when it is being changed. No synchronization is done. It is
 * assumed that users of this class who are mutating instances will be
 * doing their own synchronization at a higher level.
 * 
 * @spec RTSJ 1.0.1
 */
public abstract class HighResolutionTime 
    implements java.lang.Comparable, Cloneable {

    /**
     * Behaves exactly like <code>target.wait()</code> but with the
     * enhancement that it waits with a precision of
     * <code>HighResolutionTime</code>.
     *
     * @param target The object on which to wait.
     * The current thread must have a lock on the object.
     *
     * @param time The time for which to wait. If it is
     * <code>RelativeTime(0,0)</code> then wait indefinitely. If it is
     * <code>null</code> then wait indefinitely.
     *
     * @throws InterruptedException Thrown if another thread
     * or <code>AIE.fire()</code>
     * interrupts this thread while it is waiting.
     *
     * @throws IllegalArgumentException Thrown if <code>time</code>
     *          represents a relative time less than zero.
     * 
     * @throws IllegalMonitorStateException Thrown if <code>target</code>
     *          is not locked by the caller.
     * 
     *
     * @see Object#wait()
     * @see Object#wait(long)
     * @see Object#wait(long,int)
     */
    public static void waitForObject(Object target, HighResolutionTime time) 
                    throws InterruptedException {
        if (time != null) {
            if (time.clock != Clock.rtc)
                throw new UnsupportedOperationException("Incompatible clock");

            if (time instanceof AbsoluteTime) {
                if (target == null)
                    throw new NullPointerException("null target");
                if (!LibraryImports.monitorAbsoluteTimedWait(target, 
                                                             time.toNanos())) {
                    // clear interrupt flag and throw IE
                    LibraryImports.threadSetInterrupted(Thread.currentThread(),
                                                        false);
                    throw new InterruptedException();
                }
            }
            else { // must be Relative so do normal Object.wait()
                if (!time.isNegative())
                    target.wait(time.getMilliseconds(), time.getNanoseconds());
                else
                    throw new IllegalArgumentException("negative relative time");
            }
        }
        else {
            target.wait(0,0);
        }
    }


    /**
     * Convert the time of <code>this</code> to an absolute time, using the
     * given instance of {@link Clock} to determine the current time when
     * necessary.
     * If <code>clock</code> is <code>null</code> the
     * real-time clock is assumed.

     * A destination object is allocated to return the result.
     * The clock association of the result is with
     * the <code>clock</code> passed as a parameter.
     * See the derived class comments for more specific information.
     *
     *   @param	clock The instance of {@link Clock} used to convert the
     * time of <code>this</code> into absolute time,
     * and the new clock association for the result.
     *   @return The <code>AbsoluteTime</code> conversion in a newly allocated
     * object, associated with the <code>clock</code> parameter.
     *
     */
    public abstract AbsoluteTime absolute(Clock clock);

 
    /**
     * Convert the time of <code>this</code> to an absolute time, using the
     * given instance of {@link Clock} to determine the current time when
     * necessary.
     * If <code>clock</code> is <code>null</code> the
     * real-time clock is assumed.

     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     * The clock association of the result is with
     * the <code>clock</code> passed as a parameter.
     * See the derived class comments for more specific information.
     *
     *   @param	clock The instance of {@link Clock} used to convert the
     * time of <code>this</code> into absolute time,
     * and the new clock association for the result.
     *   @param	dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new object is
     * allocated for the result.
     *   @return The <code>AbsoluteTime</code> conversion in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * It is associated with the <code>clock</code> parameter.
     *
     */
    public abstract AbsoluteTime absolute(Clock clock, AbsoluteTime dest);
    
    
    /**
     * Return a clone of <code>this</code>.  
     * This method should behave effectively as if it constructed a new 
     * object with the visible values of <code>this.</code>  
     * The new object is created in the current allocation context.
     * 
     * @since 1.0.1
     */
    public Object clone(){
        try {
            return super.clone();
        }
        catch(CloneNotSupportedException ex) {
            throw new InternalError("Clone not supported????");
        }
    }

    /**
     *	Compares <code>this</code> <code>HighResolutionTime</code>
     *  with the specified <code>HighResolutionTime</code> <code>time</code>.
     *	
     *	@param time Compares with the time of <code>this</code>.
     *	@throws ClassCastException Thrown if the <code>time</code> parameter is
     * not of the same class as <code>this</code>.
     *	@throws IllegalArgumentException Thrown if the
     * <code>time</code> parameter is
     * not associated with the same clock as <code>this</code>, or when
     * the <code>time</code> parameter is <code>null</code>.
     */
    public int compareTo(HighResolutionTime time) {
        if (time == null)
            throw new IllegalArgumentException("null parameter");
        if (this.getClass() != time.getClass())
            throw new ClassCastException();
        if (this.clock != time.clock)
            throw new IllegalArgumentException("different clocks");

        if (this.milliseconds > time.milliseconds)
            return 1;
        else if (this.milliseconds < time.milliseconds)
            return -1;
        else
            return this.nanoseconds - time.nanoseconds;
    }

    /**
     * For the <code>Comparable</code> interface.
     *
     * @throws IllegalArgumentException Thrown if the
     * <code>object</code> parameter is
     * not associated with the same clock as <code>this</code>, or when
     * the <code>object</code> parameter is <code>null</code>.
     * 
     * @throws ClassCastException Thrown if the specified object's type 
     * prevents it from being compared to <code>this</code> Object.
     */
    public int compareTo(Object object) {
        return compareTo((HighResolutionTime)object);
    }

    /**
     * Returns <code>true</code> if the argument <code>time</code>
     * has the same type and values as <code>this</code>.
     * Equality includes <code>clock</code> association.
     * 
     * @param time Value compared to <code>this</code>.
     * @return <code>true</code> if the parameter <code>time</code> is of the 
     * same type and has the same values as <code>this</code>.
     */
    public boolean equals(HighResolutionTime time) {
       return this.milliseconds == time.milliseconds 
           && this.nanoseconds == time.nanoseconds 
           && this.clock == time.clock;
    }

    /**
     * Returns <code>true</code> if the argument <code>object</code>
     * has the same type and values as <code>this</code>.
     * Equality includes <code>clock</code> association.
     *
     * @param	object	Value compared to <code>this</code>.
     * @return <code>true</code>
     * if the parameter <code>object</code> is of the same type and
     * has the same values as <code>this</code>.
     */
    public boolean equals(Object object) {
        if (object instanceof HighResolutionTime)
            return equals((HighResolutionTime)object);
        return false;
    }

    /**
     * Returns a reference to the
     * <code>clock</code> associated with <code>this</code>.
     *
     * @return A reference to the <code>clock</code> associated with 
     *  <code>this</code>.
     *
     * @since 1.0.1
     */    
    public Clock getClock(){
       return clock;
    }

    /**
     *	Returns the milliseconds component of <code>this</code>.
     *
     *	@return The milliseconds component of the time
     *		represented by <code>this</code>.
     */
    public final long getMilliseconds() {
       return milliseconds;
    }

    /**
     *	Returns the nanoseconds component of <code>this</code>.
     *	
     *	@return The nanoseconds component of the time
     *		represented by <code>this</code>.
     */
    public final int getNanoseconds() {
       return nanoseconds;
    }

    /**
     * Returns a hash code for this object in accordance with the general
     *	contract of {@link Object#hashCode}. Time objects that are
     *	{@link #equals(HighResolutionTime) equal} have the same hash code.
     *
     *	@return	The hashcode value for this instance.
     */
    public int hashCode() {
        // what would be a good hashcode?
        return (int) (milliseconds ^ nanoseconds ^ clock.hashCode());
    }

    /**
     * Convert the time of <code>this</code> to a relative time, using the
     * given instance of {@link Clock} to determine the current time when
     * necessary.
     * If <code>clock</code> is <code>null</code> the
     * real-time clock is assumed.

     * A destination object is allocated to return the result.
     * The clock association of the result is with
     * the <code>clock</code> passed as a parameter.
     * See the derived class comments for more specific information.
     *
     *   @param	clock The instance of {@link Clock} used to convert the
     * time of <code>this</code> into relative time,
     * and the new clock association for the result.
     *   @return The <code>RelativeTime</code> conversion in a newly allocated
     * object, associated with the <code>clock</code> parameter.
     *
     */
    public abstract RelativeTime relative(Clock clock);

    /**
     * Convert the time of <code>this</code> to a relative time, using the
     * given instance of {@link Clock} to determine the current time when
     * necessary.
     * If <code>clock</code> is <code>null</code> the
     * real-time clock is assumed.

     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     * The clock association of the result is with
     * the <code>clock</code> passed as a parameter.
     * See the derived class comments for more specific information.
     *
     *   @param	clock The instance of {@link Clock} used to convert the
     * time of <code>this</code> into relative time,
     * and the new clock association for the result.
     *   @param	dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new object is
     * allocated for the result.
     *   @return The <code>RelativeTime</code> conversion in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * It is associated with the <code>clock</code> parameter.
     *
     */    
    public abstract RelativeTime relative(Clock clock, RelativeTime dest);

    /**
     * Change the value represented by <code>this</code> to that of the
     * given <code>time</code>. 
     * If the type of <code>this</code> and the type of the given time are
     * not the same this method will throw
     * <code>ClassCastException</code>.
     * This method ignores the possible
     * discrepancy between the <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code> associated with
     * the <code>time</code> parameter,
     * retaining the original clock association with <code>this</code>.
     *
     * @param time The new value for <code>this</code>.
     * @throws ClassCastException Thrown if the type of <code>this</code>
     * and the type of the parameter <code>time</code> are not the same.
     *
     * @since 1.0.1 The description of the method in 1.0 was erroneous.
     */
    public void set(HighResolutionTime time) {
        // @specbug: what if time is null?
        if (this.getClass() != time.getClass())
            throw new ClassCastException("different time classes");
        setDirect(time);
    }



    /**
     * Sets the millisecond component of <code>this</code> to the given
     * argument, and the nanosecond component of
     * <code>this</code> to 0.  This method is equivalent to
     * <code>set(millis, 0)</code>.
     *
     * @param millis This value shall be the value of the millisecond
     * component of <code>this</code> at the completion of the call.
     */
    public void set(long millis) {
        set(millis, 0);
    }

    /**
     * Sets the millisecond and nanosecond components of <code>this</code>.
     * The setting is subject to parameter normalization.
     * If there is an overflow in the millisecond component while normalizing
     * then an <code>IllegalArgumentException</code> will be thrown.
     *
     * If after normalization the time is negative then the time represented by
     * <code>this</code> is set to a negative value, but note that negative
     * times are not supported everywhere.  For instance, a negative relative
     * time is an invalid value for a periodic thread's period.
     *
     * @param millis The desired value for the millisecond component of
     * <code>this</code> at the completion of the call.
     * The actual value is the result of parameter normalization.
     * @param nanos The desired value for the nanosecond component of
     * <code>this</code> at the completion of the call.
     * The actual value is the result of parameter normalization.
     * @throws IllegalArgumentException Thrown if there is an overflow in the
     * millisecond component while normalizing.
     */
    public void set(long millis, int nanos) {
        if (!setNormalized(millis, nanos))
            throw new IllegalArgumentException("non-normalizable values");
    }


    // implementation details - NOTE: No protected members allowed


    /* Note that the time is always kept in a normalized form, where both
       components always have the same sign or are zero, and nanos is always
       in the range -999999 <= nanoseconds <= 999999. While a time may hold
       a value greater than that representable by a long nanosecond value, we
       convert to such a value when we use this time for sleeps/waits etc.
    */

    /** Millisecond component of the time */
    long milliseconds; 

    /** Nanosecond component of the time: -999999 <= nanoseconds <= 999999 */
    int nanoseconds;

    /** Our Clock */
    Clock clock;


    /* We mirror all the constructor forms needed by the subclasses. This is
       necessary because we want HRT to take full responsibility for clock
       checking and normalization, but the subclasses have to check for null
       parameters (and throw IllegalArgumentException) which means they can't
       invoke super constructors unless the super constructor does the null
       check - hence the super constructor must take the same parameter types
    */

    /**
     * Construct a HighResolutionTime using the given components.
     * @param millis the millisecond component
     * @param nanos the nanosecond component
     * @param clock the associated clock. If null then the Realtime clock is
     * used.
     * @throws IllegalArgument if there is an overflow in the millisecond
     * component when normalizing
     */
    HighResolutionTime(long millis, long nanos, Clock clock) {
        if (!setNormalized(millis, nanos))
            throw new IllegalArgumentException("non-normalizable values");
        setClock(clock);
    }


    static final int NANOS_PER_MILLI = 1000 * 1000;

    /**
     * Normalize the given millis and nanos components and set them in this.
     * @return <tt>true</tt> if the normalized values could be set, and
     * <tt>false</tt> if overflow would occur. If <tt>false</tt> is returned
     * then the millisecond and nanosecond components of this are unchanged.
     */
    final boolean setNormalized(final long millis, final long nanos) {
//         System.out.println("setNormalized: " + millis + ", " + nanos);
        final long millis_in_nanos = nanos / NANOS_PER_MILLI;
        final int nanosleft = (int) (nanos % NANOS_PER_MILLI);
        if (millis > 0) {
            if (nanos < 0) { // no overflow possible
                milliseconds = millis + millis_in_nanos;
                // ensure same sign
                if (milliseconds > 0 && nanosleft != 0) {
                    milliseconds--;
                    nanoseconds = nanosleft + NANOS_PER_MILLI;
                }
                else {
                    nanoseconds = nanosleft;
                }
            }
            else { // watch for overflow
                long tmp = millis + millis_in_nanos;
                if (tmp <= 0) {
//                     System.out.println("setNormalized failing: " + millis + ", " + nanos);
                    return false;
                }
                milliseconds = tmp;
                nanoseconds = nanosleft;
            }
        }
        else if (millis < 0) {
            if (nanos < 0) { // watch for negative overflow
                long tmp = millis + millis_in_nanos;
                if (tmp >= 0) {
//                     System.out.println("setNormalized failing: " + millis + ", " + nanos);
                    return false;
                }
                milliseconds = tmp;
                nanoseconds = nanosleft;
            }
            else { // no overflow possible
                milliseconds = millis + millis_in_nanos;
                // ensure same sign
                if (milliseconds < 0 && nanosleft != 0) {
                    milliseconds++;
                    nanoseconds = nanosleft - NANOS_PER_MILLI;
                }
                else {
                    nanoseconds = nanosleft;
                }
            }
        }
        else { // millis == 0
            milliseconds = millis_in_nanos;
            nanoseconds = nanosleft;
        }

        //        System.out.println("setNormalized ok: " + milliseconds + ", " + nanoseconds);
        Assert.check(! ((milliseconds < 0 && nanoseconds > 0) ||
                        (milliseconds > 0 && nanoseconds < 0)) ? Assert.OK :
                     "sign mismatch: millis = " + milliseconds + ", nanos = " + nanoseconds);
        Assert.check( nanoseconds >= -9999999 && nanoseconds <= 999999 ?
                      Assert.OK : "nanoseconds out of range");
        return true;
    }


    static final boolean CHECK_OVERFLOW = true;

    /**
     * Adds the two given values together, returning their sum if there is
     * no overflow.
     * @param arg1 first value to add
     * @param arg2 second value to add
     * @return the sum: <tt>arg1+arg2</tt>
     * @throws ArithmeticException if there is an overflow in the addition
     */
    static long addSafe(long arg1, long arg2) {
        long sum = arg1 + arg2;
        if (CHECK_OVERFLOW) {
            if ( (arg1 > 0 && arg2 > 0 && sum <=0) ||
                 (arg1 < 0 && arg2 < 0 && sum >= 0) ) {
                throw new ArithmeticException("overflow");
            }
        }
        return sum;
    }

    /**
     * Set the millis and nanos component of this to be the same as 
     * <tt>time</tt>. This is only called when we know time is the right type.
     */
    final void setDirect(HighResolutionTime time) {
        this.milliseconds = time.milliseconds;
        this.nanoseconds = time.nanoseconds;
    }

    /**
     * Set the millis and nanos component of this to be the same as 
     * this given. This is only called when we know the values are already
     * normalized.
     */
    final void setDirect(long millis, int nanos) {
        this.milliseconds = millis;
        this.nanoseconds = nanos;
    }

    /**
     * Set the millis and nanos component of this based on the nanos value
     * given. This is only called when we know the values are normalizable.
     */
    final void setDirect(long nanos) {
        this.milliseconds = nanos / NANOS_PER_MILLI;
        this.nanoseconds = (int) (nanos % NANOS_PER_MILLI);
    }


    /** Convert the given nanos value to millis
     */
    final long getMillis(long nanos) {
        return nanos / NANOS_PER_MILLI;
    }

    /** Convert the given nanos value to the remaining nanos after
        subtracting the millis
    */
    final int getNanosRemaining(long nanos) {
        return (int) (nanos % NANOS_PER_MILLI);
    }

    /**
     * Set the clock of this to the given clock, or the real-time clock if
     * null.
     */
    final void setClock(Clock clock) {
        if (clock == null) 
            this.clock = Clock.rtc;
        else
            this.clock = clock;
    }


    // convenience functions for use by other classes in javax.realtime

    /**
     * Convert this time to a pure nanos value for use with sleeps or waits.
     * The time value must be known to be positive and if it overflows a long
     * then set to <tt>Long.MAX_VALUE</tt> - we're not going to be able to
     * wait 256 years (291 less 35 since epoch) to get the bug report.
     * @return the time value of this in nanoseconds, or 
     * <tt>Long.MAX_VALUE</tt> if this time value overflows a long
     */
    final long toNanos() {
        long nanos = milliseconds*NANOS_PER_MILLI + nanoseconds;
        if (nanos < 0) {
            nanos = Long.MAX_VALUE;
        }
        return nanos;
    }

    /**
     * Returns <tt>true</tt> if this time represents a negative time value.
     * Used by time taking methods to reject negative time parameters.
     */
    final boolean isNegative() {
        return milliseconds < 0 || nanoseconds < 0;
    }




}
