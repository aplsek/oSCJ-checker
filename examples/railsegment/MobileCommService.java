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
import static javax.safetycritical.annotate.Scope.CALLER;

import javax.safetycritical.Mission;
import javax.safetycritical.Services;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@Scope("TM.A.H")
@SCJAllowed(value=LEVEL_2, members=true)
public class MobileCommService extends Mission
{
  // These three constants determined by static analysis or other
  // vendor-specific approaches
  public static final long BackingStoreRequirements = 1000;
  public static final long StackRequirements = 5000;

  private static final long MissionMemorySize = 500;

  // a guess
  private static final int ISR_PRIORITY = 32;
  private final int MOBILE_PRIORITY;
  MobileQueue mobile_data;

  MobileOversight mobile_thread;
  MobileInterruptHandler mobile_isr;

  public MobileCommService(final int mobile_priority,
                           final MobileQueue mobile_data)
  {
    this.mobile_data = mobile_data;
    MOBILE_PRIORITY = mobile_priority;
  }

  @Override
  @SCJAllowed
  public final long missionMemorySize()
  {
    // must be large enough to represent the three Schedulables
    // instantiated by the initialize() method
    return MissionMemorySize;
  }

  @Override
  @SCJAllowed(SUPPORT)
  public void initialize()
  {
    // assume I'll provide shared variables for coordination between
    // mobile_thread and mobile_isr
    Services.setCeiling(this, ISR_PRIORITY);

    // Let's assume there are two schedulables here.
    //
    //  1. One is an oversight thread that listens for requests from
    //     the application and arranges to provide responses.
    //  2. The other is an asynchronous event handler, really an
    //     interrupt handler, which waits for completion of a previously
    //     issued communication request, or possibly signals receipt of
    //     a new message.
    mobile_thread = new MobileOversight(MOBILE_PRIORITY, this, mobile_data);
    mobile_isr = new MobileInterruptHandler(this);
  }

  @Override
  @RunsIn(CALLER)
  public void requestTermination()
  {
    // do something special to coordinate with the NHRT thread
    mobile_data.requestTermination();

    // assume shutdown request is delivered top down
    // super.requestTermination();
  }
}
