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

import javax.realtime.RelativeTime;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * At Level 0, a cyclic schedule is represented by this class. There is one
 * schedule for each processor in the system. The current class assumes a single
 * processor.
 * 
 * @author plsek
 * 
 */
@SCJAllowed
public class CyclicSchedule {

	private Frame[] _frames;

	@SCJAllowed
	public CyclicSchedule(Frame[] frames) {
		_frames = frames;
	}

	Frame[] getFrames() {
		return _frames;
	}

	@SCJAllowed
	final public static class Frame {

		private RelativeTime _duration;
		private PeriodicEventHandler[] _handlers;

		@SCJAllowed
		public Frame(RelativeTime duration, PeriodicEventHandler[] handlers) {
			_duration = duration;
			_handlers = handlers;
		}

		@SCJAllowed
		public PeriodicEventHandler[] getHandlers() {
			return _handlers;
		}

		@SCJAllowed
		public RelativeTime getDuration() {
			return _duration;
		}
	}
}
