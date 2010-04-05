
package test.runtime;

import s3.services.threads.JLThread;
import test.common.TestSuite;

/**
 *
 * @author Filip Pizlo
 */
public class TestTcpSocketCreate extends TestSyncBase {
    boolean doThrow;
    
    public TestTcpSocketCreate(long disabled) {
	super("TCP Socket Creation");
	doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
    }
    
    protected boolean needsIsolation() { return true; }
    
    public static final int ITERATIONS = 5;
    
    public void run() {
        if (dispatcher == null ||
            !(dispatcher.getCurrentThread() instanceof JLThread)) {
            p(" SKIPPED: not working with JLThreads");
            return;
        }
        if (!doThrow) {
            p(" SKIPPED: requires exceptions");
            return;
        }
	
	for (int i=0;
	     i<ITERATIONS;
	     ++i) {
	    TcpSocketUtil.createAndCloseTcpSocket(this,false,false);
	    TcpSocketUtil.createAndCloseTcpSocket(this,true,false);
	    TcpSocketUtil.createAndCloseTcpSocket(this,false,true);
	    TcpSocketUtil.createAndCloseTcpSocket(this,true,true);
	}
    }
}

