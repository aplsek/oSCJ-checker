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

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Scope.CALLER;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

import railsegment.clock.SynchronizedTime;

@Scope("B")
@SCJAllowed(value=LEVEL_2, members=true)
public class TrainControl extends Mission
{
  // These three constants determined by static analysis or other
  // vendor-specific approaches
  public static final long BackingStoreRequirements = 5000;
  public static final long NativeStackRequirements = 3000;
  public static final long JavaStackRequirements = 2000;

  public static final long MissionMemorySize = 500;

  private final int CONTROL_PRIORITY;

  private final CommunicationsQueue comms_data;
  private final SynchronizedTime times_data;
  private final NavigationInfo navs_data;

  private TrainControlThread train_thread;

  public TrainControl(final CommunicationsQueue comms_data,
                      final SynchronizedTime times_data,
                      final NavigationInfo navs_data,
                      final int CONTROL_PRIORITY)
  {
    this.comms_data = comms_data;
    this.times_data = times_data;
    this.navs_data = navs_data;

    this.CONTROL_PRIORITY = CONTROL_PRIORITY;
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
    // TODO: later, we should declare the various subtypes and
    // instantiate them here.  I think I've flushed out enough of
    // these details now that the "exercise" is not instructive at
    // this point.

    // Implementation not shown for expediency.

    train_thread = new TrainControlThread(comms_data, times_data, navs_data,
                                          CONTROL_PRIORITY);

    // spawn one NHRT to take responsibility for maintaining
    // coordination with central traffic control.  this NHRT
    // pre-fetches future segment authorizations and releases old
    // authorizations no longer required.  It also tracks progress
    // along route to assure that upcoming track switches are in the
    // correct position.


    // spawn one periodic event handler with 1 minute period to
    // calculate ETA for next destination.

    // spawn one periodic event handler with 1 second period to
    // calculate desired speed and adjust train parameters appropriately

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
