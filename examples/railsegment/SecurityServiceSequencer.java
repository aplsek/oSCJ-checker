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

@Scope("TM.A")
@DefineScope(name="TM.A.E", parent="TM.A")
@SCJAllowed(value=LEVEL_2, members=true)
public class SecurityServiceSequencer
extends MissionSequencer //<SecurityService>
{
  private boolean did_mission;
  private final int SECURITY_PRIORITY;
  private final CypherQueue cypher_data;

  @SCJRestricted(INITIALIZATION)
  public SecurityServiceSequencer(final int security_priority,
                                  CypherQueue cypher_data)
  {
    super(new PriorityParameters(security_priority),
          new StorageParameters(SecurityService.BackingStoreRequirements,
                                storageArgs(), 0, 0),
          new String("Communication Services Sequencer"));

    SECURITY_PRIORITY = security_priority;
    this.cypher_data = cypher_data;
    did_mission = false;
  }

  private static long[] storageArgs() {
    long[] storage_args = {
      SecurityService.NestedBackingStoreRequirements,
      SecurityService.NativeStackRequirements,
      SecurityService.JavaStackRequirements};
    return storage_args;
  }

  @Override
  @RunsIn("TM.A.E")
  @SCJAllowed(SUPPORT)
  protected SecurityService getNextMission()
  {
    if (!did_mission) {
      did_mission = true;
      return new SecurityService(SECURITY_PRIORITY, cypher_data);
    }
    else {
      return null;
    }
  }
}
