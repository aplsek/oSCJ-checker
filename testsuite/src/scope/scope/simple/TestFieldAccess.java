package scope.scope.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import java.util.Arrays;
import java.util.List;

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

@SCJAllowed(members=true)
@Scope(IMMORTAL)
public class TestFieldAccess {


    @Scope(IMMORTAL)
    @SCJAllowed(members=true)
    @DefineScope(name="X", parent=IMMORTAL)
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {
            super(null, null);
        }
    }

    @Scope("X")
    @SCJAllowed(members=true)
    @DefineScope(name="Y", parent="X")
    static abstract class Y extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public Y() {
            super(null, null);
        }
    }

    @Scope(CALLER)
    @SCJAllowed(members=true)
    static public class A {

        F f = new F();

        @RunsIn(CALLER)
        public void method (B b) {

            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            f.b.c.m();                  // 1/3 ERR

            F ff = new F();
            ff.b.c.m();             // OK
        }

        public void method2 (B b, @Scope(CALLER) C c, S s, SS ss) {

            f.b.c.m();

            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            s.f.b.c.m();            // 2/3 ERR

            s.f.b.c.mmm();            // OK

            s.mmm();                // OK

            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            ss.s.m();               // 3/3 ERR

            ss.s.mmm();             // OK

            ss.s.f.b.c.mmm();       // OK

            ss.f.b.c.mmm();         // OK
        }

        @RunsIn("Y")
        public void m( SS ss) {
            ss.f.m();               // OK
        }

    }

    @SCJAllowed(members=true)
    static public class F {

        B b = new B();

        public void m() {}

        public void mm(C c) {}
    }

    @SCJAllowed(members=true)
    static public class B{

        C c = new C();

        public void m() {}

        public void mm(C c) {}

        @Scope(CALLER)
        public Object mmm() {
            return new Object();
        }

        public static void method() {

        }
    }

    @SCJAllowed(members=true)
    static public class L{
        public void m() {}

        public void mm(C c) {}
    }

    @SCJAllowed(members=true)
    static public class C{
        public void m() {}

        @RunsIn(CALLER)
        public void mmm() {
        }
    }

    @Scope("X")
    @SCJAllowed(members=true)
    static public class S {
        F f = new F();

        public void m() {
        }

        @RunsIn(CALLER)
        public void mmm() {
        }
    }

    @Scope("Y")
    @SCJAllowed(members=true)
    static public class SS {
        S s;

        F f = new F();

        @RunsIn(CALLER)
        public void mmm() {
        }
    }
}