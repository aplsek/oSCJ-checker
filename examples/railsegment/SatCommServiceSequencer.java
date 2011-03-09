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

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("A")
public class SatCommServiceSequencer
  extends MissionSequencer<SatCommService>
{
  private boolean did_mission;

  private final int SAT_PRIORITY;
  private final SatQueue sat_data;

  public SatCommServiceSequencer(final int sat_priority, SatQueue sat_data)
  {
    super(new PriorityParameters(sat_priority),
          new StorageParameters(SatCommService.BackingStoreRequirements,
                                SatCommService.NativeStackRequirements,
                                SatCommService.JavaStackRequirements),
          "Communication Services Sequencer");

    SAT_PRIORITY = sat_priority;
    this.sat_data = sat_data;
    did_mission = false;
  }

  @RunsIn("G")
  protected SatCommService getNextMission()
  {
    if (!did_mission) {
      did_mission = true;
      return new SatCommService(SAT_PRIORITY, sat_data);
    }
    else {
      return null;
    }
  }
}