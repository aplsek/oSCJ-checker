/*---------------------------------------------------------------------*\
 *
 * aicas GmbH, Karlsruhe, Germany 2007
 *
 * This code is provided to the JSR 302 group for evaluation purpose
 * under the LGPL 2 license from GNU.  This notice must appear in all
 * derived versions of the code and the source must be made available
 * with any binary version.  Viewing this code does not prejudice one
 * from writing an independent version of the classes within.
 *
 * $Source: /home/cvs/jsr302/scj/specsrc/javax/safetycritical/Safelet.java,v $
 * $Revision: 1.4 $
 * $Author: schoeberl $
 * Contents: Java source of HIJA Safety Critical Java class Safelet
 *
\*---------------------------------------------------------------------*/

package javax.safetycritical;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * Safelet provides the means of declaring and starting a top level Mission in
 * an RTSJ implementation. A top level mission is defined by
 * subclassing Safelet 
 * and defining. Calling
 * its main creates a NoHeapRealtime Thread and makes sure that the classes
 * declared for the Mission are initialized. Then it starts the given Safelet
 * named on the command line. This is necessary since a Safety Critical
 * Application has a different startup configuration from a standard RTSJ
 * implementation.
 * <p>
 * TBD: kelvin removed the link to requiredClasses because we removed
 * this from the API I believe.
 */
@SCJAllowed
public interface Safelet {

  
  /**
   * getSequencer
   * 
   * @return the mission sequencer that should be used for this application.
   */
  @SCJAllowed
  public MissionSequencer getSequencer();
  
  public void setup();
  
  /**
   * TODO: changed to "teardown" to be compatible with the RI,
   * but I think we agreed on tearDown (MS)
   */
  public void teardown();

}
