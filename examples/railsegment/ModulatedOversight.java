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

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import static javax.safetycritical.annotate.Scope.CALLER;

@Scope("F")
@DefineScope(name="MO_Private", parent="F")
class ModulatedOversight extends NoHeapRealtimeThread {

  // Determined by VM-specific static analysis tools
  private static final long BackingStoreRequirements = 500;
  private static final long NativeStackRequirements = 2000;
  private static final long JavaStackRequirements = 300;

  final int MODULATED_PRIORITY;
  final ModulatedQueue modulated_data;
  final ModulatedCommService mission;

  ModulatedOversight(int priority,
                     ModulatedCommService my_mission,
                     ModulatedQueue modulated_data) {
    super(new PriorityParameters(priority),
          new StorageParameters(BackingStoreRequirements,
                                NativeStackRequirements,
                                JavaStackRequirements));

    this.MODULATED_PRIORITY = priority;
    this.mission = my_mission;
    this.modulated_data = modulated_data;
  }

  private boolean stop_me = false;

  @RunsIn(CALLER)
  public synchronized void requestTermination() {
    stop_me = true;

    // assume shutdowns are delivered top-down
    // comms_data.smc.issueApplicationRequest();
  }

  @RunsIn(CALLER)
  private synchronized boolean terminationRequested() {
    return stop_me;
  }

  @Override
  @RunsIn("MO_Private")
  public final void run() {
    byte[] buffer = null;
    long key = 0L;
    int length = 0;
    boolean queued_operation = false;
    boolean queued_encrypt = false; // else, pending decrypt

    // true iff hardware is currently processing a request
    boolean pending_operation = false;


    // details not shown, but generally, this thread acts as follows.
    //  1. issue initialization commands to the encryption hardware
    //  2. repeatedly:
    //     a) in parallel, wait for a command to arrive from the
    //        application and wait for a previously issued hardware
    //        command to complete its execution
    //     b) if a new command is ready, farm it out to the
    //        hardware if the hardware is not busy.
    //     c) if a previously issued hardware request has
    //        completed, digest the result.  Generate
    //        a response to the application.  If there is another
    //        pending hardware operation, issue it now.
    //
    while (true) {
      ModulatedQueue.RequestEncoding request = modulated_data.awaitRequest();

      // Note: the CommunicationOversight thread always waits for a
      // previously issued request to be serviced before it issues
      // another request.  Thus, we can use a single copy of the
      // buffer, key, and length fields to process this data.
      switch (request) {

        // this is patterned after the body of
        // SecurityOversight.run(), but I'm going to skip the details
        // for now.


      }

      if (terminationRequested()) {
        return;
      }
    }
  }
}
