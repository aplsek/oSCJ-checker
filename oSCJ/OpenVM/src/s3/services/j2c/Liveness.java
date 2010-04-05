package s3.services.j2c;

import ovm.util.BitSet;
import ovm.util.ArrayList;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.Descriptor;
import ovm.core.domain.Blueprint;

public class Liveness extends Dataflow.Backward {

    private static final boolean IGNORE_UNMOVABLE_REFERENCES = true;
    private static final boolean DEBUG_REFERENCES = false;
    
    static class Node {
	BitSet gen;
	BitSet kill;
	BitSet live;

	Node(BitSet g, BitSet k, BitSet l) {
	    gen = g;
	    kill = k;
	    live = l;
	}
    }

    // effects of each block at each safe point and through the beginning.
    Node[][] b;

    public class RefFinder extends ExprVisitor {
	BitSet refs;

	public void visit(J2cValue v) {
	
	    if (DEBUG_REFERENCES) {
  	      System.err.println("refFinder: visiting value "+v+" method "+mc.meth);
  	      System.err.println("  isConcrete()=="+!v.isConcrete()+" name=="+v.name+" flavor=="+v.flavor+
	        " LocalAllocator.REF_VAR=="+LocalAllocator.REF_VAR);
            }
	      
	    if (!v.isConcrete() && v.name != null
		&& v.flavor == LocalAllocator.REF_VAR) {
//		if (mc.debug && v.number == 104)
//		    System.err.println("found ref to _stack_79 as " +
//				       v.name);

                if (IGNORE_UNMOVABLE_REFERENCES) {
                  Blueprint bp = v.getBlueprint(mc.ctx.domain);
                  if ((bp != null) && !bp.isSubtypeOf(mc.ctx.OopBP) && (bp != mc.ctx.OpaqueBP) && 
                    !mc.ctx.anal.isHeapAllocated(bp)) {
                    if (DEBUG_REFERENCES) {
                      System.err.println("refFinder: removing unmovable reference in value "+v+" method "+mc.meth+" of blueprint "+bp);
                    }
                    return ;
                  } 
                }
                
	        if (DEBUG_REFERENCES) {
	          System.err.println("refFinder: adding to references.");                  
                }
		refs.set(v.number);
	    }
	    else {
	      if (DEBUG_REFERENCES) {
	        System.err.println("refFinder: not adding to references.");
              }
              super.visit(v);
            }
	}
    }

    public String toString(BitSet bs) {
	if (bs.nextSetBit(0) == -1)
	    return "";
	StringBuffer b = new StringBuffer();
	String pfx = "";
	for (int i = bs.nextSetBit(0); i != -1; i = bs.nextSetBit(i+1)) {
	    b.append(pfx);
	    pfx = ", ";
	    if (i < mc.allocator.nVars()) {
		J2cValue v = mc.allocator.getVar(i);
		if (v != null) {
		    b.append(v.name);
		} else {
		    b.append("<unknown>");
		}
	    } else {
		b.append("<big>");
	    }
	    b.append(" ");
	    b.append(i);
	}
	return b.toString();
    }

    public Liveness(MethodCompiler mc) {
	super(mc);
	int keep = (mc.meth.getMode().isSynchronized()
		    ? mc.allocator.findLocal(TypeCodes.OBJECT, 0).number
		    : -1);
	if (mc.debug)
	    System.err.println("building liveness graph for " +
			       mc.meth.getSelector() +
			       " with " + mc.allocator.nVars() +
			       " total variables");
	b = new Node[mc.block.length][];
	
	RefFinder genFrom = new RefFinder();
	ArrayList block = new ArrayList();
	final J2cValue[] hVar = new J2cValue[b.length];
	if (mc.tryBlock != null)
	    mc.tryBlock.accept(new TryBlock.Visitor() {
		    public void visit(TryBlock _) { }
		    public void visit(TryBlock _, TryBlock.Handler h) {
			hVar[h.block.number] = h.catchVar;
		    }
		});
	for (int i = b.length; i --> 0; ) {
	    BBSpec.Expr[] code = mc.block[i].code;
	    BitSet gen = genFrom.refs = new BitSet();
	    BitSet kill = new BitSet();

	    if (keep != -1) gen.set(keep);

	    for (int j = code.length; j --> 0; ) {
		if (code[j] == null)
		    continue;
		if (code[j].dest != null
		    && code[j].dest.flavor == LocalAllocator.REF_VAR) {
		    gen.clear(code[j].dest.number);
		    kill.set(code[j].dest.number);
// 		    System.err.println("kill " + code[j].dest.number);
		}
		if (code[j].liveRefOut != null) {
		    block.add(new Node(gen, kill, code[j].liveRefOut));
		    gen = genFrom.refs = new BitSet();
		    kill = new BitSet();
		}
		genFrom.visitAppropriate(code[j]);
		if (mc.debug && gen.get(104))
		    System.err.println(mc.block[i].startPC + "+" + j +
				       " uses _stack_79: " + code[j]);
	    }
	    if (hVar[i] != null) {
		gen.clear(hVar[i].number);
		kill.set(hVar[i].number);
// 		System.err.println("kill catch " + hVar[i].number);
	    }
	    if (gen.nextSetBit(0) != -1
		|| kill.nextSetBit(0) != -1
		|| block.size() == 0) {
		block.add(new Node(gen, kill, new BitSet()));
	    }
// 	    System.err.println("block " + i + " has "
// 			       + block.size() + " nodes");
	    b[i] = new Node[block.size()];
	    for (int j = b[i].length; j --> 0; ) {
		b[i][j] = (Node) block.get(b[i].length - j - 1);
		if (mc.debug) {
		    System.err.println("gen[" + mc.block[i].startPC + "][" +
				       j + "] = " +  toString(b[i][j].gen));
		    System.err.println("kill[" + mc.block[i].startPC + "][" +
				       j + "] = " + toString(b[i][j].kill));
		}
	    }
	    block.clear();
	}
    }

    private static final BitSet emptyHandlers = new BitSet(0);
    public boolean run(BBSpec block) {
	if (mc.debug)
	    System.err.println("run(" + block.number +")");
	BitSet live = new BitSet();
	BitSet liveAtHandlers;

	for (int i = block.next.length; i --> 0; )
	    live.or(b[block.next[i].number][0].live);
// 	System.err.println("incoming cardinality " + live.cardinality());
// 	System.err.println("incoming size " + live.size());
	switch (block.handler.length) {
	case 0:
	    liveAtHandlers = emptyHandlers;
	    break;
	case 1:
	    liveAtHandlers = b[block.handler[0].number][0].live;
	    break;
	default:
	    liveAtHandlers = new BitSet();
	    for (int i = block.handler.length; i --> 0; )
		liveAtHandlers.or(b[block.handler[i].number][0].live);
	}
// 	System.err.println("handler cardinality " +
// 			   liveAtHandlers.cardinality());
// 	System.err.println("handler size " +
// 			   liveAtHandlers.size());
	Node[] n = b[block.number];
	for (int i = n.length; i --> 1; ) {
	    live.andNot(n[i].kill);
	    live.or(n[i].gen);
	    live.or(liveAtHandlers);
	    live.copyInto(n[i].live);
	    if (mc.debug)
		System.err.println("live[" + block.startPC +"][" + i + "] = " +
				   toString(n[i].live));
	}
	live.andNot(n[0].kill);
	live.or(n[0].gen);
	live.or(liveAtHandlers);
	boolean changed = !live.equals(n[0].live);
	live.copyInto(n[0].live);
	if (mc.debug) {
	    System.err.println("live[" + block.startPC +"][0] = " +
			       toString(n[0].live));
	    System.err.println((changed ? "" : "un") + "changed");
	}
	return changed;
    }

    public void run() {
	super.run();
// 	System.err.println("computed liveness for "
// 			   + mc.meth.getSelector());
	mc.falseLiveness = (BitSet) b[0][0].live.clone();

	if (mc.tryBlock != null) {
	    mc.tryBlock.accept(new TryBlock.Visitor() {
		    public void visit(TryBlock t) {
			t.liveAtCatch = new BitSet();
		    }
		    public void visit(TryBlock t, TryBlock.Handler h) {
			t.liveAtCatch.or(b[h.block.number][0].live);
		    }
		});
	    if (mc.ctx.catchPointsUsed)
		mergeLiveness(mc.tryBlock);
	}
	// false liveness should only happen in the presence of
	// exceptions, but paranoia can be a good thing
	Descriptor.Method desc = mc.meth.getSelector().getDescriptor();
	for (int i = desc.getArgumentLength()/4 + 1; i --> 0; ) {
	    J2cValue v = mc.allocator.findLocal('L', i);
	    if (v != null)
		mc.falseLiveness.clear(v.number);
	}
    }
    

    // This code ensures that PACKCALL and CATCHRESTORE will always
    // see the same list of variables.
    private void mergeLiveness(TryBlock t) {
	for ( ; t != null; t = t.next) {
	    if (t.liveAtCatch.cardinality() == 0) {
		mergeLiveness(t.inner);
		continue;
	    }
	    for (int i = t.firstBlock.number; i <= t.lastBlock.number; i++) {
		BBSpec b = mc.block[i];
		for (int j = 0; j < b.code.length; j++) {
		    BBSpec.Expr e = b.code[j];
		    if (e != null && e.liveRefOut != null) {
			t.liveAtCatch.or(e.liveRefOut);
			e.liveRefOut = t.liveAtCatch;
		    }
		}
	    }
	    BitSet falseHere = (BitSet) t.liveAtCatch.clone();
	    falseHere.andNot(b[t.firstBlock.number][0].live);
	    mc.falseLiveness.or(falseHere);
	}
    }

    // Find the set of variables that are live at any safe point
    public BitSet everLive() {
	BitSet all = new BitSet();
	for (int i = 0; i < mc.block.length; i++) {
	    BBSpec.Expr[] code = mc.block[i].code;
	    for (int j = 0; j < code.length; j++)
		if (code[j] != null && code[j].liveRefOut != null)
		    all.or(code[j].liveRefOut);
	}
	return all;
    }
}
