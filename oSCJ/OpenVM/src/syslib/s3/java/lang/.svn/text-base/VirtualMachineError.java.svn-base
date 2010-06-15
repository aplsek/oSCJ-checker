package java.lang;


public class VirtualMachineError extends Error {

    /** 
     * Instantiate this VirtualMachineError with an empty message and a
     * filled in stack trace
     */
    public VirtualMachineError() {
	super();
    }
    
    /**
     * Instantiate this VirtualMachineError with the given message and a
     * filled in stack trace
     * @param message the message to associate with the VirtualMachineError.
     */
    public VirtualMachineError(String message) {
        super(message);
    }

    /**
     * Instantiate this VirtualMachineError with an empty message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected VirtualMachineError(boolean trace) {
        super(trace);
    }

    /**
     * Instantiate this VirtualMachineError with the given message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param message the message to associate with the VirtualMachineError.
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected VirtualMachineError(String message, boolean trace) {
        super(message, trace);
    }
}
