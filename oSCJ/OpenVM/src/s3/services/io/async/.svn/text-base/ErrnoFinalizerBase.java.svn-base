// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/ErrnoFinalizerBase.java,v 1.6 2004/04/02 16:07:58 pizlofj Exp $

package s3.services.io.async;

import ovm.core.execution.NativeConstants;
import ovm.services.io.async.AsyncFinalizer;
import ovm.services.io.async.IOException;
import ovm.services.threads.UserLevelThreadManager;

/**
 *
 * @author Filip Pizlo
 */
abstract class ErrnoFinalizerBase implements AsyncFinalizer {
    
    private IOException error_=null;
    protected UserLevelThreadManager tm;
    
    public ErrnoFinalizerBase(UserLevelThreadManager tm) {
        this.tm=tm;
    }
    
    protected boolean checkRepeat(int res,int errno) {
        if (res<0 && (errno==NativeConstants.EWOULDBLOCK ||
                      errno==NativeConstants.EAGAIN ||
                      errno==NativeConstants.EINTR)) {
            return true;
        }
        check(res,errno);
        return false;
    }
    
    protected void setErrno(int errno) {
        error_=IOException.System.make(errno);
    }
    
    protected boolean check(int res,int errno) {
        if (res<0) {
            setErrno(errno);
            return false;
        }
        return true;
    }
    
    protected boolean check(long res,int errno) {
        if (res<0) {
            setErrno(errno);
            return false;
        }
        return true;
    }
    
    protected void setError(IOException error) {
        this.error_=error;
    }
    
    public IOException getError() {
        return error_;
    }
    
}

