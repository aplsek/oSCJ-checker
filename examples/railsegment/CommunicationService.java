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

import javax.safetycritical.Mission;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.CALLER;

import static javax.safetycritical.annotate.Scope.UNKNOWN;

/**
 * Assume the CommunicationService class provides reliable
 * communication.  Every message "sent" is guaranteed to be delivered.
 * Every message "received" is guaranteed to be legitimate.
 */
@DefineScope(name="A", parent="TM")
@Scope("A")
public class CommunicationService extends Mission
{
  // These three constants are determined by static analysis or other
  // vendor-specific approaches
  public static final long BackingStoreRequirements = 10000;
  public static final long NativeStackRequirements = 5000;
  public static final long JavaStackRequirements = 2000;

  // This is large enough to represent the four sub-mission sequencers
  // allocated by my initialize method
  public static final long MissionMemorySize = 6000;

  final CommunicationsQueue comms_data;
  final int COMMS_PRIORITY;

  private CypherQueue cypherq;
  private ModulatedQueue modulatedq;
  private SatQueue satq;
  private MobileQueue mobileq;

  CommunicationsOversight management_thread;

  SecurityServiceSequencer cyphersq;
  TrackModulatedCommServiceSequencer trackcommsq;
  SatCommServiceSequencer satcommsq;
  MobileCommServiceSequencer mobilecommsq;

  public CommunicationService(CommunicationsQueue comms_data,
                              final int COMMS_PRIORITY)
  {
    this.comms_data = comms_data;
    this.COMMS_PRIORITY = COMMS_PRIORITY;
  }

  @Override
  public final long missionMemorySize()
  {
    // This must be large enough to hold each of the four submissions,
    // the NHRT, and all auxiliary data structures associated with
    // communication with these missions.
    return MissionMemorySize;
  }

  @Override
  public void initialize()
  {
    cypherq = new CypherQueue(COMMS_PRIORITY);
    cypherq.initialize();

    modulatedq = new ModulatedQueue(COMMS_PRIORITY);
    modulatedq.initialize();

    satq = new SatQueue(COMMS_PRIORITY);
    satq.initialize();

    mobileq = new MobileQueue(COMMS_PRIORITY);
    mobileq.initialize();

    // We'll have one oversight NHRT that monitors comms_data for
    // service requests and assumes responsibility for providing an
    // appropriate response.  This thread delegates each communication
    // service request to one or more sub-missions.
    management_thread = new CommunicationsOversight(COMMS_PRIORITY,
                                                    comms_data, cypherq,
                                                    modulatedq, satq, mobileq);

    // TODO: The ceiling priority of SecurityServiceSequencer needs to
    // be interrupt level, because we're assuming a hardware
    // implementation of encryption services and the interrupt
    // handler that indicates completion of a hardware operation needs
    // to update certain synchronized fields within the
    // SecurityService mission.


    // start up four sub-mission sequencers to represent four kinds of
    // communication services
    cyphersq = new SecurityServiceSequencer(COMMS_PRIORITY, cypherq);
    trackcommsq = new TrackModulatedCommServiceSequencer(COMMS_PRIORITY,
                                                         modulatedq);
    satcommsq = new SatCommServiceSequencer(COMMS_PRIORITY, satq);
    mobilecommsq = new MobileCommServiceSequencer(COMMS_PRIORITY, mobileq);
  }

  @Override
@RunsIn(CALLER)
  public void requestTermination()
  {
    management_thread.requestTermination();

  }
}
