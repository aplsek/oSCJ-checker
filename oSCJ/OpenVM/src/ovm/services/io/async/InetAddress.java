// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/InetAddress.java,v 1.1 2004/04/08 18:39:58 pizlofj Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public interface InetAddress {
    /**
     * @return the IP address in network ordering.
     */
    public byte[] getIPAddress();
    
    /**
     * @return the address family
     */
    public int getAddressFamily();
}

