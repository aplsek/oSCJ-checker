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

import javax.safetycritical.Mission;
import javax.safetycritical.Services;

import railsegment.clock.SynchronizedTime;
import railsegment.clock.TrainClock;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import static javax.safetycritical.annotate.Scope.CALLER;


@DefineScope(name="TM", parent=IMMORTAL)
@Scope("TM")  // Scope(IMMORTAL)
public class TrainMission extends Mission
{
  public final static long BackingStoreRequirements = 10000;
  public final static long NativeStackRequirements = 10000;
  public final static long JavaStackRequirements = 8000;

  private final static long MissionMemorySize = 1000;

  // These ceiling priorities enable communication between the
  // TrainControl mission and each of the service components,
  // representing communications server, synchronized time server, and
  // navigation server.  The ceiling priorities must be equal or
  // higher than the priorities of the threads running in the
  // respective missions.
  //
  //  Here's the rationale for priority assignments:
  //   global time synchronization, deadline 250 microseconds, priority 24
  //   comms "drivers", deadline 250 microseconds (to prevent hardware buffer
  //        overflows and to "serve" time synchronization), priority 24
  //   comms_responses (on behalf of global time synchronization),
  //        priority 24.  note that some of comms_responses are in
  //        behalf of train control, which has lower priority but it
  //        will inherit the highest priority of its clients.
  //
  //   timer_tick, deadline 500 microseconds, priority 20
  //
  //   gps_driver, deadline 1 ms, priority 16
  //
  //   train control, deadline 1 second, priority 8
  //   navigation_responses (on behalf of train control), inherit
  //        priority 8
  //   navigation_responses (on behalf of train control), inherit
  //        priority 8
  //

  // TODO
  // TODO
  // TODO
  // TODO: rectify priority assignments with above table
  // TODO
  // TODO
  // TODO


  public final int COMMS_CEILING = 16;
  public final int TIMES_CEILING = 24;
  public final int NAVS_CEILING = 16;

  public final int COMMS_PRIORITY = 16;
  public final int TIMES_PRIORITY = 24;
  public final int NAVS_PRIORITY = 16;
  public final int GPS_PRIORITY = 16;
  public final int CONTROL_PRIORITY = 16;

  private CommunicationsQueue comms_data;
  private SynchronizedTime times_data;
  private NavigationInfo navs_data;

  private CommunicationServiceSequencer commsq;
  private TimeServiceSequencer timesq;
  private NavigationServiceSequencer navsq;
  private TrainControlSequencer controlsq;

  @SCJRestricted(INITIALIZATION)
  public TrainMission() {
    // nothing much happens here
  }

  @Override
  public final long missionMemorySize()
  {
    // must be large enough to represent the three Schedulables
    // instantiated by the initialize() method
    return MissionMemorySize;
  }

  @Override
  @SCJRestricted(INITIALIZATION)
  public void initialize() {
    // it all happens here instead

    final int NUM_BUFFERS = 8;
    final int BUFFER_LENGTH = 1024;

    comms_data = new CommunicationsQueue(NUM_BUFFERS,
                                         BUFFER_LENGTH, COMMS_CEILING);
    comms_data.initialize();

    times_data = new SynchronizedTime(TIMES_CEILING);
    TrainClock train_clock = times_data.initialize();

    navs_data = new NavigationInfo(train_clock, NAVS_CEILING);
    navs_data.initialize();


    // Why don't I just instantiate LinearMissionSequencer instead of
    // these special sequencers?  Because I want the inner missions to
    // be allocated in their respective Mission memories, allowing them to
    // keep references to their inner entities.

    commsq = new CommunicationServiceSequencer(COMMS_PRIORITY, comms_data);
    navsq = new NavigationServiceSequencer(NAVS_PRIORITY, GPS_PRIORITY,
                                           train_clock, navs_data);
    timesq = new TimeServiceSequencer(TIMES_PRIORITY,
                                      comms_data, times_data, train_clock);

    controlsq = new TrainControlSequencer(CONTROL_PRIORITY,
                                          comms_data, times_data, navs_data);
    commsq.register();
    timesq.register();
    navsq.register();
    controlsq.register();
  }

  // no need to override the default implementation of cleanup, which
  // does nothing.

  @Override
  @RunsIn(CALLER)
  public void requestTermination()
  {
    commsq.requestSequenceTermination();
    timesq.requestSequenceTermination();
    navsq.requestSequenceTermination();
    controlsq.requestSequenceTermination();
    super.requestTermination();
  }
}
