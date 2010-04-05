package edu.purdue.scjtck.bench.util;

import javax.realtime.Clock;
import javax.realtime.HighResolutionTime;

public class Time {
	// TODO: refer to Suramadu
	// public static native long getCycles();
//
//	static {
//		System.loadLibrary("S3UtilTime");
//	}


	public static native long getNanosecondsNative();

	/**
	 * Convert a HighResolutionTime object to a long in nanosecond units.
	 * 
	 * @return a long representing some amount of time in nanoseconds
	 */
	public static long timeToLong(HighResolutionTime time) {
		return time.getMilliseconds() * 1000000 + time.getNanoseconds();
	}

	/**
	 * Convert the current time to a long in nanosecond units.
	 * 
	 * @return a long representing the current time in nanoseconds
	 */
	public static long currentTimeToLong() {
		Clock clock = Clock.getRealtimeClock();
		return timeToLong(clock.getTime());
	}
}
