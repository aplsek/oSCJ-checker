package s3.services.j2c;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.TypeName;
import ovm.core.repository.Selector;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.InstructionVisitor;
import ovm.util.ArrayList;
import ovm.util.BitSet;
import ovm.util.Collections;
import ovm.util.Comparator;
import ovm.util.ListIterator;
import ovm.util.NoSuchElementException;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;

/**
 * TODO:
 * 0. OK.  Get something running and get something checked in.  The
 *    first step is probably to back out the changes to
 *    SpecInstantiation, and keep testing this code until it stops
 *    crashing and produces no obviously bad results.
 * 1. Get this done
 *    a. deal with reachable and stateful instructions better
 *    b. dump SpecInstantion.resolveTypeName and so on
 *    c. determine whether handers (not their target PCs) are live,
 *       and start pruning try blocks without live handlers
 *    d. hoist translateThrowable calls to the first block that
 *       catches unchecked exceptions.
 * 2. Fix the way abstract interpreter frames are merged, and start
 *    dealing with JSR/RET 
 *    a. compute liveness in all methods, and use it outside of
 *       handlers
 *    b. use liveness inside handlers
 * N. Merge into HEAD and add optimizations
 *    a. use abstract interpreter to eliminate NULLCHECKs, including
 *       those on nonvirtual invocations
 *    b. use forward dataflow to eliminate initializeBlueprint
 *    c. find blocks that live inside catch clauses, and find
 *       j2c_throw() calls that can be throw;
 *
 *       NOTE: I've been wanting to do this for a long time, but it
 *       is not really safe with precise gc.  I suppose to make it
 *       GC-safe one would have to catch a *&amp;, save the address
 *       of the thrown exception as part of the thread-state, and
 *       clear that address before leaving a catch block normally.
 *
 *       Is this optimization really worth the trouble?
 * 
 * TODO some more:
 * 1. Replace all these instance methods with static methods that can
 *    safely be called on null.
 * 2. Rename Handler to Catch
 * 3. Rename BBSpec to BasicBlock
 * 4. Rename SpecInsantiation to AbstractInterpreter (or J2cIntepreter)
 * 5. Move more code from J2cInterpreter into MethodCompiler:
 *    o line number parser
 *    o local variable lookup
 *    ...
 **/
public class TryBlock {
    TryBlock next;
    TryBlock inner;
    TryBlock outer;
    Handler handler;

    boolean hasUncheckedCatch;
    boolean hasXDCalls;
    
    int startPC, endPC;
    BBSpec firstBlock, lastBlock;

    BitSet liveAtCatch;
    
    private static int idCount;
    int id;

    public static class Handler {
	S3Blueprint bp;
	J2cValue catchVar;
	Handler next;

	int PC;
	BBSpec block;

	boolean reachable;

	Handler(S3Blueprint bp, int pc, Handler next) {
	    this.bp = bp;
	    this.PC = pc;
	    this.next = next;
	}

	// PARBEGIN PAREND
	boolean isUncheckedCatch(Context ctx) {
	    return (bp == ctx.throwableBP
		    || bp.isSubtypeOf(ctx.RuntimeExceptionBP)
		    || bp.isSubtypeOf(ctx.ErrorBP)
		    || bp == ctx.EDAbortedExceptionBP
		    || bp == ctx.AbortedExceptionBP);
	}

	public String toString() { return bp + " => " + PC;    }
    }

    public interface Visitor {
	void visit(TryBlock t);
	void visit(TryBlock t, Handler h);
    }

    public interface HandlerVisitor {
	void visit(Handler h);
    }

    public TryBlock findInnerMost(int pc) {
// 	System.err.println("findInnerMost(" + pc + ") in");
// 	System.err.println(this);
	if (pc < startPC)
	    ;
	else if (pc < endPC) {
	    TryBlock ret = inner == null ? null : inner.findInnerMost(pc);
	    if (ret == null) {
// 		System.err.println("findInnerMost(" + pc + ") =>");
// 		System.err.println(this);
		return this;
	    } else
		return ret;
	} else if (next != null)
	    return next.findInnerMost(pc);
	else
	    ;
// 	System.err.println("findInnerMost(" + pc + ") => null");
	return null;
    }

    static class BPList {
	final S3Blueprint bp;
	final BPList next;
	BPList(S3Blueprint bp, BPList next) {
	    this.bp = bp;
	    this.next = next;
	}
	boolean contains(S3Blueprint bp) {
	    for (BPList l = this; l != null; l = l.next)
		if (bp == l.bp || bp.isSubtypeOf(l.bp))
		    return true;
	    return false;
	}
    }
    /**
     * When called on the innermost TryBlock of a particular
     * instruction, this method iterates over all Handlers that apply
     * to the exception type the instruction may throw
     **/
    public void forApplicableHandlers(S3Blueprint bp,
				      HandlerVisitor v,
				      Context ctx) {
	BPList seen = null;
// 	System.err.println("forApplicableHandlers" + bp);
	for (TryBlock t = this; t != null; t = t.outer)
	    for (Handler h = t.handler; h != null; h = h.next) {
		if ((seen == null || !seen.contains(h.bp))
		    && (bp.isSubtypeOf(h.bp)
			|| h.bp.isSubtypeOf(bp)
			|| h.bp == ctx.EDAbortedExceptionBP
			|| h.bp == ctx.AbortedExceptionBP)) {

		    seen = new BPList(h.bp, seen);
//  		    System.err.println(h.bp + " catches " + bp);
		    v.visit(h);
		} else {
//  		    System.err.println(h.bp + " does not catch " + bp);
		}
	    }
    }

    public void accept(Visitor v) {
	for (TryBlock t = this; t != null; t = t.next) {
	    v.visit(t);
	    for (Handler h = t.handler; h != null; h = h.next)
		v.visit(t, h);
	    if (t.inner != null)
		t.inner.accept(v);
	}
    }

    public void markBoundaryPCs(final BitSet b) {
	accept(new Visitor() {
		public void visit(TryBlock t) {
		    b.set(t.startPC);
		    b.set(t.endPC);
		}
		public void visit(TryBlock _, Handler h) {
		    b.set(h.PC);
		}
	    });
    }

    public TryBlock prune(MethodCompiler mc) {
	hasUncheckedCatch = false;
	for (Handler prev = null, h = handler; h != null; h = h.next) {
	    if (h.reachable) {
		if (h.isUncheckedCatch(mc.ctx))
		    hasUncheckedCatch = true;
		prev = h;
	    } else {
		mc.log("prune catch ",  this, ", " , h);
		if (prev == null)
		    handler = h.next;
		else
		    prev.next = h.next;
	    }
	}
	if (inner != null)
	    inner = inner.prune(mc);
	if (next != null)
	    next = next.prune(mc);
	if (handler == null) {
	    mc.log("prune try " + this);
	    if (inner != null) {
		TryBlock t = inner;
		while(t.next != null) {
		    t.outer = outer;
		    t = t.next;
		}
		t.outer = outer;
		t.next = next;
		return inner;
	    } else
		return next;
	} else
	    return this;
    }

    public String toString() { return "[" +  startPC + ", " + endPC + ")"; }
    
    public void toStringBuf(StringBuffer buf, String indent) {
	if (indent.length() > 40) {
	    buf.append("...\n");
	    return;
	}
	for (TryBlock t = this; t != null; t = t.next) {
	    buf.append(indent);
	    buf.append(t);
	    buf.append(" {\n");
	    if (t.inner != null) 
		t.inner.toStringBuf(buf, indent + "  ");
	    buf.append(indent);
	    buf.append("}\n");
	    for (Handler h = t.handler; h != null; h = h.next) {
		buf.append(indent);
		buf.append("catch ");
		buf.append(h);
		buf.append("\n");
	    }
	}
    }
    public String toLongString() {
	StringBuffer buf = new StringBuffer();
	toStringBuf(buf, "");
	return buf.toString();
    }

    public void link(final MethodCompiler mc) {
	accept(new Visitor() {
		public void visit(TryBlock t) {
		    t.firstBlock = mc.findBlock(t.startPC, false);
		    try {
			BBSpec after = mc.findBlock(t.endPC, false);
			t.lastBlock = mc.block[after.number - 1];
		    } catch (NoSuchElementException _) {
			// The try block must cover the last
			// reachable instruction
			t.lastBlock = mc.block[mc.block.length - 1];
		    }
		}
		public void visit(TryBlock _, Handler h) {
		    h.block = mc.findBlock(h.PC);
		}
	    });
    }

    private static S3Blueprint resolve(MethodCompiler mc,
				       ExceptionHandler eh) {
	return mc.resolve(eh.getCatchTypeName());
    }

    // When visit is called with nextPC in the middle of a try block
    // that curPC is outside of entirely, that try block is split so
    // that nextPC is the first instruction of a try block.  In C++,
    // it is illegal to jump into the middle of a try.
    private static class Splitter extends MethodCompiler.BranchVisitor {
	final MethodCompiler mc;
	final ArrayList handlers;
	boolean handlingCatches = false;
	boolean changed;
	Splitter(MethodCompiler mc, ArrayList handlers) {
	    this.mc = mc;
	    this.handlers = handlers;
	}

	public void visit(Instruction.JsrInstruction i) {
	    // FIXME: We really don't know how to split for JSR/RET,
	    // but we can assume that if the JSR target is not in this
	    // block, then neither is the return point.
	    int target = i.getTarget(buf) + buf.getPC();
	    visit(target, buf.getPC() + i.size(buf));
	    super.visit(i);
	}

	public void visit(int curPC, int nextPC) {
	    // skip JSR/RET
	    if (curPC == -1 || nextPC == -1)
		return;

	    for (ListIterator it = handlers.listIterator(); it.hasNext(); ) {
		ExceptionHandler h = (ExceptionHandler) it.next();
		if (h.getStartPC() < nextPC && h.getEndPC() > nextPC
		    && (h.getStartPC() > curPC || h.getEndPC() <= curPC)) {
		    /* nextPC is in the middle of this handler, and
		     * curPC is outside of the handler.  Split the
		     * handler so that a handler starts at nextPC
		     */
 		    if (!handlingCatches)
 			mc.log("jump from ", curPC + " -> " + nextPC,
			       " splits ", h);
//  		    else
//  			System.err.println("splitting recursive try "
// 					   + h + " for endPC=" + curPC
// 					   + " and handlerPC=" + nextPC);
		    it.remove();
		    it.add(new ExceptionHandler((char) h.getStartPC(),
						(char) nextPC,
						(char) h.getHandlerPC(),
						h.getCatchTypeName()));
		    it.add(new ExceptionHandler((char) nextPC,
						(char) h.getEndPC(),
						(char) h.getHandlerPC(),
						h.getCatchTypeName()));
		    changed = true;
		}
	    }
	}

	public void fix(S3ByteCode code) {
	    do {
		changed = false;

		handlingCatches = true;
		ExceptionHandler[] h = new ExceptionHandler[handlers.size()];
		handlers.toArray(h);
		for (int i = 0; i < h.length; i++) {
			// this line splits recursive exception handlers
			visit(h[i].getEndPC(), h[i].getHandlerPC());
			// this line splits subsequent exception handlers that start exactly where h ends
			visit(h[i].getEndPC() - 1, h[i].getHandlerPC());
		}
		handlingCatches = false;
		
		run(code);
		mc.log("pass complete");
	    } while (changed);
	}
    }

    // For all i and j where i < j, look at the PC ranges of
    // handler[i] and handler[j].  If they overlap at all, the range
    // of handler[i] must be a subset of the range of handler[j].  If
    // this invarient does not hold, split handler[i] so that part of
    // it is enclosed by handler[j], and part of it is disjoint.
    private static void forceNesting(ArrayList handlers) {
	for (ListIterator it = handlers.listIterator(); it.hasNext(); ) {
	    ExceptionHandler h_i = (ExceptionHandler) it.next();
	    int h_i_start = h_i.getStartPC();
	    int h_i_end   = h_i.getEndPC();
	    for (int j = it.nextIndex(); j < handlers.size(); j++) {
		ExceptionHandler h_j = (ExceptionHandler) it.next();
		int h_j_start = h_j.getStartPC();
		int h_j_end   = h_j.getEndPC();
		if (h_i_start < h_j_end && h_i_end > h_j_start) {
		    // OK, they overlap.  Make sure that h_i's range
		    // is strictly smaller.
		    if (h_i_start < h_j_start) {
			System.err.println("split start of "
					   + h_i + " for " + h_j);
			it.remove();
			// Add the overlapping part first
			h_i = new ExceptionHandler((char) h_j_start,
						   (char) h_i_end,
						   (char) h_i.getHandlerPC(),
						   h_i.getCatchTypeName());
			it.add(h_i);
			it.add(new ExceptionHandler((char) h_i_start,
						    (char) h_j_start,
						    (char) h_i.getHandlerPC(),
						    h_i.getCatchTypeName()));
			it.previous();         // Back up to new h_i
			j++;		       // fix inner loop index
			h_i_start = h_j_start; // fix range
		    }
		    if (h_i_end > h_j_end) {
			System.err.println("split end of "
					   + h_i + " for " + h_j);
			it.remove();
			// Add the overlapping part first
			h_i = new ExceptionHandler((char) h_i_start,
						   (char) h_j_end,
						   (char) h_i.getHandlerPC(),
						   h_i.getCatchTypeName());
			it.add(h_i);
			it.add(new ExceptionHandler((char) h_j_end,
						    (char) h_i_end,
						    (char) h_i.getHandlerPC(),
						    h_i.getCatchTypeName()));
			it.previous();         // Back up to new h_i
			j++;		       // fix inner loop index
			h_i_end = h_j_end;     // fix range
		    }
		}
	    }
	}
    }

    private TryBlock(ExceptionHandler h, S3Blueprint catchBP,
		     TryBlock next, TryBlock outer,
		     Context ctx) {
	this.next = next;
	this.outer = outer;
	startPC = h.getStartPC();
	endPC = h.getEndPC();
	handler = new Handler(catchBP, h.getHandlerPC(), null);
	if (handler.isUncheckedCatch(ctx))
	    hasUncheckedCatch = true;
	id=idCount++;
    }

    // This method is called on a strictly nested exception handler
    // table, from the last (most general) entry up to the first
    // (most specific) entry.  If we need to insert a new TryBlock,
    // it will always be either a sibling or a child.  It will never
    // be a parent.
    private TryBlock insertHandler(ExceptionHandler h, S3Blueprint catchBP,
				   Context ctx) {
	if (h.getStartPC() < startPC)
	    return new TryBlock(h, catchBP, this, outer, ctx);
	else if (h.getStartPC() == startPC && h.getEndPC() == endPC) {
	    handler = new Handler(catchBP, h.getHandlerPC(), handler);
	    if (handler.isUncheckedCatch(ctx))
		hasUncheckedCatch = true;
	}
	else if (h.getEndPC() <= endPC) {
	    if (inner == null)
		inner = new TryBlock(h, catchBP, null, this, ctx);
	    else
		inner = inner.insertHandler(h, catchBP, ctx);
	} else {
	    if (next == null)
		next = new TryBlock(h, catchBP, null, outer, ctx);
	    else
		next = next.insertHandler(h, catchBP, ctx);
	}
	return this;
    }
	    
    public static TryBlock make(final MethodCompiler mc) {
	S3ByteCode cf = mc.code;
	ExceptionHandler[] h = cf.getExceptionHandlers();
	if (h.length == 0)
	    return null;
	mc.log("orginal exception table:");
	ArrayList handlers = new ArrayList(h.length);
	for (int i = 0; i < h.length; i++) {
	    try {
		resolve(mc, h[i]);
		mc.log(h[i]);
		handlers.add(h[i]);
	    } catch (LinkageException.Runtime e) {
		System.err.println("catch of nonexistent type: " + h[i]);
	    }
	}

	Splitter splitter = new Splitter(mc, handlers);

	// First split to ensure that handlers only overlap when a
	// more specific one is nested within a less specific one.
	// Again, this should not effect java code
	forceNesting(handlers);

	// Split recursive exception handlers into a recursive and a
	// non-recursive part.  Javac actually does this sort of
	// thing.
	// 
	// Also, split according to actual branches.  We want to
	// do this last, because while a jump may have been OK
	// initially, the first two steps might have split the source
	// and target PCs into two different blocks.
	// 
	// These steps must be repeated until a fixpoint is acheived.
	splitter.fix(cf);

	// Now we are ready to build the TryBlock tree.  We want to
	// build the tree starting at the most general handlers and
	// moving down to the most specific ones.
	ExceptionHandler first =
	    (ExceptionHandler) handlers.get(handlers.size() - 1);
	TryBlock ret = new TryBlock(first, resolve(mc, first), null, null,
				    mc.ctx);
	mc.log("final handlers");
	mc.log(ret);
	for (int i = handlers.size() - 1; i --> 0; ) {
	    ExceptionHandler eh = (ExceptionHandler) handlers.get(i);
	    mc.log(eh);
	    ret = ret.insertHandler(eh, resolve(mc, eh), mc.ctx);
	    // mc.log("tree = " + ret.toLongString());
	}

	// All done
// 	System.err.println("Initial TryBlock");
// 	System.err.println(ret);
// 	ret = ret.prune(mc.ctx);
// 	System.err.println("Pruned TryBlock");
// 	System.err.println(ret);
	return ret;
    }
}
