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

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;

//import edu.purdue.scj.utils.Utils;

/**
 * In SCJ, all asynchronous events must have their handlers bound when they are
 * cre- ated (during the initialization phase). The binding is permanent. Thus,
 * the AsyncEvent- Handler constructors are hidden from public view in the SCJ
 * specification.
 * 
 * 
 * TODO: AEH here has a dedicated real-time thread, which does not follow the
 * spec. We do this for simplicity, but it is expected that bounding should be
 * done dynamically.
 * 
 * LEVEL: is defined at LEVEL 0 just because of the class structure
 * (PeriodicEventHandler and MissionSequencer extend this ) - other than this
 * class-hierarchy, AsynchEvents are used at LEVEL 1
 */
@SCJAllowed
public class AsyncEventHandler implements Schedulable {

	private final MemoryArea _initMemory;

	/** Our bound thread, created at first release */
	private RealtimeThread _handler = null;

	/** Flag that tells the handler there is work to do */
	private boolean _noWork = true; // access while holding 'lock'

	private boolean _threadStarted = false;

	/** Internal lock object for synchronization */
	protected final Object _lock = new Object();

	/** The fire count for this event */
	protected int _fireCount; // access while holding 'lock'

	/** The Runnable bound to this event handler - if any */
	protected final Runnable _logic;

	/**
	 * Changes because of the change of the constructor, we are initializing the
	 * _handler not in the constructor but in the "handleEvent" when its called
	 * for the first time - which is done at Level 1, at Level 0 its not needed
	 * at all.
	 */
	private SchedulingParameters _scheduling;
	private ReleaseParameters _release;
	private MemoryParameters _memory;
	private ProcessingGroupParameters _group;

	/** The logic executed by our handler */
	Runnable handlerLogic = new Runnable() {
		public void run() {
			while (!Thread.interrupted()) {

				// we don't need or want to hold the lock while processing
				AsyncEventHandler.this.run();
			}
		}
	};

	public AsyncEventHandler() {
		this(null, null, null, null, null, true, null);
	}

	public AsyncEventHandler(SchedulingParameters scheduling,
			ReleaseParameters release, MemoryParameters memory,
			MemoryArea area, ProcessingGroupParameters group, boolean nonHeap,
			Runnable logic) {
		_initMemory = area;
		_logic = logic;
		_group = group;
		_memory = memory;
		_release = release;
		_scheduling = scheduling;
	}

	@SCJAllowed
	public ReleaseParameters getReleaseParameters() {
		return _handler.getReleaseParameters();
	}

	@SCJAllowed
	public SchedulingParameters getSchedulingParameters() {
		return _handler.getSchedulingParameters();
	}

	/**
	 * Spec says: This is overridden by the application to provide the handling code.
	 */
	public void handleAsyncEvent() {
		if (_logic != null)
			_logic.run();
	}

	/** Infrastructure code. Must not be called. */
	@SCJAllowed(INFRASTRUCTURE)
	public final void run() {
		while (getAndDecrementPendingFireCount() > 0)
			handleAsyncEvent();
	}

	// there should not be daemon thread in SCJ
	// public final void setDaemon(boolean on) {
	// _handler.setDaemon(on);
	// }

	/** This is never released at L0, only once - for the MissionSequencer!! */
	void releaseHandler() {

		// first increment the fire count. We only release if the
		// previous count was zero, otherwise the handler is already
		// running
		if (!_threadStarted) {
			_handler = new NoHeapRealtimeThread(_scheduling, _release, _memory,
					_initMemory, _group, handlerLogic);
			_threadStarted = true;
			_handler.start();
		}
		if (getAndIncrementPendingFireCount() == 0) {
			_noWork = false;
		}
	}

	// for javax.safetycritical use
	protected MemoryArea getInitMemoryArea() {
		return _initMemory;
	}

	/**
	 * Allow SCJ mission manager to control the life time of AEH. NOTE: after
	 * this method is invoked, the AEH is completely dead and cannot be reused.
	 */
	protected void requestTermination() {
		_handler.interrupt();
	}

	protected int getAndIncrementPendingFireCount() {
		return _fireCount++;
	}

	protected int getAndDecrementPendingFireCount() {
		int temp = _fireCount;
		if (_fireCount > 0)
			_fireCount--;
		return temp;
	}
}
