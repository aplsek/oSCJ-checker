package scope.scope.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("D")
@SCJAllowed(value = LEVEL_2, members = true)
public class TestUpcast {

    @Scope("D")
    @DefineScope(name = "D", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @Scope("C")
    @DefineScope(name = "C", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class Y extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Y(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }


    public void method() {
        MyRun r = new MyRun();
        Runnable up ;

        //## checkers.scope.ScopeChecker.ERR_BAD_RUNNABLE_UPCAST
        up = r;

        MyRun2 r2 = new MyRun2();
        up = r2;
    }

    @SCJAllowed(value = LEVEL_2, members = true)
    class MyRun implements Runnable {

        @Override
        @RunsIn("C")
        public void run() {
        }
    }

    @SCJAllowed(value = LEVEL_2, members = true)
    class MyRun2 implements Runnable {

        @Override
        public void run() {
        }
    }
}

