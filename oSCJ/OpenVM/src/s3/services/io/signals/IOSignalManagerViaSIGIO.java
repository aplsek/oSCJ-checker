package s3.services.io.signals;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Oop;
import ovm.core.execution.NativeConstants;
import ovm.core.execution.NativeInterface;
import ovm.core.services.events.EventManager;
import ovm.core.services.memory.VM_Address;
import ovm.core.stitcher.EventServicesFactory;
import ovm.core.stitcher.IOServiceConfigurator;
import ovm.services.io.signals.IOSignalManager;
import ovm.core.services.memory.MemoryManager;
import ovm.util.OVMError;
import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;
/**
 * This class should only be called from sections of code where both
 * scheduling and event management are disabled.
 * @author Filip Pizlo
 */
public class IOSignalManagerViaSIGIO
    extends IOSignalManagerBase
    implements ovm.core.services.events.EventManager.EventProcessor {
    
    private static final class NativeHelper
        implements NativeInterface {
        
        // the four functions below return 0 on success and errno on error.
        // this assumes that an errno of zero is 'ESUCCESS'.
        
        public static native int enableIOSignalManager();
        public static native int disableIOSignalManager();
        
        public static native int enableIOSignal(int fd);
        public static native int disableIOSignal(int fd);
        
        // returns -1 if we do not know which fds triggered, and the
        // number of fds that triggered otherwise.
        public static native int getKnownSignaledFDs(VM_Address fds);

	public static native int checkIfBlock(int fd, int mode);
        
    }
    
    protected EventManager em;
    
    private static final IOSignalManagerViaSIGIO
        instance_=new IOSignalManagerViaSIGIO();
    public static IOSignalManager getInstance() {
        return instance_;
    }
    
    public void init() {
        em = ((EventServicesFactory) IOServiceConfigurator
              .config
              .getServiceFactory(EventServicesFactory.name))
            .getEventManager();
        if (em==null) {
            throw new OVMError.Configuration("need a configured event manager");
        }
        isInited=true;
        
        if (MemoryManager.the().usesArraylets()) {
          assert( NativeConstants.FD_SETSIZE < MemoryManager.the().arrayletSize() + 32 );
        }
    }
    
    public void start() {
        OVMError.System.check("Enabling edge trigger manager",
                              NativeHelper.enableIOSignalManager());
        em.addEventProcessor(this);
        super.start();
    }
    
    public void stop() {
        em.removeEventProcessor(this);
        OVMError.System.check("Disabling edge trigger manager",
                              NativeHelper.disableIOSignalManager());
        super.stop();
    }
    
    protected void enableFD(int fd) {            
        OVMError.System.check("Enabling edge trigger on file descriptor",
                              NativeHelper.enableIOSignal(fd));
    }
    
    protected void disableFD(int fd) {
        NativeHelper.disableIOSignal(fd);
    }
    
    public void addCallbackForRead(int fd,Callback cback)
	throws PragmaAtomic {
        super.addCallbackForRead(fd,cback);

	if (NativeHelper.checkIfBlock(fd,
				      NativeConstants.BLOCKINGIO_READ)
	    == 0) {
	    if (!cback.signal(true)) {
		removeCallbackFromFDImpl(fd,cback,Callback.BY_SIGNAL);
	    }
	}
    }

    public void addCallbackForWrite(int fd,Callback cback)
	throws PragmaAtomic {
        super.addCallbackForWrite(fd,cback);

	if (NativeHelper.checkIfBlock(fd,
				      NativeConstants.BLOCKINGIO_WRITE)
	    == 0) {
	    if (!cback.signal(true)) {
		removeCallbackFromFDImpl(fd,cback,Callback.BY_SIGNAL);
	    }
	}
    }

    public void addCallbackForExcept(int fd,Callback cback)
	throws PragmaAtomic {
        super.addCallbackForExcept(fd,cback);

	if (NativeHelper.checkIfBlock(fd,
				      NativeConstants.BLOCKINGIO_EXCEPT)
	    == 0) {
	    if (!cback.signal(true)) {
		removeCallbackFromFDImpl(fd,cback,Callback.BY_SIGNAL);
	    }
	}
    }

    public String eventProcessorShortName() {
	return "sigio";
    }
    
    private int[] fds=new int[NativeConstants.FD_SETSIZE];

    public void eventFired() throws PragmaNoPollcheck {
        // check if we know what fd's are triggered.  if we do not,
        // simply do edge trigger on everything.
        
        // note that the VM_Address stuff we are doing here is GC
        // safe because fds is allocated in the image (by virtue
        // of the fact that this object is allocated in the image).
        // if this object is ever not allocated in the image, then
        // it should almost certainly end up in immortal memory
        // since it is a singleton.
        
        Oop oop=VM_Address.fromObject(fds).asOop();
        Blueprint.Array bp=(Blueprint.Array)oop.getBlueprint();
        VM_Address ea = null;
        if (MemoryManager.the().usesArraylets()) {
          ea = MemoryManager.the().addressOfElement(VM_Address.fromObjectNB(oop), 0, bp.getComponentSize());
        } else {
          ea = bp.addressOfElement(oop,0);
        }
        int len=NativeHelper.getKnownSignaledFDs(ea);
        if (len<0) {
            // iterate over all
            for (int i=0;i<NativeConstants.FD_SETSIZE;++i) {
                if (!fdUsed(i)) {
                    continue;
                }
                callIOSignalOnFd(i);
            }
        } else {
            // iterate over only some
            for (int i=0;i<len;++i) {
                callIOSignalOnFd(fds[i]);
            }
        }
    }
}

