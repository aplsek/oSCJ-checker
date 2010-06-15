package s3.services.bootimage;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.ConstantResolvedFieldref;
import ovm.core.domain.ConstantResolvedMethodref;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.domain.UserDomain;
import ovm.core.repository.ConstantClass;
import ovm.core.repository.ConstantFieldref;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.MethodCodeScanner;
import ovm.util.ArrayList;
import ovm.util.BitSet;
import ovm.util.HashMap;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.SparseArrayList;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;

public class RapidTypeAnalysis extends Analysis {

    final static boolean DEBUG_HEAP_ALLOCATION = false;
    
    // set to trace call graph walk
    String indent = "";

    // set of all classes that are ever instantiated
    final BitSet[]  liveClasses;

    // set of all classes that may be heap allocated, a subset of
    // liveClasses
    final BitSet[] heapClasses;
    
//    final BitSet[] imageClasses;    
    
    // the set of all selectors for virtual call sites
    final HashSet   calledVtSelectors;

    // the set of all virtual methods in live classes
    final BitSet[]  liveVtMethods;

    // maps a selector to a list of virtual method in live classes
    final HashMap   selector2liveVtMethods;

    // Remember when <clinit> has already been checked.
    final BitSet[]  initializedClasses;

    // Maps each method <CID,UID> to a list of methods that make
    // nonvirtual calls to it.  We use the callee as a key here to
    final SparseArrayList[] nonvirtualMethodCallers;
    // Maps each method to a list of methods that refer to it through
    // virtual/interface dispatch.
    final SparseArrayList[] virtualFunctionCallers;

    Method curMethod;

    static class MethodNode {
	Method method;
	MethodNode next;
	MethodNode(Method method, MethodNode next) {
	    this.method = method;
	    this.next = next;
	}
	boolean contains(Method m) {
	    MethodNode node = this;
	    while (node != null) {
		if (node.method == m)
		    return true;
		node = node.next;
	    }
	    return false;
	}
    }

    RapidTypeAnalysis(Domain d) {
	super(d);
	int nctx = DomainDirectory.maxContextID() + 1;
	liveClasses = new BitSet[nctx];
	heapClasses = new BitSet[nctx];
//	imageClasses = new BitSet[nctx];	
	liveVtMethods = new BitSet[nctx];
	initializedClasses = clinitIsLive ? new BitSet[nctx] : null;

	nonvirtualMethodCallers = new SparseArrayList[nctx];
	virtualFunctionCallers = new SparseArrayList[nctx];
	calledVtSelectors = new HashSet();
	selector2liveVtMethods = new HashMap();

	for (int i = 0; i < nctx; i++) {
	    Type.Context tc = DomainDirectory.getContext(i);
	    if (tc != null && tc.getDomain() == domain) {
		liveClasses[i] = new BitSet(tc.getBlueprintCount());
		heapClasses[i] = new BitSet(tc.getBlueprintCount());
//		imageClasses[i] = new BitSet(tc.getBlueprintCount());		
		liveVtMethods[i] = new BitSet(tc.getMethodCount());
		nonvirtualMethodCallers[i] = new SparseArrayList();
		virtualFunctionCallers[i] = new SparseArrayList();
		if (clinitIsLive)
		    initializedClasses[i] = new BitSet(tc.getBlueprintCount());
	    }
	}
    }

    void addVirtualMethods(Blueprint bp) {
	String saveIndent = indent; 
	indent = "    ";
	Code[] vt = ((S3Blueprint) bp).getVTable();
	for (int i = 0; i < vt.length; i++) {
	    Method m = vt[i].getMethod();
	    if (m.isNonVirtual())
		continue;
	    if (shouldAnalyze(m)) {
		UnboundSelector ubs
		    = m.getSelector().getUnboundSelector();
		if (liveVtMethods[m.getCID()].get(m.getUID())) {
		    // method is inherited from a live class,
		    // nothing to do
		    continue;
		}
		else if (calledVtSelectors.contains(ubs)) {
		    // If we have already seen a call, add the
		    // virtual method to the work stack

		    // FIXME: to build a stack trace for call and
		    // for allocation TracingWorkList needs to
		    // know why isVirtual is tree, and to see
		    // every allocation point
		    processMethod(m, true);
		}
		else  {
		    // Otherwise, remember to add it if we do
		    // encounter a call
		    liveVtMethods[m.getCID()].set(m.getUID());
		    MethodNode next
			= (MethodNode) selector2liveVtMethods.get(ubs);
		    selector2liveVtMethods.put(ubs,
					       new MethodNode(m, next));
		}
	    }
	}
	indent = saveIndent;
    }

    public void addConcreteType(Blueprint bp) {
	// Arrays don't create any more work, but we need to remember
	// them for checkcast elimination
	int cid = bp.getCID();
	int bpid = bp.getUID();
	if (bp.getType().isArray())
	    liveClasses[cid].set(bpid);
	if (shouldCompile(bp) &&!liveClasses[cid].get(bpid)) {
	    Type.Compound t = bp.getType().asCompound();
	    if (TRACE)
		System.err.println(indent + t.getUnrefinedName()
				   + " is concrete");
	    liveClasses[cid].set(bpid);
	    addVirtualMethods(bp);
	}

	if (clinitIsLive && bp.isScalar())
	    addClinit(bp.getSharedState().getBlueprint());
    }
/*
    public void addImageType(Blueprint bp) {

	int cid = bp.getCID();
	int uid = bp.getUID();

	if (!imageClasses[cid].get(uid)) {
	  imageClasses[cid].set(uid);
	}	
      
    }
*/
    public void addHeapType(Blueprint bp) {
	int cid = bp.getCID();
	int uid = bp.getUID();
	if (!heapClasses[cid].get(uid)) {
	    addConcreteType(bp);
	    if (liveClasses[cid].get(uid)) {
		heapClasses[cid].set(uid);
                if (DEBUG_HEAP_ALLOCATION) {
                  System.err.println("adding "+bp+" to heap types (cid="+cid+", uid="+uid+")");
                }
            } else {
                if (DEBUG_HEAP_ALLOCATION) {
                  System.err.println("not adding "+bp+" to heap types, because it's not live ");
                }
                // can this happen ?
            }
	}
    }
    
    private void addClinit(Blueprint shSt) {
	if (!initializedClasses[shSt.getCID()].get(shSt.getUID())) {
	    initializedClasses[shSt.getCID()].set(shSt.getUID());
	    Method clinit = getClinit(shSt);
	    if (clinit != null)
		addNonvirtualCall(clinit);

	    Blueprint ibp = shSt.getInstanceBlueprint();
	    if (ibp != ((S3Domain) domain).ROOT_BLUEPRINT) {
		try {
		    Type.Class sup = ibp.getType().getSuperclass();
		    Oop supShSt = sup.getSharedStateType().getSingleton();
		    addClinit(supShSt.getBlueprint());
		} catch (NullPointerException npe) {
		    System.err.println("Something is wrong with the blueprint " + ibp);
		}
						    
	    }
	}
    }

    /**
     * Update our callgraph-building data structures with a new call.
     * We maintain seperate tables that map a method object to it's
     * virtual and nonvirtual callers.
     **/
    private void recordCall(Method method, SparseArrayList[] callers) {
	if (true || curMethod == null)
	    return;
	
	SparseArrayList ctxCallers = callers[method.getCID()];
	MethodNode graphNode = (MethodNode) ctxCallers.get(method.getUID());
	if (graphNode == null || graphNode.method != curMethod) {
	    ctxCallers.set(method.getUID(),
			   new MethodNode(curMethod, graphNode));
	}
    }

    public void addVirtualCall(Method method) {
	UnboundSelector ubs = method.getSelector().getUnboundSelector();
	if (!calledVtSelectors.contains(ubs)) {
	    if (TRACE)
		System.err.println(indent + "virtual call to    " + ubs);
	    calledVtSelectors.add(ubs);
	    String saveIndent = indent;
	    indent = indent == "" ? "  " : "    ";
	    for (MethodNode n = (MethodNode) selector2liveVtMethods.get(ubs);
		 n != null; n = n.next)
		processMethod(n.method, true);
	    // this entry no longer needed
	    selector2liveVtMethods.put(ubs, null);
	    indent = saveIndent;
	}
	recordCall(method, virtualFunctionCallers);
    }

    public void addAllocation(Type t) {
	addHeapType(domain.blueprintFor(t));
    }

    public void addNonvirtualCall(Method method) {
	processMethod(method, false);
	if (clinitIsLive
	    && method.getSelector().getUnboundSelector() != JavaNames.CLINIT
	    && method.getDeclaringType().getDomain() == domain)
	{
	    Blueprint bp = domain.blueprintFor(method.getDeclaringType());
	    addClinit(bp.isSharedState() ? bp
		      : (Blueprint) bp.getSharedState().getBlueprint());
	}
	recordCall(method, nonvirtualMethodCallers);
    }

    public void addFieldRead(Field f) {
	Type t = f.getDeclaringType();
	if (clinitIsLive && t.isSharedState()) {
	    addClinit(domain.blueprintFor(t));
	}
	// Force field types to be resolved as well, even if we only
	// store null
	// FIXME: Does this load too much?
	try { f.getType(); }
	catch (LinkageException _) { }
    }
    public void addFieldWrite(Field f) {
	Type t = f.getDeclaringType();
	if (clinitIsLive && t.isSharedState()) {
	    addClinit(domain.blueprintFor(t));
	}
	// Force field types to be resolved as well, even if we only
	// store null
	// FIXME: Does this load too much?
	try { f.getType(); }
	catch (LinkageException _) { }
    }
    public void addArrayAccess(Type.Array _) { }

    private void processMethod(Method method, boolean isVirtual) {
	int cnum = method.getCID();
	int num = method.getUID();
	if (!calledMethods[cnum].get(num))
	    if (shouldAnalyze(method)) {
		if (TRACE)
		    System.err.println(indent +
				       (isVirtual
					? "push "
					: "nonvirtual call to ")
				       + method.getSelector());
		addMethod(method);
	    }
	    else
		System.err.println("skip bad method " + method);
    }

    private final MethodCodeScanner visitor = new MethodCodeScanner() {
	    public void visit(Instruction.Invocation i) {
		ConstantMethodref mr = i.getConstantMethodref(buf, cp);
		try {
		    Method m = ((ConstantResolvedMethodref) mr).getMethod();
		    if (m.getDeclaringType().getDomain() != domain)
			return;
		    if (m.isNonVirtual() 
			|| m.getMode().isFinal()
			|| m.getDeclaringType().getMode().isFinal()
			|| i instanceof Instruction.INVOKESPECIAL)
			addNonvirtualCall(m);
		    else
			addVirtualCall(m);
		} catch (ClassCastException e) {
		    warnMissing(i.getSelector(buf, cp).toString());
		}
	    }

	    // This catches both reads and writes, which is actually what
	    // we want.
	    public void visit(Instruction.FieldAccess i) {
		ConstantFieldref r = i.getConstantFieldref(buf, cp);
		try {
		    Field f = ((ConstantResolvedFieldref) r).getField();
		    addFieldRead(f); // read/write doesn't matter
		} catch (ClassCastException e) {
		    warnMissing(i.getSelector(buf, cp).toString());
		}
	    }

	    // Force resolution of instance fields, or move that into
	    // IRewriter?
	    public void visit(Instruction.Allocation i) {
		ConstantClass cl = i.getResultType(buf, cp);
		try {
		    addHeapType((Blueprint) cl);
		} catch (ClassCastException e) {
		    warnMissing("in " + i + ": " + cl.toString());
		    //e.printStackTrace(System.err);
		}
	    }

	    public void visit(Instruction.ConstantPoolLoad i) {
		int idx = i.getCPIndex(buf);
		if (cp.getTagAt(idx) == JVMConstants.CONSTANT_SharedState) {
		    TypeName.Gemeinsam gtn =
			(TypeName.Gemeinsam) cp.getConstantAt(idx);
		    Type.Context ctx =
			code.getMethod().getDeclaringType().getContext();
		    try {
			Type t = ctx.typeFor(gtn.getInstanceTypeName());
			// Even if we don't call addClinit, we still
			// need to call typeFor for its side effect
			// (defining a type).
			if (clinitIsLive && t.isScalar())
			    addClinit(t.getSharedStateType().
				      getSingleton().getBlueprint());
		    } catch (LinkageException e) {
			warnMissing("Can't resolve class constant "  + gtn
				    + ": " + e.getMessage());
		    }
		}
	    }
	};

    public void analyzeMethod(Method m) {
	super.analyzeMethod(m);
	if (TRACE)
	    System.err.println("anlayzing " + m.getSelector());
	curMethod = m;
	indent = "  ";
	visitor.run(m);
	indent = "";
	curMethod = null;
    }

    public boolean analyzePartialHeap(java.util.Iterator it) {
	while (it.hasNext()) {
	    addConcreteType(((Oop) it.next()).getBlueprint());
	}
	return analyze();
    }

    public boolean analyze() {
	Blueprint classBP = domain.blueprintFor(domain.getMetaClass());
	// We don't mark java.lang.Class as concrete, because it is in
	// fact an abstract supertype all shared-state types.  We do,
	// however, want to make sure that methods defined in
	// java.lang.Class are treated as live
	addVirtualMethods(classBP);
	if (clinitIsLive)
	    addClinit(classBP.getSharedState().getBlueprint());
	return super.analyze();
    }


    // Code that uses analysis results to answer questions such as
    // "can this call be devirtualized," and "does this subtype test
    // suceed."  At one point, there was a seperate class called
    // RapidTypeAnalysis.Result that answered these questions.

    // A refrence to a possibly null Method object
    static class Devirt {
	final Method method;
	Devirt(Method method) { this.method = method; }
    }
    static final Devirt VIRTUAL = new Devirt(null);

    Blueprint[]   liveClassList;
    BitSet[]      liveVTables;
    BitSet[]      liveIFTables;

    int maxIFTable;
	
    Blueprint[][][] liveSubtypeCache;
    byte[][][][]      safeCastCache;
    Object[][][]      cacheDevirtV;
    Object[][][]      cacheDevirtI;
	
    BitSet[] vtCallee;

    // Maintain counters to monitor caching and accuracy
    int subtypeCacheHits;

    int devirtCalls;
    int devirtSuccess;

    int castChecks;
    int castSuccess;
    int castFailure;

    int nclasses;
    int nmethods;
	
    int sumCardinality(BitSet[] bs) {
	int ret = 0;
	for (int i = 0; i < bs.length; i++)
	    if (bs[i] != null)
		ret += bs[i].cardinality();
	return ret;
    }

    public void analysisComplete() {
	super.analysisComplete();
	assert (liveClassList == null);

	// We don't really need to check live classes by number,
	// but we do need to iterate over them.  Copy all live
	// blueprints into an array
	int nLive = sumCardinality(liveClasses);

	// Be sure to treat all shared-state blueprints as live.
	// The shared-state objects for linked-in classes are part of
	// the bootimage, but because the user domain analysis does
	// not depend on the bootimage contents, we do not know that
	// they are live.  In the executive domain, we do call
	// addConcreteType for shared-states, but they are not added
	// to the bitset, because shouldCompile() returns false.
	for (Iterator it = domain.getBlueprintIterator();
	     it.hasNext(); ) {
	    Blueprint bp = (Blueprint) it.next();
	    if (bp.isSharedState())
		nLive++;
	}
	liveClassList = new Blueprint[nLive];
	int idx = 0;
	for (Iterator it = domain.getBlueprintIterator();
	     it.hasNext(); ) {
	    Blueprint bp = (Blueprint) it.next();
	    if (liveClasses[bp.getCID()].get(bp.getUID())) {
		liveClassList[idx++] = bp;
		Code[] tab = ((S3Blueprint) bp).getIFTable();
		if (tab != null && tab.length > maxIFTable)
		    maxIFTable = tab.length;
	    } else if (bp.isSharedState()) {
		liveClassList[idx++] = bp;
	    }
	}


	int nctx = DomainDirectory.maxContextID() + 1;
	liveVTables = new BitSet[nctx];
	liveIFTables = new BitSet[nctx];
	// Hmm.  Would it make sense to create a linked list of
	// each blueprint's direct subclasses and classes that
	// directly or indirectly implement an interface?
	// (Somehow I find the thought of listing an interface's
	// subclasses a little scary.  It seems like there may be
	// some way to generate cycles.)
	liveSubtypeCache = new Blueprint[nctx][][];

	// Try to speed up evalCast and devirtualize by memoizing
	// results.  I have a feeling that these caches won't
	// suffice.
	safeCastCache = new byte[nctx][][][];
	cacheDevirtV = new Object[nctx][][];
	cacheDevirtI = new Object[nctx][][];

	for (int i = 0; i < nctx; i++) {
	    Type.Context tc = DomainDirectory.getContext(i);
	    if (tc != null && tc.getDomain() == domain) {
		int nbp = tc.getBlueprintCount();
		nclasses += nbp;
		nmethods += tc.getMethodCount();
		liveVTables[i] = new BitSet(nbp);
		liveIFTables[i] = new BitSet(nbp);
		liveSubtypeCache[i] = new Blueprint[nbp][];
		safeCastCache[i] = new byte[nbp][][];
		cacheDevirtV[i] = new Object[nbp][];
		cacheDevirtI[i] = new Object[nbp][];
	    }
	}
    }


    public void printStats() {
	System.err.println("\nRapid Type Analysis for " + domain + ":");
	System.err.println("\t" + liveClassList.length + " of " + nclasses + " blueprints are concrete");
	System.err.println("\t" + sumCardinality(calledMethods) + " of " + nmethods + " methods are called");
	System.err.println("\t" + devirtSuccess + " of " + devirtCalls + " calls devirtualized");
	System.err.println("\t" + castSuccess + " of " + castChecks + " casts known to succeed");
	System.err.println("\t" + castFailure + " of " + castChecks + " casts known to fail");
	System.err.println("\t" + sumCardinality(liveVTables) + " virtual tables used");
	System.err.println("\t" + sumCardinality(liveIFTables) + " interface tables used");
	// ... still need to print cache stats
    }

    ArrayList subtypeList = new ArrayList();

    public Blueprint[] concreteSubtypesOf(Blueprint bp) {
	int cid = bp.getCID();
	int uid = bp.getUID();
	Blueprint[] ret = liveSubtypeCache[cid][uid];
	if (ret == null) {
	    if (bp == ((S3Domain) domain).ROOT_BLUEPRINT)
		ret = liveSubtypeCache[cid][uid] = liveClassList;
	    else {
		for (int i = 0; i < liveClassList.length; i++)
		    if (liveClassList[i].isSubtypeOf(bp))
			subtypeList.add(liveClassList[i]);
		ret = new Blueprint[subtypeList.size()];
		subtypeList.toArray(ret);
		liveSubtypeCache[cid][uid] = ret;
		subtypeList.clear();
	    }
	}
	subtypeCacheHits++;
	return ret;
    }

    public boolean isBlueprintConcrete(Blueprint bp) {
	return liveClasses[bp.getCID()].get(bp.getUID());
    }

    public boolean isHeapAllocated(Blueprint bp) {
	Blueprint[] sub = concreteSubtypesOf(bp);
	if (DEBUG_HEAP_ALLOCATION) {
    	  System.err.println("isHeapAllocated: "+bp+"(cid="+bp.getCID()+", uid="+bp.getUID()+") checking subtypes:");
        }
	for (int i = 0; i < sub.length; i++) {
	
// this used to be here - and I think it is a major bug	
// (and, fixing it really made some bugs go away)
//	    if (heapClasses[bp.getCID()].get(sub[i].getUID())) { ??

	    if (heapClasses[sub[i].getCID()].get(sub[i].getUID())) {
	        if (DEBUG_HEAP_ALLOCATION) {
                  System.err.println("  subtype "+sub[i]+"(cid="+sub[i].getCID()+", uid="+sub[i].getUID()+") is a heap class, thus "+bp+" is heap allocated");
                }
		return true;
            }
            if (DEBUG_HEAP_ALLOCATION) {
              System.err.println("  subtype "+sub[i]+"(cid="+sub[i].getCID()+", uid="+sub[i].getUID()+")  is not a heap class");
            }
        }
        if (DEBUG_HEAP_ALLOCATION) {
          System.err.println("  No subtype of "+bp+" is a heap class, thus it is not heap allocated");
        }
	return false;
    }

/*
    public boolean isImageAllocated(Blueprint bp) {
    
	Blueprint[] sub = concreteSubtypesOf(bp);
	for (int i = 0; i < sub.length; i++)
	    if (imageClasses[bp.getCID()].get(sub[i].getUID()))
		return true;
	return false;
    }
*/    

    public CallGraph getCallerGraph() {
	final int nctx = DomainDirectory.maxContextID() + 1;
	final MethodNode[][] allCallers = new MethodNode[nctx][];
	for (int i = 0; i < nctx; i++) {
	    Type.Context tc = DomainDirectory.getContext(i);
	    if (tc.getDomain() == domain)
		allCallers[i] = new MethodNode[tc.getMethodCount()];
	}
	new MethodWalker() {
	    public void walk(Method m) {
		allCallers[m.getCID()][m.getUID()] =
		    (MethodNode)nonvirtualMethodCallers[m.getCID()].get(m.getUID());
	    }
	}.walkDomain(domain);
	new MethodWalker() {
	    private void pushCallers(MethodNode methodCallers, Method callee) {
		int CID = callee.getCID(), UID = callee.getUID();
		while (methodCallers != null) {
		    if (allCallers[CID][UID] == null
			|| !allCallers[CID][UID].contains(methodCallers.method)) {
			allCallers[CID][UID] =
			    new MethodNode(methodCallers.method,
					   allCallers[CID][UID]);
		    }
		    methodCallers = methodCallers.next;
		}
	    }

	    public void walk(Method  base) {
		MethodNode vtCallers = 
		    (MethodNode) virtualFunctionCallers[base.getCID()].get(base.getUID());
		if (vtCallers == null)
		    return;
		Blueprint rcv = domain.blueprintFor(base.getDeclaringType());
		Object tgt = getTargets1(rcv, base);
		if (tgt == null)
		    return;
		else if (tgt instanceof Method) {
		    pushCallers(vtCallers, (Method) tgt);
		} else {
		    Method[] target = (Method[])tgt;
		    for (int i = 0; i < target.length; i++)
			pushCallers(vtCallers, target[i]);
		}
	    }
	}.walkDomain(domain);

	return new CallGraph() {
	    public Iterator getEdges(Method m) {
		final MethodNode node = allCallers[m.getCID()][m.getUID()];
		return new Iterator() {
		    MethodNode cur = node;
		    public boolean hasNext() { return cur != null; }
		    public Object next() {
			Method ret = cur.method;
			cur = cur.next;
			return ret;
		    }
		    public void remove()  {
			throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }

    private ArrayList targets = new ArrayList();
    private synchronized Object getTargets1(Blueprint receiver, Method m) { 
	int rc = receiver.getCID();
	int rNum = receiver.getUID();
	Object[] dv;
	BitSet[] liveTables;
	int index;
	String table;
	
	if (m.isVirtual()) {
	    dv = cacheDevirtV[rc][rNum];
	    liveTables = liveVTables;
	    if (dv == null) {
		dv = new Object[((S3Blueprint) receiver).getVTable().length];
		cacheDevirtV[rc][rNum] = dv;
	    }
	    index = receiver.getVirtualMethodOffset(m);
	    assert index != -1: ("no vtable offset for " + m +
				 " in " + receiver);
	    table = "vTable";
	}
	else if (m.isInterface()) {
	    dv = cacheDevirtI[rc][rNum];
	    liveTables = liveIFTables;
	    if (dv == null) {
		dv = new Object[receiver.getType().isInterface()
				? maxIFTable
				: ((S3Blueprint) receiver).getIFTable().length];
		cacheDevirtI[rc][rNum] = dv;
	    }
	    index = receiver.getInterfaceMethodOffset(m);
	    assert index != -1: "interface index -1 for " + m;
	    table = "ifTable";
	}
	else
	    throw new RuntimeException("bad table name");

	if (index >= dv.length)
	    /* This indicates dead code.  An attempt is being made
	     * to call through an interface that is never
	     * instantiated.
	     */
	    return null;

	Object result = dv[index];
	if (result == null) {
	    Blueprint[] bp = concreteSubtypesOf(receiver);
	    Method candidate = null;
	    boolean valid = true;
	    for (int i = 0; i < bp.length; i++) {
		Method c2 = ((S3Blueprint) bp[i]).lookupMethod(index, table);
		// This call may generate an AbstractMethodError,
		// don't devirtualize it!
		if (c2.getMode().isAbstract()) {
		    System.err.println(bp[i]+" inherits abstract method "+c2);
		    valid = false;
		    if (candidate != null)
			targets.add(candidate);
		}
		if (!valid) {
		    if (!targets.contains(c2))
			targets.add(c2);
		} else if (candidate == null)
		    candidate = c2;
		else if (candidate != c2) {
		    valid = false;
		    targets.add(candidate);
		    targets.add(c2);
		}
	    }
	    if (valid) {
		// Hmm, another symptom of unreachable code
		if (candidate == null)
		    result = new Method[0];
		else
		    result = candidate;
	    } else {
		Method[] mr = new Method[targets.size()];
		targets.toArray(mr);
		targets.clear();
		result = mr;
	    }
	    if (!valid)
		for (int i = 0; i < bp.length; i++)
		    if (bp[i].getType().isScalar()) {
			Blueprint bp_i = bp[i];
			if (bp_i.isSharedState()) {
// 			    System.err.println("marking class vtable live "
// 					       + " for shared state");
			    bp_i = ((S3Blueprint) bp_i).getParentBlueprint();
			}
			liveTables[bp_i.getCID()].set(bp_i.getUID());
		    }
		
	    dv[index] = result;
	}
	devirtCalls++;
	if (result instanceof Method)
	    devirtSuccess++;
	return result;
    }

    public Method getTarget(Blueprint receiver, Method m) {
	Object dv = getTargets1(receiver, m);
	if (dv instanceof Method)
	    return (Method) dv;
	else
	    return null;
    }

    public Method[] getTargets(Blueprint receiver, Method m) {
	Object dv = getTargets1(receiver, m);
	if (dv instanceof Method)
	    return new Method[] { (Method) dv };
	else
	    return (Method[]) dv;
    }

    public int evalCast(Blueprint from, Blueprint to) {
	int fc = from.getCID();
	int fNumber = from.getUID();
	int tc = to.getCID();
	int tNumber = to.getUID();
	    
	if (safeCastCache[fc][fNumber] == null) {
	    byte[][] tcs = safeCastCache[fc][fNumber]
		= new byte[DomainDirectory.maxContextID()+1][];
	    for (int i = 0; i < tcs.length; i++) {
		Type.Context tctx = DomainDirectory.getContext(i);
		if (tctx != null && tctx.getDomain() == domain)
		    tcs[i] = new byte[tctx.getBlueprintCount()];
	    }
	}
	int isSafe = safeCastCache[fc][fNumber][tc][tNumber];
	if (isSafe == 0) {
	    // casting from Object is common.  Make it fast
	    if (from == ((S3Domain) domain).ROOT_BLUEPRINT) {
		int len = concreteSubtypesOf(to).length;
		isSafe = (len == liveClassList.length ? GOOD_CAST
			  : len == 0 ? BAD_CAST
			  : UNKNOWN);
	    } else {
		boolean canSucceed = false;
		boolean canFail = false;
		Blueprint[] bp = concreteSubtypesOf(from);
		for (int i = 0; i < bp.length; i++) {
		    if (bp[i].isSubtypeOf(to))
			canSucceed = true;
		    else
			canFail = true;

		    if (canSucceed && canFail)
			break;
		}
		isSafe = (canSucceed
			  ? (canFail ? UNKNOWN : GOOD_CAST)
			  : BAD_CAST);
	    }
	    safeCastCache[fc][fNumber][tc][tNumber] = (byte) isSafe;
	}
	castChecks++;
	if (isSafe == GOOD_CAST)
	    castSuccess++;
	else if (isSafe == BAD_CAST)
	    castFailure++;
	return isSafe;
    }

    public boolean isVTableUsed(Blueprint bp) {
	return liveVTables[bp.getCID()].get(bp.getUID());
    }

    public boolean isIFTableUsed(Blueprint bp) {
	return liveIFTables[bp.getCID()].get(bp.getUID());
    }

    public static class Factory extends Analysis.Factory {
	public Analysis make(UserDomain d) {
	    return new RapidTypeAnalysis(d);
	}
    }
}
