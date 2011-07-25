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
package railsegment.clock;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;

import javax.realtime.AbsoluteTime;
import javax.safetycritical.Mission;
import javax.safetycritical.Services;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import railsegment.CommunicationsQueue;

@SCJAllowed(value=LEVEL_2, members=true)
@Scope("TM.C")
public class TimeService extends Mission
{
  // These four constants determined by static analysis or other
  // vendor-specific approaches
  public static final long BackingStoreRequirements = 500;
  public static final long StackRequirements = 5000;

  public static final long MissionMemorySize = 500;

  private final int TIME_TICK_PRIORITY;
  private final int TIME_COORDINATION_PRIORITY;

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
                     final int TIME_TICK_PRIORITY,
                     final int TIME_COORDINATION_PRIORITY)
  {
    this.times_data = times_data;
    this.comms_data = comms_data;
    this.train_clock = train_clock;
    this.TIME_TICK_PRIORITY = TIME_TICK_PRIORITY;
    this.TIME_COORDINATION_PRIORITY = TIME_COORDINATION_PRIORITY;
  }

  @Override
  @SCJAllowed(SUPPORT)
  public final long missionMemorySize()
  {
    // must be large enough to represent the three Schedulables
    // instantiated by the initialize() method
    return MissionMemorySize;
  }

  @Override
  @SCJAllowed(SUPPORT)
  @SCJRestricted(INITIALIZATION)
  public void initialize()
  {
    // How is synchronized time implemented?  Let's
    // assume that there is one high priority periodic event handler
    // that runs every 250 ns to update global time, and there's a
    // NHRT thread that takes responsibility for awaiting call back
    // times and invoking the call-back service.

    // Set my ceiling to the tick priority since that's higher than
    // the coordination priority.  The timer tick invokes my
    // synchronized updateGlobalTime method.
    Services.setCeiling(this, TIME_TICK_PRIORITY);

    timer_thread = new TimerOversight(this, comms_data,
                                      TIME_COORDINATION_PRIORITY - 1);
    timer_tick = new TimerTick(this, train_clock, TIME_TICK_PRIORITY);
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
  @SCJAllowed
  public void requestTermination()
  {
    // something special to coordinate with the NHRT thread

    super.requestTermination();
  }
}
