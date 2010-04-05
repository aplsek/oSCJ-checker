// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/StallingHostLookupManager.java,v 1.3 2004/10/09 21:43:05 pizlofj Exp $

package s3.services.io.async;

import ovm.core.services.memory.*;
import ovm.services.io.async.*;

/**
 *
 * @author Filip Pizlo
 */
public class StallingHostLookupManager
    extends AsyncIOManagerBase
    implements HostLookupManager {
    
    public AsyncHandle getHostByName(byte[] name,
				     AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            try {
                cback.ready(new SimpleGetHostByNameFinalizer(
                    HostLookupUtil.getHostByNameNow(name)));
            } catch (IOException e) {
                cback.ready(AsyncFinalizer.Error.make(e));
            }
        } finally {
            U.l(prev);
        }
	return StallingUtil.asyncHandle;
    }
    
    public AsyncHandle getHostByAddr(InetAddress ip,
				     AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            try {
                cback.ready(new SimpleGetHostByAddrFinalizer(
                    HostLookupUtil.getHostByAddrNow(ip)));
            } catch (IOException e) {
                cback.ready(AsyncFinalizer.Error.make(e));
            }
        } finally {
            U.l(prev);
        }
	return StallingUtil.asyncHandle;
    }
}

