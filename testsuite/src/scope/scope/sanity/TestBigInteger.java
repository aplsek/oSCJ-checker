package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.MemoryArea;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(value = LEVEL_2, members = true)
public class TestBigInteger {

    @Scope(IMMORTAL)
    @DefineScope(name = "TM", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @SCJAllowed(value = LEVEL_2, members = true)
    @Scope("TM")
    static class TrainMission {
        public int value;

        public MBInt crypto_key = new MBInt(1);
        public MBInt crypto_key2 = null;


        @RunsIn("TM")
        public void run() {
            CalculateCryptoKey1 calculator1 = new CalculateCryptoKey1(this);

            ((ManagedMemory) MemoryArea.getMemoryArea(this))
                    .enterPrivateMemory(1000, calculator1);
        }
    }

    @DefineScope(name = "TM.0", parent = "TM")
    @SCJAllowed(value = LEVEL_2, members = true)
    @Scope("TM")
    static class CalculateCryptoKey1 implements Runnable {

        TrainMission tm;

        public CalculateCryptoKey1(TrainMission the_mission) {
            tm = the_mission;
        }

        @RunsIn("TM.0")
        public void run() {
            // some work
            MBInt t1, t2;
            t1 = new MBInt(128);
            t2 = new MBInt(128);

            tm.crypto_key = tm.crypto_key.multiplyA(t1);
            tm.crypto_key = tm.crypto_key.multiplyA(t2);

        }
    }

    @DefineScope(name = "TM.1", parent = "TM")
    @SCJAllowed(value = LEVEL_2, members = true)
    @Scope("TM")
    static class CalculateCryptoKey2 implements Runnable {

        TrainMission tm;

        public CalculateCryptoKey2(TrainMission the_mission) {
            tm = the_mission;
        }

        @RunsIn("TM.1")
        public void run() {
            // some work
            MBInt t1, t2, t3;
            t1 = new MBInt(128);
            t2 = new MBInt(128);
            t3 = t1.multiplyB(t2);

            AssignCryptoKey assigner = new AssignCryptoKey(tm, t3);
            ((ManagedMemory) (MemoryArea.getMemoryArea(tm))).executeInArea(assigner);

        }
    }

    @SCJAllowed(value=LEVEL_2, members=true)
    @Scope("TM.1")
    static class AssignCryptoKey implements Runnable {

      // assumes scope("TM")
      TrainMission tm;

      // assumes scope(THIS)
      MBInt bi;

      AssignCryptoKey(TrainMission tm, MBInt bi) {
        this.tm = tm;
        this.bi = bi;
      }

      @RunsIn("TM")
      public void run() {
        // copy bi into the "TM" scope (from the "TM.0" scope)
        tm.crypto_key2 = new MBInt(bi);

        // or
        tm.crypto_key2 = bi.multiplyB(new MBInt(1));
      }
    }

    /**
     * MyBigInteger - immutable class - TODO: use singletons?
     *
     * - do not change the API of BigInteger!
     */
    @SCJAllowed(value = LEVEL_2, members = true)
    static class MBInt {
        public int value;

        public MBInt(int val) {
            value = val;
        }

        public MBInt(MBInt bi) {
            value = bi.value;
        }

        // public MBInt(@Scope(IMMORTAL) MBInt bi) {
        // //TODO:
        // }

        @RunsIn(CALLER)
        @Scope(CALLER)
        public MBInt multiplyB(MBInt bi) {
            int n_value = compute(bi);
            return new MBInt(n_value);
        }

        @RunsIn(CALLER)
        @Scope(THIS)
        public MBInt multiplyA(MBInt bi) {
            try {
                MBInt n_bi;
                n_bi = (MBInt) MemoryArea.newInstanceInArea(this,MBInt.class);
                n_bi.deepCopy(bi);
                return n_bi;

            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            }

            return null;

        }

        @RunsIn(CALLER)
        private void deepCopy(MBInt bi) {
            this.value = bi.value;
        }

        @RunsIn(CALLER)
        private int compute(MBInt bi) {
            // TODO: Do the computation.
            return 0;
        }
    }
}
