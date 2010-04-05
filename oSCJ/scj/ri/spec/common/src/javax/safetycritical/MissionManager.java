/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */
package javax.safetycritical;

import java.util.ArrayList;
import java.util.Collection;

import javax.realtime.RealtimeThread;
import javax.safetycritical.annotate.SCJAllowed;

import edu.purdue.scj.utils.Utils;

@SCJAllowed
// public abstract class MissionManager {
public class MissionManager extends PortalExtender {

	private Mission _mission;
	
	Collection _peHandlers = new ArrayList(); // PeriodicEventHandler

	

	public MissionManager() {
	}

	MissionManager(Mission mission) {
		_mission = mission;
	}

	public Mission getMission() {
		return _mission;
	}

	void startAll() {
		
	}

	void cleanAll() {
		
	}

	

	void addEventHandler(ManagedEventHandler handler) {
		if (handler instanceof PeriodicEventHandler)
			_peHandlers.add((PeriodicEventHandler) handler);
	}


	static MissionManager getCurrentMissionManager() {
		return ((ManagedMemory) RealtimeThread.getCurrentMemoryArea())
				.getManager();
	}
}
