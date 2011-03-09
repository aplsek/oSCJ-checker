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

import javax.realtime.InterruptServiceRoutine;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.CURRENT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope("E")
class SecurityInterruptHandler extends InterruptServiceRoutine {

  // Determined by VM-specific static analysis tools
  private static final long BackingStoreRequirements = 500;
  private static final long NativeStackRequirements = 2000;
  private static final long JavaStackRequirements = 300;

  final int INTERRUPT_PRIORITY;
  final CypherQueue cypher_data;
  final SecurityService mission;

  @RunsIn(CURRENT)
  SecurityInterruptHandler(SecurityService my_mission,
                           CypherQueue cypher_data) {
    //    super(new PriorityParameters(INTERRUPT_PRIORITY),
    //          new StorageParameters(BackingStoreRequirements,
    //                                NativeStackRequirements,
    //                                JavaStackRequirements));
    super("CypherHardwareInterrupt");
    this.mission = my_mission;
    this.cypher_data = cypher_data;

    // a guess
    INTERRUPT_PRIORITY = 32;
  }

  @DefineScope(name="SIH_Private", parent="E")
  @RunsIn("SIH_Private")
  public final void handle() {
    // this interrupt means the previously issued security operation
    // has completed its execution.


    cypher_data.completeHardwareRequest(0);
  }
}