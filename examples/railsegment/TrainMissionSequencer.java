package railsegment;

import javax.realtime.PriorityParameters;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@Scope(IMMORTAL)
public class TrainMissionSequencer extends MissionSequencer {
  private returned_mission;

  public TrainMissionSequencer() {
    super(new PriorityParameters(SequencerPriority),
          new StorageParameters(TrainMission.BackingStoreRequirements,
                                TrainMission.NativeStackRequirements,
                                TrainMission.JavaStackRequirements));
    returned_mission = false;
  }

  @RunsIn("TM")
  public Mission getNextMission() {
    if (returned_mission) {
      return null;
    }
    else {
      returned_mission = true;
      return new TrainMission();
    }
  }
}