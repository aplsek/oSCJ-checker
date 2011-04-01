package java.lang.reflect;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class InvocationTargetException extends Exception
{
  @SCJAllowed
  private static final long serialVersionUID = -4241593084954200629L;

  @SCJAllowed
  public InvocationTargetException() {}

  @SCJAllowed
  public InvocationTargetException(String msg) { super(msg); }

  @SCJAllowed
  public InvocationTargetException(Throwable cause) { super(cause); }

  @SCJAllowed
  public InvocationTargetException(String msg, Throwable cause) { super(msg, cause); }
}
