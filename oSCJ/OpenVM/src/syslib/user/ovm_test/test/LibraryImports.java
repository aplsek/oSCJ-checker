package test;

/**
 * The interface to the OVM kernel services from the Java VM personality.
 *
 */
final class LibraryImports {

    private LibraryImports() {} // no instantiation

    // testing aids for synchronization
    static native boolean currentThreadOwnsMonitor(Object mon);
    static native int getEntryCountForMonitor(Object mon);
    static native boolean isUnownedMonitor(Object mon);

    static native void print(String msg);

    static native void halt(int reason);


    // experimental -- add jv
    public static native int tlock_copyIntoBuffer(Object object, int to) ;
    public static native int tlock_startBuffer(Object arr) ;
    public static native void tlock_copyFromBuffer(int from, Object object);   
    
    // To determine whether PARs are even enabled in this configuration.
    public static native boolean PARenabled();
    

}

