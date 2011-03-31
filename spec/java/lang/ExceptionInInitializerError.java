package java.lang;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class ExceptionInInitializerError extends Exception
{
  @SCJAllowed
  private static final long serialVersionUID = -4241593084954200629L;

  @SCJAllowed
  public ExceptionInInitializerError() {}

  @SCJAllowed
  public ExceptionInInitializerError(String msg) { super(msg); }

  @SCJAllowed
  public ExceptionInInitializerError(Throwable cause) { super(cause); }

  @SCJAllowed
  public ExceptionInInitializerError(String msg, Throwable cause) { super(msg, cause); }
}
