package ovm.util;

public abstract class Buffer extends ovm.core.OVMBase {
    /**
     * THE value for an unset mark.
     * @see #mark
     **/
    protected static final int INVALID_MARK = -1;

    /**
     * This is a sort of bookmark for a position
     **/
    protected int mark = INVALID_MARK;

    public abstract int position();
    public abstract Buffer position(int newPosition);
    public abstract int capacity();
    public abstract Buffer limit(int newLimit);
    public abstract int limit();

    public abstract int remaining();
    
    public boolean hasRemaining() {
	return remaining() > 0;
    }

    /**
     * Return the position cursor to the mark value (see {@link #mark}).
     * This also resets the mark value to the <code>INVALID_MARK</code>
     * value. Attempting to reset when the mark value has never been
     * set will cause an exception.
     * @return this byte buffer with the position reset and the mark
     *         cleared.
     **/
    public Buffer reset() {
	if (mark == INVALID_MARK) {
	    throw new OVMError("should be: InvalidMarkException");
	}
	position(mark);
	mark = INVALID_MARK;
	return this;
    }

    /**
     * JDK docs say to clear the buffer, set the position to zero and
     * the limit to capacity and to discard the mark.
     *
     * Except for setting the limit to capacity (how?),
     * we follow these semantics.
     **/
    public Buffer clear() {
	mark = INVALID_MARK;
	limit(capacity());
	position(0);
	return this;
    }

    /**
     * Set the mark value to the current position. (Mark functions
     * as a place marker for the current position and can be
     * returned to by using the {@link #reset} method.
     * @return this byte buffer with the mark set
     **/
    public Buffer mark() {
	mark = position();
	return this;
    }


    /**
     * Return the <code>ByteBuffer</code> to its initial state,
     * resetting the current position to the beginning of the
     * buffer and removing any mark which may exist.
     * @return the buffer, with mark and position reset
     **/
    public final Buffer rewind() {
	mark = INVALID_MARK;
	position(0);
	return this;
    }
    /**
     * Declare the end (limit) of the byte buffer to be whatever the current
     * position is, reset the position cursor to 0, and remove any
     * marks which may exist.
     * @return the buffer, with its limit value changed and its mark
     *         and position values reset
     * @see #rewind
     **/
    public abstract Buffer flip();


    public Buffer advance(int delta) {
	position(position() + delta);
	return this;
    }

}
