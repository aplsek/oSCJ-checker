// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/IODescriptor.java,v 1.7 2004/10/09 21:43:04 pizlofj Exp $

package ovm.services.io.async;

/**
 * An object-oriented portable representation of a file descriptor with
 * all otherwise blocking operations being asynchronous.
 * <p>
 * This interface is meant to be represent any object that can do IO,
 * hence it does not include even the <code>read()</code> or <code>write()</code>
 * syscalls.  If you are looking for those, then look at <code>RWIODescriptor</code>.
 * <p>
 * <code>IODescriptor</code> objects are created by an <code>IOFactory</code>, which
 * is an OVM service.  Once you are done with an <code>IODescriptor</code>, you can
 * <code>close()</code> it.  If the <code>IODescriptor> is also an instance of the
 * <code>CancelableIODescriptor</code> interface, this will cancel all pending async
 * operations by returning <code>IOException.Canceled</code>.
 * <p>
 * Typically, you would use an <code>IODescriptor</code> by casting it to one of its
 * subinterfaces, such as <code>RWIODescriptor</code> or
 * <code>SocketIODescriptor</code>.
 *
 * @author Filip Pizlo
 */
public interface IODescriptor {
    
    /** Returns <code>true</code> if the descriptor is open, <code>false</code>
     * otherwise.  A descriptor is open from the time of instantiation to the
     * time when <code>close()</code> is called.
     * @return <code>true</code> or <code>false</code>
     */
    public boolean isOpen();
    
    /**
     * Duplicate the <code>IODescriptor</code>.  It is not guaranteed that the
     * underlying file descriptor is duplicated.  The resource that this
     * <code>IODescriptor</code> refers to will remain open until <emph>both</emph>
     * the receiver and the returned object are closed via <code>close()</code>.
     * Note that a correct implementation of this method will simply increment
     * a reference count and return itself.
     */
    public IODescriptor dup() throws IOException;
    
    /**
     * Closes the IO descriptor.  This descriptor may not be used after it has
     * been closed.  Any attempt to use the descriptor after calling this method
     * will result in undefined behavior.  This method will automatically call
     * the <code>cancel()</code> method below before actually closing the
     * underlying resource.
     */
    public void close();
    
    /**
     * Cancels any pending IO operations and notifies any selectors if you're
     * waiting for one.  When an IO operation is canceled, the
     * <code>AsyncCallback.ready()</code> method is called with an
     * <code>AsyncFinalizer</code> that will return the error you pass in here.
     */
    public void cancel(IOException error);
}

