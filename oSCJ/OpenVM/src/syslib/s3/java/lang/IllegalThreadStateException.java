package java.lang;


public class IllegalThreadStateException extends Exception
{
  /**
   * Create an exception without a message.
   */
  public IllegalThreadStateException()
    {
      super();
    }

  /**
   * Create an exception with a message.
   */
  public IllegalThreadStateException(String s)
    {
      super(s);
    }
}
