package ovm.services.bytecode;

import ovm.core.repository.TypeCodes;
import ovm.services.bytecode.Instruction.BranchInstruction;
import ovm.services.bytecode.SpecificationIR.ConcreteStreamableValue;
import ovm.util.ByteBuffer;
import ovm.util.ArrayList;
import ovm.util.NumberRanges;
import ovm.util.Iterator;
import ovm.util.OVMError;

/**
 * Node in the instruction list.
 * @author yamauchi
 */
public class InstructionHandle extends SpecificationIR.ConcretePCValue
    implements MethodInformation
{
    private static int instanceCounter = 0;

    private final int instanceNumber;
    private Instruction inst;
    private int position = -1;
    InstructionHandle prev = null;
    InstructionHandle next = null;
    ArrayList targeters;
    
    public InstructionHandle(int pos, Instruction i) {
        super(pos);
        position = pos;
        inst = i;
        relative = false;
	instanceNumber = instanceCounter++;
    }

    // MethodInformation adapters:
    public int getPC() { return position; }
    public ByteBuffer getCode() { throw new OVMError.Unimplemented(); }

    public void accept(Instruction.Visitor v) {
        inst.accept(v);
    }

    public boolean containsTargeter(InstructionTargeter it) {
	if (targeters != null) {
		for(Iterator i = targeters.iterator(); i.hasNext(); ) {
			Object o = i.next();
			if (it == o)
				return true;
		}
	}
        return false;
    }
	public void removeTargeter(InstructionTargeter it) {
		if (targeters != null) {
			for(Iterator i = targeters.iterator(); i.hasNext(); ) {
				Object o = i.next();
				if (it == o) {
					i.remove();
					//return;
				}
			}
		}
	}
    public void addTargeter(InstructionTargeter it) {
        if (targeters == null)
            targeters = new ArrayList();
	targeters.add(it);
/*
	boolean alreadyInThere = false;
	for(Iterator i = targeters.iterator(); i.hasNext(); ) {
		Object o = i.next();
		if (it == o) {
   			alreadyInThere = true;
			break;
		}	
	}
	if (! alreadyInThere)
	        targeters.add(it);
*/
    }
    public InstructionTargeter[] getTargeters() {
        if (targeters == null)
            return null;
        InstructionTargeter[] ret = new InstructionTargeter[targeters.size()];
        targeters.toArray(ret);
        return ret;
    }
    
    public void setInstruction(Instruction i) { inst = i; }
    public Instruction getInstruction() { return inst; }
    void setPosition(int pos) { position = pos; }
    public int getPosition() { return position; }
    public InstructionHandle getPrev() { return prev; }
    public InstructionHandle getNext() { return next; }

    public String toString() {
        return "IH@" + position + "[" + inst + "]#" + instanceNumber;
    }

    // Legacy from ConcretePCValue
    public Object concreteValue() { return new Integer(position); }
    public int intValue() { return position; }
    public char getType() { throw new Error(); }
    public int bytestreamSize() { throw new Error(); }
    public Number decodeStream(MethodInformation iv, int offset) { throw new Error(); }
    public ConcreteStreamableValue concretize(MethodInformation iv, int ofIet) { throw new Error(); }
    public void encode(ByteBuffer code) { throw new Error(); }

    public int bytestreamSize(BranchInstruction ins) {
        if (ins.isBranchOffsetShort())
            return 2;
        else
            return 4;
    }
    
    public void encode(ByteBuffer code, int sourcePC) {
        encode(code, TypeCodes.INT, sourcePC);
    }
    public void encode(ByteBuffer code, char type, int sourcePC) {
        int value = position - sourcePC;
        switch (type) {
        case TypeCodes.UBYTE:
            code.put(NumberRanges.checkUnsignedByte(value));
        break;
        case TypeCodes.BYTE:   
            code.put(NumberRanges.checkByte(value));
        break;
        case TypeCodes.CHAR:
        case TypeCodes.USHORT:
            code.putChar(NumberRanges.checkUnsignedShort(value));
            break;
        case TypeCodes.SHORT:
            code.putShort(NumberRanges.checkShort(value));
            break;
        case TypeCodes.UINT:
        case TypeCodes.INT:
            code.putInt(value);
            break;
        default:
            throw new Error();
        }
    }
}
