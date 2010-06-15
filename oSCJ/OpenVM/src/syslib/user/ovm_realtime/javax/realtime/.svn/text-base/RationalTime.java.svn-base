/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/RationalTime.java,v 1.1 2004/10/15 01:53:11 dholmes Exp $
 */
package javax.realtime;

/**
 * An object that represents a time interval milliseconds/10<sup>3</sup> + 
 * nanoseconds/10<sup>9</sup> seconds long that
 * is divided into subintervals by some frequency. 
 * This is generally used in periodic events, threads, and feasibility 
 * analysis to specify periods where there is a basic
 * period that must be adhered to strictly (the interval), but within that 
 * interval the periodic events are supposed to happen frequency times, as 
 * uniformly spaced as possible, but clock and scheduling jitter is moderately 
 * acceptable.
 * 
 * <p>If the value of any of the millisecond or nanosecond fields is negative 
 * the variable is set to negative value. Although logically
 * this may represent time before the epoch, invalid results may occur if
 * an instance of {@link RationalTime} representing time before the epoch is 
 * given as a parameter to the a method.
 *
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded 
 * situations when it is being changed. No synchronization is done. 
 * It is assumed that users of this class who are mutating instances will 
 * be doing their own synchronization at a higher level.
 * <p>
 * All Implemented Interfaces: java.lang.Comparable
 *
 * <h3>OVM Implementation Notes</h3>
 * <p>The deprecated state of this class means it is effectively unsupported.
 * There are no clear meaningful semantics for many methods in many cases so
 * we try to do the least surprising thing, but really you should just never 
 * use this class - EVER!
 * <p>Methods in other classes that are passed a RationalTime object will
 * treat it as a RelativeTime object and ignore the frequency.
 *
 * @deprecated As of RTSJ 1.0.1
 *
 * @spec RTSJ 1.0.1
 */
public class RationalTime extends RelativeTime {

    int freq;
		
    /**
     * Construct an instance of <code>RationalTime</code>.
     * Equivalent to new <code>RationalTime(1000, 0, frequency)</code> - 
     * essentially a cycles-per-second value.
     * 
     * @deprecated 1.0.1
     */
    public RationalTime(int frequency){
        this(frequency, 1000, 0);
    }

    /**
     * Construct an instance of <code>RationalTime</code>.
     * All arguments must be greater than or equal to zero.
     *
     * @param  frequency The frequency value.
     *
     * @param  millis The milliseconds value.
     *
     * @param  nanos The nanoseconds value.
     *
     * @throws IllegalArgumentException If any of the argument values are 
     * less than zero.
     * 
     * @deprecated 1.0.1
     */
    public RationalTime(int frequency, long millis, int nanos) {
        super(millis, nanos);
        if (frequency < 0 || millis < 0 || nanos < 0)
            throw new IllegalArgumentException("value < 0");
        this.freq = frequency;
    }

    /**
     * Construct an instance of <code>RationalTime</code> from the given 
     * {@link RelativeTime}.
     *
     * @param frequency The frequency value.
     *
     * @param interval The given instance of {@link RelativeTime}.
     *
     * @throws IllegalArgumentException If any of the argument values are 
     * less than zero.
     * 
     * @deprecated 1.0.1
     */
    public RationalTime(int frequency, RelativeTime interval)  {
        this(frequency, interval.milliseconds, interval.nanoseconds);
    }

    /** 
     * Convert time of <code>this</code> to an absolute time. 
     *
     * @param clock The reference clock. 
     * If null, <code>Clock.getRealTimeClock()</code> is used.
     *
     * @param destination A reference to the destination instance.
     * 
     * @deprecated 1.0.1
     */
    public AbsoluteTime absolute(Clock clock, 
				 AbsoluteTime destination){
       return super.absolute(clock, destination);
    }

    /**
     * Add the time of <code>this</code> to an {@link AbsoluteTime} 
     *
     * It is almost the same <code>dest.add(this, dest)</code> except
     * that it accounts for (i.e. divides by) the frequency.
     * 
     * @deprecated 1.0.1
     *
     * @param destination A reference to the destination instance.
     */
    public void addInterarrivalTo(AbsoluteTime destination){
        destination.add(this.milliseconds/freq, this.nanoseconds/freq);
    }

    /**
     * Gets the value of <code>frequency</code>.
     *
     * @return The value of <code>frequency</code> as an integer.
     * 
     * @deprecated 1.0.1
     */
    public int getFrequency(){
       return freq;
    }

    /**
     * Gets the interarrival time. 
     * This time is (milliseconds/10<sup>3</sup> + nanoseconds/10<sup>9</sup>)
     * /frequency rounded down to the nearest expressible value of the fields 
     * and their types of {@link RelativeTime}.
     *
     *
     * @deprecated 1.0.1
     */
    public RelativeTime getInterarrivalTime(){
        return new RelativeTime(this.milliseconds/freq, this.nanoseconds/freq);
    }
	
    /**
     * Gets the interarrival time. 
     * This time is (milliseconds/10<sup>3</sup> + nanoseconds/10<sup>9</sup>)
     * /frequency rounded down to the nearest expressible value of the fields 
     * and their types of {@link RelativeTime}.
     *
     *  @param dest Result is stored in dest and returned, if null, a new object is returned.
     * 
     * @deprecated 1.0.1
     */
    public RelativeTime getInterarrivalTime(RelativeTime dest){
        if (dest == null) return getInterarrivalTime();
        dest.setDirect(this.milliseconds/freq, this.nanoseconds/freq);
        return dest;
    }

    /**
     * Sets the indicated fields to the given values.
     *
     * @param millis The new value for the millisecond field.
     *
     * @param nanos The new value for the nanosecond field.
     * 
     * @deprecated 1.0.1
     */
    public void set(long millis, 
		    int nanos)  {
        super.set(millis, nanos);
    }

    /**
     * Sets the value of the <code>frequency</code> field.
     *
     * @param frequency The new value for the <code>frequency</code>.
     * 
     * @deprecated 1.0.1
     */
    public void setFrequency(int frequency)  {
        if (frequency < 0) throw new IllegalArgumentException("value < 0");
        freq = frequency;
    }
    
}

