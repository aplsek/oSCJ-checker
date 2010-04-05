package javax.safetycritical;

import javax.realtime.Happening;
import javax.safetycritical.annotate.SCJAllowed;

  /**
   *
   */
@SCJAllowed
public class InterruptHappening extends Happening {

 /**
   *
   */
  @SCJAllowed
  public InterruptHappening() {};

  /**
   *
   */
  @SCJAllowed
  public InterruptHappening(int id) {};

  /**
   *
   */
  @SCJAllowed
  public InterruptHappening(int id, String name) {};

  /**
   *
   */
  @SCJAllowed
  public InterruptHappening(String name) {};
  
  
  /**
   *
   */
  @SCJAllowed
  protected synchronized void process()  {}; 
  
  /**
   *
   */
  @SCJAllowed
  public final int getPriority(int id)   { return 1;};
  
}
