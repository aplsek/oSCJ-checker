package ovm.util;

/**
 * A ByteBuffer for which {@link #array} is guaranteed to always
 * return the same fixed-sized byte array.  FixedByteBuffers are
 * returned by {@link ByteBuffer#wrap(byte[])}.  This public subtype
 * of ByteBuffer is not present in the java.nio API, but it comes in
 * handy in VM implementation code.
 **/
public class FixedByteBuffer extends ByteBuffer implements Cloneable {

    public ByteBuffer duplicate() {
      	try {
	    return (ByteBuffer)clone();
	}
	catch ( CloneNotSupportedException cnse ) {
	    throw failure( cnse);
	}
    }
    
    /**
     * The byte data from which this data stream should be created
     **/
    protected byte[] data; // initialize 
    private int limit;
    private final int offset;
    private final int capacity;
    
    /**
     * Creates a new byte data stream from the specified byte array.
     * @param dt the input bytes.
     */
    FixedByteBuffer(byte[] dt) { 
	data = dt; 
	offset = 0;
	limit = data.length - offset;
	capacity = data.length;
    }

    private FixedByteBuffer(byte[] data, int offset, int available) {
	this.data = data;
	this.offset = offset;
	this.limit = available;
	this.capacity = available;
    }

    /**
     * Get the limit (in bytes) of this <code>ByteBuffer</code>
     * @return the end position of this byte buffer
     **/
    public final int limit() {
	return limit;
    }

    public Buffer limit(int newLimit) {
	if ((newLimit < 0) || (newLimit > capacity())) {
	    throw new IllegalArgumentException();
	}
	this.limit = newLimit;
	return this;
    }

    public int capacity() {
	return capacity;
    }

    public boolean hasArray() { return true; }
    public byte[] array() { return data; }
    public int arrayOffset() { return offset; } 
    

    /**
     * Declare the end (limit) of the byte buffer to be whatever the current
     * position is, reset the position cursor to 0, and remove any
     * marks which may exist.
     * @return the buffer, with its limit value changed and its mark
     *         and position values reset
     * @see #rewind
     **/
    public final Buffer flip() {
	limit = pos;
	return rewind();
    }

    /**
     * Reads a signed 8-bit value from this byte data stream at the indicated
     * byte index. If the byte read is <code>b</code>, where
     * 0&nbsp;&lt;=&nbsp;<code>b</code>&nbsp;&lt;=&nbsp;255, then the
     * result is: <ul><code> (byte)(b) </code></ul>
     * @param i the index from which the data should be read
     * @return byte <code>i</code> of this input stream as a signed 8-bit
     *         <code>byte</code>.
     * @throws IndexOutOfBoundsException if the input index is out of bounds
     **/
    public byte get(int i) { 
	if (i >= limit()) {
	    throw new IndexOutOfBoundsException("tried to access " 
					       + i + " last valid " 
					       + (limit() - 1));
	}
	return data[offset + i]; 
    }

    protected void getNoAdvance(byte[] array, int start, int length) {
	if (length > remaining()) {
	    throw new BufferUnderflowException("tried reading " + length
					      + " but remaining " 
					      + remaining());
	}

	System.arraycopy(data, offset + pos, array, start, length);
    }

    protected void putNoAdvance(byte[] array, int start, int length) {
	ensureAvailable(length, position());
	System.arraycopy(array, start, data, offset + pos, length);
    }

    /**
     * Writes a signed 8-bit value to this byte data stream at the
     * given index..
     * @param index the position to which the byte should be written
     * @param b the byte to write
     * @return the newly modified buffer
     * @throws BufferOverflowException if there is an attempt to write
     *                                 beyond the end of the buffer
     **/
    public ByteBuffer put(int index, byte b) {
	ensureAvailable(1, index);
	data[offset + index] = b; 
	return this;
    }

    /**
     * Reads a signed 16-bit number from this byte data stream at the indicated
     * index. The method reads two bytes from the underlying input stream.
     * @param      index the position in the byte data stream from which to 
     *                   read
     * @return     the indicated bytes of this input stream, interpreted as a
     *             signed 16-bit number.
     **/
    public short getShort(int index) {
	int high;
	int low;
	if (isLittleEndian) {
	    high = data[offset + index + 1] & 0xff;
	    low  = data[offset + index + 0] & 0xff;
	} else {
	    high = data[offset + index + 0] & 0xff;
	    low  = data[offset + index + 1] & 0xff;
	}
	return (short)((high << 8) + low);
    }


    /**
     * Writes a signed 16-bit number to this byte data stream at the indicated
     * cursor position. The method writes two bytes to the underlying input 
     * stream.
     * @param index the position at which to write the number
     * @param value the value to write
     * @return the newly modified byte buffer    
     **/
    public ByteBuffer putShort(int index, short value) {
	ensureAvailable(2, index);
	byte high = (byte)((value >>> 8) & 0xff);
	byte low =  (byte)((value >>> 0) & 0xff);
	if (isLittleEndian) {
	    data[offset + index + 0] = low;
	    data[offset + index + 1] = high;
	} else {
	    data[offset + index + 0] = high;
	    data[offset + index + 1] = low;
	}
	return this;
    }

    protected void ensureAvailable(int size, int aPos) {
	if (aPos + size > limit() ) {
	    throw new BufferOverflowException("tried writing " + size
					       + " but limit " 
					       + limit());
	}
    }

    /**
     * Writes an unsigned 16-bit number to this byte data stream at the
     * indicated position.
     * @param index the index into the byte stream at which the char should
     *              be written
     * @param value the char to be written
     * @return the newly modified buffer
     **/    
    public ByteBuffer putChar(int index, char value) {
	ensureAvailable(2, index);
	byte high = (byte)((value >>> 8) & 0xff);
	byte low =  (byte)((value >>> 0) & 0xff);
	if (isLittleEndian) {
	    data[offset + index + 0] = low;
	    data[offset + index + 1] = high; 
	} else {
	    data[offset + index + 0] = high;
	    data[offset + index + 1] = low; 
	}
	return this;
    }

    /**
     * Reads an unsigned 16-bit number from this byte data stream.
     * @param      index   the position in the byte data stream
     * @return     the next two bytes of this input stream as a char.
     **/
    public char getChar(int index)  {
	int high;
	int low;
	if (isLittleEndian) {
	    high = data[offset + index + 1] & 0xff;
	    low  = data[offset + index + 0] & 0xff;
	} else {
	    high = data[offset + index + 0] & 0xff;
	    low  = data[offset + index + 1] & 0xff;
	}
	return (char)((high   << 8) + low);
    }


    /**
     * Reads a signed 32-bit integer from this byte data stream starting
     * at the given index. This method reads four bytes from the underlying 
     * byte stream.
     * @param index the position in the byte data stream from which to read
     * @return      the next four bytes of this input stream, interpreted as an
     *              <code>int</code>.
     **/
    public int getInt(int index)  {
	int lowest;
	int low;
	int high;
	int highest;
	if (isLittleEndian) {
	    lowest =  data[offset + index + 0] & 0xff;
	    low =     data[offset + index + 1] & 0xff;
	    high =    data[offset + index + 2] & 0xff;
	    highest = data[offset + index + 3] & 0xff;
	} else {
	    lowest =  data[offset + index + 3] & 0xff;
	    low =     data[offset + index + 2] & 0xff;
	    high =    data[offset + index + 1] & 0xff;
	    highest = data[offset + index + 0] & 0xff;
	}
	return (highest << 24) | (high << 16) | (low << 8) | lowest;
    }

    /**
     * Writes a signed 32-bit integer to this byte data stream starting
     * at the given index. This method writes four bytes from the underlying 
     * byte stream.
     * @param index the index at which to begin writing
     * @param value the value to write
     * @return the newly modified byte buffer
     **/
    public ByteBuffer putInt(int index, int value)  {
	ensureAvailable(4, index);
	byte lowest =   (byte)((value >>> 0) & 0xff);
	byte low =      (byte)((value >>> 8) & 0xff);
	byte high =     (byte)((value >>> 16) & 0xff);
	byte highest =  (byte)((value >>> 24) & 0xff);

	if (isLittleEndian) {
	    data[offset + index + 0] = lowest;
	    data[offset + index + 1] = low;
	    data[offset + index + 2] = high;
	    data[offset + index + 3] = highest;
	} else {
	    data[offset + index + 3] = lowest;
	    data[offset + index + 2] = low;
	    data[offset + index + 1] = high;
	    data[offset + index + 0] = highest;
	}
	return this;
    }

    /**
     * Find the last position within the the indicated range
     * that the given character occurs in the buffer (note that
     * this involves casting individual <code>bytes</code> to
     * <code>chars</code> and then comparing).
     * @param c the character being searched for
     * @param start the first index to search
     * @param end the last index to search
     * @return the last position at which <code>c</code> occurs,
     *         or -1 if not found.
     * @throws BufferOverflowException if the array size is
     *         exceeded at any point.
     **/
    public int findLastOffsetOf(char c, int start, int end) {
	int i = end - 1;
	if (i <= limit())
	    throw new BufferOverflowException("tried to access " + i +
					      " last valid " + (limit() - 1));
	for ( ; i >= start; i--) {
	    // the end is not included in the range!
	    if ((char)data[offset + i] == c) return i;
	}
	return -1;
    }

    /**
     * Find the first offset of the char <code>c</code> starting from
     * the current position. Returns the offset of <code>c</code> or
     * <code>-1</code> if not found.
     * @param c the character being searched for
     * @return the position of the first occurance <code>c</code> after
     *         the current cursor postion, or -1 if not found.
     **/
    public int findFirstOffsetOf(char c) {
	int end = limit();
	for (int i = pos; i < end; i++) {
		if ((char)data[offset + i] == c) return i;
	}
	return -1;
    }


    public  ByteBuffer slice() {
	return new FixedByteBuffer(data, position(), remaining());
    }
    
}
