// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/RandomRWIODescriptor.java,v 1.2 2004/10/09 21:43:04 pizlofj Exp $

package ovm.services.io.async;

import ovm.core.services.memory.*;

/**
 *
 * @author Filip Pizlo
 */
public interface RandomRWIODescriptor
    extends RWIODescriptor,
	    SeekableIODescriptor {
    
    public AsyncHandle pread(AsyncMemoryCallback data,
			     int maxBytes,
			     long offset,
			     AsyncCallback cback);
    
    public int tryPreadNow(VM_Address address,
                           int maxBytes,
                           long offset) throws IOException;
    
    public AsyncHandle pwrite(AsyncMemoryCallback data,
			      int maxBytes,
			      long offset,
			      AsyncCallback cback);
    
    public int tryPwriteNow(VM_Address address,
                            int maxBytes,
                            long offset) throws IOException;
}

