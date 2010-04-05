
package ovm.core.services.io;


import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;
import ovm.core.services.memory.MemoryManager;
import ovm.util.ByteBuffer;
import ovm.util.OVMException;
import ovm.util.StringConversion;

import ovm.core.services.memory.MemoryPolicy;
import ovm.core.domain.DomainDirectory;
/**
 * Defines the basic I/O facilities of the OVM. A simple printing API is
 * available, similar to Java's PrintWriter, with access to the standard
 * streams in a similar way to System.out etc.
 * <p>Output methods use the conversion routines of
 * {@link StringConversion} when printing primitive types.
 * <h3>Notes</h3>
 * <p>This class is intended to provide low-level I/O capabilities that can
 * be used from anywhere within the OVM - typically as part of logging or
 * debugging. To do this we have to make sure that we don't do things that
 * might fail when used in certain parts of the OVM. So, there is no
 * synchronization for example (and the kernel StringBuffer doesn't use it
 * either). We also avoid the implicit allocation involved in getting bytes
 * from strings by passing the strings straight to the native functions.
 * <p>There should be nothing in this class (other than not being initialized)
 * that should force you to take recourse to the Native API directly.
 *
 * @author David Holmes
 *
 */
public class BasicIO extends ovm.core.OVMBase {

    // these should be final but we have to change their value at runtime.

    // This is build time only!  We are doing this twice to get different
    // values at build time and at runtime... fun eh?
    public static PrintWriter out = new PrintWriter(Native.getStdOut());
    public static PrintWriter err = new PrintWriter(Native.getStdErr());


    /**
     * Initialize the PrintWriters used.
     * <p>This is a critical function that must occur as soon in the
     * initialization process as possible. We need allocation so the
     * memory manager must be initialized first. Beyond that all we do
     * and all we are allowed to do is invoke the native methods to do
     * the initialization. If you want to debug this use
     * <tt>Native.print_string</tt>.
     */
    public static void init() {
	Object current = MemoryPolicy.the().
            enterMetaDataArea(DomainDirectory.getExecutiveDomain()); 
	try {
            out = new PrintWriter(Native.getStdOut());
            err = new PrintWriter(Native.getStdErr());
	}
	finally {
	    MemoryPolicy.the().leave(current);
	}
    }

    /**
     * Return the last error message, as defined by the <tt>errno</tt>
     * value, set by the last system call.
     */
    public static String getLastErrorMessage() {
//        byte[] msg = new byte[100];
        byte[] msg = MemoryManager.the().allocateContinuousByteArray(100);
        int rlen = Native.get_error_string(msg, msg.length);
        return new String(msg, 0, rlen);
    }

    /**
     * Return the string message corresponding to the given
     * <tt>errno</tt> value.
     * @param errno
     */
    public static String getErrorMessageFor(int errno) {
//        byte[] msg = new byte[100];
        byte[] msg = MemoryManager.the().allocateContinuousByteArray(100);
        int rlen = Native.get_specific_error_string(errno, msg, msg.length);
        return new String(msg, 0, rlen);
    }

    /**
     * Read the contents of the file specified by <tt>pathStr</tt> and
     * return them in a byte[].
     * @param pathStr the path to the file to read
     * @return the contents of the file
     * @throws OVMException.IO if the file cannot be read
     */
    public static byte[] contentsAsByteArray(String pathStr)
        throws OVMException.IO {
        byte[] path = Native.Utils.string2c_string(pathStr);
        int fd = Native.open(path, NativeConstants.O_RDONLY, 0);
        if (fd < 0) {
            Native.print_string("Throwing exception in contentsAsByteArray, pathStr is "+pathStr+"\n");
            throw new OVMException.IO(getLastErrorMessage()  +
                                      ": " + pathStr);
        }
        int size = (int) Native.file_size(fd);
        if (size < 0) {
            throw new OVMException.IO(getLastErrorMessage() + ": invalid size " +  size);
        }

        byte[] data = null;
        
        if (MemoryManager.the().usesArraylets()) {
          data = MemoryManager.the().allocateContinuousByteArray(size);
        } else {
          data = new byte[size];
        }

        int nbytes = Native.read(fd, data, size);
        Native.close(fd);
        if (nbytes != size) {
            throw new OVMException.IO(getLastErrorMessage()
                                      + ": read only " + nbytes + " of " +  size);
        }
        return data;
    }

    /**
     * Read the contents of the file specified by <tt>pathStr</tt> and
     * return them in ByteBuffer.
     * @param pathStr the path to the file to read
     * @return the contents of the file
     * @throws OVMException.IO if the file cannot be read
     */
    public static ByteBuffer contents(String pathStr)
        throws OVMException.IO {
        return ByteBuffer.wrap(contentsAsByteArray(pathStr));
    }


    /*
     * A simple printing class based on the java.io.PrintWriter API.
     * <p>Note this this class <b>does not use synchronization</b>. Without
     * protection at a higher level, calls from multiple threads will be
     * arbitrarily interleaved - including between the printing of the main
     * characters and the newline.
     *
     */
    public static final class PrintWriter
        extends ovm.core.OVMBase
    {

        private final Native.FilePtr fp;

        /** 
         * Needs a FILE pointer (and not a file descriptor!)
         * @param fp
         */
        public PrintWriter(int fp) {
            this.fp = new Native.FilePtr(fp);
        }

        /**
         * Write a single character.
         * @param c int specifying a character to be written.
         * @return the character written or NativeConstants.EOF on error
         */
        public int write(int c) {
            return Native.print_char_on(fp, c);
        }

        /**
         * Write a portion of an array of bytes.
         * @param buf Array of bytes to write from
         * @param off Offset from which to start writing bytes
         * @param len Number of bytes to write
         * @return 0 on success and NativeConstants.EOF if an error occurs
         * @throws IndexOutOfBoundException if the offset and length takes us
         * outside the array
         */
        public int write(byte[] buf, int off, int len) {
            for (int i = off; i < off+len; i++) {
                if( write(buf[i]) == NativeConstants.EOF) {
                    return NativeConstants.EOF;
                }
            }
            return 0;
        }

        /**
         * Write an array of bytes.
         * @param buf Array of bytes to be written
         * @return 0 on success and NativeConstants.EOF if an error occurs
         */
        public int write(byte[] buf) {
            return write(buf, 0, buf.length);
        }

        /**
         * Write a portion of a string.
         * @param s A String
         * @param off Offset from which to start writing characters
         * @param len Number of characters to write
         */
        public int write(String s, int off, int len) {
            if (off < 0 || len < 0 || (off+len > s.length()) ) {
                throw new IllegalArgumentException("off/len out of range");
            }
            return Native.print_substring_on(fp, s, off, len);
        }

        /**
         * Write a string.
         * @param s String to be written
         */
        public int write(String s) {
            return Native.print_string_on(fp, s);
        }


        // print functions that don't terminate a line

        /**
         * Print a boolean value.
         *
         * @param b The <code>boolean</code> to be printed
         */
        public void print(boolean b) {
            write(b ? "true" : "false");
        }

        /**
         * Print a character.
         * @param c The <code>char</code> to be printed
         */
        public void print(char c) {
            write(c);
        }

        /**
         * Print an integer.
         * @param i The <code>int</code> to be printed
         */
        public void print(int i) {
            write(StringConversion.toString(i));
        }

        /**
         * Print a long integer.
         *
         * @param l The <code>long</code> to be printed
         */
        public void print(long l) {
            write(StringConversion.toString(l));
        }

        /**
         * Print a floating-point number.
         * @param f The <code>float</code> to be printed
         */
        public void print(float f) {
            write(StringConversion.toString(f));
        }

        /**
         * Print a double-precision floating-point number.
         *
         * @param d The <code>double</code> to be printed
         */
        public void print(double d) {
            write(StringConversion.toString(d));
        }

        /**
         * Print an array of bytes.
         * @param s The array of bytes to be printed
         */
        public void print(byte[] s) {
            write(s);
        }

        /**
         * Print a string. If the argument is <code>null</code> then the string
         * <code>"null"</code> is printed.
         *
         * @param s The <code>String</code> to be printed
         */
        public void print(String s) {
            if (s == null) {
                s = "null";
            }
            write(s);
        }

        /**
         * Print an object.
         * @param obj The <code>Object</code> to be printed
         */
        public void print(Object obj) {
            if (obj == null) {
                obj = "null";
            }
            write(obj.toString());
        }

        /* Methods that do terminate lines */

        /**
         * Terminate the current line by writing the line separator string.
         */
        public void println() {
            write('\n');
            Native.fflush(fp);
        }

        /**
         * Print a boolean value and then terminate the line.  This method behaves
         * as though it invokes <code>{@link #print(boolean)}</code> and then
         * <code>{@link #println()}</code>.
         *
         * @param x the <code>boolean</code> value to be printed
         */
        public void println(boolean x) {
            print(x);
            println();
        }

        /**
         * Print a character and then terminate the line.  This method behaves as
         * though it invokes <code>{@link #print(char)}</code> and then <code>{@link
         * #println()}</code>.
         *
         * @param x the <code>char</code> value to be printed
         */
        public void println(char x) {
                print(x);
                println();
        }

        /**
         * Print an integer and then terminate the line.  This method behaves as
         * though it invokes <code>{@link #print(int)}</code> and then <code>{@link
         * #println()}</code>.
         *
         * @param x the <code>int</code> value to be printed
         */
        public void println(int x) {
            print(x);
            println();
        }

        /**
         * Print a long integer and then terminate the line.  This method behaves
         * as though it invokes <code>{@link #print(long)}</code> and then
         * <code>{@link #println()}</code>.
         *
         * @param x the <code>long</code> value to be printed
         */
        public void println(long x) {
            print(x);
            println();
        }

        /**
         * Print a floating-point number and then terminate the line.  This method
         * behaves as though it invokes <code>{@link #print(float)}</code> and then
         * <code>{@link #println()}</code>.
         *
         * @param x the <code>float</code> value to be printed
         */
        public void println(float x) {
            print(x);
            println();
        }

        /**
         * Print a double-precision floating-point number and then terminate the
         * line.  This method behaves as though it invokes <code>{@link
         * #print(double)}</code> and then <code>{@link #println()}</code>.
         *
         * @param x the <code>double</code> value to be printed
         */
        public void println(double x) {
            print(x);
            println();
        }

        /**
         * Print an array of bytes and then terminate the line.  This method
         * behaves as though it invokes <code>{@link #print(byte[])}</code> and then
         * <code>{@link #println()}</code>.
         *
         * @param x the array of <code>byte</code> values to be printed
         */
        public void println(byte[] x) {
            print(x);
            println();
        }

        /**
         * Print a String and then terminate the line.  This method behaves as
         * though it invokes <code>{@link #print(String)}</code> and then
         * <code>{@link #println()}</code>.
         *
         * @param x the <code>String</code> value to be printed
         */
        public void println(String x) {
            print(x);
            println();
        }

        /**
         * Print an Object and then terminate the line.  This method behaves as
         * though it invokes <code>{@link #print(Object)}</code> and then
         * <code>{@link #println()}</code>.
         *
         * @param x the <code>Object</code> value to be printed
         */
        public void println(Object x) {
            print(x);
            println();
        }

    }  // end of PrintWriter


}

