/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/AbsoluteTime.java,v 1.1 2004/10/15 01:53:11 dholmes Exp $
 */
package javax.realtime;
import java.util.Date;

/** 
 * An object that represents a specific point in time given by
 * milliseconds plus nanoseconds past some point in time fixed by the
 * <code>clock</code>.  For the default real-time clock the
 * fixed point is the Epoch
 * (January 1, 1970, 00:00:00 GMT).
 * 
 * The correctness of the Epoch as a time base depends on the
 * real-time clock
 * synchronization with an external world time reference.
 * This representation was designed to be compatible with
 * the standard Java representation of an absolute time in the
 * {@link java.util.Date} class.
 *
 * <p>
 * A time object in normalized form represents negative time if
 * both components are nonzero and negative, or one is nonzero and negative
 * and the other is zero.
 * For <code>add</code> and <code>subtract</code> negative values
 * behave as they do in arithmetic.
 * </p>
 *
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded
 * situations when it is being changed. No synchronization is done. It
 * is assumed that users of this class who are mutating instances will be
 * doing their own synchronization at a higher level.
 *
 * @spec RTSJ 1.0.1
 */
public class AbsoluteTime extends HighResolutionTime {


    /** 
     * Helper method to extract a millisecond value from a time object,
     * throwing <tt>IllegalArgumentException</tt> if the parameter is null.
     * This is invoked from the invocation of a this() or super() constructor.
     * @param time a time object
     */
    private static long getMillisNonNull(AbsoluteTime time) {
        if (time == null)
            throw new IllegalArgumentException("null parameter");
        return time.milliseconds;
    }


    /** 
     * Helper method to extract a millisecond value from a date object,
     * throwing <tt>IllegalArgumentException</tt> if the parameter is null.
     * This is invoked from the invocation of a this() or super() constructor.
     * @param date a date object
     */
    private static long getMillisNonNull(Date date) {
        if (date == null)
            throw new IllegalArgumentException("null parameter");
        return date.getTime();
    }


    /* All constructors defer to this(...) except the most general that
       defers to super(millis, nanos, clock)
    */

    /**
     * Equivalent to new <code>AbsoluteTime(0,0)</code>.
     * The clock association is implicitly made with the real-time clock.
     */
    public AbsoluteTime(){
        this(0, 0, null);
    }

    /**
     * Make a new <code>AbsoluteTime</code> object from the given
     * <code>AbsoluteTime</code> object.
     * The new object will have the same clock association as the
     * <code>time</code> parameter.
     *
     * @param time The <code>AbsoluteTime</code> object which is the
     * source for the copy.
     * @throws IllegalArgumentException Thrown if the <code>time</code>
     * parameter is <code>null</code>.
     */
    public AbsoluteTime(AbsoluteTime time){
        this(getMillisNonNull(time), time.nanoseconds, time.clock);
    }

    /**
     * Make a new <code>AbsoluteTime</code> object from the given
     * <code>AbsoluteTime</code> object.
     * The clock association is made with the <code>clock</code> parameter.
     * If <code>clock</code> is null the association is made with
     * the real-time clock.
     *
     * @param time The <code>AbsoluteTime</code> object which is the
     * source for the copy.
     * @param clock The clock providing the association for the newly
     * constructed object.
     * @throws IllegalArgumentException Thrown if the <code>time</code>
     * parameter is <code>null</code>.
     *
     * @since 1.0.1
     */
    public AbsoluteTime(AbsoluteTime time, Clock clock){
        this(getMillisNonNull(time), time.nanoseconds, clock);
    }

    /**
     * Equivalent to new <code>AbsoluteTime(0,0,clock)</code>.
     * The clock association is made with the <code>clock</code> parameter.
     * If <code>clock</code> is null the association is made with
     * the real-time clock.
     *
     * @param clock The clock providing the association for the newly
     * constructed object.
     *
     * @since 1.0.1
     */
    public AbsoluteTime(Clock clock){
        this(0, 0, clock);
    }

    /**
     * Equivalent to new <code>AbsoluteTime (date.getTime(),0)</code>.
     * The clock association is implicitly made with the real-time clock.
     *
     * @param date The <code>java.util.Date</code> representation of the
     * time past the Epoch.
     * @throws IllegalArgumentException Thrown if the <code>date</code>
     * parameter is <code>null</code>.
     */	
    public AbsoluteTime(java.util.Date date){
        this(getMillisNonNull(date), 0, null);
    }

    /**
     * Equivalent to new <code>AbsoluteTime (date.getTime(),0,clock)</code>.
     * <p>
     * Warning: While the <code>date</code> is used to set the milliseconds
     * component of the new <code>AbsoluteTime</code> object (with
     * nanoseconds component set to 0), the new object represents the
     * <code>date</code> only if the <code>clock</code> parameter has an
     * epoch equal to Epoch.
     * <p>
     * The clock association is made with the <code>clock</code> parameter.
     * If <code>clock</code> is null the association is made with
     * the real-time clock.
     *
     * @param date The <code>java.util.Date</code>
     * representation of the time past the Epoch.
     * @param clock The clock providing the association for the newly
     * constructed object.
     * @throws IllegalArgumentException Thrown if the <code>date</code>
     * parameter is <code>null</code>.
     *
     * @since 1.0.1
     */	
    public AbsoluteTime(java.util.Date date, Clock clock){
        this(getMillisNonNull(date), 0, clock);
    }

    /**
     * Construct an <code>AbsoluteTime</code> object with time millisecond
     * and nanosecond components past the real-time clock's Epoch
     * (00:00:00 GMT on January 1, 1970) based on the parameter
     * <code>millis</code> plus the parameter <code>nanos</code>.
     * The construction is subject to <code>millis</code> and
     * <code>nanos</code> parameters normalization.
     * If there is an overflow in the millisecond component
     * when normalizing then an <code>IllegalArgumentException</code>
     * will be thrown.
     * If after normalization the time object is negative then the time
     * represented by this is time before the Epoch.
     * <p>
     * The clock association is implicitly made with the real-time clock.
     *
     * @param millis The desired value for the millisecond component
     * of <code>this</code>.
     * The actual value is the result of parameter normalization.
     * @param nanos The desired value for the nanosecond component
     * of <code>this</code>.
     * The actual value is the result of parameter normalization.
     * @throws IllegalArgumentException Thrown if there is an overflow in the
     * millisecond component when normalizing.
     */
    public AbsoluteTime(long millis, int nanos){
        this(millis, nanos, null);
    }

    /**
     * Construct an <code>AbsoluteTime</code> object with time millisecond
     * and nanosecond components past the epoch for <code>clock</code>.
     * <p>
     * The value of the <code>AbsoluteTime</code> instance is based on the
     * parameter
     * <code>millis</code> plus the parameter <code>nanos</code>.
     * The construction is subject to <code>millis</code> and
     * <code>nanos</code> parameters normalization.
     * If there is an overflow in the millisecond component
     * when normalizing then an <code>IllegalArgumentException</code>
     *  will be thrown.
     * If after normalization the time object is negative then the time
     * represented by this is time before the <code>epoch</code>.
     * <p>
     * The clock association is made with the <code>clock</code> parameter.
     * If <code>clock</code> is null the association is made with
     * the real-time clock.
     * <p>
     * Note: The start of a clock's epoch is an attribute of the clock.
     * It is defined as the Epoch (00:00:00 GMT on Jan 1, 1970) for the
     * default real-time clock, but other classes of clock may define other
     * epochs.
     *
     * @param millis The desired value for the millisecond component
     * of <code>this</code>.
     * The actual value is the result of parameter normalization.
     * @param nanos The desired value for the nanosecond component
     * of <code>this</code>.
     * The actual value is the result of parameter normalization.
     * @param clock The clock providing the association for the newly
     * constructed object.
     * @throws IllegalArgumentException Thrown if there is an overflow in the
     * millisecond component when normalizing.
     *
     * @since 1.0.1
     */
    public AbsoluteTime(long millis, int nanos, Clock clock){
         super(millis, (long)nanos, clock);
    }

    /**
     * Return a copy of <code>this</code> modified if necessary to
     * have the specified clock association.
     * A new object is allocated for the result.
     * This method is the implementation of the
     * <code>abstract</code> method of the <code>HighResolutionTime</code>
     * base class. No conversion into <code>AbsoluteTime</code> is needed
     * in this case.
     *
     * The clock association of the result is with
     * the <code>clock</code> passed as a parameter.
     * If <code>clock</code> is null the association is made with
     * the real-time clock.
     *
     *   @param clock 
     * The <code>clock</code> parameter is
     * used only as the new clock association with the result,
     * since no conversion is needed.
     *   @return The copy of <code>this</code> in a
     * newly allocated <code>AbsoluteTime</code> object,
     * associated with the <code>clock</code> parameter.
     * 
     */
    public AbsoluteTime absolute(Clock clock){
        return new AbsoluteTime(this.milliseconds, this.nanoseconds, clock);
    }

    /**
     * Return a copy of <code>this</code> modified if necessary to
     * have the specified clock association.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * in <code>dest</code> 
     * and returned. Otherwise, a new object is allocated for the result.
     * This method is the implementation of the
     * <code>abstract</code> method of the <code>HighResolutionTime</code>
     * base class. No conversion into <code>AbsoluteTime</code> is needed
     * in this case.
     *    
     * The clock association of the result is with
     * the <code>clock</code> passed as a parameter.
     * If <code>clock</code> is null the association is made with
     * the real-time clock.
     *
     *   @param clock 
     * The <code>clock</code> parameter is
     * used only as the new clock association with the result,
     * since no conversion is needed.
     *   @param	dest 
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     *   @return The copy of <code>this</code> in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * It is associated with the <code>clock</code> parameter.
     */
    public AbsoluteTime absolute(Clock clock, AbsoluteTime dest) {
        if (dest == null)
            return absolute(clock);
        else {
            dest.setDirect(this);
            dest.setClock(clock);
            return dest;
        }
    }

    /**
     * Create a new object representing the result of adding 
     * <code>millis</code> and <code>nanos</code> to 
     * the values from <code>this</code> and normalizing the result.
     * The result will have the same clock association as <code>this</code>.
     * An <code>ArithmeticException</code> is thrown if there is an overflow 
     * in the result after normalization.
     *
     * @param millis The number of milliseconds to be added
     * to <code>this</code>.
     * @param nanos The number of nanoseconds to be added
     * to <code>this</code>.
     * @return A new <code>AbsoluteTime</code> object whose time is
     * the normalization of
     * <code>this</code> plus <code>millis</code> and <code>nanos</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */
    public AbsoluteTime add(long millis, int nanos){
        return add(millis, nanos, new AbsoluteTime(0, 0, this.clock));
    }

    /**
     * Return an object containing the value 
     * resulting from adding <code>millis</code> and <code>nanos</code> to 
     * the values from <code>this</code> and normalizing the result.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     * The result will have the same clock association as <code>this</code>,
     * and the clock association with <code>dest</code> is ignored.
     * An <code>ArithmeticException</code> is thrown if there is an overflow 
     * in the result after normalization.
     *
     * @param millis The number of milliseconds to be added
     * to <code>this</code>.
     * @param nanos The number of nanoseconds to be added
     * to <code>this</code>.
     * @param dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new
     * object is allocated for the result.
     *   @return  the result of the normalization of
     * <code>this</code> plus <code>millis</code> and <code>nanos</code> in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     *
     */	
    public AbsoluteTime add(long millis, int nanos, AbsoluteTime dest) {
        // Note: dest could == this, so take care
        if (dest == null)
            dest = new AbsoluteTime(0, 0, this.clock);
        if (!dest.setNormalized(addSafe(this.milliseconds, millis),
                                ((long)this.nanoseconds) + nanos) )
            throw new ArithmeticException("non-normalizable result");
        // note: don't change dest until we know no exceptions will occur
        dest.setClock(this.clock);
        return dest;
    }

    /**
     * Create a new instance of <code>AbsoluteTime </code>
     * representing the result of adding <code>time</code> to 
     * the value of <code>this</code> and normalizing the result.
     * The <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * must be the same, and such association is used for the result.
     *
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different.
     *
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>time</code> parameter is <code>null</code>.
     * An <code>ArithmeticException</code> is thrown if there is an overflow 
     * in the result after normalization.
     *
     * @param time The time to add to <code>this</code>.
     * @return 
     * A new <code>AbsoluteTime</code> object whose time is
     * the normalization of
     * <code>this</code> plus the parameter <code>time</code>.
     * @throws IllegalArgumentException Thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different, or when the
     * <code>time</code> parameter is <code>null</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */
    public AbsoluteTime add(RelativeTime time){
        return this.add(time, null);
    }	


    /**
     * Return an object containing the value 
     * resulting from adding <code>time</code> to 
     * the value of <code>this</code> and normalizing the result.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     * The <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * must be the same, and such association is used for the result.
     * The <code>clock</code>
     * associated with the <code>dest</code> parameter is ignored.
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter are different.
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>time</code> parameter is <code>null</code>.
     * An <code>ArithmeticException</code> is thrown if there is an 
     * overflow in the result after normalization.
     *
     * @param time The time to add to <code>this</code>.
     * @param dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new
     * object is allocated for the result.
     *   @return  the result of the normalization of
     * <code>this</code> plus the <code>RelativeTime</code>
     * parameter <code>time</code> in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * @throws IllegalArgumentException Thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different, or when the
     * <code>time</code> parameter is <code>null</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */	
    public AbsoluteTime add(RelativeTime time, AbsoluteTime dest){
        if (time == null || time.clock != this.clock)
            throw new IllegalArgumentException("null arg or different clock");
        return this.add(time.milliseconds, time.nanoseconds, dest);
    }


    /**
     * Convert the time given by <code>this</code> to a {@link Date} format. 
     * Note that {@link Date} represents time as milliseconds so the
     * nanoseconds of <code>this</code> will be lost.
     * An <code>UnsupportedOperationException</code> is thrown if the clock
     * associated with <code>this</code> does not have the concept of date.
     * @return A newly allocated
     * {@link Date} object with a value of the
     * time past the Epoch represented by <code>this</code>.
     * @throws UnsupportedOperationException  Thrown if the
     * clock associated with <code>this</code> does not have the concept
     * of date.
     */
    public Date getDate(){
        return new Date(this.milliseconds +
                        this.clock.getEpochOffset().milliseconds); 
    }	


    /**
     * Convert the time of <code>this</code> to a relative time, using the
     * given instance of {@link Clock} to determine the current time.
     *
     * The calculation is the current time indicated by the given instance of 
     * {@link Clock} subtracted from the time given by <code>this</code>.
     *
     * If <code>clock</code> is <code>null</code> the
     * real-time clock is assumed.
     *
     * A destination object is allocated to return the result.
     *
     * The clock association of the result is with
     * the <code>clock</code> passed as a parameter.
     *
     *   @param	clock The instance of {@link Clock} used to convert the
     * time of <code>this</code> into relative time,
     * and the new clock association for the result.
     *   @return The <code>RelativeTime</code> conversion in a newly allocated
     * object, associated with the <code>clock</code> parameter.
     * 
     *  @throws ArithmeticException Thrown if computation of
     * 	the relative time gives an overflow after normalization.
     */
    public RelativeTime relative(Clock clock){
        return relative(clock, null);
    }


    /**
     * Convert the time of <code>this</code> to a relative time, using the
     * given instance of {@link Clock} to determine the current time.
     *
     * The calculation is the current time indicated by the given instance of 
     * {@link Clock} subtracted from the time given by <code>this</code>.
     *
     * If <code>clock</code> is <code>null</code> the
     * real-time clock is assumed.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     *    
     * The clock association of the result is with
     * the <code>clock</code> passed as a parameter.
     *
     *   @param	clock The instance of {@link Clock} used to convert the
     * time of <code>this</code> into relative time,
     * and the new clock association for the result.
     *
     *   @param	dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new object is
     * allocated for the result.
     *   @return The <code>RelativeTime</code> conversion in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * It is associated with the <code>clock</code> parameter.
     * 
     *  @throws ArithmeticException Thrown if computation of
     * 	the relative time gives an overflow after normalization.
     */
    public RelativeTime relative(Clock clock, RelativeTime dest){
        if (clock == null)
            clock = Clock.rtc;
        // can't avoid creating the intermediate AbsoluteTime in general
        // We could avoid for the RTC if it is worth the effort
        return clock.getTime().subtract(this.milliseconds, 
                                        this.nanoseconds, dest);
    }

    /**
     * Change the time represented by <code>this</code> to that given
     * by the parameter.
     * Note that {@link Date} represents time as milliseconds so the
     * nanoseconds of <code>this</code> will be set to 0.
     * An <code>UnsupportedOperationException</code> is thrown if the clock
     * associated with <code>this</code> does not have the concept of date.
     * @param date A reference to a {@link Date} which will become
     * the time represented by <code>this</code>
     * after the completion of this method.
     * @throws UnsupportedOperationException  if the
     * clock associated with <code>this</code> does not have the concept
     * of date.
     */
    public void set(Date date){
        // @specbug: what if date is null?
        // Note: normalization errors are impossible
        setDirect(addSafe(date.getTime(),
                  this.clock.getEpochOffset().milliseconds),
                  0); 
    }


    /**
     * Create a new instance of <code>RelativeTime</code>
     * representing the result of subtracting <code>time</code> from 
     * the value of <code>this</code> and normalizing the result.
     *
     * The <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * must be the same, and such association
     * is used for the result.
     *
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different.
     *
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>time</code> parameter is <code>null</code>.
     * An <code>ArithmeticException</code> is thrown if there is an 
     * overflow in the result after normalization.
     * 
     * @param time The time to subtract from <code>this</code>.
     *
     * @return 
     * A new <code>RelativeTime</code> object whose time is
     * the normalization of
     * <code>this</code> minus the <code>AbsoluteTime</code>
     * parameter <code>time</code>.
     * @throws IllegalArgumentException
     * if the <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different, or when the
     * <code>time</code> parameter is <code>null</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */
    public RelativeTime subtract(AbsoluteTime time){
        return this.subtract(time, null);
    }


    /**
     * Return an object containing the value 
     * resulting from subtracting <code>time</code> from 
     * the value of <code>this</code>
     * and normalizing the result.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     * The <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * must be the same, and such association is used for the result.
     *
     * The <code>clock</code> associated with the <code>dest</code> parameter
     * is ignored.
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different.
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>time</code> parameter is <code>null</code>.
     * An <code>ArithmeticException</code> is thrown if there is an 
     * overflow in the result after normalization.
     * 
     * @param time The time to subtract from <code>this</code>.
     * @param dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new
     * object is allocated for the result.
     *   @return  the result of the normalization of
     * <code>this</code> minus the <code>AbsoluteTime</code>
     * parameter <code>time</code> in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * @throws IllegalArgumentException
     * if the* <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different, or when the <code>time</code> parameter is 
     * <code>null</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */
    public RelativeTime subtract(AbsoluteTime time, RelativeTime dest){
        if (time == null || time.clock != this.clock)
            throw new IllegalArgumentException("null arg or different clock");
        return this.subtract(time.milliseconds, time.nanoseconds, dest);
    }


    /**
     * Create a new instance of <code>AbsoluteTime</code>
     * representing the result of subtracting <code>time</code> from 
     * the value of <code>this</code> and normalizing the result.

     * The <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * must be the same, and such association
     * is used for the result.

     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different.
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>time</code> parameter is <code>null</code>.
     * An <code>ArithmeticException</code> is thrown if there is an overflow
     * in the result after normalization.
     *
     * @param time The time to subtract from <code>this</code>.
     * @return 
     * A new <code>AbsoluteTime</code> object whose time is
     * the normalization of
     * <code>this</code> minus the parameter <code>time</code>.
     * @throws IllegalArgumentException Thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different, or when the
     * <code>time</code> parameter is <code>null</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */
    public AbsoluteTime subtract(RelativeTime time){
        return this.subtract(time, null);
    }
		

    /**
     * Return an object containing the value 
     * resulting from subtracting <code>time</code> from 
     * the value of <code>this</code> and normalizing the result.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     * The <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * must be the same, and such association* is used for the result.
     * The <code>clock</code>
     * associated with the <code>dest</code> parameter is ignored.
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter are different.
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>time</code> parameter is <code>null</code>.
     * An <code>ArithmeticException</code> is thrown if there is an 
     * overflow in the result after normalization.
     * 
     * @param time The time to subtract from <code>this</code>.
     * @param dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new
     * object is allocated for the result.
     * @return  the result of the normalization of
     * <code>this</code> minus the <code>RelativeTime</code>
     * parameter <code>time</code> in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * @throws IllegalArgumentException Thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different, or when the
     * <code>time</code> parameter is <code>null</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */
    public AbsoluteTime subtract(RelativeTime time, AbsoluteTime dest){
        if (time == null || time.clock != this.clock)
            throw new IllegalArgumentException("null arg or different clock");
        if (dest == null)
            dest = new AbsoluteTime(0, 0, this.clock);
        if (!dest.setNormalized(addSafe(this.milliseconds, -time.milliseconds), 
                                ((long)this.nanoseconds) - time.nanoseconds))
            throw new ArithmeticException("non-normalizable values");
        // note: don't change dest until we know no exceptions will occur
        dest.setClock(this.clock);
        return dest;
    }


    /**
     * Create a printable string of the time given by <code>this</code>.
     * <p>
     * The string shall be a decimal representation of the milliseconds and 
     * nanosecond values; formatted as follows "(2251 ms, 750000 ns)"
     * @return String object converted from the
     * time given by <code>this</code>.
     */
    public String toString(){
        // ms since epoch needs 13 digits, 6 for nanos, 10 for rest
        return new StringBuffer(29).append("(").append(milliseconds).
            append(" ms, ").append(nanoseconds).append(" ns)").toString();
    }



    // implementation details

    /**
     * Construct an absolute time from an absolute time in nanoseconds since
     * the Epoch.
     */
    AbsoluteTime(long nanos) {
        super(nanos/NANOS_PER_MILLI, (int)(nanos%NANOS_PER_MILLI), Clock.rtc);
    }

    /**
     * Return a <tt>RelativeTime</tt> object containing the value 
     * resulting from subtracting <code>millis</code> and <code>nanos</code>  
     * from the values from <code>this</code> and normalizing the result.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     * The result will have the same clock association as <code>this</code>,
     * and the clock association with <code>dest</code> is ignored.
     * An <code>ArithmeticException</code> is thrown if there is an overflow 
     * in the result after normalization.
     *
     * @param millis The number of milliseconds to be subtracted from
     * <code>this</code>.
     * @param nanos The number of nanoseconds to be subtracted from
     * <code>this</code>.
     * @param dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new
     * object is allocated for the result.
     * @return  the result of the normalization of
     * <code>this</code> minus <code>millis</code> and <code>nanos</code> in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     *
     */	
    RelativeTime subtract(long millis, int nanos, RelativeTime dest) {
        if (dest == null)
            dest = new RelativeTime(0, 0, this.clock);
        if (!dest.setNormalized(addSafe(this.milliseconds, -millis),
                                ((long)this.nanoseconds) - nanos) )
            throw new ArithmeticException("non-normalizable result");
        // note: don't change dest until we know no exceptions will occur
        dest.setClock(this.clock);
        return dest;
    }
    
}
