package ovm.util;

import ovm.util.UnicodeBuffer.Factory;
import ovm.util.UnicodeBuffer.UTF16Buffer;

public class StandaloneUnicodeBufferFactory extends Factory {
    static Factory _ = new StandaloneUnicodeBufferFactory();

    /**
     * FIXME: This should really be a CharSequnceBuffer but we don't
     * define CharSequence in syslib/s3.
     **/
    private static class StringBuffer extends UTF16Buffer {
	String str;
	
	protected char getAbsoluteChar(int offset) {
	    return str.charAt(offset);
	}
	protected void setAbsoluteChar(int offset, char value) {
	    throw new OVMError("read only UnicodeBuffer");
	}

	public UnicodeBuffer slice(int _start, int _end) {
	    return new StringBuffer(str, _start, _end);
	}

	public String toString() {
	    if (start == 0 && end == str.length())
		return str;
	    else
		return str.substring(start, end - start);
	}
	
	protected StringBuffer(String str, int start, int end) {
	    super(start, end);
	    this.str = str;
	}
    }

    public UnicodeBuffer wrap(String str, int off, int len) {
	return new StringBuffer(str, off, len);
    }

    public String toString(UnicodeBuffer b)  {
	return new String(b.toCharArray());
    }
}
