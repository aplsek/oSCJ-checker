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
import javax.safetycritical.Mission;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.UNKNOWN;

@DefineScope(name="E", parent="A")
@Scope("E")
public class SecurityService extends Mission
{
  // These three constants determined by static analysis or other
  // vendor-specific approaches
  public static final long BackingStoreRequirements = 5000;
  public static final long NativeStackRequirements = 3000;
  public static final long JavaStackRequirements = 2000;

  public static final long MissionMemorySize = 500;

  // a guess.
  private static final int ISR_PRIORITY = 32;

  private final int CYPHER_PRIORITY;
  private final CypherQueue cypher_data;

  private SecurityOversight cypher_thread;
  private SecurityInterruptHandler cypher_isr;

  public SecurityService(final int cypher_priority,
                         final CypherQueue cypher_data)
  {
    this.cypher_data = cypher_data;
    CYPHER_PRIORITY = cypher_priority;
  }

  @Override
@RunsIn(CURRENT)
  public final long missionMemorySize()
  {
    // must be large enough to represent the three Schedulables
    // instantiated by the initialize() method
    return MissionMemorySize;
  }

  @Override
@RunsIn(CURRENT)
  public void initialize()
  {
    // Let's assume there are two schedulables here.
    //
    //  1. One is an oversight thread that listens for requests from
    //     the application and arranges to provide responses.
    //  2. The other is an asynchronous event handler, really an
    //     interrupt handler, which waits for completion of a DMA
    //     encryption service.

    cypher_thread = new SecurityOversight(CYPHER_PRIORITY, this, cypher_data);
    cypher_isr = new SecurityInterruptHandler(this, cypher_data);
  }

  @Override
@RunsIn(UNKNOWN)
  public void requestTermination()
  {
    // do something special to coordinate with the NHRT thread
    cypher_data.issueShutdownRequest();

    super.requestTermination();
  }
}