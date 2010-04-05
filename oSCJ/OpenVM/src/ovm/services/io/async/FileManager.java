// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/FileManager.java,v 1.5 2004/10/09 21:43:04 pizlofj Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public interface FileManager extends ovm.services.ServiceInstance {
    
    /**
     * Open or create a file.  This maps to the open() syscall except that
     * it is asynchronous.  The finalizer given to the AsyncCallback is
     * guaranteed to be an AsyncBuildFinalizer.
     * @param filename a null-terminated string containing the filename
     */
    public AsyncHandle open(byte[] filename,
			    int flags,
			    int mode,
			    AsyncCallback cback);
    
    /**
     * Create a new and unique temporary file with a name that has the given
     * template.  The finalizer given to the AsyncCallback is
     * guaranteed to be an AsyncBuildFinalizer.
     * @param template the template that is used to produce the filename.  this
     *		       template will be modified so that upon successful completion,
     *                 it will contain the actual filename.
     */
    public AsyncHandle mkstemp(byte[] template,
			       AsyncCallback cback);
    
    // TODO: this should contain methods like stat, opendir, and unlink.  and
    // they should all be asynchronous.
    
}

