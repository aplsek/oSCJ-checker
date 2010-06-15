// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/blocking/BaselineBlockingManagerImpl.java,v 1.2 2004/02/20 08:52:45 jthomas Exp $

package s3.services.io.blocking;

import ovm.services.io.blocking.*;
import ovm.core.services.threads.OVMThread;

/**
 *
 * @author Filip Pizlo
 */
public class BaselineBlockingManagerImpl
    extends ovm.services.ServiceInstanceImpl
    implements BlockingManager {
    
    private static final BlockingManager instance_
        = new BaselineBlockingManagerImpl();
    public static BlockingManager getInstance() {
        return instance_;
    }
    
    public void init() {
        isInited=true;
    }
    
    public int getPriority(OVMThread t) {
        return 0;
    }
    
    public void notifyBlock(OVMThread t) {
    }
    
    public void notifyUnblock(OVMThread t) {
    }
    
}

