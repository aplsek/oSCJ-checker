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



import edu.purdue.scj.VMSupport;

/**
 * All of the exceptions thrown directly by the virtual machine (such as
 * ArithmeticException, OutOfMemoryError, StackOverflowError) are preallocated
 * in immortal memory.
 * 
 * TODO: are they shared or per-thread?
 * 
 * @author leizhao, Ales Plsek
 * 
 */
@SCJAllowed
public final class ImmortalMemory extends MemoryArea {

    private static ImmortalMemory _instance = new ImmortalMemory();

    static ArithmeticException _ArithmeticException;
    static OutOfMemoryError _OutOfMemoryError;
    static StackOverflowError _StackOverflowError;
    // TODO: more?

    static {
        // FIXME: make sure they are allocated in Immortal
        _ArithmeticException = new ArithmeticException();
        _OutOfMemoryError = new OutOfMemoryError();
        _StackOverflowError = new StackOverflowError();
    }

    private ImmortalMemory() {
        super(VMSupport.getImmortalArea());
    }

    @SCJAllowed
    @SCJRestricted()
    public static ImmortalMemory instance() {
        return _instance;
    }
    
    @SCJAllowed
    public void enter(Runnable logic) {
    	return;
    }

    @SCJAllowed
    public long memoryConsumed() {
    	return VMSupport.memoryConsumed(_instance.get_scopeID());
    }
    
    @SCJAllowed
    public long memoryRemaining() {
    	return VMSupport.memoryRemaining(_instance.get_scopeID());
    }
    
    @SCJAllowed
    public long size() {
    	return VMSupport.getScopeSize(_instance.get_scopeID());
    }
}
