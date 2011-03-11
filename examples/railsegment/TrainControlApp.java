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

import javax.safetycritical.LinearMissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope(IMMORTAL)
public class TrainControlApp extends LinearMissionSequencer
  implements Safelet {

  public static final int SequencerPriority = 32;

  @SCJRestricted(INITIALIZATION)
  public TrainControlApp() {
    super(new PriorityParameters(SequencerPriority),
          new StorageParameters(TrainMission.BackingStoreRequirements,
                                TrainMission.NativeStackRequirements,
                                TrainMission.JavaStackRequirements),
          new TrainMission());
  }

  // Inherits getNextMission() from LinearMissionSequencer

  // The following three methods implement the Safelet interface
  public LinearMissionSequencer getSequencer() {
    return this;
  }

  public void setUp() {
    // do nothing
  }

  public void tearDown() {
    // do nothing
  }
}
