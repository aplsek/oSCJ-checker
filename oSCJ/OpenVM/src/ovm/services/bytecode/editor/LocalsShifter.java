package ovm.services.bytecode.editor;

import ovm.services.bytecode.Instruction;
import ovm.util.NumberRanges;

/**
 * Reserve a number of local slots in the locals array by shifting all
 * references to locals.
 * 
 * @author K. Palacz
 * @author Christian Grothoff
 **/
public class LocalsShifter 
    extends InstructionEditVisitor {
    
    protected int delta;
    protected int fromIndex;

    /**
     * @param delta how many slots to reserve
     * @param fromIndex where to put reserved slots
     **/
    public LocalsShifter(int delta, int fromIndex) {
	this.delta = delta;
	this.fromIndex = fromIndex;
    }

    private char getNewIndex(int idx) {
	if (idx < fromIndex)
	    return NumberRanges.checkChar(idx);
	else
	    return NumberRanges.checkChar(idx + delta);
    }
    public void visit(Object o) {} 
    public void visit(Instruction.ASTORE_0 i) {
	cfe.replaceInstruction()
	    .addAStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ASTORE_1 i) { 
	cfe.replaceInstruction()
	    .addAStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ASTORE_2 i) { 
	cfe.replaceInstruction()
	    .addAStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ASTORE_3 i) {
	cfe.replaceInstruction()
	    .addAStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ASTORE i) { 
	cfe.replaceInstruction()
	    .addAStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ISTORE_0 i) {
	cfe.replaceInstruction()
	    .addIStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ISTORE_1 i) {
	cfe.replaceInstruction()
	    .addIStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ISTORE_2 i) { 
	cfe.replaceInstruction()
	    .addIStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ISTORE_3 i) {
	cfe.replaceInstruction()
	    .addIStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ISTORE i) {
	cfe.replaceInstruction()
	    .addIStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FSTORE_0 i) {
	cfe.replaceInstruction()
	    .addFStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FSTORE_1 i) {
	cfe.replaceInstruction()
	    .addFStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FSTORE_2 i) {
	cfe.replaceInstruction()
	    .addFStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FSTORE_3 i) {
	cfe.replaceInstruction()
	    .addFStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FSTORE i) { 
	cfe.replaceInstruction()
	    .addFStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DSTORE_0 i) {
	cfe.replaceInstruction()
	    .addDStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DSTORE_1 i) {
	cfe.replaceInstruction()
	    .addDStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DSTORE_2 i) {
	cfe.replaceInstruction()
	    .addDStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DSTORE_3 i) { 
	cfe.replaceInstruction()
	    .addDStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DSTORE i) { 
	cfe.replaceInstruction()
	    .addDStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.LSTORE_0 i) {
	cfe.replaceInstruction()
	    .addLStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.LSTORE_1 i) {
	cfe.replaceInstruction()
	    .addLStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.LSTORE_2 i) {
	cfe.replaceInstruction()
	    .addLStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.LSTORE_3 i) {
	cfe.replaceInstruction()
	    .addLStore(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.LSTORE i) {
	cfe.replaceInstruction()
	    .addLStore(getNewIndex(i.getLocalVariableOffset(buf)));
    } 
    public void visit(Instruction.ALOAD_0 i) {
	cfe.replaceInstruction()
	    .addALoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ALOAD_1 i) {
	cfe.replaceInstruction()
	    .addALoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ALOAD_2 i) {
	cfe.replaceInstruction()
	    .addALoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ALOAD_3 i) { 	
	cfe.replaceInstruction()
	    .addALoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ALOAD i) { 
	cfe.replaceInstruction()
	    .addALoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ILOAD_0 i) { 
	cfe.replaceInstruction()
	    .addILoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ILOAD_1 i) { 
	cfe.replaceInstruction()
	    .addILoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ILOAD_2 i) {
	cfe.replaceInstruction()
	    .addILoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ILOAD_3 i) { 
	cfe.replaceInstruction()
	    .addILoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.ILOAD i) {
	cfe.replaceInstruction()
	    .addILoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FLOAD_0 i) { 
	cfe.replaceInstruction()
	    .addFLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FLOAD_1 i) { 
	cfe.replaceInstruction()
	    .addFLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FLOAD_2 i) { 
	cfe.replaceInstruction()
	    .addFLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FLOAD_3 i) {
	cfe.replaceInstruction()
	    .addFLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.FLOAD i) {
	cfe.replaceInstruction()
	    .addFLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DLOAD_0 i) {
	cfe.replaceInstruction()
	    .addDLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DLOAD_1 i) {
	cfe.replaceInstruction()
	    .addDLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DLOAD_2 i) {
	cfe.replaceInstruction()
	    .addDLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DLOAD_3 i) { 
	cfe.replaceInstruction()
	    .addDLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.DLOAD i) { 
	cfe.replaceInstruction()
	    .addDLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.LLOAD_0 i) { 
	cfe.replaceInstruction()
	    .addLLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }    
    public void visit(Instruction.LLOAD_1 i) {
	cfe.replaceInstruction()
	    .addLLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.LLOAD_2 i) {
	cfe.replaceInstruction()
	    .addLLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.LLOAD_3 i) {
	cfe.replaceInstruction()
	    .addLLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.LLOAD i) {
 	cfe.replaceInstruction()
	    .addLLoad(getNewIndex(i.getLocalVariableOffset(buf)));
    } 
    public void visit(Instruction.RET i) {
	cfe.replaceInstruction()
	    .addRet(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.IINC i) { 
	cfe.replaceInstruction()
	    .addIInc(getNewIndex(i.getLocalVariableOffset(buf)),
		     (short)i.getValue(buf));
    }
    public void visit(Instruction.WIDE_RET i) {
	cfe.replaceInstruction()
	    .addRet(getNewIndex(i.getLocalVariableOffset(buf)));
    }
    public void visit(Instruction.WIDE_IINC i) { 
	cfe.replaceInstruction()
	    .addIInc(getNewIndex(i.getLocalVariableOffset(buf)),
		     (short)i.getValue(buf));
    }
 
} // end of LocalsShifter
