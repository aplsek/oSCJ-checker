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

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

// This assumes there is at most one client for NavigationInfo, and
// that the single client always waits for a response to a previously
// issued request before issuing another request.

@Scope("A")
public class MobileQueue {
  static enum RequestEncoding {
    NoRequest, ResponseReady,
    RequestShutdown,
    Transmit, Receive
    };

  private final int CEILING_PRIORITY;

  private RequestEncoding pending_request;
  private int int_response;
  private long long_response;

  @Scope(IMMORTAL)
  byte[] pending_xmit_buffer;
  int pending_channel_no;
  int pending_length;

  MobileQueue(int ceiling) {
    CEILING_PRIORITY = ceiling;
    pending_request = RequestEncoding.NoRequest;
  }

  @RunsIn(CURRENT)
  void initialize() {
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
  int transmit(@Scope(IMMORTAL) byte[] buffer, int length, int channel_no) {
    // by requiring an immortal buffer, this instance can keep a reference
    // to it.
    while (pending_request != RequestEncoding.NoRequest) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }

    pending_request = RequestEncoding.Transmit;
    pending_channel_no = channel_no;
    pending_length = length;

    notifyAll();
    while (pending_request != RequestEncoding.ResponseReady) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }
    pending_request = RequestEncoding.NoRequest;
    return int_response;
  }

  // invoked by CommunicationsOversight thread
  @RunsIn(UNKNOWN)
  int receive(@Scope(IMMORTAL) byte[] buffer, int length, int channel_no) {
    // by requiring an immortal buffer, this instance can keep a reference
    // to it.
    while (pending_request != RequestEncoding.NoRequest) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }

    pending_request = RequestEncoding.Receive;
    pending_channel_no = channel_no;
    pending_length = length;

    notifyAll();
    while (pending_request != RequestEncoding.ResponseReady) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }
    pending_request = RequestEncoding.NoRequest;
    return int_response;
  }

  // invoked by CommunicationsOversight thread
  @RunsIn(UNKNOWN)
  synchronized void requestTermination() {
    pending_request = RequestEncoding.RequestShutdown;
    notifyAll();
  }


  // invoked by MobileService sub-mission
  @RunsIn(UNKNOWN)
  synchronized RequestEncoding awaitRequest() {
    while (pending_request == RequestEncoding.NoRequest) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }
    return pending_request;
  }

  // invoked by MobileService sub-mission
  // no synchronization necessary.  we already waited for notification
  // in awaitRequest().
  @RunsIn(UNKNOWN) @Scope(IMMORTAL)
  byte[] getBuffer() {
    return pending_xmit_buffer;
  }

  // invoked by MobileService sub-mission
  @RunsIn(UNKNOWN)
  int getBufferLength() {
    return pending_length;
  }

  // invoked by MobileService sub-mission
  @RunsIn(UNKNOWN)
  int getChannel() {
    return pending_channel_no;
  }

  // invoked by MobileService sub-mission
  @RunsIn(UNKNOWN)
  synchronized void serviceRequest(int result) {
    int_response = result;
    pending_request = RequestEncoding.ResponseReady;
    notifyAll();
  }

  // invoked by MobileService sub-mission
  @RunsIn(UNKNOWN)
  synchronized void serviceRequest(long result) {
    long_response = result;
    pending_request = RequestEncoding.ResponseReady;
    notifyAll();
  }
}
