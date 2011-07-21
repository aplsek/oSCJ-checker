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
public class TestMethodArgument {

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

        F f = new F();

        @RunsIn(CALLER)
        public void method(B b) {

        }

        public void method2(B b, @Scope(CALLER) C c, S s, SS ss) {
            this.m();

            this.m(b, b, b);
        }

        private void m(B... bees) {
        }

        private void mm(S... sees) {
            for (S s : sees) {
                S ss = s;
            }

            for (Object o : sees) {
                S ss = (S) o;
            }
        }

    }

    @SCJAllowed(members = true)
    static public class F {
    }

    @SCJAllowed(members = true)
    static public class B {
    }

    @SCJAllowed(members = true)
    static public class L {
    }

    @SCJAllowed(members = true)
    static public class C {
    }

    @Scope("X")
    @SCJAllowed(members = true)
    static public class S {
    }

    @Scope("Y")
    @SCJAllowed(members = true)
    static public class SS {
    }
}