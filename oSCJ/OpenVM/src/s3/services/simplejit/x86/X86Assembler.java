package s3.services.simplejit.x86;


import ovm.util.FixedByteBuffer;
import s3.services.simplejit.Assembler;
import ovm.core.services.memory.VM_Address;

/**
 * @author Hiroshi Yamauchi
 **/
public class X86Assembler
    extends Assembler 
    implements X86Constants {



    public X86Assembler(FixedByteBuffer cb, int startPosition) {
	super(cb, startPosition);
    }

    public void setBranchSourceAndJcc(Branch b, byte condition) { // backward branch
	jcc_unlinked(condition);
	assert(b.sourcePC == -1);
	int offset = b.targetPC - getPC();
	cb.putInt(cb.position() - 4, offset);
    }
    public Branch setBranchSourceAndJcc(byte condition) { // forward branch
	jcc_unlinked(condition);
	return new Branch(getPC(), -1);
    }
    public void setBranchSourceAndJmp(Branch b) { // backward branch
	jmp_unlinked();
	assert(b.sourcePC == -1);
	int offset = b.targetPC - getPC();
	cb.putInt(cb.position() - 4, offset);
    }
    public Branch setBranchSourceAndJmp() { // forward branch
	jmp_unlinked();
	return new Branch(getPC(), -1);
    }
    
    public void setBranchTarget(Branch b) { // forward branch
	assert(b.targetPC == -1);
	int offset = getPC() - b.sourcePC;
	cb.putInt(startPosition + b.sourcePC - 4, offset);
    }
    public Branch setBranchTarget() { // backward branch
	return new Branch(-1, getPC());
    }


    /**
     * Patches the given forward branch source at runtime.
     * Destroys EDI.
     *
     *     jmp L0    ... (*)       [=> L1]  
     * L0:
     *     __slow_path__
     *     self_modify (*)'s jump offset from L0 to L1
     * L1:
     *     __fast_path__
     * L2:
     *
     */
    public void setSelfModifyingBranchTarget(Branch b) {
	assert(b.targetPC == -1);
	call(0);
	int patchTarget = 
	    getPC() 
	    - b.sourcePC 
	    + 4 /* for jump offset slot in jmp_unlinked */;
	popR(R_EDI);
	int patchJumpOffset = 
	    getPC()
	    + l_movIM_wide()
	    - b.sourcePC;
	movIM_wide(patchJumpOffset, R_EDI, - patchTarget);
    }

    /**
     * Self-modify a "jmp rel32" into a "mov imm32, reg".
     * Destroys EDI and EBX.
     * @param b the Branch object whose sourcePC points to the address
     * right after the "jmp rel32".
     * @param reg the register which has the value imm32 for the mov
     * and the destination register of the "mov imm32, reg"
     *
     *
     *     jmp L0  ... (*)            [=> "mov imm32, reg"]     
     * L2:
     *     __fast_path__ (eg call *ecx)
     *     jmp L1
     * L0:
     *     __slow_path__
     *     self_modify: replace (*) with a "mov imm32, reg"
     *     (op. jmp L2 if the end of the slow path is the same as the fast path)
     * L1:
     */
    public void selfModifyJmpWithMovIR(Branch b, 
				       byte reg) {
	assert(b.targetPC == -1);
	call(0);
	int offsetToJmp =
	    getPC() 
	    - b.sourcePC 
	    + 5; // offset to the beginning of the "jmp rel32"
	int offsetToJmpOp =
	    getPC() 
	    - b.sourcePC 
	    + 4; // offset to the operand of the "jmp rel32"
	popR(R_EDI);
	movIR(0xB8 + reg, R_EBX); // EBX = the opcode of "mov imm32, reg"
	movR8M(R_EBX, R_EDI, - offsetToJmp);
	movRM(reg, R_EDI, - offsetToJmpOp);
    }


    /**
     * Self-modify a "jmp rel32" into a 5-byte nops.
     * Destroys EDI and EBX.
     * @param b the Branch object whose sourcePC points to the address
     * right after the "jmp rel32".
     */
    public void selfModifyJmpWithNops(Branch b) {
	assert(b.targetPC == -1);
	call(0);
	int offsetToJmp =
	    getPC() 
	    - b.sourcePC 
	    + 5; // offset to the beginning of the "jmp rel32"
	int offsetToJmpOp =
	    getPC() 
	    - b.sourcePC 
	    + 4; // offset to the operand of the "jmp rel32"
	popR(R_EDI);
	movIR(0x90909090, R_EBX);
	movR8M(R_EBX, R_EDI, - offsetToJmp);
	movRM(R_EBX, R_EDI, - offsetToJmpOp);
    }

    /**
     * Self-modify a 4-byte memory location (eg a jump offset,
     * immediate value in a move) with a given value.
     * Destorys R_EDI.
     * @param pc the pc *right after* the location
     * @param reg the register that has the value to be written to the location
     */
    public void selfModify4Bytes(int pc, byte reg) {
	call(0);
	int offset = getPC() - pc + 4;
	popR(R_EDI);
	movRM(reg, R_EDI, - offset);
    }

    /**
     * Self-modify a 4-byte memory location (eg a jump offset,
     * immediate value in a move) with a given value.
     * Destorys R_EDI.
     * @param pc the pc *right after* the location
     * @param value the value to be written to the location
     */
    public void selfModify4Bytes(int pc, int value) {
	call(0);
	int offset = getPC() - pc + 4;
	popR(R_EDI);
	movIM(value, R_EDI, - offset);
    }
    
    // mod = {MOD_R, MOD_M_32, MOD_M_8, MOD_M}
    private byte modRM(int mod, int regOp, int rm) {
	return (byte)(mod | (regOp << 3) | rm);
    }

    // scale = {SS_I_0, SS_I_2, SS_I_4, SS_I_8}
    private byte SIB(int scale, int index, int base) {
	return (byte)(scale | (index << 3) | base);
    }

    public int breakpoint() {
	cb.put((byte)0xcc);
	return 1;
    }

    public int write(byte b) {
	cb.put(b);
	return 1;
    }

    public int write32(int w) {
	cb.putInt(w);
	return 4;
    }

    public int write(byte[] b) {
	for(int i = 0; i < b.length; i++)
	    cb.put(b[i]);
	return b.length;
    }

    public int cdq() {
	cb.put((byte)0x99);
	return 1;
    }

    public int cmpxchgRM(boolean LOCKprefix, 
			 byte src, 
			 byte dst,
			 int offset) {
	int locklen = 0;
	if (LOCKprefix) {
	    locklen = 1;
	    cb.put((byte)0xF0);
	}
	cb.put((byte)0x0F);
	cb.put((byte)0xB1);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, src, dst));
	    return locklen + 3;
	} else if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, src, dst));
	    cb.put((byte)offset);
	    return locklen + 4;
	} else {
	    cb.put(modRM(MOD_M_32, src, dst));
	    cb.putInt(offset);
	    return locklen + 7;
	}
    }

    public int sete(byte dst) {
	cb.put((byte)0x0f);
	cb.put((byte)0x94);
	cb.put(modRM(MOD_R, 0, dst));
	return 3;
    }
	
    public int addRR(byte src, byte dst) {
	cb.put((byte)0x03);
	cb.put(modRM(MOD_R, dst, src));
	return 2;
    }

    public int l_addRR() {
	return 2;
    }

    public int adcRR(byte src, byte dst) {
	cb.put((byte)0x13);
	cb.put(modRM(MOD_R, dst, src));
	return 2;
    }

    public int addRM(byte src, byte dst, int offset) {
	cb.put((byte)0x01);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(offset);
	return 6;
    }

    public int adcRM(byte src, byte dst, int offset) {
	cb.put((byte)0x11);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(offset);
	return 6;
    }

    public int addIR(int immediate, byte dst) {
	cb.put((byte)0x81);
	cb.put(modRM(MOD_R, 0, dst));
	cb.putInt(immediate);
	return 6;
    }

    public int l_addIR() {
	return 6;
    }

    // add offset(srcreg, indexreg, 4) to destreg
    public int addMRI4(byte src_reg, 
		       byte index_reg, 
		       int offset, 
		       byte dest_reg) {
	cb.put((byte)0x03);
	cb.put(modRM(MOD_M_32, dest_reg, 4));
	cb.put(SIB(SS_I_4, index_reg, src_reg));
	cb.putInt(offset);
	return 7;
    }

    public int l_addMRI4() {
	return 7;
    }

    public int leaMRI4(byte src_reg,
		       byte index_reg,
		       int offset,
		       byte dest_reg) {
	cb.put((byte)0x8D);
	cb.put(modRM(MOD_M_32, dest_reg, 4));
	cb.put(SIB(SS_I_4, index_reg, src_reg));
	cb.putInt(offset);
	return 7;
    }

    public int l_leaMRI4() {
	return 7;
    }

    public int addIM(int immediate, byte dst, int offset) {
	cb.put((byte)0x81);
	if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, 0, dst));
	    cb.put((byte)offset);
	    cb.putInt(immediate);
	    return 3;
	} else {
	    cb.put(modRM(MOD_M_32, 0, dst));
	    cb.putInt(offset);
	    cb.putInt(immediate);
	    return 10;
	}
    }

    public int subIR(int immediate, byte dst) {
	cb.put((byte)0x81);
	cb.put((byte)(0xE8 + dst));
	cb.putInt(immediate);
	return 6;
    }	

    // sub mem from reg
    public int subMR(byte src, int soffset, byte dst) {
	cb.put((byte)0x2B);
	cb.put(modRM(MOD_M_32, dst, src));
	cb.putInt(soffset);
	return 6;
    }

    public int sbbMR(byte src, int soffset, byte dst) {
	cb.put((byte)0x1B);
	cb.put(modRM(MOD_M_32, dst, src));
	cb.putInt(soffset);
	return 6;
    }

    public int subRM(byte src, byte dst, int doffset) {
	cb.put((byte)0x29);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(doffset);
	return 6;
    }

    public int subRR(byte src, byte dst) {
	cb.put((byte)0x29);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int sbbRM(byte src, byte dst, int doffset) {
	cb.put((byte)0x19);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(doffset);
	return 6;
    }

    public int sbbRR(byte src, byte dst) {
	cb.put((byte)0x19);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int mulRR(byte src, byte dst) {
	cb.put((byte)0x0F);
	cb.put((byte)0xAF);
	cb.put(modRM(MOD_R, dst, src));
	return 3;
    }

    public int l_mulRR() {
	return 3;
    }

    // unsigned mul R_EAX * mem - > R_EDX:R_EAX
    public int umulM(byte src, int offset) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_M_32, 4, src));
	cb.putInt(offset);
	return 6;
    }

    // unsigned mul R_EAX * reg -> R_EDX:R_EAX
    public int umulR(byte src) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_R, 4, src));
	return 2;
    }
    
    // div R_EDX:R_EAX / mem -> R_EAX
    //     R_EDX:R_EAX % mem -> R_EDX
    public int divM(byte src, int offset) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_M_32, 7, src));
	cb.putInt(offset);
	return 6;
    }

    // div R_EDX:R_EAX / reg -> R_EAX
    //     R_EDX:R_EAX % reg -> R_EDX
    public int divR(byte src) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_R, 7, src));
	return 2;
    }

    public int pushR(byte src) {
	cb.put((byte)(0x50 | src));
	return 1;
    }

    public int l_pushR() {
	return 1;
    }

    public int popR(byte dst) {
	cb.put((byte)(0x58 | dst));
	return 1;
    }

    public int l_popR() {
	return 1;
    }

    public int pushM(byte src, int offset) {
	cb.put((byte)0xFF);
	if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, 6, src));
	    cb.put((byte)offset);
	    return 3;
	} else {
	    cb.put(modRM(MOD_M_32, 6, src));
	    cb.putInt(offset);
	    return 6;
	}
    }

    public int pushM_wide(byte src, int offset) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_M_32, 6, src));
	cb.putInt(offset);
	return 6;
    }

    /**
     * @return the pc right after this instruction
     */
    public int pushM_to_be_patched(byte src) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_M_32, 6, src));
	cb.putInt(-1);
	return getPC();
    }

    public int l_pushM(int offset) {
	if (-128 <= offset && offset <= 127) {
	    return 3;
	} else {
	    return 6;
	}
    }
	
    // push [base_reg + 4 * index_reg]
    public int pushMS4(byte base_reg,
		       byte index_reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_M, 6, 4));
	cb.put(SIB(SS_I_4, index_reg, base_reg));
	return 3;
    }

    // push [base_reg + 8 * index_reg]
    public int pushMS8(byte base_reg,
		       byte index_reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_M, 6, 4));
	cb.put(SIB(SS_I_8, index_reg, base_reg));
	return 3;
    }

    public int l_pushMS8() {
	return 3;
    }

    public int popM(byte dst, int offset) {
	cb.put((byte)0x8F);
	if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, 0, dst));
	    cb.put((byte)offset);
	    return 3;
	} else {
	    cb.put(modRM(MOD_M_32, 0, dst));
	    cb.putInt(offset);
	    return 6;
	}
    }

    public int pushI32(int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    cb.put((byte)0x6A);
	    cb.put((byte)immediate);
	    return 2;
	} else {
	    cb.put((byte)0x68);
	    cb.putInt(immediate);
	    return 5;
	}
    }

    /**
     *  A version of pushI32 that always uses a 32-bit wide immediate
     *  value.
     **/
    public int pushI32_wide(int immediate) {
	cb.put((byte)0x68);
	cb.putInt(immediate);
	return 5;
    }	

    public int l_pushI32_wide() {
	return 5;
    }

    /**
     * @return the pc right after this instruction
     */
    public int pushI32_to_be_patched() {
	cb.put((byte)0x68);
	cb.putInt(-1);
	return getPC();
    }	

    /**
     * @return the offset of the immediate value from the beginning of
     * the instruction pushI32
     **/
    public int imm_offset_pushI32_wide() {
	return 1;
    }

    public int l_pushI32(int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    return 2;
	} else {
	    return 5;
	}
    }	

    public int pushI64(long immediate) {
	cb.put((byte)0x68);
	cb.putInt((int)((immediate >> 32) & 0xFFFFFFFF));
	cb.put((byte)0x68);
	cb.putInt((int)(immediate & 0xFFFFFFFF));
	return 10;
    }

    public int l_pushI64() {
	return 10;
    }

    public int leaMR(byte src, int offset, byte dst) {
	assert(src != R_ESP);
	cb.put((byte)0x8D);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, dst, src));
	    return 2;
	} else if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, dst, src));
	    cb.put((byte)offset);
	    return 3;
	} else {
	    cb.put(modRM(MOD_M_32, dst, src));
	    cb.putInt(offset);
	    return 6;
	}
    }

    public int movMR(byte src, int offset, byte dst) {
	assert(src != R_ESP);
	cb.put((byte)0x8B);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, dst, src));
	    return 2;
	} else if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, dst, src));
	    cb.put((byte)offset);
	    return 3;
	} else {
	    cb.put(modRM(MOD_M_32, dst, src));
	    cb.putInt(offset);
	    return 6;
	}
    }

    public int movMR_wide(byte src, int offset, byte dst) {
	assert(src != R_ESP);
	cb.put((byte)0x8B);
	cb.put(modRM(MOD_M_32, dst, src));
	cb.putInt(offset);
	return 6;
    }

    /**
     * @return the pc right after this instruction
     */
    public int movMR_to_be_patched(byte src, byte dst) {
	assert(src != R_ESP);
	cb.put((byte)0x8B);
	cb.put(modRM(MOD_M_32, dst, src));
	cb.putInt(-1);
	return getPC();
    }

    public int l_movMR(int offset) {
	if (offset == 0) {
	    return 2;
	} else if (-128 <= offset && offset <= 127) {
	    return 3;
	} else {
	    return 6;
	}
    }

    // mov offset(srcreg, indexreg, 4) -> destreg
    public int movMRI4(byte src_reg, 
		       byte index_reg, 
		       int offset, 
		       byte dest_reg) {
	cb.put((byte)0x8B);
	cb.put(modRM(MOD_M_32, dest_reg, 4));
	cb.put(SIB(SS_I_4, index_reg, src_reg));
	cb.putInt(offset);
	return 7;
    }

    public int l_movMRI4() {
	return 7;
    }

    public int movRM(byte src, byte dst, int offset) {
	cb.put((byte)0x89);
	if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, src, dst));
	    cb.put((byte)offset);
	    return 3;
	} else {
	    cb.put(modRM(MOD_M_32, src, dst));
	    cb.putInt(offset);
	    return 6;
	}
    }

    public int movRM_wide(byte src, byte dst, int offset) {
	cb.put((byte)0x89);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(offset);
	return 6;
    }

    /**
     * @return the pc right after this instruction
     */
    public int movRM_to_be_patched(byte src, byte dst) {
	cb.put((byte)0x89);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(-1);
	return getPC();
    }

    public int l_movRM_wide() {
	return 6;
    }

    public int movRM(byte src, byte dst) {
	assert(dst != R_ESP);
	cb.put((byte)0x89);
	cb.put(modRM(MOD_M, src, dst));
	return 2;
    }

    /*
    public int movIR(VM_Address value, byte dst) {
	cb.put((byte)(0xB8 + dst));
	cb.putInt(value.asInt());
	return 5;
   }
    */

    public int movIR(int value, byte dst) {
	/*
	if (value == 0)
	    return xorRR(dst, dst);
	else {
	*/
	    cb.put((byte)(0xB8 + dst));
	    cb.putInt(value);
	    return 5;
    //}
    }

    public int l_movIR() {
	return 5;
    }

    /**
     * @return the pc right after this instruction
     */
    public int movIR_to_be_patched(byte dst) {
	cb.put((byte)(0xB8 + dst));
	cb.putInt(-1);
	return getPC();
    }	

    public int movIM(int value, byte dst, int offset) {
	cb.put((byte)0xC7);
	if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, 0, dst));
	    cb.put((byte)offset);
	    cb.putInt(value);
	    return 7;
	} else {
	    cb.put(modRM(MOD_M_32, 0, dst));
	    cb.putInt(offset);
	    cb.putInt(value);
	    return 10;
	}
    }

    public int movIM_wide(int value, byte dst, int offset) {
	cb.put((byte)0xC7);
	cb.put(modRM(MOD_M_32, 0, dst));
	cb.putInt(offset);
	cb.putInt(value);
	return 10;
    }

    public int l_movIM_wide() {
	return 10;
    }

    // Return in which offset, from the start of the instruction, the
    // immediate value is written in the instruction movIM
    public int movIM_Ioffset(int offset) {
	if (-128 <= offset && offset <= 127) {
	    return 3;
	} else {
	    return 6;
	}
    }

    public int movI8M8(byte imm, byte dst_reg, byte offset) {
	assert(dst_reg != R_ESP);
	cb.put((byte)0xC6);
	cb.put(modRM(MOD_M_8, 0, dst_reg));
	cb.put(offset);
	cb.put(imm);
	return 4;
    }

    /*
    public int movIM(int value, int address) {
	cb.put((byte)0xC7);
	cb.put((byte)0x24);
	cb.put((byte)0x25);
	cb.putInt(address);
	cb.putInt(value);
	return 11;
    }
    */

    // Move a sign-extended byte in memory to a register
    public int movsxM8R(byte src, int offset, byte dst) {
	assert(src != R_ESP);
	cb.put((byte)0x0F);
	cb.put((byte)0xBE);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, dst, src));
	    return 3;
	} else if (-128 <= offset && offset < 127) {
	    cb.put(modRM(MOD_M_8, dst, src));
	    cb.put((byte)offset);
	    return 4;
	} else {
	    cb.put(modRM(MOD_M_32, dst, src));
	    cb.putInt(offset);
	    return 7;
	}
    }

    // Move a sign-extended short in memory to a register
    public int movsxM16R(byte src, int offset, byte dst) {
	assert(src != R_ESP);
	cb.put((byte)0x0F);
	cb.put((byte)0xBF);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, dst, src));
	    return 3;
	} else if (-128 <= offset && offset < 127) {
	    cb.put(modRM(MOD_M_8, dst, src));
	    cb.put((byte)offset);
	    return 4;
	} else {
	    cb.put(modRM(MOD_M_32, dst, src));
	    cb.putInt(offset);
	    return 7;
	}
    }

    // Move a zero-extended short in memory to a register
    public int movzxM16R(byte src, int offset, byte dst) {
	assert(src != R_ESP);
	cb.put((byte)0x0F);
	cb.put((byte)0xB7);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, dst, src));
	    return 3;
	} else if (-128 <= offset && offset < 127) {
	    cb.put(modRM(MOD_M_8, dst, src));
	    cb.put((byte)offset);
	    return 4;
	} else {
	    cb.put(modRM(MOD_M_32, dst, src));
	    cb.putInt(offset);
	    return 7;
	}
    }

    public int movRR(byte src, byte dst) {
	cb.put((byte)0x89);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    /**
     * Return the size of the macro movRR
     **/
    public int l_movRR() {
	return 2;
    }

    public int movR8M(byte src, byte dst, int offset) {
	assert(dst != R_ESP);
	assert(src == R_EAX
	       || src == R_ECX
	       || src == R_EDX
	       || src == R_EBX);
	cb.put((byte)0x88);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, src, dst));
	    return 2;
	} else if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, src, dst));
	    cb.put((byte)offset);
	    return 3;
	} else {
	    cb.put(modRM(MOD_M_32, src, dst));
	    cb.putInt(offset);
	    return 6;
	}
    }

    public int movR16M(byte src, byte dst, int offset) {
	assert(dst != R_ESP);
	assert(src == R_EAX
	       || src == R_ECX
	       || src == R_EDX
	       || src == R_EBX);
	cb.put((byte)0x66);
	cb.put((byte)0x89);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, src, dst));
	    return 3;
	} else if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, src, dst));
	    cb.put((byte)offset);
	    return 4;
	} else {
	    cb.put(modRM(MOD_M_32, src, dst));
	    cb.putInt(offset);
	    return 7;
	}
    }

    // reg = (reg << width)
    public int shlRI(byte reg, byte width) {
	cb.put((byte)0xC1);
	cb.put(modRM(MOD_R, 4, reg));
	cb.put(width);
	return 3;
    }

    // reg = (reg << CL)
    public int shlR(byte reg) {
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_R, 4, reg));
	return 2;
    }

    // tmem = (tmem << wmem)
    public int shlMM(byte target_reg, 
		     int toffset,
		     byte width_reg, 
		     int woffset) {
	int l1 = pushR(R_ECX);
	int l2 = movMR(width_reg, woffset, R_ECX);
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_M_32, 4, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_ECX);
	return l1 + l2 + 6 + l3;
    }

    // tmem = (tmem << wmem) w/ shift-in from sireg
    // Note sireg != ECX
    public int shldMM(byte target_reg, 
		      int toffset,
		      byte width_reg, 
		      int woffset,
		      byte sireg) {
	assert(sireg != R_ECX);
	int l1 = pushR(R_ECX);
	int l2 = movMR(width_reg, woffset, R_ECX);
	cb.put((byte)0x0F);
	cb.put((byte)0xA5);
	cb.put(modRM(MOD_M_32, sireg, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_ECX);
	return l1 + l2 + 7 + l3;
    }

    // treg = (treg << ECX) w/ shift-in from sireg
    // Note sireg != ECX
    public int shldR(byte target_reg, 
		     byte sireg) {
	assert(sireg != R_ECX);
	cb.put((byte)0x0F);
	cb.put((byte)0xA5);
	cb.put(modRM(MOD_R, sireg, target_reg));
	return 3;
    }

    // tmem = (tmem >>> wmem)
    public int shrMM(byte target_reg, 
		     int toffset,
		     byte width_reg,
		     int woffset) {
	int l1 = pushR(R_ECX);
	int l2 = movMR(width_reg, woffset, R_ECX);
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_M_32, 5, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_ECX);
	return l1 + l2 + 6 + l3;
    }

    // reg = (reg >>> CL)
    public int shrR(byte reg) {
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_R, 5, reg));
	return 2;
    }

    // treg = (treg >> ECX) w/ shift-in from sireg
    // Note sireg != ECX
    public int shrdR(byte target_reg, 
		     byte sireg) {
	assert(sireg != R_ECX);
	cb.put((byte)0x0F);
	cb.put((byte)0xAD);
	cb.put(modRM(MOD_R, sireg, target_reg));
	return 3;
    }

    // tmem = (tmem >> wmem) w/ shift-in from sireg
    // Note sireg != ECX
    public int shrdMM(byte target_reg, 
		      int toffset,
		      byte width_reg, 
		      int woffset,
		      byte sireg) {
	assert(sireg != R_ECX);
	int l1 = pushR(R_ECX);
	int l2 = movMR(width_reg, woffset, R_ECX);
	cb.put((byte)0x0F);
	cb.put((byte)0xAD);
	cb.put(modRM(MOD_M_32, sireg, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_ECX);
	return l1 + l2 + 7 + l3;
    }

    // tmem = (tmem >> wmem)
    public int sarMM(byte target_reg, 
		     int toffset,
		     byte width_reg,
		     int woffset) {
	int l1 = pushR(R_ECX);
	int l2 = movMR(width_reg, woffset, R_ECX);
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_M_32, 7, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_ECX);
	return l1 + l2 + 6 + l3;
    }

    // reg = (reg >> CL)
    public int sarR(byte reg) {
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_R, 7, reg));
	return 2;
    }

    // reg = (reg >> width)
    public int sarRI(byte reg, 
		     byte width) {
	cb.put((byte)0xC1);
	cb.put(modRM(MOD_R, 7, reg));
	cb.put(width);
	return 3;
    }

    public int l_sarRI() {
	return 3;
    }

    // mem = (mem >> width)
    public int sarMI(byte target_reg, 
		     int toffset,
		     byte width) {
	cb.put((byte)0xC1);
	cb.put(modRM(MOD_M_32, 7, target_reg));
	cb.putInt(toffset);
	cb.put(width);
	return 7;
    }

    // test imm & mem
    public int testIM(int immediate,
		      byte target_reg,
		      int toffset) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_M_32, 0, target_reg));
	cb.putInt(toffset);
	cb.putInt(immediate);
	return 10;
    }

    // test imm & reg
    public int testIR(int immediate,
		      byte reg) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_R, 0, reg));
	cb.putInt(immediate);
	return 6;
    }

    public int andIR(int immediate,
		     byte reg) {
	cb.put((byte)0x81);
	cb.put(modRM(MOD_R, 4, reg));
	cb.putInt(immediate);
	return 6;
    }

    public int andRR(byte src,
		     byte dst) {
	cb.put((byte)0x21);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int andRM(byte src,
		     byte dst,
		     int doffset) {
	cb.put((byte)0x21);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(doffset);
	return 6;
    }

    public int andMR(byte src,
		     int soffset,
		     byte dst) {
	cb.put((byte)0x23);
	cb.put(modRM(MOD_M_32, dst, src));
	cb.putInt(soffset);
	return 6;
    }

    public int orRM(byte src,
		    byte dst,
		    int doffset) {
	cb.put((byte)0x09);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(doffset);
	return 6;
    }

    public int orRR(byte src,
		    byte dst) {
	cb.put((byte)0x09);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int orMR(byte src,
		    int soffset,
		    byte dst) {
	cb.put((byte)0x0B);
	cb.put(modRM(MOD_M_32, dst, src));
	cb.putInt(soffset);
	return 6;
    }

    public int xorRM(byte src,
		     byte dst,
		     int doffset) {
	cb.put((byte)0x31);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(doffset);
	return 6;
    }

    public int xorRR(byte src,
		     byte dst) {
	cb.put((byte)0x31);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int xorMR(byte src,
		     int soffset,
		     byte dst) {
	cb.put((byte)0x33);
	cb.put(modRM(MOD_M_32, dst, src));
	cb.putInt(soffset);
	return 6;
    }

    // converts an integer on memory into a double and pushes it onto FPU stack
    public int fildM32(byte reg, int offset) {
	cb.put((byte)0xDB);
	cb.put(modRM(MOD_M_32, 0, reg));
	cb.putInt(offset);
	return 6;
    }

    // converts an integer on memory into a double and pushes it onto FPU stack
    public int fildM32(byte reg) {
	assert(reg != R_ESP);
	cb.put((byte)0xDB);
	cb.put(modRM(MOD_M, 0, reg));
	return 2;
    }

    // converts a long on memory into a double and pushes it onto FPU stack
    public int fildM64(byte reg, int offset) {
	cb.put((byte)0xDF);
	cb.put(modRM(MOD_M_32, 5, reg));
	cb.putInt(offset);
	return 6;
    }

    // converts a long on memory into a double and pushes it onto FPU stack
    public int fildM64(byte reg) {
	assert(reg != R_ESP);
	cb.put((byte)0xDF);
	cb.put(modRM(MOD_M, 5, reg));
	return 2;
    }

    // converts a double on FPU stack into an integer and stores it on memory
    public int fistM32(byte reg, int offset) {
	cb.put((byte)0xDB);
	cb.put(modRM(MOD_M_32, 3, reg));
	cb.putInt(offset);
	return 6;
    }

    // converts a double on FPU stack into an integer and stores it on memory
    public int fistM32(byte reg) {
	assert(reg != R_ESP);
	cb.put((byte)0xDB);
	cb.put(modRM(MOD_M, 3, reg));
	return 2;
    }

    // converts a double on FPU stack into a long and stores it on memory
    public int fistM64(byte reg, int offset) {
	cb.put((byte)0xDF);
	cb.put(modRM(MOD_M_32, 7, reg));
	cb.putInt(offset);
	return 6;
    }

    // converts a double on FPU stack into a long and stores it on memory
    public int fistM64(byte reg) {
	assert(reg != R_ESP);
	cb.put((byte)0xDF);
	cb.put(modRM(MOD_M, 7, reg));
	return 2;
    }

    public int cmpRR(byte reg1, byte reg2) {
	cb.put((byte)0x39);
	cb.put(modRM(MOD_R, reg2, reg1));
	return 2;
    }

    public int l_cmpRR() {
	return 2;
    }

    public int cmpMR(byte reg1, int offset, byte reg2) {
	cb.put((byte)0x39);
	cb.put(modRM(MOD_M_32, reg2, reg1));
	cb.putInt(offset);
	return 6;
    }

    public int cmpMI(byte reg, int offset, int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    cb.put((byte)0x83);
	    cb.put(modRM(MOD_M_32, 7, reg));
	    cb.putInt(offset);
	    cb.put((byte)immediate);
	    return 7;
	} else {
	    cb.put((byte)0x81);
	    cb.put(modRM(MOD_M_32, 7, reg));
	    cb.putInt(offset);
	    cb.putInt(immediate);
	    return 10;
	}
    }

    public int cmpRI(byte reg, int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    cb.put((byte)0x83);
	    cb.put(modRM(MOD_R, 7, reg));
	    cb.put((byte)immediate);
	    return 3;
	} else {
	    cb.put((byte)0x81);
	    cb.put(modRM(MOD_R, 7, reg));
	    cb.putInt(immediate);
	    return 6;
	}
    }

    public int l_cmpIR(int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    return 3;
	} else {
	    return 6;
	}
    }

    /**
     * Emit a relative unconditional jump
     * @param offset the jump offset excluding the size of the jmp
     * instruction. If positive, forward jump, if negative backward
     * jump.
     **/
    public int jmp(int offset) {
	if (offset < 0) { // backward jump
	    int offset8  = offset - 2;
	    if (-128 <= offset8 && offset8 <= 127) {
		cb.put((byte)0xEB);
		cb.put((byte)offset8);
		return 2;
	    } else {
		int offset32 = offset - 5;
		cb.put((byte)0xE9);
		cb.putInt(offset32);
		return 5;
	    }
	} else { // forward jump
	    if (-128 <= offset && offset <= 127) {
		cb.put((byte)0xEB);
		cb.put((byte)offset);
		return 2;
	    } else {
		cb.put((byte)0xE9);
		cb.putInt(offset);
		return 5;
	    }
	}
    }

    /**
     * Return the size of an unconditional jump
     * @param offset the jump offset excluding the size of the jmp
     * instruction. If positive, forward jump, if negative backward
     * jump.
     **/
    public int l_jmp(int offset) {
	if (offset < 0) {
	    int offset8 = offset - 2;
	    if (-128 <= offset8 && offset8 <= 127)
		return 2;
	    else
		return 5;
	} else {
	    if (-128 <= offset && offset <= 127)
		return 2;
	    else 
		return 5;
	}
    }

    public int jmp_long(int offset) {
	if (offset < 0) { // backward jump
	    int offset32 = offset - 6;
	    cb.put((byte)0xE9);
	    cb.putInt(offset32);
	    return 5;
	} else { // forward jump
	    cb.put((byte)0xE9);
	    cb.putInt(offset);
	    return 5;
	}
    }	

    public int l_jmp_long() {
	return 5;
    }

    /**
     * Emit an unconditional jump whose jump offset is not yet determined.
     * Assume a near jump which has a 32 bit offset.
     **/
    public int jmp_unlinked() {
	cb.put((byte)0xE9);
	cb.putInt(0);
	return 5;
    }

    /**
     * @return the offset of the immediate value from the beginning of 
     * the jump_unlinked instruction
     **/
    public int imm_offset_jmp_unlinked() {
	return 1;
    }

    public int l_jmp_unlinked() {
	return 5;
    }

    public int jmpAbsM(byte reg, int offset) {
	if (-128 <= offset && offset <= 127) {
	    cb.put((byte)0xFF);
	    cb.put(modRM(MOD_M_8, 4, reg));
	    cb.put((byte)offset);
	    return 3;
	} else {
	    cb.put((byte)0xFF);
	    cb.put(modRM(MOD_M_32, 4, reg));
	    cb.putInt(offset);
	    return 6;
	}
    }

    public int jmpAbsR(byte reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_R, 4, reg));
	return 2;
    }

    public int l_jmpAbsR() {
	return 2;
    }

    // jmp reg (scale index base) jmp (base, reg*4)    
    public int jmpAbsMS(int base,
			byte index_reg) {
	cb.put((byte)0xFF);
	cb.put((byte)0x24);
	cb.put(modRM(MOD_M_32, index_reg, 5));
	cb.putInt(base);
	return 7;
    }

    public int jmpAbsMS_unlinked(byte index_reg) {
	cb.put((byte)0xFF);
	cb.put((byte)0x24);
	cb.put(modRM(MOD_M_32, index_reg, 5));
	cb.putInt(0);
	return 7;
    }

    public int l_jmpAbsMS_unlinked() {
	return 7;
    }

    public int imm_offset_jmpAbsMS_unlinked() {
	return 3;
    }

    public int call(int offset) {
	cb.put((byte)0xE8);
	cb.putInt(offset);
	return 5;
    }

    public int l_call() {
	return 5;
    }

    public int call_imm_offset() {
	return 1;
    }

    public int call_unlinked() {
	cb.put((byte)0xE8);
	cb.putInt(0);
	return 5;
    }

    public int call_unlinked_imm_offset() {
	return 1;
    }
	
    public int callAbsR(byte reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_R, 2, reg));
	return 2;
    }

    public int l_callAbsR() {
	return 2;
    }

    public void call(VM_Address addr) {
	// Compute delta between next PC on return and the function to
	// be called.  (The call instruction will always be 5 bytes
	// long.  We could generate a 4 byte instruction with a 16-bit
	// delta, but we probably shouldn't bother with something gas
	// doesn't do.)
	call(addr.diff(getAbsolutePC().add(5)).asInt());
    }

    /**
     * Emit a conditional jump
     * @param condition the condition of the jump
     * @param offset the jump offset excluding the size of the jcc
     * instruction. If positive, forward jump, if negative backward
     * jump.
     **/
    public int jcc(byte condition, int offset) {
	if (offset < 0) { // backward jump
	    int offset8  = offset - 2;
	    if (-128 <= offset8 && offset8 <= 127) {
		cb.put((byte)(0x70 + condition));
		cb.put((byte)offset8);
		return 2;
	    } else {
		int offset32 = offset - 6;
		cb.put((byte)0x0F);
		cb.put((byte)(0x80 + condition));
		cb.putInt(offset32);
		return 6;
	    }
	} else { // forward jump
	    if (-128 <= offset && offset <= 127) {
		cb.put((byte)(0x70 + condition));
		cb.put((byte)offset);
		return 2;
	    } else {
		cb.put((byte)0x0F);
		cb.put((byte)(0x80 + condition));
		cb.putInt(offset);
		return 6;
	    }
	}
    }

    /**
     * Return the size of a conditional jump
     * @param offset the jump offset excluding the size of the jcc
     * instruction. If positive, forward jump, if negative backward
     * jump.
     **/
    public int l_jcc(int offset) {
	if (offset < 0) {
	    int offset8 = offset - 2;
	    if (-128 <= offset8 && offset8 <= 127)
		return 2;
	    else
		return 6;
	} else {
	    if (-128 <= offset && offset <= 127)
		return 2;
	    else 
		return 6;
	}
    }

    /**
     * Emit a conditional jump whose jump offset is not yet determined.
     * Assume a near jump which has a 32 bit offset.
     **/
    public int jcc_unlinked(byte condition) {
	cb.put((byte)0x0F);
	cb.put((byte)(0x80 + condition));
	cb.putInt(0);
	return 6;
    }

    public int imm_offset_jcc_unlinked() {
	return 2;
    }

    // fld (32bits) mem -> (FPU reg stack top)
    public int fldM32(byte src, int offset) {
	cb.put((byte)0xD9);
	cb.put(modRM(MOD_M_32, 0, src));
	cb.putInt(offset);
	return 6;
    }

    // fld (32bits) mem -> (FPU reg stack top)
    public int fldM32(byte src) {
	assert(src != R_ESP);
	cb.put((byte)0xD9);
	cb.put(modRM(MOD_M, 0, src));
	return 2;
    }

    // fadd (32 or 64 bits) st(0) = st(0) + st(1)
    public int fadd() {
	cb.put((byte)0xD8);
	cb.put((byte)(0xC0 + 1));
	return 2;
    }

    // fadd (32bits) (FPU reg stack top) += mem
    public int faddM32(byte src,
		       int offset) {
	cb.put((byte)0xD8);
	cb.put(modRM(MOD_M_32, 0, src));
	cb.putInt(offset);
	return 6;
    }

    // fsub (32bits) (FPU reg stack top) -= mem
    public int fsubM32(byte src,
		       int offset) {
	cb.put((byte)0xD8);
	cb.put(modRM(MOD_M_32, 4, src));
	cb.putInt(offset);
	return 6;
    }

    // fsub (32bits) (FPU reg stack top) -= mem
    public int fsubM32(byte src) {
	assert(src != R_ESP);
	cb.put((byte)0xD8);
	cb.put(modRM(MOD_M, 4, src));
	return 2;
    }

    // fmul (32bits) (FPU reg stack top) *= mem
    public int fmulM32(byte src,
		       int offset) {
	cb.put((byte)0xD8);
	cb.put(modRM(MOD_M_32, 1, src));
	cb.putInt(offset);
	return 6;
    }

    // fdiv (32bits) (FPU reg stack top) /= mem
    public int fdivM32(byte src,
		       int offset) {
	cb.put((byte)0xD8);
	cb.put(modRM(MOD_M_32, 6, src));
	cb.putInt(offset);
	return 6;
    }

    // fdiv (32bits) (FPU reg stack top) /= mem
    public int fdivM32(byte src) {
	assert(src != R_ESP);
	cb.put((byte)0xD8);
	cb.put(modRM(MOD_M, 6, src));
	return 2;
    }

    // fstp (32bits) (FPU reg stack top) -> mem
    public int fstpM32(byte dst,
		       int offset) {
	cb.put((byte)0xD9);
	cb.put(modRM(MOD_M_32, 3, dst));
	cb.putInt(offset);
	return 6;
    }

    // fstp (32bits) (FPU reg stack top) -> mem
    public int fstpM32(byte dst) {
	assert(dst != R_ESP);
	cb.put((byte)0xD9);
	cb.put(modRM(MOD_M, 3, dst));
	return 2;
    }

    // fchs (changes the sign of (FPU reg stack top)
    public int fchs() {
	cb.put((byte)0xD9);
	cb.put((byte)0xE0);
	return 2;
    }

    // fld (64bits) mem -> (FPU reg stack top)
    public int fldM64(byte src,
		      int offset) {
	cb.put((byte)0xDD);
	cb.put(modRM(MOD_M_32, 0, src));
	cb.putInt(offset);
	return 6;
    }

    // fld (64bits) mem -> (FPU reg stack top)
    public int fldM64(byte src) {
	assert(src != R_ESP);
	cb.put((byte)0xDD);
	cb.put(modRM(MOD_M, 0, src));
	return 2;
    }

    // fadd (64bits) (FPU reg stack top) += mem
    public int faddM64(byte src,
		       int offset) {
	cb.put((byte)0xDC);
	cb.put(modRM(MOD_M_32, 0, src));
	cb.putInt(offset);
	return 6;
    }

    // fsub (64bits) (FPU reg stack top) -= mem
    public int fsubM64(byte src,
		       int offset) {
	cb.put((byte)0xDC);
	cb.put(modRM(MOD_M_32, 4, src));
	cb.putInt(offset);
	return 6;
    }

    // fsub (64bits) (FPU reg stack top) -= mem
    public int fsubM64(byte src) {
	assert(src != R_ESP);
	cb.put((byte)0xDC);
	cb.put(modRM(MOD_M, 4, src));
	return 2;
    }

    // fmul (64bits) (FPU reg stack top) *= mem
    public int fmulM64(byte src,
		       int offset) {
	cb.put((byte)0xDC);
	cb.put(modRM(MOD_M_32, 1, src));
	cb.putInt(offset);
	return 6;
    }

    // fdiv (64bits) (FPU reg stack top) /= mem
    public int fdivM64(byte src,
		       int offset) {
	cb.put((byte)0xDC);
	cb.put(modRM(MOD_M_32, 6, src));
	cb.putInt(offset);
	return 6;
    }

    // fdiv (64bits) (FPU reg stack top) /= mem
    public int fdivM64(byte src) {
	assert(src != R_ESP);
	cb.put((byte)0xDC);
	cb.put(modRM(MOD_M, 6, src));
	return 2;
    }

    // fstp (64bits) (FPU reg stack top) -> mem
    public int fstpM64(byte src,
		       int offset) {
	cb.put((byte)0xDD);
	cb.put(modRM(MOD_M_32, 3, src));
	cb.putInt(offset);
	return 6;
    }

    // fstp (64bits) (FPU reg stack top) -> mem
    public int fstpM64(byte src) {
	assert(src != R_ESP);
	cb.put((byte)0xDD);
	cb.put(modRM(MOD_M, 3, src));
	return 2;
    }

    // fucompp (fcmp + 2 pops)
    public int fucompp() {
	cb.put((byte)0xDA);
	cb.put((byte)0xE9);
	return 2;
    }

    // fucompp + fstsw + sahf
    public int fcmp() {
	cb.put((byte)0xDA);
	cb.put((byte)0xE9);
	cb.put((byte)0x9B);
	cb.put((byte)0xDF);
	cb.put((byte)0xE0);
	cb.put((byte)0x9E);
	return 6;
    }

    public int fldcw(byte src, int offset) {
	assert(src != R_ESP);
	cb.put((byte)0xD9);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, 5, src));
	    return 2;
	} else if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, 5, src));
	    cb.put((byte)offset);
	    return 3;
	} else {
	    cb.put(modRM(MOD_M_32, 5, src));
	    cb.putInt(offset);
	    return 6;
	}
    }

    public int fnstcw(byte dst, int offset) {
	assert(dst != R_ESP);
	cb.put((byte)0xD9);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, 7, dst));
	    return 2;
	} else if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, 7, dst));
	    cb.put((byte)offset);
	    return 3;
	} else {
	    cb.put(modRM(MOD_M_32, 7, dst));
	    cb.putInt(offset);
	    return 6;
	}
    }

    public int nop() {
	cb.put((byte)0x90);
	return 1;
    }

    public int ret() {
	cb.put((byte)0xC3);
	return 1;
    }

    public int retleave() {
	cb.put((byte)0xC9);
	cb.put((byte)0xC3);
	return 2;
    }

    public int incR(byte reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_R, 0, reg));
	return 2;
    }

    public int l_incR() {
	return 2;
    }

    public int decR(byte reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_R, 1, reg));
	return 2;
    }

    public int l_decR() {
	return 2;
    }

    /**
     * Emits a movMR depending upon the size of the value
     *
     * @param srcReg the base register of the source
     * @param displacement the 32-bit displacement of the source
     * @param destReg1 the destination register 1 (lower word)
     * @param destReg2 the destination register 2 (higher word) untouched unless valueSize == 8
     * @param valueSize the size of the value (1, 2, 4, or 8 bytes)
     * @param signed if true, when valueSize == 2, the value is sign-extended; otherwise zero-extended
     **/
  /* unused
    private void movMRS(byte srcReg, 
			int displacement, 
			byte destReg1, 
			byte destReg2,
			int valueSize,
			boolean signed) {
        switch(valueSize) {
        case 1:
            movsxM8R(srcReg, displacement, destReg1);
            break;
        case 2:
	    if (signed)
		movsxM16R(srcReg, displacement, destReg1);
	    else
		movzxM16R(srcReg, displacement, destReg1);
            break;
        case 4:
	    if (displacement == 0) {
		movMR(srcReg, 0, destReg1);
	    } else {
		movMR(srcReg, displacement, destReg1);
	    }
            break;
	case 8:
	    if (displacement == 0) {
		movMR(srcReg, 0, destReg1);
	    } else {
		movMR(srcReg, displacement, destReg1);
	    }
	    movMR(srcReg, displacement + 4, destReg2);
	    break;
        default:
            throw new Error("Invalid array element size");
	}
    }

    */
}

