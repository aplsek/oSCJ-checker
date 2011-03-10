package scope.level1;

import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PriorityScheduler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;

@Scope(IMMORTAL)
@SCJAllowed(value=LEVEL_1, members=true)
public class One implements Safelet {

    @SCJRestricted(INITIALIZATION)
    public MissionSequencer getSequencer() {
        return new OneSequencer(new PriorityParameters(PriorityScheduler
                .instance().getNormPriority()), new StorageParameters(100000L,
                1000, 1000));
    }

    public void setUp() {
    }

    @SCJRestricted(CLEANUP)
    public void tearDown() {
    }
}
