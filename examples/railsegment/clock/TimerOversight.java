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

package railsegment.clock;

import railsegment.CommunicationsQueue;

import javax.realtime.AbsoluteTime;
import javax.realtime.PeriodicParameters;
import javax.realtime.RelativeTime;

import javax.realtime.PriorityParameters;

import javax.safetycritical.NoHeapRealtimeThread;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope("C")
public class TimerOversight extends NoHeapRealtimeThread
{
  // Determined by VM-specific static analysis tools
  private static final long BackingStoreRequirements = 500;
  private static final long NativeStackRequirements = 2000;
  private static final long JavaStackRequirements = 300;

  private CommunicationsQueue comms_data;
  private TimeService time_mission;

  public TimerOversight(TimeService time_mission,
                        CommunicationsQueue comms_data,
                        int priority) {
    super(new PriorityParameters(priority),
          new StorageParameters(BackingStoreRequirements,
                                NativeStackRequirements,
                                JavaStackRequirements));
    this.comms_data = comms_data;
  }

  @Override
@DefineScope(name="C:TO", parent="C")
  @Scope("C:TO")
  public void run() {

    // I assume there's a fairly complicated algorithm required to
    // implement globally synchronized clocks.  I don't begin to
    // understand what this algorithm is.  But I know it involves
    // interacting with other nodes.
    //  read, write: comms_data
    //  read: time_mission.getGlobalTime()

    // Based on my interactions with other nodes, I'll refine my
    // understanding of the current time, and will update a field in
    //  write: time_mission.updateGlobalTime()


  }
}