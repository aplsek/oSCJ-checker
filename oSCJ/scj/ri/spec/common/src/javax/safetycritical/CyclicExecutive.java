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

import java.util.Iterator;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.HighResolutionTime;
import javax.safetycritical.annotate.SCJAllowed;

import edu.purdue.scj.VMSupport;
//import edu.purdue.scj.utils.Utils;

/**
 * 
 * Level 0 applications are assumed to be scheduled by a cyclic executive where
 * the schedule is created by static analysis tools offline.
 * 
 * @author plsek
 * 
 */
@SCJAllowed
public abstract class CyclicExecutive extends Mission implements Safelet {

	private MissionSequencer _sequencer;
	private static final int NANOS_PER_MILLI = 1000 * 1000;

	@SCJAllowed
	public CyclicExecutive(StorageParameters storage) {
		_sequencer = new SingleMissionSequencer(null, storage, this);
	}

	@SCJAllowed
	public MissionSequencer getSequencer() {
		return _sequencer;
	}

	@SCJAllowed
	public abstract CyclicSchedule getSchedule(PeriodicEventHandler[] peh);

	/** Do the Cyclic Execution. */
	protected final void exec(MissionManager manager) {
	   //Utils.debugIndentIncrement("###[SCJ] CyclicExecutive.exec");
	    
	    
		if (manager.getHandlers() == 0)
			return;

		PeriodicEventHandler[] handlers = new PeriodicEventHandler[manager
				.getHandlers()];
		PeriodicEventHandler handler = (PeriodicEventHandler) manager
				.getFirstHandler();
		int iter = 0;
		while (handler != null) {
			handlers[iter] = handler;
			handler = (PeriodicEventHandler) handler.getNext();
			iter++;
		}

		CyclicSchedule schedule = getSchedule(handlers);
		CyclicSchedule.Frame[] frames = schedule.getFrames();

		Wrapper wrapper = new Wrapper();
		AbsoluteTime targetTime = Clock.getRealtimeClock().getTime();

		while (_phase == Mission.Phase.EXECUTE) {
			for (int i = 0; i < frames.length; i++) {
				targetTime.add(frames[i].getDuration(), targetTime);
				PeriodicEventHandler[] frameHandlers = frames[i].getHandlers();
				for (int j = 0; j < frameHandlers.length; j++) {
					if (frameHandlers[j] != null) { // we check that handler is
													// not null,
						wrapper._handler = frameHandlers[j];
						
						////Utils.debugPrintln("###[SCJ] CyclicExecutive: run handler");
						wrapper.runInItsInitArea();
					}
				}
				if (_phase != Mission.Phase.EXECUTE)
					break;
				else
					waitForNextFrame(targetTime);
			}
		}
		
		
        ////Utils.decreaseIndent();
	}

	private static void waitForNextFrame(AbsoluteTime targetTime) {
		int result;
		////Utils.debugPrintln("###[SCJ] CyclicExecutive: wait for the next frame");
		
		while (true) {
			result = VMSupport.delayCurrentThreadAbsolute(toNanos(targetTime));
			if (result == -1) {
				break;
			} else if (result == 0)
				break;
			// TODO: here, result == 1, the sleep is interrupted, try to sleep
			// again.
		}
	}

	static long toNanos(HighResolutionTime time) {
		long nanos = time.getMilliseconds() * NANOS_PER_MILLI
				+ time.getNanoseconds();
		if (nanos < 0)
			nanos = Long.MAX_VALUE;

		return nanos;
	}

	/** For making every handler run in its own PrivateMemory */
	class Wrapper implements Runnable {

		PeriodicEventHandler _handler = null;

		void runInItsInitArea() {
			if (_handler != null) // TODO: if we will use the "maxHandlers"
									// fiels in MissionManager, we dont need
									// this.
				_handler.getInitArea().enter(this);
			else {
				////Utils.panic("ERROR: handler is null");
			}
		}

		public void run() {
			try {
				//System.out.println("[SCJ] running hadnerl...");
				
				
				_handler.handleEvent();
			} catch (Throwable t) {
				////Utils.debugPrintln(t.toString());
				t.printStackTrace();
			}
		}
	}
}
