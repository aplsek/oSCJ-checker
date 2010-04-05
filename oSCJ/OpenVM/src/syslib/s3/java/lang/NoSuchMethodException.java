package java.lang;


public class NoSuchMethodException extends Exception
{
  /**
   * Create an exception without a message.
   */
  public NoSuchMethodException()
    {
      super();
    }

  /**
   * Create an exception with a message.
   */
  public NoSuchMethodException(String s)
    {
      super(s);
    }
}
