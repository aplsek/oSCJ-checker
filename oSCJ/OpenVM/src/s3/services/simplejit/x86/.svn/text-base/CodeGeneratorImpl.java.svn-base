package s3.services.simplejit.x86;


import ovm.core.domain.ConstantResolvedInstanceFieldref;
import ovm.core.domain.ConstantResolvedInstanceMethodref;
import ovm.core.domain.ConstantResolvedInterfaceMethodref;
import ovm.core.domain.ConstantResolvedStaticFieldref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.LinkageException;
import ovm.core.repository.Descriptor;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionSet;
import ovm.services.bytecode.JVMConstants;
import ovm.util.OVMError;
import s3.core.domain.S3Field;
import s3.core.domain.S3Method;
import s3.services.simplejit.CodeGenContext;
import s3.services.simplejit.CodeGenerator;
import s3.services.simplejit.CompilerVMInterface;
import s3.services.simplejit.StackLayout;
import s3.services.simplejit.Assembler.Branch;
import ovm.core.execution.NativeConstants;

/**
 * @author Hiroshi Yamauchi
 **/
public final class CodeGeneratorImpl 
    extends CodeGenerator 
    implements JVMConstants, 
               JVMConstants.InvokeSystemArguments, 
               X86Constants {
    /**
     * We should really align on all systems.  When simplejit/j2c
     * integration is available, we will <b>need</b> to align on all
     * systems, because j2c uses -mfpmath=sse2.  Currently, however,
     * we only align on OSX.
     **/
    static final boolean ALIGN_CALLS = NativeConstants.OSX_BUILD;

    /**
     * x86 stack frames should be aligned to 16 byte boundaries.  The
     * stack should be aligned to this boundary before a call
     * instruction, and aligned to an odd 8 byte boundary after the
     * caller's sp is saved.
     **/
    static final int STACK_ALIGNMENT = 16;

    private X86Assembler xasm;

    public CodeGeneratorImpl(S3Method method,
			     InstructionSet is,
			     CompilerVMInterface compilerVMInterface,
			     Precomputed precomputed,
			     boolean debugPrintOn) {
	super(method, is, compilerVMInterface, debugPrintOn, 
	      precomputed);
    }

    protected void beforeBytecode() {
	if (ENABLE_BYTECODE_MARKING)
	    xasm.breakpoint();
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

    private int getNativeLocalOffset(int javaLocalOffset) {
	return stackLayout.getLocalVariableNativeOffset(javaLocalOffset);
    }	

    protected CodeGenContext makeCodeGenContext() {
	return new CodeGenContextImpl(bytecode.getBytes().length, method.getDeclaringType().getContext());
    }

    protected void prepareAssembler(CodeGenContext context) {
	this.xasm = new X86Assembler(context.getCodeBuffer(), 
				     CodeGenContext.globalCodeArray.position());
    }


    protected void generatePrologue() {
	// does not include the 3 words for callee-saved registers
	int frameSize = stackLayout.getNativeFrameSize();

	xasm.pushR(R_EBP);
	xasm.movRR(R_ESP, R_EBP);
	xasm.pushR(R_EDI); /* 3 words for callee-saved registers */
	xasm.pushR(R_ESI);
	xasm.pushR(R_EBX);
	if (frameSize != 0) 
	    xasm.subIR(frameSize, R_ESP);
	if (! OMIT_STACKOVERFLOW_CHECKS)
	    generateStackoverflowCheck();
	if (isSynchronized) {
	    int localOffset = getNativeLocalOffset(0);    // this or shst
	    xasm.pushM(R_EBP, localOffset);
	    generateCSACall(precomputed.csa_monitorEnter_index, 
			    precomputed.csa_monitorEnter_desc);
	}
    }

    protected void generateEpilogue() {
    }

    /**
     * A macro for calling a C function. All registers may be modified
     * after a call to this method.  The return value will be stored
     * in EAX after a call to this method. The stack pointer will be
     * adjusted to the level before arguments were being pushed.
     * @param function the C function name
     * @param argLength the # of words the arguments have to the C function
     **/
    private void generateCFunctionCall(String function,
				       int argLength) {
	int rtFuncIndex = compilerVMInterface.getRuntimeFunctionIndex(function);
	if (rtFuncIndex == -1)
	    throw new OVMError
		.Unimplemented("Runtime function \"" + function + "\" Not Found");
	int offset_fp_in_table = rtFuncIndex * 4;
	
	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to a movIR
	Branch b2 = xasm.setBranchTarget();

	alignArguments(argLength);
	// FIXME: why not call immediate?
	xasm.callAbsR(R_ECX);                          // call the function
	_generateAfterCall(argLength, 'V');

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path
	
	xasm.movIR(precomputed.
		  runtimeFunctionTableHandle.
		  asInt(), R_EAX);           // eax = table handle
	xasm.movMR(R_EAX, 0, R_EDX);                  // edx = table objectref
	xasm.movMR(R_EDX, offset_fp_in_table, R_ECX); // ecx = function pointer

	//
	xasm.selfModifyJmpWithMovIR(b0, R_ECX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);

    }

    /**
     * Call a C function with arguments already pushed into aligned
     * stack slots.  This method cleans up the stack by calling
     * {@link #unalign} to restore ESP to the value before
     * {@link #alignForCall}.  This method should only be called when
     * {@link #ALIGN_CALLS} is true.
     *
     * @see #generateCFunctionCall
     **/
    private void generateAlignedFunctionCall(String function) {
	assert (ALIGN_CALLS);
	int rtFuncIndex = compilerVMInterface.getRuntimeFunctionIndex(function);
	if (rtFuncIndex == -1)
	    throw new OVMError
		.Unimplemented("Runtime function \"" + function + "\" Not Found");
	int offset_fp_in_table = rtFuncIndex * 4;
	
	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to a movIR
	Branch b2 = xasm.setBranchTarget();

	// FIXME: why not call immediate?
	xasm.callAbsR(R_ECX);                          // call the function
	unalign();

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path
	
	xasm.movIR(precomputed.
		  runtimeFunctionTableHandle.
		  asInt(), R_EAX);           // eax = table handle
	xasm.movMR(R_EAX, 0, R_EDX);                  // edx = table objectref
	xasm.movMR(R_EDX, offset_fp_in_table, R_ECX); // ecx = function pointer

	//
	xasm.selfModifyJmpWithMovIR(b0, R_ECX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);

    }

    /**
     *
     * Because we do not know the current stack depth, we align the
     * stack by rounding down to a {@link #STACK_ALIGNMENT} boundary,
     * then subtracting space to pad out the argument list to an
     * alignment boundary.  The arguments stillmust be pushed after
     * calling this method.
     *
     * We save the old stack pointer in ESI, so that we pop aligned
     * arguments in a subsquent call to  {@link #unalign}.  ESI is
     * can also be used to repush arguments that where stored above the
     * old SP
     **/
    private void alignForCall(int argBytes) {
	if (ALIGN_CALLS) {
	    xasm.movRR(R_ESP, R_ESI);
	    xasm.andIR(~(STACK_ALIGNMENT - 1), R_ESP);
	    argBytes &= (STACK_ALIGNMENT - 1);
	    if (argBytes != 0)
		xasm.addIR(argBytes - STACK_ALIGNMENT, R_ESP);
	}
    }

    /**
     * Undo stack alignment.  Restore SP to it's value before a call to
     * {@link #alignForCall}.
     **/
    private void unalign() {
	if (ALIGN_CALLS) {
	    xasm.movRR(R_ESI, R_ESP);
	}
    }

    /**
     * Emit the code to pop arguments and copy the return value to the
     * operand stack
     * @param wordsToPop
     * receiver object
     * @param returnTypeCode - the return type code
     */
    private void _generateAfterCall(int wordsToPop, char returnTypeCode) {
	unalign();
	if (wordsToPop != 0)
	    xasm.addIR(4 * wordsToPop, R_ESP);
	else
	    xasm.nop(); // Don't let the call instruction the last
		       // instruction for the bytecode
	switch(returnTypeCode) {
	case TypeCodes.VOID:
	    break;
	case TypeCodes.INT:
	case TypeCodes.SHORT:
	case TypeCodes.CHAR:
	case TypeCodes.BYTE:
	case TypeCodes.BOOLEAN:
	case TypeCodes.OBJECT:
	case TypeCodes.ARRAY:
	    xasm.pushR(R_EAX);
	    break;
	case TypeCodes.LONG:
	    xasm.pushR(R_EDX);
	    xasm.pushR(R_EAX);
	    break;
	case TypeCodes.DOUBLE:
	    xasm.movRR(R_ESP, R_EDX);
	    xasm.fstpM64(R_EDX, -8);
	    xasm.subIR(8, R_ESP);
	    break;
	case TypeCodes.FLOAT:
	    xasm.movRR(R_ESP, R_EDX);
	    xasm.fstpM32(R_EDX, -4);
	    xasm.subIR(4, R_ESP);
	    break;
	default:
	    throw new OVMError();
	}
    }

    private void generateAfterCall(Descriptor.Method desc, boolean includesThis) {
	int wordsToPop = desc.getArgumentLength()/4;
	if (ALIGN_CALLS) {
	    if (includesThis)
		wordsToPop += 1;
	} else {
	    if (includesThis)
		// args, including this, appear twice, SimpleJITCode
		// appears once
		wordsToPop = 2*(wordsToPop + 1) + 1;
	    else
		// args appear twice, shared-state and SimpleJITCode
		// appear once
		wordsToPop = 2*wordsToPop + 2;
	}
	_generateAfterCall(wordsToPop, desc.getType().getTypeTag());
    }

    private void generateArgumentRepushing(Descriptor.Method desc) {
	int stackOffset = 0;
	int argCount = desc.getArgumentCount();
	if (ALIGN_CALLS)
	    alignForCall(desc.getArgumentLength() + 8);
	else
	    xasm.movRR(R_ESP, R_ESI);
	for(int i = argCount - 1; i >= 0; i--) {
	    if (desc.isArgumentWidePrimitive(i)) {
		xasm.pushM(R_ESI, stackOffset + 4);
		xasm.pushM(R_ESI, stackOffset);
		stackOffset += 8;
	    } else {
		xasm.pushM(R_ESI, stackOffset);
		stackOffset += 4;
	    }
	}
	// for receiver
	xasm.pushM(R_ESI, stackOffset);
	stackOffset += 4;
    }

    private void generateArgumentRepushingNoReceiver(Descriptor.Method desc) {
	int stackOffset = 0;
	int argCount = desc.getArgumentCount();
	if (ALIGN_CALLS)
	    alignForCall(desc.getArgumentLength() + 8);
	else
	    xasm.movRR(R_ESP, R_ESI);
	for(int i = argCount - 1; i >= 0; i--) {
	    if (desc.isArgumentWidePrimitive(i)) {
		xasm.pushM(R_ESI, stackOffset + 4);
		xasm.pushM(R_ESI, stackOffset);
		stackOffset += 8;
	    } else {
		xasm.pushM(R_ESI, stackOffset);
		stackOffset += 4;
	    }
	}
    }

    /**
     * This method ensures stack alignment when arguments have already
     * been pushed in the right order.  It aligns the stack pointer,
     * and copies arguments to the new top-of-stack.
     * <p>
     * clobbers EAX, ESI, EDI
     * FIXME: If we could clobber ECX, then we could use `REP MOVSL'
     **/
    private void alignArguments(int wordsToAlign) {
	if (ALIGN_CALLS) {
	    // We could actually do alignForCall 0, and copy
	    // overlapping arguments carefully.
	    alignForCall(4 * wordsToAlign);
	    xasm.subIR(4 * wordsToAlign, R_ESP);
	    xasm.movRR(R_ESP, R_EDI);
	    for (int i = 0; i < wordsToAlign; i++) {
		xasm.movMR(R_ESI, 4*i, R_EAX);
		xasm.movRM(R_EAX, R_EDI, 4*i);
	    }
	}
    }
	
    /**
     * A macro for calling a Java CSA method. All registers may be modified
     * after the call. The return value will be on the stack. The stack pointer
     * will be adjusted to where the return value will be.
     * @param methodIndex the index of the CSA method to call to the CSA native vtable
     * @param desc the desecriptor
     **/
    private void generateCSACall(int methodIndex,
				 Descriptor.Method desc) {
	if (ED_OBJECT_DONT_MOVE && DEVIRTUALIZE_CSA)
	    generateCSACall_Fast(methodIndex, desc);
	else
	    generateCSACall_Slow(methodIndex, desc);
    }

    private void generateCSACall_Slow(int methodIndex,
				      Descriptor.Method desc) {
	//System.err.println("generateCSACall() index = " + methodIndex);
	int offset_cf_in_csavtable = getArrayElementOffset(executiveDomain,
							   OBJECT, 
							   methodIndex);
	int argLength = desc.getArgumentLength() / 4 + 1;

	xasm.movMR(R_EBP, stackLayout.getCodeFragmentOffset(), R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_csa_in_cf, R_ECX);
	xasm.pushR(R_ECX);
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_ECX, R_EDX)); // edx = csa bp
	xasm.movMR(R_EDX, precomputed.offset_vtbl_in_bp, R_EAX); // eax csa vtbl
	xasm.movMR(R_EAX, offset_cf_in_csavtable, R_ECX);        // ecx = csa method cf
	xasm.pushR(R_ECX);                                       // push next cf
	xasm.movMR(R_ECX, precomputed.offset_code_in_cf, R_EDX); // edx = code

	alignArguments(argLength + 1);

	xasm.callAbsR(R_EDX);
	//xasm.nop();// Don't let the call be the last instruction for the bytecode

	_generateAfterCall(argLength + 1, desc.getType().getTypeTag());
    }

    private void generateCSACall_Fast(int methodIndex,
				      Descriptor.Method desc) {
	//System.err.println("generateCSACall() index = " + methodIndex);
	int offset_cf_in_csavtable = getArrayElementOffset(executiveDomain,
							   OBJECT, 
							   methodIndex);
	int argLength = desc.getArgumentLength() / 4 + 1;

	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	Branch b2 = xasm.setBranchTarget();

	//... fast path

	int patch_pc_csa_obj = xasm.pushI32_to_be_patched();                 // push csa obj
	int patch_pc_code_obj = xasm.pushI32_to_be_patched();
	int patch_pc_code_ptr = xasm.movIR_to_be_patched(R_EDX);

	alignArguments(argLength + 1);
	xasm.callAbsR(R_EDX);
	//xasm.nop();// Don't let the call be the last instruction for the bytecode
	_generateAfterCall(argLength + 1, desc.getType().getTypeTag());

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path

	xasm.movMR(R_EBP, stackLayout.getCodeFragmentOffset(), R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_csa_in_cf, R_ECX);
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_ECX, R_EDX)); // edx = csa bp
	xasm.movMR(R_EDX, precomputed.offset_vtbl_in_bp, R_EAX); // eax csa vtbl
	xasm.movMR(R_EAX, offset_cf_in_csavtable, R_EDX);        // edx = csa method cf
	xasm.movMR(R_EDX, precomputed.offset_code_in_cf, R_EAX); // eax = code ptr
	//
	xasm.selfModifyJmpWithNops(b0);
	xasm.selfModify4Bytes(patch_pc_csa_obj, R_ECX);
	xasm.selfModify4Bytes(patch_pc_code_obj, R_EDX);
	xasm.selfModify4Bytes(patch_pc_code_ptr, R_EAX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);
    }

    protected void debugPrint(String message) {
	byte[] msg = message.getBytes();
	int l_popR = xasm.l_popR();
	int l_addRR = xasm.l_addRR();
	int l_movIR = xasm.l_movIR();
	int l_jmp_long = xasm.l_jmp_long();
	xasm.call(0);
	xasm.popR(R_EAX);
	xasm.movIR(l_popR + l_addRR + l_movIR + l_jmp_long, R_EDX);
	xasm.addRR(R_EDX, R_EAX);
	xasm.jmp_long(msg.length + 1);
	for(int i = 0; i < msg.length; i++)
	    xasm.write(msg[i]);
	xasm.write((byte)0); // null character
	xasm.pushR(R_EAX);
	generateCFunctionCall("printf", 1);
	generateCFunctionCall("fflush_out", 0);
	
    }

    private void generateMemDebug() {
	if (!DO_MEM_DEBUG) {
	    return;
	}
	xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	xasm.movMR(R_ECX, precomputed.offset_name_in_cf, R_EDX); 
	xasm.pushR(R_EDX);
	xasm.pushI32(getPC());
	generateCFunctionCall("memDebugHook", 2);
    }

    /**
     * This catches unsupported instructions
     */
    public void visit(Instruction i) {
	//generateCFunctionCall("hit_unrewritten_code", 0);
    }

    public void visit(Instruction.NOP i) {
	
	// go on vacation, do nothing
    }
    public void visit(Instruction.AFIAT _) {
	
	// go home, take a nap
    }
    public void visit(Instruction.PrimFiat _) {
	
	// go out, have fun
    }
    public void visit(Instruction.WidePrimFiat _) {
	
	// go to hell, die
    }
    
    public void visit(Instruction.POLLCHECK i) {
	
	// event poll check
	if (OPTIMIZE_POLLCHECK) {
	    int eventUnionIndex = 
		compilerVMInterface
		.getRuntimeFunctionIndex("eventUnion");
	    if (eventUnionIndex == -1)
		throw new OVMError();
	    /* hand inlinig of eventPollcheck() */
	    xasm.movIR(precomputed.
		      runtimeFunctionTableHandle.
		      asInt(), R_EAX);
	    xasm.movMR(R_EAX, 0, R_EDX);
	    xasm.movMR(R_EDX, eventUnionIndex * 4, R_ECX);
	    xasm.movMR(R_ECX, 0, R_EAX);
	    xasm.cmpRI(R_EAX, 0);
	    Branch b0 = xasm.setBranchSourceAndJcc(J_NE);
	    // Event occurred
	    xasm.movIR(0x00010001, R_EAX); /* setting notSignaled=1 and
					   * notEnabled=1
					   * simultaneously */
	    xasm.movRM(R_EAX, R_ECX, 0);
	    generateCSACall(precomputed.csa_pollingEventHook_index, 
			    precomputed.csa_pollingEventHook_desc);
	    xasm.setBranchTarget(b0);
	    // Event not occurred - do nothing
	} else {
	    generateCFunctionCall("eventPollcheck", 0);
	    xasm.cmpRI(R_EAX, 0);
	    Branch b = xasm.setBranchSourceAndJcc(J_E);
	    generateCSACall(precomputed.csa_pollingEventHook_index, 
			    precomputed.csa_pollingEventHook_desc);
	    xasm.setBranchTarget(b);
	}
    }

    public void visit(Instruction.ACONST_NULL i) {
	
	xasm.pushI32(0);
    }

    // ICONST_X, BIPUSH, SIPUSH,
    public void visit(Instruction.IConstantLoad i) {
	
	xasm.pushI32(i.getValue(buf));
    }

    public void visit(Instruction.FConstantLoad f) {
	
	float val = f.getFValue(buf);
	int i = Float.floatToIntBits(val);
	xasm.pushI32(i);
    }

    public void visit(Instruction.DConstantLoad i) {
	
	double val = i.getDValue(buf);
	long l = Double.doubleToLongBits(val);
	xasm.pushI64(l);
    }

    public void visit(Instruction.LConstantLoad i) {
	
	xasm.pushI64(i.getLValue(buf));
    }

    public void visit(Instruction.LocalRead i) {
	
	int operand = i.getLocalVariableOffset(buf);
	int localOffset = getNativeLocalOffset(operand);
	if (i.stackOuts.length > 1)
	    // 8 byte operation
	    xasm.pushM(R_EBP, localOffset + 4);
	xasm.pushM(R_EBP, localOffset);
    }

    public void visit(Instruction.LocalWrite i) {
	
	int operand = i.getLocalVariableOffset(buf);
	int localOffset = getNativeLocalOffset(operand);
	xasm.popM(R_EBP, localOffset);
	if (i.stackIns.length > 1)
	    // 8 byte operation
	    xasm.popM(R_EBP, localOffset + 4);
    }    

    // LDC, LDC_W, LDC2_W
    public void visit(Instruction.ConstantPoolLoad i) {
	if (! (i instanceof Instruction.LDC 
		|| i instanceof Instruction.LDC2_W
		|| i instanceof Instruction.LDC_W 
		|| i instanceof Instruction.LDC_W_REF_QUICK
		|| i instanceof Instruction.LDC_REF_QUICK))
		return;

	
	
	int cpindex = i.getCPIndex(buf);
	switch(cp.getTagAt(cpindex)) {
	case CONSTANT_Integer:
	case CONSTANT_Float:
	    xasm.pushI32(cp.getValueAt(cpindex));
	    break;
	case CONSTANT_Long:
	case CONSTANT_Double:
	    xasm.pushI64(cp.getWideValueAt(cpindex));
	    break;
	case CONSTANT_String:
	case CONSTANT_Binder:
	case CONSTANT_SharedState: {
	    if (precomputed.isExecutive) {
		cp.resolveConstantAt(cpindex);
	    } else {
		Branch br0 = xasm.setBranchSourceAndJmp();

		// slow path
		xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EAX);
		xasm.pushR(R_EAX); // push S3Constants
		xasm.pushI32(cpindex);
		generateCSACall(precomputed.csa_resolveLdc_index, 
				precomputed.csa_resolveLdc_desc);
		xasm.setSelfModifyingBranchTarget(br0);
	    }

	    // fast path - LDC_QUICK
	    int stroffset2 = getArrayElementOffset(executiveDomain,
						   OBJECT, 
						   cpindex);

	    if (ED_OBJECT_DONT_MOVE) {
		// this jmp will be self-modified to a movIR
		Branch b0 = xasm.setBranchSourceAndJmp();
		Branch b2 = xasm.setBranchTarget();

		//... fast path
		xasm.movMR(R_EDX, stroffset2, R_ECX);
		xasm.pushR(R_ECX);

		Branch b1 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(b0);
		
		//... slow path
		xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_values_in_cp, R_EDX);
		//
		xasm.selfModifyJmpWithMovIR(b0, R_EDX);
		xasm.setBranchSourceAndJmp(b2);
		xasm.setBranchTarget(b1);
	    } else {
		xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_values_in_cp, R_EDX);
		xasm.movMR(R_EDX, stroffset2, R_ECX);
		xasm.pushR(R_ECX);
	    }
	    break;
	}
	case CONSTANT_Reference:
	    int stroffset2 = getArrayElementOffset(executiveDomain,
						   OBJECT, 
						   cpindex);
	    if (ED_OBJECT_DONT_MOVE) {
		// this jmp will be self-modified to a movIR
		Branch b0 = xasm.setBranchSourceAndJmp();
		Branch b2 = xasm.setBranchTarget();

		//... fast path
		xasm.movMR(R_EDX, stroffset2, R_ECX);
		xasm.pushR(R_ECX);

		Branch b1 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(b0);
		
		//... slow path
		xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_values_in_cp, R_EDX);
		//
		xasm.selfModifyJmpWithMovIR(b0, R_EDX);
		xasm.setBranchSourceAndJmp(b2);
		xasm.setBranchTarget(b1);
	    } else {
		xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_values_in_cp, R_EDX);
		xasm.movMR(R_EDX, stroffset2, R_ECX);
		xasm.pushR(R_ECX);
	    }
	    break;	    
	default:
	    throw new Error("Unexpected CP tag: " + cp.getTagAt(cpindex));
	}
    }

    public void visit(Instruction.LOAD_SHST_METHOD i) {
	
	if (ED_OBJECT_DONT_MOVE)
	    visitLOAD_SHST_METHOD_Fast(i);
	else
	    visitLOAD_SHST_METHOD_Slow(i);
    }

    public void visitLOAD_SHST_METHOD_Fast(Instruction.LOAD_SHST_METHOD i) {
	int cp_index = i.getCPIndex(buf);
	if (cp.getTagAt(cp_index) != CONSTANT_ResolvedStaticMethod) {
	    throw new Error();
	}
	int offset_methodref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);

	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to a movIR
	Branch b2 = xasm.setBranchTarget();

	//... fast path

	xasm.pushR(R_EAX);
	Branch b = xasm.setBranchSourceAndJmp();
	// FIXME: Is initializeBlueprint on the fast path?  Surely it
	// shouldn't be.
	generateCSACall(precomputed.csa_initializeBlueprint_index,
			precomputed.csa_initializeBlueprint_desc);
	// initializeBlueprint returns the given shst
	xasm.setSelfModifyingBranchTarget(b);

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path

	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
	xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_shst_in_staticmref, R_EAX); // EAX = shst

	//
	xasm.selfModifyJmpWithMovIR(b0, R_EAX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);
    }

    public void visitLOAD_SHST_METHOD_Slow(Instruction.LOAD_SHST_METHOD i) {
	int cp_index = i.getCPIndex(buf);
	if (cp.getTagAt(cp_index) != CONSTANT_ResolvedStaticMethod) {
	    throw new Error();
	}
	int offset_methodref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);
	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
	xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_shst_in_staticmref, R_EAX); // EAX = shst

	xasm.pushR(R_EAX);
	Branch b = xasm.setBranchSourceAndJmp();
	generateCSACall(precomputed.csa_initializeBlueprint_index,
			precomputed.csa_initializeBlueprint_desc);
	// initializeBlueprint returns the given shst
	xasm.setSelfModifyingBranchTarget(b);
    }

    public void visit(Instruction.LOAD_SHST_FIELD i) {
	
	if (ED_OBJECT_DONT_MOVE)
	    visitLOAD_SHST_FIELD_Fast(i);
	else
	    visitLOAD_SHST_FIELD_Slow(i);
    }

    public void visitLOAD_SHST_FIELD_Fast(Instruction.LOAD_SHST_FIELD i) {
	int cp_index = i.getCPIndex(buf);
	if (cp.getTagAt(cp_index) != CONSTANT_ResolvedStaticField) {
	    throw new Error();
	}
	int offset_fieldref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);

	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to a movIR
	Branch b2 = xasm.setBranchTarget();

	//... fast path

	xasm.pushR(R_EAX);
	Branch b = xasm.setBranchSourceAndJmp();
	generateCSACall(precomputed.csa_initializeBlueprint_index,
			precomputed.csa_initializeBlueprint_desc);
	// initializeBlueprint returns the given shst
	xasm.setSelfModifyingBranchTarget(b);

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path

	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
	xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);

	//
	xasm.selfModifyJmpWithMovIR(b0, R_EAX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);
    }

    public void visitLOAD_SHST_FIELD_Slow(Instruction.LOAD_SHST_FIELD i) {
	int cp_index = i.getCPIndex(buf);
	if (cp.getTagAt(cp_index) != CONSTANT_ResolvedStaticField) {
	    throw new Error();
	}
	int offset_fieldref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);
	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
	xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);

	xasm.pushR(R_EAX);

	Branch b = xasm.setBranchSourceAndJmp();
	generateCSACall(precomputed.csa_initializeBlueprint_index,
			precomputed.csa_initializeBlueprint_desc);
	// initializeBlueprint returns the given shst
	xasm.setSelfModifyingBranchTarget(b);
    }

    public void visit(Instruction.LOAD_SHST_METHOD_QUICK i) {
	
	if (ED_OBJECT_DONT_MOVE)
	    visitLOAD_SHST_METHOD_QUICK_Fast(i);
	else
	    visitLOAD_SHST_METHOD_QUICK_Slow(i);
    }

    public void visitLOAD_SHST_METHOD_QUICK_Fast
	(Instruction.LOAD_SHST_METHOD_QUICK i) {
	int cp_index = i.getCPIndex(buf);
	if (cp.getTagAt(cp_index) != CONSTANT_ResolvedStaticMethod) {
	    throw new Error();
	}
	int offset_methodref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);

	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	Branch b2 = xasm.setBranchTarget();

	//... fast path
	int patch_pc = xasm.pushI32_to_be_patched();

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path

	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
	xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_shst_in_staticmref, R_EAX);

	//
	xasm.selfModifyJmpWithNops(b0);
	xasm.selfModify4Bytes(patch_pc, R_EAX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);
    }

    public void visitLOAD_SHST_METHOD_QUICK_Slow
	(Instruction.LOAD_SHST_METHOD_QUICK i) {
	int cp_index = i.getCPIndex(buf);
	if (cp.getTagAt(cp_index) != CONSTANT_ResolvedStaticMethod) {
	    throw new Error();
	}
	int offset_methodref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);
	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
	xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_shst_in_staticmref, R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LOAD_SHST_FIELD_QUICK i) {
	
	if (ED_OBJECT_DONT_MOVE)
	    visitLOAD_SHST_FIELD_QUICK_Fast(i);
	else
	    visitLOAD_SHST_FIELD_QUICK_Slow(i);
    }

    public void visitLOAD_SHST_FIELD_QUICK_Fast
	(Instruction.LOAD_SHST_FIELD_QUICK i) {
	int cp_index = i.getCPIndex(buf);
	if (cp.getTagAt(cp_index) != CONSTANT_ResolvedStaticField) {
	    throw new Error();
	}
	int offset_fieldref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);

	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	Branch b2 = xasm.setBranchTarget();

	//... fast path
	int patch_pc = xasm.pushI32_to_be_patched();

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path

	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
	xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);

	//
	xasm.selfModifyJmpWithNops(b0);
	xasm.selfModify4Bytes(patch_pc, R_EAX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);
    }

    public void visitLOAD_SHST_FIELD_QUICK_Slow
	(Instruction.LOAD_SHST_FIELD_QUICK i) {
	int cp_index = i.getCPIndex(buf);
	if (cp.getTagAt(cp_index) != CONSTANT_ResolvedStaticField) {
	    throw new Error();
	}
	int offset_fieldref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);
	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
	xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
	xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);
	xasm.pushR(R_EAX);
    }

    private void generateStackoverflowCheck() {
	if (OPTIMIZE_STACK_OVERFLOW_CHECK)
	    generateStackoverflowCheck_Fast();
	else
	    generateStackoverflowCheck_Slow();
    }

    private void generateStackoverflowCheck_Fast() {
	int currentContextIndex =
	    compilerVMInterface
	    .getRuntimeFunctionIndex("currentContext");
	if (currentContextIndex == -1)
	    throw new OVMError();

	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	Branch b2 = xasm.setBranchTarget();

	//... fast path

	int patch_pc = xasm.movIR_to_be_patched(R_ECX);
	xasm.movMR(R_ECX, 0, R_EAX); // eax = current context
	xasm.movMR(R_EAX, precomputed.offset_mtb_in_nc, R_EDX); // edx = mtb
	xasm.movMR(R_EDX, precomputed.offset_redzone_in_mtb, R_ECX); // ecx = redzone
	xasm.cmpRR(R_EBP, R_ECX);
		  
	Branch br0 = xasm.setBranchSourceAndJcc(J_G);
	xasm.movMR(R_EAX, precomputed.offset_pso_in_nc, R_EDX);
	xasm.cmpRI(R_EDX, 0);
	Branch br1 = xasm.setBranchSourceAndJcc(J_NE);

	// overflow or already processing overflow
	xasm.movIM(1, R_EAX, precomputed.offset_pso_in_nc);
	xasm.pushI32(0);                                // push meta
	xasm.pushI32(precomputed.stackOverflowErrorID); // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path

	xasm.movIR(precomputed.
		  runtimeFunctionTableHandle.
		  asInt(), R_EAX);
	xasm.movMR(R_EAX, 0, R_EDX);
	xasm.movMR(R_EDX, currentContextIndex * 4, R_ECX);
	
	//
	xasm.selfModifyJmpWithNops(b0);
	xasm.selfModify4Bytes(patch_pc, R_ECX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);

	// not overflow
	xasm.setBranchTarget(br0);
	xasm.setBranchTarget(br1);
    }

    private void generateStackoverflowCheck_Slow() {
	generateCFunctionCall("check_stack_overflow", 0);
	xasm.cmpRI(R_EAX, 0);
	Branch b = xasm.setBranchSourceAndJcc(J_E);
	xasm.pushI32(0);                                // push meta
	xasm.pushI32(precomputed.stackOverflowErrorID); // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);
	xasm.setBranchTarget(b);
    }

    private void generateNullCheck(byte receiver_reg) {
	if (OMIT_NULLPOINTER_CHECKS) return;
	xasm.cmpRI(receiver_reg, 0);
	Branch b0 = xasm.setBranchSourceAndJcc(J_NE);
	xasm.pushI32(0);                                  // push meta
	xasm.pushI32(precomputed.nullPointerExceptionID); // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);
	xasm.setBranchTarget(b0);
    }

    private void generateNullCheckForMonitor(byte receiver_reg) {
	if (OMIT_NULLPOINTER_CHECKS_MONITOR) return;
	xasm.cmpRI(receiver_reg, 0);
	Branch b0 = xasm.setBranchSourceAndJcc(J_NE);
	xasm.pushI32(0);                                  // push meta
	xasm.pushI32(precomputed.nullPointerExceptionID); // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);
	xasm.setBranchTarget(b0);
    }

    private void generateDivisionByIntZeroCheck(byte divisor_reg) {
	xasm.cmpRI(divisor_reg, 0);
	Branch b0 = xasm.setBranchSourceAndJcc(J_NE);
	xasm.pushI32(0);                                 // push meta
	xasm.pushI32(precomputed.arithmeticExceptionID); // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);
	xasm.setBranchTarget(b0);
    }

    private void generateDivisionByLongZeroCheck(byte divisor_h_reg,
						 byte divisor_l_reg) {
	xasm.movRR(divisor_h_reg, R_ESI);
	xasm.orRR(divisor_l_reg, R_ESI);
	xasm.cmpRI(R_ESI, 0);
	Branch b0 = xasm.setBranchSourceAndJcc(J_NE);
	xasm.pushI32(0);                                 // push meta
	xasm.pushI32(precomputed.arithmeticExceptionID); // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);
	xasm.setBranchTarget(b0);
    }

    private void generateArrayBoundCheck(byte array_reg,
					 byte index_reg) {
	if (OMIT_ARRAYBOUND_CHECKS) return;
        switch(precomputed.tArrayLengthFieldSize) {
        case 2:
            xasm.movsxM16R(array_reg, precomputed.tArrayLengthFieldOffset, R_ESI);
            break;
        case 4:
            xasm.movMR(array_reg, precomputed.tArrayLengthFieldOffset, R_ESI);
            break;
        default:
            throw new Error("Invalid array element size");
        }
	xasm.cmpRR(index_reg, R_ESI);
	Branch b0 = xasm.setBranchSourceAndJcc(J_B);
	xasm.pushI32(0);                                   // push meta
	xasm.pushI32(precomputed.arrayIndexOutOfBoundsExceptionID); // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);
	xasm.setBranchTarget(b0);
    }

    // Note:
    // saves R_EAX, R_ECX, R_EDX, R_EDI
    private void generateArrayStoreCheck(byte array_reg,
					 byte elem_reg) {
	if (OMIT_ARRAYSTORE_CHECKS) return;
	// save live regs
	xasm.pushR(R_EAX);
	xasm.pushR(R_ECX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EDI);

	byte array_bp = array_reg;
	byte comp_bp = array_reg;
	byte elem_bp = elem_reg;
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(array_reg, array_bp));
	xasm.movMR(array_bp, precomputed.offset_componentbp_in_arraybp, comp_bp);
	xasm.cmpRI(elem_reg, 0);
	Branch b0 = xasm.setBranchSourceAndJcc(J_E);
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(elem_reg, elem_bp));

	xasm.pushR(comp_bp);
	xasm.pushR(elem_bp);
	generateCFunctionCall("is_subtype_of", 2);
	xasm.cmpRI(R_EAX, 0);
	Branch b1 = xasm.setBranchSourceAndJcc(J_NE);

	// check failed (throw exception)

	xasm.pushI32(0);                                 // push meta
	xasm.pushI32(precomputed.arrayStoreExceptionID); // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);

	// null is ok
	xasm.setBranchTarget(b0);

	// subtype test passed
	xasm.setBranchTarget(b1);

	// restore
	xasm.popR(R_EDI);
	xasm.popR(R_EDX);
	xasm.popR(R_ECX);
	xasm.popR(R_EAX);
    }

    public void visit(Instruction.IALOAD i) {
	
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tIntArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tIntArrayElementSize, R_EDX);
        xasm.mulRR(R_ECX, R_EDX);
        xasm.addRR(R_EDX, R_EAX);
        xasm.movMR(R_EAX, 0, R_ECX);
        xasm.pushR(R_ECX);
    }

    public void visit(Instruction.LALOAD i) {
	
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tLongArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tLongArrayElementSize, R_EDX);
        xasm.mulRR(R_ECX, R_EDX);
        xasm.addRR(R_EDX, R_EAX);
        xasm.movMR(R_EAX, 0, R_ECX);
        xasm.movMR(R_EAX, 4, R_EDX);
        xasm.pushR(R_EDX);
        xasm.pushR(R_ECX);
    }

    public void visit(Instruction.FALOAD i) {
	
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tFloatArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tFloatArrayElementSize, R_EDX);
        xasm.mulRR(R_ECX, R_EDX);
        xasm.addRR(R_EDX, R_EAX);
        xasm.movMR(R_EAX, 0, R_ECX);
        xasm.pushR(R_ECX);
    }

    public void visit(Instruction.DALOAD i) {
	
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tDoubleArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tDoubleArrayElementSize, R_EDX);
        xasm.mulRR(R_ECX, R_EDX);
        xasm.addRR(R_EDX, R_EAX);
        xasm.movMR(R_EAX, 0, R_ECX);
        xasm.movMR(R_EAX, 4, R_EDX);
        xasm.pushR(R_EDX);
        xasm.pushR(R_ECX);
    }

    public void visit(Instruction.AALOAD i) {
	
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tObjectArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tObjectArrayElementSize, R_EDX);
        xasm.mulRR(R_ECX, R_EDX);
        xasm.addRR(R_EDX, R_EAX);
        xasm.movMR(R_EAX, 0, R_ECX);
        xasm.pushR(R_ECX);
    }

    public void visit(Instruction.SALOAD i) {
	
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tShortArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tShortArrayElementSize, R_EDX);
        xasm.mulRR(R_ECX, R_EDX);
        xasm.addRR(R_EDX, R_EAX);
	xasm.movsxM16R(R_EAX, 0, R_ECX);
        xasm.pushR(R_ECX);
    }

    public void visit(Instruction.CALOAD i) {
	
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tCharArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tCharArrayElementSize, R_EDX);
        xasm.mulRR(R_ECX, R_EDX);
        xasm.addRR(R_EDX, R_EAX);
	xasm.movzxM16R(R_EAX, 0, R_ECX);
        xasm.pushR(R_ECX);
    }

    public void visit(Instruction.BALOAD i) {
	
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tByteArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tByteArrayElementSize, R_EDX);
        xasm.mulRR(R_ECX, R_EDX);
        xasm.addRR(R_EDX, R_EAX);
	xasm.movsxM8R(R_EAX, 0, R_ECX);
        xasm.pushR(R_ECX);
    }

    public void visit(Instruction.IASTORE i) {
	
        xasm.popR(R_EDX);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tIntArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tIntArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addRR(R_ECX, R_EAX);
        xasm.movRM(R_EDX, R_EAX);
    }

    public void visit(Instruction.LASTORE i) {
	
        xasm.popR(R_EDX);
        xasm.popR(R_EDI);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tLongArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tLongArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addRR(R_ECX, R_EAX);
        xasm.movRM(R_EDX, R_EAX);
        xasm.movRM(R_EDI, R_EAX, 4);
    }

    public void visit(Instruction.FASTORE i) {
	
        xasm.popR(R_EDX);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tFloatArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tFloatArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addRR(R_ECX, R_EAX);
        xasm.movRM(R_EDX, R_EAX);
    }

    public void visit(Instruction.DASTORE i) {
	
        xasm.popR(R_EDX);
        xasm.popR(R_EDI);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tDoubleArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tDoubleArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addRR(R_ECX, R_EAX);
        xasm.movRM(R_EDX, R_EAX);
        xasm.movRM(R_EDI, R_EAX, 4);
    }

    public void visit(Instruction.UNCHECKED_AASTORE i) {
	
        xasm.popR(R_EDX);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tObjectArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tObjectArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addRR(R_ECX, R_EAX);
        xasm.movRM(R_EDX, R_EAX);
    }

    public void visit(Instruction.AASTORE i) {
	if (i instanceof Instruction.AASTORE_WITH_BARRIER)
	    return;
	
        xasm.popR(R_EDX);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);
	generateArrayStoreCheck(R_EAX, R_EDX);

        xasm.addIR(precomputed.tObjectArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tObjectArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addRR(R_ECX, R_EAX);
        xasm.movRM(R_EDX, R_EAX);
    }

    public void visit(Instruction.AASTORE_WITH_BARRIER i) {
	
        xasm.popR(R_EDX);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);
	generateArrayStoreCheck(R_EAX, R_EDX);

        xasm.movIR(precomputed.tObjectArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addIR(precomputed.tObjectArrayHeaderSize, R_ECX);
	// now ECX = offset

	// Write barrier call
	xasm.pushR(R_EDX); // value
	xasm.pushR(R_ECX); // offset
	xasm.pushR(R_EAX); // arr
	generateCSACall(precomputed.csa_aastoreBarrier_index,
			precomputed.csa_aastoreBarrier_desc);
    }

    public void visit(Instruction.READ_BARRIER i) {
	
     generateCSACall(precomputed.csa_readBarrier_index,
		precomputed.csa_readBarrier_desc);
    }

    public void visit(Instruction.SASTORE i) {
	
        xasm.popR(R_EDX);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tShortArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tShortArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addRR(R_ECX, R_EAX);
	xasm.movR16M(R_EDX, R_EAX, 0);
    }

    public void visit(Instruction.CASTORE i) {
	
        xasm.popR(R_EDX);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tCharArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tCharArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addRR(R_ECX, R_EAX);
	xasm.movR16M(R_EDX, R_EAX, 0);
    }

    public void visit(Instruction.BASTORE i) {
	
        xasm.popR(R_EDX);
        xasm.popR(R_ECX);
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);
	generateArrayBoundCheck(R_EAX, R_ECX);

        xasm.addIR(precomputed.tByteArrayHeaderSize, R_EAX);
        xasm.movIR(precomputed.tByteArrayElementSize, R_EBX);
        xasm.mulRR(R_EBX, R_ECX);
        xasm.addRR(R_ECX, R_EAX);
	xasm.movR8M(R_EDX, R_EAX, 0);
    }


    public void visit(Instruction.ARRAYLENGTH i) {
	
        xasm.popR(R_EAX);

	generateNullCheck(R_EAX);

        switch(precomputed.tArrayLengthFieldSize) {
        case 2:
            xasm.movsxM16R(R_EAX, precomputed.tArrayLengthFieldOffset, R_EDX);
            break;
        case 4:
            xasm.movMR(R_EAX, precomputed.tArrayLengthFieldOffset, R_EDX);
            break;
        default:
            throw new Error("Invalid array element size");
        }
        xasm.pushR(R_EDX);
    }

    public void visit(Instruction.POP i) {
	
	xasm.addIR(4, R_ESP);
    }

    public void visit(Instruction.POP2 i) {
	
	xasm.addIR(8, R_ESP);
    }

    public void visit(Instruction.DUP i) {
	
	xasm.popR(R_EAX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.DUP_X1 i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.DUP_X2 i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.popR(R_ECX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_ECX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.DUP2 i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.DUP2_X1 i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.popR(R_ECX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_ECX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.DUP2_X2 i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.popR(R_EBX);
	xasm.popR(R_ECX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_ECX);
	xasm.pushR(R_EBX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.SWAP i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_EDX);
    }

    public void visit(Instruction.IADD i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.addRR(R_EDX, R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LADD i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_ECX);
	xasm.popR(R_EDX);
	xasm.addRR(R_EDX, R_EAX);
	xasm.popR(R_EDX);
	xasm.adcRR(R_ECX, R_EDX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.FADD i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM32(R_EDX);
	xasm.faddM32(R_EDX, 4);
	xasm.fstpM32(R_EDX, 4);
	xasm.addIR(4, R_ESP);
    }

    public void visit(Instruction.DADD i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM64(R_EDX);
	xasm.faddM64(R_EDX, 8);
	xasm.fstpM64(R_EDX, 8);
	xasm.addIR(8, R_ESP);
    }

    public void visit(Instruction.ISUB i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.subRR(R_EAX, R_EDX);
	xasm.pushR(R_EDX);
    }

    public void visit(Instruction.LSUB i) {
	
	xasm.popR(R_EDX);
	xasm.popR(R_ECX);
	xasm.popR(R_EAX);
	xasm.subRR(R_EDX, R_EAX);
	xasm.popR(R_EDX);
	xasm.sbbRR(R_ECX, R_EDX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.FSUB i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM32(R_EDX, 4);
	xasm.fsubM32(R_EDX);
	xasm.fstpM32(R_EDX, 4);
	xasm.addIR(4, R_ESP);
    }

    public void visit(Instruction.DSUB i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM64(R_EDX, 8);
	xasm.fsubM64(R_EDX);
	xasm.fstpM64(R_EDX, 8);
	xasm.addIR(8, R_ESP);
    }

    public void visit(Instruction.IMUL i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.mulRR(R_EDX, R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LMUL i) {
	
	// clobbers ebx,esi,edi
	xasm.popR(R_EBX); // 1
	xasm.popR(R_ECX); // 2
	xasm.popR(R_EAX); // 3
	xasm.movRR(R_EAX, R_EDI); // copy 3
	xasm.popR(R_ESI); // 4
	// eax * ebx -> edx:eax
	xasm.umulR(R_EBX); // 1 * 3
	xasm.mulRR(R_ESI, R_EBX); // 1 * 4
	xasm.mulRR(R_EDI, R_ECX); // 2 * 3
	xasm.addRR(R_EBX, R_EDX); // h(1*4) + h(1*3)
	xasm.addRR(R_ECX, R_EDX); // h(2*3) + h(1*4) + h(1*3) 
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.FMUL i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM32(R_EDX);
	xasm.fmulM32(R_EDX, 4);
	xasm.fstpM32(R_EDX, 4);
	xasm.addIR(4, R_ESP);
    }

    public void visit(Instruction.DMUL i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM64(R_EDX);
	xasm.fmulM64(R_EDX, 8);
	xasm.fstpM64(R_EDX, 8);
	xasm.addIR(8, R_ESP);
    }

    public void visit(Instruction.IDIV i) {
	
	xasm.popR(R_ECX);
	generateDivisionByIntZeroCheck(R_ECX);
	xasm.popR(R_EAX);
	xasm.cdq();
	xasm.divR(R_ECX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LDIV i) { // call __divdi3
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	generateDivisionByLongZeroCheck(R_EAX, R_EDX);
	xasm.popR(R_ECX);
	xasm.popR(R_EBX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_EBX);
	xasm.pushR(R_ECX);
	// re-push args!
	generateCFunctionCall("ldiv", 4);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.FDIV i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM32(R_EDX, 4);
	xasm.fdivM32(R_EDX);
	xasm.fstpM32(R_EDX, 4);
	xasm.addIR(4, R_ESP);
    }

    public void visit(Instruction.DDIV i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM64(R_EDX, 8);
	xasm.fdivM64(R_EDX);
	xasm.fstpM64(R_EDX, 8);
	xasm.addIR(8, R_ESP);
    }

    public void visit(Instruction.IREM i) {
	
	xasm.popR(R_ECX);
	generateDivisionByIntZeroCheck(R_ECX);
	xasm.popR(R_EAX);
	xasm.cdq();
	xasm.divR(R_ECX);
	xasm.pushR(R_EDX);
    }

    public void visit(Instruction.LREM i)  { // call __moddi3
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	generateDivisionByLongZeroCheck(R_EAX, R_EDX);
	xasm.popR(R_ECX);
	xasm.popR(R_EBX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_EBX);
	xasm.pushR(R_ECX);
	generateCFunctionCall("lrem", 4);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.FREM i)  { // call fmod w/ conversion to doubles

	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM32(R_EDX, 0);
	xasm.fldM32(R_EDX, 4);
	xasm.fstpM64(R_EDX, -8);
	xasm.fstpM64(R_EDX, 0);
	xasm.subIR(8, R_ESP);
	generateCFunctionCall("fmod", 4);
	xasm.subIR(4, R_ESP);
	xasm.movRR(R_ESP, R_EDX);
	xasm.fstpM32(R_EDX);
    }

    public void visit(Instruction.DREM i)  { // call fmod
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.popR(R_ECX);
	xasm.popR(R_EBX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
	xasm.pushR(R_EBX);
	xasm.pushR(R_ECX);
	generateCFunctionCall("fmod", 4);
	xasm.subIR(8, R_ESP);
	xasm.movRR(R_ESP, R_EDX);
	xasm.fstpM64(R_EDX);
    }

    public void visit(Instruction.INEG i) {
	
	xasm.popR(R_EAX);
	xasm.movIR(0, R_EDX);
	xasm.subRR(R_EAX, R_EDX);
	xasm.pushR(R_EDX);
    }

    public void visit(Instruction.LNEG i) {
	
	xasm.popR(R_ECX);
	xasm.movIR(0, R_EAX);
	xasm.subRR(R_ECX, R_EAX);
	xasm.popR(R_ECX);
	xasm.movIR(0, R_EDX);
	xasm.sbbRR(R_ECX, R_EDX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.FNEG i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM32(R_EDX);
	xasm.fchs();
	xasm.fstpM32(R_EDX);
    }

    public void visit(Instruction.DNEG i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM64(R_EDX);
	xasm.fchs();
	xasm.fstpM64(R_EDX);
    }

    public void visit(Instruction.ISHL i) {
	
	xasm.popR(R_ECX);
	xasm.andIR(0x1f, R_ECX);
	xasm.popR(R_EAX);
	xasm.shlR(R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LSHL i) {
	
	xasm.popR(R_ECX); // width
	xasm.andIR(0x3f, R_ECX);
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.shldR(R_EDX, R_EAX);
	xasm.shlR(R_EAX);
	xasm.testIR(32, R_ECX);
	Branch b0 = xasm.setBranchSourceAndJcc(J_E);
	xasm.movRR(R_EAX, R_EDX);
	xasm.movIR(0, R_EAX);
	xasm.setBranchTarget(b0);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.ISHR i) {
	
	xasm.popR(R_ECX);
	xasm.andIR(0x1f, R_ECX);
	xasm.popR(R_EAX);
	xasm.sarR(R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LSHR i) {
	
	xasm.popR(R_ECX); // width
	xasm.andIR(0x3f, R_ECX);
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.shrdR(R_EAX, R_EDX);
	xasm.sarR(R_EDX);
	xasm.testIR(32, R_ECX);
	Branch b0 = xasm.setBranchSourceAndJcc(J_E);
	xasm.movRR(R_EDX, R_EAX);
	xasm.sarRI(R_EDX, (byte)31);
	xasm.setBranchTarget(b0);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.IUSHR i) {
	
	xasm.popR(R_ECX);
	xasm.andIR(0x1f, R_ECX);
	xasm.popR(R_EAX);
	xasm.shrR(R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LUSHR i) {
	
	xasm.popR(R_ECX); // width
	xasm.andIR(0x3f, R_ECX);
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.shrdR(R_EAX, R_EDX);
	xasm.shrR(R_EDX);
	xasm.testIR(32, R_ECX);
	Branch b0 = xasm.setBranchSourceAndJcc(J_E);
	xasm.movRR(R_EDX, R_EAX);
	xasm.movIR(0, R_EDX);
	xasm.setBranchTarget(b0);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.IAND i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.andRR(R_EDX, R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LAND i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_ECX);
	xasm.popR(R_EDX);
	xasm.andRR(R_EDX, R_EAX);
	xasm.popR(R_EDX);
	xasm.andRR(R_ECX, R_EDX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.IOR i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.orRR(R_EDX, R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LOR i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_ECX);
	xasm.popR(R_EDX);
	xasm.orRR(R_EDX, R_EAX);
	xasm.popR(R_EDX);
	xasm.orRR(R_ECX, R_EDX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.IXOR i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.xorRR(R_EDX, R_EAX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.LXOR i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_ECX);
	xasm.popR(R_EDX);
	xasm.xorRR(R_EDX, R_EAX);
	xasm.popR(R_EDX);
	xasm.xorRR(R_ECX, R_EDX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.IINC i) {
	
	int index = i.getLocalVariableOffset(buf);
	int delta = i.getValue(buf);
	int localOffset = getNativeLocalOffset(index);
	xasm.addIM(delta, R_EBP, localOffset);
    }

/*
    public void visit(Instruction.WIDE_IINC i) {
	int index = i.getLocalVariableOffset(buf);
	int delta = i.getValue(buf);
	int localOffset = getNativeLocalOffset(index);
	xasm.addIM(delta, R_EBP, localOffset);
    }
*/

    public void visit(Instruction.I2L i) {
	
	xasm.popR(R_EAX);
	xasm.movRR(R_EAX, R_EDX);
	xasm.movIR(31, R_ECX);
	xasm.sarR(R_EDX);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.I2F i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fildM32(R_EDX);
	xasm.fstpM32(R_EDX);
    }

    public void visit(Instruction.I2D i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fildM32(R_EDX);
	xasm.fstpM64(R_EDX, -4);
	xasm.subIR(4, R_ESP);
    }

    public void visit(Instruction.L2I i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.pushR(R_EAX);
    }

    public void visit(Instruction.L2F i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fildM64(R_EDX);
	xasm.fstpM32(R_EDX, 4);
	xasm.addIR(4, R_ESP);
    }

    public void visit(Instruction.L2D i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fildM64(R_EDX);
	xasm.fstpM64(R_EDX);
    }

    public void visit(Instruction.F2I i) {
	generateCFunctionCall("f2i", 1);
	xasm.pushR(R_EAX);
	/*
	xasm.movRR(R_ESP, R_EDI);
	xasm.movMR(R_EDI, 0, R_EDX);
	xasm.cmpRR(R_EDX, R_EDX);
	Branch b1 = xasm.setBranchSourceAndJcc(J_E);
	// NaN
	xasm.movIM(0, R_EDI, 0);
	Branch b2 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b1);
	// Non NaN
	xasm.movIR(Float.floatToIntBits(Float.POSITIVE_INFINITY), R_EAX);
	xasm.movIR(Float.floatToIntBits(Float.NEGATIVE_INFINITY), R_ECX);
	xasm.cmpRR(R_EAX, R_EDX);
	Branch b3 = xasm.setBranchSourceAndJcc(J_NE);
	// == Positive infinity
	xasm.movIM(Integer.MAX_VALUE, R_EDI, 0);
	Branch b4 = xasm.setBranchSourceAndJmp();

	xasm.setBranchTarget(b3);
	xasm.cmpRR(R_ECX, R_EDX);
	Branch b5 = xasm.setBranchSourceAndJcc(J_NE);
	// == Negative infinity
	xasm.movIM(Integer.MIN_VALUE, R_EDI, 0);
	Branch b6 = xasm.setBranchSourceAndJmp();

	xasm.setBranchTarget(b5);
	*/
	/*
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM32(R_EDX);
	xasm.fnstcw(R_EDX, -4);
	xasm.movMR(R_EDX, -4, R_EAX);
	xasm.movI8M8((byte)0xc, R_EDX, (byte)-3);
	xasm.fldcw(R_EDX, -4);
	xasm.movRM(R_EAX, R_EDX, -4);
	xasm.fistM32(R_EDX);
	xasm.fldcw(R_EDX, -4);

	xasm.setBranchTarget(b2);
	xasm.setBranchTarget(b4);
	xasm.setBranchTarget(b6);
	*/
    }

    public void visit(Instruction.F2L i) {
	generateCFunctionCall("f2l", 1);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);
	/*	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM32(R_EDX);
	xasm.fnstcw(R_EDX, -8);
	xasm.movMR(R_EDX, -8, R_EAX);
	xasm.movI8M8((byte)0xc, R_EDX, (byte)-7);
	xasm.fldcw(R_EDX, -8);
	xasm.movRM(R_EAX, R_EDX, -8);
	xasm.fistM64(R_EDX,  -4);
	xasm.fldcw(R_EDX, -8);
	xasm.subIR(4, R_ESP);
	*/
    }

    public void visit(Instruction.F2D i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM32(R_EDX);
	xasm.fstpM64(R_EDX,  -4);
	xasm.subIR(4, R_ESP);
    }

    public void visit(Instruction.D2I i) {
	generateCFunctionCall("d2i", 2);
	xasm.pushR(R_EAX);
	/*
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM64(R_EDX);
	xasm.fnstcw(R_EDX, -4);
	xasm.movMR(R_EDX, -4, R_EAX);
	xasm.movI8M8((byte)0xc, R_EDX, (byte)-3);
	xasm.fldcw(R_EDX, -4);
	xasm.movRM(R_EAX, R_EDX, -4);
	xasm.fistM32(R_EDX, 4);
	xasm.fldcw(R_EDX, -4);
	xasm.addIR(4, R_ESP);
	*/
    }

    public void visit(Instruction.D2L i) {
	generateCFunctionCall("d2l", 2);
	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);

	/*
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM64(R_EDX);
	xasm.fnstcw(R_EDX, -4);
	xasm.movMR(R_EDX, -4, R_EAX);
	xasm.movI8M8((byte)0xc, R_EDX, (byte)-3);
	xasm.fldcw(R_EDX, -4);
	xasm.movRM(R_EAX, R_EDX, -4);
	xasm.fistM64(R_EDX);
	xasm.fldcw(R_EDX, -4);
	*/
    }

    public void visit(Instruction.D2F i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.fldM64(R_EDX);
	xasm.fstpM32(R_EDX, 4);
	xasm.addIR(4, R_ESP);
    }

    public void visit(Instruction.I2B i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.movsxM8R(R_EDX, 0, R_EAX);
	xasm.movRM(R_EAX, R_EDX, 0);
    }

    public void visit(Instruction.I2C i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.movzxM16R(R_EDX, 0, R_EAX);
	xasm.movRM(R_EAX, R_EDX, 0);
    }

    public void visit(Instruction.I2S i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.movsxM16R(R_EDX, 0, R_EAX);
	xasm.movRM(R_EAX, R_EDX, 0);
    }

    // ifeq, ifne, ifle, iflt, ifge, ifgt, ifnull, ifnonnull
    public void visit(Instruction.IfZ i) {
	
	int branchBCOffset = i.getBranchTarget(buf);

	int opcode = i.getOpcode();
	byte condition;

	switch(opcode) {
	case Opcodes.IFEQ:
	case Opcodes.IFNULL:
	    condition = J_E;
	    break;
	case Opcodes.IFNE:
	case Opcodes.IFNONNULL:
	    condition = J_NE;
	    break;
	case Opcodes.IFLT:
	    condition = J_L;
	    break;
	case Opcodes.IFGE:
	    condition = J_GE;
	    break;
	case Opcodes.IFGT:
	    condition = J_G;
	    break;
	case Opcodes.IFLE:
	    condition = J_LE;
	    break;
	default:
	    throw new Error();
	}
	xasm.popR(R_EAX);
	xasm.cmpRI(R_EAX, 0);
	if (branchBCOffset <= 0) { // backward
	    int branchNativeOffset = 
		codeGenContext.getBytecodePC2NativePC(getPC() + branchBCOffset) 
		- xasm.getPC();
	    xasm.jcc(condition, branchNativeOffset);
	} else { // forward
	    int jcc_unlinked_imm_offset = xasm.imm_offset_jcc_unlinked();
	    int jcc_unlinked_startPC = xasm.getPC();
	    codeGenContext.
		addRelativeJumpPatch(jcc_unlinked_startPC + jcc_unlinked_imm_offset,
				     getPC() + branchBCOffset);
	    xasm.jcc_unlinked(condition);
	}
    }

    // goto, goto_w
    public void visit(Instruction.GotoInstruction i) {
	
	int branchBCOffset = i.getTarget(buf);

	if (branchBCOffset <= 0) {
	    int branchNativeOffset = 
		codeGenContext.
		getBytecodePC2NativePC(getPC() + branchBCOffset) - xasm.getPC();
	    xasm.jmp(branchNativeOffset);
	} else {
	    int jmp_unlinked_imm_offset = xasm.imm_offset_jmp_unlinked();
	    int jmp_unlinked_startPC = xasm.getPC();
	    codeGenContext.
		addRelativeJumpPatch(jmp_unlinked_startPC + jmp_unlinked_imm_offset,
				     getPC() + branchBCOffset);
	    xasm.jmp_unlinked();
	}
    }

    public void visit(Instruction.LCMP i) {
	
	xasm.popR(R_EAX);
	xasm.popR(R_ECX);
	xasm.popR(R_EDX);
	xasm.popR(R_EBX);
	xasm.cmpRR(R_EBX, R_ECX);
	Branch b1 = xasm.setBranchSourceAndJcc(J_L);
	Branch b2 = xasm.setBranchSourceAndJcc(J_G);
	xasm.cmpRR(R_EDX, R_EAX);
	Branch b3 = xasm.setBranchSourceAndJcc(J_B);
	Branch b4 = xasm.setBranchSourceAndJcc(J_A);
	xasm.pushI32(0);
	Branch b5 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b1);
	xasm.setBranchTarget(b3);
	xasm.pushI32(-1);
	Branch b6 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b2);
	xasm.setBranchTarget(b4);
	xasm.pushI32(1);
	xasm.setBranchTarget(b5);
	xasm.setBranchTarget(b6);
    }

    public void visit(Instruction.FCMPL i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.addIR(8, R_ESP);
	xasm.fldM32(R_EDX);
	xasm.fldM32(R_EDX, 4);
	xasm.fcmp();
	Branch b0 = xasm.setBranchSourceAndJcc(J_P);
	Branch b1 = xasm.setBranchSourceAndJcc(J_C);
	Branch b2 = xasm.setBranchSourceAndJcc(J_NZ);
	xasm.pushI32(0);
	Branch b3 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b2);
	xasm.pushI32(1);
	Branch b4 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);
	xasm.setBranchTarget(b1);
	xasm.pushI32(-1);
	xasm.setBranchTarget(b3);
	xasm.setBranchTarget(b4);
    }

    public void visit(Instruction.FCMPG i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.addIR(8, R_ESP);
	xasm.fldM32(R_EDX);
	xasm.fldM32(R_EDX, 4);
	xasm.fcmp();
	Branch b0 = xasm.setBranchSourceAndJcc(J_P);
	Branch b1 = xasm.setBranchSourceAndJcc(J_C);
	Branch b2 = xasm.setBranchSourceAndJcc(J_NZ);
	xasm.pushI32(0);
	Branch b3 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);
	xasm.setBranchTarget(b2);
	xasm.pushI32(1);
	Branch b4 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b1);
	xasm.pushI32(-1);
	xasm.setBranchTarget(b3);
	xasm.setBranchTarget(b4);
    }

    public void visit(Instruction.DCMPL i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.addIR(16, R_ESP);
	xasm.fldM64(R_EDX);
	xasm.fldM64(R_EDX, 8);
	xasm.fcmp();
	Branch b0 = xasm.setBranchSourceAndJcc(J_P);
	Branch b1 = xasm.setBranchSourceAndJcc(J_C);
	Branch b2 = xasm.setBranchSourceAndJcc(J_NZ);
	xasm.pushI32(0);
	Branch b3 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b2);
	xasm.pushI32(1);
	Branch b4 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);
	xasm.setBranchTarget(b1);
	xasm.pushI32(-1);
	xasm.setBranchTarget(b3);
	xasm.setBranchTarget(b4);
    }

    public void visit(Instruction.DCMPG i) {
	
	xasm.movRR(R_ESP, R_EDX);
	xasm.addIR(16, R_ESP);
	xasm.fldM64(R_EDX);
	xasm.fldM64(R_EDX, 8);
	xasm.fcmp();
	Branch b0 = xasm.setBranchSourceAndJcc(J_P);
	Branch b1 = xasm.setBranchSourceAndJcc(J_C);
	Branch b2 = xasm.setBranchSourceAndJcc(J_NZ);
	xasm.pushI32(0);
	Branch b3 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);
	xasm.setBranchTarget(b2);
	xasm.pushI32(1);
	Branch b4 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b1);
	xasm.pushI32(-1);
	xasm.setBranchTarget(b3);
	xasm.setBranchTarget(b4);
    }

    // if_icmpxx, if_acmpxx
    public void visit(Instruction.IfCmp i) {
	
	int branchBCOffset = i.getBranchTarget(buf);

	int opcode = i.getOpcode();
	byte condition;
	switch(opcode) {
	case Opcodes.IF_ICMPEQ:
	case Opcodes.IF_ACMPEQ:
	    condition = J_E;
	    break;
	case Opcodes.IF_ICMPNE:
	case Opcodes.IF_ACMPNE:
	    condition = J_NE;
	    break;
	case Opcodes.IF_ICMPLT:
	    condition = J_L;
	    break;
	case Opcodes.IF_ICMPGE:
	    condition = J_GE;
	    break;
	case Opcodes.IF_ICMPGT:
	    condition = J_G;
	    break;
	case Opcodes.IF_ICMPLE:
	    condition = J_LE;
	    break;
	default:
	    throw new Error();
	}
	xasm.popR(R_EAX);
	xasm.popR(R_EDX);
	xasm.cmpRR(R_EDX, R_EAX);
	if (branchBCOffset <= 0) { // backward
	    int branchNativeOffset = 
		codeGenContext.
		getBytecodePC2NativePC(getPC() + branchBCOffset) - xasm.getPC();
	    xasm.jcc(condition, branchNativeOffset);
	} else { // forward
	    int jcc_unlinked_imm_offset = xasm.imm_offset_jcc_unlinked();
	    int jcc_unlinked_startPC = xasm.getPC();
	    codeGenContext.
		addRelativeJumpPatch(jcc_unlinked_startPC + jcc_unlinked_imm_offset,
				     getPC() + branchBCOffset);
	    xasm.jcc_unlinked(condition);
	}
    }

    // PIC version
    public void visit(Instruction.TABLESWITCH i) {
	
	int def = i.getDefaultTarget(buf);
	int high = i.getHigh(buf);
	int low = i.getLow(buf);
	int[] targets = i.getTargets(buf);

	xasm.popR(R_EDX);
	xasm.subIR(low, R_EDX);
	xasm.cmpRI(R_EDX, high - low);
	if (def <= 0) {
	    int defaultNativeOffset =
		codeGenContext.getBytecodePC2NativePC(getPC() + def) - xasm.getPC();
	    xasm.jcc(J_NBE, defaultNativeOffset);
	} else {
	    int jcc_unlinked_imm_offset = xasm.imm_offset_jcc_unlinked();
	    int jcc_unlinked_startPC = xasm.getPC();
	    codeGenContext.
		addRelativeJumpPatch(jcc_unlinked_startPC + jcc_unlinked_imm_offset,
				     getPC() + def);
	    xasm.jcc_unlinked(J_NBE);
	}

	int offset_until_table = xasm.l_popR() + xasm.l_movRR() 
	    + xasm.l_leaMRI4() + xasm.l_addIR()
	    + xasm.l_addMRI4() + xasm.l_jmpAbsR();

	xasm.call(0);
	xasm.popR(R_ECX);
	xasm.movRR(R_ECX, R_EAX);

	xasm.leaMRI4(R_ECX, R_EDX, offset_until_table, R_EAX);
	xasm.addIR(4, R_EAX);

	xasm.addMRI4(R_ECX, R_EDX, offset_until_table, R_EAX);
	xasm.jmpAbsR(R_EAX);

	// filling the jump table
	for(int j = 0; j <= high - low; j++) {
	    int jumpOffset = targets[j];
	    codeGenContext.addRelativeJumpPatch(xasm.getPC(),
						getPC() + jumpOffset);
	    xasm.write32(0); // dummy
	}
    }

    /*
    // non-PIC version
    public void visit(Instruction.TABLESWITCH i) {
	int def = i.getDefaultTarget(buf);
	int high = i.getHigh(buf);
	int low = i.getLow(buf);
	int[] targets = i.getTargets(buf);
	xasm.popR(R_EDX);
	xasm.addIR(4, R_ESP);
	xasm.subIR(low, R_EDX);
	xasm.cmpRI(R_EDX, high - low);
	if (def <= 0) {
	    int defaultNativeOffset =
		codeGenContext.getBytecodePC2NativePC(getPC() + def) - xasm.getPC();
	    xasm.jcc(J_NBE, defaultNativeOffset);
	} else {
	    int jcc_unlinked_imm_offset = xasm.imm_offset_jcc_unlinked();
	    int jcc_unlinked_startPC = xasm.getPC();
	    codeGenContext.
		addRelativeJumpPatch(jcc_unlinked_startPC + jcc_unlinked_imm_offset,
				     getPC() + def);
	    xasm.jcc_unlinked(J_NBE);
	}

	int jmp_startPC = xasm.getPC();
	int jmp_imm_offset = xasm.imm_offset_jmpAbsMS_unlinked();
	int l_jmp = xasm.l_jmpAbsMS_unlinked();
	int tableAddress = jmp_startPC + l_jmp;
	tableAddress = (tableAddress + 3) & ~3; // word alignment
	codeGenContext.addAbsoluteNativeRefPatch(jmp_startPC + jmp_imm_offset,
						 tableAddress);
	
	xasm.jmpAbsMS_unlinked(R_EDX);

	// filling the padding
	int npc = xasm.getPC();
	while (npc < tableAddress) {
	    xasm.nop();
	    npc = xasm.getPC();
	}

	// filling the jump table
	for(int j = 0; j <= high - low; j++) {
	    int jumpOffset = targets[j];
	    codeGenContext.addAbsoluteBCRefPatch(xasm.getPC(),
						 getPC() + jumpOffset);
	    xasm.write32(0); // dummy
	}
    }
    */

    public void visit(Instruction.LOOKUPSWITCH i) {
	
	int def = i.getDefaultTarget(buf);
	int npairs = i.getTargetCount(buf) - 1;
	int[] cases = i.getIndexForTargets(buf);
	int[] targets = i.getTargets(buf);

	xasm.popR(R_EDX);

	for (int j = 0 ; j < npairs; j++) {
	    int caseValue = cases[j];
	    int jumpOffset = targets[j];
	    xasm.cmpRI(R_EDX, caseValue);

	    if (jumpOffset <= 0) {
		int jumpNativeOffset = 
		    codeGenContext.getBytecodePC2NativePC(getPC() + jumpOffset)
		    - xasm.getPC();
		xasm.jcc(J_E, jumpNativeOffset);
	    } else {
		int jcc_unlinked_imm_offset = xasm.imm_offset_jcc_unlinked();
		int jcc_unlinked_startPC = xasm.getPC();
		codeGenContext.
		    addRelativeJumpPatch(jcc_unlinked_startPC + jcc_unlinked_imm_offset,
					 getPC() + jumpOffset);
		xasm.jcc_unlinked(J_E);
	    }
	}

	if (def <= 0) {
	    int defaultJumpNativeOffset = 
		codeGenContext.getBytecodePC2NativePC(getPC() + def) 
		- xasm.getPC();
	    xasm.jmp(defaultJumpNativeOffset);
	} else {
	    int jmp_unlinked_imm_offset = xasm.imm_offset_jmp_unlinked();
	    int jmp_unlinked_startPC = xasm.getPC();
	    codeGenContext.
		addRelativeJumpPatch(jmp_unlinked_startPC + jmp_unlinked_imm_offset,
				     getPC() + def);
	    xasm.jmp_unlinked();
	}
    }

    public void visit(Instruction.JsrInstruction i) {
	

	int jumpOffset = i.getTarget(buf);
	if (jumpOffset <= 0) { // backward
	    int l_call = xasm.l_call();
	    int jumpNativeOffset = 
		codeGenContext.getBytecodePC2NativePC(getPC() + jumpOffset) 
		- (xasm.getPC() + l_call);
	    xasm.call(jumpNativeOffset);
	} else { // forward
	    int call_unlinked_imm_offset = xasm.call_unlinked_imm_offset();
	    codeGenContext.addRelativeJumpPatch(xasm.getPC() + call_unlinked_imm_offset,
						getPC() + jumpOffset);
	    xasm.call_unlinked();
	}
    }

    public void visit(Instruction.RET i) {
	

	int index = i.getLocalVariableOffset(buf);
	int localOffset = getNativeLocalOffset(index);
	xasm.movMR(R_EBP, localOffset, R_EAX);
	xasm.jmpAbsR(R_EAX);
    }

    public void visit(Instruction.WIDE_RET i) {
	
	int index = i.getLocalVariableOffset(buf);
	int localOffset = getNativeLocalOffset(index);
	xasm.movMR(R_EBP, localOffset, R_EAX);
	xasm.jmpAbsR(R_EAX);
    }

    // ireturn, freturn, areturn, 
    public void visit(Instruction.ReturnValue i) {
	
	if (isSynchronized) {
	    int localOffset = getNativeLocalOffset(0);    // this or shst
	    xasm.pushM(R_EBP, localOffset);
	    generateCSACall(precomputed.csa_monitorExit_index, 
			    precomputed.csa_monitorExit_desc);
	}
	//int argLength = stackLayout.getArgLength();
	if (debugPrintOn)
	    debugPrint("[Returning from " + getSelector().toString() 
		       + " ##" + counter + "##]\n");
	int opcode = i.getOpcode();
	switch(opcode) {
	case Opcodes.IRETURN:
	    xasm.popR(R_EAX);
	    break;
	case Opcodes.LRETURN:
	    xasm.popR(R_EAX);
	    xasm.popR(R_EDX);
	    break;
	case Opcodes.FRETURN:
	    xasm.movRR(R_ESP, R_EDX);
	    xasm.fldM32(R_EDX);
	    break;
	case Opcodes.DRETURN:
	    xasm.movRR(R_ESP, R_EDX);
	    xasm.fldM64(R_EDX);
	    break;
	case Opcodes.ARETURN:
	    xasm.popR(R_EAX);
	    break;
	default:
	    throw new OVMError();
	}
	xasm.leaMR(R_EBP, -12, R_ESP);
	xasm.popR(R_EBX);
	xasm.popR(R_ESI);
	xasm.popR(R_EDI);
	
	xasm.popR(R_EBP);
	xasm.ret();
    }

    public void visit(Instruction.RETURN i) {
	
	if (isSynchronized) {
	    int localOffset = getNativeLocalOffset(0);    // this or shst
	    xasm.pushM(R_EBP, localOffset);
	    generateCSACall(precomputed.csa_monitorExit_index, 
			    precomputed.csa_monitorExit_desc);
	}
	if (debugPrintOn)
	    debugPrint("[Returning from " + getSelector().toString() 
		       + " ##" + counter + "##]\n");
	xasm.leaMR(R_EBP, -12, R_ESP);
	xasm.popR(R_EBX);
	xasm.popR(R_ESI);
	xasm.popR(R_EDI);

	xasm.popR(R_EBP);
	xasm.ret();
    }

    public void visit(Instruction.ATHROW i) {
	
	xasm.popR(R_EDX);

	generateNullCheck(R_EDX);

	xasm.pushR(R_EDX);                                    // push exception obj
	generateCSACall(precomputed.csa_processThrowable_index, 
			precomputed.csa_processThrowable_desc);
    }

    
    public void visit(Instruction.GETFIELD i) {
	
	int cpindex = i.getCPIndex(buf);
	Selector.Field sel = i.getSelector(buf, cp);
	boolean isWidePrimitive = sel.getDescriptor().isWidePrimitive();

	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
            try {
		ConstantResolvedInstanceFieldref ifi =
		    cp.resolveInstanceField(cpindex);
		S3Field field = (S3Field)ifi.getField();
		int offset = field.getOffset();
		xasm.popR(R_EAX);
		generateNullCheck(R_EAX);
		if (isWidePrimitive)
		    xasm.pushM(R_EAX, offset + 4);
		xasm.pushM(R_EAX, offset);
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    int pc_high = 0;
	    int pc_low = 0;

	    // GETFIELD_QUICK
	    xasm.popR(R_EAX);
	    generateNullCheck(R_EAX);
	    if (isWidePrimitive) {
		pc_high = xasm.pushM_to_be_patched(R_EAX); // the offset will be patched at runtime
	    }
	    pc_low = xasm.pushM_to_be_patched(R_EAX); // the offset will be patched at runtime
	    
	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    // GETFIELD
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveInstanceField_index, 
			    precomputed.csa_resolveInstanceField_desc);
	    xasm.popR(R_EDX);
	
	    //
	    xasm.selfModifyJmpWithNops(b0);
	    if (isWidePrimitive) {
		xasm.movRR(R_EDX, R_ECX);
		xasm.addIR(4, R_ECX);
		xasm.selfModify4Bytes(pc_high, R_ECX);
	    }
	    xasm.selfModify4Bytes(pc_low, R_EDX);

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	}
    }

    public void visit(Instruction.PUTFIELD i) {
	if (i instanceof Instruction.PUTFIELD_WITH_BARRIER_REF)
	    return;
	
	int cpindex = i.getCPIndex(buf);
	Selector.Field sel = i.getSelector(buf, cp);
	boolean isWidePrimitive = sel.getDescriptor().isWidePrimitive();

	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
            try {
		ConstantResolvedInstanceFieldref ifi =
		    cp.resolveInstanceField(cpindex);
		S3Field field = (S3Field)ifi.getField();
		int offset = field.getOffset();
		if (isWidePrimitive) {
		    xasm.popR(R_ECX);
		    xasm.popR(R_EDX);
		    xasm.popR(R_EAX);
		    generateNullCheck(R_EAX);
		    xasm.movRM(R_ECX, R_EAX, offset);
		    xasm.movRM(R_EDX, R_EAX, offset + 4);
		} else {
		    xasm.popR(R_ECX);
		    xasm.popR(R_EAX);
		    generateNullCheck(R_EAX);
		    xasm.movRM(R_ECX, R_EAX, offset);
		}
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();
	    int pc_high = 0;
	    int pc_low = 0;
	    
	    // PUTFIELD_QUICK
	    xasm.popR(R_ECX);
	    if (isWidePrimitive)
		xasm.popR(R_EDX);
	    xasm.popR(R_EAX);
	    generateNullCheck(R_EAX);
	    pc_low = xasm.movRM_to_be_patched(R_ECX, R_EAX); // the offset will be patched at runtime
	    if (isWidePrimitive) {
		pc_high = xasm.movRM_to_be_patched(R_EDX, R_EAX); // the offset will be patched at runtime
	    }

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    // PUTFIELD
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveInstanceField_index, 
			    precomputed.csa_resolveInstanceField_desc);
	    xasm.popR(R_EDX);
	
	    //
	    xasm.selfModifyJmpWithNops(b0);
	    if (isWidePrimitive) {
		xasm.movRR(R_EDX, R_ECX);
		xasm.addIR(4, R_ECX);
		xasm.selfModify4Bytes(pc_high, R_ECX);
	    }
	    xasm.selfModify4Bytes(pc_low, R_EDX);

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	}
    }

    public void visit(Instruction.GETFIELD_QUICK i) {
	
	int offset = i.getOffset(buf);
	xasm.popR(R_EAX);
	generateNullCheck(R_EAX);
	xasm.pushM(R_EAX, offset);
    }

    public void visit(Instruction.REF_GETFIELD_QUICK i) {
	
	int offset = i.getOffset(buf);
	xasm.popR(R_EAX);
	generateNullCheck(R_EAX);
	xasm.pushM(R_EAX, offset);
    }

    // this handles PUTFIELD_QUICK_WITH_BARRIER_REF, too?
    public void visit(Instruction.PUTFIELD_QUICK i) {
	
	int offset = i.getOffset(buf);
	xasm.popR(R_ECX);
	xasm.popR(R_EAX);
	generateNullCheck(R_EAX);
	xasm.movRM(R_ECX, R_EAX, offset);
    }

    public void visit(Instruction.PUTFIELD_WITH_BARRIER_REF i) {
	
	int cpindex = i.getCPIndex(buf);

	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
            try {
		ConstantResolvedInstanceFieldref ifi =
		    cp.resolveInstanceField(cpindex);
		S3Field field = (S3Field)ifi.getField();
		int offset = field.getOffset();
		xasm.popR(R_ECX);
		xasm.popR(R_EAX);
		generateNullCheck(R_EAX);
		xasm.pushR(R_ECX);    // value
		xasm.pushI32(offset);
		xasm.pushR(R_EAX);    // obj
		generateCSACall(precomputed.csa_putFieldBarrier_index,
				precomputed.csa_putFieldBarrier_desc);
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    // PUTFIELD_QUICK_WITH_BARRIER_REF
	    xasm.popR(R_ECX);
	    xasm.popR(R_EAX);
	    generateNullCheck(R_EAX);
	    xasm.pushR(R_ECX);    // value
	    xasm.pushI32_wide(-1); // offset (will be patched at runtime)
	    int patchPC = xasm.getPC();
	    xasm.pushR(R_EAX);    // obj
	    generateCSACall(precomputed.csa_putFieldBarrier_index,
			    precomputed.csa_putFieldBarrier_desc);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    // PUTFIELD_WITH_BARRIER_REF
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveInstanceField_index, 
			    precomputed.csa_resolveInstanceField_desc);
	    xasm.popR(R_EDX);
	
	    //
	    xasm.selfModifyJmpWithNops(b0);
	    xasm.selfModify4Bytes(patchPC, R_EDX);
	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	}
    }

    public void visit(Instruction.GETFIELD2_QUICK i) {
	
	int offset = i.getOffset(buf);
	xasm.popR(R_EAX);
	generateNullCheck(R_EAX);
	xasm.pushM(R_EAX, offset + 4);
	xasm.pushM(R_EAX, offset);
    }

    public void visit(Instruction.PUTFIELD2_QUICK i) {
	
	int offset = i.getOffset(buf);
	xasm.popR(R_ECX);
	xasm.popR(R_EDX);
	xasm.popR(R_EAX);

	generateNullCheck(R_EAX);

	xasm.movRM(R_ECX, R_EAX, offset);
	xasm.movRM(R_EDX, R_EAX, offset + 4);
    }

    // This handles PUTSTATIC_WITH_BARRIER_REF, too
    public void visit(Instruction.PUTSTATIC i) {
	
	int cpindex = i.getCPIndex(buf);
	int offset_fieldref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cpindex);
	Selector.Field sel = i.getSelector(buf, cp);
	boolean isWidePrimitive = sel.getDescriptor().isWidePrimitive();

	if (precomputed.isExecutive) {
            try {
		ConstantResolvedStaticFieldref sfi =
		    cp.resolveStaticField(cpindex);
		S3Field field = (S3Field)sfi.getField();
		int offset = field.getOffset();

		if (ED_OBJECT_DONT_MOVE) {
		    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
		    Branch b2 = xasm.setBranchTarget();

		    // fast path
		    int patch_pc = xasm.movIR_to_be_patched(R_EAX);
		    if (isWidePrimitive) {
			xasm.popR(R_ECX);
			xasm.popR(R_EDX);
			xasm.movRM(R_ECX, R_EAX, offset);
			xasm.movRM(R_EDX, R_EAX, offset + 4);
		    } else {
 			if (i instanceof Instruction.PUTSTATIC_WITH_BARRIER_REF) {
 			    // value is on the stack
 			    xasm.pushI32(offset);
 			    xasm.pushR(R_EAX);
 			    generateCSACall(precomputed.csa_putFieldBarrier_index,
 					    precomputed.csa_putFieldBarrier_desc);
 			} else {
			    xasm.popR(R_ECX);
			    xasm.movRM(R_ECX, R_EAX, offset);
			}
		    }

		    Branch b1 = xasm.setBranchSourceAndJmp();
		    xasm.setBranchTarget(b0);

		    // slow path
		    xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		    xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		    xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);

		    //
		    xasm.selfModifyJmpWithNops(b0);
		    xasm.selfModify4Bytes(patch_pc, R_EAX);
		    xasm.setBranchSourceAndJmp(b2);
		    xasm.setBranchTarget(b1);
		} else {
		    xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		    xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		    xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);
		    if (isWidePrimitive) {
			xasm.popR(R_ECX);
			xasm.popR(R_EDX);
			xasm.movRM(R_ECX, R_EAX, offset);
			xasm.movRM(R_EDX, R_EAX, offset + 4);
		    } else {
 			if (i instanceof Instruction.PUTSTATIC_WITH_BARRIER_REF) {
 			    // value is on the stack
 			    xasm.pushI32(offset);
			    xasm.pushR(R_EAX);
 			    generateCSACall(precomputed.csa_putFieldBarrier_index,
 					    precomputed.csa_putFieldBarrier_desc);
 			} else {
			    xasm.popR(R_ECX);
			    xasm.movRM(R_ECX, R_EAX, offset);
			}
		    }
		}
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();
	    int pc_high = 0;
	    int pc_low = 0;
	    int patch_pc_shst = 0;

	    if (ED_OBJECT_DONT_MOVE) {
		patch_pc_shst = xasm.movIR_to_be_patched(R_EAX);
	    } else {
		xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);
	    }
 	    if (i instanceof Instruction.PUTSTATIC_WITH_BARRIER_REF) {
 		// value is on the stack
 		pc_low = xasm.pushI32_to_be_patched();
 		xasm.pushR(R_EAX);
 		generateCSACall(precomputed.csa_putFieldBarrier_index,
 				precomputed.csa_putFieldBarrier_desc);
 	    } else {
		xasm.popR(R_ECX);
		if (isWidePrimitive)
		    xasm.popR(R_EDX);
		pc_low = xasm.movRM_to_be_patched(R_ECX, R_EAX); // the offset will be patched at runtime
		if (isWidePrimitive) {
		    pc_high = xasm.movRM_to_be_patched(R_EDX, R_EAX); // the offset will be patched at runtime
		}
	    }

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    // PUTFIELD
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveStaticField_index, 
			    precomputed.csa_resolveStaticField_desc);
	    xasm.popR(R_EDX);
	
	    //
	    xasm.selfModifyJmpWithNops(b0);

 	    if (i instanceof Instruction.PUTSTATIC_WITH_BARRIER_REF) {
 		xasm.selfModify4Bytes(pc_low, R_EDX);
 	    } else {
		if (isWidePrimitive) {
		    xasm.movRR(R_EDX, R_ECX);
		    xasm.addIR(4, R_ECX);
		    xasm.selfModify4Bytes(pc_high, R_ECX);
		}
		xasm.selfModify4Bytes(pc_low, R_EDX);
	    }

	    if (ED_OBJECT_DONT_MOVE) {
		xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);

		xasm.selfModify4Bytes(patch_pc_shst, R_EAX);
	    }

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	}
    }

    public void visit(Instruction.GETSTATIC i) {
	
	int cpindex = i.getCPIndex(buf);
	int offset_fieldref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cpindex);
	Selector.Field sel = i.getSelector(buf, cp);
	boolean isWidePrimitive = sel.getDescriptor().isWidePrimitive();

	if (precomputed.isExecutive) {
            try {
		ConstantResolvedStaticFieldref sfi =
		    cp.resolveStaticField(cpindex);
		S3Field field = (S3Field)sfi.getField();
		int offset = field.getOffset();

		if (ED_OBJECT_DONT_MOVE) {
		    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
		    Branch b2 = xasm.setBranchTarget();

		    // fast path
		    int patch_pc = xasm.movIR_to_be_patched(R_EAX);
		    if (isWidePrimitive)
			xasm.pushM(R_EAX, offset + 4);
		    xasm.pushM(R_EAX, offset);

		    Branch b1 = xasm.setBranchSourceAndJmp();
		    xasm.setBranchTarget(b0);

		    // slow path
		    xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		    xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		    xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);

		    //
		    xasm.selfModifyJmpWithNops(b0);
		    xasm.selfModify4Bytes(patch_pc, R_EAX);
		    xasm.setBranchSourceAndJmp(b2);
		    xasm.setBranchTarget(b1);
		} else {
		    xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		    xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		    xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);
		    if (isWidePrimitive)
			xasm.pushM(R_EAX, offset + 4);
		    xasm.pushM(R_EAX, offset);
		}
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    int pc_high = 0;
	    int pc_low = 0;
	    int patch_pc_shst = 0;

	    if (ED_OBJECT_DONT_MOVE) {
		patch_pc_shst = xasm.movIR_to_be_patched(R_EAX);
	    } else {
		xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);
	    }
	    if (isWidePrimitive) {
		pc_high = xasm.pushM_to_be_patched(R_EAX);
	    }
	    pc_low = xasm.pushM_to_be_patched(R_EAX);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    // GETSTATIC
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveStaticField_index, 
			    precomputed.csa_resolveStaticField_desc);
	    xasm.popR(R_EDX);
	
	    //
	    xasm.selfModifyJmpWithNops(b0);
	    if (isWidePrimitive) {
		xasm.movRR(R_EDX, R_ECX);
		xasm.addIR(4, R_ECX);
		xasm.selfModify4Bytes(pc_high, R_ECX);
	    }
	    xasm.selfModify4Bytes(pc_low, R_EDX);

	    if (ED_OBJECT_DONT_MOVE) {
		xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		xasm.movMR(R_EDX, offset_fieldref_in_cpvalues, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_shst_in_staticfref, R_EAX);

		xasm.selfModify4Bytes(patch_pc_shst, R_EAX);
	    }

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	}
    }

    public void visit(Instruction.INVOKEVIRTUAL i) {
	
	int cpindex = i.getCPIndex(buf);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
	int arg_length = desc.getArgumentLength() / 4;

	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
            try {
                ConstantResolvedInstanceMethodref ifi = cp.resolveInstanceMethod(cpindex);
                int vtbl_index = ifi.getOffset();
                int offset_cf_in_vtbl = getArrayElementOffset(executiveDomain, OBJECT, vtbl_index);
		generateArgumentRepushing(desc);
		xasm.popR(R_EAX); // RAX = receiver
		xasm.pushR(R_EAX);
		generateNullCheck(R_EAX);
		xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));   // edx = next bp
		xasm.movMR(R_EDX, precomputed.offset_vtbl_in_bp, R_ECX);  // ecx = vtbl
		xasm.movMR(R_ECX, offset_cf_in_vtbl, R_EAX);              // eax = next cf
		xasm.pushR(R_EAX);                                        // push next cf
		xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);  // ecx = code
		
		xasm.callAbsR(R_ECX);
		//xasm.nop();// Don't let the call be the last instruction for the bytecode

		generateAfterCall(desc, true);
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    // INVOKEVIRTUAL_QUICK
	    generateArgumentRepushing(desc);
	    xasm.popR(R_EAX); // RAX = receiver
	    xasm.pushR(R_EAX);
	    generateNullCheck(R_EAX);
	    xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));   // edx = next bp
	    xasm.movMR(R_EDX, precomputed.offset_vtbl_in_bp, R_ECX);  // ecx = vtbl
	    int patch_pc = xasm.movMR_to_be_patched(R_ECX, R_EAX);              // eax = next cf
	    xasm.pushR(R_EAX);                                        // push next cf
	    xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);  // ecx = code

	    xasm.callAbsR(R_ECX);
	    //xasm.nop();// Don't let the call be the last instruction for the bytecode

	    generateAfterCall(desc, true);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    // INVOKEVIRTUAL
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveInstanceMethod_index,
			    precomputed.csa_resolveInstanceMethod_desc);
	    xasm.popR(R_EDX);

	    //
	    xasm.selfModifyJmpWithNops(b0);
	    xasm.movIR(precomputed.eObjectArrayElementSize, R_EAX);
	    xasm.mulRR(R_EDX, R_EAX);
	    xasm.addIR(precomputed.eObjectArrayHeaderSize, R_EAX);
	    xasm.selfModify4Bytes(patch_pc, R_EAX);

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	}

    }

    public void visit(Instruction.INVOKESPECIAL i) {
	
	int cpindex = i.getCPIndex(buf);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
	int arg_length = desc.getArgumentLength() / 4;
	int offset_methodref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cpindex);

	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
	    try {
		ConstantResolvedInstanceMethodref imi =
		    cp.resolveInstanceMethod(cpindex);
		if (imi.isNonVirtual) { // NONVIRTUAL2_QUICK
		    int nvtbl_index = imi.getOffset();
		    int offset_cf_in_nvtbl = getArrayElementOffset(executiveDomain, 
								   OBJECT, 
								   nvtbl_index);
		    generateArgumentRepushing(desc);

		    if (ED_OBJECT_DONT_MOVE) { // fast
			Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
			Branch b2 = xasm.setBranchTarget();

			//... fast path

			int patch_pc_code_obj = xasm.pushI32_to_be_patched();
			int patch_pc_code_ptr = xasm.movIR_to_be_patched(R_ECX);
			xasm.callAbsR(R_ECX);
			//xasm.nop();// Don't let the call be the last instruction for the bytecode

			generateAfterCall(desc, true);

			Branch b1 = xasm.setBranchSourceAndJmp();
			xasm.setBranchTarget(b0);

			//... slow path

			xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // eax = code
			xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
			xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
			xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
			xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_ECX); // ecx = target bp
			xasm.movMR(R_ECX, precomputed.offset_nvtbl_in_bp, R_EDX);   // edx = nvtbl
			xasm.movMR(R_EDX, offset_cf_in_nvtbl, R_EAX);               // eax = ncf
			xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);    // ecx = code ptr

			//
			xasm.selfModifyJmpWithNops(b0);
			xasm.selfModify4Bytes(patch_pc_code_obj, R_EAX);
			xasm.selfModify4Bytes(patch_pc_code_ptr, R_ECX);
			xasm.setBranchSourceAndJmp(b2);
			xasm.setBranchTarget(b1);
		    } else { // slow
			xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // eax = code
			xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
			xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
			xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
			xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_ECX); // ecx = target bp

			xasm.movMR(R_ECX, precomputed.offset_nvtbl_in_bp, R_EDX);   // edx = nvtbl
			xasm.movMR(R_EDX, offset_cf_in_nvtbl, R_EAX);               // eax = ncf
			xasm.pushR(R_EAX);                                          // push next ncf
			xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);    // ecx = code

			xasm.callAbsR(R_ECX);
			//xasm.nop();// Don't let the call be the last instruction for the bytecode

			generateAfterCall(desc, true);
		    }

		} else { // SUPER_QUICK
		    int vtbl_index = imi.getOffset();
		    int offset_cf_in_vtbl = getArrayElementOffset(executiveDomain, 
								  OBJECT, 
								  vtbl_index);
		    generateArgumentRepushing(desc);


		    if (ED_OBJECT_DONT_MOVE) {
			Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to a movIR
			Branch b2 = xasm.setBranchTarget();

			//... fast path
			int patch_pc_code_obj = xasm.pushI32_to_be_patched();
			int patch_pc_code_ptr = xasm.movIR_to_be_patched(R_ECX);
			xasm.callAbsR(R_ECX);
			//xasm.nop();// Don't let the call be the last instruction for the bytecode
	    
			generateAfterCall(desc, true);

			Branch b1 = xasm.setBranchSourceAndJmp();
			xasm.setBranchTarget(b0);
	    
			//... slow path
	    
			xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // edx = code
			xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // eax = cp
			xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // ecx = cpvalues
			xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
			xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_ECX); // ecx = target bp

			xasm.movMR(R_ECX, precomputed.offset_vtbl_in_bp, R_EDX);          // edx = vtbl
			xasm.movMR(R_EDX, offset_cf_in_vtbl, R_EAX);          // eax = ncf
			xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);          // ecx = code

			//
			xasm.selfModifyJmpWithNops(b0);
			xasm.selfModify4Bytes(patch_pc_code_obj, R_EAX);
			xasm.selfModify4Bytes(patch_pc_code_ptr, R_ECX);
			xasm.setBranchSourceAndJmp(b2);
			xasm.setBranchTarget(b1);

		    } else {
			xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // edx = code
			xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // eax = cp
			xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // ecx = cpvalues
			xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
			xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_ECX); // ecx = target bp

			xasm.movMR(R_ECX, precomputed.offset_vtbl_in_bp, R_EDX);          // edx = vtbl
			xasm.movMR(R_EDX, offset_cf_in_vtbl, R_EAX);          // eax = ncf
			xasm.pushR(R_EAX);                                    // push next ncf
			xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);          // ecx = code

			xasm.callAbsR(R_ECX);
			//xasm.nop();// Don't let the call be the last instruction for the bytecode
	
			generateAfterCall(desc, true);
		    }
		}
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }

	} else { // UD

	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    int patch_pc_table_offset = 0;
	    int patch_pc_code_offset = 0;
	    int patch_pc_code_obj = 0;
	    int patch_pc_code_ptr = 0;

	    generateArgumentRepushing(desc);

	    if (ED_OBJECT_DONT_MOVE) {
		patch_pc_code_obj = xasm.pushI32_to_be_patched();
		patch_pc_code_ptr = xasm.movIR_to_be_patched(R_EDX);
	    } else {
		xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // edx = code
		xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // eax = cp
		xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // ecx = cpvalues
		xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
		xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_EDX); // edx = target bp
		patch_pc_table_offset = xasm.movMR_to_be_patched(R_EDX, R_EAX); // eax = table
		patch_pc_code_offset = xasm.movMR_to_be_patched(R_EAX, R_ECX); // ecx = code
		xasm.pushR(R_ECX);                                    // push next ncf
		xasm.movMR(R_ECX, precomputed.offset_code_in_cf, R_EDX); // edx = code
	    }

	    xasm.callAbsR(R_EDX);
	    //xasm.nop();// Don't let the call be the last instruction for the bytecode
	
	    generateAfterCall(desc, true);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    // INVOKESPECIAL
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveInstanceMethod_index,
			    precomputed.csa_resolveInstanceMethod_desc);
	    xasm.popR(R_EDX);

	    //
	    xasm.selfModifyJmpWithNops(b0);

	    if (ED_OBJECT_DONT_MOVE) {
		xasm.movIR(precomputed.eObjectArrayElementSize, R_EBX);
		xasm.mulRR(R_EDX, R_EBX);
		xasm.addIR(precomputed.eObjectArrayHeaderSize, R_EBX); // ebx = code offset in table

		xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // eax = code
		xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
		xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
		xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
		xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_EDX); // edx = target bp
		xasm.movMR(R_ECX, precomputed.offset_nonvirtual_in_instancemref, R_EAX); // eax = special
	    
		xasm.cmpRI(R_EAX, 0);
		Branch bnv2orsp = xasm.setBranchSourceAndJcc(J_E);
		// NONVIRTUAL2_QUICK
		xasm.movMR(R_EDX, precomputed.offset_nvtbl_in_bp, R_ECX); // ecx = nvtbl
		xasm.addRR(R_EBX, R_ECX);
		xasm.movMR(R_ECX, 0, R_EAX); // eax = code
		xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_EDX); // edx = code
		xasm.selfModify4Bytes(patch_pc_code_obj, R_EAX);
		xasm.selfModify4Bytes(patch_pc_code_ptr, R_EDX);
		Branch bnv2orsp2 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(bnv2orsp);
		// SUPER_QUICK
		xasm.movMR(R_EDX, precomputed.offset_vtbl_in_bp, R_ECX); // ecx = nvtbl
		xasm.addRR(R_EBX, R_ECX);
		xasm.movMR(R_ECX, 0, R_EAX); // eax = code
		xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_EDX); // edx = code
		xasm.selfModify4Bytes(patch_pc_code_obj, R_EAX);
		xasm.selfModify4Bytes(patch_pc_code_ptr, R_EDX);
		xasm.setBranchTarget(bnv2orsp2);
	    } else {
		xasm.movIR(precomputed.eObjectArrayElementSize, R_EAX);
		xasm.mulRR(R_EDX, R_EAX);
		xasm.addIR(precomputed.eObjectArrayHeaderSize, R_EAX);
		xasm.selfModify4Bytes(patch_pc_code_offset, R_EAX);

		xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // eax = code
		xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
		xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
		xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
		xasm.movMR(R_ECX, precomputed.offset_nonvirtual_in_instancemref, R_EAX); // eax = special
	    
		xasm.cmpRI(R_EAX, 0);
		Branch bnv2orsp = xasm.setBranchSourceAndJcc(J_E);
		// NONVIRTUAL2_QUICK
		xasm.selfModify4Bytes(patch_pc_table_offset, precomputed.offset_nvtbl_in_bp);
		Branch bnv2orsp2 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(bnv2orsp);
		// SUPER_QUICK
		xasm.selfModify4Bytes(patch_pc_table_offset, precomputed.offset_vtbl_in_bp);
		xasm.setBranchTarget(bnv2orsp2);
	    }

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	}
    }

    public void visit(Instruction.INVOKESTATIC i) {
	
	int cpindex = i.getCPIndex(buf);
	int offset_methodref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cpindex);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
	int arg_length = desc.getArgumentLength() / 4;

	if (precomputed.isExecutive) { // ED
            try {
		ConstantResolvedStaticMethodref ifi =
		    cp.resolveStaticMethod(cpindex);
		int nvtbl_index = ifi.getOffset();
		int offset_cf_in_nvtbl = getArrayElementOffset(executiveDomain, 
							       OBJECT, 
							       nvtbl_index);
		generateArgumentRepushingNoReceiver(desc);

		if (ED_OBJECT_DONT_MOVE) {
		    { // loading the shst
			Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to nops
			Branch b2 = xasm.setBranchTarget();

			//... fast path
			int patch_pc = xasm.pushI32_to_be_patched();
			Branch b1 = xasm.setBranchSourceAndJmp();
			xasm.setBranchTarget(b0);

			//... slow path
			xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
			xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
			xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
			xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);
			xasm.movMR(R_ECX, precomputed.offset_shst_in_staticmref, R_EAX); // EAX = shst

			//
			xasm.selfModifyJmpWithNops(b0);
			xasm.selfModify4Bytes(patch_pc, R_EAX);
			xasm.setBranchSourceAndJmp(b2);
			xasm.setBranchTarget(b1);
		    }
		    
		    { // loading the code
			Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to nops
			Branch b2 = xasm.setBranchTarget();

			//... fast path
			int patch_pc_code_obj = xasm.pushI32_to_be_patched();
			// FIXME: why can't we call through (esp), or
			// call immediate?
			int patch_pc_code_ptr = xasm.movIR_to_be_patched(R_ECX);

			xasm.callAbsR(R_ECX);
			//xasm.nop();// Don't let the call be the last instruction for the bytecode

			generateAfterCall(desc, false);

			Branch b1 = xasm.setBranchSourceAndJmp();
			xasm.setBranchTarget(b0);

			//... slow path
			xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));   // edx = next bp
			xasm.movMR(R_EDX, precomputed.offset_nvtbl_in_bp, R_ECX);  // ecx = vtbl
			xasm.movMR(R_ECX, offset_cf_in_nvtbl, R_EAX);              // eax = next cf
			xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);  // ecx = code

			//
			xasm.selfModifyJmpWithNops(b0);
			xasm.selfModify4Bytes(patch_pc_code_obj, R_EAX);
			xasm.selfModify4Bytes(patch_pc_code_ptr, R_ECX);
			xasm.setBranchSourceAndJmp(b2);
			xasm.setBranchTarget(b1);
		    }
		} else {
		    xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		    xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		    xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);
		    xasm.movMR(R_ECX, precomputed.offset_shst_in_staticmref, R_EAX); // EAX = shst
		    xasm.pushR(R_EAX);

		    xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));   // edx = next bp
		    xasm.movMR(R_EDX, precomputed.offset_nvtbl_in_bp, R_ECX);  // ecx = vtbl
		    xasm.movMR(R_ECX, offset_cf_in_nvtbl, R_EAX);              // eax = next cf
		    xasm.pushR(R_EAX);                                        // push next cf
		    xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);  // ecx = code
		    xasm.callAbsR(R_ECX);
		    //xasm.nop();// Don't let the call be the last instruction for the bytecode
		    
		    generateAfterCall(desc, false);
		}

	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else { // UD
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    int patch_pc = 0;

	    generateArgumentRepushingNoReceiver(desc);

	    if (ED_OBJECT_DONT_MOVE) {
		Branch br0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to nops
		Branch br2 = xasm.setBranchTarget();

		//... fast path
		int _patch_pc = xasm.movIR_to_be_patched(R_EAX);
		xasm.pushR(R_EAX);

		Branch br1 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(br0);

		//... slow path
		xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_shst_in_staticmref, R_EAX); // EAX = shst

		//
		xasm.selfModifyJmpWithNops(br0);
		xasm.selfModify4Bytes(_patch_pc, R_EAX);
		xasm.setBranchSourceAndJmp(br2);
		xasm.setBranchTarget(br1);
	    } else {
		xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);
		xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX);
		xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);
		xasm.movMR(R_ECX, precomputed.offset_shst_in_staticmref, R_EAX); // EAX = shst
		xasm.pushR(R_EAX);
	    }

	    xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));   // edx = next bp
	    xasm.movMR(R_EDX, precomputed.offset_nvtbl_in_bp, R_ECX);  // ecx = nvtbl
	    patch_pc = xasm.movMR_to_be_patched(R_ECX, R_EAX);              // eax = next cf
	    xasm.pushR(R_EAX);                                        // push next cf
	    xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);  // ecx = code

	    xasm.callAbsR(R_ECX);
	    //xasm.nop();// Don't let the call be the last instruction for the bytecode

	    generateAfterCall(desc, false);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveStaticMethod_index,
			    precomputed.csa_resolveStaticMethod_desc);
	    xasm.popR(R_EDX);

	    //
	    xasm.selfModifyJmpWithNops(b0);
	    xasm.movIR(precomputed.eObjectArrayElementSize, R_EAX);
	    xasm.mulRR(R_EDX, R_EAX);
	    xasm.addIR(precomputed.eObjectArrayHeaderSize, R_EAX);
	    xasm.selfModify4Bytes(patch_pc, R_EAX);

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	}
    }

    public void visit(Instruction.INVOKEINTERFACE i) {
	
	int cpindex = i.getCPIndex(buf);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
	int arg_length = desc.getArgumentLength() / 4;

	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
            try {
		ConstantResolvedInterfaceMethodref ifi =
		    cp.resolveInterfaceMethod(cpindex);
		int iftbl_index = ifi.getOffset();
		int offset_cf_in_iftbl = getArrayElementOffset(executiveDomain, 
							       OBJECT, 
							       iftbl_index);
		generateArgumentRepushing(desc);
		xasm.popR(R_EAX); // RAX = receiver
		xasm.pushR(R_EAX);
		generateNullCheck(R_EAX);
		xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));   // edx = next bp
		xasm.movMR(R_EDX, precomputed.offset_iftbl_in_bp, R_ECX);  // ecx = iftbl
		xasm.movMR(R_ECX, offset_cf_in_iftbl, R_EAX);              // eax = next cf
		xasm.pushR(R_EAX);                                        // push next cf
		xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);  // ecx = code
		
		xasm.callAbsR(R_ECX);
		//xasm.nop();// Don't let the call be the last instruction for the bytecode

		generateAfterCall(desc, true);
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    int patch_pc = 0;

	    // INVOKEINTERFACE_QUICK
	    generateArgumentRepushing(desc);
	    xasm.popR(R_EAX); // RAX = receiver
	    xasm.pushR(R_EAX);
	    generateNullCheck(R_EAX);
	    xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));   // edx = next bp
	    xasm.movMR(R_EDX, precomputed.offset_iftbl_in_bp, R_ECX);  // ecx = iftbl
	    patch_pc = xasm.movMR_to_be_patched(R_ECX, R_EAX);              // eax = next cf
	    xasm.pushR(R_EAX);                                        // push next cf
	    xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);  // ecx = code

	    xasm.callAbsR(R_ECX);
	    //xasm.nop();// Don't let the call be the last instruction for the bytecode

	    generateAfterCall(desc, true);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    // INVOKEINTERFACE
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveInterfaceMethod_index,
			    precomputed.csa_resolveInterfaceMethod_desc);
	    xasm.popR(R_EDX);

	    //
	    xasm.selfModifyJmpWithNops(b0);
	    xasm.movIR(precomputed.eObjectArrayElementSize, R_EAX);
	    xasm.mulRR(R_EDX, R_EAX);
	    xasm.addIR(precomputed.eObjectArrayHeaderSize, R_EAX);
	    xasm.selfModify4Bytes(patch_pc, R_EAX);

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	}
    }

    public void visit(Instruction.INVOKEVIRTUAL_QUICK i) {
	
	int vtbl_index = i.getMethodTableIndex(buf);
	int arg_length = i.getArgumentLengthInWords(buf);
	int offset_cf_in_vtbl = getArrayElementOffset(executiveDomain, 
						      OBJECT, 
						      vtbl_index);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();

	generateArgumentRepushing(desc);

	xasm.popR(R_EAX); // RAX = receiver
	xasm.pushR(R_EAX);
	generateNullCheck(R_EAX);

	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));   // edx = next bp
	xasm.movMR(R_EDX, precomputed.offset_vtbl_in_bp, R_ECX);  // ecx = vtbl
	xasm.movMR(R_ECX, offset_cf_in_vtbl, R_EAX);              // eax = next cf
	xasm.pushR(R_EAX);                                        // push next cf
	xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);  // ecx = code

	xasm.callAbsR(R_ECX);
	//xasm.nop();// Don't let the call be the last instruction for the bytecode

	generateAfterCall(desc, true);
    }

    public void visit(Instruction.INVOKENONVIRTUAL_QUICK i) {
	
	if (ED_OBJECT_DONT_MOVE)
	    visitINVOKENONVIRTUAL_QUICK_Fast(i);
	else
	    visitINVOKENONVIRTUAL_QUICK_Slow(i);
    }

    public void visitINVOKENONVIRTUAL_QUICK_Fast
	(Instruction.INVOKENONVIRTUAL_QUICK i) {
	int nvtbl_index = i.getMethodTableIndex(buf);
	int arg_length = i.getArgumentLengthInWords(buf);
	int offset_cf_in_nvtbl = getArrayElementOffset(executiveDomain, 
						       OBJECT, 
						       nvtbl_index);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();

	generateArgumentRepushing(desc);

	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	Branch b2 = xasm.setBranchTarget();

	//... fast path

	int patch_pc_code_obj = xasm.pushI32_to_be_patched();
	int patch_pc_code_ptr = xasm.movIR_to_be_patched(R_ECX);
	xasm.callAbsR(R_ECX);
	//xasm.nop();// Don't let the call be the last instruction for the bytecode
	
	generateAfterCall(desc, true);

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path

	xasm.popR(R_EAX);
	xasm.pushR(R_EAX);
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));    // edx = next bp
	xasm.movMR(R_EDX, precomputed.offset_nvtbl_in_bp, R_ECX);  // ecx = nvtbl
	xasm.movMR(R_ECX, offset_cf_in_nvtbl, R_EAX);              // eax = next cf
	xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);   // ecx = code

	//
	xasm.selfModifyJmpWithNops(b0);
	xasm.selfModify4Bytes(patch_pc_code_obj, R_EAX);
	xasm.selfModify4Bytes(patch_pc_code_ptr, R_ECX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);
    }

    public void visitINVOKENONVIRTUAL_QUICK_Slow
	(Instruction.INVOKENONVIRTUAL_QUICK i) {
	int nvtbl_index = i.getMethodTableIndex(buf);
	int arg_length = i.getArgumentLengthInWords(buf);
	int offset_cf_in_nvtbl = getArrayElementOffset(executiveDomain, 
						       OBJECT, 
						       nvtbl_index);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();

	generateArgumentRepushing(desc);

	xasm.popR(R_EAX);
	xasm.pushR(R_EAX);
	//	generateNullCheck(R_EAX);

	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX));    // edx = next bp
	xasm.movMR(R_EDX, precomputed.offset_nvtbl_in_bp, R_ECX);  // ecx = nvtbl
	xasm.movMR(R_ECX, offset_cf_in_nvtbl, R_EAX);              // eax = next cf
	xasm.pushR(R_EAX);                                         // push next cf
	xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);   // ecx = code

	xasm.callAbsR(R_ECX);
	//xasm.nop();// Don't let the call be the last instruction for the bytecode

	generateAfterCall(desc, true);
    }

    public void visit(Instruction.INVOKENONVIRTUAL2_QUICK i) {
	
	if (ED_OBJECT_DONT_MOVE)
	    visitINVOKENONVIRTUAL2_QUICK_Fast(i);
	else
	    visitINVOKENONVIRTUAL2_QUICK_Slow(i);
    }

    public void visitINVOKENONVIRTUAL2_QUICK_Fast
	(Instruction.INVOKENONVIRTUAL2_QUICK i) {
	int cp_index = i.getCPIndex(buf);
	int nvtbl_index = i.getMethodTableIndex(buf);
	int arg_length = i.getArgumentLengthInWords(buf);
	int offset_methodref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);
	int offset_cf_in_nvtbl = getArrayElementOffset(executiveDomain, 
						       OBJECT, 
						       nvtbl_index);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();

	generateArgumentRepushing(desc);

	Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to a movIR
	Branch b2 = xasm.setBranchTarget();

	//... fast path

	xasm.pushR(R_EAX);                                          // push next ncf
	xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);    // ecx = code
	xasm.callAbsR(R_ECX);
	//xasm.nop();// Don't let the call be the last instruction for the bytecode

	generateAfterCall(desc, true);

	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);

	//... slow path

	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // eax = code
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
	xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
	xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_ECX); // ecx = target bp
	xasm.movMR(R_ECX, precomputed.offset_nvtbl_in_bp, R_EDX);   // edx = nvtbl
	xasm.movMR(R_EDX, offset_cf_in_nvtbl, R_EAX);               // eax = ncf

	//
	xasm.selfModifyJmpWithMovIR(b0, R_EAX);
	xasm.setBranchSourceAndJmp(b2);
	xasm.setBranchTarget(b1);
    }

    public void visitINVOKENONVIRTUAL2_QUICK_Slow
	(Instruction.INVOKENONVIRTUAL2_QUICK i) {
	int cp_index = i.getCPIndex(buf);
	int nvtbl_index = i.getMethodTableIndex(buf);
	int arg_length = i.getArgumentLengthInWords(buf);
	int offset_methodref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);
	int offset_cf_in_nvtbl = getArrayElementOffset(executiveDomain, 
						       OBJECT, 
						       nvtbl_index);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();

	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // eax = code
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
	xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
	xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_ECX); // ecx = target bp

	generateArgumentRepushing(desc);

	xasm.movMR(R_ECX, precomputed.offset_nvtbl_in_bp, R_EDX);   // edx = nvtbl
	xasm.movMR(R_EDX, offset_cf_in_nvtbl, R_EAX);               // eax = ncf
	xasm.pushR(R_EAX);                                          // push next ncf
	xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);    // ecx = code

	xasm.callAbsR(R_ECX);
	//xasm.nop();// Don't let the call be the last instruction for the bytecode

	generateAfterCall(desc, true);
    }

    public void visit(Instruction.INVOKESUPER_QUICK i) {
	
	int cp_index = i.getCPIndex(buf);
	int vtbl_index = i.getMethodTableIndex(buf);
	int arg_length = i.getArgumentLengthInWords(buf);
	int offset_methodref_in_cpvalues = 
	    getArrayElementOffset(executiveDomain, 
				  OBJECT, 
				  cp_index);
	int offset_cf_in_vtbl = getArrayElementOffset(executiveDomain, 
						      OBJECT, 
						      vtbl_index);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();

	generateArgumentRepushing(desc);

	if (ED_OBJECT_DONT_MOVE) {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be self-modified to a movIR
	    Branch b2 = xasm.setBranchTarget();

	    //... fast path
	    int patch_pc_code_obj = xasm.pushI32_to_be_patched();
	    int patch_pc_code_ptr = xasm.movIR_to_be_patched(R_ECX);
	    xasm.callAbsR(R_ECX);
	    //xasm.nop();// Don't let the call be the last instruction for the bytecode
	    
	    generateAfterCall(desc, true);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);
	    
	    //... slow path
	    
	    xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // edx = code
	    xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // eax = cp
	    xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // ecx = cpvalues
	    xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
	    xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_ECX); // ecx = target bp

	    xasm.movMR(R_ECX, precomputed.offset_vtbl_in_bp, R_EDX);          // edx = vtbl
	    xasm.movMR(R_EDX, offset_cf_in_vtbl, R_EAX);          // eax = ncf
	    xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);          // ecx = code

	    //
	    xasm.selfModifyJmpWithNops(b0);
	    xasm.selfModify4Bytes(patch_pc_code_obj, R_EAX);
	    xasm.selfModify4Bytes(patch_pc_code_ptr, R_ECX);
	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);

	} else {
	    xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // edx = code
	    xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // eax = cp
	    xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // ecx = cpvalues
	    xasm.movMR(R_EDX, offset_methodref_in_cpvalues, R_ECX);    // ecx = target methodref
	    xasm.movMR(R_ECX, precomputed.offset_bp_in_instancemref, R_ECX); // ecx = target bp

	    xasm.movMR(R_ECX, precomputed.offset_vtbl_in_bp, R_EDX);          // edx = vtbl
	    xasm.movMR(R_EDX, offset_cf_in_vtbl, R_EAX);          // eax = ncf
	    xasm.pushR(R_EAX);                                    // push next ncf
	    xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);          // ecx = code

	    xasm.callAbsR(R_ECX);
	    //xasm.nop();// Don't let the call be the last instruction for the bytecode
	
	    generateAfterCall(desc, true);
	}
    }

    public void visit(Instruction.INVOKEINTERFACE_QUICK i) {
	
	int iftbl_index = i.getMethodTableIndex(buf);
	int arg_length = i.getArgumentLengthInWords(buf);
	int offset_cf_in_iftbl = getArrayElementOffset(executiveDomain, 
						       OBJECT, 
						       iftbl_index);
	Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();

	generateArgumentRepushing(desc);

	xasm.popR(R_EAX); // EAX = receiver
	xasm.pushR(R_EAX);
	generateNullCheck(R_EAX);
	
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EAX, R_EDX)); // edx = next bp
	//xasm.movMR(R_EAX, precomputed.offset_blueprint_in_header, R_EDX); // edx = next bp
	xasm.movMR(R_EDX, precomputed.offset_iftbl_in_bp, R_ECX);  // ecx = vtbl
	xasm.movMR(R_ECX, offset_cf_in_iftbl, R_EAX);  // eax = next cf
	xasm.pushR(R_EAX);                             // push next cf
	xasm.movMR(R_EAX, precomputed.offset_code_in_cf, R_ECX);  // ecx = code

	xasm.callAbsR(R_ECX);
	//xasm.nop();// Don't let the call be the last instruction for the bytecode

	generateAfterCall(desc, true);
    }

    public void visit(Instruction.ROLL i) {
	
	int span = i.getSpan(buf);
	int count = i.getCount(buf);
	if (count > 0) {
	    xasm.movRR(R_ESP, R_EDX);
	    xasm.addIR(span * 4, R_EDX);
	    // We use count-many extra stack slots.  Make sure that
	    // they are not trashed by signal handlers
	    xasm.subIR(count * 4, R_ESP);
	    for(int k = span ; k > 0; k--) {
		xasm.movMR(R_EDX, - (k * 4), R_ECX);
		xasm.movRM(R_ECX, R_EDX, - ((k + count) * 4));
	    }
	    for(int k = count; k > 0; k--) {
		xasm.movMR(R_EDX, - ((span + k) * 4), R_ECX);
		xasm.movRM(R_ECX, R_EDX, - (k * 4));
	    }
	    xasm.addIR(count * 4, R_ESP);
	} else if (count < 0) { 
	    int pcount = - count; // Note count is negative
	    xasm.movRR(R_ESP, R_EDX); 
	    xasm.movRR(R_EDX, R_ECX);
	    xasm.subIR(pcount * 4, R_ESP);
	    xasm.subIR(pcount * 4, R_EDX); // edx : dest base
	    xasm.addIR((span - pcount) * 4, R_ECX);  // ecx : src base
	    for(int k = 0 ; k < pcount; k++) {
		xasm.movMR(R_ECX, k * 4, R_EAX);
		xasm.movRM(R_EAX, R_EDX, k * 4);
	    }
	    xasm.subIR((span - pcount) * 4, R_ECX);
	    for(int k = (span - pcount) - 1; k >= 0; k--) {
		xasm.movMR(R_ECX, k * 4, R_EAX);
		xasm.movRM(R_EAX, R_ECX, (k + pcount) * 4);
	    }
	    for(int k = pcount - 1; k >= 0; k--) {
		xasm.movMR(R_EDX, k * 4, R_EAX);
		xasm.movRM(R_EAX, R_EDX, (pcount + k) * 4);
	    }
	    xasm.addIR(pcount * 4, R_ESP);
	}
    }

    public void visit(Instruction.MONITORENTER i) {
	
	xasm.popR(R_EDX);

	generateNullCheckForMonitor(R_EDX);

	xasm.pushR(R_EDX);
	generateCSACall(precomputed.csa_monitorEnter_index, 
			precomputed.csa_monitorEnter_desc);
    }

    public void visit(Instruction.MONITOREXIT i) {
	
	xasm.popR(R_EDX);

	generateNullCheckForMonitor(R_EDX);

	xasm.pushR(R_EDX);
	generateCSACall(precomputed.csa_monitorExit_index, 
			precomputed.csa_monitorExit_desc);
    }

    public void visit(Instruction.NEW i) {
	

	generateMemDebug();

	int cpindex = i.getCPIndex(buf);

	if (precomputed.isExecutive) {
	    try {
		cp.resolveClassAt(cpindex);
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp();

	    // NEW
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveNew_index, 
			    precomputed.csa_resolveNew_desc);
	    xasm.setSelfModifyingBranchTarget(b0);
	}
	    
	// NEW_QUICK 
	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
							  OBJECT, 
							  cpindex);

	if (ED_OBJECT_DONT_MOVE) {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    int patch_pc_bp = xasm.pushI32_to_be_patched();
	    generateCSACall(precomputed.csa_allocateObject_index, 
			    precomputed.csa_allocateObject_desc);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    xasm.movMR(R_EBP, offset_code_in_stack, R_EDX);            // edx = code
	    xasm.movMR(R_EDX, precomputed.offset_cp_in_cf, R_EAX);     // eax = cp
	    xasm.movMR(R_EAX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	    xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EDX);           // edx = param bp

	    xasm.selfModifyJmpWithNops(b0);
	    xasm.selfModify4Bytes(patch_pc_bp, R_EDX);

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	} else {
	    xasm.movMR(R_EBP, offset_code_in_stack, R_EDX);            // edx = code
	    xasm.movMR(R_EDX, precomputed.offset_cp_in_cf, R_EAX);     // eax = cp
	    xasm.movMR(R_EAX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	    xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EDX);           // edx = param bp
	
	    xasm.pushR(R_EDX);                                          // push param bp
	    generateCSACall(precomputed.csa_allocateObject_index, 
			    precomputed.csa_allocateObject_desc);
	}
    }

    public void visit(Instruction.NEW_QUICK i) {
	
	generateMemDebug();

	int cp_index = i.getCPIndex(buf);
	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
							  OBJECT, 
							  cp_index);

	xasm.movMR(R_EBP, offset_code_in_stack, R_EDX);            // edx = code
	xasm.movMR(R_EDX, precomputed.offset_cp_in_cf, R_EAX);     // eax = cp
	xasm.movMR(R_EAX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EDX);           // edx = param bp

	xasm.pushR(R_EDX);                                          // push param bp
	generateCSACall(precomputed.csa_allocateObject_index, 
			precomputed.csa_allocateObject_desc);
    }

    public void visit(Instruction.SINGLEANEWARRAY i) {
	
	generateMemDebug();

	int cpindex = i.getCPIndex(buf);

	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
	    try {
		cp.resolveClassAt(cpindex);
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {

	    // SINGLEANEWARRAY
	    Branch b0 = xasm.setBranchSourceAndJmp();
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveClass_index, 
			    precomputed.csa_resolveClass_desc);
	    xasm.setSelfModifyingBranchTarget(b0);
	}

	// ANEWARRAY_QUICK
	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, 
							  OBJECT, 
							  cpindex);
	if (ED_OBJECT_DONT_MOVE) {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    int patch_pc_bp = xasm.pushI32_to_be_patched();
	    generateCSACall(precomputed.csa_allocateArray_index, 
			    precomputed.csa_allocateArray_desc);
	    
	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // eax = code
	    xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
	    xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
	    xasm.movMR(R_EDX, offset_bp_in_cpvalues, R_ECX);       // ecx = param bp

	    xasm.selfModifyJmpWithNops(b0);
	    xasm.selfModify4Bytes(patch_pc_bp, R_ECX);

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	} else {

	    xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // eax = code
	    xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
	    xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
	    xasm.movMR(R_EDX, offset_bp_in_cpvalues, R_ECX);       // ecx = param bp

	    xasm.pushR(R_ECX);                                    // push param bp
	    generateCSACall(precomputed.csa_allocateArray_index, 
			    precomputed.csa_allocateArray_desc);
	}
    }

    public void visit(Instruction.ANEWARRAY_QUICK i) {
	
	generateMemDebug();

	int cp_index = i.getCPIndex(buf);

	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, 
							  OBJECT, 
							  cp_index);
	xasm.popR(R_EBX);                                        // ebx = array length

	xasm.movMR(R_EBP, offset_code_in_stack, R_EAX);        // eax = code
	xasm.movMR(R_EAX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
	xasm.movMR(R_EDX, offset_bp_in_cpvalues, R_ECX);       // ecx = param bp

	xasm.pushR(R_EBX);                                    // push param array length
	xasm.pushR(R_ECX);                                    // push param bp
	generateCSACall(precomputed.csa_allocateArray_index, 
			precomputed.csa_allocateArray_desc);
    }

    public void visit(Instruction.MULTIANEWARRAY i) {
	
	generateMemDebug();

	int cpindex = i.getCPIndex(buf);


	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
	    try {
		cp.resolveClassAt(cpindex);
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch br0 = xasm.setBranchSourceAndJmp();

	    // MULTIANEWARRAY
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveClass_index, 
			    precomputed.csa_resolveClass_desc);
	    xasm.setSelfModifyingBranchTarget(br0);
	}

	// MULTIANEWARRAY_QUICK
	int dimensions = i.getDimensions(buf);
	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, 
							  OBJECT, 
							  cpindex);
	int arrayLengthFieldOffset = precomputed.eArrayLengthFieldOffset;

	xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);        // ecx = code
	xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EAX); // eax = cp
	xasm.movMR(R_EAX, precomputed.offset_values_in_cp, R_EDX); // edx = cpvalues
	xasm.movMR(R_EDX, offset_bp_in_cpvalues, R_ECX);       // ecx = param bp

	xasm.movRR(R_ESP, R_EDX);
	xasm.subIR(-4 * (dimensions - 1), R_EDX); // R_EDX = dimensionArray

	xasm.pushI32(dimensions);
	xasm.pushR(R_EDX);
	xasm.pushR(R_ECX);

	generateCSACall(precomputed.csa_allocateMultiArray_index, 
			precomputed.csa_allocateMultiArray_desc);

	// deallocate parameters on stack
	xasm.popR(R_ECX);                  // ecx = allocated array ref (retvalue)
	xasm.addIR(4 * dimensions, R_ESP); 
	xasm.pushR(R_ECX);
    }

    public void visit(Instruction.MULTIANEWARRAY_QUICK i) {
	
	generateMemDebug();

	int cp_index = i.getCPIndex(buf);
	int dimensions = i.getDimensions(buf);
	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, 
							  OBJECT, 
							  cp_index);
	int arrayLengthFieldOffset = precomputed.eArrayLengthFieldOffset;

	// allocate an integer array on stack
	xasm.movRR(R_ESP, R_EDX);
	xasm.movIR(0, R_ECX);
	Branch b0 = xasm.setBranchSourceAndJmp();
	Branch b1 = xasm.setBranchTarget();
	xasm.pushMS4(R_EDX, R_ECX);
	xasm.incR(R_ECX);
	xasm.setBranchTarget(b0);
	xasm.cmpRI(R_ECX, dimensions);
	xasm.setBranchSourceAndJcc(b1, J_NE);
	xasm.subIR(precomputed.tIntArrayHeaderSize, R_ESP);
	xasm.movRR(R_ESP, R_EBX);
	xasm.movIM(dimensions, R_EBX, arrayLengthFieldOffset); // ebx = int[] arraylengths

	xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);        // ecx = code
	xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ecx = cp
	xasm.movMR(R_ECX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EDX);       // edx = param bp

	xasm.pushR(R_EBX);                                    // push int[] arraylengths
	xasm.pushI32(0);                                      // push pos
	xasm.pushR(R_EDX);                                    // push param bp

	generateCSACall(precomputed.csa_allocateMultiArray_index, 
			precomputed.csa_allocateMultiArray_desc);

	// deallocate the integer array and parameters on stack
	xasm.popR(R_ECX);                  // ecx = allocated array ref (retvalue)
	xasm.addIR(precomputed.tIntArrayHeaderSize 
		  + 4 * dimensions /* stack-allocated int array */
		  + 4 * dimensions, /* original multianewarray arguments - 1 for ecx */
		  R_ESP); 
	xasm.pushR(R_ECX);
    }

    public void visit(Instruction.INSTANCEOF i) {
	
	int cpindex = i.getCPIndex(buf);

	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
	    try {
		cp.resolveClassAt(cpindex);
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp();

	    // INSTANCEOF
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveClass_index, 
			    precomputed.csa_resolveClass_desc);
	    xasm.setSelfModifyingBranchTarget(b0);
	}
	    
	// INSTANCEOF_QUICK 
	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, 
							  OBJECT, 
							  cpindex);

	if (ED_OBJECT_DONT_MOVE) {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();
	    
	    int patch_pc_bp = xasm.movIR_to_be_patched(R_EAX);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);        // ecx = code
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EDX); // edx = cp
	    xasm.movMR(R_EDX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	    xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EAX);       // eax = provider bp

	    xasm.selfModifyJmpWithNops(b0);
	    xasm.selfModify4Bytes(patch_pc_bp, R_EAX);

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	} else {
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);        // ecx = code
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EDX); // edx = cp
	    xasm.movMR(R_EDX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	    xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EAX);       // eax = provider bp
	}

	xasm.popR(R_EDX);                                      // edx = objectref
	xasm.cmpRI(R_EDX, 0);
	Branch b0 = xasm.setBranchSourceAndJcc(J_E);
	xasm.pushR(R_EAX);                                         // push provider bp
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EDX, R_ECX));    // ecx = client bp
	xasm.pushR(R_ECX);                                         // push client bp
	generateCFunctionCall("is_subtype_of", 2);
	xasm.pushR(R_EAX);                                         // push return value
	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);
	xasm.pushI32(0);
	xasm.setBranchTarget(b1);
    }

    public void visit(Instruction.INSTANCEOF_QUICK i) {
	
	int cp_index = i.getCPIndex(buf);
	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, 
							  OBJECT, 
							  cp_index);
	xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);        // ecx = code
	xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EDX); // edx = cp
	xasm.movMR(R_EDX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EAX);       // eax = provider bp

	xasm.popR(R_EDX);                                      // edx = objectref
	xasm.cmpRI(R_EDX, 0);
	Branch b0 = xasm.setBranchSourceAndJcc(J_E);
	xasm.pushR(R_EAX);                                         // push provider bp
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EDX, R_ECX));    // ecx = client bp
	xasm.pushR(R_ECX);                                         // push client bp
	generateCFunctionCall("is_subtype_of", 2);
	xasm.pushR(R_EAX);                                         // push return value
	Branch b1 = xasm.setBranchSourceAndJmp();
	xasm.setBranchTarget(b0);
	xasm.pushI32(0);
	xasm.setBranchTarget(b1);
    }

    public void visit(Instruction.CHECKCAST i) {
	
	int cpindex = i.getCPIndex(buf);

	if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
	    try {
		cp.resolveClassAt(cpindex);
	    } catch (LinkageException e) {
		warn(i, "Unresolvable in ED : " + e.getMessage());
		generateCFunctionCall("hit_unrewritten_code", 0);
		return;
	    }
	} else {
	    Branch b0 = xasm.setBranchSourceAndJmp();

	    // CHECKCAST
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX); // get code fragment
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_ECX); // ECX = S3Constants
	    xasm.pushR(R_ECX);
	    xasm.pushI32(cpindex);
	    generateCSACall(precomputed.csa_resolveClass_index, 
			    precomputed.csa_resolveClass_desc);
	    xasm.setSelfModifyingBranchTarget(b0);
	}
	
	// CHECKCAST_QUICK
	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, 
							 OBJECT, 
							 cpindex);
	int classCastException = precomputed.classCastExceptionID;

	if (ED_OBJECT_DONT_MOVE) {
	    Branch b0 = xasm.setBranchSourceAndJmp(); // this jmp will be nopped out
	    Branch b2 = xasm.setBranchTarget();

	    int patch_pc_bp = xasm.movIR_to_be_patched(R_EAX);

	    Branch b1 = xasm.setBranchSourceAndJmp();
	    xasm.setBranchTarget(b0);

	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);        // ecx = code
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EDX); // edx = cp
	    xasm.movMR(R_EDX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	    xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EAX);       // eax = provider bp

	    xasm.selfModifyJmpWithNops(b0);
	    xasm.selfModify4Bytes(patch_pc_bp, R_EAX);

	    xasm.setBranchSourceAndJmp(b2);
	    xasm.setBranchTarget(b1);
	} else {
	    xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);        // ecx = code
	    xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EDX); // edx = cp
	    xasm.movMR(R_EDX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	    xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EAX);       // eax = provider bp
	}

	xasm.popR(R_EDX);                                      // edx = objectref
	
	// fix null case
	xasm.cmpRI(R_EDX, 0);
	Branch b1 = xasm.setBranchSourceAndJcc(J_E);

	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);                                         // push provider bp
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EDX, R_ECX)); // ecx = client bp
	xasm.pushR(R_ECX);                                         // push client bp

	generateCFunctionCall("is_subtype_of", 2);
	xasm.cmpRI(R_EAX, 0);                                  // eax = return value
	Branch b0 = xasm.setBranchSourceAndJcc(J_NE); // jump if subtype test was success

	xasm.popR(R_EDX);         // remove the objectref
	xasm.pushI32(0);                                       // push meta
	xasm.pushI32(classCastException);                      // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);

	xasm.setBranchTarget(b1);
	xasm.pushR(R_EDX);
	xasm.setBranchTarget(b0);
    }

    public void visit(Instruction.CHECKCAST_QUICK i) {
	
	int cp_index = i.getCPIndex(buf);
	int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, 
							 OBJECT, 
							 cp_index);
	int classCastException = precomputed.classCastExceptionID;

	xasm.movMR(R_EBP, offset_code_in_stack, R_ECX);        // ecx = code
	xasm.movMR(R_ECX, precomputed.offset_cp_in_cf, R_EDX); // edx = cp
	xasm.movMR(R_EDX, precomputed.offset_values_in_cp, R_ECX); // ecx = cpvalues
	xasm.movMR(R_ECX, offset_bp_in_cpvalues, R_EAX);       // eax = provider bp

	xasm.popR(R_EDX);                                      // edx = objectref
	
	// fix null case
	xasm.cmpRI(R_EDX, 0);
	Branch b1 = xasm.setBranchSourceAndJcc(J_E);

	xasm.pushR(R_EDX);
	xasm.pushR(R_EAX);                                         // push provider bp
	xasm.write(compilerVMInterface.getGetBlueprintCode_X86(R_EDX, R_ECX)); // ecx = client bp
	xasm.pushR(R_ECX);                                         // push client bp

	generateCFunctionCall("is_subtype_of", 2);
	xasm.cmpRI(R_EAX, 0);                                  // eax = return value
	Branch b0 = xasm.setBranchSourceAndJcc(J_NE); // jump if subtype test was success

	xasm.popR(R_EDX);         // remove the objectref
	xasm.pushI32(0);                                       // push meta
	xasm.pushI32(classCastException);                      // push exception type 
	generateCSACall(precomputed.csa_generateThrowable_index, 
			precomputed.csa_generateThrowable_desc);

	xasm.setBranchTarget(b1);
	xasm.pushR(R_EDX);
	xasm.setBranchTarget(b0);
    }


    public void visit(Instruction.INVOKE_NATIVE i) {
	
	int mindex = i.getMethodIndex(buf);
	UnboundSelector.Method[] argList = compilerVMInterface.getNativeMethodList();
	if (mindex < 0 || mindex >= argList.length)
	    throw new OVMError.Internal("Invoke native argument " + 
					Integer.toString(mindex) + 
					" is somehow out of range");
	UnboundSelector.Method m = argList[mindex];
	Descriptor.Method desc = m.getDescriptor();
 	int argWordSize = desc.getArgumentLength() / 4;
	int returnWordSize = desc.returnValueWordSize();
	TypeName returnTypeName = desc.getType();
	char returnTypeTag = returnTypeName.getTypeTag();

	// Inline key functions
	if (INLINE_SOME_IN_INVOKENATIVE) { 
	    if (m.toString().equals("eventsSetEnabled:(Z)V")) {
		int eventUnionIndex =
		    compilerVMInterface
		    .getRuntimeFunctionIndex("eventUnion");
		if (eventUnionIndex == -1)
		    throw new OVMError();
		/* hand inlining of eventsSetEnabled */
		xasm.movIR(precomputed.
			  runtimeFunctionTableHandle.
			  asInt(), R_EAX);
		xasm.movMR(R_EAX, 0, R_ECX);
		xasm.movMR(R_ECX, eventUnionIndex * 4, R_EDX);
		xasm.movMR(R_EDX, 0, R_ECX); // load old eventUion
		xasm.andIR(0xFFFF, R_ECX);   // extract notSignaled
		xasm.popR(R_EAX);            // pop new notEnalbed
		xasm.cmpRI(R_EAX, 0);        // compute !notEnabled
		Branch b0 = xasm.setBranchSourceAndJcc(J_E);
		xasm.xorRR(R_EAX, R_EAX); // zero R_EAX
		Branch b1 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(b0);
		xasm.movIR(0x00010000, R_EAX);
		xasm.setBranchTarget(b1);
		xasm.orRR(R_EAX, R_ECX);     // new eventUnion
		xasm.movRM(R_ECX, R_EDX, 0); // store new eventUnion
		return;
	    }
	}

	xasm.pushI32(mindex);
	generateCFunctionCall("invoke_native", 1 + argWordSize);
	switch(returnWordSize) {
	case 0:
	    break;
	case 1:
	    if (false && returnTypeTag == 'F') {
		xasm.movRR(R_ESP, R_EDX);
		xasm.fstpM32(R_EDX, -4);
		xasm.subIR(4, R_ESP);
	    } else {
		xasm.pushR(R_EAX);
	    }
	    break;
	case 2:
	    if (true || returnTypeTag == 'J') {
		xasm.pushR(R_EDX);
		xasm.pushR(R_EAX);
	    } else if (returnTypeTag == 'D') {
		xasm.movRR(R_ESP, R_EDX);
		xasm.fstpM64(R_EDX, -8);
		xasm.subIR(8, R_ESP);
	    }
	    break;
	default:
	    throw new OVMError.Internal
		("The word size of return value of INVOKE_NATIVE " + 
		 returnWordSize + " not supported");
	}
    }

    public void visit(Instruction.INVOKE_SYSTEM i) {
	
	int mindex = (0xFF & (0x100 + i.getMethodIndex(buf)));
	int optype = i.getOpType(buf);
	switch(mindex) {
	case JVMConstants.InvokeSystemArguments.BREAKPOINT: {
	    xasm.popR(R_EAX);
	    xasm.breakpoint();
	    break;
	}
	case RUN: {
	    xasm.popR(R_EAX); // Reorder arguments
	    xasm.popR(R_EDX);
	    xasm.pushR(R_EAX);
	    xasm.pushR(R_EDX);
	    generateCFunctionCall("SYSTEM_RUN", 2);
	    break;
	}
	case EMPTY_CSA_CALL: {
	    generateCSACall(precomputed.csa_emptyCall_index,
			    precomputed.csa_emptyCall_desc);
	    break;
	}
	case GET_CONTEXT: {
	    generateCFunctionCall("SYSTEM_GET_CONTEXT", 1);
	    xasm.pushR(R_EAX);
	    break;
	}
	case NEW_CONTEXT: {
	    generateCFunctionCall("SYSTEM_NEW_CONTEXT", 1);
	    xasm.pushR(R_EAX);
	    break;
	}
	case DESTROY_NATIVE_CONTEXT: {
	    generateCFunctionCall("SYSTEM_DESTROY_NATIVE_CONTEXT", 1);
	    break;
	}
	case GET_ACTIVATION: {
	    generateCFunctionCall("SYSTEM_GET_ACTIVATION", 1);
	    xasm.pushR(R_EAX);
	    break;
	}
	case MAKE_ACTIVATION: {
	    xasm.popR(R_EAX);
	    xasm.popR(R_EDX);
	    xasm.popR(R_ECX);
	    xasm.pushR(R_EAX);
	    xasm.pushR(R_EDX);
	    xasm.pushR(R_ECX);
	    generateCFunctionCall("SYSTEM_MAKE_ACTIVATION", 3);
	    xasm.pushR(R_EAX);
	    break;
	}
	case CUT_TO_ACTIVATION: {
	    xasm.popR(R_EAX);
	    xasm.popR(R_EDX);
	    xasm.pushR(R_EAX);
	    xasm.pushR(R_EDX);
	    generateCFunctionCall("SYSTEM_CUT_TO_ACTIVATION", 2);
	    break;
	}
	case INVOKE: {
	    xasm.popR(R_EAX);  // pop arguments to SYSTEM_INVOKE
	    xasm.popR(R_EDX);

	    alignForCall(8);
	    xasm.pushR(R_EAX); // reorder and push arguments to SYSTEM_INVOKE
	    xasm.pushR(R_EDX);

	    if (ALIGN_CALLS) {
		generateAlignedFunctionCall("SYSTEM_INVOKE");
		// We've consumed our two JVM operands, and restored
		// %esp from %esi.  All done.
	    } else {
		generateCFunctionCall("SYSTEM_INVOKE", 0 /* no argument popping here */);

		xasm.popR(R_ECX); // ECX = SimpleJITCode
		xasm.movMR(R_ECX, 
			   precomputed.offset_argumentLength_in_SimpleJITCode, 
			   R_ECX); // ECX = argumentLength

		xasm.addRR(R_ECX, R_ESP); // pop arguments to the
					  // reflectively-called
					  // method
	    }

	    switch(optype) {
	    case TypeCodes.VOID:
		break;
	    case TypeCodes.INT:
	    case TypeCodes.SHORT:
	    case TypeCodes.CHAR:
	    case TypeCodes.BYTE:
	    case TypeCodes.BOOLEAN:
	    case TypeCodes.OBJECT:
		xasm.pushR(R_EAX);
		break;
	    case TypeCodes.LONG:
		xasm.pushR(R_EDX);
		xasm.pushR(R_EAX);
		break;
	    case TypeCodes.DOUBLE:
		xasm.movRR(R_ESP, R_EDX);
		xasm.fstpM64(R_EDX, -8);
		xasm.subIR(8, R_ESP);
		break;
	    case TypeCodes.FLOAT:
		xasm.movRR(R_ESP, R_EDX);
		xasm.fstpM32(R_EDX, -4);
		xasm.subIR(4, R_ESP);
		break;
	    default:
		throw new OVMError();
	    }
	    break;
	}
	case START_TRACING: {
	    generateCFunctionCall("SYSTEM_START_TRACING", 1);
	    break;
	}
	case STOP_TRACING: {
	    generateCFunctionCall("SYSTEM_STOP_TRACING", 0);
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
	    xasm.popR(R_EDX); //rhs
	    if (op == WordOps.uI2L) {
		xasm.pushI32(0);
		xasm.pushR(R_EDX);
		break;
	    }
	    xasm.popR(R_EAX); //lhs
	    switch(op) {
	    case WordOps.sCMP: {
		xasm.cmpRR(R_EAX, R_EDX);
		Branch b1 = xasm.setBranchSourceAndJcc(J_E);
		Branch b2 = xasm.setBranchSourceAndJcc(J_L);
		// 1
		xasm.pushI32(1);
		Branch b3 = xasm.setBranchSourceAndJmp();
		// -1
		xasm.setBranchTarget(b2);
		xasm.pushI32(-1);
		Branch b4 = xasm.setBranchSourceAndJmp();
		// 0
		xasm.setBranchTarget(b1);
		xasm.pushI32(0);
		xasm.setBranchTarget(b3);
		xasm.setBranchTarget(b4);
		break;
	    }
	    case WordOps.uCMP: {
		xasm.cmpRR(R_EAX, R_EDX);
		Branch b1 = xasm.setBranchSourceAndJcc(J_E);
		Branch b2 = xasm.setBranchSourceAndJcc(J_B);
		// 1
		xasm.pushI32(1);
		Branch b3 = xasm.setBranchSourceAndJmp();
		// -1
		xasm.setBranchTarget(b2);
		xasm.pushI32(-1);
		Branch b4 = xasm.setBranchSourceAndJmp();
		// 0
		xasm.setBranchTarget(b1);
		xasm.pushI32(0);
		xasm.setBranchTarget(b3);
		xasm.setBranchTarget(b4);
		break;
	    }
	    case WordOps.uLT: {
		xasm.cmpRR(R_EAX, R_EDX);
		Branch b1 = xasm.setBranchSourceAndJcc(J_B);
		xasm.pushI32(0);
		Branch b2 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(b1);
		xasm.pushI32(1);
		xasm.setBranchTarget(b2);
		break;
	    }
	    case WordOps.uLE: {
		xasm.cmpRR(R_EAX, R_EDX);
		Branch b1 = xasm.setBranchSourceAndJcc(J_BE);
		xasm.pushI32(0);
		Branch b2 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(b1);
		xasm.pushI32(1);
		xasm.setBranchTarget(b2);
		break;
	    }
	    case WordOps.uGE: {
		xasm.cmpRR(R_EAX, R_EDX);
		Branch b1 = xasm.setBranchSourceAndJcc(J_AE);
		xasm.pushI32(0);
		Branch b2 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(b1);
		xasm.pushI32(1);
		xasm.setBranchTarget(b2);
		break;
	    }
	    case WordOps.uGT: {
		xasm.cmpRR(R_EAX, R_EDX);
		Branch b1 = xasm.setBranchSourceAndJcc(J_A);
		xasm.pushI32(0);
		Branch b2 = xasm.setBranchSourceAndJmp();
		xasm.setBranchTarget(b1);
		xasm.pushI32(1);
		xasm.setBranchTarget(b2);
		break;
	    }
	    default:
		throw new OVMError.Internal
		    ("Unsupported invoke_system wordop argument : " + op);
	    }
	    break;
	}
	case DEREFERENCE: {
	    int op = (0xFF & (0x100 + i.getOpType(buf)));
	    switch(op) {
	    case DereferenceOps.getByte:
		xasm.popR(R_EAX);
		xasm.movsxM8R(R_EAX, 0, R_EDX);
		xasm.pushR(R_EDX);
		break;
	    case DereferenceOps.getShort:
		xasm.popR(R_EAX);
		xasm.movsxM16R(R_EAX, 0, R_EDX);
		xasm.pushR(R_EDX);
		break;
	    case DereferenceOps.getChar:
		xasm.popR(R_EAX);
		xasm.movzxM16R(R_EAX, 0, R_EDX);
		xasm.pushR(R_EDX);
		break;
	    case DereferenceOps.setByte:
		xasm.popR(R_ECX); // val
		xasm.popR(R_EAX); // loc
		xasm.movR8M(R_ECX, R_EAX, 0);
		break;
	    case DereferenceOps.setShort:
		xasm.popR(R_ECX); // val
		xasm.popR(R_EAX); // loc
		xasm.movR16M(R_ECX, R_EAX, 0);
		break;
	    case DereferenceOps.setBlock:
		xasm.popR(R_ECX); // size
		xasm.popR(R_EDX); // src
		xasm.popR(R_EAX); // dst
		xasm.pushR(R_ECX); // size
		xasm.pushR(R_EDX); // src
		xasm.pushR(R_EAX); // dst
		generateCFunctionCall("memmove", 3);
		break;
	    default:
		throw new OVMError.Internal
		    ("Unsupported invoke_system dereference op argument : " + op);
	    }
	    break;
	}

	case CAS32: {
	    /* non-inlined version 
	       {
	       xasm.popR(R_ECX);  // new value
	       xasm.popR(R_EAX);  // old value
	       xasm.popR(R_EDX);  // ptr
	       xasm.pushR(R_ECX);
	       xasm.pushR(R_EAX);
	       xasm.pushR(R_EDX);
	       generateCFunctionCall("CAS32", 3);
	       xasm.pushR(R_EAX);
               }
	    */
	    xasm.popR(R_ECX);  // new value
	    xasm.popR(R_EAX);  // old value
	    xasm.popR(R_ESI);  // ptr
	    xasm.xorRR(R_EDX, R_EDX); // zero ESI
	    // if *ptr == oldvalue
	    // then {*ptr = newvalue; push(1); }
	    // else { push(0); }
	    xasm.cmpxchgRM(true, R_ECX, R_ESI, 0);
	    xasm.sete(R_EDX);
	    xasm.pushR(R_EDX);
	    break;
	}
	default:
	    throw new OVMError.Internal
		("Unsupported invoke_system argument : " + mindex);
	}
    }

} // end of CodeGeneratorImpl
