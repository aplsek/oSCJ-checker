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
import javax.realtime.PriorityParameters;
import javax.realtime.ReleaseParameters;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(Level.LEVEL_0)
public abstract class ManagedEventHandler extends BoundAsyncEventHandler {

    private String _name;

    public ManagedEventHandler(PriorityParameters priority,
            ReleaseParameters release, StorageParameters storage,
            long psize, String name) {
        super(priority, release, null, new PrivateMemory(psize), null, true,
                null);
        _name = name;
        MissionManager.getCurrentMissionManager().addEventHandler(this);
    }

    /**
     * Application developers override this method with code to be executed
     * whenever the event(s) to which this event handler is bound is fired.
     */
    @SCJAllowed(Level.LEVEL_0)
    public abstract void handleEvent();

    /**
     * This is overridden to ensure entry into the local scope for each release.
     * This may change for RTSJ 1.1, where a provided scope is automatically
     * entered at each release.
     */
    @Override
    @SCJAllowed(Level.LEVEL_0)
    public final void handleAsyncEvent() {
        handleEvent();
    }

    @SCJAllowed(Level.LEVEL_0)
    public void cleanup() {
    }

    public String getName() {
        return _name;
    }

    PrivateMemory getInitArea() {
        return (PrivateMemory) getInitMemoryArea();
    }

    void join() {
    }
}
