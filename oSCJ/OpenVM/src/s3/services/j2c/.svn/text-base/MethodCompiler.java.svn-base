package s3.services.j2c;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.ovmj.util.Runabout;

import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.Mode;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeCodes;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.analysis.Frame;
import ovm.services.bytecode.analysis.State;
import ovm.util.BitSet;
import ovm.util.ListIterator;
import ovm.util.NoSuchElementException;

import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Method;

import s3.services.j2c.J2cValue.J2cReference;

import s3.util.PragmaMayNotLink;

public class MethodCompiler {
    static final boolean USE_LIVENESS = true;
    
    // Save some important information about the method being compiled.
    final StringBuffer logBuffer;
    final S3Method meth;
    final S3ByteCode code;
    final Context ctx;
    final Type.Context tctx;

    // Save data structures that are needed all the way through C++
    // code generation.
    TryBlock tryBlock;
    SpecInstantiation.LineNumberParser parser;	// FIXME: move here
    BitSet storedVariables;	// FIXME: used?
    LocalAllocator allocator;
    BBSpec[] block;
    boolean hasXDCalls;
    int maxMultiArray = 0;
    BitSet everLive;
    BitSet falseLiveness;

    boolean lookupFailure;

    static final int debugName = -1; // Context.getUTF("Intersect");
    static final Selector debugSel = null;
// 	RepositoryUtils.selectorFor("Lovm/core/repository/Mode$AllModes;",
// 				    "<init>:(I)V");
    boolean debug;

    public MethodCompiler(StringBuffer logBuffer,
			  S3Method meth,
			  S3ByteCode code,
			  Context ctx) {
	this.logBuffer = logBuffer;
	this.meth = meth;
	this.code = code;
	this.ctx = ctx;
	tctx = meth.getDeclaringType().getContext();

	debug = (meth.getSelector().getNameIndex() == debugName
		 || meth.getSelector() == debugSel);
    }

    public void compile(PrintWriter w) {
	tryBlock = TryBlock.make(this);
	final BitSet blockStart = new BitSet();
	final BitSet statefulInstructions = new BitSet();
	blockStart.set(0);
	new BranchVisitor() {
	    public void visit(int fromPC, int toPC) {
		if (toPC == -1)
		    statefulInstructions.set(fromPC);
		else
		    blockStart.set(toPC);
	    }
	}.run(code);
	if (tryBlock != null)
	    // Don't assume that a try block creates a basic block
	    // until you know whether the try block is real (up to
	    // 20%) can be eliminated
	    tryBlock.markBoundaryPCs(statefulInstructions);

	statefulInstructions.or(blockStart);
	log("saving interpreter state at", statefulInstructions);
	SpecInstantiation translator =
	    new SpecInstantiation(this, statefulInstructions);
	State state = runAI(translator);
	if (state == null) {
	    ctx.dontCompile(meth);
	    return;
	}

	parser = translator.parser;
	storedVariables = translator.storedVariables;

	if (tryBlock != null) {
	    tryBlock = tryBlock.prune(this);
	    if (tryBlock == null) {
		log("no try blocks");
	    } else {
		log("final try blocks");
		if (J2cImageCompiler.KEEP_LOGS)
		    log(tryBlock.toLongString());
		tryBlock.markBoundaryPCs(blockStart);
	    }
	}
	blockStart.and(translator.reachable);
	log("reachable instructions at: ", translator.reachable);
	log("found basic blocks at: ", blockStart);

	allocator = new LocalAllocator(this,
				       translator.getMaxLocals(),
				       translator.getMaxStack());
	block = new BBSpec[blockStart.cardinality()];
	for (int i = 0, start = 0;
	     start != -1;
	     start = blockStart.nextSetBit(start + 1), i++)
	    block[i] = new BBSpec(this, start, i);
	if (tryBlock != null)
	    tryBlock.link(this);
	BBSpec.Builder bbb = new BBSpec.Builder(this, translator, state);
	for (int i = 0; i < block.length; i++)
	    bbb.build(block[i]);
	bbb.finish();

	boolean sync = meth.getMode().isSynchronized();
	boolean trace = false;//meth.getSelector().getNameIndex() == debugName;
	if (trace) {
	    System.err.println("\ndataflow for " + meth.getSelector());
	    BBSpec.printCFG(block);
	    if (tryBlock != null)
		System.err.print(tryBlock.toLongString());
	}
	if (sync && allocator.findLocal(TypeCodes.OBJECT, 0) == null) {
	    S3Blueprint bp =
		ctx.blueprintFor(meth.getDeclaringType());
	    J2cValue thisVar =
		new J2cReference(null, bp, false, true);
	    allocator.allocateLocal(thisVar,
				    0,
				    "ovm_this");
	}

	if (ctx.compiler.propagateCopies)
	    new CopyPropagation(this).run();
	Liveness l = new Liveness(this);
	l.run();
	if (ctx.gcFrames)
	    everLive = l.everLive();
	if (ctx.compiler.assumeLiveness && ctx.gcFrames) {
	    J2cValue[] allVars = allocator.getAllVars();
	    for (int i = 0; i < allVars.length; i++) {
		if (allVars[i] != null
		    && allVars[i].kind == J2cValue.LOCAL_VAR
		    && allVars[i].flavor == LocalAllocator.REF_VAR)
		    everLive.set(allVars[i].number);
	    }
	}

	// Now, we have to deal not only with blocks, but
	// Exception handlers: The C++ exception handlers do not
	// contain code, but merely assign the thrown object to
	// the handler's incoming stack node, and jumps to the
	// appropriate PC.
    
	// What about lexical blocks?  Does it help to limit the scope
	// of each temporary corresponding to a stack slot, or maybe
	// reuse them in some fashion?  Reusing them would probably
	// help, given gcc's difficulty with the interpreter.
	new CodeGen(this, w);
	if (debug)
	    dumpLog(System.err, null, "compiled " + meth);
// 	if (lookupFailure) {
// 	    PrintWriter db = new PrintWriter(System.err);
// 	    code.dumpAscii("LOOKUP FAILURE", db);
// 	    db.println("");
// 	    db.println("");
// 	    db.flush();
// 	}
    }

    private State runAI(SpecInstantiation machine) {
       	SpecInstantiation.J2cFixpointIterator it
	    = machine.getFixpointIterator();
	Runabout[] visitors 
	    = new Runabout[] { machine, it.getPostVisitor() };
	try {
	    ovm.services.bytecode.analysis.Driver.run(visitors, it);
	} catch (LinkageException.Runtime e) {
	    Type t = meth.getDeclaringType();
	    if (!PragmaMayNotLink.declaredBy(meth.getSelector(),
					     ctx.blueprintFor(t))) {
		System.err.println(machine.getSourceFile()
				   + ":" + machine.getLine()
				   + ": method " + meth.getSelector()
				   + " is boot time: "
				   + e.getMessage());
 		e.printStackTrace(System.err);
	    }
	    return null;
	}
	return it.getState();
    }

    public BBSpec findBlock(int pc, boolean exact) {
	for (int i = 0; i < block.length; i++)
	    if (block[i].startPC >= pc) {
		if (exact && block[i].startPC != pc)
		    throw new NoSuchElementException("pc " + pc +
						     " in mid-block");
		else
		    return block[i];
	    }
	throw new NoSuchElementException("pc " + pc + " off end of blocks");
    }
    public BBSpec findBlock(int pc) { return findBlock(pc, true); }

    /**
     * Iterate over all control flow changes in a method.  These
     * change include both taken branches and not-taken branches, so
     * when a conditional branch is encountered, we call visit on the
     * current PC, and the branch target, then we call visit again
     * with the current PC and the next PC.<p>
     *
     * The effects of JSR/RET instructions are also dealt with in a
     * vague manner.  There may be a branch to the instruction
     * following every JSR, but we don't know the source of the
     * branch, so we call visit with a source PC of -1.  Similarly,
     * there is a branch at every RET instruction, but we don't know
     * where the branch might take us, so we call vist with a target
     * PC of -1.<p>
     *
     * FIXME:  Should probably be in <code>ovm.services.bytecode</code>.
     * Can this be used in other places such as bootimage or
     * simplejit?  
     **/
    static public abstract class BranchVisitor extends Instruction.Visitor {
	protected InstructionBuffer buf;
	public abstract void visit(int curPC, int nextPC);

	public void visit(Instruction.ConditionalJump i) {
	    visit(buf.getPC(), i.getBranchTarget(buf) + buf.getPC());
	    visit(buf.getPC(), i.size(buf) + buf.getPC());
	}
	public void visit(Instruction.UnconditionalJump i) {
	    visit(buf.getPC(), i.getTarget(buf) + buf.getPC());
	}
	public void visit(Instruction.JsrInstruction i) {
	    visit((Instruction.UnconditionalJump) i);
	    // We know that there will be a branch to the next
	    // instruction, but we don't know where it will come from.
	    visit(-1, i.size(buf) + buf.getPC());
	}
	public void visit(Instruction.RET i) {
	    visit(buf.getPC(), -1);
	}
	public void visit(Instruction.WIDE_RET i) {
	    visit(buf.getPC(), -1);
	}
	public void visit(Instruction.Switch i) {
	    int[] target = i.getTargets(buf);
	    for (int idx = 0; idx < target.length; idx++)
		visit(buf.getPC(), target[idx] + buf.getPC());
	    visit(buf.getPC(), i.getDefaultTarget(buf) + buf.getPC());
	}

	public void visit(Instruction.WIDE i) {
	    i.specialize(buf).accept(this);
	}

	public void run(S3ByteCode code) {
	    buf = InstructionBuffer.wrap(code);
	    while (buf.hasRemaining())
		buf.get().accept(this);
	}
    }

    public S3Blueprint resolve(TypeName tn) {
	try {
	    Type t = tctx.typeFor(tn);
	    return (S3Blueprint) ctx.domain.blueprintFor(t);
	} catch (LinkageException e) {
	    throw e.unchecked();
	}
    }
    
    static String I(int i) {
	return (J2cImageCompiler.KEEP_LOGS
		? Integer.toString(i)
		: null);
    }
    final void log(Object o) {
	if (J2cImageCompiler.KEEP_LOGS) {
	    logBuffer.append(o);
	    logBuffer.append('\n');
	}
    }
    final void log(Object o1, Object o2) {
	if (J2cImageCompiler.KEEP_LOGS)
	    log(o1.toString() + o2);
    }
    final void log(Object o1, Object o2, Object o3) {
	if (J2cImageCompiler.KEEP_LOGS)
	    log(o1.toString() + o2 + o3);
    }
    final void log(Object o1, Object o2, Object o3, Object o4) {
	if (J2cImageCompiler.KEEP_LOGS)
	    log(o1.toString() + o2 + o3 + o4);
    }
    final void log(Object o1, Object o2, Object o3, Object o4,
			Object o5) {
	if (J2cImageCompiler.KEEP_LOGS)
	    log(o1.toString() + o2 + o3 + o4 + o5);
    }
    final void log(Object o1, Object o2, Object o3, Object o4,
			Object o5, Object o6) {
	if (J2cImageCompiler.KEEP_LOGS)
	    log(o1.toString() + o2 + o3 + o4 + o5 + o6);
    }

    void dumpLog(PrintStream stream, Throwable reason, String message) {
	if (message != null)
	    stream.println(message);
	if (J2cImageCompiler.KEEP_LOGS)
	    stream.print(logBuffer.toString());
	stream.println(code);
	if (reason != null)
	    reason.printStackTrace(stream);
    }
}
