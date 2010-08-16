package javax.safetycritical;

import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class SingleMissionSequencer extends MissionSequencer
{
  @SCJAllowed
  @BlockFree
  public SingleMissionSequencer(PriorityParameters priority, StorageConfigurationParameters storage, Mission mission)
  { super(priority, storage);
  }

  /**
   * @see javax.safetycritical.MissionSequencer#getInitialMission()
   */
  @SCJAllowed
  @BlockFree
  @Override
  protected Mission getInitialMission()
  {return null;
  }
 
  @SCJAllowed
  @BlockFree
  @Override
  protected Mission getNextMission()
  {return null;
  }

}
