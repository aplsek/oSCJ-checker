// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/PollingFileDescriptor.java,v 1.14 2007/06/03 01:25:47 baker29 Exp $

package s3.services.io.async;

import ovm.core.execution.*;
import ovm.core.services.memory.*;
import ovm.services.io.async.*;
import ovm.services.io.signals.*;
import ovm.services.threads.*;

/**
 * Implements file I/O using polling.
 * @author Filip Pizlo
 */
public class PollingFileDescriptor
    extends SignalDescriptorBase
    implements FileDescriptor {
    
    protected AsyncOpQueue unifiedQueue;
    
    /**
     * @param _iosm a polling implementation of the IOSignalManager
     * @param fd a file descriptor that requires polling and allows for seeking
     */
    PollingFileDescriptor(UserLevelThreadManager tm,
			  final IOPollingManager _iosm,
			  int fd) {
	super(tm, _iosm, fd);
	
	unifiedQueue=new AsyncOpQueue(this,fd,tm,"unified"){
		protected void addToSignalManager(AsyncOpQueue.OpNode cback) {
		    _iosm.addCallback(this.fd,cback);
		}
		protected void removeFromSignalManager(AsyncOpQueue.OpNode cback,
						       Object byWhat) {
		    _iosm.removeCallbackFromFD(this.fd,cback,byWhat);
		}
	    };
    }
    
    protected static class SeekOpNodeImpl
	extends AsyncOpQueue.OpNode
	implements SeekFinalizer {
	
	private int fd_;
	private long offset_;
	private int whence_;
	private long absoluteOffset_;
	private long relativeOffset_;
	
	public SeekOpNodeImpl(int fd,
			      long offset,
			      int whence,
			      AsyncCallback cback) {
	    super(cback);
	    this.fd_ = fd;
	    this.offset_ = offset;
	    this.whence_ = whence;
	}
	
	public final boolean doOp() {
	    long oldOffset;
	    
	    if (!check(oldOffset=Native.lseek(fd_, 0, NativeConstants.SEEK_CUR),
		       Native.getErrno())) {
		return true;
	    }
	    
	    if (!check(absoluteOffset_=Native.lseek(fd_, offset_, whence_),
		       Native.getErrno())) {
		return true;
	    }
	    
	    relativeOffset_=absoluteOffset_-oldOffset;
	    
	    return true;
	}
	
	public long getAbsoluteOffset() {
	    return absoluteOffset_;
	}
	
	public long getRelativeOffset() {
	    return relativeOffset_;
	}
    }
    
    protected class ReadOpNodeImpl extends ReadOpNode {
        
        public ReadOpNodeImpl(SignalDescriptorBase outer,
			      AsyncMemoryCallback data,
			      int maxBytes,
			      AsyncCallback cback) {
            super(outer,data,maxBytes,cback);
        }
        
        protected int doRead(VM_Address buf, int bytes) {
            return Native.read(getFD(), buf, bytes);
        }
        
        protected int doRead(VM_Address buf) {
            return doRead(buf,maxBytes);
        }        
    }
    
    protected class WriteOpNodeImpl extends WriteOpNode {
        
        public WriteOpNodeImpl(SignalDescriptorBase outer,
			       AsyncMemoryCallback data,
                               int maxBytes,
                               AsyncCallback cback) {
            super(outer,data,maxBytes,cback);
        }
        
        protected int doWrite(VM_Address buf, int bytes) {
            return Native.write(getFD(),buf,bytes);
        }

        protected int doWrite(VM_Address buf) {
            return doWrite(buf,maxBytes);
        }        
    }

    protected class PReadOpNodeImpl extends ReadOpNode {
	private long offset_;
	
	public PReadOpNodeImpl(SignalDescriptorBase outer,
			       AsyncMemoryCallback data,
			       int maxBytes,
			       long offset,
			       AsyncCallback cback) {
	    super(outer, data, maxBytes, cback);
	    this.offset_=offset;
	}
	
	protected int doRead(VM_Address buf, int bytes) {
	    return Native.pread(getFD(), buf, bytes, offset_);
	}

	protected int doRead(VM_Address buf) {
	    return doRead(buf, maxBytes);
	}	
    }
    
    protected class PWriteOpNodeImpl extends WriteOpNode {
	private long offset_;
	
	public PWriteOpNodeImpl(SignalDescriptorBase outer,
				AsyncMemoryCallback data,
				int maxBytes,
				long offset,
				AsyncCallback cback) {
	    super(outer, data, maxBytes, cback);
	    this.offset_=offset;
	}
	
	protected int doWrite(VM_Address buf, int bytes) {
	    return Native.pwrite(getFD(), buf, bytes, offset_);
	}

	protected int doWrite(VM_Address buf) {
	    return doWrite(buf, maxBytes);
	}	
    }
    
    protected static class TruncateOpNodeImpl extends AsyncOpQueue.OpNode {
	int fd_;
	long newSize_;
	
	public TruncateOpNodeImpl(int fd,
				  long newSize,
				  AsyncCallback cback) {
	    super(cback);
	    this.fd_=fd;
	    this.newSize_=newSize;
	}
	
	public final boolean doOp() {
	    check(Native.ftruncate(fd_, newSize_),
		  Native.getErrno());
	    return true;
	}
    }
    
    public synchronized AsyncHandle read(final AsyncMemoryCallback data,
					 final int maxBytes,
					 final AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return unifiedQueue.performOp(new ReadOpNodeImpl(this,data,maxBytes,cback));
        } finally {
            U.l(prev);
        }
    }
    
    public synchronized AsyncHandle write(final AsyncMemoryCallback data,
					  final int maxBytes,
					  final AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return unifiedQueue.performOp(new WriteOpNodeImpl(this,data,maxBytes,cback));
        } finally {
            U.l(prev);
        }
    }
    
    public long tell() throws IOException {
	long result;
	IOException.System.check(
	    result=Native.lseek(getFD(), 0, NativeConstants.SEEK_CUR),
	    Native.getErrno());
	return result;
    }
    
    public long getSize() throws IOException {
	long result;
	IOException.System.check(
	    result=Native.file_size(getFD()),
	    Native.getErrno());
	return result;
    }
 
    public synchronized AsyncHandle seekSet(long offset,
					    AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return unifiedQueue.performOp(new SeekOpNodeImpl(getFD(),
							     offset,
							     NativeConstants.SEEK_SET,
							     cback));
        } finally {
            U.l(prev);
        }
    }   
 
    public synchronized AsyncHandle seekEnd(long offset,
					    AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return unifiedQueue.performOp(new SeekOpNodeImpl(getFD(),
							     offset,
							     NativeConstants.SEEK_END,
							     cback));
        } finally {
            U.l(prev);
        }
    }   
 
    public synchronized AsyncHandle seekCur(long offset,
					    AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return unifiedQueue.performOp(new SeekOpNodeImpl(getFD(),
							     offset,
							     NativeConstants.SEEK_CUR,
							     cback));
        } finally {
            U.l(prev);
        }
    }   

    public synchronized AsyncHandle pread(AsyncMemoryCallback data,
					  int maxBytes,
					  long offset,
					  AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return unifiedQueue.performOp(new PReadOpNodeImpl(this,
							      data,
							      maxBytes,
							      offset,
							      cback));
        } finally {
            U.l(prev);
        }
    }
    
    public synchronized AsyncHandle pwrite(AsyncMemoryCallback data,
					   int maxBytes,
					   long offset,
					   AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return unifiedQueue.performOp(new PWriteOpNodeImpl(this,
							       data,
							       maxBytes,
							       offset,
							       cback));
        } finally {
            U.l(prev);
        }
    }
    
    public synchronized AsyncHandle truncate(long size,
					     AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return unifiedQueue.performOp(new TruncateOpNodeImpl(getFD(),size,cback));
        } finally {
            U.l(prev);
        }
    }
    
    public void bufferSyncNow() throws IOException {
	// do nothing! :-)
    }

    public synchronized void cancel(IOException error) {
        iosm.removeFD(getFD(),error);
        unifiedQueue.cancelAll(error);
    }

    protected IODescriptor createMyselfWithFD(int newFd) {
	return new PollingFileDescriptor(tm,(IOPollingManager)iosm,newFd);
    }
    
    public static class SpecificWrapifier
	extends SignalDescriptorBase.SpecificWrapifier {
        
        public SpecificWrapifier(IOPollingManager iosm) {
	    super(iosm);
        }
        
        public IODescriptor wrap(FileDescriptorWrapifier.FDType type,
                                 int fd) {
            return new PollingFileDescriptor(tm,(IOPollingManager)iosm,fd);
        }
    }

    public AsyncHandle lock(long start,
			    long len,
			    boolean shared,
			    AsyncCallback ac) {
        VM_Area prev = U.e(ac);
        try {
            return unifiedQueue.performOp(new LockOpNodeImpl(getFD(),
							     start,
							     len,
							     shared,
							     ac));
        } finally {
            U.l(prev);
        }
    }

    // NOTE: the locking code assumes that Native.mylock
    // is non-blocking, which it is unless we're on NFS.
    // On NFS, the VM may stall for the duration of the NFS
    // locking request.
    public int trylock(long start,
		       long len,
		       boolean shared) {
 	return Native.mylock(getFD(), 
			     start,
			     len,
			     shared?1:0);
    }

    public int unlock(long start,
		      long len) {
	return Native.myunlock(getFD(),
			       start,
			       len);
    }
   
    static class LockOpNodeImpl extends AsyncOpQueue.OpNode {
	int fd_;
	long start_;
	long len_;
	boolean shared_;
	
	LockOpNodeImpl(int fd,
		       long start,
		       long len,
		       boolean shared,
		       AsyncCallback cback) {
	    super(cback);
	    this.fd_ = fd;
	    this.start_ = start;
	    this.len_ = len;
	    this.shared_ = shared;
	}
	
	public final boolean doOp() {
	    int ret
		= Native.mylock(fd_, 
				start_,
				len_,
				shared_?1:0);
	    if (ret == -1) {
		check(ret,
		      Native.getErrno());
		return true;
	    }
	    if (ret == 0)
		return true;
	    return false; // ret == 1: try again
	}
    }
}

