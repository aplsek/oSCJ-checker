package s3.services.bootimage;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantResolvedMethodref;
import ovm.core.domain.InlinedAttribute;
import ovm.core.domain.Method;
import ovm.core.repository.Attribute;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.ExceptionHandler;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.editor.CloneInstructionVisitor;
import ovm.services.bytecode.editor.Marker;
import ovm.services.bytecode.reader.ByteCodeConstants.Attributes;
import ovm.util.ArrayList;
import ovm.util.CommandLine;
import s3.core.domain.S3ByteCode;
import s3.services.j2c.MethodCompiler.BranchVisitor;
import s3.util.PragmaInline;
import ovm.core.repository.Descriptor;
import ovm.core.repository.TypeCodes;
import ovm.core.OVMBase;
import ovm.util.BitSet;
import ovm.services.bytecode.InstructionVisitor;
import ovm.services.bytecode.reader.ByteCodeConstants;
import s3.core.domain.S3Method;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.editor.Cursor;
import ovm.services.bytecode.InstructionBuffer;
import ovm.core.domain.Type;
import s3.util.PragmaNoInline;
import s3.util.PragmaMayNotLink;
import java.util.Stack;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import ovm.core.repository.ConstantsEditor;
import ovm.core.repository.Selector;
import ovm.core.domain.ConstantPool;
import ovm.core.domain.Domain;
import ovm.core.domain.ConstantResolvedInstanceMethodref;
import ovm.core.domain.DomainDirectory;
import ovm.core.repository.JavaNames;
import ovm.core.repository.UnboundSelector;
import ovm.core.repository.ConstantClass;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;


/**
 * An initial version that does no cross domain or gaurded inlining.
 * Cross-domain inlining is a huge deal for things like allocation,
 * and locking.  This is something that gcc can do for us, but it may
 * be hard to stage properly at the java level.  Gaurded inlining is
 * potentially a huge win.  If a method has two extremely small
 * implementations, replacing a virtual dispatch with a type check
 * is a big deal.<p>
 *
 * One of the most important practical benifits of this whole process
 * is that fully inlined methods can be excluded from the bootimage.
 * This shoudl greatly speed up compilation with gcc.  Doing this
 * means keeping track of which methods may be called virtually.<p>
 *
 * Gaurded inlining, similarly, means iterating over all called
 * methods to make inlining decisions.
 **/
public class Inliner implements JVMConstants.Opcodes {

    static public final boolean DEBUG = false;

    static private double c_0;
    static private double c_1;

    static private final double LIGHT_C_0 = 6;
    static private final double LIGHT_C_1 = 1;
    static private final double HEAVY_C_0 = 16;
    static private final double HEAVY_C_1 = 2;

    /**
     * If true, ensure that every inlined method has its own set of
     * local variables.  This will allow j2c to do a better job of
     * assigning local variables names and static types.  It also
     * improves j2c's ability to do copy propagation.  But, if
     * there is register pressure, it will slow down the resulting
     * machine code.<p>
     * 
     * As a compromise, we generate distinct indexes
     * for the local variables of each inlined function <i>unless</i>
     * the total number of locals exceeds
     * {@link #MAX_DISTINCT_LOCALS}.
     **/
    boolean genDistinctLocals;

    /**
     * Generate a distinct index for each inlined local variable
     * unless the total number of locals exceeds this value.  This
     * variable is controlled by the -inline-max-distinct-locals
     * paraemter.
     **/
    static private int MAX_DISTINCT_LOCALS;

    /**
     * This exception is thrown when {@link #MAX_DISTINCT_LOCALS} is
     * exceeded.
     **/
    static class TooManyLocals extends RuntimeException {
    }

    // static private final int MAX_BYTES = 2048;
    
    static public boolean getParameters() {
	MAX_DISTINCT_LOCALS = InvisibleStitcher.getInt("inline-max-distinct-vars");
	String parm = InvisibleStitcher.getString("inline");
	if (parm == "")
	    parm = "light";
	else if (parm == null)
	    parm = "none";
	parm = parm.toLowerCase().intern();
	if (parm == "none") {
	    c_0 = c_1 = 0;
	} else if (parm == "light") {
	    c_0 = LIGHT_C_0;
	    c_1 = LIGHT_C_1;
	} else if (parm == "heavy") {
	    c_0 = HEAVY_C_0;
	    c_1 = HEAVY_C_1;
	} else {
	    String[] p = parm.split(",");
	    if (p.length != 2) {
		throw new MisconfiguredException("inline parameter " +
						 p + " not a pair");
	    }
	    try {
		c_0 = Double.parseDouble(p[0]);
		c_1 = Double.parseDouble(p[1]);
	    } catch (NumberFormatException _) {
		throw new MisconfiguredException("inline parameter " + p +
						 " not pair of numbers");
	    }
	}
	return enabled();
    }

    static public boolean enabled() {
	return c_0 != 0 || c_1 != 0;
    }

    int newMaxLocals;
    int newMaxStack;
    int curMaxStack;
    S3ByteCode code;
    CodeFragmentEditor cfe;
    ConstantsEditor constants;
    int outerPc;
    
    static interface Copier {
	void bindPC();
	void keepInstruction(Instruction i);
	int getMaxLocals();
	Cursor replaceCall(int pc);
	void finish();
    }

    private Copier nullCopier = new Copier() {
	    public void bindPC() { }
	    public void keepInstruction(Instruction _) { }
	    public int getMaxLocals() {
		return code.getMaxLocals();
	    }
	    public Cursor replaceCall(int pc) {
		outerPc = pc;
		cfe.removeInstruction(pc);
		return cfe.getCursorAfterMarker(pc);
	    }
	    public void finish() { }
	};

    public class InlineCopier extends CloneInstructionVisitor
	implements Copier
    {
	Marker[] pcMap;
	int localDelta;
	int maxLocals;

	/**
	 * Store arguments into fresh local variables.  Define marker
	 * for each branch target, LNT and local variable entry.  Add
	 * all locals/line #s.  Add inlined-method attribute
	 *
	 * FIXME: The LocalVariableTable entry for every inlined
	 * method * parameter should start at the instruction in which
	 * that parameter * is assigned.  However, all these entries
	 * start before any parameter * is assigned.  This is good
	 * enough for j2c, but it may not be good * enough for a JIT +
	 * JVMDI.
	 **/
	public InlineCopier(S3ByteCode code,
			    InstructionBuffer buf,
			    Cursor cursor,
			    int localDelta,
			    ConstantMethodref mr) {
	    super(buf, cfe);
	    setCursor(cursor);
	    this.localDelta = localDelta;
	    this.maxLocals = code.getMaxLocals();
	    this.pcMap = new Marker[code.getBytes().length + 1];
	    if (getMaxLocals() > newMaxLocals)
		newMaxLocals = getMaxLocals();

	    Marker headMarker = cursor.makeUnboundMarker();
	    cfe.addInlinedMethod(outerPc,
				 makePCMarker(0),
				 makePCMarker(pcMap.length - 1),
				 code.getMethod());
	    ExceptionHandler[] h = code.getExceptionHandlers();
	    for (int i = 0; h != null && i < h.length; i++) {
		makePCMarker(h[i].getStartPC());
		makePCMarker(h[i].getEndPC());
		makePCMarker(h[i].getHandlerPC());
	    }
	    Attribute[] att = code.getAttributes();
	    int[] n = ByteCodeConstants.attributeNames;
	    for (int i = 0; i < att.length; i++) {
		int n_i = att[i].getNameIndex();
		if (n_i == n[Attributes.LineNumberTable]) {
		    Attribute.LineNumberTable lnt =
		        (Attribute.LineNumberTable) att[i];
		    int[] line = lnt.getLineNumberTable();
		    int[] startPC = lnt.getStartPCTable();
		    for (int j = 0; j < startPC.length; j++) {
		        cfe.addLineNumber(makePCMarker(startPC[j]),
					  line[j]);
		    }
		} else if (n_i == n[Attributes.LocalVariableTable]) {
		    Attribute.LocalVariableTable lvt =
		        (Attribute.LocalVariableTable) att[i];
		    for (int j = lvt.size(); j --> 0; ) {
			int startPC = lvt.getStartPC(j);
			int endPC = startPC + lvt.getLength(j);
			cfe.addLocalVariable((startPC == 0
					      ? headMarker
					      : makePCMarker(startPC)),
					     makePCMarker(endPC),
					     lvt.getDescriptor(j),
					     lvt.getVariableNameIndex(j),
					     lvt.getIndex(j) + localDelta);
		    }
		}
	    }

	    new BranchVisitor() {
		public void visit(int curPC, int nextPC) {
		    if (curPC != -1 && nextPC != -1)
			makePCMarker(nextPC);
		}
	    }.run(code);


	    Descriptor.Method desc = code.getSelector().getDescriptor();
	    int ac = desc.getArgumentCount();
	    int aw = ac + desc.getWideArgumentCount();
	    boolean isStatic = code.getMethod().getMode().isStatic();
	    cursor.bindMarker(headMarker);
	    if (isStatic) {
		if (code.getMethod().getDeclaringType().getDomain()
		    == DomainDirectory.getExecutiveDomain())
		    cursor.addLOAD_SHST_METHOD_QUICK(constants.addMethodref(mr));
		else
		    cursor.addLOAD_SHST_METHOD(constants.addMethodref(mr));
		cursor.addSimpleInstruction(POP);
	    }
	    aw++;		// NOTE: localshift already done for
				// static methods.  Shoudn't be!

	    for (aw--; ac --> 0; aw--)
		switch (desc.getArgumentType(ac).getTypeTag()) {
		case TypeCodes.BOOLEAN:
		case TypeCodes.BYTE:
		case TypeCodes.SHORT:
		case TypeCodes.CHAR:
		case TypeCodes.INT:
		    cursor.addIStore((char) (localDelta + aw));
		    break;
		case TypeCodes.FLOAT:
		    cursor.addFStore((char) (localDelta + aw));
		    break;
		case TypeCodes.OBJECT:
		case TypeCodes.ARRAY:
		    cursor.addAStore((char) (localDelta + aw));
		    break;
		case TypeCodes.LONG:
		    cursor.addLStore((char) (localDelta + --aw));
		    break;
		case TypeCodes.DOUBLE:
		    cursor.addDStore((char) (localDelta + --aw));
		    break;
		}
	    if (!isStatic) {
		Type t = code.getMethod().getDeclaringType();
		cursor.addFiat((ConstantClass) t.getDomain().blueprintFor(t));
		cursor.addAStore((char) localDelta);
	    }
	}
	
	Marker makePCMarker(int pc) {
	    if (pcMap[pc] == null)
		pcMap[pc] = cursor.makeUnboundMarker();
	    return pcMap[pc];
	}

	protected Marker getMarkerAtPC(int pc) {
	    return pcMap[pc];
	}

	public void bindPC() {
	    if (pcMap[getPC()] != null)
		cursor.bindMarker(pcMap[getPC()]);
	}

	public void keepInstruction(Instruction i) {
	    bindPC();
	    super.visitAppropriate(i);
	}

	public int getMaxLocals() { return localDelta + maxLocals; }

	public Cursor replaceCall(int _) {
	    bindPC();
	    return cursor;
	}

	public void finish() {
	    cursor.bindMarker(pcMap[pcMap.length - 1]);
	    // FIXME: need to restore line # at callsite
	}

	public void visit(Instruction.ALOAD i) {
	    cursor.addALoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_ALOAD i) {
	    cursor.addALoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ALOAD_0 i) {
	    cursor.addALoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ALOAD_1 i) {
	    cursor.addALoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ALOAD_2 i) {
	    cursor.addALoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ALOAD_3 i) {
	    cursor.addALoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ILOAD i) {
	    cursor.addILoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_ILOAD i) {
	    cursor.addILoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ILOAD_0 i) {
	    cursor.addILoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ILOAD_1 i) {
	    cursor.addILoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ILOAD_2 i) {
	    cursor.addILoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ILOAD_3 i) {
	    cursor.addILoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FLOAD i) {
	    cursor.addFLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_FLOAD i) {
	    cursor.addFLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FLOAD_0 i) {
	    cursor.addFLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FLOAD_1 i) {
	    cursor.addFLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FLOAD_2 i) {
	    cursor.addFLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FLOAD_3 i) {
	    cursor.addFLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LLOAD i) {
	    cursor.addLLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_LLOAD i) {
	    cursor.addLLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LLOAD_0 i) {
	    cursor.addLLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LLOAD_1 i) {
	    cursor.addLLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LLOAD_2 i) {
	    cursor.addLLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LLOAD_3 i) {
	    cursor.addLLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DLOAD i) {
	    cursor.addDLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_DLOAD i) {
	    cursor.addDLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DLOAD_0 i) {
	    cursor.addDLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DLOAD_1 i) {
	    cursor.addDLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DLOAD_2 i) {
	    cursor.addDLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DLOAD_3 i) {
	    cursor.addDLoad((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}

	public void visit(Instruction.ASTORE i) {
	    cursor.addAStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_ASTORE i) {
	    cursor.addAStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ASTORE_0 i) {
	    cursor.addAStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ASTORE_1 i) {
	    cursor.addAStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ASTORE_2 i) {
	    cursor.addAStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ASTORE_3 i) {
	    cursor.addAStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ISTORE i) {
	    cursor.addIStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_ISTORE i) {
	    cursor.addIStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ISTORE_0 i) {
	    cursor.addIStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ISTORE_1 i) {
	    cursor.addIStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ISTORE_2 i) {
	    cursor.addIStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.ISTORE_3 i) {
	    cursor.addIStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FSTORE i) {
	    cursor.addFStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_FSTORE i) {
	    cursor.addFStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FSTORE_0 i) {
	    cursor.addFStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FSTORE_1 i) {
	    cursor.addFStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FSTORE_2 i) {
	    cursor.addFStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.FSTORE_3 i) {
	    cursor.addFStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LSTORE i) {
	    cursor.addLStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_LSTORE i) {
	    cursor.addLStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LSTORE_0 i) {
	    cursor.addLStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LSTORE_1 i) {
	    cursor.addLStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LSTORE_2 i) {
	    cursor.addLStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.LSTORE_3 i) {
	    cursor.addLStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DSTORE i) {
	    cursor.addDStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.WIDE_DSTORE i) {
	    cursor.addDStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DSTORE_0 i) {
	    cursor.addDStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DSTORE_1 i) {
	    cursor.addDStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DSTORE_2 i) {
	    cursor.addDStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}
	public void visit(Instruction.DSTORE_3 i) {
	    cursor.addDStore((char)
			    (i.getLocalVariableOffset(buf) + localDelta));
	}

	public void visit(Instruction.IINC i) {
	    cursor.addIInc((char)(i.getLocalVariableOffset(buf) + localDelta),
			   (short)i.getValue(buf));
	}

	public void visit(Instruction.RET i) {
	    if (true)
		throw new Error("JSR/RET in inlined method!");
	    cursor.addRet((char)(i.getLocalVariableOffset(buf) + localDelta));
	}

	// FIXME: what about return with non-empty stack?  I really
	// need to do dataflow!
	public void visit(Instruction.ReturnValue i) {
	    if (buf.hasRemaining())
		cursor.addGoto(pcMap[pcMap.length-1]);
	}

	// FIXME: what about return with non-emtpy stack?  I really
	// need to do dataflow!
	public void visit(Instruction.RETURN i) {	
	    if (buf.hasRemaining())
		cursor.addGoto(pcMap[pcMap.length-1]);
	}
    }

    /**
     * FIXME: We should do type inference here to improve dispatch
     * accuracy.  Right now, it is impossible to inline things like
     * hashCode, equals, and toString.
     **/
    public class InlineExpander extends InstructionVisitor {
	Copier copier;

	public void visit(Instruction i) {
	    copier.keepInstruction(i);
	}

	// INVOKEVIRTUAL || INVOKEINTERFACE
	public void visit(Instruction.Invocation i) {
	    ConstantMethodref mr = i.getConstantMethodref(buf, cp);
	    if (!(mr instanceof ConstantResolvedMethodref)) {
		copier.keepInstruction(i);
		return;
	    }
	    Method mBase = ((ConstantResolvedMethodref) mr).getMethod();
	    Type dt  = mBase.getDeclaringType();

	    // FIXME: Do we inline in user before starting analysis in
	    // executive?  I think there are two seperate analysis
	    // objects to deal with anyway!
	    if (dt.getDomain() != dom) {
		copier.keepInstruction(i);
		return;
	    }
		
	    // FIXME: see class doc
	    Blueprint rcv = dom.blueprintFor(dt);
	    Method mDv = anal.getTarget(rcv, mBase);

	    if (mDv == null) {
		copier.keepInstruction(i);
		markLive(rcv, mBase);
	    } else if (!tryInline(ConstantResolvedInstanceMethodref.make
				  (mDv,
				   ((ConstantResolvedMethodref) mr).getOffset(),
				   dom.blueprintFor(mDv.getDeclaringType()))))
	    {
		// FIXME: should devirtualize as INVOKESPECIAL
		copier.keepInstruction(i);
		markLive(mDv);
	    }
	}

	public void visit(Instruction.INVOKESPECIAL i) {
	    ConstantResolvedMethodref mr;
	    try {
		mr = (ConstantResolvedMethodref) i.getConstantMethodref(buf, cp);
	    } catch (ClassCastException _) {
		copier.keepInstruction(i);
		return;
	    }
	    if (!tryInline(mr)) {
		copier.keepInstruction(i);
		markLive(mr.getMethod());
	    }
	}

	public void visit(Instruction.INVOKESTATIC i) {
	    ConstantResolvedMethodref mr;
	    try {
		mr = (ConstantResolvedMethodref) i.getConstantMethodref(buf, cp);
	    } catch (ClassCastException _) {
		copier.keepInstruction(i);
		return;
	    }
	    if (!tryInline(mr)) {
		copier.keepInstruction(i);
		markLive(mr.getMethod());
	    }
	}

	private boolean shouldInline(Method m) {
	    if (m.getMode().isSynchronized()) {
	        if (DEBUG) System.err.println("INLINER: Not inlining "+m+" because it is synchronized.");
		return false;
            }
	    S3ByteCode c = m.getByteCode();
	    if (c.getExceptionHandlers().length != 0) {
	        if (DEBUG) {
	          System.err.println("INLINER: Not inlining "+m+" because it has exception handlers:");	    
	          Object[] handlers = c.getExceptionHandlers();
	        
	        
	          for(int i=0; i<handlers.length;i++) {
	            System.err.println("INLINER: "+i+" "+handlers[i]);
                  }
                }
		return false;
            }
	    Selector.Method sel = m.getSelector();
	    Blueprint bp = dom.blueprintFor(m.getDeclaringType());

	    if (PragmaNoInline.declaredBy(sel, bp)) {
              if (DEBUG) System.err.println("INLINER: Not inlining "+m+" because it is PragmaNoInline");	      
              return false;
	    }
	    
	    if (PragmaMayNotLink.declaredBy(sel, bp)) {
	      if (DEBUG) System.err.println("INLINER: Not inlining "+m+" because it is PragmaMayNotLink");	      
	      return false;
	    }
            
            if (inMethod[m.getCID()].get(m.getUID())) {
              if (DEBUG) System.err.println("INLINER: Not inlining "+m+" because it is being processed");	                  
              return false;
            }
            
            if (!anal.shouldCompile(m)) {
              if (DEBUG) System.err.println("INLINER: Not inlining "+m+" because analysis says it is not reachable");
              return false;
            }
            
	    if (PragmaInline.declaredBy(sel, bp)) {
	      if (DEBUG) System.err.println("INLINER: Inlining "+m+" because it is PragmaInline");
              return true;
            }
	    int argCount = c.getSelector().getDescriptor().getArgumentCount();
	    boolean res = c.getBytes().length <= c_0 + c_1*argCount;
	    
	    if (DEBUG) System.err.println("INLINER: Should inline "+m+" is "+res );
	    return res;
	}
	
	private boolean tryInline(ConstantResolvedMethodref mr) {
	    Method m = mr.getMethod();
	    if (shouldInline(m)) {
		if (Driver.img_ovmir_ascii != null)
		    Driver.img_ovmir_ascii.println("inlining " + m);
		S3ByteCode code = m.getByteCode();
		InstructionBuffer buf = InstructionBuffer.wrap(code);
		int localOffset;
		if (!genDistinctLocals)
		    localOffset = copier.getMaxLocals();
		else if (newMaxLocals + code.getMaxLocals() > MAX_DISTINCT_LOCALS) {
		    throw new TooManyLocals();
		}
		else
		    localOffset = newMaxLocals;
		InlineCopier childCopier =
		    new InlineCopier(code, buf,
				     copier.replaceCall(getPC()),
				     localOffset,
				     mr);
		InlineExpander child = new InlineExpander(buf, childCopier);
		child.expandBody(m);
		return true;
	    } else
		return false;
	}

	void expandBody(Method m) {
	    S3ByteCode code = m.getByteCode();

	    inMethod[m.getCID()].set(m.getUID());
	    curMaxStack += code.getMaxStack();
	    try {
		if (curMaxStack > newMaxStack)
		    newMaxStack = curMaxStack;
		while (buf.hasRemaining())
		    visitAppropriate(buf.get());
		copier.finish();
	    } finally {
		inMethod[m.getCID()].clear(m.getUID());
		curMaxStack -= code.getMaxStack();
	    }
	}

	InlineExpander(InstructionBuffer buf, Copier copier) {
	    super(buf);
	    this.copier = copier;
	}
    }

    void expandMethod(Method m) {
	code = m.getByteCode();
	S3ByteCode.Builder builder = new S3ByteCode.Builder(code);
        ConstantPool rcpb = code.getConstantPool();
	genDistinctLocals = code.getMaxLocals() <= MAX_DISTINCT_LOCALS;
	if (false)
	    code.dumpAscii("BEFORE", Driver.img_ovmir_ascii);
	while (true) {
// 	    if (!genDistinctLocals)
// 		System.err.println("compiling " + m.getSelector()
// 				   + " with fewer locals");
	    cfe = new CodeFragmentEditor(code, rcpb);
	    constants = rcpb;
	    newMaxLocals = code.getMaxLocals();
	    newMaxStack = 0;
	    try {
		InlineExpander exp = new InlineExpander(InstructionBuffer.wrap(code),
							nullCopier);
		exp.expandBody(m);
	    } catch (TooManyLocals _) {
		genDistinctLocals = false;
		continue;
	    }
	    break;
	} 
	if (cfe.wasEdited()) {
	    cfe.commit(builder, (char) newMaxStack, (char) newMaxLocals);
	    code.bang(builder.build());
	}
	if (false)
	    code.dumpAscii("AFTER", Driver.img_ovmir_ascii);
    }

    BitSet[] inMethod;
    BitSet[] liveMethods;
    Stack work = new Stack();
    Domain dom;
    Analysis anal;

    Inliner(Domain dom, Analysis anal, boolean runTimeLoading) {
	this.dom = dom;
	this.anal = anal;
	liveMethods = new BitSet[DomainDirectory.maxContextID() + 1];
	for (int i = 0; i < liveMethods.length; i++)
	    liveMethods[i] = new BitSet();
	inMethod = new BitSet[DomainDirectory.maxContextID() + 1];
	for (int i = 0; i < liveMethods.length; i++)
	    inMethod[i] = new BitSet();
    }

    void run() {
	Method[] m = dom.getReflectiveCalls();
	for (int i = 0; i < m.length; i++)
	    markLive(m[i]);
	m = dom.getReflectiveVirtualCalls();
	for (int i = 0; i < m.length; i++)
	    markLive(dom.blueprintFor(m[i].getDeclaringType()), m[i]);
	if (anal.clinitIsLive)
	    new Analysis.MethodWalker() {
		    public void walk(Method m) {
			UnboundSelector sel =
			    m.getSelector().getUnboundSelector();
			if (sel == JavaNames.CLINIT)
			    markLive(m);
		    }
		}.walkDomain(dom);
	while (!work.isEmpty())
	    expandMethod((Method) work.pop());
	new Analysis.MethodWalker() {
		public void walk(Method m) {
		    if (!(liveMethods[m.getCID()].get(m.getUID())))
			anal.dontCompile(m);
		}
	    }.walkDomain(dom);
    }

    // Caching the blueprint/method combination is a pain in the ass!
    void markLive(Blueprint rcv, Method base) {
	Method[] m = anal.getTargets(rcv, base);
	if (m == null)
	    return;
	for (int i = 0; i < m.length; i++)
	    markLive(m[i]);
    }

    void markLive(Method m) {
	if (!liveMethods[m.getCID()].get(m.getUID())
	    && anal.shouldCompile(m))
	{
	    work.push(m);
	    liveMethods[m.getCID()].set(m.getUID());
	}
    }
}
