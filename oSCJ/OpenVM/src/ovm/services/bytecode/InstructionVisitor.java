/**
 * This file defines the base class for operations on
 * bytecode.
 *
 * @file ovm/services/bytecode/InstructionVisitor.java
 **/
package ovm.services.bytecode;

import ovm.core.repository.Selector;
import ovm.core.repository.Constants;
import ovm.util.ByteBuffer;
import org.ovmj.util.Runabout;
import ovm.util.Location;

/**
 * The InstructionVisitor is the base-class for  processing bytecode. 
 * 
 * The documentation ought to be revisited:
 * Subclass InstructionVisitor to implement an analysis 
 * involving the Instructions. Implement a Strategy to
 * specify in which order the visit methods should be 
 * executed. Use the run method to start the traversal.
 *
 * @see ovm.util.Runabout
 * @see ovm.services.bytecode.Instruction
 * @author Christian Grothoff
 **/
public class InstructionVisitor 
    extends Runabout {

    // FIXME: aren't protected non-final fields frowned upon?
    protected InstructionBuffer buf;
    protected Constants cp;

    /**
     * FIXME: What the hell is disable supposed to do?  It isn't used!
     **/
    protected InstructionVisitor(boolean disable,
				 InstructionBuffer buf) {
	//super(disable);
	super();
        this.buf = buf;
	cp = buf.getConstantPool();
    }
    
    public InstructionVisitor(InstructionBuffer buf) {
        this.buf = buf;
	if (buf != null)
	    cp = buf.getConstantPool();
    }

    public MethodInformation getInstructionBuffer() {
	return buf;
    }
    
    /**
     * Get the code of the method that we are analyzing.
     **/
    public ByteBuffer getCode() {
        return buf.getCode();
    }

    /**
     * Get the constant pool of the method that we are analyzing.
     **/
    public Constants getConstantPool() {
        return cp;
    }

    public Location.Bytecode getLocation(String desc) {
	return new Location.Bytecode(getSelector(),
				     getPC(),
				     desc);
    }
    public Location.Bytecode getLocation(Instruction i) {
	return new Location.Bytecode(getSelector(),
				     getPC(),
				     i.toString(buf));
    }
    public Location.Bytecode getLocation() {
	return new Location.Bytecode(getSelector(),
				     getPC(),
				     "");
    }

    /**
     * Get the selector of the method that we are analyzing.
     * @return null if no selector is available
     **/
    public Selector.Method getSelector() {
        return buf.getSelector();
    }

    /**
     * Get the current PC into the method that we are analyzing.
     **/
    public int getPC() {
        return buf.getPC();
    }

    protected void visitDefault(Object o) {
    }

    public void visit(Instruction.WIDE i) {
	visitAppropriate(i.specialize(buf));
    }

} // End of InstructionVisitor
