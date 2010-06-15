package s3.services.bytecode.analysis;


import ovm.core.repository.Selector;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.ExceptionHandler;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.InstructionVisitor;

/**
 * Build a boolean array that marks the PCs which are jump targets.
 * Generally used to define a new merge strategy that always merges
 * the control flow at any control flow merge points.  Note that
 * the default merge strategy implemented by the FixpointIterator
 * makes no such guarantees whatsoever (it may or may not merge,
 * depending on what the heuristics believe would be faster).
 *
 * @author Jason Baker, Christian Grothoff
 **/
public abstract class BranchTargetResolver 
    extends InstructionVisitor
    implements Runnable {

    /**
     * Note that exception targets are not marked as branch targets!
     */
    public static boolean[] findTargets(RepositoryMember.Method me) {
	BI bi = new BI(me);
        bi.run();
        return bi.findTargets();
    }

    /**
     * Note that exception targets are not marked as branch targets!
     */
    public static boolean[] findTargets(InstructionBuffer buf) {
	BI bi = new BI(buf);
        bi.run();
        return bi.findTargets();
    }
  
    /**
     * INCLUDES exception targets are possibly back jumps.
     */
    public static boolean[] findBackJumps(RepositoryMember.Method me) {
	BB bb = new BB(me);
        bb.run();
        boolean [] ret 
	    = bb.findTargets();
	ExceptionHandler[] h = me.getCodeFragment().getExceptionHandlers();
	for (int i=h.length-1;i>=0;i--) {
	    if (h[i].getHandlerPC() < 
		h[i].getEndPC())
		ret[h[i].getHandlerPC()] = true;
	}
	return ret;
    }

    /**
     * Counts the number of times any given PC is
     * the target of a jump (including fall-though
     * from branches, excluding fall-through from normal
     * code!).
     */
    public static int[] countTargets(RepositoryMember.Method me) {
	BT bt = new BT(me);
        bt.run();
        return bt.countTargets();
    }

    public static int[] countMerges(RepositoryMember.Method me) {
	BM bt = new BM(me);
        bt.run();
        return bt.countTargets();
    }

    abstract void mark(int pc);
    
    public void visit(Instruction.Switch insn) {
	int[] st = insn.getTargets(buf);
	for (int i = 0; i < st.length; i++)
	    mark(getPC() + st[i]);
	mark(getPC() + insn.getDefaultTarget(buf));
    }
    
    public void visit(Instruction.UnconditionalJump i) {
	mark(getPC() + i.getTarget(buf));
    }
    
    public void visit(Instruction.ConditionalJump i) {
	mark(getPC() + i.getBranchTarget(buf));
	mark(getPC() + i.size(buf));
    }

    BranchTargetResolver(InstructionBuffer buf) {
	super(buf);
    }
    
    public void run() {
	while (buf.hasRemaining())
	    visitAppropriate(buf.get());
    }

    public static class BB extends BranchTargetResolver {
	private boolean[] target;
	
	BB(InstructionBuffer buf) {
	    super(buf);
	    target = new boolean[buf.getCode().limit()];
        } 
	
	BB(RepositoryMember.Method me) {
	    this(InstructionBuffer.wrap(me));
	}       
     
	void mark(int pc) {
	    if (getPC() > pc)
		target[pc] = true;
	}

	public boolean[] findTargets() {
	    return target;
	}

    }

    public static class BI extends BranchTargetResolver {
	private boolean[] target;
	
	BI(InstructionBuffer buf) {
	    super(buf);
	    target = new boolean[buf.getCode().limit()];
        } 
	
	BI(RepositoryMember.Method me) {
	    this(InstructionBuffer.wrap(me));
	}       
     
	void mark(int pc) {
	    target[pc] = true;
	}

	public boolean[] findTargets() {
	    return target;
	}

    }

    public static class BM extends BranchTargetResolver {
	private int[] target;
	
	BM(InstructionBuffer buf) {
	    super(buf);
	    target = new int[buf.getCode().limit()];
        } 
	
	BM(RepositoryMember.Method me) {
	    this(InstructionBuffer.wrap(me));
	}       
     
	void mark(int pc) {
	    target[pc]++;
	}

	public int[] countTargets() {
	    return target;
	}

	public void visit(Instruction.FlowEnd i) {
	}

	public void visit(Instruction i) {
	    mark(getPC() + i.size(buf));
	}

    }

    public static class BT extends BranchTargetResolver {
    	private int[] target;
        private boolean[] isSSA;
        private boolean[] isMA;	
	private boolean[] isJSR;

	public BT(RepositoryMember.Method me) {
	    super(InstructionBuffer.wrap(me));
	    target = new int[buf.getCode().limit()];
	    isJSR = new boolean[buf.getCode().limit()];
	    isSSA = new boolean[me.getCodeFragment().getMaxLocals()];
            isMA = new boolean[me.getCodeFragment().getMaxLocals()];
            Selector.Method sel = me.getSelector();
            int cnt = sel.getDescriptor().getArgumentLength() / 4;
            if (!me.getMode().isStatic())            
        	cnt++;
	    for (int i=0;i<cnt;i++)
              isSSA[i] = true;
        } 
	
        void wmark(int local) {
	   if (isSSA[local])
             isMA[local] = true;
           else
              isSSA[local] = true;
        }

	void mark(int pc) {
	    target[pc]++;
	}
	
        public boolean[] getSSALocals() {
            for (int i=isSSA.length-1;i>=0;i--) {
              if (isMA[i]) {
                isSSA[i] = false;
              } else {
                isSSA[i] = true;
              }
            }
            return isSSA;
        }

	public void visit(Instruction.JSR i) {
	    super.visit(i);
	    isJSR[getPC()+i.getTarget(buf)] = true;
	}

        public void visit(Instruction.IINC i) {
           wmark(i.getLocalVariableOffset(buf));
        }

        public void visit(Instruction.LocalWrite i) {
           wmark(i.getLocalVariableOffset(buf));
        }

	public int[] countTargets() {
	    return target;
	}

	public boolean[] getJSRTargets() {
	    return isJSR;
	}

    }

}
