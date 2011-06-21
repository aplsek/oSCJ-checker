package scope.scope.simple;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

/**
 * This class is the Safelet for unit test. This unit test program is also an example level one
 * SCJ application.
 */
@SCJAllowed(members = true)
class TestOuterClassThis2 {

    FF f;

    @SCJAllowed(members = true)
    public class ZZZ {
        void method(){
            TestOuterClassThis2.this.f = new FF();
        }
    }
}

@SCJAllowed(members = true)
class FF {}