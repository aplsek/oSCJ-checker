import javax.realtime.Clock;
import javax.realtime.AbsoluteTime;

/**
 * Timer utilities. This class is used for measuring duration of benchmarking
 * procedures.
 * 
 * @author marekprochazka
 */
public class Timer {

    /** Initial time */
    private long startTime;
    /** Stored absolute time */
    private final AbsoluteTime at = new AbsoluteTime();

	/**
     * Default constructor. Initializes the init time.
     */
	public Timer( ) {
		initTimer( );
	}

	/**
	 * Timer initialization. Gets and stores initial absolute time
	 * @return absolute time in nanoseconds
	 */
	public long initTimer( ) {
		Clock.getRealtimeClock().getTime(at);
		return startTime = getNanoseconds(at);
	}
	/**
	 * Gets absolute time. Doesn't affect stored initial time.
	 * @return absolute time in nanoseconds.
	 */
	public long getAbsoluteTime( ) {
		Clock.getRealtimeClock().getTime(at);
		return getNanoseconds(at);
	}
	/**
	 * Gets relative time. Doesn't affect stored initial time.
	 * @return Relative time elapsed from the initial time.  
	 */
	public long getRelativeTime( ) {
		return (getAbsoluteTime() - startTime);
	}
	/**
	 * Converts <code>HighPrecisionTime</code> to nanoseconds.
	 * @param time to convert
	 * @return converted time in nanoseconds
	 */
	private long getNanoseconds(AbsoluteTime time) {
		return time.getNanoseconds() + time.getMilliseconds() * 1000000; 
	}
}
