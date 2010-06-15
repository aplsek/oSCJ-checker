package s3.services.j2c;
import ovm.core.execution.NativeInterface;
import ovm.core.domain.Oop;

/**
 * J2C builtin functions. j2cThrow isn't actually inlined...
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 */
public final class Inline implements NativeInterface {
    // 2nd argument space-seperated list of local variable names.
    // We make lvalues for these variables available in $0..$n
//     static public native void C(String C_code, String vars);

    // out, in, may refer to local variables, and will be replaced by
    // the appropriate lvalues
//     static public native void asm(String asm_code, String out,
// 				  String in, String clobber);

    // Giant typecase leading to a throw expression whose thrown
    // objects static type is the runtime type of our throwable
    // pointer. 
    static public native void j2cThrow(Oop throwable);
}
