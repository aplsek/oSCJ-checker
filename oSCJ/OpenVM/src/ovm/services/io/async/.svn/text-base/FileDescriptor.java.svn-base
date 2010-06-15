// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/FileDescriptor.java,v 1.4 2004/10/09 21:43:04 pizlofj Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public interface FileDescriptor extends RandomRWIODescriptor {   

    public int trylock(long start,
		       long len,
		       boolean shared);

    public int unlock(long start,
		      long len);
 
    public AsyncHandle lock(long start,
			    long len,
			    boolean shared,
			    AsyncCallback ac);
 
    public AsyncHandle truncate(long size,
				AsyncCallback cback);
    
    /**
     * Cause the underlying OS to flush its buffers for this file.  If implemented,
     * this operation may block the whole VM.
     */
    public void bufferSyncNow() throws IOException;
}

