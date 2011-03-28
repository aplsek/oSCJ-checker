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
import javax.safetycritical.NoHeapRealtimeThread;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@Scope("D")
@DefineScope(name="D:NO", parent="D")
public class NavigationOversight extends NoHeapRealtimeThread
{
  // Determined by VM-specific static analysis tools
  private static final long BackingStoreRequirements = 500;
  private static final long NativeStackRequirements = 2000;
  private static final long JavaStackRequirements = 300;

  private NavigationService nav_mission;
  private NavigationInfo nav_data;
  private RouteData route_data;

  public NavigationOversight(NavigationService nav_mission,
                             NavigationInfo nav_data,
                             RouteData route_data,
                             int priority) {
    super(new PriorityParameters(priority),
          new StorageParameters(BackingStoreRequirements,
                                NativeStackRequirements,
                                JavaStackRequirements));
    this.nav_mission = nav_mission;
    this.nav_data = nav_data;
    this.route_data = route_data;
  }

  @Override
  @RunsIn("D:NO")
  public void run() {

    while (true) {
      NavigationInfo.RequestEncoding code;

      code = nav_data.awaitRequest();
      switch (code) {
        case CurrentSpeed:
        {
          int speed = nav_mission.getSpeed();
          nav_data.serviceRequest(speed);
          break;
        }

        case CurrentPosition:
          // mimic above pattern for other cases

        case SegmentLength:
        case SegmentSpeed:
        case SegmentSwitch:
        case NextTrackSegment:
        case PrevTrackSegment:
        case NextStop:
      }

    }
  }
}
