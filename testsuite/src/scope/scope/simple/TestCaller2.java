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
public class TestCaller2 {


    @Scope(IMMORTAL)
    @SCJAllowed(members=true)
    @DefineScope(name="X", parent=IMMORTAL)
    static abstract class MS extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public MS() {
            super(null, null);
        }
    }

    @Scope(CALLER)
    @SCJAllowed(members=true)
    static public class A {

        F f =  new F();

        C c = new C();

        @RunsIn(CALLER)
        public void method (B b) {
            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            f.m();  // ERR

            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            this.f = new F();   // ERR

            b.m();  // OK

            L l = new L();
            l.m();             // OK

        }


        @RunsIn(CALLER)
        public void method (B b, C c) {

            b.mm(c);                        // OK

            b.method();                     // OK

            L l = new L();
            l.mm(c);                        // OK

            C cc = new C();
            l.mm(cc);                       // OK

            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            l.mm(this.c);                   // ERR

            Object o = b.mmm();             // OK

            B.method();                     //OK
        }

        public void method2 (B b) {
            b.method();                      // OK

            B.method();                     //OK
        }

    }

    @SCJAllowed(members=true)
    static public class F{
        public void m() {}

        public void mm(C c) {}
    }

    @SCJAllowed(members=true)
    static public class B{
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
    }


}