package s3.services.simplejit.x86;

import ovm.util.ByteBuffer;
import ovm.util.HTString2int;
import s3.services.simplejit.CodeLinker;

/**
 * @author Hiroshi Yamauchi
 **/
public class CodeLinkerImpl extends CodeLinker {

    public CodeLinkerImpl(int[] bytecodePC2NativePC) {
	super(bytecodePC2NativePC);
    }

    protected RelativeJump makeRelativeJump(int branchPC,
					    int shift,
					    int bits,
					    int targetBytecodePC) {
	return new RelativeJumpImpl(branchPC, shift, bits,
				    targetBytecodePC);
    }
	
    public static class RelativeJumpImpl extends RelativeJump {
	private RelativeJumpImpl(int branchPC, int shift, int bits,
				 int targetBytecodePC) {
	    super(branchPC, shift, bits, targetBytecodePC);
	}
	public void patch(ByteBuffer code, int[] bytecodePC2NativePC,
			  int offset) {
	    int targetNativePC = bytecodePC2NativePC[targetBytecodePC];
	    // shift and bits are ignored for now under x86
	    code.putInt(branchPC + offset, targetNativePC - branchPC - 4);
	}
    }
}
