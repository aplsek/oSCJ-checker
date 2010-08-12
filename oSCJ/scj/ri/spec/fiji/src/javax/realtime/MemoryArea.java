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

import java.lang.reflect.Array;

import javax.realtime.AllocationContext;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

import edu.purdue.scj.BackingStoreID;
import edu.purdue.scj.VMSupport;
//import edu.purdue.scj.utils.Utils;

@SCJAllowed
public abstract class MemoryArea implements AllocationContext {
	/**
	 * space for temporary region when making a temporary scope stack. Normally
	 * it just needs to hold the actual ScopeStack object, but assertions and
	 * debug code can change that.
	 */
	static final long _TEMPSIZE = ScopeStack._BASE_SIZE;
	private BackingStoreID _scopeID;
	long _size;

	/** For ScopedMemory use only. The backing store will be allocated as enter. */
	protected MemoryArea(long size) {
		if (size < 0)
			throw new IllegalArgumentException("size must be non-negative");
		_size = size;
		
		//System.out.println("Scope init, size: " + _size);
	}

	/**
	 * For ImmortalMemory use only. The immortal backing store is always there,
	 * so we know how to set up _scopeID.
	 */
	protected MemoryArea(BackingStoreID scopeID) {
		_scopeID = scopeID;
		_size = VMSupport.getScopeSize(_scopeID);
		VMSupport.setNote(_scopeID, this);
		
		//System.out.println("[MEmoryArea conscturtor]Scope initialized: size:" + _size +"  \n\t--- MemoryArea - scopeID");
		//System.out.println("\n[SCJ Debug] mission:" +  VMSupport.memoryConsumed(RealtimeThread.getCurrentMemoryArea().get_scopeID()));
        //System.out.println("\n[SCJ Debug] mission:" +  this.memoryRemaining());
	}

	@SCJAllowed
	public static MemoryArea getMemoryArea(Object object) {
		return getMemoryAreaObject(VMSupport.areaOf(object));
	}
	
	
	

	@SCJAllowed
	public void enter(Runnable logic) {
	    ////Utils.debugIndentIncrement("###[SCJ] MemoryArea.enter");
	    
	    if (logic == null)
			throw new IllegalArgumentException("null logic not permitted");
		RealtimeThread thread = RealtimeThread.currentRealtimeThread();
		enterImpl(thread, logic);
		
		////Utils.decreaseIndent();
	}

	// 
	// @SCJAllowed(Level.LEVEL_1)
	// public void executeInArea(Runnable logic) throws
	// InaccessibleAreaException {
	// if (logic == null)
	// throw new IllegalArgumentException("null logic not permitted");
	// RealtimeThread thread = RealtimeThread.currentRealtimeThread();
	// execInAreaImpl(thread, logic);
	// }

	@SCJAllowed(Level.LEVEL_1)
	public void executeInArea(Runnable logic) throws InaccessibleAreaException {
		if (logic == null)
			throw new IllegalArgumentException("null logic not permitted");
		RealtimeThread thread = RealtimeThread.currentRealtimeThread();
		execInAreaImpl(thread, logic);
	}

	@SCJAllowed
	public Object newInstance(Class clazz) throws InstantiationException,
			IllegalAccessException {
		RealtimeThread thread = RealtimeThread.currentRealtimeThread();
		return newInstanceImpl(thread, clazz);
	}

	@SCJAllowed
	public Object newArray(Class clazz, int number)
			throws NegativeArraySizeException, IllegalAccessException {
		RealtimeThread thread = RealtimeThread.currentRealtimeThread();
		return newArrayImpl(thread, clazz, number);
	}

	@SCJAllowed
	public static Object newArrayInArea(Object object, Class clazz, int size)
			throws IllegalAccessException {
		return getMemoryArea(object).newArray(clazz, size);
	}

	@SCJAllowed
	public static Object newInstanceInArea(Object object, Class clazz)
			throws InstantiationException, IllegalAccessException {
		return getMemoryArea(object).newInstance(clazz);
	}

	@SCJAllowed
	public long memoryConsumed() {
		return VMSupport.memoryConsumed(get_scopeID());
	}

	@SCJAllowed
	public long memoryRemaining() {
		return VMSupport.memoryRemaining(get_scopeID());
	}

	@SCJAllowed
	public long size() {
		return _size;
	}

	/**
	 * Assumption: only one thread is allowed to enter and be active in a
	 * ScopedMemory at a time. This is particularly true according to SCJ
	 * requirements, and guaranteed by the runtime check in PrivateMemory and
	 * MissionMemory.
	 * 
	 * The right size of backing store is allocated during enter, and
	 * deallocated on exit.
	 */
	final void enterImpl(RealtimeThread thread, Runnable logic) {
	    ////Utils.debugIndentIncrement("###[SCJ] MemoryArea.enterIml");
	    
	    preScopeEnter(thread);
		allocBackingStore();
		thread.getScopeStack().push(this);
		
		// TODO: what to do with exception?
		try {
		    ////Utils.debugPrintln("###[SCJ] VMsupport.enter");
		    
			VMSupport.enter(get_scopeID(), logic);
		} finally {
			thread.getScopeStack().pop();
			freeBackingStore();
			postScopeEnter();
		}
		
		////Utils.decreaseIndent();
	}

	/**
	 * Unwinds the scope stack so that <tt>this</tt> is the current area,
	 * executes the logic and then restores the original scope stack and current
	 * allocation context.
	 */
	final void execInAreaImpl(RealtimeThread thread, Runnable logic) {
		ScopeStack stack = thread.getScopeStack();
		int oldActivePointer = stack.getDepth(true);
		int indexOfThis = stack.getIndex(this, true);

		stack.setActivePointer(indexOfThis);
		BackingStoreID oldScope = VMSupport.setCurrentArea(get_scopeID());

		try {
			logic.run();
		} finally {
			VMSupport.setCurrentArea(oldScope);
			stack.setActivePointer(oldActivePointer);
		}
	}

	final Object newArrayImpl(RealtimeThread thread, Class clazz, int number)
			throws IllegalAccessException, NegativeArraySizeException {
		ScopeStack stack = thread.getScopeStack();
		int oldActivePointer = stack.getDepth(true);
		int indexOfThis = stack.getIndex(this, true);

		stack.setActivePointer(indexOfThis);
		BackingStoreID oldScope = VMSupport.setCurrentArea(get_scopeID());

		try {
			return Array.newInstance(clazz, number);
		} finally {
			VMSupport.setCurrentArea(oldScope);
			stack.setActivePointer(oldActivePointer);
		}
	}

	final Object newInstanceImpl(RealtimeThread thread, Class clazz)
			throws InstantiationException, IllegalAccessException {
		ScopeStack stack = thread.getScopeStack();
		int oldActivePointer = stack.getDepth(true);
		int indexOfThis = stack.getIndex(this, true);

		stack.setActivePointer(indexOfThis);
		BackingStoreID oldScope = VMSupport.setCurrentArea(get_scopeID());

		try {
			return clazz.newInstance();
		} finally {
			VMSupport.setCurrentArea(oldScope);
			stack.setActivePointer(oldActivePointer);
		}
	}

	static MemoryArea getMemoryAreaObject(BackingStoreID scopeID) {
	    if (scopeID == ImmortalMemory.instance().get_scopeID()) 
	        return ImmortalMemory.instance();
		else 
			return (MemoryArea) VMSupport.getNote(scopeID);
	}

	@com.fiji.fivm.r1.NoScopeChecks
	BackingStoreID get_scopeID() {
		return _scopeID;
	}

	@com.fiji.fivm.r1.NoScopeChecks
	void set_scopeID(BackingStoreID scopeID) {
		_scopeID = scopeID;
	}

	protected void preScopeEnter(RealtimeThread thread) {
	}

	protected void postScopeEnter() {
	}

	/**
	 * Set the memory to a new size. NOTE: resizing is allowed ONLY when NO
	 * thread is in the memory area.
	 */
	protected void setSize(long size) {
		if (size < 0)
			throw new IllegalArgumentException(
					"Mission memory size must be non-negative");
		_size = size;
	}

	/**
	 * Allocate (and push) _sizeInBytes of backing store. Associate the ID with
	 * "this". Don't do anything if this is of ImmortalMemory class.
	 */
	private void allocBackingStore() {
	    ////Utils.debugIndentIncrement("###[SCJ] MemoryArea.allocBackingStore, size : " + _size);
	    if (get_scopeID() == null) {
			set_scopeID(VMSupport.pushScope(_size));
			VMSupport.setNote(get_scopeID(), this);
		}
	    ////Utils.decreaseIndent();
	}

	/** pop the backing store unless this is immortal */
	private void freeBackingStore() {
		if (get_scopeID() != ImmortalMemory.instance().get_scopeID()) {
			VMSupport.popScope();
			set_scopeID(null);
		}
	}

	// TODO: not consider the exception handling now
	// private static void rethrowUnchecked(Throwable t) {
	// if (t instanceof Error) {
	// throw (Error) t;
	// } else if (t instanceof RuntimeException) {
	// throw (RuntimeException) t;
	// } else {
	// throw new Error("Unexpected checked exception", t);
	// }
	// }
	//
	// /**
	// * Check if <tt>e</tt> can be stored in the given outer memory area, if so
	// * then rethrow it, else wrap it in a <tt>ThrowBoundaryError</tt>
	// allocated
	// * in the outer memory area and throw that.
	// */
	// private static void reThrowTBE(BackingStoreID outerScope, Throwable t) {
	// MemoryArea eArea = getMemoryArea(t);
	// if (eArea instanceof ScopedMemory) {
	//    
	// // TODO: Here would be a lot of work to do for SCJ exception
	// // mechanism
	// }
	// rethrowUnchecked(t);
	// }
}
