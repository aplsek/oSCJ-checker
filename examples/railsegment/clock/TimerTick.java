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

import javax.realtime.AbsoluteTime;
import javax.realtime.PeriodicParameters;
import javax.realtime.RelativeTime;

import javax.realtime.PriorityParameters;

import javax.safetycritical.NoHeapRealtimeThread;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.CURRENT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope("C")
public class TimerTick extends PeriodicEventHandler
{
  // Determined by VM-specific static analysis tools
  private static final long BackingStoreRequirements = 500;
  private static final long NativeStackRequirements = 2000;
  private static final long JavaStackRequirements = 300;

  private TimeService time_mission;
  private TrainClock train_clock;

  // This periodic task runs every 250 microseconds
  @Scope(CURRENT)
  public TimerTick(TimeService time_mission,
                   TrainClock train_clock, int priority) {
    super(new PriorityParameters(priority),
          new PeriodicParameters(null, new RelativeTime(0L, 250000)),
          new StorageParameters(BackingStoreRequirements,
                                NativeStackRequirements,
                                JavaStackRequirements));
    this.time_mission = time_mission;
    this.train_clock = train_clock;
  }

  @DefineScope(name="C:TT", parent="C")
  @Scope("C:TT")
  public void handleAsyncEvent() {
    AbsoluteTime t = new AbsoluteTime(0L, 0, train_clock);
    time_mission.getGlobalTime(t);
    train_clock.updateTime(t);
  }
}