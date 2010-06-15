package s3.services.transactions;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantResolvedInstanceFieldref;
import ovm.core.domain.ConstantResolvedInstanceMethodref;
import ovm.core.domain.ConstantResolvedInterfaceMethodref;
import ovm.core.domain.ConstantResolvedMethodref;
import ovm.core.domain.ConstantResolvedStaticFieldref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.execution.Native;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.repository.UnboundSelector;
import ovm.core.repository.TypeName.Compound;
import ovm.core.repository.TypeName.Scalar;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.JVMConstants.DereferenceOps;
import ovm.services.bytecode.JVMConstants.InvokeSystemArguments;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import ovm.services.bytecode.editor.Cursor;
import ovm.services.bytecode.editor.CodeFragmentEditor.ExceptionHandlerList;
import ovm.util.ArrayList;
import ovm.util.BitSet;
import ovm.util.HashMap;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.UnicodeBuffer;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3MemberResolver;
import s3.services.bytecode.ovmify.IRewriter;
import s3.services.bytecode.ovmify.NativeCallGenerator;
import s3.util.PragmaException;
import s3.util.PragmaTransformCallsiteIR;
import ovm.core.services.memory.MemoryPolicy;

/**
 * This visitor insert calls to transaction logging methods. Before every memory write
 * the original value of the location will be saved. Method calls are transformed to
 * calls of the corresponding logging methods.
 */
public abstract class PARifyingRewriteVisitor extends IRewriter.RewriteVisitor {

    static String _transSuffix;

    BitSet specPC = new BitSet();
    boolean parLogged = false;
    S3Transaction transact;

    int READ_COUNT;
    int WRITE_COUNT;
    boolean METHOD_IGNORED = true;

    public PARifyingRewriteVisitor(S3Transaction t, IRewriter ir, 
				   S3Blueprint bp, CodeFragmentEditor cfe_) {
	ir.super(bp, false, false);  
	cfe = cfe_; transact = t; 
	_transSuffix = t.getTRANS_SUFFIX();
    }


    protected boolean parSpecializeInstruction() { return specPC.get(getPC()); }
    
    public void beginEditing(InstructionBuffer buf, CodeFragmentEditor cfe) {
	super.beginEditing(buf, cfe);

	if(GENERATE_PAR_GRAPH)
	    par_graph.put(getSelector(), new MthdInfo());
			
	parLogged = buf.getSelector().getName().endsWith(_transSuffix);
	specPC.set(0, buf.limit(), parLogged);
	TypeName.Scalar exceptionType;
	if (this.isExecutive_)
	    exceptionType = Transaction.ED_ABORTED_EXCEPTION;
	else 
	    exceptionType = Transaction.ABORTED_EXCEPTION;
	
	for (ExceptionHandlerList curr = cfe.getExceptionHandlers(); curr != null; curr = curr.next()) {
	    if (curr.getExceptionType() == null || 
		!curr.getExceptionType().equals(exceptionType))
		continue;
	    
	    if (parLogged) S3Transaction.NESTED_ATOMIC_BLOCK_COUNT++;
	    else S3Transaction.ATOMIC_BLOCK_COUNT++;
	    METHOD_IGNORED = false;
	    
	    if (parLogged) { curr.delete(); continue; }	                
	    int start = curr.getStart().getCursor().getPC();
	    int finish = curr.getEnd().getCursor().getPC();
	    specPC.set(start, finish - 1, true);		    		
	    for (ExceptionHandlerList inner = cfe.getExceptionHandlers(); inner != null; inner = inner.next()) {
		if (inner == curr || (inner.getExceptionType() != exceptionType)) continue;
		int innerStart = inner.getStart().getCursor().getPC();
		if ((innerStart >= start) && (innerStart < finish)) S3Transaction.NESTED_ATOMIC_BLOCK_COUNT++;
		if ((innerStart >= start) && (innerStart < finish)) inner.delete();			    

	    }
	}      		    
	while (buf.hasRemaining()) buf.get();
	buf.rewind();	   	    
    }
    
    protected void addRTMethodCall(Cursor c, UnboundSelector.Method meth) {
	int offset = S3MemberResolver.resolveVTableOffset(rtExportsBpt(), meth, null);
	Method m = S3MemberResolver.resolveInstanceMethod(rtExportsBpt().getType().asCompound(), meth, null);
	Type.Context ctx = curBp_.getType().getContext();
	Object r = MemoryPolicy.the().enterMetaDataArea(ctx);
	try {
	    ConstantResolvedInstanceMethodref imi = ConstantResolvedInstanceMethodref.make(m, offset, rtExportsBpt());
	    c.addINVOKEVIRTUAL(imi);
	} finally {
	    MemoryPolicy.the().leave(r);
	}
    }
    

    /**
     * This visitor deals with labeling methods " T".  It is separate from
     * the actual logging inserter because invocations of native methods need
     * to be changed before PragmaTransformCallsiteIR rewrites invokations.
     * 
     * The actual logging needs to take place after PragmaTransformCallsiteIR 
     * rewriting because some of those rewriters introduce putfields that have to
     * be logged.
     *
     */
    public static class TransactionalizeVisitor extends PARifyingRewriteVisitor {

	public TransactionalizeVisitor(S3Transaction t, IRewriter ir,
				       S3Blueprint bp, CodeFragmentEditor cfe_) {
	    super(t, ir, bp, cfe_);
	}

	public void visit(Instruction.INVOKEVIRTUAL i) {
	    try {
	        ConstantResolvedInstanceMethodref vmi = 
		    cp.resolveInstanceMethod(i.getCPIndex(buf));
	        visitInvoke(i, vmi, (S3Blueprint) vmi.getStaticDefinerBlueprint());
	    } catch (LinkageException e) {
	        IRewriter.warn(i, e.getMessage());
	    }
	}

	public void visit(Instruction.INVOKESTATIC i) {
	    try {
	        ConstantResolvedStaticMethodref smi = 
		    getConstants().resolveStaticMethod(i.getCPIndex(buf));
	        visitInvoke(i, smi, (S3Blueprint) smi.getSharedState().getBlueprint());
	    } catch (LinkageException e) { 
	        IRewriter.warn(i, e.getMessage()); 
	    }
	}

	public void visit(Instruction.INVOKESPECIAL i) {
	    try { 
	        ConstantResolvedInstanceMethodref smi = 
		    getConstants().resolveInstanceMethod(i.getCPIndex(buf));
	        visitInvoke(i, smi, (S3Blueprint) smi.getStaticDefinerBlueprint());
	    } catch (LinkageException e) { 
	        IRewriter.warn(i, e.getMessage());  
	    }
	}

	public void visit(Instruction.INVOKEINTERFACE i) {
	    try { 
	        ConstantResolvedInterfaceMethodref smi = 
		    getConstants().resolveInterfaceMethod(i.getCPIndex(buf));
	        visitInvoke(i, smi, (S3Blueprint) smi.getStaticDefinerBlueprint());
	    } catch (LinkageException e) { 
	        IRewriter.warn(i, e.getMessage());  
	    }
	}

	private void visitInvoke(Instruction invoker, 
				 ConstantResolvedMethodref crm, 
				 S3Blueprint bp) throws ovm.core.domain.LinkageException {
	    if (!this.parSpecializeInstruction()) return;
	
	    if (PARifyingRewriteVisitor.org_ovmj_transact_Transaction == 
		crm.asSelector().getDefiningClass().asScalar()) {
		// this gets rid of the calls to Transaction.start / commit
		// in well-written programs, but not all possible calls
		String name = crm.asSelector().getName();
		if ((name.indexOf("commit") != -1) || (name.indexOf("start") != -1))
		    cfe.removeInstruction(getPC());
	    }
	    
	    boolean isNative = false;
	    if (doSystemRewriting_ &&
	        (isReplacedWithBytecode(crm.asSelector(), bp) ||
	         (isNative = isNativeCall(crm, invoker, bp)) ||
	         !kernelPAR(crm))) {
	        // if the invocation is native, then do some other things.
	        if (isNative && !nativePAR(crm, bp)) {
		    Compound c = crm.asSelector().getDefiningClass();
	
		    if (c.equals(ovm_util_Mem_PollingAware_Nat)) {
	
			// if it can be replaced by Java code, do so
			Selector.Method sel = crm.asSelector();
	
			int idx = getTransNameAsUTF8Index(sel);
			Selector.Method newSel = 
			    Selector.Method.make(UnboundSelector.Method.make(idx, sel.getDescriptor()),
						 ovm_util_Mem_PollingAware_NatJava);
			int index = cfe.getConstantsEditor().addMethodref(newSel);
			ConstantResolvedStaticMethodref imi = getConstants().resolveStaticMethod(index);
			cfe.replaceInstruction().addINVOKESTATIC(imi);
		    } else {
			// Otherwise, throw an exception on invocation
			Cursor cur = cfe.getCursorAfterMarker(getPC());
			cur.addResolvedRefLoadConstant(rtExportsOop());
			cur.addLdc(new RepositoryString(crm.toString()));
			addRTMethodCall(cur, nativeExceptionSelector);
		    }
	        }
	        return;
	    }
	
	    if (verboten(crm)) 
	        return;
	
	    parifyCall(crm, invoker);
	}

	private void parifyCall(ConstantResolvedMethodref imi, Instruction i) throws LinkageException {
	    assert parSpecializeInstruction() : "parifyCall called from non-specialize context";
	    int idx = parSpecializeCall(imi);
	    if (imi instanceof ConstantResolvedInstanceMethodref) {
		imi = getConstants().resolveInstanceMethod(idx);		   
	    } else if (imi instanceof ConstantResolvedStaticMethodref) {
		imi = getConstants().resolveStaticMethod(idx);
	    } else if (imi instanceof ConstantResolvedInterfaceMethodref) {
		imi = getConstants().resolveInterfaceMethod(idx);		    
	    }
	    
	    //System.out.println("invoking "+imi.getMethod()+" from PAR scope");
	    if(GENERATE_PAR_GRAPH) {
		MthdInfo tmp_ = (MthdInfo)par_graph.get(getSelector());
		tmp_.children.add(imi.asSelector());
		tmp_.chldCnt++; tmp_.total++;
	    }
	    
	    if (i instanceof Instruction.INVOKE_NATIVE) {
		IRewriter.warn(i, "Invoking " + imi + " from atomic");
	    } else if (i instanceof Instruction.INVOKE_SYSTEM) {
		IRewriter.warn(i, "Invoking " + imi + " from atomic");
	    } else if (i instanceof Instruction.INVOKEINTERFACE) {
		cfe.replaceInstruction().addINVOKEINTERFACE(imi);
	    } else if (i instanceof Instruction.INVOKEVIRTUAL) {		
		cfe.replaceInstruction().addINVOKEVIRTUAL(imi);
	    } else if (i instanceof Instruction.INVOKESTATIC) {
		cfe.replaceInstruction().addINVOKESTATIC(imi);
	    } else if (i instanceof Instruction.INVOKESPECIAL) {
		cfe.replaceInstruction().addINVOKESPECIAL(imi);
	    }
	}

	// Tranform a non-logged method selector into a logged method selector.  
	// The logged method selector is added to the constant pool and its index is returned.
	private int parSpecializeCall(ConstantMethodref ref) {
	    Selector.Method sel = ref.asSelector();
	    if (sel.getName().endsWith(_transSuffix)) {		
	        if (ref instanceof ConstantResolvedInterfaceMethodref)  // return existing CP entry
		    return cfe.getConstantsEditor().addInterfaceMethodref(ref);
	        else return cfe.getConstantsEditor().addMethodref(ref);
	    }	
	    int idx = getTransNameAsUTF8Index(sel);
	    Selector.Method newSel = Selector.Method.make(UnboundSelector.Method.make(idx, sel.getDescriptor()), sel.getDefiningClass());
	    if (ref instanceof ConstantResolvedInterfaceMethodref)
	        return cfe.getConstantsEditor().addInterfaceMethodref(newSel);
	    else return cfe.getConstantsEditor().addMethodref(newSel);
	}

	int getTransNameAsUTF8Index(Selector sel) {
	    String newName = sel.getName() + _transSuffix;
	    UnicodeBuffer newNameBuf = UnicodeBuffer.factory().wrap(newName);
	    int idx = UTF8Store._.findUtf8(newNameBuf);
	    if (idx == -1) throw new Error("can't find " + newName + " for " + sel + " in " + curBp_);
	    return idx;
	}

	private boolean verboten(ConstantResolvedMethodref smi) {
	    int include = ClassMethodPair.contains(noParMethods, smi.asSelector());
	    if (include == ClassMethodPair.INCLUDED) {
		return true;
	    }
	    else return false;
	}

	/** @return true if selector represents a native method that can be 
	 * rewritten to INVOKENATIVE and cannot be called by a PAR
	 */
	private boolean isNativeCall(ConstantResolvedMethodref crm, Instruction i,
				     S3Blueprint bp) {
	
	    return getNCG().getSpecialSequence(crm.getMethod()) != null;
	}

	private boolean isReplacedWithBytecode(Selector.Method sel, Blueprint bp) {	    
	    try { 
	        return PragmaTransformCallsiteIR.descendantDeclaredBy(sel, bp) != null;
	    } catch (PragmaException.UnregisteredPragmaException upe) { return false; }
	}

    }

    
    /**
     * Adds the logging code.  See documentation on TransactionalizeVisitor for information
     * about why they are separate.
     *
     */
    public static class LoggingVisitor extends PARifyingRewriteVisitor {

	public LoggingVisitor(S3Transaction t, IRewriter ir, S3Blueprint bp,
			      CodeFragmentEditor cfe_) {
	    super(t, ir, bp, cfe_);
	}

	public void visit(Instruction.PUTFIELD_QUICK i) { 
	    visitPUTFIELD(i, false, i.getOffset(super.getInstructionBuffer()));
	}

	public void visit(Instruction.PUTFIELD2_QUICK i) { 
	    visitPUTFIELD(i, true, i.getOffset(super.getInstructionBuffer()));
	}
	
	public void visit(Instruction.PUTFIELD i) { 
	    try { 
		ConstantResolvedInstanceFieldref field = cp.resolveInstanceField(i.getCPIndex(buf));
	        visitPUTFIELD(i, field.getField().getType().isWidePrimitive(), field.getOffset());
	    } catch (LinkageException e) {IRewriter.warn(i, e.getMessage());}
	}

	private void visitPUTFIELD(Instruction i, boolean wide, int offset) {
	    if (!this.parSpecializeInstruction()) return;		
	    WRITE_COUNT++;
	    Cursor c = cfe.getCursorBeforeMarker(getPC());
	    if (!wide) { // tgt val  
	        c.addSimpleInstruction(DUP2);  // tgt val ==> tgt val tgt val
	        c.addSimpleInstruction(POP);   // tgt val tgt val ==> tgt val tgt
	    } else {  // tgt  valval
	        c.addSimpleInstruction(DUP2_X1);  // valval tgt valval
	        c.addSimpleInstruction(POP2);   // valval tgt
	        c.addSimpleInstruction(DUP_X2);  // tgt valval tgt
	        // tgt valval tgt 
	    }
	    addLogCall(c, offset, wide);
	}

	public void visit(Instruction.INVOKE_SYSTEM i) {
	    if (!this.parSpecializeInstruction()) return;		
	    WRITE_COUNT++;

	    int opType = i.getMethodIndex(super.getInstructionBuffer());
	    Cursor cur = cfe.getCursorBeforeMarker(getPC());
	    if (opType == InvokeSystemArguments.DEREFERENCE) {
		switch (i.getOpType(super.getInstructionBuffer())) {
		case DereferenceOps.setByte:
		case DereferenceOps.setShort:
		    // cur.addINVOKESYSTEM(InvokeSystemArguments.DEREFERENCE, DereferenceOps.getByte);
		    cur.addSimpleInstruction(DUP2);
		    cur.addSimpleInstruction(POP);
		    addLogCall(cur, 0, false);
		    break;
		case DereferenceOps.setBlock:
	    	    cur.addResolvedRefLoadConstant(rtExportsOop());
	    	    cur.addLdc(new RepositoryString("setBlock not supported in PAR"));
	    	    addRTMethodCall(cur, nativeExceptionSelector);
		    break;
		}
	    }
	}

	public void visit(Instruction.PUTSTATIC i) { 
	    if (!parSpecializeInstruction()) return;
	    WRITE_COUNT++;
	    try {
	        ConstantResolvedStaticFieldref field = getConstants().resolveStaticField(i.getCPIndex(buf));
	        boolean wide = field.getField().getType().isWidePrimitive();
	        Cursor c = cfe.getCursorBeforeMarker(getPC());
	        c.addLOAD_SHST_FIELD(i.getCPIndex(buf));  //  v? ==>  v? tgt
	        addLogCall(c,field.getOffset(),wide);
	    } catch (LinkageException e) {IRewriter.warn(i, e.getMessage());}
	}

	public void visit(Instruction.ArrayStore i) {
	    if (!this.parSpecializeInstruction()) return;
	    
	    WRITE_COUNT++;
	    
	    Cursor c = cfe.getCursorBeforeMarker(getPC());
	    UnboundSelector.Method meth;
	    if (!( i instanceof Instruction.DASTORE) &&
	    	!(i instanceof Instruction.LASTORE)) {   // t i v
	        c.addSimpleInstruction(DUP_X2); // v t i v
	        c.addSimpleInstruction(POP);    // v t i
	        c.addSimpleInstruction(DUP2_X1); // t i v t i
	        meth = JavaNames.PAR_LOG_ARR;
	    } else { // t i vv
	        c.addSimpleInstruction(DUP2_X2); // vv t i vv
	        c.addSimpleInstruction(POP2);    // vv t i
	        c.addSimpleInstruction(DUP2_X2); // t i vv t i
	        meth = JavaNames.PAR_LOG_ARRW;
	    }
	    c.addResolvedRefLoadConstant(rtExportsOop()); // t i v? t i r
	    c.addSimpleInstruction(DUP_X2); // t i v? r t i r
	    c.addSimpleInstruction(POP); // t i v? r t i
	    addRTMethodCall(c, meth);
	}

	private void addLogCall(Cursor c, int offset, boolean wide) {
	    c.addResolvedRefLoadConstant(rtExportsOop());  // ==> -- tgt RTE
	    c.addSimpleInstruction(SWAP); // -- RTE tgt
	    c.addLoadConstant(offset); // -- RTE tgt off
	    addRTMethodCall(c, (!wide)? JavaNames.PAR_LOG : JavaNames.PAR_LOGW);
	}

	public void visit(Instruction.ArrayLoad i) {doRead();}

	public void visit(Instruction.GETFIELD i)  {doRead();}

	public void visit(Instruction.GETSTATIC i) {doRead();}

	private void doRead() {
	    if(!Transaction.the().gatherStatistics())return;
	    if (!parSpecializeInstruction()) return;
	    READ_COUNT++;
	    Cursor c = cfe.getCursorBeforeMarker(getPC());
	    c.addResolvedRefLoadConstant(rtExportsOop()); 
	    addRTMethodCall(c, JavaNames.PAR_READ);  
	}

    }

    static UnboundSelector.Method nativeExceptionSelector = 
	(UnboundSelector.Method) RepositoryUtils.makeUnboundSelector("par_throwNativeCallException:(Ljava/lang/String;)V");


    static class ClassMethodPair {
	static final String ALL = "*";
	static final String NOT = "!";
	static ClassMethodPair anInstance = new ClassMethodPair(null, null, false);
	Compound _class; 
	String _method;
	boolean _included;

	public int hashCode() {
	    return _method.hashCode() + _class.hashCode();
	}

	public boolean equals(Object o) {
	    if (o == null) return false;
	    if (o == this) return true;
	    if (!(o instanceof ClassMethodPair)) return false;
	    ClassMethodPair cmp = (ClassMethodPair) o;

	    return _class.equals(cmp._class) &&
		_method.equals(cmp._method) && 
		_included == cmp._included;
	}
	public ClassMethodPair(Compound clas, String method, boolean included) {
	    _class = clas; _method = method; _included = included;
	}
	public ClassMethodPair setPair(Compound clas, String method, boolean included) {
	    _class = clas; _method = method; _included = included;
	    return this;
	}
	
	public static final int INCLUDED = 1;
	public static final int EXCLUDED = 2;
	public static final int NOT_PRESENT = 3;
	
	public static int contains(HashSet hs, Selector s) {
	    Compound c = s.getDefiningClass();
	    // Compound c = s.getDefiningClass();
	    TypeName tn = c.getInstanceTypeName();
	    /*if (c.getShortNameIndex()  == RepositoryUtils.asUTF("LibraryImports") ||
	      c.getShortNameIndex() == RepositoryUtils.asUTF("RuntimeExports"))
	      c = ovm_core_execution_RuntimeExports;
	    */
	    if (tn.asScalar().getShortNameIndex() == RepositoryUtils.asUTF("LibraryImports") || 
		tn.asScalar().getShortNameIndex() == RepositoryUtils.asUTF("RuntimeExports")) {
		c = ovm_core_execution_RuntimeExports;
	    }
	    
	    boolean included = hs.contains(ClassMethodPair.anInstance.setPair(c, s.getName(), true)) ||
		hs.contains(ClassMethodPair.anInstance.setPair(c, ALL, true));
	    boolean excluded = hs.contains(ClassMethodPair.anInstance.setPair(c, s.getName(), false));
	    // order counts!
	    if (excluded) return EXCLUDED;
	    if (included) return INCLUDED;
	    return NOT_PRESENT;
	}
	public String toString() {
	    return "(" +_class.toString() + "," + _method + "," + _included + ")";
	}
    }

    // list of kernel methods to be allowed to be " T"
    static final HashSet kernelParMethods = new HashSet();

    // list of userland methods to be prevented from being " T"
    static final HashSet noParMethods = new HashSet();

    static final int javax_realtime = RepositoryUtils.asUTF("javax/realtime");
    public static final Scalar javax_realtime_MemoryArea = Scalar.make(javax_realtime, RepositoryUtils.asUTF("MemoryArea"));

    static final int org_ovmj_transact = RepositoryUtils.asUTF(Transaction.PACKAGE_NAME);       
    public static final Scalar org_ovmj_transact_Transaction = Scalar.make(org_ovmj_transact, RepositoryUtils.asUTF("Transaction"));
 
    static final int ovm_core_execution = RepositoryUtils.asUTF("ovm/core/execution");
    static final Scalar ovm_core_execution_RuntimeExports = Scalar.make(ovm_core_execution, RepositoryUtils.asUTF("RuntimeExports"));

    static final int ovm_services_threads = RepositoryUtils.asUTF("ovm/services/threads");
    static final Scalar ovm_services_threads_UserLevelThreadManager = 
	Scalar.make(ovm_services_threads, RepositoryUtils.asUTF("UserLevelThreadManager"));

    static final int ovm_util = RepositoryUtils.asUTF("ovm/util");
    static final TypeName.Gemeinsam ovm_util_Mem_PollingAware_Nat = 
	Scalar.make(ovm_util, RepositoryUtils.asUTF("Mem$PollingAware$Nat")).getGemeinsamTypeName();
    static final TypeName.Gemeinsam ovm_util_Mem_PollingAware_NatJava = 
	Scalar.make(ovm_util, RepositoryUtils.asUTF("Mem$PollingAware$NatJava")).getGemeinsamTypeName();


    static final int s3_services_transactions = RepositoryUtils.asUTF("s3/services/transactions");
    static final Scalar s3_services_transactions_Transaction = 
	Scalar.make(s3_services_transactions, RepositoryUtils.asUTF("Transaction"));

    static {
	String all = ClassMethodPair.ALL;

	noParMethods.add(new ClassMethodPair(JavaNames.java_lang_Throwable, all, true));
	noParMethods.add(new ClassMethodPair(JavaNames.java_lang_String, all, true));
	noParMethods.add(new ClassMethodPair(JavaNames.java_lang_Class, all, true));
	noParMethods.add(new ClassMethodPair(javax_realtime_MemoryArea, all, true));
	noParMethods.add(new ClassMethodPair(s3_services_transactions_Transaction, all, true));
	noParMethods.add(new ClassMethodPair(org_ovmj_transact_Transaction, all, true));
	noParMethods.add(new ClassMethodPair(JavaNames.java_lang_StringBuffer, all, true));

	noParMethods.add(new ClassMethodPair(ovm_services_threads_UserLevelThreadManager, "setReschedulingEnabled", true));
	noParMethods.add(new ClassMethodPair(javax_realtime_MemoryArea, "newInstance", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "PARenabled", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "start", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "commit", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "undo", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "retry", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "logSize", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "par_log_arr", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "par_log_arrw", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "par_log", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "par_logw", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "par_read", true));
	noParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "par_throwNativeCallException", true));

	kernelParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "newInstance", true));
	kernelParMethods.add(new ClassMethodPair(ovm_core_execution_RuntimeExports, "allocateObject", true));

    }
    /**
     * @return true if the method is declared PragmaPARSafe (and is therefore safe to call
     * from within a PAR), or if there is a replacement Java version.
     */
    private static boolean nativePAR(ConstantResolvedMethodref crm, S3Blueprint bp) {
	try {
	    return PragmaPARSafe.descendantDeclaredBy(crm.asSelector(), bp) != null;
	} catch (Exception e) {
	    ovm.core.execution.Native.print_string("undeclared pragma " + e);
	    return false;
	}
    }

    public boolean kernelPAR(ConstantResolvedMethodref crm) {
	Selector.Method mi = crm.asSelector();
	int include = ClassMethodPair.contains(kernelParMethods, mi);
	if (include == ClassMethodPair.INCLUDED) {
	    return true;
	} 

	return (mi.getDefiningClass().getShortNameIndex() != RepositoryUtils.asUTF("RuntimeExports")); 
	// I am sure the following line is here for a reason, 
	// but I can't remember what, and there is a bug 
	// associated with it.
	//&& (!descendantDeclaredBy(mi)); 
    }
   

    static s3.core.domain.S3Domain executiveDomain = 
	(s3.core.domain.S3Domain) ovm.core.domain.DomainDirectory.getExecutiveDomain();
    static Type _throwableType;

    static {
	TypeName.Scalar exc = TypeName.Scalar.make(RepositoryUtils.asUTF("java/lang"),
						   getTypeName("java.lang.Throwable").getShortNameIndex());
	try {
	    _throwableType = executiveDomain.getSystemTypeContext().typeFor(exc);
	} catch (LinkageException le) {
	    ovm.core.OVMBase.d("WARNING: no Type for ED 2: " + le);
	}
    }

    protected final static boolean descendantDeclaredBy(Selector.Method sel) {

	Blueprint bp;
	try {
	    bp = executiveDomain.blueprintFor(sel.getDefiningClass(), 
					      executiveDomain.getSystemTypeContext());
	} catch (Exception le ) {
	    // ovm.core.OVMBase.d("WARNING: no Type for ED 1: " + le);
	    return false;
	}

	Type unrefined = getType(sel, bp);
	Type.Context ctx = unrefined.getContext();

	if (!ctx.getDomain().isExecutive()) {
	    return false;
	}

	boolean retval = unrefined.isSubtypeOf(_throwableType);

	return retval;
    }

    protected final static TypeName.Scalar getTypeName(String className) {
   
	// similar logic in Driver.makeTypeNameFromClass depends on JDK2OVM, which
	// contains a dependency on java.lang.reflect.Field that makes phase 1 blow
	// up. Doing the hard way:

	String packageName;
	int split = className.lastIndexOf( '.');
	if ( split == -1 )
	    packageName = "";
	else {
	    packageName =
		className.substring( 0, split).replace( '.',
							'/');
	    //FIXME should be:      S3TypeName.PACKAGE_SEPARATOR);
	    // removed while trying to extricate ovm.* from s3.*
	    className = className.substring( split + 1);
	}
	return RepositoryUtils.makeTypeName( packageName, className);
    }

    private static Type getType( Selector.Method sel, Blueprint bp) {
	Type t = bp.getType();
	return t;
    }

    ////////////////////////////////////////////////
    // Code to generate PAR scope reachability graph
    ////////////////////////////////////////////////
    private static boolean GENERATE_PAR_GRAPH = false;
    private static HashMap par_graph = new HashMap(3000);
    private static ArrayList par_roots = new ArrayList();
    private static ArrayList psudo_roots = new ArrayList();
    private static class MthdInfo {
	public int chldCnt = 0;
	public int total = 0;
	public int level = -1;
	public ArrayList children = new ArrayList();
    }
    private static String[] xxx = {"", "  ", "    ", "      ", "        ", "          ", "            "};
    private static String getIndent(int n) {
	if(n <= 6) return xxx[n];
	String s = "            ";
	while( (n -= 6) > 0) s += "~";
	return s;
    }
    private static int assign_level(Selector.Method m, int l) {
	MthdInfo tmp_ = (MthdInfo) par_graph.get(m);
	if(tmp_ == null) {
	    System.out.println(0+"\t"+l+" "+getIndent(l)+m+" NOT FOUND in par_graph; Abstract?");
	    return 0;
	}
	if(tmp_.level >= 0 ) return tmp_.total; //already visited
	tmp_.level = l;
	Object[] chs_ = tmp_.children.toArray();
	for(int i=0; i < chs_.length; i++) 
	    tmp_.total += assign_level((Selector.Method)chs_[i], l+1); //stack overflow?

	System.out.print(tmp_.total+"\t"+tmp_.level+" "+getIndent(tmp_.level));
	System.out.println(m);

	return tmp_.total;
    }
    private static void compute_psudo_roots() {
	//mark those that are reachable from some methods
	Object[] infos_ = par_graph.values().toArray();
	for(int i=0; i<infos_.length; i++) {
	    MthdInfo mi_ = (MthdInfo) infos_[i];
	    if(mi_.level < 0) { //not visited 
		for(int j=0; j < mi_.children.size(); j++) {
		    Object chld_ = mi_.children.get(j);
		    MthdInfo ch_mi = (MthdInfo) par_graph.get(chld_);
		    if(ch_mi == null) {
			//System.err.println("Weird: '"+chld_+"' not in par_graph. Abstract? Continue ...");
			continue;
		    }
		    if(ch_mi.level < 0) //not visitied 
			ch_mi.level = -2;
		}
	    }
	}
			
	//after the above marking pass, method with level = -1 are psudo
	//roots
	for( Iterator it = par_graph.keySet().iterator(); it.hasNext(); ) {
	    Object tmp_ = it.next();
	    MthdInfo mi_ = (MthdInfo) par_graph.get(tmp_);
	    if(mi_.level == -1) 
		psudo_roots.add(tmp_);
	}
    }
    public static void dump_par_graph() {
	if( ! GENERATE_PAR_GRAPH) {
	    System.out.println("Skipping ...");
	    return;
	}
	System.out.print("dump_par_graph: graph size = ");
	System.out.println(par_graph.size());
	System.out.print("root size = ");
	System.out.println(par_roots.size());
			
	int sum_ = 0;
	Object[] roots_ = par_roots.toArray();
	for(int i=0; i<roots_.length; i++) {
	    int t_ = assign_level((Selector.Method) roots_[i], 0);
	    System.out.println();
	    System.out.println("Root #: "+i+" reaching "+t_+" method(s)");
	    System.out.println();
	    sum_ += t_;
	}

	System.out.println();
	System.out.println("Total # of functions reachable from roots: "+sum_);
	System.out.println();
					
	sum_ = 0;
	compute_psudo_roots();
			
	System.out.print("psudo root size = ");
	System.out.println(psudo_roots.size());

	Object[] p_roots_ = psudo_roots.toArray();
	for(int i=0; i<p_roots_.length; i++) {
	    int t_ = assign_level((Selector.Method) p_roots_[i], 0);
	    System.out.println();
	    System.out.println("Psudo Root #: "+i+" reaching "+t_+" method(s)");
	    System.out.println();
	    sum_ += t_;
	}
			
	System.out.println();
	System.out.println("Total # of functions reachable from psudo roots: "+sum_);
	System.out.println();
    }

    public void endEditing() {
	if(GENERATE_PAR_GRAPH) {
	    if(((MthdInfo)par_graph.get(getSelector())).total == 0) {
		if( ! getSelector().getName().endsWith(_transSuffix))
		    par_graph.remove(getSelector());
	    } else {
		if( ! getSelector().getName().endsWith(_transSuffix))
		    par_roots.add(getSelector());
	    }
					
	}				
	super.endEditing();
    }
} 
