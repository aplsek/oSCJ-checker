package s3.services.io.async;

import ovm.core.execution.*;
import ovm.core.services.memory.*;
import ovm.services.io.async.*;
import ovm.services.io.signals.*;
import ovm.services.threads.*;

/**
 *
 * @author Filip Pizlo
 */
public class SignalRWDescriptor
    extends SignalRWDescriptorBase
    implements RWIODescriptor,
               MultiSelectableIODescriptor {

    SignalRWDescriptor(UserLevelThreadManager tm,
                       IOSignalManager iosm,
                       int fd) {
	super(tm, iosm, fd);
    }
    
    public synchronized void addToSelector(Selector mux) {
        try {
            ((BaselineSelector)mux).addDescriptor(getFD(),this);
        } catch (ClassCastException e) {
            throw new ovm.util.OVMError.UnsupportedOperation(
                "Attempt to add an SignalRWDescriptor "+
                "to a Selector that is not an instance "+
                "of BaselineSelector");
        }
    }
    
    public synchronized void removeFromSelector(Selector mux) {
        try {
            ((BaselineSelector)mux).removeDescriptor(getFD(),this);
        } catch (ClassCastException e) {
            throw new ovm.util.OVMError.UnsupportedOperation(
                "Attempt to add an SignalRWDescriptor "+
                "to a Selector that is not an instance "+
                "of BaselineSelector");
        }
    }
    
    protected static class ReadOpNodeImpl extends ReadOpNode {
        
        public ReadOpNodeImpl(SignalDescriptorBase outer,
			      AsyncMemoryCallback data,
			      int maxBytes,
			      AsyncCallback cback) {
            super(outer,data,maxBytes,cback);
        }
        
        protected int doRead(VM_Address buf, int bytes) {
            if (true) {
              MemoryManager.the().checkAccess(buf);
              MemoryManager.the().checkAccess(buf.add(bytes-1));
            }
            return Native.read(getFD(), buf, bytes);
        }
        
        protected int doRead(VM_Address buf) {
            return doRead(buf,maxBytes);
        }
    }
    
    public synchronized AsyncHandle read(final AsyncMemoryCallback data,
					 final int maxBytes,
					 final AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return readQueue.performOp(new ReadOpNodeImpl(this,data,maxBytes,cback));
        } finally {
            U.l(prev);
        }
    }
    
    static protected class WriteOpNodeImpl extends WriteOpNode {
        
        public WriteOpNodeImpl(SignalDescriptorBase outer,
			       AsyncMemoryCallback data,
			       int maxBytes,
			       AsyncCallback cback) {
            super(outer, data,maxBytes,cback);
        }
        
        protected int doWrite(VM_Address buf, int bytes) {
            if (true) {
              MemoryManager.the().checkAccess(buf);
              MemoryManager.the().checkAccess(buf.add(bytes-1));
            }        
            return Native.write(getFD(), buf, bytes);
        }
        
        protected int doWrite(VM_Address buf) {
            return doWrite(buf, maxBytes);
        }
    }

    public synchronized AsyncHandle write(final AsyncMemoryCallback data,
					  final int maxBytes,
					  final AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return writeQueue.performOp(new WriteOpNodeImpl(this,data,maxBytes,cback));
        } finally {
            U.l(prev);
        }
    }
    
    protected IODescriptor createMyselfWithFD(int newFd) {
        return new SignalRWDescriptor(tm,iosm,newFd);
    }
    
    public static class SpecificWrapifier
	extends SignalDescriptorBase.SpecificWrapifier {
        
        public SpecificWrapifier(IOSignalManager iosm) {
	    super(iosm);
        }
        
        public IODescriptor wrap(FileDescriptorWrapifier.FDType type,
                                 int fd) {
            return new SignalRWDescriptor(tm,iosm,fd);
        }
    }

}

