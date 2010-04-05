
package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.ImmortalMemory;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Allocate.Area;

/**
 * Mission is the super class of the main class in a Safety Critical Java
 * application. A subclass must implement the method initialize() to
 * sets up the Mission and missionMemorySize() to define how much memory the 
 * mission requires. The initial instance of the subclass will be
 * allocated in immortal memory by the safety critical Java system.
 * <p>
 * 
 * initialize() is executed in a NoHeapRealtimeThread that is running
 * in a mission memory. The mission memory size is calculated by
 * calling missionMemorySize(). 
 * <p>
 * 
 * Only Missions which implement Safelet can be started
 * directly. These missions are called primary missions. A primary
 * mission can implement Safelet directly if it is a Level 1 or Level
 * 2 application. A Level 0 primary mission implements Safelet
 * indirectly by extending CyclicExecutive. 
 * <p>
 */

@SCJAllowed
public abstract class Mission
{
  /**
   * Constructor for a Mission.  TBD: what does Foo have to do with
   * Mission?  Under what circumstance will this constructor throw an
   * IllegalStateException? 
   * 
   * @throws IllegalStateException
   *             if an instance of Foo has already been created.
   */
  @Allocate({Area.THIS}) // initializer_ initialization
  //Ales: @SCJAllowed(LEVEL_1)
    @SCJAllowed
  public Mission() {
  }
  
  /**
   * Method to clean up after an application terminates. It is called after
   * all threads have been terminated but before the MissionScope is exited.
   * By default, nothing is done.
   */
  @SCJAllowed(LEVEL_1)
  protected void cleanup() {
  }
  
  /**
   * Perform initialization of the mission. All missions must define
   * an initialize() method that will, at a minimum, create the event
   * handlers for this mission. In addition, all other objects
   * associated with this mission should be initialized 
   * in this method.
   */
  @SCJAllowed()
  protected abstract void initialize();
  
  // not sure what this does.  i believe this is an implementation
  // artifact, not SCJAllowed
  static Mission instance() {
    return null;
  }
  
  /*
   * Each vendor of a JSR-302 run-time must provide a mechanism to map
   * String names to SchedulableParameters.
   * <p>
   * The process of developing a Mission includes the following:
   *
   *  1. Identify all Schedulables associated with this Mission
   *  2. Identify the resource needs of each Schedulable, which may
   *     be vendor-dependent.
   *  3. Organize all Schedulables into clusters, where each cluster
   *     contains all the Schedulables that have the same resource needs
   *  4. Create a string name to name the SchedulableParameters
   *     associated with each cluster of Schedulables
   *  5. Work with the vendor to establish a mapping between each
   *     string name and the corresponding SchedulableParameters
   *     object.
   *  6. Maybe we have some "reserved names" to represent default
   *     SchedulableParameters values, e.g. to run HelloWorld without
   *     requiring a lot of analysis.  ("*default_small_stack*",
   *     "*default_medium_stack*", or null means a default
   *     typical stack.)
   */
  // Mike prefers to "hide" the existence of SchedulableParameters,
  // and use a String parameter to each Schedulable constructor which
  // gets mapped to the appropriate "internal representation" of
  // SchedulableParameters information.  The mapping would be specific
  // to the given instance of Mission, and may depend on parameters to
  // the Mission's constructor.
  //
  // protected SchedulableParameters get(String name);
  //
  // We are leaving this commentary in place because the final
  // resolution has not yet been approved by consensus.
  


  @SCJAllowed()
  public void requestTermination() {
  }
  
  /**
   * Ask for termination of the current mission and its sequencer.
   * Upon terminiation, all activities will be stopped, all scoped
   * memory areas including mission memory will be reclaimed.
   */
  @SCJAllowed()
  public void requestSequenceTermination() {
	  
  }
  
  /**
   * @return the amount of memory that this mission will require to operate.
   */
  @SCJAllowed()
  abstract public long missionMemorySize();

  /**
   * Obtain the current mission.
   *
   * @return the current mission instance.
   */
  @SCJAllowed
  public static Mission getCurrentMission() {
	  return null;
  }
}

