package s3.services.simplejit.powerpc;

import ovm.core.domain.ConstantResolvedInstanceFieldref;
import ovm.core.domain.ConstantResolvedInstanceMethodref;
import ovm.core.domain.ConstantResolvedInterfaceMethodref;
import ovm.core.domain.ConstantResolvedStaticFieldref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.LinkageException;
import ovm.core.repository.Descriptor;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.UnboundSelector;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionSet;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.Instruction.RETURN;
import ovm.util.OVMError;
import s3.core.domain.S3Field;
import s3.core.domain.S3Method;
import s3.services.simplejit.CodeGenContext;
import s3.services.simplejit.CodeGenerator;
import s3.services.simplejit.CompilerVMInterface;
import s3.services.simplejit.StackLayout;
import s3.services.simplejit.Assembler.Branch;

/**
 * @author Hiroshi Yamauchi
 **/
public class CodeGeneratorImpl 
    extends CodeGenerator 
    implements JVMConstants, 
               JVMConstants.InvokeSystemArguments, 
               PPCConstants {

    protected static int FAR_JUMP_THRESHOULD = 600;
    protected PPCAssembler xasm;
    private int opSP;

    public CodeGeneratorImpl(S3Method method,
                 InstructionSet is,
                 CompilerVMInterface compilerVMInterface,
                 Precomputed precomputed,
                 boolean debugPrintOn) {
        super(method, is, compilerVMInterface, debugPrintOn, precomputed);
    }

    protected void beforeBytecode() {
        //if (ENABLE_BYTECODE_MARKING)
        //xasm.breakpoint();
    }

    protected void afterBytecode() {
    }

    protected StackLayout makeStackLayout(int maxLocals,
                    int maxStack,
                    int argLength) {
        return new StackLayoutImpl(maxLocals,
                   maxStack,
                   argLength);
    }

    protected CodeGenContext makeCodeGenContext() {
        return new CodeGenContextImpl(bytecode.getBytes().length, method.getDeclaringType().getContext());
    }

    protected void prepareAssembler(CodeGenContext context) {
        opSP = R31;
        this.xasm = new PPCAssembler(context.getCodeBuffer(), 
				     CodeGenContext.globalCodeArray.position(),
				     opSP);
    }

    protected void debugPrint(String message) {
    }
    
    private void generateCSACall(int methodIndex, Descriptor.Method desc) {
        // System.err.println("generateCSACall() index = " + methodIndex);
        int offset_cf_in_csavtable = getArrayElementOffset(executiveDomain, OBJECT, methodIndex);
        int frame_size = stackLayout.getNativeFrameSize();

        xasm.emit_lwz(R30, SP, frame_size + stackLayout.getCodeFragmentOffset());
        xasm.emit_lwz(R4, R30, precomputed.offset_csa_in_cf);
        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R29, R4));
        xasm.emit_lwz(R28, R29, precomputed.offset_vtbl_in_bp);
        xasm.emit_lwz(R3, R28, offset_cf_in_csavtable);
        xasm.emit_lwz(R27, R3, precomputed.offset_code_in_cf);
        xasm.emit_mtctr(R27);
        saveVolatileRegisters();
        xasm.emit_bctr_lk();
        restoreVolatileRegisters();
        unloadReturnValue(desc.getType().getTypeTag());
    }
    
    private void generateCFunctionCall(String function) {
        int rtFuncIndex = compilerVMInterface.getRuntimeFunctionIndex(function);
        if (rtFuncIndex == -1)
            throw new OVMError.Unimplemented("Runtime function \"" + function + "\" Not Found");
        int offset_fp_in_table = rtFuncIndex * 4;

        xasm.emit_li32(R11, precomputed.runtimeFunctionTableHandle.asInt()); // R11 = table handle
        xasm.emit_lwz(R11, R11, 0); // R11 = table objectref
        xasm.emit_lwz(R11, R11, offset_fp_in_table); // R11 = function pointer
        xasm.emit_mtctr(R11);
        // opSP = R31 is non-volatile register so it does have to be saved and restored
        // when calling a C function
        xasm.emit_bctr_lk();
        xasm.emit_nop();
    }
    
    private void generateNullCheck(int receiver_reg) {
        if (OMIT_NULLPOINTER_CHECKS) return;
        xasm.emit_cmpi(CR7, false, receiver_reg, 0);
        Branch b0 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R6, 0);                                  // push meta
        xasm.emit_li32(R5, precomputed.nullPointerExceptionID); // push exception type 
        generateCSACall(precomputed.csa_generateThrowable_index, 
                precomputed.csa_generateThrowable_desc);
        xasm.setBranchTarget(b0);
    }

    private void generateNullCheckForMonitor(int receiver_reg) {
	if (OMIT_NULLPOINTER_CHECKS_MONITOR) return;
        xasm.emit_cmpi(CR7, false, receiver_reg, 0);
        Branch b0 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R6, 0);                                  // push meta
        xasm.emit_li32(R5, precomputed.nullPointerExceptionID); // push exception type 
        generateCSACall(precomputed.csa_generateThrowable_index, 
                precomputed.csa_generateThrowable_desc);
        xasm.setBranchTarget(b0);
    }
    
    private void generateDivisionByIntZeroCheck(int divisor_reg) {
        xasm.emit_cmpi(CR7, false, divisor_reg, 0);
        Branch b0 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R6, 0);                                  // push meta
        xasm.emit_li32(R5, precomputed.arithmeticExceptionID); // push exception type 
        generateCSACall(precomputed.csa_generateThrowable_index, 
                precomputed.csa_generateThrowable_desc);
        xasm.setBranchTarget(b0);
    }

    private void generateDivisionByLongZeroCheck(int divisor_h_reg,
            int divisor_l_reg) {
        xasm.emit_or(R6, divisor_h_reg, divisor_l_reg);
        xasm.emit_cmpi(CR7, false, R6, 0);
        Branch b0 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R6, 0);                                  // push meta
        xasm.emit_li32(R5, precomputed.arithmeticExceptionID); // push exception type 
        generateCSACall(precomputed.csa_generateThrowable_index, 
                precomputed.csa_generateThrowable_desc);
        xasm.setBranchTarget(b0);
    }
        
    private void generateArrayBoundCheck(int array_reg, int index_reg) {
        if (OMIT_ARRAYBOUND_CHECKS) return;
        switch(precomputed.tArrayLengthFieldSize) {
        case 2:
            xasm.emit_lhz(R6, array_reg, precomputed.tArrayLengthFieldOffset);
            break;
        case 4:
            xasm.emit_lwz(R6, array_reg, precomputed.tArrayLengthFieldOffset);
            break;
        default:
            throw new Error("Invalid array element size");
        }
        xasm.emit_cmpl(CR7, false, index_reg, R6);
        Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_LT);
        //generateCFunctionCall("hit_unrewritten_code");
        xasm.emit_li32(R6, 0);                                   // push meta
        xasm.emit_li32(R5, precomputed.arrayIndexOutOfBoundsExceptionID); // push exception type 
        generateCSACall(precomputed.csa_generateThrowable_index, 
                precomputed.csa_generateThrowable_desc);
        xasm.setBranchTarget(b0);
    }
    private void generateArrayStoreCheck(int array_reg, int elem_reg) {
        if (OMIT_ARRAYSTORE_CHECKS) return;

        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R5, array_reg));
        xasm.emit_lwz(R4, R5, precomputed.offset_componentbp_in_arraybp);
        xasm.emit_cmpi(CR7, false, elem_reg, 0);
        Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R3, elem_reg));
	xasm.emit_cmp(CR7, false, R3, R4);
	Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
        generateCFunctionCall("is_subtype_of");
        xasm.emit_cmpi(CR7, false, R3, 0);
        Branch b1 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);

        // check failed (throw exception)
        xasm.emit_li32(R6, 0);                                 // push meta
        xasm.emit_li32(R5, precomputed.arrayStoreExceptionID); // push exception type 
        generateCSACall(precomputed.csa_generateThrowable_index, 
                precomputed.csa_generateThrowable_desc);

	// blueprints are the same
	xasm.setBranchTarget(b2);

        // null is ok
        xasm.setBranchTarget(b0);

        // subtype test passed
        xasm.setBranchTarget(b1);
    }
    
    private void generateStackoverflowCheck() {
        if (OPTIMIZE_STACK_OVERFLOW_CHECK) {
            int currentContextIndex =
                compilerVMInterface
                .getRuntimeFunctionIndex("currentContext");
            if (currentContextIndex == -1)
                throw new OVMError();
            // R30 is live in generatePrologue
            xasm.emit_li32(R23, precomputed.runtimeFunctionTableHandle.asInt());
            xasm.emit_lwz(R29, R23, 0);
            xasm.emit_lwz(R28, R29, currentContextIndex * 4); 
            xasm.emit_lwz(R28, R28, 0); // R28 = current context
            xasm.emit_lwz(R27, R28, precomputed.offset_mtb_in_nc);
            xasm.emit_lwz(R26, R27, precomputed.offset_redzone_in_mtb);
            xasm.emit_cmpl(CR7, false, SP, R26);
            Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_GT);
            xasm.emit_lwz(R25, R28, precomputed.offset_pso_in_nc);
            xasm.emit_cmpi(CR7, false, R25, 0);
            Branch b1 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
            xasm.emit_li32(R24, 1);
            xasm.emit_stw(R24, R28, precomputed.offset_pso_in_nc);
            xasm.emit_li32(R6, 0);                                  // push meta
            xasm.emit_li32(R5, precomputed.stackOverflowErrorID); // push exception type 
            generateCSACall(precomputed.csa_generateThrowable_index, 
                    precomputed.csa_generateThrowable_desc);
            xasm.setBranchTarget(b0);
            xasm.setBranchTarget(b1);
        } else {
            generateCFunctionCall("check_stack_overflow");
            xasm.emit_cmpi(CR7, false, R3, 0);
            Branch b = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
            xasm.emit_li32(R6, 0);                                // push meta
            xasm.emit_li32(R5, precomputed.stackOverflowErrorID); // push exception type 
            generateCSACall(precomputed.csa_generateThrowable_index, 
                    precomputed.csa_generateThrowable_desc);
            xasm.setBranchTarget(b);
        }
    }
        
    protected void generatePrologue() {
        if (method.getArgumentLength() + 8 /*receiver+code*/ > StackLayoutImpl.PARAM_AREA_SIZE) {
            throw new Error("PARAM_AREA_SIZE is too small");
        }
        
        int nativeFrameSize = stackLayout.getNativeFrameSize();
        xasm.emit_mflr(R0);
        xasm.emit_stw(R0, SP, stackLayout.getReturnAddressOffset());
        if (nativeFrameSize < -stackLayout.getInitialOperandStackPointerOffset()) {
            throw new Error("Initial operand stack pointer is below the stack pointer : "
                       + " nativeFrameSize " + nativeFrameSize + ", initialOperandStackPointer " + 
                       stackLayout.getInitialOperandStackPointerOffset());
            
        }
        xasm.emit_addi(R31, SP, stackLayout.getInitialOperandStackPointerOffset());
        if ((nativeFrameSize >> 15) != 0) {
            throw new Error("nativeFrameSize is too large " + nativeFrameSize);
        }
        xasm.emit_stwu(SP, SP, -nativeFrameSize);

        unloadArguments(method.getSelector().getDescriptor());
        
        xasm.emit_mr(R30, R4);
        
        if (! OMIT_STACKOVERFLOW_CHECKS)
            generateStackoverflowCheck();
        
        if (isSynchronized) {
            xasm.emit_mr(R5, R30);
            generateCSACall(precomputed.csa_monitorEnter_index,
                    precomputed.csa_monitorEnter_desc);
        }
    }

    protected void generateEpilogue() {
    }

    public void visit(Instruction i) {
        throw new Error("Unimplemented instruction " + i);
        //generateCFunctionCall("hit_unrewritten_code");
    }

    public void visit(Instruction.NOP _) {}
    public void visit(Instruction.AFIAT _) {}
    public void visit(Instruction.PrimFiat _) {}
    public void visit(Instruction.WidePrimFiat _) {}

    public void visit(Instruction.POLLCHECK i) {
        if (OPTIMIZE_POLLCHECK) {
            int eventUnionIndex = compilerVMInterface
                    .getRuntimeFunctionIndex("eventUnion");
            if (eventUnionIndex == -1)
                throw new OVMError();
            // hand inlinig of eventPollcheck()
            xasm.emit_li32(R30, precomputed.runtimeFunctionTableHandle.asInt());
            xasm.emit_lwz(R29, R30, 0);
            xasm.emit_lwz(R28, R29, eventUnionIndex * 4);
            xasm.emit_lwz(R27, R28, 0);
            xasm.emit_cmpi(CR7, false, R27, 0);
            Branch b1 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
            // Event occurred
            xasm.emit_li32(R26, 0x00010001);
            xasm.emit_stw(R26, R28, 0);
            generateCSACall(precomputed.csa_pollingEventHook_index,
                    precomputed.csa_pollingEventHook_desc);
            xasm.setBranchTarget(b1);
        } else {
            generateCFunctionCall("eventPollcheck");
            xasm.emit_cmpi(CR7, false, R3, 0);
            Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
            generateCSACall(precomputed.csa_pollingEventHook_index,
                    precomputed.csa_pollingEventHook_desc);
            xasm.setBranchTarget(b1);
        }
    }
    
    public void visit(Instruction.ACONST_NULL i) {
        xasm.emit_li32(R30, 0);
        xasm.emit_push(R30);
    }
    
    // ICONST_X, BIPUSH, SIPUSH,
    public void visit(Instruction.IConstantLoad i) {
        xasm.emit_li32(R30, i.getValue(buf));
        xasm.emit_push(R30);
    }
    
    public void visit(Instruction.FConstantLoad f) {
        float val = f.getFValue(buf);
        int bits = Float.floatToIntBits(val);
        xasm.emit_li32(R30, bits);
        xasm.emit_push(R30);
    }
    
    public void visit(Instruction.LConstantLoad i) {
        long value = i.getLValue(buf);
        xasm.emit_li32(R30, (int)(value & 0xFFFFFFFFL));
        xasm.emit_li32(R29, (int)((value >> 32) & 0xFFFFFFFFL));
        xasm.emit_push(R30);
        xasm.emit_push(R29);
    }
    
    public void visit(Instruction.DConstantLoad i) {    
        double val = i.getDValue(buf);
        long bits = Double.doubleToLongBits(val);
        xasm.emit_li32(R30, (int)(bits & 0xFFFFFFFFL));
        xasm.emit_li32(R29, (int)((bits >> 32) & 0xFFFFFFFFL));
        xasm.emit_push(R30);
        xasm.emit_push(R29);
    }
    
    public void visit(Instruction.LocalRead i) {
        int operand = i.getLocalVariableOffset(buf);
        int localOffset = stackLayout.getLocalVariableNativeOffset(operand);
        int localOffsetFromSP = localOffset + stackLayout.getNativeFrameSize();
        if (i.stackOuts.length > 1) {
            // 8 byte operation
            xasm.emit_lwz(R30, SP, localOffsetFromSP + 4);
            xasm.emit_push(R30);
        }
        xasm.emit_lwz(R29, SP, localOffsetFromSP);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.LocalWrite i) {    
        int operand = i.getLocalVariableOffset(buf);
        int localOffset = stackLayout.getLocalVariableNativeOffset(operand);
        int localOffsetFromSP = localOffset + stackLayout.getNativeFrameSize();
        xasm.emit_pop(R30);
        xasm.emit_stw(R30, SP, localOffsetFromSP);
        if (i.stackIns.length > 1) {
            // 8 byte operation
            xasm.emit_pop(R29);
            xasm.emit_stw(R29, SP, localOffsetFromSP + 4);
        }
    }    
    
    public void visit(Instruction.LOAD_SHST_METHOD i) {
        throw new Error("LOAD_SHST_METHOD not implemented");
    }
    public void visit(Instruction.LOAD_SHST_FIELD i) {
        throw new Error("LOAD_SHST_FIELD not implemented");
    }
    
    public void visit(Instruction.LOAD_SHST_METHOD_QUICK i) {
        throw new Error("LOAD_SHST_METHOD_QUICK not implemented");
    }
    public void visit(Instruction.LOAD_SHST_FIELD_QUICK i) {
        throw new Error("LOAD_SHST_FIELD_QUICK not implemented");
    }
    
    public void visit(Instruction.IADD i) {    
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_add(R28, R29, R30);
        xasm.emit_push(R28);
    }
    
    public void visit(Instruction.LADD i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        xasm.emit_addc(R25, R27, R29);
        xasm.emit_adde(R26, R28, R30);
        xasm.emit_push(R25);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.FADD i) {
        xasm.emit_popfs(F31);
        xasm.emit_popfs(F30);
        xasm.emit_fadds(F29, F30, F31);
        xasm.emit_pushfs(F29);
    }

    public void visit(Instruction.DADD i) {   
        xasm.emit_popfd(F31);
        xasm.emit_popfd(F30);
        xasm.emit_fadd(F29, F30, F31);
        xasm.emit_pushfd(F29);
    }

    public void visit(Instruction.ISUB i) {  
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_sub(R28, R29, R30);
        xasm.emit_push(R28);
    }

    public void visit(Instruction.LSUB i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        xasm.emit_subc(R25, R27, R29);
        xasm.emit_sube(R26, R28, R30);
        xasm.emit_push(R25);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.FSUB i) {
        xasm.emit_popfs(F31);
        xasm.emit_popfs(F30);
        xasm.emit_fsubs(F29, F30, F31);
        xasm.emit_pushfs(F29);
    }

    public void visit(Instruction.DSUB i) {
        xasm.emit_popfd(F31);
        xasm.emit_popfd(F30);
        xasm.emit_fsub(F29, F30, F31);
        xasm.emit_pushfd(F29);
    }

    public void visit(Instruction.IMUL i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_mullw(R28, R30, R29);
        xasm.emit_push(R28);
    }

    public void visit(Instruction.LMUL i) {
        //           1  2
        //        x  3  4
        // ----------------
        //    h(2x4) l(2x4)
        //    l(1x4)
        //    l(2x3)
        // +  l(1x3)
        // ----------------
        //           l(2x4)
        xasm.emit_pop(R30); // 1
        xasm.emit_pop(R29); // 2
        xasm.emit_pop(R28); // 3
        xasm.emit_pop(R27); // 4
        xasm.emit_mulhwu(R26, R29, R27); // h(2 x 4)
        xasm.emit_mullw(R25, R29, R27);  // l(2 x 4)
        xasm.emit_mullw(R24, R30, R27);  // l(1 x 4)
        xasm.emit_mullw(R23, R29, R28);  // l(2 x 3)
        xasm.emit_mullw(R22, R30, R28);  // l(1 x 3)
        xasm.emit_add(R21, R26, R24);    // h(2x4) + l(1x4)
        xasm.emit_add(R20, R21, R23);    // h(2x4) + l(1x4) + l(2x3)
        xasm.emit_add(R19, R20, R22);    // h(2x4) + l(1x4) + l(2x3) + l(1x3)
        xasm.emit_push(R25);
        xasm.emit_push(R19);
    }

    public void visit(Instruction.FMUL i) {
        xasm.emit_popfs(F30);
        xasm.emit_popfs(F29);
        xasm.emit_fmuls(F28, F29, F30);
        xasm.emit_pushfs(F28);
    }

    public void visit(Instruction.DMUL i) {
        xasm.emit_popfd(F30);
        xasm.emit_popfd(F29);
        xasm.emit_fmul(F28, F29, F30);
        xasm.emit_pushfd(F28);
    }

    public void visit(Instruction.IDIV i) {
        xasm.emit_pop(R30);
        generateDivisionByIntZeroCheck(R30);
        xasm.emit_pop(R29);
        xasm.emit_divw(R28, R29, R30);
        xasm.emit_push(R28);
    }

    public void visit(Instruction.LDIV i) { // call __divdi3    
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateDivisionByLongZeroCheck(R30, R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        xasm.emit_mr(R3, R28);
        xasm.emit_mr(R4, R27);
        xasm.emit_mr(R5, R30);
        xasm.emit_mr(R6, R29);
        generateCFunctionCall("ldiv");
        xasm.emit_push(R4);
        xasm.emit_push(R3);
    }

    public void visit(Instruction.FDIV i) {
        xasm.emit_popfs(F30);
        xasm.emit_popfs(F29);
        xasm.emit_fdivs(F28, F29, F30);
        xasm.emit_pushfs(F28);
    }

    public void visit(Instruction.DDIV i) {
        xasm.emit_popfd(F30);
        xasm.emit_popfd(F29);
        xasm.emit_fdiv(F28, F29, F30);
        xasm.emit_pushfd(F28);
    }

    public void visit(Instruction.IREM i) {
        xasm.emit_pop(R30);
        generateDivisionByIntZeroCheck(R30);
        xasm.emit_pop(R29);
        xasm.emit_divw(R28, R29, R30);
        xasm.emit_mullw(R27, R28, R30);
        xasm.emit_sub(R26, R29, R27);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.LREM i)  { // call __moddi3
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateDivisionByLongZeroCheck(R30, R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        xasm.emit_mr(R3, R28);
        xasm.emit_mr(R4, R27);
        xasm.emit_mr(R5, R30);
        xasm.emit_mr(R6, R29);
        generateCFunctionCall("lrem");
        xasm.emit_push(R4);
        xasm.emit_push(R3);
    }

    public void visit(Instruction.FREM i)  { // call fmod w/ conversion to doubles
        xasm.emit_popfs(F2);
        xasm.emit_popfs(F1);
        generateCFunctionCall("fmod");
        xasm.emit_pushfs(F1);
    }

    public void visit(Instruction.DREM i)  { // call fmod
        xasm.emit_popfd(F2);
        xasm.emit_popfd(F1);
        generateCFunctionCall("fmod");
        xasm.emit_pushfd(F1);
    }

    public void visit(Instruction.INEG i) {
        xasm.emit_pop(R30);
        xasm.emit_neg(R29, R30);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.LNEG i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_li32(R28, 0);
        xasm.emit_subc(R27, R28, R29);
        xasm.emit_sube(R26, R28, R30);
        xasm.emit_push(R27);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.FNEG i) {  
        xasm.emit_popfs(F30);
        xasm.emit_fneg(F29, F30);
        xasm.emit_pushfs(F29);
    }

    public void visit(Instruction.DNEG i) {
        xasm.emit_popfd(F30);
        xasm.emit_fneg(F29, F30);
        xasm.emit_pushfd(F29);
    }

    public void visit(Instruction.ISHL i) {    
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_andi(R30, R30, 0x1F);
        xasm.emit_slw(R28, R29, R30);
        xasm.emit_push(R28);
    }
    
    
    public void visit(Instruction.LSHL i) {
        xasm.emit_pop(R30); // width
        xasm.emit_pop(R29); // high
        xasm.emit_pop(R28); // low
        xasm.emit_andi(R30, R30, 0x3F);
        xasm.emit_addicd(R27, R30, 0xFFFFFFE0);
        Branch b0 = xasm.emit_bc_d(BO_TRUE, CR0_LT);
        xasm.emit_slw(R25, R28, R27);
        xasm.emit_li32(R24, 0);
        Branch b1 = xasm.emit_b_d();
        xasm.setBranchTarget(b0);
        xasm.emit_rlwinm(R27, R28, 31, 1, 31);
        xasm.emit_subfic(R26, R30, 0x1F);
        xasm.emit_srw(R27, R27, R26);
        xasm.emit_slw(R25, R29, R30);
        xasm.emit_or(R25, R27, R25);
        xasm.emit_slw(R24, R28, R30);
        xasm.setBranchTarget(b1);
        xasm.emit_or(R29, R25, R25);
        xasm.emit_or(R28, R24, R24);
        xasm.emit_push(R28);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.ISHR i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_andi(R30, R30, 0x1F);
        xasm.emit_sraw(R28, R29, R30);
        xasm.emit_push(R28);
    }

    public void visit(Instruction.LSHR i) {
        xasm.emit_pop(R30); // width
        xasm.emit_pop(R29); // high
        xasm.emit_pop(R28); // low
        xasm.emit_andi(R30, R30, 0x3F);
        xasm.emit_addicd(R27, R30, 0xFFFFFFE0);
        Branch b0 = xasm.emit_bc_d(BO_TRUE, CR0_LT);
        xasm.emit_sraw(R24, R29, R27);
        xasm.emit_srawi(R25, R29, 31);
        Branch b1 = xasm.emit_b_d();
        xasm.setBranchTarget(b0);
        xasm.emit_rlwinm(R27, R29, 1, 0, 30);
        xasm.emit_subfic(R26, R30, 0x1F);
        xasm.emit_slw(R27, R27, R26);
        xasm.emit_srw(R24, R28, R30);
        xasm.emit_or(R24, R27, R24);
        xasm.emit_sraw(R25, R29, R30);
        xasm.setBranchTarget(b1);
        xasm.emit_or(R29, R25, R25);
        xasm.emit_or(R28, R24, R24);
        xasm.emit_push(R28);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.IUSHR i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_andi(R30, R30, 0x1F);
        xasm.emit_srw(R28, R29, R30);
        xasm.emit_push(R28);
    }

    public void visit(Instruction.LUSHR i) {
        xasm.emit_pop(R30); // width
        xasm.emit_pop(R29); // high
        xasm.emit_pop(R28); // low
        xasm.emit_andi(R30, R30, 0x3F);
        xasm.emit_addicd(R27, R30, 0xFFFFFFE0);
        Branch b0 = xasm.emit_bc_d(BO_TRUE, CR0_LT);
        xasm.emit_srw(R24, R29, R27);
        xasm.emit_li32(R25, 0);
        Branch b1 = xasm.emit_b_d();
        xasm.setBranchTarget(b0);
        xasm.emit_rlwinm(R27, R29, 1, 0, 30);
        xasm.emit_subfic(R26, R30, 0x1F);
        xasm.emit_slw(R27, R27, R26);
        xasm.emit_srw(R24, R28, R30);
        xasm.emit_or(R24, R27, R24);
        xasm.emit_srw(R25, R29, R30);
        xasm.setBranchTarget(b1);
        xasm.emit_or(R29, R25, R25);
        xasm.emit_or(R28, R24, R24);
        xasm.emit_push(R28);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.IAND i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_and(R28, R30, R29);
        xasm.emit_push(R28);
    }

    public void visit(Instruction.LAND i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        xasm.emit_and(R26, R30, R28);
        xasm.emit_and(R25, R27, R29);
        xasm.emit_push(R25);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.IOR i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_or(R28, R30, R29);
        xasm.emit_push(R28);
    }

    public void visit(Instruction.LOR i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        xasm.emit_or(R26, R30, R28);
        xasm.emit_or(R25, R27, R29);
        xasm.emit_push(R25);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.IXOR i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_xor(R28, R30, R29);
        xasm.emit_push(R28);
    }

    public void visit(Instruction.LXOR i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        xasm.emit_xor(R26, R30, R28);
        xasm.emit_xor(R25, R27, R29);
        xasm.emit_push(R25);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.IINC i) {
        int index = i.getLocalVariableOffset(buf);
        int delta = i.getValue(buf);
        int localOffset = stackLayout.getLocalVariableNativeOffset(index);
        int localOffsetFromSP = localOffset + stackLayout.getNativeFrameSize();
        xasm.emit_lwz(R30, SP, localOffsetFromSP);
        xasm.emit_addi(R29, R30, delta);
        xasm.emit_stw(R29, SP, localOffsetFromSP);
    }
    
    public void visit(Instruction.I2L i) {
        xasm.emit_pop(R30);
        xasm.emit_mr(R29, R30);
        xasm.emit_srawi(R30, R30, 31);
        xasm.emit_push(R29);
        xasm.emit_push(R30);
    }

    public void visit(Instruction.I2F i) {
        xasm.emit_pop(R30);
        xasm.emit_xoris(R30, R30, 0x8000);
        xasm.emit_li32(R29, 0x43300000);
        xasm.emit_li32(R28, 0x80000000);
        xasm.emit_stw(R29, opSP, -8);
        xasm.emit_stw(R30, opSP, -4);
        xasm.emit_lfd(F31, opSP, -8);
        xasm.emit_stw(R29, opSP, -8);
        xasm.emit_stw(R28, opSP, -4);
        xasm.emit_lfd(F30, opSP, -8);
        xasm.emit_fsub(F29, F31, F30);
        xasm.emit_frsp(F28, F29);
        xasm.emit_pushfs(F28);
    }

    public void visit(Instruction.I2D i) {
        xasm.emit_pop(R30);
        xasm.emit_xoris(R30, R30, 0x8000);
        xasm.emit_li32(R29, 0x43300000);
        xasm.emit_li32(R28, 0x80000000);
        xasm.emit_stw(R29, opSP, -8);
        xasm.emit_stw(R30, opSP, -4);
        xasm.emit_lfd(F31, opSP, -8);
        xasm.emit_stw(R29, opSP, -8);
        xasm.emit_stw(R28, opSP, -4);
        xasm.emit_lfd(F30, opSP, -8);
        xasm.emit_fsub(F29, F31, F30);
        xasm.emit_pushfd(F29);
    }

    public void visit(Instruction.L2I i) {
        xasm.emit_addi(opSP, opSP, 4);
    }

    public void visit(Instruction.L2F i) {
        xasm.emit_pop(R3);
        xasm.emit_pop(R4);
        generateCFunctionCall("l2f");
        xasm.emit_pushfs(F1);
    }

    public void visit(Instruction.L2D i) { 
        xasm.emit_pop(R3);
        xasm.emit_pop(R4);
        generateCFunctionCall("l2d");
        xasm.emit_pushfd(F1);
    }

    /*
     * IEEE compliance. The algorithm is the following: If the input
     * is equals to itself, it is not a NaN.  If it is a NaN, push
     * zero. If it is not and it is a positive or negative infinity,
     * push Integer.MAX_VALUE or MIN_VALUE, respectively. Otherwise,
     * do a normal conversion using HW.
     */
    public void visit(Instruction.F2I i) {
        xasm.emit_popfs(F1);
        generateCFunctionCall("f2i");
        xasm.emit_push(R3);

	/*
        xasm.emit_popfs(F31);
        xasm.emit_fcmpu(CR7, F31, F31);
        Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
	// NaN
	xasm.emit_li32(R30, 0);
	xasm.emit_push(R30);
	Branch b2 = xasm.emit_b_d();

	xasm.setBranchTarget(b1);
	// Not NaN
	xasm.emit_li32(R30, Float.floatToIntBits(Float.POSITIVE_INFINITY));
	xasm.emit_li32(R29, Float.floatToIntBits(Float.NEGATIVE_INFINITY));
	xasm.emit_stw(R30, opSP, -8);
	xasm.emit_stw(R29, opSP, -4);
	xasm.emit_lfs(F30, opSP, -8);
	xasm.emit_lfs(F29, opSP, -4);

        xasm.emit_fcmpu(CR7, F31, F30);
        Branch b3 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
	// == POSITIVE_INFINITY
	xasm.emit_li32(R30, Integer.MAX_VALUE);
	xasm.emit_push(R30);
	Branch b4 = xasm.emit_b_d();

	// != POSITIVE_INFINITY
	xasm.setBranchTarget(b3);
        xasm.emit_fcmpu(CR7, F31, F29);
        Branch b5 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);

	// == NEGATIVE_INFINITY
	xasm.emit_li32(R30, Integer.MIN_VALUE);
	xasm.emit_push(R30);
	Branch b6 = xasm.emit_b_d();

	// != NEGATIVE_INFINITY
	xasm.setBranchTarget(b5);

        xasm.emit_fctiw(F30, F31);
        xasm.emit_stfd(F30, opSP, -8);
        xasm.emit_lwz(R30, opSP, -4);
        xasm.emit_push(R30);

	xasm.setBranchTarget(b2);
	xasm.setBranchTarget(b4);
	xasm.setBranchTarget(b6);
	*/
    }

    public void visit(Instruction.F2L i) {
        xasm.emit_popfs(F1);
        generateCFunctionCall("f2l");
        xasm.emit_push(R4);
        xasm.emit_push(R3);
    }

    public void visit(Instruction.F2D i) {
        xasm.emit_popfs(F31);
        xasm.emit_pushfd(F31);
        //xasm.emit_popfs(F1);
        //generateCFunctionCall("f2d");
        //xasm.emit_pushfd(F1);
    }

    public void visit(Instruction.D2I i) {
	xasm.emit_popfd(F1);
	generateCFunctionCall("d2i");
	xasm.emit_push(R3);
	/*
        xasm.emit_popfd(F31);
        xasm.emit_fcmpu(CR7, F31, F31);
        Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
	// NaN
	xasm.emit_li32(R30, 0);
	xasm.emit_push(R30);
	Branch b2 = xasm.emit_b_d();

	xasm.setBranchTarget(b1);
	// Not NaN
	long dpi = Double.doubleToLongBits(Double.POSITIVE_INFINITY);
	long dni = Double.doubleToLongBits(Double.NEGATIVE_INFINITY);
	int dpil = (int)(dpi & 0x00000000FFFFFFFFL);
	int dpih = (int)((dpi >> 32) & 0x00000000FFFFFFFFL);
	int dnil = (int)(dni & 0x00000000FFFFFFFFL);
	int dnih = (int)((dni >> 32) & 0x00000000FFFFFFFFL);
	xasm.emit_li32(R30, dpil);
	xasm.emit_li32(R29, dpih);
	xasm.emit_li32(R28, dnil);
	xasm.emit_li32(R27, dnih);

	xasm.emit_stw(R30, opSP, -4);
	xasm.emit_stw(R29, opSP, -8);
	xasm.emit_stw(R28, opSP, -12);
	xasm.emit_stw(R27, opSP, -16);

	xasm.emit_lfd(F30, opSP, -8); // PI
	xasm.emit_lfd(F29, opSP, -16); // NI

        xasm.emit_fcmpu(CR7, F31, F30);
        Branch b3 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
	// == POSITIVE_INFINITY
	xasm.emit_li32(R30, Integer.MAX_VALUE);
	xasm.emit_push(R30);
	Branch b4 = xasm.emit_b_d();

	// != POSITIVE_INFINITY
	xasm.setBranchTarget(b3);
        xasm.emit_fcmpu(CR7, F31, F29);
        Branch b5 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);

	// == NEGATIVE_INFINITY
	xasm.emit_li32(R30, Integer.MIN_VALUE);
	xasm.emit_push(R30);
	Branch b6 = xasm.emit_b_d();

	// != NEGATIVE_INFINITY
	xasm.setBranchTarget(b5);

        xasm.emit_fctiw(F30, F31);
        xasm.emit_stfd(F30, opSP, -8);
        xasm.emit_lwz(R30, opSP, -4);
        xasm.emit_push(R30);

	xasm.setBranchTarget(b2);
	xasm.setBranchTarget(b4);
	xasm.setBranchTarget(b6);
	*/
    }

    public void visit(Instruction.D2L i) {
        xasm.emit_popfd(F1);
        generateCFunctionCall("d2l");
        xasm.emit_push(R4);
        xasm.emit_push(R3);
    }

    public void visit(Instruction.D2F i) {
        xasm.emit_popfd(F31);
        xasm.emit_frsp(F30, F31);
        xasm.emit_pushfs(F30);
    }

    public void visit(Instruction.I2B i) {
        xasm.emit_pop(R30);
        xasm.emit_extsb(R29, R30);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.I2C i) {
        xasm.emit_pop(R30);
        xasm.emit_rlwinm(R29, R30, 0, 16, 31);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.I2S i) {
        xasm.emit_pop(R30);
        xasm.emit_extsh(R29, R30);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.LCMP i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        xasm.emit_cmp(CR7, false, R27, R29);
        Branch b1 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_cmp(CR7, false, R28, R30);
        Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R26, 0);
        Branch b3 = xasm.emit_b_d();
        xasm.setBranchTarget(b1);
        xasm.setBranchTarget(b2);
        xasm.emit_cmp(CR7, false, R30, R28);
        Branch b4 = xasm.emit_bc_d(BO_TRUE, CR7_GT);
        Branch b5 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_cmpl(CR7, false, R29, R27);
        Branch b6 = xasm.emit_bc_d(BO_FALSE, CR7_GT);
        xasm.setBranchTarget(b4);
        xasm.emit_li32(R26, -1);
        Branch b7 = xasm.emit_b_d();
        xasm.setBranchTarget(b5);
        xasm.setBranchTarget(b6);
        xasm.emit_li32(R26, 1);
        xasm.setBranchTarget(b3);
        xasm.setBranchTarget(b7);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.FCMPL i) {
        xasm.emit_popfs(F31);
        xasm.emit_popfs(F30);
        xasm.emit_fcmpu(CR7, F30, F31);
        Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_SO_OR_FU);
        Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R30, 0);
        Branch b3 = xasm.emit_b_d();
        xasm.setBranchTarget(b2);
        xasm.emit_fcmpu(CR7, F30, F31);
        Branch b4 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
        xasm.setBranchTarget(b1);
        xasm.emit_li32(R30, -1);
        Branch b5 = xasm.emit_b_d();
        xasm.setBranchTarget(b4);
        xasm.emit_li32(R30, 1);
        xasm.setBranchTarget(b3);
        xasm.setBranchTarget(b5);
        xasm.emit_push(R30);
    }
    
    public void visit(Instruction.FCMPG i) {
    	/*
        xasm.emit_popfs(F2);
        xasm.emit_popfs(F1);
        generateCFunctionCall("fcmpg");
        xasm.emit_push(R3);
    	*/
        xasm.emit_popfs(F31);
        xasm.emit_popfs(F30);
        xasm.emit_fcmpu(CR7, F30, F31);
        Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_SO_OR_FU);
        Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R30, 0);
        Branch b3 = xasm.emit_b_d();
        xasm.setBranchTarget(b2);
        xasm.emit_fcmpu(CR7, F30, F31);
        Branch b4 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
        xasm.emit_li32(R30, -1);
        Branch b5 = xasm.emit_b_d();
        xasm.setBranchTarget(b1);
        xasm.setBranchTarget(b4);
        xasm.emit_li32(R30, 1);
        xasm.setBranchTarget(b3);
        xasm.setBranchTarget(b5);
        xasm.emit_push(R30);
    }
    
    public void visit(Instruction.DCMPL i) {
        xasm.emit_popfd(F31);
        xasm.emit_popfd(F30);
        xasm.emit_fcmpu(CR7, F30, F31);
        Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_SO_OR_FU);
        Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R30, 0);
        Branch b3 = xasm.emit_b_d();
        xasm.setBranchTarget(b2);
        xasm.emit_fcmpu(CR7, F30, F31);
        Branch b4 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
        xasm.setBranchTarget(b1);
        xasm.emit_li32(R30, -1);
        Branch b5 = xasm.emit_b_d();
        xasm.setBranchTarget(b4);
        xasm.emit_li32(R30, 1);
        xasm.setBranchTarget(b3);
        xasm.setBranchTarget(b5);
        xasm.emit_push(R30);
    }
    
    public void visit(Instruction.DCMPG i) {
        xasm.emit_popfd(F31);
        xasm.emit_popfd(F30);
        xasm.emit_fcmpu(CR7, F30, F31);
        Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_SO_OR_FU);
        Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R30, 0);
        Branch b3 = xasm.emit_b_d();
        xasm.setBranchTarget(b2);
        xasm.emit_fcmpu(CR7, F30, F31);
        Branch b4 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
        xasm.emit_li32(R30, -1);
        Branch b5 = xasm.emit_b_d();
        xasm.setBranchTarget(b1);
        xasm.setBranchTarget(b4);
        xasm.emit_li32(R30, 1);
        xasm.setBranchTarget(b3);
        xasm.setBranchTarget(b5);
        xasm.emit_push(R30);
    }
    
    public void visit(Instruction.POP i) {
        xasm.emit_addi(opSP, opSP, 4);
    }

    public void visit(Instruction.POP2 i) {
        xasm.emit_addi(opSP, opSP, 8);
    }

    public void visit(Instruction.DUP i) {
        xasm.emit_pop(R30);
        xasm.emit_push(R30);
        xasm.emit_push(R30);
    }

    public void visit(Instruction.DUP_X1 i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_push(R30);
        xasm.emit_push(R29);
        xasm.emit_push(R30);
    }

    public void visit(Instruction.DUP_X2 i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28); 
        xasm.emit_push(R30);
        xasm.emit_push(R28); 
        xasm.emit_push(R29);
        xasm.emit_push(R30);
    }

    public void visit(Instruction.DUP2 i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_push(R29);
        xasm.emit_push(R30);
        xasm.emit_push(R29);
        xasm.emit_push(R30);
    }

    public void visit(Instruction.DUP2_X1 i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28); 
        xasm.emit_push(R29);
        xasm.emit_push(R30);
        xasm.emit_push(R28); 
        xasm.emit_push(R29);
        xasm.emit_push(R30);
    }

    public void visit(Instruction.DUP2_X2 i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28); 
        xasm.emit_pop(R27);
        xasm.emit_push(R29);
        xasm.emit_push(R30);
        xasm.emit_push(R27);
        xasm.emit_push(R28); 
        xasm.emit_push(R29);
        xasm.emit_push(R30);
    }

    public void visit(Instruction.SWAP i) {
        xasm.emit_pop(R30); 
        xasm.emit_pop(R29);
        xasm.emit_push(R30);
        xasm.emit_push(R29);
    }
        
    public void visit(Instruction.ROLL i) {
        int span = i.getSpan(buf);
        int count = i.getCount(buf);
        if (count > 0) {
            xasm.emit_addi(R30, opSP, span * 4);
            for (int k = span; k > 0; k--) {
                xasm.emit_lwz(R29, R30, -(k * 4));
                xasm.emit_stw(R29, R30, -((k + count) * 4));
            }
            for (int k = count; k > 0; k--) {
                xasm.emit_lwz(R29, R30, -((span + k) * 4));
                xasm.emit_stw(R29, R30, -(k * 4));
            }
        } else if (count < 0) {
            int pcount = -count; // Note count is negative
            xasm.emit_addi(R30, opSP, -pcount * 4);
            xasm.emit_addi(R29, opSP, (span - pcount) * 4);
            for (int k = 0; k < pcount; k++) {
                xasm.emit_lwz(R28, R29, k * 4);
                xasm.emit_stw(R28, R30, k * 4);
            }
            xasm.emit_addi(R29, R29, -(span - pcount) * 4);
            for (int k = (span - pcount) - 1; k >= 0; k--) {
                xasm.emit_lwz(R28, R29, k * 4);
                xasm.emit_stw(R28, R29, (k + pcount) * 4);
            }
            for (int k = pcount - 1; k >= 0; k--) {
                xasm.emit_lwz(R28, R30, k * 4);
                xasm.emit_stw(R28, R30, (k + pcount) * 4);
            }
        }
    }
    
    // goto, goto_w
    public void visit(Instruction.GotoInstruction i) {
        int branchBCOffset = i.getTarget(buf);
	if (branchBCOffset > FAR_JUMP_THRESHOULD 
	    || branchBCOffset < -FAR_JUMP_THRESHOULD) { // far jump
	    xasm.emit_b_i(1, false, true); // call the next instruction
	    xasm.emit_mflr(R28);
	    xasm.emit_addi(R27, R28, 6 * 4); // add the distance from the beginning of the mflr to the jump table
	    xasm.emit_lwz(R24, R27, 0); // R24 - jump offset
	    xasm.emit_add(R23, R27, R24);
	    xasm.emit_mtctr(R23);
	    xasm.emit_bctr();
	    Branch branch = new Branch(xasm.getPC(), 2, 30);
	    codeGenContext.addRelativeJumpPatch(branch, getPC() + branchBCOffset);
	    xasm.writeWord(0); // jump table of length 1
	} else {
	    if (branchBCOffset <= 0) {
		Branch branch = new Branch(codeGenContext.getBytecodePC2NativePC(getPC() + branchBCOffset));
		xasm.emit_b_i(branch);
            } else {
		Branch branch = xasm.emit_b_i_d(false);
		codeGenContext.addRelativeJumpPatch(branch, getPC() + branchBCOffset);
	    }
        }
    }
    
    // ifeq, ifne, ifle, iflt, ifge, ifgt, ifnull, ifnonnull
    public void visit(Instruction.IfZ i) {
        int branchBCOffset = i.getBranchTarget(buf);
        int opcode = i.getOpcode();
        int bi;
        int bo = BO_TRUE;
        switch (opcode) {
        case Opcodes.IFEQ:
        case Opcodes.IFNULL:
            bi = CR7_EQ;
            break;
        case Opcodes.IFNE:
        case Opcodes.IFNONNULL:
            bi = CR7_EQ;
            bo = BO_FALSE;
            break;
        case Opcodes.IFLT:
            bi = CR7_LT;
            break;
        case Opcodes.IFGE:
            bi = CR7_LT;
            bo = BO_FALSE;
            break;
        case Opcodes.IFGT:
            bi = CR7_GT;
            break;
        case Opcodes.IFLE:
            bi = CR7_GT;
            bo = BO_FALSE;
            break;
        default:
            throw new Error();
        }
        xasm.emit_pop(R30);
        xasm.emit_cmpi(CR7, false, R30, 0);

	if (branchBCOffset > FAR_JUMP_THRESHOULD 
	    || branchBCOffset < -FAR_JUMP_THRESHOULD) { // far jump
	    Branch b0 = xasm.emit_bc_d(bo, bi);
	    Branch b1 = xasm.emit_b_d();
	    xasm.setBranchTarget(b0);
	    xasm.emit_b_i(1, false, true); // call the next instruction
	    xasm.emit_mflr(R28);
	    xasm.emit_addi(R27, R28, 6 * 4); // add the distance from the beginning of the mflr to the jump table
	    xasm.emit_lwz(R24, R27, 0); // R24 - jump offset
	    xasm.emit_add(R23, R27, R24);
	    xasm.emit_mtctr(R23);
	    xasm.emit_bctr();
	    Branch branch = new Branch(xasm.getPC(), 2, 30);
	    codeGenContext.addRelativeJumpPatch(branch, getPC() + branchBCOffset);
	    xasm.writeWord(0); // jump table of length 1
	    xasm.setBranchTarget(b1);
	} else {
	    if (branchBCOffset <= 0) {
		Branch branch = new Branch(codeGenContext.getBytecodePC2NativePC(getPC() + branchBCOffset));
		xasm.emit_bc(bo, bi, branch);
            } else {
                Branch branch = xasm.emit_bc_d(bo, bi);
                codeGenContext.addRelativeJumpPatch(branch, getPC() + branchBCOffset);
            }
        }
    }
    
    // if_icmpxx, if_acmpxx
    public void visit(Instruction.IfCmp i) {
        int branchBCOffset = i.getBranchTarget(buf);
        int opcode = i.getOpcode();
        int bi;
        int bo = BO_TRUE;
        switch (opcode) {
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ACMPEQ:
            bi = CR7_EQ;
            break;
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ACMPNE:
            bi = CR7_EQ;
            bo = BO_FALSE;
            break;
        case Opcodes.IF_ICMPLT:
            bi = CR7_LT;
            break;
        case Opcodes.IF_ICMPGE:
            bi = CR7_LT;
            bo = BO_FALSE;
            break;
        case Opcodes.IF_ICMPGT:
            bi = CR7_GT;
            break;
        case Opcodes.IF_ICMPLE:
            bi = CR7_GT;
            bo = BO_FALSE;
            break;
        default:
            throw new Error();
        }
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_cmp(CR7, false, R29, R30);

	if (branchBCOffset > FAR_JUMP_THRESHOULD 
	    || branchBCOffset < -FAR_JUMP_THRESHOULD) { // far jump
	    Branch b0 = xasm.emit_bc_d(bo, bi);
	    Branch b1 = xasm.emit_b_d();
	    xasm.setBranchTarget(b0);
	    xasm.emit_b_i(1, false, true); // call the next instruction
	    xasm.emit_mflr(R28);
	    xasm.emit_addi(R27, R28, 6 * 4); // add the distance from the beginning of the mflr to the jump table
	    xasm.emit_lwz(R24, R27, 0); // R24 - jump offset
	    xasm.emit_add(R23, R27, R24);
	    xasm.emit_mtctr(R23);
	    xasm.emit_bctr();
	    Branch branch = new Branch(xasm.getPC(), 2, 30);
	    codeGenContext.addRelativeJumpPatch(branch, getPC() + branchBCOffset);
	    xasm.writeWord(0); // jump table of length 1
	    xasm.setBranchTarget(b1);
	} else {
	    if (branchBCOffset <= 0) {
		Branch branch = new Branch(codeGenContext.getBytecodePC2NativePC(getPC() + branchBCOffset));
		xasm.emit_bc(bo, bi, branch);
	    } else {
		Branch branch = xasm.emit_bc_d(bo, bi);
		codeGenContext.addRelativeJumpPatch(branch, getPC() + branchBCOffset);
	    }
        }
    }
    
    public void visit(Instruction.LOOKUPSWITCH i) {
        int def = i.getDefaultTarget(buf);
        int npairs = i.getTargetCount(buf) - 1;
        int[] cases = i.getIndexForTargets(buf);
        int[] targets = i.getTargets(buf);

        xasm.emit_pop(R30);
        for (int j = 0 ; j < npairs; j++) {
            int caseValue = cases[j];
            int jumpOffset = targets[j];
            xasm.emit_cmpi32(CR7, false, R30, caseValue, R29);
            if (jumpOffset <= 0) {
                Branch branch = new Branch(codeGenContext.getBytecodePC2NativePC(getPC() + jumpOffset));
                xasm.emit_bc(BO_TRUE, CR7_EQ, branch);
            } else {
		if (jumpOffset > FAR_JUMP_THRESHOULD) { // far jump
		    Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		    Branch b1 = xasm.emit_b_d();
		    xasm.setBranchTarget(b0);
		    xasm.emit_b_i(1, false, true); // call the next instruction
		    xasm.emit_mflr(R28);
		    xasm.emit_addi(R27, R28, 6 * 4); // add the distance from the beginning of the mflr to the jump table
		    xasm.emit_lwz(R24, R27, 0); // R24 - jump offset
		    xasm.emit_add(R23, R27, R24);
		    xasm.emit_mtctr(R23);
		    xasm.emit_bctr();
		    Branch branch = new Branch(xasm.getPC(), 2, 30);
		    codeGenContext.addRelativeJumpPatch(branch, getPC() + jumpOffset);
		    xasm.writeWord(0); // jump table of length 1
		    xasm.setBranchTarget(b1);
		} else {
		    Branch branch = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		    codeGenContext.addRelativeJumpPatch(branch, getPC() + jumpOffset);
		}
            }
        }

	if (def > FAR_JUMP_THRESHOULD 
	    || def < -FAR_JUMP_THRESHOULD) { // far jump
	    xasm.emit_b_i(1, false, true); // call the next instruction
	    xasm.emit_mflr(R28);
	    xasm.emit_addi(R27, R28, 6 * 4); // add the distance from the beginning of the mflr to the jump table
	    xasm.emit_lwz(R24, R27, 0); // R24 - jump offset
	    xasm.emit_add(R23, R27, R24);
	    xasm.emit_mtctr(R23);
	    xasm.emit_bctr();
	    Branch branch = new Branch(xasm.getPC(), 2, 30);
	    codeGenContext.addRelativeJumpPatch(branch, getPC() + def);
	    xasm.writeWord(0); // jump table of length 1
	} else {
	    if (def <= 0) {
		Branch branch = new Branch(codeGenContext.getBytecodePC2NativePC(getPC() + def));
		xasm.emit_b_i(branch);
	    } else {
		Branch branch = xasm.emit_b_d();
		codeGenContext.addRelativeJumpPatch(branch, getPC() + def);
	    }
        }
    }
    
    public void visit(Instruction.TABLESWITCH i) {
        int def = i.getDefaultTarget(buf);
        int high = i.getHigh(buf);
        int low = i.getLow(buf);
        int[] targets = i.getTargets(buf);

        xasm.emit_pop(R30);
        xasm.emit_addi(R29, R30, -low);
        xasm.emit_cmpli(CR7, false, R29, high-low);
        
        if (def <= 0) {
            Branch branch = new Branch(codeGenContext.getBytecodePC2NativePC(getPC() + def));
            xasm.emit_bc(BO_TRUE, CR7_GT, branch);
        } else {
            if (def > FAR_JUMP_THRESHOULD) { // far jump
                Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_GT);
                Branch b1 = xasm.emit_b_d();
                xasm.setBranchTarget(b0);
                xasm.emit_b_i(1, false, true); // call the next instruction
                xasm.emit_mflr(R28);
                xasm.emit_addi(R27, R28, 6 * 4); // add the distance from the beginning of the mflr to the jump table
                xasm.emit_lwz(R24, R27, 0); // R24 - jump offset
                xasm.emit_add(R23, R27, R24);
                xasm.emit_mtctr(R23);
                xasm.emit_bctr();
                Branch branch = new Branch(xasm.getPC(), 2, 30);
                codeGenContext.addRelativeJumpPatch(branch, getPC() + def);
                xasm.writeWord(0); // jump table of length 1
                xasm.setBranchTarget(b1);
            } else {
		Branch branch = xasm.emit_bc_d(BO_TRUE, CR7_GT);
		codeGenContext.addRelativeJumpPatch(branch, getPC() + def);
	    }
        }

        xasm.emit_b_i(1, false, true); // call the next instruction
        xasm.emit_mflr(R28);
        xasm.emit_addi(R27, R28, 8 * 4); // add the distance from the beginning of the mflr to the jump table
        xasm.emit_slwi(R26, R29, 2); // x4
        xasm.emit_add(R25, R27, R26); // R25 - jump offset
        xasm.emit_lwz(R24, R25, 0); // R24 - jump offset
        xasm.emit_add(R23, R25, R24);
        xasm.emit_mtctr(R23);
        xasm.emit_bctr();

        // filling the jump table
        for(int j = 0; j <= high - low; j++) {
            int jumpOffset = targets[j];
            Branch branch = new Branch(xasm.getPC(), 2, 30);
            codeGenContext.addRelativeJumpPatch(branch, getPC() + jumpOffset);
            xasm.writeWord(0); // dummy
        }
    }
    
    public void visit(Instruction.JsrInstruction i) {
        int jumpOffset = i.getTarget(buf);
        xasm.emit_b_i(1, false, true); // call the next instruction
        xasm.emit_mflr(R30);
        xasm.emit_addi(R29, R30, 5 * 4);
        xasm.emit_push(R29); // 2 instructions
        if (jumpOffset <= 0) {
            Branch branch = new Branch(codeGenContext.getBytecodePC2NativePC(getPC() + jumpOffset));
            xasm.emit_b_i(branch);
        } else {
            Branch branch = xasm.emit_b_i_d(false);
            codeGenContext.addRelativeJumpPatch(branch, getPC() + jumpOffset);
        }
    }

    private void generateRET(int localVariableIndex) {
        int localOffset = stackLayout.getLocalVariableNativeOffset(localVariableIndex);
        int localOffsetFromSP = localOffset + stackLayout.getNativeFrameSize();
        xasm.emit_lwz(R29, SP, localOffsetFromSP);
        xasm.emit_mtctr(R29);
        xasm.emit_bctr();
    }
    
    public void visit(Instruction.RET i) {
        generateRET(i.getLocalVariableOffset(buf));
    }

    public void visit(Instruction.WIDE_RET i) {
        generateRET(i.getLocalVariableOffset(buf));
    }

    public void visit(Instruction.ReturnValue i) {
        if (isSynchronized) {
            int receiverLocalOffset = stackLayout.getLocalVariableNativeOffset(0)
                + stackLayout.getNativeFrameSize();
            xasm.emit_lwz(R5, SP, receiverLocalOffset);
            generateCSACall(precomputed.csa_monitorExit_index, 
                    precomputed.csa_monitorExit_desc);
        }
        if (debugPrintOn)
            debugPrint("[Returning from " + getSelector().toString() 
                   + " ##" + counter + "##]\n");
        int opcode = i.getOpcode();
        switch(opcode) {
        case Opcodes.IRETURN:
        case Opcodes.ARETURN:
            xasm.emit_pop(R3);
            break;
        case Opcodes.LRETURN:
            xasm.emit_pop(R3);
            xasm.emit_pop(R4);
            break;
        case Opcodes.FRETURN:
            xasm.emit_popfs(F1);
            break;
        case Opcodes.DRETURN:
            xasm.emit_popfd(F1);
            break;
        default:
            throw new OVMError();
        }
        generateReturnSequence();
    }

    public void visit(RETURN i) {
        if (isSynchronized) {
            int receiverLocalOffset = stackLayout.getLocalVariableNativeOffset(0)
                + stackLayout.getNativeFrameSize();
            xasm.emit_lwz(R5, SP, receiverLocalOffset);
            generateCSACall(precomputed.csa_monitorExit_index, 
                    precomputed.csa_monitorExit_desc);
        }
        generateReturnSequence();
    }
    
    public void visit(Instruction.ATHROW i) {
        xasm.emit_pop(R5);
        generateNullCheck(R5);
        xasm.emit_push(R5);
        generateCSACall(precomputed.csa_processThrowable_index, 
                precomputed.csa_processThrowable_desc);
    }
    
    private void generateReturnSequence() {
        xasm.emit_lwz(R0, SP, stackLayout.getReturnAddressOffset() + stackLayout.getNativeFrameSize());
        xasm.emit_mtlr(R0);
        xasm.emit_addi(SP, SP, stackLayout.getNativeFrameSize());
        xasm.emit_blr();
    }
    
    private void saveVolatileRegisters() {
        xasm.emit_stw(opSP, SP, stackLayout.getNativeFrameSize() + stackLayout.getGeneralRegisterOffset(opSP));
    }
    
    private void restoreVolatileRegisters() {
        xasm.emit_lwz(opSP, SP, stackLayout.getNativeFrameSize() + stackLayout.getGeneralRegisterOffset(opSP));
    }
    
    private void unloadArguments(Descriptor.Method desc) {
        int frame_size = stackLayout.getNativeFrameSize();
        int argOffset = stackLayout.getReceiverOffset() + frame_size;
        xasm.emit_stw(R3, SP, stackLayout.getCodeFragmentOffset() + frame_size); // code
        xasm.emit_stw(R4, SP, argOffset); // receiver
        argOffset += 4;
        int gprArgumentIndex = R5; // R5 to R10
        int fprArgumentIndex = F1; // F1 to F13
        for(int k = 0; k < desc.getArgumentCount(); k++) {
            char t = desc.getArgumentType(k).getTypeTag();
            switch(t) {
            case TypeCodes.VOID:
                break;
            case TypeCodes.INT:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
                if (gprArgumentIndex <= R10) {
                    xasm.emit_stw(gprArgumentIndex, SP, argOffset);
                    gprArgumentIndex++;
                }
                argOffset += 4;
                break;
            case TypeCodes.LONG:
                if (gprArgumentIndex <= R10) {
                    xasm.emit_stw(gprArgumentIndex, SP, argOffset);
                    gprArgumentIndex++;
                }
                argOffset += 4;
                if (gprArgumentIndex <= R10) {
                    xasm.emit_stw(gprArgumentIndex, SP, argOffset);
                    gprArgumentIndex++;
                }
                argOffset += 4;
                break;
            case TypeCodes.DOUBLE:
                xasm.emit_stfd(fprArgumentIndex, SP, argOffset);
                fprArgumentIndex++;
                argOffset += 8;
                break;
            case TypeCodes.FLOAT:
                xasm.emit_stfs(fprArgumentIndex, SP, argOffset);
                fprArgumentIndex++;
                argOffset += 4;
                break;
            default:
                throw new OVMError();
            }
        }
    }
    private void loadArguments(Descriptor.Method desc, boolean receiverOnStack) {
        int gprRegCount = 1; // receiver
        int fprRegCount = 0;
        int argWordCount = 1;
        for(int k = desc.getArgumentCount() - 1; k >= 0; k--) {
            char t = desc.getArgumentType(k).getTypeTag();
            switch(t) {
            case TypeCodes.VOID:
                break;
            case TypeCodes.INT:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
                gprRegCount++;
                argWordCount++;
                break;
            case TypeCodes.LONG:
                gprRegCount += 2;
                argWordCount += 2;
                break;
            case TypeCodes.DOUBLE:
                fprRegCount++;
                argWordCount += 2;
                break;
            case TypeCodes.FLOAT:
                fprRegCount++;
                argWordCount++;
                break;
            default:
                throw new OVMError();
            }
        }
        if (fprRegCount > 13) {
            throw new Error("Passing FP parameter in memory not implemented yet.");
        }
        int gprMemCount = gprRegCount - 7;
        int argOffset = 12 + argWordCount * 4;
        int gprRegIndex = R3 + (gprRegCount > 7 ? 7 : gprRegCount); // R4 to R10 (R3 is for the code object)
        int fprRegIndex = F0 + fprRegCount; // F1 to F13
        for(int k = desc.getArgumentCount() - 1; k >= 0; k--) {
            char t = desc.getArgumentType(k).getTypeTag();
            switch(t) {
            case TypeCodes.VOID:
                break;
            case TypeCodes.INT:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
                if (gprMemCount > 0) {
                    xasm.emit_pop(R30);
                    xasm.emit_stw(R30, SP, argOffset);
                    gprMemCount--;
                } else {
                    xasm.emit_pop(gprRegIndex);
                    gprRegIndex--;
                }
                argOffset -= 4;
                break;
            case TypeCodes.LONG:
                if (gprMemCount >= 2) { // both words on stack
                    xasm.emit_pop(R30);
                    xasm.emit_pop(R29);
                    xasm.emit_stw(R29, SP, argOffset);
                    gprMemCount--;
                    argOffset -= 4;
                    xasm.emit_stw(R30, SP, argOffset);
                    gprMemCount--;
                    argOffset -= 4;
                } else if (gprMemCount == 1) { // one word on stack
                    xasm.emit_pop(gprRegIndex);
                    gprRegIndex--;
                    xasm.emit_pop(R29);
                    xasm.emit_stw(R29, SP, argOffset);
                    gprMemCount--;
                    argOffset -= 8;
                } else { // both in registers
                    xasm.emit_pop(gprRegIndex - 1);
                    xasm.emit_pop(gprRegIndex);
                    gprRegIndex -= 2;
                    argOffset -= 8;
                }
                break;
            case TypeCodes.DOUBLE:
                xasm.emit_popfd(fprRegIndex);
                fprRegIndex--;
                argOffset -= 8;
                break;
            case TypeCodes.FLOAT:
                xasm.emit_popfs(fprRegIndex);
                fprRegIndex--;
                argOffset -= 4;
                break;
            default:
                throw new OVMError();
            }
        }
        if (gprRegIndex != R4) {
            throw new Error();
        }
        if (receiverOnStack) {
            xasm.emit_pop(gprRegIndex); // receiver
        }
    }
    
    private void loadArgumentsForInvokeNative(Descriptor.Method desc) {
        int allArgumentRegCount = 0;
        int fprArgumentRegCount = 0;
        for(int k = desc.getArgumentCount() - 1; k >= 0; k--) {
            char t = desc.getArgumentType(k).getTypeTag();
            switch(t) {
            case TypeCodes.VOID:
                break;
            case TypeCodes.INT:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
                allArgumentRegCount++;
                break;
            case TypeCodes.LONG:
                allArgumentRegCount += 2;
                break;
            case TypeCodes.DOUBLE:
                fprArgumentRegCount++;
                allArgumentRegCount += 2;
                break;
            case TypeCodes.FLOAT:
                fprArgumentRegCount++;
                allArgumentRegCount++;
                break;
            default:
                throw new OVMError();
            }
        }
        if (allArgumentRegCount > 7 || fprArgumentRegCount > 13) {
            throw new Error("Passing parameter in memory (vararg) not implemented yet.");
        }
        int allArgumentRegIndex = R4; // R3 for the method index
        int fprArgumentRegIndex = F1;
        for(int k = desc.getArgumentCount() - 1; k >= 0; k--) {
            char t = desc.getArgumentType(k).getTypeTag();
            switch(t) {
            case TypeCodes.VOID:
                break;
            case TypeCodes.INT:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
                xasm.emit_pop(allArgumentRegIndex);
                allArgumentRegIndex++;
                break;
            case TypeCodes.LONG:
                xasm.emit_pop(allArgumentRegIndex);
                xasm.emit_pop(allArgumentRegIndex + 1);
                allArgumentRegIndex += 2;
                break;
            case TypeCodes.DOUBLE:
                xasm.emit_popfd(fprArgumentRegIndex);
                xasm.emit_pushfd(fprArgumentRegIndex);
                xasm.emit_pop(allArgumentRegIndex);
                xasm.emit_pop(allArgumentRegIndex + 1);
                fprArgumentRegIndex++;
                allArgumentRegIndex += 2;
                break;
            case TypeCodes.FLOAT:
                xasm.emit_popfs(fprArgumentRegIndex);
                xasm.emit_pushfs(fprArgumentRegIndex);
                xasm.emit_pop(allArgumentRegIndex);
                fprArgumentRegIndex++;
                allArgumentRegIndex++;
                break;
            default:
                throw new OVMError();
            }
        }
        if (allArgumentRegIndex != R4 + allArgumentRegCount) {
            throw new Error();
        }
    }
    
    private void unloadReturnValue(char returnTypeCode) {
        switch(returnTypeCode) {
        case TypeCodes.VOID:
            xasm.emit_nop();
            break;
        case TypeCodes.INT:
        case TypeCodes.SHORT:
        case TypeCodes.CHAR:
        case TypeCodes.BYTE:
        case TypeCodes.BOOLEAN:
        case TypeCodes.OBJECT:
        case TypeCodes.ARRAY:
            xasm.emit_push(R3);
            break;
        case TypeCodes.LONG:
            xasm.emit_push(R4);
            xasm.emit_push(R3);
            break;
        case TypeCodes.DOUBLE:
            xasm.emit_pushfd(F1);
            break;
        case TypeCodes.FLOAT:
            xasm.emit_pushfs(F1);
            break;
        default:
            throw new OVMError();
        }
    }
    
    private void unloadReturnValueForInvokeNative(char returnTypeCode) {
        switch(returnTypeCode) {
        case TypeCodes.VOID:
            xasm.emit_nop();
            break;
        case TypeCodes.INT:
        case TypeCodes.SHORT:
        case TypeCodes.CHAR:
        case TypeCodes.BYTE:
        case TypeCodes.BOOLEAN:
        case TypeCodes.OBJECT:
        case TypeCodes.ARRAY:
            xasm.emit_push(R4); // !
            break;
        case TypeCodes.LONG:
            xasm.emit_push(R4);
            xasm.emit_push(R3);
            break;
        case TypeCodes.DOUBLE:
            //xasm.emit_pushfd(F1);
            xasm.emit_push(R4);
            xasm.emit_push(R3);
            break;
        case TypeCodes.FLOAT:
            xasm.emit_push(R4); // !
            //xasm.emit_pushfs(F1);
            break;
        default:
            throw new OVMError();
        }
    }
    
    public void visit(Instruction.INVOKENONVIRTUAL2_QUICK i) {
        throw new Error("INVOKENONVIRTUAL2_QUICK not implemented");
        /*
        int cp_index = i.getCPIndex(buf);
        int nvtbl_index = i.getMethodTableIndex(buf);
        int offset_methodref_in_cpvalues = getArrayElementOffset(executiveDomain, OBJECT, cp_index);
        int offset_cf_in_nvtbl = getArrayElementOffset(executiveDomain, OBJECT, nvtbl_index);
        Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
        int frame_size = stackLayout.getNativeFrameSize();
        loadArguments(desc, true);
        xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);     // R30 = current code
        xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);          // R29 = cp
        xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);      // R28 = cpvalues
        xasm.emit_lwz(R27, R28, offset_methodref_in_cpvalues);         // R27 = methodref
        xasm.emit_lwz(R26, R27, precomputed.offset_bp_in_instancemref);// R26 = target bp
        xasm.emit_lwz(R25, R26, precomputed.offset_nvtbl_in_bp);       // R25 = nvtbl
        xasm.emit_lwz(R3, R25, offset_cf_in_nvtbl);                    // R3 = next code
        xasm.emit_lwz(R24, R3, precomputed.offset_code_in_cf);         // R24 = code pointer
        xasm.emit_mtctr(R24);
        saveVolatileRegisters();
        xasm.emit_bctr_lk();
        restoreVolatileRegisters();
        unloadReturnValue(desc.getType().getTypeTag());
        */
    }

    // Self-modify 3 nops into a sequence of lis, ori, and b.
    // R11 and R12 are destroyed.
    protected void selfModify_lis_ori_and_b(int offsetReg, int nopPC) {
        xasm.emit_b_i(1, false, true); // call the next instruction
        int basePC = xasm.getPC();
        xasm.emit_mflr(R11);
        xasm.emit_addi(R11, R11, -(basePC-nopPC)); // R11 - absolute nopPC

        // overwrite the 1st nop with a lis
        int lis_hi = ((15 << 26) | (offsetReg << 21) | (0 << 16)) >> 16;
        xasm.emit_srwi(R12, offsetReg, 16);
        xasm.emit_oris(R12, R12, lis_hi);
        xasm.emit_stw(R12, R11, 0);
        // overwrite the 2nd nop with an ori
        int ori_hi = ((24 << 26) | (offsetReg << 21) | (offsetReg << 16)) >> 16;
        xasm.emit_andi(R12, offsetReg, -1); // extract lower 16 bits
        xasm.emit_oris(R12, R12, ori_hi);
        xasm.emit_stw(R12, R11, 4);

        // overwrite the 3rd nop with a b (branch)
        int branch_offset = basePC - nopPC - 2 * 4 + 4 * 16; // distance from the 3rd nop to the end of this method
        if (-32768 <= branch_offset && branch_offset <= 32767) {
            // b
            int b_hi = (18 << 26) >> 16;
            xasm.emit_li(R12, branch_offset);
            xasm.emit_oris(R12, R12, b_hi);
            xasm.emit_stw(R12, R11, 8);
        } else {
            throw new Error();
        }

        xasm.emit_li(R12, 0);
        xasm.emit_dcbf(R11, R12);     // flush i-cache and so on
        xasm.emit_sync();
        xasm.emit_icbl(R11, R12);
        xasm.emit_isync();
    }

    // Self-modify 5 nops into a sequence of lis, ori, lis, ori, and b.
    // R11 and R12 are destroyed.
    protected void selfModify_lis_ori_lis_ori_and_b(int offsetReg, int offsetReg2, int nopPC) {
        xasm.emit_b_i(1, false, true); // call the next instruction
        int basePC = xasm.getPC();
        xasm.emit_mflr(R11);
        xasm.emit_addi(R11, R11, -(basePC-nopPC)); // R11 - absolute nopPC

        // overwrite the 1st nop with a lis
        int lis_hi = ((15 << 26) | (offsetReg << 21) | (0 << 16)) >> 16;
        xasm.emit_srwi(R12, offsetReg, 16);
        xasm.emit_oris(R12, R12, lis_hi);
        xasm.emit_stw(R12, R11, 0);
        // overwrite the 2nd nop with an ori
        int ori_hi = ((24 << 26) | (offsetReg << 21) | (offsetReg << 16)) >> 16;
        xasm.emit_andi(R12, offsetReg, -1); // extract lower 16 bits
        xasm.emit_oris(R12, R12, ori_hi);
        xasm.emit_stw(R12, R11, 4);

        // overwrite the 3rd nop with a lis
        lis_hi = ((15 << 26) | (offsetReg2 << 21) | (0 << 16)) >> 16;
        xasm.emit_srwi(R12, offsetReg2, 16);
        xasm.emit_oris(R12, R12, lis_hi);
        xasm.emit_stw(R12, R11, 8);
        // overwrite the 4th nop with an ori
        ori_hi = ((24 << 26) | (offsetReg2 << 21) | (offsetReg2 << 16)) >> 16;
        xasm.emit_andi(R12, offsetReg2, -1); // extract lower 16 bits
        xasm.emit_oris(R12, R12, ori_hi);
        xasm.emit_stw(R12, R11, 12);

        // overwrite the 5th nop with a b (branch)
        int branch_offset = basePC - nopPC - 4 * 4 + 4 * 22; // distance from the 5th nop to the end of this method
        if (-32768 <= branch_offset && branch_offset <= 32767) {
            // b
            int b_hi = (18 << 26) >> 16;
            xasm.emit_li(R12, branch_offset);
            xasm.emit_oris(R12, R12, b_hi);
            xasm.emit_stw(R12, R11, 16);
        } else {
            throw new Error();
        }

        xasm.emit_li(R12, 0);
        xasm.emit_dcbf(R11, R12);     // flush i-cache and so on
        xasm.emit_sync();
        xasm.emit_icbl(R11, R12);
        xasm.emit_isync();
    }

    // Self-modify 1 nop into a b (branch)
    // R11 and R12 are destroyed.
    protected void selfModify_b(int nopPC) {
        xasm.emit_b_i(1, false, true); // call the next instruction
        int basePC = xasm.getPC();
        xasm.emit_mflr(R11);
        xasm.emit_addi(R11, R11, -(basePC - nopPC)); // R11 - absolute nopPC

        // overwrite the nop with a b (branch)
        int branch_offset = basePC - nopPC + 4 * 10; // distance from the nop
                                                        // to the end of this
                                                        // method
        if (-32768 <= branch_offset && branch_offset <= 32767) {
            // b
            int b_hi = (18 << 26) >> 16;
            xasm.emit_li(R12, branch_offset);
            xasm.emit_oris(R12, R12, b_hi);
            xasm.emit_stw(R12, R11, 0);
        } else {
            throw new Error();
        }

        xasm.emit_li(R12, 0);
        xasm.emit_dcbf(R11, R12); // flush i-cache and so on
        xasm.emit_sync();
        xasm.emit_icbl(R11, R12);
        xasm.emit_isync();
    }

    public void visit(Instruction.INVOKEVIRTUAL_QUICK i) {
        throw new Error("INVOKEVIRTUAL_QUICK not implemented");
    }
    public void visit(Instruction.INVOKENONVIRTUAL_QUICK i) {
        throw new Error("INVOKENONVIRTUAL_QUICK not implemented");
    }
    public void visit(Instruction.INVOKEINTERFACE_QUICK i) {
        throw new Error("INVOKEINTERFACE_QUICK not implemented");
    }
    public void visit(Instruction.INVOKESUPER_QUICK i) {
        throw new Error("INVOKESUPER_QUICK not implemented");
    }
    public void visit(Instruction.INVOKEVIRTUAL i) {
        int cpindex = i.getCPIndex(buf);
        Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isInstanceMethodResolved(cpindex)) {
            try {
                ConstantResolvedInstanceMethodref ifi = cp.resolveInstanceMethod(cpindex);
                int vtbl_index = ifi.getOffset();
                int offset_cf_in_vtbl = getArrayElementOffset(executiveDomain, OBJECT, vtbl_index);
                loadArguments(desc, true);
                generateNullCheck(R4);
                xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R30, R4)); // R30 = receiver's bp
                xasm.emit_lwz(R29, R30, precomputed.offset_vtbl_in_bp);           // R29 = vtbl
                xasm.emit_lwz(R3, R29, offset_cf_in_vtbl);                        // R3 = code
                xasm.emit_lwz(R28, R3, precomputed.offset_code_in_cf);            // R28 = code pointer
                xasm.emit_mtctr(R28);   
                saveVolatileRegisters();
                xasm.emit_bctr_lk();
                restoreVolatileRegisters();
                unloadReturnValue(desc.getType().getTypeTag());
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a li32
	    xasm.emit_nop(); //
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveInstanceMethod_index,
                    precomputed.csa_resolveInstanceMethod_desc);
            xasm.emit_pop(R3); // R3 = vtbl_index
            if (precomputed.eObjectArrayElementSize == 4) {
                xasm.emit_slwi(R29, R3, 2);
            } else {
                xasm.emit_mulli(R29, R3, precomputed.eObjectArrayElementSize);
            }
            xasm.emit_addi(R28, R29, precomputed.eObjectArrayHeaderSize);

	    selfModify_lis_ori_and_b(R28, nopPC);

	    // the branch from the 3rd nop comes here

            loadArguments(desc, true);
            generateNullCheck(R4);
            xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R27, R4)); // R27 = receiver's bp
            xasm.emit_lwz(R26, R27, precomputed.offset_vtbl_in_bp);           // R26 = vtbl
            xasm.emit_lwzx(R3, R26, R28);                                     // R3 = code
            xasm.emit_lwz(R25, R3, precomputed.offset_code_in_cf);            // R28 = code pointer
            xasm.emit_mtctr(R25);   
            saveVolatileRegisters();
            xasm.emit_bctr_lk();
            restoreVolatileRegisters();
            unloadReturnValue(desc.getType().getTypeTag());
        }
    }

    public void visit(Instruction.INVOKESTATIC i) {
        int cpindex = i.getCPIndex(buf);
        int offset_methodref_in_cpvalues = getArrayElementOffset(executiveDomain, OBJECT, cpindex);
        Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
        int frame_size = stackLayout.getNativeFrameSize();
        
        if (precomputed.isExecutive) {
	    //	    || cp.isStaticMethodResolved(cpindex)) { // ED
            try {
                ConstantResolvedStaticMethodref ifi = cp
                        .resolveStaticMethod(cpindex);
                int nvtbl_index = ifi.getOffset();
                int offset_cf_in_nvtbl = getArrayElementOffset(executiveDomain,
                        OBJECT, nvtbl_index);
                loadArguments(desc, false);
                if (ED_OBJECT_DONT_MOVE) {
		    int nopPC = xasm.getPC();
		    xasm.emit_nop(); // will become a li32
		    xasm.emit_nop(); //
		    xasm.emit_nop(); // will become a li32
		    xasm.emit_nop(); //
		    xasm.emit_nop(); // will become a branch

                    xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);      // R30 = current code
                    xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);           // R29 = cp
                    xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);       // R28 = cpvalues
                    xasm.emit_lwz(R27, R28, offset_methodref_in_cpvalues);          // R27 = target methodref
                    xasm.emit_lwz(R4, R27, precomputed.offset_shst_in_staticmref);  // R4  = shared state (receiver)
                    xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R26, R4));// R26 = target bp
                    xasm.emit_lwz(R25, R26, precomputed.offset_nvtbl_in_bp);        // R25 = nvtbl
                    xasm.emit_lwz(R3, R25, offset_cf_in_nvtbl);                     // R3 = next code

		    selfModify_lis_ori_lis_ori_and_b(R3, R4, nopPC);

                    xasm.emit_lwz(R24, R3, precomputed.offset_code_in_cf);          // R24 = code pointer
                    xasm.emit_mtctr(R24);   
                } else {
                    xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);      // R30 = current code
                    xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);           // R29 = cp
                    xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);       // R28 = cpvalues
                    xasm.emit_lwz(R27, R28, offset_methodref_in_cpvalues);          // R27 = target methodref
                    xasm.emit_lwz(R4, R27, precomputed.offset_shst_in_staticmref);  // R4  = shared state (receiver)
                    xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R26, R4));// R26 = target bp
                    xasm.emit_lwz(R25, R26, precomputed.offset_nvtbl_in_bp);        // R25 = nvtbl
                    xasm.emit_lwz(R3, R25, offset_cf_in_nvtbl);                     // R3 = next code
                    xasm.emit_lwz(R24, R3, precomputed.offset_code_in_cf);          // R24 = code pointer
                    xasm.emit_mtctr(R24);   
                }
		saveVolatileRegisters();
		xasm.emit_bctr_lk();
		restoreVolatileRegisters();
		unloadReturnValue(desc.getType().getTypeTag());
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    if (ED_OBJECT_DONT_MOVE) {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a branch

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
		xasm.emit_li32(R5, cpindex);
		generateCSACall(precomputed.csa_resolveStaticMethod_index,
				precomputed.csa_resolveStaticMethod_desc);
		xasm.emit_pop(R3); // R3 = vtbl_index
		if (precomputed.eObjectArrayElementSize == 4) {
		    xasm.emit_slwi(R29, R3, 2);
		} else {
		    xasm.emit_mulli(R29, R3, precomputed.eObjectArrayElementSize);
		}
		xasm.emit_addi(R23, R29, precomputed.eObjectArrayHeaderSize);

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);      // R30 = current code
		xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);           // R29 = cp
		xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);       // R28 = cpvalues
		xasm.emit_lwz(R27, R28, offset_methodref_in_cpvalues);          // R27 = target methodref
		xasm.emit_lwz(R4, R27, precomputed.offset_shst_in_staticmref);  // R4  = shared state (receiver)
		xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R26, R4));// R26 = target bp
		xasm.emit_lwz(R25, R26, precomputed.offset_nvtbl_in_bp);        // R25 = nvtbl
		xasm.emit_lwzx(R3, R25, R23);                     // R3 = next code

		selfModify_lis_ori_lis_ori_and_b(R3, R4, nopPC);

		loadArguments(desc, false);
		xasm.emit_lwz(R24, R3, precomputed.offset_code_in_cf);          // R24 = code pointer
		xasm.emit_mtctr(R24);   

	    } else {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a branch

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
		xasm.emit_li32(R5, cpindex);
		generateCSACall(precomputed.csa_resolveStaticMethod_index,
				precomputed.csa_resolveStaticMethod_desc);
		xasm.emit_pop(R3); // R3 = vtbl_index
		if (precomputed.eObjectArrayElementSize == 4) {
		    xasm.emit_slwi(R29, R3, 2);
		} else {
		    xasm.emit_mulli(R29, R3, precomputed.eObjectArrayElementSize);
		}
		xasm.emit_addi(R23, R29, precomputed.eObjectArrayHeaderSize);

		selfModify_lis_ori_and_b(R23, nopPC);

		loadArguments(desc, false);
		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);      // R30 = current code
		xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);           // R29 = cp
		xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);       // R28 = cpvalues
		xasm.emit_lwz(R27, R28, offset_methodref_in_cpvalues);          // R27 = target methodref
		xasm.emit_lwz(R4, R27, precomputed.offset_shst_in_staticmref);  // R4  = shared state (receiver)
		xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R26, R4));// R26 = target bp
		xasm.emit_lwz(R25, R26, precomputed.offset_nvtbl_in_bp);        // R25 = nvtbl
		xasm.emit_lwzx(R3, R25, R23);                     // R3 = next code
		xasm.emit_lwz(R24, R3, precomputed.offset_code_in_cf);          // R24 = code pointer
		xasm.emit_mtctr(R24);   
	    }
            saveVolatileRegisters();
            xasm.emit_bctr_lk();
            restoreVolatileRegisters();
            unloadReturnValue(desc.getType().getTypeTag());
        }
    }
    
    public void visit(Instruction.INVOKESPECIAL i) {
        int cpindex = i.getCPIndex(buf);
        Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
        int offset_methodref_in_cpvalues = getArrayElementOffset(executiveDomain, OBJECT, cpindex);
        int frame_size = stackLayout.getNativeFrameSize();

        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isInstanceMethodResolved(cpindex)) {
            try {
                ConstantResolvedInstanceMethodref imi = cp
                        .resolveInstanceMethod(cpindex);
                if (imi.isNonVirtual) { // NONVIRTUAL2_QUICK
                    int nvtbl_index = imi.getOffset();
                    int offset_cf_in_nvtbl = getArrayElementOffset(executiveDomain, OBJECT, nvtbl_index);
                    loadArguments(desc, true);
                    generateNullCheck(R4);
                    if (ED_OBJECT_DONT_MOVE) { // fast
			int nopPC = xasm.getPC();
			xasm.emit_nop(); // will become a li32
			xasm.emit_nop(); //
			xasm.emit_nop(); // will become a branch
                        xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);     // R30 = current code
                        xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);          // R29 = cp
                        xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);      // R28 = cpvalues
                        xasm.emit_lwz(R27, R28, offset_methodref_in_cpvalues);         // R27 = target methodref
                        xasm.emit_lwz(R26, R27, precomputed.offset_bp_in_instancemref);         // R26 = target bp
                        xasm.emit_lwz(R25, R26, precomputed.offset_nvtbl_in_bp);       // R25 = nvtbl
                        xasm.emit_lwz(R3, R25, offset_cf_in_nvtbl);                    // R3 = next code
			
			selfModify_lis_ori_and_b(R3, nopPC);

                        xasm.emit_lwz(R24, R3, precomputed.offset_code_in_cf);         // R24 = code pointer
                        xasm.emit_mtctr(R24);   
                    } else { // slow
                        xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);     // R30 = current code
                        xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);          // R29 = cp
                        xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);      // R28 = cpvalues
                        xasm.emit_lwz(R27, R28, offset_methodref_in_cpvalues);         // R27 = target methodref
                        xasm.emit_lwz(R26, R27, precomputed.offset_bp_in_instancemref);         // R26 = target bp
                        xasm.emit_lwz(R25, R26, precomputed.offset_nvtbl_in_bp);       // R25 = nvtbl
                        xasm.emit_lwz(R3, R25, offset_cf_in_nvtbl);                    // R3 = next code
                        xasm.emit_lwz(R24, R3, precomputed.offset_code_in_cf);         // R24 = code pointer
                        xasm.emit_mtctr(R24);   
                    }
		    saveVolatileRegisters();
		    xasm.emit_bctr_lk();
		    restoreVolatileRegisters();
		    unloadReturnValue(desc.getType().getTypeTag());
                } else { // SUPER_QUICK
                    int vtbl_index = imi.getOffset();
                    int offset_cf_in_vtbl = getArrayElementOffset(executiveDomain, OBJECT, vtbl_index);
                    loadArguments(desc, true);
                    generateNullCheck(R4);
                    if (ED_OBJECT_DONT_MOVE) {
			int nopPC = xasm.getPC();
			xasm.emit_nop(); // will become a li32
			xasm.emit_nop(); //
			xasm.emit_nop(); // will become a branch
                        xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);     // R30 = current code
                        xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);          // R29 = cp
                        xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);      // R28 = cpvalues
                        xasm.emit_lwz(R27, R28, offset_methodref_in_cpvalues);         // R27 = target methodref
                        xasm.emit_lwz(R26, R27, precomputed.offset_bp_in_instancemref);         // R26 = target bp
                        xasm.emit_lwz(R25, R26, precomputed.offset_vtbl_in_bp);        // R25 = vtbl
                        xasm.emit_lwz(R3, R25, offset_cf_in_vtbl);                     // R3 = next code

			selfModify_lis_ori_and_b(R3, nopPC);

                        xasm.emit_lwz(R24, R3, precomputed.offset_code_in_cf);         // R24 = code pointer
                        xasm.emit_mtctr(R24);
                    } else {
                        xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);     // R30 = current code
                        xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);          // R29 = cp
                        xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);      // R28 = cpvalues
                        xasm.emit_lwz(R27, R28, offset_methodref_in_cpvalues);         // R27 = target methodref
                        xasm.emit_lwz(R26, R27, precomputed.offset_bp_in_instancemref);         // R26 = target bp
                        xasm.emit_lwz(R25, R26, precomputed.offset_vtbl_in_bp);        // R25 = vtbl
                        xasm.emit_lwz(R3, R25, offset_cf_in_vtbl);                     // R3 = next code
                        xasm.emit_lwz(R24, R3, precomputed.offset_code_in_cf);         // R24 = code pointer
                        xasm.emit_mtctr(R24);
                    }
		    saveVolatileRegisters();
		    xasm.emit_bctr_lk();
		    restoreVolatileRegisters();
		    unloadReturnValue(desc.getType().getTypeTag());
                }
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }

        } else { // UD
	    if (ED_OBJECT_DONT_MOVE) {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a branch

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
		xasm.emit_li32(R5, cpindex);
		generateCSACall(precomputed.csa_resolveInstanceMethod_index,
				precomputed.csa_resolveInstanceMethod_desc);
		xasm.emit_pop(R3); // R3 = vtbl_index
		if (precomputed.eObjectArrayElementSize == 4) {
		    xasm.emit_slwi(R29, R3, 2);
		} else {
		    xasm.emit_mulli(R29, R3, precomputed.eObjectArrayElementSize);
		}
		xasm.emit_addi(R28, R29, precomputed.eObjectArrayHeaderSize);
		// R28 - offset in table

		xasm.emit_lwz(R27, SP, frame_size + offset_code_in_stack);      // R30 = current code
		xasm.emit_lwz(R26, R27, precomputed.offset_cp_in_cf);           // R29 = cp
		xasm.emit_lwz(R25, R26, precomputed.offset_values_in_cp);       // R28 = cpvalues
		xasm.emit_lwz(R24, R25, offset_methodref_in_cpvalues);          // R27 = target methodref
		xasm.emit_lwz(R23, R24, precomputed.offset_nonvirtual_in_instancemref);
		xasm.emit_cmpi(CR7, false, R23, 0);
		Branch bnv2orsp = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		// NONVIRTUAL2_QUICK
		xasm.emit_li32(R22, precomputed.offset_nvtbl_in_bp);
		Branch bnv2orsp2 = xasm.emit_b_d();
		xasm.setBranchTarget(bnv2orsp);
		// SUPER_QUICK
		xasm.emit_li32(R22, precomputed.offset_vtbl_in_bp);
		xasm.setBranchTarget(bnv2orsp2);
		// R22 - table offset in bp
		xasm.emit_lwz(R21, R24, precomputed.offset_bp_in_instancemref); // R21 = target bp
		xasm.emit_lwzx(R20, R21, R22); // R20 = table
		xasm.emit_lwzx(R3, R20, R28); // R3 = code

		selfModify_lis_ori_and_b(R3, nopPC);

		loadArguments(desc, true);
		generateNullCheck(R4);

		xasm.emit_lwz(R19, R3, precomputed.offset_code_in_cf);         // R19 = code pointer
		xasm.emit_mtctr(R19);
		saveVolatileRegisters();
		xasm.emit_bctr_lk();
		restoreVolatileRegisters();
		unloadReturnValue(desc.getType().getTypeTag());
	    } else {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a branch

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
		xasm.emit_li32(R5, cpindex);
		generateCSACall(precomputed.csa_resolveInstanceMethod_index,
				precomputed.csa_resolveInstanceMethod_desc);
		xasm.emit_pop(R3); // R3 = vtbl_index
		if (precomputed.eObjectArrayElementSize == 4) {
		    xasm.emit_slwi(R29, R3, 2);
		} else {
		    xasm.emit_mulli(R29, R3, precomputed.eObjectArrayElementSize);
		}
		xasm.emit_addi(R28, R29, precomputed.eObjectArrayHeaderSize);
		// R28 - offset in table

		selfModify_lis_ori_and_b(R28, nopPC);

		xasm.emit_lwz(R27, SP, frame_size + offset_code_in_stack);      // R30 = current code
		xasm.emit_lwz(R26, R27, precomputed.offset_cp_in_cf);           // R29 = cp
		xasm.emit_lwz(R25, R26, precomputed.offset_values_in_cp);       // R28 = cpvalues
		xasm.emit_lwz(R24, R25, offset_methodref_in_cpvalues);          // R27 = target methodref
		xasm.emit_lwz(R23, R24, precomputed.offset_nonvirtual_in_instancemref);
		xasm.emit_cmpi(CR7, false, R23, 0);
		Branch bnv2orsp = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		// NONVIRTUAL2_QUICK
		xasm.emit_li32(R22, precomputed.offset_nvtbl_in_bp);
		Branch bnv2orsp2 = xasm.emit_b_d();
		xasm.setBranchTarget(bnv2orsp);
		// SUPER_QUICK
		xasm.emit_li32(R22, precomputed.offset_vtbl_in_bp);
		xasm.setBranchTarget(bnv2orsp2);
		// R22 - table offset in bp
		loadArguments(desc, true);
		generateNullCheck(R4);
		xasm.emit_lwz(R21, R24, precomputed.offset_bp_in_instancemref); // R21 = target bp
		xasm.emit_lwzx(R20, R21, R22); // R20 = table
		xasm.emit_lwzx(R3, R20, R28); // R3 = code
		xasm.emit_lwz(R19, R3, precomputed.offset_code_in_cf);         // R19 = code pointer
		xasm.emit_mtctr(R19);
		saveVolatileRegisters();
		xasm.emit_bctr_lk();
		restoreVolatileRegisters();
		unloadReturnValue(desc.getType().getTypeTag());
	    }
        }
    }

    public void visit(Instruction.INVOKEINTERFACE i) {
        int cpindex = i.getCPIndex(buf);
        Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isInterfaceMethodResolved(cpindex)) {
            try {
                ConstantResolvedInterfaceMethodref ifi = cp.resolveInterfaceMethod(cpindex);
                int iftbl_index = ifi.getOffset();
                int offset_cf_in_iftbl = getArrayElementOffset(executiveDomain, OBJECT, iftbl_index);
                loadArguments(desc, true);
                generateNullCheck(R4);
                xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R30, R4)); // R30 = receiver's bp
                xasm.emit_lwz(R29, R30, precomputed.offset_iftbl_in_bp);           // R29 = iftbl
                xasm.emit_lwz(R3, R29, offset_cf_in_iftbl);                        // R3 = code
                xasm.emit_lwz(R28, R3, precomputed.offset_code_in_cf);            // R28 = code pointer
                xasm.emit_mtctr(R28);   
                saveVolatileRegisters();
                xasm.emit_bctr_lk();
                restoreVolatileRegisters();
                unloadReturnValue(desc.getType().getTypeTag());
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a li32
	    xasm.emit_nop(); //
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveInterfaceMethod_index,
                    precomputed.csa_resolveInterfaceMethod_desc);
            xasm.emit_pop(R3); // R3 = iftbl_index
            if (precomputed.eObjectArrayElementSize == 4) {
                xasm.emit_slwi(R29, R3, 2);
            } else {
                xasm.emit_mulli(R29, R3, precomputed.eObjectArrayElementSize);
            }
            xasm.emit_addi(R28, R29, precomputed.eObjectArrayHeaderSize);

	    selfModify_lis_ori_and_b(R28, nopPC);

            loadArguments(desc, true);
            generateNullCheck(R4);
            xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R27, R4)); // R27 = receiver's bp
            xasm.emit_lwz(R26, R27, precomputed.offset_iftbl_in_bp);           // R26 = iftbl
            xasm.emit_lwzx(R3, R26, R28);                                     // R3 = code
            xasm.emit_lwz(R25, R3, precomputed.offset_code_in_cf);            // R28 = code pointer
            xasm.emit_mtctr(R25);   
            saveVolatileRegisters();
            xasm.emit_bctr_lk();
            restoreVolatileRegisters();
            unloadReturnValue(desc.getType().getTypeTag());
        }
    }


    public void visit(Instruction.INVOKE_NATIVE i) {
        int mindex = i.getMethodIndex(buf);
        UnboundSelector.Method[] argList = compilerVMInterface.getNativeMethodList();
        if (mindex < 0 || mindex >= argList.length) {
            throw new OVMError.Internal("Invoke native argument " + 
                        Integer.toString(mindex) + 
                        " is somehow out of range");
        }
        UnboundSelector.Method m = argList[mindex];
        Descriptor.Method desc = m.getDescriptor();

        if (INLINE_SOME_IN_INVOKENATIVE) {
            if (m.toString().equals("eventsSetEnabled:(Z)V")) {
                int eventUnionIndex = compilerVMInterface
                        .getRuntimeFunctionIndex("eventUnion");
                if (eventUnionIndex == -1) {
                    throw new Error();
                }
                /* hand inlining of eventsSetEnabled */
                xasm.emit_li32(R30, precomputed.runtimeFunctionTableHandle.asInt());
                xasm.emit_lwz(R29, R30, 0);
                xasm.emit_lwz(R28, R29, eventUnionIndex * 4);
                xasm.emit_lwz(R27, R28, 0); // load old eventUion
                xasm.emit_li32(R23, 0xFFFF0000);
                xasm.emit_and(R26, R27, R23); // extract notSignaled
                xasm.emit_pop(R25); // pop new notEnalbed
                xasm.emit_cmpi(CR7, false, R25, 0);// compute !notEnabled
                Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
                xasm.emit_li32(R25, 0);
                Branch b1 = xasm.emit_b_d();
                xasm.setBranchTarget(b0);
                xasm.emit_li32(R25, 0x00000001);
                xasm.setBranchTarget(b1);
                xasm.emit_or(R24, R25, R26); // new eventUnion
                xasm.emit_stw(R24, R28, 0); // store new eventUnion
                return;
            }
        }
        // invoke_native() expects the argument in the reverse order with the mindex as the first argument
        loadArgumentsForInvokeNative(desc);
        xasm.emit_li32(R3, mindex);
        generateCFunctionCall("invoke_native");
        unloadReturnValueForInvokeNative(desc.getType().getTypeTag());
    }

    public void visit(Instruction.GETFIELD i) {
        int cpindex = i.getCPIndex(buf);
        Selector.Field sel = i.getSelector(buf, cp);
        boolean isWidePrimitive = sel.getDescriptor().isWidePrimitive();
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isInstanceFieldResolved(cpindex)) {
            try {
                ConstantResolvedInstanceFieldref ifi = cp
                        .resolveInstanceField(cpindex);
                S3Field field = (S3Field) ifi.getField();
                int offset = field.getOffset();
                xasm.emit_pop(R30);
                generateNullCheck(R30);
                if (isWidePrimitive) {
                    xasm.emit_lwz(R29, R30, offset + 4);
                    xasm.emit_push(R29);
                }
                xasm.emit_lwz(R28, R30, offset);
                xasm.emit_push(R28);
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a li32
	    xasm.emit_nop(); //
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack); // get code fragment
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf); // ECX = S3Constants
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveInstanceField_index, 
                    precomputed.csa_resolveInstanceField_desc);
            xasm.emit_pop(R3); // R3 = field offset

	    selfModify_lis_ori_and_b(R3, nopPC);

            xasm.emit_pop(R29); // receiver
            generateNullCheck(R29);
            if (isWidePrimitive) {
                xasm.emit_addi(R28, R3, 4);
                xasm.emit_lwzx(R27, R29, R28);
                xasm.emit_push(R27);
            }
            xasm.emit_lwzx(R28, R29, R3);
            xasm.emit_push(R28);
        }
    }
    
    public void visit(Instruction.PUTFIELD i) {
        if (i instanceof Instruction.PUTFIELD_WITH_BARRIER_REF)
            return;

        int cpindex = i.getCPIndex(buf);
        Selector.Field sel = i.getSelector(buf, cp);
        boolean isWidePrimitive = sel.getDescriptor().isWidePrimitive();
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isInstanceFieldResolved(cpindex)) {
            try {
                ConstantResolvedInstanceFieldref ifi = cp
                        .resolveInstanceField(cpindex);
                S3Field field = (S3Field) ifi.getField();
                int offset = field.getOffset();
                if (isWidePrimitive) {
                    xasm.emit_pop(R30);
                    xasm.emit_pop(R29);
                    xasm.emit_pop(R28);
                    generateNullCheck(R28);
                    xasm.emit_stw(R30, R28, offset);
                    xasm.emit_stw(R29, R28, offset + 4);
                } else {
                    xasm.emit_pop(R30);
                    xasm.emit_pop(R29);
                    generateNullCheck(R29);
                    xasm.emit_stw(R30, R29, offset);
                }
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a li32
	    xasm.emit_nop(); //
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack); // get code fragment
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf); // ECX = S3Constants
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveInstanceField_index, 
                    precomputed.csa_resolveInstanceField_desc);
            xasm.emit_pop(R3); // R3 = field offset

	    selfModify_lis_ori_and_b(R3, nopPC);

            if (isWidePrimitive) {
                xasm.emit_pop(R29);
                xasm.emit_pop(R28);
                xasm.emit_pop(R27); // receiver
                generateNullCheck(R27);
                xasm.emit_addi(R26, R3, 4);
                xasm.emit_stwx(R29, R27, R3);
                xasm.emit_stwx(R28, R27, R26);
            } else {
                xasm.emit_pop(R28);
                xasm.emit_pop(R27); // receiver
                generateNullCheck(R27);
                xasm.emit_stwx(R28, R27, R3);
            }
        }
    }

    public void visit(Instruction.GETSTATIC i) {
        int cpindex = i.getCPIndex(buf);
        int offset_fieldref_in_cpvalues = getArrayElementOffset(executiveDomain, OBJECT, cpindex);
        Selector.Field sel = i.getSelector(buf, cp);
        boolean isWidePrimitive = sel.getDescriptor().isWidePrimitive();
        int frame_size = stackLayout.getNativeFrameSize();
        
        if (precomputed.isExecutive) {
	    //	    || cp.isStaticFieldResolved(cpindex)) {
            try {
                ConstantResolvedStaticFieldref sfi = cp.resolveStaticField(cpindex);
                S3Field field = (S3Field) sfi.getField();
                int offset = field.getOffset();
                if (ED_OBJECT_DONT_MOVE) {
		    int nopPC = xasm.getPC();
		    xasm.emit_nop(); // will become a li32
		    xasm.emit_nop(); //
		    xasm.emit_nop(); // will become a branch

                    xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
                    xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
                    xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
                    xasm.emit_lwz(R27, R28, offset_fieldref_in_cpvalues);
                    xasm.emit_lwz(R26, R27, precomputed.offset_shst_in_staticfref);

		    selfModify_lis_ori_and_b(R26, nopPC);
		    
                    if (isWidePrimitive) {
                        xasm.emit_lwz(R25, R26, offset + 4);
                        xasm.emit_push(R25);
                    }
                    xasm.emit_lwz(R25, R26, offset);
                    xasm.emit_push(R25);
                } else {
                    xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
                    xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
                    xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
                    xasm.emit_lwz(R27, R28, offset_fieldref_in_cpvalues);
                    xasm.emit_lwz(R26, R27, precomputed.offset_shst_in_staticfref);
                    if (isWidePrimitive) {
                        xasm.emit_lwz(R25, R26, offset + 4);
                        xasm.emit_push(R25);
                    }
                    xasm.emit_lwz(R24, R26, offset);
                    xasm.emit_push(R24);
                }
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    if (ED_OBJECT_DONT_MOVE) {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a branch

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack); // get code fragment
		xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf); // ECX = S3Constants
		xasm.emit_li32(R5, cpindex);
		generateCSACall(precomputed.csa_resolveStaticField_index, 
				precomputed.csa_resolveStaticField_desc);
		xasm.emit_pop(R3); // R3 = field offset

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
		xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
		xasm.emit_lwz(R27, R28, offset_fieldref_in_cpvalues);
		xasm.emit_lwz(R26, R27, precomputed.offset_shst_in_staticfref);
		xasm.emit_add(R25, R26, R3);

		selfModify_lis_ori_and_b(R25, nopPC);

		if (isWidePrimitive) {
		    xasm.emit_lwz(R23, R25, 4);
		    xasm.emit_push(R23);
		}
		xasm.emit_lwz(R23, R25, 0);
		xasm.emit_push(R23);

	    } else {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a branch

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack); // get code fragment
		xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf); // ECX = S3Constants
		xasm.emit_li32(R5, cpindex);
		generateCSACall(precomputed.csa_resolveStaticField_index, 
				precomputed.csa_resolveStaticField_desc);
		xasm.emit_pop(R3); // R3 = field offset

		selfModify_lis_ori_and_b(R3, nopPC);

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
		xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
		xasm.emit_lwz(R27, R28, offset_fieldref_in_cpvalues);
		xasm.emit_lwz(R26, R27, precomputed.offset_shst_in_staticfref);
		if (isWidePrimitive) {
		    xasm.emit_addi(R24, R3, 4);
		    xasm.emit_lwzx(R23, R26, R24);
		    xasm.emit_push(R23);
		}
		xasm.emit_lwzx(R23, R26, R3);
		xasm.emit_push(R23);
	    }
        }
    }

    // This handles PUTSTATIC_WITH_BARRIER_REF, too
    public void visit(Instruction.PUTSTATIC i) {
        int cpindex = i.getCPIndex(buf);
        int offset_fieldref_in_cpvalues = getArrayElementOffset(executiveDomain, OBJECT, cpindex);
        Selector.Field sel = i.getSelector(buf, cp);
        boolean isWidePrimitive = sel.getDescriptor().isWidePrimitive();
        int frame_size = stackLayout.getNativeFrameSize();
        
        if (precomputed.isExecutive) {
	    //	    || cp.isStaticFieldResolved(cpindex)) {
            try {
                ConstantResolvedStaticFieldref sfi = cp.resolveStaticField(cpindex);
                S3Field field = (S3Field) sfi.getField();
                int offset = field.getOffset();

                if (ED_OBJECT_DONT_MOVE) {
		    int nopPC = xasm.getPC();
		    xasm.emit_nop(); // will become a li32
		    xasm.emit_nop(); //
		    xasm.emit_nop(); // will become a branch

                    xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
                    xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
                    xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
                    xasm.emit_lwz(R27, R28, offset_fieldref_in_cpvalues);
                    xasm.emit_lwz(R5, R27, precomputed.offset_shst_in_staticfref);

                    selfModify_lis_ori_and_b(R5, nopPC);

                    if (isWidePrimitive) {
                        xasm.emit_pop(R25);
                        xasm.emit_pop(R24);
                        xasm.emit_stw(R25, R5, offset);
                        xasm.emit_stw(R24, R5, offset + 4);
                    } else {
                        if (i instanceof Instruction.PUTSTATIC_WITH_BARRIER_REF) {
                            xasm.emit_li32(R6, offset);
                            xasm.emit_pop(R7);
                            generateCSACall(precomputed.csa_putFieldBarrier_index,
                                    precomputed.csa_putFieldBarrier_desc);
                        } else {
                            xasm.emit_pop(R25);
                            xasm.emit_stw(R25, R5, offset);
                        }
                    }

                } else {
                    xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
                    xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
                    xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
                    xasm.emit_lwz(R27, R28, offset_fieldref_in_cpvalues);
                    xasm.emit_lwz(R5, R27, precomputed.offset_shst_in_staticfref);
                    
                    if (isWidePrimitive) {
                        xasm.emit_pop(R25);
                        xasm.emit_pop(R24);
                        xasm.emit_stw(R25, R5, offset);
                        xasm.emit_stw(R24, R5, offset + 4);
                    } else {
                        if (i instanceof Instruction.PUTSTATIC_WITH_BARRIER_REF) {
                            xasm.emit_li32(R6, offset);
                            xasm.emit_pop(R7);
                            generateCSACall(precomputed.csa_putFieldBarrier_index,
                                    precomputed.csa_putFieldBarrier_desc);
                        } else {
                            xasm.emit_pop(R25);
                            xasm.emit_stw(R25, R5, offset);
                        }
                    }
                }
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    if (ED_OBJECT_DONT_MOVE) {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a branch

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack); // get code fragment
		xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf); // ECX = S3Constants
		xasm.emit_li32(R5, cpindex);
		generateCSACall(precomputed.csa_resolveStaticField_index, 
				precomputed.csa_resolveStaticField_desc);
		xasm.emit_pop(R6); // R6 = field offset

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
		xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
		xasm.emit_lwz(R27, R28, offset_fieldref_in_cpvalues);
		xasm.emit_lwz(R5, R27, precomputed.offset_shst_in_staticfref);

		selfModify_lis_ori_lis_ori_and_b(R5, R6, nopPC);

		if (i instanceof Instruction.PUTSTATIC_WITH_BARRIER_REF) {
		    xasm.emit_pop(R7);//value
		    generateCSACall(precomputed.csa_putFieldBarrier_index,
				    precomputed.csa_putFieldBarrier_desc);
		} else {
		    if (isWidePrimitive) {
			xasm.emit_pop(R29);
			xasm.emit_pop(R28);
			xasm.emit_addi(R25, R6, 4);
			xasm.emit_stwx(R29, R5, R6);
			xasm.emit_stwx(R28, R5, R25);
		    } else {
			xasm.emit_pop(R28);
			xasm.emit_stwx(R28, R5, R6);
		    }
		}
	    } else {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a li32
		xasm.emit_nop(); //
		xasm.emit_nop(); // will become a branch

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack); // get code fragment
		xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf); // ECX = S3Constants
		xasm.emit_li32(R5, cpindex);
		generateCSACall(precomputed.csa_resolveStaticField_index, 
				precomputed.csa_resolveStaticField_desc);
		xasm.emit_pop(R3); // R3 = field offset

		selfModify_lis_ori_and_b(R3, nopPC);

		xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
		xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
		xasm.emit_lwz(R27, R28, offset_fieldref_in_cpvalues);
		xasm.emit_lwz(R5, R27, precomputed.offset_shst_in_staticfref);
		if (i instanceof Instruction.PUTSTATIC_WITH_BARRIER_REF) {
		    xasm.emit_mr(R6, R3);
		    xasm.emit_pop(R7);//value
		    generateCSACall(precomputed.csa_putFieldBarrier_index,
				    precomputed.csa_putFieldBarrier_desc);
		} else {
		    if (isWidePrimitive) {
			xasm.emit_pop(R29);
			xasm.emit_pop(R28);
			xasm.emit_addi(R25, R3, 4);
			xasm.emit_stwx(R29, R5, R3);
			xasm.emit_stwx(R28, R5, R25);
		    } else {
			xasm.emit_pop(R28);
			xasm.emit_stwx(R28, R5, R3);
		    }
		}
	    }
        }
    }
    
    public void visit(Instruction.GETFIELD_QUICK i) {
        int offset = i.getOffset(buf);
        xasm.emit_pop(R30);
        generateNullCheck(R30);
        xasm.emit_lwz(R29, R30, offset);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.REF_GETFIELD_QUICK i) {
        int offset = i.getOffset(buf);
        xasm.emit_pop(R30);
        generateNullCheck(R30);
        xasm.emit_lwz(R29, R30, offset);
        xasm.emit_push(R29);
    }

    // this handles PUTFIELD_QUICK_WITH_BARRIER_REF, too?
    public void visit(Instruction.PUTFIELD_QUICK i) {
        int offset = i.getOffset(buf);
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateNullCheck(R29);
        xasm.emit_stw(R30, R29, offset);
    }

    public void visit(Instruction.PUTFIELD_WITH_BARRIER_REF i) {
        int cpindex = i.getCPIndex(buf);
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isInstanceFieldResolved(cpindex)) {
            try {
                ConstantResolvedInstanceFieldref ifi = cp
                        .resolveInstanceField(cpindex);
                S3Field field = (S3Field) ifi.getField();
                int offset = field.getOffset();
                xasm.emit_pop(R7);
                xasm.emit_li32(R6, offset);
                xasm.emit_pop(R5);
                generateNullCheck(R5);
                generateCSACall(precomputed.csa_putFieldBarrier_index,
                        precomputed.csa_putFieldBarrier_desc);
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a li32
	    xasm.emit_nop(); //
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack); // get code fragment
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf); // ECX = S3Constants
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveInstanceField_index, 
                    precomputed.csa_resolveInstanceField_desc);
            xasm.emit_pop(R3); // R3 = field offset

	    selfModify_lis_ori_and_b(R3, nopPC);

            xasm.emit_mr(R6, R3);
            xasm.emit_pop(R7); // value
            xasm.emit_pop(R5); // receiver
            generateNullCheck(R5);
            generateCSACall(precomputed.csa_putFieldBarrier_index,
                    precomputed.csa_putFieldBarrier_desc);
        }
    }

    public void visit(Instruction.GETFIELD2_QUICK i) {
        int offset = i.getOffset(buf);
        xasm.emit_pop(R30);
        generateNullCheck(R30);
        xasm.emit_lwz(R29, R30, offset);
        xasm.emit_lwz(R28, R30, offset + 4);
        xasm.emit_push(R28);
        xasm.emit_push(R29);
    }

    public void visit(Instruction.PUTFIELD2_QUICK i) {
        int offset = i.getOffset(buf);
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        generateNullCheck(R28);
        xasm.emit_stw(R29, R28, offset);
        xasm.emit_stw(R30, R28, offset + 4);
    }

    
    public void visit(Instruction.IALOAD i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateNullCheck(R29);
        generateArrayBoundCheck(R29, R30);
        if (precomputed.tIntArrayElementSize == 4) {
            xasm.emit_slwi(R27, R30, 2);
        } else {
            xasm.emit_mulli(R27, R30, precomputed.tIntArrayElementSize);
        }
        xasm.emit_add(R28, R29, R27);
        xasm.emit_lwz(R26, R28, precomputed.tIntArrayHeaderSize);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.LALOAD i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateNullCheck(R29);
        generateArrayBoundCheck(R29, R30);
        if (precomputed.tLongArrayElementSize == 8) {
            xasm.emit_slwi(R27, R30, 3);
        } else {
            xasm.emit_mulli(R27, R30, precomputed.tLongArrayElementSize);
        }
        xasm.emit_add(R28, R29, R27);
        xasm.emit_lwz(R26, R28, precomputed.tLongArrayHeaderSize);
        xasm.emit_lwz(R25, R28, precomputed.tLongArrayHeaderSize + 4);
        xasm.emit_push(R25);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.FALOAD i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateNullCheck(R29);
        generateArrayBoundCheck(R29, R30);
        if (precomputed.tFloatArrayElementSize == 4) {
            xasm.emit_slwi(R27, R30, 2);
        } else {
            xasm.emit_mulli(R27, R30, precomputed.tFloatArrayElementSize);
        }
        xasm.emit_add(R28, R29, R27);
        xasm.emit_lwz(R26, R28, precomputed.tFloatArrayHeaderSize);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.DALOAD i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateNullCheck(R29);
        generateArrayBoundCheck(R29, R30);
        if (precomputed.tDoubleArrayElementSize == 8) {
            xasm.emit_slwi(R27, R30, 3);
        } else {
            xasm.emit_mulli(R27, R30, precomputed.tDoubleArrayElementSize);
        }
        xasm.emit_add(R28, R29, R27);
        xasm.emit_lwz(R26, R28, precomputed.tDoubleArrayHeaderSize);
        xasm.emit_lwz(R25, R28, precomputed.tDoubleArrayHeaderSize + 4);
        xasm.emit_push(R25);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.AALOAD i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateNullCheck(R29);
        generateArrayBoundCheck(R29, R30);
        if (precomputed.tObjectArrayElementSize == 4) {
            xasm.emit_slwi(R27, R30, 2);
        } else {
            xasm.emit_mulli(R27, R30, precomputed.tObjectArrayElementSize);
        }
        xasm.emit_add(R28, R29, R27);
        xasm.emit_lwz(R26, R28, precomputed.tObjectArrayHeaderSize);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.SALOAD i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateNullCheck(R29);
        generateArrayBoundCheck(R29, R30);
        if (precomputed.tShortArrayElementSize == 2) {
            xasm.emit_slwi(R27, R30, 1);
        } else {
            xasm.emit_mulli(R27, R30, precomputed.tShortArrayElementSize);
        }
        xasm.emit_add(R28, R29, R27);
        xasm.emit_lha(R26, R28, precomputed.tShortArrayHeaderSize);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.CALOAD i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateNullCheck(R29);
        generateArrayBoundCheck(R29, R30);
        if (precomputed.tCharArrayElementSize == 2) {
            xasm.emit_slwi(R27, R30, 1);
        } else {
            xasm.emit_mulli(R27, R30, precomputed.tCharArrayElementSize);
        }
        xasm.emit_add(R28, R29, R27);
        xasm.emit_lhz(R26, R28, precomputed.tCharArrayHeaderSize);
        xasm.emit_push(R26);
    }

    public void visit(Instruction.BALOAD i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        generateNullCheck(R29);
        generateArrayBoundCheck(R29, R30);
        if (precomputed.tByteArrayElementSize == 1) {
            xasm.emit_add(R28, R29, R30);
        } else {
            xasm.emit_mulli(R27, R30, precomputed.tByteArrayElementSize);
            xasm.emit_add(R28, R29, R27);
        }
        xasm.emit_lbz(R26, R28, precomputed.tByteArrayHeaderSize);
        xasm.emit_extsb(R25, R26);
        xasm.emit_push(R25);
    }

    public void visit(Instruction.IASTORE i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        generateNullCheck(R28);
        generateArrayBoundCheck(R28, R29);
        if (precomputed.tIntArrayElementSize == 4) {
            xasm.emit_slwi(R27, R29, 2);
        } else {
            xasm.emit_mulli(R27, R29, precomputed.tIntArrayElementSize);
        }
        xasm.emit_add(R26, R28, R27);
        xasm.emit_stw(R30, R26, precomputed.tIntArrayHeaderSize);
    }

    public void visit(Instruction.LASTORE i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        generateNullCheck(R27);
        generateArrayBoundCheck(R27, R28);
        if (precomputed.tLongArrayElementSize == 8) {
            xasm.emit_slwi(R26, R28, 3);
        } else {
            xasm.emit_mulli(R26, R28, precomputed.tLongArrayElementSize);
        }
        xasm.emit_add(R25, R27, R26);
        xasm.emit_stw(R30, R25, precomputed.tLongArrayHeaderSize);
        xasm.emit_stw(R29, R25, precomputed.tLongArrayHeaderSize + 4);
    }

    public void visit(Instruction.FASTORE i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        generateNullCheck(R28);
        generateArrayBoundCheck(R28, R29);
        if (precomputed.tFloatArrayElementSize == 4) {
            xasm.emit_slwi(R27, R29, 2);
        } else {
            xasm.emit_mulli(R27, R29, precomputed.tFloatArrayElementSize);
        }
        xasm.emit_add(R26, R28, R27);
        xasm.emit_stw(R30, R26, precomputed.tFloatArrayHeaderSize);
    }

    public void visit(Instruction.DASTORE i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        xasm.emit_pop(R27);
        generateNullCheck(R27);
        generateArrayBoundCheck(R27, R28);
        if (precomputed.tDoubleArrayElementSize == 8) {
            xasm.emit_slwi(R26, R28, 3);
        } else {
            xasm.emit_mulli(R26, R28, precomputed.tDoubleArrayElementSize);
        }
        xasm.emit_add(R25, R27, R26);
        xasm.emit_stw(R30, R25, precomputed.tDoubleArrayHeaderSize);
        xasm.emit_stw(R29, R25, precomputed.tDoubleArrayHeaderSize + 4);
    }

    public void visit(Instruction.UNCHECKED_AASTORE i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        generateNullCheck(R28);
        generateArrayBoundCheck(R28, R29);
        if (precomputed.tObjectArrayElementSize == 4) {
            xasm.emit_slwi(R27, R29, 2);
        } else {
            xasm.emit_mulli(R27, R29, precomputed.tObjectArrayElementSize);
        }
        xasm.emit_add(R26, R28, R27);
        xasm.emit_stw(R30, R26, precomputed.tObjectArrayHeaderSize);
    }

    public void visit(Instruction.AASTORE i) {
        if (i instanceof Instruction.AASTORE_WITH_BARRIER)
            return;

        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        generateNullCheck(R28);
        generateArrayBoundCheck(R28, R29);
        generateArrayStoreCheck(R28, R30);
        if (precomputed.tObjectArrayElementSize == 4) {
            xasm.emit_slwi(R27, R29, 2);
        } else {
            xasm.emit_mulli(R27, R29, precomputed.tObjectArrayElementSize);
        }
        xasm.emit_add(R26, R28, R27);
        xasm.emit_stw(R30, R26, precomputed.tObjectArrayHeaderSize);
    }

    public void visit(Instruction.AASTORE_WITH_BARRIER i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        generateNullCheck(R28);
        generateArrayBoundCheck(R28, R29);
        generateArrayStoreCheck(R28, R30);
        if (precomputed.tObjectArrayElementSize == 4) {
            xasm.emit_slwi(R27, R29, 2);
        } else {
            xasm.emit_mulli(R27, R29, precomputed.tObjectArrayElementSize);
        }
        xasm.emit_mr(R7, R30);
        xasm.emit_addi(R6, R27, precomputed.tObjectArrayHeaderSize);
        xasm.emit_mr(R5, R28);
        generateCSACall(precomputed.csa_aastoreBarrier_index,
                precomputed.csa_aastoreBarrier_desc);
    }


    public void visit(Instruction.READ_BARRIER i) {
        xasm.emit_pop(R5);
        generateCSACall(precomputed.csa_readBarrier_index,
                precomputed.csa_readBarrier_desc);
    }

    
    public void visit(Instruction.SASTORE i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        generateNullCheck(R28);
        generateArrayBoundCheck(R28, R29);
        if (precomputed.tShortArrayElementSize == 2) {
            xasm.emit_slwi(R27, R29, 1);
        } else {
            xasm.emit_mulli(R27, R29, precomputed.tShortArrayElementSize);
        }
        xasm.emit_add(R26, R28, R27);
        xasm.emit_sth(R30, R26, precomputed.tShortArrayHeaderSize);
    }

    public void visit(Instruction.CASTORE i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        generateNullCheck(R28);
        generateArrayBoundCheck(R28, R29);
        if (precomputed.tCharArrayElementSize == 2) {
            xasm.emit_slwi(R27, R29, 1);
        } else {
            xasm.emit_mulli(R27, R29, precomputed.tCharArrayElementSize);
        }
        xasm.emit_add(R26, R28, R27);
        xasm.emit_sth(R30, R26, precomputed.tCharArrayHeaderSize);
    }

    public void visit(Instruction.BASTORE i) {
        xasm.emit_pop(R30);
        xasm.emit_pop(R29);
        xasm.emit_pop(R28);
        generateNullCheck(R28);
        generateArrayBoundCheck(R28, R29);
        if (precomputed.tByteArrayElementSize == 1) {
            xasm.emit_add(R26, R28, R29);
        } else {
            xasm.emit_mulli(R27, R29, precomputed.tByteArrayElementSize);
            xasm.emit_add(R26, R28, R27);
        }
        xasm.emit_stb(R30, R26, precomputed.tByteArrayHeaderSize);
    }

    public void visit(Instruction.ARRAYLENGTH i) {
        xasm.emit_pop(R30);
        generateNullCheck(R30);
        switch(precomputed.tArrayLengthFieldSize) {
        case 2:
            xasm.emit_lhz(R29, R30, precomputed.tArrayLengthFieldOffset);
            break;
        case 4:
            xasm.emit_lwz(R29, R30, precomputed.tArrayLengthFieldOffset);
            break;
        default:
            throw new Error("Invalid array element size");
        }
        xasm.emit_push(R29);
    }
    
    // LDC, LDC_W, LDC2_W
    public void visit(Instruction.ConstantPoolLoad i) {
        if (!(i instanceof Instruction.LDC || i instanceof Instruction.LDC2_W
                || i instanceof Instruction.LDC_W
                || i instanceof Instruction.LDC_W_REF_QUICK || i instanceof Instruction.LDC_REF_QUICK))
            return;

        int frame_size = stackLayout.getNativeFrameSize();
        int cpindex = i.getCPIndex(buf);
        switch (cp.getTagAt(cpindex)) {
        case CONSTANT_Integer:
        case CONSTANT_Float:
            xasm.emit_li32(R30, cp.getValueAt(cpindex));
            xasm.emit_push(R30);
            break;
        case CONSTANT_Long:
        case CONSTANT_Double:
            long value = cp.getWideValueAt(cpindex);
            xasm.emit_li32(R30, (int)(value & 0xFFFFFFFFL));
            xasm.emit_li32(R29, (int)((value >> 32) & 0xFFFFFFFFL));
            xasm.emit_push(R30);
            xasm.emit_push(R29);
            break;
        case CONSTANT_String:
        case CONSTANT_Binder:
        case CONSTANT_SharedState: {
            if (precomputed.isExecutive || cp.isConstantResolved(cpindex)) {
                cp.resolveConstantAt(cpindex);
            } else {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveLdc_index,
                        precomputed.csa_resolveLdc_desc);

		selfModify_b(nopPC);
            }

            // fast path - LDC_QUICK
            int cpoffset = getArrayElementOffset(executiveDomain, OBJECT,
                    cpindex);

            if (false && ED_OBJECT_DONT_MOVE) { // Strings are moved by GC
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a lis32
		xasm.emit_nop();
		xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
                xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
                xasm.emit_lwz(R27, R28, cpoffset);

		selfModify_lis_ori_and_b(R27, nopPC);
                xasm.emit_push(R27);

            } else {
                xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
                xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
                xasm.emit_lwz(R27, R28, cpoffset);
                xasm.emit_push(R27);
            }
            break;
        }
        case CONSTANT_Reference:
            int cpoffset = getArrayElementOffset(executiveDomain, OBJECT,
                    cpindex);
            if (false && ED_OBJECT_DONT_MOVE) { // Strings and other objects are moved by GC
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a lis32
		xasm.emit_nop();
		xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
                xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
                xasm.emit_lwz(R27, R28, cpoffset);

		selfModify_lis_ori_and_b(R27, nopPC);
                xasm.emit_push(R27);
            } else {
                xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
                xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
                xasm.emit_lwz(R27, R28, cpoffset);
                xasm.emit_push(R27);
            }
            break;
        default:
            throw new Error("Unexpected CP tag: " + cp.getTagAt(cpindex));
        }
    }

    public void visit(Instruction.MONITORENTER i) {
        xasm.emit_pop(R5);
        generateNullCheckForMonitor(R5);
        generateCSACall(precomputed.csa_monitorEnter_index,
                precomputed.csa_monitorEnter_desc);
    }

    public void visit(Instruction.MONITOREXIT i) {
        xasm.emit_pop(R5);
        generateNullCheckForMonitor(R5);
        generateCSACall(precomputed.csa_monitorExit_index,
                precomputed.csa_monitorExit_desc);
    }
        
    public void visit(Instruction.NEW i) {
        int cpindex = i.getCPIndex(buf);
        int frame_size = stackLayout.getNativeFrameSize();
        
        if (precomputed.isExecutive) {
	    //	    || cp.isClassResolved(cpindex)) {
            try {
                cp.resolveClassAt(cpindex);
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveNew_index,
                    precomputed.csa_resolveNew_desc);

	    selfModify_b(nopPC);
        }

        // NEW_QUICK 
        int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, OBJECT, cpindex);

        if (ED_OBJECT_DONT_MOVE) {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a lis32
	    xasm.emit_nop();
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
            xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
            xasm.emit_lwz(R5, R28, offset_bp_in_cpvalues);

	    selfModify_lis_ori_and_b(R5, nopPC);

            generateCSACall(precomputed.csa_allocateObject_index,
                    precomputed.csa_allocateObject_desc);
	    
        } else {
            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
            xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
            xasm.emit_lwz(R5, R28, offset_bp_in_cpvalues);
            generateCSACall(precomputed.csa_allocateObject_index,
                    precomputed.csa_allocateObject_desc);
        }
    }

    public void visit(Instruction.NEW_QUICK i) {
        throw new Error("NEW_QUICK is not implemented");
    }
    
    public void visit(Instruction.SINGLEANEWARRAY i) {
        int cpindex = i.getCPIndex(buf);
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isClassResolved(cpindex)) {
            try {
                cp.resolveClassAt(cpindex);
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveClass_index,
                    precomputed.csa_resolveClass_desc);

	    selfModify_b(nopPC);
        }

        // ANEWARRAY_QUICK
        int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
                OBJECT, cpindex);
        if (ED_OBJECT_DONT_MOVE) {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a lis32
	    xasm.emit_nop();
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
            xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
            xasm.emit_lwz(R5, R28, offset_bp_in_cpvalues);

	    selfModify_lis_ori_and_b(R5, nopPC);

            xasm.emit_pop(R6);
            generateCSACall(precomputed.csa_allocateArray_index,
                    precomputed.csa_allocateArray_desc);

        } else {
            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
            xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
            xasm.emit_lwz(R5, R28, offset_bp_in_cpvalues);
            xasm.emit_pop(R6);
            generateCSACall(precomputed.csa_allocateArray_index,
                    precomputed.csa_allocateArray_desc);
        }
    }

    public void visit(Instruction.ANEWARRAY_QUICK i) {
        throw new Error("ANEWARRAY_QUICK not implemented");
    }
    
    public void visit(Instruction.MULTIANEWARRAY i) {
        int cpindex = i.getCPIndex(buf);
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isClassResolved(cpindex)) {
            try {
                cp.resolveClassAt(cpindex);
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveClass_index,
                    precomputed.csa_resolveClass_desc);

	    selfModify_b(nopPC);
        }

        // MULTIANEWARRAY_QUICK
        int dimensions = i.getDimensions(buf);
        int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
                OBJECT, cpindex);

	if (ED_OBJECT_DONT_MOVE) {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a lis32
	    xasm.emit_nop();
	    xasm.emit_nop(); // will become a branch

	    xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
	    xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
	    xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
	    xasm.emit_lwz(R5, R28, offset_bp_in_cpvalues); // R5 = param bp

	    selfModify_lis_ori_and_b(R5, nopPC);

	    xasm.emit_addi(R6, opSP, 4 * (dimensions - 1));
	    xasm.emit_addi(opSP, opSP, 4 * dimensions);
	    xasm.emit_li32(R7, dimensions);
	    generateCSACall(precomputed.csa_allocateMultiArray_index,
			    precomputed.csa_allocateMultiArray_desc);

	} else {
	    xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
	    xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
	    xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
	    xasm.emit_lwz(R5, R28, offset_bp_in_cpvalues); // R5 = param bp
	    xasm.emit_addi(R6, opSP, 4 * (dimensions - 1));
	    xasm.emit_addi(opSP, opSP, 4 * dimensions);
	    xasm.emit_li32(R7, dimensions);
	    generateCSACall(precomputed.csa_allocateMultiArray_index,
			    precomputed.csa_allocateMultiArray_desc);
	}
    }

    public void visit(Instruction.MULTIANEWARRAY_QUICK i) {
        throw new Error("MULTIANEWARRAY_QUICK not implemented");
    }
    
    public void visit(Instruction.INSTANCEOF i) {
        int cpindex = i.getCPIndex(buf);
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isClassResolved(cpindex)) {            
	    try {
                cp.resolveClassAt(cpindex);
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveClass_index,
                    precomputed.csa_resolveClass_desc);

	    selfModify_b(nopPC);
        }

        // INSTANCEOF_QUICK 
        int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
                OBJECT, cpindex);

        if (ED_OBJECT_DONT_MOVE) {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a li32
	    xasm.emit_nop();
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
            xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
            xasm.emit_lwz(R4, R28, offset_bp_in_cpvalues); // R4 = provider bp

	    selfModify_lis_ori_and_b(R4, nopPC);
        } else {
            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
            xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
            xasm.emit_lwz(R4, R28, offset_bp_in_cpvalues); // R4 = provider bp
        }

        xasm.emit_pop(R27); // R27 = objectref
        xasm.emit_cmpi(CR7, false, R27, 0);
        Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R3, R27)); // R3 = client bp
	xasm.emit_cmp(CR7, false, R4, R3);
	Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
        generateCFunctionCall("is_subtype_of");
        xasm.emit_push(R3);
        Branch b1 = xasm.emit_b_d();
	xasm.setBranchTarget(b2);
	xasm.emit_li32(R26, 1);
	xasm.emit_push(R26);
	Branch b3 = xasm.emit_b_d();
        xasm.setBranchTarget(b0);
        xasm.emit_li32(R26, 0);
        xasm.emit_push(R26);
        xasm.setBranchTarget(b1);
        xasm.setBranchTarget(b3);
    }

    public void visit(Instruction.INSTANCEOF_QUICK i) {
        throw new Error("INSTANCEOF_QUICK not implemented");
    }
    
    public void visit(Instruction.CHECKCAST i) {
        int cpindex = i.getCPIndex(buf);
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isClassResolved(cpindex)) {
            try {
                cp.resolveClassAt(cpindex);
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code");
                return;
            }
        } else {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R6, R30, precomputed.offset_cp_in_cf);
            xasm.emit_li32(R5, cpindex);
            generateCSACall(precomputed.csa_resolveClass_index,
                    precomputed.csa_resolveClass_desc);

	    selfModify_b(nopPC);
        }

        // CHECKCAST_QUICK
        int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
                OBJECT, cpindex);
        int classCastException = precomputed.classCastExceptionID;

        if (ED_OBJECT_DONT_MOVE) {
	    int nopPC = xasm.getPC();
	    xasm.emit_nop(); // will become a li32
	    xasm.emit_nop();
	    xasm.emit_nop(); // will become a branch

            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
            xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
            xasm.emit_lwz(R4, R28, offset_bp_in_cpvalues); // R4 = provider bp

	    selfModify_lis_ori_and_b(R4, nopPC);
        } else {
            xasm.emit_lwz(R30, SP, frame_size + offset_code_in_stack);
            xasm.emit_lwz(R29, R30, precomputed.offset_cp_in_cf);
            xasm.emit_lwz(R28, R29, precomputed.offset_values_in_cp);
            xasm.emit_lwz(R4, R28, offset_bp_in_cpvalues); // R4 = provider bp
        }

        xasm.emit_pop(R27); // R27 = objectref
        xasm.emit_cmpi(CR7, false, R27, 0);
        Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
        xasm.emit_push(R27);
        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R3, R27)); // R3 = client bp
	xasm.emit_cmp(CR7, false, R3, R4);
	Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
        generateCFunctionCall("is_subtype_of"); // R27 is preserved across this function call
        xasm.emit_cmpi(CR7, false, R3, 0);
        Branch b1 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_pop(R27);
        xasm.emit_li32(R6, 0);
        xasm.emit_li32(R5, classCastException);
        generateCSACall(precomputed.csa_generateThrowable_index,
                precomputed.csa_generateThrowable_desc);
        xasm.setBranchTarget(b0);
        xasm.emit_push(R27);
        xasm.setBranchTarget(b2);
        xasm.setBranchTarget(b1);
    }

    public void visit(Instruction.CHECKCAST_QUICK i) {
        throw new Error("CHECKCAST_QUICK not implemented");
    }
    
    public void visit(Instruction.INVOKE_SYSTEM i) {
        int mindex = (0xFF & (0x100 + i.getMethodIndex(buf)));
        int optype = i.getOpType(buf);
        switch (mindex) {
        case JVMConstants.InvokeSystemArguments.BREAKPOINT: {
            //throw new Error("INVOKE_SYSTEM_.BREAKPOINT not implemented");
            break;
        }
        case RUN: {
            xasm.emit_pop(R4);
            xasm.emit_pop(R3);
            generateCFunctionCall("SYSTEM_RUN");
            break;
        }
        case EMPTY_CSA_CALL: {
            generateCSACall(precomputed.csa_emptyCall_index,
                    precomputed.csa_emptyCall_desc);
            break;
        }
        case GET_CONTEXT: {
            xasm.emit_pop(R3);
            generateCFunctionCall("SYSTEM_GET_CONTEXT");
            xasm.emit_push(R3);
            break;
        }
        case NEW_CONTEXT: {
            xasm.emit_pop(R3);
            generateCFunctionCall("SYSTEM_NEW_CONTEXT");
            xasm.emit_push(R3);
            break;
        }
        case DESTROY_NATIVE_CONTEXT: {
            xasm.emit_pop(R3);
            generateCFunctionCall("SYSTEM_DESTROY_NATIVE_CONTEXT");
            break;
        }
        case GET_ACTIVATION: {
            xasm.emit_pop(R3);
            generateCFunctionCall("SYSTEM_GET_ACTIVATION");
            xasm.emit_push(R3);
            break;
        }
        case MAKE_ACTIVATION: {
            xasm.emit_pop(R5);
            xasm.emit_pop(R4);
            xasm.emit_pop(R3);
            generateCFunctionCall("SYSTEM_MAKE_ACTIVATION");
            xasm.emit_push(R3);
            break;
        }
        case CUT_TO_ACTIVATION: {
            xasm.emit_pop(R4);
            xasm.emit_pop(R3);
            generateCFunctionCall("SYSTEM_CUT_TO_ACTIVATION");
            break;
        }
        case INVOKE: {
            xasm.emit_pop(R4);
            xasm.emit_pop(R3);
            saveVolatileRegisters();
            generateCFunctionCall("SYSTEM_INVOKE");
            restoreVolatileRegisters();
            unloadReturnValue((char)optype);
            break;
        }
        case START_TRACING: {
            xasm.emit_pop(R3);
            generateCFunctionCall("SYSTEM_START_TRACING");
            break;
        }
        case STOP_TRACING: {
            generateCFunctionCall("SYSTEM_STOP_TRACING");
            break;
        }
        case BEGIN_OVERRIDE_TRACING: {
            break;
        }
        case END_OVERRIDE_TRACING: {
            break;
        }
        case WORD_OP: {
            int op = (0xFF & (0x100 + i.getOpType(buf)));
            if (op == WordOps.uI2L) {
                xasm.emit_li32(R30, 0);
                xasm.emit_push(R30);
                break;
            }
            xasm.emit_pop(R30); // rhs
            xasm.emit_pop(R29); // lhs
            switch (op) {
            case WordOps.sCMP: {
                xasm.emit_cmp(CR7, false, R29, R30);
                Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
                Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_LT);
                // 1
                xasm.emit_li32(R28, 1);
                xasm.emit_push(R28);
                Branch b3 = xasm.emit_b_d();
                // -1
                xasm.setBranchTarget(b2);
                xasm.emit_li32(R28, -1);
                xasm.emit_push(R28);
                Branch b4 = xasm.emit_b_d();
                // 0
                xasm.setBranchTarget(b1);
                xasm.emit_li32(R28, 0);
                xasm.emit_push(R28);
                xasm.setBranchTarget(b3);
                xasm.setBranchTarget(b4);
                break;
            }
            case WordOps.uCMP: {
                xasm.emit_cmpl(CR7, false, R29, R30);
                Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
                Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_LT);
                // 1
                xasm.emit_li32(R28, 1);
                xasm.emit_push(R28);
                Branch b3 = xasm.emit_b_d();
                // -1
                xasm.setBranchTarget(b2);
                xasm.emit_li32(R28, -1);
                xasm.emit_push(R28);
                Branch b4 = xasm.emit_b_d();
                // 0
                xasm.setBranchTarget(b1);
                xasm.emit_li32(R28, 0);
                xasm.emit_push(R28);
                xasm.setBranchTarget(b3);
                xasm.setBranchTarget(b4);
                break;
            }
            case WordOps.uLT: {
                xasm.emit_cmpl(CR7, false, R29, R30);
                Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_LT);
                // 0
                xasm.emit_li32(R28, 0);
                xasm.emit_push(R28);
                Branch b3 = xasm.emit_b_d();
                // 1
                xasm.setBranchTarget(b2);
                xasm.emit_li32(R28, 1);
                xasm.emit_push(R28);
                xasm.setBranchTarget(b3);
                break;
            }
            case WordOps.uLE: {
                xasm.emit_cmpl(CR7, false, R29, R30);
                Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_GT);
                // 0
                xasm.emit_li32(R28, 0);
                xasm.emit_push(R28);
                Branch b3 = xasm.emit_b_d();
                // 1
                xasm.setBranchTarget(b2);
                xasm.emit_li32(R28, 1);
                xasm.emit_push(R28);
                xasm.setBranchTarget(b3);
                break;
            }
            case WordOps.uGE: {
                xasm.emit_cmpl(CR7, false, R29, R30);
                Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
                // 0
                xasm.emit_li32(R28, 0);
                xasm.emit_push(R28);
                Branch b3 = xasm.emit_b_d();
                // 1
                xasm.setBranchTarget(b2);
                xasm.emit_li32(R28, 1);
                xasm.emit_push(R28);
                xasm.setBranchTarget(b3);
                break;
            }
            case WordOps.uGT: {
                xasm.emit_cmpl(CR7, false, R29, R30);
                Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_GT);
                // 0
                xasm.emit_li32(R28, 0);
                xasm.emit_push(R28);
                Branch b3 = xasm.emit_b_d();
                // 1
                xasm.setBranchTarget(b2);
                xasm.emit_li32(R28, 1);
                xasm.emit_push(R28);
                xasm.setBranchTarget(b3);
                break;
            }
            default:
                throw new Error("Unsupported invoke_system wordop argument : " + op);
            }
            break;
        }
        case DEREFERENCE: {
            int op = (0xFF & (0x100 + i.getOpType(buf)));
            switch (op) {
            case DereferenceOps.getByte:
                xasm.emit_pop(R30);
                xasm.emit_lbz(R29, R30, 0);
                xasm.emit_extsb(R28, R29);
                xasm.emit_push(R28);
                break;
            case DereferenceOps.getShort:
                xasm.emit_pop(R30);
                xasm.emit_lha(R29, R30, 0);
                xasm.emit_push(R29);
                break;
            case DereferenceOps.getChar:
                xasm.emit_pop(R30);
                xasm.emit_lhz(R29, R30, 0);
                xasm.emit_push(R29);
                break;
            case DereferenceOps.setByte:
                xasm.emit_pop(R30);
                xasm.emit_pop(R29);
                xasm.emit_stb(R30, R29, 0);
                break;
            case DereferenceOps.setShort:
                xasm.emit_pop(R30);
                xasm.emit_pop(R29);
                xasm.emit_sth(R30, R29, 0);
                break;
            case DereferenceOps.setBlock:
                xasm.emit_pop(R5);
                xasm.emit_pop(R4);
                xasm.emit_pop(R3);
                generateCFunctionCall("memmove");
                break;
            default:
                throw new Error("Unsupported invoke_system dereference op argument : " + op);
            }
            break;
        }

        case CAS32: {
           xasm.emit_pop(R5);  // new value
           xasm.emit_pop(R4);  // old value
           xasm.emit_pop(R3);  // ptr
           generateCFunctionCall("CAS32");
           xasm.emit_push(R3);
           break;
        }
        default:
            throw new Error("Unsupported invoke_system argument : " + mindex);
        }
    }

} // end of CodeGeneratorImpl
