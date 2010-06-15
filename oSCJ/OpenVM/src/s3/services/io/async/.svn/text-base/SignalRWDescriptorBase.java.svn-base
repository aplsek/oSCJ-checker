// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/SignalRWDescriptorBase.java,v 1.7 2004/10/09 21:43:04 pizlofj Exp $

package s3.services.io.async;

import ovm.core.execution.NativeConstants;
import ovm.core.execution.NativeInterface;
import ovm.services.io.async.IOException;
import ovm.services.io.signals.IOSignalManager;
import ovm.services.threads.UserLevelThreadManager;

/**
 *
 * @author Filip Pizlo
 */
abstract class SignalRWDescriptorBase
    extends SignalDescriptorBase {

    private static final class NativeHelper
        implements NativeInterface {
        
        public static native int checkIfBlock(int fd,int forWhat);
        
    }

    protected AsyncOpQueue readQueue;
    protected AsyncOpQueue writeQueue;
    
    SignalRWDescriptorBase(UserLevelThreadManager tm,
			   IOSignalManager iosm,
			   int _fd) {
        super(tm,iosm,_fd);

        this.readQueue=new AsyncOpQueue(this,_fd,tm,"read"){
		protected void addToSignalManager(AsyncOpQueue.OpNode cback) {
		    SignalRWDescriptorBase.this.iosm.addCallbackForRead(fd,cback);
		}
		protected void removeFromSignalManager(AsyncOpQueue.OpNode cback,
						       Object byWhat) {
		    SignalRWDescriptorBase.this.iosm
			.removeCallbackFromFD(fd,cback,byWhat);
		}
	    };
        this.writeQueue=new AsyncOpQueue(this,_fd,tm,"write"){
		protected void addToSignalManager(AsyncOpQueue.OpNode cback) {
		    SignalRWDescriptorBase.this.iosm.addCallbackForWrite(fd,cback);
		}
		protected void removeFromSignalManager(AsyncOpQueue.OpNode cback,
						       Object byWhat) {
		    SignalRWDescriptorBase.this.iosm
			.removeCallbackFromFD(fd,cback,byWhat);
		}
	    };
    }
    
    public boolean readyForRead() {
        return NativeHelper.checkIfBlock(getFD(),NativeConstants.BLOCKINGIO_READ)==0;
    }
    
    public boolean readyForWrite() {
        return NativeHelper.checkIfBlock(getFD(),NativeConstants.BLOCKINGIO_WRITE)==0;
    }
    
    public boolean readyForExcept() {
        return NativeHelper.checkIfBlock(getFD(),NativeConstants.BLOCKINGIO_EXCEPT)==0;
    }
    
    public synchronized void cancel(IOException error) {
        iosm.removeFD(getFD(),error);
        readQueue.cancelAll(error);
        writeQueue.cancelAll(error);
    }
}

