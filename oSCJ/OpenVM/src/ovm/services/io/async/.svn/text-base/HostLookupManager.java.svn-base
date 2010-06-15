// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/HostLookupManager.java,v 1.2 2004/10/09 21:43:04 pizlofj Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public interface HostLookupManager extends ovm.services.ServiceInstance {
    
    public AsyncHandle getHostByName(byte[] name,
				     AsyncCallback cback);
    
    public AsyncHandle getHostByAddr(InetAddress addr,
				     AsyncCallback cback);
    
    public static interface GetHostByNameFinalizer extends AsyncFinalizer {
        public InetAddress[] getAddresses();
    }
    
    public static interface GetHostByAddrFinalizer extends AsyncFinalizer {
        public byte[] getHostname();
    }
}

