// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/DescriptorBase.java,v 1.4 2004/10/09 21:43:04 pizlofj Exp $

package s3.services.io.async;

import ovm.core.execution.*;
import ovm.services.io.async.*;
import ovm.core.services.memory.*;
import ovm.core.*;

/**
 *
 * @author Filip Pizlo
 */
abstract class DescriptorBase implements IODescriptor {
    
    private static final class NativeHelper
        implements NativeInterface {
        
        public static native int dup(int fd);
        
    }

    private int fd_;
    
    DescriptorBase(int fd) {
        this.fd_=fd;
        
        // made the fd non-blocking
        if (Native.makeNonBlocking(fd)<0) {
            throw Executive.panic(
                "Could not make file descriptor non-blocking.");
        }
    }
    
    public int getFD() {
	return fd_;
    }
    
    public boolean isOpen() {
        return fd_>=0;
    }
    
    public synchronized int getAvailable() throws IOException {
	int result=Native.availableForRead(getFD());
	int errno=Native.getErrno();
	
	// why is ENOTTY used to signal bad file descriptor type?  probably
	// some weird historical reason that I don't wanna know about.
	if (errno==NativeConstants.ENOTTY) {
	    throw IOException.Unsupported.getInstance();
	}
	
	return IOException.System.check(result,errno);
    }
    
    public int tryReadNow(VM_Address address,
			  int maxBytes) throws IOException {
	while (true) {
	    if (true) {
	      MemoryManager.the().checkAccess(address);
	      MemoryManager.the().checkAccess(address.add(maxBytes-1));
	    }
	    
            int result=Native.read(getFD(),address,maxBytes);
            int errno=Native.getErrno();
            
            if (result>=0) {
                return result;
            }
            
            if (errno==NativeConstants.EWOULDBLOCK ||
                errno==NativeConstants.EAGAIN) {
                return -1;
            }
            
            if (errno!=NativeConstants.EINTR) {
                throw IOException.System.make(errno);
            }
        }
    }
    
    public int tryPreadNow(VM_Address address,
                           int maxBytes,
                           long offset) throws IOException {
        while (true) {
            int result=Native.pread(getFD(),address,maxBytes,offset);
            int errno=Native.getErrno();
            
            if (result>=0) {
                return result;
            }
            
            if (errno==NativeConstants.EWOULDBLOCK ||
                errno==NativeConstants.EAGAIN) {
                return -1;
            }
            
            if (errno!=NativeConstants.EINTR) {
                throw IOException.System.make(errno);
            }
        }
    }

    public int tryWriteNow(VM_Address address,
			   int maxBytes) throws IOException {
	while (true) {

	    if (true) {
	      MemoryManager.the().checkAccess(address);
	      MemoryManager.the().checkAccess(address.add(maxBytes-1));
	    }
	    
            int result=Native.write(getFD(),address,maxBytes);
            int errno=Native.getErrno();
            
            if (result>=0) {
                return result;
            }
            
            if (errno==NativeConstants.EWOULDBLOCK ||
                errno==NativeConstants.EAGAIN) {
                return -1;
            }
            
            if (errno!=NativeConstants.EINTR) {
                throw IOException.System.make(errno);
            }
        }
    }

    public int tryPwriteNow(VM_Address address,
			    int maxBytes,
                            long offset) throws IOException {
	while (true) {
            int result=Native.pwrite(getFD(),address,maxBytes,offset);
            int errno=Native.getErrno();
            
            if (result>=0) {
                return result;
            }
            
            if (errno==NativeConstants.EWOULDBLOCK ||
                errno==NativeConstants.EAGAIN) {
                return -1;
            }
            
            if (errno!=NativeConstants.EINTR) {
                throw IOException.System.make(errno);
            }
        }
    }

    public synchronized void close() {
        //Native.print_string("CLOSING ");
        //Native.print_int(getFD());
        //Native.print_string("\n");
        cancel(IOException.Canceled.getInstance());
        Native.close(fd_);
        fd_=-1;
    }
    
    protected abstract IODescriptor createMyselfWithFD(int newFd);
    
    public synchronized IODescriptor dup() throws IOException {
        int newFd;
        boolean allGood=false;
        IOException.System.check(newFd=NativeHelper.dup(getFD()),
                                 Native.getErrno());
        try {
            IODescriptor ret=createMyselfWithFD(newFd);
            allGood=true;
            return ret;
        } finally {
            if (!allGood) {
                Native.close(newFd);
            }
        }
    }
}

