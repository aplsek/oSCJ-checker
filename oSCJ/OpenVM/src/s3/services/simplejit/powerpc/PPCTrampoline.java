package s3.services.simplejit.powerpc;

import ovm.util.FixedByteBuffer;
import s3.services.simplejit.StackLayout;
import s3.services.simplejit.SimpleJITTrampoline;
import ovm.core.services.memory.VM_Address;
import ovm.core.domain.Code;

public class PPCTrampoline extends SimpleJITTrampoline.Impl
	implements PPCConstants 
{
    public VM_Address compileAndRestart(Code meth, Object recv) {
	PPCAssembler xasm = new PPCAssembler(codeBuf, 0, 0);
	VM_Address ret = xasm.getAbsolutePC();
	StackLayout stackLayout = new StackLayoutImpl(0, 0, 0);
        
	int nativeFrameSize = stackLayout.getNativeFrameSize();
	xasm.emit_mflr(R0);
	xasm.emit_stw(R0, SP, stackLayout.getReturnAddressOffset());
	if (nativeFrameSize < -stackLayout.getInitialOperandStackPointerOffset()) {
            throw new Error("Initial operand stack pointer is below the stack "
			    + "pointer: " + nativeFrameSize
			    + ", initialOperandStackPointer " +
			    stackLayout.getInitialOperandStackPointerOffset());
            
        }
        if ((nativeFrameSize >> 15) != 0) {
            throw new Error("nativeFrameSize is too large " + nativeFrameSize);
        }
        xasm.emit_stwu(SP, SP, -nativeFrameSize);

        
        
        // Save all argument registers
	// We have to save R3 even though we will replace it later,
	// otherwise simplejit's stack-walking code will choke on this
	// frame.  (In the future, simplejit stack-walking code will
	// never see this frame, but that is another story.)
        for(int r = R3; r <= R10; ++r) {
	    xasm.emit_stw(r, SP, stackLayout.getNativeFrameSize() 
			  + stackLayout.getGeneralRegisterOffset(r));
        }
        for(int r = F1; r <= F13; ++r) {
            xasm.emit_stfd(r, SP, stackLayout.getNativeFrameSize()
			   + stackLayout.getFPRegisterOffset(r));
        }
        
        // Call the compiler
        // R2 = CSA.compile(...)
        xasm.emit_mr(R5, R3); // SimpleJITCode as the 1st argument
	xasm.emit_li32(R3, VM_Address.fromObject(meth).asInt());
	xasm.emit_li32(R4, VM_Address.fromObject(recv).asInt());
	xasm.emit_bl(meth.getForeignEntry());
        xasm.emit_mr(R2, R3); // R3 = return value (new code object)
	// R2 = foreignEntry
	xasm.emit_lwz(R2, R2, precomputed.offset_code_in_cf);
        
        // Restore all argument registers
        for(int r = R4; r <= R10; ++r) {
	    xasm.emit_lwz(r, SP, stackLayout.getNativeFrameSize() 
			  + stackLayout.getGeneralRegisterOffset(r));
        }
        for(int r = F1; r <= F13; ++r) {
            xasm.emit_lfd(r, SP, stackLayout.getNativeFrameSize()
			  + stackLayout.getFPRegisterOffset(r));
        }        
        
        //      jump to the compiled code
        xasm.emit_lwz(R0, SP, stackLayout.getReturnAddressOffset()
		      + stackLayout.getNativeFrameSize());
        xasm.emit_mtlr(R0);
        xasm.emit_addi(SP, SP, stackLayout.getNativeFrameSize());
	xasm.emit_mtctr(R2);
	xasm.emit_bctr();
	return ret;
    }
}
