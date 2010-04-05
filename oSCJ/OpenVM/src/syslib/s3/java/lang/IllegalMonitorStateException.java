package java.lang;

public class IllegalMonitorStateException extends Exception
{
  /**
   * Create an exception without a message.
   */
  public IllegalMonitorStateException()
    {
      super();
    }

  /**
   * Create an exception with a message.
   */
  public IllegalMonitorStateException(String s)
    {
      super(s);
    }
}

