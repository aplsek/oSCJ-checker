/**
 * @author Ben L. Titzer
 *
 * This class represents a string that is mutable. This is NOT
 * meant to be an implementation of java.lang.StringBuffer, but is
 * meant as a representation of java.lang.StringBuffer used within
 * the VM internally.
 *
 **/
package java.lang;
import ovm.core.services.io.BasicIO;


/**
 * This class is _not_ meant to be a replacement for java.lang.StringBuffer.
 * It is meant to be a basic implementation for the internal operation of
 * the OVM virtual machine.
 *
 * @author Ben L. Titzer
 **/
public final class StringBuffer {

    private static final int DEFAULT_CAPACITY = 16;

    private byte[] data;
    private int count;
    private int capacity;

    public StringBuffer() {
	data = new byte[capacity = DEFAULT_CAPACITY];
    }

    public StringBuffer(int cap) {
	data = new byte[capacity = cap];
    }

    public StringBuffer(String s) {
	data = s.getBytes();
	count = capacity = data.length;
    }

    public StringBuffer append(boolean bool) {
	if ( bool ) return append("true");
	return append("false");
    }

    public StringBuffer append(char ch) {
	ensureCapacity(count+1);
	data[count++] = (byte)ch;
	return this;
    }

    public StringBuffer append(int i) {
	return append(String.valueOf(i));
    }

    public StringBuffer append(long l) {
	return append(String.valueOf(l));
    }

    public StringBuffer append(float f) {
	return append(String.valueOf(f));
    }

    public StringBuffer append(double d) {
	return append(String.valueOf(d));
    }

    public StringBuffer append(Object o) {
	return append(String.valueOf(o));
    }

    public StringBuffer append(String str) {
	if (str == null) {
	    return append((Object) null);      
	} else {
	    return append(str.data, str.offset, str.count);
	}
    }

    /**
     * Internal method to append an array of bytes to the end of this StringBuffer
     **/
    private StringBuffer append(byte[] array) {
	append(array, 0, array.length);
	return this;
    }

    private StringBuffer append(byte[] array, int offset, int cnt) {
	ensureCapacity(count+cnt);
	System.arraycopy(array, offset, data, count, cnt);
	count += cnt;
	return this;
    }

    public StringBuffer append(char [] array) {
	append(array, 0, array.length);
	return this;
    }

    public StringBuffer append(char [] array, int offset, int cnt) {
	ensureCapacity(count+cnt);
	arraycopy(array, offset, data, count, cnt);
	count += cnt;
	return this;
    }

    public int capacity() {
	return capacity;
    }

    public char charAt(int index) {
	return (char)data[index];
    }

    public StringBuffer delete(int start, int end) {
	System.arraycopy(data, end, data, start, count - end);
	count -= (end - start);
	return this;
    }

    public StringBuffer deleteCharAt(int index) {
	System.arraycopy(data, index+1, data, index, count-index);
	count--;
	return this;
    }

    public void ensureCapacity(int min) {
	if ( capacity < min ) {
	    byte nd[] = new byte[min];
	    System.arraycopy(data, 0, nd, 0, count);
	    data = nd;
	}
    }
    public void getChars(int srcOffset, int srcEnd, char [] dst, int dstOffset) {
	int cnt = srcEnd - srcOffset;
	for ( int cntr = 0; cntr < cnt; cntr++ ) 
	    dst[dstOffset+cntr] = (char)data[srcOffset+cntr];
    }

    public StringBuffer insert(boolean bool, int offset) {
	if ( bool ) return insert(offset, "true");
	return insert(offset, "false");
    }

    public StringBuffer insert(int offset, char ch) {
	ensureCapacity(count+1);
	System.arraycopy(data, offset, data, offset+1, count - offset);
	data[offset] = (byte)ch;
	count++;
	return this;
    }

    public StringBuffer insert(int offset, int i) {
	return insert(offset, String.valueOf(i));
    }

    public StringBuffer insert(int offset, long l) {
	return insert(offset, String.valueOf(l));
    }

    public StringBuffer insert(int offset, float f) {
	return insert(offset, String.valueOf(f));
    }

    public StringBuffer insert(int offset, double d) {
	return insert(offset, String.valueOf(d));
    }

    public StringBuffer insert(int offset, Object o) {
	return insert(offset, String.valueOf(o));
    }

    public StringBuffer insert(int offset, String str)  {
	return insert(offset, str.getBytes());
    }

    private StringBuffer insert(int offset, byte [] array) {
	ensureCapacity(count+array.length);
	// open up space in the middle to insert array
	System.arraycopy(data, offset, data, offset+array.length, count-offset);
	System.arraycopy(array, 0, data, offset, array.length);
	count += array.length;
	return this;
    }

    public int length() {
	return count;
    }

//     public StringBuffer replace(int start, int end, String str) {
// 	// make sure we have enough space for new string
// 	ensureCapacity(count-(end-start)+str.length());
// 	/* TODO: implement replace() */
// 	return this;
//     }

    public StringBuffer reverse() {
        for (int i = count >> 1, j = count - i; --i >= 0; ++j) {
            byte b = data[i];
            data[i] = data[j];
            data[j] = b;
        }
	return this;
    }

    public void setCharAt(int index, char ch) {
	data[index] = (byte)ch;
    }

    public void setLength(int newLength) {
	if ( newLength < count ) { count = newLength; return; }
	ensureCapacity(newLength);
	// zero rest of characters
	for ( int cntr = count; cntr < newLength; cntr++ )
	    data[cntr] = 0;
	count = newLength;
    }
    
    public String substring(int begIndex) {
	return new String(data, begIndex, count - begIndex);
    }

    public String substring(int begIndex, int endIndex) {
	return new String(data, begIndex, endIndex);
    }

    public String toString() {
	return new String(data, 0, count);
    }

    // array copies between char[] and byte[]
    private void arraycopy(char src[], int srcpos, 
			   byte dest[], int destpos, int cnt) {
	for ( int cntr = 0; cntr < cnt; cntr++ ) {
            if (destpos+cntr >= dest.length ||
                srcpos+cntr >= src.length) {
                }
	    dest[destpos+cntr] = (byte)src[srcpos+cntr];
        }
    }


}
