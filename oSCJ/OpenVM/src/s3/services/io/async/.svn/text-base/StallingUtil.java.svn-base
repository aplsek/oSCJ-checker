package s3.services.io.async;

import ovm.services.io.async.*;

/**
 * Helpful stuff if you're implementing an async function in a stalling
 * manner.
 *
 * @author Filip Pizlo
 */
class StallingUtil {
    static AsyncHandle asyncHandle=new AsyncHandle(){
	    public boolean canCancelQuickly() {
		return false; /* this has no meaning since the operation is
				 already complete. */
	    }
	    public void cancel(IOException error) {
		/* do nothing since the operation is already complete. */
	    }
	};
}

