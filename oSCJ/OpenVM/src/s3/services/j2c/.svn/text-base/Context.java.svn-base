package s3.services.j2c;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.SpecificationIR.ValueSource;
import ovm.util.BitSet;
import ovm.util.HTObject2int;
import ovm.util.HashMap;
import ovm.util.Iterator;
import ovm.util.UnicodeBuffer;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Method;
import s3.services.bootimage.Analysis;
import s3.services.j2c.J2cValue.*;
import s3.services.transactions.Transaction;

/**
 * Per-domain (and some global) environment.  This class also does
 * some global memoization of Values and ValueSources.  Maybe that
 * code should be moved to J2cValue.java?
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 */
public class Context {
    J2cImageCompiler compiler;
    S3Domain executiveDomain;
    S3Domain domain;
    Analysis anal;

    boolean gcSupport;
    boolean safePointMacros;
    boolean noInlineWithSafePoints;
    boolean catchPointsUsed;
    boolean frameLists;
    boolean ptrStack;
    boolean initGCFrame;
    
    /** always set to frameLists||ptrStack */
    boolean gcFrames;

    boolean noCppExceptions;
    boolean cExceptions;
    boolean cExceptionsCount;
    boolean doBarrierProf;
    boolean innerBarrierProf;
    boolean counterExitPollcheck;

    boolean dontRenameLocals = false;

    boolean clinitIsLive;
    boolean finalizeIsLive;
    int clinitUtf;
    BitSet[] clinitNeeded;
    BitSet[] clinitKnown;

    BitSet[] hasSafePoints;
    
    /**
     * Is it safe to optimize is_subtype_of(?, array-type) to false?
     * We optimize is_subtype_of to false if (based on analysis and
     * reflection hints) the from type and to type don't share any
     * concrete subtypes.  If reflection hints are wrong for a class type,
     * we can't actually allocate an instance.  But, if reflection
     * hints are wrong for an array type, we can.  The result of
     * Array.newInstance is immediately cast to the actual type.  But,
     * RTA has told us that the actual type is never allocated, so the
     * cast fails.
     */
    static final boolean optimizeBadCastToArray = false;
    
    // types in our domain
    S3Blueprint intBP;
    S3Blueprint longBP;
    S3Blueprint booleanBP;
    S3Blueprint objectBP;
    S3Blueprint stringBP;
    S3Blueprint throwableBP;
    S3Blueprint RuntimeExceptionBP;
    S3Blueprint ErrorBP;
    S3Blueprint AbortedExceptionBP;
    S3Blueprint EDAbortedExceptionBP;
    S3Blueprint NullPointerExceptionBP;
    S3Blueprint ClassBP;
    S3Blueprint voidBP;
    S3Blueprint interruptHandlerBP;

    // types and values in the executive domain
    S3Blueprint intArrayBP;
    S3Blueprint bpbp;
    S3Blueprint RepositoryStringBP;

    S3Blueprint CSAbp;
    J2cValue CSAvalue;
    S3Method finalize;
    S3Method pollingEventHook;
    S3Method translateThrowable;
    S3Method monitorEnter;
    S3Method monitorExit;
    S3Method processThrowable;
    S3Method generateThrowable;
    S3Method makeThrowable;
    S3Method checkingTranslatingReadBarrier;
    S3Method acmpeqBarrier;    
    S3Method acmpneBarrier;    
    S3Blueprint OpaqueBP;
    
    // Other blueprints are only filled in within the executive domain
    S3Blueprint OopBP;
    S3Blueprint VM_AddressBP;
    S3Blueprint VM_WordBP;
    S3Blueprint CodeBP;

    S3Blueprint monitorBP;

    S3Blueprint ContextBP;

    J2cValue j2cInvoke;

    int headerSize;

    private static HashMap domainContexts = new HashMap();

    public static Iterator getIterator() {
	return domainContexts.values().iterator();
    }

    public static Context findContext(final J2cImageCompiler cmp,
				      final Domain domain) {
	Context ret = (Context) domainContexts.get(domain);
	if (ret == null) {
	    // Note that the constructor is responsible for adding a
	    // context to the map.  This allows recursive calls to
	    // findContext from the constructor to work.
	    final Context[] _ret = new Context[1];
	    try {
		_ret[0] = new Context((S3Domain) domain, cmp);
	    } catch (LinkageException e) {
		domainContexts.remove(domain);
		throw e.unchecked();
	    }
	    return _ret[0];
	}
	else
	    return ret;
    }

    // May return null
    public static Context findContext(Domain domain) {
	return (Context) domainContexts.get(domain);
    }

    private Context(S3Domain d, J2cImageCompiler cmp) 
	throws LinkageException
    {
	domainContexts.put(d, this);
	this.domain = d;
	this.executiveDomain = (S3Domain) cmp.getExecutiveDomain();
	this.compiler = cmp;
	gcSupport = cmp.gcSupport;
	safePointMacros = cmp.safePointMacros;
	noInlineWithSafePoints = cmp.noInlineWithSafePoints;
	catchPointsUsed = cmp.catchPointsUsed;
	frameLists = cmp.frameLists;
	ptrStack = cmp.ptrStack;
	initGCFrame = cmp.initGCFrame;
	gcFrames = (frameLists || ptrStack);
	noCppExceptions = cmp.noCppExceptions;
	cExceptions = cmp.cExceptions;
	cExceptionsCount = cmp.cExceptionsCount;
	doBarrierProf = cmp.doBarrierProf;
	innerBarrierProf = cmp.innerBarrierProf;
	counterExitPollcheck = cmp.counterExitPollcheck;

	ObjectModel m = ObjectModel.getObjectModel();
	headerSize = m.headerSkipBytes();

	finalizeIsLive = clinitIsLive = d != executiveDomain;
	clinitUtf = getUTF("<clinit>");
	
	intBP = blueprintFor(domain.INT);
	longBP = blueprintFor(domain.LONG);
	booleanBP = blueprintFor(domain.BOOLEAN);
	voidBP = blueprintFor(domain.VOID);

	stringBP = (S3Blueprint)
	    domain.blueprintFor(JavaNames.java_lang_String,
				domain.getSystemTypeContext());

	objectBP = (S3Blueprint)
	    domain.blueprintFor(JavaNames.java_lang_Object,
				domain.getSystemTypeContext());

	throwableBP = (S3Blueprint)
	    domain.blueprintFor(JavaNames.java_lang_Throwable,
				domain.getSystemTypeContext());
	NullPointerExceptionBP = (S3Blueprint)
	    domain.blueprintFor(JavaNames.java_lang_NullPointerException,
				domain.getSystemTypeContext());
	RuntimeExceptionBP = (S3Blueprint)
	    domain.blueprintFor(JavaNames.java_lang_RuntimeException,
				domain.getSystemTypeContext());
				
        if (domain.isExecutive()) {
          interruptHandlerBP = null ;
        } else {          				
  	  interruptHandlerBP = (S3Blueprint)
	    domain.blueprintFor(JavaNames.ovm_hw_InterruptHandler,
				domain.getSystemTypeContext());
				
          OpaqueBP = (S3Blueprint) domain.blueprintFor
	    (JavaNames.org_ovmj_java_Opaque,
	     domain.getSystemTypeContext());
				
        }
												
	ErrorBP = (S3Blueprint)
	    domain.blueprintFor(JavaNames.java_lang_Error,
				domain.getSystemTypeContext());
	ClassBP = (S3Blueprint)
	    domain.blueprintFor(JavaNames.java_lang_Class,
				domain.getSystemTypeContext());
	if (domain != executiveDomain
	    && Transaction.the().transactionalMode()) {
	    AbortedExceptionBP = (S3Blueprint)
		domain.blueprintFor(Transaction.ABORTED_EXCEPTION,
				    domain.getSystemTypeContext());
	} 
	// PARBEGIN PAREND
	if (domain == executiveDomain 
		&& Transaction.the().transactionalMode()) {
	    this.EDAbortedExceptionBP = (S3Blueprint) 	    	
	    	executiveDomain.blueprintFor(Transaction.ED_ABORTED_EXCEPTION,
	    			    executiveDomain.getSystemTypeContext());
	}
	intArrayBP = (S3Blueprint) executiveDomain.blueprintFor
	    (TypeName.Array.make(TypeName.INT, 1),
	     executiveDomain.getSystemTypeContext());
	OopBP = (S3Blueprint) executiveDomain.blueprintFor
	    (RepositoryUtils.makeTypeName("ovm/core/domain","Oop"),
	     executiveDomain.getSystemTypeContext());

	RepositoryStringBP = (S3Blueprint) executiveDomain.blueprintFor
	    (RepositoryUtils.makeTypeName("ovm/core/repository",
					  "RepositoryString"),
	     executiveDomain.getSystemTypeContext());
	ContextBP = (S3Blueprint) executiveDomain.blueprintFor
	    (RepositoryUtils.makeTypeName("ovm/core/execution",
					  "Context"),
	     executiveDomain.getSystemTypeContext());

	if (executiveDomain == d) {
	    // blueprints needed for INVOKE_SYSTEM instruction
	    CodeBP = (S3Blueprint) domain.blueprintFor
		(RepositoryUtils.makeTypeName("ovm/core/domain", "Code"),
		 domain.getSystemTypeContext());
	    monitorBP = (S3Blueprint) domain.blueprintFor
		(RepositoryUtils.makeTypeName("ovm/services/monitors",
					      "Monitor"),
		 domain.getSystemTypeContext());
	}
	if (finalizeIsLive) 
	    finalize = lookupMethod(d.ROOT_BLUEPRINT, "finalize:()V");
	
	// I should really make the typename from the actual value
	// in the image header
	Object csaObject = d.getCoreServicesAccess();
	Oop csaOop = VM_Address.fromObject(csaObject).asOop();
	CSAvalue = new ConcreteScalar(null, csaOop);
	CSAbp = (S3Blueprint) csaOop.getBlueprint();

	pollingEventHook
	    = lookupMethod(CSAbp, "pollingEventHook:()V");
	translateThrowable
	    = lookupMethod(CSAbp, "translateThrowable:" +
			   "(Ljava/lang/Object;)Lovm/core/domain/Oop;");
	processThrowable
	    = lookupMethod(CSAbp, "processThrowable:" +
			   "(Lovm/core/domain/Oop;)Ljava/lang/Error;");
        makeThrowable
            = lookupMethod(CSAbp, "makeThrowable:" +
                            "(ILovm/core/domain/Oop;Lovm/core/domain/Oop;)Lovm/core/domain/Oop;");
	generateThrowable
	    = lookupMethod(CSAbp, "generateThrowable:(II)V");
	monitorEnter
	    = lookupMethod(CSAbp, "monitorEnter:(Lovm/core/domain/Oop;)V");
	monitorExit
	    = lookupMethod(CSAbp, "monitorExit:(Lovm/core/domain/Oop;)V");
        checkingTranslatingReadBarrier
            = lookupMethod(CSAbp, "checkingTranslatingReadBarrier:(Lovm/core/domain/Oop;I)Lovm/core/domain/Oop;");    
        acmpneBarrier
            = lookupMethod(CSAbp, "acmpneBarrier:(Lovm/core/domain/Oop;Lovm/core/domain/Oop;II)I");    
        acmpeqBarrier
            = lookupMethod(CSAbp, "acmpeqBarrier:(Lovm/core/domain/Oop;Lovm/core/domain/Oop;II)I");    
            

	Type.Context ctx = executiveDomain.getSystemTypeContext();
	TypeName.Scalar t = RepositoryUtils.makeTypeName("s3/core/domain",
							 "S3Blueprint");
	bpbp = (S3Blueprint) executiveDomain.blueprintFor(t, ctx);
	VM_AddressBP = (S3Blueprint) executiveDomain.blueprintFor
	    (JavaNames.ovm_core_services_memory_VM_Address, ctx);
	VM_WordBP = (S3Blueprint) executiveDomain.blueprintFor
	    (JavaNames.ovm_core_services_memory_VM_Word, ctx);

	if (domain != executiveDomain) {
	    clinitNeeded = new BitSet[DomainDirectory.maxContextID() + 1];
	    clinitKnown = new BitSet[DomainDirectory.maxContextID() + 1];
	    for (int i = 0; i < clinitNeeded.length; i++) {
		Type.Context tc = DomainDirectory.getContext(i);
		if (tc != null && tc.getDomain() == domain) {
		    clinitNeeded[i] = new BitSet(tc.getBlueprintCount());
		    clinitKnown[i] = new BitSet(tc.getBlueprintCount());
		}
	    }
	}
	hasSafePoints = new BitSet[DomainDirectory.maxContextID() + 1];
	for (int i = 0; i < hasSafePoints.length; i++) {
	    Type.Context tc = DomainDirectory.getContext(i);
	    if (tc != null && tc.getDomain() == domain) 
		hasSafePoints[i] = new BitSet(tc.getBlueprintCount());
	}

	j2cInvoke = J2cValue.makeSymbolicReference("j2cInvoke");
    }

    boolean shouldCompile(Blueprint bp) {
	return anal.shouldCompile(bp);
    }
    boolean shouldCompile(Method meth) {
	return anal.shouldCompile(meth);
    }
    void dontCompile(Method meth) {
	anal.dontCompile(meth);
	// validMethods[meth.getCID()].clear(meth.getUID());
    }
    void hasSafePoints(Method meth) {
	hasSafePoints[meth.getCID()].set(meth.getUID());
    }
    boolean hasSafePoints_p(Method meth) {
	return hasSafePoints[meth.getCID()].get(meth.getUID());
    }

    boolean needsInit(S3Blueprint bp) {
	BitSet cKnown = clinitKnown[bp.getCID()];
	BitSet cNeeded = clinitNeeded[bp.getCID()];
	int idx = bp.getUID();
	if (!cKnown.get(idx)) {
	    boolean needed = false;	    
	    if (bp.getParentBlueprint() != null) {
		needed = needsInit(bp.getParentBlueprint());
		Type.Interface[] ifs = bp.getType().getInterfaces();
		for (int i = 0; !needed && i < ifs.length; i++) {
		    S3Blueprint ibp = blueprintFor(ifs[i]);
		    needed = needsInit(ibp);
		}
	    }
	    // Even if none of our parents have <clinit>s, there still
	    // may be one locally.
	    if (!needed) {
		S3Method m
		    = findMethodObject((S3Blueprint) bp.getSharedState().getBlueprint(),
				       clinitUtf);
		if (m != null
		    && (m.getByteCode()).getBytes().length > 1)
		    needed = true;
	    }
	    if (needed)
		cNeeded.set(idx);
	    cKnown.set(idx);
	    //System.err.println(bp + (needed ? " needs" : " doesn't need")
	    //		       + "init");
	}
	return cNeeded.get(idx);
    }
	    
    boolean isVTableUsed(Blueprint bp) {
	return (anal == null
		? true
		: anal.isVTableUsed(bp));
    }
    boolean isIFTableUsed(Blueprint bp) {
	return (anal == null
		? true
		: anal.isIFTableUsed(bp));
    }

    S3Method lookupMethod(S3Blueprint bp, String mn) {
	UnicodeBuffer buf = UnicodeBuffer.factory().wrap(mn);
	UnboundSelector.Method usel
	    = ((UnboundSelector.Method) UnboundSelector.parse(buf));

	Method m = null;
	do {
	    TypeName.Scalar tn = bp.getName().asScalar();
	    Selector.Method sel
		= Selector.Method.make(usel, tn);
	    Type.Class t = (Type.Class) bp.getType();
	    m = t.getMethod(sel);
	    bp = bp.getParentBlueprint();
	} while (m == null);
	return (S3Method) m;
    }
	    
    J2cValue makeValue(ValueSource source, Type t, boolean promoteInt) {
	char tag = t.getUnrefinedName().getTypeTag();
	Domain d = t.getContext().getDomain();
	switch (tag) {
	case TypeCodes.ARRAY:
	    return new J2cArray(source, (S3Blueprint) d.blueprintFor(t));
	case TypeCodes.OBJECT:
	    return new J2cReference(source, (S3Blueprint) d.blueprintFor(t));
	case TypeCodes.LONG:
	    return new J2cLong(source, null, tag);
	case TypeCodes.FLOAT:
	    return new J2cFloat(source, null);
	case TypeCodes.DOUBLE:
	    return new J2cDouble(source, null);
	case TypeCodes.VOID:
	    return new J2cVoid(source);
	default:
	    // What is the type of a char field.  It isn't
	    // `unsigned short', its `unsigned int'.  This
	    // isn't like arrays, where each member is the
	    // size it is supposed to be.
	    return new J2cInt(source,
			      promoteInt ? TypeCodes.INT : tag);
	}
    }

    /**
     * @deprecated
     * If everything wasn't typed as S3Blueprint, it would be easy to
     * replace all calls with ctx.blueprintFor(t) with
     * ctx.domain.blueprintFor(t).  This method is kind of silly.
     **/
    S3Blueprint blueprintFor(Type t) {
	return (S3Blueprint) domain.blueprintFor(t);
    }

    private HashMap csaMethods = new HashMap();
    private int csaHits;

    S3Method findCSAMethod(String name) {
	S3Method ret = (S3Method) csaMethods.get(name);
	if (ret == null) {
	    ret = findMethodObject(CSAbp, getUTF(name));
	    csaMethods.put(name, ret);
	}
	if (KEEP_CACHE_STATS)
	    csaHits++;
	return ret;
    }

    {
	if (KEEP_CACHE_STATS) {
	    new J2cImageCompiler.StatPrinter() {
		public void printStats() {
		    System.err.println("\nCSA methods in "
				       + domain + ":");
		    System.err.println("\t" + csaMethods.size()
				       + " methods resolved a total of"
				       + csaHits + " times");
		}
	    };
	}
    }

    /**
     * Search for a method one of a blueprint's dispatch tables.  If
     * it is a shared state blueprint, search the NVTable, otherwise
     * search the VTable.
     * @param bp   the blueprint to search
     * @param name the method's name as a UTF8 number
     **/
    static S3Method findMethodObject(S3Blueprint bp, int name) {
	for (Method.Iterator it = bp.getType().localMethodIterator();
	     it.hasNext(); ) {
	    Method m = it.next();
	    if (m.getSelector().getNameIndex() == name)
		return (S3Method) m;
	}
	if (bp.getParentBlueprint() != null) 
	    return findMethodObject((S3Blueprint) bp.getParentBlueprint(),
				    name);
	else
	    return null;
    }

    static final boolean KEEP_CACHE_STATS = J2cImageCompiler.KEEP_STATS;
    
    /**
     * Cache utf8 internment.  Does this really speed things up, or is
     * string->utf8 already fast.  utf8->string is dog slow.
     **/
    private static final HTObject2int string2utf = new HTObject2int();
    private static int string2utfHits;
    public static int getUTF(String name) {
	int nidx = string2utf.get(name);
	if (nidx == -1) {
	    nidx = RepositoryUtils.asUTF(name);
	    string2utf.put(name, nidx);
	}
	if (KEEP_CACHE_STATS)
	    string2utfHits++;
	return nidx;
    }

    static {
	if (KEEP_CACHE_STATS)
	    new J2cImageCompiler.StatPrinter() {
		public void printStats() {
		    System.err.println("\nContext caches:\n");
		    System.err.println("string -> utf8 mapping:");
		    System.err.println("\tValues Allocated: "
				       + string2utf.size());
		    System.err.println("\tHits:             "
				       + string2utfHits);
		}
	    };
    }
}

