/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */
package javax.realtime;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;


import edu.purdue.scj.VMSupport;

@SCJAllowed
public abstract class Clock {

	/** The RealtimeClock subclass */
	static class RealtimeClock extends Clock {

		/** The resolution of this clock */
		static RelativeTime resolution = null;

		/** No construction allowed */
		private RealtimeClock() {
		}

		/**
		 * Utility method for other javax.realtime classes to read the current
		 * time in nanoseconds.
		 */
		static long getCurrentTimeNanos() {
			return VMSupport.getCurrentTime();
		}

		static long getResolutionNanos() {
			return resolution.toNanos();
		}

		/** Initialize the RTC instance */
		private static RealtimeClock instance() {
			long nanosR = VMSupport.getClockResolution();
			long millis = nanosR / HighResolutionTime.NANOS_PER_MILLI;
			int nanos = (int) (nanosR % HighResolutionTime.NANOS_PER_MILLI);
			RealtimeClock c = new RealtimeClock();
			resolution = new RelativeTime(millis, nanos, c);
			return c;
		}

		@Override
		public RelativeTime getResolution() {
			return new RelativeTime(resolution); // defensive copy
		}

		@Override
		public AbsoluteTime getTime() {
			return getTime(new AbsoluteTime(0, 0, this));
		}

		@Override
		public void setResolution(RelativeTime resolution) {
			throw new UnsupportedOperationException();
		}

		public AbsoluteTime getTime(AbsoluteTime time) {
			if (time != null) {
				long nanos = getCurrentTimeNanos();
				long millis = nanos / HighResolutionTime.NANOS_PER_MILLI;
				nanos = (nanos % HighResolutionTime.NANOS_PER_MILLI);
				time.setDirect(millis, (int) nanos);
				time.setClock(this);
			}
			return time;
		}

		/**
		 * Spec 0.73 says: the relative time of the offset of the epoch of this
		 * clock from the Epoch. For the real-time clock it will return a
		 * RelativeTime value equal to 0. A newly allocated RelativeTime object
		 * in the current execution context with the offset past the Epoch for
		 * this clock.
		 * 
		 * The returned object is associated with this clock. -
		 * epoch.setClock(this);
		 * 
		 * @return For the real-time clock it will return a RelativeTime value
		 *         equal to 0.
		 * 
		 */
		public RelativeTime getEpochOffset() {
			RelativeTime epoch = new RelativeTime(0, 0);
			epoch.setClock(this);
			return epoch;
		}

		/**
		 * At Level 0, we do not trigger the eecution of events. Therefore, this
		 * clock is not able to trigger anything. --> we can say that this clock
		 * is "read-only".
		 * 
		 * @return true if and only if this Clock is able to trigger the
		 *         execution of time-driven activities. (Spec 0.73)
		 * 
		 */
		@Override
		@SCJAllowed
		protected boolean drivesEvents() {
			return false;
		}

		@Override
		@SCJAllowed(LEVEL_1)
		protected void registerCallBack(AbsoluteTime time,
				ClockCallBack clockEvent) {
			// this should not be called at Level 0
		}

		@Override
		@SCJAllowed(LEVEL_1)
		protected boolean resetTargetTime(AbsoluteTime time) {
			// this should not be called at Level 0
			return false;
		}

		@Override
		public RelativeTime getResolution(RelativeTime time) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	/** Our real-time clock instance - package accessible for convenience */
	static final RealtimeClock rtc = RealtimeClock.instance();

	@SCJAllowed
	public Clock() {
	}

	@SCJAllowed
	@SCJRestricted()
	public static Clock getRealtimeClock() {
		return rtc;
	}

	/** Returns previously allocated resolution object, which is immutable. */
	@SCJAllowed
	@SCJRestricted()
	public abstract RelativeTime getResolution();

	@SCJAllowed
	@SCJRestricted()
	public abstract RelativeTime getResolution(RelativeTime time);
	
	@SCJAllowed
	@SCJRestricted()
	public abstract AbsoluteTime getTime();
	
	@SCJAllowed
	@SCJRestricted()
	public abstract AbsoluteTime getTime(AbsoluteTime time);

	/**
	 * Returns true if and only if this Clock is able to trigger the execution
	 * of time-driven activities.
	 * 
	 * @return
	 */
	@SCJAllowed
	@SCJRestricted()
	protected abstract boolean drivesEvents();

	/**
	 * Code in the abstract base Clock class makes this call to the subclass.
	 * 
	 * @param time
	 * @param clockEvent
	 */
	@SCJAllowed(LEVEL_1)
	@SCJRestricted()
	protected abstract void registerCallBack(AbsoluteTime time,
			ClockCallBack clockEvent);

	/**
	 * Replace the target time being used by the ClockCallBack registered by
	 * registerCallBack(AbsoluteTime, ClockCallBack).
	 * 
	 * @param time
	 * @return
	 */
	@SCJAllowed(LEVEL_1)
	@SCJRestricted()
	protected abstract boolean resetTargetTime(AbsoluteTime time);

	/**
	 * Set the resolution of this. TBD: do we keep this in SCJ?
	 * 
	 * @param resolution
	 */
	@SCJAllowed(LEVEL_1)
	protected abstract void setResolution(javax.realtime.RelativeTime resolution);
	
	
	@SCJAllowed
	@SCJRestricted()
	public abstract RelativeTime getEpochOffset();
}
