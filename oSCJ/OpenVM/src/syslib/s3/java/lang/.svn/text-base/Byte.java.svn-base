package java.lang;
public final class Byte extends Number implements Comparable {
    private byte val;
    public int compareTo(Object o) { return val - ((Byte) o).val; }
    public Byte(byte val) { this.val = val; }


  /**
   * Converts the specified <code>String</code> into a <code>byte</code>.
   * This function assumes a radix of 10.
   *
   * @param s the <code>String</code> to convert
   * @return the <code>byte</code> value of <code>s</code>
   * @throws NumberFormatException if <code>s</code> cannot be parsed as a
   *         <code>byte</code>
   * @see #parseByte(String)
   */
  public static byte parseByte(String s)
  {
    return parseByte(s, 10);
  }

  /**
   * Converts the specified <code>String</code> into an <code>int</code>
   * using the specified radix (base). The string must not be <code>null</code>
   * or empty. It may begin with an optional '-', which will negate the answer,
   * provided that there are also valid digits. Each digit is parsed as if by
   * <code>Character.digit(d, radix)</code>, and must be in the range
   * <code>0</code> to <code>radix - 1</code>. Finally, the result must be
   * within <code>MIN_VALUE</code> to <code>MAX_VALUE</code>, inclusive.
   * Unlike Double.parseDouble, you may not have a leading '+'.
   *
   * @param s the <code>String</code> to convert
   * @param radix the radix (base) to use in the conversion
   * @return the <code>String</code> argument converted to </code>byte</code>
   * @throws NumberFormatException if <code>s</code> cannot be parsed as a
   *         <code>byte</code>
   */
  public static byte parseByte(String s, int radix)
  {
    int i = Integer.parseInt(s, radix, false);
    if ((byte) i != i)
      throw new NumberFormatException();
    return (byte) i;
  }

  public byte byteValue() {
	  return val;
  }
  public int intValue() { return (int)val; }
  public long longValue() { return (long)val; }
  public float floatValue() { return (float)val; }
  public double doubleValue() { return (double)val; }
}
