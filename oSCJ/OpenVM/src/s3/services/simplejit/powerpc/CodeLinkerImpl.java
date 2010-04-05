package s3.services.simplejit.powerpc;

import ovm.util.ByteBuffer;
import s3.services.simplejit.CodeLinker;

/**
 * @author Hiroshi Yamauchi
 */
public class CodeLinkerImpl extends CodeLinker {

    public CodeLinkerImpl(int[] bytecodePC2NativePC) {
        super(bytecodePC2NativePC);
    }

    protected RelativeJump makeRelativeJump(int branchPC, int shift, int bits,
            int targetBytecodePC) {
        return new RelativeJumpImpl(branchPC, shift, bits, targetBytecodePC);
    }

    public static class RelativeJumpImpl extends RelativeJump {
        private static void check_range(int value, int width) {
            int shifted = value >> (width - 1);
            if (shifted != 0 && shifted != -1) {
                throw new Error("Out-of-range relative jump : offset = " + value
				+ " width = " + width);
            }
        }
        private static int sign_mask(int value, int num_bits) {
            return value & ((1 << num_bits) - 1);
        }

        private RelativeJumpImpl(int branchPC, int shift, int bits,
				 int targetBytecodePC) {
            super(branchPC, shift, bits, targetBytecodePC);
        }

        public void patch(ByteBuffer code, int[] bytecodePC2NativePC,
			  int offset) {
            int targetNativePC = bytecodePC2NativePC[targetBytecodePC];
            check_range((targetNativePC - branchPC) >> 2, bits);
            int value = code.getInt(offset + branchPC);
            value &= ~(sign_mask(0xFFFFFFFF, bits) << shift);
            value |= sign_mask((targetNativePC - branchPC) >> 2, bits) << shift;
            code.putInt(offset + branchPC, value);
        }
    }
}
