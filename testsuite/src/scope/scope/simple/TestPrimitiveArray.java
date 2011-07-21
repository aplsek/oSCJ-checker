package scope.scope.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@SCJAllowed(members = true)
@Scope(IMMORTAL)
public class TestPrimitiveArray {

    @Scope(IMMORTAL)
    @SCJAllowed(members = true)
    @DefineScope(name = "X", parent = IMMORTAL)
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {
            super(null, null);
        }
    }

    @Scope("X")
    @SCJAllowed(members = true)
    @DefineScope(name = "Y", parent = "X")
    static abstract class Y extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public Y() {
            super(null, null);
        }
    }

    @Scope(CALLER)
    @SCJAllowed(members = true)
    static public class A {

        int[] arr;

        public void method(T t) {
            arr = new int[10];

            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            arr = t.arr;         // ERR
        }

        @RunsIn(CALLER)
        public void m(int[] a, T t) {
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            arr = new int[10];          // ERR

            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            arr = a;            // ERR

            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            arr = t.arr;            // ERR
        }


    }

    @Scope("X")
    static public class T {

        int[] arr;
    }

}