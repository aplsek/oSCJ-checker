package javax.realtime;

/**
 * A clock marks the passing of time.
 * It has a concept of now that can be queried through
 * <code>Clock.getTime()</code>, and it can have events
 * queued on it which will be fired when their appointed time is reached.
 * 
 * 
 * @spec RTSJ 1.0.1
 */
public abstract class Clock{

    /**
     * There is always at least one clock object available:
     * a real-time clock that advances in sync
     * with the external world. This is the default <code>Clock</code>.
     *
     * @return The singleton instance of the default <code>Clock</code>
     */
    // @specbug: remove statement re advancing in sync with the external world
    public static Clock getRealtimeClock(){
       return rtc;
    }

    /**
     * Constructor for the abstract class.
     */
    protected Clock(){
    }
    
    /**
     * Returns the relative time of the offset of the epoch of
     * <code>this</code> clock from the Epoch.
     * For the real-time clock it will return a <tt>RelativeTime</tt> 
     * value equal to 0.
     * <p>
     * An <code>UnsupportedOperationException</code> is thrown if the clock
     * does not support the concept of date.

     * @return A newly allocated {@link RelativeTime}
     * object in the current execution context with the offset past
     * the Epoch for <code>this</code> clock.
     * The returned object is associated with <code>this</code> clock.
     * 
     * @throws UnsupportedOperationException Thrown if the
     * clock does not have the concept of date.
     * 
     * @since 1.0.1
     */
    public abstract RelativeTime getEpochOffset();


    /**
     * Gets the resolution of the clock, the nominal interval between ticks.
     *
     * @return A newly allocated {@link RelativeTime} object in the
     * current execution context representing
     * the resolution of <code>this</code>.
     *
     * The returned object is associated with <code>this</code> clock.
     */
    public abstract RelativeTime getResolution();

    /**
     * Gets the current time in a newly allocated object.
     * <p>
     * <em>Note:</em> This method will return an absolute time value
     * that represents the clock's notion of an absolute time.  For clocks
     * that do not measure calendar time this absolute time may not represent
     * a wall clock time.
     *
     * @return  A newly allocated instance of {@link AbsoluteTime} in the
     * current allocation context, representing the current time.
     * The returned object is associated with <code>this</code> clock.
     */
    public abstract AbsoluteTime getTime();

    /**
     * Gets the current time in an existing object. The time represented by
     * the given {@link AbsoluteTime} is changed at some time between the
     * invocation of the method and the return of the method.
     *
     * <p><em>Note:</em> This method will return an absolute time value
     * that represents the clock's notion of an absolute time.  For clocks
     * that do not measure calendar time this absolute time may not represent
     * a wall clock time.
     *
     * @param time The instance of {@link AbsoluteTime} object
     * which will be updated in place.
     * The clock association of the <code>time</code> parameter
     * is ignored.
     * When <code>time</code> is not <code>null</code> the returned object
     * is associated with <code>this</code> clock.
     * If <code>time</code> is <code>null</code>, then nothing happens.
     *
     * @return  The instance of {@link AbsoluteTime} passed as parameter,
     * representing the current time, associated with <code>this</code>
     * clock, or <code>null</code> if <code>time</code> was <code>null</code>.
     *
     * @since 1.0.1 The return value is updated from <code>void</code> to
     * <code>AbsoluteTime.</code>
     */
    public abstract AbsoluteTime getTime(AbsoluteTime time);

    /**
     * Set the resolution of <code>this</code>.
     * For some hardware clocks setting resolution is
     * impossible and if this method is called on those clocks,
     * then an <code>UnsupportedOperationException</code> is thrown.
     *
     * @param resolution The new resolution of <code>this</code>.
     * The clock association of the <code>resolution</code> parameter
     * is ignored.
     *
     * @throws IllegalArgumentException Thrown if <code>resolution</code>
     * represents an interval less than or equal to zero.
     * @throws UnsupportedOperationException Thrown if the
     * clock does not support setting its resolution.
     */
    public abstract void setResolution(RelativeTime resolution);


    /**
     * Our real-time clock instance - package accessible for convenience
     */
    static final RealtimeClock rtc = RealtimeClock.instance();

    /**
     * The RealtimeClock subclass
     */
    static class RealtimeClock extends Clock {
        
        /** The resolution of this clock */
        static RelativeTime resolution = null;


        /** Utility method for other javax.realtime classes to read the
            current time in nanoseconds.
        */
        static long getCurrentTimeNanos() {
            return LibraryImports.getCurrentTime();
        }

        static long getResolutionNanos() {
            return resolution.toNanos();
        }

        /** Initialize the RTC instance */
        private static RealtimeClock instance() {
            long nanos = LibraryImports.getClockResolution();
            if (nanos == -1) { // shouldn't be possible 
                throw new Error("getClockResolution failed");
            }
            long millis = nanos / HighResolutionTime.NANOS_PER_MILLI;
            int nanosI = (int)(nanos % HighResolutionTime.NANOS_PER_MILLI);
            RealtimeClock c = new RealtimeClock();
            resolution = new RelativeTime(millis, nanosI, c);
            return c;
        }

        /** No construction allowed */
        private RealtimeClock() {}

        public RelativeTime getResolution() {
            return new RelativeTime(resolution); // defensive copy
        }

        public AbsoluteTime getTime() {
            return getTime(new AbsoluteTime(0, 0, this));
        }
        
        public AbsoluteTime getTime(AbsoluteTime time) {
            if (time != null) {
                long nanos = getCurrentTimeNanos();
                long millis = nanos / HighResolutionTime.NANOS_PER_MILLI;
                nanos =  (nanos % HighResolutionTime.NANOS_PER_MILLI);
                time.setDirect(millis, (int)nanos);
                time.setClock(this);
            }
            return time;
        }
        
        public void setResolution(RelativeTime resolution) {
            throw new UnsupportedOperationException();
        }

        public RelativeTime getEpochOffset() {
            return new RelativeTime(0,0);
        }
    }

}


