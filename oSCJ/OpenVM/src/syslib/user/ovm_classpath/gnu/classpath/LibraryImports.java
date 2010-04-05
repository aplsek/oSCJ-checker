package gnu.classpath;

class LibraryImports {
    // VM-specific properties
    static final native String VMPropertyName(int i);
    static final native String VMPropertyValue(int i);
    static final native String getClassPath();

    // stack walking
    static final native int getClassContextDepth();
    static final native void  getClassContext(Class[] classes, int skipFrames);
    static final native boolean classContextSkipsCaller();

    static final native void printCharAsByte(char c);
    
    static final native void printString(String s);
}
