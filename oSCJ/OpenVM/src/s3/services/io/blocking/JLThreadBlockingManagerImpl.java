// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/blocking/JLThreadBlockingManagerImpl.java,v 1.2 2004/02/20 08:52:45 jthomas Exp $

package s3.services.io.blocking;

import ovm.services.io.blocking.*;
import ovm.core.services.threads.OVMThread;
import s3.services.threads.JLThread;

/**
 *
 * @author Filip Pizlo
 */
public class JLThreadBlockingManagerImpl
    extends ovm.services.ServiceInstanceImpl
    implements BlockingManager {
    
    private static final BlockingManager instance_
        = new JLThreadBlockingManagerImpl();
    public static BlockingManager getInstance() {
        return instance_;
    }
    
    public void init() {
        isInited=true;
    }
    
    public int getPriority(OVMThread t) {
        return ((JLThread)t).getPriority();
    }
    
    public void notifyBlock(OVMThread t) {
        ((JLThread)t).setBlockedIO(true);
    }
    
    public void notifyUnblock(OVMThread t) {
        ((JLThread)t).setBlockedIO(false);
    }
    
}

