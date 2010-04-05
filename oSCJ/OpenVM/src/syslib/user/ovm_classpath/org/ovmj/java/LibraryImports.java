/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_classpath/org/ovmj/java/LibraryImports.java,v 1.1 2005/04/14 04:21:29 dholmes Exp $
 */
package org.ovmj.java;

/**
 * The interface to the OVM kernel services from the Java VM personality.
 *
 */
final class LibraryImports {

    private LibraryImports() {} // no instantiation

    static final native boolean isReschedulingEnabled();

    // this is called by the Launcher when not running the JVM
    static final native void enableClassInitialization();

    // temporary means for the Launcher to see if we have a JVM configuration
    static final native boolean isJVMConfig();

    // Low-level I/O for use during initialization 

    static final native void printString(String msg);

    static final native void panic(String msg);

    static final native void halt(int reason);

    static final native String[] getCommandlineArgumentStringArray();
}




