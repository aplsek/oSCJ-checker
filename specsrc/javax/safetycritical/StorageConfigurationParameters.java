package javax.safetycritical;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class StorageConfigurationParameters
{

  /**
   * Stack sizes for schedulable objects and sequencers. Passed as
   * parameter to the constructor of mission sequencers and
   * schedulable objects. 
   *
   * @param totalBackingStore size of the backing store reservation
   *        for worst-case scope usage in bytes
   * @param nativeStack size of native stack in bytes (vendor specific)
   * @param javaStack size of Java execution stack in bytes (vendor specific)
   */
  @SCJAllowed
  public StorageConfigurationParameters(long totalBackingStore,
                                        int nativeStack, int javaStack) {}
  
  /**
   * Stack sizes for schedulable objects and sequencers. Passed as
   * parameter to the constructor of mission sequencers and
   * schedulable objects. 
   *
   * @param totalBackingStore size of the backing store reservation
   *        for worst-case scope usage in bytes 
   *
   * @param nativeStack size of native stack in bytes (vendor specific)
   *
   * @param javaStack size of Java execution stack in bytes (vendor specific)
   *
   * @param messageLength length of the space in bytes dedicated to
   *        message associated with this Schedulable object's
   *        ThrowBoundaryError exception plus all the method  
   *        names/identifiers in the stack backtrace
   *
   * @param stackTraceLength the number of byte for the
   *        StackTraceElement array dedicated to stack backtrace associated
   *        with this Schedulable object's ThrowBoundaryError exception. 
   */
  @SCJAllowed
  public StorageConfigurationParameters(long totalBackingStore,
                                        int nativeStackSize, int javaStackSize,
                                        int messageLength,
                                        int stackTraceLength) { }

  /**
   *
   * @return the size of the total backing store available for scoped
   *         memory areas created by the assocated SO.
   */
  @SCJAllowed
  public long getTotalBackingStoreSize() { return 0L; }
    
  /**
   *
   * @return the size of the native method stack available to the assocated SO.
   */
  @SCJAllowed
  public long getNativeStackSize() { return 0L; }
    
  /**
   *
   * @return the size of the Java stack available to the assocated SO.
   */ 
  @SCJAllowed
  public long getJavaStackSize() { return 0L; }
    
  /**
   * 
   * return the length of the message buffer
   */
  @SCJAllowed
  public int getMessageLength(){return 0; }

  /**
   * 
   * return the length of the stack trace buffer
   */
  @SCJAllowed
  public int getStackTraceLength() {return 0; }
}
