package s3.services.bootimage;

import java.util.Stack;

import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.ExecutiveDomain;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.domain.UserDomain;
import ovm.core.repository.JavaNames;
import ovm.core.repository.Mode;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.repository.UnboundSelector;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.util.BitSet;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.OVMError;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Constants;
import s3.core.domain.S3Method;
import s3.services.bootimage.ImageObserver.BlueprintWalker;
import s3.util.PragmaTransformCallsiteIR;

/**
 * The base type of for the static analysis used to build ovm
 * executables.  A new analysis is defined by defining a subtype of
 * <code>Analysis</code> and a corresponding subtype of
 * {@link Analysis.Factory}.
 *
 * @author <a href="mailto://baker29@cs.purdue.edu"> Jason Baker </a>
 **/
public abstract class Analysis {
    static final boolean TRACE = false;
    /**
     * domain describes the Java clas hierarchy (rooted at Object)
     * that we are analyzing
     **/
    protected final Domain domain;

    /**
     * True when static analysis has finished
     * @see #analysisComplete
     **/
    protected boolean complete = false;
    
    // The following methods drive analysis:

    /**
     * Register the domain's entry points.  These entry points include
     * virtual and nonvirtual calls from the VM, as well as
     * VM-internal allocation sites, field references, and array
     * references.  These roots are registered through the add*
     * methods described below.
     **/
    protected void pushRoots() {
	// Construct blueprints for all types that are only accessed
	// through class.forName and Array.newInstance.
	domain.getRootTypes();

	if (TRACE)
	    System.err.println("pushing roots for " + domain);
	Type[] t = domain.getReflectiveNews();
	for (int i = 0; i < t.length; i++) {
	    addAllocation(t[i]);
	    if (t[i].isArray())
		addArrayAccess(t[i].asArray());
	}
	Method[] m = domain.getReflectiveCalls();
	for (int i = 0; i < m.length; i++) {//System.err.println("ADDING " + m[i]);
	    addNonvirtualCall(m[i]);}
	m = domain.getReflectiveVirtualCalls();
	for (int i =0; i < m.length; i++)
	    addVirtualCall(m[i]);
	Field[] f = domain.getReflectiveFields();
	for (int i = 0; i < f.length; i++) {
	    addFieldRead(f[i]);
	    addFieldWrite(f[i]);
	}
     }
    /**
     * Called on every field that may be dereferenced implicitly by
     * the VM.
     **/
    public abstract void addFieldRead(Field f);
    /**
     * Called on every field that may be assigned implicitly by the VM.
     **/
    public abstract void addFieldWrite(Field f);
    /**
     * Called on every type that may be allocated implicitly by the
     * VM.  <b>NOTE</b>: this list will <i>not</i> include every type
     * that may recieve an Object.clone call, or every type referenced
     * in bytecode instructions that allocate, or every type for which
     **/
    public abstract void addAllocation(Type t);
    /**
     * Called on every method definition that may be invoked by the
     * VM.  <b>NOTE:</b> this list includes source-level constructors
     * as well as ordinary methods, but this list does <i>not</i>
     * include static initializers.
     **/
    public abstract void addNonvirtualCall(Method m);
    /**
     * Called on every virtual function that may be invoked by the
     * VM through either interface or virtual function dispatch.
     **/
    public abstract void addVirtualCall(Method m);

    /**
     * Called on every type whose array elements may be accessed by
     * the VM.  <b>NOTE</b>: this list will <i>not</i> include every
     * array type passed to System.arraycopy or every type referenced
     * in array access bytecode instructions.
     **/
    public abstract void addArrayAccess(Type.Array t);

    /**
     * Analyze a domain given both it's code, and a subset of the
     * objects that may be pre-allocated in the bootimage.  This
     * method is called with ever-larger image sets until it returns
     * false.  A false return value indicates that this call to
     * analyzePartialHeap had no work to do, and implies that further
     * attempts to grow the image will not effect the analsyis results.
     * <p>
     * This is an optional operation, and probably can only be
     * implemented for the most basic static analysis techniques.
     *
     * @param it :Iterator&lt;Oop&gt;
     *
     * FIXME: We should not use Iterators here, but something more
     * friendly towards {@link ovm.core.services.memory.ExtentWalker}
     **/
    public boolean analyzePartialHeap(java.util.Iterator it) {
	throw new OVMError.Unimplemented();
    }

    /**
     * Analyze a domain that does not contain any pre-existing
     * objects.
     **/
    public void analyzeCode() {
	analyze();
    }

    /**
     * This hook is called after either a single call to
     * {@link #analyzeCode}or the final call to
     * {@link #analyzePartialHeap}.  
     **/
    public void analysisComplete() { complete = true; }

    /**
     * <code>analyzeMethod</code> is called from {@link #analyze()} to
     * process each method 
     * that may be called.  When <code>analyzeMethod</code> is called, meth
     * contains an {@link s3.core.domain.S3ByteCode} definition in OVM's
     * symbolic form.  Calls to C code have been expanded to
     * INVOKE_NATIVE instructions, and calls to magic methods have
     * been expanded to their OvmIR equivalents, but instructions that
     * require linking (such as INVOKESTATIC and GETFIELD) have not
     * been translated to their quickened form.
     **/
    public void analyzeMethod(Method meth) {
    }

    private Stack work = new Stack();
    private HashSet rewrittenMethods;
    protected final BitSet[] calledMethods;

    /**
     * Call {@link #pushRoots()}, then process each method that has
     * been added to the worklist by {@link #addMethod(Method)}.
     * Processing a method involves:
     * <ul>
     * <li> converting the method's bytecode to Ovm's internal symbolic form
     * <li> calling {@link #analyzeMethod(Method)}
     * <li> registering the method with the compiler
     * </ul>
     * @return true if this call performed any work
     **/
    protected boolean analyze() {
	pushRoots();
	// If the first call to pushRoots turned up no work, we must be done
	if (work.isEmpty())
	    return false;
	rewrittenMethods = new HashSet();
	while (!work.isEmpty()) {
	    Method m = (Method) work.pop();
	    try {
		m.getReturnType();
		for (int i = 0, max = m.getArgumentCount(); i < max; i++)
		    m.getArgumentType(i);
	    } catch (LinkageException _) {
		dontCompile(m);
	    }
	    S3ByteCode code = m.getByteCode(); 
	    domain.getRewriter().ensureState(code, S3ByteCode.EXPANDED);
	    analyzeMethod(m);
	    rewrittenMethods.add(m);
	    ImageObserver.the().addMethod(m);

	    if (Driver.img_ovmir_ascii != null) {
		code.dumpAscii(domain.blueprintFor(m.getDeclaringType()).getDomain().toString(), 
			       Driver.img_ovmir_ascii);

	    }
	}
	return true;
    }


    /**
     * <code>addMethod</code> adds a method to the worklist.  If a
     * call to {@link #analyze()} is currently active, {@link
     * #analyzeMethod(Method)} will be called on <code>m</code> before
     * <code>analyze()</code> returns.  Otherwise
     * <code>analyzeMethod(m)</code> will be called on the next
     * invocation of <code>analyze()</code>.
     **/
    public void addMethod(Method m) {
	work.add(m);
        calledMethods[m.getCID()].set(m.getUID());
    }

    // The following methods report analysis results:
    public Oop getSingleton(Blueprint bp) {
	if (bp.isSharedState())
	    return bp.getSharedState();
	else
	    return null;
    }

    /**
     * Return true if m is ever called.
     * FIXME: the set of called methods can be maintained by addMethod
     * here, rather than in subtypes.
     **/
    public boolean isMethodCalled(Method m) {
        return calledMethods[m.getCID()].get(m.getUID());
    }

//     /**
//      * This class is not used.  It could be used to track methods with
//      * exactly one call site.  (Such methods can be inlined regardless
//      * of their size, and it may be better to inline these methods
//      * before inlining small methods.)
//      **/
//     public static class CallSite {
// 	public final Method caller;
// 	public final int pc;

// 	protected CallSite(Method caller, int pc) {
// 	    this.caller = caller;
// 	    this.pc = pc;
// 	}
//     }

//     public static final CallSite REFLECTION = new CallSite(null, -1);
//     public static final CallSite MANY = new CallSite(null, -1);
    
//     /**
//      * Return a method's callers, or null if the set of callers is
//      * unknown.
//      */
//     public CallSite[] getCallSites(Method m) {
// 	return null;
//     }

    /**
     * If a virtual call to method on receiver can only be
     * dispatched to one method definition, return that method.
     * Otherwise, return null.  Kind of a goofy interface, but
     * it's easy to implement
     **/
    public Method getTarget(Blueprint recv, Method m) {
	if (m.isNonVirtual() || m.getMode().isFinal())
	    return m;
	else if (recv.getType().getMode().isFinal()) {
	    // FIXME: java 1.2.  We need to be aware of m's
	    // package-level visibility
	    UnboundSelector.Method sel = m.getSelector().getUnboundSelector();
	    return recv.getType().getMethod(sel);
	}
	else
	    return null;
    }

    public Method[] getTargets(Blueprint recv, Method m) {
	throw new OVMError.Unimplemented();
    }

    static public interface CallGraph {
	Iterator getEdges(Method m);
    }

    public CallGraph getCallerGraph() {
	throw new OVMError.Unimplemented();
    }

    public CallGraph getCalleGraph() {
	throw new OVMError.Unimplemented();
    }
    
    public static final int GOOD_CAST = 1;
    public static final int BAD_CAST = 2;
    public static final int UNKNOWN =3 ;

    /**
     * @param to   type being cast to (receiver of isSubtypeOf)
     * @param from type of value to be cast
     * @return 
     * <ul>
     * <li> <code>GOOD_CAST</code> if a cast succeeds:  in other
     * words, all concrete subtypes of from are also subtypes of
     * to.
     * <li> <code>BAD_CAST</code> if a cast fails: in other words,
     * no concrete subtypes of from are also subtypes of to
     * <li> <code>UNKNOWN</code> if a cast may either succeed or
     * fail.
     * </ul>
     * FIXME: from should not be a blueprint, but some sort of handle
     * on the analysis's idea of a variable.
     * <p>
     * FIXME: Should the RTA implementation of this be moved to Analysis?
     **/
    public int evalCast(Blueprint to, Blueprint from) {
	return UNKNOWN;
    }
	
    /**
     * @return The concrete subtypes of this blueprint, or null if
     * unknown
     * <p>
     * FIXME: should the RTA implementation of this be moved to analysis?
     **/
    public Blueprint[] concreteSubtypesOf(Blueprint bp) {
	return null;
    }

    /**
     * Return true if objects of this type may exist.  By default, all
     * array types and all class types that aren't declared abstract
     * are considered concrete.
     **/
    public boolean isConcrete(Blueprint bp) {
	Type t = bp.getType();
	return t.isArray() || (t.isClass() && !t.getMode().isAbstract());
    }

    /**
     * Return true if objects of this type may be allocated at
     * runtime.
     * <code><pre>isConcrete(bp) && !isHeapAllocated(bp)</pre></code>
     * implies that instances of a certain type exist in the bootimage
     * but not the heap.  By default, this is equivalent to
     * {@link #isConcrete(Blueprint)}.
     **/
    public boolean isHeapAllocated(Blueprint bp) {
	return isConcrete(bp);
    }

    /**
     * Return true if this blueprint's vtable may be used at runtime
     **/
    public boolean isVTableUsed(Blueprint bp) {
	return isConcrete(bp);
    }
    /**
     * Return true if this blueprint's interface table may be used at
     * runtime.
     **/
    public boolean isIFTableUsed(Blueprint bp) {
	return isConcrete(bp);
    }

    // Why does javadoc miss this import?
    /**
     * An abstract factory that is linked to a concrete implemenation
     * through the {@link ovm.core.stitcher.InvisibleStitcher}
     * configuration langauge.
     **/
    public static abstract class Factory {
	/**
	 * Construct a static analyzer for an ordinary java namespace.
	 **/
	abstract public Analysis make(UserDomain d);

	/**
	 * Construct a static analyzer for the VM-interal executive
	 * domain.  By default, we return a {@link RapidTypeAnalysis},
	 * which may be the most sophisticated analysis for which
	 * {@link #analyzePartialHeap} can be implemented.
	 **/
	public Analysis make(ExecutiveDomain d) {
	    return new RapidTypeAnalysis(d);
	}
    }

    /**
     * Return the factory configured by the
     * {@link ovm.core.stitcher.InvisibleStitcher}.
     **/
    public static Factory factory() {
	return (Factory) InvisibleStitcher.singletonFor(Factory.class);
    }

    // Helpers adapted from s3.services.j2c.Context:
    private BitSet[] knownClasses;
    private BitSet[] validClasses;
    private BitSet[] knownMethods;
    private BitSet[] validMethods;

    /**
     * If true, this is an ordinary Java domain in which
     * <code>&lt;clinit&gt;</code> methods are automatically called on
     * a class's first active use.
     **/
    protected final boolean clinitIsLive;

    private Blueprint EphemeralBP;
    
    public boolean shouldCompile(Blueprint bp) {
	if (bp.isSharedState())
	    return false;
	if (!knownClasses[bp.getCID()].get(bp.getUID())) {
	    boolean result;

	    if (EphemeralBP == null && domain.isExecutive()) {
		Type.Context ctx = domain.getSystemTypeContext();
		TypeName tn =
		    ReflectionSupport.typeNameForClass(Ephemeral.class);
		try { EphemeralBP = domain.blueprintFor(ctx.typeFor(tn)); }
		catch (LinkageException e) { throw e.unchecked(); }
	    }
	    if (bp instanceof Blueprint.Scalar
		&& (EphemeralBP == null
		    || !Driver.isSubtypeOf(bp, EphemeralBP)))
		result = true;
	    else
		result = false;
	    knownClasses[bp.getCID()].set(bp.getUID());
	    validClasses[bp.getCID()].set(bp.getUID(), result);
	    return result;
	} else
	    return validClasses[bp.getCID()].get(bp.getUID());
    }

    protected boolean shouldAnalyze(Method meth) {
	if (!knownMethods[meth.getCID()].get(meth.getUID())) {
	    // avoid using domain.getRewriter() after static analysis
	    // is finished.
	    if (complete)
		return false;
            Selector.Method msel = meth.getSelector();
            
	    Mode.Method mode = meth.getMode();
	    Blueprint bp = domain.blueprintFor(meth.getDeclaringType());
	    if (mode.isStatic())
		bp = bp.getInstanceBlueprint();
	    boolean result =
		(shouldCompile(bp)
		 && !(mode.isAbstract()
		      || (domain.isExecutive() && mode.isNative())
		      || domain.getRewriter().isMagic(meth)
		      || (!clinitIsLive
			  && msel.getUnboundSelector() == JavaNames.CLINIT)));
	    knownMethods[meth.getCID()].set(meth.getUID(), true);
	    validMethods[meth.getCID()].set(meth.getUID(), result);
	    return result;
	} else
	    return validMethods[meth.getCID()].get(meth.getUID());
    }
    public boolean shouldCompile(Method meth) {
	return shouldAnalyze(meth) && isMethodCalled(meth);
    }

    public void dontCompile(Method meth) {
	knownMethods[meth.getCID()].set(meth.getUID());
	validMethods[meth.getCID()].clear(meth.getUID());
    }

    /**
     * Iterate over all the methods in the system, calling
     * {@link #walk} for methods that not be compiled and
     * {@link #walkDead} for methods that should be ignored.
     **/
    public static abstract class MethodWalker extends BlueprintWalker {
	protected Analysis anal;
	protected Domain domain;
	
	public MethodWalker() {
	}

	protected boolean shouldWalk(Method m) {
	    return anal.shouldCompile(m);
	}

	public void walkDomain(Domain d) {
	    this.domain = d;
	    if (d == Driver.executiveDomainSprout.dom)
		this.anal = Driver.executiveDomainSprout.anal;
	    else if (d == Driver.userDomainSprout.dom)
		this.anal = Driver.userDomainSprout.anal;
	    else
		throw new Error("unknown domain " + d);
	    super.walkDomain(d);
	}
	
	/**
	 * Iterate over both static and instance methods declared
	 * in an instance type.
	 **/
	public void walkBlueprint(Blueprint bp) {
	    if (anal.shouldCompile(bp)) {
		Type.Class t;
		if (bp.getType() instanceof Type.Class) {
		    t = (Type.Class) bp.getType();
		    forMethodsOf(bp, t.localMethodIterator());
		}
		t = bp.getType().getSharedStateType();
		bp = domain.blueprintFor(t);
		forMethodsOf(bp, t.localMethodIterator());
	    }
	}

	private void forMethodsOf(Blueprint bp, Method.Iterator it) {
	    while (it.hasNext()) {
		Method meth = it.next();
		if (shouldWalk(meth))
		    walk(meth);
		else
		    walkDead(meth);
	    }
	}

	public void walkDead(Method _) { }
	public abstract void walk(Method meth);
    }

    /**
     * Return the static initialized for a shared-state type.
     **/
    public Method getClinit(Blueprint shStBp) {
	Type t = shStBp.getType();
	assert(t.isSharedState());
	return t.getMethod(JavaNames.CLINIT);
    }

    /**
     * Print any statistics that we collected over the course of analysis.
     **/
    public void printStats() {
    }

    protected Analysis(Domain d) {
	domain = d;

	clinitIsLive = !d.isExecutive();
	knownClasses = new BitSet[DomainDirectory.maxContextID() + 1];
	validClasses = new BitSet[DomainDirectory.maxContextID() + 1];
	knownMethods = new BitSet[DomainDirectory.maxContextID() + 1];
	validMethods = new BitSet[DomainDirectory.maxContextID() + 1];
        calledMethods = new BitSet[DomainDirectory.maxContextID() + 1];
	for (int i = 0; i < validClasses.length; i++) {
	    Type.Context tc = DomainDirectory.getContext(i);
	    if (tc != null && tc.getDomain() == domain) {
		knownClasses[i] = new BitSet(tc.getBlueprintCount());
		validClasses[i] = new BitSet(tc.getBlueprintCount());
		knownMethods[i] = new BitSet(tc.getMethodCount());
		validMethods[i] = new BitSet(tc.getMethodCount());
                calledMethods[i] = new BitSet(tc.getMethodCount());
	    }
	}
    }
}
