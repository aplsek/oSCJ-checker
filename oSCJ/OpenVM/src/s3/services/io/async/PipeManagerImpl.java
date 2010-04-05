// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/PipeManagerImpl.java,v 1.10 2004/05/28 20:24:55 jv Exp $

package s3.services.io.async;

import ovm.core.execution.Native;
import ovm.services.io.async.IODescriptor;
import ovm.services.io.async.IOException;
import ovm.services.io.async.PipeManager;

/**
 *
 * @author Filip Pizlo
 */
public class PipeManagerImpl
    extends AsyncIOManagerBase
    implements PipeManager {
    
    private static PipeManagerImpl instance_ = new PipeManagerImpl();
    public static PipeManager getInstance() {
        return instance_;
    }
    
    public void pipe(IODescriptor[] pipe) throws IOException {
        int[] sysPipe=new int[2];
        
	IOException.System.check(Native.myPipe(sysPipe),
				 Native.getErrno());
        
        // we use wrapNow() because we know that the file descriptors are either
        // pipes or sockets.  that's right, even though we called the pipe
        // syscall, these may be sockets!  I think that the BSDs (Mac OS X in
        // particular) are guilty of eliminating pipes in favour of socketpairs.
        pipe[0]=wrapifier.wrapNow(sysPipe[0]);
        pipe[1]=wrapifier.wrapNow(sysPipe[1]);
    }
    
}

