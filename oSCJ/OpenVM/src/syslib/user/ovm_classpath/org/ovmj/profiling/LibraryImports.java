package org.ovmj.profiling;

class LibraryImports {
    static native void resetEvManProfileHistograms();
    static native void disableEvManProfileHistograms();
    
    static native void getrusageSelf(long[] array);
}

