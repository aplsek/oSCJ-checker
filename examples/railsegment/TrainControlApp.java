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

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.THIS;

@Scope(IMMORTAL)
public class TrainControlApp implements Safelet {

  public static final int SequencerPriority = 32;

  public TrainControlApp() {
  }

  // The following three methods implement the Safelet interface
  @SCJAllowed(SUPPORT)
  public MissionSequencer getSequencer() {
    return new TrainMissionSequencer();
  }

  @SCJAllowed(SUPPORT)
  public void setUp() {
    // do nothing
  }

  @SCJAllowed(SUPPORT)
  public void tearDown() {
    // do nothing
  }

  @SCJRestricted(INITIALIZATION)
  public void method() {
    // do nothing
  }


  @RunsIn(CALLER)
  public void foo() {

  }

  @Scope("B")
  public Object bar() {
      return null;
  }
}

@Scope(IMMORTAL)
class App extends TrainControlApp {

    @Override
    public void method() {
      // do nothing
    }

    //@SCJRestricted(CLEANUP)
    public void methodCLEANUP() {
        //method();
    }

    //@Override
    //public void foo() {

    //}

    @Override
    @Scope("B")
    public Object bar() {
        return null;
    }


}
