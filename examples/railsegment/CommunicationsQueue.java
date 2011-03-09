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
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.CURRENT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@SCJAllowed
@Scope("TM")
public class CommunicationsQueue
{
  public final static int APPLICATION_REQUEST = 0x01;
  public final static int MODULATED_SERVICE_DONE = 0x02;
  public final static int SATELLITE_SERVICE_DONE = 0x04;
  public final static int MOBILE_SERVICE_DONE = 0x08;
  
  // Indicate that a message has been received by the corresponding
  // channel. 
  public final static int MODULATED_SERVICE_RECEIVED_MESSAGE = 0x10;
  public final static int SATELLITE_SERVICE_RECEIVED_MESSAGE = 0x20;
  public final static int MOBILE_SERVICE_RECEIVED_MESSAGE = 0x40;

  public final static int RD_ONLY = 0;
  public final static int WR_ONLY = 1;
  

  private final static int MAXIMUM_FILES = 40;

  @Scope("TM")
  static class SubmissionCoordination {

    int status;

    @RunsIn(IMMORTAL)
    SubmissionCoordination() {
      status = 0;
    }

    // This is how I implement the equivalent of Unix select.  This is
    // invoked from the CommunicationsOversight thread.
    @RunsIn(UNKNOWN)
    final synchronized int waitForWork() {
      while (status == 0) {
        try {
          wait();
        } catch (InterruptedException x) {
          ;                     // resume wait
        }
      }
      return status;
    }

    // invoked by Application component
    @RunsIn(UNKNOWN)
    final synchronized void issueApplicationRequest() {
      status |= APPLICATION_REQUEST;
      notifyAll();
    }

    // invoked by CommunicationOversight component
    // Important to avoid a race condition here:
    //  Suppose the application issues an application request
    //  The CommunicationsOversight thread recocgnizes the request and
    //    begins to service it.  At this point, the
    //    CommunicationsOversight thread invokes
    //    unissueApplicationRequest. The application waits on a
    //    different condition for the response to the issued request.
    @RunsIn(UNKNOWN)
    final synchronized void unissueApplicationRequest() {
      status &= ~APPLICATION_REQUEST;
    }

    // invoked by ModulatedServices submission
    @RunsIn(UNKNOWN)
    final synchronized void finishModulatedService() {
      status |= MODULATED_SERVICE_DONE;
      notifyAll();
    }

    // invoked by CommunicationsOversight
    @RunsIn(UNKNOWN)
    final synchronized void unfinishModulatedService() {
      status &= ~MODULATED_SERVICE_DONE;
    }

    // invoked by SatelliteServices submission
    @RunsIn(UNKNOWN)
    final synchronized void finishSatelliteService() {
      status |= SATELLITE_SERVICE_DONE;
      notifyAll();
    }

    // invoked by CommunicationsOversight
    @RunsIn(UNKNOWN)
    final synchronized void unfinishSatelliteService() {
      status &= ~SATELLITE_SERVICE_DONE;
    }

    // invoked by MobileServices submission
    @RunsIn(UNKNOWN)
    final synchronized void finishMobileService() {
      status |= MOBILE_SERVICE_DONE;
      notifyAll();
    }

    // invoked by CommunicationsOversight
    @RunsIn(UNKNOWN)
    final synchronized void unfinishMobileService() {
      status &= ~MOBILE_SERVICE_DONE;
    }

    
    int mobile_message_count = 0;
    int modulated_message_count = 0;
    int satellite_message_count = 0;

    // Note the protocol for notifying receipt of messages uses a
    // count.  This addresses the "problem" that multiple messages may
    // be received before (or while) a first message is (being)
    // digested.  We don't want a notification to get lost because it
    // is mistakenly canceled (or perceived as redundant).


    // invoked by MobileServices sub-mission
    @RunsIn(UNKNOWN)
    final synchronized void mobileMessageReady() {
      mobile_message_count++;
      status |= MOBILE_SERVICE_RECEIVED_MESSAGE;
      notifyAll();
    }

    // invoked by ModulatedServices sub-mission
    @RunsIn(UNKNOWN)
    final synchronized void modulatedMessageReady() {
      mobile_message_count++;
      status |= MODULATED_SERVICE_RECEIVED_MESSAGE;
      notifyAll();
    }

    // invoked by SatelliteServices sub-mission
    @RunsIn(UNKNOWN)
    final synchronized void satelliteMessageReady() {
      mobile_message_count++;
      status |= SATELLITE_SERVICE_RECEIVED_MESSAGE;
      notifyAll();
    }

    // invoked by CommunicationsOversight
    @RunsIn(UNKNOWN)
    final synchronized void acknowledgeMobileMessage() {
      mobile_message_count--;
      if (mobile_message_count <= 0) {
        status &= ~MOBILE_SERVICE_RECEIVED_MESSAGE;
      }
    }

    // invoked by CommunicationsOversight
    @RunsIn(UNKNOWN)
    final synchronized void acknowledgeModulatedMessage() {
      modulated_message_count--;
      if (modulated_message_count <= 0) {
        status &= ~MODULATED_SERVICE_RECEIVED_MESSAGE;
      }
    }

    // invoked by CommunicationsOversight
    @RunsIn(UNKNOWN)
    final synchronized void acknowledgeSatelliteMessage() {
      satellite_message_count--;
      if (satellite_message_count <= 0) {
        status &= ~SATELLITE_SERVICE_RECEIVED_MESSAGE;
      }
    }
  }

  public final static int O_RDONLY = 0x01;
  public final static int O_WRONLY = 0x02;

  // Purdue team: all enums are presumed to reside in IMMORTAL?
  private static enum RequestType {
    REQUEST_READ,
    REQUEST_WRITE,
    REQUEST_WRITE_SOCKET,
    REQUEST_READ_SOCKET,
    CLOSE_SOCKET,
    READ_COMPLETED,
    WRITE_COMPLETED,
    SOCKET_COMPLETED,
    };

  final SubmissionCoordination smc;

  final int BUFFER_LENGTH;
  final int BUFFER_COUNT;
  
  int free_buffers, reserved_buffers, command_buffers, response_buffers;
  int free_count, reserved_count, command_count, response_count;

  byte[][] buffers;

  int[] buffer_lists;
  int[] command_args;
  int[] file_numbers;
  int response_codes[];
  RequestType[] activity_codes;

  // todo: why do I have SubmissionCoordination?  Is it supposed to
  // have a different ceiling priority than me?
  private int my_ceiling;

  @RunsIn(CURRENT)
  CommunicationsQueue(int num_buffers, int buffer_length, int ceiling_priority)
  {
    smc = new SubmissionCoordination();
    Services.setCeiling(smc, ceiling_priority);

    my_ceiling = ceiling_priority;

    BUFFER_COUNT = num_buffers;
    BUFFER_LENGTH = buffer_length;
    buffers = new byte[num_buffers][buffer_length];

    buffer_lists = new int[num_buffers];
    for (int i = 0; i < num_buffers; i++) {
      buffer_lists[i] = i+1;
    }
    buffer_lists[num_buffers-1] = -1;
    free_buffers = 0;
    reserved_buffers = -1;
    command_buffers = -1;
    response_buffers = -1;

    free_count = num_buffers;
    reserved_count = 0;
    command_count = 0;
    response_count = 0;

    command_args = new int[num_buffers];
    activity_codes = new RequestType[num_buffers];
    response_codes = new int[num_buffers];
    file_numbers = new int[num_buffers];
  }

  @RunsIn(CURRENT)
  void initialize() {
    Services.setCeiling(this, my_ceiling);
  }

  // In order to transmit data, application components must first copy
  // the requested data into a reserved buffer.
  @RunsIn(UNKNOWN)
  private synchronized int reserveBuffer() {
    int reserved_index;

    while (free_count <= 0) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }

    reserved_index = free_buffers;
    free_buffers = buffer_lists[reserved_index];
    free_count--;

    buffer_lists[reserved_index] = reserved_buffers;
    reserved_buffers = reserved_index;
    reserved_count++;

    return reserved_index;
  }

  // assume trustworthy clients...
  @RunsIn(UNKNOWN) @Scope(IMMORTAL)
  private byte[] getBuffer(int index) {
    return buffers[index];
  }

  @RunsIn(UNKNOWN)
  private synchronized void issueRequest(int buffer_no, RequestType command) {

    activity_codes[buffer_no] = command;

    // remove from the reserved buffer
    int prev_ndx = -1;
    for (int response_ndx = reserved_buffers; true;
         response_ndx = buffer_lists[response_ndx]) {
      if (response_ndx == buffer_no) {
        if (prev_ndx > 0) {
          buffer_lists[prev_ndx] = buffer_lists[response_ndx];
        } else {
          reserved_buffers = buffer_lists[response_ndx];
        }
        reserved_count--;
        break;
      }
      else if (response_ndx < 0) {
        internalError("Buffer not found on reserved list");
      }
    }

    // and add buffer to command list
    buffer_lists[buffer_no] = command_buffers;
    command_buffers = buffer_no;
    command_count++;
    smc.issueApplicationRequest();
  }

  @RunsIn(UNKNOWN)
  private synchronized void freeBufferFromResponse(int buffer_no) {

    // remove buffer from response list
    int prev_ndx = -1;
    for (int response_ndx = response_buffers; true;
         response_ndx = buffer_lists[response_ndx]) {
      if (response_ndx == buffer_no) {
        if (prev_ndx > 0) {
          buffer_lists[prev_ndx] = buffer_lists[response_ndx];
        } else {
          response_buffers = buffer_lists[response_ndx];
        }
        response_count--;
        break;
      }
      else if (response_ndx < 0) {
        internalError("Buffer not found on response list");
      }
    }

    // and add buffer to free list
    buffer_lists[buffer_no] = free_buffers;
    free_buffers = buffer_no;
    free_count++;
    notifyAll();
  }
                                       
  @RunsIn(UNKNOWN)
  private synchronized void awaitResponse(int buffer_no, RequestType initial) {
    while (activity_codes[buffer_no] == initial) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }
  }

  @RunsIn(UNKNOWN)
  final private void internalError(String msg) {
    // not sure how to print out an error message
    // Note that System.exit() is @SCJAllowed, surprisingly...
    System.exit(-1);
  }


  //
  // The following four methods are invoked by application threads
  //

  /**
   * Returns the assigned file_no, or -1 if an error condition is
   * encountered.  I allow the implementation of the communication
   * services to decide whether the supplied channel_name is expressed
   * in an appropriate syntax.
   */
  @RunsIn(UNKNOWN)
  public int open(byte[] channel_name, int byte_count, int mode) {
    if ((mode == RD_ONLY) || (mode == WR_ONLY)) {
      if (byte_count > BUFFER_LENGTH) {
        return -1;
      }
      int buffer_no = reserveBuffer();
      byte buffer[] = getBuffer(buffer_no);
      for (int i = 0; i < byte_count; i++) {
        buffer[i] = channel_name[i];
      }
      command_args[buffer_no] = byte_count;

      RequestType request = ((mode == RD_ONLY)?
                             RequestType.REQUEST_READ_SOCKET:
                             RequestType.REQUEST_WRITE_SOCKET); 
      issueRequest(buffer_no, request);
      awaitResponse(buffer_no, request);

      int result = response_codes[buffer_no];
      freeBufferFromResponse(buffer_no);
      return result;
    }
    else {
      return -1;
    }
  }

  @RunsIn(UNKNOWN)
  public int write(int file_no, byte[] buffer, int byte_count) {
    int buffer_no = reserveBuffer();
    byte[] internal_buffer = getBuffer(buffer_no);
    for (int i = 0; i < byte_count; i++) {
      internal_buffer[i] = buffer[i];
    }
    command_args[buffer_no] = byte_count;
    file_numbers[buffer_no] = file_no;
    issueRequest(buffer_no, RequestType.REQUEST_WRITE);
    awaitResponse(buffer_no, RequestType.REQUEST_WRITE);

    int result = response_codes[buffer_no];
    freeBufferFromResponse(buffer_no);
    return result;
  }

  @RunsIn(UNKNOWN)
  public int read(int file_no, byte[] buffer, int byte_count) {
    int buffer_no = reserveBuffer();
    byte[] internal_buffer = getBuffer(buffer_no);
    command_args[buffer_no] = byte_count;
    file_numbers[buffer_no] = file_no;
    issueRequest(buffer_no, RequestType.REQUEST_READ);
    awaitResponse(buffer_no, RequestType.REQUEST_READ);

    int result = response_codes[buffer_no];
    for (int i = 0; i < result; i++) {
      buffer[i] = internal_buffer[i];
    }
    freeBufferFromResponse(buffer_no);
    return result;
  }

  @RunsIn(UNKNOWN)
  public int close(int file_no) {
    int buffer_no = reserveBuffer();
    command_args[buffer_no] = file_no;
    issueRequest(buffer_no, RequestType.CLOSE_SOCKET);
    awaitResponse(buffer_no, RequestType.CLOSE_SOCKET);
    int result = response_codes[buffer_no];
    freeBufferFromResponse(buffer_no);
    return result;
  }

  public static final int INPUT_DATA_READY = 0x01;
  public static final int OUTPUT_BUFFER_EMPTY = 0x02;

  private int file_status[] = new int[MAXIMUM_FILES];

  @RunsIn(UNKNOWN)
  public synchronized int fileStatus(int file_no) {
    return file_status[file_no];
  }

  //
  // The following six methods are invoked by the implementation of
  // Communications Services
  //

  // there should really be some better security on this, but i'll
  // worry about that later...


  @RunsIn(UNKNOWN)
  public final synchronized void setFileStatus(int file_no, int status) {
    file_status[file_no] = status;
  }

  /**
   * Return the buffer_no of the issued command
   */
  @RunsIn(UNKNOWN)
  public final synchronized int getCommandNumber() {
    while (command_count <= 0) {
      try {
        wait();
      } catch (InterruptedException x) {
        ;                     // resume wait
      }
    }

    int command_ndx = command_buffers;

    // Remove from command buffer, but leave in limbo
    // remove from the reserved buffer
    command_buffers = buffer_lists[command_ndx];
    command_count--;

    if (command_count == 0) {
      smc.unissueApplicationRequest();
    }
    return command_ndx;
  }

  @RunsIn(UNKNOWN)
  public final RequestType getCommandCode(int command_ndx) {
    return activity_codes[command_ndx];
  }

  @RunsIn(UNKNOWN) @Scope(IMMORTAL)
  public final byte[] getCommandBuffer(int command_ndx) {
    return buffers[command_ndx];
  }

  @RunsIn(UNKNOWN) 
  public final int getCommandArg(int command_ndx) {
    return command_args[command_ndx];
  }

  @RunsIn(UNKNOWN)
  public final synchronized void
  putCommandResponse(int command_ndx, int response_code)
  {
    response_codes[command_ndx] = response_code;

    buffer_lists[command_ndx] = response_buffers;
    response_buffers = command_ndx;
    response_count++;
    notifyAll();
  }

  
}