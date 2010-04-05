// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/clients/PosixIO.java,v 1.20 2007/06/03 01:25:47 baker29 Exp $

package ovm.services.io.clients;
import ovm.core.domain.Domain;
import ovm.core.domain.Oop;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.core.execution.*;

/**
 * Provides the I/O abstraction layer as defined by the POSIX (IEEE 1003.1)
 * specification, with some additional utility functions. 
 * This includes the basic synchronous operations on file
 * descriptors, as well as some socket and pipe operations.
 *
 * <p>The particular implementation of this I/O sub-system for a given
 * configuration is obtained via the {@link #factory factory} object.
 *
 * @author Jason Baker
 * @author Filip Pizlo
 * @author David Holmes
 */
public abstract class PosixIO {
    
    /**
     * @return true if the file descriptor is valid
     */
    public abstract boolean isValid(int fd);
    
    /**
     * Force the file descriptor
     * to not be reused until a call to ecrofValid(fd).  These calls may be interweaved
     * concurrently.  The file descriptor will only become a candidate for reuse when
     * for every call to forceValid(fd) there has been a call to ecrofValid(fd).
     */
    public abstract void forceValid(int fd);
    
    /**
     * Allows a file descriptor that has been passed as an argument to forceValid(fd)
     * to be reused provided that for every call to forceValid(fd) there has been a
     * call to ecrofValid(fd).
     */
    public abstract void ecrofValid(int fd);

    /**
     * @return true if the file is a directory.
     */
    public abstract boolean isDirectory(Oop string);

    /**
     * @return true if the file is a directory.
     */
    public abstract boolean isPlainFile(Oop string);
    
    /**
     * Maps to the <tt>open</tt> system call.
     * @param name a domain-specific string naming the file to open
     * @param flags the flags to be applied to the opened file
     * @param mode the access permission to be applied to a newly created file.
     *             NOTE: the mode must not contain O_NONBLOCK
     * @return a new file descriptor for the opened file, or -1 on error
     */
    public abstract int open(Oop name, int flags, int mode);
    
    /**
     * Maps to the <tt>mkstemp</tt> system call.  However, since
     * strings are immutable, but mkstemp must manipulate the string,
     * the Oop argument is actually an array of length greater than
     * zero where the 0th element is a reference to a string.  This
     * reference will be replaced, upon success, with a referene to
     * a new string that is the real name of the temporary file.
     * @param template the template for the temporary file name.
     * @return a new file descriptor for the opened file, or -1 on error
     */
    public abstract int mkstemp(Oop template);

    /**
     * Maps to the <tt>mkdir</tt> system call.
     */
    public abstract int mkdir(Oop name,
			      int mode);
    
 
    /**
     * Maps to the <tt>rename</tt> system call.
     */
    public abstract int renameTo(Oop oldpath,
				 Oop newpath);
    
 
    /**
     * Obtain last modification time using <tt>stat</tt> system call.
     */
    public abstract long getLastModified(Oop name);
    
 
    /**
     * Set last modification time.
     */
    public abstract int setLastModified(Oop name,
					long time);
    
    /**
     * Maps to the <tt>read</tt> system call.
     * @param fd the file descriptor to read from
     * @param buf the domain-specific byte-array to write to
     * @param byteOffset the offset in <tt>buf</tt> to start writing in
     * @param byteCount the number of bytes to read
     * @return the number of bytes read, or -1 on error
     */
    public abstract int read(int fd,
                             Oop buf,
                             int byteOffset,
                             int byteCount,
                             boolean blocking);
    
    /**
     * Returns the number of bytes that are immediately available for reading.
     */
    public abstract int getAvailable(int fd);
    
    /**
     * Skips some number of bytes.  Does so in the most efficient way for
     * the given file descriptor type.
     * @return the number of bytes skipped, or -1 on error
     */
    public abstract long skip(int fd,
                              long offset,
                              boolean blocking);

    /**
     * Maps to the <tt>write</tt> system call.
     * @param fd the file descriptor to write to
     * @param buf the domain-specific byte-array holding the data to write
     * @param byteOffset the offset in <tt>buf</tt> thats holds the first
     * byte to write
     * @param byteCount the number of bytes to write
     * @return the number of bytes written, or -1 on error
     */
    public abstract int write(int fd,
                              Oop buf,
                              int byteOffset,
                              int byteCount,
                              boolean blocking);

    /**
     * Maps to the <tt>close</tt> system call.
     * @param fd the file descriptor to close
     * @return zero on success, and -1 on error
     */
    public abstract int close(int fd);
    
    /**
     * Cancel any blocked IO operations on the file descriptor.
     * @param fd the file descriptor on which to perform the cancelation
     * @return zero on success, and -1 on error
     */
    public abstract int cancel(int fd);

    /**
     * Maps to the <tt>pipe</tt> system call.
     * @param fds a domain-specific <tt>int[2]</tt>
     * @return zero on success, else -1 on error
     */
    public abstract int pipe(Oop fds);

    // fixme: finish these socket descriptions

    /**
     * Maps to the <tt>socketpair</tt> system call.
     * @param domain the socket domain, such as <tt>AF_INET</tt> or
     *               <tt>AF_UNIX</tt>.  For the <tt>socketpair</tt>
     *               syscall, you are usually limited to the <tt>AF_UNIX</tt>
     *               domain.
     * @param type the socket type, such as <tt>SOCK_STREAM</tt> or
     *             <tt>SOCK_DGRAM</tt>.  For the <tt>socketpair</tt>
     *             syscall, you are usually limited to the <tt>SOCK_STREAM</tt>
     *             type.
     * @param protocol the protocol to use.  this will usually be 0, indicating
     *                 that the protocol is unambiguously determined by
     *                 <code>domain</code> and <code>type</code>.
     * @param sb a domain-specific <tt>int[2]</tt> that gets populated with
     *           the two new file descriptors for the two sockets.  The two
     *           descriptors are identical, and anything written onto one
     *           will appear on the reading end of the other.
     * @return zero on success and -1 on error.
     */
    public abstract int socketpair(int domain,int type,int protocol,Oop sb);
    
    /**
     * Maps to the <tt>socket</tt> system call.
     * @param domain the socket domain, such as <tt>AF_INET</tt> or
     *               <tt>AF_UNIX</tt>.
     * @param type the socket type, such as <tt>SOCK_STREAM</tt> or
     *             <tt>SOCK_DGRAM</tt>.
     * @param protocol the protocol to use.  this will usually be 0, indicating
     *                 that the protocol is unambiguously determined by
     *                 <code>domain</code> and <code>type</code>.
     * @return the new file descriptor (a non-negative integer) on success
     *         or -1 on error.
     */
    public abstract int socket(int domain,int type,int protocol);
    
    /**
     * Maps to the <tt>bind</tt> system call.
     * @param sock the socket file descriptor for which the binding is to be
     *        done.
     * @param address a domain-specific object of a compound type that
     *        contains at a minimum a field of type <code>int</code>
     *        by the name <code>family</code> that corresponds to the
     *        domain parameter in the <tt>socket</tt> syscall.  What
     *        other fields are expected in this object depends on the
     *        value of <code>family</code>.  If, for example, <code>family</code>
     *        is <tt>AF_INET</tt>, then the fields <code>long addr</code> and
     *        <code>int port</code> are expected.  In general, the fields and
     *        their names map to the <tt>sockaddr</tt> structures used in
     *        BSD sockets.
     * @return zero on success and -1 on error.
     */ 
    public abstract int bind(int sock,Oop address);
    
    /**
     * Maps to the <tt>connect</tt> system call.
     * @param sock the socket file descriptor for which the connecting is to be
     *        done.
     * @param address a domain-specific object of a compound type that
     *        contains at a minimum a field of type <code>int</code>
     *        by the name <code>family</code> that corresponds to the
     *        domain parameter in the <tt>socket</tt> syscall.  What
     *        other fields are expected in this object depends on the
     *        value of <code>family</code>.  If, for example, <code>family</code>
     *        is <tt>AF_INET</tt>, then the fields <code>long addr</code> and
     *        <code>int port</code> are expected.  In general, the fields and
     *        their names map to the <tt>sockaddr</tt> structures used in
     *        BSD sockets.
     * @return zero on success and -1 on error.
     */
    public abstract int connect(int sock,
                                Oop address);

    // NOTE: there is not, nor will there ever be, a non-blocking connect()
    // call.  For details, read the comment in
    // ovm.services.io.async.SocketIODescriptor

    /**
     * Maps to the <tt>accept</tt> system call.
     * @param sock the socket file descriptor that represents a bound
     *             socket on which to do the accepting.
     * @param address a domain-specific object of a compound type that
     *        contains at a minimum a field of type <code>int</code>
     *        by the name <code>family</code> that corresponds to the
     *        domain parameter in the <tt>socket</tt> syscall.  What
     *        other fields are expected in this object depends on the
     *        value of <code>family</code>.  If, for example, <code>family</code>
     *        is <tt>AF_INET</tt>, then the fields <code>long addr</code> and
     *        <code>int port</code> are expected.  In general, the fields and
     *        their names map to the <tt>sockaddr</tt> structures used in
     *        BSD sockets.
     * @return a new socket file descriptor on success or -1 on error.
     */
    public abstract int accept(int sock,
                               Oop address,
                               boolean blocking);
    
    public abstract int getsockname(int sock,Oop address);
    public abstract int getpeername(int sock,Oop address);
    
    /**
     * Maps to the <tt>listen</tt> system call.
     * @param sock the socket file descriptor for which to do the listening.
     * @param queueSize the queue limit to be passed to the <tt>listen</tt> syscall.
     * @return zero on success and -1 on error.
     */
    public abstract int listen(int sock,int queueSize);
    
    /**
     * Maps to <tt>setsockopt</tt> with the <tt>SOL_SOCKET</tt>/
     * <tt>SO_REUSEADDR</tt> parameters.
     * @param sock the socket file descriptor for which to set the <tt>SO_REUSEADDR</tt>
     *             option.
     * @param reuseAddr the new value of the <tt>SO_REUSEADDR</tt> option.
     * @return zero on success and -1 on error.
     */
    public abstract int setSoReuseAddr(int sock,boolean reuseAddr);

    /**
     * Receive a datagram from an UDP socket.
     * @param sock the UDP socket
     * @param source_addr set to the address from where the packet was received;
     *        contains at a minimum a field of type <code>int</code>
     *        by the name <code>family</code> that corresponds to the
     *        domain parameter in the <tt>socket</tt> syscall.  What
     *        other fields are expected in this object depends on the
     *        value of <code>family</code>.  If, for example, <code>family</code>
     *        is <tt>AF_INET</tt>, then the fields <code>long addr</code> and
     *        <code>int port</code> are expected.  In general, the fields and
     *        their names map to the <tt>sockaddr</tt> structures used in
     *        BSD sockets.
     * @param dstBuf buffer to write the received datagram to
     * @return number of bytes in datagram, -1 on error
     */
    public abstract int receive(int sock,
				Oop source_addr,
				Oop dstBuf,
				int len,
                                boolean blocking);

    /**
     * Transmits an UDP datagram.
     * @param address the target address, a domain-specific object of a compound type that
     *        contains at a minimum a field of type <code>int</code>
     *        by the name <code>family</code> that corresponds to the
     *        domain parameter in the <tt>socket</tt> syscall.  What
     *        other fields are expected in this object depends on the
     *        value of <code>family</code>.  If, for example, <code>family</code>
     *        is <tt>AF_INET</tt>, then the fields <code>long addr</code> and
     *        <code>int port</code> are expected.  In general, the fields and
     *        their names map to the <tt>sockaddr</tt> structures used in
     *        BSD sockets.
     * @param buf a byte[] with the data to transmit
     * @param off offset into buf where to start
     * @param len how many bytes from off to send
     * @return 0 on success, -1 on error
     */
    public abstract int sendto(Oop address,
			       Oop buf,
			       int off,
			       int len);

    public abstract int getSoReuseAddr(int sock);
    
    public abstract int setSoKeepAlive(int sock,boolean keepAlive);
    public abstract int getSoKeepAlive(int sock);
    
    /** linger is a structure that contains:
     * <ul><li>boolean onoff</li>
     * <li>int linger</li></ul>
     */
    public abstract int setSoLinger(int sock,Oop linger);

    /** linger is a structure that contains:
     * <ul><li>boolean onoff</li>
     * <li>int linger</li></ul>
     */
    public abstract int getSoLinger(int sock,Oop linger);
    
    public abstract int setSoOOBInline(int sock,boolean oobInline);
    public abstract int getSoOOBInline(int sock);
    
    public abstract int setTcpNoDelay(int sock,boolean noDelay);
    public abstract int getTcpNoDelay(int sock);

    /**
     * Utility function to query if a file of the given name has
     * certain access properties (man 2 access).
     * @param name the domain-specific string naming the file
     * @param mode the access mode to check
     * @return <tt>true</tt> if the mode is available exists and <tt>false</tt>
     * otherwise
     */
    public abstract boolean access(Oop name,
				   int mode);
    
    /**
     * maps to the <tt>lseek</tt> syscall.
     * @return the new absolute location in the file.
     */
    public abstract long lseek(int fd,
			       long offset,
			       int whence);
    
    /**
     * Calls <tt>lseek</tt> to rewind the file to the beginning.
     * @param fd the file descriptor to rewind
     * @return 0 on success, -1 on error.
     */
    public int rewind(int fd) {
	if (lseek(fd,0,NativeConstants.SEEK_SET)<0) {
	    return -1;
	}
	return 0;
    }
    
    /**
     * Maps to the <tt>unlink</tt> system call.
     * @param name the name of the file to unlink.
     * @return -1 on error, 0 on success.
     */
    public abstract int unlink(Oop name);

    /**
     * Maps to the <tt>rmdir</tt> system call.
     * @param name the name of the file to unlink.
     * @return -1 on error, 0 on success.
     */
    public abstract int rmdir(Oop name);

    /**
     * Maps to the <tt>stat</tt> system call to obtain file access modifiers.
     * @param name the name of the file to stat
     * @return -1 on error, modifiers otherwise.
     */
    public abstract int getmod(Oop name);

    /**
     * Maps to the <tt>chmod</tt> system call.
     * @param name the name of the file to change modifiers for
     * @return -1 on error, 0 on success.
     */
    public abstract int chmod(Oop name, int mode);

    /**
     * List the files in the given directory.
     */
    public abstract byte[] list_directory(Oop name);

    /**
     * Query the file size in bytes
     * @param name the domain-specific string naming the file
     * @return the file size
     */
    public abstract long length(Oop name);
    
    /**
     * Query the file size of an open file
     * @param fd file descriptor
     * @return the file size
     */
    public abstract long length(int fd);

    /**
     * Change the size of an open file
     * @param fd file descriptor
     * @param size the new size
     * @return 0 for success, -1 for failure
     */
    public abstract int ftruncate(int fd, long size);

    /**
     * Lock a region in a file.
     */
    public abstract int lock(int fd,
			     long start,
			     long size,
			     boolean shared,
			     boolean wait);

    /**
     * Lock a region in a file.
     */
    public abstract int unlock(int fd,
			       long start,
			       long size);
    
    /**
     * Sync the file buffers
     * @param fd file descriptor
     * @return 0 for success, -1 for failure
     */
    public abstract int fsync(int fd);

    public abstract Oop getHostByName(Oop hostname);
    
    public abstract Oop getHostByAddr(Oop ip,int af);

    /**
     * Utility function to read the value of <tt>errno</tt> for the
     * current thread. This value is only set when an error occurs.
     * @return the value of <tt>errno</tt> for this thread
     */
    public abstract int getErrno();    

    public abstract int getHErrno();

    /**
     * Factory interface for creating a specific <tt>PosixIO</tt>
     * implementation
     */
    public static interface Factory {
        /**
         * Return a <tt>PosixIO</tt> implementation for the given domain
         * @param dom the domain for this implementation to use
         * @return a <tt>PosixIO</tt> implementation for the given domain
         */
	PosixIO make(Domain dom);
    }

    /**
     * Return the currently configured factory instance.
     */
    public static Factory factory() throws PragmaStitchSingleton {
	return (PosixIO.Factory) InvisibleStitcher.singletonFor(Factory.class);
    }
}
