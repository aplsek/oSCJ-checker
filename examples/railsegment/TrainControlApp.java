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
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import static javax.safetycritical.annotate.Level.LEVEL_2;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@SCJAllowed(value=LEVEL_2, members=true)
public class TrainControlApp implements Safelet {

  @SCJAllowed(SUPPORT)
  public TrainControlApp() {
      init();
  }

  private void init() {

  }

  // The following three methods implement the Safelet interface
  @SCJAllowed(SUPPORT)
  @SCJRestricted(INITIALIZATION)
  public MissionSequencer getSequencer() {
    return new TrainMissionSequencer();
  }

  @SCJAllowed(SUPPORT)
  @SCJRestricted(INITIALIZATION)
  public void setUp() {
    // do nothing
  }

  @SCJAllowed(SUPPORT)
  @SCJRestricted(CLEANUP)
  public void tearDown() {
    // do nothing
  }
}
