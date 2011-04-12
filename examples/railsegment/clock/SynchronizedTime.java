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

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.realtime.AbsoluteTime;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

// SynchronizedTime is used in the implementation of a globally
// synchronized time service.  The implementation assures
// monotonically increasing time.

@SCJAllowed(value=LEVEL_2, members=true)
@Scope("TM")
public class SynchronizedTime
{
  private long synchronized_ms;
  private int synchronized_ns;

  // This is the time at which I am required to perform the next callback
  AbsoluteTime callback_time;

  private int ns_tolerance;

  private final int CEILING;

  TrainClock train_clock;
  private boolean initialized;

  public SynchronizedTime(int ceiling)
  {
    CEILING = ceiling;
    initialized = false;
  }

  // package access
  public synchronized TrainClock initialize() {

    //Services.setCeiling(this, CEILING);
    train_clock = new TrainClock(this);
    train_clock.initialize();
    callback_time = new AbsoluteTime(0L, 0, train_clock);
    initialized = true;
    notifyAll();
    return train_clock;
  }

  @RunsIn(CALLER)
  public synchronized final AbsoluteTime getTime()
  {
    while (!initialized) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }
    // Purdue says this is ok, though a guard will probably be
    // necessary in the caller's context...
    return new AbsoluteTime(synchronized_ms,
                            synchronized_ns, train_clock);
  }

  @RunsIn(CALLER)
  private synchronized void updateTime(long new_ms, int new_ns)
  {
    if (((synchronized_ms < new_ms) ||
         ((synchronized_ms == new_ms) && (synchronized_ns < new_ns))) &&
        (new_ns < 1000000)) {
      synchronized_ms = new_ms;
      synchronized_ns = new_ns;
    }
    if (!initialized) {
      initialized = true;
      notifyAll();
    }
  }

  @RunsIn(CALLER)
  synchronized void setCallBackTime(@Scope(UNKNOWN) AbsoluteTime new_timeout)
  {
    callback_time.set(new_timeout.getMilliseconds(),
                      new_timeout.getNanoseconds());
    notifyAll();
  }

  // package access
  @RunsIn(CALLER)
  synchronized void awaitCallBackTime()
  {
    while ((callback_time != null) &&
           (synchronized_ms < callback_time.getMilliseconds()) ||
           ((synchronized_ms == callback_time.getMilliseconds()) &&
            (synchronized_ns < callback_time.getNanoseconds()))) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }
  }
}
