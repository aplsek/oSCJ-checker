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
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.NoHeapRealtimeThread;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("E")
@DefineScope(name="SEC_Private", parent="E")
class SecurityOversight extends NoHeapRealtimeThread {

  // Determined by VM-specific static analysis tools
  private static final int BackingStoreRequirements = 500;
  private static final int NativeStackRequirements = 2000;
  private static final int JavaStackRequirements = 300;

  final int CYPHER_PRIORITY;
  final CypherQueue cypher_data;
  final SecurityService mission;

  SecurityOversight(int cypher_priority,
                    SecurityService my_mission,
                    CypherQueue cypher_data) {
    super(new PriorityParameters(cypher_priority),
          new StorageParameters(BackingStoreRequirements,
                                NativeStackRequirements,
                                JavaStackRequirements));

    this.CYPHER_PRIORITY = cypher_priority;
    this.mission = my_mission;
    this.cypher_data = cypher_data;
  }

  private boolean stop_me = false;

  @RunsIn(CALLER)
  public synchronized void requestTermination() {
    stop_me = true;
    // assume termination request is propagated top down.
    //   comms_data.smc.issueApplicationRequest();
  }

  @RunsIn(CALLER)
  private synchronized boolean terminationRequested() {
    return stop_me;
  }

  @Override
  @RunsIn("SO_Private")
  public final void run() {
    @Scope(IMMORTAL) byte[] buffer = null;
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
      CypherQueue.RequestEncoding request = cypher_data.awaitRequest();

      // Note: the CommunicationOversight thread always waits for a
      // previously issued request to be serviced before it issues
      // another request.  Thus, we can use a single copy of the
      // buffer, key, and length fields to process this data.
      switch (request) {

        case Encrypt: {
          buffer = cypher_data.getBuffer();
          key = cypher_data.getKey();
          length = cypher_data.getBufferLength();

          if (cypher_data.hardwareBusy()) {
            // "queue" this request until the hardware is ready ...
            queued_operation = true;
            queued_encrypt = true;
          }
          else {
            // issue the request to hardware if hardware is idle ...
            cypher_data.issueHardwareRequest(true, buffer, length, key);
            pending_operation = true;
          }
          break;
        }
        case Decrypt: {
          buffer = cypher_data.getBuffer();
          key = cypher_data.getKey();
          length = cypher_data.getBufferLength();

          if (cypher_data.hardwareBusy()) {
            // "queue" this request until the hardware is ready ...
            queued_operation = true;
            queued_encrypt = false;
          }
          else {
            // issue the request to hardware if hardware is idle ...
            cypher_data.issueHardwareRequest(false, buffer, length, key);
            pending_operation = true;
          }
          break;
        }
        case HardwareCompletion: {
          if (pending_operation) {
            // get status code from the hardware
            int status = cypher_data.getHardwareStatus();

            int result = status; // more generally, some function of status
            cypher_data.serviceRequest(result);
            pending_operation = false;
          }
          else {
            // spontaneous unexpected ISR
            internalError();

          }
        }
        case TerminateMission:
          return;

        case ResponseReady:
        case NoRequest:
          // these codes should not occur
          internalError();
      }


      if (terminationRequested()) {
        return;
      }
    }
  }


  @RunsIn(CALLER)
  private void internalError() {
    System.exit(-1);
  }

}
