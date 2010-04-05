// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/SelectableIODescriptor.java,v 1.4 2004/09/02 00:42:10 pizlofj Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public interface SelectableIODescriptor extends IODescriptor {
    
    public boolean readyForRead();
    public boolean readyForWrite();
    public boolean readyForExcept();
    
}

