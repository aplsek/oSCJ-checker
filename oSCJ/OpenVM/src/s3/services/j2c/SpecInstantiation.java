package s3.services.j2c;
import java.util.Arrays;
import java.util.StringTokenizer;

import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantResolvedInstanceMethodref;
import ovm.core.domain.ConstantResolvedStaticFieldref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.execution.NativeConstants;
import ovm.core.repository.Attribute;
import ovm.core.repository.ConstantPool;
import ovm.core.repository.Descriptor;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.JavaNames;
import ovm.core.repository.Repository;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.repository.Attribute.LineNumberTable;
import ovm.core.repository.Attribute.LocalVariableTable;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.MemoryManager;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.SpecificationIR.ArrayAccessExp;
import ovm.services.bytecode.SpecificationIR.ArrayLengthExp;
import ovm.services.bytecode.SpecificationIR.AssignmentExp;
import ovm.services.bytecode.SpecificationIR.BinExp;
import ovm.services.bytecode.SpecificationIR.BitFieldExp;
import ovm.services.bytecode.SpecificationIR.BlueprintAccessExp;
import ovm.services.bytecode.SpecificationIR.CPAccessExp;
import ovm.services.bytecode.SpecificationIR.CSACallExp;
import ovm.services.bytecode.SpecificationIR.CallExp;
import ovm.services.bytecode.SpecificationIR.ConcreteIntValue;
import ovm.services.bytecode.SpecificationIR.CondExp;
import ovm.services.bytecode.SpecificationIR.ConversionExp;
import ovm.services.bytecode.SpecificationIR.CurrentPC;
import ovm.services.bytecode.SpecificationIR.DoubleValue;
import ovm.services.bytecode.SpecificationIR.FieldAccessExp;
import ovm.services.bytecode.SpecificationIR.FloatValue;
import ovm.services.bytecode.SpecificationIR.IfCmd;
import ovm.services.bytecode.SpecificationIR.IfExp;
import ovm.services.bytecode.SpecificationIR.IntValue;
import ovm.services.bytecode.SpecificationIR.LinkSetAccessExp;
import ovm.services.bytecode.SpecificationIR.ListElementExp;
import ovm.services.bytecode.SpecificationIR.LocalExp;
import ovm.services.bytecode.SpecificationIR.LocalStore;
import ovm.services.bytecode.SpecificationIR.LongValue;
import ovm.services.bytecode.SpecificationIR.LookupExp;
import ovm.services.bytecode.SpecificationIR.MemExp;
import ovm.services.bytecode.SpecificationIR.NonnulRefValue;
import ovm.services.bytecode.SpecificationIR.NullRefValue;
import ovm.services.bytecode.SpecificationIR.ReinterpretExp;
import ovm.services.bytecode.SpecificationIR.SecondHalf;
import ovm.services.bytecode.SpecificationIR.ShiftMaskExp;
import ovm.services.bytecode.SpecificationIR.StreamableValue;
import ovm.services.bytecode.SpecificationIR.SymbolicConstant;
import ovm.services.bytecode.SpecificationIR.Temp;
import ovm.services.bytecode.SpecificationIR.UnaryExp;
import ovm.services.bytecode.SpecificationIR.Value;
import ovm.services.bytecode.SpecificationIR.ValueSource;
import ovm.services.bytecode.SpecificationIR.RootsArrayBaseAccessExp;
import ovm.services.bytecode.SpecificationIR.RootsArrayOffsetExp;
import ovm.services.bytecode.SpecificationIR.WideValue;
import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.Frame;
import ovm.services.bytecode.analysis.Heap;
import ovm.services.bytecode.analysis.State;
import ovm.services.bytecode.analysis.WorkList;
import ovm.services.bytecode.analysis.AbstractValue.WidePrimitive;
import ovm.util.BitSet;
import ovm.util.HashMap;
import ovm.util.OVMRuntimeException;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Constants;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Method;
import s3.services.bootimage.Analysis;
import s3.services.bytecode.ovmify.NativeCallGenerator;
import s3.services.bytecode.analysis.BranchTargetResolver;
import s3.services.bytecode.analysis.FixpointIterator;
import s3.services.bytecode.analysis.S3Frame;
import s3.services.bytecode.analysis.S3Heap;
import s3.services.bytecode.verifier.VerificationFixpointIterator;
import s3.services.bytecode.verifier.VerificationInterpreter;
import s3.services.bytecode.analysis.S3Frame;
import s3.services.bytecode.analysis.S3Heap;
import s3.services.bytecode.analysis.FixpointIterator;
import ovm.services.bytecode.analysis.WorkList;
import s3.services.j2c.J2cValue.*;
import s3.services.bootimage.Analysis;
import ovm.core.domain.Method;
import s3.services.bytecode.ovmify.NativeCallGenerator;
import ovm.services.bytecode.JVMConstants.Throwables;
import ovm.core.domain.InlinedAttribute;
import s3.core.domain.MachineSizes;
import ovm.core.services.format.CxxFormat; 
import ovm.core.services.memory.MemoryManager.Assert;

/**
 * J2c's abstract interpreter serves a number of functions.
 * <ul>
 *    <li> It transates each Instruction's
 *         {@link ovm.services.bytecode.SpecificationIR} to j2c's 
 *         slightly different {@link s3.services.j2c.J2cValue}
 *         representation.  This translation essentially treats the
 *         instruction specification as a template for the J2cValue
 *         code that is generated.  Hence, it instantiaties the spec.
 *    <li> It Dequickifies code by discovering the actual fields and
 *         methods referred to through _QUICK instructions.
 *    <li> It solves a number of forward dataflow problems, this is
 *         mostly a matter of inferring types.
 *    <li> It saves the result of it's type analysis at the program
 *         points requested by the caller.  (Typically, this will be
 *         the start of every basic block, along with any and all RET
 *         instructions.
 *    <li> It discover unreachable code, and unreachable catch handlers
 *    <li> It discovers which local variables are ever stored to
 *    <li> SpecInstantiation is also the place where we perform a
 *         wide variety of optimizations, including some of our most
 *         important ones.  These optimizations include:
 *         <ul>
 *            <li> typecheck elimination and simplification
 *            <li> devirtualization
 *         </ul>
 *         This really isn't an appropriate place for optimizations,
 *         though, and optimizations will be moved out of here on a
 *         gradual basis.
 * </ul>
 *
 * There are a number of problems with J2c, and most of them are
 * related to SpecInstantiation one way or another.
 * <ol>
 *    <li> J2c takes quickified input, and then unquickens it.  This
 *         is silly.
 *    <li> SpecInstantiation includes a great deal of
 *         constant-folding code that exists soley for the purpose of
 *         dequickification.  It would be much better to split
 *         SpecificationIR translation and constant folding into two
 *         seperate operations.
 *    <li> Optimizations such as typecheck stuff and devirtualization
 *         are performed over and over again because they are done
 *         here.  Even in a methd with no branches, SpecInstantiation
 *         will vist every instruction twice.
 * </ol>
 * This is a big complicated class, that actually accomplishes very
 * little.<p>
 *
 * Somewhere, the typing rules for VM_Address, int, and Object need
 * to be explained.  VM_Addresses show up as ints in fields, and
 * Objects in local variables, but we always need to treat them as
 * ints for the purpose of GC.  VM_Address and VM_Word are
 * represented very strangely: They are represented as J2cReference
 * values, but their flavor fields contain LocalAllocator.INT_VAR.
 * Bytecode-level locals are declared as jint, but bytecode-level
 * stack slots are declared as VM_1Address *.
 *
 * @author <a href="mailto://baker29@cs.purdue.edu"> Jason </a>
 */
public class SpecInstantiation extends VerificationInterpreter
    implements JVMConstants
{
    HashMap valueMap = new HashMap();
    Temp[] temps = new Temp[2];	  // FIXME: max temps + 1
    J2cValue[] evals = new J2cValue[4]; // FIXME: max evals + 1

    S3Blueprint.Scalar homeBp;
    final S3Method s3Method;
    final S3Constants dcp;
    final TryBlock tryBlock;
    final BitSet statefulInstructions;
    final BitSet reachable;
    final BitSet storedVariables;
    
    final J2cNumeric j2cDomNum;

    final Context ctx;
    final MethodCompiler mc;
    
    final LineNumberParser parser;
    final LocalVariableTable vars;

    String getVariableName(int idx, int pc) {
        String ret = null;
        if (vars != null) {
	    int utf = vars.getVariableNameIndex(idx, pc);
	    if (utf != -1)
		ret = J2cFormat.getUTF(utf);
	    else
		mc.lookupFailure = true;
	}
        return ret == null ? ("_local" + idx) : J2cFormat.encode(ret);
    }
    
    int getLine() {
	return parser.getLineNumber(getPC());
    }
    String getSourceFile() {
	return parser.getSourceFile(getPC());
    }

    static abstract class LineNumberParser {
	public abstract int getLineNumber(int pc);
	public abstract String getSourceFile(int pc);
    }
    
    private static String neuterFileName(String file) {
	for(int i = 0; i < file.length(); i++) {
	    char c = file.charAt(i);
	    if (Character.isISOControl(c))
		file = file.replace(c, ' ');
	}
	return file;
    }

    static String makeFilename(Method m) {
	// FIXME QUICK: did we cache fully qualified names?
	Type.Scalar t = m.getDeclaringType().asScalar();
	TypeName.Scalar tn = t.getName().asScalar();
	int idx = t.getSourceFileNameIndex();
	String srcFileName =
	    (idx != 0
	     ? J2cFormat.getUTF(idx)
	     : J2cFormat.getUTF(tn.getShortNameIndex()) + ".java");
	String dir = J2cFormat.getUTF(tn.getPackageNameIndex());
	return dir + "/" + neuterFileName(srcFileName);
    }

    LineNumberParser makeLineNumberParser(final Method m,
					  final LineNumberTable table,
					  final InlinedAttribute inlined)
    {
	final String fqName = makeFilename(m);
	if (table == null) {
	    return new LineNumberParser() {
		    public int getLineNumber(int _) {
			return 0;
		    }
		    public String getSourceFile(int _) {
			return fqName;
		    }
		};
	} else if (inlined != null) {
	    return new LineNumberParser() {
		    String[] methodFileNames = new String[inlined.size() + 1];

		    public int getLineNumber(int pc) {
			return table.getLineNumber(pc);
		    }
		    public String getSourceFile(int pc) {
			int i = inlined.getInnerMostMethodIndex(pc) + 1;
			if (methodFileNames[i] == null)
			    methodFileNames[i] =
				makeFilename(i == 0
					     ? m
					     : inlined.getMethod(i-1));
			return methodFileNames[i];
		    }
		};
	} else if (fqName.indexOf(';') != -1) {
	    // AspectJ line numbers: source file format is
	    // <original-name>{;<fully-qualified-name>(<offset/1000>k)}*
	    StringTokenizer tok = new StringTokenizer(fqName, ";");
	    int nf = tok.countTokens();
	    final int[] offset = new int[nf];
	    final String[] file = new String[nf];
	    offset[0] = 0;
	    file[0] = tok.nextToken();
	    for (int i = 1; i < nf; i++) {
		String nameAndOffset = tok.nextToken();
		int pivot = nameAndOffset.indexOf('(');
		file[i] = neuterFileName(nameAndOffset.substring(0, pivot));
		int off = 0;
		for (int j = pivot + 1;
		     nameAndOffset.charAt(j) != 'k';
		     j++)
		    off = 10*off + (nameAndOffset.charAt(j) - '0');
		offset[i] = 1000 * off;
	    }
	    return new LineNumberParser() {
		    public int getLineNumber(int pc) {
			int ajLine = table.getLineNumber(pc);
			int next = 1;
			while (next < offset.length) {
			    if (offset[next] > ajLine)
				break;
			    next++;
			}
			return ajLine - offset[next - 1];
		    }
		    public String getSourceFile(int pc) {
			int ajLine = table.getLineNumber(pc);
			int next = 1;
			while (next < offset.length) {
			    if (offset[next] > ajLine)
				break;
			    next++;
			}
			return file[next - 1];
		    }
		};
	} else {
	    return new LineNumberParser() {
		    public int getLineNumber(int pc) {
			return table.getLineNumber(pc);
		    }
		    public String getSourceFile(int _) {
			return fqName;
		    }
		};
	}
    }

    public J2cValue[] getIns() { return (J2cValue[]) inputRegister; }
    public J2cValue[] getOuts() { return (J2cValue[]) outputRegister; }

    // FIXME: We can't get an upper bound on the stack height
    // of rewritten code until the dangling symbolic
    // references are cleaned up.
    public int getMaxStack() {
        S3ByteCode b = s3Method.getByteCode();
        return (b.getMaxStack() + 3) * 2;
    }
    public int getMaxLocals() {
        S3ByteCode b = s3Method.getByteCode();
        return b.getMaxLocals() + 1;
    }
    
    SpecInstantiation(MethodCompiler mc,
		      BitSet statefulInstructions) {
	super(InstructionBuffer.wrap(mc.code),
	      J2cValue.bogusFactory);
	this.mc = mc;
	this.tryBlock = mc.tryBlock;
	this.statefulInstructions = statefulInstructions;
	this.ctx = mc.ctx;
	this.j2cDomNum =
	    J2cValue.makeIntConstant(TypeCodes.INT,
				     this.ctx.domain.getUID());
	S3Method m = s3Method = mc.meth;
	inputRegister = new J2cValue[inputRegister.length];
	outputRegister = new J2cValue[outputRegister.length];
	S3ByteCode cf = mc.code;
	reachable = new BitSet(mc.code.getBytes().length);
	// FIXME: not in Method, overly wide return type?
	Type.Scalar declaringType = (Type.Scalar) m.getDeclaringType();
	homeBp = (S3Blueprint.Scalar) blueprintFor(declaringType);
	if (homeBp.isSharedState())
	    homeBp = (S3Blueprint.Scalar) homeBp.getInstanceBlueprint();
	dcp = (S3Constants) cf.getConstantPool();
	storedVariables = new BitSet(getMaxLocals());

        Attribute[] att = cf.getAttributes();
        vars = (LocalVariableTable) getAttribute(ctx, "LocalVariableTable", att);
// 	if (vars != null)
// 	    System.err.println(m.getSelector() + ": " + vars);
	LineNumberTable lineNo = 
            (LineNumberTable) getAttribute(ctx, "LineNumberTable", att);
	InlinedAttribute inlined =
	    (InlinedAttribute) getAttribute(ctx, "org.ovmj.Inlined", att);
	parser = makeLineNumberParser(mc.meth, lineNo, inlined);
    }

    Attribute getAttribute(Context _, String name, Attribute[] att) {
        int idx = Context.getUTF(name);
        for (int i = 0; i < att.length; i++)
            if (att[i] != null && att[i].getNameIndex() == idx) {
        	return att[i];
            }
        return null;
    }

    public class SIFrame extends S3Frame {
	public Frame make(AbstractValue[] opStack,
			  AbstractValue[] locals,
			  int stackHeight,
			  Heap heap) {
	    return new SIFrame(opStack, locals, stackHeight, heap);
	}
	public SIFrame(int maxStack, int maxLocals,
		       Heap heap) {
	    super(maxStack, maxLocals, heap);
	}
	private SIFrame(AbstractValue[] opStack,
			AbstractValue[] locals,
			int stackHeight,
			Heap heap) {
	    super(opStack, locals, stackHeight, heap);
	}
	public void store(int index, AbstractValue v) {
	    super.store(index, (v == null
				? null
				: ((J2cValue) v).allocate(getPC(), index,
							  J2cValue.LOCAL)));
	}
    }

    private static final S3Heap.Factory hfactory 
	= new S3Heap.Factory();
    // A renaming in verifier caused a clash here...
    public class J2cFixpointIterator extends VerificationFixpointIterator {
	public J2cFixpointIterator() {
	    super(J2cValue.bogusFactory,
		  SpecInstantiation.this,
		  s3Method,
		  InstructionBuffer.wrap(s3Method.getByteCode()));
	}

	/**
	 * Get the next Instruction and update the Frame and PC of the AbstractInterpreter.
	 */
	public Instruction next() {
	    Frame f = ai.getFrame();
	    if (f != null) 
		f.setLastPC(ai.getPC());
	    WorkList.Entry curr = null;
	    while (worklist.hasNext()) {
		curr = worklist.remove();
		if (curr.tryMerge ==WorkList.NOMERGE
		    && !statefulInstructions.get(curr.pc))
		    // Force merge of initial frame at PC 0 even
		    // though add() is not called on this frame
		    break;
		f = merge(curr.frame, 
			  curr.tryMerge == WorkList.MERGECLONE, 
			  curr.pc);
		if (f == null) {
		    curr = null; // do not simulate this one again
		}
		else {
		    curr.frame = f;
		    break;
		}
	    }
	    if (curr == null)
		return null;
	    ai.setFrameAndPC(curr.frame, curr.pc);
	    return buf.get(curr.pc);
	}
	
	
	public void add(int pc, Frame f, int merge) {
	    // Treat the next instruction after an try block as the
	    // start of a basic block
	    super.add(pc, f, ((merge == WorkList.NOMERGE
			       && statefulInstructions.get(pc))
			      ? WorkList.MERGECLONE
			      : merge));
	}

	public State getState() { return state; }

	public Frame makeInitialFrame() {
	    Frame f = new SIFrame(getMaxStack(), getMaxLocals(),
                  		  hfactory.makeHeap());
	    int pos = 0;
	    int cnt = 0;
	    int argCount = s3Method.getArgumentCount();
	    if (!s3Method.getMode().isStatic())
		f.store(pos++,
			new J2cReference(null, homeBp, false, true));
	    else
		f.store(pos++,
			new SharedStateReference
			(null,  homeBp.getSharedState()));
	    while (cnt < argCount) {
		try {
		    Type t = s3Method.getArgumentType(cnt++);
		    f.store(pos++, makeValue(null, t, false));
		    if (t.isWidePrimitive())
			f.store(pos++, null);
		} catch (LinkageException e) { throw e.unchecked(); }
	    }
	    // state.addFrameAt(0, f.cloneFrame());
	    return f;
	}

	public class Merger 
	    extends FixpointIterator.MergeStrategyVisitor {
	    public void visit(Instruction i) {
		if (i instanceof Instruction.ExceptionThrower) 
		    addAllExceptionTargets((Instruction.ExceptionThrower)i);

		if (executionTerminates) {
		    executionTerminates = false;
		    return;
		}

		int npc = ai.getPC()+i.size(ai.getPosition());
		add(npc, ai.getFrame(), WorkList.NOMERGE);
	    }
	}

	public class NoExceptionMerger extends Merger {
	    protected void addAllExceptionTargets(Instruction.ExceptionThrower _) {
	    }
	}

	public class ExceptionMerger extends Merger
	    implements TryBlock.HandlerVisitor {
	    TryBlock inner;
	    int pc;		// FIXME: should be local
	    Frame f;
	    S3Blueprint thrown;

	    // propagate throws accurately, and with a real J2cValue
	    protected void addAllExceptionTargets(Instruction.ExceptionThrower i) {
		f = ai.getFrame();
		TypeName.Scalar[] ex = i.getThrowables();
		/*int*/pc = ai.getPC();
		TryBlock t;
		if (inner == null
		    || inner.startPC > pc
		    || inner.endPC <= pc)
		    // remember last block, and avoid search if possible
		    inner = tryBlock.findInnerMost(pc);
		else if (inner.inner != null)
		    // speed up search when the current innermost
		    // handler may be nested inside the last
		    inner = inner.findInnerMost(pc);

		if (inner == null)
		    return;
		for (int j=0;j<ex.length;j++) {
		    thrown = mc.resolve(ex[j]);
		    inner.forApplicableHandlers(mc.resolve(ex[j]),
						this, ctx);
		}
	    }

	    public void visit(TryBlock.Handler h) {
		h.reachable = true;
		S3Blueprint caught = h.bp;
		f = f.cloneFrame();
		f.clearStack();
		f.push(new J2cReference
		       (null,
			(thrown.isSubtypeOf(caught)
			 ? thrown
			 : caught),
			false,
			false));
  		mc.log(thrown, " props from ", I(pc), " to ", I(h.PC));
		add(h.PC, f, WorkList.MERGE);
	    }
	}

	public FixpointIterator.MergeStrategyVisitor makeStrategyVisitor() {
	    return (tryBlock == null
		    ? (Merger) new NoExceptionMerger()
		    : (Merger) new ExceptionMerger());
	}
    }

    // Must be given a name to be public
    public J2cFixpointIterator getFixpointIterator() {
	return new J2cFixpointIterator();
    }

    /*
     * If NULL flows into the receiver position for an invocation or
     * field read, we can't continue analyzing the current basic
     * block, because we don't know what to put on the stack.  We
     * discover this situation somewhere deep in the recursive
     * ValueSource walk, and are left to deal with the consequences
     * later on.
     */

    boolean executionTerminates = false;
    Instruction cur;

    public void visitAppropriate(Object o, Class c) {
	valueMap.clear();
	reachable.set(getPC());
	// FIXME: J2c abstract interpretation weirdness
	//
	// With a certain level of inlining, we can no longer rely on
	// this flag being cleared by the MergeStrategyVisitor.
	// Basically, I'm seeing the following: S3Executive.panic()
	// gets inlined, and it ends in `throw null;', for some
	// reason, the MergeStrategyVisitor does not run, and when we
	// consider the first instruction following the
	// `if (???) panic()' statement, we conclude that it also
	// crashes.  I can no longer remember when the
	// MergeStrategyVisitor is called or why.  Fucked up.
	//
	// This is probably the better place to clear
	// executionTerminates anyway.
	//
	// If j2c took unquicked input, it could happily continue past
	// all these unconditional crashes and handle type-unsafe
	// inlining to boot...
	executionTerminates = false;

	// Align the current instruction here and in the
	// InstructionBuffer.
	// FIXME: why is this only needed in j2c?
	buf.get(getPC());
	cur = (Instruction) o;
	log("initial frame = ", getFrame());
	Arrays.fill(inputRegister, J2cValue.TERMINATOR);
	Arrays.fill(outputRegister, J2cValue.TERMINATOR);
	Arrays.fill(evals, J2cValue.TERMINATOR);
	super.visitAppropriate(o, c);
    }

    String where(String message) {
	String sel =  s3Method.getSelector().toString();
	String name = cur.getName();
	return("J2C: " + sel + ": " + name
	       + "@" + getPC()
	       + " (line " + getSourceFile() + ":" + getLine() + "): "
	       + message);
    }
    static String I(int i) {
	return (J2cImageCompiler.KEEP_LOGS
		? Integer.toString(i)
		: null);
    }
    void log(String message) {
	if (J2cImageCompiler.KEEP_LOGS)
	    mc.log(where(message));
    }
    final void log(Object o1, Object o2, Object o3) {
	if (J2cImageCompiler.KEEP_LOGS)
	    log(o1.toString() + o2 + o3);
    }
    final void log(Object o1, Object o2) {
	if (J2cImageCompiler.KEEP_LOGS)
	    log(o1.toString() + o2);
    }

    Error die(String message) { return new Error(where(message));  }
    int iDie(String message)            { throw die(message); }
    AbstractValue avDie(String message) { throw die(message); }
    String sDie(String message)         { throw die(message); }

    S3Method devirtualize(S3Blueprint receiver, Method m) {
	try {
	    return (ctx.anal != null
		    ? (S3Method) ctx.anal.getTarget(receiver, m)
		    : null);
	} catch (ArrayIndexOutOfBoundsException e) {
	    System.err.println("can't find " + m + " in table for "
			       + receiver);
	    throw e;
	}
    }
    int evalCast(S3Blueprint from, S3Blueprint to) {
 	return (ctx.anal != null
 		? ctx.anal.evalCast(from, to)
 		: Analysis.UNKNOWN);
    }
    Blueprint[] concreteSubtypesOf(Blueprint bp) {
	return (ctx.anal != null
		? ctx.anal.concreteSubtypesOf(bp)
		: (bp.isArray() && elementBP(bp) instanceof Blueprint.Primitive
		   ? new Blueprint[] { bp }
		   : null));
    }

    S3Blueprint elementBP(Blueprint bp) {
	while (bp.isArray())
	    bp = bp.asArray().getComponentBlueprint();
	return (S3Blueprint) bp;
    }

    static int subtypeEq = 0;
    static int subtypeArray = 0;
    static int subtypeScalar = 0;
    static int subtypeFull = 0;

    /*
    static {
	new J2cImageCompiler.StatPrinter() {
	    public void printStats() {
		System.err.println("isSubtypeOf optimization");
		System.err.println("All numbers are somewhat more than "
				   + "twice as large as they should be");
		System.err.println("\t" + subtypeEq
				   + " checks for pointer equality");
		System.err.println("\t" + subtypeArray
				   + " array subtype tests");
		System.err.println("\t" + subtypeScalar
				   + " scalar object subtype tests");
		System.err.println("\t" + subtypeFull
				   + " full subtype tests");
	    }
	};
    }
    */

    S3Blueprint resolveClassAt(int idx) {
	try {
	    return (S3Blueprint) dcp.resolveClassAt(idx);
	} catch (LinkageException e) {
	    throw e.unchecked();
	}
    }

    public class J2cCanonicalizer extends Canonicalizer {
	protected Value origin;
	protected ValueSource source;

	// Methods for converting a SpecificationIR.Value to a
	// J2cValue.  These methods are called after we have tried to
	// constant-fold the value and computed its new ValueSource in
	// source
	public void visit(ConcreteIntValue v) {
	    if (source != null) {
		System.err.println("concrete int has source " + source);
		result = new J2cInt(source, v.getType(), v.intValue());
	    } else {
		result = J2cValue.makeIntConstant(v.getType(), v.intValue());
	    }
	}
	public void visit(IntValue v) {
	    result = new J2cInt(source, v.getType());
	}
	public void visit(LongValue v) {
	    result = new J2cLong(source, (Number) v.concreteValue(),
				 v.getType());
	}
	public void visit(FloatValue v) {
	    result = new J2cFloat(source, (Number) v.concreteValue());
	}
	public void visit(DoubleValue v) {
	    result = new J2cDouble(source, (Number) v.concreteValue());
	}
	public void visit(NullRefValue v) {
	    result = J2cValue.NULL;
	}
	public void visit(J2cValue v) {
	    result = v;
	}
	public void visit(SecondHalf v) {
	    result = null;
	}

	public void visit(Value v) {
	    assert(source != null);
	    result = new J2cVoid(source);
	}
	// Methods for simplifying SpecificationIR.ValueSource
	// expressions and replacing SpecificationIR operands with
	// J2cValue operands.  If we are able to simplyfy the
	// expression, set the AbstractValue field result to the
	// simplified value, otherwise, store a recursively tranformed
	// copy of the ValueSource object in source.
	public void visit(CurrentPC exp) {
	    // Is ths only called during jsr?  If I implement ret as a
	    // computed goto, I need to put a label here

	    result = J2cValue.makeIntConstant(TypeCodes.INT, getPC());
	}
	public void visit(CPAccessExp exp) {
	    J2cValue value = j2cCanonicalize(exp.value);
	    if (value.isConcrete()) {
		int idx = ((J2cInt) value).intValue();
		byte tag = dcp.getTagAt(idx);
		try {
		    switch (tag) {
		    case CONSTANT_SharedState:
			TypeName.Gemeinsam tn = (TypeName.Gemeinsam) dcp.getConstantAt(idx);
			Type.Context tc = dcp.getType().getContext();
			TypeName.Compound bpn = tn.getInstanceTypeName().asCompound();
			Blueprint ibp;
			
			try {
			    ibp = ctx.domain.blueprintFor(tc.typeFor(bpn));
			} catch ( LinkageException le ) {
			    tc = ctx.domain.getApplicationTypeContext();
			    ibp = ctx.domain.blueprintFor(tc.typeFor(bpn));
			}
			Oop oops =  ibp.getSharedState();
			result = new SharedStateReference(null, oops);
			break;
		    case CONSTANT_ResolvedClass:
			result = new BlueprintReference(resolveClassAt(idx));
			break;
		    case CONSTANT_Reference:
		    case CONSTANT_Binder:
			Oop oops2 = dcp.resolveConstantAt(idx);
			result =  (oops2.getBlueprint().isSharedState()
				   ? new SharedStateReference(null, oops2)
				   : new ConcreteScalar(null, oops2));
			break;
		    case CONSTANT_String:
			RepositoryString rs1
			    = (RepositoryString) dcp.getConstantAt(idx);
			VM_Address rs
			    = VM_Address.fromObject(rs1,
						    ctx.RepositoryStringBP);
			result = new ConcreteScalar(null, rs.asOop(),
						    ctx.stringBP);
			break;
		    case CONSTANT_Integer:
			result = J2cValue.makeIntConstant(TypeCodes.INT, dcp.getValueAt(idx));
			break;
		    case CONSTANT_Long:
			result = new J2cLong(null, (Number) dcp.getConstantAt(idx), TypeCodes.LONG);
			break;
		    case CONSTANT_Float:
			result = new J2cFloat(null, (Number) dcp.getConstantAt(idx));
			break;
		    case CONSTANT_Double:
			result = new J2cDouble(null, (Number) dcp.getConstantAt(idx));
			break;
		    default:
			throw die("unknown cp tag " + tag);		    
		    }		
		} catch (ConstantPool.AccessException e) {
		    throw new OVMRuntimeException("couldn't get "
						  + tag + " constant "
						  + dcp.getConstantAt(idx),
						  e);
		} catch (LinkageException e) {
		    throw new OVMRuntimeException("couldn't get "
						  + tag + " constant "
						  + dcp.getConstantAt(idx),
						  e);
		}
	    }
	    else {
		mc.log("unresolved constant pool index ", value);
		source = new CPAccessExp(value);
	    }
	}
	public void visit(SymbolicConstant exp) {
	    source = exp;
	}
	// FIXME: quickened IR doesn't generate FieldAccessExps, why do
	// I bother visiting them?  I need to convert this to a
	// J2cFieldAccessExp anyway!
	// If it is a static final field with a know value, create a
	// J2cValue with the appropriate static information
	public void visit(FieldAccessExp exp) {
	    J2cValue obj = j2cCanonicalize(exp.obj);
	    source =  new FieldAccessExp(exp.selector, obj);
	}
	// umm...  index can certainly be evaled away.  If the method
	// can be be devirtualized, replace the whole thing with the
	// method's j2cName.
	public void visit(LookupExp exp) {
	    // I'm no longer certain 
	    J2cValue bpv = j2cCanonicalize(exp.bp);
	    J2cValue idx = j2cCanonicalize(exp.index);

	    // If this is an interface lookup on a monotonic
	    // interface, make it a virtual lookup
	    if (!idx.isConcrete())
		throw die("lookup variable index " + idx);
	    int index = ((J2cInt)idx).intValue();

	    S3Blueprint bp = ((BlueprintReference) bpv).blueprintValue();
	    S3Method m = bp.lookupMethod(index, exp.tableName);
	    if (m == null)
		throw new LinkageException
		   ("no method found at " + bp + "." + exp.tableName
		    + "[" + index + "]").unchecked();

	    boolean virtual = !bpv.isConcrete();
 	    if (virtual && devirtualize(bp, m) != null) {
 		virtual = false;
 		m = devirtualize(bp, m);
 	    }
	    if (virtual) {
		source =  new J2cLookupExp(bpv, exp.tableName, idx);
		result = new MethodReference(source, m);
		mc.log("virtual call to ", m);
	    } else {
		result = J2cValue.makeNonvirtualReference(m);
		mc.log(bpv.isConcrete() ? "nonvirtual " : "devirtualized ",
			 J2cFormat.format(m));
	    }
	}
	// should be evaled away
	public void visit(LinkSetAccessExp exp) {
	    // Link sets are gone now.
	    throw new Error();
	}
	// What's this?  It is only used in TABLESWITCH.  I would much
	// rather implement both switch flavors in terms of C++ switch
	// statements.  On the other hand, it is hard to simplify an
	// atomic expression corresponding to a C++ switch statement!
	public void visit(ListElementExp exp) {
	    // exp.list the array of PC values at the end of a
	    // TABLESWITCH instruction.

	    // exp.index is the switch argument

	    // If we can't deal with TABLESWITCH directly, we might as
	    // well compile it down to a computed goto.  And, if we
	    // can't compile it down to a computed goto, we might as
	    // well deal with it directly.

	    J2cValue index = j2cCanonicalize(exp.index);
	    if (index.isConcrete())
		mc.log("I know the switch target, but can't do anything about it");
	    source = new ListElementExp(exp.list, index);
	}

	// umm...I guess I will see these
	public void visit(Temp exp) {
	    result = j2cCanonicalize(exp.init);
	    //source = new Temp(exp.type, exp.name, j2cCanonicalize(exp.init));
	}
	// Should these be left standing, or replaced with references
	// into the frame?  They should certainly pick up type
	// information from the frame!
	public void visit(LocalExp exp) {
	    J2cInt v = (J2cInt) j2cCanonicalize(exp.number);
           // int idx = v.intValue();
            source = J2cValue.makeLocalExp(v.intValue());
	}

	public void visit(J2cLocalExp exp) {
	    source = exp;
	}

	public void visit(BinExp exp) {
	    J2cValue lhs = asPrimitive(exp.lhs);
	    J2cValue rhs = asPrimitive(exp.rhs);

	    char lcode = 0xffff;
	    if (exp.lhs instanceof IntValue)
		lcode = ((IntValue) exp.lhs).getType();
	    if (exp.lhs instanceof LongValue)
		lcode = ((LongValue) exp.lhs).getType();
	    else if (exp.lhs instanceof J2cInt)
		lcode = ((J2cInt) exp.lhs).typeCode;
	    else if (exp.lhs instanceof J2cLong)
		lcode = ((J2cLong) exp.lhs).typeCode;
	    
	    if (origin instanceof IntValue) {
		IntValue iorigin = (IntValue) origin;
		J2cInt ilhs = (J2cInt) lhs;
		J2cInt irhs = (J2cInt) rhs;
		if (lhs.isConcrete() && rhs.isConcrete()) {
		    int lv = ilhs.intValue();
		    int rv = irhs.intValue();
		    String op = exp.operator;
		    if ((op == "/" || op == "%") && rv == 0)
			/* don't simplify */;
		    else {
			int value = (op == "+" ? lv + rv
				     : op == "-" ? lv - rv
				     : op == "*" ? lv * rv
				     : op == "/" ? lv / rv
				     : op == "%" 
				       ? lv % rv
				     : op == "|" ? (lv | rv)
				     : op == "&" ? (lv & rv)
				     : op == "^" ? (lv ^ rv)
				     : op == "<<" ? lv << rv
				     : (op == ">>"
					? (lcode == TypeCodes.UINT
					   ? (lv >>> rv)
					   : (lv >> rv))
					: iDie("unkonwn operator " + op)));
			result = J2cValue.makeIntConstant(iorigin.getType(), value);
			mc.log("constant folding ", exp);
			return;
		    }
		} else if (lhs.isConcrete() || rhs.isConcrete()) {
		    // Handle some important cases in which we can fix
		    // a tighter bound on input expressions.  We must
		    // be careful not to introduce infinite loops into
		    // the abstract interpereter
		    int known = (lhs.isConcrete()
				 ? ilhs.intValue()
				 : irhs.intValue());
		    J2cInt unknown = (lhs.isConcrete() ? irhs : ilhs);
		    String op = exp.operator;
                    if (op == "&" && known == 0) {
                        result = J2cValue.makeIntConstant(TypeCodes.INT, 0);
                    } else if (op == "&" && (known & 0x80000000) == 0) {
			// int x;  x & 5 => J2cInt[0,5]
			// which is what we want, but
			// char x; (int) x & 0x10000 => J2cInt[0,65535]
			// when it is really 0.
			result = new J2cInt(new BinExp(ilhs, "&", irhs),
					    iorigin.getType(),
					    0, known);
			mc.log("bounding size of ", result);
			return;
		    }
		    else if (op == ">>" && unknown == ilhs) {
			// We must beware of sign change!
			int min = (lcode == TypeCodes.UINT
				   ? unknown.min >> known
				   : unknown.min >>> known);
			int max = unknown.max >> known;
			if (lcode == TypeCodes.UINT)
			    lhs = new J2cInt(new ConversionExp(lhs), lcode);
			result = new J2cInt(new BinExp(lhs, ">>", rhs),
					    iorigin.getType(),
					    Math.max(Math.min(0, min),
						     ilhs.min),
					    Math.min(Math.max(min, max),
						     ilhs.max));
			mc.log("bounding size of ", result);
			return;
		    }
		    else if (op == "%" 
		        && unknown == ilhs) {
			int max = Math.abs(known) - 1;
			int min = ilhs.min >= 0 ? 0 : -max;
			result = new J2cInt(new BinExp(ilhs, "%", 
			                      irhs),
					    iorigin.getType(),
					    min, max);
			mc.log("bounding size of ", result);
			return;
		    }
		}
	    }

	    if (lcode == TypeCodes.UINT)
		lhs = new J2cInt(new ConversionExp(lhs), lcode);
	    else if (lcode == TypeCodes.ULONG)
		lhs = new J2cLong(new ConversionExp(lhs), null, lcode);

	    source = new BinExp(lhs, exp.operator, rhs);
	}
	public void visit(UnaryExp exp) {
	    if (exp.operator == "&")
		source = new UnaryExp(exp.operator, j2cCanonicalize(exp.arg));
	    else {
		J2cValue arg = asPrimitive(exp.arg);
		if (exp.operator == "!" && arg.isConcrete()) {
		    int v = ((J2cInt) arg).intValue();
		    result = J2cValue.makeIntConstant(TypeCodes.INT,
						      v == 0 ? 1 : 0);
		    mc.log("!", arg, " => ", result);
		} else {
		    if (exp.operator == "!")
			mc.log("not a constant: ", arg);
		    source = new UnaryExp(exp.operator, arg);
		}
	    }
	}
	public void visit(ConversionExp exp) {
	    J2cValue before = j2cCanonicalize(exp.before);
	    // Hmm.  Is this safe?   I certainly need to handle
	    // conversions of contants...
	    if (before instanceof J2cInt && origin instanceof IntValue) {
		J2cInt bint = (J2cInt) before;
		char code = ((IntValue) origin).getType();
		if (bint.isConcrete() &&  bint.inRange(code))
		{
		    result = J2cValue.makeIntConstant(code, before.intValue());
		    mc.log("constant type conversion ", result);
		    return;
		} else if (code == TypeCodes.INT) {
		    // FIXME: what exactly does this accomplish?
		    result = new J2cInt(before.source, code,
					bint.min, bint.max);
		    return;
		}
	    }
		source = new ConversionExp(before);
	}
	public void visit(ReinterpretExp exp) {
	    source = new ReinterpretExp(j2cCanonicalize(exp.before));
	}
	public void visit(ShiftMaskExp exp) {
	    if (true || NativeConstants.SHIFTS_NEED_MASK)
		source = new ShiftMaskExp(j2cCanonicalize(exp.exponent),
					  j2cCanonicalize(exp.sizeType));
	    else
		// Don't bother allocating a no-op expression
		result = j2cCanonicalize(exp.exponent);
	}
	// Why isn't this an assignment to a LocalAccess?  Oh well,
	// things may be easier this way...
	public void visit(LocalStore exp) {
	    source = new LocalStore(j2cCanonicalize(exp.index),
				    j2cCanonicalize(exp.value));
	}
	public void visit(AssignmentExp exp) {
	    J2cValue dest = j2cCanonicalize(exp.dest);
	    J2cValue src = j2cCanonicalize(exp.src);

	    // FIXME: VM_Address.putFloat(), VM_Address.putDouble()
	    // are defined in terms of PUTFIELD_QUICK and
	    // PUTFIELD2_QUICK, which operate on ints and longs.
	    if (dest.source instanceof MemExp
		&& dest instanceof J2cInt
		&& src instanceof J2cFloat)
		dest = new J2cFloat(dest.source, null);
	    else if (dest.source instanceof MemExp
		     && dest instanceof J2cLong
		     && src instanceof J2cDouble)
		dest = new J2cDouble(dest.source, null);
	    source = new AssignmentExp(dest, src);
	}
	// FIXME: I need to contant-fold these tests if I want to get
	// anywhere.  Note that some CondExps (bounds checks in
	// particular) use unsigned comparisons
	public void visit(CondExp exp) {
	    // Handle signed comparisons on VM_Word and VM_Address
	    if (exp.operator == "<" || exp.operator == "<="
		|| exp.operator == ">=" || exp.operator == ">"
		|| exp.lhs.isPrimitive())
	    {
		J2cValue lhs = asPrimitive(exp.lhs);
		J2cValue rhs = asPrimitive(exp.rhs);
		
		if (lhs.isConcrete() && (lhs instanceof J2cInt) &&
		    rhs.isConcrete() && (rhs instanceof J2cInt))
		{
		    J2cInt li = (J2cInt) lhs;
		    J2cInt ri = (J2cInt) rhs;
		    boolean v;
		    if (li.typeCode == TypeCodes.UINT) {
			long lv = li.intValue() & 0xffffffffL;
			long rv = ri.intValue() & 0xffffffffL;
			v = (exp.operator == "<"  ? (lv < rv)  :
			     exp.operator == "<=" ? (lv <= rv) :
			     exp.operator == ">=" ? (lv >= rv) :
			     exp.operator == ">"  ? (lv > rv)  :
			     exp.operator == "==" ? (lv == rv) :
			                            (lv != rv));
		    } else {
			int lv = li.intValue();
			int rv = ri.intValue();
			v = (exp.operator == "<"  ? (lv < rv)  :
			     exp.operator == "<=" ? (lv <= rv) :
			     exp.operator == ">=" ? (lv >= rv) :
			     exp.operator == ">"  ? (lv > rv)  :
			     exp.operator == "==" ? (lv == rv) :
			                            (lv != rv));
		    }
		    result = J2cValue.makeIntConstant(TypeCodes.INT,
						      v ? 1 : 0);
		    mc.log(lhs, exp.operator, rhs, " => ", result);
//                    System.err.println("CMP0: "+lhs+" "+rhs);
		} else {
//                    System.err.println("CMP1: "+lhs+" "+rhs);		
		    source = new CondExp(lhs, exp.operator, rhs);
                }
	    } else if ((exp.operator == "==" || exp.operator == "!=")
		       && exp.lhs.isReference()
		       && exp.rhs.isReference()) {
		J2cValue lhs = asAddress(exp.lhs);
		S3Blueprint lbp = lhs.getBlueprint(ctx.domain);
		J2cValue rhs = asAddress(exp.rhs);
		S3Blueprint rbp = rhs.getBlueprint(ctx.domain);
		
		if (lhs != J2cValue.NULL
		    && rhs != J2cValue.NULL
		    && !lbp.getType().isInterface()
		    && !rbp.getType().isInterface()
		    && !lbp.isSubtypeOf(rbp)
		    && !rbp.isSubtypeOf(lbp)
		    && lbp != ctx.OopBP
		    && lbp != ctx.VM_AddressBP
		    && rbp != ctx.OopBP
		    && rbp != ctx.VM_AddressBP)
		{
		    System.err.println(where("useless pointer comparison"));
		    result = J2cValue.makeIntConstant
			(TypeCodes.BOOLEAN, exp.operator == "==" ? 0 : 1);
		} else {

		    // zero, one, or both of the arguments can be VM_Address

                    if ( ((rbp == ctx.VM_AddressBP) != (lbp == ctx.VM_AddressBP))
                      && lhs != J2cValue.NULL && rhs != J2cValue.NULL ) {
                      System.err.println("Comparison between VM_Address and non-VM_Address, where neither is null.  Better rewrite it manually...:" + lbp + "( " +lhs + " ) "+
                        exp.operator + " "+ rbp + "( "+rhs+" )");
                        throw (new LinkageException("please fix")).unchecked();
                    }
		    

		    // fix comparison of object reference and something else (array)...
                    // cannot used lhs/rhs directly, because integers are already represented
                    // as scalars, despite neededing a cast
                    
                    boolean riss = j2cCanonicalize(exp.rhs).getBlueprint(ctx.domain).isScalar();
                    boolean liss = j2cCanonicalize(exp.lhs).getBlueprint(ctx.domain).isScalar();
		    
		    if (liss && !riss) {
		        rhs = new InternalReference(new CCastExp("HEADER *",rhs));
                    } else if (riss && !liss) {
                        lhs = new InternalReference(new CCastExp("HEADER *",lhs));
                    }

//                    System.err.println("CMP2: "+lbp+" "+rbp);
                    
                    source = new CondExp(lhs, exp.operator, rhs);
                    /* this doesn't work - it breaks copy propagation - is it too late for CSA Calls to be inserted ? Or why ?
                    
                    if (lbp == ctx.VM_AddressBP && rbp == ctx.VM_AddressBP ) {
		      source = new CondExp(lhs, exp.operator, rhs);
                    } else {
  		      source = new CSACallExp(
		        (exp.operator=="==") ? CSACallExp.Names.acmpeqBarrier : CSACallExp.Names.acmpneBarrier, lhs, rhs
                      );
                    }
                    */
		}
	    } else { // this branch is needed just for cannonicalization of non-reference types

	        assert(exp.operator == "==" || exp.operator == "!=");
	        assert(!exp.rhs.isReference() && !exp.lhs.isReference());

                // fix comparison of object reference and something else
                // (i.e. int, ...)
                J2cValue rhsv = j2cCanonicalize(exp.rhs);
                J2cValue lhsv = j2cCanonicalize(exp.lhs);
                
	        boolean riss = rhsv.getBlueprint(ctx.domain).isScalar();
                boolean liss = lhsv.getBlueprint(ctx.domain).isScalar();

                assert(!riss && !liss);

//                System.err.println("CMP3: "+lhsv+" "+rhsv);

/*
                this never happens - see assertion above
                		   
		if (liss && !riss) {
		    rhsv = new InternalReference(new CCastExp("HEADER *",rhsv));
                } else if (riss && !liss) {
                    lhsv = new InternalReference(new CCastExp("HEADER *",lhsv));
                }
*/                 
		 source = new CondExp(lhsv, exp.operator, rhsv);
//                source = new CSACallExp(
//                  (exp.operator=="==") ? CSACallExp.Names.acmpeqBarrier : CSACallExp.Names.acmpneBarrier, lhsv, rhsv);
            }				     
	}
	public void visit(IfExp exp) {
	    if (!checkIf(exp)) {
		J2cValue ifT = j2cCanonicalize(exp.ifTrue);
		J2cValue ifE = j2cCanonicalize(exp.ifFalse);
		// For checkcast and arraystore, we may get
		// (if (null? p) #t #t).  This optimization is not
		// safe if there are side-effects in the condition.
		if (ifT == ifE) {
		    mc.log("(if ", exp.cond, "  X X ) => X, namely ",
			     ifT);
		    result = ifT;
		}
		else
		    source = new IfExp(j2cCanonicalize(exp.cond), ifT, ifE);
	    }
	}
	// We get these for header fields we don't know about, such as
	// the monitor or hashcode.
	public void visit(MemExp exp) {
	    J2cValue v = asAddress(exp.addr);
	    J2cValue idx = j2cCanonicalize(exp.offset);
	    
	    source = new MemExp(v, idx);
	    if (v instanceof J2cReference && idx.isConcrete()) {
		S3Blueprint bp = ((J2cReference) v).getBlueprint();
		int offset = ((J2cInt) idx).intValue();

		if (bp == ctx.VM_AddressBP) {
		    // All VM_Address accessors load and store primitives
		    // at offset 0.  The only other thing you can do
		    // with an address is implicitly convert it to an
		    // Oop.
		    if (offset == 0 && !origin.isReference()) {
			if (origin instanceof IntValue)
			    result = new J2cInt(source,
						((IntValue) origin).getType());
			else if (origin instanceof LongValue)
			    result = new J2cLong(source, null, TypeCodes.LONG);
			else
			    throw new RuntimeException
				("bad VM_Address load type: " + origin);
			log("VM_Address operation =>", result);
			return;
		    }  else if (offset < ctx.headerSize) {
			mc.log("VM_Address.asOop() detected");
                        v = new J2cReference
                            (new ValueAccessExp(v), ctx.OopBP);
			source = new MemExp(v, idx);
		    } else {
			mc.log("VM_Address used as object?");
			// else we must be compiling code that treats
			// VM_Address as an object
		    }
		}

		Field f = bp.fieldAt(offset);
		if (f == null) {
		    result = makeValue(source, ctx.intBP.getType(), true);
		    return;
		}
		Selector.Field fsel = f.getSelector();
		mc.log("sel = ", fsel);
		mc.log("field = ", f);
		    
		// Basically, the only pointers to concrete values are
		// blueprints shared state objects, and strings.  What
		// happens when we see a final field dereferenced from
		// one of these objects?  We probably don't want to
		// pull it out and indirect through J2cRoots rather
		// than the field itself, but we can at least realize
		// that we know it's exact type and outermost array
		// dimension.  This could be useful when inlining
		// String methods...

		// FIXME:  We need to take a peek at the field to
		// determine its exact type, outermost array
		// dimension, and value if it is primitive.  How is
		// this to be done?  We can't use VM_Address
		// operations during bootup, because the heap hasn't
		// been copied into image memory yet.
		//boolean isConcrete = v.isConcrete() && f.getMode().isFinal();

		S3Blueprint fbp = blueprintFor(f.getDeclaringType());
		J2cFieldAccessExp ref = new J2cFieldAccessExp(v, fbp, fsel);
		try {
		    Type ft= f.getType();
		    result = makeValue(ref, ft, true);
		} catch (LinkageException e) { throw e.unchecked(); }
		mc.log("patched field access = ", result);
	    }
	    else
		throw die("bad memexp " + v + " + " + idx);
	}

	public void visit(ArrayAccessExp exp) {
	    // Surround the array access in an apporpriately typed
	    // node!
	    J2cValue arr = j2cCanonicalize(exp.arr);
	    J2cValue idx = j2cCanonicalize(exp.index);
	    source = new ArrayAccessExp(arr, idx);
	    if (arr instanceof J2cArray) {
		S3Blueprint.Array abp
		    = (S3Blueprint.Array) ((J2cArray) arr).getBlueprint();
		S3Blueprint ebp = (S3Blueprint) abp.getComponentBlueprint();
		char tag = ebp.getType().getUnrefinedName().getTypeTag();
		switch (tag) {
		case TypeCodes.ARRAY:
		    result = new J2cArray(source, ebp);
		    break;
		case TypeCodes.OBJECT:
		    result = new J2cReference(source, ebp);
		    break;
		case TypeCodes.LONG:
		    result = new J2cLong(source, null, TypeCodes.LONG);
		    break;
		case TypeCodes.FLOAT:
		    result = new J2cFloat(source, null);
		    break;
		case TypeCodes.DOUBLE:
		    result = new J2cDouble(source, null);
		    break;
		default:
		    // for xALOAD where x strictly< int, the IR
		    // includes the necessary conversion.
		    result = new J2cInt(source, tag);
		}
		    
	    } else
		mc.log("array access on non-array value ", arr);
		
	}
	// Can possibly optimize.  To do something useful, I need to
	// expand CHECK_ARRAY_BOUNDS() into IfCmd(..., throwException())
	public void visit(ArrayLengthExp exp) {
	    J2cValue v = j2cCanonicalize(exp.arr);
	    source = new ArrayLengthExp(v);
	    if (v instanceof J2cArray) {
		J2cInt length = ((J2cArray) v).dims[0];
		if (length.isConcrete()) {
		    // Umm, should I always do this transformation?  I
		    // I could if the array's first dimension somehow
		    // pointed back to the array.  But then, how would I
		    // represent an actual access to the length field;
		    result = length;
		    mc.log("constant folding ", v, ".length => ", result);
		    return;
		} else if (length.max < Integer.MAX_VALUE) {
		    mc.log("bounding ", v, ".length to ", length);
		}
		// always remember that it is non-negative
		result = new J2cInt(source, TypeCodes.INT,
				    length.min, length.max);
	    } else {
		mc.log("not an array: ", v);
	    }
	}
	// Can optimize when exact type known
	public void visit(BlueprintAccessExp exp) {
	    // Hmm.  What happens if r has a monomorphic interface type?
            J2cReference r = (J2cReference) asAddress(exp.ref);
	    
	    if (r.t.exactTypeKnown()) {
		result = new BlueprintReference(r.getBlueprint());
		mc.log("statically found blueprint of ", exp.ref);
	    } else {
		source = new BlueprintAccessExp(r);
		result = new BlueprintReference(source, r.getBlueprint());
		mc.log("blueprint for ", r, " unknown");
	    }
	}
	// can optimize constant values, such as in invocations
	public void visit(BitFieldExp exp) {
	    J2cValue v = j2cCanonicalize(exp.word);
	    if (v.isConcrete()) {
		int ival = ((J2cInt) v).intValue();
		ival = (ival >> exp.shift) & exp.mask;
		result = J2cValue.makeIntConstant(((IntValue) origin).getType(), ival);
		mc.log("computed bit field value ", exp);
	    } else {
		source = new BitFieldExp(v, exp.shift, exp.mask);
		result = new J2cInt(source, ((IntValue) origin).getType(),
				    0, (exp.mask == -1
					? -1 >>> exp.shift
					: exp.mask));
	    }
	}

	private J2cValue getComponentBlueprint(Blueprint _bp) {
	    Blueprint.Array bp = (Blueprint.Array) _bp;
	    S3Blueprint cbp = (S3Blueprint) bp.getComponentBlueprint();
	    return new BlueprintReference(null, cbp);
	}
	
	// CallExp is a hack: It is used to call functions and macros
	// inside the interpreter such as is_subtype_of,
	// CHECK_ARRAY_BOUNDS, INVOKE, and ROLL.  I'm not sure whether
	// it is worth the trouble to eliminate.  It probably is.  As
	// it stands, we have INVOKE_NATIVE calls, INVOKE_SYSTEM
	// calls, INVOKE_CSA calls and CallExp operations.  This is
	// too much!
	public void visit(CallExp exp) {
	    // rolls are handled by abstract interpretation.  Should I
	    // ever see a ROLL() expression?
	    if (exp.fname == "ROLL") {
		mc.log("no-op ROLL()");
		result = J2cValue.NULL;
	    }
	    else if (exp.fname == "COPY") { // I don't know why this should be here, I just mimic ROLL
		mc.log("no-op COPY()");
		result = J2cValue.NULL;
	    }	    
	    else if (exp.fname == "is_subtype_of") {
		// FIXME: special case where arg0 is concrete?
		BlueprintReference arg0
		    = (BlueprintReference) j2cCanonicalize(exp.args[0]);
		S3Blueprint bp0 = arg0.blueprintValue();
		BlueprintReference arg1
		    = (BlueprintReference) j2cCanonicalize(exp.args[1]);
		S3Blueprint bp1 = arg1.blueprintValue();

		// FIXME: If there is a more complete analysis.
		// Oop is at the wrong spot in the type hierarchy
		int status = ((bp0 == ctx.OopBP || bp0 == ctx.VM_AddressBP)
			      ? Analysis.UNKNOWN
			      : evalCast(bp0, bp1));
		if (status == Analysis.GOOD_CAST) {
// 		    System.err.println(getSourceFile() + ":" + getLine()
// 				       + " good cast "
// 				       + bp0  + " to " + bp1);
		    result = J2cValue.makeIntConstant(TypeCodes.BOOLEAN, 1);
		} else if (status == Analysis.BAD_CAST
			   && (ctx.optimizeBadCastToArray || !bp1.isArray()))
		{
// 		    System.err.println(getSourceFile() + ":" + getLine()
// 				       + " bad cast "
// 				       + bp0 + " to " + bp1);
		    result = J2cValue.makeIntConstant(TypeCodes.BOOLEAN, 0);
		}
		else {
		    Blueprint[] ctypes = concreteSubtypesOf(bp1);
		    if (ctypes != null && ctypes.length == 1
			&& ctypes[0] != ctx.ClassBP)
		    {
			result = new J2cInt(new CondExp
					    (arg0, "==",
					     new BlueprintReference((S3Blueprint) ctypes[0])),
					    TypeCodes.BOOLEAN);
			subtypeEq++;
		    } else if (false && bp1.isArray()) {
			S3Blueprint.Array abp1 = (S3Blueprint.Array) bp1;
			result = new J2cInt(new InvocationExp("subtype_of_array",
							      arg0, arg1,
							      ctx.booleanBP),
					    TypeCodes.BOOLEAN);
			subtypeArray++;
		    } else if (bp1 instanceof Blueprint.Scalar) {
			result = new J2cInt(new InvocationExp("subtype_of_scalar",
							      arg0, arg1,
							      ctx.booleanBP),
					    TypeCodes.BOOLEAN);
			subtypeScalar++;
		    } else {
			source = new InvocationExp
			    (J2cValue.makeSymbolicReference("is_subtype_of"),
			     new Value[] { arg0, arg1 },
			     null);
			subtypeFull++;
		    }
		}
	    }
	    else if (exp.fname == "ARRAY_STORE_INVALID") {
		// Try to eliminate this check.  If we know the exact
		// type of the array, we can go through the standard
		// is_subtype_of optimizations.  Otherwise, we take a
		// much slower path.
		J2cValue _a = j2cCanonicalize(exp.args[0]);
		if (_a == J2cValue.NULL) {
		    result = J2cValue.makeIntConstant(TypeCodes.INT, 0);
		    return;
		}
		J2cArray a = (J2cArray) _a;
		S3Blueprint abp = a.getBlueprint(null);
		Blueprint[] concreteAtype = concreteSubtypesOf(abp);
		J2cValue _o = j2cCanonicalize(exp.args[1]);
		if (_o == J2cValue.NULL) {
		    result = J2cValue.makeIntConstant(TypeCodes.INT, 0);
		    return;
		}
		J2cReference o = (J2cReference) _o;
		ValueSource os = (o.getTypeInfo().exactTypeKnown()
				  ? null
				  : new BlueprintAccessExp(o));
		J2cValue obpr
		    = new BlueprintReference(os, o.getBlueprint(null));
		J2cInt subtypeTest = null;
		if (a.getTypeInfo().exactTypeKnown()) {
		    // We can optimize the test because we know
		    // exactly which blueprints are involved
		    J2cValue arg2 = getComponentBlueprint(abp);
		    subtypeTest = 
			(J2cInt) j2cCanonicalize(new IntValue
						 (TypeCodes.BOOLEAN,
						  new CallExp("is_subtype_of",
							      obpr,
							      arg2)));
		} else if (concreteAtype != null
			   && concreteAtype.length == 1) {
		    J2cValue arg2
			= getComponentBlueprint(concreteAtype[0]);
		    subtypeTest = 
			(J2cInt) j2cCanonicalize(new IntValue
						 (TypeCodes.BOOLEAN,
						  new CallExp("is_subtype_of",
							      obpr,
							      arg2)));
		}
		if (subtypeTest != null) {
		    if (o.getTypeInfo().includesNull()) {
			J2cValue one =
			    J2cValue.makeIntConstant(TypeCodes.BOOLEAN, 1);
			ValueSource vs = new IfExp(o, subtypeTest, one);
			subtypeTest = (J2cInt) j2cCanonicalize
			    (new IntValue(TypeCodes.BOOLEAN, vs));
		    }
		    result = j2cCanonicalize
			(new IntValue(TypeCodes.BOOLEAN,
				      new UnaryExp("!", subtypeTest)));
		} else {
		    source = new InvocationExp("ARRAY_STORE_INVALID",
					       new InternalReference(new CCastExp("HEADER *",a)), o, null);
		}
	    }
	    else if (exp.fname == "INVOKE") {
		J2cValue target = j2cCanonicalize(exp.args[1]);
		J2cValue[] args = new J2cValue[getArgumentRegisterCount()];
		for (int i = 0; i < args.length; i++)
		    args[i] = (J2cValue) getArgumentRegisterAt(i);
		S3Method m = ((MethodReference) target).method;
		try {
		    Type rt = m.getReturnType();
		    S3Domain d = (S3Domain) rt.getContext().getDomain();
		    S3Blueprint rbp = (S3Blueprint) d.blueprintFor(rt);
		    S3Blueprint[] at = new S3Blueprint[args.length];
		    
		    // devirtualization may lead to wider receiver types!
		    at[0] = (S3Blueprint) d.blueprintFor(m.getDeclaringType());
		    for (int ati = 1, ai = 0;
			 ati < args.length;
			 ati++) {
			// don't fill in type for 2nd half values
			if (args[ati] == null)
			    continue;
			try {
			    Type t = m.getArgumentType(ai++);
			    at[ati] = (S3Blueprint) d.blueprintFor(t);
			} catch (RuntimeException e) {
			    mc.log("error getting arguments to ", m);
			    throw e;
			}
		    }
		    if (m == ctx.processThrowable) {
			source = new InvocationExp("j2c_throw",
						   args[1],
						   rbp);
		    } else {
			source = new InvocationExp(target, args, rbp, at);
		    }
		    result = makeValue(source, rt, false);
		} catch (LinkageException e) { throw e.unchecked(); }
	    }
	    else if (false &&  exp.fname == "CHECK_NULL") {
		// Can I eliminate this null pointer check?
		// Null pointer checks aren't even explicit in the
		// SpecificationIR yet...
		//
		// Most null checks can be eliminated because they
		// immediately preceed some access to the object.
		// Null checks are needed on:
		// * nonvirtual method calls
		// * array accesses that lack other checks
		// * reads if page 0 is readable
		// ...
	    }
	    else if (exp.fname == "GET_CONSTANT_BP_RESOLVED_INSTANCE_METHODREF") {
		J2cValue _idx = j2cCanonicalize(exp.args[0]);
		int idx = ((J2cInt) _idx).intValue();
		try {
		    ConstantResolvedInstanceMethodref rinfo = dcp.resolveInstanceMethod(idx);
                    result = new BlueprintReference((S3Blueprint) rinfo.getStaticDefinerBlueprint());
		} catch (LinkageException e) {
		    throw e.unchecked();
		}
	    }
	    else if (exp.fname == "GET_CONSTANT_SHST_RESOLVED_STATIC_METHODREF") {
		J2cValue _idx = j2cCanonicalize(exp.args[0]);
		int idx = ((J2cInt) _idx).intValue();
		try {
		    ConstantResolvedStaticMethodref rinfo =
			dcp.resolveStaticMethod(idx);
		    result = 
			new SharedStateReference(null,
						 rinfo.getSharedState());
		} catch (LinkageException e) {
		    throw e.unchecked();
		}
	    }
	    else if (exp.fname == "GET_CONSTANT_SHST_RESOLVED_STATIC_FIELDREF") {
		J2cValue _idx = j2cCanonicalize(exp.args[0]);
		int idx = ((J2cInt) _idx).intValue();
		try {
		    ConstantResolvedStaticFieldref rinfo =
			dcp.resolveStaticField(idx);
		    result = 
			new SharedStateReference(null,
						 rinfo.getSharedState());
		} catch (LinkageException e) {
		    throw e.unchecked();
		}
	    }
	    else {
		// general case of internal calls
		Value[] args = new Value[exp.args.length];
		for (int i = 0; i < args.length; i++)
		    args[i] = j2cCanonicalize(exp.args[i]);
		source =  new InvocationExp
		    (J2cValue.makeSymbolicReference(exp.fname), args, null);
	    }
	}

	CSACallExp expandPrimitiveStoreBarrier(CSACallExp exp) {
	
	  J2cArray array = (J2cArray) j2cCanonicalize(exp.args[0]);
	  J2cValue index = j2cCanonicalize(exp.args[1]);
	  
          int aArray = examineReference(array) | Assert.NONNULL;
          
          Blueprint.Array bp = (Blueprint.Array) array.getBlueprint();
          int componentSize = bp.getComponentSize();

          if (exp.args[2].isWide()) {
	    return new CSACallExp(exp.fname,
              new Value[] {
                array,
                index,
                exp.args[2],
                ((WideValue)exp.args[2]).getSecondHalf(),
                J2cValue.makeIntConstant(TypeCodes.INT, componentSize),
                J2cValue.makeIntConstant(TypeCodes.INT, aArray),
              }
            );          
          } else {
	    return new CSACallExp(exp.fname,
              new Value[] {
                array,
                index,
                exp.args[2],
                J2cValue.makeIntConstant(TypeCodes.INT, componentSize),
                J2cValue.makeIntConstant(TypeCodes.INT, aArray),
              }
            );
          }
	}
	
	CSACallExp expandReferenceStoreBarrier(CSACallExp exp) {
	
	  J2cArray array = (J2cArray) j2cCanonicalize(exp.args[0]);
	  J2cValue index = j2cCanonicalize(exp.args[1]);
	  J2cValue newPointer = asAddress(exp.args[2]);
	  
          int aArray = examineReference(array) | Assert.NONNULL;
          
          if (false) {
            if ( (aArray & Assert.ARRAY_LENGTH_KNOWN) != 0) {
              System.out.println("\nArray size known at load" + array);
            }
          }
          
          int aNewPointer = examineReference(newPointer);          
          
          Blueprint.Array bp = (Blueprint.Array) array.getBlueprint();
          int componentSize = bp.getComponentSize();

          Blueprint cbp = bp.getComponentBlueprint();
          if ( (cbp != ctx.OopBP) && (cbp != ctx.OpaqueBP) && (cbp == null || !ctx.anal.isHeapAllocated(cbp)) ) {
            aArray|=Assert.IN_IMAGEONLY_SLOT; // a bit confusing, but this really means that the old value does not need a Yuasa barrier
          }

	  return new CSACallExp(exp.fname,
              new Value[] {
                array,
                index,
                newPointer,
                J2cValue.makeIntConstant(TypeCodes.INT, componentSize),
                J2cValue.makeIntConstant(TypeCodes.INT, aArray),
                J2cValue.makeIntConstant(TypeCodes.INT, aNewPointer)                 
              }
          );
        }          

	CSACallExp expandLoadBarrier(CSACallExp exp) {
	
	  J2cArray array = (J2cArray) j2cCanonicalize(exp.args[0]);
	  J2cValue index = j2cCanonicalize(exp.args[1]);
	  
          int aArray = examineReference(array) | Assert.NONNULL;
          
          if (false) {
            if ( (aArray & Assert.ARRAY_LENGTH_KNOWN) != 0) {
              System.out.println("\nArray size known at load" + array);
            }
          }
          
          Blueprint.Array bp = (Blueprint.Array) array.getBlueprint();
          int componentSize = bp.getComponentSize();

	  return new CSACallExp(exp.fname,
              new Value[] {
                array,
                index,
                J2cValue.makeIntConstant(TypeCodes.INT, componentSize),
                J2cValue.makeIntConstant(TypeCodes.INT, aArray),
              }
          );
	}

	CSACallExp expandAastoreBarrier(CSACallExp exp) { // this is the nonarraylet version of the barrier
	
	  J2cArray array = (J2cArray) j2cCanonicalize(exp.args[0]);
	  J2cValue offset = j2cCanonicalize(exp.args[1]);
	  J2cValue newPointer = asAddress(exp.args[2]);
	  
          int aArray = examineReference(array) | Assert.NONNULL;
          int aNewPointer = examineReference(newPointer);
          
          Blueprint.Array bp = (Blueprint.Array) array.getBlueprint();
          
          Blueprint cbp = bp.getComponentBlueprint();
          if ( (cbp != ctx.OopBP) && (cbp != ctx.OpaqueBP) && (cbp == null || !ctx.anal.isHeapAllocated(cbp)) ) {
            aArray|=Assert.IN_IMAGEONLY_SLOT; // a bit confusing, but this really means that the old value does not need a Yuasa barrier
          }

	  return new CSACallExp("aastoreWriteBarrier",
              new Value[] {
                array,
                offset,
                newPointer,
                J2cValue.makeIntConstant(TypeCodes.INT, aArray),
                J2cValue.makeIntConstant(TypeCodes.INT, aNewPointer) 
              }
          );
	}
  
	// presently not used, as ACMP is handled in CodeGen.java
	CSACallExp expandAcmpBarrier(CSACallExp exp) {
	
	  J2cValue v1 = asAddress(exp.args[0]);
	  J2cValue v2 = asAddress(exp.args[1]);
	  
          int aV1 = examineReference(v1);
          int aV2 = examineReference(v2);
          
	  return new CSACallExp(exp.fname,
              new Value[] {
                v1,
                v2,
                J2cValue.makeIntConstant(TypeCodes.INT, aV1),
                J2cValue.makeIntConstant(TypeCodes.INT, aV2) 
              }
          );
	}

	CSACallExp expandCSACall(CSACallExp exp) {
	
	  if (false) {
      	    System.err.print("CSA Call expander: "+exp.fname);
      	    System.err.println(", args: ");
      	    for(int i=0;i<exp.args.length;i++) {
	      System.err.println("\t["+i+"]="+exp.args[i]);
            }
          }
          
          if (exp.fname == CSACallExp.Names.aastoreBarrier) {
            if (MemoryManager.the().needsArrayAccessBarriers()) {
              return expandReferenceStoreBarrier(exp);
            } else {
              return expandAastoreBarrier(exp);
            }
          } else if ( false && ((exp.fname == CSACallExp.Names.acmpneBarrier) || (exp.fname == CSACallExp.Names.acmpeqBarrier)) ) {
            // now handled in CodeGen.java
            return expandAcmpBarrier(exp); 
          } else if ( MemoryManager.the().needsArrayAccessBarriers() && 
             ((exp.fname == CSACallExp.Names.bastoreBarrier) ||
              (exp.fname == CSACallExp.Names.castoreBarrier) || 
              (exp.fname == CSACallExp.Names.dastoreBarrier) ||
              (exp.fname == CSACallExp.Names.fastoreBarrier) ||
              (exp.fname == CSACallExp.Names.iastoreBarrier) ||
              (exp.fname == CSACallExp.Names.lastoreBarrier) ||
              (exp.fname == CSACallExp.Names.sastoreBarrier) ))
              {
            return expandPrimitiveStoreBarrier(exp);
            
          } else if ( MemoryManager.the().needsArrayAccessBarriers()  &&
             ((exp.fname == CSACallExp.Names.baloadBarrier) ||
              (exp.fname == CSACallExp.Names.caloadBarrier) || 
              (exp.fname == CSACallExp.Names.daloadBarrier) ||
              (exp.fname == CSACallExp.Names.faloadBarrier) ||
              (exp.fname == CSACallExp.Names.ialoadBarrier) ||
              (exp.fname == CSACallExp.Names.laloadBarrier) ||
              (exp.fname == CSACallExp.Names.saloadBarrier)  ||
              (exp.fname == CSACallExp.Names.aaloadBarrier) ))
              {
            return expandLoadBarrier(exp);
            
          }

	  return exp;
	}


	// FIXME: I should really be caching the methods and
	// devirtualized lookup exps for csa calls, since they
	// happen _quite_ frequently.
	public void visit(CSACallExp exp) {
	    exp = expandCSACall(exp);
	    
	    if (false) {
      	      System.err.print("After expansion: "+exp.fname);
      	      System.err.println(", args "+exp.args.length+": ");
      	      for(int i=0;i<exp.args.length;i++) {
	        System.err.println("\t["+i+"]="+exp.args[i]);
              }
              System.err.println("result: "+result);              
            }
	    
	    J2cValue[] args = new J2cValue[exp.args.length + 1];
	    args[0] = ctx.CSAvalue;
	    valueMap.put(args[0], args[0]); // ?
	    for (int i = 1; i < args.length; i++)
		args[i] = j2cCanonicalize(exp.args[i-1]);
	    S3Method m = ctx.findCSAMethod(exp.fname);
	    S3Domain e = ctx.executiveDomain;
	    try {
		S3Blueprint rt = (S3Blueprint) e.blueprintFor(m.getReturnType());
		S3Blueprint[] at = new S3Blueprint[args.length];
		at[0] = ctx.CSAbp;
		for (int ai = 1, ati = 0;
		     ai < args.length;
		     ai++) {
		    if (args[ai] == null)
			continue;
		    Type t = m.getArgumentType(ati++);
		    at[ai] = (S3Blueprint) e.blueprintFor(t);
		}
		if (m == ctx.processThrowable) {
		    source = new InvocationExp("j2c_throw",
					       args[1],
					       ctx.voidBP);
		}
		else {
		    if (false) {
		      System.err.print("As invocation: "+exp.fname);
		      System.err.println(", args "+args.length+": ");
		      for(int i=0;i<args.length;i++) {
                        System.err.println("\t["+i+"]="+args[i]+" type "+at[i]);
                      }
                      System.err.println("rt: "+rt);
                      System.err.println("result: "+result);
                    }

		    source = new InvocationExp(J2cValue.makeNonvirtualReference(m),
					       args, rt, at);
					       
                    if ( (exp.fname==CSACallExp.Names.aaloadBarrier) ||
                      (exp.fname == CSACallExp.Names.baloadBarrier) ||
                      (exp.fname == CSACallExp.Names.caloadBarrier) || 
                      (exp.fname == CSACallExp.Names.daloadBarrier) ||
                      (exp.fname == CSACallExp.Names.faloadBarrier) ||
                      (exp.fname == CSACallExp.Names.ialoadBarrier) ||
                      (exp.fname == CSACallExp.Names.laloadBarrier) ||
                      (exp.fname == CSACallExp.Names.saloadBarrier)  ||
                      (exp.fname == CSACallExp.Names.aaloadBarrier) ) {
                      
                      Blueprint.Array bp = (Blueprint.Array) (  ((J2cArray)exp.args[0]).getBlueprint() );
                      
                      S3Blueprint ebp = (S3Blueprint) bp.getComponentBlueprint();
                      char tag = ebp.getType().getUnrefinedName().getTypeTag();
                      switch (tag) {
                        case TypeCodes.ARRAY:
                          result = new J2cArray(source, ebp);
                          break;
                        case TypeCodes.OBJECT:
                          result = new J2cReference(source, ebp);
                          break;
                        case TypeCodes.LONG:
                          result = new J2cLong(source, null, TypeCodes.LONG);
                          break;
                        case TypeCodes.FLOAT:
                          result = new J2cFloat(source, null);
                          break;
                        case TypeCodes.DOUBLE:
                          result = new J2cDouble(source, null);
                          break;
                        default:
                          // for xALOAD where x strictly< int, the IR
                          // includes the necessary conversion.
                          result = new J2cInt(source, tag);
                      }
                    }
		}
	    }
	    catch (LinkageException exn) { throw exn.unchecked(); }
	}
	public void visit(IfCmd exp) {
	    if (!checkIf(exp))
		source = new IfCmd(j2cCanonicalize(exp.cond),
				   j2cCanonicalize(exp.ifTrue),
				   (exp.ifFalse == null
				    ? null
				    : j2cCanonicalize(exp.ifFalse)));
	}

	// eliminate branches
	J2cValue canonicalizeElse(Value elsePart) {
	    return (elsePart == null
		    ? J2cValue.NULL
		    : j2cCanonicalize(elsePart));
	}
	boolean checkIf(IfExp exp) {
	    J2cValue v = j2cCanonicalize(exp.cond);
	    mc.log("checkIf(", v, ")");
	    if (v == J2cValue.NULL) {
		result = canonicalizeElse(exp.ifFalse);
		mc.log("condition(", v, ") => else(", result, ")");
		return true;
	    } else if (v instanceof J2cReference) {
		if (!((J2cReference) v).t.includesNull()) {
		    result = j2cCanonicalize(exp.ifTrue);
		    mc.log("condition(", v, ") => then(", result, ")");
		    return true;
		}
	    } else if (v instanceof J2cInt) {
		J2cInt iv = (J2cInt) v;
		if (iv.min > 0 || iv.max < 0) {
		    result = j2cCanonicalize(exp.ifTrue);
		    mc.log("condition(", v, ") => then(", result, ")");
		    return true;
		} else if (iv.isConcrete() && iv.intValue() == 0) {
		    result = canonicalizeElse(exp.ifFalse);
		    mc.log("condition(", v, ") => else(", result, ")");
		    return true;
		}
	    }
	    return false;
	}

	// convert a SpecificationIR value to a J2cValue.  We maintain
	// a cache describing how values are mapped.  If the value is
	// found in the cache, return its mapping.  Otherwise,
	// simplify the IR.  On the way, we may discover that this
	// value is actually a constant.  If not, try converting the
	// value directly.
	int depth = 0;
	public AbstractValue convert(Value v) {
	    AbstractValue ret = (AbstractValue) valueMap.get(v);
	    // Man, is this ever fucked up.  I don't see how this can
	    // happen.  Luckily, it seems to be OK to blow out these
	    // values each time we recurse (?)
	    assert(depth > 0 ||
			      (source == null && origin == null && result == null));
	    if (ret == null) {
		if (J2cImageCompiler.KEEP_LOGS)
		    mc.log(depth + ": convert " + v);
		depth++;
		ValueSource saveSource = source;
		AbstractValue saveResult = result;
		Value saveOrigin = origin;
		try {
		    if (v.source != null) {
			if (v instanceof J2cValue) {
			    mc.log((depth-1) + ": ", v, " already J2cValue");
			    valueMap.put(v, v);
			    return v;
			}
			origin = v;
			visitAppropriate(v.source);
			ret = result;
		    }
		    if (ret == null)
			ret = super.convert(v);
		}
		finally {
		    origin = saveOrigin; 
		    source = saveSource;
		    result = saveResult;
		}
		valueMap.put(v, ret);
		valueMap.put(ret, ret);
		depth--;
		if (J2cImageCompiler.KEEP_LOGS)
		    mc.log(depth + ": convert ", v, " => ", ret);
	    } else if (J2cImageCompiler.KEEP_LOGS)
		mc.log(depth + ": convert ", v, " => ", ret, " (cached)");
		
	    
	    assert(depth > 0 ||
			      (source == null && origin == null && result == null));
	    return ret;
	}

	public ValueSource mapIr(ValueSource vs) {
	    Value saveOrigin = origin;
	    ValueSource saveSource = source;
	    AbstractValue saveResult = result;
	    if (J2cImageCompiler.KEEP_LOGS)
		mc.log(depth + ": mapIr ", vs);
	    depth++;
	    try {
		origin = null;
		source = null;
		result = null;
		visitAppropriate(vs);
		//assert(irMapper.result == null, "too much
		//simplification");
		if (result != null)
		    mc.log("mapIr ", vs, " can't go to ", result);
		if (J2cImageCompiler.KEEP_LOGS)
		    mc.log((depth-1) + ": mapIr ", vs, " => ", source);
		return source;
	    }
	    finally {
		depth--;
		origin = saveOrigin;
		source = saveSource;
		result = saveResult;
	    }
	}
    }

    J2cValue makeValue(ValueSource source, Type t, boolean promoteInt) {
	return ctx.makeValue(source, t, promoteInt);
    }

    S3Blueprint blueprintFor(Type t) {
	return ctx.blueprintFor(t);
    }
	
    // Do I need this irMapper thing?  Probably, since I need to
    // track an instruction's Temp expressions.  I guess I need to
    // cache ValueSource mappings, as well as Value mappings (or
    // simply wrap Temps in Values?).
    protected J2cCanonicalizer irMapper; // duplicate specMapper value
					 // with more specific type
    protected Canonicalizer makeCanonicalizer() {
	return irMapper = new J2cCanonicalizer();
    }


    // What does this accomplish exactly?
    ValueSource mapIr(ValueSource vs) {
	return irMapper.mapIr(vs);
    }

    // Hmm.  J2cValue.INVALID is not a J2cValue
    J2cValue j2cCanonicalize(Value v) {
	return (J2cValue) irMapper.convert(v);
    }

    J2cValue asPrimitive(Value v) {
	J2cValue ret = j2cCanonicalize(v);
	if (ret instanceof J2cReference)
	    return new J2cInt(new ConversionExp(ret), J2cValue.SIZE_T_CODE);
	else if (ret == J2cValue.NULL)
	    return J2cValue.makeIntConstant(J2cValue.SIZE_T_CODE, 0);
	else
	    return ret;
    }

    J2cValue asAddress(Value v) {
	J2cValue ret = j2cCanonicalize(v);
	
	// FIXME: doesn't this screw-up J2cInt being concrete values 0 ?
	// it doesn't create J2cNull ...
	
	/*
	if (ret instanceof J2cInt)
	    return new J2cReference(new ConversionExp(ret), ctx.VM_AddressBP);
	else
	    return ret;		// J2cReference or J2cNull
	*/

	// however, this I wrote I vaguely remember used to cause problems,
	// though I can't reproduce the now ... so maybe, it was caused
	// by something else
	
	if (ret instanceof J2cInt) {
          Integer ivalue = (Integer) ((J2cInt)ret).concreteValue();
          if (ivalue != null) {
            if (ivalue.intValue()==0) {
              return J2cValue.NULL;
            } else {
              return new J2cReference( new ConversionExp(ret), ctx.VM_AddressBP, false ,false);  //? should be exact ? ever used ?
            }
          } else {
          
            J2cInt nullInt = (J2cInt) J2cValue.makeIntConstant(J2cValue.SIZE_T_CODE, 0);
            if ( ((J2cInt)ret).includes( nullInt ) ) {
              return new J2cReference(new ConversionExp(ret), ctx.VM_AddressBP, false, true);
            } else {
              return new J2cReference(new ConversionExp(ret), ctx.VM_AddressBP, false, false);
            }
          } 
	    // return new J2cReference(new ConversionExp(ret), ctx.VM_AddressBP);
	} else {
	    return ret;		// J2cReference or J2cNull	    
        }	
/*
    // this didn't work, but I don't know why
    // still, the question is if it's useful - we probably don't care if VM_Address is null
    // we still, however, do care, when it's converted back to reference (asOop)
    // does such conversion preserve nullness information ?
    // I think it does not - see the AFIAT visitor

	if (ret instanceof J2cInt) {
          Integer ivalue = (Integer) ((J2cInt)ret).concreteValue();
          if (ivalue != null) {
            if (ivalue.intValue()==0) {
              return J2cValue.NULL;
            } else {
              return new J2cReference( new ConversionExp(ret), ctx.VM_AddressBP, true ,false);  //? should be exact ? ever used ?
            }
          } else {
          
            J2cInt nullInt = (J2cInt) J2cValue.makeIntConstant(J2cValue.SIZE_T_CODE, 0);
            if ( ((J2cInt)ret).includes( nullInt ) ) {
              return new J2cReference(new ConversionExp(ret), ctx.VM_AddressBP, false, true);
            } else {
              return new J2cReference(new ConversionExp(ret), ctx.VM_AddressBP, false, false);
            }
          } 
	    // return new J2cReference(new ConversionExp(ret), ctx.VM_AddressBP);
	} else {
	    return ret;		// J2cReference or J2cNull	    
        }
*/        
    }

    // Umm.  Is this needed?
    J2cReference asOop(Value v) {
	J2cValue ret = j2cCanonicalize(v);
	if (ret instanceof J2cInt)
	    return new J2cReference(new ConversionExp(ret), ctx.OopBP);
	else
	    return (J2cReference) ret;
    }

    public void parseImmediates(Instruction insn) {
	// Map SpecificationIR immediate operands to their concrete
	// values
	if (insn instanceof Instruction.Switch)
	    // FIXME: Deal with Padding and IntValueList.  We can
	    // ignore switch instructions for now, since they don't
	    // have any evals.
	    return;
	if (insn.istreamIns.length > 0) {
	    StreamableValue[] imm = insn.istreamIns;
	    int off = ((insn.getOpcode() & JVMConstants.Opcodes.WIDE_OFFSET) == 0
		       ? 1
		       : 2);
	    for (int i = 0; i < imm.length; i++) {
		valueMap.put(imm[i],
			     J2cValue.makeAnyConstant(imm[i].decodeStream(buf, off)));
		log(imm[i], " = ", valueMap.get(imm[i]));
		off += imm[i].bytestreamSize();
	    }
	}
    }

    public void handleBody(Instruction insn) {
	// map temporary expressions to J2c
	int idx;
	for (idx = 0; idx < insn.temps.length; idx++) {
	    temps[idx] = (Temp) mapIr(insn.temps[idx]);
	}
	temps[idx] = null;

	for (idx = 0; idx < insn.evals.length; idx++) {
	    evals[idx] = j2cCanonicalize(insn.evals[idx]);
	}
	evals[idx] = J2cValue.TERMINATOR;
    }
    
    public void popInputs(Instruction insn) {
	parseImmediates(insn);
	super.popInputs(insn);
	//boolean gotANull;
	// map stackIns to popped inputs
	int ridx = 0;
	for (int vidx = 0; vidx < insn.stackIns.length; vidx++)
	    if (!(insn.stackIns[vidx] instanceof SecondHalf)) {
		valueMap.put(insn.stackIns[vidx], inputRegister[ridx]);
		// FIXME: make it possible to do some manner of explicit
		// nullchecking; here seems to be the place where we should
		// inject an expression for doing that.
		if (insn.stackIns[vidx] instanceof NonnulRefValue
		    && inputRegister[ridx] instanceof J2cNull) {
		    executionTerminates = true;
		    if (J2cImageCompiler.KEEP_LOGS)
			mc.log("null argument @" + ridx);
		}
		ridx++;
	    }
	// FIXME: Why is this done here, and repeated in so many
	// instructions below? 
	if (executionTerminates)
	    evals[0] = j2cCanonicalize(Instruction.throwException(T.NULL_POINTER_EXCEPTION));
	else
	    handleBody(insn);
    }

    public void pushOutputs(Instruction i) {
	if (!executionTerminates)
	    super.pushOutputs(i);

	// FIXME: This is SO fucked up.  stackOuts contains weird
	// SecoondHalf values for the second half of wide values, but
	// the S3AbstractInterpreter's output register array doesn't
	// contain either SecondHalf objects or nulls (as found in
	// Frame).  To make things even less consistent, I fill in the
	// argument register array myself for calls, and nulls for
	// second halves are present there.
	int realLen = 0;
	for (int idx = 0; idx < i.stackOuts.length; idx++)
	    if (!(i.stackOuts[idx] instanceof SecondHalf))
		realLen++;
    }

    // The following two methods are really only needed when copying
    // out to basic blocks.  They ensure that the values corresponding
    // to locals and stack slots are distinct.
    public void visit(Instruction.LocalRead i) {
        popInputs(i);
	J2cLocalExp refExp = (J2cLocalExp) mapIr(i.stackOuts[0].source);
	int idx =  refExp.num;
	J2cValue var = (J2cValue) getFrame().load(idx);
	J2cValue refVal = var.copy(refExp);
	outputRegister[0] = refVal;
	if (i.stackOuts.length > 1)
	    getFrame().pushWide(refVal);
	else
	    getFrame().push(refVal);
    }

    public void visit(Instruction.LocalWrite i) {
	popInputs(i);
	handleBody(i);
	J2cValue sv = (J2cValue) inputRegister[0];
	J2cValue rv = sv.copy(null);
	int idx = i.getLocalVariableOffset(buf);
	storedVariables.set(idx);
	getFrame().store(idx, rv);
    }

    public void visit(Instruction.IINC i) {
        // Umm, do I need to do anything special with the input?
	popInputs(i);
	handleBody(i);
	J2cValue rv = new J2cInt(null, TypeCodes.INT);
	int idx = i.getLocalVariableOffset(buf);
	storedVariables.set(idx);
	getFrame().store(i.getLocalVariableOffset(buf), rv);
    }

    public void visit(Instruction.MONITORENTER i) {
	visit((Instruction) i);
	if ( (!ctx.cExceptions) && ( true || ctx.domain != ctx.executiveDomain )) {
	    // We need an explicit null check before entering the
	    // executive domain.
	    //
	    // We also need to do the check outside of CAS32 for
	    // unoptimized ppc-linux builds.  For some reason, a SEGV
	    // inside the out-of-line CAS32 will screw up C++ stack
	    // walking.  
	    //
	    // FIXME: Hmm, is this a more general problem w/CAS32?
	    J2cValue obj = (J2cValue) inputRegister[0];
	    if (obj == J2cValue.NULL
		|| ((J2cReference) obj).t.includesNull()) {
		evals[1] = evals[0];
		evals[0] = new J2cVoid(new InvocationExp("NULLCHECK",
							 obj,
							 ctx.voidBP));
		evals[2] = J2cValue.TERMINATOR;
	    }
	}
    }

    private J2cValue initializeBlueprint(S3Blueprint ibp) {
	if (ctx.needsInit(ibp) &&
	    // FIXME: shouldn't interfaces be initialized already
	    (ibp.getType().isInterface() || !homeBp.isSubtypeOf(ibp))) {
	    J2cValue shSt = new SharedStateReference(null,
						     ibp.getSharedState());
	    return new J2cVoid(new ClinitExp
			       (ibp, mapIr(new CSACallExp
					   ("initializeBlueprint", shSt))));
	} else {
	    return J2cValue.TERMINATOR;
	}
    }
	
    private void visitResolving(Instruction.ConstantPoolLoad i) {
	popInputs(i);
	Value idx = (Value) i.istreamIns[0];
	ValueSource exp = new CPAccessExp(idx);
	outputRegister[0] = j2cCanonicalize(new Value(exp));
	if (ctx.domain != ctx.executiveDomain) {
	    if (outputRegister[0] instanceof SharedStateReference) {
		S3Blueprint sbp =
		    ((J2cValue) outputRegister[0]).getBlueprint(null);
		evals[0] = initializeBlueprint((S3Blueprint) sbp.getInstanceBlueprint());
	    }
	    else if (((J2cValue) outputRegister[0]).getBlueprint(ctx.domain)
		     == ctx.stringBP) {
		Oop v = ((ConcreteScalar) outputRegister[0]).value;
		RepositoryString rs =
		    (RepositoryString) VM_Address.fromObject(v).asObject();
		ConcreteScalar slot = new ConcreteScalar(null, v, ctx.OopBP);
		IntValue idxVal = ConcreteIntValue.make(rs.getUtf8Index());
		ValueSource alloc1 = mapIr(new CSACallExp("resolveUTF8",
							 idxVal));
		J2cValue alloc = new J2cReference(alloc1,
						  ctx.OopBP, 
						  false,
						  false);
			
                // aastoreBarrier source offset target (source+offset := target)
                // source [Oop] is domain.j2cRoots array
                // offset [int] is the index where to store it times size of element to
                //   store to
                // target [Oop] is the reference to be stored
                
                int slotIdx = FieldMagic._.findRoot(ctx.domain, slot.value);

		evals[0]
		    = new J2cVoid(new IfCmd
				  (new J2cInt(new UnaryExp("!", slot),
					      TypeCodes.INT),
                                  /* insert barrier here for image protection !*/
                                  /* this however BREAKS SPLIT REGION MANAGER */
				  // new J2cVoid(new AssignmentExp(slot, alloc))
				  
				  // well, we just need image barrier, but we don't want store check and
				  // we don't want Dijkstra barrier(the string is just allocated, so it's black, so it doesn't get 
				  // swept) ; we don't want Yuasa barrier, because we know the old value is null
				  // why don't we just mark the roots array dirty in the image from one place, programmatically ?
				   new J2cVoid(new AssignmentExp(slot, alloc))
                    ));
				  
				   
                                   //new J2cVoid(mapIr(
                                   
/*                                   
                                    new CSACallExp("aastoreBarrier", 
                                        new J2cReference(new RootsArrayBaseAccessExp(),ctx.OopBP), 
                        //              ConcreteIntValue.make(slotIdx*MachineSizes.BYTES_IN_ADDRESS) , 
					new J2cInt(new RootsArrayOffsetExp(slotIdx),TypeCodes.INT),
                                      alloc)))
*/                                      						  				   
				  // ));
	    } 
	}
	getFrame().push(outputRegister[0]);
    }
    public void visit(Instruction.LDC i) {
	visitResolving(i);
    }
    public void visit(Instruction.LDC_W i) {
	visitResolving(i);
    }

    public void visit(Instruction.ConstantPoolLoad i) {
	visit((Instruction) i);
    }

    public void visit(Instruction.AALOAD i) { visit((Instruction) i); }

    public void visit(Instruction.WIDE i) {
	cur = i.specialize(buf);
	super.visit(i);
    }

    // What about local variable accesses?
    
    public void visit(Instruction.ROLL r) {
	Frame frame = getFrame();
	int span = r.getSpan(buf);
	int count = r.getCount(buf);
	if (J2cImageCompiler.KEEP_LOGS)
	    log("roll(" + span + ", " + count + ")");
	setArgumentRegisterCount(span);
	for (int i = span - 1; i >= 0; i--) {
	    setArgumentRegisterAt(i, frame.pop());
	    log("popped " + getArgumentRegisterAt(i));
	}
	for (int i = 0; i < span; i++) {
	    frame.push(getArgumentRegisterAt((i + span - count) % span));
	    log("pushed " + getArgumentRegisterAt((i + span - count) % span));
	}
    }

    public void visit(Instruction.COPY c) {
	Frame frame = getFrame();
	int offset = c.getOffset(buf);
	if (J2cImageCompiler.KEEP_LOGS)
	    log("copy(" + offset  + ")");
	setArgumentRegisterCount(offset); 
	for (int i = offset - 1; i >= 0; i--) {
	    setArgumentRegisterAt(i, frame.pop());
	    log("popped " + getArgumentRegisterAt(i));
	}
	for (int i = 0; i < offset; i++) {
	    frame.push(getArgumentRegisterAt(i));
	    log("pushed " + getArgumentRegisterAt(i));
	}
	for (int i = 0; i < offset; i++) {
	    frame.push(getArgumentRegisterAt(i));
	    log("pushed " + getArgumentRegisterAt(i));
	}
		
    }

    // Deal with varargs the same way INVOKE* does in
    // S3AbstractInterpreter 
    public void visit(Instruction.Invocation_Quick i) {
	Frame frame = getFrame();
	// We are going to have to expect nulls in the argument
	// registers!
	parseImmediates(i);
	int argCount = i.getArgumentLengthInWords(buf);
	setArgumentRegisterCount(argCount+1);
	if (J2cImageCompiler.KEEP_LOGS)
	    mc.log(argCount + " arg words: "
		     + i.getArgumentCount(buf) + "/"
		     + i.getWideArgumentCount(buf));
	// pops argCount (last) through receiver (0)
	for (int idx = argCount; idx > 0; idx--)
	    setArgumentRegisterAt(idx, frame.pop());
	AbstractValue r = frame.pop();
	setArgumentRegisterAt(0, r);
	valueMap.put(i.stackIns[1], r);

	if (r instanceof J2cNull) {
	    evals[0] = j2cCanonicalize(Instruction.throwException(T.NULL_POINTER_EXCEPTION));
	    mc.log("null receiver");
	    executionTerminates = true;
	} else {
	    handleBody(i);
	    if (!(evals[0] instanceof J2cVoid)) {
		J2cValue v = evals[0];
		evals[0] = J2cValue.TERMINATOR;
		outputRegister[0] = v;
		if (v instanceof WidePrimitive)
		    frame.pushWide(v);
		else
		    frame.push(v);
	    }
	}
    }

    // Fix LinkSetAccess!
    S3Blueprint linkedBP(IntValue iv) {
	J2cValue v = j2cCanonicalize(iv);
	if (v.isConcrete())
	    return resolveClassAt(((J2cInt) v).intValue());
	else
	    throw die("can't resolve blueprint " + v);
    }
    S3Blueprint linkedBP(Instruction.ConstantPoolRead i) {
	return linkedBP((IntValue)i.istreamIns[0]);
    }

    J2cValue typeOutput(Instruction i, S3Blueprint bp) {
	if (executionTerminates)
	    return null;
	// S3Blueprint bp = linkedBP(i);
	J2cValue v = typeOutput(i.stackOuts[0].source, bp);
	valueMap.put(i.stackOuts[0].source, v);
	return v;
    }

    J2cValue typeOutput(ValueSource exp, S3Blueprint bp) {
	J2cValue v = makeValue(mapIr(exp), bp.getType(), false);
	outputRegister[0] = v;
	if (v instanceof WidePrimitive)
	    getFrame().pushWide(v);
	else
	    getFrame().push(v);
	return v;
    }

    public void visit(Instruction.NEW_QUICK i) {
	popInputs(i);
	S3Blueprint bp = linkedBP(i);
	ValueSource vs = mapIr(i.stackOuts[0].source);
	outputRegister[0] = new J2cReference(vs, bp, true, false);
	getFrame().push(outputRegister[0]);
    }

    // FIXME: Why am I defining methods on both new and new_quick?  Is
    // the second one actually used?
    public void visit(Instruction.NEW i) {
	popInputs(i);
	S3Blueprint bp = resolveClassAt(i.getCPIndex(buf));
	assert(evals[0] == J2cValue.TERMINATOR);
	evals[0] = initializeBlueprint(bp);
	ValueSource alloc = new CSACallExp("allocateObject",
					   new BlueprintReference(bp));
	outputRegister[0] = new J2cReference(mapIr(alloc), bp, true, false);
	getFrame().push(outputRegister[0]);
    }

    public void visit(Instruction.LOAD_SHST_FIELD i) {
	try {
	    int cpindex = i.getCPIndex(buf);
	    ConstantResolvedStaticFieldref rinfo =
		dcp.resolveStaticField(cpindex);
	    Oop shst = rinfo.getSharedState();
	    if (ctx.domain != ctx.executiveDomain) {
		evals[0] = initializeBlueprint
		    ((S3Blueprint)
		     shst.getBlueprint().getInstanceBlueprint());
	    }
	    SharedStateReference shstRef =
		new SharedStateReference(null, shst);
	    outputRegister[0] = shstRef;
	    getFrame().push(shstRef);
	} catch (LinkageException e) { 
	    throw e.unchecked(); 
	}
    }

    public void visit(Instruction.LOAD_SHST_METHOD i) {
	try {
	    int cpindex = i.getCPIndex(buf);
	    ConstantResolvedStaticMethodref rinfo =
		dcp.resolveStaticMethod(cpindex);
	    Oop shst = rinfo.getSharedState();
	    if (ctx.domain != ctx.executiveDomain) {
		evals[0] = initializeBlueprint
		    ((S3Blueprint)
		     shst.getBlueprint().getInstanceBlueprint());
	    }
	    SharedStateReference shstRef =
		new SharedStateReference(null, shst);
	    outputRegister[0] = shstRef;
	    getFrame().push(shstRef);
	} catch (LinkageException e) { 
	    throw e.unchecked(); 
	}
    }

    public void visit(Instruction.ANEWARRAY_QUICK i) {
	popInputs(i);
	S3Blueprint bp = linkedBP(i);
	ValueSource alloc = mapIr(i.stackOuts[0].source);
//	System.err.println("ANEWARRAY_QUICK resolved blueprint is " +bp);
	
	outputRegister[0] = new J2cArray(alloc,
					 TypeInfo.make(bp, true, false),
					 (J2cInt) inputRegister[0]);
	getFrame().push(outputRegister[0]);

    }
    public void visit(Instruction.MULTIANEWARRAY_QUICK i) {
	parseImmediates(i);

	int count = i.getDimensions(buf);
	if (count > mc.maxMultiArray)
	    mc.maxMultiArray = count;
	setArgumentRegisterCount(count);
	Frame frame = getFrame();
	for (int idx = count - 1; idx >= 0; idx--)
	    setArgumentRegisterAt(idx, frame.pop());
	Value dims = new J2cArray(new DimensionArrayExp(argumentRegisters,
							count),
				  ctx.intArrayBP,
				  J2cValue.makeIntConstant(TypeCodes.INT, count));
	S3Blueprint rt = linkedBP(i);
	ValueSource call = new CSACallExp("allocateMultiArray",
					  new BlueprintReference(rt),
					  J2cValue.makeIntConstant(TypeCodes.INT, 0),
					  dims);
	outputRegister[0] = new J2cArray(mapIr(call),
					 TypeInfo.make(rt, true, false),
					 (J2cInt) getArgumentRegisterAt(0));
	frame.push(outputRegister[0]);
    }
    public void visit(Instruction.CHECKCAST_QUICK i) {
	popInputs(i);
	// OK.  How do I indicate that the stack type of
	// inputRegister[0] has changed?
	S3Blueprint bp = linkedBP(i);
	J2cValue v;
	ValueSource vs = new ConversionExp((J2cValue) inputRegister[0]);

	if (bp instanceof Blueprint.Array)
	    v = new J2cArray(vs, bp);
	else
	    v = new J2cReference(vs, bp);
	outputRegister[0] = v;
	getFrame().push(v);
    }

    /* assumes asAddress was run on the argument 
       - J2cInt converted to VM_Address */
       
    private int examineReference(J2cValue v) {
      return getReferenceAssertions(ctx, v);
    }   
    public static int getReferenceAssertions(Context ctx, J2cValue v) {
    
      int assertions = 0;
      
      if (v instanceof J2cNull) {
        return Assert.NULL;
      }
      
      J2cReference arg = (J2cReference)v;
      S3Blueprint bp = arg.getBlueprint();

      if (bp == null) {
        System.err.println("Strange - null blueprint in getReferenceAssertions...");
        return 0;
      }
      
      if (bp == ctx.VM_AddressBP) {
        assertions |= Assert.ADDRESS;
      }
      
      TypeInfo ti = arg.getTypeInfo();
      if (!ti.includesNull()) {
        assertions |= Assert.NONNULL;
      }

//FIXME:  
// This does not work: it regards types such as Class as heap only, but they can contain structures returned from
//	RuntimeExports (classFor) as Oop, but handled as Class
//      if ((bp!=ctx.OpaqueBP) && (!bp.getDomain().isExecutive()) && (bp!=ctx.VM_AddressBP) && (!bp.isSharedState())) {

//  doesn't help, either ... 
//  we would probably have to enumerate user domain types that are not heap only, but it would be quite a hassle... 
//  maybe we could also remember image allocated types

//      if ((bp!=ctx.OpaqueBP) && (!bp.getDomain().isExecutive()) && (bp!=ctx.VM_AddressBP) && (!bp.isSharedState()) && ctx.anal.isHeapAllocated(bp)) {
//        assertions |= Assert.HEAPONLY;
//      }
    
      //System.err.println("in getReferenceAssertions: v is "+v+" bp is "+bp+" v.isConcrete() is "+v.isConcrete());
    
    
      if ( bp.isSubtypeOf(ctx.bpbp) || bp.isSharedState() ) { // isHeapAllocated fails on blueprints, so this is not only an optimization
          assertions |= Assert.IMAGEONLY;
      } else {
        if ( (bp!=ctx.VM_AddressBP) && !bp.isSubtypeOf(ctx.OopBP) && (bp!=ctx.OpaqueBP) && !ctx.anal.isHeapAllocated(bp) ) {
          assertions |= Assert.IMAGEONLY;
        }
      }
      
      if (bp instanceof Blueprint.Array) {
        assertions |= Assert.ARRAY;
        
        J2cArray array = (J2cArray)v;
        J2cInt[] dims = array.getDimmensions();
        if (dims[0].isConcrete()) {
          assertions |= Assert.ARRAY_LENGTH_KNOWN;
          
          Blueprint.Array abp = (Blueprint.Array) array.getBlueprint();
          int componentSize = abp.getComponentSize();
          
          if (MemoryManager.the().usesArraylets() && (dims[0].intValue()*componentSize < MemoryManager.the().arrayletSize())) {
            assertions |= Assert.ARRAY_UP_TO_SINGLE_ARRAYLET;
          }
        }
        
        
      }
      
      if (bp instanceof Blueprint.Scalar) {
        assertions |= Assert.SCALAR;
      }
      
      return assertions;
    }

    public static String referenceComment(int a, Blueprint bp) {
    
      return "/* "+
        (((a&Assert.NONNULL)!=0) ? "NONNULL " : "")  +
        (((a&Assert.NULL)!=0) ? "NULL " : "")  +
        (((a&Assert.IMAGEONLY)!=0) ? "IMAGEONLY " : "")  +
        (((a&Assert.HEAPONLY)!=0) ? "HEAPONLY " : "")  +        
        (((a&Assert.SCALAR)!=0) ? "SCALAR " : "")  +    
        (((a&Assert.ARRAY)!=0) ? "ARRAY " : "")  +        
        (((a&Assert.ADDRESS)!=0) ? "ADDRESS " : "")  +        
        (((a&Assert.IN_IMAGEONLY_SLOT)!=0) ? "IN_IMAGEONLY_SLOT " : "")  +        
        bp +
        " */";
    }
 /* 
 doesn't work....
    
    public void visit(Instruction.READ_BARRIER i) {    
      popInputs(i);
      
      J2cValue v = asAddress( (Value) inputRegister[0] );      
      
      if (v instanceof J2cNull) {
        // evals[0] = J2cValue.TERMINATOR;
        evals[0] =  new J2cVoid( mapIr( new CSACallExp("emptyCall") ));
        System.err.println("Skipping read barrier for NULL..\n");
      } else {
        J2cReference r = (J2cReference) v;
        S3Blueprint bp = r.getBlueprint();
        
        if (bp == ctx.VM_AddressBP) {
//          evals[0] = J2cValue.TERMINATOR;
          evals[0] =  new J2cVoid( mapIr( new CSACallExp("emptyCall") ));
          System.err.println("Skipping read barrier for VM_Address..\n");
        } else {
          evals[0] = new J2cVoid( mapIr(new CSACallExp("readBarrier",r)));
        }
      }
    }
 */   
 
 /*
    private J2cValue makeArrayElement(Instruction.ArrayAccess i,ValueSource vs) {
      if ( (i instanceof Instruction.AALOAD) ||
           (i instanceof Instruction.AASTORE) ) {
           
        return new J2cReference(vs);
      }
      if ( (i instanceof Instruction.BALOAD) ||
           (i instanceof Instruction.BASTORE) ) {
           
        return new J2cInt(vs, TypeCodes.BOOLEAN);
      }
      if ( (i instanceof Instruction.CALOAD) ||
           (i instanceof Instruction.CASTORE) ) {
           
        return new J2cInt(vs, TypeCodes.CHAR);
      }
      if ( (i instanceof Instruction.DALOAD) ||
           (i instanceof Instruction.DASTORE) ) {
           
        return new J2cDouble(vs);
      }
      if ( (i instanceof Instruction.FALOAD) ||
           (i instanceof Instruction.FASTORE) ) {
           
        return new J2cFloat(vs);
      }
      if ( (i instanceof Instruction.IALOAD) ||
           (i instanceof Instruction.IASTORE) ) {
           
        return new;
      }
      if ( (i instanceof Instruction.SALOAD) ||
           (i instanceof Instruction.SASTORE) ) {
           
        return 'i';
      }
      if ( (i instanceof Instruction.LALOAD) ||
           (i instanceof Instruction.LASTORE) ) {
           
        return 'l';
      }
      throw new RuntimeException("unsupported instruction");    
    }
 
    private char getArrayTag(Instruction.ArrayAccess i) {
      if ( (i instanceof Instruction.AALOAD) ||
           (i instanceof Instruction.AASTORE) ) {
           
        return 'a';
      }
      if ( (i instanceof Instruction.BALOAD) ||
           (i instanceof Instruction.BASTORE) ) {
           
        return 'b';
      }
      if ( (i instanceof Instruction.CALOAD) ||
           (i instanceof Instruction.CASTORE) ) {
           
        return 'c';
      }
      if ( (i instanceof Instruction.DALOAD) ||
           (i instanceof Instruction.DASTORE) ) {
           
        return 'd';
      }
      if ( (i instanceof Instruction.FALOAD) ||
           (i instanceof Instruction.FASTORE) ) {
           
        return 'f';
      }
      if ( (i instanceof Instruction.IALOAD) ||
           (i instanceof Instruction.IASTORE) ) {
           
        return 'i';
      }
      if ( (i instanceof Instruction.SALOAD) ||
           (i instanceof Instruction.SASTORE) ) {
           
        return 'i';
      }
      if ( (i instanceof Instruction.LALOAD) ||
           (i instanceof Instruction.LASTORE) ) {
           
        return 'l';
      }
      throw new RuntimeException("unsupported instruction");
    }

    // arrayref, index
    public void visit(Instruction.ArrayLoad i) {
    
      popInputs(i);
      
      if ( asAddress( (Value) inputRegister[0] ) instanceof J2cNull) {
        System.err.println("DO FIX THE NULLCHECK!");
        return ;
      }
      
      J2cArray array = (J2cArray) j2cCanonicalize(inputRegister[0]);
      int aArray = examineReference(array);
      
      Blueprint.Array bp = array.getBlueprint().asArray();
      int componentSize = bp.getComponentSize(); 
      
      J2cInt index = (J2cInt) j2cCanonicalize(inputRegister[1]);

      ValueSource vs = mapIr(new CSACallExp(getArrayTag(i) + "aloadBarrier",
        new Value[] {
	  array,
	  index,
	  J2cValue.makeIntConstant(TypeCodes.INT, aArray),
	  J2cValue.makeIntConstant(TypeCodes.INT, componentSize) 
        }
      )));      
      
      outputRegister[0] = makeArrayElement(i, vs);
      
      getFrame().push(outputRegister[0]);      
      // get array assertions
      // generate CSA call of particular name
      //	AALOAD, DALOAD, BALOAD, CALOAD, IALOAD, ...
      // call arguments:
      //	Oop array, int index, int aArray, int minIndex, int maxIndex
      //	the safe (but not fast) defaults are  0, 0, MAX_INT
      //
      //	or maybe only give the index assertion if it is a known (single) values
      //		because it seems unlikely that the interval will be useful
      //
      //	but also give array length assertion	
    
    }
    
    public void visit(Instruction.ArrayStore i) {
    
      // get array assertions
      // generate CSA call like for array load 
      // 	if it's aastore, get also assertions for the pointer
      //	- so aastore will have more arguments
    }
 */
    /*
    public void visit(Instruction.PUTFIELD_QUICK_WITH_BARRIER_REF i) {
    
      popInputs(i);
      
      // FIXME:
      //   it should be true that this cannot be J2cNull, because there must be a nullcheck before 
      //   that.. but, we have to fix the nullcheck instruction first...
      if ( asAddress( (Value) inputRegister[1] ) instanceof J2cNull) {
        System.err.println("DO FIX THE NULLCHECK!");
        return ;
      }
      J2cReference vTargetObject = (J2cReference) asAddress( (Value) inputRegister[1] );  /* cannot be J2cNull */
    /*  J2cValue vNewPointer = asAddress( (Value) inputRegister[0] );
      
      int aTargetObject = examineReference(vTargetObject) | Assert.NONNULL;
      int aNewPointer = examineReference(vNewPointer);
      
      //IntValue offset = (IntValue)i.istreamIns[0];
      J2cValue vOffset = j2cCanonicalize((IntValue)i.istreamIns[0]);

      Blueprint bp = vTargetObject.getBlueprint();
      
      // FIXME: refactor this so that it does not duplicate code from J2cImageCompiler..
      if (vOffset.isConcrete() && (bp instanceof Blueprint.Scalar) && (bp.isSharedState() || ctx.anal.isConcrete(bp))) {
        int offset = ((J2cInt)vOffset).intValue();

        Field f = ((S3Blueprint)bp).fieldAt(offset);
        if ((f!=null) && (bp.isSharedState() || ctx.anal.isConcrete(bp))) {
          try {
            Blueprint fbp = ctx.blueprintFor(f.getType());
            if ( (fbp != ctx.OopBP) && (fbp != ctx.OpaqueBP) && (fbp == null || !ctx.anal.isHeapAllocated(fbp)) ) {
              aTargetObject |= Assert.IN_IMAGEONLY_SLOT;
            }
          } catch (LinkageException e) {
            throw e.unchecked();
          }	        
        }
      }
      
      evals[0] = new J2cVoid( mapIr(new CSACallExp("putFieldBarrier",
                                        new Value[] {
					  vTargetObject,
					  vOffset,
					  vNewPointer,
					  J2cValue.makeIntConstant(TypeCodes.INT, aTargetObject),
					  J2cValue.makeIntConstant(TypeCodes.INT, aNewPointer) 
                                        }
      )));
      //evals[1] = J2cValue.TERMINATOR;					
    }
*/

    // FIXME: image-only slots cannot be handled here, as we don't know 
    //   the fields anymore (i.e. for GETFIELD ..)
    
    void addTranslatingReadBarrier(int aMask) {
    
      J2cValue v = asAddress( (Value) inputRegister[0] );
      int a = examineReference(v) | aMask;
      
      if ( ((a&Assert.NULL)!=0) ) {
        outputRegister[0] = v;
      } else {
        // this assertion probably should not hold in general, but it helped me to find many bugs..
        // note that there are native functions returning Java pointers as VM_Addresses, such as
        // getEngineSpecificPtrLoc of PtrStackLocalReferenceIterator ... 
        // 
        // this can happen in case of comparison of VM_Addresses (pointers...)
        // assert( (a&Assert.ADDRESS)==0 );
        //
        // now - sometimes we want the translations
        //     - when the VM_Address is an address of a java object
        //     - sometimes we DO NOT - VM_Address operations in the GC (comparison!, etc)
        //
        // In general, returning a java pointer from the C space as a VM_Address should
        //  include a translating barrier, like VM_Address.fromObject(...) does
        // However, the barrier should not ever translate VM_Address...
                
        J2cReference arg = (J2cReference)v;
        
        if ((a&Assert.ADDRESS)!=0) {
          outputRegister[0] = arg.copy(new ValueAccessExp(arg)); 
        } else {
        
          S3Blueprint bp = arg.getBlueprint();
          TypeInfo ti = arg.getTypeInfo();

          J2cReference refv = new J2cReference(new CCastExp("HEADER * "+referenceComment(a,bp), arg), bp, ti.exactTypeKnown(), ti.includesNull());
          //J2cValue refv = arg;
        
          // FIXME: this is so retarted, why cannot VM_Address be represented consistently ?
          // either as interger, or as VM_Address referefence ?
          // I think the reference is only created by some forgotten? FIAT insertion after method return
/*        
        if ( inputRegister[0] instanceof J2cInt ) {
          refv = new J2cReference(new CCastExp("HEADER *", arg), bp, ti.exactTypeKnown(), ti.includesNull());
          assert( (a&Assert.ADDRESS) !=0 );
        } else {
          // assert( (a&Assert.ADDRESS)==0 ); // doesn't hold !! 
        }
*/        
          ValueSource vs = mapIr(new CSACallExp("checkingTranslatingReadBarrier", 
            refv,
            J2cValue.makeIntConstant(TypeCodes.INT, a)
          ));
        
        
          if ( (a&Assert.ARRAY) != 0 ) {
          
            // FIXME: use this when nullchecks are fixed
            //outputRegister[0] = new J2cArray(vs, bp, ti.exactTypeKnown(),  (a&Assert.NONNULL)==0);  
            outputRegister[0] = new J2cArray(vs, bp, ti.exactTypeKnown(), ti.includesNull());  
          
   /*     } else if ( inputRegister[0] instanceof J2cInt ) {
          outputRegister[0] = new J2cInt(vs, TypeCodes.INT); */
          
          } else {
            assert( (a&Assert.SCALAR) != 0 );
            // FIXME: use this when nullchecks are fixed
            //outputRegister[0] = new J2cReference(vs, bp, ti.exactTypeKnown(), (a&Assert.NONNULL)==0 );
            outputRegister[0] = new J2cReference(vs, bp, ti.exactTypeKnown(), ti.includesNull() );
          }
        }
      }
    
      getFrame().push(outputRegister[0]); 
    }
    
    public void visit(Instruction.NONCHECKING_TRANSLATING_READ_BARRIER i) {
      popInputs(i);
      addTranslatingReadBarrier(Assert.NONNULL);
    }

    public void visit(Instruction.CHECKING_TRANSLATING_READ_BARRIER i) {
      popInputs(i);
      addTranslatingReadBarrier(0);
    }

/*
    public void visit(Instruction.NONCHECKING_TRANSLATING_READ_BARRIER i) {
    
        popInputs(i);
      
        if (inputRegister[0] instanceof J2cNull) {
          
          outputRegister[0] = inputRegister[0];
          // this is not correct, because there is sometimes code which intentionally / always runs into 
          // NPE, and then there is a nullcheck between this barrier, but the nullcheck at this level
          // is not rewritten to exception throw (it's done in CodeGen..)
          
          // throw die("Always null value (J2cNull) passed to non-checking translating read barrier.\n");
          
        } else if (inputRegister[0] instanceof J2cInt) {

          J2cInt arg = (J2cInt) inputRegister[0];
          outputRegister[0] = arg.copy(new ValueAccessExp(arg));                  

        } else {
        
          J2cReference arg = (J2cReference) inputRegister[0];
          S3Blueprint bp = arg.getBlueprint();

          // via csa 
  	  ValueSource vs = mapIr(new CSACallExp("nonCheckingTranslatingReadBarrier", arg));

          // via macro
//          ValueSource vs = new InvocationExp("TRANSLATE_PTR", arg, bp);

	  J2cValue v;
	  TypeInfo ti = arg.getTypeInfo();
	
	  boolean heapAllocated = ctx.anal.isHeapAllocated(bp);
	  
	  if (bp == ctx.VM_AddressBP ) {	
	    v = arg.copy(new ValueAccessExp(arg));
          } else if (bp instanceof Blueprint.Array) {
            if (!heapAllocated) {
              // FIXME: what about array of Oop or Opaque ?
              v = arg.copy(new ValueAccessExp(arg));
            } else {
	      v = new J2cArray(vs, bp, ti.exactTypeKnown(), ti.includesNull());
            }
          } else {
            // FIXME: can't we do better ?
            // just make sure it's not Oop and Opaqua          
            if ((!heapAllocated) && (bp instanceof Blueprint.Scalar)
             		    && (bp.isSharedState() || ctx.anal.isConcrete(bp))) {
              v = arg.copy(new ValueAccessExp(arg));;
            } else {
	      v = new J2cReference(vs, bp, ti.exactTypeKnown(), ti.includesNull());
            }
          }
	    
	  outputRegister[0] = v;
      }
      
      getFrame().push(outputRegister[0]);
    }    

    // FIXME: re-factor the two barriers into a common visitor
    
    public void visit(Instruction.CHECKING_TRANSLATING_READ_BARRIER i) {

        popInputs(i);
      
        if (inputRegister[0] instanceof J2cNull) {
          outputRegister[0] = inputRegister[0];
          
        } else if (inputRegister[0] instanceof J2cInt) {
        
          J2cInt arg = (J2cInt) inputRegister[0];
          outputRegister[0] = arg.copy(new ValueAccessExp(arg));
                
        } else {
        
          J2cReference arg = (J2cReference) inputRegister[0];
          S3Blueprint bp = arg.getBlueprint();

          J2cValue v;
	  TypeInfo ti = arg.getTypeInfo();
	
	  String csaCall = null;
	  if (ti.includesNull()) {
	    csaCall = "checkingTranslatingReadBarrier";
          } else {
            csaCall = "nonCheckingTranslatingReadBarrier";
          }
          // via csa 
  	  ValueSource vs = mapIr(new CSACallExp(csaCall, arg));

	  boolean heapAllocated = ctx.anal.isHeapAllocated(bp);
	  
	  if (bp == ctx.VM_AddressBP) {	
	    v = arg.copy(new ValueAccessExp(arg));
          } else if (bp instanceof Blueprint.Array) {
            if (!heapAllocated) {
              // FIXME: what about array of Oop or Opaque ?            
              v = arg.copy(new ValueAccessExp(arg));
            } else {          
  	      v = new J2cArray(vs, bp, ti.exactTypeKnown(), ti.includesNull());
            }
          } else {
            // FIXME: can't we do better ?
            // just make sure it's not Oop and Opaqua
            if ((!heapAllocated) && (bp instanceof Blueprint.Scalar)
             		    && (bp.isSharedState() || ctx.anal.isConcrete(bp))) {
              v = arg.copy(new ValueAccessExp(arg));;
            } else {
	      v = new J2cReference(vs, bp, ti.exactTypeKnown(), ti.includesNull());
            }
          }

	  outputRegister[0] = v;
      }
      
      getFrame().push(outputRegister[0]);
    }    
*/

/*
null-checking version
     public void visit(Instruction.TRANSLATING_READ_BARRIER i) {
    
        popInputs(i);
      
      	J2cReference arg = (J2cReference) inputRegister[0];
      	S3Blueprint bp = arg.getBlueprint();

	ValueSource vs = mapIr(new CSACallExp("translatingReadBarrier", arg));

	J2cValue v;
	if (bp instanceof Blueprint.Array)
	    v = new J2cArray(vs, bp, false, false);
	else
	    v = new J2cReference(vs, bp, false, false);
	    
	outputRegister[0] = v;
      
      getFrame().push(outputRegister[0]);
    }    
    
  */  
    public void visit(Instruction.INB i) {
    
      popInputs(i);
      outputRegister[0] = callI( new InvocationExp("ovm_inb",inputRegister, 1, true, ctx.blueprintFor(ctx.domain.INT)));

      getFrame().push(outputRegister[0]);
    }


    public void visit(Instruction.OUTB i) {
      popInputs(i);
      evals[0] = callV( new InvocationExp("ovm_outb", inputRegister, 2 , true, ctx.blueprintFor(ctx.domain.VOID)));
      
    }
    
    public void visit(Instruction.AFIAT i) {
	popInputs(i);
	// FIXME: double resolution
	
	S3Blueprint bp = resolveClassAt(i.getCPIndex(buf));
	ValueSource vs = mapIr(i.stackOuts[0].source);
	J2cValue inner = (J2cValue) ((ReinterpretExp) vs).before;
	J2cValue v;

	S3Blueprint iType = inner.getBlueprint(ctx.domain);
	if (iType != ctx.VM_AddressBP && !iType.isSubtypeOf(ctx.OopBP)
	    && bp != ctx.VM_AddressBP && bp != ctx.OopBP
	    && iType.isSubtypeOf(bp))
	    // If we simply return inner, we will try to redeclare the
	    // stack slot of our operand, better to copy it to a
	    // second stack slot, and let copy propagation do its thing.
	    v = inner.copy(new ValueAccessExp(inner));
	else if (bp instanceof Blueprint.Array)
	    v = new J2cArray(vs, bp);
	else {
// this doesn't work - it blocks running ovm
// why ???	
/*	    if (inner instanceof J2cReference) {
	      J2cReference r = (J2cReference)inner;
	      v = new J2cReference(vs, bp, false, r.getTypeInfo().includesNull());
	    } else if (inner instanceof J2cNull) {
	      v = J2cValue.NULL;
	    } else if (inner instanceof J2cInt) {
              Integer ivalue = (Integer) ((J2cInt)inner).concreteValue();
              if (ivalue != null) {
                if (ivalue.intValue()==0) {
                  v = J2cValue.NULL;
                } else {
                  v = new J2cReference(vs, bp, false, false);
                }
              } else {
                J2cInt nullInt = (J2cInt) J2cValue.makeIntConstant(J2cValue.SIZE_T_CODE, 0);
                if ( ! ((J2cInt)inner).includes( nullInt ) ) {
                  v = new J2cReference(vs, bp, false, false);
                } else {
                  v = new J2cReference(vs, bp, false, true);
                }
              }
            } else {  
              // ever reached ? */
              v = new J2cReference(vs, bp);
/*            }*/
        }
	outputRegister[0] = v;
	getFrame().push(v);
    }

    // INSTANCEOF_QUICK is an important one for 
//     public void visit(Instruction.INSTANCEOF_QUICK i) {
//     }
//     public void visit(Instruction.REF_PUTFIELD_QUICK i) {
//     }

    void bt(Selector.Method s) {
	throw new LinkageException
	    ("calls undefined method: " + s).unchecked();
    }
    void bt(Selector.Field s) {
	throw new LinkageException
	    ("references udefined field: " + s).unchecked();
    }
    void bt(TypeName t) {
	throw new LinkageException
	    ("references udefined type: " + t).unchecked();
    }
    
    public void visit(Instruction.GETFIELD i)  { bt(i.getSelector(buf, cp)); }
    public void visit(Instruction.GETSTATIC i) { bt(i.getSelector(buf, cp)); }
    public void visit(Instruction.PUTFIELD i)  { bt(i.getSelector(buf, cp)); }
    public void visit(Instruction.PUTSTATIC i) { bt(i.getSelector(buf, cp)); }
    protected void visitInvocation(Instruction.Invocation i) {
	bt(i.getSelector(buf, cp));
    }
    public void visit(Instruction.INVOKESTATIC i) { bt(i.getSelector(buf, cp)); }
    public void visit(Instruction.CHECKCAST i)    { bt(i.getResultTypeName(buf, cp)); }
    public void visit(Instruction.INSTANCEOF i)   { bt(i.getTypeName(buf, cp)); }
    public void visit(Instruction.ANEWARRAY i)    { bt(i.getClassName(buf, cp)); }
    public void visit(Instruction.SINGLEANEWARRAY i) {
	bt(i.getArrayName(buf, cp));
    }
    public void visit(Instruction.MULTIANEWARRAY i) {
	bt(i.getClassName(buf, cp));
    }
    
    

    public J2cValue callV(InvocationExp exp) {
	return new J2cVoid(exp);
    }
    public J2cValue callI(ValueSource exp) {
	return new J2cInt(exp, TypeCodes.INT);
    }

    J2cValue repackJLong(ValueSource callExp, TypeName n) {
	char t = n.getTypeTag();
	ValueSource toJValue
	    = new InvocationExp("to_jvalue_jlong",
				new J2cLong(callExp, null, TypeCodes.LONG),
				null);
	ValueSource callJValue
	    = new JValueExp(new InternalReference(toJValue));
	switch (t) {
	case TypeCodes.ARRAY:
	case TypeCodes.OBJECT:
	    try {
		S3Blueprint bp  = (S3Blueprint) ctx.domain.blueprintFor
		    (n.asCompound(),
		     ctx.domain.getSystemTypeContext());
		if (t == TypeCodes.OBJECT)
		    return new J2cReference(callJValue, bp);
		else
		    return new J2cArray(callJValue, bp);
	    } catch (LinkageException e) { throw e.unchecked(); }
	case TypeCodes.FLOAT:
	    return new J2cFloat(callJValue, null);
	case TypeCodes.DOUBLE:
	    return new J2cDouble(callJValue, null);
	case TypeCodes.LONG:
	    return new J2cLong(callJValue, null, TypeCodes.LONG);
	default:
	    return new J2cInt(callJValue, t);
	}
    }
	
    J2cValue jlongToType(ValueSource callExpr, TypeName n) {
	char t = n.getTypeTag();
	ValueSource callJValue
	    = new InvocationExp("to_"+J2cValue.typeCodeToCtype[t]+"_jlong",
				new InternalReference(callExpr),
				null);
	switch (t) {
	case TypeCodes.ARRAY:
	case TypeCodes.OBJECT:
	    try {
		S3Blueprint bp  = (S3Blueprint) ctx.domain.blueprintFor
		    (n.asCompound(),
		     ctx.domain.getSystemTypeContext());
		if (t == TypeCodes.OBJECT)
		    return new J2cReference(callJValue, bp);
		else
		    return new J2cArray(callJValue, bp);
	    } catch (LinkageException e) { throw e.unchecked(); }
	case TypeCodes.FLOAT:
	    return new J2cFloat(callJValue, null);
	case TypeCodes.DOUBLE:
	    return new J2cDouble(callJValue, null);
	case TypeCodes.LONG:
	    return new J2cLong(callJValue, null, TypeCodes.LONG);
	default:
	    return new J2cInt(callJValue, t);
	}
    }

    private interface S extends InvokeSystemArguments { }
    private interface W extends WordOps { }
    private interface D extends DereferenceOps { }
    private interface T extends Throwables { }
    
    // Umm, what are the argument and return types for these guys?
    // Certainly word ops are easy: they can be expressed in terms of
    // the IR.
    public void visit(Instruction.INVOKE_SYSTEM i) {
	int pc = getPC();
	Frame frame = getFrame();
	int call = i.getMethodIndex(buf);
	int sub = i.getOpType(buf);
	int iidx = 0;
	int eidx = 0;
	int oidx = 0;
	//ValueSource fakeExp = new SymbolicConstant("0");
	
	switch (call) {
	case S.ACTIVE_CONTEXT:
	case S.SWITCH_CONTEXT:
	    mc.log("call " + call + " not implemented in interpreter");
	    break;
	case S.NEW_CONTEXT:
	    inputRegister[iidx++] = frame.pop(); // Context
	    outputRegister[oidx++]
		= callI(new InvocationExp("newNativeContext",
					  inputRegister, iidx, true,
					  ctx.blueprintFor(ctx.domain.INT)));
	    break;
	case S.RUN:
	    inputRegister[iidx++] = frame.pop(); // int (J2cContext*)
	    inputRegister[iidx++] = frame.pop(); // int
	    evals[eidx++] = callV(new InvocationExp
				  ("run", inputRegister, iidx, true,
				   ctx.blueprintFor(ctx.domain.VOID)));
	    break;
	case S.GET_CONTEXT: {
	    inputRegister[iidx++] = frame.pop(); // int
	    ValueSource callExp =
		new InvocationExp("getContext",
				  (J2cValue) inputRegister[0],
				  ctx.blueprintFor(ctx.domain.INT));
	    outputRegister[oidx++] =
		new J2cReference(callExp, ctx.ContextBP, false, false);
	    break;
	}
	case S.GET_NATIVE_CONTEXT: {
	    inputRegister[iidx++] = frame.pop(); // int
	    ValueSource callExp =
		new InvocationExp("getNativeContext",
				  (J2cValue) inputRegister[0],
				  ctx.blueprintFor(ctx.domain.INT));
	    outputRegister[oidx++] =
		new J2cInt(callExp, TypeCodes.INT);
	    break;
	}	
	case S.NATIVE_CONTEXT_TO_CONTEXT: {
	    inputRegister[iidx++] = frame.pop(); // int
	    ValueSource callExp =
		new InvocationExp("nativeContextToContext",
				  (J2cValue) inputRegister[0],
				  ctx.blueprintFor(ctx.domain.INT));
	    outputRegister[oidx++] =
		new J2cReference(callExp, ctx.ContextBP, false, true);
	    break;
	}		
/*	case S.INCREMENT_COUNTER:
	    inputRegister[iidx++] = frame.pop(); // int
	    evals[eidx++]
	        = callV(new InvocationExp("incrementCounter",
	                          (J2cValue) inputRegister[0],
	                          ctx.blueprintFor(ctx.domain.VOID)));
            break;*/
	case S.DESTROY_NATIVE_CONTEXT:
	    inputRegister[iidx++] = frame.pop(); // int (J2cContext*)
	    evals[eidx++]
		= callV(new InvocationExp("destroyNativeContext",
					  inputRegister, iidx, true,
					  ctx.blueprintFor(ctx.domain.VOID)));
	    break;
	case S.SETPOLLCOUNT:	// (J)J
	    mc.log("ignoring setpollcount");
	    break;
	case S.EMPTY_CSA_CALL:
	    evals[eidx++]
		= j2cCanonicalize(new Value(new CSACallExp("emptyCall")));
	    break;
	case S.WORD_OP: {
	    if (sub == W.uI2L) {
		inputRegister[iidx++] = frame.pop();
		outputRegister[oidx++]
		    = new J2cLong(new ConversionExp
				  (new J2cInt(new ConversionExp
					      ((J2cValue) inputRegister[0]),
					      TypeCodes.UINT)),
				  null, TypeCodes.LONG);
		outputRegister[oidx++] = null;
		break;
	    }
	    inputRegister[iidx++] = frame.pop(); // VM_Word
	    inputRegister[iidx++] = frame.pop(); // VM_Word
	    char code  = sub == WordOps.sCMP ? TypeCodes.INT : TypeCodes.UINT;
	    J2cValue v1 = new J2cInt(new ConversionExp((J2cValue)inputRegister[0]),
				     code);
	    J2cValue v2 = new J2cInt(new ConversionExp((J2cValue)inputRegister[1]),
				     code);
	    if (sub == W.sCMP || sub == W.uCMP) {
		outputRegister[oidx++]
		    = new J2cInt(Instruction.buildCMP(v2, v1), TypeCodes.INT);
	    } else {
		String op = (sub == W.uLT ? "<"
			     : sub == W.uLE ? "<="
			     : sub == W.uGE ? ">="
			     : sub == W.uGT ? ">"
			     : sDie("bad WORD_OP " + sub));
		outputRegister[oidx++] = new J2cInt(new CondExp(v2, op, v1),
						    TypeCodes.BOOLEAN);
	    }
	    break;
	}
	case S.DEREFERENCE: {
	    char tag = TypeCodes.SHORT;
	    switch (sub) {
	    case D.getByte:
  		inputRegister[iidx++] = frame.pop();	// VM_Address
  		outputRegister[oidx++]
		    = new J2cInt(new MemExp((J2cValue) inputRegister[0],
					    J2cValue.makeIntConstant(TypeCodes.INT, 0)),
				 TypeCodes.BYTE);
		break;
	    case D.getShort:
		inputRegister[iidx++] = frame.pop();	// VM_Address
		outputRegister[oidx++]
		    = new J2cInt(new MemExp((J2cValue) inputRegister[0],
					    J2cValue.makeIntConstant(TypeCodes.INT, 0)),
				 TypeCodes.SHORT);
		break;
	    case D.getChar:
		inputRegister[iidx++] = frame.pop();	// VM_Address
		outputRegister[oidx++]
		    = new J2cInt(new MemExp((J2cValue) inputRegister[0],
					    J2cValue.makeIntConstant(TypeCodes.INT, 0)),
				 TypeCodes.CHAR);
		break;
	    case D.setByte: tag = TypeCodes.BYTE;
	    case D.setShort:
		inputRegister[iidx++] = frame.pop(); // src
		inputRegister[iidx++] = frame.pop(); // dest
		evals[eidx++] = new J2cVoid
		    (new AssignmentExp
		     (new J2cInt
		      (new MemExp((J2cValue) inputRegister[1],
				  J2cValue.makeIntConstant(TypeCodes.INT, 0)),
		       tag),
		      (J2cValue) inputRegister[0]));
		break;
		    
	    case D.setBlock:
		inputRegister[iidx++] = frame.pop(); // count
		inputRegister[iidx++] = frame.pop(); // src
		inputRegister[iidx++] = frame.pop(); // dest
		evals[eidx++]
		    = callV(new InvocationExp
			    (J2cValue.makeSymbolicReference("memmove"),
			     new Value[] {
				 new InternalReference(new CCastExp("void *", (Value) inputRegister[2])),
				 new InternalReference(new CCastExp("const void *", (Value) inputRegister[1])),
				 new J2cInt(new ConversionExp((J2cValue)inputRegister[0]),
					    TypeCodes.INT)
			     },
			     ctx.blueprintFor(ctx.domain.VOID)));
		break;
	    }
	    break;
	}
	case S.START_TRACING:	// (Z)V
	    mc.log("ignore startTracing");
	    inputRegister[iidx++] = frame.pop();
	case S.STOP_TRACING:	// ()V
	    mc.log("ignore stopTracing");
	    break;
	case S.CUT_TO_ACTIVATION: // (II)V
	    mc.log("ignore CUT_TO_ACTIVATION");
	    inputRegister[iidx++] = frame.pop();
	    inputRegister[iidx++] = frame.pop();
	    break;
	case S.MAKE_ACTIVATION:	// (ILovm/core/domain/Blueprint;Lovm/core/repository/RepositoryByteCodeFragment;[Lovm/core/domain/Oop;)I
	    inputRegister[iidx++] = frame.pop(); //InvocationMessage
	    inputRegister[iidx++] = frame.pop(); // Code
	    inputRegister[iidx++] = frame.pop(); // int
	    outputRegister[oidx++]
		= callI(new InvocationExp("makeActivation",
					  inputRegister, iidx, true,
					  ctx.intBP));
	    break;
	case S.GET_ACTIVATION:	// (I)I
	    inputRegister[iidx++] = frame.pop();
	    outputRegister[oidx++]
		= callI(new InvocationExp("getActivation",
					  inputRegister, iidx, true,
					  ctx.intBP));
	    break;
	case S.INVOKE:
	    inputRegister[iidx++] = frame.pop(); // InvocationMessage
	    inputRegister[iidx++] = frame.pop(); // Code
	    ValueSource callExp = new InvocationExp("j2cInvoke",
						    inputRegister, iidx, true,
						    ctx.longBP);
// 	    ValueSource callJValue
// 		= new JValueExp(new InternalReference(callExp));
	    S3Blueprint rt;
	    switch (sub) {
	    case TypeCodes.VOID:
		evals[eidx++] = new J2cVoid(callExp);
		break;
	    case TypeCodes.OBJECT:
		outputRegister[oidx++] =
		    repackJLong(callExp, JavaNames.ovm_core_domain_Oop);
		break;
	    case TypeCodes.FLOAT:
		outputRegister[oidx++] =
		    repackJLong(callExp, TypeName.Primitive.FLOAT);
		break;
	    case TypeCodes.DOUBLE:
		outputRegister[oidx++] =
		    repackJLong(callExp, TypeName.Primitive.DOUBLE);
                outputRegister[oidx++] = null;
		break;
	    case TypeCodes.LONG:
		outputRegister[oidx++] =
		    new J2cLong(callExp, null, TypeCodes.LONG);
                outputRegister[oidx++] = null;
		break;
	    default:
		outputRegister[oidx++] =
		    repackJLong(callExp, TypeName.Primitive.INT);
	    }
	    assert(pc == getPC());
	    break;
	case S.CAS32:
	    inputRegister[iidx++] = asPrimitive((Value) frame.pop()); // new
 	    inputRegister[iidx++] = asPrimitive((Value) frame.pop()); // old
	    inputRegister[iidx++] = asAddress((Value) frame.pop());   // addr;
	    outputRegister[oidx++]
		= callI(new InvocationExp("CAS32",
					  inputRegister, iidx, true,
					  ctx.intBP));
	    break;
	case S.CAS64:
	    inputRegister[iidx++] = frame.pop(); // new
	    frame.pop();			 // 2nd half
 	    inputRegister[iidx++] = frame.pop(); // old
	    frame.pop();			 // 2nd half
	    inputRegister[iidx++] = frame.pop(); // addr;
	    outputRegister[oidx++]
		= callI(new InvocationExp("CAS64",
					  inputRegister, iidx, true,
					  ctx.intBP));
	    break;
	default:
	    mc.log("uknown SYSTEM instruction " + call);
	}
	for (int j = 0; j < oidx; j++)
	    if (j < oidx - j && outputRegister[j + 1] == null)
		frame.pushWide(outputRegister[j++]);
	    else
		frame.push(outputRegister[j]);
    }

    static final HashMap inlineNatives = new HashMap();
    static void inlineMe(String name, String sig) {
	String sb = name+':'+sig;
	UnboundSelector.Method sel = RepositoryUtils.makeUnboundSelector(sb).asMethod();
	if (!sel.getDescriptor().getType().isPrimitive())
	    throw new RuntimeException("Currently only inlining primitive return types");
	inlineNatives.put(sel, name);
    }
    static {
	inlineMe("setEnabled", "(Z)V");
	inlineMe("eventsSetEnabled", "(Z)V");
	inlineMe("memcpy", "("+
		 "Lovm/core/services/memory/VM_Address;"+
		 "Lovm/core/services/memory/VM_Address;"+
		 "I)V");
	inlineMe("memmove", "("+
		 "Lovm/core/services/memory/VM_Address;"+
		 "Lovm/core/services/memory/VM_Address;"+
		 "I)V");
	inlineMe("memset", "("+
		 "Lovm/core/services/memory/VM_Address;"+
		 "II)V");
	inlineMe("bzero", "("+
		 "Lovm/core/services/memory/VM_Address;"+
		 "I)V");
	inlineMe("SAVE_REGISTERS", "()V");
	inlineMe("sqrt", "(D)D");
    }
    
//     TypeName inlineTN
// 	= OVMBase.repository.makeTypeName("s3/services/j2c", "Inline");

    // I guess I have to get argument and return information from the
    // corresponding method signatures
    public void visit(Instruction.INVOKE_NATIVE insn) {
	Frame frame = getFrame();
	int callNo = insn.getMethodIndex(buf);
	UnboundSelector.Method m =
	    NativeCallGenerator.getNativeArgumentMap()[callNo];
	if (false /*&& m.getDeclaringTypeName() == InlineTN*/) {
	    // handle Inline.C and Inline.asm specially
	    return;
	}
	Descriptor.Method sig = m.getDescriptor();
	int nargs = sig.getArgumentLength() / 4;
	mc.log("call to ", m, "( with ", I(nargs), " args)" );

	ValueSource callExpr;

	J2cValue[] origArgs = new J2cValue[nargs];
	for (int idx = nargs - 1; idx >= 0; idx--) 
	    origArgs[idx] = (J2cValue) frame.pop();

	
	String name = (String) inlineNatives.get(m);
	if (name != null) {
	    TypeName VM_AddressTN =
		ctx.VM_AddressBP.getType().getUnrefinedName();
	    setArgumentRegisterCount(nargs);
	    // i is the "real" argument index (accounting for wides)
	    // j is the index into the argument list
	    for (int i = 0, j = 0; i < nargs; i++, j++) {
		J2cValue ai = origArgs[i];
		TypeName at = sig.getArgumentType(j);
		if (at == VM_AddressTN)
		    ai = new InternalReference(new CCastExp("void *", ai));
		setArgumentRegisterAt(i, ai);
		if (ai.isWidePrimitive()) {
		    i++; j++;
		    setArgumentRegisterAt(i, null);
		}
	    }

	    char tag = sig.getType().getTypeTag();
	    switch (tag) {
	    case 'V':
		callExpr = new InvocationExp(name, argumentRegisters,
					     nargs, false,
					     ctx.voidBP);
		evals[0] = new J2cVoid(callExpr);
		break;
	    case 'I': case 'S': case 'C': case 'B': case 'Z':
		callExpr = new InvocationExp(name, argumentRegisters,
					     nargs, false,
					     ctx.intBP);
		outputRegister[0] = new J2cInt(callExpr, tag);
		frame.push(outputRegister[0]);
		break;
	    case 'J':
		callExpr = new InvocationExp(name, argumentRegisters,
					     nargs, false,
					     ctx.longBP);
		outputRegister[0] = new J2cLong(callExpr, null, tag);
		frame.pushWide(outputRegister[0]);
		break;
	    case 'F':
		callExpr = new InvocationExp(name, argumentRegisters,
					     nargs, false,
					     ctx.blueprintFor(ctx.domain.FLOAT));
		outputRegister[0] = new J2cFloat(callExpr, null);
		frame.push(outputRegister[0]);
		break;
	    case 'D':
		callExpr = new InvocationExp(name, argumentRegisters,
					     nargs, false,
					     ctx.blueprintFor(ctx.domain.DOUBLE));
		outputRegister[0] = new J2cDouble(callExpr, null);
		frame.pushWide(outputRegister[0]);
		break;
	    default:
		assert false : "not primitive: " + tag;
	    }
	    return;
	} else {

	    setArgumentRegisterCount(nargs + 1);
	    setArgumentRegisterAt(0, J2cValue.makeIntConstant(TypeCodes.INT, callNo));
	    for (int i = 0, j = 0; i < nargs; j++, i++) {
		J2cValue ai = origArgs[j];
		Type atype = ai.getJavaType(ctx.domain);
		char atag = atype.getUnrefinedName().getTypeTag();
		
		if (ai.isWidePrimitive()) {
		    setArgumentRegisterAt(nargs - i, null);
		    i++; j++;
		    ai = new J2cLong(new InvocationExp("to_jwide_"+J2cValue.typeCodeToCtype[atag], ai,
						       ctx.longBP),
				     null, TypeCodes.LONG);
		}
		else {
		    J2cValue newarg;
                    if (ai instanceof J2cArray) {
                      newarg = new InternalReference(new CCastExp(" HEADER *",ai));
                    } else {
                      newarg = ai;
                    }                      
		    ai = new J2cInt(new InvocationExp("to_jnarrow_"+J2cValue.typeCodeToCtype[atag], newarg,
						      ctx.intBP),
				    TypeCodes.INT);
                }
		setArgumentRegisterAt(nargs - i, ai);
	    }

	    callExpr = new InvocationExp("invoke_native",
					 argumentRegisters,
					 nargs + 1, false, null);
	}
	TypeName n = sig.getType();
	char t = n.getTypeTag();
	if (t == TypeCodes.VOID)
	    evals[0] = new J2cVoid(callExpr);
	else
	    outputRegister[0] = jlongToType(callExpr, n);

	if (outputRegister[0] instanceof Value.WidePrimitive)
	    frame.pushWide(outputRegister[0]);
	else
	    frame.push(outputRegister[0]);
    }
}
