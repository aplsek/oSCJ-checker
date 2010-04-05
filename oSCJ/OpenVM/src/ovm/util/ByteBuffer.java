/* $header/SSS/cvs/OpenVM/src/ovm/util/ByteBuffer.java,v 1.28.2.2 2002/09/19 06:03:12 jv Exp $ */

package ovm.util;
import ovm.core.execution.NativeConstants;

/**
 * <code>ByteBuffer</code> is a byte data stream based on the
 * <code>java.nio</code> buffer, though it is not not compatible with that
 * class as it has extra functionality.
 **/
public abstract class ByteBuffer 
    extends Buffer  {

    /**
     * The content of the new buffer will be that of this buffer. Changes to
     * this buffer's content will be visible in the new buffer, and vice versa;
     * the two buffers' position, limit, and mark values will be independent.
     * @return a shallow copy of this ByteBuffer
     **/
    public abstract ByteBuffer duplicate();
    
    
    /**
     * Current cursor position within the buffer
     **/
    protected int pos = 0;


    /**
     * True if this bytebuffer is little-endian, otherwise false
     **/
    protected boolean isLittleEndian = false;


    /**
     * True if the 32-bit words of double values are swapped
     * with respect to what would be normally expected.
     * For instance, on the ARM each word of a double is
     * ordered in little-endian mode, but the two words are
     * ordered in big-endian order (swapped respect to x86).
     **/
    protected boolean doubleWordsSwapped = false;

    /**
     * Allocate a new <code>ByteBuffer</code> of the specified capacity.
     * @param capacity the number of bytes to allocate for the data portion
     *        of the new <code>ByteBuffer</code>
     * @return the new <code>ByteBuffer</code>
     **/
    public static ByteBuffer allocate(int capacity) {
	if (capacity == Integer.MAX_VALUE) {
	    return new GrowableByteBuffer();
	} else {
	    return new FixedByteBuffer(new byte[capacity]);
	}
    }

    /**
     * For now, same as "allocate".  JDK distinguishes between
     * direct and non-direct buffers, OVM currently does not.
     *
     * @param capacity the number of bytes to allocate for the data portion
     *        of the new <code>ByteBuffer</code>
     * @return the new <code>ByteBuffer</code>
     **/
    public static ByteBuffer allocateDirect(int capacity) {
	if (capacity == Integer.MAX_VALUE) {
	    return new GrowableByteBuffer();
	} else {
	    return new FixedByteBuffer(new byte[capacity]);
	}
    }

    /**
     * Given a byte array, wrap it in a <code>ByteBuffer</code> object.
     **/
    public static ByteBuffer wrap(byte[] data) {
	return new FixedByteBuffer(data);
    }


    public abstract int limit();
    
    public abstract Buffer limit(int newLimit);

    /**
     * Return the capacity of this <code>ByteBuffer</code> (i.e. the
     * maximum number of bytes stored within the data portion)
     * @return the capacity of this <code>ByteBuffer</code>
     **/
    public abstract int capacity();

    /**
     * Determine, from the current position, how much of the 
     * <code>ByteBuffer</code> is left until we reach the end.
     * @return the size, in bytes, of the remaining portion of the
     *         buffer
     **/
    public int remaining() {
	return limit() - pos;
    }

    public abstract boolean hasArray();
    public abstract byte[] array();
    public abstract int arrayOffset();

    
    /**
     * Determine if the byte order of this buffer is
     * little-endian or big-endian.
     * @return <code>NativeConstants.LITTLE_ENDIAN</code> or
     *         <code>NativeConstants.BIG_ENDIAN</code> or
     *         <code>NativeConstants.LITTLE_ENDIAN_DOUBLES_SWAPPED</code>
     * as appropriate.
     * @see NativeConstants
     **/
    public int order() {
	if (isLittleEndian) {
	    return doubleWordsSwapped ?
		NativeConstants.LITTLE_ENDIAN_DOUBLES_SWAPPED :
		NativeConstants.LITTLE_ENDIAN;
	} else {
	    return NativeConstants.BIG_ENDIAN;
	}
    }

    /**
     * Set the byte order of this <code>ByteBuffer</code> to
     * the input value: <code>NativeConstants.LITTLE_ENDIAN</code>,
     * or <code>NativeConstants.BIG_ENDIAN</code> respectively.
     * @return this byte buffer, with the byte order value now
     *         set
     **/
    public ByteBuffer order(int o) {
	if (o == NativeConstants.LITTLE_ENDIAN) {
	    isLittleEndian = true; doubleWordsSwapped = false;
	} else if (o == NativeConstants.LITTLE_ENDIAN_DOUBLES_SWAPPED) {
	    isLittleEndian = true; doubleWordsSwapped = true;
	} else {
	    isLittleEndian = false; doubleWordsSwapped = false;
	}
	return this;
    }

    /**
     * Return the cursor position on this buffer.
     * @return the position value of the buffer
     **/
    public int position() {
	return pos;
    }


    /**
     * Set the position of this byte buffer to the specified
     * byte index.
     * @return this byte buffer, with the position cursor set to the
     *         new position
     * @throws OVMError.IllegalArgument if the new
     *         position indicated is out of bounds.
     **/
    public Buffer position(int newPosition) {
	if (newPosition > limit() || newPosition < 0) {
	    throw new OVMError.IllegalArgument("newPosition " + newPosition 
					       + " not in [0 ," + limit() 
					       +"]" );
	}
	pos = newPosition;
	if (mark > pos) {
	    mark = INVALID_MARK;
	}
	return this;
    }


    /**
     * Reads a signed 8-bit value from this byte data stream at the current 
     * cursor position. If the byte read is <code>b</code>, where
     * 0&nbsp;&lt;=&nbsp;<code>b</code>&nbsp;&lt;=&nbsp;255, then the
     * result is: <ul><code> (byte)(b) </code></ul>
     * @return     the next byte of this input stream as a signed 8-bit
     *             <code>byte</code>.
     **/
    public byte get()  { 
	byte result = get(pos);
	pos ++;
	return result;
    }

    public abstract byte get(int i);

    /**
     * Reads an array of signed 8-bit values from this byte buffer starting
     * at its current position, given the index into the destination array
     * where copying should begin, and the number of bytes to read. The buffer's
     * current position is advanced by the number of bytes read.
     * @param array the array to which the values should be written
     * @param start the starting position within the target byte array
     *              to where the data is being copied
     * @param length the number of bytes to be read into the array
     * @return byte array of this input stream as signed 8-bit
     *         <code>bytes</code>.
     * @throws BufferUnderflowException if there is an attempt to read
     *                                  beyond the end of the buffer
     **/
    public ByteBuffer get(byte[] array, int start, int length) {
      	getNoAdvance( array, start, length);
	pos += length;
	return this;
    }
    
    protected abstract void getNoAdvance( byte[] array, int start, int length);

    /**
     * Reads an array of signed 8-bit values from this byte data stream
     * starting from the current position of this buffer and given an array to
     * which the data should be written. The amount of data which will be read
     * from the buffer corresponds to the size of the input array. If the array
     * is longer than the remaining space in the byte buffer, an exception will
     * be thrown. The buffer's current position is advanced by the number of
     * bytes read.
     * @param array the array to which the values should be written
     * @return byte array of this input stream as signed 8-bit
     *         <code>bytes</code>.
     * @throws BufferUnderflowException if there is an attempt to read
     *                                  beyond the end of the buffer
     **/
    public ByteBuffer get(byte[] array) {
	return get(array, 0, array.length);
    }

    /**
     * Writes a number of bytes from an array of signed 8-bit values to this 
     * byte buffer starting it its current position, from a given starting
     * position in the source array. The buffer's position is advanced by the
     * number of bytes written.
     * @param array the array from which the values should be read
     * @param start the starting position within the source array from
     *              which the data should be read
     * @param length the number of bytes to be written into the buffer
     * @return the newly modified version of this byte buffer
     * @throws BufferOverflowException if there is an attempt to write
     *                                 beyond the end of the buffer
     **/
    public ByteBuffer put(byte[] array, int start, int length) {
      	putNoAdvance( array, start, length);
	pos += length;
	return this;
    }
    
    protected abstract void putNoAdvance( byte[] array, int start, int length);

    /**
     * Given an array of bytes, writes this array of signed 8-bit values to 
     * this byte buffer starting at the current position of this buffer.
     * The amount of data which will be written to the buffer corresponds to 
     * the size of the input array. The buffer's position is advanced by the
     * number of bytes written. 
     * @param array the array from which the values should be read
     * @return the newly modified version of this byte buffer
     * @throws BufferOverflowException if there is an attempt to write
     *                                 beyond the end of the buffer
     **/
    public ByteBuffer put(byte[] array) {
	return put(array, 0, array.length);
    }


    public abstract ByteBuffer put(int index, byte b);

    /**
     * Writes a signed 8-bit value to this byte data stream at the
     * current cursor position.
     * @param b the byte to write
     * @return the newly modified buffer
     **/
    public ByteBuffer put(byte b) {
	put(pos, b);
	pos++;
	return this;
    }

    public ByteBuffer put(ByteBuffer src) {
	if (src.remaining() > remaining()) {
	    throw new BufferOverflowException("tried to transfer " 
					      + src.remaining()
					      + " but only " + remaining()
					      + " available.");
	}
	if ( src.hasArray() ) {
	    put( src.array(),
                 src.arrayOffset()+src.position(),
                 src.remaining() );
	    src.position( src.limit());
	    return this;
	}
	while (src.hasRemaining()) {
	    put(src.get());
	}
	return this;
    }


    /**
     * Reads a signed 16-bit number from this byte data stream at the current
     * cursor position. The method reads two bytes from the underlying input 
     * stream.
     * @return     the next two bytes of this input stream, interpreted as a
     *             signed 16-bit number.
     **/
    public short getShort() {
	short result = getShort(pos);
	pos += 2;
	return result;
    }

    public abstract short getShort(int index);

    /**
     * Writes a signed 16-bit number to this byte data stream at the current
     * cursor position. The method writes two bytes to the underlying input 
     * stream.
     * @param value the value to write
     * @return the newly modified byte buffer    
     **/
    public ByteBuffer putShort(short value) {
	putShort(pos, value);
	pos += 2;
	return this;
    }

    public abstract ByteBuffer putShort(int index, short value);


    /**
     * Return the character at index without advancing the cursor
     * @param index
     * @return the two bytes at index and index+1
     */
    public abstract char getChar(int index);


    /**
     * Reads an unsigned 16-bit number from this byte data stream.
     * @return     the next two bytes of this input stream as a char.
     **/
    public char getChar()  {
	char result = this.getChar(pos);
	pos += 2;
	return result;
    }

    public abstract ByteBuffer putChar(int index, char value);
	

    /**
     * Writes an unsigned 16-bit number to this byte data stream at the current
     * cursor position.
     * @param value the char to be written
     * @return the newly modified buffer
     **/    
    public ByteBuffer putChar(char value) {
	putChar(pos, value);
	pos += 2;
	return this;
    }



    /**
     * Reads a signed 32-bit integer from this byte data stream at the current
     * cursor position. This method reads four bytes from the underlying byte
     * stream.
     * @return     the next four bytes of this input stream, interpreted as an
     *             <code>int</code>.
     **/
    public int getInt()  {
        int result = this.getInt(pos);
	pos += 4;
	return result;
    }

    /**
     * Writes a signed 32-bit integer to this byte data stream at the
     * current cursor position. This method writes four bytes to the
     * underlying byte stream.
     * @param value the int to be written to the buffer
     * @return the newly modified buffer
     **/
    public ByteBuffer putInt(int value) {
	this.putInt(pos, value);
	pos += 4;
	return this;
    }

    public abstract int getInt(int index);


    public abstract ByteBuffer putInt(int index, int value);


    /**
     * Reads a signed 64-bit integer from this byte data stream. This 
     * method reads eight bytes from the underlying data stream.
     * @return     the next eight bytes of this input stream, interpreted as a
     *             <code>long</code>.
     **/
    public long getLong()  {
	long result = this.getLong(pos);
	pos += 8;
	return result;
    }

    /**
     * FIXME: comment is copy of JavaDoc from SUN!
     *
     * Creates a new byte buffer whose content is a shared subsequence
     * of this buffer's content.
     *
     * The content of the new buffer will start at this buffer's
     * current position. Changes to this buffer's content will be
     * visible in the new buffer, and vice versa; the two buffers'
     * position, limit, and mark values will be independent.
     *
     * The new buffer's position will be zero, its capacity and its
     * limit will be the number of bytes remaining in this buffer, and
     * its mark will be undefined. The new buffer will be direct if,
     * and only if, this buffer is direct, and it will be read-only
     * if, and only if, this buffer is read-only.
     *
     * @return The new byte buffer
     **/
    public abstract ByteBuffer slice();


    /**
     * Writes a signed 64-bit integer to this byte data stream at the
     * current cursor position. This method writes eight bytes to the
     * underlying byte stream.
     * @param value the long to be written to the buffer
     * @return the newly modified buffer
     **/
    public ByteBuffer putLong(long value)  {
	putLong(pos, value);
	pos += 8;
	return this;

    }

    /**
     * Writes a signed 64-bit integer to this byte data stream starting
     * at the given index. This method writes eight bytes to the underlying 
     * byte stream. Note that it's not atomic yet, but neither is very much
     * else in this implementation.
     * @param index the index at which to begin writing
     * @param value the value to write
     * @return the newly modified byte buffer
     **/
    public ByteBuffer putLong(int index, long value) {
	int high = (int)(value >>> 32);
	int low =  (int)(value & 0xffffffffL);
	if (isLittleEndian) {
	    putInt(index, low);
	    putInt(index + 4, high);
	} else {
	    putInt(index + 4, low);
	    putInt(index, high);
	}
	return this;
    }

    /**
     * Reads a signed 64-bit integer from this byte data stream. This 
     * method reads eight bytes from the underlying input stream.
     * @param      index   the position in the byte data stream
     * @return     the next eight bytes of this input stream, interpreted as a
     *             <code>long</code>.
     **/
    public long getLong(int index)  {
	long accum = 0L;
	if (isLittleEndian) {
	    for (int i = 0; i < 8; i++) {
		accum |= (long)(get(index) & 0xff) << (8*i);
		index ++;
	    }
	} else {
	    for (int i = 7; i >= 0; i--) {
		accum |= (long)(get(index) & 0xff) << (8*i);
		index ++;
	    }
	}
	return accum;
    }


    /**
     * Reads a <code>float</code> from this byte data stream. This 
     * method reads an <code>int</code> value as if by the 
     * <code>readInt</code> method and then converts that 
     * <code>int</code> to a <code>float</code> using the 
     * <code>intBitsToFloat</code> method in class <code>Float</code>. 
     * @return     the next four bytes of this input stream, interpreted as a
     *             <code>float</code>.
     **/
    public float getFloat()  {
        return Float.intBitsToFloat(getInt());
    }


    /**
     * Writes a <code>float</code> to this byte data stream at the
     * current cursor position.
     * @param val the float to be written to the buffer
     * @return the newly modified buffer
     **/
    public ByteBuffer putFloat(float val)  {
	return putInt(Float.floatToIntBits(val));
    }


    /**
     * Writes a <code>float</code> to this byte data stream starting
     * at the given index.
     * @param index the index at which to begin writing
     * @param val   the value to write
     * @return the newly modified byte buffer
     **/
    public ByteBuffer putFloat(int index, float val)  {
	return putInt(index, Float.floatToIntBits(val));
    }


    /**
     * Reads a <code>float</code> from this byte data stream. This 
     * method reads an <code>int</code> value as if by the 
     * <code>readInt</code> method and then converts that 
     * <code>int</code> to a <code>float</code> using the 
     * <code>intBitsToFloat</code> method in class <code>Float</code>. 
     * @param      i   the position in the byte data stream
     * @return     the next four bytes of this input stream, interpreted as a
     *             <code>float</code>.
     **/
    public float getFloat(int i)  {
        return Float.intBitsToFloat(getInt(i));
    }

    /**
     * Reads a <code>double</code> from this byte data stream. This 
     * method reads a <code>long</code> value as if by the 
     * <code>readLong</code> method and then converts that 
     * <code>long</code> to a <code>double</code> using the 
     * <code>longBitsToDouble</code> method in class <code>Double</code>.
     * @return     the next eight bytes of this input stream, interpreted as a
     *             <code>double</code>.
     **/
    public double getDouble()  {
	long val=getLong();
	if (doubleWordsSwapped) {
	    val=swapLongWords(val);
	}
	return Double.longBitsToDouble(val);
    }

    /*
     * Flips the two 32-bit words of a long value
     */
    private long swapLongWords(long value) {
	return (value << 32) | (value >>> 32);
    }

    /**
     * Writes a <code>double</code> to this byte data stream starting
     * at the given index.
     * @param index the index at which to begin writing
     * @param value the value to write
     * @return the newly modified byte buffer
     **/
    public ByteBuffer putDouble(int index, double value)  {
        long val=Double.doubleToLongBits(value);
	if (doubleWordsSwapped) {
	    val=swapLongWords(val);
	}
	return putLong(index, val);
    }

    /**
     * Writes a <code>double</code> to this byte data stream at the
     * current cursor position.
     * @param value the double to be written to the buffer
     * @return the newly modified buffer
     **/
    public ByteBuffer putDouble(double value) {
	putDouble(pos, value);
	pos += 8;
	return this;
    }
    /**
     * Reads a <code>double</code> from this byte data stream. This 
     * method reads a <code>long</code> value as if by the 
     * <code>readLong</code> method and then converts that 
     * <code>long</code> to a <code>double</code> using the 
     * <code>longBitsToDouble</code> method in class <code>Double</code>.
     * @param      i   the position in the byte data stream
     * @return     the next eight bytes of this input stream, interpreted as a
     *             <code>double</code>.
     **/
    public double getDouble(int i)  {
        long val=getLong(i);
	if (doubleWordsSwapped) {
	    val=swapLongWords(val);
	}
        return Double.longBitsToDouble(val);
    }
    
    /**
     * Exception to be thrown on attempts to read more from a buffer
     * than it actually contains.
     **/
    public static class BufferUnderflowException extends OVMError {
	/**
	 * Create a new buffer underflow exception to be thrown
	 * with the given message.
	 **/
	public BufferUnderflowException() {
	    this("No message");
	}

	/**
	 * Create a new buffer underflow exception to be thrown
	 * with the given message.
	 * @param message the message to be associated with the exception
	 **/
	public BufferUnderflowException(String message) {
	    super(message);
	}

	/**
	 * Create a new chained buffer underflow exception which
	 * was caused by the exception indicated.
	 * @param e the causing exception
	 **/
	public BufferUnderflowException(Exception e) {
	    super(e);
	}
    }

    /**
     * Exception to be thrown on attempts to write more to a buffer
     * than its capacity permits.
     **/
    public static class BufferOverflowException extends OVMError {
	/**
	 * Create a new buffer overflow exception to be thrown
	 * with the given message.
	 **/
	public BufferOverflowException() {
	    this("No message");
	}

	/**
	 * Create a new buffer overflow exception to be thrown
	 * with the given message.
	 * @param message the message to be associated with the exception
	 **/
	public BufferOverflowException(String message) {
	    super(message);
	}


	/**
	 * Create a new chained buffer overflow exception which
	 * was caused by the exception indicated.
	 * @param e the causing exception
	 **/
	public BufferOverflowException(Exception e) {
	    super(e);
	}
    }

    /**
     * Not used (only to make Jarvester compile).  Thrown in JDK for
     * read-only byte buffers (which we may want to add to OVM).
     **/
    public static class ReadOnlyBufferException extends OVMError {
	/**
	 * Create a new buffer overflow exception to be thrown
	 * with the given message.
	 **/
	public ReadOnlyBufferException() {
	    this("No message");
	}

	/**
	 * Create a new buffer overflow exception to be thrown
	 * with the given message.
	 * @param message the message to be associated with the exception
	 **/
	public ReadOnlyBufferException(String message) {
	    super(message);
	}


	/**
	 * Create a new chained buffer overflow exception which
	 * was caused by the exception indicated.
	 * @param e the causing exception
	 **/
	public ReadOnlyBufferException(Exception e) {
	    super(e);
	}
    }


    public abstract int findLastOffsetOf(char c, int start, int end);

    public abstract int findFirstOffsetOf(char c);

    
} // End of ByteBuffer
