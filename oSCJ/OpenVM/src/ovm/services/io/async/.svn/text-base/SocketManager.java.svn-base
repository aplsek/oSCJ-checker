// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/SocketManager.java,v 1.3 2004/04/05 17:49:02 pizlofj Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public interface SocketManager extends ovm.services.ServiceInstance {
    
    public SocketIODescriptor socket(int domain,
                                     int type,
                                     int protocol)
        throws IOException;

    /**
     * @param sv an array of two.  upon success, the two elements will be
     *           set to a pair of sockets.  These sockets will be
     *           indistinguishable.  Any data written on one socket will
     *           become available on the reading end of the other.
     */
    public void socketpair(int domain,
                           int type,
                           int protocol,
                           IODescriptor[] sv)
        throws IOException;
    
}

