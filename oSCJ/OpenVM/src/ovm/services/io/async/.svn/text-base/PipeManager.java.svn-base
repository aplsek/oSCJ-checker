// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/PipeManager.java,v 1.2 2004/02/20 08:48:30 jthomas Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public interface PipeManager extends ovm.services.ServiceInstance {
    
    /**
     * @param pipe an array of two.  upon success, the first element will
     *             be a readable IODescriptor and the second element will
     *             be a writable IODescriptor.  It is also possible for
     *             both to be readable and writable, if the system supports
     *             full-duplex pipes.
     */
    public void pipe(IODescriptor[] pipe)
        throws IOException;
    
}

