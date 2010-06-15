// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/FileDescriptorWrapifier.java,v 1.6 2007/06/03 01:25:47 baker29 Exp $

package ovm.services.io.async;

/**
 * Interface responsible for wrapping file descriptors with the most appropriate
 * IODescriptor implementation by introspecting the file descriptors' abilities.
 * @author Filip Pizlo
 */
public interface FileDescriptorWrapifier extends ovm.services.ServiceInstance {
    
    /**
     * Wrap the given file descriptor with the most appropriate <code>IODescriptor</code>
     * implementation by introspecting the file descriptor's abilities.  Note that
     * after the finalizer completes, the file descriptor is owned by the
     * returned <code>IODescriptor</code> object.  If an error occurs, the
     * file descriptor is closed.
     * @param fd the file descriptor to wrap
     * @param cback callback that gets passed an AsyncBuildFinalizer
     */
    public void wrap(int fd,
                     AsyncCallback cback);
    
    /**
     * Like <code>wrap()</code> but is not asynchronous (as in, it will block the whole
     * VM).  This can and should be used in the following circumstances:
     * <ul>
     * <li>You are in the VM initialization, or</li>
     * <li>You know that the file descriptor does not refer to the file system or
     *     unknown exotic devices.  So, if you know that the file descriptor is one
     *     of TTY, socket or pipe, then calling this makes perfect sense.</li>
     * </ul>
     * It is <emph>highly recommended</emph> that every call to this method be
     * justified with a comment saying either that it is OK to call it because you are
     * in VM initialization, or that it is OK because you know something about the
     * file descriptor already.
     */
    public IODescriptor wrapNow(int fd) throws IOException;
    
    /**
     * The wrapifier must know about different file descriptor types.  This is
     * an object that describes types.  Each instance of this object represents
     * a distinct type.
     */
    public static class FDType {
        private String name_;
        public FDType(String name) {
            this.name_ = name;
        }
        public String getName() {
            return name_;
        }
        public String toString() {
            return "FDType: "+name_;
        }
    }
    
    /**
     * The socket file descriptor type.
     */
    public static final FDType FD_TYPE_SOCKET  = new FDType("socket");
    
    /**
     * The 'file' file descriptor type.  This indicates not just any file,
     * but regular disk files specifically.
     */
    public static final FDType FD_TYPE_FILE    = new FDType("file");
    
    /**
     * Use this type any time that you do not know the type.  If the
     * FileDescriptorWrapifier is ever given a type that it has no
     * record of, it will internally convert it to this one.
     */
    public static final FDType FD_TYPE_UNKNOWN = new FDType("unknown");
    
    /**
     * Once a type is known, the FileDescriptorWrapifier calls upon a
     * SpecificWrapifier object to produce an IODescriptor.
     */
    public static interface SpecificWrapifier {
        public IODescriptor wrap(FDType type, int fd);
    }
    
    /**
     * To register a specific wrapifier, call this.
     */
    public void register(FDType type,
                         SpecificWrapifier wrapifier);
    
    /**
     * DON'T USE THIS UNLESS YOU REALLY KNOW WHAT YOU'RE DOING!
     * This forces a file descriptor to be wrapped as if it were of a particular
     * type.  Like wrapNow(), this method will cause the wrapping to happen
     * immediately.
     */
    public IODescriptor wrapAsType(int fd, FDType type);
}


