package gnu.java.nio.channels;

import org.ovmj.java.NativeConstants;
import org.ovmj.java.Opaque;
import java.nio.MappedByteBuffer;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.SyncFailedException;
import java.io.FileDescriptor;
//import gnu.classpath.RawData;
import java.nio.MappedByteBufferImpl;
import java.nio.MappedByteBuffer;

class LibraryGlue implements NativeConstants {

      // native helpers
    static native int get_specific_error_string(int errno,
						byte[] buf,
						int len);


    /**
     * Poll value of errno and build the appropriate error message.
     */
    private static String getErrorMessage(int reason) {	
	byte[] buf = new byte[128];
	int len = get_specific_error_string(reason, buf, buf.length);
	return new String(buf, 0, len);
    }

    private static IOException die(int reason) {
	return new IOException(getErrorMessage(reason));
    }

    private static int check(int ret) throws IOException {
	if (ret < 0) 
	    throw die(LibraryImports.getErrno());
	return ret;
    }

    private static long check(long ret) throws IOException {
	if (ret < 0) 
	    throw die(LibraryImports.getErrno());
	return ret;
    }

    /* ***************** native methods from FileDescriptor ****************** */


   /**
   * This method is called in the class initializer to do any require
   * native library initialization. 
   */
    static void FileChannelImpl_init() {
    }

  /**
   * Opens the specified file in the specified mode.  This can be done
   * in one of the specified modes:
   * <ul>
   * <li>r - Read Only
   * <li>rw - Read / Write
   * <li>ra - Read / Write - append to end of file
   * <li>rws - Read / Write - synchronous writes of data/metadata
   * <li>rwd - Read / Write - synchronous writes of data.
   *
   * @param path Name of the file to open
   * @param mode Mode to open
   *
   * @return The resulting file descriptor for the opened file, or -1
   * on failure (exception also signaled).
   *
   * @exception IOException If an error occurs.
   */
    static int open(FileChannelImpl _,
		    String path, int mode)
	throws FileNotFoundException {

        //        System.out.println("Trying to open: " + path + " with mode " + mode);
	int flags = -1;
        if ( ((mode&7) & FileChannelImpl.READ) > 0)
	    flags = O_RDONLY;
        if ( ((mode&7) & FileChannelImpl.WRITE) > 0) {
	    if ((mode & (FileChannelImpl.READ|FileChannelImpl.APPEND)) == 0)
		flags = O_WRONLY | O_CREAT | O_TRUNC;
	    else
		flags = O_RDWR | O_CREAT;
	}
        if ( ((mode&7) & FileChannelImpl.APPEND) > 0)
	    flags = O_RDWR | O_CREAT | O_APPEND;

        if (flags == -1) throw new Error("Invalid mode " + mode);

	if ((mode & FileChannelImpl.EXCL) > 0)
	    flags |= O_EXCL;
	if ((mode & FileChannelImpl.SYNC) > 0)
	    flags |= O_SYNC;
	if ((mode & FileChannelImpl.DSYNC) > 0)
	    flags |= 0; // FIXME: What is DSYNC supposed to be?

        //        System.out.println("Final flags = " + flags + "\n mode & 7 = " + (mode & 7));
        
		
	int result = LibraryImports.open(path, flags, S_IRWXU|S_IRWXG|S_IRWXO); /* 777, umask does the rest!*/
	if (result == -1)
	    throw new FileNotFoundException(path);
	return result;
    }

  /**
   * Closes this specified file descriptor
   * 
   * @param fd The native file descriptor to close
   * @exception IOException If an error occurs 
   */    
    static void implCloseChannel(FileChannelImpl _) throws IOException {
	check(LibraryImports.close(_.fd));	
    }
 
  /**
   * Writes a single byte to the file
   *
   * @param fd The native file descriptor to write to
   * @param b The byte to write, encoded in the low eight bits
   *
   * @return The return code of the native write command
   *
   * @exception IOException If an error occurs
   */
    static void write(FileChannelImpl _,
		      int b) throws IOException {
	check(LibraryImports.writeOneByte(_.fd,b,true));
    }

  /**
   * Writes a byte buffer to the file
   *
   * @param fd The native file descriptor to write to
   * @param buf The byte buffer to write from
   * @param int The offset into the buffer to start writing from
   * @param len The number of bytes to write.
   *
   * @exception IOException If an error occurs
   */
    static void write(FileChannelImpl _,
		      byte[] buf, int offset, int len)
      throws IOException {
      // always do complete write, classpath relies on such semantics!
      while (len>0) {
	  int result = check(LibraryImports.write(_.fd,buf,offset,len,true));
	  offset += result;
	  len -= result;
      }
  }

  /**
   * Reads a single byte from the file
   *
   * @param fd The native file descriptor to read from
   *
   * @return The byte read, in the low eight bits on a long, or -1
   * if end of file
   *
   * @exception IOException If an error occurs
   */
    static int read(FileChannelImpl _) throws IOException {
	int res=LibraryImports.readOneByte(_.fd,true);
	if (res==-2) {
	    die(LibraryImports.getErrno());
	}
	return res;
    }

  /**
   * Reads a buffer of  bytes from the file
   *
   * @param fd The native file descriptor to read from
   * @param buf The buffer to read bytes into
   * @param offset The offset into the buffer to start storing bytes
   * @param len The number of bytes to read.
   *
   * @return The number of bytes read, or -1 if end of file.
   *
   * @exception IOException If an error occurs
   */
    static int read(FileChannelImpl _,
		    byte[] buf, int offset, int len) 
	throws IOException {
	int read = LibraryImports.read(_.fd, buf, offset, len, true);
	if ( (read == 0) && (len > 0) )
            return -1; // end of file!
        return check(read);	
   }
   
  /**
   * Returns the number of bytes available for reading
   *
   * @param fd The native file descriptor
   *
   * @return The number of bytes available for reading
   *
   * @exception IOException If an error occurs
   */
    static int available(FileChannelImpl _) throws IOException {
	long len = size(_);
	long pos = implPosition(_);
	if (pos > len)
	    throw new IOException("Assertion violated: pos > len!?");
	int delta = (int) (len - pos);
	if (delta < 0)
	    delta = 0x7FFFFFFF; /* MAXINT */
	return delta;
    }

  /**
   * Method to do a "seek" operation on the file
   * 
   * @param fd The native file descriptor 
   * @param offset The number of bytes to seek
   *
   * @exception IOException If an error occurs
   */
    static void seek (FileChannelImpl _,
		      long offset) 
	throws IOException {
	check(LibraryImports.lseek(_.fd, 
				   offset,
				   SEEK_SET));
    }
    

  /**
   * Returns the current position of the file pointer in the file
   *
   * @param fd The native file descriptor
   *
   * @exception IOException If an error occurs
   */
    static long implPosition(FileChannelImpl _) throws IOException {
  	return check(LibraryImports.lseek(_.fd, 0, SEEK_CUR));
    }

  /**
   * Returns the length of the file in bytes
   *
   * @param fd The native file descriptor
   *
   * @return The length of the file in bytes
   *
   * @exception IOException If an error occurs
   */
    static long size(FileChannelImpl _) throws IOException {
	return check(LibraryImports.length(_.fd));
    }

  /**
   * Sets the length of the file to the specified number of bytes
   * This can result in truncation or extension.
   *
   * @param fd The native file descriptor  
   * @param len The new length of the file
   *
   * @exception IOException If an error occurs
   */
    static void implTruncate(FileChannelImpl _,
			     long len) throws IOException {
	check(LibraryImports.ftruncate(_.fd,len));
    }

  /**
   * Tests a file descriptor for validity
   *
   * @param fd The native file descriptor
   *
   * @return <code>true</code> if the fd is valid, <code>false</code> 
   * otherwise
   */
    static boolean nativeValid(FileChannelImpl _) {
	return LibraryImports.isValid(_.fd);
    }

  /**
   * Flushes any buffered contents to disk
   *
   * @param fd The native file descriptor
   *
   * @exception IOException If an error occurs
   */
    static void force(FileChannelImpl _) throws SyncFailedException {
	if (0 != LibraryImports.fsync(_.fd))
 	  throw new SyncFailedException(getErrorMessage(LibraryImports.getErrno()));
  }

				   
//     static MappedByteBuffer mapImpl(FileChannelImpl _,
// 				    char mode, 
// 				    long position, 
// 				    int size)
// 	throws IOException {
// 	int flags = 0;
// 	int prot = 0;
// 	if (mode == 'r') {
// 	    prot = NativeConstants.PROT_READ;
// 	    flags = NativeConstants.MAP_SHARED;
// 	} else if (mode == '+') {
// 	    prot =  NativeConstants.PROT_READ| NativeConstants.PROT_WRITE;
// 	    flags = NativeConstants.MAP_SHARED;
// 	} else if (mode == 'c') {
// 	    prot =  NativeConstants.PROT_READ| NativeConstants.PROT_WRITE;
// 	    flags =  NativeConstants.MAP_PRIVATE;
// 	} else
// 	    throw new IllegalArgumentException("Do not know about mode " + mode);
// 	RawData map
// 	    = LibraryImports.memmap(_.fd,
// 				    prot,
// 				    flags,
// 				    position,
// 				    size);
// 	if (map == null)
// 	    throw new IOException(getErrorMessage(LibraryImports.getErrno()));	    
// 	return new MappedByteBufferImpl(map,
// 					size,
// 					mode == 'r');
//     }

   /** Try to acquire a lock at the given position and size.
   * On success return true.
   * If wait as specified, block until we can get it.
   * Otherwise return false.
   */
    static boolean lock(FileChannelImpl _,
			long position, long size,
			boolean shared, boolean wait) throws IOException {
	int ret = LibraryImports.lock(_.fd,
				      position,
				      size,
				      shared,
				      wait);
	if (ret == -1)
	    throw new IOException(getErrorMessage(LibraryImports.getErrno()));
	return (ret == 0);
    }
 
  
    static void unlock (FileChannelImpl _,
			long pos, long len) throws IOException {
	check(LibraryImports.unlock(_.fd,
				    pos,
				    len));
    }

}
