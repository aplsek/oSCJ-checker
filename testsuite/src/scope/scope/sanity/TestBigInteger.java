package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import java.math.BigInteger;
import java.util.Random;

import javax.realtime.AbsoluteTime;
import javax.realtime.PriorityParameters;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
public class TestBigInteger {


    @Scope(IMMORTAL)
    @DefineScope(name = "X", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @SCJAllowed(members=true)
    @Scope("X")
    static class Data  {
        public int value;

        @RunsIn("X")
        public void compareTo() {



            AbsoluteTime now = javax.realtime.Clock.getRealtimeClock().getTime();
            Random r = new Random(now.getMilliseconds());
            BigInteger t1, t2, t3;

            t1 = new BigInteger(128, 24, r);
            t2 = new BigInteger(128, 24, r);
            t3 = t1.multiply(t2);

        }
    }

}


