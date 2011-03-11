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
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.RelativeTime;

import javax.realtime.PriorityParameters;

import javax.safetycritical.Mission;
import javax.safetycritical.NoHeapRealtimeThread;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import static javax.safetycritical.annotate.Scope.CALLER;

@DefineScope(name="C", parent=IMMORTAL)
@Scope("C")
public class TimeService extends Mission
{
  // These three constants determined by static analysis or other
  // vendor-specific approaches
  public static final long BackingStoreRequirements = 5000;
  public static final long NativeStackRequirements = 3000;
  public static final long JavaStackRequirements = 2000;

  public static final long MissionMemorySize = 500;

  private final int TIME_PRIORITY;

  private final SynchronizedTime times_data;

  private final CommunicationsQueue comms_data;

  private final TrainClock train_clock;

  private TimerOversight timer_thread;
  private TimerTick timer_tick;

  private long synchronized_ms;
  private int synchronized_ns;

  public TimeService(final SynchronizedTime times_data,
                     final CommunicationsQueue comms_data,
                     final TrainClock train_clock,
                     final int TIME_PRIORITY)
  {
    this.times_data = times_data;
    this.comms_data = comms_data;
    this.train_clock = train_clock;
    this.TIME_PRIORITY = TIME_PRIORITY;
  }

  @Override
  public final long missionMemorySize()
  {
    // must be large enough to represent the three Schedulables
    // instantiated by the initialize() method
    return MissionMemorySize;
  }

  @Override
  public void initialize()
  {
    // How is synchronized time implemented?  Let's
    // assume that there is one high priority periodic event handler
    // that runs every 250 ns to update global time, and there's a
    // NHRT thread that takes responsibility for awaiting call back
    // times and invoking the call-back service.

    timer_thread = new TimerOversight(this, comms_data, TIME_PRIORITY - 1);
    timer_tick = new TimerTick(this, train_clock, TIME_PRIORITY);
  }

  @RunsIn(CALLER)
  synchronized void updateGlobalTime(long ms, int ns) {

    // TODO: should normalize numbers, maybe...

    synchronized_ms = ms;
    synchronized_ns = ns;
  }

  @RunsIn(CALLER)
  synchronized void getGlobalTime(AbsoluteTime t) {
    if ((t != null) && (t.getClock() == train_clock)) {
      t.set(synchronized_ms, synchronized_ns);
    }
    else {
      // todo: make this a preallocated exception
      throw new IllegalArgumentException();
    }
  }

  @Override
@RunsIn(CALLER)
  public void requestTermination()
  {
    // something special to coordinate with the NHRT thread

    super.requestTermination();
  }
}
