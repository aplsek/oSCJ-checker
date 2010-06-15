// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/FileManagerImpl.java,v 1.9 2004/10/09 21:43:04 pizlofj Exp $

package s3.services.io.async;

import ovm.core.execution.Native;
import ovm.services.io.async.*;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;

/**
 *
 * @author Filip Pizlo
 */
public class FileManagerImpl
    extends AsyncIOManagerBase
    implements FileManager {
    
    private static final FileManagerImpl instance_ = new FileManagerImpl();
    public static FileManager getInstance() {
        return instance_;
    }
    
    public AsyncHandle open(byte[] filename,
			    int flags,
			    int mode,
			    AsyncCallback cback) {
        int fd;
        if (IOException.System.check(
                fd=Native.open(filename,
			       flags,
			       mode),
                Native.getErrno(),
                cback)) {
            wrapifier.wrap(fd,cback);
        }
	return StallingUtil.asyncHandle;
    }
    
    public AsyncHandle mkstemp(byte[] byteTemplate,
			       final AsyncCallback cback) {
        int fd;
        
        fd = Native.mkstemp(byteTemplate);
        MemoryManager.the().assertSingleReplica(VM_Address.fromObject(byteTemplate));
        
        if (IOException.System.check(
                fd,
                Native.getErrno(),
                cback)) {
	    wrapifier.wrap(fd,cback);
        }
	return StallingUtil.asyncHandle;
    }
}

