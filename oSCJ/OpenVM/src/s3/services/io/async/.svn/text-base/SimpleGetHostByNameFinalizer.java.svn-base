// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/SimpleGetHostByNameFinalizer.java,v 1.1 2004/04/08 18:39:59 pizlofj Exp $

package s3.services.io.async;

import ovm.services.io.async.*;

/**
 *
 * @author Filip Pizlo
 */
class SimpleGetHostByNameFinalizer
    extends AsyncFinalizer.Success
    implements HostLookupManager.GetHostByNameFinalizer {
    
    private InetAddress[] result_;
    
    public SimpleGetHostByNameFinalizer(InetAddress[] result) {
        this.result_=result;
    }
    
    public InetAddress[] getAddresses() {
        return result_;
    }
}

