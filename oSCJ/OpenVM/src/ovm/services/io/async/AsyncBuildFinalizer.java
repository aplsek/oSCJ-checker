// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/AsyncBuildFinalizer.java,v 1.2 2004/02/20 08:48:30 jthomas Exp $

package ovm.services.io.async;

/**
 * When <code>ready()</code> gets called after a request that
 * would build an <code>IODescriptor</code> and finish()
 * returns <code>true</code> and <code>getError()</code> returns
 * null, you can cast the finalizer to this interface and
 * call <code>getNewDescriptor</code> to retrieve your descriptor.
 * @author Filip Pizlo
 */
public interface AsyncBuildFinalizer extends AsyncFinalizer {
    
    public IODescriptor getNewDescriptor();
    
}

