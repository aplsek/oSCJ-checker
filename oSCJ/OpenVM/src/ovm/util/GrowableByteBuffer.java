package ovm.util;

class GrowableByteBuffer
    extends FixedByteBuffer
{

    private final int pageSize;

    private void growIfNeeded(int size, int currPos) {
        int newPos = size + currPos;
        if (newPos > data.length) {
            int newSize = ((newPos / pageSize) + 1) * pageSize*2;
            byte[] newData = new byte[newSize];
            System.arraycopy(data, 0, newData, 0, data.length);
            this.data = newData;
        }
    }

    public GrowableByteBuffer(int pageSize) {
        super(new byte[pageSize]);
        this.pageSize = pageSize;
        limit(Integer.MAX_VALUE);
    }

    public GrowableByteBuffer() {
        this(4096);
    }
    
    /**
     * The content of the new buffer will be that of this buffer. Changes to
     * this buffer's content will be visible in the new buffer, and vice versa;
     * the two buffers' position, limit, and mark values will be independent.
     *<p>
     * Changes made in one buffer will be visible in the other. Because this
     * is a growable byte buffer, "changes" include resizing. So reallocation
     * of the array will not break sharing, to duplicate a growable byte
     * buffer is not simply to clone it, but to create a proxy that refers to
     * it. Note that the proxy is not itself an instance of GrowableByteBuffer.
     * These internal implementation classes of ByteBuffer should not be
     * mentioned explicitly or used in instanceof tests!  The result is
     * certainly a ByteBuffer and you know it is growable because its capacity
     * is Integer.MAX_VALUE.  Nothing more ought to matter.
     *<p>
     * This solution has the property that the cost of indirection is not paid
     * for operations on the original GrowableByteBuffer, or indeed at all for
     * GrowableByteBuffers that are never duplicated.
     * @return a "duplicate" of this ByteBuffer
     **/
    public ByteBuffer duplicate() {
      	return new Proxy( this, mark);
    }
    
    public int capacity() {
        return Integer.MAX_VALUE;
    }

    protected void getNoAdvance(byte[] array, int start, int length) {
        growIfNeeded(length, position());
        super.getNoAdvance(array, start, length);
    }

    protected void putNoAdvance(byte[] array, int start, int length) {
        growIfNeeded(length, position());
        super.putNoAdvance(array, start, length);
    }

    public ByteBuffer put(int index, byte b) {
        growIfNeeded(1, index);
        return super.put(index, b);
    }

    public short getShort(int index) {
        growIfNeeded(2, index);
        return super.getShort(index);
    }

    public ByteBuffer putShort(int index, short value) {
        growIfNeeded(2, index);
        return super.putShort(index, value);
    }
    public ByteBuffer putChar(int index, char value) {
        growIfNeeded(2, index);
        return super.putChar(index, value);
    }

    public char getChar(int index)  {
        growIfNeeded(2, index);
        return super.getChar(index);
    }

    public int getInt(int index)  {
        growIfNeeded(4, index);
        return super.getInt(index);
    }

    public ByteBuffer putInt(int index, int value)  {
        growIfNeeded(4, index);
        return super.putInt(index, value);
    }

    public ByteBuffer slice() {
	throw new UnsupportedOperationException();
    }


    /**
     * Duplicating a GrowableByteBuffer gets you one of these, encapsulating
     * a reference to the original. And yes, this means the original is held
     * live as long as any proxy is, but this wastes a few words
     * maximum--the size of the original GrowableByteBuffer object only, not
     * the much larger backing array, which would be live in any case.
     **/
    static class Proxy extends ByteBuffer implements Cloneable {
      	/**
	 * Duplicating a Proxy is simple: just clone it.
	 **/
	public ByteBuffer duplicate() {
      	    try {
		return (ByteBuffer)clone();
	    }
	    catch ( CloneNotSupportedException cnse ) {
		throw failure( cnse);
	    }
	}
	
      	private int limit_;
	private GrowableByteBuffer orig_;
	
	/* private */ Proxy( GrowableByteBuffer orig, int mark) {
	    this.orig_ = orig;
	    limit( orig.limit());
	    position( orig.position());
	    this.mark = mark;
	}
	
	public int limit() { return limit_; }

	public Buffer limit( int newLimit) {
	    limit_ = newLimit;
	    return this;
	}
	
        public int capacity() {
            return Integer.MAX_VALUE;
        }

	public boolean hasArray() { return orig_.hasArray(); }
	public  byte[] array() { return orig_.array(); }
	public   int   arrayOffset() { return orig_.arrayOffset(); }

	public byte get( int i) {
	    return orig_.get( i);
	}
	
	protected void getNoAdvance( byte[] array, int start, int length) {
	    orig_.getNoAdvance( array, start, length);
	}
	
	protected void putNoAdvance( byte[] array, int start, int length) {
	    orig_.putNoAdvance( array, start, length);
	}
	
	public ByteBuffer put( int index, byte b) {
	    orig_.put( index, b);
	    return this;
	}
	
	public short getShort( int i) { return orig_.getShort( i); }
	
	public ByteBuffer putShort( int i, short v) {
	    orig_.putShort( i, v);
	    return this;
	}
	
	public char getChar( int i) { return orig_.getChar( i); }
	
	public ByteBuffer putChar( int i, char v) {
	    orig_.putChar( i, v);
	    return this;
	}
	
	public int getInt( int i) { return orig_.getInt( i); }
	
	public ByteBuffer putInt( int i, int v) {
	    orig_.putInt( i, v);
	    return this;
	}
	
	public int findFirstOffsetOf( char c) {
	    return orig_.findFirstOffsetOf( c);
	}
	
	public int findLastOffsetOf( char c, int start, int end) {
	    return orig_.findLastOffsetOf( c, start, end);
	}
	
	public Buffer flip() { return limit( position()).rewind(); }
	
	public ByteBuffer slice() {
	    throw new UnsupportedOperationException();
	}
	
    }
} // End of GrowableByteBuffer
