// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/FileDescriptorWrapifierImpl.java,v 1.7 2004/04/08 18:39:59 pizlofj Exp $

package s3.services.io.async;

import ovm.core.execution.*;
import ovm.core.services.memory.*;
import ovm.services.io.async.*;

/**
 * 
 * @author Filip Pizlo
 */
public class FileDescriptorWrapifierImpl
    extends ovm.services.ServiceInstanceImpl
    implements FileDescriptorWrapifier {
    
    private static final class NativeHelper implements NativeInterface {
        public static final native int getFdMode(int fd);
    }
    
    private SpecificWrapifier socketWrapifier = null;
    private SpecificWrapifier fileWrapifier   = null;
    private SpecificWrapifier otherWrapifier  = null;
    
    /** feel free to override, but please do call super.init() after your own
     * initialization!! */
    public void init() {
        isInited=true;
    }
    
    protected IODescriptor wrapGivenMode(int fd,
                                         int mode) {
        if ((mode & NativeConstants.S_IFSOCK)==NativeConstants.S_IFSOCK) {
	    //ovm.core.OVMBase.d("wrapping as socket");
            return wrapAsSocket(fd);
        }
        
        // is this heuristic correct?
        if ((mode & NativeConstants.S_IFREG)!=0) {
	    //ovm.core.OVMBase.d("wrapping as file");
            return wrapAsFile(fd);
        }
        
        //ovm.core.OVMBase.d("wrapping as other");
        return wrapAsOther(fd);
    }
    
    public IODescriptor wrapNow(int fd) throws IOException {
        try {
            int mode;
            
            IOException.System.check(mode=NativeHelper.getFdMode(fd),
                                     Native.getErrno());
            
            return wrapGivenMode(fd,mode);
        } catch (Throwable e) {
            Native.close(fd);
            if (e instanceof Error) {
                throw (Error)e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            } else if (e instanceof IOException) {
                throw (IOException)e;
            } else {
                throw new Error("Some error happened and I can't rethrow it becuase Java sucks.");
            }
        }
    }
    
    /** currently this may stall the VM on Mac OS X */
    public void wrap(int fd,
                     AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            try {
                cback.ready(new SimpleBuildFinalizer(wrapNow(fd)));
            } catch (IOException e) {
                cback.ready(AsyncFinalizer.Error.make(e));
            }
        } finally {
            U.l(prev);
        }
    }
    
    /** the fd is a socket, so it can be used with SIGIO, select, epoll,
     * and a bunch of other stuff.  also wrap in such a way that socket
     * syscalls are provided. */
    protected IODescriptor wrapAsSocket(int fd) {
        if (socketWrapifier != null) {
            return socketWrapifier.wrap(FD_TYPE_SOCKET, fd);
        }
        
        return otherWrapifier.wrap(FD_TYPE_SOCKET, fd);
    }
    
    /** the fd is a file, so SIGIO and select will not work, but AIO may
     * work if it is available.  also wrap in such a way that seekability
     * syscalls are provided. */
    protected IODescriptor wrapAsFile(int fd) {
        if (fileWrapifier != null) {
            return fileWrapifier.wrap(FD_TYPE_FILE, fd);
        }
        
        return otherWrapifier.wrap(FD_TYPE_FILE, fd);
    }
    
    /** we have no clue what the heck the descriptor is, so wrap it
     * conservatively. */
    protected IODescriptor wrapAsOther(int fd) {
        return otherWrapifier.wrap(FD_TYPE_UNKNOWN, fd);
    }
    
    public IODescriptor wrapAsType(int fd, FDType type) {
        if (type == FD_TYPE_SOCKET) {
            return wrapAsSocket(fd);
        }
        
        if (type == FD_TYPE_FILE) {
            return wrapAsFile(fd);
        }
        
        return wrapAsOther(fd);
    }
    
    public void register(FDType type,
                         SpecificWrapifier wrapifier) {
        if (type == FD_TYPE_SOCKET) {
            socketWrapifier = wrapifier;
        } else if (type == FD_TYPE_FILE) {
            fileWrapifier = wrapifier;
        } else if (type == FD_TYPE_UNKNOWN) {
            otherWrapifier = wrapifier;
        } else {
            // throw away the wrapifier as we don't know what the heck
            // it is for.
        }
    }
}

