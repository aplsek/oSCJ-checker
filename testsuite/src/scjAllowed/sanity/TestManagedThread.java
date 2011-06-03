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

package scjAllowed.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedThread;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;


@SCJAllowed(value=LEVEL_2, members=true)
@Scope(IMMORTAL)
@DefineScope(name="C:TO", parent=IMMORTAL)
public class TestManagedThread extends ManagedThread
{
  private static final long BackingStoreRequirements = 500;
  private static final long NativeStackRequirements = 2000;
  private static final long JavaStackRequirements = 300;


  @SCJRestricted(INITIALIZATION)
  public TestManagedThread(int priority) {
      super(new PriorityParameters(priority),
          new StorageParameters(BackingStoreRequirements,
                                NativeStackRequirements,
                                JavaStackRequirements));
  }
  @Override
  @RunsIn("C:TO")
  @SCJAllowed(SUPPORT)
  public void run() {
  }
}
