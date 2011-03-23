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

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope("A")
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
                        SecurityService.NativeStackRequirements,
                        SecurityService.JavaStackRequirements),
                        "Communication Services Sequencer");

        SECURITY_PRIORITY = security_priority;
        this.cypher_data = cypher_data;
        did_mission = false;
    }

    @Override
    @RunsIn("E")
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
