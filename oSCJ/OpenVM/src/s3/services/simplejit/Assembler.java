package s3.services.simplejit;

import ovm.util.FixedByteBuffer;
import ovm.core.domain.Oop;
import ovm.core.services.memory.VM_Address;
import ovm.core.domain.Blueprint;

/**
 * An abstract type of assemblers
 *
 * @author Hiroshi Yamauchi
 **/
public abstract class Assembler {

    protected FixedByteBuffer cb;
    protected int startPosition;

    protected Assembler(FixedByteBuffer cb, int startPosition) {
	this.cb = cb;
	this.startPosition = startPosition;
    }

    public int getPC() {
        return cb.position() - startPosition;
    }

    public VM_Address getAbsolutePC() {
	Oop cbOop = VM_Address.fromObject(cb.array()).asOop();
	Blueprint.Array bp = cbOop.getBlueprint().asArray();
	return bp.addressOfElement(cbOop, cb.position());
    }

    /* 
     * Note on the branch mechanism: since calculating branch offsets is tedious,
     * use the following convention:
     * ex.
     * (forward jump)
     *     ...
     *     Branch b = asm.setBranchSourceAndJcc(J_E);
     *     ...
     *     ...
     *     asm.setBranchTarget(b);
     *
     * (backward jump)
     *     ...
     *     Branch b = asm.setBranchTarget();
     *     ...
     *     ...
     *     asm.setBranchSourceAndJmp(b);
     *
     * By this mechanism, programmers do not need to calculate branch offsets.
     * This automatically patches up the jump offset in the jump instructions.
     * The only disadvantage of this approach is that not being to able to use 
     * 1-byte field of branch offsets.
     */
    public static class Branch {
        /*
         * We need to patch the branch instruction at sourcePC.
         * The branch offset location within the instruction is
         * written as code[sourcePC] = sign_mask((targetPC-sourcePC),bits)<<shift;
         */
        public int sourcePC; // The PC of the branch instruction
        public int shift;
        public int bits;
        public int targetPC; // The PC of the target instruction
        
        private Branch(int sourcePC, int shift, int bits, int targetPC) {
            this.sourcePC = sourcePC;
            this.shift = shift;
            this.bits = bits;
            this.targetPC = targetPC;
        }
        public Branch(int sourcePC, int shift, int bits) {
            this(sourcePC, shift, bits, -1);
        }
        public Branch(int targetPC) {
            this(-1, 0, 0, targetPC);
        }
        public Branch(int spc, int tpc) {
            this(spc, 32, 0, tpc);
        }

        public Branch copy() {
            return new Branch(this.sourcePC, this.targetPC);
        }
    }
}
