package s3.services.simplejit;

import ovm.core.services.io.BasicIO;

/**
 * @author Hiroshi Yamauchi
 **/
public abstract class StackLayout {

    /**
     * A mapping from a local slot to its offset from the base pointer
     *
     * offsets         : elements
     * 0               : local var 0
     * 1               : local var 1
     * ...
     * (maxLocals - 1) : local var (maxLocals - 1)
     * (maxLocals)     : stack slot 0
     * (maxLocals + 1) : stack slot 1
     * ...
     * (maxLocals + maxStack - 1) : stack slot (maxStack - 1)
     **/
    protected int[] javaStack2NativeStack;
    protected final int maxStack;
    protected final int maxLocals;
    protected final int argLength;

    /**
     * @param maxLocals
     * @param maxStack
     * @param argLength the number of words that arguments takes including this pointer
     **/
    protected StackLayout(int maxLocals, int maxStack, int argLength) {
	this.maxLocals = maxLocals;
    if (maxStack > 65000) {
        // there is a bug in maxStack
        maxStack = 64;
    }
	this.maxStack = maxStack;
	this.argLength = argLength;
	this.javaStack2NativeStack 
	    = new int[this.maxLocals + this.maxStack];
	for (int i = 0; i < this.javaStack2NativeStack.length; i++) {
	    this.javaStack2NativeStack[i] = -1;
	}
	defineNativeStackFrame();
    }

    public int getMaxStack() {
	return maxStack;
    }

    public int getMaxLocals() {
	return maxLocals;
    }

    public int getArgLength() {
	return argLength;
    }

    private int getNativeOffset(int joffset) {
	try {
	    return this.javaStack2NativeStack[joffset];
	} catch (ArrayIndexOutOfBoundsException e) {
	    BasicIO.err.println("joffset = " + joffset + ", "
			       + "maxStack = " + maxStack + ", "
			       + "maxLocals = " + maxLocals + ", "
			       + "argLength = " + argLength);
	    e.printStackTrace();
        throw e;
	}
	//return -1;
    }

    public abstract int getInitialOperandStackPointerOffset(); 
    public abstract int getGeneralRegisterOffset(int register);
    
    public abstract int getFPRegisterOffset(int register);
       
    public int getLocalVariableNativeOffset(int localIndex) {
        return getNativeOffset(localIndex);
    }
    
    public int getOperandStackNativeOffset(int stackIndex) {
        return getNativeOffset(maxLocals + stackIndex);
    }
    
    public int getFrameSize() {
	return this.javaStack2NativeStack.length;
    }

    /**
     * Return the native frame size in bytes. The definition of the
     * frame size is architecture and calling convention dependent.
     */
    public abstract int getNativeFrameSize();

    public abstract int getReceiverOffset();

    public abstract int getCodeFragmentOffset();

    public abstract int getReturnAddressOffset();
    
    protected abstract void defineNativeStackFrame();
}
