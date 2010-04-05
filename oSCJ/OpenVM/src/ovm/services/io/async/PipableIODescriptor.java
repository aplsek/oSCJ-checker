// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/PipableIODescriptor.java,v 1.2 2004/02/20 08:48:30 jthomas Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public interface PipableIODescriptor extends RWIODescriptor {
    
    public PipableIODescriptor getPeer();
    
}

