package scope.scope.simple;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;

import java.awt.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import javax.realtime.Clock;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PriorityScheduler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@SCJAllowed( members = true)
public abstract class TestConstructor {

    public One getA() {
        return new One(new A());  // OK
    }

    public void test () {
        new CyclicSchedule(
                new CyclicSchedule.Frame[] { new CyclicSchedule.Frame(new RelativeTime(200, 0),
                        null) });
    }

    public One get() {
        return new One();  // OK
    }

    @RunsIn(CALLER)
    public void method () {
        String prefix = "S-FRAME " + "" + " ";
    }

    public void m () {
        String prefix = "S-FRAME " + "" + " ";
    }


    @Scope(IMMORTAL)
    @SCJAllowed(members = true)
    static public class One  {
        A a;
        A aa;

        One() {
        }

        One(A a) {
            this.a = a;               // OK
            this.aa = new A();         // OK
        }

        @RunsIn(CALLER)
        public void method (A a) {
            this.method2(a);               // OK
            this.method2(new A());               // OK
        }

        @RunsIn(CALLER)
        public void method2 (A a) {
        }

        public void m (A a) {
            this.mm(a);
        }

        public void mm (A a) {
        }


        public void meth() {
            new One(new A());

            A a = new A();
            new One(a);
        }

        @RunsIn("X")
        public void meth(A a) {
            new A();

            new A(a);

            new A(new A());
        }
    }

    @SCJAllowed(members = true)
    static class A {
        public A() {
        }

        public A(A a) {
        }

        @RunsIn(CALLER)
        public void method () {
            String prefix = "S-FRAME " + "" + " ";
        }

        public void m () {
            String prefix = "S-FRAME " + "" + " ";
        }


        @RunsIn("X")
        public void meth() {
            final LinkedList ret = new LinkedList();

            final LinkedList list = new LinkedList();
            Object o = new Object();
            list.add(o);
        }

    }

    @Scope("X")
    @SCJAllowed(members = true)
    static class ST {

        final private HashMap    map = new HashMap();

        Object o = new Object();
        @RunsIn("X")
        void method () {
            map.put(o, new Object());
    }
    }

    @SCJAllowed(members=true)
    @Scope(IMMORTAL)
    @DefineScope(name="X", parent=IMMORTAL)
    abstract static class MSafelet extends CyclicExecutive {
        @SCJRestricted(INITIALIZATION)
        public MSafelet() {
            super(null);
        }


        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("X")
        public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {

            return new CyclicSchedule(
                              new CyclicSchedule.Frame[] { new CyclicSchedule.Frame(new RelativeTime(200, 0),
                                      handlers) });
        }
    }
}
