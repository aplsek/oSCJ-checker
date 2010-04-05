/*
 * Copyright (C) 2008, The Open Group 
 * 
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * 
 * License: TBD will probably be Ecplipse
 * 
 */

/**
 * 
 */
package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

/**
 * A simple Terminal that puts out UTF8 version of String/StringBuilder,....
 * Does not allocate memory. The output device is implementation dependent and
 * writing to /dev/nul is a a valid implementation.
 * 
 * @author Martin Schoeberl
 * 
 */
// (annotations turned off to work with Java 1.4) @SCJAllowed
public class Terminal {

	private static Terminal single = new Terminal();

	private Terminal() {
	}

	/**
	 * Get the single output device.
	 * 
	 * @return something
	 */
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public static Terminal getTerminal() {
		return single;
	}

	/**
	 * Write the character sequence to the implementation dependent output
	 * device in UTF8.
	 * 
	 * @param s
	 * 
	 */
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public void write(CharSequence s) {
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (c < 128) {
				write((byte) (c & 0x7f));
			} else if (c < 0x800) {
				write((byte) (0xc0 | (c >>> 6)));
				write((byte) (0x80 | (c & 0x3f)));
			} else if (c < 0x1000) {
				write((byte) (0xe0 | (c >>> 12)));
				write((byte) (0x80 | ((c >>> 6) & 0x3f)));
				write((byte) (0x80 | (c & 0x3f)));
			} else {
				// TODO: we don't care on unicode that needs an escape itself
			}
		}
	}

	/**
	 * Same as write, but add a newline. CRLF does not hurt on a Unix terminal.
	 * 
	 * @param s
	 */
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public void writeln(CharSequence s) {
		write(s);
		writeln();
	}

	/**
	 * Just a CRLF output.
	 */
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public void writeln() {
		write("\r\n");
	}

	/**
	 * Does the actual work. Change for your implementation.
	 * 
	 * @param b
	 *            A UTF8 byte to be written.
	 */
	private void write(byte b) {
		System.out.write(b);
	}
}
