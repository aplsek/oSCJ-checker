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

import javax.realtime.MemoryArea;
import javax.realtime.PriorityParameters;

import javax.safetycritical.Mission;
import javax.safetycritical.NoHeapRealtimeThread;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.CURRENT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope("H")
public class MobileOversight extends NoHeapRealtimeThread {

  // These three constants determined by static analysis or other
  // vendor-specific approaches
  public static final long BackingStoreRequirements = 5000;
  public static final long NativeStackRequirements = 3000;
  public static final long JavaStackRequirements = 2000;

  // a guess
  private static final int ISR_PRIORITY = 32;
  private final int MOBILE_PRIORITY;

  MobileQueue mobile_data;

  MobileCommService mission;

  MobileOversight mobile_thread;
  MobileInterruptHandler mobile_isr;

  @RunsIn(CURRENT)
  public MobileOversight(final int mobile_priority,
                         final MobileCommService my_mission,
                         final MobileQueue mobile_data)
  {
    super(new PriorityParameters(mobile_priority),
          new StorageParameters(BackingStoreRequirements,
                                NativeStackRequirements,
                                JavaStackRequirements));

    this.mobile_data = mobile_data;
    this.mission = my_mission;
    MOBILE_PRIORITY = mobile_priority;
  }


  @RunsIn(UNKNOWN)
  public void requestTermination()
  {
    // do something special to coordinate with the NHRT thread
    mobile_data.requestTermination();
  }

  @DefineScope(name="MO_Private", parent="H")
  @RunsIn("MO_Private")
  public void run() {

    while (true) {
      MobileQueue.RequestEncoding req = mobile_data.awaitRequest();

      if (req == MobileQueue.RequestEncoding.RequestShutdown) {
        return;
      }
      else {

        // do some work

      }

    }

  }
}