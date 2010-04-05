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


/**
 * 
 * This is just a skeleton class, see in SCJ for the RealtimeThread class.
 * 
 * 
 */

package javax.realtime;

public class RealtimeThread extends Thread {

	 //  new RealtimeThread((VMThread) this, null, priority, daemon);
    public RealtimeThread(VMThread vmThread, String name, int priority, boolean daemon) {
        super(vmThread, null, priority, daemon);
     // method stub
    }

    
	public void completeInitialization() {
		// method stub
	}
	
}
