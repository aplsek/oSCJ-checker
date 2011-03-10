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
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import railsegment.clock.SynchronizedTime;

import static javax.safetycritical.annotate.Scope.CALLER;

@Scope("TM")
public class TrainControlSequencer extends MissionSequencer // <TrainControl>
{
    private boolean did_mission;

    private final int CONTROL_PRIORITY;

    private final CommunicationsQueue comms_data;
    private final SynchronizedTime times_data;
    private final NavigationInfo navs_data;

    public TrainControlSequencer(final int CONTROL_PRIORITY,
            CommunicationsQueue comms_data, SynchronizedTime times_data,
            NavigationInfo navs_data) {
        super(new PriorityParameters(CONTROL_PRIORITY), new StorageParameters(
                TrainControl.BackingStoreRequirements,
                TrainControl.NativeStackRequirements,
                TrainControl.JavaStackRequirements), "Train Control Sequencer");

        this.CONTROL_PRIORITY = CONTROL_PRIORITY;

        this.comms_data = comms_data;
        this.times_data = times_data;
        this.navs_data = navs_data;

        did_mission = false;
    }

    @Override
    @RunsIn("B")
    public TrainControl getNextMission() {
        if (!did_mission) {
            did_mission = true;
            return new TrainControl(comms_data, times_data, navs_data,
                    CONTROL_PRIORITY);
        } else {
            return null;
        }
    }

    @Override
    protected Mission getInitialMission() {
        // TODO Auto-generated method stub
        return null;
    }
}