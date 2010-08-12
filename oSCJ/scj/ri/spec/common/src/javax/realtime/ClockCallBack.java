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
package javax.realtime;

import javax.safetycritical.annotate.SCJRestricted;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;

/**
 * The ClockEvent interface may be used by subclasses of Clock to indicate to
 * the clock infrastructure that the clock has either reached a designated time,
 * or has experienced a discontinuity.
 * 
 * TODO: implement it when necessary
 * 
 */
@SCJAllowed
public interface ClockCallBack {
	//
	// /**
	// * Clock has reached the designated time.
	// *
	// * @param clock
	// */
	//    
	//
	@SCJAllowed(LEVEL_1)
	@SCJRestricted()
	void atTime(Clock clock);
	//
	//
	// /**
	// * clock experienced a time discontinuity.
	// *
	// * @param clock
	// * @param updatedTime
	// */
	//    
	@SCJAllowed(LEVEL_1)
	@SCJRestricted()
	void discontinuity(Clock clock, AbsoluteTime updatedTime);
}
