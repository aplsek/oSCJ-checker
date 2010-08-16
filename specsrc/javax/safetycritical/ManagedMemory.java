package javax.safetycritical;


import javax.safetycritical.annotate.SCJAllowed;

/**
 * This class provides a uniform method of retrieving a safety-critical
 * memory areas manager.
 */
@SCJAllowed
public interface ManagedMemory
{
  /**
   * @return the manager for this memory area.
   */
   @SCJAllowed
  public MissionManager getManager();
}
