/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/RelativeTime.java,v 1.1 2004/10/15 01:53:12 dholmes Exp $
 */
package javax.realtime;

/**
 * An object that represents a time interval
 * milliseconds/10<sup>3</sup> + nanoseconds/10<sup>9</sup> seconds long.
 * It generally is used to represent a time relative to now.
 * 
 * <p>
 * The time interval is kept in normalized form. The range goes from
 * <tt>[(-2</tt><sup><tt>63</tt></sup><tt>)</tt> milliseconds
 * <tt>+ (-10</tt><sup><tt>6</tt></sup><tt>+1)</tt> nanoseconds<tt>]</tt>
 * to
 * <tt>[(2</tt><sup><tt>63</tt></sup><tt>-1)</tt> milliseconds
 * <tt>+ (10</tt><sup><tt>6</tt></sup><tt>-1)</tt> nanoseconds<tt>]</tt>.
 * </p>
 * <p>
 * A negative interval relative to now represents time in the past.
 * Although logically, and correctly, this may represent time before the
 * epoch, an instance of
 * <code>RelativeTime</code> representing time before the epoch may cause some
 * methods to throw an <code>IllegalArgumentException</code>.
 * For <code>add</code> and <code>subtract</code> negative values behave
 * as they do in arithmetic.
 * </p>
 *   
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded
 * situations when it is being changed. No synchronization is done.
 * It is assumed that users of this class who are mutating instances will
 * be doing their own synchronization at a higher level.
 *
 * @spec RTSJ 1.0.1
 */
public class RelativeTime extends HighResolutionTime {


    /** 
     * Helper method to extract a millisecond value from a time object,
     * throwing <tt>IllegalArgumentException</tt> if the parameter is null.
     * This is invoked from the invocation of a this() or super() constructor.
     * @param time a time object
     */
    private static long getMillisNonNull(RelativeTime time) {
        if (time == null)
            throw new IllegalArgumentException("null parameter");
        return time.milliseconds;
    }


    /* All constructors defer to this(...) except the most general that
       defers to super(millis, nanos, clock)
    */


    /**
     * Equivalent to <code>new RelativeTime(0,0)</code>.
     * The clock association is implicitly made with the real-time clock.
     */
    public RelativeTime(){
        this(0, 0, null);
    }

    /**
     * Equivalent to <code>new RelativeTime(0,0,clock)</code>.
     * The clock association is made with the <code>clock</code> parameter.
     * If <code>clock</code> is null the association is made with
     * the real-time clock.
     *
     * @param clock The clock providing the association for the newly
     * constructed object.
     *
     * @since 1.0.1
     */
    public RelativeTime(Clock clock){
        this(0, 0, clock);
    }


    /**
     * Construct a <code>RelativeTime</code> object representing an interval 
     * based on the parameter
     * <code>millis</code> plus the parameter <code>nanos</code>.
     * The construction is subject to <code>millis</code> and
     * <code>nanos</code> parameters normalization.
     * If there is an overflow in the millisecond component
     * when normalizing then an
     * <code>IllegalArgumentException</code> will be thrown.
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
    public RelativeTime(long millis, int nanos){
        this(millis, nanos, null);
    }


    /**
     * Construct a <code>RelativeTime</code> object representing an interval 
     * based on the parameter
     * <code>millis</code> plus the parameter <code>nanos</code>.
     * The construction is subject to <code>millis</code> and
     * <code>nanos</code> parameters normalization.
     * If there is an overflow in the millisecond component
     * when normalizing then an
     * <code>IllegalArgumentException</code>
     * will be thrown.
     * The clock association is made with the <code>clock</code> parameter.
     * If <code>clock</code> is null the association is made with
     * the real-time clock.
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
    public RelativeTime(long millis, int nanos, Clock clock){
        super(millis, (long)nanos, clock);
    }


    /**	
     * Make a new <code>RelativeTime</code> object from the given
     * <code>RelativeTime</code> object.
     * The new object will have the same clock association as the
     * <code>time</code> parameter.
     *
     * @param time The <code>RelativeTime</code> object which is the
     * source for the copy.
     * @throws IllegalArgumentException Thrown if the <code>time</code>
     * parameter is <code>null</code>.
     */
    public RelativeTime(RelativeTime time){
        this(getMillisNonNull(time), time.nanoseconds, time.clock);
    }


    /**	
     * Make a new <code>RelativeTime</code> object from the given
     * <code>RelativeTime</code> object.
     * The clock association is made with the <code>clock</code> parameter.
     * If <code>clock</code> is null the association is made with
     * the real-time clock.
     *
     * @param time The <code>RelativeTime</code> object which is the
     * source for the copy.
     * @param clock The clock providing the association for the newly
     * constructed object.
     * @throws IllegalArgumentException Thrown if the <code>time</code>
     * parameter is <code>null</code>.
     *
     * @since 1.0.1
     */
    public RelativeTime(RelativeTime time, Clock clock){
        this(getMillisNonNull(time), time.nanoseconds, clock);
    }

    /** 
     * Convert the time of <code>this</code> to an absolute time, using the
     * given instance of {@link Clock} to determine the current time.
     *
     * The calculation is the current time indicated by the given instance of 
     * {@link Clock} plus the interval given by <code>this</code>.
     *
     * If <code>clock</code> is <code>null</code> the
     * real-time clock is assumed.
     * A destination object is allocated to return the result.
     *
     * The clock association of the result is with
     * the <code>clock</code> passed as a parameter.
     *
     *   @param	clock The instance of {@link Clock} used to convert the
     * time of <code>this</code> into absolute time,
     * and the new clock association for the result.
     *   @return The <code>AbsoluteTime</code> conversion in a newly allocated
     * object, associated with the <code>clock</code> parameter.
     */
    /*
     * @specbug: This throws clause elided by accident
     *  @throws ArithmeticException Thrown if computation of
     * 	the absolute time gives an overflow after normalization.     
     */
    public AbsoluteTime absolute(Clock clock){
        return absolute(clock, null);
    }

    /** 
     * Convert the time of <code>this</code> to an absolute time, using the
     * given instance of {@link Clock} to determine the current time.
     *
     * The calculation is the current time indicated by the given instance of 
     * {@link Clock} plus the interval given by <code>this</code>.
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
     * time of <code>this</code> into absolute time,
     * and the new clock association for the result.
     *   @param	dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new object is
     * allocated for the result.
     *   @return The <code>AbsoluteTime</code> conversion in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * It is associated with the <code>clock</code> parameter.
     */
    /*
     * @specbug: This throws clause elided by accident
     *  @throws ArithmeticException Thrown if computation of
     * 	the absolute time gives an overflow after normalization.     
     */
    public AbsoluteTime absolute(Clock clock, AbsoluteTime dest){
        if (dest == null)
            dest = new AbsoluteTime(0, 0, clock);
        // avoid any further construction. This implementation can safely
        // do arithmetic in place.
        return clock.getTime(dest).add(this.milliseconds,
                                       this.nanoseconds,
                                       dest);
    }


    /**
     * Create a new object representing the result of adding 
     * <code>millis</code> and <code>nanos</code> to the values from 
     * <code>this</code> and normalizing the result.
     *
     * The result will have the same clock association as <code>this</code>.
     * An <code>ArithmeticException</code> is thrown if there is an overflow
     * in the result after normalization.
     *
     * @param millis The number of milliseconds to be added
     * to <code>this</code>.
     * @param nanos The number of nanoseconds to be added
     * to <code>this</code>.
     * @return A new <code>RelativeTime</code> object whose time is
     * the normalization of
     * <code>this</code> plus <code>millis</code> and <code>nanos</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     *
     * @return A new object containing the result of the addition.
     */
    public RelativeTime add(long millis, int nanos){
        return add(millis, nanos, null);
    }	


    /**
     * Return an object containing the value 
     * resulting from adding <code>millis</code> and <code>nanos</code> to 
     * the values from <code>this</code> and normalizing the result.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     *
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
     */
    public RelativeTime add(long millis, int nanos, RelativeTime dest){
        // Note: dest could == this, so take care
        if (dest == null)
            dest = new RelativeTime(0, 0, this.clock);
        if (!dest.setNormalized(addSafe(this.milliseconds, millis),
                                ((long)this.nanoseconds) + nanos) )
            throw new ArithmeticException("non-normalizable result");
        // note: don't change dest until we know no exceptions will occur
        dest.setClock(this.clock);
        return dest;
    }

    /**
     * Create a new instance of <code>RelativeTime </code>
     * representing the result of adding <code>time</code> to 
     * the value of <code>this</code> and normalizing the result.
     * The <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are expected to be the same, and such association
     * is used for the result.
     *
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
     * @param time The time to add to <code>this</code>.
     * @return A new <code>RelativeTime</code> object whose time is
     * the normalization of
     * <code>this</code> plus the parameter <code>time</code>.
     * @throws IllegalArgumentException Thrown if the
     * 
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different, or when the
     * <code>time</code> parameter is <code>null</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */
    public RelativeTime add(RelativeTime time){
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
     * are expected to be the same, and such association
     * is used for the result.
     *
     * The <code>clock</code>
     * associated with the <code>dest</code> parameter
     * is ignored.
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
     * @param dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new
     * object is allocated for the result.
     * @return  the result of the normalization of
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
    public RelativeTime add(RelativeTime time, RelativeTime dest){
        if (time == null || time.clock != this.clock)
            throw new IllegalArgumentException("null arg or different clock");
        return this.add(time.milliseconds, time.nanoseconds, dest);
    }


    /* the next three methods should have been deleted as they are in this
       class by mistake. They have been deprecated because of perceived
       compatability issues instead.
    */

    /**
     * Add the interval of <code>this</code> to the given instance of 
     * {@link AbsoluteTime}.
     *
     * <p><b>In this OVM implementation this method does nothing: as a 
     * <tt>RelativeTime</tt> does not have an inter-arrival time, 
     * there is nothing to add.
     * Neither this method, nor its description makes any sense whatsoever.</b
     *
     * @deprecated As of RTSJ 1.0.1
     *
     * @param timeAndDestination A reference to the given instance of 
     * {@link AbsoluteTime} and the result.
     *
     */
    public void addInterarrivalTo(AbsoluteTime timeAndDestination){
    }


    /**
     * Gets the interval defined by <code>this</code>. 
     * For an instance of {@link RationalTime} it
     * is the interval divided by the frequency.
     *
     * <p><b>In this OVM implementation we return 
     * <tt>new RelativeTime(0,0)</tt>.
     * As we have no inter-arrival time then any instance of 
     * <tt>RelativeTime</tt> has the same one that we do.
     * Neither this method, nor its description makes any sense whatsoever.</b>
     *
     * @deprecated As of RTSJ 1.0.1
     *
     * @return A reference to a new instance of {@link RelativeTime} with the 
     * same interval as <code>this</code>.
     */
    public RelativeTime getInterarrivalTime(){
       return new RelativeTime(0,0);
    }


    /**
     * Gets the interval defined by <code>this</code>. 
     * For an instance of {@link RationalTime} it
     * is the interval divided by the frequency.
     *
     * <p><b>In this OVM implementation this method returns 
     * <tt>destination</tt> with no other effect: as a <tt>RelativeTime</tt> 
     * does not have an inter-arrival time, the destination object already
     * adequately represents it.
     * Neither this method, nor its description makes any sense whatsoever.</b
     *
     * @deprecated As of RTSJ 1.0.1
     *
     * @param destination A reference to the new object holding the result.
     *
     * @return A reference to an object holding the result.
     */
    public RelativeTime getInterarrivalTime(RelativeTime destination){
       return destination;
    }


    /**
     * Return a copy of <code>this</code>.
     * A new object is allocated for the result.
     * This method is the implementation of the
     * <code>abstract</code> method of the <code>HighResolutionTime</code>
     * base class. No conversion into <code>RelativeTime</code> is needed
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
     * 
     *   @return The copy of <code>this</code> in a
     * newly allocated <code>RelativeTime</code> object,
     * associated with the <code>clock</code> parameter.
     */
    public RelativeTime relative(Clock clock){
        return new RelativeTime(this.milliseconds, this.nanoseconds, clock);
    }


    /**
     * Return a copy of <code>this</code>.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     * This method is the implementation of the
     * <code>abstract</code> method of the <code>HighResolutionTime</code>
     * base class. No conversion into <code>RelativeTime</code> is needed
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
     * 
     *   @return The copy of <code>this</code> in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * It is associated with the <code>clock</code> parameter.
     * 
     */
    public RelativeTime relative(Clock clock, RelativeTime dest){
        if (dest == null)
            return relative(clock);
        else {
            dest.setDirect(this);
            dest.setClock(clock);
            return dest;
        }
    }

    /**
     * Create a new instance of <code>RelativeTime</code>
     * representing the result of subtracting <code>time</code> from 
     * the value of <code>this</code>
     * and normalizing the result.
     * The <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are expected to be the same, and such association
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
     * A new <code>RelativeTime</code> object whose time is
     * the normalization of
     * <code>this</code> minus the parameter <code>time</code>
     * parameter <code>time</code>.
     * @throws IllegalArgumentException Thrown if the
     * <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different, or when the
     * <code>time</code> parameter is <code>null</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */
    public RelativeTime subtract(RelativeTime time){
       return this.subtract(time, null);
    }

    /**
     * Return an object containing the value 
     * resulting from subtracting the value of <code>time</code> from 
     * the value of <code>this</code>
     * and normalizing the result.
     * If <code>dest</code> is not <code>null</code>, the result is placed
     * there and returned. Otherwise, a new object is allocated for the result.
     * The <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are expected to be the same, and such association
     * is used for the result.
     * The <code>clock</code>
     * associated with the <code>dest</code> parameter
     * is ignored.
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
     * @param dest If <code>dest</code> is not <code>null</code>,
     * the result is placed there and returned. Otherwise, a new
     * object is allocated for the result.
     *   @return  the result of the normalization of
     * <code>this</code> minus the <code>RelativeTime</code>
     * parameter <code>time</code> in
     * <code>dest</code> if <code>dest</code> is not <code>null</code>,
     * otherwise the result is returned in a newly allocated object.
     * @throws IllegalArgumentException Thrown if the
     * if the <code>clock</code> associated with
     * <code>this</code> and the <code>clock</code>
     * associated with the <code>time</code> parameter
     * are different, or when the
     * <code>time</code> parameter is <code>null</code>.
     * @throws ArithmeticException Thrown if there is an overflow in
     * the result after normalization.
     */
    public RelativeTime subtract(RelativeTime time, RelativeTime dest){
        if (time == null || time.clock != this.clock)
            throw new IllegalArgumentException("null arg or different clock");
        if (dest == null)
            dest = new RelativeTime(0, 0, this.clock);
        if (!dest.setNormalized(addSafe(this.milliseconds, -time.milliseconds),
                                ((long)this.nanoseconds) - time.nanoseconds) )
            throw new ArithmeticException("non-normalizable result");
        // note: don't change dest until we know no exceptions will occur
        dest.setClock(this.clock);
        return dest;
    }

	
    /**
     * 
     * Create a printable string of the time given by <code>this</code>.
     * <p>The string shall be a decimal reprepresentation of the milliseconds 
     * and nanosecond values; formatted as follows "(2251 ms, 750000 ns)"
     * 
     * @return String object converted from the
     * time given by <code>this</code>.
     */
    public String toString(){
        // although typically shorter than absolute times we may as well use 
        // the same format
        // ms since epoch needs 13 digits, 6 for nanos, 10 for rest
        return new StringBuffer(29).append("(").append(milliseconds).
            append(" ms, ").append(nanoseconds).append(" ns)").toString();
    }
    
}
