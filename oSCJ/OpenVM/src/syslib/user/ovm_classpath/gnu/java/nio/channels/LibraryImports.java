package gnu.java.nio.channels;

// import gnu.classpath.RawData;

class LibraryImports {
    static native int getErrno();
    static native int open(String name, int flags, int mode);
    static native int readOneByte(int fd,
				  boolean blocking);
    static native int read(int fd,
			   byte[] buf,
			   int byteOffset, 
			   int byteCount, 
			   boolean blocking);
    static native long length(int fd);
    static native int fsync(int fd);
    static native int ftruncate(int fd, long size);
    static native int writeOneByte(int fd, 
				   int b, 
				   boolean blocking);
    static native int write(int fd,
			    byte[] buf, 
			    int byteOffset, 
			    int byteCount, 
			    boolean blocking);
    static native int close(int fd);
    static native void printString(String msg);
    static native boolean isValid(int fd);
    static native long lseek(int fd,long offset,int whence);

//     static native RawData memmap(int fd,
// 				 int prot,
// 				 int flags,
// 				 long position, 
// 				 int size);
  

    // return 0 on success, 1 on failure to lock -1 on error
    static native int lock(int fd,
			   long start,
			   long size,
			   boolean shared,
			   boolean wait);
    
    // return 0 on success, -1 on error
    static native int unlock(int fd,
			     long start,
			     long size);

}
