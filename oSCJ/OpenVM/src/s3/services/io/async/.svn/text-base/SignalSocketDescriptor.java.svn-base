
package s3.services.io.async;

import ovm.core.execution.*;
import ovm.core.services.memory.*;
import ovm.services.io.async.*;
import ovm.services.io.signals.*;
import ovm.services.threads.*;

/**
 *
 * @author Filip Pizlo
 */
public class SignalSocketDescriptor
    extends SignalRWDescriptor
    implements SocketIODescriptor {
    
    private static final class NativeHelper implements NativeInterface {
        public static final native int getSoError(int sock);
    }
    
    SignalSocketDescriptor(UserLevelThreadManager tm,
                           IOSignalManager iosm,
                           int fd) {
        super(tm,iosm,fd);
    }
    
    public synchronized SocketAddress getSockName() throws IOException {
        int[] ipAddress=new int[1];
        int[] port=new int[1];
        IOException.System.check(Native.myInetGetSockName(getFD(),ipAddress,port),
                                 Native.getErrno());
        return new IPv4SocketAddress(ipAddress[0],port[0]);
    }
    
    public synchronized SocketAddress getPeerName() throws IOException {
        int[] ipAddress=new int[1];
        int[] port=new int[1];
        IOException.System.check(Native.myInetGetPeerName(getFD(),ipAddress,port),
                                 Native.getErrno());
        return new IPv4SocketAddress(ipAddress[0],port[0]);
    }
    
    public synchronized void bind(SocketAddress address) throws IOException {
        boolean enabled=tm.setReschedulingEnabled(false);
        try {
            IPv4SocketAddress ta=(IPv4SocketAddress)address;
            IOException.System.check(
                Native.myInetBind(getFD(),
                                  ta.getIPv4Address(),
                                  ta.getPort()),
                Native.getErrno());
        } catch (ClassCastException e) {
            throw IOException.Unsupported.getInstance();
        } finally {
            tm.setReschedulingEnabled(enabled);
        }
    }
    
    public synchronized void listen(int queueSize)
        throws IOException {
        IOException.System.check(
            Native.listen(getFD(),queueSize),
            Native.getErrno());
    }

 
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
    public synchronized AsyncHandle receive(AsyncMemoryCallback data,
					    int maxBytes,
					    AsyncCallback cback) {
	VM_Area prev=U.e(cback);
        try {
            return readQueue.performOp(new ReceiveOpNodeImpl(this,
							     data,
							     maxBytes,
							     cback));
        } finally {
            U.l(prev);
        }	
    }

    static class ReceiveOpNodeImpl extends ReadOpNodeImpl
	implements ReceiveFinalizer {
	private IPv4SocketAddress isa;
        
        public ReceiveOpNodeImpl(SignalDescriptorBase outer,
				 AsyncMemoryCallback data,
				 int maxBytes,
				 AsyncCallback cback) {
            super(outer,data,maxBytes,cback);
        }
        
        protected int doRead(VM_Address buf) {
	    int[] addr
		= new int[2];
            int ret 
		= Native.myrecvfrom(getFD(),
				    buf,
				    maxBytes,
				    NativeConstants.MSG_DONTWAIT,
				    addr);
	    if (ret == -1)
		return -1;
	    isa = new IPv4SocketAddress(addr[0],
					addr[1]);
	    return ret;
        }
	public SocketAddress getRemoteSocketAddress() {
	    return isa;
	}

    }

    public int tryReceiveNow(VM_Address data,
			     int maxBytes,
			     SocketAddress[] sa) throws IOException {
	VM_Area prev=U.e(sa);
	try {
	    int ret;
	    int[] addr=new int[2];
	    if (IOException.System
		.checkRepeat(ret=Native.myrecvfrom(getFD(),
						   data,
						   maxBytes,
						   NativeConstants.MSG_DONTWAIT,
						   addr),
			     Native.getErrno())) {
		return -1;
	    }
	    sa[0]=new IPv4SocketAddress(addr[0],addr[1]);
	    return ret;
	} finally {
	    U.l(prev);
	}
    }

    protected static class AcceptOpNode
        extends AsyncOpQueue.OpNode
        implements AcceptFinalizer {
        
	private SignalSocketDescriptor outer_;
	protected void setOuter(SignalSocketDescriptor outer) throws PragmaNoBarriers {
	    this.outer_=outer;
	}
	protected SignalSocketDescriptor getOuter() throws PragmaNoBarriers {
	    return outer_;
	}
	
        private SocketIODescriptor newSocket_;
        private SocketAddress newAddress_;
	
	private int[] ipAddress=new int[1];
	private int[] port=new int[1];
        
        public AcceptOpNode(SignalSocketDescriptor outer,
			    AsyncCallback cback) {
            super(cback);
	    setOuter(outer);
        }
        
        public final boolean doOp() {
            // NOTES:
            // Q: how the heck do we create the right subclass of
            //    SocketAddress?
            // A: we know that it can only be IPv4SocketAddress,
            //    since accept() requires bind() and our bind()
            //    only takes IPv4SocketAddress.
            
	    final int fd;
	    
	    if (checkRepeat(fd=Native.myInetAccept(getOuter().getFD(),
						   ipAddress,
						   port),
			    Native.getErrno())) {
		return false;
	    }
	    
	    if (fd>=0) {
                VM_Area prev=U.e(this);
                try {
                    newSocket_=new SignalSocketDescriptor(getOuter().tm,
							  getOuter().iosm,
							  fd);
                    newAddress_=new IPv4SocketAddress(ipAddress[0],
						      port[0]);
                } finally {
                    U.l(prev);
                }
	    }

	    return true;
        }
        
        public IODescriptor getNewDescriptor() {
            return newSocket_;
        }
        
        public SocketAddress getRemoteSocketAddress() {
            return newAddress_;
        }
    }

    public boolean tryAcceptNow(IODescriptor[] iod,
				SocketAddress[] sa) throws IOException {
	VM_Area prev=U.e(iod); /* can only hope that the areas of iod and
				  sa are the same */
	try {
	    int fd;

	    int[] ipAddress=new int[1];
	    int[] port=new int[1];
	    
	    if (IOException.System.checkRepeat(fd=Native.myInetAccept(getFD(),
								      ipAddress,
								      port),
					       Native.getErrno())) {
		return false;
	    }

	    iod[0]=new SignalSocketDescriptor(tm,iosm,fd);
	    sa[0]=new IPv4SocketAddress(ipAddress[0],port[0]);
	    return true;
	} finally {
	    U.l(prev);
	}
    }
    
    public synchronized AsyncHandle accept(AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return readQueue.performOp(new AcceptOpNode(this,cback));
        } finally {
            U.l(prev);
        }
    }
    
    public synchronized int sendto(SocketAddress address,
                                   AsyncMemoryCallback data,
                                   int maxbytes) 
	throws IOException {
	VM_Address buf = null;
	try {
	    final IPv4SocketAddress ta
		= (IPv4SocketAddress)address;
	    buf = data.getBuffer(maxbytes, false);
	    int errno
		= NativeConstants.EINTR;
	    int ret 
		= 0;
	    while (errno == NativeConstants.EINTR) {
		ret = Native.mysendto(getFD(),
				      buf,
				      maxbytes,
				      NativeConstants.MSG_DONTWAIT|
				      NativeConstants.MSG_NOSIGNAL,
				      ta.getIPv4Address(),
				      ta.getPort());
		if (ret == -1) 
		    errno = Native.getErrno();
		else
		    errno = 0;
	    }
	    return IOException.System.check
		(ret,
		 errno);
	}  catch (ClassCastException e) {
	    throw IOException.Unsupported.getInstance();
        } finally {
	    if (buf != null)
		data.doneBuffer(buf, 0);
        }
    }
    
    
    protected static class ConnectSignalCallback
        implements IOSignalManager.Callback,
		   AsyncHandle {
	
	private SignalSocketDescriptor outer_;
	protected void setOuter(SignalSocketDescriptor outer) throws PragmaNoBarriers {
	    this.outer_=outer;
	}
	protected SignalSocketDescriptor getOuter() throws PragmaNoBarriers {
	    return outer_;
	}
	
        private final AsyncCallback cback_;
	
        public ConnectSignalCallback(SignalSocketDescriptor outer,
				     AsyncCallback cback) {
            setOuter(outer);
	    this.cback_ = cback;
        }
	
	public boolean signal(boolean certain) {
	    if (!certain && !getOuter().readyForWrite()) {
		return true;
	    }
	    
	    getOuter().connected(cback_);
	    
	    return false;
	}
	
        public void removed(Object byWhat) {
            if (byWhat instanceof IOException) {
                cback_.ready(AsyncFinalizer.Error.make((IOException)byWhat));
            } /* else removed because signal() returned false */
        }

	public boolean canCancelQuickly() {
	    return true;
	}

	public void cancel(IOException error) {
	    getOuter().iosm.removeCallbackFromFD(getOuter().getFD(),
						 this,
						 error);
	}
    }
    
    private void connected(final AsyncCallback cback) {
        // big problem: we know that connect 'returned', but we
        // do not know why!  anotherwords, did it succeed, or
        // did it fail?  and if it failed, then why?  this seems
        // to be a particularly badly documented part of the 
        // socket API.  if you want a good discussion, go here:
        // http://cr.yp.to/docs/connection.html
        // let me try to outline the important points:
        // -> the error from a non-blocking connect call is
        //    stored in what is called an 'so_error', that is
        //    presumably part of the kernel's socket structure
        // -> getsockopt() with SO_ERROR will give you the
        //    value of 'so_error', but that's apparently not
        //    very portable.
        // -> the value of so_error will 'slip out' into any
        //    calls to read/write API calls.
        // even though getsockopt() is supposedly not portable,
        // I'll use it because it seems the most reasonable.  if
        // this ever becomes a problem, I would recommend using
        // error slippage on read().  simply call read here.  if
        // you get an error, then you know that connect() failed.
        // if you get data back, or if the error is EWOULDBLOCK
        // or something, then you know that you're connected.  if
        // you did get data back, then simply place it in a field
        // in this object, and override read(), recv(), and
        // whatever else to return that block of data if it's
        // present.  (that's ugly, I know, but it is pretty much
        // guaranteed to work.)
        int so_error=NativeHelper.getSoError(getFD());
        if (so_error==0) {
            cback.ready(AsyncFinalizer.Success.getInstance());
        } else {
            cback.ready(
                AsyncFinalizer.Error.make(
                    IOException.System.make(so_error)));
        }
    }

    public synchronized AsyncHandle connect(final SocketAddress address,
					    final AsyncCallback cback) {
        // what this does: the POSIX/BSD connect() is actually almost like
        // an async call.  what happens is that you make the socket non-blocking
        // (this is already the case, as all file descriptors that go
        // through signalableSocketDescriptor are made non-blocking)
        // and then you call connect().  if connect() decides that it would
        // have blocked, then it returned -1 with errno=EINPROGRESS, and
        // then you have to wait until the file descriptor (the socket)
        // is ready for writing (not reading or except, but writing).
        // once it becomes ready for writing, you know that it is either
        // connected or it experienced an error.
        VM_Area prev=U.e(cback);
        try {
            final IPv4SocketAddress ta=(IPv4SocketAddress)address;
            
            int result;
            int errno;
            
            for (;;) {
                result=Native.myInetConnect(
                    getFD(),
                    ta.getIPv4Address(),
                    ta.getPort());
                
                errno=Native.getErrno();
                
                if (result>=0) {
                    cback.ready(AsyncFinalizer.Success.getInstance());
                    return StallingUtil.asyncHandle;
                }
                
                if (errno==NativeConstants.EWOULDBLOCK ||
                    errno==NativeConstants.EAGAIN ||
                    errno==NativeConstants.EINTR) {
                    continue;
                }
                
                if (errno==NativeConstants.EINPROGRESS) {
                    break;
                }
                
                cback.ready(
                    AsyncFinalizer.Error.make(
                        IOException.System.make(errno)));
                return StallingUtil.asyncHandle;
            }

	    ConnectSignalCallback csc=
		new ConnectSignalCallback(this,cback);
            
            iosm.addCallbackForWrite(getFD(),csc);
	    
	    return csc;
        } catch (ClassCastException e) {
            cback.ready(
                AsyncFinalizer.Error.make(
                    IOException.Unsupported.getInstance()));
        } finally {
            U.l(prev);
        }

	return StallingUtil.asyncHandle;
    }
    
    public synchronized void setSoReuseAddr(boolean reuseAddr)
        throws IOException {
        IOException.System.check(
            Native.setSoReuseAddr(getFD(),reuseAddr),
            Native.getErrno());
    }
    
    public synchronized boolean getSoReuseAddr()
        throws IOException {
        return IOException.System.check(
            Native.getSoReuseAddr(getFD()),
            Native.getErrno())==1;
    }
    
    public synchronized void setSoKeepAlive(boolean keepAlive)
        throws IOException {
        IOException.System.check(
            Native.setSoReuseAddr(getFD(),keepAlive),
            Native.getErrno());
    }
    
    public synchronized boolean getSoKeepAlive()
        throws IOException {
        return IOException.System.check(
            Native.getSoKeepAlive(getFD()),
            Native.getErrno())==1;
    }
    
    public synchronized void setSoLinger(Linger l)
        throws IOException {
        IOException.System.check(
            Native.setSoLinger(getFD(),l.onoff,l.linger),
            Native.getErrno());
    }
    
    public synchronized void getSoLinger(Linger l)
        throws IOException {
        boolean[] onoff=new boolean[1];
        int[] linger=new int[1];
        IOException.System.check(
            Native.getSoLinger(getFD(),onoff,linger),
            Native.getErrno());
        l.onoff=onoff[0];
        l.linger=linger[0];
    }
    
    public synchronized void setSoOOBInline(boolean oobInline)
        throws IOException {
        IOException.System.check(
            Native.setSoOOBInline(getFD(),oobInline),
            Native.getErrno());
    }
    
    public synchronized boolean getSoOOBInline()
        throws IOException {
        return IOException.System.check(
            Native.getSoOOBInline(getFD()),
            Native.getErrno())==1;
    }
    
    public synchronized void setTcpNoDelay(boolean noDelay)
        throws IOException {
        IOException.System.check(
            Native.setTcpNoDelay(getFD(),noDelay),
            Native.getErrno());
    }
    
    public synchronized boolean getTcpNoDelay()
        throws IOException {
        return IOException.System.check(
            Native.getTcpNoDelay(getFD()),
            Native.getErrno())==1;
    }
    
    protected IODescriptor createMyselfWithFD(int newFd) {
        return new SignalSocketDescriptor(tm,iosm,newFd);
    }
    
    public static class SpecificWrapifier extends SignalRWDescriptorBase.SpecificWrapifier {
        public SpecificWrapifier(IOSignalManager iosm) {
            super(iosm);
        }
        
        public IODescriptor wrap(FileDescriptorWrapifier.FDType type,
                                 int fd) {
            return new SignalSocketDescriptor(tm,iosm,fd);
        }
    }
}

