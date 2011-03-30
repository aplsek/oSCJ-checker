package thruster;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.RelativeTime;
import javax.safetycritical.Mission;
import javax.safetycritical.StorageParameters;

 /**
  * This class contains an PEH which will terminate the MissionSequencer.
  *
  * @author Lilei Zhai
  *
  */
public class MyTerminatorMission extends Mission {
	private MyTerminatorPeriodicEventHandler myPEH;

	@Override
    protected void initialize() {

		myPEH = new MyTerminatorPeriodicEventHandler(
				new PriorityParameters(PriorityScheduler.instance().getNormPriority()),
				new PeriodicParameters(new RelativeTime(), new RelativeTime(500, 0)),
				new StorageParameters(100, 100, 100), 10000, "MyPEH");

		myPEH.register();
	}

	@Override
    public long missionMemorySize() {
		return 0;
	}

	protected void cleanup() {
	}

}
