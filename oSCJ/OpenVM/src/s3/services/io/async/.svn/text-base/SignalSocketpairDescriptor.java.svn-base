// $Header: /p/sss/cvs/OpenVM/src/s3/services/io/async/SignalSocketpairDescriptor.java,v 1.4 2004/02/20 08:52:42 jthomas Exp $

package s3.services.io.async;

import ovm.services.io.async.*;
import ovm.services.io.signals.*;
import ovm.services.threads.*;

/**
 *
 * @author Filip Pizlo
 */
class SignalSocketpairDescriptor
    extends SignalSocketDescriptor
    implements PipableIODescriptor {
    
    private SignalSocketpairDescriptor peer_;
    
    SignalSocketpairDescriptor(UserLevelThreadManager tm,
                               IOSignalManager iosm,
                               int fd) {
        super(tm,iosm,fd);
    }
    
    void setPeer(SignalSocketpairDescriptor peer) {
        this.peer_=peer;
    }
    
    public PipableIODescriptor getPeer() {
        return peer_;
    }
    
    protected IODescriptor createMyselfWithFD(int newFd) {
        SignalSocketpairDescriptor ret=
            new SignalSocketpairDescriptor(tm,iosm,newFd);
        ret.setPeer(peer_);
        return ret;
    }
}

