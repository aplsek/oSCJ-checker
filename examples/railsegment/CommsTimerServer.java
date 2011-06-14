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
import static javax.safetycritical.annotate.Scope.CALLER;

import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedThread;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("TM.A")
@DefineScope(name="TM.A.A", parent="TM.A")
@SCJAllowed(value=LEVEL_2, members=true)
class CommsTimerServer extends ManagedThread {

  // Determined by VM-specific static analysis tools
  private static final long BackingStoreRequirements = 500;
  private static final long StackRequirements = 3000;

  final int COMMS_PRIORITY;
  final CommunicationsQueue comms_data;

  final CypherQueue cypherq;
  final ModulatedQueue modulatedq;
  final SatQueue satq;
  final MobileQueue mobileq;

  @SCJRestricted(INITIALIZATION)
  CommsTimerServer(int comms_priority,
                   CommunicationsQueue comms_data,
                   CypherQueue cypherq,
                   ModulatedQueue modulatedq,
                   SatQueue satq,
                   MobileQueue mobileq) {
    super(new PriorityParameters(comms_priority),
          new StorageParameters(BackingStoreRequirements, storageArgs(), 0, 0));

    this.COMMS_PRIORITY = comms_priority;
    this.comms_data = comms_data;
    this.cypherq = cypherq;
    this.modulatedq = modulatedq;
    this.satq = satq;
    this.mobileq = mobileq;
  }

  private static long[] storageArgs() {
    long[] storage_args = {StackRequirements};
    return storage_args;
  }

  private boolean stop_me = false;

  @RunsIn(CALLER)
  public synchronized void requestTermination() {
    stop_me = true;
    comms_data.smc.issueApplicationRequest();
  }

  @RunsIn(CALLER)
  @SCJAllowed
  private synchronized boolean terminationRequested() {
    return stop_me;
  }

  @Override
  @RunsIn("TM.A.A")
  @SCJAllowed(SUPPORT)
  public final void run() {
    // details not shown, but generally, this thread acts as follows.
    //  1. issue initialization commands to modulatedq, satq, mobileq
    //  2. repeatedly:
    //     a) in parallel, wait for a command to arrive from the
    //        application and wait for a previously issued sub-mission
    //        command to complete its execution
    //     b) if a new command is ready, farm it out to the
    //        appropriate sub-mission(s)
    //     c) if a previously issued sub-mission request has been
    //        completed, digest the result.  if appropriate, generate
    //        a response to the application
    //
    // The intent is that this communications oversight thread takes
    // responsibility for load balancing and all necessary redundancy.
    // It may choose to transmit messages on multiple networks in
    // parallel.  It may also receive the same message on multiple
    // networks, in which case it forwards the first received message
    // and discards redundantly received messages.  How long will it
    // remember what has been already received so that it can discard
    // redundant messages?  Let's assume all messages have a ttl
    // attribute, and we can discard anything that is no longer
    // "living"?
    //
    while (true) {
      int status = comms_data.smc.waitForWork();
      if (terminationRequested()) {
        return;
      }
      if ((status & CommunicationsQueue.APPLICATION_REQUEST) != 0) {
        int command_no = comms_data.getCommandNumber();

        // get more information about the command, assign relevant
        // work to the appropriate sub-missions

      }
      if ((status & CommunicationsQueue.MODULATED_SERVICE_DONE) != 0) {



        // take the response, forward along to the application if appropriate
        comms_data.smc.unfinishModulatedService();
      }
      if ((status & CommunicationsQueue.SATELLITE_SERVICE_DONE) != 0) {

        // take the response, forward along to the application if appropriate
        comms_data.smc.unfinishSatelliteService();
      }
      if ((status & CommunicationsQueue.MOBILE_SERVICE_DONE) != 0) {

        // take the response, forward along to the application if appropriate
        comms_data.smc.unfinishMobileService();
      }

      if ((status & CommunicationsQueue.MOBILE_SERVICE_RECEIVED_MESSAGE) != 0) {

        // need to notify application code that there is a message
        // available.

      }
    }
  }
}
