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

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.realtime.AbsoluteTime;
import javax.safetycritical.Mission;
import javax.safetycritical.Services;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import railsegment.clock.TrainClock;


@DefineScope(name="D", parent = "TM")
@Scope("D")
class NavigationService extends Mission {

  // These three constants are determined by static analysis or other
  // vendor-specific approaches
  public static final long BackingStoreRequirements = 10000;
  public static final long NativeStackRequirements = 5000;
  public static final long JavaStackRequirements = 2000;

  // This is large enough to represent the four sub-mission sequencers
  // allocated by my initialize method
  public static final long MissionMemorySize = 6000;

  private final int GPS_PRIORITY;
  private final int NAV_LOOKUP_PRIORITY;

  private final RouteData route_data;
  private final NavigationInfo navs_data;

  private TrainClock train_clock;

  private GPSDriver gps_driver;
  private NavigationOversight nav_thread;

  NavigationService(NavigationInfo navs_data, TrainClock train_clock,
                    RouteData route_data, int gps_priority,
                    int nav_lookup_priority) {
    GPS_PRIORITY = gps_priority;
    NAV_LOOKUP_PRIORITY = nav_lookup_priority;

    this.route_data = route_data;
    this.navs_data = navs_data;
    this.train_clock = train_clock;

    update_time = new AbsoluteTime(0L, 0, train_clock);
    prior_update_time = new AbsoluteTime(0L, 0, train_clock);
  }

  @Override
  public final long missionMemorySize()
  {
    // This must be large enough to hold each of the four submissions,
    // the NHRT, and all auxiliary data structures associated with
    // communication with these missions.
    return MissionMemorySize;
  }

  @Override
  public void initialize() {
    // This is where I'd like to set my ceiling.  Confirm that it is
    // ok here.  The following assumes the GPS_PRIORITY > NAV_LOOKUP_PRIORITY
    Services.setCeiling(this, GPS_PRIORITY);

    // startup one thread to be the device driver for the GPS system
    gps_driver = new GPSDriver(this, train_clock, GPS_PRIORITY);

    // startup a second thread to respond to user requests for
    // "current position", "track segment info", etc.
    nav_thread = new NavigationOversight(this, navs_data,
                                         route_data, NAV_LOOKUP_PRIORITY);
  }

  @Override
  public void cleanUp() {

  }

  @Override
@RunsIn(CALLER)
  public void requestTermination() {

  }

  private int prior_longitude, prior_lattitude;
  AbsoluteTime prior_update_time;

  private int longitude, lattitude;
  AbsoluteTime update_time;

  // called periodically by the GPS Driver
  @RunsIn(CALLER)
  synchronized void updatePosition(@Scope(UNKNOWN) AbsoluteTime time_stamp,
                                   int longitude, int lattitude) {

    if (time_stamp.getClock() != train_clock) {
      // todo: preallocate this exception
      throw new IllegalArgumentException();
    }

    prior_longitude = this.longitude;
    prior_lattitude = this.lattitude;
    prior_update_time.set(update_time.getMilliseconds(),
                          update_time.getNanoseconds());

    this.longitude = longitude;
    this.lattitude = lattitude;

    update_time.set(time_stamp);
  }

  // return the most recently measured speed in tenths of mile per hour
  @RunsIn(CALLER)
  synchronized int getSpeed() {
    // calculate distance between prior_longitude, prior_lattitude and
    // current longitude, lattitude.

    // calculate time between update_time and prior_update_time

    // use the ratio to represent tenths of mile per hour

    return 0;
  }

  /**
   * Position is lat, long encoded within the single long result.  The
   * high-order 32 bits represent millionths of a degree lattitude.  The
   * low-order 32 bits represent mimllionths of a degree longitude.
   *
   * It seems that millions of a degree represent measurement
   *  precision to within about a foot.  Circumference of earth is
   *  approximately 25,000 miles = 132,000,000 ft.
   *
   *     132,000,000 ft     1 degree      ~      1/3 ft
   *     -------------- * --------------  = ----------------
   *      360 degrees     10^6 millionths   millionth degree
   */
  @RunsIn(CALLER)
  synchronized long getPosition(@Scope(UNKNOWN) AbsoluteTime time_stamp) {

    if ((time_stamp != null) && (time_stamp.getClock() != train_clock)) {
      // todo: preallocate exception
      throw new IllegalArgumentException();
    }

    long result = (((long) longitude) << 32) | (((long) lattitude) & 0xffff);
    if (time_stamp != null) {
      time_stamp.set(update_time.getMilliseconds(),
                     update_time.getNanoseconds());
    }
    return result;
  }

}
