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

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class ThrowBoundaryError extends javax.realtime.ThrowBoundaryError {

	@SCJAllowed
	public ThrowBoundaryError() {
	}

	@SCJAllowed
	public String getPropagatedMessage() {
		return null;
	}

	@SCJAllowed
	public StackTraceElement[] getPropagatedStackTrace() {
		return null;
	}

	@SCJAllowed
	public int getPropagatedStackTraceDepth() {
		return 0;
	}

	@SCJAllowed
	public Class getPropagatedExceptionClass() {
		return null;
	}
}
