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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *  USA
 */

package railsegment;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.PriorityParameters;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import railsegment.clock.TrainClock;

@Scope("TM")
@DefineScope(name="TM.D", parent = "TM")
@SCJAllowed(value=LEVEL_2, members=true)
public class NavigationServiceSequencer
  extends MissionSequencer //<NavigationService>
{
  boolean did_mission;
  private final int NAVS_SERVER_PRIORITY;
  private final int NAVS_GPS_PRIORITY;
  private final NavigationInfo navs_data;
  private final TrainClock train_clock;

  @SCJRestricted(INITIALIZATION)
  public NavigationServiceSequencer(TrainClock train_clock,
                                    NavigationInfo navs_data,
                                    int NAVS_GPS_PRIORITY,
                                    int NAVS_SERVER_PRIORITY)
  {
    // note: the sequencer thread will be blocked throughout most of
    // the mission's execution.  since the sequencer is coordinating
    // with multiple subordinate threads, it seems reasonable to think
    // of it as a server to all of those.  and as a server, it seems
    // appropriate that its priority is the maximum of the subordinate
    // threads' priorities.
    super(new PriorityParameters(NAVS_GPS_PRIORITY),
          new StorageParameters(NavigationService.BackingStoreRequirements,
                                storageArgs(), 0, 0),
          new String("Navigation Service Sequencer"));

    did_mission = false;
    this.NAVS_GPS_PRIORITY = NAVS_GPS_PRIORITY;
    this.NAVS_SERVER_PRIORITY = NAVS_SERVER_PRIORITY;
    this.train_clock = train_clock;
    this.navs_data = navs_data;
  }

  private static long[] storageArgs() {
    long[] storage_args = {NavigationService.NestedBackingStoreRequirements,
                           NavigationService.NativeStackRequirements,
                           NavigationService.JavaStackRequirements};
    return storage_args;
  }

  @Override
  @RunsIn("TM.D")
  @SCJAllowed(SUPPORT)
  public NavigationService getNextMission()
  {
    if (!did_mission) {
      did_mission = true;
      return new NavigationService(navs_data, train_clock, new RouteData(),
                                   NAVS_GPS_PRIORITY, NAVS_SERVER_PRIORITY);
    }
    else {
      return null;
    }
  }
}
