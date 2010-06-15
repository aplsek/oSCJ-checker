package ovm.core.execution;
import ovm.core.OVMBase;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.util.OVMError;

/**
 * The <tt>Native</tt> class defines some utility native functions for
 * use with low-level I/O, and provides nested types that allow access to
 * native I/O, and other, data structures and types.
 * It also exposes a number of library/system functions that can be used
 * by services in the OVM. Some of these are exposed directly, while others
 * require wrappers (see <tt>native_helpers.c</tt>).
 * <p>Any method with an actual body contains Java code to be executed under
 * a host VM at build time.
 *
 * @see NativeInterface
 * @author Krzysztof Palacz
 * @author David Holmes
 **/

// contiguous memory buffer (VM_Address)
//	input parameter - keep - WARNING !!! read, write cannot be solved at this level, as we don't have pointers to the beginning
//	output parameter - nothing
//
// array ( byte[], ... )
//	input parameter - forward, dereference arraylets, if non-contigous, create a contiguous copy
//	output parameter - forward, dereference arraylets, if non-contiguous, create contiguous one instead, copy to original on return
//			   if replicating and not with arraylets, ensure copy 
//	i/o parameter - ditto, copy on input

public final class Native extends OVMBase implements NativeInterface {

    /** This class should never be instantiated */
    private Native() { throw new OVMError("Don't instantiate this class"); }


    /**
     * A logically immutable holder class representing native pointer values.
     * Parameters of this type, or a subtype, when appearing in the
     * parameter list of a method within a
     * {@link NativeInterface native interface class} are treated specially,
     * such that the value is cast to the correct native pointer type.
     * Once set the value of the pointer can not be changed other than to
     * clear it. Clearing a pointer indicates that the native pointer is
     * no longer valid.
     *
     * @see NativeInterface
     * @author David Holmes
     */
    public static abstract class Ptr extends OVMBase {

        /** Flag to check whether it has been set yet */
        private boolean set = false;

        /** the value of the pointer */
        protected int value;

        /** Construct a pointer with the given value*/
        public Ptr(int value) {
            this.value = value;
            set = true;
        }
        /** Construct an uninitialized pointer */
        public Ptr() {}

        /** Return the type of this pointer
         * @return the type name of this pointer
         */
        public static String getTypeName() {
            return "<you must redefine this method in subclasses>";
        }

        /**
         * Return the value of this pointer
         * @return the value of this pointer
         */
        public final int getValue(){ return value; }

        /**
         * Set the value of this pointer
         *
         * @param value the new value
         */
        public final void setValue(int value) {
            if (set){
                throw new OVMError("attempt to re-set a Ptr type");
            }
            this.value = value;
            set = true;
        }

        /**
         * Clears the value of this pointer. Once cleared a pointers
         * value is zero. Clearing a pointer indicates that the native
         * pointer is no longer valid.
         *
         */
        public final void clear() {
            this.value = 0;
            set = true; // can't "unclear" it
        }
    }


    /**
     * Class to represent <code>FILE*</code>
     */
    public static class FilePtr extends Ptr {
        public static String getTypeName() {
            return "FILE*";
        }

        public FilePtr(int value) {
            super(value);
        }
        public FilePtr() {
            super(0);
        }
    }


    /**
     * Utility methods to assist in the use of the native I/O methods.
     * For example, making a NUL-terminated byte array from a normal String.
     *
     */
    public static class Utils extends OVMBase {

        /**
         * Returns a NUL-terminated byte array with the same contents
         * as the passed in string.
         *
         * @param str the string to convert
         * @return a byte array with the same contents as <code>str</code>
         * but with a NUL character in the <code>length-1</code> position.
         *
         */
         
        // al ok, Java
        public static byte[] string2c_string(String str) {
            assert str != null: "NULL string passed";
            byte[] temp = null;
            
            if (MemoryManager.the().usesArraylets()) {
              temp = MemoryManager.the().allocateContinuousByteArray(str.length()+1);
            } else {
              temp = new byte[str.length()+1];
            }
            
            return string2c_string(str, temp);
        }

        /**
         * Copies the characters of the specified string into the given
         * byte array and stores a NUL character as the last element of the
         * array. The destination array must have a length one greater
         * than the length of the string
         *
         * @param str the string to convert
         * @param dest the detination array to write into
         * @return the destination array
         */
         
        // al ok, Java
        public static byte[] string2c_string(String str, byte[] dest) {
            assert str != null: "NULL string passed";
            assert dest != null && dest.length == str.length()+1: "dest array too small";
            str.getBytes(0, str.length(), dest, 0);
            dest[dest.length-1] = '\0';
            return dest;
        }

    }


    // These functions map directly to UNIX/POSIX system calls

    public static int fflush(FilePtr fp) {
        if (fp.getValue() == NativeConstants.STDOUT) {
            System.out.flush();
        } else if (fp.getValue() == NativeConstants.STDERR) {
            System.err.flush();
        } else {
            throw new OVMError("Invalid file pointer value: " + fp.getValue());
        }
        return 0;
    }

    public static native void fclose(FilePtr fp);

    /**
     * The return value is typed as VM_Address for use with
     * {@link ovm.core.domain#stringFromLocalizedCString(VM_Address)}.
     **/
    //### assert small
    public static native VM_Address getenv(byte[] name);

    //### assert small / copy 
    public static native int is_directory(byte[] path);
    
    //### assert small / copy
    public static native int is_plainfile(byte[] path);

    public static native int access(byte[] path, int mode);
    public static native int mkdir(byte[] path, int mode);
    public static native int rename(byte[] oldpath, byte[] newpath);
    public static native long get_last_modified(byte[] path);
    public static native int set_last_modified(byte[] path, long time);
    public static native int open(byte[] path, int flags, int mode);
    public static native int mkstemp(byte[] template);
    public static native int close(int fd);
    public static native int read(int fd, VM_Address buf, int nbytes);
    public static native int pread(int fd, VM_Address buf, int nbytes, long offset);
    public static native int write(int fd, VM_Address buf, int nbytes);
    public static native int pwrite(int fd, VM_Address buf, int nbytes, long offset);
    public static native int fsync(int fd);
    public static native int ftruncate(int fd,long size);
    public static native int dup(int fd);
    public static native int pipe(int[] fds);
    public static native int socket(int domain, int type, int protocol);
    public static native int listen(int s, int backlog);
    public static native long lseek(int fd, long offset, int whence);
    public static native int mysendto(int s, byte[] buf, int len, int flags, int ip, int port);
    public static native int mylock(int s, long start, long len, int shared);
    public static native int myunlock(int s, long start, long len);

    public static native int mysendto(int s, VM_Address buf, int len, int flags, int ip, int port);
    public static native int myrecvfrom(int s, byte[] buf, int len, int flags, int[] addr);
    public static native int myrecvfrom(int s, VM_Address buf, int len, int flags, int[] addr);
    public static native int socketpair(int d, int type, int protocol,
					int[] sv);    
    public static native VM_Address mmap(VM_Address addr,
					 int len,
					 int prot,
					 int flags,
					 int fd,
					 int offset);
    public static native int munmap(VM_Address addr, int len);
    public static native int mprotect(VM_Address addr, int len, int prot);
    public static native int fork();
    public static native void _exit(int code);
    
    public static final native int myInetBind(int sock,
                                              int ipAddress,
                                              int port);
    public static final native int myInetConnect(int sock,
                                                 int ipAddress,
                                                 int port);
    public static final native int myInetAccept(int sock,
                                                int[] ipAddress,
                                                int[] port);
    public static final native int myInetGetSockName(int sock,
                                                     int[] ipAddress,
                                                     int[] port);
    public static final native int myInetGetPeerName(int sock,
                                                     int[] ipAddress,
                                                     int[] port);

    public static native int setSoReuseAddr(int sock,boolean reuseAddr);
    public static native int getSoReuseAddr(int sock);
    public static native int setSoKeepAlive(int sock,boolean keepAlive);
    public static native int getSoKeepAlive(int sock);
    public static native int setSoLinger(int sock,boolean onoff,int linger);
    public static native int getSoLinger(int sock,boolean[] onoff,int[] linger);
    public static native int setSoTimeout(int sock,int timeout);
    public static native int getSoTimeout(int sock);
    public static native int setSoOOBInline(int sock,boolean oobInline);
    public static native int getSoOOBInline(int sock);
    public static native int setTcpNoDelay(int sock,boolean noDelay);
    public static native int getTcpNoDelay(int sock);

    public static native int waitpid(int wpid,int[] status,int options);
    
    // note: these are macros
    public static native int WIFEXITED(int status);
    public static native int WEXITSTATUS(int status);
    public static native int WIFSIGNALED(int status);
    public static native int WTERMSIG(int status);
    public static native int WCOREDUMP(int status);
  
    // the next two methods are used for IO test cases.  but they
    // may be generally useful...  in any case, myPipe() maps
    // to the pipe(2) syscall.  mySocketpair() maps to
    // socketpair(2) with the first three arguments being set
    // to create a pair of local domain stream sockets.
    public static native int myPipe(int[] fds);
    public static native int mySocketpair(int[] sv);
    
    // useful for blocking io tests.  you can make your socket/pipe/whatever
    // non-blocking using this call, and then assert that your otherwise
    // blocking operation makes progress (reads at least one byte or writes
    // at least one byte).  the reason why that works is that the blocking
    // io manager (or whatever replaces it) should cause reads and writes
    // to block (by making your thread not-ready until the OS says that
    // data is available) regardless of whether or not they would block
    // in the OS.  so, if blocking io manager is not working, then a read
    // or write call on a non-blocking file descriptor may not make progress.
    // but if the blocking io manager is working, progress should always be
    // made, at the cost of some of the calls blocking.
    public static native int makeNonBlocking(int fd);
    public static native int makeBlocking(int fd);
    
    // returns the number of bytes that are available for reading using
    // ioctl(FIONREAD)
    public static native int availableForRead(int fd);
    
    // overload read and write to operate on the contents of byte
    // arrays, rather than raw pointers
    public static native int read(int fd, byte[] arr, int nbytes);
    public static native int write(int fd, byte[] arr, int nbytes);

    // wrappers around stat and fstat define in native_helpers.c
    public static native long    file_size(int fd);
    public static native long    file_size_path(byte[] path);
    public static native boolean is_file_valid(int fd);

    public static native int unlink(byte[] path);
    public static native int rmdir(byte[] path);
    public static native int getmod(byte[] path);
    public static native int chmod(byte[] path, int mode);
    
    public static native int rewindFd(int fd);
    // !!! buf is VM allocated, name is small
    public static native int list_directory(byte[] name,
					    byte[] buf,
					    int max);
    // !!! name is small
    public static native int estimate_directory(byte[] name);


    // These are our locally defined helper methods. All printing operations 
    // ensure that the FILE is in blocking mode before performing the action.


    /**
     *  parse a double representation in chars into a double value
     * using <tt>strtod</tt>
     *
     * @param c_str the double representation as a C string
     * @return      the double value as defined by <tt>strtod</tt>
     */
    public static native double strtod_helper(byte[] c_str);

    public static native void print_ustring_at_address( VM_Address ptr );


    /**
     * Reads an int from stdin in blocking mode
     * @return the int read or 0x80000000 on error
     */
    public static native int readInt();

    /**
     * Reads an long from stdin in blocking mode
     * @return the long read or 0x8000000000000000 on error
     */
    public static native long readLong();
    
    public static native void turnOffBuffering();

    /**
     * Prints the given string to the file pointed to by <code>fp</code>
     * using the <code>fprintf</code> function.
     * The file is forced into blocking mode for this operation.
     * @param fp a native file pointer (such as stdout or stderr)
     * @param str the string to print
     * @return the return value from <code>fprintf</code>
     */
    public static int print_string_on(FilePtr fp, String str) {
        if (fp.getValue() == NativeConstants.STDOUT) {
            System.out.print(str);
        } else if (fp.getValue() == NativeConstants.STDERR){
            System.err.print(str);
        }
        else {
            throw new OVMError("Shouldn't be doing this under a host VM");
        }
        return 0;
    }

    /**
     * Prints part of the given string to the file pointed to 
     * by <code>fp</code> using the <code>fprintf</code> function.
     * The file is forced into blocking mode for this operation.
     * @param fp a native file pointer (such as stdout or stderr)
     * @param str the string to print
     * @param offset the index of the first character in the string to print
     * @param nchars the number of characters from the string to print
     * @return the return value from <code>fprintf</code>
     */
    public static int print_substring_on(FilePtr fp, String str, 
                                         int offset, int nchars) {
        if (fp.getValue() == NativeConstants.STDOUT) {
            System.out.print(str.substring(offset, nchars));
        } else if (fp.getValue() == NativeConstants.STDERR){
            System.err.print(str.substring(offset, nchars));
        }
        else {
            throw new OVMError("Shouldn't be doing this under a host VM");
        }
        return 0;
    }

    /**
     * Prints part of the given string to <tt>stderr</tt>
     * (forced into blocking mode for this operation).
     * @param str the string to print
     * @param offset the index of the first character in the string to print
     * @param nchars the number of characters from the string to print
     * @return the return value from <code>fprintf</code>
     */
    public static int print_substring(String str, int offset, int nchars) {
        System.err.print(str.substring(offset, nchars));
        return 0;
    }


    /**
     * Prints the given string to <tt>stderr</tt>      
     * (forced into blocking mode for this operation) 
     * using the <code>fprintf</code> function.
     * @param str the string to print
     * @return the return value from <code>fprintf</code>
     */
    public static int print_string(String str) {
        System.err.print(str);
        return 0;
    }

    public static int print_ptr(VM_Address ptr) {
        System.err.print(ptr.asInt());
        return 0;
    }
    
    public static int print_bytearr_len(byte[] str,int len) {
	System.err.write(str,0,len);
	return 0;
    }
    
    public static int print(String str) {
        return print_string(str);
    }

    public static int print_boolean(boolean b) {
      if (b) {
        return print_string("true");
      } else {
        return print_string("false");
      }
    }

    /**
     * Prints the given int to the file pointed to by <code>fp</code>
     * using the <code>fprintf</code> function.
     * The file is forced into blocking mode for this operation.
     * @param fp a native file pointer (such as stdout or stderr)
     * @param val the value to print
     * @return the return value from <code>fprintf</code>
     */
    public static int print_int_on(FilePtr fp, int val) {
        if (fp.getValue() == NativeConstants.STDOUT) {
            System.out.print(val);
        } else if (fp.getValue() == NativeConstants.STDERR){
            System.err.print(val);
        }
        else {
            throw new OVMError("Shouldn't be doing this under a host VM");
        }
        return 0;
    }

    /**
     * Prints the given int to <tt>stderr</tt>
     * using the <code>fprintf</code> function.
     * @param val the value to print
     * @return the return value from <code>fprintf</code>
     */
    public static int print_int(int val) {
        System.err.print(val);
        return 0;
    }

    /**
     * Prints the given int to <tt>stderr</tt>
     * using the <code>fprintf</code> function with the <code>%u</code>
     * format specifier.
     * @param val the value to print
     * @return the return value from <code>fprintf</code>
     */
    public static native int print_int_as_unsigned(int val) ;

    /**
     * Prints the given int to <tt>stderr</tt>
     * using the <code>fprintf</code> function with the
     * <code>%x</code> format.
     * @param val the value to print
     * @return the return value from <code>fprintf</code>
     */
    public static int print_hex_int(int val) {
        System.err.print(val);      // FIXME: should maybe print as hex
        return 0;
    }

    /**
     * Prints the given character directly to the file pointed to 
     * by <code>fp</code> using the <code>fputc</code> function.
     * The file is forced into blocking mode for this operation.
     * @param fp a native file pointer (such as stdout or stderr)
     * @param ch the char to print
     * @return the return value from <code>fputc</code>
     */
    public static int print_char_on(FilePtr fp, int ch) {
        if (fp.getValue() == NativeConstants.STDOUT) {
            System.out.print((char)ch);
        } else if (fp.getValue() == NativeConstants.STDERR){
            System.err.print((char)ch);
        }
        else {
            throw new OVMError("Shouldn't be doing this under a host VM");
        }
        return 0;
    }

    /**
     * Prints the given character to
     * <code>stderr</code> using the <code>fputc</code> function.
     * stderr is forced into blocking mode for this operation.
     * @param ch the char to print
     * @return the return value from <code>fputc</code>
     */
    public static native int print_char(int ch);


    /**
     * Prints the given long to the file pointed to by <code>fp</code>
     * using the <code>fprintf</code> function.
     * @param fp a native file pointer (such as stdout or stderr)
     * @param val the value to print
     * @return the return value from <code>fprintf</code>
     */
    public static native int print_long_on(FilePtr fp, long val);

    /**
     * Prints the given long to stderr
     * using the <code>fprintf</code> function.
     * @param val the value to print
     * @return the return value from <code>fprintf</code>
     */
    public static native int print_long(long val);

    /**
     * Prints the given long to stderr
     * using the <code>fprintf</code> function with the <code>%x</code>
     * format.
     * @param val the value to print
     * @return the return value from <code>fprintf</code>
     */
    public static native int print_hex_long(long val);

    /**
     * Prints the given pointer to stderr using the <code>fprintf</code>
     * function.
     */
//    public static native int print_ptr(VM_Address ptr);

    /**
     * Prints the given float to the file pointed to by <code>fp</code>
     * using the <code>fprintf</code> function.
     * @param fp a native file pointer (such as stdout or stderr)
     * @param val the value to print
     * @return the return value from <code>fprintf</code>
     */
    public static native int print_float_on(FilePtr fp, float val);


    /**
     * Prints the given double to the file pointed to by <code>fp</code>
     * using the <code>fprintf</code> function.
     * @param fp a native file pointer (such as stdout or stderr)
     * @param val the value to print
     * @return the return value from <code>fprintf</code>
     */
    public static native int print_double_on(FilePtr fp, double val);

    public static native int print_double(double val);


    /** @return a file pointer representing the stderr stream
     * <p>You should convert the returned raw pointer value into a typesafe
     * pointer value as follows:
     * <pre><code>
     *     FilePtr myfile = new FilePtr(getStdErr());
     * </code></pre>
     */
    public static int getStdErr() { return NativeConstants.STDERR; }


    /** @return a FILE pointer representing the stdout stream
     * <p>You should convert the returned raw pointer value into a typesafe
     * pointer value as follows:
     * <pre><code>
     *     FilePtr myfile = new FilePtr(getStdOut());
     * </code></pre>
     */
    public static int getStdOut() { return NativeConstants.STDOUT; }

    /** @return a file pointer representing the stdin stream
     * <p>You should convert the returned raw pointer value into a typesafe
     * pointer value as follows:
     * <pre><code>
     *     FilePtr myfile = new FilePtr(getStdIn());
     * </code></pre>
     */
    public static native int getStdIn();



    // error related support functions

    /**
     * Return the current value of <code>errno</code> for this thread
     * @return the current value of <code>errno</code> for this thread
     */
    public static native int getErrno();
    
    public static native int getHErrno();
    
    
    /**
     * 
     * @param filename
     * @param mode
     * @return a FILE pointer
     */
    public static native int fopen(byte[] filename, byte[] mode);

    /**
     * Fill in the given buffer with the string message corresponding
     * to the last error as stored in <code>ERRNO</code>.
     * This is obtained using
     * the <code>strerror</code> function.
     * @param buf the location to store the message
     * @param len the maximum length of the message to be stored. It is
     * required that <code>len <= buf.length</code>.
     * @return on success, the number of characters written into the array.
     * If an error occurs then a negative value is returned.
     * If the return value is greater than or equal to <tt>len</tt> it means
     * the buffer was not large enough to hold the error string and so the 
     * string has been truncated.
     */
    // fixme this is a trusted method, len <= buf.length
    public static native int get_error_string(byte[] buf, int len);

    public static native int gethostname(byte[] buf, int len);

    public static native int get_host_by_addr(byte[] ip, 
					      int iplen,
					      int af,
					      byte[] buf,
					      int buflen);

    public static native long get_host_by_name(byte[] name,
					       byte[] ret,
					       int retlen);
    
    /**
     * Fill in the given buffer with the string message corresponding
     * to the specified error code. This is obtained using
     * the <code>strerror</code> function.
     * @param errno the error code to be used
     * @param buf the location to store the message
     * @param len the maximum length of the message to be stored. It is
     * required that <code>len <= buf.length</code>.
     *
     * @return on success, the number of characters written into the array.
     * If an error occurs then a negative value is returned.
     * If the return value is greater than or equal to <tt>len</tt> it means
     * the buffer was not large enough to hold the error string and so the 
     * string has been truncated.
     */
    // fixme this is a trusted method, len <= buf.length
    public static native int get_specific_error_string(int errno, byte[] buf, int len);

    public static native int get_specific_h_error_string(int h_errno, byte[] buf, int len);


    // time/clock related functions - these should probably be split out
    // sometime


    /**
     * Returns the current time in nanoseconds since the epoch.
     * If running on a system that defines <code>_POSIX_TIMERS</code>
     * then the POSIX <code>clock_gettime</code> function is used, querying
     * the real-time clock; otherwise this
     * uses the UNIX <code>gettimeofday</code> function.
     * @return the current time in nanoseconds since the epoch, or -1
     * if an error occurred. If an error occurs then ERRNO is set.
     **/
    public static native long getCurrentTime();
    
    public static native long getTimeStamp();
    
    public static native void ovm_outb( int value, int address );
    public static native int ovm_inb( int address );

    /**
     * Returns the resolution of the clock used to report the current time.
     * This is the smallest interval, in nanoseconds that could possibly be
     * observed  between two call to {@link #getCurrentTime}.
     * If running on a system that defines <code>_POSIX_TIMERS</code>
     * then the POSIX <code>clock_getclockres</code> function is used, querying
     * the real-time clock; otherwise there is no standard system call to make
     * and so the value representing the default UNIX 10ms clock resolution is
     * returned.
     * @return the clock resolution in nanoseconds, or -1
     * if an error occurred. If an error occurs then ERRNO is set.
     **/
    public static native int getClockResolution();

    /**
     * Queries whether the underlying runtime system supports the use of
     * the POSIX 1003.1b real-time timers API.
     * @return <code>true</code> if they are supported and <code>false</code>
     * otherwise.
     *
     */
    public static native boolean supportsPOSIXTimers();

    /**
     * Gets the current CPU clock tick in 64 bits (only on X86)
     * @return the current CPU clock tick
     **/

    public static native long getClockTickCount();

    public static native float getClockFrequency();


    // misc system calls and other native wrappers
    
    public static native void exit_process(int code);
    
    public static native void abort();

    /**
     * This wrapper around abort can be useful where obtaining
     * core-files is problematic.  Rather than simply calling
     * abort(2), print a message including the current PID to stderr,
     * and wait secs many seconds for the user to the dying process to
     * gdb.
     * <p>
     * Obtaining a corefile on a mac can be problematic.  Core files
     * are dropped in the /cores directory, which is only writable by
     * the group admin.  Also, core files may not save thread
     * information, in which case there is no way to obtain a stack
     * trace.
     */
    public static native void abortDelayed(int secs);
    
    /**
     * Allocate a block of memory from the OS.
     * @param size the size of the block in INTs.
     * @return a block of memory, without type or length field set!
     **/
    public static native VM_Address getmem(int size);
    
    /*
      Get memory for Java heap - on most platforms, it falls back
      to getmem.
    */
    public static native VM_Address getheap(int size);

    /**
     * Free a block of memory (give back to OS).
     * @param memblock a block of memory, must have been obtained from getMem
     *        earlier!
     **/
    public static native void freemem(VM_Address memblock);

    
    
    /**
     * fill in buf up to buf_len with contents of argv[index]
     * @return number of (nonnul) bytes written into buf
     **/
    public static native int get_process_arg(int index, byte[] buf, int buf_len);
    /**
     * get argc
     **/
    public static native int get_process_arg_count();
    

    /**
     * Address of first java object in the boot image
     **/
    public static native VM_Address getImageBaseAddress();
    /**
     * Address just past the last java object in the boot image
     **/
    public static native VM_Address getImageEndAddress();
    
    // event management configuration methods
    public static native void makeSignalEventSimple();
    public static native void makeSignalEventBroken();
    public static native void makeSignalEventFromThreadSimple();
    public static native void makeSignalEventFromThreadBroken();
    public static native void makeSignalEventFromThreadProper();
    
    public static native int sched_yield();
    
    public static native int getStoredLineNumber();
    
    public static native void generateTimerInterrupt();
}


