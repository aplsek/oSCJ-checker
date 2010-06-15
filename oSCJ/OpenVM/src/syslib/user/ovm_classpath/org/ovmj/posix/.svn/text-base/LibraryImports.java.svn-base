package org.ovmj.posix;

class LibraryImports {
    static native int open(String name,int flags,int mode);
    static native int close(int fd);
    static native int readOneByte(int fd,boolean block);
    static native int read(int fd,byte[] array,int offset,int count,boolean block);
    static native int writeOneByte(int fd,int b,boolean block);
    static native int write(int fd,byte[] array,int offset,int count,boolean block);
    static native int getErrno();
    static native boolean errnoIsWouldBlock();


    static native int OPEN2(int flags,int mode);
    static native int CLOSE(int fd);
    static native int WRITE(int fd,int b, boolean block);
 

}

