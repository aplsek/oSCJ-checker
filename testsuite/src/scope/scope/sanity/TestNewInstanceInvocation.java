/**
 *  This file is part of miniCDx benchmark of oSCJ.
 *
 *   miniCDx is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   miniCDx is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with miniCDx.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010
 *   @authors  Daniel Tang, Ales Plsek
 *
 *   See: http://sss.cs.purdue.edu/projects/oscj/
 */
package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.*;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name="cdx.Level0Safelet",parent=IMMORTAL)
public class TestNewInstanceInvocation extends CyclicExecutive {

    public TestNewInstanceInvocation() {
        super(null);
    }

    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public void setUp() {

        new ImmortalEntry().run();
    }

    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void tearDown() {
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("cdx.Level0Safelet")
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        CyclicSchedule.Frame[] frames = new CyclicSchedule.Frame[1];
        frames[0] = new CyclicSchedule.Frame(new RelativeTime(100, 0), handlers);
        CyclicSchedule schedule = new CyclicSchedule(frames);
        return schedule;
    }

    @Override
    @RunsIn("cdx.Level0Safelet")
    @SCJAllowed(SUPPORT)
    protected void initialize() {
    }


    @Override
    @SCJAllowed(SUPPORT)
    public long missionMemorySize() {
        return 0;
    }

    @SCJAllowed(members=true)
    @Scope(IMMORTAL)
    public static class ImmortalEntry implements Runnable {
        /** Called only once during initialization. Runs in immortal memory */
        public void run() {

        }
    }

}
