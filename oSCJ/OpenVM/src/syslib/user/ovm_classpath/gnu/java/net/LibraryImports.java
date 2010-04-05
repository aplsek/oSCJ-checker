package gnu.java.net;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketImpl;
import org.ovmj.java.NativeConstants;
import org.ovmj.java.Opaque;

class LibraryImports {
    static native int getErrno();
    static native int socket(int domain, int type, int protocol);
    static native int read(int fd, 
			   byte[] buf, 
			   int byteOffset, 
			   int byteCount,
			   boolean blocking);
    static native int write(int fd,
			    byte[] buf,
			    int byteOffset,
			    int byteCount,
			    boolean blocking);
    static native int close(int fd);
    static native int bind(int fd, sockaddr_in addr);
    static native int connect(int fd, sockaddr_in sockaddr); 
    static native int listen(int fd, int queuelen);
    static native int accept(int fd, 
			     sockaddr_in sockaddr,
			     boolean blocking); 

    // static native int setOption(int fd, int optId, byte[] value); ??
    // static native int getOption(int fd, int optId, byte[] value); ??

    static native int receive(int fd,
			      sockaddr_in saddr,
			      byte[] buf,
			      int len,
			      boolean blocking);
    static native int sendto(sockaddr_in dst,
			     byte[] buf,
			     int off,
			     int len);


    static native int getAvailable(int fd); 

    static native int setSoReuseAddr(int sock,boolean reuseAddr);

    static native int getSoReuseAddr(int sock);
    
    static native int setSoKeepAlive(int sock,boolean keepAlive);
    static native int getSoKeepAlive(int sock);
    
    static native int setSoLinger(int sock,linger l);
    static native int getSoLinger(int sock,linger l);
    
    static native int setSoTimeout(int sock,int timeout);
    static native int getSoTimeout(int sock);
    
    static native int setSoOOBInline(int sock,boolean oobInline);
    static native int getSoOOBInline(int sock);
    
    static native int setTcpNoDelay(int sock,boolean noDelay);
    static native int getTcpNoDelay(int sock);
    
    static native int getsockname(int sock,sockaddr_in addr);
    static native int getpeername(int sock,sockaddr_in addr);
    
    static native Opaque hackFieldForName(Object object,String name);
    static native void fieldSetInt(Opaque field,Object object,int value);
    static native void fieldSetReference(Opaque field,Object object,Object value);
    
    static class linger {
        boolean onoff;
        int linger;
        
        public linger() {}
        public linger(boolean onoff,
                      int linger) {
            this.onoff=onoff;
            this.linger=linger;
        }
    }
    
    static class sockaddr_in {
	int family;
        int addr;
        int port;
	sockaddr_in() { }
	sockaddr_in(InetAddress add,
		    int port) {
	    byte[] ad = add.getAddress();
	    int address = ad [3] & 0xff;
	    address |= ((ad [2] << 8) & 0xff00);
	    address |= ((ad [1] << 16) & 0xff0000);
	    address |= ((ad [0] << 24) & 0xff000000);
	    this.family = NativeConstants.AF_INET;
	    this.addr = address;
	    this.port = port;	    
	}
	byte[] getAddr() {
	    byte[] ret = new byte[4];
	    ret[3] = (byte) (addr & 0xff);
	    ret[2] = (byte) ((addr & 0xff00) >> 8);
	    ret[1] = (byte) ((addr & 0xff0000) >> 16);
	    ret[0] = (byte) ((addr & 0xff000000) >> 24);
	    return ret;
	}
    }

}
