package javax.safetycritical;

import javax.realtime.PriorityParameters;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.BlockFree;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public class SingleMissionSequencer extends MissionSequencer {

    private Mission _mission;

// (annotations turned off to work with Java 1.4)     @SCJAllowed
// (annotations turned off to work with Java 1.4)     @BlockFree
    public SingleMissionSequencer(PriorityParameters priority,
            StorageConfigurationParameters storage, Mission mission) {
        super(priority, storage);
        _mission = mission;
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed
// (annotations turned off to work with Java 1.4)     @BlockFree
// (annotations turned off to work with Java 1.4)     @Override
    protected Mission getInitialMission() {
        return _mission;
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed
// (annotations turned off to work with Java 1.4)     @BlockFree
// (annotations turned off to work with Java 1.4)     @Override
    protected Mission getNextMission() {
        return _mission;
    }
}
