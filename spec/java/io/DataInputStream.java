package java.io;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class DataInputStream extends InputStream implements DataInput
{
  /**
   * The input stream.
   */
  protected java.io.InputStream in;

  /**
   * Creates a DataInputStream and saves its argument, the input stream in, for later use.
   */
  public DataInputStream(java.io.InputStream in) { }

  /**
   * Returns the number of bytes that can be read from this input stream without blocking.
   * This method simply performs in.available() and returns the result.
   */
  public int available() throws IOException { return 0; }

  /**
   * Closes this input stream and releases any system resources associated with the stream. This method simply
   * performs in.close().
   */
  public void close() throws IOException {}

  /**
   * Marks the current position in this input stream. A subsequent call to the reset method repositions this
   * stream at the last marked position so that subsequent reads re-read the same bytes.
   * The readlimit argument tells this input stream to allow that many bytes to be read before the mark
   * position gets invalidated.
   * This method simply performs in.mark(readlimit).
   */
  public void mark(int readlimit) {}

  /**
   * Tests if this input stream supports the mark and reset methods. This method simply performs
   * in.markSupported().
   */
  public boolean markSupported() { return false; }

  /**
   * Reads the next byte of data from this input stream. The value byte is returned as an int in the range 0 to
   * 255. If no byte is available because the end of the stream has been reached, the value -1 is returned. This
   * method blocks until input data is available, the end of the stream is detected, or an exception is thrown.
   * This method simply performs in.read() and returns the result.
   */
  public int read() throws IOException { return 0; }

  /**
   * See the general contract of the read method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final int read(byte[] b) throws IOException { return 0; }

  /**
   * Reads up to len bytes of data from this input stream into an array of bytes. This method blocks until some
   * input is available.
   * This method simply performs in.read(b, off, len) and returns the result.
   */
  public final int read(byte[] b, int off, int len)  throws IOException { return 0; }

  /**
   * See the general contract of the readBoolean method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final boolean readBoolean() throws IOException { return false; }

  /**
   * See the general contract of the readByte method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final byte readByte() throws IOException { return 0; }

  /**
   * See the general contract of the readChar method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final char readChar() throws IOException { return 0; }

  /**
   * See the general contract of the readDouble method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final double readDouble() throws IOException { return 0.0; }

  /**
   * See the general contract of the readFloat method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final float readFloat() throws IOException { return (float)0.0; }

  /**
   * See the general contract of the readFully method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final void readFully(byte[] b) throws IOException {}

  /**
   * See the general contract of the readFully method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final void readFully(byte[] b, int off, int len) throws IOException {}

  /**
   * See the general contract of the readInt method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final int readInt() throws IOException { return 0; }

  /**
   * See the general contract of the readLong method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final long readLong() throws IOException { return 0L; }

  /**
   * See the general contract of the readShort method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final short readShort() throws IOException { return 0; }

  /**
   * See the general contract of the readUnsignedByte method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final int readUnsignedByte() throws IOException { return 0; }

  /**
   * See the general contract of the readUnsignedShort method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final int readUnsignedShort() throws IOException { return 0; }

  /**
   * See the general contract of the readUTF method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final java.lang.String readUTF() throws IOException { return ""; }

  /**
   * Reads from the stream in a representation of a Unicode character string encoded in Java modified UTF-8
   * format; this string of characters is then returned as a String. The details of the modified UTF-8
   * representation are exactly the same as for the readUTF method of DataInput
   */
  public static final java.lang.String readUTF(DataInput in) throws IOException { return ""; }

  /**
   * Repositions this stream to the position at the time the mark method was last called on this input stream.
   * This method simply performs in.reset().
   * Stream marks are intended to be used in situations where you need to read ahead a little to see what's in the
   * stream. Often this is most easily done by invoking some general parser. If the stream is of the type handled
   * by the parse, it just chugs along happily. If the stream is not of that type, the parser should toss an exception
   * when it fails. If this happens within readlimit bytes, it allows the outer code to reset the stream and try
   * another parser.
   */
  public void reset() throws IOException {}

  /**
   * Skips over and discards n bytes of data from the input stream. The skip method may, for a variety of
   * reasons, end up skipping over some smaller number of bytes, possibly 0. The actual number of bytes
   * skipped is returned.
   * This method simply performs in.skip(n).
   */
  public long skip(long n) throws IOException { return 0L; }

  /**
   * See the general contract of the skipBytes method of DataInput.
   * Bytes for this operation are read from the contained input stream.
   */
  public final int skipBytes(int n) throws IOException { return 0; }

}
