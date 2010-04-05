package s3.services.j2c;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import ovm.core.repository.Mode;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import org.ovmj.util.Runabout;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.process.HostedProcess;
import ovm.core.services.io.BasicIO;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.analysis.Frame;
import ovm.services.bytecode.analysis.State;
import ovm.util.Arrays;
import ovm.util.BitSet;
import ovm.util.HashMap;
import ovm.util.Iterator;
import ovm.util.ListIterator;
import ovm.util.Map;
import ovm.util.OVMRuntimeException;
import s3.core.domain.MachineSizes;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Constants;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Method;
import s3.core.domain.S3Type;
import s3.services.bootimage.Analysis;
import s3.services.bootimage.Ephemeral;
import s3.services.bootimage.ImageObserver;
import s3.services.bootimage.Driver;
import s3.services.bootimage.BootImage;
import ovm.core.services.memory.LocalReferenceIterator;
import s3.services.memory.precise.PreciseReferenceIterator;
import s3.services.j2c.J2cValue.J2cReference;
import s3.util.PragmaInline;
import s3.util.PragmaCAlwaysInline;
import s3.util.PragmaNoInline;
import s3.util.Walkabout;
import ovm.core.stitcher.InvisibleStitcher.Component;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;
import ovm.core.services.format.CxxFormat;
import ovm.util.CommandLine;

public class J2cImageCompiler extends ImageObserver
    implements Ephemeral.Void, Component
{
    // Parameters controlled by makeimage arguments:
    /**
     * Command line argument.  If set, don't explicitly compile all
     * methods in a domain
     **/
    boolean compileDomains; // true;

    private static final boolean disableCAlwaysInline = false;  
      // this is sometimes needed when doing runtime checks
      // well, sometimes it's needed even without runtime checks, the problem is that GCC may get
      // a method body too late for inlining
    
    public static final boolean debugExceptions = false;
      // debug throwing and propagating of an exception

    /**
     * Command line argument   Should we allocate references GCFrame
     * structures or directly in local variables
     **/
    // non-final to break initialization cycles.  Actually, there may
    // come a point when we can support precise garbage collectors
    // with or without GCFrame structures.
    /*final*/ boolean gcSupport;	// false
    String cflags;
    boolean safePointMacros;
    boolean noInlineWithSafePoints;
    boolean catchPointsUsed;
    boolean frameLists;
    boolean ptrStack;
    boolean initGCFrame=false;
    boolean noCppExceptions;
    boolean cExceptions;
    boolean cExceptionsCount;
    boolean doBarrierProf;
    boolean innerBarrierProf;
    boolean counterExitPollcheck;

    boolean propagateCopies;

    boolean assumeLiveness;
    
    int padImageSize;
    
    String linkerOptions;
    
    // Some hard-coded parameters.  Edit to taste:

    /**
     * Set this to optimize the bit maps returned by
     * Blueprint.getRefMask(): If no subtype of a field's declared
     * type is ever heap-allocated, the garbage collector need not
     * walk it.
     *
     * It seems to work modulo Oop and Opaque, but it doesn't do as
     * much as I hoped.
     **/
    // I've resurrected this code, I think there was a bug that made it not to work.
    // It has some performance improvements, at least in RTGC.
    // It now handles Opaque and Oop correctly, afaik.
    static final boolean PRUNE_GC_MAPS = true;
    static final boolean DEBUG_PRUNE_GC_MAPS = false;

    /**
     * Set this to disable try block generation.
     **/
    static final boolean EXCEPTIONS_BROKEN = false;

    /**
     * If true, keep extremely detailed log of the compilation of
     * each method, and dump it in the case of an error.  A lot of
     * work has gone into keeping the logging calls fast when
     * KEEP_LOGS is false, but if KEEP_LOGS stops being a compile-time
     * contant, much of that work will vanish.
     **/
    static final boolean KEEP_LOGS = false;

    /**
     * Keep various statistics that are printed at the end of a
     * compiler run.  New statistics can be printed by allocating
     * a subtype of StatPrinter.  Note that all StatPrinters will be
     * executed regardless of the value of the global KEEP_STATS
     * variable.  KEEP_STATS is just an easy way to turn lots of things
     * on at once.
     **/
    static final boolean KEEP_STATS = false;

    /**
     * J2c is capable of continuing from an exception raised compiling
     * a single method.  But we don't want to continue if every other
     * method crashes.
     **/
    static final int IGNORE_FAILURE_COUNT = 5;
 
    /**
     * If set, we return a non-zero exit status if we crashed
     * compiling any method.
     **/
    static final boolean FAILURES_ARE_FATAL = true;
    
    /**
     * measure execution time inside make
     **/
    static final boolean TIME_MAKE = false;

    /** true if C symbols are prefixed by `_' **/
    static final boolean AS_NEEDS_UNDERSCORE
	= NativeConstants.ASM_NEEDS_UNDERSCORE;

    static final String DIRECTORY = ".";
    static final String MAKE = "make";

   static int compilationFailures = 0;

    HashMap sym2addr = new HashMap();

    PrintWriter w;

    static int make(String target) throws IOException {
	return make(DIRECTORY, target);
    }

    static int make(String[] args) throws IOException {
	return make(DIRECTORY, args);
    }

    static int make(String dir, String arg) throws IOException {
	return make(dir, new String[] { arg });
    }
    
    static int make(String dir, String[] args) throws IOException {
	System.out.print(dir+">");
	int offset = TIME_MAKE ? 2 : 1;
	String[] argv = new String[args.length + offset];
	if (TIME_MAKE)
	    argv[0] = "time";
	argv[offset - 1] = "make";
	System.arraycopy(args, 0, argv, offset, args.length);
	for (int i=0;i<argv.length;++i) {
	    System.out.print(" "+argv[i]);
	}
	System.out.println();
	return HostedProcess.system(dir, argv);
    }

    static PrintWriter open(String name)
	throws IOException {
	name = DIRECTORY + '/' + name;
 	return new PrintWriter
 	    (new BufferedOutputStream(new FileOutputStream(name)));
    }

    static StatPrinter statPrinters = null;

    /**
     * This class was defined for the purpose of printing various
     * cache statistics, but it is really just a kind of atexit()
     * mechanism.  The printStats method of every StatPrinter
     * allocated will execute at the end of a succesful compiler run.
     * 
     * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
     */
    public abstract static class StatPrinter {
	StatPrinter next;
	public StatPrinter() {
	    next = statPrinters;
	    statPrinters = this;
	}
	public static int countNonNull(Object[] arr) {
	    int ret = 0;
	    for (int i = 0; i < arr.length; i++)
		if (arr[i] != null)
		    ret++;
	    return ret;
	}
	public abstract void printStats();
    }

    abstract class BlueprintWalker extends DomainWalker {
	protected Context ctx;
	
	public abstract void walkBlueprint(S3Blueprint bp);

	public void beforeDomain(S3Domain d) {
	    ctx = findContext(d);
	}

	// return false to skip this domain
	public boolean acceptDomain(S3Domain d) {
	    beforeDomain(d);
	    // return d == getExecutiveDomain();
	    return true;
	}

	public void walkDomain(Domain _d) {
	    S3Domain d = (S3Domain) _d;
	    if (acceptDomain(d))
		for (Iterator it = d.getBlueprintIterator(); it.hasNext(); ) {
		    S3Blueprint bp = (S3Blueprint) it.next();
		    walkBlueprint(bp);
		}
	}
    }
	    
	
    static final String[] headerSymbol = new String[] {
	"coreServicesAccess",
	"mainObject",
	"bootContext"
    };
    
    DomainWalker genExterns = new MethodWalker() {
	    public void walk(Method meth) {
		w.print("extern e_s3_services_j2c_J2cCodeFragment ");
		w.print(J2cFormat.format(meth));
		w.println("_code;");
	    }
	    
	    public void beforeDomain(Domain dom) {
		w.print("extern e_s3_core_domain_");
		w.print(dom.isExecutive()
			? "S3ExecutiveDomain "
			: "S3JavaUserDomain ");
		w.println(J2cFormat.format(dom) + ";");
		super.beforeDomain(dom);
	    }

	    public void walkBlueprint(Blueprint bp) {
		w.print("extern e_s3_core_domain_S3Blueprint");
		w.print(bp instanceof Blueprint.Scalar ? "_Scalar "
			: bp instanceof Blueprint.Array ? "_Array "
			: "_Primitive ");
		w.println(J2cFormat.formatBP(bp) + ";");
		if (bp.isSharedState()) {
		    S3Blueprint classType =
			((bp.getInstanceBlueprint().isScalar()
			  && ctx.shouldCompile(bp.getInstanceBlueprint()))
			 ? (S3Blueprint) bp
			 : ctx.blueprintFor(ctx.domain.getMetaClass()));
		    w.print("extern " + J2cFormat.format(classType) + " ");
		    w.println(J2cFormat.formatShSt(bp) + ";");
		}
		super.walkBlueprint(bp);
	    }

	    public void forAllDomains() {
		for (int i = 0; i < headerSymbol.length; i++) {
		    Object value = getHeader(headerSymbol[i]);
		    Oop oops = VM_Address.fromObject(value).asOop();
		    S3Blueprint bp = (S3Blueprint) oops.getBlueprint();
		    w.print("extern ");
		    w.print(J2cFormat.format(bp));
		    w.print(" ");
		    w.print(headerSymbol[i]);
		    w.println(";");
		}
		super.forAllDomains();
	    }
	};

    DomainWalker genHeapSymbols = new MethodWalker() {
	    void genHeapSymbol(String name, Object _value) {
		if (AS_NEEDS_UNDERSCORE)
		    name = "_" + name;
		int value = VM_Address.fromObject(_value).asInt();
	
		w.print("\t.globl ");
		w.println(name);
		w.print("\t.set ");
		w.print(name);
		w.print(", 0x");
		w.print(Integer.toHexString(value));
		w.println("");
	    }

	    public void walk(Method meth) {
		genHeapSymbol(J2cFormat.format(meth) + "_code",
			      meth.getCode(J2cCodeFragment.KIND));
	    }

	    public void beforeDomain(final Domain dom) {
		genHeapSymbol(J2cFormat.format(dom), dom);
		super.beforeDomain(dom);
	    }

	    public void walkBlueprint(Blueprint bp) {
		genHeapSymbol(J2cFormat.formatBP(bp), bp);
		if (bp.isSharedState()) {
		    genHeapSymbol(J2cFormat.formatShSt(bp),
				  bp.getSharedState());
		}
		super.walkBlueprint(bp);
	    }

	    public void forAllDomains() {
		for (int i = 0; i < headerSymbol.length; i++) {
		    Object value = getHeader(headerSymbol[i]);
		    genHeapSymbol(headerSymbol[i], value);
		}
		super.forAllDomains();
	    }
	};

    // FIXME: uncomment the method below to see MemberResolver blow up
//     void addHeader(String name, Object value) {
//     }

    String stype(Context ctx, Type t) {
	S3Blueprint bp = ctx.blueprintFor(t);
	StringBuffer result = new StringBuffer();
	
	if (bp.isScalar()) {
	  result.append("HEADER * /* ");
	  result.append(J2cFormat.format(bp));
	  result.append(" */ ");
        } else if (bp.getType() instanceof Type.Array) {
          result.append(CxxFormat.formatCArray((S3Blueprint.Array)bp));
          result.append("* /*");
          result.append(CxxFormat.format(((S3Blueprint.Array)bp).getComponentBlueprint()));
          result.append(" */ ");
        } else {
          result.append(J2cFormat.format(bp));
          if (bp.isReference()) {
	    result.append(" *");
	    }
        }

	return result.toString();
    }

    void ptype(Context ctx, Type t) {
	w.print(stype(ctx,t));
    }

    public Context findContext(Domain dom) {
	return Context.findContext(this, dom);
    }
    
    public static abstract class MethodWalker extends Analysis.MethodWalker {
	protected Context ctx;

	public void walkDomain(Domain d) {
	    this.ctx = Context.findContext(d);
	    super.walkDomain(d);
	}
	protected boolean shouldWalk(Method m) {
	    return anal.shouldCompile(m);
	}
    }

    MethodWalker protoPrinter = new MethodWalker() {
	    BitSet[] reflectiveCalls;
	    
	    public void forAllDomains() {
		reflectiveCalls = new BitSet[DomainDirectory.maxContextID()+1];
		for (int i = 0; i < reflectiveCalls.length; i++) {
		    Type.Context tc = DomainDirectory.getContext(i);
		    if (tc != null)
			reflectiveCalls[i] = new BitSet(tc.getMethodCount());
		}
		super.forAllDomains();
	    }
	    public void walkDomain(Domain d) {
		Method[] meth = d.getReflectiveCalls();
		for (int i = 0; i < meth.length; i++) {
		    reflectiveCalls[meth[i].getCID()].set(meth[i].getUID());
		  //  Transaction.the().setReflectiveCalls(reflectiveCalls, d, meth, i);
		}
		super.walkDomain(d);
	    }	   
	    
	    public void walk(Method meth)
	    {
		// FIXME: what about finalize?
		if (J2cValue.isMethodNamed(meth)
		    || reflectiveCalls[meth.getCID()].get(meth.getUID())
		    || meth.getSelector().getNameIndex() == ctx.clinitUtf
		    || meth.getSelector().getUnboundSelector() == JavaNames.FINALIZE
		    || meth.getSelector().getUnboundSelector() == JavaNames.LOAD_CLASS)
		{
		    // For static methods, this must be a shared state
		    S3Blueprint bp = ctx.blueprintFor(meth.getDeclaringType());
		    
		    StringBuffer proto=new StringBuffer();
		    proto.append("static ");
		    if (PragmaInline.declaredBy(meth.getSelector(), bp)) {
			if (noInlineWithSafePoints && ctx.hasSafePoints_p(meth))
			    proto.append("/*DECLARED INLINE*/");
			else
			    proto.append("inline ");
		    }
		    try { proto.append(stype(ctx, meth.getReturnType())); }
		    catch (LinkageException e) { throw e.unchecked(); }
		    proto.append(' ' + J2cFormat.format(meth) + '(');
		    proto.append(stype(ctx, meth.getDeclaringType()));
		    int nargs = meth.getArgumentCount();
		    for (int i = 0; i < nargs; i++) {
			proto.append(", ");
			try { proto.append(stype(ctx, meth.getArgumentType(i))); }
			catch (LinkageException e) { throw e.unchecked(); }
		    }
		    proto.append(")");
		    
		    String protos=proto.toString();

		    w.print(protos);
		    w.println(";");

		    if (PragmaNoInline.declaredBy(meth.getSelector(), bp))
			w.println(protos+" __attribute__((noinline));");
		    else if (noInlineWithSafePoints) {
			if (ctx.hasSafePoints_p(meth))
			    w.println(protos+" __attribute__((noinline));");
		    }
		    
		    
                    if (!disableCAlwaysInline && PragmaCAlwaysInline.declaredBy(meth.getSelector(), bp) && !(noInlineWithSafePoints && ctx.hasSafePoints_p(meth))) {
			    w.println(protos+" __attribute__((always_inline));");
                    }
                  

		    // Prevent gcc from removing non-virtual
		    // reflectively called methods.  Gcc does have the
		    // option of inlining non-reflective methods away.
		    if (reflectiveCalls[meth.getCID()].get(meth.getUID())
			|| (ctx.clinitIsLive
			    && (meth.getSelector().getNameIndex()
				== ctx.clinitUtf)))
			w.println(protos+" __attribute__((used));");
		}
	    }

	    public void walkDead(Method meth) {
		if (J2cValue.isMethodNamed(meth)) {
		    w.print("#define ");
		    w.print(J2cFormat.format(meth));
		    w.print("(...) ({ j2cFail() ; ((");
		    try { ptype(ctx, meth.getReturnType()); }
		    catch (LinkageException e) {
			w.print("HEADER *");
		    }
		    catch (LinkageException.Runtime e) {
			w.print("HEADER *");
		    }
		    w.println(")0); })");
		    /*
		    w.print(" ((");
		    try { ptype(ctx, meth.getReturnType()); }
		    catch (LinkageException e) {
			w.print("HEADER *");
		    }
		    catch (LinkageException.Runtime e) {
			w.print("HEADER *");
		    }
		    w.print(" (*)(");
		    w.print(stype(ctx, meth.getDeclaringType()));
                    int nargs = meth.getArgumentCount();
		    for (int i = 0; i < nargs; i++) {
			w.print(",");
			try { w.print(stype(ctx, meth.getArgumentType(i))); }
			catch (LinkageException e) { throw e.unchecked(); }
		    }
		    w.println(")) j2cFail)");
		    */
		}
	    }
	};

    interface CP extends JVMConstants { }

    /** Hack to allow special casing the image for j2c--jv*/
    public boolean isJ2c() {return true;}
    public boolean shouldQuickify() { return true; }

    final StringBuffer logBuffer = new StringBuffer();
    
    MethodWalker methodCompiler = new MethodWalker() {
	public void beforeDomain(Domain d) {
	    System.err.println("compiling " + d);
	    super.beforeDomain(d);
	    // Generate per-domain helper functions.
	    new CodeGen(w, ctx);
	}
    
	public void walk(Method _meth) {
	    S3Method meth = (S3Method) _meth;
	    S3ByteCode cf = meth.getByteCode();
	    logBuffer.setLength(0);
	    MethodCompiler mc = new MethodCompiler(logBuffer, meth, cf, ctx);
	    try {
		mc.compile(w);
	    } catch (RuntimeException e) {
		mc.dumpLog(System.err, e,
			   "compilation of " + meth.getSelector()
			   + " failed:");
		compilationFailures++;
		if (compilationFailures > IGNORE_FAILURE_COUNT)
		    System.exit(-1);
	    }
	    catch (Error e) {
		mc.dumpLog(System.err, e,
			   "compilation of " + meth.getSelector()
			   + " failed:");
		compilationFailures++;
		if (compilationFailures > IGNORE_FAILURE_COUNT)
		    System.exit(-1);
	    }
	}
    };

    static final int headerWords
        = ObjectModel.getObjectModel().headerSkipBytes()/4;
    
    DomainWalker dumpTables = new BlueprintWalker() {
	    void dumpTable(S3Blueprint bp, Code[] tab,
			   String suffix, boolean isUsed) {
		String name = J2cFormat.format(bp) + suffix;
		if (isUsed) {
///		    w.println("# 1 \"vtables\"");

		 //   Type.Class t = (Type.Class) bp.getType();
		    w.print("void *");
		    w.print(name);
		    w.print("[] = {");
		    String prefix = ",\n\t(void *)";
		    // FIXME: fake an object header, we don't want
		    // to initialize these objects.  Note that the
		    // blueprint is null, because we don't assign
		    // symbols to array blueprints!
		    
		    // we still need to set up forwarding pointer, sometimes
		    
		    int fwdOffset = -1;
		    if (ObjectModel.getObjectModel().getUnforwardedSemantics()==ObjectModel.FWD_SELF) {
		      fwdOffset = ObjectModel.getObjectModel().getForwardOffset()/MachineSizes.BYTES_IN_WORD;
		    }
                    for (int i = 0; i < headerWords; i++) {
                    
                        if (i==fwdOffset) {
                          w.print("\n\t");
                          w.print(name);
                          w.print(",");
                        } else {
  		          w.print("\n\t(void *) 0,");
                        }
                    }
                    
		    w.print("\n\t(void *) " + tab.length);
		    for (int j = 0; j < tab.length; j++) {
			w.print(prefix);
			if (tab[j] == null) {
			    w.print("0");
			    continue;
			}
			S3Method meth = (S3Method) tab[j].getMethod();
			if (ctx.shouldCompile(meth))
			    w.print(J2cFormat.format(meth));
			else
			    w.print("j2cFail");
		    }
		    w.println("\n};\n");
		} else {
		    VM_Address placeHolder = (VM_Address) sym2addr.get(name);
		    if (placeHolder == null) {
// 			System.err.println("unused dispatch table in " +
// 					   "unused blueprint " + bp);
			return;
		    }
		    placeHolder.bind(0);
		    sym2addr.remove(name);
		}
	    }
	    public void walkBlueprint(S3Blueprint bp) {
		if (ctx.shouldCompile(bp) && bp.getType().isClass()) {
		    dumpTable(bp, bp.getVTable(), "_vtable",
			      ctx.isVTableUsed(bp));
		    dumpTable(bp, bp.getIFTable(), "_iftable",
			      ctx.isIFTableUsed(bp));
		}
	    }

	    public void walkDomain(S3Domain d) {
		w.print("\n# 1 \"tables for ");
		w.print(d);
		w.println("\"");
		super.walkDomain(d);
	    }
	};

    /**
     * What is the long-term plan here?  The code fragment table
     * should contain the start and end pcs of every code fragment
     * that J2C generates.  
     *
     * What is a code fragment?  One answer is that a code fragment is
     * a C++ function whose address can be taken.  A more interesting
     * answer is that it is the region of code corresponding to the
     * invocation of some java method.  If either J2c or gcc chooses
     * to inline one method into another, there might be four code
     * fragments: one for the out-of-line callee, one for the region
     * of the caller before the inline call, one for the methods taken
     * together, and one for the region of the caller after the inline
     * call.  That sounds complicated.
     *
     * At minimum, end pcs are probably needed to support
     * Interpreter.getCode.
     *
     * A second approach might be to call addr2line, and map C++
     * function names back to J2cCodeFragment objects.  addr2line is
     * probably the quickest way to get Java stack traces working
     */
    void parseCodeIndexes(BufferedReader r) {
	final int[][] UID2mapIndex =
	    new int[DomainDirectory.maxContextID() + 1][];
	for (int i = 0; i < UID2mapIndex.length; i++) {
	    Type.Context tc = DomainDirectory.getContext(i);
	    if (tc != null) {
		UID2mapIndex[i] = new int[tc.getMethodCount()];
		Arrays.fill(UID2mapIndex[i], -1);
	    }
	}
	int nr = 0;
	try {
	    while (r.ready()) {
		int cuid = Integer.parseInt(r.readLine());
		int cid = cuid >> 16;
		int uid = cuid & 0xffff;
		UID2mapIndex[cid][uid] = nr++;
	    }
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
	new MethodWalker() {
	    public void walk(Method meth) {
		J2cCodeFragment cf =
		    (J2cCodeFragment) meth.getCode(J2cCodeFragment.KIND);
		if (cf == null)
		    throw new Error("no j2c code for " + meth);
		cf.index = UID2mapIndex[meth.getCID()][meth.getUID()];
	    }
	}.forAllDomains();
    }

    void bind(String name, Object value) {
//	System.err.println(value + " taken from C symbol " + name);
	sym2addr.put(name, VM_Address.makeUnbound(value));
    }

    // Here J2cImageCompiler's toplevel entry points, written in the
    // order in which they execute

    // We don't want to call MemoryManager.the() or
    // LocalReferenceIterator.factor() from the constructor, because
    // this is a recipe for
    // because this can 
//     private boolean forceNoInlineWithSafePoints;
//     private boolean forceCatchPointsUsed;
    
    public J2cImageCompiler(String OVMMakefile,
			    String gdbinit,
			    String cflags,
			    String noInlineWithSafePoints,
			    String catchPointsUsed,
			    String noCppExceptions,
			    String cExceptions,
			    String cExceptionsCount,
			    String doBarrierProf,
			    String counterExitPollcheck,
			    String propagateCopies,
			    String assumeLiveness,
			    String padImageSize,
			    String linkerOptions) {
	super(OVMMakefile, gdbinit);
	
	this.padImageSize=CommandLine.parseSize(padImageSize);
	this.linkerOptions=linkerOptions;

	this.cflags = cflags;
// 	forceNoInlineWithSafePoints = noInlineWithSafePoints != null;
// 	forceCatchPointsUsed = catchPointsUsed != null;
	this.doBarrierProf = doBarrierProf != null;
	if (this.doBarrierProf) {
	    if (doBarrierProf.equals("inner")) {
		this.innerBarrierProf=true;
	    } else if (doBarrierProf.equals("outer")) {
		this.innerBarrierProf=false;
	    } else {
		throw new MisconfiguredException("-do-barrier-prof must have "+
						 "either 'inner' or 'outer' " +
						 "as its argument.");
	    }
	}
	this.counterExitPollcheck = counterExitPollcheck!=null;
	this.noCppExceptions = noCppExceptions != null;
	this.cExceptions = cExceptions != null;
	this.cExceptionsCount = cExceptionsCount != null;
	this.noInlineWithSafePoints |= noInlineWithSafePoints != null;
	this.catchPointsUsed |= catchPointsUsed != null;
	this.propagateCopies = (propagateCopies != null
				&& !propagateCopies.equals("false"));
	this.assumeLiveness = (assumeLiveness != null
			       && !assumeLiveness.equals("false"));
    }

    /**
     * Determine how to generate code for the configured
     * {@link LocalReferenceIterator}, and create <file>GenOvm.mk</file>.
     **/
    public void initialize() {
	try {
	    PrintWriter w = open("GenOvm.mk");
	    
	    if (cExceptions) {
              cflags += " -DCEXCEPTIONS";
              if (cExceptionsCount) {
                cflags += " -DCEXCEPTIONS_COUNT";
              }
              cflags += " -fno-exceptions";
            } else {
              cflags += " -fexceptions -fnon-call-exceptions";
            }
            
	    gcSupport = LocalReferenceIterator.the().expectsEngineSupport();
	    if (gcSupport) {
		safePointMacros = (LocalReferenceIterator.the() instanceof
				   PreciseReferenceIterator);
		if (safePointMacros) {
		    cflags += " -DPRECISE_SAFE_POINTS";
		    cflags += (" -DPRECISE_"
			       + PreciseReferenceIterator.getFlavor());
		    if (this.doBarrierProf) {
			cflags += " -DPRECISE_BARRIER_PROF";
		    }
		    if (this.counterExitPollcheck) {
			cflags += " -DCOUNTER_POLLCHECK";
		    }
		    if (this.noCppExceptions) {
			cflags += " -DCOUNTER_EXCEPTIONS";
		    }
		    this.noInlineWithSafePoints |=
			PreciseReferenceIterator.noInlineWithSafePoints();
		    this.catchPointsUsed |=
			PreciseReferenceIterator.catchPointsUsed();
		    w.println("CXX_SRC += precise.c");
		} else if (LocalReferenceIterator.the()
			   instanceof HendersonLocalReferenceIterator) {
		    cflags += " -DPRECISE_HENDERSON";
		    frameLists = true;
		    initGCFrame = true;
		} else if (LocalReferenceIterator.the()
			   instanceof PtrStackLocalReferenceIterator) {
		    cflags += " -DPRECISE_PTRSTACK";
		    ptrStack = true;
		    initGCFrame = !((PtrStackLocalReferenceIterator)LocalReferenceIterator.the()).zero;
		} else {
		    throw new Error("Don't know how to support precise stack walking.");
		}
	    }
	    // actually the proper fix would be to modify
	    // j2c so that it does not generate identifiers
	    // with dollar signs in them
 	    if (NativeConstants.OVM_ARM) {
	        cflags += " -fdollars-in-identifiers -fno-omit-frame-pointer";
	    }
	    w.println("OPT=" + cflags);
	    w.println("DEBUG=");
	    w.println("HEADER_SKIP_BYTES="+
		      ObjectModel.getObjectModel().headerSkipBytes());
            w.print("SELF_FORWARDING_OFFSET=");
            if (ObjectModel.getObjectModel().getUnforwardedSemantics()==ObjectModel.FWD_SELF) {
              w.println( ObjectModel.getObjectModel().getForwardOffset() );
            } else {
              w.println("-1");
            }
	    w.println("UNDERSCORE="+(AS_NEEDS_UNDERSCORE ? "_" :""));
	    if (gcSupport && safePointMacros && this.doBarrierProf) {
		w.println("FILTER_OVM_INLINE=ruby $(srcdir)/barrier_filt.rb "+
			  "$(HEADER_SKIP_BYTES) \"$(UNDERSCORE)\" < $<");
	    } else {
		w.println("FILTER_OVM_INLINE=cat $<");
	    }
	    w.println("PAD_IMAGE_SIZE="+padImageSize);
	    w.println("ADDL_LDFLAGS="+linkerOptions);
	    w.close();
	} catch (IOException e) {
	    throw new RuntimeException("error writing GenOvm.mk", e);
	}

	if (noCppExceptions && safePointMacros &&
	    !PreciseReferenceIterator.canHandleExceptions()) {
	    throw new MisconfiguredException("cannot disable C++ exceptions "+
					     "unless you are using counters "+
					     "for accurate GC");
	}
	
        if (cExceptions && noCppExceptions) {
          throw new MisconfiguredException("C exceptions and no-CPP exceptions "+
            "do not work together (and do not make sense together, either)");
        }
        
        if (cExceptionsCount && !cExceptions) {
          throw new MisconfiguredException("C exceptions counting "+
            "does not work without C exceptions");
        }

        if (!cExceptions) {
          throw new MisconfiguredException("C exception must always be enabled "+
            "because OVM now only generates C code.");
        }        

    }

    public void registerLoaderAdvice(Gardener g) {
	// The compiler is reachable through the invisible stitcher,
	// but doesn't belong in the image
	g.excludeClass("s3/services/j2c", "J2cImageCompiler");

	// reachable from J2cCodeFragment consturctor.  Sadly, marking
	// the constructor as BCdead doesn't keep J2cFormat out of the
	// typename closure.
	g.excludeClass("s3/services/j2c", "J2cFormat");

	Selector.Method sel = RepositoryUtils.methodSelectorFor
	    ("Gs3/services/j2c/J2cCodeFragment;",
	     "throwWildcardException:(Lovm/core/domain/Oop;)V");
	getExecutiveDomain().registerCall(sel);
	sel = RepositoryUtils.methodSelectorFor
	    ("Gs3/services/j2c/J2cCodeFragment;",
	     "generateThrowable:(I)V");
	getExecutiveDomain().registerCall(sel);
	sel = RepositoryUtils.methodSelectorFor
	    ("Gs3/services/j2c/J2cCodeFragment;",
	     "assertAddressValid:(Lovm/core/services/memory/VM_Address;)V");
	getExecutiveDomain().registerCall(sel);
    }

    public void registerGCAdvice(Walkabout w) {
	//VM_Address.fromObject(((S3Domain) getExecutiveDomain()).ROOT_BLUEPRINT);
	w.register(new Walkabout.FieldAdvice() {
		public Class targetClass() { return S3Blueprint.Scalar.class; }
		public Object beforeField(Object o,
					  java.lang.reflect.Field f,
					  Object value) {
		    S3Blueprint bp = (S3Blueprint) o;
		    if (!bp.isSharedState()) {
			String symbol;
			if (f.getName().equals("j2cVTable"))
			    symbol = J2cFormat.format(bp) + "_vtable";
			else if (f.getName().equals("j2cIfTable"))
			    symbol = J2cFormat.format(bp) + "_iftable";
			else
			    symbol = null;

			if (symbol != null      // field of interest
			    && sym2addr != null // not done with linking
			    && sym2addr.get(symbol) == null) // not seen
			    bind(symbol, value);
			// After the call bind,
			// VM_Address.fromObject(value) will do what
			// we want.
			return value;
		    }
		    return value;
		}
	    });
    }

    private boolean once = false;

    // We should be able to allocate J2cCodeFragment objects lazily,
    // but we do need to bind the static fields at some point.
    public void addMethod(Method meth) {
	S3ByteCode cf = meth.getByteCode();

	// Make sure that global pointers into C++ data exist
        if (!once) {
            once = true;
            bind("j2c_method_ranges", J2cCodeFragment.ranges);
            bind("j2c_method_pointers", J2cCodeFragment.frags);
        }
	// Pre-allocate J2cCodeFragment object
	meth.addCode(new J2cCodeFragment((S3Method) meth, -1));

	// Make sure that contstants are resolved appropriately
	S3Constants cp = (S3Constants)cf.getConstantPool();
	if (cp == null)
	    System.err.println(meth + " has null constant pool");
	else
	    processConstants(findContext(meth.getDeclaringType().getDomain()),
			     cp);
    }

    // Should no longer be needed
    private void processConstants(Context ctx, S3Constants cp) {
	VM_Address nullAddr
	    = VM_Address.fromObject(null);
	int len = cp.getConstantCount();
	for (int i = 0; i < len; i++) {
	    int tag = cp.getTagAt(i);
	    switch (tag) {
	    case CP.CONSTANT_String:
		Object rs = cp.getConstantAt(i);
		if (ctx.domain.isExecutive())
		    rs = cp.resolveConstantAt(i);
		FieldMagic._.findRoot(ctx.domain, 
				      VM_Address.fromObject(rs).asOop());
		break;
	    case CP.CONSTANT_Reference:
		Oop val = cp.resolveConstantAt(i);
		FieldMagic._.findRoot(ctx.domain, val);
		break;
	    case CP.CONSTANT_Binder:
		throw new Error("Binders should be resolved at this stage, or not?");
	    case CP.CONSTANT_SharedState:
		// nothing to be done
		break;
	    default:
		// nothing
		break;
	    }
	}
    }

     private final BlueprintWalker refPruner = new BlueprintWalker() { 
 	    int nPruned;
 	    int nRefs;
 	    BitSet[] prunedClasses = null;
 	    
 	    public void walkBlueprint(S3Blueprint bp) {

 		if (bp instanceof Blueprint.Scalar
		    && (bp.isSharedState() || ctx.anal.isConcrete(bp))) {

                    if (prunedClasses[bp.getCID()].get(bp.getUID())) {
                      return ;
                    }
                    
 		    int[] offset = bp.getRefMap();
 		    
                    // don't forget that parents may share refmaps with it's children
                    S3Blueprint.Scalar sbp = (S3Blueprint.Scalar)bp;
                    S3Blueprint.Scalar pb = sbp.getParentBlueprint();
                      
                    if (pb != null) {
                      
                        S3Type.Scalar st = (S3Type.Scalar)sbp.getType();
                        boolean hasReferences = false;

                        Field.Iterator fi = st.localFieldIterator();
                        while(fi.hasNext()) {
                          if (fi.next() instanceof Field.Reference) {
                            hasReferences = true;
                            break;
                          }
                        }                      

                        if (!hasReferences) {
                          /* shared map with parent */
                          walkBlueprint(pb);
                          bp.setRefMap(pb.getRefMap());
                          if (DEBUG_PRUNE_GC_MAPS) {
                            System.err.println("Took parent's refmap");
                          }
                          prunedClasses[bp.getCID()].set(bp.getUID()); 
                          if (DEBUG_PRUNE_GC_MAPS && (bp.getRefMap().length != offset.length)) {
                            System.out.println("Object "+bp+" has "+bp.getRefMap().length+" pointers, before it had " + offset.length);
                          }
                          return;
                        }
                        
                        assert( offset != pb.getRefMap() ); // no refmap sharing should be here
                    }
 		    
 		    int bpPruned = 0;
 		    
 		    for(int i=0;i < offset.length; i++) {
 		      
 		      assert(offset[i]!=-1); // error in updating refmaps shared among blueprints
 		        
 		      try {
 		        Field f = bp.fieldAt(offset[i]);
 		        if (f==null) {
 		          if (DEBUG_PRUNE_GC_MAPS) {
                            System.err.println("Strange, field at offset "+offset[i]+" of "+bp+" has no representation.\n");
                          }
                          nRefs++;
                          continue;
                        }
                          
                        S3Blueprint fbp = ctx.blueprintFor(f.getType());
                        
                        if ( fbp.isSubtypeOf(ctx.OopBP) || (ctx.OpaqueBP == fbp) ) {
                          nRefs++;
                          continue;
                        }
                        
                        if (fbp == null || !ctx.anal.isHeapAllocated(fbp)) {
                          if (DEBUG_PRUNE_GC_MAPS) {
                            System.err.println("pruning reference "
                              + (f == null ? (" undefined " + bp + " field at " + offset[i])
                              : f.getSelector().toString()));
                          }
                            
                          nPruned++;
                          bpPruned++;
                          offset[i]=-1;
                        } 
                        nRefs++;
                        
                      } catch (LinkageException e) { 
                        //throw e.unchecked(); 
                        if (DEBUG_PRUNE_GC_MAPS) {
                          System.err.println("Strange, while pruning, cannot resolve field "+ bp.fieldAt(offset[i]) + " of " + bp);
                        }
                      } 		      
 		    }
 		    
 		    if (bpPruned!=0) {
 		      int[] newoffset = new int[ offset.length - bpPruned ];
 		      int newi = 0;
 		      for(int i=0;i<offset.length;i++) {
 		        if (offset[i]!=-1) {
 		          newoffset[newi++] = offset[i];
                        }
 		      }
 		      bp.setRefMap(newoffset);
 		      if (DEBUG_PRUNE_GC_MAPS) {
     		        System.out.println("Object "+bp+" has "+newoffset.length+" pointers, before it had " + offset.length);
                      }
 		    }
 		    prunedClasses[bp.getCID()].set(bp.getUID()); 
                }		    
                    
/* 		    
 		    int[] offset = bp.getRefMap();
 		    for(int i=0;i < offset.length; i++) {
 		      if (!bp.refMaskGet(offset[i])) {
 		        continue; // already cleared, can this happen ?
 		      }
 		      
 		      try {
 		        Field f = bp.fieldAt(offset[i]);
 		        if (f==null) {
                          System.err.println("Strange, field at offset "+offset[i]+" of "+bp+" has no representation.\n");
                          continue;
                        }
                          
                        S3Blueprint fbp = ctx.blueprintFor(f.getType());
                        
                        if (ctx.OopBP == fbp) {
                          System.err.println("skipping Oop\n");
                          continue;
                        }
                        
                        if (ctx.OpaqueBP == fbp) {
                          System.err.println("skipping Opaque\n");
                          continue;
                        }
                        
                        if (fbp == null || !ctx.anal.isHeapAllocated(fbp)) {
                          System.err.println("pruning reference "
                            + (f == null ? (" undefined " + bp + " field at " + offset[i])
                            : f.getSelector().toString()));
                            
                          nPruned++;
                          bp.refMaskClear(offset[i]);
                        }
                        nRefs++;
                        
                      } catch (LinkageException e) { 
                        //throw e.unchecked(); 
                        System.err.println("Strange, while pruning, cannot resolve field "+ bp.fieldAt(offset[i]) + " of " + bp);
                      } 		      
 		    }
                }
*/                
            }  
 		    

 	    public void walkDomain(Domain d) {
 		nPruned = nRefs = 0;
 		
 		prunedClasses = new BitSet[DomainDirectory.maxContextID() + 1];
                for (int i = 0; i < prunedClasses.length; i++) {
                  Type.Context tc = DomainDirectory.getContext(i);
                  if (tc != null && tc.getDomain() == d) {
                    prunedClasses[i] = new BitSet(tc.getBlueprintCount());
                  }
                }
 		
 		super.walkDomain(d);
 		System.err.println("pruned " + nPruned + 
 				   " of " + nRefs +
 				   " references from class GC maps");
 	    }
 	};

    private void generateOvmCCPrologue() {
	try {
	    w = open("ovm_inline.c");
	    w.println("// This file was generated by J2C");
	    w.println("#include \"j2c.h\"");
	    w.println("#include \"ovm_inline.h\"");
	    w.println("#include \"fdutils.h\"");
	    w.println("");

	    w.println("#ifndef NDEBUG");
	    w.println("  #include \"cdstable.inc\"");
	    w.println("#endif");

	    if (cExceptions) {
	        w.println("// C exceptions");
	       
	        if (debugExceptions) {
	          if (MemoryManager.the().usesArraylets()) {
  	            w.println("#define THROWDEB ubprintf(\"Throwing exception %s in CETHROW at line %d in function %s .\\n\", *(char **)((*((e_s3_core_domain_S3Blueprint *)HEADER_BLUEPRINT(cur_exc))).ovm_dbg_1string->values), __LINE__,__func__)");
  	            w.println("#define EXCDEB ubprintf(\"Detected exception %s at line %d in function %s .\\n\", *(char **)((*((e_s3_core_domain_S3Blueprint *)HEADER_BLUEPRINT(cur_exc))).ovm_dbg_1string->values), __LINE__,__func__)");
                  } else {
  	            w.println("#define THROWDEB ubprintf(\"Throwing exception %s in CETHROW at line %d in function %s .\\n\", (char *)((*((e_s3_core_domain_S3Blueprint *)HEADER_BLUEPRINT(cur_exc))).ovm_dbg_1string->values), __LINE__,__func__)");
  	            w.println("#define EXCDEB ubprintf(\"Detected exception %s at line %d in function %s .\\n\", (char *)((*((e_s3_core_domain_S3Blueprint *)HEADER_BLUEPRINT(cur_exc))).ovm_dbg_1string->values), __LINE__,__func__)");
                  }
                } else {
	          w.println("#define THROWDEB");
	          w.println("#define EXCDEB");
                }
	        
/*
translating read barrier
              
                w.println("#define TRB_DEB ubprintf(\" Redirected pointer at line %d in function %s. \\n\",__LINE__,__func__)"); 
	        w.println("#define TRANSLATE_PTR(_ptr) ({ HEADER * _tmp = (HEADER *)(_ptr); if (unlikely(_tmp->forward!=0)) { _tmp=(HEADER *)(_tmp->forward); TRB_DEB; } ; ((typeof(_ptr)) _tmp); })");
*/	        
	      
	        w.println("#define CHECK_BOUNDS_IF_ENABLED(_x) _x");
	        
	        w.println("#ifdef CEXCEPTIONS_COUNT");
	        w.println("\t#define EXCCOUNT n_exc_thrown++");
	        w.println("#else");
	        w.println("\t#define EXCCOUNT");
	        w.println("#endif");
	      
                w.println("#define STORE_LINE_NUMBER(_dummy) ({ stored_line_number = __LINE__; })");	        	        
	      
	        if (!safePointMacros) {  
                 //w.println("#define CECALL_NONVOID(_exc_jump, _x) ({ cur_exc=0 ; typeof(_x) RET = (_x) ; if (unlikely(cur_exc!=0)) { EXCDEB ; _exc_jump;} ; RET ; })");
                 //w.println("#define CECALL_VOID(_exc_jump, _x) ({ cur_exc=0 ; (_x) ; if (unlikely(cur_exc!=0)) { EXCDEB ; _exc_jump;} ; })");
                 w.println("#define CECALL_NONVOID(_exc_jump, _x) ({ typeof(_x) RET = (_x) ; if (unlikely(cur_exc!=0)) { EXCDEB ; _exc_jump;} ; RET ; })");
                 w.println("#define CECALL_VOID(_exc_jump, _x) ({ (_x) ; if (unlikely(cur_exc!=0)) { EXCDEB ; _exc_jump;} ; })");
                } 
                
                // these are regular nullchecks
	        w.println("#define CENULLCHECK(_thrower_jump,_ref) ({ if (unlikely(_ref==0)) { EXCCOUNT ; _thrower_jump ; } })");                

	        // use this to turn NULLCHECKS off
	        // w.println("#define CENULLCHECK(_thrower_jump,_ref)");
	        
                // use this to turn null pointer exceptions into segfaults
//                w.println("#define CENULLCHECK(_thrower_jump,_ref) ({ if (unlikely(_ref==0)) { EXCCOUNT ; *((int *)0) = 0 ; _thrower_jump ; } })");                 
	        
	        if (safePointMacros && catchPointsUsed) {
	          w.println("#define CETHROW(_exc_jump,_exc) ({ THROWDEB ; EXCCOUNT ; HEADER *_tmp_exc = (HEADER *) _exc; cur_exc_dom = domainForBlueprint((S3Blueprint *)HEADER_BLUEPRINT(_tmp_exc)) ; cur_exc = _tmp_exc ; accurate::counterSetException() ; _exc_jump ; ((HEADER *)0); })");	        
	        } else {
	          w.println("#define CETHROW(_exc_jump,_exc) ({ EXCCOUNT ; HEADER *_tmp_exc = (HEADER *) _exc; cur_exc_dom = domainForBlueprint((S3Blueprint *)HEADER_BLUEPRINT(_tmp_exc)) ; cur_exc = _tmp_exc ; THROWDEB ; _exc_jump ; ((HEADER *)0); })");

// use this to turn every exception thrown explicitly to a segfault
//	          w.println("#define CETHROW(_exc_jump,_exc) ({ THROWDEB ; EXCCOUNT ; HEADER *_tmp_exc = (HEADER *) _exc; cur_exc_dom = domainForBlueprint((S3Blueprint *)HEADER_BLUEPRINT(_tmp_exc)) ; cur_exc = _tmp_exc ; *((int *)0) = 0 ; ((HEADER *)0); })");

	        }  
	      
	        if (safePointMacros) {
	          w.println("#define CEXCEPTION_JUMP undefined");
	          w.println("#define CEXCEPTION_METHOD_LEAVE undefined");
                }

	        // This does not work, because with transactions off, there is inlined code that attempts to throw 0 exception
	        // with inlining, GCC finds out and would not compile
	        //  w.println("#define CETHROW(_exc_jump,_exc) ({ cur_exc = (e_java_lang_Throwable *)_exc ; cur_exc_dom = domainForBlueprint(HEADER_BLUEPRINT((typeof(_exc)) (cur_exc))) ; _exc_jump ; ((e_java_lang_Error *)0); })");

	        w.println("");
	    }

	    if (noCppExceptions) {
                w.println("static void *curExc;");
		w.println("static int curExcDom;");
		w.println("static void setExc(void *exc,int dom) { curExc=exc; curExcDom=dom; accurate::counterSetException(); }");
		w.println("");
	    }
	} catch (IOException io) {
	    System.err.println(io.toString());
	}
    }

    public void compileDomain(Domain d, final Analysis anal) {
	// Some initial housekeeping.  Define a couple symbols,
	// generate  heap_exports .s and
	// .h files, and generate the ovm_inline.c preable
	Context ctx = findContext(d);
	ctx.anal = anal;
	Object csaObj = d.getCoreServicesAccess();
	VM_Address addr = VM_Address.fromObject(csaObj);
	FieldMagic._.findRoot((S3Domain) d, addr.asOop());

	// First, allocate space in the bootimage for shared-state
	// blueprints
	new BlueprintWalker() {
	    public void walkBlueprint(S3Blueprint bp) {
		if (bp.isSharedState()
		    && anal.shouldCompile(bp.getInstanceBlueprint()))
		    MemoryManager.the().pin(bp);
	    }
	}.walkDomain(d);
	// Next, allocate space in the bootimage for shared-state
	// objects.  They should now be layed out continuously,
	// and this should lead to fewer pages of the bootimage being
	// written.
	new BlueprintWalker() {
	    public void walkBlueprint(S3Blueprint bp) {
		if (bp.isSharedState()
		    && anal.shouldCompile(bp.getInstanceBlueprint()))
		    MemoryManager.the().pin(bp.getSharedState());
	    }
	}.walkDomain(d);
	
	if (w == null)
	    generateOvmCCPrologue();

 	if (PRUNE_GC_MAPS)
 	    refPruner.walkDomain(d);
  	methodCompiler.walkDomain(d);
 	w.println();
 	dumpTables.walkDomain(d);
    }

    public void compilationComplete() {
	super.compilationComplete();
	FieldMagic._.saveRoots();
	try {
	    // Generate global helper methods
	    w.println("");
	    w.println("# 1 \"j2c builtins\"");
	    new CodeGen(w, this);
 	    w.close();

	    // Declare blueprint, domain, and shared state objects
	    // defined by ovm_heap_exports.s
	    w = open("ovm_heap_exports.h");
	    w.println("// This file was generated by J2C");
	    w.println("");
	    genExterns.forAllDomains();
	    w.close();

	    // Generate a .s file giving the address of each
	    // domain, blueprint, and shared state object
	    w = open("ovm_heap_exports.s");
	    genHeapSymbols.forAllDomains();
	    w.close();

	    // We could generate prototypes before the method bodies, 
	    // but we don't know which symbols should alias j2cFail
	    // until after the methods have been compiled.  gcc-3.1
	    // can be very finnicky about defining a function as an
	    // alias after a forward declaration.  The nice thing
	    // about aliases vs. #define is that an alias returns the
	    // expected type.
	    w = open("ovm_inline.h");
	    w.println("// This file was generated by J2C");
	    w.println("");

	    protoPrinter.forAllDomains();
	    w.close();
	    
	    System.out.println("outputting dummy image");
	    BootImage.the().save("img");
	    System.out.println("image file size: "+Driver.imgFileSize());

	    // Now that we've generated the souce code, we can compile
	    // down to an executable and look up the addresses of
	    // symbols in our data segment.  The heap is patched with
	    // these addresses before it is written to disk.
	    if (make(new String[] { "-f", "OVMMakefile",
				    "ovm_heap_imports" }) != 0)
		System.exit(-1);
	    
	    BufferedReader r = new BufferedReader
		(new FileReader(DIRECTORY + "/code_indexes"));
	    parseCodeIndexes(r);
	    r.close();
	    
	    r = new BufferedReader
		(new FileReader(DIRECTORY + "/ovm_heap_imports"));
	    char[] hexBuf = new char[9];
	    while (true) {
		int nr = r.read(hexBuf, 0, 9);
		if (nr == -1)
		    break;
		else if (nr < 9 || hexBuf[8] != ' ')
		    throw new IOException("junk in ovm_heap_imports: `"
					  + new String(hexBuf, 0, nr)
					  + "'");
		if (AS_NEEDS_UNDERSCORE) {
		    int c = r.read();
		    if (c != '_') {
			System.err.println("missing underscore: `"
					   + (char) c + r.readLine()
					   + "'");
			continue;
		    }
		}
		String sym = r.readLine();
		if (sym == null)
		    throw new IOException("error parsing ovm_heap_imports");
		VM_Address ref =  (VM_Address) sym2addr.get(sym);
		if (ref != null) {
		    int val = Integer.parseInt(new String(hexBuf, 0, 8), 16);
		    ref.bind(val);
		    sym2addr.remove(sym);
		}
		// Only warn about missing heap refs for symbols that
		// look like they should have refs
		else if (sym.startsWith("e_") || sym.startsWith("u1_"))
		    System.err.println("no heap refs to " + sym);
	    }
	    if (!sym2addr.isEmpty()) {
		for (Iterator it = sym2addr.entrySet().iterator();
		     it.hasNext(); ) {
		    Map.Entry ent = (Map.Entry) it.next();
		    VM_Address val = (VM_Address) ent.getValue();
// 		    System.err.println(ent.getKey() + " not defined " +
// 				       "in program, using null");
		    val.bind(0);
		}
	    }
	    sym2addr = null;
	    for (StatPrinter p = statPrinters; p != null; p = p.next)
		p.printStats();
	    if (FAILURES_ARE_FATAL && compilationFailures > 0) {
		System.err.println("J2c failed to compile "
				   + compilationFailures + " methods");
		System.exit(compilationFailures);
	    }
	}
	catch (IOException e) { throw new OVMRuntimeException(e); }
    }
}
