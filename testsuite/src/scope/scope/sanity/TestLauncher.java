package scope.scope.sanity;
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
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.*;

@SCJAllowed(members=true)
public class TestLauncher {

    @SCJRestricted(INITIALIZATION)
    @RunsIn(IMMORTAL)
    public static void main(final String[] args) {

        Safelet safelet = new MySafelet();
        safelet.setUp();
        safelet.getSequencer().start();
    }

    @SCJAllowed(members=true)
    @Scope(IMMORTAL)
    @DefineScope(name="cdx.Level0Safelet",parent=IMMORTAL)
    public static class MySafelet extends CyclicExecutive {

        public MySafelet() {
            super(null, null);
        }

        @SCJAllowed(SUPPORT)
        @SCJRestricted(INITIALIZATION)
        public void setUp() {
        }

        @SCJAllowed(SUPPORT)
        @SCJRestricted(CLEANUP)
        public void tearDown() {
        }


        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("cdx.Level0Safelet")
        public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
            return null;
        }

        @Override
        @RunsIn("cdx.Level0Safelet")
        @SCJAllowed(SUPPORT)
        protected void initialize() {
        }


        @Override
        public long missionMemorySize() {
            return 0;
        }



    }
}
