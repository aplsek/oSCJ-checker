package java.lang;

import java.io.Serializable;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;

@SCJAllowed
public class IllegalThreadStateException
  extends RuntimeException implements Serializable
{
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public IllegalThreadStateException() {}

  @Allocate({CURRENT})
  @MemoryAreaEncloses(inner = {"this"}, outer = {"msg"})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public IllegalThreadStateException(String description)
  {
    super(description);
  }
}
