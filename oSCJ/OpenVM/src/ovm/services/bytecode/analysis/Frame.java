/**
 * @file ovm/services/bytecode/analysis/Frame.java 
 **/
package ovm.services.bytecode.analysis;


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
 * <code>a.includes(b)</code> then <code>a.merge(b)</code> should be equal
 * to <code>a</code>. For most implementations the partial order on frame
 * is induced by the partial order on values. The merge operation returns
 * an upper bound of both frames.
 *
 * @see State
 * @see AbstractValue
 * @author Christian Grothoff
 * @author Jan Vitek
 **/
public interface Frame {
    // BTW: we could make this a 'public abstract class' instead to protect
    // ourselves from whimpy implementations of invokeinterface.

    // --------------- Comparisons --------------------- 

    /**
     * Compares two frames for semantic equality. This should return true
     * if <code>this.include(frame)</code> and
     * <code>frame.include(this)</code> hold.
     **/
    public boolean compareFrames(Frame frame);

    /**
     * Compares two frames with respect to the partial order. Frames with
     * different stack heights can not be related.
     * @param frame to compare with
     * @return true if this frame is more informative than the argument.
     **/
    public boolean includes(Frame frame);

    /**
     * Merges the information contained in two stack frames.  This returns
     * an upper bound that includes both stack frames.  The implementation
     * returns <code>null</code> if the merge fails.  The current frame
     * is NOT modified IF THE MERGE FAILS.
     * @param frame the other frame
     * @return new merged frame or <code>null</code> if the merge fails.
     **/
    public Frame merge(Frame frame);

    public int getLastPC();
    public void setLastPC(int i);

    // ------------- Update ----------------------------------

    /**
     * Loads an AbstractValue from a local variable. If the index is out of
     * range for this frame <code>Invalid</code> will be returned. For wide
     * primitives two local variables slots will be used.
     * @see AbstractValue.Invalid
     * @param index the local variable
     * @return the AbstractValue at that location.
     **/
    public AbstractValue load(int index);
    
    /**
     * Stores an AbstractValue in a local variable. An implementation
     * should handle accesses out of range in an appropriate way. For wide
     * primitives two local variable slots will be used.
     * @param index the local variable
     * @param value the object to store
     **/
    public void store(int index, AbstractValue value);

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
    public void push(AbstractValue value);

    /**
     * Push wide. 
     **/
    public void pushWide(AbstractValue value);

    /**
     * Pops an abstract value from this frame's stack. An implementation
     * should handle stack underflow in an appropriate way.  Wide values
     * are represented as a pair of values on the stack, an
     * <code>AbstractValue</code> of the appropriate type and an
     * <code>Invalid</code> object and can be accessed as such via pop.
     **/
    public AbstractValue pop();
 
    /**
     * Pops an abstract wide value from this frame's stack. An implementation
     * should handle stack underflow and the fact that there may not be a wide
     * AbstractValue on the Stack in an appropriate way. 
     **/
    public AbstractValue popWide();

    /**
     * Get the abstract value that is at depth deep of the
     * operand stack. For deep == 0, return the top of the
     * stack. Does not change the frame itself.
     **/
    public AbstractValue peek(int deep);

    /**
     * Set the abstract value that is at depth deep of the
     * operand stack. For deep == 0, set the top of the
     * stack. 
     **/
    public void poke(int deep,
		     AbstractValue av);

    /**
     * Returns the height of the stack.
     **/
    public int getStackHeight();


    /**
     * Clears the stack and sets the height to zero.
     **/
    public void clearStack();


    public Heap getHeap();

    public void setHeap(Heap h);
    

    // --------------------------- Assorted ---------------------------
    
    /**
     * Type specific variant of <code>clone</code> that returns a copy of
     * this Frame. This method preserves sharing of AbstractValues.
     **/
    public Frame cloneFrame();

    /**
     * Creates new Frames.
     * @author Christian Grothoff
     **/
    public interface Factory {
	/**
	 * Creates a new Frame. The initial stack height is zero, the stack
	 * height is allowed to vary between 0 and
	 * stackSize. Implementations may have different policies wrt
	 * initialization of the contents of the stack with default values.
	 *
	 * @param locals number of local variables
	 * @param stackSize  max stack height
	 **/
	public Frame makeFrame(int locals, int stackSize);

    } // End of Frame.Factory

} // end of Frame











