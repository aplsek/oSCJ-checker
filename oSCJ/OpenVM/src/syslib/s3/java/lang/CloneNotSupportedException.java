package java.lang;
public class CloneNotSupportedException extends Exception
{
  /**
   * Create an exception without a message.
   */
  public CloneNotSupportedException()
    {
      super();
    }

  /**
   * Create an exception with a message.
   */
  public CloneNotSupportedException(String s)
    {
      super(s);
    }
}
