package java.io;

public interface DataOutput
{

 /**
  * Writes len bytes from the specified byte array starting at offset off to the underlying output stream.
  */
  public void   write(byte[] b, int off, int len) throws IOException;

 /**
  * Writes the specified byte (the low eight bits of the argument b) to the underlying output stream.
  */
  public void   write(int b) throws IOException;

 /**
  * Writes a boolean to the underlying output stream as a 1-byte value.
  */
   public void   writeBoolean(boolean v) throws IOException;

 /**
  * Writes out a byte to the underlying output stream as a 1-byte value.
  */
  public void   writeByte(int v) throws IOException;

 /**
  * Writes a char to the underlying output stream as a 2-byte value, high byte first.
  */
  public void   writeChar(int v) throws IOException;

 /**
  * Writes a string to the underlying output stream as a sequence of characters.
  */
  public void   writeChars(String s) throws IOException;

 /**
  * Converts the double argument to a long using the doubleToLongBits method in class Double, and then writes that long
  * value to the underlying output stream as an 8-byte quantity, high byte first.
  */
  public void   writeDouble(double v) throws IOException;

  /**
  * Converts the float argument to an int using the floatToIntBits method in class Float,
  * and then writes that int value to the underlying output stream as a 4-byte quantity, high byte first.
  */
  public void   writeFloat(float v) throws IOException;


 /**
  * Writes an int to the underlying output stream as four bytes, high byte first.
  */
  public void   writeInt(int v) throws IOException;


 /**
  * Writes a long to the underlying output stream as eight bytes, high byte first.
  */
  public void   writeLong(long v) throws IOException;


 /**
  * Writes a short to the underlying output stream as two bytes, high byte first.
  */
   public void   writeShort(int v) throws IOException;

 /**
  * Writes a string to the underlying output stream using UTF-8 encoding in a machine-independent manner.
  */
  public void   writeUTF(String str) throws IOException;


}
