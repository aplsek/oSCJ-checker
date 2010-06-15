
package ovm.services.bytecode.editor;

import ovm.core.OVMBase;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Code;
import ovm.core.repository.Attribute;
import ovm.core.repository.Bytecode;
import ovm.core.repository.CodeBuilder;
import ovm.core.repository.ConstantPoolBuilder;
import ovm.core.repository.ConstantsEditor;
import ovm.core.repository.Descriptor;
import ovm.core.repository.ExceptionHandler;
import ovm.core.services.io.BasicIO;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.InstructionVisitor;
import ovm.util.ByteBuffer;
import ovm.util.OVMError;
import ovm.util.ArrayList;
import ovm.services.bytecode.reader.ByteCodeConstants;
import ovm.services.bytecode.reader.ByteCodeConstants.Attributes;
import ovm.util.Collections;
import ovm.core.domain.Method;
import ovm.util.ListIterator;
import ovm.core.domain.InlinedAttribute;
import ovm.util.LinkedList;
import ovm.util.Iterator;
import s3.core.domain.S3ByteCode;

/**
 * The editor allows inserting and removing of opcodes. The editor will keep
 * track of the changes and eventually produce a ByteCodeFragment and a
 * constant pool that reflects the changes.
 * @see ovm.services.bytecode.editor.CodeFragmentEditor
 * @author Christian Grothoff, Ben L. Titzer
 */
public class CodeFragmentEditor extends OVMBase {

    /**
     * Original code fragment that is being edited.
     */
    protected final InstructionBuffer oldCode_;

    /**
     * Each instruction is copied from input to output by adding it to
     * the PC's postCursor (obtained by getCursorAfterMarker).  We
     * want to avoid adding builders for instructions that are
     * removed, but ensure that the cursor obtained by
     * getCursorAfterMarker actually points AFTER the requested
     * instruction.  We do this by copying the instruction on the
     * first call to getCursorAfterMarker, and ensuring that
     * getCursorAfterMarker has been called for every instruction
     * before the new code is committed.
     **/
    private final CloneInstructionVisitor civ;

    /**
     * List of all the Cursors created so far. There may only be one Cursor per
     * pc, so we can use a Hashtable to map pc to Cursor.
     * <p>
     * preCursors are for the code that is inserted before the marker
     * associated with the old instruction's offset.
     */
    private final HTint2Cursor preCursors_;
    /**
     * List of all the Cursors created so far. There may only be one Cursor per
     * pc, so we can use a Hashtable to map pc to Cursor.
     * <p>
     * postCursors are for the code that is inserted after the marker
     * associated with the old instruction's offset.
     */
    private final HTint2Cursor postCursors_;
    /**
     * Bitmap marking offsets of instructions that were removed.
     */
    private final boolean[] removed_;
    /**
     * The constant pool that was build in the last "commit".
     */
    protected final ConstantsEditor cPoolBuilder;

    /**
     * The current prediction for the offsets
     */
    private final int[] predictedOffsets_;

    /**
     * List if exception handlers (internal representation)
     */
    private final ExceptionHandlerList ehl_;

    private ArrayList lineNumbers = new ArrayList();
    private ArrayList localVars = new ArrayList();
    private LinkedList inlinedMethods = new LinkedList();

    private final Attribute[] attrs_;

    /**
     * Whether any code has been changed using this editor.
     */
    private boolean dirty_ = false;

    /**
     * Create an Editor. <em>FIXME wish: this constructor already does a complete pass through
     * the bytecode to identify the PCs at which instructions begin, and even
     * plants markers to identify catch blocks. It would not be hard to
     * identify branch targets in the same pass, and that information would be
     * useful to confirm the correctness of certain edits. Of course ideally,
     * someday, rewriting should happen after verification, and ideally any
     * rewriter will be able to query the type and flow information collected
     * during verification.</em>
     * @param cPoolb the target constant pool in which to put the new constants
     */
    public CodeFragmentEditor(Bytecode original, ConstantsEditor cPoolb) {

        this(
            InstructionBuffer.wrap(original),
            original.getAttributes(),
            original.getExceptionHandlers(),
            cPoolb);
    }

    /**
     * Create a CodeFragmentEditor starting with an empty code fragment (that
     * just contains a single NOP, has 0 LV, 0 maxstack, no attributes and no
     * exception handlers).
     * @param cPoolb the constant pool builder to use
     * @param sel the method selector to use
     */
    public CodeFragmentEditor(ConstantsEditor cPoolb, Selector.Method sel) {
	this(new Bytecode.Builder(new byte[1], //code
		new ConstantPoolBuilder().build(), // constantPool,
		(char) 0, //maxStack,
		(char) 0, //maxLocals,
		new ExceptionHandler[0], //ExceptionHandler[] exceptions,
		sel, //Selector.Method selector,
		new Attribute[0]).build(), 
	     cPoolb);
    }

    public CodeFragmentEditor(S3ByteCode original, ConstantsEditor cPoolb) {
        this(
            InstructionBuffer.wrap(original),
            original.getAttributes(),
            original.getExceptionHandlers(),
            cPoolb);
    }

    private CodeFragmentEditor(InstructionBuffer oldCode, Attribute[] attrs, ExceptionHandler[] ex,
	    ConstantsEditor cPoolb) {
	this.oldCode_ = oldCode;
	this.attrs_ = attrs;
	int len = oldCode_.getCode().limit();
	this.preCursors_ = new HTint2Cursor();
	this.postCursors_ = new HTint2Cursor();
	this.removed_ = new boolean[len];
	this.predictedOffsets_ = new int[len];
	this.cPoolBuilder = cPoolb;
	this.civ = new CloneInstructionVisitor(oldCode_, this);

        oldCode.rewind();
        while (oldCode_.hasRemaining()) {
            Instruction i = oldCode_.get();
            int pc = oldCode_.getPC();
            predictedOffsets_[pc] = pc;
            int size = i.size(oldCode_);
            if (size == 0)
                fail("InstructionSize is " + size + ": " + i);
            try {
                for (int k = pc + 1; k < pc + size; k++)
                    predictedOffsets_[k] = -1;
            } catch (ArrayIndexOutOfBoundsException aeiob) {
                BasicIO.err.println(
                    "AIOBE At PC " + pc + " i.size():" + size + " len: " + len);
                for (int x = 0; x < len; x++)
                    BasicIO.err.println("Code: " + x + " " + oldCode_.get(x));

                // throw aeiob;
            }
            // invalid, no beginning of instruction
        }
        if ((ex == null) || (ex.length == 0)) {
            ehl_ = new ExceptionHandlerList(null, null, null, null);
            ehl_.delete();
        } else {
            ehl_ =
                new ExceptionHandlerList(
                    getCursorAfterMarker(ex[0].getStartPC()).getMarkerAtZero(),
                    getCursorBeforeMarker(ex[0].getEndPC()).addMarker(),
                    getCursorBeforeMarker(ex[0].getHandlerPC()).addMarker(),
                    ex[0].getCatchTypeName());
            ExceptionHandlerList pos = ehl_;
            for (int i = 1; i < ex.length; i++)
                pos =
                    pos.insertAfter(
                        getCursorAfterMarker(ex[i].getStartPC()).getMarkerAtZero(),
                        getCursorBeforeMarker(ex[i].getEndPC()).addMarker(),
                        getCursorBeforeMarker(ex[i].getHandlerPC()).addMarker(),
                        ex[i].getCatchTypeName());
        }

	int lastPC = predictedOffsets_.length - 1;
	while (predictedOffsets_[lastPC] == -1)
	    lastPC--;
	Marker methodEndMarker = getCursorAfterMarker(lastPC).addMarker();

	for (int i = 0; i < attrs.length; i++) {
	    if (attrs[i].getNameIndex() ==
		ByteCodeConstants.attributeNames[Attributes.LocalVariableTable])
	    {
		Attribute.LocalVariableTable lvt =
		    (Attribute.LocalVariableTable) attrs[i];
		for (int j = lvt.size(); j --> 0; ) {
		    int startPC = lvt.getStartPC(j);
		    int endPC = startPC + lvt.getLength(j);
		    Marker endMarker = (endPC == predictedOffsets_.length
					? methodEndMarker
					: getMarkerAtPC(endPC));
		    addLocalVariable(getMarkerAtPC(startPC),
				     endMarker,
				     lvt.getDescriptor(j),
				     lvt.getVariableNameIndex(j),
				     lvt.getIndex(j));
		}
	    }
	    else if (attrs[i].getNameIndex() == 
		     ByteCodeConstants.attributeNames[Attributes.LineNumberTable])
	    {
		Attribute.LineNumberTable lnt =
		    (Attribute.LineNumberTable) attrs[i];
		int[] line = lnt.getLineNumberTable();
		int[] startPC = lnt.getStartPCTable();
		for (int j = 0; j < startPC.length; j++)
		    addLineNumber(getMarkerAtPC(startPC[j]), line[j]);
	    }
	    else if (attrs[i].getNameIndex() == InlinedAttribute.nameIndex) {
		InlinedAttribute ia = (InlinedAttribute) attrs[i];
		for (int j = ia.size(); j --> 0; ) {
		    int startPC = ia.getStartPC(j);
		    Marker start = getMarkerAtPC(startPC);
		    int endPC = startPC + ia.getLength(j);
		    Marker end = (endPC == predictedOffsets_.length
				  ? methodEndMarker
				  : getMarkerAtPC(endPC));
		    // Note: We can't simply call addInlinedMethod in
		    // reverse order.  If m1 contains m2, and both
		    // share a start pc, that would swap them!
		    inlinedMethods.add(0, new InlinedMethod
				       (startPC, start, end,
					ia.getMethod(j)));
		}
	    }
	}
    }

    public ConstantsEditor getConstantsEditor() {
	return cPoolBuilder;
    }

    public InstructionBuffer getOriginalCode() {
        return oldCode_;
    }

    /**
     * Get a Cursor to add instructions immediately before the marker
     * associated with the given pc.
     * @param pc this pc designates the position in the old bytecode
     * @return a Cursor that will add instructions before the marker of the
     *         instruction we are currently at
     */
    public Cursor getCursorBeforeMarker(int pc) {
        Cursor g = preCursors_.get(pc);
        if (g == null) {
            g = new Cursor(this, cPoolBuilder, pc);
            preCursors_.put(pc, g);
        }
        return g;
    }

    /**
     * Get the marker at the given pc.
     * @param pc this pc designates the position in the old bytecode
     * @return the marker for this PC.
     */
    public Marker getMarkerAtPC(int pc) {
        return getCursorBeforeMarker(pc).getMarkerAtZero();
    }

    /**
     * Get a Cursor to add instructions immediately after the marker associated
     * with the given pc but before the old instruction.
     * @param pc this pc designates the position in the old bytecode
     * @return a Cursor that will add instructions after the marker of the
     *         instruction we are currently at
     */
    public Cursor getCursorAfterMarker(int pc) {
        Cursor g = postCursors_.get(pc);
        if (g == null) {
            g = new Cursor(this, cPoolBuilder, pc);
            postCursors_.put(pc, g);
	    if (!removed_[pc]) {
		boolean wasDirty = dirty_;
		try {
		    civ.setCursor(g);
		    civ.visitAppropriate(oldCode_.get(pc));
		} finally { dirty_ = wasDirty; }
	    }
        }
        return g;
    }

    /**
     * Remove an instruction from the CodeFragment.
     * @param pc the position where to remove the instruction
     * @throws DoubleRemoveError if called on the same pc twice
     * @throws IllegalProgramPointError if the pc is not valid (not implemented
     *                 yet)
     */
    public void removeInstruction(int pc) {
	if (removed_[pc]) throw new DoubleRemoveError("Instruction removed twice: " + pc);
	if (predictedOffsets_[pc] == -1) throw new IllegalProgramPointError("No opcode at pc " + pc);
	removed_[pc] = true;
	dirty_ = true;
	Cursor g = postCursors_.get(pc);
	if (g != null) g.removeFirst();

    }

    public Cursor replaceInstruction(int pc) {
        removeInstruction(pc);
        Cursor c = getCursorAfterMarker(pc);
        return c;
    }

    public Cursor replaceInstruction() {
        return replaceInstruction(oldCode_.getPC());
    }

    /**
     * Obtain a linked list of the exception handlers.
     * @return never null, if no handlers are declared, a dummy handler list
     *         with a deleted handler and null-markers is returned
     */
    public ExceptionHandlerList getExceptionHandlers() {
        return ehl_;
    }

    public void addLineNumber(Marker m, int line) {
	lineNumbers.add(new LineNumber(m, line));
    }

    public void addLocalVariable(Marker startPC, Marker endPC,
				 Descriptor.Field type, int nameIndex,
				 int index) {
	localVars.add(new LocalVar(startPC, endPC, type, nameIndex, index));
    }

    /**
     * Declare an inlined method.  Because the Inlined attribute
     * implies nesting relationships, there are limits on how
     * addInlinedMethod may be called.  In particular, we need to be
     * able to resolve nesting relationships based only on inlined
     * methods start PCs in the original bytecode.<p>
     *
     * When adding one level of inlining, calls to addInlinedMethod
     * can be performed in any order.  If you wish to add nested
     * inlined methods in a single CodeFragmentEditor, calls to
     * addInlinedMethod must be ordered according to the start
     * Markers, and nesting.  So, if in the final bytecode, inlined
     * method A appears before or around inlined method B,
     * addInlinedMethod must be called on A before B.
     *
     * @param startPC the last PC in the original bytecode before the
     * first instruction in m
     * @param start   the position immediately before the first
     * instruction in m
     * @param end     the position immediately after the last
     * instruction in m
     * @param m       the inlined method
     **/
    public void addInlinedMethod(int startPC, Marker start, Marker end,
				 Method m) {
	ListIterator it = inlinedMethods.listIterator();
	while (it.hasNext()) {
	    InlinedMethod im = (InlinedMethod) it.next();
	    if (im.startPC > startPC) {
		it.previous();
		break;
	    }
	}
	it.add(new InlinedMethod(startPC, start, end, m));
    }

    /**
     * Finalize all the changes and generate a CodeFragment. Will also compute
     * the ConstantPool and enable "getConstantPool()"
     * @param minLocals the minimum number of local variables (this-ptr,  arguments!)
     * @return the edited code fragment
     */
    public CodeBuilder commit(CodeBuilder builder, char minLocals) {
	ByteBuffer code = generateBytecode();
	builder.setCode(code);
	// run inference to obtain stackheight / local var count  (special state object!)

	InstructionBuffer newCode = 
	    InstructionBuffer.wrap(code, oldCode_.getSelector(), cPoolBuilder.unrefinedBuild());
	MaxHeightInferenceVisitor heightv = new MaxHeightInferenceVisitor(newCode);
	heightv.run();
	if (heightv.getMaxLocals() > minLocals) minLocals = heightv.getMaxLocals();
	minLocals++; /* we count differently... */
	builder.declareTemporaries(heightv.getMaxStack(), minLocals);
	copyAttributesAndUpdateLNTs(builder, code.limit());
	ehl_.commit(builder);

        builder.setUnrefinedConstantPool(cPoolBuilder.unrefinedBuild());
        // System.err.println("Commit: new: " + builder.unrefinedBuild());
        return builder;
    }

    /**
     * Finalize all the changes and generate a CodeFragment. Will also compute
     * the ConstantPool and enable "getConstantPool()".
     * <p>
     * Use this method if verification or even simple stack-height inference
     * would fail because the generated bytecode is unsafe.
     * @param maxStack the maximum height of the operand stack
     * @param maxLocals the maximum number of local variables
     * @return the edited code fragment
     */
    public CodeBuilder commit(CodeBuilder builder, char maxStack, char maxLocals) {

	// generate visitor that operates on normal instructions
	ByteBuffer code = generateBytecode();
	// use stackheight / local var count from arguments...
	builder.declareTemporaries(maxStack, maxLocals);
	builder.setCode(code);

	ehl_.commit(builder);
	copyAttributesAndUpdateLNTs(builder, code.limit());

	builder.setUnrefinedConstantPool(cPoolBuilder.unrefinedBuild());
	return builder;
    }


    // callback for Markers if their offsets change!
    void loopAgain() {
    }

    /**
     * Helper method for commit, computes the bytecode.
     */
    private ByteBuffer generateBytecode() {
        boolean dirty = dirty_;
        try {
            //first, append the old instructions that were not explicitly	    
            oldCode_.rewind();
            while (oldCode_.hasRemaining()) {
		
                oldCode_.get(); // done for its side effects
                int pc = oldCode_.getPC();
                if (!removed_[pc]) {
		    // ensure that the instruction has been copied
		    // into it's postCursor
		    getCursorAfterMarker(pc);
                }
            }
            // then: fixpoint iteration to determine new offsets of
	    // jump-targets
            int pos = new PredictOffsets().run(oldCode_, predictedOffsets_);

            // finally obtain new bytecode
            ByteBuffer newCode = ByteBuffer.allocate(pos);
            genCode(oldCode_, newCode); // side-effects buf!
            return newCode;
        } finally {
            dirty_ = dirty;
        }
    }
    
    private void genCode(InstructionBuffer oldCode, ByteBuffer newCode) {
 	int pos = 0;
 	oldCode.rewind();
 	while (oldCode.hasRemaining()) {
 	    oldCode.get(); // advances
 	    int pc = oldCode.getPC();
  	    Cursor gpre = preCursors_.get(pc);
 	    if (gpre != null) {
 		int predict = gpre.predictSize(pos);
 		gpre.write(newCode);
 		pos += predict;
 	    }
 	    if (predictedOffsets_[pc] != pos) 
 		fail("prediction wrong? "+predictedOffsets_[pc]+"(predicted) != (actual)"+pos);
 	    Cursor gpost = postCursors_.get(pc);
 	    if (gpost != null) {
 		int predict = gpost.predictSize(pos);
 		newCode.position(pos);
 		gpost.write(newCode);
 		pos += predict;
 	    }
 	}
     }
   
    private void copyAttributesAndUpdateLNTs(CodeBuilder builder, int max) {
        if (attrs_ == null)
            return;
        // do not forget that JVM Spec permits more than one LineNumberTable!
        for (int i = 0; i < attrs_.length; i++) {
            if (!(attrs_[i] instanceof Attribute.LineNumberTable)
		&& !(attrs_[i] instanceof Attribute.LocalVariableTable)
		&& !(attrs_[i] instanceof InlinedAttribute))
                builder.declareAttribute(attrs_[i]);
	    else
		builder.removeAttribute(attrs_[i]);
	}
	if (lineNumbers.size() > 0) {
	    Collections.sort(lineNumbers);
	    int[] startPC = new int[lineNumbers.size()];
	    int[] lineNumber = new int[lineNumbers.size()];
	    for (int i = 0; i < startPC.length; i++) {
		LineNumber ln = (LineNumber) lineNumbers.get(i);
		startPC[i] = ln.pc.getOffset();
		lineNumber[i] = ln.lineNo;
	    }
	    builder.declareAttribute(Attribute.LineNumberTable.make(startPC,
								    lineNumber));
	}
	if (localVars.size() > 0) {
	    char[] length = new char[localVars.size()];
	    char[] startPC = new char[length.length];
	    Descriptor.Field[] type = new Descriptor.Field[length.length];
	    int[] name = new int[length.length];
	    char[] index = new char[length.length];

	    for (int i = 0; i < length.length; i++) {
		LocalVar lv = (LocalVar) localVars.get(i);
		startPC[i] = (char) lv.startPC.getOffset();
		length[i] = (char) (lv.endPC.getOffset() - startPC[i]);
		name[i] = lv.name;
		type[i] = lv.type;
		index[i] = (char) lv.index;
	    }

	    builder.declareAttribute(new Attribute.LocalVariableTable
				     (startPC, length, name, type, index));
	}
	if (inlinedMethods.size() > 0) {
	    int[] length = new int[inlinedMethods.size()];
	    int[] startPC = new int[length.length];
	    Method[] m = new Method[length.length];

	    int i = 0;
	    for (Iterator it = inlinedMethods.iterator(); it.hasNext(); ) {
		InlinedMethod im = (InlinedMethod) it.next();
		startPC[i] = im.start.getOffset();
		length[i] = im.end.getOffset() - startPC[i];
		m[i++] = im.method;
	    }
	    
	    builder.declareAttribute(new InlinedAttribute(startPC, length, m));
	}
    }

    public void runVisitor(EditVisitor.Controller controller) {
        oldCode_.rewind(); // just to be sure
        controller.run(this);
    }

    /**
     * Flag that something has been edited. Package access: the Cursor can call
     * this.
     */
    void dirty() {
        dirty_ = true;
    }

    /**
     * Query whether this editor has been used to change any of the code.
     */
    public boolean wasEdited() {
        return dirty_;
    }

    /**
     * Class to run the predict offsets method on each of the original
     * instructions.
     * @author Christian Grothoff, and friend.
     */
    class PredictOffsets {

        /**
	 * @return the size of the generated code
	 */
        public int run(InstructionBuffer buf, int[] predictedOffsets) {
            boolean changed;
            int pos;
            do {
                changed = false;
                pos = 0;
                buf.rewind();
                while (buf.hasRemaining()) {
                    /*used for side effect*/ buf.get();
                    int pc = buf.getPC();
                    Cursor gpre = preCursors_.get(pc);
                    if (gpre != null)
                        pos += gpre.predictSize(pos);
                    if (predictedOffsets[pc] != pos) {
                        changed = true;
                        predictedOffsets[pc] = pos;
                        // the markers into old code jump here!
                    }
                    Cursor gpost = postCursors_.get(pc);
                    if (gpost != null)
                        pos += gpost.predictSize(pos);
                 }
            }
            while (changed);
            return pos;
        }

    } // end of PredictOffsets

   /**
     * This visitor infers maximum stack heights and number of local variables.
     * It assumes that the bytecode verifies. Especially, JSRs may not be
     * recursive (otherwise we may go crazy). KP: this must have been broken
     * for a while
     * @author Christian Grothoff
     */
    public static class MaxHeightInferenceVisitor extends Instruction.IVisitor {
        /**
	 * Index of the highest local variable ever used so far.
	 */
        private char maxLocals_;

        /**
	 * Maximum stack heights in the code for each pc.
	 */
        private char[] maxStacks_;

        /**
	 * Did we have a backward-branch with increased stack height? In that
	 * case, we must loop!
	 */
        boolean loop;

        /**
	 * Create an inference visitor to determine the max stack and max local
	 * counts for the given method.
	 * @param codeBuf the bytecode of the method
	 */
        MaxHeightInferenceVisitor(InstructionBuffer codeBuf) {
            super(codeBuf);
            maxStacks_ = new char[getCode().limit()];
            maxLocals_ = 0; /* !!! */
        }

        /**
	 * Run the analysis.
	 */
        void run() {
            loop = true;
            int loopc = 0;
            // CodePrettyPrinter pp = new CodePrettyPrinter(System.err);
            while (loop) {

                loopc++;
                if (loopc > 65536)
                    fail(
                        "MaxStackHeight inference failed."
                            + "The generated code does not verify.");
                loop = false;
                buf.rewind();
                while (buf.hasRemaining()) {
                    Instruction i = buf.get();
                    // pp.visitAppropriate(i);
                    this.visitAppropriate(i);
                }
            }
        }

        /**
	 * After running the analysis, this method returns the number of local
	 * variables used.
	 */
        char getMaxLocals() {
            return maxLocals_;
        }

        /**
	 * After running the analysis, this method returns the maximum stack
	 * height required.
	 */
        char getMaxStack() {
            char maxStack = 0;
            for (int i = maxStacks_.length - 1; i >= 0; i--)
                if (maxStack < maxStacks_[i])
                    maxStack = maxStacks_[i];
            return maxStack;
        }

        /**
	 * Record that the stack height between the given old and new pcs
	 * changes by delta. <br>If due to a backward branch (newPC &lt;
	 * oldPC) we need to iterate again, this method sets loop to true.
	 */
        private void stackChange(int pcDelta, int stackDelta) {
            int oldPC = getPC();
            int newPC = oldPC + pcDelta;
            //System.out.println("StackChange: " + oldPC + " x " + newPC +
            //	       " y " + delta + " z " + getPC());
            if (newPC < 0 || newPC >= maxStacks_.length) {
                // can this be valid for a trailing NOP?
                pln(
                    "possibly bad PC change "
                        + oldPC
                        + " to "
                        + newPC
                        + " stack changed by "
                        + stackDelta
                        + " in method "
                        + getSelector());
                return;
            }
            char nval = (char) (maxStacks_[oldPC] + stackDelta);
            if (nval > maxStacks_[newPC]) {
                maxStacks_[newPC] = nval;
                if (newPC < oldPC)
                    loop = true; // we must loop...
            }
        }

        /* ***************** and now: visit methods *************** */

        // I greatly trimmed down the number of visit methods after
        // streamlining the Instruction class hierarchy
        public void visit(Instruction i) {
            stackChange(i.size(buf), i.stackIns.length - i.stackOuts.length);
        }

        public void visit(Instruction.LocalAccess i) {
            visit((Instruction) i);
            char a = (char) i.getLocalVariableOffset(buf);
            if (a > maxLocals_)
                maxLocals_ = a;
        }
        public void visit(Instruction.Synchronization i) {
            stackChange(i.size(buf), 1);
        }
        public void visit(Instruction.Invocation i) {
            Selector.Method s = i.getSelector(buf, cp);
            Descriptor.Method d = s.getDescriptor();
            int returnTypeDiff = d.returnValueWordSize();
            // 0 = void, 1 = int, 2 = double
            stackChange(
                i.size(buf),
                returnTypeDiff - i.getArgumentLengthInWords(buf, cp));
            // args popped, return type pushed
            // getArgumentLengthInWords takes static/non-static into account!
        }
        public void visit(Instruction.Invocation_Quick i) {
            // We don't know what method is being called, but we can
            // conservatively approximate our stack usage by assuming
            // a wide return. A wide return only grows the stack by 1
            // because every quick invoke takes a receiver in addition
            // to args.
            //
            // Umm... So Invocation.getArgCount and
            // Invocation_Quick.getArgumentCount have different
            // semantics. I suppose it is a good thing that they have
            // different names too.
            stackChange(i.size(buf), 1 - i.getArgumentLengthInWords(buf));
        }
        public void visit(Instruction.INVOKE_NATIVE i) {
            // method unknown, worst case signature ()J
            stackChange(i.size(buf), 2);
        }
        public void visit(Instruction.INVOKE_SYSTEM i) {
            // FIXME the effect on the stack is truly unknown for
	    // INVOKE_SYSTEM,
            // but the worst case is currently WORD_OP.uI2L.
            stackChange(i.size(buf), 1);
        }
        public void visit(Instruction.Switch i) {
            stackChange(i.getDefaultTarget(buf), -1); // 1 popped!
            int[] targets = i.getTargets(buf);
            for (int j = 0; j < targets.length; j++)
                stackChange(targets[j], -1);
        }
        public void visit(Instruction.If i) {
            stackChange(i.size(buf), -1); // 1 popped!
            stackChange(i.getBranchTarget(buf), -1); // other target
        }
        public void visit(Instruction.IfCmp i) {
            //d("If statement at " +pc ": two targets: " + (pc+i.size(buf)) +
	    // " and " + (pc+i.getBranchTarget()));
            stackChange(i.size(buf), -2); // 2 popped!
            stackChange(i.getBranchTarget(buf), -2); // other target
        }
        public void visit(Instruction.UnconditionalJump i) {
            stackChange(i.getTarget(buf), 0);
        }
        public void visit(Instruction.NOP i) {
            stackChange(i.size(buf), 0);
        }
        public void visit(Instruction.GETFIELD i) {
            Selector.Field s = i.getSelector(buf, cp);
            Descriptor.Field f = s.getDescriptor();
            stackChange(i.size(buf), f.wordSize() - 1);
            // receiver popped, value pushed
            // need to look at selector to find out if wide or not!
        }
        public void visit(Instruction.PUTFIELD i) {
            Selector.Field s = i.getSelector(buf, cp);
            Descriptor.Field f = s.getDescriptor();
            stackChange(i.size(buf), -f.wordSize() - 1);
            // receiver popped, value popped
            // need to look at selector to find out if wide or not!
        }
        public void visit(Instruction.GETSTATIC i) {
            Selector.Field s = i.getSelector(buf, cp);
            Descriptor.Field f = s.getDescriptor();
            stackChange(i.size(buf), f.wordSize()); // value pushed
        }
        public void visit(Instruction.PUTSTATIC i) {
            Selector.Field s = i.getSelector(buf, cp);
            Descriptor.Field f = s.getDescriptor();
            stackChange(i.size(buf), -f.wordSize()); // value popped
        }
        public void visit(Instruction.MULTIANEWARRAY i) {
            stackChange(i.size(buf), 1 - i.getDimensions(buf));
        }
        public void visit(Instruction.MULTIANEWARRAY_QUICK i) {
            stackChange(i.size(buf), 1 - i.getDimensions(buf));
        }
        public void visit(Instruction.IRETURN i) {
            // end of flow
        }
        public void visit(Instruction.FRETURN i) {
            // end of flow
        }
        public void visit(Instruction.DRETURN i) {
            // end of flow
        }
        public void visit(Instruction.LRETURN i) {
            // end of flow
        }
        public void visit(Instruction.ARETURN i) {
            // end of flow
        }
        public void visit(Instruction.RETURN i) {
            // end of flow
        }
        public void visit(Instruction.JSR i) {
            stackChange(i.getTarget(buf), 1); // pushed jump target
        }
        public void visit(Instruction.JSR_W i) {
            stackChange(i.getTarget(buf), 1); // pushed jump target
        }
        public void visit(Instruction.RET i) {
            visit((Instruction.LocalAccess) i);
        }
        public void visit(Instruction.ATHROW i) {
            // should be handled by generic exception handler,
            // no next pc
        }
        public void visit(Instruction.WIDE i) {
            visitAppropriate(i.specialize(buf));
        }
    }

    /**
     * Interface to the exception handlers of this method. This interface
     * allows to delete handlers or to delete them. The order of exception
     * handlers matters for the JVM. The user-code is responsbile to make sure
     * the order makes sense.
     * @author Christian Grothoff
     */
    public static class ExceptionHandlerList {

        private final Marker startPC_;
        private final Marker endPC_;
        private final Marker targetPC_;
        private final TypeName.Scalar exceptionType_;
        private ExceptionHandlerList next_;
        private ExceptionHandlerList prev_;
        private boolean deleted_;

        /**
	 * Create another exception handler (otherwise empty list)
	 * @param startPC the Marker specifying where the block for this
	 *                handler starts (inclusive)
	 * @param endPC the Marker specifying where the block for this handler
	 *                ends (exclusive)
	 * @param targetPC the Marker specifying where the exception handler is
	 *                located
	 * @param exceptionType the type of exception that is caught at that
	 *                location
	 */
        ExceptionHandlerList(
            Marker startPC,
            Marker endPC,
            Marker targetPC,
            TypeName.Scalar exceptionType) {
            this.startPC_ = startPC;
            this.endPC_ = endPC;
            this.targetPC_ = targetPC;
            this.exceptionType_ = exceptionType;
        }
        /**
	 * Get the next exception handler in the list.
	 * @return null if this is the last handler
	 */
        public ExceptionHandlerList next() {
            return next_;
        }
        /**
	 * Get the previous exception handler in the list.
	 * @return null if this is the first handler
	 */
        public ExceptionHandlerList prev() {
            return prev_;
        }
        /**
	 * Delete this exception handler from the list. The reference remains
	 * valid and stays in the list. The handler is just marked as invalid
	 * and will not be written to the bytecode.
	 */
        public void delete() {
            deleted_ = true;
        }
        /**
	 * Un-does a delete operation.
	 */
        public void undelete() {
            deleted_ = false;
        }
        /**
	 * Was this exception handler deleted?
	 */
        public boolean isDeleted() {
            return deleted_;
        }
        /**
	 * What is the startPC (inclusive) of the block this handler catches
	 * exceptions in?
	 */
        public Marker getStart() {
            return startPC_;
        }
        /**
	 * What is the endPC (exclusive) of the block this handler catches
	 * exceptions in?
	 */
        public Marker getEnd() {
            return endPC_;
        }
        /**
	 * What is the PC of the handler (target on exception)?
	 */
        public Marker getTarget() {
            return targetPC_;
        }
        /**
	 * What type of exceptions is caught with this handler?
	 */
        public TypeName.Scalar getExceptionType() {
            return exceptionType_;
        }
        /**
	 * Create another exception handler and insert it before this one.
	 * @param startPC the Marker specifying where the block for this
	 *                handler starts (inclusive)
	 * @param endPC the Marker specifying where the block for this handler
	 *                ends (exclusive)
	 * @param targetPC the Marker specifying where the exception handler is
	 *                located
	 * @param exceptionType the type of exception that is caught at that
	 *                location. Must be a subtype of Throwable (not
	 *                checked!).
	 * @return a reference to the new handler
	 */
        public ExceptionHandlerList insertBefore(
            Marker startPC,
            Marker endPC,
            Marker targetPC,
            TypeName.Scalar exceptionType) {
            ExceptionHandlerList ehl =
                new ExceptionHandlerList(
                    startPC,
                    endPC,
                    targetPC,
                    exceptionType);
            ehl.next_ = this;
            ehl.prev_ = this.prev_;
            this.prev_ = ehl;
	    if (ehl.prev_ != null)
		ehl.prev_.next_ = ehl;
            return ehl;
        }
        /**
	 * Create another exception handler and insert it after this one.
	 * @param startPC the Marker specifying where the block for this
	 *                handler starts (inclusive)
	 * @param endPC the Marker specifying where the block for this handler
	 *                ends (exclusive)
	 * @param targetPC the Marker specifying where the exception handler is
	 *                located
	 * @param exceptionType the type of exception that is caught at that
	 *                location
	 * @return a reference to the new handler
	 */
        public ExceptionHandlerList insertAfter(
            Marker startPC,
            Marker endPC,
            Marker targetPC,
            TypeName.Scalar exceptionType) {
            ExceptionHandlerList ehl =
                new ExceptionHandlerList(
                    startPC,
                    endPC,
                    targetPC,
                    exceptionType);
            ehl.prev_ = this;
            ehl.next_ = this.next_;
            this.next_ = ehl;
            return ehl;
        }

        /**
	 * Take this list of ExceptionHandlers and add it to the code fragment.
	 * Attention: this method may be invoked on an object that is not the
	 * beginning of the list!
	 * @param builder the builder
	 */
        void commit(CodeBuilder builder) {
            builder.setExceptionHandlers(null); // kill old!
            ExceptionHandlerList hl = this;
            // move to the beginning of the list
            while (hl.prev_ != null)
                hl = hl.prev_;
            // write out targets
            while (hl != null) {
                if (!hl.isDeleted()) {
                    builder.declareExceptionHandler(new ExceptionHandler
                    // FIXME: instead of simple casts, do also range-check!
                    ((char) hl.startPC_.getOffset(),
                        (char) hl.endPC_.getOffset(),
                        (char) hl.targetPC_.getOffset(),
                        hl.exceptionType_));
                }
                hl = hl.next_;
            }
        }

    } // end of ExceptionHandlerList

    /**
     * An instruction was removed twice.
     */
    public static class DoubleRemoveError extends OVMError {
        DoubleRemoveError(String s) {
            super(s);
        }
    } // End of DoubleRemoveError

    /**
     * Attempt to remove an instruction at an offset that is not at the
     * beginning of an instruction.
     */
    public static class IllegalProgramPointError extends OVMError {
        IllegalProgramPointError(String s) {
            super(s);
        }
    } // end of IllegalProgramPointError

    private static class LineNumber implements Comparable {
	Marker pc;
	int lineNo;

	public int compareTo(Object other) {
	    return pc.getOffset() - ((LineNumber) other).pc.getOffset();
	}

	LineNumber(Marker pc, int lineNo) {
	    this.pc = pc;
	    this.lineNo = lineNo;
	}
    }

    private static class LocalVar {
	Marker startPC;
	Marker endPC;

	Descriptor.Field type;
	int name;
	int index;

	LocalVar(Marker startPC, Marker endPC,
		 Descriptor.Field type, int name, int index) {
	    this.startPC = startPC;
	    this.endPC = endPC;
	    this.index = index;
	    this.type = type;
	    this.name = name;
	}
    }

    private static class InlinedMethod {
	int startPC;
	Marker start;
	Marker end;
	Method method;

	InlinedMethod(int startPC, Marker start, Marker end, Method method) {
	    this.startPC = startPC;
	    this.start = start;
	    this.end = end;
	    this.method = method;
	}
    }
} // end of CodeFragmentEditor
