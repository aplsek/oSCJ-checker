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

import javax.realtime.MemoryArea;

import org.ovmj.java.Opaque;

public class LibraryImports {

    static native Opaque getVMThread(Thread t);

    static native void threadSetInterrupted(Thread t, boolean set);

    static native void startThreadDelayed(Opaque vmThread, long startTime);

    public static native int delayCurrentThreadAbsolute(long nanos);

    static native int delayCurrentThreadAbsoluteUninterruptible(long nanos);

    static native boolean setPriorityIfAllowed(Opaque vmThread, int newPrio);

    public static native int getMinRTPriority();

    public static native int getMaxRTPriority();

    public static native long getCurrentTime();

    public static native long getClockResolution();

    // signal monitoring related functions

    static native Opaque createSignalWatcher();

    static native void addSignalWatch(Opaque watcher, int sig);

    static native void removeSignalWatch(Opaque watcher, int sig);

    static native boolean canMonitorSignal(int sig);

    static native void waitForSignal(Opaque watcher, int[] counts);

    // interrupt monitoring related functions

    static native boolean waitForInterrupt(int interruptIndex);

    static native void interruptServed(int interruptIndex);

    static native boolean isMonitoredInterrupt(int interruptIndex);

    static native boolean stopMonitoringInterrupt(int interruptIndex);

    static native boolean startMonitoringInterrupt(int interruptIndex);

    // sizeof

    // We'd like to have the Opaque vmType reference but it's not
    // accessible so the ED will grab it directly
    public static native long sizeOf(Class type);

    public static native long sizeOfReferenceArray(int length);

    public static native long sizeOfPrimitiveArray(int length, Class type);

    static native long sizeOfMonitor();

    // direct monitor access for scoped memory support
    static native void monitorEnter(Object o);

    static native void monitorExit(Object o);

    static native void monitorTransfer(Object o, Opaque newOwner);

    // absolute MonitorWait method
    static native boolean monitorAbsoluteTimedWait(Object o, long deadline);

    // Memory areas

    static native Opaque getHeapArea();

    public static native Opaque getImmortalArea();

    public static native Opaque makeArea(MemoryArea mirror, int size);

    public static native Opaque makeExplicitArea(int size);

    public static native Opaque setCurrentArea(Opaque area);

    public static native Opaque getCurrentArea();

    public static native Opaque areaOf(Object ref);

    static native void setParentArea(Opaque child, Opaque parent);

    static native void resetParentArea(Opaque child);

    static native boolean hasChildArea(Opaque area);

    static native boolean hasMultipleChildren(Opaque area);

    static native int getHierarchyDepth(Opaque area);

    public static native void resetArea(Opaque area);

    public static native void destroyArea(Opaque area);

    static native boolean runFinalizers(Opaque area);

    public static native MemoryArea getAreaMirror(Opaque area);

    public static native int memoryConsumed(Opaque area);

    public static native int memoryRemaining(Opaque area);

    public static native int getAreaSize(Opaque area);

    static native boolean isProperDescendant(Opaque child, Opaque parent);

    static native boolean isScope(Opaque area);

    static native boolean reallySupportNHRTT();

    static native boolean supportScopeAreaOf();

    // misc

    static native void disableHeapChecksForTermination(Thread current);

    public static native void storeInOpaqueArray(Opaque[] arr, int index,
            Opaque val);

    static native void copyArrayElements(Object from, int fromIndex, Object to,
            int toStartIndex, int howMany);

    // for debugging

    static native void showAddress(Opaque a);

    static native int toAddress(Object a);

    static native void printString(String str);

    // priority inheritance testing
    static native int getInheritanceQueueSize(Thread thread);

    static native boolean checkInheritanceQueueHead(Thread thread, Thread t);

    static native boolean checkInheritanceQueueTail(Thread thread, Thread t);

    static native int getBasePriority(Thread thread);

    static native int getActivePriority(Thread thread);

    // RTGC
    static native void runGCThread();

    static native void setShouldPause(boolean shouldPause);

    static native boolean needsGCPauseTimer();

    static native boolean needsGCThread();

    static native int getGCTimerMutatorCounts();

    static native int getGCTimerCollectorCounts();

}
