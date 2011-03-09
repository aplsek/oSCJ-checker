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
package railsegment.clock;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.ClockCallBack;
import javax.realtime.RelativeTime;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.CURRENT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@SCJAllowed
@Scope("TM")
public class TrainClock extends Clock {

  // PROBLEM: static cannot point to an instance residing in scope "TM"
  // private static TrainClock instance;
  // private static SynchronizedTime times_data;

  // constructor is package access, to allow instantiation from
  // SynchronizedTime.
  @RunsIn(CURRENT)
  @SCJAllowed
  TrainClock(SynchronizedTime times_data) {
    // only allow one instantiation.
    if (times_data != null) {
      // todo: preallocate this exception
      throw new IllegalStateException();
    }
    times_data = times_data;
  }

  @RunsIn(CURRENT)
  void initialize() {
    // instance = this;
  }

  @RunsIn(CURRENT)              // Not allowed to override
  @SCJAllowed
  public AbsoluteTime getTime() {
    return null;
  }

  @RunsIn(UNKNOWN)
  @SCJAllowed
  public AbsoluteTime getPrivateTime() {
    return null;
  }

  @RunsIn(CURRENT)              // Not allowed to override
  @SCJAllowed
  public AbsoluteTime getTime(AbsoluteTime dest) {
    return null;
  }

  @RunsIn(UNKNOWN)
  @SCJAllowed
  public AbsoluteTime getPrivateTime(AbsoluteTime dest) {
    return null;
  }

  @RunsIn(CURRENT)
  @SCJAllowed
  public RelativeTime getResolution() {
    return null;
  }

  @RunsIn(CURRENT)
  @SCJAllowed
  public RelativeTime getEpochOffset() {
    return null;
  }

  @RunsIn(CURRENT)
  @SCJAllowed
  public final boolean drivesEvents() {
    return true;
  }

  ClockCallBack callback_event;
  AbsoluteTime callback_time;
  AbsoluteTime current_time;

  @RunsIn(CURRENT)
  public final synchronized
  void registerCallBack(@Scope(CURRENT) AbsoluteTime t,
                        @Scope(CURRENT) ClockCallBack clock_event) {
    callback_time.set(t.getMilliseconds(), t.getNanoseconds());
    callback_event = clock_event;
  }

  @SCJRestricted(maySelfSuspend = false)
  protected boolean resetTargetTime(AbsoluteTime time) {

    // skeleton implementation
    return false;
  }

  @RunsIn(CURRENT)
  // This is the tick.  It gets called 4 times per ms.  If there is
  // a pending callback, invoke the service
  synchronized void updateTime(AbsoluteTime new_time) {

    if (new_time.getClock() != this) {
      // todo: preallocate exception
      throw new IllegalArgumentException();
    }
    current_time.set(new_time.getMilliseconds(), new_time.getNanoseconds());

    if ((callback_event != null) &&
        (new_time.compareTo(callback_time) >= 0)) {
      ClockCallBack tmp = callback_event;
      callback_event = null;
      tmp.atTime(this);
    }
  }

  @RunsIn(CURRENT)
  final protected void setResolution(javax.realtime.RelativeTime resolution) {
    // todo: preallocate exception, maybe change which exception is thrown.
    throw new IllegalStateException();
  }

  @RunsIn(CURRENT)
  @SCJAllowed
  @SCJRestricted(mayAllocate = false, maySelfSuspend = false)
  public final RelativeTime getResolution(RelativeTime dest) {
    return null;
  }

}