/**
 *  Name: Railsegment
 *  Author : Kelvin Nilsen, <kelvin.nilsen@atego.com>
 *
 *  Copyright (C) 2011  Kelvin Nilsen
 *
 *  Railsegment is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  Railsegment is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with Railsegment; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package railsegment;

import railsegment.clock.TrainClock;

import javax.realtime.AbsoluteTime;

import javax.safetycritical.Services;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import static javax.safetycritical.annotate.Scope.CALLER;

// This assumes there is at most one client for NavigationInfo, and
// that the single client always waits for a response to a previously
// issued request before issuing another request.

@Scope("TM")
public class NavigationInfo {
  static enum RequestEncoding {
    NoRequest, ResponseReady,
    CurrentSpeed, CurrentPosition,
    SegmentLength, SegmentSpeed, SegmentSwitch,
    NextTrackSegment, PrevTrackSegment,
    NextStop
    };

  private RequestEncoding pending_request;

  // Purdue SCJ: This ought to work, right?
  private int int_argument;
  @Scope("TM") private AbsoluteTime time1_argument;
  @Scope("TM") private AbsoluteTime time2_argument;

  private int int_response;
  private long long_response;

  private final int CEILING_PRIORITY;

  private final TrainClock train_clock;

  NavigationInfo(TrainClock train_clock, int ceiling) {
    CEILING_PRIORITY = ceiling;
    this.train_clock = train_clock;
    pending_request = RequestEncoding.NoRequest;
    time1_argument = new AbsoluteTime(0L, 0, train_clock);
    time2_argument = new AbsoluteTime(0L, 0, train_clock);
  }

  void initialize() {
    Services.setCeiling(this, CEILING_PRIORITY);
  }


  /**
   * Returns speed as measured in tenths of a mile per hour.
   *
   * Updates time_stamp to reflect the time at which the current speed
   * was calculated.
   */
  @RunsIn(CALLER)
  public synchronized int getCurrentSpeed(@Scope(UNKNOWN)
                                          AbsoluteTime time_stamp) {
    pending_request = RequestEncoding.CurrentSpeed;
    notifyAll();
    while (pending_request != RequestEncoding.ResponseReady) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }
    pending_request = RequestEncoding.NoRequest;
    return int_response;
  }

  /**
   * Returns current track segment in high-order 32 bits, and forward
   * progress within current track segment, measured in feet, in
   * low-order 32 bits.
   *
   * Updates time_stamp to reflect the time at which this returned current
   * position was sampled.
   */
  @RunsIn(CALLER)
  public synchronized long getCurrentPosition(@Scope(UNKNOWN)
                                              AbsoluteTime time_stamp) {
    // mimic the behavior of getCurrentSpeed to exchange request and
    // response with the NavigationOversight thread.

    return 0L;
  }

  @RunsIn(CALLER)
  public synchronized int trackSegmentLength(int segment_no) {
    // mimic the behavior of getCurrentSpeed to exchange request and
    // response with the NavigationOversight thread.

    return 0;
  }

  /**
   * Return speed limit for track segment, in tenths of mile per hour.
   *
   * Assume the entire track segment has the same speed limit.
   */
  @RunsIn(CALLER)
  public synchronized int trackSegmentSpeed(int segment_no) {
    // mimic the behavior of getCurrentSpeed to exchange request and
    // response with the NavigationOversight thread.

    return 0;
  }

  /**
   * Assume each track segment has no more than one two-way switch.
   * The returned value is one of:
   *   0: no switch in this track segment
   *  -1: switch to the left
   *   1: switch to the right
   */
  @RunsIn(CALLER)
  public synchronized int trackSegmentSwitch(int segment_no) {
    // mimic the behavior of getCurrentSpeed to exchange request and
    // response with the NavigationOversight thread.

    return 0;
  }

  @RunsIn(CALLER)
  public synchronized int nextSegment(int segment_no) {
    // mimic the behavior of getCurrentSpeed to exchange request and
    // response with the NavigationOversight thread.

    return 0;
  }

  @RunsIn(CALLER)
  public synchronized int prevSegment(int segment_no) {
    // mimic the behavior of getCurrentSpeed to exchange request and
    // response with the NavigationOversight thread.

    return 0;
  }

  /**
   * Returns the track segment number in high-order 32 bits, the offset
   * in meters within that track segment within low-order 32 bits.
   * Updates timestamp to reflect time of report.  Updates schedule to
   * reflect scheduled time of arrival.
   */
  @RunsIn(CALLER)
  public synchronized long nextStop(@Scope(UNKNOWN) AbsoluteTime time_stamp,
                                    @Scope(UNKNOWN) AbsoluteTime schedule) {
    // mimic the behavior of getCurrentSpeed to exchange request and
    // response with the NavigationOversight thread.

    return 0L;
  }

  // invoked by the NavigationOversight thread
  @RunsIn(CALLER)
  synchronized RequestEncoding awaitRequest() {
    while (pending_request == RequestEncoding.NoRequest) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }
    return pending_request;
  }

  @RunsIn(CALLER)
  private final void overwriteTime1(long millis, int nanos) {
    time1_argument.set(millis, nanos);
  }

  @RunsIn(CALLER)
  private final void overwriteTime2(long millis, int nanos) {
    time2_argument.set(millis, nanos);
  }

  // invoked by the NavigationOversight thread
  @RunsIn(CALLER)
  synchronized void serviceRequest(int result) {
    int_response = result;
    pending_request = RequestEncoding.ResponseReady;
    notifyAll();
  }

  // invoked by the NavigationOversight thread
  @RunsIn(CALLER)
  synchronized void serviceRequest(long result) {
    long_response = result;
    pending_request = RequestEncoding.ResponseReady;
    notifyAll();
  }
}

