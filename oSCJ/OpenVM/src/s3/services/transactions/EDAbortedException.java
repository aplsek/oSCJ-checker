package s3.services.transactions;
import ovm.util.OVMRuntimeException;

public class EDAbortedException extends OVMRuntimeException {
    // FIXME: replicate fields of RuntimeException.
    // Use long rather than int for 64-bit saftey.  Use long rather than
    // pointer types to save memory.
    private long detailMessage;
    private long cause;
    private long code;
    private long pc;
    private long trace;

    
    public EDAbortedException() {
	// FIXME: This is an ugly, fragile workaround.  Since j2c makes this class into
	// a subclass of Object, virtual method dispatch doesn't work.
	// The only time that virtual method invocation might happen
	// is when Throwable checks to see if it is supposed to 
	// fill in a stack trace.  This is a sentinel value to tell it
	// not to fill in the stack trace.
	// Of course, if Throwable is changed to invoke another virtual method,
	// this won't work.
	// The correct solution would be for j2c to make this a subclass of Object
	// AND keep the correct VTable, but that seems like a lot of work...
	super(ovm.core.execution.RuntimeExports.dontFillStackTrace);
    }
    
}
