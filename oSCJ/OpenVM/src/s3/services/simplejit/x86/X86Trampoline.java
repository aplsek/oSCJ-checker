package s3.services.simplejit.x86;

import s3.services.simplejit.CompilerVMInterface;
import s3.services.simplejit.StackLayout;
import s3.services.simplejit.SimpleJITTrampoline;
import ovm.core.domain.Code;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.io.BasicIO;

public class X86Trampoline extends SimpleJITTrampoline.Impl
    implements X86Constants
{
    public VM_Address compileAndRestart(Code code, Object recv) {
	X86Assembler xasm = new X86Assembler(codeBuf, 0);
	VM_Address ret = xasm.getAbsolutePC();

	if (false) {
	    BasicIO.out.println("Building trampoline for " + code);
	    BasicIO.out.println("Trampoline address: " + ret.toString());
	    BasicIO.out.println("Code address: "
				+ code.getForeignEntry().toString());
	}
	xasm.pushR(R_EBP);
	xasm.movRR(R_ESP, R_EBP);
	// preserve 16-byte alignment:
	// ret + callerbp + 3 args = 20 bytes
	xasm.subIR(12, R_ESP);

        // Call the compiler
	xasm.movMR(R_EBP, 8, R_EAX);
	xasm.pushR(R_EAX); // the Code to be compiled
	xasm.pushI32(VM_Address.fromObject(recv).asInt());
	xasm.pushI32(VM_Address.fromObject(code).asInt());
	xasm.call(code.getForeignEntry());
	// EAX = return value (Code object to call).  So, we need to
	// update the code argument, restore the stack, and jump
	// through the code's foreignEntry
	xasm.movRM(R_EAX, R_EBP, 8);
	xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_EAX);

	// restore ESP
	xasm.addIR(24, R_ESP);

	// and tail-call the compiled code
	xasm.popR(R_EBP);
	xasm.jmpAbsR(R_EAX);
	return ret;
    }
}
