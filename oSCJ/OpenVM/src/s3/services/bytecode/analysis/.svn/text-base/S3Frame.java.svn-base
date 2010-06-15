/**
 * @file s3/services/bytecode/verifier/S3Frame.java
 **/
package s3.services.bytecode.analysis;


import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.Frame;
import ovm.services.bytecode.analysis.Heap;
import ovm.services.bytecode.analysis.AbstractExecutionError;

/**
 * Represents a call frame during the abstract interpretation of a Java
 * method.  Depending on the interpretation strategy chosen there may be
 * one or many frames active during the interepretation of a method. For a
 * standard interpreter a single frame per method is sufficient, a
 * monovariant analysis may require a single frame per program point, while
 * a polyvariant analysis may require several frames per program point.<p>
 * A Frame stores information about the content of the Java stack and local
 * variables in the form of <code>AbstractValues</code>.<p> A Frame can be
 * manipulated by storing/loading values to variables and pushing/poping
 * values from the stack.  This interface does not provide a direct way to
 * manipulate wide primitive types such as double and long. The
 * understanding that a wide value is represented as a pair of values on
 * the stack, an <code>AbstractValue</code> of the appropriate type and an
 * <code>Invalid</code> object.<p>
 *
 * For abstract interpretation (verification, program analysis) frames are
 * partially ordered. The <code>includes</code> relation specifies if a
 * Frame contains as much information as another Frame. If
 * <code>a.includes(b)</code> then <code>a.mergeb)</code> should be equal
 * to <code>a</code>. For most implementations the partial order on frame
 * is induced by the partial order on values. The merge operation returns
 * an upper bound of both frames.
 *
 * @see ovm.services.bytecode.analysis.State
 * @see ovm.services.bytecode.analysis.AbstractValue
 * @see ovm.core.execution.Interpreter
 * @author Christian Grothoff
 * @author Jan Vitek
 **/
public class S3Frame
    implements Frame {

    private static int ID_COUNTER = 0;
    private final int ID = ID_COUNTER++;

    /**
     * The operand stack.
     **/
    protected final AbstractValue[] opStack_;

    /**
     * The local variables.
     **/
    protected final AbstractValue[] locals_;

    /**
     * Current height of the operand stack.
     **/
    protected int stackHeight_;
 
    private Heap heap_;

    private int lastPC_;
   
    public S3Frame(int maxStack,
		   int maxLocals,
		   Heap heap) {
	this.opStack_ = new AbstractValue[maxStack];
	this.locals_ = new AbstractValue[maxLocals];
	this.heap_ = heap;
	this.lastPC_ = -1;
    }

    public int getLastPC() {
	return lastPC_;
    }

    public void setLastPC(int pc) {
	lastPC_ = pc;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer(100);
	sb.append("FRAME ");
	sb.append(ID);
	sb.append("; LOCALS: ");
	for (int i=0;i<locals_.length;i++) {
	    // It is important to skip nulls when inlining with
	    // disjoint 
	    if (locals_[i] != null) {
		sb.append(i);
		sb.append(":");
		sb.append(locals_[i]);
		sb.append(" ");
	    }
	}
	sb.append(" || STACK ");
	sb.append(stackHeight_);
	sb.append(": ");
	for (int i=0;i<stackHeight_;i++) {
	    sb.append(opStack_[i]);
	    sb.append(" ");
	}
	return sb.toString() + " heap: " + heap_;
    }

    // I think this is more readable -HY
    public String toMultiLineString() {
	StringBuffer sb = new StringBuffer(100);
	sb.append("FRAME ");
	sb.append(ID);
	sb.append(":\n\tLOCALS: \n");
	for (int i=0;i<locals_.length;i++) {
	    sb.append("\t");
	    sb.append(i);
	    sb.append(":");
	    sb.append(locals_[i]);
	    sb.append("\n");
	}
	sb.append("\n\tSTACK ");
	sb.append(stackHeight_);
	sb.append(": \n");
	for (int i=0;i<stackHeight_;i++) {
	    sb.append("\t" + i + ":");
	    sb.append(opStack_[i]);
	    sb.append("\n");
	}
	return sb.toString() + "\theap: " + heap_;
    }

    protected S3Frame(AbstractValue[] opStack,
		      AbstractValue[] locals,
		      int stackHeight,
		      Heap heap) {
	this.opStack_ = opStack;
	this.locals_ = locals;
	this.stackHeight_ = stackHeight;
	this.heap_ = heap;
    }

    public Frame make(AbstractValue[] opStack,
		      AbstractValue[] locals,
		      int stackHeight,
		      Heap heap) {
	return new S3Frame(opStack, locals, stackHeight, heap);
    }

    // --------------- Comparisons ---------------------

    /**
     * Compares two frames for semantic equality. This should return true
     * if <code>this.include(frame)</code> and
     * <code>frame.include(this)</code> hold.
     **/
    public boolean compareFrames(Frame frame) {
	S3Frame s3frame = (S3Frame) frame;
	AbstractValue[] lb = s3frame.locals_;
	AbstractValue[] mb = this.locals_;
	if (stackHeight_ != s3frame.stackHeight_)
	    return false;
	for (int i=mb.length-1;i>=0;i--) {
	    AbstractValue a = mb[i];
	    AbstractValue b = lb[i];
	    if ((a == null) && (b == null))
		continue;
	    if ((a == null) || (b == null))
		return false;
	    if (!a.equals(b))
		return false;
	}
	lb = s3frame.opStack_;
	mb = this.opStack_;
	for (int i=stackHeight_-1;i>=0;i--) {
	    AbstractValue a = mb[i];
	    AbstractValue b = lb[i];
	    if ((a == null) && (b == null))
		continue;
	    if ((a == null) || (b == null))
		return false;
	    if (!a.equals(b))
		return false;
	}
	return heap_.equals(s3frame.heap_);
    }

    /**
     * Compares two frames with respect to the partial order. Frames with
     * different stack heights can not be related.
     * @param frame to compare with
     * @return true if this frame is more informative than the argument.
     **/
    public boolean includes(Frame frame) {
	S3Frame s3frame = (S3Frame) frame;
	if (this == s3frame)
	    return true;
	AbstractValue[] lb = s3frame.locals_;
	AbstractValue[] mb = this.locals_;
	if (stackHeight_ != s3frame.stackHeight_)
	    return false;
	for (int i=mb.length-1;i>=0;i--) {
	    AbstractValue a = mb[i];
	    AbstractValue b = lb[i];
	    if (a == null)
		continue; /* if it works for null, it works for everything */
	    if (b == null)
		return false; /* if a is not null, it may not work for null */
	    if (!a.includes(b))
		return false;
	}
	lb = s3frame.opStack_;
	mb = this.opStack_;
	for (int i=stackHeight_-1;i>=0;i--) {
	    AbstractValue a = mb[i];
	    AbstractValue b = lb[i];
	    if (a == null)
		continue;
	    if (b == null)
		return false;
	    if (!a.includes(b))
		return false;
	}
	return heap_.includes(s3frame.heap_);
    }

    /**
     * Merges the information contained in two stack frames. This returns
     * an upper bound that includes both stack frames. The implementation
     * returns <code>null</code> if the merge fails.  The current frame
     * is NOT modified IF THE MERGE FAILS.
     * @param frame the other frame
     * @return new merged frame or <code>null</code> if the merge fails.
     **/
    public Frame merge(Frame frame) {
  	S3Frame s3frame = (S3Frame) frame;
	AbstractValue[] lb = s3frame.locals_;
	AbstractValue[] mb = this.locals_;
	if (stackHeight_ != s3frame.stackHeight_)
	    return null;
	AbstractValue[] nb = new AbstractValue[mb.length];
	for (int i=0;i<mb.length;i++) {
	    AbstractValue a = mb[i];
	    AbstractValue b = lb[i];
	    if ((a == null) || (b == null))
		continue;
	    if (null == (nb[i] = a.merge(b)))
		return null;
	}
	lb = s3frame.opStack_;
	mb = this.opStack_;
	AbstractValue[] os = new AbstractValue[mb.length];
	for (int i=stackHeight_-1;i>=0;i--) {
	    AbstractValue a = mb[i];
	    AbstractValue b = lb[i];
	    if ((a == null) || (b == null))
		continue;
	    if (null == (os[i] = a.merge(b)))
		return null;
	}
	Heap hp = heap_.merge(s3frame.heap_);
	if (hp == null)
	    return null;
	return make(os, nb, stackHeight_, hp);
    }    

    public Heap getHeap() {
	return heap_;
    }

    public void setHeap(Heap h) {
	this.heap_ = h;
    }

    // ------------- Update ----------------------------------

    /**
     * Loads an AbstractValue from a local variable. If the index is out of
     * range for this frame <code>Invalid</code> will be returned. For wide
     * primitives two local variables slots will be used.
     * @see ovm.services.bytecode.analysis.AbstractValue.Invalid
     * @param index the local variable
     * @return the AbstractValue at that location.
     **/
    public AbstractValue load(int index) {
	return locals_[index];
    }
    
    /**
     * Stores an AbstractValue in a local variable. An implementation
     * should handle accesses out of range in an appropriate way. For wide
     * primitives two local variable slots will be used.
     * @param index the local variable
     * @param value the object to store
     **/
    public void store(int index, AbstractValue value) {
	locals_[index] = value;
    }

    /**
     * Pushes an abstract value on this frame's stack. An implementation
     * should handle stack overflow in an appropriate way.  This interface
     * does not provide a direct way to manipulate wide primitive types
     * such as double and long. The understanding that a wide value is
     * represented as a pair of values on the stack, an
     * <code>AbstractValue</code> of the appropriate type and an
     * <code>Invalid</code> object.
     * @param value the object to push
     **/
    public void push(AbstractValue value) {
	    if (value != null && value.isInvalid())
		return; // VOID, not pushed
	try {
	    opStack_[stackHeight_] = value;
	    stackHeight_++;
	} catch (ArrayIndexOutOfBoundsException aiob) {
	    throw new AbstractExecutionError("Stack Overflow, " +
					"maximum stack height is " 
					+ opStack_.length + 
					" and current frame is " 
					+ this.toString());
	}
    }

    /**
     * Push wide.
     **/
    public void pushWide(AbstractValue value) {
	try {
	    opStack_[stackHeight_] = value;
	    opStack_[stackHeight_+1] = null;// invalid!
	    stackHeight_ += 2;
 	} catch (ArrayIndexOutOfBoundsException aiob) {
	    throw new AbstractExecutionError("Stack Overflow, maximum stack " +
					"height is " + opStack_.length);
	}
    }

    /**
     * Pops an abstract value from this frame's stack. An implementation
     * should handle stack overflow in an appropriate way.  This interface
     * does not provide a direct way to manipulate wide primitive types
     * such as double and long. The understanding that a wide value is
     * represented as a pair of values on the stack, an
     * <code>AbstractValue</code> of the appropriate type and an
     * <code>Invalid</code> object.
     **/
    public AbstractValue pop() {
	try {
	    return opStack_[--stackHeight_];
 	} catch (ArrayIndexOutOfBoundsException aiob) {
	    throw new AbstractExecutionError("Stack Underflow (pop)");
	}
    }

    public AbstractValue popWide() {
	if (opStack_[--stackHeight_] != null)
	    throw new AbstractExecutionError("popWide can not be applied " +
					"to current stack");
	try {
	    return opStack_[--stackHeight_];
 	} catch (ArrayIndexOutOfBoundsException aiob) {
	    throw new AbstractExecutionError("Stack Underflow (popWide)");
	}
    }

    /**
     * Set the abstract value that is at depth deep of the
     * operand stack. For deep == 0, set the top of the
     * stack. 
     **/
    public void poke(int deep,
		     AbstractValue av) {
	try {
	    opStack_[stackHeight_ - deep - 1] = av;
	} catch (ArrayIndexOutOfBoundsException aiob) {
	    throw new AbstractExecutionError("Stack Underflow (poke)");
	}
    }

    /**
     * Get the abstract value that is at depth deep of the
     * operand stack. For deep == 0, return the top of the
     * stack. Does not change the frame itself.
     **/
    public AbstractValue peek(int deep) {
	try {
	    return opStack_[stackHeight_ - deep - 1];
	} catch (ArrayIndexOutOfBoundsException aiob) {
	    throw new AbstractExecutionError("Stack Underflow (peek)");
	}
    }

    public int getMaxLocals() {
	return locals_.length;
    }

    public int getMaxStack() {
	return opStack_.length;
    }

    /**
     * Returns the height of the stack.
     **/
    public int getStackHeight() {
	return stackHeight_;
    }

    /**
     * Clears the stack and sets the height to zero.
     **/
    public void clearStack() {
	stackHeight_ = 0;
    }


    // --------------------------- Assorted ---------------------------
    
    /**
     * Type specific variant of <code>clone</code> that returns a copy of
     * this Frame. This method preserves sharing of AbstractValues.
     **/
    public Frame cloneFrame() {
	return make((AbstractValue[])opStack_.clone(),
		    (AbstractValue[])locals_.clone(),
		    stackHeight_,
		    heap_.cloneHeap());
    }

    public static class Factory 
	implements Frame.Factory {

	private final Heap.Factory heapFactory;

	public Factory(Heap.Factory hf) {
	    this.heapFactory = hf;
	}

	/**
	 * Creates a new Frame. The initial stack height is zero, the stack
	 * height is allowed to vary between 0 and
	 * stackSize. Implementations may have different policies wrt
	 * initialization of the contents of the stack with default values.
	 *
	 * @param locals number of local variables
	 * @param stackSize  max stack height
	 **/
	public Frame makeFrame(int locals, int stackSize) {
	    return new S3Frame(locals, stackSize, heapFactory.makeHeap());
	}

    }

} // end of S3Frame











