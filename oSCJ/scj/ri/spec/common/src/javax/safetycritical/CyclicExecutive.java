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
import javax.realtime.RealtimeThread;
import javax.safetycritical.annotate.SCJAllowed;

import edu.purdue.scj.VMSupport;
import edu.purdue.scj.utils.Utils;

@SCJAllowed
public abstract class CyclicExecutive extends Mission implements Safelet {

    private MissionSequencer _sequencer;
    private static final int NANOS_PER_MILLI = 1000 * 1000;

    @SCJAllowed
    public CyclicExecutive(StorageConfigurationParameters storage) {
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

        PeriodicEventHandler[] handlers = new PeriodicEventHandler[manager._peHandlers
                .size()];

        int counter = 0;
        for (Iterator i = manager._peHandlers.iterator(); i.hasNext();) {
            handlers[counter] = (PeriodicEventHandler) i.next();
            counter++;
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
                    wrapper._handler = frameHandlers[j];
                    wrapper.runInItsInitArea();
                }
                if (_phase != Mission.Phase.EXECUTE)
                    break;
                else
                    waitForNextFrame(targetTime);
            }
        }
    }

    private static void waitForNextFrame(AbsoluteTime targetTime) {
        int result;
        while (true) {
            result = VMSupport.delayCurrentThreadAbsolute(toNanos(targetTime));
            if (result == -1) {
                break;
            } else if (result == 0)
                break;
            // here, result == 1, the sleep is interrupted, try to sleep
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
            if (_handler != null)
                _handler.getInitArea().enter(this);
            else
                Utils.panic("handler is null");
        }

        public void run() {
            try {
                _handler.handleEvent();
            } catch (Throwable t) {
                Utils.debugPrint(t.toString());
                t.printStackTrace();
            }
        }
    }
}
