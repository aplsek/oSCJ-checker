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
@DefineScope(name="TM.B", parent="TM")
@SCJAllowed(value=LEVEL_2, members=true)
public class TrainControlSequencer extends MissionSequencer // <TrainControl>
{
  private boolean did_mission;

  private final int CONTROL_PRIORITY;

  private final CommunicationsQueue comms_data;
  private final NavigationInfo navs_data;

  private final TrainClock train_clock;

  @SCJRestricted(INITIALIZATION)
  public TrainControlSequencer(CommunicationsQueue comms_data,
                               NavigationInfo navs_data,
                               TrainClock train_clock,
                               final int CONTROL_PRIORITY) {
    super(new PriorityParameters(CONTROL_PRIORITY),
          new StorageParameters(TrainControl.BackingStoreRequirements,
                                storageArgs(), 0, 0),
          new String("Train Control Sequencer"));

    this.CONTROL_PRIORITY = CONTROL_PRIORITY;

    this.comms_data = comms_data;
    this.navs_data = navs_data;
    this.train_clock = train_clock;

    did_mission = false;
  }

  private static long[] storageArgs() {
    long[] storage_args = {TrainControl.NestedBackingStoreRequirements,
                           TrainControl.NativeStackRequirements,
                           TrainControl.JavaStackRequirements};
    return storage_args;
  }


  @Override
  @RunsIn("TM.B")
  @SCJAllowed(SUPPORT)
  public TrainControl getNextMission() {
    if (!did_mission) {
      did_mission = true;
      return new TrainControl(comms_data, navs_data, 
                              train_clock, CONTROL_PRIORITY);
    } else {
      return null;
    }
  }
}
