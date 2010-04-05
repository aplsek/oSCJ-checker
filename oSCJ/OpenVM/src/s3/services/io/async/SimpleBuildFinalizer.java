// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/SimpleBuildFinalizer.java,v 1.3 2004/04/02 16:07:58 pizlofj Exp $

package s3.services.io.async;

import ovm.services.io.async.*;

/**
 *
 * @author Filip Pizlo
 */
class SimpleBuildFinalizer
    extends AsyncFinalizer.Success
    implements AsyncBuildFinalizer {
    
    private IODescriptor newDescriptor_;
    
    public SimpleBuildFinalizer(IODescriptor newDescriptor) {
        this.newDescriptor_ = newDescriptor;
    }
    
    public IODescriptor getNewDescriptor() {
        return newDescriptor_;
    }
    
}

