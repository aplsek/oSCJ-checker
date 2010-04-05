package java.io;

import org.ovmj.java.NativeConstants;
import org.ovmj.java.Opaque;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;


/**
 * Defines all native methods for FileDescriptor and File.
 * <p>
 * open, close, read, write, and anything having to do with String ->
 * C string converion goes through executive domain for a few reasons:
 * <ul>
 * <li> domain resource accounting -- we need to close down fds on
 *      exit
 * <li> blocking IO -- only executive domain code knows how to
 *      reschedule when a syscall would block
 * <li> consistency -- existsInternal converts a Java string to a unix
 *      file name the same way open does
 * </li>
 * Anything that doesn't need to be in the kernel for one of those
 * reasons is done right here.  You may argue that we've already lost
 * consistency, and you may be right.
 *
 * checkNative and checkLong are a bit strange.  We need to capture
 * the value of errno before some other thread preempts us.
 * Basically, we need to call getErrno before any ordinary method
 * calls, CSA calls, or backward branches in J2C.  In the interpreter,
 * it is slightly worse: even forward branches are safe points.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 * @author Christian Grothoff
 */
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



  
  /* ********************* native methods from File ****************** */

    /*
   * This native method does the actual check of whether or not a file
   * is a plain file or not.  It also handles the existence check to
   * eliminate the overhead of a call to exists()
   */
    static boolean VMFile_isFile(String path) {
	return LibraryImports.is_plainfile(path);
    }

 /*
   * This method does the actual check of whether or not a file is a
   * directory or not.  It also handle the existence check to eliminate
   * the overhead of a call to exists()
   */
  static boolean VMFile_isDirectory(String path) {
      return LibraryImports.is_directory(path);
  }

  /**
   * This native method checks file permissions for reading
   */
  static boolean VMFile_canRead(String path) {
      return LibraryImports.access(path, R_OK);
  }

  /**
   * This native method checks file permissions for writing
   */
  static boolean VMFile_canWrite(String path) {
      return LibraryImports.access(path, W_OK);
  }

  /*
   * This native method actually determines the length of the file and
   * returns 0 if the specified file does not exist.
   */
    static long VMFile_length(String path) {
	long ret = LibraryImports.length(path);
	if (ret < 0)
	    return 0;
	return ret;
    }

  /*
   * This native function actually produces the list of file in this
   * directory
   */
    static String[] VMFile_list(String dirname) {
	return LibraryImports.list_directory(dirname);
    }

  /**
   * This method is used to create a temporary file
   */
    static boolean VMFile_create(String name) throws IOException {
	int fd = LibraryImports.open(name, O_CREAT|O_EXCL|O_RDWR, 0600);
	if (fd < 0)
	    return false;
	LibraryImports.close(fd);
	return true;
  }

  /**
   * This native method sets the permissions to make the file read only.
   */
    static boolean VMFile_setReadOnly(String path) {
	int mode = LibraryImports.getmod(path);
	if (mode == -1)
	    return false; // error
	mode = mode & (~ (S_IWUSR | S_IWGRP | S_IWOTH));
	return (0 == LibraryImports.chmod(path, mode));
  }

  /*
   * This native method actually creates the directory
   */
    static boolean VMFile_mkdir(String path) {
	return (0 == LibraryImports.mkdir(path, S_IRWXU|S_IRWXG|S_IRWXO));
    }
    
  /*
   * This native method actually performs the rename.
   */
    static boolean VMFile_renameTo(String target, String dest) {
	return (0 == LibraryImports.renameTo(target, dest));
  }

  /*
   * This native method handles the actual deleting of the file
   */
  static boolean VMFile_delete(String path) {
      // One could check for ENOTDIR, but then we would get the errno
      // result from rmdir rather than the one from unlink.  Only
      // trying rmdir if unlink fails on a directory is a bit more
      // tricky: on MacOS errno == EPERM, and on Linux errno == EISDIR.
      return (LibraryImports.rmdir(path) == 0
	      || LibraryImports.unlink(path) == 0);
  }

  /*
   * This method does the actual setting of the modification time.
   */
    static boolean VMFile_setLastModified(String path, long time) {
	return (0 == LibraryImports.setLastModified(path, time));
    }
 
    /*
     * This native method does the actual work of getting the last file
     * modification time.  It also does the existence check to avoid the
     * overhead of a call to exists()
     */
    static long VMFile_lastModified(String path) {
	long ret = LibraryImports.getLastModified(path);
	if (ret < 0)
	    return 0; // classpath doc says to return 0 if the file does not exist!
	return ret;
    }
    
    static boolean VMFile_exists(String name) {
	return LibraryImports.access(name, F_OK);
    }

    /* ******************* ObjectInputStream ***************** */

    // Creates an instance of Class c but does not invoke a constructor on it.
    static Object allocateObject(ObjectInputStream _,
				 Class c) 
	throws InstantiationException {
	Object ret = LibraryImports.allocateObject(c);
	if (ret == null)
	    throw new InstantiationException("Could not instantiate " + c);
	return ret;
    }

    // Takes a raw object created by allocateObject and invokes the no-arg
    // constructor defined by Class c, upon it.
    static void callConstructor(ObjectInputStream _,
				Class c,
				Object o) {
	Error error
	    = LibraryImports.callConstructor(c, o); 
	if (error != null)
	    throw error;
    }


    // this method is meant as a native "tunnel" to invoke the protected
    // currentClassLoader method of the given security manager. We should be
    // able to do this via reflection, with access checks disabled,
    // with the help of doPrivileged.
    static ClassLoader currentClassLoader(final SecurityManager sm) {
        return (ClassLoader) AccessController.doPrivileged( new PrivilegedAction() {
                public Object run() {
                    try {
                        Method m = sm.getClass().getDeclaredMethod("currentClassLoader", new Class[0]);
                        m.setAccessible(true);
                        return m.invoke(sm, null);
                    }
                    catch (Throwable t) {
                        throw (Error) new Error("Unexpected reflective exception").initCause(t);
                    }
                }
            });
    }

}
