// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/IPv4SocketAddress.java,v 1.1 2004/04/08 18:39:58 pizlofj Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public class IPv4SocketAddress
    extends IPv4Address
    implements InetSocketAddress {
    
    // this is in host ordering.
    private int port_;
    
    /**
     * @param ipAddress the IPv4 address using host ordering.  it is really
     *                  a 32-bit unsigned integer.
     * @param port the port using host ordering.  it is really a 16-bit
     *             unsigned integer.
     */
    public IPv4SocketAddress(int ipAddress,
                             int port) {
        super(ipAddress);
        this.port_ = port;
    }
    
    /**
     * @param ipAddress the IPv4 address represented as an object
     * @param port the port using host ordering.  it is really a 16-bit
     *             unsigned integer.
     */
    public IPv4SocketAddress(IPv4Address ipAddress,
                             int port) {
        this(ipAddress.getIPv4Address(),port);
    }
    
    public IPv4SocketAddress(byte[] ipAddress,
                             int port) {
        super(ipAddress);
        this.port_ = port;
    }
    
    public int getPort() {
        return port_;
    }
    
    public String toString() {
        return super.toString()+":"+port_;
    }
}

