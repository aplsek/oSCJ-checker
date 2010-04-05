package org.ovmj.posix;

class LibraryGlue {
    static native int get_specific_error_string(int errno,
						byte[] buf,
						int len);
    
    static native int write(int fd,byte[] array,int len);
    
    static native int ioctl(int d, int req, int[] arg);
}

