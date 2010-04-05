package s3.services.simplejit.x86;

import ovm.util.OVMError;
import s3.services.simplejit.StackLayout;

/**
 * @author Hiroshi Yamauchi
 **/
public class StackLayoutImpl extends StackLayout {

    public StackLayoutImpl(int maxLocals, 
			   int maxStack, 
			   int argLength) {
	super(maxLocals, maxStack, argLength);
    }

    /**
     * Native stack frame layout:
     *
     * esp --> stack [top]
     *         stack [top-1]
     *         stack [top-2]
     *         ...
     *         stack [0]
     *         local [argLength]
     *         local [argLength+1]
     *         local [argLength+2]
     *         ...
     * -28     local [maxLocals-1]
     * -24     caller-saved eax
     * -20     caller-saved ecx
     * -16     caller-saved edx
     * -12     callee-saved ebx
     * -8      callee-saved esi
     * -4      callee-saved edi
     * ebp --> previous ebp
     * +4      return address
     * +8      code fragment
     * +12     arg   [0] (this pointer)
     * +16     arg   [1]
     *         ...
     *         arg   [argLength-2]
     *         arg   [argLength-1]
     **/
    protected void defineNativeStackFrame() {
	int[] j2n = this.javaStack2NativeStack;
	for (int i = 0 ; i < j2n.length; i++) {
	    if (i < maxLocals) {
		if (i < argLength) {
		    j2n[i] = 4 * (3 + i);
		} else {
		    j2n[i] = - 4 * (6 + (maxLocals - argLength) 
				    - (i - argLength));
		}
	    } else {
            // there is a bug : it should be - (4 * (i + 7 - argLength))
		j2n[i] = - (4 * (i + 7 + (maxLocals - argLength)));
	    }
	}
    }

    public int getReceiverOffset() {
	if (argLength <= 0)
	    throw new OVMError.Internal("no this pointer");
	return 12;
    }

    public int getCodeFragmentOffset() {
	return 8;
    }

    /**
     * Return the frame size in bytes. This does not include the three
     * words occupied by the callee-saved registers.
     */
    public int getNativeFrameSize() {
	return 4 * (3 + maxLocals - argLength);
    }

    public int getReturnAddressOffset() {
        return 4;
    }
    
    public int getGeneralRegisterOffset(int register) {
        throw new Error();
    }
    
    public int getFPRegisterOffset(int register) {
        throw new Error();
    }
    
    public int getInitialOperandStackPointerOffset() {
        throw new Error();
    }
    
    /**
     * Return the C Functions defining the stack frame layout as text
     */
    public static String getStackLayoutAsCFunction() {
	String f = "";
	f +=  "static int getStackFrameLocalOffset(int maxLocals, int argLength, int index) {\n"
	    + "\tif (index < argLength)\n"
	    + "\t\treturn 3 + index;\n"
	    + "\telse\n"
	    + "\t\treturn - (6 + (maxLocals - argLength) - (index - argLength));\n"
	    + "}\n";
	f +=  "static int getStackFrameOperandStackOffset(int maxLocals, int argLength, int index) {\n"
	    + "\treturn - (index + 7 + (maxLocals - argLength));\n"
	    + "}\n";
	f +=  "static int getStackFrameCodeOffset() {\n"
	    + "\treturn 2;\n"
	    + "}\n";
	return f;
    }
}
