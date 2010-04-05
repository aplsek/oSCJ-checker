
package s3.services.io.signals;

import ovm.core.execution.NativeConstants;
import ovm.core.services.timer.*;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.TimerServicesFactory;
import ovm.services.io.signals.*;
import ovm.util.OVMError;
import s3.util.PragmaAtomic;

/**
 * Craptastic implementation of IOSignalManager that just does
 * polling.
 * @author Filip Pizlo
 */
public class IOSignalManagerViaPolling
    extends IOSignalManagerBase
    implements TimerInterruptAction,
	       IOPollingManager {
    
    protected TimerManager tm;
    
    private static final IOSignalManagerViaPolling
        instance_=new IOSignalManagerViaPolling();
    public static IOPollingManager getInstance() {
        return instance_;
    }
    
    public void init() {
        tm = ((TimerServicesFactory) ThreadServiceConfigurator
              .config
              .getServiceFactory(TimerServicesFactory.name))
            .getTimerManager();
        if (tm==null) {
            throw new OVMError.Configuration("need a configured timer manager");
        }
        isInited=true;
    }
    
    public void start() {
        tm.addTimerInterruptAction(this);
        super.start();
    }
    
    public void stop() {
        tm.removeTimerInterruptAction(this);
        super.stop();
    }

    public void addCallback(int fd,
			    Callback cback)
	throws PragmaAtomic {
	addCallbackImpl(fd,cback);
    }
    
    protected boolean[] enabledFdMap=new boolean[NativeConstants.FD_SETSIZE];
    protected int[] enabledFds=new int[NativeConstants.FD_SETSIZE];
    protected int numEnabledFds=0;

    protected void enableFD(int fd) {
	if (!enabledFdMap[fd]) {
	    enabledFds[numEnabledFds++]=fd;
	    enabledFdMap[fd]=true;
	}
    }
    protected void disableFD(int fd) {
	// disabled lazily
    }
    
    public void fire(int ticks) {
        if (ticks<1) {
            return;
        }

	for (int i=0;i<numEnabledFds;++i) {
	    int fd=enabledFds[i];
	    if (fdUsed(fd)) {
		callIOSignalOnFd(fd);
	    } else {
		enabledFds[i--]=enabledFds[--numEnabledFds];
		enabledFdMap[fd]=false;
	    }
        }
    }

    public String timerInterruptActionShortName() {
	return "pollingio";
    }
}

