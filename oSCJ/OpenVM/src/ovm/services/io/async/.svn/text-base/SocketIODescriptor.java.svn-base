// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/SocketIODescriptor.java,v 1.6 2004/10/09 21:43:04 pizlofj Exp $

package ovm.services.io.async;

import ovm.core.services.memory.*;

/**
 *
 * @author Filip Pizlo
 */
public interface SocketIODescriptor extends RWIODescriptor {
    
    public void bind(SocketAddress address)
        throws IOException;
    
    public void listen(int queueSize)
        throws IOException;
    
    public boolean tryAcceptNow(IODescriptor[] iod,
				SocketAddress[] sa) throws IOException;

    public AsyncHandle accept(AsyncCallback cback);
    
    public static interface AcceptFinalizer extends AsyncBuildFinalizer {
        public SocketAddress getRemoteSocketAddress();
    }

    public static interface ReceiveFinalizer extends ReadFinalizer {
        public SocketAddress getRemoteSocketAddress();
    }
    
    // NOTE: there is not, nor will there ever be, a non-blocking variant of
    // connect().  this is because POSIX does not provide a connect() function
    // that is truly non-blocking.  Instead, calling connect() on a socket
    // that has non-blocking results in an asynchronous connect (that
    // is, if the connect() does not immediately succeed, it immediately
    // returns but continues in the background; the user is then given the
    // opportunity, using SIGIO, select(), or some other multiplexing mechanism,
    // to check when the connect() finishes).  Moreover, NIO's 'non-blocking'
    // connect() is also asynchronous - in much the same way as the POSIX
    // connect().  So, when implementing the so-called 'non-blocking' NIO
    // connect() function, we should instead provide a way for user land to
    // use the AsyncCallback mechanism.  Once this is done, it would be trivial
    // to implement NIO's non-blocking connect().

    public AsyncHandle connect(SocketAddress address,
			       AsyncCallback cback);
    
    public SocketAddress getPeerName() throws IOException;
    public SocketAddress getSockName() throws IOException;
        
    // FIXME: this should contain the annoying
    // recv/send/recvfrom/sendto/recvmsg/sendmsg syscalls
    
    // FIXME: this should contain all of the annoying socket option
    // getters and setters (see java.net.Socket - I think that though
    // painful, having getters/setters like in there would be the
    // Right Way).  NO!! don't have all of the annoying setters
    // and getters.  just make a setsockopt/getsockopt thingy.
    // OK. after a long while I settled for the individual getters/setters.
    
    public boolean getSoReuseAddr() throws IOException;
    
    public void setSoKeepAlive(boolean keepAlive) throws IOException;
    public boolean getSoKeepAlive() throws IOException;
    
    public void setSoLinger(Linger linger) throws IOException;
    public void getSoLinger(Linger linger) throws IOException;
    
    public void setSoReuseAddr(boolean reuseAddr)
        throws IOException;

    public int sendto(SocketAddress address,
		      AsyncMemoryCallback data,
		      int maxbytes)
	throws IOException;
   
    public int tryReceiveNow(VM_Address data,
			     int maxBytes,
			     SocketAddress[] sa) throws IOException;

    /**
     * Initiate an asynchronous receive.  To learn about how asynchronous
     * operations work, look at <code>AsyncCallback</code> and
     * <code>AsyncFinalizer</code>.  To learn
     * about why you need to pass in an <code>AsyncMemoryCallback</code>
     * rather than just a <code>VM_Address</code> or <code>byte[]</code>,
     * look at that class's documentation. 
     * The difference between read and receive is that receive obtains
     * the IP:port of the sender (via ReceiveFinalizer).
     * <p>
     * @param data an <code>AsyncMemoryCallback</code> that will return,
     *             when asked to do so, a pointer to a buffer of size
     *             <code>maxBytes</code>, into which the data will be read.
     * @param maxBytes the maximum number of bytes to read.  This operation
     *                 will only complete once at least one byte is read.
     *                 As such, <code>maxByte</code> being 0 will have
     *                 unknown consequences.
     * @param cback the callback that is called when this operation is ready
     *              to complete.  The <code>AsyncFinalizer</code> passed to
     *              <code>AsyncCallback.read()</code> is guaranteed to be
     *              an instance of <code>RWIODescriptor.ReadFinalizer</code>.
     */
    public AsyncHandle receive(AsyncMemoryCallback data,
			       int maxBytes,
			       AsyncCallback cback);
 

    public void setSoOOBInline(boolean oobInline) throws IOException;
    public boolean getSoOOBInline() throws IOException;
    
    public void setTcpNoDelay(boolean noDelay) throws IOException;
    public boolean getTcpNoDelay() throws IOException;
    
}

