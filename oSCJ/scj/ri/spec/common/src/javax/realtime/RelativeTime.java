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

import javax.safetycritical.annotate.SCJAllowed;


import javax.safetycritical.annotate.SCJRestricted;

/**
 * An object that represents a time interval milliseconds/103 + nanoseconds/109
 * seconds long. It generally is used to represent a time relative to now.
 * 
 * @author plsek
 * 
 */
@SCJAllowed
public class RelativeTime extends HighResolutionTime {

	/** Note: immutable */
	public RelativeTime() {
		this(0, 0);
	}

	@SCJAllowed
	public RelativeTime(long millis, int nanos) {
		super(millis, (long) nanos, null);
	}

	@SCJAllowed
	public RelativeTime(RelativeTime time) {
		this(getMillisNonNull(time), time._nanoseconds);
	}

	RelativeTime(long millis, int nanos, Clock clock) {
		super(millis, (long) nanos, clock);
	}

	@SCJAllowed
	@SCJRestricted()
	public RelativeTime add(long millis, int nanos) {
		return (RelativeTime) super.add(millis, nanos, new RelativeTime(0, 0,
				_clock));
	}

	@SCJAllowed
	@SCJRestricted()
	public RelativeTime add(RelativeTime time) {
		if (time == null || time._clock != _clock)
			throw new IllegalArgumentException("null arg or different clock");

		return add(time._milliseconds, time._nanoseconds);
	}

	/**
	 * Note: it is not "safe" to automatically convert from one clock basis to
	 * another.
	 */
	public RelativeTime relative(Clock clock) {
		return new RelativeTime(_milliseconds, _nanoseconds, clock);
	}

	@SCJAllowed
	@SCJRestricted()
	public RelativeTime subtract(RelativeTime time) {
		if (time == null || time._clock != _clock)
			throw new IllegalArgumentException("null arg or different clock");

		return add(-time._milliseconds, -time._nanoseconds);
	}

	private static long getMillisNonNull(RelativeTime time) {
		if (time == null)
			throw new IllegalArgumentException("null parameter");
		return time._milliseconds;
	}
}
