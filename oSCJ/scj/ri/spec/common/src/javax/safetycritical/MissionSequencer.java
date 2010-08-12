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

import javax.realtime.BoundAsyncEventHandler;
import javax.realtime.MemoryArea;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.safetycritical.annotate.SCJAllowed;

//import edu.purdue.scj.utils.Utils;


import static javax.safetycritical.annotate.Level.LEVEL_2;


@SCJAllowed
public abstract class MissionSequencer extends BoundAsyncEventHandler {

	/**
	 * NOTE!!! 
	 * We create a MemoryArea with a "dummy size" = 0, since the mission memory is 
	 * created only once in the lifetime of the application ... and we reuse it for different mission
	 * when calling handleAsyncEvent we first resize the mission memory to the actual size needed
	 * by the mission being executed and then we enter this resized area!.
	 */
	private MissionMemory _mem = new MissionMemory(0);
	private MissionWrapper _wrapper = new MissionWrapper();
	private Mission _mission;

	class MissionWrapper implements Runnable {

		private Mission _mission;

		void setMission(Mission mission) {
			_mission = mission;
		}

		public void run() {
			_mission.run();
		}
	}

	// TODO: do something to the storage parameter
	@SCJAllowed
	public MissionSequencer(PriorityParameters priority,
			StorageParameters storage) {
		super(priority, null, null, null, null, true, null);
		MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
	}

	/** user can call this method on Level 2 */
	@SCJAllowed(LEVEL_2)
	public final void start() {
		// TODO: note this does not work for nested mission on Level 2
		handleAsyncEvent();
	}

	@SCJAllowed
	public final void handleAsyncEvent() {
	    ////Utils.debugIndentIncrement("###[SCJ] MissionSequencer.handleAsyncEvent");
	    
	    _mission = getInitialMission();
		while (_mission != null) {
		    ////Utils.debugPrintln("Mission enter, size:" + _mission.missionMemorySize());
		    
		    _wrapper.setMission(_mission);
			_mem.resize(_mission.missionMemorySize());
			_mem.enter(_wrapper);
			if (_mission._terminateAll)
				break;
			_mission = getNextMission();
		}
		////Utils.decreaseIndent();
	}

	@SCJAllowed(LEVEL_2)
	public final void requestSequenceTermination() {
		_mission.requestSequenceTermination();
	}

	

	@SCJAllowed
	protected abstract Mission getInitialMission();

	@SCJAllowed
	protected abstract Mission getNextMission();
}
