package gnu.java.nio;

import org.ovmj.java.Opaque;

final class LibraryImports {

    static native int getErrno();

    static native int close(int fd);
 
    static native int pipe(int[] p);

    static native int read(int fd,
			   byte[] dst,
			   int off,
			   int len,
			   boolean block);

    static native int write(int fd,
			    byte[] dst,
			    int off,
			    int len,
			    boolean block);

    static native Opaque createSelectCookie();

    static native void releaseSelectCookie(Opaque selectCookie);

    static native int select(Opaque selectCookie,
			     long timeout,
			     ResultSet rs);

    static native void registerSelector(Opaque selectCookie,
					int fd,
					int ops,
					Object cpCookie);

    static native void unregisterSelector(Opaque selectCookie,
					  int fd,
					  Object cpCookie);

    static class ResultSet {
	/**
	 * How many FDs are ready?
	 */
	int readyCount;

	Object[] readyCpCookieSet;
	
	// ready to read/write/error?
	// Set bit 1:2:4 respectively!
	int[] readyType;

    }

}