package java.lang;
public class IndexOutOfBoundsException extends RuntimeException
{
  /**
   * Create an exception without a message.
   */
  public IndexOutOfBoundsException()
    {
      super();
    }

  /**
   * Create an exception with a message.
   */
  public IndexOutOfBoundsException(String s)
    {
      super(s);
    }
}
