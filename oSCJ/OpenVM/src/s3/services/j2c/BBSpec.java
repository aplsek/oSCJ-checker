package s3.services.j2c;

import org.ovmj.util.Runabout;

import ovm.core.OVMBase;
import ovm.core.domain.Domain;
import ovm.core.repository.JavaNames;
import ovm.core.domain.Blueprint;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.JavaNames;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.UnboundSelector;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.InstructionVisitor;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.SpecificationIR.AssignmentExp;
import ovm.services.bytecode.SpecificationIR.BinExp;
import ovm.services.bytecode.SpecificationIR.ConversionExp;
import ovm.services.bytecode.SpecificationIR.IfCmd;
import ovm.services.bytecode.SpecificationIR.LocalStore;
import ovm.services.bytecode.SpecificationIR.MemExp;
import ovm.services.bytecode.SpecificationIR.Value;
import ovm.services.bytecode.SpecificationIR.ValueSource;
import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.Frame;
import ovm.services.bytecode.analysis.State;
import ovm.services.bytecode.analysis.AbstractValue.JumpTarget;
import ovm.util.ArrayList;
import ovm.util.BitSet;
import ovm.util.ListIterator;
import s3.core.domain.S3Blueprint;
import s3.services.bootimage.Ephemeral;
import s3.services.bytecode.analysis.S3Frame;
import s3.services.j2c.SpecInstantiation.LineNumberParser;
import s3.services.j2c.J2cValue.*;
import s3.services.memory.precise.PreciseReferenceIterator;
import org.ovmj.util.Runabout;
import ovm.core.domain.Blueprint;
import s3.services.transactions.Transaction;

/**
 * The specification for a basic block.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason </a>
 **/
public class BBSpec implements Ephemeral.Void, JVMConstants
{
    static final boolean OPTIMIZE_GEN_THROWABLE = true;
    
    final int startPC;
    final int number;
    final TryBlock innerMostTry;

    Expr[] code;

    BBSpec[] next;
    BBSpec[] handler;
    boolean fallsThrough;

    int[] pcDelta;		// (code offset x bytecode PC)

    static final BBSpec[] NO_NEXT_BLOCK = new BBSpec[0];

    public static class Expr {
	J2cValue dest;

	// expressions that contain safe points have a non-null
	// liveRefOut.  This is the set of variables live across the
	// expression.
	BitSet liveRefOut;

	Expr(J2cValue dest) { this.dest = dest; }
	public String toString() {
	    String dstr =  dest == null ? "" : dest + " = ";
	    String cname = getClass().getName();
	    cname = cname.substring(cname.lastIndexOf('$') + 1,
				    cname.length());
	    return cname + ": " + dstr;
	}
    }
    public static class IRExpr extends Expr {
	ValueSource source;
	IRExpr(J2cValue dest, ValueSource source) {
	    super(dest);
	    this.source = source;
	    // assert(source != null);
	}
	public String toString() { return super.toString() + source; }
    }
    public static class BCExpr extends Expr {
	Instruction source;
	J2cValue[] inputs;
	BCExpr(J2cValue dest, Instruction source, J2cValue[] inputs) {
	    super(dest);
	    this.source = source;
	    this.inputs = inputs;
	}
	public String toString() { return super.toString() + source; }
    }
    public static class FlowChangeExpr extends BCExpr {
	FlowChangeExpr(J2cValue dest, Instruction source, J2cValue[] inputs) {
	    super(dest, source, inputs);
	}
    }
    
    public static class BRExpr extends FlowChangeExpr {
	// J2cValue test;
	BRExpr(J2cValue dest, Instruction source, J2cValue[] inputs) {
	    super(dest, source, inputs);
	}
    }
    public static class SWExpr extends FlowChangeExpr {
	int[] keys;
	SWExpr(J2cValue dest, Instruction source, J2cValue[] inputs, 
	       int[] keys, int delta) {
	    super(dest, source, inputs);
	    this.keys = keys;
	}
    }
    
    public BBSpec(MethodCompiler mc, int pc, int number) {
	this.startPC = pc;
	this.number  = number;
	innerMostTry = (mc.tryBlock == null
			? null
			: mc.tryBlock.findInnerMost(pc));
    }
    public boolean branchesBack() {
	for (int i = next.length; i --> 0; )
	    if (next[i].number <= number)
		return true;
	for (int i = handler.length; i --> 0; )
	    if (handler[i].number <= number)
		return true;
	return false;
    }
    public String toString() {
	StringBuffer b =
	    new StringBuffer("block " + number + "@" + startPC + " -> ");
	if (next == null)
	    b.append("RET");
	else {
	    String pfx = "";
	    for (int i = 0; i < next.length; i++) {
		b.append(pfx);
		b.append(next[i].number);
		pfx =  ", ";
	    }
	}
	if (handler.length != 0) {
	    String pfx = " throws -> ";
	    for (int i = 0; i < handler.length; i++) {
		b.append(pfx);
		b.append(handler[i].number);
		pfx = ", ";
	    }
	}
	// Should I print livness here too?
	return b.toString();
    }

    /**
     * This code is responsible for translating a bytecode method and
     * the abstract interpreter state at the start of each basic block
     * to a standard CFG representation.  In this step, names are
     * assigned to every JVM level variable, and every distinct value
     * pushed onto the JVM stack.  GC safe points are also identified.<p>
     *
     * The allocation of local variables is tricky because two
     * distinct values pushed onto the stack (that are stored in two
     * distinct variables) may flow into the same basic block at a
     * join point.  The fact that instructions like SWAP do not
     * generate new values further complicates the problem.<p>
     *
     * This problem is handled in four steps.
     * <ol>
     *    <li> For each block that takes stack operands, define a
     *         <i>phi</i> variable for each incoming argument.
     *    <li> For each block that produces stack operands, choose the
     *         highest-numbered successor block, and link the outgoing
     *         stack contents to that block's phi variables.  Outgoing
     *         variables are renamed the corresponding phi variables,
     *         and assignments are generated from outgoing constants
     *         to corresponding phi variables.
     *    <li> For each additional successor, assignments are
     *         generated from the <i>canonical</i> block's phi
     *         variables to the corresponding phi variables of this
     *         successor.
     *    <li> copy propoagation is used to eliminate redundant
     *         assignments.
     * </ol>
     *
     * This may seem complicated, but it will avoid unneeded copies
     * for Java code (or any code generated by a sane compiler for a
     * structured langauge) while handling arbitrary bytecode
     * correctly.
     **/
    public static class Builder extends InstructionVisitor {
	// NOTE: not thread-safe.  We use these static variables to build
	// up each BBSpec
	final ArrayList codeBuffer = new ArrayList();

	final SpecInstantiation translator;
	final Context ctx;
	final MethodCompiler mc;
	final LocalAllocator allocator;
	final State st;
	final J2cValue[][] stackIns;

	final ArrayList handler = new ArrayList();
	final TryBlock.HandlerVisitor saveHandlerBlocks =
	    new TryBlock.HandlerVisitor() {
		public void visit(TryBlock.Handler h) {
		    handler.add(h.block);
		}
	    };

	Frame frame;
	BBSpec block;
	boolean canThrow;

	boolean hasSafePoint;

	boolean done = false;

	boolean isXDCall(ValueSource _exp) {
	    if (ctx.domain != ctx.executiveDomain
		&& _exp instanceof InvocationExp)
		{
		    InvocationExp exp = (InvocationExp) _exp;
		    if (exp.target instanceof MethodReference) {
			MethodReference mr = (MethodReference) exp.target;
			Domain d = mr.method.getDeclaringType().getDomain();
			return d.isExecutive();
		    }
		} 
	    return false;
	}

	TryBlock uncheckedHandler() {
	    for (TryBlock t = block.innerMostTry; t != null; t = t.outer)
		if (t.hasUncheckedCatch)
		    return t;
	    return null;
	}
	
	// This method is called when we encounter a gc safe point.  
	void noteSafePoint() {
	    ctx.hasSafePoints(mc.meth);
	    hasSafePoint = true;
	}

	J2cValue findVar(int idx, int pc) {
	    J2cValue content = (J2cValue) translator.getFrame().load(idx);
	    String name = translator.getVariableName(idx, pc);
	    return allocator.allocateLocal(content, idx, name); 
	}

	J2cValue mungeValue(Value _v) {
	    J2cValue v = (J2cValue) _v;
	    return v.copy(mungeExpr(v.source));
	}
	// This method walks over a SpecificationIR tree for two purposes:
	// 1. finding GC safe points
	// 2. finding and transforming cross-domain calls.
	// 3. transforming local reads and writes.  As a side effect,
	//    these transformations resolve names from the LocalVariableTable
	//
	// This code is nasty and horrible and deeply wrong.  We don't
	// traverse an entire AST, but zero in on specific constructs that
	// we know may contain safe points.
	ValueSource mungeExpr(ValueSource src) {
	    if (src == null) {
		return null;
	    } else if (src instanceof InvocationExp) {
		InvocationExp call = (InvocationExp) src;
		if (call.isSafePoint) {
		    if (isXDCall(src)) {
			TryBlock t = uncheckedHandler();
			if (t == null)
			    mc.hasXDCalls = true;
			else
			    t.hasXDCalls = true;
		    }
		    if (OPTIMIZE_GEN_THROWABLE
			&& (call.target instanceof MethodReference)
			&& (((MethodReference) call.target).method
			    == ctx.generateThrowable)
			&& uncheckedHandler() == null)
			call.isSafePoint = false;
		    else
			noteSafePoint();
		} else if (call.target == J2cValue.to_jvalueTarget) {
		    // Search inside to_jvalue(...).jv_varient
		    // for a j2cInvoke call.  This is so fucking silly
		    call.args[0] = mungeValue(call.args[0]);
		    return call;
		}
	    }
	    else if (src instanceof J2cLocalExp) {
		J2cValue var = findVar(((J2cLocalExp) src).num,
				       translator.getPC());
		return new ValueAccessExp(var);
	    }
	    else if (src instanceof JValueExp) {
		JValueExp jv = (JValueExp) src;
		jv.source = mungeValue(jv.source);
		return jv;
	    }
	    else if (src instanceof LocalStore) {
		if (true)
		    throw new Error("LocalStore translated late!");
		LocalStore st = (LocalStore) src;
		// local variable ranges start AFTER the initial store
		// instruction
		int idx = st.index.intValue();
		J2cValue var = findVar(idx,
				       translator.getPC()
				       + (idx > 65535 ? 4
					  : idx > 3 ? 2
					  : 1));
		J2cValue v = ((J2cValue) st.value);
		if (v.kind == J2cValue.NOVAR)
		    // FIXME: it must be an IINC.  see below for quick hack
		    v = mungeValue(v);
		return new AssignmentExp(var, v);
	    }
	    else if (src instanceof BinExp) {
		BinExp op = (BinExp) src;
		J2cValue lhs =  (J2cValue) op.lhs;
		if (lhs.kind == J2cValue.NOVAR) {
		    return new BinExp(mungeValue(lhs), op.operator, op.rhs);
		}
	    }
	    // 
	    else if (src instanceof MemExp) {
		MemExp hdrAccess = (MemExp) src;
		// Special case for getReference().
		// FIXME: goes away when VM_Address <=> Oop conversions fixed?
		if (hdrAccess.addr.source instanceof ValueAccessExp) {
		    // For whatever reason, we have inferred that a stack
		    // slot is a VM_Address, but is is actually an Oop.
		    // We wrap the VM_Address value in a ValueAccessExp to
		    // give it the appropriate type.
		    //
		    // FIXME: are there multiple ways to encode a cast?
		    return new MemExp(mungeValue(hdrAccess.addr),
				      hdrAccess.offset);
		}
	    }
	    else if (src instanceof IfCmd) {
		// pick out CSA calls such as generateThrowable,
		// resolveUTF, and initializeBlueprint
		IfCmd test = (IfCmd) src;
		if (test.ifFalse == null)
		    return new IfCmd(test.cond, mungeValue(test.ifTrue));
	    }
	    else if (src instanceof SeqExp) {
		// pick out initializeBlueprint from a conditional block 
		SeqExp seq = (SeqExp) src;
		if (seq.v.length == 2) {
		    return new SeqExp(mungeValue(seq.v[0]), seq.v[1]);
		}
	    }
	    else if (src instanceof AssignmentExp) {
		AssignmentExp asgn = (AssignmentExp) src;
		// FIXME: Pick out string resolution calls inside an IfCmd (why?)
		if (asgn.src.source != null
		    // make sure not to copy values from _phi and _stack
		    // variables
		    && ((J2cValue) asgn.src).kind == J2cValue.NOVAR)
		    return new AssignmentExp(asgn.dest,
					     mungeValue(asgn.src));
	    }
	    return src;
	}

	void addExprWithSafePoints(Expr e) {
	    codeBuffer.add(e);
	    if (hasSafePoint)
		e.liveRefOut = new BitSet();
	}

	void addFixup(Expr e) {
	    if (block.fallsThrough && block.next.length == 1)
		// ends in normal Expr
		codeBuffer.add(e);
	    else
		// ends in switch, branch, or goto, insert the fixup
		// before the flowChange
		codeBuffer.add(codeBuffer.size() - 1, e);
	}

	void addExpr(Expr e) {
	    hasSafePoint = false;
	    if (allocator != null && e instanceof IRExpr) {
		IRExpr ie = (IRExpr) e;
		ie.source = mungeExpr(ie.source);
	    }
	    mc.log("adding ", e, " to ", I(block.number));
	    addExprWithSafePoints(e);
	}

	public Builder(MethodCompiler mc, SpecInstantiation translator,
		       State st) {
	    super(InstructionBuffer.wrap(mc.code));
	    this.mc = mc;
	    this.ctx = mc.ctx;
	    this.translator = translator;
	    this.st = st;
	    allocator = mc.allocator;
	    stackIns = new J2cValue[mc.block.length][];
	    for (int idx = 0; idx < mc.block.length; idx++) {
		int startPC = mc.block[idx].startPC;
		Frame f = st.getFrameAt(startPC);
		int h = f.getStackHeight();
		if (h > 0) {
		    mc.log("ALLOC phi for block ", I(idx), "@", I(startPC));
		    int nf = st.getFrameCountAt(startPC);
		    J2cValue[] stackIn = stackIns[idx] =
			new J2cValue[h];
		    for (int i = 0; i < h; i++) {
			stackIn[i] = (J2cValue) f.peek(i);
			// dammit! wide values!
			mc.log("input[", I(i), "] = ", stackIn[i]);
			if (stackIn[i] == null)
			    continue;
			if (nf > 1 && !(stackIn[i] instanceof JumpTarget)) {
			    mc.log("merging at ", translator.getSelector(),
				   ":", I(startPC));
			    State.Iterator it = st.iterator(startPC);
			    for (Frame f1 = it.currentFrame(); f1 != null;
				 f1 = it.nextFrame()) {
				stackIn[i] = (J2cValue)
				    stackIn[i].merge(f1.peek(i));
				mc.log("input[", I(i), "] = ",
				       stackIn[i]);
			    }
			}
			if (!stackIn[i].isConcrete()) {
			    stackIn[i] = stackIn[i].copy(null);
			    allocator.allocateTemp(stackIn[i],
						   J2cValue.MERGED_STACK_SLOT,
						   i);
			}
		    }
		}
	    }
	}

	public void finish() {
	    if (mc.tryBlock != null)
		mc.tryBlock.accept(new TryBlock.Visitor() {
			public void visit(TryBlock _) { }
			public void visit(TryBlock _, TryBlock.Handler h) {
			    h.catchVar = stackIns[h.block.number][0];
			}
		    });
	}

	public void build(BBSpec block) {
	    this.block = block;
	    int startPC = block.startPC;
	    int endPC = (block.number == mc.block.length - 1
			 ? mc.code.getBytes().length
			 : mc.block[block.number + 1].startPC);
	    mc.log("EXTRACT block", I(block.number), "@", I(startPC));

	    Frame f = st.getFrameAt(startPC);
	    int nf = st.getFrameCountAt(startPC);
	    int h = f.getStackHeight();
	    frame = f = f.cloneFrame();

	    if (nf > 1) {
		int max = ((S3Frame) frame).getMaxLocals();
		State.Iterator it = st.iterator(startPC);
		for (Frame f1 = it.currentFrame(); f1 != null;
		     f1 = it.nextFrame()) {
		    for (int lidx = 0; lidx < max; lidx++) {
			AbstractValue v1 = frame.load(lidx);
			if (v1 == null || v1.isInvalid() || v1.isJumpTarget())
			    continue;
			AbstractValue v2 = f1.load(lidx);
			if (v2 != null)
			    v2 = v1.merge(v2);
			if (v2 == J2cValue.INVALID)
			    v2 = null;
			frame.store(lidx, v2);
		    }
		}
	    }

	    // Shit.  Manually merge stack for this frame.
	    if (nf > 0 && h > 0) {
		for (int i = 0; i < h; i++)
		    f.pop();
		for (int i = h - 1; i >= 0; i--)
		    f.push(stackIns[block.number][i]);
	    }

	    // temp array, but we like to log the start pc of a block...
	    int[] pcd = new int[2 * (endPC + 1 - startPC)];
	    pcd[0] = 0;
	    pcd[1] = startPC;	// not strictly needed...
	    int pcdix = 2;
	
	    buf.position(startPC);
	    canThrow = false;
	    done = false;
	    Instruction last = null;
	    do {
		Instruction i = buf.get();
		int pc = buf.getPC();
		int off = codeBuffer.size();
		mc.log("EXTRACT ", i, "@", I(pc));

		// Some instructions don't result in any code at all.
		if (pcd[pcdix-2] == off)
		    pcd[pcdix-1] = pc;
		else {
		    pcd[pcdix++] = codeBuffer.size();
		    pcd[pcdix++] = pc;
		}
		translator.executionTerminates = false;
		translator.setFrameAndPC(f, buf.getPC());
		visitAppropriate(i);
		last = i;
	    } while (!done && buf.position() < endPC);
	
	    // Save PC, file, line information.  Leave half an empty entry
	    // for expressions in the middle of endPC
	    block.pcDelta = new int[pcdix + 1];
	    System.arraycopy(pcd, 0, block.pcDelta, 0, pcdix);
	    block.pcDelta[pcdix] = -1;

	    if (canThrow && block.innerMostTry != null) {
		block.innerMostTry.forApplicableHandlers(ctx.throwableBP,
							 saveHandlerBlocks,
							 ctx);
		block.handler = new BBSpec[handler.size()];
		handler.toArray(block.handler);
		handler.clear();
	    } else
		block.handler = NO_NEXT_BLOCK;

	    block.fallsThrough =
		!(translator.executionTerminates
		  || (last instanceof Instruction.UnconditionalJump)
		  || (last instanceof Instruction.Switch)
		  || (last instanceof Instruction.FlowEnd)
		  || (last instanceof Instruction.RET));
	    if (block.next == null) {
		if (block.fallsThrough)
		    block.next = new BBSpec[] { mc.block[block.number+1] };
		// FIXME: I should handle ret properly
		else if (!(last instanceof Instruction.RET))
		    block.next = NO_NEXT_BLOCK;
	    }
	    if (f.getStackHeight() > 0 && block.next != NO_NEXT_BLOCK) {
		J2cValue[] stackOut = new J2cValue[f.getStackHeight()];
		for (int i = 0; i < stackOut.length; i++) {
		    stackOut[i] = (J2cValue) f.peek(i);
		    mc.log("output[", I(i), "] = ", stackOut[i]);
		}
		J2cValue[][] stackIn = new J2cValue[block.next.length][];
		for (int i = 0; i < block.next.length; i++)
		    stackIn[i] = this.stackIns[block.next[i].number];

		for (int i = 0; i < stackOut.length; i++) {
		    if (stackOut[i] == null)
			continue;
		    J2cValue rename = null;
		    for (int j = 0; j < stackIn.length; j++) {
			if (!stackIn[j][i].isConcrete()
			    && (rename == null
				|| stackIn[j][i].number > rename.number))
			    rename = stackIn[j][i];
		    }
		    if (rename != null) {
			if (stackOut[i].isConcrete()
			    // don't allow renames to cascade, it may
			    // be dangerous
			    || (rename.getBlueprint(mc.ctx.domain)
				!= stackOut[i].getBlueprint(mc.ctx.domain))
			    || stackOut[i].kind != J2cValue.STACK_SLOT)
			    addFixup(new IRExpr(rename,
						new ValueAccessExp(stackOut[i])));
			else
			    allocator.rename(stackOut[i], rename);

			for (int j = 0; j < stackIn.length; j++)
			    if (stackIn[j][i] != rename
				&& !stackIn[j][i].isConcrete())
				addFixup(new IRExpr(stackIn[j][i],
						    new ValueAccessExp(rename)));
		    }
		}
	    }
	    // dump the code
	    block.code = new Expr[codeBuffer.size()];
	    codeBuffer.toArray(block.code);
	    codeBuffer.clear();
	    initsHere.clear();
	}

	
	public J2cValue[] getInputs(int count) {
	    if (count == 0)
		return J2cValue.EMPTY_ARRAY;
	    J2cValue[] ret = new J2cValue[count];
	    J2cValue[] in = translator.getIns();
	    for (int i = 0; i < count; i++)
		ret[i] = in[i];
	    return ret;
	}

	final ArrayList initsHere = new ArrayList();

	/**
	 * J2c really needs to be refactored.  Any number of optimizations
	 * are performed inside SpecInstantation, but there are others,
	 * such as the throw conversion below, that can only be
	 * implemented after SpecInstantiation has been run to a fixpoint.
	 **/
	public class Optimizer extends Runabout {
	    ValueSource retVs;

	    ValueSource run(ValueSource vs) {
		//ValueSource outer = retVs;
		retVs = vs;
		visitAppropriate(vs);
		vs = retVs;
		//retVs = outer;
		return vs;
	    }

	    public void visit(ValueSource vs) { }

	    public final InternalReference j2c_throw =
		J2cValue.makeSymbolicReference("j2c_throw");

	    public void visit(ClinitExp exp) {
		if (initsHere.contains(exp.bp))
		    retVs = null;
		// FIXME: Why am I expanding this out, instead of
		// generating the if in a special case in CodeGen?
		else {
		    initsHere.add(exp.bp);
		    J2cInt cidxv =
			J2cValue.makeIntConstant(TypeCodes.INT,
						 exp.bp.getCID());
		    J2cInt idxv  =
			J2cValue.makeIntConstant(TypeCodes.INT,
						 exp.bp.getUID());
		    Value test = new J2cInt(new InvocationExp("needs_init",
							      cidxv, idxv,
							      ctx.intBP),
					    TypeCodes.INT);
		    Value go = new J2cVoid(exp.csaCall);
		    Value set = new J2cVoid(new InvocationExp("did_init",
							      cidxv, idxv,
							      null));
		    Value then = new J2cVoid(new SeqExp(go, set));
		    retVs = new IfCmd(test, then);
		}
	    }
	    
	    public void visit(InvocationExp exp) {
		if (!ctx.noCppExceptions && !ctx.cExceptions && exp.target == j2c_throw) {
		    // Optimize j2c_throw() calls to throw statements
		    // if:
		    // 1. We know the exact type of the exeception
		    //    from local dataflow, or
		    // 2. We know the only possible type of the
		    //    exception from global analysis.
		    // In 
		    J2cReference ex = (J2cReference) exp.args[0];
		    if (ex.t.exactTypeKnown()) {
			retVs = new InvocationExp("THROW", ex, exp.rt);
		    } else if (ctx.anal != null) {
			Blueprint[] subs
			    = ctx.anal.concreteSubtypesOf(ex.getBlueprint());
			if (subs != null && subs.length == 1) {
			    if (subs[0] != ex.getBlueprint())
				ex = new J2cReference(new ConversionExp(ex),
						      (S3Blueprint) subs[0]);
			    retVs = new InvocationExp("THROW", ex, exp.rt);
			}
		    }
		    if (retVs != exp && ex.t.includesNull()) {
			// FIXME: Maybe it would be easier to throw in
			// null checks as needed, and eliminate them
			// in a recursive call to optimize.
			J2cValue nullCk
			    = new J2cVoid(new InvocationExp
					  ("NULLCHECK", ex, ctx.voidBP));
			retVs = new SeqExp(nullCk, new J2cVoid(retVs));
		    }
		}
	    }
	}

	Optimizer opt = new Optimizer();

	void walkEvals() {
	    J2cValue[] eval = translator.evals;
	    for (int i = 0; i < eval.length; i++) {
		// all done
		if (eval[i] == J2cValue.TERMINATOR)
		    break;
		// SpecInstantiation eliminated this eval
		if (eval[i] == J2cValue.NULL)
		    continue;
		// check for throw optimization and redundant ClinitExps
		if (eval[i].source instanceof LocalStore) {
		    LocalStore st = (LocalStore) eval[i].source;
		    // local variable ranges start AFTER the initial store
		    // instruction
		    int idx = st.index.intValue();
		    J2cValue var = findVar(idx,
					   translator.getPC()
					   + (idx > 65535 ? 4
					      : idx > 3 ? 2
					      : 1));
		    J2cValue v = ((J2cValue) st.value);
		    if (v.kind == J2cValue.NOVAR && !v.isConcrete())
			addExpr(new IRExpr(var, mungeExpr(v.source)));
		    else
			addExpr(new IRExpr(var, new ValueAccessExp(v)));
		    continue;
		}

		ValueSource vs = opt.run(eval[i].source);
		if (vs != null)
		    addExpr(new IRExpr(null, vs));
	    }
	}

	public void visit(Instruction insn) {
	    if (insn instanceof Instruction.ExceptionThrower)
		canThrow = true;
	    translator.visitAppropriate(insn);
	    walkEvals();
	    J2cValue[] out = translator.getOuts();
	    // Declare _stack variables for this instruction's
	    // results.  The abstact interpreter will propagate
	    // results further down the basic block, but we want to
	    // make sure the instruction's code is only generated
	    // once.
	    //
	    // We give each J2cValue a name, corresponding to a
	    // variable that is set here, and referenced further down
	    // in this basic block.
	    for (int i = 0; i < out.length; i++) {
		if (out[i] == J2cValue.TERMINATOR)
		    break;
		else if (out[i] == null)
		    continue;
		else if (out[i].source == null) {
		    if (!out[i].isConcrete())
			OVMBase.fail("abstract value " + out[i] +
				     " has no concrete value or expression");
		    // constants don't need names
		} else if (out[i].isConcrete()
			   && out[i].source instanceof J2cLocalExp) {
		    // Don't declare a _stack variable for this value,
		    // references will be compiled as a constant
		    // rather than a C++ variable reference anyway,
		    // and we don't want to call allocateTemp for the
		    // same J2cValue more than once.
		} else {
		    if (out[i].isConcrete()
			// AFIAT can take a constant parameter, and it
			// may be safer to leave the unchecked casts
			// in generated C++ code.
			&& !(out[i].source instanceof ValueAccessExp))
			System.out.println
			  (translator.where("concrete value " + out[i] +
					    " has source exp " +
					    out[i].source));
		    allocator.allocateTemp(out[i],
					   J2cValue.STACK_SLOT,
					   i);
		    addExpr(new IRExpr(out[i],  out[i].source));
		}
	    }
	    done = translator.executionTerminates;
	    if (done)
		block.next = NO_NEXT_BLOCK;
	}

	// pick up <clinit> calls in shared-state loads
	public void visit(Instruction.ConstantPoolLoad insn) {
	    translator.visitAppropriate(insn);
	    walkEvals();
	}
	// Bookkeeping instructions that need not generate any IR
	public void visit(Instruction.ConstantLoad i) {
	    translator.visitAppropriate(i);
	}
	public void visit(Instruction.StackManipulation i) {
	    translator.visitAppropriate(i);
	}
	// Instructions that either terminate a basic block, or are not
	// completely described by the IR
	public void visit(Instruction.ReturnValue i) {
	    visit((Instruction) i);
	    addExpr(new BCExpr(null, i, getInputs(1)));
	    block.next = NO_NEXT_BLOCK;
	    done = true;
	}
	public void visit(Instruction.RETURN i) {
	    visit((Instruction) i);
	    addExpr(new BCExpr(null, i, getInputs(0)));
// 	    if (mc.meth.getMode().isSynchronized())
// 		block.next = block.handler;
// 	    else
		block.next = NO_NEXT_BLOCK;
	    done = true;
	}
	public void visit(Instruction.ATHROW i) {
	    visit((Instruction) i);
	    block.next = NO_NEXT_BLOCK;
// 	    block.next = block.handler;
	    done = true;
	}
	// This is a bit of a problem.  Subroutines get multiple Frames if
	// they are called with different local variables.  These frames
	// should really be merged, and the abstract value on the stack
	// should tell us what successors we have.
	public void visit(Instruction.RET i) {
	    int vidx = i.getLocalVariableOffset(buf);
	    State.Iterator it = st.iterator(buf.getPC());
	    for (Frame f = it.currentFrame();
		 f != null;
		 f = it.nextFrame()) {
		J2cJumpTarget pc = (J2cJumpTarget) f.load(vidx);
		if (!handler.contains(pc))
		    handler.add(pc);
	    }
	    if (handler.size() == 1) {
		System.err.println(translator.where("RET has one target"));
		J2cJumpTarget pc = (J2cJumpTarget) handler.get(0);
		addExpr(new BRExpr(null, i, getInputs(0)));
		block.next = new BBSpec[] { mc.findBlock(pc.pc) };
	    } else {
		visit((Instruction) i);
		J2cValue[] ins = new J2cValue[] {
		    allocator.allocateLocal(translator.getIns()[0],
					    vidx,
					    "_local" + vidx)
		};
		addExpr(new BCExpr(null, i, ins));
		block.next = new BBSpec[handler.size()];
		for (int idx = 0; idx < block.next.length; idx++) {
		    J2cJumpTarget pc = (J2cJumpTarget) handler.get(idx);
		    block.next[idx] = mc.findBlock(pc.pc);
		}
	    }
	    handler.clear();
	    done = true;
	}

	public void visit(Instruction.Switch i) {
	    // must decode targets and keys
	    visit((Instruction) i);
	    addExpr(new SWExpr(null, i, getInputs(1),
			       i.getIndexForTargets(buf),
			       getPC()));
	    int[] target = i.getTargets(buf);
	    int dflt = i.getDefaultTarget(buf);
	    int pc = getPC();
	    block.next = new BBSpec[target.length + 1];
	    for (int idx = 0; idx < target.length; idx++)
		block.next[idx] = mc.findBlock(target[idx] + pc);
	    block.next[target.length] = mc.findBlock(dflt + pc);
	    done = true;
	}

	public void visit(Instruction.UnconditionalJump i) {
	    visit((Instruction) i);
	    int npc = getPC() + i.getTarget(buf);
	    addExpr(new BRExpr(null, i, getInputs(0)));
	    block.next = new BBSpec[] { mc.findBlock(npc) };
	    done = true;
	}

	public void visit(Instruction.ConditionalJump i) {
	    visit((Instruction) i);
	    int npc = getPC() + i.size(buf);
	    int bpc = getPC() + i.getBranchTarget(buf);
	    J2cValue test = translator.j2cCanonicalize(i.getControlValue());
	    addExpr(new BRExpr(null, i, new J2cValue[] { test }));
	    block.next = new BBSpec[] { mc.findBlock(npc), mc.findBlock(bpc) };
	    done = true;
	}
    
	public void visit(Instruction.POLLCHECK i) {
	    hasSafePoint = false;
	    noteSafePoint();
	    addExprWithSafePoints(new BCExpr(null,
					     i,
					     J2cValue.EMPTY_ARRAY));
	}

	public void visit(Instruction.NULLCHECK i) {
	  
	    translator.popInputs(i);

	    J2cValue[] ins = getInputs(1);
	    addExpr(new BCExpr(null, i, ins));
	    
	    translator.pushOutputs(i);

	}

	public void visit(Instruction.INCREMENT_COUNTER i) {
	  
	    translator.popInputs(i);

	    J2cValue[] ins = getInputs(1);
	    addExpr(new BCExpr(null, i, ins));
	    
	   // translator.pushOutputs(i);

	}
	
	
    }
    static String I(int i) {
	return (J2cImageCompiler.KEEP_LOGS
		? Integer.toString(i)
		: null);
    }
    
    public void compact() {
	int newLen = 0;
	for (int i = 0; i < code.length; i++)
	    if (code[i] != null)
		newLen++;
	if (newLen == code.length)
	    return;
	Expr[] newCode = new Expr[newLen];
	for (int i = 0, j = 0, k = 0; i < code.length; i++) {
	    if (k < pcDelta.length && pcDelta[k] == i) {
		pcDelta[k] = j;
		k += 2;
	    }
	    if (code[i] != null) {
		newCode[j++] = code[i];
	    }
	}
	code = newCode;
    }
	
    static public void printCFG(BBSpec[] block) {
	for (int i = 0; i < block.length; i++) 
	    System.err.println(block[i]);
    }

    /**
     * An alternative to computeLiveness.  Assume that all pointer
     * variables are live after ever expression in a block
     **/
    public static void assumeLiveness(MethodCompiler mc, LocalAllocator allocator)
    {
// 	System.err.print("asssumeLiveness found: ");
// 	String pfx = "";
// 	final BitSet liveRef = new BitSet();
// 	J2cValue[] locals = allocator.findLocals(LocalAllocator.REF_VAR);
// 	for (int i = 0; i < allocator.nlocals; i++) {
// 	    if (locals[i] != null) {
// // 		System.err.print(pfx + locals[i].getName());
// // 		pfx = ", ";
// 		liveRef.set(i);
// 	    }
// 	}
// // 	System.err.println();
// 	for (int i = 0; i < mc.block.length; i++) {
// 	    for (int j = 0; j < mc.block[i].code.length; j++)
// 		mc.block[i].code[j].liveRefOut = liveRef;
// 	}
// 	if (mc.tryBlock != null)
// 	    mc.tryBlock.accept(new TryBlock.Visitor() {
// 		    public void visit(TryBlock t) {
// 			t.liveAtCatch = liveRef;
// 		    }
// 		    public void visit(TryBlock _, TryBlock.Handler __) { }
// 		});
    }
}
