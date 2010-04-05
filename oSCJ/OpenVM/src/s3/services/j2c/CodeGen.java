package s3.services.j2c;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import ovm.core.OVMBase;
import ovm.core.repository.TypeName;
import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.ObjectModel;
import ovm.core.repository.JavaNames;
import ovm.core.services.events.PollcheckManager;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.services.format.CxxFormat;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.SpecificationIR.*;
import ovm.services.bytecode.JVMConstants.Throwables;
import ovm.util.BitSet;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.NoSuchElementException;
import ovm.util.Vector;
import org.ovmj.util.Runabout;
import s3.core.domain.MachineSizes;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Method;
import s3.services.j2c.BBSpec.*;
import s3.services.j2c.J2cValue.*;
import s3.services.j2c.SpecInstantiation.LineNumberParser;
import s3.services.transactions.Transaction;
import s3.util.PragmaInline;
import s3.util.PragmaAssertNoExceptions;
import s3.util.PragmaIgnoreExceptions;
import s3.util.PragmaIgnoreSafePoints;

public class CodeGen extends Runabout {
	
    static final boolean omitBoundChecksInExecutiveDomain = true;

    // Print debug messages to standard error at runtime - so far only
    // used in C exceptions
	
    static final boolean DEBUG = false;
    
    static final boolean LATENCY_PROFILING = false;
      // storing a line number after pollcheck for RTGC latency profiling
	
    // Print method name as soon as a method is called
    // (for offline experiments with coverage, tracing, etc)
	
    static final boolean TRACE_METHOD_CALLS = false;
    
    // insert translating barriers on monitors
    // this is most likely (?) not needed, but it's good for debugging
    static final boolean BARRIERS_ON_MONITORS = false;
    
    // Generate code to test live variable analysis by setting all
    // non-live pointer variables to -1.
    static final boolean TEST_LIVENESS = false;

    static final boolean EMIT_LINE_NUMBERS = true; // changed so that I can debug j2c
	
    static final boolean DISABLE_LINE_CONTINUATION = true;

    /**
     * If false, avoid emitting a #line directive for the `next'
     * line.  This should make the .c file smaller, and potentially
     * make compilation faster, but it also makes the .c file less
     * readable.
     **/
    static final boolean EMIT_ALL_LINE_NUMBERS = true;

    /**
     * pc comments don't work well with line numbers and gcc 3.0.2:
     * gcc -E will ignore the newline escape for lines containing
     * comments immediately after #line directives.
     *
     * Anyway, the pc of a push statement is clear from the
     * left-hand-side, and the start pc of a block is clear from its
     * label.
     **/
    static final boolean EMIT_PC_COMMENTS = false;

    static final String TERMINATOR
    = ( (EMIT_LINE_NUMBERS && (!DISABLE_LINE_CONTINUATION))
	? " \\" + System.getProperty("line.separator")
	: System.getProperty("line.separator"));

    static final String rootsField = "ovm_j2cRoots";

    MethodCompiler mc;
    private boolean currentCallsiteIgnoresExceptions = false;
    private boolean currentCallsiteIgnoresSafePoints = false;
    Context ctx;
    Method m;
    PrintWriter w;
    S3Domain dom;
    BBSpec[] blocks;
    int bidx;
    J2cValue synchVar;
    boolean hasGcFrame = false;

    LocalAllocator allocator;
    J2cValue[] allVars;

    // Information about the containing instruction that is used by
    // startCall() to compute safe point data
    BitSet liveRefs;

    // Safe point data produced in startCall(), and dumped out in
    // endCall().
    int nSaved;
    StringBuffer localsList = new StringBuffer();

    static final int BOOT_METHOD = 0;
    static final int THROW_METHOD = 1;
    static final int THROW_WILD_METHOD = 2;
    static final int CODE_FROM_PC_METHOD = 3;

    BBSpec curBlock;
    BBSpec nextBlock() { return blocks[bidx + 1]; }

    // index of last visited try block (starts from 0)
    // used for indexing labels in C exceptions code
    // could this be removed ? isn't there already some other try block id ?
    int lastTryBlock=-1;

    // top level method generator of NullPointerException
    // jumps to abrupt_end_of_method
    boolean usedTopNpeThrower = false;

    // stack of nested try blocks - for exception handling (CExceptions)
    Vector tryBlocks=new Vector();
    class TryBlockInfo {
	public int code; // block identification
	public boolean generatedNpeThrower;
		
	public TryBlockInfo (int code) {
	    this.code = code;
	    this.generatedNpeThrower = false;
	}
    }
	
    // code to be emmited after the end of other method code
    // this is rather a hack, the output is still written 
    // to "w" and using "terminate" and "terminateAndIndent", but
    // "w" can be switched by "setAfterEndPrintWriter" and "setNormalPrintWriter"
	
    ByteArrayOutputStream afterEndStream = new ByteArrayOutputStream();
    PrintWriter afterEndPrintWriter = new PrintWriter( afterEndStream );

    // set to true when emitting "goto _abrupt_end_of_method", so that the
    // label is generated
    boolean usedAbruptEndOfMethod = false;
	
    void ptype(S3Blueprint bp) {
	if (bp.isScalar()) {
	    w.print("HEADER * /* ");
	    w.print(J2cFormat.format(bp));
	    w.print(", ");
	    w.print(bp);
	    w.print(" */");
	} else if (bp.getType() instanceof Type.Array) {
	    w.print(CxxFormat.formatCArray((S3Blueprint.Array)bp));
	    w.print("*");
	    w.print(" /* ");
	    w.print(J2cFormat.format(((S3Blueprint.Array)bp).getComponentBlueprint()));
	    w.print(", ");
	    w.print(bp);
	    w.print(" */");
	} else {
	    w.print(J2cFormat.format(bp));
	    w.print(" /* ");
	    w.print(bp);
	    w.print(" */");
	    if (bp.isReference())
		w.print(" *");
	}
    }

    void ptype(Type t) {
	ptype((S3Blueprint) dom.blueprintFor(t));
    }

    boolean fresh = true;

    // Should plain-old terminate set fresh?  I don't know.  problems
    // arise when w.print is called after terminateAndIndent, and
    // fresh goes out of date
	
    void terminate(String tail) {
	terminate(w, tail);
    }
	
    void terminate(PrintWriter pw, String tail) {
	pw.print(tail);
	pw.print(TERMINATOR);		
    }
	
    void terminateAndIndent(String tail) {
	terminateAndIndent(w, tail);
    }
		
    void terminateAndIndent(PrintWriter pw, String tail) {
	terminate(pw,tail);
	pw.print("\t");
	pw.flush();
	fresh = true;
    }
	
    // these variables are to be used only internally by methods
    // "setAfterEndPrintWriter" and "setNormalPrintWriter"
	
    PrintWriter normalPrintWriter = null;
    boolean normalFresh;
    boolean afterEndFresh = false;
	
    void setAfterEndPrintWriter() {
	if (normalPrintWriter==null) {
	    normalPrintWriter = w;
	    normalFresh = fresh;
	}
	w = afterEndPrintWriter;
	fresh = afterEndFresh;
    }
	
    void setNormalPrintWriter () {
	assert(normalPrintWriter != null);
	w = normalPrintWriter;
	fresh = normalFresh;
    }
	
    boolean inAEBlock = false;


    /**
     * exists to decide that the finally clause should be implemented
     * as a catch of java_lang_Object, which it sometimes is in PARs,
     * or should be implemented as a catch of java_lang_Throwable,
     * which is what we have been doing all along.
     * 
     * You will notice that where there used to be strings like "catch
     * e_java_lang_Object *o" in the rest of this code, there are now
     * calls to this method "finallyReplacement("o", "e", false);
     * 
     * PARBEGIN PAREND
     * 
     * @param varName the variable name for the caught object
     * @param typeContext Executive or User Domain
     * @param translateThrowable
     * @return code that opens a catch clause and binds varName
     * either
     * <blockquote><code>
     * catch (<i>d</i>_java_lang_Object *o) {
     *    <i>d</i>_java_lang_Throwable *<i>varName</i> = (<i>d</d>_java_lang_Throwable *) o;
     * </code></blockquote>
     * or
     * <blockquote><code>
     * catch(<i>d</i>_java_lang_Throwable *<i>varName</i>) {
     * </code></blockquote>
     */
    String finallyReplacement(String varName, String typeContext, boolean translateThrowable) {
	String objectString, throwableString;
	if (typeContext == null) {
	    objectString = J2cFormat.format(ctx.objectBP);
	    throwableString = J2cFormat.format(ctx.throwableBP);
	} else {
	    objectString = typeContext + "_java_lang_Object";
	    throwableString = typeContext + "_java_lang_Throwable";
	}
	if (Transaction.the().transactionalMode() &&
	    translateThrowable ||
	    ((!Transaction.the().isTransMethod(m)) &&
	     (!Transaction.the().isAtomic(m)) &&
	     (!inAEBlock))) {
	    return "catch (" + objectString +  " *o) { " +
		throwableString + " *" + 
		varName + 
		" = (" + throwableString + " *) o;";
	} else {
	    return "catch (" + throwableString + " *" + varName + ") { ";
	}

    }

    int lastLine;
    String lastFile;
    boolean emitLineNumber(LineNumberParser parser, int pc) {
	if (EMIT_LINE_NUMBERS) {
	    int line = parser.getLineNumber(pc);
	    String file = parser.getSourceFile(pc);
	    if (line != lastLine || file != lastFile) {
		w.println("");
		if (EMIT_ALL_LINE_NUMBERS || line != lastLine + 1) {
//					w.print("# ");
		    w.print("/* ");	
		    w.print(line < 0 ? 0 : line);
		    w.print(" \"./");
		    w.print(file);
		    w.print('"');
		    w.println(" enable CPP line numbers in CodeGen.java */");
		}
		lastLine = line;
		lastFile = file;
		return true;
	    }
	}
	return false;
    }

    void emitBarrierSetup() {
	String side=ctx.innerBarrierProf?"INNER":"OUTER";

	w.println("#undef BEGIN_"+side+"_BARRIER");
	w.println("#undef END_"+side+"_BARRIER");

	// can exclude methods from profiling if needed.  I thought it would
	// be needed but right now it looks like it might not be.  hmmm.
	// maybe I'll remove this.  -- FP
	if (ctx.doBarrierProf) {
	    w.println("#define BEGIN_"+side+"_BARRIER __asm__ __volatile__ (\"# begin barrier\")");
	    w.println("#define END_"+side+"_BARRIER __asm__ __volatile__ (\"# end barrier\")");
	} else {
	    w.println("#define BEGIN_"+side+"_BARRIER");
	    w.println("#define END_"+side+"_BARRIER");
	}
    }

    String outerMostExceptionJump;
    boolean compilingVoidMethod;

    /**
     * Return true if it is not safe to use the variable var in the
     * C++ formal parameter list with type formalType.
     * This may happen because we never bother to construct a J2cValue
     * for an unused parameter, or we have inferred a different type
     * for the parameter, or it is a pointer that must be copied to a
     * GCFrame structure.
     **/
    boolean mustRenameParameter(J2cValue var, Type formalType) {
	char fTag = formalType.getUnrefinedName().getTypeTag();
	return (var == null || var.kind != J2cValue.LOCAL_VAR
		|| var.getBlueprint(ctx.domain).getType() != formalType
		// FIXME: this has something to do with
		// VM_Address/VM_Word magic, but I don't know if it is
		// needed.  VM_Address _stack variables end up being
		// declared as pointers rather than ints anyway.
		|| (LocalAllocator.flavorTag[var.flavor] != fTag
		    && fTag != TypeCodes.ARRAY)
		|| (ctx.gcFrames && mc.everLive.get(var.number) && !currentCallsiteIgnoresSafePoints));
    }

    /**
     * Compile a java method
     **/
    CodeGen(MethodCompiler mc, PrintWriter w) {
	this.mc = mc;
	this.w = w;
	this.blocks = mc.block;
	this.ctx = mc.ctx;
	this.m = mc.meth;
	this.allocator = mc.allocator;
	

        if (PragmaIgnoreExceptions.descendantDeclaredBy(
          mc.meth.getSelector(), ctx.blueprintFor(mc.meth.getDeclaringType()) ) != null) {
    			
          this.currentCallsiteIgnoresExceptions = true;	
            // safePointMacros not supported by this mechanism
        } else {
          this.currentCallsiteIgnoresExceptions = false;
        }

        if (PragmaIgnoreSafePoints.descendantDeclaredBy(
          mc.meth.getSelector(), ctx.blueprintFor(mc.meth.getDeclaringType()) ) != null) {
    			
          this.currentCallsiteIgnoresSafePoints = true;	
            // safePointMacros not supported by this mechanism
        } else {
          this.currentCallsiteIgnoresSafePoints = false;
        }

	BitSet storedLocals = mc.storedVariables;
	S3Method meth = mc.meth;
	S3ByteCode cf = mc.code;

	boolean outerTranslate = mc.hasXDCalls;
		
		
	usedAbruptEndOfMethod = false;

	Domain d = meth.getDeclaringType().getContext().getDomain();
	dom = (S3Domain) d;

	if (DEBUG) {
	    if (dom.isExecutive()) {
		w.println("/* EXECUTIVE domain method */");
	    } else {
		w.println("/* USER domain method */");
	    }
	}

	if (omitBoundChecksInExecutiveDomain) {
	  w.println("#undef CHECK_BOUNDS_IF_ENABLED");
	  if (dom.isExecutive()) {
	    w.println("#define CHECK_BOUNDS_IF_ENABLED(_x)");
          } else {
            w.println("#define CHECK_BOUNDS_IF_ENABLED(_x) _x");            
          }
	}

	/* the interrupt handler detection is presently not used... if this stays for a while, it can be removed */
	boolean isInterruptHandler = false;
		
	if (ctx.interruptHandlerBP != null ) {
	    isInterruptHandler = meth.getDeclaringType().isSubtypeOf( ctx.interruptHandlerBP.getType() );
	}
		
	if (isInterruptHandler) {
	    w.println("/* INTERRUPT HANDLER METHOD */");
	}
		
	if (ctx.cExceptions && ctx.safePointMacros) {

	    w.println("#undef CEXCEPTION_JUMP");
	    w.println("#define CEXCEPTION_JUMP goto _abrupt_end_of_method");
	    w.println("#undef CEXCEPTION_METHOD_LEAVE");
			
	    w.print("#define CEXCEPTION_METHOD_LEAVE ");
	    try {
		if (meth.getReturnType() != dom.VOID) {
		    w.println("return 0");
		} else {
		    w.println("return");					}
	    } catch (LinkageException e) {
		throw e.unchecked();
	    }
	}
		
	if (ctx.safePointMacros) {
	    if (!ctx.cExceptions) {
		w.println();
		w.println("#undef ACCURATE_R");
		w.print("#define ACCURATE_R");
		try {
		    if (meth.getReturnType() != dom.VOID) {
			w.print(" val");
		    } else {
			w.print(" void");
		    }
		} catch (LinkageException e) {
		    throw e.unchecked();
		}
		w.println();
		w.println("#undef ACCURATE_R_TYPE");
		w.print("#define ACCURATE_R_TYPE ");
		try { ptype(meth.getReturnType()); }
		catch (LinkageException e) { throw e.unchecked(); }
		w.println();
		if (ctx.noCppExceptions) {
		    w.println("#undef EXCEPTION_JUMP");
		    w.print("#define EXCEPTION_JUMP ");
		    if (m.getMode().isSynchronized() || outerTranslate) {
			outerMostExceptionJump="goto outerTranslate";
		    } else {
			try {
			    if (meth.getReturnType()!=dom.VOID) {
				outerMostExceptionJump="return 0";
			    } else {
				outerMostExceptionJump="return";
			    }
			} catch (LinkageException e) {
			    throw e.unchecked();
			}
		    }
		    w.println(outerMostExceptionJump);
		}
		emitBarrierSetup();
	    }
	}

	if (ctx.cExceptions) {
	    try {
		compilingVoidMethod = (meth.getReturnType()==dom.VOID);
	    } catch (LinkageException e) {
		throw e.unchecked();
	    }
	}

	emitLineNumber(mc.parser, blocks[0].pcDelta[1]);
	// emit method declaration
	w.print("static ");
	if (PragmaInline.declaredBy(meth.getSelector(),
				    ctx.blueprintFor(meth.getDeclaringType()))
	    && !(ctx.noInlineWithSafePoints && ctx.hasSafePoints_p(meth)))
	    w.print("inline ");
	try { ptype(meth.getReturnType()); }
	catch (LinkageException e) { throw e.unchecked(); }
	w.print(" ");
	w.print(J2cFormat.format(meth));
	terminateAndIndent("(");
	liveRefs = new BitSet();
	BitSet argLocals = new BitSet();
	J2cValue thisVar = allocator.findLocal(TypeCodes.OBJECT, 0);
	if (mustRenameParameter(thisVar, meth.getDeclaringType())) {
	    ptype(meth.getDeclaringType());
	    w.print("_self");
	}
	else {
	    argLocals.set(thisVar.number);
	    //w.print(thisVar.getType());
	    ptype(thisVar.getBlueprint(dom));
	    w.print(" ");
	    w.print(thisVar.name);
	    liveRefs.set(thisVar.number);
	}


	for (int i = 0, index=1; i < meth.getArgumentCount(); i++) {
	    terminateAndIndent(",");
	    try {
		Type t = meth.getArgumentType(i);
		int tag = t.getUnrefinedName().getTypeTag();
		S3Blueprint bp = ctx.blueprintFor(t);
		J2cValue var = allocator.findLocal(bp, index++);
		if (tag == TypeCodes.DOUBLE || tag == TypeCodes.LONG)
		    index++;
		if (mustRenameParameter(var, meth.getArgumentType(i))) {
		    ptype(meth.getArgumentType(i));
		    w.print(" _arg" + i);
		} else {
		    argLocals.set(var.number);
		    if (var.flavor == LocalAllocator.REF_VAR) {
			liveRefs.set(var.number);
		    }
		    //w.print(var.getType());
		    ptype(var.getBlueprint(dom));
		    w.print(" ");
		    w.print(var.getName());
		}
	    }
	    catch (LinkageException e) { throw e.unchecked(); }
	}
	terminate(")");
	terminateAndIndent("{");
		
	if (TRACE_METHOD_CALLS) {
	    terminateAndIndent("ubprintf(\":%s\\n\",__func__);");
	}

	if (m.getMode().isSynchronized()) {
	    // FIXME: If local 0 is mutable, we need to save it
	    // somewhere.  On the other hand, if we make synch
	    // explicit in the bytecode, we don't need to handle it
	    // here.
	    synchVar = thisVar;
	    if (synchVar == null)
		throw new Error("this not found!");
	}

	// declare local variable slots and initialize parameter slots
	// declare phi stack variables
	allVars = allocator.getAllVars();
	for (int i = 0; i < allVars.length; i++) {
	    if (allVars[i] != null
		&& (allVars[i].kind != J2cValue.STACK_SLOT
		    || mc.falseLiveness.get(allVars[i].number))
		&& !(ctx.gcFrames && mc.everLive.get(allVars[i].number) && !currentCallsiteIgnoresSafePoints )
		&& !argLocals.get(allVars[i].number))
	    {
		J2cValue v = allVars[i];
		//w.print(v.getType());
		S3Blueprint vbp = v.getBlueprint(dom);
		if (vbp!=null) {
		    ptype(v.getBlueprint(dom));
		} else {
		    //yes, this happens (for "int" variables, for instance)
		    w.print(v.getType());
		}
					
		w.print(" ");
		w.print(v.getName());
		terminateAndIndent(";");
	    }
	}
//		terminate("");
	if (mc.maxMultiArray > 0) {
	    //terminateAndIndent("e_Array<jint, " + mc.maxMultiArray + ">"
	    //		+ " _dims;");
//			terminateAndIndent("\te_Array_dims _dims;");
//			terminateAndIndent("_dims.length = 2;");
			
	    terminateAndIndent("struct { ");
	    w.print("\t");
//			w.print(CxxFormat.format(dom));
	    terminateAndIndent("e_java_lang_Object _parent_;");
	    terminateAndIndent("\tint length;");
	    if (MemoryManager.the().usesArraylets()) {
              assert( mc.maxMultiArray * MachineSizes.BYTES_IN_WORD * 2 < MemoryManager.the().arrayletSize() );
	      terminateAndIndent("\tint *arrayletPtr;");
	    }
	    w.print("\tchar values[ ");
	    w.print("sizeof(jint)*");

	    w.print(mc.maxMultiArray);
	    terminateAndIndent(" ];");
	    terminateAndIndent("} _dims;");
	    w.print("_dims.length = ");
	    w.print(mc.maxMultiArray);
	    terminateAndIndent(";");
	
	    if (MemoryManager.the().usesArraylets()) {
	      terminateAndIndent("_dims.arrayletPtr = (int *)_dims.values;");
	    }
	        
	    // make the object PINNED in the RTGC
	    // a safer bet would probably be to fully initialize it to zeros
	    terminateAndIndent("((int *)&_dims)[0] = 0;");
	    
	    switch (ObjectModel.getObjectModel().getUnforwardedSemantics()) {
	    	case ObjectModel.FWD_SELF:
		    terminateAndIndent("((HEADER *)&_dims)->forward = (struct e_java_lang_Object*)&_dims;");
		    break;
		case ObjectModel.FWD_IGNORE:
		    break;
		default:
		    throw new Error("Unkown forwarding pointer handling);");
	    }
		    
	} else {
//			terminateAndIndent("");
	}

	// FIXME: this is a bizarre mix of Jason's changes to Henderson and
	// Filip's hacking of pointer lists onto the old Henderson approach.  this
	// should eventually get cleaned up so that the GCFrame struct is
	// defined in both Henderson and PtrStack.  Then this code will become
	// a lot simpler.
	if (ctx.gcFrames) {
	    if (mc.everLive.nextSetBit(0) == -1 || currentCallsiteIgnoresSafePoints) {
//		terminateAndIndent("GCFrame &frame = *currentContext->gcTop;");
		terminateAndIndent("GCFrame *frame = currentContext->gcTop;");
	    } else {
		int cnt = 0;
		hasGcFrame = true;
		if (ctx.frameLists) {
//		    terminateAndIndent("struct Frame: GCFrame {");
		    terminateAndIndent("struct Frame {");
		    terminateAndIndent("\tchar _parent_GCFrame_space[ sizeof(GCFrame) ];");
		} else {
		    terminateAndIndent("struct Frame {");
		}
		for (int i = 0; i < allVars.length; i++)
		    if (allVars[i] != null
			&& mc.everLive.get(allVars[i].number))
		    {
			w.print("\t");
//XXX			w.print(allVars[i].getType());
			ptype(allVars[i].getBlueprint(dom));
			w.print(" ");
			w.print(allVars[i].getName());
			terminateAndIndent(";");
			cnt++;
		    }
		terminateAndIndent("};");
		terminateAndIndent("typedef struct Frame Frame;");

		if (ctx.frameLists) {
//		    terminateAndIndent("Frame frame;");
		    terminateAndIndent("Frame frame_alloc;");
		    terminateAndIndent("Frame *frame = &frame_alloc;");
//		    w.print("frame.length = ");
		    w.print("((GCFrame *)frame)->length = ");
		    w.print(cnt);
		    terminateAndIndent(";");
//		    terminateAndIndent("frame.next = currentContext->gcTop;");
		    terminateAndIndent("((GCFrame *)frame)->next = &(currentContext->gcTop);");
//		    terminateAndIndent("currentContext->gcTop = &frame;");
		    terminateAndIndent("currentContext->gcTop = (GCFrame *)frame;");
		} else {
//		    terminateAndIndent("Frame &frame=*((Frame*)(currentContext->gcTop));");
		    terminateAndIndent("Frame* frame= ((Frame*)(currentContext->gcTop));");
//		    terminateAndIndent("currentContext->gcTop=(GCFrame*)((&rame)+1);");
		    terminateAndIndent("currentContext->gcTop=(GCFrame*)(frame+1);");
		}
		
		terminateAndIndent("#ifndef NDEBUG");
		terminateAndIndent("	if ( (int)getPtrStackLimit((jint)currentContext) <= (int)getPtrStackTop((jint)currentContext) ) {");
		terminateAndIndent("		ubprintf(\"Out of pointer stack space at line %d\\n\", __LINE__);");
		terminateAndIndent("		abort();");
		terminateAndIndent("	}");
		terminateAndIndent("#endif");
		
		// FIXME: is this really needed ?
		// NO - if the code was correct before, it should be now as well, as stack
		// allocated variables are not guaranteed to be zeroed
		
		
		/* zero all references and arrays in frames, otherwise the write barrier might try to 
		   mark them (and crash) 
		   let's hope that GCC optimizes these out when the value is not used  
		 */
		/*   
		for (int i = 0; i < allVars.length; i++)
		    if (allVars[i] != null
			&& mc.everLive.get(allVars[i].number))
		    {
		    	S3Blueprint bp = allVars[i].getBlueprint(dom);
		    	
		    	if (bp.isReference()) {
			    	w.print("frame->");
			    	w.print(allVars[i].getName());
			    	w.print(" = ");
			    	w.print("(");
				if (bp.getType() instanceof Type.Array) {
					w.print(CxxFormat.formatCArray((S3Blueprint.Array)bp));
				} else {
					w.print("HEADER");
				}			    	
			    	w.print(" *)");
			    	terminateAndIndent(" 0;");
			}
		    }
		*/
	    }
	}

	if (ctx.safePointMacros
	    && (synchVar != null || outerTranslate
		|| cf.getExceptionHandlers().length > 0)
	    && !ctx.noCppExceptions && !ctx.cExceptions)
	    terminateAndIndent("CATCHPREPARE();");

	if (synchVar != null) {
	    terminateAndIndent("int monitorEntered=0;");
	}

	if ((synchVar != null || outerTranslate) && !ctx.noCppExceptions && !ctx.cExceptions)
	    terminateAndIndent("try {");

	if (thisVar != null && !argLocals.get(thisVar.number)) {
	    visit(thisVar);
	    terminateAndIndent(" = _self;");
	    liveRefs.set(thisVar.number);
	}
	if (synchVar != null && synchVar != thisVar) {
	    liveRefs.set(synchVar.number);
	    visit(synchVar);
	    if (synchVar.getBlueprint(ctx.domain) == ctx.OopBP)
		terminateAndIndent(" = (e_ovm_core_domain_Oop *) _self;");
	    else
		terminateAndIndent(" = _self;");
	}
	for (int i = 0, index = 1; i < meth.getArgumentCount(); i++) {
	    try {
		Type t = meth.getArgumentType(i);
		int tag = t.getUnrefinedName().getTypeTag();
		S3Blueprint bp = ctx.blueprintFor(t);
		J2cValue var = allocator.findLocal(bp, index++);
		if (tag == TypeCodes.DOUBLE || tag == TypeCodes.LONG)
		    index++;
		// Unused parameters may have been removed from
		// allVars, but are still available through the tag *
		// index map used by findLocal.  C++ variables have
		// not been declared, so we had better not assign to
		// them!
		if (var == null || allVars[var.number] == null)
		    continue;       // unused parameter                    
		if (!argLocals.get(var.number)) {
		    visit(var);
		    w.print((bp == ctx.VM_AddressBP || bp == ctx.VM_WordBP)
			    ? " = (jint) _arg"
			    : " = _arg");
		    w.print(i);
		    terminateAndIndent(";");
		    if (var.flavor == LocalAllocator.REF_VAR)
			liveRefs.set(var.number);
		}
	    }
	    catch (LinkageException e) { throw e.unchecked(); }
	}

	if (ctx.initGCFrame && hasGcFrame) {
	    terminateAndIndent("");
	    // Initialize all fields of the GCFrame that have not
	    // already been initialized
	    for (int i = 0; i < allVars.length; i++)
		if (allVars[i] != null
		    // liveRefs is used to keep track of whether
		    // parameters are stored in their final
		    // locations.  
		    && !liveRefs.get(allVars[i].number)
		    && mc.everLive.get(allVars[i].number) 
		    && !currentCallsiteIgnoresSafePoints )
		{
		    visit(allVars[i]);
		    terminateAndIndent(" = 0;");
		}
	}

	// Null out any variables that we may use in a safe point
	// before init
	for (int i = mc.falseLiveness.nextSetBit(0);
	     i != -1;
	     i = mc.falseLiveness.nextSetBit(i+1)) {
	    visit(allVars[i]);
	    terminateAndIndent(" = 0 /* FALSE LIVE*/;");
	}

	if (m.getMode().isSynchronized()) {
	    if (!(outerTranslate || dom.isExecutive())
		&& !ctx.noCppExceptions && !ctx.cExceptions)
		w.print("try { ");
	    startCall(true, null, ctx.monitorEnter);
	    if (ctx.safePointMacros) {
		usedAbruptEndOfMethod = true;
		w.print("PACKCALL(");
	    }
	    w.print(J2cFormat.format(ctx.monitorEnter));
	    if (ctx.safePointMacros)
		w.print(",2,");
	    else
		w.print("(");
	    visitAppropriate(ctx.CSAvalue);
	    w.print(", ");
	    
	    if (BARRIERS_ON_MONITORS) {
	      readBarrierStart();
            }
	    compileWithType(synchVar, ctx.OopBP);
	    if (BARRIERS_ON_MONITORS) {
	      readBarrierEnd();
            }
	    
	    w.print(")");

	    endCall();

	    w.print("; monitorEntered=1; ");
	    if (DEBUG) {
		terminateAndIndent("ubprintf(\"Entered monitor (implicitly) at %s:%d\\n\",__FILE__,__LINE__);");				
	    }

	    if (outerTranslate || dom.isExecutive() || ctx.noCppExceptions || ctx.cExceptions )
		terminateAndIndent("");
	    else {
		terminateAndIndent(" }");
		w.print(finallyReplacement("e", "e", true));
		startCatch(0 , "");
		w.print("j2c_throw(");
		startCall(true, ctx.OopBP, ctx.translateThrowable);
		if (ctx.safePointMacros)
		    w.print("PACKCALL(");
		w.print(J2cFormat.format(ctx.translateThrowable));
		if (ctx.safePointMacros) 
		    w.print(",2,");
		else
		    w.print("(");
		visitAppropriate(ctx.CSAvalue);
		w.print(", e)");
		endCall();
		terminateAndIndent("); }");
	    }
			
	}

	terminate("");
	// emit code for basic blocks.  Wrap try blocks in try/catch.
	for (bidx = 0; bidx < blocks.length; bidx++) {
	    curBlock = blocks[bidx];
	    int pc = curBlock.startPC;
	    int dix = 0;
	    int j = 0;

	    emitLineNumber(mc.parser, pc);
	    w.print("_pc");
	    w.print(pc);
	    w.print(": ");
	    startTry(curBlock, curBlock.innerMostTry);
	    terminateAndIndent("{");
	    for ( ; j < curBlock.code.length; j++) {
		if (curBlock.pcDelta[dix] == j) {
		    pc = curBlock.pcDelta[dix+1];
		    if (emitLineNumber(mc.parser, pc))
			w.print("\t");
		    if (EMIT_PC_COMMENTS)
			terminateAndIndent("/* PC " + pc + " */");
		    dix += 2;
		}
		Expr e = curBlock.code[j];
		if (e == null)
		    continue;
					
		String rest = "";					
		if (e.dest != null) {
		    if (e.dest.kind == J2cValue.STACK_SLOT
			&& !(hasGcFrame && mc.everLive.get(e.dest.number) && !currentCallsiteIgnoresSafePoints )
			&& !mc.falseLiveness.get(e.dest.number)) {
			//w.print(e.dest.getType());
			ptype(e.dest.getBlueprint(dom));
			w.print(" ");
		    }
		    if (ctx.gcFrames && mc.everLive.get(e.dest.number) && !currentCallsiteIgnoresSafePoints )
			w.print("frame->");		    
//			w.print("frame.");
		    w.print(e.dest.getName());
		    w.print(" = ");

			if (e.dest instanceof J2cReference) {
			    S3Blueprint destbp = e.dest.getBlueprint(dom);
					
			    if (destbp.isScalar()) {
				w.print("(HEADER *) (");
				rest=")";
			    } else if (destbp.getType() instanceof Type.Array) {
				w.print(" ((");
				w.print(CxxFormat.formatCArray((S3Blueprint.Array)destbp));
				w.print(" *)");
				rest=")";
			    }
			}
		}
		fresh = false;
//				w.print("/*vaesta*/");
		visitAppropriate(e);
//				w.print("/*vaeend*/");
		w.print(rest);
				
		if (!fresh)
		    terminateAndIndent(";");
	    }
	    terminate("}");
	    endTry(curBlock, curBlock.innerMostTry);
	}

	if (ctx.cExceptions) {} else
	    if (ctx.noCppExceptions) {
		// could be here either because of user exception or executive
		// exception.  cannot be here because of normal execution.
		// if there is a monitor to release, the first order of business
		// is to release it - though this may by itself throw some
		// executive exception.  the next step afterwards is to translate
		// exceptions if we are in UD and there was a chance for an ED
		// exception.  once that is done we just set the exception bit
		// again and return.
		if (synchVar!=null || outerTranslate) {
		    w.println("outerTranslate:");
		    // need to save the exception-related global stuff since
		    // it might get clobbered inside of monitorExit()
		    w.println("    int myExcDom=curExcDom;");
		    w.println("    void *myExc=curExc;");
		    w.println("#undef EXCEPTION_JUMP");
		    w.print("#define EXCEPTION_JUMP ");
		    if (!dom.isExecutive()) {
			w.println("myExcDom=curExcDom; myExc=curExc; goto outerTranslate2");
		    } else {
			try {
			    if (meth.getReturnType()!=dom.VOID) {
				w.println("return 0");
			    } else {
				w.println("return");
			    }
			} catch (LinkageException e) {
			    throw e.unchecked();
			}
		    }
		    if (synchVar!=null) {
			w.println("    accurate::counterClearException();");
			w.print("    if (monitorEntered) { CALL(PACKCALL(");
			w.print(J2cFormat.format(ctx.monitorExit));
			w.print(",2,");
			visitAppropriate(ctx.CSAvalue);
			w.print(", ");
			//compileWithType(synchVar, ctx.OopBP);
			if (BARRIERS_ON_MONITORS) {
			  readBarrierStart();
                        }
			visitAppropriate(synchVar);
			if (BARRIERS_ON_MONITORS) {
			  readBarrierEnd();
                        }
			w.print("),\t1,myExc); }");
		    }
		    if (!dom.isExecutive()) {
			w.println("outerTranslate2:");
			w.println("    if (myExcDom==0) {");
			w.println("        accurate::counterClearException();");
			w.print("        curExc=");
			startCall(false,ctx.OopBP,ctx.translateThrowable);
			w.print("PACKCALL(");
			w.print(J2cFormat.format(ctx.translateThrowable));
			w.print(",2,");
			visitAppropriate(ctx.CSAvalue);
			w.print(", (e_java_lang_Throwable*) myExc)");
			endCall();
			w.println(";");
			w.print("        curExcDom=");
			w.print((int)dom.getUID());
			w.println(";");
			w.println("    }");
		    }
		    w.println("    accurate::counterSetException();");
		    try {
			if (meth.getReturnType()!=dom.VOID) {
			    w.println("    return 0;");
			} else {
			    w.println("    return;");
			}
		    } catch (LinkageException e) {
			throw e.unchecked();
		    }
		}
	    } else {
		if (outerTranslate || synchVar != null)
		    terminateAndIndent("}");
		if (outerTranslate) {
		    terminateAndIndent(finallyReplacement("e", "e", false));
		    if (synchVar != null)
			startCatch(1, ", "+synchVar.getName());
		    else
			startCatch(0, "");
		    if (synchVar != null) {
			// FIXME: convert to use counter exceptions.  this try is
			// for translate throwable
			w.print("if (monitorEntered) { try { monitorEntered=0; ");
			// montitorExit must keep `e' alive
			if (ctx.safePointMacros)
			    w.print("CALL(PACKCALL(");
			w.print(J2cFormat.format(ctx.monitorExit));
			if (ctx.safePointMacros) 
			    w.print(",2,");
			else
			    w.print("(");
			visitAppropriate(ctx.CSAvalue);
			w.print(", ");
			//compileWithType(synchVar, ctx.OopBP);
			
			if (BARRIERS_ON_MONITORS) {
			  readBarrierStart();
                        }
			visitAppropriate(synchVar);
			if (BARRIERS_ON_MONITORS) {
			  readBarrierEnd();
                        }
			
			w.print(")");
			if (ctx.safePointMacros)
			    w.print(",\t1, e)");
			w.print(";");
			if (DEBUG) {
			    terminateAndIndent("ubprintf(\"Exited monitor (implicitly) at %s:%d \\n\",__FILE__,__LINE__);");
			}					
			terminateAndIndent(" }");
			w.print(finallyReplacement("e", "e", true));
			startCatch(0, "");
			w.print("j2c_throw(");
			// translateThrowable keeps nothing alive
			startCall(false, ctx.OopBP, ctx.translateThrowable);
			if (ctx.safePointMacros)
			    w.print("PACKCALL(");
			w.print(J2cFormat.format(ctx.translateThrowable));
			if (ctx.safePointMacros) 
			    w.print(",2,");
			else
			    w.print("(");
			visitAppropriate(ctx.CSAvalue);
			w.print(", e)");
			endCall();
			terminateAndIndent("); } }");
		    }
		    w.print("j2c_throw(");
		    // translateThrowable keeps nothing alive
		    startCall(false, ctx.OopBP, ctx.translateThrowable);
		    if (ctx.safePointMacros)
			w.print("PACKCALL(");
		    w.print(J2cFormat.format(ctx.translateThrowable));
		    if (ctx.safePointMacros) 
			w.print(",2,");
		    else
			w.print("(");
		    visitAppropriate(ctx.CSAvalue);
		    w.print(", e)");
		    endCall();
		    terminateAndIndent(");");
		    terminateAndIndent("}");
		}
		if (synchVar != null) {
		    // w.print("catch (");
		    // w.print(J2cFormat.format(ctx.throwableBP));
		    // terminateAndIndent(" *ex) { ");
		    terminateAndIndent(this.finallyReplacement("ex", null, false));
		    startCatch(1, ", "+synchVar.getName());
		    // FIXME: convert to use counter exceptions.  this try is
		    // for translate throwable
		    if (!dom.isExecutive())
			w.print("try { ");
		    w.print("if (monitorEntered) { monitorEntered=0; ");
		    if (ctx.safePointMacros)
			w.print("CALL(PACKCALL(");
		    w.print(J2cFormat.format(ctx.monitorExit));
		    if (ctx.safePointMacros)
			w.print(",2,");
		    else
			w.print("(");
		    visitAppropriate(ctx.CSAvalue);
		    w.print(", ");
		    // compileWithType(synchVar, ctx.OopBP);
		    if (BARRIERS_ON_MONITORS) {
		      readBarrierStart();
                    }
		    visitAppropriate(synchVar);
		    if (BARRIERS_ON_MONITORS) {
		      readBarrierEnd();
                    }
		    w.print(")");
		    if (ctx.safePointMacros)
			w.print(",\t1, ex)");
		    w.print(";");
		    if (DEBUG) {
			terminateAndIndent("ubprintf(\"Exited monitor (implicitly-1) at %s:%d \\n\",__FILE__,__LINE__);");
		    }				
		    if (dom.isExecutive())
			terminateAndIndent(" }");
		    else {
			terminateAndIndent(" } }");
			w.print(finallyReplacement("e", "e", true));
			startCatch(0, "");
			w.print("j2c_throw(");
			// translateThrowable keeps nothing alive
			startCall(false, ctx.OopBP, ctx.translateThrowable);
			if (ctx.safePointMacros)
			    w.print("PACKCALL(");
			w.print(J2cFormat.format(ctx.translateThrowable));
			if (ctx.safePointMacros) 
			    w.print(",2,");
			else
			    w.print("(");
			visitAppropriate(ctx.CSAvalue);
			w.print(", e)");
			endCall();
			terminateAndIndent("); }");
		    }
		    w.print("j2c_throw(");
		    terminateAndIndent("ex);");
		    terminateAndIndent("}");
		}
	    }

	// flush code generated for the end of the method
	// this includes the C exceptions dispatch blocks
		
	afterEndPrintWriter.flush();
	boolean usedEndOfMethod = false;
		
		
	// the afterEndStream is so far used only by C exceptions,
	// but can be used by other code as well

	if ((afterEndStream.size()!=0) || usedTopNpeThrower ) {
	    terminateAndIndent("\t /* likely not reached */  ");		
	    terminate("goto _end_of_method;");
	    usedEndOfMethod = true;
	    if (afterEndStream.size()!=0) {
		w.print(afterEndStream.toString());
	    }
	}
		
	if (ctx.cExceptions) {

	    // generate top npe thrower
	    if (usedTopNpeThrower) {
			
		// generating NPE throwers here breaks the line numbers macros
		// (the exception would not be reported to be thrown on the correct line)
		// if needed, this can be fixed by a global variable that would hold __LINE__,
		// and then some tricks with the exception backtrace

		terminateAndIndent("_top_npe_thrower: {");

		// w.print("cur_exc = ");
		terminateAndIndent("cur_exc = 0;");
		if (ctx.safePointMacros && ctx.catchPointsUsed) {
		    terminateAndIndent("accurate::counterClearException();");
		}
		w.print("HEADER *npe_ex =");
		w.print("(");
		ptype(ctx.NullPointerExceptionBP);
		w.print(")");
		w.print(J2cFormat.format(ctx.makeThrowable));
		if (false && ctx.safePointMacros) {
		    w.print(",4,");
		} else {
		    w.print("(");
		}
		visitAppropriate(ctx.CSAvalue);
		w.print(", ");
		w.print(Throwables.NULL_POINTER_EXCEPTION);
		terminateAndIndent(",0,0);");
				
		if (ctx.safePointMacros) {
		    terminateAndIndent("CEXCEPTION_PRECISE_GCE_CHECK;");
		}
		terminateAndIndent("assert(cur_exc==0);");			

		terminateAndIndent("cur_exc = npe_ex;");
	
		w.print("cur_exc_dom = ");
		if (dom.isExecutive()) {
		    terminateAndIndent("0;");
		} else {
		    terminateAndIndent("1;");
		}		
		if (ctx.safePointMacros && ctx.catchPointsUsed) {
		    terminateAndIndent("accurate::counterSetException();");
		}
		//	terminateAndIndent("goto _abrupt_end_of_method;");				
		terminate("} /* _top_npe_thrower */");
		terminate("");
		usedAbruptEndOfMethod = true;
				

	    }
	    /* if you insert anything here, uncomment the goto above */
	    if (usedAbruptEndOfMethod) {
			
		terminateAndIndent("_abrupt_end_of_method:");
		if (DEBUG) {
		    terminateAndIndent("ubprintf(\"Reached abrupt end of method at %s:%d, exception is %x, exception domain is %x\\n\",__FILE__,__LINE__,cur_exc,cur_exc_dom);");
		}					
				
		if (ctx.safePointMacros && !ctx.catchPointsUsed) {
		    terminateAndIndent("assert(cur_exc!=(HEADER *)&accurate::GCException_singleton);");
		}
				
		if (synchVar!=null) {
					
		    // must exit the monitor if it was entered
					
		    terminateAndIndent("if (monitorEntered) {");
		    if (DEBUG) {
			terminateAndIndent("\tubprintf(\"Releasing monitor at this method..\\n\");");
		    }
		    terminateAndIndent("\tHEADER *current_exc = (HEADER *)cur_exc;");
		    terminateAndIndent("\tint current_exc_dom = cur_exc_dom;");
		    terminateAndIndent("\tcur_exc = 0;");
		    if (ctx.safePointMacros && ctx.catchPointsUsed) {
			terminateAndIndent("accurate::counterClearException();");
		    }
		    w.print("\t");
		    w.print(J2cFormat.format(ctx.monitorExit));
		    w.print("(");
		    visitAppropriate(ctx.CSAvalue);
		    w.print(", ");
		    // compileWithType(synchVar, ctx.OopBP);
		    visitAppropriate(synchVar);
		    terminateAndIndent(");");	
					
		    if (DEBUG) {
			terminateAndIndent("\tubprintf(\"Exited monitor (implicitly-2) at %s:%d \\n\",__FILE__,__LINE__);");
		    }
					
		    if (ctx.safePointMacros) {
			terminateAndIndent("\tCEXCEPTION_PRECISE_GCE_CHECK;");
		    }
					
		    terminateAndIndent("\tcur_exc = current_exc;");
		    terminateAndIndent("\tcur_exc_dom = current_exc_dom;");
		    if (ctx.safePointMacros && ctx.catchPointsUsed) {
			terminateAndIndent("accurate::counterSetException();");					
		    }
		    terminateAndIndent("}");
		}
	    }
				
	    try {
		if (meth.getReturnType()!=dom.VOID) {
		    w.println("return 0;");
		} else {
		    w.println("return;");
		}
	    } catch (LinkageException e) {
		throw e.unchecked();
	    }
	}
		
	if (usedEndOfMethod) {		
	    terminate("_end_of_method: ;"); // the semicolon is here to make GCC happy
	}
		
	terminate("}");
	// terminate("");
    }

    /**
     * Compile global helper functions.  Mostly, these functions are
     * used to assign well-known names to java methods (whose c++
     * names have a weird suffix).
     **/
    CodeGen(final PrintWriter w, J2cImageCompiler cmp) {
	this.w = w;
	ctx = Context.findContext(cmp, (S3Domain) cmp.getExecutiveDomain());

	S3Blueprint cfBP;
	S3Blueprint lrtBP = null;
	try {
	    S3Domain exec = (S3Domain) cmp.getExecutiveDomain();
	    TypeName.Scalar cfN
		= RepositoryUtils.makeTypeName("s3/services/j2c",
					       "J2cCodeFragment");
	    S3Blueprint _cfBP = (S3Blueprint)
		exec.blueprintFor(cfN.asCompound(),
				  exec.getSystemTypeContext());
	    cfBP = (S3Blueprint) _cfBP.getSharedState().getBlueprint();
	    if (ctx.safePointMacros) {
		TypeName.Scalar lrtTN =
		    RepositoryUtils.makeTypeName("s3/services/memory/precise",
						 "PreciseReferenceIterator");
		lrtBP = (S3Blueprint)
		    exec.blueprintFor(lrtTN.asCompound(),
				      exec.getSystemTypeContext());
		lrtBP = (S3Blueprint) lrtBP.getSharedState().getBlueprint();
	    }
	} catch(LinkageException e) { throw e.unchecked(); }


	// void boot() { S3Executive_startup_xxxx(); }
	Object mainObject = cmp.getHeader("mainObject");
	S3ByteCode mainBC = (S3ByteCode) cmp.getHeader("mainMethod");
	Oop mainObjectOops = VM_Address.fromObject(mainObject).asOop();
	S3Blueprint mainBP= (S3Blueprint) mainObjectOops.getBlueprint();
	Type.Scalar mainType = mainBP.getType().asScalar();
	S3Method mainMethod
	    = (S3Method) mainType.getMethod(mainBC.getSelector());
	w.println("void boot() {");
	// OK.  We could build an expression for the call, but
	// it would be a lot easier to simply generate the string
	// here.
	w.print("\t");
	w.print(J2cValue.makeNonvirtualReference(mainMethod).getName());
	w.println("((HEADER *)&mainObject);");
	w.println("}");

	// domain for header
	w.println("jint domainForBlueprint(S3Blueprint *bp) {");
	w.println("    switch(bp->ovm_ctxNumber) {");
	for (int i=0;i<=DomainDirectory.the().maxContextID();++i) {
	    Type.Context c=DomainDirectory.the().getContext(i);
	    if (c!=null) {
		w.println("    case "+i+": return "+
			  ((int)c.getDomain().getUID())+";");
	    }
	}
	w.println("    default: abort();");
	w.println("    }");
	w.println("}");

	// j2c_throw(HEADER *ex)
	final TypeName.Scalar tName = JavaNames.java_lang_Throwable;
	// Match return type of CSA.processThrowable.
		
	if (ctx.noCppExceptions) {
	    w.println("HEADER *getCurrentException() { return (HEADER*)curExc; }");
	    w.println("e_java_lang_Error *j2c_throw(HEADER *o) { setExc((void*)o,domainForBlueprint(HEADER_BLUEPRINT(o))); }");
	    w.println("#define j2c_throw(_o) ({ HEADER *o=_o; setExc((void*)o,domainForBlueprint(HEADER_BLUEPRINT(o))); EXCEPTION_JUMP; (e_java_lang_Error*)0; })");

	} else 	if (ctx.cExceptions) {

	    // although j2c_throw is now translated to a macro
	    // in the code generated by j2c, it could be
	    // called from other modules ; if called directly, it would not work, since
	    // it just sets the exception, but returns normally
			
	    terminateAndIndent("e_java_lang_Error *j2c_throw(HEADER *_o) {");
	    terminateAndIndent("cur_exc=(HEADER *)_o;");
	    terminateAndIndent("cur_exc_dom = domainForBlueprint(HEADER_BLUEPRINT(_o));");
	    if (ctx.safePointMacros && ctx.catchPointsUsed) {
		terminateAndIndent("accurate::counterSetException();");
	    }
	    if (DEBUG) {
		terminateAndIndent("ubprintf(\"j2c_throw setting exception %x at %s:%d.\\n\",cur_exc,__FILE__,__LINE__);");
	    }				
	    w.println(" }");
	} else {
			
	    w.println("e_java_lang_Error *j2c_throw(HEADER *o) {");
	    if (false) w.println("\tubprintf(\"in j2c_throw: %p\\n\",o);");
	    w.println("\tswitch (HEADER_BLUEPRINT(o)->ovm_ctxNumber << 16 | HEADER_BLUEPRINT(o)->ovm_number) {");
	    cmp.new BlueprintWalker() {
		    S3Blueprint throwableBP;

		    public void beforeDomain(S3Domain d) {
			Type.Context sCtx = d.getSystemTypeContext();
			try {
			    throwableBP
				= (S3Blueprint) d.blueprintFor(tName,sCtx);
			}
			catch (LinkageException e) { throw e.unchecked(); }
		    }
		    public void walkBlueprint(S3Blueprint bp) {
			if (bp.isSubtypeOf(throwableBP) || 
			    bp.asTypeName().equals(Transaction.ED_ABORTED_EXCEPTION) ||
			    bp.asTypeName().equals(Transaction.ABORTED_EXCEPTION)) { //PARBEGIN PAREND FIXME...
			    w.print("\tcase ");
			    w.print(bp.getCID() << 16 | bp.getUID());
			    w.print(":  ");
			    w.print("THROW");
			    w.print("((");
			    w.print(CxxFormat.format(bp));
			    w.println(" *) o);");
			}
		    }
		}.forAllDomains();
	    // the unreachable abort() suppresses noreturn warnings
	    w.println("\tdefault: assert(!\"bad throwable\"); abort();");
	    w.println("}");
	    w.println("}");
	}

	// throwWilcardException(HEADER *ex)
	int idx = Context.getUTF("throwWildcardException");
	S3Method met = Context.findMethodObject(cfBP, idx);
	w.println("void throwWildcardException(HEADER *oop) {");
	w.print("\t");
	w.print(J2cValue.makeNonvirtualReference(met).getName());
	w.println("(0, (HEADER *) oop);");
	// the unreachable abort() suppresses noreturn warnings
	//w.println("\tassert(!\"unreachable\"); abort();");
	w.println("}");


	// generateThrowable(int)
	idx = Context.getUTF("generateThrowable");
	met = Context.findMethodObject(cfBP, idx);
	w.println("void generateThrowable(int code) {");
	if (DEBUG) {
	    w.println("\tubprintf(\"Generate throwable called for code %d\\n\",code);");
	}
	w.println("\tsigprocmask(SIG_SETMASK, &j2cNormalMask, 0);");
	if (DEBUG) {
	    w.println("\tubprintf(\"Generate throwable called for code %d (1)\\n\",code);");
	}	
	w.print("\t");
	w.print(J2cValue.makeNonvirtualReference(met).getName());
	w.println("(0, code);");
	// the unreachable abort() suppresses noreturn warnings
	//w.println("\tassert(!\"unreachable\"); abort();");
	w.println("}");

	// assertAddressValid(void*)
	idx = Context.getUTF("assertAddressValid");
	met = Context.findMethodObject(cfBP, idx);
	w.println("void assertAddressValid(void *ptr) {");
	w.print("\t");
	w.print(J2cValue.makeNonvirtualReference(met).getName());
	w.println("(0, (e_ovm_core_services_memory_VM_1Address*)ptr);");
	w.println("}");

	// codeFromPc(jint)
//		idx = Context.getUTF("fromPC");
//		met = Context.findMethodObject(cfBP, idx);
//		w.println("e_s3_services_j2c_J2cCodeFragment *" +
//		"codeFromPC(jint pc) {");
//		w.print("\treturn ");
//		w.print(J2cFormat.format(met));
//		w.println("(0, pc);");
//		w.println("}");

	if (ctx.safePointMacros) {
	    // int nextThreadToWalk()
	    idx = Context.getUTF("nextThreadToWalk");
	    met = Context.findMethodObject(lrtBP, idx);
	    w.println("jint nextThreadToWalk() {");
	    w.print("\treturn ");
	    w.print(J2cValue.makeNonvirtualReference(met).getName());
	    w.println("(0);");
	    w.println("}");
	}
    }

    /**
     * Generate per-domain helper methods, currently the just
     * &lt;domain&gt;_signalEvent().  This noninlinable function is
     * called whenever CHECK_EVENTS() returns true, and is responsible
     * for setting up native-level event handling state and calling
     * pollingEventHook() on the appropriate CoreServicesAccess object
     **/
    public CodeGen(PrintWriter w, Context ctx) {
	this.w = w;
	this.ctx = ctx;
	this.dom = ctx.domain;
	if (dom.isExecutive()) {
	    w.println();
	    if (ctx.safePointMacros) {
		if (!ctx.cExceptions) {
		    w.println("#undef ACCURATE_R");
		    w.println("#define ACCURATE_R void");
		    w.println("#undef ACCURATE_R_TYPE");
		    w.println("#define ACCURATE_R_TYPE void");
		}
		emitBarrierSetup();
	    }
	    w.print("jint ");
	    w.println("j2c_signalEvent() {");
	    if (ctx.safePointMacros) {
		w.println("\taccurate::resetCounterSignal();");
	    }
	    w.println("\tif (!"+
		      PollcheckManager.getSettings().slowPathInC()+
		      ") return 0;");
	    MethodReference hook =
		J2cValue.makeNonvirtualReference(ctx.pollingEventHook);
	    // FIXME: change to use counter exceptions.  this try is for
	    // aborting on exceptions that happen when handling events.
	    // FIXME: Abort removed because it was interfering with PARs
	    w.print("\t");
	    w.print(hook.getName());
	    w.print("(");
	    visitAppropriate(ctx.CSAvalue);
	    terminateAndIndent(");");
	    w.println("return 1;");
	    w.println("}");
	}
    }

    void startTry(BBSpec block, TryBlock t) {
	if (t != null && t.firstBlock == block) {

	    // for PARs
	    for (TryBlock.Handler h = t.handler; h != null; h = h.next) {
		if (h.bp.equals(ctx.AbortedExceptionBP) || 
		    h.bp.equals(ctx.EDAbortedExceptionBP))
		    this.inAEBlock = true;
	    }
	    startTry(block, t.outer);
	    terminate("");
			
	    if (ctx.cExceptions) {
			
		// register the try block (assign an id to it)
		emitLineNumber(mc.parser, t.startPC);
		lastTryBlock++;
		tryBlocks.add(new TryBlockInfo(lastTryBlock));
		terminateAndIndent("");
		if (DEBUG) {
		    terminateAndIndent("/* registered start of try block no. "+lastTryBlock+" */");
		}
		if (ctx.safePointMacros) {
		    terminateAndIndent("#undef CEXCEPTION_JUMP");
		    w.print("#define CEXCEPTION_JUMP ");
				
		    if (dom.isExecutive()) {
			w.print("goto _try_dispatch_");
		    } else {
			w.print("goto _try_dispatch_translation_");
		    }
				
		    w.print(lastTryBlock);					
		    terminateAndIndent("");
		}

	    } else {
		if (ctx.noCppExceptions) {
		    w.println(); // get outside of last line
		    w.println("#undef EXCEPTION_JUMP");
		    w.print("#define EXCEPTION_JUMP goto tb");
		    emitLineNumber(mc.parser,block.startPC);
		    w.println(t.id);
		} else {
		    emitLineNumber(mc.parser, t.startPC);
		    w.print("    try {");
		    if (t.hasXDCalls) {
			terminate("");
			w.print("      try {");
		    }
		}
	    }
	}
    }

    public static int tryCode = 0;
    void endTry(BBSpec block, TryBlock t) {
	if (t != null && t.lastBlock == block) {
	    if (ctx.noCppExceptions) {
		w.print("    goto tbEnd");
		w.print(t.id);
		w.println(";");
		w.print("tb");
		w.print(t.id);
		w.print(":");
		terminateAndIndent(" accurate::counterClearException();");
	    }
	    // gcc-3.4.4 gets upset when a try block starts and ends
	    // in two different source files.  Maybe something to do
	    // with templates?
	    emitLineNumber(mc.parser, t.startPC);
	    if (t.hasXDCalls && !ctx.cExceptions) {
		translateThrow(t);
	    }
	    if (!ctx.noCppExceptions && !ctx.cExceptions) {
		terminate("    }");
	    }
			
	    int currentTryBlock=-1;
	    if (ctx.cExceptions) {
		// emit the code to translate exceptions and start their dispatch
				
		try {
		    currentTryBlock = ((TryBlockInfo)tryBlocks.lastElement()).code;
		    tryBlocks.removeElementAt( tryBlocks.size() - 1 );					
		    terminateAndIndent("");

		    if (DEBUG) {
			terminateAndIndent("/* registered end of try block no. "+currentTryBlock+" */");
		    }
					
		    if (ctx.safePointMacros) {
			terminateAndIndent("#undef CEXCEPTION_JUMP");
			w.print("#define CEXCEPTION_JUMP ");
					
			if (tryBlocks.size()==0) {
			    w.print("goto _abrupt_end_of_method");
			} else {
			    if (dom.isExecutive()) {
				w.print("goto _try_dispatch_");
			    } else {
				w.print("goto _try_dispatch_translation_");
			    }
				
			    int prevBlock = ((TryBlockInfo)tryBlocks.lastElement()).code;
			    w.print(prevBlock);
			}

		    }
		    terminateAndIndent("");					
					
		} catch (NoSuchElementException e) {
		    // j2c internal error
		    throw new Error("try block stack is empty when generating try dispatch code!");
		}
				
		tryCode++;
				
		setAfterEndPrintWriter();
		w.println("");
		if (!dom.isExecutive()) {

		    // exception translation is done in user domain code and translates 
		    // executive exceptions to user exceptions
									
		    w.print("_try_dispatch_translation_");
		    w.print(currentTryBlock);
		    terminateAndIndent(": {");
		    if (DEBUG) {
			w.print("ubprintf(\"Translation exception block with code ");
			w.print(tryCode); 
			terminateAndIndent(" at line %d for exception %x from domain %x...\\n\",__LINE__,cur_exc,cur_exc_dom);");
		    }	
		    if (ctx.safePointMacros && !ctx.catchPointsUsed) {
			terminateAndIndent("assert(cur_exc!=(HEADER *)&accurate::GCException_singleton);");
		    }					
		    terminateAndIndent("if (!cur_exc_dom) {");
		    if (DEBUG) {
			terminateAndIndent("\tubprintf(\"Invoking translation call...1\\n\");");
		    }
		    terminateAndIndent("\te_java_lang_Throwable *caught_exc = (e_java_lang_Throwable *)cur_exc;");
		    terminateAndIndent("\tcur_exc = 0;");

		    if (ctx.safePointMacros && ctx.catchPointsUsed) {
			terminateAndIndent("accurate::counterClearException();");
		    }

		    w.print("\tHEADER * translated_exc = (HEADER *)");
//		    w.print("\tHEADER * cur_exc = (HEADER *)");
		    w.print(J2cFormat.format(ctx.translateThrowable));
		    w.print("(");
		    visitAppropriate(ctx.CSAvalue);
		    w.print(", (HEADER *) caught_exc)");
		    terminateAndIndent(";");
					
		    if (ctx.safePointMacros) {
			terminateAndIndent("\tCEXCEPTION_PRECISE_GCE_CHECK;");
		    }
		    terminateAndIndent("\tif (cur_exc==0) { cur_exc = translated_exc; }");					
					
		    if (ctx.safePointMacros && ctx.catchPointsUsed) {
			terminateAndIndent("accurate::counterSetException();");
		    }
		    //terminateAndIndent("\tcur_exc = translated_exc;");
		    terminateAndIndent("\tcur_exc_dom = 1;");
		    if (DEBUG) {
			terminateAndIndent("\tubprintf(\"Translation call returned, " +
					   "current exception is now %x.\\n\",cur_exc);");
		    }							
		    terminate("}");
		    terminate("} /* _try_dispatch_translation_ " + currentTryBlock + " */");
		}
				
		w.print("_try_dispatch_");
		w.print(currentTryBlock);
		terminateAndIndent(": {");
		if (DEBUG) {
		    terminateAndIndent(" /* exception dispatch code */");

		    w.print("ubprintf(\"Dispatch block with code ");
		    w.print(tryCode);
		    terminateAndIndent("\\n\");");
		    terminateAndIndent("ubprintf(\"Dispatching exception %x at %s:%d...\\n\",cur_exc,__FILE__,__LINE__);");
		}
		if (ctx.safePointMacros && !ctx.catchPointsUsed) {
		    terminateAndIndent("assert(cur_exc!=(HEADER *)&accurate::GCException_singleton);");
		}									
		setNormalPrintWriter();
	    }

	    for (TryBlock.Handler h = t.handler; h != null; h = h.next) {
		BBSpec b = h.block;
		J2cValue ex = h.catchVar;

		if (ctx.cExceptions) {

		    // generate dispatch blocks
		    // - tests for exception type and jumps to appropriate
		    // catch handlers
					
		    setAfterEndPrintWriter();
		    emitLineNumber(mc.parser, b.startPC);
					
		    terminateAndIndent("");
		    if (DEBUG) {
			w.print("/* exception handler for exception ");
			w.print(J2cFormat.format(h.bp));
			terminateAndIndent(" */");
		    }
		    if (h.bp.equals(ctx.throwableBP)) {
			if (DEBUG) {
			    terminateAndIndent("ubprintf(\"This is the JavaC generated finally block (catch for Throwable)\\n\"); ");
			}
		    }
		    w.print("if (is_subtype_of(((HEADER *)HEADER_BLUEPRINT((");
		    ptype(ctx.throwableBP);
//					w.print(")cur_exc)), (e_s3_core_domain_S3Blueprint*)&");
		    w.print(")cur_exc)), (HEADER *) (&");
		    w.print(J2cFormat.formatBP(h.bp));
		    terminateAndIndent("))) {");
		    w.print("\t");
		    visit(ex);
		    w.print(" = (");
		    ptype(ex.getBlueprint(null));
		    terminateAndIndent(") cur_exc; ");
		    if (DEBUG) {
			w.print("\tubprintf(\"Jumping to exception handler of exception %x of type ");
			ptype(ex.getBlueprint(null)); 
			terminateAndIndent("...\\n\",cur_exc); ");
		    }
		    terminateAndIndent("\tcur_exc=0;");
		    if (ctx.safePointMacros && ctx.catchPointsUsed) {
			terminateAndIndent("accurate::counterClearException();");
		    }
		    if (ctx.frameLists)
			terminateAndIndent("\tcurrentContext->gcTop = (GCFrame *)frame; ");		    
//			terminateAndIndent("\tcurrentContext->gcTop = &frame; ");
		    if (ctx.ptrStack) {
			if (hasGcFrame) {
//			    terminateAndIndent("currentContext->gcTop=(GCFrame*)((&frame)+1); ");
			    terminateAndIndent("currentContext->gcTop=(GCFrame*)(frame+1); ");
			} else {
//			    terminateAndIndent("currentContext->gcTop=(GCFrame*)&frame; ");
			    terminateAndIndent("currentContext->gcTop=(GCFrame*)frame; ");
			}
		    }
		    w.print("\t");
		    jump(b);
		    terminate("}");

		    setNormalPrintWriter();
		    continue;
		}
				
		if (ctx.noCppExceptions) {
		    emitLineNumber(mc.parser, b.startPC);
		    w.print("    if (subtype_of_scalar(HEADER_BLUEPRINT((");
		    ptype(ctx.throwableBP);
		    w.print(")curExc),&");
		    w.print(J2cFormat.formatBP(h.bp));
		    terminateAndIndent(")) {");
		    liveRefs=t.liveAtCatch;
		    visit(ex);
		    w.print(" = (");
		    ptype(ex.getBlueprint(null));
		    w.print(") curExc;");
		    jump(b);
		    terminate(";");
		} else {
		    if (h.bp.equals(ctx.AbortedExceptionBP) || 
			h.bp.equals(ctx.EDAbortedExceptionBP)) {
			this.inAEBlock = false;
		    }
		    if (h.bp.equals(ctx.throwableBP)) {
			terminateAndIndent(this.finallyReplacement("c", null, false));
		    } else {
			w.print("    catch (");
			ptype(h.bp);
			terminateAndIndent("c) {");
		    }
		    liveRefs = t.liveAtCatch;
		    startCatch();
		    visit(ex);
		    w.print(" = (");
		    ptype(ex.getBlueprint(null));
		    w.print(") c;");
		    jump(b);
		}
		terminate("  }");							
	    }


	    if (ctx.noCppExceptions) {
		w.println("    accurate::counterSetException();");
		w.println("#undef EXCEPTION_JUMP");
		w.print("#define EXCEPTION_JUMP ");
		if (t.outer!=null) {
		    w.print("goto tb");
		    w.println(t.outer.id);
		} else {
		    w.println(outerMostExceptionJump);
		}
		w.println("    EXCEPTION_JUMP;");
		w.print("tbEnd");
		w.print(t.id);
		w.println(":;");
	    } else if (ctx.cExceptions) {
				
		// generate last resort jump (if no catch clause caught the exception)
		// and finalize the dispatch code
				
		setAfterEndPrintWriter();
				
		if (DEBUG) {
		    terminateAndIndent("\t/* exception is not handled by this try block, propagate */");
		} else {
		    terminateAndIndent("");
		}
				
		if (tryBlocks.size()==0) {
		    w.print("goto _abrupt_end_of_method;");
		    usedAbruptEndOfMethod = true;
		} else {
		    w.print("goto _try_dispatch_" + (currentTryBlock-1));
		}
				
		terminate(";");
		terminate("} /* _try_dispatch_ " + currentTryBlock + " */");
				
		setNormalPrintWriter();
	    }
	    endTry(block, t.outer);
	}
    }

    void callCSA(S3Method m) {
	w.print(J2cFormat.format(m));
	w.print("(");
	visitAppropriate(ctx.CSAvalue);
	w.print(")");
    }

    /**
     * Coerce an expression of type `from' to type `to'.  This may or
     * may not involve a cast.  If a cast is needed, print '((T) ' and
     * return the string needed to complete the expression '('.
     * Otherwise, print nothing and return the empty string.
     *
     * @param from type to convert from (may be null)
     * @param to   type to convert to (may be null)
     *
     * @return a string to be printed after the expression
     *
     * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
     **/
    String coerce(S3Blueprint from, S3Blueprint to) {
	boolean needCast;

	if (from == to)
	    needCast = false;
	else if (from instanceof S3Blueprint.Primitive)
	    needCast = !(to instanceof S3Blueprint.Primitive);
	else if (from != null && from.isScalar() && !(to instanceof S3Blueprint.Primitive)) {
	    if (to==null || !to.isScalar()) {
//				w.print("/* !!! passing from scalar " + CxxFormat.format(from) + " - " + from + " to nonscalar " + CxxFormat.format(to) + " - " + to + " */");
		w.print("((");
		w.print(CxxFormat.formatCArray((S3Blueprint.Array)to));
		w.print(" *) /*");
		w.print(CxxFormat.format(((S3Blueprint.Array)to).getComponentBlueprint()));
		w.print("*/");
		return ")";
	    }
	    // both from and to are scalars
	    // no need to cast, scalars are represented as HEADER * 
	    needCast = false;
	} else if (to != null && to.isScalar()) {
	    if (from==null || !from.isScalar()) {
//				w.print("/* !!! passing from nonscalar " + (from==null ? "" : CxxFormat.format(from)) + " - " + from + " to scalar " + CxxFormat.format(to) + " - " + to + "*/");
		w.print("((HEADER *)");
		return ")";
	    }
	    // no need to cast, scalars are represented as HEADER * 
	    // but this is not reached, anyway
	    needCast = false;
	} else if ((from != null && from.isSubtypeOf(ctx.OopBP))
		   || to == ctx.OopBP
		   || from == ctx.VM_AddressBP || to == ctx.VM_AddressBP
		   || from == ctx.VM_WordBP || to == ctx.VM_WordBP
		   || (to instanceof S3Blueprint.Array)
		   || (to instanceof S3Blueprint.Primitive))
	    needCast = true;
	else if (to.getType().isScalar() && !to.getType().isInterface())
	    // May need to downcast receiver on devirtualized call
	    needCast = !from.isSubtypeOf(to);
	else
	    needCast = false;

	if (needCast) {
	    w.print ("((");
	    if (to.getType() instanceof Type.Array) {
		w.print(CxxFormat.formatCArray((S3Blueprint.Array)to));
		w.print(" *");
	    } else if (to.isScalar()) {
		w.print("HEADER *");
	    } else {
		w.print(CxxFormat.format(to));
		//if (to.isReference())
		//	w.print(" *");
	    }
	    w.print(" )");
	    return ")";
	} else
	    return "";
    }

    void compileWithType(J2cValue v, S3Blueprint t) {
	String tail = coerce(v.getBlueprint(ctx.domain), t);
	visitAppropriate(v);
	w.print(tail);
    }

    protected void visitDefault(Object o) {
    }

    /*
     * Handle SpecificationIR that is (mostly) shared with the
     * interpreter generator
     */
    public void visit(IRExpr exp) {
	liveRefs = exp.liveRefOut;
	outerValue = exp.dest;
//		if (outerValue != null) {
//		try {
//		// getJavaType may throw an error, in this case, assume
//		// that we don't need a cast :)
//		Type t = exp.dest.getJavaType(ctx.domain);
//		S3Blueprint bp = ctx.blueprintFor(t);
//		compileWithType(exp.source, bp);
//		return;
//		}
//		catch (Error _) { }
//		}
	if (exp.source == null) {
	    w.print("0");
	} else {
	    visitAppropriate(exp.source);
	}
    }

    /*
     * Handle Values that appear within SpecIR expressions
     */
    public void visit(J2cNull _) {
	w.print("0");
    }

    public void visit(SharedStateReference v) {
	w.print("(HEADER *)(&" + J2cFormat.formatShSt(v.getBlueprint()) + ")");
    }

    public void visit(BlueprintReference v) {
	if (v.isConcrete()) {
	    w.print("(HEADER *)(&" + J2cFormat.formatBP(v.blueprintValue()) + ")");
//			w.print("/* unformatted bp is " + v.blueprintValue() + " (case1)*/");
	} else {
	    // FIXME: What exactly am I doing here.  Why is a
	    // reference to the blueprint of an object whose concrete
	    // type is unknown an instance of ConcreteScalar?
//			w.print("((HEADER*)(");
	    visit((J2cReference) v);
//			w.print("))");
//			w.print("/* unformatted bp is " + v.blueprintValue() + " (case2)*/");
	}			
    }

    public void visit(ConcreteScalar v) {
	visitWithBP(v.value, v.getBlueprint());
    }
    public void visit(ConcreteArray v) {
	visitWithBP(v.value, v.getBlueprint());
    }
	
    // FIXME: currently unused, remove ?	
    public void visit(RootsArrayBaseAccessExp v) {
	
//	w.print( "(HEADER *) ( ((e_s3_core_domain_S3Domain*)&"+ CxxFormat.format(dom) + 
//		 ")->" + rootsField + "->values)" );

        if (true) {
          throw new RuntimeException("If used, fix.");
        }
	w.print( "(HEADER *)  ((e_s3_core_domain_S3Domain*)&"+ CxxFormat.format(dom) + 
		 ")->" + rootsField  );
    }

    // FIXME: currently unused, remove ?	
    public void visit(RootsArrayOffsetExp v) {
/*
    
    	w.print( "&((HEADER *) ( ((e_s3_core_domain_S3Domain*)&"+ CxxFormat.format(dom) + 
		 ")->" + rootsField + "->values["+v.idx+"])) - &((HEADER *)  ((e_s3_core_domain_S3Domain*)&"+ CxxFormat.format(dom) +
		                  ")->" + rootsField+")");
*/    

        if (true) {
          throw new RuntimeException("If used, fix.");
        }
    	w.print( "(jint) (&(( (HEADER **) (((e_s3_core_domain_S3Domain*)&"+ CxxFormat.format(dom) + 
		 ")->" + rootsField + "->values)) ["+v.idx+"])) " + 
		 
		"- (jint)(  ((e_s3_core_domain_S3Domain*)&"+ CxxFormat.format(dom) +
		                  ")->" + rootsField+")");
    }

    public void visitWithBP(Oop pointerConst, S3Blueprint t) {
	int idx = FieldMagic._.findRoot(dom, pointerConst);
	// j2cRoots is already an Oop[], so we don't need to cast a
	// root to Oop
//		if (t != ctx.OopBP)
//			w.print("((" + CxxFormat.format(t) + "*)");
//		else
//			w.print("((HEADER *)(");

      if (MemoryManager.the().usesArraylets()) {
      	w.print("(  *(HEADER  ***) (((e_s3_core_domain_S3Domain*)&"+ CxxFormat.format(dom) + 
		")->" + rootsField + "->values) ) [" + idx + "]");
      } else {
	w.print("((HEADER **)(  ((e_s3_core_domain_S3Domain*)&"+ CxxFormat.format(dom) + 
		")->" + rootsField + "->values)) [" + idx + "]");
      }
    }

    public void visit(J2cInt v) {
	if (v.isConcrete()) 
	    w.print("((" + v.getType() + ") 0x" +
		    Integer.toHexString(v.intValue())
		    + ")");
	else
	    visit((J2cValue) v);
    }

    public void visit(J2cNumeric v) {
	if (v.isConcrete()) {
	    String suffix = null;
	    Object printable = v.concreteValue();
	    switch (v.typeCode) {
	    case TypeCodes.LONG:
		suffix = "LL";
		printable
		    = "(jlong) 0x" + Long.toHexString(((Long) printable).longValue());
		break;
	    case TypeCodes.ULONG:
		suffix = "ULL";
		printable
		    = "0x" + Long.toHexString(((Long) printable).longValue());
		break;
	    case TypeCodes.DOUBLE:
		double d = ((Number) v.concreteValue()).doubleValue();
		if (Double.isNaN(d))
		    w.print("_j2c_NaN");
		else if (d == Double.POSITIVE_INFINITY)
		    w.print("HUGE_VAL");
		else if (d == Double.NEGATIVE_INFINITY)
		    w.print("-HUGE_VAL");
		else if (Double.doubleToRawLongBits(d) == Double.doubleToRawLongBits(-0.0))
		    w.print("_j2c_nzero");
		else {
		    w.print(" ");
		    suffix = "";
		    break;
		}
		return;
	    case TypeCodes.FLOAT:
		float f = ((Number) v.concreteValue()).floatValue();
		if (Float.isNaN(f))
		    w.print("_j2c_NaNf");
		else if (f == Float.POSITIVE_INFINITY)
		    w.print("((float) HUGE_VAL)");
		else if (f == Float.NEGATIVE_INFINITY)
		    w.print("((float) -HUGE_VAL)");
		else if (Float.floatToRawIntBits(f) == Float.floatToRawIntBits(-0.0f))
		    w.print("_j2c_nzerof");
		else {
		    w.print(" ");
		    suffix = "F";
		    break;
		}
		return;
	    }
	    w.print(printable + suffix);
	} else
	    visit((J2cValue) v);
    }

    J2cValue outerValue;

    public void visit(J2cValue v) {
	if (v.getName() != null) {
	    if (ctx.gcFrames && mc.everLive.get(v.number) && !currentCallsiteIgnoresSafePoints )
		w.print("frame->");	    
//		w.print("frame.");
// this debug message breaks compilation of some math expressions...
//			w.print("/*j2cv1*/"); 
	    w.print(v.getName());
	} else if (v.source != null) {
	    J2cValue saveOuter = outerValue;
	    outerValue = v;
//			w.print("/*j2cv2*/");
	    visitAppropriate(v.source);
	    outerValue = saveOuter;
	}
	else
	    throw new Error("can't compile value: " + v);
    }

    public void visit(InternalReference r) {
	J2cValue saveOuter = outerValue;
	outerValue = null;
//		w.print("/*intrefsta*/");
	visitAppropriate(r.source);
//		w.print("/*intrefend*/");
	outerValue = saveOuter;
    }

    public void visit(ValueSource v) {
	throw new Error("can't compile ValueSource " + v);
    }

    public void visit(SymbolicConstant c) {
	w.print(c.name);
    }

    public void visit(CurrentPC _) {
	w.print("&&_pc" + nextBlock().startPC);
    }

    public void visit(PCRefExp exp) {
	w.print(exp.target);
    }

    S3Blueprint curRetBP;
    S3Blueprint[] curArgBP;

    public void visit(J2cLookupExp exp) {
	w.print("((");
	ptype(curRetBP);
	w.print("(*)(");
	String prefix = "";
	for (int i = 0; i < curArgBP.length; i++)
	    if (curArgBP[i] != null) {
		w.print(prefix);
		ptype(curArgBP[i]);
		prefix = ", ";
	    }
	w.print("))");
	w.print("( (e_ovm_core_services_memory_VM_1Address **) ( ((e_s3_core_domain_S3Blueprint*) (");
	visitAppropriate(exp.bp);
	w.print("))");
	String table = ("ovm_j2c"
			+ Character.toUpperCase(exp.tableName.charAt(0))
			+ exp.tableName.substring(1, exp.tableName.length()));
	w.print("->"  + table + "->values)) [");
	visitAppropriate(exp.index);
	w.print("])");
    }

    public void visit(JValueExp exp) {
	if (outerValue.isPrimitive() || outerValue.isWidePrimitive()) {
	    visitAppropriate(exp.source);
	    w.print(".jv_" + outerValue.getType());
	} else {
	    w.print("((");
	    if (outerValue instanceof J2cArray) {
		w.print(CxxFormat.formatCArray((S3Blueprint.Array)outerValue.getBlueprint(dom)));
		w.print(" *");
	    } else if (outerValue instanceof J2cReference) {
		w.print("HEADER *");
	    } else {
		//w.print("((" + outerValue.getType() + ")");
	    }
	    w.print(")");
	    visitAppropriate(exp.source);
	    w.print(".jv_jref)");
	}
    }

    public void visit(BinExp exp) {
	Type t = outerValue.getBlueprint(ctx.domain).getType();
	if (exp.operator == "%") {
	    if (t == ctx.domain.FLOAT || t == ctx.domain.DOUBLE) {
		w.print("fmod(");
		visitAppropriate(exp.lhs);
		w.print(", ");
		visitAppropriate(exp.rhs);
		w.print(")");
		return;
	    }
	}
	if ((exp.operator == "/" || exp.operator == "%")
	    && t != ctx.domain.FLOAT && t != ctx.domain.DOUBLE
	    && ((J2cValue) exp.rhs).isConcrete()
	    && exp.rhs.intValue() == 0)
	{
	    /* Work around apple gcc-3.3 bug: it gets stuck trying to
	     * compile division by 0.  This code will have to change
	     * if we ever want to do hardware division by zero checks.
	     */
	    w.print("({ assert(!\"/0 unreachable\"); 0; })");
	    return;
	}
	w.print("(");
	visitAppropriate(exp.lhs);
	w.print(exp.operator);
	visitAppropriate(exp.rhs);
	w.print(")");
    }

    public void visit(UnaryExp exp) {
	w.print("(" + exp.operator + " (");
	visitAppropriate(exp.arg);
	w.print("))");
    }

    public void visit(ConversionExp exp) {

	String rest = "";
		
	if (outerValue instanceof J2cArray) {
	    w.print("((");
	    w.print(CxxFormat.formatCArray((S3Blueprint.Array)outerValue.getBlueprint(dom)));
	    w.print("*)");
	    rest=")";
	} else if (!(outerValue instanceof J2cReference)) { 
	    w.print("((" + outerValue.getType() + ")");
	    rest = ")";
	} else {
//			w.print("/*j2cconv ignored*/");
	}
	visitAppropriate(exp.before);
	w.print(rest);
    }

    //TODO: refactor this method, too much duplicated code... and hard to read
    public void visit(ReinterpretExp exp) {
	Type type = outerValue.getJavaType(ctx.domain);
	char tag = type.getUnrefinedName().getTypeTag();
		
	Type bType = ((J2cValue)exp.before).getJavaType(ctx.domain);
	char bTag = bType.getUnrefinedName().getTypeTag();
		
	if (outerValue.isReference()
	    && (exp.before instanceof J2cFloat
		|| exp.before instanceof J2cDouble)) {
	    w.print("(( ");
	    if (outerValue instanceof J2cArray) {
		w.print(CxxFormat.formatCArray((S3Blueprint.Array)outerValue.getBlueprint(dom)));
		w.print(" * ");
			
	    } else {
		assert(outerValue instanceof J2cReference);
		w.print("HEADER * ");
	    }
	    w.print(")to_jvalue_");
	    w.print(J2cValue.typeCodeToCtype[bTag]);
	    w.print("(");
	    visitAppropriate(exp.before);
	    w.print(").jv_ref)");
			
	} else if (outerValue instanceof J2cReference) {
	    String rest="";
	    if (outerValue instanceof J2cArray) {
		w.print("(( ");
		w.print(CxxFormat.formatCArray((S3Blueprint.Array)outerValue.getBlueprint(dom)));
		w.print(" *) to_jvalue_");
		w.print(J2cValue.typeCodeToCtype[bTag]);
		w.print("(");
		rest=").jv_jref)";
	    } else {
		assert(outerValue instanceof J2cReference);
		if ((exp.before instanceof J2cReference) && !(exp.before instanceof J2cArray)) {
//					w.print("/*j2crei ignored */");
		} else {
		    w.print("( (HEADER *) (");
		    rest="))";
		}
	    }
	    visitAppropriate(exp.before);
	    w.print(rest);
			
	} else {
	    w.print(" to_jvalue_");
	    w.print(J2cValue.typeCodeToCtype[bTag]);
	    w.print("(");
	    visitAppropriate(exp.before);
	    w.print(").jv_");
	    w.print(J2cValue.typeCodeToCtype[tag]);
	}
    }

    public void visit(CCastExp exp) {
	w.print("((");
	w.print(exp.type);
	w.print(")");
	visitAppropriate(exp.exp);
	w.print(")");
    }

    public void visit(ShiftMaskExp exp) {
	w.print("((");
	visitAppropriate(exp.exponent);
	if (((J2cValue) exp.sizeType) instanceof J2cInt)
	    w.print(") & 0x1f)");
	else
	    w.print(") & 0x3f)");

    }

//	public void visit(ListElementExp) { }  used?

    public void visit(LocalStore exp) {
	throw new Error("LocalStore left lying around!");
    }

    // used in array store and elsewhere...
    public void visit(AssignmentExp exp) {
	visitAppropriate(exp.dest);
	w.print(" = ");
	if (exp.dest instanceof InternalReference)
	    visitAppropriate(exp.src);
	else
	    compileWithType((J2cValue) exp.src,
			    ((J2cValue) exp.dest).getBlueprint(ctx.domain));
    }

    public void visit(ValueAccessExp exp) {
	compileWithType(exp.v, outerValue.getBlueprint(ctx.domain));
    }

    public void visit(CondExp exp) {
	w.print("(");
	// FIXME: I would rather not have to even use the type
	// e_ovm_core_domain_Oop, and simply assign all Oop values the
	// C++ type of HEADER, but that isn't possible.  In many
	// cases, I can work around this by infering an Oop value's
	// real type from its uses, but not here
	
	boolean bar = false; 
	
	if (false) {
	  w.println("/*");
	  w.println("exp.lhs.isReference() "+exp.lhs.isReference());
	  w.println("exp.rhs.isReference() "+ exp.rhs.isReference() );
	  w.println("*/");
        }

	if ( exp.lhs.isReference() && exp.rhs.isReference() ) {
	  if (false) {
	    w.println("/*");
            w.println("((J2cValue) exp.lhs).getBlueprint(ctx.domain) " + ((J2cValue) exp.lhs).getBlueprint(ctx.domain));
            w.println("((J2cValue) exp.rhs).getBlueprint(ctx.domain) " + ((J2cValue) exp.rhs).getBlueprint(ctx.domain));
	    w.println("((J2cValue) exp.lhs).isConcrete() " + ((J2cValue) exp.lhs).isConcrete() );
	    w.println("((J2cValue) exp.rhs).isConcrete() " + ((J2cValue) exp.rhs).isConcrete() );
	    w.println("*/");
	  }
	  
	  if ( ( ((J2cValue) exp.lhs).getBlueprint(ctx.domain) != ctx.VM_AddressBP) &&
	       ( ((J2cValue) exp.rhs).getBlueprint(ctx.domain) != ctx.VM_AddressBP) 
	       
//	        && !((J2cValue) exp.lhs).isConcrete() && !((J2cValue) exp.rhs).isConcrete()
// this breaks constant strings comparison (user domain constant strings can move)
// and it was a workaround for non-working getReferenceAssertions - which is now fixed
// delete the comment when sufficiently confident that it is now correct
	       ) {
	         bar = MemoryManager.the().needsAcmpBarrier(); 
          }
	}
	
	if (bar) {
	  int al = SpecInstantiation.getReferenceAssertions( ctx, (J2cValue) exp.lhs );
	  int ar = SpecInstantiation.getReferenceAssertions( ctx, (J2cValue) exp.rhs );
	
	  S3Blueprint lbp = ((J2cValue) exp.lhs).getBlueprint(ctx.domain);
	  S3Blueprint rbp = ((J2cValue) exp.rhs).getBlueprint(ctx.domain);
	  
	  // !! implicitly assumed here the barrier does not have safe points, does not
	  // throw exceptions, ... - otherwise, startCall, endCall will be needed
          w.print(J2cFormat.format(
            ( exp.operator == "==" ) ? ctx.acmpeqBarrier : ctx.acmpneBarrier));
          w.print("(");
          visitAppropriate(ctx.CSAvalue);
          w.print(", ");            
	  if ( lbp == ctx.OopBP || !lbp.isScalar() ) {
	    w.print("(HEADER*) ");
	  }
          visitAppropriate(exp.lhs);	  
	  w.print(", ");                        
	  if ( rbp == ctx.OopBP || !rbp.isScalar() ) {
	    w.print("(HEADER*) ");
	  }
          visitAppropriate(exp.rhs);
	  w.print(", ");
	  SpecInstantiation.referenceComment(al, lbp);
	  w.print(al);
	  w.print(", ");
	  SpecInstantiation.referenceComment(ar, rbp);
	  w.print(ar);
	  w.print(")");
	} else {

	  if (exp.lhs.isReference() &&
	    ((J2cValue) exp.lhs).getBlueprint(ctx.domain) == ctx.OopBP)
	    w.print("(HEADER*) ");
          visitAppropriate(exp.lhs);
          w.print(exp.operator);
          if (exp.rhs.isReference() &&
	    ((J2cValue) exp.rhs).getBlueprint(ctx.domain) == ctx.OopBP)
	    w.print("(HEADER*) ");
          visitAppropriate(exp.rhs);
        }
	w.print(")");
    }

    boolean inCondition = false;
    public void visit(IfExp ifExp) {
	inCondition=true;
	w.print("(");
	visitAppropriate(ifExp.cond);
	w.print(" ? ");
	visitAppropriate(ifExp.ifTrue);
	w.print(" : ");
	visitAppropriate(ifExp.ifFalse);
	w.print(")");
	inCondition=false;
    }
    public void visit(IfCmd ifCmd) {
	w.print("if (");
	inCondition = true;
	visitAppropriate(ifCmd.cond);
	inCondition = false;
	terminateAndIndent(") {");
	w.print("\t");
	visitAppropriate(ifCmd.ifTrue);
	terminateAndIndent(";");
	if (ifCmd.ifFalse != null) {
	    terminateAndIndent("} else {");
	    w.print("\t");
	    visitAppropriate(ifCmd.ifFalse);
	    terminateAndIndent(";");
	}
	w.print("}");
    }

//	public void visit(BitFieldExp exp) { } used?

    public void visit(MemExp exp) {
	w.print("*(" + outerValue.getType() + "*)");
	w.print("((char*)");
	visitAppropriate(exp.addr); 
	w.print(" + "); 
	visitAppropriate(exp.offset); 
	w.print(")"); 
    }

    public void visit(ArrayAccessExp exp) {
	J2cArray arr = (J2cArray)exp.arr;
	S3Blueprint.Array abp = (S3Blueprint.Array) arr.getBlueprint();
	S3Blueprint ebp = (S3Blueprint) abp.getComponentBlueprint();
		
	w.print(" (( (");
	ptype(ebp.getType());
	w.print("*)");
	visitAppropriate(exp.arr);
	w.print("->values )[");
	visitAppropriate(exp.index);
	w.print("])");
    }

    public void visit(ArrayLengthExp exp) {
	w.print("(");
	visitAppropriate(exp.arr);
	w.print("->length)");
    }

    public void visit(BlueprintAccessExp exp) {
	w.print("((HEADER *)HEADER_BLUEPRINT(");
//		w.print("/*bpae*/(HEADER_BLUEPRINT(");
	visitAppropriate(exp.ref);
	w.print("))");
//		w.print("))");
    }

    public void visit(J2cFieldAccessExp exp) {
	String name = J2cFormat.getUTF(exp.selector.getNameIndex());
	w.print("((");
	w.print(CxxFormat.format(exp.declaringBP));
	w.print("*)");
	visitAppropriate(exp.obj);
	w.print(")->");
	w.print(CxxFormat.encode(name));
//		w.print("->" + CxxFormat.format(exp.declaringBP) + "::"
//				+  CxxFormat.encode(name));
    }


    HashSet exceptionNonThrowers = null;
	
    public boolean cannotThrowException( Value target ) {
		
	if (exceptionNonThrowers==null) {
	    exceptionNonThrowers = new HashSet();
	    exceptionNonThrowers.add(J2cValue.makeSymbolicReference("needs_init"));
	    exceptionNonThrowers.add(J2cValue.makeSymbolicReference("is_subtype_of"));
	    exceptionNonThrowers.add(J2cValue.makeSymbolicReference("subtype_of_scalar"));
	    exceptionNonThrowers.add(J2cValue.makeSymbolicReference("array_store_valid"));
	    exceptionNonThrowers.add(J2cValue.makeSymbolicReference("ARRAY_STORE_INVALID"));
	}
		
	return exceptionNonThrowers.contains(target);
    }
	
    public void visit(InvocationExp exp) {
	curRetBP = exp.rt;
	curArgBP = exp.at;


	if (ctx.cExceptions && (J2cValue.makeSymbolicReference("j2c_throw")==exp.target)) {

	    // translate j2c_throw calls to macro CETHROW
			
	    w.print("CETHROW(");
					
	    if (tryBlocks.size()==0) {
		w.print("goto _abrupt_end_of_method");
		usedAbruptEndOfMethod=true;
	    } else {
		if (dom.isExecutive()) {
		    w.print("goto _try_dispatch_");
		} else {
		    w.print("goto _try_dispatch_translation_");
		}
				
		try {
		    int currentTryBlock = ((TryBlockInfo)tryBlocks.lastElement()).code;
		    w.print(currentTryBlock);					

		} catch (NoSuchElementException e) {
		    // not reached
		    throw new Error("VM internal error!");
		}

	    }
			
	    w.print(", ");
	    visitAppropriate(exp.args[0]);
	    w.print(")");
	    return ;
	}
		
	String rest = (outerValue == null
		       ? ""
		       : coerce(exp.rt, outerValue.getBlueprint(ctx.domain)));


	if (exp.gcInScope) {
	    startCall(exp.isSafePoint, exp.rt, exp.target);
	    if (ctx.safePointMacros) {
		usedAbruptEndOfMethod = true;
		w.print("PACKCALL(");
	    }
	}

	visitAppropriate(exp.target);
	String prefix;
	if (exp.gcInScope && ctx.safePointMacros) {
	    w.print(",");
	    // must precalculate the actual
	    // number of arguments
	    int nargs = 0;
	    for (int i = 0; i < exp.args.length; i++) {
		if (exp.args[i].isWidePrimitive()) {
		    i++;
		}
		nargs++;
	    }
	    w.print(nargs);
	    prefix = ",";
	} else {
	    w.print("(");
	    prefix = "";
	}
	for (int i = 0; i < exp.args.length; i++) {
	    w.print(prefix);
	    w.flush();
			
	    String crest = "";
/*			if ((exp.at != null) && (exp.at[i].isScalar())) {
			w.print("(HEADER *)(");
			crest = ")";
			} 
*/			
	    if (exp.at != null)
		compileWithType((J2cValue) exp.args[i], exp.at[i]);
	    else
		visitAppropriate(exp.args[i]);
	    if (exp.args[i].isWidePrimitive())
		i++;
	    w.print(crest);
			
	    prefix = ", ";
	}
	w.print(")");
	if (exp.gcInScope)
	    endCall();
	w.print(rest);
    }

    int computeSavedVars() {
	localsList.setLength(0);
	int nSaved = 0;
	if (liveRefs == null) {
	    w.print("NO_LIVENESS_CALL(");
	    return 1;
	}
	for (int i = liveRefs.nextSetBit(0);
	     i != -1;
	     i = liveRefs.nextSetBit(i+1)) {
	    //System.err.println("startCall: var @" + i);
	    J2cValue v = allocator.getVar(i);
	    localsList.append(", ");
	    if (v == null)
		localsList.append("unknown var " + i);
	    else
		localsList.append(v.getName());
	    nSaved++;
	}
	return nSaved;
    }

    private boolean currentCallCanThrowExceptions = true;
    
    void startCall(boolean isSafePoint, S3Blueprint bp, Value target) {
    
    	S3Method m = null;
    	
    	if ((target!=null) && (target instanceof MethodReference)) {
    		m = ((MethodReference)target).method ;
	}
	
	startCall(isSafePoint, bp, m);
    }
        
    void startCall(boolean isSafePoint, S3Blueprint bp, S3Method m) {
    		
	S3Blueprint targetBP = null; 
	
	if (m!=null) {
		targetBP = (S3Blueprint) m.getDeclaringType().getDomain().blueprintFor(m.getDeclaringType());
	} 
	startCall( isSafePoint, bp, m, targetBP );
    }

    void startCall(boolean isSafePoint, S3Blueprint bp, S3Method m,
    	S3Blueprint targetBP) {
    	
    	if (currentCallsiteIgnoresExceptions) {

    	  currentCallCanThrowExceptions = false;
    	} else {

    	  currentCallCanThrowExceptions = true;
    	  if ((m!=null) && (targetBP!=null)) {
    		
		if (PragmaAssertNoExceptions.descendantDeclaredBy(
			m.getSelector(), targetBP ) != null) {
    			
			currentCallCanThrowExceptions = false;	
				// safePointMacros not supported by this mechanism
		}
          }
        }
	
	w.print(" /* ");
	if (m!=null) {
	  w.print(m.getSelector());
        } else {
          w.print("(null)");
        }
        
        if (isSafePoint) {
          w.print(" (SafePoint) ");
        } else {
          w.print(" (NON-SafePoint) ");
        }
	w.print(" */ ");
	
	if (ctx.cExceptions && !ctx.safePointMacros && currentCallCanThrowExceptions) {
		
	    // before each call, exceptions must be cleared
	    // (there can be a context switch that could otherwise clobber the exception
	    // status)	
			
	    // after each call, must check of exception and if there is any, jump
	    // to appropriate exception dispatch code
			
	    if (bp == null
		|| bp.getType().getUnrefinedName() == TypeName.Primitive.VOID) {
		w.print("CECALL_VOID(");
	    } else {
		w.print("CECALL_NONVOID(");
	    }

	    if (tryBlocks.size()==0) {
		w.print("goto _abrupt_end_of_method");
		usedAbruptEndOfMethod=true;
	    } else {
		if (dom.isExecutive()) {
		    w.print("goto _try_dispatch_");
		} else {
		    w.print("goto _try_dispatch_translation_");
		}
				
		try {
		    int currentTryBlock = ((TryBlockInfo)tryBlocks.lastElement()).code;
		    w.print(currentTryBlock);					

		} catch (NoSuchElementException e) {
		    // not reached
		    throw new Error("VM internal error!");
		}

	    }
		
	    w.print(", ");
	}
		
	if (TEST_LIVENESS) {
	    localsList.setLength(0);
	    if (!isSafePoint)
		return;
	    w.print("({ ");
	    boolean ret =
		(bp != null
		 && bp.getType().getUnrefinedName() != TypeName.Primitive.VOID);
	    if (ret) {
		ptype(bp);
		w.print(" _r =");
	    }
	    localsList.append("; ");
	    localsList.append("/*LIVE=" + liveRefs + "*/");
	    for (int i = 0; i < allVars.length; i++) {
		if (allVars[i] != null
		    && allVars[i].flavor == LocalAllocator.REF_VAR
		    && allVars[i].kind != J2cValue.STACK_SLOT
		    && !liveRefs.get(allVars[i].number))
		{
		    localsList.append(allVars[i].getName());
		    localsList.append(" = (");
		    S3Blueprint cbp = allVars[i].getBlueprint(ctx.domain);
		    localsList.append(CxxFormat.format(cbp));
		    localsList.append(" *) 0xffffffff /*" +
				      allVars[i].number +
				      "*/;");
		}
	    }
	    if (ret)
		localsList.append("_r; ");
	    localsList.append("})");
	    return;
	}
	if (ctx.safePointMacros) {
	    nSaved = isSafePoint ? computeSavedVars() : 0;
	    if (nSaved != 0
		&& curBlock != null
		&& curBlock.innerMostTry != null
		&& liveRefs == curBlock.innerMostTry.liveAtCatch)
		w.print("/*TRYBLOAT*/");
	    if (bp == null
		|| bp.getType().getUnrefinedName() == TypeName.Primitive.VOID)
	    {
		w.print("CALL");
	    }
	    else {
		w.print("RCALL");
	    }
	    if (nSaved == 0)
		w.print('0');
	    w.print('(');
	}
			
    }

    void endCall() {

	if (TEST_LIVENESS) {
	    w.print(localsList.toString());
	    return;
	}
	if (ctx.safePointMacros) {
	    if (nSaved > 0) {
		w.print(",\t");
		w.print(nSaved);
		w.print(localsList.toString());
	    }
	    w.print(")");
	}
	if (ctx.cExceptions && !ctx.safePointMacros && currentCallCanThrowExceptions) {
	    w.print(")");
	}
    }

    void startCatch(int nrefs, String refList) {
	if (ctx.frameLists)
	    w.print("currentContext->gcTop = (GCFrame *)frame; ");	
//	    w.print("currentContext->gcTop = &frame; ");
	if (ctx.ptrStack) {
	    if (hasGcFrame) {
//		w.print("currentContext->gcTop=(GCFrame*)((&frame)+1); ");
		w.print("currentContext->gcTop=(GCFrame*)(frame+1); ");		
	    } else {
//		w.print("currentContext->gcTop=(GCFrame*)&frame; ");
		w.print("currentContext->gcTop=(GCFrame*)frame; ");
	    }
	}
	if (ctx.catchPointsUsed) {
	    w.print("CATCHRESTORE(\t");
	    if (nrefs == -1)
		w.print("/*NO_LIVENESS_CATCH*/0");
	    else {
		w.print(nrefs);
		w.print(refList);
	    }
	    terminateAndIndent(");");
	}
    }

    void startCatch() {
	if (ctx.safePointMacros && liveRefs != null)
	    startCatch(computeSavedVars(), localsList.toString());
	else
	    startCatch(-1, "");
    }

    void translateThrow(TryBlock t) {
	liveRefs = t.liveAtCatch;
	terminate("");
	// Restore from any handler block.  They are all treated the
	// same 
	if (ctx.noCppExceptions) {
	    w.println("if (curExcDom==0) {");
	    w.print("        curExc=(void*)");
	    fresh=false;
	    startCall(true,ctx.OopBP,ctx.translateThrowable);
	    w.print("PACKCALL(");
	    w.print(J2cFormat.format(ctx.translateThrowable));
	    w.print(",2,");
	    visitAppropriate(ctx.CSAvalue);
	    w.print(", (e_java_lang_Throwable*)curExc)");
	    endCall();
	    terminate(";");
	    w.print("        curExcDom=");
	    w.print((int)dom.getUID());
	    w.println(";");
	    terminate("    }");
	} else {
	    terminateAndIndent("      }" + finallyReplacement("e", "e", true));
	    startCatch();
	    w.print("j2c_throw(");
	    fresh = false;
	    if (!TEST_LIVENESS) {
		// because otherwise liveAtCatch is null
		startCall(true, ctx.OopBP, ctx.translateThrowable);
		if (ctx.safePointMacros)
		    w.print("PACKCALL(");
	    }
	    w.print(J2cFormat.format(ctx.translateThrowable));
	    if (ctx.safePointMacros && !TEST_LIVENESS)
		w.print(",2,");
	    else
		w.print("(");
	    visitAppropriate(ctx.CSAvalue);
	    w.print(", e)");
	    if (!TEST_LIVENESS)
		endCall();
	    terminate(");");
	    terminate("      }");
	}
    }

    public void visit(SeqExp exp) {
	char open;
	char delim;
	String close;

	if (outerValue instanceof J2cVoid) {
	    open = '{';
	    delim = ';';
	    close = "; }";
	} else {
	    open = '(';
	    delim = ',';
	    close = ")";
	}
	char prefix = open;
	for (int i = 0; i < exp.v.length; i++, prefix = delim) {
	    w.print(prefix);
	    w.print(" ");
	    visitAppropriate(exp.v[i]);
	}
	w.print(close);
    }

    public void visit(DimensionArrayExp exp) {
	terminateAndIndent("({");
	terminateAndIndent("_dims.length = " + exp.dims.length + ";");
	// Should stamp with blueprint?
	for (int i = 0; i < exp.dims.length; i++) {
	    w.print("((jint *)_dims.values)[ " + i + "] = ");
	    visitAppropriate(exp.dims[i]);
	    terminateAndIndent(";");
	}
	// e_Array<jint, n> not subtype of e_Array<jint, 1>!
	//terminateAndIndent("(e_Array<jint> *) &_dims;");
	terminateAndIndent("(e_Array *) &_dims;");
	w.print("})");
	fresh = false;
    }

    /**
     * Instructions whose behavior isn't completely described by
     * SpecIR
     */
    void jump(BBSpec to) {
	terminateAndIndent("goto _pc" + to.startPC + ";");
    }

    public void visit(BRExpr e) {
	liveRefs = null;
	if (e.inputs.length > 0) {
	    w.print("if (");
	    visitAppropriate(e.inputs[0]);
	    w.print(") ");

	    jump(curBlock.next[1]);
	} else {
	    jump(curBlock.next[0]);
	}
    }

    public void visit(SWExpr e) {
	liveRefs = null;
	w.print("switch (");
	visitAppropriate(e.inputs[0]);
	terminateAndIndent(") {");

	for (int i = 0; i < e.keys.length; i++) {
	    w.print("case " + e.keys[i] + ": ");
	    jump(curBlock.next[i]);
	}
	w.print("default: ");
	jump(curBlock.next[e.keys.length]);
	terminateAndIndent("}");
    }

    // Need a whole new BCExpr subclass for multinewarray_quick
    J2cValue[] inputs;

    public void visit(BCExpr exp) {
	liveRefs = exp.liveRefOut;
	inputs = exp.inputs;
	visitAppropriate(exp.source);
    }

    public void visit(Instruction.POLLCHECK i) {

	terminateAndIndent("if ("+PollcheckManager.getSettings().fastPathInC()+") {");
	w.print("\t");
	boolean ei = currentCallsiteIgnoresExceptions;
	currentCallsiteIgnoresExceptions = true;
	startCall(true, null, J2cValue.makeSymbolicReference("j2c_signalEvent"));
//	startCall(true, null, (S3Method)null, null);
	if (ctx.safePointMacros) {
	    usedAbruptEndOfMethod = true;
	    w.print("PACKCALL(");
	}
	w.print("j2c_signalEvent");
	if (ctx.safePointMacros)
	    w.print(",0)");
	else
	    w.print("()");
	endCall();
	terminateAndIndent(";");
	w.print("}");
        if (LATENCY_PROFILING) {	
  	  w.print("STORE_LINE_NUMBER();\n\t");
        }    
	currentCallsiteIgnoresExceptions = ei;
    }

    public void visit(Instruction.INCREMENT_COUNTER i) {
      w.print("incrementCounter(");
      visitAppropriate(inputs[0]);
      terminateAndIndent(");");
    }

    public void visit(Instruction.NULLCHECK i) {
    
    	boolean alwaysNull = false;

	if (inputs[0] instanceof J2cReference) {
	    J2cReference r = (J2cReference)inputs[0];
	    	
	    if (!r.getTypeInfo().includesNull()) {
		// terminateAndIndent("/* NC: cannot be null, eliding */");
		return ;
	    }
	    
	    J2cValue thisVar = allocator.findLocal(TypeCodes.OBJECT, 0);
	    
            if (false) {
              terminateAndIndent("/* NC: r.kind="+r.kind+" r.number="+r.number+" m.isVirtual="+m.isVirtual()+" m.isConstructor="+m.isConstructor()+" r.name="+r.name+" */"); 
              terminateAndIndent("/* thisVar = "+thisVar+"*/");
              if (thisVar!=null) {
                terminateAndIndent("/* NC: thisVar.kind="+thisVar.kind+" thisVar.number="+thisVar.number+" thisVar.name="+thisVar.name+" */"); 
              }
            }
            
            // FIXME: a lot of guessing is involved in this condition
            // FIXME: this doesn't allow to remove nullchecks in inlined methods
            if ( ( thisVar != null ) && (r.kind == J2cValue.LOCAL_VAR) && (r.number == thisVar.number) && (m.isVirtual() || m.isConstructor()) ) {
//	      terminateAndIndent("/* NC: this cannot be null, eliding */"); 
	      return ;
	    }
	    
	} else {
	    if (inputs[0] instanceof J2cNull) {
	    	// terminateAndIndent("/* NC: is always null, changing to unconditional jump */");
		alwaysNull = true;
	    } else {
		terminateAndIndent("/* NC: is neither J2cReference, nor J2cNull: "+inputs[0]+"*/");
		throw new Error("NULLCHECK input is neither J2cNull, nor J2cReference");
	    }
	}


	if (ctx.cExceptions) {
	
	    if (!alwaysNull) {
	    	w.print("CENULLCHECK(");
	    }

	    // insert a macro call here for checking the reference and
	    // possible throwing a null pointer exception					
					
	    if (tryBlocks.size()==0) {
			
		w.print("goto _top_npe_thrower");
		usedTopNpeThrower = true;
				
	    } else {
		w.print("goto _npe_thrower_");
				
		TryBlockInfo ti;
		try {
		    ti = (TryBlockInfo)tryBlocks.lastElement();

		} catch (NoSuchElementException e) {
		    // not reached
		    throw new Error("VM internal error!");
		}
				
		int currentTryBlock = ti.code;
		w.print(currentTryBlock);
				

		if (!ti.generatedNpeThrower) {
									
		    setAfterEndPrintWriter();		
		    w.print("_npe_thrower_");
		    w.print(ti.code);
		    terminateAndIndent(": {");
						
		    terminateAndIndent("cur_exc = 0;");
		    if (ctx.safePointMacros && ctx.catchPointsUsed) {
			terminateAndIndent("accurate::counterClearException();");
		    }
		    w.print("HEADER *npe_ex =");
		    w.print("(");
		    ptype(ctx.NullPointerExceptionBP);
		    w.print(")");
		    w.print(J2cFormat.format(ctx.makeThrowable));
		    w.print("(");
		    visitAppropriate(ctx.CSAvalue);
		    w.print(", ");
		    w.print(Throwables.NULL_POINTER_EXCEPTION);
		    terminateAndIndent(",0,0);");
		    if (ctx.safePointMacros) {
			terminateAndIndent("\tCEXCEPTION_PRECISE_GCE_CHECK;");
		    }
		    terminateAndIndent("assert(cur_exc==0);");
					
		    terminateAndIndent("cur_exc = npe_ex;");					
		    w.print("cur_exc_dom = ");
		    if (dom.isExecutive()) {
			terminateAndIndent("0;");
		    } else {
			terminateAndIndent("1;");
		    }						
					
		    if (ctx.safePointMacros && ctx.catchPointsUsed) {
			terminateAndIndent("accurate::counterSetException();");
		    }

		    w.print("goto _try_dispatch_");
		    w.print(ti.code);
		    terminate(";");
		    w.print("} /* _npe_thrower_");
		    w.print(ti.code);
		    terminate(" */");
					
		    setNormalPrintWriter();
		    ti.generatedNpeThrower = true;
		}
	    } 
	
	    if (!alwaysNull) {		
	    	w.print(", ");
	    	visitAppropriate(inputs[0]);
	    	w.print(")");
	    }
	    terminateAndIndent(";");
	}
    }

    public void visit(Instruction.ReturnValue i) {
	S3Blueprint bp;
	try {
	    bp = ctx.blueprintFor(m.getReturnType());
	} catch (LinkageException e) { throw e.unchecked(); }
	if (m.getMode().isSynchronized()) {
	    // FIXME: where is the saveRetVal behaviour with gcFrames?
	    // This is a *major* bug.
	    boolean saveRetVal =
		(ctx.safePointMacros
		 && !inputs[0].isConcrete()
		 && allocator.bpFlavor(bp) == LocalAllocator.REF_VAR);

	    if (ctx.cExceptions && ctx.safePointMacros && !ctx.catchPointsUsed) {
		terminateAndIndent("assert(cur_exc!=(HEADER *)&accurate::GCException_singleton);");
	    }

	    w.print("monitorEntered=0; ");						

	    if (saveRetVal)
		w.print("CALL(");
	    else
		startCall(false, null, ctx.monitorExit);
	    if (saveRetVal || ctx.safePointMacros) {
		usedAbruptEndOfMethod = true;
		w.print("PACKCALL(");
	    }
	    w.print(J2cFormat.format(ctx.monitorExit));
	    if (saveRetVal || ctx.safePointMacros) 
		w.print(",2,");
	    else
		w.print("(");
	    visitAppropriate(ctx.CSAvalue);
	    //w.print(", (e_ovm_core_domain_Oop *)");
	    w.print(", ");
	    if (BARRIERS_ON_MONITORS) {
	      readBarrierStart();
            }
	    visitAppropriate(synchVar);
	    if (BARRIERS_ON_MONITORS) {
	      readBarrierEnd();
            }
	    w.print(")");
	    if (saveRetVal)
		w.print(",\t1, " + inputs[0].getName() + ")");
	    else
		endCall();
	    terminateAndIndent(";");
	    if (DEBUG) {
		terminateAndIndent("ubprintf(\"Exited monitor (implicitly-3) at %s:%d \\n\",__FILE__,__LINE__);");
	    }			
	}
	if (hasGcFrame) {
	    if (ctx.frameLists)
		terminateAndIndent("currentContext->gcTop = ((GCFrame *)frame)->next;");	    
//		terminateAndIndent("currentContext->gcTop = frame.next;");
	    if (ctx.ptrStack)
		terminateAndIndent("currentContext->gcTop = (GCFrame*)frame;");	    
//		terminateAndIndent("currentContext->gcTop = (GCFrame*)&frame;");
	}
	fresh = false;
	w.print("return ");
	compileWithType(inputs[0], bp);
    }
    public void visit(Instruction.RETURN _) {
	if (m.getMode().isSynchronized()) {
	    // FIXME: Duplicate code

	    if (ctx.cExceptions && ctx.safePointMacros && !ctx.catchPointsUsed) {
		terminateAndIndent("assert(cur_exc!=(HEADER *)&accurate::GCException_singleton);");
	    }
			
	    w.print("monitorEntered=0; ");

	    startCall(false, null, ctx.monitorExit);
	    if (ctx.safePointMacros) {
		usedAbruptEndOfMethod = true;
		w.print("PACKCALL(");
	    }
	    w.print(J2cFormat.format(ctx.monitorExit));
	    if (ctx.safePointMacros) 
		w.print(",2,");
	    else
		w.print("(");
	    visitAppropriate(ctx.CSAvalue);
	    //w.print(", (e_ovm_core_domain_Oop *) ");
	    w.print(", ");
	    if (BARRIERS_ON_MONITORS) {
	      readBarrierStart();
            }
	    visitAppropriate(synchVar);
	    if (BARRIERS_ON_MONITORS) {
	      readBarrierEnd();
            }
	    w.print(")");
	    endCall();
	    terminateAndIndent(";");
	    if (DEBUG) {
		terminateAndIndent("ubprintf(\"Exited monitor (implicitly-4) at %s:%d \\n\",__FILE__,__LINE__);");
	    }			
	}
	if (hasGcFrame) {
	    if (ctx.frameLists)
//		terminateAndIndent("currentContext->gcTop = frame.next;");
		terminateAndIndent("currentContext->gcTop = ((GCFrame*)frame)->next;");
	    if (ctx.ptrStack)
//		terminateAndIndent("currentContext->gcTop = (char*)&frame;");
		terminateAndIndent("currentContext->gcTop = (char*)frame;");
	}
	fresh = false;
	w.print("return");
    }
    public void visit(Instruction.RET _) {
	// inputs[0] isn't an intermediate value that holds a
	// LocalExp, it is the actual value we found in the abstract
	// machine.  Luckily, everything still works
	String Else  = "";
	for (int i = curBlock.next.length; i --> 1; ) {
	    w.print(Else);
	    w.print(" if (");
	    visitAppropriate(inputs[0]);
	    w.print(" == ");
	    w.print(curBlock.next[i].startPC);
	    w.print(") goto _pc");
	    w.print(curBlock.next[i].startPC);
	    terminateAndIndent(";");
	    Else = "else";
	}
	if (curBlock.next.length > 0) {
	    w.print(Else);
	    w.print(" goto _pc");
	    w.print(curBlock.next[0].startPC);
	    terminateAndIndent(";");
	}
    }

    // FIXME: when these methods are really needed, make sure to insert
    //  the barriers depending on style - Brooks, replicating...
    void readBarrierStart() {
      w.print(J2cFormat.format(ctx.checkingTranslatingReadBarrier));
      w.print("(");
      visitAppropriate(ctx.CSAvalue);
      w.print(",");
    }
    
    void readBarrierEnd() {
      w.print(")");
    } 
}

