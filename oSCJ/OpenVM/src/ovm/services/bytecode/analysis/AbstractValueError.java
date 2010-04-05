package ovm.services.bytecode.analysis;


/**
 * @author Christian Grothoff
 **/
public class AbstractValueError 
    extends Error {
    public AbstractValueError() {
    }
    public AbstractValueError(String s) {
	super(s);
    }
}
