package javax.realtime;

import static javax.safetycritical.annotate.Allocate.Area.SCOPED;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;

public abstract class ScopedMemory extends MemoryArea
{
  @Allocate({SCOPED})
  @BlockFree
  @SCJProtected
  public ScopedMemory(long size) {}

  @Allocate({SCOPED})
  @BlockFree
  @SCJProtected
  public ScopedMemory(SizeEstimator estimator) {}

  /**
   * Not @SCJAllowed
   *
  public Object getPortal() throws MemoryAccessError, IllegalAssignmentError
  {
    return null; // dummy return
  }
   */

  /**
   * Not @SCJAllowed
   *
  public void setPortal(Object object) {}
   */

  public void join() throws InterruptedException {}
}
