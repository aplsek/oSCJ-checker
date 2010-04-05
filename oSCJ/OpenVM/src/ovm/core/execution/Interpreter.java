package ovm.core.execution;
import ovm.core.domain.Code;
import ovm.core.domain.Oop;
import ovm.core.domain.WildcardException;
import s3.services.transactions.PragmaPARSafe;

/**
 * This class contains methods that are translated by the 
 * ImageRewriter to unsafe bytecode sequences. It is used
 * to make special opcodes accessible to the VM core code.
 * <p>
 * In order for a method to work, it must have a mapping in the
 * {@link s3.services.bytecode.ovmify.NativeCallGenerator NativeCallGenerator} 
 * and the interpreter must understand the opcodes that are defined by the
 * mapping.
 * 
 * @author Jan Vitek
 * @author Christian Grothoff
 * @author James Liang
 */
final public class Interpreter {

    /**
     * This is a native class should not be instantiated.
     **/
    private Interpreter(){}



    /* ****************** STACK MANIPULATION ************** */
    /*           see also: exception handling ...           */

    // add methods to match requirements from Actvation_.java
    // here!

    /* It may make sense to leave these methods here, or perhaps these
     * should be moved Code.
     */
    native static int invokeInteger(Code targetCode, InvocationMessage invoc) throws WildcardException, PragmaPARSafe;
    native static long invokeLong(Code targetCode, InvocationMessage invoc) throws WildcardException, PragmaPARSafe;
    native static float invokeFloat(Code targetCode, InvocationMessage invoc) throws WildcardException, PragmaPARSafe;
    native static double invokeDouble(Code targetCode, InvocationMessage invoc) throws WildcardException, PragmaPARSafe;
    native static void invokeVoid(Code targetCode, InvocationMessage invoc) throws WildcardException, PragmaPARSafe;
    native static Oop invokeOop(Code targetCode, InvocationMessage invoc) throws WildcardException, PragmaPARSafe;

    /* Low-level threading interface */
    /**
     * @param interpreterContextHandle the magic cookie from Context that
     * corresponds to something in the bowels of the interpreter that is
     * needed when manipulating Activations. (KP: shouldn't this be
     * a VM_Word or VM_Address maybe? ) 
     * @param rcf The code fragment this Activation will be executing
     * @param invocation the arguments to the method.
     **/
    native static int makeActivation(int interpreterContextHandle, Code rcf, 
                                     InvocationMessage invocation);
    public native static Context getContext(int processorHandle) throws s3.services.transactions.PragmaPARSafe;
    native static int newNativeContext(Context ctx);
    native static void destroyNativeContext(int nativeCtx);
    public native static void run(int processorHandle, int interpreterContextHandle);

    /* This methods is only used by s3.core.services.interpreter.
     * Currently, they must be implemented as magic instructions
     * inside run_interpreter.
    native static void cutToActivation(int f, int a);
     */


    /* These methods are interpreter-specific, but are not encapulated
     * in s3.core.services.interpreter.  They provide the so-called
     * debugging supprot
     */

    /**
     * start printing executed instructions on stderr. Only works
     * with debugging enabled.
     * @param printStacks <code>true</code> if you want stack traces also
     **/
    public static native void startTracing(boolean printStacks);

    public static native void stopTracing();
    
    public static native void beginOverrideTracing();
    
    public static native void endOverrideTracing();

    /**
     * A well defined place to stop the C interpreter, or simplejit.
     **/
    public static native void breakpoint(Object toInspect);

} // End of Interpreter
