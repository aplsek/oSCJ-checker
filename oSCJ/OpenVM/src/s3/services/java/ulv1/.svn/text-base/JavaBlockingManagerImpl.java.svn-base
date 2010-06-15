package s3.services.java.ulv1;

import ovm.core.services.threads.OVMThread;
import ovm.services.io.blocking.BlockingManager;
import ovm.services.java.JavaOVMThread;

/**
 *
 * @author Filip Pizlo
 */
public class JavaBlockingManagerImpl
    extends ovm.services.ServiceInstanceImpl
    implements ovm.services.io.blocking.BlockingManager {
    
    private static final BlockingManager instance_ = new JavaBlockingManagerImpl();

    public static BlockingManager getInstance() {
        return instance_;
    }
    
    public void init() {
        isInited=true;
    }
    
    public int getPriority(OVMThread t) {
        return ((JavaOVMThread)t).getPriority();
    }
    
    public void notifyBlock(OVMThread t) {
        ((JavaOVMThread)t).setState(JavaOVMThread.BLOCKED_IO);
    }
    
    public void notifyUnblock(OVMThread t) {
        ((JavaOVMThread)t).setState(JavaOVMThread.READY);
    }   
}