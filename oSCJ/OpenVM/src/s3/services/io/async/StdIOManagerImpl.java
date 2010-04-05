
package s3.services.io.async;

import ovm.core.Executive;
import ovm.services.io.async.IODescriptor;
import ovm.services.io.async.IOException;
import ovm.services.io.async.StdIOManager;
import s3.util.PragmaAtomic;

/**
 *
 * @author Filip Pizlo
 */
public class StdIOManagerImpl
    extends AsyncIOManagerBase
    implements StdIOManager {

    
    
    private static StdIOManagerImpl instance_ = new StdIOManagerImpl();
    public static StdIOManager getInstance() {
        return instance_;
    }
    
    private IODescriptor stdin_;
    private IODescriptor stdout_;
    private IODescriptor stderr_;
    
    public void init() {
        super.init();
    }
    
    public IODescriptor getStdIn() throws IOException, PragmaAtomic {
	if (stdin_==null) {
	    try {
		stdin_=wrapifier.wrapNow(0);
	    } catch (IOException e) {
		throw Executive.panicOnException(e,"Could not wrap stdin");
	    }
	}
        return stdin_;
    }
    
    public IODescriptor getStdOut() throws IOException, PragmaAtomic  {
	if (stdout_==null) {
	    try {
		stdout_=wrapifier.wrapNow(1);
	    } catch (IOException e) {
		throw Executive.panicOnException(e,"Could not wrap stdout");
	    }
	}
        return stdout_;
    }
    
    public IODescriptor getStdErr() throws IOException, PragmaAtomic  {
	if (stderr_==null) {
	    try {
		stderr_=wrapifier.wrapNow(2);
	    } catch (IOException e) {
		throw Executive.panicOnException(e,"Could not wrap stderr");
	    }
	}
        return stderr_;
    }
    
}

