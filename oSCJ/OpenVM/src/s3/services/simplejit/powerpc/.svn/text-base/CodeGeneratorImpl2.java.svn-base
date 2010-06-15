package s3.services.simplejit.powerpc;

import ovm.core.domain.ConstantResolvedInstanceFieldref;
import ovm.core.domain.ConstantResolvedInstanceMethodref;
import ovm.core.domain.ConstantResolvedInterfaceMethodref;
import ovm.core.domain.ConstantResolvedStaticFieldref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.LinkageException;
import ovm.core.repository.Attribute;
import ovm.core.repository.Descriptor;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Area;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionSet;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.Instruction.RETURN;
import ovm.util.ArrayList;
import ovm.util.ByteBuffer;
import ovm.util.HTint2Object;
import ovm.util.OVMError;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Field;
import s3.core.domain.S3Method;
import s3.services.simplejit.CodeGenContext;
import s3.services.simplejit.CodeGenerator;
import s3.services.simplejit.SimpleJIT;
import s3.services.simplejit.CompilerVMInterface;
import s3.services.simplejit.Assembler.Branch;
import s3.services.simplejit.bytecode.Translator.Liveness;
import s3.services.simplejit.bytecode.Translator.RegisterTable;
import ovm.util.ByteBuffer.BufferOverflowException;

/**
 * Code generator with register allocation.
 * @author Hiroshi Yamauchi
 */
public final class CodeGeneratorImpl2 extends CodeGeneratorImpl {

    /*
     * All caller-saved
     * 
     * GRP: R17-R31 (15) - allocatable, R11-R16 - scratch, R3-R10 - parameters
     * 15, 11 (R17-R27), 7 (R17-R23), 3 (R17-R19)
     * FRP: F19-F31 (13) - allocatable, F14-F18 - scratch, F1-F13 - parameters
     * 13, 10 (F19-F28), 7 (F19-F25), 3 (F19-F21)
     */
    public static class Registers {
        // GPR
        static final int firstParamaterGPR = R3;

        static final int lastParamaterGPR = R10;

        static final int nParamaterGPRs = lastParamaterGPR - firstParamaterGPR
                + 1;

        static final int firstScratchGPR = R11;

        static final int lastScratchGPR = R16;

        static final int nScratchGPRs = lastScratchGPR - firstScratchGPR + 1;

        static final int firstAllocatableGPR = lastScratchGPR + 1;

        static final int lastAllocatableGPR = R31;

        static final int nAllocatableGPRs = lastAllocatableGPR
                - firstAllocatableGPR + 1;

        static final int getParameterGPR(int n) {
            if (n >= nParamaterGPRs) {
                throw new Error("Out of parameter GPRs. [" + firstParamaterGPR
                        + "-" + lastParamaterGPR + "]");
            }
            return n + firstParamaterGPR;
        }

        static final int getScratchGPR(int n) {
            if (n >= nScratchGPRs) {
                throw new Error("Out of scracth GPRs. [" + firstScratchGPR
                        + "-" + lastScratchGPR + "]");
            }
            return n + firstScratchGPR;
        }

        static int currentScratchGPR = firstScratchGPR;

        static boolean scratchGPRsAllReturned() {
            return currentScratchGPR == firstScratchGPR;
        }

        static final int getScratchGPR() {
            if (firstScratchGPR <= currentScratchGPR
                    && currentScratchGPR <= lastScratchGPR) {
                // BasicIO.ut.println("lent " + currentScratchGPR);
                return currentScratchGPR++;
            }
            throw new Error("Out of scratch GPRs");
        }

        static final void returnScratchGPR(int gpr) {
            if (firstScratchGPR <= gpr && gpr <= lastScratchGPR
                    && gpr == currentScratchGPR - 1) {
                // BasicIO.out.println("returned " + gpr);
                currentScratchGPR--;
            } else {
                throw new Error("Wrong GPR returned : " + gpr);
            }
        }

        static final int getAllocatableGPR(int n) {
            if (n >= nAllocatableGPRs) {
                throw new Error("Out of allocatable GPRs. ["
                        + firstAllocatableGPR + "-" + lastAllocatableGPR + "]");
            }
            return n + firstAllocatableGPR;
        }

        // FPR
        static final int firstParamaterFPR = F1;

        static final int lastParamaterFPR = F13;

        static final int nParamaterFPRs = lastParamaterFPR - firstParamaterFPR
                + 1;

        static final int firstScratchFPR = F14;

        static final int lastScratchFPR = F18;

        static final int nScratchFPRs = lastScratchFPR - firstScratchFPR + 1;

        static final int firstAllocatableFPR = F19;

        static final int lastAllocatableFPR = F31;

        static final int nAllocatableFPRs = lastAllocatableFPR
                - firstAllocatableFPR + 1;

        static final int getParameterFPR(int n) {
            if (n >= nParamaterFPRs) {
                throw new Error("Out of parameter FPRs. [" + firstParamaterFPR
                        + "-" + lastParamaterFPR + "]");
            }
            return n + firstParamaterFPR;
        }

        static final int getScratchFPR(int n) {
            if (n >= nScratchFPRs) {
                throw new Error("Out of scracth FPRs. [" + firstScratchFPR
                        + "-" + lastScratchFPR + "]");
            }
            return n + firstScratchFPR;
        }

        static int currentScratchFPR = firstScratchFPR;

        static boolean scratchFPRsAllReturned() {
            return currentScratchFPR == firstScratchFPR;
        }

        static final int getScratchFPR() {
            if (firstScratchFPR <= currentScratchFPR
                    && currentScratchFPR <= lastScratchFPR) {
                return currentScratchFPR++;
            }
            throw new Error("Out of scratch FPRs");
        }

        static final void returnScratchFPR(int fpr) {
            if (firstScratchFPR <= fpr && fpr <= lastScratchFPR
                    && fpr == currentScratchFPR - 1) {
                currentScratchFPR--;
            } else {
                throw new Error("Wrong FPR returned : " + fpr);
            }
        }

        static final int getAllocatableFPR(int n) {
            if (n >= nAllocatableFPRs) {
                throw new Error("Out of allocatable FPRs. ["
                        + firstAllocatableFPR + "-" + lastAllocatableFPR + "]");
            }
            return n + firstAllocatableFPR;
        }

        private final RegisterTable registerTable;

        private final Liveness liveness;

        private final S3ByteCode bytecode;

        private final int[] l2r;

        Registers(S3ByteCode bytecode, RegisterTable registerTable,
                Liveness liveness) {
            this.registerTable = registerTable;
            this.liveness = liveness;
            this.bytecode = bytecode;
            if (bytecode.getMaxLocals() != registerTable.getMapLength()) {
                throw new Error("# of locals does not match");
            }
            this.l2r = new int[registerTable.getMapLength()];
            computeRegisterAssignments();
        }

        boolean[] getLivenessAt(int pc) {
            Liveness.Entry[] entries = liveness.getEntries();
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].position() == pc) {
                    return entries[i].liveness();
                }
            }
            throw new Error("Liveness not available at PC " + pc);
        }

        char getLocalType(int index) {
            RegisterTable.Entry[] entries = registerTable.getEntries();
            return entries[index].type();
        }

        private static class Local {
            int index;

            int score;

            Local(int i, int s) {
                index = i;
                score = s;
            }
        }

        // GPR: I, A, FPR: F, D, Non-allocated: J
        void computeRegisterAssignments() {
            ArrayList gprList = new ArrayList(); // list of gpr locals in the
            // order of scores
            ArrayList fprList = new ArrayList(); // list of fpr locals in the
            // order of scores
            sortLocals(gprList, fprList);
            assignRegisters(gprList, fprList);
            //BasicIO.out.println(toString());
        }

        public String toString() {
            String r = "[ ";
            for (int i = 0; i < l2r.length; i++) {
                r += "L" + i + ":" + l2r[i] + " ";
            }
            r += " ]";
            return r;
        }

        void assignRegisters(ArrayList gprList, ArrayList fprList) {
            for (int i = 0; i < l2r.length; i++) {
                l2r[i] = 0;
            }
            for (int i = 0; i < nAllocatableGPRs; i++) {
                int r = i + firstAllocatableGPR;
                if (i < gprList.size()) {
                    int localIndex = ((Local) gprList.get(i)).index;
                    l2r[localIndex] = r;
                }
            }
            for (int i = 0; i < nAllocatableFPRs; i++) {
                int r = i + firstAllocatableFPR;
                if (i < fprList.size()) {
                    int localIndex = ((Local) fprList.get(i)).index;
                    l2r[localIndex] = r;
                }
            }
        }

        /**
         * Sort locals in the descending order of scores
         * 
         * GPR: I, A, FPR: F, D, Non-allocated: J
         * 
         * @param gprList
         * @param fprList
         */
        void sortLocals(ArrayList gprList, ArrayList fprList) {
            RegisterTable.Entry[] entries = registerTable.getEntries();
	    ArrayList alist = new ArrayList();
	    ArrayList ilist = new ArrayList();
	    ArrayList flist = new ArrayList();
	    ArrayList dlist = new ArrayList();

            for (int i = 0; i < entries.length; i++) {
                // i : local variable index
                RegisterTable.Entry entry = entries[i];
                char type = entry.type();
                int score = entry.score();
                switch (type) {
                case TypeCodes.VOID:
                    continue;
                case TypeCodes.INT:
		    ilist.add(new Local(i, score));
		    break;
                case TypeCodes.REFERENCE:
		    alist.add(new Local(i, score));
                    break;
                case TypeCodes.FLOAT:
		    flist.add(new Local(i, score));
		    break;
                case TypeCodes.DOUBLE:
		    dlist.add(new Local(i, score));
                    break;
                case TypeCodes.LONG:
                    break;
                default:
                    throw new Error("Unexpected type code : " + type);
                }
            }

	    int alistSize = alist.size();
	    int ilistSize = ilist.size();
	    int flistSize = flist.size();
	    int dlistSize = dlist.size();

	    // merge sort
	    int a_index = 0;
	    int i_index = 0;
	    while (a_index < alistSize &&  i_index < ilistSize) {
		Local al = (Local) alist.get(a_index);
		Local il = (Local) ilist.get(i_index);
		if (al.score > il.score) {
		    gprList.add(al);
		    a_index++;
		} else {
		    gprList.add(il);
		    i_index++;
		}
	    }
	    while (a_index < alistSize) {
		gprList.add(alist.get(a_index));
		a_index++;
	    }
	    while (i_index < ilistSize) {
		gprList.add(ilist.get(i_index));
		i_index++;
	    }

	    int f_index = 0;
	    int d_index = 0;
	    while (f_index < flistSize && d_index < dlistSize) {
		Local fl = (Local) flist.get(f_index);
		Local dl = (Local) dlist.get(d_index);
		if (fl.score > dl.score) {
		    fprList.add(fl);
		    f_index++;
		} else {
		    fprList.add(dl);
		    d_index++;
		}
	    }
	    while (f_index < flistSize) {
		fprList.add(flist.get(f_index));
		f_index++;
	    }
	    while (d_index < dlistSize) {
		fprList.add(dlist.get(d_index));
		d_index++;
	    }

        }

        /**
         * Return the register number (both FPR and GPR) assigned to the given
         * local index
         * 
         * @param local
         * @return the register number. 0 means no register assigned.
         */
        int l2r(int local) {
            return l2r[local];
        }
    }

    public static class VStack {
        public static abstract class Item {
        }

        public static class Local extends Item {
            int index;

            char type;

            Local(char type, int index) {
                this.index = index;
                this.type = type;
            }

            char type() {
                return type;
            }

            int index() {
                return index;
            }

            public String toString() {
                return "L:" + type + ":" + index;
            }
        }

        public static class Constant extends Item {
            char type;

            Object value;

            Constant(char type, Object value) {
                this.type = type;
                this.value = value;
            }

            public String toString() {
                switch (type) {
                case TypeCodes.INT:
                    return "C:I:" + intValue();
                case TypeCodes.LONG:
                    return "C:J:" + longValue();
                default:
                    throw new Error();
                }
            }

            char type() {
                return type;
            }

            int intValue() {
                if (type == TypeCodes.INT) {
                    return ((Integer) value).intValue();
                }
                throw new Error();
            }

            long longValue() {
                if (type == TypeCodes.LONG) {
                    return ((Long) value).longValue();
                }
                throw new Error();
            }

            float floatValue() {
                if (type == TypeCodes.FLOAT) {
                    return ((Float) value).floatValue();
                }
                throw new Error();
            }

            double doubleValue() {
                if (type == TypeCodes.DOUBLE) {
                    return ((Double) value).doubleValue();
                }
                throw new Error();
            }
        }

        private ArrayList internal;

        public VStack() {
            internal = new ArrayList();
        }

        public int size() {
            return internal.size();
        }

        public Item get(int n) {
            return (Item) internal.get(n);
        }

        public Item pop() {
            return (Item) internal.remove(internal.size() - 1);
        }

        public void push(Item o) {
            internal.add(o);
        }

        public boolean empty() {
            return internal.size() == 0;
        }

        public String toString() {
            String r = "VS[ ";
            for (int i = 0; i < internal.size(); i++) {
                r += internal.get(i).toString() + " ";
            }
            r += "]";
            return r;
        }
    }

    private ByteBuffer bytecodeBuffer;

    private Registers registers;

    private VStack vstack;

    private int currInstrSize;
    
    private HTint2Object handlerPCs;
    
    private int intrinsicPC;
    
    public CodeGeneratorImpl2(S3Method method, InstructionSet is,
            CompilerVMInterface compilerVMInterface, Precomputed precomputed,
            boolean debugPrintOn) {
        super(method, is, compilerVMInterface, precomputed, debugPrintOn);
        bytecodeBuffer = getCode();
        bytecodeBuffer.rewind();
        vstack = new VStack();
        handlerPCs = new HTint2Object();
        for(int i = 0; i < exceptionHandlers.length; i++) {
            handlerPCs.put(exceptionHandlers[i].getHandlerPC(), this);
        }
    }

    private void readSpecialAttributes() {
        S3ByteCode bytecode = this.bytecode;
        Attribute[] attributes = bytecode.getAttributes();
        RegisterTable registerTable = null;
        Liveness liveness = null;
        for (int i = 0; i < attributes.length; i++) {
            Attribute att = attributes[i];
            if ("RegisterTable".equals(att.getName())) {
                registerTable = (RegisterTable) att;
            }
            if ("Liveness".equals(att.getName())) {
                liveness = (Liveness) att;
            }
        }
        if (registerTable == null || liveness == null) {
            throw new Error("Missing special attributes");
        }
        registers = new Registers(bytecode, registerTable, liveness);
    }

    protected void prepareAssembler(CodeGenContext context) {
	SimpleJIT.pauseCompileTimer();
        readSpecialAttributes();
	SimpleJIT.restartCompileTimer();
	bytecodeBuffer.rewind();
	currInstrSize = 0;
        this.xasm = new PPCAssembler(context.getCodeBuffer(), 
				     CodeGenContext.globalCodeArray.position(),
				     0);
    }

    protected void generatePrologue() {
        if (method.getArgumentLength() + 8 /* receiver+code */> StackLayoutImpl.PARAM_AREA_SIZE) {
            throw new Error("PARAM_AREA_SIZE is too small");
        }

        int nativeFrameSize = stackLayout.getNativeFrameSize();
        xasm.emit_mflr(R0);
        xasm.emit_stw(R0, SP, stackLayout.getReturnAddressOffset());
        if (nativeFrameSize < -stackLayout
                .getInitialOperandStackPointerOffset()) {
            throw new Error(
                    "Initial operand stack pointer is below the stack pointer : "
                            + " nativeFrameSize " + nativeFrameSize
                            + ", initialOperandStackPointer "
                            + stackLayout.getInitialOperandStackPointerOffset());

        }
        if ((nativeFrameSize >> 15) != 0) {
            throw new Error("nativeFrameSize is too large " + nativeFrameSize);
        }
        xasm.emit_stwu(SP, SP, -nativeFrameSize);

        unloadArguments(method.getSelector().getDescriptor());

        // Adjust the PC to the end of argument copying
        bytecodeBuffer.advance(currInstrSize);
        currInstrSize = 0;
        intrinsicPC = getPC();
        
        if (! OMIT_STACKOVERFLOW_CHECKS) 
            generateStackoverflowCheck(isSynchronized ? new int[] {R4} : null, null);
        
        if (isSynchronized) { 
            xasm.emit_stw(R4, SP, stackLayout.getLocalVariableNativeOffset(0)
                    + nativeFrameSize);
            xasm.emit_mr(R5, R4);
            generateCSACall(precomputed.csa_monitorEnter_index,
                    precomputed.csa_monitorEnter_desc, null, null); 
        }
    }

    public void storeGPRToLocalOnStack(int gpr, int localIndex) {
        int localOffset = stackLayout.getLocalVariableNativeOffset(localIndex);
        int localOffsetFromSP = localOffset + stackLayout.getNativeFrameSize();
        xasm.emit_stw(gpr, SP, localOffsetFromSP);
    }

    public void storeLongGPRsToLocalOnStack(int gprh, int gprl, int localIndex) {
        int localOffset = stackLayout.getLocalVariableNativeOffset(localIndex);
        int localOffsetFromSP = localOffset + stackLayout.getNativeFrameSize();
        xasm.emit_stw(gprh, SP, localOffsetFromSP);
        xasm.emit_stw(gprl, SP, localOffsetFromSP + 4);
    }

    public void storeDoubleFPRToLocalOnStack(int fpr, int localIndex) {
        int localOffset = stackLayout.getLocalVariableNativeOffset(localIndex);
        int localOffsetFromSP = localOffset + stackLayout.getNativeFrameSize();
        xasm.emit_stfd(fpr, SP, localOffsetFromSP);
    }

    public void storeFloatFPRToLocalOnStack(int fpr, int localIndex) {
        int localOffset = stackLayout.getLocalVariableNativeOffset(localIndex);
        int localOffsetFromSP = localOffset + stackLayout.getNativeFrameSize();
        xasm.emit_stfs(fpr, SP, localOffsetFromSP);
    }

    private void unloadArguments(Descriptor.Method desc) {
        int frame_size = stackLayout.getNativeFrameSize();
        int argOffset = stackLayout.getReceiverOffset() + frame_size;
        xasm.emit_stw(R3, SP, stackLayout.getCodeFragmentOffset() + frame_size); // code

        int gprArgumentIndex = R5; // R5 to R10
        int fprArgumentIndex = F1; // F1 to F13

        { // Receiver
            Instruction.LocalRead load = (Instruction.LocalRead) nextInstruction();
            // int paramIndex = load.getLocalVariableOffset(buf);
            Instruction store_or_pop = nextInstruction();
            if (store_or_pop instanceof Instruction.LocalWrite) {
                Instruction.LocalWrite store = (Instruction.LocalWrite) store_or_pop;
                int localIndex = store.getLocalVariableOffset(buf);
                int reg = registers.l2r(localIndex);
                if (reg > 0) {
                    xasm.emit_mr(reg, R4);
                } else {
                    storeGPRToLocalOnStack(R4, localIndex);
                }
            } else {
                if (!(store_or_pop instanceof Instruction.POP || store_or_pop instanceof Instruction.POP2)) {
                    throw new Error("Found neither a store or pop"
				    + store_or_pop);
                }
            }
            argOffset += 4;
        }

        for (int k = 0; k < desc.getArgumentCount(); k++) {
            Instruction.LocalRead load = (Instruction.LocalRead) nextInstruction();
            // int paramIndex = load.getLocalVariableOffset(buf);
            Instruction store_or_pop = nextInstruction();
            char t = desc.getArgumentType(k).getTypeTag();
            if (store_or_pop instanceof Instruction.LocalWrite) {
                Instruction.LocalWrite store = (Instruction.LocalWrite) store_or_pop;
                int localIndex = store.getLocalVariableOffset(buf);
                switch (t) {
                case TypeCodes.VOID:
                    break;
                case TypeCodes.INT:
                case TypeCodes.REFERENCE:
                case TypeCodes.SHORT:
                case TypeCodes.CHAR:
                case TypeCodes.BYTE:
                case TypeCodes.BOOLEAN:
                case TypeCodes.OBJECT:
                case TypeCodes.ARRAY:
                    if (gprArgumentIndex <= R10) {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_mr(reg, gprArgumentIndex);
                        } else {
                            storeGPRToLocalOnStack(gprArgumentIndex, localIndex);
                        }
                        gprArgumentIndex++;
                    } else {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_lwz(reg, SP, argOffset);
                        } else {
                            int scratch = Registers.getScratchGPR();
                            try {
                                xasm.emit_lwz(scratch, SP, argOffset);
                                storeGPRToLocalOnStack(scratch, localIndex);
                            } finally {
                                Registers.returnScratchGPR(scratch);
                            }
                        }
                    }
                    argOffset += 4;
                    break;
                case TypeCodes.LONG:
                    if (gprArgumentIndex <= R10) {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_mr(reg, gprArgumentIndex);
                        } else {
                            int localOffset = stackLayout
                                    .getLocalVariableNativeOffset(localIndex)
                                    + stackLayout.getNativeFrameSize();
                            xasm.emit_stw(gprArgumentIndex, SP, localOffset);
                        }
                        gprArgumentIndex++;
                    } else {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_lwz(reg, SP, argOffset);
                        } else {
                            int scratch = Registers.getScratchGPR();
                            try {
                                xasm.emit_lwz(scratch, SP, argOffset);
                                int localOffset = stackLayout
                                        .getLocalVariableNativeOffset(localIndex)
                                        + stackLayout.getNativeFrameSize();
                                xasm.emit_stw(scratch, SP, localOffset);
                            } finally {
                                Registers.returnScratchGPR(scratch);
                            }
                        }
                    }
                    argOffset += 4;
                    if (gprArgumentIndex <= R10) {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_mr(reg, gprArgumentIndex);
                        } else {
                            int localOffset = stackLayout
                                    .getLocalVariableNativeOffset(localIndex)
                                    + stackLayout.getNativeFrameSize();
                            xasm
                                    .emit_stw(gprArgumentIndex, SP,
                                            localOffset + 4);
                        }
                        gprArgumentIndex++;
                    } else {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_lwz(reg, SP, argOffset);
                        } else {
                            int scratch = Registers.getScratchGPR();
                            try {
                                xasm.emit_lwz(scratch, SP, argOffset);
                                int localOffset = stackLayout
                                        .getLocalVariableNativeOffset(localIndex)
                                        + stackLayout.getNativeFrameSize();
                                xasm.emit_stw(scratch, SP, localOffset + 4);
                            } finally {
                                Registers.returnScratchGPR(scratch);
                            }
                        }
                    }
                    argOffset += 4;
                    break;
                case TypeCodes.DOUBLE:
                    if (fprArgumentIndex <= F13) {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_fmr(reg, fprArgumentIndex);
                        } else {
                            storeDoubleFPRToLocalOnStack(fprArgumentIndex,
                                    localIndex);
                        }
                        fprArgumentIndex++;
                    } else {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_lfd(reg, SP, argOffset);
                        } else {
                            int scratch = Registers.getScratchFPR();
                            try {
                                xasm.emit_lfd(scratch, SP, argOffset);
                                storeDoubleFPRToLocalOnStack(scratch,
                                        localIndex);
                            } finally {
                                Registers.returnScratchFPR(scratch);
                            }
                        }
                    }
                    argOffset += 8;
                    break;
                case TypeCodes.FLOAT:
                    if (fprArgumentIndex <= F13) {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_fmr(reg, fprArgumentIndex);
                        } else {
                            storeFloatFPRToLocalOnStack(fprArgumentIndex,
                                    localIndex);
                        }
                        fprArgumentIndex++;
                    } else {
                        int reg = registers.l2r(localIndex);
                        if (reg > 0) {
                            xasm.emit_lfs(reg, SP, argOffset);
                        } else {
                            int scratch = Registers.getScratchFPR();
                            try {
                                xasm.emit_lfs(scratch, SP, argOffset);
                                storeFloatFPRToLocalOnStack(scratch, localIndex);
                            } finally {
                                Registers.returnScratchFPR(scratch);
                            }
                        }
                    }
                    argOffset += 4;
                    break;
                default:
                    throw new OVMError();
                }
            } else {
                if (store_or_pop instanceof Instruction.POP 
                        || store_or_pop instanceof Instruction.POP2) {
                    switch (t) {
                    case TypeCodes.VOID:
                        break;
                    case TypeCodes.INT:
                    case TypeCodes.REFERENCE:
                    case TypeCodes.SHORT:
                    case TypeCodes.CHAR:
                    case TypeCodes.BYTE:
                    case TypeCodes.BOOLEAN:
                    case TypeCodes.OBJECT:
                    case TypeCodes.ARRAY:
                        if (gprArgumentIndex <= R10) {
                            gprArgumentIndex++;
                        }
                        argOffset += 4;
                        break;
                    case TypeCodes.LONG:
                        if (gprArgumentIndex <= R10) {
                            gprArgumentIndex++;
                        }
                        argOffset += 4;
                        if (gprArgumentIndex <= R10) {
                            gprArgumentIndex++;
                        }
                        argOffset += 4;
                        break;
                    case TypeCodes.DOUBLE:
                        if (fprArgumentIndex <= F13) {
                            fprArgumentIndex++;
                        }
                        argOffset += 8;
                        break;
                    case TypeCodes.FLOAT:
                        if (fprArgumentIndex <= F13) {
                            fprArgumentIndex++;
                        }
                        argOffset += 4;
                        break;
                    default:
                        throw new OVMError();
                    }
                } else {
                    throw new Error("Found neither a store or pop");
                }
            }
        }
    }

    private Instruction nextInstruction() {
        Instruction[] set = is_.set;
        bytecodeBuffer.advance(currInstrSize);
        Instruction i = set[(bytecodeBuffer.get(getPC()) + 0x100) & 0xFF];
        if (i instanceof Instruction.WIDE) {
        		i = ((Instruction.WIDE)i).specialize(buf);
        }
        currInstrSize = i.size(buf);
        //codeGenContext.setBytecodePC2NativePC(getPC(), codeGenContext
        //        .getNativePC());
        return i;
    }

    private void printIntArray(int[] arr) {
        String s = "IA:";
        for(int i = 0; i < arr.length; i++) {
            s += "[" + i + "]=" + arr[i] + " ";
        }
        BasicIO.out.println(s);
    }
    
    private void completePCMap() {
	SimpleJIT.pauseCompileTimer();
        Instruction[] set = is_.set;
        int[] bpc2npc = codeGenContext.getBytecodePC2NativePC();
        //printIntArray(bpc2npc);
        bytecodeBuffer.rewind();
        while (bytecodeBuffer.remaining() > 0) {
            Instruction i = set[(bytecodeBuffer.get(getPC()) + 0x100) & 0xFF];
            int size = i.size(buf);
            int npc = bpc2npc[getPC()];
            if (npc == 0) {
                for(int bpc = getPC() + size; bpc < bpc2npc.length; bpc++) {
                    if (bpc2npc[bpc] != 0) {
                        bpc2npc[getPC()] = bpc2npc[bpc];
                        break;
                    }
                }
            }
            bytecodeBuffer.advance(size);
        }
        //printIntArray(bpc2npc);
	SimpleJIT.restartCompileTimer();
    }
    
    protected void generateBody() {
        Instruction[] set = is_.set;
        bytecodeBuffer.advance(currInstrSize);
        while (bytecodeBuffer.remaining() > 0) {
            Instruction i = set[(bytecodeBuffer.get(getPC()) + 0x100) & 0xFF];
            currInstrSize = i.size(buf);
            codeGenContext.setBytecodePC2NativePC(getPC(), codeGenContext
                    .getNativePC());
            intrinsicPC = getPC();
            try {
                if (handlerPCs.get(getPC()) != null) {
                    generateHandlerEntry(i);
                } else {
                    this.visitAppropriate(i);
                }
            } catch (BufferOverflowException e) {
		throw e;
	    } catch (Throwable t) {
		Error e = new Error("error compiling instruction " + getPC());
		e.initCause(t);
		throw e;
            }
	    if (intrinsicPC != getPC()) { // nextInstruction() was called in the visit method
		codeGenContext.setBytecodePC2NativePC(getPC(), codeGenContext.getNativePC());
	    }
	    /*
            if (!(Registers.scratchGPRsAllReturned() && Registers
                    .scratchFPRsAllReturned())) {
                throw new Error(
                        "Some scratch registers are not returned at PC "
                                + getPC());
            }
	    */
            bytecodeBuffer.advance(currInstrSize);
        }
        //completePCMap();
        codeGenContext.linkRelativeJumps();
    }

    private void generateHandlerEntry(Instruction i) {
        final int offset = stackLayout.getInitialOperandStackPointerOffset() - 4
            + stackLayout.getNativeFrameSize();
        if (i instanceof Instruction.POP) {
            restoreRegisters(null, null);
            return;
        }
        int S0 = Registers.getScratchGPR();
        try {
            Instruction.LocalWrite write = (Instruction.LocalWrite) i;
            int dindex = write.getLocalVariableOffset(buf);
            int dreg = registers.l2r(dindex);
            int dLocalOffset = stackLayout
                    .getLocalVariableNativeOffset(dindex)
                    + stackLayout.getNativeFrameSize();
            if (dreg > 0) {
                xasm.emit_lwz(dreg, SP, offset);
            } else {
                xasm.emit_lwz(S0, SP, offset);
                xasm.emit_stw(S0, SP, dLocalOffset);
            }
        } finally {
            Registers.returnScratchGPR(S0);
        }
        restoreRegisters(null, null);
    }
    
    private void generateStackoverflowCheck(int[] GPRsToSave,
            int[] FPRsToSave) {
        if (OPTIMIZE_STACK_OVERFLOW_CHECK) {
            int currentContextIndex =
                compilerVMInterface
                .getRuntimeFunctionIndex("currentContext");
            if (currentContextIndex == -1)
                throw new OVMError();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                xasm.emit_li32(S0, precomputed.runtimeFunctionTableHandle.asInt());
                xasm.emit_lwz(S0, S0, 0);
                xasm.emit_lwz(S0, S0, currentContextIndex * 4); 
                xasm.emit_lwz(S1, S0, 0); // S1 = current context
                xasm.emit_lwz(S0, S1, precomputed.offset_mtb_in_nc);
                xasm.emit_lwz(S0, S0, precomputed.offset_redzone_in_mtb);
                xasm.emit_cmpl(CR7, false, SP, S0);
                Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_GT);
                xasm.emit_lwz(S0, S1, precomputed.offset_pso_in_nc);
                xasm.emit_cmpi(CR7, false, S0, 0);
                Branch b1 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
                xasm.emit_li32(S0, 1);
                xasm.emit_stw(S0, S1, precomputed.offset_pso_in_nc);
                xasm.emit_li32(R6, 0);                                  // push meta
                xasm.emit_li32(R5, precomputed.stackOverflowErrorID); // push exception type 
                generateCSACall(precomputed.csa_generateThrowable_index, 
                        precomputed.csa_generateThrowable_desc, GPRsToSave, FPRsToSave);
                xasm.setBranchTarget(b0);
                xasm.setBranchTarget(b1);
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        } else {
            generateCFunctionCall("check_stack_overflow", GPRsToSave, FPRsToSave);
            xasm.emit_cmpi(CR7, false, R3, 0);
            Branch b = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
            xasm.emit_li32(R6, 0);                                // push meta
            xasm.emit_li32(R5, precomputed.stackOverflowErrorID); // push exception type 
            generateCSACall(precomputed.csa_generateThrowable_index, 
                    precomputed.csa_generateThrowable_desc, GPRsToSave, GPRsToSave);
            xasm.setBranchTarget(b);
        }
    }
    
    protected void generateCFunctionCall(String function, int[] GPRsToSave,
            int[] FPRsToSave) {
        int rtFuncIndex = compilerVMInterface.getRuntimeFunctionIndex(function);
        if (rtFuncIndex == -1)
            throw new OVMError.Unimplemented("Runtime function \"" + function
                    + "\" Not Found");
        int offset_fp_in_table = rtFuncIndex * 4;

        int S0 = Registers.getScratchGPR();
        try {
            xasm.emit_li32(S0, precomputed.runtimeFunctionTableHandle
                    .asInt()); // R11 = table handle
            xasm.emit_lwz(S0, S0, 0); // R11 = table objectref
            xasm.emit_lwz(S0, S0, offset_fp_in_table); // R11 = function
                                                        // pointer
            xasm.emit_mtctr(S0);
            saveRegistersForArgumentLocals();
            saveRegisters(GPRsToSave, FPRsToSave);
            xasm.emit_bctr_lk();
            restoreRegisters(GPRsToSave, FPRsToSave);
            restoreRegistersForArgumentLocals();
            xasm.emit_nop();
        } finally {
            Registers.returnScratchGPR(S0);
        }
    }

    protected void generateCFunctionCall(String function, int[] GPRsToSave,
            int[] FPRsToSave, int S0) {
        int rtFuncIndex = compilerVMInterface.getRuntimeFunctionIndex(function);
        if (rtFuncIndex == -1)
            throw new OVMError.Unimplemented("Runtime function \"" + function
                    + "\" Not Found");
        int offset_fp_in_table = rtFuncIndex * 4;

        xasm.emit_li32(S0, precomputed.runtimeFunctionTableHandle
                .asInt()); // R11 = table handle
        xasm.emit_lwz(S0, S0, 0); // R11 = table objectref
        xasm.emit_lwz(S0, S0, offset_fp_in_table); // R11 = function
                                                    // pointer
        xasm.emit_mtctr(S0);
        saveRegistersForArgumentLocals();
        saveRegisters(GPRsToSave, FPRsToSave);
        xasm.emit_bctr_lk();
        restoreRegisters(GPRsToSave, FPRsToSave);
        restoreRegistersForArgumentLocals();
        xasm.emit_nop();
    }
    
    protected void generateCSACall(int methodIndex, Descriptor.Method desc,
            int[] GPRsToSave, int[] FPRsToSave) {

        // BasicIO.err.println("generateCSACall() index = " + methodIndex);
        int offset_cf_in_csavtable = getArrayElementOffset(executiveDomain,
                OBJECT, methodIndex);
        int frame_size = stackLayout.getNativeFrameSize();

        int S0 = Registers.getScratchGPR();
        try {
            xasm.emit_lwz(S0, SP, frame_size
                    + stackLayout.getCodeFragmentOffset());
            xasm.emit_lwz(R4, S0, precomputed.offset_csa_in_cf);
            xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(S0, R4));
            xasm.emit_lwz(S0, S0, precomputed.offset_vtbl_in_bp);
            xasm.emit_lwz(R3, S0, offset_cf_in_csavtable);
            xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
            xasm.emit_mtctr(S0);
            saveRegistersForArgumentLocals();
            saveRegisters(GPRsToSave, FPRsToSave);
            xasm.emit_bctr_lk();
            restoreRegisters(GPRsToSave, FPRsToSave);
            restoreRegistersForArgumentLocals();
	    xasm.emit_nop();
            // unloadReturnValue(desc.getType().getTypeTag());
        } finally {
            Registers.returnScratchGPR(S0);
        }
    }

    protected void generateCSACall(int methodIndex, Descriptor.Method desc,
            int[] GPRsToSave, int[] FPRsToSave, int S0) {

        // BasicIO.err.println("generateCSACall() index = " + methodIndex);
        int offset_cf_in_csavtable = getArrayElementOffset(executiveDomain,
                OBJECT, methodIndex);
        int frame_size = stackLayout.getNativeFrameSize();

        xasm.emit_lwz(S0, SP, frame_size
                + stackLayout.getCodeFragmentOffset());
        xasm.emit_lwz(R4, S0, precomputed.offset_csa_in_cf);
        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(S0, R4));
        xasm.emit_lwz(S0, S0, precomputed.offset_vtbl_in_bp);
        xasm.emit_lwz(R3, S0, offset_cf_in_csavtable);
        xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
        xasm.emit_mtctr(S0);
        saveRegistersForArgumentLocals();
        saveRegisters(GPRsToSave, FPRsToSave);
        xasm.emit_bctr_lk();
        restoreRegisters(GPRsToSave, FPRsToSave);
        restoreRegistersForArgumentLocals();
	xasm.emit_nop();
        // unloadReturnValue(desc.getType().getTypeTag());
    }
    
    protected void generateNullCheck(int receiver_reg, int[] GPRsToSave,
            int[] FPRsToSave) {
        if (OMIT_NULLPOINTER_CHECKS)
            return;
        xasm.emit_cmpi(CR7, false, receiver_reg, 0);
        Branch b0 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R6, 0); // push meta
        xasm.emit_li32(R5, precomputed.nullPointerExceptionID); // push
        // exception
        // type
        generateCSACall(precomputed.csa_generateThrowable_index,
                precomputed.csa_generateThrowable_desc, GPRsToSave, FPRsToSave);
        xasm.setBranchTarget(b0);
    }
    protected void generateNullCheckForMonitor(int receiver_reg, int[] GPRsToSave,
            int[] FPRsToSave) {
        if (OMIT_NULLPOINTER_CHECKS_MONITOR)
            return;
        xasm.emit_cmpi(CR7, false, receiver_reg, 0);
        Branch b0 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R6, 0); // push meta
        xasm.emit_li32(R5, precomputed.nullPointerExceptionID); // push
        // exception
        // type
        generateCSACall(precomputed.csa_generateThrowable_index,
                precomputed.csa_generateThrowable_desc, GPRsToSave, FPRsToSave);
        xasm.setBranchTarget(b0);
    }

    private void generateArrayBoundCheck(int array_reg, int index_reg, int[] GPRsToSave,
            int[] FPRsToSave) {
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
        xasm.emit_li32(R6, 0);                                   // push meta
        xasm.emit_li32(R5, precomputed.arrayIndexOutOfBoundsExceptionID); // push exception type 
        generateCSACall(precomputed.csa_generateThrowable_index, 
                precomputed.csa_generateThrowable_desc, GPRsToSave, FPRsToSave);
        xasm.setBranchTarget(b0);
    }
    private void generateArrayStoreCheck(int array_reg, int elem_reg, int[] GPRsToSave,
            int[] FPRsToSave) {
        if (OMIT_ARRAYSTORE_CHECKS) return;

        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R5, array_reg));
        xasm.emit_lwz(R4, R5, precomputed.offset_componentbp_in_arraybp);
        xasm.emit_cmpi(CR7, false, elem_reg, 0);
        Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R3, elem_reg));
	xasm.emit_cmp(CR7, false, R3, R4);
	Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
        generateCFunctionCall("is_subtype_of", GPRsToSave, FPRsToSave);
        xasm.emit_cmpi(CR7, false, R3, 0);
        Branch b1 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);

        // check failed (throw exception)
        xasm.emit_li32(R6, 0);                                 // push meta
        xasm.emit_li32(R5, precomputed.arrayStoreExceptionID); // push exception type 
        generateCSACall(precomputed.csa_generateThrowable_index, 
                precomputed.csa_generateThrowable_desc, GPRsToSave, FPRsToSave);

	// blueprints are the same
	xasm.setBranchTarget(b2);

        // null is ok
        xasm.setBranchTarget(b0);

        // subtype test passed
        xasm.setBranchTarget(b1);
    }
    
    private void generateDivisionByIntZeroCheck(int divisor_reg,
            int[] GPRsToSave, int[] FPRsToSave) {
        if (OMIT_DIVISION_BY_ZERO_CHECKS) return;
        xasm.emit_cmpi(CR7, false, divisor_reg, 0);
        Branch b0 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R6, 0); // push meta
        xasm.emit_li32(R5, precomputed.arithmeticExceptionID); // push
        // exception
        // type
        generateCSACall(precomputed.csa_generateThrowable_index,
                precomputed.csa_generateThrowable_desc, GPRsToSave, FPRsToSave);
        xasm.setBranchTarget(b0);
    }

    private void generateDivisionByLongZeroCheck(int divisor_h_reg,
            int divisor_l_reg, int[] GPRsToSave, int[] FPRsToSave) {
        if (OMIT_DIVISION_BY_ZERO_CHECKS) return;
        xasm.emit_or(R6, divisor_h_reg, divisor_l_reg);
        xasm.emit_cmpi(CR7, false, R6, 0);
        Branch b0 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
        xasm.emit_li32(R6, 0); // push meta
        xasm.emit_li32(R5, precomputed.arithmeticExceptionID); // push
        // exception
        // type
        generateCSACall(precomputed.csa_generateThrowable_index,
                precomputed.csa_generateThrowable_desc, GPRsToSave, FPRsToSave, R7);
        xasm.setBranchTarget(b0);
    }

    private void generateArithmeticException(int[] GPRsToSave, int[] FPRsToSave) {
        xasm.emit_li32(R6, 0); // push meta
        xasm.emit_li32(R5, precomputed.arithmeticExceptionID); // push
        // exception
        // type
        generateCSACall(precomputed.csa_generateThrowable_index,
                precomputed.csa_generateThrowable_desc, GPRsToSave, FPRsToSave);
    }

    private void generateNullPointerException(int[] GPRsToSave, int[] FPRsToSave) {
        xasm.emit_li32(R6, 0); // push meta
        xasm.emit_li32(R5, precomputed.nullPointerExceptionID); // push
        // exception
        // type
        generateCSACall(precomputed.csa_generateThrowable_index,
                precomputed.csa_generateThrowable_desc, GPRsToSave, FPRsToSave);
    }

    public void visit(Instruction i) {
        throw new Error("Unimplemented instruction " + i);
        // generateCFunctionCall("hit_unrewritten_code");
    }

    public void visit(Instruction.NOP _) {
    }

    public void visit(Instruction.AFIAT _) {
    }

    public void visit(Instruction.PrimFiat _) {
    }

    public void visit(Instruction.WidePrimFiat _) {
    }

    public void visit(Instruction.POLLCHECK i) {
        if (OMIT_POLLCHECKS) return;
        // need to save registers before and after
        if (OPTIMIZE_POLLCHECK) {
            int eventUnionIndex = compilerVMInterface
                    .getRuntimeFunctionIndex("eventUnion");
            if (eventUnionIndex == -1)
                throw new OVMError();
            // hand inlinig of eventPollcheck()
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                xasm.emit_li32(S0, precomputed.runtimeFunctionTableHandle.asInt());
                xasm.emit_lwz(S0, S0, 0);
                xasm.emit_lwz(S1, S0, eventUnionIndex * 4);
                xasm.emit_lwz(S0, S1, 0);
                xasm.emit_cmpi(CR7, false, S0, 0);
                Branch b1 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
                // Event occurred
                xasm.emit_li32(S0, 0x00010001);
                xasm.emit_stw(S0, S1, 0);
                generateCSACall(precomputed.csa_pollingEventHook_index,
                        precomputed.csa_pollingEventHook_desc, null, null);
                xasm.setBranchTarget(b1);
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        } else {
            generateCFunctionCall("eventPollcheck", null, null);
            xasm.emit_cmpi(CR7, false, R3, 0);
            Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
            generateCSACall(precomputed.csa_pollingEventHook_index,
                    precomputed.csa_pollingEventHook_desc, null, null);
            xasm.setBranchTarget(b1);
        }
    }

    public void visit(Instruction.ACONST_NULL i) {
        vstack.push(new VStack.Constant(TypeCodes.INT, new Integer(0)));
    }

    // ICONST_X, BIPUSH, SIPUSH,
    public void visit(Instruction.IConstantLoad i) {
        vstack.push(new VStack.Constant(TypeCodes.INT, new Integer(i
                .getValue(buf))));
    }

    public void visit(Instruction.FConstantLoad i) {
        vstack.push(new VStack.Constant(TypeCodes.FLOAT, new Float(i
                .getFValue(buf))));
    }

    public void visit(Instruction.LConstantLoad i) {
        vstack.push(new VStack.Constant(TypeCodes.LONG, new Long(i
                .getLValue(buf))));
    }

    public void visit(Instruction.DConstantLoad i) {
        vstack.push(new VStack.Constant(TypeCodes.DOUBLE, new Double(i
                .getDValue(buf))));
    }

    public void visit(Instruction.LocalRead i) {
        int index = i.getLocalVariableOffset(buf);
        char type = i.getTypeCode();

        vstack.push(new VStack.Local(type, index));
    }

    public void visit(Instruction.LocalWrite i) {
        VStack.Item item = vstack.pop();
        char dtype = i.getTypeCode();
        int index = i.getLocalVariableOffset(buf);
        int dreg = registers.l2r(index);
        int dLocalOffset = stackLayout.getLocalVariableNativeOffset(index)
                + stackLayout.getNativeFrameSize();

        boolean[] liveness = registers.getLivenessAt(intrinsicPC + currInstrSize);
	if (! liveness[index]) {
	    return;
	}

        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            if (index == local.index()) {
                return; // redundant copy
            }
            char stype = local.type();
            int sreg = registers.l2r(local.index());
            int sLocalOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            if (dreg > 0) {
                if (sreg > 0) {
                    switch (dtype) {
                    case TypeCodes.INT:
                    case TypeCodes.REFERENCE:
                        switch(stype) {
                        case TypeCodes.INT:
                        case TypeCodes.REFERENCE:
                            xasm.emit_mr(dreg, sreg);
                            break;
                        case TypeCodes.FLOAT:
                            xasm.emit_stfs(sreg, SP, sLocalOffset);
                            xasm.emit_lwz(dreg, SP, sLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    case TypeCodes.FLOAT:
                        switch(stype) {
                        case TypeCodes.INT:
                        case TypeCodes.REFERENCE:
                            xasm.emit_stw(sreg, SP, sLocalOffset);
                            xasm.emit_lfs(dreg, SP, sLocalOffset);
                            break;
                        case TypeCodes.FLOAT:
                            xasm.emit_fmr(dreg, sreg);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    case TypeCodes.DOUBLE:
                        switch(stype) {
                        case TypeCodes.DOUBLE:
                            xasm.emit_fmr(dreg, sreg);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    default:
                        throw new Error();
                    }
                } else {
                    switch (dtype) {
                    case TypeCodes.INT:
                    case TypeCodes.REFERENCE:
                        switch(stype) {
                        case TypeCodes.INT:
                        case TypeCodes.REFERENCE:
                            xasm.emit_lwz(dreg, SP, sLocalOffset);
                            break;
                        case TypeCodes.FLOAT:
                            xasm.emit_lfs(dreg, SP, sLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    case TypeCodes.FLOAT:
                        switch(stype) {
                        case TypeCodes.INT:
                        case TypeCodes.REFERENCE:
                        case TypeCodes.FLOAT:
                            xasm.emit_lfs(dreg, SP, sLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    case TypeCodes.DOUBLE:
                        switch(stype) {
                        case TypeCodes.DOUBLE:
                        case TypeCodes.LONG:
                            xasm.emit_lfd(sreg, SP, dLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    default:
                        throw new Error();
                    }
                }
            } else {
                if (sreg > 0) {
                    switch (dtype) {
                    case TypeCodes.INT:
                    case TypeCodes.REFERENCE:
                        switch(stype) {
                        case TypeCodes.INT:
                        case TypeCodes.REFERENCE:
                            xasm.emit_stw(sreg, SP, dLocalOffset);
                            break;
                        case TypeCodes.FLOAT:
                            xasm.emit_stfs(sreg, SP, dLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    case TypeCodes.LONG:
                        switch(stype) {
                        case TypeCodes.DOUBLE:
                            xasm.emit_stfd(sreg, SP, dLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    case TypeCodes.FLOAT:
                        switch(stype) {
                        case TypeCodes.INT:
                        case TypeCodes.REFERENCE:
                            xasm.emit_stw(sreg, SP, dLocalOffset);
                            break;
                        case TypeCodes.FLOAT:
                            xasm.emit_stfs(sreg, SP, dLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    case TypeCodes.DOUBLE:
                        switch(stype) {
                        case TypeCodes.DOUBLE:
                            xasm.emit_stfd(sreg, SP, dLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }
                        break;
                    default:
                        throw new Error();
                    }
                } else {
                    switch (dtype) {
                    case TypeCodes.INT:
                    case TypeCodes.REFERENCE: {
                        int scratch = Registers.getScratchGPR();
                        try {
                            switch(stype) {
                            case TypeCodes.INT:
                            case TypeCodes.REFERENCE:
                            case TypeCodes.FLOAT:
                                xasm.emit_lwz(scratch, SP, sLocalOffset);
                                xasm.emit_stw(scratch, SP, dLocalOffset);
                                break;
                            default:
                                throw new Error();
                            }
                        } finally {
                            Registers.returnScratchGPR(scratch);
                        }
                        break;
                    }
                    case TypeCodes.LONG: {
                        int scratch = Registers.getScratchGPR();
                        try {
                            switch(stype) {
                            case TypeCodes.DOUBLE:
                            case TypeCodes.LONG:
                                xasm.emit_lwz(scratch, SP, sLocalOffset);
                                xasm.emit_stw(scratch, SP, dLocalOffset);
                                xasm.emit_lwz(scratch, SP, sLocalOffset + 4);
                                xasm.emit_stw(scratch, SP, dLocalOffset + 4);
                                break;
                            default:
                                throw new Error();
                            }
                        } finally {
                            Registers.returnScratchGPR(scratch);
                        }
                        break;
                    }
                    case TypeCodes.FLOAT: {
                        int scratch = Registers.getScratchFPR();
                        try {
                            switch(stype) {
                            case TypeCodes.INT:
                            case TypeCodes.REFERENCE:
                            case TypeCodes.FLOAT:
                                xasm.emit_lfs(scratch, SP, sLocalOffset);
                                xasm.emit_stfs(scratch, SP, dLocalOffset);
                                break;
                            default:
                                throw new Error();
                            }
                        } finally {
                            Registers.returnScratchFPR(scratch);
                        }
                        break;
                    }
                    case TypeCodes.DOUBLE: {
                        int scratch = Registers.getScratchFPR();
                        try {
                            switch(stype) {
                            case TypeCodes.DOUBLE:
                            case TypeCodes.LONG:
                                xasm.emit_lfd(scratch, SP, sLocalOffset);
                                xasm.emit_stfd(scratch, SP, dLocalOffset);
                                break;
                            default:
                                throw new Error();
                            }
                        } finally {
                            Registers.returnScratchFPR(scratch);
                        }
                        break;
                    }
                    default:
                        throw new Error();
                    }
                }
            }
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            char stype = constant.type();
            if (dreg > 0) {
                switch (dtype) {
                case TypeCodes.INT:
                case TypeCodes.REFERENCE:
                    switch(stype) {
                    case TypeCodes.INT:
                    case TypeCodes.REFERENCE:
                        xasm.emit_li32(dreg, constant.intValue());
                        break;
                    case TypeCodes.FLOAT:
                        xasm.emit_li32(dreg, Float.floatToIntBits(constant.floatValue()));
                        break;
                    default:
                        throw new Error();
                    }
                    break;
                case TypeCodes.FLOAT: {
                    switch(stype) {
                    case TypeCodes.INT:
                    case TypeCodes.REFERENCE: {
                        int scratch = Registers.getScratchGPR();
                        try {
                            xasm.emit_li32(scratch, constant.intValue());
                            xasm.emit_stw(scratch, SP, dLocalOffset);
                            xasm.emit_lfs(dreg, SP, dLocalOffset);
                        } finally {
                            Registers.returnScratchGPR(scratch);
                        }
                        break;
                    }
                    case TypeCodes.FLOAT: {
                        int scratch = Registers.getScratchGPR();
                        try {
                            xasm.emit_li32(scratch, Float.floatToIntBits(constant.floatValue()));
                            xasm.emit_stw(scratch, SP, dLocalOffset);
                            xasm.emit_lfs(dreg, SP, dLocalOffset);
                        } finally {
                            Registers.returnScratchGPR(scratch);
                        }
                        break;
                    }
                    default:
                        throw new Error();
                    }
                    break;
                }
                case TypeCodes.DOUBLE: {
                    int S0 = Registers.getScratchGPR();
                    int S1 = Registers.getScratchGPR();
                    try {
                        switch(stype) {
                        case TypeCodes.DOUBLE: {
                            long bits = Double.doubleToLongBits(constant
                                    .doubleValue());
                            xasm.emit_li32(S0, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_li32(S1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset + 4);
                            xasm.emit_stw(S1, SP, dLocalOffset);
                            xasm.emit_lfd(dreg, SP, dLocalOffset);
                            break;
                        }
                        case TypeCodes.LONG: {
                            long bits = constant.longValue();
                            xasm.emit_li32(S0, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_li32(S1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset + 4);
                            xasm.emit_stw(S1, SP, dLocalOffset);
                            xasm.emit_lfd(dreg, SP, dLocalOffset);
                            break;
                        }
                        default:
                            throw new Error();
                        }
                    } finally {
                        Registers.returnScratchGPR(S1);
                        Registers.returnScratchGPR(S0);
                    }
                    break;
                }
                default:
                    throw new Error();
                }
            } else {
                switch (dtype) {
                case TypeCodes.INT:
                case TypeCodes.REFERENCE: {
                    int scratch = Registers.getScratchGPR();
                    try {
                        switch(stype) {
                        case TypeCodes.INT:
                        case TypeCodes.REFERENCE:
                            xasm.emit_li32(scratch, constant.intValue());
                            xasm.emit_stw(scratch, SP, dLocalOffset);
                            break;
                        case TypeCodes.FLOAT:
                            xasm.emit_li32(scratch, Float.floatToIntBits(constant.floatValue()));
                            xasm.emit_stw(scratch, SP, dLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }
                    } finally {
                        Registers.returnScratchGPR(scratch);
                    }
                    break;
                }
                case TypeCodes.LONG: {
                    int S0 = Registers.getScratchGPR();
                    int S1 = Registers.getScratchGPR();
                    try {
                        switch(stype) {
                        case TypeCodes.DOUBLE: {
                            long value = Double.doubleToLongBits(constant
                                    .doubleValue());
                            xasm.emit_li32(S0, (int) (value & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset + 4);
                            xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset);
                            break;
                        }
                        case TypeCodes.LONG: {
                            long value = constant.longValue();
                            xasm.emit_li32(S0, (int) (value & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset + 4);
                            xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset);
                            break;
                        }
                        default:
                            throw new Error();
                        }
                    } finally {
                        Registers.returnScratchGPR(S1);
                        Registers.returnScratchGPR(S0);
                    }
                    break;
                }
                case TypeCodes.FLOAT: {
                    int scratch = Registers.getScratchGPR();
                    try {
                        switch(stype) {
                        case TypeCodes.INT:
                        case TypeCodes.REFERENCE:
                            xasm.emit_li32(scratch, constant.intValue());
                            xasm.emit_stw(scratch, SP, dLocalOffset);
                            break;
                        case TypeCodes.FLOAT:
                            xasm.emit_li32(scratch, Float.floatToIntBits(constant
                                    .floatValue()));
                            xasm.emit_stw(scratch, SP, dLocalOffset);
                            break;
                        default:
                            throw new Error();
                        }

                    } finally {
                        Registers.returnScratchGPR(scratch);
                    }
                    break;
                }
                case TypeCodes.DOUBLE: {
                    int S0 = Registers.getScratchGPR();
                    try {
                        switch(stype) {
                        case TypeCodes.DOUBLE: {
                            long bits = Double.doubleToLongBits(constant
                                    .doubleValue());
                            xasm.emit_li32(S0, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset + 4);
                            xasm.emit_li32(S0, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset);
                            break;
                        }
                        case TypeCodes.LONG: {
                            long bits = constant.longValue();
                            xasm.emit_li32(S0, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset + 4);
                            xasm.emit_li32(S0, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset);
                            break;
                        }
                        default:
                            throw new Error();
                        }
                    } finally {
                        Registers.returnScratchGPR(S0);
                    }
                    break;
                }
                default:
                    throw new Error();
                }
            }
        }
    }

    private InstructionGenerator_I_II ig_IADD = null;

    public void visit(Instruction.IADD i) {
	if (ig_IADD == null)
	    ig_IADD = new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                return s0 + s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_add(d, s0, s1);
            }
	};

	ig_IADD.generate();
    }

    private InstructionGenerator_J_JJ ig_LADD = null;
	
    public void visit(Instruction.LADD i) {
	if (ig_LADD == null)
	    ig_LADD = new InstructionGenerator_J_JJ() {
            long fold(long s0, long s1) {
                return s0 + s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1h, int s1l) {
                xasm.emit_addc(dl, s0l, s1l);
                xasm.emit_adde(dh, s0h, s1h);
            }
        };
	ig_LADD.generate();
    }

    private InstructionGenerator_F_FF ig_FADD = null;

    public void visit(Instruction.FADD i) {
	if (ig_FADD == null)
	    ig_FADD = new InstructionGenerator_F_FF() {
            float fold(float s0, float s1) {
                return s0 + s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fadds(d, s0, s1);
            }
        };

	ig_FADD.generate();
    }

    private InstructionGenerator_D_DD ig_DADD = null;

    public void visit(Instruction.DADD i) {
	if (ig_DADD == null)
	    ig_DADD =         new InstructionGenerator_D_DD() {
            double fold(double s0, double s1) {
                return s0 + s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fadd(d, s0, s1);
            }
        };

	ig_DADD.generate();
    }

    private InstructionGenerator_I_II ig_ISUB = null;

    public void visit(Instruction.ISUB i) {
	if (ig_ISUB == null)
	    ig_ISUB = new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                return s0 - s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_sub(d, s0, s1);
            }
        };
	ig_ISUB.generate();
    }

    private InstructionGenerator_J_JJ ig_LSUB = null;

    public void visit(Instruction.LSUB i) {
	if (ig_LSUB == null)
	    ig_LSUB = new InstructionGenerator_J_JJ() {
            long fold(long s0, long s1) {
                return s0 - s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1h, int s1l) {
                // all the registers are scratches, so they are not aliased
                xasm.emit_subc(dl, s0l, s1l);
                xasm.emit_sube(dh, s0h, s1h);
            }
        };
	ig_LSUB.generate();
    }

    private InstructionGenerator_F_FF ig_FSUB = null;

    public void visit(Instruction.FSUB i) {
	if (ig_FSUB == null)
	    ig_FSUB =         new InstructionGenerator_F_FF() {
            float fold(float s0, float s1) {
                return s0 - s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fsubs(d, s0, s1);
            }
        };

	ig_FSUB.generate();
    }

    private InstructionGenerator_D_DD ig_DSUB = null;

    public void visit(Instruction.DSUB i) {
	if (ig_DSUB == null)
	    ig_DSUB =         new InstructionGenerator_D_DD() {
            double fold(double s0, double s1) {
                return s0 - s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fsub(d, s0, s1);
            }
        };

	ig_DSUB.generate();
    }

    private InstructionGenerator_I_II ig_IMUL = null;

    public void visit(Instruction.IMUL i) {
	if (ig_IMUL == null)
	    ig_IMUL =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                return s0 * s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_mullw(d, s0, s1);
            }
        };

	ig_IMUL.generate();
    }

    private InstructionGenerator_J_JJ ig_LMUL = null;

    public void visit(Instruction.LMUL i) {
	if (ig_LMUL == null)
	    ig_LMUL =         new InstructionGenerator_J_JJ() {
            long fold(long s0, long s1) {
                return s0 * s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1h, int s1l) {
                // 1 2
                // x 3 4
                // ----------------
                // h(2x4) l(2x4)
                // l(1x4)
                // l(2x3)
                // + l(1x3)
                // ----------------
                // l(2x4)
                
                // all the registers are scratches, so they are not aliased
                xasm.emit_mulhwu(dh, s1l, s0l); // h(2 x 4)
                xasm.emit_mullw(dl, s1h, s0l); // l(1 x 4)
                xasm.emit_add(dl, dh, dl); // h(2x4) + l(1x4)
                xasm.emit_mullw(dh, s1l, s0h); // l(2 x 3)
                xasm.emit_add(dl, dl, dh); // h(2x4) + l(1x4) + l(2x3)
                xasm.emit_mullw(dh, s1h, s0h); // l(1 x 3)
                xasm.emit_add(dh, dl, dh); // h(2x4) + l(1x4) + l(2x3) + l(1x3)
                xasm.emit_mullw(dl, s1l, s0l); // l(2 x 4)
            }
        };

	ig_LMUL.generate();
    }

    private InstructionGenerator_F_FF ig_FMUL = null;

    public void visit(Instruction.FMUL i) {
	if (ig_FMUL == null)
	    ig_FMUL =         new InstructionGenerator_F_FF() {
            float fold(float s0, float s1) {
                return s0 * s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fmuls(d, s0, s1);
            }
        };

	ig_FMUL.generate();
    }

    private InstructionGenerator_D_DD ig_DMUL = null;

    public void visit(Instruction.DMUL i) {
	if (ig_DMUL == null)
	    ig_DMUL =         new InstructionGenerator_D_DD() {
            double fold(double s0, double s1) {
                return s0 * s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fmul(d, s0, s1);
            }
        };

	ig_DMUL.generate();
    }

    private InstructionGenerator_I_II ig_IDIV = null;

    public void visit(Instruction.IDIV i) {
	if (ig_IDIV == null)
	    ig_IDIV =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                if (s1 == 0) {
                    generateArithmeticException(null, null);
                    return -1;
                }
                return s0 / s1;
            }

            void emit(int d, int s0, int s1) {
                generateDivisionByIntZeroCheck(s1, null, null);
                xasm.emit_divw(d, s0, s1);
            }
        };

	ig_IDIV.generate_se();
    }

    private InstructionGenerator_J_JJ ig_LDIV = null;

    public void visit(Instruction.LDIV i) {
	if (ig_LDIV == null)
	    ig_LDIV =         new InstructionGenerator_J_JJ() {
            long fold(long s0, long s1) {
                if (s1 == 0L) {
                    generateArithmeticException(null, null);
                    return -1;
                }
                return s0 / s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1h, int s1l) {
                // all the registers are scratches, so they are not aliased
                generateDivisionByLongZeroCheck(s1h, s1l, null, null);
                xasm.emit_mr(R3, s0h);
                xasm.emit_mr(R4, s0l);
                xasm.emit_mr(R5, s1h);
                xasm.emit_mr(R6, s1l);
                generateCFunctionCall("ldiv", null, null, R7);
                xasm.emit_mr(dh, R3);
                xasm.emit_mr(dl, R4);
            }
        };

	ig_LDIV.generate_se();
    }

    private InstructionGenerator_F_FF ig_FDIV = null;

    public void visit(Instruction.FDIV i) {
	if (ig_FDIV == null)
	    ig_FDIV =         new InstructionGenerator_F_FF() {
            float fold(float s0, float s1) {
                return s0 / s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fdivs(d, s0, s1);
            }
        };

	ig_FDIV.generate();
    }

    private InstructionGenerator_D_DD ig_DDIV = null;

    public void visit(Instruction.DDIV i) {
	if (ig_DDIV == null)
	    ig_DDIV =         new InstructionGenerator_D_DD() {
            double fold(double s0, double s1) {
                return s0 / s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fdiv(d, s0, s1);
            }
        };

	ig_DDIV.generate();
    }

    private InstructionGenerator_I_II ig_IREM = null;

    public void visit(Instruction.IREM i) {
	if (ig_IREM == null)
	    ig_IREM =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                if (s1 == 0) {
                    generateArithmeticException(null, null);
                    return -1;
                }
                return s0 % s1;
            }

            void emit(int d, int s0, int s1) {
                generateDivisionByIntZeroCheck(s1, null, null);
                int S = Registers.getScratchGPR();
                try {
                    // s0, s1 and d can be aliased, so you must not
                    // modify d before s0 and s1 are both dead
                    xasm.emit_divw(S, s0, s1);
                    xasm.emit_mullw(S, S, s1);
                    xasm.emit_sub(d, s0, S);
                } finally {
                    Registers.returnScratchGPR(S);
                }
            }
        };

	ig_IREM.generate_se();
    }

    private InstructionGenerator_J_JJ ig_LREM = null;

    public void visit(Instruction.LREM i) {
	if (ig_LREM == null)
	    ig_LREM =         new InstructionGenerator_J_JJ() {
            long fold(long s0, long s1) {
                if (s1 == 0L) {
                    generateArithmeticException(null, null);
                    return -1L;
                }
                return s0 % s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1h, int s1l) {
                // all the registers are scratches, so they are not aliased
                generateDivisionByLongZeroCheck(s1h, s1l, null, null);
                xasm.emit_mr(R3, s0h);
                xasm.emit_mr(R4, s0l);
                xasm.emit_mr(R5, s1h);
                xasm.emit_mr(R6, s1l);
                generateCFunctionCall("lrem", null, null, R7);
                xasm.emit_mr(dh, R3);
                xasm.emit_mr(dl, R4);
            }
        };

	ig_LREM.generate_se();
    }

    private InstructionGenerator_F_FF ig_FREM = null;

    public void visit(Instruction.FREM i) {
	if (ig_FREM == null)
	    ig_FREM =         new InstructionGenerator_F_FF() {
            float fold(float s0, float s1) {
                return s0 % s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fmr(F1, s0);
                xasm.emit_fmr(F2, s1);
                generateCFunctionCall("fmod", null, null);
                xasm.emit_fmr(d, F1);
            }
        };

	ig_FREM.generate();
    }

    private InstructionGenerator_D_DD ig_DREM = null;

    public void visit(Instruction.DREM i) {
	if (ig_DREM == null)
	    ig_DREM =         new InstructionGenerator_D_DD() {
            double fold(double s0, double s1) {
                return s0 % s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fmr(F1, s0);
                xasm.emit_fmr(F2, s1);
                generateCFunctionCall("fmod", null, null);
                xasm.emit_fmr(d, F1);
            }
        };

	ig_DREM.generate();
    }

    private InstructionGenerator_I_I ig_INEG = null;

    public void visit(Instruction.INEG i) {
	if (ig_INEG == null)
	    ig_INEG =         new InstructionGenerator_I_I() {
            int fold(int s0) {
                return -s0;
            }

            void emit(int d, int s0) {
                xasm.emit_neg(d, s0);
            }
        };

	ig_INEG.generate();
    }

    private InstructionGenerator_J_J ig_LNEG = null;

    public void visit(Instruction.LNEG i) {
	if (ig_LNEG == null)
	    ig_LNEG =         new InstructionGenerator_J_J() {
            long fold(long s0) {
                return -s0;
            }

            void emit(int dh, int dl, int s0h, int s0l) {
                // all the registers are scratches, so they are not aliased
                int S = Registers.getScratchGPR();
                try {
                    xasm.emit_xor(S, S, S); // S = 0
                    xasm.emit_subc(dl, S, s0l);
                    xasm.emit_sube(dh, S, s0h);
                } finally {
                    Registers.returnScratchGPR(S);
                }
            }
        };

	ig_LNEG.generate();
    }

    private InstructionGenerator_F_F ig_FNEG = null;

    public void visit(Instruction.FNEG i) {
	if (ig_FNEG == null)
	    ig_FNEG =         new InstructionGenerator_F_F() {
            float fold(float s0) {
                return -s0;
            }

            void emit(int d, int s0) {
                xasm.emit_fneg(d, s0);
            }
        };

	ig_FNEG.generate();
    }

    private InstructionGenerator_D_D ig_DNEG = null;

    public void visit(Instruction.DNEG i) {
	if (ig_DNEG == null)
	    ig_DNEG =         new InstructionGenerator_D_D() {
            double fold(double s0) {
                return -s0;
            }

            void emit(int d, int s0) {
                xasm.emit_fneg(d, s0);
            }
        };

	ig_DNEG.generate();
    }

    private InstructionGenerator_I_II ig_ISHL = null;

    public void visit(Instruction.ISHL i) {
	if (ig_ISHL == null)
	    ig_ISHL =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                return s0 << s1;
            }

            void emit(int d, int s0, int s1) {
                int S0 = Registers.getScratchGPR();
                try {
                    xasm.emit_andi(S0, s1, 0x1F);
                    xasm.emit_slw(d, s0, S0);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
        };

	ig_ISHL.generate();
    }

    private InstructionGenerator_J_JI ig_LSHL = null;

    public void visit(Instruction.LSHL i) {
	if (ig_LSHL == null)
	    ig_LSHL =         new InstructionGenerator_J_JI() {
            long fold(long s0, int s1) {
                return s0 << s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1) {
                // all the registers are scratches, so they are not aliased
                int S0 = Registers.getScratchGPR();
                try {
                    xasm.emit_andi(S0, s1, 0x3F);
                    xasm.emit_addicd(dl, S0, 0xFFFFFFE0);
                    Branch b0 = xasm.emit_bc_d(BO_TRUE, CR0_LT);
                    xasm.emit_slw(dh, s0l, dl);
                    xasm.emit_li32(dl, 0);
                    Branch b1 = xasm.emit_b_d();
                    xasm.setBranchTarget(b0);
                    xasm.emit_rlwinm(dl, s0l, 31, 1, 31);
                    xasm.emit_subfic(dh, S0, 0x1F);
                    xasm.emit_srw(dl, dl, dh);
                    xasm.emit_slw(dh, s0h, S0);
                    xasm.emit_or(dh, dl, dh);
                    xasm.emit_slw(dl, s0l, S0);
                    xasm.setBranchTarget(b1);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
        };

	ig_LSHL.generate();
    }

    private InstructionGenerator_I_II ig_ISHR = null;

    public void visit(Instruction.ISHR i) {
	if (ig_ISHR == null)
	    ig_ISHR =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                return s0 >> s1;
            }

            void emit(int d, int s0, int s1) {
                int S0 = Registers.getScratchGPR();
                try {
                    xasm.emit_andi(S0, s1, 0x1F);
                    xasm.emit_sraw(d, s0, S0);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
        };

	ig_ISHR.generate();
    }

    private InstructionGenerator_J_JI ig_LSHR = null;

    public void visit(Instruction.LSHR i) {
	if (ig_LSHR == null)
	    ig_LSHR =         new InstructionGenerator_J_JI() {
            long fold(long s0, int s1) {
                return s0 >> s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1) {
                // all the registers are scratches, so they are not aliased
                int S0 = Registers.getScratchGPR();
                try {
                    xasm.emit_andi(S0, s1, 0x3F);
                    xasm.emit_addicd(dh, S0, 0xFFFFFFE0);
                    Branch b0 = xasm.emit_bc_d(BO_TRUE, CR0_LT);
                    xasm.emit_sraw(dl, s0h, dh);
                    xasm.emit_srawi(dh, s0h, 31);
                    Branch b1 = xasm.emit_b_d();
                    xasm.setBranchTarget(b0);
                    xasm.emit_rlwinm(dh, s0h, 1, 0, 30);
                    xasm.emit_subfic(dl, S0, 0x1F);
                    xasm.emit_slw(dh, dh, dl);
                    xasm.emit_srw(dl, s0l, S0);
                    xasm.emit_or(dl, dh, dl);
                    xasm.emit_sraw(dh, s0h, S0);
                    xasm.setBranchTarget(b1);
                } finally {
                    Registers.returnScratchGPR(S0);
                }

            }
        };

	ig_LSHR.generate();
    }

    private InstructionGenerator_I_II ig_IUSHR = null;

    public void visit(Instruction.IUSHR i) {
	if (ig_IUSHR == null)
	    ig_IUSHR =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                return s0 >>> s1;
            }

            void emit(int d, int s0, int s1) {
                int S0 = Registers.getScratchGPR();
                try {
                    xasm.emit_andi(S0, s1, 0x1F);
                    xasm.emit_srw(d, s0, S0);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
        };

	ig_IUSHR.generate();
    }

    private InstructionGenerator_J_JI ig_LUSHR = null;

    public void visit(Instruction.LUSHR i) {
	if (ig_LUSHR == null)
	    ig_LUSHR =         new InstructionGenerator_J_JI() {
            long fold(long s0, int s1) {
                return s0 >>> s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1) {
                // all the registers are scratches, so they are not aliased
                int S0 = Registers.getScratchGPR();
                try {
                    xasm.emit_andi(S0, s1, 0x3F);
                    xasm.emit_addicd(dh, S0, 0xFFFFFFE0);
                    Branch b0 = xasm.emit_bc_d(BO_TRUE, CR0_LT);
                    xasm.emit_srw(dl, s0h, dh);
                    xasm.emit_li32(dh, 0);
                    Branch b1 = xasm.emit_b_d();
                    xasm.setBranchTarget(b0);
                    xasm.emit_rlwinm(dh, s0h, 1, 0, 30);
                    xasm.emit_subfic(dl, S0, 0x1F);
                    xasm.emit_slw(dh, dh, dl);
                    xasm.emit_srw(dl, s0l, S0);
                    xasm.emit_or(dl, dh, dl);
                    xasm.emit_srw(dh, s0h, S0);
                    xasm.setBranchTarget(b1);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
        };

	ig_LUSHR.generate();
    }

    private InstructionGenerator_I_II ig_IAND = null;

    public void visit(Instruction.IAND i) {
	if (ig_IAND == null)
	    ig_IAND =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                return s0 & s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_and(d, s0, s1);
            }
        };

	ig_IAND.generate();
    }

    private InstructionGenerator_J_JJ ig_LAND = null;

    public void visit(Instruction.LAND i) {
	if (ig_LAND == null)
	    ig_LAND =         new InstructionGenerator_J_JJ() {
            long fold(long s0, long s1) {
                return s0 & s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1h, int s1l) {
//              all the registers are scratches, so they are not aliased
                xasm.emit_and(dl, s0l, s1l);
                xasm.emit_and(dh, s0h, s1h);
            }
        };

	ig_LAND.generate();
    }

    private InstructionGenerator_I_II ig_IOR = null;

    public void visit(Instruction.IOR i) {
	if (ig_IOR == null)
	    ig_IOR =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                return s0 | s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_or(d, s0, s1);
            }
        };

	ig_IOR.generate();
    }

    private InstructionGenerator_J_JJ ig_LOR = null;

    public void visit(Instruction.LOR i) {
	if (ig_LOR == null)
	    ig_LOR =         new InstructionGenerator_J_JJ() {
            long fold(long s0, long s1) {
                return s0 | s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1h, int s1l) {
//              all the registers are scratches, so they are not aliased
                xasm.emit_or(dl, s0l, s1l);
                xasm.emit_or(dh, s0h, s1h);
            }
        };

	ig_LOR.generate();
    }

    private InstructionGenerator_I_II ig_IXOR = null;

    public void visit(Instruction.IXOR i) {
	if (ig_IXOR == null)
	    ig_IXOR =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                return s0 ^ s1;
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_xor(d, s0, s1);
            }
        };

	ig_IXOR.generate();
    }

    private InstructionGenerator_J_JJ ig_LXOR = null;

    public void visit(Instruction.LXOR i) {
	if (ig_LXOR == null)
	    ig_LXOR =         new InstructionGenerator_J_JJ() {
            long fold(long s0, long s1) {
                return s0 ^ s1;
            }

            void emit(int dh, int dl, int s0h, int s0l, int s1h, int s1l) {
//              all the registers are scratches, so they are not aliased
                xasm.emit_xor(dl, s0l, s1l);
                xasm.emit_xor(dh, s0h, s1h);
            }
        };

	ig_LXOR.generate();
    }

    public void visit(Instruction.IINC i) {
        int index = i.getLocalVariableOffset(buf);
        int delta = i.getValue(buf);
        int localOffset = stackLayout.getLocalVariableNativeOffset(index)
            + stackLayout.getNativeFrameSize();
        int reg = registers.l2r(index);
        if (reg > 0) {
            xasm.emit_addi(reg, reg, delta);
        } else {
            int S0 = Registers.getScratchGPR();
            try {
                xasm.emit_lwz(S0, SP, localOffset);
                xasm.emit_addi(S0, S0, delta);
                xasm.emit_stw(S0, SP, localOffset);
            } finally {
                Registers.returnScratchGPR(S0);
            }
        }
    }

    private InstructionGenerator_J_I ig_I2L = null;

    public void visit(Instruction.I2L i) {
	if (ig_I2L == null)
	    ig_I2L =         new InstructionGenerator_J_I() {
            long fold(int s0) {
                return (long) s0;
            }

            void emit(int dh, int dl, int s0) {
                xasm.emit_mr(dl, s0);
                xasm.emit_srawi(dh, s0, 31);
            }
        };

	ig_I2L.generate();
    }

    private InstructionGenerator_F_I ig_I2F = null;

    public void visit(Instruction.I2F i) {
	if (ig_I2F == null)
	    ig_I2F =         new InstructionGenerator_F_I() {
            float fold(int s0) {
                return (float) s0;
            }

            void emit(int d, int s0) {
                int S0 = Registers.getScratchGPR();
                int S1 = Registers.getScratchGPR();
                int S2 = Registers.getScratchGPR();
                int S3 = Registers.getScratchFPR();
                int S4 = Registers.getScratchFPR();
                try {
                    int offset = stackLayout.getFPRegisterOffset(S3)
                    + stackLayout.getNativeFrameSize();
                    xasm.emit_xoris(S0, s0, 0x8000);
                    xasm.emit_li32(S1, 0x43300000);
                    xasm.emit_li32(S2, 0x80000000);
                    xasm.emit_stw(S1, SP, offset);
                    xasm.emit_stw(S0, SP, offset + 4);
                    xasm.emit_lfd(S3, SP, offset);
                    xasm.emit_stw(S1, SP, offset);
                    xasm.emit_stw(S2, SP, offset + 4);
                    xasm.emit_lfd(S4, SP, offset);
                    xasm.emit_fsub(d, S3, S4);
                    xasm.emit_frsp(d, d);
                } finally {
                    Registers.returnScratchFPR(S4);
                    Registers.returnScratchFPR(S3);
                    Registers.returnScratchGPR(S2);
                    Registers.returnScratchGPR(S1);
                    Registers.returnScratchGPR(S0);
                }
            }
        };

	ig_I2F.generate();
    }

    private InstructionGenerator_D_I ig_I2D = null;

    public void visit(Instruction.I2D i) {
	if (ig_I2D == null)
	    ig_I2D =         new InstructionGenerator_D_I() {
            double fold(int s0) {
                return (double) s0;
            }

            void emit(int d, int s0) {
                int offset = stackLayout.getFPRegisterOffset(d)
                        + stackLayout.getNativeFrameSize();
                int S0 = Registers.getScratchGPR();
                int S1 = Registers.getScratchGPR();
                int S2 = Registers.getScratchGPR();
                int S3 = Registers.getScratchFPR();
                int S4 = Registers.getScratchFPR();
                try {
                    xasm.emit_xoris(S0, s0, 0x8000);
                    xasm.emit_li32(S1, 0x43300000);
                    xasm.emit_li32(S2, 0x80000000);
                    xasm.emit_stw(S1, SP, offset);
                    xasm.emit_stw(S0, SP, offset + 4);
                    xasm.emit_lfd(S3, SP, offset);
                    xasm.emit_stw(S1, SP, offset);
                    xasm.emit_stw(S2, SP, offset + 4);
                    xasm.emit_lfd(S4, SP, offset);
                    xasm.emit_fsub(d, S3, S4);
                } finally {
                    Registers.returnScratchFPR(S4);
                    Registers.returnScratchFPR(S3);
                    Registers.returnScratchGPR(S2);
                    Registers.returnScratchGPR(S1);
                    Registers.returnScratchGPR(S0);
                }
            }
        };

	ig_I2D.generate();
    }

    private InstructionGenerator_I_J ig_L2I = null;

    public void visit(Instruction.L2I i) {
	if (ig_L2I == null)
	    ig_L2I =         new InstructionGenerator_I_J() {
            int fold(long s0) {
                return (int) s0;
            }

            void emit(int d, int s0h, int s0l) {
                xasm.emit_mr(d, s0l);
            }
        };

	ig_L2I.generate();
    }

    private InstructionGenerator_F_J ig_L2F = null;

    public void visit(Instruction.L2F i) {
	if (ig_L2F == null)
	    ig_L2F =         new InstructionGenerator_F_J() {
            float fold(long s0) {
                return (float) s0;
            }

            void emit(int d, int s0h, int s0l) {
                xasm.emit_mr(R3, s0h);
                xasm.emit_mr(F4, s0l);
                generateCFunctionCall("l2f", null, null);
                xasm.emit_fmr(d, F1);
            }
        };

	ig_L2F.generate();
    }

    private InstructionGenerator_D_J ig_L2D = null;

    public void visit(Instruction.L2D i) {
	if (ig_L2D == null)
	    ig_L2D =         new InstructionGenerator_D_J() {
            double fold(long s0) {
                return (double) s0;
            }

            void emit(int d, int s0h, int s0l) {
                xasm.emit_mr(R3, s0h);
                xasm.emit_mr(F4, s0l);
                generateCFunctionCall("l2d", null, null);
                xasm.emit_fmr(d, F1);
            }
        };

	ig_L2D.generate();
    }

    private InstructionGenerator_I_F ig_F2I = null;

    /*
     * IEEE compliance. The algorithm is the following: If the input
     * is equals to itself, it is not a NaN.  If it is a NaN, push
     * zero. If it is not and it is a positive or negative infinity,
     * push Integer.MAX_VALUE or MIN_VALUE, respectively. Otherwise,
     * do a normal conversion using HW.
     */
    public void visit(Instruction.F2I i) {
	if (ig_F2I == null)
	    ig_F2I =         new InstructionGenerator_I_F() {
            int fold(float s0) {
                return (int) s0;
            }

            void emit(int d, int s0) {
		xasm.emit_fmr(F1, s0);
                generateCFunctionCall("f2i", null, null);
                xasm.emit_mr(d, R3);
		/*
                int FS0 = Registers.getScratchFPR();
                int FS1 = Registers.getScratchFPR();
		int GS0 = Registers.getScratchGPR();
		int GS1 = Registers.getScratchGPR();
                try {
                    int offset0 = stackLayout.getFPRegisterOffset(FS0)
                            + stackLayout.getNativeFrameSize();
                    int offset1 = stackLayout.getFPRegisterOffset(FS1)
                            + stackLayout.getNativeFrameSize();

		    xasm.emit_fcmpu(CR7, s0, s0);
		    Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		    // NaN
		    xasm.emit_li32(d, 0);
		    Branch b2 = xasm.emit_b_d();

		    xasm.setBranchTarget(b1);
		    // Not NaN
		    xasm.emit_li32(GS0, Float.floatToIntBits(Float.POSITIVE_INFINITY));
		    xasm.emit_li32(GS1, Float.floatToIntBits(Float.NEGATIVE_INFINITY));
		    xasm.emit_stw(GS0, SP, offset0);
		    xasm.emit_stw(GS1, SP, offset1);
		    xasm.emit_lfs(FS0, SP, offset0);
		    xasm.emit_lfs(FS1, SP, offset1);

		    xasm.emit_fcmpu(CR7, s0, FS0);
		    Branch b3 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
		    // == POSITIVE_INFINITY
		    xasm.emit_li32(d, Integer.MAX_VALUE);
		    Branch b4 = xasm.emit_b_d();

		    // != POSITIVE_INFINITY
		    xasm.setBranchTarget(b3);
		    xasm.emit_fcmpu(CR7, s0, FS1);
		    Branch b5 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);

		    // == NEGATIVE_INFINITY
		    xasm.emit_li32(d, Integer.MIN_VALUE);
		    Branch b6 = xasm.emit_b_d();

		    // != NEGATIVE_INFINITY
		    xasm.setBranchTarget(b5);

		    xasm.emit_fctiw(FS0, s0);
		    xasm.emit_stfd(FS0, SP, offset0);
		    xasm.emit_lwz(d, SP, offset0 + 4);

		    xasm.setBranchTarget(b2);
		    xasm.setBranchTarget(b4);
		    xasm.setBranchTarget(b6);

                } finally {
                    Registers.returnScratchFPR(FS1);
                    Registers.returnScratchFPR(FS0);
                    Registers.returnScratchGPR(GS1);
                    Registers.returnScratchGPR(GS0);
                }
		*/
            }
        };

	ig_F2I.generate();
    }

    private InstructionGenerator_J_F ig_F2L = null;

    public void visit(Instruction.F2L i) {
	if (ig_F2L == null)
	    ig_F2L =         new InstructionGenerator_J_F() {
            long fold(float s0) {
                return (long) s0;
            }

            void emit(int dh, int dl, int s0) {
                xasm.emit_fmr(F1, s0);
                generateCFunctionCall("f2l", null, null);
                xasm.emit_mr(dh, R3);
                xasm.emit_mr(dl, R4);
            }
        };

	ig_F2L.generate();
    }

    private InstructionGenerator_D_F ig_F2D = null;

    public void visit(Instruction.F2D i) {
	if (ig_F2D == null)
	    ig_F2D =         new InstructionGenerator_D_F() {
            double fold(float s0) {
                return (double) s0;
            }

            void emit(int d, int s0) {
                xasm.emit_fmr(d, s0);
            }
        };

	ig_F2D.generate();
    }

    private InstructionGenerator_I_D ig_D2I = null;

    public void visit(Instruction.D2I i) {
	if (ig_D2I == null)
	    ig_D2I =         new InstructionGenerator_I_D() {
            int fold(double s0) {
                return (int) s0;
            }

            void emit(int d, int s0) {
                xasm.emit_fmr(F1, s0);
                generateCFunctionCall("d2i", null, null);
                xasm.emit_mr(d, R3);
		/*
                int FS0 = Registers.getScratchFPR();
                int FS1 = Registers.getScratchFPR();
                int GS0 = Registers.getScratchGPR();
                int GS1 = Registers.getScratchGPR();
                int GS2 = Registers.getScratchGPR();
                int GS3 = Registers.getScratchGPR();
                try {
                    int offset0 = stackLayout.getFPRegisterOffset(FS0)
                            + stackLayout.getNativeFrameSize();
                    int offset1 = stackLayout.getFPRegisterOffset(FS1)
                            + stackLayout.getNativeFrameSize();

		    xasm.emit_fcmpu(CR7, s0, s0);
		    Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		    // NaN
		    xasm.emit_li32(d, 0);
		    Branch b2 = xasm.emit_b_d();

		    xasm.setBranchTarget(b1);
		    // Not NaN
		    long dpi = Double.doubleToLongBits(Double.POSITIVE_INFINITY);
		    long dni = Double.doubleToLongBits(Double.NEGATIVE_INFINITY);
		    int dpil = (int)(dpi & 0x00000000FFFFFFFFL);
		    int dpih = (int)((dpi >> 32) & 0x00000000FFFFFFFFL);
		    int dnil = (int)(dni & 0x00000000FFFFFFFFL);
		    int dnih = (int)((dni >> 32) & 0x00000000FFFFFFFFL);
		    xasm.emit_li32(GS0, dpil);
		    xasm.emit_li32(GS1, dpih);
		    xasm.emit_li32(GS2, dnil);
		    xasm.emit_li32(GS3, dnih);

		    xasm.emit_stw(GS0, SP, offset0 + 4);
		    xasm.emit_stw(GS1, SP, offset0);
		    xasm.emit_stw(GS2, SP, offset1 + 4);
		    xasm.emit_stw(GS3, SP, offset1);

		    xasm.emit_lfd(FS0, SP, offset0); // PI
		    xasm.emit_lfd(FS1, SP, offset1); // NI

		    xasm.emit_fcmpu(CR7, s0, FS0);
		    Branch b3 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
		    // == POSITIVE_INFINITY
		    xasm.emit_li32(d, Integer.MAX_VALUE);
		    Branch b4 = xasm.emit_b_d();

		    // != POSITIVE_INFINITY
		    xasm.setBranchTarget(b3);
		    xasm.emit_fcmpu(CR7, s0, FS1);
		    Branch b5 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);

		    // == NEGATIVE_INFINITY
		    xasm.emit_li32(d, Integer.MIN_VALUE);
		    Branch b6 = xasm.emit_b_d();

		    // != NEGATIVE_INFINITY
		    xasm.setBranchTarget(b5);

		    xasm.emit_fctiw(FS0, s0);
		    xasm.emit_stfd(FS0, SP, offset0);
		    xasm.emit_lwz(d, SP, offset0 + 4);

		    xasm.setBranchTarget(b2);
		    xasm.setBranchTarget(b4);
		    xasm.setBranchTarget(b6);

                } finally {
                    Registers.returnScratchFPR(FS1);
                    Registers.returnScratchFPR(FS0);
                    Registers.returnScratchGPR(GS3);
                    Registers.returnScratchGPR(GS2);
                    Registers.returnScratchGPR(GS1);
                    Registers.returnScratchGPR(GS0);
                }
		*/
            }
        };

	ig_D2I.generate();
    }

    private InstructionGenerator_J_D ig_D2L = null;

    public void visit(Instruction.D2L i) {
	if (ig_D2L == null)
	    ig_D2L =         new InstructionGenerator_J_D() {
            long fold(double s0) {
                return (long) s0;
            }

            void emit(int dh, int dl, int s0) {
                xasm.emit_fmr(F1, s0);
                generateCFunctionCall("d2l", null, null);
                xasm.emit_mr(dh, R3);
                xasm.emit_mr(dl, R4);
            }
        };

	ig_D2L.generate();
    }

    private InstructionGenerator_F_D ig_D2F = null;

    public void visit(Instruction.D2F i) {
	if (ig_D2F == null)
	    ig_D2F =         new InstructionGenerator_F_D() {
            float fold(double s0) {
                return (float) s0;
            }

            void emit(int d, int s0) {
                xasm.emit_frsp(d, s0);
            }
        };

	ig_D2F.generate();
    }

    private InstructionGenerator_I_I ig_I2B = null;

    public void visit(Instruction.I2B i) {
	if (ig_I2B == null)
	    ig_I2B =         new InstructionGenerator_I_I() {
            int fold(int s0) {
                return (int) (byte) s0;
            }

            void emit(int d, int s0) {
                xasm.emit_extsb(d, s0);
            }
        };

	ig_I2B.generate();
    }

    private InstructionGenerator_I_I ig_I2C = null;

    public void visit(Instruction.I2C i) {
	if (ig_I2C == null)
	    ig_I2C =         new InstructionGenerator_I_I() {
            int fold(int s0) {
                return (int) (char) s0;
            }

            void emit(int d, int s0) {
                xasm.emit_rlwinm(d, s0, 0, 16, 31);
            }
        };

	ig_I2C.generate();
    }

    private InstructionGenerator_I_I ig_I2S = null;

    public void visit(Instruction.I2S i) {
	if (ig_I2S == null)
	    ig_I2S =         new InstructionGenerator_I_I() {
            int fold(int s0) {
                return (int) (short) s0;
            }

            void emit(int d, int s0) {
                xasm.emit_extsh(d, s0);
            }
        };

	ig_I2S.generate();
    }

    private InstructionGenerator_I_JJ ig_LCMP = null;

    public void visit(Instruction.LCMP i) {
	if (ig_LCMP == null)
	    ig_LCMP =         new InstructionGenerator_I_JJ() {
            int fold(long s0, long s1) {
                if (s0 > s1) {
                    return 1;
                } else if (s0 == s1) {
                    return 0;
                } else {
                    return -1;
                }
            }

            void emit(int d, int s0h, int s0l, int s1h, int s1l) {
                xasm.emit_cmp(CR7, false, s0l, s1l);
                Branch b1 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
                xasm.emit_cmp(CR7, false, s0h, s1h);
                Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
                xasm.emit_li32(d, 0);
                Branch b3 = xasm.emit_b_d();
                xasm.setBranchTarget(b1);
                xasm.setBranchTarget(b2);
                xasm.emit_cmp(CR7, false, s1h, s0h);
                Branch b4 = xasm.emit_bc_d(BO_TRUE, CR7_GT);
                Branch b5 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
                xasm.emit_cmpl(CR7, false, s1l, s0l);
                Branch b6 = xasm.emit_bc_d(BO_FALSE, CR7_GT);
                xasm.setBranchTarget(b4);
                xasm.emit_li32(d, -1);
                Branch b7 = xasm.emit_b_d();
                xasm.setBranchTarget(b5);
                xasm.setBranchTarget(b6);
                xasm.emit_li32(d, 1);
                xasm.setBranchTarget(b3);
                xasm.setBranchTarget(b7);
            }
        };

	ig_LCMP.generate();
    }

    private InstructionGenerator_I_FF ig_FCMPL = null;

    public void visit(Instruction.FCMPL i) {
	if (ig_FCMPL == null)
	    ig_FCMPL =         new InstructionGenerator_I_FF() {
            int fold(float s0, float s1) {
                if (s0 > s1) {
                    return 1;
                } else if (s0 == s1) {
                    return 0;
                } else {
                    return -1;
                }
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fcmpu(CR7, s0, s1);
                Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_SO_OR_FU);
                Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
                xasm.emit_li32(d, 0);
                Branch b3 = xasm.emit_b_d();
                xasm.setBranchTarget(b2);
                xasm.emit_fcmpu(CR7, s0, s1);
                Branch b4 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
                xasm.setBranchTarget(b1);
                xasm.emit_li32(d, -1);
                Branch b5 = xasm.emit_b_d();
                xasm.setBranchTarget(b4);
                xasm.emit_li32(d, 1);
                xasm.setBranchTarget(b3);
                xasm.setBranchTarget(b5);
            }
        };

	ig_FCMPL.generate();
    }

    private InstructionGenerator_I_FF ig_FCMPG = null;

    public void visit(Instruction.FCMPG i) {
	if (ig_FCMPG == null)
	    ig_FCMPG =         new InstructionGenerator_I_FF() {
            int fold(float s0, float s1) {
                if (s0 > s1) {
                    return 1;
                } else if (s0 == s1) {
                    return 0;
                } else {
                    return -1;
                }
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fcmpu(CR7, s0, s1);
                Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_SO_OR_FU);
                Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
                xasm.emit_li32(d, 0);
                Branch b3 = xasm.emit_b_d();
                xasm.setBranchTarget(b2);
                xasm.emit_fcmpu(CR7, s0, s1);
                Branch b4 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
                xasm.emit_li32(d, -1);
                Branch b5 = xasm.emit_b_d();
                xasm.setBranchTarget(b1);
                xasm.setBranchTarget(b4);
                xasm.emit_li32(d, 1);
                xasm.setBranchTarget(b3);
                xasm.setBranchTarget(b5);
            }
        };

	ig_FCMPG.generate();
    }

    private InstructionGenerator_I_DD ig_DCMPL = null;

    public void visit(Instruction.DCMPL i) {
	if (ig_DCMPL == null)
	    ig_DCMPL =         new InstructionGenerator_I_DD() {
            int fold(double s0, double s1) {
                if (s0 > s1) {
                    return 1;
                } else if (s0 == s1) {
                    return 0;
                } else {
                    return -1;
                }
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fcmpu(CR7, s0, s1);
                Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_SO_OR_FU);
                Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
                xasm.emit_li32(d, 0);
                Branch b3 = xasm.emit_b_d();
                xasm.setBranchTarget(b2);
                xasm.emit_fcmpu(CR7, s0, s1);
                Branch b4 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
                xasm.setBranchTarget(b1);
                xasm.emit_li32(d, -1);
                Branch b5 = xasm.emit_b_d();
                xasm.setBranchTarget(b4);
                xasm.emit_li32(d, 1);
                xasm.setBranchTarget(b3);
                xasm.setBranchTarget(b5);
            }
        };

	ig_DCMPL.generate();
    }

    private InstructionGenerator_I_DD ig_DCMPG = null;

    public void visit(Instruction.DCMPG i) {
	if (ig_DCMPG == null)
	    ig_DCMPG =         new InstructionGenerator_I_DD() {
            int fold(double s0, double s1) {
                if (s0 > s1) {
                    return 1;
                } else if (s0 == s1) {
                    return 0;
                } else {
                    return -1;
                }
            }

            void emit(int d, int s0, int s1) {
                xasm.emit_fcmpu(CR7, s0, s1);
                Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_SO_OR_FU);
                Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_EQ);
                xasm.emit_li32(d, 0);
                Branch b3 = xasm.emit_b_d();
                xasm.setBranchTarget(b2);
                xasm.emit_fcmpu(CR7, s0, s1);
                Branch b4 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
                xasm.emit_li32(d, -1);
                Branch b5 = xasm.emit_b_d();
                xasm.setBranchTarget(b1);
                xasm.setBranchTarget(b4);
                xasm.emit_li32(d, 1);
                xasm.setBranchTarget(b3);
                xasm.setBranchTarget(b5);
            }
        };

	ig_DCMPG.generate();
    }

    // goto, goto_w
    public void visit(Instruction.GotoInstruction i) {
        int S2 = Registers.getScratchGPR();
        int S3 = Registers.getScratchGPR();
        try {
            int branchBCOffset = i.getTarget(buf);
            if (branchBCOffset > FAR_JUMP_THRESHOULD
                    || branchBCOffset < -FAR_JUMP_THRESHOULD) { // far jump
                xasm.emit_b_i(1, false, true); // call the next instruction
                xasm.emit_mflr(S2);
                xasm.emit_addi(S2, S2, 6 * 4); // add the distance from the
                // beginning of the mflr to the
                // jump table
                xasm.emit_lwz(S3, S2, 0); // R24 - jump offset
                xasm.emit_add(S3, S2, S3);
                xasm.emit_mtctr(S3);
                xasm.emit_bctr();
                Branch branch = new Branch(xasm.getPC(), 2, 30);
                codeGenContext.addRelativeJumpPatch(branch, getPC()
                        + branchBCOffset);
                xasm.writeWord(0); // jump table of length 1
            } else {
                if (branchBCOffset <= 0) {
                    Branch branch = new Branch(codeGenContext
                            .getBytecodePC2NativePC(getPC() + branchBCOffset));
                    xasm.emit_b_i(branch);
                } else {
                    Branch branch = xasm.emit_b_i_d(false);
                    codeGenContext.addRelativeJumpPatch(branch, getPC()
                            + branchBCOffset);
                }
            }
        } finally {
            Registers.returnScratchGPR(S3);
            Registers.returnScratchGPR(S2);
        }
    }

    // ifeq, ifne, ifle, iflt, ifge, ifgt, ifnull, ifnonnull
    public void visit(Instruction.IfZ i) {
        final int branchBCOffset = i.getBranchTarget(buf);
        int opcode = i.getOpcode();
        int _bi;
        int _bo = BO_TRUE;
        switch (opcode) {
        case Opcodes.IFEQ:
        case Opcodes.IFNULL:
            _bi = CR7_EQ;
            break;
        case Opcodes.IFNE:
        case Opcodes.IFNONNULL:
            _bi = CR7_EQ;
            _bo = BO_FALSE;
            break;
        case Opcodes.IFLT:
            _bi = CR7_LT;
            break;
        case Opcodes.IFGE:
            _bi = CR7_LT;
            _bo = BO_FALSE;
            break;
        case Opcodes.IFGT:
            _bi = CR7_GT;
            break;
        case Opcodes.IFLE:
            _bi = CR7_GT;
            _bo = BO_FALSE;
            break;
        default:
            throw new Error();
        }
        final int bi = _bi;
        final int bo = _bo;

        new InstructionGenerator__I() {
            void emit(int s0) {
                int S0 = Registers.getScratchGPR();
                int S1 = Registers.getScratchGPR();
                try {
                    xasm.emit_cmpi(CR7, false, s0, 0);

                    if (branchBCOffset > FAR_JUMP_THRESHOULD
                            || branchBCOffset < -FAR_JUMP_THRESHOULD) { // far
                                                                        // jump
                        Branch b0 = xasm.emit_bc_d(bo, bi);
                        Branch b1 = xasm.emit_b_d();
                        xasm.setBranchTarget(b0);
                        xasm.emit_b_i(1, false, true); // call the next
                                                        // instruction
                        xasm.emit_mflr(S0); // R28 - S2
                        xasm.emit_addi(S0, S0, 6 * 4); // add the distance from
                        // the
                        // beginning of the mflr to the
                        // jump table
                        xasm.emit_lwz(S1, S0, 0); // R24 - jump offset
                        xasm.emit_add(S1, S0, S1);
                        xasm.emit_mtctr(S1);
                        xasm.emit_bctr();
                        Branch branch = new Branch(xasm.getPC(), 2, 30);
                        codeGenContext.addRelativeJumpPatch(branch, getPC()
                                + branchBCOffset);
                        xasm.writeWord(0); // jump table of length 1
                        xasm.setBranchTarget(b1);
                    } else {
                        if (branchBCOffset <= 0) {
                            Branch branch = new Branch(codeGenContext
                                    .getBytecodePC2NativePC(getPC()
                                            + branchBCOffset));
                            xasm.emit_bc(bo, bi, branch);
                        } else {
                            Branch branch = xasm.emit_bc_d(bo, bi);
                            codeGenContext.addRelativeJumpPatch(branch, getPC()
                                    + branchBCOffset);
                        }
                    }
                } finally {
                    Registers.returnScratchGPR(S1);
                    Registers.returnScratchGPR(S0);
                }
            }
        }.generate();
    }

    // if_icmpxx, if_acmpxx
    public void visit(Instruction.IfCmp i) {
        final int branchBCOffset = i.getBranchTarget(buf);
        int opcode = i.getOpcode();
        int _bi;
        int _bo = BO_TRUE;
        switch (opcode) {
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ACMPEQ:
            _bi = CR7_EQ;
            break;
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ACMPNE:
            _bi = CR7_EQ;
            _bo = BO_FALSE;
            break;
        case Opcodes.IF_ICMPLT:
            _bi = CR7_LT;
            break;
        case Opcodes.IF_ICMPGE:
            _bi = CR7_LT;
            _bo = BO_FALSE;
            break;
        case Opcodes.IF_ICMPGT:
            _bi = CR7_GT;
            break;
        case Opcodes.IF_ICMPLE:
            _bi = CR7_GT;
            _bo = BO_FALSE;
            break;
        default:
            throw new Error();
        }

        final int bi = _bi;
        final int bo = _bo;
        new InstructionGenerator__II() {
            void emit(int s0, int s1) {
                int S2 = Registers.getScratchGPR();
                int S3 = Registers.getScratchGPR();
                try {
                    xasm.emit_cmp(CR7, false, s0, s1);
                    if (branchBCOffset > FAR_JUMP_THRESHOULD
                            || branchBCOffset < -FAR_JUMP_THRESHOULD) { // far
                                                                        // jump
                        Branch b0 = xasm.emit_bc_d(bo, bi);
                        Branch b1 = xasm.emit_b_d();
                        xasm.setBranchTarget(b0);
                        xasm.emit_b_i(1, false, true); // call the next
                                                        // instruction
                        xasm.emit_mflr(S2); // R28 - S2
                        xasm.emit_addi(S2, S2, 6 * 4); // add the distance from
                        // the
                        // beginning of the mflr to the
                        // jump table
                        xasm.emit_lwz(S3, S2, 0); // R24 - jump offset
                        xasm.emit_add(S3, S2, S3);
                        xasm.emit_mtctr(S3);
                        xasm.emit_bctr();
                        Branch branch = new Branch(xasm.getPC(), 2, 30);
                        codeGenContext.addRelativeJumpPatch(branch, getPC()
                                + branchBCOffset);
                        xasm.writeWord(0); // jump table of length 1
                        xasm.setBranchTarget(b1);
                    } else {
                        if (branchBCOffset <= 0) {
                            Branch branch = new Branch(codeGenContext
                                    .getBytecodePC2NativePC(getPC()
                                            + branchBCOffset));
                            xasm.emit_bc(bo, bi, branch);
                        } else {
                            Branch branch = xasm.emit_bc_d(bo, bi);
                            codeGenContext.addRelativeJumpPatch(branch, getPC()
                                    + branchBCOffset);
                        }
                    }
                } finally {
                    Registers.returnScratchGPR(S3);
                    Registers.returnScratchGPR(S2);
                }
            }
        }.generate();
    }

    public void visit(Instruction.LOOKUPSWITCH i) {
        final int def = i.getDefaultTarget(buf);
        final int npairs = i.getTargetCount(buf) - 1;
        final int[] cases = i.getIndexForTargets(buf);
        final int[] targets = i.getTargets(buf);

        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        try {
            new InstructionGenerator__I() {
                void emit(int s0) {
                    for (int j = 0 ; j < npairs; j++) {
                        int caseValue = cases[j];
                        int jumpOffset = targets[j];
                        xasm.emit_cmpi32(CR7, false, s0, caseValue, S0);
                        if (jumpOffset <= 0) {
                            Branch branch = new Branch(codeGenContext.getBytecodePC2NativePC(getPC() + jumpOffset));
                            xasm.emit_bc(BO_TRUE, CR7_EQ, branch);
                        } else {
                            if (jumpOffset > FAR_JUMP_THRESHOULD) { // far jump
                                Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
                                Branch b1 = xasm.emit_b_d();
                                xasm.setBranchTarget(b0);
                                xasm.emit_b_i(1, false, true); // call the next instruction
                                xasm.emit_mflr(S0);
                                xasm.emit_addi(S0, S0, 6 * 4); // add the distance from the beginning of the mflr to the jump table
                                xasm.emit_lwz(S1, S0, 0); // R24 - jump offset
                                xasm.emit_add(S0, S0, S1);
                                xasm.emit_mtctr(S0);
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
                        xasm.emit_mflr(S0);
                        xasm.emit_addi(S0, S0, 6 * 4); // add the distance from the beginning of the mflr to the jump table
                        xasm.emit_lwz(S1, S0, 0); // R24 - jump offset
                        xasm.emit_add(S0, S0, S1);
                        xasm.emit_mtctr(S0);
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
            }.generate();
        } finally {
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }
    
    public void visit(Instruction.TABLESWITCH i) {
        final int def = i.getDefaultTarget(buf);
        final int high = i.getHigh(buf);
        final int low = i.getLow(buf);
        final int[] targets = i.getTargets(buf);

        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        final int S2 = Registers.getScratchGPR();
        try {
            new InstructionGenerator__I() {
                void emit(int s0) {
                    xasm.emit_addi(S2, s0, -low);
                    xasm.emit_cmpli(CR7, false, S2, high-low);
                    
                    if (def <= 0) {
                        Branch branch = new Branch(codeGenContext.getBytecodePC2NativePC(getPC() + def));
                        xasm.emit_bc(BO_TRUE, CR7_GT, branch);
                    } else {
                        if (def > FAR_JUMP_THRESHOULD) { // far jump
                            Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_GT);
                            Branch b1 = xasm.emit_b_d();
                            xasm.setBranchTarget(b0);
                            xasm.emit_b_i(1, false, true); // call the next instruction
                            xasm.emit_mflr(S0);
                            xasm.emit_addi(S0, S0, 6 * 4); // add the distance from the beginning of the mflr to the jump table
                            xasm.emit_lwz(S1, S0, 0); // R24 - jump offset
                            xasm.emit_add(S0, S0, S1);
                            xasm.emit_mtctr(S0);
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
                    xasm.emit_mflr(S0);
                    xasm.emit_addi(S0, S0, 8 * 4); // add the distance from the beginning of the mflr to the jump table
                    xasm.emit_slwi(S1, S2, 2); // x4
                    xasm.emit_add(S0, S0, S1); // R25 - jump offset
                    xasm.emit_lwz(S1, S0, 0); // R24 - jump offset
                    xasm.emit_add(S0, S0, S1);
                    xasm.emit_mtctr(S0);
                    xasm.emit_bctr();

                    // filling the jump table
                    for(int j = 0; j <= high - low; j++) {
                        int jumpOffset = targets[j];
                        Branch branch = new Branch(xasm.getPC(), 2, 30);
                        codeGenContext.addRelativeJumpPatch(branch, getPC() + jumpOffset);
                        xasm.writeWord(0); // dummy
                    }
                }
            }.generate();
        } finally {
            Registers.returnScratchGPR(S2);
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }
    
    public void visit(Instruction.JsrInstruction i) {
        throw new Error();
    }

    public void visit(Instruction.RET i) {
        throw new Error();
    }

    public void visit(Instruction.WIDE_RET i) {
        throw new Error();
    }
    
    public void visit(Instruction.ReturnValue i) {
         if (isSynchronized) {
             int receiverLocalOffset = stackLayout
             .getLocalVariableNativeOffset(0)
             + stackLayout.getNativeFrameSize();
             xasm.emit_lwz(R5, SP, receiverLocalOffset);
             generateCSACall(precomputed.csa_monitorExit_index,
                    precomputed.csa_monitorExit_desc, null, null);
        }
         
        if (debugPrintOn)
            debugPrint("[Returning from " + getSelector().toString() + " ##"
                    + counter + "##]\n");
        int opcode = i.getOpcode();
        switch (opcode) {
        case Opcodes.IRETURN:
        case Opcodes.ARETURN:
            new InstructionGenerator__I() {
                void emit(int s0) {
                    xasm.emit_mr(R3, s0);
                }
            }.generate();
            break;
        case Opcodes.LRETURN:
            new InstructionGenerator__J() {
                void emit(int s0h, int s0l) {
                    xasm.emit_mr(R3, s0h);
                    xasm.emit_mr(R4, s0l);
                }
            }.generate();
            break;
        case Opcodes.FRETURN:
            new InstructionGenerator__F() {
                void emit(int s0) {
                    xasm.emit_fmr(F1, s0);
                }
            }.generate();
            break;
        case Opcodes.DRETURN:
            new InstructionGenerator__D() {
                void emit(int s0) {
                    xasm.emit_fmr(F1, s0);
                }
            }.generate();
            break;
        default:
            throw new OVMError();
        }
        generateReturnSequence();
    }

    public void visit(RETURN i) {
        if (isSynchronized) {
            int receiverLocalOffset = stackLayout
            .getLocalVariableNativeOffset(0)
            + stackLayout.getNativeFrameSize();
            xasm.emit_lwz(R5, SP, receiverLocalOffset);
            generateCSACall(precomputed.csa_monitorExit_index,
                   precomputed.csa_monitorExit_desc, null, null);
       }
       
        generateReturnSequence();
    }

    private InstructionGenerator__I ig_ATHROW = null;

    public void visit(Instruction.ATHROW i) {
	if (ig_ATHROW == null)
	    ig_ATHROW =         new InstructionGenerator__I() {
            void emit(int s0) {
                xasm.emit_mr(R5, s0);
                generateNullCheck(R5, null, null);
                generateCSACall(precomputed.csa_processThrowable_index, 
                        precomputed.csa_processThrowable_desc, null, null);
            }
        };

	ig_ATHROW.generate();
    }
    
    private void generateReturnSequence() {
        xasm.emit_lwz(R0, SP, stackLayout.getReturnAddressOffset()
                + stackLayout.getNativeFrameSize());
        xasm.emit_mtlr(R0);
        xasm.emit_addi(SP, SP, stackLayout.getNativeFrameSize());
        xasm.emit_blr();
    }

    private void popVStackTop2GPR(int gpr) {
        VStack.Item item = vstack.pop();
        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            int reg = registers.l2r(local.index());
            int localOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            if (reg > 0) {
                xasm.emit_mr(gpr, reg);
            } else {
                xasm.emit_lwz(gpr, SP, localOffset);
            }
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            xasm.emit_li32(gpr, constant.intValue());
        }
    }

    private void popFloatVStackTop2GPR(int gpr) {
        VStack.Item item = vstack.pop();
        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            int reg = registers.l2r(local.index());
            int localOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            if (reg > 0) {
                xasm.emit_stfs(reg, SP, localOffset);
                xasm.emit_lwz(gpr, SP, localOffset);
            } else {
                xasm.emit_lwz(gpr, SP, localOffset);
            }
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            xasm.emit_li32(gpr, Float.floatToIntBits(constant.floatValue()));
        }
    }

    private void popDoubleVStackTop2GPR(int gprh, int gprl) {
        VStack.Item item = vstack.pop();
        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            int reg = registers.l2r(local.index());
            int localOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            if (reg > 0) {
                xasm.emit_stfd(reg, SP, localOffset);
            }
            xasm.emit_lwz(gprh, SP, localOffset);
            xasm.emit_lwz(gprl, SP, localOffset + 4);
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            long value = Double.doubleToLongBits(constant.doubleValue());
            xasm.emit_li32(gprh, (int) ((value >> 32) & 0xFFFFFFFFL));
            xasm.emit_li32(gprl, (int) (value & 0xFFFFFFFFL));
        }
    }

    private void popLongVStackTop2GPR(int gprh, int gprl) {
        VStack.Item item = vstack.pop();
        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            int localOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            xasm.emit_lwz(gprh, SP, localOffset);
            xasm.emit_lwz(gprl, SP, localOffset + 4);
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            long value = constant.longValue();
            xasm.emit_li32(gprh, (int) ((value >> 32) & 0xFFFFFFFFL));
            xasm.emit_li32(gprl, (int) (value & 0xFFFFFFFFL));
        }
    }

    private void popLongVStackTop2GPRAndStackSlot(int gprh, int offset,
            int scratchGPR) {
        VStack.Item item = vstack.pop();
        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            int localOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            xasm.emit_lwz(gprh, SP, localOffset);
            xasm.emit_lwz(scratchGPR, SP, localOffset + 4);
            xasm.emit_stw(scratchGPR, SP, offset);
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            long value = constant.longValue();
            xasm.emit_li32(gprh, (int) ((value >> 32) & 0xFFFFFFFFL));
            xasm.emit_li32(scratchGPR, (int) (value & 0xFFFFFFFFL));
            xasm.emit_stw(scratchGPR, SP, offset);
        }
    }

    private void popLongVStackTop2StackSlots(int offseth, int offsetl,
            int scratchGPR) {
        VStack.Item item = vstack.pop();
        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            int localOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            xasm.emit_lwz(scratchGPR, SP, localOffset);
            xasm.emit_stw(scratchGPR, SP, offseth);
            xasm.emit_lwz(scratchGPR, SP, localOffset + 4);
            xasm.emit_stw(scratchGPR, SP, offsetl);
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            long value = constant.longValue();
            xasm.emit_li32(scratchGPR, (int) ((value >> 32) & 0xFFFFFFFFL));
            xasm.emit_stw(scratchGPR, SP, offseth);
            xasm.emit_li32(scratchGPR, (int) (value & 0xFFFFFFFFL));
            xasm.emit_stw(scratchGPR, SP, offsetl);
        }
    }

    private void popVStackTop2StackSlot(int offset, int scratchGPR) {
        VStack.Item item = vstack.pop();
        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            int reg = registers.l2r(local.index());
            int localOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            if (reg > 0) {
                xasm.emit_stw(reg, SP, offset);
            } else {
                xasm.emit_lwz(scratchGPR, SP, localOffset);
                xasm.emit_stw(scratchGPR, SP, offset);
            }
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            xasm.emit_li32(scratchGPR, constant.intValue());
            xasm.emit_stw(scratchGPR, SP, offset);
        }
    }

    private void popVStackTop2FPR(int fpr, boolean isDouble, int scratchGPR) {
        VStack.Item item = vstack.pop();
        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            int reg = registers.l2r(local.index());
            int localOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            if (reg > 0) {
                xasm.emit_fmr(fpr, reg);
            } else {
                if (isDouble) {
                    xasm.emit_lfd(fpr, SP, localOffset);
                } else {
                    xasm.emit_lfs(fpr, SP, localOffset);
                }
            }
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            int fprSaveSlotOffset = stackLayout.getFPRegisterOffset(fpr)
                    + stackLayout.getNativeFrameSize();
            if (isDouble) {
                double val = constant.doubleValue();
                long bits = Double.doubleToLongBits(val);
                xasm.emit_li32(scratchGPR, (int) (bits & 0xFFFFFFFFL));
                xasm.emit_stw(scratchGPR, SP, fprSaveSlotOffset + 4);
                xasm.emit_li32(scratchGPR, (int) ((bits >> 32) & 0xFFFFFFFFL));
                xasm.emit_stw(scratchGPR, SP, fprSaveSlotOffset);
                xasm.emit_lfd(fpr, SP, fprSaveSlotOffset);
            } else {
                float val = constant.floatValue();
                int bits = Float.floatToIntBits(val);
                xasm.emit_li32(scratchGPR, bits);
                xasm.emit_stw(scratchGPR, SP, fprSaveSlotOffset);
                xasm.emit_lfs(fpr, SP, fprSaveSlotOffset);
            }
        }
    }

    private void peekVStackTop2FPR(int fpr, boolean isDouble, int scratchGPR) {
        VStack.Item item = vstack.get(vstack.size() - 1);
        if (item instanceof VStack.Local) {
            VStack.Local local = (VStack.Local) item;
            int reg = registers.l2r(local.index());
            int localOffset = stackLayout.getLocalVariableNativeOffset(local
                    .index())
                    + stackLayout.getNativeFrameSize();
            if (reg > 0) {
                xasm.emit_fmr(fpr, reg);
            } else {
                if (isDouble) {
                    xasm.emit_lfd(fpr, SP, localOffset);
                } else {
                    xasm.emit_lfs(fpr, SP, localOffset);
                }
            }
        } else {
            VStack.Constant constant = (VStack.Constant) item;
            int fprSaveSlotOffset = stackLayout.getFPRegisterOffset(fpr)
                    + stackLayout.getNativeFrameSize();
            if (isDouble) {
                double val = constant.doubleValue();
                long bits = Double.doubleToLongBits(val);
                xasm.emit_li32(scratchGPR, (int) (bits & 0xFFFFFFFFL));
                xasm.emit_stw(scratchGPR, SP, fprSaveSlotOffset + 4);
                xasm.emit_li32(scratchGPR, (int) ((bits >> 32) & 0xFFFFFFFFL));
                xasm.emit_stw(scratchGPR, SP, fprSaveSlotOffset);
                xasm.emit_lfd(fpr, SP, fprSaveSlotOffset);
            } else {
                float val = constant.floatValue();
                int bits = Float.floatToIntBits(val);
                xasm.emit_li32(scratchGPR, bits);
                xasm.emit_stw(scratchGPR, SP, fprSaveSlotOffset);
                xasm.emit_lfs(fpr, SP, fprSaveSlotOffset);
            }
        }
    }

    private void loadArguments(Descriptor.Method desc, boolean receiverOnStack,
            int scratchGPR) {
        int gprRegCount = 1; // receiver
        int fprRegCount = 0;
        int argWordCount = 1;
        for (int k = desc.getArgumentCount() - 1; k >= 0; k--) {
            char t = desc.getArgumentType(k).getTypeTag();
            switch (t) {
            case TypeCodes.VOID:
                break;
            case TypeCodes.INT:
            case TypeCodes.REFERENCE:
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
            throw new Error(
                    "Passing FP parameter in memory not implemented yet.");
        }
        int gprMemCount = gprRegCount - 7;
        int argOffset = 12 + argWordCount * 4;
        int gprRegIndex = R3 + (gprRegCount > 7 ? 7 : gprRegCount); // R4 to R10
        // (R3 is
        // for the
        // code
        // object)
        int fprRegIndex = F0 + fprRegCount; // F1 to F13
        for (int k = desc.getArgumentCount() - 1; k >= 0; k--) {
            char t = desc.getArgumentType(k).getTypeTag();
            switch (t) {
            case TypeCodes.VOID:
                break;
            case TypeCodes.INT:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
            case TypeCodes.REFERENCE:
                if (gprMemCount > 0) {
                    popVStackTop2StackSlot(argOffset, scratchGPR);
                    gprMemCount--;
                } else {
                    popVStackTop2GPR(gprRegIndex);
                    gprRegIndex--;
                }
                argOffset -= 4;
                break;
            case TypeCodes.LONG:
                if (gprMemCount >= 2) { // both words on stack
                    popLongVStackTop2StackSlots(argOffset - 4, argOffset,
                            scratchGPR);
                    gprMemCount -= 2;
                    argOffset -= 8;
                } else if (gprMemCount == 1) { // one word on stack
                    popLongVStackTop2GPRAndStackSlot(gprRegIndex, argOffset,
                            scratchGPR);
                    gprRegIndex--;
                    gprMemCount--;
                    argOffset -= 8;
                } else { // both in registers
                    popLongVStackTop2GPR(gprRegIndex - 1, gprRegIndex);
                    gprRegIndex -= 2;
                    argOffset -= 8;
                }
                break;
            case TypeCodes.DOUBLE:
                popVStackTop2FPR(fprRegIndex, true, scratchGPR);
                fprRegIndex--;
                argOffset -= 8;
                break;
            case TypeCodes.FLOAT:
                popVStackTop2FPR(fprRegIndex, false, scratchGPR);
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
            popVStackTop2GPR(gprRegIndex);
        }
    }

    private void loadArgumentsForInvokeNative(Descriptor.Method desc) {
        int allArgumentRegCount = 0;
        int fprArgumentRegCount = 0;
        for (int k = desc.getArgumentCount() - 1; k >= 0; k--) {
            char t = desc.getArgumentType(k).getTypeTag();
            switch (t) {
            case TypeCodes.VOID:
                break;
            case TypeCodes.INT:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
            case TypeCodes.REFERENCE:
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
            throw new Error(
                    "Passing parameter in memory (vararg) not implemented yet.");
        }
        int allArgumentRegIndex = R4; // R3 for the method index
        int fprArgumentRegIndex = F1;
        for (int k = desc.getArgumentCount() - 1; k >= 0; k--) {
            char t = desc.getArgumentType(k).getTypeTag();
            switch (t) {
            case TypeCodes.VOID:
                break;
            case TypeCodes.INT:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
            case TypeCodes.REFERENCE:
                popVStackTop2GPR(allArgumentRegIndex);
                allArgumentRegIndex++;
                break;
            case TypeCodes.LONG:
                popLongVStackTop2GPR(allArgumentRegIndex,
                        allArgumentRegIndex + 1);
                allArgumentRegIndex += 2;
                break;
            case TypeCodes.DOUBLE: {
                int S0 = Registers.getScratchGPR();
                try {
                    peekVStackTop2FPR(fprArgumentRegIndex, true, S0);
                    popDoubleVStackTop2GPR(allArgumentRegIndex,
                            allArgumentRegIndex + 1);
                    fprArgumentRegIndex++;
                    allArgumentRegIndex += 2;
                } finally {
                    Registers.returnScratchGPR(S0);
                }
                break;
            }
            case TypeCodes.FLOAT: {
                int S0 = Registers.getScratchGPR();
                try {
                    peekVStackTop2FPR(fprArgumentRegIndex, false, S0);
                    popFloatVStackTop2GPR(allArgumentRegIndex);
                    fprArgumentRegIndex++;
                    allArgumentRegIndex++;
                } finally {
                    Registers.returnScratchGPR(S0);
                }
                break;
            }
            default:
                throw new OVMError();
            }
        }
        if (allArgumentRegIndex != R4 + allArgumentRegCount) {
            throw new Error();
        }
    }

    /**
     * Save the registers that are assigned to the locals that are on the VStack
     */
    private void saveRegistersForArgumentLocals() {
        for (int i = 0; i < vstack.size(); i++) {
            VStack.Item item = vstack.get(i);
            if (item instanceof VStack.Local) {
                VStack.Local local = (VStack.Local) item;
                int reg = registers.l2r(local.index());
                if (reg > 0) {
                    switch (local.type()) {
                    case TypeCodes.INT:
                    case TypeCodes.SHORT:
                    case TypeCodes.CHAR:
                    case TypeCodes.BYTE:
                    case TypeCodes.BOOLEAN:
                    case TypeCodes.OBJECT:
                    case TypeCodes.ARRAY:
                    case TypeCodes.REFERENCE:
                        xasm.emit_stw(reg, SP, stackLayout
                                .getGeneralRegisterOffset(reg)
                                + stackLayout.getNativeFrameSize());
                        break;
                    case TypeCodes.LONG:
                        throw new Error("Longs are not assigned to registers");
                    case TypeCodes.DOUBLE:
                    case TypeCodes.FLOAT:
                        xasm.emit_stfd(reg, SP, stackLayout
                                .getFPRegisterOffset(reg)
                                + stackLayout.getNativeFrameSize());
                        break;
                    default:
                        throw new OVMError();
                    }
                }
            }
        }
    }

    /**
     * Save the registers that are assigned to the locals that will be the
     * arguments for a call based on the VStack
     */
    private void restoreRegistersForArgumentLocals() {
        for (int i = 0; i < vstack.size(); i++) {
            VStack.Item item = vstack.get(i);
            if (item instanceof VStack.Local) {
                VStack.Local local = (VStack.Local) item;
                int reg = registers.l2r(local.index());
                if (reg > 0) {
                    switch (local.type()) {
                    case TypeCodes.INT:
                    case TypeCodes.SHORT:
                    case TypeCodes.CHAR:
                    case TypeCodes.BYTE:
                    case TypeCodes.BOOLEAN:
                    case TypeCodes.OBJECT:
                    case TypeCodes.ARRAY:
                    case TypeCodes.REFERENCE:
                        xasm.emit_lwz(reg, SP, stackLayout
                                .getGeneralRegisterOffset(reg)
                                + stackLayout.getNativeFrameSize());
                        break;
                    case TypeCodes.LONG:
                        throw new Error("Longs are not assigned to registers");
                    case TypeCodes.DOUBLE:
                    case TypeCodes.FLOAT:
                        xasm.emit_lfd(reg, SP, stackLayout
                                .getFPRegisterOffset(reg)
                                + stackLayout.getNativeFrameSize());
                        break;
                    default:
                        throw new OVMError();
                    }
                }
            }
        }
    }

    private void unloadReturnValue(char returnTypeCode) {
        if (returnTypeCode == TypeCodes.VOID) {
            xasm.emit_nop();
            return;
        }
        Instruction nextInstruction = nextInstruction();
        if (nextInstruction instanceof Instruction.POP
                || nextInstruction instanceof Instruction.POP2) {
            xasm.emit_nop();
            return;
        }
        Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
        int dindex = write.getLocalVariableOffset(buf);
        int dreg = registers.l2r(dindex);
        int dLocalOffset = stackLayout.getLocalVariableNativeOffset(dindex)
                + stackLayout.getNativeFrameSize();
        switch (returnTypeCode) {
        case TypeCodes.INT:
        case TypeCodes.SHORT:
        case TypeCodes.CHAR:
        case TypeCodes.BYTE:
        case TypeCodes.BOOLEAN:
        case TypeCodes.OBJECT:
        case TypeCodes.ARRAY:
        case TypeCodes.REFERENCE:
            if (dreg > 0) {
                xasm.emit_mr(dreg, R3);
            } else {
                xasm.emit_stw(R3, SP, dLocalOffset);
            }
            break;
        case TypeCodes.LONG:
            xasm.emit_stw(R4, SP, dLocalOffset + 4);
            xasm.emit_stw(R3, SP, dLocalOffset);
            break;
        case TypeCodes.DOUBLE:
            if (dreg > 0) {
                xasm.emit_fmr(dreg, F1);
            } else {
                xasm.emit_stfd(F1, SP, dLocalOffset);
            }
            break;
        case TypeCodes.FLOAT:
            if (dreg > 0) {
                xasm.emit_fmr(dreg, F1);
            } else {
                xasm.emit_stfs(F1, SP, dLocalOffset);
            }
            break;
        default:
            throw new OVMError();
        }
    }

    private void unloadReturnValueForInvokeNative(char returnTypeCode) {
        if (returnTypeCode == TypeCodes.VOID) {
            xasm.emit_nop();
            return;
        }
        Instruction nextInstruction = nextInstruction();
        if (nextInstruction instanceof Instruction.POP
	    || nextInstruction instanceof Instruction.POP2) {
            return;
        }
        Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
        int dindex = write.getLocalVariableOffset(buf);
        int dreg = registers.l2r(dindex);
        int dLocalOffset = stackLayout.getLocalVariableNativeOffset(dindex)
                + stackLayout.getNativeFrameSize();
        switch (returnTypeCode) {
        case TypeCodes.INT:
        case TypeCodes.SHORT:
        case TypeCodes.CHAR:
        case TypeCodes.BYTE:
        case TypeCodes.BOOLEAN:
        case TypeCodes.OBJECT:
        case TypeCodes.ARRAY:
        case TypeCodes.REFERENCE:
            if (dreg > 0) {
                xasm.emit_mr(dreg, R4);
            } else {
                xasm.emit_stw(R4, SP, dLocalOffset);
            }
            break;
        case TypeCodes.LONG:
            xasm.emit_stw(R4, SP, dLocalOffset + 4);
            xasm.emit_stw(R3, SP, dLocalOffset);
            break;
        case TypeCodes.DOUBLE:
            xasm.emit_stw(R4, SP, dLocalOffset + 4);
            xasm.emit_stw(R3, SP, dLocalOffset);
            if (dreg > 0) {
                xasm.emit_lfd(dreg, SP, dLocalOffset);
            }
            break;
        case TypeCodes.FLOAT:
            xasm.emit_stw(R4, SP, dLocalOffset);
            if (dreg > 0) {
                xasm.emit_lfs(dreg, SP, dLocalOffset);
            }
            break;
        default:
            throw new OVMError();
        }
    }

    private void saveRegisters() {
        saveRegisters(null, null);
    }

    private void restoreRegisters() {
        restoreRegisters(null, null);
    }

    /**
     * Save the registers corresponding to the locals that are live-out of this
     * bytecode instruction, and the auxiliary GPRs and FPRs.
     */
    private void saveRegisters(int[] auxGPRs, int[] auxFPRs) {
        if (auxGPRs != null) {
            for (int i = 0; i < auxGPRs.length; i++) {
                int reg = auxGPRs[i];
                xasm.emit_stw(reg, SP, stackLayout.getNativeFrameSize()
                        + stackLayout.getGeneralRegisterOffset(reg));
            }
        }
        if (auxFPRs != null) {
            for (int i = 0; i < auxFPRs.length; i++) {
                int reg = auxFPRs[i];
                xasm.emit_stfd(reg, SP, stackLayout.getNativeFrameSize()
                        + stackLayout.getFPRegisterOffset(reg));
            }
        }
        boolean[] liveness = registers.getLivenessAt(intrinsicPC);
        for (int i = 0; i < liveness.length; i++) {
            // i : local index
            if (!liveness[i]) {
                continue;
            }
            char type = registers.getLocalType(i);
            switch (type) {
            case TypeCodes.VOID:
                continue;
            case TypeCodes.INT:
            case TypeCodes.REFERENCE:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY: {
                int reg = registers.l2r(i);
                if (reg > 0) {
                    xasm.emit_stw(reg, SP, stackLayout.getNativeFrameSize()
                            + stackLayout.getGeneralRegisterOffset(reg));
                }
                break;
            }
            case TypeCodes.LONG: {
                int reg = registers.l2r(i);
                if (reg > 0) {
                    throw new Error("Longs are not assigned to registers");
                }
                break;
            }
            case TypeCodes.FLOAT:
            case TypeCodes.DOUBLE: {
                int reg = registers.l2r(i);
                if (reg > 0) {
                    xasm.emit_stfd(reg, SP, stackLayout.getNativeFrameSize()
                            + stackLayout.getFPRegisterOffset(reg));
                }
                break;
            }
            default:
                throw new Error();
            }
        }
    }

    private void restoreRegisters(int[] auxGPRs, int[] auxFPRs) {
        if (auxGPRs != null) {
            for (int i = 0; i < auxGPRs.length; i++) {
                int reg = auxGPRs[i];
                xasm.emit_lwz(reg, SP, stackLayout.getNativeFrameSize()
                        + stackLayout.getGeneralRegisterOffset(reg));
            }
        }
        if (auxFPRs != null) {
            for (int i = 0; i < auxFPRs.length; i++) {
                int reg = auxFPRs[i];
                xasm.emit_lfd(reg, SP, stackLayout.getNativeFrameSize()
                        + stackLayout.getFPRegisterOffset(reg));
            }
        }
        boolean[] liveness = registers.getLivenessAt(intrinsicPC);
        for (int i = 0; i < liveness.length; i++) {
            // i : local index
            if (!liveness[i]) {
                continue;
            }
            char type = registers.getLocalType(i);
            switch (type) {
            case TypeCodes.INT:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.REFERENCE:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY: {
                int reg = registers.l2r(i);
                if (reg > 0) {
                    xasm.emit_lwz(reg, SP, stackLayout.getNativeFrameSize()
                            + stackLayout.getGeneralRegisterOffset(reg));
                }
                break;
            }
            case TypeCodes.LONG: {
                int reg = registers.l2r(i);
                if (reg > 0) {
                    throw new Error("Longs are not assigned to registers");
                }
                break;
            }
            case TypeCodes.FLOAT:
            case TypeCodes.DOUBLE: {
                int reg = registers.l2r(i);
                if (reg > 0) {
                    xasm.emit_lfd(reg, SP, stackLayout.getNativeFrameSize()
                            + stackLayout.getFPRegisterOffset(reg));
                }
                break;
            }
            default:
                throw new OVMError();
            }
        }
    }

    // Self-modify 3 nops into a sequence of lis, ori, and b.
    // S0 and S1 are scratch registers.
    protected void selfModify_lis_ori_and_b(int offsetReg, int nopPC, int S0,
            int S1) {
        xasm.emit_b_i(1, false, true); // call the next instruction
        int basePC = xasm.getPC();
        xasm.emit_mflr(S0);
        xasm.emit_addi(S0, S0, -(basePC - nopPC)); // S0 - absolute nopPC

        // overwrite the 1st nop with a lis
        int lis_hi = ((15 << 26) | (offsetReg << 21) | (0 << 16)) >> 16;
        xasm.emit_srwi(S1, offsetReg, 16);
        xasm.emit_oris(S1, S1, lis_hi);
        xasm.emit_stw(S1, S0, 0);
        // overwrite the 2nd nop with an ori
        int ori_hi = ((24 << 26) | (offsetReg << 21) | (offsetReg << 16)) >> 16;
        xasm.emit_andi(S1, offsetReg, -1); // extract lower 16 bits
        xasm.emit_oris(S1, S1, ori_hi);
        xasm.emit_stw(S1, S0, 4);

        // overwrite the 3rd nop with a b (branch)
        int branch_offset = basePC - nopPC - 2 * 4 + 4 * 16; // distance from
        // the 3rd nop
        // to the end of
        // this method
        if (-32768 <= branch_offset && branch_offset <= 32767) {
            // b
            int b_hi = (18 << 26) >> 16;
            xasm.emit_li(S1, branch_offset);
            xasm.emit_oris(S1, S1, b_hi);
            xasm.emit_stw(S1, S0, 8);
        } else {
            throw new Error();
        }

        xasm.emit_li(S1, 0);
        xasm.emit_dcbf(S0, S1); // flush i-cache and so on
        xasm.emit_sync();
        xasm.emit_icbl(S0, S1);
        xasm.emit_isync();
    }

    // Self-modify 5 nops into a sequence of lis, ori, lis, ori and b.
    // S0 and S1 are scratch registers.
    protected void selfModify_lis_ori_lis_ori_and_b(int offsetReg, int offsetReg2, int nopPC, int S0,
            int S1) {
        xasm.emit_b_i(1, false, true); // call the next instruction
        int basePC = xasm.getPC();
        xasm.emit_mflr(S0);
        xasm.emit_addi(S0, S0, -(basePC - nopPC)); // S0 - absolute nopPC

        // overwrite the 1st nop with a lis
        int lis_hi = ((15 << 26) | (offsetReg << 21) | (0 << 16)) >> 16;
        xasm.emit_srwi(S1, offsetReg, 16);
        xasm.emit_oris(S1, S1, lis_hi);
        xasm.emit_stw(S1, S0, 0);
        // overwrite the 2nd nop with an ori
        int ori_hi = ((24 << 26) | (offsetReg << 21) | (offsetReg << 16)) >> 16;
        xasm.emit_andi(S1, offsetReg, -1); // extract lower 16 bits
        xasm.emit_oris(S1, S1, ori_hi);
        xasm.emit_stw(S1, S0, 4);

        // overwrite the 3rd nop with a lis
        lis_hi = ((15 << 26) | (offsetReg2 << 21) | (0 << 16)) >> 16;
        xasm.emit_srwi(S1, offsetReg2, 16);
        xasm.emit_oris(S1, S1, lis_hi);
        xasm.emit_stw(S1, S0, 8);
        // overwrite the 4th nop with an ori
        ori_hi = ((24 << 26) | (offsetReg2 << 21) | (offsetReg2 << 16)) >> 16;
        xasm.emit_andi(S1, offsetReg2, -1); // extract lower 16 bits
        xasm.emit_oris(S1, S1, ori_hi);
        xasm.emit_stw(S1, S0, 12);

        // overwrite the 5th nop with a b (branch)
        int branch_offset = basePC - nopPC - 4 * 4 + 4 * 22; // distance from
        // the 5th nop
        // to the end of
        // this method
        if (-32768 <= branch_offset && branch_offset <= 32767) {
            // b
            int b_hi = (18 << 26) >> 16;
            xasm.emit_li(S1, branch_offset);
            xasm.emit_oris(S1, S1, b_hi);
            xasm.emit_stw(S1, S0, 16);
        } else {
            throw new Error();
        }

        xasm.emit_li(S1, 0);
        xasm.emit_dcbf(S0, S1); // flush i-cache and so on
        xasm.emit_sync();
        xasm.emit_icbl(S0, S1);
        xasm.emit_isync();
    }

    // Self-modify 1 nop into a b (branch)
    // S0 and S1 are scratch registers.
    protected void selfModify_b(int nopPC, int S0, int S1) {
        xasm.emit_b_i(1, false, true); // call the next instruction
        int basePC = xasm.getPC();
        xasm.emit_mflr(S0);
        xasm.emit_addi(S0, S0, -(basePC - nopPC)); // S0 - absolute nopPC

        // overwrite the nop with a b (branch)
        int branch_offset = basePC - nopPC + 4 * 10; // distance from the nop
        // to the end of this
        // method
        if (-32768 <= branch_offset && branch_offset <= 32767) {
            // b
            int b_hi = (18 << 26) >> 16;
            xasm.emit_li(S1, branch_offset);
            xasm.emit_oris(S1, S1, b_hi);
            xasm.emit_stw(S1, S0, 0);
        } else {
            throw new Error();
        }

        xasm.emit_li(S1, 0);
        xasm.emit_dcbf(S0, S1); // flush i-cache and so on
        xasm.emit_sync();
        xasm.emit_icbl(S0, S1);
        xasm.emit_isync();
    }

    public void visit(Instruction.INVOKEVIRTUAL i) {
        int S0 = Registers.getScratchGPR();
        int S1 = Registers.getScratchGPR();
        int S2 = Registers.getScratchGPR();
        try {
            int cpindex = i.getCPIndex(buf);
            Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
            int frame_size = stackLayout.getNativeFrameSize();
            if (precomputed.isExecutive || AOT_RESOLUTION_UD
		|| cp.isInstanceMethodResolved(cpindex)) {
                try {
                    ConstantResolvedInstanceMethodref ifi = cp
                            .resolveInstanceMethod(cpindex);
                    int vtbl_index = ifi.getOffset();
                    int offset_cf_in_vtbl = getArrayElementOffset(
                            executiveDomain, OBJECT, vtbl_index);
                    loadArguments(desc, true, S0);
                    generateNullCheck(R4, null, null);
                    xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(S0,
                            R4));
                    xasm.emit_lwz(S0, S0, precomputed.offset_vtbl_in_bp);
                    xasm.emit_lwz(R3, S0, offset_cf_in_vtbl);
                    xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
                    xasm.emit_mtctr(S0);
                    saveRegisters();
                    xasm.emit_bctr_lk();
                    restoreRegisters();
                    unloadReturnValue(desc.getType().getTypeTag());
                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
                    return;
                }
            } else {
                int nopPC = xasm.getPC();
                xasm.emit_nop(); // will become a li32
                xasm.emit_nop(); //
                xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveInstanceMethod_index,
                        precomputed.csa_resolveInstanceMethod_desc, null, null);
                // R3 = vtbl_index
                if (precomputed.eObjectArrayElementSize == 4) {
                    xasm.emit_slwi(S0, R3, 2);
                } else {
                    xasm
                            .emit_mulli(S0, R3,
                                    precomputed.eObjectArrayElementSize);
                }
                xasm.emit_addi(S1, S0, precomputed.eObjectArrayHeaderSize);

                selfModify_lis_ori_and_b(S1, nopPC, S0, S2);

                // the branch from the 3rd nop comes here

                loadArguments(desc, true, S0);
                generateNullCheck(R4, new int[] { S1 }, null);
                xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(S0, R4));
                xasm.emit_lwz(S0, S0, precomputed.offset_vtbl_in_bp);
                xasm.emit_lwzx(R3, S0, S1);
                xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
                xasm.emit_mtctr(S0);
                saveRegisters();
                xasm.emit_bctr_lk();
                restoreRegisters();
                unloadReturnValue(desc.getType().getTypeTag());
            }
        } finally {
            Registers.returnScratchGPR(S2);
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }

    public void visit(Instruction.INVOKEINTERFACE i) {
        int S0 = Registers.getScratchGPR();
        int S1 = Registers.getScratchGPR();
        int S2 = Registers.getScratchGPR();
        try {
            int cpindex = i.getCPIndex(buf);
            Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
            int frame_size = stackLayout.getNativeFrameSize();
            if (precomputed.isExecutive || AOT_RESOLUTION_UD
		|| cp.isInterfaceMethodResolved(cpindex)) {
                try {
                    ConstantResolvedInterfaceMethodref ifi = cp.resolveInterfaceMethod(cpindex);
                    int iftbl_index = ifi.getOffset();
                    int offset_cf_in_iftbl = getArrayElementOffset(executiveDomain, OBJECT, iftbl_index);
                    loadArguments(desc, true, S0);
                    generateNullCheck(R4, null, null);
                    xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(S0, R4)); // R30 = receiver's bp
                    xasm.emit_lwz(S0, S0, precomputed.offset_iftbl_in_bp);           // R29 = iftbl
                    xasm.emit_lwz(R3, S0, offset_cf_in_iftbl);                        // R3 = code
                    xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);            // R28 = code pointer
                    xasm.emit_mtctr(S0);   
                    saveRegisters();
                    xasm.emit_bctr_lk();
                    restoreRegisters();
                    unloadReturnValue(desc.getType().getTypeTag());
                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
                    return;
                }
            } else {
                int nopPC = xasm.getPC();
                xasm.emit_nop(); // will become a li32
                xasm.emit_nop(); //
                xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveInterfaceMethod_index,
                        precomputed.csa_resolveInterfaceMethod_desc, null, null);
                // R3 = iftbl_index
                if (precomputed.eObjectArrayElementSize == 4) {
                    xasm.emit_slwi(S0, R3, 2);
                } else {
                    xasm.emit_mulli(S0, R3, precomputed.eObjectArrayElementSize);
                }
                xasm.emit_addi(S1, S0, precomputed.eObjectArrayHeaderSize);

                selfModify_lis_ori_and_b(S1, nopPC, S0, S2);

                loadArguments(desc, true, S0);
                generateNullCheck(R4, new int[] {S1}, null);
                xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(S0, R4)); // R27 = receiver's bp
                xasm.emit_lwz(S0, S0, precomputed.offset_iftbl_in_bp);           // R26 = iftbl
                xasm.emit_lwzx(R3, S0, S1);                                     // R3 = code
                xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);            // R28 = code pointer
                xasm.emit_mtctr(S0);   
                saveRegisters();
                xasm.emit_bctr_lk();
                restoreRegisters();
                unloadReturnValue(desc.getType().getTypeTag());
            }
        } finally {
            Registers.returnScratchGPR(S2);
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }
    
    public void visit(Instruction.INVOKESTATIC i) {
        int S0 = Registers.getScratchGPR();
        int S1 = Registers.getScratchGPR();
        int S2 = Registers.getScratchGPR();
        try {
            int cpindex = i.getCPIndex(buf);
            int offset_methodref_in_cpvalues = getArrayElementOffset(
                    executiveDomain, OBJECT, cpindex);
            Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
            int frame_size = stackLayout.getNativeFrameSize();

            if (precomputed.isExecutive) { // ED
                try {
                    ConstantResolvedStaticMethodref ifi = cp
                            .resolveStaticMethod(cpindex);
                    int nvtbl_index = ifi.getOffset();
                    int offset_cf_in_nvtbl = getArrayElementOffset(
                            executiveDomain, OBJECT, nvtbl_index);
                    loadArguments(desc, false, S0);
                    if (ED_OBJECT_DONT_MOVE) {
			int nopPC = xasm.getPC();
			xasm.emit_nop(); // will become a li32
			xasm.emit_nop(); //
			xasm.emit_nop(); // will become a li32
			xasm.emit_nop(); //
			xasm.emit_nop(); // will become a branch

                        xasm.emit_lwz(S0, SP, frame_size
                                        + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(S0, S0, offset_methodref_in_cpvalues);
                        xasm.emit_lwz(R4, S0,
                                precomputed.offset_shst_in_staticmref);
                        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(
                                S0, R4));
                        xasm.emit_lwz(S0, S0, precomputed.offset_nvtbl_in_bp);
                        xasm.emit_lwz(R3, S0, offset_cf_in_nvtbl);

			selfModify_lis_ori_lis_ori_and_b(R3, R4, nopPC, S0, S1);

                        xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
                        xasm.emit_mtctr(S0);
                    } else {
                        xasm.emit_lwz(S0, SP, frame_size
                                        + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(S0, S0, offset_methodref_in_cpvalues);
                        xasm.emit_lwz(R4, S0,
                                precomputed.offset_shst_in_staticmref);
                        xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(
                                S0, R4));
                        xasm.emit_lwz(S0, S0, precomputed.offset_nvtbl_in_bp);
                        xasm.emit_lwz(R3, S0, offset_cf_in_nvtbl);
                        xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
                        xasm.emit_mtctr(S0);
                    }
		    saveRegisters();
		    xasm.emit_bctr_lk();
		    restoreRegisters();
		    unloadReturnValue(desc.getType().getTypeTag());
                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
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

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_li32(R5, cpindex);
		    generateCSACall(precomputed.csa_resolveStaticMethod_index,
				    precomputed.csa_resolveStaticMethod_desc, null, null);
		    // R3 = vtbl_index
		    if (precomputed.eObjectArrayElementSize == 4) {
			xasm.emit_slwi(S0, R3, 2);
		    } else {
			xasm
                            .emit_mulli(S0, R3,
					precomputed.eObjectArrayElementSize);
		    }
		    xasm.emit_addi(S1, S0, precomputed.eObjectArrayHeaderSize);

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		    xasm.emit_lwz(S0, S0, offset_methodref_in_cpvalues);
		    xasm.emit_lwz(R4, S0, precomputed.offset_shst_in_staticmref);
		    xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(S0, R4));
		    xasm.emit_lwz(S0, S0, precomputed.offset_nvtbl_in_bp);
		    xasm.emit_lwzx(R3, S0, S1);

		    selfModify_lis_ori_lis_ori_and_b(R3, R4, nopPC, S0, S1);

		    loadArguments(desc, false, S0);
		    xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
		    xasm.emit_mtctr(S0);
		} else {
		    int nopPC = xasm.getPC();
		    xasm.emit_nop(); // will become a li32
		    xasm.emit_nop(); //
		    xasm.emit_nop(); // will become a branch

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_li32(R5, cpindex);
		    generateCSACall(precomputed.csa_resolveStaticMethod_index,
				    precomputed.csa_resolveStaticMethod_desc, null, null);
		    // R3 = vtbl_index
		    if (precomputed.eObjectArrayElementSize == 4) {
			xasm.emit_slwi(S0, R3, 2);
		    } else {
			xasm
                            .emit_mulli(S0, R3,
					precomputed.eObjectArrayElementSize);
		    }
		    xasm.emit_addi(S1, S0, precomputed.eObjectArrayHeaderSize);

		    selfModify_lis_ori_and_b(S1, nopPC, S0, S2);

		    loadArguments(desc, false, S0);
		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		    xasm.emit_lwz(S0, S0, offset_methodref_in_cpvalues);
		    xasm.emit_lwz(R4, S0, precomputed.offset_shst_in_staticmref);
		    xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(S0, R4));
		    xasm.emit_lwz(S0, S0, precomputed.offset_nvtbl_in_bp);
		    xasm.emit_lwzx(R3, S0, S1);
		    xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
		    xasm.emit_mtctr(S0);
		}
                saveRegisters();
                xasm.emit_bctr_lk();
                restoreRegisters();
                unloadReturnValue(desc.getType().getTypeTag());
            }
        } finally {
            Registers.returnScratchGPR(S2);
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }

    public void visit(Instruction.INVOKESPECIAL i) {
        int S0 = Registers.getScratchGPR();
        int S1 = Registers.getScratchGPR();
        int S2 = Registers.getScratchGPR();
        try {
            int cpindex = i.getCPIndex(buf);
            Descriptor.Method desc = i.getSelector(buf, cp).getDescriptor();
            int offset_methodref_in_cpvalues = getArrayElementOffset(
                    executiveDomain, OBJECT, cpindex);
            int frame_size = stackLayout.getNativeFrameSize();

            if (precomputed.isExecutive || AOT_RESOLUTION_UD
		|| cp.isInstanceMethodResolved(cpindex)) {
                try {
                    ConstantResolvedInstanceMethodref imi = cp
                            .resolveInstanceMethod(cpindex);
                    loadArguments(desc, true, S0);
                    generateNullCheck(R4, null, null);
                    if (imi.isNonVirtual) { // NONVIRTUAL2_QUICK
                        int nvtbl_index = imi.getOffset();
                        int offset_cf_in_nvtbl = getArrayElementOffset(
                                executiveDomain, OBJECT, nvtbl_index);
                        if (ED_OBJECT_DONT_MOVE) { // fast
			    int nopPC = xasm.getPC();
			    xasm.emit_nop(); // will become a li32
			    xasm.emit_nop(); //
			    xasm.emit_nop(); // will become a branch

                            xasm.emit_lwz(S0, SP, frame_size
                                    + offset_code_in_stack);
                            xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_values_in_cp);
                            xasm.emit_lwz(S0, S0, offset_methodref_in_cpvalues);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_bp_in_instancemref);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_nvtbl_in_bp);
                            xasm.emit_lwz(R3, S0, offset_cf_in_nvtbl);

			    selfModify_lis_ori_and_b(R3, nopPC, S1, S2);
                            xasm
                                    .emit_lwz(S0, R3,
                                            precomputed.offset_code_in_cf);
                            xasm.emit_mtctr(S0);

                        } else { // slow
                            xasm.emit_lwz(S0, SP, frame_size
                                    + offset_code_in_stack);
                            xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_values_in_cp);
                            xasm.emit_lwz(S0, S0, offset_methodref_in_cpvalues);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_bp_in_instancemref);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_nvtbl_in_bp);
                            xasm.emit_lwz(R3, S0, offset_cf_in_nvtbl);
                            xasm
                                    .emit_lwz(S0, R3,
                                            precomputed.offset_code_in_cf);
                            xasm.emit_mtctr(S0);
			}
			saveRegisters();
			xasm.emit_bctr_lk();
			restoreRegisters();
			unloadReturnValue(desc.getType().getTypeTag());
                    } else { // SUPER_QUICK
                        int vtbl_index = imi.getOffset();
                        int offset_cf_in_vtbl = getArrayElementOffset(
                                executiveDomain, OBJECT, vtbl_index);
                        if (ED_OBJECT_DONT_MOVE) {
			    int nopPC = xasm.getPC();
			    xasm.emit_nop(); // will become a li32
			    xasm.emit_nop(); //
			    xasm.emit_nop(); // will become a branch
                            xasm.emit_lwz(S0, SP, frame_size
                                    + offset_code_in_stack);
                            xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_values_in_cp);
                            xasm.emit_lwz(S0, S0, offset_methodref_in_cpvalues);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_bp_in_instancemref);
                            xasm
                                    .emit_lwz(S0, S0,
                                            precomputed.offset_vtbl_in_bp);
                            xasm.emit_lwz(R3, S0, offset_cf_in_vtbl);

			    selfModify_lis_ori_and_b(R3, nopPC, S1, S2);
                            xasm
                                    .emit_lwz(S0, R3,
                                            precomputed.offset_code_in_cf);
                            xasm.emit_mtctr(S0);
                        } else {
                            xasm.emit_lwz(S0, SP, frame_size
                                    + offset_code_in_stack);
                            xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_values_in_cp);
                            xasm.emit_lwz(S0, S0, offset_methodref_in_cpvalues);
                            xasm.emit_lwz(S0, S0,
                                    precomputed.offset_bp_in_instancemref);
                            xasm
                                    .emit_lwz(S0, S0,
                                            precomputed.offset_vtbl_in_bp);
                            xasm.emit_lwz(R3, S0, offset_cf_in_vtbl);
                            xasm
                                    .emit_lwz(S0, R3,
                                            precomputed.offset_code_in_cf);
                            xasm.emit_mtctr(S0);
			}
			saveRegisters();
			xasm.emit_bctr_lk();
			restoreRegisters();
			unloadReturnValue(desc.getType().getTypeTag());
                    }
                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
                    return;
                }

            } else { // UD
		if (ED_OBJECT_DONT_MOVE) {
		    int nopPC = xasm.getPC();
		    xasm.emit_nop(); // will become a li32
		    xasm.emit_nop(); //
		    xasm.emit_nop(); // will become a branch

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_li32(R5, cpindex);
		    generateCSACall(precomputed.csa_resolveInstanceMethod_index,
				    precomputed.csa_resolveInstanceMethod_desc, null, null);
		    // R3 (return from resolveInstanceMethod) is the vtbl index
		    if (precomputed.eObjectArrayElementSize == 4) {
			xasm.emit_slwi(S0, R3, 2);
		    } else {
			xasm
                            .emit_mulli(S0, R3,
					precomputed.eObjectArrayElementSize);
		    }
		    xasm.emit_addi(S1, S0, precomputed.eObjectArrayHeaderSize);
		    // R28 - offset in table

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		    xasm.emit_lwz(S2, S0, offset_methodref_in_cpvalues);
		    xasm.emit_lwz(S0, S2,
				  precomputed.offset_nonvirtual_in_instancemref);
		    xasm.emit_cmpi(CR7, false, S0, 0);
		    Branch bnv2orsp = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		    // NONVIRTUAL2_QUICK
		    xasm.emit_li32(S0, precomputed.offset_nvtbl_in_bp);
		    Branch bnv2orsp2 = xasm.emit_b_d();
		    xasm.setBranchTarget(bnv2orsp);
		    // SUPER_QUICK
		    xasm.emit_li32(S0, precomputed.offset_vtbl_in_bp);
		    xasm.setBranchTarget(bnv2orsp2);
		    // R22 - table offset in bp
		    xasm.emit_lwz(S2, S2, precomputed.offset_bp_in_instancemref);
		    xasm.emit_lwzx(S0, S2, S0);
		    xasm.emit_lwzx(R3, S0, S1);

		    selfModify_lis_ori_and_b(R3, nopPC, S0, S2);

		    loadArguments(desc, true, S0);
		    generateNullCheck(R4, null, null);

		    xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
		    xasm.emit_mtctr(S0);
		    saveRegisters();
		    xasm.emit_bctr_lk();
		    restoreRegisters();
		    unloadReturnValue(desc.getType().getTypeTag());
		} else {
		    int nopPC = xasm.getPC();
		    xasm.emit_nop(); // will become a li32
		    xasm.emit_nop(); //
		    xasm.emit_nop(); // will become a branch

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_li32(R5, cpindex);
		    generateCSACall(precomputed.csa_resolveInstanceMethod_index,
				    precomputed.csa_resolveInstanceMethod_desc, null, null);
		    // R3 (return from resolveInstanceMethod) is the vtbl index
		    if (precomputed.eObjectArrayElementSize == 4) {
			xasm.emit_slwi(S0, R3, 2);
		    } else {
			xasm
                            .emit_mulli(S0, R3,
					precomputed.eObjectArrayElementSize);
		    }
		    xasm.emit_addi(S1, S0, precomputed.eObjectArrayHeaderSize);
		    // R28 - offset in table

		    selfModify_lis_ori_and_b(S1, nopPC, S0, S2);

		    loadArguments(desc, true, S0);
		    generateNullCheck(R4, new int[] { S1 }, null);

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		    xasm.emit_lwz(S2, S0, offset_methodref_in_cpvalues);
		    xasm.emit_lwz(S0, S2,
				  precomputed.offset_nonvirtual_in_instancemref);
		    xasm.emit_cmpi(CR7, false, S0, 0);
		    Branch bnv2orsp = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		    // NONVIRTUAL2_QUICK
		    xasm.emit_li32(S0, precomputed.offset_nvtbl_in_bp);
		    Branch bnv2orsp2 = xasm.emit_b_d();
		    xasm.setBranchTarget(bnv2orsp);
		    // SUPER_QUICK
		    xasm.emit_li32(S0, precomputed.offset_vtbl_in_bp);
		    xasm.setBranchTarget(bnv2orsp2);
		    // R22 - table offset in bp
		    xasm.emit_lwz(S2, S2, precomputed.offset_bp_in_instancemref);
		    xasm.emit_lwzx(S0, S2, S0);
		    xasm.emit_lwzx(R3, S0, S1);
		    xasm.emit_lwz(S0, R3, precomputed.offset_code_in_cf);
		    xasm.emit_mtctr(S0);
		    saveRegisters();
		    xasm.emit_bctr_lk();
		    restoreRegisters();
		    unloadReturnValue(desc.getType().getTypeTag());
		}
            }
        } finally {
            Registers.returnScratchGPR(S2);
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }

    public void visit(Instruction.INVOKE_NATIVE i) {
        int mindex = i.getMethodIndex(buf);
        UnboundSelector.Method[] argList = compilerVMInterface
                .getNativeMethodList();
        if (mindex < 0 || mindex >= argList.length) {
            throw new OVMError.Internal("Invoke native argument "
                    + Integer.toString(mindex) + " is somehow out of range");
        }
        UnboundSelector.Method m = argList[mindex];
        Descriptor.Method desc = m.getDescriptor();

        if (INLINE_SOME_IN_INVOKENATIVE) {
            if (m.toString().equals("eventsSetEnabled:(Z)V")) {
                final int eventUnionIndex = compilerVMInterface
                        .getRuntimeFunctionIndex("eventUnion");
                if (eventUnionIndex == -1) {
                    throw new Error();
                }
                new InstructionGenerator__I() {
                    void emit(int s0) {
                        int S0 = Registers.getScratchGPR();
                        int S1 = Registers.getScratchGPR();
                        int S2 = Registers.getScratchGPR();
                        try {
                            /* hand inlining of eventsSetEnabled */
                            xasm.emit_li32(S0, precomputed.runtimeFunctionTableHandle
                                    .asInt());
                            xasm.emit_lwz(S0, S0, 0);
                            xasm.emit_lwz(S2, S0, eventUnionIndex * 4);
                            xasm.emit_lwz(S0, S2, 0); // load old eventUion
                            xasm.emit_li32(S1, 0xFFFF0000);
                            xasm.emit_and(S1, S0, S1); // extract notSignaled
                            xasm.emit_cmpi(CR7, false, s0, 0);// compute !notEnabled
                            Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
                            xasm.emit_li32(S0, 0);
                            Branch b1 = xasm.emit_b_d();
                            xasm.setBranchTarget(b0);
                            xasm.emit_li32(S0, 0x00000001);
                            xasm.setBranchTarget(b1);
                            xasm.emit_or(S1, S0, S1); // new eventUnion
                            xasm.emit_stw(S1, S2, 0); // store new eventUnion
                        } finally {
                            Registers.returnScratchGPR(S2);
                            Registers.returnScratchGPR(S1);
                            Registers.returnScratchGPR(S0);
                        }
                    }
                }.generate();
                return;
            }
        }
        // invoke_native() expects the argument in the reverse order with the
        // mindex as the first argument
        loadArgumentsForInvokeNative(desc);
        xasm.emit_li32(R3, mindex);
        generateCFunctionCall("invoke_native", null, null);
        unloadReturnValueForInvokeNative(desc.getType().getTypeTag());
    }

    public void visit(Instruction.GETFIELD i) {
        int cpindex = i.getCPIndex(buf);
        Selector.Field sel = i.getSelector(buf, cp);
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isInstanceFieldResolved(cpindex)) {
            try {
                ConstantResolvedInstanceFieldref ifi = cp
                        .resolveInstanceField(cpindex);
                S3Field field = (S3Field) ifi.getField();
                final int offset = field.getOffset();
                switch (sel.getDescriptor().getType().getTypeTag()) {
                case TypeCodes.INT:
                case TypeCodes.REFERENCE:
                case TypeCodes.SHORT:
                case TypeCodes.CHAR:
                case TypeCodes.BYTE:
                case TypeCodes.BOOLEAN:
                case TypeCodes.OBJECT:
                case TypeCodes.ARRAY:
                    new InstructionGenerator_I_I() {
                        int fold(int s0) {
                            if (s0 == 0) {
                                generateNullPointerException(null, null);
                                return -1;
                            } else {
                                throw new Error();
                            }
                        }

                        void emit(int d, int s0) {
                            generateNullCheck(s0, null, null);
                            xasm.emit_lwz(d, s0, offset);
                        }
                    }.generate_se();
                    break;
                case TypeCodes.LONG:
                    new InstructionGenerator_J_I() {
                        long fold(int s0) {
                            if (s0 == 0) {
                                generateNullPointerException(null, null);
                                return -1L;
                            } else {
                                throw new Error();
                            }
                        }

                        void emit(int dh, int dl, int s0) {
                            generateNullCheck(s0, null, null);
                            xasm.emit_lwz(dl, s0, offset + 4);
                            xasm.emit_lwz(dh, s0, offset);
                        }
                    }.generate_se();
                    break;
                case TypeCodes.DOUBLE:
                    new InstructionGenerator_D_I() {
                        double fold(int s0) {
                            if (s0 == 0) {
                                generateNullPointerException(null, null);
                                return -1.0;
                            } else {
                                throw new Error();
                            }
                        }

                        void emit(int d, int s0) {
                            generateNullCheck(s0, null, null);
                            xasm.emit_lfd(d, s0, offset);
                        }
                    }.generate_se();
                    break;
                case TypeCodes.FLOAT:
                    new InstructionGenerator_F_I() {
                        float fold(int s0) {
                            if (s0 == 0) {
                                generateNullPointerException(null, null);
                                return -1.0F;
                            } else {
                                throw new Error();
                            }
                        }

                        void emit(int d, int s0) {
                            generateNullCheck(s0, null, null);
                            xasm.emit_lfs(d, s0, offset);
                        }
                    }.generate_se();
                    break;
                default:
                    throw new OVMError();
                }
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code", null, null);
                return;
            }
        } else {
            int nopPC = xasm.getPC();
            xasm.emit_nop(); // will become a li32
            xasm.emit_nop(); //
            xasm.emit_nop(); // will become a branch

            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveInstanceField_index,
                        precomputed.csa_resolveInstanceField_desc, null, null);
                // R3 = field offset
                selfModify_lis_ori_and_b(R3, nopPC, S1, S0);
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
            
            switch (sel.getDescriptor().getType().getTypeTag()) {
            case TypeCodes.INT:
            case TypeCodes.REFERENCE:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
                new InstructionGenerator_I_I() {
                    int fold(int s0) {
                        if (s0 == 0) {
                            generateNullPointerException(null, null);
                            return -1;
                        } else {
                            throw new Error();
                        }
                    }

                    void emit(int d, int s0) {
                        generateNullCheck(s0, null, null);
                        xasm.emit_lwzx(d, s0, R3);
                    }
                }.generate_se();
                break;
            case TypeCodes.LONG:
                new InstructionGenerator_J_I() {
                    long fold(int s0) {
                        if (s0 == 0) {
                            generateNullPointerException(null, null);
                            return -1L;
                        } else {
                            throw new Error();
                        }
                    }

                    void emit(int dh, int dl, int s0) {
                        generateNullCheck(s0, null, null);
                        xasm.emit_lwzx(dh, s0, R3);
                        xasm.emit_addi(dl, R3, 4);
                        xasm.emit_lwzx(dl, s0, dl);
                    }
                }.generate_se();
                break;
            case TypeCodes.DOUBLE:
                new InstructionGenerator_D_I() {
                    double fold(int s0) {
                        if (s0 == 0) {
                            generateNullPointerException(null, null);
                            return -1.0;
                        } else {
                            throw new Error();
                        }
                    }

                    void emit(int d, int s0) {
                        generateNullCheck(s0, null, null);
                        xasm.emit_lfdx(d, s0, R3);
                    }
                }.generate_se();
                break;
            case TypeCodes.FLOAT:
                new InstructionGenerator_F_I() {
                    float fold(int s0) {
                        if (s0 == 0) {
                            generateNullPointerException(null, null);
                            return -1.0F;
                        } else {
                            throw new Error();
                        }
                    }

                    void emit(int d, int s0) {
                        generateNullCheck(s0, null, null);
                        xasm.emit_lfsx(d, s0, R3);
                    }
                }.generate_se();
                break;
            default:
                throw new OVMError();
            }

        }
    }

    public void visit(Instruction.PUTFIELD i) {
        if (i instanceof Instruction.PUTFIELD_WITH_BARRIER_REF)
            return;

        int cpindex = i.getCPIndex(buf);
        Selector.Field sel = i.getSelector(buf, cp);
        int frame_size = stackLayout.getNativeFrameSize();
        if (precomputed.isExecutive || AOT_RESOLUTION_UD
	    || cp.isInstanceFieldResolved(cpindex)) {
            try {
                ConstantResolvedInstanceFieldref ifi = cp
                        .resolveInstanceField(cpindex);
                S3Field field = (S3Field) ifi.getField();
                final int offset = field.getOffset();
                switch (sel.getDescriptor().getType().getTypeTag()) {
                case TypeCodes.INT:
                case TypeCodes.REFERENCE:
                case TypeCodes.SHORT:
                case TypeCodes.CHAR:
                case TypeCodes.BYTE:
                case TypeCodes.BOOLEAN:
                case TypeCodes.OBJECT:
                case TypeCodes.ARRAY:
                    new InstructionGenerator__II() {
                        void emit(int s0, int s1) {
                            generateNullCheck(s0, null, null);
                            xasm.emit_stw(s1, s0, offset);
                        }
                    }.generate();
                    break;
                case TypeCodes.LONG:
                    new InstructionGenerator__IJ() {
                        void emit(int s0, int s1h, int s1l) {
                            generateNullCheck(s0, null, null);
                            xasm.emit_stw(s1h, s0, offset);
                            xasm.emit_stw(s1l, s0, offset + 4);
                        }
                    }.generate();
                    break;
                case TypeCodes.DOUBLE:
                    new InstructionGenerator__ID() {
                        void emit(int s0, int s1) {
                            generateNullCheck(s0, null, null);
                            xasm.emit_stfd(s1, s0, offset);
                        }
                    }.generate();
                    break;
                case TypeCodes.FLOAT:
                    new InstructionGenerator__IF() {
                        void emit(int s0, int s1) {
                            generateNullCheck(s0, null, null);
                            xasm.emit_stfs(s1, s0, offset);
                        }
                    }.generate();
                    break;
                default:
                    throw new OVMError();
                }
            } catch (LinkageException e) {
                warn(i, "Unresolvable in ED : " + e.getMessage());
                generateCFunctionCall("hit_unrewritten_code", null, null);
                return;
            }
        } else {
            int nopPC = xasm.getPC();
            xasm.emit_nop(); // will become a li32
            xasm.emit_nop(); //
            xasm.emit_nop(); // will become a branch

            int S0 = Registers.getScratchGPR();
            final int S1 = Registers.getScratchGPR();
            try {
                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveInstanceField_index,
                        precomputed.csa_resolveInstanceField_desc, null, null);
                // R3 = field offset
                selfModify_lis_ori_and_b(R3, nopPC, S1, S0);
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
            
            switch (sel.getDescriptor().getType().getTypeTag()) {
            case TypeCodes.INT:
            case TypeCodes.REFERENCE:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
                new InstructionGenerator__II() {
                    void emit(int s0, int s1) {
                        generateNullCheck(s0, null, null);
                        xasm.emit_stwx(s1, s0, R3);
                    }
                }.generate();
                break;
            case TypeCodes.LONG:
                new InstructionGenerator__IJ() {
                    void emit(int s0, int s1h, int s1l) {
                        generateNullCheck(s0, null, null);
                        final int S1 = Registers.getScratchGPR();
                        try {
                            xasm.emit_stwx(s1h, s0, R3);
                            xasm.emit_addi(S1, R3, 4);
                            xasm.emit_stwx(s1l, s0, S1);
                        } finally {
                            Registers.returnScratchGPR(S1);
                        }
                    }
                }.generate();
                break;
            case TypeCodes.DOUBLE:
                new InstructionGenerator__ID() {
                    void emit(int s0, int s1) {
                        generateNullCheck(s0, null, null);
                        xasm.emit_stfdx(s1, s0, R3);
                    }
                }.generate();
                break;
            case TypeCodes.FLOAT:
                new InstructionGenerator__IF() {
                    void emit(int s0, int s1) {
                        generateNullCheck(s0, null, null);
                        xasm.emit_stfsx(s1, s0, R3);
                    }
                }.generate();
                break;
            default:
                throw new OVMError();
            }

        }
    }

    public void visit(Instruction.GETSTATIC i) {
        int cpindex = i.getCPIndex(buf);
        int offset_fieldref_in_cpvalues = getArrayElementOffset(executiveDomain, OBJECT, cpindex);
        Selector.Field sel = i.getSelector(buf, cp);
        int frame_size = stackLayout.getNativeFrameSize();
        
        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        final int S2 = Registers.getScratchGPR();
        try {
	    if (precomputed.isExecutive) {
		try {
		    ConstantResolvedStaticFieldref sfi = cp.resolveStaticField(cpindex);
		    S3Field field = (S3Field) sfi.getField();
		    final int offset = field.getOffset();
		    if (ED_OBJECT_DONT_MOVE) {
			int nopPC = xasm.getPC();
			xasm.emit_nop(); // will become a li32
			xasm.emit_nop(); //
			xasm.emit_nop(); // will become a branch

			xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
			xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
			xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
			xasm.emit_lwz(S0, S0, offset_fieldref_in_cpvalues);
			xasm.emit_lwz(S0, S0, precomputed.offset_shst_in_staticfref);

			selfModify_lis_ori_and_b(S0, nopPC, S1, S2);
		    } else {
			xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
			xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
			xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
			xasm.emit_lwz(S0, S0, offset_fieldref_in_cpvalues);
			xasm.emit_lwz(S0, S0, precomputed.offset_shst_in_staticfref);
		    }
		    switch (sel.getDescriptor().getType().getTypeTag()) {
		    case TypeCodes.INT:
		    case TypeCodes.REFERENCE:
		    case TypeCodes.SHORT:
		    case TypeCodes.CHAR:
		    case TypeCodes.BYTE:
		    case TypeCodes.BOOLEAN:
		    case TypeCodes.OBJECT:
		    case TypeCodes.ARRAY:
			new InstructionGenerator_I_() {
			    void emit(int d) {
				xasm.emit_lwz(d, S0, offset);
			    }
			}.generate();
			break;
		    case TypeCodes.LONG:
			new InstructionGenerator_J_() {
			    void emit(int dh, int dl) {
				xasm.emit_lwz(dl, S0, offset + 4);
				xasm.emit_lwz(dh, S0, offset);
			    }
			}.generate();
			break;
		    case TypeCodes.DOUBLE:
			new InstructionGenerator_D_() {
			    void emit(int d) {
				xasm.emit_lfd(d, S0, offset);
			    }
			}.generate();
			break;
		    case TypeCodes.FLOAT:
			new InstructionGenerator_F_() {
			    void emit(int d) {
				xasm.emit_lfs(d, S0, offset);
			    }
			}.generate();
			break;
		    default:
			throw new OVMError();
		    }
		} catch (LinkageException e) {
		    warn(i, "Unresolvable in ED : " + e.getMessage());
		    generateCFunctionCall("hit_unrewritten_code", null, null);
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

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_li32(R5, cpindex);
		    generateCSACall(precomputed.csa_resolveStaticField_index, 
				    precomputed.csa_resolveStaticField_desc, null, null);
		    // R3 = field offset

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		    xasm.emit_lwz(S0, S0, offset_fieldref_in_cpvalues);
		    xasm.emit_lwz(S0, S0, precomputed.offset_shst_in_staticfref);

		    selfModify_lis_ori_lis_ori_and_b(S0, R3, nopPC, S1, S2);

		} else {
		    int nopPC = xasm.getPC();
		    xasm.emit_nop(); // will become a li32
		    xasm.emit_nop(); //
		    xasm.emit_nop(); // will become a branch

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_li32(R5, cpindex);
		    generateCSACall(precomputed.csa_resolveStaticField_index, 
				    precomputed.csa_resolveStaticField_desc, null, null);
		    // R3 = field offset

		    selfModify_lis_ori_and_b(R3, nopPC, S1, S2);

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		    xasm.emit_lwz(S0, S0, offset_fieldref_in_cpvalues);
		    xasm.emit_lwz(S0, S0, precomputed.offset_shst_in_staticfref);
		}
            
		switch (sel.getDescriptor().getType().getTypeTag()) {
		case TypeCodes.INT:
		case TypeCodes.REFERENCE:
		case TypeCodes.SHORT:
		case TypeCodes.CHAR:
		case TypeCodes.BYTE:
		case TypeCodes.BOOLEAN:
		case TypeCodes.OBJECT:
		case TypeCodes.ARRAY:
		    new InstructionGenerator_I_() {
			void emit(int d) {
			    xasm.emit_lwzx(d, S0, R3);
			}
		    }.generate();
		    break;
		case TypeCodes.LONG:
		    new InstructionGenerator_J_() {
			void emit(int dh, int dl) {
			    xasm.emit_addi(S1, R3, 4);
			    xasm.emit_lwzx(dl, S0, S1);
			    xasm.emit_lwzx(dh, S0, R3);
			}
		    }.generate();
		    break;
		case TypeCodes.DOUBLE:
		    new InstructionGenerator_D_() {
			void emit(int d) {
			    xasm.emit_lfdx(d, S0, R3);
			}
		    }.generate();
		    break;
		case TypeCodes.FLOAT:
		    new InstructionGenerator_F_() {
			void emit(int d) {
			    xasm.emit_lfsx(d, S0, R3);
			}
		    }.generate();
		    break;
		default:
		    throw new OVMError();
		}
	    }
        } finally {
            Registers.returnScratchGPR(S2);
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }

    // This handles PUTSTATIC_WITH_BARRIER_REF, too
    public void visit(Instruction.PUTSTATIC i) {
        int cpindex = i.getCPIndex(buf);
        int offset_fieldref_in_cpvalues = getArrayElementOffset(executiveDomain, OBJECT, cpindex);
        Selector.Field sel = i.getSelector(buf, cp);
        int frame_size = stackLayout.getNativeFrameSize();
        final boolean barrier = i instanceof Instruction.PUTSTATIC_WITH_BARRIER_REF;
        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        final int S2 = Registers.getScratchGPR();
        try {
	    if (precomputed.isExecutive) {
		try {
		    ConstantResolvedStaticFieldref sfi = cp.resolveStaticField(cpindex);
		    S3Field field = (S3Field) sfi.getField();
		    final int offset = field.getOffset();
		    if (ED_OBJECT_DONT_MOVE) {
			int nopPC = xasm.getPC();
			xasm.emit_nop(); // will become a li32
			xasm.emit_nop(); //
			xasm.emit_nop(); // will become a branch

			xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
			xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
			xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
			xasm.emit_lwz(S0, S0, offset_fieldref_in_cpvalues);
			xasm.emit_lwz(R5, S0, precomputed.offset_shst_in_staticfref);

			selfModify_lis_ori_and_b(R5, nopPC, S1, S2);
		    } else {
			xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
			xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
			xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
			xasm.emit_lwz(S0, S0, offset_fieldref_in_cpvalues);
			xasm.emit_lwz(R5, S0, precomputed.offset_shst_in_staticfref);
		    }
		    switch (sel.getDescriptor().getType().getTypeTag()) {
		    case TypeCodes.INT:
		    case TypeCodes.REFERENCE:
		    case TypeCodes.SHORT:
		    case TypeCodes.CHAR:
		    case TypeCodes.BYTE:
		    case TypeCodes.BOOLEAN:
		    case TypeCodes.OBJECT:
		    case TypeCodes.ARRAY:
			new InstructionGenerator__I() {
			    void emit(int s0) {
				if (barrier) {
				    xasm.emit_li32(R6, offset);
				    xasm.emit_mr(R7, s0);
				    generateCSACall(precomputed.csa_putFieldBarrier_index,
						    precomputed.csa_putFieldBarrier_desc, null, null);
				} else {
				    xasm.emit_stw(s0, R5, offset);
				}
			    }
			}.generate();
			break;
		    case TypeCodes.LONG:
			new InstructionGenerator__J() {
			    void emit(int s0h, int s0l) {
				xasm.emit_stw(s0h, R5, offset);
				xasm.emit_stw(s0l, R5, offset + 4);
			    }
			}.generate();
			break;
		    case TypeCodes.DOUBLE:
			new InstructionGenerator__D() {
			    void emit(int s0) {
				xasm.emit_stfd(s0, R5, offset);
			    }
			}.generate();
			break;
		    case TypeCodes.FLOAT:
			new InstructionGenerator__F() {
			    void emit(int s0) {
				xasm.emit_stfs(s0, R5, offset);
			    }
			}.generate();
			break;
		    default:
			throw new OVMError();
		    }
		} catch (LinkageException e) {
		    warn(i, "Unresolvable in ED : " + e.getMessage());
		    generateCFunctionCall("hit_unrewritten_code", null, null);
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

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack); // get code fragment
		    xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf); // ECX = S3Constants
		    xasm.emit_li32(R5, cpindex);
		    generateCSACall(precomputed.csa_resolveStaticField_index, 
				    precomputed.csa_resolveStaticField_desc, null, null);
		    // R3 = field offset

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		    xasm.emit_lwz(S0, S0, offset_fieldref_in_cpvalues);
		    xasm.emit_lwz(R5, S0, precomputed.offset_shst_in_staticfref);

		    selfModify_lis_ori_lis_ori_and_b(R5, R3, nopPC, S1, S2);

		} else {
		    int nopPC = xasm.getPC();
		    xasm.emit_nop(); // will become a li32
		    xasm.emit_nop(); //
		    xasm.emit_nop(); // will become a branch

		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack); // get code fragment
		    xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf); // ECX = S3Constants
		    xasm.emit_li32(R5, cpindex);
		    generateCSACall(precomputed.csa_resolveStaticField_index, 
				    precomputed.csa_resolveStaticField_desc, null, null);
		    // R3 = field offset

		    selfModify_lis_ori_and_b(R3, nopPC, S1, S2);
		    xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		    xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		    xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		    xasm.emit_lwz(S0, S0, offset_fieldref_in_cpvalues);
		    xasm.emit_lwz(R5, S0, precomputed.offset_shst_in_staticfref);
		}
            
		switch (sel.getDescriptor().getType().getTypeTag()) {
		case TypeCodes.INT:
		case TypeCodes.REFERENCE:
		case TypeCodes.SHORT:
		case TypeCodes.CHAR:
		case TypeCodes.BYTE:
		case TypeCodes.BOOLEAN:
		case TypeCodes.OBJECT:
		case TypeCodes.ARRAY:
		    new InstructionGenerator__I() {
			void emit(int s0) {
			    if (barrier) {
				xasm.emit_mr(R6, R3);
				xasm.emit_mr(R7, s0);
				generateCSACall(precomputed.csa_putFieldBarrier_index,
						precomputed.csa_putFieldBarrier_desc, null, null);
			    } else {
				xasm.emit_stwx(s0, R5, R3);
			    }
			}
		    }.generate();
		    break;
		case TypeCodes.LONG:
		    new InstructionGenerator__J() {
			void emit(int s0h, int s0l) {
			    xasm.emit_stwx(s0h, R5, R3);
			    xasm.emit_addi(S1, R3, 4);
			    xasm.emit_stwx(s0l, R5, S1);
			}
		    }.generate();
		    break;
		case TypeCodes.DOUBLE:
		    new InstructionGenerator__D() {
			void emit(int s0) {
			    xasm.emit_stfdx(s0, R5, R3);
			}
		    }.generate();
		    break;
		case TypeCodes.FLOAT:
		    new InstructionGenerator__F() {
			void emit(int s0) {
			    xasm.emit_stfsx(s0, R5, R3);
			}
		    }.generate();
		    break;
		default:
		    throw new OVMError();
		}
	    }
        } finally {
            Registers.returnScratchGPR(S2);
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }

    public void visit(Instruction.GETFIELD_QUICK i) {
        final int offset = i.getOffset(buf);
        new InstructionGenerator_I_I() {
            int fold(int s0) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1;
                } else {
                    throw new Error();
                }
            }

            void emit(int d, int s0) {
                generateNullCheck(s0, null, null);
                xasm.emit_lwz(d, s0, offset);
            }
        }.generate_se();
    }

    public void visit(Instruction.REF_GETFIELD_QUICK i) {
        final int offset = i.getOffset(buf);
        new InstructionGenerator_I_I() {
            int fold(int s0) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1;
                } else {
                    throw new Error();
                }
            }

            void emit(int d, int s0) {
                generateNullCheck(s0, null, null);
                xasm.emit_lwz(d, s0, offset);
            }
        }.generate_se();
    }

    // this handles PUTFIELD_QUICK_WITH_BARRIER_REF, too?
    public void visit(Instruction.PUTFIELD_QUICK i) {
        final int offset = i.getOffset(buf);
        new InstructionGenerator__II() {
            void emit(int s0, int s1) {
                generateNullCheck(s0, null, null);
                xasm.emit_stw(s1, s0, offset);
            }
        }.generate();
    }

    public void visit(Instruction.PUTFIELD_WITH_BARRIER_REF i) {
        int cpindex = i.getCPIndex(buf);
        int frame_size = stackLayout.getNativeFrameSize();
        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        final int S2 = Registers.getScratchGPR();
        try {
            if (precomputed.isExecutive || AOT_RESOLUTION_UD
		|| cp.isInstanceFieldResolved(cpindex)) {
                try {
                    ConstantResolvedInstanceFieldref ifi = cp
                            .resolveInstanceField(cpindex);
                    S3Field field = (S3Field) ifi.getField();
                    final int offset = field.getOffset();
                    new InstructionGenerator__II() {
                        void emit(int s0, int s1) {
                            xasm.emit_mr(R7, s1);
                            xasm.emit_li32(R6, offset);
                            xasm.emit_mr(R5, s0);
                            generateNullCheck(R5, null, null);
                            generateCSACall(precomputed.csa_putFieldBarrier_index,
                                    precomputed.csa_putFieldBarrier_desc, null, null);
                        }
                    }.generate();

                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
                    return;
                }
            } else {
                int nopPC = xasm.getPC();
                xasm.emit_nop(); // will become a li32
                xasm.emit_nop(); //
                xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack); // get code fragment
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf); // ECX = S3Constants
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveInstanceField_index, 
                        precomputed.csa_resolveInstanceField_desc, null, null);
                // R3 = field offset

                selfModify_lis_ori_and_b(R3, nopPC, S1, S2);

                new InstructionGenerator__II() {
                    void emit(int s0, int s1) {
                        xasm.emit_mr(R7, s1);
                        xasm.emit_mr(R6, R3);
                        xasm.emit_mr(R5, s0);
                        generateNullCheck(R5, null, null);
                        generateCSACall(precomputed.csa_putFieldBarrier_index,
                                precomputed.csa_putFieldBarrier_desc, null, null);
                    }
                }.generate();
            }
        } finally {
            Registers.returnScratchGPR(S2);
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }

    public void visit(Instruction.GETFIELD2_QUICK i) {
        final int offset = i.getOffset(buf);
        new InstructionGenerator_J_I() {
            long fold(int s0) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1L;
                } else {
                    throw new Error();
                }
            }

            void emit(int dh, int dl, int s0) {
                generateNullCheck(s0, null, null);
                xasm.emit_lwz(dl, s0, offset + 4);
                xasm.emit_lwz(dh, s0, offset);
            }
        }.generate_se();
    }

    public void visit(Instruction.PUTFIELD2_QUICK i) {
        final int offset = i.getOffset(buf);
        new InstructionGenerator__IJ() {
            void emit(int s0, int s1h, int s1l) {
                generateNullCheck(s0, null, null);
                xasm.emit_stw(s1h, s0, offset);
                xasm.emit_stw(s1l, s0, offset + 4);
            }
        }.generate();
    }

    private InstructionGenerator_I_II ig_IALOAD = null;

    public void visit(Instruction.IALOAD i) {
	if (ig_IALOAD == null)
	    ig_IALOAD =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1;
                } else {
                    throw new Error();
                }
            }
            void emit(int d, int s0, int s1) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tIntArrayElementSize == 4) {
                        xasm.emit_slwi(S0, s1, 2);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tIntArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_lwz(d, S0, precomputed.tIntArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_IALOAD.generate_se();
    }

    private InstructionGenerator_J_II ig_LALOAD = null;

    public void visit(Instruction.LALOAD i) {
	if (ig_LALOAD == null)
	    ig_LALOAD =         new InstructionGenerator_J_II() {
            long fold(int s0, int s1) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1L;
                } else {
                    throw new Error();
                }
            }
            void emit(int dh, int dl, int s0, int s1) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tLongArrayElementSize == 8) {
                        xasm.emit_slwi(S0, s1, 3);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tLongArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_lwz(dh, S0, precomputed.tLongArrayHeaderSize);
                    xasm.emit_lwz(dl, S0, precomputed.tLongArrayHeaderSize + 4);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_LALOAD.generate_se();
    }

    private InstructionGenerator_F_II ig_FALOAD = null;

    public void visit(Instruction.FALOAD i) {
	if (ig_FALOAD == null)
	    ig_FALOAD =         new InstructionGenerator_F_II() {
            float fold(int s0, int s1) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1F;
                } else {
                    throw new Error();
                }
            }
            void emit(int d, int s0, int s1) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tFloatArrayElementSize == 4) {
                        xasm.emit_slwi(S0, s1, 2);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tFloatArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_lfs(d, S0, precomputed.tFloatArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_FALOAD.generate_se();
    }

    private InstructionGenerator_D_II ig_DALOAD = null;

    public void visit(Instruction.DALOAD i) {
	if (ig_DALOAD == null)
	    ig_DALOAD =         new InstructionGenerator_D_II() {
            double fold(int s0, int s1) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1.0;
                } else {
                    throw new Error();
                }
            }
            void emit(int d, int s0, int s1) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tDoubleArrayElementSize == 8) {
                        xasm.emit_slwi(S0, s1, 3);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tDoubleArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_lfd(d, S0, precomputed.tDoubleArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_DALOAD.generate_se();
    }

    private InstructionGenerator_I_II ig_AALOAD = null;

    public void visit(Instruction.AALOAD i) {
	if (ig_AALOAD == null)
	    ig_AALOAD =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1;
                } else {
                    throw new Error();
                }
            }
            void emit(int d, int s0, int s1) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tObjectArrayElementSize == 4) {
                        xasm.emit_slwi(S0, s1, 2);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tObjectArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_lwz(d, S0, precomputed.tObjectArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_AALOAD.generate_se();
    }

    private InstructionGenerator_I_II ig_SALOAD = null;

    public void visit(Instruction.SALOAD i) {
	if (ig_SALOAD == null)
	    ig_SALOAD =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1;
                } else {
                    throw new Error();
                }
            }
            void emit(int d, int s0, int s1) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tShortArrayElementSize == 2) {
                        xasm.emit_slwi(S0, s1, 1);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tShortArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_lha(d, S0, precomputed.tShortArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_SALOAD.generate_se();
    }

    private InstructionGenerator_I_II ig_CALOAD = null;

    public void visit(Instruction.CALOAD i) {
	if (ig_CALOAD == null)
	    ig_CALOAD =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1;
                } else {
                    throw new Error();
                }
            }
            void emit(int d, int s0, int s1) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tCharArrayElementSize == 2) {
                        xasm.emit_slwi(S0, s1, 1);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tCharArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_lhz(d, S0, precomputed.tCharArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_CALOAD.generate_se();
    }

    private InstructionGenerator_I_II ig_BALOAD = null;

    public void visit(Instruction.BALOAD i) {
	if (ig_BALOAD == null)
	    ig_BALOAD =         new InstructionGenerator_I_II() {
            int fold(int s0, int s1) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1;
                } else {
                    throw new Error();
                }
            }
            void emit(int d, int s0, int s1) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tByteArrayElementSize == 1) {
                        xasm.emit_add(S0, s0, s1);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tByteArrayElementSize);
                        xasm.emit_add(S0, s0, S0);
                    }
                    xasm.emit_lbz(S0, S0, precomputed.tByteArrayHeaderSize);
                    xasm.emit_extsb(d, S0);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_BALOAD.generate_se();
    }

    private InstructionGenerator__III ig_IASTORE = null;

    public void visit(Instruction.IASTORE i) {
	if (ig_IASTORE == null)
	    ig_IASTORE =         new InstructionGenerator__III() {
            void emit(int s0, int s1, int s2) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tIntArrayElementSize == 4) {
                        xasm.emit_slwi(S0, s1, 2);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tIntArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_stw(s2, S0, precomputed.tIntArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_IASTORE.generate();
    }

    private InstructionGenerator__IIJ ig_LASTORE = null;

    public void visit(Instruction.LASTORE i) {
	if (ig_LASTORE == null)
	    ig_LASTORE =         new InstructionGenerator__IIJ() {
            void emit(int s0, int s1, int s2h, int s2l) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tLongArrayElementSize == 8) {
                        xasm.emit_slwi(S0, s1, 3);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tLongArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_stw(s2h, S0, precomputed.tLongArrayHeaderSize);
                    xasm.emit_stw(s2l, S0, precomputed.tLongArrayHeaderSize + 4);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_LASTORE.generate();
    }

    private InstructionGenerator__IIF ig_FASTORE = null;

    public void visit(Instruction.FASTORE i) {
	if (ig_FASTORE == null)
	    ig_FASTORE =         new InstructionGenerator__IIF() {
            void emit(int s0, int s1, int s2) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tFloatArrayElementSize == 4) {
                        xasm.emit_slwi(S0, s1, 2);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tFloatArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_stfs(s2, S0, precomputed.tFloatArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_FASTORE.generate();
    }

    private InstructionGenerator__IID ig_DASTORE = null;

    public void visit(Instruction.DASTORE i) {
	if (ig_DASTORE == null)
	    ig_DASTORE =         new InstructionGenerator__IID() {
            void emit(int s0, int s1, int s2) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tDoubleArrayElementSize == 8) {
                        xasm.emit_slwi(S0, s1, 3);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tDoubleArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_stfd(s2, S0, precomputed.tDoubleArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_DASTORE.generate();
    }

    private InstructionGenerator__III ig_UNCHECKED_AASTORE = null;

    public void visit(Instruction.UNCHECKED_AASTORE i) {
	if (ig_UNCHECKED_AASTORE == null)
	    ig_UNCHECKED_AASTORE =         new InstructionGenerator__III() {
            void emit(int s0, int s1, int s2) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tObjectArrayElementSize == 4) {
                        xasm.emit_slwi(S0, s1, 2);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tObjectArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_stw(s2, S0, precomputed.tObjectArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_UNCHECKED_AASTORE.generate();
    }

    private InstructionGenerator__III ig_AASTORE = null;

    public void visit(Instruction.AASTORE i) {
        if (i instanceof Instruction.AASTORE_WITH_BARRIER)
            return;
	if (ig_AASTORE == null)
	    ig_AASTORE =         new InstructionGenerator__III() {
            void emit(int s0, int s1, int s2) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    generateArrayStoreCheck(s0, s2, new int[] {s0, s1, s2}, null);
                    if (precomputed.tObjectArrayElementSize == 4) {
                        xasm.emit_slwi(S0, s1, 2);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tObjectArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_stw(s2, S0, precomputed.tObjectArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_AASTORE.generate();
    }

    private InstructionGenerator__III ig_AASTORE_WITH_BARRIER = null;

    public void visit(Instruction.AASTORE_WITH_BARRIER i) {
	if (ig_AASTORE_WITH_BARRIER == null)
	    ig_AASTORE_WITH_BARRIER =         new InstructionGenerator__III() {
            void emit(int s0, int s1, int s2) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    generateArrayStoreCheck(s0, s2, new int[] {s0, s1, s2}, null);
                    if (precomputed.tObjectArrayElementSize == 4) {
                        xasm.emit_slwi(S0, s1, 2);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tObjectArrayElementSize);
                    }
                    xasm.emit_mr(R7, s2);
                    xasm.emit_addi(R6, S0, precomputed.tObjectArrayHeaderSize);
                    xasm.emit_mr(R5, s0);
                    generateCSACall(precomputed.csa_aastoreBarrier_index,
                            precomputed.csa_aastoreBarrier_desc, null, null);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_AASTORE_WITH_BARRIER.generate();
    }


    private InstructionGenerator__I ig_READ_BARRIER = null;

    public void visit(Instruction.READ_BARRIER i) {
	if (ig_READ_BARRIER == null)
	    ig_READ_BARRIER =         new InstructionGenerator__I() {
            void emit(int s0) {
                xasm.emit_mr(R5, s0);
                generateCSACall(precomputed.csa_readBarrier_index,
                        precomputed.csa_readBarrier_desc, null, null);
            }
	};

        ig_READ_BARRIER.generate();
    }

    private InstructionGenerator__III ig_SASTORE = null;

    public void visit(Instruction.SASTORE i) {
	if (ig_SASTORE == null)
	    ig_SASTORE =         new InstructionGenerator__III() {
            void emit(int s0, int s1, int s2) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tShortArrayElementSize == 2) {
                        xasm.emit_slwi(S0, s1, 1);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tShortArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_sth(s2, S0, precomputed.tShortArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_SASTORE.generate();
    }

    private InstructionGenerator__III ig_CASTORE = null;

    public void visit(Instruction.CASTORE i) {
	if (ig_CASTORE == null)
	    ig_CASTORE =         new InstructionGenerator__III() {
            void emit(int s0, int s1, int s2) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tCharArrayElementSize == 2) {
                        xasm.emit_slwi(S0, s1, 1);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tCharArrayElementSize);
                    }
                    xasm.emit_add(S0, s0, S0);
                    xasm.emit_sth(s2, S0, precomputed.tCharArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_CASTORE.generate();
    }

    private InstructionGenerator__III ig_BASTORE = null;

    public void visit(Instruction.BASTORE i) {
	if (ig_BASTORE == null)
	    ig_BASTORE =         new InstructionGenerator__III() {
            void emit(int s0, int s1, int s2) {
                final int S0 = Registers.getScratchGPR();
                try {
                    generateNullCheck(s0, null, null);
                    generateArrayBoundCheck(s0, s1, null, null);
                    if (precomputed.tCharArrayElementSize == 1) {
                        xasm.emit_add(S0, s0, s1);
                    } else {
                        xasm.emit_mulli(S0, s1, precomputed.tByteArrayElementSize);
                        xasm.emit_add(S0, s0, S0);
                    }
                    xasm.emit_stb(s2, S0, precomputed.tByteArrayHeaderSize);
                } finally {
                    Registers.returnScratchGPR(S0);
                }
            }
	};

        ig_BASTORE.generate();
    }

    private InstructionGenerator_I_I ig_ARRAYLENGTH = null;

    public void visit(Instruction.ARRAYLENGTH i) {
	if (ig_ARRAYLENGTH == null)
	    ig_ARRAYLENGTH =         new InstructionGenerator_I_I() {
            int fold(int s0) {
                if (s0 == 0) {
                    generateNullPointerException(null, null);
                    return -1;
                } else {
                    throw new Error();
                }
            }
            void emit(int d, int s0) {
                generateNullCheck(s0, null, null);
                switch(precomputed.tArrayLengthFieldSize) {
                case 2:
                    xasm.emit_lhz(d, s0, precomputed.tArrayLengthFieldOffset);
                    break;
                case 4:
                    xasm.emit_lwz(d, s0, precomputed.tArrayLengthFieldOffset);
                    break;
                default:
                    throw new Error("Invalid array element size");
                }
            }
	};

        ig_ARRAYLENGTH.generate_se();
    }
    
    // LDC, LDC_W, LDC2_W
    public void visit(Instruction.ConstantPoolLoad i) {
        if (! (i instanceof Instruction.LDC 
                || i instanceof Instruction.LDC2_W
                || i instanceof Instruction.LDC_W 
                || i instanceof Instruction.LDC_W_REF_QUICK
                || i instanceof Instruction.LDC_REF_QUICK))
                return;

        final int frame_size = stackLayout.getNativeFrameSize();
        final int cpindex = i.getCPIndex(buf);
        switch(cp.getTagAt(cpindex)) {
        case CONSTANT_Integer:
            vstack.push(new VStack.Constant(TypeCodes.INT, 
                    new Integer(cp.getValueAt(cpindex))));
            break;
        case CONSTANT_Float:
            vstack.push(new VStack.Constant(TypeCodes.FLOAT, 
                    new Float(Float.floatToIntBits(cp.getValueAt(cpindex)))));
            break;
        case CONSTANT_Long:
            vstack.push(new VStack.Constant(TypeCodes.LONG, 
                    new Long(cp.getWideValueAt(cpindex))));
            break;
        case CONSTANT_Double:
            vstack.push(new VStack.Constant(TypeCodes.DOUBLE, 
                    new Double(Double.doubleToLongBits(cp.getWideValueAt(cpindex)))));
            break;
        case CONSTANT_String:
        case CONSTANT_Binder:
        case CONSTANT_SharedState: {
            new InstructionGenerator_I_() {
                void emit(int d) {
                    int S0 = Registers.getScratchGPR();
                    int S1 = Registers.getScratchGPR();
                    try {
			if (precomputed.isExecutive || cp.isConstantResolved(cpindex)) {
                            cp.resolveConstantAt(cpindex);
                        } else {
                            int nopPC = xasm.getPC();
                            xasm.emit_nop(); // will become a branch

                            xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                            xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                            xasm.emit_li32(R5, cpindex);
                            generateCSACall(precomputed.csa_resolveLdc_index,
                                    precomputed.csa_resolveLdc_desc, null, null);
                            selfModify_b(nopPC, S0, S1);
                        }

                        // fast path - LDC_QUICK
                        int cpoffset = getArrayElementOffset(executiveDomain, OBJECT,
                                cpindex);

                        if (false && ED_OBJECT_DONT_MOVE) { // Strings are moved by GC
                            throw new Error();
                        } else {
                            xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                            xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                            xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                            xasm.emit_lwz(d, S0, cpoffset);
                        }
                    } finally {
                        Registers.returnScratchGPR(S1);
                        Registers.returnScratchGPR(S0);
                    }
                }
            }.generate();
            break;
        }
        case CONSTANT_Reference: {
            new InstructionGenerator_I_() {
                void emit(int d) {
                    int S0 = Registers.getScratchGPR();
                    try {
                        int cpoffset = getArrayElementOffset(executiveDomain, OBJECT,
                                cpindex);
                        if (false && ED_OBJECT_DONT_MOVE) { // Strings are moved by GC
                            throw new Error();
                        } else {
                            xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                            xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                            xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                            xasm.emit_lwz(d, S0, cpoffset);
                        }
                    } finally {
                        Registers.returnScratchGPR(S0);
                    }
                }
            }.generate();
            break;
        }
        default:
            throw new Error("Unexpected CP tag: " + cp.getTagAt(cpindex));
        }
    }

    private InstructionGenerator__I ig_MONITORENTER = null;

    public void visit(Instruction.MONITORENTER i) {
	if (ig_MONITORENTER == null)
	    ig_MONITORENTER =         new InstructionGenerator__I() {
            void emit(int s0) {
                xasm.emit_mr(R5, s0);
                generateNullCheckForMonitor(R5, null, null);
                generateCSACall(precomputed.csa_monitorEnter_index,
                        precomputed.csa_monitorEnter_desc, null, null);
            }
        };

	ig_MONITORENTER.generate();
    }

    private InstructionGenerator__I ig_MONITOREXIT = null;

    public void visit(Instruction.MONITOREXIT i) {
	if (ig_MONITOREXIT == null)
	    ig_MONITOREXIT =         new InstructionGenerator__I() {
            void emit(int s0) {
                xasm.emit_mr(R5, s0);
                generateNullCheckForMonitor(R5, null, null);
                generateCSACall(precomputed.csa_monitorExit_index,
                        precomputed.csa_monitorExit_desc, null, null);
            }
	};

        ig_MONITOREXIT.generate();
    }
        
    public void visit(Instruction.NEW i) {
        final int cpindex = i.getCPIndex(buf);
        final int frame_size = stackLayout.getNativeFrameSize();
        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        try {
            if (precomputed.isExecutive || AOT_RESOLUTION_UD) {
                try {
                    cp.resolveClassAt(cpindex);
                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
                    return;
                }
            } else {
                int nopPC = xasm.getPC();
                xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveNew_index,
                        precomputed.csa_resolveNew_desc, null, null);

                selfModify_b(nopPC, S0, S1);
            }
            new InstructionGenerator_I_() {
                void emit(int d) {
                    // NEW_QUICK 
                    int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain, OBJECT, cpindex);

                    if (ED_OBJECT_DONT_MOVE) {
			int nopPC = xasm.getPC();
			xasm.emit_nop(); // will become a lis32
			xasm.emit_nop();
			xasm.emit_nop(); // will become a branch

                        xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(R5, S0, offset_bp_in_cpvalues);

			selfModify_lis_ori_and_b(R5, nopPC, S0, S1);

                        generateCSACall(precomputed.csa_allocateObject_index,
                                precomputed.csa_allocateObject_desc, null, null);
                        xasm.emit_mr(d, R3);

                    } else {
                        xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(R5, S0, offset_bp_in_cpvalues);
                        generateCSACall(precomputed.csa_allocateObject_index,
                                precomputed.csa_allocateObject_desc, null, null);
                        xasm.emit_mr(d, R3);
                    }
                }
            }.generate();
        } finally {
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }

    public void visit(Instruction.SINGLEANEWARRAY i) {
        final int cpindex = i.getCPIndex(buf);
        final int frame_size = stackLayout.getNativeFrameSize();
        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        try {
            if (precomputed.isExecutive || AOT_RESOLUTION_UD
		|| cp.isClassResolved(cpindex)) {                
		try {
                    cp.resolveClassAt(cpindex);
                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
                    return;
                }
            } else {
                int nopPC = xasm.getPC();
                xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveClass_index,
                        precomputed.csa_resolveClass_desc, null, null);

                selfModify_b(nopPC, S0, S1);
            }
            new InstructionGenerator_A_I() {
                void emit(int d, int s0) {
                    // ANEWARRAY_QUICK
                    int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
                            OBJECT, cpindex);
                    if (ED_OBJECT_DONT_MOVE) {
			int nopPC = xasm.getPC();
			xasm.emit_nop(); // will become a lis32
			xasm.emit_nop();
			xasm.emit_nop(); // will become a branch

                        xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(R5, S0, offset_bp_in_cpvalues);
			
			selfModify_lis_ori_and_b(R5, nopPC, S0, S1);

                        xasm.emit_mr(R6, s0);
                        generateCSACall(precomputed.csa_allocateArray_index,
                                precomputed.csa_allocateArray_desc, null, null);
                        xasm.emit_mr(d, R3);

                    } else {
                        xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(R5, S0, offset_bp_in_cpvalues);
                        xasm.emit_mr(R6, s0);
                        generateCSACall(precomputed.csa_allocateArray_index,
                                precomputed.csa_allocateArray_desc, null, null);
                        xasm.emit_mr(d, R3);
                    }
                }
            }.generate_se();
        } finally {
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }
    
    public void visit(Instruction.MULTIANEWARRAY i) {
        final int cpindex = i.getCPIndex(buf);
        final int frame_size = stackLayout.getNativeFrameSize();
        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        try {
            if (precomputed.isExecutive || AOT_RESOLUTION_UD
		|| cp.isClassResolved(cpindex)) {
                try {
                    cp.resolveClassAt(cpindex);
                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
                    return;
                }
            } else {
                int nopPC = xasm.getPC();
                xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveClass_index,
                        precomputed.csa_resolveClass_desc, null, null);

                selfModify_b(nopPC, S0, S1);
            }

            // MULTIANEWARRAY_QUICK
            final int dimensions = i.getDimensions(buf);
            int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
                    OBJECT, cpindex);

	    if (ED_OBJECT_DONT_MOVE) {
		int nopPC = xasm.getPC();
		xasm.emit_nop(); // will become a lis32
		xasm.emit_nop();
		xasm.emit_nop(); // will become a branch

		xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		xasm.emit_lwz(R5, S0, offset_bp_in_cpvalues); // R5 = param bp

		selfModify_lis_ori_and_b(R5, nopPC, S0, S1);
	    } else {
		xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
		xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
		xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
		xasm.emit_lwz(R5, S0, offset_bp_in_cpvalues); // R5 = param bp
	    }

            new InstructionGenerator_I_() {
                void emit(int d) {
                    final int begin_offset = stackLayout.getInitialOperandStackPointerOffset() - 4
                        + stackLayout.getNativeFrameSize() - 4 * (dimensions - 1);
                    int offset = begin_offset;
                    for(int k = 0; k < dimensions; k++) {
                        VStack.Item item = vstack.pop();
                        if (item instanceof VStack.Local) {
                            VStack.Local local = (VStack.Local) item;
                            int sreg = registers.l2r(local.index());
                            int sLocalOffset = stackLayout
                                    .getLocalVariableNativeOffset(local.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg > 0) {
                                xasm.emit_stw(sreg, SP, offset);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset);
                                xasm.emit_stw(S0, SP, offset);
                            }
                        } else {
                            VStack.Constant cons = (VStack.Constant) item;
                            xasm.emit_li32(S0, cons.intValue());
                            xasm.emit_stw(S0, SP, offset);
                        }
                        offset += 4;
                    }
                    xasm.emit_addi(R6, SP, offset - 4);
                    xasm.emit_li32(R7, dimensions);
                    generateCSACall(precomputed.csa_allocateMultiArray_index,
                            precomputed.csa_allocateMultiArray_desc, null, null);
                    xasm.emit_mr(d, R3);
                }
            }.generate_se();
        } finally {
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }

    public void visit(Instruction.INSTANCEOF i) {
        final int cpindex = i.getCPIndex(buf);
        final int frame_size = stackLayout.getNativeFrameSize();
        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        try {
            if (precomputed.isExecutive || AOT_RESOLUTION_UD
		|| cp.isClassResolved(cpindex)) {                
		try {
                    cp.resolveClassAt(cpindex);
                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
                    return;
                }
            } else {
                int nopPC = xasm.getPC();
                xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveClass_index,
                        precomputed.csa_resolveClass_desc, null, null);

                selfModify_b(nopPC, S0, S1);
            }
            new InstructionGenerator_I_I() {
                int fold(int s0) {
                    if (s0 == 0) {
                        return 0;
                    } else {
                        throw new Error();
                    }
                }
                void emit(int d, int s0) {
                    // INSTANCEOF_QUICK 
                    int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
                            OBJECT, cpindex);
                    if (ED_OBJECT_DONT_MOVE) {
			int nopPC = xasm.getPC();
			xasm.emit_nop(); // will become a li32
			xasm.emit_nop();
			xasm.emit_nop(); // will become a branch

                        xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(R4, S0, offset_bp_in_cpvalues); // R4 = provider bp

			selfModify_lis_ori_and_b(R4, nopPC, S0, S1);
                    } else {
                        xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(R4, S0, offset_bp_in_cpvalues); // R4 = provider bp
                    }
                    xasm.emit_cmpi(CR7, false, s0, 0);
                    Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
                    xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R3, s0)); // R3 = client bp
		    xasm.emit_cmp(CR7, false, R4, R3);
		    Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);

		    // lastsucc
		    xasm.emit_lwz(S0, R3, precomputed.offset_lastsucc_in_bp);
		    xasm.emit_cmp(CR7, false, R4, S0);
		    Branch b5 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);

		    // lastfail
		    xasm.emit_lwz(S0, R3, precomputed.offset_lastfail_in_bp);
		    xasm.emit_cmp(CR7, false, R4, S0);
		    Branch b4 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);

		    xasm.emit_stw(R3, SP, stackLayout.getNativeFrameSize()
				  + stackLayout.getGeneralRegisterOffset(R3));
		    xasm.emit_stw(R4, SP, stackLayout.getNativeFrameSize()
				  + stackLayout.getGeneralRegisterOffset(R4));
		    
                    generateCFunctionCall("is_subtype_of", null, null);
                    xasm.emit_cmpi(CR7, false, R3, 1);

		    xasm.emit_lwz(R3, SP, stackLayout.getNativeFrameSize()
				  + stackLayout.getGeneralRegisterOffset(R3));
		    xasm.emit_lwz(R4, SP, stackLayout.getNativeFrameSize()
				  + stackLayout.getGeneralRegisterOffset(R4));

		    Branch b6 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		    // fail
		    xasm.emit_stw(R4, R3, precomputed.offset_lastfail_in_bp);
                    Branch b7 = xasm.emit_b_d();
                    xasm.setBranchTarget(b6);
		    // succ
		    xasm.emit_stw(R4, R3, precomputed.offset_lastsucc_in_bp);

		    xasm.setBranchTarget(b2);
                    xasm.setBranchTarget(b5);
		    xasm.emit_li32(d, 1);
		    Branch b3 = xasm.emit_b_d();
                    xasm.setBranchTarget(b0);
                    xasm.setBranchTarget(b4);
                    xasm.setBranchTarget(b7);
                    xasm.emit_li32(d, 0);
		    xasm.setBranchTarget(b3);
                }
            }.generate();
        } finally {
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
    }

    public void visit(Instruction.CHECKCAST i) {
        final int cpindex = i.getCPIndex(buf);
        final int frame_size = stackLayout.getNativeFrameSize();
        final int S0 = Registers.getScratchGPR();
        final int S1 = Registers.getScratchGPR();
        try {
            if (precomputed.isExecutive || AOT_RESOLUTION_UD
		|| cp.isClassResolved(cpindex)) {
                try {
                    cp.resolveClassAt(cpindex);
                } catch (LinkageException e) {
                    warn(i, "Unresolvable in ED : " + e.getMessage());
                    generateCFunctionCall("hit_unrewritten_code", null, null);
                    return;
                }
            } else {
                int nopPC = xasm.getPC();
                xasm.emit_nop(); // will become a branch

                xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                xasm.emit_lwz(R6, S0, precomputed.offset_cp_in_cf);
                xasm.emit_li32(R5, cpindex);
                generateCSACall(precomputed.csa_resolveClass_index,
                        precomputed.csa_resolveClass_desc, null, null);

                selfModify_b(nopPC, S0, S1);
            }
            new InstructionGenerator_I_I() {
                int fold(int s0) {
                    if (s0 == 0) {
                        return 0;
                    } else {
                        throw new Error();
                    }
                }
                void emit(int d, int s0) {
                    // CHECKCAST_QUICK
                    int offset_bp_in_cpvalues = getArrayElementOffset(executiveDomain,
                            OBJECT, cpindex);
                    int classCastException = precomputed.classCastExceptionID;

                    if (ED_OBJECT_DONT_MOVE) {
			int nopPC = xasm.getPC();
			xasm.emit_nop(); // will become a li32
			xasm.emit_nop();
			xasm.emit_nop(); // will become a branch

                        xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(R4, S0, offset_bp_in_cpvalues); // R4 = provider bp

			selfModify_lis_ori_and_b(R4, nopPC, S0, S1);
                    } else {
                        xasm.emit_lwz(S0, SP, frame_size + offset_code_in_stack);
                        xasm.emit_lwz(S0, S0, precomputed.offset_cp_in_cf);
                        xasm.emit_lwz(S0, S0, precomputed.offset_values_in_cp);
                        xasm.emit_lwz(R4, S0, offset_bp_in_cpvalues); // R4 = provider bp
                    }

                    xasm.emit_cmpi(CR7, false, s0, 0);
                    Branch b0 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
                    xasm.write(compilerVMInterface.getGetBlueprintCode_PPC(R3, s0)); // R3 = client bp
		    xasm.emit_cmp(CR7, false, R3, R4);
		    Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);

		    // lastsucc
		    xasm.emit_lwz(S0, R3, precomputed.offset_lastsucc_in_bp);
		    xasm.emit_cmp(CR7, false, R4, S0);
		    Branch b3 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);

		    // lastfail
		    xasm.emit_lwz(S0, R3, precomputed.offset_lastfail_in_bp);
		    xasm.emit_cmp(CR7, false, R4, S0);
		    Branch b4 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);

		    xasm.emit_stw(R3, SP, stackLayout.getNativeFrameSize()
				  + stackLayout.getGeneralRegisterOffset(R3));
		    xasm.emit_stw(R4, SP, stackLayout.getNativeFrameSize()
				  + stackLayout.getGeneralRegisterOffset(R4));
		    
                    generateCFunctionCall("is_subtype_of", null, null);
                    xasm.emit_cmpi(CR7, false, R3, 1);

		    xasm.emit_lwz(R3, SP, stackLayout.getNativeFrameSize()
				  + stackLayout.getGeneralRegisterOffset(R3));
		    xasm.emit_lwz(R4, SP, stackLayout.getNativeFrameSize()
				  + stackLayout.getGeneralRegisterOffset(R4));

		    Branch b5 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
		    // fail
		    xasm.emit_stw(R4, R3, precomputed.offset_lastfail_in_bp);
                    Branch b6 = xasm.emit_b_d();
                    xasm.setBranchTarget(b5);
		    // succ
		    xasm.emit_stw(R4, R3, precomputed.offset_lastsucc_in_bp);
                    Branch b1 = xasm.emit_b_d();

                    xasm.setBranchTarget(b4);
                    xasm.setBranchTarget(b6);
                    xasm.emit_li32(R6, 0);
                    xasm.emit_li32(R5, classCastException);
                    generateCSACall(precomputed.csa_generateThrowable_index,
                            precomputed.csa_generateThrowable_desc, null, null);
                    xasm.setBranchTarget(b0);
                    xasm.setBranchTarget(b1);
		    xasm.setBranchTarget(b2);
                    xasm.setBranchTarget(b3);
                    xasm.emit_mr(d, s0);
                }
            }.generate_se();
        } finally {
            Registers.returnScratchGPR(S1);
            Registers.returnScratchGPR(S0);
        }
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
            new InstructionGenerator__II() {
                void emit(int s0, int s1) {
                    xasm.emit_mr(R4, s1);
                    xasm.emit_mr(R3, s0);
                    generateCFunctionCall("SYSTEM_RUN", null, null);
                }
            }.generate();
            break;
        }
        case EMPTY_CSA_CALL: {
            generateCSACall(precomputed.csa_emptyCall_index,
                    precomputed.csa_emptyCall_desc, null, null);
            break;
        }
        case GET_CONTEXT: {
            new InstructionGenerator_I_I() {
                int fold(int s0) {
                    throw new Error();
                }
                void emit(int d, int s0) {
                    xasm.emit_mr(R3, s0);
                    generateCFunctionCall("SYSTEM_GET_CONTEXT", null, null);
                    xasm.emit_mr(d, R3);
                }
            }.generate();
            break;
        }
        case NEW_CONTEXT: {
            new InstructionGenerator_I_I() {
                int fold(int s0) {
                    throw new Error();
                }
                void emit(int d, int s0) {
                    xasm.emit_mr(R3, s0);
                    generateCFunctionCall("SYSTEM_NEW_CONTEXT", null, null);
                    xasm.emit_mr(d, R3);
                }
            }.generate();
            break;
        }
        case DESTROY_NATIVE_CONTEXT: {
            new InstructionGenerator__I() {
                void emit(int s0) {
                    xasm.emit_mr(R3, s0);
                    generateCFunctionCall("SYSTEM_DESTROY_NATIVE_CONTEXT", null, null);
                }
            }.generate();
            break;
        }
        case GET_ACTIVATION: {
            new InstructionGenerator_I_I() {
                int fold(int s0) {
                    throw new Error();
                }
                void emit(int d, int s0) {
                    xasm.emit_mr(R3, s0);
                    generateCFunctionCall("SYSTEM_GET_ACTIVATION", null, null);
                    xasm.emit_mr(d, R3);
                }
            }.generate();
            break;
        }
        case MAKE_ACTIVATION: {
            new InstructionGenerator_I_() {
                void emit(int d) {
                    VStack.Item item2 = vstack.pop();
                    VStack.Item item1 = vstack.pop();
                    VStack.Item item0 = vstack.pop();
                    if (item0 instanceof VStack.Local) {
                        VStack.Local local0 = (VStack.Local) item0;
                        int sreg0 = registers.l2r(local0.index());
                        int sLocalOffset0 = stackLayout
                                .getLocalVariableNativeOffset(local0.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg0 > 0) {
                            xasm.emit_mr(R3, sreg0);
                        } else {
                            xasm.emit_lwz(R3, SP, sLocalOffset0);
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(R3, const0.intValue());
                    }
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg1 > 0) {
                            xasm.emit_mr(R4, sreg1);
                        } else {
                            xasm.emit_lwz(R4, SP, sLocalOffset1);
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(R4, const1.intValue());
                    }
                    if (item2 instanceof VStack.Local) {
                        VStack.Local local2 = (VStack.Local) item2;
                        int sreg2 = registers.l2r(local2.index());
                        int sLocalOffset2 = stackLayout
                                .getLocalVariableNativeOffset(local2.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg2 > 0) {
                            xasm.emit_mr(R5, sreg2);
                        } else {
                            xasm.emit_lwz(R5, SP, sLocalOffset2);
                        }
                    } else {
                        VStack.Constant const2 = (VStack.Constant) item2;
                        xasm.emit_li32(R5, const2.intValue());
                    }
                    generateCFunctionCall("SYSTEM_MAKE_ACTIVATION", null, null);
                    xasm.emit_mr(d, R3);
                }
            }.generate_se();
            break;
        }
        case CUT_TO_ACTIVATION: {
            new InstructionGenerator__II() {
                void emit(int s0, int s1) {
                    xasm.emit_mr(R4, s1);
                    xasm.emit_mr(R3, s0);
                    generateCFunctionCall("SYSTEM_CUT_TO_ACTIVATION", null, null);
                }
            }.generate();
            break;
        }
        case INVOKE: {
            final char resultType = (char)optype;
            new InstructionGenerator_X_II() {
                void emit(int d, int dh, int dl, int s0, int s1) {
                    xasm.emit_mr(R4, s1);
                    xasm.emit_mr(R3, s0);
                    generateCFunctionCall("SYSTEM_INVOKE", null, null);
                    switch (resultType) {
                    case TypeCodes.VOID:
                        break;
                    case TypeCodes.INT:
                    case TypeCodes.REFERENCE:
                    case TypeCodes.SHORT:
                    case TypeCodes.CHAR:
                    case TypeCodes.BYTE:
                    case TypeCodes.BOOLEAN:
                    case TypeCodes.OBJECT:
                    case TypeCodes.ARRAY:
                        xasm.emit_mr(d, R3);
                        break;
                    case TypeCodes.LONG:
                        xasm.emit_mr(dh, R3);
                        xasm.emit_mr(dl, R4);
                        break;
                    case TypeCodes.DOUBLE:
                    case TypeCodes.FLOAT:
                        xasm.emit_fmr(d, F1);
                        break;
                    default:
                        throw new Error();
                    }
                }
            }.generate_se(resultType);
            break;
        }
        case START_TRACING: {
            new InstructionGenerator__I() {
                void emit(int s0) {
                    xasm.emit_mr(R3, s0);
                    generateCFunctionCall("SYSTEM_START_TRACING", null, null);
                }
            }.generate();
            break;
        }
        case STOP_TRACING: {
            generateCFunctionCall("SYSTEM_STOP_TRACING", null, null);
            break;
        }
        case BEGIN_OVERRIDE_TRACING: {
            break;
        }
        case END_OVERRIDE_TRACING: {
            break;
        }
        case WORD_OP: {
            final int S0 = Registers.getScratchGPR();
            final int S1 = Registers.getScratchGPR();
            try {
                int op = (0xFF & (0x100 + i.getOpType(buf)));
                if (op == WordOps.uI2L) {
                    new InstructionGenerator_J_I() {
                        long fold(int s0) {
                            return ((long)s0 & 0x00000000FFFFFFFFL);
                        }
                        void emit(int dh, int dl, int s0) {
                            xasm.emit_mr(dl, s0);
                            xasm.emit_li32(dh, 0);
                        }
                    }.generate();
                    break;
                }
                switch (op) {
                case WordOps.sCMP: {
                    new InstructionGenerator_I_II() {
                        int fold(int s0, int s1) {
                            if (s0 == s1) {
                                return 0;
                            } else if (s0 > s1) {
                                return 1;
                            } else {
                                return -1;
                            }
                        }
                        void emit(int d, int s0, int s1) {
                            xasm.emit_cmp(CR7, false, s0, s1);
                            Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
                            Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_LT);
                            // 1
                            xasm.emit_li32(d, 1);
                            Branch b3 = xasm.emit_b_d();
                            // -1
                            xasm.setBranchTarget(b2);
                            xasm.emit_li32(d, -1);
                            Branch b4 = xasm.emit_b_d();
                            // 0
                            xasm.setBranchTarget(b1);
                            xasm.emit_li32(d, 0);
                            xasm.setBranchTarget(b3);
                            xasm.setBranchTarget(b4);
                        }
                    }.generate();
                    break;
                }
                case WordOps.uCMP: {
                    new InstructionGenerator_I_II() {
                        int p(int s0, int s1) {
                            if (s0 == s1) {
                                return 0;
                            } else if (s0 > s1) {
                                return 1;
                            } else {
                                return -1;
                            }
                        }
                        int fold(int s0, int s1) {
                            if (true) throw new Error("uCMP fold");
                            if (s0 >= 0) {
                                if (s1 >= 0) {
                                    return p(s0, s1);
                                } else {
                                    return -1;
                                }
                            } else {
                                if (s1 >= 0) {
                                    return 1;
                                } else {
                                    return p(s1, s0);
                                }
                            }
                        }
                        void emit(int d, int s0, int s1) {
                            xasm.emit_cmpl(CR7, false, s0, s1);
                            Branch b1 = xasm.emit_bc_d(BO_TRUE, CR7_EQ);
                            Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_LT);
                            // 1
                            xasm.emit_li32(d, 1);
                            Branch b3 = xasm.emit_b_d();
                            // -1
                            xasm.setBranchTarget(b2);
                            xasm.emit_li32(d, -1);
                            Branch b4 = xasm.emit_b_d();
                            // 0
                            xasm.setBranchTarget(b1);
                            xasm.emit_li32(d, 0);
                            xasm.setBranchTarget(b3);
                            xasm.setBranchTarget(b4);
                        }
                    }.generate();
                    break;
                }
                case WordOps.uLT: {
                    new InstructionGenerator_I_II() {
                        int p(int s0, int s1) {
                            if (s0 == s1) {
                                return 0;
                            } else if (s0 > s1) {
                                return 0;
                            } else {
                                return 1;
                            }
                        }
                        int fold(int s0, int s1) {
                            if (true) throw new Error("uLT fold");
                            if (s0 >= 0) {
                                if (s1 >= 0) {
                                    return p(s0, s1);
                                } else {
                                    return 1;
                                }
                            } else {
                                if (s1 >= 0) {
                                    return 0;
                                } else {
                                    return p(s0, s1);
                                }
                            }
                        }
                        void emit(int d, int s0, int s1) {
                            xasm.emit_cmpl(CR7, false, s0, s1);
                            Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_LT);
                            // 0
                            xasm.emit_li32(d, 0);
                            Branch b3 = xasm.emit_b_d();
                            // 1
                            xasm.setBranchTarget(b2);
                            xasm.emit_li32(d, 1);
                            xasm.setBranchTarget(b3);
                        }
                    }.generate();
                    break;
                }
                case WordOps.uLE: {
                    new InstructionGenerator_I_II() {
                        int p(int s0, int s1) {
                            if (s0 == s1) {
                                return 1;
                            } else if (s0 > s1) {
                                return 0;
                            } else {
                                return 1;
                            }
                        }
                        int fold(int s0, int s1) {
                            if (true) throw new Error("uLE fold");
                            if (s0 >= 0) {
                                if (s1 >= 0) {
                                    return p(s0, s1);
                                } else {
                                    return 1;
                                }
                            } else {
                                if (s1 >= 0) {
                                    return 0;
                                } else {
                                    return p(s0, s1);
                                }
                            }
                        }
                        void emit(int d, int s0, int s1) {
                            xasm.emit_cmpl(CR7, false, s0, s1);
                            Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_GT);
                            // 0
                            xasm.emit_li32(d, 0);
                            Branch b3 = xasm.emit_b_d();
                            // 1
                            xasm.setBranchTarget(b2);
                            xasm.emit_li32(d, 1);
                            xasm.setBranchTarget(b3);
                        }
                    }.generate();
                    break;
                }
                case WordOps.uGE: {
                    new InstructionGenerator_I_II() {
                        int p(int s0, int s1) {
                            if (s0 == s1) {
                                return 1;
                            } else if (s0 > s1) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                        int fold(int s0, int s1) {
                            if (true) throw new Error("uGE fold");
                            if (s0 >= 0) {
                                if (s1 >= 0) {
                                    return p(s0, s1);
                                } else {
                                    return 0;
                                }
                            } else {
                                if (s1 >= 0) {
                                    return 1;
                                } else {
                                    return p(s0, s1);
                                }
                            }
                        }
                        void emit(int d, int s0, int s1) {
                            xasm.emit_cmpl(CR7, false, s0, s1);
                            Branch b2 = xasm.emit_bc_d(BO_FALSE, CR7_LT);
                            // 0
                            xasm.emit_li32(d, 0);
                            Branch b3 = xasm.emit_b_d();
                            // 1
                            xasm.setBranchTarget(b2);
                            xasm.emit_li32(d, 1);
                            xasm.setBranchTarget(b3);
                        }
                    }.generate();
                    break;
                }
                case WordOps.uGT: {
                    new InstructionGenerator_I_II() {
                        int p(int s0, int s1) {
                            if (s0 == s1) {
                                return 0;
                            } else if (s0 > s1) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                        int fold(int s0, int s1) {
                            if (true) throw new Error("uGT fold");
                            if (s0 >= 0) {
                                if (s1 >= 0) {
                                    return p(s0, s1);
                                } else {
                                    return 0;
                                }
                            } else {
                                if (s1 >= 0) {
                                    return 1;
                                } else {
                                    return p(s0, s1);
                                }
                            }
                        }
                        void emit(int d, int s0, int s1) {
                            xasm.emit_cmpl(CR7, false, s0, s1);
                            Branch b2 = xasm.emit_bc_d(BO_TRUE, CR7_GT);
                            // 0
                            xasm.emit_li32(d, 0);
                            Branch b3 = xasm.emit_b_d();
                            // 1
                            xasm.setBranchTarget(b2);
                            xasm.emit_li32(d, 1);
                            xasm.setBranchTarget(b3);
                        }
                    }.generate();
                    break;
                }
                default:
                    throw new Error("Unsupported invoke_system wordop argument : " + op);
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
            break;
        }
        case DEREFERENCE: {
            int op = (0xFF & (0x100 + i.getOpType(buf)));
            switch (op) {
            case DereferenceOps.getByte:
                new InstructionGenerator_I_I() {
                    int fold(int s0) { throw new Error(); }
                    void emit(int d, int s0) {
                        xasm.emit_lbz(d, s0, 0);
                        xasm.emit_extsb(d, d);
                    }
                }.generate();
                break;
            case DereferenceOps.getShort:
                new InstructionGenerator_I_I() {
                    int fold(int s0) { throw new Error(); }
                    void emit(int d, int s0) {
                        xasm.emit_lha(d, s0, 0);
                    }
                }.generate();
                break;
            case DereferenceOps.getChar:
                new InstructionGenerator_I_I() {
                    int fold(int s0) { throw new Error(); }
                    void emit(int d, int s0) {
                        xasm.emit_lhz(d, s0, 0);
                    }
                }.generate();
                break;
            case DereferenceOps.setByte:
                new InstructionGenerator__II() {
                    void emit(int s0, int s1) {
                      xasm.emit_stb(s1, s0, 0);
                    }
                }.generate();
                break;
            case DereferenceOps.setShort:
                new InstructionGenerator__II() {
                    void emit(int s0, int s1) {
                        xasm.emit_sth(s1, s0, 0);
                    }
                }.generate();
                break;
            case DereferenceOps.setBlock:
                new InstructionGenerator__III() {
                void emit(int s0, int s1, int s2) {
                    xasm.emit_mr(R5, s2);
                    xasm.emit_mr(R4, s1);
                    xasm.emit_mr(R3, s0);
                    generateCFunctionCall("memmove", null, null);
                }
                }.generate();
                break;
            default:
                throw new Error("Unsupported invoke_system dereference op argument : " + op);
            }
            break;
        }

        case CAS32: {
            new InstructionGenerator_I_() {
                void emit(int d) {
                    VStack.Item item2 = vstack.pop();
                    VStack.Item item1 = vstack.pop();
                    VStack.Item item0 = vstack.pop();
                    if (item0 instanceof VStack.Local) {
                        VStack.Local local0 = (VStack.Local) item0;
                        int sreg0 = registers.l2r(local0.index());
                        int sLocalOffset0 = stackLayout
                                .getLocalVariableNativeOffset(local0.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg0 > 0) {
                            xasm.emit_mr(R3, sreg0);
                        } else {
                            xasm.emit_lwz(R3, SP, sLocalOffset0);
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(R3, const0.intValue());
                    }
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg1 > 0) {
                            xasm.emit_mr(R4, sreg1);
                        } else {
                            xasm.emit_lwz(R4, SP, sLocalOffset1);
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(R4, const1.intValue());
                    }
                    if (item2 instanceof VStack.Local) {
                        VStack.Local local2 = (VStack.Local) item2;
                        int sreg2 = registers.l2r(local2.index());
                        int sLocalOffset2 = stackLayout
                                .getLocalVariableNativeOffset(local2.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg2 > 0) {
                            xasm.emit_mr(R5, sreg2);
                        } else {
                            xasm.emit_lwz(R5, SP, sLocalOffset2);
                        }
                    } else {
                        VStack.Constant const2 = (VStack.Constant) item2;
                        xasm.emit_li32(R5, const2.intValue());
                    }
                    generateCFunctionCall("CAS32", null, null);
                    xasm.emit_mr(d, R3);
                }
            }.generate_se();
           break;
        }
        default:
            throw new Error("Unsupported invoke_system argument : " + mindex);
        }
    }
    
    private abstract class InstructionGenerator_I_II {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S2, sreg0, sreg1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S2, S0, sreg1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, S0, sreg1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        int value = fold(const0.intValue(), const1.intValue());
                        if (dreg > 0) {
                            xasm.emit_li32(dreg, value);
                        } else {
                            xasm.emit_li32(S0, value);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        void generate_se() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dreg = write != null ? registers.l2r(dindex) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S2, sreg0, sreg1);
                                    if (write != null)
                                        xasm.emit_stw(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    if (write != null)
                                        xasm.emit_stw(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S2, S0, sreg1);
                                    if (write != null)
                                        xasm.emit_stw(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    if (write != null)
                                        xasm.emit_stw(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                if (write != null)
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                if (write != null)
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, S0, sreg1);
                                if (write != null)
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                if (write != null)
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        int value = fold(const0.intValue(), const1.intValue());
                        if (write != null) {
                            if (dreg > 0) {
                                xasm.emit_li32(dreg, value);
                            } else {
                                xasm.emit_li32(S0, value);
                                xasm.emit_stw(S0, SP, dLocalOffset);
                            }
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract int fold(int s0, int s1);

        abstract void emit(int d, int s0, int s1);
    }

    private abstract class InstructionGenerator_X_II {
        void _emit(boolean do_write, char resultType, int dregG, int dregF, int dregH, int  dregL, 
                int dLocalOffset, int sreg0, int sreg1) {
            switch (resultType) {
            case TypeCodes.VOID:
                emit(-1, -1, -1, sreg0, sreg1);
                break;
            case TypeCodes.INT:
            case TypeCodes.REFERENCE:
            case TypeCodes.SHORT:
            case TypeCodes.CHAR:
            case TypeCodes.BYTE:
            case TypeCodes.BOOLEAN:
            case TypeCodes.OBJECT:
            case TypeCodes.ARRAY:
                emit(dregG, -1, -1, sreg0, sreg1);
                if (do_write) {
                    xasm.emit_stw(dregG, SP, dLocalOffset);
                }
                break;
            case TypeCodes.LONG:
                emit(-1, dregH, dregL, sreg0, sreg1);
                if (do_write) {
                    xasm.emit_stw(dregH, SP, dLocalOffset);
                    xasm.emit_stw(dregL, SP, dLocalOffset + 4);
                }
                break;
            case TypeCodes.DOUBLE:
                emit(dregF, -1, -1, sreg0, sreg1);
                if (do_write) {
                    xasm.emit_stfd(dregF, SP, dLocalOffset);
                }
                break;
            case TypeCodes.FLOAT:
                emit(dregF, -1, -1, sreg0, sreg1);
                if (do_write) {
                    xasm.emit_stfs(dregF, SP, dLocalOffset);
                }
                break;
            default:
                throw new Error();
            }
        }
        void generate_se(char resultType) {
            generate(resultType);
        }
        void generate(char resultType) {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = null;
            if (resultType != TypeCodes.VOID) {
                nextInstruction = nextInstruction();
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            int S3 = Registers.getScratchGPR();
            int S4 = Registers.getScratchGPR();
            int SF = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction != null
                        && nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                boolean do_write = write != null;
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dreg = write != null ? registers.l2r(dindex) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, -1, -1, sreg0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset1);
                                    emit(dreg, -1, -1, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(dreg, -1, -1, S0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(dreg, -1, -1, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    _emit(do_write, resultType, S2, SF, S3, S4, dLocalOffset, sreg0, sreg1);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    _emit(do_write, resultType, S2, SF, S3, S4, dLocalOffset, sreg0, S1);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    _emit(do_write, resultType, S2, SF, S3, S4, dLocalOffset, S0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    _emit(do_write, resultType, S2, SF, S3, S4, dLocalOffset, S0, S1);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, -1, -1, sreg0, S1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(dreg, -1, -1, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                _emit(do_write, resultType, S2, SF, S3, S4, dLocalOffset, sreg0, S1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                _emit(do_write, resultType, S2, SF, S3, S4, dLocalOffset, S0, S1);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, -1, -1, S0, sreg1);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(dreg, -1, -1, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                _emit(do_write, resultType, S2, SF, S3, S4, dLocalOffset, S0, sreg1);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                _emit(do_write, resultType, S2, SF, S3, S4, dLocalOffset, S0, S1);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S0, const0.intValue());
                        xasm.emit_li32(S1, const1.intValue());
                        if (dreg > 0) {
                            emit(dreg, -1, -1, S0, S1);
                        } else {
                            _emit(do_write, resultType, S2, SF, S3, S4, dLocalOffset, S0, S1);
                        }
                    }
                }
            } finally {
                Registers.returnScratchFPR(SF);
                Registers.returnScratchGPR(S4);
                Registers.returnScratchGPR(S3);
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract void emit(int d, int dh, int dl, int s0, int s1);
    }


    private abstract class InstructionGenerator_J_II {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            int S3 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg0 > 0) {
                            if (sreg1 > 0) {
                                emit(S2, S3, sreg0, sreg1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                                xasm.emit_stw(S3, SP, dLocalOffset + 4);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S3, sreg0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                                xasm.emit_stw(S3, SP, dLocalOffset + 4);
                            }
                        } else {
                            if (sreg1 > 0) {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S2, S3, S0, sreg1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                                xasm.emit_stw(S3, SP, dLocalOffset + 4);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S3, S0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                                xasm.emit_stw(S3, SP, dLocalOffset + 4);
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (sreg0 > 0) {
                            emit(S2, S3, sreg0, S1);
                            xasm.emit_stw(S2, SP, dLocalOffset);
                            xasm.emit_stw(S3, SP, dLocalOffset + 4);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S2, S3, S0, S1);
                            xasm.emit_stw(S2, SP, dLocalOffset);
                            xasm.emit_stw(S3, SP, dLocalOffset + 4);
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (sreg1 > 0) {
                            emit(S2, S3, S0, sreg1);
                            xasm.emit_stw(S2, SP, dLocalOffset);
                            xasm.emit_stw(S3, SP, dLocalOffset + 4);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset1);
                            emit(S2, S3, S0, S1);
                            xasm.emit_stw(S2, SP, dLocalOffset);
                            xasm.emit_stw(S3, SP, dLocalOffset + 4);
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value = fold(const0.intValue(), const1.intValue());
                        xasm.emit_li32(S2, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S3, (int) (value & 0xFFFFFFFFL));
                        xasm.emit_stw(S2, SP, dLocalOffset);
                        xasm.emit_stw(S3, SP, dLocalOffset + 4);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S3);
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        void generate_se() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            int S3 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg0 > 0) {
                            if (sreg1 > 0) {
                                emit(S2, S3, sreg0, sreg1);
                                if (write != null) {
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                    xasm.emit_stw(S3, SP, dLocalOffset + 4);
                                }
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S3, sreg0, S1);
                                if (write != null) {
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                    xasm.emit_stw(S3, SP, dLocalOffset + 4);
                                }
                            }
                        } else {
                            if (sreg1 > 0) {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S2, S3, S0, sreg1);
                                if (write != null) {
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                    xasm.emit_stw(S3, SP, dLocalOffset + 4);
                                }
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S3, S0, S1);
                                if (write != null) {
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                    xasm.emit_stw(S3, SP, dLocalOffset + 4);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (sreg0 > 0) {
                            emit(S2, S3, sreg0, S1);
                            if (write != null) {
                                xasm.emit_stw(S2, SP, dLocalOffset);
                                xasm.emit_stw(S3, SP, dLocalOffset + 4);
                            }
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S2, S3, S0, S1);
                            if (write != null) {
                                xasm.emit_stw(S2, SP, dLocalOffset);
                                xasm.emit_stw(S3, SP, dLocalOffset + 4);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (sreg1 > 0) {
                            emit(S2, S3, S0, sreg1);
                            if (write != null) {
                                xasm.emit_stw(S2, SP, dLocalOffset);
                                xasm.emit_stw(S3, SP, dLocalOffset + 4);
                            }
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset1);
                            emit(S2, S3, S0, S1);
                            if (write != null) {
                                xasm.emit_stw(S2, SP, dLocalOffset);
                                xasm.emit_stw(S3, SP, dLocalOffset + 4);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value = fold(const0.intValue(), const1.intValue());
                        if (write != null) {
                            xasm.emit_li32(S2, (int) ((value >> 32) & 0xFFFFFFFFL));
                            xasm.emit_li32(S3, (int) (value & 0xFFFFFFFFL));
                            xasm.emit_stw(S2, SP, dLocalOffset);
                            xasm.emit_stw(S3, SP, dLocalOffset + 4);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S3);
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract long fold(int s0, int s1);

        abstract void emit(int dh, int dl, int s0, int s1);
    }


    private abstract class InstructionGenerator_F_II {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S2, sreg0, sreg1);
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S2, S0, sreg1);
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                xasm.emit_stfs(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                xasm.emit_stfs(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, S0, sreg1);
                                xasm.emit_stfs(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                xasm.emit_stfs(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        float value = fold(const0.intValue(), const1.intValue());
                        xasm.emit_li32(SI, Float.floatToIntBits(value));
                        if (dreg > 0) {
                            xasm.emit_stw(SI, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfs(dreg, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                        } else {
                            xasm.emit_stw(SI, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfs(S2, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_stfs(S2, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        void generate_se() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dreg = write != null ? registers.l2r(dindex) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S2, sreg0, sreg1);
                                    if (write != null)
                                        xasm.emit_stfs(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    if (write != null)
                                        xasm.emit_stfs(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S2, S0, sreg1);
                                    if (write != null)
                                        xasm.emit_stfs(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    if (write != null)
                                        xasm.emit_stfs(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                if (write != null)
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                if (write != null)
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, S0, sreg1);
                                if (write != null)
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                if (write != null)
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        float value = fold(const0.intValue(), const1.intValue());
                        if (write != null) {
                            xasm.emit_li32(SI, Float.floatToIntBits(value));
                            if (dreg > 0) {
                                xasm.emit_stw(SI, SP, stackLayout
                                        .getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize());
                                xasm.emit_lfs(dreg, SP, stackLayout
                                        .getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize());
                            } else {
                                xasm.emit_stw(SI, SP, stackLayout
                                        .getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize());
                                xasm.emit_lfs(S2, SP, stackLayout
                                        .getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize());
                                xasm.emit_stfs(S2, SP, dLocalOffset);
                            }
                        }
                    }
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        abstract float fold(int s0, int s1);

        abstract void emit(int d, int s0, int s1);
    }

    private abstract class InstructionGenerator_D_II {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S2, sreg0, sreg1);
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S2, S0, sreg1);
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                xasm.emit_stfd(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                xasm.emit_stfd(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, S0, sreg1);
                                xasm.emit_stfd(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                xasm.emit_stfd(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        double value = fold(const0.intValue(), const1.intValue());
                        long bits = Double.doubleToLongBits(value);

                        if (dreg > 0) {
                            xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize() + 4);
                            xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfd(dreg, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                        } else {
                            xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize() + 4);
                            xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfd(S2, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_stfd(S2, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        void generate_se() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dreg = write != null ? registers.l2r(dindex) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S2, sreg0, sreg1);
                                    if (write != null)
                                        xasm.emit_stfd(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    if (write != null)
                                        xasm.emit_stfd(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S2, S0, sreg1);
                                    if (write != null)
                                        xasm.emit_stfd(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    if (write != null)
                                        xasm.emit_stfd(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                if (write != null)
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                if (write != null)
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, S0, sreg1);
                                if (write != null)
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                if (write != null)
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        double value = fold(const0.intValue(), const1.intValue());
                        long bits = Double.doubleToLongBits(value);
                        if (write != null) {
                            if (dreg > 0) {
                                xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                                xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize() + 4);
                                xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                                xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize());
                                xasm.emit_lfd(dreg, SP, stackLayout.getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize());
                            } else {
                                xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                                xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize() + 4);
                                xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                                xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize());
                                xasm.emit_lfd(S2, SP, stackLayout.getFPRegisterOffset(S2)
                                        + stackLayout.getNativeFrameSize());
                                xasm.emit_stfd(S2, SP, dLocalOffset);
                            }
                        }
                    }
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        abstract double fold(int s0, int s1);

        abstract void emit(int d, int s0, int s1);
    }
    private abstract class InstructionGenerator__III {
        void generate() {
            VStack.Item item2 = vstack.pop();
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            try {
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    if (sreg2 > 0) {
                                        emit(sreg0, sreg1, sreg2);
                                    } else {
                                        xasm.emit_lwz(S2, SP, sLocalOffset2);
                                        emit(sreg0, sreg1, S2);
                                    }
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    if (sreg2 > 0) {
                                        emit(sreg0, S1, sreg2);
                                    } else {
                                        xasm.emit_lwz(S2, SP, sLocalOffset2);
                                        emit(sreg0, S1, S2);
                                    }
                                }
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                if (sreg1 > 0) {
                                    if (sreg2 > 0) {
                                        emit(S0, sreg1, sreg2);
                                    } else {
                                        xasm.emit_lwz(S2, SP, sLocalOffset2);
                                        emit(S0, sreg1, S2);
                                    }
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    if (sreg2 > 0) {
                                        emit(S0, S1, sreg2);
                                    } else {
                                        xasm.emit_lwz(S2, SP, sLocalOffset2);
                                        emit(S0, S1, S2);
                                    }
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            xasm.emit_li32(S2, const2.intValue());
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(sreg0, sreg1, S2);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(sreg0, S1, S2);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S0, sreg1, S2);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S0, S1, S2);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg0 > 0) {
                                if (sreg2 > 0) {
                                    emit(sreg0, S1, sreg2);
                                } else {
                                    xasm.emit_lwz(S2, SP, sLocalOffset2);
                                    emit(sreg0, S1, S2);
                                }
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                if (sreg2 > 0) {
                                    emit(S0, S1, sreg2);
                                } else {
                                    xasm.emit_lwz(S2, SP, sLocalOffset2);
                                    emit(S0, S1, S2);
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            xasm.emit_li32(S2, const2.intValue());
                            if (sreg0 > 0) {
                                emit(sreg0, S1, S2);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S0, S1, S2);
                            }
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    xasm.emit_li32(S0, const0.intValue());
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg1 > 0) {
                                if (sreg2 > 0) {
                                    emit(S0, sreg1, sreg2);
                                } else {
                                    xasm.emit_lwz(S2, SP, sLocalOffset2);
                                    emit(S0, sreg1, S2);
                                }
                            } else {
                                if (sreg2 > 0) {
                                    emit(S0, S1, sreg2);
                                } else {
                                    xasm.emit_lwz(S2, SP, sLocalOffset2);
                                    emit(S0, S1, S2);
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            xasm.emit_li32(S2, const2.intValue());
                            if (sreg1 > 0) {
                                emit(S0, sreg1, S2);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S0, S1, S2);
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg2 > 0) {
                                emit(S0, S1, sreg2);
                            } else {
                                xasm.emit_lwz(S2, SP, sLocalOffset2);
                                emit(S0, S1, S2);
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            xasm.emit_li32(S2, const2.intValue());
                            emit(S0, S1, S2);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }
        abstract void emit(int s0, int s1, int s2);
    }

    private abstract class InstructionGenerator__IIJ {
        void generate() {
            VStack.Item item2 = vstack.pop();
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            int S3 = Registers.getScratchGPR();
            try {
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S2, SP, sLocalOffset2);
                                    xasm.emit_lwz(S3, SP, sLocalOffset2 + 4);
                                    emit(sreg0, sreg1, S2, S3);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    xasm.emit_lwz(S2, SP, sLocalOffset2);
                                    xasm.emit_lwz(S3, SP, sLocalOffset2 + 4);
                                    emit(sreg0, S1, S2, S3);
                                }
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S2, SP, sLocalOffset2);
                                    xasm.emit_lwz(S3, SP, sLocalOffset2 + 4);
                                    emit(S0, sreg1, S2, S3);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    xasm.emit_lwz(S2, SP, sLocalOffset2);
                                    xasm.emit_lwz(S3, SP, sLocalOffset2 + 4);
                                    emit(S0, S1, S2, S3);
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            long value = const2.longValue();
                            xasm.emit_li32(S2, (int) ((value >> 32) & 0xFFFFFFFFL));
                            xasm.emit_li32(S3, (int) (value & 0xFFFFFFFFL));
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(sreg0, sreg1, S2, S3);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(sreg0, S1, S2, S3);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S0, sreg1, S2, S3);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S0, S1, S2, S3);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg0 > 0) {
                                xasm.emit_lwz(S2, SP, sLocalOffset2);
                                xasm.emit_lwz(S3, SP, sLocalOffset2 + 4);
                                emit(sreg0, S1, S2, S3);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                xasm.emit_lwz(S2, SP, sLocalOffset2);
                                xasm.emit_lwz(S3, SP, sLocalOffset2 + 4);
                                emit(S0, S1, S2, S3);
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            long value = const2.longValue();
                            xasm.emit_li32(S2, (int) ((value >> 32) & 0xFFFFFFFFL));
                            xasm.emit_li32(S3, (int) (value & 0xFFFFFFFFL));
                            if (sreg0 > 0) {
                                emit(sreg0, S1, S2, S3);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S0, S1, S2, S3);
                            }
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    xasm.emit_li32(S0, const0.intValue());
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg1 > 0) {
                                xasm.emit_lwz(S2, SP, sLocalOffset2);
                                xasm.emit_lwz(S3, SP, sLocalOffset2 + 4);
                                emit(S0, sreg1, S2, S3);
                            } else {
                                xasm.emit_lwz(S2, SP, sLocalOffset2);
                                xasm.emit_lwz(S3, SP, sLocalOffset2 + 4);
                                emit(S0, S1, S2, S3);
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            long value = const2.longValue();
                            xasm.emit_li32(S2, (int) ((value >> 32) & 0xFFFFFFFFL));
                            xasm.emit_li32(S3, (int) (value & 0xFFFFFFFFL));
                            if (sreg1 > 0) {
                                emit(S0, sreg1, S2, S3);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S0, S1, S2, S3);
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            xasm.emit_lwz(S2, SP, sLocalOffset2);
                            xasm.emit_lwz(S3, SP, sLocalOffset2 + 4);
                            emit(S0, S1, S2, S3);
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            long value = const2.longValue();
                            xasm.emit_li32(S2, (int) ((value >> 32) & 0xFFFFFFFFL));
                            xasm.emit_li32(S3, (int) (value & 0xFFFFFFFFL));
                            emit(S0, S1, S2, S3);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S3);
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }
        abstract void emit(int s0, int s1, int s2h, int s2l);
    }

    private abstract class InstructionGenerator__IIF {
        void generate() {
            VStack.Item item2 = vstack.pop();
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchFPR();
            try {
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    if (sreg2 > 0) {
                                        emit(sreg0, sreg1, sreg2);
                                    } else {
                                        xasm.emit_lfs(S2, SP, sLocalOffset2);
                                        emit(sreg0, sreg1, S2);
                                    }
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    if (sreg2 > 0) {
                                        emit(sreg0, S1, sreg2);
                                    } else {
                                        xasm.emit_lfs(S2, SP, sLocalOffset2);
                                        emit(sreg0, S1, S2);
                                    }
                                }
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                if (sreg1 > 0) {
                                    if (sreg2 > 0) {
                                        emit(S0, sreg1, sreg2);
                                    } else {
                                        xasm.emit_lfs(S2, SP, sLocalOffset2);
                                        emit(S0, sreg1, S2);
                                    }
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    if (sreg2 > 0) {
                                        emit(S0, S1, sreg2);
                                    } else {
                                        xasm.emit_lfs(S2, SP, sLocalOffset2);
                                        emit(S0, S1, S2);
                                    }
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            xasm.emit_li32(SI, Float.floatToIntBits(const2.floatValue()));
                            xasm.emit_stw(SI, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfs(S2, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(sreg0, sreg1, S2);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(sreg0, S1, S2);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S0, sreg1, S2);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S0, S1, S2);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg0 > 0) {
                                if (sreg2 > 0) {
                                    emit(sreg0, S1, sreg2);
                                } else {
                                    xasm.emit_lfs(S2, SP, sLocalOffset2);
                                    emit(sreg0, S1, S2);
                                }
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                if (sreg2 > 0) {
                                    emit(S0, S1, sreg2);
                                } else {
                                    xasm.emit_lfs(S2, SP, sLocalOffset2);
                                    emit(S0, S1, S2);
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            xasm.emit_li32(SI, Float.floatToIntBits(const2.floatValue()));
                            xasm.emit_stw(SI, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfs(S2, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            if (sreg0 > 0) {
                                emit(sreg0, S1, S2);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S0, S1, S2);
                            }
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    xasm.emit_li32(S0, const0.intValue());
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg1 > 0) {
                                if (sreg2 > 0) {
                                    emit(S0, sreg1, sreg2);
                                } else {
                                    xasm.emit_lfs(S2, SP, sLocalOffset2);
                                    emit(S0, sreg1, S2);
                                }
                            } else {
                                if (sreg2 > 0) {
                                    emit(S0, S1, sreg2);
                                } else {
                                    xasm.emit_lfs(S2, SP, sLocalOffset2);
                                    emit(S0, S1, S2);
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            xasm.emit_li32(SI, Float.floatToIntBits(const2.floatValue()));
                            xasm.emit_stw(SI, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfs(S2, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            if (sreg1 > 0) {
                                emit(S0, sreg1, S2);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S0, S1, S2);
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg2 > 0) {
                                emit(S0, S1, sreg2);
                            } else {
                                xasm.emit_lfs(S2, SP, sLocalOffset2);
                                emit(S0, S1, S2);
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            xasm.emit_li32(SI, Float.floatToIntBits(const2.floatValue()));
                            xasm.emit_stw(SI, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfs(S2, SP, stackLayout
                                    .getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            emit(S0, S1, S2);
                        }
                    }
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }
        abstract void emit(int s0, int s1, int s2);
    }

    private abstract class InstructionGenerator__IID {
        void generate() {
            VStack.Item item2 = vstack.pop();
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchFPR();
            try {
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    if (sreg2 > 0) {
                                        emit(sreg0, sreg1, sreg2);
                                    } else {
                                        xasm.emit_lfd(S2, SP, sLocalOffset2);
                                        emit(sreg0, sreg1, S2);
                                    }
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    if (sreg2 > 0) {
                                        emit(sreg0, S1, sreg2);
                                    } else {
                                        xasm.emit_lfd(S2, SP, sLocalOffset2);
                                        emit(sreg0, S1, S2);
                                    }
                                }
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                if (sreg1 > 0) {
                                    if (sreg2 > 0) {
                                        emit(S0, sreg1, sreg2);
                                    } else {
                                        xasm.emit_lfd(S2, SP, sLocalOffset2);
                                        emit(S0, sreg1, S2);
                                    }
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    if (sreg2 > 0) {
                                        emit(S0, S1, sreg2);
                                    } else {
                                        xasm.emit_lfd(S2, SP, sLocalOffset2);
                                        emit(S0, S1, S2);
                                    }
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            double value = const2.doubleValue();
                            long bits = Double.doubleToLongBits(value);
                            xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize() + 4);
                            xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfd(S2, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(sreg0, sreg1, S2);
                                } else {
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(sreg0, S1, S2);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    emit(S0, sreg1, S2);
                                } else {
                                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                                    xasm.emit_lwz(S1, SP, sLocalOffset1);
                                    emit(S0, S1, S2);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg0 > 0) {
                                if (sreg2 > 0) {
                                    emit(sreg0, S1, sreg2);
                                } else {
                                    xasm.emit_lfd(S2, SP, sLocalOffset2);
                                    emit(sreg0, S1, S2);
                                }
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                if (sreg2 > 0) {
                                    emit(S0, S1, sreg2);
                                } else {
                                    xasm.emit_lfd(S2, SP, sLocalOffset2);
                                    emit(S0, S1, S2);
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            double value = const2.doubleValue();
                            long bits = Double.doubleToLongBits(value);
                            xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize() + 4);
                            xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfd(S2, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            if (sreg0 > 0) {
                                emit(sreg0, S1, S2);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S0, S1, S2);
                            }
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    xasm.emit_li32(S0, const0.intValue());
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg1 > 0) {
                                if (sreg2 > 0) {
                                    emit(S0, sreg1, sreg2);
                                } else {
                                    xasm.emit_lfd(S2, SP, sLocalOffset2);
                                    emit(S0, sreg1, S2);
                                }
                            } else {
                                if (sreg2 > 0) {
                                    emit(S0, S1, sreg2);
                                } else {
                                    xasm.emit_lfd(S2, SP, sLocalOffset2);
                                    emit(S0, S1, S2);
                                }
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            double value = const2.doubleValue();
                            long bits = Double.doubleToLongBits(value);
                            xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize() + 4);
                            xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfd(S2, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            if (sreg1 > 0) {
                                emit(S0, sreg1, S2);
                            } else {
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S0, S1, S2);
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (item2 instanceof VStack.Local) {
                            VStack.Local local2 = (VStack.Local) item2;
                            int sreg2 = registers.l2r(local2.index());
                            int sLocalOffset2 = stackLayout
                                    .getLocalVariableNativeOffset(local2.index())
                                    + stackLayout.getNativeFrameSize();
                            if (sreg2 > 0) {
                                emit(S0, S1, sreg2);
                            } else {
                                xasm.emit_lfd(S2, SP, sLocalOffset2);
                                emit(S0, S1, S2);
                            }
                        } else {
                            VStack.Constant const2 = (VStack.Constant) item2;
                            double value = const2.doubleValue();
                            long bits = Double.doubleToLongBits(value);
                            xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize() + 4);
                            xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                            xasm.emit_stw(SI, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfd(S2, SP, stackLayout.getFPRegisterOffset(S2)
                                    + stackLayout.getNativeFrameSize());
                            emit(S0, S1, S2);
                        }
                    }
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }
        abstract void emit(int s0, int s1, int s2);
    }

    private abstract class InstructionGenerator_I_I {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(dreg, S0);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S1, sreg0);
                            xasm.emit_stw(S1, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S1, S0);
                            xasm.emit_stw(S1, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    int value = fold(const0.intValue());
                    if (dreg > 0) {
                        xasm.emit_li32(dreg, value);
                    } else {
                        xasm.emit_li32(S0, value);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        void generate_se() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dreg = write != null ? registers.l2r(dindex) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(dreg, S0);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S1, sreg0);
                            if (write != null)
                                xasm.emit_stw(S1, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S1, S0);
                            if (write != null)
                                xasm.emit_stw(S1, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    int value = fold(const0.intValue());
                    if (dreg > 0) {
                        xasm.emit_li32(dreg, value);
                    } else {
                        if (write != null) {
                            xasm.emit_li32(S0, value);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract int fold(int s0);

        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_A_I {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(dreg, S0);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S1, sreg0);
                            xasm.emit_stw(S1, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S1, S0);
                            xasm.emit_stw(S1, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    xasm.emit_li32(S1, const0.intValue());
                    if (dreg > 0) {
                        emit(dreg, S1);
                    } else {
                        emit(S0, S1);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }
        void generate_se() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dreg = write != null ? registers.l2r(dindex) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(dreg, S0);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S1, sreg0);
                            if (write != null)
                                xasm.emit_stw(S1, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S1, S0);
                            if (write != null)
                                xasm.emit_stw(S1, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    xasm.emit_li32(S1, const0.intValue());
                    if (dreg > 0) {
                        emit(dreg, S1);
                    } else {
                        emit(S0, S1);
                        if (write != null)
                            xasm.emit_stw(S0, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }
        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_I_ {
        void generate() {
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (dreg > 0) {
                    emit(dreg);
                } else {
                    emit(S0);
                    xasm.emit_stw(S0, SP, dLocalOffset);
                }
            } finally {
                Registers.returnScratchGPR(S0);
            }
        }
        void generate_se() {
            Instruction nextInstruction = nextInstruction();
            int S0 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dreg = write != null ? registers.l2r(dindex) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (dreg > 0) {
                    emit(dreg);
                } else {
                    emit(S0);
                    if (write != null)
                        xasm.emit_stw(S0, SP, dLocalOffset);
                }
            } finally {
                Registers.returnScratchGPR(S0);
            }
        }
        abstract void emit(int d);
    }

    private abstract class InstructionGenerator_J_ {
        void generate() {
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                emit(S0, S1);
                xasm.emit_stw(S0, SP, dLocalOffset);
                xasm.emit_stw(S1, SP, dLocalOffset + 4);
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }
        abstract void emit(int dh, int dl);
    }
    
    private abstract class InstructionGenerator_F_ {
        void generate() {
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (dreg > 0) {
                    emit(dreg);
                } else {
                    emit(S0);
                    xasm.emit_stfs(S0, SP, dLocalOffset);
                }
            } finally {
                Registers.returnScratchFPR(S0);
            }
        }
        abstract void emit(int d);
    }
    
    private abstract class InstructionGenerator_D_ {
        void generate() {
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (dreg > 0) {
                    emit(dreg);
                } else {
                    emit(S0);
                    xasm.emit_stfd(S0, SP, dLocalOffset);
                }
            } finally {
                Registers.returnScratchFPR(S0);
            }
        }
        abstract void emit(int d);
    }
    
    private abstract class InstructionGenerator_I_F {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lfs(S1, SP, sLocalOffset0);
                            emit(dreg, S1);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(S0, S1);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    int value = fold(const0.floatValue());
                    if (dreg > 0) {
                        xasm.emit_li32(dreg, value);
                    } else {
                        xasm.emit_li32(S0, value);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract int fold(float s0);

        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_I_D {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lfd(S1, SP, sLocalOffset0);
                            emit(dreg, S1);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(S0, S1);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    int value = fold(const0.doubleValue());
                    if (dreg > 0) {
                        xasm.emit_li32(dreg, value);
                    } else {
                        xasm.emit_li32(S0, value);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract int fold(double s0);

        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_I_J {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    xasm.emit_lwz(S1, SP, sLocalOffset0);
                    xasm.emit_lwz(S2, SP, sLocalOffset0 + 4);
                    if (dreg > 0) {
                        emit(dreg, S1, S2);
                    } else {
                        emit(S0, S1, S2);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    int value = fold(const0.longValue());
                    if (dreg > 0) {
                        xasm.emit_li32(dreg, value);
                    } else {
                        xasm.emit_li32(S0, value);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                    }

                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract int fold(long s0);

        abstract void emit(int d, int s0h, int s0l);
    }

    private abstract class InstructionGenerator__II {
        void generate() {
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                VStack.Item item1 = vstack.pop();
                VStack.Item item0 = vstack.pop();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg0 > 0) {
                            if (sreg1 > 0) {
                                emit(sreg0, sreg1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset1);
                                emit(sreg0, S0);
                            }
                        } else {
                            if (sreg1 > 0) {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S0, sreg1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                xasm.emit_lwz(S1, SP, sLocalOffset1);
                                emit(S0, S1);
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S1, const1.intValue());
                        if (sreg0 > 0) {
                            emit(sreg0, S1);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S0, S1);
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (sreg1 > 0) {
                            emit(S0, sreg1);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset1);
                            emit(S0, S1);
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        // Chance to do branch optimization
                        xasm.emit_li32(S0, const0.intValue());
                        xasm.emit_li32(S1, const1.intValue());
                        emit(S0, S1);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract void emit(int s0, int s1);
    }

    private abstract class InstructionGenerator__I {
        void generate() {
            int S0 = Registers.getScratchGPR();
            try {
                VStack.Item item0 = vstack.pop();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (sreg0 > 0) {
                        emit(sreg0);
                    } else {
                        xasm.emit_lwz(S0, SP, sLocalOffset0);
                        emit(S0);
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    // Chance to do branch optimization
                    xasm.emit_li32(S0, const0.intValue());
                    emit(S0);
                }
            } finally {
                Registers.returnScratchGPR(S0);
            }
        }

        abstract void emit(int s0);
    }

    private abstract class InstructionGenerator__J {
        void generate() {
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            try {
                VStack.Item item0 = vstack.pop();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    xasm.emit_lwz(S0, SP, sLocalOffset0);
                    xasm.emit_lwz(S1, SP, sLocalOffset0 + 4);
                    emit(S0, S1);
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    // Chance to do branch optimization
                    long value = const0.longValue();
                    xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                    xasm.emit_li32(S1, (int) (value & 0xFFFFFFFFL));
                    emit(S0, S1);
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract void emit(int s0h, int s0l);
    }

    private abstract class InstructionGenerator__F {
        void generate() {
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchFPR();
            try {
                VStack.Item item0 = vstack.pop();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (sreg0 > 0) {
                        emit(sreg0);
                    } else {
                        xasm.emit_lfs(S1, SP, sLocalOffset0);
                        emit(S1);
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    xasm.emit_li32(S0, Float
                            .floatToIntBits(const0.floatValue()));
                    xasm.emit_stw(S0, SP, stackLayout
                            .getGeneralRegisterOffset(S0)
                            + stackLayout.getNativeFrameSize());
                    xasm.emit_lfs(S1, SP, stackLayout
                            .getGeneralRegisterOffset(S0)
                            + stackLayout.getNativeFrameSize());
                    emit(S1);
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract void emit(int s0);
    }

    private abstract class InstructionGenerator__D {
        void generate() {
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchFPR();
            try {
                VStack.Item item0 = vstack.pop();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (sreg0 > 0) {
                        emit(sreg0);
                    } else {
                        xasm.emit_lfd(S1, SP, sLocalOffset0);
                        emit(S1);
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    double value = const0.doubleValue();
                    long bits = Double.doubleToLongBits(value);
                    xasm.emit_li32(S0, (int) (bits & 0xFFFFFFFFL));
                    xasm.emit_stw(S0, SP, stackLayout.getFPRegisterOffset(S1)
                            + stackLayout.getNativeFrameSize() + 4);
                    xasm.emit_li32(S0, (int) ((bits >> 32) & 0xFFFFFFFFL));
                    xasm.emit_stw(S0, SP, stackLayout.getFPRegisterOffset(S1)
                            + stackLayout.getNativeFrameSize());
                    xasm.emit_lfd(S1, SP, stackLayout.getFPRegisterOffset(S1)
                            + stackLayout.getNativeFrameSize());
                    emit(S1);
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract void emit(int s0);
    }

    private abstract class InstructionGenerator_J_JJ {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            int S3 = Registers.getScratchGPR();
            int S4 = Registers.getScratchGPR();
            int S5 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        xasm.emit_lwz(S2, SP, sLocalOffset0);
                        xasm.emit_lwz(S3, SP, sLocalOffset0 + 4);
                        xasm.emit_lwz(S4, SP, sLocalOffset1);
                        xasm.emit_lwz(S5, SP, sLocalOffset1 + 4);
                        emit(S0, S1, S2, S3, S4, S5);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value = const1.longValue();
                        xasm.emit_lwz(S2, SP, sLocalOffset0);
                        xasm.emit_lwz(S3, SP, sLocalOffset0 + 4);
                        xasm.emit_li32(S4, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S5, (int) (value & 0xFFFFFFFFL));
                        emit(S0, S1, S2, S3, S4, S5);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        long value = const0.longValue();
                        xasm.emit_li32(S2, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S3, (int) (value & 0xFFFFFFFFL));
                        xasm.emit_lwz(S4, SP, sLocalOffset1);
                        xasm.emit_lwz(S5, SP, sLocalOffset1 + 4);
                        emit(S0, S1, S2, S3, S4, S5);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value = fold(const0.longValue(), const1
                                .longValue());
                        xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S1, (int) (value & 0xFFFFFFFFL));
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S5);
                Registers.returnScratchGPR(S4);
                Registers.returnScratchGPR(S3);
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        void generate_se() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            int S3 = Registers.getScratchGPR();
            int S4 = Registers.getScratchGPR();
            int S5 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        xasm.emit_lwz(S2, SP, sLocalOffset0);
                        xasm.emit_lwz(S3, SP, sLocalOffset0 + 4);
                        xasm.emit_lwz(S4, SP, sLocalOffset1);
                        xasm.emit_lwz(S5, SP, sLocalOffset1 + 4);
                        emit(S0, S1, S2, S3, S4, S5);
                        if (write != null) {
                            xasm.emit_stw(S0, SP, dLocalOffset);
                            xasm.emit_stw(S1, SP, dLocalOffset + 4);
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value = const1.longValue();
                        xasm.emit_lwz(S2, SP, sLocalOffset0);
                        xasm.emit_lwz(S3, SP, sLocalOffset0 + 4);
                        xasm.emit_li32(S4, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S5, (int) (value & 0xFFFFFFFFL));
                        emit(S0, S1, S2, S3, S4, S5);
                        if (write != null) {
                            xasm.emit_stw(S0, SP, dLocalOffset);
                            xasm.emit_stw(S1, SP, dLocalOffset + 4);
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        long value = const0.longValue();
                        xasm.emit_li32(S2, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S3, (int) (value & 0xFFFFFFFFL));
                        xasm.emit_lwz(S4, SP, sLocalOffset1);
                        xasm.emit_lwz(S5, SP, sLocalOffset1 + 4);
                        emit(S0, S1, S2, S3, S4, S5);
                        if (write != null) {
                            xasm.emit_stw(S0, SP, dLocalOffset);
                            xasm.emit_stw(S1, SP, dLocalOffset + 4);
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value = fold(const0.longValue(), const1
                                .longValue());
                        if (write != null) {
                            xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                            xasm.emit_li32(S1, (int) (value & 0xFFFFFFFFL));
                            xasm.emit_stw(S0, SP, dLocalOffset);
                            xasm.emit_stw(S1, SP, dLocalOffset + 4);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S5);
                Registers.returnScratchGPR(S4);
                Registers.returnScratchGPR(S3);
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract long fold(long s0, long s1);

        abstract void emit(int dh, int dl, int s0h, int s0l, int s1h, int s1l);
    }

    private abstract class InstructionGenerator_J_JI {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            int S3 = Registers.getScratchGPR();
            int S4 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        xasm.emit_lwz(S2, SP, sLocalOffset0);
                        xasm.emit_lwz(S3, SP, sLocalOffset0 + 4);
                        if (sreg1 > 0) {
                            emit(S0, S1, S2, S3, sreg1);
                        } else {
                            xasm.emit_lwz(S4, SP, sLocalOffset1);
                            emit(S0, S1, S2, S3, S4);
                        }
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        int value = const1.intValue();
                        xasm.emit_lwz(S2, SP, sLocalOffset0);
                        xasm.emit_lwz(S3, SP, sLocalOffset0 + 4);
                        xasm.emit_li32(S4, value);
                        emit(S0, S1, S2, S3, S4);
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        long value = const0.longValue();
                        xasm.emit_li32(S2, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S3, (int) (value & 0xFFFFFFFFL));
                        if (sreg1 > 0) {
                            emit(S0, S1, S2, S3, sreg1);
                        } else {
                            xasm.emit_lwz(S4, SP, sLocalOffset1);
                            emit(S0, S1, S2, S3, S4);
                        }
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value = fold(const0.longValue(), const1.intValue());
                        xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S1, (int) (value & 0xFFFFFFFFL));
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S4);
                Registers.returnScratchGPR(S3);
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract long fold(long s0, int s1);

        abstract void emit(int dh, int dl, int s0h, int s0l, int s1);
    }

    private abstract class InstructionGenerator_J_J {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            int S3 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    xasm.emit_lwz(S2, SP, sLocalOffset0);
                    xasm.emit_lwz(S3, SP, sLocalOffset0 + 4);
                    emit(S0, S1, S2, S3);
                    xasm.emit_stw(S0, SP, dLocalOffset);
                    xasm.emit_stw(S1, SP, dLocalOffset + 4);
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    long value = fold(const0.longValue());
                    xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                    xasm.emit_li32(S1, (int) (value & 0xFFFFFFFFL));
                    xasm.emit_stw(S0, SP, dLocalOffset);
                    xasm.emit_stw(S1, SP, dLocalOffset + 4);
                }
            } finally {
                Registers.returnScratchGPR(S3);
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract long fold(long s0);

        abstract void emit(int dh, int dl, int s0h, int s0l);
    }

    private abstract class InstructionGenerator_J_I {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    int sreg = registers.l2r(local0.index());
                    if (sreg > 0) {
                        emit(S0, S1, sreg);
                    } else {
                        xasm.emit_lwz(S2, SP, sLocalOffset0);
                        emit(S0, S1, S2);
                    }
                    xasm.emit_stw(S0, SP, dLocalOffset);
                    xasm.emit_stw(S1, SP, dLocalOffset + 4);
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    long value = fold(const0.intValue());
                    xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                    xasm.emit_li32(S1, (int) (value & 0xFFFFFFFFL));
                    xasm.emit_stw(S0, SP, dLocalOffset);
                    xasm.emit_stw(S1, SP, dLocalOffset + 4);
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        void generate_se() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    int sreg = registers.l2r(local0.index());
                    if (sreg > 0) {
                        emit(S0, S1, sreg);
                    } else {
                        xasm.emit_lwz(S2, SP, sLocalOffset0);
                        emit(S0, S1, S2);
                    }
                    if (write != null) {
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    long value = fold(const0.intValue());
                    if (write != null) {
                        xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S1, (int) (value & 0xFFFFFFFFL));
                        xasm.emit_stw(S0, SP, dLocalOffset);
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }
        abstract long fold(int s0);

        abstract void emit(int dh, int dl, int s0);
    }

    private abstract class InstructionGenerator_J_F {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (sreg0 > 0) {
                        emit(S0, S1, sreg0);
                    } else {
                        xasm.emit_lfs(S2, SP, sLocalOffset0);
                        emit(S0, S1, S2);
                    }
                    xasm.emit_stw(S0, SP, dLocalOffset);
                    xasm.emit_stw(S1, SP, dLocalOffset + 4);
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    long value = fold(const0.floatValue());
                    xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                    xasm.emit_li32(S1, (int) (value & 0xFFFFFFFFL));
                    xasm.emit_stw(S0, SP, dLocalOffset);
                    xasm.emit_stw(S1, SP, dLocalOffset + 4);
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract long fold(float s0);

        abstract void emit(int dh, int dl, int s0);
    }

    private abstract class InstructionGenerator_J_D {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (sreg0 > 0) {
                        emit(S0, S1, sreg0);
                    } else {
                        xasm.emit_lfd(S2, SP, sLocalOffset0);
                        emit(S0, S1, S2);
                    }
                    xasm.emit_stw(S0, SP, dLocalOffset);
                    xasm.emit_stw(S1, SP, dLocalOffset + 4);
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    long value = fold(const0.doubleValue());
                    xasm.emit_li32(S0, (int) ((value >> 32) & 0xFFFFFFFFL));
                    xasm.emit_li32(S1, (int) (value & 0xFFFFFFFFL));
                    xasm.emit_stw(S0, SP, dLocalOffset);
                    xasm.emit_stw(S1, SP, dLocalOffset + 4);
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract long fold(double s0);

        abstract void emit(int dh, int dl, int s0);
    }

    private abstract class InstructionGenerator_I_JJ {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            int S3 = Registers.getScratchGPR();
            int S4 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        xasm.emit_lwz(S1, SP, sLocalOffset0);
                        xasm.emit_lwz(S2, SP, sLocalOffset0 + 4);
                        xasm.emit_lwz(S3, SP, sLocalOffset1);
                        xasm.emit_lwz(S4, SP, sLocalOffset1 + 4);
                        if (dreg > 0) {
                            emit(dreg, S1, S2, S3, S4);
                        } else {
                            emit(S0, S1, S2, S3, S4);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value = const1.longValue();
                        xasm.emit_lwz(S1, SP, sLocalOffset0);
                        xasm.emit_lwz(S2, SP, sLocalOffset0 + 4);
                        xasm.emit_li32(S3, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S4, (int) (value & 0xFFFFFFFFL));
                        if (dreg > 0) {
                            emit(dreg, S1, S2, S3, S4);
                        } else {
                            emit(S0, S1, S2, S3, S4);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        long value = const0.longValue();
                        xasm.emit_li32(S1, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S2, (int) (value & 0xFFFFFFFFL));
                        xasm.emit_lwz(S3, SP, sLocalOffset1);
                        xasm.emit_lwz(S4, SP, sLocalOffset1 + 4);
                        if (dreg > 0) {
                            emit(dreg, S1, S2, S3, S4);
                        } else {
                            emit(S0, S1, S2, S3, S4);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        int value = fold(const0.longValue(), const1.longValue());
                        if (dreg > 0) {
                            xasm.emit_li32(dreg, value);
                        } else {
                            xasm.emit_li32(S0, value);
                            xasm.emit_stw(S0, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S4);
                Registers.returnScratchGPR(S3);
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract int fold(long s0, long s1);

        abstract void emit(int d, int s0h, int s0l, int s1h, int s1l);
    }

    private abstract class InstructionGenerator__IJ {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            try {
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg0 > 0) {
                            xasm.emit_lwz(S1, SP, sLocalOffset1);
                            xasm.emit_lwz(S2, SP, sLocalOffset1 + 4);
                            emit(sreg0, S1, S2);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            xasm.emit_lwz(S1, SP, sLocalOffset1);
                            xasm.emit_lwz(S2, SP, sLocalOffset1 + 4);
                            emit(S0, S1, S2);
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value = const1.longValue();
                        xasm.emit_li32(S1, (int) ((value >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S2, (int) (value & 0xFFFFFFFFL));
                        if (sreg0 > 0) {
                            emit(sreg0, S1, S2);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S0, S1, S2);
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        int value = const0.intValue();
                        xasm.emit_li32(S0, value);
                        xasm.emit_lwz(S1, SP, sLocalOffset1);
                        xasm.emit_lwz(S2, SP, sLocalOffset1 + 4);
                        emit(S0, S1, S2);
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long value1 = const1.longValue();
                        xasm.emit_li32(S0, const0.intValue());
                        xasm.emit_li32(S1, (int) ((value1 >> 32) & 0xFFFFFFFFL));
                        xasm.emit_li32(S2, (int) (value1 & 0xFFFFFFFFL));
                        emit(S0, S1, S2);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchGPR(S0);
            }
        }

        abstract void emit(int s0, int s1h, int s1l);
    }

    private abstract class InstructionGenerator_F_FF {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchFPR();
            int S2 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lfs(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lfs(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lfs(S0, SP, sLocalOffset0);
                                    xasm.emit_lfs(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S0, sreg0, sreg1);
                                    xasm.emit_stfs(S0, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lfs(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lfs(S1, SP, sLocalOffset0);
                                    emit(S2, S1, sreg1);
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lfs(S0, SP, sLocalOffset0);
                                    xasm.emit_lfs(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    xasm.emit_stfs(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(SI, Float.floatToIntBits(const1
                                .floatValue()));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(S1, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lfs(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                xasm.emit_stfs(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lfs(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                xasm.emit_stfs(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(SI, Float.floatToIntBits(const0
                                .floatValue()));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(S0, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lfs(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, S0, sreg1);
                                xasm.emit_stfs(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lfs(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                xasm.emit_stfs(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        float value = fold(const0.floatValue(), const1
                                .floatValue());
                        xasm.emit_li32(SI, Float.floatToIntBits(value));
                        if (dreg > 0) {
                            xasm.emit_stw(SI, SP, stackLayout
                                    .getGeneralRegisterOffset(SI)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfs(dreg, SP, stackLayout
                                    .getGeneralRegisterOffset(SI)
                                    + stackLayout.getNativeFrameSize());
                        } else {
                            xasm.emit_stw(SI, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchFPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        abstract float fold(float s0, float s1);

        abstract void emit(int d, int s0, int s1);
    }

    private abstract class InstructionGenerator_F_F {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lfs(S0, SP, sLocalOffset0);
                            emit(dreg, S0);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            xasm.emit_stfs(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lfs(S0, SP, sLocalOffset0);
                            emit(S1, S0);
                            xasm.emit_stfs(S1, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    float value = fold(const0.floatValue());
                    xasm.emit_li32(SI, Float.floatToIntBits(value));
                    if (dreg > 0) {
                        xasm.emit_stw(SI, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(dreg, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                    } else {
                        xasm.emit_stw(SI, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        abstract float fold(float s0);

        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_F_I {
        void generate_se() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dreg = write != null ? registers.l2r(dindex) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(dreg, S1);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            if (write != null)
                                xasm.emit_stfs(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(S0, S1);
                            if (write != null)
                                xasm.emit_stfs(S0, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    float value = fold(const0.intValue());
                    xasm.emit_li32(S1, Float.floatToIntBits(value));
                    if (write != null) {
                        if (dreg > 0) {
                            xasm.emit_stw(S1, SP, stackLayout
                                    .getGeneralRegisterOffset(S1)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfs(dreg, SP, stackLayout
                                    .getGeneralRegisterOffset(S1)
                                    + stackLayout.getNativeFrameSize());
                        } else {
                            xasm.emit_stw(S1, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchFPR(S0);
            }
        }

        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(dreg, S1);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            xasm.emit_stfs(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(S0, S1);
                            xasm.emit_stfs(S0, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    float value = fold(const0.intValue());
                    xasm.emit_li32(S1, Float.floatToIntBits(value));
                    if (dreg > 0) {
                        xasm.emit_stw(S1, SP, stackLayout
                                .getGeneralRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(dreg, SP, stackLayout
                                .getGeneralRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                    } else {
                        xasm.emit_stw(S1, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchFPR(S0);
            }
        }

        abstract float fold(int s0);

        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_F_J {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    xasm.emit_lwz(S1, SP, sLocalOffset0);
                    xasm.emit_lwz(S2, SP, sLocalOffset0 + 4);
                    if (dreg > 0) {
                        emit(dreg, S1, S2);
                    } else {
                        emit(S0, S1, S2);
                        xasm.emit_stfs(S0, SP, dLocalOffset);
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    float value = fold(const0.longValue());
                    xasm.emit_li32(S1, Float.floatToIntBits(value));
                    if (dreg > 0) {
                        xasm.emit_stw(S1, SP, stackLayout
                                .getGeneralRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(dreg, SP, stackLayout
                                .getGeneralRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                    } else {
                        xasm.emit_stw(S1, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchFPR(S0);
            }
        }

        abstract float fold(long s0);

        abstract void emit(int d, int s0h, int s0l);
    }

    private abstract class InstructionGenerator_F_D {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lfd(S1, SP, sLocalOffset0);
                            emit(dreg, S1);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            xasm.emit_stfs(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lfd(S1, SP, sLocalOffset0);
                            emit(S0, S1);
                            xasm.emit_stfs(S0, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    float value = fold(const0.doubleValue());
                    xasm.emit_li32(SI, Float.floatToIntBits(value));
                    if (dreg > 0) {
                        xasm.emit_stw(SI, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(dreg, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                    } else {
                        xasm.emit_stw(SI, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        abstract float fold(double s0);

        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_D_DD {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int SI0 = Registers.getScratchGPR();
            int SI1 = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchFPR();
            int S2 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lfd(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lfd(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lfd(S0, SP, sLocalOffset0);
                                    xasm.emit_lfd(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S0, sreg0, sreg1);
                                    xasm.emit_stfd(S0, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lfd(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lfd(S1, SP, sLocalOffset0);
                                    emit(S2, S1, sreg1);
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lfd(S0, SP, sLocalOffset0);
                                    xasm.emit_lfd(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long bits = Double.doubleToLongBits(const1
                                .doubleValue());
                        xasm.emit_li32(SI0, (int) (bits & 0xFFFFFFFFL));
                        xasm.emit_li32(SI1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                        xasm.emit_stw(SI0, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_stw(SI1, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(S1, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lfd(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                xasm.emit_stfd(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lfd(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                xasm.emit_stfd(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        long bits = Double.doubleToLongBits(const0
                                .doubleValue());
                        xasm.emit_li32(SI0, (int) (bits & 0xFFFFFFFFL));
                        xasm.emit_li32(SI1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                        xasm.emit_stw(SI0, SP, stackLayout
                                .getFPRegisterOffset(S0)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_stw(SI1, SP, stackLayout
                                .getFPRegisterOffset(S0)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(S0, SP, stackLayout
                                .getFPRegisterOffset(S0)
                                + stackLayout.getNativeFrameSize());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lfd(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, S0, sreg1);
                                xasm.emit_stfd(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lfd(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                xasm.emit_stfd(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        double value = fold(const0.doubleValue(), const1
                                .doubleValue());
                        long bits = Double.doubleToLongBits(value);
                        xasm.emit_li32(SI0, (int) (bits & 0xFFFFFFFFL));
                        xasm.emit_li32(SI1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                        if (dreg > 0) {
                            xasm.emit_stw(SI0, SP, stackLayout
                                    .getFPRegisterOffset(dreg)
                                    + stackLayout.getNativeFrameSize() + 4);
                            xasm.emit_stw(SI1, SP, stackLayout
                                    .getFPRegisterOffset(dreg)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfd(dreg, SP, stackLayout
                                    .getFPRegisterOffset(dreg)
                                    + stackLayout.getNativeFrameSize());
                        } else {
                            xasm.emit_stw(SI0, SP, dLocalOffset + 4);
                            xasm.emit_stw(SI1, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchFPR(S2);
                Registers.returnScratchFPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI1);
                Registers.returnScratchGPR(SI0);
            }
        }

        abstract double fold(double s0, double s1);

        abstract void emit(int d, int s0, int s1);
    }

    private abstract class InstructionGenerator_D_D {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int SI0 = Registers.getScratchGPR();
            int SI1 = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lfd(S0, SP, sLocalOffset0);
                            emit(dreg, S0);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            xasm.emit_stfd(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lfd(S0, SP, sLocalOffset0);
                            emit(S1, S0);
                            xasm.emit_stfd(S1, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    double value = fold(const0.doubleValue());
                    long bits = Double.doubleToLongBits(value);
                    xasm.emit_li32(SI0, (int) (bits & 0xFFFFFFFFL));
                    xasm.emit_li32(SI1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                    if (dreg > 0) {
                        xasm.emit_stw(SI0, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_stw(SI1, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(dreg, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize());
                    } else {
                        xasm.emit_stw(SI0, SP, dLocalOffset + 4);
                        xasm.emit_stw(SI1, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI1);
                Registers.returnScratchGPR(SI0);
            }
        }

        abstract double fold(double s0);

        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_D_I {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int SI0 = Registers.getScratchGPR();
            int SI1 = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(dreg, S1);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            xasm.emit_stfd(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(S0, S1);
                            xasm.emit_stfd(S0, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    double value = fold(const0.intValue());
                    long bits = Double.doubleToLongBits(value);
                    xasm.emit_li32(SI0, (int) (bits & 0xFFFFFFFFL));
                    xasm.emit_li32(SI1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                    if (dreg > 0) {
                        xasm.emit_stw(SI0, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_stw(SI1, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(dreg, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize());
                    } else {
                        xasm.emit_stw(SI0, SP, dLocalOffset + 4);
                        xasm.emit_stw(SI1, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI1);
                Registers.returnScratchGPR(SI0);
            }
        }

        void generate_se() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            int SI0 = Registers.getScratchGPR();
            int SI1 = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = null;
                if (nextInstruction instanceof Instruction.LocalWrite) {
                    write = (Instruction.LocalWrite) nextInstruction;
                }
                int dindex = write != null ? write.getLocalVariableOffset(buf) : -1;
                int dreg = write != null ? registers.l2r(dindex) : -1;
                int dLocalOffset = write != null ? stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize() : -1;
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(dreg, S1);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            if (write != null)
                                xasm.emit_stfd(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lwz(S1, SP, sLocalOffset0);
                            emit(S0, S1);
                            if (write != null)
                                xasm.emit_stfd(S0, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    double value = fold(const0.intValue());
                    long bits = Double.doubleToLongBits(value);
                    if (write != null) {
                        xasm.emit_li32(SI0, (int) (bits & 0xFFFFFFFFL));
                        xasm.emit_li32(SI1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                        if (dreg > 0) {
                            
                            xasm.emit_stw(SI0, SP, stackLayout
                                    .getFPRegisterOffset(dreg)
                                    + stackLayout.getNativeFrameSize() + 4);
                            xasm.emit_stw(SI1, SP, stackLayout
                                    .getFPRegisterOffset(dreg)
                                    + stackLayout.getNativeFrameSize());
                            xasm.emit_lfd(dreg, SP, stackLayout
                                    .getFPRegisterOffset(dreg)
                                    + stackLayout.getNativeFrameSize());
                        } else {
                            xasm.emit_stw(SI0, SP, dLocalOffset + 4);
                            xasm.emit_stw(SI1, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI1);
                Registers.returnScratchGPR(SI0);
            }
        }

        abstract double fold(int s0);

        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_D_F {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int SI0 = Registers.getScratchGPR();
            int SI1 = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchFPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (dreg > 0) {
                        if (sreg0 > 0) {
                            emit(dreg, sreg0);
                        } else {
                            xasm.emit_lfs(S1, SP, sLocalOffset0);
                            emit(dreg, S1);
                        }
                    } else {
                        if (sreg0 > 0) {
                            emit(S0, sreg0);
                            xasm.emit_stfd(S0, SP, dLocalOffset);
                        } else {
                            xasm.emit_lfs(S1, SP, sLocalOffset0);
                            emit(S0, S1);
                            xasm.emit_stfd(S0, SP, dLocalOffset);
                        }
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    double value = fold(const0.floatValue());
                    long bits = Double.doubleToLongBits(value);
                    xasm.emit_li32(SI0, (int) (bits & 0xFFFFFFFFL));
                    xasm.emit_li32(SI1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                    if (dreg > 0) {
                        xasm.emit_stw(SI0, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_stw(SI1, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(dreg, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize());
                    } else {
                        xasm.emit_stw(SI0, SP, dLocalOffset + 4);
                        xasm.emit_stw(SI1, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI1);
                Registers.returnScratchGPR(SI0);
            }
        }

        abstract double fold(float s0);

        abstract void emit(int d, int s0);
    }

    private abstract class InstructionGenerator_D_J {
        void generate() {
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP2) {
                return;
            }
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchGPR();
            int S2 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    xasm.emit_lwz(S1, SP, sLocalOffset0);
                    xasm.emit_lwz(S2, SP, sLocalOffset0 + 4);
                    if (dreg > 0) {
                        emit(dreg, S1, S2);
                    } else {
                        emit(S0, S1, S2);
                        xasm.emit_stfd(S0, SP, dLocalOffset);
                    }
                } else {
                    VStack.Constant const0 = (VStack.Constant) item0;
                    double value = fold(const0.longValue());
                    long bits = Double.doubleToLongBits(value);
                    xasm.emit_li32(S1, (int) (bits & 0xFFFFFFFFL));
                    xasm.emit_li32(S2, (int) ((bits >> 32) & 0xFFFFFFFFL));
                    if (dreg > 0) {
                        xasm.emit_stw(S1, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_stw(S2, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(dreg, SP, stackLayout
                                .getFPRegisterOffset(dreg)
                                + stackLayout.getNativeFrameSize());
                    } else {
                        xasm.emit_stw(S1, SP, dLocalOffset + 4);
                        xasm.emit_stw(S2, SP, dLocalOffset);
                    }
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchGPR(S1);
                Registers.returnScratchFPR(S0);
            }
        }

        abstract double fold(long s0);

        abstract void emit(int d, int s0h, int s0l);
    }

    private abstract class InstructionGenerator_I_FF {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchFPR();
            int S2 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lfs(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lfs(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lfs(S0, SP, sLocalOffset0);
                                    xasm.emit_lfs(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S2, sreg0, S0);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lfs(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lfs(S1, SP, sLocalOffset0);
                                    emit(S2, S1, sreg1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lfs(S0, SP, sLocalOffset0);
                                    xasm.emit_lfs(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(SI, Float.floatToIntBits(const1
                                .floatValue()));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(S1, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lfs(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lfs(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(SI, Float.floatToIntBits(const0
                                .floatValue()));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(S0, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lfs(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, sreg1, S0);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lfs(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        int value = fold(const0.floatValue(), const1
                                .floatValue());
                        if (dreg > 0) {
                            xasm.emit_li32(dreg, value);
                        } else {
                            xasm.emit_li32(SI, value);
                            xasm.emit_stw(SI, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchFPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        abstract int fold(float s0, float s1);

        abstract void emit(int d, int s0, int s1);
    }

    private abstract class InstructionGenerator__IF {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchFPR();
            try {
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg0 > 0) {
                            if (sreg1 > 0) {
                                emit(sreg0, sreg1);
                            } else {
                                xasm.emit_lfs(S1, SP, sLocalOffset1);
                                emit(sreg0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S0, sreg1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                xasm.emit_lfs(S1, SP, sLocalOffset1);
                                emit(S0, S1);
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(SI, Float.floatToIntBits(const1
                                .floatValue()));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(S1, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        if (sreg0 > 0) {
                            emit(sreg0, S1);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S0, S1);
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (sreg1 > 0) {
                            emit(S0, sreg1);
                        } else {
                            xasm.emit_lfs(S1, SP, sLocalOffset1);
                            emit(S0, S1);
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S0, const0.intValue());
                        xasm.emit_li32(SI, Float.floatToIntBits(const1
                                .floatValue()));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfs(S1, SP, stackLayout
                                .getGeneralRegisterOffset(SI)
                                + stackLayout.getNativeFrameSize());
                        emit(S0, S1);
                    }
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchGPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        abstract void emit(int s0, int s1);
    }

    private abstract class InstructionGenerator_I_DD {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            Instruction nextInstruction = nextInstruction();
            if (nextInstruction instanceof Instruction.POP) {
                return;
            }
            int SI0 = Registers.getScratchGPR();
            int SI1 = Registers.getScratchGPR();
            int S0 = Registers.getScratchFPR();
            int S1 = Registers.getScratchFPR();
            int S2 = Registers.getScratchGPR();
            try {
                Instruction.LocalWrite write = (Instruction.LocalWrite) nextInstruction;
                int dindex = write.getLocalVariableOffset(buf);
                int dreg = registers.l2r(dindex);
                int dLocalOffset = stackLayout
                        .getLocalVariableNativeOffset(dindex)
                        + stackLayout.getNativeFrameSize();
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(dreg, sreg0, sreg1);
                                } else {
                                    xasm.emit_lfd(S0, SP, sLocalOffset1);
                                    emit(dreg, sreg0, S0);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lfd(S0, SP, sLocalOffset0);
                                    emit(dreg, S0, sreg1);
                                } else {
                                    xasm.emit_lfd(S0, SP, sLocalOffset0);
                                    xasm.emit_lfd(S1, SP, sLocalOffset1);
                                    emit(dreg, S0, S1);
                                }
                            }
                        } else {
                            if (sreg0 > 0) {
                                if (sreg1 > 0) {
                                    emit(S2, sreg0, sreg1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lfd(S1, SP, sLocalOffset1);
                                    emit(S2, sreg0, S1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                }
                            } else {
                                if (sreg1 > 0) {
                                    xasm.emit_lfd(S1, SP, sLocalOffset0);
                                    emit(S2, S1, sreg1);
                                    xasm.emit_stfd(S2, SP, dLocalOffset);
                                } else {
                                    xasm.emit_lfd(S0, SP, sLocalOffset0);
                                    xasm.emit_lfd(S1, SP, sLocalOffset1);
                                    emit(S2, S0, S1);
                                    xasm.emit_stw(S2, SP, dLocalOffset);
                                }
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long bits = Double.doubleToLongBits(const1
                                .doubleValue());
                        xasm.emit_li32(SI0, (int) (bits & 0xFFFFFFFFL));
                        xasm.emit_li32(SI1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                        xasm.emit_stw(SI0, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_stw(SI1, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(S1, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        if (dreg > 0) {
                            if (sreg0 > 0) {
                                emit(dreg, sreg0, S1);
                            } else {
                                xasm.emit_lfd(S0, SP, sLocalOffset0);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg0 > 0) {
                                emit(S2, sreg0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lfd(S0, SP, sLocalOffset0);
                                emit(S2, S0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            }
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        long bits = Double.doubleToLongBits(const0
                                .doubleValue());
                        xasm.emit_li32(SI0, (int) (bits & 0xFFFFFFFFL));
                        xasm.emit_li32(SI1, (int) ((bits >> 32) & 0xFFFFFFFFL));
                        xasm.emit_stw(SI0, SP, stackLayout
                                .getFPRegisterOffset(S0)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_stw(SI1, SP, stackLayout
                                .getFPRegisterOffset(S0)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(S0, SP, stackLayout
                                .getFPRegisterOffset(S0)
                                + stackLayout.getNativeFrameSize());
                        if (dreg > 0) {
                            if (sreg1 > 0) {
                                emit(dreg, S0, sreg1);
                            } else {
                                xasm.emit_lfd(S1, SP, sLocalOffset1);
                                emit(dreg, S0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                emit(S2, S0, sreg1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            } else {
                                xasm.emit_lfd(S1, SP, sLocalOffset1);
                                emit(S2, S0, S1);
                                xasm.emit_stw(S2, SP, dLocalOffset);
                            }
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        int value = fold(const0.doubleValue(), const1
                                .doubleValue());
                        if (dreg > 0) {
                            xasm.emit_li32(dreg, value);
                        } else {
                            xasm.emit_li32(SI0, value);
                            xasm.emit_stw(SI0, SP, dLocalOffset);
                        }
                    }
                }
            } finally {
                Registers.returnScratchGPR(S2);
                Registers.returnScratchFPR(S1);
                Registers.returnScratchFPR(S0);
                Registers.returnScratchGPR(SI1);
                Registers.returnScratchGPR(SI0);
            }
        }

        abstract int fold(double s0, double s1);

        abstract void emit(int d, int s0, int s1);
    }
    
    private abstract class InstructionGenerator__ID {
        void generate() {
            VStack.Item item1 = vstack.pop();
            VStack.Item item0 = vstack.pop();
            int SI = Registers.getScratchGPR();
            int S0 = Registers.getScratchGPR();
            int S1 = Registers.getScratchFPR();
            try {
                if (item0 instanceof VStack.Local) {
                    VStack.Local local0 = (VStack.Local) item0;
                    int sreg0 = registers.l2r(local0.index());
                    int sLocalOffset0 = stackLayout
                            .getLocalVariableNativeOffset(local0.index())
                            + stackLayout.getNativeFrameSize();
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        if (sreg0 > 0) {
                            if (sreg1 > 0) {
                                emit(sreg0, sreg1);
                            } else {
                                xasm.emit_lfd(S1, SP, sLocalOffset1);
                                emit(sreg0, S1);
                            }
                        } else {
                            if (sreg1 > 0) {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                emit(S0, sreg1);
                            } else {
                                xasm.emit_lwz(S0, SP, sLocalOffset0);
                                xasm.emit_lfd(S1, SP, sLocalOffset1);
                                emit(S0, S1);
                            }
                        }
                    } else {
                        VStack.Constant const1 = (VStack.Constant) item1;
                        long bits = Double.doubleToLongBits(const1.doubleValue());
                        xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(S1, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        if (sreg0 > 0) {
                            emit(sreg0, S1);
                        } else {
                            xasm.emit_lwz(S0, SP, sLocalOffset0);
                            emit(S0, S1);
                        }
                    }
                } else {
                    if (item1 instanceof VStack.Local) {
                        VStack.Local local1 = (VStack.Local) item1;
                        int sreg1 = registers.l2r(local1.index());
                        int sLocalOffset1 = stackLayout
                                .getLocalVariableNativeOffset(local1.index())
                                + stackLayout.getNativeFrameSize();
                        VStack.Constant const0 = (VStack.Constant) item0;
                        xasm.emit_li32(S0, const0.intValue());
                        if (sreg1 > 0) {
                            emit(S0, sreg1);
                        } else {
                            xasm.emit_lfd(S1, SP, sLocalOffset1);
                            emit(S0, S1);
                        }
                    } else {
                        VStack.Constant const0 = (VStack.Constant) item0;
                        VStack.Constant const1 = (VStack.Constant) item1;
                        xasm.emit_li32(S0, const0.intValue());
                        long bits = Double.doubleToLongBits(const1.doubleValue());
                        xasm.emit_li32(SI, (int) (bits & 0xFFFFFFFFL));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize() + 4);
                        xasm.emit_li32(SI, (int) ((bits >> 32) & 0xFFFFFFFFL));
                        xasm.emit_stw(SI, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        xasm.emit_lfd(S1, SP, stackLayout
                                .getFPRegisterOffset(S1)
                                + stackLayout.getNativeFrameSize());
                        emit(S0, S1);
                    }
                }
            } finally {
                Registers.returnScratchFPR(S1);
                Registers.returnScratchGPR(S0);
                Registers.returnScratchGPR(SI);
            }
        }

        abstract void emit(int s0, int s1);
    }
}
