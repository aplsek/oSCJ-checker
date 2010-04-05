// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/SocketStateCallback.java,v 1.2 2004/02/20 08:52:42 jthomas Exp $

package s3.services.io.async;

/**
 * Almost all sockets change their readability and writability
 * depending on their bound and connected state.  For example,
 * an AF_INET/SOCK_STREAM socket will only be readable and
 * writable when connected; when disconnected, and even when
 * bound, it will be neither writable nor readable.  By contrast,
 * a AF_INET/SOCK_DGRAM socket will be readable iff bound and
 * writable iff connected (oops: will it really be writable,
 * or just 'sendable'?).  So, in order to allow for all of these
 * crazy possibilities without hard-coding them in
 * <code>EdgeTriggerableSocketDescriptor</code>, we introduce
 * this here interface, instances of which are passed to
 * <code>EdgeTriggerableSocketDescriptor</code> on construction.
 * @author Filip Pizlo
 */
interface SocketStateCallback {
    public void setConnected(boolean connected,
                             Setter rw);
    public void setBound(boolean bound,
                         Setter rw);
    interface Setter {
        public void setReadable(boolean readable);
        public void setWritable(boolean writable);
    }
}

