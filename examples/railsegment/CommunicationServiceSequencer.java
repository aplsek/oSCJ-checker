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

@Scope("TM")
@DefineScope(name="TM.A", parent="TM")
@SCJAllowed(value=LEVEL_2, members=true)
public class CommunicationServiceSequencer
  extends MissionSequencer //<CommunicationService>
{
  private boolean did_mission;

  private final int COMMS_DRIVER_PRIORITY;
  private final int COMMS_CONTROL_SERVER_PRIORITY;
  private final int COMMS_TIMES_SERVER_PRIORITY;

  private final CommunicationsQueue comms_control_data;
  private final CommunicationsQueue comms_times_data;

  @SCJRestricted(INITIALIZATION)
  public CommunicationServiceSequencer(CommunicationsQueue comms_control_data,
                                       CommunicationsQueue comms_times_data,
                                       int COMMS_DRIVER_PRIORITY,
                                       int COMMS_CONTROL_SERVER_PRIORITY,
                                       int COMMS_TIMES_SERVER_PRIORITY)
  {
    super(new PriorityParameters(COMMS_DRIVER_PRIORITY),
          new StorageParameters(CommunicationService.BackingStoreRequirements,
                                storageArgs(), 0, 0),
          new String("Communication Services Sequencer"));

    this.COMMS_DRIVER_PRIORITY = COMMS_DRIVER_PRIORITY;
    this.COMMS_CONTROL_SERVER_PRIORITY = COMMS_CONTROL_SERVER_PRIORITY;
    this.COMMS_TIMES_SERVER_PRIORITY = COMMS_TIMES_SERVER_PRIORITY;
    this.comms_control_data = comms_control_data;
    this.comms_times_data = comms_times_data;
    did_mission = false;
  }

  private static long[] storageArgs() {
    long[] storage_args = {CommunicationService.StackRequirements};
    return storage_args;
  }

  @Override
  @RunsIn("TM.A")
  @SCJAllowed(SUPPORT)
  protected CommunicationService getNextMission()
  {
    if (!did_mission) {
      did_mission = true;
      return new CommunicationService(comms_control_data, comms_times_data, 
                                      COMMS_DRIVER_PRIORITY,
                                      COMMS_CONTROL_SERVER_PRIORITY,
                                      COMMS_TIMES_SERVER_PRIORITY);
    }
    else {
      return null;
    }
  }
}
