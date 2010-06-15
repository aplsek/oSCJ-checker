package s3.services.simplejit.powerpc;

import ovm.util.FixedByteBuffer;
import s3.services.simplejit.Assembler;
import ovm.core.services.memory.VM_Address;

/**
 * PowerPC Assembler
 * 
 * @author Filip Pizlo
 * @author Hiroshi Yamauchi
 */
public class PPCAssembler extends Assembler {

    private static final boolean is64bit = false;

    private final int operandStackPointerRegister;

    public PPCAssembler(FixedByteBuffer cb, int startPosition,
			int operandStackPointerRegister) {
        super(cb, startPosition);
        this.operandStackPointerRegister = operandStackPointerRegister;
    }

    private static int sign_mask(int value, int num_bits) {
        checkRange(value, num_bits);
        return value & ((1 << num_bits) - 1);
    }

    private static class OutOfRange extends RuntimeException {
        OutOfRange() {
            super();
        }
    }
    private static int checkRange(int value, int num_bits) {
        int shifted = value >> (num_bits - 1);
        if (shifted == 0 || shifted == -1) {
            return value;
        } else {
            throw new OutOfRange();
        }
        /*
        final int min = - (1 << (num_bits - 1));
        final int max = (1 << (num_bits - 1)) - 1;
        if (min <= value && value <= max) {
            return value;
        } else {
            throw new OutOfRange();
        }
        */
    }

    private static int abs(int a) {
        return a < 0 ? -a : a;
    }

    private static boolean addr_too_far(int addr, int num_bits) {
        return (abs(addr) & ~((1 << num_bits) - 1)) != 0 ? true : false;
    }

    void writeWord(int word) {
        cb.putInt(word);
    }
    
    void write(int[] instructions) {
        for(int i = 0; i < instructions.length; i++) {
            cb.putInt(instructions[i]);
        }
    }

    void emit_dcbf(int ra, int rb) {
	cb.putInt((31 << 26) | (0 << 21) | (ra << 16)
		  | (rb << 11) | (86 << 1));
    }

    void emit_icbl(int ra, int rb) {
	cb.putInt((31 << 26) | (0 << 21) | (ra << 16)
		  | (rb << 11) | (982 << 1));
    }

    void emit_isync() {
	cb.putInt((19 << 26) | (150 << 1));
    }

    void emit_sync() {
	cb.putInt((31 << 26) | (598 << 1));
    }

    void emit_li32(int rt, int value) {
        if (rt == 0 && value == 4) {
            throw new Error();
        }
        if (-32768 <= value && value <= 32767) {
            emit_li(rt, value);
        } else {
            emit_lis(rt, value >> 16);
            emit_ori(rt, rt, value);
        }
    }

    void emit_li(int rt, int value) {
        cb.putInt((14 << 26) | (rt << 21) | (0 << 16)
                | (sign_mask(value, 16) << 0));
    }

    void emit_lis(int rt, int value) {
        cb.putInt((15 << 26) | (rt << 21) | (0 << 16)
                | (sign_mask(value, 16) << 0));
    }

    void emit_mr(int rt, int rs) {
        cb.putInt((31 << 26) | (rs << 21) | (rt << 16) | (rs << 11)
                | (444 << 1) | (0 << 0));
    }

    void emit_nop() {
        emit_ori(0, 0, 0);
    }
    void emit_push(int r) {
        emit_addi(operandStackPointerRegister, operandStackPointerRegister, -4);
        emit_stw(r, operandStackPointerRegister, 0);
    }

    void emit_pop(int r) {
        emit_lwz(r, operandStackPointerRegister, 0);
        emit_addi(operandStackPointerRegister, operandStackPointerRegister, 4);
    }

    void emit_pushfs(int fp) {
        emit_addi(operandStackPointerRegister, operandStackPointerRegister, -4);
        emit_stfs(fp, operandStackPointerRegister, 0);
    }

    void emit_popfs(int fp) {
        emit_lfs(fp, operandStackPointerRegister, 0);
        emit_addi(operandStackPointerRegister, operandStackPointerRegister, 4);
    }

    void emit_pushfd(int fp) {
        emit_addi(operandStackPointerRegister, operandStackPointerRegister, -8);
        emit_stfd(fp, operandStackPointerRegister, 0);
    }

    void emit_popfd(int fp) {
        emit_lfd(fp, operandStackPointerRegister, 0);
        emit_addi(operandStackPointerRegister, operandStackPointerRegister, 8);
    }

    /* branch I-form */
    // address in words
    void emit_b_i(int address, boolean absolute, boolean link) {
        if (addr_too_far(address, 24))
            throw new Error("address too far");
        cb.putInt((18 << 26) | (sign_mask(address, 24) << 2)
                | ((absolute ? 1 : 0) << 1) | ((link ? 1 : 0) << 0));
    }

    /*
     * branch I-form with deferred target using branches API; this form always
     * has a relative address.
     */
    Branch emit_b_i_d(boolean link) {
        Branch b = new Branch(getPC(), 2, 24);
        cb.putInt((18 << 26)
        /* leaving out address */
        | (0 << 1) | ((link ? 1 : 0) << 0));
        return b;
    }

    /* branch B-form */
    // address in words
    void emit_b_b(int bo, int bi, int address, boolean absolute, boolean link) {
        if (addr_too_far(address, 14))
            throw new Error("address too far");
        cb.putInt((16 << 26) | (bo << 21) | (bi << 16)
                | (sign_mask(address, 14) << 2) | ((absolute ? 1 : 0) << 1)
                | ((link ? 1 : 0) << 0));
    }

    /*
     * branch B-form with deferred target using branches API; this form always
     * has a relative address.
     */
    Branch emit_b_b_d(int bo, int bi, boolean link) {
        Branch b = new Branch(getPC(), 2, 14);
        cb.putInt((16 << 26) | (bo << 21) | (bi << 16)
        /* leaving out address */
        | (0 << 1) | ((link ? 1 : 0) << 0));
        return b;
    }

    void setBranchTarget(Branch branch) {
        int instruction = cb.getInt(startPosition + branch.sourcePC);
        int offsetField = sign_mask((getPC() - branch.sourcePC) >> 2, branch.bits) << branch.shift;
        cb.putInt(startPosition + branch.sourcePC, instruction | offsetField);
    }
    
    Branch setBranchTarget() {
        return new Branch(getPC());
    }
    
    void emit_b_i(Branch branch) {
        if (((branch.targetPC - getPC()) % 4) != 0) {
            throw new Error("Misalignment");
        }
        emit_b_i((branch.targetPC - getPC()) >> 2, false, false);
    }
    
    void emit_b_i_lk(Branch branch) {
        if (((branch.targetPC - getPC()) % 4) != 0) {
            throw new Error("Misalignment");
        }
        emit_b_i((branch.targetPC - getPC()) >> 2, false, true);
    }
    
    void emit_bc(int bo, int bi, Branch branch) {
        if (((branch.targetPC - getPC()) % 4) != 0) {
            throw new Error("Misalignment");
        }
        emit_b_b(bo, bi, (branch.targetPC - getPC()) >> 2, false, false);
    }
    Branch emit_bc_d(int bo, int bi) {
        return emit_b_b_d(bo, bi, false);
    }
    Branch emit_b_d() {
        return emit_b_i_d(false);
    }
    void emit_blr() {
        emit_b_xl_lr(0x14, 0, 0, false);
    }

    void emit_blr_lk() {
        emit_b_xl_lr(0x14, 0, 0, true);
    }

    void emit_bctr() {
        emit_b_xl_ctr(0x14, 0, 0, false);
    }

    void emit_bctr_lk() {
        emit_b_xl_ctr(0x14, 0, 0, true);
    }

    /** Branch and link to addr.  Possibly clobber R0. **/
    void emit_bl(VM_Address addr) {
	int delta = addr.diff(getAbsolutePC()).asInt();
	if (!addr_too_far(delta, 26))
	    emit_b_i(delta >> 2, false, true);
	else {
	    emit_li32(PPCConstants.R0, addr.asInt());
	    emit_mtctr(PPCConstants.R0);
	    emit_bctr_lk();
	}
    }

    /* branch XL-form, to LR */
    void emit_b_xl_lr(int bo, int bi, int bh, boolean link) {
        cb.putInt((19 << 26) | (bo << 21) | (bi << 16) | (0 << 13) | (bh << 11)
                | (16 << 1) | ((link ? 1 : 0) << 0));
    }

    /* branch XL-form, to CTR */
    void emit_b_xl_ctr(int bo, int bi, int bh, boolean link) {
        cb.putInt((19 << 26) | (bo << 21) | (bi << 16) | (0 << 13) | (bh << 11)
                | (528 << 1) | ((link ? 1 : 0) << 0));
    }

    /*
     * will skip syscall and condition register logic instructions for now,
     * since I'm not sure that I will need them.
     */

    void emit_lbz(int rt, int ra, int offset) {
        cb.putInt((34 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lbzx(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (87 << 1)
                | (0 << 0));
    }

    void emit_lbzu(int rt, int ra, int offset) {
        cb.putInt((35 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lbzux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (119 << 1) | (0 << 0));
    }

    void emit_lhz(int rt, int ra, int offset) {
        cb.putInt((40 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lhzx(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (279 << 1) | (0 << 0));
    }

    void emit_lhzu(int rt, int ra, int offset) {
        cb.putInt((41 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lhzux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (311 << 1) | (0 << 0));
    }

    void emit_lha(int rt, int ra, int offset) {
        cb.putInt((42 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lhax(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (343 << 1) | (0 << 0));
    }

    void emit_lhau(int rt, int ra, int offset) {
        cb.putInt((43 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lhaux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (375 << 1) | (0 << 0));
    }

    void emit_lwz(int rt, int ra, int offset) {
        cb.putInt((32 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lwzx(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (23 << 1)
                | (0 << 0));
    }

    void emit_lwzu(int rt, int ra, int offset) {
        cb.putInt((33 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lwzux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (55 << 1)
                | (0 << 0));
    }

    void emit_lwa(int rt, int ra, int offset) {
        cb.putInt((58 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) & ~3) | (2 << 0));
    }

    void emit_lwax(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (341 << 1) | (0 << 0));
    }

    void emit_lwaux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (373 << 1) | (0 << 0));
    }

    void emit_ld(int rt, int ra, int offset) {
        cb.putInt((58 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) & ~3) | (0 << 0));
    }

    void emit_ldx(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (21 << 1)
                | (0 << 0));
    }

    void emit_ldu(int rt, int ra, int offset) {
        cb.putInt((58 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) & ~3) | (1 << 0));
    }

    void emit_ldux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (53 << 1)
                | (0 << 0));
    }

    /* pointer load */
    void emit_lp(int rt, int ra, int offset) {
        if (is64bit) {
            emit_ld(rt, ra, offset);
        } else {
            emit_lwz(rt, ra, offset);
        }
    }

    void emit_lpu(int rt, int ra, int offset) {
        if (is64bit) {
            emit_ldu(rt, ra, offset);
        } else {
            emit_lwzu(rt, ra, offset);
        }
    }

    void emit_lpx(int rt, int ra, int rb) {
        if (is64bit) {
            emit_ldx(rt, ra, rb);
        } else {
            emit_lwzx(rt, ra, rb);
        }
    }

    void emit_lpux(int rt, int ra, int rb) {
        if (is64bit) {
            emit_ldux(rt, ra, rb);
        } else {
            emit_lwzux(rt, ra, rb);
        }
    }

    /* and now come the store instructions... */

    void emit_stb(int rt, int ra, int offset) {
        cb.putInt((38 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_stbx(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (215 << 1) | (0 << 0));
    }

    void emit_stbu(int rt, int ra, int offset) {
        cb.putInt((39 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_stbux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (247 << 1) | (0 << 0));
    }

    void emit_sth(int rt, int ra, int offset) {
        cb.putInt((44 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_sthx(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (407 << 1) | (0 << 0));
    }

    void emit_sthu(int rt, int ra, int offset) {
        cb.putInt((45 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_sthux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (439 << 1) | (0 << 0));
    }

    void emit_stw(int rt, int ra, int offset) {
        cb.putInt((36 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_stwx(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (151 << 1) | (0 << 0));
    }

    void emit_stwu(int rt, int ra, int offset) {
        cb.putInt((37 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_stwux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (183 << 1) | (0 << 0));
    }

    void emit_std(int rt, int ra, int offset) {
        cb.putInt((62 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) & ~3) | (0 << 0));
    }

    void emit_stdx(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (149 << 1) | (0 << 0));
    }

    void emit_stdu(int rt, int ra, int offset) {
        cb.putInt((62 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) & ~3) | (1 << 0));
    }

    void emit_stdux(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11)
                | (181 << 1) | (0 << 0));
    }

    /* pointer store */
    void emit_stp(int rt, int ra, int offset) {
        if (is64bit) {
            emit_std(rt, ra, offset);
        } else {
            emit_stw(rt, ra, offset);
        }
    }

    void emit_stpu(int rt, int ra, int offset) {
        if (is64bit) {
            emit_stdu(rt, ra, offset);
        } else {
            emit_stwu(rt, ra, offset);
        }
    }

    void emit_stpx(int rt, int ra, int rb) {
        if (is64bit) {
            emit_stdx(rt, ra, rb);
        } else {
            emit_stwx(rt, ra, rb);
        }
    }

    void emit_stpux(int rt, int ra, int rb) {
        if (is64bit) {
            emit_stdux(rt, ra, rb);
        } else {
            emit_stwux(rt, ra, rb);
        }
    }

    /*
     * don't need byte-reversing instructions since I plan on only generating
     * parsers that deal with big endian data.
     */

    void emit_lmw(int rt, int ra, int offset) {
        cb.putInt((46 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_stmw(int rs, int ra, int offset) {
        cb.putInt((47 << 26) | (rs << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    /*
     * I don't give a crap about fixed point string load/store instructions. if
     * I find that I should have a reason to give a crap about them, I'll
     * include them.
     */

    void emit_addi(int rt, int ra, int value) {
        cb.putInt((14 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(value, 16) << 0));
    }

    void emit_addis(int rt, int ra, int value) {
        cb.putInt((15 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(value, 16) << 0));
    }

    /*
     * for arithmetic I assume that nobody gives a crap about carry bits and
     * control registers. I may be wrong, and if so, I'll add that ability.
     */

    void emit_add(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (266 << 1) | (0 << 0));
    }

    void emit_addc(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (10 << 1) | (0 << 0));
    }

    void emit_addic(int rt, int ra, int value) {
        cb.putInt((12 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(value, 16) << 0));
    }
    void emit_addicd(int rt, int ra, int value) {
        cb.putInt((13 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(value, 16) << 0));
    }

    void emit_adde(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (138 << 1) | (0 << 0));
    }

    void emit_sub(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (rb << 16) | (ra << 11) | (0 << 10)
                | (40 << 1) | (0 << 0));
    }

    void emit_subc(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (rb << 16) | (ra << 11) | (0 << 10)
                | (8 << 1) | (0 << 0));
    }

    void emit_sube(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (rb << 16) | (ra << 11) | (0 << 10)
                | (136 << 1) | (0 << 0));
    }

    void emit_subfic(int rd, int ra, int value) {
        cb.putInt((8 << 26) | (rd << 21) | (ra << 16)
                | (sign_mask(value, 16) << 0));
    }

    void emit_neg(int rt, int ra) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (0 << 11) | (0 << 10)
                | (104 << 1) | (0 << 0));
    }

    void emit_mulli(int rt, int ra, int value) {
        cb.putInt((7 << 26) | (rt << 21) | (ra << 16)
                | (sign_mask(value, 16) << 0));
    }

    void emit_mulld(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (233 << 1) | (0 << 0));
    }

    void emit_mullw(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (235 << 1) | (0 << 0));
    }

    void emit_mullhd(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (73 << 1) | (0 << 0));
    }

    void emit_mulhw(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (75 << 1) | (0 << 0));
    }

    void emit_mulhdu(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (9 << 1) | (0 << 0));
    }

    void emit_mulhwu(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (11 << 1) | (0 << 0));
    }

    void emit_divd(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (489 << 1) | (0 << 0));
    }

    void emit_divw(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (491 << 1) | (0 << 0));
    }

    void emit_divdu(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (457 << 1) | (0 << 0));
    }

    void emit_divwu(int rt, int ra, int rb) {
        cb.putInt((31 << 26) | (rt << 21) | (ra << 16) | (rb << 11) | (0 << 10)
                | (459 << 1) | (0 << 0));
    }

    /* move to special purpose register */

    void emit_mtspr(int rs, int spr) {
        cb.putInt((31 << 26) | (rs << 21)
                | ((((spr & 31) << 5) | ((spr >> 5) & 31)) << 11) | (467 << 1)
                | (0 << 0));
    }

    void emit_mtxer(int rs) {
        emit_mtspr(rs, 1);
    }

    void emit_mtlr(int rs) {
        emit_mtspr(rs, 8);
    }

    void emit_mtctr(int rs) {
        emit_mtspr(rs, 9);
    }

    /* move from special purpose register */

    void emit_mfspr(int rt, int spr) {
        cb.putInt((31 << 26) | (rt << 21)
                | ((((spr & 31) << 5) | ((spr >> 5) & 31)) << 11) | (339 << 1)
                | (0 << 0));
    }

    void emit_mfxer(int rt) {
        emit_mfspr(rt, 1);
    }

    void emit_mflr(int rt) {
        emit_mfspr(rt, 8);
    }

    void emit_mfctr(int rt) {
        emit_mfspr(rt, 9);
    }

    void emit_mtcrf(int fxm, int rs) {
        cb.putInt((31 << 26) | (rs << 21) | (0 << 20) | (fxm << 12)
                | (144 << 1) | (0 << 0));
    }

    void emit_mtcr(int rs) {
        emit_mtcrf(0xff, rs);
    }

    void emit_mfcr(int rt) {
        cb.putInt((31 << 26) | (rt << 21) | (0 << 20) | (0 << 11) | (19 << 1)
                | (0 << 0));
    }

    /* compare instructions */
    void emit_cmpi32(int bf, boolean double_word, int ra, int value, int scratch_reg) {
	try {
	    checkRange(value, 16);
	    // short
	    emit_cmpi(bf, double_word, ra, value);
	} catch (OutOfRange _) {
	    // long
	    emit_li32(scratch_reg, value);
	    emit_cmp(bf, double_word, ra, scratch_reg);
	}
    }

    void emit_cmpi(int bf, boolean double_word, int ra, int value) {
        cb.putInt((11 << 26) | (bf << 23) | (0 << 22)
                | ((double_word ? 1 : 0) << 21) | (ra << 16)
                | (sign_mask(value, 16) << 0));
    }

    void emit_cmp(int bf, boolean double_word, int ra, int rb) {
        cb.putInt((31 << 26) | (bf << 23) | (0 << 22)
                | ((double_word ? 1 : 0) << 21) | (ra << 16) | (rb << 11)
                | (0 << 1) | (0 << 0));
    }

    void emit_cmpli(int bf, boolean double_word, int ra, int value) {
        cb.putInt((10 << 26) | (bf << 23) | (0 << 22)
                | ((double_word ? 1 : 0) << 21) | (ra << 16) | (value << 0));
    }

    void emit_cmpl(int bf, boolean double_word, int ra, int rb) {
        cb.putInt((31 << 26) | (bf << 23) | (0 << 22)
                | ((double_word ? 1 : 0) << 21) | (ra << 16) | (rb << 11)
                | (32 << 1) | (0 << 0));
    }

    /* logical instructions */
    void emit_andi(int ra, int rs, int value) {
        /* FIXME: this should use rlwinm instead of using andi. */
        cb.putInt((28 << 26) | (rs << 21) | (ra << 16)
                | (sign_mask(value, 16) << 0));
    }

    void emit_ori(int ra, int rs, int value) {
        cb.putInt((24 << 26) | (rs << 21) | (ra << 16)
                | ((value & 0xffff) << 0));
    }

    void emit_oris(int ra, int rs, int value) {
        cb.putInt((25 << 26) | (rs << 21) | (ra << 16)
                | ((value & 0xffff) << 0));
    }

    void emit_xori(int ra, int rs, int value) {
        cb.putInt((26 << 26) | (rs << 21) | (ra << 16)
                | ((value & 0xffff) << 0));
    }

    void emit_xoris(int ra, int rs, int value) {
        cb.putInt((27 << 26) | (rs << 21) | (ra << 16)
                | ((value & 0xffff) << 0));
    }

    void emit_and(int ra, int rs, int rb) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (rb << 11) | (28 << 1)
                | (0 << 0));
    }

    void emit_or(int ra, int rs, int rb) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (rb << 11)
                | (444 << 1) | (0 << 0));
    }

    void emit_xor(int ra, int rs, int rb) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (rb << 11)
                | (316 << 1) | (0 << 0));
    }

    void emit_nand(int ra, int rs, int rb) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (rb << 11)
                | (476 << 1) | (0 << 0));
    }

    void emit_nor(int ra, int rs, int rb) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (rb << 11)
                | (124 << 1) | (0 << 0));
    }

    void emit_srd(int ra, int rs, int rb) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (rb << 11)
                | (539 << 1) | (0 << 0));
    }

    void emit_srp(int ra, int rs, int rb) {
        if (is64bit) {
            emit_srd(ra, rs, rb);
        } else {
            emit_srw(ra, rs, rb);
        }
    }

    void emit_sradi(int ra, int rs, int sh) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16)
                | (((sh & 62) >> 1) << 11) | (413 << 2) | ((sh & 1) << 1)
                | (0 << 0));
    }

    void emit_srawi(int ra, int rs, int sh) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (sh << 11)
                | (824 << 1) | (0 << 0));
    }

    void emit_extsb(int ra, int rs) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (0 << 11) | (954 << 1)
                | (0 << 0));
    }

    void emit_extsh(int ra, int rs) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (0 << 11) | (922 << 1)
                | (0 << 0));
    }

    void emit_extsw(int ra, int rs) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (0 << 11) | (986 << 1)
                | (0 << 0));
    }

    /* bunch of fucking confusing mother fuckers */
    void emit_cntlzw(int ra, int rs) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (0 << 11) | (26 << 1)
                | (0 << 0));
    }

    void emit_rldicl(int ra, int rs, int sh, int mb) {
        cb.putInt((30 << 26) | (rs << 21) | (ra << 16)
                | (((sh & 62) >> 1) << 11) | (mb << 5) | (0 << 2)
                | ((sh & 1) << 1) | (0 << 0));
    }

    void emit_rldicr(int ra, int rs, int sh, int mb) {
        cb.putInt((30 << 26) | (rs << 21) | (ra << 16)
                | (((sh & 62) >> 1) << 11) | (mb << 5) | (1 << 2)
                | ((sh & 1) << 1) | (0 << 0));
    }

    void emit_srdi(int ra, int rs, int n) {
        emit_rldicl(ra, rs, 64 - n, n);
    }

    void emit_rldic(int ra, int rs, int sh, int mb) {
        cb.putInt((30 << 26) | (rs << 21) | (ra << 16)
                | (((sh & 62) >> 1) << 11) | (mb << 5) | (2 << 2)
                | ((sh & 1) << 1) | (0 << 0));
    }

    void emit_rlwinm(int ra, int rs, int sh, int mb, int me) {
        cb.putInt((21 << 26) | (rs << 21) | (ra << 16) | (sh << 11) | (mb << 6)
                | (me << 1) | (0 << 0));
    }

    void emit_srwi(int ra, int rs, int n) {
        emit_rlwinm(ra, rs, 32 - n, n, 31);
    }

    void emit_slwi(int ra, int rs, int n) {
        emit_rlwinm(ra, rs, n, 0, 31 - n);
    }

    void emit_slw(int ra, int rs, int rb) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (rb << 11) | (24 << 1) | (0 << 0));
    }
    void emit_srw(int ra, int rs, int rb) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (rb << 11) | (536 << 1) | (0 << 0));
    }
    void emit_sraw(int ra, int rs, int rb) {
        cb.putInt((31 << 26) | (rs << 21) | (ra << 16) | (rb << 11) | (792 << 1) | (0 << 0));
    }
    
    
    void emit_srpi(int ra, int rs, int n) {
        if (is64bit) {
            emit_srdi(ra, rs, n);
        } else {
            emit_srwi(ra, rs, n);
        }
    }

    /* a nice little helper */
    void emit_daddi(int rt, int ra, int value) {
        int abs_value, high_value, remainder, sign;

        if (value < 0) {
            abs_value = -value;
            sign = -1;
        } else {
            abs_value = value;
            sign = 1;
        }

        if (value < -2147450879) {
            /* special case to avoid overflow */
            high_value = -2147483648;
        } else if ((abs_value & 32768) != 0
                && (value > 0 || (abs_value & 32767) != 0)
                && value < 2147450879) {
            high_value = sign * ((abs_value + 65535) & ~65535);
        } else {
            high_value = sign * (abs_value & ~65535);
        }

        if (high_value != 0) {
            emit_addis(rt, ra, high_value >> 16);
            ra = rt;
        }

        remainder = value - high_value;

        while (remainder != 0) {
            if (remainder > 32767) {
                emit_addi(rt, ra, 32767);
                remainder -= 32767;
            } else if (remainder < -32768) {
                emit_addi(rt, ra, -32768);
                remainder += 32768;
            } else {
                emit_addi(rt, ra, remainder);
                break;
            }
            ra = rt;
        }
    }

    /*
     * double load immediate; this is a combination of 1, 2, or 3 other
     * instructions.
     */
    /* FIXME: this will not do an int load correctly in 64-bit mode! */
    /* ... right! so, use udli instead! */
    void emit_dli(int rt, int value) {
        if (value == 0) {
            emit_li(rt, 0);
        }
        emit_daddi(rt, 0, value);
    }

    /*
     * double load immediate of int value. this is a combination of 1, 2, or 3
     * other instructions.
     */
    void emit_udli(int rt, int value) {
        emit_li(rt, value & 32767);

        if ((value & 32768) != 0) {
            emit_ori(rt, rt, 32768);
        }

        if ((value & (32767 << 16)) != 0) {
            emit_oris(rt, rt, value >> 16);
        }
    }

    /* side-effects r0 */
    void emit_dcmpli(int bf, boolean double_word, int ra, int value) {
        /* if double_word==emit_false, this can be optimized quite a bit */

        if (value < 65536) {
            emit_cmpli(bf, double_word, ra, value);
        }

        emit_udli(0, value);
        emit_cmpl(bf, double_word, ra, 0);
    }

    /* silly floating point crap */
    void emit_lfs(int frt, int ra, int offset) {
        cb.putInt((48 << 26) | (frt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lfsx(int frt, int ra, int rb) {
        cb.putInt((31 << 26) | (frt << 21) | (ra << 16) | (rb << 11)
                | (535 << 1) | (0 << 0));
    }

    void emit_lfsu(int frt, int ra, int offset) {
        cb.putInt((49 << 26) | (frt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lfsux(int frt, int ra, int rb) {
        cb.putInt((31 << 26) | (frt << 21) | (ra << 16) | (rb << 11)
                | (567 << 1) | (0 << 0));
    }

    void emit_lfd(int frt, int ra, int offset) {
        cb.putInt((50 << 26) | (frt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lfdx(int frt, int ra, int rb) {
        cb.putInt((31 << 26) | (frt << 21) | (ra << 16) | (rb << 11)
                | (599 << 1) | (0 << 0));
    }

    void emit_lfdu(int frt, int ra, int offset) {
        cb.putInt((51 << 26) | (frt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_lfdux(int frt, int ra, int rb) {
        cb.putInt((31 << 26) | (frt << 21) | (ra << 16) | (rb << 11)
                | (631 << 1) | (0 << 0));
    }

    void emit_stfs(int frt, int ra, int offset) {
        cb.putInt((52 << 26) | (frt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_stfsx(int frt, int ra, int rb) {
        cb.putInt((31 << 26) | (frt << 21) | (ra << 16) | (rb << 11)
                | (663 << 1) | (0 << 0));
    }

    void emit_stfsu(int frt, int ra, int offset) {
        cb.putInt((53 << 26) | (frt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_stfsux(int frt, int ra, int rb) {
        cb.putInt((31 << 26) | (frt << 21) | (ra << 16) | (rb << 11)
                | (695 << 1) | (0 << 0));
    }

    public int getPreviousWord() {
        return cb.getInt(cb.position()-4);
    }
    
    void emit_stfd(int frt, int ra, int offset) {
        cb.putInt((54 << 26) | (frt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_stfdx(int frt, int ra, int rb) {
        cb.putInt((31 << 26) | (frt << 21) | (ra << 16) | (rb << 11)
                | (727 << 1) | (0 << 0));
    }

    void emit_stfdu(int frt, int ra, int offset) {
        cb.putInt((55 << 26) | (frt << 21) | (ra << 16)
                | (sign_mask(offset, 16) << 0));
    }

    void emit_stfdux(int frt, int ra, int rb) {
        cb.putInt((31 << 26) | (frt << 21) | (ra << 16) | (rb << 11)
                | (759 << 1) | (0 << 0));
    }

    void emit_stfiwx(int frs, int ra, int rb) {
        cb.putInt((31 << 26) | (frs << 21) | (ra << 16) | (rb << 11)
                | (983 << 1) | (0 << 0));
    }

    void emit_fmr(int frt, int frb) {
        cb.putInt((63 << 26) | (frt << 21) | (0 << 16) | (frb << 11)
                | (72 << 1) | (0 << 0));
    }

    void emit_fneg(int frt, int frb) {
        cb.putInt((63 << 26) | (frt << 21) | (0 << 16) | (frb << 11)
                | (40 << 1) | (0 << 0));
    }

    void emit_fabs(int frt, int frb) {
        cb.putInt((63 << 26) | (frt << 21) | (0 << 16) | (frb << 11)
                | (264 << 1) | (0 << 0));
    }

    void emit_fnabs(int frt, int frb) {
        cb.putInt((63 << 26) | (frt << 21) | (0 << 16) | (frb << 11)
                | (136 << 1) | (0 << 0));
    }

    void emit_frsp(int frd, int frb) {
        cb.putInt((63 << 26) | (frd << 21) | (0 << 16) | (frb << 11)
                | (12 << 1) | (0 << 0));
    }
    
    void emit_fcmpu(int crfd, int fra, int frb) {
        cb.putInt((63 << 26) | (crfd << 23) | (fra << 16) | (frb << 11)
                | (0 << 1));
    }
    void emit_fctiwz(int frd, int frb) {
        cb.putInt((63 << 26) | (frd << 21) | (0 << 16) | (frb << 11)
                | (15 << 1) | (0 << 0));
    }
    void emit_fctiw(int frd, int frb) {
        cb.putInt((63 << 26) | (frd << 21) | (0 << 16) | (frb << 11)
                | (14 << 1) | (0 << 0));
    }
    
    void emit_fadd(int frt, int fra, int frb) {
        cb.putInt((63 << 26) | (frt << 21) | (fra << 16) | (frb << 11)
                | (0 << 6) | (21 << 1) | (0 << 0));
    }

    void emit_fadds(int frt, int fra, int frb) {
        cb.putInt((59 << 26) | (frt << 21) | (fra << 16) | (frb << 11)
                | (0 << 6) | (21 << 1) | (0 << 0));
    }

    void emit_fsub(int frt, int fra, int frb) {
        cb.putInt((63 << 26) | (frt << 21) | (fra << 16) | (frb << 11)
                | (0 << 6) | (20 << 1) | (0 << 0));
    }

    void emit_fsubs(int frt, int fra, int frb) {
        cb.putInt((59 << 26) | (frt << 21) | (fra << 16) | (frb << 11)
                | (0 << 6) | (20 << 1) | (0 << 0));
    }

    void emit_fmul(int frt, int fra, int frb) {
        cb.putInt((63 << 26) | (frt << 21) | (fra << 16) | (0 << 11)
                | (frb << 6) | (25 << 1) | (0 << 0));
    }

    void emit_fmuls(int frt, int fra, int frb) {
        cb.putInt((59 << 26) | (frt << 21) | (fra << 16) | (0 << 11)
                | (frb << 6) | (25 << 1) | (0 << 0));
    }
    
    void emit_fdiv(int frt, int fra, int frb) {
        cb.putInt((63 << 26) | (frt << 21) | (fra << 16) | (frb << 11)
                | (0 << 6) | (18 << 1) | (0 << 0));
    }

    void emit_fdivs(int frt, int fra, int frb) {
        cb.putInt((59 << 26) | (frt << 21) | (fra << 16) | (frb << 11)
                | (0 << 6) | (18 << 1) | (0 << 0));
    }
}
