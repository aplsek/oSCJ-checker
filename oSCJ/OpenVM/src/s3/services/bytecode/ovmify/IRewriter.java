package s3.services.bytecode.ovmify;

import java.io.IOException;
import java.util.HashMap;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantResolvedInstanceFieldref;
import ovm.core.domain.ConstantResolvedInstanceMethodref;
import ovm.core.domain.ConstantResolvedInterfaceMethodref;
import ovm.core.domain.ConstantResolvedStaticFieldref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.Domain;
import ovm.core.domain.Field;
import ovm.core.domain.JavaUserDomain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.Attribute;
import ovm.core.repository.Descriptor;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.UnboundSelector;
import ovm.core.repository.Attribute.LocalVariableTable;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.PragmaNoReadBarriers;
import ovm.core.services.memory.VM_Address;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.InstructionSet;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import ovm.services.bytecode.editor.Cursor;
import ovm.services.bytecode.editor.InstructionEditVisitor;
import ovm.services.bytecode.editor.LinearPassController;
import ovm.services.bytecode.editor.LocalsShifter;
import ovm.services.bytecode.editor.Marker;
import ovm.services.bytecode.editor.CodeFragmentEditor.ExceptionHandlerList;
import ovm.services.bytecode.reader.ByteCodeConstants;
import ovm.services.bytecode.reader.ByteCodeConstants.Attributes;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.ArrayList;
import ovm.util.NumberRanges;
import ovm.util.OVMError;
import ovm.util.PragmaUnsafe;
import ovm.util.logging.Logger;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import ovm.core.domain.ConstantPool;
import s3.core.domain.S3Domain;
import s3.core.domain.S3MemberResolver;
import s3.core.domain.S3Method;
import s3.core.domain.S3Type;
import s3.services.bootimage.Analysis;
import s3.services.bootimage.BootBase;
import s3.services.bootimage.ImageObserver;
import s3.services.transactions.Transaction;
import s3.util.PragmaAtomic;
import s3.util.PragmaAtomicNoYield;
import s3.util.PragmaException;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaTransformCallsiteIR;
import ovm.core.OVMBase;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.domain.DomainDirectory;
import ovm.services.bytecode.JVMConstants.InvokeSystemArguments;

/**
 * Rewriting code for the bootimage.  Any call to a static native
 * method of any class named LibraryImports will be resolved against
 * the blueprint of RuntimeExports
 *
 * FIXME: revisit when / how do we re-build S3Constants from scratch?
 *
 * @author Christian Grothoff, Jan Vitek, Krzysztof Palacz
 **/
public class IRewriter
    implements JVMConstants.InvokeSystemArguments, JVMConstants.Opcodes,
	JVMConstants {

    private static final boolean NO_POLL_CHECK_ENTRY =
	!InvisibleStitcher.getBoolean("emit-entry-pollcheck");
    private static final boolean NO_POLL_CHECK_LOOP =
	!InvisibleStitcher.getBoolean("emit-loop-pollcheck");
	
    private static final boolean COUNT_LOADS_STORES = false;
    	// just for debugging


    private final static HashMap warningsPrinted_ = new HashMap();
    private boolean isHandlingPragmas = true;

    Domain domain;
    protected NativeCallGenerator ncg;

    private S3Blueprint rtExportsBpt_;
    private Oop rtExportsOop_;

    public static void d(int kind, String msg)
	throws PragmaTransformCallsiteIR.Ignore
    {
	BootBase.d(kind, msg);
    }
    
    public static void d(int kind, Object o1, Object o2)
	throws PragmaTransformCallsiteIR.Ignore
    {
	BootBase.d(kind, o1, o2);
    }

    public static void pln(String str)
	throws PragmaTransformCallsiteIR.Ignore
    {
	BootBase.pln(str);
    }

    public IRewriter(Domain d) {
	domain = d;
	ncg = (d.isExecutive()
	       ? (NativeCallGenerator) new NativeCallGenerator.Executive(d)
	       : (NativeCallGenerator) new NativeCallGenerator.User());
	rtExportsOop_ = VM_Address.fromObject(d.getRuntimeExports()).asOop();
	rtExportsBpt_ = (S3Blueprint) rtExportsOop_.getBlueprint();
    }

    public void setHandlingPragmas(boolean flag) {
        this.isHandlingPragmas = flag;
    }

    private static Selector.Method currentSelector_;
    
    /**
     * Rewrite one method.
     * @return true iff rewriting changed anything.
     **/
    public boolean rewrite(Method method,
			   S3ByteCode.Builder builder,
			   S3Blueprint curBp,
			   boolean hasWriteBarriers,
			   boolean hasReadBarriers) {
        S3ByteCode code = builder.build();

	d(BootBase.LOG_REWRITING, code.getSelector() +
	  " has " + code.getAttributes().length + " attributes");
        ConstantPool rcpb = code.getConstantPool();
        CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);
        Selector.Method sel = method.getSelector();

        currentSelector_ = sel;
        
        if (isMagic(method)) {
	    d(BootBase.LOG_REWRITING, "skipped " + sel);
            return false;
	}

        cfe.runVisitor(
            new LinearPassController(
                new NonQuickifyingRewriteVisitor(curBp, hasWriteBarriers, hasReadBarriers)));

	d(BootBase.LOG_REWRITING, sel + (cfe.wasEdited() ? " changed" : " unchanged"));
        // int n = method.getSelector().getDescriptor().getArgumentLength();
        // cfe.commit(builder, n + 1);
	// FIXME: Do we need to commit if wasEdited is false?
        cfe.commit(builder, builder.getMaxStack(), builder.getMaxLocals());

// if you want to enable nullcheck squirter here, do not forget to disable it in its original
// place
//      squirtNullChecks((S3Method)code.getMethod(), builder, curBp);
        return cfe.wasEdited();
    }

    public void quickify(Method method, S3Blueprint bp) {
	quickify(method, method.getByteCode(), bp,
		 (MemoryManager.the().needsWriteBarrier()
		  && (PragmaNoBarriers.descendantDeclaredBy(method.getSelector(),
							    bp) == null
		      || MemoryManager.the().forceBarriers())),
		 (MemoryManager.the().needsReadBarrier() &&
		   PragmaNoReadBarriers.descendantDeclaredBy(method.getSelector(),bp)==null
		 
		  && ( PragmaNoBarriers.descendantDeclaredBy(method.getSelector(),
							    bp) == null
		        || MemoryManager.the().forceBarriers()
		     )  
		  ));
    }

    public boolean quickify(Method method, S3ByteCode code, S3Blueprint curBp, boolean hasWriteBarriers,
	    boolean hasReadBarriers) {
	S3ByteCode.Builder builder = new S3ByteCode.Builder(code);
	ConstantPool rcpb = code.getConstantPool();
	CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);
	Selector.Method sel = method.getSelector();
	currentSelector_ = sel;
	if (isMagic(method)) return false;
	cfe.runVisitor(new LinearPassController(new QuickifyingRewriteVisitor(curBp, hasWriteBarriers,
			hasReadBarriers)));
	cfe.commit(builder, builder.getMaxStack(), builder.getMaxLocals());
	method.addCode(builder.build());
	if (builder.getMaxLocals() == 0) Logger.global.warning("MaxLocal == 0 : " + method);
	return cfe.wasEdited();
    }

    public void transcribe(S3ByteCode.Builder builder) {
	S3ByteCode code = builder.build();
	char maxLocals = code.getMaxLocals();
	Method m = code.getMethod();

	if (m.getMode().isStatic()) {
	    ++maxLocals;
	    LocalVariableTable tbl = null;
	    int idx = 0;
	    int lvt_idx = ByteCodeConstants.attributeNames[Attributes.LocalVariableTable];
	    Attribute[] att = code.getAttributes();
	    if (att != null) att = (Attribute[]) att.clone();
	    else att = new Attribute[0];
	    for (; idx < att.length; idx++)
		if (att[idx].getNameIndex() == lvt_idx) {
		    tbl = (LocalVariableTable) att[idx];
		    break;
		}
	    if (tbl != null) {
		int len = tbl.size();
		char[] spc = new char[len + 1];
		char[] vlen = new char[len + 1];
		int[] name = new int[len + 1];
		Descriptor.Field[] type = new Descriptor.Field[len + 1];
		char[] vidx = new char[len + 1];

		for (int i = 0; i < len; i++) {
		    spc[i] = (char) tbl.getStartPC(i);
		    vlen[i] = (char) tbl.getLength(i);
		    name[i] = tbl.getVariableNameIndex(i);
		    type[i] = tbl.getDescriptor(i);
		    vidx[i] = (char) (tbl.getIndex(i) + 1);
		}

		// put local 0 at the end, since j2c never needs to look it up.
		spc[len] = 0;
		vlen[len] = (char) code.getBytes().length;
		name[len] = RepositoryUtils.asUTF("this");
		TypeName gt = m.getDeclaringType().getUnrefinedName();
		if (!gt.isGemeinsam()) gt = gt.asScalar().getGemeinsamTypeName();

		type[len] = Descriptor.Field.make(gt);
		vidx[len] = 0;

		att[idx] = new LocalVariableTable(spc, vlen, name, type, vidx);
	    }
	    builder.setAttributes(att);
	    code = builder.build();
	}
	ConstantPool rcpb = code.getConstantPool();
	CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);
	final Type.Scalar dclt = m.getDeclaringType().asScalar();

	// Conversion to Ovm specific bytecode
	LinearPassController lpc =
	    new LinearPassController(new BytecodeToOVMIR.Converter());
	lpc.add(new LocalsShifter(1, 0), new LinearPassController.MethodPicker() {
	    public boolean pickMethod(Selector.Method sel) {
		return dclt.getMethod(sel.getUnboundSelector()).getMode().isStatic();
	    }
	});

	lpc.run(cfe);
	cfe.commit(builder, code.getMaxStack(), maxLocals);
    }

    /**
     * The primary IRewriter interface.  This method is responsible
     * for transitioning bytecode objects through various states.
     *
     * @see s3.core.domain.S3ByteCode#getState
     **/
    synchronized public void ensureState(S3ByteCode b, int state) {
	assert (state == S3ByteCode.EXPANDED
		|| state == S3ByteCode.REWRITTEN);
	if (state == b.getState())
	    return;
	if (b.getState() == S3ByteCode.ORIGINAL)
	    rewriteMethod(b);
	if (state == S3ByteCode.REWRITTEN)
	    rewriteMethod2(b);
    }

    /**
     * Update code from the {@link S3ByteCode#ORIGINAL ORIGINAL} format
     * to the {@link S3ByteCode#REWRITTEN REWRITTEN} format.
     */
    private void rewriteMethod(S3ByteCode code) {
	S3Method method = (S3Method) code.getMethod();
	S3Blueprint bp =
	    (S3Blueprint) domain.blueprintFor(method.getDeclaringType());
        S3ByteCode.Builder builder;

        if (method.getMode().isNative()) {
            ConstantPool cBuilder = ((Type.Scalar) bp.getType()).getConstantPool();
            builder = ncg.fillInNativeMethod(method, cBuilder);
            if (builder == null) return; // native method that does not get any bytecode!
        } else {
            builder = new S3ByteCode.Builder(code);
        }
        transcribe(builder);

        // if nullchecks were ever needed in the executive domain, the nullcheck squirter
        // has to be extended to handle correctly VM_Address and VM_Word
        // (which look like object references, but are not - and must not be nullchecked)
        
        if (!domain.isExecutive()) {
		squirtNullChecks(method, builder, bp);
		squirtHWIORegistersAccess(method, builder, bp);
	}

        boolean hasWriteBarriers = MemoryManager.the().needsWriteBarrier();
        boolean hasReadBarriers  = MemoryManager.the().needsReadBarrier();
//        boolean hasEagerTranslatingReadBarriers = MemoryManager.the().needsEagerTranslatingReadBarrier();
	boolean hasLazyTranslatingReadBarriers = MemoryManager.the().needsBrooksTranslatingBarrier() ||
		MemoryManager.the().needsReplicatingTranslatingBarrier();
		
	boolean registerFinalizers =
	    (bp.getDomain() instanceof JavaUserDomain
	     && MemoryManager.the().supportsDestructors());
        
        if (PragmaNoBarriers
	    .descendantDeclaredBy(method.getSelector(), bp) != null
	    && !MemoryManager.the().forceBarriers()) {
            d(BootBase.LOG_BARRIERS, "no barriers for ", method);
            hasWriteBarriers=false;
            hasReadBarriers=false;
        }

        //!! just debugging - no read barriers in executive domain, where we have VM_Addresses all around
        if (PragmaNoReadBarriers
	    .descendantDeclaredBy(method.getSelector(), bp) != null || domain.isExecutive()) {
            hasReadBarriers=false;
        }

	// Bytecode transformation passes through several major steps,
        // where this is the largest.
	// A. transcription (in which local variable 0 is freed up in
	//    static methods, NEWARRAY and ANEWARRAY are replaced by
	//    SINGLENEWARRAY, and other random transformations take place
	//
	// Part A was done above, which brings us to
        // B. Now we have four rewriting passes:
	// (1) Optionally rewrite method calls to be labeled transactional 
        // (must be done before (2))
	// (2) Pragma and native call transformations
        // (1a) Add logging (must be done after (2), because that adds heap writes)
	// (3) Add RTjava read barriers
	// (4) Add registerFinalizer calls for finalizable object allocation
	//
	// Parts A and B are driven by the static analysis engine.
	// These transformations expand java bytecode to OvmIR, and
	// make the meaning of magic method calls explicit.
	// Subsequent passes run after static analysis is
	// complete, and can use the results of analysis.
	//
	// C. perform inlining
	// D. add pollchecks
	// E. optionally perform quickification
	
	int i = 0;
	if (Transaction.the().transactionalMode())
	    // FIXME: Why does this need a fixpoint?
	    while (Transaction.the() // (1) - rewrites method calls
		   .rewriteTransactional(i++, this, method, builder, bp, 1));
	while (rewrite(method, builder, bp, hasWriteBarriers, hasReadBarriers)); // (2)		    
	if (Transaction.the().transactionalMode())
	    // FIXME: Why does this need a fixpoint?
	    while (Transaction.the() // (1a) - inserts logging
		   .rewriteTransactional(i++, this, method, builder, bp, 2));
	method.addCode(builder.build());
	if (hasReadBarriers) {
	    readBarrierSquirter_.run(method);
	}
	    
//	if (hasEagerTranslatingReadBarriers) {
//	    eagerTranslatingReadBarrierSquirter_.run(method);
//	}
	if (hasLazyTranslatingReadBarriers) {
	    lazyTranslatingReadBarrierSquirter_.run(method);
	}	
	
	if (registerFinalizers)
	    finalizerRegistrar.run(method);
	REWRITTEN_METHOD_COUNT++;
	
	builder = new S3ByteCode.Builder(code);
//	squirtPollChecks(method, builder, bp);
	code.bang(builder.build());

	code.setState(S3ByteCode.EXPANDED);
    }

    static public int REWRITTEN_METHOD_COUNT;
    /**
     * This visitor does the transformations such as pragma
     * transformations and native call transformations, but not
     * quickification.
     **/
    public class NonQuickifyingRewriteVisitor extends RewriteVisitor {
        public NonQuickifyingRewriteVisitor(S3Blueprint bp, boolean wbar, boolean rbar) {
            super(bp, wbar, rbar);
        }

        /** @return true if selector represents a native method that can be 
         * rewritten to INVOKENATIVE */
        private boolean handledNativeCall(Method meth, Instruction i) {
	    boolean isExecutive = curBp_.getDomain().isExecutive();
	    byte[] special = ncg.getSpecialSequence(meth);
	    if (special != null) {
		// replace with special opcode!
		Cursor c = cfe.replaceInstruction();
		c.addSpecialSequence(special);
		// call all of the boot_ method that have been 
		// registered from the Driver. This happens after the
		// call to LibraryImports.boot()
		return true;
	    }
	    return false;
	}

        public void visit(Instruction.PUTFIELD_WITH_BARRIER_REF i) {
        }

        public void visit(Instruction.PUTFIELD i) {
	    try {
		ConstantResolvedInstanceFieldref f = cp.resolveInstanceField(i.getCPIndex(buf));
		if (addWriteBarriers && f.getField() instanceof Field.Reference) 
		    cfe.replaceInstruction().addPUTFIELD_WITH_BARRIER_REF(f);
	    } catch (LinkageException e) {  warn(i, e.getMessage());   }
	}

	public void visit(Instruction.PUTSTATIC_WITH_BARRIER_REF i) {}

	public void visit(Instruction.PUTSTATIC i) {
	    try {
		ConstantResolvedStaticFieldref f = cp.resolveStaticField(i.getCPIndex(buf));
		if (addWriteBarriers && f.getField() instanceof Field.Reference) 
		    cfe.replaceInstruction().addPUTSTATIC_WITH_BARRIER_REF(f);
	    } catch (LinkageException e) { warn(i, e.getMessage());  }
	}

        //TODO: As long as we do not erase VM_Address (and other Ephemerals)
        // this code will be incorrect.
        public void visit(Instruction.AASTORE i) {
	    if (addWriteBarriers && !MemoryManager.the().needsArrayAccessBarriers()) {
	    	cfe.replaceInstruction().addSimpleInstruction(AASTORE_WITH_BARRIER);
	    }
	}

	public void visit(Instruction.GETFIELD i) {
	    try {
		cp.resolveInstanceField(i.getCPIndex(buf));
	    } catch (LinkageException e) { warn(i, e.getMessage());  }
	}
	public void visit(Instruction.GETSTATIC i) {
	    try {
		cp.resolveStaticField(i.getCPIndex(buf));
	    } catch (LinkageException e) { warn(i, e.getMessage());  }
	}
	public void visit(Instruction.AFIAT i) {
	    try {
		cp.resolveClassAt(i.getCPIndex(buf));
	    } catch (LinkageException e) { warn(i, e.getMessage()); }
	}
	public void visit(Instruction.CHECKCAST i) {
	    try {
		cp.resolveClassAt(i.getCPIndex(buf));
	    } catch (LinkageException e) { warn(i, e.getMessage());  }
	}
	public void visit(Instruction.INSTANCEOF i) {
	    try {
		cp.resolveClassAt(i.getCPIndex(buf));
	    } catch (LinkageException e) { warn(i, e.getMessage());  }
	}
	public void visit(Instruction.NEW i) {
	    try {
		cp.resolveClassAt(i.getCPIndex(buf));
	    } catch (LinkageException e) { warn(i, e.getMessage());  }
	}
	public void visit(Instruction.SINGLEANEWARRAY i) {
	    try {
		cp.resolveClassAt(i.getCPIndex(buf));
	    } catch (LinkageException e) { warn(i, e.getMessage());  }
	}
	public void visit(Instruction.MULTIANEWARRAY i) {
	    try {
		cp.resolveClassAt(i.getCPIndex(buf));
	    } catch (LinkageException e) { warn(i, e.getMessage());  }
	}
        public void visit(Instruction.AASTORE_WITH_BARRIER i) {   }
        
        /**
         * The strategy for dealing with Miranda methods in Java 1.2 (i.e. uses of
         * INVVIRT where INVIFC is expected; this occurs for unimplemented interface
         * methods of an abstract class) is as follows: We try to resolve
         * every INVOKEVIRTUAL. When an instance method is not found, there will
         * be an exception. We catch the exception and resolve the selector as
         * an interface method, then we rewrite the bytecode to an INVOKEINTERFACE. 
         */

    // from sun/tools/java/ClassDefinition.java, inserted for all of those like me
    // who had no clue what Miranda methods are
    //
    // In early VM's there was a bug -- the VM didn't walk the interfaces
    // of a class looking for a method, they only walked the superclass
    // chain.  This meant that abstract methods defined only in interfaces
    // were not being found.  To fix this bug, a counter-bug was introduced
    // in the compiler -- the so-called Miranda methods.  If a class
    // does not provide a definition for an abstract method in one of
    // its interfaces then the compiler inserts one in the class artificially.
    // That way the VM didn't have to bother looking at the interfaces.
    //
    // This is a problem.  Miranda methods are not part of the specification.
    // But they continue to be inserted so that old VM's can run new code.
    // Someday, when the old VM's are gone, perhaps classes can be compiled
    // without Miranda methods.  Towards this end, the compiler has a
    // flag, -nomiranda, which can turn off the creation of these methods.
    // Eventually that behavior should become the default.
    //
    // Why are they called Miranda methods?  Well the sentence "If the
    // class is not able to provide a method, then one will be provided
    // by the compiler" is very similar to the sentence "If you cannot
    // afford an attorney, one will be provided by the court," -- one
    // of the so-called "Miranda" rights in the United States.

        private void javaOnePointTwo() throws LinkageException {
	    Instruction.INVOKEINTERFACE i = Instruction.INVOKEINTERFACE.singleton;
	    ConstantResolvedInterfaceMethodref imi = cp.resolveAndAddInterfaceMethod(i.getCPIndex(buf));
	    if (doSystemRewriting_) {
		Selector.Method sel = imi.asSelector();
		S3Blueprint bp = (S3Blueprint) imi.getStaticDefinerBlueprint();
		if (handledInlineSubstituteBytecode(sel, bp, i)) {
		    return;
		}
		//if (handledNativeCall(imi.getMethod(), i)) { pln("!!"+imi);return; }
	    }
	    cfe.replaceInstruction().addINVOKEINTERFACE(imi);	    
	}
         public void visit(Instruction.INVOKEVIRTUAL i) {
	     try {
		ConstantResolvedInstanceMethodref vmi = null;
		try {
		    vmi = cp.resolveInstanceMethod(i.getCPIndex(buf));
		} catch (LinkageException e) {
		    javaOnePointTwo();return;
		} catch (OVMError e) {
		    javaOnePointTwo();return;
		}
		if (doSystemRewriting_) {		   
		    S3Blueprint bp = (S3Blueprint) vmi.getStaticDefinerBlueprint();
		    if (handledInlineSubstituteBytecode(vmi.asSelector(), bp, i)) return;
		    // FIXME: never rewrite native virtual call sites, do
		    // create an body for this method that contains INVOKE_NATIVE
		    if (handledNativeCall(vmi.getMethod(), i)) { return; }
		}
	    } catch (LinkageException e) { warn(i, e.getMessage()); }
	}

        public void visit(Instruction.INVOKESTATIC i) {
	    try {
		ConstantResolvedStaticMethodref smi = getConstants().resolveStaticMethod(i.getCPIndex(buf));
		if (doSystemRewriting_) {
		    Selector.Method sel = smi.asSelector();
		    // FIXME call resolver first! else access permissions are unchecked!
		    //allowUnsafe(sel, bp);
		    if (handledKernelCall(sel, i)) return;
		    S3Blueprint bp = (S3Blueprint) smi.getSharedState().getBlueprint();
		    try {
			if (handledInlineSubstituteBytecode(sel, bp, i) || 
		            handledNativeCall(smi.getMethod(), i)) return;
		    } catch (Error e) {
			throw new Error("error rewriting " + sel + " for method " + smi.getMethod() + " original exception:\n"+e);
		    }
		    Descriptor.Method rdm = sel.getDescriptor();
		    int argwords = rdm.getArgumentCount() + rdm.getWideArgumentCount();
		    NumberRanges.checkChar(1 + argwords);
		}
	    } catch (LinkageException e) { warn(i, e.getMessage()); }
	}

        public void visit(Instruction.INVOKESPECIAL i) {
            try {
		ConstantResolvedInstanceMethodref smi = getConstants().resolveInstanceMethod(i.getCPIndex(buf));
		if (doSystemRewriting_) {
		    Selector.Method sel = smi.asSelector();
		    // FIXME call resolver first! else access permissions are unchecked!
		    S3Blueprint bp = (S3Blueprint) smi.getStaticDefinerBlueprint();
		    if (handledInlineSubstituteBytecode(sel, bp, i))
			return;
		    // FIXME: remember that privates and constructors take a `this' pointer.
		    if (handledNativeCall(smi.getMethod(), i)) return;
                 }
            } catch (LinkageException e) { warn(i, e.getMessage());  }
        }

        private boolean isKernelCall(Selector.Method sel) {
	    return sel.getDefiningClass().getShortNameIndex() == RepositoryUtils.asUTF("LibraryImports");
	}

        public void visit(Instruction.INVOKEINTERFACE i) {
            try {
		ConstantResolvedInterfaceMethodref imi = getConstants().resolveInterfaceMethod(i.getCPIndex(buf));
		if (doSystemRewriting_) {
		    Selector.Method sel = imi.asSelector();
		    S3Blueprint bp = (S3Blueprint) imi.getStaticDefinerBlueprint();
		    if (handledInlineSubstituteBytecode(sel, bp, i)) return;
		}
	    } catch (LinkageException e) { warn(i, e.getMessage());  }
        }

        private ConstantResolvedInstanceMethodref resolveKernelCall(Selector.Method sel) {
            Descriptor.Method desc = sel.getDescriptor();
            ArrayList newargs = new ArrayList();
            for (int i = 0; i < desc.getArgumentCount(); i++) {
		TypeName tn = desc.getArgumentType(i);
		if (tn.isCompound()) newargs.add(JavaNames.ovm_core_domain_Oop);
		else newargs.add(tn);
	    }
	    TypeName rettype = desc.getType();
	    if (rettype.isCompound()) {
		rettype = JavaNames.ovm_core_domain_Oop;
	    }
	    Descriptor.Method newdesc = Descriptor.Method.make(newargs, rettype);
	    int nameIndex = sel.getUnboundSelector().getNameIndex();
	    UnboundSelector.Method newsel = UnboundSelector.Method.make(nameIndex, newdesc);
	    int offset = S3MemberResolver.resolveVTableOffset(rtExportsBpt_, newsel, null);
	    if (offset == -1)
		throw new OVMError.Internal("Unable to match "
					    + sel
					    + " to RuntimeExports method");

	    Selector.Method rtesel = Selector.Method.make(newsel, rtExportsBpt_.getType().getUnrefinedName()
		    .asCompound());
	    Method m = S3MemberResolver.resolveInstanceMethod
		(rtExportsBpt_.getType().asCompound(),
		 rtesel.getUnboundSelector(),
		 null);
	    Type.Context ctx = curBp_.getType().getContext();
	    Object r = MemoryPolicy.the().enterMetaDataArea(ctx);
	    try {
		ConstantResolvedInstanceMethodref imi = 
		    ConstantResolvedInstanceMethodref.make(m, offset,
							   rtExportsBpt_);
		return imi;
	    } finally {
		MemoryPolicy.the().leave(r);
	    }
        }

	private boolean handledKernelCall(Selector.Method sel, Instruction.Invocation i) {
	    if (!isKernelCall(sel)) return false;
	    if (rtExportsBpt_ == null) {
		warn(i, "can't rewrite kernel call: ");
		return true;
	    }
	    ConstantResolvedInstanceMethodref imr = resolveKernelCall(sel);
	    Cursor c = cfe.replaceInstruction();
	    Descriptor.Method rdm = sel.getDescriptor();
	    int argwords = rdm.getArgumentCount() + rdm.getWideArgumentCount();
	    char rollDepth = NumberRanges.checkChar(1 + argwords);
	    c.addResolvedRefLoadConstant(rtExportsOop_);
	    c.addRoll(rollDepth, (byte) 1);
	    c.addINVOKEVIRTUAL(imr);
	    if (sel.getDescriptor().getType().isCompound()) c.addFiat(sel.getDescriptor().getType());
	    return true;
	}

        /** If <em>sel</em> and <em>bp</em> identify a method that declares 
         * (a descendant of) {@link s3.util.PragmaTransformCallsiteIR},  remove the 
         * current instruction, replace it with the bytecode sequence associated with 
         * the pragma, and return <code>true</code>, otherwise return <code>false</code>.
         *  @param sel selector of a method
         *  @param bp blueprint of the method's declaring class (caller ensures) 
         *  @return <code>true</code> if a pragma was found and processed,
         * otherwise <code>false</code>.*/
        private boolean handledInlineSubstituteBytecode(Selector.Method sel, Blueprint bp, Instruction.Invocation instr) {
	    if (!isHandlingPragmas) return false;
	    Object sub;
	    try {
		sub = PragmaTransformCallsiteIR.descendantDeclaredBy(sel, bp);
	    } catch (PragmaException.UnregisteredPragmaException upe) {
		d(-1, sel + " with unregistered InlineSubstituteBytecode" + " behavior is invoked in " + buf.getSelector());
		sub = PragmaTransformCallsiteIR.DeadRewriter.make("Method has unregistered pragma: ");
	    }
	    if (sub == null) return false;
	    int pc = buf.getPC();
	    if (sub instanceof byte[]) {
		cfe.replaceInstruction(pc).addSpecialSequence((byte[]) sub);
		return true;
	    }
	    // rewrite() calls cfe.getCursorAfterMarker( pc) again; if I understand
	    // correctly, that will return the same cursor obtained above and
	    // positioned in the same place (after ROLL/POP if used), so it's ok.
	    ((PragmaTransformCallsiteIR.Rewriter) sub).rewrite(curBp_, instr, bp, sel, cfe);
	    return true;
	}

        public void visit(Instruction.LDC i) {
            try {
                i.getCPIndex(buf); // advance PC
                byte tag = i.getCType(buf, cp);
                switch (tag) {
                    case JVMConstants.CONSTANT_Integer :
                    case JVMConstants.CONSTANT_Float :
                        cfe.replaceInstruction(getPC()).addQuickLdc( i.getValue(buf, cp));
                        break;
                    case JVMConstants.CONSTANT_Reference :
                    case JVMConstants.CONSTANT_String :
                    case JVMConstants.CONSTANT_SharedState :
                    case JVMConstants.CONSTANT_Binder :
                    case JVMConstants.CONSTANT_Class:
		    case JVMConstants.CONSTANT_ResolvedClass:
                	break;
                    default : warn(i, "did not expect constant type " + tag);
                }
            } catch (ovm.util.OVMError e) { warn(i, e.getMessage()); }
        }

        public void visit(Instruction.LDC_W i) {
            try {
                byte tag = i.getCType(buf, cp);
                switch (tag) {
                    case JVMConstants.CONSTANT_Integer :
                    case JVMConstants.CONSTANT_Float :
                        cfe.replaceInstruction().addQuickLdc(i.getValue(buf, cp));
                        break;
                    case JVMConstants.CONSTANT_Reference :
                    case JVMConstants.CONSTANT_String :
                    case JVMConstants.CONSTANT_SharedState :
                    case JVMConstants.CONSTANT_Binder :
                    case JVMConstants.CONSTANT_Class:
		    case JVMConstants.CONSTANT_ResolvedClass:
                	break;
                    default :
                        warn(i, "did not expect constant type " + tag);
                }
            } catch (ovm.util.OVMError e) {
                warn(i, e.getMessage());
            }
        }

        public void visit(Instruction.LDC2_W i) {
            cfe.replaceInstruction(getPC()).addQuickLdc(i.getValue(buf, cp));
        }

    } // end NonQuickifyingRewriteVisitor


    
    private static ReadBarrierSquirter readBarrierSquirter_ = new ReadBarrierSquirter();

    public static class ReadBarrierSquirter extends InstructionEditVisitor {
     
	public void visit(Instruction.AALOAD i) {
	    Cursor c=cfe.getCursorAfterMarker(getPC());
	    c.addSimpleInstruction(DUP);
	    c.addSimpleInstruction(READ_BARRIER);        
	}

	// fixme: what about VM_Address ? - this does include barrier on VM_Address
	// !! just for debugging - this is not reading of a reference from the heap...
/*	
	public void visit(Instruction.ALOAD i) {
	    Cursor c=cfe.getCursorAfterMarker(getPC());
	    c.addSimpleInstruction(DUP);
	    c.addSimpleInstruction(READ_BARRIER);        
	}
*/
	// fixme: what about VM_Address ? - this does include barrier on VM_Address
	// !! just for debugging
	
/*	public void visit(Instruction.ASTORE i) {
	    Cursor c=cfe.getCursorBeforeMarker(getPC());
	    c.addSimpleInstruction(DUP);
	    c.addSimpleInstruction(READ_BARRIER);        
	}
*/	

	public void visit(Instruction.GETFIELD i) {
	    Cursor c=cfe.getCursorAfterMarker(getPC());
	    Descriptor.Field f = i.getSelector(buf, cp).getDescriptor();
	    if (f.isReference()
		&& f.getType() != JavaNames.ovm_core_services_memory_VM_Address
		&& f.getType() != JavaNames.ovm_core_services_memory_VM_Word) {
		c.addSimpleInstruction(DUP);
		c.addSimpleInstruction(READ_BARRIER);        
	    }
	}

	public void visit(Instruction.GETSTATIC i) {
	    Cursor c=cfe.getCursorAfterMarker(getPC());
	    Descriptor.Field f = i.getSelector(buf, cp).getDescriptor();
	    if (f.isReference()
		&& f.getType() != JavaNames.ovm_core_services_memory_VM_Address
		&& f.getType() != JavaNames.ovm_core_services_memory_VM_Word) {
		c.addSimpleInstruction(DUP);
		c.addSimpleInstruction(READ_BARRIER);        
	    }
	}
    }
    
//    private static EagerTranslatingReadBarrierSquirter eagerTranslatingReadBarrierSquirter_ = new EagerTranslatingReadBarrierSquirter();
/*
    public static class EagerTranslatingReadBarrierSquirter extends InstructionEditVisitor {
     
	public void visit(Instruction.AALOAD i) {
	    Cursor c=cfe.getCursorAfterMarker(getPC());
	    c.addSimpleInstruction(CHECKING_TRANSLATING_READ_BARRIER);        
	}

	public void visit(Instruction.GETFIELD i) {
	    Cursor c=cfe.getCursorAfterMarker(getPC());
	    Descriptor.Field f = i.getSelector(buf, cp).getDescriptor();
	    if (f.isReference()
		&& f.getType() != JavaNames.ovm_core_services_memory_VM_Address
		&& f.getType() != JavaNames.ovm_core_services_memory_VM_Word) {
		c.addSimpleInstruction(CHECKING_TRANSLATING_READ_BARRIER);
	    }
	}

	public void visit(Instruction.GETSTATIC i) {
	    Cursor c=cfe.getCursorAfterMarker(getPC());
	    Descriptor.Field f = i.getSelector(buf, cp).getDescriptor();
	    if (f.isReference()
		&& f.getType() != JavaNames.ovm_core_services_memory_VM_Address
		&& f.getType() != JavaNames.ovm_core_services_memory_VM_Word) {
		c.addSimpleInstruction(CHECKING_TRANSLATING_READ_BARRIER);        
	    }
	}
    }
*/
    // we rely on that nullcheck is inserted before the barrier in user domain
    // in executive domain nullchecks are not inserted, we assume no np dereferences there
    
    private static LazyTranslatingReadBarrierSquirter lazyTranslatingReadBarrierSquirter_ = new LazyTranslatingReadBarrierSquirter();

    // FIXME: what about VM_Word, VM_Address ?
    public static class LazyTranslatingReadBarrierSquirter extends InstructionEditVisitor {    

    	private boolean brooks = MemoryManager.the().needsBrooksTranslatingBarrier();
    	private boolean replicating = MemoryManager.the().needsReplicatingTranslatingBarrier();

        private void addBarrierHere(int offset, int ibar) {
        	Cursor c=cfe.getCursorBeforeMarker(getPC());
   	 	c.addRoll( (char)offset, (byte) -1);
   	 	c.addSimpleInstruction(ibar);
        	c.addRoll( (char)offset, (byte) 1 );
        }
        
        private void addBarrierHere(int ibar) {
        	Cursor c=cfe.getCursorBeforeMarker(getPC());
        	c.addSimpleInstruction(ibar);
        }

        private void addCounter(int code) {
        	Cursor c=cfe.getCursorBeforeMarker(getPC());
        	c.addLoadConstant(code);
        	c.addSimpleInstruction( INCREMENT_COUNTER );
        }
        
	// called when the barrier is added	
	public void caught(Instruction i) {
	//	System.out.println("TRB-caught:"+i);
	}
        
        // ..., arrayref, index  -> ..., value 

        public void visit(Instruction.ArrayLoad i) {
        	
        	if (COUNT_LOADS_STORES) {
        		addCounter(3);
        	}
        	
        	caught(i);
        	if (brooks && !MemoryManager.the().needsArrayAccessBarriers()) {
	        	addBarrierHere(2,NONCHECKING_TRANSLATING_READ_BARRIER);
		}
	}
	

        //     ..., arrayref, index, value  -> ...
        public void visit(Instruction.ArrayStore i) {

        	if (COUNT_LOADS_STORES) {
        		addCounter(4);
        	}

        	caught(i);
        	if (MemoryManager.the().needsArrayAccessBarriers()) {
			return;
        	}
        	if ((i instanceof Instruction.DASTORE) || (i instanceof Instruction.LASTORE)) {
        		if (replicating) {
				Cursor c=cfe.getCursorBeforeMarker(getPC());
        			c.addCOPY((byte)4);
        			c.addSimpleInstruction(i.getOpcode());
			}
			addBarrierHere(4,NONCHECKING_TRANSLATING_READ_BARRIER);		
		} else if (i instanceof Instruction.AASTORE) {
			// translation done already in the AASTORE barrier
			return; 
		} else {
			if (replicating) {
				Cursor c=cfe.getCursorBeforeMarker(getPC());
				c.addCOPY((byte)3);
				c.addSimpleInstruction(i.getOpcode());
			}
			addBarrierHere(3,NONCHECKING_TRANSLATING_READ_BARRIER);
		}
	}


	// ARRAYLENGTH not needed, because array length is read only
	// public void visit(Instruction.ARRAYLENGTH i)
 
	// ..., objectref  -> ..., value	
	public void visit(Instruction.GETFIELD i) {
        	if (COUNT_LOADS_STORES) {
        		addCounter(1);
        	}
	
        	caught(i);
        	if (brooks) {
	        	addBarrierHere(NONCHECKING_TRANSLATING_READ_BARRIER);
		}
	}
		
	// ..., objectref, value ->  ...
	public void visit(Instruction.PUTFIELD i) {
        	if (COUNT_LOADS_STORES) {
        		addCounter(2);
        	}
	
        	caught(i);		
       
		ConstantPool cp = (ConstantPool) cfe.getConstantsEditor();
		ConstantResolvedInstanceFieldref f = null;
		
		try {
			f = cp.resolveInstanceField(i.getCPIndex(buf));
		} catch (LinkageException e) { 
			throw new Error("error in field resolving while inserting barriers for instruction "+i+": "+e);
		}
		
		
		Selector.Field sel = f.asSelector();
		Descriptor.Field des = sel.getDescriptor();	
		
		if (des.isWidePrimitive()) {
			if (replicating) {
				Cursor c=cfe.getCursorBeforeMarker(getPC());
				c.addCOPY((byte)3);
				c.addPUTFIELD(f);
			}
	        	addBarrierHere(3,NONCHECKING_TRANSLATING_READ_BARRIER);
		} else {
			if (des.isReference() 		
				&& des.getType() != JavaNames.ovm_core_services_memory_VM_Address
				&& des.getType() != JavaNames.ovm_core_services_memory_VM_Word) {
				
				// handled in PUTFIELD barrier
				return;

			}
			if (replicating) {
				Cursor c=cfe.getCursorBeforeMarker(getPC());			
				c.addCOPY((byte)2);
				c.addPUTFIELD(f); 
			}
			addBarrierHere(2,NONCHECKING_TRANSLATING_READ_BARRIER);
		}
	}

	// PUTSTATIC
	// ...,value =>...
	// handled by putfield barrier

	// INVOKEVIRTUAL, INVOKESPECIAL, and INVOKEINTERFACE
	//     ..., objectref, [arg1, [arg2 ...]]  -> ...
	// not needed for invocations: blueprints are read-only and a copy stays at the original
	// object's location as long as the fowarding pointer does

	// MONITORENTER, MONITOREXIT	
	// ..., objectref  -> ...
	// handled by monitor implementation 
	
	// IF_ACMPEQ, IF_ACMPNE	
	// objectref1, objectref2 -> ...
	// handled by barriers inserted in CodeGen.java
	
	// just for debugging - checking pointers -- this is otherwise not needed 
	/*
	public void visit(Instruction.INSTANCEOF i) {
		addBarrierHere(CHECKING_TRANSLATING_READ_BARRIER);
	}
	
	public void visit(Instruction.CHECKCAST i) {
		addBarrierHere(CHECKING_TRANSLATING_READ_BARRIER);
	}
	*/
    }


     /**
     * Visitor that actually adds poll checks.
     */
    public static class PollCheckSquirter extends InstructionEditVisitor {
        private void addPOLLCHECKHere() {
            cfe.getCursorBeforeMarker(getPC()).addSimpleInstruction(POLLCHECK);
        }
        public void visit(Instruction.Switch i) {
	    // code taken our of J2c's BBSpec
	    int[] target = i.getTargets(buf);
	    int dflt = i.getDefaultTarget(buf);
	    boolean back = dflt < 0;
	    for (int idx = 0; idx < target.length && !back; idx++)
		if (target[idx] < 0) back = true;
	    if (back) addPOLLCHECKHere();	    
	}
        public void visit(Instruction.ConditionalJump i) {
	    if (i.getBranchTarget(buf) < 0) addPOLLCHECKHere();
	}

	public void visit(Instruction.UnconditionalJump i) {
	    if (i.getTarget(buf) < 0) addPOLLCHECKHere();
	}
    }

    private static PollCheckSquirter pollCheckSquirter_ = new PollCheckSquirter();


    // 
    // INB
    // portaddress -> value
    //
    // OUTB
    // value, portaddress -> ...
    //
    public static class HWIORegistersAccessSquirter extends InstructionEditVisitor {
    
    	// which means that container extends HardwareObject + field is of type "volatile int" and is not final
    	private boolean fieldIsIORegister(int fieldIndex) {

		ConstantPool cp = (ConstantPool) cfe.getConstantsEditor();
		ConstantResolvedInstanceFieldref f = null;
		
		try {
			f = cp.resolveInstanceField(fieldIndex);
		} catch (LinkageException e) { 
			throw new Error("Error in field resolving "+e);

		}
		
		Type t = f.getField().getDeclaringType();
		Domain d = (S3Domain)  t.getDomain();
			
		Blueprint bp = null;
		
		try {
			bp = (S3Blueprint) d.blueprintFor( JavaNames.ovm_hw_HardwareObject, d.getSystemTypeContext() );  
		} catch (LinkageException e) { 
			throw new Error("Error in resolving HardwareObject - base of HW objects: "+e);
		}

		if (!t.isSubtypeOf( bp.getType() )) {
			return false;
		}
		
		Type ft = null;
		
		try {
			ft = f.getField().getType();
		} catch (LinkageException e) {
			throw new Error("Error in field type resolving "+e);
		}
		
		return ft.isPrimitive() &&
			f.getField().getMode().isVolatile() &&
			!f.getField().getMode().isFinal() &&
			( ((Type.Primitive)ft).getName().getTypeTag() == TypeCodes.INT);
    	}
    
	// ..., objectref, value ->  ...    
    	public void visit(Instruction.PUTFIELD i) {

    		if (!fieldIsIORegister(i.getCPIndex(buf))) {
    			return ;
		}
			
    		// roll the objectref on top of stack
    		
    	       	Cursor c=cfe.getCursorBeforeMarker(getPC());
   	 	c.addRoll( (char)2, (byte) -1);
   	 	
   	 	// ..., value, objectref
   	 	
   	 	
   	 	// get the port address on the stack 
   	 	
		ConstantPool cp = (ConstantPool) cfe.getConstantsEditor();
		ConstantResolvedInstanceFieldref f = null;
		
		try {
			f = cp.resolveInstanceField(i.getCPIndex(buf));
		} catch (LinkageException e) { 
			throw new Error("Error in field resolving"+e);

		}

   	 	cfe.replaceInstruction().addGETFIELD(f);
   	 	
   	 	// ..., value, portaddress
   	 	
   	 	c = cfe.getCursorAfterMarker(getPC());
   	 	
   	 	c.addSimpleInstruction(OUTB);
   	 	
   	 	// ...
   	 	
    	}

    	// ..., objectref  -> ..., value    	
    	//
    	// after reading the port address from the field, do 
    	// the port access
    	public void visit(Instruction.GETFIELD i) {
    	
    		if (!fieldIsIORegister(i.getCPIndex(buf))) {
    			return ;
		}

    		Cursor c=cfe.getCursorAfterMarker(getPC());
        	c.addSimpleInstruction(INB);		
    	}
    	
     }
     
     /**
     * Visitor that actually adds null checks.
     *
     * if nullchecks were ever needed in the executive domain, the nullcheck squirter
     * has to be extended to handle correctly VM_Address and VM_Word
     * (which look like object references, but are not - and must not be nullchecked)
     * another issue is to throw the exception from the correct domain, specially when in 
     * inlined code
     */
    public static class NullCheckSquirter extends InstructionEditVisitor {

        private void addNULLCHECKHere(int offset) {
        	Cursor c=cfe.getCursorBeforeMarker(getPC());
   	 	c.addRoll( (char)offset, (byte) -1);
   	 	c.addSimpleInstruction(NULLCHECK);
        	c.addRoll( (char)offset, (byte) 1 );
        }
        
        private void addNULLCHECKHere() {
        	Cursor c=cfe.getCursorBeforeMarker(getPC());
        	c.addSimpleInstruction(NULLCHECK);
        }
        
        // called when the nullcheck is not added
        public void visit(Instruction i) {
        //	System.out.println("NC-missed:"+i);
	}

	// called when the nullcheck is added	
	public void caught(Instruction i) {
	//	System.out.println("NC-caught:"+i);
	}
        
        // ..., arrayref, index  -> ..., value 
        public void visit(Instruction.ArrayLoad i) {
        	caught(i);
        	addNULLCHECKHere(2);
	}

        //     ..., arrayref, index, value  -> ...
        public void visit(Instruction.ArrayStore i) {
        	caught(i);
        	if ((i instanceof Instruction.DASTORE) || (i instanceof Instruction.LASTORE)) {
			addNULLCHECKHere(4);		
		} else {
			addNULLCHECKHere(3);
		}
	}

	// ..., arrayref  ->  ..., length 
	public void visit(Instruction.ARRAYLENGTH i) {
        	caught(i);
        	addNULLCHECKHere();
	}
 
	// ..., objectref ->  objectref 
	public void visit(Instruction.ATHROW i) {
        	caught(i);
        	addNULLCHECKHere();
	}
	
	// ..., objectref  -> ..., value
	public void visit(Instruction.GETFIELD i) {
        	caught(i);
        	addNULLCHECKHere();
	}
		
	// ..., objectref, value ->  ...
	public void visit(Instruction.PUTFIELD i) {
        	caught(i);		
        	
		ConstantPool cp = (ConstantPool) cfe.getConstantsEditor();
		ConstantResolvedInstanceFieldref f = null;
		
		try {
			f = cp.resolveInstanceField(i.getCPIndex(buf));
		} catch (LinkageException e) { 
			throw new Error("error in field resolving while inserting nullchecks for instruction "+i+": "+e);
		}
		
		Selector.Field sel = f.asSelector();
		
		
		if (sel.getDescriptor().isWidePrimitive()) {
	        	addNULLCHECKHere(3);
		} else {
			addNULLCHECKHere(2);
		}
	}

	// for INVOKEVIRTUAL, INVOKESPECIAL, and INVOKEINTERFACE
	//     ..., objectref, [arg1, [arg2 ...]]  -> ...
	public void visit(Instruction.Invocation i) {
        	caught(i);		
		if (i instanceof Instruction.INVOKESTATIC) {
			return ;
		}

		ConstantPool cp = (ConstantPool) cfe.getConstantsEditor();
		Selector.Method sel =	i.getSelector(buf, cp);	        	
		Descriptor.Method d = sel.getDescriptor();
		
		/*
		this was an attempt to insert nullchecks into executive domain code
		it seem to have worked, though did not solve all issues - Miranda methods,
		nullchecks in inlined code, and potentially many other
		
		S3Blueprint bp = null;
		
		try {
			if (i instanceof Instruction.INVOKEVIRTUAL || i instanceof Instruction.INVOKESPECIAL) {

				ConstantResolvedInstanceMethodref vmi = null;
				vmi = cp.resolveInstanceMethod(i.getCPIndex(buf));
				bp = (S3Blueprint) vmi.getStaticDefinerBlueprint();
				
			} else if (i instanceof Instruction.INVOKEINTERFACE) {
			
				ConstantResolvedInterfaceMethodref vmi = null;
				vmi = cp.resolveInterfaceMethod( ((Instruction.INVOKEINTERFACE)i).getCPIndex(buf));
				bp = (S3Blueprint) vmi.getStaticDefinerBlueprint();
			} else {

				throw new Error("internal error");
			}
		} catch (LinkageException lex) {
			//throw new Error("failed to resolve method while inserting nullcheck for instruction "+i+": "+lex);		
			System.out.println("Failed to resolve method while inserting nullcheck for instruction "+i+": "+lex+
				", STILL adding nullcheck");
			int argwords = d.getArgumentCount() + d.getWideArgumentCount();
		
        		addNULLCHECKHere(argwords+1);				
			return;
		}
		
		Object sub;
		try {
			sub = PragmaTransformCallsiteIR.descendantDeclaredBy(sel, bp);
		} catch (PragmaException.UnregisteredPragmaException upe) {
			throw new Error("unregistered pragma while inserting nullcheck for instruction "+i+": "+upe);
		}
		
		if (sub==null) {
			// no bytecode inlines	
			int argwords = d.getArgumentCount() + d.getWideArgumentCount();
		
        		addNULLCHECKHere(argwords+1);
		} else {
			System.out.println("SKIPPED NULLCHECK for Inline - instruction "+i);
		}
		*/
		
		int argwords = d.getArgumentCount() + d.getWideArgumentCount();
		
        	addNULLCHECKHere(argwords+1);
	}
	
	// ..., objectref  -> ...
	public void visit(Instruction.Synchronization i) {
        	caught(i);		
		// MONITORENTER, MONITOREXIT
        	addNULLCHECKHere();
	}
	
    }

    private static HWIORegistersAccessSquirter hwIORegistersAccessSquirter_ = new HWIORegistersAccessSquirter();
    private static NullCheckSquirter nullCheckSquirter_ = new NullCheckSquirter();

    private static final UnboundSelector.Method setReschedulingEnabledSel = 
	RepositoryUtils.selectorFor("Lovm/services/threads/UserLevelThreadManager;", 
		"setReschedulingEnabled:(Z)Z").asMethod().getUnboundSelector();

    private static final UnboundSelector.Method setReschedulingEnabledNoYieldSel = 
	RepositoryUtils.selectorFor("Lovm/services/threads/UserLevelThreadManager;", 
		"setReschedulingEnabledNoYield:(Z)Z").asMethod().getUnboundSelector();

    // UserLevelThreadManager.setReschedulingEnabled() is an interface
    // method, but since we know the exact type of the ThreadManager
    // singleton, we can emit a virtual call
    private static ConstantResolvedInstanceMethodref setReschedulingEnabled;
    private static ConstantResolvedInstanceMethodref setReschedulingEnabledNoYield;
    
    public static class RestoreEnabledSquirter extends InstructionEditVisitor {
	int enabledVar;
	Object tm;
	boolean noYield;
	
	private RestoreEnabledSquirter(int enabledVar, Object tm, boolean noYield) {
	    this.enabledVar = enabledVar;
	    this.tm = tm;
	    this.noYield = noYield;
	}
	
	private void restoreEnabled(Cursor c) {
	    c.addResolvedRefLoadConstant(tm);
	    c.addILoad((char) enabledVar);
	    
	    c.addINVOKEVIRTUAL( noYield ? setReschedulingEnabledNoYield : setReschedulingEnabled );
	    c.addSimpleInstruction(POP);
	}

	public void visit(Instruction.ReturnValue _) {
	    restoreEnabled(cfe.getCursorBeforeMarker(getPC()));
	}
	public void visit(Instruction.RETURN _) {
	    restoreEnabled(cfe.getCursorBeforeMarker(getPC()));
	}
    }

    /**
     * This parameter from ImageObserver is captured at build-time by
     * a call to finishRewriting, and used in runtime calls to
     * rewriteMethod2.
     *
     * FIXME: How obscure.
     **/
    boolean shouldQuickify;
    
    /**
     * Complete rewriting of pre-compiled code for a domain.  This
     * method calls {@link #rewriteMethod2} on all methods in the
     * domain that are to be compiled.
     **/
    public void finishRewriting(final Analysis anal) {
	shouldQuickify = ImageObserver.the().shouldQuickify();
	new Analysis.MethodWalker() {
		public void walk(Method _m) {
		    if (!anal.shouldCompile(_m))
			return;
		    S3Blueprint curBP =
			(S3Blueprint) domain.blueprintFor(_m.getDeclaringType());
		    rewriteMethod2(_m.getByteCode());
		}
	    }.walkDomain(domain);
    }

    /**
     * A second bytecode rewriting pass that should be performed after
     * bytecode-level analysis and transformation.
     **/
    private void rewriteMethod2(S3ByteCode code) {
	S3Method m = (S3Method) code.getMethod();
	S3Blueprint curBp =
	    (S3Blueprint) domain.blueprintFor(m.getDeclaringType());
	S3ByteCode.Builder builder = new S3ByteCode.Builder(code);

	// when done here, it breaks PragmaAtomic and PragmaNoPollcheck
	//squirtPollChecks(m, builder, curBp);

	code.bang(builder.build());

	if (shouldQuickify)
	    quickify(m, curBp);

	code.setState(S3ByteCode.REWRITTEN);
    }
    
    
    private void squirtHWIORegistersAccess(S3Method method,
                                  S3ByteCode.Builder builder,
                                  S3Blueprint curBp) {
        
        int nexc = method.getThrownTypeCount();
        
        try {
	        if (nexc>0) {
 	      	 	for(int i=0;i<nexc;i++) {
        			if ( method.getThrownType(i).getName().equals( JavaNames.ovm_hw_PragmaNoHWIORegistersAccess ) ) {
        				return;
				}
			}
        
		}
	} catch (LinkageException e) { 
		throw new Error("error in looking up thrown exceptions of method "+method+":"+e);
	}
		
        
	S3ByteCode code=builder.build();
   	ConstantPool rcpb = code.getConstantPool();
      /* TODO: should this be included here ? CNT++;*/
        CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);

        InstructionBuffer codeBuf = cfe.getOriginalCode();
        codeBuf.rewind();

	hwIORegistersAccessSquirter_.beginEditing(codeBuf, cfe);
	try {
		while (codeBuf.hasRemaining()) {
			hwIORegistersAccessSquirter_.visitAppropriate(codeBuf.get());
		}
	} finally {
		hwIORegistersAccessSquirter_.endEditing();
	}
	cfe.commit(builder, builder.getMaxStack(), builder.getMaxLocals());
    }


    /**
     * Adds NULLCHECK instructions before each instruction
     * that takes reference from stack and should throw 
     * null pointer exception, if it is null
     */
    private void squirtNullChecks(S3Method method,
                                  S3ByteCode.Builder builder,
                                  S3Blueprint curBp) {
        
	S3ByteCode code=builder.build();
   	ConstantPool rcpb = code.getConstantPool();
      /* TODO: should this be included here ? CNT++;*/
        CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);

        InstructionBuffer codeBuf = cfe.getOriginalCode();
        codeBuf.rewind();

	nullCheckSquirter_.beginEditing(codeBuf, cfe);
	try {
		while (codeBuf.hasRemaining()) {
			nullCheckSquirter_.visitAppropriate(codeBuf.get());
		}
	} finally {
		nullCheckSquirter_.endEditing();
	}
	cfe.commit(builder, builder.getMaxStack(), builder.getMaxLocals());
    }

    /**
     * Adds POLLCHECK instructions at the beginning of the method
     * and immediately prior to backwards branches.
     */
    private void squirtPollChecks(S3Method method,
                                  S3ByteCode.Builder builder,
                                  S3Blueprint curBp) {
	if (PragmaAtomic.descendantDeclaredBy(method.getSelector(), curBp) != null) {
	    d(BootBase.LOG_POLLCHECKS, method + " is atomic");
	    ThreadServicesFactory tsf = (ThreadServicesFactory)
		ThreadServiceConfigurator.config.
		getServiceFactory(ThreadServicesFactory.name);
	    UserLevelThreadManager tm
		= (UserLevelThreadManager) tsf.getThreadManager();
		
	    boolean noYield = PragmaAtomicNoYield.descendantDeclaredBy(method.getSelector(), curBp) != null;
	    
	    if ( !noYield && setReschedulingEnabled == null) {
		S3Blueprint bp = (S3Blueprint) VM_Address.fromObject(tm).asOop().getBlueprint();
		int offset = S3MemberResolver.resolveVTableOffset(bp, setReschedulingEnabledSel, null);
		Method m = S3MemberResolver.resolveInstanceMethod(bp.getType().asCompound(), setReschedulingEnabledSel, null);
		Type.Context ctx = curBp.getType().getContext();
		Object r = MemoryPolicy.the().enterMetaDataArea(ctx);
		try {
		    setReschedulingEnabled = ConstantResolvedInstanceMethodref.make(m, offset, bp);
		} finally {
		    MemoryPolicy.the().leave(r);
		}
	    } else if (noYield && setReschedulingEnabledNoYield == null) {
		S3Blueprint bp = (S3Blueprint) VM_Address.fromObject(tm).asOop().getBlueprint();
		int offset = S3MemberResolver.resolveVTableOffset(bp, setReschedulingEnabledNoYieldSel, null);
		Method m = S3MemberResolver.resolveInstanceMethod(bp.getType().asCompound(), setReschedulingEnabledNoYieldSel, null);
		Type.Context ctx = curBp.getType().getContext();
		Object r = MemoryPolicy.the().enterMetaDataArea(ctx);
		try {
		    setReschedulingEnabledNoYield = ConstantResolvedInstanceMethodref.make(m, offset, bp);
		} finally {
		    MemoryPolicy.the().leave(r);
		}
	    }	    
	    S3ByteCode code = builder.build();
	    ConstantPool rcpb = code.getConstantPool();
	    int enabledVar = code.getMaxLocals();
	    CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);
	    InstructionBuffer codeBuf = cfe.getOriginalCode();
	    Cursor c = cfe.getCursorBeforeMarker(0);
	    c.addResolvedRefLoadConstant(tm);
	    c.addLoadConstant(0);
	    c.addINVOKEVIRTUAL( noYield ? setReschedulingEnabledNoYield : setReschedulingEnabled);
	    c.addIStore((char) enabledVar);
	    Marker enableSaved = c.addMarker();
	    RestoreEnabledSquirter squirt = new RestoreEnabledSquirter(enabledVar, tm, noYield);
	    codeBuf.rewind();
	    squirt.beginEditing(codeBuf, cfe);
	    int lastPC = 0;
	    Instruction last = null;
            while (codeBuf.hasRemaining()) {
		last = codeBuf.get();
		lastPC = codeBuf.getPC();
                squirt.visitAppropriate(last);
            }

	    ExceptionHandlerList eh = cfe.getExceptionHandlers();
	    for (ExceptionHandlerList n = eh.next();
		 n != null;
		 eh = n, n = eh.next())
		;
	    c = cfe.getCursorAfterMarker(lastPC);
	    Marker m = c.addMarker();
	    eh.insertAfter(enableSaved, m, m, JavaNames.java_lang_Throwable);
	    squirt.restoreEnabled(c);
	    c.addSimpleInstruction(ATHROW);
	    cfe.commit(builder,
		       (char) (code.getMaxStack() + 2),
		       (char) (enabledVar + 1));

	    return;
	}
        if (PragmaNoPollcheck.descendantDeclaredBy(method.getSelector(),
						   curBp) != null) {
            d(BootBase.LOG_POLLCHECKS, "squirtPollChecks() skipping ", method);
            return;
        }
        
	S3ByteCode code=builder.build();
   	ConstantPool rcpb = code.getConstantPool();
	CNT++;
        CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);
	if (!PragmaInline.declaredBy(method.getSelector(), curBp)
	    && !NO_POLL_CHECK_ENTRY) {
	    // Skip pollcheck in prologue if this method shouldn't
	    // have a prologue to begin with.
	    cfe.getCursorBeforeMarker(0).addSimpleInstruction(POLLCHECK);
        } else {
            d(BootBase.LOG_POLLCHECKS, "eliding entry pollcheck on inline method: ", method);
	}
// 	    // Skip pollcheck in prologue if this method shouldn't
// 	    // have a prologue to begin with.
// 	   if (// FIXME::: We are getting rid of pollchecks in leaf methods.. Transaction.the().transactionalMode() &&
// 		   !method.getMode().isNative()) {
// 	       Optimizer.SimpleSequenceVisitor ssv = 	new Optimizer.SimpleSequenceVisitor();
// 	       ssv.run(method);
// 	       if (!ssv.hasBranch) {
// 		   NOB++;
// 		   if(!ssv.hasAlloc) NOA++;
// 		   if (!ssv.hasCall) NOC++;
// 		   if (!ssv.hasCall && !ssv.hasAlloc) NONE++;
// 	       }
// 	    if (!ssv.clean)
// 		cfe.getCursorBeforeMarker(0).addPOLLCHECK();
// 	   } else cfe.getCursorBeforeMarker(0).addPOLLCHECK();
// 	} else {
//             d(BootBase.LOG_POLLCHECKS, "eliding entry pollcheck on inline method: ", method);
// 	}

	if (!NO_POLL_CHECK_LOOP) {
	    InstructionBuffer codeBuf = cfe.getOriginalCode();
	    codeBuf.rewind();
	    pollCheckSquirter_.beginEditing(codeBuf, cfe);
	    try {
		while (codeBuf.hasRemaining()) {
		    pollCheckSquirter_.visitAppropriate(codeBuf.get());
		}
	    } finally {
		pollCheckSquirter_.endEditing();
	    }
	}
        cfe.commit(builder, builder.getMaxStack(), builder.getMaxLocals());
    }
    public static int CNT, NOB, NOC, NOA, NONE;

    public class FinalizerRegistrar extends InstructionEditVisitor
    {
	// FIXME: We really want to register the finalizer just before
	// the constructor invocation, rather than just after the NEW
	// instruction.  However, there is no easy way to determine
	// whether a call to <init> comes from a new expression, or a
	// super(); thingy.  (super() thingies are not statements, but
	// special parts of the constructor body).
	public void visit(Instruction.NEW i) {
	    try {
		Type.Scalar t = (Type.Scalar)
		    ((ConstantPool) cp).resolveClassAt(i.getCPIndex(buf)).getType();
		while (t.getSuperclass() != null) {
		    if (t.getMethod(JavaNames.FINALIZE) != null)
			break;
		    t = t.getSuperclass();
		}
		if (t.getSuperclass() != null) {
		    Cursor c = cfe.getCursorAfterMarker(getPC());
		    c.addSimpleInstruction(DUP);
		    c.addResolvedRefLoadConstant(rtExportsOop_);
		    c.addSimpleInstruction(SWAP);
		   int offset = S3MemberResolver.resolveVTableOffset(rtExportsBpt_, JavaNames.REGISTER_FINALIZER, null);
		    Method m = S3MemberResolver.resolveInstanceMethod(rtExportsBpt_.getType().asCompound(),
			    JavaNames.REGISTER_FINALIZER, null);
		    // This is all very funny.  Usually, we allocate
		    // resolved constants in the meta-data area for
		    // the caller, but here we break with standard
		    // policy.  The standard policy sounds nice at
		    // first glance.  But, we know that the callee
		    // must live as long as all callers, and that all
		    // these damned resolved references are thrown
		    // into a global cache.  I think make() methods
		    // should probably be responsible for choosing the
		    // allocation area, and should use the meta-data
		    // area for the callee.
		    Object r = MemoryPolicy.the().enterMetaDataArea(DomainDirectory.getExecutiveDomain());
		    try {
			ConstantResolvedInstanceMethodref imi = ConstantResolvedInstanceMethodref.make(m, offset, rtExportsBpt_);
			c.addINVOKEVIRTUAL(imi);
		    } finally {
			MemoryPolicy.the().leave(r);
		    }
		}
		// the broken, yet somehow more correct way: rewrite
		// calls to <init> after NEW instructions.  This code
		// rewrites all calls to <init>, which is wrong.
// 		Selector.Method sel = i.getSelector(buf, cp);
// 		if (sel.getNameIndex() != JavaNames.INIT)
// 		    return;
		
// 		ConstantResolvedInstanceMethodref smi =
// 		    ((S3Constants) cp).resolveInstanceMethod(i.getCPIndex(buf));
// 		Type.Scalar t = (Type.Scalar)
// 		    smi.getStaticDefinerBlueprint().getType();
// 		while (t.getSuperclass() != null) {
// 		    if (t.getMethod(JavaNames.FINALIZE) != null)
// 			break;
// 		    t = t.getSuperclass();
// 		}
// 		if (t.getSuperclass() != null) {
// 		    System.err.println("finalizer in " + t);
// 		    // We have successfully evaluated all ctor
// 		    // arguments, now it is safe to call
// 		    // registerFinalizer:
// 		    //
// 		    // roll instance to top
// 		    // dup
// 		    // push RTE reference
// 		    // swap
// 		    // call registerFinalizer
// 		    // roll instance to bottom
// 		    // original bytecode
// 		    int length = i.getArgumentLengthInWords(buf, cp);
// 		    Cursor c = cfe.getCursorBeforeMarker(getPC());

// 		    if (length > 1)
// 			c.addRoll((char) length, (byte) -1);
// 		    c.addSimpleInstruction(DUP);
// 		    c.addResolvedRefLoadConstant(rtExportsOop_);
// 		    c.addSimpleInstruction(SWAP);
// 		    MemberResolver resolver = t.getDomain().getMemberResolver();
// 		    ResolutionInfo ri =
// 			resolver.resolvePublicVirtualMethod(rtExportsBpt_,
// 							    JavaNames.REGISTER_FINALIZER);
// 		    Method m = resolver
// 			.resolveInstanceMethod(rtExportsBpt_.getType().asCompound(),
// 					       JavaNames.REGISTER_FINALIZER,
// 					       null);
// 		    ConstantResolvedInstanceMethodref imi =
// 			ConstantResolvedInstanceMethodref.make(m, ri.getOffset(), rtExportsBpt_);
// 		    c.addINVOKEVIRTUAL(imi);
// 		    if (length > 1)
// 			c.addRoll((char) length, (byte) 1);
// 		}
	    } catch (LinkageException _) {
		// don't report error again.
	    }
	}
    }

    private FinalizerRegistrar finalizerRegistrar = new FinalizerRegistrar();

    /**
     * This visitor does the quickification.
     **/
    public class QuickifyingRewriteVisitor extends RewriteVisitor {
	private Instruction[] iset;
        public QuickifyingRewriteVisitor(S3Blueprint bp, boolean wbar, boolean rbar) {
	    super(bp, wbar, rbar);
	    iset = InstructionSet.SINGLETON.getInstructions();
	}

	public void visit(Instruction.NEW i) {
	    if (!isExecutive_) return; // do NOT quickify in user domain!
	    try {
		int idx = i.getCPIndex(buf);
		getConstants().resolveClassAt(idx);
		cfe.replaceInstruction().addQuickOpcode(NEW_QUICK, (char) idx);
	    } catch (LinkageException e) {
		warn(i, e.getMessage() + " in " + getSelector());
		// no need to expand, size(NEW) == size(NEW_QUICK)
	    }
	}
        public void visit(Instruction.NEWARRAY i) {
	    warn(i, "NEWARRAY should have been rewritten to SINGLEANEWARRAY");
        }
	public void visit(Instruction.SINGLEANEWARRAY i) {
	    try {
		int idx = i.getCPIndex(buf);
		getConstants().resolveClassAt(idx);
		cfe.replaceInstruction().addQuickOpcode(ANEWARRAY_QUICK, (char) idx);
	    } catch (LinkageException e) {
                warn(i, e.getMessage());
		// no need to expand, 
		// size(SINGLEANEWARRAY) == size(ANEWARRAY_QUICK)
            }
	}
        public void visit(Instruction.MULTIANEWARRAY i) {
            try {
		int idx = i.getCPIndex(buf);
		getConstants().resolveClassAt(idx);
		cfe.replaceInstruction().addMULTIANEWARRAY_QUICK((char) idx, i.getDimensions(buf));
            } catch (LinkageException e) {
                warn(i, e.getMessage());
		// no need to expand, 
		// size(MULTIANEWARRAY) == size(MULTIANEWARRAY_QUICK)
            }
        }
        public void visit(Instruction.LDC i) {
            try {
                char idx = (char) i.getCPIndex(buf);
                byte tag = i.getCType(buf, cp);
                switch (tag) {
                    case JVMConstants.CONSTANT_Reference :
                        // already done...
                        break;
                    case JVMConstants.CONSTANT_Integer :
                    case JVMConstants.CONSTANT_Float :
			throw new Error
			    ("Should have been rewritten "
			     + "by NonQuickifyingRewriteVisitor");
                    case JVMConstants.CONSTANT_String :
                    case JVMConstants.CONSTANT_SharedState :
                    case JVMConstants.CONSTANT_Binder :
                        if (isExecutive_) {
                            cfe.replaceInstruction(getPC()).addQuickLdc(
                                getConstants().resolveConstantAt(idx));
                        }
                        break;
                    default :
                        warn(i, "did not expect constant type " + tag);
                }
            } catch (ovm.util.OVMError e) {
                warn(i, e.getMessage());
            }
        }
        public void visit(Instruction.LDC_W i) {
            try {
                char idx = (char) i.getCPIndex(buf);
                byte tag = i.getCType(buf, cp);
                switch (tag) {
                    case JVMConstants.CONSTANT_Reference :
                        // already done...
                        break;
                    case JVMConstants.CONSTANT_Integer :
                    case JVMConstants.CONSTANT_Float :
			throw new Error
			    ("Should have been rewritten "
			     + "by NonQuickifyingRewriteVisitor");
                    case JVMConstants.CONSTANT_String :
                    case JVMConstants.CONSTANT_SharedState :
                    case JVMConstants.CONSTANT_Binder :
                        if (isExecutive_) {
                            cfe.replaceInstruction(getPC()).addQuickLdc(
                                getConstants().resolveConstantAt(idx));
                        }
                        break;
                    default :
                        warn(i, "did not expect constant type " + tag);
                }
            } catch (ovm.util.OVMError e) {
                warn(i, e.getMessage());
            }
        }
        public void visit(Instruction.LDC2_W i) {
	    throw new Error("Should have been rewritten by NonQuickifyingRewriteVisitor");
	}

        public void visit(Instruction.ANEWARRAY i) {
	    warn(i, "ANEWARRAY should have been rewritten to SINGLEANEWARRAY");
        }

         public void visit(Instruction.GETFIELD i) {
            try {
		ConstantResolvedInstanceFieldref ifi = getConstants().resolveInstanceField(i.getCPIndex(buf));
		S3Blueprint bp = (S3Blueprint) ifi.getStaticDefinerBlueprint();
		Selector.Field sel = ifi.asSelector();
		Field rinfo = ifi.getField();
		if (rinfo == null) {
		    warnMissing(i, bp, sel);
		    return;
		}

		Cursor c = cfe.replaceInstruction();
		rinfo.addGetfieldQuick(c);
            } catch (LinkageException e) {
                warn(i, e.getMessage());
		// no need to expand, 
		// size(GETFIELD) == size(GETFIELD_QUICK)
            }
        }
        
        public void visit(Instruction.PUTFIELD i) {
           try {
		ConstantResolvedInstanceFieldref ifi = getConstants().resolveInstanceField(i.getCPIndex(buf));
		S3Blueprint bp = (S3Blueprint) ifi.getStaticDefinerBlueprint();
		Selector.Field sel = ifi.asSelector();
		Field resinfo = ifi.getField();
		if (resinfo == null) {
		    warnMissing(i, bp, sel);
		    return;
		}
		Cursor c = cfe.replaceInstruction();
		if (i instanceof Instruction.PUTFIELD_WITH_BARRIER_REF) {
		    resinfo.addPutfieldQuickWithBarrier(c);
		} else {
		    resinfo.addPutfieldQuick(c);
		}
            } catch (LinkageException e) {
                warn(i, e.getMessage());
		// no need to expand, 
		// size(PUTFIELD) == size(PUTFIELD_QUICK)
            }
        }
        
        public void visit(Instruction.GETSTATIC i) {
            try {
		ConstantResolvedStaticFieldref sfi = getConstants().resolveStaticField(i.getCPIndex(buf));
		Field field = sfi.getField();
		Cursor c = cfe.replaceInstruction();
		if (isExecutive_) c.addLOAD_SHST_FIELD_QUICK(i.getCPIndex(buf));
		else c.addLOAD_SHST_FIELD(i.getCPIndex(buf));
		Selector.Field sel = sfi.asSelector();
		TypeName tn = sel.getDescriptor().getType();
		field.addGetfieldQuick(c);
            } catch (LinkageException e) {
                warn(i, e.getMessage());
		// expand
		padd(iset[LOAD_SHST_FIELD].size(null) + iset[GETFIELD_QUICK].size(null), i);
            }
        }

        public void visit(Instruction.PUTSTATIC i) {
            try {
		ConstantResolvedStaticFieldref sfi = getConstants().resolveStaticField(i.getCPIndex(buf));
		Field field = sfi.getField();
		Selector.Field sel = sfi.asSelector();
		Cursor c = cfe.replaceInstruction();
		if (isExecutive_) c.addLOAD_SHST_FIELD_QUICK(i.getCPIndex(buf));
		else c.addLOAD_SHST_FIELD(i.getCPIndex(buf));
		if (sel.getDescriptor().isWidePrimitive()) c.addRoll((char) 3, (byte) 1);
		else c.addSimpleInstruction(SWAP); // eqv roll(2,1)

		TypeName tn = sel.getDescriptor().getType();
		if (addWriteBarriers && tn != JavaNames.ovm_core_services_memory_VM_Address
			&& tn != JavaNames.ovm_core_services_memory_VM_Word) field.addPutfieldQuickWithBarrier(c);
		else field.addPutfieldQuick(c);
	    } catch (LinkageException e) {
		warn(i, e.getMessage());
		// expand
		padd(iset[LOAD_SHST_FIELD].size(null) + iset[ROLL].size(null) + // assume size(SWAP) < size(ROLL)
			iset[GETFIELD_QUICK].size(null), i);
	    }
        }

        public void visit(Instruction.INVOKEVIRTUAL i) {
            try {
		ConstantResolvedInstanceMethodref vmi = getConstants()
                             .resolveInstanceMethod(i.getCPIndex(buf));
                Selector.Method sel = vmi.asSelector();
                cfe.replaceInstruction().addINVOKEVIRTUAL_QUICK(vmi.getOffset(), sel);
            } catch (LinkageException e) {
                warn(i, e.getMessage());
		padd(iset[INVOKEVIRTUAL_QUICK].size(null), i); // INVOKEVIRTUAL_QUICK
            }
        }

        public void visit(Instruction.INVOKESPECIAL i) {
            try {
		int cpindex = i.getCPIndex(buf);
		ConstantResolvedInstanceMethodref smi = 
		     getConstants().resolveInstanceMethod(cpindex);
                Selector.Method sel = smi.asSelector();
                curIBp.getType(); // FIXME: does this have any sideeffect? if not delete--jv

                boolean isSuperCall = ! smi.isNonVirtual;
                if (isSuperCall) {
		// Case 1. a super call (via vtable)
                    cfe.replaceInstruction().addINVOKESUPER_QUICK(smi.getOffset(), cpindex, sel);
                } else {
                    // Case 2. a constructor / private call (via nvtable)
                    cfe.replaceInstruction().addINVOKENONVIRTUAL2_QUICK(smi.getOffset(), cpindex, sel);
                }
            } catch (LinkageException e) {
                warn(i, e.getMessage());
		// expand
		padd(iset[INVOKENONVIRTUAL2_QUICK].size(null),  i); 
                // assume size(INVOKENONVIRTUAL2_QUICK) == size(INVOKESUPER_QUICK)
            }
        }

        public void visit(Instruction.INVOKEINTERFACE i) {
            try {
		ConstantResolvedInterfaceMethodref imi = getConstants().resolveInterfaceMethod(i.getCPIndex(buf));      
		cfe.replaceInstruction().addINVOKEINTERFACE_QUICK(imi.getOffset(), imi.asSelector());
            } catch (LinkageException e) {
                warn(i, e.getMessage());
                // expand
                padd(iset[INVOKEINTERFACE_QUICK].size(null), i);
            }
        }
        public void visit(Instruction.INVOKESTATIC i) {
	    Selector.Method sel = i.getSelector(buf, cp);
	    int argLength = sel.getDescriptor().getArgumentCount() + sel.getDescriptor().getWideArgumentCount() + 1; //shst
	    int cpindex = i.getCPIndex(buf);
	    try {
		ConstantResolvedStaticMethodref smi = getConstants().resolveStaticMethod(i.getCPIndex(buf));
		Cursor c = cfe.replaceInstruction();
		if (isExecutive_) c.addLOAD_SHST_METHOD_QUICK(cpindex);
		else c.addLOAD_SHST_METHOD(cpindex);
		c.addRoll((char) argLength, (byte) 1);
		c.addINVOKENONVIRTUAL_QUICK(smi.getOffset(), smi.asSelector());
	    } catch (LinkageException e) {
		warn(i, e.getMessage());
		// expand
		padd(iset[LOAD_SHST_METHOD].size(null) + iset[ROLL].size(null) + // assume size(SWAP) < size(ROLL)
			iset[INVOKENONVIRTUAL_QUICK].size(null), i);
	    }
        }
        public void visit(Instruction.INSTANCEOF i) {
            try {
		int idx = i.getCPIndex(buf);
		getConstants().resolveClassAt(idx);
		cfe.replaceInstruction().addQuickOpcode(INSTANCEOF_QUICK, (char) idx);
            } catch (LinkageException e) {
                warn(i, e.getMessage());
		// no need to expand, 
		// size(INSTANCEOF) == size(INSTANCEOF_QUICK)
            }
        }

        public void visit(Instruction.CHECKCAST i) {
            try {
		int idx = i.getCPIndex(buf);
		getConstants().resolveClassAt(idx);
		cfe.replaceInstruction().addQuickOpcode(CHECKCAST_QUICK, (char) idx);
            } catch (LinkageException e) {
                warn(i, e.getMessage());
		// no need to expand, 
		// size(INSTANCEOF) == size(INSTANCEOF_QUICK)
            }

        }

	private void padd(int t, Instruction i) {
	    int s = i.size(buf);

	    if (s != t) {
		Cursor c = cfe.getCursorBeforeMarker(getPC() + s);
		if (s < t) for (int j = s; j < t; j++)
		    c.addSimpleInstruction(NOP);
		else throw new Error("Quick instruction may not be smaller than original bytecode!");		
	    }
	}

    } // end QuickifyingRewriteVisitor

    /**
     * Refactored into the above two visitors.
     **/
    public abstract class RewriteVisitor extends InstructionEditVisitor implements JVMConstants.Opcodes {
	protected final S3Blueprint curBp_;
	final S3Blueprint curIBp;
	protected final boolean isExecutive_;
	protected final boolean isSystemTypeContext_;
	protected final boolean doSystemRewriting_;
	protected final boolean addWriteBarriers;
	protected final boolean addReadBarriers;


	protected RewriteVisitor(S3Blueprint bp, boolean wbar, boolean rbar) {
            assert(bp != null);
	    this.curBp_ = bp;
	    curIBp = (bp.isSharedState() ? (S3Blueprint) bp.getInstanceBlueprint() : bp);

	    S3Domain dom = (S3Domain) curBp_.getType().getDomain();
	    isExecutive_ = dom.isExecutive();
	    isSystemTypeContext_ = isInSystemTypeContext();
	    doSystemRewriting_ = isSystemTypeContext_
		    || InvisibleStitcher.getBoolean("system-rewrite-application-code");
	    addWriteBarriers = wbar;
	    addReadBarriers = rbar;
        }

	protected NativeCallGenerator getNCG() { return ncg; }
	protected S3Blueprint rtExportsBpt() { return rtExportsBpt_; }
	protected Oop rtExportsOop() { return rtExportsOop_; }
	
	private boolean isInSystemTypeContext() {
	    try {
		Type curType = curBp_.getType();
		S3Domain cdom = (S3Domain)curBp_.getDomain();
		Type.Context ctx = cdom.getSystemTypeContext();
		Type aType = ctx.typeFor(curType.getUnrefinedName());
		return aType == curType;
	    } catch (LinkageException e) {
		return false;
	    }
	}

        //--------------------------------------------------------
        //---------------------- helpers -------------------------
        //--------------------------------------------------------

        /** If <em>sel</em> and <em>bp</em> identify a method that declares 
         * (a descendant of) {@link ovm.util.PragmaUnsafe},  check that the
         * current class has declared to implement {@link ovm.util.PragmaUnsafe}. 
         */
        protected boolean allowUnsafe(Selector.Method sel, Blueprint bp) {
            if (true || !isHandlingPragmas) // FIXME re?nable some day
                return true;
            if (PragmaUnsafe.declaredBy(sel, bp)) {
		try {
		    // FIXME: If UnsafeAccess is allowed at all in the
		    // user domain, the marker would appear in
		    // org.ovmj somewhere.  package translation, ala
		    // pragmas, is not being done.
		    Blueprint unsafe = (S3Blueprint) domain.blueprintFor
			(JavaNames.ovm_util_UnsafeAccess,
			 domain.getSystemTypeContext());
		    if (!curBp_.getType().isSubtypeOf(unsafe.getType())) {
			pln("Attempting to access  unsafe method: " + sel + "\n\tfrom:" + curBp_);
			return false;
		    }
		} catch (LinkageException e) {
		    // Must be user domain?
		    pln("Attempting to access  unsafe method: " + sel + "\n\tfrom:" + curBp_);
		    return false;
		}
            }
            return true;
        }

        protected void warnMissing(Instruction i, Blueprint bpt, Selector sel) {
	    if (sel instanceof Selector.Method) {
		Selector.Method msel = (Selector.Method) sel;
		warn(i, JavaFormat._.format(bpt.getName()) + " lacks " + msel.getUnboundSelector() + " in "
			+ buf.getSelector());
	    } else {
		Selector.Field msel = (Selector.Field) sel;
		warn(i, JavaFormat._.format(bpt.getName()) + " lacks " + msel.getUnboundSelector() + " in "
			+ buf.getSelector());
	    }
	}

	protected ConstantPool cp;
	public void beginEditing(InstructionBuffer b, CodeFragmentEditor cfe) {
	    super.beginEditing(b, cfe);
	    cp = (ConstantPool) cfe.getConstantsEditor();
	}
	protected ConstantPool getConstants() {
	    return cp;
	}


    } // end RewriteVisitor

  
    public static void warn(Instruction i, String s)
	throws PragmaTransformCallsiteIR.Ignore
    {
	if ((BootBase.logMask & BootBase.LOG_REWRITE_ERRORS) == 0) return;
	String message = "Rewriting " + i.getName() + ": " + s + " in " + currentSelector_;
	if (warningsPrinted_.get(message) != null) return;
	warningsPrinted_.put(message, message);
	Logger.global.warning(message);
    }

    /**
     * Return true if calls meth are expanded to special bytecode
     * sequences (such as INVOKE_NATIVE, INVOKE_SYSTEM, or pragma
     * transformations).
     **/
    public boolean isMagic(Method meth) {
	Selector.Method msel = meth.getSelector();	
	Blueprint pbp = domain.blueprintFor(meth.getDeclaringType());

	return (PragmaTransformCallsiteIR.descendantDeclaredBy(msel, pbp) != null
		|| ncg.getSpecialSequence(meth) != null);
    }
	
} // end of IRewriter
