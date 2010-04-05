package gnu.java.net;

import java.io.*;
import java.net.*;
import org.ovmj.java.*;

/**
 * Native methods for gnu.java.net.
 *
 * @see java.io.LibraryGlue
 * @author Christian Grothoff
 * @author Filip Pizlo
 */
class LibraryGlue
    implements NativeConstants {

  static native int get_specific_error_string(int errno,
					      byte[] buf,
					      int len);

    /**
     * Poll value of errno and build the appropriate error message.
     */
    private static String getErrorMessage() {	
	byte[] buf = new byte[128];
	int len = get_specific_error_string(LibraryImports.getErrno(),
					    buf, buf.length);
	return new String(buf, 0, len);
    }

    /* ************************ native methods for PlainSocketImpl ************* */

    /**
    * Sets the specified option on a socket to the passed in object.  For
    * options that take an integer argument, the passed in object is an
    * Integer.  The option_id parameter is one of the defined constants in
    * this interface.
    *
    * @param option_id The identifier of the option
    * @param val The value to set the option to
    *
    * @exception SocketException If an error occurs
    */
    static void setOption(PlainSocketImpl _,int optID, Object value) throws SocketException {
        int res=0;
        switch (optID) {
            case SocketOptions.SO_REUSEADDR:
                res=LibraryImports.setSoReuseAddr(_.native_fd,
                                                  ((Boolean)value).booleanValue());
                break;
            case SocketOptions.SO_KEEPALIVE:
                res=LibraryImports.setSoKeepAlive(_.native_fd,
                                                  ((Boolean)value).booleanValue());
                break;
            case SocketOptions.SO_LINGER:
                LibraryImports.linger l;
                if (value instanceof Boolean) {
                    l=new LibraryImports.linger(((Boolean)value).booleanValue(),
                                                0);
                } else {
                    l=new LibraryImports.linger(true,
                                                ((Integer)value).intValue());
                }
                res=LibraryImports.setSoLinger(_.native_fd,l);
                break;
            case SocketOptions.SO_TIMEOUT:
                res=LibraryImports.setSoTimeout(_.native_fd,
                                                ((Integer)value).intValue());
                break;
            case SocketOptions.SO_OOBINLINE:
                res=LibraryImports.setSoOOBInline(_.native_fd,
                                                  ((Boolean)value).booleanValue());
                break;
            case SocketOptions.TCP_NODELAY:
                res=LibraryImports.setTcpNoDelay(_.native_fd,
                                                 ((Boolean)value).booleanValue());
                break;
            default:
                throw new SocketException("setOption(): optID = "+optID+" not supported");
        }
        if (res<0) {
            throw new SocketException(getErrorMessage());
        }
    }

    private static Boolean wrapBool(int val) throws SocketException {
        if (val<0) {
            throw new SocketException(getErrorMessage());
        }
        return new Boolean(val==1);
    }

    private static Integer wrapInt(int val) throws SocketException {
        if (val<0) {
            throw new SocketException(getErrorMessage());
        }
        return new Integer(val);
    }

    /**
    * Returns the current setting of the specified option.  The Object returned
    * will be an Integer for options that have integer values.  The option_id
    * is one of the defined constants in this interface.
    *
    * @param option_id The option identifier
    *
    * @return The current value of the option
    *
    * @exception SocketException If an error occurs
    */
    static Object getOption(PlainSocketImpl _,int optID) throws SocketException {
        switch (optID) {
            case SocketOptions.SO_REUSEADDR:
                return wrapBool(LibraryImports.getSoReuseAddr(_.native_fd));
            case SocketOptions.SO_KEEPALIVE:
                return wrapBool(LibraryImports.getSoKeepAlive(_.native_fd));
            case SocketOptions.SO_LINGER:
                LibraryImports.linger l=new LibraryImports.linger();
                if (LibraryImports.getSoLinger(_.native_fd,l)<0) {
                    throw new SocketException(getErrorMessage());
                }
                if (l.onoff) {
                    return new Integer(l.linger);
                } else {
                    return new Boolean(false);
                }
            case SocketOptions.SO_TIMEOUT:
                return wrapInt(LibraryImports.getSoTimeout(_.native_fd));
            case SocketOptions.SO_OOBINLINE:
                return wrapBool(LibraryImports.getSoOOBInline(_.native_fd));
            case SocketOptions.TCP_NODELAY:
                return wrapBool(LibraryImports.getTcpNoDelay(_.native_fd));
	    case SocketOptions.SO_BINDADDR:
		LibraryImports.sockaddr_in sa=
		    new LibraryImports.sockaddr_in();
		if (LibraryImports.getsockname(_.native_fd,sa)!=0) {
		    throw new SocketException(getErrorMessage());
		}
		StringBuffer buf=new StringBuffer();
		byte[] addr=sa.getAddr();
		for (int i=0;i<addr.length;++i) {
		    if (i!=0) {
			buf.append('.');
		    }
		    buf.append(addr[i]&0xff);
		}
		try {
		    return InetAddress.getByName(buf.toString());
		} catch (UnknownHostException e) {
		    throw new SocketException(e.toString());
		}
            default:
                throw new SocketException("getOption(): optID = "+optID+" not supported");
        }
    }

  /**
   * Creates a new socket that is not bound to any local address/port and
   * is not connected to any remote address/port.  This will be created as
   * a stream socket if the stream parameter is true, or a datagram socket
   * if the stream parameter is false.
   *
   * @param stream true for a stream socket, false for a datagram socket
   */
  static void create(PlainSocketImpl self)
      throws IOException {
      int sock = LibraryImports.socket(PF_INET, /* how about INET6??? */
				       SOCK_STREAM,
				       6); // TCP:UDP
      // classpath's C code also does:
      // fcntl(sock,F_SETFD,FD_CLOEXEC); 
      // Filip, can / do we need to do this with your IO code???
      
      // interesting.  strictly speaking, we don't need it, since we're not really
      // doing any forking and executing yet.  But I can figure out ways of implementing
      // this.
      
      if (sock == -1)
	  throw new IOException(getErrorMessage());
      self.native_fd = sock;
  }

  /**
   * Creates a new socket that is not bound to any local address/port and
   * is not connected to any remote address/port.  This will be always
   * created as a datagram socket.
   */
  static void create(PlainDatagramSocketImpl _)
      throws IOException {
      int sock = LibraryImports.socket(PF_INET, /* how about INET6??? */
				       SOCK_DGRAM,
				       17); // UDP
      // classpath's C code also does:
      // fcntl(sock,F_SETFD,FD_CLOEXEC); 
      // Filip, can / do we need to do this with your IO code???
      
      // interesting.  strictly speaking, we don't need it, since
      // we're not really doing any forking and executing yet.  But I
      // can figure out ways of implementing this.
      
      if (sock == -1)
	  throw new IOException(getErrorMessage());
      _.native_fd = sock;
  }


  /**
   * Connects to the remote address and port specified as arguments.
   *
   * @param addr The remote address to connect to
   * @param port The remote port to connect to
   *
   * @exception IOException If an error occurs
   */
  static void connect(PlainSocketImpl _,
		      InetAddress addr, 
		      int port)
      throws IOException {
      LibraryImports.sockaddr_in sa 
	  = new LibraryImports.sockaddr_in(addr, port);
      if (0 != LibraryImports.connect(_.native_fd, sa))
	  throw new SocketException(getErrorMessage());
      
      // should memoize these.
      Opaque fdField        = LibraryImports.hackFieldForName(_,"fd");
      Opaque addressField   = LibraryImports.hackFieldForName(_,"address");
      Opaque localportField = LibraryImports.hackFieldForName(_,"localport");
      Opaque portField      = LibraryImports.hackFieldForName(_,"port");
      
      // why the hell fill in an invalid file descriptor?  'cause that's
      // what classpath does!
      LibraryImports.fieldSetReference(fdField,_,new FileDescriptor());
      LibraryImports.fieldSetReference(addressField,_,addr);
      
      if (LibraryImports.getsockname(_.native_fd,sa)!=0) {
          throw new SocketException(getErrorMessage());
      }
      LibraryImports.fieldSetInt(localportField,_,sa.port);
      
      if (LibraryImports.getpeername(_.native_fd,sa)!=0) {
          throw new SocketException(getErrorMessage());
      }
      LibraryImports.fieldSetInt(portField,_,sa.port);
  }

  /**
   * Binds to the specified port on the specified addr.  Note that this addr
   * must represent a local IP address.  **** How bind to INADDR_ANY? ****
   *
   * @param addr The address to bind to
   * @param port The port number to bind to
   *
   * @exception IOException If an error occurs
   */
  static void bind(PlainSocketImpl _,
		   InetAddress addr, 
		   int port)
      throws IOException {
      LibraryImports.sockaddr_in sa 
	  = new LibraryImports.sockaddr_in(addr, port);
      if (0 != LibraryImports.bind(_.native_fd, sa))
	  throw new IOException(getErrorMessage());
      
      if (0 != LibraryImports.setSoReuseAddr(_.native_fd, true))
	  throw new IOException(getErrorMessage());
      
      Opaque localportField = LibraryImports.hackFieldForName(_,"localport");
      if (LibraryImports.getsockname(_.native_fd,sa)!=0) {
          throw new SocketException(getErrorMessage());
      }
      LibraryImports.fieldSetInt(localportField,_,sa.port);
  }

   /**
   * Binds this socket to a particular port and interface
   *
   * @param port The port to bind to
   * @param addr The address to bind to
   *
   * @exception SocketException If an error occurs
   */
    static void bind(PlainDatagramSocketImpl _,
		     int port, InetAddress addr) 
	throws SocketException {
	LibraryImports.sockaddr_in sa 
	    = new LibraryImports.sockaddr_in(addr, port);
	if (0 != LibraryImports.bind(_.native_fd, sa))
	    throw new SocketException(getErrorMessage());

    }

 

  /**
   * Starts listening for connections on a socket. The queuelen parameter
   * is how many pending connections will queue up waiting to be serviced
   * before being accept'ed.  If the queue of pending requests exceeds this
   * number, additional connections will be refused.
   *
   * @param queuelen The length of the pending connection queue
   * 
   * @exception IOException If an error occurs
   */
  static void listen(PlainSocketImpl _,
		     int queuelen)
      throws IOException {
      int ret = LibraryImports.listen(_.native_fd, 
				      queuelen);
      if (ret != 0)
	  throw new IOException(getErrorMessage());
  }

  /**
   * Accepts a new connection on this socket and returns in in the 
   * passed in SocketImpl.
   *
   * @param impl The SocketImpl object to accept this connection.
   */
  static void accept(PlainSocketImpl _,
		     SocketImpl impl)
      throws IOException {
      LibraryImports.sockaddr_in so
	  = new LibraryImports.sockaddr_in();
      int sock 
	  = LibraryImports.accept(_.native_fd, 
				  so,
				  true);
      if (sock == -1)
	  throw new IOException(getErrorMessage());
      PlainSocketImpl pi = (PlainSocketImpl) impl;
      pi.native_fd = sock;
      java.net.LibraryGlue.setAddress(pi,
				      so.port,
				      so.getAddr());
  }

  /**
   * Returns the number of bytes that the caller can read from this socket
   * without blocking. 
   *
   * @return The number of readable bytes before blocking
   *
   * @exception IOException If an error occurs
   */
  static int available(PlainSocketImpl _) throws IOException {
      return LibraryImports.getAvailable(_.native_fd);
  }

  /**
   * Closes the socket.  This will cause any InputStream or OutputStream
   * objects for this Socket to be closed as well.
   * <p>
   * Note that if the SO_LINGER option is set on this socket, then the
   * operation could block.
   *
   * @exception IOException If an error occurs
   */
  static void close (PlainSocketImpl _) throws IOException {
      int ret = LibraryImports.close(_.native_fd);
      _.native_fd = -1;
      if (ret != 0)
	  throw new IOException(getErrorMessage());
  }

    /**
   * Closes the socket
   */
    static void close(PlainDatagramSocketImpl _) {
	int ret = LibraryImports.close(_.native_fd);
	_.native_fd = -1;
	// no exception according to classpath...
    }



  /**
   * <b>NOTE: this javadoc comment is totally wrong.  That is not what
   *    the JNI method actually does!  And as such, that is not what
   *    this here method does.  But I will keep this silly javadoc
   *    thingy around for humor and amusement.</b>
   *
   * Internal method used by SocketInputStream for reading data from
   * the connection.  Reads up to len bytes of data into the buffer
   * buf starting at offset bytes into the buffer.
   *
   * @return The actual number of bytes read or -1 if end of stream.
   *
   * @exception IOException If an error occurs
   */
  static int read(PlainSocketImpl _,byte[] buf, int offset, int len)
      throws IOException {
      int ret = LibraryImports.read(_.native_fd, 
				    buf, offset, len,
				    true);
      if (ret >= 0)
	  return ret;
      throw new IOException(getErrorMessage());
  }

  /**
   * Internal method used by SocketOuputStream for writing data to
   * the connection.  Writes ALL len bytes of data from the buffer
   * buf starting at offset bytes into the buffer.
   *
   * @exception IOException If an error occurs
   */
  static void write(PlainSocketImpl _,
		    byte[] buf, int offset, int len)
      throws IOException {
      int pos = 0;
      while (pos < len) {
	  int ret = LibraryImports.write(_.native_fd, 
					 buf, offset+pos,
					 len-pos,
					 true);
	  if (ret == -1)
	      throw new IOException(getErrorMessage());
	  pos += ret;
      }
  }



 
  /**
   * Sends a packet of data to a remote host
   *
   * @param addr The address to send to
   * @param port The port to send to
   * @param buf The buffer to send
   * @param offset The offset of the data in the buffer to send
   * @param len The length of the data to send
   *
   * @exception IOException If an error occurs
   */
    static void sendto (PlainDatagramSocketImpl _,
			InetAddress addr, int port,
			byte[] buf, int offset, int len)
	throws IOException {
	LibraryImports.sockaddr_in ad
	    = new LibraryImports.sockaddr_in(addr,
					     port);
	if (0 != LibraryImports.sendto(ad,
				       buf,
				       offset,
				       len))
	    throw new IOException(getErrorMessage());
    }



  /**
   * Receives a UDP packet from the network
   *
   * @param packet The packet to fill in with the data received
   *
   * @exception IOException IOException If an error occurs
   */
    static void receive0(PlainDatagramSocketImpl _,
			 DatagramPacket packet)
    throws IOException {
	LibraryImports.sockaddr_in sa
	    = new LibraryImports.sockaddr_in();
	int ret
	    = LibraryImports.receive(_.native_fd,
				     sa,
				     packet.getData(),
				     packet.getData().length,
				     true);
	if (ret == -1)
	    throw new IOException(getErrorMessage());
	try {
	    packet.setLength(ret);
	    packet.setPort(sa.port); 
	    packet.setAddress(InetAddress.getByAddress(sa.getAddr()));
	} catch (UnknownHostException uhe) {
	    throw new IOException(uhe.toString());
	}
    }



  /**
   * Sets the value of an option on the socket
   *
   * @param option_id The identifier of the option to set
   * @param val The value of the option to set
   *
   * @exception SocketException If an error occurs
   */
    static void setOption(PlainDatagramSocketImpl _,
			  int option_id, Object val)
	throws SocketException {
       int res=0;
         switch (option_id) {
       case SocketOptions.SO_REUSEADDR:
           res=LibraryImports.setSoReuseAddr(_.native_fd,
                                             ((Boolean)val).booleanValue());
           break;
       case SocketOptions.SO_TIMEOUT:
           res=LibraryImports.setSoTimeout(_.native_fd,
                                           ((Integer)val).intValue());
           break;
           /*
       case IP_TTL:
           res = LibraryImports.setIPTTL(_.native_fd,
                                         ((Integer)val).intValue());
           break;
           */
       default:
           throw new SocketException("setOption(): optID = "+option_id+" not supported");
         }
         if (res<0) {
             throw new SocketException(getErrorMessage());
         }
    }

  /**
   * Retrieves the value of an option on the socket
   *
   * @param option_id The identifier of the option to retrieve
   *
   * @return The value of the option
   *
   * @exception SocketException If an error occurs
   */
    static Object getOption(PlainDatagramSocketImpl _,
			    int option_id)
	throws SocketException {
	switch (option_id) {
	    /*
	      case IP_TTL:
	      return wrapInt(LibraryImports.getIPTTL(_.native_fd));
	    */
	case SocketOptions.SO_REUSEADDR:
	    return wrapBool(LibraryImports.getSoReuseAddr(_.native_fd));
	case SocketOptions.SO_TIMEOUT:
	    return wrapInt(LibraryImports.getSoTimeout(_.native_fd));
	default:
	    throw new SocketException("getOption(): optID = "+option_id+" not supported");
	}	
    }


  /**
   * Joins a multicast group
   *
   * @param addr The group to join
   *
   * @exception IOException If an error occurs
   */
    static void join(PlainDatagramSocketImpl _,
		     InetAddress addr) throws IOException {
	throw new Error("Not supported yet!");
    }

  /**
   * Leaves a multicast group
   *
   * @param addr The group to leave
   *
   * @exception IOException If an error occurs
   */
    static void leave(InetAddress addr) throws IOException {
	throw new Error("not supported yet!");
    }


 


}
