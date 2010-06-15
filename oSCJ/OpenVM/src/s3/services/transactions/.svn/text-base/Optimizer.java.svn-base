package s3.services.transactions;

import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.MethodCodeScanner;

public class Optimizer {

   static public  class SimpleSequenceVisitor extends MethodCodeScanner {
	public boolean clean = true;
	public boolean hasBranch;
	public boolean hasCall;
	public boolean hasAlloc;
	
	public void visit(Instruction.Invocation i) {
	    hasCall = true; clean = false;
	}

	public void visit(Instruction.Allocation i) {
	    hasAlloc = true; clean = false;
	}

	public void visit(Instruction.Switch i) {
	    int[] target = i.getTargets(buf);
	    int dflt = i.getDefaultTarget(buf);
	    boolean back = dflt < 0;
	    for (int idx = 0; idx < target.length && !back; idx++)
		if (target[idx] < 0) {
		    clean = false;
		    hasBranch = true;
		}
	}

	public void visit(Instruction.ConditionalJump i) {
	    if (i.getBranchTarget(buf) < 0) {
		hasBranch = true; clean = false;
	    }
	}

	public void visit(Instruction.UnconditionalJump i) {
	    if (i.getTarget(buf) < 0) {
		hasBranch = true; clean = false;
	    }
	}
    }
}