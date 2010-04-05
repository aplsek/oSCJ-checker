package ovm.core.execution;
import ovm.core.OVMBase;
import ovm.util.OVMError;
import ovm.core.services.memory.MemoryManager;
import s3.util.PragmaInline;
import ovm.core.services.memory.PragmaNoBarriers;
/**
 * A Processor is the OVM/Java representation of the global state of an
 * execution engine.<br> Typically, OVM will have as many processors as
 * there are physical CPUs in the system, but a processor could also
 * correspond to a 'virtual' CPU simulated by the OS, such as a pthread.
 * Since OVM is not an OS, a Processor can be in a state where it is
 * not actually executing code.  Currently, OVM supports only a single
 * Processor, later, we will need some more support from the interpreter to
 * get processor IDs. 
 * <p>All methods that disable rescheduling or which are only called with
 * rescheduling disabled, declare PragmaInline (which also disables the
 * pollcheck). Barriers are elided for data accesses as this data is allocated
 * at image time.
 *
 * @author Grothoff, Flack
 */ 
public class Processor extends OVMBase {

    public static Processor[] processors_;

    /**
     * Get a list of available Processors from the System.
     **/
    public static Processor[] getProcessors() throws PragmaInline,
                                                     PragmaNoBarriers {
	return processors_;
    }

    /**
     * Get the current Processor from the System.
     **/
    public static Processor getCurrentProcessor() throws PragmaInline,
                                                         PragmaNoBarriers {
	return processors_[0];
    }

    /**
     * The ID of this processor.
     **/
    private final int processor_;

    /**
     * Create a new processor.
     **/
    public Processor() {
	if (processors_ != null) 
	    throw new OVMError.Unimplemented("Single process implementation");	
	processor_ = 0;
	processors_ = new Processor[] {this};
    }

    /**
     * Cause this processor to swith to the given Context.  Returns if
     * somebody invokes run(this.getContext()).
     * @param ctx The new Context to run. 
     **/
    public void run(Context ctx) throws PragmaInline {
	Interpreter.run(processor_, ctx.nativeContextHandle_);
        // when run returns it is the current context that has been switched to
        // not ctx. ctx was who the current context switched to when it was
        // switched out
	MemoryManager.the().observeContextSwitch(Interpreter.getContext(processor_));
    }

    /**
     * Get the Context that this Processor is 'currently' running.
     **/
    public Context getContext() throws PragmaInline {
	return Interpreter.getContext(processor_);
    }
    
    /**
     * Destroy this processor (free associated resources, stop eventually
     * running activation).
     **/
    public void destroy() {
	throw new OVMError.Unimplemented();
    }
}
