package ovm.util;

import ovm.core.OVMBase;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;

/**
 * UnicodeBuffer abstracts different ways to represent unicode
 * character sequences.  The OVM represents unicode character
 * sequences in a number of ways:
 * <ul>
 * <li> ranges of utf8 bytes within the UTF8Store byte[]
 * <li> Strings of chars at image build time
 * <li> Strings of utf8 bytes at runtime
 * <li> user-domain Strings of chars at runtime.
 * </ul>
 * By wrapping each representation of a character sequnce in a
 * UnicodeBuffer, we can avoid unnecessary copies when working with
 * and translating between represenations.  We also ensure that
 * translation to and from UTF8 actually occurs, rather than
 * translation to and from the default character encoding.
 * 
 * @author <a href="mailto://baker29@cs.purdue.edu"> Jason Baker </a>
 */
public abstract class UnicodeBuffer {
    /** The starting position **/
    protected int start;
    /** The current position **/
    protected int pos;
    /** The end position. **/
    protected int end;
    
    /** Return the next UTF8-encoded byte in this character sequence. */
    public abstract int getByte();

    /**
     * If called on a character boundary, return the first byte of
     * the next character, without effecting the current buffer
     * position.
     **/
    public int peekByte() {
	int _start = getPosition();
	int ret = getByte();
	setPosition(_start);
	return ret;
    }

    /**
     * Return the next unicode character in this character sequence.
     * The return value is undefined if the last call to getByte()
     * returned a partial character.
     **/
    public abstract int getChar();
    /**
     * Return a descriptor for the current position in this sequence.
     * The return value is undefined if the last call to getByte()
     * returned a partial character.
     **/
    public final int getPosition()      { return pos; }
    /**
     * Return a descriptor for the position at the start of this
     * sequence.
     **/
    public final int getStartPosition() { return start; }
    /**
     * Return a descriptor for the position at the end of this sequnce.
     **/
    public final int getEndPosition()   { return end; }
    /**
     * Restore the positoin within this sequnce from a descriptor
     * obtained from getPosition.<p>
     *
     * (Leaving this non-final in case extra state is associated with a
     * position.)
     **/
    public void setPosition(int position) {
	assert(start <= position && position <= end);
	pos = position;
    }
    /** Reset the position of this sequence to the beginning. */
    public final void rewind() { setPosition(start); }
    /**
     * Return true if data remains in this sequence, or false
     * otherwise.
     **/
    public boolean hasMore() { return pos < end; }
    /**
     * Return the number of bytes in the UTF8 encoding of this
     * sequence.
     **/
    public abstract int byteCount();

    /**
     * Return the minimum number of bytes that may needed to represent
     * this character sequnce in UTF8.  This number is typically close to
     * the actual byte count.
     **/
    public int minByteCount() { return byteCount(); }
    /**
     * Return the maximum number of bytes that may be needed to
     * represent this character sequence in UTF8.  This number may be
     * up to three times larger than the actual byte count.
     **/
    public int maxByteCount() { return byteCount(); }
    
    /** Return the number of characters in this sequence. */
    public abstract int charCount();

    /**
     * Return the minimum number of chars needed to represent
     * this character sequence in UTF16.  This number may be up to
     * three times smaller than the actual char count.
     **/
    public int minCharCount() { return charCount(); }
    /**
     * Return the maximum number of chars needed to represent this
     * character sequnce in UTF16.  This number is typically close to
     * the actual char count.
     **/
    public int maxCharCount() { return charCount(); }
    
    /**
     * Return a UnicodeBuffer for all characters before the current
     * position (including the character returned by the last call(s)
     * to getByte() or getChar()).  The result is undefined if the last
     * call to getByte() returned a partial character.
     **/
    public final UnicodeBuffer sliceBefore() { return slice(start, pos); }
    /**
     * Return a UnicodeBuffer for all characters after the current
     * position.  The result is undefined if the last call to
     * getByte() returned a partial character.
     **/
    public final UnicodeBuffer slice() { return slice(pos, end); }

    /**
     * Return a UnicodeBuffer for all charachters from the current
     * position to end.
     **/
    public final UnicodeBuffer slice(int _end) { return slice(pos, _end); }

    /**
     * Return a UnicodeBuffer for all characters in the given range
     * of positions.  Each position must be valid for this buffer.
     * (That is, each must be between getStartPosition and
     * getEndPosition and must correspond to a character boundary.)
     **/
    public abstract UnicodeBuffer slice(int _start, int _end);

    /**
     * Search for the first occurence of the character c in this
     * buffer, and return the position for which it is the next
     * character returned.
     * @return a suitable argument to setPosition or -1
     **/
    public final int firstPositionOf(char c) {
	return firstPositionOf(c, start);
    }
    /**
     * Search the remainder of this buffer (after the current
     * positon), for tthe character c.
     **/
    public final int nextPositionOf(char c) {
	return firstPositionOf(c, pos);
    }
    /**
     * Search for the first occurence of the character c starting at
     * or following the postion pos in this buffer, and return the
     * position for which it is the next character returned.
     * @return a suitable argument to setPosition or -1
     **/
    public abstract int firstPositionOf(char c, int _pos);
    /**
     * Search for the last occurrence of the character c in this
     * buffer, and return the position for which it is the next
     * character teturned.
     * @return a suitable argument to setPosition or -1
     **/
    public final int lastPositionOf(char c) {
	return lastPositionOf(c, end);
    }
    /**
     * Search for the first occurence of the character c starting at
     * or before the postion pos in this buffer, and return the
     * position for which it is the next character returned.
     * @return a suitable argument to setPosition or -1
     **/
    public abstract int lastPositionOf(char c, int _pos);
    /**
     * Compare this UnicodeBuffer with another.  Return true if their
     * contents are the same.  A buffer's position is undefined after
     * it has been passed as an argument to another buffer's equals
     * method.
     **/
    public abstract boolean equals(Object o);

    private int hashCode = 0;
    /**
     * This is the standard hash function used by
     * ovm.core.repository.UTF8Store.  One can rehash the UTF8Store
     * without duplicating code by carefully subclassing UTF8Buffer.
     * <p>
     * The result of getPostion() is undefined after a call to this
     * method.
     **/
    public final int hashCode() {
	if (hashCode == 0) {
/*	    rewind();
	    int ret = 0;
	    while (hasMore())
		// We want negative byte values (presumably)
		ret = (ret << 5) - ret + (byte) getByte();
*/

          // note that there is a pollcheck in this loop
          // making it atomic would be probably faster, but for larger buffers
          // the pause times might be bad... should be rewriten using the 
          // max atomic config to work on larger chunks
          
            int ret = 0;
            for(int i=start; i<end; i++) {
              
              int currentPosition = getPosition();
              setPosition(i);
              int content = getByte();
              setPosition(currentPosition);
              
              ret = (ret << 5) - ret + (byte) content;
            }
            
	    hashCode= ret;
	}
	return hashCode;
    }


    public static abstract class UTF8Buffer extends UnicodeBuffer {
	protected abstract byte getAbsoluteByte(int offset);
	protected abstract void setAbsoluteByte(int offset, byte b);

	public int getByte() {
	    if (pos == end)
		return -1;
	    else
		return getAbsoluteByte(pos++) & 0xff;
	}

	public int peekByte() {
	    if (pos == end)
		return -1;
	    else
		return getAbsoluteByte(pos) & 0xff;
	}

	public int byteCount() { return end - start; }
	public int minCharCount() { return byteCount()/3; }
	public int maxCharCount() { return byteCount(); }

	public int getChar() {
	    int b = getByte();
	    if (b < 0x80) {
		return b;
	    } else if ((b & 0xf0) == 0xe0) {
		int b2 = getByte();
		int b3 = getByte();
		if ((b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80)
		    throw new OVMError("bad UTF8 input: "
				       + Integer.toHexString(b)
				       + " at " + pos);
		return (((b & 0x0f) << 12)
			+ ((b2 & 0x3f) << 6)
			+ (b3 & 0x3f));
	    } else if ((b & 0xe0) == 0xc0) {
		int b2 = getByte();
		if ((b2 & 0xc0) != 0x80)
		    throw new OVMError("bad UTF8 input: "
				       + Integer.toHexString(b)
				       + " at " + pos);
		return (((b & 0x1f) << 6)
			+ (b2 & 0x3f));
	    } else {
		throw new OVMError("bad UTF8 input: "+Integer.toHexString(b)
				   + " at " + pos);
	    }
	}

	public int charCount() {
	    rewind();
	    int nc;
	    for (nc = 0; hasMore(); nc++)
		getChar();
	    return nc;
	}

	protected void copy(UnicodeBuffer buf) {
	    buf.rewind();
	    for (int i = start; i < end; i++) {
		byte b = (byte) buf.getByte();
		setAbsoluteByte(i, b);
	    }
	}

	public int firstPositionOf(char c, int _start) {
	    int b1, b2, b3;
	    int _end;
	    if (c > 0 && c < 0x80) {
		b1 = c;
		b2 = -1;
		b3 = -1;
		_end = this.end;
	    } else if (c < 0x800) {
		b1 = (c >> 6) + 0xc0;
		b2 = (c & 0x3f) + 0x80;
		b3 = -1;
		_end = this.end - 1;
	    } else {
		b1 = (c >> 12) + 0xe0;
		b2 = ((c >> 6) & 0x3f) + 0x80;
		b3 = (c & 0x3f) + 0x80;
		_end = this.end - 2;
	    }
	    for (int p = _start; p < _end; p++)
		if (b1 == getAbsoluteByte(p)
		    && (b2 == -1 || b2 == getAbsoluteByte(p+1))
		    && (b3 == -1 || b3 == getAbsoluteByte(p+2)))
		    return p;
	    return -1;
	}

	public int lastPositionOf(char c, int _end) {
	    int b1, b2, b3;
	    if (c > 0 && c < 0x80) {
		b1 = c;
		b2 = -1;
		b3 = -1;
	    } else if (c < 0x800) {
		b1 = (c >> 6) + 0xc0;
		b2 = (c & 0x3f) + 0x80;
		b3 = -1;
		_end -= 1;
	    } else {
		b1 = (c >> 12) + 0xe0;
		b2 = ((c >> 6) & 0x3f) + 0x80;
		b3 = (c & 0x3f) + 0x80;
		_end -= 2;
	    }
	    for (int p = _end - 1; p --> start; )
		if (b1 == getAbsoluteByte(p)
		    && (b2 == -1 || b2 == getAbsoluteByte(p+1))
		    && (b3 == -1 || b3 == getAbsoluteByte(p+2)))
		    return p;
	    return -1;
	}

	public boolean equals(Object o) {
	    if (!(o instanceof UnicodeBuffer))
		return false;
	    
	    UnicodeBuffer b = (UnicodeBuffer) o;
	    if (b.minByteCount() > byteCount()
		|| b.maxByteCount() < byteCount())
		return false;

	    rewind();
	    b.rewind();
	    for (int i = start; i < end; i++)
		if (!b.hasMore() || getAbsoluteByte(i) != b.getByte())
		    return false;
	    return !b.hasMore();
	}
	
	protected UTF8Buffer(int start, int end) {
	    this.start = start;
        this.pos = start;
	    this.end = end;
	}
    }

    private static class ByteArrayBuffer extends UTF8Buffer {
	private byte[] arr;
	
	protected byte getAbsoluteByte(int absPos) { return arr[absPos]; }
	protected void setAbsoluteByte(int absPos, byte b) { arr[absPos] = b; }

	public UnicodeBuffer slice(int _start, int _end) {
	    return new ByteArrayBuffer(arr, _start, _end);
	}

	private ByteArrayBuffer(byte[] arr, int start, int end) {
	    super(start, end);
	    assert(start <= end);
	    this.arr = arr;
	}

	public byte[] toByteArray() {
	    if (start == 0 && end == arr.length)
		return arr;
	    else
		return super.toByteArray();
	}

	/**
	 * Initialize this buffer from the contents of b.  B's
	 * position is undefined after this constructor is called.
	 *
	 * @param b another UnicodeBuffer
	 **/
	private ByteArrayBuffer(UnicodeBuffer b) {
	    super(0, b.byteCount());
	    arr = new byte[end];
	    copy(b);
	}
    }

    /**
     * Return the byte[] representation of this UnicodeBuffer.  This
     * byte[] should not be mutated, since for a <code>byte[]<code> <i>b</i>
     * <code> UnicodeBuffer.make(b).toByteArray() == b</code>
     **/
    public byte[] toByteArray() {
	ByteArrayBuffer buf = new ByteArrayBuffer(this);
	return buf.arr;
    }

    public static abstract class UTF16Buffer extends UnicodeBuffer {
	protected abstract char getAbsoluteChar(int index);
	protected abstract void setAbsoluteChar(int index, char value);

	// Offset within the character starting at pos
	protected int curChar;
	protected int remaining;

	public final void setPosition(int pos) {
	    super.setPosition(pos);
	    remaining = 0;
	}

	public boolean hasMore() { return remaining > 0 || pos < end; }
	
	public int getChar() {
	    if (pos == end)
		return -1;
	    return getAbsoluteChar(pos++);
	}

	public int charCount() { return end - start; }

	public int minByteCount() { return charCount(); }
	public int maxByteCount() { return charCount() * 3; }

	public int getByte() {
	    switch (remaining) {
	    case 0:
		int c = getChar();
		if ((c > 0 && c < 0x80) || c == -1) {
		    return c;
		} else if (c < 0x800) {
		    remaining = 1;
		    curChar = c;
		    return (c >> 6) + 0xc0;
		} else {
		    remaining = 2;
		    curChar = c;
		    return (c >> 12) + 0xe0;
		}
	    case 1:
		remaining = 0;
		return (curChar & 0x3f) + 0x80;
	    case 2:
		remaining = 1;
		return ((curChar >> 6) & 0x3f) + 0x80;
	    default:
		throw new OVMError("impossible");
	    }
	}

	public int byteCount() {
	    rewind();
	    int nb;
	    for (nb = 0; hasMore(); nb++)
		getByte();
	    return nb;
	}

	public int firstPositionOf(char c, int _start) {
	    for (int p = _start; p < end; p++)
		if (getAbsoluteChar(p) == c)
		    return p;
	    return -1;
	}

	public int lastPositionOf(char c, int _end) {
	    for (int p = _end -1; p --> start; )
		if (getAbsoluteChar(p) == c)
		    return p;
	    return -1;
	}

	protected void copy(UnicodeBuffer b) {
	    b.rewind();
	    for (int i = start; i < end; i++) {
		char c = (char) b.getChar();
		setAbsoluteChar(i, c);
	    }
	}

	public boolean equals(Object o) {
	    if (!(o instanceof UnicodeBuffer))
		return false;
	    
	    UnicodeBuffer b = (UnicodeBuffer) o;
	    if (b.minCharCount() > charCount()
		|| b.maxCharCount() < charCount())
		return false;

	    rewind();
	    b.rewind();
	    for (int i = start; i < end; i++)
		if (!b.hasMore() || getAbsoluteChar(i) != b.getChar())
		    return false;
	    return !b.hasMore();
	}
	
	protected UTF16Buffer(int start, int end) {
	    this.start = start;
	    this.pos = start;
	    this.end = end;
	}
    }

    private static class CharArrayBuffer extends UTF16Buffer {
	char[] arr;

	protected char getAbsoluteChar(int offset) { return arr[offset]; }
	protected void setAbsoluteChar(int offset, char value) {
	    arr[offset] = value;
	}

	public UnicodeBuffer slice(int _start, int _end) {
	    return new CharArrayBuffer(arr, _start, _end);
	}

	public char[] toCharArray() {
	    if (start == 0 && end == arr.length)
		return arr;
	    else
		return super.toCharArray();
	}

	protected CharArrayBuffer(char[] arr, int start, int end) {
	    super(start, end);
	    this.arr = arr;
	}

	CharArrayBuffer(UnicodeBuffer b) {
	    super(0, b.charCount());
	    arr = new char[end];
	    copy(b);
	}
    }

    /**
     * Return the char[] representation of this UnicodeBuffer.  This
     * char[] should not be mutated, since for a <code>char[]<code> <i>b</i>
     * <code> UnicodeBuffer.make(c).toByteArray() == c</code>
     **/
    public char[] toCharArray() {
	CharArrayBuffer buf = new CharArrayBuffer(this);
	return buf.arr;
    }

    /**
     * Return the String representation of this UnicodeBuffer
     **/
    public String toString() {
	return factory().toString(this);
    }
    
    public static abstract class Factory {
	public UnicodeBuffer wrap(String str) {
	    return wrap(str, 0, str.length());
	}
	public abstract UnicodeBuffer wrap(String str, int off, int len);
	public abstract String toString(UnicodeBuffer b);

	public UnicodeBuffer wrap(char[] arr) {
	    return wrap(arr, 0, arr.length);
	}
	public UnicodeBuffer wrap(char[] arr, int off, int len) {
	    return new CharArrayBuffer(arr, off, off+len);
	}
	public UnicodeBuffer wrap(byte[] arr) {
	    return wrap(arr, 0, arr.length);
	}
	public UnicodeBuffer wrap(byte[] arr, int off, int len) {
	    return new ByteArrayBuffer(arr, off, off+len);
	}
	public UnicodeBuffer wrap(ByteBuffer b, int off, int len) {
	    return wrap(b.array(), b.arrayOffset() + off, len);
	}
	public UnicodeBuffer wrap(ByteBuffer b) {
	    return wrap(b, 0, b.limit());
	}
    }

    public static Factory factory() throws PragmaStitchSingleton {
	// Choose an ED-String-aware factory at run time, and a
	// java-String aware factory at image build time (and in
	// application code).
	return StandaloneUnicodeBufferFactory._;
    }

    // A random string from gnu.java.lang.CharData for testing.
//     static final String UPPER_EXPAND
// 	= "SS\u02bcNJ\u030c\u0399\u0308\u0301\u03a5\u0308"
// 	+ "\u0301\u0535\u0552H\u0331T\u0308W\u030aY\u030a"
// 	+ "A\u02be\u03a5\u0313\u03a5\u0313\u0300\u03a5\u0313\u0301\u03a5"
// 	+ "\u0313\u0342\u1f08\u0399\u1f09\u0399\u1f0a\u0399\u1f0b\u0399\u1f0c"
// 	+ "\u0399\u1f0d\u0399\u1f0e\u0399\u1f0f\u0399\u1f08\u0399\u1f09\u0399"
// 	+ "\u1f0a\u0399\u1f0b\u0399\u1f0c\u0399\u1f0d\u0399\u1f0e\u0399\u1f0f"
// 	+ "\u0399\u1f28\u0399\u1f29\u0399\u1f2a\u0399\u1f2b\u0399\u1f2c\u0399"
// 	+ "\u1f2d\u0399\u1f2e\u0399\u1f2f\u0399\u1f28\u0399\u1f29\u0399\u1f2a"
// 	+ "\u0399\u1f2b\u0399\u1f2c\u0399\u1f2d\u0399\u1f2e\u0399\u1f2f\u0399"
// 	+ "\u1f68\u0399\u1f69\u0399\u1f6a\u0399\u1f6b\u0399\u1f6c\u0399\u1f6d"
// 	+ "\u0399\u1f6e\u0399\u1f6f\u0399\u1f68\u0399\u1f69\u0399\u1f6a\u0399"
// 	+ "\u1f6b\u0399\u1f6c\u0399\u1f6d\u0399\u1f6e\u0399\u1f6f\u0399\u1fba"
// 	+ "\u0399\u0391\u0399\u0386\u0399\u0391\u0342\u0391\u0342\u0399\u0391"
// 	+ "\u0399\u1fca\u0399\u0397\u0399\u0389\u0399\u0397\u0342\u0397\u0342"
// 	+ "\u0399\u0397\u0399\u0399\u0308\u0300\u0399\u0308\u0301\u0399\u0342"
// 	+ "\u0399\u0308\u0342\u03a5\u0308\u0300\u03a5\u0308\u0301\u03a1\u0313"
// 	+ "\u03a5\u0342\u03a5\u0308\u0342\u1ffa\u0399\u03a9\u0399\u038f\u0399"
// 	+ "\u03a9\u0342\u03a9\u0342\u0399\u03a9\u0399FFFI"
// 	+ "FLFFIFFLSTS"
// 	+ "T\u0544\u0546\u0544\u0535\u0544\u053b\u054e\u0546\u0544\u053d";

//     static void sizes(String name, UnicodeBuffer b) {
// 	System.out.println(name + " has " + b.charCount() +
// 			   " chars/" + b.byteCount() + " bytes");
//     }
    
//     public static void main(String[] args) {
// 	UnicodeBuffer sb = factory().wrap(UPPER_EXPAND);
// 	sizes("string buffer", sb);
// 	byte[] b = sb.toByteArray();
// 	System.out.println("got " + b.length + " bytes");
// 	UnicodeBuffer bb = factory().wrap(b);
// 	sizes("byte buffer", bb);
// 	String copy = bb.toString();
// 	System.out.println("got " + copy.length() + " chars");
// 	for (int i = 0; i < copy.length(); i++) 
// 	    if (copy.charAt(i) != UPPER_EXPAND.charAt(i))
// 		System.out.println("at char " + i + ": found " +
// 				   Integer.toHexString(copy.charAt(i)) +
// 				   " but expected " +
// 				   Integer.toHexString(UPPER_EXPAND.charAt(i)));
// 	if (!copy.equals(UPPER_EXPAND)) {
// 	    System.out.println("bad conversion");
// 	    System.exit(-1);
// 	}
//     }
}
