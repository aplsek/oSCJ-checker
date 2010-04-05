package edu.purdue.scj;

import java.io.IOException;
import java.io.InputStream;

public class PropFileReader {

	private static InputStream _in;

	static void setInputStream(InputStream in) {
		_in = in;
	}

	public static CharSequence readAll() {
		if (_in == null)
			return null;
		
		byte[] bytebuf = null;
		char[] charbuf = null;
		try {
			bytebuf = new byte[_in.available()];
			charbuf = new char[_in.available()];
			_in.read(bytebuf);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < bytebuf.length; i++) 
			charbuf[i] = (char) bytebuf[i];
		
		return new StringBuffer().append(charbuf);
	}
}
