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

import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.LTMemory;
import javax.realtime.SizeEstimator;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * Facts:
 * 
 * 1. Only one thread (launch thread) is possible to enter the mission memory
 * 
 * 2. Can have children of MissionMemory and PrivateMemory
 * 
 * 3. Can be allocated in MissionMemory (sub-mission) or ImmortalMemory (top
 * mission)
 * 
 * 4. All schedulable objects will be allocated in MissionMemory
 */
@SCJAllowed(LEVEL_0)
class MissionMemory extends LTMemory implements ManagedMemory {


	@SCJAllowed(LEVEL_1)
	public MissionMemory(SizeEstimator size) {
		super(size);
	}

	MissionMemory(long sizeInByte) {
		super(sizeInByte);
	}

	@SCJAllowed
	public final void enter(Runnable logic) {
		super.enter(logic);
	}

	void resize(long sizeInByte) {
		setSize(sizeInByte);
	}

	@SCJAllowed
	public MissionManager getManager() {
		// TODO: check type
		return (MissionManager) getPortal();
	}

	void setManager(MissionManager manager) {
		setPortal(manager);
	}

	
}
