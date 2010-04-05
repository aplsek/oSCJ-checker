package java.lang;


public class ClassFormatError extends Error
{
  /**
   * Create an exception without a message.
   */
  public ClassFormatError()
    {
      super();
    }

  /**
   * Create an exception with a message.
   */
  public ClassFormatError(String s)
    {
      super(s);
    }
}
