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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package railsegment;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import railsegment.clock.SynchronizedTime;
import railsegment.clock.TrainClock;


@Scope("TM")
@SCJAllowed(value=LEVEL_2, members=true)
public class TrainMission extends Mission
{
  public final static long BackingStoreRequirements = 1000;
  public final static long StackRequirements = 16000;

  private final static long MissionMemorySize = 1000;

  // These ceiling priorities enable communication between the
  // TrainControl mission and each of the service components,
  // representing communications server, synchronized time server, and
  // navigation server.  The ceiling priorities must be equal or
  // higher than the priorities of the threads running in the
  // respective missions.
  //
  //  Here's the rationale for priority assignments:
  //
  //   TrainControl mission, deadline 1 second, priority 8
  //     Requires services provided by clock.TimeService,
  //       NavigationService, and CommunicationService missions
  //
  //   TimeService mission implements a user-defined clock
  //   implementation of globally synchronized time.
  //     An internal tick driver has deadline 250 microseconds,
  //       running at priority 22
  //     Additionally, there is a thread that implements global time
  //       synchronization to within +/- 10 ms.  Assume this
  //       thread has "responsiveness" deadlines of 2 ms, running at
  //       priority 18
  //       This thread requires services provided by the
  //         CommunicationService mission
  //
  //   NavigationService implements a GPS device driver, and maintains a
  //   data base of map data.  The map is presumed to be statically
  //   loaded prior to the start of a journey.  No communications
  //   services are required.
  //     The GPS device driver has a 1 ms response deadline, running
  //       at priority 20
  //     A server thread provides responses to the
  //       TrainControlMission, running at "inherited" priority 8
  //
  //   CommunicationService implements possibly redundant
  //   telecommunication services to support exchange of information
  //   with central dispatch and possibly with other trains.
  //     Assume that the underlying communication hardware requires
  //       interrupt-driven device drivers with 50 microsecond response time
  //       requirements, running at interrupt level priority 28
  //     A server thread provides responses to the TrainControl
  //       mission, running at inherited priority 8
  //     A second server thread provides responses to the TimeService
  //       mission, running at inherited priority 18

  public final int CONTROL_PRIORITY = 8;

  public final int TIMES_TICK_PRIORITY = 22;
  public final int TIMES_COORDINATION_PRIORITY = 18;

  // The maximum of TIMES_TICK_PRIORITY and TIMES_COORDINATION_PRIORITY
  public final int TIMES_CEILING = TIMES_TICK_PRIORITY;

  public final int NAVS_GPS_PRIORITY = 20;
  public final int NAVS_SERVER_PRIORITY = CONTROL_PRIORITY;

  // The maximum of NAVS_GPS_PRIORITY and NAVS_SERVER_PRIORITY
  public final int NAVS_CEILING = NAVS_SERVER_PRIORITY;

  public final int COMMS_DRIVER_PRIORITY = 28;
  public final int COMMS_CONTROL_SERVER_PRIORITY = CONTROL_PRIORITY;
  public final int COMMS_TIMES_SERVER_PRIORITY = TIMES_COORDINATION_PRIORITY;

  public final int COMMS_CONTROL_CEILING = COMMS_CONTROL_SERVER_PRIORITY;
  public final int COMMS_TIMES_CEILING = COMMS_TIMES_SERVER_PRIORITY;
  
  // comms_control_data provides buffers for communication between the
  // COMMS thread and the CONTROL thread.  COMMS provides a service to
  // the TrainControl algorithm, for the purpose of communicating with
  // central dispatch.
  private CommunicationsQueue comms_control_data;

  // comms_control_data provides buffers for communication between the
  // COMMS thread and the TIMES_DRIVER thread.  COMMS provides a
  // service to the TimeServer, for the purpose of global time
  // synchronization.
  private CommunicationsQueue comms_times_data;

  // times_data provides buffers for communication between the
  // TimeServices tick driver and the implementation of TrainClock.
  // Note that the methods of SynchronizedTime are visible only
  // within the railsegment.clock package.
  //
  // Application code that desires to coordinate with the globally
  // synchronized time does so by interacting directly with the
  // train_clock object.
  private SynchronizedTime times_data;

  // navs_data provides buffers for communication between the
  // NAVS_SERVER and the CONTROL thread.
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
  @SCJAllowed
  public final long missionMemorySize()
  {
    // must be large enough to represent the three Schedulables
    // instantiated by the initialize() method
    return MissionMemorySize;
  }

  @Override
  @SCJRestricted(INITIALIZATION)
  @SCJAllowed(SUPPORT)
  public void initialize() {
    // it all happens here instead

    final int NUM_BUFFERS = 8;
    final int BUFFER_LENGTH = 1024;

    // CommunicationsQueue provides buffers for communication between
    // the COMMS_CONTROL_SERVER thread and the CONTROL thread
    comms_control_data = new CommunicationsQueue(NUM_BUFFERS,
                                                 BUFFER_LENGTH,
                                                 COMMS_CONTROL_CEILING);
    comms_control_data.initialize();

    comms_times_data = new CommunicationsQueue(NUM_BUFFERS,
                                               BUFFER_LENGTH,
                                               COMMS_TIMES_CEILING);
    comms_control_data.initialize();

    times_data = new SynchronizedTime(TIMES_CEILING);
    TrainClock train_clock = times_data.initialize();

    navs_data = new NavigationInfo(train_clock, NAVS_CEILING);
    navs_data.initialize();

    // The reason we don't just instantiate LinearMissionSequencer instead of
    // these special sequencers is because we want the inner missions to
    // be allocated in their respective Mission memories, allowing them to
    // keep references to their inner entities.

    // note that I need two comms_data servers...

    commsq = new CommunicationServiceSequencer(comms_control_data,
                                               comms_times_data,
                                               COMMS_DRIVER_PRIORITY,
                                               COMMS_CONTROL_SERVER_PRIORITY,
                                               COMMS_TIMES_SERVER_PRIORITY);

    navsq = new NavigationServiceSequencer(train_clock, navs_data,
                                           NAVS_GPS_PRIORITY,
                                           NAVS_SERVER_PRIORITY);

    timesq = new TimeServiceSequencer(comms_times_data,
                                      times_data,
                                      train_clock,
                                      TIMES_TICK_PRIORITY,
                                      TIMES_COORDINATION_PRIORITY);

    controlsq = new TrainControlSequencer(comms_control_data,
                                          navs_data,
                                          train_clock,
                                          CONTROL_PRIORITY);

    commsq.register();
    timesq.register();
    navsq.register();
    controlsq.register();
  }

  // no need to override the default implementation of cleanup, which
  // does nothing.

  @Override
  @RunsIn(CALLER)
  @SCJAllowed(LEVEL_2)
  public void requestTermination()
  {
    commsq.requestSequenceTermination();
    timesq.requestSequenceTermination();
    navsq.requestSequenceTermination();
    controlsq.requestSequenceTermination();

    notifyAll();

    super.requestTermination();
  }
}
