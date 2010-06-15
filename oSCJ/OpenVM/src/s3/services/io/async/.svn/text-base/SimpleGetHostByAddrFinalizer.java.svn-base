// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/SimpleGetHostByAddrFinalizer.java,v 1.1 2004/04/08 18:39:59 pizlofj Exp $

package s3.services.io.async;

import ovm.services.io.async.*;

/**
 *
 * @author Filip Pizlo
 */
class SimpleGetHostByAddrFinalizer
    extends AsyncFinalizer.Success
    implements HostLookupManager.GetHostByAddrFinalizer {
    
    private byte[] hostname_;
    
    public SimpleGetHostByAddrFinalizer(byte[] hostname) {
        this.hostname_=hostname;
    }
    
    public byte[] getHostname() {
        return hostname_;
    }
}

