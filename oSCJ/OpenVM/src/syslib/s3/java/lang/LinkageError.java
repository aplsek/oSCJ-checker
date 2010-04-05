package java.lang;


public class LinkageError extends Error
{
  /**
   * Create an exception without a message.
   */
  public LinkageError()
    {
      super();
    }

  /**
   * Create an exception with a message.
   */
  public LinkageError(String s)
    {
      super(s);
    }
}
