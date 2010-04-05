/*
 * Created on Mar 9, 2005
 *
 */
package ovm.services.bytecode;

import ovm.services.bytecode.Instruction.*;
import ovm.util.ByteBuffer;
import ovm.util.HTint2Object;
import ovm.util.Vector;

/**
 * Linked list of Instructions. 
 * @author yamauchi
 */
public class InstructionList {
    
    private InstructionHandle start = null;
    private InstructionHandle end = null;
    private int[] positions = null;
    
    public InstructionList(byte[] bytes) {
        this(InstructionBuffer.wrap(ByteBuffer.wrap(bytes), null, null));
    }
    
    public InstructionList(InstructionBuffer codeBuf) {
        HTint2Object pc2ih = new HTint2Object();
        // Populate the list
        codeBuf.rewind();
	int count = 0;
        while (codeBuf.hasRemaining()) {
	    count++;
            int pos = codeBuf.position();
            Instruction absIns = codeBuf.get();
            Instruction concIns = absIns.concretize(codeBuf);
            InstructionHandle ih = new InstructionHandle(pos, concIns);
            pc2ih.put(pos, ih);
            append(ih);
        }
	positions = new int[count];
	int k = 0;
        // Link branch targets
        for(InstructionHandle ih = start; ih != null; ih = ih.next) {
            int pos = ih.getPosition();
	    positions[k] = pos;
	    k++;
            Instruction ins = ih.getInstruction();
            if (ins instanceof BranchInstruction) {
                // Replace ConcretePCValues in the branch instruction with InstructionHandles
                BranchInstruction bins = (BranchInstruction)ins;
                SpecificationIR.ConcretePCValue[] pcValues = bins.getAllTargets();
                for (int i = 0; i < pcValues.length; i++) {
                    InstructionHandle tih = 
                        (InstructionHandle)pc2ih.get(pos + pcValues[i].intValue());
                    if (tih == null) {
                        throw new Error();
                    }
                    tih.addTargeter(bins);
                    pcValues[i] = tih;
                }
                bins.setAllTargets(pcValues);
            }
        }
        
    }   

    public InstructionList copy() {
        byte[] bytes = getByteCode();
        return new InstructionList(bytes);
    }
    
    public InstructionHandle getStart() { return start; }
    public InstructionHandle getEnd() { return end; }

    public int getLength() {
	if (isEmpty())
	    return 0;
	int length = 1;
	InstructionHandle ih = start;
	while (ih != end) {
	    length++;
	    ih = ih.getNext();
	}
	return length;
    }

    public boolean isEmpty() {
        return start == null && end == null;   
    }
    
    public boolean contains(InstructionHandle ih) {
        for(InstructionHandle h = start; h != null; h = h.next) {
            if (h == ih)
                return true;
        }
        return false;
    }

    public InstructionHandle[] getInstructionHandles() {
        Vector v = new Vector();
        for(InstructionHandle ih = start; ih != null; ih = ih.next) {
            v.add(ih);
        }
        InstructionHandle[] ihs = new InstructionHandle[v.size()];
        v.toArray(ihs);
        return ihs;
    }
    
    public void delete(InstructionHandle from, InstructionHandle to) throws TargetLostException {
        Vector v = new Vector();
        for(InstructionHandle ih = from; ih != to.next; ) {
            InstructionHandle next = ih.next;
            try {
                delete(ih);
            } catch (TargetLostException e) {
                v.add(ih);
            }
            ih = next;
        }
        if (v.size() > 0) {
            InstructionHandle[] targets = new InstructionHandle[v.size()];
            v.toArray(targets);
            throw new TargetLostException(targets);
        }
    }
    
    /**
     * Delete the InstructionHandle from the list.
     * A TargetLostException will be raised if the deleted IH was pointed to by some InstructionTargeters. 
     */
    public void delete(InstructionHandle ih) throws TargetLostException {
        if (ih == null || ! contains(ih)) 
            throw new IllegalArgumentException();
        InstructionHandle prev = ih.prev;
        InstructionHandle next = ih.next;
        if (start == ih) { 
            start = ih.next;
            if (start != null)
                start.prev = null; 
        }  
        if (end == ih) { 
            end = ih.prev;
            if (end != null)
                end.next = null; 
        } 
        if (prev != null)
            prev.next = next;
        if (next != null)
            next.prev = prev;

	Instruction ins = ih.getInstruction();
	if (ins instanceof BranchInstruction) {
		BranchInstruction b = (BranchInstruction)ins;
		InstructionHandle t = b.getTargetHandle();
		if (t != null)
			t.removeTargeter(b);
		if (b instanceof Switch) {
			InstructionHandle[] targets = ((Switch)b).getTargetHandles();
			for(int i = 0; i < targets.length; i++)
				if (targets[i] != null)
					targets[i].removeTargeter(b);
		}		
	}

        InstructionTargeter[] targeters = ih.getTargeters();
        if (targeters != null)
            throw new TargetLostException(new InstructionHandle[] {ih});
    }
    
    /**
     * Insert an InstructionList. The InstructionHandles in it are consumed.
     * @param position
     * @param ilist
     * @return the first one in the inserted IHs.
     */
    public InstructionHandle insert(InstructionHandle position, InstructionList ilist) {
        InstructionHandle ret = ilist.getStart();
        for(InstructionHandle ih = ilist.getStart(); ih != null; ) {
            InstructionHandle next = ih.next;
            insert(position, ih);
            ih = next;
        }
	ilist.start = null;
	ilist.end = null;
        return ret;
    }
        
    public InstructionHandle insert(InstructionHandle position, InstructionHandle inserted) {
        if (position == null || inserted == null || 
                ! contains(position) || contains(inserted)) 
            throw new IllegalArgumentException((position == null ? "1" : "")
                + (inserted == null ? "2" : "")
                + (!contains(position) ? "3" : "")
                + (contains(inserted) ? "4" : ""));
        InstructionHandle prev = position.prev;
        if (prev == null) { // when ih == start
            start = inserted;
            inserted.prev = null;
            inserted.next = position;
            position.prev = inserted;
        } else {
            prev.next = inserted;
            inserted.prev = prev;
            inserted.next = position;
            position.prev = inserted;
        }
        return inserted;
    }
    
    /**
     * Insert an Instruction before the IH
     * @param ih
     * @param ins
     * @return the IH for the inserted Instruction
     */
    public InstructionHandle insert(InstructionHandle ih, Instruction ins) {
        if (ins == null || ih == null || ! contains(ih)) 
            throw new IllegalArgumentException();
        InstructionHandle inserted = new InstructionHandle(-1, ins);
        InstructionHandle prev = ih.prev;
        if (prev == null) { // when ih == start
            start = inserted;
            inserted.prev = null;
            inserted.next = ih;
            ih.prev = inserted;
        } else {
            prev.next = inserted;
            inserted.prev = prev;
            inserted.next = ih;
            ih.prev = inserted;
        }
        return inserted;
    }
    
    public InstructionHandle append(Instruction ins) {
        InstructionHandle ih = new InstructionHandle(-1, ins);
        return append(ih);
    }
    
    /**
     * Append an InstructionHandle at the end of the list
     * @param ih
     * @return the appended IH
     */
    public InstructionHandle append(InstructionHandle ih) {
        if (ih == null) throw new IllegalArgumentException();
        if (isEmpty()) {
            start = ih;
            end = ih;
        } else {
            end.next = ih;
            ih.prev = end;
            ih.next = null;
            end = ih;
        }
        return ih;
    }

    /**
     * Return an array of instruction positions in the ascending
     * order. Note that if it is before setPositions() is called, this
     * array reflects the old (before insertion/deletion) instruction
     * list.
     */
    public int[] getInstructionPositions() {
	return positions;
    }
    
    public int size() {
        int num = 0;
        for(InstructionHandle ih = start; ih != null; ih = ih.next)
            num++;
        return num;
    }
    
    // Update the positions of the InstructionHandles. Widen instructions as needed.
    public void setPositions() {
	int count = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            int position = 0;
	    count = 0;
            for (InstructionHandle ih = start; ih != null; ih = ih.next) {
		count++;
                ih.setPosition(position);
                position += ih.getInstruction().size(position);
            }
            for (InstructionHandle ih = start; ih != null; ih = ih.next) {
                Instruction ins = ih.getInstruction();
                if (ins.doesNeedWidening()) {
                    //System.err.println(ih + " needed widening");
                    changed = true;
                    Instruction widened = ins.widen();
                    ih.setInstruction(widened);
                }
            }
        }
	positions = new int[count];
	int i = 0;
	for (InstructionHandle ih = start; ih != null; ih = ih.next) {
	    positions[i] = ih.getPosition();
	    i++;
	}
    }
    
    public void encode(ByteBuffer code) {
        setPositions();
        for(InstructionHandle ih = start; ih != null; ih = ih.next) {
            Instruction ins = ih.getInstruction();
            ins.encode(code);
        }
    }
    
    public byte[] getByteCode() {
        setPositions();
        ByteBuffer buf = ByteBuffer.allocate(Integer.MAX_VALUE);
        buf.rewind();
        encode(buf);
        int end = buf.position();
        byte[] bytes = new byte[end];
        buf.rewind();
        buf.get(bytes, 0, end);
        return bytes;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for(InstructionHandle ih = start; ih != null; ih = ih.next) {
            buf.append(ih.toString());
            buf.append("\n");
        }
        return buf.toString();
    }
}
