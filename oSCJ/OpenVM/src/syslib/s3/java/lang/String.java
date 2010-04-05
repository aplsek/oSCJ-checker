/**
 * @author Ben L. Titzer
 *
 * NOTE: This class is <b>NOT</b> meant to be an implementation of
 * java.lang.String rather, it represents the Strings that are used within
 * the OVM internals (e.g. error messages, selectors). One important
 * difference with java.lang.String is that we currently do not plan for
 * unicode support. As the core VM will <i>implement</i> unicode support -- our
 * strings are therefore meant to be plain ASCII strings. Nor do we expect
 * to be type compatible with java.lang.String, we will only include the
 * methods that are needed in the VM.<p>
 *                                     -- jv Feb 12 2002<p>
 *
 * This description is not quite accurate.  The core VM does <b>not</b>
 * implement unicode support.  It can translate between UTF8 and
 * UTF16, but knows nothing about other encodings.  Executive domain
 * strings are not plain ASCII, but UTF8.  charAt indexes the
 * underlying byte array, and may fail to return a complete character.
 * String operations do not check that newly created strings are valid
 * UTF8, but other parts of the system require this property.<p> 
 *					-- jb Mar  9 2004<p>
 **/
package java.lang;

//  import java.lang.Double;
//  import java.lang.Float;
//  import java.lang.Integer;
//  import java.lang.Long;

import ovm.util.StringConversion;

/**
 *
 * This class provides a basic implementation of java.lang.String for the
 * OVM runtime. Some of the implementation of these functions was derived
 * from the Classpath source.
 *
 * @author Ben L. Titzer
 * @author David Holmes (bug fixes and extensions :-) )
 **/
public final class String {

    /* empty array for empty strings */
    private static  byte[] empty_;


    /* internal data is represented with byte arrays */
    byte[] data;

    /* count caches the length of the array: easier for some purposes */
    int offset;
    int count;

    /* data, offset, and count are package-private for use in
     * StringBuffer
     */
    private int hashCode;
    
    public String() {
	data = empty_;
	hashCode = 0; // JLS dictates 0 for empty string.
    }

    public String(String s) {
        data = s.data;
	offset = s.offset;
	count = s.count;
    }

    public String(byte d[]) {
	count = d.length;
	data = arraydup(d);
    }

    public String(char d[]) {
	count = d.length;
	data = arraydup(d);
    }

    public String(char d[], int off, int cnt) {
	data = new byte[count = cnt];
	arraycopy(d, off, data, 0, cnt);
    }

    public String(byte d[], int off, int cnt) {
	data = new byte[count = cnt];
	arraycopy(d, off, data, 0, cnt);
    }

    public String(byte[] d, int off, int cnt, boolean dontCopy) {
	if (dontCopy) {
	    data = d;
	    this.offset = off;
	    this.count = cnt;
	} else {
	    data = new byte[count = cnt];
	    arraycopy(d, off, data, 0, cnt);
	}
    }
	
    /**
     * Private constructor that accomplishes appending.
     **/
    private String(String a, String b) {
	int cntr;
	count = a.count + b.count;
	data = new byte[count];
	arraycopy(a.data, 0, data, 0, a.count);
	arraycopy(b.data, 0, data, a.count, b.count);
    }

    public boolean equals(Object obj) {
	if ( obj == null ) return false;
	if ( !(obj instanceof String) ) return false;
	String str = (String)obj;
	if ( str.count != count ) return false;

	for ( int cntr = 0; cntr < count; cntr++ )
	    if ( data[offset+cntr] != str.data[str.offset+cntr] ) return false;
	return true;
    }

    public boolean equalsIgnoreCase(String str) {
	if ( str.count != count ) return false;

	for ( int cntr = 0; cntr < count; cntr++ )
	    if ( !equalsIgnoreCase(data[offset+cntr],
				   str.data[str.offset+cntr]) )
		return false;
	return true;
    }

    public String toUpperCase() {
	byte[] dat = new byte[count];
	for (int i=0;i<count;i++)
	    if ( (data[offset+i] >= 'a') &&
		 (data[offset+i] <= 'z') )
		dat[i] = (byte) (data[offset+i] - ('a' - 'A'));
	    else
		dat[i] = data[offset+i];
	return new String(dat);
    }

    private boolean equalsIgnoreCase(byte a, byte b) {
	return toLower(a) == toLower(b);
    }   


    private byte toLower(byte b) {
	if ( (b <= 'Z') && (b >= 'A') ) return (byte)((b - 'A') + 'a');
	return b;
    }

    /**
     * NOTE: This hashCode function must return comply EXACTLY to the hashCodes
     * returned by the host VM's java.lang.String class. Otherwise, finding
     * strings in hashtables that were created under the host VM would be
     * impossible at runtime using this implementation.
     **/
    public int hashCode() {
	if ( hashCode != 0 ) return hashCode;

	int h = 0;
	for (int cntr = 0; cntr < count; cntr++)
	    h = h * 31 + data[offset+cntr];
	
	hashCode = h;
	return h;
    }

    public int length() {
	return count;
    }

    public char charAt(int index) {
	return (char)data[offset+index];
    }

    public String toLowerCase() {
	byte[] array=new byte[length()];
	for (int i=0;i<length();++i) {
	    if (charAt(i)>='A' && charAt(i)<='Z') {
		array[i]=(byte)((int)charAt(i)-(int)'A'+(int)'a');
	    } else {
		array[i]=(byte)charAt(i);
	    }
	}
	return new String(array);
    }

    public int compareTo(String str) {
	int min = (count < str.count) ? count : str.count;

	for ( int cntr = 0; cntr < min; cntr++ ) {
	    int result = data[offset+cntr] - str.data[str.offset+cntr];
	    if ( result != 0 ) return result;
	}
	return count - str.count;
    }
    
    public boolean startsWith(String prefix) {
	if ( prefix.count == 0 ) return true;
	if ( count < prefix.count ) return false; // this string too short to match
	for ( int cntr = 0; cntr < prefix.count; cntr++ ) {
	    if ( data[offset+cntr] != prefix.data[prefix.offset+cntr] )
		return false;
	}
	return true;
    }

    public boolean endsWith(String suffix) {
	int i, cntr;
	if ( suffix.count == 0 ) return true;
	if ( count < suffix.count ) return false; // this string to short to match
	for ( cntr = count - suffix.count, i = 0; cntr < count; cntr++, i++ ) {
	    if ( data[offset+cntr] != suffix.data[suffix.offset+i] )
		return false;
	}
	return true;
    }

    public int indexOf(int ch) {
	return indexOf(ch, 0);
    }

    public int indexOf(int ch, int start) {
	byte b = (byte)ch;
	if ( start < 0 ) start = 0;
	for ( int cntr = start; cntr < count; cntr++ )
	    if ( data[offset+cntr] == b ) return cntr;
	return -1;
    }

    public int indexOf(String str) {
	return indexOf(str, 0);
    }

    public int indexOf(String str, int start) {
	if ( str.count == 0 ) return 0;
	for ( int cntr = start; cntr < (count - str.count)+1; cntr++ ) {
	    if ( data[offset+cntr] == str.data[str.offset] ) {
		int i;
		for ( i = 0; i < str.count; i++ ) {
		    if ( data[offset+cntr+i] != str.data[str.offset+i] ) break;
		}
		if ( i == str.count ) return cntr; // everything matched
	    }
	}
	return -1;
    }

    public int lastIndexOf(int ch) {
	return lastIndexOf(ch, count-1);
    }

    public int lastIndexOf(int ch, int from) {
	byte b = (byte)ch;
	if (from >= count)
	    from = count-1;
	for (int cntr = from; cntr >= 0; cntr--)
	    if (data[offset+cntr] == b)
		return cntr;
	return -1;
   }

    public int lastIndexOf(String str) {
	return lastIndexOf(str, count-1);
    }

    public int lastIndexOf(String str, int from) {
	int strl = str.count;
	int end = 1+from - strl;
	
	if ( end < 0 ) return -1;
	for ( int cntr = end; cntr >= 0; cntr-- ) {
	    int i;
	    for ( i = 0; i < strl; i++ ) {
		if ( data[offset+cntr+i] != str.data[str.offset+i] ) break;
	    }
	    if ( i == strl ) return cntr;
	}
	return -1;
    }

    public String[] split(String sep) {
	char c;
	if (sep.equals("\\."))
	    c = '.';
	else if (sep.length() > 1 || sep.charAt(0) == '.')
	    throw new IllegalArgumentException("split string too hard: "
					       + sep);
	else
	    c = sep.charAt(0);

	int count = 1;
	for (int offset = 0; offset != -1; offset = indexOf(c, offset))
	    count++;
	String[] ret = new String[count];
	for (int start = 0, end = indexOf(c, start), idx = 0;
	     start != -1;
	     start = end + 1, end = indexOf(c, start), idx++) 
	    ret[idx] = end == -1 ? substring(start) : substring(start, end);
	return ret;
    }

    public String substring(int beginIndex) {
	return new String(data,
			  offset + beginIndex,
			  count-beginIndex,
			  true);
    }

    public String substring(int beginIndex, int endIndex) {
	return new String(data,
			  offset + beginIndex,
			  endIndex-beginIndex,
			  true);
    }

    public String concat(String str) {
	return new String(this, str);
    }

    public String replace(char oldch, char newch) {
	int index = 0;
	for (; index < count; index++)
	    if (data[offset+index] == oldch)
		break;
	if (index == count) return this;
	byte[] newStr = new byte[count];
	arraycopy(data, 0, newStr, 0, count);
	for (int i = index; i < count; i++)
	    if (data[offset+i] == oldch)
		newStr[i] = (byte)newch;
	return new String(newStr);
    }

    public String trim() {
        int trimFront, trimBack;
        for ( trimFront = 0; trimFront < count; trimFront++ ) { 
            if ( !isWhiteSpace(data[offset+trimFront]) ) break;
        }
        if ( trimFront == count ) // if all the characters were whitespace
           return new String("");
        for ( trimBack = count-1; trimBack > trimFront; trimBack-- ) {
            if ( !isWhiteSpace(data[offset+trimBack]) ) break;
        }
	int newsize = 1+trimBack-trimFront;
	if ( newsize == count ) return this;
        return new String(data, trimFront, newsize);
    }

    private static boolean isWhiteSpace(byte b) {
        if ( b <= ' ' ) return true;
        return false;
    }

    public static String valueOf(Object obj) {
	return (obj == null)? "null" : obj.toString();
    }

    public static String valueOf(boolean b) {
	return (b) ? "true" : "false";
    }


    public static String valueOf(char c) {
	return StringConversion.toString(c);
    }

    public static String valueOf(int v) {
	return StringConversion.toString(v);
    }


    public static String valueOf(long v) {
	return StringConversion.toString(v);
    }

    public static String valueOf(float v) {
	return StringConversion.toString(v);
    }

    public static String valueOf(double v) {
	return StringConversion.toString(v);
    }

    public String toString() {
        return this;
    }

    /**
     * (dangerous) package scoped function so that 'StringBuffer can 
     * access internal fields.
     **/
    byte [] getInternalBytes() {
	return data;
    }

    public byte[] getBytes() {
	return arraydup(data, offset, count);
    }

    /**
     * Copies characters from this string into the destination byte
     * array. Each byte receives the 8 low-order bits of the
     * corresponding character. The eight high-order bits of each character
     * are not copied and do not participate in the transfer in any way.
     * <p>
     * The first character to be copied is at index <code>srcBegin</code>;
     * the last character to be copied is at index <code>srcEnd-1</code>.
     * The total number of characters to be copied is
     * <code>srcEnd-srcBegin</code>. The characters, converted to bytes,
     * are copied into the subarray of <code>dst</code> starting at index
     * <code>dstBegin</code> and ending at index:
     * <p><blockquote><pre>
     *     dstbegin + (srcEnd-srcBegin) - 1
     * </pre></blockquote>
     *
     *
     * @param      srcBegin   index of the first character in the string
     *                        to copy.
     * @param      srcEnd     index after the last character in the string
     *                        to copy.
     * @param      dst        the destination array.
     * @param      dstBegin   the start offset in the destination array.
     * @exception IndexOutOfBoundsException if any of the following
     *            is true:
     *           <ul><li><code>srcBegin</code> is negative
     *           <li><code>srcBegin</code> is greater than <code>srcEnd</code>
     *           <li><code>srcEnd</code> is greater than the length of this
     *            String
     *           <li><code>dstBegin</code> is negative
     *           <li><code>dstBegin+(srcEnd-srcBegin)</code> is larger than
     *            <code>dst.length</code></ul>
     * @exception NullPointerException if <code>dst</code> is <code>null</code>
     */
    public void getBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if (srcEnd > count) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
        }
        int dest = dstBegin;
        int src = srcBegin + offset;
	srcEnd += offset;
        byte[] val = data;   /* avoid getfield opcode */

        while (src < srcEnd) {
            dst[dest++] = val[src++];
        }
    }

    
    /**
     * Replacements for System.arraycopy()
     **/
    private void arraycopy(byte src[], int srcpos, 
			   byte dest[], int destpos, int cnt) {
	for ( int cntr = 0; cntr < cnt; cntr++ )
	    dest[destpos+cntr] = src[srcpos+cntr];
    }

    private void arraycopy(char src[], int srcpos, 
			   byte dest[], int destpos, int cnt) {
	for ( int cntr = 0; cntr < cnt; cntr++ )
	    dest[destpos+cntr] = (byte)src[srcpos+cntr];
    }

    private byte[] arraydup(char src[]) {
	int size = src.length;
	byte nd[] = new byte[size];
	for ( int cntr = 0; cntr < size; cntr++ )
	    nd[cntr] = (byte)src[cntr];
	return nd;
    }

    private byte[] arraydup(byte src[]) {
	return arraydup(src, 0, src.length);
    }

    private byte[] arraydup(byte[] src, int offset, int size) {
	byte nd[] = new byte[size];
	for ( int cntr = 0; cntr < size; cntr++ )
	    nd[cntr] = src[offset+cntr];
	return nd;
    }
    
    private static final void boot_() {
	empty_ = new byte[0];
    }

    public char[] toCharArray() {
	int size = count;
	char[] ret = new char[size];
	for(int i = 0; i < size; i++)
	    ret[i] = (char)data[offset+i];
	return ret;
    }

    public String intern() {
	return (String) (Object) myDomain().internString(toOop());
    }
}







