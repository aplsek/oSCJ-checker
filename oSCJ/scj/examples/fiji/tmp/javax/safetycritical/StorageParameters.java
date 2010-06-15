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

/**
 * 
 * Each schedulable object has several associated types of storage. As well as
 * its Java run-time execution stack, there is also a native method stack (if
 * this memory is dis- tinct from the run-time stack). In addition, each
 * schedulable object has: a backing store that is used for any scoped memory
 * areas it may create and a number of bytes dedicated to the message associated
 * with this Schedulable objects ThrowBoundary- Error exception plus all the
 * method names/identifiers in the stack backtrace.
 * 
 * This class allows the programmer to set the sizes of these memory stores only
 * at construction time (the objects are immutable). It is assumed that these
 * sizes are obtained from vendor-specific tools.
 * 
 * @author plsek
 * 
 */
@SCJAllowed
public class StorageParameters {

    // TODO: what should the defaults be? Make them configurable?
    static int DEFAULT_MESSAGE_LENGTH = 0;
    static int DEFAULT_STACKTRACE_LENGTH = 0;

    long _totalBackingStore;
    int _nativeStackSize;
    int _javaStackSize;
    int _messageLength;
    int _stackTraceLength;

    /**
     * Stack sizes for schedulable objects and sequencers. Passed as parameter
     * to the constructor of mission sequencers and schedulable objects.
     * 
     * @param totalBackingStore
     *            size of the backing store reservation for worst-case scope
     *            usage in bytes
     * @param nativeStackSize
     *            size of native stack in bytes (vendor specific)
     * @param javaStackSize
     *            size of Java execution stack in bytes (vendor specific)
     */
    @SCJAllowed
    public StorageParameters(long totalBackingStore, int nativeStackSize,
            int javaStackSize) {
        this(totalBackingStore, nativeStackSize, javaStackSize,
                DEFAULT_MESSAGE_LENGTH, DEFAULT_STACKTRACE_LENGTH);
    }

    /**
     * Stack sizes for schedulable objects and sequencers. Passed as parameter
     * to the constructor of mission sequencers and schedulable objects.
     * 
     * @param totalBackingStore
     *            size of the backing store reservation for worst-case scope
     *            usage in bytes
     * 
     * @param nativeStackSize
     *            size of native stack in bytes (vendor specific)
     * 
     * @param javaStackSize
     *            size of Java execution stack in bytes (vendor specific)
     * 
     * @param messageLength
     *            length of the space in bytes dedicated to message associated
     *            with this Schedulable object's ThrowBoundaryError exception
     *            plus all the method names/identifiers in the stack backtrace
     * 
     * @param stackTraceLength
     *            the number of byte for the StackTraceElement array dedicated
     *            to stack backtrace associated with this Schedulable object's
     *            ThrowBoundaryError exception.
     */
    @SCJAllowed
    public StorageParameters(long totalBackingStore, int nativeStackSize,
            int javaStackSize, int messageLength, int stackTraceLength) {
        _totalBackingStore = checkLegality(totalBackingStore);
        _nativeStackSize = checkLegality(nativeStackSize);
        _javaStackSize = checkLegality(javaStackSize);
        _messageLength = checkLegality(messageLength);
        _stackTraceLength = checkLegality(stackTraceLength);
    }

    /**
     * 
     * @return the size of the total backing store available for scoped memory
     *         areas created by the assocated SO.
     */
    @SCJAllowed
    public long getTotalBackingStoreSize() {
        return _totalBackingStore;
    }

    // TODO: return value: long <--> int?
    /**
     * 
     * @return the size of the native method stack available to the assocated
     *         SO.
     */
    @SCJAllowed
    public long getNativeStackSize() {
        return _nativeStackSize;
    }

    // TODO: return value: long <--> int?
    /**
     * 
     * @return the size of the Java stack available to the assocated SO.
     */
    @SCJAllowed
    public long getJavaStackSize() {
        return _javaStackSize;
    }

    /**
     * 
     * return the length of the message buffer
     */
    @SCJAllowed
    public int getMessageLength() {
        return _messageLength;
    }

    /**
     * 
     * return the length of the stack trace buffer
     */
    @SCJAllowed
    public int getStackTraceLength() {
        return _stackTraceLength;
    }

    private long checkLegality(long par) {
        if (par < 0) {
            // TODO: throw what exception? Can we new?
            return 0;
        } else {
            return par;
        }
    }

    private int checkLegality(int par) {
        if (par < 0) {
            // TODO: throw what exception? Can we new?
            return 0;
        } else {
            return par;
        }
    }
}
