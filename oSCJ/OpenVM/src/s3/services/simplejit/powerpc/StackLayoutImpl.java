package s3.services.simplejit.powerpc;

import ovm.util.OVMError;
import s3.services.simplejit.StackLayout;

/**
 * The StackLayout for PowerPC
 * @author Hiroshi Yamauchi
 **/
public class StackLayoutImpl extends StackLayout {

    /**
     * The fixed size of the param area in bytes. This would be variable once we have analysis
     * to compute the maximum parameter size of the methods that the method may call.
     */
    public static final int PARAM_AREA_SIZE = 64 * 4;
    
    public StackLayoutImpl(int maxLocals, 
               int maxStack, 
               int argLength) {
        super(maxLocals, maxStack, argLength);
    }

    /**
     * Native stack frame layout:
     * <Caller>
     *            ...
     *            arg[argLength-1] (args for Callee)
     *            arg[argLength-2]
     *            ...
     *            arg[0] (=this)
     *            SimpleJITCode for Callee
     * PrevSP+8   LR (=RetAddr to Caller)
     * PrevSP+4   CR
     * PrevSP --> PrevPrevSP (=SP of Caller's caller)
     *            Caller-saved r31  (32 x 4 bytes = 128 bytes)
     *            Caller-saved r30
     *            ...
     * PrevSP-128 Caller-saved r0
     * PrevSP-136 Caller-saved fp31 (32 x 8 bytes = 256 bytes)
     *            ...
     * PrevSP-384 Caller-saved fp0 (-384+14*8(112) = -272)
     * <Callee>   <padding> (register save area = 384 bytes)
     *            local[argLength]
     *            local[argLength+1]
     *            ...
     *            local[maxLocals-1]
     *            stack[0]
     *            ...
     *            stack[maxStack-1]
     *            (arg area for the method called by Callee)
     *            LR (RetAddr to Callee)
     *            CR
     *     SP --> PrevSP (=Caller's SP)
     **/
    protected void defineNativeStackFrame() {
        // Even if there is no FP on PowerPC, the offsets from the previous SP are computed here
        // because the stack frame size is not determined here (due to the arg area)
        int[] j2n = this.javaStack2NativeStack;
        for (int i = 0; i < j2n.length; i++) {
            if (i < maxLocals) {
                if (i < argLength) {
                    j2n[i] = 4 * (4 + i);
                } else {
                    j2n[i] = -384 - 4 * ((maxLocals - argLength) - (i - argLength));
                }
            } else {
                j2n[i] = -384 - 4 * (1 + i - argLength);
            }
        }
    }

    public int getReceiverOffset() {
        if (argLength <= 0)
            throw new OVMError.Internal("no this pointer");
        return 16;
    }

    public int getCodeFragmentOffset() {
        return 12;
    }

    public int getReturnAddressOffset() {
        return 8;
    }
    
    /**
     * Return the frame size in words. This includes the register save area and non-argument 
     * local variables, stack, arg area and linkage area.
     */
    public int getNativeFrameSize() {
        return 384 + 4 * (maxLocals - argLength + maxStack) + PARAM_AREA_SIZE + 12;
    }
    
    public int getGeneralRegisterOffset(int register) {
        if (0 <= register && register <= 31) {
            return - 4 * (32 - register);
        }
        throw new Error();
    }
    
    public int getFPRegisterOffset(int register) {
        if (0 <= register && register <= 31) {
            return - 4 * 32 - (32 - register) * 8;
        }
        throw new Error();
    }
    
    public int getInitialOperandStackPointerOffset() {
        if (getMaxStack() > 0) {
            return getOperandStackNativeOffset(0) + 4;
        } else if (getMaxLocals() > getArgLength()) {
            return getLocalVariableNativeOffset(getMaxLocals() - 1);
        } else {
            return -384;
        }
    }
    /**
     * Return the C Functions defining the stack frame layout as text
     */
    public static String getStackLayoutAsCFunction() {
        String f = "";
        f += "static int getStackFrameLocalOffset(int maxLocals, int argLength, int index) {\n"
                + "\tif (index < argLength)\n"
                + "\t\treturn 4 + index;\n"
                + "\telse\n"
                + "\t\treturn - (96 + (maxLocals - argLength) - (index - argLength));\n"
                + "}\n";
        f += "static int getStackFrameOperandStackOffset(int maxLocals, int argLength, int index) {\n"
                + "\treturn - (index + 96 + 1 + (maxLocals - argLength));\n" + "}\n";
        f += "static int getStackFrameCodeOffset() {\n" + "\treturn 3;\n"
                + "}\n";
        return f;
    }
}
