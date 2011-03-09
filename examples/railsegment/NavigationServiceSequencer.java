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

import javax.realtime.PriorityParameters;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope("TM")
public class NavigationServiceSequencer
  extends MissionSequencer<NavigationService>
{
  boolean did_mission;
  private final int NAVS_PRIORITY;
  private final int GPS_PRIORITY;
  private final NavigationInfo navs_data;
  private final TrainClock train_clock;

  public NavigationServiceSequencer(final int NAVS_PRIORITY,
                                    final int GPS_PRIORITY,
                                    TrainClock train_clock,
                                    NavigationInfo navs_data)
  {
    super(new PriorityParameters(NAVS_PRIORITY),
          new StorageParameters(NavigationService.BackingStoreRequirements,
                                NavigationService.NativeStackRequirements,
                                NavigationService.JavaStackRequirements),
          "Navigation Service Sequencer");

    did_mission = false;
    this.NAVS_PRIORITY = NAVS_PRIORITY;
    this.GPS_PRIORITY = GPS_PRIORITY;
    this.train_clock = train_clock;
    this.navs_data = navs_data;
  }

  @RunsIn("D")
  public NavigationService getNextMission()
  {
    if (!did_mission) {
      did_mission = true;
      return new NavigationService(navs_data, train_clock, new RouteData(),
                                   GPS_PRIORITY, NAVS_PRIORITY);
    }
    else {
      return null;
    }
  }
}