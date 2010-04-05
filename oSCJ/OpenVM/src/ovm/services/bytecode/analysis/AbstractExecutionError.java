package ovm.services.bytecode.analysis;

import ovm.util.OVMError;

public class AbstractExecutionError
    extends OVMError {

    public AbstractExecutionError(String message) {
	super(message);
    }
    public AbstractExecutionError(String message,
				  Throwable cause) {
	super(message, cause);
    }
    
    public AbstractExecutionError(Throwable cause) {
	super(cause);
    }
}
