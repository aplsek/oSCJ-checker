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

import javax.realtime.PriorityParameters;

import javax.safetycritical.NoHeapRealtimeThread;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import railsegment.clock.SynchronizedTime;
import static javax.safetycritical.annotate.Scope.CALLER;


@Scope("B")
@DefineScope(name="B:TCT", parent="B")
public class TrainControlThread extends NoHeapRealtimeThread
{
  private final static int BackingStoreSize = 1000;
  private final static int NativeStackSize = 1000;
  private final static int JavaStackSize = 1000;

  private final CommunicationsQueue comms_data;
  private final SynchronizedTime times_data;
  private final NavigationInfo navs_data;

  public TrainControlThread(final CommunicationsQueue comms_data,
                            final SynchronizedTime times_data,
                            final NavigationInfo navs_data,
                            final int priority)
  {
    super(new PriorityParameters(priority),
          new StorageParameters(BackingStoreSize,
                                NativeStackSize, JavaStackSize));

    this.comms_data = comms_data;
    this.times_data = times_data;
    this.navs_data = navs_data;
  }

  private boolean shutting_down = false;

  @RunsIn(CALLER)
  public synchronized void requestTermination() {
    shutting_down = true;
  }

  @RunsIn(CALLER)
  private synchronized boolean terminationRequested() {
    return shutting_down;
  }

  @Override
  @RunsIn("B:TCT")
  public void run()
  {
    // my goal is to run this loop once every half mile.  if I
    // fall behind this schedule, I need to decrease my speed in
    // order to restore the schedule.

    while (true) {
      if (shutting_down) {
        break;
      }

      // get current position
      //   read: navs_data


      // have new messages from central dispatch arrived for me?  If
      // so, open the messages and update my status.  (probably new
      // authorizations or authorization renewals)
      //   read: comms_data

      // how far have i traveled since last time through this loop?
      // if more than .5 mile, decrease speed and request "load
      // shedding" for next N iterations.

      // how long has it been since last time through this loop?
      // if more than 1/2 s, decrease speed and request "load
      // shedding" for next N iterations
      //   read: times_data


      // confirm that my current speed is no greater than the speed
      // limit for current position and future one minute of track.
      // if current or upcoming speed limits require decreasing speed,
      // make train speed adjustments now (it takes a while to slow
      // this beast).
      //   read: navs_data (to get current speed)


      // if i have entered a new rail segment since the last time
      // through this loop, notify central dispatch that I now occupy
      // these new rail segments.
      //   write: comms_data


      // if not load shedding
      //   calculate position of caboose
      //   if caboose has departed any previously authorized regions,
      //   relinquish the authorization so that it can be granted to
      //   another train.
      //     read: navs_data

      // if any of my current rail-segment authorizations have expired,
      // relinquish them.  For each relinquished rail-segment
      // authorization, notify central dispatch to request a
      // renewal...
      //  read: times_data
      //  write: comms_data

      // calculate how far do my current rail-segment authorizations
      // take me.  If they take me less than 10 minutes into the
      // future, issue requests for the next rail segments (to give me
      // at least 10 minutes of future authorization). For any rail
      // segment that spans a track switch, make sure the
      // authorization request identifies the desired switch position.
      //  read: navs_data (speed, authorization range)
      //  write: comms_data

      // if not load shedding:
      //   check my eta vs. scheduled arrival for next milestone.
      //   adjust speed if appropriate (speed up if i'm behind schedule and
      //   within speed limit; slow down if i'm ahead of schedule)
      //     read local data to get eta
      //     read: navs_data to get scheduled arrival

      // check whether i have at least 3 minutes of future
      // rail-segment authorization.  if not, apply the brakes to stop
      // asap.  (it may take me three minutes to bring this big rig to
      // a stop.)
      //  read: navs_data (current speed, speed limits, range of
      //                   future authorizations)

    }
  }
}
