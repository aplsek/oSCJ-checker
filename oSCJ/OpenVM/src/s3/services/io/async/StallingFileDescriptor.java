package s3.services.io.async;

import ovm.services.io.async.*;
import ovm.core.execution.*;
import ovm.core.services.memory.*;
import ovm.util.*;

public class StallingFileDescriptor
    extends DescriptorBase
    implements FileDescriptor {

    static final class NativeHelpers implements NativeInterface {
        static native int get_flags(int fd);
        static native int set_flags(int fd, int flags);
    }

    
    int flags;  // these are the file descriptor flags, with the O_NONBLOCK
                // bit set.

    public StallingFileDescriptor(int fd) {
        super(fd);
        
        OVMError.System.check("NativeHelpers.get_flags()",
                              flags=NativeHelpers.get_flags(fd),
                              Native.getErrno());
    }
    
    static class ReadFinalizerImpl
	extends AsyncFinalizer.Success
	implements ReadFinalizer {
	int result;
	ReadFinalizerImpl(int result) {
	    this.result=result;
	}
	public int getNumBytes() {
	    return result;
	}
    }

    static class WriteFinalizerImpl
	extends AsyncFinalizer.Success
	implements WriteFinalizer {
	int result;
	WriteFinalizerImpl(int result) {
	    this.result=result;
	}
	public int getNumBytes() {
	    return result;
	}
    }
    
    static class SeekFinalizerImpl
	extends AsyncFinalizer.Success
	implements SeekFinalizer {
	long absoluteOffset;
	long relativeOffset;
	SeekFinalizerImpl(long absoluteOffset,
			  long relativeOffset) {
	    this.absoluteOffset=absoluteOffset;
	    this.relativeOffset=relativeOffset;
	}
	public long getAbsoluteOffset() {
	    return absoluteOffset;
	}
	public long getRelativeOffset() {
	    return relativeOffset;
	}
    }

    public synchronized AsyncHandle read(AsyncMemoryCallback data,
					 int maxBytes,
					 AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            int result=-1;
            VM_Address buf=data.getBuffer(maxBytes,false);
            try {
                if (IOException.System.check(NativeHelpers.set_flags(getFD(),
                                                                     flags&~NativeConstants.O_NONBLOCK),
                                             Native.getErrno(),
                                             cback)) {
                    if (IOException.System.check(result=Native.read(getFD(),buf,maxBytes),
                                                 Native.getErrno(),
                                                 cback)) {
                        cback.ready(new ReadFinalizerImpl(result));
                    }
                    OVMError.System.check("NativeHelpers.set_flags()",
                                          NativeHelpers.set_flags(getFD(),flags),
                                          Native.getErrno());
                }
            } finally {
                data.doneBuffer(buf,result<0?0:result);
            }
        } finally {
            U.l(prev);
        }
	return StallingUtil.asyncHandle;
    }
    
    public synchronized AsyncHandle write(AsyncMemoryCallback data,
					  int numBytes,
					  AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            VM_Address buf=data.getBuffer(numBytes,false);
            try {
                if (IOException.System.check(NativeHelpers.set_flags(getFD(),
                                                                     flags&~NativeConstants.O_NONBLOCK),
                                             Native.getErrno(),
                                             cback)) {
                    final int result;
                    if (IOException.System.check(result=Native.write(getFD(),buf,numBytes),
                                                 Native.getErrno(),
                                                 cback)) {
                        cback.ready(new WriteFinalizerImpl(result));
                    }
                    OVMError.System.check("NativeHelpers.set_flags()",
                                          NativeHelpers.set_flags(getFD(),flags),
                                          Native.getErrno());
                }
            } finally {
                data.doneBuffer(buf,0);
            }
        } finally {
            U.l(prev);
        }
	return StallingUtil.asyncHandle;
    }

    public synchronized AsyncHandle pread(AsyncMemoryCallback data,
					  int maxBytes,
					  long offset,
					  AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            int result=-1;
            VM_Address buf=data.getBuffer(maxBytes,false);
            try {
                if (IOException.System.check(NativeHelpers.set_flags(getFD(),
                                                                     flags&~NativeConstants.O_NONBLOCK),
                                             Native.getErrno(),
                                             cback)) {
                    if (IOException.System.check(result=Native.pread(getFD(),buf,maxBytes,offset),
                                                 Native.getErrno(),
                                                 cback)) {
                        cback.ready(new ReadFinalizerImpl(result));
                    }
                    OVMError.System.check("NativeHelpers.set_flags()",
                                          NativeHelpers.set_flags(getFD(),flags),
                                          Native.getErrno());
                }
            } finally {
                data.doneBuffer(buf,result<0?0:result);
            }
        } finally {
            U.l(prev);
        }
	return StallingUtil.asyncHandle;
    }
    
    public synchronized AsyncHandle pwrite(AsyncMemoryCallback data,
					   int numBytes,
					   long offset,
					   AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            VM_Address buf=data.getBuffer(numBytes,false);
            try {
                if (IOException.System.check(NativeHelpers.set_flags(getFD(),
                                                                     flags&~NativeConstants.O_NONBLOCK),
                                             Native.getErrno(),
                                             cback)) {
                    final int result;
                    if (IOException.System.check(result=Native.pwrite(getFD(),buf,numBytes,offset),
                                                 Native.getErrno(),
                                                 cback)) {
                        cback.ready(new WriteFinalizerImpl(result));
                    }
                    OVMError.System.check("NativeHelpers.set_flags()",
                                          NativeHelpers.set_flags(getFD(),flags),
                                          Native.getErrno());
                }
            } finally {
                data.doneBuffer(buf,0);
            }
        } finally {
            U.l(prev);
        }
	return StallingUtil.asyncHandle;
    }

    public synchronized long tell() throws IOException {
	long result;
	IOException.System.check(
	    result=Native.lseek(getFD(), 0, NativeConstants.SEEK_CUR),
	    Native.getErrno());
	return result;
    }
    
    public synchronized long getSize() throws IOException {
	long result;
	IOException.System.check(
	    result=Native.file_size(getFD()),
	    Native.getErrno());
	return result;
    }
 
    protected IODescriptor createMyselfWithFD(int newFd) {
        return new StallingFileDescriptor(newFd);
    }
    
    synchronized void seek(long offset,
                           int whence,
                           AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            long oldOffset;
            long newOffset;
            if (!IOException.System.check(
                     oldOffset=Native.lseek(getFD(), 0, NativeConstants.SEEK_CUR),
                     Native.getErrno(),
                     cback)) {
                return;
            }
            if (!IOException.System.check(
                     newOffset=Native.lseek(getFD(), offset, whence),
                     Native.getErrno(),
                     cback)) {
                return;
            }
            cback.ready(new SeekFinalizerImpl(newOffset, newOffset-oldOffset));
        } finally {
            U.l(prev);
        }
    }
    
    public AsyncHandle seekSet(long offset,
			       AsyncCallback cback) {
	seek(offset, NativeConstants.SEEK_SET, cback);
	return StallingUtil.asyncHandle;
    }
    
    public AsyncHandle seekEnd(long offset,
			       AsyncCallback cback) {
	seek(offset, NativeConstants.SEEK_END, cback);
	return StallingUtil.asyncHandle;
    }
    
    public AsyncHandle seekCur(long offset,
			       AsyncCallback cback) {
	seek(offset, NativeConstants.SEEK_CUR, cback);
	return StallingUtil.asyncHandle;
    }
    
    public synchronized AsyncHandle truncate(long size,
					     AsyncCallback cback) {
	if (IOException.System.check(
		Native.ftruncate(getFD(), size),
		Native.getErrno(),
		cback)) {
	    cback.ready(AsyncFinalizer.Success.getInstance());
	}
	return StallingUtil.asyncHandle;
    }
    
    public synchronized void bufferSyncNow() throws IOException {
	IOException.System.check(Native.fsync(getFD()),
				 Native.getErrno());
    }

    public void cancel(IOException error) {
	// do nothing because all operations are stalling anyway
    }

    public static class SpecificWrapifier
        implements FileDescriptorWrapifier.SpecificWrapifier {
        public IODescriptor wrap(FileDescriptorWrapifier.FDType type,
                                 int fd) {
            return new StallingFileDescriptor(fd);
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
 
    public AsyncHandle lock(long start,
			    long len,
			    boolean shared,
			    AsyncCallback ac) {
	while (true) {
	    // FIXME: add yield here!
	    int ret
		= Native.mylock(getFD(), 
				start,
				len,
				shared?1:0);
	    if (ret == 0) {
		ac.ready(AsyncFinalizer.Success.getInstance());
		return StallingUtil.asyncHandle; // success
	    }
	    if (ret == -1) { // failure (but not due to other lock)
		ac.ready(new AsyncFinalizer.Error
			 (IOException.System.make(Native.getErrno())));
		return StallingUtil.asyncHandle;  // error
	    }
	}
    }
}


