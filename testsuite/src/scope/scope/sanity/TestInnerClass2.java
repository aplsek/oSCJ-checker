package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
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

@Scope(IMMORTAL)
@DefineScope(name = "A", parent = IMMORTAL)
@SCJAllowed(members = true)
public class TestInnerClass2 extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestInnerClass2(PriorityParameters priority, StorageParameters storage) {
        super(priority, storage);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("A")
    protected Mission getNextMission() {
        return null;
    }
}

@SCJAllowed(members = true)
@Scope("A")
class AA {

    @SCJAllowed(members = true)
    @Scope("A")
    public class X {}

    @SCJAllowed(members = true)
    static class Y {}

    @SCJAllowed(members = true)
    @Scope("A") class Z {}

    @Scope(IMMORTAL)
    @DefineScope(name = "A", parent = IMMORTAL)
    @SCJAllowed(members = true)
    static class MS extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public MS(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("A")
        protected Mission getNextMission() {
            return null;
        }
    }

    @SCJAllowed(members = true)
    static class Bar {

        @RunsIn("A")
        void foo() {

            AA i = new AA();

            X x = i.new X();  // OK

            new AA.Y();  // OK

            Z z = i.new Z();  // OK
        }
    }
}


@SCJAllowed(members = true)
class O {

    F f;

    @SCJAllowed(members = true)
    class I {

        F fff;

        void mm() {
            F ff  = f;
            m();
        }
    }

    void m () {
        O o = new O();
        O.I i = o.new I();
    }

    void m2 () {
        I i = this.new I();
        F myF = i.fff;
    }
}

@SCJAllowed(members = true)
class F {}
