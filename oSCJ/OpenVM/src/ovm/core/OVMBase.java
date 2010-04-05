package ovm.core;

import java.io.Serializable;

import ovm.core.domain.Oop;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.VM_Address;
import ovm.util.OVMError;
import ovm.util.logging.Logger;
import s3.util.PragmaTransformCallsiteIR;

/**
 * Classes part of the image
 * usually extend this class. In previous versions of OVM, we used to
 * require that all classes in the image must extend OVMBase. This is no
 * longer the case.
 *
 * @author Vitek, Grothoff
 **/
public class OVMBase  implements Serializable {

    public static boolean dbg_mode_on = true; // This mode should be false
    // whenever this file is committed to CVS to avoid polluting output.

    public OVMBase() { }

    public static boolean isBuildTime()
	throws PragmaTransformCallsiteIR.Return0
    {
	return true;
    }

    public static final Oop asOop(Object o) {
	return VM_Address.fromObject(o).asOop();
    }

    /** Print the debugging string to the global logger terminated by a newline
     * @param s the debugging string to print
     **/
    public static void d(String s){  Logger.global.severe(s);  }

    /** Print the debugging string to the global logger
     * @param s the debugging string to print
     **/
    public static void p(String s){ Logger.global.severe(s);   }

    /**Print the debugging string to the global logger terminated by a newline
     * @param s the debugging string to print
     **/
    public static void pln(String s){ Logger.global.severe(s);   }

    /**Print directly using low level IO.
     */
    public static void bpln(String s) {
	BasicIO.bpln(s);
    }
    
    /**
     * Return a new {@link ovm.util.OVMError OVMError} given an error.
     **/
    public static OVMError failure(Throwable err) {return new OVMError(err); }

    /**
     * Return a new {@link ovm.util.OVMError OVMError} given an error
     * message.
     **/
    public static OVMError failure(String err) {return new OVMError(err); }

    /**
     * Throw a new {@link ovm.util.OVMError OVMError} given an error message.
     **/
    public static void fail(String message) { throw new OVMError(message); }

    /**
     * Throw an {@link OVMError} given some other throwable that has
     * caused the error.
     * @param err the <code>Throwable</code> related to this error
     **/
     public static void fail(Throwable err) { throw new OVMError(err); }

    /**
     * Throw an {@link ovm.util.OVMError.Unimplemented OVMError.Unimplemented}
     * with a message.
     * @param message error message to associated with the error
     **/
    public static void failUnimplemented(String message)
      { throw new OVMError.Unimplemented(message); }
} // end of OVMBase
