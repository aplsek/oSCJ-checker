// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/Selector.java,v 1.3 2004/10/09 21:43:04 pizlofj Exp $

package ovm.services.io.async;

/**
 * Represents a set of descriptor that can essentially be select()'ed on.
 * <p>
 * The special contract for this class is that you cannot manipulate the
 * set (via calls to
 * <code>MultiSelectableIODescriptor.addToSelector()</code>
 * or
 * <code>MultiSelectableIODescriptor.removeFromSelector()</code>)
 * after you have called <code>select()</code> but before you have either
 * called <code>cancel()</code> or have had <code>AsyncCallback.read()</code>
 * called on you with an <code>AsyncFinializer</code> whos <code>finish()</code>
 * method returned <code>true</code>.
 * @author Filip Pizlo
 */
public interface Selector {
    
    public AsyncHandle select(AsyncCallback cback);
    
    public void cancel();
    
    public static interface SelectFinalizer extends AsyncFinalizer {
        public int getNumAwoken();
        public MultiSelectableIODescriptor getAwoken(int index);
    }
    
}

