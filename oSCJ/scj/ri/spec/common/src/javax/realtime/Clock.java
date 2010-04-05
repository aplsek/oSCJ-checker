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

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

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

        AbsoluteTime getTime(AbsoluteTime time) {
            if (time != null) {
                long nanos = getCurrentTimeNanos();
                long millis = nanos / HighResolutionTime.NANOS_PER_MILLI;
                nanos = (nanos % HighResolutionTime.NANOS_PER_MILLI);
                time.setDirect(millis, (int) nanos);
                time.setClock(this);
            }
            return time;
        }

        RelativeTime getEpochOffset() {
            // TODO: revise
            return new RelativeTime(0, 0);
        }

        @Override
        protected boolean drivesEvents() {
            // TODO: implement this
            return false;
        }

        @Override
        protected void registerCallBack(AbsoluteTime time,
                ClockCallBack clockEvent) {
            // TODO: implement this
        }

        @Override
        protected boolean resetTargetTime(AbsoluteTime time) {
            // TODO: implement this
            return false;
        }
    }

    /** Our real-time clock instance - package accessible for convenience */
    static final RealtimeClock rtc = RealtimeClock.instance();

    @SCJAllowed
    public Clock() {
    }

    @SCJAllowed
    public static Clock getRealtimeClock() {
        return rtc;
    }

    /** Returns previously allocated resolution object, which is immutable. */
    @SCJAllowed
    public abstract RelativeTime getResolution();

    @SCJAllowed
    public abstract AbsoluteTime getTime();

    /**
     * Returns true if and only if this Clock is able to trigger the execution
     * of time-driven activities.
     * 
     * @return
     */
    @SCJAllowed
    protected abstract boolean drivesEvents();

    /**
     * Code in the abstract base Clock class makes this call to the subclass.
     * 
     * @param time
     * @param clockEvent
     */
    @SCJAllowed(Level.LEVEL_1)
    protected abstract void registerCallBack(javax.realtime.AbsoluteTime time,
            ClockCallBack clockEvent);

    /**
     * Replace the target time being used by the ClockCallBack registered by
     * registerCallBack(AbsoluteTime, ClockCallBack).
     * 
     * @param time
     * @return
     */
    @SCJAllowed(Level.LEVEL_1)
    protected abstract boolean resetTargetTime(javax.realtime.AbsoluteTime time);

    /**
     * Set the resolution of this. TBD: do we keep this in SCJ?
     * 
     * @param resolution
     */
    @SCJAllowed(Level.LEVEL_1)
    protected abstract void setResolution(javax.realtime.RelativeTime resolution);
}
