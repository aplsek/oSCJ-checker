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


import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name="TM", parent=IMMORTAL)
@SCJAllowed(value=LEVEL_2, members=true)
public class TrainMissionSequencer extends MissionSequencer {
  private static final int SequencerPriority = 20;

  private boolean returned_mission;

  @SCJRestricted(INITIALIZATION)
  public TrainMissionSequencer() {
    super(new PriorityParameters(SequencerPriority),
          new StorageParameters(TrainMission.BackingStoreRequirements,
                                TrainMission.NativeStackRequirements,
                                TrainMission.JavaStackRequirements));
    returned_mission = false;
  }

  @Override
  @RunsIn("TM")
  @SCJAllowed(SUPPORT)
  public Mission getNextMission() {
    if (returned_mission) {
      return null;
    }
    else {
      returned_mission = true;
      return new TrainMission();
    }
  }
}
