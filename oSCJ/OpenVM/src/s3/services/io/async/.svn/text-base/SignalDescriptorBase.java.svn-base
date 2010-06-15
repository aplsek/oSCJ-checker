
package s3.services.io.async;

import ovm.core.execution.Native;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.MemoryManager;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.io.async.AsyncCallback;
import ovm.services.io.async.AsyncMemoryCallback;
import ovm.services.io.async.FileDescriptorWrapifier;
import ovm.services.io.async.IODescriptor;
import ovm.services.io.async.RWIODescriptor;
import ovm.services.io.signals.IOSignalManager;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.OVMError;

/**
 *
 * @author Filip Pizlo
 */
abstract class SignalDescriptorBase extends DescriptorBase {

    private static boolean DEBUG = false;

    protected UserLevelThreadManager tm;
    protected IOSignalManager iosm;

    SignalDescriptorBase(UserLevelThreadManager tm,
			 IOSignalManager iosm,
			 int fd) {
        super(fd);
        this.tm=tm;
        this.iosm=iosm;
    }
    
    protected static abstract class RWOpNode
        extends AsyncOpQueue.OpNode {
	
        protected int processedBytes = 0;
	private SignalDescriptorBase outer_;

	protected void setOuter(SignalDescriptorBase outer)
	    throws PragmaNoBarriers {
	    this.outer_=outer;
	}
	protected SignalDescriptorBase getOuter() throws PragmaNoBarriers {
	    return outer_;
	}
	
	protected  abstract int doTheOp( VM_Address buf, int bytes );
	protected  abstract int doTheOp( VM_Address buf );
	
        /* returns true if the call is done: either sucesfully or with an error */
        public boolean doOp() {
        
          if (MemoryManager.the().usesArraylets()) {
            data.pinBuffer();

            if (DEBUG) {
              Native.print_string("In doOp\n");
            }
            try {
              for(;;) {

                VM_Address buf = data.getContiguousBuffer( maxBytes-processedBytes, processedBytes );
                
                if (DEBUG) {
                  Native.print_string("Got contiguous buffer at ");
                  Native.print_ptr(buf);
                  Native.print_string(" for number of bytes ");
                  Native.print_int(data.getLastContiguousBufferLength());
                  Native.print_string(" after having processed number of bytes ");
                  Native.print_int(processedBytes);
		  Native.print_string("\n");                  
                }
                
		if (checkRepeat(result=doTheOp(buf, data.getLastContiguousBufferLength()), /* returns true if the call should be repeated later */
				Native.getErrno())) {
                    if (DEBUG) {
                      Native.print_string("Bailing out because would block\n");				
                    }
		    return false; 
		}              
		if (result<0) {
		  if (DEBUG) {
  		    Native.print_string("Bailing out with error\n");
                  }
		  return true; /* error */
		}
		processedBytes += result;
		
		if (DEBUG) {
		  Native.print_string("Buffer content after sucessful operation:\n");
		  for(int off=0;off<result;off++) {
  		    Native.print_char(buf.add(off).getByte());
                  }
                  Native.print_string("\n----------------end of content dump----------------\n");
		}
		
		if ( (processedBytes == maxBytes) || (result==0) ) {
		  result = processedBytes;
		  if (DEBUG) {
		    Native.print_string("Exiting because done\n");
                  }
		  return true; /* done */    
		}
              }
            } finally {
              if (DEBUG) {
                Native.print_string("Finally - unpinning buffer\n");
              }
              data.unpinBuffer();
            }
          } else {

	    VM_Address buf=data.getBuffer(maxBytes,false);
	    try {
		if (checkRepeat(result=doTheOp(buf), /* returns true if the call should be repeated later */
				Native.getErrno())) {
		    return false; 
		}
	    } finally {
		data.doneBuffer(buf,result<0?0:result);
	    }
	    return true;
          }
        }
	
	// helper
	protected int getFD() {
	    return getOuter().getFD();
	}

        protected AsyncMemoryCallback data;
        protected int maxBytes;
        protected int result=0;
        
        public RWOpNode(SignalDescriptorBase outer,
			AsyncMemoryCallback data,
                        int maxBytes,
                        AsyncCallback cback)
	    throws PragmaNoBarriers {
            super(cback);
	    setOuter(outer);
            this.data=data;
            this.maxBytes=maxBytes;
        }
        
        public final int getNumBytes() {
            return result;
        }
    }
    
    protected static abstract class ReadOpNode
        extends RWOpNode
        implements RWIODescriptor.ReadFinalizer {
        
        public ReadOpNode(SignalDescriptorBase outer,
			  AsyncMemoryCallback data,
                          int maxBytes,
                          AsyncCallback cback) {
            super(outer,data,maxBytes,cback);
        }
        
        protected abstract int doRead(VM_Address buf);
        
        protected abstract int doRead(VM_Address buf, int bytes);

        protected int doTheOp(VM_Address buf) {
          return doRead(buf);
        }

        protected int doTheOp(VM_Address buf, int bytes) {
          return doRead(buf, bytes);
        }        
    }
    
    protected static abstract class WriteOpNode
        extends RWOpNode
        implements RWIODescriptor.WriteFinalizer {
        

        public WriteOpNode(SignalDescriptorBase outer,
			   AsyncMemoryCallback data,
                           int maxBytes,
                           AsyncCallback cback) {
            super(outer, data, maxBytes,cback);
	    setOuter(outer);

        }
        
        protected abstract int doWrite(VM_Address buf);
        
        protected abstract int doWrite(VM_Address buf, int bytes);
        
        protected int doTheOp(VM_Address buf) {
          return doWrite(buf);
        }
	
	protected int doTheOp(VM_Address buf, int bytes) {
          return doWrite(buf, bytes);
        }
	        

    }

    public boolean readyForRead() {
	return true;
    }
    
    public boolean readyForWrite() {
	return true;
    }
    
    public boolean readyForExcept() {
	return true;
    }
    
    public abstract static class SpecificWrapifier
        implements FileDescriptorWrapifier.SpecificWrapifier {
        
        protected UserLevelThreadManager tm;
        protected IOSignalManager iosm;
        
        public SpecificWrapifier(IOSignalManager iosm) {
            tm = (UserLevelThreadManager)
                ((ThreadServicesFactory)ThreadServiceConfigurator.config.
                    getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
            if (tm == null) {
                throw new OVMError.Configuration("need a configured thread manager");
            }
            
            this.iosm = iosm;
        }
        
        public abstract IODescriptor wrap(FileDescriptorWrapifier.FDType type,
					  int fd);
    }
}

