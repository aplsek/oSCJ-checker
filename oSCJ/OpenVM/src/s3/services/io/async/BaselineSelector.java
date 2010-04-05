// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/BaselineSelector.java,v 1.3 2004/10/09 21:43:04 pizlofj Exp $

package s3.services.io.async;

import ovm.services.io.async.*;
import ovm.core.execution.NativeConstants;

/**
 *
 * @author Filip Pizlo
 */
class BaselineSelector implements Selector {
    
    private int[] fds_=new int[NativeConstants.FD_SETSIZE];
    private MultiSelectableIODescriptor[] ios_=
        new MultiSelectableIODescriptor[NativeConstants.FD_SETSIZE];
    private int num_=0;
    
    private boolean selecting=false;
    
    public void addDescriptor(int fd,
                              MultiSelectableIODescriptor io) {
        if (selecting) {
            throw new ovm.util.OVMError.IllegalState(
                "BaselineSelector.addDescriptor() called while "+
                "select()ing");
        }
        
        fds_[num_]=fd;
        ios_[num_]=io;
        ++num_;
    }
    
    public void removeDescriptor(int fd,
                                 MultiSelectableIODescriptor io) {
        if (selecting) {
            throw new ovm.util.OVMError.IllegalState(
                "BaselineSelector.removeDescriptor() called while "+
                "select()ing");
        }
        
        for (int i=0;i<num_;++i) {
            if (fds_[i]==fd &&
                ios_[i]==io) {
                --num_;
                fds_[i]=fds_[num_];
                ios_[i]=ios_[num_];
                return;
            }
        }
        
        throw new ovm.util.OVMError(
            "fd="+fd+", io="+io+" not found in "+this);
    }
    
    public AsyncHandle select(AsyncCallback cback) {
        if (selecting) {
            throw new ovm.util.OVMError.IllegalState(
                "BaselineSelector.select() called when already "+
                "select()ing");
        }
        
        cback.ready(new AsyncFinalizer.Error(new IOException.Unimplemented()));
	return StallingUtil.asyncHandle;
    }
    
    public void cancel() {
        if (!selecting) {
            // there may be race conditions where someone calls
            // cancel() and has no guarantee that by the time we get
            // in here the select() would not have returned.  since
            // presumably the purpose of calling cancel() was probably not
            // so much to prevent select() from producing meaningful
            // results, but to just make it return so that its thread can
            // do other stuff.  so, the only FIXME with regards to this
            // issue is: better document cancel()'s semantics in the
            // Selector interface to indicate that sometimes, cancel()
            // will return successfully but select() will not return
            // IOException.Canceled but will instead return successfully.
            // the user is responsible for dealing with this.
            return;
        }
        
        
    }
    
}

