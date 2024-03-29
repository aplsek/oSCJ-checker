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

import javax.realtime.InterruptServiceRoutine;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;


@Scope("TM.A.E")
@DefineScope(name="TM.A.E.0", parent="TM.A.E")
@SCJAllowed(value=LEVEL_2, members=true)
class SecurityInterruptHandler extends InterruptServiceRoutine {

  // Determined by VM-specific static analysis tools
  private static final long BackingStoreRequirements = 500;
  private static final long NativeStackRequirements = 2000;
  private static final long JavaStackRequirements = 300;

  final int INTERRUPT_PRIORITY;
  final CypherQueue cypher_data;
  final SecurityService mission;

  SecurityInterruptHandler(SecurityService my_mission,
                           CypherQueue cypher_data) {
    //    super(new PriorityParameters(INTERRUPT_PRIORITY),
    //          new StorageParameters(BackingStoreRequirements,
    //                                NativeStackRequirements,
    //                                JavaStackRequirements));
    super(new String("CypherHardwareInterrupt"));
    this.mission = my_mission;
    this.cypher_data = cypher_data;

    // a guess
    INTERRUPT_PRIORITY = 32;
  }

  @Override
  @RunsIn("TM.A.E.0")
  //@SCJAllowed(SUPPORT)
  public final void handle() {
    // this interrupt means the previously issued security operation
    // has completed its execution.


    cypher_data.completeHardwareRequest(0);
  }
}
