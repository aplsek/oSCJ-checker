package s3.util;

import java.util.Hashtable;

import ovm.core.repository.Selector;
import ovm.util.Location;

/**
 * Location that specifically singles out one of the arguments of a
 * method.  Note that the Selector is ALWAYS LBound.
 *
 * @author Christian Grothoff
 **/
public class MethodArgumentSelector 
    extends Location {

    private Selector.Method sel;

    /**
     * Index of the argument that this selector refers to.
     * 0 is the first argument, Descriptor.getArgumentCount()-1
     * is the last argument and for an instance method,
     * Descriptor.getArgumentCount() is the 'this' pointer.
     **/
    final int argIndex;

    private MethodArgumentSelector(Selector.Method sel,
				   int argIndex) {
	super(sel.toString() + "@" + argIndex);
	this.sel = sel;
	this.argIndex = argIndex;
    } 

    public boolean isReceiver() {
	return argIndex == sel.getDescriptor().getArgumentCount();
    }

    public Selector.Method getSelector() {
	return sel;
    }

    public int getArgumentIndex() {
	return argIndex;
    }
    
    public String toString() {
	return getDescription();
    }

    public boolean equals(Object o) {
	if (o == null)
	    return false;
	if (o == this)
	    return true;
	try {
	    MethodArgumentSelector mas 
		= (MethodArgumentSelector) o;
	    return ( (mas.sel == this.sel) &&
		     (argIndex == mas.argIndex) );
	} catch (ClassCastException cce) {
	    return false;
	}
    }

    public int hashCode() {
	return sel.hashCode() + argIndex;
    }

    public final static class Factory {

	Hashtable interned;
	
	public Factory() {
	    this.interned = new Hashtable();
	}

	public MethodArgumentSelector makeMAS(Selector.Method sel,
					      int argIndex) {
	    MethodArgumentSelector mas 
		= new MethodArgumentSelector(sel.makeLBound(), 
					     argIndex);
	    if (interned.get(mas) != null)
		return (MethodArgumentSelector) interned.get(mas);
	    else {
		interned.put(mas, mas);
		return mas;
	    }
	}

    } // end of MethodArgumentSelector.Factory

    private final static Factory fac = new Factory();

    public static MethodArgumentSelector make
	(Selector.Method sel,
	 int argIndex) {
	return fac.makeMAS(sel, argIndex);
    }

} // end of MethodArgumentSelector
