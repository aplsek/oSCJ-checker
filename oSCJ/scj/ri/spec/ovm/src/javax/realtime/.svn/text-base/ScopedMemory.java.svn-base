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

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;


@SCJAllowed
public abstract class ScopedMemory extends MemoryArea implements ScopedAllocationContext {

    /**
     * The primordial scope acts as the parent for any scope for which no scoped
     * memory appears above it in the current scope stack.
     */
    static final ScopedMemory _primordialScope = new ScopedMemory(0) {
        public String toString() {
            return "PrimordialScope";
        }

		@Override
		public void resize(long size) throws IllegalStateException {
			// TODO: resizing Scoped Memory, should we implement this?
		}
    };

   
    
    ScopedMemory _parent;
    private Object _portal;

    @SCJAllowed(INFRASTRUCTURE)
    public ScopedMemory(long size) {
        super(size);
    }

    @SCJAllowed(INFRASTRUCTURE)
    public ScopedMemory(SizeEstimator estimator) {
        super(estimator.getEstimate());
    }

    /*
     * we have to override most of the public methods of MemoryArea to declare
     * the right exceptions
     */
    @Override
    public void enter(Runnable logic) {
        if (logic == null)
            throw new IllegalArgumentException("null logic not permitted");
        RealtimeThread current = RealtimeThread.currentRealtimeThread();
        enterImpl(current, logic);
    }

    @Override
    public void executeInArea(Runnable logic) {
        if (logic == null)
            throw new IllegalArgumentException("null logic not permitted");
        RealtimeThread current = RealtimeThread.currentRealtimeThread();
        checkAccessible(current);
        execInAreaImpl(current, logic);
    }

    @Override
    public Object newArray(Class type, int number)
            throws NegativeArraySizeException, IllegalAccessException {
        RealtimeThread current = RealtimeThread.currentRealtimeThread();
        checkAccessible(current);
        return newArrayImpl(current, type, number);
    }

    @Override
    public Object newInstance(Class klass) throws InstantiationException,
            IllegalAccessException {
        RealtimeThread current = RealtimeThread.currentRealtimeThread();
        checkAccessible(current);
        return newInstanceImpl(current, klass);
    }

    public void join() throws InterruptedException {
    }

    @SCJAllowed
    public void setPortal(Object o) {
        doSetPortal(o);
    }

    @SCJAllowed
    public Object getPortal() {
        
        return doGetPortal();
    }

    /** Actual setting of the portal */
    void doSetPortal(Object o) throws org.ovmj.util.PragmaNoBarriers {
        _portal = o;
    }

    Object doGetPortal() throws org.ovmj.util.PragmaNoBarriers {
        return _portal;
    }

    private void checkAccessible(RealtimeThread thread) {
        if (thread.getScopeStack().getIndex(this, true) < 0)
            throw new InaccessibleAreaException();
    }

    @Override
    protected void preScopeEnter(RealtimeThread t) {
            _parent = RealtimeThread.currentRealtimeThread().getScopeStack()
                    .getTopScopedMemory(true);
    }

    @Override
    protected void postScopeEnter() {
            _parent = null;
    }
}
