// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/SocketManagerImpl.java,v 1.11 2004/05/28 20:24:55 jv Exp $

package s3.services.io.async;

import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;
import ovm.core.execution.NativeInterface;
import ovm.services.io.async.IODescriptor;
import ovm.services.io.async.IOException;
import ovm.services.io.async.SocketIODescriptor;
import ovm.services.io.async.SocketManager;

/**
 *
 * @author Filip Pizlo
 */
public class SocketManagerImpl
    extends AsyncIOManagerBase
    implements SocketManager {
    
    private static final class NativeHelper implements NativeInterface {
        public static final native int socket(int domain,int type,int protocol);
    }
    
    private static SocketManagerImpl instance_ = new SocketManagerImpl();
    public static SocketManager getInstance() {
        return instance_;
    }
    
    public SocketIODescriptor socket(int domain,
                                     int type,
                                     int protocol)
        throws IOException {
        
        if (domain != NativeConstants.AF_INET ||
            (type  != NativeConstants.SOCK_STREAM &&
             type  != NativeConstants.SOCK_DGRAM)) {
            throw IOException.Unsupported.getInstance();
        }
        
        int fd;
        
        IOException.System.check(
            fd=NativeHelper.socket(domain,
                                   type,
                                   protocol),
            Native.getErrno());
        
        // we call wrapNow() because we know that the fd is a socket, and we
        // simply want to leverage the wrapifier's knowledge about what
        // IODescriptor implementation to use.
        return (SocketIODescriptor)wrapifier.wrapNow(fd);
    }

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
        throws IOException {
        
        if (domain   != NativeConstants.AF_UNIX ||
            type     != NativeConstants.SOCK_STREAM ||
            protocol != 0) {
            throw IOException.Unsupported.getInstance();
        }
        
        int[] sysSv=new int[2];
        
	IOException.System.check(Native.mySocketpair(sysSv),
                                 Native.getErrno());
        
        // we call wrapNow() because we know that the fd is a socket, and we
        // simply want to leverage the wrapifier's knowledge about what
        // IODescriptor implementation to use.
        sv[0]=wrapifier.wrapNow(sysSv[0]);
        sv[1]=wrapifier.wrapNow(sysSv[1]);
    }
    
}

