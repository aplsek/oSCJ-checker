package s3.services.transactions;

import ovm.core.domain.Domain;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.ReflectiveConstructor;
import ovm.core.domain.WildcardException;
import ovm.core.repository.Descriptor;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.threads.OVMThread;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.bytecode.SpecificationIR.ValueSource;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import ovm.services.bytecode.editor.LinearPassController;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.BitSet;
import ovm.util.UnicodeBuffer;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import ovm.core.domain.ConstantPool;
import s3.core.domain.S3Type;
import s3.core.execution.S3CoreServicesAccess;
import s3.services.bytecode.ovmify.IRewriter;
import s3.services.bytecode.ovmify.NativeCallGenerator;
import s3.services.j2c.J2cValue.InvocationExp;
import s3.services.j2c.J2cValue.MethodReference;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaTransformCallsiteIR.BCnothing;
import s3.util.PragmaTransformCallsiteIR.BCdead;


public class S3Transaction extends Transaction {
 
    // The suffix used to annotate transactional methods.  It should contain at least one character not found 
    // in java identifiers.
    private static Oop ABORTED_EXCEPTION;    
    private static final int LOGSIZE = 100*1000;
    private static boolean commitOnOverflow;
    private static int logOffset;
    private static final int bound = (2 * LOGSIZE) -2;  
    private static  final int[] log = new int[2*LOGSIZE];   
    
    
    S3CoreServicesAccess _csa;
    static protected UserLevelThreadManager threadMan;

    public void initialize() {
	threadMan = 
	    (UserLevelThreadManager)((ThreadServicesFactory)ThreadServiceConfigurator.config.
		getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
    }

    private static ReflectiveConstructor constructor;
    private static ReflectiveConstructor nativeConstructor;
    private  VM_Address logArrayStart;
    private static boolean overflow;    
    private static boolean rescheduling;
    private EDAbortedException parEDA;

    public static int inPAR;

    public boolean inTransaction() throws PragmaInline, 
					  PragmaNoPollcheck, PragmaNoBarriers {
	return inPAR == 0; 
    }    
  
    public void par_read() throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers {
	S3TransactionWithStats.recordRead();	
    }

     void doLog(int field, int value) throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers{	
	if (logOffset < bound) { 
	    VM_Address start = logArrayStart; 
	    start = start.add(logOffset*4);
	    logOffset += 2;
	    start.setInt(value);
	    start = start.add(4);
	    start.setInt(field);	
	} else  overflow();
    }
  
    private void overflow()  throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers {	
	overflow = true;
	rescheduling = threadMan.setReschedulingEnabled(false);     	
    }
    
    public void par_log(VM_Address object, int offset) throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers {
	if (object == null)return;
	VM_Address field = object.add(offset);
	int value = field.getInt();
	doLog(field.asInt(),value);
    }
    public void par_logw(VM_Address object, int offset) throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers {
	if (object == null)return;
	VM_Address field = object.add(offset);
	int value = field.getInt();
	doLog(field.asInt(),value);
	field = object.add(offset+4);			
	value = field.getInt();
	doLog(field.asInt(),value);
    }   
    public void par_log_arr(Oop arr, int idx)  throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers {
	if (arr == null)return;
	int off = arr.getBlueprint().asArray().byteOffset(idx);	
	VM_Address loc = VM_Address.fromObject(arr).add(off);
	int value = loc.getInt();
	doLog(loc.asInt(), value);
    }     
    public void par_log_arrw(Oop arr, int idx)  throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers {
	if (arr == null)return;
	int off = arr.getBlueprint().asArray().byteOffset(idx);	
	VM_Address loc = VM_Address.fromObject(arr).add(off);
	int value = loc.getInt();
	doLog(loc.asInt(), value);
	loc = VM_Address.fromObject(arr).add(off+4);
	value = loc.getInt();
	doLog(loc.asInt(), value);
    }
      
    public void undo() throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers  {
	int off = logOffset;
	logOffset = 0;
	while (off > 0) {
	    int adr = log[--off];
	    int value = log[--off];
	    VM_Address tgt = VM_Address.fromInt(adr);
	    tgt.setInt(value);
	}
    }
    public void start(int size, boolean commit, S3CoreServicesAccess csa) throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers  {
	commitOnOverflow = commit;
	start(csa);
    }
    public void start(S3CoreServicesAccess csa) throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers  {
	_csa = csa;
	inPAR++;
    }
    public int logSize() throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers  {
	return LOGSIZE;
    }
    
    public void commit() throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers  {
	inPAR--;
	if (inPAR == 0) {
	    logOffset = 0;
	    //	threadInTrans = null;
	    if(overflow) overflowExit();
	}
    }
    
    private void overflowExit()  throws  PragmaNoPollcheck,PragmaNoBarriers{	
	if (overflow) {
	    overflow = false;
	    threadMan.setReschedulingEnabled(rescheduling);
	}
    }
    
    public void postRunThreadHook(boolean aborting) { 
	
	if (aborting) retry(); 
    }
    
    public void retry() throws PragmaInline, PragmaNoPollcheck, PragmaNoBarriers { 
	throw parEDA;//forceCast(ABORTED_EXCEPTION);
    }
    
    public EDAbortedException getEDA() throws EDAbortedException {
	return parEDA;
    }
    
    private Error forceCast(Oop X) throws BCnothing { return (Error) X; }
    
    public boolean preRunThreadHook(OVMThread currentThread, OVMThread nextThread) throws PragmaNoPollcheck {
	if (inPAR != 0) {
	    undo();
	    inPAR = 0;
	    return true;
	}   
	overflow = false;
	return false;
    }
 
    /**
     * We replace a reflective method invocation with a call to its transactional equivalent.
     * Class initializers are currently not rewritten ... they could ... should be PragmaAtomic then.
     */
     public Method selectReflectiveMethod(Method method) {
	 if (//!method.getDeclaringType().getDomain().isExecutive() && 
        	!method.isClassInit() 
        	&& inTransaction()
       	&& threadMan.isReschedulingEnabled()) // why ?? FIXME
        {
            Selector.Method sel = method.getSelector();
            UnboundSelector.Method usel_t = getTSel(sel);
            if (usel_t==null) return method;
            method = method.getDeclaringType().asScalar().getMethod(usel_t);
        }
        return method;
    }
    
    public boolean isTransMethod(Method m) {return m.getSelector().getName().endsWith(getTRANS_SUFFIX());  }

    public void setExceptionConstructors(Domain d) {
	constructor = new ReflectiveConstructor(d, Transaction.ABORTED_EXCEPTION, new TypeName[]{});
	nativeConstructor = new ReflectiveConstructor(d, Transaction.NATIVE_EXCEPTION, new TypeName[]{});
    }

    public void throwNativeCallException(String s) {
	Oop nativeException = nativeConstructor.make();
	if (Transaction.DEBUG) {
	    ovm.core.execution.Native.print_string(s);
	    (new ovm.util.OVMError()).printStackTrace();
	}
	throw new WildcardException(nativeException);
    }
    
    public void boot() {	
	Oop logOop = VM_Address.fromObject(log).asOop();
        logArrayStart = logOop.getBlueprint().asArray().addressOfElement(logOop, 0);
        ABORTED_EXCEPTION = constructor.make();
        VM_Area currentArea = MemoryManager.the().getCurrentArea();
        MemoryPolicy.the().enterInternedStringArea(ovm.core.domain.DomainDirectory.getExecutiveDomain());
        parEDA = new EDAbortedException();
        MemoryManager.the().setCurrentArea(currentArea);
    }
    
    public Oop getUDException() { return S3Transaction.ABORTED_EXCEPTION; }
    
    public UnboundSelector.Method translateTransactionalSelector(UnboundSelector.Method usel) {
	String base = usel.getName();                                            	
	if (base.endsWith(getTRANS_SUFFIX())) {                  
	    base = base.substring(0, base.length() - 2);                         
	    usel = UnboundSelector.Method.make(RepositoryUtils.asUTF(base), usel.getDescriptor());            
	}
	return usel;
    }

    /** @deprecated Use {@link Transaction#TRANS_SUFFIX} */
    String getTRANS_SUFFIX()    { return TRANS_SUFFIX;}        
    public boolean transactionalMode() { return true; }
        
    /** This method duplicates the method tables and create transactional copies of
     *  every method. We hopefully prune all of the unreachable ones later.	 */
    public RepositoryMember.Method[] initMethods(S3Type type, RepositoryMember.Method[] repoMethods, RepositoryMember.Method[] cachedRepoMethods) {
	// if (type.getDomain().isExecutive())  return repoMethods;
	if (cachedRepoMethods == null) {
	    cachedRepoMethods = new RepositoryMember.Method[2*repoMethods.length];
	    for (int i = 0; i < repoMethods.length; i++) {
		RepositoryMember.Method orig = repoMethods[i];
		RepositoryMember.Method.Builder builder = new RepositoryMember.Method.Builder(orig);
		String newName = (orig.getName() + getTRANS_SUFFIX());
		UnicodeBuffer newNameBuffer= UnicodeBuffer.factory().wrap(newName);
		builder.setName(UTF8Store._.installUtf8(newNameBuffer));
		cachedRepoMethods[2*i]     = orig;
		cachedRepoMethods[2*i + 1] = builder.build();
	    } 
	}
	return cachedRepoMethods;
    }   
    /**
     * Design choice: In Ovm all reflective methods must be predeclared. Should calls to
     * reflective transactional methods be declared too?
     * Answering no implies that all of the DARPA benchmark is compiled in transactional
     * mode (i.e. 2x code bloat). This because the main method is invoked reflectively!
     * 
     * For now, we will assume that if a method is to be invoked reflectively it should
     * be explictly declared...
     * 
     * To revisit.
     *
     */
    public void setReflectiveCalls(BitSet[] reflectiveCalls, Domain d, Method[] meth, int i) {
	// if (d.isExecutive()) return;
	Selector.Method sel = meth[i].getSelector();
	UnboundSelector.Method usel_t  = getTSel(sel);
	if(usel_t==null) return; 
	Method meth_t = meth[i].getDeclaringType().asScalar().getMethod(usel_t);
	reflectiveCalls[meth_t.getCID()].set(meth_t.getUID());
    }
    
    private UnboundSelector.Method getTSel(Selector.Method sel) {	
	if (sel.toString().indexOf(" T")>0)return null; 
	Descriptor.Method desc = sel.getDescriptor();
	UnicodeBuffer name = UTF8Store._.getUtf8(sel.getNameIndex());
	UnicodeBuffer name_t  = UnicodeBuffer.factory().wrap(name + getTRANS_SUFFIX());
	int idx = UTF8Store._.findUtf8(name_t);
	if (idx == -1) throw new Error("can't find " + name_t + " for " + sel);
	UnboundSelector.Method usel_t  = UnboundSelector.Method.make(idx, desc);
	return usel_t;
    }
    
    
    public  boolean rewriteTransactional(int x, IRewriter ir, Method method, 
					 S3ByteCode.Builder builder, S3Blueprint curBp,
					 int phase) {
	 
	S3ByteCode code = builder.build();
	ConstantPool rcpb = code.getConstantPool();

	CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);
	Selector.Method sel = method.getSelector();
	if (ir.isMagic(method) ) return false;
	if (method.getMode().isNative()) return false; // avoid rewriting call to LibraryGlue...

	if ( isAtomic(method) && 
	    ! isTransMethod(method) && x == 0) {
	    Optimizer.SimpleSequenceVisitor ssv = 	new Optimizer.SimpleSequenceVisitor();
	    ssv.run(method);
	    if ( ssv.clean) { 
		//System.out.println("Method " + method + " is clean -- skipping PARification");
		ATOMIC_NONLOGGED_METHOD_COUNT++;
		return false;
	    }
	    PARMethodRewriter vis = new  PARMethodRewriter(ir, curBp,  cfe, rcpb);
	    cfe.runVisitor(new LinearPassController(vis));
	    cfe.commit(builder, builder.getMaxStack(), builder.getMaxLocals());	
	    x++; 
	    return true;
	}
	if (isAtomic(method)) {
	    if (isTransMethod(method)) // note that a method can be counted twice if called from an
		// atomic and non-atomic context
		  NESTED_ATOMIC_METHOD_COUNT++;
	    else
		 ATOMIC_METHOD_COUNT++;
	}
	PARifyingRewriteVisitor vis;
	if (phase == 1) {
	    vis = new PARifyingRewriteVisitor.TransactionalizeVisitor(this, ir, curBp, cfe);
	} else {
	    vis = new PARifyingRewriteVisitor.LoggingVisitor(this, ir, curBp, cfe);    
	}
	
	cfe.runVisitor(new LinearPassController(vis));
	cfe.commit(builder, builder.getMaxStack(), builder.getMaxLocals());
	
	if (!vis.METHOD_IGNORED) {
	    READ_COUNTS[IDX] = vis.READ_COUNT;
	    WRITE_COUNTS[IDX] = vis.WRITE_COUNT;
	    IDX++;
	}
	return false;
    }
    
    /*-- this code is meant to replace array ops with uncheck arrray ops... right now on the backburner.
    public static void rewriteArrays(IRewriter rewriter, S3Method method, Builder builder, S3Blueprint bp) {
	S3ByteCode code = builder.build();
	S3ConstantsBuilder rcpb = (S3ConstantsBuilder) code.getConstantPool();
	CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);
	ArrayRewriteVisitor vis = new ArrayRewriteVisitor(rewriter, bp);
	cfe.runVisitor(new LinearPassController(vis));
	cfe.commit(builder, builder.getMaxStack(), builder.getMaxLocals());
	builder.setConstantPool(rcpb);
    } 
    static class ArrayRewriteVisitor extends IRewriter.RewriteVisitor {
	ArrayRewriteVisitor(IRewriter r, S3Blueprint bp) { r.super(bp, false, false); }	 
	public void visit(ArrayStore i) { cfe.replaceInstruction().addSimpleInstruction(UNCHECKED_AASTORE); }
    }
    */
    static public int ATOMIC_METHOD_COUNT, NESTED_ATOMIC_METHOD_COUNT;
    static public int ATOMIC_BLOCK_COUNT, NESTED_ATOMIC_BLOCK_COUNT;
    static public int ATOMIC_NONLOGGED_METHOD_COUNT;
    static public int IDX;
    static public int[] READ_COUNTS = new int[50000];
    static public int[] WRITE_COUNTS = new int[50000];
       
    public boolean ignoreLogCall(ValueSource _exp) {
	if (_exp instanceof InvocationExp) {
	    InvocationExp exp = (InvocationExp) _exp;
	    if (exp.target instanceof MethodReference) {
		Selector sel = ((MethodReference) exp.target).method.getSelector();
		UnboundSelector.Method us  = sel.asMethod().getUnboundSelector();
		if (us == JavaNames.PAR_LOG || us == JavaNames.PAR_LOGW ||
		    us == JavaNames.PAR_LOG_ARR || us == JavaNames.PAR_LOG_ARRW) 
		    return true;
	    }
	}
	return false;

    }

    public boolean PARenabled() { return true; }

}
