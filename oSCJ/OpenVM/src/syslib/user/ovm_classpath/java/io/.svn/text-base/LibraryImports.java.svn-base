package java.io;

import org.ovmj.java.Opaque;

class LibraryImports {
    static native int getErrno();
    static native long length(String name);
    static native int mkdir(String name, int mode);
    static native boolean access(String name, int mode);
    static native boolean is_directory(String name);
    static native boolean is_plainfile(String name);
    static native int unlink(String path);
    static native int rmdir(String path);
    static native int getmod(String path);
    static native int chmod(String path, int mode);
    static native String[] list_directory(String path);

    static native int renameTo(String oldname,
			       String target);
    static native long getLastModified(String path);
    static native int setLastModified(String path, long time);
    static native Error callConstructor(Class c, Object o);
    static native Object allocateObject(Class c);
   
    static native int open(String path, int flags, int mode);
    static native int close(int fd);
    static native void printString(String msg);
    static native Opaque hackFieldForName(Object object,String name);

    // VMObjectStreamClass: unchecked reflective setters (Field.setXXX should
    // check against final fields, these ones do not!).
    static final native void fieldSetInt(Opaque vmField, Object o, int value);
    static final native void fieldSetBoolean(Opaque vmField, Object o, boolean value);
    static final native void fieldSetShort(Opaque vmField, Object o, short value);
    static final native void fieldSetChar(Opaque vmField, Object o, char value);
    static final native void fieldSetByte(Opaque vmField, Object o, byte value);
    static final native void fieldSetFloat(Opaque vmField, Object o, float value);
    static final native void fieldSetDouble(Opaque vmField, Object o, double value);
    static final native void fieldSetLong(Opaque vmField, Object o, long value);
    static final native void fieldSetReference(Opaque vmField, Object o, Object value);


}
