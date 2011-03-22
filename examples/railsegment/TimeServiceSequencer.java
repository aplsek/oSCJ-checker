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

import railsegment.clock.SynchronizedTime;
import railsegment.clock.TimeService;
import railsegment.clock.TrainClock;

import javax.realtime.PriorityParameters;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


@Scope("TM")
public class TimeServiceSequencer extends MissionSequencer
{
    private final SynchronizedTime times_data;
    private final CommunicationsQueue comms_data;
    private final TrainClock train_clock;
    private final int TIMES_PRIORITY;

    @SCJRestricted(INITIALIZATION)
    public TimeServiceSequencer(final int TIMES_PRIORITY,
            CommunicationsQueue comms_data,
            SynchronizedTime times_data,
            TrainClock train_clock)
    {
        super(new PriorityParameters(TIMES_PRIORITY),
                new StorageParameters(TimeService.BackingStoreRequirements,
                        TimeService.NativeStackRequirements,
                        TimeService.JavaStackRequirements),
                        "Time Service Sequencer");
        this.TIMES_PRIORITY = TIMES_PRIORITY;
        this.times_data = times_data;
        this.comms_data = comms_data;
        this.train_clock = train_clock;
    }

    @Override
    @RunsIn("C")
    public TimeService getNextMission()
    {
        return new TimeService(times_data,
                comms_data, train_clock, TIMES_PRIORITY);
    }

    @Override
    protected Mission getInitialMission() {
        // TODO Auto-generated method stub
        return null;
    }

}
