package s3.services.j2c;
import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.repository.*;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.io.BasicIO;
import ovm.core.domain.Code;
import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.WildcardException;
import ovm.services.bytecode.JVMConstants.Throwables;
import ovm.util.UnsafeAccess;
import s3.core.domain.S3Method;
import s3.core.domain.S3ByteCode;
import s3.util.PragmaTransformCallsiteIR.BCdead;

class J2cCodeFragment extends Code {
    // If true, report the method that receives a
    // NullPointerException/StackOverflowError trap.  Very useful on
    // Macs, where debugging signal handlers is virtually impossible
    private static final boolean TRACE_TRAPS = false;
    public static final Code.Kind KIND = new Code.Kind() { };
    
    static VM_Address[] ranges = new VM_Address[0];
    static J2cCodeFragment[] frags = new J2cCodeFragment[0];

    String cname;
    char rtype;			// simplify c++ code
    int index = -1;
    int startingLine;

    // Code fragments are allocated at boot time, and should not drag
    // ovm.core.services.format into the image
    J2cCodeFragment(S3Method m, int index)
	throws BCdead
    {
	super(m);
	this.cname = J2cFormat.format(m);
	this.index = index;
	S3ByteCode bc = m.getByteCode();
	startingLine = bc.getLineNumber(0);
	rtype = m.getSelector().getDescriptor().getType().getTypeTag();
    }

    VM_Address getEntryPoint() {
	return index == -1 ? VM_Address.fromObject(null) : ranges[index<<1];
    }

    public void bang(ovm.core.domain.Code c) {
	throw new Error("can not bang J2c code: not implemented yet");
    }

    public Kind getKind() { return KIND; }

    public int getLineNumber(int pc) {
	return startingLine;
    }

    private static void throwWildcardException(Oop ex) {
	Context ctx = Context.getCurrentContext();
	throw (WildcardException) ctx.makeWildcardException(ex);
    }

    private static void generateThrowable(int ex) {
       	Activation act = Context.getCurrentContext().getCurrentActivation();
	act = act.caller(act);
	Code code = act.getCode();
	if (TRACE_TRAPS) {
	    BasicIO.err.print("dispatch exception for ");
	    BasicIO.err.println(((J2cCodeFragment) code).cname);
	}
	Domain d = code.getMethod().getDeclaringType().getDomain();
	CoreServicesAccess csa = d.getCoreServicesAccess();
	csa.generateThrowable(ex, 0);
    }

    private static void assertAddressValid(VM_Address ptr) {
	MemoryManager.the().assertAddressValid(ptr);
    }
}
