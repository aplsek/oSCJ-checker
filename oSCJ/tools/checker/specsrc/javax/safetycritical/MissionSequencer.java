package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_2;

import javax.realtime.BoundAsyncEventHandler;
import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public abstract class MissionSequencer extends BoundAsyncEventHandler 
{
  @MemoryAreaEncloses(inner = { "this" }, outer = { "priority" })
  @SCJAllowed
  public MissionSequencer(PriorityParameters priority,
                          StorageConfigurationParameters storage) {
  }
  
  /**
   * @return The Mission of the first mission to run.  Will
   * be called only once.
   */
  @SCJAllowed
  protected abstract Mission getInitialMission();
  
  /**
   * @return null if no further missions should be started.
   *
   * The same mission may be started several times.  The return value
   * may depend on computations of previous missions, provided that
   * the relevant information computed by previous missions has been
   * copied into this MissionSequencer object.  By the time this
   * MissionSequencer's event handling thread invokes this method, the
   * previously executed MissionMemory has been exited and reclaimed.
   */
  @SCJAllowed
  protected abstract Mission getNextMission();
  
  /**
   * TBD: Someone has described this method as "package private
   * now". However, there's no way to override the public visibility
   * inherited from BoundAsyncEventHandler.
   *
   * James is going to change this so that ManagedEventHandler is an
   * interface rather than a subclass of BAEH.  That will allow us
   * to remove unwanted methods inherited from irrelevant superclasses...
   */
  @SCJAllowed
  public final synchronized void handleAsyncEvent() {
  }

  /**
   * This final method implements the infrastructure code required to
   * run a sequence of missions.  User code should not invoke this
   * method directly, as undefined behavior will result.
   *
   * Aside: Though user code most likely has access to this
   * MissionSequencer object, which is a ManagedEventHandler, the user
   * code does not have access to the event that is authorized to
   * trigger its execution.  That Event is hidden within the
   * infrastructure.  Thus, there is no way for user code to fire the
   * MissionSequencer's handleEvent() code.  This is intentional.
   *
   * TBD: Is there a way for handleEvent() to check if it is not being
   * executed by the thread bound to this event handler?  It would be
   * desirable to throw an exception in that case rather than allowing
   * the "undefined behavior" to result.  It would be even better if we
   * could hide this method entirely from the user.  We don't have to
   * "publish" that the MissionSequencer is a ManagedEventHandler even
   * though it behaves as if it were one...
   */
  @SCJAllowed
  public final synchronized void handleEvent()
  {
  }
  
  /**
   * This method is only intended to be run at Level 2. 
   * At Levels 0 and 1, the infrastructure automatically starts the
   * mission sequencer.
   * <p>
   * At level 2, the initial mission sequencer is started by
   * infrastructure code, but application code is required to
   * explicitly start inner-nested MissionSequencers.  They may do so
   * from any Schedulable context, including the enclosing
   * MissionSequencer's event handling thread.
   */
  @SCJAllowed(LEVEL_2)
  public final synchronized void start() {
  }
  
  /**
   * Try to finish the work of this mission sequencer soon by invoking
   * the currently running Mission's requestTermination method.
   * Upon completion of the currently running Mission, this
   * MissionSequencer shall return from its eventHandler method
   * without invoking getNextMission and without starting 
   * any additional missions.  
   * <p>
   * Note that requestSequenceTermination does not force the sequence
   * to terminate because the currently running Mission must
   * voluntarily relinquish its resources.
   */
  @SCJAllowed(LEVEL_2)
  public final void requestSequenceTermination() {
  }
}
