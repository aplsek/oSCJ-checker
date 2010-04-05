package java.lang;


public class UnsupportedOperationException extends RuntimeException
{
  /**
   * Create an exception without a message.
   */
  public UnsupportedOperationException()
    {
      super();
    }

  /**
   * Create an exception with a message.
   */
  public UnsupportedOperationException(String s)
    {
      super(s);
    }
}
