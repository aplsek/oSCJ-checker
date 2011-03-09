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

import javax.safetycritical.Services;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.CURRENT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

// This assumes there is at most one client for NavigationInfo, and
// that the single client always waits for a response to a previously
// issued request before issuing another request.

@Scope("A")
public class CypherQueue {

  // The ceiling for instances of this class needs to be in the
  // interrupt priority range
  @Scope("A")
  static class HardwareCoordination {
    // a guess
    private static final int INTERRUPT_PRIORITY = 32;

    private boolean hardware_idle;
    private RequestEncoding pending_request;
    byte[] encryption_buffer;
    int encryption_length;
    long encryption_key;

    // this holds the status code for the most recently completed
    // hardware operation
    int hardware_status;

    private int int_response;
    private long long_response;

    @RunsIn(CURRENT)
    HardwareCoordination() {
      hardware_idle = true;
      pending_request = RequestEncoding.NoRequest;
    }

    @RunsIn(CURRENT)
    synchronized void initialize() {
      Services.setCeiling(this, INTERRUPT_PRIORITY);
    }

    @RunsIn(UNKNOWN)
    synchronized boolean idle() {
      return hardware_idle;
    }

    @RunsIn(UNKNOWN)
    synchronized void scheduleHardware() {
      hardware_idle = false;
    }

    @RunsIn(UNKNOWN)
    synchronized void finishHardware() {
      hardware_idle = true;
      // set status code to HardwareCompletion
      notifyAll();
    }

    // we need to move the implementation of awaitRequest here,
    // because the implementation includes coordination with hardware,
    // at interrupt-level priorities. 
    @RunsIn(UNKNOWN)
    synchronized RequestEncoding awaitRequest()
    {
      while (pending_request == RequestEncoding.NoRequest) {
        try {
          wait();
        } catch (InterruptedException x) {
          ;                     // try again
        }
      }
      return pending_request;
    }

    @RunsIn(UNKNOWN)
    synchronized int issueDecryptRequest(@Scope(IMMORTAL) byte[] buffer,
                                         int length, long cypher)
      throws InterruptedException
    {
      while ((pending_request != RequestEncoding.NoRequest) &&
             (pending_request != RequestEncoding.TerminateMission)) {
        try {
          wait();
        } catch (InterruptedException x) {
          ;                     // try again
        }
      }
      if (pending_request == RequestEncoding.TerminateMission) {
        // todo: preallocate this
        throw new InterruptedException();
      }
      pending_request = RequestEncoding.Decrypt;
      encryption_key = cypher;
      encryption_buffer = buffer;
      encryption_length = length;
      notifyAll();
      while ((pending_request != RequestEncoding.ResponseReady) &&
             (pending_request != RequestEncoding.TerminateMission)) {
        try {
          wait();
        } catch (InterruptedException x) {
          ;                     // try again
        }
      }
      if (pending_request == RequestEncoding.TerminateMission) {
        // todo: preallocate this
        throw new InterruptedException();
      }
      
      pending_request = RequestEncoding.NoRequest;
      notifyAll();
      return int_response;
    }

    @RunsIn(UNKNOWN)
    synchronized int issueEncryptRequest(@Scope(IMMORTAL) byte[] buffer,
                                         int length, long cypher)
      throws InterruptedException
    {

      // by requiring an immortal buffer, this is instance can keep a reference
      // to it.
      while ((pending_request != RequestEncoding.NoRequest) &&
             (pending_request != RequestEncoding.TerminateMission)) {
        try {
          wait();
        } catch (InterruptedException x) {
          ;                     // try again
        }
      }
      if (pending_request == RequestEncoding.TerminateMission) {
        // todo: preallocate this
        throw new InterruptedException();
      }
      
      pending_request = RequestEncoding.Encrypt;
      encryption_key = cypher;
      encryption_buffer = buffer;
      encryption_length = length;
      
      notifyAll();

      while ((pending_request != RequestEncoding.ResponseReady) &&
             (pending_request != RequestEncoding.TerminateMission)) {
        try {
          wait();
        } catch (InterruptedException x) {
          ;                     // try again
        }
      }
      if (pending_request == RequestEncoding.TerminateMission) {
        // todo: preallocate this
        throw new InterruptedException();
      }

      pending_request = RequestEncoding.NoRequest;
      notifyAll();
      return int_response;
    }

    @RunsIn(UNKNOWN)
    synchronized final void issueShutdownRequest() {
      // unblock all pending threads ...

      pending_request = RequestEncoding.TerminateMission;
      notifyAll();
    }

    // invoked by SecurityService sub-mission
    // no synchronization necessary.  we already waited for notification
    // in awaitRequest().
    @RunsIn(UNKNOWN)
    synchronized final byte[] getBuffer() {
      return encryption_buffer;
    }

    // invoked by SecurityService sub-mission
    @RunsIn(UNKNOWN)
    synchronized final int getBufferLength() {
      return encryption_length;
    }

    // invoked by SecurityService sub-mission
    @RunsIn(UNKNOWN)
    synchronized final long getKey() {
      return encryption_key;
    }

    // invoked by SecurityService sub-mission
    @RunsIn(UNKNOWN)
    synchronized final void serviceRequest(int result) {
      int_response = result;
      pending_request = RequestEncoding.ResponseReady;
      notifyAll();
    }
  
    // invoked by SecurityService sub-mission
    @RunsIn(UNKNOWN)
    synchronized final void serviceRequest(long result) {
      long_response = result;
      pending_request = RequestEncoding.ResponseReady;
      notifyAll();
    }

    @RunsIn(UNKNOWN)
    synchronized final void issueHardwareRequest(boolean encrypt,
                                                 @Scope(IMMORTAL) byte[] buffer,
                                                 int length, long cypher) {
      // issue the request to the hardware


      hardware_idle = false;
    }

    @RunsIn(UNKNOWN)
    synchronized final void awaitHardwareCompletion() {
      while (hardware_idle == false) {
        try {
          wait();
        } catch (InterruptedException x) {
          ;                     // try again
        }
      }
    }


    // return true iff the hardware is currently busy
    @RunsIn(UNKNOWN)
    synchronized final boolean busy() {
      return !hardware_idle;
    }

    // invoked by the hardware interrupt service routine
    @RunsIn(UNKNOWN)
    synchronized final void completeHardwareRequest(int status) {
      hardware_status = status;
      hardware_idle = true;
      notifyAll();
    }

    @RunsIn(UNKNOWN)
    synchronized int getHardwareStatus() {
      return hardware_status;
    }
  }
  // End of HardwareCoordination
  
  // Presumed to reside in IMMORTAL
  static enum RequestEncoding {
    NoRequest, ResponseReady,
    Encrypt, Decrypt,
    HardwareCompletion,
    TerminateMission
    };

  private final int CEILING_PRIORITY;
  private final HardwareCoordination hardware;

  @RunsIn(CURRENT)
  CypherQueue(int ceiling) {
    CEILING_PRIORITY = ceiling;

    hardware = new HardwareCoordination();
    hardware.initialize();
  }

  @RunsIn(CURRENT)
  final void initialize() {
    Services.setCeiling(this, CEILING_PRIORITY);
  }

  /* These are the sorts of service I expect to provide.  I spawn to a
   * separate thread because it may be that the implementation uses
   * hardware encryption implementations, and because I want the
   * temporary memory for the encryption algorithms to be allocated in
   * a separate sub-mission.
   */

  // invoked by CommunicationsOversight thread
  @RunsIn(UNKNOWN)
  final void encrypt(@Scope(IMMORTAL) byte[] buffer, int length, long cypher)
    throws InterruptedException
  {
    hardware.issueEncryptRequest(buffer, length, cypher);
  }

  // invoked by CommunicationsOversight thread
  @RunsIn(UNKNOWN)
  final void decrypt(@Scope(IMMORTAL) byte[] buffer, int length, long cypher)
    throws InterruptedException
  {
    hardware.issueDecryptRequest(buffer, length, cypher);
  }

  // TODO: move implementation into HardwareCoordination.  All the
  // data needs to move there too, I suppose.  (can't have nested
  // synchronization, because that might suspend while holding lock.)

  // invoked by SecurityService sub-mission
  @RunsIn(UNKNOWN)
  final RequestEncoding awaitRequest() {
    return hardware.awaitRequest();
  }

  // invoked by SecurityService sub-mission
  // no synchronization necessary.  we already waited for notification
  // in awaitRequest().
  @RunsIn(UNKNOWN)
  final byte[] getBuffer() {
    return hardware.getBuffer();
  }

  // invoked by SecurityService sub-mission
  @RunsIn(UNKNOWN)
  final int getBufferLength() {
    return hardware.getBufferLength();
  }

  @RunsIn(UNKNOWN)
  // invoked by SecurityService sub-mission
  final long getKey() {
    return hardware.getKey();
  }

  // invoked by SecurityService sub-mission
  @RunsIn(UNKNOWN)
  final void serviceRequest(int result) {
    hardware.serviceRequest(result);
  }
  
  // invoked by SecurityService sub-mission
  @RunsIn(UNKNOWN)
  final void serviceRequest(long long_result) {
    hardware.serviceRequest(long_result);
  }

  // returns true if encryption hardware is busy
  @RunsIn(UNKNOWN)
  final boolean hardwareBusy() {
    return hardware.busy();
  }

  @RunsIn(UNKNOWN)
  final void issueHardwareRequest(boolean encrypt,
                                  @Scope(IMMORTAL) byte[] buffer,
                                  int length, long cypher) {
    hardware.issueHardwareRequest(encrypt, buffer, length, cypher);
  }
    

  // invoked by the hardware interrupt service routine
  @RunsIn(UNKNOWN)
  final void completeHardwareRequest(int status) {
    hardware.completeHardwareRequest(status);
  }

  @RunsIn(UNKNOWN)
  final int getHardwareStatus() {
    return hardware.getHardwareStatus();
  }

  @RunsIn(UNKNOWN)
  synchronized final void issueShutdownRequest() {
    hardware.issueShutdownRequest();
  }
}

