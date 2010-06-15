package ovm.core;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.util.CommandLine;
/**
 * General stuff relevant to the whole OVM.
 * Before you start complaining, this is a stage of refactoring.
 * Ultimately this would be a OVM-wide equivalent of JavaVirtualMachine
 **/
public class Executive extends OVMBase {
    
    public interface Interface {

	/**
	 * OVM entry point.
	 **/
	void startup();

	void shutdown();

	void shutdown(int reason);

	void panic(String message);

	void panicOnException(Throwable t, String message);
        
        void panicOnErrno(String message, int errno);

	CommandLine getCommandLine();
	
    }
    // The pragma ensures this method body cannot be live at run time, so
    // the reference to class.getName() is safe; also the rewriting of the
    // method call will be no less efficient than a reference to a private
    // static final field.
    private static final Interface instance() throws PragmaStitchSingleton {
	return (Interface)
	      	InvisibleStitcher.singletonFor(Interface.class.getName());
    }
    
    private Executive() {} 

    public static void shutdown() {
	instance().shutdown();
    }

    public static void shutdown(int reason) {
	instance().shutdown(reason);
    }

    /**
     * @return nothing really, doesn't return but you can throw the result
     *         to document unreachability.
     **/
    public static Error panic(String message) {
	instance().panic(message);
	return null;
    }

    /**
     * stop execution because we executed something we shouldn't.
     * @return nothing really, doesn't return but you can throw the result
     *         to document unreachability.
     **/
    public static Error unreachable() {
	return panic("Should not reach");
    }

    /**
     * @return nothing really, doesn't return but you can throw the result
     *         to document unreachability.
     **/
    
    public static Error panicOnException(Throwable t, String message) {
	instance().panicOnException(t, message);
	return null;
    }
    
    public static Error panicOnErrno(String message, int errno) {
        instance().panicOnErrno(message,errno);
        return null;
    }

    /**
     * @return nothing really, doesn't return but you can throw the result
     *         to document unreachability.
     **/
    
    public static Error panicOnException(Throwable t) {
	return panicOnException(t, "");
    }

    /**
     * The main entry point for the OVM executive domain.
     * <p>This method performs all the necessary initialization of the OVM
     * as defined by the OVM configuration that has been built.
     * Typically, this would include initialising and starting all of the
     * OVM {@link ovm.services.ServiceInstance services} that need to be set up
     * upon start-up. The start-up protocol for such services is that all
     * services are initialized before any services are started.
     */
    public static void startup() {
	instance().startup();
    }

    public static CommandLine getCommandLine() {
	return instance().getCommandLine();
    }

}
